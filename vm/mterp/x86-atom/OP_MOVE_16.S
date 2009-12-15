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
    * File: OP_MOVE_16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/16, move-object/16
    *
    * Description: Copies contents from one non-object register to another.
    *              fp[A]<- fp[B]
    *
    * Format: ØØ|op AAAA BBBB (32x)
    *
    * Syntax: op vAAAA, vBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBB
    FETCH       1, %ecx                 # %ecx<- AAAA
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, %ecx              # vA<- vB; %edx
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp
