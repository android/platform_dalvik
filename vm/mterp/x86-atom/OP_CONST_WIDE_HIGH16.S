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
    * File: OP_CONST_WIDE_HIGH16.S
    *
    * Code: Move a literal value to a register. Uses no substitutions.
    *
    * For: const-wide/high16
    *
    * Description: Move the given literal value (right-zero-extended to 64
    *              bits) into the specified register
    *
    * Format: AA|op BBBB (21h)
    *
    * Syntax: op vAA, #+BBBB000000000000
    */

    FETCH       1, %ecx                 # %ecx<- 0000BBBB (zero-extended)
    shl         $$16, %ecx              # rINST<- AA
    movl        $$0, (rFP, rINST, 4)    # vAAlow<- 00000000
    movl        %ecx, 4(rFP, rINST, 4)  # vAAhigh<- %ecx; BBBB0000
    FINISH      2                       # jump to next instruction
