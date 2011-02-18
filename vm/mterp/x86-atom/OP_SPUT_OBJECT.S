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
    * File: OP_SPUT_OBJECT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $$0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField
    je          .L${opcode}_resolve
    jmp         .L${opcode}_finish
%break

.L${opcode}_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $$0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.L${opcode}_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA


    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    testl       rINST, rINST            # stored null object ptr?
    je          1f
    movl        rGLUE, %edx             # get glue
    movl        offField_clazz(%ecx), %ecx # ecx<- field->clazz
    movl        offGlue_cardTable(%edx), %edx # get card table base
    shrl        $$GC_CARD_SHIFT, %ecx   # head to card number
    movb        %dl, (%edx, %ecx)       # mark card
1:
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
