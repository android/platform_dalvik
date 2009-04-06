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
    * File: OP_SGET_WIDE.S
    *
    * Code: 64-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-wide
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field, loading or storing
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %edx                 # %edx<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $$0, (%ecx, %edx, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .L${opcode}_resolve

.L${opcode}_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        offStaticField_value(%ecx), %xmm0 # %xmm0<- field value
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- field value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
%break

   /*
    * Continuation if the field has not yet been resolved.
    *  %edx: BBBB field ref
    */

.L${opcode}_resolve:
    movl        offGlue_method(%eax), %eax # %eax <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %edx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%eax), %eax # %eax<- method->clazz
    movl        %eax, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    lea         8(%esp), %esp
    cmp         $$0, %eax               # check if initalization failed
    movl        %eax, %ecx              # %ecx<- result
    jne         .L${opcode}_finish      # success, continue
    jmp         common_exceptionThrown  # failed; handle exception
