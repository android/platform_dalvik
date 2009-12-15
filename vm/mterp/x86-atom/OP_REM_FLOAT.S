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
    * File: OP_REM_FLOAT.S
    *
    * Code: Computes the remainder of a division. Performs no substitutions.
    *
    * For: rem-float
    *
    * Description: Calls fmod to compute the remainder of the result of dividing a
    *              source register by a second, and stores the result in a
    *              destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    movl        %ecx, -8(%esp)          # push parameter float
    movl        %edx, -4(%esp)          # push parameter float
    lea         -8(%esp), %esp
    call        fmodf                   # call: (float x, float y)
                                        # return: float
    lea         8(%esp), %esp
    fstps       (rFP, rINST, 4)         # vAA<- remainder; return of fmod
    FINISH      2                       # jump to next instruction

