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
    * File: OP_MOVE_EXCEPTION.S
    *
    * Code: Moves an exception to a register
    *
    * For: move-exception
    *
    * Description: Save a just-caught exception into the given register. This
    *              instruction is only valid as the first instruction of an
    *              exception handler.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %ecx # %ecx<- glue->self
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        offThread_exception(%ecx), %edx # %edx<- glue->self->exception
    movl        $$0, offThread_exception(%ecx) # clear exception
    SET_VREG    %edx, rINST             # vAA<- glue->self->exception
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

