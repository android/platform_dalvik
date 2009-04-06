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
    * File: OP_NEW_ARRAY.S
    *
    * Code: Create a new array. Uses no substitutions.
    *
    * For: new-array
    *
    * Description: Construct a new array of the indicated type and size.
    *              The type must be an array type.
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- glue->pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    GET_VREG    %edx                    # %edx<- vB
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- glue->pDvmDex->pResClasses
    cmp         $$0, %edx               # check for negative length
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved class
    js          common_errNegativeArraySize # handle negative array length
    cmp         $$0, %eax               # check for null
    EXPORT_PC                           # required for resolve
    jne         .L${opcode}_finish      # already resovled so continue
    jmp         .L${opcode}_resolve     # need to resolve
%break

   /*
    * Resolve class.  (This is an uncommon case.)
    *
    *  %edx holds array length
    *  %ecx holds class ref CCCC
    */

.L${opcode}_resolve:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        %edx, -4(%esp)          # save length
    movl        $$0, -8(%esp)           # push parameter false
    movl        %ecx, -12(%esp)         # push parameter class ref
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        %eax, -16(%esp)         # push parameter clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer,
                                        #       u4 classIdx, bool fromUnverifiedConstant)
                                        # return: ClassObject*
    cmp         $$0, %eax               # check for failure
    lea         16(%esp), %esp
    je          common_exceptionThrown  # handle exception
    movl        -4(%esp), %edx          # %edx<- length

   /*
    * Finish allocation.
    *
    *  %eax holds class
    *  %edx holds array length
    */

.L${opcode}_finish:
    movl        %eax, -12(%esp)         # push parameter class
    movl        %edx, -8(%esp)          # push parameter length
    movl        $$ALLOC_DONT_TRACK, -4(%esp)
    lea         -12(%esp), %esp
    call        dvmAllocArrayByClass    # call: (ClassObject* arrayClass,
                                        # size_t length, int allocFlags)
                                        # return: ArrayObject*
    and         $$15, rINST             # rINST<- A
    cmp         $$0, %eax               # check for allocation failure
    lea         12(%esp), %esp
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vA<- pArray
    FINISH      2                       # jump to next instruction

