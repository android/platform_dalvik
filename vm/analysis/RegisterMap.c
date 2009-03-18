/*
 * Copyright (C) 2009 The Android Open Source Project
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

// ** UNDER CONSTRUCTION **

/*
 * This code generate "register maps" for Dalvik bytecode.  In a stack-based
 * VM we might call these "stack maps".  They are used to increase the
 * precision in the garbage collector when scanning references in the
 * interpreter thread stacks.
 */
#include "Dalvik.h"
#include "analysis/CodeVerify.h"
#include "analysis/RegisterMap.h"
#include "libdex/DexCatch.h"
#include "libdex/InstrUtils.h"

#include <stddef.h>


/*
Notes on just-in-time RegisterMap generation

Generating RegisterMap tables as part of verification is convenient because
we generate most of what we need to know as part of doing the verify.
The negative aspect of doing it this way is that we must store the
result in the DEX file (if we're verifying ahead of time) or in memory
(if verifying during class load) for every concrete non-native method,
even if we never actually need the map during a GC.

A simple but compact encoding of register map data increases the size of
optimized DEX files by about 25%, so size considerations are important.

We can instead generate the RegisterMap at the point where it is needed.
In a typical application we only need to convert about 2% of the loaded
methods, and we can generate type-precise roots reasonably quickly because
(a) we know the method has already been verified and hence can make a
lot of assumptions, and (b) we don't care what type of object a register
holds, just whether or not it holds a reference, and hence can skip a
lot of class resolution gymnastics.

There are a couple of problems with this approach however.  First, to
get good performance we really want an implementation that is largely
independent from the verifier, which means some duplication of effort.
Second, we're dealing with post-dexopt code, which contains "quickened"
instructions.  We can't process those without either tracking type
information (which slows us down) or storing additional data in the DEX
file that allows us to reconstruct the original instructions (adds ~5%
to the size of the ODEX).


Implementation notes...

Both type-precise and live-precise information can be generated knowing
only whether or not a register holds a reference.  We don't need to
know what kind of reference or whether the object has been initialized.
Not only can we skip many of the fancy steps in the verifier, we can
initialize from simpler sources, e.g. the initial registers and return
type are set from the "shorty" signature rather than the full signature.

The short-term storage needs for just-in-time register map generation can
be much lower because we can use a 1-byte SRegType instead of a 4-byte
RegType.  On the other hand, if we're not doing type-precise analysis
in the verifier we only need to store register contents at every branch
target, rather than every GC point (which are much more frequent).

Whether it happens in the verifier or independently, because this is done
with native heap allocations that may be difficult to return to the system,
an effort should be made to minimize memory use.
*/

// fwd
static void outputTypeVector(const RegType* regs, int insnRegCount, u1* data);
static bool verifyMap(VerifierData* vdata, const RegisterMap* pMap);

/*
 * Generate the register map for a method that has just been verified
 * (i.e. we're doing this as part of verification).
 *
 * For type-precise determination we have all the data we need, so we
 * just need to encode it in some clever fashion.
 *
 * Returns a pointer to a newly-allocated RegisterMap, or NULL on failure.
 */
RegisterMap* dvmGenerateRegisterMapV(VerifierData* vdata)
{
    RegisterMap* pMap = NULL;
    RegisterMap* pResult = NULL;
    RegisterMapFormat format;
    u1 regWidth;
    u1* mapData;
    int i, bytesForAddr, gcPointCount;
    int bufSize;

    regWidth = (vdata->method->registersSize + 7) / 8;
    if (vdata->insnsSize < 256) {
        format = kFormatCompact8;
        bytesForAddr = 1;
    } else {
        format = kFormatCompact16;
        bytesForAddr = 2;
    }

    /*
     * Count up the number of GC point instructions.
     *
     * NOTE: this does not automatically include the first instruction,
     * since we don't count method entry as a GC point.
     */
    gcPointCount = 0;
    for (i = 0; i < vdata->insnsSize; i++) {
        if (dvmInsnIsGcPoint(vdata->insnFlags, i))
            gcPointCount++;
    }
    if (gcPointCount >= 65536) {
        /* we could handle this, but in practice we don't get near this */
        LOGE("ERROR: register map can't handle %d gc points in one method\n",
            gcPointCount);
        goto bail;
    }

    /*
     * Allocate a buffer to hold the map data.
     */
    bufSize = offsetof(RegisterMap, data);
    bufSize += gcPointCount * (bytesForAddr + regWidth);

    LOGD("+++ grm: %s.%s (adr=%d gpc=%d rwd=%d bsz=%d)\n",
        vdata->method->clazz->descriptor, vdata->method->name,
        bytesForAddr, gcPointCount, regWidth, bufSize);

    pMap = (RegisterMap*) malloc(bufSize);
    pMap->format = format;
    pMap->regWidth = regWidth;
    pMap->numEntries = gcPointCount;

    /*
     * Populate it.
     */
    mapData = pMap->data;
    for (i = 0; i < vdata->insnsSize; i++) {
        if (dvmInsnIsGcPoint(vdata->insnFlags, i)) {
            assert(vdata->addrRegs[i] != NULL);
            if (format == kFormatCompact8) {
                *mapData++ = i;
            } else /*kFormatCompact16*/ {
                *mapData++ = i & 0xff;
                *mapData++ = i >> 8;
            }
            outputTypeVector(vdata->addrRegs[i], vdata->insnRegCount, mapData);
            mapData += regWidth;
        }
    }

    LOGI("mapData=%p pMap=%p bufSize=%d\n", mapData, pMap, bufSize);
    assert(mapData - (const u1*) pMap == bufSize);

#if 1
    if (!verifyMap(vdata, pMap))
        goto bail;
#endif

    pResult = pMap;

bail:
    return pResult;
}

/*
 * Release the storage held by a RegisterMap.
 */
void dvmFreeRegisterMap(RegisterMap* pMap)
{
    if (pMap == NULL)
        return;

    free(pMap);
}

/*
 * Determine if the RegType value is a reference type.
 *
 * Ordinarily we include kRegTypeZero in the "is it a reference"
 * check.  There's no value in doing so here, because we know
 * the register can't hold anything but zero.
 */
static inline bool isReferenceType(RegType type)
{
    return (type > kRegTypeMAX || type == kRegTypeUninit);
}

/*
 * Given a line of registers, output a bit vector that indicates whether
 * or not the register holds a reference type (which could be null).
 *
 * We use '1' to indicate it's a reference, '0' for anything else (numeric
 * value, uninitialized data, merge conflict).  Register 0 will be found
 * in the low bit of the first byte.
 */
static void outputTypeVector(const RegType* regs, int insnRegCount, u1* data)
{
    u1 val = 0;
    int i;

    for (i = 0; i < insnRegCount; i++) {
        RegType type = *regs++;
        val >>= 1;
        if (isReferenceType(type))
            val |= 0x80;        /* set hi bit */

        if ((i & 0x07) == 7)
            *data++ = val;
    }
    if ((i & 0x07) != 0) {
        /* flush bits from last byte */
        val >>= 8 - (i & 0x07);
        *data++ = val;
    }
}

/*
 * Double-check the map.
 *
 * We run through all of the data in the map, and compare it to the original.
 */
static bool verifyMap(VerifierData* vdata, const RegisterMap* pMap)
{
    const u1* data = pMap->data;
    int ent;

    for (ent = 0; ent < pMap->numEntries; ent++) {
        int addr;

        switch (pMap->format) {
        case kFormatCompact8:
            addr = *data++;
            break;
        case kFormatCompact16:
            addr = *data++;
            addr |= (*data++) << 8;
            break;
        default:
            /* shouldn't happen */
            LOGE("GLITCH: bad format (%d)", pMap->format);
            dvmAbort();
        }

        const RegType* regs = vdata->addrRegs[addr];
        if (regs == NULL) {
            LOGE("GLITCH: addr %d has no data\n", addr);
            return false;
        }

        u1 val;
        int i;

        for (i = 0; i < vdata->method->registersSize; i++) {
            bool bitIsRef, regIsRef;

            val >>= 1;
            if ((i & 0x07) == 0) {
                /* load next byte of data */
                val = *data++;
            }

            bitIsRef = val & 0x01;

            RegType type = regs[i];
            regIsRef = isReferenceType(type);

            if (bitIsRef != regIsRef) {
                LOGE("GLITCH: addr %d reg %d: bit=%d reg=%d(%d)\n",
                    addr, i, bitIsRef, regIsRef, type);
                return false;
            }
        }

        /* print the map as a binary string */
        if (false) {
            char outBuf[vdata->method->registersSize +1];
            for (i = 0; i < vdata->method->registersSize; i++) {
                if (isReferenceType(regs[i])) {
                    outBuf[i] = '1';
                } else {
                    outBuf[i] = '0';
                }
            }
            outBuf[i] = '\0';
            LOGD("  %04d %s\n", addr, outBuf);
        }
    }

    return true;
}


/*
 * ===========================================================================
 *      Just-in-time generation
 * ===========================================================================
 */

#if 0   /* incomplete implementation; may be removed entirely in the future */

/*
 * This is like RegType in the verifier, but simplified.  It holds a value
 * from the reg type enum, or kRegTypeReference.
 */
typedef u1 SRegType;
#define kRegTypeReference kRegTypeMAX

/*
 * We need an extra "pseudo register" to hold the return type briefly.  It
 * can be category 1 or 2, so we need two slots.
 */
#define kExtraRegs  2
#define RESULT_REGISTER(_insnRegCountPlus)  (_insnRegCountPlus - kExtraRegs)

/*
 * Working state.
 */
typedef struct WorkState {
    /*
     * The method we're working on.
     */
    const Method* method;

    /*
     * Number of instructions in the method.
     */
    int         insnsSize;

    /*
     * Number of registers we track for each instruction.  This is equal
     * to the method's declared "registersSize" plus kExtraRegs.
     */
    int         insnRegCountPlus;

    /*
     * Instruction widths and flags, one entry per code unit.
     */
    InsnFlags*  insnFlags;

    /*
     * Array of SRegType arrays, one entry per code unit.  We only need
     * to create an entry when an instruction starts at this address.
     * We can further reduce this to instructions that are GC points.
     *
     * We could just go ahead and allocate one per code unit, but for
     * larger methods that can represent a significant bit of short-term
     * storage.
     */
    SRegType**  addrRegs;

    /*
     * A single large alloc, with all of the storage needed for addrRegs.
     */
    SRegType*   regAlloc;
} WorkState;

// fwd
static bool generateMap(WorkState* pState, RegisterMap* pMap);
static bool analyzeMethod(WorkState* pState);
static bool handleInstruction(WorkState* pState, SRegType* workRegs,\
    int insnIdx, int* pStartGuess);
static void updateRegisters(WorkState* pState, int nextInsn,\
    const SRegType* workRegs);


/*
 * Set instruction flags.
 */
static bool setInsnFlags(WorkState* pState, int* pGcPointCount)
{
    const Method* meth = pState->method;
    InsnFlags* insnFlags = pState->insnFlags;
    int insnsSize = pState->insnsSize;
    const u2* insns = meth->insns;
    int gcPointCount = 0;
    int offset;

    /* set the widths */
    if (!dvmComputeCodeWidths(meth, pState->insnFlags, NULL))
        return false;

    /* mark "try" regions and exception handler branch targets */
    if (!dvmSetTryFlags(meth, pState->insnFlags))
        return false;

    /* the start of the method is a "branch target" */
    dvmInsnSetBranchTarget(insnFlags, 0, true);

    /*
     * Run through the instructions, looking for switches and branches.
     * Mark their targets.
     *
     * We don't really need to "check" these instructions -- the verifier
     * already did that -- but the additional overhead isn't significant
     * enough to warrant making a second copy of the "Check" function.
     *
     * Mark and count GC points while we're at it.
     */
    for (offset = 0; offset < insnsSize; offset++) {
        static int gcMask = kInstrCanBranch | kInstrCanSwitch |
            kInstrCanThrow | kInstrCanReturn;
        u1 opcode = insns[offset] & 0xff;
        InstructionFlags opFlags = dexGetInstrFlags(gDvm.instrFlags, opcode);

        if (opFlags & kInstrCanBranch) {
            if (!dvmCheckBranchTarget(meth, insnFlags, offset, true))
                return false;
        }
        if (opFlags & kInstrCanSwitch) {
            if (!dvmCheckSwitchTargets(meth, insnFlags, offset))
                return false;
        }

        if ((opFlags & gcMask) != 0) {
            dvmInsnSetGcPoint(pState->insnFlags, offset, true);
            gcPointCount++;
        }
    }

    *pGcPointCount = gcPointCount;
    return true;
}

/*
 * Generate the register map for a method.
 *
 * Returns a pointer to newly-allocated storage.
 */
RegisterMap* dvmGenerateRegisterMap(const Method* meth)
{
    WorkState* pState = NULL;
    RegisterMap* pMap = NULL;
    RegisterMap* result = NULL;
    SRegType* regPtr;

    pState = (WorkState*) calloc(1, sizeof(WorkState));
    if (pState == NULL)
        goto bail;

    pMap = (RegisterMap*) calloc(1, sizeof(RegisterMap));
    if (pMap == NULL)
        goto bail;

    pState->method = meth;
    pState->insnsSize = dvmGetMethodInsnsSize(meth);
    pState->insnRegCountPlus = meth->registersSize + kExtraRegs;

    pState->insnFlags = calloc(sizeof(InsnFlags), pState->insnsSize);
    pState->addrRegs = calloc(sizeof(SRegType*), pState->insnsSize);

    /*
     * Set flags on instructions, and calculate the number of code units
     * that happen to be GC points.
     */
    int gcPointCount;
    if (!setInsnFlags(pState, &gcPointCount))
        goto bail;

    if (gcPointCount == 0) {
        /* the method doesn't allocate or call, and never returns? unlikely */
        LOG_VFY_METH(meth, "Found do-nothing method\n");
        goto bail;
    }

    pState->regAlloc = (SRegType*)
        calloc(sizeof(SRegType), pState->insnsSize * gcPointCount);
    regPtr = pState->regAlloc;

    /*
     * For each instruction that is a GC point, set a pointer into the
     * regAlloc buffer.
     */
    int offset;
    for (offset = 0; offset < pState->insnsSize; offset++) {
        if (dvmInsnIsGcPoint(pState->insnFlags, offset)) {
            pState->addrRegs[offset] = regPtr;
            regPtr += pState->insnRegCountPlus;
        }
    }
    assert(regPtr - pState->regAlloc == pState->insnsSize * gcPointCount);
    assert(pState->addrRegs[0] != NULL);

    /*
     * Compute the register map.
     */
    if (!generateMap(pState, pMap))
        goto bail;

    /* success */
    result = pMap;
    pMap = NULL;

bail:
    if (pState != NULL) {
        free(pState->insnFlags);
        free(pState->addrRegs);
        free(pState->regAlloc);
        free(pState);
    }
    if (pMap != NULL)
        dvmFreeRegisterMap(pMap);
    return result;
}

/*
 * Release the storage associated with a RegisterMap.
 */
void dvmFreeRegisterMap(RegisterMap* pMap)
{
    if (pMap == NULL)
        return;
}


/*
 * Create the RegisterMap using the provided state.
 */
static bool generateMap(WorkState* pState, RegisterMap* pMap)
{
    bool result = false;

    /*
     * Analyze the method and store the results in WorkState.
     */
    if (!analyzeMethod(pState))
        goto bail;

    /*
     * Convert the analyzed data into a RegisterMap.
     */
    // TODO

    result = true;

bail:
    return result;
}

/*
 * Set the register types for the method arguments.  We can pull the values
 * out of the "shorty" signature.
 */
static bool setTypesFromSignature(WorkState* pState)
{
    const Method* meth = pState->method;
    int argReg = meth->registersSize - meth->insSize;   /* first arg */
    SRegType* pRegs = pState->addrRegs[0];
    SRegType* pCurReg = &pRegs[argReg];
    const char* ccp;

    /*
     * Include "this" pointer, if appropriate.
     */
    if (!dvmIsStaticMethod(meth)) {
        *pCurReg++ = kRegTypeReference;
    }

    ccp = meth->shorty +1;      /* skip first byte, which holds return type */
    while (*ccp != 0) {
        switch (*ccp) {
        case 'L':
        //case '[':
            *pCurReg++ = kRegTypeReference;
            break;
        case 'Z':
            *pCurReg++ = kRegTypeBoolean;
            break;
        case 'C':
            *pCurReg++ = kRegTypeChar;
            break;
        case 'B':
            *pCurReg++ = kRegTypeByte;
            break;
        case 'I':
            *pCurReg++ = kRegTypeInteger;
            break;
        case 'S':
            *pCurReg++ = kRegTypeShort;
            break;
        case 'F':
            *pCurReg++ = kRegTypeFloat;
            break;
        case 'D':
            *pCurReg++ = kRegTypeDoubleLo;
            *pCurReg++ = kRegTypeDoubleHi;
            break;
        case 'J':
            *pCurReg++ = kRegTypeLongLo;
            *pCurReg++ = kRegTypeLongHi;
            break;
        default:
            assert(false);
            return false;
        }
    }

    assert(pCurReg - pRegs == meth->insSize);
    return true;
}

/*
 * Find the start of the register set for the specified instruction in
 * the current method.
 */
static inline SRegType* getRegisterLine(const WorkState* pState, int insnIdx)
{
    return pState->addrRegs[insnIdx];
}

/*
 * Copy a set of registers.
 */
static inline void copyRegisters(SRegType* dst, const SRegType* src,
    int numRegs)
{
    memcpy(dst, src, numRegs * sizeof(SRegType));
}

/*
 * Compare a set of registers.  Returns 0 if they match.
 */
static inline int compareRegisters(const SRegType* src1, const SRegType* src2,
    int numRegs)
{
    return memcmp(src1, src2, numRegs * sizeof(SRegType));
}

/*
 * Run through the instructions repeatedly until we have exercised all
 * possible paths.
 */
static bool analyzeMethod(WorkState* pState)
{
    const Method* meth = pState->method;
    SRegType workRegs[pState->insnRegCountPlus];
    InsnFlags* insnFlags = pState->insnFlags;
    int insnsSize = pState->insnsSize;
    int insnIdx, startGuess;
    bool result = false;

    /*
     * Initialize the types of the registers that correspond to method
     * arguments.
     */
    if (!setTypesFromSignature(pState))
        goto bail;

    /*
     * Mark the first instruction as "changed".
     */
    dvmInsnSetChanged(insnFlags, 0, true);
    startGuess = 0;

    if (true) {
        IF_LOGI() {
            char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
            LOGI("Now mapping: %s.%s %s (ins=%d regs=%d)\n",
                meth->clazz->descriptor, meth->name, desc,
                meth->insSize, meth->registersSize);
            LOGI(" ------ [0    4    8    12   16   20   24   28   32   36\n");
            free(desc);
        }
    }

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
            SRegType* insnRegs = getRegisterLine(pState, insnIdx);
            assert(insnRegs != NULL);
            copyRegisters(workRegs, insnRegs, pState->insnRegCountPlus);

        } else {
#ifndef NDEBUG
            /*
             * Sanity check: retrieve the stored register line (assuming
             * a full table) and make sure it actually matches.
             */
            SRegType* insnRegs = getRegisterLine(pState, insnIdx);
            if (insnRegs != NULL &&
                compareRegisters(workRegs, insnRegs,
                                 pState->insnRegCountPlus) != 0)
            {
                char* desc = dexProtoCopyMethodDescriptor(&meth->prototype);
                LOG_VFY("HUH? workRegs diverged in %s.%s %s\n",
                        meth->clazz->descriptor, meth->name, desc);
                free(desc);
            }
#endif
        }

        /*
         * Update the register sets altered by this instruction.
         */
        if (!handleInstruction(pState, workRegs, insnIdx, &startGuess)) {
            goto bail;
        }

        dvmInsnSetVisited(insnFlags, insnIdx, true);
        dvmInsnSetChanged(insnFlags, insnIdx, false);
    }

    // TODO - add dead code scan to help validate this code?

    result = true;

bail:
    return result;
}

/*
 * Get a pointer to the method being invoked.
 *
 * Returns NULL on failure.
 */
static Method* getInvokedMethod(const Method* meth,
    const DecodedInstruction* pDecInsn, MethodType methodType)
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

        LOG_VFY("VFY: unable to resolve %s method %u: %s.%s %s\n",
            dvmMethodTypeStr(methodType), pDecInsn->vB,
            classDescriptor, methodName, methodDesc);
        free(methodDesc);
        return NULL;
    }

    return resMethod;
}

/*
 * Return the register type for the method.  Since we don't care about
 * the actual type, we can just look at the "shorty" signature.
 *
 * Returns kRegTypeUnknown for "void".
 */
static SRegType getMethodReturnType(const Method* meth)
{
    SRegType type;

    switch (meth->shorty[0]) {
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
    //case '[':
        type = kRegTypeReference;
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
 * Copy a category 1 register.
 */
static inline void copyRegister1(SRegType* insnRegs, u4 vdst, u4 vsrc)
{
    insnRegs[vdst] = insnRegs[vsrc];
}

/*
 * Copy a category 2 register.  Note the source and destination may overlap.
 */
static inline void copyRegister2(SRegType* insnRegs, u4 vdst, u4 vsrc)
{
    //memmove(&insnRegs[vdst], &insnRegs[vsrc], sizeof(SRegType) * 2);
    SRegType r1 = insnRegs[vsrc];
    SRegType r2 = insnRegs[vsrc+1];
    insnRegs[vdst] = r1;
    insnRegs[vdst+1] = r2;
}

/*
 * Set the type of a category 1 register.
 */
static inline void setRegisterType(SRegType* insnRegs, u4 vdst, SRegType type)
{
    insnRegs[vdst] = type;
}

/*
 * Decode the specified instruction and update the register info.
 */
static bool handleInstruction(WorkState* pState, SRegType* workRegs,
    int insnIdx, int* pStartGuess)
{
    const Method* meth = pState->method;
    const u2* insns = meth->insns + insnIdx;
    InsnFlags* insnFlags = pState->insnFlags;
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
    DecodedInstruction decInsn;
    SRegType entryRegs[pState->insnRegCountPlus];
    const int insnRegCountPlus = pState->insnRegCountPlus;
    bool justSetResult = false;
    int branchTarget = 0;
    SRegType tmpType;

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
        copyRegisters(entryRegs, workRegs, insnRegCountPlus);
    }

    switch (decInsn.opCode) {
    case OP_NOP:
        break;

    case OP_MOVE:
    case OP_MOVE_FROM16:
    case OP_MOVE_16:
    case OP_MOVE_OBJECT:
    case OP_MOVE_OBJECT_FROM16:
    case OP_MOVE_OBJECT_16:
        copyRegister1(workRegs, decInsn.vA, decInsn.vB);
        break;
    case OP_MOVE_WIDE:
    case OP_MOVE_WIDE_FROM16:
    case OP_MOVE_WIDE_16:
        copyRegister2(workRegs, decInsn.vA, decInsn.vB);
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
    case OP_MOVE_RESULT_OBJECT:
        copyRegister1(workRegs, decInsn.vA, RESULT_REGISTER(insnRegCountPlus));
        break;
    case OP_MOVE_RESULT_WIDE:
        copyRegister2(workRegs, decInsn.vA, RESULT_REGISTER(insnRegCountPlus));
        break;

    case OP_MOVE_EXCEPTION:
        /*
         * This statement can only appear as the first instruction in an
         * exception handler (though not all exception handlers need to
         * have one of these).  We verify that as part of extracting the
         * exception type from the catch block list.
         */
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;

    case OP_RETURN_VOID:
    case OP_RETURN:
    case OP_RETURN_WIDE:
    case OP_RETURN_OBJECT:
        break;

    case OP_CONST_4:
    case OP_CONST_16:
    case OP_CONST:
        /* could be boolean, int, float, or a null reference */
        setRegisterType(workRegs, decInsn.vA,
            dvmDetermineCat1Const((s4)decInsn.vB));
        break;
    case OP_CONST_HIGH16:
        /* could be boolean, int, float, or a null reference */
        setRegisterType(workRegs, decInsn.vA,
            dvmDetermineCat1Const((s4) decInsn.vB << 16));
        break;
    case OP_CONST_WIDE_16:
    case OP_CONST_WIDE_32:
    case OP_CONST_WIDE:
    case OP_CONST_WIDE_HIGH16:
        /* could be long or double; default to long and allow conversion */
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_CONST_STRING:
    case OP_CONST_STRING_JUMBO:
    case OP_CONST_CLASS:
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;

    case OP_MONITOR_ENTER:
    case OP_MONITOR_EXIT:
        break;

    case OP_CHECK_CAST:
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;
    case OP_INSTANCE_OF:
        /* result is boolean */
        setRegisterType(workRegs, decInsn.vA, kRegTypeBoolean);
        break;

    case OP_ARRAY_LENGTH:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;

    case OP_NEW_INSTANCE:
    case OP_NEW_ARRAY:
        /* add the new uninitialized reference to the register ste */
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;
    case OP_FILLED_NEW_ARRAY:
    case OP_FILLED_NEW_ARRAY_RANGE:
        setRegisterType(workRegs, RESULT_REGISTER(insnRegCountPlus),
            kRegTypeReference);
        justSetResult = true;
        break;

    case OP_CMPL_FLOAT:
    case OP_CMPG_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeBoolean);
        break;
    case OP_CMPL_DOUBLE:
    case OP_CMPG_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeBoolean);
        break;
    case OP_CMP_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeBoolean);
        break;

    case OP_THROW:
    case OP_GOTO:
    case OP_GOTO_16:
    case OP_GOTO_32:
    case OP_PACKED_SWITCH:
    case OP_SPARSE_SWITCH:
        break;

    case OP_FILL_ARRAY_DATA:
        break;

    case OP_IF_EQ:
    case OP_IF_NE:
    case OP_IF_LT:
    case OP_IF_GE:
    case OP_IF_GT:
    case OP_IF_LE:
    case OP_IF_EQZ:
    case OP_IF_NEZ:
    case OP_IF_LTZ:
    case OP_IF_GEZ:
    case OP_IF_GTZ:
    case OP_IF_LEZ:
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
        setRegisterType(workRegs, decInsn.vA, tmpType);
        break;

    case OP_AGET_WIDE:
        /*
         * We know this is either long or double, and we don't really
         * discriminate between those during verification, so we
         * call it a long.
         */
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;

    case OP_AGET_OBJECT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;

    case OP_APUT:
    case OP_APUT_BOOLEAN:
    case OP_APUT_BYTE:
    case OP_APUT_CHAR:
    case OP_APUT_SHORT:
    case OP_APUT_WIDE:
    case OP_APUT_OBJECT:
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
        setRegisterType(workRegs, decInsn.vA, tmpType);
        break;

    case OP_IGET_WIDE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;

    case OP_IGET_OBJECT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;

    case OP_IPUT:
    case OP_IPUT_BOOLEAN:
    case OP_IPUT_BYTE:
    case OP_IPUT_CHAR:
    case OP_IPUT_SHORT:
    case OP_IPUT_WIDE:
    case OP_IPUT_OBJECT:
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
        setRegisterType(workRegs, decInsn.vA, tmpType);
        break;

    case OP_SGET_WIDE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;

    case OP_SGET_OBJECT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeReference);
        break;

    case OP_SPUT:
    case OP_SPUT_BOOLEAN:
    case OP_SPUT_BYTE:
    case OP_SPUT_CHAR:
    case OP_SPUT_SHORT:
    case OP_SPUT_WIDE:
    case OP_SPUT_OBJECT:
        break;

    case OP_INVOKE_VIRTUAL:
    case OP_INVOKE_VIRTUAL_RANGE:
    case OP_INVOKE_SUPER:
    case OP_INVOKE_SUPER_RANGE:
        {
            Method* calledMethod;

            calledMethod = getInvokedMethod(meth, &decInsn, METHOD_VIRTUAL);
            if (calledMethod == NULL)
                goto bail;
            setRegisterType(workRegs, RESULT_REGISTER(insnRegCountPlus),
                getMethodReturnType(calledMethod));
            justSetResult = true;
        }
        break;
    case OP_INVOKE_DIRECT:
    case OP_INVOKE_DIRECT_RANGE:
        {
            Method* calledMethod;

            calledMethod = getInvokedMethod(meth, &decInsn, METHOD_DIRECT);
            if (calledMethod == NULL)
                goto bail;
            setRegisterType(workRegs, RESULT_REGISTER(insnRegCountPlus),
                getMethodReturnType(calledMethod));
            justSetResult = true;
        }
        break;
    case OP_INVOKE_STATIC:
    case OP_INVOKE_STATIC_RANGE:
        {
            Method* calledMethod;

            calledMethod = getInvokedMethod(meth, &decInsn, METHOD_STATIC);
            if (calledMethod == NULL)
                goto bail;
            setRegisterType(workRegs, RESULT_REGISTER(insnRegCountPlus),
                getMethodReturnType(calledMethod));
            justSetResult = true;
        }
        break;
    case OP_INVOKE_INTERFACE:
    case OP_INVOKE_INTERFACE_RANGE:
        {
            Method* absMethod;

            absMethod = getInvokedMethod(meth, &decInsn, METHOD_INTERFACE);
            if (absMethod == NULL)
                goto bail;
            setRegisterType(workRegs, RESULT_REGISTER(insnRegCountPlus),
                getMethodReturnType(absMethod));
            justSetResult = true;
        }
        break;

    case OP_NEG_INT:
    case OP_NOT_INT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_NEG_LONG:
    case OP_NOT_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_NEG_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_NEG_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_INT_TO_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_INT_TO_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_INT_TO_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_LONG_TO_INT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_LONG_TO_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_LONG_TO_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_FLOAT_TO_INT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_FLOAT_TO_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_FLOAT_TO_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_DOUBLE_TO_INT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_DOUBLE_TO_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_DOUBLE_TO_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_INT_TO_BYTE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeByte);
        break;
    case OP_INT_TO_CHAR:
        setRegisterType(workRegs, decInsn.vA, kRegTypeChar);
        break;
    case OP_INT_TO_SHORT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeShort);
        break;

    case OP_ADD_INT:
    case OP_SUB_INT:
    case OP_MUL_INT:
    case OP_REM_INT:
    case OP_DIV_INT:
    case OP_SHL_INT:
    case OP_SHR_INT:
    case OP_USHR_INT:
    case OP_AND_INT:
    case OP_OR_INT:
    case OP_XOR_INT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_ADD_LONG:
    case OP_SUB_LONG:
    case OP_MUL_LONG:
    case OP_DIV_LONG:
    case OP_REM_LONG:
    case OP_AND_LONG:
    case OP_OR_LONG:
    case OP_XOR_LONG:
    case OP_SHL_LONG:
    case OP_SHR_LONG:
    case OP_USHR_LONG:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_ADD_FLOAT:
    case OP_SUB_FLOAT:
    case OP_MUL_FLOAT:
    case OP_DIV_FLOAT:
    case OP_REM_FLOAT:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_ADD_DOUBLE:
    case OP_SUB_DOUBLE:
    case OP_MUL_DOUBLE:
    case OP_DIV_DOUBLE:
    case OP_REM_DOUBLE:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_ADD_INT_2ADDR:
    case OP_SUB_INT_2ADDR:
    case OP_MUL_INT_2ADDR:
    case OP_REM_INT_2ADDR:
    case OP_SHL_INT_2ADDR:
    case OP_SHR_INT_2ADDR:
    case OP_USHR_INT_2ADDR:
    case OP_AND_INT_2ADDR:
    case OP_OR_INT_2ADDR:
    case OP_XOR_INT_2ADDR:
    case OP_DIV_INT_2ADDR:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;
    case OP_ADD_LONG_2ADDR:
    case OP_SUB_LONG_2ADDR:
    case OP_MUL_LONG_2ADDR:
    case OP_DIV_LONG_2ADDR:
    case OP_REM_LONG_2ADDR:
    case OP_AND_LONG_2ADDR:
    case OP_OR_LONG_2ADDR:
    case OP_XOR_LONG_2ADDR:
    case OP_SHL_LONG_2ADDR:
    case OP_SHR_LONG_2ADDR:
    case OP_USHR_LONG_2ADDR:
        setRegisterType(workRegs, decInsn.vA, kRegTypeLongLo);
        break;
    case OP_ADD_FLOAT_2ADDR:
    case OP_SUB_FLOAT_2ADDR:
    case OP_MUL_FLOAT_2ADDR:
    case OP_DIV_FLOAT_2ADDR:
    case OP_REM_FLOAT_2ADDR:
        setRegisterType(workRegs, decInsn.vA, kRegTypeFloat);
        break;
    case OP_ADD_DOUBLE_2ADDR:
    case OP_SUB_DOUBLE_2ADDR:
    case OP_MUL_DOUBLE_2ADDR:
    case OP_DIV_DOUBLE_2ADDR:
    case OP_REM_DOUBLE_2ADDR:
        setRegisterType(workRegs, decInsn.vA, kRegTypeDoubleLo);
        break;
    case OP_ADD_INT_LIT16:
    case OP_RSUB_INT:
    case OP_MUL_INT_LIT16:
    case OP_DIV_INT_LIT16:
    case OP_REM_INT_LIT16:
    case OP_AND_INT_LIT16:
    case OP_OR_INT_LIT16:
    case OP_XOR_INT_LIT16:
    case OP_ADD_INT_LIT8:
    case OP_RSUB_INT_LIT8:
    case OP_MUL_INT_LIT8:
    case OP_DIV_INT_LIT8:
    case OP_REM_INT_LIT8:
    case OP_SHL_INT_LIT8:
    case OP_SHR_INT_LIT8:
    case OP_USHR_INT_LIT8:
    case OP_AND_INT_LIT8:
    case OP_OR_INT_LIT8:
    case OP_XOR_INT_LIT8:
        setRegisterType(workRegs, decInsn.vA, kRegTypeInteger);
        break;


    /*
     * See comments in analysis/CodeVerify.c re: why some of these are
     * annoying to deal with.  It's worse in this implementation, because
     * we're not keeping any information about the classes held in each
     * reference register.
     *
     * Handling most of these would require retaining the field/method
     * reference info that we discarded when the instructions were
     * quickened.  This is feasible but not currently supported.
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
        dvmAbort();     // not implemented, shouldn't be here
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
        dvmAbort();
        break;

    /*
     * DO NOT add a "default" clause here.  Without it the compiler will
     * complain if an instruction is missing (which is desirable).
     */
    }


    /*
     * If we didn't just set the result register, clear it out.  This
     * isn't so important here, but does help ensure that our output matches
     * the verifier.
     */
    if (!justSetResult) {
        int reg = RESULT_REGISTER(pState->insnRegCountPlus);
        workRegs[reg] = workRegs[reg+1] = kRegTypeUnknown;
    }

    /*
     * Handle "continue".  Tag the next consecutive instruction.
     */
    if ((nextFlags & kInstrCanContinue) != 0) {
        int insnWidth = dvmInsnGetWidth(insnFlags, insnIdx);

        /*
         * We want to update the registers and set the "changed" flag on the
         * next instruction (if necessary).  We aren't storing register
         * changes for all addresses, so for non-GC-point targets we just
         * compare "entry" vs. "work" to see if we've changed anything.
         */
        if (getRegisterLine(pState, insnIdx+insnWidth) != NULL) {
            updateRegisters(pState, insnIdx+insnWidth, workRegs);
        } else {
            /* if not yet visited, or regs were updated, set "changed" */
            if (!dvmInsnIsVisited(insnFlags, insnIdx+insnWidth) ||
                compareRegisters(workRegs, entryRegs,
                    pState->insnRegCountPlus) != 0)
            {
                dvmInsnSetChanged(insnFlags, insnIdx+insnWidth, true);
            }
        }
    }

    /*
     * Handle "branch".  Tag the branch target.
     */
    if ((nextFlags & kInstrCanBranch) != 0) {
        bool isConditional;

        dvmGetBranchTarget(meth, insnFlags, insnIdx, &branchTarget,
                &isConditional);
        assert(isConditional || (nextFlags & kInstrCanContinue) == 0);
        assert(!isConditional || (nextFlags & kInstrCanContinue) != 0);

        updateRegisters(pState, insnIdx+branchTarget, workRegs);
    }

    /*
     * Handle "switch".  Tag all possible branch targets.
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
            assert(absOffset >= 0 && absOffset < pState->insnsSize);

            updateRegisters(pState, absOffset, workRegs);
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
            while (true) {
                DexCatchHandler* handler = dexCatchIteratorNext(&iterator);
                if (handler == NULL)
                    break;

                /* note we use entryRegs, not workRegs */
                updateRegisters(pState, handler->address, entryRegs);
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

    assert(*pStartGuess >= 0 && *pStartGuess < pState->insnsSize &&
        dvmInsnGetWidth(insnFlags, *pStartGuess) != 0);

    result = true;

bail:
    return result;
}


/*
 * Merge two SRegType values.
 *
 * Sets "*pChanged" to "true" if the result doesn't match "type1".
 */
static SRegType mergeTypes(SRegType type1, SRegType type2, bool* pChanged)
{
    SRegType result;

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
            assert(type1 == type2);
            result = type1;
        }
    }

    if (result != type1)
        *pChanged = true;
    return result;
}

/*
 * Control can transfer to "nextInsn".
 *
 * Merge the registers from "workRegs" into "addrRegs" at "nextInsn", and
 * set the "changed" flag on the target address if the registers have changed.
 */
static void updateRegisters(WorkState* pState, int nextInsn,
    const SRegType* workRegs)
{
    const Method* meth = pState->method;
    InsnFlags* insnFlags = pState->insnFlags;
    const int insnRegCountPlus = pState->insnRegCountPlus;
    SRegType* targetRegs = getRegisterLine(pState, nextInsn);

    if (!dvmInsnIsVisitedOrChanged(insnFlags, nextInsn)) {
        /*
         * We haven't processed this instruction before, and we haven't
         * touched the registers here, so there's nothing to "merge".  Copy
         * the registers over and mark it as changed.  (This is the only
         * way a register can transition out of "unknown", so this is not
         * just an optimization.)
         */
        LOGVV("COPY into 0x%04x\n", nextInsn);
        copyRegisters(targetRegs, workRegs, insnRegCountPlus);
        dvmInsnSetChanged(insnFlags, nextInsn, true);
    } else {
        /* merge registers, set Changed only if different */
        LOGVV("MERGE into 0x%04x\n", nextInsn);
        bool changed = false;
        int i;

        for (i = 0; i < insnRegCountPlus; i++) {
            targetRegs[i] = mergeTypes(targetRegs[i], workRegs[i], &changed);
        }

        if (changed)
            dvmInsnSetChanged(insnFlags, nextInsn, true);
    }
}

#endif /*#if 0*/

