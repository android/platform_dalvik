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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    andl        $$15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    $instr                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


