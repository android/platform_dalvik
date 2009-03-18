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
 *
 * NOTE: if we broadcast a notify, and somebody sneaks in a Thread.interrupt
 * before the notify finishes (i.e. before all threads sleeping on the
 * condition variable have awoken), we could end up with a nonzero value for
 * "notifying" after everybody is gone because one of the notified threads
 * will actually exit via the "interrupted" path.  This can be detected as
 * (notifying + interrupting > waiting), i.e. the number of threads that need
 * to be woken is greater than the number waiting.  The fix is to test and
 * adjust "notifying" at the start of the wait() call.
 * -> This is probably not a problem if we notify less than the full set
 * before the interrupt comes in.  If we have four waiters, two pending
 * notifies, and an interrupt hits, we will interrupt one thread and notify
 * two others.  Doesn't matter if the interrupted thread would have been
 * one of the notified.  Count is only screwed up if we have two waiters,
 * in which case it's safe to fix it at the start of the next wait().
 */
#include "Dalvik.h"

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
 * The two states that an Object's lock may have are referred to as
 * "thin" and "fat".  The lock may transition between the two states
 * for various reasons.
 *
 * The lock value itself is stored in Object.lock, which is a union of
 * the form:
 *
 *     typedef union Lock {
 *         u4          thin;
 *         Monitor*    mon;
 *     } Lock;
 *
 * It is possible to tell the current state of the lock from the actual
 * value, so we do not need to store any additional state.  When the
 * lock is "thin", it has the form:
 *
 *     [31 ---- 16] [15 ---- 1] [0]
 *      lock count   thread id   1
 *
 * When it is "fat", the field is simply a (Monitor *).  Since the pointer
 * will always be 4-byte-aligned, bits 1 and 0 will always be zero when
 * the field holds a pointer.  Hence, we can tell the current fat-vs-thin
 * state by checking the least-significant bit.
 *
 * For an in-depth description of the mechanics of thin-vs-fat locking,
 * read the paper referred to above.
 *
 * To reduce the amount of work when attempting a compare and exchange,
 * Thread.threadId is guaranteed to have bit 0 set, and all new Objects
 * have their lock fields initialized to the value 0x1, or
 * DVM_LOCK_INITIAL_THIN_VALUE, via DVM_OBJECT_INIT().
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
 */
struct Monitor {
    Thread*     owner;          /* which thread currently owns the lock? */
    int         lockCount;      /* owner's recursive lock depth */
    Object*     obj;            /* what object are we part of [debug only] */

    int         waiting;        /* total #of threads waiting on this */
    int         notifying;      /* #of threads being notified */
    int         interrupting;   /* #of threads being interrupted */

    pthread_mutex_t lock;
    pthread_cond_t  cond;

    Monitor*    next;

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
    mon->obj = obj;
    dvmInitMutex(&mon->lock);
    pthread_cond_init(&mon->cond, NULL);

    /* replace the head of the list with the new monitor */
    do {
        mon->next = gDvm.monitorList;
    } while (!ATOMIC_CMP_SWAP((int32_t*)(void*)&gDvm.monitorList,
                              (int32_t)mon->next, (int32_t)mon));

    return mon;
}

/*
 * Release a Monitor.
 */
static void releaseMonitor(Monitor* mon)
{
    // TODO
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
 * Checks whether the given thread holds the given
 * objects's lock.
 */
bool dvmHoldsLock(Thread* thread, Object* obj)
{
    if (thread == NULL || obj == NULL) {
        return false;
    }

    /* Since we're reading the lock value multiple times,
     * latch it so that it doesn't change out from under
     * us if we get preempted.
     */
    Lock lock = obj->lock;
    if (IS_LOCK_FAT(&lock)) {
        return thread == lock.mon->owner;
    } else {
        return thread->threadId == (lock.thin & 0xffff);
    }
}

/*
 * Free the monitor associated with an object and make the object's lock
 * thin again.  This is called during garbage collection.
 */
void dvmFreeObjectMonitor_internal(Lock *lock)
{
    Monitor *mon;

    /* The macro that wraps this function checks IS_LOCK_FAT() first.
     */
    assert(IS_LOCK_FAT(lock));

#ifdef WITH_DEADLOCK_PREDICTION
    if (gDvm.deadlockPredictMode != kDPOff)
        removeCollectedObject(lock->mon->obj);
#endif

    mon = lock->mon;
    lock->thin = DVM_LOCK_INITIAL_THIN_VALUE;

    /* This lock is associated with an object
     * that's being swept.  The only possible way
     * anyone could be holding this lock would be
     * if some JNI code locked but didn't unlock
     * the object, in which case we've got some bad
     * native code somewhere.
     */
    assert(pthread_mutex_trylock(&mon->lock) == 0);
    pthread_mutex_destroy(&mon->lock);
    pthread_cond_destroy(&mon->cond);
#if 1
//TODO: unlink from the monitor list (would require a lock)
// (might not -- the GC suspension may be enough)
    {
        Monitor *next;
        next = mon->next;
#ifdef WITH_DEADLOCK_PREDICTION
        expandObjClear(&mon->historyChildren);
        expandObjClear(&mon->historyParents);
        free(mon->historyRawStackTrace);
#endif
        memset(mon, 0, sizeof (*mon));
        mon->next = next;
    }
//free(mon);
#endif
}


/*
 * Lock a monitor.
 */
static void lockMonitor(Thread* self, Monitor* mon)
{
    int cc;

    if (mon->owner == self) {
        mon->lockCount++;
    } else {
        ThreadStatus oldStatus;

        if (pthread_mutex_trylock(&mon->lock) != 0) {
            /* mutex is locked, switch to wait status and sleep on it */
            oldStatus = dvmChangeStatus(self, THREAD_MONITOR);
            cc = pthread_mutex_lock(&mon->lock);
            assert(cc == 0);
            dvmChangeStatus(self, oldStatus);
        }

        mon->owner = self;
        assert(mon->lockCount == 0);

        /*
         * "waiting", "notifying", and "interrupting" could all be nonzero
         * if we're locking an object on which other threads are waiting.
         * Nothing worth assert()ing about here.
         */
    }
}

/*
 * Try to lock a monitor.
 *
 * Returns "true" on success.
 */
static bool tryLockMonitor(Thread* self, Monitor* mon)
{
    int cc;

    if (mon->owner == self) {
        mon->lockCount++;
        return true;
    } else {
        cc = pthread_mutex_trylock(&mon->lock);
        if (cc == 0) {
            mon->owner = self;
            assert(mon->lockCount == 0);
            return true;
        } else {
            return false;
        }
    }
}


/*
 * Unlock a monitor.
 *
 * Returns true if the unlock succeeded.
 * If the unlock failed, an exception will be pending.
 */
static bool unlockMonitor(Thread* self, Monitor* mon)
{
    assert(mon != NULL);        // can this happen?

    if (mon->owner == self) {
        /*
         * We own the monitor, so nobody else can be in here.
         */
        if (mon->lockCount == 0) {
            int cc;
            mon->owner = NULL;
            cc = pthread_mutex_unlock(&mon->lock);
            assert(cc == 0);
        } else {
            mon->lockCount--;
        }
    } else {
        /*
         * We don't own this, so we're not allowed to unlock it.
         * The JNI spec says that we should throw IllegalMonitorStateException
         * in this case.
         */
        if (mon->owner == NULL) {
            //LOGW("Unlock fat %p: not owned\n", mon->obj);
        } else {
            //LOGW("Unlock fat %p: id %d vs %d\n",
            //    mon->obj, mon->owner->threadId, self->threadId);
        }
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "unlock of unowned monitor");
        return false;
    }
    return true;
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
    int cc;

    /* Make sure that the lock is fat and that we hold it. */
    if (mon == NULL || ((u4)mon & 1) != 0 || mon->owner != self) {
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
        s8 endSec;

#ifdef HAVE_TIMEDWAIT_MONOTONIC
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        endSec = now.tv_sec + msec / 1000;
        if (endSec >= 0x7fffffff) {
            LOGV("NOTE: end time exceeds epoch\n");
            endSec = 0x7ffffffe;
        }
        ts.tv_sec = endSec;
        ts.tv_nsec = (now.tv_nsec + (msec % 1000) * 1000 * 1000) + nsec;
#else
        struct timeval now;
        gettimeofday(&now, NULL);
        endSec = now.tv_sec + msec / 1000;
        if (endSec >= 0x7fffffff) {
            LOGV("NOTE: end time exceeds epoch\n");
            endSec = 0x7ffffffe;
        }
        ts.tv_sec = endSec;
        ts.tv_nsec = (now.tv_usec + (msec % 1000) * 1000) * 1000 + nsec;
#endif

        /* catch rollover */
        if (ts.tv_nsec >= 1000000000L) {
            ts.tv_sec++;
            ts.tv_nsec -= 1000000000L;
        }
        timed = true;
    }

    /*
     * Make sure "notifying" wasn't screwed up by earlier activity.  If this
     * is wrong we could end up waking up too many people.  (This is a rare
     * situation, but we need to handle it correctly.)
     */
    if (mon->notifying + mon->interrupting > mon->waiting) {
        LOGD("threadid=%d: bogus mon %d+%d>%d; adjusting\n",
            self->threadId, mon->notifying, mon->interrupting,
            mon->waiting);

        assert(mon->waiting >= mon->interrupting);
        mon->notifying = mon->waiting - mon->interrupting;
    }

    /*
     * Add ourselves to the set of threads waiting on this monitor, and
     * release our hold.  We need to let it go even if we're a few levels
     * deep in a recursive lock, and we need to restore that later.
     *
     * The order of operations here isn't significant, because we still
     * hold the pthread mutex.
     */
    int prevLockCount;

    prevLockCount = mon->lockCount;
    mon->lockCount = 0;
    mon->waiting++;
    mon->owner = NULL;

    /*
     * Update thread status.  If the GC wakes up, it'll ignore us, knowing
     * that we won't touch any references in this state, and we'll check
     * our suspend mode before we transition out.
     */
    if (timed)
        dvmChangeStatus(self, THREAD_TIMED_WAIT);
    else
        dvmChangeStatus(self, THREAD_WAIT);

    /*
     * Tell the thread which monitor we're waiting on.  This is necessary
     * so that Thread.interrupt() can wake us up.  Thread.interrupt needs
     * to gain ownership of the monitor mutex before it can signal us, so
     * we're still not worried about race conditions.
     */
    self->waitMonitor = mon;

    /*
     * Handle the case where the thread was interrupted before we called
     * wait().
     */
    if (self->interrupted) {
        wasInterrupted = true;
        goto done;
    }

    LOGVV("threadid=%d: waiting on %p\n", self->threadId, mon);

    while (true) {
        if (!timed) {
            cc = pthread_cond_wait(&mon->cond, &mon->lock);
            assert(cc == 0);
        } else {
#ifdef HAVE_TIMEDWAIT_MONOTONIC
            cc = pthread_cond_timedwait_monotonic(&mon->cond, &mon->lock, &ts);
#else
            cc = pthread_cond_timedwait(&mon->cond, &mon->lock, &ts);
#endif
            if (cc == ETIMEDOUT) {
                LOGVV("threadid=%d wakeup: timeout\n", self->threadId);
                break;
            }
        }

        /*
         * We woke up because of an interrupt (which does a broadcast) or
         * a notification (which might be a signal or a broadcast).  Figure
         * out what we need to do.
         */
        if (self->interruptingWait) {
            /*
             * The other thread successfully gained the monitor lock, and
             * has confirmed that we were waiting on it.  If this is an
             * interruptible wait, we bail out immediately.  If not, we
             * continue on.
             */
            self->interruptingWait = false;
            mon->interrupting--;
            assert(self->interrupted);
            if (interruptShouldThrow) {
                wasInterrupted = true;
                LOGD("threadid=%d wakeup: interrupted\n", self->threadId);
                break;
            } else {
                LOGD("threadid=%d wakeup: not interruptible\n", self->threadId);
            }
        }
        if (mon->notifying) {
            /*
             * One or more threads are being notified.  Remove ourselves
             * from the set.
             */
            mon->notifying--;
            LOGVV("threadid=%d wakeup: notified\n", self->threadId);
            break;
        } else {
            /*
             * Looks like we were woken unnecessarily, probably as a
             * result of another thread being interrupted.  Go back to
             * sleep.
             */
            LOGVV("threadid=%d wakeup: going back to sleep\n", self->threadId);
        }
    }

done:
    //if (wasInterrupted) {
    //    LOGW("threadid=%d: throwing InterruptedException:\n", self->threadId);
    //    dvmDumpThread(self, false);
    //}

    /*
     * Put everything back.  Again, we hold the pthread mutex, so the order
     * here isn't significant.
     */
    self->waitMonitor = NULL;
    mon->owner = self;
    mon->waiting--;
    mon->lockCount = prevLockCount;

    /* set self->status back to THREAD_RUNNING, and self-suspend if needed */
    dvmChangeStatus(self, THREAD_RUNNING);

    if (wasInterrupted) {
        /*
         * We were interrupted while waiting, or somebody interrupted an
         * un-interruptable thread earlier and we're bailing out immediately.
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
    /* Make sure that the lock is fat and that we hold it. */
    if (mon == NULL || ((u4)mon & 1) != 0 || mon->owner != self) {
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "object not locked by thread before notify()");
        return;
    }

    /*
     * Check to see if anybody is there to notify.  We subtract off
     * threads that are being interrupted and anything that has
     * potentially already been notified.
     */
    if (mon->notifying + mon->interrupting < mon->waiting) {
        /* wake up one thread */
        int cc;

        LOGVV("threadid=%d: signaling on %p\n", self->threadId, mon);

        mon->notifying++;
        cc = pthread_cond_signal(&mon->cond);
        assert(cc == 0);
    } else {
        LOGVV("threadid=%d: nobody to signal on %p\n", self->threadId, mon);
    }
}

/*
 * Notify all threads waiting on this monitor.
 *
 * We keep a count of how many threads we notified, so that our various
 * counts remain accurate.
 */
static void notifyAllMonitor(Thread* self, Monitor* mon)
{
    /* Make sure that the lock is fat and that we hold it. */
    if (mon == NULL || ((u4)mon & 1) != 0 || mon->owner != self) {
        dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
            "object not locked by thread before notifyAll()");
        return;
    }

    mon->notifying = mon->waiting - mon->interrupting;
    if (mon->notifying > 0) {
        int cc;

        LOGVV("threadid=%d: broadcasting to %d threads on %p\n",
            self->threadId, mon->notifying, mon);

        cc = pthread_cond_broadcast(&mon->cond);
        assert(cc == 0);
    } else {
        LOGVV("threadid=%d: nobody to broadcast to on %p\n", self->threadId,mon);
    }
}

#if THIN_LOCKING
/*
 * Thin locking support
 */

/*
 * Implements monitorenter for "synchronized" stuff.
 *
 * This does not fail or throw an exception (unless deadlock prediction
 * is enabled and set to "err" mode).
 */
void dvmLockObject(Thread* self, Object *obj)
{
    volatile u4 *thinp = &obj->lock.thin;
    u4 threadId = self->threadId;

    /* First, try to grab the lock as if it's thin;
     * this is the common case and will usually succeed.
     */
    if (!ATOMIC_CMP_SWAP((int32_t *)thinp,
                         (int32_t)DVM_LOCK_INITIAL_THIN_VALUE,
                         (int32_t)threadId)) {
        /* The lock is either a thin lock held by someone (possibly 'self'),
         * or a fat lock.
         */
        if ((*thinp & 0xffff) == threadId) {
            /* 'self' is already holding the thin lock; we can just
             * bump the count.  Atomic operations are not necessary
             * because only the thread holding the lock is allowed
             * to modify the Lock field.
             */
            *thinp += 1<<16;
        } else {
            /* If this is a thin lock we need to spin on it, if it's fat
             * we need to acquire the monitor.
             */
            if ((*thinp & 1) != 0) {
                ThreadStatus oldStatus;
                static const unsigned long maxSleepDelay = 1 * 1024 * 1024;
                unsigned long sleepDelay;

                LOG_THIN("(%d) spin on lock 0x%08x: 0x%08x (0x%08x) 0x%08x\n",
                         threadId, (uint)&obj->lock,
                         DVM_LOCK_INITIAL_THIN_VALUE, *thinp, threadId);

                /* The lock is still thin, but some other thread is
                 * holding it.  Let the VM know that we're about
                 * to wait on another thread.
                 */
                oldStatus = dvmChangeStatus(self, THREAD_MONITOR);

                /* Spin until the other thread lets go.
                 */
                sleepDelay = 0;
                do {
                    /* In addition to looking for an unlock,
                     * we need to watch out for some other thread
                     * fattening the lock behind our back.
                     */
                    while (*thinp != DVM_LOCK_INITIAL_THIN_VALUE) {
                        if ((*thinp & 1) == 0) {
                            /* The lock has been fattened already.
                             */
                            LOG_THIN("(%d) lock 0x%08x surprise-fattened\n",
                                     threadId, (uint)&obj->lock);
                            dvmChangeStatus(self, oldStatus);
                            goto fat_lock;
                        }

                        if (sleepDelay == 0) {
                            sched_yield();
                            sleepDelay = 1 * 1000;
                        } else {
                            usleep(sleepDelay);
                            if (sleepDelay < maxSleepDelay / 2) {
                                sleepDelay *= 2;
                            }
                        }
                    }
                } while (!ATOMIC_CMP_SWAP((int32_t *)thinp,
                                          (int32_t)DVM_LOCK_INITIAL_THIN_VALUE,
                                          (int32_t)threadId));
                LOG_THIN("(%d) spin on lock done 0x%08x: "
                         "0x%08x (0x%08x) 0x%08x\n",
                         threadId, (uint)&obj->lock,
                         DVM_LOCK_INITIAL_THIN_VALUE, *thinp, threadId);

                /* We've got the thin lock; let the VM know that we're
                 * done waiting.
                 */
                dvmChangeStatus(self, oldStatus);

                /* Fatten the lock.  Note this relinquishes ownership.
                 * We could also create the monitor in an "owned" state
                 * to avoid "re-locking" it in fat_lock.
                 */
                obj->lock.mon = dvmCreateMonitor(obj);
                LOG_THIN("(%d) lock 0x%08x fattened\n",
                         threadId, (uint)&obj->lock);

                /* Fall through to acquire the newly fat lock.
                 */
            }

            /* The lock is already fat, which means
             * that obj->lock.mon is a regular (Monitor *).
             */
        fat_lock:
            assert(obj->lock.mon != NULL);
            lockMonitor(self, obj->lock.mon);
        }
    }
    // else, the lock was acquired with the ATOMIC_CMP_SWAP().

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
    volatile u4 *thinp = &obj->lock.thin;
    u4 threadId = self->threadId;

    /* Check the common case, where 'self' has locked 'obj' once, first.
     */
    if (*thinp == threadId) {
        /* Unlock 'obj' by clearing our threadId from 'thin'.
         * The lock protects the lock field itself, so it's
         * safe to update non-atomically.
         */
        *thinp = DVM_LOCK_INITIAL_THIN_VALUE;
    } else if ((*thinp & 1) != 0) {
        /* If the object is locked, it had better be locked by us.
         */
        if ((*thinp & 0xffff) != threadId) {
            /* The JNI spec says that we should throw an exception
             * in this case.
             */
            //LOGW("Unlock thin %p: id %d vs %d\n",
            //    obj, (*thinp & 0xfff), threadId);
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "unlock of unowned monitor");
            return false;
        }

        /* It's a thin lock, but 'self' has locked 'obj'
         * more than once.  Decrement the count.
         */
        *thinp -= 1<<16;
    } else {
        /* It's a fat lock.
         */
        assert(obj->lock.mon != NULL);
        if (!unlockMonitor(self, obj->lock.mon)) {
            /* exception has been raised */
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
    Monitor* mon = obj->lock.mon;
    u4 thin = obj->lock.thin;

    /* If the lock is still thin, we need to fatten it.
     */
    if ((thin & 1) != 0) {
        /* Make sure that 'self' holds the lock.
         */
        if ((thin & 0xffff) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before wait()");
            return;
        }

        /* This thread holds the lock.  We need to fatten the lock
         * so 'self' can block on it.  Don't update the object lock
         * field yet, because 'self' needs to acquire the lock before
         * any other thread gets a chance.
         */
        mon = dvmCreateMonitor(obj);

        /* 'self' has actually locked the object one or more times;
         * make sure that the monitor reflects this.
         */
        lockMonitor(self, mon);
        mon->lockCount = thin >> 16;
        LOG_THIN("(%d) lock 0x%08x fattened by wait() to count %d\n",
                 self->threadId, (uint)&obj->lock, mon->lockCount);

        /* Make the monitor public now that it's in the right state.
         */
        obj->lock.mon = mon;
    }

    waitMonitor(self, mon, msec, nsec, interruptShouldThrow);
}

/*
 * Object.notify().
 */
void dvmObjectNotify(Thread* self, Object *obj)
{
    Monitor* mon = obj->lock.mon;
    u4 thin = obj->lock.thin;

    /* If the lock is still thin, there aren't any waiters;
     * waiting on an object forces lock fattening.
     */
    if ((thin & 1) != 0) {
        /* Make sure that 'self' holds the lock.
         */
        if ((thin & 0xffff) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before notify()");
            return;
        }

        /* no-op;  there are no waiters to notify.
         */
    } else {
        /* It's a fat lock.
         */
        notifyMonitor(self, mon);
    }
}

/*
 * Object.notifyAll().
 */
void dvmObjectNotifyAll(Thread* self, Object *obj)
{
    u4 thin = obj->lock.thin;

    /* If the lock is still thin, there aren't any waiters;
     * waiting on an object forces lock fattening.
     */
    if ((thin & 1) != 0) {
        /* Make sure that 'self' holds the lock.
         */
        if ((thin & 0xffff) != self->threadId) {
            dvmThrowException("Ljava/lang/IllegalMonitorStateException;",
                "object not locked by thread before notifyAll()");
            return;
        }

        /* no-op;  there are no waiters to notify.
         */
    } else {
        Monitor* mon = obj->lock.mon;

        /* It's a fat lock.
         */
        notifyAllMonitor(self, mon);
    }
}

#else  // not THIN_LOCKING

/*
 * Implements monitorenter for "synchronized" stuff.
 *
 * This does not fail or throw an exception.
 */
void dvmLockObject(Thread* self, Object* obj)
{
    Monitor* mon = obj->lock.mon;

    if (mon == NULL) {
        mon = dvmCreateMonitor(obj);
        if (!ATOMIC_CMP_SWAP((int32_t *)&obj->lock.mon,
                             (int32_t)NULL, (int32_t)mon)) {
            /* somebody else beat us to it */
            releaseMonitor(mon);
            mon = obj->lock.mon;
        }
    }

    lockMonitor(self, mon);
}

/*
 * Implements monitorexit for "synchronized" stuff.
 */
bool dvmUnlockObject(Thread* self, Object* obj)
{
    Monitor* mon = obj->lock.mon;

    return unlockMonitor(self, mon);
}


/*
 * Object.wait().
 */
void dvmObjectWait(Thread* self, Object* obj, u8 msec, u4 nsec)
{
    Monitor* mon = obj->lock.mon;

    waitMonitor(self, mon, msec, nsec);
}

/*
 * Object.notify().
 */
void dvmObjectNotify(Thread* self, Object* obj)
{
    Monitor* mon = obj->lock.mon;

    notifyMonitor(self, mon);
}

/*
 * Object.notifyAll().
 */
void dvmObjectNotifyAll(Thread* self, Object* obj)
{
    Monitor* mon = obj->lock.mon;

    notifyAllMonitor(self, mon);
}

#endif  // not THIN_LOCKING


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
 *
 * We need to increment the monitor's "interrupting" count, and set the
 * interrupted status for the thread in question.  Doing so requires
 * gaining the monitor's lock, which may not happen in a timely fashion.
 * We are left with a decision between failing to interrupt the thread
 * and stalling the interrupting thread.
 *
 * We must take some care to ensure that we don't try to interrupt the same
 * thread on the same mutex twice.  Doing so would leave us with an
 * incorrect value for Monitor.interrupting.
 */
void dvmThreadInterrupt(volatile Thread* thread)
{
    Monitor* mon;

    /*
     * Raise the "interrupted" flag.  This will cause it to bail early out
     * of the next wait() attempt, if it's not currently waiting on
     * something.
     */
    thread->interrupted = true;
    MEM_BARRIER();

    /*
     * Is the thread waiting?
     *
     * Note that fat vs. thin doesn't matter here;  waitMonitor
     * is only set when a thread actually waits on a monitor,
     * which implies that the monitor has already been fattened.
     */
    mon = thread->waitMonitor;
    if (mon == NULL)
        return;

    /*
     * Try to acquire the monitor, if we don't already own it.  We need
     * to hold the same mutex as the thread in order to signal the
     * condition it's waiting on.  When the thread goes to sleep it will
     * release the monitor's mutex, allowing us to signal it.
     *
     * TODO: we may be able to get rid of the explicit lock by coordinating
     * this more closely with waitMonitor.
     */
    Thread* self = dvmThreadSelf();
    if (!tryLockMonitor(self, mon)) {
        /*
         * Failed to get the monitor the thread is waiting on; most likely
         * the other thread is in the middle of doing something.
         */
        const int kSpinSleepTime = 500*1000;        /* 0.5s */
        u8 startWhen = dvmGetRelativeTimeUsec();
        int sleepIter = 0;

        while (dvmIterativeSleep(sleepIter++, kSpinSleepTime, startWhen)) {
            /*
             * Still time left on the clock, try to grab it again.
             */
            if (tryLockMonitor(self, mon))
                goto gotit;

            /*
             * If the target thread is no longer waiting on the same monitor,
             * the "interrupted" flag we set earlier will have caused the
             * interrupt when the thread woke up, so we can stop now.
             */
            if (thread->waitMonitor != mon)
                return;
        }

        /*
         * We have to give up or risk deadlock.
         */
        LOGW("threadid=%d: unable to interrupt threadid=%d\n",
            self->threadId, thread->threadId);
        return;
    }

gotit:
    /*
     * We've got the monitor lock, which means nobody can be added or
     * removed from the wait list.  This also means that the Thread's
     * waitMonitor/interruptingWait fields can't be modified by anyone
     * else.
     *
     * If things look good, raise flags and wake the threads sleeping
     * on the monitor's condition variable.
     */
    if (thread->waitMonitor == mon &&       // still on same monitor?
        thread->interrupted &&              // interrupt still pending?
        !thread->interruptingWait)          // nobody else is interrupting too?
    {
        int cc;

        LOGVV("threadid=%d: interrupting threadid=%d waiting on %p\n",
            self->threadId, thread->threadId, mon);

        thread->interruptingWait = true;    // prevent re-interrupt...
        mon->interrupting++;                // ...so we only do this once
        cc = pthread_cond_broadcast(&mon->cond);
        assert(cc == 0);
    }

    unlockMonitor(self, mon);
}


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
    Lock lock = parent->lock;
    if (!IS_LOCK_FAT(&lock)) {
        //LOGI("on thin\n");
        return false;
    }

    return expandObjHas(&lock.mon->historyChildren, child);
}

/*
 * Print the child list.
 */
static void dumpKids(Object* parent)
{
    Monitor* mon = parent->lock.mon;

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
    //assert(parent->lock.mon->owner == dvmThreadSelf());   // !owned for merge
    assert(IS_LOCK_FAT(&parent->lock));
    assert(IS_LOCK_FAT(&child->lock));
    assert(parent != child);
    Monitor* mon;

    mon = parent->lock.mon;
    assert(!expandObjHas(&mon->historyChildren, child));
    expandObjAddEntry(&mon->historyChildren, child);

    mon = child->lock.mon;
    assert(!expandObjHas(&mon->historyParents, parent));
    expandObjAddEntry(&mon->historyParents, parent);
}


/*
 * Remove "child" from the list of children in "parent".
 */
static void unlinkParentFromChild(Object* parent, Object* child)
{
    //assert(parent->lock.mon->owner == dvmThreadSelf());   // !owned for GC
    assert(IS_LOCK_FAT(&parent->lock));
    assert(IS_LOCK_FAT(&child->lock));
    assert(parent != child);
    Monitor* mon;

    mon = parent->lock.mon;
    if (!expandObjRemoveEntry(&mon->historyChildren, child)) {
        LOGW("WARNING: child %p not found in parent %p\n", child, parent);
    }
    assert(!expandObjHas(&mon->historyChildren, child));
    assert(expandObjCheckForDuplicates(&mon->historyChildren) < 0);

    mon = child->lock.mon;
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
    Monitor* mon = obj->lock.mon;

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
        Lock lock = child->lock;
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
            acqObj, acqObj->lock.thin >> 16);
        Monitor* newMon = dvmCreateMonitor(acqObj);
        lockMonitor(self, newMon);      // can't stall, don't need VMWAIT
        newMon->lockCount += acqObj->lock.thin >> 16;
        acqObj->lock.mon = newMon;
    }

    /* if we don't have a stack trace for this monitor, establish one */
    if (acqObj->lock.mon->historyRawStackTrace == NULL) {
        Monitor* mon = acqObj->lock.mon;
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
            mrl->obj, acqObj, mrl->obj->lock.thin >> 16);
        Monitor* newMon = dvmCreateMonitor(mrl->obj);
        lockMonitor(self, newMon);      // can't stall, don't need VMWAIT
        newMon->lockCount += mrl->obj->lock.thin >> 16;
        mrl->obj->lock.mon = newMon;
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
    mon = child->lock.mon;
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

#if 0
    /*
     * We're currently running through the entire set of known monitors.
     * This can be somewhat slow.  We may want to keep lists of parents
     * in each child to speed up GC.
     */
    mon = gDvm.monitorList;
    while (mon != NULL) {
        Object* parent = mon->obj;
        if (parent != NULL) {       /* value nulled for deleted entries */
            if (objectInChildList(parent, obj)) {
                LOGVV("removing child %p from parent %p\n", obj, parent);
                unlinkParentFromChild(parent, obj);
                mergeChildren(parent, obj);
            }
        }
        mon = mon->next;
    }
#endif

    /*
     * For every parent of this object:
     *  - merge all of our children into the parent's child list (creates
     *    a two-way link between parent and child)
     *  - remove ourselves from the parent's child list
     */
    ExpandingObjectList* pList;
    int i;

    assert(IS_LOCK_FAT(&obj->lock));
    mon = obj->lock.mon;
    pList = &mon->historyParents;
    for (i = expandBufGetCount(pList)-1; i >= 0; i--) {
        Object* parent = expandBufGetEntry(pList, i);
        Monitor* parentMon = parent->lock.mon;

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
        Monitor* childMon = child->lock.mon;

        if (!expandObjRemoveEntry(&childMon->historyParents, obj)) {
            LOGW("WARNING: parent %p not found in child %p\n", obj, child);
        }
        assert(!expandObjHas(&childMon->historyParents, obj));
    }
}

#endif /*WITH_DEADLOCK_PREDICTION*/

