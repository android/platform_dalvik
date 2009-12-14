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
    * File: OP_MUL_INT_2ADDR.S
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, %edx               # %edx<- B
    andl        $$15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vA
    imul        (rFP, %edx, 4), %ecx    # %ecx<- vA * vB
    addl        $$2, rPC                  # update PC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP2 %eax                    # jump to next instruction; getop, jmp
