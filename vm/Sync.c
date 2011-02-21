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
 * Fundamental synchronization mechanisms.
 *
 * The top part of the file has operations on "monitor" structs; the
 * next part has the native calls on objects.
 *
 * The current implementation uses "thin locking" to avoid allocating
 * an Object's full Monitor struct until absolutely necessary (i.e.,
 * during contention or a call to wait()).
 *
 * TODO: make improvements to thin locking
 * We may be able to improve performance and reduce memory requirements by:
 *  - reverting to a thin lock once the Monitor is no longer necessary
 *  - using a pool of monitor objects, with some sort of recycling scheme
 *
 * TODO: recycle native-level monitors when objects are garbage collected.
 */
#include "Dalvik.h"

#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>
#include <sys/time.h>
#include <errno.h>

#define LOG_THIN    LOGV

#ifdef WITH_DEADLOCK_PREDICTION     /* fwd */
static const char* kStartBanner =
    "<-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#";
static const char* kEndBanner =
    "#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#->";

/*
 * Unsorted, expanding list of objects.
 *
 * This is very similar to PointerSet (which came into existence after this),
 * but these are unsorted, uniqueness is not enforced by the "add" function,
 * and the base object isn't allocated on the heap.
 */
typedef struct ExpandingObjectList {
    u2          alloc;
    u2          count;
    Object**    list;
} ExpandingObjectList;

/* fwd */
static void updateDeadlockPrediction(Thread* self, Object* obj);
static void removeCollectedObject(Object* obj);
static void expandObjClear(ExpandingObjectList* pList);
#endif

/*
 * Every Object has a monitor associated with it, but not every Object is
 * actually locked.  Even the ones that are locked do not need a
 * full-fledged monitor until a) there is actual contention or b) wait()
 * is called on the Object.
 *
 * For Dalvik, we have implemented a scheme similar to the one described
 * in Bacon et al.'s "Thin locks: featherweight synchronization for Java"
 * (ACM 1998).  Things are even easier for us, though, because we have
 * a full 32 bits to work with.
 *
 * The two states of an Object's lock are referred to as "thin" and
 * "fat".  A lock may transition from the "thin" state to the "fat"
 * state and this transition is referred to as inflation.  Once a lock
 * has been inflated it remains in the "fat" state indefinitely.
 *
 * The lock value itself is stored in Object.lock.  The LSB of the
 * lock encodes its state.  When cleared, the lock is in the "thin"
 * state and its bits are formatted as follows:
 *
 *    [31 ---- 19] [18 ---- 3] [2 ---- 1] [0]
 *     lock count   thread id  hash state  0
 *
 * When set, the lock is in the "fat" state and its bits are formatted
 * as follows:
 *
 *    [31 ---- 3] [2 ---- 1] [0]
 *      pointer   hash state  1
 *
 * For an in-depth description of the mechanics of thin-vs-fat locking,
 * read the paper referred to above.
 */

/*
 * Monitors provide:
 *  - mutually exclusive access to resources
 *  - a way for multiple threads to wait for notification
 *
 * In effect, they fill the role of both mutexes and condition variables.
 *
 * Only one thread can own the monitor at any time.  There may be several
 * threads waiting on it (the wait call unlocks it).  One or more waiting
 * threads may be getting interrupted or notified at any given time.
 *
 * TODO: the various members of monitor are not SMP-safe.
 */
struct Monitor {
    Thread*     owner;          /* which thread currently owns the lock? */
    int         lockCount;      /* owner's recursive lock depth */
    Object*     obj;            /* what object are we part of [debug only] */

    Thread*     waitSet;	/* threads currently waiting on this monitor */

    pthread_mutex_t lock;

    Monitor*    next;

    /*
     * Who last acquired this monitor, when lock sampling is enabled.
     * Even when enabled, ownerFileName may be NULL.
     */
    char*       ownerFileName;
    u4          ownerLineNumber;

#ifdef WITH_DEADLOCK_PREDICTION
    /*
     * Objects that have been locked immediately after this one in the
     * past.  We use an expanding flat array, allocated on first use, to
     * minimize allocations.  Deletions from the list, expected to be
     * infrequent, are crunched down.
     */
    ExpandingObjectList historyChildren;

    /*
     * We also track parents.  This isn't strictly necessary, but it makes
     * the cleanup at GC time significantly faster.
     */
    ExpandingObjectList historyParents;

    /* used during cycle detection */
    bool        historyMark;

    /* stack trace, established the first time we locked the object */
    int         historyStackDepth;
    int*        historyRawStackTrace;
#endif
};


/*
 * Create and initialize a monitor.
 */
Monitor* dvmCreateMonitor(Object* obj)
{
    Monitor* mon;

    mon = (Monitor*) calloc(1, sizeof(Monitor));
    if (mon == NULL) {
        LOGE("Unable to allocate monitor\n");
        dvmAbort();
    }
    if (((u4)mon & 7) != 0) {
        LOGE("Misaligned monitor: %p\n", mon);
        dvmAbort();
    }
    mon->obj = obj;
    dvmInitMutex(&mon->lock);

    /* replace the head of the list with the new monitor */
    do {
        mon->next = gDvm.monitorList;
    } while (android_atomic_release_cas((int32_t)mon->next, (int32_t)mon,
            (int32_t*)(void*)&gDvm.monitorList) != 0);

    return mon;
}

/*
 * Free the monitor list.  Only used when shutting the VM down.
 */
void dvmFreeMonitorList(void)
{
    Monitor* mon;
    Monitor* nextMon;

    mon = gDvm.monitorList;
    while (mon != NULL) {
        nextMon = mon->next;

#ifdef WITH_DEADLOCK_PREDICTION
        expandObjClear(&mon->historyChildren);
        expandObjClear(&mon->historyParents);
        free(mon->historyRawStackTrace);
#endif
        free(mon);
        mon = nextMon;
    }
}

/*
 * Log some info about our monitors.
 */
void dvmDumpMonitorInfo(const char* msg)
{
#if QUIET_ZYGOTE_MONITOR
    if (gDvm.zygote) {
        return;
    }
#endif

    int totalCount;
    int liveCount;

    totalCount = liveCount = 0;
    Monitor* mon = gDvm.monitorList;
    while (mon != NULL) {
        totalCount++;
        if (mon->obj != NULL)
            liveCount++;
        mon = mon->next;
    }

    LOGD("%s: monitor list has %d entries (%d live)\n",
        msg, totalCount, liveCount);
}

/*
 * Get the object that a monitor is part of.
 */
Object* dvmGetMonitorObject(Monitor* mon)
{
    if (mon == NULL)
        return NULL;
    else
        return mon->obj;
}

/*
 * Returns the thread id of the thread owning the given lock.
 */
static u4 lockOwner(Object* obj)
{
    Thread *owner;
    u4 lock;

    assert(obj != NULL);
    /*
     * Since we're reading the lock value multiple times, latch it so
     * that it doesn't change out from under us if we get preempted.
     */
    lock = obj->lock;
    if (LW_SHAPE(lock) == LW_SHAPE_THIN) {
        return LW_LOCK_OWNER(lock);
    } else {
        owner = LW_MONITOR(lock)->owner;
        return owner ? owner->threadId : 0;
    }
}

/*
 * Get the thread that holds the lock on the specified object.  The
 * object may be unlocked, thin-locked, or fat-locked.
 *
 * The caller must lock the thread list before calling here.
 */
Thread* dvmGetObjectLockHolder(Object* obj)
{
    u4 threadId = lockOwner(obj);

    if (threadId == 0)
        return NULL;
    return dvmGetThreadByThreadId(threadId);
}

/*
 * Checks whether the given thread holds the given
 * objects's lock.
 */
bool dvmHoldsLock(Thread* thread, Object* obj)
{
    if (thread == NULL || obj == NULL) {
        return false;
    } else {
        return thread->threadId == lockOwner(obj);
    }
}

/*
 * Free the monitor associated with an object and make the object's lock
 * thin again.  This is called during garbage collection.
 */
static void freeObjectMonitor(Object* obj)
{
    Monitor *mon;

    assert(LW_SHAPE(obj->lock) == LW_SHAPE_FAT);

#ifdef WITH_DEADLOCK_PREDICTION
    if (gDvm.deadlockPredictMode != kDPOff)
        removeCollectedObject(obj);
#endif

    mon = LW_MONITOR(obj->lock);
    obj->lock = DVM_LOCK_INITIAL_THIN_VALUE;

    /* This lock is associated with an object
     * that's being swept.  The only possible way
     * anyone could be holding this lock would be
     * if some JNI code locked but didn't unlock
     * the object, in which case we've got some bad
     * native code somewhere.
     */
    assert(pthread_mutex_trylock(&mon->lock) == 0);
    assert(pthread_mutex_unlock(&mon->lock) == 0);
    dvmDestroyMutex(&mon->lock);
#ifdef WITH_DEADLOCK_PREDICTION
    expandObjClear(&mon->historyChildren);
    expandObjClear(&mon->historyParents);
    free(mon->historyRawStackTrace);
#endif
    free(mon);
}

/*
 * Frees monitor objects belonging to unmarked objects.
 */
void dvmSweepMonitorList(Monitor** mon, int (*isUnmarkedObject)(void*))
{
    Monitor handle;
    Monitor *prev, *curr;
    Object *obj;

    assert(mon != NULL);
    assert(isUnmarkedObject != NULL);
#ifdef WITH_DEADLOCK_PREDICTION
    dvmDumpMonitorInfo("before monitor sweep");
#endif
    prev = &handle;
    prev->next = curr = *mon;
    while (curr != NULL) {
        obj = curr->obj;
        if (obj != NULL && (*isUnmarkedObject)(obj) != 0) {
            prev->next = curr = curr->next;
            freeObjectMonitor(obj);
        } else {
            prev = curr;
            curr = curr->next;
        }
    }
    *mon = handle.next;
#ifdef WITH_DEADLOCK_PREDICTION
    dvmDumpMonitorInfo("after monitor sweep");
#endif
}

static char *logWriteInt(char *dst, int value)
{
    *dst++ = EVENT_TYPE_INT;
    set4LE((u1 *)dst, value);
    return dst + 4;
}

static char *logWriteString(char *dst, const char *value, size_t len)
{
    *dst++ = EVENT_TYPE_STRING;
    len = len < 32 ? len : 32;
    set4LE((u1 *)dst, len);
    dst += 4;
    memcpy(dst, value, len);
    return dst + len;
}

#define EVENT_LOG_TAG_dvm_lock_sample 20003

static void logContentionEvent(Thread *self, u4 waitMs, u4 samplePercent,
                               const char *ownerFileName, u4 ownerLineNumber)
{
    const StackSaveArea *saveArea;
    const Method *meth;
    u4 relativePc;
    char eventBuffer[174];
    const char *fileName;
    char procName[33], *selfName;
    char *cp;
    size_t len;
    int fd;

    saveArea = SAVEAREA_FROM_FP(self->curFrame);
    meth = saveArea->method;
    cp = eventBuffer;

    /* Emit the event list length, 1 byte. */
    *cp++ = 9;

    /* Emit the process name, <= 37 bytes. */
    fd = open("/proc/self/cmdline", O_RDONLY);
    memset(procName, 0, sizeof(procName));
    read(fd, procName, sizeof(procName) - 1);
    close(fd);
    len = strlen(procName);
    cp = logWriteString(cp, procName, len);

    /* Emit the main thread status, 5 bytes. */
    bool isMainThread = (self->systemTid == getpid());
    cp = logWriteInt(cp, isMainThread);

    /* Emit self thread name string, <= 37 bytes. */
    selfName = dvmGetThreadName(self);
    cp = logWriteString(cp, selfName, strlen(selfName));
    free(selfName);

    /* Emit the wait time, 5 bytes. */
    cp = logWriteInt(cp, waitMs);

    /* Emit the source code file name, <= 37 bytes. */
    fileName = dvmGetMethodSourceFile(meth);
    if (fileName == NULL) fileName = "";
    cp = logWriteString(cp, fileName, strlen(fileName));

    /* Emit the source code line number, 5 bytes. */
    relativePc = saveArea->xtra.currentPc - saveArea->method->insns;
    cp = logWriteInt(cp, dvmLineNumFromPC(meth, relativePc));

    /* Emit the lock owner source code file name, <= 37 bytes. */
    if (ownerFileName == NULL) {
      ownerFileName = "";
    } else if (strcmp(fileName, ownerFileName) == 0) {
      /* Common case, so save on log space. */
      ownerFileName = "-";
    }
    cp = logWriteString(cp, ownerFileName, strlen(ownerFileName));

    /* Emit the source code line number, 5 bytes. */
    cp = logWriteInt(cp, ownerLineNumber);

    /* Emit the sample percentage, 5 bytes. */
    cp = logWriteInt(cp, samplePercent);

    assert((size_t)(cp - eventBuffer) <= sizeof(eventBuffer));
    android_btWriteLog(EVENT_LOG_TAG_dvm_lock_sample,
                       EVENT_TYPE_LIST,
                       eventBuffer,
                       (size_t)(cp - eventBuffer));
}

/*
 * Lock a monitor.
 */
static void lockMonitor(Thread* self, Monitor* mon)
{
    ThreadStatus oldStatus;
    u4 waitThreshold, samplePercent;
    u8 waitStart, waitEnd, waitMs;

    if (mon->owner == self) {
        mon->lockCount++;
        return;
    }
    if (dvmTryLockMutex(&mon->lock) != 0) {
        oldStatus = dvmChangeStatus(self, THREAD_MONITOR);
        waitThreshold = gDvm.lockProfThreshold;
        if (waitThreshold) {
            waitStart = dvmGetRelativeTimeUsec();
        }
        const char* currentOwnerFileName = mon->ownerFileName;
        u4 currentOwnerLineNumber = mon->ownerLineNumber;

        dvmLockMutex(&mon->lock);
        if (waitThreshold) {
            waitEnd = dvmGetRelativeTimeUsec();
        }
        dvmChangeStatus(self, oldStatus);
        if (waitThreshold && self->curFrame != NULL) {
            waitMs = (waitEnd - waitStart) / 1000;
            if (waitMs >= waitThreshold) {
                samplePercent = 100;
            } else {
                samplePercent = 100 * waitMs / waitThreshold;
            }
            if (samplePercent != 0 && ((u4)rand() % 100 < samplePercent)) {
                logContentionEvent(self, waitMs, samplePercent,
                                   currentOwnerFileName, currentOwnerLineNumber);
            }
        }
    }
    mon->owner = self;
    assert(mon->lockCount == 0);

    // When debugging, save the current monitor holder for future
    // acquisition failures to use in sampled logging.
    if (gDvm.lockProfThreshold > 0) {
        const StackSaveArea *saveArea;
        const Method *meth;
        mon->ownerLineNumber = 0;
        if (self->curFrame == NULL) {
            mon->ownerFileName = "no_frame";
        } else if ((saveArea = SAVEAREA_FROM_FP(self->curFrame)) == NULL) {
            mon->ownerFileName = "no_save_area";
        } else if ((meth = saveArea->method) == NULL) {
            mon->ownerFileName = "no_method";
        } else {
            u4 relativePc = saveArea->xtra.currentPc - saveArea->method->insns;
            mon->ownerFileName = (char*) dvmGetMethodSourceFile(meth);
            if (mon->ownerFileName == NULL) {
                mon->ownerFileName = "no_method_file";
            } else {
                mon->ownerLineNumber = dvmLineNumFromPC(meth, relativePc);
            }
        }
    }
}

/*
 * Try to lock a monitor.
 *
 * Returns "true" on success.
 */
#ifdef WITH_COPYING_GC
static bool tryLockMonitor(Thread* self, Monitor* mon)
{
    if (mon->owner == self) {
        mon->lockCount++;
        return true;
    } else {
        if (dvmTryLockMutex(&mon->lock) == 0) {
            mon->owner = self;
            assert(mon->lockCount == 0);
            return true;
        } else {
            return false;
        }
    }
}
#endif

/*
 * Unlock a monitor.
 *
 * Returns true if the unlock succeeded.
 * If the unlock failed, an exception will be pending.
 */
static bool unlockMonitor(Thread* self, Monitor* mon)
{
    assert(self != NULL);
    assert(mon != NULL);
    if (mon->owner == self) {
        /*
         * We own the monitor, so nobody else can be in here.
         */
        if (mon->lockCount == 0) {
            mon->owner = NULL;
            mon->ownerFileName = "unlocked";
            mon->ownerLineNumber = 0;
            dvmUnlockMutex(&mon->lock);
        } else {
            mon->lockCount--;
        }
    } else {
        /*
         * We don't own this, so we're not allowed to unlock it.
         * The JNI spec says that we should throw IllegalMonitorStateException
         * in this case.
         */
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                          "unlock of unowned monitor");
        return false;
    }
    return true;
}

/*
 * Checks the wait set for circular structure.  Returns 0 if the list
 * is not circular.  Otherwise, returns 1.  Used only by asserts.
 */
#ifndef NDEBUG
static int waitSetCheck(Monitor *mon)
{
    Thread *fast, *slow;
    size_t n;

    assert(mon != NULL);
    fast = slow = mon->waitSet;
    n = 0;
    for (;;) {
        if (fast == NULL) return 0;
        if (fast->waitNext == NULL) return 0;
        if (fast == slow && n > 0) return 1;
        n += 2;
        fast = fast->waitNext->waitNext;
        slow = slow->waitNext;
    }
}
#endif

/*
 * Links a thread into a monitor's wait set.  The monitor lock must be
 * held by the caller of this routine.
 */
static void waitSetAppend(Monitor *mon, Thread *thread)
{
    Thread *elt;

    assert(mon != NULL);
    assert(mon->owner == dvmThreadSelf());
    assert(thread != NULL);
    assert(thread->waitNext == NULL);
    assert(waitSetCheck(mon) == 0);
    if (mon->waitSet == NULL) {
        mon->waitSet = thread;
        return;
    }
    elt = mon->waitSet;
    while (elt->waitNext != NULL) {
        elt = elt->waitNext;
    }
    elt->waitNext = thread;
}

/*
 * Unlinks a thread from a monitor's wait set.  The monitor lock must
 * be held by the caller of this routine.
 */
static void waitSetRemove(Monitor *mon, Thread *thread)
{
    Thread *elt;

    assert(mon != NULL);
    assert(mon->owner == dvmThreadSelf());
    assert(thread != NULL);
    assert(waitSetCheck(mon) == 0);
    if (mon->waitSet == NULL) {
        return;
    }
    if (mon->waitSet == thread) {
        mon->waitSet = thread->waitNext;
        thread->waitNext = NULL;
        return;
    }
    elt = mon->waitSet;
    while (elt->waitNext != NULL) {
        if (elt->waitNext == thread) {
            elt->waitNext = thread->waitNext;
            thread->waitNext = NULL;
            return;
        }
        elt = elt->waitNext;
    }
}

/*
 * Converts the given relative waiting time into an absolute time.
 */
void absoluteTime(s8 msec, s4 nsec, struct timespec *ts)
{
    s8 endSec;

#ifdef HAVE_TIMEDWAIT_MONOTONIC
    clock_gettime(CLOCK_MONOTONIC, ts);
#else
    {
        struct timeval tv;
        gettimeofday(&tv, NULL);
        ts->tv_sec = tv.tv_sec;
        ts->tv_nsec = tv.tv_usec * 1000;
    }
#endif
    endSec = ts->tv_sec + msec / 1000;
    if (endSec >= 0x7fffffff) {
        LOGV("NOTE: end time exceeds epoch\n");
        endSec = 0x7ffffffe;
    }
    ts->tv_sec = endSec;
    ts->tv_nsec = (ts->tv_nsec + (msec % 1000) * 1000000) + nsec;

    /* catch rollover */
    if (ts->tv_nsec >= 1000000000L) {
        ts->tv_sec++;
        ts->tv_nsec -= 1000000000L;
    }
}

int dvmRelativeCondWait(pthread_cond_t* cond, pthread_mutex_t* mutex,
                        s8 msec, s4 nsec)
{
    int ret;
    struct timespec ts;
    absoluteTime(msec, nsec, &ts);
#if defined(HAVE_TIMEDWAIT_MONOTONIC)
    ret = pthread_cond_timedwait_monotonic(cond, mutex, &ts);
#else
    ret = pthread_cond_timedwait(cond, mutex, &ts);
#endif
    assert(ret == 0 || ret == ETIMEDOUT);
    return ret;
}

/*
 * Wait on a monitor until timeout, interrupt, or notification.  Used for
 * Object.wait() and (somewhat indirectly) Thread.sleep() and Thread.join().
 *
 * If another thread calls Thread.interrupt(), we throw InterruptedException
 * and return immediately if one of the following are true:
 *  - blocked in wait(), wait(long), or wait(long, int) methods of Object
 *  - blocked in join(), join(long), or join(long, int) methods of Thread
 *  - blocked in sleep(long), or sleep(long, int) methods of Thread
 * Otherwise, we set the "interrupted" flag.
 *
 * Checks to make sure that "nsec" is in the range 0-999999
 * (i.e. fractions of a millisecond) and throws the appropriate
 * exception if it isn't.
 *
 * The spec allows "spurious wakeups", and recommends that all code using
 * Object.wait() do so in a loop.  This appears to derive from concerns
 * about pthread_cond_wait() on multiprocessor systems.  Some commentary
 * on the web casts doubt on whether these can/should occur.
 *
 * Since we're allowed to wake up "early", we clamp extremely long durations
 * to return at the end of the 32-bit time epoch.
 */
static void waitMonitor(Thread* self, Monitor* mon, s8 msec, s4 nsec,
    bool interruptShouldThrow)
{
    struct timespec ts;
    bool wasInterrupted = false;
    bool timed;
    int ret;
    char *savedFileName;
    u4 savedLineNumber;

    assert(self != NULL);
    assert(mon != NULL);

    /* Make sure that we hold the lock. */
    if (mon->owner != self) {
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "object not locked by thread before wait()");
        return;
    }

    /*
     * Enforce the timeout range.
     */
    if (msec < 0 || nsec < 0 || nsec > 999999) {
        dvmThrowException("Ljava/lang/IllegalArgumentException;",
            "timeout arguments out of range");
        return;
    }

    /*
     * Compute absolute wakeup time, if necessary.
     */
    if (msec == 0 && nsec == 0) {
        timed = false;
    } else {
        absoluteTime(msec, nsec, &ts);
        timed = true;
    }

    /*
     * Add ourselves to the set of threads waiting on this monitor, and
     * release our hold.  We need to let it go even if we're a few levels
     * deep in a recursive lock, and we need to restore that later.
     *
     * We append to the wait set ahead of clearing the count and owner
     * fields so the subroutine can check that the calling thread owns
     * the monitor.  Aside from that, the order of member updates is
     * not order sensitive as we hold the pthread mutex.
     */
    waitSetAppend(mon, self);
    int prevLockCount = mon->lockCount;
    mon->lockCount = 0;
    mon->owner = NULL;
    savedFileName = mon->ownerFileName;
    mon->ownerFileName = NULL;
    savedLineNumber = mon->ownerLineNumber;
    mon->ownerLineNumber = 0;

    /*
     * Update thread status.  If the GC wakes up, it'll ignore us, knowing
     * that we won't touch any references in this state, and we'll check
     * our suspend mode before we transition out.
     */
    if (timed)
        dvmChangeStatus(self, THREAD_TIMED_WAIT);
    else
        dvmChangeStatus(self, THREAD_WAIT);

    dvmLockMutex(&self->waitMutex);

    /*
     * Set waitMonitor to the monitor object we will be waiting on.
     * When waitMonitor is non-NULL a notifying or interrupting thread
     * must signal the thread's waitCond to wake it up.
     */
    assert(self->waitMonitor == NULL);
    self->waitMonitor = mon;

    /*
     * Handle the case where the thread was interrupted before we called
     * wait().
     */
    if (self->interrupted) {
        wasInterrupted = true;
        self->waitMonitor = NULL;
        dvmUnlockMutex(&self->waitMutex);
        goto done;
    }

    /*
     * Release the monitor lock and wait for a notification or
     * a timeout to occur.
     */
    dvmUnlockMutex(&mon->lock);

    if (!timed) {
        ret = pthread_cond_wait(&self->waitCond, &self->waitMutex);
        assert(ret == 0);
    } else {
#ifdef HAVE_TIMEDWAIT_MONOTONIC
        ret = pthread_cond_timedwait_monotonic(&self->waitCond, &self->waitMutex, &ts);
#else
        ret = pthread_cond_timedwait(&self->waitCond, &self->waitMutex, &ts);
#endif
        assert(ret == 0 || ret == ETIMEDOUT);
    }
    if (self->interrupted) {
        wasInterrupted = true;
    }

    self->interrupted = false;
    self->waitMonitor = NULL;

    dvmUnlockMutex(&self->waitMutex);

    /* Reacquire the monitor lock. */
    lockMonitor(self, mon);

done:
    /*
     * We remove our thread from wait set after restoring the count
     * and owner fields so the subroutine can check that the calling
     * thread owns the monitor. Aside from that, the order of member
     * updates is not order sensitive as we hold the pthread mutex.
     */
    mon->owner = self;
    mon->lockCount = prevLockCount;
    mon->ownerFileName = savedFileName;
    mon->ownerLineNumber = savedLineNumber;
    waitSetRemove(mon, self);

    /* set self->status back to THREAD_RUNNING, and self-suspend if needed */
    dvmChangeStatus(self, THREAD_RUNNING);

    if (wasInterrupted) {
        /*
         * We were interrupted while waiting, or somebody interrupted an
         * un-interruptible thread earlier and we're bailing out immediately.
         *
         * The doc sayeth: "The interrupted status of the current thread is
         * cleared when this exception is thrown."
         */
        self->interrupted = false;
        if (interruptShouldThrow)
            dvmThrowException("Ljava/lang/InterruptedException;", NULL);
    }
}

/*
 * Notify one thread waiting on this monitor.
 */
static void notifyMonitor(Thread* self, Monitor* mon)
{
    Thread* thread;

    assert(self != NULL);
    assert(mon != NULL);

    /* Make sure that we hold the lock. */
    if (mon->owner != self) {
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "object not locked by thread before notify()");
        return;
    }
    /* Signal the first waiting thread in the wait set. */
    while (mon->waitSet != NULL) {
        thread = mon->waitSet;
        mon->waitSet = thread->waitNext;
        thread->waitNext = NULL;
        dvmLockMutex(&thread->waitMutex);
        /* Check to see if the thread is still waiting. */
        if (thread->waitMonitor != NULL) {
            pthread_cond_signal(&thread->waitCond);
            dvmUnlockMutex(&thread->waitMutex);
            return;
        }
        dvmUnlockMutex(&thread->waitMutex);
    }
}

/*
 * Notify all threads waiting on this monitor.
 */
static void notifyAllMonitor(Thread* self, Monitor* mon)
{
    Thread* thread;

    assert(self != NULL);
    assert(mon != NULL);

    /* Make sure that we hold the lock. */
    if (mon->owner != self) {
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "object not locked by thread before notifyAll()");
        return;
    }
    /* Signal all threads in the wait set. */
    while (mon->waitSet != NULL) {
        thread = mon->waitSet;
        mon->waitSet = thread->waitNext;
        thread->waitNext = NULL;
        dvmLockMutex(&thread->waitMutex);
        /* Check to see if the thread is still waiting. */
        if (thread->waitMonitor != NULL) {
            pthread_cond_signal(&thread->waitCond);
        }
        dvmUnlockMutex(&thread->waitMutex);
    }
}

/*
 * Changes the shape of a monitor from thin to fat, preserving the
 * internal lock state.  The calling thread must own the lock.
 */
static void inflateMonitor(Thread *self, Object *obj)
{
    Monitor *mon;
    u4 thin;

    assert(self != NULL);
    assert(obj != NULL);
    assert(LW_SHAPE(obj->lock) == LW_SHAPE_THIN);
    assert(LW_LOCK_OWNER(obj->lock) == self->threadId);
    /* Allocate and acquire a new monitor. */
    mon = dvmCreateMonitor(obj);
    lockMonitor(self, mon);
    /* Propagate the lock state. */
    thin = obj->lock;
    mon->lockCount = LW_LOCK_COUNT(thin);
    thin &= LW_HASH_STATE_MASK << LW_HASH_STATE_SHIFT;
    thin |= (u4)mon | LW_SHAPE_FAT;
    /* Publish the updated lock word. */
    android_atomic_release_store(thin, (int32_t *)&obj->lock);
}

/*
 * Implements monitorenter for "synchronized" stuff.
 *
 * This does not fail or throw an exception (unless deadlock prediction
 * is enabled and set to "err" mode).
 */
void dvmLockObject(Thread* self, Object *obj)
{
    volatile u4 *thinp;
    ThreadStatus oldStatus;
    useconds_t sleepDelay;
    const useconds_t maxSleepDelay = 1 << 20;
    u4 thin, newThin, threadId;

    assert(self != NULL);
    assert(obj != NULL);
    threadId = self->threadId;
    thinp = &obj->lock;
retry:
    thin = *thinp;
    if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
        /*
         * The lock is a thin lock.  The owner field is used to
         * determine the acquire method, ordered by cost.
         */
        if (LW_LOCK_OWNER(thin) == threadId) {
            /*
             * The calling thread owns the lock.  Increment the
             * value of the recursion count field.
             */
            obj->lock += 1 << LW_LOCK_COUNT_SHIFT;
            if (LW_LOCK_COUNT(obj->lock) == LW_LOCK_COUNT_MASK) {
                /*
                 * The reacquisition limit has been reached.  Inflate
                 * the lock so the next acquire will not overflow the
                 * recursion count field.
                 */
                inflateMonitor(self, obj);
            }
        } else if (LW_LOCK_OWNER(thin) == 0) {
            /*
             * The lock is unowned.  Install the thread id of the
             * calling thread into the owner field.  This is the
             * common case.  In performance critical code the JIT
             * will have tried this before calling out to the VM.
             */
            newThin = thin | (threadId << LW_LOCK_OWNER_SHIFT);
            if (android_atomic_acquire_cas(thin, newThin,
                    (int32_t*)thinp) != 0) {
                /*
                 * The acquire failed.  Try again.
                 */
                goto retry;
            }
        } else {
            LOG_THIN("(%d) spin on lock %p: %#x (%#x) %#x",
                     threadId, &obj->lock, 0, *thinp, thin);
            /*
             * The lock is owned by another thread.  Notify the VM
             * that we are about to wait.
             */
            oldStatus = dvmChangeStatus(self, THREAD_MONITOR);
            /*
             * Spin until the thin lock is released or inflated.
             */
            sleepDelay = 0;
            for (;;) {
                thin = *thinp;
                /*
                 * Check the shape of the lock word.  Another thread
                 * may have inflated the lock while we were waiting.
                 */
                if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
                    if (LW_LOCK_OWNER(thin) == 0) {
                        /*
                         * The lock has been released.  Install the
                         * thread id of the calling thread into the
                         * owner field.
                         */
                        newThin = thin | (threadId << LW_LOCK_OWNER_SHIFT);
                        if (android_atomic_acquire_cas(thin, newThin,
                                (int32_t *)thinp) == 0) {
                            /*
                             * The acquire succeed.  Break out of the
                             * loop and proceed to inflate the lock.
                             */
                            break;
                        }
                    } else {
                        /*
                         * The lock has not been released.  Yield so
                         * the owning thread can run.
                         */
                        if (sleepDelay == 0) {
                            sched_yield();
                            sleepDelay = 1000;
                        } else {
                            usleep(sleepDelay);
                            if (sleepDelay < maxSleepDelay / 2) {
                                sleepDelay *= 2;
                            }
                        }
                    }
                } else {
                    /*
                     * The thin lock was inflated by another thread.
                     * Let the VM know we are no longer waiting and
                     * try again.
                     */
                    LOG_THIN("(%d) lock %p surprise-fattened",
                             threadId, &obj->lock);
                    dvmChangeStatus(self, oldStatus);
                    goto retry;
                }
            }
            LOG_THIN("(%d) spin on lock done %p: %#x (%#x) %#x",
                     threadId, &obj->lock, 0, *thinp, thin);
            /*
             * We have acquired the thin lock.  Let the VM know that
             * we are no longer waiting.
             */
            dvmChangeStatus(self, oldStatus);
            /*
             * Fatten the lock.
             */
            inflateMonitor(self, obj);
            LOG_THIN("(%d) lock %p fattened", threadId, &obj->lock);
        }
    } else {
        /*
         * The lock is a fat lock.
         */
        assert(LW_MONITOR(obj->lock) != NULL);
        lockMonitor(self, LW_MONITOR(obj->lock));
    }
#ifdef WITH_DEADLOCK_PREDICTION
    /*
     * See if we were allowed to grab the lock at this time.  We do it
     * *after* acquiring the lock, rather than before, so that we can
     * freely update the Monitor struct.  This seems counter-intuitive,
     * but our goal is deadlock *prediction* not deadlock *prevention*.
     * (If we actually deadlock, the situation is easy to diagnose from
     * a thread dump, so there's no point making a special effort to do
     * the checks before the lock is held.)
     *
     * This needs to happen before we add the object to the thread's
     * monitor list, so we can tell the difference between first-lock and
     * re-lock.
     *
     * It's also important that we do this while in THREAD_RUNNING, so
     * that we don't interfere with cleanup operations in the GC.
     */
    if (gDvm.deadlockPredictMode != kDPOff) {
        if (self->status != THREAD_RUNNING) {
            LOGE("Bad thread status (%d) in DP\n", self->status);
            dvmDumpThread(self, false);
            dvmAbort();
        }
        assert(!dvmCheckException(self));
        updateDeadlockPrediction(self, obj);
        if (dvmCheckException(self)) {
            /*
             * If we're throwing an exception here, we need to free the
             * lock.  We add the object to the thread's monitor list so the
             * "unlock" code can remove it.
             */
            dvmAddToMonitorList(self, obj, false);
            dvmUnlockObject(self, obj);
            LOGV("--- unlocked, pending is '%s'\n",
                dvmGetException(self)->clazz->descriptor);
        }
    }

    /*
     * Add the locked object, and the current stack trace, to the list
     * held by the Thread object.  If deadlock prediction isn't on,
     * don't capture the stack trace.
     */
    dvmAddToMonitorList(self, obj, gDvm.deadlockPredictMode != kDPOff);
#elif defined(WITH_MONITOR_TRACKING)
    /*
     * Add the locked object to the list held by the Thread object.
     */
    dvmAddToMonitorList(self, obj, false);
#endif
}

/*
 * Implements monitorexit for "synchronized" stuff.
 *
 * On failure, throws an exception and returns "false".
 */
bool dvmUnlockObject(Thread* self, Object *obj)
{
    u4 thin;

    assert(self != NULL);
    assert(self->status == THREAD_RUNNING);
    assert(obj != NULL);
    /*
     * Cache the lock word as its value can change while we are
     * examining its state.
     */
    thin = obj->lock;
    if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
        /*
         * The lock is thin.  We must ensure that the lock is owned
         * by the given thread before unlocking it.
         */
        if (LW_LOCK_OWNER(thin) == self->threadId) {
            /*
             * We are the lock owner.  It is safe to update the lock
             * without CAS as lock ownership guards the lock itself.
             */
            if (LW_LOCK_COUNT(thin) == 0) {
                /*
                 * The lock was not recursively acquired, the common
                 * case.  Unlock by clearing all bits except for the
                 * hash state.
                 */
                obj->lock &= (LW_HASH_STATE_MASK << LW_HASH_STATE_SHIFT);
            } else {
                /*
                 * The object was recursively acquired.  Decrement the
                 * lock recursion count field.
                 */
                obj->lock -= 1 << LW_LOCK_COUNT_SHIFT;
            }
        } else {
            /*
             * We do not own the lock.  The JVM spec requires that we
             * throw an exception in this case.
             */
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                              "unlock of unowned monitor");
            return false;
        }
    } else {
        /*
         * The lock is fat.  We must check to see if unlockMonitor has
         * raised any exceptions before continuing.
         */
        assert(LW_MONITOR(obj->lock) != NULL);
        if (!unlockMonitor(self, LW_MONITOR(obj->lock))) {
            /*
             * An exception has been raised.  Do not fall through.
             */
            return false;
        }
    }

#ifdef WITH_MONITOR_TRACKING
    /*
     * Remove the object from the Thread's list.
     */
    dvmRemoveFromMonitorList(self, obj);
#endif

    return true;
}

/*
 * Object.wait().  Also called for class init.
 */
void dvmObjectWait(Thread* self, Object *obj, s8 msec, s4 nsec,
    bool interruptShouldThrow)
{
    Monitor* mon;
    u4 thin = obj->lock;

    /* If the lock is still thin, we need to fatten it.
     */
    if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
        /* Make sure that 'self' holds the lock.
         */
        if (LW_LOCK_OWNER(thin) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before wait()");
            return;
        }

        /* This thread holds the lock.  We need to fatten the lock
         * so 'self' can block on it.  Don't update the object lock
         * field yet, because 'self' needs to acquire the lock before
         * any other thread gets a chance.
         */
        inflateMonitor(self, obj);
        LOG_THIN("(%d) lock %p fattened by wait() to count %d",
                 self->threadId, &obj->lock, mon->lockCount);
    }
    mon = LW_MONITOR(obj->lock);
    waitMonitor(self, mon, msec, nsec, interruptShouldThrow);
}

/*
 * Object.notify().
 */
void dvmObjectNotify(Thread* self, Object *obj)
{
    u4 thin = obj->lock;

    /* If the lock is still thin, there aren't any waiters;
     * waiting on an object forces lock fattening.
     */
    if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
        /* Make sure that 'self' holds the lock.
         */
        if (LW_LOCK_OWNER(thin) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before notify()");
            return;
        }

        /* no-op;  there are no waiters to notify.
         */
    } else {
        /* It's a fat lock.
         */
        notifyMonitor(self, LW_MONITOR(thin));
    }
}

/*
 * Object.notifyAll().
 */
void dvmObjectNotifyAll(Thread* self, Object *obj)
{
    u4 thin = obj->lock;

    /* If the lock is still thin, there aren't any waiters;
     * waiting on an object forces lock fattening.
     */
    if (LW_SHAPE(thin) == LW_SHAPE_THIN) {
        /* Make sure that 'self' holds the lock.
         */
        if (LW_LOCK_OWNER(thin) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before notifyAll()");
            return;
        }

        /* no-op;  there are no waiters to notify.
         */
    } else {
        /* It's a fat lock.
         */
        notifyAllMonitor(self, LW_MONITOR(thin));
    }
}

/*
 * This implements java.lang.Thread.sleep(long msec, int nsec).
 *
 * The sleep is interruptible by other threads, which means we can't just
 * plop into an OS sleep call.  (We probably could if we wanted to send
 * signals around and rely on EINTR, but that's inefficient and relies
 * on native code respecting our signal mask.)
 *
 * We have to do all of this stuff for Object.wait() as well, so it's
 * easiest to just sleep on a private Monitor.
 *
 * It appears that we want sleep(0,0) to go through the motions of sleeping
 * for a very short duration, rather than just returning.
 */
void dvmThreadSleep(u8 msec, u4 nsec)
{
    Thread* self = dvmThreadSelf();
    Monitor* mon = gDvm.threadSleepMon;

    /* sleep(0,0) wakes up immediately, wait(0,0) means wait forever; adjust */
    if (msec == 0 && nsec == 0)
        nsec++;

    lockMonitor(self, mon);
    waitMonitor(self, mon, msec, nsec, true);
    unlockMonitor(self, mon);
}

/*
 * Implement java.lang.Thread.interrupt().
 */
void dvmThreadInterrupt(Thread* thread)
{
    assert(thread != NULL);

    dvmLockMutex(&thread->waitMutex);

    /*
     * If the interrupted flag is already set no additional action is
     * required.
     */
    if (thread->interrupted == true) {
        dvmUnlockMutex(&thread->waitMutex);
        return;
    }

    /*
     * Raise the "interrupted" flag.  This will cause it to bail early out
     * of the next wait() attempt, if it's not currently waiting on
     * something.
     */
    thread->interrupted = true;

    /*
     * Is the thread waiting?
     *
     * Note that fat vs. thin doesn't matter here;  waitMonitor
     * is only set when a thread actually waits on a monitor,
     * which implies that the monitor has already been fattened.
     */
    if (thread->waitMonitor != NULL) {
        pthread_cond_signal(&thread->waitCond);
    }

    dvmUnlockMutex(&thread->waitMutex);
}

#ifndef WITH_COPYING_GC
u4 dvmIdentityHashCode(Object *obj)
{
    return (u4)obj;
}
#else
/*
 * Returns the identity hash code of the given object.
 */
u4 dvmIdentityHashCode(Object *obj)
{
    Thread *self, *thread;
    volatile u4 *lw;
    size_t size;
    u4 lock, owner, hashState;

    if (obj == NULL) {
        /*
         * Null is defined to have an identity hash code of 0.
         */
        return 0;
    }
    lw = &obj->lock;
retry:
    hashState = LW_HASH_STATE(*lw);
    if (hashState == LW_HASH_STATE_HASHED) {
        /*
         * The object has been hashed but has not had its hash code
         * relocated by the garbage collector.  Use the raw object
         * address.
         */
        return (u4)obj >> 3;
    } else if (hashState == LW_HASH_STATE_HASHED_AND_MOVED) {
        /*
         * The object has been hashed and its hash code has been
         * relocated by the collector.  Use the value of the naturally
         * aligned word following the instance data.
         */
        assert(obj->clazz != gDvm.classJavaLangClass);
        if (IS_CLASS_FLAG_SET(obj->clazz, CLASS_ISARRAY)) {
            size = dvmArrayObjectSize((ArrayObject *)obj);
            size = (size + 2) & ~2;
        } else {
            size = obj->clazz->objectSize;
        }
        return *(u4 *)(((char *)obj) + size);
    } else if (hashState == LW_HASH_STATE_UNHASHED) {
        /*
         * The object has never been hashed.  Change the hash state to
         * hashed and use the raw object address.
         */
        self = dvmThreadSelf();
        if (self->threadId == lockOwner(obj)) {
            /*
             * We already own the lock so we can update the hash state
             * directly.
             */
            *lw |= (LW_HASH_STATE_HASHED << LW_HASH_STATE_SHIFT);
            return (u4)obj >> 3;
        }
        /*
         * We do not own the lock.  Try acquiring the lock.  Should
         * this fail, we must suspend the owning thread.
         */
        if (LW_SHAPE(*lw) == LW_SHAPE_THIN) {
            /*
             * If the lock is thin assume it is unowned.  We simulate
             * an acquire, update, and release with a single CAS.
             */
            lock = DVM_LOCK_INITIAL_THIN_VALUE;
            lock |= (LW_HASH_STATE_HASHED << LW_HASH_STATE_SHIFT);
            if (android_atomic_acquire_cas(
                                (int32_t)DVM_LOCK_INITIAL_THIN_VALUE,
                                (int32_t)lock,
                                (int32_t *)lw) == 0) {
                /*
                 * A new lockword has been installed with a hash state
                 * of hashed.  Use the raw object address.
                 */
                return (u4)obj >> 3;
            }
        } else {
            if (tryLockMonitor(self, LW_MONITOR(*lw))) {
                /*
                 * The monitor lock has been acquired.  Change the
                 * hash state to hashed and use the raw object
                 * address.
                 */
                *lw |= (LW_HASH_STATE_HASHED << LW_HASH_STATE_SHIFT);
                unlockMonitor(self, LW_MONITOR(*lw));
                return (u4)obj >> 3;
            }
        }
        /*
         * At this point we have failed to acquire the lock.  We must
         * identify the owning thread and suspend it.
         */
        dvmLockThreadList(self);
        /*
         * Cache the lock word as its value can change between
         * determining its shape and retrieving its owner.
         */
        lock = *lw;
        if (LW_SHAPE(lock) == LW_SHAPE_THIN) {
            /*
             * Find the thread with the corresponding thread id.
             */
            owner = LW_LOCK_OWNER(lock);
            assert(owner != self->threadId);
            /*
             * If the lock has no owner do not bother scanning the
             * thread list and fall through to the failure handler.
             */
            thread = owner ? gDvm.threadList : NULL;
            while (thread != NULL) {
                if (thread->threadId == owner) {
                    break;
                }
                thread = thread->next;
            }
        } else {
            thread = LW_MONITOR(lock)->owner;
        }
        /*
         * If thread is NULL the object has been released since the
         * thread list lock was acquired.  Try again.
         */
        if (thread == NULL) {
            dvmUnlockThreadList();
            goto retry;
        }
        /*
         * Wait for the owning thread to suspend.
         */
        dvmSuspendThread(thread);
        if (dvmHoldsLock(thread, obj)) {
            /*
             * The owning thread has been suspended.  We can safely
             * change the hash state to hashed.
             */
            *lw |= (LW_HASH_STATE_HASHED << LW_HASH_STATE_SHIFT);
            dvmResumeThread(thread);
            dvmUnlockThreadList();
            return (u4)obj >> 3;
        }
        /*
         * The wrong thread has been suspended.  Try again.
         */
        dvmResumeThread(thread);
        dvmUnlockThreadList();
        goto retry;
    }
    LOGE("object %p has an unknown hash state %#x", obj, hashState);
    dvmDumpThread(dvmThreadSelf(), false);
    dvmAbort();
    return 0;  /* Quiet the compiler. */
}
#endif  /* WITH_COPYING_GC */

#ifdef WITH_DEADLOCK_PREDICTION
/*
 * ===========================================================================
 *      Deadlock prediction
 * ===========================================================================
 */
/*
The idea is to predict the possibility of deadlock by recording the order
in which monitors are acquired.  If we see an attempt to acquire a lock
out of order, we can identify the locks and offending code.

To make this work, we need to keep track of the locks held by each thread,
and create history trees for each lock.  When a thread tries to acquire
a new lock, we walk through the "history children" of the lock, looking
for a match with locks the thread already holds.  If we find a match,
it means the thread has made a request that could result in a deadlock.

To support recursive locks, we always allow re-locking a currently-held
lock, and maintain a recursion depth count.

An ASCII-art example, where letters represent Objects:

        A
       /|\
      / | \
     B  |  D
      \ |
       \|
        C

The above is the tree we'd have after handling Object synchronization
sequences "ABC", "AC", "AD".  A has three children, {B, C, D}.  C is also
a child of B.  (The lines represent pointers between parent and child.
Every node can have multiple parents and multiple children.)

If we hold AC, and want to lock B, we recursively search through B's
children to see if A or C appears.  It does, so we reject the attempt.
(A straightforward way to implement it: add a link from C to B, then
determine whether the graph starting at B contains a cycle.)

If we hold AC and want to lock D, we would succeed, creating a new link
from C to D.

The lock history and a stack trace is attached to the Object's Monitor
struct, which means we need to fatten every Object we lock (thin locking
is effectively disabled).  If we don't need the stack trace we can
avoid fattening the leaf nodes, only fattening objects that need to hold
history trees.

Updates to Monitor structs are only allowed for the thread that holds
the Monitor, so we actually do most of our deadlock prediction work after
the lock has been acquired.

When an object with a monitor is GCed, we need to remove it from the
history trees.  There are two basic approaches:
 (1) For through the entire set of known monitors, search all child
     lists for the object in question.  This is rather slow, resulting
     in GC passes that take upwards of 10 seconds to complete.
 (2) Maintain "parent" pointers in each node.  Remove the entries as
     required.  This requires additional storage and maintenance for
     every operation, but is significantly faster at GC time.
For each GCed object, we merge all of the object's children into each of
the object's parents.
*/

#if !defined(WITH_MONITOR_TRACKING)
# error "WITH_DEADLOCK_PREDICTION requires WITH_MONITOR_TRACKING"
#endif

/*
 * Clear out the contents of an ExpandingObjectList, freeing any
 * dynamic allocations.
 */
static void expandObjClear(ExpandingObjectList* pList)
{
    if (pList->list != NULL) {
        free(pList->list);
        pList->list = NULL;
    }
    pList->alloc = pList->count = 0;
}

/*
 * Get the number of objects currently stored in the list.
 */
static inline int expandBufGetCount(const ExpandingObjectList* pList)
{
    return pList->count;
}

/*
 * Get the Nth entry from the list.
 */
static inline Object* expandBufGetEntry(const ExpandingObjectList* pList,
    int i)
{
    return pList->list[i];
}

/*
 * Add a new entry to the list.
 *
 * We don't check for or try to enforce uniqueness.  It's expected that
 * the higher-level code does this for us.
 */
static void expandObjAddEntry(ExpandingObjectList* pList, Object* obj)
{
    if (pList->count == pList->alloc) {
        /* time to expand */
        Object** newList;

        if (pList->alloc == 0)
            pList->alloc = 4;
        else
            pList->alloc *= 2;
        LOGVV("expanding %p to %d\n", pList, pList->alloc);
        newList = realloc(pList->list, pList->alloc * sizeof(Object*));
        if (newList == NULL) {
            LOGE("Failed expanding DP object list (alloc=%d)\n", pList->alloc);
            dvmAbort();
        }
        pList->list = newList;
    }

    pList->list[pList->count++] = obj;
}

/*
 * Returns "true" if the element was successfully removed.
 */
static bool expandObjRemoveEntry(ExpandingObjectList* pList, Object* obj)
{
    int i;

    for (i = pList->count-1; i >= 0; i--) {
        if (pList->list[i] == obj)
            break;
    }
    if (i < 0)
        return false;

    if (i != pList->count-1) {
        /*
         * The order of elements is not important, so we just copy the
         * last entry into the new slot.
         */
        //memmove(&pList->list[i], &pList->list[i+1],
        //    (pList->count-1 - i) * sizeof(pList->list[0]));
        pList->list[i] = pList->list[pList->count-1];
    }

    pList->count--;
    pList->list[pList->count] = (Object*) 0xdecadead;
    return true;
}

/*
 * Returns "true" if "obj" appears in the list.
 */
static bool expandObjHas(const ExpandingObjectList* pList, Object* obj)
{
    int i;

    for (i = 0; i < pList->count; i++) {
        if (pList->list[i] == obj)
            return true;
    }
    return false;
}

/*
 * Print the list contents to stdout.  For debugging.
 */
static void expandObjDump(const ExpandingObjectList* pList)
{
    int i;
    for (i = 0; i < pList->count; i++)
        printf(" %p", pList->list[i]);
}

/*
 * Check for duplicate entries.  Returns the index of the first instance
 * of the duplicated value, or -1 if no duplicates were found.
 */
static int expandObjCheckForDuplicates(const ExpandingObjectList* pList)
{
    int i, j;
    for (i = 0; i < pList->count-1; i++) {
        for (j = i + 1; j < pList->count; j++) {
            if (pList->list[i] == pList->list[j]) {
                return i;
            }
        }
    }

    return -1;
}


/*
 * Determine whether "child" appears in the list of objects associated
 * with the Monitor in "parent".  If "parent" is a thin lock, we return
 * false immediately.
 */
static bool objectInChildList(const Object* parent, Object* child)
{
    u4 lock = parent->lock;
    if (!IS_LOCK_FAT(&lock)) {
        //LOGI("on thin\n");
        return false;
    }

    return expandObjHas(&LW_MONITOR(lock)->historyChildren, child);
}

/*
 * Print the child list.
 */
static void dumpKids(Object* parent)
{
    Monitor* mon = LW_MONITOR(parent->lock);

    printf("Children of %p:", parent);
    expandObjDump(&mon->historyChildren);
    printf("\n");
}

/*
 * Add "child" to the list of children in "parent", and add "parent" to
 * the list of parents in "child".
 */
static void linkParentToChild(Object* parent, Object* child)
{
    //assert(LW_MONITOR(parent->lock)->owner == dvmThreadSelf());   // !owned for merge
    assert(IS_LOCK_FAT(&parent->lock));
    assert(IS_LOCK_FAT(&child->lock));
    assert(parent != child);
    Monitor* mon;

    mon = LW_MONITOR(parent->lock);
    assert(!expandObjHas(&mon->historyChildren, child));
    expandObjAddEntry(&mon->historyChildren, child);

    mon = LW_MONITOR(child->lock);
    assert(!expandObjHas(&mon->historyParents, parent));
    expandObjAddEntry(&mon->historyParents, parent);
}


/*
 * Remove "child" from the list of children in "parent".
 */
static void unlinkParentFromChild(Object* parent, Object* child)
{
    //assert(LW_MONITOR(parent->lock)->owner == dvmThreadSelf());   // !owned for GC
    assert(IS_LOCK_FAT(&parent->lock));
    assert(IS_LOCK_FAT(&child->lock));
    assert(parent != child);
    Monitor* mon;

    mon = LW_MONITOR(parent->lock);
    if (!expandObjRemoveEntry(&mon->historyChildren, child)) {
        LOGW("WARNING: child %p not found in parent %p\n", child, parent);
    }
    assert(!expandObjHas(&mon->historyChildren, child));
    assert(expandObjCheckForDuplicates(&mon->historyChildren) < 0);

    mon = LW_MONITOR(child->lock);
    if (!expandObjRemoveEntry(&mon->historyParents, parent)) {
        LOGW("WARNING: parent %p not found in child %p\n", parent, child);
    }
    assert(!expandObjHas(&mon->historyParents, parent));
    assert(expandObjCheckForDuplicates(&mon->historyParents) < 0);
}


/*
 * Log the monitors held by the current thread.  This is done as part of
 * flagging an error.
 */
static void logHeldMonitors(Thread* self)
{
    char* name = NULL;

    name = dvmGetThreadName(self);
    LOGW("Monitors currently held by thread (threadid=%d '%s')\n",
        self->threadId, name);
    LOGW("(most-recently-acquired on top):\n");
    free(name);

    LockedObjectData* lod = self->pLockedObjects;
    while (lod != NULL) {
        LOGW("--- object %p[%d] (%s)\n",
            lod->obj, lod->recursionCount, lod->obj->clazz->descriptor);
        dvmLogRawStackTrace(lod->rawStackTrace, lod->stackDepth);

        lod = lod->next;
    }
}

/*
 * Recursively traverse the object hierarchy starting at "obj".  We mark
 * ourselves on entry and clear the mark on exit.  If we ever encounter
 * a marked object, we have a cycle.
 *
 * Returns "true" if all is well, "false" if we found a cycle.
 */
static bool traverseTree(Thread* self, const Object* obj)
{
    assert(IS_LOCK_FAT(&obj->lock));
    Monitor* mon = LW_MONITOR(obj->lock);

    /*
     * Have we been here before?
     */
    if (mon->historyMark) {
        int* rawStackTrace;
        int stackDepth;

        LOGW("%s\n", kStartBanner);
        LOGW("Illegal lock attempt:\n");
        LOGW("--- object %p (%s)\n", obj, obj->clazz->descriptor);

        rawStackTrace = dvmFillInStackTraceRaw(self, &stackDepth);
        dvmLogRawStackTrace(rawStackTrace, stackDepth);
        free(rawStackTrace);

        LOGW(" ");
        logHeldMonitors(self);

        LOGW(" ");
        LOGW("Earlier, the following lock order (from last to first) was\n");
        LOGW("established -- stack trace is from first successful lock):\n");
        return false;
    }
    mon->historyMark = true;

    /*
     * Examine the children.  We do NOT hold these locks, so they might
     * very well transition from thin to fat or change ownership while
     * we work.
     *
     * NOTE: we rely on the fact that they cannot revert from fat to thin
     * while we work.  This is currently a safe assumption.
     *
     * We can safely ignore thin-locked children, because by definition
     * they have no history and are leaf nodes.  In the current
     * implementation we always fatten the locks to provide a place to
     * hang the stack trace.
     */
    ExpandingObjectList* pList = &mon->historyChildren;
    int i;
    for (i = expandBufGetCount(pList)-1; i >= 0; i--) {
        const Object* child = expandBufGetEntry(pList, i);
        u4 lock = child->lock;
        if (!IS_LOCK_FAT(&lock))
            continue;
        if (!traverseTree(self, child)) {
            LOGW("--- object %p (%s)\n", obj, obj->clazz->descriptor);
            dvmLogRawStackTrace(mon->historyRawStackTrace,
                mon->historyStackDepth);
            mon->historyMark = false;
            return false;
        }
    }

    mon->historyMark = false;

    return true;
}

/*
 * Update the deadlock prediction tree, based on the current thread
 * acquiring "acqObj".  This must be called before the object is added to
 * the thread's list of held monitors.
 *
 * If the thread already holds the lock (recursion), or this is a known
 * lock configuration, we return without doing anything.  Otherwise, we add
 * a link from the most-recently-acquired lock in this thread to "acqObj"
 * after ensuring that the parent lock is "fat".
 *
 * This MUST NOT be called while a GC is in progress in another thread,
 * because we assume exclusive access to history trees in owned monitors.
 */
static void updateDeadlockPrediction(Thread* self, Object* acqObj)
{
    LockedObjectData* lod;
    LockedObjectData* mrl;

    /*
     * Quick check for recursive access.
     */
    lod = dvmFindInMonitorList(self, acqObj);
    if (lod != NULL) {
        LOGV("+++ DP: recursive %p\n", acqObj);
        return;
    }

    /*
     * Make the newly-acquired object's monitor "fat".  In some ways this
     * isn't strictly necessary, but we need the GC to tell us when
     * "interesting" objects go away, and right now the only way to make
     * an object look interesting is to give it a monitor.
     *
     * This also gives us a place to hang a stack trace.
     *
     * Our thread holds the lock, so we're allowed to rewrite the lock
     * without worrying that something will change out from under us.
     */
    if (!IS_LOCK_FAT(&acqObj->lock)) {
        LOGVV("fattening lockee %p (recur=%d)\n",
            acqObj, LW_LOCK_COUNT(acqObj->lock.thin));
        inflateMonitor(self, acqObj);
    }

    /* if we don't have a stack trace for this monitor, establish one */
    if (LW_MONITOR(acqObj->lock)->historyRawStackTrace == NULL) {
        Monitor* mon = LW_MONITOR(acqObj->lock);
        mon->historyRawStackTrace = dvmFillInStackTraceRaw(self,
            &mon->historyStackDepth);
    }

    /*
     * We need to examine and perhaps modify the most-recently-locked
     * monitor.  We own that, so there's no risk of another thread
     * stepping on us.
     *
     * Retrieve the most-recently-locked entry from our thread.
     */
    mrl = self->pLockedObjects;
    if (mrl == NULL)
        return;         /* no other locks held */

    /*
     * Do a quick check to see if "acqObj" is a direct descendant.  We can do
     * this without holding the global lock because of our assertion that
     * a GC is not running in parallel -- nobody except the GC can
     * modify a history list in a Monitor they don't own, and we own "mrl".
     * (There might be concurrent *reads*, but no concurrent *writes.)
     *
     * If we find it, this is a known good configuration, and we're done.
     */
    if (objectInChildList(mrl->obj, acqObj))
        return;

    /*
     * "mrl" is going to need to have a history tree.  If it's currently
     * a thin lock, we make it fat now.  The thin lock might have a
     * nonzero recursive lock count, which we need to carry over.
     *
     * Our thread holds the lock, so we're allowed to rewrite the lock
     * without worrying that something will change out from under us.
     */
    if (!IS_LOCK_FAT(&mrl->obj->lock)) {
        LOGVV("fattening parent %p f/b/o child %p (recur=%d)\n",
            mrl->obj, acqObj, LW_LOCK_COUNT(mrl->obj->lock));
        inflateMonitor(self, mrl->obj);
    }

    /*
     * We haven't seen this configuration before.  We need to scan down
     * acqObj's tree to see if any of the monitors in self->pLockedObjects
     * appear.  We grab a global lock before traversing or updating the
     * history list.
     *
     * If we find a match for any of our held locks, we know that the lock
     * has previously been acquired *after* acqObj, and we throw an error.
     *
     * The easiest way to do this is to create a link from "mrl" to "acqObj"
     * and do a recursive traversal, marking nodes as we cross them.  If
     * we cross one a second time, we have a cycle and can throw an error.
     * (We do the flag-clearing traversal before adding the new link, so
     * that we're guaranteed to terminate.)
     *
     * If "acqObj" is a thin lock, it has no history, and we can create a
     * link to it without additional checks.  [ We now guarantee that it's
     * always fat. ]
     */
    bool failed = false;
    dvmLockMutex(&gDvm.deadlockHistoryLock);
    linkParentToChild(mrl->obj, acqObj);
    if (!traverseTree(self, acqObj)) {
        LOGW("%s\n", kEndBanner);
        failed = true;

        /* remove the entry so we're still okay when in "warning" mode */
        unlinkParentFromChild(mrl->obj, acqObj);
    }
    dvmUnlockMutex(&gDvm.deadlockHistoryLock);

    if (failed) {
        switch (gDvm.deadlockPredictMode) {
        case kDPErr:
            dvmThrowException("Ldalvik/system/PotentialDeadlockError;", NULL);
            break;
        case kDPAbort:
            LOGE("Aborting due to potential deadlock\n");
            dvmAbort();
            break;
        default:
            /* warn only */
            break;
        }
    }
}

/*
 * We're removing "child" from existence.  We want to pull all of
 * child's children into "parent", filtering out duplicates.  This is
 * called during the GC.
 *
 * This does not modify "child", which might have multiple parents.
 */
static void mergeChildren(Object* parent, const Object* child)
{
    Monitor* mon;
    int i;

    assert(IS_LOCK_FAT(&child->lock));
    mon = LW_MONITOR(child->lock);
    ExpandingObjectList* pList = &mon->historyChildren;

    for (i = expandBufGetCount(pList)-1; i >= 0; i--) {
        Object* grandChild = expandBufGetEntry(pList, i);

        if (!objectInChildList(parent, grandChild)) {
            LOGVV("+++  migrating %p link to %p\n", grandChild, parent);
            linkParentToChild(parent, grandChild);
        } else {
            LOGVV("+++  parent %p already links to %p\n", parent, grandChild);
        }
    }
}

/*
 * An object with a fat lock is being collected during a GC pass.  We
 * want to remove it from any lock history trees that it is a part of.
 *
 * This may require updating the history trees in several monitors.  The
 * monitor semantics guarantee that no other thread will be accessing
 * the history trees at the same time.
 */
static void removeCollectedObject(Object* obj)
{
    Monitor* mon;

    LOGVV("+++ collecting %p\n", obj);

    /*
     * For every parent of this object:
     *  - merge all of our children into the parent's child list (creates
     *    a two-way link between parent and child)
     *  - remove ourselves from the parent's child list
     */
    ExpandingObjectList* pList;
    int i;

    assert(IS_LOCK_FAT(&obj->lock));
    mon = LW_MONITOR(obj->lock);
    pList = &mon->historyParents;
    for (i = expandBufGetCount(pList)-1; i >= 0; i--) {
        Object* parent = expandBufGetEntry(pList, i);
        Monitor* parentMon = LW_MONITOR(parent->lock);

        if (!expandObjRemoveEntry(&parentMon->historyChildren, obj)) {
            LOGW("WARNING: child %p not found in parent %p\n", obj, parent);
        }
        assert(!expandObjHas(&parentMon->historyChildren, obj));

        mergeChildren(parent, obj);
    }

    /*
     * For every child of this object:
     *  - remove ourselves from the child's parent list
     */
    pList = &mon->historyChildren;
    for (i = expandBufGetCount(pList)-1; i >= 0; i--) {
        Object* child = expandBufGetEntry(pList, i);
        Monitor* childMon = LW_MONITOR(child->lock);

        if (!expandObjRemoveEntry(&childMon->historyParents, obj)) {
            LOGW("WARNING: parent %p not found in child %p\n", obj, child);
        }
        assert(!expandObjHas(&childMon->historyParents, obj));
    }
}

#endif /*WITH_DEADLOCK_PREDICTION*/
