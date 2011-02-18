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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

%default { "mov":"l","scale":"4", "value": "rINST"}

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $$0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, $scale), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    mov$mov     $value, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
