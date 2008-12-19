   /* Copyright (C) 2008 Intel Corporation
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
    * File: debug.c
    */

#include <inttypes.h>
#include "Dalvik.h"
#include "interp/InterpDefs.h"
#include "mterp/Mterp.h"
#include "mterp/c/opcommon.c"


long byteRun[256] = { 0 };

#undef H
#define H(_op) #_op
DEFINE_GOTO_TABLE(gDvmMterpHandlerNamesIa32)


void logByteRun(int inst) {
  ++byteRun[inst];
}

void clearByteRun() {
  int i;
  for (i = 0; i < 256; ++i) { byteRun[i] = 0; }
}

void printByteRun() {
  int i;
  LOGI("[PBR]");
  for (i = 0; i < 256; ++i) {
    LOGI("[PBR] %-35s\t%ld\n",  (const char*) gDvmMterpHandlerNamesIa32[i], byteRun[i]);
  }
  clearByteRun();
}

void printIA32_Op_c(int inst) {
#if WITH_FAST_IA_PRINT > 4 || 1
    LOGI("[JB]-Execute ASM-IA32 handler %s\n", (const char*) gDvmMterpHandlerNamesIa32[inst & 0xff]); 
#endif
  logByteRun(inst & 0xff);
}

void printTesting(char* message) {
  LOGI("[JB]-Message: %s\n", message);
}


