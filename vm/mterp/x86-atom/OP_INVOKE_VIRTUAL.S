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
    * File: OP_INVOKE_VIRTUAL.S
    *
    * Code: Call a virtual method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-virtual, invoke-virtual/range
    *
    * Description: invoke-virtual is used to invoke a normal virtual method;
    *              a method that is not static or final, and is not a constructor.
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

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # must export pc for invoke
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offDvmDex_pResMethods(%eax), %eax # %eax<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if         (!$isrange)
    and         $$15, %edx              # %edx<- D if not range
    .endif
    cmp         $$0, (%eax, %ecx, 4)    # check if already resolved
    je          .L${opcode}_break
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved base method
    jmp         .L${opcode}_continue
%break

.L${opcode}_break:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %edx, -4(%esp)          # save "this" pointer register
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        $$METHOD_VIRTUAL, -8(%esp) # push parameter method type
    movl        %ecx, -12(%esp)         # push paramter method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    lea         -16(%esp), %esp
    movl        %eax, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $$0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    jne         .L${opcode}_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * At this point:
    *  %eax = resolved base method
    *  %edx = D or CCCC (index of first arg, which is the "this" ptr)
    */

.L${opcode}_continue:
    GET_VREG    %edx                    # %edx<- "this" ptr
    movzwl      offMethod_methodIndex(%eax), %eax # %eax<- baseMethod->methodIndex
    cmp         $$0, %edx               # %edx<- check for null "this"
    je          common_errNullObject    # handle null object
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %eax, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethod${routine} # invoke method common code

