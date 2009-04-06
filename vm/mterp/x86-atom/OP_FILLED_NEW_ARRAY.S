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
    * File: OP_FILLED_NEW_ARRAY.S
    *
    * Code: Constructs and fills an array with the given data. Provides
    *
    * For: float-to-int
    *
    * Description: Construct an array of the given type and size,
    *              filling it with the supplied contents. The type
    *              must be an array type. The array's contents
    *              must be single-word. The constructed instance
    *              is stored as a result in the same way that the
    *              method invocation instructions store their results,
    *              so the constructed instance must be moved to a
    *              register with a subsequent move-result-object
    *              instruction.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc) (range)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, vtaboff@CCCC
    *         [B=4] op {vD, vE, vF, vG}, vtaboff@CCCC
    *         [B=3] op {vD, vE, vF}, vtaboff@CCCC
    *         [B=2] op {vD, vE}, vtaboff@CCCC
    *         [B=1] op {vD}, vtaboff@CCCC
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB
    *         op {vCCCC .. vNNNN}, type@BBBB
    */

%default { "isrange":"0" }

    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- glue->methodClassDex
    movl        offDvmDex_pResClasses(%edx), %edx # %edx<- glue->methodClassDex->pResClasses
    FETCH       1, %ecx                 # %ecx<- BBBB
    EXPORT_PC
    movl (%edx, %ecx, 4), %eax # %eax<- possibly resolved class
    cmp         $$0, %eax               # %eax<- check if already resolved
    jne         .L${opcode}_continue
    jmp         .L${opcode}_break
%break

.L${opcode}_break:
    movl        $$0, -8(%esp)           # push parameter false
    movl        %ecx, -12(%esp)         # push parameter BBBB
    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter glue->method->clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

   /*
    * On entry:
    *  %eax holds array class
    *  rINST holds BA or AA
    */

.L${opcode}_continue:
    movl        offClassObject_descriptor(%eax), %eax # %eax<- arrayClass->descriptor
    movzbl      1(%eax), %eax           # %eax<- descriptor[1]
    cmp         $$'I', %eax             # check if array of ints
    jne         .L${opcode}_notimpl     # jump to not implemented
    movl        rINST, -12(%esp)        # push parameter length
    movl        %eax, -16(%esp)         # push parameter descriptor[1]
    movl        $$ALLOC_DONT_TRACK, -8(%esp) # push parameter to allocate flags
    .if         (!$isrange)
    shrl        $$4, -12(%esp)          # parameter length is B
    .endif
    lea         -16(%esp), %esp
    call        dvmAllocPrimitiveArray  # call: (char type, size_t length, int allocFlags)
                                        # return: ArrayObject*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

    FETCH       2, %edx                 # %edx<- FEDC or CCCC
    movl        rGLUE, %ecx             # %ecx<- MterpGlue pointer
    movl        %eax, offGlue_retval(%ecx) # retval<- new array
    lea         offArrayObject_contents(%eax), %eax # %eax<- newArray->contents
    subl        $$1, -12(%esp)          # length--; check for negative
    js          2f                      # if length was zero, finish

   /*
    * copy values from registers into the array
    * %eax=array, %edx=CCCC/FEDC, -12(%esp)=length (from AA or B), rINST=AA/BA
    */

    .if         $isrange
    lea         (rFP, %edx, 4), %ecx    # %ecx<- &fpp[CCCC]
1:
    movl        (%ecx), %edx            # %edx<- %ecx++
    lea         4(%ecx), %ecx           # %ecx++
    movl        %edx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    subl        $$1, -12(%esp)          # length--
    jns         1b                      # or continue at 2
    .else
    cmp         $$4, -12(%esp)          # check length
    jne         1f                      # has four args
    and         $$15, rINST             # rINST<- A
    GET_VREG    rINST                   # rINST<- vA
    subl        $$1, -12(%esp)          # count--
    movl        rINST, 16(%eax)         # contents[4]<- vA
1:
    movl        %edx, %ecx              # %ecx<- %edx; ecx for temp
    andl        $$15, %ecx              # %ecx<- G/F/E/D
    GET_VREG    %ecx                    # %ecx<- vG/vF/vE/vD
    shr         $$4, %edx               # %edx<- put next reg in low 4
    subl        $$1, -12(%esp)          # count--
    movl        %ecx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    jns         1b                      # or continue at 2
    .endif
2:
    FINISH      3                       # jump to next instruction

   /*
    * Throw an exception to indicate this mode of filled-new-array
    * has not been implemented.
    */

.L${opcode}_notimpl:
    movl        $$.LstrInternalError, -8(%esp)
    movl        $$.LstrFilledNewArrayNotImpl, -4(%esp)
    lea         -8(%esp), %esp
    call        dvmThrowException # call: (const char* exceptionDescriptor,
                                  #        const char* msg)
                                  # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown

.if         (!$isrange)                 # define in one or the other, not both
.LstrFilledNewArrayNotImpl:
.asciz      "filled-new-array only implemented for 'int'"
.LstrInternalError:
.asciz  "Ljava/lang/InternalError;"
.endif
