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
 * VM-specific state associated with a DEX file.
 */
#include "Dalvik.h"

/*
 * Create auxillary data structures.
 *
 * We need a 4-byte pointer for every reference to a class, method, field,
 * or string constant.  Summed up over all loaded DEX files (including the
 * whoppers in the boostrap class path), this adds up to be quite a bit
 * of native memory.
 *
 * For more traditional VMs these values could be stuffed into the loaded
 * class file constant pool area, but we don't have that luxury since our
 * classes are memory-mapped read-only.
 *
 * The DEX optimizer will remove the need for some of these (e.g. we won't
 * use the entry for virtual methods that are only called through
 * invoke-virtual-quick), creating the possibility of some space reduction
 * at dexopt time.
 */
static DvmDex* allocateAuxStructures(DexFile* pDexFile)
{
    DvmDex* pDvmDex;
    const DexHeader* pHeader;
    u4 stringCount, classCount, methodCount, fieldCount;

    pDvmDex = (DvmDex*) calloc(1, sizeof(DvmDex));
    if (pDvmDex == NULL)
        return NULL;

    pDvmDex->pDexFile = pDexFile;
    pDvmDex->pHeader = pDexFile->pHeader;

    pHeader = pDvmDex->pHeader;

    stringCount = pHeader->stringIdsSize;
    classCount = pHeader->typeIdsSize;
    methodCount = pHeader->methodIdsSize;
    fieldCount = pHeader->fieldIdsSize;

#if (DVM_RESOLVER_CACHE == DVM_RC_REDUCING) || \
    (DVM_RESOLVER_CACHE == DVM_RC_EXPANDING)
    if (pDexFile->indexMap.stringReducedCount > 0)
        stringCount = pDexFile->indexMap.stringReducedCount;
    if (pDexFile->indexMap.classReducedCount > 0)
        classCount = pDexFile->indexMap.classReducedCount;
    if (pDexFile->indexMap.methodReducedCount > 0)
        methodCount = pDexFile->indexMap.methodReducedCount;
    if (pDexFile->indexMap.fieldReducedCount > 0)
        fieldCount = pDexFile->indexMap.fieldReducedCount;
#elif (DVM_RESOLVER_CACHE == DVM_RC_NO_CACHE)
    stringCount = classCount = methodCount = fieldCount = 0;
#endif

    pDvmDex->pResStrings = (struct StringObject**)
        calloc(stringCount, sizeof(struct StringObject*));

    pDvmDex->pResClasses = (struct ClassObject**)
        calloc(classCount, sizeof(struct ClassObject*));

    pDvmDex->pResMethods = (struct Method**)
        calloc(methodCount, sizeof(struct Method*));

    pDvmDex->pResFields = (struct Field**)
        calloc(fieldCount, sizeof(struct Field*));

    LOGV("+++ DEX %p: allocateAux %d+%d+%d+%d * 4 = %d bytes\n",
        pDvmDex, stringCount, classCount, methodCount, fieldCount,
        (stringCount + classCount + methodCount + fieldCount) * 4);

    pDvmDex->pInterfaceCache = dvmAllocAtomicCache(DEX_INTERFACE_CACHE_SIZE);

    if (pDvmDex->pResStrings == NULL ||
        pDvmDex->pResClasses == NULL ||
        pDvmDex->pResMethods == NULL ||
        pDvmDex->pResFields == NULL ||
        pDvmDex->pInterfaceCache == NULL)
    {
        LOGE("Alloc failure in allocateAuxStructures\n");
        free(pDvmDex->pResStrings);
        free(pDvmDex->pResClasses);
        free(pDvmDex->pResMethods);
        free(pDvmDex->pResFields);
        free(pDvmDex);
        return NULL;
    }

    return pDvmDex;

}

/*
 * Given an open optimized DEX file, map it into read-only shared memory and
 * parse the contents.
 *
 * Returns nonzero on error.
 */
int dvmDexFileOpenFromFd(int fd, DvmDex** ppDvmDex)
{
    DvmDex* pDvmDex;
    DexFile* pDexFile;
    MemMapping memMap;
    int parseFlags = kDexParseDefault;
    int result = -1;

    if (gDvm.verifyDexChecksum)
        parseFlags |= kDexParseVerifyChecksum;

    if (lseek(fd, 0, SEEK_SET) < 0) {
        LOGE("lseek rewind failed\n");
        goto bail;
    }

    if (sysMapFileInShmem(fd, &memMap) != 0) {
        LOGE("Unable to map file\n");
        goto bail;
    }

    pDexFile = dexFileParse(memMap.addr, memMap.length, parseFlags);
    if (pDexFile == NULL) {
        LOGE("DEX parse failed\n");
        sysReleaseShmem(&memMap);
        goto bail;
    }

    pDvmDex = allocateAuxStructures(pDexFile);
    if (pDvmDex == NULL) {
        dexFileFree(pDexFile);
        sysReleaseShmem(&memMap);
        goto bail;
    }

    /* tuck this into the DexFile so it gets released later */
    sysCopyMap(&pDvmDex->memMap, &memMap);
    *ppDvmDex = pDvmDex;
    result = 0;

bail:
    return result;
}

/*
 * Create a DexFile structure for a "partial" DEX.  This is one that is in
 * the process of being optimized.  The optimization header isn't finished
 * and we won't have any of the auxillary data tables, so we have to do
 * the initialization slightly differently.
 *
 * Returns nonzero on error.
 */
int dvmDexFileOpenPartial(const void* addr, int len, DvmDex** ppDvmDex)
{
    DvmDex* pDvmDex;
    DexFile* pDexFile;
    int parseFlags = kDexParseDefault;
    int result = -1;

    if (gDvm.verifyDexChecksum)
        parseFlags |= kDexParseVerifyChecksum;

    pDexFile = dexFileParse(addr, len, parseFlags);
    if (pDexFile == NULL) {
        LOGE("DEX parse failed\n");
        goto bail;
    }
    pDvmDex = allocateAuxStructures(pDexFile);
    if (pDvmDex == NULL) {
        dexFileFree(pDexFile);
        goto bail;
    }

    *ppDvmDex = pDvmDex;
    result = 0;

bail:
    return result;
}

/*
 * Free up the DexFile and any associated data structures.
 *
 * Note we may be called with a partially-initialized DvmDex.
 */
void dvmDexFileFree(DvmDex* pDvmDex)
{
    if (pDvmDex == NULL)
        return;

    dexFileFree(pDvmDex->pDexFile);

    LOGV("+++ DEX %p: freeing aux structs\n", pDvmDex);
    free(pDvmDex->pResStrings);
    free(pDvmDex->pResClasses);
    free(pDvmDex->pResMethods);
    free(pDvmDex->pResFields);
    dvmFreeAtomicCache(pDvmDex->pInterfaceCache);

    sysReleaseShmem(&pDvmDex->memMap);
    free(pDvmDex);
}

