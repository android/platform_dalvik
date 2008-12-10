/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef _DALVIK_CLZ

#include <stdint.h>
#include <machine/cpu-features.h>

#if defined(__ARM_HAVE_CLZ) && !defined(__thumb__)

#define CLZ(x) __builtin_clz(x)

#else

int clz_impl(unsigned long int x);
#define CLZ(x) clz_impl(x)

#endif

#endif // _DALVIK_CLZ
