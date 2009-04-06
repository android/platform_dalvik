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
    * File: OP_FILL_ARRAY_DATA.S
    *
    * Code: Fills an array with given data. Uses no substitutions.
    *
    * For: fill-array-data
    *
    * Description: Fill the given array with the idicated data. The reference
    *              must be an array of primitives, and the data table must
    *              match it in type and size
    *
    * Format: AA|op BBBBlo BBBBhi (31t)
    *
    * Syntax: op vAA, +BBBBBBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $$16, %edx              # prepare to create +BBBBBBBB
    or          %ecx, %edx              # %edx<- +BBBBBBBB
    lea         (rPC, %edx, 2), %edx    # %edx<- PC + +BBBBBBBB; array data location
    EXPORT_PC
    push        %edx
    push        (rFP, rINST, 4)
    call        dvmInterpHandleFillArrayData # call: (ArrayObject* arrayObject, const u2* arrayData)
                                             # return: bool
    FFETCH_ADV  3, %edx                 # %edx<- next instruction hi; fetch, advance
    cmp         $$0, %eax
    lea         8(%esp), %esp
    je          common_exceptionThrown
    FGETOP_JMP  3, %edx                 # jump to next instruction; getop, jmp
