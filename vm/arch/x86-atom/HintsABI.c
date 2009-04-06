   /* Copyright (C) 2008 The Android Open Source Project
    *
    * Licensed under the Apache License, Version 2.0 (the "License");
    * you may not use this file except in compliance with the License.
    * You may obtain a copy of the License at
    *
    * http://www.apache.org/licenses/LICENSE-2.0
    *
    * Unless required by applicable law or agreed to in writing, software
    * distributed under the License is distributed on an "AS IS" BASIS,
    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    * See the License for the specific language governing permissions and
    * limitations under the License.
    */

   /*
    * The class loader will associate with each method a 32-bit info word
    * (jniArgInfo) to support JNI calls.  The high order 4 bits of this word
    * are the same for all targets, while the lower 28 are used for hints to
    * allow accelerated JNI bridge transfers.
    *
    * jniArgInfo (32-bit int) layout:
    *
    *    SRRRHHHH HHHHHHHH HHHHHHHH HHHHHHHH
    *
    *    S - if set, ignore the hints and do things the hard way (scan signature)
    *    R - return-type enumeration
    *    H - target-specific hints (see below for details)
    *
    * This function produces IA32-specific hints for the standard 32-bit 386 ABI.
    * All arguments have 32-bit alignment.  Padding is not an issue.
    *
    * IA32 ABI JNI hint format
    *
    *       ZZZZ ZZZZZZZZ AAAAAAAA AAAAAAAA
    *
    *   Z - reserved, must be 0
    *   A - size of variable argument block in 32-bit words (note - does not
    *       include JNIEnv or clazz)
    *
    * For the 386 ABI, valid hints should always be generated.
    */


#include "Dalvik.h"
#include "libdex/DexClass.h"
#include <stdlib.h>
#include <stddef.h>
#include <sys/stat.h>

u4 dvmPlatformInvokeHints(const DexProto* proto)  {

const char* sig = dexProtoGetShorty(proto);
unsigned int wordCount = 0;
char sigByte;

 while (1) {

   /*
    * Move past return type; dereference sigByte
    */

    sigByte = *(++sig);
    if (sigByte == '\0') { break; }
    ++wordCount;

    if (sigByte == 'D' || sigByte == 'J') {
      ++wordCount;
    }
 }

/*
 * Check for Dex file limitation and return
 */

 if (wordCount > 0xFFFF) { return DALVIK_JNI_NO_ARG_INFO; }
 return wordCount;

}
