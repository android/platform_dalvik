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
    * File: OP_CONST_CLASS.S
    *
    * Code: Move a class reference to a register. Uses no substitutions.
    *
    * For: const/class
    *
    * Description: Move a reference to the class specified
    *              by the given index into the specified register.
    *              In the case where the indicated type is primitive,
    *              this will store a reference to the primitive type's
    *              degenerate class.
    *
    * Format: AA|op BBBBlo BBBBhi (21c)
    *
    * Syntax: op vAA, field@BBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- pDvmDex
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- pDvmDex->pResClasses
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved class
    cmp         $$0, %eax               # check if classes is resolved before?
    je          .L${opcode}_resolve     # resolve class
    SET_VREG    %eax, rINST             # vAA<- resolved class
    FINISH      2                       # jump to next instruction
%break

   /*
    * Continuation if the Class has not yet been resolved.
    *  %ecx: BBBB (Class ref)
    *  need: target register
    */

.L${opcode}_resolve:
    EXPORT_PC
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        $$1, -4(%esp)           # push parameter true
    movl        %ecx, -8(%esp)          # push parameter
    movl        %edx, -12(%esp)         # push parameter glue->method->clazz
    lea         -12(%esp), %esp
    call        dvmResolveClass         # resolve ClassObject pointer
                                        # class: (const ClassObject* referrer, u4 classIdx,
                                        #         bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    cmp         $$0, %eax               # check for null pointer
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vAA<- resolved class
    FINISH      2                       # jump to next instruction