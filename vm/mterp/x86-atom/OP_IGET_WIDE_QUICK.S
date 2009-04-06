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
    * File: OP_IGET_WIDE_QUICK.S
    *
    * Code: Optimization for iget
    *
    * For: iget/wide-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    andl        $$15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB; object to operate on
    cmp         $$0, %edx               # check if object is null
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    je          common_errNullObject    # handle null object
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    movq        (%ecx, %edx), %xmm0     # %xmm0<- object field
    movq        %xmm0, (rFP, rINST, 4)  # fp[A]<- %xmm0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


