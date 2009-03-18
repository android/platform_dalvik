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
 * Access the contents of a .dex file.
 */

#include "DexFile.h"
#include "DexProto.h"
#include "Leb128.h"
#include "sha1.h"
#include "ZipArchive.h"

#include <zlib.h>

#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>

/*
 * Verifying checksums is good, but it slows things down and causes us to
 * touch every page.  In the "optimized" world, it doesn't work at all,
 * because we rewrite the contents.
 */
static const bool kVerifyChecksum = false;
static const bool kVerifySignature = false;


/* Compare two '\0'-terminated modified UTF-8 strings, using Unicode
 * code point values for comparison. This treats different encodings
 * for the same code point as equivalent, except that only a real '\0'
 * byte is considered the string terminator. The return value is as
 * for strcmp(). */
int dexUtf8Cmp(const char* s1, const char* s2) {
    for (;;) {
        if (*s1 == '\0') {
            if (*s2 == '\0') {
                return 0;
            }
            return -1;
        } else if (*s2 == '\0') {
            return 1;
        }

        int utf1 = dexGetUtf16FromUtf8(&s1);
        int utf2 = dexGetUtf16FromUtf8(&s2);
        int diff = utf1 - utf2;

        if (diff != 0) {
            return diff;
        }
    }
}

/* for dexIsValidMemberNameUtf8(), a bit vector indicating valid low ascii */
u4 DEX_MEMBER_VALID_LOW_ASCII[4] = {
    0x00000000, // 00..1f low control characters; nothing valid
    0x03ff2010, // 20..3f digits and symbols; valid: '0'..'9', '$', '-'
    0x87fffffe, // 40..5f uppercase etc.; valid: 'A'..'Z', '_'
    0x07fffffe  // 60..7f lowercase etc.; valid: 'a'..'z'
};

/* Helper for dexIsValidMemberNameUtf8(); do not call directly. */
bool dexIsValidMemberNameUtf8_0(const char** pUtf8Ptr) {
    /*
     * It's a multibyte encoded character. Decode it and analyze. We
     * accept anything that isn't (a) an improperly encoded low value,
     * (b) an improper surrogate pair, (c) an encoded '\0', (d) a high
     * control character, or (e) a high space, layout, or special
     * character (U+00a0, U+2000..U+200f, U+2028..U+202f,
     * U+fff0..U+ffff).
     */
    
    u2 utf16 = dexGetUtf16FromUtf8(pUtf8Ptr);

    // Perform follow-up tests based on the high 8 bits.
    switch (utf16 >> 8) {
        case 0x00: {
            // It's only valid if it's above the ISO-8859-1 high space (0xa0).
            return (utf16 > 0x00a0);
        }
        case 0xd8:
        case 0xd9:
        case 0xda:
        case 0xdb: {
            /*
             * It's a leading surrogate. Check to see that a trailing
             * surrogate follows.
             */
            utf16 = dexGetUtf16FromUtf8(pUtf8Ptr);
            return (utf16 >= 0xdc00) && (utf16 <= 0xdfff);
        }
        case 0xdc:
        case 0xdd:
        case 0xde:
        case 0xdf: {
            // It's a trailing surrogate, which is not valid at this point.
            return false;
        }
        case 0x20:
        case 0xff: {
            // It's in the range that has spaces, controls, and specials.
            switch (utf16 & 0xfff8) {
                case 0x2000:
                case 0x2008:
                case 0x2028:
                case 0xfff0:
                case 0xfff8: {
                    return false;
                }
            }
            break;
        }
    }

    return true;
}

/* Return whether the given string is a valid field or method name. */
bool dexIsValidMemberName(const char* s) {
    bool angleName = false;
    
    switch (*s) {
        case '\0': {
            // The empty string is not a valid name.
            return false;
        }
        case '<': {
            /*
             * '<' is allowed only at the start of a name, and if present,
             * means that the name must end with '>'.
             */
            angleName = true;
            s++;
            break;
        }
    }

    for (;;) {
        switch (*s) {
            case '\0': {
                return !angleName;
            }
            case '>': {
                return angleName && s[1] == '\0';
            }
        }
        if (!dexIsValidMemberNameUtf8(&s)) {
            return false;
        }
    }
}

/* Return whether the given string is a valid type descriptor. */
bool dexIsValidTypeDescriptor(const char* s) {
    int arrayCount = 0;

    while (*s == '[') {
        arrayCount++;
        s++;
    }

    if (arrayCount > 255) {
        // Arrays may have no more than 255 dimensions.
        return false;
    }
    
    switch (*(s++)) {
        case 'B': 
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'S':
        case 'Z': {
            // These are all single-character descriptors for primitive types.
            return (*s == '\0');
        }
        case 'V': {
            // You can't have an array of void.
            return (arrayCount == 0) && (*s == '\0');
        }
        case 'L': {
            // Break out and continue below.
            break;
        }
        default: {
            // Oddball descriptor character.
            return false;
        }
    }

    // We just consumed the 'L' that introduces a class name.

    bool slashOrFirst = true; // first character or just encountered a slash
    for (;;) {
        u1 c = (u1) *s;
        switch (c) {
            case '\0': {
                // Premature end.
                return false;
            }
            case ';': {
                /*
                 * Make sure that this is the end of the string and that
                 * it doesn't end with an empty component (including the
                 * degenerate case of "L;").
                 */
                return (s[1] == '\0') && !slashOrFirst;
            }
            case '/': {
                if (slashOrFirst) {
                    // Slash at start or two slashes in a row.
                    return false;
                }
                slashOrFirst = true;
                s++;
                break;
            }
            default: {
                if (!dexIsValidMemberNameUtf8(&s)) {
                    return false;
                }
                slashOrFirst = false;
                break;
            }
        }
    }
}

/* Return whether the given string is a valid reference descriptor. This
 * is true if dexIsValidTypeDescriptor() returns true and the descriptor
 * is for a class or array and not a primitive type. */
bool dexIsReferenceDescriptor(const char* s) {
    if (!dexIsValidTypeDescriptor(s)) {
        return false;
    }

    return (s[0] == 'L') || (s[0] == '[');
}

/* Return whether the given string is a valid class descriptor. This
 * is true if dexIsValidTypeDescriptor() returns true and the descriptor
 * is for a class and not an array or primitive type. */
bool dexIsClassDescriptor(const char* s) {
    if (!dexIsValidTypeDescriptor(s)) {
        return false;
    }

    return s[0] == 'L';
}

/* Return whether the given string is a valid field type descriptor. This
 * is true if dexIsValidTypeDescriptor() returns true and the descriptor
 * is for anything but "void". */
bool dexIsFieldDescriptor(const char* s) {
    if (!dexIsValidTypeDescriptor(s)) {
        return false;
    }

    return s[0] != 'V';
}

/* Return the UTF-8 encoded string with the specified string_id index,
 * also filling in the UTF-16 size (number of 16-bit code points).*/
const char* dexStringAndSizeById(const DexFile* pDexFile, u4 idx,
        u4* utf16Size) {
    const DexStringId* pStringId = dexGetStringId(pDexFile, idx);
    const u1* ptr = pDexFile->baseAddr + pStringId->stringDataOff;

    *utf16Size = readUnsignedLeb128(&ptr);
    return (const char*) ptr;
}

/*
 * Format an SHA-1 digest for printing.  tmpBuf must be able to hold at
 * least kSHA1DigestOutputLen bytes.
 */
const char* dvmSHA1DigestToStr(const unsigned char digest[], char* tmpBuf);

/*
 * Compute a SHA-1 digest on a range of bytes.
 */
static void dexComputeSHA1Digest(const unsigned char* data, size_t length,
    unsigned char digest[])
{
    SHA1_CTX context;
    SHA1Init(&context);
    SHA1Update(&context, data, length);
    SHA1Final(digest, &context);
}

/*
 * Format the SHA-1 digest into the buffer, which must be able to hold at
 * least kSHA1DigestOutputLen bytes.  Returns a pointer to the buffer,
 */
static const char* dexSHA1DigestToStr(const unsigned char digest[],char* tmpBuf)
{
    static const char hexDigit[] = "0123456789abcdef";
    char* cp;
    int i;

    cp = tmpBuf;
    for (i = 0; i < kSHA1DigestLen; i++) {
        *cp++ = hexDigit[digest[i] >> 4];
        *cp++ = hexDigit[digest[i] & 0x0f];
    }
    *cp++ = '\0';

    assert(cp == tmpBuf + kSHA1DigestOutputLen);

    return tmpBuf;
}

/*
 * Compute a hash code on a UTF-8 string, for use with internal hash tables.
 *
 * This may or may not be compatible with UTF-8 hash functions used inside
 * the Dalvik VM.
 *
 * The basic "multiply by 31 and add" approach does better on class names
 * than most other things tried (e.g. adler32).
 */
static u4 classDescriptorHash(const char* str)
{
    u4 hash = 1;

    while (*str != '\0')
        hash = hash * 31 + *str++;

    return hash;
}

/*
 * Add an entry to the class lookup table.  We hash the string and probe
 * until we find an open slot.
 */
static void classLookupAdd(DexFile* pDexFile, DexClassLookup* pLookup,
    int stringOff, int classDefOff, int* pNumProbes)
{
    const char* classDescriptor =
        (const char*) (pDexFile->baseAddr + stringOff);
    const DexClassDef* pClassDef =
        (const DexClassDef*) (pDexFile->baseAddr + classDefOff);
    u4 hash = classDescriptorHash(classDescriptor);
    int mask = pLookup->numEntries-1;
    int idx = hash & mask;

    /*
     * Find the first empty slot.  We oversized the table, so this is
     * guaranteed to finish.
     */
    int probes = 0;
    while (pLookup->table[idx].classDescriptorOffset != 0) {
        idx = (idx + 1) & mask;
        probes++;
    }
    //if (probes > 1)
    //    LOGW("classLookupAdd: probes=%d\n", probes);

    pLookup->table[idx].classDescriptorHash = hash;
    pLookup->table[idx].classDescriptorOffset = stringOff;
    pLookup->table[idx].classDefOffset = classDefOff;
    *pNumProbes = probes;
}

/*
 * Round up to the next highest power of 2.
 *
 * Found on http://graphics.stanford.edu/~seander/bithacks.html.
 */
u4 dexRoundUpPower2(u4 val)
{
    val--;
    val |= val >> 1;
    val |= val >> 2;
    val |= val >> 4;
    val |= val >> 8;
    val |= val >> 16;
    val++;

    return val;
}

/*
 * Create the class lookup hash table.
 *
 * Returns newly-allocated storage.
 */
DexClassLookup* dexCreateClassLookup(DexFile* pDexFile)
{
    DexClassLookup* pLookup;
    int allocSize;
    int i, numEntries;
    int numProbes, totalProbes, maxProbes;

    numProbes = totalProbes = maxProbes = 0;

    assert(pDexFile != NULL);

    /*
     * Using a factor of 3 results in far less probing than a factor of 2,
     * but almost doubles the flash storage requirements for the bootstrap
     * DEX files.  The overall impact on class loading performance seems
     * to be minor.  We could probably get some performance improvement by
     * using a secondary hash.
     */
    numEntries = dexRoundUpPower2(pDexFile->pHeader->classDefsSize * 2);
    allocSize = offsetof(DexClassLookup, table)
                    + numEntries * sizeof(pLookup->table[0]);

    pLookup = (DexClassLookup*) calloc(1, allocSize);
    if (pLookup == NULL)
        return NULL;
    pLookup->size = allocSize;
    pLookup->numEntries = numEntries;

    for (i = 0; i < (int)pDexFile->pHeader->classDefsSize; i++) {
        const DexClassDef* pClassDef;
        const char* pString;

        pClassDef = dexGetClassDef(pDexFile, i);
        pString = dexStringByTypeIdx(pDexFile, pClassDef->classIdx);

        classLookupAdd(pDexFile, pLookup, 
            (u1*)pString - pDexFile->baseAddr,
            (u1*)pClassDef - pDexFile->baseAddr, &numProbes);

        if (numProbes > maxProbes)
            maxProbes = numProbes;
        totalProbes += numProbes;
    }

    LOGV("Class lookup: classes=%d slots=%d (%d%% occ) alloc=%d"
         " total=%d max=%d\n",
        pDexFile->pHeader->classDefsSize, numEntries,
        (100 * pDexFile->pHeader->classDefsSize) / numEntries,
        allocSize, totalProbes, maxProbes);

    return pLookup;
}


/*
 * Set up the basic raw data pointers of a DexFile. This function isn't
 * meant for general use.
 */
void dexFileSetupBasicPointers(DexFile* pDexFile, const u1* data) {
    DexHeader *pHeader = (DexHeader*) data;

    pDexFile->baseAddr = data;
    pDexFile->pHeader = pHeader;
    pDexFile->pStringIds = (const DexStringId*) (data + pHeader->stringIdsOff);
    pDexFile->pTypeIds = (const DexTypeId*) (data + pHeader->typeIdsOff);
    pDexFile->pFieldIds = (const DexFieldId*) (data + pHeader->fieldIdsOff);
    pDexFile->pMethodIds = (const DexMethodId*) (data + pHeader->methodIdsOff);
    pDexFile->pProtoIds = (const DexProtoId*) (data + pHeader->protoIdsOff);
    pDexFile->pClassDefs = (const DexClassDef*) (data + pHeader->classDefsOff);
    pDexFile->pLinkData = (const DexLink*) (data + pHeader->linkOff);
}


/*
 * Parse out an index map entry, advancing "*pData" and reducing "*pSize".
 */
static bool parseIndexMapEntry(const u1** pData, u4* pSize, bool expanding,
    u4* pFullCount, u4* pReducedCount, const u2** pMap)
{
    const u4* wordPtr = (const u4*) *pData;
    u4 size = *pSize;
    u4 mapCount;

    if (expanding) {
        if (size < 4)
            return false;
        mapCount = *pReducedCount = *wordPtr++;
        *pFullCount = (u4) -1;
        size -= sizeof(u4);
    } else {
        if (size < 8)
            return false;
        mapCount = *pFullCount = *wordPtr++;
        *pReducedCount = *wordPtr++;
        size -= sizeof(u4) * 2;
    }

    u4 mapSize = mapCount * sizeof(u2);

    if (size < mapSize)
        return false;
    *pMap = (const u2*) wordPtr;
    size -= mapSize;

    /* advance the pointer */
    const u1* ptr = (const u1*) wordPtr;
    ptr += (mapSize + 3) & ~0x3;

    /* update pass-by-reference values */
    *pData = (const u1*) ptr;
    *pSize = size;

    return true;
}

/*
 * Set up some pointers into the mapped data.
 *
 * See analysis/ReduceConstants.c for the data layout description.
 */
static bool parseIndexMap(DexFile* pDexFile, const u1* data, u4 size,
    bool expanding)
{
    if (!parseIndexMapEntry(&data, &size, expanding,
            &pDexFile->indexMap.classFullCount,
            &pDexFile->indexMap.classReducedCount,
            &pDexFile->indexMap.classMap))
    {
        return false;
    }

    if (!parseIndexMapEntry(&data, &size, expanding,
            &pDexFile->indexMap.methodFullCount,
            &pDexFile->indexMap.methodReducedCount,
            &pDexFile->indexMap.methodMap))
    {
        return false;
    }

    if (!parseIndexMapEntry(&data, &size, expanding,
            &pDexFile->indexMap.fieldFullCount,
            &pDexFile->indexMap.fieldReducedCount,
            &pDexFile->indexMap.fieldMap))
    {
        return false;
    }

    if (!parseIndexMapEntry(&data, &size, expanding,
            &pDexFile->indexMap.stringFullCount,
            &pDexFile->indexMap.stringReducedCount,
            &pDexFile->indexMap.stringMap))
    {
        return false;
    }

    if (expanding) {
        /*
         * The map includes the "reduced" counts; pull the original counts
         * out of the DexFile so that code has a consistent source.
         */
        assert(pDexFile->indexMap.classFullCount == (u4) -1);
        assert(pDexFile->indexMap.methodFullCount == (u4) -1);
        assert(pDexFile->indexMap.fieldFullCount == (u4) -1);
        assert(pDexFile->indexMap.stringFullCount == (u4) -1);

#if 0   // TODO: not available yet -- do later or just skip this
        pDexFile->indexMap.classFullCount =
            pDexFile->pHeader->typeIdsSize;
        pDexFile->indexMap.methodFullCount =
            pDexFile->pHeader->methodIdsSize;
        pDexFile->indexMap.fieldFullCount =
            pDexFile->pHeader->fieldIdsSize;
        pDexFile->indexMap.stringFullCount =
            pDexFile->pHeader->stringIdsSize;
#endif
    }

    LOGI("Class : %u %u %u\n",
        pDexFile->indexMap.classFullCount,
        pDexFile->indexMap.classReducedCount,
        pDexFile->indexMap.classMap[0]);
    LOGI("Method: %u %u %u\n",
        pDexFile->indexMap.methodFullCount,
        pDexFile->indexMap.methodReducedCount,
        pDexFile->indexMap.methodMap[0]);
    LOGI("Field : %u %u %u\n",
        pDexFile->indexMap.fieldFullCount,
        pDexFile->indexMap.fieldReducedCount,
        pDexFile->indexMap.fieldMap[0]);
    LOGI("String: %u %u %u\n",
        pDexFile->indexMap.stringFullCount,
        pDexFile->indexMap.stringReducedCount,
        pDexFile->indexMap.stringMap[0]);

    return true;
}

/*
 * Parse some auxillary data tables.
 *
 * v1.0 wrote a zero in the first 32 bits, followed by the DexClassLookup
 * table.  Subsequent versions switched to the "chunk" format.
 */
static bool parseAuxData(const u1* data, DexFile* pDexFile)
{
    const u4* pAux = (const u4*) (data + pDexFile->pOptHeader->auxOffset);
    u4 indexMapType = 0;

    /* v1.0 format? */
    if (*pAux == 0) {
        LOGV("+++ found OLD dex format\n");
        pDexFile->pClassLookup = (const DexClassLookup*) (pAux+1);
        return true;
    }
    LOGV("+++ found NEW dex format\n");

    /* process chunks until we see the end marker */
    while (*pAux != kDexChunkEnd) {
        u4 size = *(pAux+1);
        u1* data = (u1*) (pAux + 2);

        switch (*pAux) {
        case kDexChunkClassLookup:
            pDexFile->pClassLookup = (const DexClassLookup*) data;
            break;
        case kDexChunkReducingIndexMap:
            LOGI("+++ found reducing index map, size=%u\n", size);
            if (!parseIndexMap(pDexFile, data, size, false)) {
                LOGE("Failed parsing reducing index map\n");
                return false;
            }
            indexMapType = *pAux;
            break;
        case kDexChunkExpandingIndexMap:
            LOGI("+++ found expanding index map, size=%u\n", size);
            if (!parseIndexMap(pDexFile, data, size, true)) {
                LOGE("Failed parsing expanding index map\n");
                return false;
            }
            indexMapType = *pAux;
            break;
        default:
            LOGI("Unknown chunk 0x%08x (%c%c%c%c), size=%d in aux data area\n",
                *pAux,
                (char) ((*pAux) >> 24), (char) ((*pAux) >> 16),
                (char) ((*pAux) >> 8),  (char)  (*pAux),
                size);
            break;
        }

        /*
         * Advance pointer, padding to 64-bit boundary.  The extra "+8" is
         * for the type/size header.
         */
        size = (size + 8 + 7) & ~7;
        pAux += size / sizeof(u4);
    }

#if 0   // TODO: propagate expected map type from the VM through the API
    /*
     * If we're configured to expect an index map, and we don't find one,
     * reject this DEX so we'll regenerate it.  Also, if we found an
     * "expanding" map but we're not configured to use it, we have to fail
     * because the constants aren't usable without translation.
     */
    if (indexMapType != expectedIndexMapType) {
        LOGW("Incompatible index map configuration: found 0x%04x, need %d\n",
            indexMapType, DVM_REDUCE_CONSTANTS);
        return false;
    }
#endif

    return true;
}

/*
 * Parse an optimized or unoptimized .dex file sitting in memory.  This is
 * called after the byte-ordering and structure alignment has been fixed up.
 *
 * On success, return a newly-allocated DexFile.
 */
DexFile* dexFileParse(const u1* data, size_t length, int flags)
{
    DexFile* pDexFile = NULL;
    const DexHeader* pHeader;
    const u1* magic;
    int result = -1;

    if (length < sizeof(DexHeader)) {
        LOGE("too short to be a valid .dex\n");
        goto bail;      /* bad file format */
    }

    pDexFile = (DexFile*) malloc(sizeof(DexFile));
    if (pDexFile == NULL)
        goto bail;      /* alloc failure */
    memset(pDexFile, 0, sizeof(DexFile));

    /*
     * Peel off the optimized header.
     */
    if (memcmp(data, DEX_OPT_MAGIC, 4) == 0) {
        magic = data;
        if (memcmp(magic+4, DEX_OPT_MAGIC_VERS, 4) != 0) {
            LOGE("bad opt version (0x%02x %02x %02x %02x)\n",
                 magic[4], magic[5], magic[6], magic[7]);
            goto bail;
        }

        pDexFile->pOptHeader = (const DexOptHeader*) data;
        LOGV("Good opt header, DEX offset is %d, flags=0x%02x\n",
            pDexFile->pOptHeader->dexOffset, pDexFile->pOptHeader->flags);

        /* locate some auxillary data tables */
        if (!parseAuxData(data, pDexFile))
            goto bail;

        /* ignore the opt header and appended data from here on out */
        data += pDexFile->pOptHeader->dexOffset;
        length -= pDexFile->pOptHeader->dexOffset;
        if (pDexFile->pOptHeader->dexLength > length) {
            LOGE("File truncated? stored len=%d, rem len=%d\n",
                pDexFile->pOptHeader->dexLength, (int) length);
            goto bail;
        }
        length = pDexFile->pOptHeader->dexLength;
    }

    dexFileSetupBasicPointers(pDexFile, data);
    pHeader = pDexFile->pHeader;

    magic = pHeader->magic;
    if (memcmp(magic, DEX_MAGIC, 4) != 0) {
        /* not expected */
        LOGE("bad magic number (0x%02x %02x %02x %02x)\n",
             magic[0], magic[1], magic[2], magic[3]);
        goto bail;
    }
    if (memcmp(magic+4, DEX_MAGIC_VERS, 4) != 0) {
        LOGE("bad dex version (0x%02x %02x %02x %02x)\n",
             magic[4], magic[5], magic[6], magic[7]);
        goto bail;
    }

    /*
     * Verify the checksum.  This is reasonably quick, but does require
     * touching every byte in the DEX file.  The checksum changes after
     * byte-swapping and DEX optimization.
     */
    if (flags & kDexParseVerifyChecksum) {
        u4 adler = dexComputeChecksum(pHeader);
        if (adler != pHeader->checksum) {
            LOGE("ERROR: bad checksum (%08x vs %08x)\n",
                adler, pHeader->checksum);
            if (!(flags & kDexParseContinueOnError))
                goto bail;
        } else {
            LOGV("+++ adler32 checksum (%08x) verified\n", adler);
        }
    }

    /*
     * Verify the SHA-1 digest.  (Normally we don't want to do this --
     * the digest is used to uniquely identify a DEX file, and can't be
     * computed post-optimization.)
     *
     * The digest will be invalid after byte swapping and DEX optimization.
     */
    if (kVerifySignature) {
        unsigned char sha1Digest[kSHA1DigestLen];
        const int nonSum = sizeof(pHeader->magic) + sizeof(pHeader->checksum) +
                            kSHA1DigestLen;

        dexComputeSHA1Digest(data + nonSum, length - nonSum, sha1Digest);
        if (memcmp(sha1Digest, pHeader->signature, kSHA1DigestLen) != 0) {
            char tmpBuf1[kSHA1DigestOutputLen];
            char tmpBuf2[kSHA1DigestOutputLen];
            LOGE("ERROR: bad SHA1 digest (%s vs %s)\n",
                dexSHA1DigestToStr(sha1Digest, tmpBuf1),
                dexSHA1DigestToStr(pHeader->signature, tmpBuf2));
            if (!(flags & kDexParseContinueOnError))
                goto bail;
        } else {
            LOGV("+++ sha1 digest verified\n");
        }
    }

    if (pHeader->fileSize != length) {
        LOGE("ERROR: stored file size (%d) != expected (%d)\n",
            (int) pHeader->fileSize, (int) length);
        if (!(flags & kDexParseContinueOnError))
            goto bail;
    }

    if (pHeader->classDefsSize == 0) {
        LOGE("ERROR: DEX file has no classes in it, failing\n");
        goto bail;
    }

    /*
     * Success!
     */
    result = 0;

bail:
    if (result != 0 && pDexFile != NULL) {
        dexFileFree(pDexFile);
        pDexFile = NULL;
    }
    return pDexFile;
}

/*
 * Free up the DexFile and any associated data structures.
 *
 * Note we may be called with a partially-initialized DexFile.
 */
void dexFileFree(DexFile* pDexFile)
{
    if (pDexFile == NULL)
        return;

    free(pDexFile);
}

/*
 * Look up a class definition entry by descriptor.
 *
 * "descriptor" should look like "Landroid/debug/Stuff;".
 */
const DexClassDef* dexFindClass(const DexFile* pDexFile,
    const char* descriptor)
{
    const DexClassLookup* pLookup = pDexFile->pClassLookup;
    u4 hash;
    int idx, mask;

    hash = classDescriptorHash(descriptor);
    mask = pLookup->numEntries - 1;
    idx = hash & mask;

    /*
     * Search until we find a matching entry or an empty slot.
     */
    while (true) {
        int offset;

        offset = pLookup->table[idx].classDescriptorOffset;
        if (offset == 0)
            return NULL;

        if (pLookup->table[idx].classDescriptorHash == hash) {
            const char* str;
        
            str = (const char*) (pDexFile->baseAddr + offset);
            if (strcmp(str, descriptor) == 0) {
                return (const DexClassDef*)
                    (pDexFile->baseAddr + pLookup->table[idx].classDefOffset);
            }
        }

        idx = (idx + 1) & mask;
    }
}


/*
 * Compute the DEX file checksum for a memory-mapped DEX file.
 */
u4 dexComputeChecksum(const DexHeader* pHeader)
{
    const u1* start = (const u1*) pHeader;

    uLong adler = adler32(0L, Z_NULL, 0);
    const int nonSum = sizeof(pHeader->magic) + sizeof(pHeader->checksum);

    return (u4) adler32(adler, start + nonSum, pHeader->fileSize - nonSum);
}


/*
 * ===========================================================================
 *      Debug info
 * ===========================================================================
 */

/*
 * Decode the arguments in a method signature, which looks something
 * like "(ID[Ljava/lang/String;)V".
 *
 * Returns the type signature letter for the next argument, or ')' if
 * there are no more args.  Advances "pSig" to point to the character
 * after the one returned.
 */
static char decodeSignature(const char** pSig)
{
    const char* sig = *pSig;

    if (*sig == '(')
        sig++;

    if (*sig == 'L') {
        /* object ref */
        while (*++sig != ';')
            ;
        *pSig = sig+1;
        return 'L';
    }
    if (*sig == '[') {
        /* array; advance past array type */
        while (*++sig == '[')
            ;
        if (*sig == 'L') {
            while (*++sig != ';')
                ;
        }
        *pSig = sig+1;
        return '[';
    }
    if (*sig == '\0')
        return *sig;        /* don't advance further */

    *pSig = sig+1;
    return *sig;
}

/*
 * returns the length of a type string, given the start of the
 * type string. Used for the case where the debug info format
 * references types that are inside a method type signature.
 */
static int typeLength (const char *type) {
    // Assumes any leading '(' has already been gobbled
    const char *end = type;
    decodeSignature(&end);
    return end - type;
}

/*
 * Reads a string index as encoded for the debug info format,
 * returning a string pointer or NULL as appropriate.
 */
static const char* readStringIdx(const DexFile* pDexFile, 
        const u1** pStream) {
    u4 stringIdx = readUnsignedLeb128(pStream);

    // Remember, encoded string indicies have 1 added to them.
    if (stringIdx == 0) {
        return NULL;
    } else {
        return dexStringById(pDexFile, stringIdx - 1);
    }
}

/*
 * Reads a type index as encoded for the debug info format, returning
 * a string pointer for its descriptor or NULL as appropriate.
 */
static const char* readTypeIdx(const DexFile* pDexFile, 
        const u1** pStream) {
    u4 typeIdx = readUnsignedLeb128(pStream);

    // Remember, encoded type indicies have 1 added to them.
    if (typeIdx == 0) {
        return NULL;
    } else {
        return dexStringByTypeIdx(pDexFile, typeIdx - 1);
    }
}

/* access_flag value indicating that a method is static */
#define ACC_STATIC              0x0008

typedef struct LocalInfo {
    const char *name;
    const char *descriptor;
    const char *signature;
    u2 startAddress;
    bool live;
} LocalInfo;

static void emitLocalCbIfLive (void *cnxt, int reg, u4 endAddress, 
        LocalInfo *localInReg, DexDebugNewLocalCb localCb)
{
    if (localCb != NULL && localInReg[reg].live) {
        localCb(cnxt, reg, localInReg[reg].startAddress, endAddress,
                localInReg[reg].name, 
                localInReg[reg].descriptor, 
                localInReg[reg].signature == NULL 
                ? "" : localInReg[reg].signature );
    }
}

// TODO optimize localCb == NULL case
void dexDecodeDebugInfo(
            const DexFile* pDexFile,
            const DexCode* pCode,
            const char* classDescriptor,
            u4 protoIdx,
            u4 accessFlags,
            DexDebugNewPositionCb posCb, DexDebugNewLocalCb localCb,
            void* cnxt)
{
    const u1 *stream = dexGetDebugInfoStream(pDexFile, pCode);
    u4 line;
    u4 parametersSize;
    u4 address = 0;
    LocalInfo localInReg[pCode->registersSize];
    u4 insnsSize = pCode->insnsSize;
    DexProto proto = { pDexFile, protoIdx };

    memset(localInReg, 0, sizeof(LocalInfo) * pCode->registersSize);

    if (stream == NULL) {
        goto end;
    }

    line = readUnsignedLeb128(&stream);
    parametersSize = readUnsignedLeb128(&stream);

    u2 argReg = pCode->registersSize - pCode->insSize;

    if ((accessFlags & ACC_STATIC) == 0) {
        /*
         * The code is an instance method, which means that there is
         * an initial this parameter. Also, the proto list should
         * contain exactly one fewer argument word than the insSize
         * indicates.
         */
        assert(pCode->insSize == (dexProtoComputeArgsSize(&proto) + 1));
        localInReg[argReg].name = "this";
        localInReg[argReg].descriptor = classDescriptor;
        localInReg[argReg].startAddress = 0;
        localInReg[argReg].live = true;
        argReg++;
    } else {
        assert(pCode->insSize == dexProtoComputeArgsSize(&proto));
    }
    
    DexParameterIterator iterator;
    dexParameterIteratorInit(&iterator, &proto);

    while (parametersSize-- != 0) {
        const char* descriptor = dexParameterIteratorNextDescriptor(&iterator);
        const char *name;
        int reg;
        
        if ((argReg >= pCode->registersSize) || (descriptor == NULL)) {
            goto invalid_stream;
        }

        name = readStringIdx(pDexFile, &stream);
        reg = argReg;

        switch (descriptor[0]) {
            case 'D':
            case 'J':
                argReg += 2;
                break;
            default:
                argReg += 1;
                break;
        }

        if (name != NULL) {
            localInReg[reg].name = name;
            localInReg[reg].descriptor = descriptor;
            localInReg[reg].signature = NULL;
            localInReg[reg].startAddress = address;
            localInReg[reg].live = true;
        }
    }

    for (;;)  {
        u1 opcode = *stream++;
        u2 reg;

        switch (opcode) {
            case DBG_END_SEQUENCE:
                goto end;

            case DBG_ADVANCE_PC:
                address += readUnsignedLeb128(&stream);
                break;
                
            case DBG_ADVANCE_LINE:
                line += readSignedLeb128(&stream);
                break;

            case DBG_START_LOCAL:
            case DBG_START_LOCAL_EXTENDED:
                reg = readUnsignedLeb128(&stream);
                if (reg > pCode->registersSize) goto invalid_stream;

                // Emit what was previously there, if anything
                emitLocalCbIfLive (cnxt, reg, address, 
                    localInReg, localCb);

                localInReg[reg].name = readStringIdx(pDexFile, &stream);
                localInReg[reg].descriptor = readTypeIdx(pDexFile, &stream);
                if (opcode == DBG_START_LOCAL_EXTENDED) {
                    localInReg[reg].signature 
                        = readStringIdx(pDexFile, &stream);
                } else {
                    localInReg[reg].signature = NULL;
                }
                localInReg[reg].startAddress = address;
                localInReg[reg].live = true;
                break;

            case DBG_END_LOCAL:
                reg = readUnsignedLeb128(&stream);
                if (reg > pCode->registersSize) goto invalid_stream;

                emitLocalCbIfLive (cnxt, reg, address, localInReg, localCb);
                localInReg[reg].live = false;
                break;

            case DBG_RESTART_LOCAL:
                reg = readUnsignedLeb128(&stream);
                if (reg > pCode->registersSize) goto invalid_stream;

                if (localInReg[reg].name == NULL 
                        || localInReg[reg].descriptor == NULL) {
                    goto invalid_stream;
                }

                /*
                 * If the register is live, the "restart" is superfluous,
                 * and we don't want to mess with the existing start address.
                 */
                if (!localInReg[reg].live) {
                    localInReg[reg].startAddress = address;
                    localInReg[reg].live = true;
                }
                break;

            case DBG_SET_PROLOGUE_END:
            case DBG_SET_EPILOGUE_BEGIN:
            case DBG_SET_FILE:
                break;

            default: {
                int adjopcode = opcode - DBG_FIRST_SPECIAL;

                address += adjopcode / DBG_LINE_RANGE;
                line += DBG_LINE_BASE + (adjopcode % DBG_LINE_RANGE);

                if (posCb != NULL) {
                    int done; 
                    done = posCb(cnxt, address, line);

                    if (done) {
                        // early exit
                        goto end;
                    }
                }
                break;
            }
        }
    }

end:
    {
        int reg;
        for (reg = 0; reg < pCode->registersSize; reg++) {
            emitLocalCbIfLive (cnxt, reg, insnsSize, localInReg, localCb);
        }
    }
    return;

invalid_stream:
    IF_LOGE() {
        char* methodDescriptor = dexProtoCopyMethodDescriptor(&proto);
        LOGE("Invalid debug info stream. class %s; proto %s",
                classDescriptor, methodDescriptor);
        free(methodDescriptor);
    }
}
