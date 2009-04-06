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
    * File: OP_MOVE_FROM16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/from16, move-object/from16
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: AA|op BBBB (22x)
    *
    * Syntax: op vAA, vBBBB
    */

    FETCH       1, %edx                 # %edx<- BBBB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, rINST             # vA<- vB; %edx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
