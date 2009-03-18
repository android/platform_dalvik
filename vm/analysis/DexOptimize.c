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
 * Convert the output from "dx" into a locally-optimized DEX file.
 *
 * TODO: the format of the optimized header is currently "whatever we
 * happen to write", since the VM that writes it is by definition the same
 * as the VM that reads it.  Still, it should be better documented and
 * more rigorously structured.
 */
#include "Dalvik.h"
#include "libdex/InstrUtils.h"
#include "libdex/OptInvocation.h"

#include <zlib.h>

#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <errno.h>

/*
 * Virtual/direct calls to "method" are replaced with an execute-inline
 * instruction with index "idx".
 */
typedef struct InlineSub {
    Method* method;
    int     inlineIdx;
} InlineSub;


/* fwd */
static int writeDependencies(int fd, u4 modWhen, u4 crc);
static bool writeAuxData(int fd, const DexClassLookup* pClassLookup,\
    const IndexMapSet* pIndexMapSet);
static void logFailedWrite(size_t expected, ssize_t actual, const char* msg,
    int err);

static bool rewriteDex(u1* addr, int len, bool doVerify, bool doOpt,\
    u4* pHeaderFlags, DexClassLookup** ppClassLookup);
static void updateChecksum(u1* addr, int len, DexHeader* pHeader);
static bool loadAllClasses(DvmDex* pDvmDex);
static void optimizeLoadedClasses(DexFile* pDexFile);
static void optimizeClass(ClassObject* clazz, const InlineSub* inlineSubs);
static bool optimizeMethod(Method* method, const InlineSub* inlineSubs);
static void rewriteInstField(Method* method, u2* insns, OpCode newOpc);
static bool rewriteVirtualInvoke(Method* method, u2* insns, OpCode newOpc);
static bool rewriteDirectInvoke(Method* method, u2* insns);
static bool rewriteExecuteInline(Method* method, u2* insns,
    MethodType methodType, const InlineSub* inlineSubs);


/*
 * Return the fd of an open file in the DEX file cache area.  If the cache
 * file doesn't exist or is out of date, this will remove the old entry,
 * create a new one (writing only the file header), and return with the
 * "new file" flag set.
 *
 * It's possible to execute from an unoptimized DEX file directly,
 * assuming the byte ordering and structure alignment is correct, but
 * disadvantageous because some significant optimizations are not possible.
 * It's not generally possible to do the same from an uncompressed Jar
 * file entry, because we have to guarantee 32-bit alignment in the
 * memory-mapped file.
 *
 * For a Jar/APK file (a zip archive with "classes.dex" inside), "modWhen"
 * and "crc32" come from the Zip directory entry.  For a stand-alone DEX
 * file, it's the modification date of the file and the Adler32 from the
 * DEX header (which immediately follows the magic).  If these don't
 * match what's stored in the opt header, we reject the file immediately.
 *
 * On success, the file descriptor will be positioned just past the "opt"
 * file header, and will be locked with flock.  "*pCachedName" will point
 * to newly-allocated storage.
 */
int dvmOpenCachedDexFile(const char* fileName, const char* cacheFileName,
    u4 modWhen, u4 crc, bool isBootstrap, bool* pNewFile, bool createIfMissing)
{
    int fd, cc;
    struct stat fdStat, fileStat;
    bool readOnly = false;

    *pNewFile = false;

retry:
    /*
     * Try to open the cache file.  If we've been asked to,
     * create it if it doesn't exist.
     */
    fd = createIfMissing ? open(cacheFileName, O_CREAT|O_RDWR, 0644) : -1;
    if (fd < 0) {
        fd = open(cacheFileName, O_RDONLY, 0);
        if (fd < 0) {
            if (createIfMissing) {
                LOGE("Can't open dex cache '%s': %s\n",
                    cacheFileName, strerror(errno));
            }
            return fd;
        }
        readOnly = true;
    }

    /*
     * Grab an exclusive lock on the cache file.  If somebody else is
     * working on it, we'll block here until they complete.  Because
     * we're waiting on an external resource, we go into VMWAIT mode.
     */
    int oldStatus;
    LOGV("DexOpt: locking cache file %s (fd=%d, boot=%d)\n",
        cacheFileName, fd, isBootstrap);
    oldStatus = dvmChangeStatus(NULL, THREAD_VMWAIT);
    cc = flock(fd, LOCK_EX | LOCK_NB);
    if (cc != 0) {
        LOGD("DexOpt: sleeping on flock(%s)\n", cacheFileName);
        cc = flock(fd, LOCK_EX);
    }
    dvmChangeStatus(NULL, oldStatus);
    if (cc != 0) {
        LOGE("Can't lock dex cache '%s': %d\n", cacheFileName, cc);
        close(fd);
        return -1;
    }
    LOGV("DexOpt:  locked cache file\n");

    /*
     * Check to see if the fd we opened and locked matches the file in
     * the filesystem.  If they don't, then somebody else unlinked ours
     * and created a new file, and we need to use that one instead.  (If
     * we caught them between the unlink and the create, we'll get an
     * ENOENT from the file stat.)
     */
    cc = fstat(fd, &fdStat);
    if (cc != 0) {
        LOGE("Can't stat open file '%s'\n", cacheFileName);
        LOGVV("DexOpt: unlocking cache file %s\n", cacheFileName);
        goto close_fail;
    }
    cc = stat(cacheFileName, &fileStat);
    if (cc != 0 ||
        fdStat.st_dev != fileStat.st_dev || fdStat.st_ino != fileStat.st_ino)
    {
        LOGD("DexOpt: our open cache file is stale; sleeping and retrying\n");
        LOGVV("DexOpt: unlocking cache file %s\n", cacheFileName);
        flock(fd, LOCK_UN);
        close(fd);
        usleep(250 * 1000);     /* if something is hosed, don't peg machine */
        goto retry;
    }

    /*
     * We have the correct file open and locked.  If the file size is zero,
     * then it was just created by us, and we want to fill in some fields
     * in the "opt" header and set "*pNewFile".  Otherwise, we want to
     * verify that the fields in the header match our expectations, and
     * reset the file if they don't.
     */
    if (fdStat.st_size == 0) {
        if (readOnly) {
            LOGW("DexOpt: file has zero length and isn't writable\n");
            goto close_fail;
        }
        cc = dexOptCreateEmptyHeader(fd);
        if (cc != 0)
            goto close_fail;
        *pNewFile = true;
        LOGV("DexOpt: successfully initialized new cache file\n");
    } else {
        bool expectVerify, expectOpt;

        if (gDvm.classVerifyMode == VERIFY_MODE_NONE)
            expectVerify = false;
        else if (gDvm.classVerifyMode == VERIFY_MODE_REMOTE)
            expectVerify = !isBootstrap;
        else /*if (gDvm.classVerifyMode == VERIFY_MODE_ALL)*/
            expectVerify = true;

        if (gDvm.dexOptMode == OPTIMIZE_MODE_NONE)
            expectOpt = false;
        else if (gDvm.dexOptMode == OPTIMIZE_MODE_VERIFIED)
            expectOpt = expectVerify;
        else /*if (gDvm.dexOptMode == OPTIMIZE_MODE_ALL)*/
            expectOpt = true;

        LOGV("checking deps, expecting vfy=%d opt=%d\n",
            expectVerify, expectOpt);

        if (!dvmCheckOptHeaderAndDependencies(fd, true, modWhen, crc,
                expectVerify, expectOpt))
        {
            if (readOnly) {
                /*
                 * We could unlink and rewrite the file if we own it or
                 * the "sticky" bit isn't set on the directory.  However,
                 * we're not able to truncate it, which spoils things.  So,
                 * give up now.
                 */
                if (createIfMissing) {
                    LOGW("Cached DEX '%s' (%s) is stale and not writable\n",
                        fileName, cacheFileName);
                }
                goto close_fail;
            }

            /*
             * If we truncate the existing file before unlinking it, any
             * process that has it mapped will fail when it tries to touch
             * the pages.
             *
             * This is very important.  The zygote process will have the
             * boot DEX files (core, framework, etc.) mapped early.  If
             * (say) core.dex gets updated, and somebody launches an app
             * that uses App.dex, then App.dex gets reoptimized because it's
             * dependent upon the boot classes.  However, dexopt will be
             * using the *new* core.dex to do the optimizations, while the
             * app will actually be running against the *old* core.dex
             * because it starts from zygote.
             *
             * Even without zygote, it's still possible for a class loader
             * to pull in an APK that was optimized against an older set
             * of DEX files.  We must ensure that everything fails when a
             * boot DEX gets updated, and for general "why aren't my
             * changes doing anything" purposes its best if we just make
             * everything crash when a DEX they're using gets updated.
             */
            LOGD("Stale deps in cache file; removing and retrying\n");
            if (ftruncate(fd, 0) != 0) {
                LOGW("Warning: unable to truncate cache file '%s': %s\n",
                    cacheFileName, strerror(errno));
                /* keep going */
            }
            if (unlink(cacheFileName) != 0) {
                LOGW("Warning: unable to remove cache file '%s': %d %s\n",
                    cacheFileName, errno, strerror(errno));
                /* keep going; permission failure should probably be fatal */
            }
            LOGVV("DexOpt: unlocking cache file %s\n", cacheFileName);
            flock(fd, LOCK_UN);
            close(fd);
            goto retry;
        } else {
            LOGV("DexOpt: good deps in cache file\n");
        }
    }

    assert(fd >= 0);
    return fd;

close_fail:
    flock(fd, LOCK_UN);
    close(fd);
    return -1;
}

/*
 * Unlock the file descriptor.
 *
 * Returns "true" on success.
 */
bool dvmUnlockCachedDexFile(int fd)
{
    LOGVV("DexOpt: unlocking cache file fd=%d\n", fd);
    return (flock(fd, LOCK_UN) == 0);
}


/*
 * Given a descriptor for a file with DEX data in it, produce an
 * optimized version.
 *
 * The file pointed to by "fd" is expected to be a locked shared resource
 * (or private); we make no efforts to enforce multi-process correctness
 * here.
 *
 * "fileName" is only used for debug output.  "modWhen" and "crc" are stored
 * in the dependency set.
 *
 * The "isBootstrap" flag determines how the optimizer and verifier handle
 * package-scope access checks.  When optimizing, we only load the bootstrap
 * class DEX files and the target DEX, so the flag determines whether the
 * target DEX classes are given a (synthetic) non-NULL classLoader pointer.
 * This only really matters if the target DEX contains classes that claim to
 * be in the same package as bootstrap classes.
 *
 * The optimizer will need to load every class in the target DEX file.
 * This is generally undesirable, so we start a subprocess to do the
 * work and wait for it to complete.
 *
 * Returns "true" on success.  All data will have been written to "fd".
 */
bool dvmOptimizeDexFile(int fd, off_t dexOffset, long dexLength,
    const char* fileName, u4 modWhen, u4 crc, bool isBootstrap)
{
    const char* lastPart = strrchr(fileName, '/');
    if (lastPart != NULL)
        lastPart++;
    else
        lastPart = fileName;

    /*
     * For basic optimizations (byte-swapping and structure aligning) we
     * don't need to fork().  It looks like fork+exec is causing problems
     * with gdb on our bewildered Linux distro, so in some situations we
     * want to avoid this.
     *
     * For optimization and/or verification, we need to load all the classes.
     *
     * We don't check gDvm.generateRegisterMaps, since that is dependent
     * upon the verifier state.
     */
    if (gDvm.classVerifyMode == VERIFY_MODE_NONE &&
        (gDvm.dexOptMode == OPTIMIZE_MODE_NONE ||
         gDvm.dexOptMode == OPTIMIZE_MODE_VERIFIED))
    {
        LOGD("DexOpt: --- BEGIN (quick) '%s' ---\n", lastPart);
        return dvmContinueOptimization(fd, dexOffset, dexLength,
                fileName, modWhen, crc, isBootstrap);
    }


    LOGD("DexOpt: --- BEGIN '%s' (bootstrap=%d) ---\n", lastPart, isBootstrap);

    pid_t pid;

    /*
     * This could happen if something in our bootclasspath, which we thought
     * was all optimized, got rejected.
     */
    if (gDvm.optimizing) {
        LOGW("Rejecting recursive optimization attempt on '%s'\n", fileName);
        return false;
    }

    pid = fork();
    if (pid == 0) {
        static const int kUseValgrind = 0;
        static const char* kDexOptBin = "/bin/dexopt";
        static const char* kValgrinder = "/usr/bin/valgrind";
        static const int kFixedArgCount = 10;
        static const int kValgrindArgCount = 5;
        static const int kMaxIntLen = 12;   // '-'+10dig+'\0' -OR- 0x+8dig
        int bcpSize = dvmGetBootPathSize();
        int argc = kFixedArgCount + bcpSize
            + (kValgrindArgCount * kUseValgrind);
        char* argv[argc+1];             // last entry is NULL
        char values[argc][kMaxIntLen];
        char* execFile;
        char* androidRoot;
        int flags;

        /* full path to optimizer */
        androidRoot = getenv("ANDROID_ROOT");
        if (androidRoot == NULL) {
            LOGW("ANDROID_ROOT not set, defaulting to /system\n");
            androidRoot = "/system";
        }
        execFile = malloc(strlen(androidRoot) + strlen(kDexOptBin) + 1);
        strcpy(execFile, androidRoot);
        strcat(execFile, kDexOptBin);

        /*
         * Create arg vector.
         */
        int curArg = 0;

        if (kUseValgrind) {
            /* probably shouldn't ship the hard-coded path */
            argv[curArg++] = (char*)kValgrinder;
            argv[curArg++] = "--tool=memcheck";
            argv[curArg++] = "--leak-check=yes";        // check for leaks too
            argv[curArg++] = "--leak-resolution=med";   // increase from 2 to 4
            argv[curArg++] = "--num-callers=16";        // default is 12
            assert(curArg == kValgrindArgCount);
        }
        argv[curArg++] = execFile;

        argv[curArg++] = "--dex";

        sprintf(values[2], "%d", DALVIK_VM_BUILD);
        argv[curArg++] = values[2];

        sprintf(values[3], "%d", fd);
        argv[curArg++] = values[3];

        sprintf(values[4], "%d", (int) dexOffset);
        argv[curArg++] = values[4];

        sprintf(values[5], "%d", (int) dexLength);
        argv[curArg++] = values[5];

        argv[curArg++] = (char*)fileName;

        sprintf(values[7], "%d", (int) modWhen);
        argv[curArg++] = values[7];

        sprintf(values[8], "%d", (int) crc);
        argv[curArg++] = values[8];

        flags = 0;
        if (gDvm.dexOptMode != OPTIMIZE_MODE_NONE) {
            flags |= DEXOPT_OPT_ENABLED;
            if (gDvm.dexOptMode == OPTIMIZE_MODE_ALL)
                flags |= DEXOPT_OPT_ALL;
        }
        if (gDvm.classVerifyMode != VERIFY_MODE_NONE) {
            flags |= DEXOPT_VERIFY_ENABLED;
            if (gDvm.classVerifyMode == VERIFY_MODE_ALL)
                flags |= DEXOPT_VERIFY_ALL;
        }
        if (isBootstrap)
            flags |= DEXOPT_IS_BOOTSTRAP;
        if (gDvm.generateRegisterMaps)
            flags |= DEXOPT_GEN_REGISTER_MAP;
        sprintf(values[9], "%d", flags);
        argv[curArg++] = values[9];

        assert(((!kUseValgrind && curArg == kFixedArgCount) ||
               ((kUseValgrind && curArg == kFixedArgCount+kValgrindArgCount))));

        ClassPathEntry* cpe;
        for (cpe = gDvm.bootClassPath; cpe->ptr != NULL; cpe++) {
            argv[curArg++] = cpe->fileName;
        }
        assert(curArg == argc);

        argv[curArg] = NULL;

        if (kUseValgrind)
            execv(kValgrinder, argv);
        else
            execv(execFile, argv);

        LOGE("execv '%s'%s failed: %s\n", execFile,
            kUseValgrind ? " [valgrind]" : "", strerror(errno));
        exit(1);
    } else {
        LOGV("DexOpt: waiting for verify+opt, pid=%d\n", (int) pid);
        int status;
        pid_t gotPid;
        int oldStatus;

        /*
         * Wait for the optimization process to finish.  We go into VMWAIT
         * mode here so GC suspension won't have to wait for us.
         */
        oldStatus = dvmChangeStatus(NULL, THREAD_VMWAIT);
        while (true) {
            gotPid = waitpid(pid, &status, 0);
            if (gotPid == -1 && errno == EINTR) {
                LOGD("waitpid interrupted, retrying\n");
            } else {
                break;
            }
        }
        dvmChangeStatus(NULL, oldStatus);
        if (gotPid != pid) {
            LOGE("waitpid failed: wanted %d, got %d: %s\n",
                (int) pid, (int) gotPid, strerror(errno));
            return false;
        }

        if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
            LOGD("DexOpt: --- END '%s' (success) ---\n", lastPart);
            return true;
        } else {
            LOGW("DexOpt: --- END '%s' --- status=0x%04x, process failed\n",
                lastPart, status);
            return false;
        }
    }
}

/*
 * Do the actual optimization.  This is called directly for "minimal"
 * optimization, or from a newly-created process for "full" optimization.
 *
 * For best use of disk/memory, we want to extract once and perform
 * optimizations in place.  If the file has to expand or contract
 * to match local structure padding/alignment expectations, we want
 * to do the rewrite as part of the extract, rather than extracting
 * into a temp file and slurping it back out.  (The structure alignment
 * is currently correct for all platforms, and this isn't expected to
 * change, so we should be okay with having it already extracted.)
 *
 * Returns "true" on success.
 */
bool dvmContinueOptimization(int fd, off_t dexOffset, long dexLength,
    const char* fileName, u4 modWhen, u4 crc, bool isBootstrap)
{
    DexClassLookup* pClassLookup = NULL;
    IndexMapSet* pIndexMapSet = NULL;
    bool doVerify, doOpt;
    u4 headerFlags = 0;

    if (gDvm.classVerifyMode == VERIFY_MODE_NONE)
        doVerify = false;
    else if (gDvm.classVerifyMode == VERIFY_MODE_REMOTE)
        doVerify = !isBootstrap;
    else /*if (gDvm.classVerifyMode == VERIFY_MODE_ALL)*/
        doVerify = true;

    if (gDvm.dexOptMode == OPTIMIZE_MODE_NONE)
        doOpt = false;
    else if (gDvm.dexOptMode == OPTIMIZE_MODE_VERIFIED)
        doOpt = doVerify;
    else /*if (gDvm.dexOptMode == OPTIMIZE_MODE_ALL)*/
        doOpt = true;

    LOGV("Continuing optimization (%s, isb=%d, vfy=%d, opt=%d)\n",
        fileName, isBootstrap, doVerify, doOpt);

    assert(dexOffset >= 0);

    /* quick test so we don't blow up on empty file */
    if (dexLength < (int) sizeof(DexHeader)) {
        LOGE("too small to be DEX\n");
        return false;
    }
    if (dexOffset < (int) sizeof(DexOptHeader)) {
        LOGE("not enough room for opt header\n");
        return false;
    }

    bool result = false;

    /*
     * Drop this into a global so we don't have to pass it around.  We could
     * also add a field to DexFile, but since it only pertains to DEX
     * creation that probably doesn't make sense.
     */
    gDvm.optimizingBootstrapClass = isBootstrap;

    {
        /*
         * Map the entire file (so we don't have to worry about page
         * alignment).  The expectation is that the output file contains
         * our DEX data plus room for a small header.
         */
        bool success;
        void* mapAddr;
        mapAddr = mmap(NULL, dexOffset + dexLength, PROT_READ|PROT_WRITE,
                    MAP_SHARED, fd, 0);
        if (mapAddr == MAP_FAILED) {
            LOGE("unable to mmap DEX cache: %s\n", strerror(errno));
            goto bail;
        }

        /*
         * Rewrite the file.  Byte reordering, structure realigning,
         * class verification, and bytecode optimization are all performed
         * here.
         */
        success = rewriteDex(((u1*) mapAddr) + dexOffset, dexLength,
                    doVerify, doOpt, &headerFlags, &pClassLookup);

        if (success) {
            DvmDex* pDvmDex = NULL;
            u1* dexAddr = ((u1*) mapAddr) + dexOffset;

            if (dvmDexFileOpenPartial(dexAddr, dexLength, &pDvmDex) != 0) {
                LOGE("Unable to create DexFile\n");
            } else {
                /*
                 * If configured to do so, scan the instructions, looking
                 * for ways to reduce the size of the resolved-constant table.
                 * This is done post-optimization, across the instructions
                 * in all methods in all classes (even the ones that failed
                 * to load).
                 */
                pIndexMapSet = dvmRewriteConstants(pDvmDex);

                updateChecksum(dexAddr, dexLength,
                    (DexHeader*) pDvmDex->pHeader);

                dvmDexFileFree(pDvmDex);
            }
        }

        /* unmap the read-write version, forcing writes to disk */
        if (msync(mapAddr, dexOffset + dexLength, MS_SYNC) != 0) {
            LOGW("msync failed: %s\n", strerror(errno));
            // weird, but keep going
        }
#if 1
        /*
         * This causes clean shutdown to fail, because we have loaded classes
         * that point into it.  For the optimizer this isn't a problem,
         * because it's more efficient for the process to simply exit.
         * Exclude this code when doing clean shutdown for valgrind.
         */
        if (munmap(mapAddr, dexOffset + dexLength) != 0) {
            LOGE("munmap failed: %s\n", strerror(errno));
            goto bail;
        }
#endif

        if (!success)
            goto bail;
    }

    /* get start offset, and adjust deps start for 64-bit alignment */
    off_t depsOffset, auxOffset, endOffset, adjOffset;
    int depsLength, auxLength;

    depsOffset = lseek(fd, 0, SEEK_END);
    if (depsOffset < 0) {
        LOGE("lseek to EOF failed: %s\n", strerror(errno));
        goto bail;
    }
    adjOffset = (depsOffset + 7) & ~(0x07);
    if (adjOffset != depsOffset) {
        LOGV("Adjusting deps start from %d to %d\n",
            (int) depsOffset, (int) adjOffset);
        depsOffset = adjOffset;
        lseek(fd, depsOffset, SEEK_SET);
    }

    /*
     * Append the dependency list.
     */
    if (writeDependencies(fd, modWhen, crc) != 0) {
        LOGW("Failed writing dependencies\n");
        goto bail;
    }


    /* compute deps length, and adjust aux start for 64-bit alignment */
    auxOffset = lseek(fd, 0, SEEK_END);
    depsLength = auxOffset - depsOffset;

    adjOffset = (auxOffset + 7) & ~(0x07);
    if (adjOffset != auxOffset) {
        LOGV("Adjusting aux start from %d to %d\n",
            (int) auxOffset, (int) adjOffset);
        auxOffset = adjOffset;
        lseek(fd, auxOffset, SEEK_SET);
    }

    /*
     * Append any auxillary pre-computed data structures.
     */
    if (!writeAuxData(fd, pClassLookup, pIndexMapSet)) {
        LOGW("Failed writing aux data\n");
        goto bail;
    }

    endOffset = lseek(fd, 0, SEEK_END);
    auxLength = endOffset - auxOffset;

    /*
     * Output the "opt" header with all values filled in and a correct
     * magic number.
     */
    DexOptHeader optHdr;
    memset(&optHdr, 0xff, sizeof(optHdr));
    memcpy(optHdr.magic, DEX_OPT_MAGIC, 4);
    memcpy(optHdr.magic+4, DEX_OPT_MAGIC_VERS, 4);
    optHdr.dexOffset = (u4) dexOffset;
    optHdr.dexLength = (u4) dexLength;
    optHdr.depsOffset = (u4) depsOffset;
    optHdr.depsLength = (u4) depsLength;
    optHdr.auxOffset = (u4) auxOffset;
    optHdr.auxLength = (u4) auxLength;

    optHdr.flags = headerFlags;

    ssize_t actual;
    lseek(fd, 0, SEEK_SET);
    actual = write(fd, &optHdr, sizeof(optHdr));
    if (actual != sizeof(optHdr)) {
        logFailedWrite(sizeof(optHdr), actual, "opt header", errno);
        goto bail;
    }

    LOGV("Successfully wrote DEX header\n");
    result = true;

bail:
    dvmFreeIndexMapSet(pIndexMapSet);
    free(pClassLookup);
    return result;
}


/*
 * Get the cache file name from a ClassPathEntry.
 */
static const char* getCacheFileName(const ClassPathEntry* cpe)
{
    switch (cpe->kind) {
    case kCpeJar:
        return dvmGetJarFileCacheFileName((JarFile*) cpe->ptr);
    case kCpeDex:
        return dvmGetRawDexFileCacheFileName((RawDexFile*) cpe->ptr);
    default:
        LOGE("DexOpt: unexpected cpe kind %d\n", cpe->kind);
        dvmAbort();
        return NULL;
    }
}

/*
 * Get the SHA-1 signature.
 */
static const u1* getSignature(const ClassPathEntry* cpe)
{
    DvmDex* pDvmDex;

    switch (cpe->kind) {
    case kCpeJar:
        pDvmDex = dvmGetJarFileDex((JarFile*) cpe->ptr);
        break;
    case kCpeDex:
        pDvmDex = dvmGetRawDexFileDex((RawDexFile*) cpe->ptr);
        break;
    default:
        LOGE("unexpected cpe kind %d\n", cpe->kind);
        dvmAbort();
        pDvmDex = NULL;         // make gcc happy
    }

    assert(pDvmDex != NULL);
    return pDvmDex->pDexFile->pHeader->signature;
}


/*
 * Dependency layout:
 *  4b  Source file modification time, in seconds since 1970 UTC
 *  4b  CRC-32 from Zip entry, or Adler32 from source DEX header
 *  4b  Dalvik VM build number
 *  4b  Number of dependency entries that follow
 *  Dependency entries:
 *    4b  Name length (including terminating null)
 *    var Full path of cache entry (null terminated)
 *    20b SHA-1 signature from source DEX file
 *
 * If this changes, update DEX_OPT_MAGIC_VERS.
 */
static const size_t kMinDepSize = 4 * 4;
static const size_t kMaxDepSize = 4 * 4 + 1024;     // sanity check

/*
 * Read the "opt" header, verify it, then read the dependencies section
 * and verify that data as well.
 *
 * If "sourceAvail" is "true", this will verify that "modWhen" and "crc"
 * match up with what is stored in the header.  If they don't, we reject
 * the file so that it can be recreated from the updated original.  If
 * "sourceAvail" isn't set, e.g. for a .odex file, we ignore these arguments.
 *
 * On successful return, the file will be seeked immediately past the
 * "opt" header.
 */
bool dvmCheckOptHeaderAndDependencies(int fd, bool sourceAvail, u4 modWhen,
    u4 crc, bool expectVerify, bool expectOpt)
{
    DexOptHeader optHdr;
    u1* depData = NULL;
    const u1* magic;
    off_t posn;
    int result = false;
    ssize_t actual;

    /*
     * Start at the start.  The "opt" header, when present, will always be
     * the first thing in the file.
     */
    if (lseek(fd, 0, SEEK_SET) != 0) {
        LOGE("DexOpt: failed to seek to start of file: %s\n", strerror(errno));
        goto bail;
    }

    /*
     * Read and do trivial verification on the opt header.  The header is
     * always in host byte order.
     */
    if (read(fd, &optHdr, sizeof(optHdr)) != sizeof(optHdr)) {
        LOGE("DexOpt: failed reading opt header: %s\n", strerror(errno));
        goto bail;
    }

    magic = optHdr.magic;
    if (memcmp(magic, DEX_OPT_MAGIC, 4) != 0) {
        /* not a DEX file, or previous attempt was interrupted */
        LOGD("DexOpt: incorrect opt magic number (0x%02x %02x %02x %02x)\n",
            magic[0], magic[1], magic[2], magic[3]);
        goto bail;
    }
    if (memcmp(magic+4, DEX_OPT_MAGIC_VERS, 4) != 0) {
        LOGW("DexOpt: stale opt version (0x%02x %02x %02x %02x)\n",
            magic[4], magic[5], magic[6], magic[7]);
        goto bail;
    }
    if (optHdr.depsLength < kMinDepSize || optHdr.depsLength > kMaxDepSize) {
        LOGW("DexOpt: weird deps length %d, bailing\n", optHdr.depsLength);
        goto bail;
    }

    /*
     * Do the header flags match up with what we want?
     *
     * This is useful because it allows us to automatically regenerate
     * a file when settings change (e.g. verification is now mandatory),
     * but can cause difficulties if the bootstrap classes we depend upon
     * were handled differently than the current options specify.  We get
     * upset because they're not verified or optimized, but we're not able
     * to regenerate them because the installer won't let us.
     *
     * (This is also of limited value when !sourceAvail.)
     *
     * So, for now, we essentially ignore "expectVerify" and "expectOpt"
     * by limiting the match mask.
     *
     * The only thing we really can't handle is incorrect byte-ordering.
     */
    const u4 matchMask = DEX_OPT_FLAG_BIG;
    u4 expectedFlags = 0;
#if __BYTE_ORDER != __LITTLE_ENDIAN
    expectedFlags |= DEX_OPT_FLAG_BIG;
#endif
    if (expectVerify)
        expectedFlags |= DEX_FLAG_VERIFIED;
    if (expectOpt)
        expectedFlags |= DEX_OPT_FLAG_FIELDS | DEX_OPT_FLAG_INVOCATIONS;
    if ((expectedFlags & matchMask) != (optHdr.flags & matchMask)) {
        LOGI("DexOpt: header flag mismatch (0x%02x vs 0x%02x, mask=0x%02x)\n",
            expectedFlags, optHdr.flags, matchMask);
        goto bail;
    }

    posn = lseek(fd, optHdr.depsOffset, SEEK_SET);
    if (posn < 0) {
        LOGW("DexOpt: seek to deps failed: %s\n", strerror(errno));
        goto bail;
    }

    /*
     * Read all of the dependency stuff into memory.
     */
    depData = (u1*) malloc(optHdr.depsLength);
    if (depData == NULL) {
        LOGW("DexOpt: unable to allocate %d bytes for deps\n",
            optHdr.depsLength);
        goto bail;
    }
    actual = read(fd, depData, optHdr.depsLength);
    if (actual != (ssize_t) optHdr.depsLength) {
        LOGW("DexOpt: failed reading deps: %d of %d (err=%s)\n",
            (int) actual, optHdr.depsLength, strerror(errno));
        goto bail;
    }

    /*
     * Verify simple items.
     */
    const u1* ptr;
    u4 val;

    ptr = depData;
    val = read4LE(&ptr);
    if (sourceAvail && val != modWhen) {
        LOGI("DexOpt: source file mod time mismatch (%08x vs %08x)\n",
            val, modWhen);
        goto bail;
    }
    val = read4LE(&ptr);
    if (sourceAvail && val != crc) {
        LOGI("DexOpt: source file CRC mismatch (%08x vs %08x)\n", val, crc);
        goto bail;
    }
    val = read4LE(&ptr);
    if (val != DALVIK_VM_BUILD) {
        LOGI("DexOpt: VM build mismatch (%d vs %d)\n", val, DALVIK_VM_BUILD);
        goto bail;
    }

    /*
     * Verify dependencies on other cached DEX files.  It must match
     * exactly with what is currently defined in the bootclasspath.
     */
    ClassPathEntry* cpe;
    u4 numDeps;

    numDeps = read4LE(&ptr);
    LOGV("+++ DexOpt: numDeps = %d\n", numDeps);
    for (cpe = gDvm.bootClassPath; cpe->ptr != NULL; cpe++) {
        const char* cacheFileName = getCacheFileName(cpe);
        const u1* signature = getSignature(cpe);
        size_t len = strlen(cacheFileName) +1;
        u4 storedStrLen;

        if (numDeps == 0) {
            /* more entries in bootclasspath than in deps list */
            LOGI("DexOpt: not all deps represented\n");
            goto bail;
        }

        storedStrLen = read4LE(&ptr);
        if (len != storedStrLen ||
            strcmp(cacheFileName, (const char*) ptr) != 0)
        {
            LOGI("DexOpt: mismatch dep name: '%s' vs. '%s'\n",
                cacheFileName, ptr);
            goto bail;
        }

        ptr += storedStrLen;

        if (memcmp(signature, ptr, kSHA1DigestLen) != 0) {
            LOGI("DexOpt: mismatch dep signature for '%s'\n", cacheFileName);
            goto bail;
        }
        ptr += kSHA1DigestLen;

        LOGV("DexOpt: dep match on '%s'\n", cacheFileName);

        numDeps--;
    }

    if (numDeps != 0) {
        /* more entries in deps list than in classpath */
        LOGI("DexOpt: Some deps went away\n");
        goto bail;
    }

    // consumed all data and no more?
    if (ptr != depData + optHdr.depsLength) {
        LOGW("DexOpt: Spurious dep data? %d vs %d\n",
            (int) (ptr - depData), optHdr.depsLength);
        assert(false);
    }

    result = true;

bail:
    free(depData);
    return result;
}

/*
 * Write the dependency info to "fd" at the current file position.
 */
static int writeDependencies(int fd, u4 modWhen, u4 crc)
{
    u1* buf = NULL;
    ssize_t actual;
    int result = -1;
    ssize_t bufLen;
    ClassPathEntry* cpe;
    int i, numDeps;

    /*
     * Count up the number of completed entries in the bootclasspath.
     */
    numDeps = 0;
    bufLen = 0;
    for (cpe = gDvm.bootClassPath; cpe->ptr != NULL; cpe++) {
        const char* cacheFileName = getCacheFileName(cpe);
        LOGV("+++ DexOpt: found dep '%s'\n", cacheFileName);

        numDeps++;
        bufLen += strlen(cacheFileName) +1;
    }

    bufLen += 4*4 + numDeps * (4+kSHA1DigestLen);

    buf = malloc(bufLen);

    set4LE(buf+0, modWhen);
    set4LE(buf+4, crc);
    set4LE(buf+8, DALVIK_VM_BUILD);
    set4LE(buf+12, numDeps);

    // TODO: do we want to add dvmGetInlineOpsTableLength() here?  Won't
    // help us if somebody replaces an existing entry, but it'd catch
    // additions/removals.

    u1* ptr = buf + 4*4;
    for (cpe = gDvm.bootClassPath; cpe->ptr != NULL; cpe++) {
        const char* cacheFileName = getCacheFileName(cpe);
        const u1* signature = getSignature(cpe);
        int len = strlen(cacheFileName) +1;

        if (ptr + 4 + len + kSHA1DigestLen > buf + bufLen) {
            LOGE("DexOpt: overran buffer\n");
            dvmAbort();
        }

        set4LE(ptr, len);
        ptr += 4;
        memcpy(ptr, cacheFileName, len);
        ptr += len;
        memcpy(ptr, signature, kSHA1DigestLen);
        ptr += kSHA1DigestLen;
    }

    assert(ptr == buf + bufLen);

    actual = write(fd, buf, bufLen);
    if (actual != bufLen) {
        result = (errno != 0) ? errno : -1;
        logFailedWrite(bufLen, actual, "dep info", errno);
    } else {
        result = 0;
    }

    free(buf);
    return result;
}


/*
 * Write a block of data in "chunk" format.
 *
 * The chunk header fields are always in "native" byte order.  If "size"
 * is not a multiple of 8 bytes, the data area is padded out.
 */
static bool writeChunk(int fd, u4 type, const void* data, size_t size)
{
    ssize_t actual;
    union {             /* save a syscall by grouping these together */
        char raw[8];
        struct {
            u4 type;
            u4 size;
        } ts;
    } header;

    assert(sizeof(header) == 8);

    LOGV("Writing chunk, type=%.4s size=%d\n", (char*) &type, size);

    header.ts.type = type;
    header.ts.size = (u4) size;
    actual = write(fd, &header, sizeof(header));
    if (actual != sizeof(header)) {
        logFailedWrite(size, actual, "aux chunk header write", errno);
        return false;
    }

    if (size > 0) {
        actual = write(fd, data, size);
        if (actual != (ssize_t) size) {
            logFailedWrite(size, actual, "aux chunk write", errno);
            return false;
        }
    }

    /* if necessary, pad to 64-bit alignment */
    if ((size & 7) != 0) {
        int padSize = 8 - (size & 7);
        LOGV("size was %d, inserting %d pad bytes\n", size, padSize);
        lseek(fd, padSize, SEEK_CUR);
    }

    assert( ((int)lseek(fd, 0, SEEK_CUR) & 7) == 0);

    return true;
}

/*
 * Write aux data.
 *
 * We have different pieces, some of which may be optional.  To make the
 * most effective use of space, we use a "chunk" format, with a 4-byte
 * type and a 4-byte length.  We guarantee 64-bit alignment for the data,
 * so it can be used directly when the file is mapped for reading.
 */
static bool writeAuxData(int fd, const DexClassLookup* pClassLookup,
    const IndexMapSet* pIndexMapSet)
{
    /* pre-computed class lookup hash table */
    if (!writeChunk(fd, (u4) kDexChunkClassLookup, pClassLookup,
            pClassLookup->size))
    {
        return false;
    }

    /* remapped constants (optional) */
    if (pIndexMapSet != NULL) {
        if (!writeChunk(fd, pIndexMapSet->chunkType, pIndexMapSet->chunkData,
                pIndexMapSet->chunkDataLen))
        {
            return false;
        }
    }

    /* write the end marker */
    if (!writeChunk(fd, (u4) kDexChunkEnd, NULL, 0)) {
        return false;
    }

    return true;
}

/*
 * Log a failed write.
 */
static void logFailedWrite(size_t expected, ssize_t actual, const char* msg,
    int err)
{
    LOGE("Write failed: %s (%d of %d): %s\n",
        msg, (int)actual, (int)expected, strerror(err));
}


/*
 * ===========================================================================
 *      Optimizations
 * ===========================================================================
 */

/*
 * Perform in-place rewrites on a memory-mapped DEX file.
 *
 * This happens in a short-lived child process, so we can go nutty with
 * loading classes and allocating memory.
 */
static bool rewriteDex(u1* addr, int len, bool doVerify, bool doOpt,
    u4* pHeaderFlags, DexClassLookup** ppClassLookup)
{
    u8 prepWhen, loadWhen, verifyWhen, optWhen;
    DvmDex* pDvmDex = NULL;
    bool result = false;

    *pHeaderFlags = 0;

    LOGV("+++ swapping bytes\n");
    if (dexFixByteOrdering(addr, len) != 0)
        goto bail;
#if __BYTE_ORDER != __LITTLE_ENDIAN
    *pHeaderFlags |= DEX_OPT_FLAG_BIG;
#endif

    /*
     * Now that the DEX file can be read directly, create a DexFile for it.
     */
    if (dvmDexFileOpenPartial(addr, len, &pDvmDex) != 0) {
        LOGE("Unable to create DexFile\n");
        goto bail;
    }

    /*
     * Create the class lookup table.
     */
    //startWhen = dvmGetRelativeTimeUsec();
    *ppClassLookup = dexCreateClassLookup(pDvmDex->pDexFile);
    if (*ppClassLookup == NULL)
        goto bail;

    /*
     * Bail out early if they don't want The Works.  The current implementation
     * doesn't fork a new process if this flag isn't set, so we really don't
     * want to continue on with the crazy class loading.
     */
    if (!doVerify && !doOpt) {
        result = true;
        goto bail;
    }

    /* this is needed for the next part */
    pDvmDex->pDexFile->pClassLookup = *ppClassLookup;

    prepWhen = dvmGetRelativeTimeUsec();

    /*
     * Load all classes found in this DEX file.  If they fail to load for
     * some reason, they won't get verified (which is as it should be).
     */
    if (!loadAllClasses(pDvmDex))
        goto bail;
    loadWhen = dvmGetRelativeTimeUsec();

    /*
     * Verify all classes in the DEX file.  Export the "is verified" flag
     * to the DEX file we're creating.
     */
    if (doVerify) {
        dvmVerifyAllClasses(pDvmDex->pDexFile);
        *pHeaderFlags |= DEX_FLAG_VERIFIED;
    }
    verifyWhen = dvmGetRelativeTimeUsec();

    /*
     * Optimize the classes we successfully loaded.  If the opt mode is
     * OPTIMIZE_MODE_VERIFIED, each class must have been successfully
     * verified or we'll skip it.
     */
#ifndef PROFILE_FIELD_ACCESS
    if (doOpt) {
        optimizeLoadedClasses(pDvmDex->pDexFile);
        *pHeaderFlags |= DEX_OPT_FLAG_FIELDS | DEX_OPT_FLAG_INVOCATIONS;
    }
#endif
    optWhen = dvmGetRelativeTimeUsec();

    LOGD("DexOpt: load %dms, verify %dms, opt %dms\n",
        (int) (loadWhen - prepWhen) / 1000,
        (int) (verifyWhen - loadWhen) / 1000,
        (int) (optWhen - verifyWhen) / 1000);

    result = true;

bail:
    /* free up storage */
    dvmDexFileFree(pDvmDex);

    return result;
}

/*
 * Update the Adler-32 checksum stored in the DEX file.  This covers the
 * swapped and optimized DEX data, but does not include the opt header
 * or auxillary data.
 */
static void updateChecksum(u1* addr, int len, DexHeader* pHeader)
{
    /*
     * Rewrite the checksum.  We leave the SHA-1 signature alone.
     */
    uLong adler = adler32(0L, Z_NULL, 0);
    const int nonSum = sizeof(pHeader->magic) + sizeof(pHeader->checksum);

    adler = adler32(adler, addr + nonSum, len - nonSum);
    pHeader->checksum = adler;
}

/*
 * Try to load all classes in the specified DEX.  If they have some sort
 * of broken dependency, e.g. their superclass lives in a different DEX
 * that wasn't previously loaded into the bootstrap class path, loading
 * will fail.  This is the desired behavior.
 *
 * We have no notion of class loader at this point, so we load all of
 * the classes with the bootstrap class loader.  It turns out this has
 * exactly the behavior we want, and has no ill side effects because we're
 * running in a separate process and anything we load here will be forgotten.
 *
 * We set the CLASS_MULTIPLE_DEFS flag here if we see multiple definitions.
 * This works because we only call here as part of optimization / pre-verify,
 * not during verification as part of loading a class into a running VM.
 *
 * This returns "false" if the world is too screwed up to do anything
 * useful at all.
 */
static bool loadAllClasses(DvmDex* pDvmDex)
{
    u4 count = pDvmDex->pDexFile->pHeader->classDefsSize;
    u4 idx;
    int loaded = 0;

    LOGV("DexOpt: +++ trying to load %d classes\n", count);

    dvmSetBootPathExtraDex(pDvmDex);

    /*
     * We have some circularity issues with Class and Object that are most
     * easily avoided by ensuring that Object is never the first thing we
     * try to find.  Take care of that here.  (We only need to do this when
     * loading classes from the DEX file that contains Object, and only
     * when Object comes first in the list, but it costs very little to
     * do it in all cases.)
     */
    if (dvmFindSystemClass("Ljava/lang/Class;") == NULL) {
        LOGE("ERROR: java.lang.Class does not exist!\n");
        return false;
    }

    for (idx = 0; idx < count; idx++) {
        const DexClassDef* pClassDef;
        const char* classDescriptor;
        ClassObject* newClass;

        pClassDef = dexGetClassDef(pDvmDex->pDexFile, idx);
        classDescriptor =
            dexStringByTypeIdx(pDvmDex->pDexFile, pClassDef->classIdx);

        LOGV("+++  loading '%s'", classDescriptor);
        //newClass = dvmDefineClass(pDexFile, classDescriptor,
        //        NULL);
        newClass = dvmFindSystemClassNoInit(classDescriptor);
        if (newClass == NULL) {
            LOGV("DexOpt: failed loading '%s'\n", classDescriptor);
            dvmClearOptException(dvmThreadSelf());
        } else if (newClass->pDvmDex != pDvmDex) {
            /*
             * We don't load the new one, and we tag the first one found
             * with the "multiple def" flag so the resolver doesn't try
             * to make it available.
             */
            LOGD("DexOpt: '%s' has an earlier definition; blocking out\n",
                classDescriptor);
            SET_CLASS_FLAG(newClass, CLASS_MULTIPLE_DEFS);
        } else {
            loaded++;
        }
    }
    LOGV("DexOpt: +++ successfully loaded %d classes\n", loaded);

    dvmSetBootPathExtraDex(NULL);
    return true;
}


/*
 * Create a table of inline substitutions.
 *
 * TODO: this is currently just a linear array.  We will want to put this
 * into a hash table as the list size increases.
 */
static InlineSub* createInlineSubsTable(void)
{
    const InlineOperation* ops = dvmGetInlineOpsTable();
    const int count = dvmGetInlineOpsTableLength();
    InlineSub* table;
    Method* method;
    ClassObject* clazz;
    int i, tableIndex;

    /*
     * Allocate for optimism: one slot per entry, plus an end-of-list marker.
     */
    table = malloc(sizeof(InlineSub) * (count+1));

    tableIndex = 0;
    for (i = 0; i < count; i++) {
        clazz = dvmFindClassNoInit(ops[i].classDescriptor, NULL);
        if (clazz == NULL) {
            LOGV("DexOpt: can't inline for class '%s': not found\n",
                ops[i].classDescriptor);
            dvmClearOptException(dvmThreadSelf());
        } else {
            /*
             * Method could be virtual or direct.  Try both.  Don't use
             * the "hier" versions.
             */
            method = dvmFindDirectMethodByDescriptor(clazz, ops[i].methodName,
                        ops[i].methodSignature);
            if (method == NULL)
                method = dvmFindVirtualMethodByDescriptor(clazz, ops[i].methodName,
                        ops[i].methodSignature);
            if (method == NULL) {
                LOGW("DexOpt: can't inline %s.%s %s: method not found\n",
                    ops[i].classDescriptor, ops[i].methodName,
                    ops[i].methodSignature);
            } else {
                if (!dvmIsFinalClass(clazz) && !dvmIsFinalMethod(method)) {
                    LOGW("DexOpt: WARNING: inline op on non-final class/method "
                         "%s.%s\n",
                        clazz->descriptor, method->name);
                    /* fail? */
                }
                if (dvmIsSynchronizedMethod(method) ||
                    dvmIsDeclaredSynchronizedMethod(method))
                {
                    LOGW("DexOpt: WARNING: inline op on synchronized method "
                         "%s.%s\n",
                        clazz->descriptor, method->name);
                    /* fail? */
                }

                table[tableIndex].method = method;
                table[tableIndex].inlineIdx = i;
                tableIndex++;

                LOGV("DexOpt: will inline %d: %s.%s %s\n", i,
                    ops[i].classDescriptor, ops[i].methodName,
                    ops[i].methodSignature);
            }
        }
    }

    /* mark end of table */
    table[tableIndex].method = NULL;
    LOGV("DexOpt: inline table has %d entries\n", tableIndex);

    return table;
}

/*
 * Run through all classes that were successfully loaded from this DEX
 * file and optimize their code sections.
 */
static void optimizeLoadedClasses(DexFile* pDexFile)
{
    u4 count = pDexFile->pHeader->classDefsSize;
    u4 idx;
    InlineSub* inlineSubs = NULL;

    LOGV("DexOpt: +++ optimizing up to %d classes\n", count);
    assert(gDvm.dexOptMode != OPTIMIZE_MODE_NONE);

    inlineSubs = createInlineSubsTable();

    for (idx = 0; idx < count; idx++) {
        const DexClassDef* pClassDef;
        const char* classDescriptor;
        ClassObject* clazz;

        pClassDef = dexGetClassDef(pDexFile, idx);
        classDescriptor = dexStringByTypeIdx(pDexFile, pClassDef->classIdx);

        /* all classes are loaded into the bootstrap class loader */
        clazz = dvmLookupClass(classDescriptor, NULL, false);
        if (clazz != NULL) {
            if ((pClassDef->accessFlags & CLASS_ISPREVERIFIED) == 0 &&
                gDvm.dexOptMode == OPTIMIZE_MODE_VERIFIED)
            {
                LOGV("DexOpt: not optimizing '%s': not verified\n",
                    classDescriptor);
            } else if (clazz->pDvmDex->pDexFile != pDexFile) {
                /* shouldn't be here -- verifier should have caught */
                LOGD("DexOpt: not optimizing '%s': multiple definitions\n",
                    classDescriptor);
            } else {
                optimizeClass(clazz, inlineSubs);

                /* set the flag whether or not we actually did anything */
                ((DexClassDef*)pClassDef)->accessFlags |=
                    CLASS_ISOPTIMIZED;
            }
        } else {
            LOGV("DexOpt: not optimizing unavailable class '%s'\n",
                classDescriptor);
        }
    }

    free(inlineSubs);
}

/*
 * Optimize the specified class.
 */
static void optimizeClass(ClassObject* clazz, const InlineSub* inlineSubs)
{
    int i;

    for (i = 0; i < clazz->directMethodCount; i++) {
        if (!optimizeMethod(&clazz->directMethods[i], inlineSubs))
            goto fail;
    }
    for (i = 0; i < clazz->virtualMethodCount; i++) {
        if (!optimizeMethod(&clazz->virtualMethods[i], inlineSubs))
            goto fail;
    }

    return;

fail:
    LOGV("DexOpt: ceasing optimization attempts on %s\n", clazz->descriptor);
}

/*
 * Optimize instructions in a method.
 *
 * Returns "true" if all went well, "false" if we bailed out early when
 * something failed.
 */
static bool optimizeMethod(Method* method, const InlineSub* inlineSubs)
{
    u4 insnsSize;
    u2* insns;
    u2 inst;

    if (dvmIsNativeMethod(method) || dvmIsAbstractMethod(method))
        return true;

    insns = (u2*) method->insns;
    assert(insns != NULL);
    insnsSize = dvmGetMethodInsnsSize(method);

    while (insnsSize > 0) {
        int width;

        inst = *insns & 0xff;

        switch (inst) {
        case OP_IGET:
        case OP_IGET_BOOLEAN:
        case OP_IGET_BYTE:
        case OP_IGET_CHAR:
        case OP_IGET_SHORT:
            rewriteInstField(method, insns, OP_IGET_QUICK);
            break;
        case OP_IGET_WIDE:
            rewriteInstField(method, insns, OP_IGET_WIDE_QUICK);
            break;
        case OP_IGET_OBJECT:
            rewriteInstField(method, insns, OP_IGET_OBJECT_QUICK);
            break;
        case OP_IPUT:
        case OP_IPUT_BOOLEAN:
        case OP_IPUT_BYTE:
        case OP_IPUT_CHAR:
        case OP_IPUT_SHORT:
            rewriteInstField(method, insns, OP_IPUT_QUICK);
            break;
        case OP_IPUT_WIDE:
            rewriteInstField(method, insns, OP_IPUT_WIDE_QUICK);
            break;
        case OP_IPUT_OBJECT:
            rewriteInstField(method, insns, OP_IPUT_OBJECT_QUICK);
            break;

        case OP_INVOKE_VIRTUAL:
            if (!rewriteExecuteInline(method, insns, METHOD_VIRTUAL,inlineSubs))
            {
                if (!rewriteVirtualInvoke(method, insns, OP_INVOKE_VIRTUAL_QUICK))
                    return false;
            }
            break;
        case OP_INVOKE_VIRTUAL_RANGE:
            if (!rewriteVirtualInvoke(method, insns, OP_INVOKE_VIRTUAL_QUICK_RANGE))
                return false;
            break;
        case OP_INVOKE_SUPER:
            if (!rewriteVirtualInvoke(method, insns, OP_INVOKE_SUPER_QUICK))
                return false;
            break;
        case OP_INVOKE_SUPER_RANGE:
            if (!rewriteVirtualInvoke(method, insns, OP_INVOKE_SUPER_QUICK_RANGE))
                return false;
            break;

        case OP_INVOKE_DIRECT:
            if (!rewriteExecuteInline(method, insns, METHOD_DIRECT, inlineSubs))
            {
                if (!rewriteDirectInvoke(method, insns))
                    return false;
            }
            break;
        case OP_INVOKE_STATIC:
            rewriteExecuteInline(method, insns, METHOD_STATIC, inlineSubs);
            break;

        default:
            // ignore this instruction
            ;
        }

        if (*insns == kPackedSwitchSignature) {
            width = 4 + insns[1] * 2;
        } else if (*insns == kSparseSwitchSignature) {
            width = 2 + insns[1] * 4;
        } else if (*insns == kArrayDataSignature) {
            u2 elemWidth = insns[1];
            u4 len = insns[2] | (((u4)insns[3]) << 16);
            width = 4 + (elemWidth * len + 1) / 2;
        } else {
            width = dexGetInstrWidth(gDvm.instrWidth, inst);
        }
        assert(width > 0);

        insns += width;
        insnsSize -= width;
    }

    assert(insnsSize == 0);
    return true;
}


/*
 * If "referrer" and "resClass" don't come from the same DEX file, and
 * the DEX we're working on is not destined for the bootstrap class path,
 * tweak the class loader so package-access checks work correctly.
 *
 * Only do this if we're doing pre-verification or optimization.
 */
static void tweakLoader(ClassObject* referrer, ClassObject* resClass)
{
    if (!gDvm.optimizing)
        return;
    assert(referrer->classLoader == NULL);
    assert(resClass->classLoader == NULL);

    if (!gDvm.optimizingBootstrapClass) {
        /* class loader for an array class comes from element type */
        if (dvmIsArrayClass(resClass))
            resClass = resClass->elementClass;
        if (referrer->pDvmDex != resClass->pDvmDex)
            resClass->classLoader = (Object*) 0xdead3333;
    }
}

/*
 * Undo the effects of tweakLoader.
 */
static void untweakLoader(ClassObject* referrer, ClassObject* resClass)
{
    if (!gDvm.optimizing || gDvm.optimizingBootstrapClass)
        return;

    if (dvmIsArrayClass(resClass))
        resClass = resClass->elementClass;
    resClass->classLoader = NULL;
}


/*
 * Alternate version of dvmResolveClass for use with verification and
 * optimization.  Performs access checks on every resolve, and refuses
 * to acknowledge the existence of classes defined in more than one DEX
 * file.
 *
 * Exceptions caused by failures are cleared before returning.
 */
ClassObject* dvmOptResolveClass(ClassObject* referrer, u4 classIdx)
{
    DvmDex* pDvmDex = referrer->pDvmDex;
    ClassObject* resClass;

    /*
     * Check the table first.  If not there, do the lookup by name.
     */
    resClass = dvmDexGetResolvedClass(pDvmDex, classIdx);
    if (resClass == NULL) {
        const char* className = dexStringByTypeIdx(pDvmDex->pDexFile, classIdx);
        if (className[0] != '\0' && className[1] == '\0') {
            /* primitive type */
            resClass = dvmFindPrimitiveClass(className[0]);
        } else {
            resClass = dvmFindClassNoInit(className, referrer->classLoader);
        }
        if (resClass == NULL) {
            /* not found, exception should be raised */
            LOGV("DexOpt: class %d (%s) not found\n",
                classIdx,
                dexStringByTypeIdx(pDvmDex->pDexFile, classIdx));
            dvmClearOptException(dvmThreadSelf());
            return NULL;
        }

        /*
         * Add it to the resolved table so we're faster on the next lookup.
         */
        dvmDexSetResolvedClass(pDvmDex, classIdx, resClass);
    }

    /* multiple definitions? */
    if (IS_CLASS_FLAG_SET(resClass, CLASS_MULTIPLE_DEFS)) {
        LOGI("DexOpt: not resolving ambiguous class '%s'\n",
            resClass->descriptor);
        return NULL;
    }

    /* access allowed? */
    tweakLoader(referrer, resClass);
    bool allowed = dvmCheckClassAccess(referrer, resClass);
    untweakLoader(referrer, resClass);
    if (!allowed) {
        LOGW("DexOpt: resolve class illegal access: %s -> %s\n",
            referrer->descriptor, resClass->descriptor);
        return NULL;
    }

    return resClass;
}

/*
 * Alternate version of dvmResolveInstField().
 */
InstField* dvmOptResolveInstField(ClassObject* referrer, u4 ifieldIdx)
{
    DvmDex* pDvmDex = referrer->pDvmDex;
    InstField* resField;

    resField = (InstField*) dvmDexGetResolvedField(pDvmDex, ifieldIdx);
    if (resField == NULL) {
        const DexFieldId* pFieldId;
        ClassObject* resClass;

        pFieldId = dexGetFieldId(pDvmDex->pDexFile, ifieldIdx);

        /*
         * Find the field's class.
         */
        resClass = dvmOptResolveClass(referrer, pFieldId->classIdx);
        if (resClass == NULL) {
            //dvmClearOptException(dvmThreadSelf());
            assert(!dvmCheckException(dvmThreadSelf()));
            return NULL;
        }

        resField = dvmFindInstanceFieldHier(resClass,
            dexStringById(pDvmDex->pDexFile, pFieldId->nameIdx),
            dexStringByTypeIdx(pDvmDex->pDexFile, pFieldId->typeIdx));
        if (resField == NULL) {
            LOGD("DexOpt: couldn't find field %s.%s\n",
                resClass->descriptor,
                dexStringById(pDvmDex->pDexFile, pFieldId->nameIdx));
            return NULL;
        }

        /*
         * Add it to the resolved table so we're faster on the next lookup.
         */
        dvmDexSetResolvedField(pDvmDex, ifieldIdx, (Field*) resField);
    }

    /* access allowed? */
    tweakLoader(referrer, resField->field.clazz);
    bool allowed = dvmCheckFieldAccess(referrer, (Field*)resField);
    untweakLoader(referrer, resField->field.clazz);
    if (!allowed) {
        LOGI("DexOpt: access denied from %s to field %s.%s\n",
            referrer->descriptor, resField->field.clazz->descriptor,
            resField->field.name);
        return NULL;
    }

    return resField;
}

/*
 * Alternate version of dvmResolveStaticField().
 *
 * Does not force initialization of the resolved field's class.
 */
StaticField* dvmOptResolveStaticField(ClassObject* referrer, u4 sfieldIdx)
{
    DvmDex* pDvmDex = referrer->pDvmDex;
    StaticField* resField;

    resField = (StaticField*)dvmDexGetResolvedField(pDvmDex, sfieldIdx);
    if (resField == NULL) {
        const DexFieldId* pFieldId;
        ClassObject* resClass;

        pFieldId = dexGetFieldId(pDvmDex->pDexFile, sfieldIdx);

        /*
         * Find the field's class.
         */
        resClass = dvmOptResolveClass(referrer, pFieldId->classIdx);
        if (resClass == NULL) {
            //dvmClearOptException(dvmThreadSelf());
            assert(!dvmCheckException(dvmThreadSelf()));
            return NULL;
        }

        resField = dvmFindStaticFieldHier(resClass,
                    dexStringById(pDvmDex->pDexFile, pFieldId->nameIdx),
                    dexStringByTypeIdx(pDvmDex->pDexFile, pFieldId->typeIdx));
        if (resField == NULL) {
            LOGD("DexOpt: couldn't find static field\n");
            return NULL;
        }

        /*
         * Add it to the resolved table so we're faster on the next lookup.
         *
         * We can only do this if we're in "dexopt", because the presence
         * of a valid value in the resolution table implies that the class
         * containing the static field has been initialized.
         */
        if (gDvm.optimizing)
            dvmDexSetResolvedField(pDvmDex, sfieldIdx, (Field*) resField);
    }

    /* access allowed? */
    tweakLoader(referrer, resField->field.clazz);
    bool allowed = dvmCheckFieldAccess(referrer, (Field*)resField);
    untweakLoader(referrer, resField->field.clazz);
    if (!allowed) {
        LOGI("DexOpt: access denied from %s to field %s.%s\n",
            referrer->descriptor, resField->field.clazz->descriptor,
            resField->field.name);
        return NULL;
    }

    return resField;
}


/*
 * Rewrite an iget/iput instruction.  These all have the form:
 *   op vA, vB, field@CCCC
 *
 * Where vA holds the value, vB holds the object reference, and CCCC is
 * the field reference constant pool offset.  We want to replace CCCC
 * with the byte offset from the start of the object.
 *
 * "clazz" is the referring class.  We need this because we verify
 * access rights here.
 */
static void rewriteInstField(Method* method, u2* insns, OpCode newOpc)
{
    ClassObject* clazz = method->clazz;
    u2 fieldIdx = insns[1];
    InstField* field;
    int byteOffset;

    field = dvmOptResolveInstField(clazz, fieldIdx);
    if (field == NULL) {
        LOGI("DexOpt: unable to optimize field ref 0x%04x at 0x%02x in %s.%s\n",
            fieldIdx, (int) (insns - method->insns), clazz->descriptor,
            method->name);
        return;
    }

    if (field->byteOffset >= 65536) {
        LOGI("DexOpt: field offset exceeds 64K (%d)\n", field->byteOffset);
        return;
    }

    insns[0] = (insns[0] & 0xff00) | (u2) newOpc;
    insns[1] = (u2) field->byteOffset;
    LOGVV("DexOpt: rewrote access to %s.%s --> %d\n",
        field->field.clazz->descriptor, field->field.name,
        field->byteOffset);
}

/*
 * Alternate version of dvmResolveMethod().
 *
 * Doesn't throw exceptions, and checks access on every lookup.
 */
Method* dvmOptResolveMethod(ClassObject* referrer, u4 methodIdx,
    MethodType methodType)
{
    DvmDex* pDvmDex = referrer->pDvmDex;
    Method* resMethod;

    assert(methodType != METHOD_INTERFACE);

    LOGVV("--- resolving method %u (referrer=%s)\n", methodIdx,
        referrer->descriptor);

    resMethod = dvmDexGetResolvedMethod(pDvmDex, methodIdx);
    if (resMethod == NULL) {
        const DexMethodId* pMethodId;
        ClassObject* resClass;

        pMethodId = dexGetMethodId(pDvmDex->pDexFile, methodIdx);

        resClass = dvmOptResolveClass(referrer, pMethodId->classIdx);
        if (resClass == NULL) {
            /* can't find the class that the method is a part of */
            LOGV("DexOpt: can't find called method's class (?.%s)\n",
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx));
            return NULL;
        }
        if (dvmIsInterfaceClass(resClass)) {
            /* method is part of an interface; this is wrong method for that */
            LOGW("DexOpt: method is in an interface\n");
            return NULL;
        }

        /*
         * We need to chase up the class hierarchy to find methods defined
         * in super-classes.  (We only want to check the current class
         * if we're looking for a constructor.)
         */
        DexProto proto;
        dexProtoSetFromMethodId(&proto, pDvmDex->pDexFile, pMethodId);

        if (methodType == METHOD_DIRECT) {
            resMethod = dvmFindDirectMethod(resClass,
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx), &proto);
        } else if (methodType == METHOD_STATIC) {
            resMethod = dvmFindDirectMethodHier(resClass,
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx), &proto);
        } else {
            resMethod = dvmFindVirtualMethodHier(resClass,
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx), &proto);
        }

        if (resMethod == NULL) {
            LOGV("DexOpt: couldn't find method '%s'\n",
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx));
            return NULL;
        }

        /* see if this is a pure-abstract method */
        if (dvmIsAbstractMethod(resMethod) && !dvmIsAbstractClass(resClass)) {
            LOGW("DexOpt: pure-abstract method '%s' in %s\n",
                dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx),
                resClass->descriptor);
            return NULL;
        }

        /*
         * Add it to the resolved table so we're faster on the next lookup.
         *
         * We can only do this for static methods if we're not in "dexopt",
         * because the presence of a valid value in the resolution table
         * implies that the class containing the static field has been
         * initialized.
         */
        if (methodType != METHOD_STATIC || gDvm.optimizing)
            dvmDexSetResolvedMethod(pDvmDex, methodIdx, resMethod);
    }

    LOGVV("--- found method %d (%s.%s)\n",
        methodIdx, resMethod->clazz->descriptor, resMethod->name);

    /* access allowed? */
    tweakLoader(referrer, resMethod->clazz);
    bool allowed = dvmCheckMethodAccess(referrer, resMethod);
    untweakLoader(referrer, resMethod->clazz);
    if (!allowed) {
        IF_LOGI() {
            char* desc = dexProtoCopyMethodDescriptor(&resMethod->prototype);
            LOGI("DexOpt: illegal method access (call %s.%s %s from %s)\n",
                resMethod->clazz->descriptor, resMethod->name, desc,
                referrer->descriptor);
            free(desc);
        }
        return NULL;
    }

    return resMethod;
}

/*
 * Rewrite invoke-virtual, invoke-virtual/range, invoke-super, and
 * invoke-super/range.  These all have the form:
 *   op vAA, meth@BBBB, reg stuff @CCCC
 *
 * We want to replace the method constant pool index BBBB with the
 * vtable index.
 */
static bool rewriteVirtualInvoke(Method* method, u2* insns, OpCode newOpc)
{
    ClassObject* clazz = method->clazz;
    Method* baseMethod;
    u2 methodIdx = insns[1];

    baseMethod = dvmOptResolveMethod(clazz, methodIdx, METHOD_VIRTUAL);
    if (baseMethod == NULL) {
        LOGD("DexOpt: unable to optimize virt call 0x%04x at 0x%02x in %s.%s\n",
            methodIdx,
            (int) (insns - method->insns), clazz->descriptor,
            method->name);
        return false;
    }

    assert((insns[0] & 0xff) == OP_INVOKE_VIRTUAL ||
           (insns[0] & 0xff) == OP_INVOKE_VIRTUAL_RANGE ||
           (insns[0] & 0xff) == OP_INVOKE_SUPER ||
           (insns[0] & 0xff) == OP_INVOKE_SUPER_RANGE);

    /*
     * Note: Method->methodIndex is a u2 and is range checked during the
     * initial load.
     */
    insns[0] = (insns[0] & 0xff00) | (u2) newOpc;
    insns[1] = baseMethod->methodIndex;

    //LOGI("DexOpt: rewrote call to %s.%s --> %s.%s\n",
    //    method->clazz->descriptor, method->name,
    //    baseMethod->clazz->descriptor, baseMethod->name);

    return true;
}

/*
 * Rewrite invoke-direct, which has the form:
 *   op vAA, meth@BBBB, reg stuff @CCCC
 *
 * There isn't a lot we can do to make this faster, but in some situations
 * we can make it go away entirely.
 *
 * This must only be used when the invoked method does nothing and has
 * no return value (the latter being very important for verification).
 */
static bool rewriteDirectInvoke(Method* method, u2* insns)
{
    ClassObject* clazz = method->clazz;
    Method* calledMethod;
    u2 methodIdx = insns[1];

    calledMethod = dvmOptResolveMethod(clazz, methodIdx, METHOD_DIRECT);
    if (calledMethod == NULL) {
        LOGD("DexOpt: unable to opt direct call 0x%04x at 0x%02x in %s.%s\n",
            methodIdx,
            (int) (insns - method->insns), clazz->descriptor,
            method->name);
        return false;
    }

    /* TODO: verify that java.lang.Object() is actually empty! */
    if (calledMethod->clazz == gDvm.classJavaLangObject &&
        dvmCompareNameDescriptorAndMethod("<init>", "()V", calledMethod) == 0)
    {
        /*
         * Replace with "empty" instruction.  DO NOT disturb anything
         * else about it, as we want it to function the same as
         * OP_INVOKE_DIRECT when debugging is enabled.
         */
        assert((insns[0] & 0xff) == OP_INVOKE_DIRECT);
        insns[0] = (insns[0] & 0xff00) | (u2) OP_INVOKE_DIRECT_EMPTY;

        //LOGI("DexOpt: marked-empty call to %s.%s --> %s.%s\n",
        //    method->clazz->descriptor, method->name,
        //    calledMethod->clazz->descriptor, calledMethod->name);
    }

    return true;
}

/*
 * Resolve an interface method reference.
 *
 * Returns NULL if the method was not found.  Does not throw an exception.
 */
Method* dvmOptResolveInterfaceMethod(ClassObject* referrer, u4 methodIdx)
{
    DvmDex* pDvmDex = referrer->pDvmDex;
    Method* resMethod;
    int i;

    LOGVV("--- resolving interface method %d (referrer=%s)\n",
        methodIdx, referrer->descriptor);

    resMethod = dvmDexGetResolvedMethod(pDvmDex, methodIdx);
    if (resMethod == NULL) {
        const DexMethodId* pMethodId;
        ClassObject* resClass;

        pMethodId = dexGetMethodId(pDvmDex->pDexFile, methodIdx);

        resClass = dvmOptResolveClass(referrer, pMethodId->classIdx);
        if (resClass == NULL) {
            /* can't find the class that the method is a part of */
            dvmClearOptException(dvmThreadSelf());
            return NULL;
        }
        if (!dvmIsInterfaceClass(resClass)) {
            /* whoops */
            LOGI("Interface method not part of interface class\n");
            return NULL;
        }

        const char* methodName =
            dexStringById(pDvmDex->pDexFile, pMethodId->nameIdx);
        DexProto proto;
        dexProtoSetFromMethodId(&proto, pDvmDex->pDexFile, pMethodId);

        LOGVV("+++ looking for '%s' '%s' in resClass='%s'\n",
            methodName, methodSig, resClass->descriptor);
        resMethod = dvmFindVirtualMethod(resClass, methodName, &proto);
        if (resMethod == NULL) {
            /* scan superinterfaces and superclass interfaces */
            LOGVV("+++ did not resolve immediately\n");
            for (i = 0; i < resClass->iftableCount; i++) {
                resMethod = dvmFindVirtualMethod(resClass->iftable[i].clazz,
                                methodName, &proto);
                if (resMethod != NULL)
                    break;
            }

            if (resMethod == NULL) {
                LOGVV("+++ unable to resolve method %s\n", methodName);
                return NULL;
            }
        } else {
            LOGVV("+++ resolved immediately: %s (%s %d)\n", resMethod->name,
                resMethod->clazz->descriptor, (u4) resMethod->methodIndex);
        }

        /* we're expecting this to be abstract */
        if (!dvmIsAbstractMethod(resMethod)) {
            char* desc = dexProtoCopyMethodDescriptor(&resMethod->prototype);
            LOGW("Found non-abstract interface method %s.%s %s\n",
                resMethod->clazz->descriptor, resMethod->name, desc);
            free(desc);
            return NULL;
        }

        /*
         * Add it to the resolved table so we're faster on the next lookup.
         */
        dvmDexSetResolvedMethod(pDvmDex, methodIdx, resMethod);
    }

    LOGVV("--- found interface method %d (%s.%s)\n",
        methodIdx, resMethod->clazz->descriptor, resMethod->name);

    /* interface methods are always public; no need to check access */

    return resMethod;
}
/*
 * See if the method being called can be rewritten as an inline operation.
 * Works for invoke-virtual, invoke-direct, and invoke-static.
 *
 * Returns "true" if we replace it.
 */
static bool rewriteExecuteInline(Method* method, u2* insns,
    MethodType methodType, const InlineSub* inlineSubs)
{
    ClassObject* clazz = method->clazz;
    Method* calledMethod;
    u2 methodIdx = insns[1];

    //return false;

    calledMethod = dvmOptResolveMethod(clazz, methodIdx, methodType);
    if (calledMethod == NULL) {
        LOGV("+++ DexOpt inline: can't find %d\n", methodIdx);
        return false;
    }

    while (inlineSubs->method != NULL) {
        /*
        if (extra) {
            LOGI("comparing %p vs %p %s.%s %s\n",
                inlineSubs->method, calledMethod,
                inlineSubs->method->clazz->descriptor,
                inlineSubs->method->name,
                inlineSubs->method->signature);
        }
        */
        if (inlineSubs->method == calledMethod) {
            assert((insns[0] & 0xff) == OP_INVOKE_DIRECT ||
                   (insns[0] & 0xff) == OP_INVOKE_STATIC ||
                   (insns[0] & 0xff) == OP_INVOKE_VIRTUAL);
            insns[0] = (insns[0] & 0xff00) | (u2) OP_EXECUTE_INLINE;
            insns[1] = (u2) inlineSubs->inlineIdx;

            //LOGI("DexOpt: execute-inline %s.%s --> %s.%s\n",
            //    method->clazz->descriptor, method->name,
            //    calledMethod->clazz->descriptor, calledMethod->name);
            return true;
        }

        inlineSubs++;
    }

    return false;
}

