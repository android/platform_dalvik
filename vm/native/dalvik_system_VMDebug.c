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
 * dalvik.system.VMDebug
 */
#include "Dalvik.h"
#include "native/InternalNativePriv.h"


#ifdef WITH_PROFILER
/* These must match the values in dalvik.system.VMDebug.
 */
enum {
    KIND_ALLOCATED_OBJECTS = 1<<0,
    KIND_ALLOCATED_BYTES   = 1<<1,
    KIND_FREED_OBJECTS     = 1<<2,
    KIND_FREED_BYTES       = 1<<3,
    KIND_GC_INVOCATIONS    = 1<<4,
#if PROFILE_EXTERNAL_ALLOCATIONS
    KIND_EXT_ALLOCATED_OBJECTS = 1<<12,
    KIND_EXT_ALLOCATED_BYTES   = 1<<13,
    KIND_EXT_FREED_OBJECTS     = 1<<14,
    KIND_EXT_FREED_BYTES       = 1<<15,
#endif // PROFILE_EXTERNAL_ALLOCATIONS

    KIND_GLOBAL_ALLOCATED_OBJECTS   = KIND_ALLOCATED_OBJECTS,
    KIND_GLOBAL_ALLOCATED_BYTES     = KIND_ALLOCATED_BYTES,
    KIND_GLOBAL_FREED_OBJECTS       = KIND_FREED_OBJECTS,
    KIND_GLOBAL_FREED_BYTES         = KIND_FREED_BYTES,
    KIND_GLOBAL_GC_INVOCATIONS      = KIND_GC_INVOCATIONS,
#if PROFILE_EXTERNAL_ALLOCATIONS
    KIND_GLOBAL_EXT_ALLOCATED_OBJECTS = KIND_EXT_ALLOCATED_OBJECTS,
    KIND_GLOBAL_EXT_ALLOCATED_BYTES = KIND_EXT_ALLOCATED_BYTES,
    KIND_GLOBAL_EXT_FREED_OBJECTS   = KIND_EXT_FREED_OBJECTS,
    KIND_GLOBAL_EXT_FREED_BYTES     = KIND_EXT_FREED_BYTES,
#endif // PROFILE_EXTERNAL_ALLOCATIONS

    KIND_THREAD_ALLOCATED_OBJECTS   = KIND_ALLOCATED_OBJECTS << 16,
    KIND_THREAD_ALLOCATED_BYTES     = KIND_ALLOCATED_BYTES << 16,
    KIND_THREAD_FREED_OBJECTS       = KIND_FREED_OBJECTS << 16,
    KIND_THREAD_FREED_BYTES         = KIND_FREED_BYTES << 16,
#if PROFILE_EXTERNAL_ALLOCATIONS
    KIND_THREAD_EXT_ALLOCATED_OBJECTS = KIND_EXT_ALLOCATED_OBJECTS << 16,
    KIND_THREAD_EXT_ALLOCATED_BYTES = KIND_EXT_ALLOCATED_BYTES << 16,
    KIND_THREAD_EXT_FREED_OBJECTS   = KIND_EXT_FREED_OBJECTS << 16,
    KIND_THREAD_EXT_FREED_BYTES     = KIND_EXT_FREED_BYTES << 16,
#endif // PROFILE_EXTERNAL_ALLOCATIONS
    KIND_THREAD_GC_INVOCATIONS      = KIND_GC_INVOCATIONS << 16,

    // TODO: failedAllocCount, failedAllocSize
};

#define KIND_ALL_COUNTS 0xffffffff

/*
 * Zero out the specified fields.
 */
static void clearAllocProfStateFields(AllocProfState *allocProf,
    unsigned int kinds)
{
    if (kinds & KIND_ALLOCATED_OBJECTS) {
        allocProf->allocCount = 0;
    }
    if (kinds & KIND_ALLOCATED_BYTES) {
        allocProf->allocSize = 0;
    }
    if (kinds & KIND_FREED_OBJECTS) {
        allocProf->freeCount = 0;
    }
    if (kinds & KIND_FREED_BYTES) {
        allocProf->freeSize = 0;
    }
    if (kinds & KIND_GC_INVOCATIONS) {
        allocProf->gcCount = 0;
    }
#if PROFILE_EXTERNAL_ALLOCATIONS
    if (kinds & KIND_EXT_ALLOCATED_OBJECTS) {
        allocProf->externalAllocCount = 0;
    }
    if (kinds & KIND_EXT_ALLOCATED_BYTES) {
        allocProf->externalAllocSize = 0;
    }
    if (kinds & KIND_EXT_FREED_OBJECTS) {
        allocProf->externalFreeCount = 0;
    }
    if (kinds & KIND_EXT_FREED_BYTES) {
        allocProf->externalFreeSize = 0;
    }
#endif // PROFILE_EXTERNAL_ALLOCATIONS
}
#endif

/*
 * static void startAllocCounting()
 *
 * Reset the counters and enable counting.
 *
 * TODO: this currently only resets the per-thread counters for the current
 * thread.  If we actually start using the per-thread counters we'll
 * probably want to fix this.
 */
static void Dalvik_dalvik_system_VMDebug_startAllocCounting(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

#ifdef WITH_PROFILER
    clearAllocProfStateFields(&gDvm.allocProf, KIND_ALL_COUNTS);
    clearAllocProfStateFields(&dvmThreadSelf()->allocProf, KIND_ALL_COUNTS);
    dvmStartAllocCounting();
#endif
    RETURN_VOID();
}

/*
 * public static void stopAllocCounting()
 */
static void Dalvik_dalvik_system_VMDebug_stopAllocCounting(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

#ifdef WITH_PROFILER
    dvmStopAllocCounting();
#endif
    RETURN_VOID();
}

/*
 * private static int getAllocCount(int kind)
 */
static void Dalvik_dalvik_system_VMDebug_getAllocCount(const u4* args,
    JValue* pResult)
{
#ifdef WITH_PROFILER
    AllocProfState *allocProf;
    unsigned int kind = args[0];
    if (kind < (1<<16)) {
        allocProf = &gDvm.allocProf;
    } else {
        allocProf = &dvmThreadSelf()->allocProf;
        kind >>= 16;
    }
    switch (kind) {
    case KIND_ALLOCATED_OBJECTS:
        pResult->i = allocProf->allocCount;
        break;
    case KIND_ALLOCATED_BYTES:
        pResult->i = allocProf->allocSize;
        break;
    case KIND_FREED_OBJECTS:
        pResult->i = allocProf->freeCount;
        break;
    case KIND_FREED_BYTES:
        pResult->i = allocProf->freeSize;
        break;
    case KIND_GC_INVOCATIONS:
        pResult->i = allocProf->gcCount;
        break;
#if PROFILE_EXTERNAL_ALLOCATIONS
    case KIND_EXT_ALLOCATED_OBJECTS:
        pResult->i = allocProf->externalAllocCount;
        break;
    case KIND_EXT_ALLOCATED_BYTES:
        pResult->i = allocProf->externalAllocSize;
        break;
    case KIND_EXT_FREED_OBJECTS:
        pResult->i = allocProf->externalFreeCount;
        break;
    case KIND_EXT_FREED_BYTES:
        pResult->i = allocProf->externalFreeSize;
        break;
#endif // PROFILE_EXTERNAL_ALLOCATIONS
    default:
        assert(false);
        pResult->i = -1;
    }
#else
    RETURN_INT(-1);
#endif
}

/*
 * public static void resetAllocCount(int kinds)
 */
static void Dalvik_dalvik_system_VMDebug_resetAllocCount(const u4* args,
    JValue* pResult)
{
#ifdef WITH_PROFILER
    unsigned int kinds = args[0];
    clearAllocProfStateFields(&gDvm.allocProf, kinds & 0xffff);
    clearAllocProfStateFields(&dvmThreadSelf()->allocProf, kinds >> 16);
#endif
    RETURN_VOID();
}

/*
 * static void startMethodTracing(String traceFileName,
 *     int bufferSize, int flags)
 *
 * Start method trace profiling.
 */
static void Dalvik_dalvik_system_VMDebug_startMethodTracing(const u4* args,
    JValue* pResult)
{
#ifdef WITH_PROFILER
    StringObject* traceFileStr = (StringObject*) args[0];
    int bufferSize = args[1];
    int flags = args[2];
    char* traceFileName;

    if (bufferSize == 0) {
        // Default to 8MB per the documentation.
        bufferSize = 8 * 1024 * 1024;
    }

    if (traceFileStr == NULL || bufferSize < 1024) {
        dvmThrowException("Ljava/lang/InvalidArgument;", NULL);
        RETURN_VOID();
    }

    traceFileName = dvmCreateCstrFromString(traceFileStr);

    dvmMethodTraceStart(traceFileName, bufferSize, flags);
    free(traceFileName);
#else
    // throw exception?
#endif
    RETURN_VOID();
}

/*
 * static void stopMethodTracing()
 *
 * Stop method tracing.
 */
static void Dalvik_dalvik_system_VMDebug_stopMethodTracing(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

#ifdef WITH_PROFILER
    dvmMethodTraceStop();
#else
    // throw exception?
#endif
    RETURN_VOID();
}

/*
 * static void startEmulatorTracing()
 *
 * Start sending method trace info to the emulator.
 */
static void Dalvik_dalvik_system_VMDebug_startEmulatorTracing(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

#ifdef WITH_PROFILER
    dvmEmulatorTraceStart();
#else
    // throw exception?
#endif
    RETURN_VOID();
}

/*
 * static void stopEmulatorTracing()
 *
 * Start sending method trace info to the emulator.
 */
static void Dalvik_dalvik_system_VMDebug_stopEmulatorTracing(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

#ifdef WITH_PROFILER
    dvmEmulatorTraceStop();
#else
    // throw exception?
#endif
    RETURN_VOID();
}

/*
 * static int setAllocationLimit(int limit)
 *
 * Set the current allocation limit in this thread.  Return the previous
 * value.
 */
static void Dalvik_dalvik_system_VMDebug_setAllocationLimit(const u4* args,
    JValue* pResult)
{
#if defined(WITH_ALLOC_LIMITS)
    gDvm.checkAllocLimits = true;

    Thread* self = dvmThreadSelf();
    int newLimit = args[0];
    int oldLimit = self->allocLimit;

    if (newLimit < -1) {
        LOGE("WARNING: bad limit request (%d)\n", newLimit);
        newLimit = -1;
    }
    self->allocLimit = newLimit;
    RETURN_INT(oldLimit);
#else
    UNUSED_PARAMETER(args);
    RETURN_INT(-1);
#endif
}

/*
 * static int setGlobalAllocationLimit(int limit)
 *
 * Set the allocation limit for this process.  Returns the previous value.
 */
static void Dalvik_dalvik_system_VMDebug_setGlobalAllocationLimit(const u4* args,
    JValue* pResult)
{
#if defined(WITH_ALLOC_LIMITS)
    gDvm.checkAllocLimits = true;

    int newLimit = args[0];
    int oldLimit = gDvm.allocationLimit;

    if (newLimit < -1 || newLimit > 0) {
        LOGE("WARNING: bad limit request (%d)\n", newLimit);
        newLimit = -1;
    }
    // TODO: should use an atomic swap here
    gDvm.allocationLimit = newLimit;
    RETURN_INT(oldLimit);
#else
    UNUSED_PARAMETER(args);
    RETURN_INT(-1);
#endif
}

/*
 * static boolean isDebuggerConnected()
 *
 * Returns "true" if a debugger is attached.
 */
static void Dalvik_dalvik_system_VMDebug_isDebuggerConnected(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

    RETURN_BOOLEAN(dvmDbgIsDebuggerConnected());
}

/*
 * static boolean isDebuggingEnabled()
 *
 * Returns "true" if debugging is enabled.
 */
static void Dalvik_dalvik_system_VMDebug_isDebuggingEnabled(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

    RETURN_BOOLEAN(gDvm.jdwpConfigured);
}

/*
 * static long lastDebuggerActivity()
 *
 * Returns the time, in msec, since we last had an interaction with the
 * debugger (send or receive).
 */
static void Dalvik_dalvik_system_VMDebug_lastDebuggerActivity(const u4* args,
    JValue* pResult)
{
    UNUSED_PARAMETER(args);

    RETURN_LONG(dvmDbgLastDebuggerActivity());
}

/*
 * static void startInstructionCounting()
 */
static void Dalvik_dalvik_system_VMDebug_startInstructionCounting(const u4* args,
    JValue* pResult)
{
#if defined(WITH_PROFILER)
    dvmStartInstructionCounting();
#else
    dvmThrowException("Ljava/lang/UnsupportedOperationException;", NULL);
#endif
    RETURN_VOID();
}

/*
 * static void stopInstructionCounting()
 */
static void Dalvik_dalvik_system_VMDebug_stopInstructionCounting(const u4* args,
    JValue* pResult)
{
#if defined(WITH_PROFILER)
    dvmStopInstructionCounting();
#else
    dvmThrowException("Ljava/lang/UnsupportedOperationException;", NULL);
#endif
    RETURN_VOID();
}

/*
 * static boolean getInstructionCount(int[] counts)
 *
 * Grab a copy of the global instruction count array.
 *
 * Since the instruction counts aren't synchronized, we use sched_yield
 * to improve our chances of finishing without contention.  (Only makes
 * sense on a uniprocessor.)
 */
static void Dalvik_dalvik_system_VMDebug_getInstructionCount(const u4* args,
    JValue* pResult)
{
#if defined(WITH_PROFILER)
    ArrayObject* countArray = (ArrayObject*) args[0];
    int* storage;

    storage = (int*) countArray->contents;
    sched_yield();
    memcpy(storage, gDvm.executedInstrCounts,
        kNumDalvikInstructions * sizeof(int));
#else
    dvmThrowException("Ljava/lang/UnsupportedOperationException;", NULL);
#endif
    RETURN_VOID();
}

/*
 * static boolean resetInstructionCount()
 *
 * Reset the instruction count array.
 */
static void Dalvik_dalvik_system_VMDebug_resetInstructionCount(const u4* args,
    JValue* pResult)
{
#if defined(WITH_PROFILER)
    sched_yield();
    memset(gDvm.executedInstrCounts, 0, kNumDalvikInstructions * sizeof(int));
#else
    dvmThrowException("Ljava/lang/UnsupportedOperationException;", NULL);
#endif
    RETURN_VOID();
}

/*
 * static void printLoadedClasses(int flags)
 *
 * Dump the list of loaded classes.
 */
static void Dalvik_dalvik_system_VMDebug_printLoadedClasses(const u4* args,
    JValue* pResult)
{
    int flags = args[0];

    dvmDumpAllClasses(flags);

    RETURN_VOID();
}

/*
 * static int getLoadedClassCount()
 *
 * Return the number of loaded classes
 */
static void Dalvik_dalvik_system_VMDebug_getLoadedClassCount(const u4* args,
    JValue* pResult)
{
    int count;

    UNUSED_PARAMETER(args);

    count = dvmGetNumLoadedClasses();

    RETURN_INT(count);
}

/*
 * Returns the thread-specific CPU-time clock value for the current thread,
 * or -1 if the feature isn't supported.
 */
static void Dalvik_dalvik_system_VMDebug_threadCpuTimeNanos(const u4* args,
    JValue* pResult)
{
    jlong result;

#ifdef HAVE_POSIX_CLOCKS
    struct timespec now;
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &now);
    result = (jlong) (now.tv_sec*1000000000LL + now.tv_nsec);
#else
    result = (jlong) -1;
#endif

    RETURN_LONG(result);
}

/*
 * static void dumpHprofData(String fileName)
 *
 * Cause "hprof" data to be dumped.  We can throw an IOException if an
 * error occurs during file handling.
 */
static void Dalvik_dalvik_system_VMDebug_dumpHprofData(const u4* args,
    JValue* pResult)
{
#ifdef WITH_HPROF
    StringObject* fileNameStr = (StringObject*) args[0];
    char* fileName;

    if (fileNameStr == NULL) {
        dvmThrowException("Ljava/lang/NullPointerException;", NULL);
        RETURN_VOID();
    }

    fileName = dvmCreateCstrFromString(fileNameStr);
    if (fileName == NULL) {
        /* unexpected -- malloc failure? */
        dvmThrowException("Ljava/lang/RuntimeException;", "malloc failure?");
        RETURN_VOID();
    }

    hprofDumpHeap(fileName);
    free(fileName);
#else
    dvmThrowException("Ljava/lang/UnsupportedOperationException;", NULL);
#endif

    RETURN_VOID();
}

const DalvikNativeMethod dvm_dalvik_system_VMDebug[] = {
    { "getAllocCount",              "(I)I",
        Dalvik_dalvik_system_VMDebug_getAllocCount },
    { "resetAllocCount",            "(I)V",
        Dalvik_dalvik_system_VMDebug_resetAllocCount },
    //{ "print",              "(Ljava/lang/String;)V",
    //    Dalvik_dalvik_system_VMDebug_print },
    { "startAllocCounting",         "()V",
        Dalvik_dalvik_system_VMDebug_startAllocCounting },
    { "stopAllocCounting",          "()V",
        Dalvik_dalvik_system_VMDebug_stopAllocCounting },
    { "startMethodTracing",         "(Ljava/lang/String;II)V",
        Dalvik_dalvik_system_VMDebug_startMethodTracing },
    { "stopMethodTracing",          "()V",
        Dalvik_dalvik_system_VMDebug_stopMethodTracing },
    { "startEmulatorTracing",       "()V",
        Dalvik_dalvik_system_VMDebug_startEmulatorTracing },
    { "stopEmulatorTracing",        "()V",
        Dalvik_dalvik_system_VMDebug_stopEmulatorTracing },
    { "setAllocationLimit",         "(I)I",
        Dalvik_dalvik_system_VMDebug_setAllocationLimit },
    { "setGlobalAllocationLimit",   "(I)I",
        Dalvik_dalvik_system_VMDebug_setGlobalAllocationLimit },
    { "startInstructionCounting",   "()V",
        Dalvik_dalvik_system_VMDebug_startInstructionCounting },
    { "stopInstructionCounting",    "()V",
        Dalvik_dalvik_system_VMDebug_stopInstructionCounting },
    { "resetInstructionCount",      "()V",
        Dalvik_dalvik_system_VMDebug_resetInstructionCount },
    { "getInstructionCount",        "([I)V",
        Dalvik_dalvik_system_VMDebug_getInstructionCount },
    { "isDebuggerConnected",        "()Z",
        Dalvik_dalvik_system_VMDebug_isDebuggerConnected },
    { "isDebuggingEnabled",         "()Z",
        Dalvik_dalvik_system_VMDebug_isDebuggingEnabled },
    { "lastDebuggerActivity",       "()J",
        Dalvik_dalvik_system_VMDebug_lastDebuggerActivity },
    { "printLoadedClasses",         "(I)V",
        Dalvik_dalvik_system_VMDebug_printLoadedClasses },
    { "getLoadedClassCount",        "()I",
        Dalvik_dalvik_system_VMDebug_getLoadedClassCount },
    { "threadCpuTimeNanos",         "()J",
        Dalvik_dalvik_system_VMDebug_threadCpuTimeNanos },
    { "dumpHprofData",              "(Ljava/lang/String;)V",
        Dalvik_dalvik_system_VMDebug_dumpHprofData },
    { NULL, NULL, NULL },
};

