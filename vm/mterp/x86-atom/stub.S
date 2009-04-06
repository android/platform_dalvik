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
    * File: stub.S
    */

    SAVE_PC_FP_TO_GLUE %edx             # save program counter and frame pointer
    pushl       rGLUE                   # push parameter glue
    call        dvmMterp_${opcode}      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
