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
    * File: binopS2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%edx = %edx op %cl".
    *
    * For: shl-int/2addr, shr-int/2addr, ushr-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $$4, %ecx               # %ecx<- B
    FFETCH_ADV  1, %eax                 # %ecx<- next i nstruction hi; fetch, advance
    andl        $$15, rINST             # rINST<- A
    addl        $$2, rPC                  # update PC
    movl        rINST, %edx             # %edx<- A
    GET_VREG    %ecx                    # %ecx<- vB
    GET_VREG    %edx                    # %edx<- vA
    $instr                              # %edx<- vA op vB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP2 %eax                    # jump to next instruction; getop, jmp