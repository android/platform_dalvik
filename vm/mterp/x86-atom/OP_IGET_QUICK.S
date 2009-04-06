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
    * File: OP_IGET_QUICK.S
    *
    * Code: Optimization for iget
    *
    * For: iget-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $$4, %eax               # %eax<- B
    and         $$15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB; object to operate on
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    cmp         $$0, %eax               # check if object is null
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %edx                 # %eax<- next instruction hi; fetch, advance
    movl        (%ecx, %eax), %eax      # %eax<- object field
    SET_VREG    %eax, rINST             # fp[A]<- %eax
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp
