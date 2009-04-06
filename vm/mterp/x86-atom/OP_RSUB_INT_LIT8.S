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
    * File: OP_RSUB_INT_LIT8.S
    *
    * Code: 32-bit reverse-subtraction. Uses no substitutions.
    *
    * For: rsub-int/lit8
    *
    * Description: Perform a reverse subtraction on a register and a
    *              signed extended 8-bit literal value.
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    GET_VREG    %ecx                    # %ecx<- vBB
    sub         %ecx, %edx              # %edx<- +CC sub vBB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FINISH      2                       # jump to next instruction

