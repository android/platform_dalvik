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
    * File: OP_IGET_WIDE.S
    *
    * Code: 64 bit instance field "get" operation. Uses no substitutions.
    *
    * For: iget-wide
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    * Format:  B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %eax             # %eax<- MterpGlue pointer
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- pDvmDex
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- CCCC
    FETCH       1, %edx                 # %edx<- pDvmDex->pResFields
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved InstField ptr
    cmp         $$0, %ecx               # check for null ptr; resolved InstField ptr
    jne         .L${opcode}_finish
    movl        offGlue_method(%eax), %ecx # %ecx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- method->clazz
    movl        %ecx, -8(%esp)          # push parameter CCCC; field ref
    movl        %edx, -4(%esp)          # push parameter method->clazz
    jmp         .L${opcode}_finish2
%break

.L${opcode}_finish2:
    lea         -8(%esp), %esp
    call        dvmResolveInstField     # resolve InstField ptr
                                        # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $$0, %eax               # check if resolved
    lea         8(%esp), %esp
    movl        %eax, %ecx              # %ecx<- %eax; %ecx expected to hold field
    je          common_exceptionThrown

   /*
    *  %ecx holds resolved field
    */

.L${opcode}_finish:

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    andl        $$15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB
    cmp         $$0, %edx               # check for null object
    je          common_errNullObject
    movl        offInstField_byteOffset(%ecx), %ecx # %ecx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (%ecx, %edx), %xmm0     # %xmm0<- object field
    movq        %xmm0, (rFP, rINST, 4)  # vA<- %xmm0; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


