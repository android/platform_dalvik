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
    * File: OP_MONITOR_EXIT.S
    *
    * Code: Release a monitor
    *
    * For: monitor-exit
    *
    * Description: Release a monitor for the indicated object. If this instruction needs
    *              to throw an execption, it must do so as if the pc has already
    *              advanced pased the instruction.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # export the pc
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $$0, rINST              # rINST<- check for null object
    je          common_errNullObject    # handle null object
    push        rINST                   # push parameter object
    push        offGlue_self(%eax)      # push parameter self
    call        dvmUnlockObject         # call: (struct Thread* self,
                                        #       struct Object* obj)
                                        # return: bool
    FINISH_FETCH_ADVANCE 1, %edx        # advance pc before exception
    cmp         $$0, %eax               # check for success
    lea         8(%esp), %esp
    je          common_exceptionThrown  # handle exception
    FINISH_JMP  %edx                    # jump to next instruction
