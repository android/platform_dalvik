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
 * The "dexdump" tool is intended to mimic "objdump".  When possible, use
 * similar command-line arguments.
 *
 * TODO: rework the output format to be more regexp-friendly
 */
#include "libdex/DexFile.h"
#include "libdex/DexCatch.h"
#include "libdex/DexClass.h"
#include "libdex/DexProto.h"
#include "libdex/InstrUtils.h"
#include "libdex/SysUtil.h"
#include "libdex/CmdUtils.h"

#include "dexdump/OpCodeNames.h"

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <getopt.h>
#include <errno.h>
#include <assert.h>

static const char* gProgName = "dexdump";

static InstructionWidth* gInstrWidth;
static InstructionFormat* gInstrFormat;

/* command-line options */
struct {
    bool disassemble;
    bool showFileHeaders;
    bool showSectionHeaders;
    const char* tempFileName;
} gOptions;

/* basic info about a field or method */
typedef struct FieldMethodInfo {
    const char* classDescriptor;
    const char* name;
    const char* signature;
} FieldMethodInfo;

/*
 * Get 2 little-endian bytes. 
 */ 
static inline u2 get2LE(unsigned char const* pSrc)
{
    return pSrc[0] | (pSrc[1] << 8);
}   

/*
 * Return a newly-allocated string for the "dot version" of the class
 * name for the given type descriptor. That is, The initial "L" and
 * final ";" (if any) have been removed and all occurrences of '/'
 * have been changed to '.'.
 */
static char* descriptorToDot(const char* str)
{
    size_t at = strlen(str);
    char* newStr;

    if (str[0] == 'L') {
        assert(str[at - 1] == ';');
        at -= 2; /* Two fewer chars to copy. */
        str++; /* Skip the 'L'. */
    }

    newStr = malloc(at + 1); /* Add one for the '\0'. */
    newStr[at] = '\0';

    while (at > 0) {
        at--;
        newStr[at] = (str[at] == '/') ? '.' : str[at];
    }

    return newStr;
}

/*
 * Count the number of '1' bits in a word.
 *
 * Having completed this, I'm ready for an interview at Google.
 *
 * TODO? there's a parallel version w/o loops.  Performance not currently
 * important.
 */
static int countOnes(u4 val)
{
    int count = 0;

    while (val != 0) {
        val &= val-1;
        count++;
    }

    return count;
}


/*
 * Flag for use with createAccessFlagStr().
 */
typedef enum AccessFor {
    kAccessForClass = 0, kAccessForMethod = 1, kAccessForField = 2,
    kAccessForMAX
} AccessFor;

/*
 * Create a new string with human-readable access flags.
 *
 * In the base language the access_flags fields are type u2; in Dalvik
 * they're u4.
 */
static char* createAccessFlagStr(u4 flags, AccessFor forWhat)
{
#define NUM_FLAGS   18
    static const char* kAccessStrings[kAccessForMAX][NUM_FLAGS] = {
        {   
            /* class, inner class */
            "PUBLIC",           /* 0x0001 */
            "PRIVATE",          /* 0x0002 */
            "PROTECTED",        /* 0x0004 */
            "STATIC",           /* 0x0008 */
            "FINAL",            /* 0x0010 */
            "?",                /* 0x0020 */
            "?",                /* 0x0040 */
            "?",                /* 0x0080 */
            "?",                /* 0x0100 */
            "INTERFACE",        /* 0x0200 */
            "ABSTRACT",         /* 0x0400 */
            "?",                /* 0x0800 */
            "SYNTHETIC",        /* 0x1000 */
            "ANNOTATION",       /* 0x2000 */
            "ENUM",             /* 0x4000 */
            "?",                /* 0x8000 */
            "VERIFIED",         /* 0x10000 */
            "OPTIMIZED",        /* 0x20000 */
        },
        {
            /* method */
            "PUBLIC",           /* 0x0001 */
            "PRIVATE",          /* 0x0002 */
            "PROTECTED",        /* 0x0004 */
            "STATIC",           /* 0x0008 */
            "FINAL",            /* 0x0010 */
            "SYNCHRONIZED",     /* 0x0020 */
            "BRIDGE",           /* 0x0040 */
            "VARARGS",          /* 0x0080 */
            "NATIVE",           /* 0x0100 */
            "?",                /* 0x0200 */
            "ABSTRACT",         /* 0x0400 */
            "STRICT",           /* 0x0800 */
            "SYNTHETIC",        /* 0x1000 */
            "?",                /* 0x2000 */
            "?",                /* 0x4000 */
            "MIRANDA",          /* 0x8000 */
            "CONSTRUCTOR",      /* 0x10000 */
            "DECLARED_SYNCHRONIZED", /* 0x20000 */
        },
        {
            /* field */
            "PUBLIC",           /* 0x0001 */
            "PRIVATE",          /* 0x0002 */
            "PROTECTED",        /* 0x0004 */
            "STATIC",           /* 0x0008 */
            "FINAL",            /* 0x0010 */
            "?",                /* 0x0020 */
            "VOLATILE",         /* 0x0040 */
            "TRANSIENT",        /* 0x0080 */
            "?",                /* 0x0100 */
            "?",                /* 0x0200 */
            "?",                /* 0x0400 */
            "?",                /* 0x0800 */
            "SYNTHETIC",        /* 0x1000 */
            "?",                /* 0x2000 */
            "ENUM",             /* 0x4000 */
            "?",                /* 0x8000 */
            "?",                /* 0x10000 */
            "?",                /* 0x20000 */
        },
    };
    const int kLongest = 21;        /* strlen of longest string above */
    int i, count;
    char* str;
    char* cp;

    /*
     * Allocate enough storage to hold the expected number of strings,
     * plus a space between each.  We over-allocate, using the longest
     * string above as the base metric.
     */
    count = countOnes(flags);
    cp = str = (char*) malloc(count * (kLongest+1) +1);

    for (i = 0; i < NUM_FLAGS; i++) {
        if (flags & 0x01) {
            const char* accessStr = kAccessStrings[forWhat][i];
            int len = strlen(accessStr);
            if (cp != str)
                *cp++ = ' ';

            memcpy(cp, accessStr, len);
            cp += len;
        }
        flags >>= 1;
    }
    *cp = '\0';

    return str;
}


/*
 * Dump the file header.
 */
void dumpFileHeader(const DexFile* pDexFile)
{
    const DexHeader* pHeader = pDexFile->pHeader;

    printf("DEX file header:\n");
    printf("magic               : '%.8s'\n", pHeader->magic);
    printf("checksum            : %08x\n", pHeader->checksum);
    printf("signature           : %02x%02x...%02x%02x\n",
        pHeader->signature[0], pHeader->signature[1],
        pHeader->signature[kSHA1DigestLen-2],
        pHeader->signature[kSHA1DigestLen-1]);
    printf("file_size           : %d\n", pHeader->fileSize);
    printf("header_size         : %d\n", pHeader->headerSize);
    printf("link_size           : %d\n", pHeader->linkSize);
    printf("link_off            : %d (0x%06x)\n",
        pHeader->linkOff, pHeader->linkOff);
    printf("string_ids_size     : %d\n", pHeader->stringIdsSize);
    printf("string_ids_off      : %d (0x%06x)\n",
        pHeader->stringIdsOff, pHeader->stringIdsOff);
    printf("type_ids_size       : %d\n", pHeader->typeIdsSize);
    printf("type_ids_off        : %d (0x%06x)\n",
        pHeader->typeIdsOff, pHeader->typeIdsOff);
    printf("field_ids_size      : %d\n", pHeader->fieldIdsSize);
    printf("field_ids_off       : %d (0x%06x)\n",
        pHeader->fieldIdsOff, pHeader->fieldIdsOff);
    printf("method_ids_size     : %d\n", pHeader->methodIdsSize);
    printf("method_ids_off      : %d (0x%06x)\n",
        pHeader->methodIdsOff, pHeader->methodIdsOff);
    printf("class_defs_size     : %d\n", pHeader->classDefsSize);
    printf("class_defs_off      : %d (0x%06x)\n",
        pHeader->classDefsOff, pHeader->classDefsOff);
    printf("data_size           : %d\n", pHeader->dataSize);
    printf("data_off            : %d (0x%06x)\n",
        pHeader->dataOff, pHeader->dataOff);
    printf("\n");
}

/*
 * Dump a class_def_item.
 */
void dumpClassDef(DexFile* pDexFile, int idx)
{
    const DexClassDef* pClassDef;
    const u1* pEncodedData;
    DexClassData* pClassData;

    pClassDef = dexGetClassDef(pDexFile, idx);
    pEncodedData = dexGetClassData(pDexFile, pClassDef);
    pClassData = dexReadAndVerifyClassData(&pEncodedData, NULL);

    if (pClassData == NULL) {
        fprintf(stderr, "Trouble reading class data\n");
        return;
    }

    printf("Class #%d header:\n", idx);
    printf("class_idx           : %d\n", pClassDef->classIdx);
    printf("access_flags        : %d (0x%04x)\n",
        pClassDef->accessFlags, pClassDef->accessFlags);
    printf("superclass_idx      : %d\n", pClassDef->superclassIdx);
    printf("interfaces_off      : %d (0x%06x)\n",
        pClassDef->interfacesOff, pClassDef->interfacesOff);
    printf("source_file_idx     : %d\n", pClassDef->sourceFileIdx);
    printf("annotations_off     : %d (0x%06x)\n",
        pClassDef->annotationsOff, pClassDef->annotationsOff);
    printf("class_data_off      : %d (0x%06x)\n",
        pClassDef->classDataOff, pClassDef->classDataOff);
    printf("static_fields_size  : %d\n", pClassData->header.staticFieldsSize);
    printf("instance_fields_size: %d\n",
            pClassData->header.instanceFieldsSize);
    printf("direct_methods_size : %d\n", pClassData->header.directMethodsSize);
    printf("virtual_methods_size: %d\n",
            pClassData->header.virtualMethodsSize);
    printf("\n");

    free(pClassData);
}

/*
 * Dump an interface.
 */
void dumpInterface(const DexFile* pDexFile, const DexTypeItem* pTypeItem,
    int i)
{
    const char* interfaceName =
        dexStringByTypeIdx(pDexFile, pTypeItem->typeIdx);

    printf("    #%d              : '%s'\n", i, interfaceName);
}

/*
 * Dump the catches table associated with the code.
 */
void dumpCatches(DexFile* pDexFile, const DexCode* pCode)
{
    u4 triesSize = pCode->triesSize;

    if (triesSize == 0) {
        printf("      catches       : (none)\n");
        return;
    } 

    printf("      catches       : %d\n", triesSize);

    const DexTry* pTries = dexGetTries(pCode);
    u4 i;

    for (i = 0; i < triesSize; i++) {
        const DexTry* pTry = &pTries[i];
        u4 start = pTry->startAddr;
        u4 end = start + pTry->insnCount;
        DexCatchIterator iterator;
        
        printf("        0x%04x - 0x%04x\n", start, end);

        dexCatchIteratorInit(&iterator, pCode, pTry->handlerOff);

        for (;;) {
            DexCatchHandler* handler = dexCatchIteratorNext(&iterator);
            const char* descriptor;
            
            if (handler == NULL) {
                break;
            }
            
            descriptor = (handler->typeIdx == kDexNoIndex) ? "<any>" : 
                dexStringByTypeIdx(pDexFile, handler->typeIdx);
            
            printf("          %s -> 0x%04x\n", descriptor,
                    handler->address);
        }
    }
}

static int dumpPositionsCb(void *cnxt, u4 address, u4 lineNum)
{
    printf("        0x%04x line=%d\n", address, lineNum);
    return 0;
}

/*
 * Dump the positions list.
 */
void dumpPositions(DexFile* pDexFile, const DexCode* pCode, 
        const DexMethod *pDexMethod)
{
    printf("      positions     : \n");
    const DexMethodId *pMethodId 
            = dexGetMethodId(pDexFile, pDexMethod->methodIdx);
    const char *classDescriptor
            = dexStringByTypeIdx(pDexFile, pMethodId->classIdx);

    dexDecodeDebugInfo(pDexFile, pCode, classDescriptor, pMethodId->protoIdx,
            pDexMethod->accessFlags, dumpPositionsCb, NULL, NULL);
}

static void dumpLocalsCb(void *cnxt, u2 reg, u4 startAddress,
        u4 endAddress, const char *name, const char *descriptor,
        const char *signature)
{
    printf("        0x%04x - 0x%04x reg=%d %s %s %s\n",
            startAddress, endAddress, reg, name, descriptor, 
            signature);
}

/*
 * Dump the locals list.
 */
void dumpLocals(DexFile* pDexFile, const DexCode* pCode,
        const DexMethod *pDexMethod)
{
    printf("      locals        : \n");

    const DexMethodId *pMethodId 
            = dexGetMethodId(pDexFile, pDexMethod->methodIdx);
    const char *classDescriptor 
            = dexStringByTypeIdx(pDexFile, pMethodId->classIdx);

    dexDecodeDebugInfo(pDexFile, pCode, classDescriptor, pMethodId->protoIdx,
            pDexMethod->accessFlags, NULL, dumpLocalsCb, NULL);
}

/*
 * Get information about a method.
 */
bool getMethodInfo(DexFile* pDexFile, u4 methodIdx, FieldMethodInfo* pMethInfo)
{
    const DexMethodId* pMethodId;

    if (methodIdx >= pDexFile->pHeader->methodIdsSize)
        return false;

    pMethodId = dexGetMethodId(pDexFile, methodIdx);
    pMethInfo->name = dexStringById(pDexFile, pMethodId->nameIdx);
    pMethInfo->signature = dexCopyDescriptorFromMethodId(pDexFile, pMethodId);

    pMethInfo->classDescriptor = 
            dexStringByTypeIdx(pDexFile, pMethodId->classIdx);
    return true;
}

/*
 * Get information about a field.
 */
bool getFieldInfo(DexFile* pDexFile, u4 fieldIdx, FieldMethodInfo* pFieldInfo)
{
    const DexFieldId* pFieldId;

    if (fieldIdx >= pDexFile->pHeader->fieldIdsSize)
        return false;

    pFieldId = dexGetFieldId(pDexFile, fieldIdx);
    pFieldInfo->name = dexStringById(pDexFile, pFieldId->nameIdx);
    pFieldInfo->signature = dexStringByTypeIdx(pDexFile, pFieldId->typeIdx);
    pFieldInfo->classDescriptor =
        dexStringByTypeIdx(pDexFile, pFieldId->classIdx);
    return true;
}


/*
 * Look up a class' descriptor.
 */
const char* getClassDescriptor(DexFile* pDexFile, u4 classIdx)
{
    return dexStringByTypeIdx(pDexFile, classIdx);
}

/*
 * Dump a single instruction.
 */
void dumpInstruction(DexFile* pDexFile, const DexCode* pCode, int insnIdx,
    int insnWidth, const DecodedInstruction* pDecInsn)
{
    const u2* insns = pCode->insns;
    int i;

    printf("%06x:", ((u1*)insns - pDexFile->baseAddr) + insnIdx*2);
    for (i = 0; i < 8; i++) {
        if (i < insnWidth) {
            if (i == 7) {
                printf(" ... ");
            } else {
                /* print 16-bit value in little-endian order */
                const u1* bytePtr = (const u1*) &insns[insnIdx+i];
                printf(" %02x%02x", bytePtr[0], bytePtr[1]);
            }
        } else {
            fputs("     ", stdout);
        }
    }

    if (pDecInsn->opCode == OP_NOP) {
        u2 instr = get2LE((const u1*) &insns[insnIdx]);
        if (instr == kPackedSwitchSignature) {
            printf("|%04x: packed-switch-data (%d units)",
                insnIdx, insnWidth);
        } else if (instr == kSparseSwitchSignature) {
            printf("|%04x: sparse-switch-data (%d units)",
                insnIdx, insnWidth);
        } else if (instr == kArrayDataSignature) {
            printf("|%04x: array-data (%d units)",
                insnIdx, insnWidth);
        } else {
            printf("|%04x: nop // spacer", insnIdx);
        }
    } else {
        printf("|%04x: %s", insnIdx, getOpcodeName(pDecInsn->opCode));
    }

    switch (dexGetInstrFormat(gInstrFormat, pDecInsn->opCode)) {
    case kFmt10x:        // op
        break;
    case kFmt12x:        // op vA, vB
        printf(" v%d, v%d", pDecInsn->vA, pDecInsn->vB);
        break;
    case kFmt11n:        // op vA, #+B
        printf(" v%d, #int %d // #%x",
            pDecInsn->vA, (s4)pDecInsn->vB, (u1)pDecInsn->vB);
        break;
    case kFmt11x:        // op vAA
        printf(" v%d", pDecInsn->vA);
        break;
    case kFmt10t:        // op +AA
    case kFmt20t:        // op +AAAA
        {
            s4 targ = (s4) pDecInsn->vA;
            printf(" %04x // %c%04x",
                insnIdx + targ,
                (targ < 0) ? '-' : '+',
                (targ < 0) ? -targ : targ);
        }
        break;
    case kFmt22x:        // op vAA, vBBBB
        printf(" v%d, v%d", pDecInsn->vA, pDecInsn->vB);
        break;
    case kFmt21t:        // op vAA, +BBBB
        {
            s4 targ = (s4) pDecInsn->vB;
            printf(" v%d, %04x // %c%04x", pDecInsn->vA,
                insnIdx + targ,
                (targ < 0) ? '-' : '+',
                (targ < 0) ? -targ : targ);
        }
        break;
    case kFmt21s:        // op vAA, #+BBBB
        printf(" v%d, #int %d // #%x",
            pDecInsn->vA, (s4)pDecInsn->vB, (u2)pDecInsn->vB);
        break;
    case kFmt21h:        // op vAA, #+BBBB0000[00000000]
        // The printed format varies a bit based on the actual opcode.
        if (pDecInsn->opCode == OP_CONST_HIGH16) {
            s4 value = pDecInsn->vB << 16;
            printf(" v%d, #int %d // #%x",
                pDecInsn->vA, value, (u2)pDecInsn->vB);
        } else {
            s8 value = ((s8) pDecInsn->vB) << 48;
            printf(" v%d, #long %lld // #%x",
                pDecInsn->vA, value, (u2)pDecInsn->vB);
        }
        break;
    case kFmt21c:        // op vAA, thing@BBBB
        if (pDecInsn->opCode == OP_CONST_STRING) {
            printf(" v%d, \"%s\" // string@%04x", pDecInsn->vA,
                dexStringById(pDexFile, pDecInsn->vB), pDecInsn->vB);
        } else if (pDecInsn->opCode == OP_CHECK_CAST ||
                   pDecInsn->opCode == OP_NEW_INSTANCE ||
                   pDecInsn->opCode == OP_CONST_CLASS)
        {
            printf(" v%d, %s // class@%04x", pDecInsn->vA,
                getClassDescriptor(pDexFile, pDecInsn->vB), pDecInsn->vB);
        } else /* OP_SGET* */ {
            FieldMethodInfo fieldInfo;
            if (getFieldInfo(pDexFile, pDecInsn->vB, &fieldInfo)) {
                printf(" v%d, %s.%s:%s // field@%04x", pDecInsn->vA,
                    fieldInfo.classDescriptor, fieldInfo.name,
                    fieldInfo.signature, pDecInsn->vB);
            } else {
                printf(" v%d, ??? // field@%04x", pDecInsn->vA, pDecInsn->vB);
            }
        }
        break;
    case kFmt23x:        // op vAA, vBB, vCC
        printf(" v%d, v%d, v%d", pDecInsn->vA, pDecInsn->vB, pDecInsn->vC);
        break;
    case kFmt22b:        // op vAA, vBB, #+CC
        printf(" v%d, v%d, #int %d // #%02x",
            pDecInsn->vA, pDecInsn->vB, (s4)pDecInsn->vC, (u1)pDecInsn->vC);
        break;
    case kFmt22t:        // op vA, vB, +CCCC
        {
            s4 targ = (s4) pDecInsn->vC;
            printf(" v%d, v%d, %04x // %c%04x", pDecInsn->vA, pDecInsn->vB,
                insnIdx + targ,
                (targ < 0) ? '-' : '+',
                (targ < 0) ? -targ : targ);
        }
        break;
    case kFmt22s:        // op vA, vB, #+CCCC
        printf(" v%d, v%d, #int %d // #%04x",
            pDecInsn->vA, pDecInsn->vB, (s4)pDecInsn->vC, (u2)pDecInsn->vC);
        break;
    case kFmt22c:        // op vA, vB, thing@CCCC
        if (pDecInsn->opCode >= OP_IGET && pDecInsn->opCode <= OP_IPUT_SHORT) {
            FieldMethodInfo fieldInfo;
            if (getFieldInfo(pDexFile, pDecInsn->vC, &fieldInfo)) {
                printf(" v%d, v%d, %s.%s:%s // field@%04x", pDecInsn->vA,
                    pDecInsn->vB, fieldInfo.classDescriptor, fieldInfo.name,
                    fieldInfo.signature, pDecInsn->vC);
            } else {
                printf(" v%d, v%d, ??? // field@%04x", pDecInsn->vA,
                    pDecInsn->vB, pDecInsn->vC);
            }
        } else {
            printf(" v%d, v%d, %s // class@%04x",
                pDecInsn->vA, pDecInsn->vB,
                getClassDescriptor(pDexFile, pDecInsn->vC), pDecInsn->vC);
        }
        break;
    case kFmt22cs:       // [opt] op vA, vB, field offset CCCC
        printf(" v%d, v%d, [obj+%04x]",
            pDecInsn->vA, pDecInsn->vB, pDecInsn->vC);
        break;
    case kFmt30t:
        printf(" #%08x", pDecInsn->vA);
        break;
    case kFmt31i:        // op vAA, #+BBBBBBBB
        {
            /* this is often, but not always, a float */
            union {
                float f;
                u4 i;
            } conv;
            conv.i = pDecInsn->vB;
            printf(" v%d, #float %f // #%08x",
                pDecInsn->vA, conv.f, pDecInsn->vB);
        }
        break;
    case kFmt31c:        // op vAA, thing@BBBBBBBB
        printf(" v%d, \"%s\" // string@%08x", pDecInsn->vA,
            dexStringById(pDexFile, pDecInsn->vB), pDecInsn->vB);
        break;
    case kFmt31t:       // op vAA, offset +BBBBBBBB
        printf(" v%d, %08x // +%08x",
            pDecInsn->vA, insnIdx + pDecInsn->vB, pDecInsn->vB);
        break;
    case kFmt32x:        // op vAAAA, vBBBB
        printf(" v%d, v%d", pDecInsn->vA, pDecInsn->vB);
        break;
    case kFmt35c:        // op vB, {vD, vE, vF, vG, vA}, thing@CCCC
        {
            /* NOTE: decoding of 35c doesn't quite match spec */
            fputs(" {", stdout);
            for (i = 0; i < (int) pDecInsn->vA; i++) {
                if (i == 0)
                    printf("v%d", pDecInsn->arg[i]);
                else
                    printf(", v%d", pDecInsn->arg[i]);
            }
            if (pDecInsn->opCode == OP_FILLED_NEW_ARRAY) {
                printf("}, %s // class@%04x",
                    getClassDescriptor(pDexFile, pDecInsn->vB), pDecInsn->vB);
            } else {
                FieldMethodInfo methInfo;
                if (getMethodInfo(pDexFile, pDecInsn->vB, &methInfo)) {
                    printf("}, %s.%s:%s // method@%04x",
                        methInfo.classDescriptor, methInfo.name,
                        methInfo.signature, pDecInsn->vB);
                } else {
                    printf("}, ??? // method@%04x", pDecInsn->vB);
                }
            }
        }
        break;
    case kFmt35ms:       // [opt] invoke-virtual+super
    case kFmt35fs:       // [opt] invoke-interface
        {
            fputs(" {", stdout);
            for (i = 0; i < (int) pDecInsn->vA; i++) {
                if (i == 0)
                    printf("v%d", pDecInsn->arg[i]);
                else
                    printf(", v%d", pDecInsn->arg[i]);
            }
            printf("}, [%04x] // vtable #%04x", pDecInsn->vB, pDecInsn->vB);
        }
        break;
    case kFmt3rc:        // op {vCCCC .. v(CCCC+AA-1)}, meth@BBBB
        {
            /*
             * This doesn't match the "dx" output when some of the args are
             * 64-bit values -- dx only shows the first register.
             */
            fputs(" {", stdout);
            for (i = 0; i < (int) pDecInsn->vA; i++) {
                if (i == 0)
                    printf("v%d", pDecInsn->vC + i);
                else
                    printf(", v%d", pDecInsn->vC + i);
            }
            if (pDecInsn->opCode == OP_FILLED_NEW_ARRAY_RANGE) {
                printf("}, %s // class@%04x",
                    getClassDescriptor(pDexFile, pDecInsn->vB), pDecInsn->vB);
            } else {
                FieldMethodInfo methInfo;
                if (getMethodInfo(pDexFile, pDecInsn->vB, &methInfo)) {
                    printf("}, %s.%s:%s // method@%04x",
                        methInfo.classDescriptor, methInfo.name,
                        methInfo.signature, pDecInsn->vB);
                } else {
                    printf("}, ??? // method@%04x", pDecInsn->vB);
                }
            }
        }
        break;
    case kFmt3rms:       // [opt] invoke-virtual+super/range
    case kFmt3rfs:       // [opt] invoke-interface/range
        {
            /*
             * This doesn't match the "dx" output when some of the args are
             * 64-bit values -- dx only shows the first register.
             */
            fputs(" {", stdout);
            for (i = 0; i < (int) pDecInsn->vA; i++) {
                if (i == 0)
                    printf("v%d", pDecInsn->vC + i);
                else
                    printf(", v%d", pDecInsn->vC + i);
            }
            printf("}, [%04x] // vtable #%04x", pDecInsn->vB, pDecInsn->vB);
        }
        break;
    case kFmt3inline:    // [opt] inline invoke
        {
#if 0
            const InlineOperation* inlineOpsTable = dvmGetInlineOpsTable();
            u4 tableLen = dvmGetInlineOpsTableLength();
#endif

            fputs(" {", stdout);
            for (i = 0; i < (int) pDecInsn->vA; i++) {
                if (i == 0)
                    printf("v%d", pDecInsn->arg[i]);
                else
                    printf(", v%d", pDecInsn->arg[i]);
            }
#if 0
            if (pDecInsn->vB < tableLen) {
                printf("}, %s.%s:%s // inline #%04x",
                    inlineOpsTable[pDecInsn->vB].classDescriptor,
                    inlineOpsTable[pDecInsn->vB].methodName,
                    inlineOpsTable[pDecInsn->vB].methodSignature,
                    pDecInsn->vB);
            } else {
#endif
                printf("}, [%04x] // inline #%04x", pDecInsn->vB, pDecInsn->vB);
#if 0
            }
#endif
        }
        break;
    case kFmt51l:        // op vAA, #+BBBBBBBBBBBBBBBB
        {
            /* this is often, but not always, a double */
            union {
                double d;
                u8 j;
            } conv;
            conv.j = pDecInsn->vB_wide;
            printf(" v%d, #double %f // #%016llx",
                pDecInsn->vA, conv.d, pDecInsn->vB_wide);
        }
        break;
    case kFmtUnknown:
        break;
    default:
        printf(" ???");
        break;
    }


    putchar('\n');

}

/*
 * Dump a bytecode disassembly.
 */
void dumpBytecodes(DexFile* pDexFile, const DexMethod* pDexMethod)
{
    const DexCode* pCode = dexGetCode(pDexFile, pDexMethod);
    const u2* insns;
    int insnIdx;
    FieldMethodInfo methInfo;
    int startAddr;
    char* className = NULL;

    assert(pCode->insnsSize > 0);
    insns = pCode->insns;

    getMethodInfo(pDexFile, pDexMethod->methodIdx, &methInfo);
    startAddr = ((u1*)pCode - pDexFile->baseAddr);
    className = descriptorToDot(methInfo.classDescriptor);

    printf("%06x:                                        |[%06x] %s.%s:%s\n",
        startAddr, startAddr,
        className, methInfo.name, methInfo.signature);

    insnIdx = 0;
    while (insnIdx < (int) pCode->insnsSize) {
        int insnWidth;
        OpCode opCode;
        DecodedInstruction decInsn;
        u2 instr;

        instr = get2LE((const u1*)insns);
        if (instr == kPackedSwitchSignature) {
            insnWidth = 4 + get2LE((const u1*)(insns+1)) * 2;
        } else if (instr == kSparseSwitchSignature) {
            insnWidth = 2 + get2LE((const u1*)(insns+1)) * 4;
        } else if (instr == kArrayDataSignature) {
            int width = get2LE((const u1*)(insns+1));
            int size = get2LE((const u1*)(insns+2)) | 
                       (get2LE((const u1*)(insns+3))<<16);
            // The plus 1 is to round up for odd size and width 
            insnWidth = 4 + ((size * width) + 1) / 2;
        } else {
            opCode = instr & 0xff;
            insnWidth = dexGetInstrWidthAbs(gInstrWidth, opCode);
            if (insnWidth == 0) {
                fprintf(stderr,
                    "GLITCH: zero-width instruction at idx=0x%04x\n", insnIdx);
                break;
            }
        }

        dexDecodeInstruction(gInstrFormat, insns, &decInsn);
        dumpInstruction(pDexFile, pCode, insnIdx, insnWidth, &decInsn);

        insns += insnWidth;
        insnIdx += insnWidth;
    }

    free(className);
}

/*
 * Dump a "code" struct.
 */
void dumpCode(DexFile* pDexFile, const DexMethod* pDexMethod)
{
    const DexCode* pCode = dexGetCode(pDexFile, pDexMethod);

    printf("      registers     : %d\n", pCode->registersSize);
    printf("      ins           : %d\n", pCode->insSize);
    printf("      outs          : %d\n", pCode->outsSize);
    printf("      insns size    : %d 16-bit code units\n", pCode->insnsSize);

    if (gOptions.disassemble)
        dumpBytecodes(pDexFile, pDexMethod);

    dumpCatches(pDexFile, pCode);
    /* both of these are encoded in debug info */
    dumpPositions(pDexFile, pCode, pDexMethod);
    dumpLocals(pDexFile, pCode, pDexMethod);
}

/*
 * Dump a method.
 */
void dumpMethod(DexFile* pDexFile, const DexMethod* pDexMethod, int i)
{
    const DexMethodId* pMethodId;
    const char* backDescriptor;
    const char* name;
    char* typeDescriptor;
    char* accessStr;

    pMethodId = dexGetMethodId(pDexFile, pDexMethod->methodIdx);
    name = dexStringById(pDexFile, pMethodId->nameIdx);
    typeDescriptor = dexCopyDescriptorFromMethodId(pDexFile, pMethodId);

    backDescriptor = dexStringByTypeIdx(pDexFile, pMethodId->classIdx);

    accessStr = createAccessFlagStr(pDexMethod->accessFlags,
                    kAccessForMethod);

    printf("    #%d              : (in %s)\n", i, backDescriptor);
    printf("      name          : '%s'\n", name);
    printf("      type          : '%s'\n", typeDescriptor);
    printf("      access        : 0x%04x (%s)\n",
        pDexMethod->accessFlags, accessStr);

    if (pDexMethod->codeOff == 0) {
        printf("      code          : (none)\n");
    } else {
        printf("      code          -\n");
        dumpCode(pDexFile, pDexMethod);
    }

    if (gOptions.disassemble)
        putchar('\n');

    free(typeDescriptor);
    free(accessStr);
}

/*
 * Dump a static (class) field.
 */
void dumpSField(const DexFile* pDexFile, const DexField* pSField, int i)
{
    const DexFieldId* pFieldId;
    const char* backDescriptor;
    const char* name;
    const char* typeDescriptor;
    char* accessStr;

    pFieldId = dexGetFieldId(pDexFile, pSField->fieldIdx);
    name = dexStringById(pDexFile, pFieldId->nameIdx);
    typeDescriptor = dexStringByTypeIdx(pDexFile, pFieldId->typeIdx);
    backDescriptor = dexStringByTypeIdx(pDexFile, pFieldId->classIdx);

    accessStr = createAccessFlagStr(pSField->accessFlags, kAccessForField);

    printf("    #%d              : (in %s)\n", i, backDescriptor);
    printf("      name          : '%s'\n", name);
    printf("      type          : '%s'\n", typeDescriptor);
    printf("      access        : 0x%04x (%s)\n",
        pSField->accessFlags, accessStr);

    free(accessStr);
}

/*
 * Dump an instance field.
 */
void dumpIField(const DexFile* pDexFile, const DexField* pIField, int i)
{
    const DexFieldId* pFieldId;
    const char* backDescriptor;
    const char* name;
    const char* typeDescriptor;
    char* accessStr;

    pFieldId = dexGetFieldId(pDexFile, pIField->fieldIdx);
    name = dexStringById(pDexFile, pFieldId->nameIdx);
    typeDescriptor = dexStringByTypeIdx(pDexFile, pFieldId->typeIdx);
    backDescriptor = dexStringByTypeIdx(pDexFile, pFieldId->classIdx);

    accessStr = createAccessFlagStr(pIField->accessFlags, kAccessForField);

    printf("    #%d              : (in %s)\n", i, backDescriptor);
    printf("      name          : '%s'\n", name);
    printf("      type          : '%s'\n", typeDescriptor);
    printf("      access        : 0x%04x (%s)\n",
        pIField->accessFlags, accessStr);

    free(accessStr);
}

/*
 * Dump the class.
 */
void dumpClass(DexFile* pDexFile, int idx)
{
    const DexTypeList* pInterfaces;
    const DexClassDef* pClassDef;
    DexClassData* pClassData;
    const u1* pEncodedData;
    const char* fileName;
    const char* classDescriptor;
    const char* superclassDescriptor;
    char* accessStr;
    int i;

    pClassDef = dexGetClassDef(pDexFile, idx);
    printf("Class #%d            -\n", idx);

    pEncodedData = dexGetClassData(pDexFile, pClassDef);
    pClassData = dexReadAndVerifyClassData(&pEncodedData, NULL);

    if (pClassData == NULL) {
        printf("Trouble reading class data\n");
        return;
    }
    
    classDescriptor = dexStringByTypeIdx(pDexFile, pClassDef->classIdx);
    printf("  Class descriptor  : '%s'\n", classDescriptor);

    accessStr = createAccessFlagStr(pClassDef->accessFlags, kAccessForClass);
    printf("  Access flags      : 0x%04x (%s)\n",
        pClassDef->accessFlags, accessStr);

    if (pClassDef->superclassIdx == kDexNoIndex)
        superclassDescriptor = "(none)";
    else {
        superclassDescriptor =
            dexStringByTypeIdx(pDexFile, pClassDef->superclassIdx);
        printf("  Superclass        : '%s'\n", superclassDescriptor);
    }

    printf("  Interfaces        -\n");
    pInterfaces = dexGetInterfacesList(pDexFile, pClassDef);
    if (pInterfaces != NULL) {
        for (i = 0; i < (int) pInterfaces->size; i++)
            dumpInterface(pDexFile, dexGetTypeItem(pInterfaces, i), i);
    }

    printf("  Static fields     -\n");
    for (i = 0; i < (int) pClassData->header.staticFieldsSize; i++) {
        dumpSField(pDexFile, &pClassData->staticFields[i], i);
    }

    printf("  Instance fields   -\n");
    for (i = 0; i < (int) pClassData->header.instanceFieldsSize; i++) {
        dumpIField(pDexFile, &pClassData->instanceFields[i], i);
    }

    printf("  Direct methods    -\n");
    for (i = 0; i < (int) pClassData->header.directMethodsSize; i++) {
        dumpMethod(pDexFile, &pClassData->directMethods[i], i);
    }

    printf("  Virtual methods   -\n");
    for (i = 0; i < (int) pClassData->header.virtualMethodsSize; i++) {
        dumpMethod(pDexFile, &pClassData->virtualMethods[i], i);
    }

    // TODO: Annotations.

    if (pClassDef->sourceFileIdx != kDexNoIndex)
        fileName = dexStringById(pDexFile, pClassDef->sourceFileIdx);
    else
        fileName = "unknown";
    printf("  source_file_idx   : %d (%s)\n",
        pClassDef->sourceFileIdx, fileName);

    printf("\n");

    free(pClassData);
    free(accessStr);
}

/*
 * Dump the requested sections of the file.
 */
void processDexFile(const char* fileName, DexFile* pDexFile)
{
    int i;

    printf("Opened '%s', DEX version '%.3s'\n", fileName,
        pDexFile->pHeader->magic +4);

    if (gOptions.showFileHeaders)
        dumpFileHeader(pDexFile);

    for (i = 0; i < (int) pDexFile->pHeader->classDefsSize; i++) {
        if (gOptions.showSectionHeaders)
            dumpClassDef(pDexFile, i);

        dumpClass(pDexFile, i);
    }
}


/*
 * Process one file.
 */
int process(const char* fileName)
{
    DexFile* pDexFile = NULL;
    MemMapping map;
    bool mapped = false;
    int result = -1;

    printf("Processing '%s'...\n", fileName);

    if (dexOpenAndMap(fileName, gOptions.tempFileName, &map, false) != 0)
        goto bail;
    mapped = true;

    pDexFile = dexFileParse(map.addr, map.length,
        kDexParseVerifyChecksum | kDexParseContinueOnError);
    if (pDexFile == NULL) {
        fprintf(stderr, "ERROR: DEX parse failed\n");
        goto bail;
    }

    processDexFile(fileName, pDexFile);

    result = 0;

bail:
    if (mapped)
        sysReleaseShmem(&map);
    if (pDexFile != NULL)
        dexFileFree(pDexFile);
    return result;
}


/*
 * Show usage.
 */
void usage(void)
{
    fprintf(stderr, "Copyright (C) 2007 The Android Open Source Project\n\n");
    fprintf(stderr, "%s: [-d] [-f] [-h] [-t tempfile] dexfile...\n", gProgName);
    fprintf(stderr, "\n");
    fprintf(stderr, " -d : disassemble code sections\n");
    fprintf(stderr, " -f : display summary information from file header\n");
    fprintf(stderr, " -h : display file header details\n");
    fprintf(stderr, " -t : temp file name (defaults to /sdcard/dex-temp-*)\n");
}

/*
 * Parse args.
 *
 * I'm not using getopt_long() because we may not have it in libc.
 */
int main(int argc, char* const argv[])
{
    bool wantUsage = false;
    int ic;

    memset(&gOptions, 0, sizeof(gOptions));

    while (1) {
        ic = getopt(argc, argv, "dfht:");
        if (ic < 0)
            break;

        switch (ic) {
        case 'd':       // disassemble Dalvik instructions
            gOptions.disassemble = true;
            break;
        case 'f':       // dump outer file header
            gOptions.showFileHeaders = true;
            break;
        case 'h':       // dump section headers, i.e. all meta-data
            gOptions.showSectionHeaders = true;
            break;
        case 't':       // temp file, used when opening compressed Jar
            gOptions.tempFileName = argv[optind];
            break;
        default:
            wantUsage = true;
            break;
        }
    }

    if (optind == argc) {
        fprintf(stderr, "%s: no file specified\n", gProgName);
        wantUsage = true;
    }

    /* initialize some VM tables */
    gInstrWidth = dexCreateInstrWidthTable();
    gInstrFormat = dexCreateInstrFormatTable();

    if (wantUsage) {
        usage();
        return 2;
    }

    while (optind < argc)
        process(argv[optind++]);

    free(gInstrWidth);
    free(gInstrFormat);

    return 0;
}
