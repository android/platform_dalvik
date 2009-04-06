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
    * File: OP_RSUB_INT.S
    *
    * Code: 32-bit reverse-subtraction. Uses no substitutions.
    *
    * For: rsub-int
    *
    * Description: Perform a reverse subtraction on a register and a
    *              signed extended 16-bit literal value and store the
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
    GET_VREG    %ecx                    # %ecx<- vB
    subl        %ecx, %edx              # %edx<- +CCCC sub vB
    SET_VREG    %edx, rINST             # vA<- %edx; result
    FINISH      2                       # jump to next instruction