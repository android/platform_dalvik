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
    * File: OP_CONST_WIDE_32.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide/32
    *
    * Description: Move the given literal value (sign-extended to 64 bits)
    *              into the specified register-pair
    *
    * Format: AA|op BBBBlo BBBBhi (31i)
    *
    * Syntax: op vAA, #+BBBBBBBB
    */

    FETCH       1,  %edx                # %edx<- BBBBlo
    FETCHs      2, %ecx                 # %ecx<- BBBBhi
    shl         $$16, %ecx              # prepare to create #+BBBBBBBB
    or          %ecx, %edx              # %edx<- %edx<- #+BBBBBBBB
    sar         $$31, %ecx              # %ecx<- sign bit
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        %edx, (rFP, rINST, 4)   # vAA<-  BBBBBBBB
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1<- ssssssss
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp
