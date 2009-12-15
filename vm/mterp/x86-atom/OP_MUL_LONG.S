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
    * File: OP_MUL_LONG.S
    *
    * Code: 64-bit integer multiply
    *
    * For: mul-long
    *
    * Description: Multiply two source registers and store the
    *              result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

   /*
    * Signed 64-bit integer multiply.
    *
    * Consider WXxYZ (r1r0 x r3r2) with a long multiply:
    *        WX
    *      x YZ
    *  --------
    *     ZW ZX
    *  YW YX
    *
    * The low word of the result holds ZX, the high word holds
    * (ZW+YX) + (the high overflow from ZX).  YW doesn't matter because
    * it doesn't fit in the low 64 bits.
    */

    movl        rINST, -4(%esp)         # -4(%esp)<- AA+
    FETCH_BB    1, rINST                # rINST<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    jmp         .L${opcode}_finish
%break

   /*
    * X = (rFP, rINST, 4)
    * W = 4(rFP, rINST, 4)
    * Z = (rFP, %edx, 4)
    * Y = 4(rFP, %edx, 4)
    */

.L${opcode}_finish:
    movl        4(rFP, rINST, 4), %ecx  # %ecx<- W
    imull       (rFP, %edx, 4),  %ecx   # %ecx<- WxZ
    mov         4(rFP, %edx, 4), %eax   # %ecx<- Y
    imull       (rFP, rINST, 4), %eax   # %eax<- XxY
    addl        %eax, %ecx              # %ecx<- (WZ + XY)
    movl        (rFP, %edx, 4), %eax    # %eax<- Z
    mull        (rFP, rINST, 4)         # %edx:eax<- XZ
    movzbl      -4(%esp), rINST         # rINST<- AA
    addl        %edx, %ecx              # %ecx<- carry + (WZ + XY)
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1<- results hi
    movl        %eax, (rFP, rINST, 4)   # vAA<- results lo
    FINISH      2                       # jump to next instruction

