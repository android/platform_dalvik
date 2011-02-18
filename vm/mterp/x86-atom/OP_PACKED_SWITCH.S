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
    * File: OP_PACKED_SWITCH.S
    *
    * Code: Jump to a new instruction using a jump table
    *
    * For: packed-switch, sparse-switch
    *
    * Description: Jump to a new instruction based on the value in the given
    *              register, using a table of offsets corresponding to each
    *              value in a particular integral range, or fall through to
    *              the next instruction if there is no match.
    *
    * Format: AA|op BBBBlo BBBBhi (31t)
    *
    * Syntax: op vAA, +BBBBBBBB
    */

%default { "func":"dvmInterpHandlePackedSwitch" }

    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $$16, %edx              # prepare to create +BBBBBBBB
    or          %edx, %ecx              # %ecx<- +BBBBBBBB
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, -4(%esp)         # push parameter vAA
    lea         (rPC, %ecx, 2), %ecx    # %ecx<- PC + +BBBBBBBB*2
    movl        %ecx, -8(%esp)          # push parameter PC + +BBBBBBBB*2
    lea         -8(%esp), %esp
    call        $func                   # call code-unit branch offset
    shl         $$1, %eax               # shift for byte offset
    movl        %eax, %edx              # %edx<- offset
    lea         8(%esp), %esp
    jle         common_periodicChecks_backwardBranch  # do backward branch
    jmp         .L${opcode}_finish
%break
.L${opcode}_finish:
    FINISH_RB   %edx, %ecx              # jump to next instruction
