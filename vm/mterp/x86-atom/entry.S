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
    * File: entry.S
    */

#define ASSIST_DEBUGGER 1
    .text
    .align      2
    .global     dvmMterpStdRun
    .type       dvmMterpStdRun, %function

   /*
    * Save registers, initialize sp and fp.
    * On entry:
    *     bool MterpGlue(glue *)
    */

    .macro      MTERP_ENTRY
    movl        4(%esp), %ecx           # get first argument
    movl        %ebp, -4(%esp)          # save caller base pointer
    movl        %ebx, -8(%esp)          # save %ebx
    movl        %esi, -12(%esp)         # save %esi
    movl        %edi, -16(%esp)         # save %edi
    lea         -40(%esp), %ebp         # set callee base pointer
    lea         -40(%esp), %esp         # set callee stack pointer
    .endm

   /*
    * Restore registers.
    * This function returns a boolean "changeInterp" value.
    * The return value is from dvmMterpStdBail().
    */

    .macro      MTERP_EXIT
    lea         40(%esp), %esp          # correct stack pointer
    movl        -16(%esp), %edi         # restore %edi
    movl        -12(%esp), %esi         # restore %esi
    movl        -8(%esp), %ebx          # restore %ebx
    movl        -4(%esp), %ebp          # restore caller base pointer
    ret                                 # return
    .endm

   /*
    * DvmMterpStdRun entry point: save stack pointer, setup memory locations, get
    * entry point, start executing instructions.
    */

dvmMterpStdRun:
    MTERP_ENTRY
    movl        %ecx, rGLUE             # save value for pMterpGlue
    movl        offGlue_pc(%ecx), rPC   # get program counter
    cmp         $$kInterpEntryInstr, offGlue_entryPoint(%ecx) # check instruction
    movl        offGlue_fp(%ecx), rFP   # get frame pointer
    movl        %esp, offGlue_bailPtr(%ecx) # save SP for eventual return
    FFETCH      %edx                    # %edx<- opcode
    jne         .Lnot_instr             # no, handle it
    FGETOP_JMPa %edx                    # start executing the instruction at rPC

   /*
    * Not an instruction. Are we returning from a method?
    */

.Lnot_instr:
    cmpl        $$kInterpEntryReturn, offGlue_entryPoint(%ecx)
    je          common_returnFromMethod

   /*
    * No, are we throwing an exception?
    */

.Lnot_return:
    cmpl        $$kInterpEntryThrow, offGlue_entryPoint(%ecx)
    je          common_exceptionThrown

   /*
    * No, then we must abort.
    */

.Lbad_arg:
    pushl       offGlue_entryPoint(%ecx)
    movl        $$.LstrBadEntryPoint, -4(%esp)
    lea         -4(%esp), %esp
    call        printf
    lea         8(%esp), %esp
    call        dvmAbort                # call (void)

   /*
    * Restore the stack pointer and PC from the save point established on entry and
    * return to whoever called dvmMterpStdRun.
    *
    * On entry:
    *  4(%esp) MterpGlue* glue
    *  8(%esp) bool changeInterp
    */

    .global     dvmMterpStdBail
    .type       dvmMterpStdBail, %function

dvmMterpStdBail:
    movl        4(%esp), %ecx           # get first argument
    movl        8(%esp), %eax           # get second argument
    movl        offGlue_bailPtr(%ecx), %esp # sp <- saved SP
    MTERP_EXIT

   /*
    * String references.
    */

.LstrBadEntryPoint:
    .asciz "Bad entry point %d\n"


dvmAsmInstructionJmpTable = .LdvmAsmInstructionJmpTable
.LdvmAsmInstructionJmpTable:
.long .L_OP_NOP
.long .L_OP_MOVE
.long .L_OP_MOVE_FROM16
.long .L_OP_MOVE_16
.long .L_OP_MOVE_WIDE
.long .L_OP_MOVE_WIDE_FROM16
.long .L_OP_MOVE_WIDE_16
.long .L_OP_MOVE_OBJECT
.long .L_OP_MOVE_OBJECT_FROM16
.long .L_OP_MOVE_OBJECT_16
.long .L_OP_MOVE_RESULT
.long .L_OP_MOVE_RESULT_WIDE
.long .L_OP_MOVE_RESULT_OBJECT
.long .L_OP_MOVE_EXCEPTION
.long .L_OP_RETURN_VOID
.long .L_OP_RETURN
.long .L_OP_RETURN_WIDE
.long .L_OP_RETURN_OBJECT
.long .L_OP_CONST_4
.long .L_OP_CONST_16
.long .L_OP_CONST
.long .L_OP_CONST_HIGH16
.long .L_OP_CONST_WIDE_16
.long .L_OP_CONST_WIDE_32
.long .L_OP_CONST_WIDE
.long .L_OP_CONST_WIDE_HIGH16
.long .L_OP_CONST_STRING
.long .L_OP_CONST_STRING_JUMBO
.long .L_OP_CONST_CLASS
.long .L_OP_MONITOR_ENTER
.long .L_OP_MONITOR_EXIT
.long .L_OP_CHECK_CAST
.long .L_OP_INSTANCE_OF
.long .L_OP_ARRAY_LENGTH
.long .L_OP_NEW_INSTANCE
.long .L_OP_NEW_ARRAY
.long .L_OP_FILLED_NEW_ARRAY
.long .L_OP_FILLED_NEW_ARRAY_RANGE
.long .L_OP_FILL_ARRAY_DATA
.long .L_OP_THROW
.long .L_OP_GOTO
.long .L_OP_GOTO_16
.long .L_OP_GOTO_32
.long .L_OP_PACKED_SWITCH
.long .L_OP_SPARSE_SWITCH
.long .L_OP_CMPL_FLOAT
.long .L_OP_CMPG_FLOAT
.long .L_OP_CMPL_DOUBLE
.long .L_OP_CMPG_DOUBLE
.long .L_OP_CMP_LONG
.long .L_OP_IF_EQ
.long .L_OP_IF_NE
.long .L_OP_IF_LT
.long .L_OP_IF_GE
.long .L_OP_IF_GT
.long .L_OP_IF_LE
.long .L_OP_IF_EQZ
.long .L_OP_IF_NEZ
.long .L_OP_IF_LTZ
.long .L_OP_IF_GEZ
.long .L_OP_IF_GTZ
.long .L_OP_IF_LEZ
.long .L_OP_UNUSED_3E
.long .L_OP_UNUSED_3F
.long .L_OP_UNUSED_40
.long .L_OP_UNUSED_41
.long .L_OP_UNUSED_42
.long .L_OP_UNUSED_43
.long .L_OP_AGET
.long .L_OP_AGET_WIDE
.long .L_OP_AGET_OBJECT
.long .L_OP_AGET_BOOLEAN
.long .L_OP_AGET_BYTE
.long .L_OP_AGET_CHAR
.long .L_OP_AGET_SHORT
.long .L_OP_APUT
.long .L_OP_APUT_WIDE
.long .L_OP_APUT_OBJECT
.long .L_OP_APUT_BOOLEAN
.long .L_OP_APUT_BYTE
.long .L_OP_APUT_CHAR
.long .L_OP_APUT_SHORT
.long .L_OP_IGET
.long .L_OP_IGET_WIDE
.long .L_OP_IGET_OBJECT
.long .L_OP_IGET_BOOLEAN
.long .L_OP_IGET_BYTE
.long .L_OP_IGET_CHAR
.long .L_OP_IGET_SHORT
.long .L_OP_IPUT
.long .L_OP_IPUT_WIDE
.long .L_OP_IPUT_OBJECT
.long .L_OP_IPUT_BOOLEAN
.long .L_OP_IPUT_BYTE
.long .L_OP_IPUT_CHAR
.long .L_OP_IPUT_SHORT
.long .L_OP_SGET
.long .L_OP_SGET_WIDE
.long .L_OP_SGET_OBJECT
.long .L_OP_SGET_BOOLEAN
.long .L_OP_SGET_BYTE
.long .L_OP_SGET_CHAR
.long .L_OP_SGET_SHORT
.long .L_OP_SPUT
.long .L_OP_SPUT_WIDE
.long .L_OP_SPUT_OBJECT
.long .L_OP_SPUT_BOOLEAN
.long .L_OP_SPUT_BYTE
.long .L_OP_SPUT_CHAR
.long .L_OP_SPUT_SHORT
.long .L_OP_INVOKE_VIRTUAL
.long .L_OP_INVOKE_SUPER
.long .L_OP_INVOKE_DIRECT
.long .L_OP_INVOKE_STATIC
.long .L_OP_INVOKE_INTERFACE
.long .L_OP_UNUSED_73
.long .L_OP_INVOKE_VIRTUAL_RANGE
.long .L_OP_INVOKE_SUPER_RANGE
.long .L_OP_INVOKE_DIRECT_RANGE
.long .L_OP_INVOKE_STATIC_RANGE
.long .L_OP_INVOKE_INTERFACE_RANGE
.long .L_OP_UNUSED_79
.long .L_OP_UNUSED_7A
.long .L_OP_NEG_INT
.long .L_OP_NOT_INT
.long .L_OP_NEG_LONG
.long .L_OP_NOT_LONG
.long .L_OP_NEG_FLOAT
.long .L_OP_NEG_DOUBLE
.long .L_OP_INT_TO_LONG
.long .L_OP_INT_TO_FLOAT
.long .L_OP_INT_TO_DOUBLE
.long .L_OP_LONG_TO_INT
.long .L_OP_LONG_TO_FLOAT
.long .L_OP_LONG_TO_DOUBLE
.long .L_OP_FLOAT_TO_INT
.long .L_OP_FLOAT_TO_LONG
.long .L_OP_FLOAT_TO_DOUBLE
.long .L_OP_DOUBLE_TO_INT
.long .L_OP_DOUBLE_TO_LONG
.long .L_OP_DOUBLE_TO_FLOAT
.long .L_OP_INT_TO_BYTE
.long .L_OP_INT_TO_CHAR
.long .L_OP_INT_TO_SHORT
.long .L_OP_ADD_INT
.long .L_OP_SUB_INT
.long .L_OP_MUL_INT
.long .L_OP_DIV_INT
.long .L_OP_REM_INT
.long .L_OP_AND_INT
.long .L_OP_OR_INT
.long .L_OP_XOR_INT
.long .L_OP_SHL_INT
.long .L_OP_SHR_INT
.long .L_OP_USHR_INT
.long .L_OP_ADD_LONG
.long .L_OP_SUB_LONG
.long .L_OP_MUL_LONG
.long .L_OP_DIV_LONG
.long .L_OP_REM_LONG
.long .L_OP_AND_LONG
.long .L_OP_OR_LONG
.long .L_OP_XOR_LONG
.long .L_OP_SHL_LONG
.long .L_OP_SHR_LONG
.long .L_OP_USHR_LONG
.long .L_OP_ADD_FLOAT
.long .L_OP_SUB_FLOAT
.long .L_OP_MUL_FLOAT
.long .L_OP_DIV_FLOAT
.long .L_OP_REM_FLOAT
.long .L_OP_ADD_DOUBLE
.long .L_OP_SUB_DOUBLE
.long .L_OP_MUL_DOUBLE
.long .L_OP_DIV_DOUBLE
.long .L_OP_REM_DOUBLE
.long .L_OP_ADD_INT_2ADDR
.long .L_OP_SUB_INT_2ADDR
.long .L_OP_MUL_INT_2ADDR
.long .L_OP_DIV_INT_2ADDR
.long .L_OP_REM_INT_2ADDR
.long .L_OP_AND_INT_2ADDR
.long .L_OP_OR_INT_2ADDR
.long .L_OP_XOR_INT_2ADDR
.long .L_OP_SHL_INT_2ADDR
.long .L_OP_SHR_INT_2ADDR
.long .L_OP_USHR_INT_2ADDR
.long .L_OP_ADD_LONG_2ADDR
.long .L_OP_SUB_LONG_2ADDR
.long .L_OP_MUL_LONG_2ADDR
.long .L_OP_DIV_LONG_2ADDR
.long .L_OP_REM_LONG_2ADDR
.long .L_OP_AND_LONG_2ADDR
.long .L_OP_OR_LONG_2ADDR
.long .L_OP_XOR_LONG_2ADDR
.long .L_OP_SHL_LONG_2ADDR
.long .L_OP_SHR_LONG_2ADDR
.long .L_OP_USHR_LONG_2ADDR
.long .L_OP_ADD_FLOAT_2ADDR
.long .L_OP_SUB_FLOAT_2ADDR
.long .L_OP_MUL_FLOAT_2ADDR
.long .L_OP_DIV_FLOAT_2ADDR
.long .L_OP_REM_FLOAT_2ADDR
.long .L_OP_ADD_DOUBLE_2ADDR
.long .L_OP_SUB_DOUBLE_2ADDR
.long .L_OP_MUL_DOUBLE_2ADDR
.long .L_OP_DIV_DOUBLE_2ADDR
.long .L_OP_REM_DOUBLE_2ADDR
.long .L_OP_ADD_INT_LIT16
.long .L_OP_RSUB_INT
.long .L_OP_MUL_INT_LIT16
.long .L_OP_DIV_INT_LIT16
.long .L_OP_REM_INT_LIT16
.long .L_OP_AND_INT_LIT16
.long .L_OP_OR_INT_LIT16
.long .L_OP_XOR_INT_LIT16
.long .L_OP_ADD_INT_LIT8
.long .L_OP_RSUB_INT_LIT8
.long .L_OP_MUL_INT_LIT8
.long .L_OP_DIV_INT_LIT8
.long .L_OP_REM_INT_LIT8
.long .L_OP_AND_INT_LIT8
.long .L_OP_OR_INT_LIT8
.long .L_OP_XOR_INT_LIT8
.long .L_OP_SHL_INT_LIT8
.long .L_OP_SHR_INT_LIT8
.long .L_OP_USHR_INT_LIT8
.long .L_OP_IGET_VOLATILE
.long .L_OP_IPUT_VOLATILE
.long .L_OP_SGET_VOLATILE
.long .L_OP_SPUT_VOLATILE
.long .L_OP_IGET_OBJECT_VOLATILE
.long .L_OP_IGET_WIDE_VOLATILE
.long .L_OP_IPUT_WIDE_VOLATILE
.long .L_OP_SGET_WIDE_VOLATILE
.long .L_OP_SPUT_WIDE_VOLATILE
.long .L_OP_BREAKPOINT
.long .L_OP_THROW_VERIFICATION_ERROR
.long .L_OP_EXECUTE_INLINE
.long .L_OP_EXECUTE_INLINE_RANGE
.long .L_OP_INVOKE_DIRECT_EMPTY
.long .L_OP_UNUSED_F1
.long .L_OP_IGET_QUICK
.long .L_OP_IGET_WIDE_QUICK
.long .L_OP_IGET_OBJECT_QUICK
.long .L_OP_IPUT_QUICK
.long .L_OP_IPUT_WIDE_QUICK
.long .L_OP_IPUT_OBJECT_QUICK
.long .L_OP_INVOKE_VIRTUAL_QUICK
.long .L_OP_INVOKE_VIRTUAL_QUICK_RANGE
.long .L_OP_INVOKE_SUPER_QUICK
.long .L_OP_INVOKE_SUPER_QUICK_RANGE
.long .L_OP_IPUT_OBJECT_VOLATILE
.long .L_OP_SGET_OBJECT_VOLATILE
.long .L_OP_SPUT_OBJECT_VOLATILE
.long .L_OP_UNUSED_FF
