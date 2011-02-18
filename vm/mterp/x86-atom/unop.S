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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

%default {"preinstr":"", "instr":""}

    movl        rINST, %ecx             # %ecx<- BA+
    shr         $$4, %ecx               # %ecx<- B
    and         $$15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    $preinstr                           # do operation part 1
    $instr                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp
