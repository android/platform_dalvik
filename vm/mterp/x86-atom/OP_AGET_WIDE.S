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
    * File: OP_AGET_WIDE.S
    *
    * Code: 64-bit array get operation.
    *
    * For: aget-wide
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the destination
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $$0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        offArrayObject_contents(%ecx, %edx, 8), %xmm0 # %xmm0<- vBB[vCC]
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %xmm0; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp