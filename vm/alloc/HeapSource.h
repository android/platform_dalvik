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
#ifndef _DALVIK_HEAP_SOURCE
#define _DALVIK_HEAP_SOURCE

#include "alloc/HeapInternal.h" // for GcHeap

/* dlmalloc uses one size_t per allocated chunk.
 */
#define HEAP_SOURCE_CHUNK_OVERHEAD         (1 * sizeof (size_t))
#define HEAP_SOURCE_WORST_CHUNK_OVERHEAD   (32 * sizeof (size_t))

/* The largest number of separate heaps we can handle.
 */
#define HEAP_SOURCE_MAX_HEAP_COUNT 3

/*
 * Initializes the heap source; must be called before any other
 * dvmHeapSource*() functions.
 */
GcHeap *dvmHeapSourceStartup(size_t startSize, size_t absoluteMaxSize);

/*
 * If the HeapSource was created while in zygote mode, this
 * will create a new heap for post-zygote allocations.
 * Having a separate heap should maximize the number of pages
 * that a given app_process shares with the zygote process.
 */
bool dvmHeapSourceStartupAfterZygote(void);

/*
 * If the HeapSource was created while in zygote mode, this
 * will create an additional zygote heap before the first fork().
 * Having a separate heap should reduce the number of shared
 * pages subsequently touched by the zygote process.
 */
bool dvmHeapSourceStartupBeforeFork(void);

/*
 * Tears down the heap source and frees any resources associated with it.
 */
void dvmHeapSourceShutdown(GcHeap *gcHeap);

/*
 * Writes shallow copies of the currently-used bitmaps into outBitmaps,
 * returning the number of bitmaps written.  Returns <0 if the array
 * was not long enough.
 */
ssize_t dvmHeapSourceGetObjectBitmaps(HeapBitmap outBitmaps[],
        size_t maxBitmaps);

/*
 * Replaces the object location HeapBitmaps with the elements of
 * <objectBitmaps>.  The elements of <objectBitmaps> are overwritten
 * with shallow copies of the old bitmaps.
 *
 * Returns false if the number of bitmaps doesn't match the number
 * of heaps.
 */
bool dvmHeapSourceReplaceObjectBitmaps(HeapBitmap objectBitmaps[],
        size_t nBitmaps);

/*
 * Returns the requested value. If the per-heap stats are requested, fill
 * them as well.
 */
enum HeapSourceValueSpec {
    HS_FOOTPRINT,
    HS_ALLOWED_FOOTPRINT,
    HS_BYTES_ALLOCATED,
    HS_OBJECTS_ALLOCATED,
    HS_EXTERNAL_BYTES_ALLOCATED,
    HS_EXTERNAL_LIMIT
};
size_t dvmHeapSourceGetValue(enum HeapSourceValueSpec spec, 
                             size_t perHeapStats[], size_t arrayLen);

/*
 * Allocates <n> bytes of zeroed data.
 */
void *dvmHeapSourceAlloc(size_t n);

/*
 * Allocates <n> bytes of zeroed data, growing up to absoluteMaxSize
 * if necessary.
 */
void *dvmHeapSourceAllocAndGrow(size_t n);

/*
 * Frees the memory pointed to by <ptr>, which may be NULL.
 */
void dvmHeapSourceFree(void *ptr);

/*
 * Frees the first numPtrs objects in the ptrs list. The list must
 * contain addresses all in the same mspace, and must be in increasing
 * order. This implies that there are no duplicates, and no entries
 * are NULL.
 */
void dvmHeapSourceFreeList(size_t numPtrs, void **ptrs);

/*
 * Returns true iff <ptr> was allocated from the heap source.
 */
bool dvmHeapSourceContains(const void *ptr);

/*
 * Returns the value of the requested flag.
 */
enum HeapSourcePtrFlag {
    HS_CONTAINS,    // identical to dvmHeapSourceContains()
    HS_ALLOCATED_IN_ZYGOTE
};
bool dvmHeapSourceGetPtrFlag(const void *ptr, enum HeapSourcePtrFlag flag);

/*
 * Returns the number of usable bytes in an allocated chunk; the size
 * may be larger than the size passed to dvmHeapSourceAlloc().
 */
size_t dvmHeapSourceChunkSize(const void *ptr);

/*
 * Returns the number of bytes that the heap source has allocated
 * from the system using sbrk/mmap, etc.
 */
size_t dvmHeapSourceFootprint(void);

/*
 * Gets the maximum number of bytes that the heap source is allowed
 * to allocate from the system.
 */
size_t dvmHeapSourceGetIdealFootprint(void);

/*
 * Given the current contents of the heap, increase the allowed
 * heap footprint to match the target utilization ratio.  This
 * should only be called immediately after a full mark/sweep.
 */
void dvmHeapSourceGrowForUtilization(void);

/*
 * Return unused memory to the system if possible.  If <bytesTrimmed>
 * is non-NULL, the number of bytes returned to the system is written to it.
 */
void dvmHeapSourceTrim(size_t bytesTrimmed[], size_t arrayLen);

/*
 * Walks over the heap source and passes every allocated and
 * free chunk to the callback.
 */
void dvmHeapSourceWalk(void(*callback)(const void *chunkptr, size_t chunklen,
                                      const void *userptr, size_t userlen,
                                      void *arg),
                       void *arg);
/*
 * Gets the number of heaps available in the heap source.
 */
size_t dvmHeapSourceGetNumHeaps(void);

#endif  // _DALVIK_HEAP_SOURCE
