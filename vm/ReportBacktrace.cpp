/*
 * Copyright (C) 2014 The Android Open Source Project
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

#include "Dalvik.h"

#include <unistd.h>
#include <signal.h>
#include <sys/syscall.h>
#include <errno.h>

#include "ReportBacktrace.h"

int sys_sigwaitinfo(const sigset_t *set, siginfo_t *info)
{
    int ret;
    union {
        uint64_t kernel_sigset;
        sigset_t dummy_sigset;
    } u;

    u.kernel_sigset = 0;
    u.dummy_sigset = *set;

    do {
        ret = syscall(__NR_rt_sigtimedwait, &u.dummy_sigset, info,
                      NULL, sizeof(u.kernel_sigset));
    } while ((ret < 0) && (EAGAIN == errno));

    return ret;
}

static int sys_rt_tgsigqueueinfo(pid_t tgid, pid_t tid, int sig, siginfo_t* si)
{
    int ret = syscall(__NR_rt_tgsigqueueinfo, tgid, tid, sig, si);
    return ret;
}

static char* dumpStack2Buf(const char* end, char* buf, Thread *thread)
{
    void* framePtr = thread->interpSave.curFrame;
    if (framePtr != NULL) {
        const char* safe_end = end - 1;
        const StackSaveArea* saveArea;
        const Method* method;
        const u2* currPc = NULL;
        void* saveFramePtr;

        if (!dvmIsBreakFrame((u4*)framePtr)) {
            saveArea = SAVEAREA_FROM_FP(framePtr);
            currPc = saveArea->xtra.currentPc;
        }
        do {
            saveArea = SAVEAREA_FROM_FP(framePtr);
            method = saveArea->method;
            if (!dvmIsBreakFrame((u4*)framePtr)) {
                int len;
                std::string methodName(dvmHumanReadableMethod(method, false));
                const char* methodNameCstr = methodName.c_str();

                if (dvmIsNativeMethod(method)) {
                    len = snprintf(buf, safe_end - buf, "%s(Native)\n",
                                   methodNameCstr);
                } else {
                    const char* file = dvmGetMethodSourceFile(method);
                    int line;
                    int relPc;
                    relPc = (currPc != NULL) ? currPc - method->insns : -1;
                    line = relPc < 0 ? -1 : dvmLineNumFromPC(method, relPc);
                    len = snprintf(buf, safe_end - buf, "%s(%s:%d)\n",
                                   methodNameCstr, file ? file : "NA", line);
                }
                if (len > 0) {
                    buf += len;
                    if (buf >= safe_end) {
                        /* Printed too much, not showing any more */
                        break;
                    }
                }
            }
            currPc = saveArea->savedPc;
            saveFramePtr = framePtr;
            framePtr = saveArea->prevFrame;
        } while ((framePtr != NULL) && (framePtr > saveFramePtr));
    }
    return buf;
}

static char* grabBacktrace(const char* end, char* buf, pid_t tid)
{
    char* bufPtr = buf;

    /*
     * Try best to dump backtrace if it's a VM thread.
     *
     * The thread typically should be paused but not be using CPU.
     */
    if (dvmTryLockThreadList()) {
        Thread *thread = gDvm.threadList;
        for (; thread != NULL; thread = thread->next) {
            if (thread->systemTid == tid) {
                /* VM thread */
                if (0 == dvmTryLockMutex(&gDvm.threadSuspendCountLock)) {
                    /*
                     * By making suspendCount positive, we enable
                     * kSubModeSuspendPending subMode and switch to
                     * altHandlerTable of "thread". Then at the coming
                     * safe-point, "thread" will suspend itself.
                     *
                     * In this case, a safe-point is a point of instruction
                     * which can either branch, or switch, or return. Check
                     * VERIFY_GC_INST_MASK and dvmCheckBefore() for details.
                     */
                    dvmAddToSuspendCounts(thread, 1, 1);

                    /*
                     * As "thread" will suspend at the coming safe-point,
                     * we are sure its VM backtrace won't change. This means
                     * dvmDumpThreadStack() works perfectly here.
                     * Don't bother to call dvmDumpRunningThreadStack()
                     */
                    bufPtr = dumpStack2Buf(end, bufPtr, thread);

                    if (thread->suspendCount > 0) {
                        dvmAddToSuspendCounts(thread, -1, -1);
                    }
                    if (thread->suspendCount == 0) {
                        dvmBroadcastCond(&gDvm.threadSuspendCountCond);
                    }
                    dvmUnlockMutex(&gDvm.threadSuspendCountLock);
                }
                break;
            }
        }
        dvmUnlockThreadList();
    }
    *bufPtr = '\0';
    return bufPtr;
}

static void publishBacktrace(char* buf, pid_t tid)
{
    siginfo_t si;
    sigset_t omask;
    sigset_t mask;
    pid_t mytid = syscall(__NR_gettid);

    si.si_pid = mytid;
    si.si_uid = getuid();
    si.si_signo = SIG_BACKTRACE;
    si.si_errno = 0;
    si.si_code = SI_GOT_BACKTRACE;
    si.si_ptr = buf;

    sigemptyset(&mask);
    sigaddset(&mask, SIG_BACKTRACE);
    pthread_sigmask(SIG_UNBLOCK, &mask, &omask);

    sys_rt_tgsigqueueinfo(getpid(), mytid, si.si_signo, &si);

    pthread_sigmask(SIG_SETMASK, &omask, NULL);
}

int reportBacktrace(siginfo_t *info)
{
    if (info->si_code == SI_GET_BACKTRACE) {
        /* NOTE: Hack si_ptr for the offending thread id */
        pid_t tid = (pid_t)(info->si_ptr);
        Thread* self = dvmThreadSelf();
        char buf[8192];

        if (UNLIKELY(NULL == self)) {
            buf[0] = '\0';
            publishBacktrace(buf, tid);
        } else {
            ThreadStatus oldStatus = dvmChangeStatus(self, THREAD_VMWAIT);
            grabBacktrace(buf + sizeof(buf), buf, tid);
            publishBacktrace(buf, tid);
            dvmChangeStatus(self, oldStatus);
        }
        return 1;
    }
    return 0;
}
