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
    * File: OP_NEW_INSTANCE.S
    *
    * Code: Create a new instance of a given type. Uses no substitutions.
    *
    * For: new-instance
    *
    * Description: Construct a new instance of the indicated type,
    *              storing a reference to it in the destination.
    *              The type must refer to a non-array class.
    *
    *
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, type@BBBB
    *         op vAA, field@BBBB
    *         op vAA, string@BBBB
    */

    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %edx                 # %edx<- BBBB
    movl        offDvmDex_pResClasses(%ecx), %ecx # %ecx<- glue->pDvmDex->pResClasses
    movl        (%ecx, %edx, 4), %edx   # %edx<- vB
    EXPORT_PC                           # required for resolve
    cmp         $$0, %edx               # check for null
    je          .L${opcode}_resolve     # need to resolve

   /*
    *  %edx holds class object
    */

.L${opcode}_resolved:
    movzbl      offClassObject_status(%edx), %eax # %eax<- class status
    cmp         $$CLASS_INITIALIZED, %eax # check if class is initialized
    jne         .L${opcode}_needinit    # initialize class

   /*
    *  %edx holds class object
    */

.L${opcode}_initialized:
    testl       $$(ACC_INTERFACE|ACC_ABSTRACT), offClassObject_accessFlags(%edx)
    mov         $$ALLOC_DONT_TRACK, %eax # %eax<- flag for alloc call
    je          .L${opcode}_finish      # continue
    jmp         .L${opcode}_abstract    # handle abstract or interface

   /*
    *  %edx holds class object
    *  %eax holds flags for alloc call
    */

%break
.balign 32
.L${opcode}_finish:
    movl        %edx, -8(%esp)          # push parameter object
    movl        %eax, -4(%esp)          # push parameter flags
    lea         -8(%esp), %esp
    call        dvmAllocObject          # call: (ClassObject* clazz, int flags)
                                        # return: Object*
    cmp         $$0, %eax               # check for failure
    lea         8(%esp), %esp
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vAA<- pObject
    FINISH      2                       # jump to next instruction

   /*
    * Class initialization required.
    *
    *  %edx holds class object
    */

.L${opcode}_needinit:
    movl        %edx, -4(%esp)          # push parameter object
    lea         -4(%esp), %esp
    call        dvmInitClass            # call: (ClassObject* clazz)
                                        # return: bool
    lea         4(%esp), %esp
    cmp         $$0, %eax               # check for failure
    movl        -4(%esp), %edx          # %edx<- object
    je          common_exceptionThrown  # handle exception
    testl       $$(ACC_INTERFACE|ACC_ABSTRACT), offClassObject_accessFlags(%edx)
    mov         $$ALLOC_DONT_TRACK, %eax # %eax<- flag for alloc call
    je          .L${opcode}_finish      # continue
    jmp         .L${opcode}_abstract    # handle abstract or interface

   /*
    * Resolution required.  This is the least-likely path.
    *
    *  BBBB in %eax
    */

.L${opcode}_resolve:


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter clazz
    movl        $$0, -4(%esp)           # push parameter false
    movl        %eax, -8(%esp)          # push parameter BBBB
    lea         -12(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer,
                                        #       u4 classIdx, bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    movl        %eax, %edx              # %edx<- pObject
    cmp         $$0, %edx               # check for failure
    jne         .L${opcode}_resolved    # continue
    jmp         common_exceptionThrown  # handle exception

   /*
    * We can't instantiate an abstract class or interface, so throw an
    * InstantiationError with the class descriptor as the message.
    *
    *  %edx holds class object
    */

.L${opcode}_abstract:
    movl        offClassObject_descriptor(%edx), %ecx # %ecx<- descriptor
    movl        %ecx, -4(%esp)          # push parameter descriptor
    movl        $$.LstrInstantiationErrorPtr, -8(%esp) # push parameter message
    lea         -8(%esp), %esp
    call        dvmThrowExceptionWithClassMessage # call: (const char* exceptionDescriptor,
                                                  #        const char* messageDescriptor)
                                                  # return: void
    jmp         common_exceptionThrown  # handle exception

.LstrInstantiationErrorPtr:
.asciz      "Ljava/lang/InstantiationError;"
