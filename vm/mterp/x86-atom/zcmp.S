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
    * File: zcmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform
    *
    * For: if-eqz, if-gez, if-gtz, if-lez, if-ltz, if-nez
    *
    * Description: Branch to the given destination if the given register's
    *              value compares with 0 as specified.
    *
    * Format: AA|op BBBB (21t)
    *
    * Syntax: op vAA, +BBBB
    */

    cmp         $$0, (rFP, rINST, 4)    # compare vAA with zero
    j${revcmp}  ${opcode}_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $$1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks2  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

${opcode}_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $$4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction
