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
 * Command-line DEX optimization and verification entry point.
 *
 * There are two ways to launch this:
 * (1) From the VM.  This takes a dozen args, one of which is a file
 *     descriptor that acts as both input and output.  This allows us to
 *     remain ignorant of where the DEX data originally came from.
 * (2) From installd or another native application.  Pass in a file
 *     descriptor for a zip file, a file descriptor for the output, and
 *     a filename for debug messages.  Many assumptions are made about
 *     what's going on (verification + optimization are enabled, boot
 *     class path is in BOOTCLASSPATH, etc).
 *
 * There are some fragile aspects around bootclasspath entries, owing
 * largely to the VM's history of working on whenever it thought it needed
 * instead of strictly doing what it was told.  If optimizing bootclasspath
 * entries, always do them in the order in which they appear in the path.
 */
#include "Dalvik.h"
#include "libdex/OptInvocation.h"

#include "utils/Log.h"
#include "cutils/process_name.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

static const char* kClassesDex = "classes.dex";


/*
 * Extract "classes.dex" from zipFd into "cacheFd", leaving a little space
 * up front for the DEX optimization header.
 */
static int extractAndProcessZip(int zipFd, int cacheFd,
    const char* debugFileName, int isBootstrap, const char* bootClassPath,
    const char* dexoptFlagStr)
{
    ZipArchive zippy;
    ZipEntry zipEntry;
    long uncompLen, modWhen, crc32;
    off_t dexOffset;
    int err;
    int result = -1;

    memset(&zippy, 0, sizeof(zippy));

    /* make sure we're still at the start of an empty file */
    if (lseek(cacheFd, 0, SEEK_END) != 0) {
        LOGE("DexOptZ: new cache file '%s' is not empty\n", debugFileName);
        goto bail;
    }

    /*
     * Write a skeletal DEX optimization header.  We want the classes.dex
     * to come just after it.
     */
    err = dexOptCreateEmptyHeader(cacheFd);
    if (err != 0)
        goto bail;

    /* record the file position so we can get back here later */
    dexOffset = lseek(cacheFd, 0, SEEK_CUR);
    if (dexOffset < 0)
        goto bail;

    /*
     * Open the zip archive, find the DEX entry.
     */
    if (dexZipPrepArchive(zipFd, debugFileName, &zippy) != 0) {
        LOGW("DexOptZ: unable to open zip archive '%s'\n", debugFileName);
        goto bail;
    }

    zipEntry = dexZipFindEntry(&zippy, kClassesDex);
    if (zipEntry == NULL) {
        LOGW("DexOptZ: zip archive '%s' does not include %s\n",
            debugFileName, kClassesDex);
        goto bail;
    }

    /*
     * Extract some info about the zip entry.
     */
    if (!dexZipGetEntryInfo(&zippy, zipEntry, NULL, &uncompLen, NULL, NULL,
            &modWhen, &crc32))
    {
        LOGW("DexOptZ: zip archive GetEntryInfo failed on %s\n", debugFileName);
        goto bail;
    }

    uncompLen = uncompLen;
    modWhen = modWhen;
    crc32 = crc32;

    /*
     * Extract the DEX data into the cache file at the current offset.
     */
    if (!dexZipExtractEntryToFile(&zippy, zipEntry, cacheFd)) {
        LOGW("DexOptZ: extraction of %s from %s failed\n",
            kClassesDex, debugFileName);
        goto bail;
    }

    /*
     * Prep the VM and perform the optimization.
     */
    DexClassVerifyMode verifyMode = VERIFY_MODE_ALL;
    DexOptimizerMode dexOptMode = OPTIMIZE_MODE_VERIFIED;
    int dexoptFlags = 0;        /* bit flags, from enum DexoptFlags */
    if (dexoptFlagStr[0] != '\0') {
        const char* opc;
        const char* val;

        opc = strstr(dexoptFlagStr, "v=");      /* verification */
        if (opc != NULL) {
            switch (*(opc+2)) {
            case 'n':   verifyMode = VERIFY_MODE_NONE;          break;
            case 'r':   verifyMode = VERIFY_MODE_REMOTE;        break;
            case 'a':   verifyMode = VERIFY_MODE_ALL;           break;
            default:                                            break;
            }
        }

        opc = strstr(dexoptFlagStr, "o=");      /* optimization */
        if (opc != NULL) {
            switch (*(opc+2)) {
            case 'n':   dexOptMode = OPTIMIZE_MODE_NONE;        break;
            case 'v':   dexOptMode = OPTIMIZE_MODE_VERIFIED;    break;
            case 'a':   dexOptMode = OPTIMIZE_MODE_ALL;         break;
            default:                                            break;
            }
        }

        opc = strstr(dexoptFlagStr, "m=y");     /* register map */
        if (opc != NULL) {
            dexoptFlags |= DEXOPT_GEN_REGISTER_MAPS;
        }
    }
    if (dvmPrepForDexOpt(bootClassPath, dexOptMode, verifyMode,
            dexoptFlags) != 0)
    {
        LOGE("DexOptZ: VM init failed\n");
        goto bail;
    }

    //vmStarted = 1;

    /* do the optimization */
    if (!dvmContinueOptimization(cacheFd, dexOffset, uncompLen, debugFileName,
            modWhen, crc32, isBootstrap))
    {
        LOGE("Optimization failed\n");
        goto bail;
    }

    /* we don't shut the VM down -- process is about to exit */

    result = 0;

bail:
    dexZipCloseArchive(&zippy);
    return result;
}


/* advance to the next arg and extract it */
#define GET_ARG(_var, _func, _msg)                                          \
    {                                                                       \
        char* endp;                                                         \
        (_var) = _func(*++argv, &endp, 0);                                  \
        if (*endp != '\0') {                                                \
            LOGE("%s '%s'", _msg, *argv);                                   \
            goto bail;                                                      \
        }                                                                   \
        --argc;                                                             \
    }

/*
 * Parse arguments.  We want:
 *   0. (name of dexopt command -- ignored)
 *   1. "--zip"
 *   2. zip fd (input, read-only)
 *   3. cache fd (output, read-write, locked with flock)
 *   4. filename of file being optimized (used for debug messages and
 *      for comparing against BOOTCLASSPATH -- does not need to be
 *      accessible or even exist)
 *   5. dexopt flags
 *
 * The BOOTCLASSPATH environment variable is assumed to hold the correct
 * boot class path.  If the filename provided appears in the boot class
 * path, the path will be truncated just before that entry (so that, if
 * you were to dexopt "core.jar", your bootclasspath would be empty).
 *
 * This does not try to normalize the boot class path name, so the
 * filename test won't catch you if you get creative.
 */
static int fromZip(int argc, char* const argv[])
{
    int result = -1;
    int zipFd, cacheFd, vmBuildVersion;
    const char* inputFileName;
    char* bcpCopy = NULL;
    const char* dexoptFlagStr;

    if (argc != 6) {
        LOGE("Wrong number of args for --zip (found %d)\n", argc);
        goto bail;
    }

    /* skip "--zip" */
    argc--;
    argv++;

    GET_ARG(zipFd, strtol, "bad zip fd");
    GET_ARG(cacheFd, strtol, "bad cache fd");
    inputFileName = *++argv;
    --argc;
    dexoptFlagStr = *++argv;
    --argc;

    /*
     * Check to see if this is a bootstrap class entry.  If so, truncate
     * the path.
     */
    const char* bcp = getenv("BOOTCLASSPATH");
    if (bcp == NULL) {
        LOGE("DexOptZ: BOOTCLASSPATH not set\n");
        goto bail;
    }

    int isBootstrap = false;
    const char* match = strstr(bcp, inputFileName);
    if (match != NULL) {
        /*
         * TODO: we have a partial string match, but that doesn't mean
         * we've matched an entire path component.  We should make sure
         * that we're matching on the full inputFileName, and if not we
         * should re-do the strstr starting at (match+1).
         *
         * The scenario would be a bootclasspath with something like
         * "/system/framework/core.jar" while we're trying to optimize
         * "/framework/core.jar".  Not very likely since all paths are
         * absolute and end with ".jar", but not impossible.
         */
        int matchOffset = match - bcp;
        if (matchOffset > 0 && bcp[matchOffset-1] == ':')
            matchOffset--;
        LOGV("DexOptZ: found '%s' in bootclasspath, cutting off at %d\n",
            inputFileName, matchOffset);
        bcpCopy = strdup(bcp);
        bcpCopy[matchOffset] = '\0';

        bcp = bcpCopy;
        LOGD("DexOptZ: truncated BOOTCLASSPATH to '%s'\n", bcp);
        isBootstrap = true;
    }

    result = extractAndProcessZip(zipFd, cacheFd, inputFileName,
                isBootstrap, bcp, dexoptFlagStr);

bail:
    free(bcpCopy);
    return result;
}

/*
 * Parse arguments for an "old-style" invocation directly from the VM.
 *
 * Here's what we want:
 *   0. (name of dexopt command -- ignored)
 *   1. "--dex"
 *   2. DALVIK_VM_BUILD value, as a sanity check
 *   3. file descriptor, locked with flock, for DEX file being optimized
 *   4. DEX offset within file
 *   5. DEX length
 *   6. filename of file being optimized (for debug messages only)
 *   7. modification date of source (goes into dependency section)
 *   8. CRC of source (goes into dependency section)
 *   9. flags (optimization level, isBootstrap)
 *  10. bootclasspath entry #1
 *  11. bootclasspath entry #2
 *   ...
 *
 * dvmOptimizeDexFile() in dalvik/vm/analysis/DexOptimize.c builds the
 * argument list and calls this executable.
 *
 * The bootclasspath entries become the dependencies for this DEX file.
 *
 * The open file descriptor MUST NOT be for one of the bootclasspath files.
 * The parent has the descriptor locked, and we'll try to lock it again as
 * part of processing the bootclasspath.  (We can catch this and return
 * an error by comparing filenames or by opening the bootclasspath files
 * and stat()ing them for inode numbers).
 */
static int fromDex(int argc, char* const argv[])
{
    int result = -1;
    bool vmStarted = false;
    char* bootClassPath = NULL;
    int fd, flags, vmBuildVersion;
    long offset, length;
    const char* debugFileName;
    u4 crc, modWhen;
    char* endp;

    if (argc < 10) {
        /* don't have all mandatory args */
        LOGE("Not enough arguments for --dex (found %d)\n", argc);
        goto bail;
    }

    /* skip "--dex" */
    argc--;
    argv++;

    /*
     * Extract the args.
     */
    GET_ARG(vmBuildVersion, strtol, "bad vm build");
    if (vmBuildVersion != DALVIK_VM_BUILD) {
        LOGE("Inconsistent build rev: %d vs %d\n",
            vmBuildVersion, DALVIK_VM_BUILD);
        goto bail;
    }
    GET_ARG(fd, strtol, "bad fd");
    GET_ARG(offset, strtol, "bad offset");
    GET_ARG(length, strtol, "bad length");
    debugFileName = *++argv;
    --argc;
    GET_ARG(modWhen, strtoul, "bad modWhen");
    GET_ARG(crc, strtoul, "bad crc");
    GET_ARG(flags, strtol, "bad flags");

    LOGV("Args: fd=%d off=%ld len=%ld name='%s' mod=0x%x crc=0x%x flg=%d (argc=%d)\n",
        fd, offset, length, debugFileName, modWhen, crc, flags, argc);
    assert(argc > 0);

    if (--argc == 0) {
        bootClassPath = strdup("");
    } else {
        int i, bcpLen;
        char* const* argp;
        char* cp;

        bcpLen = 0;
        for (i = 0, argp = argv; i < argc; i++) {
            ++argp;
            LOGV("DEP: '%s'\n", *argp);
            bcpLen += strlen(*argp) + 1;
        }

        cp = bootClassPath = (char*) malloc(bcpLen +1);
        for (i = 0, argp = argv; i < argc; i++) {
            int strLen;

            ++argp;
            strLen = strlen(*argp);
            if (i != 0)
                *cp++ = ':';
            memcpy(cp, *argp, strLen);
            cp += strLen;
        }
        *cp = '\0';

        assert((int) strlen(bootClassPath) == bcpLen-1);
    }
    LOGV("  bootclasspath is '%s'\n", bootClassPath);

    /* start the VM partway */
    bool onlyOptVerifiedDex = false;
    DexClassVerifyMode verifyMode;
    DexOptimizerMode dexOptMode;
    int dexoptFlags = 0;

    /* ugh -- upgrade these to a bit field if they get any more complex */
    if ((flags & DEXOPT_VERIFY_ENABLED) != 0) {
        if ((flags & DEXOPT_VERIFY_ALL) != 0)
            verifyMode = VERIFY_MODE_ALL;
        else
            verifyMode = VERIFY_MODE_REMOTE;
    } else {
        verifyMode = VERIFY_MODE_NONE;
    }
    if ((flags & DEXOPT_OPT_ENABLED) != 0) {
        if ((flags & DEXOPT_OPT_ALL) != 0)
            dexOptMode = OPTIMIZE_MODE_ALL;
        else
            dexOptMode = OPTIMIZE_MODE_VERIFIED;
    } else {
        dexOptMode = OPTIMIZE_MODE_NONE;
    }
    if ((flags & DEXOPT_GEN_REGISTER_MAP) != 0) {
        dexoptFlags |= DEXOPT_GEN_REGISTER_MAPS;
    }

    if (dvmPrepForDexOpt(bootClassPath, dexOptMode, verifyMode,
            dexoptFlags) != 0)
    {
        LOGE("VM init failed\n");
        goto bail;
    }

    vmStarted = true;

    /* do the optimization */
    if (!dvmContinueOptimization(fd, offset, length, debugFileName,
            modWhen, crc, (flags & DEXOPT_IS_BOOTSTRAP) != 0))
    {
        LOGE("Optimization failed\n");
        goto bail;
    }

    result = 0;

bail:
    /*
     * In theory we should gracefully shut the VM down at this point.  In
     * practice that only matters if we're checking for memory leaks with
     * valgrind -- simply exiting is much faster.
     *
     * As it turns out, the DEX optimizer plays a little fast and loose
     * with class loading.  We load all of the classes from a partially-
     * formed DEX file, which is unmapped when we're done.  If we want to
     * do clean shutdown here, perhaps for testing with valgrind, we need
     * to skip the munmap call there.
     */
#if 0
    if (vmStarted) {
        LOGI("DexOpt shutting down, result=%d\n", result);
        dvmShutdown();
    }
#endif

    //dvmLinearAllocDump(NULL);

#if 0
    {
        extern int gDvm__totalInstr, gDvm__gcInstr, gDvm__gcData,
               gDvm__gcSimpleData;
        LOGI("GC DATA: totinst=%d, gcinst=%d, gcdata=%d simpled=%d\n",
            gDvm__totalInstr, gDvm__gcInstr, gDvm__gcData, gDvm__gcSimpleData);
    }
#endif

    free(bootClassPath);
    LOGV("DexOpt command complete (result=%d)\n", result);
    return result;
}

/*
 * Main entry point.  Decide where to go.
 */
int main(int argc, char* const argv[])
{
    set_process_name("dexopt");

    setvbuf(stdout, NULL, _IONBF, 0);

    if (argc > 1) {
        if (strcmp(argv[1], "--zip") == 0)
            return fromZip(argc, argv);
        else if (strcmp(argv[1], "--dex") == 0)
            return fromDex(argc, argv);
    }

    fprintf(stderr, "Usage: don't use this\n");
    return 1;
}

