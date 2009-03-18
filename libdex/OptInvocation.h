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
 * Utility functions related to "dexopt".
 */
#ifndef _LIBDEX_OPTINVOCATION
#define _LIBDEX_OPTINVOCATION

#include <stdint.h>
#include <unistd.h>

#ifdef __cplusplus
extern "C" {
#endif


/*
 * Utility routines, used by the VM.
 */
char* dexOptGenerateCacheFileName(const char* fileName,
    const char* subFileName);
int dexOptCreateEmptyHeader(int fd);

/* some flags that get passed through to "dexopt" command */
#define DEXOPT_OPT_ENABLED      (1)
#define DEXOPT_OPT_ALL          (1 << 1)
#define DEXOPT_VERIFY_ENABLED   (1 << 2)
#define DEXOPT_VERIFY_ALL       (1 << 3)
#define DEXOPT_IS_BOOTSTRAP     (1 << 4)
#define DEXOPT_GEN_REGISTER_MAP (1 << 5)


#ifdef __cplusplus
};
#endif

#endif /*_LIBDEX_OPTINVOCATION*/
