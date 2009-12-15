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
    * File: binopD.S
    *
    * Code: 32-bit integer divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int, rem-int
    *
    * Description: Perform a binary operation on two source
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

%default {"div":"1"}


    FETCH_CC    1, %ecx                 # %ecx<- CC
    FETCH_BB    1, %eax                 # %eax<- BB
    GET_VREG    %ecx                    # %ecx<- vCC
    testl       %ecx, %ecx
    GET_VREG    %eax                    # %eax<- vBB
    je          common_errDivideByZero  # handle divide by zero
    cmpl        $$-1, %ecx              # handle -1 special case divide error
    jnz         .L${opcode}_continue
    cmpl        $$0x80000000,%eax       # handle min int special case divide error
    je         .L${opcode}_break
.L${opcode}_continue:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    .if  $div
    FFETCH_ADV 2 %edx
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    FGETOP_JMP  2 %edx
    .else
    FFETCH_ADV  2 %eax
    SET_VREG %edx rINST  # vAA<- %edx (remainder)
    FGETOP_JMP  2 %eax
    .endif
%break
.L${opcode}_break:
    FFETCH_ADV  2 %eax
    .if  $div
    movl        $$0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $$0, (rFP, rINST, 4)    # vAA<- 0
    .endif
    FGETOP_JMP  2 %eax