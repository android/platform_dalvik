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
 * Garbage-collecting memory allocator.
 */
#include "Dalvik.h"
#include "alloc/HeapTable.h"
#include "alloc/Heap.h"
#include "alloc/HeapInternal.h"
#include "alloc/DdmHeap.h"
#include "alloc/HeapSource.h"
#include "alloc/MarkSweep.h"

#include "utils/threads.h"      // need Android thread priorities
#define kInvalidPriority        10000

#include <sys/time.h>
#include <sys/resource.h>
#include <limits.h>
#include <errno.h>

#define kNonCollectableRefDefault   16
#define kFinalizableRefDefault      128

/*
 * Initialize the GC heap.
 *
 * Returns true if successful, false otherwise.
 */
bool dvmHeapStartup()
{
    GcHeap *gcHeap;

#if defined(WITH_ALLOC_LIMITS)
    gDvm.checkAllocLimits = false;
    gDvm.allocationLimit = -1;
#endif

    gcHeap = dvmHeapSourceStartup(gDvm.heapSizeStart, gDvm.heapSizeMax);
    if (gcHeap == NULL) {
        return false;
    }
    gcHeap->heapWorkerCurrentObject = NULL;
    gcHeap->heapWorkerCurrentMethod = NULL;
    gcHeap->heapWorkerInterpStartTime = 0LL;
    gcHeap->softReferenceCollectionState = SR_COLLECT_NONE;
    gcHeap->softReferenceHeapSizeThreshold = gDvm.heapSizeStart;
    gcHeap->ddmHpifWhen = 0;
    gcHeap->ddmHpsgWhen = 0;
    gcHeap->ddmHpsgWhat = 0;
    gcHeap->ddmNhsgWhen = 0;
    gcHeap->ddmNhsgWhat = 0;
#if WITH_HPROF
    gcHeap->hprofDumpOnGc = false;
    gcHeap->hprofContext = NULL;
#endif

    /* This needs to be set before we call dvmHeapInitHeapRefTable().
     */
    gDvm.gcHeap = gcHeap;

    /* Set up the table we'll use for ALLOC_NO_GC.
     */
    if (!dvmHeapInitHeapRefTable(&gcHeap->nonCollectableRefs,
                           kNonCollectableRefDefault))
    {
        LOGE_HEAP("Can't allocate GC_NO_ALLOC table\n");
        goto fail;
    }

    /* Set up the lists and lock we'll use for finalizable
     * and reference objects.
     */
    dvmInitMutex(&gDvm.heapWorkerListLock);
    gcHeap->finalizableRefs = NULL;
    gcHeap->pendingFinalizationRefs = NULL;
    gcHeap->referenceOperations = NULL;

    /* Initialize the HeapWorker locks and other state
     * that the GC uses.
     */
    dvmInitializeHeapWorkerState();

    return true;

fail:
    gDvm.gcHeap = NULL;
    dvmHeapSourceShutdown(gcHeap);
    return false;
}

bool dvmHeapStartupAfterZygote()
{
    /* Update our idea of the last GC start time so that we
     * don't use the last time that Zygote happened to GC.
     */
    gDvm.gcHeap->gcStartTime = dvmGetRelativeTimeUsec();

    return dvmHeapSourceStartupAfterZygote();
}

void dvmHeapShutdown()
{
//TODO: make sure we're locked
    if (gDvm.gcHeap != NULL) {
        GcHeap *gcHeap;

        gcHeap = gDvm.gcHeap;
        gDvm.gcHeap = NULL;

        /* Tables are allocated on the native heap;
         * they need to be cleaned up explicitly.
         * The process may stick around, so we don't
         * want to leak any native memory.
         */
        dvmHeapFreeHeapRefTable(&gcHeap->nonCollectableRefs);

        dvmHeapFreeLargeTable(gcHeap->finalizableRefs);
        gcHeap->finalizableRefs = NULL;

        dvmHeapFreeLargeTable(gcHeap->pendingFinalizationRefs);
        gcHeap->pendingFinalizationRefs = NULL;

        dvmHeapFreeLargeTable(gcHeap->referenceOperations);
        gcHeap->referenceOperations = NULL;

        /* Destroy the heap.  Any outstanding pointers
         * will point to unmapped memory (unless/until
         * someone else maps it).  This frees gcHeap
         * as a side-effect.
         */
        dvmHeapSourceShutdown(gcHeap);
    }
}

/*
 * We've been asked to allocate something we can't, e.g. an array so
 * large that (length * elementWidth) is larger than 2^31.  We want to
 * throw an OutOfMemoryError, but doing so implies that certain other
 * actions have taken place (like clearing soft references).
 *
 * TODO: for now we just throw an InternalError.
 */
void dvmThrowBadAllocException(const char* msg)
{
    dvmThrowException("Ljava/lang/InternalError;", msg);
}

/*
 * Grab the lock, but put ourselves into THREAD_VMWAIT if it looks like
 * we're going to have to wait on the mutex.
 */
bool dvmLockHeap()
{
    if (pthread_mutex_trylock(&gDvm.gcHeapLock) != 0) {
        Thread *self;
        ThreadStatus oldStatus;
        int cc;

        self = dvmThreadSelf();
        if (self != NULL) {
            oldStatus = dvmChangeStatus(self, THREAD_VMWAIT);
        } else {
            oldStatus = -1; // shut up gcc
        }

        cc = pthread_mutex_lock(&gDvm.gcHeapLock);
        assert(cc == 0);

        if (self != NULL) {
            dvmChangeStatus(self, oldStatus);
        }
    }

    return true;
}

void dvmUnlockHeap()
{
    dvmUnlockMutex(&gDvm.gcHeapLock);
}

/* Pop an object from the list of pending finalizations and
 * reference clears/enqueues, and return the object.
 * The caller must call dvmReleaseTrackedAlloc()
 * on the object when finished.
 *
 * Typically only called by the heap worker thread.
 */
Object *dvmGetNextHeapWorkerObject(HeapWorkerOperation *op)
{
    Object *obj;
    LargeHeapRefTable *table;
    GcHeap *gcHeap = gDvm.gcHeap;

    assert(op != NULL);

    obj = NULL;

    dvmLockMutex(&gDvm.heapWorkerListLock);

    /* We must handle reference operations before finalizations.
     * If:
     *     a) Someone subclasses WeakReference and overrides clear()
     *     b) A reference of this type is the last reference to
     *        a finalizable object
     * then we need to guarantee that the overridden clear() is called
     * on the reference before finalize() is called on the referent.
     * Both of these operations will always be scheduled at the same
     * time, so handling reference operations first will guarantee
     * the required order.
     */
    obj = dvmHeapGetNextObjectFromLargeTable(&gcHeap->referenceOperations);
    if (obj != NULL) {
        uintptr_t workBits;

        workBits = (uintptr_t)obj & (WORKER_CLEAR | WORKER_ENQUEUE);
        assert(workBits != 0);
        obj = (Object *)((uintptr_t)obj & ~(WORKER_CLEAR | WORKER_ENQUEUE));

        *op = workBits;
    } else {
        obj = dvmHeapGetNextObjectFromLargeTable(
                &gcHeap->pendingFinalizationRefs);
        if (obj != NULL) {
            *op = WORKER_FINALIZE;
        }
    }

    if (obj != NULL) {
        /* Don't let the GC collect the object until the
         * worker thread is done with it.
         *
         * This call is safe;  it uses thread-local storage
         * and doesn't acquire any locks.
         */
        dvmAddTrackedAlloc(obj, NULL);
    }

    dvmUnlockMutex(&gDvm.heapWorkerListLock);

    return obj;
}

/* Used for a heap size change hysteresis to avoid collecting
 * SoftReferences when the heap only grows by a small amount.
 */
#define SOFT_REFERENCE_GROWTH_SLACK (128 * 1024)

/* Whenever the effective heap size may have changed,
 * this function must be called.
 */
void dvmHeapSizeChanged()
{
    GcHeap *gcHeap = gDvm.gcHeap;
    size_t currentHeapSize;

    currentHeapSize = dvmHeapSourceGetIdealFootprint();

    /* See if the heap size has changed enough that we should care
     * about it.
     */
    if (currentHeapSize <= gcHeap->softReferenceHeapSizeThreshold -
            4 * SOFT_REFERENCE_GROWTH_SLACK)
    {
        /* The heap has shrunk enough that we'll use this as a new
         * threshold.  Since we're doing better on space, there's
         * no need to collect any SoftReferences.
         *
         * This is 4x the growth hysteresis because we don't want
         * to snap down so easily after a shrink.  If we just cleared
         * up a bunch of SoftReferences, we don't want to disallow
         * any new ones from being created.
         * TODO: determine if the 4x is important, needed, or even good
         */
        gcHeap->softReferenceHeapSizeThreshold = currentHeapSize;
        gcHeap->softReferenceCollectionState = SR_COLLECT_NONE;
    } else if (currentHeapSize >= gcHeap->softReferenceHeapSizeThreshold +
            SOFT_REFERENCE_GROWTH_SLACK)
    {
        /* The heap has grown enough to warrant collecting SoftReferences.
         */
        gcHeap->softReferenceHeapSizeThreshold = currentHeapSize;
        gcHeap->softReferenceCollectionState = SR_COLLECT_SOME;
    }
}


/* Do a full garbage collection, which may grow the
 * heap as a side-effect if the live set is large.
 */
static void gcForMalloc(bool collectSoftReferences)
{
#ifdef WITH_PROFILER
    if (gDvm.allocProf.enabled) {
        Thread* self = dvmThreadSelf();
        gDvm.allocProf.gcCount++;
        if (self != NULL) {
            self->allocProf.gcCount++;
        }
    }
#endif
    /* This may adjust the soft limit as a side-effect.
     */
    LOGD_HEAP("dvmMalloc initiating GC%s\n",
            collectSoftReferences ? "(collect SoftReferences)" : "");
    dvmCollectGarbageInternal(collectSoftReferences);
}

/* Try as hard as possible to allocate some memory.
 */
static DvmHeapChunk *tryMalloc(size_t size)
{
    DvmHeapChunk *hc;

    /* Don't try too hard if there's no way the allocation is
     * going to succeed.  We have to collect SoftReferences before
     * throwing an OOME, though.
     */
    if (size >= gDvm.heapSizeMax) {
        LOGW_HEAP("dvmMalloc(%zu/0x%08zx): "
                "someone's allocating a huge buffer\n", size, size);
        hc = NULL;
        goto collect_soft_refs;
    }

//TODO: figure out better heuristics
//    There will be a lot of churn if someone allocates a bunch of
//    big objects in a row, and we hit the frag case each time.
//    A full GC for each.
//    Maybe we grow the heap in bigger leaps
//    Maybe we skip the GC if the size is large and we did one recently
//      (number of allocations ago) (watch for thread effects)
//    DeflateTest allocs a bunch of ~128k buffers w/in 0-5 allocs of each other
//      (or, at least, there are only 0-5 objects swept each time)

    hc = dvmHeapSourceAlloc(size + sizeof(DvmHeapChunk));
    if (hc != NULL) {
        return hc;
    }

    /* The allocation failed.  Free up some space by doing
     * a full garbage collection.  This may grow the heap
     * if the live set is sufficiently large.
     */
    gcForMalloc(false);
    hc = dvmHeapSourceAlloc(size + sizeof(DvmHeapChunk));
    if (hc != NULL) {
        return hc;
    }

    /* Even that didn't work;  this is an exceptional state.
     * Try harder, growing the heap if necessary.
     */
    hc = dvmHeapSourceAllocAndGrow(size + sizeof(DvmHeapChunk));
    dvmHeapSizeChanged();
    if (hc != NULL) {
        size_t newHeapSize;

        newHeapSize = dvmHeapSourceGetIdealFootprint();
//TODO: may want to grow a little bit more so that the amount of free
//      space is equal to the old free space + the utilization slop for
//      the new allocation.
        LOGI_HEAP("Grow heap (frag case) to "
                "%zu.%03zuMB for %zu-byte allocation\n",
                FRACTIONAL_MB(newHeapSize), size);
        return hc;
    }

    /* Most allocations should have succeeded by now, so the heap
     * is really full, really fragmented, or the requested size is
     * really big.  Do another GC, collecting SoftReferences this
     * time.  The VM spec requires that all SoftReferences have
     * been collected and cleared before throwing an OOME.
     */
//TODO: wait for the finalizers from the previous GC to finish
collect_soft_refs:
    LOGI_HEAP("Forcing collection of SoftReferences for %zu-byte allocation\n",
            size);
    gcForMalloc(true);
    hc = dvmHeapSourceAllocAndGrow(size + sizeof(DvmHeapChunk));
    dvmHeapSizeChanged();
    if (hc != NULL) {
        return hc;
    }
//TODO: maybe wait for finalizers and try one last time

    LOGE_HEAP("Out of memory on a %zd-byte allocation.\n", size);
//TODO: tell the HeapSource to dump its state
    dvmDumpThread(dvmThreadSelf(), false);

    return NULL;
}

/* Throw an OutOfMemoryError if there's a thread to attach it to.
 * Avoid recursing.
 *
 * The caller must not be holding the heap lock, or else the allocations
 * in dvmThrowException() will deadlock.
 */
static void throwOOME()
{
    Thread *self;

    if ((self = dvmThreadSelf()) != NULL) {
        /* If the current (failing) dvmMalloc() happened as part of thread
         * creation/attachment before the thread became part of the root set,
         * we can't rely on the thread-local trackedAlloc table, so
         * we can't keep track of a real allocated OOME object.  But, since
         * the thread is in the process of being created, it won't have
         * a useful stack anyway, so we may as well make things easier
         * by throwing the (stackless) pre-built OOME.
         */
        if (dvmIsOnThreadList(self) && !self->throwingOOME) {
            /* Let ourselves know that we tried to throw an OOM
             * error in the normal way in case we run out of
             * memory trying to allocate it inside dvmThrowException().
             */
            self->throwingOOME = true;

            /* Don't include a description string;
             * one fewer allocation.
             */
            dvmThrowException("Ljava/lang/OutOfMemoryError;", NULL);
        } else {
            /*
             * This thread has already tried to throw an OutOfMemoryError,
             * which probably means that we're running out of memory
             * while recursively trying to throw.
             *
             * To avoid any more allocation attempts, "throw" a pre-built
             * OutOfMemoryError object (which won't have a useful stack trace).
             *
             * Note that since this call can't possibly allocate anything,
             * we don't care about the state of self->throwingOOME
             * (which will usually already be set).
             */
            dvmSetException(self, gDvm.outOfMemoryObj);
        }
        /* We're done with the possible recursion.
         */
        self->throwingOOME = false;
    }
}

/*
 * Allocate storage on the GC heap.  We guarantee 8-byte alignment.
 *
 * The new storage is zeroed out.
 *
 * Note that, in rare cases, this could get called while a GC is in
 * progress.  If a non-VM thread tries to attach itself through JNI,
 * it will need to allocate some objects.  If this becomes annoying to
 * deal with, we can block it at the source, but holding the allocation
 * mutex should be enough.
 *
 * In rare circumstances (JNI AttachCurrentThread) we can be called
 * from a non-VM thread.
 *
 * We implement ALLOC_NO_GC by maintaining an internal list of objects
 * that should not be collected.  This requires no actual flag storage in
 * the object itself, which is good, but makes flag queries expensive.
 *
 * Use ALLOC_DONT_TRACK when we either don't want to track an allocation
 * (because it's being done for the interpreter "new" operation and will
 * be part of the root set immediately) or we can't (because this allocation
 * is for a brand new thread).
 *
 * Returns NULL and throws an exception on failure.
 *
 * TODO: don't do a GC if the debugger thinks all threads are suspended
 */
void* dvmMalloc(size_t size, int flags)
{
    GcHeap *gcHeap = gDvm.gcHeap;
    DvmHeapChunk *hc;
    void *ptr;
    bool triedGc, triedGrowing;

#if 0
    /* handy for spotting large allocations */
    if (size >= 100000) {
        LOGI("dvmMalloc(%d):\n", size);
        dvmDumpThread(dvmThreadSelf(), false);
    }
#endif

#if defined(WITH_ALLOC_LIMITS)
    /*
     * See if they've exceeded the allocation limit for this thread.
     *
     * A limit value of -1 means "no limit".
     *
     * This is enabled at compile time because it requires us to do a
     * TLS lookup for the Thread pointer.  This has enough of a performance
     * impact that we don't want to do it if we don't have to.  (Now that
     * we're using gDvm.checkAllocLimits we may want to reconsider this,
     * but it's probably still best to just compile the check out of
     * production code -- one less thing to hit on every allocation.)
     */
    if (gDvm.checkAllocLimits) {
        Thread* self = dvmThreadSelf();
        if (self != NULL) {
            int count = self->allocLimit;
            if (count > 0) {
                self->allocLimit--;
            } else if (count == 0) {
                /* fail! */
                assert(!gDvm.initializing);
                self->allocLimit = -1;
                dvmThrowException("Ldalvik/system/AllocationLimitError;",
                    "thread allocation limit exceeded");
                return NULL;
            }
        }
    }

    if (gDvm.allocationLimit >= 0) {
        assert(!gDvm.initializing);
        gDvm.allocationLimit = -1;
        dvmThrowException("Ldalvik/system/AllocationLimitError;",
            "global allocation limit exceeded");
        return NULL;
    }
#endif

    dvmLockHeap();

    /* Try as hard as possible to allocate some memory.
     */
    hc = tryMalloc(size);
    if (hc != NULL) {
alloc_succeeded:
        /* We've got the memory.
         */
        if ((flags & ALLOC_FINALIZABLE) != 0) {
            /* This object is an instance of a class that
             * overrides finalize().  Add it to the finalizable list.
             *
             * Note that until DVM_OBJECT_INIT() is called on this
             * object, its clazz will be NULL.  Since the object is
             * in this table, it will be scanned as part of the root
             * set.  scanObject() explicitly deals with the NULL clazz.
             */
            if (!dvmHeapAddRefToLargeTable(&gcHeap->finalizableRefs,
                                    (Object *)hc->data))
            {
                LOGE_HEAP("dvmMalloc(): no room for any more "
                        "finalizable objects\n");
                dvmAbort();
            }
        }

#if WITH_OBJECT_HEADERS
        hc->header = OBJECT_HEADER;
        hc->birthGeneration = gGeneration;
#endif
        ptr = hc->data;

        /* The caller may not want us to collect this object.
         * If not, throw it in the nonCollectableRefs table, which
         * will be added to the root set when we GC.
         *
         * Note that until DVM_OBJECT_INIT() is called on this
         * object, its clazz will be NULL.  Since the object is
         * in this table, it will be scanned as part of the root
         * set.  scanObject() explicitly deals with the NULL clazz.
         */
        if ((flags & ALLOC_NO_GC) != 0) {
            if (!dvmHeapAddToHeapRefTable(&gcHeap->nonCollectableRefs, ptr)) {
                LOGE_HEAP("dvmMalloc(): no room for any more "
                        "ALLOC_NO_GC objects: %zd\n",
                        dvmHeapNumHeapRefTableEntries(
                                &gcHeap->nonCollectableRefs));
                dvmAbort();
            }
        }

#ifdef WITH_PROFILER
        if (gDvm.allocProf.enabled) {
            Thread* self = dvmThreadSelf();
            gDvm.allocProf.allocCount++;
            gDvm.allocProf.allocSize += size;
            if (self != NULL) {
                self->allocProf.allocCount++;
                self->allocProf.allocSize += size;
            }
        }
#endif
    } else {
        /* The allocation failed.
         */
        ptr = NULL;

#ifdef WITH_PROFILER
        if (gDvm.allocProf.enabled) {
            Thread* self = dvmThreadSelf();
            gDvm.allocProf.failedAllocCount++;
            gDvm.allocProf.failedAllocSize += size;
            if (self != NULL) {
                self->allocProf.failedAllocCount++;
                self->allocProf.failedAllocSize += size;
            }
        }
#endif
    }

    dvmUnlockHeap();

    if (ptr != NULL) {
        /*
         * If this block is immediately GCable, and they haven't asked us not
         * to track it, add it to the internal tracking list.
         *
         * If there's no "self" yet, we can't track it.  Calls made before
         * the Thread exists should use ALLOC_NO_GC.
         */
        if ((flags & (ALLOC_DONT_TRACK | ALLOC_NO_GC)) == 0) {
            dvmAddTrackedAlloc(ptr, NULL);
        }
    } else {
        /* 
         * The allocation failed; throw an OutOfMemoryError.
         */
        throwOOME();
    }

    return ptr;
}

/*
 * Returns true iff <obj> points to a valid allocated object.
 */
bool dvmIsValidObject(const Object* obj)
{
    const DvmHeapChunk *hc;

    /* Don't bother if it's NULL or not 8-byte aligned.
     */
    hc = ptr2chunk(obj);
    if (obj != NULL && ((uintptr_t)hc & (8-1)) == 0) {
        /* Even if the heap isn't locked, this shouldn't return
         * any false negatives.  The only mutation that could
         * be happening is allocation, which means that another
         * thread could be in the middle of a read-modify-write
         * to add a new bit for a new object.  However, that
         * RMW will have completed by the time any other thread
         * could possibly see the new pointer, so there is no
         * danger of dvmIsValidObject() being called on a valid
         * pointer whose bit isn't set.
         *
         * Freeing will only happen during the sweep phase, which
         * only happens while the heap is locked.
         */
        return dvmHeapSourceContains(hc);
    }
    return false;
}

/*
 * Clear flags that were passed into dvmMalloc() et al.
 * e.g., ALLOC_NO_GC, ALLOC_DONT_TRACK.
 */
void dvmClearAllocFlags(Object *obj, int mask)
{
    if ((mask & ALLOC_NO_GC) != 0) {
        dvmLockHeap();
        if (dvmIsValidObject(obj)) {
            if (!dvmHeapRemoveFromHeapRefTable(&gDvm.gcHeap->nonCollectableRefs,
                                               obj))
            {
                LOGE_HEAP("dvmMalloc(): failed to remove ALLOC_NO_GC bit from "
                        "object 0x%08x\n", (uintptr_t)obj);
                dvmAbort();
            }
//TODO: shrink if the table is very empty
        }
        dvmUnlockHeap();
    }

    if ((mask & ALLOC_DONT_TRACK) != 0) {
        dvmReleaseTrackedAlloc(obj, NULL);
    }
}

size_t dvmObjectSizeInHeap(const Object *obj)
{
    return dvmHeapSourceChunkSize(ptr2chunk(obj)) - sizeof(DvmHeapChunk);
}

/*
 * Initiate garbage collection.
 *
 * NOTES:
 * - If we don't hold gDvm.threadListLock, it's possible for a thread to
 *   be added to the thread list while we work.  The thread should NOT
 *   start executing, so this is only interesting when we start chasing
 *   thread stacks.  (Before we do so, grab the lock.)
 *
 * We are not allowed to GC when the debugger has suspended the VM, which
 * is awkward because debugger requests can cause allocations.  The easiest
 * way to enforce this is to refuse to GC on an allocation made by the
 * JDWP thread -- we have to expand the heap or fail.
 */
void dvmCollectGarbageInternal(bool collectSoftReferences)
{
    GcHeap *gcHeap = gDvm.gcHeap;
    Object *softReferences;
    Object *weakReferences;
    Object *phantomReferences;

    u8 now;
    s8 timeSinceLastGc;
    s8 gcElapsedTime;
    int numFreed;
    size_t sizeFreed;

#if DVM_TRACK_HEAP_MARKING
    /* Since weak and soft references are always cleared,
     * they don't require any marking.
     * (Soft are lumped into strong when they aren't cleared.)
     */
    size_t strongMarkCount = 0;
    size_t strongMarkSize = 0;
    size_t finalizeMarkCount = 0;
    size_t finalizeMarkSize = 0;
    size_t phantomMarkCount = 0;
    size_t phantomMarkSize = 0;
#endif

    /* The heap lock must be held.
     */

    if (gcHeap->gcRunning) {
        LOGW_HEAP("Attempted recursive GC\n");
        return;
    }
    gcHeap->gcRunning = true;
    now = dvmGetRelativeTimeUsec();
    if (gcHeap->gcStartTime != 0) {
        timeSinceLastGc = (now - gcHeap->gcStartTime) / 1000;
    } else {
        timeSinceLastGc = 0;
    }
    gcHeap->gcStartTime = now;

    LOGV_HEAP("GC starting -- suspending threads\n");

    dvmSuspendAllThreads(SUSPEND_FOR_GC);

    /* Get the priority (the "nice" value) of the current thread.  The
     * getpriority() call can legitimately return -1, so we have to
     * explicitly test errno.
     */
    errno = 0;
    int oldThreadPriority = kInvalidPriority;
    int priorityResult = getpriority(PRIO_PROCESS, 0);
    if (errno != 0) {
        LOGI_HEAP("getpriority(self) failed: %s\n", strerror(errno));
    } else if (priorityResult > ANDROID_PRIORITY_NORMAL) {
        /* Current value is numerically greater than "normal", which
         * in backward UNIX terms means lower priority.
         */
        if (setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_NORMAL) != 0) {
            LOGI_HEAP("Unable to elevate priority from %d to %d\n",
                priorityResult, ANDROID_PRIORITY_NORMAL);
        } else {
            /* priority elevated; save value so we can restore it later */
            LOGD_HEAP("Elevating priority from %d to %d\n",
                priorityResult, ANDROID_PRIORITY_NORMAL);
            oldThreadPriority = priorityResult;
        }
    }

    /* Wait for the HeapWorker thread to block.
     * (It may also already be suspended in interp code,
     * in which case it's not holding heapWorkerLock.)
     */
    dvmLockMutex(&gDvm.heapWorkerLock);

    /* Make sure that the HeapWorker thread hasn't become
     * wedged inside interp code.  If it has, this call will
     * print a message and abort the VM.
     */
    dvmAssertHeapWorkerThreadRunning();

    /* Lock the pendingFinalizationRefs list.
     *
     * Acquire the lock after suspending so the finalizer
     * thread can't block in the RUNNING state while
     * we try to suspend.
     */
    dvmLockMutex(&gDvm.heapWorkerListLock);

#ifdef WITH_PROFILER
    dvmMethodTraceGCBegin();
#endif

#if WITH_HPROF

/* Set DUMP_HEAP_ON_DDMS_UPDATE to 1 to enable heap dumps
 * whenever DDMS requests a heap update (HPIF chunk).
 * The output files will appear in /data/misc, which must
 * already exist.
 * You must define "WITH_HPROF := true" in your buildspec.mk
 * and recompile libdvm for this to work.
 *
 * To enable stack traces for each allocation, define
 * "WITH_HPROF_STACK := true" in buildspec.mk.  This option slows down
 * allocations and also requires 8 additional bytes per object on the
 * GC heap.
 */
#define DUMP_HEAP_ON_DDMS_UPDATE 0
#if DUMP_HEAP_ON_DDMS_UPDATE
    gcHeap->hprofDumpOnGc |= (gcHeap->ddmHpifWhen != 0);
#endif

    if (gcHeap->hprofDumpOnGc) {
        char nameBuf[128];

        if (gcHeap->hprofFileName == NULL) {
            /* no filename was provided; invent one */
            sprintf(nameBuf, "/data/misc/heap-dump-tm%d-pid%d.hprof",
                (int) time(NULL), (int) getpid());
            gcHeap->hprofFileName = nameBuf;
        }
        gcHeap->hprofContext = hprofStartup(gcHeap->hprofFileName);
        if (gcHeap->hprofContext != NULL) {
            hprofStartHeapDump(gcHeap->hprofContext);
        }
        gcHeap->hprofDumpOnGc = false;
        gcHeap->hprofFileName = NULL;
    }
#endif

    if (timeSinceLastGc < 10000) {
        LOGD_HEAP("GC! (%dms since last GC)\n",
                (int)timeSinceLastGc);
    } else {
        LOGD_HEAP("GC! (%d sec since last GC)\n",
                (int)(timeSinceLastGc / 1000));
    }
#if DVM_TRACK_HEAP_MARKING
    gcHeap->markCount = 0;
    gcHeap->markSize = 0;
#endif

    /* Set up the marking context.
     */
    dvmHeapBeginMarkStep();

    /* Mark the set of objects that are strongly reachable from the roots.
     */
    LOGD_HEAP("Marking...");
    dvmHeapMarkRootSet();

    /* dvmHeapScanMarkedObjects() will build the lists of known
     * instances of the Reference classes.
     */
    gcHeap->softReferences = NULL;
    gcHeap->weakReferences = NULL;
    gcHeap->phantomReferences = NULL;

    /* Make sure that we don't hard-mark the referents of Reference
     * objects by default.
     */
    gcHeap->markAllReferents = false;

    /* Don't mark SoftReferences if our caller wants us to collect them.
     * This has to be set before calling dvmHeapScanMarkedObjects().
     */
    if (collectSoftReferences) {
        gcHeap->softReferenceCollectionState = SR_COLLECT_ALL;
    }

    /* Recursively mark any objects that marked objects point to strongly.
     * If we're not collecting soft references, soft-reachable
     * objects will also be marked.
     */
    LOGD_HEAP("Recursing...");
    dvmHeapScanMarkedObjects();
#if DVM_TRACK_HEAP_MARKING
    strongMarkCount = gcHeap->markCount;
    strongMarkSize = gcHeap->markSize;
    gcHeap->markCount = 0;
    gcHeap->markSize = 0;
#endif

    /* Latch these so that the other calls to dvmHeapScanMarkedObjects() don't
     * mess with them.
     */
    softReferences = gcHeap->softReferences;
    weakReferences = gcHeap->weakReferences;
    phantomReferences = gcHeap->phantomReferences;

    /* All strongly-reachable objects have now been marked.
     */
    if (gcHeap->softReferenceCollectionState != SR_COLLECT_NONE) {
        LOGD_HEAP("Handling soft references...");
        dvmHeapHandleReferences(softReferences, REF_SOFT);
        // markCount always zero

        /* Now that we've tried collecting SoftReferences,
         * fall back to not collecting them.  If the heap
         * grows, we will start collecting again.
         */
        gcHeap->softReferenceCollectionState = SR_COLLECT_NONE;
    } // else dvmHeapScanMarkedObjects() already marked the soft-reachable set
    LOGD_HEAP("Handling weak references...");
    dvmHeapHandleReferences(weakReferences, REF_WEAK);
    // markCount always zero

    /* Once all weak-reachable objects have been taken
     * care of, any remaining unmarked objects can be finalized.
     */
    LOGD_HEAP("Finding finalizations...");
    dvmHeapScheduleFinalizations();
#if DVM_TRACK_HEAP_MARKING
    finalizeMarkCount = gcHeap->markCount;
    finalizeMarkSize = gcHeap->markSize;
    gcHeap->markCount = 0;
    gcHeap->markSize = 0;
#endif

    /* Any remaining objects that are not pending finalization
     * could be phantom-reachable.  This will mark any phantom-reachable
     * objects, as well as enqueue their references.
     */
    LOGD_HEAP("Handling phantom references...");
    dvmHeapHandleReferences(phantomReferences, REF_PHANTOM);
#if DVM_TRACK_HEAP_MARKING
    phantomMarkCount = gcHeap->markCount;
    phantomMarkSize = gcHeap->markSize;
    gcHeap->markCount = 0;
    gcHeap->markSize = 0;
#endif

//TODO: take care of JNI weak global references

#if DVM_TRACK_HEAP_MARKING
    LOGI_HEAP("Marked objects: %dB strong, %dB final, %dB phantom\n",
            strongMarkSize, finalizeMarkSize, phantomMarkSize);
#endif

#ifdef WITH_DEADLOCK_PREDICTION
    dvmDumpMonitorInfo("before sweep");
#endif
    LOGD_HEAP("Sweeping...");
    dvmHeapSweepUnmarkedObjects(&numFreed, &sizeFreed);
#ifdef WITH_DEADLOCK_PREDICTION
    dvmDumpMonitorInfo("after sweep");
#endif

    LOGD_HEAP("Cleaning up...");
    dvmHeapFinishMarkStep();

    LOGD_HEAP("Done.");

    /* Now's a good time to adjust the heap size, since
     * we know what our utilization is.
     *
     * This doesn't actually resize any memory;
     * it just lets the heap grow more when necessary.
     */
    dvmHeapSourceGrowForUtilization();
    dvmHeapSizeChanged();

#if WITH_HPROF
    if (gcHeap->hprofContext != NULL) {
        hprofFinishHeapDump(gcHeap->hprofContext);
//TODO: write a HEAP_SUMMARY record
        hprofShutdown(gcHeap->hprofContext);
        gcHeap->hprofContext = NULL;
    }
#endif

    /* Now that we've freed up the GC heap, return any large
     * free chunks back to the system.  They'll get paged back
     * in the next time they're used.  Don't do it immediately,
     * though;  if the process is still allocating a bunch of
     * memory, we'll be taking a ton of page faults that we don't
     * necessarily need to.
     *
     * Cancel any old scheduled trims, and schedule a new one.
     */
    dvmScheduleHeapSourceTrim(5);  // in seconds

#ifdef WITH_PROFILER
    dvmMethodTraceGCEnd();
#endif
    LOGV_HEAP("GC finished -- resuming threads\n");

    gcHeap->gcRunning = false;

    dvmUnlockMutex(&gDvm.heapWorkerListLock);
    dvmUnlockMutex(&gDvm.heapWorkerLock);

    dvmResumeAllThreads(SUSPEND_FOR_GC);
    if (oldThreadPriority != kInvalidPriority) {
        if (setpriority(PRIO_PROCESS, 0, oldThreadPriority) != 0) {
            LOGW_HEAP("Unable to reset priority to %d: %s\n",
                oldThreadPriority, strerror(errno));
        } else {
            LOGD_HEAP("Reset priority to %d\n", oldThreadPriority);
        }
    }
    gcElapsedTime = (dvmGetRelativeTimeUsec() - gcHeap->gcStartTime) / 1000;
    if (gcElapsedTime < 10000) {
        LOGD("GC freed %d objects / %zd bytes in %dms\n",
                numFreed, sizeFreed, (int)gcElapsedTime);
    } else {
        LOGD("GC freed %d objects / %zd bytes in %d sec\n",
                numFreed, sizeFreed, (int)(gcElapsedTime / 1000));
    }
    dvmLogGcStats(numFreed, sizeFreed, gcElapsedTime);

    if (gcHeap->ddmHpifWhen != 0) {
        LOGD_HEAP("Sending VM heap info to DDM\n");
        dvmDdmSendHeapInfo(gcHeap->ddmHpifWhen, false);
    }
    if (gcHeap->ddmHpsgWhen != 0) {
        LOGD_HEAP("Dumping VM heap to DDM\n");
        dvmDdmSendHeapSegments(false, false);
    }
    if (gcHeap->ddmNhsgWhen != 0) {
        LOGD_HEAP("Dumping native heap to DDM\n");
        dvmDdmSendHeapSegments(false, true);
    }
}

#if WITH_HPROF
/*
 * Perform garbage collection, writing heap information to the specified file.
 *
 * If "fileName" is NULL, a suitable name will be generated automatically.
 */
void hprofDumpHeap(const char* fileName)
{
    dvmLockMutex(&gDvm.gcHeapLock);

    gDvm.gcHeap->hprofDumpOnGc = true;
    gDvm.gcHeap->hprofFileName = fileName;
    dvmCollectGarbageInternal(false);

    dvmUnlockMutex(&gDvm.gcHeapLock);
}

void dvmHeapSetHprofGcScanState(hprof_heap_tag_t state, u4 threadSerialNumber)
{
    if (gDvm.gcHeap->hprofContext != NULL) {
        hprofSetGcScanState(gDvm.gcHeap->hprofContext, state,
                threadSerialNumber);
    }
}
#endif
