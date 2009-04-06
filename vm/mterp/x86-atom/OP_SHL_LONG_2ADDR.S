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
    * File: OP_SHL_LONG_2ADDR.S
    *
    * Code: Performs a shift left long. Uses no substitutions.
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
    andl        $$15, rINST             # rINST<- A
    movss       (rFP, %edx, 4),  %xmm0  # %xmm0<- vB
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vA
    movq        .LshiftMask, %xmm2      # %xmm2<- mask for the shift bits
    pand        %xmm2, %xmm0            # %xmm0<- masked shift bits
    psllq       %xmm0, %xmm1            # %xmm1<- shifted vA
    movq        %xmm1, (rFP, rINST, 4)  # vA<- shifted vA
    FINISH      1                       # jump to next instruction

