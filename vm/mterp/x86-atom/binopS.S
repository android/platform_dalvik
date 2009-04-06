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
    * File: binopS.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%edx = %edx op %cl"
    *
    * For: shl-int, shr-int, ushr-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_CC    1, %ecx                 # %ecx<- CC
    FETCH_BB    1, %edx                 # %edx<- BB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    addl        $$4, rPC                  # update PC
    GET_VREG    %ecx                    # %ecx<- vCC
    GET_VREG    %edx                    # %edx<- vBB
    $instr                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP2 %eax                    # jump to next instruction; getop, jmp