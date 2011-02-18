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
    * File: OP_APUT_OBJECT.S
    *
    * Code: 32-bit array put operation.  Provides an "scale" variable
    *       specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the mov
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %eax                    # %eax<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $$0, %eax               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%eax), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    GET_VREG    rINST                   # rINST<- vAA
    lea         (%eax, %edx, 4), %edx   # %edx<- &vBB[vCC]
    cmp         $$0, rINST              # check for null reference
    je          .L${opcode}_skip_check  # reference is null so skip type check
    jmp         .L${opcode}_finish
%break

.L${opcode}_finish:
    movl        %edx, sReg0             # save &vBB[vCC]
    movl        %eax, sReg1             # save object head
    movl        offObject_clazz(rINST), %edx # %edx<- obj->clazz
    movl        %edx, -8(%esp)          # push parameter obj->clazz
    movl        offObject_clazz(%eax), %eax # %eax<- arrayObj->clazz
    movl        %eax, -4(%esp)          # push parameter arrayObj->clazz
    lea         -8(%esp), %esp
    call        dvmCanPutArrayElement   # test object type vs. array type
                                        # call: ClassObject* elemClass, ClassObject* arrayClass)
                                        # return: bool
    lea         8(%esp), %esp
    testl       %eax, %eax              # check for invalid array value
    je          common_errArrayStore    # handle invalid array value
    movl        sReg0, %edx             # restore &vBB[vCC]
    movl        rINST, offArrayObject_contents(%edx)
    movl        rGLUE, %eax
    FFETCH_ADV  2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    movl        offGlue_cardTable(%eax), %eax # get card table base
    movl        sReg1, %edx             # restore object head
    shrl        $$GC_CARD_SHIFT, %edx   # object head to card number
    movb        %al, (%eax, %edx)       # mark card using object head
    FGETOP_JMP  2, %ecx                 # jump to next instruction; getop, jmp
.L${opcode}_skip_check:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        rINST, offArrayObject_contents(%edx)
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
