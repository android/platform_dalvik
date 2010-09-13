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
 * Dalvik implementation of JNI interfaces.
 */
#include "Dalvik.h"
#include "JniInternal.h"

#include <stdlib.h>
#include <stdarg.h>
#include <limits.h>

/*
Native methods and interaction with the GC

All JNI methods must start by changing their thread status to
THREAD_RUNNING, and finish by changing it back to THREAD_NATIVE before
returning to native code.  The switch to "running" triggers a thread
suspension check.

With a rudimentary GC we should be able to skip the status change for
simple functions, e.g.  IsSameObject, GetJavaVM, GetStringLength, maybe
even access to fields with primitive types.  Our options are more limited
with a compacting GC, so we should replace JNI_ENTER with JNI_ENTER_NCGC
or somesuch on the "lite" functions if we want to try this optimization.

For performance reasons we do as little error-checking as possible here.
For example, we don't check to make sure the correct type of Object is
passed in when setting a field, and we don't prevent you from storing
new values in a "final" field.  Such things are best handled in the
"check" version.  For actions that are common, dangerous, and must be
checked at runtime, such as array bounds checks, we do the tests here.


General notes on local/global reference tracking

JNI provides explicit control over natively-held references that the GC
needs to know about.  These can be local, in which case they're released
when the native method returns into the VM, or global, which are held
until explicitly released.  (There are also weak-global references,
which have the lifespan and visibility of global references, but the
object they refer to may be collected.)

The references can be created with explicit JNI NewLocalRef / NewGlobalRef
calls.  The former is very unusual, the latter is reasonably common
(e.g. for caching references to class objects).

Local references are most often created as a side-effect of JNI functions.
For example, the AllocObject/NewObject functions must create local
references to the objects returned, because nothing else in the GC root
set has a reference to the new objects.

The most common mode of operation is for a method to create zero or
more local references and return.  Explicit "local delete" operations
are expected to be exceedingly rare, except when walking through an
object array, and the Push/PopLocalFrame calls are expected to be used
infrequently.  For efficient operation, we want to add new local refs
with a simple store/increment operation; to avoid infinite growth in
pathological situations, we need to reclaim the space used by deleted
entries.

If we just want to maintain a list for the GC root set, we can use an
expanding append-only array that compacts when objects are deleted.
In typical situations, e.g. running through an array of objects, we will
be deleting one of the most recently added entries, so we can minimize
the number of elements moved (or avoid having to move any).

If we want to conceal the pointer values from native code, which is
necessary to allow the GC to move JNI-referenced objects around, then we
have to use a more complicated indirection mechanism.

The spec says, "Local references are only valid in the thread in which
they are created.  The native code must not pass local references from
one thread to another."


Pinned objects

For some large chunks of data, notably primitive arrays and String data,
JNI allows the VM to choose whether it wants to pin the array object or
make a copy.  We currently pin the memory for better execution performance.

TODO: we're using simple root set references to pin primitive array data,
because they have the property we need (i.e. the pointer we return is
guaranteed valid until we explicitly release it).  However, if we have a
compacting GC and don't want to pin all memory held by all global refs,
we need to treat these differently.


Global reference tracking

There should be a small "active" set centered around the most-recently
added items.

Because it's global, access to it has to be synchronized.  Additions and
removals require grabbing a mutex.  If the table serves as an indirection
mechanism (i.e. it's not just a list for the benefit of the garbage
collector), reference lookups may also require grabbing a mutex.

The JNI spec does not define any sort of limit, so the list must be able
to expand to a reasonable size.  It may be useful to log significant
increases in usage to help identify resource leaks.


Weak-global reference tracking

[TBD]


Local reference tracking

The table of local references can be stored on the interpreted stack or
in a parallel data structure (one per thread).

*** Approach #1: use the interpreted stack

The easiest place to tuck it is between the frame ptr and the first saved
register, which is always in0.  (See the ASCII art in Stack.h.)  We can
shift the "VM-specific goop" and frame ptr down, effectively inserting
the JNI local refs in the space normally occupied by local variables.

(Three things are accessed from the frame pointer:
 (1) framePtr[N] is register vN, used to get at "ins" and "locals".
 (2) framePtr - sizeof(StackSaveArea) is the VM frame goop.
 (3) framePtr - sizeof(StackSaveArea) - numOuts is where the "outs" go.
The only thing that isn't determined by an offset from the current FP
is the previous frame.  However, tucking things below the previous frame
can be problematic because the "outs" of the previous frame overlap with
the "ins" of the current frame.  If the "ins" are altered they must be
restored before we return.  For a native method call, the easiest and
safest thing to disrupt is #1, because there are no locals and the "ins"
are all copied to the native stack.)

We can implement Push/PopLocalFrame with the existing stack frame calls,
making sure we copy some goop from the previous frame (notably the method
ptr, so that dvmGetCurrentJNIMethod() doesn't require extra effort).

We can pre-allocate the storage at the time the stack frame is first
set up, but we have to be careful.  When calling from interpreted code
the frame ptr points directly at the arguments we're passing, but we can
offset the args pointer when calling the native bridge.

To manage the local ref collection, we need to be able to find three
things: (1) the start of the region, (2) the end of the region, and (3)
the next available entry.  The last is only required for quick adds.
We currently have two easily-accessible pointers, the current FP and the
previous frame's FP.  (The "stack pointer" shown in the ASCII art doesn't
actually exist in the interpreted world.)

We can't use the current FP to find the first "in", because we want to
insert the variable-sized local refs table between them.  It's awkward
to use the previous frame's FP because native methods invoked via
dvmCallMethod() or dvmInvokeMethod() don't have "ins", but native methods
invoked from interpreted code do.  We can either track the local refs
table size with a field in the stack frame, or insert unnecessary items
so that all native stack frames have "ins".

Assuming we can find the region bounds, we still need pointer #3
for an efficient implementation.  This can be stored in an otherwise
unused-for-native field in the frame goop.

When we run out of room we have to make more space.  If we start allocating
locals immediately below in0 and grow downward, we will detect end-of-space
by running into the current frame's FP.  We then memmove() the goop down
(memcpy if we guarantee the additional size is larger than the frame).
This is nice because we only have to move sizeof(StackSaveArea) bytes
each time.

Stack walking should be okay so long as nothing tries to access the
"ins" by an offset from the FP.  In theory the "ins" could be read by
the debugger or SIGQUIT handler looking for "this" or other arguments,
but in practice this behavior isn't expected to work for native methods,
so we can simply disallow it.

A conservative GC can just scan the entire stack from top to bottom to find
all references.  An exact GC will need to understand the actual layout.

*** Approach #2: use a parallel stack

Each Thread/JNIEnv points to a reference table.  The struct has
a system-heap-allocated array of references and a pointer to the
next-available entry ("nextEntry").

Each stack frame has a pointer to what it sees as the "bottom" element
in the array (we can double-up the "currentPc" field).  This is set to
"nextEntry" when the frame is pushed on.  As local references are added
or removed, "nextEntry" is updated.

We implement Push/PopLocalFrame with actual stack frames.  Before a JNI
frame gets popped, we set "nextEntry" to the "top" pointer of the current
frame, effectively releasing the references.

The GC will scan all references from the start of the table to the
"nextEntry" pointer.

*** Comparison

All approaches will return a failure result when they run out of local
reference space.  For #1 that means blowing out the stack, for #2 it's
running out of room in the array.

Compared to #1, approach #2:
 - Needs only one pointer in the stack frame goop.
 - Makes pre-allocating storage unnecessary.
 - Doesn't contend with interpreted stack depth for space.  In most
   cases, if something blows out the local ref storage, it's because the
   JNI code was misbehaving rather than called from way down.
 - Allows the GC to do a linear scan per thread in a buffer that is 100%
   references.  The GC can be slightly less smart when scanning the stack.
 - Will be easier to work with if we combine native and interpeted stacks.

 - Isn't as clean, especially when popping frames, since we have to do
   explicit work.  Fortunately we only have to do it when popping native
   method calls off, so it doesn't add overhead to interpreted code paths.
 - Is awkward to expand dynamically.  We'll want to pre-allocate the full
   amount of space; this is fine, since something on the order of 1KB should
   be plenty.  The JNI spec allows us to limit this.
 - Requires the GC to scan even more memory.  With the references embedded
   in the stack we get better locality of reference.

*/

/* fwd */
static const struct JNINativeInterface gNativeInterface;
static jobject addGlobalReference(Object* obj);

#ifdef WITH_JNI_STACK_CHECK
# define COMPUTE_STACK_SUM(_self)   computeStackSum(_self);
# define CHECK_STACK_SUM(_self)     checkStackSum(_self);
//static void computeStackSum(Thread* self);
//static void checkStackSum(Thread* self);
#else
# define COMPUTE_STACK_SUM(_self)   ((void)0)
# define CHECK_STACK_SUM(_self)     ((void)0)
#endif


/*
 * ===========================================================================
 *      Utility functions
 * ===========================================================================
 */

/*
 * Entry/exit processing for all JNI calls.
 *
 * If TRUSTED_JNIENV is set, we get to skip the (curiously expensive)
 * thread-local storage lookup on our Thread*.  If the caller has passed
 * the wrong JNIEnv in, we're going to be accessing unsynchronized
 * structures from more than one thread, and things are going to fail
 * in bizarre ways.  This is only sensible if the native code has been
 * fully exercised with CheckJNI enabled.
 */
#define TRUSTED_JNIENV
#ifdef TRUSTED_JNIENV
# define JNI_ENTER()                                                        \
        Thread* _self = ((JNIEnvExt*)env)->self;                            \
        CHECK_STACK_SUM(_self);                                             \
        dvmChangeStatus(_self, THREAD_RUNNING)
#else
# define JNI_ENTER()                                                        \
        Thread* _self = dvmThreadSelf();                                    \
        UNUSED_PARAMETER(env);                                              \
        CHECK_STACK_SUM(_self);                                             \
        dvmChangeStatus(_self, THREAD_RUNNING)
#endif
#define JNI_EXIT()                                                          \
        dvmChangeStatus(_self, THREAD_NATIVE);                              \
        COMPUTE_STACK_SUM(_self)

#define kGlobalRefsTableInitialSize 512
#define kGlobalRefsTableMaxSize     51200       /* arbitrary, must be < 64K */
#define kGrefWaterInterval          100
#define kTrackGrefUsage             true

#define kPinTableInitialSize        16
#define kPinTableMaxSize            1024
#define kPinComplainThreshold       10

/*
 * Allocate the global references table, and look up some classes for
 * the benefit of direct buffer access.
 */
bool dvmJniStartup(void)
{
#ifdef USE_INDIRECT_REF
    if (!dvmInitIndirectRefTable(&gDvm.jniGlobalRefTable,
            kGlobalRefsTableInitialSize, kGlobalRefsTableMaxSize,
            kIndirectKindGlobal))
        return false;
#else
    if (!dvmInitReferenceTable(&gDvm.jniGlobalRefTable,
            kGlobalRefsTableInitialSize, kGlobalRefsTableMaxSize))
        return false;
#endif

    dvmInitMutex(&gDvm.jniGlobalRefLock);
    gDvm.jniGlobalRefLoMark = 0;
    gDvm.jniGlobalRefHiMark = kGrefWaterInterval * 2;

    if (!dvmInitReferenceTable(&gDvm.jniPinRefTable,
            kPinTableInitialSize, kPinTableMaxSize))
        return false;

    dvmInitMutex(&gDvm.jniPinRefLock);

    Method* meth;

    /*
     * Grab the PhantomReference constructor.
     */
    gDvm.classJavaLangRefPhantomReference =
        dvmFindSystemClassNoInit("Ljava/lang/ref/PhantomReference;");
    if (gDvm.classJavaLangRefPhantomReference == NULL) {
        LOGE("Unable to find PhantomReference class\n");
        return false;
    }
    meth= dvmFindDirectMethodByDescriptor(gDvm.classJavaLangRefPhantomReference,
        "<init>", "(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V");
    if (meth == NULL) {
        LOGE("Unable to find constructor for PhantomReference\n");
        return false;
    }
    gDvm.methJavaLangRefPhantomReference_init = meth;


    /*
     * Look up and cache pointers to some direct buffer classes, fields,
     * and methods.
     */
    ClassObject* platformAddressClass =
        dvmFindSystemClassNoInit("Lorg/apache/harmony/luni/platform/PlatformAddress;");
    ClassObject* platformAddressFactoryClass =
        dvmFindSystemClassNoInit("Lorg/apache/harmony/luni/platform/PlatformAddressFactory;");
    ClassObject* directBufferClass =
        dvmFindSystemClassNoInit("Lorg/apache/harmony/nio/internal/DirectBuffer;");
    ClassObject* readWriteBufferClass =
        dvmFindSystemClassNoInit("Ljava/nio/ReadWriteDirectByteBuffer;");
    ClassObject* bufferClass =
        dvmFindSystemClassNoInit("Ljava/nio/Buffer;");

    if (platformAddressClass == NULL || platformAddressFactoryClass == NULL ||
        directBufferClass == NULL || readWriteBufferClass == NULL ||
        bufferClass == NULL)
    {
        LOGE("Unable to find internal direct buffer classes\n");
        return false;
    }
    gDvm.classJavaNioReadWriteDirectByteBuffer = readWriteBufferClass;
    gDvm.classOrgApacheHarmonyNioInternalDirectBuffer = directBufferClass;
    /* need a global reference for extended CheckJNI tests */
    gDvm.jclassOrgApacheHarmonyNioInternalDirectBuffer =
        addGlobalReference((Object*) directBufferClass);

    /*
     * We need a Method* here rather than a vtable offset, because
     * DirectBuffer is an interface class.
     */
    meth = dvmFindVirtualMethodByDescriptor(
                gDvm.classOrgApacheHarmonyNioInternalDirectBuffer,
                "getEffectiveAddress",
                "()Lorg/apache/harmony/luni/platform/PlatformAddress;");
    if (meth == NULL) {
        LOGE("Unable to find PlatformAddress.getEffectiveAddress\n");
        return false;
    }
    gDvm.methOrgApacheHarmonyNioInternalDirectBuffer_getEffectiveAddress = meth;

    meth = dvmFindVirtualMethodByDescriptor(platformAddressClass,
                "toLong", "()J");
    if (meth == NULL) {
        LOGE("Unable to find PlatformAddress.toLong\n");
        return false;
    }
    gDvm.voffOrgApacheHarmonyLuniPlatformPlatformAddress_toLong =
        meth->methodIndex;

    meth = dvmFindDirectMethodByDescriptor(platformAddressFactoryClass,
                "on",
                "(I)Lorg/apache/harmony/luni/platform/PlatformAddress;");
    if (meth == NULL) {
        LOGE("Unable to find PlatformAddressFactory.on\n");
        return false;
    }
    gDvm.methOrgApacheHarmonyLuniPlatformPlatformAddress_on = meth;

    meth = dvmFindDirectMethodByDescriptor(readWriteBufferClass,
                "<init>",
                "(Lorg/apache/harmony/luni/platform/PlatformAddress;II)V");
    if (meth == NULL) {
        LOGE("Unable to find ReadWriteDirectByteBuffer.<init>\n");
        return false;
    }
    gDvm.methJavaNioReadWriteDirectByteBuffer_init = meth;

    gDvm.offOrgApacheHarmonyLuniPlatformPlatformAddress_osaddr =
        dvmFindFieldOffset(platformAddressClass, "osaddr", "I");
    if (gDvm.offOrgApacheHarmonyLuniPlatformPlatformAddress_osaddr < 0) {
        LOGE("Unable to find PlatformAddress.osaddr\n");
        return false;
    }

    gDvm.offJavaNioBuffer_capacity =
        dvmFindFieldOffset(bufferClass, "capacity", "I");
    if (gDvm.offJavaNioBuffer_capacity < 0) {
        LOGE("Unable to find Buffer.capacity\n");
        return false;
    }

    gDvm.offJavaNioBuffer_effectiveDirectAddress =
        dvmFindFieldOffset(bufferClass, "effectiveDirectAddress", "I");
    if (gDvm.offJavaNioBuffer_effectiveDirectAddress < 0) {
        LOGE("Unable to find Buffer.effectiveDirectAddress\n");
        return false;
    }

    return true;
}

/*
 * Free the global references table.
 */
void dvmJniShutdown(void)
{
#ifdef USE_INDIRECT_REF
    dvmClearIndirectRefTable(&gDvm.jniGlobalRefTable);
#else
    dvmClearReferenceTable(&gDvm.jniGlobalRefTable);
#endif
    dvmClearReferenceTable(&gDvm.jniPinRefTable);
}


/*
 * Find the JNIEnv associated with the current thread.
 *
 * Currently stored in the Thread struct.  Could also just drop this into
 * thread-local storage.
 */
JNIEnvExt* dvmGetJNIEnvForThread(void)
{
    Thread* self = dvmThreadSelf();
    if (self == NULL)
        return NULL;
    return (JNIEnvExt*) dvmGetThreadJNIEnv(self);
}

/*
 * Create a new JNIEnv struct and add it to the VM's list.
 *
 * "self" will be NULL for the main thread, since the VM hasn't started
 * yet; the value will be filled in later.
 */
JNIEnv* dvmCreateJNIEnv(Thread* self)
{
    JavaVMExt* vm = (JavaVMExt*) gDvm.vmList;
    JNIEnvExt* newEnv;

    //if (self != NULL)
    //    LOGI("Ent CreateJNIEnv: threadid=%d %p\n", self->threadId, self);

    assert(vm != NULL);

    newEnv = (JNIEnvExt*) calloc(1, sizeof(JNIEnvExt));
    newEnv->funcTable = &gNativeInterface;
    newEnv->vm = vm;
    newEnv->forceDataCopy = vm->forceDataCopy;
    if (self != NULL) {
        dvmSetJniEnvThreadId((JNIEnv*) newEnv, self);
        assert(newEnv->envThreadId != 0);
    } else {
        /* make it obvious if we fail to initialize these later */
        newEnv->envThreadId = 0x77777775;
        newEnv->self = (Thread*) 0x77777779;
    }
    if (vm->useChecked)
        dvmUseCheckedJniEnv(newEnv);

    dvmLockMutex(&vm->envListLock);

    /* insert at head of list */
    newEnv->next = vm->envList;
    assert(newEnv->prev == NULL);
    if (vm->envList == NULL)            // rare, but possible
        vm->envList = newEnv;
    else
        vm->envList->prev = newEnv;
    vm->envList = newEnv;

    dvmUnlockMutex(&vm->envListLock);

    //if (self != NULL)
    //    LOGI("Xit CreateJNIEnv: threadid=%d %p\n", self->threadId, self);
    return (JNIEnv*) newEnv;
}

/*
 * Remove a JNIEnv struct from the list and free it.
 */
void dvmDestroyJNIEnv(JNIEnv* env)
{
    JNIEnvExt* extEnv = (JNIEnvExt*) env;
    JavaVMExt* vm = extEnv->vm;
    Thread* self;

    if (env == NULL)
        return;

    self = dvmThreadSelf();
    assert(self != NULL);

    //LOGI("Ent DestroyJNIEnv: threadid=%d %p\n", self->threadId, self);

    dvmLockMutex(&vm->envListLock);

    if (extEnv == vm->envList) {
        assert(extEnv->prev == NULL);
        vm->envList = extEnv->next;
    } else {
        assert(extEnv->prev != NULL);
        extEnv->prev->next = extEnv->next;
    }
    if (extEnv->next != NULL)
        extEnv->next->prev = extEnv->prev;

    dvmUnlockMutex(&extEnv->vm->envListLock);

    free(env);
    //LOGI("Xit DestroyJNIEnv: threadid=%d %p\n", self->threadId, self);
}


/*
 * Retrieve the ReferenceTable struct for the current thread.
 *
 * Going through "env" rather than dvmThreadSelf() is faster but will
 * get weird if the JNI code is passing the wrong JNIEnv around.
 */
#ifdef USE_INDIRECT_REF
static inline IndirectRefTable* getLocalRefTable(JNIEnv* env)
#else
static inline ReferenceTable* getLocalRefTable(JNIEnv* env)
#endif
{
    //return &dvmThreadSelf()->jniLocalRefTable;
    return &((JNIEnvExt*)env)->self->jniLocalRefTable;
}

#ifdef USE_INDIRECT_REF
/*
 * Convert an indirect reference to an Object reference.  The indirect
 * reference may be local, global, or weak-global.
 *
 * If "jobj" is NULL or an invalid indirect reference, this returns NULL.
 */
Object* dvmDecodeIndirectRef(JNIEnv* env, jobject jobj)
{
    if (jobj == NULL)
        return NULL;

    Object* result;

    switch (dvmGetIndirectRefType(jobj)) {
    case kIndirectKindLocal:
        {
            IndirectRefTable* pRefTable = getLocalRefTable(env);
            result = dvmGetFromIndirectRefTable(pRefTable, jobj);
        }
        break;
    case kIndirectKindGlobal:
        {
            // TODO: find a way to avoid the mutex activity here
            IndirectRefTable* pRefTable = &gDvm.jniGlobalRefTable;
            dvmLockMutex(&gDvm.jniGlobalRefLock);
            result = dvmGetFromIndirectRefTable(pRefTable, jobj);
            dvmUnlockMutex(&gDvm.jniGlobalRefLock);
        }
        break;
    case kIndirectKindWeakGlobal:
        {
            // TODO: implement
            LOGE("weak-global not yet supported\n");
            result = NULL;
            dvmAbort();
        }
        break;
    case kIndirectKindInvalid:
    default:
        LOGW("Invalid indirect reference %p in decodeIndirectRef\n", jobj);
        dvmAbort();
        result = NULL;
        break;
    }

    return result;
}
#else
    /* use trivial inline in JniInternal.h for performance */
#endif

/*
 * Add a local reference for an object to the current stack frame.  When
 * the native function returns, the reference will be discarded.
 *
 * We need to allow the same reference to be added multiple times.
 *
 * This will be called on otherwise unreferenced objects.  We cannot do
 * GC allocations here, and it's best if we don't grab a mutex.
 *
 * Returns the local reference (currently just the same pointer that was
 * passed in), or NULL on failure.
 */
static jobject addLocalReference(JNIEnv* env, Object* obj)
{
    if (obj == NULL)
        return NULL;

    jobject jobj;

#ifdef USE_INDIRECT_REF
    IndirectRefTable* pRefTable = getLocalRefTable(env);
    void* curFrame = ((JNIEnvExt*)env)->self->curFrame;
    u4 cookie = SAVEAREA_FROM_FP(curFrame)->xtra.localRefCookie;

    jobj = (jobject) dvmAddToIndirectRefTable(pRefTable, cookie, obj);
    if (jobj == NULL) {
        dvmDumpIndirectRefTable(pRefTable, "JNI local");
        LOGE("Failed adding to JNI local ref table (has %d entries)\n",
            (int) dvmIndirectRefTableEntries(pRefTable));
        dvmDumpThread(dvmThreadSelf(), false);
        dvmAbort();     // spec says call FatalError; this is equivalent
    } else {
        LOGVV("LREF add %p  (%s.%s) (ent=%d)\n", obj,
            dvmGetCurrentJNIMethod()->clazz->descriptor,
            dvmGetCurrentJNIMethod()->name,
            (int) dvmReferenceTableEntries(pRefTable));
    }
#else
    ReferenceTable* pRefTable = getLocalRefTable(env);

    if (!dvmAddToReferenceTable(pRefTable, obj)) {
        dvmDumpReferenceTable(pRefTable, "JNI local");
        LOGE("Failed adding to JNI local ref table (has %d entries)\n",
            (int) dvmReferenceTableEntries(pRefTable));
        dvmDumpThread(dvmThreadSelf(), false);
        dvmAbort();     // spec says call FatalError; this is equivalent
    } else {
        LOGVV("LREF add %p  (%s.%s) (ent=%d)\n", obj,
            dvmGetCurrentJNIMethod()->clazz->descriptor,
            dvmGetCurrentJNIMethod()->name,
            (int) dvmReferenceTableEntries(pRefTable));
    }

    jobj = (jobject) obj;
#endif

    return jobj;
}

/*
 * Ensure that at least "capacity" references can be held in the local
 * refs table of the current thread.
 */
static bool ensureLocalCapacity(JNIEnv* env, int capacity)
{
#ifdef USE_INDIRECT_REF
    IndirectRefTable* pRefTable = getLocalRefTable(env);
    int numEntries = dvmIndirectRefTableEntries(pRefTable);
    // TODO: this isn't quite right, since "numEntries" includes holes
    return ((kJniLocalRefMax - numEntries) >= capacity);
#else
    ReferenceTable* pRefTable = getLocalRefTable(env);

    return (kJniLocalRefMax - (pRefTable->nextEntry - pRefTable->table) >= capacity);
#endif
}

/*
 * Explicitly delete a reference from the local list.
 */
static void deleteLocalReference(JNIEnv* env, jobject jobj)
{
    if (jobj == NULL)
        return;

#ifdef USE_INDIRECT_REF
    IndirectRefTable* pRefTable = getLocalRefTable(env);
    Thread* self = ((JNIEnvExt*)env)->self;
    u4 cookie = SAVEAREA_FROM_FP(self->curFrame)->xtra.localRefCookie;

    if (!dvmRemoveFromIndirectRefTable(pRefTable, cookie, jobj)) {
        /*
         * Attempting to delete a local reference that is not in the
         * topmost local reference frame is a no-op.  DeleteLocalRef returns
         * void and doesn't throw any exceptions, but we should probably
         * complain about it so the user will notice that things aren't
         * going quite the way they expect.
         */
        LOGW("JNI WARNING: DeleteLocalRef(%p) failed to find entry\n", jobj);
    }
#else
    ReferenceTable* pRefTable = getLocalRefTable(env);
    Thread* self = ((JNIEnvExt*)env)->self;
    Object** bottom = SAVEAREA_FROM_FP(self->curFrame)->xtra.localRefCookie;

    if (!dvmRemoveFromReferenceTable(pRefTable, bottom, (Object*) jobj)) {
        /*
         * Attempting to delete a local reference that is not in the
         * topmost local reference frame is a no-op.  DeleteLocalRef returns
         * void and doesn't throw any exceptions, but we should probably
         * complain about it so the user will notice that things aren't
         * going quite the way they expect.
         */
        LOGW("JNI WARNING: DeleteLocalRef(%p) failed to find entry (valid=%d)\n",
            jobj, dvmIsValidObject((Object*) jobj));
    }
#endif
}

/*
 * Add a global reference for an object.
 *
 * We may add the same object more than once.  Add/remove calls are paired,
 * so it needs to appear on the list multiple times.
 */
static jobject addGlobalReference(Object* obj)
{
    if (obj == NULL)
        return NULL;

    //LOGI("adding obj=%p\n", obj);
    //dvmDumpThread(dvmThreadSelf(), false);

    if (false && ((Object*)obj)->clazz == gDvm.classJavaLangClass) {
        ClassObject* clazz = (ClassObject*) obj;
        LOGI("-------\n");
        LOGI("Adding global ref on class %s\n", clazz->descriptor);
        dvmDumpThread(dvmThreadSelf(), false);
    }
    if (false && ((Object*)obj)->clazz == gDvm.classJavaLangString) {
        StringObject* strObj = (StringObject*) obj;
        char* str = dvmCreateCstrFromString(strObj);
        if (strcmp(str, "sync-response") == 0) {
            LOGI("-------\n");
            LOGI("Adding global ref on string '%s'\n", str);
            dvmDumpThread(dvmThreadSelf(), false);
            //dvmAbort();
        }
        free(str);
    }
    if (false && ((Object*)obj)->clazz == gDvm.classArrayByte) {
        ArrayObject* arrayObj = (ArrayObject*) obj;
        if (arrayObj->length == 8192 /*&&
            dvmReferenceTableEntries(&gDvm.jniGlobalRefTable) > 400*/)
        {
            LOGI("Adding global ref on byte array %p (len=%d)\n",
                arrayObj, arrayObj->length);
            dvmDumpThread(dvmThreadSelf(), false);
        }
    }

    jobject jobj;

    dvmLockMutex(&gDvm.jniGlobalRefLock);

    /*
     * Throwing an exception on failure is problematic, because JNI code
     * may not be expecting an exception, and things sort of cascade.  We
     * want to have a hard limit to catch leaks during debugging, but this
     * otherwise needs to expand until memory is consumed.  As a practical
     * matter, if we have many thousands of global references, chances are
     * we're either leaking global ref table entries or we're going to
     * run out of space in the GC heap.
     */
#ifdef USE_INDIRECT_REF
    jobj = dvmAddToIndirectRefTable(&gDvm.jniGlobalRefTable, IRT_FIRST_SEGMENT,
            obj);
    if (jobj == NULL) {
        dvmDumpIndirectRefTable(&gDvm.jniGlobalRefTable, "JNI global");
        LOGE("Failed adding to JNI global ref table (%d entries)\n",
            (int) dvmIndirectRefTableEntries(&gDvm.jniGlobalRefTable));
        dvmAbort();
    }

    LOGVV("GREF add %p  (%s.%s)\n", obj,
        dvmGetCurrentJNIMethod()->clazz->descriptor,
        dvmGetCurrentJNIMethod()->name);

    /* GREF usage tracking; should probably be disabled for production env */
    if (kTrackGrefUsage && gDvm.jniGrefLimit != 0) {
        int count = dvmIndirectRefTableEntries(&gDvm.jniGlobalRefTable);
        // TODO: adjust for "holes"
        if (count > gDvm.jniGlobalRefHiMark) {
            LOGD("GREF has increased to %d\n", count);
            gDvm.jniGlobalRefHiMark += kGrefWaterInterval;
            gDvm.jniGlobalRefLoMark += kGrefWaterInterval;

            /* watch for "excessive" use; not generally appropriate */
            if (count >= gDvm.jniGrefLimit) {
                JavaVMExt* vm = (JavaVMExt*) gDvm.vmList;
                if (vm->warnError) {
                    dvmDumpIndirectRefTable(&gDvm.jniGlobalRefTable,
                        "JNI global");
                    LOGE("Excessive JNI global references (%d)\n", count);
                    dvmAbort();
                } else {
                    LOGW("Excessive JNI global references (%d)\n", count);
                }
            }
        }
    }
#else
    if (!dvmAddToReferenceTable(&gDvm.jniGlobalRefTable, obj)) {
        dvmDumpReferenceTable(&gDvm.jniGlobalRefTable, "JNI global");
        LOGE("Failed adding to JNI global ref table (%d entries)\n",
            (int) dvmReferenceTableEntries(&gDvm.jniGlobalRefTable));
        dvmAbort();
    }
    jobj = (jobject) obj;

    LOGVV("GREF add %p  (%s.%s)\n", obj,
        dvmGetCurrentJNIMethod()->clazz->descriptor,
        dvmGetCurrentJNIMethod()->name);

    /* GREF usage tracking; should probably be disabled for production env */
    if (kTrackGrefUsage && gDvm.jniGrefLimit != 0) {
        int count = dvmReferenceTableEntries(&gDvm.jniGlobalRefTable);
        if (count > gDvm.jniGlobalRefHiMark) {
            LOGD("GREF has increased to %d\n", count);
            gDvm.jniGlobalRefHiMark += kGrefWaterInterval;
            gDvm.jniGlobalRefLoMark += kGrefWaterInterval;

            /* watch for "excessive" use; not generally appropriate */
            if (count >= gDvm.jniGrefLimit) {
                JavaVMExt* vm = (JavaVMExt*) gDvm.vmList;
                if (vm->warnError) {
                    dvmDumpReferenceTable(&gDvm.jniGlobalRefTable,"JNI global");
                    LOGE("Excessive JNI global references (%d)\n", count);
                    dvmAbort();
                } else {
                    LOGW("Excessive JNI global references (%d)\n", count);
                }
            }
        }
    }
#endif

bail:
    dvmUnlockMutex(&gDvm.jniGlobalRefLock);
    return jobj;
}

/*
 * Remove a global reference.  In most cases it's the entry most recently
 * added, which makes this pretty quick.
 *
 * Thought: if it's not the most recent entry, just null it out.  When we
 * fill up, do a compaction pass before we expand the list.
 */
static void deleteGlobalReference(jobject jobj)
{
    if (jobj == NULL)
        return;

    dvmLockMutex(&gDvm.jniGlobalRefLock);

#ifdef USE_INDIRECT_REF
    if (!dvmRemoveFromIndirectRefTable(&gDvm.jniGlobalRefTable,
            IRT_FIRST_SEGMENT, jobj))
    {
        LOGW("JNI: DeleteGlobalRef(%p) failed to find entry\n", jobj);
        goto bail;
    }

    if (kTrackGrefUsage && gDvm.jniGrefLimit != 0) {
        int count = dvmIndirectRefTableEntries(&gDvm.jniGlobalRefTable);
        // TODO: not quite right, need to subtract holes
        if (count < gDvm.jniGlobalRefLoMark) {
            LOGD("GREF has decreased to %d\n", count);
            gDvm.jniGlobalRefHiMark -= kGrefWaterInterval;
            gDvm.jniGlobalRefLoMark -= kGrefWaterInterval;
        }
    }
#else
    if (!dvmRemoveFromReferenceTable(&gDvm.jniGlobalRefTable,
            gDvm.jniGlobalRefTable.table, jobj))
    {
        LOGW("JNI: DeleteGlobalRef(%p) failed to find entry (valid=%d)\n",
            jobj, dvmIsValidObject((Object*) jobj));
        goto bail;
    }

    if (kTrackGrefUsage && gDvm.jniGrefLimit != 0) {
        int count = dvmReferenceTableEntries(&gDvm.jniGlobalRefTable);
        if (count < gDvm.jniGlobalRefLoMark) {
            LOGD("GREF has decreased to %d\n", count);
            gDvm.jniGlobalRefHiMark -= kGrefWaterInterval;
            gDvm.jniGlobalRefLoMark -= kGrefWaterInterval;
        }
    }
#endif

bail:
    dvmUnlockMutex(&gDvm.jniGlobalRefLock);
}


/*
 * Get the "magic" JNI weak global ReferenceQueue.  It's allocated on
 * first use.
 *
 * Returns NULL with an exception raised if allocation fails.
 */
static Object* getWeakGlobalRefQueue(void)
{
    /* use an indirect variable to avoid "type-punned pointer" complaints */
    Object** pGlobalQ = &gDvm.jniWeakGlobalRefQueue;

    if (*pGlobalQ != NULL)
        return *pGlobalQ;

    ClassObject* clazz = dvmFindSystemClass("Ljava/lang/ref/ReferenceQueue;");
    if (clazz == NULL) {
        LOGE("Unable to find java.lang.ref.ReferenceQueue");
        dvmAbort();
    }

    /*
     * Create an instance of ReferenceQueue.  The object is never actually
     * used for anything, so we don't need to call a constructor.  (We could
     * get away with using an instance of Object, but this is cleaner.)
     */
    Object* queue = dvmAllocObject(clazz, ALLOC_DEFAULT);
    if (queue == NULL) {
        LOGW("Failed allocating weak global ref queue\n");
        assert(dvmCheckException(dvmThreadSelf()));
        return NULL;
    }
    dvmReleaseTrackedAlloc(queue, NULL);

    /*
     * Save it, using atomic ops to ensure we don't double-up.  The gDvm
     * field is known to the GC.
     */
    if (!ATOMIC_CMP_SWAP((int*) pGlobalQ, 0, (int) queue)) {
        LOGD("WOW: lost race to create weak global ref queue\n");
        queue = *pGlobalQ;
    }

    return queue;
}


/*
 * We create a PhantomReference that references the object, add a
 * global reference to it, and then flip some bits before returning it.
 * The last step ensures that we detect it as special and that only
 * appropriate calls will accept it.
 *
 * On failure, returns NULL with an exception pending.
 */
static jweak createWeakGlobalRef(JNIEnv* env, jobject jobj)
{
    if (jobj == NULL)
        return NULL;

    Thread* self = ((JNIEnvExt*)env)->self;
    Object* obj = dvmDecodeIndirectRef(env, jobj);
    Object* weakGlobalQueue = getWeakGlobalRefQueue();
    Object* phantomObj;
    jobject phantomRef;

    /*
     * Allocate a PhantomReference, then call the constructor to set
     * the referent and the reference queue.
     *
     * We use a "magic" reference queue that the GC knows about; it behaves
     * more like a queueless WeakReference, clearing the referent and
     * not calling enqueue().
     */
    if (!dvmIsClassInitialized(gDvm.classJavaLangRefPhantomReference))
        dvmInitClass(gDvm.classJavaLangRefPhantomReference);
    phantomObj = dvmAllocObject(gDvm.classJavaLangRefPhantomReference,
            ALLOC_DEFAULT);
    if (phantomObj == NULL) {
        assert(dvmCheckException(self));
        LOGW("Failed on WeakGlobalRef alloc\n");
        return NULL;
    }

    JValue unused;
    dvmCallMethod(self, gDvm.methJavaLangRefPhantomReference_init, phantomObj,
        &unused, jobj, weakGlobalQueue);
    dvmReleaseTrackedAlloc(phantomObj, self);

    if (dvmCheckException(self)) {
        LOGW("PhantomReference init failed\n");
        return NULL;
    }

    LOGV("+++ WGR: created phantom ref %p for object %p\n", phantomObj, obj);

    /*
     * Add it to the global reference table, and mangle the pointer.
     */
    phantomRef = addGlobalReference(phantomObj);
    return dvmObfuscateWeakGlobalRef(phantomRef);
}

/*
 * Delete the global reference that's keeping the PhantomReference around.
 * The PhantomReference will eventually be discarded by the GC.
 */
static void deleteWeakGlobalRef(JNIEnv* env, jweak wref)
{
    if (wref == NULL)
        return;

    jobject phantomRef = dvmNormalizeWeakGlobalRef(wref);
    deleteGlobalReference(phantomRef);
}

/*
 * Extract the referent from a PhantomReference.  Used for weak global
 * references.
 *
 * "jwobj" is a "mangled" WGR pointer.
 */
static Object* getPhantomReferent(JNIEnv* env, jweak jwobj)
{
    jobject jobj = dvmNormalizeWeakGlobalRef(jwobj);
    Object* obj = dvmDecodeIndirectRef(env, jobj);

    if (obj->clazz != gDvm.classJavaLangRefPhantomReference) {
        LOGE("%p is not a phantom reference (%s)\n",
            jwobj, obj->clazz->descriptor);
        return NULL;
    }

    return dvmGetFieldObject(obj, gDvm.offJavaLangRefReference_referent);
}


/*
 * Objects don't currently move, so we just need to create a reference
 * that will ensure the array object isn't collected.
 *
 * We use a separate reference table, which is part of the GC root set.
 */
static void pinPrimitiveArray(ArrayObject* arrayObj)
{
    if (arrayObj == NULL)
        return;

    dvmLockMutex(&gDvm.jniPinRefLock);
    if (!dvmAddToReferenceTable(&gDvm.jniPinRefTable, (Object*)arrayObj)) {
        dvmDumpReferenceTable(&gDvm.jniPinRefTable, "JNI pinned array");
        LOGE("Failed adding to JNI pinned array ref table (%d entries)\n",
            (int) dvmReferenceTableEntries(&gDvm.jniPinRefTable));
        dvmDumpThread(dvmThreadSelf(), false);
        dvmAbort();
    }

    /*
     * If we're watching global ref usage, also keep an eye on these.
     *
     * The total number of pinned primitive arrays should be pretty small.
     * A single array should not be pinned more than once or twice; any
     * more than that is a strong indicator that a Release function is
     * not being called.
     */
    if (kTrackGrefUsage && gDvm.jniGrefLimit != 0) {
        int count = 0;
        Object** ppObj = gDvm.jniPinRefTable.table;
        while (ppObj < gDvm.jniPinRefTable.nextEntry) {
            if (*ppObj++ == (Object*) arrayObj)
                count++;
        }

        if (count > kPinComplainThreshold) {
            LOGW("JNI: pin count on array %p (%s) is now %d\n",
                arrayObj, arrayObj->obj.clazz->descriptor, count);
            /* keep going */
        }
    }

    dvmUnlockMutex(&gDvm.jniPinRefLock);
}

/*
 * Un-pin the array object.  If an object was pinned twice, it must be
 * unpinned twice before it's free to move.
 */
static void unpinPrimitiveArray(ArrayObject* arrayObj)
{
    if (arrayObj == NULL)
        return;

    dvmLockMutex(&gDvm.jniPinRefLock);
    if (!dvmRemoveFromReferenceTable(&gDvm.jniPinRefTable,
            gDvm.jniPinRefTable.table, (Object*) arrayObj))
    {
        LOGW("JNI: unpinPrimitiveArray(%p) failed to find entry (valid=%d)\n",
            arrayObj, dvmIsValidObject((Object*) arrayObj));
        goto bail;
    }

bail:
    dvmUnlockMutex(&gDvm.jniPinRefLock);
}

/*
 * Dump the contents of the JNI reference tables to the log file.
 *
 * We only dump the local refs associated with the current thread.
 */
void dvmDumpJniReferenceTables(void)
{
    Thread* self = dvmThreadSelf();
    JNIEnv* env = self->jniEnv;
    ReferenceTable* pLocalRefs = getLocalRefTable(env);

#ifdef USE_INDIRECT_REF
    dvmDumpIndirectRefTable(pLocalRefs, "JNI local");
    dvmDumpIndirectRefTable(&gDvm.jniGlobalRefTable, "JNI global");
#else
    dvmDumpReferenceTable(pLocalRefs, "JNI local");
    dvmDumpReferenceTable(&gDvm.jniGlobalRefTable, "JNI global");
#endif
    dvmDumpReferenceTable(&gDvm.jniPinRefTable, "JNI pinned array");
}

/*
 * GC helper function to mark all JNI global references.
 *
 * We're currently handling the "pin" table here too.
 */
void dvmGcMarkJniGlobalRefs()
{
    Object** op;

    dvmLockMutex(&gDvm.jniGlobalRefLock);

#ifdef USE_INDIRECT_REF
    IndirectRefTable* pRefTable = &gDvm.jniGlobalRefTable;
    op = pRefTable->table;
    int numEntries = dvmIndirectRefTableEntries(pRefTable);
    int i;

    for (i = 0; i < numEntries; i++) {
        Object* obj = *op;
        if (obj != NULL)
            dvmMarkObjectNonNull(obj);
        op++;
    }
#else
    op = gDvm.jniGlobalRefTable.table;
    while ((uintptr_t)op < (uintptr_t)gDvm.jniGlobalRefTable.nextEntry) {
        dvmMarkObjectNonNull(*(op++));
    }
#endif

    dvmUnlockMutex(&gDvm.jniGlobalRefLock);


    dvmLockMutex(&gDvm.jniPinRefLock);

    op = gDvm.jniPinRefTable.table;
    while ((uintptr_t)op < (uintptr_t)gDvm.jniPinRefTable.nextEntry) {
        dvmMarkObjectNonNull(*(op++));
    }

    dvmUnlockMutex(&gDvm.jniPinRefLock);
}


#ifndef USE_INDIRECT_REF
/*
 * Determine if "obj" appears in the argument list for the native method.
 *
 * We use the "shorty" signature to determine which argument slots hold
 * reference types.
 */
static bool findInArgList(Thread* self, Object* obj)
{
    const Method* meth;
    u4* fp;
    int i;

    fp = self->curFrame;
    while (1) {
        /*
         * Back up over JNI PushLocalFrame frames.  This works because the
         * previous frame on the interpreted stack is either a break frame
         * (if we called here via native code) or an interpreted method (if
         * we called here via the interpreter).  In both cases the method
         * pointer won't match.
         */
        StackSaveArea* saveArea = SAVEAREA_FROM_FP(fp);
        meth = saveArea->method;
        if (meth != SAVEAREA_FROM_FP(saveArea->prevFrame)->method)
            break;
        fp = saveArea->prevFrame;
    }

    LOGVV("+++ scanning %d args in %s (%s)\n",
        meth->insSize, meth->name, meth->shorty);
    const char* shorty = meth->shorty +1;       /* skip return type char */
    for (i = 0; i < meth->insSize; i++) {
        if (i == 0 && !dvmIsStaticMethod(meth)) {
            /* first arg is "this" ref, not represented in "shorty" */
            if (fp[i] == (u4) obj)
                return true;
        } else {
            /* if this is a reference type, see if it matches */
            switch (*shorty) {
            case 'L':
                if (fp[i] == (u4) obj)
                    return true;
                break;
            case 'D':
            case 'J':
                i++;
                break;
            case '\0':
                LOGE("Whoops! ran off the end of %s (%d)\n",
                    meth->shorty, meth->insSize);
                break;
            default:
                if (fp[i] == (u4) obj)
                    LOGI("NOTE: ref %p match on arg type %c\n", obj, *shorty);
                break;
            }
            shorty++;
        }
    }

    /*
     * For static methods, we also pass a class pointer in.
     */
    if (dvmIsStaticMethod(meth)) {
        //LOGI("+++ checking class pointer in %s\n", meth->name);
        if ((void*)obj == (void*)meth->clazz)
            return true;
    }
    return false;
}
#endif

/*
 * Verify that a reference passed in from native code is one that the
 * code is allowed to have.
 *
 * It's okay for native code to pass us a reference that:
 *  - was passed in as an argument when invoked by native code (and hence
 *    is in the JNI local refs table)
 *  - was returned to it from JNI (and is now in the local refs table)
 *  - is present in the JNI global refs table
 *
 * Used by -Xcheck:jni and GetObjectRefType.
 *
 * NOTE: in the current VM, global and local references are identical.  If
 * something is both global and local, we can't tell them apart, and always
 * return "local".
 */
jobjectRefType dvmGetJNIRefType(JNIEnv* env, jobject jobj)
{
#ifdef USE_INDIRECT_REF
    /*
     * IndirectRefKind is currently defined as an exact match of
     * jobjectRefType, so this is easy.  We have to decode it to determine
     * if it's a valid reference and not merely valid-looking.
     */
    Object* obj = dvmDecodeIndirectRef(env, jobj);

    if (obj == NULL) {
        /* invalid ref, or jobj was NULL */
        return JNIInvalidRefType;
    } else {
        return (jobjectRefType) dvmGetIndirectRefType(jobj);
    }
#else
    ReferenceTable* pRefTable = getLocalRefTable(env);
    Thread* self = dvmThreadSelf();
    //Object** top;
    Object** ptr;

    if (dvmIsWeakGlobalRef(jobj)) {
        return JNIWeakGlobalRefType;
    }

    /* check args */
    if (findInArgList(self, jobj)) {
        //LOGI("--- REF found %p on stack\n", jobj);
        return JNILocalRefType;
    }

    /* check locals */
    if (dvmFindInReferenceTable(pRefTable, pRefTable->table, jobj) != NULL) {
        //LOGI("--- REF found %p in locals\n", jobj);
        return JNILocalRefType;
    }

    /* check globals */
    dvmLockMutex(&gDvm.jniGlobalRefLock);
    if (dvmFindInReferenceTable(&gDvm.jniGlobalRefTable,
            gDvm.jniGlobalRefTable.table, jobj))
    {
        //LOGI("--- REF found %p in globals\n", jobj);
        dvmUnlockMutex(&gDvm.jniGlobalRefLock);
        return JNIGlobalRefType;
    }
    dvmUnlockMutex(&gDvm.jniGlobalRefLock);

    /* not found! */
    return JNIInvalidRefType;
#endif
}

/*
 * Register a method that uses JNI calling conventions.
 */
static bool dvmRegisterJNIMethod(ClassObject* clazz, const char* methodName,
    const char* signature, void* fnPtr)
{
    Method* method;
    bool result = false;

    if (fnPtr == NULL)
        goto bail;

    method = dvmFindDirectMethodByDescriptor(clazz, methodName, signature);
    if (method == NULL)
        method = dvmFindVirtualMethodByDescriptor(clazz, methodName, signature);
    if (method == NULL) {
        LOGW("ERROR: Unable to find decl for native %s.%s %s\n",
            clazz->descriptor, methodName, signature);
        goto bail;
    }

    if (!dvmIsNativeMethod(method)) {
        LOGW("Unable to register: not native: %s.%s %s\n",
            clazz->descriptor, methodName, signature);
        goto bail;
    }

    if (method->nativeFunc != dvmResolveNativeMethod) {
        LOGW("Warning: %s.%s %s was already registered/resolved?\n",
            clazz->descriptor, methodName, signature);
        /* keep going, I guess */
    }

    dvmUseJNIBridge(method, fnPtr);

    LOGV("JNI-registered %s.%s %s\n", clazz->descriptor, methodName,
        signature);
    result = true;

bail:
    return result;
}

/*
 * Returns "true" if CheckJNI is enabled in the VM.
 */
static bool dvmIsCheckJNIEnabled(void)
{
    JavaVMExt* vm = (JavaVMExt*) gDvm.vmList;
    return vm->useChecked;
}

/*
 * Point "method->nativeFunc" at the JNI bridge, and overload "method->insns"
 * to point at the actual function.
 */
void dvmUseJNIBridge(Method* method, void* func)
{
    enum {
        kJNIGeneral = 0,
        kJNISync = 1,
        kJNIVirtualNoRef = 2,
        kJNIStaticNoRef = 3,
    } kind;
    static const DalvikBridgeFunc stdFunc[] = {
        dvmCallJNIMethod_general,
        dvmCallJNIMethod_synchronized,
        dvmCallJNIMethod_virtualNoRef,
        dvmCallJNIMethod_staticNoRef
    };
    static const DalvikBridgeFunc checkFunc[] = {
        dvmCheckCallJNIMethod_general,
        dvmCheckCallJNIMethod_synchronized,
        dvmCheckCallJNIMethod_virtualNoRef,
        dvmCheckCallJNIMethod_staticNoRef
    };

    bool hasRefArg = false;

    if (dvmIsSynchronizedMethod(method)) {
        /* use version with synchronization; calls into general handler */
        kind = kJNISync;
    } else {
        /*
         * Do a quick scan through the "shorty" signature to see if the method
         * takes any reference arguments.
         */
        const char* cp = method->shorty;
        while (*++cp != '\0') {     /* pre-incr to skip return type */
            if (*cp == 'L') {
                /* 'L' used for both object and array references */
                hasRefArg = true;
                break;
            }
        }

        if (hasRefArg) {
            /* use general handler to slurp up reference args */
            kind = kJNIGeneral;
        } else {
            /* virtual methods have a ref in args[0] (not in signature) */
            if (dvmIsStaticMethod(method))
                kind = kJNIStaticNoRef;
            else
                kind = kJNIVirtualNoRef;
        }
    }

    if (dvmIsCheckJNIEnabled()) {
        dvmSetNativeFunc(method, checkFunc[kind], func);
    } else {
        dvmSetNativeFunc(method, stdFunc[kind], func);
    }
}

/*
 * Get the method currently being executed by examining the interp stack.
 */
const Method* dvmGetCurrentJNIMethod(void)
{
    assert(dvmThreadSelf() != NULL);

    void* fp = dvmThreadSelf()->curFrame;
    const Method* meth = SAVEAREA_FROM_FP(fp)->method;

    assert(meth != NULL);
    assert(dvmIsNativeMethod(meth));
    return meth;
}


/*
 * Track a JNI MonitorEnter in the current thread.
 *
 * The goal is to be able to "implicitly" release all JNI-held monitors
 * when the thread detaches.
 *
 * Monitors may be entered multiple times, so we add a new entry for each
 * enter call.  It would be more efficient to keep a counter.  At present
 * there's no real motivation to improve this however.
 */
static void trackMonitorEnter(Thread* self, Object* obj)
{
    static const int kInitialSize = 16;
    ReferenceTable* refTable = &self->jniMonitorRefTable;

    /* init table on first use */
    if (refTable->table == NULL) {
        assert(refTable->maxEntries == 0);

        if (!dvmInitReferenceTable(refTable, kInitialSize, INT_MAX)) {
            LOGE("Unable to initialize monitor tracking table\n");
            dvmAbort();
        }
    }

    if (!dvmAddToReferenceTable(refTable, obj)) {
        /* ran out of memory? could throw exception instead */
        LOGE("Unable to add entry to monitor tracking table\n");
        dvmAbort();
    } else {
        LOGVV("--- added monitor %p\n", obj);
    }
}


/*
 * Track a JNI MonitorExit in the current thread.
 */
static void trackMonitorExit(Thread* self, Object* obj)
{
    ReferenceTable* pRefTable = &self->jniMonitorRefTable;

    if (!dvmRemoveFromReferenceTable(pRefTable, pRefTable->table, obj)) {
        LOGE("JNI monitor %p not found in tracking list\n", obj);
        /* keep going? */
    } else {
        LOGVV("--- removed monitor %p\n", obj);
    }
}

/*
 * Release all monitors held by the jniMonitorRefTable list.
 */
void dvmReleaseJniMonitors(Thread* self)
{
    ReferenceTable* pRefTable = &self->jniMonitorRefTable;
    Object** top = pRefTable->table;

    if (top == NULL)
        return;

    Object** ptr = pRefTable->nextEntry;
    while (--ptr >= top) {
        if (!dvmUnlockObject(self, *ptr)) {
            LOGW("Unable to unlock monitor %p at thread detach\n", *ptr);
        } else {
            LOGVV("--- detach-releasing monitor %p\n", *ptr);
        }
    }

    /* zap it */
    pRefTable->nextEntry = pRefTable->table;
}

#ifdef WITH_JNI_STACK_CHECK
/*
 * Compute a CRC on the entire interpreted stack.
 *
 * Would be nice to compute it on "self" as well, but there are parts of
 * the Thread that can be altered by other threads (e.g. prev/next pointers).
 */
static void computeStackSum(Thread* self)
{
    const u1* low = (const u1*)SAVEAREA_FROM_FP(self->curFrame);
    u4 crc = dvmInitCrc32();
    self->stackCrc = 0;
    crc = dvmComputeCrc32(crc, low, self->interpStackStart - low);
    self->stackCrc = crc;
}

/*
 * Compute a CRC on the entire interpreted stack, and compare it to what
 * we previously computed.
 *
 * We can execute JNI directly from native code without calling in from
 * interpreted code during VM initialization and immediately after JNI
 * thread attachment.  Another opportunity exists during JNI_OnLoad.  Rather
 * than catching these cases we just ignore them here, which is marginally
 * less accurate but reduces the amount of code we have to touch with #ifdefs.
 */
static void checkStackSum(Thread* self)
{
    const u1* low = (const u1*)SAVEAREA_FROM_FP(self->curFrame);
    u4 stackCrc, crc;

    stackCrc = self->stackCrc;
    self->stackCrc = 0;
    crc = dvmInitCrc32();
    crc = dvmComputeCrc32(crc, low, self->interpStackStart - low);
    if (crc != stackCrc) {
        const Method* meth = dvmGetCurrentJNIMethod();
        if (dvmComputeExactFrameDepth(self->curFrame) == 1) {
            LOGD("JNI: bad stack CRC (0x%08x) -- okay during init\n",
                stackCrc);
        } else if (strcmp(meth->name, "nativeLoad") == 0 &&
                  (strcmp(meth->clazz->descriptor, "Ljava/lang/Runtime;") == 0))
        {
            LOGD("JNI: bad stack CRC (0x%08x) -- okay during JNI_OnLoad\n",
                stackCrc);
        } else {
            LOGW("JNI: bad stack CRC (%08x vs %08x)\n", crc, stackCrc);
            dvmAbort();
        }
    }
    self->stackCrc = (u4) -1;       /* make logic errors more noticeable */
}
#endif


/*
 * ===========================================================================
 *      JNI call bridge
 * ===========================================================================
 */

/*
 * The functions here form a bridge between interpreted code and JNI native
 * functions.  The basic task is to convert an array of primitives and
 * references into C-style function arguments.  This is architecture-specific
 * and usually requires help from assembly code.
 *
 * The bridge takes four arguments: the array of parameters, a place to
 * store the function result (if any), the method to call, and a pointer
 * to the current thread.
 *
 * These functions aren't called directly from elsewhere in the VM.
 * A pointer in the Method struct points to one of these, and when a native
 * method is invoked the interpreter jumps to it.
 *
 * (The "internal native" methods are invoked the same way, but instead
 * of calling through a bridge, the target method is called directly.)
 *
 * The "args" array should not be modified, but we do so anyway for
 * performance reasons.  We know that it points to the "outs" area on
 * the current method's interpreted stack.  This area is ignored by the
 * precise GC, because there is no register map for a native method (for
 * an interpreted method the args would be listed in the argument set).
 * We know all of the values exist elsewhere on the interpreted stack,
 * because the method call setup copies them right before making the call,
 * so we don't have to worry about concealing stuff from the GC.
 *
 * If we don't want to modify "args", we either have to create a local
 * copy and modify it before calling dvmPlatformInvoke, or we have to do
 * the local reference replacement within dvmPlatformInvoke.  The latter
 * has some performance advantages, though if we can inline the local
 * reference adds we may win when there's a lot of reference args (unless
 * we want to code up some local ref table manipulation in assembly.
 */

/*
 * If necessary, convert the value in pResult from a local/global reference
 * to an object pointer.
 */
static inline void convertReferenceResult(JNIEnv* env, JValue* pResult,
    const Method* method, Thread* self)
{
#ifdef USE_INDIRECT_REF
    if (method->shorty[0] == 'L' && !dvmCheckException(self) &&
            pResult->l != NULL)
    {
        pResult->l = dvmDecodeIndirectRef(env, pResult->l);
    }
#endif
}

/*
 * General form, handles all cases.
 */
void dvmCallJNIMethod_general(const u4* args, JValue* pResult,
    const Method* method, Thread* self)
{
    int oldStatus;
    u4* modArgs = (u4*) args;
    jclass staticMethodClass;
    JNIEnv* env = self->jniEnv;

    assert(method->insns != NULL);

    //LOGI("JNI calling %p (%s.%s:%s):\n", method->insns,
    //    method->clazz->descriptor, method->name, method->shorty);

#ifdef USE_INDIRECT_REF
    /*
     * Walk the argument list, creating local references for appropriate
     * arguments.
     */
    int idx = 0;
    if (dvmIsStaticMethod(method)) {
        /* add the class object we pass in */
        staticMethodClass = addLocalReference(env, (Object*) method->clazz);
        if (staticMethodClass == NULL) {
            assert(dvmCheckException(self));
            return;
        }
    } else {
        /* add "this" */
        staticMethodClass = NULL;
        jobject thisObj = addLocalReference(env, (Object*) modArgs[0]);
        if (thisObj == NULL) {
            assert(dvmCheckException(self));
            return;
        }
        modArgs[idx] = (u4) thisObj;
        idx = 1;
    }

    const char* shorty = &method->shorty[1];        /* skip return type */
    while (*shorty != '\0') {
        switch (*shorty++) {
        case 'L':
            //LOGI("  local %d: 0x%08x\n", idx, modArgs[idx]);
            if (modArgs[idx] != 0) {
                //if (!dvmIsValidObject((Object*) modArgs[idx]))
                //    dvmAbort();
                jobject argObj = addLocalReference(env, (Object*) modArgs[idx]);
                if (argObj == NULL) {
                    assert(dvmCheckException(self));
                    return;
                }
                modArgs[idx] = (u4) argObj;
            }
            break;
        case 'D':
        case 'J':
            idx++;
            break;
        default:
            /* Z B C S I -- do nothing */
            break;
        }

        idx++;
    }
#else
    staticMethodClass = dvmIsStaticMethod(method) ?
        (jclass) method->clazz : NULL;
#endif

    oldStatus = dvmChangeStatus(self, THREAD_NATIVE);

    COMPUTE_STACK_SUM(self);
    dvmPlatformInvoke(env, staticMethodClass,
        method->jniArgInfo, method->insSize, modArgs, method->shorty,
        (void*)method->insns, pResult);
    CHECK_STACK_SUM(self);

    dvmChangeStatus(self, oldStatus);

    convertReferenceResult(env, pResult, method, self);
}

/*
 * Handler for the unusual case of a synchronized native method.
 *
 * Lock the object, then call through the general function.
 */
void dvmCallJNIMethod_synchronized(const u4* args, JValue* pResult,
    const Method* method, Thread* self)
{
    Object* lockObj;

    assert(dvmIsSynchronizedMethod(method));

    if (dvmIsStaticMethod(method))
        lockObj = (Object*) method->clazz;
    else
        lockObj = (Object*) args[0];

    LOGVV("Calling %s.%s: locking %p (%s)\n",
        method->clazz->descriptor, method->name,
        lockObj, lockObj->clazz->descriptor);

    dvmLockObject(self, lockObj);
    dvmCallJNIMethod_general(args, pResult, method, self);
    dvmUnlockObject(self, lockObj);
}

/*
 * Virtual method call, no reference arguments.
 *
 * We need to local-ref the "this" argument, found in args[0].
 */
void dvmCallJNIMethod_virtualNoRef(const u4* args, JValue* pResult,
    const Method* method, Thread* self)
{
    u4* modArgs = (u4*) args;
    int oldStatus;

#ifdef USE_INDIRECT_REF
    jobject thisObj = addLocalReference(self->jniEnv, (Object*) args[0]);
    if (thisObj == NULL) {
        assert(dvmCheckException(self));
        return;
    }
    modArgs[0] = (u4) thisObj;
#endif

    oldStatus = dvmChangeStatus(self, THREAD_NATIVE);

    COMPUTE_STACK_SUM(self);
    dvmPlatformInvoke(self->jniEnv, NULL,
        method->jniArgInfo, method->insSize, modArgs, method->shorty,
        (void*)method->insns, pResult);
    CHECK_STACK_SUM(self);

    dvmChangeStatus(self, oldStatus);

    convertReferenceResult(self->jniEnv, pResult, method, self);
}

/*
 * Static method call, no reference arguments.
 *
 * We need to local-ref the class reference.
 */
void dvmCallJNIMethod_staticNoRef(const u4* args, JValue* pResult,
    const Method* method, Thread* self)
{
    jclass staticMethodClass;
    int oldStatus;

#ifdef USE_INDIRECT_REF
    staticMethodClass = addLocalReference(self->jniEnv, (Object*)method->clazz);
    if (staticMethodClass == NULL) {
        assert(dvmCheckException(self));
        return;
    }
#else
    staticMethodClass = (jobject) method->clazz;
#endif

    oldStatus = dvmChangeStatus(self, THREAD_NATIVE);

    COMPUTE_STACK_SUM(self);
    dvmPlatformInvoke(self->jniEnv, staticMethodClass,
        method->jniArgInfo, method->insSize, args, method->shorty,
        (void*)method->insns, pResult);
    CHECK_STACK_SUM(self);

    dvmChangeStatus(self, oldStatus);

    convertReferenceResult(self->jniEnv, pResult, method, self);
}

/*
 * Extract the return type enum from the "jniArgInfo" field.
 */
DalvikJniReturnType dvmGetArgInfoReturnType(int jniArgInfo)
{
    return (jniArgInfo & DALVIK_JNI_RETURN_MASK) >> DALVIK_JNI_RETURN_SHIFT;
}


/*
 * ===========================================================================
 *      JNI implementation
 * ===========================================================================
 */

/*
 * Return the version of the native method interface.
 */
static jint GetVersion(JNIEnv* env)
{
    JNI_ENTER();
    /*
     * There is absolutely no need to toggle the mode for correct behavior.
     * However, it does provide native code with a simple "suspend self
     * if necessary" call.
     */
    JNI_EXIT();
    return JNI_VERSION_1_6;
}

/*
 * Create a new class from a bag of bytes.
 *
 * This is not currently supported within Dalvik.
 */
static jclass DefineClass(JNIEnv* env, const char *name, jobject loader,
    const jbyte* buf, jsize bufLen)
{
    UNUSED_PARAMETER(name);
    UNUSED_PARAMETER(loader);
    UNUSED_PARAMETER(buf);
    UNUSED_PARAMETER(bufLen);

    JNI_ENTER();
    LOGW("JNI DefineClass is not supported\n");
    JNI_EXIT();
    return NULL;
}

/*
 * Find a class by name.
 *
 * We have to use the "no init" version of FindClass here, because we might
 * be getting the class prior to registering native methods that will be
 * used in <clinit>.
 *
 * We need to get the class loader associated with the current native
 * method.  If there is no native method, e.g. we're calling this from native
 * code right after creating the VM, the spec says we need to use the class
 * loader returned by "ClassLoader.getBaseClassLoader".  There is no such
 * method, but it's likely they meant ClassLoader.getSystemClassLoader.
 * We can't get that until after the VM has initialized though.
 */
static jclass FindClass(JNIEnv* env, const char* name)
{
    JNI_ENTER();

    const Method* thisMethod;
    ClassObject* clazz;
    jclass jclazz = NULL;
    Object* loader;
    char* descriptor = NULL;

    thisMethod = dvmGetCurrentJNIMethod();
    assert(thisMethod != NULL);

    descriptor = dvmNameToDescriptor(name);
    if (descriptor == NULL) {
        clazz = NULL;
        goto bail;
    }

    //Thread* self = dvmThreadSelf();
    if (_self->classLoaderOverride != NULL) {
        /* hack for JNI_OnLoad */
        assert(strcmp(thisMethod->name, "nativeLoad") == 0);
        loader = _self->classLoaderOverride;
    } else if (thisMethod == gDvm.methFakeNativeEntry) {
        /* start point of invocation interface */
        if (!gDvm.initializing)
            loader = dvmGetSystemClassLoader();
        else
            loader = NULL;
    } else {
        loader = thisMethod->clazz->classLoader;
    }

    clazz = dvmFindClassNoInit(descriptor, loader);
    jclazz = addLocalReference(env, (Object*) clazz);

bail:
    free(descriptor);

    JNI_EXIT();
    return jclazz;
}

/*
 * Return the superclass of a class.
 */
static jclass GetSuperclass(JNIEnv* env, jclass jclazz)
{
    JNI_ENTER();
    jclass jsuper = NULL;

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    if (clazz != NULL)
        jsuper = addLocalReference(env, (Object*)clazz->super);
    JNI_EXIT();
    return jsuper;
}

/*
 * Determine whether an object of clazz1 can be safely cast to clazz2.
 *
 * Like IsInstanceOf, but with a pair of class objects instead of obj+class.
 */
static jboolean IsAssignableFrom(JNIEnv* env, jclass jclazz1, jclass jclazz2)
{
    JNI_ENTER();

    ClassObject* clazz1 = (ClassObject*) dvmDecodeIndirectRef(env, jclazz1);
    ClassObject* clazz2 = (ClassObject*) dvmDecodeIndirectRef(env, jclazz2);

    jboolean result = dvmInstanceof(clazz1, clazz2);

    JNI_EXIT();
    return result;
}

/*
 * Given a java.lang.reflect.Method or .Constructor, return a methodID.
 */
static jmethodID FromReflectedMethod(JNIEnv* env, jobject jmethod)
{
    JNI_ENTER();
    jmethodID methodID;
    Object* method = dvmDecodeIndirectRef(env, jmethod);
    methodID = (jmethodID) dvmGetMethodFromReflectObj(method);
    JNI_EXIT();
    return methodID;
}

/*
 * Given a java.lang.reflect.Field, return a fieldID.
 */
static jfieldID FromReflectedField(JNIEnv* env, jobject jfield)
{
    JNI_ENTER();
    jfieldID fieldID;
    Object* field = dvmDecodeIndirectRef(env, jfield);
    fieldID = (jfieldID) dvmGetFieldFromReflectObj(field);
    JNI_EXIT();
    return fieldID;
}

/*
 * Convert a methodID to a java.lang.reflect.Method or .Constructor.
 *
 * (The "isStatic" field does not appear in the spec.)
 *
 * Throws OutOfMemory and returns NULL on failure.
 */
static jobject ToReflectedMethod(JNIEnv* env, jclass jcls, jmethodID methodID,
    jboolean isStatic)
{
    JNI_ENTER();
    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jcls);
    Object* obj = dvmCreateReflectObjForMethod(clazz, (Method*) methodID);
    dvmReleaseTrackedAlloc(obj, NULL);
    jobject jobj = addLocalReference(env, obj);
    JNI_EXIT();
    return jobj;
}

/*
 * Convert a fieldID to a java.lang.reflect.Field.
 *
 * (The "isStatic" field does not appear in the spec.)
 *
 * Throws OutOfMemory and returns NULL on failure.
 */
static jobject ToReflectedField(JNIEnv* env, jclass jcls, jfieldID fieldID,
    jboolean isStatic)
{
    JNI_ENTER();
    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jcls);
    Object* obj = dvmCreateReflectObjForField(jcls, (Field*) fieldID);
    dvmReleaseTrackedAlloc(obj, NULL);
    jobject jobj = addLocalReference(env, obj);
    JNI_EXIT();
    return jobj;
}

/*
 * Take this exception and throw it.
 */
static jint Throw(JNIEnv* env, jthrowable jobj)
{
    JNI_ENTER();

    jint retval;

    if (jobj != NULL) {
        Object* obj = dvmDecodeIndirectRef(env, jobj);
        dvmSetException(_self, obj);
        retval = JNI_OK;
    } else {
        retval = JNI_ERR;
    }

    JNI_EXIT();
    return retval;
}

/*
 * Constructs an exception object from the specified class with the message
 * specified by "message", and throws it.
 */
static jint ThrowNew(JNIEnv* env, jclass jclazz, const char* message)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    dvmThrowExceptionByClass(clazz, message);
    // TODO: should return failure if this didn't work (e.g. OOM)

    JNI_EXIT();
    return JNI_OK;
}

/*
 * If an exception is being thrown, return the exception object.  Otherwise,
 * return NULL.
 *
 * TODO: if there is no pending exception, we should be able to skip the
 * enter/exit checks.  If we find one, we need to enter and then re-fetch
 * the exception (in case it got moved by a compacting GC).
 */
static jthrowable ExceptionOccurred(JNIEnv* env)
{
    JNI_ENTER();

    Object* exception;
    jobject localException;

    exception = dvmGetException(_self);
    localException = addLocalReference(env, exception);
    if (localException == NULL && exception != NULL) {
        /*
         * We were unable to add a new local reference, and threw a new
         * exception.  We can't return "exception", because it's not a
         * local reference.  So we have to return NULL, indicating that
         * there was no exception, even though it's pretty much raining
         * exceptions in here.
         */
        LOGW("JNI WARNING: addLocal/exception combo\n");
    }

    JNI_EXIT();
    return localException;
}

/*
 * Print an exception and stack trace to stderr.
 */
static void ExceptionDescribe(JNIEnv* env)
{
    JNI_ENTER();

    Object* exception = dvmGetException(_self);
    if (exception != NULL) {
        dvmPrintExceptionStackTrace();
    } else {
        LOGI("Odd: ExceptionDescribe called, but no exception pending\n");
    }

    JNI_EXIT();
}

/*
 * Clear the exception currently being thrown.
 *
 * TODO: we should be able to skip the enter/exit stuff.
 */
static void ExceptionClear(JNIEnv* env)
{
    JNI_ENTER();
    dvmClearException(_self);
    JNI_EXIT();
}

/*
 * Kill the VM.  This function does not return.
 */
static void FatalError(JNIEnv* env, const char* msg)
{
    //dvmChangeStatus(NULL, THREAD_RUNNING);
    LOGE("JNI posting fatal error: %s\n", msg);
    dvmAbort();
}

/*
 * Push a new JNI frame on the stack, with a new set of locals.
 *
 * The new frame must have the same method pointer.  (If for no other
 * reason than FindClass needs it to get the appropriate class loader.)
 */
static jint PushLocalFrame(JNIEnv* env, jint capacity)
{
    JNI_ENTER();
    int result = JNI_OK;
    if (!ensureLocalCapacity(env, capacity) ||
        !dvmPushLocalFrame(_self /*dvmThreadSelf()*/, dvmGetCurrentJNIMethod()))
    {
        /* yes, OutOfMemoryError, not StackOverflowError */
        dvmClearException(_self);
        dvmThrowException("Ljava/lang/OutOfMemoryError;",
            "out of stack in JNI PushLocalFrame");
        result = JNI_ERR;
    }
    JNI_EXIT();
    return result;
}

/*
 * Pop the local frame off.  If "result" is not null, add it as a
 * local reference on the now-current frame.
 */
static jobject PopLocalFrame(JNIEnv* env, jobject jresult)
{
    JNI_ENTER();
    Object* result = dvmDecodeIndirectRef(env, jresult);
    if (!dvmPopLocalFrame(_self /*dvmThreadSelf()*/)) {
        LOGW("JNI WARNING: too many PopLocalFrame calls\n");
        dvmClearException(_self);
        dvmThrowException("Ljava/lang/RuntimeException;",
            "too many PopLocalFrame calls");
    }
    jresult = addLocalReference(env, result);
    JNI_EXIT();
    return result;
}

/*
 * Add a reference to the global list.
 */
static jobject NewGlobalRef(JNIEnv* env, jobject jobj)
{
    Object* obj;

    JNI_ENTER();
    if (dvmIsWeakGlobalRef(jobj))
        obj = getPhantomReferent(env, (jweak) jobj);
    else
        obj = dvmDecodeIndirectRef(env, jobj);
    jobject retval = addGlobalReference(obj);
    JNI_EXIT();
    return retval;
}

/*
 * Delete a reference from the global list.
 */
static void DeleteGlobalRef(JNIEnv* env, jobject jglobalRef)
{
    JNI_ENTER();
    deleteGlobalReference(jglobalRef);
    JNI_EXIT();
}


/*
 * Add a reference to the local list.
 */
static jobject NewLocalRef(JNIEnv* env, jobject jobj)
{
    Object* obj;

    JNI_ENTER();
    if (dvmIsWeakGlobalRef(jobj))
        obj = getPhantomReferent(env, (jweak) jobj);
    else
        obj = dvmDecodeIndirectRef(env, jobj);
    jobject retval = addLocalReference(env, obj);
    JNI_EXIT();
    return retval;
}

/*
 * Delete a reference from the local list.
 */
static void DeleteLocalRef(JNIEnv* env, jobject jlocalRef)
{
    JNI_ENTER();
    deleteLocalReference(env, jlocalRef);
    JNI_EXIT();
}

/*
 * Ensure that the local references table can hold at least this many
 * references.
 */
static jint EnsureLocalCapacity(JNIEnv* env, jint capacity)
{
    JNI_ENTER();
    bool okay = ensureLocalCapacity(env, capacity);
    if (!okay) {
        dvmThrowException("Ljava/lang/OutOfMemoryError;",
            "can't ensure local reference capacity");
    }
    JNI_EXIT();
    if (okay)
        return 0;
    else
        return -1;
}


/*
 * Determine whether two Object references refer to the same underlying object.
 */
static jboolean IsSameObject(JNIEnv* env, jobject jref1, jobject jref2)
{
    JNI_ENTER();
    Object* obj1 = dvmDecodeIndirectRef(env, jref1);
    Object* obj2 = dvmDecodeIndirectRef(env, jref2);
    jboolean result = (obj1 == obj2);
    JNI_EXIT();
    return result;
}

/*
 * Allocate a new object without invoking any constructors.
 */
static jobject AllocObject(JNIEnv* env, jclass jclazz)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jobject result;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
        result = NULL;
    } else {
        Object* newObj = dvmAllocObject(clazz, ALLOC_DONT_TRACK);
        result = addLocalReference(env, newObj);
    }

    JNI_EXIT();
    return result;
}

/*
 * Allocate a new object and invoke the supplied constructor.
 */
static jobject NewObject(JNIEnv* env, jclass jclazz, jmethodID methodID, ...)
{
    JNI_ENTER();
    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jobject result;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
        result = NULL;
    } else {
        Object* newObj = dvmAllocObject(clazz, ALLOC_DONT_TRACK);
        result = addLocalReference(env, newObj);
        if (newObj != NULL) {
            JValue unused;
            va_list args;
            va_start(args, methodID);
            dvmCallMethodV(_self, (Method*) methodID, newObj, true, &unused,
                args);
            va_end(args);
        }
    }

    JNI_EXIT();
    return result;
}
static jobject NewObjectV(JNIEnv* env, jclass jclazz, jmethodID methodID,
    va_list args)
{
    JNI_ENTER();
    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jobject result;

    Object* newObj = dvmAllocObject(clazz, ALLOC_DONT_TRACK);
    result = addLocalReference(env, newObj);
    if (newObj != NULL) {
        JValue unused;
        dvmCallMethodV(_self, (Method*) methodID, newObj, true, &unused, args);
    }

    JNI_EXIT();
    return result;
}
static jobject NewObjectA(JNIEnv* env, jclass jclazz, jmethodID methodID,
    jvalue* args)
{
    JNI_ENTER();
    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jobject result;

    Object* newObj = dvmAllocObject(clazz, ALLOC_DONT_TRACK);
    result = addLocalReference(env, newObj);
    if (newObj != NULL) {
        JValue unused;
        dvmCallMethodA(_self, (Method*) methodID, newObj, true, &unused, args);
    }

    JNI_EXIT();
    return result;
}

/*
 * Returns the class of an object.
 *
 * JNI spec says: obj must not be NULL.
 */
static jclass GetObjectClass(JNIEnv* env, jobject jobj)
{
    JNI_ENTER();

    assert(jobj != NULL);

    Object* obj = dvmDecodeIndirectRef(env, jobj);
    jclass jclazz = addLocalReference(env, (Object*) obj->clazz);

    JNI_EXIT();
    return jclazz;
}

/*
 * Determine whether "obj" is an instance of "clazz".
 */
static jboolean IsInstanceOf(JNIEnv* env, jobject jobj, jclass jclazz)
{
    JNI_ENTER();

    assert(jclazz != NULL);

    jboolean result;

    if (jobj == NULL) {
        result = true;
    } else {
        Object* obj = dvmDecodeIndirectRef(env, jobj);
        ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
        result = dvmInstanceof(obj->clazz, clazz);
    }

    JNI_EXIT();
    return result;
}

/*
 * Get a method ID for an instance method.
 *
 * JNI defines <init> as an instance method, but Dalvik considers it a
 * "direct" method, so we have to special-case it here.
 *
 * Dalvik also puts all private methods into the "direct" list, so we
 * really need to just search both lists.
 */
static jmethodID GetMethodID(JNIEnv* env, jclass jclazz, const char* name,
    const char* sig)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jmethodID id = NULL;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
    } else {
        Method* meth;

        meth = dvmFindVirtualMethodHierByDescriptor(clazz, name, sig);
        if (meth == NULL) {
            /* search private methods and constructors; non-hierarchical */
            meth = dvmFindDirectMethodByDescriptor(clazz, name, sig);
        }
        if (meth != NULL && dvmIsStaticMethod(meth)) {
            IF_LOGD() {
                char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
                LOGD("GetMethodID: not returning static method %s.%s %s\n",
                    clazz->descriptor, meth->name, desc);
                free(desc);
            }
            meth = NULL;
        }
        if (meth == NULL) {
            LOGD("GetMethodID: method not found: %s.%s:%s\n",
                clazz->descriptor, name, sig);
            dvmThrowException("Ljava/lang/NoSuchMethodError;", name);
        }

        /*
         * The method's class may not be the same as clazz, but if
         * it isn't this must be a virtual method and the class must
         * be a superclass (and, hence, already initialized).
         */
        if (meth != NULL) {
            assert(dvmIsClassInitialized(meth->clazz) ||
                   dvmIsClassInitializing(meth->clazz));
        }
        id = (jmethodID) meth;
    }
    JNI_EXIT();
    return id;
}

/*
 * Get a field ID (instance fields).
 */
static jfieldID GetFieldID(JNIEnv* env, jclass jclazz,
    const char* name, const char* sig)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jfieldID id;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
        id = NULL;
    } else {
        id = (jfieldID) dvmFindInstanceFieldHier(clazz, name, sig);
        if (id == NULL) {
            LOGD("GetFieldID: unable to find field %s.%s:%s\n",
                clazz->descriptor, name, sig);
            dvmThrowException("Ljava/lang/NoSuchFieldError;", name);
        }
    }
    JNI_EXIT();
    return id;
}

/*
 * Get the method ID for a static method in a class.
 */
static jmethodID GetStaticMethodID(JNIEnv* env, jclass jclazz,
    const char* name, const char* sig)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jmethodID id;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
        id = NULL;
    } else {
        Method* meth;

        meth = dvmFindDirectMethodHierByDescriptor(clazz, name, sig);

        /* make sure it's static, not virtual+private */
        if (meth != NULL && !dvmIsStaticMethod(meth)) {
            IF_LOGD() {
                char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
                LOGD("GetStaticMethodID: "
                    "not returning nonstatic method %s.%s %s\n",
                    clazz->descriptor, meth->name, desc);
                free(desc);
            }
            meth = NULL;
        }

        id = (jmethodID) meth;
        if (id == NULL)
            dvmThrowException("Ljava/lang/NoSuchMethodError;", name);
    }

    JNI_EXIT();
    return id;
}

/*
 * Get a field ID (static fields).
 */
static jfieldID GetStaticFieldID(JNIEnv* env, jclass jclazz,
    const char* name, const char* sig)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jfieldID id;

    if (!dvmIsClassInitialized(clazz) && !dvmInitClass(clazz)) {
        assert(dvmCheckException(_self));
        id = NULL;
    } else {
        id = (jfieldID) dvmFindStaticField(clazz, name, sig);
        if (id == NULL)
            dvmThrowException("Ljava/lang/NoSuchFieldError;", name);
    }
    JNI_EXIT();
    return id;
}

/*
 * Get a static field.
 *
 * If we get an object reference, add it to the local refs list.
 */
#define GET_STATIC_TYPE_FIELD(_ctype, _jname, _isref)                       \
    static _ctype GetStatic##_jname##Field(JNIEnv* env, jclass jclazz,      \
        jfieldID fieldID)                                                   \
    {                                                                       \
        UNUSED_PARAMETER(jclazz);                                           \
        JNI_ENTER();                                                        \
        StaticField* sfield = (StaticField*) fieldID;                       \
        _ctype value;                                                       \
        if (_isref) {   /* only when _ctype==jobject */                     \
            Object* obj = dvmGetStaticFieldObject(sfield);                  \
            value = (_ctype)(u4)addLocalReference(env, obj);                \
        } else {                                                            \
            value = dvmGetStaticField##_jname(sfield);                      \
        }                                                                   \
        JNI_EXIT();                                                         \
        return value;                                                       \
    }
GET_STATIC_TYPE_FIELD(jobject, Object, true);
GET_STATIC_TYPE_FIELD(jboolean, Boolean, false);
GET_STATIC_TYPE_FIELD(jbyte, Byte, false);
GET_STATIC_TYPE_FIELD(jchar, Char, false);
GET_STATIC_TYPE_FIELD(jshort, Short, false);
GET_STATIC_TYPE_FIELD(jint, Int, false);
GET_STATIC_TYPE_FIELD(jlong, Long, false);
GET_STATIC_TYPE_FIELD(jfloat, Float, false);
GET_STATIC_TYPE_FIELD(jdouble, Double, false);

/*
 * Set a static field.
 */
#define SET_STATIC_TYPE_FIELD(_ctype, _jname, _isref)                       \
    static void SetStatic##_jname##Field(JNIEnv* env, jclass jclazz,        \
        jfieldID fieldID, _ctype value)                                     \
    {                                                                       \
        UNUSED_PARAMETER(jclazz);                                           \
        JNI_ENTER();                                                        \
        StaticField* sfield = (StaticField*) fieldID;                       \
        if (_isref) {   /* only when _ctype==jobject */                     \
            Object* valObj = dvmDecodeIndirectRef(env, (jobject)(u4)value); \
            dvmSetStaticFieldObject(sfield, valObj);                        \
        } else {                                                            \
            dvmSetStaticField##_jname(sfield, value);                       \
        }                                                                   \
        JNI_EXIT();                                                         \
    }
SET_STATIC_TYPE_FIELD(jobject, Object, true);
SET_STATIC_TYPE_FIELD(jboolean, Boolean, false);
SET_STATIC_TYPE_FIELD(jbyte, Byte, false);
SET_STATIC_TYPE_FIELD(jchar, Char, false);
SET_STATIC_TYPE_FIELD(jshort, Short, false);
SET_STATIC_TYPE_FIELD(jint, Int, false);
SET_STATIC_TYPE_FIELD(jlong, Long, false);
SET_STATIC_TYPE_FIELD(jfloat, Float, false);
SET_STATIC_TYPE_FIELD(jdouble, Double, false);

/*
 * Get an instance field.
 *
 * If we get an object reference, add it to the local refs list.
 */
#define GET_TYPE_FIELD(_ctype, _jname, _isref)                              \
    static _ctype Get##_jname##Field(JNIEnv* env, jobject jobj,             \
        jfieldID fieldID)                                                   \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        InstField* field = (InstField*) fieldID;                            \
        _ctype value;                                                       \
        if (_isref) {   /* only when _ctype==jobject */                     \
            Object* valObj = dvmGetFieldObject(obj, field->byteOffset);     \
            value = (_ctype)(u4)addLocalReference(env, valObj);             \
        } else {                                                            \
            value = dvmGetField##_jname(obj, field->byteOffset);            \
        }                                                                   \
        JNI_EXIT();                                                         \
        return value;                                                       \
    }
GET_TYPE_FIELD(jobject, Object, true);
GET_TYPE_FIELD(jboolean, Boolean, false);
GET_TYPE_FIELD(jbyte, Byte, false);
GET_TYPE_FIELD(jchar, Char, false);
GET_TYPE_FIELD(jshort, Short, false);
GET_TYPE_FIELD(jint, Int, false);
GET_TYPE_FIELD(jlong, Long, false);
GET_TYPE_FIELD(jfloat, Float, false);
GET_TYPE_FIELD(jdouble, Double, false);

/*
 * Set an instance field.
 */
#define SET_TYPE_FIELD(_ctype, _jname, _isref)                              \
    static void Set##_jname##Field(JNIEnv* env, jobject jobj,               \
        jfieldID fieldID, _ctype value)                                     \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        InstField* field = (InstField*) fieldID;                            \
        if (_isref) {   /* only when _ctype==jobject */                     \
            Object* valObj = dvmDecodeIndirectRef(env, (jobject)(u4)value); \
            dvmSetFieldObject(obj, field->byteOffset, valObj);              \
        } else {                                                            \
            dvmSetField##_jname(obj, field->byteOffset, value);             \
        }                                                                   \
        JNI_EXIT();                                                         \
    }
SET_TYPE_FIELD(jobject, Object, true);
SET_TYPE_FIELD(jboolean, Boolean, false);
SET_TYPE_FIELD(jbyte, Byte, false);
SET_TYPE_FIELD(jchar, Char, false);
SET_TYPE_FIELD(jshort, Short, false);
SET_TYPE_FIELD(jint, Int, false);
SET_TYPE_FIELD(jlong, Long, false);
SET_TYPE_FIELD(jfloat, Float, false);
SET_TYPE_FIELD(jdouble, Double, false);

/*
 * Make a virtual method call.
 *
 * Three versions (..., va_list, jvalue[]) for each return type.  If we're
 * returning an Object, we have to add it to the local references table.
 */
#define CALL_VIRTUAL(_ctype, _jname, _retfail, _retok, _isref)              \
    static _ctype Call##_jname##Method(JNIEnv* env, jobject jobj,           \
        jmethodID methodID, ...)                                            \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        const Method* meth;                                                 \
        va_list args;                                                       \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(obj->clazz, (Method*)methodID);      \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        va_start(args, methodID);                                           \
        dvmCallMethodV(_self, meth, obj, true, &result, args);              \
        va_end(args);                                                       \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype Call##_jname##MethodV(JNIEnv* env, jobject jobj,          \
        jmethodID methodID, va_list args)                                   \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        const Method* meth;                                                 \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(obj->clazz, (Method*)methodID);      \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        dvmCallMethodV(_self, meth, obj, true, &result, args);              \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype Call##_jname##MethodA(JNIEnv* env, jobject jobj,          \
        jmethodID methodID, jvalue* args)                                   \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        const Method* meth;                                                 \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(obj->clazz, (Method*)methodID);      \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        dvmCallMethodA(_self, meth, obj, true, &result, args);              \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }
CALL_VIRTUAL(jobject, Object, NULL, result.l, true);
CALL_VIRTUAL(jboolean, Boolean, 0, result.z, false);
CALL_VIRTUAL(jbyte, Byte, 0, result.b, false);
CALL_VIRTUAL(jchar, Char, 0, result.c, false);
CALL_VIRTUAL(jshort, Short, 0, result.s, false);
CALL_VIRTUAL(jint, Int, 0, result.i, false);
CALL_VIRTUAL(jlong, Long, 0, result.j, false);
CALL_VIRTUAL(jfloat, Float, 0.0f, result.f, false);
CALL_VIRTUAL(jdouble, Double, 0.0, result.d, false);
CALL_VIRTUAL(void, Void, , , false);

/*
 * Make a "non-virtual" method call.  We're still calling a virtual method,
 * but this time we're not doing an indirection through the object's vtable.
 * The "clazz" parameter defines which implementation of a method we want.
 *
 * Three versions (..., va_list, jvalue[]) for each return type.
 */
#define CALL_NONVIRTUAL(_ctype, _jname, _retfail, _retok, _isref)           \
    static _ctype CallNonvirtual##_jname##Method(JNIEnv* env, jobject jobj, \
        jclass jclazz, jmethodID methodID, ...)                             \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        ClassObject* clazz =                                                \
            (ClassObject*) dvmDecodeIndirectRef(env, jclazz);               \
        const Method* meth;                                                 \
        va_list args;                                                       \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(clazz, (Method*)methodID);           \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        va_start(args, methodID);                                           \
        dvmCallMethodV(_self, meth, obj, true, &result, args);              \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        va_end(args);                                                       \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype CallNonvirtual##_jname##MethodV(JNIEnv* env, jobject jobj,\
        jclass jclazz, jmethodID methodID, va_list args)                    \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        ClassObject* clazz =                                                \
            (ClassObject*) dvmDecodeIndirectRef(env, jclazz);               \
        const Method* meth;                                                 \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(clazz, (Method*)methodID);           \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        dvmCallMethodV(_self, meth, obj, true, &result, args);              \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype CallNonvirtual##_jname##MethodA(JNIEnv* env, jobject jobj,\
        jclass jclazz, jmethodID methodID, jvalue* args)                    \
    {                                                                       \
        JNI_ENTER();                                                        \
        Object* obj = dvmDecodeIndirectRef(env, jobj);                      \
        ClassObject* clazz =                                                \
            (ClassObject*) dvmDecodeIndirectRef(env, jclazz);               \
        const Method* meth;                                                 \
        JValue result;                                                      \
        meth = dvmGetVirtualizedMethod(clazz, (Method*)methodID);           \
        if (meth == NULL) {                                                 \
            JNI_EXIT();                                                     \
            return _retfail;                                                \
        }                                                                   \
        dvmCallMethodA(_self, meth, obj, true, &result, args);              \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }
CALL_NONVIRTUAL(jobject, Object, NULL, result.l, true);
CALL_NONVIRTUAL(jboolean, Boolean, 0, result.z, false);
CALL_NONVIRTUAL(jbyte, Byte, 0, result.b, false);
CALL_NONVIRTUAL(jchar, Char, 0, result.c, false);
CALL_NONVIRTUAL(jshort, Short, 0, result.s, false);
CALL_NONVIRTUAL(jint, Int, 0, result.i, false);
CALL_NONVIRTUAL(jlong, Long, 0, result.j, false);
CALL_NONVIRTUAL(jfloat, Float, 0.0f, result.f, false);
CALL_NONVIRTUAL(jdouble, Double, 0.0, result.d, false);
CALL_NONVIRTUAL(void, Void, , , false);


/*
 * Call a static method.
 */
#define CALL_STATIC(_ctype, _jname, _retfail, _retok, _isref)               \
    static _ctype CallStatic##_jname##Method(JNIEnv* env, jclass jclazz,    \
        jmethodID methodID, ...)                                            \
    {                                                                       \
        UNUSED_PARAMETER(jclazz);                                           \
        JNI_ENTER();                                                        \
        JValue result;                                                      \
        va_list args;                                                       \
        va_start(args, methodID);                                           \
        dvmCallMethodV(_self, (Method*)methodID, NULL, true, &result, args);\
        va_end(args);                                                       \
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype CallStatic##_jname##MethodV(JNIEnv* env, jclass jclazz,   \
        jmethodID methodID, va_list args)                                   \
    {                                                                       \
        UNUSED_PARAMETER(jclazz);                                           \
        JNI_ENTER();                                                        \
        JValue result;                                                      \
        dvmCallMethodV(_self, (Method*)methodID, NULL, true, &result, args);\
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }                                                                       \
    static _ctype CallStatic##_jname##MethodA(JNIEnv* env, jclass jclazz,   \
        jmethodID methodID, jvalue* args)                                   \
    {                                                                       \
        UNUSED_PARAMETER(jclazz);                                           \
        JNI_ENTER();                                                        \
        JValue result;                                                      \
        dvmCallMethodA(_self, (Method*)methodID, NULL, true, &result, args);\
        if (_isref && !dvmCheckException(_self))                            \
            result.l = addLocalReference(env, result.l);                    \
        JNI_EXIT();                                                         \
        return _retok;                                                      \
    }
CALL_STATIC(jobject, Object, NULL, result.l, true);
CALL_STATIC(jboolean, Boolean, 0, result.z, false);
CALL_STATIC(jbyte, Byte, 0, result.b, false);
CALL_STATIC(jchar, Char, 0, result.c, false);
CALL_STATIC(jshort, Short, 0, result.s, false);
CALL_STATIC(jint, Int, 0, result.i, false);
CALL_STATIC(jlong, Long, 0, result.j, false);
CALL_STATIC(jfloat, Float, 0.0f, result.f, false);
CALL_STATIC(jdouble, Double, 0.0, result.d, false);
CALL_STATIC(void, Void, , , false);

/*
 * Create a new String from Unicode data.
 *
 * If "len" is zero, we will return an empty string even if "unicodeChars"
 * is NULL.  (The JNI spec is vague here.)
 */
static jstring NewString(JNIEnv* env, const jchar* unicodeChars, jsize len)
{
    JNI_ENTER();
    jobject retval;

    StringObject* jstr = dvmCreateStringFromUnicode(unicodeChars, len);
    if (jstr == NULL) {
        retval = NULL;
    } else {
        dvmReleaseTrackedAlloc((Object*) jstr, NULL);
        retval = addLocalReference(env, (Object*) jstr);
    }

    JNI_EXIT();
    return retval;
}

/*
 * Return the length of a String in Unicode character units.
 */
static jsize GetStringLength(JNIEnv* env, jstring jstr)
{
    JNI_ENTER();

    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    jsize len = dvmStringLen(strObj);

    JNI_EXIT();
    return len;
}


/*
 * Get a string's character data.
 *
 * The result is guaranteed to be valid until ReleaseStringChars is
 * called, which means we have to pin it or return a copy.
 */
static const jchar* GetStringChars(JNIEnv* env, jstring jstr, jboolean* isCopy)
{
    JNI_ENTER();

    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    ArrayObject* strChars = dvmStringCharArray(strObj);

    pinPrimitiveArray(strChars);

    const u2* data = dvmStringChars(strObj);
    if (isCopy != NULL)
        *isCopy = JNI_FALSE;

    JNI_EXIT();
    return (jchar*)data;
}

/*
 * Release our grip on some characters from a string.
 */
static void ReleaseStringChars(JNIEnv* env, jstring jstr, const jchar* chars)
{
    JNI_ENTER();
    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    ArrayObject* strChars = dvmStringCharArray(strObj);
    unpinPrimitiveArray(strChars);
    JNI_EXIT();
}

/*
 * Create a new java.lang.String object from chars in modified UTF-8 form.
 *
 * The spec doesn't say how to handle a NULL string.  Popular desktop VMs
 * accept it and return a NULL pointer in response.
 */
static jstring NewStringUTF(JNIEnv* env, const char* bytes)
{
    JNI_ENTER();

    jstring result;

    if (bytes == NULL) {
        result = NULL;
    } else {
        /* note newStr could come back NULL on OOM */
        StringObject* newStr = dvmCreateStringFromCstr(bytes, ALLOC_DEFAULT);
        result = addLocalReference(env, (Object*) newStr);
        dvmReleaseTrackedAlloc((Object*)newStr, NULL);
    }

    JNI_EXIT();
    return result;
}

/*
 * Return the length in bytes of the modified UTF-8 form of the string.
 */
static jsize GetStringUTFLength(JNIEnv* env, jstring jstr)
{
    JNI_ENTER();

    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    jsize len = dvmStringUtf8ByteLen(strObj);

    JNI_EXIT();
    return len;
}

/*
 * Convert "string" to modified UTF-8 and return a pointer.  The returned
 * value must be released with ReleaseStringUTFChars.
 *
 * According to the JNI reference, "Returns a pointer to a UTF-8 string,
 * or NULL if the operation fails. Returns NULL if and only if an invocation
 * of this function has thrown an exception."
 *
 * The behavior here currently follows that of other open-source VMs, which
 * quietly return NULL if "string" is NULL.  We should consider throwing an
 * NPE.  (The CheckJNI code blows up if you try to pass in a NULL string,
 * which should catch this sort of thing during development.)  Certain other
 * VMs will crash with a segmentation fault.
 */
static const char* GetStringUTFChars(JNIEnv* env, jstring jstr,
    jboolean* isCopy)
{
    JNI_ENTER();
    char* newStr;

    if (jstr == NULL) {
        /* this shouldn't happen; throw NPE? */
        newStr = NULL;
    } else {
        if (isCopy != NULL)
            *isCopy = JNI_TRUE;

        StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
        newStr = dvmCreateCstrFromString(strObj);
        if (newStr == NULL) {
            /* assume memory failure */
            dvmThrowException("Ljava/lang/OutOfMemoryError;",
                "native heap string alloc failed");
        }
    }

    JNI_EXIT();
    return newStr;
}

/*
 * Release a string created by GetStringUTFChars().
 */
static void ReleaseStringUTFChars(JNIEnv* env, jstring jstr, const char* utf)
{
    JNI_ENTER();
    free((char*)utf);
    JNI_EXIT();
}

/*
 * Return the capacity of the array.
 */
static jsize GetArrayLength(JNIEnv* env, jarray jarr)
{
    JNI_ENTER();

    ArrayObject* arrObj = (ArrayObject*) dvmDecodeIndirectRef(env, jarr);
    jsize length = arrObj->length;

    JNI_EXIT();
    return length;
}

/*
 * Construct a new array that holds objects from class "elementClass".
 */
static jobjectArray NewObjectArray(JNIEnv* env, jsize length,
    jclass jelementClass, jobject jinitialElement)
{
    JNI_ENTER();

    jobjectArray newArray = NULL;
    ClassObject* elemClassObj =
        (ClassObject*) dvmDecodeIndirectRef(env, jelementClass);

    if (elemClassObj == NULL) {
        dvmThrowException("Ljava/lang/NullPointerException;",
            "JNI NewObjectArray");
        goto bail;
    }

    ArrayObject* newObj =
        dvmAllocObjectArray(elemClassObj, length, ALLOC_DEFAULT);
    if (newObj == NULL) {
        assert(dvmCheckException(_self));
        goto bail;
    }
    newArray = addLocalReference(env, (Object*) newObj);
    dvmReleaseTrackedAlloc((Object*) newObj, NULL);

    /*
     * Initialize the array.  Trashes "length".
     */
    if (jinitialElement != NULL) {
        Object* initialElement = dvmDecodeIndirectRef(env, jinitialElement);
        Object** arrayData = (Object**) newObj->contents;

        while (length--)
            *arrayData++ = initialElement;
    }


bail:
    JNI_EXIT();
    return newArray;
}

/*
 * Get one element of an Object array.
 *
 * Add the object to the local references table in case the array goes away.
 */
static jobject GetObjectArrayElement(JNIEnv* env, jobjectArray jarr,
    jsize index)
{
    JNI_ENTER();

    ArrayObject* arrayObj = (ArrayObject*) dvmDecodeIndirectRef(env, jarr);
    jobject retval = NULL;

    assert(arrayObj != NULL);

    /* check the array bounds */
    if (index < 0 || index >= (int) arrayObj->length) {
        dvmThrowException("Ljava/lang/ArrayIndexOutOfBoundsException;",
            arrayObj->obj.clazz->descriptor);
        goto bail;
    }

    Object* value = ((Object**) arrayObj->contents)[index];
    retval = addLocalReference(env, value);

bail:
    JNI_EXIT();
    return retval;
}

/*
 * Set one element of an Object array.
 */
static void SetObjectArrayElement(JNIEnv* env, jobjectArray jarr,
    jsize index, jobject jobj)
{
    JNI_ENTER();

    ArrayObject* arrayObj = (ArrayObject*) dvmDecodeIndirectRef(env, jarr);

    assert(arrayObj != NULL);

    /* check the array bounds */
    if (index < 0 || index >= (int) arrayObj->length) {
        dvmThrowException("Ljava/lang/ArrayIndexOutOfBoundsException;",
            arrayObj->obj.clazz->descriptor);
        goto bail;
    }

    //LOGV("JNI: set element %d in array %p to %p\n", index, array, value);

    Object* obj = dvmDecodeIndirectRef(env, jobj);
    ((Object**) arrayObj->contents)[index] = obj;

bail:
    JNI_EXIT();
}

/*
 * Create a new array of primitive elements.
 */
#define NEW_PRIMITIVE_ARRAY(_artype, _jname, _typechar)                     \
    static _artype New##_jname##Array(JNIEnv* env, jsize length)            \
    {                                                                       \
        JNI_ENTER();                                                        \
        ArrayObject* arrayObj;                                              \
        arrayObj = dvmAllocPrimitiveArray(_typechar, length,                \
            ALLOC_DEFAULT);                                                 \
        jarray jarr = NULL;                                                 \
        if (arrayObj != NULL) {                                             \
            jarr = addLocalReference(env, (Object*) arrayObj);              \
            dvmReleaseTrackedAlloc((Object*) arrayObj, NULL);               \
        }                                                                   \
        JNI_EXIT();                                                         \
        return (_artype)jarr;                                               \
    }
NEW_PRIMITIVE_ARRAY(jbooleanArray, Boolean, 'Z');
NEW_PRIMITIVE_ARRAY(jbyteArray, Byte, 'B');
NEW_PRIMITIVE_ARRAY(jcharArray, Char, 'C');
NEW_PRIMITIVE_ARRAY(jshortArray, Short, 'S');
NEW_PRIMITIVE_ARRAY(jintArray, Int, 'I');
NEW_PRIMITIVE_ARRAY(jlongArray, Long, 'J');
NEW_PRIMITIVE_ARRAY(jfloatArray, Float, 'F');
NEW_PRIMITIVE_ARRAY(jdoubleArray, Double, 'D');

/*
 * Get a pointer to a C array of primitive elements from an array object
 * of the matching type.
 *
 * In a compacting GC, we either need to return a copy of the elements or
 * "pin" the memory.  Otherwise we run the risk of native code using the
 * buffer as the destination of e.g. a blocking read() call that wakes up
 * during a GC.
 */
#define GET_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                        \
    static _ctype* Get##_jname##ArrayElements(JNIEnv* env,                  \
        _ctype##Array jarr, jboolean* isCopy)                               \
    {                                                                       \
        JNI_ENTER();                                                        \
        _ctype* data;                                                       \
        ArrayObject* arrayObj =                                             \
            (ArrayObject*) dvmDecodeIndirectRef(env, jarr);                 \
        pinPrimitiveArray(arrayObj);                                        \
        data = (_ctype*) arrayObj->contents;                                \
        if (isCopy != NULL)                                                 \
            *isCopy = JNI_FALSE;                                            \
        JNI_EXIT();                                                         \
        return data;                                                        \
    }

/*
 * Release the storage locked down by the "get" function.
 *
 * The spec says, "'mode' has no effect if 'elems' is not a copy of the
 * elements in 'array'."  They apparently did not anticipate the need to
 * un-pin memory.
 */
#define RELEASE_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                    \
    static void Release##_jname##ArrayElements(JNIEnv* env,                 \
        _ctype##Array jarr, _ctype* elems, jint mode)                       \
    {                                                                       \
        UNUSED_PARAMETER(elems);                                            \
        JNI_ENTER();                                                        \
        if (mode != JNI_COMMIT) {                                           \
            ArrayObject* arrayObj =                                         \
                (ArrayObject*) dvmDecodeIndirectRef(env, jarr);             \
            unpinPrimitiveArray(arrayObj);                                  \
        }                                                                   \
        JNI_EXIT();                                                         \
    }

/*
 * Copy a section of a primitive array to a buffer.
 */
#define GET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                          \
    static void Get##_jname##ArrayRegion(JNIEnv* env,                       \
        _ctype##Array jarr, jsize start, jsize len, _ctype* buf)            \
    {                                                                       \
        JNI_ENTER();                                                        \
        ArrayObject* arrayObj =                                             \
            (ArrayObject*) dvmDecodeIndirectRef(env, jarr);                 \
        _ctype* data = (_ctype*) arrayObj->contents;                        \
        if (start < 0 || len < 0 || start + len > (int) arrayObj->length) { \
            dvmThrowException("Ljava/lang/ArrayIndexOutOfBoundsException;", \
                arrayObj->obj.clazz->descriptor);                           \
        } else {                                                            \
            memcpy(buf, data + start, len * sizeof(_ctype));                \
        }                                                                   \
        JNI_EXIT();                                                         \
    }

/*
 * Copy a section of a primitive array to a buffer.
 */
#define SET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                          \
    static void Set##_jname##ArrayRegion(JNIEnv* env,                       \
        _ctype##Array jarr, jsize start, jsize len, const _ctype* buf)      \
    {                                                                       \
        JNI_ENTER();                                                        \
        ArrayObject* arrayObj =                                             \
            (ArrayObject*) dvmDecodeIndirectRef(env, jarr);                 \
        _ctype* data = (_ctype*) arrayObj->contents;                        \
        if (start < 0 || len < 0 || start + len > (int) arrayObj->length) { \
            dvmThrowException("Ljava/lang/ArrayIndexOutOfBoundsException;", \
                arrayObj->obj.clazz->descriptor);                           \
        } else {                                                            \
            memcpy(data + start, buf, len * sizeof(_ctype));                \
        }                                                                   \
        JNI_EXIT();                                                         \
    }

/*
 * 4-in-1:
 *  Get<Type>ArrayElements
 *  Release<Type>ArrayElements
 *  Get<Type>ArrayRegion
 *  Set<Type>ArrayRegion
 */
#define PRIMITIVE_ARRAY_FUNCTIONS(_ctype, _jname)                           \
    GET_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname);                           \
    RELEASE_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname);                       \
    GET_PRIMITIVE_ARRAY_REGION(_ctype, _jname);                             \
    SET_PRIMITIVE_ARRAY_REGION(_ctype, _jname);

PRIMITIVE_ARRAY_FUNCTIONS(jboolean, Boolean);
PRIMITIVE_ARRAY_FUNCTIONS(jbyte, Byte);
PRIMITIVE_ARRAY_FUNCTIONS(jchar, Char);
PRIMITIVE_ARRAY_FUNCTIONS(jshort, Short);
PRIMITIVE_ARRAY_FUNCTIONS(jint, Int);
PRIMITIVE_ARRAY_FUNCTIONS(jlong, Long);
PRIMITIVE_ARRAY_FUNCTIONS(jfloat, Float);
PRIMITIVE_ARRAY_FUNCTIONS(jdouble, Double);

/*
 * Register one or more native functions in one class.
 */
static jint RegisterNatives(JNIEnv* env, jclass jclazz,
    const JNINativeMethod* methods, jint nMethods)
{
    JNI_ENTER();

    ClassObject* clazz = (ClassObject*) dvmDecodeIndirectRef(env, jclazz);
    jint retval = JNI_OK;
    int i;

    if (gDvm.verboseJni) {
        LOGI("[Registering JNI native methods for class %s]\n",
            clazz->descriptor);
    }

    for (i = 0; i < nMethods; i++) {
        if (!dvmRegisterJNIMethod(clazz, methods[i].name,
                methods[i].signature, methods[i].fnPtr))
        {
            retval = JNI_ERR;
        }
    }

    JNI_EXIT();
    return retval;
}

/*
 * Un-register a native function.
 */
static jint UnregisterNatives(JNIEnv* env, jclass jclazz)
{
    JNI_ENTER();
    /*
     * The JNI docs refer to this as a way to reload/relink native libraries,
     * and say it "should not be used in normal native code".
     *
     * We can implement it if we decide we need it.
     */
    JNI_EXIT();
    return JNI_ERR;
}

/*
 * Lock the monitor.
 *
 * We have to track all monitor enters and exits, so that we can undo any
 * outstanding synchronization before the thread exits.
 */
static jint MonitorEnter(JNIEnv* env, jobject jobj)
{
    JNI_ENTER();
    Object* obj = dvmDecodeIndirectRef(env, jobj);
    dvmLockObject(_self, obj);
    trackMonitorEnter(_self, obj);
    JNI_EXIT();
    return JNI_OK;
}

/*
 * Unlock the monitor.
 *
 * Throws an IllegalMonitorStateException if the current thread
 * doesn't own the monitor.  (dvmUnlockObject() takes care of the throw.)
 *
 * According to the 1.6 spec, it's legal to call here with an exception
 * pending.  If this fails, we'll stomp the original exception.
 */
static jint MonitorExit(JNIEnv* env, jobject jobj)
{
    JNI_ENTER();
    Object* obj = dvmDecodeIndirectRef(env, jobj);
    bool success = dvmUnlockObject(_self, obj);
    if (success)
        trackMonitorExit(_self, obj);
    JNI_EXIT();
    return success ? JNI_OK : JNI_ERR;
}

/*
 * Return the JavaVM interface associated with the current thread.
 */
static jint GetJavaVM(JNIEnv* env, JavaVM** vm)
{
    JNI_ENTER();
    //*vm = gDvm.vmList;
    *vm = (JavaVM*) ((JNIEnvExt*)env)->vm;
    JNI_EXIT();
    if (*vm == NULL)
        return JNI_ERR;
    else
        return JNI_OK;
}

/*
 * Copies "len" Unicode characters, from offset "start".
 */
static void GetStringRegion(JNIEnv* env, jstring jstr, jsize start, jsize len,
    jchar* buf)
{
    JNI_ENTER();
    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    if (start + len > dvmStringLen(strObj))
        dvmThrowException("Ljava/lang/StringIndexOutOfBoundsException;", NULL);
    else
        memcpy(buf, dvmStringChars(strObj) + start, len * sizeof(u2));
    JNI_EXIT();
}

/*
 * Translates "len" Unicode characters, from offset "start", into
 * modified UTF-8 encoding.
 */
static void GetStringUTFRegion(JNIEnv* env, jstring jstr, jsize start,
    jsize len, char* buf)
{
    JNI_ENTER();
    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    if (start + len > dvmStringLen(strObj))
        dvmThrowException("Ljava/lang/StringIndexOutOfBoundsException;", NULL);
    else
        dvmCreateCstrFromStringRegion(strObj, start, len, buf);
    JNI_EXIT();
}

/*
 * Get a raw pointer to array data.
 *
 * The caller is expected to call "release" before doing any JNI calls
 * or blocking I/O operations.
 *
 * We need to pin the memory or block GC.
 */
static void* GetPrimitiveArrayCritical(JNIEnv* env, jarray jarr,
    jboolean* isCopy)
{
    JNI_ENTER();
    void* data;
    ArrayObject* arrayObj = (ArrayObject*) dvmDecodeIndirectRef(env, jarr);
    pinPrimitiveArray(arrayObj);
    data = arrayObj->contents;
    if (isCopy != NULL)
        *isCopy = JNI_FALSE;
    JNI_EXIT();
    return data;
}

/*
 * Release an array obtained with GetPrimitiveArrayCritical.
 */
static void ReleasePrimitiveArrayCritical(JNIEnv* env, jarray jarr,
    void* carray, jint mode)
{
    JNI_ENTER();
    if (mode != JNI_COMMIT) {
        ArrayObject* arrayObj = (ArrayObject*) dvmDecodeIndirectRef(env, jarr);
        unpinPrimitiveArray(arrayObj);
    }
    JNI_EXIT();
}

/*
 * Like GetStringChars, but with restricted use.
 */
static const jchar* GetStringCritical(JNIEnv* env, jstring jstr,
    jboolean* isCopy)
{
    JNI_ENTER();
    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    ArrayObject* strChars = dvmStringCharArray(strObj);

    pinPrimitiveArray(strChars);

    const u2* data = dvmStringChars(strObj);
    if (isCopy != NULL)
        *isCopy = JNI_FALSE;

    JNI_EXIT();
    return (jchar*)data;
}

/*
 * Like ReleaseStringChars, but with restricted use.
 */
static void ReleaseStringCritical(JNIEnv* env, jstring jstr,
    const jchar* carray)
{
    JNI_ENTER();
    StringObject* strObj = (StringObject*) dvmDecodeIndirectRef(env, jstr);
    ArrayObject* strChars = dvmStringCharArray(strObj);
    unpinPrimitiveArray(strChars);
    JNI_EXIT();
}

/*
 * Create a new weak global reference.
 */
static jweak NewWeakGlobalRef(JNIEnv* env, jobject obj)
{
    JNI_ENTER();
    jweak wref = createWeakGlobalRef(env, obj);
    JNI_EXIT();
    return wref;
}

/*
 * Delete the specified weak global reference.
 */
static void DeleteWeakGlobalRef(JNIEnv* env, jweak wref)
{
    JNI_ENTER();
    deleteWeakGlobalRef(env, wref);
    JNI_EXIT();
}

/*
 * Quick check for pending exceptions.
 *
 * TODO: we should be able to skip the enter/exit macros here.
 */
static jboolean ExceptionCheck(JNIEnv* env)
{
    JNI_ENTER();
    bool result = dvmCheckException(_self);
    JNI_EXIT();
    return result;
}

/*
 * Returns the type of the object referred to by "obj".  It can be local,
 * global, or weak global.
 *
 * In the current implementation, references can be global and local at
 * the same time, so while the return value is accurate it may not tell
 * the whole story.
 */
static jobjectRefType GetObjectRefType(JNIEnv* env, jobject jobj)
{
    JNI_ENTER();
    jobjectRefType type = dvmGetJNIRefType(env, jobj);
    JNI_EXIT();
    return type;
}

/*
 * Allocate and return a new java.nio.ByteBuffer for this block of memory.
 *
 * "address" may not be NULL, and "capacity" must be > 0.  (These are only
 * verified when CheckJNI is enabled.)
 */
static jobject NewDirectByteBuffer(JNIEnv* env, void* address, jlong capacity)
{
    JNI_ENTER();

    Thread* self = _self /*dvmThreadSelf()*/;
    Object* platformAddress = NULL;
    JValue callResult;
    jobject result = NULL;
    ClassObject* tmpClazz;

    tmpClazz = gDvm.methOrgApacheHarmonyLuniPlatformPlatformAddress_on->clazz;
    if (!dvmIsClassInitialized(tmpClazz) && !dvmInitClass(tmpClazz))
        goto bail;

    /* get an instance of PlatformAddress that wraps the provided address */
    dvmCallMethod(self,
        gDvm.methOrgApacheHarmonyLuniPlatformPlatformAddress_on,
        NULL, &callResult, address);
    if (dvmGetException(self) != NULL || callResult.l == NULL)
        goto bail;

    /* don't let the GC discard it */
    platformAddress = (Object*) callResult.l;
    dvmAddTrackedAlloc(platformAddress, self);
    LOGV("tracking %p for address=%p\n", platformAddress, address);

    /* create an instance of java.nio.ReadWriteDirectByteBuffer */
    tmpClazz = gDvm.classJavaNioReadWriteDirectByteBuffer;
    if (!dvmIsClassInitialized(tmpClazz) && !dvmInitClass(tmpClazz))
        goto bail;
    Object* newObj = dvmAllocObject(tmpClazz, ALLOC_DONT_TRACK);
    if (newObj != NULL) {
        /* call the (PlatformAddress, int, int) constructor */
        result = addLocalReference(env, newObj);
        dvmCallMethod(self, gDvm.methJavaNioReadWriteDirectByteBuffer_init,
            newObj, &callResult, platformAddress, (jint) capacity, (jint) 0);
        if (dvmGetException(self) != NULL) {
            deleteLocalReference(env, result);
            result = NULL;
            goto bail;
        }
    }

bail:
    if (platformAddress != NULL)
        dvmReleaseTrackedAlloc(platformAddress, self);
    JNI_EXIT();
    return result;
}

/*
 * Get the starting address of the buffer for the specified java.nio.Buffer.
 *
 * If this is not a "direct" buffer, we return NULL.
 */
static void* GetDirectBufferAddress(JNIEnv* env, jobject jbuf)
{
    JNI_ENTER();

    Object* bufObj = dvmDecodeIndirectRef(env, jbuf);
    Thread* self = _self /*dvmThreadSelf()*/;
    void* result;

    /*
     * All Buffer objects have an effectiveDirectAddress field.  If it's
     * nonzero, we can just return that value.  If not, we have to call
     * through DirectBuffer.getEffectiveAddress(), which as a side-effect
     * will set the effectiveDirectAddress field for direct buffers (and
     * things that wrap direct buffers).
     */
    result = (void*) dvmGetFieldInt(bufObj,
            gDvm.offJavaNioBuffer_effectiveDirectAddress);
    if (result != NULL) {
        //LOGI("fast path for %p\n", buf);
        goto bail;
    }

    /*
     * Start by determining if the object supports the DirectBuffer
     * interfaces.  Note this does not guarantee that it's a direct buffer.
     */
    if (!dvmInstanceof(bufObj->clazz,
            gDvm.classOrgApacheHarmonyNioInternalDirectBuffer))
    {
        goto bail;
    }

    /*
     * Get a PlatformAddress object with the effective address.
     *
     * If this isn't a direct buffer, the result will be NULL and/or an
     * exception will have been thrown.
     */
    JValue callResult;
    const Method* meth = dvmGetVirtualizedMethod(bufObj->clazz,
        gDvm.methOrgApacheHarmonyNioInternalDirectBuffer_getEffectiveAddress);
    dvmCallMethodA(self, meth, bufObj, false, &callResult, NULL);
    if (dvmGetException(self) != NULL) {
        dvmClearException(self);
        callResult.l = NULL;
    }

    Object* platformAddr = callResult.l;
    if (platformAddr == NULL) {
        LOGV("Got request for address of non-direct buffer\n");
        goto bail;
    }

    /*
     * Extract the address from the PlatformAddress object.  Instead of
     * calling the toLong() method, just grab the field directly.  This
     * is faster but more fragile.
     */
    result = (void*) dvmGetFieldInt(platformAddr,
                gDvm.offOrgApacheHarmonyLuniPlatformPlatformAddress_osaddr);

    //LOGI("slow path for %p --> %p\n", buf, result);

bail:
    JNI_EXIT();
    return result;
}

/*
 * Get the capacity of the buffer for the specified java.nio.Buffer.
 *
 * Returns -1 if the object is not a direct buffer.  (We actually skip
 * this check, since it's expensive to determine, and just return the
 * capacity regardless.)
 */
static jlong GetDirectBufferCapacity(JNIEnv* env, jobject jbuf)
{
    JNI_ENTER();

    /*
     * The capacity is always in the Buffer.capacity field.
     *
     * (The "check" version should verify that this is actually a Buffer,
     * but we're not required to do so here.)
     */
    Object* buf = dvmDecodeIndirectRef(env, jbuf);
    jlong result = dvmGetFieldInt(buf, gDvm.offJavaNioBuffer_capacity);

    JNI_EXIT();
    return result;
}


/*
 * ===========================================================================
 *      JNI invocation functions
 * ===========================================================================
 */

/*
 * Handle AttachCurrentThread{AsDaemon}.
 *
 * We need to make sure the VM is actually running.  For example, if we start
 * up, issue an Attach, and the VM exits almost immediately, by the time the
 * attaching happens the VM could already be shutting down.
 *
 * It's hard to avoid a race condition here because we don't want to hold
 * a lock across the entire operation.  What we can do is temporarily
 * increment the thread count to prevent a VM exit.
 *
 * This could potentially still have problems if a daemon thread calls here
 * while the VM is shutting down.  dvmThreadSelf() will work, since it just
 * uses pthread TLS, but dereferencing "vm" could fail.  Such is life when
 * you shut down a VM while threads are still running inside it.
 *
 * Remember that some code may call this as a way to find the per-thread
 * JNIEnv pointer.  Don't do excess work for that case.
 */
static jint attachThread(JavaVM* vm, JNIEnv** p_env, void* thr_args,
    bool isDaemon)
{
    JavaVMAttachArgs* args = (JavaVMAttachArgs*) thr_args;
    Thread* self;
    bool result = false;

    /*
     * Return immediately if we're already one with the VM.
     */
    self = dvmThreadSelf();
    if (self != NULL) {
        *p_env = self->jniEnv;
        return JNI_OK;
    }

    /*
     * No threads allowed in zygote mode.
     */
    if (gDvm.zygote) {
        return JNI_ERR;
    }

    /* increment the count to keep the VM from bailing while we run */
    dvmLockThreadList(NULL);
    if (gDvm.nonDaemonThreadCount == 0) {
        // dead or dying
        LOGV("Refusing to attach thread '%s' -- VM is shutting down\n",
            (thr_args == NULL) ? "(unknown)" : args->name);
        dvmUnlockThreadList();
        return JNI_ERR;
    }
    gDvm.nonDaemonThreadCount++;
    dvmUnlockThreadList();

    /* tweak the JavaVMAttachArgs as needed */
    JavaVMAttachArgs argsCopy;
    if (args == NULL) {
        /* allow the v1.1 calling convention */
        argsCopy.version = JNI_VERSION_1_2;
        argsCopy.name = NULL;
        argsCopy.group = dvmGetMainThreadGroup();
    } else {
        assert(args->version >= JNI_VERSION_1_2);

        argsCopy.version = args->version;
        argsCopy.name = args->name;
        if (args->group != NULL)
            argsCopy.group = args->group;
        else
            argsCopy.group = dvmGetMainThreadGroup();
    }

    result = dvmAttachCurrentThread(&argsCopy, isDaemon);

    /* restore the count */
    dvmLockThreadList(NULL);
    gDvm.nonDaemonThreadCount--;
    dvmUnlockThreadList();

    /*
     * Change the status to indicate that we're out in native code.  This
     * call is not guarded with state-change macros, so we have to do it
     * by hand.
     */
    if (result) {
        self = dvmThreadSelf();
        assert(self != NULL);
        dvmChangeStatus(self, THREAD_NATIVE);
        *p_env = self->jniEnv;
        return JNI_OK;
    } else {
        return JNI_ERR;
    }
}

/*
 * Attach the current thread to the VM.  If the thread is already attached,
 * this is a no-op.
 */
static jint AttachCurrentThread(JavaVM* vm, JNIEnv** p_env, void* thr_args)
{
    return attachThread(vm, p_env, thr_args, false);
}

/*
 * Like AttachCurrentThread, but set the "daemon" flag.
 */
static jint AttachCurrentThreadAsDaemon(JavaVM* vm, JNIEnv** p_env,
    void* thr_args)
{
    return attachThread(vm, p_env, thr_args, true);
}

/*
 * Dissociate the current thread from the VM.
 */
static jint DetachCurrentThread(JavaVM* vm)
{
    Thread* self = dvmThreadSelf();

    if (self == NULL)               /* not attached, can't do anything */
        return JNI_ERR;

    /* switch to "running" to check for suspension */
    dvmChangeStatus(self, THREAD_RUNNING);

    /* detach the thread */
    dvmDetachCurrentThread();

    /* (no need to change status back -- we have no status) */
    return JNI_OK;
}

/*
 * If current thread is attached to VM, return the associated JNIEnv.
 * Otherwise, stuff NULL in and return JNI_EDETACHED.
 *
 * JVMTI overloads this by specifying a magic value for "version", so we
 * do want to check that here.
 */
static jint GetEnv(JavaVM* vm, void** env, jint version)
{
    Thread* self = dvmThreadSelf();

    if (version < JNI_VERSION_1_1 || version > JNI_VERSION_1_6)
        return JNI_EVERSION;

    if (self == NULL) {
        *env = NULL;
    } else {
        /* TODO: status change is probably unnecessary */
        dvmChangeStatus(self, THREAD_RUNNING);
        *env = (void*) dvmGetThreadJNIEnv(self);
        dvmChangeStatus(self, THREAD_NATIVE);
    }
    if (*env == NULL)
        return JNI_EDETACHED;
    else
        return JNI_OK;
}

/*
 * Destroy the VM.  This may be called from any thread.
 *
 * If the current thread is attached, wait until the current thread is
 * the only non-daemon user-level thread.  If the current thread is not
 * attached, we attach it and do the processing as usual.  (If the attach
 * fails, it's probably because all the non-daemon threads have already
 * exited and the VM doesn't want to let us back in.)
 *
 * TODO: we don't really deal with the situation where more than one thread
 * has called here.  One thread wins, the other stays trapped waiting on
 * the condition variable forever.  Not sure this situation is interesting
 * in real life.
 */
static jint DestroyJavaVM(JavaVM* vm)
{
    JavaVMExt* ext = (JavaVMExt*) vm;
    Thread* self;

    if (ext == NULL)
        return JNI_ERR;

    if (gDvm.verboseShutdown)
        LOGD("DestroyJavaVM waiting for non-daemon threads to exit\n");

    /*
     * Sleep on a condition variable until it's okay to exit.
     */
    self = dvmThreadSelf();
    if (self == NULL) {
        JNIEnv* tmpEnv;
        if (AttachCurrentThread(vm, &tmpEnv, NULL) != JNI_OK) {
            LOGV("Unable to reattach main for Destroy; assuming VM is "
                 "shutting down (count=%d)\n",
                gDvm.nonDaemonThreadCount);
            goto shutdown;
        } else {
            LOGV("Attached to wait for shutdown in Destroy\n");
        }
    }
    dvmChangeStatus(self, THREAD_VMWAIT);

    dvmLockThreadList(self);
    gDvm.nonDaemonThreadCount--;    // remove current thread from count

    while (gDvm.nonDaemonThreadCount > 0)
        pthread_cond_wait(&gDvm.vmExitCond, &gDvm.threadListLock);

    dvmUnlockThreadList();
    self = NULL;

shutdown:
    // TODO: call System.exit() to run any registered shutdown hooks
    // (this may not return -- figure out how this should work)

    if (gDvm.verboseShutdown)
        LOGD("DestroyJavaVM shutting VM down\n");
    dvmShutdown();

    // TODO - free resources associated with JNI-attached daemon threads
    free(ext->envList);
    free(ext);

    return JNI_OK;
}


/*
 * ===========================================================================
 *      Function tables
 * ===========================================================================
 */

static const struct JNINativeInterface gNativeInterface = {
    NULL,
    NULL,
    NULL,
    NULL,

    GetVersion,

    DefineClass,
    FindClass,

    FromReflectedMethod,
    FromReflectedField,
    ToReflectedMethod,

    GetSuperclass,
    IsAssignableFrom,

    ToReflectedField,

    Throw,
    ThrowNew,
    ExceptionOccurred,
    ExceptionDescribe,
    ExceptionClear,
    FatalError,

    PushLocalFrame,
    PopLocalFrame,

    NewGlobalRef,
    DeleteGlobalRef,
    DeleteLocalRef,
    IsSameObject,
    NewLocalRef,
    EnsureLocalCapacity,

    AllocObject,
    NewObject,
    NewObjectV,
    NewObjectA,

    GetObjectClass,
    IsInstanceOf,

    GetMethodID,

    CallObjectMethod,
    CallObjectMethodV,
    CallObjectMethodA,
    CallBooleanMethod,
    CallBooleanMethodV,
    CallBooleanMethodA,
    CallByteMethod,
    CallByteMethodV,
    CallByteMethodA,
    CallCharMethod,
    CallCharMethodV,
    CallCharMethodA,
    CallShortMethod,
    CallShortMethodV,
    CallShortMethodA,
    CallIntMethod,
    CallIntMethodV,
    CallIntMethodA,
    CallLongMethod,
    CallLongMethodV,
    CallLongMethodA,
    CallFloatMethod,
    CallFloatMethodV,
    CallFloatMethodA,
    CallDoubleMethod,
    CallDoubleMethodV,
    CallDoubleMethodA,
    CallVoidMethod,
    CallVoidMethodV,
    CallVoidMethodA,

    CallNonvirtualObjectMethod,
    CallNonvirtualObjectMethodV,
    CallNonvirtualObjectMethodA,
    CallNonvirtualBooleanMethod,
    CallNonvirtualBooleanMethodV,
    CallNonvirtualBooleanMethodA,
    CallNonvirtualByteMethod,
    CallNonvirtualByteMethodV,
    CallNonvirtualByteMethodA,
    CallNonvirtualCharMethod,
    CallNonvirtualCharMethodV,
    CallNonvirtualCharMethodA,
    CallNonvirtualShortMethod,
    CallNonvirtualShortMethodV,
    CallNonvirtualShortMethodA,
    CallNonvirtualIntMethod,
    CallNonvirtualIntMethodV,
    CallNonvirtualIntMethodA,
    CallNonvirtualLongMethod,
    CallNonvirtualLongMethodV,
    CallNonvirtualLongMethodA,
    CallNonvirtualFloatMethod,
    CallNonvirtualFloatMethodV,
    CallNonvirtualFloatMethodA,
    CallNonvirtualDoubleMethod,
    CallNonvirtualDoubleMethodV,
    CallNonvirtualDoubleMethodA,
    CallNonvirtualVoidMethod,
    CallNonvirtualVoidMethodV,
    CallNonvirtualVoidMethodA,

    GetFieldID,

    GetObjectField,
    GetBooleanField,
    GetByteField,
    GetCharField,
    GetShortField,
    GetIntField,
    GetLongField,
    GetFloatField,
    GetDoubleField,
    SetObjectField,
    SetBooleanField,
    SetByteField,
    SetCharField,
    SetShortField,
    SetIntField,
    SetLongField,
    SetFloatField,
    SetDoubleField,

    GetStaticMethodID,

    CallStaticObjectMethod,
    CallStaticObjectMethodV,
    CallStaticObjectMethodA,
    CallStaticBooleanMethod,
    CallStaticBooleanMethodV,
    CallStaticBooleanMethodA,
    CallStaticByteMethod,
    CallStaticByteMethodV,
    CallStaticByteMethodA,
    CallStaticCharMethod,
    CallStaticCharMethodV,
    CallStaticCharMethodA,
    CallStaticShortMethod,
    CallStaticShortMethodV,
    CallStaticShortMethodA,
    CallStaticIntMethod,
    CallStaticIntMethodV,
    CallStaticIntMethodA,
    CallStaticLongMethod,
    CallStaticLongMethodV,
    CallStaticLongMethodA,
    CallStaticFloatMethod,
    CallStaticFloatMethodV,
    CallStaticFloatMethodA,
    CallStaticDoubleMethod,
    CallStaticDoubleMethodV,
    CallStaticDoubleMethodA,
    CallStaticVoidMethod,
    CallStaticVoidMethodV,
    CallStaticVoidMethodA,

    GetStaticFieldID,

    GetStaticObjectField,
    GetStaticBooleanField,
    GetStaticByteField,
    GetStaticCharField,
    GetStaticShortField,
    GetStaticIntField,
    GetStaticLongField,
    GetStaticFloatField,
    GetStaticDoubleField,

    SetStaticObjectField,
    SetStaticBooleanField,
    SetStaticByteField,
    SetStaticCharField,
    SetStaticShortField,
    SetStaticIntField,
    SetStaticLongField,
    SetStaticFloatField,
    SetStaticDoubleField,

    NewString,

    GetStringLength,
    GetStringChars,
    ReleaseStringChars,

    NewStringUTF,
    GetStringUTFLength,
    GetStringUTFChars,
    ReleaseStringUTFChars,

    GetArrayLength,
    NewObjectArray,
    GetObjectArrayElement,
    SetObjectArrayElement,

    NewBooleanArray,
    NewByteArray,
    NewCharArray,
    NewShortArray,
    NewIntArray,
    NewLongArray,
    NewFloatArray,
    NewDoubleArray,

    GetBooleanArrayElements,
    GetByteArrayElements,
    GetCharArrayElements,
    GetShortArrayElements,
    GetIntArrayElements,
    GetLongArrayElements,
    GetFloatArrayElements,
    GetDoubleArrayElements,

    ReleaseBooleanArrayElements,
    ReleaseByteArrayElements,
    ReleaseCharArrayElements,
    ReleaseShortArrayElements,
    ReleaseIntArrayElements,
    ReleaseLongArrayElements,
    ReleaseFloatArrayElements,
    ReleaseDoubleArrayElements,

    GetBooleanArrayRegion,
    GetByteArrayRegion,
    GetCharArrayRegion,
    GetShortArrayRegion,
    GetIntArrayRegion,
    GetLongArrayRegion,
    GetFloatArrayRegion,
    GetDoubleArrayRegion,
    SetBooleanArrayRegion,
    SetByteArrayRegion,
    SetCharArrayRegion,
    SetShortArrayRegion,
    SetIntArrayRegion,
    SetLongArrayRegion,
    SetFloatArrayRegion,
    SetDoubleArrayRegion,

    RegisterNatives,
    UnregisterNatives,

    MonitorEnter,
    MonitorExit,

    GetJavaVM,

    GetStringRegion,
    GetStringUTFRegion,

    GetPrimitiveArrayCritical,
    ReleasePrimitiveArrayCritical,

    GetStringCritical,
    ReleaseStringCritical,

    NewWeakGlobalRef,
    DeleteWeakGlobalRef,

    ExceptionCheck,

    NewDirectByteBuffer,
    GetDirectBufferAddress,
    GetDirectBufferCapacity,

    GetObjectRefType
};
static const struct JNIInvokeInterface gInvokeInterface = {
    NULL,
    NULL,
    NULL,

    DestroyJavaVM,
    AttachCurrentThread,
    DetachCurrentThread,

    GetEnv,

    AttachCurrentThreadAsDaemon,
};


/*
 * ===========================================================================
 *      VM/Env creation
 * ===========================================================================
 */

/*
 * Enable "checked JNI" after the VM has partially started.  This must
 * only be called in "zygote" mode, when we have one thread running.
 *
 * This doesn't attempt to rewrite the JNI call bridge associated with
 * native methods, so we won't get those checks for any methods that have
 * already been resolved.
 */
void dvmLateEnableCheckedJni(void)
{
    JNIEnvExt* extEnv;
    JavaVMExt* extVm;

    extEnv = dvmGetJNIEnvForThread();
    if (extEnv == NULL) {
        LOGE("dvmLateEnableCheckedJni: thread has no JNIEnv\n");
        return;
    }
    extVm = extEnv->vm;
    assert(extVm != NULL);

    if (!extVm->useChecked) {
        LOGD("Late-enabling CheckJNI\n");
        dvmUseCheckedJniVm(extVm);
        extVm->useChecked = true;
        dvmUseCheckedJniEnv(extEnv);

        /* currently no way to pick up jniopts features */
    } else {
        LOGD("Not late-enabling CheckJNI (already on)\n");
    }
}

/*
 * Not supported.
 */
DVM_EXPORT
jint JNI_GetDefaultJavaVMInitArgs(void* vm_args)
{
    return JNI_ERR;
}

/*
 * Return a buffer full of created VMs.
 *
 * We always have zero or one.
 */
DVM_EXPORT
jint JNI_GetCreatedJavaVMs(JavaVM** vmBuf, jsize bufLen, jsize* nVMs)
{
    if (gDvm.vmList != NULL) {
        *nVMs = 1;

        if (bufLen > 0)
            *vmBuf++ = gDvm.vmList;
    } else {
        *nVMs = 0;
    }

    return JNI_OK;
}


/*
 * Create a new VM instance.
 *
 * The current thread becomes the main VM thread.  We return immediately,
 * which effectively means the caller is executing in a native method.
 */
DVM_EXPORT
jint JNI_CreateJavaVM(JavaVM** p_vm, JNIEnv** p_env, void* vm_args)
{
    const JavaVMInitArgs* args = (JavaVMInitArgs*) vm_args;
    JNIEnvExt* pEnv = NULL;
    JavaVMExt* pVM = NULL;
    const char** argv;
    int argc = 0;
    int i, curOpt;
    int result = JNI_ERR;
    bool checkJni = false;
    bool warnError = true;
    bool forceDataCopy = false;

    if (args->version < JNI_VERSION_1_2)
        return JNI_EVERSION;

    // TODO: don't allow creation of multiple VMs -- one per customer for now

    /* zero globals; not strictly necessary the first time a VM is started */
    memset(&gDvm, 0, sizeof(gDvm));

    /*
     * Set up structures for JNIEnv and VM.
     */
    //pEnv = (JNIEnvExt*) malloc(sizeof(JNIEnvExt));
    pVM = (JavaVMExt*) malloc(sizeof(JavaVMExt));

    //memset(pEnv, 0, sizeof(JNIEnvExt));
    //pEnv->funcTable = &gNativeInterface;
    //pEnv->vm = pVM;
    memset(pVM, 0, sizeof(JavaVMExt));
    pVM->funcTable = &gInvokeInterface;
    pVM->envList = pEnv;
    dvmInitMutex(&pVM->envListLock);

    argv = (const char**) malloc(sizeof(char*) * (args->nOptions));
    memset(argv, 0, sizeof(char*) * (args->nOptions));

    curOpt = 0;

    /*
     * Convert JNI args to argv.
     *
     * We have to pull out vfprintf/exit/abort, because they use the
     * "extraInfo" field to pass function pointer "hooks" in.  We also
     * look for the -Xcheck:jni stuff here.
     */
    for (i = 0; i < args->nOptions; i++) {
        const char* optStr = args->options[i].optionString;

        if (optStr == NULL) {
            fprintf(stderr, "ERROR: arg %d string was null\n", i);
            goto bail;
        } else if (strcmp(optStr, "vfprintf") == 0) {
            gDvm.vfprintfHook = args->options[i].extraInfo;
        } else if (strcmp(optStr, "exit") == 0) {
            gDvm.exitHook = args->options[i].extraInfo;
        } else if (strcmp(optStr, "abort") == 0) {
            gDvm.abortHook = args->options[i].extraInfo;
        } else if (strcmp(optStr, "-Xcheck:jni") == 0) {
            checkJni = true;
        } else if (strncmp(optStr, "-Xjniopts:", 10) == 0) {
            const char* jniOpts = optStr + 9;
            while (jniOpts != NULL) {
                jniOpts++;      /* skip past ':' or ',' */
                if (strncmp(jniOpts, "warnonly", 8) == 0) {
                    warnError = false;
                } else if (strncmp(jniOpts, "forcecopy", 9) == 0) {
                    forceDataCopy = true;
                } else {
                    LOGW("unknown jni opt starting at '%s'\n", jniOpts);
                }
                jniOpts = strchr(jniOpts, ',');
            }
        } else {
            /* regular option */
            argv[curOpt++] = optStr;
        }
    }
    argc = curOpt;

    if (checkJni) {
        dvmUseCheckedJniVm(pVM);
        pVM->useChecked = true;
    }
    pVM->warnError = warnError;
    pVM->forceDataCopy = forceDataCopy;

    /* set this up before initializing VM, so it can create some JNIEnvs */
    gDvm.vmList = (JavaVM*) pVM;

    /*
     * Create an env for main thread.  We need to have something set up
     * here because some of the class initialization we do when starting
     * up the VM will call into native code.
     */
    pEnv = (JNIEnvExt*) dvmCreateJNIEnv(NULL);

    /* initialize VM */
    gDvm.initializing = true;
    if (dvmStartup(argc, argv, args->ignoreUnrecognized, (JNIEnv*)pEnv) != 0) {
        free(pEnv);
        free(pVM);
        goto bail;
    }

    /*
     * Success!  Return stuff to caller.
     */
    dvmChangeStatus(NULL, THREAD_NATIVE);
    *p_env = (JNIEnv*) pEnv;
    *p_vm = (JavaVM*) pVM;
    result = JNI_OK;

bail:
    gDvm.initializing = false;
    if (result == JNI_OK)
        LOGV("JNI_CreateJavaVM succeeded\n");
    else
        LOGW("JNI_CreateJavaVM failed\n");
    free(argv);
    return result;
}
