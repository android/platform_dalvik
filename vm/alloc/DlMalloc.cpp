/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include "DlMalloc.h"

#include <stdint.h>

/* Dalvik specific morecore implementation defined in HeapSource.cpp. */
#define MORECORE(x) dvmHeapSourceMorecore(m, x)
extern void* dvmHeapSourceMorecore(void* mspace, intptr_t increment);

/*
 * Ugly inclusion of C file so that Dalvik specific #defines configure
 * dlmalloc for our use for mspaces (regular dlmalloc is still declared
 * in bionic).
 */
#pragma GCC diagnostic ignored "-Wempty-body"
#pragma GCC diagnostic ignored "-Wstrict-aliasing"
#pragma GCC diagnostic ignored "-Wunused-variable"
#include "../../../bionic/libc/upstream-dlmalloc/malloc.c"
