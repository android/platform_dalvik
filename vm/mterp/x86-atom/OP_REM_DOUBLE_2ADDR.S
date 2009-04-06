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
    * File: OP_REM_DOUBLE_2ADDR.S
    *
    * Code: Computes the remainder of a division. Performs no substitutions.
    *
    * For: rem-double/2addr
    *
    * Description: Calls fmod to compute the remainder of the result of dividing a
    *              source register by a second, and stores the result in the first
    *              source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    and         $$15, rINST             # rINST<- A
    shr         $$4, %edx               # %edx<- B
    movl        (rFP, rINST, 4), %eax   # %eax<- vAlo
    movl        %eax, -20(%esp)         # push parameter vAAlo
    movl        4(rFP, rINST, 4), %eax  # %eax<- vAhi
    movl        %eax, -16(%esp)         # push parameter vAAhi
    movl        (rFP, %edx, 4), %eax    # %eax<- vBlo
    movl        %eax, -12(%esp)         # push parameter vBBlo
    movl        4(rFP, %edx, 4), %eax   # %eax<- vBhi
    movl        %eax, -8(%esp)          # push parameter vBBhi
    lea         -20(%esp), %esp
    jmp         .L${opcode}_break
%break

.L${opcode}_break:
    call        fmod                    # call: (long double x, long double y)
                                        # return: double
    lea         20(%esp), %esp
    fstpl       (rFP, rINST, 4)         # vAA<- remainder; return of fmod
    FINISH      1                       # jump to next instruction








