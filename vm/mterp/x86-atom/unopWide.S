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
    * File: unopWide.S
    *
    * Code: Generic 64-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%xmm0 = op %xmm1".
    *
    * For:  neg-double, neg-long, not-long
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

%default {"preinstr":"","result":"%xmm0"}

    movl        rINST, %ecx             # %ecx<- BA+
    shr         $$4, rINST              # rINST<- B
    and         $$15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vB
    $preinstr                           # do operation part 1
    $instr                              # do operation part 2
    movq        $result, (rFP, %ecx, 4) # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


