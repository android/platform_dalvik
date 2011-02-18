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
    * File: OP_ARRAY_LENGTH.S
    *
    * Code: 32-bit array length operation.
    *
    * For: array-length
    *
    * Description: Store the length of the indicated array in the given
    *              destination register. vB <- offArrayObject_length(vA)
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $$4, %eax               # %eax<- B
    andl        $$15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB
    cmp         $$0, %eax               # check for null array object
    je          common_errNullObject    # handle null array object
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    movl        offArrayObject_length(%eax), %eax # %eax<- array length
    movl        %eax, (rFP, rINST, 4)   # vA<- %eax; array length
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp
