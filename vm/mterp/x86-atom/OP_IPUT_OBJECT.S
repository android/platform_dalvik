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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
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

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $$0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .L${opcode}_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .L${opcode}_finish
%break

.L${opcode}_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $$0, %eax               # check if resolved
    jne         .L${opcode}_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.L${opcode}_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $$4, %ecx               # %ecx<- B
    and         $$15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $$0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    GET_VREG    rINST                   # rINST<- vA
    movl        rINST, (%edx, %ecx)     # object field<- vA
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl     rGLUE, %eax             # get glue
    movl        offGlue_cardTable(%eax), %eax # get card table base
    testl       rINST, rINST            # test if we stored a null value
    je          1f                     # skip card mark if null stored
    shrl        $$GC_CARD_SHIFT, %ecx   # set obeject head to card number
    movb        %al, (%eax, %ecx)
1:
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp
