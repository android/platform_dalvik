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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $$4, %ecx               # %ecx<- B
    andl        $$15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    $instr                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp