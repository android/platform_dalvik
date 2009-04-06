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
    * File: OP_CMP_LONG.S
    *
    * Code: Compare floating point values. Uses no substitutions.
    *
    * For: cmp-long
    *
    * Description: Perform a long comparison, storing 0 if the two
    *              arguments are equal, 1 if the second argument is larger
    *              or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    movl        4(rFP, %ecx, 4), %eax   # %eax<- vBBhi
    cmp         4(rFP, %edx, 4), %eax   # compare vCChi and vBBhi
    jl          .L${opcode}_less
    jg          .L${opcode}_greater
    movl        (rFP, %ecx, 4), %eax    # %eax<- vBBlo
    cmp         (rFP, %edx, 4), %eax    # compare vCClo and vBBlo
    ja          .L${opcode}_greater
    jne         .L${opcode}_less
    jmp         .L${opcode}_final
%break

.L${opcode}_final:
    movl        $$0x0, (rFP, rINST, 4)  # vAA<- equal
    FINISH      2                       # jump to next instruction

.L${opcode}_less:
    movl        $$0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FINISH      2                       # jump to next instruction

.L${opcode}_greater:
    movl        $$0x1, (rFP, rINST, 4)  # vAA<- greater than
    FINISH      2                       # jump to next instruction


