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
    * File: OP_LONG_TO_FLOAT.S
    *
    * Code: Convert a long to a float. Uses no substitutions.
    *
    * For: int-to-float
    *
    * Description: Converts a float in the source register, to a float, and
    *              stores the result in the destination register. vA<- (double) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA+
    shr         $$4, rINST              # rINST<- B
    and         $$15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    fildll      (rFP, rINST, 4)         # FPU<- vB
    fstps       (rFP, %ecx, 4)          # vA<- FPU; (float) vB
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp
