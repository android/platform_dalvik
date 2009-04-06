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
    * File: OP_INVOKE_STATIC.S
    *
    * Code: Call static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_static that allows up to 255 arguments.
    *
    * For: invoke-static, invoke-static/range
    *
    * Description: invoke-static is used to invoke static direct method.
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

%default { "routine":"NoRange" }

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %edx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %edx<- pDvmDex->pResMethods
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    cmp         $$0, %ecx               # check if already resolved
    EXPORT_PC                           # must export for invoke
    jne         common_invokeMethod${routine} # invoke method common code
    jmp         .L${opcode}_break
%break

.L${opcode}_break:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        $$METHOD_STATIC, -4(%esp) # resolver method type
    movl        %eax, -8(%esp)          # push parameter method index
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -12(%esp)         # push parameter method
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    cmp         $$0, %eax               # check for null method
    je          common_exceptionThrown
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethod${routine} # invoke method common code


