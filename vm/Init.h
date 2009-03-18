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
 * VM initialization and shutdown.
 */
#ifndef _DALVIK_INIT
#define _DALVIK_INIT

/*
 * Standard VM initialization, usually invoked through JNI.
 */
int dvmStartup(int argc, const char* const argv[], bool ignoreUnrecognized,
    JNIEnv* pEnv);
void dvmShutdown(void);
bool dvmInitAfterZygote(void);

/*
 * Enable Java programming language assert statements after the Zygote fork.
 */
void dvmLateEnableAssertions(void);

/*
 * Partial VM initialization; only used as part of "dexopt", which may be
 * asked to optimize a DEX file holding fundamental classes.
 */
int dvmPrepForDexOpt(const char* bootClassPath, DexOptimizerMode dexOptMode,
    DexClassVerifyMode verifyMode, int dexoptFlags);

/*
 * Unconditionally abort the entire VM.  Try not to use this.
 */
int dvmFprintf(FILE* fp, const char* format, ...);
void dvmAbort(void);

#endif /*_DALVIK_INIT*/
