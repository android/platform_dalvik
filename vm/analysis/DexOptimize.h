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
 * DEX optimization declarations.
 */
#ifndef _DALVIK_DEXOPTIMIZE
#define _DALVIK_DEXOPTIMIZE

/*
 * Global DEX optimizer control.  Determines the circumstances in which we
 * try to rewrite instructions in the DEX file.
 */
typedef enum DexOptimizerMode {
    OPTIMIZE_MODE_UNKNOWN = 0,
    OPTIMIZE_MODE_NONE,         /* never optimize */
    OPTIMIZE_MODE_VERIFIED,     /* only optimize verified classes (default) */
    OPTIMIZE_MODE_ALL           /* optimize all classes */
} DexOptimizerMode;

/* some additional bit flags for dexopt */
enum DexoptFlags {
    DEXOPT_GEN_REGISTER_MAPS = 1, /* generate register maps during verify */
};

/*
 * Given the full path to a DEX or Jar file, and (if appropriate) the name
 * within the Jar, open the optimized version from the cache.
 *
 * If "*pNewFile" is set, a new file has been created with only a stub
 * "opt" header, and the caller is expected to fill in the blanks.
 *
 * Returns the file descriptor, locked and seeked past the "opt" header.
 */
int dvmOpenCachedDexFile(const char* fileName, const char* cachedFile,
    u4 modWhen, u4 crc, bool isBootstrap, bool* pNewFile, bool createIfMissing);

/*
 * Unlock the specified file descriptor.  Use in conjunction with
 * dvmOpenCachedDexFile().
 *
 * Returns true on success.
 */
bool dvmUnlockCachedDexFile(int fd);

/*
 * Verify the contents of the "opt" header, and check the DEX file's
 * dependencies on its source zip (if available).
 */
bool dvmCheckOptHeaderAndDependencies(int fd, bool sourceAvail, u4 modWhen,
    u4 crc, bool expectVerify, bool expectOpt);

/*
 * Optimize a DEX file.  The file must start with the "opt" header, followed
 * by the plain DEX data.  It must be mmap()able.
 *
 * "fileName" is only used for debug output.
 */
bool dvmOptimizeDexFile(int fd, off_t dexOffset, long dexLen,
    const char* fileName, u4 modWhen, u4 crc, bool isBootstrap);

/*
 * Continue the optimization process on the other side of a fork/exec.
 */
bool dvmContinueOptimization(int fd, off_t dexOffset, long dexLength,
    const char* fileName, u4 modWhen, u4 crc, bool isBootstrap);

/*
 * Abbreviated resolution functions, for use by optimization and verification
 * code.
 */
ClassObject* dvmOptResolveClass(ClassObject* referrer, u4 classIdx);
Method* dvmOptResolveMethod(ClassObject* referrer, u4 methodIdx,
        MethodType methodType);
Method* dvmOptResolveInterfaceMethod(ClassObject* referrer, u4 methodIdx);
InstField* dvmOptResolveInstField(ClassObject* referrer, u4 ifieldIdx);
StaticField* dvmOptResolveStaticField(ClassObject* referrer, u4 sfieldIdx);

#endif /*_DALVIK_DEXOPTIMIZE*/
