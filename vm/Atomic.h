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
 * Atomic operations
 */
#ifndef DALVIK_ATOMIC_H_
#define DALVIK_ATOMIC_H_

#include <cutils/atomic.h>          /* use common Android atomic ops */
#include <cutils/atomic-inline.h>   /* and some uncommon ones */

void dvmQuasiAtomicsStartup();
void dvmQuasiAtomicsShutdown();

/*
 * NOTE: Two "quasiatomic" operations on the exact same memory address
 * are guaranteed to operate atomically with respect to each other,
 * but no guarantees are made about quasiatomic operations mixed with
 * non-quasiatomic operations on the same address, nor about
 * quasiatomic operations that are performed on partially-overlapping
 * memory.
 *
 * Only the "Sync" versions of these provide a memory barrier.
 */

/*
 * Swap the 64-bit value at "addr" with "value".  Returns the previous
 * value.
 */
extern "C" int64_t dvmQuasiAtomicSwap64(int64_t value, volatile int64_t* addr);

/*
 * Swap the 64-bit value at "addr" with "value".  Returns the previous
 * value.  Provides memory barriers.
 */
extern "C" int64_t dvmQuasiAtomicSwap64Sync(int64_t value,
                                            volatile int64_t* addr);

/*
 * Read the 64-bit value at "addr".
 */
extern "C" int64_t dvmQuasiAtomicRead64(volatile const int64_t* addr);

/*
 * If the value at "addr" is equal to "oldvalue", replace it with "newvalue"
 * and return 0.  Otherwise, don't swap, and return nonzero.
 */
extern "C" int dvmQuasiAtomicCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr);

/*
 * If the value at "addr" is equal to "oldvalue", replace it with "newvalue"
 * and return 0.  Otherwise, don't swap, and return nonzero.
 * Provides memory barrier for acquire semantics.
 */
extern "C" int dvmQuasiAtomicAcquireCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr);

/*
 * If the value at "addr" is equal to "oldvalue", replace it with "newvalue"
 * and return 0.  Otherwise, don't swap, and return nonzero.
 * Provides memory barrier for release semantics.
 */
extern "C" int dvmQuasiAtomicReleaseCas64(int64_t oldvalue, int64_t newvalue,
     volatile int64_t* addr);

/*
 * Load a 64bit value at ptr, and return, with acquire semantics.
 * 64bit replacement for android_atomic_acquire_load.
 */
extern "C" inline int64_t dvmQuasiAtomicAcquireLoad64(volatile const int64_t *ptr) {
    int64_t value = *ptr;
    android_memory_barrier();
    return value;
}

/*
 * Store a 64bit value at ptr, with release semantics.
 * 64bit replacement for android_atomic_acquire_store.
 */
extern "C" inline void dvmQuasiAtomicReleaseStore64(int64_t value, volatile int64_t *ptr) {
    android_memory_barrier();
    *ptr = value;
}

#if defined(_LP64)
// Routines using 128 bit values.
// Only valid on 64bit platforms.

/*
 * Compare and swap a 128 bit value.
 */
int dvmQuasiAtomicCas128(__int128 oldvalue, __int128 newvalue,
    volatile __int128* addr);
#endif

#endif  // DALVIK_ATOMIC_H_
