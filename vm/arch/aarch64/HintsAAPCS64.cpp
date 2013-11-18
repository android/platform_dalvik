/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * Target-specific optimization and run-time hints
 */


#include "Dalvik.h"
#include "libdex/DexClass.h"

#include <stdlib.h>
#include <stddef.h>
#include <sys/stat.h>


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
 * This function produces a Aarch64 specific hint format.
 *
 * Aarch64 JNI hint format:
 *
 *  xxxxHHHH HHHHHHHH HHHHHHHH HHHHHHHH
 *
 *  All 28 bits are used to hold the number of 8 byte native stack slots required.
 *  This allows one loop to be eliminated from dvmPlatformInvoke.
 *
 * If there are too many arguments to construct valid hints, this function will
 * return a result with the S bit set.
 */
u4 dvmPlatformInvokeHints(const DexProto* proto)
{
    const char* sig = dexProtoGetShorty(proto)+1;   // Skip return type.
    unsigned int jniHints = 0;
    unsigned int numInts=2;         // For 1st two parameters JNIEnv* and jobject/jclass
    unsigned int numFloats=0, stackSlots=0;

    // Floating point and/or integer registers can overflow onto the stack.
    for (; *sig != '\0'; sig++) {
        switch(*sig) {
        case 'F':
        case 'D':
            if (numFloats++ >= 8) {
                stackSlots++;
            }
        break;

        default:
            if (numInts++ >= 8) {
                stackSlots++;
            }
            break;
        }
    }

    if (stackSlots > (1 << DALVIK_JNI_COUNT_SHIFT)-1 )  {
       jniHints = DALVIK_JNI_NO_ARG_INFO;
    } else {
       jniHints |= stackSlots; // Number of stack slots - not the offset.
    }

    return jniHints;
}
