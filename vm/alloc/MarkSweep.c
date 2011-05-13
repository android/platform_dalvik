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

#include "Dalvik.h"
#include "alloc/clz.h"
#include "alloc/HeapBitmap.h"
#include "alloc/HeapInternal.h"
#include "alloc/HeapSource.h"
#include "alloc/MarkSweep.h"
#include "alloc/Visit.h"
#include <limits.h>     // for ULONG_MAX
#include <sys/mman.h>   // for madvise(), mmap()
#include <errno.h>

#define GC_LOG_TAG      LOG_TAG "-gc"

#if LOG_NDEBUG
#define LOGV_GC(...)    ((void)0)
#define LOGD_GC(...)    ((void)0)
#else
#define LOGV_GC(...)    LOG(LOG_VERBOSE, GC_LOG_TAG, __VA_ARGS__)
#define LOGD_GC(...)    LOG(LOG_DEBUG, GC_LOG_TAG, __VA_ARGS__)
#endif

#define LOGI_GC(...)    LOG(LOG_INFO, GC_LOG_TAG, __VA_ARGS__)
#define LOGW_GC(...)    LOG(LOG_WARN, GC_LOG_TAG, __VA_ARGS__)
#define LOGE_GC(...)    LOG(LOG_ERROR, GC_LOG_TAG, __VA_ARGS__)

#define LOG_SCAN(...)   LOGV_GC("SCAN: " __VA_ARGS__)

#define ALIGN_UP(x, n) (((size_t)(x) + (n) - 1) & ~((n) - 1))
#define ALIGN_UP_TO_PAGE_SIZE(p) ALIGN_UP(p, SYSTEM_PAGE_SIZE)

/* Do not cast the result of this to a boolean; the only set bit
 * may be > 1<<8.
 */
static inline long isMarked(const void *obj, const GcMarkContext *ctx)
{
    return dvmHeapBitmapIsObjectBitSet(ctx->bitmap, obj);
}

static bool createMarkStack(GcMarkStack *stack)
{
    const Object **limit;
    const char *name;
    size_t size;

    /* Create a stack big enough for the worst possible case,
     * where the heap is perfectly full of the smallest object.
     * TODO: be better about memory usage; use a smaller stack with
     *       overflow detection and recovery.
     */
    size = dvmHeapSourceGetIdealFootprint() * sizeof(Object*) /
            (sizeof(Object) + HEAP_SOURCE_CHUNK_OVERHEAD);
    size = ALIGN_UP_TO_PAGE_SIZE(size);
    name = "dalvik-mark-stack";
    limit = dvmAllocRegion(size, PROT_READ | PROT_WRITE, name);
    if (limit == NULL) {
        LOGE_GC("Could not mmap %zd-byte ashmem region '%s'", size, name);
        return false;
    }
    stack->limit = limit;
    stack->base = (const Object **)((uintptr_t)limit + size);
    stack->top = stack->base;
    return true;
}

static void destroyMarkStack(GcMarkStack *stack)
{
    munmap((char *)stack->limit,
            (uintptr_t)stack->base - (uintptr_t)stack->limit);
    memset(stack, 0, sizeof(*stack));
}

#define MARK_STACK_PUSH(stack, obj) \
    do { \
        *--(stack).top = (obj); \
    } while (false)

bool dvmHeapBeginMarkStep(GcMode mode)
{
    GcMarkContext *ctx = &gDvm.gcHeap->markContext;

    if (!createMarkStack(&ctx->stack)) {
        return false;
    }
    ctx->finger = NULL;
    ctx->immuneLimit = dvmHeapSourceGetImmuneLimit(mode);
    return true;
}

static long setAndReturnMarkBit(GcMarkContext *ctx, const void *obj)
{
    return dvmHeapBitmapSetAndReturnObjectBit(ctx->bitmap, obj);
}

static void markObjectNonNull(const Object *obj, GcMarkContext *ctx,
                              bool checkFinger)
{
    assert(ctx != NULL);
    assert(obj != NULL);
    assert(dvmIsValidObject(obj));

    if (obj < (Object *)ctx->immuneLimit) {
        assert(isMarked(obj, ctx));
        return;
    }
    if (!setAndReturnMarkBit(ctx, obj)) {
        /* This object was not previously marked.
         */
        if (checkFinger && (void *)obj < ctx->finger) {
            /* This object will need to go on the mark stack.
             */
            MARK_STACK_PUSH(ctx->stack, obj);
        }

#if WITH_HPROF
        if (gDvm.gcHeap->hprofContext != NULL) {
            hprofMarkRootObject(gDvm.gcHeap->hprofContext, obj, 0);
        }
#endif
    }
}

/* Used to mark objects when recursing.  Recursion is done by moving
 * the finger across the bitmaps in address order and marking child
 * objects.  Any newly-marked objects whose addresses are lower than
 * the finger won't be visited by the bitmap scan, so those objects
 * need to be added to the mark stack.
 */
static void markObject(const Object *obj, GcMarkContext *ctx)
{
    if (obj != NULL) {
        markObjectNonNull(obj, ctx, true);
    }
}

/* If the object hasn't already been marked, mark it and
 * schedule it to be scanned for references.
 *
 * obj may not be NULL.  The macro dvmMarkObject() should
 * be used in situations where a reference may be NULL.
 *
 * This function may only be called when marking the root
 * set.  When recursing, use the internal markObject().
 */
void dvmMarkObjectNonNull(const Object *obj)
{
    assert(obj != NULL);
    markObjectNonNull(obj, &gDvm.gcHeap->markContext, false);
}

/* Mark the set of root objects.
 *
 * Things we need to scan:
 * - System classes defined by root classloader
 * - For each thread:
 *   - Interpreted stack, from top to "curFrame"
 *     - Dalvik registers (args + local vars)
 *   - JNI local references
 *   - Automatic VM local references (TrackedAlloc)
 *   - Associated Thread/VMThread object
 *   - ThreadGroups (could track & start with these instead of working
 *     upward from Threads)
 *   - Exception currently being thrown, if present
 * - JNI global references
 * - Interned string table
 * - Primitive classes
 * - Special objects
 *   - gDvm.outOfMemoryObj
 * - Objects allocated with ALLOC_NO_GC
 * - Objects pending finalization (but not yet finalized)
 * - Objects in debugger object registry
 *
 * Don't need:
 * - Native stack (for in-progress stuff in the VM)
 *   - The TrackedAlloc stuff watches all native VM references.
 */
void dvmHeapMarkRootSet()
{
    GcHeap *gcHeap = gDvm.gcHeap;

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_STICKY_CLASS, 0);

    LOG_SCAN("immune objects");
    dvmMarkImmuneObjects(gcHeap->markContext.immuneLimit);

    LOG_SCAN("root class loader\n");
    dvmGcScanRootClassLoader();
    LOG_SCAN("primitive classes\n");
    dvmGcScanPrimitiveClasses();

    /* dvmGcScanRootThreadGroups() sets a bunch of
     * different scan states internally.
     */
    HPROF_CLEAR_GC_SCAN_STATE();

    LOG_SCAN("root thread groups\n");
    dvmGcScanRootThreadGroups();

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_INTERNED_STRING, 0);

    LOG_SCAN("interned strings\n");
    dvmGcScanInternedStrings();

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_JNI_GLOBAL, 0);

    LOG_SCAN("JNI global refs\n");
    dvmGcMarkJniGlobalRefs();

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_REFERENCE_CLEANUP, 0);

    LOG_SCAN("pending reference operations\n");
    dvmHeapMarkLargeTableRefs(gcHeap->referenceOperations);

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_FINALIZING, 0);

    LOG_SCAN("pending finalizations\n");
    dvmHeapMarkLargeTableRefs(gcHeap->pendingFinalizationRefs);

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_DEBUGGER, 0);

    LOG_SCAN("debugger refs\n");
    dvmGcMarkDebuggerRefs();

    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_VM_INTERNAL, 0);

    /* Mark any special objects we have sitting around.
     */
    LOG_SCAN("special objects\n");
    dvmMarkObjectNonNull(gDvm.outOfMemoryObj);
    dvmMarkObjectNonNull(gDvm.internalErrorObj);
    dvmMarkObjectNonNull(gDvm.noClassDefFoundErrorObj);
//TODO: scan object references sitting in gDvm;  use pointer begin & end

    HPROF_CLEAR_GC_SCAN_STATE();
}

/*
 * Callback applied to root references.  If the root location contains
 * a white reference it is pushed on the mark stack and grayed.
 */
static void markObjectVisitor(void *addr, void *arg)
{
    Object *obj;

    assert(addr != NULL);
    assert(arg != NULL);
    obj = *(Object **)addr;
    if (obj != NULL) {
        markObjectNonNull(obj, arg, true);
    }
}

/*
 * Grays all references in the roots.
 */
void dvmHeapReMarkRootSet(void)
{
    GcMarkContext *ctx = &gDvm.gcHeap->markContext;
    assert(ctx->finger == (void *)ULONG_MAX);
    dvmVisitRoots(markObjectVisitor, ctx);
}

/*
 * Nothing past this point is allowed to use dvmMarkObject() or
 * dvmMarkObjectNonNull(), which are for root-marking only.
 * Scanning/recursion must use markObject(), which takes the finger
 * into account.
 */
#undef dvmMarkObject
#define dvmMarkObject __dont_use_dvmMarkObject__
#define dvmMarkObjectNonNull __dont_use_dvmMarkObjectNonNull__

/*
 * Scans instance fields.
 */
static void scanInstanceFields(const Object *obj, GcMarkContext *ctx)
{
    assert(obj != NULL);
    assert(obj->clazz != NULL);
    assert(ctx != NULL);

    if (obj->clazz->refOffsets != CLASS_WALK_SUPER) {
        unsigned int refOffsets = obj->clazz->refOffsets;
        while (refOffsets != 0) {
            const int rshift = CLZ(refOffsets);
            refOffsets &= ~(CLASS_HIGH_BIT >> rshift);
            markObject(dvmGetFieldObject((Object*)obj,
                                          CLASS_OFFSET_FROM_CLZ(rshift)), ctx);
        }
    } else {
        ClassObject *clazz;
        int i;
        for (clazz = obj->clazz; clazz != NULL; clazz = clazz->super) {
            InstField *field = clazz->ifields;
            for (i = 0; i < clazz->ifieldRefCount; ++i, ++field) {
                void *addr = BYTE_OFFSET((Object *)obj, field->byteOffset);
                markObject(((JValue *)addr)->l, ctx);
            }
        }
    }
}

/*
 * Scans the header, static field references, and interface
 * pointers of a class object.
 */
static void scanClassObject(const ClassObject *obj, GcMarkContext *ctx)
{
    int i;

    assert(obj != NULL);
    assert(obj->obj.clazz == gDvm.classJavaLangClass);
    assert(ctx != NULL);

    markObject((Object *)obj->obj.clazz, ctx);
    if (IS_CLASS_FLAG_SET(obj, CLASS_ISARRAY)) {
        markObject((Object *)obj->elementClass, ctx);
    }
    /* Do super and the interfaces contain Objects and not dex idx values? */
    if (obj->status > CLASS_IDX) {
        markObject((Object *)obj->super, ctx);
    }
    markObject(obj->classLoader, ctx);
    /* Scan static field references. */
    for (i = 0; i < obj->sfieldCount; ++i) {
        char ch = obj->sfields[i].field.signature[0];
        if (ch == '[' || ch == 'L') {
            markObject(obj->sfields[i].value.l, ctx);
        }
    }
    /* Scan the instance fields. */
    scanInstanceFields((const Object *)obj, ctx);
    /* Scan interface references. */
    if (obj->status > CLASS_IDX) {
        for (i = 0; i < obj->interfaceCount; ++i) {
            markObject((Object *)obj->interfaces[i], ctx);
        }
    }
}

/*
 * Scans the header of all array objects.  If the array object is
 * specialized to a reference type, scans the array data as well.
 */
static void scanArrayObject(const ArrayObject *obj, GcMarkContext *ctx)
{
    size_t i;

    assert(obj != NULL);
    assert(obj->obj.clazz != NULL);
    assert(ctx != NULL);
    /* Scan the class object reference. */
    markObject((Object *)obj->obj.clazz, ctx);
    if (IS_CLASS_FLAG_SET(obj->obj.clazz, CLASS_ISOBJECTARRAY)) {
        /* Scan the array contents. */
        Object **contents = (Object **)obj->contents;
        for (i = 0; i < obj->length; ++i) {
            markObject(contents[i], ctx);
        }
    }
}

/*
 * Returns class flags relating to Reference subclasses.
 */
static int referenceClassFlags(const Object *obj)
{
    int flags = CLASS_ISREFERENCE |
                CLASS_ISWEAKREFERENCE |
                CLASS_ISPHANTOMREFERENCE;
    return GET_CLASS_FLAG_GROUP(obj->clazz, flags);
}

/*
 * Returns true if the object derives from SoftReference.
 */
static bool isSoftReference(const Object *obj)
{
    return referenceClassFlags(obj) == CLASS_ISREFERENCE;
}

/*
 * Returns true if the object derives from WeakReference.
 */
static bool isWeakReference(const Object *obj)
{
    return referenceClassFlags(obj) & CLASS_ISWEAKREFERENCE;
}

/*
 * Returns true if the object derives from PhantomReference.
 */
static bool isPhantomReference(const Object *obj)
{
    return referenceClassFlags(obj) & CLASS_ISPHANTOMREFERENCE;
}

/*
 * Adds a reference to the tail of a circular queue of references.
 */
static void enqueuePendingReference(Object *ref, Object **list)
{
    size_t offset;

    assert(ref != NULL);
    assert(list != NULL);
    offset = gDvm.offJavaLangRefReference_pendingNext;
    if (*list == NULL) {
        dvmSetFieldObject(ref, offset, ref);
        *list = ref;
    } else {
        Object *head = dvmGetFieldObject(*list, offset);
        dvmSetFieldObject(ref, offset, head);
        dvmSetFieldObject(*list, offset, ref);
    }
}

/*
 * Removes the reference at the head of a circular queue of
 * references.
 */
static Object *dequeuePendingReference(Object **list)
{
    Object *ref, *head;
    size_t offset;

    assert(list != NULL);
    assert(*list != NULL);
    offset = gDvm.offJavaLangRefReference_pendingNext;
    head = dvmGetFieldObject(*list, offset);
    if (*list == head) {
        ref = *list;
        *list = NULL;
    } else {
        Object *next = dvmGetFieldObject(head, offset);
        dvmSetFieldObject(*list, offset, next);
        ref = head;
    }
    dvmSetFieldObject(ref, offset, NULL);
    return ref;
}

/*
 * Process the "referent" field in a java.lang.ref.Reference.  If the
 * referent has not yet been marked, put it on the appropriate list in
 * the gcHeap for later processing.
 */
static void delayReferenceReferent(Object *obj, GcMarkContext *ctx)
{
    GcHeap *gcHeap = gDvm.gcHeap;
    Object *pending, *referent;
    size_t pendingNextOffset, referentOffset;

    assert(obj != NULL);
    assert(obj->clazz != NULL);
    assert(IS_CLASS_FLAG_SET(obj->clazz, CLASS_ISREFERENCE));
    assert(ctx != NULL);
    pendingNextOffset = gDvm.offJavaLangRefReference_pendingNext;
    referentOffset = gDvm.offJavaLangRefReference_referent;
    pending = dvmGetFieldObject(obj, pendingNextOffset);
    referent = dvmGetFieldObject(obj, referentOffset);
    if (pending == NULL && referent != NULL && !isMarked(referent, ctx)) {
        Object **list = NULL;
        if (isSoftReference(obj)) {
            list = &gcHeap->softReferences;
        } else if (isWeakReference(obj)) {
            list = &gcHeap->weakReferences;
        } else if (isPhantomReference(obj)) {
            list = &gcHeap->phantomReferences;
        }
        assert(list != NULL);
        enqueuePendingReference(obj, list);
    }
}

/*
 * Scans the header and field references of a data object.
 */
static void scanDataObject(DataObject *obj, GcMarkContext *ctx)
{
    assert(obj != NULL);
    assert(obj->obj.clazz != NULL);
    assert(ctx != NULL);
    /* Scan the class object. */
    markObject((Object *)obj->obj.clazz, ctx);
    /* Scan the instance fields. */
    scanInstanceFields((const Object *)obj, ctx);
    if (IS_CLASS_FLAG_SET(obj->obj.clazz, CLASS_ISREFERENCE)) {
        delayReferenceReferent((Object *)obj, ctx);
    }
}

/*
 * Scans an object reference.  Determines the type of the reference
 * and dispatches to a specialized scanning routine.
 */
static void scanObject(const Object *obj, GcMarkContext *ctx)
{
    assert(obj != NULL);
    assert(ctx != NULL);
    assert(obj->clazz != NULL);
#if WITH_HPROF
    if (gDvm.gcHeap->hprofContext != NULL) {
        hprofDumpHeapObject(gDvm.gcHeap->hprofContext, obj);
    }
#endif
    /* Dispatch a type-specific scan routine. */
    if (obj->clazz == gDvm.classJavaLangClass) {
        scanClassObject((ClassObject *)obj, ctx);
    } else if (IS_CLASS_FLAG_SET(obj->clazz, CLASS_ISARRAY)) {
        scanArrayObject((ArrayObject *)obj, ctx);
    } else {
        scanDataObject((DataObject *)obj, ctx);
    }
}

static void
processMarkStack(GcMarkContext *ctx)
{
    const Object **const base = ctx->stack.base;

    /* Scan anything that's on the mark stack.
     * We can't use the bitmaps anymore, so use
     * a finger that points past the end of them.
     */
    ctx->finger = (void *)ULONG_MAX;
    while (ctx->stack.top != base) {
        scanObject(*ctx->stack.top++, ctx);
    }
}

static size_t objectSize(const Object *obj)
{
    assert(dvmIsValidObject(obj));
    assert(dvmIsValidObject((Object *)obj->clazz));
    if (IS_CLASS_FLAG_SET(obj->clazz, CLASS_ISARRAY)) {
        return dvmArrayObjectSize((ArrayObject *)obj);
    } else if (obj->clazz == gDvm.classJavaLangClass) {
        return dvmClassObjectSize((ClassObject *)obj);
    } else {
        return obj->clazz->objectSize;
    }
}

/*
 * Scans forward to the header of the next marked object between start
 * and limit.  Returns NULL if no marked objects are in that region.
 */
static Object *nextGrayObject(u1 *base, u1 *limit, HeapBitmap *markBits)
{
    u1 *ptr;

    assert(base < limit);
    assert(limit - base <= GC_CARD_SIZE);
    for (ptr = base; ptr < limit; ptr += HB_OBJECT_ALIGNMENT) {
        if (dvmHeapBitmapIsObjectBitSet(markBits, ptr))
            return (Object *)ptr;
    }
    return NULL;
}

/*
 * Scan the card table looking for objects that have been grayed by
 * the mutator.
 */
static void scanGrayObjects(GcMarkContext *ctx)
{
    GcHeap *h = gDvm.gcHeap;
    HeapBitmap *markBits, *liveBits;
    u1 *card, *baseCard, *limitCard;
    size_t footprint;

    markBits = ctx->bitmap;
    liveBits = dvmHeapSourceGetLiveBits();
    footprint = dvmHeapSourceGetValue(HS_FOOTPRINT, NULL, 0);
    baseCard = &h->cardTableBase[0];
    limitCard = dvmCardFromAddr((u1 *)dvmHeapSourceGetBase() + footprint);
    assert(limitCard < &h->cardTableBase[h->cardTableLength]);
    for (card = baseCard; card <= limitCard; ++card) {
        if (*card == GC_CARD_DIRTY) {
            /*
             * The card is dirty.  Scan all of the objects that
             * intersect with the card address.
             */
            u1 *addr = dvmAddrFromCard(card);
            /*
             * Scan through all black objects that start on the
             * current card.
             */
            u1 *limit = addr + GC_CARD_SIZE;
            u1 *next = addr;
            while (next < limit) {
                Object *obj = nextGrayObject(next, limit, markBits);
                if (obj == NULL)
                    break;
                scanObject(obj, ctx);
                next = (u1*)obj + ALIGN_UP(objectSize(obj), HB_OBJECT_ALIGNMENT);
            }
        }
    }
}

/*
 * Callback for scanning each object in the bitmap.  The finger is set
 * to the address corresponding to the lowest address in the next word
 * of bits in the bitmap.
 */
static void scanBitmapCallback(void *addr, void *finger, void *arg)
{
    GcMarkContext *ctx = arg;
    ctx->finger = (void *)finger;
    scanObject(addr, ctx);
}

/* Given bitmaps with the root set marked, find and mark all
 * reachable objects.  When this returns, the entire set of
 * live objects will be marked and the mark stack will be empty.
 */
void dvmHeapScanMarkedObjects(void)
{
    GcMarkContext *ctx = &gDvm.gcHeap->markContext;

    assert(ctx->finger == NULL);

    /* The bitmaps currently have bits set for the root set.
     * Walk across the bitmaps and scan each object.
     */
    dvmHeapBitmapScanWalk(ctx->bitmap, scanBitmapCallback, ctx);

    /* We've walked the mark bitmaps.  Scan anything that's
     * left on the mark stack.
     */
    processMarkStack(ctx);

    LOG_SCAN("done with marked objects\n");
}

void dvmHeapReScanMarkedObjects(void)
{
    GcMarkContext *ctx = &gDvm.gcHeap->markContext;

    /*
     * The finger must have been set to the maximum value to ensure
     * that gray objects will be pushed onto the mark stack.
     */
    assert(ctx->finger == (void *)ULONG_MAX);
    scanGrayObjects(ctx);
    processMarkStack(ctx);
}

/*
 * Clear the referent field.
 */
static void clearReference(Object *reference)
{
    size_t offset = gDvm.offJavaLangRefReference_referent;
    dvmSetFieldObject(reference, offset, NULL);
}

/*
 * Returns true if the reference was registered with a reference queue
 * and has not yet been enqueued.
 */
static bool isEnqueuable(const Object *reference)
{
    Object *queue = dvmGetFieldObject(reference,
            gDvm.offJavaLangRefReference_queue);
    Object *queueNext = dvmGetFieldObject(reference,
            gDvm.offJavaLangRefReference_queueNext);
    return queue != NULL && queueNext == NULL;
}

/*
 * Schedules a reference to be appended to its reference queue.
 */
static void enqueueReference(Object *ref)
{
    assert(ref != NULL);
    assert(dvmGetFieldObject(ref, gDvm.offJavaLangRefReference_queue) != NULL);
    assert(dvmGetFieldObject(ref, gDvm.offJavaLangRefReference_queueNext) == NULL);
    if (!dvmHeapAddRefToLargeTable(&gDvm.gcHeap->referenceOperations, ref)) {
        LOGE_HEAP("enqueueReference(): no room for any more "
                  "reference operations\n");
        dvmAbort();
    }
}

/*
 * Walks the reference list marking any references subject to the
 * reference clearing policy.  References with a black referent are
 * removed from the list.  References with white referents biased
 * toward saving are blackened and also removed from the list.
 */
void dvmHandleSoftRefs(Object **list)
{
    GcMarkContext *ctx;
    Object *ref, *referent;
    Object *clear;
    size_t referentOffset;
    size_t counter;
    bool marked;

    ctx = &gDvm.gcHeap->markContext;
    referentOffset = gDvm.offJavaLangRefReference_referent;
    clear = NULL;
    counter = 0;
    while (*list != NULL) {
        ref = dequeuePendingReference(list);
        referent = dvmGetFieldObject(ref, referentOffset);
        if (referent == NULL) {
            /* Referent was cleared by the user during marking. */
            continue;
        }
        marked = isMarked(referent, ctx);
        if (!marked && ((++counter) & 1)) {
            /* Referent is white and biased toward saving, mark it. */
            markObject(referent, ctx);
            marked = true;
        }
        if (!marked) {
            /* Referent is white, queue it for clearing. */
            enqueuePendingReference(ref, &clear);
        }
    }
    *list = clear;
    /*
     * Restart the mark with the newly black references added to the
     * root set.
     */
    processMarkStack(ctx);
}

/*
 * Unlink the reference list clearing references objects with white
 * referents.  Cleared references registered to a reference queue are
 * scheduled for appending by the heap worker thread.
 */
void dvmClearWhiteRefs(Object **list)
{
    GcMarkContext *ctx;
    Object *ref, *referent;
    size_t referentOffset;
    bool doSignal;

    ctx = &gDvm.gcHeap->markContext;
    referentOffset = gDvm.offJavaLangRefReference_referent;
    doSignal = false;
    while (*list != NULL) {
        ref = dequeuePendingReference(list);
        referent = dvmGetFieldObject(ref, referentOffset);
        if (referent != NULL && !isMarked(referent, ctx)) {
            /* Referent is white, clear it. */
            clearReference(ref);
            if (isEnqueuable(ref)) {
                enqueueReference(ref);
                doSignal = true;
            }
        }
    }
    /*
     * If we cleared a reference with a reference queue we must notify
     * the heap worker to append the reference.
     */
    if (doSignal) {
        dvmSignalHeapWorker(false);
    }
    assert(*list == NULL);
}

/* Find unreachable objects that need to be finalized,
 * and schedule them for finalization.
 */
void dvmHeapScheduleFinalizations()
{
    HeapRefTable newPendingRefs;
    LargeHeapRefTable *finRefs = gDvm.gcHeap->finalizableRefs;
    Object **ref;
    Object **lastRef;
    size_t totalPendCount;
    GcMarkContext *ctx = &gDvm.gcHeap->markContext;

    /*
     * All reachable objects have been marked.
     * Any unmarked finalizable objects need to be finalized.
     */

    /* Create a table that the new pending refs will
     * be added to.
     */
    if (!dvmHeapInitHeapRefTable(&newPendingRefs)) {
        //TODO: mark all finalizable refs and hope that
        //      we can schedule them next time.  Watch out,
        //      because we may be expecting to free up space
        //      by calling finalizers.
        LOGE_GC("dvmHeapScheduleFinalizations(): no room for "
                "pending finalizations\n");
        dvmAbort();
    }

    /* Walk through finalizableRefs and move any unmarked references
     * to the list of new pending refs.
     */
    totalPendCount = 0;
    while (finRefs != NULL) {
        Object **gapRef;
        size_t newPendCount = 0;

        gapRef = ref = finRefs->refs.table;
        lastRef = finRefs->refs.nextEntry;
        while (ref < lastRef) {
            if (!isMarked(*ref, ctx)) {
                if (!dvmHeapAddToHeapRefTable(&newPendingRefs, *ref)) {
                    //TODO: add the current table and allocate
                    //      a new, smaller one.
                    LOGE_GC("dvmHeapScheduleFinalizations(): "
                            "no room for any more pending finalizations: %zd\n",
                            dvmHeapNumHeapRefTableEntries(&newPendingRefs));
                    dvmAbort();
                }
                newPendCount++;
            } else {
                /* This ref is marked, so will remain on finalizableRefs.
                 */
                if (newPendCount > 0) {
                    /* Copy it up to fill the holes.
                     */
                    *gapRef++ = *ref;
                } else {
                    /* No holes yet; don't bother copying.
                     */
                    gapRef++;
                }
            }
            ref++;
        }
        finRefs->refs.nextEntry = gapRef;
        //TODO: if the table is empty when we're done, free it.
        totalPendCount += newPendCount;
        finRefs = finRefs->next;
    }
    LOGD_GC("dvmHeapScheduleFinalizations(): %zd finalizers triggered.\n",
            totalPendCount);
    if (totalPendCount == 0) {
        /* No objects required finalization.
         * Free the empty temporary table.
         */
        dvmClearReferenceTable(&newPendingRefs);
        return;
    }

    /* Add the new pending refs to the main list.
     */
    if (!dvmHeapAddTableToLargeTable(&gDvm.gcHeap->pendingFinalizationRefs,
                &newPendingRefs))
    {
        LOGE_GC("dvmHeapScheduleFinalizations(): can't insert new "
                "pending finalizations\n");
        dvmAbort();
    }

    //TODO: try compacting the main list with a memcpy loop

    /* Mark the refs we just moved;  we don't want them or their
     * children to get swept yet.
     */
    ref = newPendingRefs.table;
    lastRef = newPendingRefs.nextEntry;
    assert(ref < lastRef);
    HPROF_SET_GC_SCAN_STATE(HPROF_ROOT_FINALIZING, 0);
    while (ref < lastRef) {
        assert(*ref != NULL);
        markObject(*ref, ctx);
        ref++;
    }
    HPROF_CLEAR_GC_SCAN_STATE();
    processMarkStack(ctx);
    dvmSignalHeapWorker(false);
}

void dvmHeapFinishMarkStep()
{
    GcMarkContext *ctx;

    ctx = &gDvm.gcHeap->markContext;

    /* The mark bits are now not needed.
     */
    dvmHeapSourceZeroMarkBitmap();

    /* Clean up everything else associated with the marking process.
     */
    destroyMarkStack(&ctx->stack);

    ctx->finger = NULL;
}

typedef struct {
    size_t numObjects;
    size_t numBytes;
    bool isConcurrent;
} SweepContext;

static void sweepBitmapCallback(size_t numPtrs, void **ptrs, void *arg)
{
    SweepContext *ctx = arg;

    if (ctx->isConcurrent) {
        dvmLockHeap();
    }
    ctx->numBytes += dvmHeapSourceFreeList(numPtrs, ptrs);
    ctx->numObjects += numPtrs;
    if (ctx->isConcurrent) {
        dvmUnlockHeap();
    }
}

/*
 * Returns true if the given object is unmarked.  This assumes that
 * the bitmaps have not yet been swapped.
 */
static int isUnmarkedObject(void *object)
{
    return !isMarked((void *)((uintptr_t)object & ~(HB_OBJECT_ALIGNMENT-1)),
            &gDvm.gcHeap->markContext);
}

/*
 * Process all the internal system structures that behave like
 * weakly-held objects.
 */
void dvmHeapSweepSystemWeaks(void)
{
    dvmGcDetachDeadInternedStrings(isUnmarkedObject);
    dvmSweepMonitorList(&gDvm.monitorList, isUnmarkedObject);
}

/*
 * Walk through the list of objects that haven't been marked and free
 * them.  Assumes the bitmaps have been swapped.
 */
void dvmHeapSweepUnmarkedObjects(GcMode mode, bool isConcurrent,
                                 size_t *numObjects, size_t *numBytes)
{
    HeapBitmap currMark[HEAP_SOURCE_MAX_HEAP_COUNT];
    HeapBitmap currLive[HEAP_SOURCE_MAX_HEAP_COUNT];
    SweepContext ctx;
    size_t numBitmaps, numSweepBitmaps;
    size_t i;

    numBitmaps = dvmHeapSourceGetNumHeaps();
    dvmHeapSourceGetObjectBitmaps(currLive, currMark, numBitmaps);
    if (mode == GC_PARTIAL) {
        numSweepBitmaps = 1;
        assert((uintptr_t)gDvm.gcHeap->markContext.immuneLimit == currLive[0].base);
    } else {
        numSweepBitmaps = numBitmaps;
    }
    ctx.numObjects = ctx.numBytes = 0;
    ctx.isConcurrent = isConcurrent;
    for (i = 0; i < numSweepBitmaps; i++) {
        HeapBitmap* prevLive = &currMark[i];
        HeapBitmap* prevMark = &currLive[i];
        dvmHeapBitmapSweepWalk(prevLive, prevMark, sweepBitmapCallback, &ctx);
    }
    *numObjects = ctx.numObjects;
    *numBytes = ctx.numBytes;
    if (gDvm.allocProf.enabled) {
        gDvm.allocProf.freeCount += ctx.numObjects;
        gDvm.allocProf.freeSize += ctx.numBytes;
    }
}
