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
    * File: OP_APUT_WIDE.S
    *
    * Code: 64-bit array put operation.
    *
    * For: aput-wide
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA.
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
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vAA
    movq        %xmm0, offArrayObject_contents(%ecx, %edx, 8) # vBB[vCC]<- %xmm0; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
