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
    * File: binopDivRemLong.S
    *
    * Code: 64-bit long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long, rem-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

%default {"func":"__divdi3"}

    FETCH_CC    1, %edx                 # %edx<- CC
    movl        (rFP, %edx, 4), %eax    # %eax<- vCC
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vCC+1
    movl        %eax, -8(%esp)          # push arg vCC
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    FETCH_BB    1, %edx                 # %edx<- BB
    movl        %ecx, -4(%esp)          # push arg vCC+1
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vBB,vBB+1
    jmp         .L${opcode}_finish
%break
.L${opcode}_finish:
    movq        %xmm0, -16(%esp)        # push arg vBB,vBB+1
    lea         -16(%esp), %esp
    call        $func                   # call func
    lea         16(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vAA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vAA+1<- return high
    FINISH      2                       # jump to next instruction
