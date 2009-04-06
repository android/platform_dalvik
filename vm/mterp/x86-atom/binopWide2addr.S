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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, rINST              # rINST<- B
    andl        $$15, %edx              # %edx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    $instr                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    #FINISH      1                      # jump to next instruction
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp