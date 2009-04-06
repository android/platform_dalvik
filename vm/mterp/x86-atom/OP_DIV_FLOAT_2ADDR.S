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
    * File: OP_DIV_FLOAT_2ADDR.S
    *
    * Code: Divides floats. Uses no substitutions.
    *
    * For: div-float/2addr
    *
    * Description: Divide operation on two source registers, storing
    *              the result in the first source reigster
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $$15, %ecx              # %ecx<- A
    shr         $$4, rINST              # rINST<- B
    flds        (rFP, %ecx, 4)          # %xmm0<- vA
    fdivs       (rFP, rINST, 4)         # divide double; vA/vB
    fstps       (rFP, %ecx, 4)          # vAA<- result
    FINISH      1                       # jump to next instruction
