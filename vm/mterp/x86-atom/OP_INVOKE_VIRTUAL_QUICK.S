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
    * File: OP_INVOKE_VIRTUAL_QUICK.S
    *
    * Code: Optimization for invoke-virtual and invoke-virtual/range
    *
    * For: invoke-virtual/quick, invoke-virtual/quick-range
    */

%default { "isrange":"0", "routine":"NoRange" }

    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if (!$isrange)
    and         $$15, %edx              # %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    GET_VREG    %edx                    # %edx<- "this" ptr
    cmp         $$0, %edx               # %edx<- check for null "this"
    EXPORT_PC                           # must export pc for invoke
    je          common_errNullObject
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethod${routine} # invoke method common code


