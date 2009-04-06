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
    * File: OP_CHECK_CAST.S
    *
    * Code: Checks to see if a cast is allowed. Uses no substitutions.
    *
    * For: check-cast
    *
    * Description: Throw if the reference in the given register cannot be
    *              cast to the indicated type. The type must be a reference
    *              type (not a primitive type).
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, type@BBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- pDvmDex
    GET_VREG    rINST                   # rINST<- vAA
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- pDvmDex->pResClasses
    cmp         $$0, rINST              # check for null reference object
    je          .L${opcode}_okay        # can always cast null object
    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        (%eax, %ecx, 4), %ecx   # %ecx<- resolved class
    cmp         $$0, %ecx               # check if classes is resolved before?
    je          .L${opcode}_resolve     # resolve class
    jmp         .L${opcode}_resolved    # continue
%break

.L${opcode}_resolved:
    cmp         %ecx, offObject_clazz(rINST) # check for same class
    jne         .L${opcode}_fullcheck   # not same class; do full check

.L${opcode}_okay:
    FINISH      2                       # jump to next instruction

   /*
    *  Trivial test failed, need to perform full check.
    *  offObject_clazz(rINST) holds obj->clazz
    *  %ecx holds class resolved from BBBB
    *  rINST holds object
    */

.L${opcode}_fullcheck:
    movl        offObject_clazz(rINST), %eax  # %eax<- obj->clazz
    movl        %eax, -12(%esp)         # push parameter obj->clazz
    movl        %ecx, -8(%esp)          # push parameter # push parameter resolved class
    lea         -12(%esp), %esp
    call        dvmInstanceofNonTrivial # call: (ClassObject* instance, ClassObject* clazz)
                                        # return: int
    lea         12(%esp), %esp
    cmp         $$0, %eax               # failed?
    jne         .L${opcode}_okay        # success

   /*
    * A cast has failed.  We need to throw a ClassCastException with the
    * class of the object that failed to be cast.
    */

    EXPORT_PC                           # we will throw an exception
    movl        $$.LstrClassCastExceptionPtr, -8(%esp) # push parameter message
    movl        offObject_clazz(rINST), rINST # rINST<- obj->clazz
    movl        offClassObject_descriptor(rINST), rINST # rINST<- obj->clazz->descriptor
    movl        rINST, -4(%esp)         # push parameter obj->clazz->descriptor
    lea         -8(%esp), %esp
    call        dvmThrowExceptionWithClassMessage # call: (const char* exceptionDescriptor,
                                                  #       const char* messageDescriptor, Object* cause)
                                                  # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown

   /*
    * Resolution required.  This is the least-likely path.
    *
    *  rINST holds object
    */

.L${opcode}_resolve:
    movl        offGlue_method(%edx), %eax # %eax<- glue->method
    FETCH       1, %ecx                 # %ecx holds BBBB
    EXPORT_PC                           # in case we throw an exception
    movl        $$0, -8(%esp)           # push parameter false
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter BBBB
    movl        %eax, -16(%esp)         # push parameter glue->method>clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # resolve ClassObject pointer
                                        # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return ClassObject*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check for null pointer
    je          common_exceptionThrown  # handle excpetion
    movl        %eax, %ecx              # %ecx<- resolved class
    jmp         .L${opcode}_resolved

.LstrClassCastExceptionPtr:
.asciz      "Ljava/lang/ClassCastException;"
