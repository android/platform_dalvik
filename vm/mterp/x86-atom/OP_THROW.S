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
    * File: OP_THROW.S
    *
    * Code: Throw an exception
    *
    * For: throw
    *
    * Description: Throw an exception object in the current thread.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # export the pc
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $$0, rINST              # check for null
    movl        offGlue_self(%eax), %ecx # %ecx<- glue->self
    je          common_errNullObject    # handle null object
    movl        rINST, offThread_exception(%ecx) # thread->exception<- object
    jmp         common_exceptionThrown  # handle exception
