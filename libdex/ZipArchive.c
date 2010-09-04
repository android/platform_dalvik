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
 * Read-only access to Zip archives, with minimal heap allocation.
 */
#include "ZipArchive.h"

#include <zlib.h>

#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>


/*
 * Zip file constants.
 */
#define kEOCDSignature      0x06054b50
#define kEOCDLen            22
#define kEOCDNumEntries     8               // offset to #of entries in file
#define kEOCDFileOffset     16              // offset to central directory

#define kMaxCommentLen      65535           // longest possible in ushort
#define kMaxEOCDSearch      (kMaxCommentLen + kEOCDLen)

#define kLFHSignature       0x04034b50
#define kLFHLen             30              // excluding variable-len fields
#define kLFHNameLen         26              // offset to filename length
#define kLFHExtraLen        28              // offset to extra length

#define kCDESignature       0x02014b50
#define kCDELen             46              // excluding variable-len fields
#define kCDEMethod          10              // offset to compression method
#define kCDEModWhen         12              // offset to modification timestamp
#define kCDECRC             16              // offset to entry CRC
#define kCDECompLen         20              // offset to compressed length
#define kCDEUncompLen       24              // offset to uncompressed length
#define kCDENameLen         28              // offset to filename length
#define kCDEExtraLen        30              // offset to extra length
#define kCDECommentLen      32              // offset to comment length
#define kCDELocalOffset     42              // offset to local hdr

/*
 * The values we return for ZipEntry use 0 as an invalid value, so we
 * want to adjust the hash table index by a fixed amount.  Using a large
 * value helps insure that people don't mix & match arguments, e.g. with
 * entry indices.
 */
#define kZipEntryAdj        10000

/*
 * Convert a ZipEntry to a hash table index, verifying that it's in a
 * valid range.
 */
static int entryToIndex(const ZipArchive* pArchive, const ZipEntry entry)
{
    long ent = ((long) entry) - kZipEntryAdj;
    if (ent < 0 || ent >= pArchive->mHashTableSize ||
        pArchive->mHashTable[ent].name == NULL)
    {
        LOGW("Invalid ZipEntry %p (%ld)\n", entry, ent);
        return -1;
    }
    return ent;
}

/*
 * Simple string hash function for non-null-terminated strings.
 */
static unsigned int computeHash(const char* str, int len)
{
    unsigned int hash = 0;

    while (len--)
        hash = hash * 31 + *str++;

    return hash;
}

/*
 * Add a new entry to the hash table.
 */
static void addToHash(ZipArchive* pArchive, const char* str, int strLen,
    unsigned int hash)
{
    const int hashTableSize = pArchive->mHashTableSize;
    int ent = hash & (hashTableSize - 1);

    /*
     * We over-allocated the table, so we're guaranteed to find an empty slot.
     */
    while (pArchive->mHashTable[ent].name != NULL)
        ent = (ent + 1) & (hashTableSize-1);

    pArchive->mHashTable[ent].name = str;
    pArchive->mHashTable[ent].nameLen = strLen;
}

/*
 * Get 2 little-endian bytes.
 */
static u2 get2LE(unsigned char const* pSrc)
{
    return pSrc[0] | (pSrc[1] << 8); 
}

/*
 * Get 4 little-endian bytes.
 */
static u4 get4LE(unsigned char const* pSrc)
{
    u4 result;

    result = pSrc[0];
    result |= pSrc[1] << 8;
    result |= pSrc[2] << 16;
    result |= pSrc[3] << 24;

    return result;
}

/*
 * Parse the Zip archive, verifying its contents and initializing internal
 * data structures.
 */
static bool parseZipArchive(ZipArchive* pArchive, const MemMapping* pMap)
{
#define CHECK_OFFSET(_off) {                                                \
        if ((unsigned int) (_off) >= maxOffset) {                           \
            LOGE("ERROR: bad offset %u (max %d): %s\n",                     \
                (unsigned int) (_off), maxOffset, #_off);                   \
            goto bail;                                                      \
        }                                                                   \
    }
    bool result = false;
    const unsigned char* basePtr = (const unsigned char*)pMap->addr;
    const unsigned char* ptr;
    size_t length = pMap->length;
    unsigned int i, numEntries, cdOffset;
    unsigned int val;

    /*
     * The first 4 bytes of the file will either be the local header
     * signature for the first file (kLFHSignature) or, if the archive doesn't
     * have any files in it, the end-of-central-directory signature
     * (kEOCDSignature).
     */
    val = get4LE(basePtr);
    if (val == kEOCDSignature) {
        LOGI("Found Zip archive, but it looks empty\n");
        goto bail;
    } else if (val != kLFHSignature) {
        LOGV("Not a Zip archive (found 0x%08x)\n", val);
        goto bail;
    }

    /*
     * Find the EOCD.  We'll find it immediately unless they have a file
     * comment.
     */
    ptr = basePtr + length - kEOCDLen;

    while (ptr >= basePtr) {
        if (*ptr == (kEOCDSignature & 0xff) && get4LE(ptr) == kEOCDSignature)
            break;
        ptr--;
    }
    if (ptr < basePtr) {
        LOGI("Could not find end-of-central-directory in Zip\n");
        goto bail;
    }

    /*
     * There are two interesting items in the EOCD block: the number of
     * entries in the file, and the file offset of the start of the
     * central directory.
     *
     * (There's actually a count of the #of entries in this file, and for
     * all files which comprise a spanned archive, but for our purposes
     * we're only interested in the current file.  Besides, we expect the
     * two to be equivalent for our stuff.)
     */
    numEntries = get2LE(ptr + kEOCDNumEntries);
    cdOffset = get4LE(ptr + kEOCDFileOffset);

    /* valid offsets are [0,EOCD] */
    unsigned int maxOffset;
    maxOffset = (ptr - basePtr) +1;

    LOGV("+++ numEntries=%d cdOffset=%d\n", numEntries, cdOffset);
    if (numEntries == 0 || cdOffset >= length) {
        LOGW("Invalid entries=%d offset=%d (len=%zd)\n",
            numEntries, cdOffset, length);
        goto bail;
    }

    /*
     * Create hash table.  We have a minimum 75% load factor, possibly as
     * low as 50% after we round off to a power of 2.  There must be at
     * least one unused entry to avoid an infinite loop during creation.
     */
    pArchive->mNumEntries = numEntries;
    pArchive->mHashTableSize = dexRoundUpPower2(1 + (numEntries * 4) / 3);
    pArchive->mHashTable = (ZipHashEntry*)
            calloc(pArchive->mHashTableSize, sizeof(ZipHashEntry));

    /*
     * Walk through the central directory, adding entries to the hash
     * table.
     */
    ptr = basePtr + cdOffset;
    for (i = 0; i < numEntries; i++) {
        unsigned int fileNameLen, extraLen, commentLen, localHdrOffset;
        const unsigned char* localHdr;
        unsigned int hash;

        if (get4LE(ptr) != kCDESignature) {
            LOGW("Missed a central dir sig (at %d)\n", i);
            goto bail;
        }
        if (ptr + kCDELen > basePtr + length) {
            LOGW("Ran off the end (at %d)\n", i);
            goto bail;
        }

        localHdrOffset = get4LE(ptr + kCDELocalOffset);
        CHECK_OFFSET(localHdrOffset);
        fileNameLen = get2LE(ptr + kCDENameLen);
        extraLen = get2LE(ptr + kCDEExtraLen);
        commentLen = get2LE(ptr + kCDECommentLen);

        //LOGV("+++ %d: localHdr=%d fnl=%d el=%d cl=%d\n",
        //    i, localHdrOffset, fileNameLen, extraLen, commentLen);
        //LOGV(" '%.*s'\n", fileNameLen, ptr + kCDELen);

        /* add the CDE filename to the hash table */
        hash = computeHash((const char*)ptr + kCDELen, fileNameLen);
        addToHash(pArchive, (const char*)ptr + kCDELen, fileNameLen, hash);

        localHdr = basePtr + localHdrOffset;
        if (get4LE(localHdr) != kLFHSignature) {
            LOGW("Bad offset to local header: %d (at %d)\n",
                localHdrOffset, i);
            goto bail;
        }

        ptr += kCDELen + fileNameLen + extraLen + commentLen;
        CHECK_OFFSET(ptr - basePtr);
    }

    result = true;

bail:
    return result;
#undef CHECK_OFFSET
}

/*
 * Open the specified file read-only.  We memory-map the entire thing and
 * parse the contents.
 *
 * This will be called on non-Zip files, especially during VM startup, so
 * we don't want to be too noisy about certain types of failure.  (Do
 * we want a "quiet" flag?)
 *
 * On success, we fill out the contents of "pArchive" and return 0.
 */
int dexZipOpenArchive(const char* fileName, ZipArchive* pArchive)
{
    int fd, err;

    LOGV("Opening archive '%s' %p\n", fileName, pArchive);

    memset(pArchive, 0, sizeof(ZipArchive));

    fd = open(fileName, O_RDONLY, 0);
    if (fd < 0) {
        err = errno ? errno : -1;
        LOGV("Unable to open '%s': %s\n", fileName, strerror(err));
        return err;
    }

    return dexZipPrepArchive(fd, fileName, pArchive);
}

/*
 * Prepare to access a ZipArchive in an open file descriptor.
 */
DEX_EXPORT
int dexZipPrepArchive(int fd, const char* debugFileName, ZipArchive* pArchive)
{
    MemMapping map;
    int err;

    map.addr = NULL;
    memset(pArchive, 0, sizeof(*pArchive));

    pArchive->mFd = fd;

    if (sysMapFileInShmemReadOnly(pArchive->mFd, &map) != 0) {
        err = -1;
        LOGW("Map of '%s' failed\n", debugFileName);
        goto bail;
    }

    if (map.length < kEOCDLen) {
        err = -1;
        LOGV("File '%s' too small to be zip (%zd)\n", debugFileName,map.length);
        goto bail;
    }

    if (!parseZipArchive(pArchive, &map)) {
        err = -1;
        LOGV("Parsing '%s' failed\n", debugFileName);
        goto bail;
    }

    /* success */
    err = 0;
    sysCopyMap(&pArchive->mMap, &map);
    map.addr = NULL;

bail:
    if (err != 0)
        dexZipCloseArchive(pArchive);
    if (map.addr != NULL)
        sysReleaseShmem(&map);
    return err;
}


/*
 * Close a ZipArchive, closing the file and freeing the contents.
 *
 * NOTE: the ZipArchive may not have been fully created.
 */
DEX_EXPORT
void dexZipCloseArchive(ZipArchive* pArchive)
{
    LOGV("Closing archive %p\n", pArchive);

    if (pArchive->mFd >= 0)
        close(pArchive->mFd);

    sysReleaseShmem(&pArchive->mMap);

    free(pArchive->mHashTable);

    pArchive->mFd = -1;
    pArchive->mNumEntries = -1;
    pArchive->mHashTableSize = -1;
    pArchive->mHashTable = NULL;
}


/*
 * Find a matching entry.
 *
 * Returns 0 if not found.
 */
DEX_EXPORT
ZipEntry dexZipFindEntry(const ZipArchive* pArchive, const char* entryName)
{
    int nameLen = strlen(entryName);
    unsigned int hash = computeHash(entryName, nameLen);
    const int hashTableSize = pArchive->mHashTableSize;
    int ent = hash & (hashTableSize-1);

    while (pArchive->mHashTable[ent].name != NULL) {
        if (pArchive->mHashTable[ent].nameLen == nameLen &&
            memcmp(pArchive->mHashTable[ent].name, entryName, nameLen) == 0)
        {
            /* match */
            return (ZipEntry) (ent + kZipEntryAdj);
        }

        ent = (ent + 1) & (hashTableSize-1);
    }

    return NULL;
}

#if 0
/*
 * Find the Nth entry.
 *
 * This currently involves walking through the sparse hash table, counting
 * non-empty entries.  If we need to speed this up we can either allocate
 * a parallel lookup table or (perhaps better) provide an iterator interface.
 */
ZipEntry findEntryByIndex(ZipArchive* pArchive, int idx)
{
    if (idx < 0 || idx >= pArchive->mNumEntries) {
        LOGW("Invalid index %d\n", idx);
        return NULL;
    }

    int ent;
    for (ent = 0; ent < pArchive->mHashTableSize; ent++) {
        if (pArchive->mHashTable[ent].name != NULL) {
            if (idx-- == 0)
                return (ZipEntry) (ent + kZipEntryAdj);
        }
    }

    return NULL;
}
#endif

/*
 * Get the useful fields from the zip entry.
 *
 * Returns "false" if the offsets to the fields or the contents of the fields
 * appear to be bogus.
 */
DEX_EXPORT
bool dexZipGetEntryInfo(const ZipArchive* pArchive, ZipEntry entry,
    int* pMethod, long* pUncompLen, long* pCompLen, off_t* pOffset,
    long* pModWhen, long* pCrc32)
{
    int ent = entryToIndex(pArchive, entry);
    if (ent < 0)
        return false;

    /*
     * Recover the start of the central directory entry from the filename
     * pointer.
     */
    const unsigned char* basePtr = (const unsigned char*)
        pArchive->mMap.addr;
    const unsigned char* ptr = (const unsigned char*)
        pArchive->mHashTable[ent].name;
    size_t zipLength =
        pArchive->mMap.length;

    ptr -= kCDELen;

    int method = get2LE(ptr + kCDEMethod);
    if (pMethod != NULL)
        *pMethod = method;

    if (pModWhen != NULL)
        *pModWhen = get4LE(ptr + kCDEModWhen);
    if (pCrc32 != NULL)
        *pCrc32 = get4LE(ptr + kCDECRC);

    /*
     * We need to make sure that the lengths are not so large that somebody
     * trying to map the compressed or uncompressed data runs off the end
     * of the mapped region.
     */
    unsigned long localHdrOffset = get4LE(ptr + kCDELocalOffset);
    if (localHdrOffset + kLFHLen >= zipLength) {
        LOGE("ERROR: bad local hdr offset in zip\n");
        return false;
    }
    const unsigned char* localHdr = basePtr + localHdrOffset;
    off_t dataOffset = localHdrOffset + kLFHLen
        + get2LE(localHdr + kLFHNameLen) + get2LE(localHdr + kLFHExtraLen);
    if ((unsigned long) dataOffset >= zipLength) {
        LOGE("ERROR: bad data offset in zip\n");
        return false;
    }

    if (pCompLen != NULL) {
        *pCompLen = get4LE(ptr + kCDECompLen);
        if (*pCompLen < 0 || (size_t)(dataOffset + *pCompLen) >= zipLength) {
            LOGE("ERROR: bad compressed length in zip\n");
            return false;
        }
    }
    if (pUncompLen != NULL) {
        *pUncompLen = get4LE(ptr + kCDEUncompLen);
        if (*pUncompLen < 0) {
            LOGE("ERROR: negative uncompressed length in zip\n");
            return false;
        }
        if (method == kCompressStored &&
            (size_t)(dataOffset + *pUncompLen) >= zipLength)
        {
            LOGE("ERROR: bad uncompressed length in zip\n");
            return false;
        }
    }

    if (pOffset != NULL) {
        *pOffset = dataOffset;
    }
    return true;
}

/*
 * Uncompress "deflate" data from one buffer to an open file descriptor.
 */
static bool inflateToFile(int fd, const void* inBuf, long uncompLen,
    long compLen)
{
    bool result = false;
    const int kWriteBufSize = 32768;
    unsigned char writeBuf[kWriteBufSize];
    z_stream zstream;
    int zerr;

    /*
     * Initialize the zlib stream struct.
     */
	memset(&zstream, 0, sizeof(zstream));
    zstream.zalloc = Z_NULL;
    zstream.zfree = Z_NULL;
    zstream.opaque = Z_NULL;
    zstream.next_in = (Bytef*)inBuf;
    zstream.avail_in = compLen;
    zstream.next_out = (Bytef*) writeBuf;
    zstream.avail_out = sizeof(writeBuf);
    zstream.data_type = Z_UNKNOWN;

	/*
	 * Use the undocumented "negative window bits" feature to tell zlib
	 * that there's no zlib header waiting for it.
	 */
    zerr = inflateInit2(&zstream, -MAX_WBITS);
    if (zerr != Z_OK) {
        if (zerr == Z_VERSION_ERROR) {
            LOGE("Installed zlib is not compatible with linked version (%s)\n",
                ZLIB_VERSION);
        } else {
            LOGE("Call to inflateInit2 failed (zerr=%d)\n", zerr);
        }
        goto bail;
    }

    /*
     * Loop while we have more to do.
     */
    do {
        /*
         * Expand data.
         */
        zerr = inflate(&zstream, Z_NO_FLUSH);
        if (zerr != Z_OK && zerr != Z_STREAM_END) {
            LOGW("zlib inflate: zerr=%d (nIn=%p aIn=%u nOut=%p aOut=%u)\n",
                zerr, zstream.next_in, zstream.avail_in,
                zstream.next_out, zstream.avail_out);
            goto z_bail;
        }

        /* write when we're full or when we're done */
        if (zstream.avail_out == 0 ||
            (zerr == Z_STREAM_END && zstream.avail_out != sizeof(writeBuf)))
        {
            long writeSize = zstream.next_out - writeBuf;
            int cc = write(fd, writeBuf, writeSize);
            if (cc != (int) writeSize) {
                if (cc < 0) {
                    LOGW("write failed in inflate: %s\n", strerror(errno));
                } else {
                    LOGW("partial write in inflate (%d vs %ld)\n",
                        cc, writeSize);
                }
                goto z_bail;
            }

            zstream.next_out = writeBuf;
            zstream.avail_out = sizeof(writeBuf);
        }
    } while (zerr == Z_OK);

    assert(zerr == Z_STREAM_END);       /* other errors should've been caught */

    /* paranoia */
    if ((long) zstream.total_out != uncompLen) {
        LOGW("Size mismatch on inflated file (%ld vs %ld)\n",
            zstream.total_out, uncompLen);
        goto z_bail;
    }

    result = true;

z_bail:
    inflateEnd(&zstream);        /* free up any allocated structures */

bail:
    return result;
}

/*
 * Uncompress an entry, in its entirety, to an open file descriptor.
 *
 * TODO: this doesn't verify the data's CRC, but probably should (especially
 * for uncompressed data).
 */
DEX_EXPORT
bool dexZipExtractEntryToFile(const ZipArchive* pArchive,
    const ZipEntry entry, int fd)
{
    bool result = false;
    int ent = entryToIndex(pArchive, entry);
    if (ent < 0)
        return -1;

    const unsigned char* basePtr = (const unsigned char*)pArchive->mMap.addr;
    int method;
    long uncompLen, compLen;
    off_t offset;

    if (!dexZipGetEntryInfo(pArchive, entry, &method, &uncompLen, &compLen,
            &offset, NULL, NULL))
    {
        goto bail;
    }

    if (method == kCompressStored) {
        ssize_t actual;

        actual = write(fd, basePtr + offset, uncompLen);
        if (actual < 0) {
            LOGE("Write failed: %s\n", strerror(errno));
            goto bail;
        } else if (actual != uncompLen) {
            LOGE("Partial write during uncompress (%d of %ld)\n",
                (int) actual, uncompLen);
            goto bail;
        } else {
            LOGI("+++ successful write\n");
        }
    } else {
        if (!inflateToFile(fd, basePtr+offset, uncompLen, compLen))
            goto bail;
    }

    result = true;

bail:
    return result;
}

