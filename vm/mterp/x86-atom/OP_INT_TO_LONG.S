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
    * File: OP_INT_TO_LONG.S
    *
    * Code:  Convert an int to a long. Uses no substitutions.
    *
    * For:
    *
    * Description: Convert an int in the source register, to a long, and
    *              stores the result in the destintation register. vA<- (long) vB
    *
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA+
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $$4, %eax               # %eax<- B
    andl        $$15, %ecx              # %ecx<- A
    GET_VREG    %eax                    # %eax<- vB
    cdq                                 # %edx:%eax<- sign-extend of %eax
    movl        %eax, (rFP, %ecx, 4)    # vA<- lo part
    movl        %edx, 4(rFP, %ecx, 4)   # vA+1<- hi part
    FINISH      1                       # jump to next instruction
