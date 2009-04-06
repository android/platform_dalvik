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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

%default { "mov":"l","scale":"4"}

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $$0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, $scale), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    mov$mov offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp