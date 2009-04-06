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
    * File: binopDLit8.S
    *
    * Code: 32-bit "lit8" divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int/lit8, rem-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signe extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

%default {"div":"1"}

    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    testl       %ecx, %ecx              # handle -1 special case divide error
    FETCH_BB    1, %eax                 # %eax<- BB
    jz          common_errDivideByZero  # handle divide by zero
    addl        $$4, rPC                # update PC
    cmpl        $$-1, %ecx
    GET_VREG    %eax                    # %eax<- vBB
    jnz         .L${opcode}_continue
    cmpl        $$0x80000000,%eax       # handle min int special case divide error
    je          .L${opcode}_break
.L${opcode}_continue:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    .if  $div
    movzbl      (rPC), %edx
    SET_VREG    %eax, rINST             # vA<- %eax (quotient)
    movzbl      1(rPC), rINST
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4)
    .else
    movzbl      (rPC), %eax
    SET_VREG    %edx, rINST             # vA<- %edx (remainder)
    movzbl      1(rPC), rINST
    jmp         *dvmAsmInstructionJmpTable(, %eax, 4)
    .endif
%break
.L${opcode}_break:
    movzbl      (rPC), %edx
    .if  $div
    movl        $$0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $$0, (rFP, rINST, 4)    # vAA<- 0
    .endif
    movzbl      1(rPC), rINST
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4)
