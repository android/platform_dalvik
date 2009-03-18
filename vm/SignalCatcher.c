/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This is a thread that catches signals and does something useful.  For
 * example, when a SIGQUIT (Ctrl-\) arrives, suspend the VM and dump the
 * status of all threads.
 */
#include "Dalvik.h"

#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <pthread.h>
#include <sys/file.h>
#include <sys/time.h>
#include <fcntl.h>
#include <errno.h>

static void* signalCatcherThreadStart(void* arg);

/*
 * Crank up the signal catcher thread.
 *
 * Returns immediately.
 */
bool dvmSignalCatcherStartup(void)
{
    gDvm.haltSignalCatcher = false;

    if (!dvmCreateInternalThread(&gDvm.signalCatcherHandle,
                "Signal Catcher", signalCatcherThreadStart, NULL))
        return false;

    return true;
}

/*
 * Shut down the signal catcher thread if it was started.
 *
 * Since we know the thread is just sitting around waiting for signals
 * to arrive, send it one.
 */
void dvmSignalCatcherShutdown(void)
{
    gDvm.haltSignalCatcher = true;
    if (gDvm.signalCatcherHandle == 0)      // not started yet
        return;

    pthread_kill(gDvm.signalCatcherHandle, SIGQUIT);

    pthread_join(gDvm.signalCatcherHandle, NULL);
    LOGV("signal catcher has shut down\n");
}


/*
 * Print the name of the current process, if we can get it.
 */
static void printProcessName(const DebugOutputTarget* target)
{
    int fd = -1;

    fd = open("/proc/self/cmdline", O_RDONLY, 0);
    if (fd < 0)
        goto bail;

    char tmpBuf[256];
    ssize_t actual;

    actual = read(fd, tmpBuf, sizeof(tmpBuf)-1);
    if (actual <= 0)
        goto bail;

    tmpBuf[actual] = '\0';
    dvmPrintDebugMessage(target, "Cmd line: %s\n", tmpBuf);

bail:
    if (fd >= 0)
        close(fd);
}

/*
 * Dump the stack traces for all threads to the log or to a file.  If it's
 * to a file we have a little setup to do.
 */
static void logThreadStacks(void)
{
    DebugOutputTarget target;

    if (gDvm.stackTraceFile == NULL) {
        /* just dump to log file */
        dvmCreateLogOutputTarget(&target, ANDROID_LOG_INFO, LOG_TAG);
        dvmDumpAllThreadsEx(&target, true);
    } else {
        FILE* fp = NULL;
        int cc, fd;

        /*
         * Open the stack trace output file, creating it if necessary.  It
         * needs to be world-writable so other processes can write to it.
         */
        fd = open(gDvm.stackTraceFile, O_WRONLY | O_APPEND | O_CREAT, 0666);
        if (fd < 0) {
            LOGE("Unable to open stack trace file '%s': %s\n",
                gDvm.stackTraceFile, strerror(errno));
            return;
        }

        /* gain exclusive access to the file */
        cc = flock(fd, LOCK_EX | LOCK_UN);
        if (cc != 0) {
            LOGV("Sleeping on flock(%s)\n", gDvm.stackTraceFile);
            cc = flock(fd, LOCK_EX);
        }
        if (cc != 0) {
            LOGE("Unable to lock stack trace file '%s': %s\n",
                gDvm.stackTraceFile, strerror(errno));
            close(fd);
            return;
        }

        fp = fdopen(fd, "a");
        if (fp == NULL) {
            LOGE("Unable to fdopen '%s' (%d): %s\n",
                gDvm.stackTraceFile, fd, strerror(errno));
            flock(fd, LOCK_UN);
            close(fd);
            return;
        }

        dvmCreateFileOutputTarget(&target, fp);

        pid_t pid = getpid();
        time_t now = time(NULL);
        struct tm* ptm;
#ifdef HAVE_LOCALTIME_R
        struct tm tmbuf;
        ptm = localtime_r(&now, &tmbuf);
#else
        ptm = localtime(&now);
#endif
        dvmPrintDebugMessage(&target,
            "\n\n----- pid %d at %04d-%02d-%02d %02d:%02d:%02d -----\n",
            pid, ptm->tm_year + 1900, ptm->tm_mon+1, ptm->tm_mday,
            ptm->tm_hour, ptm->tm_min, ptm->tm_sec);
        printProcessName(&target);
        dvmPrintDebugMessage(&target, "\n");
        fflush(fp);     /* emit at least the header if we crash during dump */
        dvmDumpAllThreadsEx(&target, true);
        fprintf(fp, "----- end %d -----\n", pid);

        /*
         * Unlock and close the file, flushing pending data before we unlock
         * it.  The fclose() will close the underyling fd.
         */
        fflush(fp);
        flock(fd, LOCK_UN);
        fclose(fp);

        LOGI("Wrote stack trace to '%s'\n", gDvm.stackTraceFile);
    }
}


/*
 * Sleep in sigwait() until a signal arrives.
 */
static void* signalCatcherThreadStart(void* arg)
{
    Thread* self = dvmThreadSelf();
    sigset_t mask;
    int cc;

    UNUSED_PARAMETER(arg);

    LOGV("Signal catcher thread started (threadid=%d)\n", self->threadId);

    /* set up mask with signals we want to handle */
    sigemptyset(&mask);
    sigaddset(&mask, SIGQUIT);
    sigaddset(&mask, SIGUSR1);

    while (true) {
        int rcvd;

        dvmChangeStatus(self, THREAD_VMWAIT);

        /*
         * Signals for sigwait() must be blocked but not ignored.  We
         * block signals like SIGQUIT for all threads, so the condition
         * is met.  When the signal hits, we wake up, without any signal
         * handlers being invoked.
         *
         * We want to suspend all other threads, so that it's safe to
         * traverse their stacks.
         *
         * When running under GDB we occasionally return with EINTR (e.g.
         * when other threads exit).
         */
loop:
        cc = sigwait(&mask, &rcvd);
        if (cc != 0) {
            if (cc == EINTR) {
                //LOGV("sigwait: EINTR\n");
                goto loop;
            }
            assert(!"bad result from sigwait");
        }

        if (!gDvm.haltSignalCatcher) {
            LOGI("threadid=%d: reacting to signal %d\n",
                dvmThreadSelf()->threadId, rcvd);
        }

        /* set our status to RUNNING, self-suspending if GC in progress */
        dvmChangeStatus(self, THREAD_RUNNING);

        if (gDvm.haltSignalCatcher)
            break;

        if (rcvd == SIGQUIT) {
            dvmSuspendAllThreads(SUSPEND_FOR_STACK_DUMP);
            dvmDumpLoaderStats("sig");

            logThreadStacks();

            if (false) {
                dvmLockMutex(&gDvm.jniGlobalRefLock);
                dvmDumpReferenceTable(&gDvm.jniGlobalRefTable, "JNI global");
                dvmUnlockMutex(&gDvm.jniGlobalRefLock);
            }

            //dvmDumpTrackedAllocations(true);
            dvmResumeAllThreads(SUSPEND_FOR_STACK_DUMP);
        } else if (rcvd == SIGUSR1) {
#if WITH_HPROF
            LOGI("SIGUSR1 forcing GC and HPROF dump\n");
            hprofDumpHeap(NULL);
#else
            LOGI("SIGUSR1 forcing GC (no HPROF)\n");
            dvmCollectGarbage(false);
#endif
        } else {
            LOGE("unexpected signal %d\n", rcvd);
        }
    }

    return NULL;
}

