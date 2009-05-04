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
#include "alloc/Heap.h"
#include "alloc/HeapInternal.h"

#if WITH_HPROF && WITH_HPROF_STACK
#include "hprof/Hprof.h"
#endif


/*
 * Initialize the GC universe.
 *
 * We're currently using a memory-mapped arena to keep things off of the
 * main heap.  This needs to be replaced with something real.
 */
bool dvmGcStartup(void)
{
    dvmInitMutex(&gDvm.gcHeapLock);

    return dvmHeapStartup();
}

/*
 * Post-zygote heap initialization, including starting
 * the HeapWorker thread.
 */
bool dvmGcStartupAfterZygote(void)
{
    if (!dvmHeapWorkerStartup()) {
        return false;
    }
    return dvmHeapStartupAfterZygote();
}

/*
 * Shut the GC down.
 */
void dvmGcShutdown(void)
{
    //TODO: grab and destroy the lock
    dvmHeapShutdown();
}

/*
 * Do any last-minute preparation before we call fork() for the first time.
 */
bool dvmGcPreZygoteFork(void)
{
    return dvmHeapSourceStartupBeforeFork();
}

/*
 * Create a "stock instance" of an exception class.  These won't have
 * useful stack traces in them, but they can be thrown when everything
 * else is not working in a container class.
 */
static Object* createStockException(const char* descriptor)
{
    ClassObject* clazz;
    Method* init;
    Object* obj;

    clazz = dvmFindSystemClass(descriptor);
    if (clazz == NULL) {
        LOGE("Unable to find %s\n", descriptor);
        return NULL;
    }

    init = dvmFindDirectMethodByDescriptor(clazz, "<init>", "()V");
    if (init == NULL) {
        LOGE("Unable to find nullary constructor for %s\n", descriptor);
        return NULL;
    }

    obj = dvmAllocObject(clazz, ALLOC_DEFAULT);
    if (obj == NULL)
        return NULL;

    Thread* self = dvmThreadSelf();
    JValue unused;
    dvmCallMethod(self, init, obj, &unused);
    if (dvmCheckException(self))
        return NULL;

    return obj;
}

/*
 * "Late" initialization.  We had to defer this until we were able to
 * interpret code.
 */
bool dvmGcLateInit(void)
{
    /*
     * Pre-allocate some throwables.  These need to be explicitly added
     * to the root set by the GC.
     */
    gDvm.outOfMemoryObj = createStockException("Ljava/lang/OutOfMemoryError;");
    dvmReleaseTrackedAlloc(gDvm.outOfMemoryObj, NULL);
    gDvm.internalErrorObj = createStockException("Ljava/lang/InternalError;");
    dvmReleaseTrackedAlloc(gDvm.internalErrorObj, NULL);
    if (gDvm.outOfMemoryObj == NULL || gDvm.internalErrorObj == NULL) {
        LOGW("Unable to create stock exceptions\n");
        return false;
    }

    return true;
}


/*
 * Create an instance of the specified class.
 *
 * Returns NULL and throws an exception on failure.
 */
Object* dvmAllocObject(ClassObject* clazz, int flags)
{
    Object* newObj;

    assert(dvmIsClassInitialized(clazz) || dvmIsClassInitializing(clazz));

    if (IS_CLASS_FLAG_SET(clazz, CLASS_ISFINALIZABLE)) {
        flags |= ALLOC_FINALIZABLE;
    }

    /* allocate on GC heap; memory is zeroed out */
    newObj = dvmMalloc(clazz->objectSize, flags);
    if (newObj != NULL) {
        DVM_OBJECT_INIT(newObj, clazz);
        LOGVV("AllocObject: %s (%d)\n", clazz->descriptor,
            (int) clazz->objectSize);
#if WITH_HPROF && WITH_HPROF_STACK
        hprofFillInStackTrace(newObj);
#endif
        dvmTrackAllocation(clazz, clazz->objectSize);
    }

    return newObj;
}

/*
 * Create a copy of an object, for Object.clone().
 *
 * We use the size actually allocated, rather than obj->clazz->objectSize,
 * because the latter doesn't work for array objects.
 */
Object* dvmCloneObject(Object* obj)
{
    Object* copy;
    int size;
    int flags;

    assert(dvmIsValidObject(obj));

    /* Class.java shouldn't let us get here (java.lang.Class is final
     * and does not implement Clonable), but make extra sure.
     * A memcpy() clone will wreak havoc on a ClassObject's "innards".
     */
    assert(obj->clazz != gDvm.classJavaLangClass);

    if (IS_CLASS_FLAG_SET(obj->clazz, CLASS_ISFINALIZABLE))
        flags = ALLOC_DEFAULT | ALLOC_FINALIZABLE;
    else
        flags = ALLOC_DEFAULT;

//TODO: use clazz->objectSize for non-arrays
    size = dvmObjectSizeInHeap(obj);

    copy = dvmMalloc(size, flags);
    if (copy == NULL)
        return NULL;
#if WITH_HPROF && WITH_HPROF_STACK
    hprofFillInStackTrace(copy);
    dvmTrackAllocation(obj->clazz, size);
#endif

    memcpy(copy, obj, size);
    DVM_LOCK_INIT(&copy->lock);

    //LOGV("CloneObject: %p->%p %s (%d)\n", obj, copy, obj->clazz->name, size);

    // TODO: deal with reference classes

    /* don't call dvmReleaseTrackedAlloc -- the caller must do that */

    return copy;
}


/*
 * Track an object that was allocated internally and isn't yet part of the
 * VM root set.
 *
 * We could do this per-thread or globally.  If it's global we don't have
 * to do the thread lookup but we do have to synchronize access to the list.
 *
 * NOTE: "obj" is not a fully-formed object; in particular, obj->clazz will
 * usually be NULL since we're being called from dvmMalloc().
 */
void dvmAddTrackedAlloc(Object* obj, Thread* self)
{
    if (self == NULL)
        self = dvmThreadSelf();

    //LOGI("TRACK ADD %p\n", obj);

    assert(self != NULL);
    if (!dvmAddToReferenceTable(&self->internalLocalRefTable, obj)) {
        LOGE("threadid=%d: unable to add %p to internal ref table\n",
            self->threadId, obj);
        dvmDumpThread(self, false);
        dvmAbort();
    }
}

/*
 * Stop tracking an object.
 *
 * We allow attempts to delete NULL "obj" so that callers don't have to wrap
 * calls with "if != NULL".
 */
void dvmReleaseTrackedAlloc(Object* obj, Thread* self)
{
    if (obj == NULL)
        return;

    if (self == NULL)
        self = dvmThreadSelf();
    assert(self != NULL);

    //LOGI("TRACK REM %p (%s)\n", obj,
    //    (obj->clazz != NULL) ? obj->clazz->name : "");

    if (!dvmRemoveFromReferenceTable(&self->internalLocalRefTable,
            self->internalLocalRefTable.table, obj))
    {
        LOGE("threadid=%d: failed to remove %p from internal ref table\n",
            self->threadId, obj);
        dvmAbort();
    }
}


/*
 * Explicitly initiate garbage collection.
 */
void dvmCollectGarbage(bool collectSoftReferences)
{
    dvmLockHeap();

    LOGVV("Explicit GC\n");
    dvmCollectGarbageInternal(collectSoftReferences, GC_USER);

    dvmUnlockHeap();
}
