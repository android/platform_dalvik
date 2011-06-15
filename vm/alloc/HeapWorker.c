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
 * An async worker thread to handle certain heap operations that need
 * to be done in a separate thread to avoid synchronization problems.
 * HeapWorkers and reference enqueuing are handled by this thread.
 * The VM does all clearing.
 */
#include "Dalvik.h"
#include "HeapInternal.h"

#include <sys/time.h>
#include <stdlib.h>
#include <pthread.h>
#include <signal.h>
#include <errno.h>  // for ETIMEDOUT, etc.

static void* heapWorkerThreadStart(void* arg);

/*
 * Initialize any HeapWorker state that Heap.c
 * cares about.  This lets the GC start before the
 * HeapWorker thread is initialized.
 */
void dvmInitializeHeapWorkerState()
{
    assert(!gDvm.heapWorkerInitialized);

    dvmInitMutex(&gDvm.heapWorkerLock);
    pthread_cond_init(&gDvm.heapWorkerCond, NULL);
    pthread_cond_init(&gDvm.heapWorkerIdleCond, NULL);

    gDvm.heapWorkerInitialized = true;
}

/*
 * Crank up the heap worker thread.
 *
 * Does not return until the thread is ready for business.
 */
bool dvmHeapWorkerStartup(void)
{
    assert(!gDvm.haltHeapWorker);
    assert(!gDvm.heapWorkerReady);
    assert(gDvm.heapWorkerHandle == 0);
    assert(gDvm.heapWorkerInitialized);

    /* use heapWorkerLock/heapWorkerCond to communicate readiness */
    dvmLockMutex(&gDvm.heapWorkerLock);

//BUG: If a GC happens in here or in the new thread while we hold the lock,
//     the GC will deadlock when trying to acquire heapWorkerLock.
    if (!dvmCreateInternalThread(&gDvm.heapWorkerHandle,
                "HeapWorker", heapWorkerThreadStart, NULL))
    {
        dvmUnlockMutex(&gDvm.heapWorkerLock);
        return false;
    }

    /*
     * Wait for the heap worker to come up.  We know the thread was created,
     * so this should not get stuck.
     */
    while (!gDvm.heapWorkerReady) {
        dvmWaitCond(&gDvm.heapWorkerCond, &gDvm.heapWorkerLock);
    }

    dvmUnlockMutex(&gDvm.heapWorkerLock);
    return true;
}

/*
 * Shut down the heap worker thread if it was started.
 */
void dvmHeapWorkerShutdown(void)
{
    void* threadReturn;

    /* note: assuming that (pthread_t)0 is not a valid thread handle */
    if (gDvm.heapWorkerHandle != 0) {
        gDvm.haltHeapWorker = true;
        dvmSignalHeapWorker(true);

        /*
         * We may not want to wait for the heapWorkers to complete.  It's
         * a good idea to do so, in case they're holding some sort of OS
         * resource that doesn't get reclaimed when the process exits
         * (e.g. an open temp file).
         */
        if (pthread_join(gDvm.heapWorkerHandle, &threadReturn) != 0)
            LOGW("HeapWorker thread join failed\n");
        else if (gDvm.verboseShutdown)
            LOGD("HeapWorker thread has shut down\n");

        gDvm.heapWorkerReady = false;
    }
}

/* Make sure that the HeapWorker thread hasn't spent an inordinate
 * amount of time inside a finalizer.
 *
 * Aborts the VM if the thread appears to be wedged.
 *
 * The caller must hold the heapWorkerLock to guarantee an atomic
 * read of the watchdog values.
 */
void dvmAssertHeapWorkerThreadRunning()
{
    if (gDvm.gcHeap->heapWorkerCurrentObject != NULL) {
        static const u8 HEAP_WORKER_WATCHDOG_TIMEOUT = 10*1000*1000LL; // 10sec

        u8 heapWorkerInterpStartTime = gDvm.gcHeap->heapWorkerInterpStartTime;
        u8 now = dvmGetRelativeTimeUsec();
        u8 delta = now - heapWorkerInterpStartTime;

        if (delta > HEAP_WORKER_WATCHDOG_TIMEOUT &&
            (gDvm.debuggerActive || gDvm.nativeDebuggerActive))
        {
            /*
             * Debugger suspension can block the thread indefinitely.  For
             * best results we should reset this explicitly whenever the
             * HeapWorker thread is resumed.  Unfortunately this is also
             * affected by native debuggers, and we have no visibility
             * into how they're manipulating us.  So, we ignore the
             * watchdog and just reset the timer.
             */
            LOGI("Debugger is attached -- suppressing HeapWorker watchdog\n");
            gDvm.gcHeap->heapWorkerInterpStartTime = now;   /* reset timer */
        } else if (delta > HEAP_WORKER_WATCHDOG_TIMEOUT) {
            /*
             * Before we give up entirely, see if maybe we're just not
             * getting any CPU time because we're stuck in a background
             * process group.  If we successfully move the thread into the
             * foreground we'll just leave it there (it doesn't do anything
             * if the process isn't GCing).
             */
            dvmLockThreadList(NULL);
            Thread* thread = dvmGetThreadByHandle(gDvm.heapWorkerHandle);
            dvmUnlockThreadList();

            if (thread != NULL) {
                int priChangeFlags, threadPrio;
                SchedPolicy threadPolicy;
                priChangeFlags = dvmRaiseThreadPriorityIfNeeded(thread,
                        &threadPrio, &threadPolicy);
                if (priChangeFlags != 0) {
                    LOGI("HeapWorker watchdog expired, raising priority"
                         " and retrying\n");
                    gDvm.gcHeap->heapWorkerInterpStartTime = now;
                    return;
                }
            }

            char* desc = dexProtoCopyMethodDescriptor(
                    &gDvm.gcHeap->heapWorkerCurrentMethod->prototype);
            LOGE("HeapWorker is wedged: %lldms spent inside %s.%s%s\n",
                    delta / 1000,
                    gDvm.gcHeap->heapWorkerCurrentObject->clazz->descriptor,
                    gDvm.gcHeap->heapWorkerCurrentMethod->name, desc);
            free(desc);
            dvmDumpAllThreads(true);

            /* try to get a debuggerd dump from the target thread */
            dvmNukeThread(thread);

            /* abort the VM */
            dvmAbort();
        } else if (delta > HEAP_WORKER_WATCHDOG_TIMEOUT / 2) {
            char* desc = dexProtoCopyMethodDescriptor(
                    &gDvm.gcHeap->heapWorkerCurrentMethod->prototype);
            LOGW("HeapWorker may be wedged: %lldms spent inside %s.%s%s\n",
                    delta / 1000,
                    gDvm.gcHeap->heapWorkerCurrentObject->clazz->descriptor,
                    gDvm.gcHeap->heapWorkerCurrentMethod->name, desc);
            free(desc);
        }
    }
}

/*
 * Acquires a mutex, transitioning to the VMWAIT state if the mutex is
 * held.  This allows the thread to suspend while it waits for another
 * thread to release the mutex.
 */
static void lockMutex(pthread_mutex_t *mu)
{
    Thread *self;
    ThreadStatus oldStatus;

    assert(mu != NULL);
    if (dvmTryLockMutex(mu) != 0) {
        self = dvmThreadSelf();
        assert(self != NULL);
        oldStatus = dvmChangeStatus(self, THREAD_VMWAIT);
        dvmLockMutex(mu);
        if (oldStatus == THREAD_RUNNING) {
            while(!dvmChangeStatusToRunning(self) ) {
                /*
                 * It was not possible to change status due to suspension
                 * pending. Release the lock while we suspend.
                 */
                dvmUnlockMutex(mu);
                dvmCheckSuspendPending(self);
                dvmLockMutex(mu);
            }
        } else {
            dvmChangeStatus(self, oldStatus);
        }

    }
}

static void callMethod(Thread *self, Object *obj, Method *method)
{
    JValue unused;

    /* Keep track of the method we're about to call and
     * the current time so that other threads can detect
     * when this thread wedges and provide useful information.
     */
    gDvm.gcHeap->heapWorkerInterpStartTime = dvmGetRelativeTimeUsec();
    gDvm.gcHeap->heapWorkerInterpCpuStartTime = dvmGetThreadCpuTimeUsec();
    gDvm.gcHeap->heapWorkerCurrentMethod = method;
    gDvm.gcHeap->heapWorkerCurrentObject = obj;

    /* Call the method.
     *
     * Don't hold the lock when executing interpreted
     * code.  It may suspend, and the GC needs to grab
     * heapWorkerLock.
     */
    dvmUnlockMutex(&gDvm.heapWorkerLock);
    if (false) {
        /* Log entry/exit; this will likely flood the log enough to
         * cause "logcat" to drop entries.
         */
        char tmpTag[16];
        sprintf(tmpTag, "HW%d", self->systemTid);
        LOG(LOG_DEBUG, tmpTag, "Call %s\n", method->clazz->descriptor);
        dvmCallMethod(self, method, obj, &unused);
        LOG(LOG_DEBUG, tmpTag, " done\n");
    } else {
        dvmCallMethod(self, method, obj, &unused);
    }
    /*
     * Reacquire the heap worker lock in a suspend-friendly way.
     */
    lockMutex(&gDvm.heapWorkerLock);

    gDvm.gcHeap->heapWorkerCurrentObject = NULL;
    gDvm.gcHeap->heapWorkerCurrentMethod = NULL;
    gDvm.gcHeap->heapWorkerInterpStartTime = 0LL;

    /* Exceptions thrown during these calls interrupt
     * the method, but are otherwise ignored.
     */
    if (dvmCheckException(self)) {
#if DVM_SHOW_EXCEPTION >= 1
        LOGI("Uncaught exception thrown by finalizer (will be discarded):\n");
        dvmLogExceptionStackTrace();
#endif
        dvmClearException(self);
    }
}

/* Process all enqueued heap work, including finalizers and reference
 * enqueueing. Clearing has already been done by the VM.
 *
 * Caller must hold gDvm.heapWorkerLock.
 */
static void doHeapWork(Thread *self)
{
    Object *obj;
    HeapWorkerOperation op;
    int numFinalizersCalled, numReferencesEnqueued;

    assert(gDvm.voffJavaLangObject_finalize >= 0);
    assert(gDvm.methJavaLangRefReference_enqueueInternal != NULL);

    numFinalizersCalled = 0;
    numReferencesEnqueued = 0;
    while ((obj = dvmGetNextHeapWorkerObject(&op)) != NULL) {
        Method *method = NULL;

        /* Make sure the object hasn't been collected since
         * being scheduled.
         */
        assert(dvmIsValidObject(obj));

        /* Call the appropriate method(s).
         */
        if (op == WORKER_FINALIZE) {
            numFinalizersCalled++;
            method = obj->clazz->vtable[gDvm.voffJavaLangObject_finalize];
            assert(dvmCompareNameDescriptorAndMethod("finalize", "()V",
                            method) == 0);
            assert(method->clazz != gDvm.classJavaLangObject);
            callMethod(self, obj, method);
        } else {
            assert(op == WORKER_ENQUEUE);
            assert(dvmGetFieldObject(
                       obj, gDvm.offJavaLangRefReference_queue) != NULL);
            assert(dvmGetFieldObject(
                       obj, gDvm.offJavaLangRefReference_queueNext) == NULL);
            numReferencesEnqueued++;
            callMethod(self, obj,
                       gDvm.methJavaLangRefReference_enqueueInternal);
        }

        /* Let the GC collect the object.
         */
        dvmReleaseTrackedAlloc(obj, self);
    }
    LOGV("Called %d finalizers\n", numFinalizersCalled);
    LOGV("Enqueued %d references\n", numReferencesEnqueued);
}

/*
 * The heap worker thread sits quietly until the GC tells it there's work
 * to do.
 */
static void* heapWorkerThreadStart(void* arg)
{
    Thread *self = dvmThreadSelf();

    UNUSED_PARAMETER(arg);

    LOGV("HeapWorker thread started (threadid=%d)\n", self->threadId);

    /* tell the main thread that we're ready */
    lockMutex(&gDvm.heapWorkerLock);
    gDvm.heapWorkerReady = true;
    dvmSignalCond(&gDvm.heapWorkerCond);
    dvmUnlockMutex(&gDvm.heapWorkerLock);

    lockMutex(&gDvm.heapWorkerLock);
    while (!gDvm.haltHeapWorker) {
        struct timespec trimtime;
        bool timedwait = false;

        /* We're done running interpreted code for now. */
        dvmChangeStatus(NULL, THREAD_VMWAIT);

        /* Signal anyone who wants to know when we're done. */
        dvmBroadcastCond(&gDvm.heapWorkerIdleCond);

        /* Trim the heap if we were asked to. */
        trimtime = gDvm.gcHeap->heapWorkerNextTrim;
        if (trimtime.tv_sec != 0 && trimtime.tv_nsec != 0) {
            struct timespec now;

#ifdef HAVE_TIMEDWAIT_MONOTONIC
            clock_gettime(CLOCK_MONOTONIC, &now);       // relative time
#else
            struct timeval tvnow;
            gettimeofday(&tvnow, NULL);                 // absolute time
            now.tv_sec = tvnow.tv_sec;
            now.tv_nsec = tvnow.tv_usec * 1000;
#endif

            if (trimtime.tv_sec < now.tv_sec ||
                (trimtime.tv_sec == now.tv_sec &&
                 trimtime.tv_nsec <= now.tv_nsec))
            {
                size_t madvisedSizes[HEAP_SOURCE_MAX_HEAP_COUNT];

                /*
                 * Acquire the gcHeapLock.  The requires releasing the
                 * heapWorkerLock before the gcHeapLock is acquired.
                 * It is possible that the gcHeapLock may be acquired
                 * during a concurrent GC in which case heapWorkerLock
                 * is held by the GC and we are unable to make forward
                 * progress.  We avoid deadlock by releasing the
                 * gcHeapLock and then waiting to be signaled when the
                 * GC completes.  There is no guarantee that the next
                 * time we are run will coincide with GC inactivity so
                 * the check and wait must be performed within a loop.
                 */
                dvmUnlockMutex(&gDvm.heapWorkerLock);
                dvmLockHeap();
                while (gDvm.gcHeap->gcRunning) {
                    dvmWaitForConcurrentGcToComplete();
                }
                dvmLockMutex(&gDvm.heapWorkerLock);

                memset(madvisedSizes, 0, sizeof(madvisedSizes));
                dvmHeapSourceTrim(madvisedSizes, HEAP_SOURCE_MAX_HEAP_COUNT);
                dvmLogMadviseStats(madvisedSizes, HEAP_SOURCE_MAX_HEAP_COUNT);

                dvmUnlockHeap();

                trimtime.tv_sec = 0;
                trimtime.tv_nsec = 0;
                gDvm.gcHeap->heapWorkerNextTrim = trimtime;
            } else {
                timedwait = true;
            }
        }

        /* sleep until signaled */
        if (timedwait) {
            int cc __attribute__ ((__unused__));
#ifdef HAVE_TIMEDWAIT_MONOTONIC
            cc = pthread_cond_timedwait_monotonic(&gDvm.heapWorkerCond,
                    &gDvm.heapWorkerLock, &trimtime);
#else
            cc = pthread_cond_timedwait(&gDvm.heapWorkerCond,
                    &gDvm.heapWorkerLock, &trimtime);
#endif
            assert(cc == 0 || cc == ETIMEDOUT);
        } else {
            dvmWaitCond(&gDvm.heapWorkerCond, &gDvm.heapWorkerLock);
        }

        /*
         * Return to the running state before doing heap work.  This
         * will block if the GC has initiated a suspend.  We release
         * the heapWorkerLock beforehand for the GC to make progress
         * and wait to be signaled after the GC completes.  There is
         * no guarantee that the next time we are run will coincide
         * with GC inactivity so the check and wait must be performed
         * within a loop.
         */
        dvmUnlockMutex(&gDvm.heapWorkerLock);
        dvmChangeStatus(NULL, THREAD_RUNNING);
        dvmLockHeap();
        while (gDvm.gcHeap->gcRunning) {
            dvmWaitForConcurrentGcToComplete();
        }
        dvmLockMutex(&gDvm.heapWorkerLock);
        dvmUnlockHeap();
        LOGV("HeapWorker is awake\n");

        /* Process any events in the queue.
         */
        doHeapWork(self);
    }
    dvmUnlockMutex(&gDvm.heapWorkerLock);

    if (gDvm.verboseShutdown)
        LOGD("HeapWorker thread shutting down\n");
    return NULL;
}

/*
 * Wake up the heap worker to let it know that there's work to be done.
 */
void dvmSignalHeapWorker(bool shouldLock)
{
    if (shouldLock) {
        dvmLockMutex(&gDvm.heapWorkerLock);
    }

    dvmSignalCond(&gDvm.heapWorkerCond);

    if (shouldLock) {
        dvmUnlockMutex(&gDvm.heapWorkerLock);
    }
}

/*
 * Block until all pending heap worker work has finished.
 */
void dvmWaitForHeapWorkerIdle()
{
    assert(gDvm.heapWorkerReady);

    dvmChangeStatus(NULL, THREAD_VMWAIT);

    dvmLockMutex(&gDvm.heapWorkerLock);

    /* Wake up the heap worker and wait for it to finish. */
    //TODO(http://b/issue?id=699704): This will deadlock if
    //     called from finalize(), enqueue(), or clear().  We
    //     need to detect when this is called from the HeapWorker
    //     context and just give up.
    dvmSignalHeapWorker(false);
    dvmWaitCond(&gDvm.heapWorkerIdleCond, &gDvm.heapWorkerLock);

    dvmUnlockMutex(&gDvm.heapWorkerLock);

    dvmChangeStatus(NULL, THREAD_RUNNING);
}

/*
 * Do not return until any pending heap work has finished.  This may
 * or may not happen in the context of the calling thread.
 * No exceptions will escape.
 */
void dvmRunFinalizationSync()
{
    if (gDvm.zygote) {
        assert(!gDvm.heapWorkerReady);

        /* When in zygote mode, there is no heap worker.
         * Do the work in the current thread.
         */
        dvmLockMutex(&gDvm.heapWorkerLock);
        doHeapWork(dvmThreadSelf());
        dvmUnlockMutex(&gDvm.heapWorkerLock);
    } else {
        /* Outside of zygote mode, we can just ask the
         * heap worker thread to do the work.
         */
        dvmWaitForHeapWorkerIdle();
    }
}

/*
 * Requests that dvmHeapSourceTrim() be called no sooner
 * than timeoutSec seconds from now.  If timeoutSec
 * is zero, any pending trim is cancelled.
 *
 * Caller must hold heapWorkerLock.
 */
void dvmScheduleHeapSourceTrim(size_t timeoutSec)
{
    GcHeap *gcHeap = gDvm.gcHeap;
    struct timespec timeout;

    if (timeoutSec == 0) {
        timeout.tv_sec = 0;
        timeout.tv_nsec = 0;
        /* Don't wake up the thread just to tell it to cancel.
         * If it wakes up naturally, we can avoid the extra
         * context switch.
         */
    } else {
#ifdef HAVE_TIMEDWAIT_MONOTONIC
        clock_gettime(CLOCK_MONOTONIC, &timeout);
        timeout.tv_sec += timeoutSec;
#else
        struct timeval now;
        gettimeofday(&now, NULL);
        timeout.tv_sec = now.tv_sec + timeoutSec;
        timeout.tv_nsec = now.tv_usec * 1000;
#endif
        dvmSignalHeapWorker(false);
    }
    gcHeap->heapWorkerNextTrim = timeout;
}
