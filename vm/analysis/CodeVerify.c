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
 * Dalvik bytecode structural verifier.  The only public entry point
 * (except for a few shared utility functions) is dvmVerifyCodeFlow().
 *
 * TODO: might benefit from a signature-->class lookup cache.  Could avoid
 * some string-peeling and wouldn't need to compute hashes.
 *
 * TODO: we do too much stuff in here that could be done in the static
 * verification pass.  It's convenient, because we have all of the
 * necessary information, but it's more efficient to do it over in
 * DexVerify.c because in here we may have to process instructions
 * multiple times.
 */
#include "Dalvik.h"
#include "analysis/CodeVerify.h"
#include "analysis/RegisterMap.h"
#include "libdex/DexCatch.h"
#include "libdex/InstrUtils.h"

#include <stddef.h>


/*
 * We don't need to store the register data for many instructions, because
 * we either only need it at branch points (for verification) or GC points
 * and branches (for verification + type-precise register analysis).
 */
typedef enum RegisterTrackingMode {
    kTrackRegsBranches,
    kTrackRegsGcPoints,
    kTrackRegsAll
} RegisterTrackingMode;

/*
 * Set this to enable dead code scanning.  This is not required, but it's
 * very useful when testing changes to the verifier (to make sure we're not
 * skipping over stuff) and for checking the optimized output from "dx".
 * The only reason not to do it is that it slightly increases the time
 * required to perform verification.
 */
#define DEAD_CODE_SCAN  true

static bool gDebugVerbose = false;      // TODO: remove this

#if 0
int gDvm__totalInstr = 0;
int gDvm__gcInstr = 0;
int gDvm__gcData = 0;
int gDvm__gcSimpleData = 0;
#endif

/*
 * Selectively enable verbose debug logging -- use this to activate
 * dumpRegTypes() calls for all instructions in the specified method.
 */
static inline bool doVerboseLogging(const Method* meth) {
    return false;       /* COMMENT OUT to enable verbose debugging */

    const char* cd = "Lop_lshr;";
    const char* mn = "test";
    const char* sg = "(II)J";
    return (strcmp(meth->clazz->descriptor, cd) == 0 &&
            dvmCompareNameDescriptorAndMethod(mn, sg, meth) == 0);
}

#define SHOW_REG_DETAILS    (0 /*| DRT_SHOW_REF_TYPES | DRT_SHOW_LOCALS*/)

/*
 * We need an extra "pseudo register" to hold the return type briefly.  It
 * can be category 1 or 2, so we need two slots.
 */
#define kExtraRegs  2
#define RESULT_REGISTER(_insnRegCount)  (_insnRegCount)

/*
 * Big fat collection of registers.
 */
typedef struct RegisterTable {
    /*
     * Array of RegType arrays, one per address in the method.  We only
     * set the pointers for certain addresses, based on what we're trying
     * to accomplish.
     */
    RegType**   addrRegs;

    /*
     * Number of registers we track for each instruction.  This is equal
     * to the method's declared "registersSize" plus kExtraRegs.
     */
    int         insnRegCountPlus;

    /*
     * A single large alloc, with all of the storage needed for addrRegs.
     */
    RegType*    regAlloc;
} RegisterTable;


/* fwd */
static void checkMergeTab(void);
static bool isInitMethod(const Method* meth);
static RegType getInvocationThis(const RegType* insnRegs,\
    const int insnRegCount, const DecodedInstruction* pDecInsn, bool* pOkay);
static void verifyRegisterType(const RegType* insnRegs, const int insnRegCount,\
    u4 vsrc, RegType checkType, bool* pOkay);
static bool doCodeVerification(const Method* meth, InsnFlags* insnFlags,\
    RegisterTable* regTable, UninitInstanceMap* uninitMap);
static bool verifyInstruction(const Method* meth, InsnFlags* insnFlags,\
    RegisterTable* regTable, RegType* workRegs, int insnIdx,
    UninitInstanceMap* uninitMap, int* pStartGuess);
static ClassObject* findCommonSuperclass(ClassObject* c1, ClassObject* c2);
static void dumpRegTypes(const Method* meth, const InsnFlags* insnFlags,\
    const RegType* addrRegs, int addr, const char* addrName,
    const UninitInstanceMap* uninitMap, int displayFlags);

/* bit values for dumpRegTypes() "displayFlags" */
enum {
    DRT_SIMPLE          = 0,
    DRT_SHOW_REF_TYPES  = 0x01,
    DRT_SHOW_LOCALS     = 0x02,
};


/*
 * ===========================================================================
 *      RegType and UninitInstanceMap utility functions
 * ===========================================================================
 */

#define __  kRegTypeUnknown
#define _U  kRegTypeUninit
#define _X  kRegTypeConflict
#define _F  kRegTypeFloat
#define _0  kRegTypeZero
#define _1  kRegTypeOne
#define _Z  kRegTypeBoolean
#define _b  kRegTypePosByte
#define _B  kRegTypeByte
#define _s  kRegTypePosShort
#define _S  kRegTypeShort
#define _C  kRegTypeChar
#define _I  kRegTypeInteger
#define _J  kRegTypeLongLo
#define _j  kRegTypeLongHi
#define _D  kRegTypeDoubleLo
#define _d  kRegTypeDoubleHi

/*
 * Merge result table for primitive values.  The table is symmetric along
 * the diagonal.
 *
 * Note that 32-bit int/float do not merge into 64-bit long/double.  This
 * is a register merge, not a widening conversion.  Only the "implicit"
 * widening within a category, e.g. byte to short, is allowed.
 *
 * Because Dalvik does not draw a distinction between int and float, we
 * have to allow free exchange between 32-bit int/float and 64-bit
 * long/double.
 *
 * Note that Uninit+Uninit=Uninit.  This holds true because we only
 * use this when the RegType value is exactly equal to kRegTypeUninit, which
 * can only happen for the zeroeth entry in the table.
 *
 * "Unknown" never merges with anything known.  The only time a register
 * transitions from "unknown" to "known" is when we're executing code
 * for the first time, and we handle that with a simple copy.
 */
const char gDvmMergeTab[kRegTypeMAX][kRegTypeMAX] =
{
    /* chk:  _  U  X  F  0  1  Z  b  B  s  S  C  I  J  j  D  d */
    { /*_*/ __,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X },
    { /*U*/ _X,_U,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X },
    { /*X*/ _X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X },
    { /*F*/ _X,_X,_X,_F,_F,_F,_F,_F,_F,_F,_F,_F,_F,_X,_X,_X,_X },
    { /*0*/ _X,_X,_X,_F,_0,_Z,_Z,_b,_B,_s,_S,_C,_I,_X,_X,_X,_X },
    { /*1*/ _X,_X,_X,_F,_Z,_1,_Z,_b,_B,_s,_S,_C,_I,_X,_X,_X,_X },
    { /*Z*/ _X,_X,_X,_F,_Z,_Z,_Z,_b,_B,_s,_S,_C,_I,_X,_X,_X,_X },
    { /*b*/ _X,_X,_X,_F,_b,_b,_b,_b,_B,_s,_S,_C,_I,_X,_X,_X,_X },
    { /*B*/ _X,_X,_X,_F,_B,_B,_B,_B,_B,_S,_S,_I,_I,_X,_X,_X,_X },
    { /*s*/ _X,_X,_X,_F,_s,_s,_s,_s,_S,_s,_S,_C,_I,_X,_X,_X,_X },
    { /*S*/ _X,_X,_X,_F,_S,_S,_S,_S,_S,_S,_S,_I,_I,_X,_X,_X,_X },
    { /*C*/ _X,_X,_X,_F,_C,_C,_C,_C,_I,_C,_I,_C,_I,_X,_X,_X,_X },
    { /*I*/ _X,_X,_X,_F,_I,_I,_I,_I,_I,_I,_I,_I,_I,_X,_X,_X,_X },
    { /*J*/ _X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_J,_X,_J,_X },
    { /*j*/ _X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_j,_X,_j },
    { /*D*/ _X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_J,_X,_D,_X },
    { /*d*/ _X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_X,_j,_X,_d },
};

#undef __
#undef _U
#undef _X
#undef _F
#undef _0
#undef _1
#undef _Z
#undef _b
#undef _B
#undef _s
#undef _S
#undef _C
#undef _I
#undef _J
#undef _j
#undef _D
#undef _d

#ifndef NDEBUG
/*
 * Verify symmetry in the conversion table.
 */
static void checkMergeTab(void)
{
    int i, j;

    for (i = 0; i < kRegTypeMAX; i++) {
        for (j = i; j < kRegTypeMAX; j++) {
            if (gDvmMergeTab[i][j] != gDvmMergeTab[j][i]) {
                LOGE("Symmetry violation: %d,%d vs %d,%d\n", i, j, j, i);
                dvmAbort();
            }
        }
    }
}
#endif

/*
 * Determine whether we can convert "srcType" to "checkType", where
 * "checkType" is one of the category-1 non-reference types.
 *
 * 32-bit int and float are interchangeable.
 */
static bool canConvertTo1nr(RegType srcType, RegType checkType)
{
    static const char convTab
        [kRegType1nrEND-kRegType1nrSTART+1][kRegType1nrEND-kRegType1nrSTART+1] =
    {
        /* chk: F  0  1  Z  b  B  s  S  C  I */
        { /*F*/ 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
        { /*0*/ 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
        { /*1*/ 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
        { /*Z*/ 1, 0, 0, 1, 1, 1, 1, 1, 1, 1 },
        { /*b*/ 1, 0, 0, 0, 1, 1, 1, 1, 1, 1 },
        { /*B*/ 1, 0, 0, 0, 0, 1, 0, 1, 0, 1 },
        { /*s*/ 1, 0, 0, 0, 0, 0, 1, 1, 1, 1 },
        { /*S*/ 1, 0, 0, 0, 0, 0, 0, 1, 0, 1 },
        { /*C*/ 1, 0, 0, 0, 0, 0, 0, 0, 1, 1 },
        { /*I*/ 1, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
    };

    assert(checkType >= kRegType1nrSTART && checkType <= kRegType1nrEND);
#if 0
    if (checkType < kRegType1nrSTART || checkType > kRegType1nrEND) {
        LOG_VFY("Unexpected checkType %d (srcType=%d)\n", checkType, srcType);
        assert(false);
        return false;
    }
#endif

    //printf("convTab[%d][%d] = %d\n", srcType, checkType,
    //    convTab[srcType-kRegType1nrSTART][checkType-kRegType1nrSTART]);
    if (srcType >= kRegType1nrSTART && srcType <= kRegType1nrEND)
        return (bool) convTab[srcType-kRegType1nrSTART][checkType-kRegType1nrSTART];

    return false;
}

/*
 * Determine whether the types are compatible.  In Dalvik, 64-bit doubles
 * and longs are interchangeable.
 */
static bool canConvertTo2(RegType srcType, RegType checkType)
{
    return ((srcType == kRegTypeLongLo || srcType == kRegTypeDoubleLo) &&
            (checkType == kRegTypeLongLo || checkType == kRegTypeDoubleLo));
}

/*
 * Determine whether or not "instrType" and "targetType" are compatible,
 * for purposes of getting or setting a value in a field or array.  The
 * idea is that an instruction with a category 1nr type (say, aget-short
 * or iput-boolean) is accessing a static field, instance field, or array
 * entry, and we want to make sure sure that the operation is legal.
 *
 * At a minimum, source and destination must have the same width.  We
 * further refine this to assert that "short" and "char" are not
 * compatible, because the sign-extension is different on the "get"
 * operations.  As usual, "float" and "int" are interoperable.
 *
 * We're not considering the actual contents of the register, so we'll
 * never get "pseudo-types" like kRegTypeZero or kRegTypePosShort.  We
 * could get kRegTypeUnknown in "targetType" if a field or array class
 * lookup failed.  Category 2 types and references are checked elsewhere.
 */
static bool checkFieldArrayStore1nr(RegType instrType, RegType targetType)
{
    if (instrType == targetType)
        return true;            /* quick positive; most common case */

    if ((instrType == kRegTypeInteger && targetType == kRegTypeFloat) ||
        (instrType == kRegTypeFloat && targetType == kRegTypeInteger))
    {
        return true;
    }

    return false;
}

/*
 * Convert a VM PrimitiveType enum value to the equivalent RegType value.
 */
static RegType primitiveTypeToRegType(PrimitiveType primType)
{
    static const struct {
        RegType         regType;        /* type equivalent */
        PrimitiveType   primType;       /* verification */
    } convTab[] = {
        /* must match order of enum in Object.h */
        { kRegTypeBoolean,      PRIM_BOOLEAN },
        { kRegTypeChar,         PRIM_CHAR },
        { kRegTypeFloat,        PRIM_FLOAT },
        { kRegTypeDoubleLo,     PRIM_DOUBLE },
        { kRegTypeByte,         PRIM_BYTE },
        { kRegTypeShort,        PRIM_SHORT },
        { kRegTypeInteger,      PRIM_INT },
        { kRegTypeLongLo,       PRIM_LONG },
        // PRIM_VOID
    };

    if (primType < 0 || primType > (int) (sizeof(convTab) / sizeof(convTab[0])))
    {
        assert(false);
        return kRegTypeUnknown;
    }

    assert(convTab[primType].primType == primType);
    return convTab[primType].regType;
}

/*
 * Create a new uninitialized instance map.
 *
 * The map is allocated and populated with address entries.  The addresses
 * appear in ascending order to allow binary searching.
 *
 * Very few methods have 10 or more new-instance instructions; the
 * majority have 0 or 1.  Occasionally a static initializer will have 200+.
 */
UninitInstanceMap* dvmCreateUninitInstanceMap(const Method* meth,
    const InsnFlags* insnFlags, int newInstanceCount)
{
    const int insnsSize = dvmGetMethodInsnsSize(meth);
    const u2* insns = meth->insns;
    UninitInstanceMap* uninitMap;
    bool isInit = false;
    int idx, addr;

    if (isInitMethod(meth)) {
        newInstanceCount++;
        isInit = true;
    }

    /*
     * Allocate the header and map as a single unit.
     *
     * TODO: consider having a static instance so we can avoid allocations.
     * I don't think the verifier is guaranteed to be single-threaded when
     * running in the VM (rather than dexopt), so that must be taken into
     * account.
     */
    int size = offsetof(UninitInstanceMap, map) +
                newInstanceCount * sizeof(uninitMap->map[0]);
    uninitMap = calloc(1, size);
    if (uninitMap == NULL)
        return NULL;
    uninitMap->numEntries = newInstanceCount;

    idx = 0;
    if (isInit) {
        uninitMap->map[idx++].addr = kUninitThisArgAddr;
    }

    /*
     * Run through and find the new-instance instructions.
     */
    for (addr = 0; addr < insnsSize; /**/) {
        int width = dvmInsnGetWidth(insnFlags, addr);

        if ((*insns & 0xff) == OP_NEW_INSTANCE)
            uninitMap->map[idx++].addr = addr;

        addr += width;
        insns += width;
    }

    assert(idx == newInstanceCount);
    return uninitMap;
}

/*
 * Free the map.
 */
void dvmFreeUninitInstanceMap(UninitInstanceMap* uninitMap)
{
    free(uninitMap);
}

/*
 * Set the class object associated with the instruction at "addr".
 *
 * Returns the map slot index, or -1 if the address isn't listed in the map
 * (shouldn't happen) or if a class is already associated with the address
 * (bad bytecode).
 *
 * Entries, once set, do not change -- a given address can only allocate
 * one type of object.
 */
int dvmSetUninitInstance(UninitInstanceMap* uninitMap, int addr,
    ClassObject* clazz)
{
    int idx;

    assert(clazz != NULL);

    /* TODO: binary search when numEntries > 8 */
    for (idx = uninitMap->numEntries - 1; idx >= 0; idx--) {
        if (uninitMap->map[idx].addr == addr) {
            if (uninitMap->map[idx].clazz != NULL &&
                uninitMap->map[idx].clazz != clazz)
            {
                LOG_VFY("VFY: addr %d already set to %p, not setting to %p\n",
                    addr, uninitMap->map[idx].clazz, clazz);
                return -1;          // already set to something else??
            }
            uninitMap->map[idx].clazz = clazz;
            return idx;
        }
    }

    LOG_VFY("VFY: addr %d not found in uninit map\n", addr);
    assert(false);      // shouldn't happen
    return -1;
}

/*
 * Get the class object at the specified index.
 */
ClassObject* dvmGetUninitInstance(const UninitInstanceMap* uninitMap, int idx)
{
    assert(idx >= 0 && idx < uninitMap->numEntries);
    return uninitMap->map[idx].clazz;
}

/* determine if "type" is actually an object reference (init/uninit/zero) */
static inline bool regTypeIsReference(RegType type) {
    return (type > kRegTypeMAX || type == kRegTypeUninit ||
            type == kRegTypeZero);
}

/* determine if "type" is an uninitialized object reference */
static inline bool regTypeIsUninitReference(RegType type) {
    return ((type & kRegTypeUninitMask) == kRegTypeUninit);
}

/* convert the initialized reference "type" to a ClassObject pointer */
/* (does not expect uninit ref types or "zero") */
static ClassObject* regTypeInitializedReferenceToClass(RegType type)
{
    assert(regTypeIsReference(type) && type != kRegTypeZero);
    if ((type & 0x01) == 0) {
        return (ClassObject*) type;
    } else {
        //LOG_VFY("VFY: attempted to use uninitialized reference\n");
        return NULL;
    }
}

/* extract the index into the uninitialized instance map table */
static inline int regTypeToUninitIndex(RegType type) {
    assert(regTypeIsUninitReference(type));
    return (type & ~kRegTypeUninitMask) >> kRegTypeUninitShift;
}

/* convert the reference "type" to a ClassObject pointer */
static ClassObject* regTypeReferenceToClass(RegType type,
    const UninitInstanceMap* uninitMap)
{
    assert(regTypeIsReference(type) && type != kRegTypeZero);
    if (regTypeIsUninitReference(type)) {
        assert(uninitMap != NULL);
        return dvmGetUninitInstance(uninitMap, regTypeToUninitIndex(type));
    } else {
        return (ClassObject*) type;
    }
}

/* convert the ClassObject pointer to an (initialized) register type */
static inline RegType regTypeFromClass(ClassObject* clazz) {
    return (u4) clazz;
}

/* return the RegType for the uninitialized reference in slot "uidx" */
static RegType regTypeFromUninitIndex(int uidx) {
    return (u4) (kRegTypeUninit | (uidx << kRegTypeUninitShift));
}


/*
 * ===========================================================================
 *      Signature operations
 * ===========================================================================
 */

/*
 * Is this method a constructor?
 */
static bool isInitMethod(const Method* meth)
{
    return (*meth->name == '<' && strcmp(meth->name+1, "init>") == 0);
}

/*
 * Is this method a class initializer?
 */
static bool isClassInitMethod(const Method* meth)
{
    return (*meth->name == '<' && strcmp(meth->name+1, "clinit>") == 0);
}

/*
 * Look up a class reference given as a simple string descriptor.
 */
static ClassObject* lookupClassByDescriptor(const Method* meth,
    const char* pDescriptor, bool* pOkay)
{
    /*
     * The javac compiler occasionally puts references to nonexistent
     * classes in signatures.  For example, if you have a non-static
     * inner class with no constructor, the compiler provides
     * a private <init> for you.  Constructing the class
     * requires <init>(parent), but the outer class can't call
     * that because the method is private.  So the compiler
     * generates a package-scope <init>(parent,bogus) method that
     * just calls the regular <init> (the "bogus" part being necessary
     * to distinguish the signature of the synthetic method).
     * Treating the bogus class as an instance of java.lang.Object
     * allows the verifier to process the class successfully.
     */

    //LOGI("Looking up '%s'\n", typeStr);
    ClassObject* clazz;
    clazz = dvmFindClassNoInit(pDescriptor, meth->clazz->classLoader);
    if (clazz == NULL) {
        dvmClearOptException(dvmThreadSelf());
        if (strchr(pDescriptor, '$') != NULL) {
            LOGV("VFY: unable to find class referenced in signature (%s)\n",
                pDescriptor);
        } else {
            LOG_VFY("VFY: unable to find class referenced in signature (%s)\n",
                pDescriptor);
        }

        if (pDescriptor[0] == '[') {
            /* We are looking at an array descriptor. */

            /*
             * There should never be a problem loading primitive arrays.
             */
            if (pDescriptor[1] != 'L' && pDescriptor[1] != '[') {
                LOG_VFY("VFY: invalid char in signature in '%s'\n",
                    pDescriptor);
                *pOkay = false;
            }

            /*
             * Try to continue with base array type.  This will let
             * us pass basic stuff (e.g. get array len) that wouldn't
             * fly with an Object.  This is NOT correct if the
             * missing type is a primitive array, but we should never
             * have a problem loading those.  (I'm not convinced this
             * is correct or even useful.  Just use Object here?)
             */
            clazz = dvmFindClassNoInit("[Ljava/lang/Object;",
                meth->clazz->classLoader);
        } else if (pDescriptor[0] == 'L') {
            /*
             * We are looking at a non-array reference descriptor;
             * try to continue with base reference type.
             */
            clazz = gDvm.classJavaLangObject;
        } else {
            /* We are looking at a primitive type. */
            LOG_VFY("VFY: invalid char in signature in '%s'\n", pDescriptor);
            *pOkay = false;
        }

        if (clazz == NULL) {
            *pOkay = false;
        }
    }

    if (dvmIsPrimitiveClass(clazz)) {
        LOG_VFY("VFY: invalid use of primitive type '%s'\n", pDescriptor);
        *pOkay = false;
        clazz = NULL;
    }

    return clazz;
}

/*
 * Look up a class reference in a signature.  Could be an arg or the
 * return value.
 *
 * Advances "*pSig" to the last character in the signature (that is, to
 * the ';').
 *
 * NOTE: this is also expected to verify the signature.
 */
static ClassObject* lookupSignatureClass(const Method* meth, const char** pSig,
    bool* pOkay)
{
    const char* sig = *pSig;
    const char* endp = sig;

    assert(sig != NULL && *sig == 'L');

    while (*++endp != ';' && *endp != '\0')
        ;
    if (*endp != ';') {
        LOG_VFY("VFY: bad signature component '%s' (missing ';')\n", sig);
        *pOkay = false;
        return NULL;
    }

    endp++;    /* Advance past the ';'. */
    int typeLen = endp - sig;
    char typeStr[typeLen+1]; /* +1 for the '\0' */
    memcpy(typeStr, sig, typeLen);
    typeStr[typeLen] = '\0';

    *pSig = endp - 1; /* - 1 so that *pSig points at, not past, the ';' */

    return lookupClassByDescriptor(meth, typeStr, pOkay);
}

/*
 * Look up an array class reference in a signature.  Could be an arg or the
 * return value.
 *
 * Advances "*pSig" to the last character in the signature.
 *
 * NOTE: this is also expected to verify the signature.
 */
static ClassObject* lookupSignatureArrayClass(const Method* meth,
    const char** pSig, bool* pOkay)
{
    const char* sig = *pSig;
    const char* endp = sig;

    assert(sig != NULL && *sig == '[');

    /* find the end */
    while (*++endp == '[' && *endp != '\0')
        ;

    if (*endp == 'L') {
        while (*++endp != ';' && *endp != '\0')
            ;
        if (*endp != ';') {
            LOG_VFY("VFY: bad signature component '%s' (missing ';')\n", sig);
            *pOkay = false;
            return NULL;
        }
    }

    int typeLen = endp - sig +1;
    char typeStr[typeLen+1];
    memcpy(typeStr, sig, typeLen);
    typeStr[typeLen] = '\0';

    *pSig = endp;

    return lookupClassByDescriptor(meth, typeStr, pOkay);
}

/*
 * Set the register types for the first instruction in the method based on
 * the method signature.
 *
 * This has the side-effect of validating the signature.
 *
 * Returns "true" on success.
 */
static bool setTypesFromSignature(const Method* meth, RegType* regTypes,
    UninitInstanceMap* uninitMap)
{
    DexParameterIterator iterator;
    int actualArgs, expectedArgs, argStart;
    bool okay = true;

    dexParameterIteratorInit(&iterator, &meth->prototype);
    argStart = meth->registersSize - meth->insSize;
    expectedArgs = meth->insSize;     /* long/double count as two */
    actualArgs = 0;

    assert(argStart >= 0);      /* should have been verified earlier */

    /*
     * Include the "this" pointer.
     */
    if (!dvmIsStaticMethod(meth)) {
        /*
         * If this is a constructor for a class other than java.lang.Object,
         * mark the first ("this") argument as uninitialized.  This restricts
         * field access until the superclass constructor is called.
         */
        if (isInitMethod(meth) && meth->clazz != gDvm.classJavaLangObject) {
            int uidx = dvmSetUninitInstance(uninitMap, kUninitThisArgAddr,
                            meth->clazz);
            assert(uidx == 0);
            regTypes[argStart + actualArgs] = regTypeFromUninitIndex(uidx);
        } else {
            regTypes[argStart + actualArgs] = regTypeFromClass(meth->clazz);
        }
        actualArgs++;
    }

    for (;;) {
        const char* descriptor = dexParameterIteratorNextDescriptor(&iterator);

        if (descriptor == NULL) {
            break;
        }

        if (actualArgs >= expectedArgs) {
            LOG_VFY("VFY: expected %d args, found more (%s)\n",
                expectedArgs, descriptor);
            goto bad_sig;
        }

        switch (*descriptor) {
        case 'L':
        case '[':
            /*
             * We assume that reference arguments are initialized.  The
             * only way it could be otherwise (assuming the caller was
             * verified) is if the current method is <init>, but in that
             * case it's effectively considered initialized the instant
             * we reach here (in the sense that we can return without
             * doing anything or call virtual methods).
             */
            {
                ClassObject* clazz =
                    lookupClassByDescriptor(meth, descriptor, &okay);
                if (!okay)
                    goto bad_sig;
                regTypes[argStart + actualArgs] = regTypeFromClass(clazz);
            }
            actualArgs++;
            break;
        case 'Z':
            regTypes[argStart + actualArgs] = kRegTypeBoolean;
            actualArgs++;
            break;
        case 'C':
            regTypes[argStart + actualArgs] = kRegTypeChar;
            actualArgs++;
            break;
        case 'B':
            regTypes[argStart + actualArgs] = kRegTypeByte;
            actualArgs++;
            break;
        case 'I':
            regTypes[argStart + actualArgs] = kRegTypeInteger;
            actualArgs++;
            break;
        case 'S':
            regTypes[argStart + actualArgs] = kRegTypeShort;
            actualArgs++;
            break;
        case 'F':
            regTypes[argStart + actualArgs] = kRegTypeFloat;
            actualArgs++;
            break;
        case 'D':
            regTypes[argStart + actualArgs] = kRegTypeDoubleLo;
            regTypes[argStart + actualArgs +1] = kRegTypeDoubleHi;
            actualArgs += 2;
            break;
        case 'J':
            regTypes[argStart + actualArgs] = kRegTypeLongLo;
            regTypes[argStart + actualArgs +1] = kRegTypeLongHi;
            actualArgs += 2;
            break;
        default:
            LOG_VFY("VFY: unexpected signature type char '%c'\n", *descriptor);
            goto bad_sig;
        }
    }

    if (actualArgs != expectedArgs) {
        LOG_VFY("VFY: expected %d args, found %d\n", expectedArgs, actualArgs);
        goto bad_sig;
    }

    const char* descriptor = dexProtoGetReturnType(&meth->prototype);

    /*
     * Validate return type.  We don't do the type lookup; just want to make
     * sure that it has the right format.  Only major difference from the
     * method argument format is that 'V' is supported.
     */
    switch (*descriptor) {
    case 'I':
    case 'C':
    case 'S':
    case 'B':
    case 'Z':
    case 'V':
    case 'F':
    case 'D':
    case 'J':
        if (*(descriptor+1) != '\0')
            goto bad_sig;
        break;
    case '[':
        /* single/multi, object/primitive */
        while (*++descriptor == '[')
            ;
        if (*descriptor == 'L') {
            while (*++descriptor != ';' && *descriptor != '\0')
                ;
            if (*descriptor != ';')
                goto bad_sig;
        } else {
            if (*(descriptor+1) != '\0')
                goto bad_sig;
        }
        break;
    case 'L':
        /* could be more thorough here, but shouldn't be required */
        while (*++descriptor != ';' && *descriptor != '\0')
            ;
        if (*descriptor != ';')
            goto bad_sig;
        break;
    default:
        goto bad_sig;
    }

    return true;

//fail:
//    LOG_VFY_METH(meth, "VFY:  bad sig\n");
//    return false;

bad_sig:
    {
        char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
        LOG_VFY("VFY: bad signature '%s' for %s.%s\n",
            desc, meth->clazz->descriptor, meth->name);
        free(desc);
    }
    return false;
}

/*
 * Return the register type for the method.  We can't just use the
 * already-computed DalvikJniReturnType, because if it's a reference type
 * we need to do the class lookup.
 *
 * Returned references are assumed to be initialized.
 *
 * Returns kRegTypeUnknown for "void".
 */
static RegType getMethodReturnType(const Method* meth)
{
    RegType type;
    const char* descriptor = dexProtoGetReturnType(&meth->prototype);

    switch (*descriptor) {
    case 'I':
        type = kRegTypeInteger;
        break;
    case 'C':
        type = kRegTypeChar;
        break;
    case 'S':
        type = kRegTypeShort;
        break;
    case 'B':
        type = kRegTypeByte;
        break;
    case 'Z':
        type = kRegTypeBoolean;
        break;
    case 'V':
        type = kRegTypeUnknown;
        break;
    case 'F':
        type = kRegTypeFloat;
        break;
    case 'D':
        type = kRegTypeDoubleLo;
        break;
    case 'J':
        type = kRegTypeLongLo;
        break;
    case 'L':
    case '[':
        {
            bool okay = true;
            ClassObject* clazz =
                lookupClassByDescriptor(meth, descriptor, &okay);
            assert(okay);
            type = regTypeFromClass(clazz);
        }
        break;
    default:
        /* we verified signature return type earlier, so this is impossible */
        assert(false);
        type = kRegTypeConflict;
        break;
    }

    return type;
}

/*
 * Convert a single-character signature value (i.e. a primitive type) to
 * the corresponding RegType.  This is intended for access to object fields
 * holding primitive types.
 *
 * Returns kRegTypeUnknown for objects, arrays, and void.
 */
static RegType primSigCharToRegType(char sigChar)
{
    RegType type;

    switch (sigChar) {
    case 'I':
        type = kRegTypeInteger;
        break;
    case 'C':
        type = kRegTypeChar;
        break;
    case 'S':
        type = kRegTypeShort;
        break;
    case 'B':
        type = kRegTypeByte;
        break;
    case 'Z':
        type = kRegTypeBoolean;
        break;
    case 'F':
        type = kRegTypeFloat;
        break;
    case 'D':
        type = kRegTypeDoubleLo;
        break;
    case 'J':
        type = kRegTypeLongLo;
        break;
    case 'V':
    case 'L':
    case '[':
        type = kRegTypeUnknown;
        break;
    default:
        assert(false);
        type = kRegTypeUnknown;
        break;
    }

    return type;
}

/*
 * Verify the arguments to a method.  We're executing in "method", making
 * a call to the method reference in vB.
 *
 * If this is a "direct" invoke, we allow calls to <init>.  For calls to
 * <init>, the first argument may be an uninitialized reference.  Otherwise,
 * calls to anything starting with '<' will be rejected, as will any
 * uninitialized reference arguments.
 *
 * For non-static method calls, this will verify that the method call is
 * appropriate for the "this" argument.
 *
 * The method reference is in vBBBB.  The "isRange" parameter determines
 * whether we use 0-4 "args" values or a range of registers defined by
 * vAA and vCCCC.
 *
 * Widening conversions on integers and references are allowed, but
 * narrowing conversions are not.
 *
 * Returns the resolved method on success, NULL (and sets "*pOkay" to "false")
 * on failure.
 */
static Method* verifyInvocationArgs(const Method* meth, const RegType* insnRegs,
    const int insnRegCount, const DecodedInstruction* pDecInsn,
    UninitInstanceMap* uninitMap, MethodType methodType, bool isRange,
    bool isSuper, bool* pOkay)
{
    Method* resMethod;
    char* sigOriginal = NULL;

    /*
     * Resolve the method.  This could be an abstract or concrete method
     * depending on what sort of call we're making.
     */
    if (methodType == METHOD_INTERFACE) {
        resMethod = dvmOptResolveInterfaceMethod(meth->clazz, pDecInsn->vB);
    } else {
        resMethod = dvmOptResolveMethod(meth->clazz, pDecInsn->vB, methodType);
    }
    if (resMethod == NULL) {
        /* failed; print a meaningful failure message */
        DexFile* pDexFile = meth->clazz->pDvmDex->pDexFile;
        const DexMethodId* pMethodId;
        const char* methodName;
        char* methodDesc;
        const char* classDescriptor;

        pMethodId = dexGetMethodId(pDexFile, pDecInsn->vB);
        methodName = dexStringById(pDexFile, pMethodId->nameIdx);
        methodDesc = dexCopyDescriptorFromMethodId(pDexFile, pMethodId);
        classDescriptor = dexStringByTypeIdx(pDexFile, pMethodId->classIdx);

        if (!gDvm.optimizing) {
            char* dotMissingClass = dvmDescriptorToDot(classDescriptor);
            char* dotMethClass = dvmDescriptorToDot(meth->clazz->descriptor);
            //char* curMethodDesc =
            //    dexProtoCopyMethodDescriptor(&meth->prototype);

            LOGE("Could not find method %s.%s, referenced from "
                 "method %s.%s\n",
                 dotMissingClass, methodName/*, methodDesc*/,
                 dotMethClass, meth->name/*, curMethodDesc*/);

            free(dotMissingClass);
            free(dotMethClass);
            //free(curMethodDesc);
        }

        LOG_VFY("VFY: unable to resolve %s method %u: %s.%s %s\n",
            dvmMethodTypeStr(methodType), pDecInsn->vB,
            classDescriptor, methodName, methodDesc);
        free(methodDesc);
        goto fail;
    }

    /*
     * Only time you can explicitly call a method starting with '<' is when
     * making a "direct" invocation on "<init>".  There are additional
     * restrictions but we don't enforce them here.
     */
    if (resMethod->name[0] == '<') {
        if (methodType != METHOD_DIRECT || !isInitMethod(resMethod)) {
            LOG_VFY("VFY: invalid call to %s.%s\n",
                    resMethod->clazz->descriptor, resMethod->name);
            goto bad_sig;
        }
    }

    /*
     * If we're using invoke-super(method), make sure that the executing
     * method's class' superclass has a vtable entry for the target method.
     */
    if (isSuper) {
        assert(methodType == METHOD_VIRTUAL);
        ClassObject* super = meth->clazz->super;
        if (super == NULL || resMethod->methodIndex > super->vtableCount) {
            char* desc = dexProtoCopyMethodDescriptor(&resMethod->prototype);
            LOG_VFY("VFY: invalid invoke-super from %s.%s to super %s.%s %s\n",
                    meth->clazz->descriptor, meth->name,
                    (super == NULL) ? "-" : super->descriptor,
                    resMethod->name, desc);
            free(desc);
            goto fail;
        }
    }

    /*
     * We use vAA as our expected arg count, rather than resMethod->insSize,
     * because we need to match the call to the signature.  Also, we might
     * might be calling through an abstract method definition (which doesn't
     * have register count values).
     */
    sigOriginal = dexProtoCopyMethodDescriptor(&resMethod->prototype);
    const char* sig = sigOriginal;
    int expectedArgs = pDecInsn->vA;
    int actualArgs = 0;

    if (!isRange && expectedArgs > 5) {
        LOG_VFY("VFY: invalid arg count in non-range invoke (%d)\n",
            pDecInsn->vA);
        goto fail;
    }
    if (expectedArgs > meth->outsSize) {
        LOG_VFY("VFY: invalid arg count (%d) exceeds outsSize (%d)\n",
            expectedArgs, meth->outsSize);
        goto fail;
    }

    if (*sig++ != '(')
        goto bad_sig;

    /*
     * Check the "this" argument, which must be an instance of the class
     * that declared the method.  For an interface class, we don't do the
     * full interface merge, so we can't do a rigorous check here (which
     * is okay since we have to do it at runtime).
     */
    if (!dvmIsStaticMethod(resMethod)) {
        ClassObject* actualThisRef;
        RegType actualArgType;

        actualArgType = getInvocationThis(insnRegs, insnRegCount, pDecInsn,
                            pOkay);
        if (!*pOkay)
            goto fail;

        if (regTypeIsUninitReference(actualArgType) && resMethod->name[0] != '<')
        {
            LOG_VFY("VFY: 'this' arg must be initialized\n");
            goto fail;
        }
        if (methodType != METHOD_INTERFACE && actualArgType != kRegTypeZero) {
            actualThisRef = regTypeReferenceToClass(actualArgType, uninitMap);
            if (!dvmInstanceof(actualThisRef, resMethod->clazz)) {
                LOG_VFY("VFY: 'this' arg '%s' not instance of '%s'\n",
                        actualThisRef->descriptor,
                        resMethod->clazz->descriptor);
                goto fail;
            }
        }
        actualArgs++;
    }

    /*
     * Process the target method's signature.  This signature may or may not
     * have been verified, so we can't assume it's properly formed.
     */
    while (*sig != '\0' && *sig != ')') {
        if (actualArgs >= expectedArgs) {
            LOG_VFY("VFY: expected %d args, found more (%c)\n",
                expectedArgs, *sig);
            goto bad_sig;
        }

        u4 getReg;
        if (isRange)
            getReg = pDecInsn->vC + actualArgs;
        else
            getReg = pDecInsn->arg[actualArgs];

        switch (*sig) {
        case 'L':
            {
                ClassObject* clazz = lookupSignatureClass(meth, &sig, pOkay);
                if (!*pOkay)
                    goto bad_sig;
                verifyRegisterType(insnRegs, insnRegCount, getReg,
                    regTypeFromClass(clazz), pOkay);
                if (!*pOkay) {
                    LOG_VFY("VFY: bad arg %d (into %s)\n",
                            actualArgs, clazz->descriptor);
                    goto bad_sig;
                }
            }
            actualArgs++;
            break;
        case '[':
            {
                ClassObject* clazz =
                    lookupSignatureArrayClass(meth, &sig, pOkay);
                if (!*pOkay)
                    goto bad_sig;
                verifyRegisterType(insnRegs, insnRegCount, getReg,
                    regTypeFromClass(clazz), pOkay);
                if (!*pOkay) {
                    LOG_VFY("VFY: bad arg %d (into %s)\n",
                            actualArgs, clazz->descriptor);
                    goto bad_sig;
                }
            }
            actualArgs++;
            break;
        case 'Z':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeBoolean, pOkay);
            actualArgs++;
            break;
        case 'C':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeChar, pOkay);
            actualArgs++;
            break;
        case 'B':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeByte, pOkay);
            actualArgs++;
            break;
        case 'I':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeInteger, pOkay);
            actualArgs++;
            break;
        case 'S':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeShort, pOkay);
            actualArgs++;
            break;
        case 'F':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeFloat, pOkay);
            actualArgs++;
            break;
        case 'D':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeDoubleLo, pOkay);
            actualArgs += 2;
            break;
        case 'J':
            verifyRegisterType(insnRegs, insnRegCount, getReg,
                kRegTypeLongLo, pOkay);
            actualArgs += 2;
            break;
        default:
            LOG_VFY("VFY: invocation target: bad signature type char '%c'\n",
                *sig);
            goto bad_sig;
        }

        sig++;
    }
    if (*sig != ')') {
        char* desc = dexProtoCopyMethodDescriptor(&resMethod->prototype);
        LOG_VFY("VFY: invocation target: bad signature '%s'\n", desc);
        free(desc);
        goto bad_sig;
    }

    if (actualArgs != expectedArgs) {
        LOG_VFY("VFY: expected %d args, found %d\n", expectedArgs, actualArgs);
        goto bad_sig;
    }

    free(sigOriginal);
    return resMethod;

bad_sig:
    if (resMethod != NULL) {
        char* desc = dexProtoCopyMethodDescriptor(&resMethod->prototype);
        LOG_VFY("VFY:  rejecting call to %s.%s %s\n",
                resMethod->clazz->descriptor, resMethod->name, desc);
        free(desc);
    }

fail:
    free(sigOriginal);
    *pOkay = false;
    return NULL;
}

/*
 * Get the class object for the type of data stored in a field.  This isn't
 * stored in the Field struct, so we have to recover it from the signature.
 *
 * This only works for reference types.  Don't call this for primitive types.
 *
 * If we can't find the class, we return java.lang.Object, so that
 * verification can continue if a field is only accessed in trivial ways.
 */
static ClassObject* getFieldClass(const Method* meth, const Field* field)
{
    ClassObject* fieldClass;
    const char* signature = field->signature;

    if ((*signature == 'L') || (*signature == '[')) {
        fieldClass = dvmFindClassNoInit(signature,
                meth->clazz->classLoader);
    } else {
        return NULL;
    }

    if (fieldClass == NULL) {
        dvmClearOptException(dvmThreadSelf());
        LOGV("VFY: unable to find class '%s' for field %s.%s, trying Object\n",
            field->signature, meth->clazz->descriptor, field->name);
        fieldClass = gDvm.classJavaLangObject;
    } else {
        assert(!dvmIsPrimitiveClass(fieldClass));
    }
    return fieldClass;
}


/*
 * ===========================================================================
 *      Register operations
 * ===========================================================================
 */

/*
 * Get the type of register N, verifying that the register is valid.
 *
 * Sets "*pOkay" to false if the register number is out of range.
 */
static inline RegType getRegisterType(const RegType* insnRegs,
    const int insnRegCount, u4 vsrc, bool* pOkay)
{
    RegType type;

    if (vsrc >= (u4) insnRegCount) {
        *pOkay = false;
        return kRegTypeUnknown;
    } else {
        return insnRegs[vsrc];
    }
}

/*
 * Get the value from a register, and cast it to a ClassObject.  Sets
 * "pOkay" to false if something fails.
 *
 * This fails if the register holds an uninitialized class.
 *
 * If the register holds kRegTypeZero, this returns a NULL pointer.
 */
static ClassObject* getClassFromRegister(const RegType* insnRegs,
    const int insnRegCount, u4 vsrc, bool* pOkay)
{
    ClassObject* clazz = NULL;
    RegType type;

    /* get the element type of the array held in vsrc */
    type = getRegisterType(insnRegs, insnRegCount, vsrc, pOkay);
    if (!*pOkay)
        goto bail;

    /* if "always zero", we allow it to fail at runtime */
    if (type == kRegTypeZero)
        goto bail;

    if (!regTypeIsReference(type)) {
        LOG_VFY("VFY: tried to get class from non-ref register v%d (type=%d)\n",
            vsrc, type);
        *pOkay = false;
        goto bail;
    }
    if (regTypeIsUninitReference(type)) {
        LOG_VFY("VFY: register %u holds uninitialized reference\n", vsrc);
        *pOkay = false;
        goto bail;
    }

    clazz = regTypeInitializedReferenceToClass(type);

bail:
    return clazz;
}

/*
 * Get the "this" pointer from a non-static method invocation.  This
 * returns the RegType so the caller can decide whether it needs the
 * reference to be initialized or not.  (Can also return kRegTypeZero
 * if the reference can only be zero at this point.)
 *
 * The argument count is in vA, and the first argument is in vC, for both
 * "simple" and "range" versions.  We just need to make sure vA is >= 1
 * and then return vC.
 */
static RegType getInvocationThis(const RegType* insnRegs,
    const int insnRegCount, const DecodedInstruction* pDecInsn, bool* pOkay)
{
    RegType thisType = kRegTypeUnknown;

    if (pDecInsn->vA < 1) {
        LOG_VFY("VFY: invoke lacks 'this'\n");
        *pOkay = false;
        goto bail;
    }

    /* get the element type of the array held in vsrc */
    thisType = getRegisterType(insnRegs, insnRegCount, pDecInsn->vC, pOkay);
    if (!*pOkay) {
        LOG_VFY("VFY: failed to get this from register %u\n", pDecInsn->vC);
        goto bail;
    }

    if (!regTypeIsReference(thisType)) {
        LOG_VFY("VFY: tried to get class from non-ref register v%d (type=%d)\n",
            pDecInsn->vC, thisType);
        *pOkay = false;
        goto bail;
    }

bail:
    return thisType;
}

/*
 * Set the type of register N, verifying that the register is valid.  If
 * "newType" is the "Lo" part of a 64-bit value, register N+1 will be
 * set to "newType+1".
 *
 * Sets "*pOkay" to false if the register number is out of range.
 */
static void setRegisterType(RegType* insnRegs, const int insnRegCount,
    u4 vdst, RegType newType, bool* pOkay)
{
    //LOGD("set-reg v%u = %d\n", vdst, newType);
    switch (newType) {
    case kRegTypeUnknown:
    case kRegTypeBoolean:
    case kRegTypeOne:
    case kRegTypeByte:
    case kRegTypePosByte:
    case kRegTypeShort:
    case kRegTypePosShort:
    case kRegTypeChar:
    case kRegTypeInteger:
    case kRegTypeFloat:
    case kRegTypeZero:
        if (vdst >= (u4) insnRegCount) {
            *pOkay = false;
        } else {
            insnRegs[vdst] = newType;
        }
        break;
    case kRegTypeLongLo:
    case kRegTypeDoubleLo:
        if (vdst+1 >= (u4) insnRegCount) {
            *pOkay = false;
        } else {
            insnRegs[vdst] = newType;
            insnRegs[vdst+1] = newType+1;
        }
        break;
    case kRegTypeLongHi:
    case kRegTypeDoubleHi:
        /* should never set these explicitly */
        *pOkay = false;
        break;

    case kRegTypeUninit:
    default:
        if (regTypeIsReference(newType)) {
            if (vdst >= (u4) insnRegCount) {
                *pOkay = false;
                break;
            }
            insnRegs[vdst] = newType;

            /*
             * In most circumstances we won't see a reference to a primitive
             * class here (e.g. "D"), since that would mean the object in the
             * register is actually a primitive type.  It can happen as the
             * result of an assumed-successful check-cast instruction in
             * which the second argument refers to a primitive class.  (In
             * practice, such an instruction will always throw an exception.)
             *
             * This is not an issue for instructions like const-class, where
             * the object in the register is a java.lang.Class instance.
             */
            break;
        }
        /* bad - fall through */

    case kRegTypeConflict:      // should only be set during a merge
        LOG_VFY("Unexpected set type %d\n", newType);
        assert(false);
        *pOkay = false;
        break;
    }
}

/*
 * Verify that the contents of the specified register have the specified
 * type (or can be converted to it through an implicit widening conversion).
 *
 * In theory we could use this to modify the type of the source register,
 * e.g. a generic 32-bit constant, once used as a float, would thereafter
 * remain a float.  There is no compelling reason to require this though.
 *
 * If "vsrc" is a reference, both it and the "vsrc" register must be
 * initialized ("vsrc" may be Zero).  This will verify that the value in
 * the register is an instance of checkType, or if checkType is an
 * interface, verify that the register implements checkType.
 */
static void verifyRegisterType(const RegType* insnRegs, const int insnRegCount,
    u4 vsrc, RegType checkType, bool* pOkay)
{
    if (vsrc >= (u4) insnRegCount) {
        *pOkay = false;
        return;
    }

    RegType srcType = insnRegs[vsrc];

    //LOGD("check-reg v%u = %d\n", vsrc, checkType);
    switch (checkType) {
    case kRegTypeFloat:
    case kRegTypeBoolean:
    case kRegTypePosByte:
    case kRegTypeByte:
    case kRegTypePosShort:
    case kRegTypeShort:
    case kRegTypeChar:
    case kRegTypeInteger:
        if (!canConvertTo1nr(srcType, checkType)) {
            LOG_VFY("VFY: register1 v%u type %d, wanted %d\n",
                vsrc, srcType, checkType);
            *pOkay = false;
        }
        break;
    case kRegTypeLongLo:
    case kRegTypeDoubleLo:
        if (vsrc+1 >= (u4) insnRegCount) {
            LOG_VFY("VFY: register2 v%u out of range (%d)\n",
                vsrc, insnRegCount);
            *pOkay = false;
        } else if (insnRegs[vsrc+1] != srcType+1) {
            LOG_VFY("VFY: register2 v%u-%u values %d,%d\n",
                vsrc, vsrc+1, insnRegs[vsrc], insnRegs[vsrc+1]);
            *pOkay = false;
        } else if (!canConvertTo2(srcType, checkType)) {
            LOG_VFY("VFY: register2 v%u type %d, wanted %d\n",
                vsrc, srcType, checkType);
            *pOkay = false;
        }
        break;

    case kRegTypeLongHi:
    case kRegTypeDoubleHi:
    case kRegTypeZero:
    case kRegTypeOne:
    case kRegTypeUnknown:
    case kRegTypeConflict:
        /* should never be checking for these explicitly */
        assert(false);
        *pOkay = false;
        return;
    case kRegTypeUninit:
    default:
        /* make sure checkType is initialized reference */
        if (!regTypeIsReference(checkType)) {
            LOG_VFY("VFY: unexpected check type %d\n", checkType);
            assert(false);
            *pOkay = false;
            break;
        }
        if (regTypeIsUninitReference(checkType)) {
            LOG_VFY("VFY: uninitialized ref not expected as reg check\n");
            *pOkay = false;
            break;
        }
        /* make sure srcType is initialized reference or always-NULL */
        if (!regTypeIsReference(srcType)) {
            LOG_VFY("VFY: register1 v%u type %d, wanted ref\n", vsrc, srcType);
            *pOkay = false;
            break;
        }
        if (regTypeIsUninitReference(srcType)) {
            LOG_VFY("VFY: register1 v%u holds uninitialized ref\n", vsrc);
            *pOkay = false;
            break;
        }
        /* if the register isn't Zero, make sure it's an instance of check */
        if (srcType != kRegTypeZero) {
            ClassObject* srcClass = regTypeInitializedReferenceToClass(srcType);
            ClassObject* checkClass = regTypeInitializedReferenceToClass(checkType);
            assert(srcClass != NULL);
            assert(checkClass != NULL);

            if (dvmIsInterfaceClass(checkClass)) {
                /*
                 * All objects implement all interfaces as far as the
                 * verifier is concerned.  The runtime has to sort it out.
                 * See comments above findCommonSuperclass.
                 */
                /*
                if (srcClass != checkClass &&
                    !dvmImplements(srcClass, checkClass))
                {
                    LOG_VFY("VFY: %s does not implement %s\n",
                            srcClass->descriptor, checkClass->descriptor);
                    *pOkay = false;
                }
                */
            } else {
                if (!dvmInstanceof(srcClass, checkClass)) {
                    LOG_VFY("VFY: %s is not instance of %s\n",
                            srcClass->descriptor, checkClass->descriptor);
                    *pOkay = false;
                }
            }
        }
        break;
    }
}

/*
 * Set the type of the "result" register.  Mostly this exists to expand
 * "insnRegCount" to encompass the result register.
 */
static void setResultRegisterType(RegType* insnRegs, const int insnRegCount,
    RegType newType, bool* pOkay)
{
    setRegisterType(insnRegs, insnRegCount + kExtraRegs,
        RESULT_REGISTER(insnRegCount), newType, pOkay);
}


/*
 * Update all registers holding "uninitType" to instead hold the
 * corresponding initialized reference type.  This is called when an
 * appropriate <init> method is invoked -- all copies of the reference
 * must be marked as initialized.
 */
static void markRefsAsInitialized(RegType* insnRegs, int insnRegCount,
    UninitInstanceMap* uninitMap, RegType uninitType, bool* pOkay)
{
    ClassObject* clazz;
    RegType initType;
    int i, changed;

    clazz = dvmGetUninitInstance(uninitMap, regTypeToUninitIndex(uninitType));
    if (clazz == NULL) {
        LOGE("VFY: unable to find type=0x%x (idx=%d)\n",
            uninitType, regTypeToUninitIndex(uninitType));
        *pOkay = false;
        return;
    }
    initType = regTypeFromClass(clazz);

    changed = 0;
    for (i = 0; i < insnRegCount; i++) {
        if (insnRegs[i] == uninitType) {
            insnRegs[i] = initType;
            changed++;
        }
    }
    //LOGD("VFY: marked %d registers as initialized\n", changed);
    assert(changed > 0);

    return;
}

/*
 * We're creating a new instance of class C at address A.  Any registers
 * holding instances previously created at address A must be initialized
 * by now.  If not, we mark them as "conflict" to prevent them from being
 * used (otherwise, markRefsAsInitialized would mark the old ones and the
 * new ones at the same time).
 */
static void markUninitRefsAsInvalid(RegType* insnRegs, int insnRegCount,
    UninitInstanceMap* uninitMap, RegType uninitType)
{
    int i, changed;

    changed = 0;
    for (i = 0; i < insnRegCount; i++) {
        if (insnRegs[i] == uninitType) {
            insnRegs[i] = kRegTypeConflict;
            changed++;
        }
    }

    //if (changed)
    //    LOGD("VFY: marked %d uninitialized registers as invalid\n", changed);
}

/*
 * Find the start of the register set for the specified instruction in
 * the current method.
 */
static inline RegType* getRegisterLine(const RegisterTable* regTable,
    int insnIdx)
{
    return regTable->addrRegs[insnIdx];
}

/*
 * Copy a bunch of registers.
 */
static inline void copyRegisters(RegType* dst, const RegType* src,
    int numRegs)
{
    memcpy(dst, src, numRegs * sizeof(RegType));
}

/*
 * Compare a bunch of registers.
 *
 * Returns 0 if they match.  Using this for a sort is unwise, since the
 * value can change based on machine endianness.
 */
static inline int compareRegisters(const RegType* src1, const RegType* src2,
    int numRegs)
{
    return memcmp(src1, src2, numRegs * sizeof(RegType));
}

/*
 * Register type categories, for type checking.
 *
 * The spec says category 1 includes boolean, byte, char, short, int, float,
 * reference, and returnAddress.  Category 2 includes long and double.
 *
 * We treat object references separately, so we have "category1nr".  We
 * don't support jsr/ret, so there is no "returnAddress" type.
 */
typedef enum TypeCategory {
    kTypeCategoryUnknown = 0,
    kTypeCategory1nr,           // byte, char, int, float, boolean
    kTypeCategory2,             // long, double
    kTypeCategoryRef,           // object reference
} TypeCategory;

/*
 * See if "type" matches "cat".  All we're really looking for here is that
 * we're not mixing and matching 32-bit and 64-bit quantities, and we're
 * not mixing references with numerics.  (For example, the arguments to
 * "a < b" could be integers of different sizes, but they must both be
 * integers.  Dalvik is less specific about int vs. float, so we treat them
 * as equivalent here.)
 *
 * For category 2 values, "type" must be the "low" half of the value.
 *
 * Sets "*pOkay" to false if not.
 */
static void checkTypeCategory(RegType type, TypeCategory cat, bool* pOkay)
{
    switch (cat) {
    case kTypeCategory1nr:
        switch (type) {
        case kRegTypeFloat:
        case kRegTypeZero:
        case kRegTypeOne:
        case kRegTypeBoolean:
        case kRegTypePosByte:
        case kRegTypeByte:
        case kRegTypePosShort:
        case kRegTypeShort:
        case kRegTypeChar:
        case kRegTypeInteger:
            break;
        default:
            *pOkay = false;
            break;
        }
        break;

    case kTypeCategory2:
        switch (type) {
        case kRegTypeLongLo:
        case kRegTypeDoubleLo:
            break;
        default:
            *pOkay = false;
            break;
        }
        break;

    case kTypeCategoryRef:
        if (type != kRegTypeZero && !regTypeIsReference(type))
            *pOkay = false;
        break;

    default:
        assert(false);
        *pOkay = false;
        break;
    }
}

/*
 * For a category 2 register pair, verify that "typeh" is the appropriate
 * high part for "typel".
 *
 * Does not verify that "typel" is in fact the low part of a 64-bit
 * register pair.
 */
static void checkWidePair(RegType typel, RegType typeh, bool* pOkay)
{
    if ((typeh != typel+1))
        *pOkay = false;
}

/*
 * Implement category-1 "move" instructions.  Copy a 32-bit value from
 * "vsrc" to "vdst".
 *
 * "insnRegCount" is the number of registers available.  The "vdst" and
 * "vsrc" values are checked against this.
 */
static void copyRegister1(RegType* insnRegs, int insnRegCount, u4 vdst,
    u4 vsrc, TypeCategory cat, bool* pOkay)
{
    RegType type = getRegisterType(insnRegs, insnRegCount, vsrc, pOkay);
    if (*pOkay)
        checkTypeCategory(type, cat, pOkay);
    if (*pOkay)
        setRegisterType(insnRegs, insnRegCount, vdst, type, pOkay);

    if (!*pOkay) {
        LOG_VFY("VFY: copy1 v%u<-v%u type=%d cat=%d\n", vdst, vsrc, type, cat);
    }
}

/*
 * Implement category-2 "move" instructions.  Copy a 64-bit value from
 * "vsrc" to "vdst".  This copies both halves of the register.
 */
static void copyRegister2(RegType* insnRegs, int insnRegCount, u4 vdst,
    u4 vsrc, bool* pOkay)
{
    RegType typel = getRegisterType(insnRegs, insnRegCount, vsrc, pOkay);
    RegType typeh = getRegisterType(insnRegs, insnRegCount, vsrc+1, pOkay);
    if (*pOkay) {
        checkTypeCategory(typel, kTypeCategory2, pOkay);
        checkWidePair(typel, typeh, pOkay);
    }
    if (*pOkay)
        setRegisterType(insnRegs, insnRegCount, vdst, typel, pOkay);

    if (!*pOkay) {
        LOG_VFY("VFY: copy2 v%u<-v%u type=%d/%d\n", vdst, vsrc, typel, typeh);
    }
}

/*
 * Implement "move-result".  Copy the category-1 value from the result
 * register to another register, and reset the result register.
 *
 * We can't just call copyRegister1 with an altered insnRegCount,
 * because that would affect the test on "vdst" as well.
 */
static void copyResultRegister1(RegType* insnRegs, const int insnRegCount,
    u4 vdst, TypeCategory cat, bool* pOkay)
{
    RegType type;
    u4 vsrc;

    vsrc = RESULT_REGISTER(insnRegCount);
    type = getRegisterType(insnRegs, insnRegCount + kExtraRegs, vsrc, pOkay);
    if (*pOkay)
        checkTypeCategory(type, cat, pOkay);
    if (*pOkay) {
        setRegisterType(insnRegs, insnRegCount, vdst, type, pOkay);
        insnRegs[vsrc] = kRegTypeUnknown;
    }

    if (!*pOkay) {
        LOG_VFY("VFY: copyRes1 v%u<-v%u cat=%d type=%d\n",
            vdst, vsrc, cat, type);
    }
}

/*
 * Implement "move-result-wide".  Copy the category-2 value from the result
 * register to another register, and reset the result register.
 *
 * We can't just call copyRegister2 with an altered insnRegCount,
 * because that would affect the test on "vdst" as well.
 */
static void copyResultRegister2(RegType* insnRegs, const int insnRegCount,
    u4 vdst, bool* pOkay)
{
    RegType typel, typeh;
    u4 vsrc;

    vsrc = RESULT_REGISTER(insnRegCount);
    typel = getRegisterType(insnRegs, insnRegCount + kExtraRegs, vsrc, pOkay);
    typeh = getRegisterType(insnRegs, insnRegCount + kExtraRegs, vsrc+1, pOkay);
    if (*pOkay) {
        checkTypeCategory(typel, kTypeCategory2, pOkay);
        checkWidePair(typel, typeh, pOkay);
    }
    if (*pOkay) {
        setRegisterType(insnRegs, insnRegCount, vdst, typel, pOkay);
        insnRegs[vsrc] = kRegTypeUnknown;
        insnRegs[vsrc+1] = kRegTypeUnknown;
    }

    if (!*pOkay) {
        LOG_VFY("VFY: copyRes2 v%u<-v%u type=%d/%d\n",
            vdst, vsrc, typel, typeh);
    }
}

/*
 * Verify types for a simple two-register instruction (e.g. "neg-int").
 * "dstType" is stored into vA, and "srcType" is verified against vB.
 */
static void checkUnop(RegType* insnRegs, const int insnRegCount,
    DecodedInstruction* pDecInsn, RegType dstType, RegType srcType,
    bool* pOkay)
{
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vB, srcType, pOkay);
    setRegisterType(insnRegs, insnRegCount, pDecInsn->vA, dstType, pOkay);
}

/*
 * We're performing an operation like "and-int/2addr" that can be
 * performed on booleans as well as integers.  We get no indication of
 * boolean-ness, but we can infer it from the types of the arguments.
 *
 * Assumes we've already validated reg1/reg2.
 *
 * Returns true if both args are Boolean, Zero, or One.
 */
static bool upcastBooleanOp(RegType* insnRegs, const int insnRegCount,
    u4 reg1, u4 reg2)
{
    RegType type1, type2;

    type1 = insnRegs[reg1];
    type2 = insnRegs[reg2];

    if ((type1 == kRegTypeBoolean || type1 == kRegTypeZero ||
            type1 == kRegTypeOne) &&
        (type2 == kRegTypeBoolean || type2 == kRegTypeZero ||
            type2 == kRegTypeOne))
    {
        return true;
    }
    return false;
}

/*
 * Verify types for A two-register instruction with a literal constant
 * (e.g. "add-int/lit8").  "dstType" is stored into vA, and "srcType" is
 * verified against vB.
 *
 * If "checkBooleanOp" is set, we use the constant value in vC.
 */
static void checkLitop(RegType* insnRegs, const int insnRegCount,
    DecodedInstruction* pDecInsn, RegType dstType, RegType srcType,
    bool checkBooleanOp, bool* pOkay)
{
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vB, srcType, pOkay);
    if (*pOkay && checkBooleanOp) {
        assert(dstType == kRegTypeInteger);
        /* check vB with the call, then check the constant manually */
        if (upcastBooleanOp(insnRegs, insnRegCount, pDecInsn->vB, pDecInsn->vB)
            && (pDecInsn->vC == 0 || pDecInsn->vC == 1))
        {
            dstType = kRegTypeBoolean;
        }
    }
    setRegisterType(insnRegs, insnRegCount, pDecInsn->vA, dstType, pOkay);
}

/*
 * Verify types for a simple three-register instruction (e.g. "add-int").
 * "dstType" is stored into vA, and "srcType1"/"srcType2" are verified
 * against vB/vC.
 */
static void checkBinop(RegType* insnRegs, const int insnRegCount,
    DecodedInstruction* pDecInsn, RegType dstType, RegType srcType1,
    RegType srcType2, bool checkBooleanOp, bool* pOkay)
{
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vB, srcType1, pOkay);
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vC, srcType2, pOkay);
    if (*pOkay && checkBooleanOp) {
        assert(dstType == kRegTypeInteger);
        if (upcastBooleanOp(insnRegs, insnRegCount, pDecInsn->vB, pDecInsn->vC))
            dstType = kRegTypeBoolean;
    }
    setRegisterType(insnRegs, insnRegCount, pDecInsn->vA, dstType, pOkay);
}

/*
 * Verify types for a binary "2addr" operation.  "srcType1"/"srcType2"
 * are verified against vA/vB, then "dstType" is stored into vA.
 */
static void checkBinop2addr(RegType* insnRegs, const int insnRegCount,
    DecodedInstruction* pDecInsn, RegType dstType, RegType srcType1,
    RegType srcType2, bool checkBooleanOp, bool* pOkay)
{
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vA, srcType1, pOkay);
    verifyRegisterType(insnRegs, insnRegCount, pDecInsn->vB, srcType2, pOkay);
    if (*pOkay && checkBooleanOp) {
        assert(dstType == kRegTypeInteger);
        if (upcastBooleanOp(insnRegs, insnRegCount, pDecInsn->vA, pDecInsn->vB))
            dstType = kRegTypeBoolean;
    }
    setRegisterType(insnRegs, insnRegCount, pDecInsn->vA, dstType, pOkay);
}


/*
 * ===========================================================================
 *      Register merge
 * ===========================================================================
 */

/*
 * Compute the "class depth" of a class.  This is the distance from the
 * class to the top of the tree, chasing superclass links.  java.lang.Object
 * has a class depth of 0.
 */
static int getClassDepth(ClassObject* clazz)
{
    int depth = 0;

    while (clazz->super != NULL) {
        clazz = clazz->super;
        depth++;
    }
    return depth;
}

/*
 * Given two classes, walk up the superclass tree to find a common
 * ancestor.  (Called from findCommonSuperclass().)
 *
 * TODO: consider caching the class depth in the class object so we don't
 * have to search for it here.
 */
static ClassObject* digForSuperclass(ClassObject* c1, ClassObject* c2)
{
    int depth1, depth2;

    depth1 = getClassDepth(c1);
    depth2 = getClassDepth(c2);

    if (gDebugVerbose) {
        LOGVV("COMMON: %s(%d) + %s(%d)\n",
            c1->descriptor, depth1, c2->descriptor, depth2);
    }

    /* pull the deepest one up */
    if (depth1 > depth2) {
        while (depth1 > depth2) {
            c1 = c1->super;
            depth1--;
        }
    } else {
        while (depth2 > depth1) {
            c2 = c2->super;
            depth2--;
        }
    }

    /* walk up in lock-step */
    while (c1 != c2) {
        c1 = c1->super;
        c2 = c2->super;

        assert(c1 != NULL && c2 != NULL);
    }

    if (gDebugVerbose) {
        LOGVV("      : --> %s\n", c1->descriptor);
    }
    return c1;
}

/*
 * Merge two array classes.  We can't use the general "walk up to the
 * superclass" merge because the superclass of an array is always Object.
 * We want String[] + Integer[] = Object[].  This works for higher dimensions
 * as well, e.g. String[][] + Integer[][] = Object[][].
 *
 * If Foo1 and Foo2 are subclasses of Foo, Foo1[] + Foo2[] = Foo[].
 *
 * If Class implements Type, Class[] + Type[] = Type[].
 *
 * If the dimensions don't match, we want to convert to an array of Object
 * with the least dimension, e.g. String[][] + String[][][][] = Object[][].
 *
 * This gets a little awkward because we may have to ask the VM to create
 * a new array type with the appropriate element and dimensions.  However, we
 * shouldn't be doing this often.
 */
static ClassObject* findCommonArraySuperclass(ClassObject* c1, ClassObject* c2)
{
    ClassObject* arrayClass = NULL;
    ClassObject* commonElem;
    int i, numDims;

    assert(c1->arrayDim > 0);
    assert(c2->arrayDim > 0);

    if (c1->arrayDim == c2->arrayDim) {
        //commonElem = digForSuperclass(c1->elementClass, c2->elementClass);
        commonElem = findCommonSuperclass(c1->elementClass, c2->elementClass);
        numDims = c1->arrayDim;
    } else {
        if (c1->arrayDim < c2->arrayDim)
            numDims = c1->arrayDim;
        else
            numDims = c2->arrayDim;
        commonElem = c1->super;     // == java.lang.Object
    }

    /* walk from the element to the (multi-)dimensioned array type */
    for (i = 0; i < numDims; i++) {
        arrayClass = dvmFindArrayClassForElement(commonElem);
        commonElem = arrayClass;
    }

    LOGVV("ArrayMerge '%s' + '%s' --> '%s'\n",
        c1->descriptor, c2->descriptor, arrayClass->descriptor);
    return arrayClass;
}

/*
 * Find the first common superclass of the two classes.  We're not
 * interested in common interfaces.
 *
 * The easiest way to do this for concrete classes is to compute the "class
 * depth" of each, move up toward the root of the deepest one until they're
 * at the same depth, then walk both up to the root until they match.
 *
 * If both classes are arrays of non-primitive types, we need to merge
 * based on array depth and element type.
 *
 * If one class is an interface, we check to see if the other class/interface
 * (or one of its predecessors) implements the interface.  If so, we return
 * the interface; otherwise, we return Object.
 *
 * NOTE: we continue the tradition of "lazy interface handling".  To wit,
 * suppose we have three classes:
 *   One implements Fancy, Free
 *   Two implements Fancy, Free
 *   Three implements Free
 * where Fancy and Free are unrelated interfaces.  The code requires us
 * to merge One into Two.  Ideally we'd use a common interface, which
 * gives us a choice between Fancy and Free, and no guidance on which to
 * use.  If we use Free, we'll be okay when Three gets merged in, but if
 * we choose Fancy, we're hosed.  The "ideal" solution is to create a
 * set of common interfaces and carry that around, merging further references
 * into it.  This is a pain.  The easy solution is to simply boil them
 * down to Objects and let the runtime invokeinterface call fail, which
 * is what we do.
 */
static ClassObject* findCommonSuperclass(ClassObject* c1, ClassObject* c2)
{
    assert(!dvmIsPrimitiveClass(c1) && !dvmIsPrimitiveClass(c2));

    if (c1 == c2)
        return c1;

    if (dvmIsInterfaceClass(c1) && dvmImplements(c2, c1)) {
        if (gDebugVerbose)
            LOGVV("COMMON/I1: %s + %s --> %s\n",
                c1->descriptor, c2->descriptor, c1->descriptor);
        return c1;
    }
    if (dvmIsInterfaceClass(c2) && dvmImplements(c1, c2)) {
        if (gDebugVerbose)
            LOGVV("COMMON/I2: %s + %s --> %s\n",
                c1->descriptor, c2->descriptor, c2->descriptor);
        return c2;
    }

    if (dvmIsArrayClass(c1) && dvmIsArrayClass(c2) &&
        !dvmIsPrimitiveClass(c1->elementClass) &&
        !dvmIsPrimitiveClass(c2->elementClass))
    {
        return findCommonArraySuperclass(c1, c2);
    }

    return digForSuperclass(c1, c2);
}

/*
 * Merge two RegType values.
 *
 * Sets "*pChanged" to "true" if the result doesn't match "type1".
 */
static RegType mergeTypes(RegType type1, RegType type2, bool* pChanged)
{
    RegType result;

    /*
     * Check for trivial case so we don't have to hit memory.
     */
    if (type1 == type2)
        return type1;

    /*
     * Use the table if we can, and reject any attempts to merge something
     * from the table with a reference type.
     *
     * The uninitialized table entry at index zero *will* show up as a
     * simple kRegTypeUninit value.  Since this cannot be merged with
     * anything but itself, the rules do the right thing.
     */
    if (type1 < kRegTypeMAX) {
        if (type2 < kRegTypeMAX) {
            result = gDvmMergeTab[type1][type2];
        } else {
            /* simple + reference == conflict, usually */
            if (type1 == kRegTypeZero)
                result = type2;
            else
                result = kRegTypeConflict;
        }
    } else {
        if (type2 < kRegTypeMAX) {
            /* reference + simple == conflict, usually */
            if (type2 == kRegTypeZero)
                result = type1;
            else
                result = kRegTypeConflict;
        } else {
            /* merging two references */
            if (regTypeIsUninitReference(type1) ||
                regTypeIsUninitReference(type2))
            {
                /* can't merge uninit with anything but self */
                result = kRegTypeConflict;
            } else {
                ClassObject* clazz1 = regTypeInitializedReferenceToClass(type1);
                ClassObject* clazz2 = regTypeInitializedReferenceToClass(type2);
                ClassObject* mergedClass;

                mergedClass = findCommonSuperclass(clazz1, clazz2);
                assert(mergedClass != NULL);
                result = regTypeFromClass(mergedClass);
            }
        }
    }

    if (result != type1)
        *pChanged = true;
    return result;
}

/*
 * Control can transfer to "nextInsn".
 *
 * Merge the registers from "workRegs" into "regTypes" at "nextInsn", and
 * set the "changed" flag on the target address if the registers have changed.
 */
static void updateRegisters(const Method* meth, InsnFlags* insnFlags,
    RegisterTable* regTable, int nextInsn, const RegType* workRegs)
{
    RegType* targetRegs = getRegisterLine(regTable, nextInsn);
    const int insnRegCount = meth->registersSize;

#if 0
    if (!dvmInsnIsBranchTarget(insnFlags, nextInsn)) {
        LOGE("insnFlags[0x%x]=0x%08x\n", nextInsn, insnFlags[nextInsn]);
        LOGE(" In %s.%s %s\n",
            meth->clazz->descriptor, meth->name, meth->descriptor);
        assert(false);
    }
#endif

    if (!dvmInsnIsVisitedOrChanged(insnFlags, nextInsn)) {
        /*
         * We haven't processed this instruction before, and we haven't
         * touched the registers here, so there's nothing to "merge".  Copy
         * the registers over and mark it as changed.  (This is the only
         * way a register can transition out of "unknown", so this is not
         * just an optimization.)
         */
        LOGVV("COPY into 0x%04x\n", nextInsn);
        copyRegisters(targetRegs, workRegs, insnRegCount + kExtraRegs);
        dvmInsnSetChanged(insnFlags, nextInsn, true);
    } else {
        if (gDebugVerbose) {
            LOGVV("MERGE into 0x%04x\n", nextInsn);
            //dumpRegTypes(meth, insnFlags, targetRegs, 0, "targ", NULL, 0);
            //dumpRegTypes(meth, insnFlags, workRegs, 0, "work", NULL, 0);
        }
        /* merge registers, set Changed only if different */
        bool changed = false;
        int i;

        for (i = 0; i < insnRegCount + kExtraRegs; i++) {
            targetRegs[i] = mergeTypes(targetRegs[i], workRegs[i], &changed);
        }

        if (gDebugVerbose) {
            //LOGI(" RESULT (changed=%d)\n", changed);
            //dumpRegTypes(meth, insnFlags, targetRegs, 0, "rslt", NULL, 0);
        }

        if (changed)
            dvmInsnSetChanged(insnFlags, nextInsn, true);
    }
}


/*
 * ===========================================================================
 *      Utility functions
 * ===========================================================================
 */

/*
 * Look up an instance field, specified by "fieldIdx", that is going to be
 * accessed in object "objType".  This resolves the field and then verifies
 * that the class containing the field is an instance of the reference in
 * "objType".
 *
 * It is possible for "objType" to be kRegTypeZero, meaning that we might
 * have a null reference.  This is a runtime problem, so we allow it,
 * skipping some of the type checks.
 *
 * In general, "objType" must be an initialized reference.  However, we
 * allow it to be uninitialized if this is an "<init>" method and the field
 * is declared within the "objType" class.
 *
 * Returns an InstField on success, returns NULL and sets "*pOkay" to false
 * on failure.
 */
static InstField* getInstField(const Method* meth,
    const UninitInstanceMap* uninitMap, RegType objType, int fieldIdx,
    bool* pOkay)
{
    InstField* instField = NULL;
    ClassObject* objClass;
    bool mustBeLocal = false;

    if (!regTypeIsReference(objType)) {
        LOG_VFY("VFY: attempt to access field of non-reference type %d\n",
            objType);
        *pOkay = false;
        goto bail;
    }

    instField = dvmOptResolveInstField(meth->clazz, fieldIdx);
    if (instField == NULL) {
        LOG_VFY("VFY: unable to resolve instance field %u\n", fieldIdx);
        *pOkay = false;
        goto bail;
    }

    if (objType == kRegTypeZero)
        goto bail;

    /*
     * Access to fields in uninitialized objects is allowed if this is
     * the <init> method for the object and the field in question is
     * declared by this class.
     */
    objClass = regTypeReferenceToClass(objType, uninitMap);
    assert(objClass != NULL);
    if (regTypeIsUninitReference(objType)) {
        if (!isInitMethod(meth) || meth->clazz != objClass) {
            LOG_VFY("VFY: attempt to access field via uninitialized ref\n");
            *pOkay = false;
            goto bail;
        }
        mustBeLocal = true;
    }

    if (!dvmInstanceof(objClass, instField->field.clazz)) {
        LOG_VFY("VFY: invalid field access (field %s.%s, through %s ref)\n",
                instField->field.clazz->descriptor, instField->field.name,
                objClass->descriptor);
        *pOkay = false;
        goto bail;
    }

    if (mustBeLocal) {
        /* for uninit ref, make sure it's defined by this class, not super */
        if (instField < objClass->ifields ||
            instField >= objClass->ifields + objClass->ifieldCount)
        {
            LOG_VFY("VFY: invalid constructor field access (field %s in %s)\n",
                    instField->field.name, objClass->descriptor);
            *pOkay = false;
            goto bail;
        }
    }

bail:
    return instField;
}

/*
 * Look up a static field.
 *
 * Returns a StaticField on success, returns NULL and sets "*pOkay" to false
 * on failure.
 */
static StaticField* getStaticField(const Method* meth, int fieldIdx,
    bool* pOkay)
{
    StaticField* staticField;

    staticField = dvmOptResolveStaticField(meth->clazz, fieldIdx);
    if (staticField == NULL) {
        DexFile* pDexFile = meth->clazz->pDvmDex->pDexFile;
        const DexFieldId* pFieldId;

        pFieldId = dexGetFieldId(pDexFile, fieldIdx);

        LOG_VFY("VFY: unable to resolve static field %u (%s) in %s\n", fieldIdx,
            dexStringById(pDexFile, pFieldId->nameIdx),
            dexStringByTypeIdx(pDexFile, pFieldId->classIdx));

        *pOkay = false;
        goto bail;
    }

bail:
    return staticField;
}

/*
 * If "field" is marked "final", make sure this is the either <clinit>
 * or <init> as appropriate.
 *
 * Sets "*pOkay" to false on failure.
 */
static void checkFinalFieldAccess(const Method* meth, const Field* field,
    bool* pOkay)
{
    if (!dvmIsFinalField(field))
        return;

    /* make sure we're in the same class */
    if (meth->clazz != field->clazz) {
        LOG_VFY_METH(meth, "VFY: can't modify final field %s.%s\n",
            field->clazz->descriptor, field->name);
        *pOkay = false;
        return;
    }

    /*
     * The EMMA code coverage tool generates a static method that
     * modifies a private static final field.  The method is only
     * called by <clinit>, so the code is reasonable if not quite
     * kosher.  (Attempting to *compile* code that does something
     * like that will earn you a quick thumbs-down from javac.)
     *
     * The verifier in another popular VM doesn't complain about this,
     * so we're going to allow classes to modify their own static
     * final fields outside of class initializers.  Further testing
     * showed that modifications to instance fields are also allowed.
     */
#if 0
    /* make sure we're in the right kind of constructor */
    if (dvmIsStaticField(field)) {
        if (!isClassInitMethod(meth)) {
            LOG_VFY_METH(meth,
                "VFY: can't modify final static field outside <clinit>\n");
            *pOkay = false;
        }
    } else {
        if (!isInitMethod(meth)) {
            LOG_VFY_METH(meth,
                "VFY: can't modify final field outside <init>\n");
            *pOkay = false;
        }
    }
#endif
}

/*
 * Make sure that the register type is suitable for use as an array index.
 *
 * Sets "*pOkay" to false if not.
 */
static void checkArrayIndexType(const Method* meth, RegType regType,
    bool* pOkay)
{
    if (*pOkay) {
        /*
         * The 1nr types are interchangeable at this level.  We could
         * do something special if we can definitively identify it as a
         * float, but there's no real value in doing so.
         */
        checkTypeCategory(regType, kTypeCategory1nr, pOkay);
        if (!*pOkay) {
            LOG_VFY_METH(meth, "Invalid reg type for array index (%d)\n",
                regType);
        }
    }
}

/*
 * Check constraints on constructor return.  Specifically, make sure that
 * the "this" argument got initialized.
 *
 * The "this" argument to <init> uses code offset kUninitThisArgAddr, which
 * puts it at the start of the list in slot 0.  If we see a register with
 * an uninitialized slot 0 reference, we know it somehow didn't get
 * initialized.
 *
 * Returns "true" if all is well.
 */
static bool checkConstructorReturn(const Method* meth, const RegType* insnRegs,
    const int insnRegCount)
{
    int i;

    if (!isInitMethod(meth))
        return true;

    RegType uninitThis = regTypeFromUninitIndex(kUninitThisArgSlot);

    for (i = 0; i < insnRegCount; i++) {
        if (insnRegs[i] == uninitThis) {
            LOG_VFY("VFY: <init> returning without calling superclass init\n");
            return false;
        }
    }
    return true;
}

/*
 * Verify that the target instruction is not "move-exception".  It's important
 * that the only way to execute a move-exception is as the first instruction
 * of an exception handler.
 *
 * Returns "true" if all is well, "false" if the target instruction is
 * move-exception.
 */
static bool checkMoveException(const Method* meth, int insnIdx,
    const char* logNote)
{
    assert(insnIdx >= 0 && insnIdx < (int)dvmGetMethodInsnsSize(meth));

    if ((meth->insns[insnIdx] & 0xff) == OP_MOVE_EXCEPTION) {
        LOG_VFY("VFY: invalid use of move-exception\n");
        return false;
    }
    return true;
}

/*
 * For the "move-exception" instruction at "insnIdx", which must be at an
 * exception handler address, determine the first common superclass of
 * all exceptions that can land here.  (For javac output, we're probably
 * looking at multiple spans of bytecode covered by one "try" that lands
 * at an exception-specific "catch", but in general the handler could be
 * shared for multiple exceptions.)
 *
 * Returns NULL if no matching exception handler can be found, or if the
 * exception is not a subclass of Throwable.
 */
static ClassObject* getCaughtExceptionType(const Method* meth, int insnIdx)
{
    const DexCode* pCode;
    DexFile* pDexFile;
    ClassObject* commonSuper = NULL;
    u4 handlersSize;
    u4 offset;
    u4 i;

    pDexFile = meth->clazz->pDvmDex->pDexFile;
    pCode = dvmGetMethodCode(meth);

    if (pCode->triesSize != 0) {
        handlersSize = dexGetHandlersSize(pCode);
        offset = dexGetFirstHandlerOffset(pCode);
    } else {
        handlersSize = 0;
        offset = 0;
    }

    for (i = 0; i < handlersSize; i++) {
        DexCatchIterator iterator;
        dexCatchIteratorInit(&iterator, pCode, offset);

        for (;;) {
            const DexCatchHandler* handler = dexCatchIteratorNext(&iterator);

            if (handler == NULL) {
                break;
            }

            if (handler->address == (u4) insnIdx) {
                ClassObject* clazz;

                if (handler->typeIdx == kDexNoIndex)
                    clazz = gDvm.classJavaLangThrowable;
                else
                    clazz = dvmOptResolveClass(meth->clazz, handler->typeIdx);

                if (clazz == NULL) {
                    LOG_VFY("VFY: unable to resolve exception class %u (%s)\n",
                        handler->typeIdx,
                        dexStringByTypeIdx(pDexFile, handler->typeIdx));
                } else {
                    if (commonSuper == NULL)
                        commonSuper = clazz;
                    else
                        commonSuper = findCommonSuperclass(clazz, commonSuper);
                }
            }
        }

        offset = dexCatchIteratorGetEndOffset(&iterator, pCode);
    }

    if (commonSuper == NULL) {
        LOG_VFY_METH(meth,
            "VFY: unable to find exception handler at addr 0x%x\n", insnIdx);
    }

    return commonSuper;
}

/*
 * Initialize the RegisterTable.
 *
 * Every instruction address can have a different set of information about
 * what's in which register, but for verification purposes we only need to
 * store it at branch target addresses (because we merge into that).
 *
 * By zeroing out the storage we are effectively initializing the register
 * information to kRegTypeUnknown.
 */
static bool initRegisterTable(const Method* meth, const InsnFlags* insnFlags,
    RegisterTable* regTable, RegisterTrackingMode trackRegsFor)
{
    const int insnsSize = dvmGetMethodInsnsSize(meth);
    int i;

    regTable->insnRegCountPlus = meth->registersSize + kExtraRegs;
    regTable->addrRegs = (RegType**) calloc(insnsSize, sizeof(RegType*));
    if (regTable->addrRegs == NULL)
        return false;

    assert(insnsSize > 0);

    /*
     * "All" means "every address that holds the start of an instruction".
     * "Branches" and "GcPoints" mean just those addresses.
     *
     * "GcPoints" fills about half the addresses, "Branches" about 15%.
     */
    int interestingCount = 0;
    //int insnCount = 0;

    for (i = 0; i < insnsSize; i++) {
        bool interesting;

        switch (trackRegsFor) {
        case kTrackRegsAll:
            interesting = dvmInsnIsOpcode(insnFlags, i);
            break;
        case kTrackRegsGcPoints:
            interesting = dvmInsnIsGcPoint(insnFlags, i) ||
                          dvmInsnIsBranchTarget(insnFlags, i);
            break;
        case kTrackRegsBranches:
            interesting = dvmInsnIsBranchTarget(insnFlags, i);
            break;
        default:
            dvmAbort();
            return false;
        }

        if (interesting)
            interestingCount++;

        /* count instructions, for display only */
        //if (dvmInsnIsOpcode(insnFlags, i))
        //    insnCount++;
    }

    regTable->regAlloc = (RegType*)
        calloc(regTable->insnRegCountPlus * interestingCount, sizeof(RegType));
    if (regTable->regAlloc == NULL)
        return false;

    RegType* regPtr = regTable->regAlloc;
    for (i = 0; i < insnsSize; i++) {
        bool interesting;

        switch (trackRegsFor) {
        case kTrackRegsAll:
            interesting = dvmInsnIsOpcode(insnFlags, i);
            break;
        case kTrackRegsGcPoints:
            interesting = dvmInsnIsGcPoint(insnFlags, i) ||
                          dvmInsnIsBranchTarget(insnFlags, i);
            break;
        case kTrackRegsBranches:
            interesting = dvmInsnIsBranchTarget(insnFlags, i);
            break;
        default:
            dvmAbort();
            return false;
        }

        if (interesting) {
            regTable->addrRegs[i] = regPtr;
            regPtr += regTable->insnRegCountPlus;
        }
    }

    //LOGD("Tracking registers for %d, total %d of %d(%d) (%d%%)\n",
    //    TRACK_REGS_FOR, interestingCount, insnCount, insnsSize,
    //    (interestingCount*100) / insnCount);

    assert(regPtr - regTable->regAlloc ==
        regTable->insnRegCountPlus * interestingCount);
    assert(regTable->addrRegs[0] != NULL);
    return true;
}


/*
 * Verify that the arguments in a filled-new-array instruction are valid.
 *
 * "resClass" is the class refered to by pDecInsn->vB.
 */
static void verifyFilledNewArrayRegs(const Method* meth,
    const RegType* insnRegs, const int insnRegCount,
    const DecodedInstruction* pDecInsn, ClassObject* resClass, bool isRange,
    bool* pOkay)
{
    u4 argCount = pDecInsn->vA;
    RegType expectedType;
    PrimitiveType elemType;
    unsigned int ui;

    assert(dvmIsArrayClass(resClass));
    elemType = resClass->elementClass->primitiveType;
    if (elemType == PRIM_NOT) {
        expectedType = regTypeFromClass(resClass->elementClass);
    } else {
        expectedType = primitiveTypeToRegType(elemType);
    }
    //LOGI("filled-new-array: %s -> %d\n", resClass->descriptor, expectedType);

    /*
     * Verify each register.  If "argCount" is bad, verifyRegisterType()
     * will run off the end of the list and fail.  It's legal, if silly,
     * for argCount to be zero.
     */
    for (ui = 0; ui < argCount; ui++) {
        u4 getReg;

        if (isRange)
            getReg = pDecInsn->vC + ui;
        else
            getReg = pDecInsn->arg[ui];

        verifyRegisterType(insnRegs, insnRegCount, getReg, expectedType, pOkay);
        if (!*pOkay) {
            LOG_VFY("VFY: filled-new-array arg %u(%u) not valid\n", ui, getReg);
            return;
        }
    }
}


/*
 * ===========================================================================
 *      Entry point and driver loop
 * ===========================================================================
 */

/*
 * Entry point for the detailed code-flow analysis.
 */
bool dvmVerifyCodeFlow(const Method* meth, InsnFlags* insnFlags,
    UninitInstanceMap* uninitMap)
{
    bool result = false;
    const int insnsSize = dvmGetMethodInsnsSize(meth);
    const u2* insns = meth->insns;
    const bool generateRegisterMap = gDvm.generateRegisterMaps;
    int i, offset;
    bool isConditional;
    RegisterTable regTable;

    memset(&regTable, 0, sizeof(regTable));

#ifndef NDEBUG
    checkMergeTab();     // only need to do this if table gets updated
#endif

    /*
     * We rely on these for verification of const-class, const-string,
     * and throw instructions.  Make sure we have them.
     */
    if (gDvm.classJavaLangClass == NULL)
        gDvm.classJavaLangClass =
            dvmFindSystemClassNoInit("Ljava/lang/Class;");
    if (gDvm.classJavaLangString == NULL)
        gDvm.classJavaLangString =
            dvmFindSystemClassNoInit("Ljava/lang/String;");
    if (gDvm.classJavaLangThrowable == NULL)
        gDvm.classJavaLangThrowable =
            dvmFindSystemClassNoInit("Ljava/lang/Throwable;");
    if (gDvm.classJavaLangObject == NULL)
        gDvm.classJavaLangObject =
            dvmFindSystemClassNoInit("Ljava/lang/Object;");

    if (meth->registersSize * insnsSize > 2*1024*1024) {
        /* should probably base this on actual memory requirements */
        LOG_VFY_METH(meth,
            "VFY: arbitrarily rejecting large method (regs=%d count=%d)\n",
            meth->registersSize, insnsSize);
        goto bail;
    }

    /*
     * Create register lists, and initialize them to "Unknown".  If we're
     * also going to create the register map, we need to retain the
     * register lists for a larger set of addresses.
     */
    if (!initRegisterTable(meth, insnFlags, &regTable,
            generateRegisterMap ? kTrackRegsGcPoints : kTrackRegsBranches))
        goto bail;

    /*
     * Initialize the types of the registers that correspond to the
     * method arguments.  We can determine this from the method signature.
     */
    if (!setTypesFromSignature(meth, regTable.addrRegs[0], uninitMap))
        goto bail;

    /*
     * Run the verifier.
     */
    if (!doCodeVerification(meth, insnFlags, &regTable, uninitMap))
        goto bail;

    /*
     * Generate a register map.
     */
    if (generateRegisterMap) {
        RegisterMap* pMap;
        VerifierData vd;

        vd.method = meth;
        vd.insnsSize = insnsSize;
        vd.insnRegCount = meth->registersSize;
        vd.insnFlags = insnFlags;
        vd.addrRegs = regTable.addrRegs;
        
        pMap = dvmGenerateRegisterMapV(&vd);
        if (pMap != NULL) {
            /*
             * Tuck it into the Method struct.  It will either get used
             * directly or, if we're in dexopt, will be packed up and
             * appended to the DEX file.
             */
            dvmSetRegisterMap((Method*)meth, pMap);
        }
    }

    /*
     * Success.
     */
    result = true;

bail:
    free(regTable.addrRegs);
    free(regTable.regAlloc);
    return result;
}

/*
 * Grind through the instructions.
 *
 * The basic strategy is as outlined in v3 4.11.1.2: set the "changed" bit
 * on the first instruction, process it (setting additional "changed" bits),
 * and repeat until there are no more.
 *
 * v3 4.11.1.1
 * - (N/A) operand stack is always the same size
 * - operand stack [registers] contain the correct types of values
 * - local variables [registers] contain the correct types of values
 * - methods are invoked with the appropriate arguments
 * - fields are assigned using values of appropriate types
 * - opcodes have the correct type values in operand registers
 * - there is never an uninitialized class instance in a local variable in
 *   code protected by an exception handler (operand stack is okay, because
 *   the operand stack is discarded when an exception is thrown) [can't
 *   know what's a local var w/o the debug info -- should fall out of
 *   register typing]
 *
 * v3 4.11.1.2
 * - execution cannot fall off the end of the code
 *
 * (We also do many of the items described in the "static checks" sections,
 * because it's easier to do them here.)
 *
 * We need an array of RegType values, one per register, for every
 * instruction.  In theory this could become quite large -- up to several
 * megabytes for a monster function.  For self-preservation we reject
 * anything that requires more than a certain amount of memory.  (Typical
 * "large" should be on the order of 4K code units * 8 registers.)  This
 * will likely have to be adjusted.
 *
 *
 * The spec forbids backward branches when there's an uninitialized reference
 * in a register.  The idea is to prevent something like this:
 *   loop:
 *     move r1, r0
 *     new-instance r0, MyClass
 *     ...
 *     if-eq rN, loop  // once
 *   initialize r0
 *
 * This leaves us with two different instances, both allocated by the
 * same instruction, but only one is initialized.  The scheme outlined in
 * v3 4.11.1.4 wouldn't catch this, so they work around it by preventing
 * backward branches.  We achieve identical results without restricting
 * code reordering by specifying that you can't execute the new-instance
 * instruction if a register contains an uninitialized instance created
 * by that same instrutcion.
 */
static bool doCodeVerification(const Method* meth, InsnFlags* insnFlags,
    RegisterTable* regTable, UninitInstanceMap* uninitMap)
{
    const int insnsSize = dvmGetMethodInsnsSize(meth);
    const u2* insns = meth->insns;
    RegType workRegs[meth->registersSize + kExtraRegs];
    bool result = false;
    bool debugVerbose = false;
    int insnIdx, startGuess, prevAddr;

    /*
     * Begin by marking the first instruction as "changed".
     */
    dvmInsnSetChanged(insnFlags, 0, true);

    if (doVerboseLogging(meth)) {
        IF_LOGI() {
            char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
            LOGI("Now verifying: %s.%s %s (ins=%d regs=%d)\n",
                meth->clazz->descriptor, meth->name, desc,
                meth->insSize, meth->registersSize);
            LOGI(" ------ [0    4    8    12   16   20   24   28   32   36\n");
            free(desc);
        }
        debugVerbose = true;
        gDebugVerbose = true;
    } else {
        gDebugVerbose = false;
    }

    startGuess = 0;

    /*
     * Continue until no instructions are marked "changed".
     */
    while (true) {
        /*
         * Find the first marked one.  Use "startGuess" as a way to find
         * one quickly.
         */
        for (insnIdx = startGuess; insnIdx < insnsSize; insnIdx++) {
            if (dvmInsnIsChanged(insnFlags, insnIdx))
                break;
        }

        if (insnIdx == insnsSize) {
            if (startGuess != 0) {
                /* try again, starting from the top */
                startGuess = 0;
                continue;
            } else {
                /* all flags are clear */
                break;
            }
        }

        /*
         * We carry the working set of registers from instruction to
         * instruction.  If this address can be the target of a branch
         * (or throw) instruction, or if we're skipping around chasing
         * "changed" flags, we need to load the set of registers from
         * the table.
         *
         * Because we always prefer to continue on to the next instruction,
         * we should never have a situation where we have a stray
         * "changed" flag set on an instruction that isn't a branch target.
         */
        if (dvmInsnIsBranchTarget(insnFlags, insnIdx)) {
            RegType* insnRegs = getRegisterLine(regTable, insnIdx);
            assert(insnRegs != NULL);
            copyRegisters(workRegs, insnRegs, meth->registersSize + kExtraRegs);

            if (debugVerbose) {
                dumpRegTypes(meth, insnFlags, workRegs, insnIdx, NULL,uninitMap,
                    SHOW_REG_DETAILS);
            }

        } else {
            if (debugVerbose) {
                dumpRegTypes(meth, insnFlags, workRegs, insnIdx, NULL,uninitMap,
                    SHOW_REG_DETAILS);
            }

#ifndef NDEBUG
            /*
             * Sanity check: retrieve the stored register line (assuming
             * a full table) and make sure it actually matches.
             */
            RegType* insnRegs = getRegisterLine(regTable, insnIdx);
            if (insnRegs != NULL &&
                compareRegisters(workRegs, insnRegs,
                    meth->registersSize + kExtraRegs) != 0)
            {
                char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
                LOG_VFY("HUH? workRegs diverged in %s.%s %s\n",
                        meth->clazz->descriptor, meth->name, desc);
                free(desc);
                dumpRegTypes(meth, insnFlags, workRegs, 0, "work",
                    uninitMap, DRT_SHOW_REF_TYPES | DRT_SHOW_LOCALS);
                dumpRegTypes(meth, insnFlags, insnRegs, 0, "insn",
                    uninitMap, DRT_SHOW_REF_TYPES | DRT_SHOW_LOCALS);
            }
#endif
        }

        //LOGI("process %s.%s %s %d\n",
        //    meth->clazz->descriptor, meth->name, meth->descriptor, insnIdx);
        if (!verifyInstruction(meth, insnFlags, regTable, workRegs, insnIdx,
                uninitMap, &startGuess))
        {
            //LOGD("+++ %s bailing at %d\n", meth->name, insnIdx);
            goto bail;
        }

#if 0
        {
            static const int gcMask = kInstrCanBranch | kInstrCanSwitch |
                                      kInstrCanThrow | kInstrCanReturn;
            OpCode opCode = *(meth->insns + insnIdx) & 0xff;
            int flags = dexGetInstrFlags(gDvm.instrFlags, opCode);

            /* 8, 16, 32, or 32*n -bit regs */
            int regWidth = (meth->registersSize + 7) / 8;
            if (regWidth == 3)
                regWidth = 4;
            if (regWidth > 4) {
                regWidth = ((regWidth + 3) / 4) * 4;
                if (false) {
                    LOGW("WOW: %d regs -> %d  %s.%s\n",
                        meth->registersSize, regWidth,
                        meth->clazz->descriptor, meth->name);
                    //x = true;
                }
            }

            if ((flags & gcMask) != 0) {
                /* this is a potential GC point */
                gDvm__gcInstr++;

                if (insnsSize < 256)
                    gDvm__gcData += 1;
                else
                    gDvm__gcData += 2;
                gDvm__gcData += regWidth;
            }
            gDvm__gcSimpleData += regWidth;

            gDvm__totalInstr++;
        }
#endif

        /*
         * Clear "changed" and mark as visited.
         */
        dvmInsnSetVisited(insnFlags, insnIdx, true);
        dvmInsnSetChanged(insnFlags, insnIdx, false);
    }

    if (DEAD_CODE_SCAN) {
        /*
         * Scan for dead code.  There's nothing "evil" about dead code, but it
         * indicates a flaw somewhere down the line, possibly in the verifier.
         */
        int deadStart = -1;
        for (insnIdx = 0; insnIdx < insnsSize;
            insnIdx += dvmInsnGetWidth(insnFlags, insnIdx))
        {
            /*
             * Switch-statement data doesn't get "visited" by scanner.  It
             * may or may not be preceded by a padding NOP.
             */
            int instr = meth->insns[insnIdx];
            if (instr == kPackedSwitchSignature ||
                instr == kSparseSwitchSignature ||
                instr == kArrayDataSignature ||
                (instr == OP_NOP &&
                 (meth->insns[insnIdx+1] == kPackedSwitchSignature ||
                  meth->insns[insnIdx+1] == kSparseSwitchSignature ||
                  meth->insns[insnIdx+1] == kArrayDataSignature)))
            {
                dvmInsnSetVisited(insnFlags, insnIdx, true);
            }

            if (!dvmInsnIsVisited(insnFlags, insnIdx)) {
                if (deadStart < 0)
                    deadStart = insnIdx;
            } else if (deadStart >= 0) {
                IF_LOGD() {
                    char* desc =
                        dexProtoCopyMethodDescriptor(&meth->prototype);
                    LOGD("VFY: dead code 0x%04x-%04x in %s.%s %s\n",
                        deadStart, insnIdx-1,
                        meth->clazz->descriptor, meth->name, desc);
                    free(desc);
                }

                deadStart = -1;
            }
        }
        if (deadStart >= 0) {
            IF_LOGD() {
                char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
                LOGD("VFY: dead code 0x%04x-%04x in %s.%s %s\n",
                    deadStart, insnIdx-1,
                    meth->clazz->descriptor, meth->name, desc);
                free(desc);
            }
        }
    }

    result = true;

bail:
    return result;
}


/*
 * Perform verification for a single instruction.
 *
 * This requires fully decoding the instruction to determine the effect
 * it has on registers.
 *
 * Finds zero or more following instructions and sets the "changed" flag
 * if execution at that point needs to be (re-)evaluated.  Register changes
 * are merged into "regTypes" at the target addresses.  Does not set or
 * clear any other flags in "insnFlags".
 */
static bool verifyInstruction(const Method* meth, InsnFlags* insnFlags,
    RegisterTable* regTable, RegType* workRegs, int insnIdx,
    UninitInstanceMap* uninitMap, int* pStartGuess)
{
    const int insnsSize = dvmGetMethodInsnsSize(meth);
    const u2* insns = meth->insns + insnIdx;
    bool result = false;

    /*
     * Once we finish decoding the instruction, we need to figure out where
     * we can go from here.  There are three possible ways to transfer
     * control to another statement:
     *
     * (1) Continue to the next instruction.  Applies to all but
     *     unconditional branches, method returns, and exception throws.
     * (2) Branch to one or more possible locations.  Applies to branches
     *     and switch statements.
     * (3) Exception handlers.  Applies to any instruction that can
     *     throw an exception that is handled by an encompassing "try"
     *     block.  (We simplify this to be any instruction that can
     *     throw any exception.)
     *
     * We can also return, in which case there is no successor instruction
     * from this point.
     *
     * The behavior can be determined from the InstrFlags.
     */

    const DexFile* pDexFile = meth->clazz->pDvmDex->pDexFile;
    RegType entryRegs[meth->registersSize + kExtraRegs];
    ClassObject* resClass;
    const char* className;
    int branchTarget = 0;
    const int insnRegCount = meth->registersSize;
    RegType tmpType;
    DecodedInstruction decInsn;
    bool justSetResult = false;
    bool okay = true;

#ifndef NDEBUG
    memset(&decInsn, 0x81, sizeof(decInsn));
#endif
    dexDecodeInstruction(gDvm.instrFormat, insns, &decInsn);

    const int nextFlags = dexGetInstrFlags(gDvm.instrFlags, decInsn.opCode);

    /*
     * Make a copy of the previous register state.  If the instruction
     * throws an exception, we merge *this* into the destination rather
     * than workRegs, because we don't want the result from the "successful"
     * code path (e.g. a check-cast that "improves" a type) to be visible
     * to the exception handler.
     */
    if ((nextFlags & kInstrCanThrow) != 0 && dvmInsnIsInTry(insnFlags, insnIdx))
    {
        copyRegisters(entryRegs, workRegs, meth->registersSize + kExtraRegs);
    } else {
#ifndef NDEBUG
        memset(entryRegs, 0xdd,
            (meth->registersSize + kExtraRegs) * sizeof(RegType));
#endif
    }

    switch (decInsn.opCode) {
    case OP_NOP:
        /*
         * A "pure" NOP has no effect on anything.  Data tables start with
         * a signature that looks like a NOP; if we see one of these in
         * the course of executing code then we have a problem.
         */
        if (decInsn.vA != 0) {
            LOG_VFY("VFY: encountered data table in instruction stream\n");
            okay = false;
        }
        break;

    case OP_MOVE:
    case OP_MOVE_FROM16:
    case OP_MOVE_16:
        copyRegister1(workRegs, insnRegCount, decInsn.vA, decInsn.vB,
            kTypeCategory1nr, &okay);
        break;
    case OP_MOVE_WIDE:
    case OP_MOVE_WIDE_FROM16:
    case OP_MOVE_WIDE_16:
        copyRegister2(workRegs, insnRegCount, decInsn.vA, decInsn.vB, &okay);
        break;
    case OP_MOVE_OBJECT:
    case OP_MOVE_OBJECT_FROM16:
    case OP_MOVE_OBJECT_16:
        copyRegister1(workRegs, insnRegCount, decInsn.vA, decInsn.vB,
            kTypeCategoryRef, &okay);
        break;

    /*
     * The move-result instructions copy data out of a "pseudo-register"
     * with the results from the last method invocation.  In practice we
     * might want to hold the result in an actual CPU register, so the
     * Dalvik spec requires that these only appear immediately after an
     * invoke or filled-new-array.
     *
     * These calls invalidate the "result" register.  (This is now
     * redundant with the reset done below, but it can make the debug info
     * easier to read in some cases.)
     */
    case OP_MOVE_RESULT:
        copyResultRegister1(workRegs, insnRegCount, decInsn.vA,
            kTypeCategory1nr, &okay);
        break;
    case OP_MOVE_RESULT_WIDE:
        copyResultRegister2(workRegs, insnRegCount, decInsn.vA, &okay);
        break;
    case OP_MOVE_RESULT_OBJECT:
        copyResultRegister1(workRegs, insnRegCount, decInsn.vA,
            kTypeCategoryRef, &okay);
        break;

    case OP_MOVE_EXCEPTION:
        /*
         * This statement can only appear as the first instruction in an
         * exception handler (though not all exception handlers need to
         * have one of these).  We verify that as part of extracting the
         * exception type from the catch block list.
         *
         * "resClass" will hold the closest common superclass of all
         * exceptions that can be handled here.
         */
        resClass = getCaughtExceptionType(meth, insnIdx);
        if (resClass == NULL) {
            okay = false;
        } else {
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                regTypeFromClass(resClass), &okay);
        }
        break;

    case OP_RETURN_VOID:
        okay = checkConstructorReturn(meth, workRegs, insnRegCount);
        if (okay && getMethodReturnType(meth) != kRegTypeUnknown) {
            LOG_VFY("VFY: return-void not expected\n");
            okay = false;
        }
        break;
    case OP_RETURN:
        okay = checkConstructorReturn(meth, workRegs, insnRegCount);
        if (okay) {
            /* check the method signature */
            RegType returnType = getMethodReturnType(meth);
            checkTypeCategory(returnType, kTypeCategory1nr, &okay);
            if (!okay)
                LOG_VFY("VFY: return-32 not expected\n");

            /* check the register contents */
            returnType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                &okay);
            checkTypeCategory(returnType, kTypeCategory1nr, &okay);
            if (!okay)
                LOG_VFY("VFY: return-32 on invalid register v%d\n", decInsn.vA);
        }
        break;
    case OP_RETURN_WIDE:
        okay = checkConstructorReturn(meth, workRegs, insnRegCount);
        if (okay) {
            RegType returnType, returnTypeHi;

            /* check the method signature */
            returnType = getMethodReturnType(meth);
            checkTypeCategory(returnType, kTypeCategory2, &okay);
            if (!okay)
                LOG_VFY("VFY: return-wide not expected\n");

            /* check the register contents */
            returnType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                &okay);
            returnTypeHi = getRegisterType(workRegs, insnRegCount,
                decInsn.vA +1, &okay);
            if (okay) {
                checkTypeCategory(returnType, kTypeCategory2, &okay);
                checkWidePair(returnType, returnTypeHi, &okay);
            }
            if (!okay) {
                LOG_VFY("VFY: return-wide on invalid register pair v%d\n",
                    decInsn.vA);
            }
        }
        break;
    case OP_RETURN_OBJECT:
        okay = checkConstructorReturn(meth, workRegs, insnRegCount);
        if (okay) {
            RegType returnType = getMethodReturnType(meth);
            checkTypeCategory(returnType, kTypeCategoryRef, &okay);
            if (!okay) {
                LOG_VFY("VFY: return-object not expected\n");
                break;
            }

            /* returnType is the *expected* return type, not register value */
            assert(returnType != kRegTypeZero);
            assert(!regTypeIsUninitReference(returnType));

            /*
             * Verify that the reference in vAA is an instance of the type
             * in "returnType".  The Zero type is allowed here.  If the
             * method is declared to return an interface, then any
             * initialized reference is acceptable.
             *
             * Note getClassFromRegister fails if the register holds an
             * uninitialized reference, so we do not allow them to be
             * returned.
             */
            ClassObject* declClass;

            declClass = regTypeInitializedReferenceToClass(returnType);
            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vA, &okay);
            if (!okay)
                break;
            if (resClass != NULL) {
                if (!dvmIsInterfaceClass(declClass) &&
                    !dvmInstanceof(resClass, declClass))
                {
                    LOG_VFY("VFY: returning %s, declared %s\n",
                            resClass->descriptor, declClass->descriptor);
                    okay = false;
                    break;
                }
            }
        }
        break;

    case OP_CONST_4:
    case OP_CONST_16:
    case OP_CONST:
        /* could be boolean, int, float, or a null reference */
        setRegisterType(workRegs, insnRegCount, decInsn.vA,
            dvmDetermineCat1Const((s4)decInsn.vB), &okay);
        break;
    case OP_CONST_HIGH16:
        /* could be boolean, int, float, or a null reference */
        setRegisterType(workRegs, insnRegCount, decInsn.vA,
            dvmDetermineCat1Const((s4) decInsn.vB << 16), &okay);
        break;
    case OP_CONST_WIDE_16:
    case OP_CONST_WIDE_32:
    case OP_CONST_WIDE:
    case OP_CONST_WIDE_HIGH16:
        /* could be long or double; default to long and allow conversion */
        setRegisterType(workRegs, insnRegCount, decInsn.vA,
            kRegTypeLongLo, &okay);
        break;
    case OP_CONST_STRING:
    case OP_CONST_STRING_JUMBO:
        assert(gDvm.classJavaLangString != NULL);
        setRegisterType(workRegs, insnRegCount, decInsn.vA,
            regTypeFromClass(gDvm.classJavaLangString), &okay);
        break;
    case OP_CONST_CLASS:
        assert(gDvm.classJavaLangClass != NULL);
        /* make sure we can resolve the class; access check is important */
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vB);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vB);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve const-class %d (%s) in %s\n",
                decInsn.vB, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else {
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                regTypeFromClass(gDvm.classJavaLangClass), &okay);
        }
        break;

    case OP_MONITOR_ENTER:
    case OP_MONITOR_EXIT:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (okay && !regTypeIsReference(tmpType)) {
            LOG_VFY("VFY: monitor op on non-object\n");
            okay = false;
        }
        break;

    case OP_CHECK_CAST:
        /*
         * If this instruction succeeds, we will promote register vA to
         * the type in vB.  (This could be a demotion -- not expected, so
         * we don't try to address it.)
         *
         * If it fails, an exception is thrown, which we deal with later
         * by ignoring the update to decInsn.vA when branching to a handler.
         */
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vB);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vB);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve check-cast %d (%s) in %s\n",
                decInsn.vB, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else {
            RegType origType;

            origType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                        &okay);
            if (!okay)
                break;
            if (!regTypeIsReference(origType)) {
                LOG_VFY("VFY: check-cast on non-reference in v%u\n",decInsn.vA);
                okay = false;
                break;
            }
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                regTypeFromClass(resClass), &okay);
        }
        break;
    case OP_INSTANCE_OF:
        /* make sure we're checking a reference type */
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vB, &okay);
        if (!okay)
            break;
        if (!regTypeIsReference(tmpType)) {
            LOG_VFY("VFY: vB not a reference (%d)\n", tmpType);
            okay = false;
            break;
        }

        /* make sure we can resolve the class; access check is important */
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vC);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vC);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve instanceof %d (%s) in %s\n",
                decInsn.vC, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else {
            /* result is boolean */
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                kRegTypeBoolean, &okay);
        }
        break;

    case OP_ARRAY_LENGTH:
        resClass = getClassFromRegister(workRegs, insnRegCount,
                        decInsn.vB, &okay);
        if (!okay)
            break;
        if (resClass != NULL && !dvmIsArrayClass(resClass)) {
            LOG_VFY("VFY: array-length on non-array\n");
            okay = false;
            break;
        }
        setRegisterType(workRegs, insnRegCount, decInsn.vA, kRegTypeInteger,
            &okay);
        break;

    case OP_NEW_INSTANCE:
        /*
         * We can check for interface and abstract classes here, but we
         * can't reject them.  We can ask the optimizer to replace the
         * instructions with a magic "always throw InstantiationError"
         * instruction.  (Not enough bytes to sub in a method call.)
         */
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vB);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vB);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve new-instance %d (%s) in %s\n",
                decInsn.vB, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else {
            RegType uninitType;

            /* add resolved class to uninit map if not already there */
            int uidx = dvmSetUninitInstance(uninitMap, insnIdx, resClass);
            assert(uidx >= 0);
            uninitType = regTypeFromUninitIndex(uidx);

            /*
             * Any registers holding previous allocations from this address
             * that have not yet been initialized must be marked invalid.
             */
            markUninitRefsAsInvalid(workRegs, insnRegCount, uninitMap,
                uninitType);

            /* add the new uninitialized reference to the register ste */
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                uninitType, &okay);
        }
        break;
    case OP_NEW_ARRAY:
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vC);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vC);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve new-array %d (%s) in %s\n",
                decInsn.vC, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else if (!dvmIsArrayClass(resClass)) {
            LOG_VFY("VFY: new-array on non-array class\n");
            okay = false;
        } else {
            /* make sure "size" register is valid type */
            verifyRegisterType(workRegs, insnRegCount, decInsn.vB,
                kRegTypeInteger, &okay);
            /* set register type to array class */
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                regTypeFromClass(resClass), &okay);
        }
        break;
    case OP_FILLED_NEW_ARRAY:
    case OP_FILLED_NEW_ARRAY_RANGE:
        resClass = dvmOptResolveClass(meth->clazz, decInsn.vB);
        if (resClass == NULL) {
            const char* badClassDesc = dexStringByTypeIdx(pDexFile, decInsn.vB);
            dvmLogUnableToResolveClass(badClassDesc, meth);
            LOG_VFY("VFY: unable to resolve filled-array %d (%s) in %s\n",
                decInsn.vB, badClassDesc, meth->clazz->descriptor);
            okay = false;
        } else if (!dvmIsArrayClass(resClass)) {
            LOG_VFY("VFY: filled-new-array on non-array class\n");
            okay = false;
        } else {
            bool isRange = (decInsn.opCode == OP_FILLED_NEW_ARRAY_RANGE);

            /* check the arguments to the instruction */
            verifyFilledNewArrayRegs(meth, workRegs, insnRegCount, &decInsn,
                resClass, isRange, &okay);
            /* filled-array result goes into "result" register */
            setResultRegisterType(workRegs, insnRegCount,
                regTypeFromClass(resClass), &okay);
            justSetResult = true;
        }
        break;

    case OP_CMPL_FLOAT:
    case OP_CMPG_FLOAT:
        verifyRegisterType(workRegs, insnRegCount, decInsn.vB, kRegTypeFloat,
            &okay);
        verifyRegisterType(workRegs, insnRegCount, decInsn.vC, kRegTypeFloat,
            &okay);
        setRegisterType(workRegs, insnRegCount, decInsn.vA, kRegTypeBoolean,
            &okay);
        break;
    case OP_CMPL_DOUBLE:
    case OP_CMPG_DOUBLE:
        verifyRegisterType(workRegs, insnRegCount, decInsn.vB, kRegTypeDoubleLo,
            &okay);
        verifyRegisterType(workRegs, insnRegCount, decInsn.vC, kRegTypeDoubleLo,
            &okay);
        setRegisterType(workRegs, insnRegCount, decInsn.vA, kRegTypeBoolean,
            &okay);
        break;
    case OP_CMP_LONG:
        verifyRegisterType(workRegs, insnRegCount, decInsn.vB, kRegTypeLongLo,
            &okay);
        verifyRegisterType(workRegs, insnRegCount, decInsn.vC, kRegTypeLongLo,
            &okay);
        setRegisterType(workRegs, insnRegCount, decInsn.vA, kRegTypeBoolean,
            &okay);
        break;

    case OP_THROW:
        resClass = getClassFromRegister(workRegs, insnRegCount,
                        decInsn.vA, &okay);
        if (okay && resClass != NULL) {
            if (!dvmInstanceof(resClass, gDvm.classJavaLangThrowable)) {
                LOG_VFY("VFY: thrown class %s not instanceof Throwable\n",
                        resClass->descriptor);
                okay = false;
            }
        }
        break;

    case OP_GOTO:
    case OP_GOTO_16:
    case OP_GOTO_32:
        /* no effect on or use of registers */
        break;

    case OP_PACKED_SWITCH:
    case OP_SPARSE_SWITCH:
        /* verify that vAA is an integer, or can be converted to one */
        verifyRegisterType(workRegs, insnRegCount, decInsn.vA,
            kRegTypeInteger, &okay);
        break;

    case OP_FILL_ARRAY_DATA:
        {
            RegType valueType;
            const u2 *arrayData;
            u2 elemWidth;

            /* Similar to the verification done for APUT */
            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vA, &okay);
            if (!okay)
                break;

            /* resClass can be null if the reg type is Zero */
            if (resClass == NULL)
                break;

            if (!dvmIsArrayClass(resClass) || resClass->arrayDim != 1 ||
                resClass->elementClass->primitiveType == PRIM_NOT ||
                resClass->elementClass->primitiveType == PRIM_VOID)
            {
                LOG_VFY("VFY: invalid fill-array-data on %s\n",
                        resClass->descriptor);
                okay = false;
                break;
            }

            valueType = primitiveTypeToRegType(
                                    resClass->elementClass->primitiveType);
            assert(valueType != kRegTypeUnknown);

            /*
             * Now verify if the element width in the table matches the element
             * width declared in the array
             */
            arrayData = insns + (insns[1] | (((s4)insns[2]) << 16));
            if (arrayData[0] != kArrayDataSignature) {
                LOG_VFY("VFY: invalid magic for array-data\n");
                okay = false;
                break;
            }

            switch (resClass->elementClass->primitiveType) {
                case PRIM_BOOLEAN:
                case PRIM_BYTE:
                     elemWidth = 1;
                     break;
                case PRIM_CHAR:
                case PRIM_SHORT:
                     elemWidth = 2;
                     break;
                case PRIM_FLOAT:
                case PRIM_INT:
                     elemWidth = 4;
                     break;
                case PRIM_DOUBLE:
                case PRIM_LONG:
                     elemWidth = 8;
                     break;
                default:
                     elemWidth = 0;
                     break;
            }

            /*
             * Since we don't compress the data in Dex, expect to see equal
             * width of data stored in the table and expected from the array
             * class.
             */
            if (arrayData[1] != elemWidth) {
                LOG_VFY("VFY: array-data size mismatch (%d vs %d)\n",
                        arrayData[1], elemWidth);
                okay = false;
            }
        }
        break;

    case OP_IF_EQ:
    case OP_IF_NE:
        {
            RegType type1, type2;
            bool tmpResult;

            type1 = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
            type2 = getRegisterType(workRegs, insnRegCount, decInsn.vB, &okay);
            if (!okay)
                break;

            /* both references? */
            if (regTypeIsReference(type1) && regTypeIsReference(type2))
                break;

            /* both category-1nr? */
            checkTypeCategory(type1, kTypeCategory1nr, &okay);
            checkTypeCategory(type2, kTypeCategory1nr, &okay);
            if (!okay) {
                LOG_VFY("VFY: args to if-eq/if-ne must both be refs or cat1\n");
                break;
            }
        }
        break;
    case OP_IF_LT:
    case OP_IF_GE:
    case OP_IF_GT:
    case OP_IF_LE:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (!okay)
            break;
        checkTypeCategory(tmpType, kTypeCategory1nr, &okay);
        if (!okay) {
            LOG_VFY("VFY: args to 'if' must be cat-1nr\n");
            break;
        }
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vB,&okay);
        if (!okay)
            break;
        checkTypeCategory(tmpType, kTypeCategory1nr, &okay);
        if (!okay) {
            LOG_VFY("VFY: args to 'if' must be cat-1nr\n");
            break;
        }
        break;
    case OP_IF_EQZ:
    case OP_IF_NEZ:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (!okay)
            break;
        if (regTypeIsReference(tmpType))
            break;
        checkTypeCategory(tmpType, kTypeCategory1nr, &okay);
        if (!okay)
            LOG_VFY("VFY: expected cat-1 arg to if\n");
        break;
    case OP_IF_LTZ:
    case OP_IF_GEZ:
    case OP_IF_GTZ:
    case OP_IF_LEZ:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (!okay)
            break;
        checkTypeCategory(tmpType, kTypeCategory1nr, &okay);
        if (!okay)
            LOG_VFY("VFY: expected cat-1 arg to if\n");
        break;

    case OP_AGET:
        tmpType = kRegTypeInteger;
        goto aget_1nr_common;
    case OP_AGET_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto aget_1nr_common;
    case OP_AGET_BYTE:
        tmpType = kRegTypeByte;
        goto aget_1nr_common;
    case OP_AGET_CHAR:
        tmpType = kRegTypeChar;
        goto aget_1nr_common;
    case OP_AGET_SHORT:
        tmpType = kRegTypeShort;
        goto aget_1nr_common;
aget_1nr_common:
        {
            RegType srcType, indexType;

            indexType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                            &okay);
            checkArrayIndexType(meth, indexType, &okay);
            if (!okay)
                break;

            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vB, &okay);
            if (!okay)
                break;
            if (resClass != NULL) {
                /* verify the class */
                if (!dvmIsArrayClass(resClass) || resClass->arrayDim != 1 ||
                    resClass->elementClass->primitiveType == PRIM_NOT)
                {
                    LOG_VFY("VFY: invalid aget-1nr target %s\n",
                        resClass->descriptor);
                    okay = false;
                    break;
                }

                /* make sure array type matches instruction */
                srcType = primitiveTypeToRegType(
                                        resClass->elementClass->primitiveType);

                if (!checkFieldArrayStore1nr(tmpType, srcType)) {
                    LOG_VFY("VFY: invalid aget-1nr, array type=%d with"
                            " inst type=%d (on %s)\n",
                        srcType, tmpType, resClass->descriptor);
                    okay = false;
                    break;
                }

            }
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                tmpType, &okay);
        }
        break;

    case OP_AGET_WIDE:
        {
            RegType dstType, indexType;

            indexType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                            &okay);
            checkArrayIndexType(meth, indexType, &okay);
            if (!okay)
                break;

            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vB, &okay);
            if (!okay)
                break;
            if (resClass != NULL) {
                /* verify the class */
                if (!dvmIsArrayClass(resClass) || resClass->arrayDim != 1 ||
                    resClass->elementClass->primitiveType == PRIM_NOT)
                {
                    LOG_VFY("VFY: invalid aget-wide target %s\n",
                        resClass->descriptor);
                    okay = false;
                    break;
                }

                /* try to refine "dstType" */
                switch (resClass->elementClass->primitiveType) {
                case PRIM_LONG:
                    dstType = kRegTypeLongLo;
                    break;
                case PRIM_DOUBLE:
                    dstType = kRegTypeDoubleLo;
                    break;
                default:
                    LOG_VFY("VFY: invalid aget-wide on %s\n",
                        resClass->descriptor);
                    dstType = kRegTypeUnknown;
                    okay = false;
                    break;
                }
            } else {
                /*
                 * Null array ref; this code path will fail at runtime.  We
                 * know this is either long or double, and we don't really
                 * discriminate between those during verification, so we
                 * call it a long.
                 */
                dstType = kRegTypeLongLo;
            }
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                dstType, &okay);
        }
        break;

    case OP_AGET_OBJECT:
        {
            RegType dstType, indexType;

            indexType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                            &okay);
            checkArrayIndexType(meth, indexType, &okay);
            if (!okay)
                break;

            /* get the class of the array we're pulling an object from */
            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vB, &okay);
            if (!okay)
                break;
            if (resClass != NULL) {
                ClassObject* elementClass;

                assert(resClass != NULL);
                if (!dvmIsArrayClass(resClass)) {
                    LOG_VFY("VFY: aget-object on non-array class\n");
                    okay = false;
                    break;
                }
                assert(resClass->elementClass != NULL);

                /*
                 * Find the element class.  resClass->elementClass indicates
                 * the basic type, which won't be what we want for a
                 * multi-dimensional array.
                 */
                if (resClass->descriptor[1] == '[') {
                    assert(resClass->arrayDim > 1);
                    elementClass = dvmFindArrayClass(&resClass->descriptor[1],
                                        resClass->classLoader);
                } else if (resClass->descriptor[1] == 'L') {
                    assert(resClass->arrayDim == 1);
                    elementClass = resClass->elementClass;
                } else {
                    LOG_VFY("VFY: aget-object on non-ref array class (%s)\n",
                        resClass->descriptor);
                    okay = false;
                    break;
                }

                dstType = regTypeFromClass(elementClass);
            } else {
                /*
                 * The array reference is NULL, so the current code path will
                 * throw an exception.  For proper merging with later code
                 * paths, and correct handling of "if-eqz" tests on the
                 * result of the array get, we want to treat this as a null
                 * reference.
                 */
                dstType = kRegTypeZero;
            }
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                dstType, &okay);
        }
        break;
    case OP_APUT:
        tmpType = kRegTypeInteger;
        goto aput_1nr_common;
    case OP_APUT_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto aput_1nr_common;
    case OP_APUT_BYTE:
        tmpType = kRegTypeByte;
        goto aput_1nr_common;
    case OP_APUT_CHAR:
        tmpType = kRegTypeChar;
        goto aput_1nr_common;
    case OP_APUT_SHORT:
        tmpType = kRegTypeShort;
        goto aput_1nr_common;
aput_1nr_common:
        {
            RegType srcType, dstType, indexType;

            indexType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                            &okay);
            checkArrayIndexType(meth, indexType, &okay);
            if (!okay)
                break;

            /* make sure the source register has the correct type */
            srcType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                            &okay);
            if (!canConvertTo1nr(srcType, tmpType)) {
                LOG_VFY("VFY: invalid reg type %d on aput instr (need %d)\n",
                    srcType, tmpType);
                okay = false;
                break;
            }

            resClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vB, &okay);
            if (!okay)
                break;

            /* resClass can be null if the reg type is Zero */
            if (resClass == NULL)
                break;

            if (!dvmIsArrayClass(resClass) || resClass->arrayDim != 1 ||
                resClass->elementClass->primitiveType == PRIM_NOT)
            {
                LOG_VFY("VFY: invalid aput-1nr on %s\n", resClass->descriptor);
                okay = false;
                break;
            }

            /* verify that instruction matches array */
            dstType = primitiveTypeToRegType(
                                    resClass->elementClass->primitiveType);
            assert(dstType != kRegTypeUnknown);

            if (!checkFieldArrayStore1nr(tmpType, dstType)) {
                LOG_VFY("VFY: invalid aput-1nr on %s (inst=%d dst=%d)\n",
                        resClass->descriptor, tmpType, dstType);
                okay = false;
                break;
            }
        }
        break;
    case OP_APUT_WIDE:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                        &okay);
        checkArrayIndexType(meth, tmpType, &okay);
        if (!okay)
            break;

        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (okay) {
            RegType typeHi =
                getRegisterType(workRegs, insnRegCount, decInsn.vA+1, &okay);
            checkTypeCategory(tmpType, kTypeCategory2, &okay);
            checkWidePair(tmpType, typeHi, &okay);
        }
        if (!okay)
            break;

        resClass = getClassFromRegister(workRegs, insnRegCount,
                        decInsn.vB, &okay);
        if (!okay)
            break;
        if (resClass != NULL) {
            /* verify the class and try to refine "dstType" */
            if (!dvmIsArrayClass(resClass) || resClass->arrayDim != 1 ||
                resClass->elementClass->primitiveType == PRIM_NOT)
            {
                LOG_VFY("VFY: invalid aput-wide on %s\n",
                        resClass->descriptor);
                okay = false;
                break;
            }

            switch (resClass->elementClass->primitiveType) {
            case PRIM_LONG:
            case PRIM_DOUBLE:
                /* these are okay */
                break;
            default:
                LOG_VFY("VFY: invalid aput-wide on %s\n",
                        resClass->descriptor);
                okay = false;
                break;
            }
        }
        break;
    case OP_APUT_OBJECT:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vC,
                        &okay);
        checkArrayIndexType(meth, tmpType, &okay);
        if (!okay)
            break;

        /* get the ref we're storing; Zero is okay, Uninit is not */
        resClass = getClassFromRegister(workRegs, insnRegCount,
                        decInsn.vA, &okay);
        if (!okay)
            break;
        if (resClass != NULL) {
            ClassObject* arrayClass;
            ClassObject* elementClass;

            /*
             * Get the array class.  If the array ref is null, we won't
             * have type information (and we'll crash at runtime with a
             * null pointer exception).
             */
            arrayClass = getClassFromRegister(workRegs, insnRegCount,
                            decInsn.vB, &okay);

            if (arrayClass != NULL) {
                /* see if the array holds a compatible type */
                if (!dvmIsArrayClass(arrayClass)) {
                    LOG_VFY("VFY: invalid aput-object on %s\n",
                            arrayClass->descriptor);
                    okay = false;
                    break;
                }

                /*
                 * Find the element class.  resClass->elementClass indicates
                 * the basic type, which won't be what we want for a
                 * multi-dimensional array.
                 *
                 * All we want to check here is that the element type is a
                 * reference class.  We *don't* check instanceof here, because
                 * you can still put a String into a String[] after the latter
                 * has been cast to an Object[].
                 */
                if (arrayClass->descriptor[1] == '[') {
                    assert(arrayClass->arrayDim > 1);
                    elementClass = dvmFindArrayClass(&arrayClass->descriptor[1],
                                        arrayClass->classLoader);
                } else {
                    assert(arrayClass->arrayDim == 1);
                    elementClass = arrayClass->elementClass;
                }
                if (elementClass->primitiveType != PRIM_NOT) {
                    LOG_VFY("VFY: invalid aput-object of %s into %s\n",
                            resClass->descriptor, arrayClass->descriptor);
                    okay = false;
                    break;
                }
            }
        }
        break;

    case OP_IGET:
        tmpType = kRegTypeInteger;
        goto iget_1nr_common;
    case OP_IGET_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto iget_1nr_common;
    case OP_IGET_BYTE:
        tmpType = kRegTypeByte;
        goto iget_1nr_common;
    case OP_IGET_CHAR:
        tmpType = kRegTypeChar;
        goto iget_1nr_common;
    case OP_IGET_SHORT:
        tmpType = kRegTypeShort;
        goto iget_1nr_common;
iget_1nr_common:
        {
            ClassObject* fieldClass;
            InstField* instField;
            RegType objType, fieldType;

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;

            /* make sure the field's type is compatible with expectation */
            fieldType = primSigCharToRegType(instField->field.signature[0]);
            if (fieldType == kRegTypeUnknown ||
                !checkFieldArrayStore1nr(tmpType, fieldType))
            {
                LOG_VFY("VFY: invalid iget-1nr of %s.%s (inst=%d field=%d)\n",
                        instField->field.clazz->descriptor,
                        instField->field.name, tmpType, fieldType);
                okay = false;
                break;
            }

            setRegisterType(workRegs, insnRegCount, decInsn.vA, tmpType, &okay);
        }
        break;
    case OP_IGET_WIDE:
        {
            RegType dstType;
            ClassObject* fieldClass;
            InstField* instField;
            RegType objType;

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;
            /* check the type, which should be prim */
            switch (instField->field.signature[0]) {
            case 'D':
                dstType = kRegTypeDoubleLo;
                break;
            case 'J':
                dstType = kRegTypeLongLo;
                break;
            default:
                LOG_VFY("VFY: invalid iget-wide of %s.%s\n",
                        instField->field.clazz->descriptor,
                        instField->field.name);
                dstType = kRegTypeUnknown;
                okay = false;
                break;
            }
            if (okay) {
                setRegisterType(workRegs, insnRegCount, decInsn.vA,
                    dstType, &okay);
            }
        }
        break;
    case OP_IGET_OBJECT:
        {
            ClassObject* fieldClass;
            InstField* instField;
            RegType objType;

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;
            fieldClass = getFieldClass(meth, &instField->field);
            if (fieldClass == NULL) {
                /* class not found or primitive type */
                LOG_VFY("VFY: unable to recover field class from '%s'\n",
                    instField->field.signature);
                okay = false;
                break;
            }
            if (okay) {
                assert(!dvmIsPrimitiveClass(fieldClass));
                setRegisterType(workRegs, insnRegCount, decInsn.vA,
                    regTypeFromClass(fieldClass), &okay);
            }
        }
        break;
    case OP_IPUT:
        tmpType = kRegTypeInteger;
        goto iput_1nr_common;
    case OP_IPUT_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto iput_1nr_common;
    case OP_IPUT_BYTE:
        tmpType = kRegTypeByte;
        goto iput_1nr_common;
    case OP_IPUT_CHAR:
        tmpType = kRegTypeChar;
        goto iput_1nr_common;
    case OP_IPUT_SHORT:
        tmpType = kRegTypeShort;
        goto iput_1nr_common;
iput_1nr_common:
        {
            RegType srcType, fieldType, objType;
            ClassObject* fieldClass;
            InstField* instField;

            /* make sure the source register has the correct type */
            srcType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                        &okay);
            if (!canConvertTo1nr(srcType, tmpType)) {
                LOG_VFY("VFY: invalid reg type %d on iput instr (need %d)\n",
                    srcType, tmpType);
                okay = false;
                break;
            }

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &instField->field, &okay);
            if (!okay)
                break;

            /* get type of field we're storing into */
            fieldType = primSigCharToRegType(instField->field.signature[0]);
            if (fieldType == kRegTypeUnknown ||
                !checkFieldArrayStore1nr(tmpType, fieldType))
            {
                LOG_VFY("VFY: invalid iput-1nr of %s.%s (inst=%d field=%d)\n",
                        instField->field.clazz->descriptor,
                        instField->field.name, tmpType, fieldType);
                okay = false;
                break;
            }
        }
        break;
    case OP_IPUT_WIDE:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (okay) {
            RegType typeHi =
                getRegisterType(workRegs, insnRegCount, decInsn.vA+1, &okay);
            checkTypeCategory(tmpType, kTypeCategory2, &okay);
            checkWidePair(tmpType, typeHi, &okay);
        }
        if (okay) {
            ClassObject* fieldClass;
            InstField* instField;
            RegType objType;

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &instField->field, &okay);
            if (!okay)
                break;

            /* check the type, which should be prim */
            switch (instField->field.signature[0]) {
            case 'D':
            case 'J':
                /* these are okay (and interchangeable) */
                break;
            default:
                LOG_VFY("VFY: invalid iput-wide of %s.%s\n",
                        instField->field.clazz->descriptor,
                        instField->field.name);
                okay = false;
                break;
            }
        }
        break;
    case OP_IPUT_OBJECT:
        {
            ClassObject* fieldClass;
            ClassObject* valueClass;
            InstField* instField;
            RegType objType, valueType;

            objType = getRegisterType(workRegs, insnRegCount, decInsn.vB,
                        &okay);
            if (!okay)
                break;
            instField = getInstField(meth, uninitMap, objType, decInsn.vC,
                            &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &instField->field, &okay);
            if (!okay)
                break;

            fieldClass = getFieldClass(meth, &instField->field);
            if (fieldClass == NULL) {
                LOG_VFY("VFY: unable to recover field class from '%s'\n",
                    instField->field.signature);
                okay = false;
                break;
            }

            valueType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                        &okay);
            if (!okay)
                break;
            if (!regTypeIsReference(valueType)) {
                LOG_VFY("VFY: storing non-ref v%d into ref field '%s' (%s)\n",
                        decInsn.vA, instField->field.name,
                        fieldClass->descriptor);
                okay = false;
                break;
            }
            if (valueType != kRegTypeZero) {
                valueClass = regTypeInitializedReferenceToClass(valueType);
                if (valueClass == NULL) {
                    LOG_VFY("VFY: storing uninit ref v%d into ref field\n",
                        decInsn.vA);
                    okay = false;
                    break;
                }
                /* allow if field is any interface or field is base class */
                if (!dvmIsInterfaceClass(fieldClass) &&
                    !dvmInstanceof(valueClass, fieldClass))
                {
                    LOG_VFY("VFY: storing type '%s' into field type '%s' (%s.%s)\n",
                            valueClass->descriptor, fieldClass->descriptor,
                            instField->field.clazz->descriptor,
                            instField->field.name);
                    okay = false;
                    break;
                }
            }
        }
        break;

    case OP_SGET:
        tmpType = kRegTypeInteger;
        goto sget_1nr_common;
    case OP_SGET_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto sget_1nr_common;
    case OP_SGET_BYTE:
        tmpType = kRegTypeByte;
        goto sget_1nr_common;
    case OP_SGET_CHAR:
        tmpType = kRegTypeChar;
        goto sget_1nr_common;
    case OP_SGET_SHORT:
        tmpType = kRegTypeShort;
        goto sget_1nr_common;
sget_1nr_common:
        {
            StaticField* staticField;
            RegType fieldType;

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;

            /*
             * Make sure the field's type is compatible with expectation.
             * We can get ourselves into trouble if we mix & match loads
             * and stores with different widths, so rather than just checking
             * "canConvertTo1nr" we require that the field types have equal
             * widths.  (We can't generally require an exact type match,
             * because e.g. "int" and "float" are interchangeable.)
             */
            fieldType = primSigCharToRegType(staticField->field.signature[0]);
            if (!checkFieldArrayStore1nr(tmpType, fieldType)) {
                LOG_VFY("VFY: invalid sget-1nr of %s.%s (inst=%d actual=%d)\n",
                    staticField->field.clazz->descriptor,
                    staticField->field.name, tmpType, fieldType);
                okay = false;
                break;
            }

            setRegisterType(workRegs, insnRegCount, decInsn.vA, tmpType, &okay);
        }
        break;
    case OP_SGET_WIDE:
        {
            StaticField* staticField;
            RegType dstType;

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;
            /* check the type, which should be prim */
            switch (staticField->field.signature[0]) {
            case 'D':
                dstType = kRegTypeDoubleLo;
                break;
            case 'J':
                dstType = kRegTypeLongLo;
                break;
            default:
                LOG_VFY("VFY: invalid sget-wide of %s.%s\n",
                        staticField->field.clazz->descriptor,
                        staticField->field.name);
                dstType = kRegTypeUnknown;
                okay = false;
                break;
            }
            if (okay) {
                setRegisterType(workRegs, insnRegCount, decInsn.vA,
                    dstType, &okay);
            }
        }
        break;
    case OP_SGET_OBJECT:
        {
            StaticField* staticField;
            ClassObject* fieldClass;

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;
            fieldClass = getFieldClass(meth, &staticField->field);
            if (fieldClass == NULL) {
                LOG_VFY("VFY: unable to recover field class from '%s'\n",
                    staticField->field.signature);
                okay = false;
                break;
            }
            if (dvmIsPrimitiveClass(fieldClass)) {
                LOG_VFY("VFY: attempt to get prim field with sget-object\n");
                okay = false;
                break;
            }
            setRegisterType(workRegs, insnRegCount, decInsn.vA,
                regTypeFromClass(fieldClass), &okay);
        }
        break;
    case OP_SPUT:
        tmpType = kRegTypeInteger;
        goto sput_1nr_common;
    case OP_SPUT_BOOLEAN:
        tmpType = kRegTypeBoolean;
        goto sput_1nr_common;
    case OP_SPUT_BYTE:
        tmpType = kRegTypeByte;
        goto sput_1nr_common;
    case OP_SPUT_CHAR:
        tmpType = kRegTypeChar;
        goto sput_1nr_common;
    case OP_SPUT_SHORT:
        tmpType = kRegTypeShort;
        goto sput_1nr_common;
sput_1nr_common:
        {
            RegType srcType, fieldType;
            StaticField* staticField;

            /* make sure the source register has the correct type */
            srcType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                        &okay);
            if (!canConvertTo1nr(srcType, tmpType)) {
                LOG_VFY("VFY: invalid reg type %d on iput instr (need %d)\n",
                    srcType, tmpType);
                okay = false;
                break;
            }

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &staticField->field, &okay);
            if (!okay)
                break;

            /*
             * Get type of field we're storing into.  We know that the
             * contents of the register match the instruction, but we also
             * need to ensure that the instruction matches the field type.
             * Using e.g. sput-short to write into a 32-bit integer field
             * can lead to trouble if we do 16-bit writes.
             */
            fieldType = primSigCharToRegType(staticField->field.signature[0]);
            if (!checkFieldArrayStore1nr(tmpType, fieldType)) {
                LOG_VFY("VFY: invalid sput-1nr of %s.%s (inst=%d actual=%d)\n",
                    staticField->field.clazz->descriptor,
                    staticField->field.name, tmpType, fieldType);
                okay = false;
                break;
            }
        }
        break;
    case OP_SPUT_WIDE:
        tmpType = getRegisterType(workRegs, insnRegCount, decInsn.vA, &okay);
        if (okay) {
            RegType typeHi =
                getRegisterType(workRegs, insnRegCount, decInsn.vA+1, &okay);
            checkTypeCategory(tmpType, kTypeCategory2, &okay);
            checkWidePair(tmpType, typeHi, &okay);
        }
        if (okay) {
            StaticField* staticField;

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &staticField->field, &okay);
            if (!okay)
                break;

            /* check the type, which should be prim */
            switch (staticField->field.signature[0]) {
            case 'D':
            case 'J':
                /* these are okay */
                break;
            default:
                LOG_VFY("VFY: invalid sput-wide of %s.%s\n",
                        staticField->field.clazz->descriptor,
                        staticField->field.name);
                okay = false;
                break;
            }
        }
        break;
    case OP_SPUT_OBJECT:
        {
            ClassObject* fieldClass;
            ClassObject* valueClass;
            StaticField* staticField;
            RegType valueType;

            staticField = getStaticField(meth, decInsn.vB, &okay);
            if (!okay)
                break;
            checkFinalFieldAccess(meth, &staticField->field, &okay);
            if (!okay)
                break;

            fieldClass = getFieldClass(meth, &staticField->field);
            if (fieldClass == NULL) {
                LOG_VFY("VFY: unable to recover field class from '%s'\n",
                    staticField->field.signature);
                okay = false;
                break;
            }

            valueType = getRegisterType(workRegs, insnRegCount, decInsn.vA,
                        &okay);
            if (!okay)
                break;
            if (!regTypeIsReference(valueType)) {
                LOG_VFY("VFY: storing non-ref v%d into ref field '%s' (%s)\n",
                        decInsn.vA, staticField->field.name,
                        fieldClass->descriptor);
                okay = false;
                break;
            }
            if (valueType != kRegTypeZero) {
                valueClass = regTypeInitializedReferenceToClass(valueType);
                if (valueClass == NULL) {
                    LOG_VFY("VFY: storing uninit ref v%d into ref field\n",
                        decInsn.vA);
                    okay = false;
                    break;
                }
                /* allow if field is any interface or field is base class */
                if (!dvmIsInterfaceClass(fieldClass) &&
                    !dvmInstanceof(valueClass, fieldClass))
                {
                    LOG_VFY("VFY: storing type '%s' into field type '%s' (%s.%s)\n",
                            valueClass->descriptor, fieldClass->descriptor,
                            staticField->field.clazz->descriptor,
                            staticField->field.name);
                    okay = false;
                    break;
                }
            }
        }
        break;

    case OP_INVOKE_VIRTUAL:
    case OP_INVOKE_VIRTUAL_RANGE:
    case OP_INVOKE_SUPER:
    case OP_INVOKE_SUPER_RANGE:
        {
            Method* calledMethod;
            RegType returnType;
            bool isRange;
            bool isSuper;

            isRange =  (decInsn.opCode == OP_INVOKE_VIRTUAL_RANGE ||
                        decInsn.opCode == OP_INVOKE_SUPER_RANGE);
            isSuper =  (decInsn.opCode == OP_INVOKE_SUPER ||
                        decInsn.opCode == OP_INVOKE_SUPER_RANGE);

            calledMethod = verifyInvocationArgs(meth, workRegs, insnRegCount,
                            &decInsn, uninitMap, METHOD_VIRTUAL, isRange,
                            isSuper, &okay);
            if (!okay)
                break;
            returnType = getMethodReturnType(calledMethod);
            setResultRegisterType(workRegs, insnRegCount, returnType, &okay);
            justSetResult = true;
        }
        break;
    case OP_INVOKE_DIRECT:
    case OP_INVOKE_DIRECT_RANGE:
        {
            RegType returnType;
            Method* calledMethod;
            bool isRange;

            isRange =  (decInsn.opCode == OP_INVOKE_DIRECT_RANGE);
            calledMethod = verifyInvocationArgs(meth, workRegs, insnRegCount,
                            &decInsn, uninitMap, METHOD_DIRECT, isRange,
                            false, &okay);
            if (!okay)
                break;

            /*
             * Some additional checks when calling <init>.  We know from
             * the invocation arg check that the "this" argument is an
             * instance of calledMethod->clazz.  Now we further restrict
             * that to require that calledMethod->clazz is the same as
             * this->clazz or this->super, allowing the latter only if
             * the "this" argument is the same as the "this" argument to
             * this method (which implies that we're in <init> ourselves).
             */
            if (isInitMethod(calledMethod)) {
                RegType thisType;
                thisType = getInvocationThis(workRegs, insnRegCount,
                            &decInsn, &okay);
                if (!okay)
                    break;

                /* no null refs allowed (?) */
                if (thisType == kRegTypeZero) {
                    LOG_VFY("VFY: unable to initialize null ref\n");
                    okay = false;
                    break;
                }

                ClassObject* thisClass;

                thisClass = regTypeReferenceToClass(thisType, uninitMap);
                assert(thisClass != NULL);

                /* must be in same class or in superclass */
                if (calledMethod->clazz == thisClass->super) {
                    if (thisClass != meth->clazz) {
                        LOG_VFY("VFY: invoke-direct <init> on super only "
                            "allowed for 'this' in <init>");
                        okay = false;
                        break;
                    }
                }  else if (calledMethod->clazz != thisClass) {
                    LOG_VFY("VFY: invoke-direct <init> must be on current "
                            "class or super\n");
                    okay = false;
                    break;
                }

                /* arg must be an uninitialized reference */
                if (!regTypeIsUninitReference(thisType)) {
                    LOG_VFY("VFY: can only initialize the uninitialized\n");
                    okay = false;
                    break;
                }

                /*
                 * Replace the uninitialized reference with an initialized
                 * one, and clear the entry in the uninit map.  We need to
                 * do this for all registers that have the same object
                 * instance in them, not just the "this" register.
                 */
                int uidx = regTypeToUninitIndex(thisType);
                markRefsAsInitialized(workRegs, insnRegCount, uninitMap,
                    thisType, &okay);
                if (!okay)
                    break;
            }
            returnType = getMethodReturnType(calledMethod);
            setResultRegisterType(workRegs, insnRegCount,
                returnType, &okay);
            justSetResult = true;
        }
        break;
    case OP_INVOKE_STATIC:
    case OP_INVOKE_STATIC_RANGE:
        {
            RegType returnType;
            Method* calledMethod;
            bool isRange;

            isRange =  (decInsn.opCode == OP_INVOKE_STATIC_RANGE);
            calledMethod = verifyInvocationArgs(meth, workRegs, insnRegCount,
                            &decInsn, uninitMap, METHOD_STATIC, isRange,
                            false, &okay);
            if (!okay)
                break;

            returnType = getMethodReturnType(calledMethod);
            setResultRegisterType(workRegs, insnRegCount, returnType, &okay);
            justSetResult = true;
        }
        break;
    case OP_INVOKE_INTERFACE:
    case OP_INVOKE_INTERFACE_RANGE:
        {
            RegType /*thisType,*/ returnType;
            Method* absMethod;
            bool isRange;

            isRange =  (decInsn.opCode == OP_INVOKE_INTERFACE_RANGE);
            absMethod = verifyInvocationArgs(meth, workRegs, insnRegCount,
                            &decInsn, uninitMap, METHOD_INTERFACE, isRange,
                            false, &okay);
            if (!okay)
                break;

#if 0       /* can't do this here, fails on dalvik test 052-verifier-fun */
            /*
             * Get the type of the "this" arg, which should always be an
             * interface class.  Because we don't do a full merge on
             * interface classes, this might have reduced to Object.
             */
            thisType = getInvocationThis(workRegs, insnRegCount,
                        &decInsn, &okay);
            if (!okay)
                break;

            if (thisType == kRegTypeZero) {
                /* null pointer always passes (and always fails at runtime) */
            } else {
                ClassObject* thisClass;

                thisClass = regTypeInitializedReferenceToClass(thisType);
                if (thisClass == NULL) {
                    LOG_VFY("VFY: interface call on uninitialized\n");
                    okay = false;
                    break;
                }

                /*
                 * Either "thisClass" needs to be the interface class that
                 * defined absMethod, or absMethod's class needs to be one
                 * of the interfaces implemented by "thisClass".  (Or, if
                 * we couldn't complete the merge, this will be Object.)
                 */
                if (thisClass != absMethod->clazz &&
                    thisClass != gDvm.classJavaLangObject &&
                    !dvmImplements(thisClass, absMethod->clazz))
                {
                    LOG_VFY("VFY: unable to match absMethod '%s' with %s interfaces\n",
                            absMethod->name, thisClass->descriptor);
                    okay = false;
                    break;
                }
            }
#endif

            /*
             * We don't have an object instance, so we can't find the
             * concrete method.  However, all of the type information is
             * in the abstract method, so we're good.
             */
            returnType = getMethodReturnType(absMethod);
            setResultRegisterType(workRegs, insnRegCount, returnType, &okay);
            justSetResult = true;
        }
        break;

    case OP_NEG_INT:
    case OP_NOT_INT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, &okay);
        break;
    case OP_NEG_LONG:
    case OP_NOT_LONG:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeLongLo, &okay);
        break;
    case OP_NEG_FLOAT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeFloat, &okay);
        break;
    case OP_NEG_DOUBLE:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeDoubleLo, &okay);
        break;
    case OP_INT_TO_LONG:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeInteger, &okay);
        break;
    case OP_INT_TO_FLOAT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeInteger, &okay);
        break;
    case OP_INT_TO_DOUBLE:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeInteger, &okay);
        break;
    case OP_LONG_TO_INT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeLongLo, &okay);
        break;
    case OP_LONG_TO_FLOAT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeLongLo, &okay);
        break;
    case OP_LONG_TO_DOUBLE:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeLongLo, &okay);
        break;
    case OP_FLOAT_TO_INT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeFloat, &okay);
        break;
    case OP_FLOAT_TO_LONG:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeFloat, &okay);
        break;
    case OP_FLOAT_TO_DOUBLE:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeFloat, &okay);
        break;
    case OP_DOUBLE_TO_INT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeDoubleLo, &okay);
        break;
    case OP_DOUBLE_TO_LONG:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeDoubleLo, &okay);
        break;
    case OP_DOUBLE_TO_FLOAT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeDoubleLo, &okay);
        break;
    case OP_INT_TO_BYTE:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeByte, kRegTypeInteger, &okay);
        break;
    case OP_INT_TO_CHAR:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeChar, kRegTypeInteger, &okay);
        break;
    case OP_INT_TO_SHORT:
        checkUnop(workRegs, insnRegCount, &decInsn,
            kRegTypeShort, kRegTypeInteger, &okay);
        break;

    case OP_ADD_INT:
    case OP_SUB_INT:
    case OP_MUL_INT:
    case OP_REM_INT:
    case OP_DIV_INT:
    case OP_SHL_INT:
    case OP_SHR_INT:
    case OP_USHR_INT:
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, kRegTypeInteger, false, &okay);
        break;
    case OP_AND_INT:
    case OP_OR_INT:
    case OP_XOR_INT:
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, kRegTypeInteger, true, &okay);
        break;
    case OP_ADD_LONG:
    case OP_SUB_LONG:
    case OP_MUL_LONG:
    case OP_DIV_LONG:
    case OP_REM_LONG:
    case OP_AND_LONG:
    case OP_OR_LONG:
    case OP_XOR_LONG:
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeLongLo, kRegTypeLongLo, false, &okay);
        break;
    case OP_SHL_LONG:
    case OP_SHR_LONG:
    case OP_USHR_LONG:
        /* shift distance is Int, making these different from other binops */
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeLongLo, kRegTypeInteger, false, &okay);
        break;
    case OP_ADD_FLOAT:
    case OP_SUB_FLOAT:
    case OP_MUL_FLOAT:
    case OP_DIV_FLOAT:
    case OP_REM_FLOAT:
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeFloat, kRegTypeFloat, false, &okay);
        break;
    case OP_ADD_DOUBLE:
    case OP_SUB_DOUBLE:
    case OP_MUL_DOUBLE:
    case OP_DIV_DOUBLE:
    case OP_REM_DOUBLE:
        checkBinop(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeDoubleLo, kRegTypeDoubleLo, false, &okay);
        break;
    case OP_ADD_INT_2ADDR:
    case OP_SUB_INT_2ADDR:
    case OP_MUL_INT_2ADDR:
    case OP_REM_INT_2ADDR:
    case OP_SHL_INT_2ADDR:
    case OP_SHR_INT_2ADDR:
    case OP_USHR_INT_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, kRegTypeInteger, false, &okay);
        break;
    case OP_AND_INT_2ADDR:
    case OP_OR_INT_2ADDR:
    case OP_XOR_INT_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, kRegTypeInteger, true, &okay);
        break;
    case OP_DIV_INT_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, kRegTypeInteger, false, &okay);
        break;
    case OP_ADD_LONG_2ADDR:
    case OP_SUB_LONG_2ADDR:
    case OP_MUL_LONG_2ADDR:
    case OP_DIV_LONG_2ADDR:
    case OP_REM_LONG_2ADDR:
    case OP_AND_LONG_2ADDR:
    case OP_OR_LONG_2ADDR:
    case OP_XOR_LONG_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeLongLo, kRegTypeLongLo, false, &okay);
        break;
    case OP_SHL_LONG_2ADDR:
    case OP_SHR_LONG_2ADDR:
    case OP_USHR_LONG_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeLongLo, kRegTypeLongLo, kRegTypeInteger, false, &okay);
        break;
    case OP_ADD_FLOAT_2ADDR:
    case OP_SUB_FLOAT_2ADDR:
    case OP_MUL_FLOAT_2ADDR:
    case OP_DIV_FLOAT_2ADDR:
    case OP_REM_FLOAT_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeFloat, kRegTypeFloat, kRegTypeFloat, false, &okay);
        break;
    case OP_ADD_DOUBLE_2ADDR:
    case OP_SUB_DOUBLE_2ADDR:
    case OP_MUL_DOUBLE_2ADDR:
    case OP_DIV_DOUBLE_2ADDR:
    case OP_REM_DOUBLE_2ADDR:
        checkBinop2addr(workRegs, insnRegCount, &decInsn,
            kRegTypeDoubleLo, kRegTypeDoubleLo, kRegTypeDoubleLo, false, &okay);
        break;
    case OP_ADD_INT_LIT16:
    case OP_RSUB_INT:
    case OP_MUL_INT_LIT16:
    case OP_DIV_INT_LIT16:
    case OP_REM_INT_LIT16:
        checkLitop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, false, &okay);
        break;
    case OP_AND_INT_LIT16:
    case OP_OR_INT_LIT16:
    case OP_XOR_INT_LIT16:
        checkLitop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, true, &okay);
        break;
    case OP_ADD_INT_LIT8:
    case OP_RSUB_INT_LIT8:
    case OP_MUL_INT_LIT8:
    case OP_DIV_INT_LIT8:
    case OP_REM_INT_LIT8:
    case OP_SHL_INT_LIT8:
    case OP_SHR_INT_LIT8:
    case OP_USHR_INT_LIT8:
        checkLitop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, false, &okay);
        break;
    case OP_AND_INT_LIT8:
    case OP_OR_INT_LIT8:
    case OP_XOR_INT_LIT8:
        checkLitop(workRegs, insnRegCount, &decInsn,
            kRegTypeInteger, kRegTypeInteger, true, &okay);
        break;


    /*
     * Verifying "quickened" instructions is tricky, because we have
     * discarded the original field/method information.  The byte offsets
     * and vtable indices only have meaning in the context of an object
     * instance.
     *
     * If a piece of code declares a local reference variable, assigns
     * null to it, and then issues a virtual method call on it, we
     * cannot evaluate the method call during verification.  This situation
     * isn't hard to handle, since we know the call will always result in an
     * NPE, and the arguments and return value don't matter.  Any code that
     * depends on the result of the method call is inaccessible, so the
     * fact that we can't fully verify anything that comes after the bad
     * call is not a problem.
     *
     * We must also consider the case of multiple code paths, only some of
     * which involve a null reference.  We can completely verify the method
     * if we sidestep the results of executing with a null reference.
     * For example, if on the first pass through the code we try to do a
     * virtual method invocation through a null ref, we have to skip the
     * method checks and have the method return a "wildcard" type (which
     * merges with anything to become that other thing).  The move-result
     * will tell us if it's a reference, single-word numeric, or double-word
     * value.  We continue to perform the verification, and at the end of
     * the function any invocations that were never fully exercised are
     * marked as null-only.
     *
     * We would do something similar for the field accesses.  The field's
     * type, once known, can be used to recover the width of short integers.
     * If the object reference was null, the field-get returns the "wildcard"
     * type, which is acceptable for any operation.
     */
    case OP_EXECUTE_INLINE:
    case OP_INVOKE_DIRECT_EMPTY:
    case OP_IGET_QUICK:
    case OP_IGET_WIDE_QUICK:
    case OP_IGET_OBJECT_QUICK:
    case OP_IPUT_QUICK:
    case OP_IPUT_WIDE_QUICK:
    case OP_IPUT_OBJECT_QUICK:
    case OP_INVOKE_VIRTUAL_QUICK:
    case OP_INVOKE_VIRTUAL_QUICK_RANGE:
    case OP_INVOKE_SUPER_QUICK:
    case OP_INVOKE_SUPER_QUICK_RANGE:
        okay = false;
        break;

    /* these should never appear */
    case OP_UNUSED_3E:
    case OP_UNUSED_3F:
    case OP_UNUSED_40:
    case OP_UNUSED_41:
    case OP_UNUSED_42:
    case OP_UNUSED_43:
    case OP_UNUSED_73:
    case OP_UNUSED_79:
    case OP_UNUSED_7A:
    case OP_UNUSED_E3:
    case OP_UNUSED_E4:
    case OP_UNUSED_E5:
    case OP_UNUSED_E6:
    case OP_UNUSED_E7:
    case OP_UNUSED_E8:
    case OP_UNUSED_E9:
    case OP_UNUSED_EA:
    case OP_UNUSED_EB:
    case OP_UNUSED_EC:
    case OP_UNUSED_ED:
    case OP_UNUSED_EF:
    case OP_UNUSED_F1:
    case OP_UNUSED_FC:
    case OP_UNUSED_FD:
    case OP_UNUSED_FE:
    case OP_UNUSED_FF:
        okay = false;
        break;

    /*
     * DO NOT add a "default" clause here.  Without it the compiler will
     * complain if an instruction is missing (which is desirable).
     */
    }

    if (!okay) {
        LOG_VFY_METH(meth, "VFY:  rejecting opcode 0x%02x at 0x%04x\n",
            decInsn.opCode, insnIdx);
        goto bail;
    }

    /*
     * If we didn't just set the result register, clear it out.  This
     * ensures that you can only use "move-result" immediately after the
     * result is set.
     */
    if (!justSetResult) {
        int reg = RESULT_REGISTER(insnRegCount);
        workRegs[reg] = workRegs[reg+1] = kRegTypeUnknown;
    }

    /*
     * Handle "continue".  Tag the next consecutive instruction.
     */
    if ((nextFlags & kInstrCanContinue) != 0) {
        int insnWidth = dvmInsnGetWidth(insnFlags, insnIdx);
        if (insnIdx+insnWidth >= insnsSize) {
            LOG_VFY_METH(meth,
                "VFY: execution can walk off end of code area (from 0x%x)\n",
                insnIdx);
            goto bail;
        }

        /*
         * The only way to get to a move-exception instruction is to get
         * thrown there.  Make sure the next instruction isn't one.
         */
        if (!checkMoveException(meth, insnIdx+insnWidth, "next"))
            goto bail;

        /*
         * We want to update the registers and set the "changed" flag on the
         * next instruction (if necessary).  We may not be storing register
         * changes for all addresses, so for non-branch targets we just
         * compare "entry" vs. "work" to see if we've changed anything.
         */
        if (getRegisterLine(regTable, insnIdx+insnWidth) != NULL) {
            updateRegisters(meth, insnFlags, regTable, insnIdx+insnWidth,
                workRegs);
        } else {
            /* if not yet visited, or regs were updated, set "changed" */
            if (!dvmInsnIsVisited(insnFlags, insnIdx+insnWidth) ||
                compareRegisters(workRegs, entryRegs,
                    insnRegCount + kExtraRegs) != 0)
            {
                dvmInsnSetChanged(insnFlags, insnIdx+insnWidth, true);
            }
        }
    }

    /*
     * Handle "branch".  Tag the branch target.
     *
     * NOTE: instructions like OP_EQZ provide information about the state
     * of the register when the branch is taken or not taken.  For example,
     * somebody could get a reference field, check it for zero, and if the
     * branch is taken immediately store that register in a boolean field
     * since the value is known to be zero.  We do not currently account for
     * that, and will reject the code.
     */
    if ((nextFlags & kInstrCanBranch) != 0) {
        bool isConditional;

        if (!dvmGetBranchTarget(meth, insnFlags, insnIdx, &branchTarget,
                &isConditional))
        {
            /* should never happen after static verification */
            LOG_VFY_METH(meth, "VFY: bad branch at %d\n", insnIdx);
            goto bail;
        }
        assert(isConditional || (nextFlags & kInstrCanContinue) == 0);
        assert(!isConditional || (nextFlags & kInstrCanContinue) != 0);

        if (!checkMoveException(meth, insnIdx+branchTarget, "branch"))
            goto bail;

        updateRegisters(meth, insnFlags, regTable, insnIdx+branchTarget,
            workRegs);
    }

    /*
     * Handle "switch".  Tag all possible branch targets.
     *
     * We've already verified that the table is structurally sound, so we
     * just need to walk through and tag the targets.
     */
    if ((nextFlags & kInstrCanSwitch) != 0) {
        int offsetToSwitch = insns[1] | (((s4)insns[2]) << 16);
        const u2* switchInsns = insns + offsetToSwitch;
        int switchCount = switchInsns[1];
        int offsetToTargets, targ;

        if ((*insns & 0xff) == OP_PACKED_SWITCH) {
            /* 0=sig, 1=count, 2/3=firstKey */
            offsetToTargets = 4;
        } else {
            /* 0=sig, 1=count, 2..count*2 = keys */
            assert((*insns & 0xff) == OP_SPARSE_SWITCH);
            offsetToTargets = 2 + 2*switchCount;
        }

        /* verify each switch target */
        for (targ = 0; targ < switchCount; targ++) {
            int offset, absOffset;

            /* offsets are 32-bit, and only partly endian-swapped */
            offset = switchInsns[offsetToTargets + targ*2] |
                     (((s4) switchInsns[offsetToTargets + targ*2 +1]) << 16);
            absOffset = insnIdx + offset;

            assert(absOffset >= 0 && absOffset < insnsSize);

            if (!checkMoveException(meth, absOffset, "switch"))
                goto bail;

            updateRegisters(meth, insnFlags, regTable, absOffset, workRegs);
        }
    }

    /*
     * Handle instructions that can throw and that are sitting in a
     * "try" block.  (If they're not in a "try" block when they throw,
     * control transfers out of the method.)
     */
    if ((nextFlags & kInstrCanThrow) != 0 && dvmInsnIsInTry(insnFlags, insnIdx))
    {
        DexFile* pDexFile = meth->clazz->pDvmDex->pDexFile;
        const DexCode* pCode = dvmGetMethodCode(meth);
        DexCatchIterator iterator;

        if (dexFindCatchHandler(&iterator, pCode, insnIdx)) {
            for (;;) {
                DexCatchHandler* handler = dexCatchIteratorNext(&iterator);

                if (handler == NULL) {
                    break;
                }

                /* note we use entryRegs, not workRegs */
                updateRegisters(meth, insnFlags, regTable, handler->address,
                    entryRegs);
            }
        }
    }

    /*
     * Update startGuess.  Advance to the next instruction of that's
     * possible, otherwise use the branch target if one was found.  If
     * neither of those exists we're in a return or throw; leave startGuess
     * alone and let the caller sort it out.
     */
    if ((nextFlags & kInstrCanContinue) != 0) {
        *pStartGuess = insnIdx + dvmInsnGetWidth(insnFlags, insnIdx);
    } else if ((nextFlags & kInstrCanBranch) != 0) {
        /* we're still okay if branchTarget is zero */
        *pStartGuess = insnIdx + branchTarget;
    }

    assert(*pStartGuess >= 0 && *pStartGuess < insnsSize &&
        dvmInsnGetWidth(insnFlags, *pStartGuess) != 0);

    result = true;

bail:
    return result;
}

/*
 * callback function used in dumpRegTypes to print local vars
 * valid at a given address.
 */
static void logLocalsCb(void *cnxt, u2 reg, u4 startAddress, u4 endAddress,
        const char *name, const char *descriptor,
        const char *signature)
{
    int addr = *((int *)cnxt);

    if (addr >= (int) startAddress && addr < (int) endAddress)
    {
        LOGI("        %2d: '%s' %s\n", reg, name, descriptor);
    }
}

/*
 * Dump the register types for the specifed address to the log file.
 */
static void dumpRegTypes(const Method* meth, const InsnFlags* insnFlags,
    const RegType* addrRegs, int addr, const char* addrName,
    const UninitInstanceMap* uninitMap, int displayFlags)
{
    int regCount = meth->registersSize;
    int fullRegCount = regCount + kExtraRegs;
    bool branchTarget = dvmInsnIsBranchTarget(insnFlags, addr);
    int i;

    assert(addr >= 0 && addr < (int) dvmGetMethodInsnsSize(meth));

    int regCharSize = fullRegCount + (fullRegCount-1)/4 + 2 +1;
    char regChars[regCharSize +1];
    memset(regChars, ' ', regCharSize);
    regChars[0] = '[';
    if (regCount == 0)
        regChars[1] = ']';
    else
        regChars[1 + (regCount-1) + (regCount-1)/4 +1] = ']';
    regChars[regCharSize] = '\0';

    //const RegType* addrRegs = getRegisterLine(regTable, addr);

    for (i = 0; i < regCount + kExtraRegs; i++) {
        char tch;

        switch (addrRegs[i]) {
        case kRegTypeUnknown:       tch = '.';  break;
        case kRegTypeConflict:      tch = 'X';  break;
        case kRegTypeFloat:         tch = 'F';  break;
        case kRegTypeZero:          tch = '0';  break;
        case kRegTypeOne:           tch = '1';  break;
        case kRegTypeBoolean:       tch = 'Z';  break;
        case kRegTypePosByte:       tch = 'b';  break;
        case kRegTypeByte:          tch = 'B';  break;
        case kRegTypePosShort:      tch = 's';  break;
        case kRegTypeShort:         tch = 'S';  break;
        case kRegTypeChar:          tch = 'C';  break;
        case kRegTypeInteger:       tch = 'I';  break;
        case kRegTypeLongLo:        tch = 'J';  break;
        case kRegTypeLongHi:        tch = 'j';  break;
        case kRegTypeDoubleLo:      tch = 'D';  break;
        case kRegTypeDoubleHi:      tch = 'd';  break;
        default:
            if (regTypeIsReference(addrRegs[i])) {
                if (regTypeIsUninitReference(addrRegs[i]))
                    tch = 'U';
                else
                    tch = 'L';
            } else {
                tch = '*';
                assert(false);
            }
            break;
        }

        if (i < regCount)
            regChars[1 + i + (i/4)] = tch;
        else
            regChars[1 + i + (i/4) + 2] = tch;
    }

    if (addr == 0 && addrName != NULL)
        LOGI("%c%s %s\n", branchTarget ? '>' : ' ', addrName, regChars);
    else
        LOGI("%c0x%04x %s\n", branchTarget ? '>' : ' ', addr, regChars);

    if (displayFlags & DRT_SHOW_REF_TYPES) {
        for (i = 0; i < regCount + kExtraRegs; i++) {
            if (regTypeIsReference(addrRegs[i]) && addrRegs[i] != kRegTypeZero)
            {
                ClassObject* clazz;

                clazz = regTypeReferenceToClass(addrRegs[i], uninitMap);
                assert(dvmValidateObject((Object*)clazz));
                if (i < regCount) {
                    LOGI("        %2d: 0x%08x %s%s\n",
                        i, addrRegs[i],
                        regTypeIsUninitReference(addrRegs[i]) ? "[U]" : "",
                        clazz->descriptor);
                } else {
                    LOGI("        RS: 0x%08x %s%s\n",
                        addrRegs[i],
                        regTypeIsUninitReference(addrRegs[i]) ? "[U]" : "",
                        clazz->descriptor);
                }
            }
        }
    }
    if (displayFlags & DRT_SHOW_LOCALS) {
        dexDecodeDebugInfo(meth->clazz->pDvmDex->pDexFile,
                dvmGetMethodCode(meth),
                meth->clazz->descriptor,
                meth->prototype.protoIdx,
                meth->accessFlags,
                NULL, logLocalsCb, &addr);
    }
}

