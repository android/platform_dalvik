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
    * File: OP_CONST_STRING_JUMBO.S
    *
    * Code: Move a string reference to a register. Uses no substitutions.
    *
    * For: const/string-jumbo
    *
    * Description: Move a reference to the string specified by the given
    *              index into the specified register. vAA <- pResString[BBBB]
    *
    * Format: AA|op BBBBlo BBBBhi (31c)
    *
    * Syntax: op vAA, string@BBBBBBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- glue->methodClassDex
    movl        offDvmDex_pResStrings(%eax), %eax # %eax<- glue->methodClassDex->pResStrings
    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $$16, %edx              # %edx<- prepare to create &BBBBBBBB
    or          %edx, %ecx              # %ecx<- &BBBBBBBB
    movl        (%eax, %ecx, 4), %eax   # %eax<- pResStrings[BBBB]
    cmp         $$0, %eax               # check if string is resolved
    je          .L${opcode}_resolve     # resolve string reference
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FINISH      3                       # jump to next instruction
%break


   /*
    * Continuation if the Class has not yet been resolved.
    *  %ecx: BBBB (Class ref)
    *  need: target register
    */
.L${opcode}_resolve:
    EXPORT_PC
    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx <- glue->method->clazz
    movl        %ecx, -4(%esp)          # push parameter class ref
    movl        %edx, -8(%esp)          # push parameter glue->method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveString        # resolve string reference
                                        # call: (const ClassObject* referrer, u4 stringIdx)
                                        # return: StringObject*
    lea         8(%esp), %esp
    cmp         $$0, %eax               # check if resolved string failed
    je          common_exceptionThrown  # resolve failed; exception thrown
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FINISH      3                       # jump to next instruction
