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
    * File: OP_INVOKE_SUPER_QUICK.S
    *
    * Code: Optimization for invoke-super and invoke-super/range
    *
    * For: invoke-super/quick, invoke-super/quick-range
    */

%default { "isrange":"0", "routine":"NoRange" }

    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %eax # %eax<- glue->method
    .if         (!$isrange)
    and         $$15, %edx              #  %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        offClassObject_super(%eax), %eax # %eax<- glue->method->clazz->super
    EXPORT_PC                           # must export for invoke
    movl        offClassObject_vtable(%eax), %eax # %edx<- glue->method->clazz->super->vtable
    cmp         $$0, (rFP, %edx, 4)     # check for null object
    movl        (%eax, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    je          common_errNullObject    # handle null object
    jmp         common_invokeMethod${routine} # invoke method common code



