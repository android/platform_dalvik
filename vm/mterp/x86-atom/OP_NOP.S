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
    * File: OP_NOP.S
    *
    * Code: Use a cycle. Uses no substitutions.
    *
    * For: nop
    *
    * Description: No operation. Use a cycle
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    FINISH      1                       # jump to next instruction

#ifdef ASSIST_DEBUGGER

   /*
    * insert fake function header to help gdb find the stack frame
    */

    .type       dalvik_inst, %function
dalvik_inst:
    MTERP_ENTRY
#endif

