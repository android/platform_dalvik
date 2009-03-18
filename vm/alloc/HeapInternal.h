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
 * Types and macros used internally by the heap.
 */
#ifndef _DALVIK_ALLOC_HEAP_INTERNAL
#define _DALVIK_ALLOC_HEAP_INTERNAL

#include <time.h>  // for struct timespec

#include "HeapTable.h"
#include "MarkSweep.h"

#define SCHEDULED_REFERENCE_MAGIC   ((Object*)0x87654321)

#define ptr2chunk(p)    (((DvmHeapChunk *)(p)) - 1)
#define chunk2ptr(p)    ((void *)(((DvmHeapChunk *)(p)) + 1))

#define WITH_OBJECT_HEADERS 0
#if WITH_OBJECT_HEADERS
#define OBJECT_HEADER   0x11335577
extern u2 gGeneration;
#endif

typedef struct DvmHeapChunk {
#if WITH_OBJECT_HEADERS
    u4 header;
    const Object *parent;
    const Object *parentOld;
    const Object *markFinger;
    const Object *markFingerOld;
    u2 birthGeneration;
    u2 markCount;
    u2 scanCount;
    u2 oldMarkGeneration;
    u2 markGeneration;
    u2 oldScanGeneration;
    u2 scanGeneration;
#endif
#if WITH_HPROF && WITH_HPROF_STACK
    u4 stackTraceSerialNumber;
#endif
    u8 data[0];
} DvmHeapChunk;

struct GcHeap {
    HeapSource      *heapSource;

    /* List of heap objects that the GC should never collect.
     * These should be included in the root set of objects.
     */
    HeapRefTable    nonCollectableRefs;

    /* List of heap objects that will require finalization when
     * collected.  I.e., instance objects
     *
     *     a) whose class definitions override java.lang.Object.finalize()
     *
     * *** AND ***
     *
     *     b) that have never been finalized.
     *
     * Note that this does not exclude non-garbage objects;  this
     * is not the list of pending finalizations, but of objects that
     * potentially have finalization in their futures.
     */
    LargeHeapRefTable  *finalizableRefs;

    /* The list of objects that need to have finalize() called
     * on themselves.  These references are part of the root set.
     *
     * This table is protected by gDvm.heapWorkerListLock, which must
     * be acquired after the heap lock.
     */
    LargeHeapRefTable  *pendingFinalizationRefs;

    /* Linked lists of subclass instances of java/lang/ref/Reference
     * that we find while recursing.  The "next" pointers are hidden
     * in the objects' <code>int Reference.vmData</code> fields.
     * These lists are cleared and rebuilt each time the GC runs.
     */
    Object         *softReferences;
    Object         *weakReferences;
    Object         *phantomReferences;

    /* The list of Reference objects that need to be cleared and/or
     * enqueued.  The bottom two bits of the object pointers indicate
     * whether they should be cleared and/or enqueued.
     *
     * This table is protected by gDvm.heapWorkerListLock, which must
     * be acquired after the heap lock.
     */
    LargeHeapRefTable  *referenceOperations;

    /* If non-null, the method that the HeapWorker is currently
     * executing.
     */
    Object *heapWorkerCurrentObject;
    Method *heapWorkerCurrentMethod;

    /* If heapWorkerCurrentObject is non-null, this gives the time when
     * HeapWorker started executing that method.  The time value must come
     * from dvmGetRelativeTimeUsec().
     *
     * The "Cpu" entry tracks the per-thread CPU timer (when available).
     */
    u8 heapWorkerInterpStartTime;
    u8 heapWorkerInterpCpuStartTime;

    /* If any fields are non-zero, indicates the next (absolute) time that
     * the HeapWorker thread should call dvmHeapSourceTrim().
     */
    struct timespec heapWorkerNextTrim;

    /* The current state of the mark step.
     * Only valid during a GC.
     */
    GcMarkContext   markContext;

    /* Set to dvmGetRelativeTimeUsec() whenever a GC begins.
     * The value is preserved between GCs, so it can be used
     * to determine the time between successive GCs.
     * Initialized to zero before the first GC.
     */
    u8              gcStartTime;

    /* Is the GC running?  Used to avoid recursive calls to GC.
     */
    bool            gcRunning;

    /* Set at the end of a GC to indicate the collection policy
     * for SoftReferences during the following GC.
     */
    enum { SR_COLLECT_NONE, SR_COLLECT_SOME, SR_COLLECT_ALL }
                    softReferenceCollectionState;

    /* The size of the heap is compared against this value
     * to determine when to start collecting SoftReferences.
     */
    size_t          softReferenceHeapSizeThreshold;

    /* A value that will increment every time we see a SoftReference
     * whose referent isn't marked (during SR_COLLECT_SOME).
     * The absolute value is meaningless, and does not need to
     * be reset or initialized at any point.
     */
    int             softReferenceColor;

    /* Indicates whether or not the object scanner should bother
     * keeping track of any references.  If markAllReferents is
     * true, referents will be hard-marked.  If false, normal
     * reference following is used.
     */
    bool            markAllReferents;

#if DVM_TRACK_HEAP_MARKING
    /* Every time an unmarked object becomes marked, markCount
     * is incremented and markSize increases by the size of
     * that object.
     */
    size_t          markCount;
    size_t          markSize;
#endif

    /*
     * Debug control values
     */

    int             ddmHpifWhen;
    int             ddmHpsgWhen;
    int             ddmHpsgWhat;
    int             ddmNhsgWhen;
    int             ddmNhsgWhat;

#if WITH_HPROF
    bool            hprofDumpOnGc;
    const char*     hprofFileName;
    hprof_context_t *hprofContext;
#endif
};

bool dvmLockHeap(void);
void dvmUnlockHeap(void);
void dvmLogGcStats(size_t numFreed, size_t sizeFreed, size_t gcTimeMs);
void dvmLogMadviseStats(size_t madvisedSizes[], size_t arrayLen);
void dvmHeapSizeChanged(void);

/*
 * Logging helpers
 */

#define HEAP_LOG_TAG      LOG_TAG "-heap"

#if LOG_NDEBUG
#define LOGV_HEAP(...)    ((void)0)
#define LOGD_HEAP(...)    ((void)0)
#else
#define LOGV_HEAP(...)    LOG(LOG_VERBOSE, HEAP_LOG_TAG, __VA_ARGS__)
#define LOGD_HEAP(...)    LOG(LOG_DEBUG, HEAP_LOG_TAG, __VA_ARGS__)
#endif
#define LOGI_HEAP(...)    LOG(LOG_INFO, HEAP_LOG_TAG, __VA_ARGS__)
#define LOGW_HEAP(...)    LOG(LOG_WARN, HEAP_LOG_TAG, __VA_ARGS__)
#define LOGE_HEAP(...)    LOG(LOG_ERROR, HEAP_LOG_TAG, __VA_ARGS__)

#define QUIET_ZYGOTE_GC 1
#if QUIET_ZYGOTE_GC
#undef LOGI_HEAP
#define LOGI_HEAP(...) \
    do { \
        if (!gDvm.zygote) { \
            LOG(LOG_INFO, HEAP_LOG_TAG, __VA_ARGS__); \
        } \
    } while (false)
#endif

#define FRACTIONAL_MB(n)    (n) / (1024 * 1024), \
                            ((((n) % (1024 * 1024)) / 1024) * 1000) / 1024
#define FRACTIONAL_PCT(n,max)    ((n) * 100) / (max), \
                                 (((n) * 1000) / (max)) % 10

#endif  // _DALVIK_ALLOC_HEAP_INTERNAL
