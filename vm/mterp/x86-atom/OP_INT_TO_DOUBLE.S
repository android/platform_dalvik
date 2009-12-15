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
    * File: OP_INT_TO_DOUBLE.S
    *
    * Code: Convert an int to a double. Uses no substitutions.
    *
    * For: int-to-double
    *
    * Description: Converts an int in the source register, to a double, and
    *              stores the result in the destination register. vA<- (double) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA+
    shr         $$4, %eax               # %eax<- B
    andl        $$15, rINST             # rINST<- A
    cvtsi2sd    (rFP, %eax, 4), %xmm0   # %xmm0<- vB
    movq        %xmm0, (rFP, rINST, 4)  # vA<- %xmm0; (double) vB
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp
