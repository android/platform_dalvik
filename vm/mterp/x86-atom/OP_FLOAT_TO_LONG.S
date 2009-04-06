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
    * File: OP_FLOAT_TO_LONG.S
    *
    * Code: Converts a float to a long. Uses no substitutions.
    *
    * For: float-to-long
    *
    * Description: Convert the float in source register to a long
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
    fildll      .LvaluePosInfLong       # push max int value
    fildll      .LvalueNegInfLong       # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .L${opcode}_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .L${opcode}_nanInf      # handle posInf or NaN
    jmp         .L${opcode}_break       # do conversion
%break

.L${opcode}_break:
    fnstcw      -2(%esp)                # save control word
    orl         $$0xc00, -2(%esp)       # update control
    fldcw       -2(%esp)                # load control word
    xorl        $$0xc00, -2(%esp)       # reset control
    fistpll     (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.L${opcode}_nanInf:
    jnp         .L${opcode}_posInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNanLong, %xmm0   # %xmm0<- NaN
    movq        %xmm0,  (rFP, %edx, 4)  # vA<- %xmm0; NaN
    FINISH      1                       # jump to next instruction

.L${opcode}_posInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvaluePosInfLong, %xmm0 # %xmm0<- posInf
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; posInf
    FINISH      1                       # jump to next instruction

.L${opcode}_negInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNegInfLong, %xmm0 # %xmm0<- negInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; negInf
    FINISH      1                       # jump to next instruction


