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
    * File: OP_INVOKE_DIRECT.S
    *
    * Code: Call a non-static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-direct, invoke-direct/range
    *
    * Description: invoke-direct is used to invoke a non-static direct method;
    *              an instance method that is non-overridable, for example,
    *              either a private instance method or a constructor.
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

    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    .if         (!$isrange)
    andl        $$15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    movl        %edx, -4(%esp)          # save "this" pointer register
    cmp         $$0, %ecx               # check if already resolved
    GET_VREG    %edx                    # %edx<- "this" pointer
    je          .L${opcode}_resolve     # handle resolve

.L${opcode}_finish:
    cmp         $$0, %edx               # check for null "this"
    jne         common_invokeMethod${routine} # invoke method common code
    jmp         common_errNullObject
%break

   /*
    * %eax = reference (BBBB or CCCC)
    * -4(%esp) = "this" register
    */

.L${opcode}_resolve:
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        $$METHOD_DIRECT, -8(%esp) # push parameter method type
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        %eax, -12(%esp)         # push parameter reference
    lea         -16(%esp), %esp
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    GET_VREG    %edx                    # get "this" pointer
    je          common_exceptionThrown  # null pointer; handle exception
    cmp         $$0, %edx               # check for null "this"
    movl        %eax, %ecx              # %ecx<- method
    jne         common_invokeMethod${routine} # invoke method common code
    jmp         common_errNullObject    # handle null object
