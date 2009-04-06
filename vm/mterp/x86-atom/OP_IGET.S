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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

%default { "mov":"l" }

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $$0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .L${opcode}_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .L${opcode}_finish
%break

.L${opcode}_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $$0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.L${opcode}_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $$4, %ecx               # %ecx<- B
    and         $$15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $$0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    mov$mov     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
