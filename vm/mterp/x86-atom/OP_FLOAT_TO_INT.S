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
    * File: OP_FLOAT_TO_INT.S
    *
    * Code: Converts a float to a int. Uses no substitutions.
    *
    * For: float-to-int
    *
    * Description: Convert the float in source register to a int
    *              and store the result in the destintation register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, rINST              # rINST<- B
    and         $$15, %edx              # %edx<- A
    flds        (rFP, rINST, 4)         # push vB to floating point stack
    fildl       .LintMax                # push max int value
    fildl       .LintMin                # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .L${opcode}_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .L${opcode}_nanInf      # handle posInf or NaN
    jmp         .L${opcode}_break       # do conversion
%break

.L${opcode}_break:
    fnstcw      -2(%esp)                # save control word
    orl         $$0xc00, -2(%esp)       # reset control
    fldcw       -2(%esp)                # load control word
    xorl        $$0xc00, -2(%esp)       # reset control
    fistpl      (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.L${opcode}_nanInf:
    jnp         .L${opcode}_posInf      # handle posInf
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $$0x00000000,  (rFP, %edx, 4) # vA<- NaN
    FINISH      1                       # jump to next instruction

.L${opcode}_posInf:
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $$0x7FFFFFFF,  (rFP, %edx, 4) # vA<- posInf
    FINISH      1                       # jump to next instruction

.L${opcode}_negInf:
    fstps       (rFP, %edx, 4)          # pop floating point stack
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $$0x80000000, (rFP, %edx, 4) # vA<- negInf
    FINISH      1                       # jump to next instruction
