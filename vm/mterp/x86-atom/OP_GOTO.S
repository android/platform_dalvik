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
    * File: OP_GOTO.S
    *
    * Code: Do an unconditional branch. Uses no substitutions.
    *
    * For: goto
    *
    * Description: Performs an unconditionally jump to the indicated instruction.
    *              The branch uses an 8-bit offset that cannot be zero.
    *
    * Format: AA|op (10t)
    *
    * Syntax: op +AA
    */

LOP_GOTO.S:

    movsbl      rINSTbl, %edx           # %edx<- +AA
    shl         $$1, %edx               # %edx is shifted for byte offset
    js          common_periodicChecks_backwardBranch  # do check on backwards branch
    FINISH_RB   %edx, %ecx              # jump to next instruction
