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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    $instr                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp