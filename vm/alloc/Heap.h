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
 * Internal heap functions
 */
#ifndef _DALVIK_ALLOC_HEAP
#define _DALVIK_ALLOC_HEAP

/*
 * Initialize the GC heap.
 *
 * Returns true if successful, false otherwise.
 */
bool dvmHeapStartup(void);

/*
 * Initialization that needs to wait until after leaving zygote mode.
 * This needs to be called before the first allocation or GC that
 * happens after forking.
 */
bool dvmHeapStartupAfterZygote(void);

/*
 * Tear down the GC heap.
 *
 * Frees all memory allocated via dvmMalloc() as
 * a side-effect.
 */
void dvmHeapShutdown(void);

#if 0       // needs to be in Alloc.h so debug code can find it.
/*
 * Returns a number of bytes greater than or
 * equal to the size of the named object in the heap.
 *
 * Specifically, it returns the size of the heap
 * chunk which contains the object.
 */
size_t dvmObjectSizeInHeap(const Object *obj);
#endif

enum GcReason {
    /* Not enough space for an "ordinary" Object to be allocated. */
    GC_FOR_MALLOC = 0,
    /* Explicit GC via Runtime.gc(), VMRuntim.gc(), or SIGUSR1. */
    GC_USER = 1,
    /* GC to try to reduce heap footprint to allow more non-GC'ed memory. */
    GC_EXTERNAL_ALLOC = 2,
    /* GC to dump heap contents to a file. */
    GC_HPROF = 3
};

/*
 * Run the garbage collector without doing any locking.
 */
void dvmCollectGarbageInternal(bool collectSoftReferences,
                               enum GcReason reason);

#endif  // _DALVIK_ALLOC_HEAP
