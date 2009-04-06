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
    * File: OP_MONITOR_ENTER.S
    *
    * Code: Aquire a monitor
    *
    * For: monitor-enter
    *
    * Description: Aquire a monitor for the indicated object.
    *
    *
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $$0, rINST              # check for null object
    movl        offGlue_self(%eax), %eax # %eax<- glue->self
#ifdef WITH_MONITOR_TRACKING
    EXPORT_PC   # export PC so we can grab stack trace
#endif
    je          common_errNullObject    # handle null object
#    jmp         .L${opcode}_finish
#%break
#.L${opcode}_finish:
    movl        rINST, -4(%esp)         # push parameter reference
    movl        %eax, -8(%esp)          # push parameter
    lea         -8(%esp), %esp
    call        dvmLockObject           # call: (struct Thread* self,
                                        #       struct Object* obj)
                                        # return: void
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    lea         8(%esp), %esp
#ifdef WITH_DEADLOCK_PREDICTION
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %eax # %eax<- glue->self
    movl        offThread_exception(%eax), %eax # %eax<- glue->self->exception
    cmp         $$0, %eax               # check for exception
    jne         common_exceptionThrown  # handle exception
#endif
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp

