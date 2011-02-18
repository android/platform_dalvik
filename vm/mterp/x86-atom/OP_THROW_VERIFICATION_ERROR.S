   /* Copyright (C) 2009 The Android Open Source Project
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
    * File: OP_THROW_VERIFICATION_ERROR.S
    *
    * Code:
    *
    * For: throw-verification-error
    *
    * Description: Throws an exception for an error discovered during verification.
    *              The exception is indicated by AA with details provided by BBBB.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, ref@BBBB
    */

    movl        rGLUE, %edx                  # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %ecx   # %ecx<- glue->method
    EXPORT_PC                                # in case an exception is thrown
    FETCH       1, %eax                      # %eax<- BBBB
    movl        %eax, -4(%esp)               # push parameter BBBB; ref
    movl        rINST, -8(%esp)              # push parameter AA
    movl        %ecx, -12(%esp)              # push parameter glue->method
    lea         -12(%esp), %esp
    call        dvmThrowVerificationError    # call: (const Method* method, int kind, int ref)
    jmp         common_exceptionThrown       # failed; handle exception
