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
    * File: binopF2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0 = %xmm0 op %xmm1".
    *
    * For: add-float/2addr, mul-float/2addr, sub-float/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $$15, %ecx              # %ecx<- A
    shr         $$4, rINST              # rINST<- B
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<- vA
    movss       (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    $instr                              # %xmm0<- vA op vB
    movss       %xmm0, (rFP, %ecx, 4)   # vA<- %xmm0; result
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp
