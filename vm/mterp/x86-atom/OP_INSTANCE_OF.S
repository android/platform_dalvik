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
    * File: OP_INSTANCE_OF.S
    *
    * Code: Checks if object is instance of a class. Uses no substitutions.
    *
    * For: instance-of
    *
    * Description: Store in the given destination register 1 if the indicated
    *              reference is an instance of the given type, or 0 if not.
    *              The type must be a reference type (not a primitive type).
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    GET_VREG    %edx                    # %edx<- vB
    cmp         $$0, %edx               # check for null object
    je          .L${opcode}_store       # null object
    jmp         .L${opcode}_break
%break

.L${opcode}_break:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- CCCC
    movl        offDvmDex_pResClasses(%ecx), %ecx # %ecx<- pDvmDex->pResClasses
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved class
    movl        offObject_clazz(%edx), %edx # %edx<- obj->clazz
    cmp         $$0, %ecx               # check if already resovled
    je          .L${opcode}_resolve     # not resolved before, so resolve now

.L${opcode}_resolved:
    cmp         %ecx, %edx              # check if same class
    je          .L${opcode}_trivial     # yes, finish
    jmp         .L${opcode}_fullcheck   # no, do full check

   /*
    * The trivial test failed, we need to perform a full check.
    * %edx holds obj->clazz
    * %ecx holds class resolved from BBBB
    */

.L${opcode}_fullcheck:
    movl        %edx, -8(%esp)          # push parameter obj->clazz
    movl        %ecx, -4(%esp)          # push parameter resolved class
    lea         -8(%esp), %esp
    call        dvmInstanceofNonTrivial # perform full check
                                        # call: (ClassObject* instance, ClassObject* clazz)
                                        # return: int
    andl        $$15, rINST             # rINST<- A
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    lea         8(%esp), %esp
    SET_VREG    %eax, rINST             # vA<- r0
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

   /*
    * %edx holds boolean result
    */

.L${opcode}_store:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    andl        $$15, rINST             # rINST<- A
    SET_VREG    %edx, rINST             # vA<- r0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

   /*
    * Trivial test succeeded, save and bail.
    */

.L${opcode}_trivial:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    andl        $$15, rINST             # rINST<- A
    SET_VREG    $$1, rINST              # vA<- r0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

   /*
    * Resolution required.  This is the least-likely path.
    * %eax holds BBBB
    */

.L${opcode}_resolve:

    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    EXPORT_PC
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter glue->method->clazz
    movl        %eax, -8(%esp)          # push parameter CCCC; type index
    movl        $$1, -4(%esp)           # push parameter true
    lea         -12(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    cmp         $$0, %eax               # check for null
    je          common_exceptionThrown  # handle exception
    movl        rINST, %edx             # %edx<- BA+
    shr         $$4, %edx               # %edx<- B
    movl        %eax, %ecx              # need class in %ecx
    GET_VREG    %edx                    # %edx<- vB
    movl        offObject_clazz(%edx), %edx # %edx<- obj->clazz
    jmp         .L${opcode}_resolved    # clazz resolved, continue
