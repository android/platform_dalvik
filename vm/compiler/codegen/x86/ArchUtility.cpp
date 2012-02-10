/*
 * Copyright (C) 2010 The Android Open Source Project
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

#include "../../CompilerInternals.h"
#include "libdex/DexOpcodes.h"
#include "X86LIR.h"

/* Dump instructions and constant pool contents */
void dvmCompilerCodegenDump(CompilationUnit *cUnit)
{
}

/* Target-specific cache flushing (not needed for x86 */
int dvmCompilerCacheFlush(long start, long end, long flags)
{
    return 0;
}

/* Target-specific cache clearing */
void dvmCompilerCacheClear(char *start, size_t size)
{
    /* 0 is an invalid opcode for x86. */
    memset(start, 0, size);
}
