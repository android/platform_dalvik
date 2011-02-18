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
    * File: binopDivRemLong2Addr.S
    *
    * Code: 64-bit "/2addr" long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long/2addr, rem-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

%default {"func":"__divdi3"}

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    and         $$15, rINST             # rINST<- A
    movl        (rFP, %edx, 4), %eax    # %eax<- vB
    movl        %eax, -12(%esp)         # push arg vB
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vB+1
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    movl        %ecx, -8(%esp)          # push arg vB+1
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vA,vA+1
    jmp         .L${opcode}_break
%break
.L${opcode}_break:
    movq        %xmm0, -20(%esp)        # push arg vA, vA+1
    lea         -20(%esp), %esp
    call        $func                   # call func
    lea         20(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vA<- return high
    FFETCH_ADV  1, %ecx                 # %ecx<- next instruction hi; fetch, advance
    FGETOP_JMP 1, %ecx                  # jump to next instruction; getop, jmp
