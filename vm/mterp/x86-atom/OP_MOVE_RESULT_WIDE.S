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
    * File: OP_MOVE_RESULT_WIDE.S
    *
    * Code: Copies a return value to a register
    *
    * For: move-result-wide
    *
    * Description: Move the double-word non-object result of the most
    *              recent method invocation into the indicated register. This
    *              must be done as the instruction immediately after a
    *              method invocation whose (single-word, non-object) result
    *              is not to be ignored; anywhere else is invalid.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movq        offGlue_retval(%eax), %xmm0 # %xmm0<- glue->retval
    movq        %xmm0, (rFP, rINST, 4)  # vA<- glue->retval
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp
