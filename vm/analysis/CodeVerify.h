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
 * Dalvik bytecode verifier.
 */
#ifndef _DALVIK_CODEVERIFY
#define _DALVIK_CODEVERIFY

#include "analysis/VerifySubs.h"


/*
 * Enumeration for register type values.  The "hi" piece of a 64-bit value
 * MUST immediately follow the "lo" piece in the enumeration, so we can check
 * that hi==lo+1.
 *
 * Assignment of constants:
 *   [-MAXINT,-32768)   : integer
 *   [-32768,-128)      : short
 *   [-128,0)           : byte
 *   0                  : zero
 *   1                  : one
 *   [2,128)            : posbyte
 *   [128,32768)        : posshort
 *   [32768,65536)      : char
 *   [65536,MAXINT]     : integer
 *
 * Allowed "implicit" widening conversions:
 *   zero -> boolean, posbyte, byte, posshort, short, char, integer, ref (null)
 *   one -> boolean, posbyte, byte, posshort, short, char, integer
 *   boolean -> posbyte, byte, posshort, short, char, integer
 *   posbyte -> posshort, short, integer, char
 *   byte -> short, integer
 *   posshort -> integer, char
 *   short -> integer
 *   char -> integer
 *
 * In addition, all of the above can convert to "float".
 *
 * We're more careful with integer values than the spec requires.  The
 * motivation is to restrict byte/char/short to the correct range of values.
 * For example, if a method takes a byte argument, we don't want to allow
 * the code to load the constant "1024" and pass it in.
 */
enum {
    kRegTypeUnknown = 0,    /* initial state; use value=0 so calloc works */
    kRegTypeUninit = 1,     /* MUST be odd to distinguish from pointer */
    kRegTypeConflict,       /* merge clash makes this reg's type unknowable */

    /*
     * Category-1nr types.  The order of these is chiseled into a couple
     * of tables, so don't add, remove, or reorder if you can avoid it.
     */
#define kRegType1nrSTART    kRegTypeFloat
    kRegTypeFloat,
    kRegTypeZero,           /* 32-bit 0, could be Boolean, Int, Float, or Ref */
    kRegTypeOne,            /* 32-bit 1, could be Boolean, Int, Float */
    kRegTypeBoolean,        /* must be 0 or 1 */
    kRegTypePosByte,        /* byte, known positive (can become char) */
    kRegTypeByte,
    kRegTypePosShort,       /* short, known positive (can become char) */
    kRegTypeShort,
    kRegTypeChar,
    kRegTypeInteger,
#define kRegType1nrEND      kRegTypeInteger

    kRegTypeLongLo,         /* lower-numbered register; endian-independent */
    kRegTypeLongHi,
    kRegTypeDoubleLo,
    kRegTypeDoubleHi,

    /*
     * Enumeration max; this is used with "full" (32-bit) RegType values.
     *
     * Anything larger than this is a ClassObject or uninit ref.  Mask off
     * all but the low 8 bits; if you're left with kRegTypeUninit, pull
     * the uninit index out of the high 24.  Because kRegTypeUninit has an
     * odd value, there is no risk of a particular ClassObject pointer bit
     * pattern being confused for it (assuming our class object allocator
     * uses word alignment).
     */
    kRegTypeMAX
};
#define kRegTypeUninitMask  0xff
#define kRegTypeUninitShift 8

/*
 * RegType holds information about the type of data held in a register.
 * For most types it's a simple enum.  For reference types it holds a
 * pointer to the ClassObject, and for uninitialized references it holds
 * an index into the UninitInstanceMap.
 */
typedef u4 RegType;

/* table with merge logic for primitive types */
extern const char gDvmMergeTab[kRegTypeMAX][kRegTypeMAX];


/*
 * Returns "true" if the flags indicate that this address holds the start
 * of an instruction.
 */
INLINE bool dvmInsnIsOpcode(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagWidthMask) != 0;
}

/*
 * Extract the unsigned 16-bit instruction width from "flags".
 */
INLINE int dvmInsnGetWidth(const InsnFlags* insnFlags, int addr) {
    return insnFlags[addr] & kInsnFlagWidthMask;
}

/*
 * Changed?
 */
INLINE bool dvmInsnIsChanged(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagChanged) != 0;
}
INLINE void dvmInsnSetChanged(InsnFlags* insnFlags, int addr, bool changed)
{
    if (changed)
        insnFlags[addr] |= kInsnFlagChanged;
    else
        insnFlags[addr] &= ~kInsnFlagChanged;
}

/*
 * Visited?
 */
INLINE bool dvmInsnIsVisited(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagVisited) != 0;
}
INLINE void dvmInsnSetVisited(InsnFlags* insnFlags, int addr, bool changed)
{
    if (changed)
        insnFlags[addr] |= kInsnFlagVisited;
    else
        insnFlags[addr] &= ~kInsnFlagVisited;
}

/*
 * Visited or changed?
 */
INLINE bool dvmInsnIsVisitedOrChanged(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & (kInsnFlagVisited|kInsnFlagChanged)) != 0;
}

/*
 * In a "try" block?
 */
INLINE bool dvmInsnIsInTry(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagInTry) != 0;
}
INLINE void dvmInsnSetInTry(InsnFlags* insnFlags, int addr, bool inTry)
{
    assert(inTry);
    //if (inTry)
        insnFlags[addr] |= kInsnFlagInTry;
    //else
    //    insnFlags[addr] &= ~kInsnFlagInTry;
}

/*
 * Instruction is a branch target or exception handler?
 */
INLINE bool dvmInsnIsBranchTarget(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagBranchTarget) != 0;
}
INLINE void dvmInsnSetBranchTarget(InsnFlags* insnFlags, int addr,
    bool isBranch)
{
    assert(isBranch);
    //if (isBranch)
        insnFlags[addr] |= kInsnFlagBranchTarget;
    //else
    //    insnFlags[addr] &= ~kInsnFlagBranchTarget;
}

/*
 * Instruction is a GC point?
 */
INLINE bool dvmInsnIsGcPoint(const InsnFlags* insnFlags, int addr) {
    return (insnFlags[addr] & kInsnFlagGcPoint) != 0;
}
INLINE void dvmInsnSetGcPoint(InsnFlags* insnFlags, int addr,
    bool isBranch)
{
    assert(isBranch);
    //if (isBranch)
        insnFlags[addr] |= kInsnFlagGcPoint;
    //else
    //    insnFlags[addr] &= ~kInsnFlagGcPoint;
}


/*
 * Table that maps uninitialized instances to classes, based on the
 * address of the new-instance instruction.
 */
typedef struct UninitInstanceMap {
    int numEntries;
    struct {
        int             addr;   /* code offset, or -1 for method arg ("this") */
        ClassObject*    clazz;  /* class created at this address */
    } map[1];
} UninitInstanceMap;
#define kUninitThisArgAddr  (-1)
#define kUninitThisArgSlot  0

/*
 * Create a new UninitInstanceMap.
 */
UninitInstanceMap* dvmCreateUninitInstanceMap(const Method* meth,
    const InsnFlags* insnFlags, int newInstanceCount);

/*
 * Release the storage associated with an UninitInstanceMap.
 */
void dvmFreeUninitInstanceMap(UninitInstanceMap* uninitMap);

/*
 * Associate a class with an address.  Returns the map slot index, or -1
 * if the address isn't listed in the map (shouldn't happen) or if a
 * different class is already associated with the address (shouldn't
 * happen either).
 */
//int dvmSetUninitInstance(UninitInstanceMap* uninitMap, int addr, 
//    ClassObject* clazz);

/*
 * Return the class associated with an uninitialized reference.  Pass in
 * the map index.
 */
//ClassObject* dvmGetUninitInstance(const UninitInstanceMap* uninitMap, int idx);

/*
 * Clear the class associated with an uninitialized reference.  Pass in
 * the map index.
 */
//void dvmClearUninitInstance(UninitInstanceMap* uninitMap, int idx);


/*
 * Verify bytecode in "meth".  "insnFlags" should be populated with
 * instruction widths and "in try" flags.
 */
bool dvmVerifyCodeFlow(const Method* meth, InsnFlags* insnFlags,
    UninitInstanceMap* uninitMap);

#endif /*_DALVIK_CODEVERIFY*/
