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
    * File: header.S
    */

   /*
    * IA32 calling convention and general notes:
    *
    * EAX, ECX, EDX - general purpose scratch registers (caller-saved);
    *
    * The stack (%esp) - used to pass arguments to functions
    *
    * EAX - holds the first 4 bytes of a return
    * EDX - holds the second 4 bytes of a return
    *
    * EBX, ESI, EDI, EBP - are callee saved
    *
    * CS, DS, SS - are segment registers
    * ES, FS, GS - are segment registers. We will try to avoid using these registers
    *
    * The stack is "full descending". Only the arguments that do not fit    * in the first two arg registers are placed on the stack.
    * "%esp" points to the first stacked argument (i.e. the 3rd arg).
    */

   /*
    * Mterp and IA32 notes
    *
    * mem          nick      purpose
    * (%ebp)       rGLUE     InterpState base pointer (A.K.A. MterpGlue Pointer)
    * %esi         rPC       interpreted program counter, used for fetching
    *                        instructions
    * %ebx         rINST     first 16-bit code unit of current instruction
    * %edi         rFP       interpreted frame pointer, used for accessing
    *                        locals and args
    */

   /*
    * Includes
    */

#include "../common/asm-constants.h"

   /*
    * Reserved registers
    */

#define rGLUE  (%ebp)
#define rINST   %ebx
#define rINSTbl  %bl
#define rINSTbh  %bh
#define rINSTw  %bx
#define rPC     %esi
#define rFP     %edi

   /*
    * Temporary register used when finishing an opcode
    */

#define rFinish %edx

   /*
    * Stack locations used for temporary data. For convenience.
    */

#define sReg0    4(%ebp)
#define sReg1    8(%ebp)
#define sReg2   12(%ebp)
#define sReg3   16(%ebp)

   /*
    * Save the PC and FP to the glue struct
    */

    .macro      SAVE_PC_FP_TO_GLUE _reg
    movl        rGLUE, \_reg
    movl        rPC, offGlue_pc(\_reg)
    movl        rFP, offGlue_fp(\_reg)
    .endm

   /*
    * Restore the PC and FP from the glue struct
    */

    .macro      LOAD_PC_FP_FROM_GLUE
    movl        rGLUE, rFP
    movl        offGlue_pc(rFP), rPC
    movl        offGlue_fp(rFP), rFP
    .endm

   /*
    * "Export" the PC to the stack frame, f/b/o future exception objects. This must
    * be done *before* something calls dvmThrowException.
    *
    * In C this is "SAVEAREA_FROM_FP(fp)->xtra.currentPc = pc", i.e.
    * fp - sizeof(StackSaveArea) + offsetof(SaveArea, xtra.currentPc)
    *
    * It's okay to do this more than once.
    */

    .macro      EXPORT_PC
    movl        rPC, (-sizeofStackSaveArea + offStackSaveArea_currentPc)(rFP)
    .endm

   /*
    * Given a frame pointer, find the stack save area.
    * In C this is "((StackSaveArea*)(_fp) -1)".
    */

    .macro      SAVEAREA_FROM_FP  _reg
    lea         -sizeofStackSaveArea(rFP), \_reg
    .endm

   /*
    * Get the 32-bit value from a dalvik register.
    */

    .macro      GET_VREG _vreg
    movl        (rFP,\_vreg, 4), \_vreg
    .endm

   /*
    * Set the 32-bit value from a dalvik register.
    */

    .macro      SET_VREG _reg _vreg
    movl        \_reg, (rFP,\_vreg, 4)
    .endm

   /*
    * Fetch the next instruction from rPC into rINST. Does not advance rPC.
    */

    .macro      FETCH_INST
    movzwl      (rPC), rINST
    .endm

   /*
    * Fetch the next instruction from the specified offset. Advances rPC
    * to point to the next instruction. "_count" is in 16-bit code units.
    *
    * This must come AFTER anything that can throw an exception, or the
    * exception catch may miss. (This also implies that it must come after
    * EXPORT_PC())
    */

    .macro      FETCH_ADVANCE_INST _count
    add         $$(\_count*2), rPC
    movzwl      (rPC), rINST
    .endm

   /*
    * Fetch the next instruction from an offset specified by _reg. Updates
    * rPC to point to the next instruction. "_reg" must specify the distance
    * in bytes, *not* 16-bit code units, and may be a signed value.
    */

    .macro      FETCH_ADVANCE_INST_RB _reg
    addl        \_reg, rPC
    movzwl      (rPC), rINST
    .endm

   /*
    * Fetch a half-word code unit from an offset past the current PC. The
    * "_count" value is in 16-bit code units. Does not advance rPC.
    * For example, given instruction of format: AA|op BBBB, it
    * fetches BBBB.
    */

    .macro      FETCH _count _reg
    movzwl      (\_count*2)(rPC), \_reg
    .endm

   /*
    * Fetch a half-word code unit from an offset past the current PC. The
    * "_count" value is in 16-bit code units. Does not advance rPC.
    * This variant treats the value as signed.
    */

    .macro      FETCHs _count _reg
    movswl      (\_count*2)(rPC), \_reg
    .endm

   /*
    * Fetch the first byte from an offset past the current PC. The
    * "_count" value is in 16-bit code units. Does not advance rPC.
    * For example, given instruction of format: AA|op CC|BB, it
    * fetches BB.
    */

    .macro      FETCH_BB _count _reg
    movzbl      (\_count*2)(rPC), \_reg
    .endm

    /*
    * Fetch the second byte from an offset past the current PC. The
    * "_count" value is in 16-bit code units. Does not advance rPC.
    * For example, given instruction of format: AA|op CC|BB, it
    * fetches CC.
    */

    .macro      FETCH_CC _count _reg
    movzbl      (\_count*2 + 1)(rPC), \_reg
    .endm

   /*
    * Fetch the second byte from an offset past the current PC. The
    * "_count" value is in 16-bit code units. Does not advance rPC.
    * This variant treats the value as signed.
    */

    .macro      FETCH_CCs _count _reg
    movsbl      (\_count*2 + 1)(rPC), \_reg
    .endm


   /*
    * Fetch one byte from an offset past the current PC.  Pass in the same
    * "_count" as you would for FETCH, and an additional 0/1 indicating which
    * byte of the halfword you want (lo/hi).
    */

    .macro      FETCH_B _reg  _count  _byte
    movzbl      (\_count*2+\_byte)(rPC), \_reg
    .endm

   /*
    * Put the instruction's opcode field into the specified register.
    */

    .macro      GET_INST_OPCODE _reg
    movzbl      rINSTbl, \_reg
    .endm

   /*
    * Begin executing the opcode in _reg.
    */

    .macro      GOTO_OPCODE _reg
    shl         $$${handler_size_bits}, \_reg
    addl        $$dvmAsmInstructionStart,\_reg
    jmp         *\_reg
    .endm



   /*
    * Macros pair attempts to speed up FETCH_INST, GET_INST_OPCODE and GOTO_OPCODE
    * by using a jump table. _rFinish should must be the same register for
    * both macros.
    */

    .macro      FFETCH _rFinish
    movzbl      (rPC), \_rFinish
    .endm

    .macro      FGETOP_JMPa _rFinish
    movzbl      1(rPC), rINST
    jmp         *dvmAsmInstructionJmpTable(,\_rFinish, 4)
    .endm

   /*
    * Macro pair attempts to speed up FETCH_INST, GET_INST_OPCODE and GOTO_OPCODE
    * by using a jump table. _rFinish and _count should must be the same register for
    * both macros.
    */

    .macro      FFETCH_ADV _count _rFinish
    movzbl      (\_count*2)(rPC), \_rFinish
    .endm

    .macro      FGETOP_JMP _count _rFinish
    movzbl      (\_count*2 + 1)(rPC), rINST
    addl        $$(\_count*2), rPC
    jmp         *dvmAsmInstructionJmpTable(,\_rFinish, 4)
    .endm

   /*
    * Macro pair attempts to speed up FETCH_INST, GET_INST_OPCODE and GOTO_OPCODE
    * by using a jump table. _rFinish and _reg should must be the same register for
    * both macros.
    */

    .macro      FFETCH_ADV_RB _reg _rFinish
    movzbl      (\_reg, rPC), \_rFinish
    .endm

    .macro      FGETOP_RB_JMP _reg _rFinish
    movzbl      1(\_reg, rPC), rINST
    addl        \_reg, rPC
    jmp         *dvmAsmInstructionJmpTable(,\_rFinish, 4)
    .endm

   /*
    * Attempts to speed up FETCH_INST, GET_INST_OPCODE using
    * a jump table. This macro should be called before FINISH_JMP where
    * rFinish should be the same register containing the opcode value.
    * This is an attempt to split up FINISH in order to reduce or remove
    * potential stalls due to the wait for rFINISH.
    */

    .macro      FINISH_FETCH _rFinish
    movzbl      (rPC), \_rFinish
    movzbl      1(rPC), rINST
    .endm


   /*
    * Attempts to speed up FETCH_ADVANCE_INST, GET_INST_OPCODE using
    * a jump table. This macro should be called before FINISH_JMP where
    * rFinish should be the same register containing the opcode value.
    * This is an attempt to split up FINISH in order to reduce or remove
    * potential stalls due to the wait for rFINISH.
    */

    .macro      FINISH_FETCH_ADVANCE _count _rFinish
    movzbl      (\_count*2)(rPC), \_rFinish
    movzbl      (\_count*2 + 1)(rPC), rINST
    addl        $$(\_count*2), rPC
    .endm

   /*
    * Attempts to speed up FETCH_ADVANCE_INST_RB, GET_INST_OPCODE using
    * a jump table. This macro should be called before FINISH_JMP where
    * rFinish should be the same register containing the opcode value.
    * This is an attempt to split up FINISH in order to reduce or remove
    * potential stalls due to the wait for rFINISH.
    */

    .macro      FINISH_FETCH_ADVANCE_RB _reg _rFinish
    movzbl      (\_reg, rPC), \_rFinish
    movzbl      1(\_reg, rPC), rINST
    addl        \_reg, rPC
    .endm

   /*
    * Attempts to speed up GOTO_OPCODE using a jump table. This macro should
    * be called after a FINISH_FETCH* instruction where rFinish should be the
    * same register containing the opcode value. This is an attempt to split up
    * FINISH in order to reduce or remove potential stalls due to the wait for rFINISH.
    */

    .macro      FINISH_JMP _rFinish
    jmp         *dvmAsmInstructionJmpTable(,\_rFinish, 4)
    .endm

   /*
    * Attempts to speed up FETCH_INST, GET_INST_OPCODE, GOTO_OPCODE by using
    * a jump table. Uses a single macro - but it should be faster if we
    * split up the fetch for rFinish and the jump using rFinish.
    */

    .macro      FINISH_A
    movzbl      (rPC), rFinish
    movzbl      1(rPC), rINST
    jmp         *dvmAsmInstructionJmpTable(,rFinish, 4)
    .endm

   /*
    * Attempts to speed up FETCH_ADVANCE_INST, GET_INST_OPCODE,
    * GOTO_OPCODE by using a jump table. Uses a single macro -
    * but it should be faster if we split up the fetch for rFinish
    * and the jump using rFinish.
    */

    .macro      FINISH _count
    movzbl      (\_count*2)(rPC), rFinish
    movzbl      (\_count*2 + 1)(rPC), rINST
    addl        $$(\_count*2), rPC
    jmp         *dvmAsmInstructionJmpTable(,rFinish, 4)
    .endm

   /*
    * Attempts to speed up FETCH_ADVANCE_INST_RB, GET_INST_OPCODE,
    * GOTO_OPCODE by using a jump table. Uses a single macro -
    * but it should be faster if we split up the fetch for rFinish
    * and the jump using rFinish.
    */

    .macro      FINISH_RB _reg _rFinish
    movzbl      (\_reg, rPC), \_rFinish
    movzbl      1(\_reg, rPC), rINST
    addl        \_reg, rPC
    jmp         *dvmAsmInstructionJmpTable(,\_rFinish, 4)
    .endm

   /*
    * Hard coded helper values.
    */

.balign 16

.LdoubNeg:
    .quad       0x8000000000000000

.L64bits:
    .quad       0xFFFFFFFFFFFFFFFF

.LshiftMask2:
    .quad       0x0000000000000000
.LshiftMask:
    .quad       0x000000000000003F

.Lvalue64:
    .quad       0x0000000000000040

.LvaluePosInfLong:
    .quad       0x7FFFFFFFFFFFFFFF

.LvalueNegInfLong:
    .quad       0x8000000000000000

.LvalueNanLong:
    .quad       0x0000000000000000

.LintMin:
.long   0x80000000

.LintMax:
.long   0x7FFFFFFF
