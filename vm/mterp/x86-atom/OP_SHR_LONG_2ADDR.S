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
    * File: OP_SHR_LONG_2ADDR.S
    *
    * Code: Performs a shift left long
    *
    * For: shl-long/2addr
    *
    * Description: Perform a binary shift operation using two source registers
    *              where the fist is the value to shift and the second is the
    *              shift amount. Store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    andl        $$15, rINST             # rINST<- BA
    movss       (rFP, %edx, 4),  %xmm0  # %xmm0<- vB
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vA
    movq        .LshiftMask, %xmm2
    pand        %xmm2, %xmm0            # %xmm0<- masked for the shift bits
    psrlq       %xmm0, %xmm1            # %xmm1<- shifted vBB
    cmpl        $$0, 4(rFP, rINST, 4)   # check if we need to consider sign
    jl          .L${opcode}_finish      # consider sign
    jmp         .L${opcode}_final       # sign is fine, finish
%break

.L${opcode}_finish:
    movq        .Lvalue64, %xmm3        # %xmm3<- 64
    psubq       %xmm0, %xmm3            # %xmm3<- 64 - shift amount
    movq        .L64bits, %xmm4         # %xmm4<- lower 64 bits set
    psllq       %xmm3, %xmm4            # %xmm4<- correct mask for sign bits
    por         %xmm4, %xmm1            # %xmm1<- signed and shifted vBB

.L${opcode}_final:
    movq        %xmm1, (rFP, rINST, 4)  # vAA<- shifted vBB
    FINISH      1                       # jump to next instruction


