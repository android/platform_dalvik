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
    * File: OP_FLOAT_TO_DOUBLE.S
    *
    * Code: Converts a float to a double. Uses no substitutions.
    *
    * For: float-to-double
    *
    * Description: Convert the float in source register to a double
    *              and store the result in the destintation register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $$4, rINST              # rINST<- B
    and         $$15, %edx              # %edx<- A
    flds        (rFP, rINST, 4)         # load float
    fstpl       (rFP, %edx, 4)          # store double
    FINISH      1                       # jump to next instruction
