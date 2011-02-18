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

    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    cmp         $$0, %ecx               # check for divide by zero
    GET_VREG    %eax                    # %eax<- vBB
    je          common_errDivideByZero  # handle divide by zero

    cmpl        $$-1, %ecx              # handle -1 special case divide error
    jne         .L${opcode}_noerror
    cmpl        $$0x80000000,%eax       # handle min int special case divide error
    je         .L${opcode}_break
.L${opcode}_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    #FFETCH_ADV 2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    .if  $div
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .L${opcode}_break2
%break
.L${opcode}_break:
    .if  $div
    movl        $$0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $$0, (rFP, rINST, 4)    # vAA<- 0
    .endif

.L${opcode}_break2:
    FINISH      2                       # jump to next instruction
    #FGETOP_JMP 2, %ecx                 # jump to next instruction; getop, jmp
