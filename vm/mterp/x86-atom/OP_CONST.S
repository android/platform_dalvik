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
    * File: OP_CONST.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const
    *
    * Description: Move the given literal value into the specified register
    *
    * Format: AA|op BBBBlo BBBBhi (31i)
    *
    * Syntax: op vAA, #+BBBBBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBBhi
    FETCH       1, %ecx                 # %ecx<- BBBBlo
    shl         $$16, %edx              # move BBBB to high bits
    or          %edx, %ecx              # %ecx<- #+BBBBBBBB
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    SET_VREG    %ecx, rINST             # vAA<- %ecx; literal
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp
