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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $$15, rINST             # rINST<- A
    shr         $$4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $$4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    j${revcmp}  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $$1, %edx
    js          common_periodicChecks2
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction
