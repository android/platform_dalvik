/*
 * Copyright (C) 2010 The Android Open Source Project
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

#include "Dalvik.h"

#include <cutils/atomic.h>

#if defined(__arm__)
#include <machine/cpu-features.h>
#endif

/*****************************************************************************/

#if defined(HAVE_MACOSX_IPC)
#define NEED_MAC_QUASI_ATOMICS 1

#elif defined(__i386__) || defined(__x86_64__)
#define NEED_PTHREADS_QUASI_ATOMICS 1

#elif defined(__mips__)
#define NEED_PTHREADS_QUASI_ATOMICS 1

#elif defined(__aarch64__)

int64_t dvmQuasiAtomicSwap64(int64_t value, volatile int64_t* addr) {
    return __atomic_exchange_n(addr, value, __ATOMIC_RELAXED);
}

int64_t dvmQuasiAtomicSwap64Sync(int64_t value, volatile int64_t* addr) {
    return __atomic_exchange_n(addr, value, __ATOMIC_SEQ_CST);
}

int dvmQuasiAtomicAcquireCas64(int64_t oldvalue, int64_t newvalue,
        volatile int64_t* addr) {
    return __atomic_compare_exchange_n(addr, &oldvalue, newvalue, false,
            __ATOMIC_ACQUIRE, __ATOMIC_ACQUIRE) == 0;
}

int dvmQuasiAtomicReleaseCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr) {
    return __atomic_compare_exchange_n(addr, &oldvalue, newvalue, false,
            __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST) == 0;
}

int64_t dvmQuasiAtomicRead64(volatile const int64_t* addr) {
    return __atomic_load_n(addr, __ATOMIC_RELAXED);
}

int dvmQuasiAtomicCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr) {
    return __atomic_compare_exchange_n(addr, &oldvalue, newvalue, false,
            __ATOMIC_RELAXED, __ATOMIC_RELAXED) == 0;
}

int dvmQuasiAtomicCas128(__int128 oldvalue, __int128 newvalue,
    volatile __int128* addr) {
    int status;
    __int128 origvalue;

    __asm__ __volatile__ (
            "    mov %w1,#1\n"
            "    ldxp %0,%H0, [%3]\n"
            "    cmp %0,%4\n"
            "    b.ne .LdvmQuasiAtomicCas128asmExit\n"
            "    cmp %H0,%H4\n"
            "    b.ne .LdvmQuasiAtomicCas128asmExit\n"
            "    stxp %w1, %5, %H5, [%3]\n"
            "   .LdvmQuasiAtomicCas128asmExit:\n"
            : "=&r" (origvalue), "=&r" ( status), "+m" (*addr)
            : "r" (addr), "r" (oldvalue), "r" (newvalue)
            : "cc", "memory");

    return status;
}


#elif defined(__arm__)

// TODO: Clang can not process our inline assembly at the moment.
#if defined(__ARM_HAVE_LDREXD) && !defined(__clang__)
#define NEED_ARM_LDREXD_QUASI_ATOMICS 1
#else
#define NEED_PTHREADS_QUASI_ATOMICS 1
#endif

#else
#error "Unsupported atomic operations for this platform"
#endif

/*****************************************************************************/

#if NEED_ARM_LDREXD_QUASI_ATOMICS

static inline int64_t dvmQuasiAtomicSwap64Body(int64_t newvalue,
                                               volatile int64_t* addr)
{
    int64_t prev;
    int status;
    do {
        __asm__ __volatile__ ("@ dvmQuasiAtomicSwap64\n"
            "ldrexd     %0, %H0, [%3]\n"
            "strexd     %1, %4, %H4, [%3]"
            : "=&r" (prev), "=&r" (status), "+m"(*addr)
            : "r" (addr), "r" (newvalue)
            : "cc");
    } while (__builtin_expect(status != 0, 0));
    return prev;
}

int64_t dvmQuasiAtomicSwap64(int64_t newvalue, volatile int64_t* addr)
{
    return dvmQuasiAtomicSwap64Body(newvalue, addr);
}

int64_t dvmQuasiAtomicSwap64Sync(int64_t newvalue, volatile int64_t* addr)
{
    int64_t prev;
    ANDROID_MEMBAR_STORE();
    prev = dvmQuasiAtomicSwap64Body(newvalue, addr);
    ANDROID_MEMBAR_FULL();
    return prev;
}

int dvmQuasiAtomicCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr)
{
    int64_t prev;
    int status;
    do {
        __asm__ __volatile__ ("@ dvmQuasiAtomicCas64\n"
            "ldrexd     %0, %H0, [%3]\n"
            "mov        %1, #0\n"
            "teq        %0, %4\n"
            "teqeq      %H0, %H4\n"
            "strexdeq   %1, %5, %H5, [%3]"
            : "=&r" (prev), "=&r" (status), "+m"(*addr)
            : "r" (addr), "Ir" (oldvalue), "r" (newvalue)
            : "cc", "memory");
    } while (__builtin_expect(status != 0, 0));
    return prev != oldvalue;
}

int dvmQuasiAtomicReleaseCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr) {

    ANDROID_MEMBAR_STORE();
    return dvmQuasiAtomicCas64(oldvalue, newvalue, addr);
}

int64_t dvmQuasiAtomicRead64(volatile const int64_t* addr)
{
    int64_t value;
    __asm__ __volatile__ (
        "ldrexd     %0, %H0, [%1]"
        : "=&r" (value)
        : "r" (addr));
    return value;
}
#endif

/*****************************************************************************/

#if NEED_MAC_QUASI_ATOMICS

#include <libkern/OSAtomic.h>

int dvmQuasiAtomicCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr)
{
    return OSAtomicCompareAndSwap64Barrier(oldvalue, newvalue,
            (int64_t*)addr) == 0;
}


static inline int64_t dvmQuasiAtomicSwap64Body(int64_t value,
                                               volatile int64_t* addr)
{
    int64_t oldValue;
    do {
        oldValue = *addr;
    } while (dvmQuasiAtomicCas64(oldValue, value, addr));
    return oldValue;
}

int64_t dvmQuasiAtomicSwap64(int64_t value, volatile int64_t* addr)
{
    return dvmQuasiAtomicSwap64Body(value, addr);
}

int64_t dvmQuasiAtomicSwap64Sync(int64_t value, volatile int64_t* addr)
{
    int64_t oldValue;
    ANDROID_MEMBAR_STORE();
    oldValue = dvmQuasiAtomicSwap64Body(value, addr);
    /* TUNING: barriers can be avoided on some architectures */
    ANDROID_MEMBAR_FULL();
    return oldValue;
}

int64_t dvmQuasiAtomicRead64(volatile const int64_t* addr)
{
    return OSAtomicAdd64Barrier(0, addr);
}
#endif

/*****************************************************************************/

#if NEED_PTHREADS_QUASI_ATOMICS

// In the absence of a better implementation, we implement the 64-bit atomic
// operations through mutex locking.

// another twist is that we use a small array of mutexes to dispatch
// the contention locks from different memory addresses

#include <pthread.h>

static const size_t kSwapLockCount = 32;
static pthread_mutex_t* gSwapLocks[kSwapLockCount];

void dvmQuasiAtomicsStartup() {
    for (size_t i = 0; i < kSwapLockCount; ++i) {
        pthread_mutex_t* m = new pthread_mutex_t;
        dvmInitMutex(m);
        gSwapLocks[i] = m;
    }
}

void dvmQuasiAtomicsShutdown() {
    for (size_t i = 0; i < kSwapLockCount; ++i) {
        pthread_mutex_t* m = gSwapLocks[i];
        gSwapLocks[i] = NULL;
        if (m != NULL) {
            dvmDestroyMutex(m);
        }
        delete m;
    }
}

static inline pthread_mutex_t* GetSwapLock(const volatile int64_t* addr) {
    return gSwapLocks[(static_cast<unsigned>((uintptr_t)(addr)) >> 3U) % kSwapLockCount];
}

int64_t dvmQuasiAtomicSwap64(int64_t value, volatile int64_t* addr)
{
    int64_t oldValue;
    pthread_mutex_t* lock = GetSwapLock(addr);

    pthread_mutex_lock(lock);

    oldValue = *addr;
    *addr    = value;

    pthread_mutex_unlock(lock);
    return oldValue;
}

/* Same as dvmQuasiAtomicSwap64 - mutex handles barrier */
int64_t dvmQuasiAtomicSwap64Sync(int64_t value, volatile int64_t* addr)
{
    return dvmQuasiAtomicSwap64(value, addr);
}

int dvmQuasiAtomicCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr)
{
    int result;
    pthread_mutex_t* lock = GetSwapLock(addr);

    pthread_mutex_lock(lock);

    if (*addr == oldvalue) {
        *addr  = newvalue;
        result = 0;
    } else {
        result = 1;
    }
    pthread_mutex_unlock(lock);
    return result;
}


/* Same as dvmQuasiAtomicCas64 - mutex handles barrier */
int dvmQuasiAtomicAcquireCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr)
{
    return dvmQuasiAtomicCas64(oldvalue, newvalue, addr);
}

/* Same as dvmQuasiAtomicCas64 - mutex handles barrier */
int dvmQuasiAtomicReleaseCas64(int64_t oldvalue, int64_t newvalue,
    volatile int64_t* addr)
{
    return dvmQuasiAtomicCas64(oldvalue, newvalue, addr);
}


int64_t dvmQuasiAtomicRead64(volatile const int64_t* addr)
{
    int64_t result;
    pthread_mutex_t* lock = GetSwapLock(addr);

    pthread_mutex_lock(lock);
    result = *addr;
    pthread_mutex_unlock(lock);
    return result;
}

#if defined(_LP64)
int dvmQuasiAtomicCas128(__int128 oldvalue, __int128 newvalue,
    volatile __int128* addr) {
    int result;
    pthread_mutex_t* lock = GetSwapLock((const volatile int64_t*)addr);

    pthread_mutex_lock(lock);

    if (*addr == oldvalue) {
        *addr  = newvalue;
        result = 0;
    } else {
        result = 1;
    }
    pthread_mutex_unlock(lock);
    return result;
}
#endif

#else

// The other implementations don't need any special setup.
void dvmQuasiAtomicsStartup() {}
void dvmQuasiAtomicsShutdown() {}

#endif /*NEED_PTHREADS_QUASI_ATOMICS*/
