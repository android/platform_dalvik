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
    * File: OP_CMPL_FLOAT.S
    *
    * Code: Provides a "nan" variable to specify the return value for
    *       NaN. Provides a variable "sod" which appends a "s" or a "d"
    *       to the move and comparison instructions, depending on if we
    *       are working with a float or a double. For instructions
    *       cmpx-float and cmpx-double, the x will be eiher a g or a l
    *       to specify positive or negative bias for NaN.
    *
    * For: cmpg-double, dmpg-float, cmpl-double, cmpl-float
    *
    * Description: Perform the indicated floating point or long comparison,
    *              storing 0 if the two arguments are equal, 1 if the second
    *              argument is larger, or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

%default { "nan":"$0xFFFFFFFF" , "sod":"s" }

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC

    fld$sod     (rFP, %edx, 4)
    fld$sod     (rFP, %ecx, 4)

    fucompp
    fnstsw      %ax
    sahf

    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    ja          .L${opcode}_greater
    jp          .L${opcode}_finalNan
    jz          .L${opcode}_final

.L${opcode}_less:
    movl        $$0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

%break
.L${opcode}_greater:
    movl        $$0x1, (rFP, rINST, 4)  # vAA<- greater than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.L${opcode}_final:
    movl        $$0x0, (rFP, rINST, 4)  # vAA<- equal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.L${opcode}_finalNan:
    movl        $nan, (rFP, rINST, 4)   # vAA<- NaN
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp
