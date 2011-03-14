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

#include <cutils/mspace.h>
#include <stdint.h>     // for SIZE_MAX
#include <sys/mman.h>
#include <errno.h>

#include "Dalvik.h"
#include "alloc/Heap.h"
#include "alloc/HeapInternal.h"
#include "alloc/HeapSource.h"
#include "alloc/HeapBitmap.h"

// TODO: find a real header file for these.
extern int dlmalloc_trim(size_t);
extern void dlmalloc_walk_free_pages(void(*)(void*, void*, void*), void*);

static void snapIdealFootprint(void);
static void setIdealFootprint(size_t max);

#define ALIGN_UP_TO_PAGE_SIZE(p) \
    (((size_t)(p) + (SYSTEM_PAGE_SIZE - 1)) & ~(SYSTEM_PAGE_SIZE - 1))
#define ALIGN_DOWN_TO_PAGE_SIZE(p) \
    ((size_t)(p) & ~(SYSTEM_PAGE_SIZE - 1))

#define HEAP_UTILIZATION_MAX        1024
#define DEFAULT_HEAP_UTILIZATION    512     // Range 1..HEAP_UTILIZATION_MAX
#define HEAP_IDEAL_FREE             (2 * 1024 * 1024)
#define HEAP_MIN_FREE               (HEAP_IDEAL_FREE / 4)

/* Start a concurrent collection when free memory falls under this
 * many bytes.
 */
#define CONCURRENT_START (128 << 10)

/* The next GC will not be concurrent when free memory after a GC is
 * under this many bytes.
 */
#define CONCURRENT_MIN_FREE (CONCURRENT_START + (128 << 10))

#define HS_BOILERPLATE() \
    do { \
        assert(gDvm.gcHeap != NULL); \
        assert(gDvm.gcHeap->heapSource != NULL); \
        assert(gHs == gDvm.gcHeap->heapSource); \
    } while (0)

#define DEBUG_HEAP_SOURCE 0
#if DEBUG_HEAP_SOURCE
#define HSTRACE(...)  LOG(LOG_INFO, LOG_TAG "-hs", __VA_ARGS__)
#else
#define HSTRACE(...)  /**/
#endif

/*
=======================================================
=======================================================
=======================================================

How will this be used?
allocating/freeing: Heap.c just wants to say "alloc(n)" and get a ptr
    - if allocating in large doesn't work, try allocating from small
Heap.c will use HeapSource.h; HeapSource.c will do the right thing
    between small and large
    - some operations should be abstracted; put in a structure

How do we manage the size trade-offs?
- keep mspace max footprint clamped to actual footprint
- if small-alloc returns null, adjust large vs. small ratio
    - give small all available slack and retry
    - success or fail, snap back to actual footprint and give rest to large

managed as "small actual" + "large actual" + "delta to allowed total footprint"
- when allocating from one source or the other, give the delta to the
    active source, but snap back afterwards
- that may not work so great for a gc heap, because small will always consume.
    - but we need to use the memory, and the current max is the amount we
      need to fill before a GC.

Find a way to permanently steal pages from the middle of the heap
    - segment tricks?

Allocate String and char[] in a separate heap?

Maybe avoid growing small heap, even if there's slack?  Look at
live ratio of small heap after a gc; scale it based on that.

=======================================================
=======================================================
=======================================================
*/

typedef struct {
    /* The mspace to allocate from.
     */
    mspace msp;

    /* The largest size that this heap is allowed to grow to.
     */
    size_t absoluteMaxSize;

    /* Number of bytes allocated from this mspace for objects,
     * including any overhead.  This value is NOT exact, and
     * should only be used as an input for certain heuristics.
     */
    size_t bytesAllocated;

    /* Number of bytes allocated from this mspace at which a
     * concurrent garbage collection will be started.
     */
    size_t concurrentStartBytes;

    /* Number of objects currently allocated from this mspace.
     */
    size_t objectsAllocated;

    /*
     * The lowest address of this heap, inclusive.
     */
    char *base;

    /*
     * The highest address of this heap, exclusive.
     */
    char *limit;
} Heap;

struct HeapSource {
    /* Target ideal heap utilization ratio; range 1..HEAP_UTILIZATION_MAX
     */
    size_t targetUtilization;

    /* Requested minimum heap size, or zero if there is no minimum.
     */
    size_t minimumSize;

    /* The starting heap size.
     */
    size_t startSize;

    /* The largest that the heap source as a whole is allowed to grow.
     */
    size_t absoluteMaxSize;

    /* The desired max size of the heap source as a whole.
     */
    size_t idealSize;

    /* The maximum number of bytes allowed to be allocated from the
     * active heap before a GC is forced.  This is used to "shrink" the
     * heap in lieu of actual compaction.
     */
    size_t softLimit;

    /* The heaps; heaps[0] is always the active heap,
     * which new objects should be allocated from.
     */
    Heap heaps[HEAP_SOURCE_MAX_HEAP_COUNT];

    /* The current number of heaps.
     */
    size_t numHeaps;

    /* External allocation count.
     */
    size_t externalBytesAllocated;

    /* The maximum number of external bytes that may be allocated.
     */
    size_t externalLimit;

    /* True if zygote mode was active when the HeapSource was created.
     */
    bool sawZygote;

    /*
     * The base address of the virtual memory reservation.
     */
    char *heapBase;

    /*
     * The length in bytes of the virtual memory reservation.
     */
    size_t heapLength;

    /*
     * The live object bitmap.
     */
    HeapBitmap liveBits;

    /*
     * The mark bitmap.
     */
    HeapBitmap markBits;

    /*
     * State for the GC daemon.
     */
    bool hasGcThread;
    pthread_t gcThread;
    bool gcThreadShutdown;
    pthread_mutex_t gcThreadMutex;
    pthread_cond_t gcThreadCond;
};

#define hs2heap(hs_) (&((hs_)->heaps[0]))

/*
 * Returns true iff a soft limit is in effect for the active heap.
 */
static inline bool
softLimited(const HeapSource *hs)
{
    /* softLimit will be either SIZE_MAX or the limit for the
     * active mspace.  idealSize can be greater than softLimit
     * if there is more than one heap.  If there is only one
     * heap, a non-SIZE_MAX softLimit should always be the same
     * as idealSize.
     */
    return hs->softLimit <= hs->idealSize;
}

/*
 * Returns approximately the maximum number of bytes allowed to be
 * allocated from the active heap before a GC is forced.
 */
static size_t
getAllocLimit(const HeapSource *hs)
{
    if (softLimited(hs)) {
        return hs->softLimit;
    } else {
        return mspace_max_allowed_footprint(hs2heap(hs)->msp);
    }
}

/*
 * Returns the current footprint of all heaps.  If includeActive
 * is false, don't count the heap at index 0.
 */
static inline size_t
oldHeapOverhead(const HeapSource *hs, bool includeActive)
{
    size_t footprint = 0;
    size_t i;

    if (includeActive) {
        i = 0;
    } else {
        i = 1;
    }
    for (/* i = i */; i < hs->numHeaps; i++) {
//TODO: include size of bitmaps?  If so, don't use bitsLen, listen to .max
        footprint += mspace_footprint(hs->heaps[i].msp);
    }
    return footprint;
}

/*
 * Returns the heap that <ptr> could have come from, or NULL
 * if it could not have come from any heap.
 */
static inline Heap *
ptr2heap(const HeapSource *hs, const void *ptr)
{
    const size_t numHeaps = hs->numHeaps;
    size_t i;

//TODO: unroll this to HEAP_SOURCE_MAX_HEAP_COUNT
    if (ptr != NULL) {
        for (i = 0; i < numHeaps; i++) {
            const Heap *const heap = &hs->heaps[i];

            if ((const char *)ptr >= heap->base && (const char *)ptr < heap->limit) {
                return (Heap *)heap;
            }
        }
    }
    return NULL;
}

/*
 * Functions to update heapSource->bytesAllocated when an object
 * is allocated or freed.  mspace_usable_size() will give
 * us a much more accurate picture of heap utilization than
 * the requested byte sizes would.
 *
 * These aren't exact, and should not be treated as such.
 */
static void countAllocation(Heap *heap, const void *ptr, bool isObj)
{
    HeapSource *hs;

    assert(heap->bytesAllocated < mspace_footprint(heap->msp));

    heap->bytesAllocated += mspace_usable_size(heap->msp, ptr) +
            HEAP_SOURCE_CHUNK_OVERHEAD;
    if (isObj) {
        heap->objectsAllocated++;
        hs = gDvm.gcHeap->heapSource;
        dvmHeapBitmapSetObjectBit(&hs->liveBits, ptr);
    }

    assert(heap->bytesAllocated < mspace_footprint(heap->msp));
}

static void countFree(Heap *heap, const void *ptr, size_t *numBytes)
{
    HeapSource *hs;
    size_t delta;

    delta = mspace_usable_size(heap->msp, ptr) + HEAP_SOURCE_CHUNK_OVERHEAD;
    assert(delta > 0);
    if (delta < heap->bytesAllocated) {
        heap->bytesAllocated -= delta;
    } else {
        heap->bytesAllocated = 0;
    }
    hs = gDvm.gcHeap->heapSource;
    dvmHeapBitmapClearObjectBit(&hs->liveBits, ptr);
    if (heap->objectsAllocated > 0) {
        heap->objectsAllocated--;
    }
    *numBytes += delta;
}

static HeapSource *gHs = NULL;

static mspace
createMspace(void *base, size_t startSize, size_t absoluteMaxSize)
{
    mspace msp;

    /* Create an unlocked dlmalloc mspace to use as
     * a small-object heap source.
     *
     * We start off reserving heapSizeStart/2 bytes but
     * letting the heap grow to heapSizeStart.  This saves
     * memory in the case where a process uses even less
     * than the starting size.
     */
    LOGV_HEAP("Creating VM heap of size %u\n", startSize);
    errno = 0;
    msp = create_contiguous_mspace_with_base(startSize/2,
            absoluteMaxSize, /*locked=*/false, base);
    if (msp != NULL) {
        /* Don't let the heap grow past the starting size without
         * our intervention.
         */
        mspace_set_max_allowed_footprint(msp, startSize);
    } else {
        /* There's no guarantee that errno has meaning when the call
         * fails, but it often does.
         */
        LOGE_HEAP("Can't create VM heap of size (%u,%u): %s\n",
            startSize/2, absoluteMaxSize, strerror(errno));
    }

    return msp;
}

static bool
addNewHeap(HeapSource *hs, mspace msp, size_t mspAbsoluteMaxSize)
{
    Heap heap;

    if (hs->numHeaps >= HEAP_SOURCE_MAX_HEAP_COUNT) {
        LOGE("Attempt to create too many heaps (%zd >= %zd)\n",
                hs->numHeaps, HEAP_SOURCE_MAX_HEAP_COUNT);
        dvmAbort();
        return false;
    }

    memset(&heap, 0, sizeof(heap));

    if (msp != NULL) {
        heap.msp = msp;
        heap.absoluteMaxSize = mspAbsoluteMaxSize;
        heap.concurrentStartBytes = SIZE_MAX;
        heap.base = hs->heapBase;
        heap.limit = hs->heapBase + heap.absoluteMaxSize;
    } else {
        void *sbrk0 = contiguous_mspace_sbrk0(hs->heaps[0].msp);
        char *base = (char *)ALIGN_UP_TO_PAGE_SIZE(sbrk0);
        size_t overhead = base - hs->heapBase;

        assert(((size_t)hs->heaps[0].base & (SYSTEM_PAGE_SIZE - 1)) == 0);
        if (overhead + HEAP_MIN_FREE >= hs->absoluteMaxSize) {
            LOGE_HEAP("No room to create any more heaps "
                    "(%zd overhead, %zd max)\n",
                    overhead, hs->absoluteMaxSize);
            return false;
        }
        hs->heaps[0].absoluteMaxSize = base - hs->heaps[0].base;
        hs->heaps[0].limit = base;
        heap.absoluteMaxSize = hs->absoluteMaxSize - overhead;
        heap.msp = createMspace(base, HEAP_MIN_FREE, heap.absoluteMaxSize);
        heap.concurrentStartBytes = HEAP_MIN_FREE - CONCURRENT_START;
        heap.base = base;
        heap.limit = heap.base + heap.absoluteMaxSize;
        if (heap.msp == NULL) {
            return false;
        }
    }

    /* Don't let the soon-to-be-old heap grow any further.
     */
    if (hs->numHeaps > 0) {
        mspace msp = hs->heaps[0].msp;
        mspace_set_max_allowed_footprint(msp, mspace_footprint(msp));
    }

    /* Put the new heap in the list, at heaps[0].
     * Shift existing heaps down.
     */
    memmove(&hs->heaps[1], &hs->heaps[0], hs->numHeaps * sizeof(hs->heaps[0]));
    hs->heaps[0] = heap;
    hs->numHeaps++;

    return true;
}

/*
 * The garbage collection daemon.  Initiates a concurrent collection
 * when signaled.
 */
static void *gcDaemonThread(void* arg)
{
    dvmChangeStatus(NULL, THREAD_VMWAIT);
    dvmLockMutex(&gHs->gcThreadMutex);
    while (gHs->gcThreadShutdown != true) {
        dvmWaitCond(&gHs->gcThreadCond, &gHs->gcThreadMutex);
        dvmLockHeap();
        dvmChangeStatus(NULL, THREAD_RUNNING);
        dvmCollectGarbageInternal(false, GC_CONCURRENT);
        dvmChangeStatus(NULL, THREAD_VMWAIT);
        dvmUnlockHeap();
    }
    dvmChangeStatus(NULL, THREAD_RUNNING);
    return NULL;
}

static bool gcDaemonStartup(void)
{
    dvmInitMutex(&gHs->gcThreadMutex);
    pthread_cond_init(&gHs->gcThreadCond, NULL);
    gHs->gcThreadShutdown = false;
    gHs->hasGcThread = dvmCreateInternalThread(&gHs->gcThread, "GC",
                                               gcDaemonThread, NULL);
    return gHs->hasGcThread;
}

static void gcDaemonShutdown(void)
{
    if (gHs->hasGcThread) {
        dvmLockMutex(&gHs->gcThreadMutex);
        gHs->gcThreadShutdown = true;
        dvmSignalCond(&gHs->gcThreadCond);
        dvmUnlockMutex(&gHs->gcThreadMutex);
        pthread_join(gHs->gcThread, NULL);
    }
}

/*
 * Initializes the heap source; must be called before any other
 * dvmHeapSource*() functions.  Returns a GcHeap structure
 * allocated from the heap source.
 */
GcHeap *
dvmHeapSourceStartup(size_t startSize, size_t absoluteMaxSize)
{
    GcHeap *gcHeap;
    HeapSource *hs;
    mspace msp;
    size_t length;
    void *base;

    assert(gHs == NULL);

    if (startSize > absoluteMaxSize) {
        LOGE("Bad heap parameters (start=%d, max=%d)\n",
           startSize, absoluteMaxSize);
        return NULL;
    }

    /*
     * Allocate a contiguous region of virtual memory to subdivided
     * among the heaps managed by the garbage collector.
     */
    length = ALIGN_UP_TO_PAGE_SIZE(absoluteMaxSize);
    base = dvmAllocRegion(length, PROT_NONE, "dalvik-heap");
    if (base == NULL) {
        return NULL;
    }

    /* Create an unlocked dlmalloc mspace to use as
     * the small object heap source.
     */
    msp = createMspace(base, startSize, absoluteMaxSize);
    if (msp == NULL) {
        goto fail;
    }

    /* Allocate a descriptor from the heap we just created.
     */
    gcHeap = mspace_malloc(msp, sizeof(*gcHeap));
    if (gcHeap == NULL) {
        LOGE_HEAP("Can't allocate heap descriptor\n");
        goto fail;
    }
    memset(gcHeap, 0, sizeof(*gcHeap));

    hs = mspace_malloc(msp, sizeof(*hs));
    if (hs == NULL) {
        LOGE_HEAP("Can't allocate heap source\n");
        goto fail;
    }
    memset(hs, 0, sizeof(*hs));

    hs->targetUtilization = DEFAULT_HEAP_UTILIZATION;
    hs->minimumSize = 0;
    hs->startSize = startSize;
    hs->absoluteMaxSize = absoluteMaxSize;
    hs->idealSize = startSize;
    hs->softLimit = SIZE_MAX;    // no soft limit at first
    hs->numHeaps = 0;
    hs->sawZygote = gDvm.zygote;
    hs->hasGcThread = false;
    hs->heapBase = base;
    hs->heapLength = length;
    if (!addNewHeap(hs, msp, absoluteMaxSize)) {
        LOGE_HEAP("Can't add initial heap\n");
        goto fail;
    }
    if (!dvmHeapBitmapInit(&hs->liveBits, base, length, "dalvik-bitmap-1")) {
        LOGE_HEAP("Can't create liveBits\n");
        goto fail;
    }
    if (!dvmHeapBitmapInit(&hs->markBits, base, length, "dalvik-bitmap-2")) {
        LOGE_HEAP("Can't create markBits\n");
        dvmHeapBitmapDelete(&hs->liveBits);
        goto fail;
    }

    gcHeap->markContext.bitmap = &hs->markBits;
    gcHeap->heapSource = hs;

    countAllocation(hs2heap(hs), gcHeap, false);
    countAllocation(hs2heap(hs), hs, false);

    gHs = hs;
    return gcHeap;

fail:
    munmap(base, length);
    return NULL;
}

bool dvmHeapSourceStartupAfterZygote(void)
{
    return gDvm.concurrentMarkSweep ? gcDaemonStartup() : true;
}

/*
 * This is called while in zygote mode, right before we fork() for the
 * first time.  We create a heap for all future zygote process allocations,
 * in an attempt to avoid touching pages in the zygote heap.  (This would
 * probably be unnecessary if we had a compacting GC -- the source of our
 * troubles is small allocations filling in the gaps from larger ones.)
 */
bool
dvmHeapSourceStartupBeforeFork()
{
    HeapSource *hs = gHs; // use a local to avoid the implicit "volatile"

    HS_BOILERPLATE();

    assert(gDvm.zygote);

    if (!gDvm.newZygoteHeapAllocated) {
        /* Create a new heap for post-fork zygote allocations.  We only
         * try once, even if it fails.
         */
        LOGV("Splitting out new zygote heap\n");
        gDvm.newZygoteHeapAllocated = true;
        return addNewHeap(hs, NULL, 0);
    }
    return true;
}

void dvmHeapSourceThreadShutdown(void)
{
    if (gDvm.gcHeap != NULL && gDvm.concurrentMarkSweep) {
        gcDaemonShutdown();
    }
}

/*
 * Tears down the entire GcHeap structure and all of the substructures
 * attached to it.  This call has the side effect of setting the given
 * gcHeap pointer and gHs to NULL.
 */
void
dvmHeapSourceShutdown(GcHeap **gcHeap)
{
    if (*gcHeap != NULL && (*gcHeap)->heapSource != NULL) {
        HeapSource *hs;

        hs = (*gcHeap)->heapSource;

        assert((char *)*gcHeap >= hs->heapBase);
        assert((char *)*gcHeap < hs->heapBase + hs->heapLength);

        dvmHeapBitmapDelete(&hs->liveBits);
        dvmHeapBitmapDelete(&hs->markBits);

        munmap(hs->heapBase, hs->heapLength);
        gHs = NULL;
        *gcHeap = NULL;
    }
}

/*
 * Gets the begining of the allocation for the HeapSource.
 */
void *dvmHeapSourceGetBase(void)
{
    return gHs->heapBase;
}

/*
 * Returns the requested value. If the per-heap stats are requested, fill
 * them as well.
 *
 * Caller must hold the heap lock.
 */
size_t
dvmHeapSourceGetValue(enum HeapSourceValueSpec spec, size_t perHeapStats[],
                      size_t arrayLen)
{
    HeapSource *hs = gHs;
    size_t value = 0;
    size_t total = 0;
    size_t i;

    HS_BOILERPLATE();

    switch (spec) {
    case HS_EXTERNAL_BYTES_ALLOCATED:
        return hs->externalBytesAllocated;
    case HS_EXTERNAL_LIMIT:
        return hs->externalLimit;
    default:
        // look at all heaps.
        ;
    }

    assert(arrayLen >= hs->numHeaps || perHeapStats == NULL);
    for (i = 0; i < hs->numHeaps; i++) {
        Heap *const heap = &hs->heaps[i];

        switch (spec) {
        case HS_FOOTPRINT:
            value = mspace_footprint(heap->msp);
            break;
        case HS_ALLOWED_FOOTPRINT:
            value = mspace_max_allowed_footprint(heap->msp);
            break;
        case HS_BYTES_ALLOCATED:
            value = heap->bytesAllocated;
            break;
        case HS_OBJECTS_ALLOCATED:
            value = heap->objectsAllocated;
            break;
        default:
            // quiet gcc
            break;
        }
        if (perHeapStats) {
            perHeapStats[i] = value;
        }
        total += value;
    }
    return total;
}

static void aliasBitmap(HeapBitmap *dst, HeapBitmap *src,
                        uintptr_t base, uintptr_t max) {
    size_t offset;

    dst->base = base;
    dst->max = max;
    dst->bitsLen = HB_OFFSET_TO_BYTE_INDEX(max - base) + sizeof(dst->bits);
    /* The exclusive limit from bitsLen is greater than the inclusive max. */
    assert(base + HB_MAX_OFFSET(dst) > max);
    /* The exclusive limit is at most one word of bits beyond max. */
    assert((base + HB_MAX_OFFSET(dst)) - max <=
           HB_OBJECT_ALIGNMENT * HB_BITS_PER_WORD);
    dst->allocLen = dst->bitsLen;
    offset = base - src->base;
    assert(HB_OFFSET_TO_MASK(offset) == 1 << 31);
    dst->bits = &src->bits[HB_OFFSET_TO_INDEX(offset)];
}

/*
 * Initializes a vector of object and mark bits to the object and mark
 * bits of each heap.  The bits are aliased to the heapsource
 * object and mark bitmaps.  This routine is used by the sweep code
 * which needs to free each object in the correct heap.
 */
void dvmHeapSourceGetObjectBitmaps(HeapBitmap liveBits[], HeapBitmap markBits[],
                                   size_t numHeaps)
{
    HeapSource *hs = gHs;
    uintptr_t base, max;
    size_t i;

    HS_BOILERPLATE();

    assert(numHeaps == hs->numHeaps);
    for (i = 0; i < hs->numHeaps; ++i) {
        base = (uintptr_t)hs->heaps[i].base;
        /* -1 because limit is exclusive but max is inclusive. */
        max = MIN((uintptr_t)hs->heaps[i].limit - 1, hs->markBits.max);
        aliasBitmap(&liveBits[i], &hs->liveBits, base, max);
        aliasBitmap(&markBits[i], &hs->markBits, base, max);
    }
}

/*
 * Get the bitmap representing all live objects.
 */
HeapBitmap *dvmHeapSourceGetLiveBits(void)
{
    HS_BOILERPLATE();

    return &gHs->liveBits;
}

void dvmHeapSourceSwapBitmaps(void)
{
    HeapBitmap tmp;

    tmp = gHs->liveBits;
    gHs->liveBits = gHs->markBits;
    gHs->markBits = tmp;
}

void dvmHeapSourceZeroMarkBitmap(void)
{
    HS_BOILERPLATE();

    dvmHeapBitmapZero(&gHs->markBits);
}

void dvmMarkImmuneObjects(const char *immuneLimit)
{
    char *dst, *src;
    size_t i, index, length;

    /*
     * Copy the contents of the live bit vector for immune object
     * range into the mark bit vector.
     */
    /* The only values generated by dvmHeapSourceGetImmuneLimit() */
    assert(immuneLimit == gHs->heaps[0].base ||
           immuneLimit == NULL);
    assert(gHs->liveBits.base == gHs->markBits.base);
    assert(gHs->liveBits.bitsLen == gHs->markBits.bitsLen);
    /* heap[0] is never immune */
    assert(gHs->heaps[0].base >= immuneLimit);
    assert(gHs->heaps[0].limit > immuneLimit);

    for (i = 1; i < gHs->numHeaps; ++i) {
        if (gHs->heaps[i].base < immuneLimit) {
            assert(gHs->heaps[i].limit <= immuneLimit);
            /* Compute the number of words to copy in the bitmap. */
            index = HB_OFFSET_TO_INDEX(
                (uintptr_t)gHs->heaps[i].base - gHs->liveBits.base);
            /* Compute the starting offset in the live and mark bits. */
            src = (char *)(gHs->liveBits.bits + index);
            dst = (char *)(gHs->markBits.bits + index);
            /* Compute the number of bytes of the live bitmap to copy. */
            length = HB_OFFSET_TO_BYTE_INDEX(
                gHs->heaps[i].limit - gHs->heaps[i].base);
            /* Do the copy. */
            memcpy(dst, src, length);
            /* Make sure max points to the address of the highest set bit. */
            if (gHs->markBits.max < (uintptr_t)gHs->heaps[i].limit) {
                gHs->markBits.max = (uintptr_t)gHs->heaps[i].limit;
            }
        }
    }
}

/*
 * Allocates <n> bytes of zeroed data.
 */
void *
dvmHeapSourceAlloc(size_t n)
{
    HeapSource *hs = gHs;
    Heap *heap;
    void *ptr;

    HS_BOILERPLATE();
    heap = hs2heap(hs);
    if (heap->bytesAllocated + n > hs->softLimit) {
        /*
         * This allocation would push us over the soft limit; act as
         * if the heap is full.
         */
        LOGV_HEAP("softLimit of %zd.%03zdMB hit for %zd-byte allocation\n",
                  FRACTIONAL_MB(hs->softLimit), n);
        return NULL;
    }
    ptr = mspace_calloc(heap->msp, 1, n);
    if (ptr == NULL) {
        return NULL;
    }
    countAllocation(heap, ptr, true);
    /*
     * Check to see if a concurrent GC should be initiated.
     */
    if (gDvm.gcHeap->gcRunning || !hs->hasGcThread) {
        /*
         * The garbage collector thread is already running or has yet
         * to be started.  Do nothing.
         */
        return ptr;
    }
    if (heap->bytesAllocated > heap->concurrentStartBytes) {
        /*
         * We have exceeded the allocation threshold.  Wake up the
         * garbage collector.
         */
        dvmSignalCond(&gHs->gcThreadCond);
    }
    return ptr;
}

/* Remove any hard limits, try to allocate, and shrink back down.
 * Last resort when trying to allocate an object.
 */
static void *
heapAllocAndGrow(HeapSource *hs, Heap *heap, size_t n)
{
    void *ptr;
    size_t max;

    /* Grow as much as possible, but don't let the real footprint
     * plus external allocations go over the absolute max.
     */
    max = heap->absoluteMaxSize;
    if (max > hs->externalBytesAllocated) {
        max -= hs->externalBytesAllocated;

        mspace_set_max_allowed_footprint(heap->msp, max);
        ptr = dvmHeapSourceAlloc(n);

        /* Shrink back down as small as possible.  Our caller may
         * readjust max_allowed to a more appropriate value.
         */
        mspace_set_max_allowed_footprint(heap->msp,
                mspace_footprint(heap->msp));
    } else {
        ptr = NULL;
    }

    return ptr;
}

/*
 * Allocates <n> bytes of zeroed data, growing as much as possible
 * if necessary.
 */
void *
dvmHeapSourceAllocAndGrow(size_t n)
{
    HeapSource *hs = gHs;
    Heap *heap;
    void *ptr;
    size_t oldIdealSize;

    HS_BOILERPLATE();
    heap = hs2heap(hs);

    ptr = dvmHeapSourceAlloc(n);
    if (ptr != NULL) {
        return ptr;
    }

    oldIdealSize = hs->idealSize;
    if (softLimited(hs)) {
        /* We're soft-limited.  Try removing the soft limit to
         * see if we can allocate without actually growing.
         */
        hs->softLimit = SIZE_MAX;
        ptr = dvmHeapSourceAlloc(n);
        if (ptr != NULL) {
            /* Removing the soft limit worked;  fix things up to
             * reflect the new effective ideal size.
             */
            snapIdealFootprint();
            return ptr;
        }
        // softLimit intentionally left at SIZE_MAX.
    }

    /* We're not soft-limited.  Grow the heap to satisfy the request.
     * If this call fails, no footprints will have changed.
     */
    ptr = heapAllocAndGrow(hs, heap, n);
    if (ptr != NULL) {
        /* The allocation succeeded.  Fix up the ideal size to
         * reflect any footprint modifications that had to happen.
         */
        snapIdealFootprint();
    } else {
        /* We just couldn't do it.  Restore the original ideal size,
         * fixing up softLimit if necessary.
         */
        setIdealFootprint(oldIdealSize);
    }
    return ptr;
}

/*
 * Frees the first numPtrs objects in the ptrs list and returns the
 * amount of reclaimed storage. The list must contain addresses all in
 * the same mspace, and must be in increasing order. This implies that
 * there are no duplicates, and no entries are NULL.
 */
size_t dvmHeapSourceFreeList(size_t numPtrs, void **ptrs)
{
    Heap *heap;
    size_t numBytes;

    HS_BOILERPLATE();

    if (numPtrs == 0) {
        return 0;
    }

    assert(ptrs != NULL);
    assert(*ptrs != NULL);
    heap = ptr2heap(gHs, *ptrs);
    numBytes = 0;
    if (heap != NULL) {
        mspace *msp = heap->msp;
        // Calling mspace_free on shared heaps disrupts sharing too
        // much. For heap[0] -- the 'active heap' -- we call
        // mspace_free, but on the other heaps we only do some
        // accounting.
        if (heap == gHs->heaps) {
            // mspace_merge_objects takes two allocated objects, and
            // if the second immediately follows the first, will merge
            // them, returning a larger object occupying the same
            // memory. This is a local operation, and doesn't require
            // dlmalloc to manipulate any freelists. It's pretty
            // inexpensive compared to free().

            // ptrs is an array of objects all in memory order, and if
            // client code has been allocating lots of short-lived
            // objects, this is likely to contain runs of objects all
            // now garbage, and thus highly amenable to this optimization.

            // Unroll the 0th iteration around the loop below,
            // countFree ptrs[0] and initializing merged.
            assert(ptrs[0] != NULL);
            assert(ptr2heap(gHs, ptrs[0]) == heap);
            countFree(heap, ptrs[0], &numBytes);
            void *merged = ptrs[0];

            size_t i;
            for (i = 1; i < numPtrs; i++) {
                assert(merged != NULL);
                assert(ptrs[i] != NULL);
                assert((intptr_t)merged < (intptr_t)ptrs[i]);
                assert(ptr2heap(gHs, ptrs[i]) == heap);
                countFree(heap, ptrs[i], &numBytes);
                // Try to merge. If it works, merged now includes the
                // memory of ptrs[i]. If it doesn't, free merged, and
                // see if ptrs[i] starts a new run of adjacent
                // objects to merge.
                if (mspace_merge_objects(msp, merged, ptrs[i]) == NULL) {
                    mspace_free(msp, merged);
                    merged = ptrs[i];
                }
            }
            assert(merged != NULL);
            mspace_free(msp, merged);
        } else {
            // This is not an 'active heap'. Only do the accounting.
            size_t i;
            for (i = 0; i < numPtrs; i++) {
                assert(ptrs[i] != NULL);
                assert(ptr2heap(gHs, ptrs[i]) == heap);
                countFree(heap, ptrs[i], &numBytes);
            }
        }
    }
    return numBytes;
}

/*
 * Returns true iff <ptr> is in the heap source.
 */
bool
dvmHeapSourceContainsAddress(const void *ptr)
{
    HS_BOILERPLATE();

    return (dvmHeapBitmapCoversAddress(&gHs->liveBits, ptr));
}

/*
 * Returns true iff <ptr> was allocated from the heap source.
 */
bool
dvmHeapSourceContains(const void *ptr)
{
    HS_BOILERPLATE();

    if (dvmHeapSourceContainsAddress(ptr)) {
        return dvmHeapBitmapIsObjectBitSet(&gHs->liveBits, ptr) != 0;
    }
    return false;
}

/*
 * Returns the value of the requested flag.
 */
bool
dvmHeapSourceGetPtrFlag(const void *ptr, enum HeapSourcePtrFlag flag)
{
    if (ptr == NULL) {
        return false;
    }

    if (flag == HS_CONTAINS) {
        return dvmHeapSourceContains(ptr);
    } else if (flag == HS_ALLOCATED_IN_ZYGOTE) {
        HeapSource *hs = gHs;

        HS_BOILERPLATE();

        if (hs->sawZygote) {
            Heap *heap;

            heap = ptr2heap(hs, ptr);
            if (heap != NULL) {
                /* If the object is not in the active heap, we assume that
                 * it was allocated as part of zygote.
                 */
                return heap != hs->heaps;
            }
        }
        /* The pointer is outside of any known heap, or we are not
         * running in zygote mode.
         */
        return false;
    }

    return false;
}

/*
 * Returns the number of usable bytes in an allocated chunk; the size
 * may be larger than the size passed to dvmHeapSourceAlloc().
 */
size_t
dvmHeapSourceChunkSize(const void *ptr)
{
    Heap *heap;

    HS_BOILERPLATE();

    heap = ptr2heap(gHs, ptr);
    if (heap != NULL) {
        return mspace_usable_size(heap->msp, ptr);
    }
    return 0;
}

/*
 * Returns the number of bytes that the heap source has allocated
 * from the system using sbrk/mmap, etc.
 *
 * Caller must hold the heap lock.
 */
size_t
dvmHeapSourceFootprint()
{
    HS_BOILERPLATE();

//TODO: include size of bitmaps?
    return oldHeapOverhead(gHs, true);
}

/*
 * Return the real bytes used by old heaps and external memory
 * plus the soft usage of the current heap.  When a soft limit
 * is in effect, this is effectively what it's compared against
 * (though, in practice, it only looks at the current heap).
 */
static size_t
getSoftFootprint(bool includeActive)
{
    HeapSource *hs = gHs;
    size_t ret;

    HS_BOILERPLATE();

    ret = oldHeapOverhead(hs, false) + hs->externalBytesAllocated;
    if (includeActive) {
        ret += hs->heaps[0].bytesAllocated;
    }

    return ret;
}

/*
 * Gets the maximum number of bytes that the heap source is allowed
 * to allocate from the system.
 */
size_t
dvmHeapSourceGetIdealFootprint()
{
    HeapSource *hs = gHs;

    HS_BOILERPLATE();

    return hs->idealSize;
}

/*
 * Sets the soft limit, handling any necessary changes to the allowed
 * footprint of the active heap.
 */
static void
setSoftLimit(HeapSource *hs, size_t softLimit)
{
    /* Compare against the actual footprint, rather than the
     * max_allowed, because the heap may not have grown all the
     * way to the allowed size yet.
     */
    mspace msp = hs->heaps[0].msp;
    size_t currentHeapSize = mspace_footprint(msp);
    if (softLimit < currentHeapSize) {
        /* Don't let the heap grow any more, and impose a soft limit.
         */
        mspace_set_max_allowed_footprint(msp, currentHeapSize);
        hs->softLimit = softLimit;
    } else {
        /* Let the heap grow to the requested max, and remove any
         * soft limit, if set.
         */
        mspace_set_max_allowed_footprint(msp, softLimit);
        hs->softLimit = SIZE_MAX;
    }
}

/*
 * Sets the maximum number of bytes that the heap source is allowed
 * to allocate from the system.  Clamps to the appropriate maximum
 * value.
 */
static void
setIdealFootprint(size_t max)
{
    HeapSource *hs = gHs;
#if DEBUG_HEAP_SOURCE
    HeapSource oldHs = *hs;
    mspace msp = hs->heaps[0].msp;
    size_t oldAllowedFootprint =
            mspace_max_allowed_footprint(msp);
#endif

    HS_BOILERPLATE();

    if (max > hs->absoluteMaxSize) {
        LOGI_HEAP("Clamp target GC heap from %zd.%03zdMB to %u.%03uMB\n",
                FRACTIONAL_MB(max),
                FRACTIONAL_MB(hs->absoluteMaxSize));
        max = hs->absoluteMaxSize;
    } else if (max < hs->minimumSize) {
        max = hs->minimumSize;
    }

    /* Convert max into a size that applies to the active heap.
     * Old heaps and external allocations will count against the ideal size.
     */
    size_t overhead = getSoftFootprint(false);
    size_t activeMax;
    if (overhead < max) {
        activeMax = max - overhead;
    } else {
        activeMax = 0;
    }

    setSoftLimit(hs, activeMax);
    hs->idealSize = max;

    HSTRACE("IDEAL %zd->%zd (%d), soft %zd->%zd (%d), allowed %zd->%zd (%d), "
            "ext %zd\n",
            oldHs.idealSize, hs->idealSize, hs->idealSize - oldHs.idealSize,
            oldHs.softLimit, hs->softLimit, hs->softLimit - oldHs.softLimit,
            oldAllowedFootprint, mspace_max_allowed_footprint(msp),
            mspace_max_allowed_footprint(msp) - oldAllowedFootprint,
            hs->externalBytesAllocated);

}

/*
 * Make the ideal footprint equal to the current footprint.
 */
static void
snapIdealFootprint()
{
    HS_BOILERPLATE();

    setIdealFootprint(getSoftFootprint(true));
}

/*
 * Gets the current ideal heap utilization, represented as a number
 * between zero and one.
 */
float dvmGetTargetHeapUtilization()
{
    HeapSource *hs = gHs;

    HS_BOILERPLATE();

    return (float)hs->targetUtilization / (float)HEAP_UTILIZATION_MAX;
}

/*
 * Sets the new ideal heap utilization, represented as a number
 * between zero and one.
 */
void dvmSetTargetHeapUtilization(float newTarget)
{
    HeapSource *hs = gHs;

    HS_BOILERPLATE();

    /* Clamp it to a reasonable range.
     */
    // TODO: This may need some tuning.
    if (newTarget < 0.2) {
        newTarget = 0.2;
    } else if (newTarget > 0.8) {
        newTarget = 0.8;
    }

    hs->targetUtilization =
            (size_t)(newTarget * (float)HEAP_UTILIZATION_MAX);
    LOGV("Set heap target utilization to %zd/%d (%f)\n",
            hs->targetUtilization, HEAP_UTILIZATION_MAX, newTarget);
}

/*
 * If set is true, sets the new minimum heap size to size; always
 * returns the current (or previous) size.  If size is negative,
 * removes the current minimum constraint (if present).
 */
size_t
dvmMinimumHeapSize(size_t size, bool set)
{
    HeapSource *hs = gHs;
    size_t oldMinimumSize;

    /* gHs caches an entry in gDvm.gcHeap;  we need to hold the
     * heap lock if we're going to look at it.  We also need the
     * lock for the call to setIdealFootprint().
     */
    dvmLockHeap();

    HS_BOILERPLATE();

    oldMinimumSize = hs->minimumSize;

    if (set) {
        /* Don't worry about external allocations right now.
         * setIdealFootprint() will take them into account when
         * minimumSize is used, and it's better to hold onto the
         * intended minimumSize than to clamp it arbitrarily based
         * on the current allocations.
         */
        if (size > hs->absoluteMaxSize) {
            size = hs->absoluteMaxSize;
        }
        hs->minimumSize = size;
        if (size > hs->idealSize) {
            /* Force a snap to the minimum value, which we just set
             * and which setIdealFootprint() will take into consideration.
             */
            setIdealFootprint(hs->idealSize);
        }
        /* Otherwise we'll just keep it in mind the next time
         * setIdealFootprint() is called.
         */
    }

    dvmUnlockHeap();

    return oldMinimumSize;
}

/*
 * Given the size of a live set, returns the ideal heap size given
 * the current target utilization and MIN/MAX values.
 *
 * targetUtilization is in the range 1..HEAP_UTILIZATION_MAX.
 */
static size_t
getUtilizationTarget(size_t liveSize, size_t targetUtilization)
{
    size_t targetSize;

    /* Use the current target utilization ratio to determine the
     * ideal heap size based on the size of the live set.
     */
    targetSize = (liveSize / targetUtilization) * HEAP_UTILIZATION_MAX;

    /* Cap the amount of free space, though, so we don't end up
     * with, e.g., 8MB of free space when the live set size hits 8MB.
     */
    if (targetSize > liveSize + HEAP_IDEAL_FREE) {
        targetSize = liveSize + HEAP_IDEAL_FREE;
    } else if (targetSize < liveSize + HEAP_MIN_FREE) {
        targetSize = liveSize + HEAP_MIN_FREE;
    }
    return targetSize;
}

/*
 * Given the current contents of the active heap, increase the allowed
 * heap footprint to match the target utilization ratio.  This
 * should only be called immediately after a full mark/sweep.
 */
void dvmHeapSourceGrowForUtilization()
{
    HeapSource *hs = gHs;
    Heap *heap;
    size_t targetHeapSize;
    size_t currentHeapUsed;
    size_t oldIdealSize;
    size_t newHeapMax;
    size_t overhead;
    size_t freeBytes;

    HS_BOILERPLATE();
    heap = hs2heap(hs);

    /* Use the current target utilization ratio to determine the
     * ideal heap size based on the size of the live set.
     * Note that only the active heap plays any part in this.
     *
     * Avoid letting the old heaps influence the target free size,
     * because they may be full of objects that aren't actually
     * in the working set.  Just look at the allocated size of
     * the current heap.
     */
    currentHeapUsed = heap->bytesAllocated;
#define LET_EXTERNAL_INFLUENCE_UTILIZATION 1
#if LET_EXTERNAL_INFLUENCE_UTILIZATION
    /* This is a hack to deal with the side-effects of moving
     * bitmap data out of the Dalvik heap.  Since the amount
     * of free space after a GC scales with the size of the
     * live set, many apps expected the large free space that
     * appeared along with megabytes' worth of bitmaps.  When
     * the bitmaps were removed, the free size shrank significantly,
     * and apps started GCing constantly.  This makes it so the
     * post-GC free space is the same size it would have been
     * if the bitmaps were still in the Dalvik heap.
     */
    currentHeapUsed += hs->externalBytesAllocated;
#endif
    targetHeapSize =
            getUtilizationTarget(currentHeapUsed, hs->targetUtilization);
#if LET_EXTERNAL_INFLUENCE_UTILIZATION
    currentHeapUsed -= hs->externalBytesAllocated;
    targetHeapSize -= hs->externalBytesAllocated;
#endif

    /* The ideal size includes the old heaps; add overhead so that
     * it can be immediately subtracted again in setIdealFootprint().
     * If the target heap size would exceed the max, setIdealFootprint()
     * will clamp it to a legal value.
     */
    overhead = getSoftFootprint(false);
    oldIdealSize = hs->idealSize;
    setIdealFootprint(targetHeapSize + overhead);

    freeBytes = getAllocLimit(hs);
    if (freeBytes < CONCURRENT_MIN_FREE) {
        /* Not enough free memory to allow a concurrent GC. */
        heap->concurrentStartBytes = SIZE_MAX;
    } else {
        heap->concurrentStartBytes = freeBytes - CONCURRENT_START;
    }
    newHeapMax = mspace_max_allowed_footprint(heap->msp);
    if (softLimited(hs)) {
        LOGD_HEAP("GC old usage %zd.%zd%%; now "
                "%zd.%03zdMB used / %zd.%03zdMB soft max "
                "(%zd.%03zdMB over, "
                "%zd.%03zdMB ext, "
                "%zd.%03zdMB real max)\n",
                FRACTIONAL_PCT(currentHeapUsed, oldIdealSize),
                FRACTIONAL_MB(currentHeapUsed),
                FRACTIONAL_MB(hs->softLimit),
                FRACTIONAL_MB(overhead),
                FRACTIONAL_MB(hs->externalBytesAllocated),
                FRACTIONAL_MB(newHeapMax));
    } else {
        LOGD_HEAP("GC old usage %zd.%zd%%; now "
                "%zd.%03zdMB used / %zd.%03zdMB real max "
                "(%zd.%03zdMB over, "
                "%zd.%03zdMB ext)\n",
                FRACTIONAL_PCT(currentHeapUsed, oldIdealSize),
                FRACTIONAL_MB(currentHeapUsed),
                FRACTIONAL_MB(newHeapMax),
                FRACTIONAL_MB(overhead),
                FRACTIONAL_MB(hs->externalBytesAllocated));
    }
}

/*
 * Return free pages to the system.
 * TODO: move this somewhere else, especially the native heap part.
 */

static void releasePagesInRange(void *start, void *end, void *nbytes)
{
    /* Linux requires that the madvise() start address is page-aligned.
    * We also align the end address.
    */
    start = (void *)ALIGN_UP_TO_PAGE_SIZE(start);
    end = (void *)((size_t)end & ~(SYSTEM_PAGE_SIZE - 1));
    if (start < end) {
        size_t length = (char *)end - (char *)start;
        madvise(start, length, MADV_DONTNEED);
        *(size_t *)nbytes += length;
    }
}

/*
 * Return unused memory to the system if possible.
 */
void
dvmHeapSourceTrim(size_t bytesTrimmed[], size_t arrayLen)
{
    HeapSource *hs = gHs;
    size_t nativeBytes, heapBytes;
    size_t i;

    HS_BOILERPLATE();

    assert(arrayLen >= hs->numHeaps);

    heapBytes = 0;
    for (i = 0; i < hs->numHeaps; i++) {
        Heap *heap = &hs->heaps[i];

        /* Return the wilderness chunk to the system.
         */
        mspace_trim(heap->msp, 0);

        /* Return any whole free pages to the system.
         */
        bytesTrimmed[i] = 0;
        mspace_walk_free_pages(heap->msp, releasePagesInRange,
                               &bytesTrimmed[i]);
        heapBytes += bytesTrimmed[i];
    }

    /* Same for the native heap.
     */
    dlmalloc_trim(0);
    nativeBytes = 0;
    dlmalloc_walk_free_pages(releasePagesInRange, &nativeBytes);

    LOGD_HEAP("madvised %zd (GC) + %zd (native) = %zd total bytes\n",
            heapBytes, nativeBytes, heapBytes + nativeBytes);
}

/*
 * Walks over the heap source and passes every allocated and
 * free chunk to the callback.
 */
void
dvmHeapSourceWalk(void(*callback)(const void *chunkptr, size_t chunklen,
                                      const void *userptr, size_t userlen,
                                      void *arg),
                  void *arg)
{
    HeapSource *hs = gHs;
    size_t i;

    HS_BOILERPLATE();

    /* Walk the heaps from oldest to newest.
     */
//TODO: do this in address order
    for (i = hs->numHeaps; i > 0; --i) {
        mspace_walk_heap(hs->heaps[i-1].msp, callback, arg);
    }
}

/*
 * Gets the number of heaps available in the heap source.
 *
 * Caller must hold the heap lock, because gHs caches a field
 * in gDvm.gcHeap.
 */
size_t
dvmHeapSourceGetNumHeaps()
{
    HeapSource *hs = gHs;

    HS_BOILERPLATE();

    return hs->numHeaps;
}


/*
 * External allocation tracking
 *
 * In some situations, memory outside of the heap is tied to the
 * lifetime of objects in the heap.  Since that memory is kept alive
 * by heap objects, it should provide memory pressure that can influence
 * GCs.
 */

/*
 * Returns true if the requested number of bytes can be allocated from
 * available storage.
 */
static bool externalBytesAvailable(const HeapSource *hs, size_t numBytes)
{
    const Heap *heap;
    size_t currentHeapSize, newHeapSize;

    /* Make sure that this allocation is even possible.
     * Don't let the external size plus the actual heap size
     * go over the absolute max.  This essentially treats
     * external allocations as part of the active heap.
     *
     * Note that this will fail "mysteriously" if there's
     * a small softLimit but a large heap footprint.
     */
    heap = hs2heap(hs);
    currentHeapSize = mspace_max_allowed_footprint(heap->msp);
    newHeapSize = currentHeapSize + hs->externalBytesAllocated + numBytes;
    if (newHeapSize <= heap->absoluteMaxSize) {
        return true;
    }
    HSTRACE("externalBytesAvailable(): "
            "footprint %zu + extAlloc %zu + n %zu >= max %zu (space for %zu)\n",
            currentHeapSize, hs->externalBytesAllocated, numBytes,
            heap->absoluteMaxSize,
            heap->absoluteMaxSize -
                    (currentHeapSize + hs->externalBytesAllocated));
    return false;
}

#define EXTERNAL_TARGET_UTILIZATION 820  // 80%

/*
 * Tries to update the internal count of externally-allocated memory.
 * If there's enough room for that memory, returns true.  If not, returns
 * false and does not update the count.
 *
 * The caller must ensure externalBytesAvailable(hs, n) == true.
 */
static bool
externalAlloc(HeapSource *hs, size_t n, bool grow)
{
    assert(hs->externalLimit >= hs->externalBytesAllocated);

    HSTRACE("externalAlloc(%zd%s)\n", n, grow ? ", grow" : "");
    assert(externalBytesAvailable(hs, n));  // The caller must ensure this.

    /* External allocations have their own "free space" that they
     * can allocate from without causing a GC.
     */
    if (hs->externalBytesAllocated + n <= hs->externalLimit) {
        hs->externalBytesAllocated += n;
#if PROFILE_EXTERNAL_ALLOCATIONS
        if (gDvm.allocProf.enabled) {
            Thread* self = dvmThreadSelf();
            gDvm.allocProf.externalAllocCount++;
            gDvm.allocProf.externalAllocSize += n;
            if (self != NULL) {
                self->allocProf.externalAllocCount++;
                self->allocProf.externalAllocSize += n;
            }
        }
#endif
        return true;
    }
    if (!grow) {
        return false;
    }

    /* GROW */
    hs->externalBytesAllocated += n;
    hs->externalLimit = getUtilizationTarget(
            hs->externalBytesAllocated, EXTERNAL_TARGET_UTILIZATION);
    HSTRACE("EXTERNAL grow limit to %zd\n", hs->externalLimit);
    return true;
}

static void
gcForExternalAlloc(bool collectSoftReferences)
{
    if (gDvm.allocProf.enabled) {
        Thread* self = dvmThreadSelf();
        gDvm.allocProf.gcCount++;
        if (self != NULL) {
            self->allocProf.gcCount++;
        }
    }
    dvmCollectGarbageInternal(collectSoftReferences, GC_EXTERNAL_ALLOC);
}

/*
 * Returns true if there is enough unused storage to perform an
 * external allocation of the specified size.  If there insufficient
 * free storage we try to releasing memory from external allocations
 * and trimming the heap.
 */
static bool externalAllocPossible(const HeapSource *hs, size_t n)
{
    size_t bytesTrimmed[HEAP_SOURCE_MAX_HEAP_COUNT];

    /*
     * If there is sufficient space return immediately.
     */
    if (externalBytesAvailable(hs, n)) {
        return true;
    }
    /*
     * There is insufficient space.  Wait for the garbage collector to
     * become inactive before proceeding.
     */
    while (gDvm.gcHeap->gcRunning) {
        dvmWaitForConcurrentGcToComplete();
    }
    /*
     * The heap may have grown or become trimmed while we were
     * waiting.
     */
    if (externalBytesAvailable(hs, n)) {
        return true;
    }
    /*
     * Try a garbage collection that clears soft references.  This may
     * make trimming more effective.
     */
    gcForExternalAlloc(true);
    if (externalBytesAvailable(hs, n)) {
        return true;
    }
    /*
     * Try trimming the mspace to reclaim unused pages.
     */
    dvmHeapSourceTrim(bytesTrimmed, NELEM(bytesTrimmed));
    snapIdealFootprint();
    if (externalBytesAvailable(hs, n)) {
        return true;
    }
    /*
     * Nothing worked, return an error.
     */
    return false;
}

/*
 * Updates the internal count of externally-allocated memory.  If there's
 * enough room for that memory, returns true.  If not, returns false and
 * does not update the count.
 *
 * May cause a GC as a side-effect.
 */
bool
dvmTrackExternalAllocation(size_t n)
{
    HeapSource *hs = gHs;
    bool ret = false;

    /* gHs caches an entry in gDvm.gcHeap;  we need to hold the
     * heap lock if we're going to look at it.
     */
    dvmLockHeap();

    HS_BOILERPLATE();
    assert(hs->externalLimit >= hs->externalBytesAllocated);

    /*
     * The externalAlloc calls require the externalAllocPossible
     * invariant to be established.
     */
    if (!externalAllocPossible(hs, n)) {
        LOGE_HEAP("%zd-byte external allocation "
                  "too large for this process.", n);
        goto out;
    }

    /* Try "allocating" using the existing "free space".
     */
    HSTRACE("EXTERNAL alloc %zu (%zu < %zu)\n",
            n, hs->externalBytesAllocated, hs->externalLimit);
    if (externalAlloc(hs, n, false)) {
        ret = true;
        goto out;
    }
    /*
     * Wait until garbage collector is quiescent before proceeding.
     */
    while (gDvm.gcHeap->gcRunning) {
        dvmWaitForConcurrentGcToComplete();
    }
    /*
     * Re-establish the invariant if it was lost while we were
     * waiting.
     */
    if (!externalAllocPossible(hs, n)) {
        LOGE_HEAP("%zd-byte external allocation "
                  "too large for this process.", n);
        goto out;
    }
    /* The "allocation" failed.  Free up some space by doing
     * a full garbage collection.  This may grow the heap source
     * if the live set is sufficiently large.
     */
    HSTRACE("EXTERNAL alloc %zd: GC 1\n", n);
    gcForExternalAlloc(false);  // don't collect SoftReferences
    if (externalAlloc(hs, n, false)) {
        ret = true;
        goto out;
    }

    /* Even that didn't work;  this is an exceptional state.
     * Try harder, growing the heap source if necessary.
     */
    HSTRACE("EXTERNAL alloc %zd: frag\n", n);
    ret = externalAlloc(hs, n, true);
    if (ret) {
        goto out;
    }

    /* We couldn't even grow enough to satisfy the request.
     * Try one last GC, collecting SoftReferences this time.
     */
    HSTRACE("EXTERNAL alloc %zd: GC 2\n", n);
    gcForExternalAlloc(true);  // collect SoftReferences
    ret = externalAlloc(hs, n, true);
    if (!ret) {
        LOGE_HEAP("Out of external memory on a %zu-byte allocation.\n", n);
    }

#if PROFILE_EXTERNAL_ALLOCATIONS
    if (gDvm.allocProf.enabled) {
        Thread* self = dvmThreadSelf();
        gDvm.allocProf.failedExternalAllocCount++;
        gDvm.allocProf.failedExternalAllocSize += n;
        if (self != NULL) {
            self->allocProf.failedExternalAllocCount++;
            self->allocProf.failedExternalAllocSize += n;
        }
    }
#endif

out:
    dvmUnlockHeap();

    return ret;
}

/*
 * Reduces the internal count of externally-allocated memory.
 */
void
dvmTrackExternalFree(size_t n)
{
    HeapSource *hs = gHs;
    size_t newExternalLimit;
    size_t oldExternalBytesAllocated;

    HSTRACE("EXTERNAL free %zu (%zu < %zu)\n",
            n, hs->externalBytesAllocated, hs->externalLimit);

    /* gHs caches an entry in gDvm.gcHeap;  we need to hold the
     * heap lock if we're going to look at it.
     */
    dvmLockHeap();

    HS_BOILERPLATE();
    assert(hs->externalLimit >= hs->externalBytesAllocated);

    oldExternalBytesAllocated = hs->externalBytesAllocated;
    if (n <= hs->externalBytesAllocated) {
        hs->externalBytesAllocated -= n;
    } else {
        n = hs->externalBytesAllocated;
        hs->externalBytesAllocated = 0;
    }

#if PROFILE_EXTERNAL_ALLOCATIONS
    if (gDvm.allocProf.enabled) {
        Thread* self = dvmThreadSelf();
        gDvm.allocProf.externalFreeCount++;
        gDvm.allocProf.externalFreeSize += n;
        if (self != NULL) {
            self->allocProf.externalFreeCount++;
            self->allocProf.externalFreeSize += n;
        }
    }
#endif

    /* Shrink as quickly as we can.
     */
    newExternalLimit = getUtilizationTarget(
            hs->externalBytesAllocated, EXTERNAL_TARGET_UTILIZATION);
    if (newExternalLimit < oldExternalBytesAllocated) {
        /* Make sure that the remaining free space is at least
         * big enough to allocate something of the size that was
         * just freed.  This makes it more likely that
         *     externalFree(N); externalAlloc(N);
         * will work without causing a GC.
         */
        HSTRACE("EXTERNAL free preserved %zu extra free bytes\n",
                oldExternalBytesAllocated - newExternalLimit);
        newExternalLimit = oldExternalBytesAllocated;
    }
    if (newExternalLimit < hs->externalLimit) {
        hs->externalLimit = newExternalLimit;
    }

    dvmUnlockHeap();
}

/*
 * Returns the number of externally-allocated bytes being tracked by
 * dvmTrackExternalAllocation/Free().
 */
size_t
dvmGetExternalBytesAllocated()
{
    const HeapSource *hs = gHs;
    size_t ret;

    /* gHs caches an entry in gDvm.gcHeap;  we need to hold the
     * heap lock if we're going to look at it.  We also need the
     * lock for the call to setIdealFootprint().
     */
    dvmLockHeap();
    HS_BOILERPLATE();
    ret = hs->externalBytesAllocated;
    dvmUnlockHeap();

    return ret;
}

void *dvmHeapSourceGetImmuneLimit(GcMode mode)
{
    if (mode == GC_PARTIAL) {
        return hs2heap(gHs)->base;
    } else {
        return NULL;
    }
}
