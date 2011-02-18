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
    * File: OP_CONST_WIDE_16.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide/16
    *
    * Description: Move the given literal value (sign-extended to 64 bits)
    *              into the specified register-pair
    *
    * Format: AA|op BBBB (21s)
    *
    * Syntax: op vAA, #+BBBB
    */

    FETCHs      1, %ecx                 # %ecx<- ssssBBBB (sign-extended)
    movl        %ecx, %edx              # %edx<- ssssBBBB (sign-extended)
    sar         $$31, %ecx              # %ecx<- sign bit
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        %edx, (rFP, rINST, 4)   # vAA<- ssssBBBB
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1<- ssssssss
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
