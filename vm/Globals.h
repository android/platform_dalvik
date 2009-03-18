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
 * Variables with library scope.
 *
 * Prefer this over scattered static and global variables -- it's easier to
 * view the state in a debugger, it makes clean shutdown simpler, we can
 * trivially dump the state into a crash log, and it dodges most naming
 * collisions that will arise when we are embedded in a larger program.
 *
 * If we want multiple VMs per process, this can get stuffed into TLS (or
 * accessed through a Thread field).  May need to pass it around for some
 * of the early initialization functions.
 */
#ifndef _DALVIK_GLOBALS
#define _DALVIK_GLOBALS

#include <stdarg.h>
#include <pthread.h>

#define MAX_BREAKPOINTS 20      /* used for a debugger optimization */

// fwd
typedef struct GcHeap GcHeap;   /* heap internal structure */

/*
 * One of these for each -ea/-da/-esa/-dsa on the command line.
 */
typedef struct AssertionControl {
    char*   pkgOrClass;         /* package/class string, or NULL for esa/dsa */
    int     pkgOrClassLen;      /* string length, for quick compare */
    bool    enable;             /* enable or disable */
    bool    isPackage;          /* string ended with "..."? */
} AssertionControl;

/*
 * Execution mode, e.g. interpreter vs. JIT.
 */
typedef enum ExecutionMode {
    kExecutionModeUnknown = 0,
    kExecutionModeInterpPortable,
    kExecutionModeInterpFast,
} ExecutionMode;

/*
 * All fields are initialized to zero.
 *
 * Storage allocated here must be freed by a subsystem shutdown function or
 * from within freeGlobals().
 */
struct DvmGlobals {
    /*
     * Some options from the command line or environment.
     */
    char*       bootClassPathStr;
    char*       classPathStr;

    unsigned int    heapSizeStart;
    unsigned int    heapSizeMax;
    unsigned int    stackSize;

    bool        verboseGc;
    bool        verboseJni;
    bool        verboseClass;

    bool        jdwpAllowed;        // debugging allowed for this process?
    bool        jdwpConfigured;     // has debugging info been provided?
    int         jdwpTransport;
    bool        jdwpServer;
    char*       jdwpHost;
    int         jdwpPort;
    bool        jdwpSuspend;

    int         (*vfprintfHook)(FILE*, const char*, va_list);
    void        (*exitHook)(int);
    void        (*abortHook)(void);

    int         jniGrefLimit;       // 0 means no limit
    bool        reduceSignals;
    bool        noQuitHandler;
    bool        verifyDexChecksum;
    char*       stackTraceFile;     // for SIGQUIT-inspired output

    bool        logStdio;

    DexOptimizerMode    dexOptMode;
    DexClassVerifyMode  classVerifyMode;
    bool        generateRegisterMaps;

    int         assertionCtrlCount;
    AssertionControl*   assertionCtrl;

    ExecutionMode   executionMode;

    /*
     * VM init management.
     */
    bool        initializing;
    int         initExceptionCount;
    bool        optimizing;

    /*
     * java.lang.System properties set from the command line.
     */
    int         numProps;
    int         maxProps;
    char**      propList;

    /*
     * Where the VM goes to find system classes.
     */
    ClassPathEntry* bootClassPath;
    /* used by the DEX optimizer to load classes from an unfinished DEX */
    DvmDex*     bootClassPathOptExtra;
    bool        optimizingBootstrapClass;

    /*
     * Loaded classes, hashed by class name.  Each entry is a ClassObject*,
     * allocated in GC space.
     */
    HashTable*  loadedClasses;

    /*
     * Value for the next class serial number to be assigned.  This is
     * incremented as we load classes.  Failed loads and races may result
     * in some numbers being skipped, and the serial number is not
     * guaranteed to start at 1, so the current value should not be used
     * as a count of loaded classes.
     */
    volatile int classSerialNumber;

    /*
     * Interned strings.
     */
    HashTable*  internedStrings;

    /*
     * Quick lookups for popular classes used internally.
     */
    ClassObject* unlinkedJavaLangClass;    // see unlinkedJavaLangClassObject
    ClassObject* classJavaLangClass;
    ClassObject* classJavaLangClassArray;
    ClassObject* classJavaLangError;
    ClassObject* classJavaLangObject;
    ClassObject* classJavaLangObjectArray;
    ClassObject* classJavaLangRuntimeException;
    ClassObject* classJavaLangString;
    ClassObject* classJavaLangThread;
    ClassObject* classJavaLangVMThread;
    ClassObject* classJavaLangThreadGroup;
    ClassObject* classJavaLangThrowable;
    ClassObject* classJavaLangStackTraceElement;
    ClassObject* classJavaLangStackTraceElementArray;
    ClassObject* classJavaLangAnnotationAnnotationArray;
    ClassObject* classJavaLangAnnotationAnnotationArrayArray;
    ClassObject* classJavaLangReflectAccessibleObject;
    ClassObject* classJavaLangReflectConstructor;
    ClassObject* classJavaLangReflectConstructorArray;
    ClassObject* classJavaLangReflectField;
    ClassObject* classJavaLangReflectFieldArray;
    ClassObject* classJavaLangReflectMethod;
    ClassObject* classJavaLangReflectMethodArray;
    ClassObject* classJavaLangReflectProxy;
    ClassObject* classJavaLangExceptionInInitializerError;
    ClassObject* classJavaLangRefReference;
    ClassObject* classJavaSecurityAccessController;
    ClassObject* classOrgApacheHarmonyLangAnnotationAnnotationFactory;
    ClassObject* classOrgApacheHarmonyLangAnnotationAnnotationMember;
    ClassObject* classOrgApacheHarmonyLangAnnotationAnnotationMemberArray;

    /* synthetic classes for arrays of primitives */
    ClassObject* classArrayBoolean;
    ClassObject* classArrayChar;
    ClassObject* classArrayFloat;
    ClassObject* classArrayDouble;
    ClassObject* classArrayByte;
    ClassObject* classArrayShort;
    ClassObject* classArrayInt;
    ClassObject* classArrayLong;

    /* method offsets - Object */
    int         voffJavaLangObject_equals;
    int         voffJavaLangObject_hashCode;
    int         voffJavaLangObject_toString;
    int         voffJavaLangObject_finalize;

    /* field offsets - Class */
    int         offJavaLangClass_pd;

    /* field offsets - String */
    volatile int javaLangStringReady;   /* 0=not init, 1=ready, -1=initing */
    int         offJavaLangString_value;
    int         offJavaLangString_count;
    int         offJavaLangString_offset;
    int         offJavaLangString_hashCode;

    /* field offsets - Thread */
    int         offJavaLangThread_vmThread;
    int         offJavaLangThread_group;
    int         offJavaLangThread_daemon;
    int         offJavaLangThread_name;
    int         offJavaLangThread_priority;

    /* method offsets - Thread */
    int         voffJavaLangThread_run;

    /* field offsets - VMThread */
    int         offJavaLangVMThread_thread;
    int         offJavaLangVMThread_vmData;

    /* method offsets - ThreadGroup */
    int         voffJavaLangThreadGroup_removeThread;

    /* field offsets - Throwable */
    int         offJavaLangThrowable_stackState;
    int         offJavaLangThrowable_message;
    int         offJavaLangThrowable_cause;

    /* field offsets - java.lang.reflect.* */
    int         offJavaLangReflectAccessibleObject_flag;
    int         offJavaLangReflectConstructor_slot;
    int         offJavaLangReflectConstructor_declClass;
    int         offJavaLangReflectField_slot;
    int         offJavaLangReflectField_declClass;
    int         offJavaLangReflectMethod_slot;
    int         offJavaLangReflectMethod_declClass;

    /* field offsets - java.lang.ref.Reference */
    int         offJavaLangRefReference_referent;
    int         offJavaLangRefReference_queue;
    int         offJavaLangRefReference_queueNext;
    int         offJavaLangRefReference_vmData;

#if FANCY_REFERENCE_SUBCLASS
    /* method offsets - java.lang.ref.Reference */
    int         voffJavaLangRefReference_clear;
    int         voffJavaLangRefReference_enqueue;
#else
    /* method pointers - java.lang.ref.Reference */
    Method*     methJavaLangRefReference_enqueueInternal;
#endif

    /* field offsets - java.nio.Buffer and java.nio.DirectByteBufferImpl */
    //int         offJavaNioBuffer_capacity;
    //int         offJavaNioDirectByteBufferImpl_pointer;

    /* method pointers - java.security.AccessController */
    volatile bool javaSecurityAccessControllerReady;
    Method*     methJavaSecurityAccessController_doPrivileged[4];

    /* constructor method pointers; no vtable involved, so use Method* */
    Method*     methJavaLangStackTraceElement_init;
    Method*     methJavaLangExceptionInInitializerError_init;
    Method*     methJavaLangReflectConstructor_init;
    Method*     methJavaLangReflectField_init;
    Method*     methJavaLangReflectMethod_init;
    Method*     methOrgApacheHarmonyLangAnnotationAnnotationMember_init;

    /* static method pointers - android.lang.annotation.* */
    Method*
        methOrgApacheHarmonyLangAnnotationAnnotationFactory_createAnnotation;

    /* direct method pointers - java.lang.reflect.Proxy */
    Method*     methJavaLangReflectProxy_constructorPrototype;

    /* field offsets - java.lang.reflect.Proxy */
    int         offJavaLangReflectProxy_h;

    /* fake native entry point method */
    Method*     methFakeNativeEntry;

    /*
     * VM-synthesized primitive classes, for arrays.
     */
    ClassObject* volatile primitiveClass[PRIM_MAX];

    /*
     * A placeholder ClassObject used during ClassObject
     * construction.
     */
    ClassObject  unlinkedJavaLangClassObject;

    /*
     * Thread list.  This always has at least one element in it (main),
     * and main is always the first entry.
     *
     * The threadListLock is used for several things, including the thread
     * start condition variable.  Generally speaking, you must hold the
     * threadListLock when:
     *  - adding/removing items from the list
     *  - waiting on or signaling threadStartCond
     *  - examining the Thread struct for another thread (this is to avoid
     *    one thread freeing the Thread struct while another thread is
     *    perusing it)
     */
    Thread*     threadList;
    pthread_mutex_t threadListLock;

    pthread_cond_t threadStartCond;

    /*
     * The thread code grabs this before suspending all threads.  There
     * are four things that can cause a "suspend all":
     *  (1) the GC is starting;
     *  (2) the debugger has sent a "suspend all" request;
     *  (3) a thread has hit a breakpoint or exception that the debugger
     *      has marked as a "suspend all" event;
     *  (4) the SignalCatcher caught a signal that requires suspension.
     *
     * Because we use "safe point" self-suspension, it is never safe to
     * do a blocking "lock" call on this mutex -- if it has been acquired,
     * somebody is probably trying to put you to sleep.  The leading '_' is
     * intended as a reminder that this lock is special.
     *
     * This lock is also held while attaching an externally-created thread
     * through JNI.  That way we can correctly set the initial suspend state.
     */
    pthread_mutex_t _threadSuspendLock;

    /*
     * Guards Thread->suspendCount for all threads, and provides the lock
     * for the condition variable that all suspended threads sleep on
     * (threadSuspendCountCond).
     *
     * This has to be separate from threadListLock because of the way
     * threads put themselves to sleep.
     */
    pthread_mutex_t threadSuspendCountLock;

    /*
     * Suspended threads sleep on this.  They should sleep on the condition
     * variable until their "suspend count" is zero.
     *
     * Paired with "threadSuspendCountLock".
     */
    pthread_cond_t  threadSuspendCountCond;

    /*
     * MUTEX ORDERING: when locking multiple mutexes, always grab them in
     * this order to avoid deadlock:
     *
     *  (1) _threadSuspendLock      (use lockThreadSuspend())
     *  (2) threadListLock          (use dvmLockThreadList())
     *  (3) threadSuspendCountLock  (use lockThreadSuspendCount())
     */


    /*
     * Thread ID bitmap.  We want threads to have small integer IDs so
     * we can use them in "thin locks".
     */
    BitVector*  threadIdMap;

    /*
     * Manage exit conditions.  The VM exits when all non-daemon threads
     * have exited.  If the main thread returns early, we need to sleep
     * on a condition variable.
     */
    int         nonDaemonThreadCount;   /* must hold threadListLock to access */
    //pthread_mutex_t vmExitLock;
    pthread_cond_t  vmExitCond;

    /*
     * The set of DEX files loaded by custom class loaders.
     */
    HashTable*  userDexFiles;

    /*
     * JNI global reference table.
     */
    ReferenceTable  jniGlobalRefTable;
    pthread_mutex_t jniGlobalRefLock;
    int         jniGlobalRefHiMark;
    int         jniGlobalRefLoMark;

    /*
     * Native shared library table.
     */
    HashTable*  nativeLibs;

    /*
     * GC heap lock.  Functions like gcMalloc() acquire this before making
     * any changes to the heap.  It is held throughout garbage collection.
     */
    pthread_mutex_t gcHeapLock;

    /* Opaque pointer representing the heap. */
    GcHeap*     gcHeap;

    /*
     * Pre-allocated object for out-of-memory errors.
     */
    Object*     outOfMemoryObj;

    /* pre-allocated general failure exception */
    Object*     internalErrorObj;

    /* Monitor list, so we can free them */
    /*volatile*/ Monitor* monitorList;

    /* Monitor for Thread.sleep() implementation */
    Monitor*    threadSleepMon;

    /* set when we create a second heap inside the zygote */
    bool        newZygoteHeapAllocated;

    /*
     * TLS keys.
     */
    pthread_key_t pthreadKeySelf;       /* Thread*, for dvmThreadSelf */

    /*
     * JNI allows you to have multiple VMs, but we limit ourselves to 1,
     * so "vmList" is really just a pointer to the one and only VM.
     */
    JavaVM*     vmList;

    /*
     * Cache results of "A instanceof B".
     */
    AtomicCache* instanceofCache;

    /* instruction width table, used for optimization and verification */
    InstructionWidth*   instrWidth;
    /* instruction flags table, used for verification */
    InstructionFlags*   instrFlags;
    /* instruction format table, used for verification */
    InstructionFormat*  instrFormat;

    /*
     * Bootstrap class loader linear allocator.
     */
    LinearAllocHdr* pBootLoaderAlloc;


    /*
     * Heap worker thread.
     */
    bool            heapWorkerInitialized;
    bool            heapWorkerReady;
    bool            haltHeapWorker;
    pthread_t       heapWorkerHandle;
    pthread_mutex_t heapWorkerLock;
    pthread_cond_t  heapWorkerCond;
    pthread_cond_t  heapWorkerIdleCond;
    pthread_mutex_t heapWorkerListLock;

    /*
     * Compute some stats on loaded classes.
     */
    int             numLoadedClasses;
    int             numDeclaredMethods;
    int             numDeclaredInstFields;
    int             numDeclaredStaticFields;

    /*
     * JDWP debugger support.
     */
    bool        debuggerConnected;      /* debugger or DDMS is connected */
    bool        debuggerActive;         /* debugger is making requests */
    JdwpState*  jdwpState;

    /*
     * Registry of objects known to the debugger.
     */
    HashTable*  dbgRegistry;

    /*
     * Breakpoint optimization table.  This is global and NOT explicitly
     * synchronized, but all operations that modify the table are made
     * from relatively-synchronized functions.  False-positives are
     * possible, false-negatives (i.e. missing a breakpoint) should not be.
     */
    const u2*   debugBreakAddr[MAX_BREAKPOINTS];

    /*
     * Single-step control struct.  We currently only allow one thread to
     * be single-stepping at a time, which is all that really makes sense,
     * but it's possible we may need to expand this to be per-thread.
     */
    StepControl stepControl;

    /*
     * DDM features embedded in the VM.
     */
    bool        ddmThreadNotification;

    /*
     * Zygote (partially-started process) support
     */
    bool        zygote;

    /*
     * Used for tracking allocations that we report to DDMS.  When the feature
     * is enabled (through a DDMS request) the "allocRecords" pointer becomes
     * non-NULL.
     */
    pthread_mutex_t allocTrackerLock;
    AllocRecord*    allocRecords;
    int             allocRecordHead;        /* most-recently-added entry */
    int             allocRecordCount;       /* #of valid entries */

#ifdef WITH_ALLOC_LIMITS
    /* set on first use of an alloc limit, never cleared */
    bool        checkAllocLimits;
    /* allocation limit, for setGlobalAllocationLimit() regression testing */
    int         allocationLimit;
#endif

#ifdef WITH_DEADLOCK_PREDICTION
    /* global lock on history tree accesses */
    pthread_mutex_t deadlockHistoryLock;

    enum { kDPOff=0, kDPWarn, kDPErr, kDPAbort } deadlockPredictMode;
#endif

#ifdef WITH_PROFILER
    /*
     * When a profiler is enabled, this is incremented.  Distinct profilers
     * include "dmtrace" method tracing, emulator method tracing, and
     * possibly instruction counting.
     *
     * The purpose of this is to have a single value that the interpreter
     * can check to see if any profiling activity is enabled.
     */
    volatile int activeProfilers;

    /*
     * State for method-trace profiling.
     */
    MethodTraceState methodTrace;

    /*
     * State for emulator tracing.
     */
    void*       emulatorTracePage;
    int         emulatorTraceEnableCount;

    /*
     * Global state for memory allocation profiling.
     */
    AllocProfState allocProf;

    /*
     * Pointers to the original methods for things that have been inlined.
     * This makes it easy for us to output method entry/exit records for
     * the method calls we're not actually making.
     */
    Method**    inlinedMethods;

    /*
     * Dalvik instruction counts (256 entries).
     */
    int*        executedInstrCounts;
    bool        instructionCountEnableCount;
#endif

    /*
     * Signal catcher thread (for SIGQUIT).
     */
    pthread_t   signalCatcherHandle;
    bool        haltSignalCatcher;

    /*
     * Stdout/stderr conversion thread.
     */
    bool            haltStdioConverter;
    bool            stdioConverterReady;
    pthread_t       stdioConverterHandle;
    pthread_mutex_t stdioConverterLock;
    pthread_cond_t  stdioConverterCond;

    /*
     * pid of the system_server process. We track it so that when system server
     * crashes the Zygote process will be killed and restarted.
     */
    pid_t systemServerPid;

//#define COUNT_PRECISE_METHODS
#ifdef COUNT_PRECISE_METHODS
    PointerSet* preciseMethods;
#endif
};

extern struct DvmGlobals gDvm;

#endif /*_DALVIK_GLOBALS*/
