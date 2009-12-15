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
    * File: OP_INVOKE_SUPER.S
    *
    * Code: Call super method.
    *
    * For: invoke-super, invoke-super/range
    *
    * Description: invoke-super is used to invoke the closest superclass's virtual
    *              method (as opposed to the one with the same method_id in the
    *              calling class).
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
    FETCH       2, %eax                 # %eax<- GFED or CCCC
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    .if         (!$isrange)
    and         $$15, %eax              # %eax<- D if not range
    .endif
    FETCH       1, %edx                 # %edx<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    cmp         $$0, (rFP, %eax, 4)     # check for null object
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved base method
    je          common_errNullObject    # handle null object
    jmp         .L${opcode}_continue2
%break

.L${opcode}_continue2:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    EXPORT_PC                           # must export for invoke
    cmp         $$0, %ecx               # check if already resolved
    jne         .L${opcode}_continue
    jmp         .L${opcode}_resolve     # handle resolve

   /*
    *  %ecx = resolved base method
    *  %eax = method->clazz
    */

.L${opcode}_continue:
    movl        offClassObject_super(%eax), %edx # %edx<- glue->method->clazz->super
    movzwl      offMethod_methodIndex(%ecx), %ecx # %ecx<-  baseMethod->methodIndex
    cmp          offClassObject_vtableCount(%edx), %ecx # compare vtableCount with methodIndex
    EXPORT_PC                           # must export for invoke
    jnc         .L${opcode}_nsm         # handle method not present
    movl        offClassObject_vtable(%edx), %edx # %edx<- glue->method->clazz->super->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethod${routine} # invoke method common code

.L${opcode}_resolve:
    movl        %eax, -12(%esp)         # push parameter clazz
    movl        %edx, -8(%esp)          # push parameter method index
    movl        $$METHOD_VIRTUAL, -4(%esp) # push parameter method type
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    movl        %eax, %ecx              # %ecx<- method
    cmp         $$0, %ecx               # check for null method return
    movl        -12(%esp), %eax         # %eax<- glue->method->clazz
    jne         .L${opcode}_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * Throw a NoSuchMethodError with the method name as the message.
    * %ecx = resolved base method
    */

.L${opcode}_nsm:
    movl        offMethod_name(%ecx), %edx # %edx<- method name
    jmp         common_errNoSuchMethod

