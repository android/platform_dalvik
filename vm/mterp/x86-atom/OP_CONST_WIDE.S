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
    * File: OP_CONST_WIDE.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide
    *
    * Description: Move the given literal value into the specified
    *              register pair
    *
    * Format: AA|op BBBBlolo BBBBlohi BBBBhilo BBBBhihi (51l)
    *
    * Syntax: op vAA, #+BBBBBBBBBBBBBBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBBlolo
    FETCH       2, %edx                 # %edx<- BBBBlohi
    shl         $$16, %edx              # %edx<- prepare to create #+BBBBBBBBlo
    or          %edx, %ecx              # %ecx<- #+BBBBBBBBlo
    movl        %ecx, (rFP, rINST, 4)   # vAA <- #+BBBBBBBBlo
    FETCH       3, %ecx                 # %ecx<- BBBBhilo
    FETCH       4, %edx                 # %edx<- BBBBhihi
    FFETCH_ADV  5, %eax                 # %eax<- next instruction hi; fetch, advance
    shl         $$16, %edx              # %edx<- prepare to create #+BBBBBBBBhi
    or          %edx, %ecx              # %ecx<- #+BBBBBBBBhi
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1 <- #+BBBBBBBBlo
    FGETOP_JMP  5, %eax                 # jump to next instruction; getop, jmp
