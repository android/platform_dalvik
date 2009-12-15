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
    * File: OP_INVOKE_INTERFACE.S
    *
    * Code: Call at method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_interface that allows up to 255 arguments.
    *
    * For: invoke-interface, invoke-interface-range
    *
    * Description: invoke-interface is used to invoke an interface method; on an
    *              object whose concrete class isn't known, using a method_id that
    *              refers to an interface.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */

%default { "isrange":"0", "routine":"NoRange" }

    FETCH       2, %edx                 # %edx<- GFED or CCCC
    FETCH       1, %ecx                 # %ecx<- method index
    movl        %ecx, -12(%esp)         # push argument method index
    .if         (!$isrange)
    and         $$15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    GET_VREG    %edx                    # %edx<- first arg "this pointer"
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- glue->pDvmDex
    movl        %eax, -4(%esp)          # push parameter class
    cmp         $$0, %edx               # check for null object
    je          common_errNullObject    # handle null object
    jmp         .L${opcode}_break
%break
.L${opcode}_break:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        %ecx, -8(%esp)          # push parameter method
    movl        offObject_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter
    lea         -16(%esp), %esp
    call        dvmFindInterfaceMethodInCache # call: (ClassObject* thisClass, u4 methodIdx,
                                              #       const Method* method, DvmDex* methodClassDex)
                                              # return: Method*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check if find failed
    je          common_exceptionThrown  # handle exception
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethod${routine} # invoke method common code
