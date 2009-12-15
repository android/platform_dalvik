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
    * File: OP_MUL_INT.S
    *
    * Code: 32-bit multiply operation.  Performs "%ecx = %ecx * %edx"
    *
    * Description: Perform a multiplication operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    addl        $$4, rPC                  # update PC
    imul        (rFP, %edx, 4), %ecx    # %ecx<- vBB * vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP2 %eax                    # jump to next instruction; getop, jmp
