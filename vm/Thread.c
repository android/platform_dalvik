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
 * Thread support.
 */
#include "Dalvik.h"
#include "native/SystemThread.h"

#include "utils/threads.h"      // need Android thread priorities

#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/mman.h>
#include <errno.h>
#include <fcntl.h>

#include <cutils/sched_policy.h>

#if defined(HAVE_PRCTL)
#include <sys/prctl.h>
#endif

/* desktop Linux needs a little help with gettid() */
#if defined(HAVE_GETTID) && !defined(HAVE_ANDROID_OS)
#define __KERNEL__
# include <linux/unistd.h>
#ifdef _syscall0
_syscall0(pid_t,gettid)
#else
pid_t gettid() { return syscall(__NR_gettid);}
#endif
#undef __KERNEL__
#endif

// Change this to enable logging on cgroup errors
#define ENABLE_CGROUP_ERR_LOGGING 0

// change this to LOGV/LOGD to debug thread activity
#define LOG_THREAD  LOGVV

/*
Notes on Threading

All threads are native pthreads.  All threads, except the JDWP debugger
thread, are visible to code running in the VM and to the debugger.  (We
don't want the debugger to try to manipulate the thread that listens for
instructions from the debugger.)  Internal VM threads are in the "system"
ThreadGroup, all others are in the "main" ThreadGroup, per convention.

The GC only runs when all threads have been suspended.  Threads are
expected to suspend themselves, using a "safe point" mechanism.  We check
for a suspend request at certain points in the main interpreter loop,
and on requests coming in from native code (e.g. all JNI functions).
Certain debugger events may inspire threads to self-suspend.

Native methods must use JNI calls to modify object references to avoid
clashes with the GC.  JNI doesn't provide a way for native code to access
arrays of objects as such -- code must always get/set individual entries --
so it should be possible to fully control access through JNI.

Internal native VM threads, such as the finalizer thread, must explicitly
check for suspension periodically.  In most cases they will be sound
asleep on a condition variable, and won't notice the suspension anyway.

Threads may be suspended by the GC, debugger, or the SIGQUIT listener
thread.  The debugger may suspend or resume individual threads, while the
GC always suspends all threads.  Each thread has a "suspend count" that
is incremented on suspend requests and decremented on resume requests.
When the count is zero, the thread is runnable.  This allows us to fulfill
a debugger requirement: if the debugger suspends a thread, the thread is
not allowed to run again until the debugger resumes it (or disconnects,
in which case we must resume all debugger-suspended threads).

Paused threads sleep on a condition variable, and are awoken en masse.
Certain "slow" VM operations, such as starting up a new thread, will be
done in a separate "VMWAIT" state, so that the rest of the VM doesn't
freeze up waiting for the operation to finish.  Threads must check for
pending suspension when leaving VMWAIT.

Because threads suspend themselves while interpreting code or when native
code makes JNI calls, there is no risk of suspending while holding internal
VM locks.  All threads can enter a suspended (or native-code-only) state.
Also, we don't have to worry about object references existing solely
in hardware registers.

We do, however, have to worry about objects that were allocated internally
and aren't yet visible to anything else in the VM.  If we allocate an
object, and then go to sleep on a mutex after changing to a non-RUNNING
state (e.g. while trying to allocate a second object), the first object
could be garbage-collected out from under us while we sleep.  To manage
this, we automatically add all allocated objects to an internal object
tracking list, and only remove them when we know we won't be suspended
before the object appears in the GC root set.

The debugger may choose to suspend or resume a single thread, which can
lead to application-level deadlocks; this is expected behavior.  The VM
will only check for suspension of single threads when the debugger is
active (the java.lang.Thread calls for this are deprecated and hence are
not supported).  Resumption of a single thread is handled by decrementing
the thread's suspend count and sending a broadcast signal to the condition
variable.  (This will cause all threads to wake up and immediately go back
to sleep, which isn't tremendously efficient, but neither is having the
debugger attached.)

The debugger is not allowed to resume threads suspended by the GC.  This
is trivially enforced by ignoring debugger requests while the GC is running
(the JDWP thread is suspended during GC).

The VM maintains a Thread struct for every pthread known to the VM.  There
is a java/lang/Thread object associated with every Thread.  At present,
there is no safe way to go from a Thread object to a Thread struct except by
locking and scanning the list; this is necessary because the lifetimes of
the two are not closely coupled.  We may want to change this behavior,
though at present the only performance impact is on the debugger (see
threadObjToThread()).  See also notes about dvmDetachCurrentThread().
*/
/*
Alternate implementation (signal-based):

Threads run without safe points -- zero overhead.  The VM uses a signal
(e.g. pthread_kill(SIGUSR1)) to notify threads of suspension or resumption.

The trouble with using signals to suspend threads is that it means a thread
can be in the middle of an operation when garbage collection starts.
To prevent some sticky situations, we have to introduce critical sections
to the VM code.

Critical sections temporarily block suspension for a given thread.
The thread must move to a non-blocked state (and self-suspend) after
finishing its current task.  If the thread blocks on a resource held
by a suspended thread, we're hosed.

One approach is to require that no blocking operations, notably
acquisition of mutexes, can be performed within a critical section.
This is too limiting.  For example, if thread A gets suspended while
holding the thread list lock, it will prevent the GC or debugger from
being able to safely access the thread list.  We need to wrap the critical
section around the entire operation (enter critical, get lock, do stuff,
release lock, exit critical).

A better approach is to declare that certain resources can only be held
within critical sections.  A thread that enters a critical section and
then gets blocked on the thread list lock knows that the thread it is
waiting for is also in a critical section, and will release the lock
before suspending itself.  Eventually all threads will complete their
operations and self-suspend.  For this to work, the VM must:

 (1) Determine the set of resources that may be accessed from the GC or
     debugger threads.  The mutexes guarding those go into the "critical
     resource set" (CRS).
 (2) Ensure that no resource in the CRS can be acquired outside of a
     critical section.  This can be verified with an assert().
 (3) Ensure that only resources in the CRS can be held while in a critical
     section.  This is harder to enforce.

If any of these conditions are not met, deadlock can ensue when grabbing
resources in the GC or debugger (#1) or waiting for threads to suspend
(#2,#3).  (You won't actually deadlock in the GC, because if the semantics
above are followed you don't need to lock anything in the GC.  The risk is
rather that the GC will access data structures in an intermediate state.)

This approach requires more care and awareness in the VM than
safe-pointing.  Because the GC and debugger are fairly intrusive, there
really aren't any internal VM resources that aren't shared.  Thus, the
enter/exit critical calls can be added to internal mutex wrappers, which
makes it easy to get #1 and #2 right.

An ordering should be established for all locks to avoid deadlocks.

Monitor locks, which are also implemented with pthread calls, should not
cause any problems here.  Threads fighting over such locks will not be in
critical sections and can be suspended freely.

This can get tricky if we ever need exclusive access to VM and non-VM
resources at the same time.  It's not clear if this is a real concern.

There are (at least) two ways to handle the incoming signals:

 (a) Always accept signals.  If we're in a critical section, the signal
     handler just returns without doing anything (the "suspend level"
     should have been incremented before the signal was sent).  Otherwise,
     if the "suspend level" is nonzero, we go to sleep.
 (b) Block signals in critical sections.  This ensures that we can't be
     interrupted in a critical section, but requires pthread_sigmask()
     calls on entry and exit.

This is a choice between blocking the message and blocking the messenger.
Because UNIX signals are unreliable (you can only know that you have been
signaled, not whether you were signaled once or 10 times), the choice is
not significant for correctness.  The choice depends on the efficiency
of pthread_sigmask() and the desire to actually block signals.  Either way,
it is best to ensure that there is only one indication of "blocked";
having two (i.e. block signals and set a flag, then only send a signal
if the flag isn't set) can lead to race conditions.

The signal handler must take care to copy registers onto the stack (via
setjmp), so that stack scans find all references.  Because we have to scan
native stacks, "exact" GC is not possible with this approach.

Some other concerns with flinging signals around:
 - Odd interactions with some debuggers (e.g. gdb on the Mac)
 - Restrictions on some standard library calls during GC (e.g. don't
   use printf on stdout to print GC debug messages)
*/

#define kMaxThreadId        ((1<<15) - 1)
#define kMainThreadId       ((1<<1) | 1)


static Thread* allocThread(int interpStackSize);
static bool prepareThread(Thread* thread);
static void setThreadSelf(Thread* thread);
static void unlinkThread(Thread* thread);
static void freeThread(Thread* thread);
static void assignThreadId(Thread* thread);
static bool createFakeEntryFrame(Thread* thread);
static bool createFakeRunFrame(Thread* thread);
static void* interpThreadStart(void* arg);
static void* internalThreadStart(void* arg);
static void threadExitUncaughtException(Thread* thread, Object* group);
static void threadExitCheck(void* arg);
static void waitForThreadSuspend(Thread* self, Thread* thread);
static int getThreadPriorityFromSystem(void);

/*
 * The JIT needs to know if any thread is suspended.  We do this by
 * maintaining a global sum of all threads' suspend counts.  All suspendCount
 * updates should go through this after aquiring threadSuspendCountLock.
 */
static inline void dvmAddToThreadSuspendCount(int *pSuspendCount, int delta)
{
    *pSuspendCount += delta;
    gDvm.sumThreadSuspendCount += delta;
}

/*
 * Initialize thread list and main thread's environment.  We need to set
 * up some basic stuff so that dvmThreadSelf() will work when we start
 * loading classes (e.g. to check for exceptions).
 */
bool dvmThreadStartup(void)
{
    Thread* thread;

    /* allocate a TLS slot */
    if (pthread_key_create(&gDvm.pthreadKeySelf, threadExitCheck) != 0) {
        LOGE("ERROR: pthread_key_create failed\n");
        return false;
    }

    /* test our pthread lib */
    if (pthread_getspecific(gDvm.pthreadKeySelf) != NULL)
        LOGW("WARNING: newly-created pthread TLS slot is not NULL\n");

    /* prep thread-related locks and conditions */
    dvmInitMutex(&gDvm.threadListLock);
    pthread_cond_init(&gDvm.threadStartCond, NULL);
    //dvmInitMutex(&gDvm.vmExitLock);
    pthread_cond_init(&gDvm.vmExitCond, NULL);
    dvmInitMutex(&gDvm._threadSuspendLock);
    dvmInitMutex(&gDvm.threadSuspendCountLock);
    pthread_cond_init(&gDvm.threadSuspendCountCond, NULL);
#ifdef WITH_DEADLOCK_PREDICTION
    dvmInitMutex(&gDvm.deadlockHistoryLock);
#endif

    /*
     * Dedicated monitor for Thread.sleep().
     * TODO: change this to an Object* so we don't have to expose this
     * call, and we interact better with JDWP monitor calls.  Requires
     * deferring the object creation to much later (e.g. final "main"
     * thread prep) or until first use.
     */
    gDvm.threadSleepMon = dvmCreateMonitor(NULL);

    gDvm.threadIdMap = dvmAllocBitVector(kMaxThreadId, false);

    thread = allocThread(gDvm.stackSize);
    if (thread == NULL)
        return false;

    /* switch mode for when we run initializers */
    thread->status = THREAD_RUNNING;

    /*
     * We need to assign the threadId early so we can lock/notify
     * object monitors.  We'll set the "threadObj" field later.
     */
    prepareThread(thread);
    gDvm.threadList = thread;

#ifdef COUNT_PRECISE_METHODS
    gDvm.preciseMethods = dvmPointerSetAlloc(200);
#endif

    return true;
}

/*
 * We're a little farther up now, and can load some basic classes.
 *
 * We're far enough along that we can poke at java.lang.Thread and friends,
 * but should not assume that static initializers have run (or cause them
 * to do so).  That means no object allocations yet.
 */
bool dvmThreadObjStartup(void)
{
    /*
     * Cache the locations of these classes.  It's likely that we're the
     * first to reference them, so they're being loaded now.
     */
    gDvm.classJavaLangThread =
        dvmFindSystemClassNoInit("Ljava/lang/Thread;");
    gDvm.classJavaLangVMThread =
        dvmFindSystemClassNoInit("Ljava/lang/VMThread;");
    gDvm.classJavaLangThreadGroup =
        dvmFindSystemClassNoInit("Ljava/lang/ThreadGroup;");
    if (gDvm.classJavaLangThread == NULL ||
        gDvm.classJavaLangThreadGroup == NULL ||
        gDvm.classJavaLangThreadGroup == NULL)
    {
        LOGE("Could not find one or more essential thread classes\n");
        return false;
    }

    /*
     * Cache field offsets.  This makes things a little faster, at the
     * expense of hard-coding non-public field names into the VM.
     */
    gDvm.offJavaLangThread_vmThread =
        dvmFindFieldOffset(gDvm.classJavaLangThread,
            "vmThread", "Ljava/lang/VMThread;");
    gDvm.offJavaLangThread_group =
        dvmFindFieldOffset(gDvm.classJavaLangThread,
            "group", "Ljava/lang/ThreadGroup;");
    gDvm.offJavaLangThread_daemon =
        dvmFindFieldOffset(gDvm.classJavaLangThread, "daemon", "Z");
    gDvm.offJavaLangThread_name =
        dvmFindFieldOffset(gDvm.classJavaLangThread,
            "name", "Ljava/lang/String;");
    gDvm.offJavaLangThread_priority =
        dvmFindFieldOffset(gDvm.classJavaLangThread, "priority", "I");

    if (gDvm.offJavaLangThread_vmThread < 0 ||
        gDvm.offJavaLangThread_group < 0 ||
        gDvm.offJavaLangThread_daemon < 0 ||
        gDvm.offJavaLangThread_name < 0 ||
        gDvm.offJavaLangThread_priority < 0)
    {
        LOGE("Unable to find all fields in java.lang.Thread\n");
        return false;
    }

    gDvm.offJavaLangVMThread_thread =
        dvmFindFieldOffset(gDvm.classJavaLangVMThread,
            "thread", "Ljava/lang/Thread;");
    gDvm.offJavaLangVMThread_vmData =
        dvmFindFieldOffset(gDvm.classJavaLangVMThread, "vmData", "I");
    if (gDvm.offJavaLangVMThread_thread < 0 ||
        gDvm.offJavaLangVMThread_vmData < 0)
    {
        LOGE("Unable to find all fields in java.lang.VMThread\n");
        return false;
    }

    /*
     * Cache the vtable offset for "run()".
     *
     * We don't want to keep the Method* because then we won't find see
     * methods defined in subclasses.
     */
    Method* meth;
    meth = dvmFindVirtualMethodByDescriptor(gDvm.classJavaLangThread, "run", "()V");
    if (meth == NULL) {
        LOGE("Unable to find run() in java.lang.Thread\n");
        return false;
    }
    gDvm.voffJavaLangThread_run = meth->methodIndex;

    /*
     * Cache vtable offsets for ThreadGroup methods.
     */
    meth = dvmFindVirtualMethodByDescriptor(gDvm.classJavaLangThreadGroup,
        "removeThread", "(Ljava/lang/Thread;)V");
    if (meth == NULL) {
        LOGE("Unable to find removeThread(Thread) in java.lang.ThreadGroup\n");
        return false;
    }
    gDvm.voffJavaLangThreadGroup_removeThread = meth->methodIndex;

    return true;
}

/*
 * All threads should be stopped by now.  Clean up some thread globals.
 */
void dvmThreadShutdown(void)
{
    if (gDvm.threadList != NULL) {
        /*
         * If we walk through the thread list and try to free the
         * lingering thread structures (which should only be for daemon
         * threads), the daemon threads may crash if they execute before
         * the process dies.  Let them leak.
         */
        freeThread(gDvm.threadList);
        gDvm.threadList = NULL;
    }

    dvmFreeBitVector(gDvm.threadIdMap);

    dvmFreeMonitorList();

    pthread_key_delete(gDvm.pthreadKeySelf);
}


/*
 * Grab the suspend count global lock.
 */
static inline void lockThreadSuspendCount(void)
{
    /*
     * Don't try to change to VMWAIT here.  When we change back to RUNNING
     * we have to check for a pending suspend, which results in grabbing
     * this lock recursively.  Doesn't work with "fast" pthread mutexes.
     *
     * This lock is always held for very brief periods, so as long as
     * mutex ordering is respected we shouldn't stall.
     */
    int cc = pthread_mutex_lock(&gDvm.threadSuspendCountLock);
    assert(cc == 0);
}

/*
 * Release the suspend count global lock.
 */
static inline void unlockThreadSuspendCount(void)
{
    dvmUnlockMutex(&gDvm.threadSuspendCountLock);
}

/*
 * Grab the thread list global lock.
 *
 * This is held while "suspend all" is trying to make everybody stop.  If
 * the shutdown is in progress, and somebody tries to grab the lock, they'll
 * have to wait for the GC to finish.  Therefore it's important that the
 * thread not be in RUNNING mode.
 *
 * We don't have to check to see if we should be suspended once we have
 * the lock.  Nobody can suspend all threads without holding the thread list
 * lock while they do it, so by definition there isn't a GC in progress.
 *
 * TODO: consider checking for suspend after acquiring the lock, and
 * backing off if set.  As stated above, it can't happen during normal
 * execution, but it *can* happen during shutdown when daemon threads
 * are being suspended.
 */
void dvmLockThreadList(Thread* self)
{
    ThreadStatus oldStatus;

    if (self == NULL)       /* try to get it from TLS */
        self = dvmThreadSelf();

    if (self != NULL) {
        oldStatus = self->status;
        self->status = THREAD_VMWAIT;
    } else {
        /* happens during VM shutdown */
        //LOGW("NULL self in dvmLockThreadList\n");
        oldStatus = -1;         // shut up gcc
    }

    int cc = pthread_mutex_lock(&gDvm.threadListLock);
    assert(cc == 0);

    if (self != NULL)
        self->status = oldStatus;
}

/*
 * Release the thread list global lock.
 */
void dvmUnlockThreadList(void)
{
    int cc = pthread_mutex_unlock(&gDvm.threadListLock);
    assert(cc == 0);
}

/*
 * Convert SuspendCause to a string.
 */
static const char* getSuspendCauseStr(SuspendCause why)
{
    switch (why) {
    case SUSPEND_NOT:               return "NOT?";
    case SUSPEND_FOR_GC:            return "gc";
    case SUSPEND_FOR_DEBUG:         return "debug";
    case SUSPEND_FOR_DEBUG_EVENT:   return "debug-event";
    case SUSPEND_FOR_STACK_DUMP:    return "stack-dump";
    default:                        return "UNKNOWN";
    }
}

/*
 * Grab the "thread suspend" lock.  This is required to prevent the
 * GC and the debugger from simultaneously suspending all threads.
 *
 * If we fail to get the lock, somebody else is trying to suspend all
 * threads -- including us.  If we go to sleep on the lock we'll deadlock
 * the VM.  Loop until we get it or somebody puts us to sleep.
 */
static void lockThreadSuspend(const char* who, SuspendCause why)
{
    const int kSpinSleepTime = 3*1000*1000;        /* 3s */
    u8 startWhen = 0;       // init req'd to placate gcc
    int sleepIter = 0;
    int cc;

    do {
        cc = pthread_mutex_trylock(&gDvm._threadSuspendLock);
        if (cc != 0) {
            if (!dvmCheckSuspendPending(NULL)) {
                /*
                 * Could be that a resume-all is in progress, and something
                 * grabbed the CPU when the wakeup was broadcast.  The thread
                 * performing the resume hasn't had a chance to release the
                 * thread suspend lock.  (We release before the broadcast,
                 * so this should be a narrow window.)
                 *
                 * Could be we hit the window as a suspend was started,
                 * and the lock has been grabbed but the suspend counts
                 * haven't been incremented yet.
                 *
                 * Could be an unusual JNI thread-attach thing.
                 *
                 * Could be the debugger telling us to resume at roughly
                 * the same time we're posting an event.
                 */
                LOGI("threadid=%d ODD: want thread-suspend lock (%s:%s),"
                     " it's held, no suspend pending\n",
                    dvmThreadSelf()->threadId, who, getSuspendCauseStr(why));
            } else {
                /* we suspended; reset timeout */
                sleepIter = 0;
            }

            /* give the lock-holder a chance to do some work */
            if (sleepIter == 0)
                startWhen = dvmGetRelativeTimeUsec();
            if (!dvmIterativeSleep(sleepIter++, kSpinSleepTime, startWhen)) {
                LOGE("threadid=%d: couldn't get thread-suspend lock (%s:%s),"
                     " bailing\n",
                    dvmThreadSelf()->threadId, who, getSuspendCauseStr(why));
                /* threads are not suspended, thread dump could crash */
                dvmDumpAllThreads(false);
                dvmAbort();
            }
        }
    } while (cc != 0);
    assert(cc == 0);
}

/*
 * Release the "thread suspend" lock.
 */
static inline void unlockThreadSuspend(void)
{
    int cc = pthread_mutex_unlock(&gDvm._threadSuspendLock);
    assert(cc == 0);
}


/*
 * Kill any daemon threads that still exist.  All of ours should be
 * stopped, so these should be Thread objects or JNI-attached threads
 * started by the application.  Actively-running threads are likely
 * to crash the process if they continue to execute while the VM
 * shuts down, so we really need to kill or suspend them.  (If we want
 * the VM to restart within this process, we need to kill them, but that
 * leaves open the possibility of orphaned resources.)
 *
 * Waiting for the thread to suspend may be unwise at this point, but
 * if one of these is wedged in a critical section then we probably
 * would've locked up on the last GC attempt.
 *
 * It's possible for this function to get called after a failed
 * initialization, so be careful with assumptions about the environment.
 *
 * This will be called from whatever thread calls DestroyJavaVM, usually
 * but not necessarily the main thread.  It's likely, but not guaranteed,
 * that the current thread has already been cleaned up.
 */
void dvmSlayDaemons(void)
{
    Thread* self = dvmThreadSelf();     // may be null
    Thread* target;
    int threadId = 0;
    bool doWait = false;

    //dvmEnterCritical(self);
    dvmLockThreadList(self);

    if (self != NULL)
        threadId = self->threadId;

    target = gDvm.threadList;
    while (target != NULL) {
        if (target == self) {
            target = target->next;
            continue;
        }

        if (!dvmGetFieldBoolean(target->threadObj,
                gDvm.offJavaLangThread_daemon))
        {
            /* should never happen; suspend it with the rest */
            LOGW("threadid=%d: non-daemon id=%d still running at shutdown?!\n",
                threadId, target->threadId);
        }

        char* threadName = dvmGetThreadName(target);
        LOGD("threadid=%d: suspending daemon id=%d name='%s'\n",
            threadId, target->threadId, threadName);
        free(threadName);

        /* mark as suspended */
        lockThreadSuspendCount();
        dvmAddToThreadSuspendCount(&target->suspendCount, 1);
        unlockThreadSuspendCount();
        doWait = true;

        target = target->next;
    }

    //dvmDumpAllThreads(false);

    /*
     * Unlock the thread list, relocking it later if necessary.  It's
     * possible a thread is in VMWAIT after calling dvmLockThreadList,
     * and that function *doesn't* check for pending suspend after
     * acquiring the lock.  We want to let them finish their business
     * and see the pending suspend before we continue here.
     *
     * There's no guarantee of mutex fairness, so this might not work.
     * (The alternative is to have dvmLockThreadList check for suspend
     * after acquiring the lock and back off, something we should consider.)
     */
    dvmUnlockThreadList();

    if (doWait) {
        usleep(200 * 1000);

        dvmLockThreadList(self);

        /*
         * Sleep for a bit until the threads have suspended.  We're trying
         * to exit, so don't wait for too long.
         */
        int i;
        for (i = 0; i < 10; i++) {
            bool allSuspended = true;

            target = gDvm.threadList;
            while (target != NULL) {
                if (target == self) {
                    target = target->next;
                    continue;
                }

                if (target->status == THREAD_RUNNING && !target->isSuspended) {
                    LOGD("threadid=%d not ready yet\n", target->threadId);
                    allSuspended = false;
                    break;
                }

                target = target->next;
            }

            if (allSuspended) {
                LOGD("threadid=%d: all daemons have suspended\n", threadId);
                break;
            } else {
                LOGD("threadid=%d: waiting for daemons to suspend\n", threadId);
            }

            usleep(200 * 1000);
        }
        dvmUnlockThreadList();
    }

#if 0   /* bad things happen if they come out of JNI or "spuriously" wake up */
    /*
     * Abandon the threads and recover their resources.
     */
    target = gDvm.threadList;
    while (target != NULL) {
        Thread* nextTarget = target->next;
        unlinkThread(target);
        freeThread(target);
        target = nextTarget;
    }
#endif

    //dvmDumpAllThreads(true);
}


/*
 * Finish preparing the parts of the Thread struct required to support
 * JNI registration.
 */
bool dvmPrepMainForJni(JNIEnv* pEnv)
{
    Thread* self;

    /* main thread is always first in list at this point */
    self = gDvm.threadList;
    assert(self->threadId == kMainThreadId);

    /* create a "fake" JNI frame at the top of the main thread interp stack */
    if (!createFakeEntryFrame(self))
        return false;

    /* fill these in, since they weren't ready at dvmCreateJNIEnv time */
    dvmSetJniEnvThreadId(pEnv, self);
    dvmSetThreadJNIEnv(self, (JNIEnv*) pEnv);

    return true;
}


/*
 * Finish preparing the main thread, allocating some objects to represent
 * it.  As part of doing so, we finish initializing Thread and ThreadGroup.
 * This will execute some interpreted code (e.g. class initializers).
 */
bool dvmPrepMainThread(void)
{
    Thread* thread;
    Object* groupObj;
    Object* threadObj;
    Object* vmThreadObj;
    StringObject* threadNameStr;
    Method* init;
    JValue unused;

    LOGV("+++ finishing prep on main VM thread\n");

    /* main thread is always first in list at this point */
    thread = gDvm.threadList;
    assert(thread->threadId == kMainThreadId);

    /*
     * Make sure the classes are initialized.  We have to do this before
     * we create an instance of them.
     */
    if (!dvmInitClass(gDvm.classJavaLangClass)) {
        LOGE("'Class' class failed to initialize\n");
        return false;
    }
    if (!dvmInitClass(gDvm.classJavaLangThreadGroup) ||
        !dvmInitClass(gDvm.classJavaLangThread) ||
        !dvmInitClass(gDvm.classJavaLangVMThread))
    {
        LOGE("thread classes failed to initialize\n");
        return false;
    }

    groupObj = dvmGetMainThreadGroup();
    if (groupObj == NULL)
        return false;

    /*
     * Allocate and construct a Thread with the internal-creation
     * constructor.
     */
    threadObj = dvmAllocObject(gDvm.classJavaLangThread, ALLOC_DEFAULT);
    if (threadObj == NULL) {
        LOGE("unable to allocate main thread object\n");
        return false;
    }
    dvmReleaseTrackedAlloc(threadObj, NULL);

    threadNameStr = dvmCreateStringFromCstr("main", ALLOC_DEFAULT);
    if (threadNameStr == NULL)
        return false;
    dvmReleaseTrackedAlloc((Object*)threadNameStr, NULL);

    init = dvmFindDirectMethodByDescriptor(gDvm.classJavaLangThread, "<init>",
            "(Ljava/lang/ThreadGroup;Ljava/lang/String;IZ)V");
    assert(init != NULL);
    dvmCallMethod(thread, init, threadObj, &unused, groupObj, threadNameStr,
        THREAD_NORM_PRIORITY, false);
    if (dvmCheckException(thread)) {
        LOGE("exception thrown while constructing main thread object\n");
        return false;
    }

    /*
     * Allocate and construct a VMThread.
     */
    vmThreadObj = dvmAllocObject(gDvm.classJavaLangVMThread, ALLOC_DEFAULT);
    if (vmThreadObj == NULL) {
        LOGE("unable to allocate main vmthread object\n");
        return false;
    }
    dvmReleaseTrackedAlloc(vmThreadObj, NULL);

    init = dvmFindDirectMethodByDescriptor(gDvm.classJavaLangVMThread, "<init>",
            "(Ljava/lang/Thread;)V");
    dvmCallMethod(thread, init, vmThreadObj, &unused, threadObj);
    if (dvmCheckException(thread)) {
        LOGE("exception thrown while constructing main vmthread object\n");
        return false;
    }

    /* set the VMThread.vmData field to our Thread struct */
    assert(gDvm.offJavaLangVMThread_vmData != 0);
    dvmSetFieldInt(vmThreadObj, gDvm.offJavaLangVMThread_vmData, (u4)thread);

    /*
     * Stuff the VMThread back into the Thread.  From this point on, other
     * Threads will see that this Thread is running (at least, they would,
     * if there were any).
     */
    dvmSetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread,
        vmThreadObj);

    thread->threadObj = threadObj;

    /*
     * Set the context class loader.  This invokes a ClassLoader method,
     * which could conceivably call Thread.currentThread(), so we want the
     * Thread to be fully configured before we do this.
     */
    Object* systemLoader = dvmGetSystemClassLoader();
    if (systemLoader == NULL) {
        LOGW("WARNING: system class loader is NULL (setting main ctxt)\n");
        /* keep going */
    }
    int ctxtClassLoaderOffset = dvmFindFieldOffset(gDvm.classJavaLangThread,
        "contextClassLoader", "Ljava/lang/ClassLoader;");
    if (ctxtClassLoaderOffset < 0) {
        LOGE("Unable to find contextClassLoader field in Thread\n");
        return false;
    }
    dvmSetFieldObject(threadObj, ctxtClassLoaderOffset, systemLoader);

    /*
     * Finish our thread prep.
     */

    /* include self in non-daemon threads (mainly for AttachCurrentThread) */
    gDvm.nonDaemonThreadCount++;

    return true;
}


/*
 * Alloc and initialize a Thread struct.
 *
 * "threadObj" is the java.lang.Thread object.  It will be NULL for the
 * main VM thread, but non-NULL for everything else.
 *
 * Does not create any objects, just stuff on the system (malloc) heap.  (If
 * this changes, we need to use ALLOC_NO_GC.  And also verify that we're
 * ready to load classes at the time this is called.)
 */
static Thread* allocThread(int interpStackSize)
{
    Thread* thread;
    u1* stackBottom;

    thread = (Thread*) calloc(1, sizeof(Thread));
    if (thread == NULL)
        return NULL;

    assert(interpStackSize >= kMinStackSize && interpStackSize <=kMaxStackSize);

    thread->status = THREAD_INITIALIZING;
    thread->suspendCount = 0;

#ifdef WITH_ALLOC_LIMITS
    thread->allocLimit = -1;
#endif

    /*
     * Allocate and initialize the interpreted code stack.  We essentially
     * "lose" the alloc pointer, which points at the bottom of the stack,
     * but we can get it back later because we know how big the stack is.
     *
     * The stack must be aligned on a 4-byte boundary.
     */
#ifdef MALLOC_INTERP_STACK
    stackBottom = (u1*) malloc(interpStackSize);
    if (stackBottom == NULL) {
        free(thread);
        return NULL;
    }
    memset(stackBottom, 0xc5, interpStackSize);     // stop valgrind complaints
#else
    stackBottom = mmap(NULL, interpStackSize, PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANON, -1, 0);
    if (stackBottom == MAP_FAILED) {
        free(thread);
        return NULL;
    }
#endif

    assert(((u4)stackBottom & 0x03) == 0); // looks like our malloc ensures this
    thread->interpStackSize = interpStackSize;
    thread->interpStackStart = stackBottom + interpStackSize;
    thread->interpStackEnd = stackBottom + STACK_OVERFLOW_RESERVE;

    /* give the thread code a chance to set things up */
    dvmInitInterpStack(thread, interpStackSize);

    return thread;
}

/*
 * Get a meaningful thread ID.  At present this only has meaning under Linux,
 * where getpid() and gettid() sometimes agree and sometimes don't depending
 * on your thread model (try "export LD_ASSUME_KERNEL=2.4.19").
 */
pid_t dvmGetSysThreadId(void)
{
#ifdef HAVE_GETTID
    return gettid();
#else
    return getpid();
#endif
}

/*
 * Finish initialization of a Thread struct.
 *
 * This must be called while executing in the new thread, but before the
 * thread is added to the thread list.
 *
 * *** NOTE: The threadListLock must be held by the caller (needed for
 * assignThreadId()).
 */
static bool prepareThread(Thread* thread)
{
    assignThreadId(thread);
    thread->handle = pthread_self();
    thread->systemTid = dvmGetSysThreadId();

    //LOGI("SYSTEM TID IS %d (pid is %d)\n", (int) thread->systemTid,
    //    (int) getpid());
    setThreadSelf(thread);

    LOGV("threadid=%d: interp stack at %p\n",
        thread->threadId, thread->interpStackStart - thread->interpStackSize);

    /*
     * Initialize invokeReq.
     */
    pthread_mutex_init(&thread->invokeReq.lock, NULL);
    pthread_cond_init(&thread->invokeReq.cv, NULL);

    /*
     * Initialize our reference tracking tables.
     *
     * Most threads won't use jniMonitorRefTable, so we clear out the
     * structure but don't call the init function (which allocs storage).
     */
#ifdef USE_INDIRECT_REF
    if (!dvmInitIndirectRefTable(&thread->jniLocalRefTable,
            kJniLocalRefMin, kJniLocalRefMax, kIndirectKindLocal))
        return false;
#else
    /*
     * The JNI local ref table *must* be fixed-size because we keep pointers
     * into the table in our stack frames.
     */
    if (!dvmInitReferenceTable(&thread->jniLocalRefTable,
            kJniLocalRefMax, kJniLocalRefMax))
        return false;
#endif
    if (!dvmInitReferenceTable(&thread->internalLocalRefTable,
            kInternalRefDefault, kInternalRefMax))
        return false;

    memset(&thread->jniMonitorRefTable, 0, sizeof(thread->jniMonitorRefTable));

    return true;
}

/*
 * Remove a thread from the internal list.
 * Clear out the links to make it obvious that the thread is
 * no longer on the list.  Caller must hold gDvm.threadListLock.
 */
static void unlinkThread(Thread* thread)
{
    LOG_THREAD("threadid=%d: removing from list\n", thread->threadId);
    if (thread == gDvm.threadList) {
        assert(thread->prev == NULL);
        gDvm.threadList = thread->next;
    } else {
        assert(thread->prev != NULL);
        thread->prev->next = thread->next;
    }
    if (thread->next != NULL)
        thread->next->prev = thread->prev;
    thread->prev = thread->next = NULL;
}

/*
 * Free a Thread struct, and all the stuff allocated within.
 */
static void freeThread(Thread* thread)
{
    if (thread == NULL)
        return;

    /* thread->threadId is zero at this point */
    LOGVV("threadid=%d: freeing\n", thread->threadId);

    if (thread->interpStackStart != NULL) {
        u1* interpStackBottom;

        interpStackBottom = thread->interpStackStart;
        interpStackBottom -= thread->interpStackSize;
#ifdef MALLOC_INTERP_STACK
        free(interpStackBottom);
#else
        if (munmap(interpStackBottom, thread->interpStackSize) != 0)
            LOGW("munmap(thread stack) failed\n");
#endif
    }

#ifdef USE_INDIRECT_REF
    dvmClearIndirectRefTable(&thread->jniLocalRefTable);
#else
    dvmClearReferenceTable(&thread->jniLocalRefTable);
#endif
    dvmClearReferenceTable(&thread->internalLocalRefTable);
    if (&thread->jniMonitorRefTable.table != NULL)
        dvmClearReferenceTable(&thread->jniMonitorRefTable);

    free(thread);
}

/*
 * Like pthread_self(), but on a Thread*.
 */
Thread* dvmThreadSelf(void)
{
    return (Thread*) pthread_getspecific(gDvm.pthreadKeySelf);
}

/*
 * Explore our sense of self.  Stuffs the thread pointer into TLS.
 */
static void setThreadSelf(Thread* thread)
{
    int cc;

    cc = pthread_setspecific(gDvm.pthreadKeySelf, thread);
    if (cc != 0) {
        /*
         * Sometimes this fails under Bionic with EINVAL during shutdown.
         * This can happen if the timing is just right, e.g. a thread
         * fails to attach during shutdown, but the "fail" path calls
         * here to ensure we clean up after ourselves.
         */
        if (thread != NULL) {
            LOGE("pthread_setspecific(%p) failed, err=%d\n", thread, cc);
            dvmAbort();     /* the world is fundamentally hosed */
        }
    }
}

/*
 * This is associated with the pthreadKeySelf key.  It's called by the
 * pthread library when a thread is exiting and the "self" pointer in TLS
 * is non-NULL, meaning the VM hasn't had a chance to clean up.  In normal
 * operation this should never be called.
 *
 * This is mainly of use to ensure that we don't leak resources if, for
 * example, a thread attaches itself to us with AttachCurrentThread and
 * then exits without notifying the VM.
 *
 * We could do the detach here instead of aborting, but this will lead to
 * portability problems.  Other implementations do not do this check and
 * will simply be unaware that the thread has exited, leading to resource
 * leaks (and, if this is a non-daemon thread, an infinite hang when the
 * VM tries to shut down).
 */
static void threadExitCheck(void* arg)
{
    Thread* thread = (Thread*) arg;

    LOGI("In threadExitCheck %p\n", arg);
    assert(thread != NULL);

    if (thread->status != THREAD_ZOMBIE) {
        LOGE("Native thread exited without telling us\n");
        dvmAbort();
    }
}


/*
 * Assign the threadId.  This needs to be a small integer so that our
 * "thin" locks fit in a small number of bits.
 *
 * We reserve zero for use as an invalid ID.
 *
 * This must be called with threadListLock held (unless we're still
 * initializing the system).
 */
static void assignThreadId(Thread* thread)
{
    /* Find a small unique integer.  threadIdMap is a vector of
     * kMaxThreadId bits;  dvmAllocBit() returns the index of a
     * bit, meaning that it will always be < kMaxThreadId.
     *
     * The thin locking magic requires that the low bit is always
     * set, so we do it once, here.
     */
    int num = dvmAllocBit(gDvm.threadIdMap);
    if (num < 0) {
        LOGE("Ran out of thread IDs\n");
        dvmAbort();     // TODO: make this a non-fatal error result
    }

    thread->threadId = ((num + 1) << 1) | 1;

    assert(thread->threadId != 0);
    assert(thread->threadId != DVM_LOCK_INITIAL_THIN_VALUE);
}

/*
 * Give back the thread ID.
 */
static void releaseThreadId(Thread* thread)
{
    assert(thread->threadId > 0);
    dvmClearBit(gDvm.threadIdMap, (thread->threadId >> 1) - 1);
    thread->threadId = 0;
}


/*
 * Add a stack frame that makes it look like the native code in the main
 * thread was originally invoked from interpreted code.  This gives us a
 * place to hang JNI local references.  The VM spec says (v2 5.2) that the
 * VM begins by executing "main" in a class, so in a way this brings us
 * closer to the spec.
 */
static bool createFakeEntryFrame(Thread* thread)
{
    assert(thread->threadId == kMainThreadId);      // main thread only

    /* find the method on first use */
    if (gDvm.methFakeNativeEntry == NULL) {
        ClassObject* nativeStart;
        Method* mainMeth;

        nativeStart = dvmFindSystemClassNoInit(
                "Ldalvik/system/NativeStart;");
        if (nativeStart == NULL) {
            LOGE("Unable to find dalvik.system.NativeStart class\n");
            return false;
        }

        /*
         * Because we are creating a frame that represents application code, we
         * want to stuff the application class loader into the method's class
         * loader field, even though we're using the system class loader to
         * load it.  This makes life easier over in JNI FindClass (though it
         * could bite us in other ways).
         *
         * Unfortunately this is occurring too early in the initialization,
         * of necessity coming before JNI is initialized, and we're not quite
         * ready to set up the application class loader.
         *
         * So we save a pointer to the method in gDvm.methFakeNativeEntry
         * and check it in FindClass.  The method is private so nobody else
         * can call it.
         */
        //nativeStart->classLoader = dvmGetSystemClassLoader();

        mainMeth = dvmFindDirectMethodByDescriptor(nativeStart,
                    "main", "([Ljava/lang/String;)V");
        if (mainMeth == NULL) {
            LOGE("Unable to find 'main' in dalvik.system.NativeStart\n");
            return false;
        }

        gDvm.methFakeNativeEntry = mainMeth;
    }

    return dvmPushJNIFrame(thread, gDvm.methFakeNativeEntry);
}


/*
 * Add a stack frame that makes it look like the native thread has been
 * executing interpreted code.  This gives us a place to hang JNI local
 * references.
 */
static bool createFakeRunFrame(Thread* thread)
{
    ClassObject* nativeStart;
    Method* runMeth;

    assert(thread->threadId != 1);      // not for main thread

    nativeStart =
        dvmFindSystemClassNoInit("Ldalvik/system/NativeStart;");
    if (nativeStart == NULL) {
        LOGE("Unable to find dalvik.system.NativeStart class\n");
        return false;
    }

    runMeth = dvmFindVirtualMethodByDescriptor(nativeStart, "run", "()V");
    if (runMeth == NULL) {
        LOGE("Unable to find 'run' in dalvik.system.NativeStart\n");
        return false;
    }

    return dvmPushJNIFrame(thread, runMeth);
}

/*
 * Helper function to set the name of the current thread
 */
static void setThreadName(const char *threadName)
{
#if defined(HAVE_PRCTL)
    int hasAt = 0;
    int hasDot = 0;
    const char *s = threadName;
    while (*s) {
        if (*s == '.') hasDot = 1;
        else if (*s == '@') hasAt = 1;
        s++;
    }
    int len = s - threadName;
    if (len < 15 || hasAt || !hasDot) {
        s = threadName;
    } else {
        s = threadName + len - 15;
    }
    prctl(PR_SET_NAME, (unsigned long) s, 0, 0, 0);
#endif
}

/*
 * Create a thread as a result of java.lang.Thread.start().
 *
 * We do have to worry about some concurrency problems, e.g. programs
 * that try to call Thread.start() on the same object from multiple threads.
 * (This will fail for all but one, but we have to make sure that it succeeds
 * for exactly one.)
 *
 * Some of the complexity here arises from our desire to mimic the
 * Thread vs. VMThread class decomposition we inherited.  We've been given
 * a Thread, and now we need to create a VMThread and then populate both
 * objects.  We also need to create one of our internal Thread objects.
 *
 * Pass in a stack size of 0 to get the default.
 */
bool dvmCreateInterpThread(Object* threadObj, int reqStackSize)
{
    pthread_attr_t threadAttr;
    pthread_t threadHandle;
    Thread* self;
    Thread* newThread = NULL;
    Object* vmThreadObj = NULL;
    int stackSize;

    assert(threadObj != NULL);

    if(gDvm.zygote) {
        // Allow the sampling profiler thread. We shut it down before forking.
        StringObject* nameStr = (StringObject*) dvmGetFieldObject(threadObj,
                    gDvm.offJavaLangThread_name);
        char* threadName = dvmCreateCstrFromString(nameStr);
        bool profilerThread = strcmp(threadName, "SamplingProfiler") == 0;
        free(threadName);
        if (!profilerThread) {
            dvmThrowException("Ljava/lang/IllegalStateException;",
                "No new threads in -Xzygote mode");

            goto fail;
        }
    }

    self = dvmThreadSelf();
    if (reqStackSize == 0)
        stackSize = gDvm.stackSize;
    else if (reqStackSize < kMinStackSize)
        stackSize = kMinStackSize;
    else if (reqStackSize > kMaxStackSize)
        stackSize = kMaxStackSize;
    else
        stackSize = reqStackSize;

    pthread_attr_init(&threadAttr);
    pthread_attr_setdetachstate(&threadAttr, PTHREAD_CREATE_DETACHED);

    /*
     * To minimize the time spent in the critical section, we allocate the
     * vmThread object here.
     */
    vmThreadObj = dvmAllocObject(gDvm.classJavaLangVMThread, ALLOC_DEFAULT);
    if (vmThreadObj == NULL)
        goto fail;

    newThread = allocThread(stackSize);
    if (newThread == NULL)
        goto fail;
    newThread->threadObj = threadObj;

    assert(newThread->status == THREAD_INITIALIZING);

    /*
     * We need to lock out other threads while we test and set the
     * "vmThread" field in java.lang.Thread, because we use that to determine
     * if this thread has been started before.  We use the thread list lock
     * because it's handy and we're going to need to grab it again soon
     * anyway.
     */
    dvmLockThreadList(self);

    if (dvmGetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread) != NULL) {
        dvmUnlockThreadList();
        dvmThrowException("Ljava/lang/IllegalThreadStateException;",
            "thread has already been started");
        goto fail;
    }

    /*
     * There are actually three data structures: Thread (object), VMThread
     * (object), and Thread (C struct).  All of them point to at least one
     * other.
     *
     * As soon as "VMThread.vmData" is assigned, other threads can start
     * making calls into us (e.g. setPriority).
     */
    dvmSetFieldInt(vmThreadObj, gDvm.offJavaLangVMThread_vmData, (u4)newThread);
    dvmSetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread, vmThreadObj);

    /*
     * Thread creation might take a while, so release the lock.
     */
    dvmUnlockThreadList();

    int cc, oldStatus;
    oldStatus = dvmChangeStatus(self, THREAD_VMWAIT);
    cc = pthread_create(&threadHandle, &threadAttr, interpThreadStart,
            newThread);
    oldStatus = dvmChangeStatus(self, oldStatus);

    if (cc != 0) {
        /*
         * Failure generally indicates that we have exceeded system
         * resource limits.  VirtualMachineError is probably too severe,
         * so use OutOfMemoryError.
         */
        LOGE("Thread creation failed (err=%s)\n", strerror(errno));

        dvmSetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread, NULL);

        dvmThrowException("Ljava/lang/OutOfMemoryError;",
            "thread creation failed");
        goto fail;
    }

    /*
     * We need to wait for the thread to start.  Otherwise, depending on
     * the whims of the OS scheduler, we could return and the code in our
     * thread could try to do operations on the new thread before it had
     * finished starting.
     *
     * The new thread will lock the thread list, change its state to
     * THREAD_STARTING, broadcast to gDvm.threadStartCond, and then sleep
     * on gDvm.threadStartCond (which uses the thread list lock).  This
     * thread (the parent) will either see that the thread is already ready
     * after we grab the thread list lock, or will be awakened from the
     * condition variable on the broadcast.
     *
     * We don't want to stall the rest of the VM while the new thread
     * starts, which can happen if the GC wakes up at the wrong moment.
     * So, we change our own status to VMWAIT, and self-suspend if
     * necessary after we finish adding the new thread.
     *
     *
     * We have to deal with an odd race with the GC/debugger suspension
     * mechanism when creating a new thread.  The information about whether
     * or not a thread should be suspended is contained entirely within
     * the Thread struct; this is usually cleaner to deal with than having
     * one or more globally-visible suspension flags.  The trouble is that
     * we could create the thread while the VM is trying to suspend all
     * threads.  The suspend-count won't be nonzero for the new thread,
     * so dvmChangeStatus(THREAD_RUNNING) won't cause a suspension.
     *
     * The easiest way to deal with this is to prevent the new thread from
     * running until the parent says it's okay.  This results in the
     * following (correct) sequence of events for a "badly timed" GC
     * (where '-' is us, 'o' is the child, and '+' is some other thread):
     *
     *  - call pthread_create()
     *  - lock thread list
     *  - put self into THREAD_VMWAIT so GC doesn't wait for us
     *  - sleep on condition var (mutex = thread list lock) until child starts
     *  + GC triggered by another thread
     *  + thread list locked; suspend counts updated; thread list unlocked
     *  + loop waiting for all runnable threads to suspend
     *  + success, start GC
     *  o child thread wakes, signals condition var to wake parent
     *  o child waits for parent ack on condition variable
     *  - we wake up, locking thread list
     *  - add child to thread list
     *  - unlock thread list
     *  - change our state back to THREAD_RUNNING; GC causes us to suspend
     *  + GC finishes; all threads in thread list are resumed
     *  - lock thread list
     *  - set child to THREAD_VMWAIT, and signal it to start
     *  - unlock thread list
     *  o child resumes
     *  o child changes state to THREAD_RUNNING
     *
     * The above shows the GC starting up during thread creation, but if
     * it starts anywhere after VMThread.create() is called it will
     * produce the same series of events.
     *
     * Once the child is in the thread list, it will be suspended and
     * resumed like any other thread.  In the above scenario the resume-all
     * code will try to resume the new thread, which was never actually
     * suspended, and try to decrement the child's thread suspend count to -1.
     * We can catch this in the resume-all code.
     *
     * Bouncing back and forth between threads like this adds a small amount
     * of scheduler overhead to thread startup.
     *
     * One alternative to having the child wait for the parent would be
     * to have the child inherit the parents' suspension count.  This
     * would work for a GC, since we can safely assume that the parent
     * thread didn't cause it, but we must only do so if the parent suspension
     * was caused by a suspend-all.  If the parent was being asked to
     * suspend singly by the debugger, the child should not inherit the value.
     *
     * We could also have a global "new thread suspend count" that gets
     * picked up by new threads before changing state to THREAD_RUNNING.
     * This would be protected by the thread list lock and set by a
     * suspend-all.
     */
    dvmLockThreadList(self);
    assert(self->status == THREAD_RUNNING);
    self->status = THREAD_VMWAIT;
    while (newThread->status != THREAD_STARTING)
        pthread_cond_wait(&gDvm.threadStartCond, &gDvm.threadListLock);

    LOG_THREAD("threadid=%d: adding to list\n", newThread->threadId);
    newThread->next = gDvm.threadList->next;
    if (newThread->next != NULL)
        newThread->next->prev = newThread;
    newThread->prev = gDvm.threadList;
    gDvm.threadList->next = newThread;

    if (!dvmGetFieldBoolean(threadObj, gDvm.offJavaLangThread_daemon))
        gDvm.nonDaemonThreadCount++;        // guarded by thread list lock

    dvmUnlockThreadList();

    /* change status back to RUNNING, self-suspending if necessary */
    dvmChangeStatus(self, THREAD_RUNNING);

    /*
     * Tell the new thread to start.
     *
     * We must hold the thread list lock before messing with another thread.
     * In the general case we would also need to verify that newThread was
     * still in the thread list, but in our case the thread has not started
     * executing user code and therefore has not had a chance to exit.
     *
     * We move it to VMWAIT, and it then shifts itself to RUNNING, which
     * comes with a suspend-pending check.
     */
    dvmLockThreadList(self);

    assert(newThread->status == THREAD_STARTING);
    newThread->status = THREAD_VMWAIT;
    pthread_cond_broadcast(&gDvm.threadStartCond);

    dvmUnlockThreadList();

    dvmReleaseTrackedAlloc(vmThreadObj, NULL);
    return true;

fail:
    freeThread(newThread);
    dvmReleaseTrackedAlloc(vmThreadObj, NULL);
    return false;
}

/*
 * pthread entry function for threads started from interpreted code.
 */
static void* interpThreadStart(void* arg)
{
    Thread* self = (Thread*) arg;

    char *threadName = dvmGetThreadName(self);
    setThreadName(threadName);
    free(threadName);

    /*
     * Finish initializing the Thread struct.
     */
    prepareThread(self);

    LOG_THREAD("threadid=%d: created from interp\n", self->threadId);

    /*
     * Change our status and wake our parent, who will add us to the
     * thread list and advance our state to VMWAIT.
     */
    dvmLockThreadList(self);
    self->status = THREAD_STARTING;
    pthread_cond_broadcast(&gDvm.threadStartCond);

    /*
     * Wait until the parent says we can go.  Assuming there wasn't a
     * suspend pending, this will happen immediately.  When it completes,
     * we're full-fledged citizens of the VM.
     *
     * We have to use THREAD_VMWAIT here rather than THREAD_RUNNING
     * because the pthread_cond_wait below needs to reacquire a lock that
     * suspend-all is also interested in.  If we get unlucky, the parent could
     * change us to THREAD_RUNNING, then a GC could start before we get
     * signaled, and suspend-all will grab the thread list lock and then
     * wait for us to suspend.  We'll be in the tail end of pthread_cond_wait
     * trying to get the lock.
     */
    while (self->status != THREAD_VMWAIT)
        pthread_cond_wait(&gDvm.threadStartCond, &gDvm.threadListLock);

    dvmUnlockThreadList();

    /*
     * Add a JNI context.
     */
    self->jniEnv = dvmCreateJNIEnv(self);

    /*
     * Change our state so the GC will wait for us from now on.  If a GC is
     * in progress this call will suspend us.
     */
    dvmChangeStatus(self, THREAD_RUNNING);

    /*
     * Notify the debugger & DDM.  The debugger notification may cause
     * us to suspend ourselves (and others).
     */
    if (gDvm.debuggerConnected)
        dvmDbgPostThreadStart(self);

    /*
     * Set the system thread priority according to the Thread object's
     * priority level.  We don't usually need to do this, because both the
     * Thread object and system thread priorities inherit from parents.  The
     * tricky case is when somebody creates a Thread object, calls
     * setPriority(), and then starts the thread.  We could manage this with
     * a "needs priority update" flag to avoid the redundant call.
     */
    int priority = dvmGetFieldInt(self->threadObj,
                        gDvm.offJavaLangThread_priority);
    dvmChangeThreadPriority(self, priority);

    /*
     * Execute the "run" method.
     *
     * At this point our stack is empty, so somebody who comes looking for
     * stack traces right now won't have much to look at.  This is normal.
     */
    Method* run = self->threadObj->clazz->vtable[gDvm.voffJavaLangThread_run];
    JValue unused;

    LOGV("threadid=%d: calling run()\n", self->threadId);
    assert(strcmp(run->name, "run") == 0);
    dvmCallMethod(self, run, self->threadObj, &unused);
    LOGV("threadid=%d: exiting\n", self->threadId);

    /*
     * Remove the thread from various lists, report its death, and free
     * its resources.
     */
    dvmDetachCurrentThread();

    return NULL;
}

/*
 * The current thread is exiting with an uncaught exception.  The
 * Java programming language allows the application to provide a
 * thread-exit-uncaught-exception handler for the VM, for a specific
 * Thread, and for all threads in a ThreadGroup.
 *
 * Version 1.5 added the per-thread handler.  We need to call
 * "uncaughtException" in the handler object, which is either the
 * ThreadGroup object or the Thread-specific handler.
 */
static void threadExitUncaughtException(Thread* self, Object* group)
{
    Object* exception;
    Object* handlerObj;
    ClassObject* throwable;
    Method* uncaughtHandler = NULL;
    InstField* threadHandler;

    LOGW("threadid=%d: thread exiting with uncaught exception (group=%p)\n",
        self->threadId, group);
    assert(group != NULL);

    /*
     * Get a pointer to the exception, then clear out the one in the
     * thread.  We don't want to have it set when executing interpreted code.
     */
    exception = dvmGetException(self);
    dvmAddTrackedAlloc(exception, self);
    dvmClearException(self);

    /*
     * Get the Thread's "uncaughtHandler" object.  Use it if non-NULL;
     * else use "group" (which is an instance of UncaughtExceptionHandler).
     */
    threadHandler = dvmFindInstanceField(gDvm.classJavaLangThread,
            "uncaughtHandler", "Ljava/lang/Thread$UncaughtExceptionHandler;");
    if (threadHandler == NULL) {
        LOGW("WARNING: no 'uncaughtHandler' field in java/lang/Thread\n");
        goto bail;
    }
    handlerObj = dvmGetFieldObject(self->threadObj, threadHandler->byteOffset);
    if (handlerObj == NULL)
        handlerObj = group;

    /*
     * Find the "uncaughtHandler" field in this object.
     */
    uncaughtHandler = dvmFindVirtualMethodHierByDescriptor(handlerObj->clazz,
            "uncaughtException", "(Ljava/lang/Thread;Ljava/lang/Throwable;)V");

    if (uncaughtHandler != NULL) {
        //LOGI("+++ calling %s.uncaughtException\n",
        //     handlerObj->clazz->descriptor);
        JValue unused;
        dvmCallMethod(self, uncaughtHandler, handlerObj, &unused,
            self->threadObj, exception);
    } else {
        /* restore it and dump a stack trace */
        LOGW("WARNING: no 'uncaughtException' method in class %s\n",
            handlerObj->clazz->descriptor);
        dvmSetException(self, exception);
        dvmLogExceptionStackTrace();
    }

bail:
#if defined(WITH_JIT)
    /* Remove this thread's suspendCount from global suspendCount sum */
    lockThreadSuspendCount();
    dvmAddToThreadSuspendCount(&self->suspendCount, -self->suspendCount);
    unlockThreadSuspendCount();
#endif
    dvmReleaseTrackedAlloc(exception, self);
}


/*
 * Create an internal VM thread, for things like JDWP and finalizers.
 *
 * The easiest way to do this is create a new thread and then use the
 * JNI AttachCurrentThread implementation.
 *
 * This does not return until after the new thread has begun executing.
 */
bool dvmCreateInternalThread(pthread_t* pHandle, const char* name,
    InternalThreadStart func, void* funcArg)
{
    InternalStartArgs* pArgs;
    Object* systemGroup;
    pthread_attr_t threadAttr;
    volatile Thread* newThread = NULL;
    volatile int createStatus = 0;

    systemGroup = dvmGetSystemThreadGroup();
    if (systemGroup == NULL)
        return false;

    pArgs = (InternalStartArgs*) malloc(sizeof(*pArgs));
    pArgs->func = func;
    pArgs->funcArg = funcArg;
    pArgs->name = strdup(name);     // storage will be owned by new thread
    pArgs->group = systemGroup;
    pArgs->isDaemon = true;
    pArgs->pThread = &newThread;
    pArgs->pCreateStatus = &createStatus;

    pthread_attr_init(&threadAttr);
    //pthread_attr_setdetachstate(&threadAttr, PTHREAD_CREATE_DETACHED);

    if (pthread_create(pHandle, &threadAttr, internalThreadStart,
            pArgs) != 0)
    {
        LOGE("internal thread creation failed\n");
        free(pArgs->name);
        free(pArgs);
        return false;
    }

    /*
     * Wait for the child to start.  This gives us an opportunity to make
     * sure that the thread started correctly, and allows our caller to
     * assume that the thread has started running.
     *
     * Because we aren't holding a lock across the thread creation, it's
     * possible that the child will already have completed its
     * initialization.  Because the child only adjusts "createStatus" while
     * holding the thread list lock, the initial condition on the "while"
     * loop will correctly avoid the wait if this occurs.
     *
     * It's also possible that we'll have to wait for the thread to finish
     * being created, and as part of allocating a Thread object it might
     * need to initiate a GC.  We switch to VMWAIT while we pause.
     */
    Thread* self = dvmThreadSelf();
    int oldStatus = dvmChangeStatus(self, THREAD_VMWAIT);
    dvmLockThreadList(self);
    while (createStatus == 0)
        pthread_cond_wait(&gDvm.threadStartCond, &gDvm.threadListLock);

    if (newThread == NULL) {
        LOGW("internal thread create failed (createStatus=%d)\n", createStatus);
        assert(createStatus < 0);
        /* don't free pArgs -- if pthread_create succeeded, child owns it */
        dvmUnlockThreadList();
        dvmChangeStatus(self, oldStatus);
        return false;
    }

    /* thread could be in any state now (except early init states) */
    //assert(newThread->status == THREAD_RUNNING);

    dvmUnlockThreadList();
    dvmChangeStatus(self, oldStatus);

    return true;
}

/*
 * pthread entry function for internally-created threads.
 *
 * We are expected to free "arg" and its contents.  If we're a daemon
 * thread, and we get cancelled abruptly when the VM shuts down, the
 * storage won't be freed.  If this becomes a concern we can make a copy
 * on the stack.
 */
static void* internalThreadStart(void* arg)
{
    InternalStartArgs* pArgs = (InternalStartArgs*) arg;
    JavaVMAttachArgs jniArgs;

    jniArgs.version = JNI_VERSION_1_2;
    jniArgs.name = pArgs->name;
    jniArgs.group = pArgs->group;

    setThreadName(pArgs->name);

    /* use local jniArgs as stack top */
    if (dvmAttachCurrentThread(&jniArgs, pArgs->isDaemon)) {
        /*
         * Tell the parent of our success.
         *
         * threadListLock is the mutex for threadStartCond.
         */
        dvmLockThreadList(dvmThreadSelf());
        *pArgs->pCreateStatus = 1;
        *pArgs->pThread = dvmThreadSelf();
        pthread_cond_broadcast(&gDvm.threadStartCond);
        dvmUnlockThreadList();

        LOG_THREAD("threadid=%d: internal '%s'\n",
            dvmThreadSelf()->threadId, pArgs->name);

        /* execute */
        (*pArgs->func)(pArgs->funcArg);

        /* detach ourselves */
        dvmDetachCurrentThread();
    } else {
        /*
         * Tell the parent of our failure.  We don't have a Thread struct,
         * so we can't be suspended, so we don't need to enter a critical
         * section.
         */
        dvmLockThreadList(dvmThreadSelf());
        *pArgs->pCreateStatus = -1;
        assert(*pArgs->pThread == NULL);
        pthread_cond_broadcast(&gDvm.threadStartCond);
        dvmUnlockThreadList();

        assert(*pArgs->pThread == NULL);
    }

    free(pArgs->name);
    free(pArgs);
    return NULL;
}

/*
 * Attach the current thread to the VM.
 *
 * Used for internally-created threads and JNI's AttachCurrentThread.
 */
bool dvmAttachCurrentThread(const JavaVMAttachArgs* pArgs, bool isDaemon)
{
    Thread* self = NULL;
    Object* threadObj = NULL;
    Object* vmThreadObj = NULL;
    StringObject* threadNameStr = NULL;
    Method* init;
    bool ok, ret;

    /* establish a basic sense of self */
    self = allocThread(gDvm.stackSize);
    if (self == NULL)
        goto fail;
    setThreadSelf(self);

    /*
     * Create Thread and VMThread objects.  We have to use ALLOC_NO_GC
     * because this thread is not yet visible to the VM.  We could also
     * just grab the GC lock earlier, but that leaves us executing
     * interpreted code with the lock held, which is not prudent.
     *
     * The alloc calls will block if a GC is in progress, so we don't need
     * to check for global suspension here.
     *
     * It's also possible for the allocation calls to *cause* a GC.
     */
    //BUG: deadlock if a GC happens here during HeapWorker creation
    threadObj = dvmAllocObject(gDvm.classJavaLangThread, ALLOC_NO_GC);
    if (threadObj == NULL)
        goto fail;
    vmThreadObj = dvmAllocObject(gDvm.classJavaLangVMThread, ALLOC_NO_GC);
    if (vmThreadObj == NULL)
        goto fail;

    self->threadObj = threadObj;
    dvmSetFieldInt(vmThreadObj, gDvm.offJavaLangVMThread_vmData, (u4)self);

    /*
     * Do some java.lang.Thread constructor prep before we lock stuff down.
     */
    if (pArgs->name != NULL) {
        threadNameStr = dvmCreateStringFromCstr(pArgs->name, ALLOC_NO_GC);
        if (threadNameStr == NULL) {
            assert(dvmCheckException(dvmThreadSelf()));
            goto fail;
        }
    }

    init = dvmFindDirectMethodByDescriptor(gDvm.classJavaLangThread, "<init>",
            "(Ljava/lang/ThreadGroup;Ljava/lang/String;IZ)V");
    if (init == NULL) {
        assert(dvmCheckException(dvmThreadSelf()));
        goto fail;
    }

    /*
     * Finish our thread prep.  We need to do this before invoking any
     * interpreted code.  prepareThread() requires that we hold the thread
     * list lock.
     */
    dvmLockThreadList(self);
    ok = prepareThread(self);
    dvmUnlockThreadList();
    if (!ok)
        goto fail;

    self->jniEnv = dvmCreateJNIEnv(self);
    if (self->jniEnv == NULL)
        goto fail;

    /*
     * Create a "fake" JNI frame at the top of the main thread interp stack.
     * It isn't really necessary for the internal threads, but it gives
     * the debugger something to show.  It is essential for the JNI-attached
     * threads.
     */
    if (!createFakeRunFrame(self))
        goto fail;

    /*
     * The native side of the thread is ready;  add it to the list.
     */
    LOG_THREAD("threadid=%d: adding to list (attached)\n", self->threadId);

    /* Start off in VMWAIT, because we may be about to block
     * on the heap lock, and we don't want any suspensions
     * to wait for us.
     */
    self->status = THREAD_VMWAIT;

    /*
     * Add ourselves to the thread list.  Once we finish here we are
     * visible to the debugger and the GC.
     */
    dvmLockThreadList(self);

    self->next = gDvm.threadList->next;
    if (self->next != NULL)
        self->next->prev = self;
    self->prev = gDvm.threadList;
    gDvm.threadList->next = self;
    if (!isDaemon)
        gDvm.nonDaemonThreadCount++;

    dvmUnlockThreadList();

    /*
     * It's possible that a GC is currently running.  Our thread
     * wasn't in the list when the GC started, so it's not properly
     * suspended in that case.  Synchronize on the heap lock (held
     * when a GC is happening) to guarantee that any GCs from here
     * on will see this thread in the list.
     */
    dvmLockMutex(&gDvm.gcHeapLock);
    dvmUnlockMutex(&gDvm.gcHeapLock);

    /*
     * Switch to the running state now that we're ready for
     * suspensions.  This call may suspend.
     */
    dvmChangeStatus(self, THREAD_RUNNING);

    /*
     * Now we're ready to run some interpreted code.
     *
     * We need to construct the Thread object and set the VMThread field.
     * Setting VMThread tells interpreted code that we're alive.
     *
     * Call the (group, name, priority, daemon) constructor on the Thread.
     * This sets the thread's name and adds it to the specified group, and
     * provides values for priority and daemon (which are normally inherited
     * from the current thread).
     */
    JValue unused;
    dvmCallMethod(self, init, threadObj, &unused, (Object*)pArgs->group,
        threadNameStr, getThreadPriorityFromSystem(), isDaemon);
    if (dvmCheckException(self)) {
        LOGE("exception thrown while constructing attached thread object\n");
        goto fail_unlink;
    }
    //if (isDaemon)
    //    dvmSetFieldBoolean(threadObj, gDvm.offJavaLangThread_daemon, true);

    /*
     * Set the VMThread field, which tells interpreted code that we're alive.
     *
     * The risk of a thread start collision here is very low; somebody
     * would have to be deliberately polling the ThreadGroup list and
     * trying to start threads against anything it sees, which would
     * generally cause problems for all thread creation.  However, for
     * correctness we test "vmThread" before setting it.
     */
    if (dvmGetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread) != NULL) {
        dvmThrowException("Ljava/lang/IllegalThreadStateException;",
            "thread has already been started");
        /* We don't want to free anything associated with the thread
         * because someone is obviously interested in it.  Just let
         * it go and hope it will clean itself up when its finished.
         * This case should never happen anyway.
         *
         * Since we're letting it live, we need to finish setting it up.
         * We just have to let the caller know that the intended operation
         * has failed.
         *
         * [ This seems strange -- stepping on the vmThread object that's
         * already present seems like a bad idea.  TODO: figure this out. ]
         */
        ret = false;
    } else
        ret = true;
    dvmSetFieldObject(threadObj, gDvm.offJavaLangThread_vmThread, vmThreadObj);

    /* These are now reachable from the thread groups. */
    dvmClearAllocFlags(threadObj, ALLOC_NO_GC);
    dvmClearAllocFlags(vmThreadObj, ALLOC_NO_GC);

    /*
     * The thread is ready to go;  let the debugger see it.
     */
    self->threadObj = threadObj;

    LOG_THREAD("threadid=%d: attached from native, name=%s\n",
        self->threadId, pArgs->name);

    /* tell the debugger & DDM */
    if (gDvm.debuggerConnected)
        dvmDbgPostThreadStart(self);

    return ret;

fail_unlink:
    dvmLockThreadList(self);
    unlinkThread(self);
    if (!isDaemon)
        gDvm.nonDaemonThreadCount--;
    dvmUnlockThreadList();
    /* fall through to "fail" */
fail:
    dvmClearAllocFlags(threadObj, ALLOC_NO_GC);
    dvmClearAllocFlags(vmThreadObj, ALLOC_NO_GC);
    if (self != NULL) {
        if (self->jniEnv != NULL) {
            dvmDestroyJNIEnv(self->jniEnv);
            self->jniEnv = NULL;
        }
        freeThread(self);
    }
    setThreadSelf(NULL);
    return false;
}

/*
 * Detach the thread from the various data structures, notify other threads
 * that are waiting to "join" it, and free up all heap-allocated storage.
 *
 * Used for all threads.
 *
 * When we get here the interpreted stack should be empty.  The JNI 1.6 spec
 * requires us to enforce this for the DetachCurrentThread call, probably
 * because it also says that DetachCurrentThread causes all monitors
 * associated with the thread to be released.  (Because the stack is empty,
 * we only have to worry about explicit JNI calls to MonitorEnter.)
 *
 * THOUGHT:
 * We might want to avoid freeing our internal Thread structure until the
 * associated Thread/VMThread objects get GCed.  Our Thread is impossible to
 * get to once the thread shuts down, but there is a small possibility of
 * an operation starting in another thread before this thread halts, and
 * finishing much later (perhaps the thread got stalled by a weird OS bug).
 * We don't want something like Thread.isInterrupted() crawling through
 * freed storage.  Can do with a Thread finalizer, or by creating a
 * dedicated ThreadObject class for java/lang/Thread and moving all of our
 * state into that.
 */
void dvmDetachCurrentThread(void)
{
    Thread* self = dvmThreadSelf();
    Object* vmThread;
    Object* group;

    /*
     * Make sure we're not detaching a thread that's still running.  (This
     * could happen with an explicit JNI detach call.)
     *
     * A thread created by interpreted code will finish with a depth of
     * zero, while a JNI-attached thread will have the synthetic "stack
     * starter" native method at the top.
     */
    int curDepth = dvmComputeExactFrameDepth(self->curFrame);
    if (curDepth != 0) {
        bool topIsNative = false;

        if (curDepth == 1) {
            /* not expecting a lingering break frame; just look at curFrame */
            assert(!dvmIsBreakFrame(self->curFrame));
            StackSaveArea* ssa = SAVEAREA_FROM_FP(self->curFrame);
            if (dvmIsNativeMethod(ssa->method))
                topIsNative = true;
        }

        if (!topIsNative) {
            LOGE("ERROR: detaching thread with interp frames (count=%d)\n",
                curDepth);
            dvmDumpThread(self, false);
            dvmAbort();
        }
    }

    group = dvmGetFieldObject(self->threadObj, gDvm.offJavaLangThread_group);
    LOG_THREAD("threadid=%d: detach (group=%p)\n", self->threadId, group);

    /*
     * Release any held monitors.  Since there are no interpreted stack
     * frames, the only thing left are the monitors held by JNI MonitorEnter
     * calls.
     */
    dvmReleaseJniMonitors(self);

    /*
     * Do some thread-exit uncaught exception processing if necessary.
     */
    if (dvmCheckException(self))
        threadExitUncaughtException(self, group);

    /*
     * Remove the thread from the thread group.
     */
    if (group != NULL) {
        Method* removeThread =
            group->clazz->vtable[gDvm.voffJavaLangThreadGroup_removeThread];
        JValue unused;
        dvmCallMethod(self, removeThread, group, &unused, self->threadObj);
    }

    /*
     * Clear the vmThread reference in the Thread object.  Interpreted code
     * will now see that this Thread is not running.  As this may be the
     * only reference to the VMThread object that the VM knows about, we
     * have to create an internal reference to it first.
     */
    vmThread = dvmGetFieldObject(self->threadObj,
                    gDvm.offJavaLangThread_vmThread);
    dvmAddTrackedAlloc(vmThread, self);
    dvmSetFieldObject(self->threadObj, gDvm.offJavaLangThread_vmThread, NULL);

    /* clear out our struct Thread pointer, since it's going away */
    dvmSetFieldObject(vmThread, gDvm.offJavaLangVMThread_vmData, NULL);

    /*
     * Tell the debugger & DDM.  This may cause the current thread or all
     * threads to suspend.
     *
     * The JDWP spec is somewhat vague about when this happens, other than
     * that it's issued by the dying thread, which may still appear in
     * an "all threads" listing.
     */
    if (gDvm.debuggerConnected)
        dvmDbgPostThreadDeath(self);

    /*
     * Thread.join() is implemented as an Object.wait() on the VMThread
     * object.  Signal anyone who is waiting.
     */
    dvmLockObject(self, vmThread);
    dvmObjectNotifyAll(self, vmThread);
    dvmUnlockObject(self, vmThread);

    dvmReleaseTrackedAlloc(vmThread, self);
    vmThread = NULL;

    /*
     * We're done manipulating objects, so it's okay if the GC runs in
     * parallel with us from here out.  It's important to do this if
     * profiling is enabled, since we can wait indefinitely.
     */
    self->status = THREAD_VMWAIT;

#ifdef WITH_PROFILER
    /*
     * If we're doing method trace profiling, we don't want threads to exit,
     * because if they do we'll end up reusing thread IDs.  This complicates
     * analysis and makes it impossible to have reasonable output in the
     * "threads" section of the "key" file.
     *
     * We need to do this after Thread.join() completes, or other threads
     * could get wedged.  Since self->threadObj is still valid, the Thread
     * object will not get GCed even though we're no longer in the ThreadGroup
     * list (which is important since the profiling thread needs to get
     * the thread's name).
     */
    MethodTraceState* traceState = &gDvm.methodTrace;

    dvmLockMutex(&traceState->startStopLock);
    if (traceState->traceEnabled) {
        LOGI("threadid=%d: waiting for method trace to finish\n",
            self->threadId);
        while (traceState->traceEnabled) {
            int cc;
            cc = pthread_cond_wait(&traceState->threadExitCond,
                    &traceState->startStopLock);
            assert(cc == 0);
        }
    }
    dvmUnlockMutex(&traceState->startStopLock);
#endif

    dvmLockThreadList(self);

    /*
     * Lose the JNI context.
     */
    dvmDestroyJNIEnv(self->jniEnv);
    self->jniEnv = NULL;

    self->status = THREAD_ZOMBIE;

    /*
     * Remove ourselves from the internal thread list.
     */
    unlinkThread(self);

    /*
     * If we're the last one standing, signal anybody waiting in
     * DestroyJavaVM that it's okay to exit.
     */
    if (!dvmGetFieldBoolean(self->threadObj, gDvm.offJavaLangThread_daemon)) {
        gDvm.nonDaemonThreadCount--;        // guarded by thread list lock

        if (gDvm.nonDaemonThreadCount == 0) {
            int cc;

            LOGV("threadid=%d: last non-daemon thread\n", self->threadId);
            //dvmDumpAllThreads(false);
            // cond var guarded by threadListLock, which we already hold
            cc = pthread_cond_signal(&gDvm.vmExitCond);
            assert(cc == 0);
        }
    }

    LOGV("threadid=%d: bye!\n", self->threadId);
    releaseThreadId(self);
    dvmUnlockThreadList();

    setThreadSelf(NULL);

    dvmDetachSystemThread(self);

    freeThread(self);
}


/*
 * Suspend a single thread.  Do not use to suspend yourself.
 *
 * This is used primarily for debugger/DDMS activity.  Does not return
 * until the thread has suspended or is in a "safe" state (e.g. executing
 * native code outside the VM).
 *
 * The thread list lock should be held before calling here -- it's not
 * entirely safe to hang on to a Thread* from another thread otherwise.
 * (We'd need to grab it here anyway to avoid clashing with a suspend-all.)
 */
void dvmSuspendThread(Thread* thread)
{
    assert(thread != NULL);
    assert(thread != dvmThreadSelf());
    //assert(thread->handle != dvmJdwpGetDebugThread(gDvm.jdwpState));

    lockThreadSuspendCount();
    dvmAddToThreadSuspendCount(&thread->suspendCount, 1);
    thread->dbgSuspendCount++;

    LOG_THREAD("threadid=%d: suspend++, now=%d\n",
        thread->threadId, thread->suspendCount);
    unlockThreadSuspendCount();

    waitForThreadSuspend(dvmThreadSelf(), thread);
}

/*
 * Reduce the suspend count of a thread.  If it hits zero, tell it to
 * resume.
 *
 * Used primarily for debugger/DDMS activity.  The thread in question
 * might have been suspended singly or as part of a suspend-all operation.
 *
 * The thread list lock should be held before calling here -- it's not
 * entirely safe to hang on to a Thread* from another thread otherwise.
 * (We'd need to grab it here anyway to avoid clashing with a suspend-all.)
 */
void dvmResumeThread(Thread* thread)
{
    assert(thread != NULL);
    assert(thread != dvmThreadSelf());
    //assert(thread->handle != dvmJdwpGetDebugThread(gDvm.jdwpState));

    lockThreadSuspendCount();
    if (thread->suspendCount > 0) {
        dvmAddToThreadSuspendCount(&thread->suspendCount, -1);
        thread->dbgSuspendCount--;
    } else {
        LOG_THREAD("threadid=%d:  suspendCount already zero\n",
            thread->threadId);
    }

    LOG_THREAD("threadid=%d: suspend--, now=%d\n",
        thread->threadId, thread->suspendCount);

    if (thread->suspendCount == 0) {
        int cc = pthread_cond_broadcast(&gDvm.threadSuspendCountCond);
        assert(cc == 0);
    }

    unlockThreadSuspendCount();
}

/*
 * Suspend yourself, as a result of debugger activity.
 */
void dvmSuspendSelf(bool jdwpActivity)
{
    Thread* self = dvmThreadSelf();

    /* debugger thread may not suspend itself due to debugger activity! */
    assert(gDvm.jdwpState != NULL);
    if (self->handle == dvmJdwpGetDebugThread(gDvm.jdwpState)) {
        assert(false);
        return;
    }

    /*
     * Collisions with other suspends aren't really interesting.  We want
     * to ensure that we're the only one fiddling with the suspend count
     * though.
     */
    lockThreadSuspendCount();
    dvmAddToThreadSuspendCount(&self->suspendCount, 1);
    self->dbgSuspendCount++;

    /*
     * Suspend ourselves.
     */
    assert(self->suspendCount > 0);
    self->isSuspended = true;
    LOG_THREAD("threadid=%d: self-suspending (dbg)\n", self->threadId);

    /*
     * Tell JDWP that we've completed suspension.  The JDWP thread can't
     * tell us to resume before we're fully asleep because we hold the
     * suspend count lock.
     *
     * If we got here via waitForDebugger(), don't do this part.
     */
    if (jdwpActivity) {
        //LOGI("threadid=%d: clearing wait-for-event (my handle=%08x)\n",
        //    self->threadId, (int) self->handle);
        dvmJdwpClearWaitForEventThread(gDvm.jdwpState);
    }

    while (self->suspendCount != 0) {
        int cc;
        cc = pthread_cond_wait(&gDvm.threadSuspendCountCond,
                &gDvm.threadSuspendCountLock);
        assert(cc == 0);
        if (self->suspendCount != 0) {
            /*
             * The condition was signaled but we're still suspended.  This
             * can happen if the debugger lets go while a SIGQUIT thread
             * dump event is pending (assuming SignalCatcher was resumed for
             * just long enough to try to grab the thread-suspend lock).
             */
            LOGD("threadid=%d: still suspended after undo (sc=%d dc=%d s=%c)\n",
                self->threadId, self->suspendCount, self->dbgSuspendCount,
                self->isSuspended ? 'Y' : 'N');
        }
    }
    assert(self->suspendCount == 0 && self->dbgSuspendCount == 0);
    self->isSuspended = false;
    LOG_THREAD("threadid=%d: self-reviving (dbg), status=%d\n",
        self->threadId, self->status);

    unlockThreadSuspendCount();
}


#ifdef HAVE_GLIBC
# define NUM_FRAMES  20
# include <execinfo.h>
/*
 * glibc-only stack dump function.  Requires link with "--export-dynamic".
 *
 * TODO: move this into libs/cutils and make it work for all platforms.
 */
static void printBackTrace(void)
{
    void* array[NUM_FRAMES];
    size_t size;
    char** strings;
    size_t i;

    size = backtrace(array, NUM_FRAMES);
    strings = backtrace_symbols(array, size);

    LOGW("Obtained %zd stack frames.\n", size);

    for (i = 0; i < size; i++)
        LOGW("%s\n", strings[i]);

    free(strings);
}
#else
static void printBackTrace(void) {}
#endif

/*
 * Dump the state of the current thread and that of another thread that
 * we think is wedged.
 */
static void dumpWedgedThread(Thread* thread)
{
    char exePath[1024];

    /*
     * The "executablepath" function in libutils is host-side only.
     */
    strcpy(exePath, "-");
#ifdef HAVE_GLIBC
    {
        char proc[100];
        sprintf(proc, "/proc/%d/exe", getpid());
        int len;

        len = readlink(proc, exePath, sizeof(exePath)-1);
        exePath[len] = '\0';
    }
#endif

    LOGW("dumping state: process %s %d\n", exePath, getpid());
    dvmDumpThread(dvmThreadSelf(), false);
    printBackTrace();

    // dumping a running thread is risky, but could be useful
    dvmDumpThread(thread, true);


    // stop now and get a core dump
    //abort();
}


/*
 * Wait for another thread to see the pending suspension and stop running.
 * It can either suspend itself or go into a non-running state such as
 * VMWAIT or NATIVE in which it cannot interact with the GC.
 *
 * If we're running at a higher priority, sched_yield() may not do anything,
 * so we need to sleep for "long enough" to guarantee that the other
 * thread has a chance to finish what it's doing.  Sleeping for too short
 * a period (e.g. less than the resolution of the sleep clock) might cause
 * the scheduler to return immediately, so we want to start with a
 * "reasonable" value and expand.
 *
 * This does not return until the other thread has stopped running.
 * Eventually we time out and the VM aborts.
 *
 * This does not try to detect the situation where two threads are
 * waiting for each other to suspend.  In normal use this is part of a
 * suspend-all, which implies that the suspend-all lock is held, or as
 * part of a debugger action in which the JDWP thread is always the one
 * doing the suspending.  (We may need to re-evaluate this now that
 * getThreadStackTrace is implemented as suspend-snapshot-resume.)
 *
 * TODO: track basic stats about time required to suspend VM.
 */
#define FIRST_SLEEP (250*1000)    /* 0.25s */
#define MORE_SLEEP  (750*1000)    /* 0.75s */
static void waitForThreadSuspend(Thread* self, Thread* thread)
{
    const int kMaxRetries = 10;
    int spinSleepTime = FIRST_SLEEP;
    bool complained = false;
    bool needPriorityReset = false;
    int savedThreadPrio = -500;

    int sleepIter = 0;
    int retryCount = 0;
    u8 startWhen = 0;       // init req'd to placate gcc
    u8 firstStartWhen = 0;

    while (thread->status == THREAD_RUNNING && !thread->isSuspended) {
        if (sleepIter == 0) {           // get current time on first iteration
            startWhen = dvmGetRelativeTimeUsec();
            if (firstStartWhen == 0)    // first iteration of first attempt
                firstStartWhen = startWhen;

            /*
             * After waiting for a bit, check to see if the target thread is
             * running at a reduced priority.  If so, bump it up temporarily
             * to give it more CPU time.
             *
             * getpriority() returns the "nice" value, so larger numbers
             * indicate lower priority.
             *
             * (Not currently changing the cgroup.  Wasn't necessary in some
             * simple experiments.)
             */
            if (retryCount == 2) {
                assert(thread->systemTid != 0);
                errno = 0;
                int threadPrio = getpriority(PRIO_PROCESS, thread->systemTid);
                if (errno == 0 && threadPrio > 0) {
                    const int kHigher = 0;
                    if (setpriority(PRIO_PROCESS, thread->systemTid, kHigher) < 0)
                    {
                        LOGW("Couldn't raise priority on tid %d to %d\n",
                            thread->systemTid, kHigher);
                    } else {
                        savedThreadPrio = threadPrio;
                        needPriorityReset = true;
                        LOGD("Temporarily raising priority on tid %d (%d -> %d)\n",
                            thread->systemTid, threadPrio, kHigher);
                    }
                }
            }
        }

#if defined (WITH_JIT)
        /*
         * If we're still waiting after the first timeout,
         * unchain all translations.
         */
        if (gDvmJit.pJitEntryTable && retryCount > 0) {
            LOGD("JIT unchain all attempt #%d",retryCount);
            dvmJitUnchainAll();
        }
#endif

        /*
         * Sleep briefly.  This returns false if we've exceeded the total
         * time limit for this round of sleeping.
         */
        if (!dvmIterativeSleep(sleepIter++, spinSleepTime, startWhen)) {
            LOGW("threadid=%d: spin on suspend #%d threadid=%d (h=%d)\n",
                self->threadId, retryCount,
                thread->threadId, (int)thread->handle);
            dumpWedgedThread(thread);
            complained = true;

            // keep going; could be slow due to valgrind
            sleepIter = 0;
            spinSleepTime = MORE_SLEEP;

            if (retryCount++ == kMaxRetries) {
                LOGE("threadid=%d: stuck on threadid=%d, giving up\n",
                    self->threadId, thread->threadId);
                dvmDumpAllThreads(false);
                dvmAbort();
            }
        }
    }

    if (complained) {
        LOGW("threadid=%d: spin on suspend resolved in %lld msec\n",
            self->threadId,
            (dvmGetRelativeTimeUsec() - firstStartWhen) / 1000);
        //dvmDumpThread(thread, false);   /* suspended, so dump is safe */
    }
    if (needPriorityReset) {
        if (setpriority(PRIO_PROCESS, thread->systemTid, savedThreadPrio) < 0) {
            LOGW("NOTE: couldn't reset priority on thread %d to %d\n",
                thread->systemTid, savedThreadPrio);
        } else {
            LOGV("Restored priority on %d to %d\n",
                thread->systemTid, savedThreadPrio);
        }
    }
}

/*
 * Suspend all threads except the current one.  This is used by the GC,
 * the debugger, and by any thread that hits a "suspend all threads"
 * debugger event (e.g. breakpoint or exception).
 *
 * If thread N hits a "suspend all threads" breakpoint, we don't want it
 * to suspend the JDWP thread.  For the GC, we do, because the debugger can
 * create objects and even execute arbitrary code.  The "why" argument
 * allows the caller to say why the suspension is taking place.
 *
 * This can be called when a global suspend has already happened, due to
 * various debugger gymnastics, so keeping an "everybody is suspended" flag
 * doesn't work.
 *
 * DO NOT grab any locks before calling here.  We grab & release the thread
 * lock and suspend lock here (and we're not using recursive threads), and
 * we might have to self-suspend if somebody else beats us here.
 *
 * The current thread may not be attached to the VM.  This can happen if
 * we happen to GC as the result of an allocation of a Thread object.
 */
void dvmSuspendAllThreads(SuspendCause why)
{
    Thread* self = dvmThreadSelf();
    Thread* thread;

    assert(why != 0);

    /*
     * Start by grabbing the thread suspend lock.  If we can't get it, most
     * likely somebody else is in the process of performing a suspend or
     * resume, so lockThreadSuspend() will cause us to self-suspend.
     *
     * We keep the lock until all other threads are suspended.
     */
    lockThreadSuspend("susp-all", why);

    LOG_THREAD("threadid=%d: SuspendAll starting\n", self->threadId);

    /*
     * This is possible if the current thread was in VMWAIT mode when a
     * suspend-all happened, and then decided to do its own suspend-all.
     * This can happen when a couple of threads have simultaneous events
     * of interest to the debugger.
     */
    //assert(self->suspendCount == 0);

    /*
     * Increment everybody's suspend count (except our own).
     */
    dvmLockThreadList(self);

    lockThreadSuspendCount();
    for (thread = gDvm.threadList; thread != NULL; thread = thread->next) {
        if (thread == self)
            continue;

        /* debugger events don't suspend JDWP thread */
        if ((why == SUSPEND_FOR_DEBUG || why == SUSPEND_FOR_DEBUG_EVENT) &&
            thread->handle == dvmJdwpGetDebugThread(gDvm.jdwpState))
            continue;

        dvmAddToThreadSuspendCount(&thread->suspendCount, 1);
        if (why == SUSPEND_FOR_DEBUG || why == SUSPEND_FOR_DEBUG_EVENT)
            thread->dbgSuspendCount++;
    }
    unlockThreadSuspendCount();

    /*
     * Wait for everybody in THREAD_RUNNING state to stop.  Other states
     * indicate the code is either running natively or sleeping quietly.
     * Any attempt to transition back to THREAD_RUNNING will cause a check
     * for suspension, so it should be impossible for anything to execute
     * interpreted code or modify objects (assuming native code plays nicely).
     *
     * It's also okay if the thread transitions to a non-RUNNING state.
     *
     * Note we released the threadSuspendCountLock before getting here,
     * so if another thread is fiddling with its suspend count (perhaps
     * self-suspending for the debugger) it won't block while we're waiting
     * in here.
     */
    for (thread = gDvm.threadList; thread != NULL; thread = thread->next) {
        if (thread == self)
            continue;

        /* debugger events don't suspend JDWP thread */
        if ((why == SUSPEND_FOR_DEBUG || why == SUSPEND_FOR_DEBUG_EVENT) &&
            thread->handle == dvmJdwpGetDebugThread(gDvm.jdwpState))
            continue;

        /* wait for the other thread to see the pending suspend */
        waitForThreadSuspend(self, thread);

        LOG_THREAD("threadid=%d:   threadid=%d status=%d c=%d dc=%d isSusp=%d\n",
            self->threadId,
            thread->threadId, thread->status, thread->suspendCount,
            thread->dbgSuspendCount, thread->isSuspended);
    }

    dvmUnlockThreadList();
    unlockThreadSuspend();

    LOG_THREAD("threadid=%d: SuspendAll complete\n", self->threadId);
}

/*
 * Resume all threads that are currently suspended.
 *
 * The "why" must match with the previous suspend.
 */
void dvmResumeAllThreads(SuspendCause why)
{
    Thread* self = dvmThreadSelf();
    Thread* thread;
    int cc;

    lockThreadSuspend("res-all", why);  /* one suspend/resume at a time */
    LOG_THREAD("threadid=%d: ResumeAll starting\n", self->threadId);

    /*
     * Decrement the suspend counts for all threads.  No need for atomic
     * writes, since nobody should be moving until we decrement the count.
     * We do need to hold the thread list because of JNI attaches.
     */
    dvmLockThreadList(self);
    lockThreadSuspendCount();
    for (thread = gDvm.threadList; thread != NULL; thread = thread->next) {
        if (thread == self)
            continue;

        /* debugger events don't suspend JDWP thread */
        if ((why == SUSPEND_FOR_DEBUG || why == SUSPEND_FOR_DEBUG_EVENT) &&
            thread->handle == dvmJdwpGetDebugThread(gDvm.jdwpState))
        {
            continue;
        }

        if (thread->suspendCount > 0) {
            dvmAddToThreadSuspendCount(&thread->suspendCount, -1);
            if (why == SUSPEND_FOR_DEBUG || why == SUSPEND_FOR_DEBUG_EVENT)
                thread->dbgSuspendCount--;
        } else {
            LOG_THREAD("threadid=%d:  suspendCount already zero\n",
                thread->threadId);
        }
    }
    unlockThreadSuspendCount();
    dvmUnlockThreadList();

    /*
     * In some ways it makes sense to continue to hold the thread-suspend
     * lock while we issue the wakeup broadcast.  It allows us to complete
     * one operation before moving on to the next, which simplifies the
     * thread activity debug traces.
     *
     * This approach caused us some difficulty under Linux, because the
     * condition variable broadcast not only made the threads runnable,
     * but actually caused them to execute, and it was a while before
     * the thread performing the wakeup had an opportunity to release the
     * thread-suspend lock.
     *
     * This is a problem because, when a thread tries to acquire that
     * lock, it times out after 3 seconds.  If at some point the thread
     * is told to suspend, the clock resets; but since the VM is still
     * theoretically mid-resume, there's no suspend pending.  If, for
     * example, the GC was waking threads up while the SIGQUIT handler
     * was trying to acquire the lock, we would occasionally time out on
     * a busy system and SignalCatcher would abort.
     *
     * We now perform the unlock before the wakeup broadcast.  The next
     * suspend can't actually start until the broadcast completes and
     * returns, because we're holding the thread-suspend-count lock, but the
     * suspending thread is now able to make progress and we avoid the abort.
     *
     * (Technically there is a narrow window between when we release
     * the thread-suspend lock and grab the thread-suspend-count lock.
     * This could cause us to send a broadcast to threads with nonzero
     * suspend counts, but this is expected and they'll all just fall
     * right back to sleep.  It's probably safe to grab the suspend-count
     * lock before releasing thread-suspend, since we're still following
     * the correct order of acquisition, but it feels weird.)
     */

    LOG_THREAD("threadid=%d: ResumeAll waking others\n", self->threadId);
    unlockThreadSuspend();

    /*
     * Broadcast a notification to all suspended threads, some or all of
     * which may choose to wake up.  No need to wait for them.
     */
    lockThreadSuspendCount();
    cc = pthread_cond_broadcast(&gDvm.threadSuspendCountCond);
    assert(cc == 0);
    unlockThreadSuspendCount();

    LOG_THREAD("threadid=%d: ResumeAll complete\n", self->threadId);
}

/*
 * Undo any debugger suspensions.  This is called when the debugger
 * disconnects.
 */
void dvmUndoDebuggerSuspensions(void)
{
    Thread* self = dvmThreadSelf();
    Thread* thread;
    int cc;

    lockThreadSuspend("undo", SUSPEND_FOR_DEBUG);
    LOG_THREAD("threadid=%d: UndoDebuggerSusp starting\n", self->threadId);

    /*
     * Decrement the suspend counts for all threads.  No need for atomic
     * writes, since nobody should be moving until we decrement the count.
     * We do need to hold the thread list because of JNI attaches.
     */
    dvmLockThreadList(self);
    lockThreadSuspendCount();
    for (thread = gDvm.threadList; thread != NULL; thread = thread->next) {
        if (thread == self)
            continue;

        /* debugger events don't suspend JDWP thread */
        if (thread->handle == dvmJdwpGetDebugThread(gDvm.jdwpState)) {
            assert(thread->dbgSuspendCount == 0);
            continue;
        }

        assert(thread->suspendCount >= thread->dbgSuspendCount);
        dvmAddToThreadSuspendCount(&thread->suspendCount,
                                   -thread->dbgSuspendCount);
        thread->dbgSuspendCount = 0;
    }
    unlockThreadSuspendCount();
    dvmUnlockThreadList();

    /*
     * Broadcast a notification to all suspended threads, some or all of
     * which may choose to wake up.  No need to wait for them.
     */
    lockThreadSuspendCount();
    cc = pthread_cond_broadcast(&gDvm.threadSuspendCountCond);
    assert(cc == 0);
    unlockThreadSuspendCount();

    unlockThreadSuspend();

    LOG_THREAD("threadid=%d: UndoDebuggerSusp complete\n", self->threadId);
}

/*
 * Determine if a thread is suspended.
 *
 * As with all operations on foreign threads, the caller should hold
 * the thread list lock before calling.
 */
bool dvmIsSuspended(Thread* thread)
{
    /*
     * The thread could be:
     *  (1) Running happily.  status is RUNNING, isSuspended is false,
     *      suspendCount is zero.  Return "false".
     *  (2) Pending suspend.  status is RUNNING, isSuspended is false,
     *      suspendCount is nonzero.  Return "false".
     *  (3) Suspended.  suspendCount is nonzero, and either (status is
     *      RUNNING and isSuspended is true) OR (status is !RUNNING).
     *      Return "true".
     *  (4) Waking up.  suspendCount is zero, status is RUNNING and
     *      isSuspended is true.  Return "false" (since it could change
     *      out from under us, unless we hold suspendCountLock).
     */

    return (thread->suspendCount != 0 &&
            ((thread->status == THREAD_RUNNING && thread->isSuspended) ||
             (thread->status != THREAD_RUNNING)));
}

/*
 * Wait until another thread self-suspends.  This is specifically for
 * synchronization between the JDWP thread and a thread that has decided
 * to suspend itself after sending an event to the debugger.
 *
 * Threads that encounter "suspend all" events work as well -- the thread
 * in question suspends everybody else and then itself.
 *
 * We can't hold a thread lock here or in the caller, because we could
 * get here just before the to-be-waited-for-thread issues a "suspend all".
 * There's an opportunity for badness if the thread we're waiting for exits
 * and gets cleaned up, but since the thread in question is processing a
 * debugger event, that's not really a possibility.  (To avoid deadlock,
 * it's important that we not be in THREAD_RUNNING while we wait.)
 */
void dvmWaitForSuspend(Thread* thread)
{
    Thread* self = dvmThreadSelf();

    LOG_THREAD("threadid=%d: waiting for threadid=%d to sleep\n",
        self->threadId, thread->threadId);

    assert(thread->handle != dvmJdwpGetDebugThread(gDvm.jdwpState));
    assert(thread != self);
    assert(self->status != THREAD_RUNNING);

    waitForThreadSuspend(self, thread);

    LOG_THREAD("threadid=%d: threadid=%d is now asleep\n",
        self->threadId, thread->threadId);
}

/*
 * Check to see if we need to suspend ourselves.  If so, go to sleep on
 * a condition variable.
 *
 * Takes "self" as an argument as an optimization.  Pass in NULL to have
 * it do the lookup.
 *
 * Returns "true" if we suspended ourselves.
 */
bool dvmCheckSuspendPending(Thread* self)
{
    bool didSuspend;

    if (self == NULL)
        self = dvmThreadSelf();

    /* fast path: if count is zero, bail immediately */
    if (self->suspendCount == 0)
        return false;

    lockThreadSuspendCount();   /* grab gDvm.threadSuspendCountLock */

    assert(self->suspendCount >= 0);        /* XXX: valid? useful? */

    didSuspend = (self->suspendCount != 0);
    self->isSuspended = true;
    LOG_THREAD("threadid=%d: self-suspending\n", self->threadId);
    while (self->suspendCount != 0) {
        /* wait for wakeup signal; releases lock */
        int cc;
        cc = pthread_cond_wait(&gDvm.threadSuspendCountCond,
                &gDvm.threadSuspendCountLock);
        assert(cc == 0);
    }
    assert(self->suspendCount == 0 && self->dbgSuspendCount == 0);
    self->isSuspended = false;
    LOG_THREAD("threadid=%d: self-reviving, status=%d\n",
        self->threadId, self->status);

    unlockThreadSuspendCount();

    return didSuspend;
}

/*
 * Update our status.
 *
 * The "self" argument, which may be NULL, is accepted as an optimization.
 *
 * Returns the old status.
 */
ThreadStatus dvmChangeStatus(Thread* self, ThreadStatus newStatus)
{
    ThreadStatus oldStatus;

    if (self == NULL)
        self = dvmThreadSelf();

    LOGVV("threadid=%d: (status %d -> %d)\n",
        self->threadId, self->status, newStatus);

    oldStatus = self->status;

    if (newStatus == THREAD_RUNNING) {
        /*
         * Change our status to THREAD_RUNNING.  The transition requires
         * that we check for pending suspension, because the VM considers
         * us to be "asleep" in all other states.
         *
         * We need to do the "suspend pending" check FIRST, because it grabs
         * a lock that could be held by something that wants us to suspend.
         * If we're in RUNNING it will wait for us, and we'll be waiting
         * for the lock it holds.
         */
        assert(self->status != THREAD_RUNNING);

        dvmCheckSuspendPending(self);
        self->status = THREAD_RUNNING;
    } else {
        /*
         * Change from one state to another, neither of which is
         * THREAD_RUNNING.  This is most common during system or thread
         * initialization.
         */
        self->status = newStatus;
    }

    return oldStatus;
}

/*
 * Get a statically defined thread group from a field in the ThreadGroup
 * Class object.  Expected arguments are "mMain" and "mSystem".
 */
static Object* getStaticThreadGroup(const char* fieldName)
{
    StaticField* groupField;
    Object* groupObj;

    groupField = dvmFindStaticField(gDvm.classJavaLangThreadGroup,
        fieldName, "Ljava/lang/ThreadGroup;");
    if (groupField == NULL) {
        LOGE("java.lang.ThreadGroup does not have an '%s' field\n", fieldName);
        dvmThrowException("Ljava/lang/IncompatibleClassChangeError;", NULL);
        return NULL;
    }
    groupObj = dvmGetStaticFieldObject(groupField);
    if (groupObj == NULL) {
        LOGE("java.lang.ThreadGroup.%s not initialized\n", fieldName);
        dvmThrowException("Ljava/lang/InternalError;", NULL);
        return NULL;
    }

    return groupObj;
}
Object* dvmGetSystemThreadGroup(void)
{
    return getStaticThreadGroup("mSystem");
}
Object* dvmGetMainThreadGroup(void)
{
    return getStaticThreadGroup("mMain");
}

/*
 * Given a VMThread object, return the associated Thread*.
 *
 * NOTE: if the thread detaches, the struct Thread will disappear, and
 * we will be touching invalid data.  For safety, lock the thread list
 * before calling this.
 */
Thread* dvmGetThreadFromThreadObject(Object* vmThreadObj)
{
    int vmData;

    vmData = dvmGetFieldInt(vmThreadObj, gDvm.offJavaLangVMThread_vmData);

    if (false) {
        Thread* thread = gDvm.threadList;
        while (thread != NULL) {
            if ((Thread*)vmData == thread)
                break;

            thread = thread->next;
        }

        if (thread == NULL) {
            LOGW("WARNING: vmThreadObj=%p has thread=%p, not in thread list\n",
                vmThreadObj, (Thread*)vmData);
            vmData = 0;
        }
    }

    return (Thread*) vmData;
}


/*
 * Conversion map for "nice" values.
 *
 * We use Android thread priority constants to be consistent with the rest
 * of the system.  In some cases adjacent entries may overlap.
 */
static const int kNiceValues[10] = {
    ANDROID_PRIORITY_LOWEST,                /* 1 (MIN_PRIORITY) */
    ANDROID_PRIORITY_BACKGROUND + 6,
    ANDROID_PRIORITY_BACKGROUND + 3,
    ANDROID_PRIORITY_BACKGROUND,
    ANDROID_PRIORITY_NORMAL,                /* 5 (NORM_PRIORITY) */
    ANDROID_PRIORITY_NORMAL - 2,
    ANDROID_PRIORITY_NORMAL - 4,
    ANDROID_PRIORITY_URGENT_DISPLAY + 3,
    ANDROID_PRIORITY_URGENT_DISPLAY + 2,
    ANDROID_PRIORITY_URGENT_DISPLAY         /* 10 (MAX_PRIORITY) */
};

/*
 * Change the priority of a system thread to match that of the Thread object.
 *
 * We map a priority value from 1-10 to Linux "nice" values, where lower
 * numbers indicate higher priority.
 */
void dvmChangeThreadPriority(Thread* thread, int newPriority)
{
    pid_t pid = thread->systemTid;
    int newNice;

    if (newPriority < 1 || newPriority > 10) {
        LOGW("bad priority %d\n", newPriority);
        newPriority = 5;
    }
    newNice = kNiceValues[newPriority-1];

    if (newNice >= ANDROID_PRIORITY_BACKGROUND) {
        set_sched_policy(dvmGetSysThreadId(), SP_BACKGROUND);
    } else if (getpriority(PRIO_PROCESS, pid) >= ANDROID_PRIORITY_BACKGROUND) {
        set_sched_policy(dvmGetSysThreadId(), SP_FOREGROUND);
    }

    if (setpriority(PRIO_PROCESS, pid, newNice) != 0) {
        char* str = dvmGetThreadName(thread);
        LOGI("setPriority(%d) '%s' to prio=%d(n=%d) failed: %s\n",
            pid, str, newPriority, newNice, strerror(errno));
        free(str);
    } else {
        LOGV("setPriority(%d) to prio=%d(n=%d)\n",
            pid, newPriority, newNice);
    }
}

/*
 * Get the thread priority for the current thread by querying the system.
 * This is useful when attaching a thread through JNI.
 *
 * Returns a value from 1 to 10 (compatible with java.lang.Thread values).
 */
static int getThreadPriorityFromSystem(void)
{
    int i, sysprio, jprio;

    errno = 0;
    sysprio = getpriority(PRIO_PROCESS, 0);
    if (sysprio == -1 && errno != 0) {
        LOGW("getpriority() failed: %s\n", strerror(errno));
        return THREAD_NORM_PRIORITY;
    }

    jprio = THREAD_MIN_PRIORITY;
    for (i = 0; i < NELEM(kNiceValues); i++) {
        if (sysprio >= kNiceValues[i])
            break;
        jprio++;
    }
    if (jprio > THREAD_MAX_PRIORITY)
        jprio = THREAD_MAX_PRIORITY;

    return jprio;
}


/*
 * Return true if the thread is on gDvm.threadList.
 * Caller should not hold gDvm.threadListLock.
 */
bool dvmIsOnThreadList(const Thread* thread)
{
    bool ret = false;

    dvmLockThreadList(NULL);
    if (thread == gDvm.threadList) {
        ret = true;
    } else {
        ret = thread->prev != NULL || thread->next != NULL;
    }
    dvmUnlockThreadList();

    return ret;
}

/*
 * Dump a thread to the log file -- just calls dvmDumpThreadEx() with an
 * output target.
 */
void dvmDumpThread(Thread* thread, bool isRunning)
{
    DebugOutputTarget target;

    dvmCreateLogOutputTarget(&target, ANDROID_LOG_INFO, LOG_TAG);
    dvmDumpThreadEx(&target, thread, isRunning);
}

/*
 * Try to get the scheduler group.
 *
 * The data from /proc/<pid>/cgroup looks like:
 *  2:cpu:/bg_non_interactive
 *
 * We return the part after the "/", which will be an empty string for
 * the default cgroup.  If the string is longer than "bufLen", the string
 * will be truncated.
 */
static bool getSchedulerGroup(Thread* thread, char* buf, size_t bufLen)
{
#ifdef HAVE_ANDROID_OS
    char pathBuf[32];
    char readBuf[256];
    ssize_t count;
    int fd;

    snprintf(pathBuf, sizeof(pathBuf), "/proc/%d/cgroup", thread->systemTid);
    if ((fd = open(pathBuf, O_RDONLY)) < 0) {
        LOGV("open(%s) failed: %s\n", pathBuf, strerror(errno));
        return false;
    }

    count = read(fd, readBuf, sizeof(readBuf));
    if (count <= 0) {
        LOGV("read(%s) failed (%d): %s\n",
            pathBuf, (int) count, strerror(errno));
        close(fd);
        return false;
    }
    close(fd);

    readBuf[--count] = '\0';    /* remove the '\n', now count==strlen */

    char* cp = strchr(readBuf, '/');
    if (cp == NULL) {
        readBuf[sizeof(readBuf)-1] = '\0';
        LOGV("no '/' in '%s' (file=%s count=%d)\n",
            readBuf, pathBuf, (int) count);
        return false;
    }

    memcpy(buf, cp+1, count);   /* count-1 for cp+1, count+1 for NUL */
    return true;
#else
    return false;
#endif
}

/*
 * Print information about the specified thread.
 *
 * Works best when the thread in question is "self" or has been suspended.
 * When dumping a separate thread that's still running, set "isRunning" to
 * use a more cautious thread dump function.
 */
void dvmDumpThreadEx(const DebugOutputTarget* target, Thread* thread,
    bool isRunning)
{
    /* tied to ThreadStatus enum */
    static const char* kStatusNames[] = {
        "ZOMBIE", "RUNNABLE", "TIMED_WAIT", "MONITOR", "WAIT",
        "INITIALIZING", "STARTING", "NATIVE", "VMWAIT"
    };
    Object* threadObj;
    Object* groupObj;
    StringObject* nameStr;
    char* threadName = NULL;
    char* groupName = NULL;
    char schedulerGroupBuf[32];
    bool isDaemon;
    int priority;               // java.lang.Thread priority
    int policy;                 // pthread policy
    struct sched_param sp;      // pthread scheduling parameters

    threadObj = thread->threadObj;
    if (threadObj == NULL) {
        LOGW("Can't dump thread %d: threadObj not set\n", thread->threadId);
        return;
    }
    nameStr = (StringObject*) dvmGetFieldObject(threadObj,
                gDvm.offJavaLangThread_name);
    threadName = dvmCreateCstrFromString(nameStr);

    priority = dvmGetFieldInt(threadObj, gDvm.offJavaLangThread_priority);
    isDaemon = dvmGetFieldBoolean(threadObj, gDvm.offJavaLangThread_daemon);

    if (pthread_getschedparam(pthread_self(), &policy, &sp) != 0) {
        LOGW("Warning: pthread_getschedparam failed\n");
        policy = -1;
        sp.sched_priority = -1;
    }
    if (!getSchedulerGroup(thread, schedulerGroupBuf,sizeof(schedulerGroupBuf)))
    {
        strcpy(schedulerGroupBuf, "unknown");
    } else if (schedulerGroupBuf[0] == '\0') {
        strcpy(schedulerGroupBuf, "default");
    }

    /* a null value for group is not expected, but deal with it anyway */
    groupObj = (Object*) dvmGetFieldObject(threadObj,
                gDvm.offJavaLangThread_group);
    if (groupObj != NULL) {
        int offset = dvmFindFieldOffset(gDvm.classJavaLangThreadGroup,
            "name", "Ljava/lang/String;");
        if (offset < 0) {
            LOGW("Unable to find 'name' field in ThreadGroup\n");
        } else {
            nameStr = (StringObject*) dvmGetFieldObject(groupObj, offset);
            groupName = dvmCreateCstrFromString(nameStr);
        }
    }
    if (groupName == NULL)
        groupName = strdup("(BOGUS GROUP)");

    assert(thread->status < NELEM(kStatusNames));
    dvmPrintDebugMessage(target,
        "\"%s\"%s prio=%d tid=%d %s\n",
        threadName, isDaemon ? " daemon" : "",
        priority, thread->threadId, kStatusNames[thread->status]);
    dvmPrintDebugMessage(target,
        "  | group=\"%s\" sCount=%d dsCount=%d s=%c obj=%p self=%p\n",
        groupName, thread->suspendCount, thread->dbgSuspendCount,
        thread->isSuspended ? 'Y' : 'N', thread->threadObj, thread);
    dvmPrintDebugMessage(target,
        "  | sysTid=%d nice=%d sched=%d/%d cgrp=%s handle=%d\n",
        thread->systemTid, getpriority(PRIO_PROCESS, thread->systemTid),
        policy, sp.sched_priority, schedulerGroupBuf, (int)thread->handle);

#ifdef WITH_MONITOR_TRACKING
    if (!isRunning) {
        LockedObjectData* lod = thread->pLockedObjects;
        if (lod != NULL)
            dvmPrintDebugMessage(target, "  | monitors held:\n");
        else
            dvmPrintDebugMessage(target, "  | monitors held: <none>\n");
        while (lod != NULL) {
            dvmPrintDebugMessage(target, "  >  %p[%d] (%s)\n",
                lod->obj, lod->recursionCount, lod->obj->clazz->descriptor);
            lod = lod->next;
        }
    }
#endif

    if (isRunning)
        dvmDumpRunningThreadStack(target, thread);
    else
        dvmDumpThreadStack(target, thread);

    free(threadName);
    free(groupName);

}

/*
 * Get the name of a thread.
 *
 * For correctness, the caller should hold the thread list lock to ensure
 * that the thread doesn't go away mid-call.
 *
 * Returns a newly-allocated string, or NULL if the Thread doesn't have a name.
 */
char* dvmGetThreadName(Thread* thread)
{
    StringObject* nameObj;

    if (thread->threadObj == NULL) {
        LOGW("threadObj is NULL, name not available\n");
        return strdup("-unknown-");
    }

    nameObj = (StringObject*)
        dvmGetFieldObject(thread->threadObj, gDvm.offJavaLangThread_name);
    return dvmCreateCstrFromString(nameObj);
}

/*
 * Dump all threads to the log file -- just calls dvmDumpAllThreadsEx() with
 * an output target.
 */
void dvmDumpAllThreads(bool grabLock)
{
    DebugOutputTarget target;

    dvmCreateLogOutputTarget(&target, ANDROID_LOG_INFO, LOG_TAG);
    dvmDumpAllThreadsEx(&target, grabLock);
}

/*
 * Print information about all known threads.  Assumes they have been
 * suspended (or are in a non-interpreting state, e.g. WAIT or NATIVE).
 *
 * If "grabLock" is true, we grab the thread lock list.  This is important
 * to do unless the caller already holds the lock.
 */
void dvmDumpAllThreadsEx(const DebugOutputTarget* target, bool grabLock)
{
    Thread* thread;

    dvmPrintDebugMessage(target, "DALVIK THREADS:\n");

    if (grabLock)
        dvmLockThreadList(dvmThreadSelf());

    thread = gDvm.threadList;
    while (thread != NULL) {
        dvmDumpThreadEx(target, thread, false);

        /* verify link */
        assert(thread->next == NULL || thread->next->prev == thread);

        thread = thread->next;
    }

    if (grabLock)
        dvmUnlockThreadList();
}

#ifdef WITH_MONITOR_TRACKING
/*
 * Count up the #of locked objects in the current thread.
 */
static int getThreadObjectCount(const Thread* self)
{
    LockedObjectData* lod;
    int count = 0;

    lod = self->pLockedObjects;
    while (lod != NULL) {
        count++;
        lod = lod->next;
    }
    return count;
}

/*
 * Add the object to the thread's locked object list if it doesn't already
 * exist.  The most recently added object is the most likely to be released
 * next, so we insert at the head of the list.
 *
 * If it already exists, we increase the recursive lock count.
 *
 * The object's lock may be thin or fat.
 */
void dvmAddToMonitorList(Thread* self, Object* obj, bool withTrace)
{
    LockedObjectData* newLod;
    LockedObjectData* lod;
    int* trace;
    int depth;

    lod = self->pLockedObjects;
    while (lod != NULL) {
        if (lod->obj == obj) {
            lod->recursionCount++;
            LOGV("+++ +recursive lock %p -> %d\n", obj, lod->recursionCount);
            return;
        }
        lod = lod->next;
    }

    newLod = (LockedObjectData*) calloc(1, sizeof(LockedObjectData));
    if (newLod == NULL) {
        LOGE("malloc failed on %d bytes\n", sizeof(LockedObjectData));
        return;
    }
    newLod->obj = obj;
    newLod->recursionCount = 0;

    if (withTrace) {
        trace = dvmFillInStackTraceRaw(self, &depth);
        newLod->rawStackTrace = trace;
        newLod->stackDepth = depth;
    }

    newLod->next = self->pLockedObjects;
    self->pLockedObjects = newLod;

    LOGV("+++ threadid=%d: added %p, now %d\n",
        self->threadId, newLod, getThreadObjectCount(self));
}

/*
 * Remove the object from the thread's locked object list.  If the entry
 * has a nonzero recursion count, we just decrement the count instead.
 */
void dvmRemoveFromMonitorList(Thread* self, Object* obj)
{
    LockedObjectData* lod;
    LockedObjectData* prevLod;

    lod = self->pLockedObjects;
    prevLod = NULL;
    while (lod != NULL) {
        if (lod->obj == obj) {
            if (lod->recursionCount > 0) {
                lod->recursionCount--;
                LOGV("+++ -recursive lock %p -> %d\n",
                    obj, lod->recursionCount);
                return;
            } else {
                break;
            }
        }
        prevLod = lod;
        lod = lod->next;
    }

    if (lod == NULL) {
        LOGW("BUG: object %p not found in thread's lock list\n", obj);
        return;
    }
    if (prevLod == NULL) {
        /* first item in list */
        assert(self->pLockedObjects == lod);
        self->pLockedObjects = lod->next;
    } else {
        /* middle/end of list */
        prevLod->next = lod->next;
    }

    LOGV("+++ threadid=%d: removed %p, now %d\n",
        self->threadId, lod, getThreadObjectCount(self));
    free(lod->rawStackTrace);
    free(lod);
}

/*
 * If the specified object is already in the thread's locked object list,
 * return the LockedObjectData struct.  Otherwise return NULL.
 */
LockedObjectData* dvmFindInMonitorList(const Thread* self, const Object* obj)
{
    LockedObjectData* lod;

    lod = self->pLockedObjects;
    while (lod != NULL) {
        if (lod->obj == obj)
            return lod;
        lod = lod->next;
    }
    return NULL;
}
#endif /*WITH_MONITOR_TRACKING*/


/*
 * GC helper functions
 */

/*
 * Add the contents of the registers from the interpreted call stack.
 */
static void gcScanInterpStackReferences(Thread *thread)
{
    const u4 *framePtr;
#if WITH_EXTRA_GC_CHECKS > 1
    bool first = true;
#endif

    framePtr = (const u4 *)thread->curFrame;
    while (framePtr != NULL) {
        const StackSaveArea *saveArea;
        const Method *method;

        saveArea = SAVEAREA_FROM_FP(framePtr);
        method = saveArea->method;
        if (method != NULL && !dvmIsNativeMethod(method)) {
#ifdef COUNT_PRECISE_METHODS
            /* the GC is running, so no lock required */
            if (dvmPointerSetAddEntry(gDvm.preciseMethods, method))
                LOGI("PGC: added %s.%s %p\n",
                    method->clazz->descriptor, method->name, method);
#endif
#if WITH_EXTRA_GC_CHECKS > 1
            /*
             * May also want to enable the memset() in the "invokeMethod"
             * goto target in the portable interpreter.  That sets the stack
             * to a pattern that makes referring to uninitialized data
             * very obvious.
             */

            if (first) {
                /*
                 * First frame, isn't native, check the "alternate" saved PC
                 * as a sanity check.
                 *
                 * It seems like we could check the second frame if the first
                 * is native, since the PCs should be the same.  It turns out
                 * this doesn't always work.  The problem is that we could
                 * have calls in the sequence:
                 *   interp method #2
                 *   native method
                 *   interp method #1
                 *
                 * and then GC while in the native method after returning
                 * from interp method #2.  The currentPc on the stack is
                 * for interp method #1, but thread->currentPc2 is still
                 * set for the last thing interp method #2 did.
                 *
                 * This can also happen in normal execution:
                 * - sget-object on not-yet-loaded class
                 * - class init updates currentPc2
                 * - static field init is handled by parsing annotations;
                 *   static String init requires creation of a String object,
                 *   which can cause a GC
                 *
                 * Essentially, any pattern that involves executing
                 * interpreted code and then causes an allocation without
                 * executing instructions in the original method will hit
                 * this.  These are rare enough that the test still has
                 * some value.
                 */
                if (saveArea->xtra.currentPc != thread->currentPc2) {
                    LOGW("PGC: savedPC(%p) != current PC(%p), %s.%s ins=%p\n",
                        saveArea->xtra.currentPc, thread->currentPc2,
                        method->clazz->descriptor, method->name, method->insns);
                    if (saveArea->xtra.currentPc != NULL)
                        LOGE("  pc inst = 0x%04x\n", *saveArea->xtra.currentPc);
                    if (thread->currentPc2 != NULL)
                        LOGE("  pc2 inst = 0x%04x\n", *thread->currentPc2);
                    dvmDumpThread(thread, false);
                }
            } else {
                /*
                 * It's unusual, but not impossible, for a non-first frame
                 * to be at something other than a method invocation.  For
                 * example, if we do a new-instance on a nonexistent class,
                 * we'll have a lot of class loader activity on the stack
                 * above the frame with the "new" operation.  Could also
                 * happen while we initialize a Throwable when an instruction
                 * fails.
                 *
                 * So there's not much we can do here to verify the PC,
                 * except to verify that it's a GC point.
                 */
            }
            assert(saveArea->xtra.currentPc != NULL);
#endif

            const RegisterMap* pMap;
            const u1* regVector;
            int i;

            Method* nonConstMethod = (Method*) method;  // quiet gcc
            pMap = dvmGetExpandedRegisterMap(nonConstMethod);
            if (pMap != NULL) {
                /* found map, get registers for this address */
                int addr = saveArea->xtra.currentPc - method->insns;
                regVector = dvmRegisterMapGetLine(pMap, addr);
                if (regVector == NULL) {
                    LOGW("PGC: map but no entry for %s.%s addr=0x%04x\n",
                        method->clazz->descriptor, method->name, addr);
                } else {
                    LOGV("PGC: found map for %s.%s 0x%04x (t=%d)\n",
                        method->clazz->descriptor, method->name, addr,
                        thread->threadId);
                }
            } else {
                /*
                 * No map found.  If precise GC is disabled this is
                 * expected -- we don't create pointers to the map data even
                 * if it's present -- but if it's enabled it means we're
                 * unexpectedly falling back on a conservative scan, so it's
                 * worth yelling a little.
                 */
                if (gDvm.preciseGc) {
                    LOGVV("PGC: no map for %s.%s\n",
                        method->clazz->descriptor, method->name);
                }
                regVector = NULL;
            }

            if (regVector == NULL) {
                /* conservative scan */
                for (i = method->registersSize - 1; i >= 0; i--) {
                    u4 rval = *framePtr++;
                    if (rval != 0 && (rval & 0x3) == 0) {
                        dvmMarkIfObject((Object *)rval);
                    }
                }
            } else {
                /*
                 * Precise scan.  v0 is at the lowest address on the
                 * interpreted stack, and is the first bit in the register
                 * vector, so we can walk through the register map and
                 * memory in the same direction.
                 *
                 * A '1' bit indicates a live reference.
                 */
                u2 bits = 1 << 1;
                for (i = method->registersSize - 1; i >= 0; i--) {
                    u4 rval = *framePtr++;

                    bits >>= 1;
                    if (bits == 1) {
                        /* set bit 9 so we can tell when we're empty */
                        bits = *regVector++ | 0x0100;
                        LOGVV("loaded bits: 0x%02x\n", bits & 0xff);
                    }

                    if (rval != 0 && (bits & 0x01) != 0) {
                        /*
                         * Non-null, register marked as live reference.  This
                         * should always be a valid object.
                         */
#if WITH_EXTRA_GC_CHECKS > 0
                        if ((rval & 0x3) != 0 ||
                            !dvmIsValidObject((Object*) rval))
                        {
                            /* this is very bad */
                            LOGE("PGC: invalid ref in reg %d: 0x%08x\n",
                                method->registersSize-1 - i, rval);
                        } else
#endif
                        {
                            dvmMarkObjectNonNull((Object *)rval);
                        }
                    } else {
                        /*
                         * Null or non-reference, do nothing at all.
                         */
#if WITH_EXTRA_GC_CHECKS > 1
                        if (dvmIsValidObject((Object*) rval)) {
                            /* this is normal, but we feel chatty */
                            LOGD("PGC: ignoring valid ref in reg %d: 0x%08x\n",
                                method->registersSize-1 - i, rval);
                        }
#endif
                    }
                }
                dvmReleaseRegisterMapLine(pMap, regVector);
            }
        }
        /* else this is a break frame and there is nothing to mark, or
         * this is a native method and the registers are just the "ins",
         * copied from various registers in the caller's set.
         */

#if WITH_EXTRA_GC_CHECKS > 1
        first = false;
#endif

        /* Don't fall into an infinite loop if things get corrupted.
         */
        assert((uintptr_t)saveArea->prevFrame > (uintptr_t)framePtr ||
               saveArea->prevFrame == NULL);
        framePtr = saveArea->prevFrame;
    }
}

static void gcScanReferenceTable(ReferenceTable *refTable)
{
    Object **op;

    //TODO: these asserts are overkill; turn them off when things stablize.
    assert(refTable != NULL);
    assert(refTable->table != NULL);
    assert(refTable->nextEntry != NULL);
    assert((uintptr_t)refTable->nextEntry >= (uintptr_t)refTable->table);
    assert(refTable->nextEntry - refTable->table <= refTable->maxEntries);

    op = refTable->table;
    while ((uintptr_t)op < (uintptr_t)refTable->nextEntry) {
        dvmMarkObjectNonNull(*(op++));
    }
}

static void gcScanIndirectRefTable(IndirectRefTable* pRefTable)
{
    Object** op = pRefTable->table;
    int numEntries = dvmIndirectRefTableEntries(pRefTable);
    int i;

    for (i = 0; i < numEntries; i++) {
        Object* obj = *op;
        if (obj != NULL)
            dvmMarkObjectNonNull(obj);
        op++;
    }
}

/*
 * Scan a Thread and mark any objects it references.
 */
static void gcScanThread(Thread *thread)
{
    assert(thread != NULL);

    /*
     * The target thread must be suspended or in a state where it can't do
     * any harm (e.g. in Object.wait()).  The only exception is the current
     * thread, which will still be active and in the "running" state.
     *
     * (Newly-created threads shouldn't be able to shift themselves to
     * RUNNING without a suspend-pending check, so this shouldn't cause
     * a false-positive.)
     */
    assert(thread->status != THREAD_RUNNING || thread->isSuspended ||
            thread == dvmThreadSelf());

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_THREAD_OBJECT, thread->threadId);

    dvmMarkObject(thread->threadObj);   // could be NULL, when constructing

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_NATIVE_STACK, thread->threadId);

    dvmMarkObject(thread->exception);   // usually NULL
    gcScanReferenceTable(&thread->internalLocalRefTable);

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_JNI_LOCAL, thread->threadId);

#ifdef USE_INDIRECT_REF
    gcScanIndirectRefTable(&thread->jniLocalRefTable);
#else
    gcScanReferenceTable(&thread->jniLocalRefTable);
#endif

    if (thread->jniMonitorRefTable.table != NULL) {
        HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_JNI_MONITOR, thread->threadId);

        gcScanReferenceTable(&thread->jniMonitorRefTable);
    }

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_JAVA_FRAME, thread->threadId);

    gcScanInterpStackReferences(thread);

    HPROF_CLEAR_GC_SCAN_STATE();
}

static void gcScanAllThreads()
{
    Thread *thread;

    /* Lock the thread list so we can safely use the
     * next/prev pointers.
     */
    dvmLockThreadList(dvmThreadSelf());

    for (thread = gDvm.threadList; thread != NULL;
            thread = thread->next)
    {
        /* We need to scan our own stack, so don't special-case
         * the current thread.
         */
        gcScanThread(thread);
    }

    dvmUnlockThreadList();
}

void dvmGcScanRootThreadGroups()
{
    /* We scan the VM's list of threads instead of going
     * through the actual ThreadGroups, but it should be
     * equivalent.
     *
     * This assumes that the ThreadGroup class object is in
     * the root set, which should always be true;  it's
     * loaded by the built-in class loader, which is part
     * of the root set.
     */
    gcScanAllThreads();
}
