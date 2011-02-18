/*
 * This file was generated automatically by gen-mterp.py for 'x86-atom'.
 *
 * --> DO NOT EDIT <--
 */

/* File: x86-atom/header.S */
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
    add         $(\_count*2), rPC
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
    shl         $6, \_reg
    addl        $dvmAsmInstructionStart,\_reg
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
    addl        $(\_count*2), rPC
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
    addl        $(\_count*2), rPC
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
    addl        $(\_count*2), rPC
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


    .global dvmAsmInstructionStart
    .type   dvmAsmInstructionStart, %function
dvmAsmInstructionStart = .L_OP_NOP
    .text

/* ------------------------------ */
    .balign 64
.L_OP_NOP: /* 0x00 */
/* File: x86-atom/OP_NOP.S */
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
    * File: OP_NOP.S
    *
    * Code: Use a cycle. Uses no substitutions.
    *
    * For: nop
    *
    * Description: No operation. Use a cycle
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    FINISH      1                       # jump to next instruction

#ifdef ASSIST_DEBUGGER

   /*
    * insert fake function header to help gdb find the stack frame
    */

    .type       dalvik_inst, %function
dalvik_inst:
    MTERP_ENTRY
#endif

/* ------------------------------ */
    .balign 64
.L_OP_MOVE: /* 0x01 */
/* File: x86-atom/OP_MOVE.S */
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
    * File: OP_MOVE.S
    *
    * Code: Copies contents from one register to another. Uses no
    *       substitutions.
    *
    * For: move, move-object, long-to-int
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vB
    SET_VREG    rINST, %ecx             # vA<- vB; %edx
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_FROM16: /* 0x02 */
/* File: x86-atom/OP_MOVE_FROM16.S */
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
    * File: OP_MOVE_FROM16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/from16, move-object/from16
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: AA|op BBBB (22x)
    *
    * Syntax: op vAA, vBBBB
    */

    FETCH       1, %edx                 # %edx<- BBBB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, rINST             # vA<- vB; %edx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_16: /* 0x03 */
/* File: x86-atom/OP_MOVE_16.S */
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
    * File: OP_MOVE_16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/16, move-object/16
    *
    * Description: Copies contents from one non-object register to another.
    *              fp[A]<- fp[B]
    *
    * Format: ØØ|op AAAA BBBB (32x)
    *
    * Syntax: op vAAAA, vBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBB
    FETCH       1, %ecx                 # %ecx<- AAAA
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, %ecx              # vA<- vB; %edx
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE: /* 0x04 */
/* File: x86-atom/OP_MOVE_WIDE.S */
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
    * File: OP_MOVE_WIDE.S
    *
    * Code: Copies contents from one register to another. Uses no
    *       substitutions.
    *
    * For: move-wide
    *
    * Description: Copies contents from one non-object register to another.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA+
    shr         $4, %edx               # %edx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vB
    movq        %xmm0, (rFP, rINST, 4)  # vA<- vB
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_FROM16: /* 0x05 */
/* File: x86-atom/OP_MOVE_WIDE_FROM16.S */
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
    * File: OP_MOVE_WIDE_FROM16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move-wide/from16
    *
    * Description: Copies contents from one non-object register to another.
    *
    * Format: AA|op BBBB (22x)
    *
    * Syntax: op vAA, vBBBB
    */

    FETCH       1, %edx                 # %edx<- BBBB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vB
    movq        %xmm0, (rFP, rINST, 4)  # vA<- vB
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_16: /* 0x06 */
/* File: x86-atom/OP_MOVE_WIDE_16.S */
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
    * File: OP_MOVE_WIDE_16.S
    *
    * Code: Copies contents from one register to another. Uses no
    *       substitutions.
    *
    * For: move-wide/16
    *
    * Description: Copies contents from one non-object register to another.
    *
    * Format: ØØ|op AAAA BBBB (32x)
    *
    * Syntax: op vAAAA, vBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBB
    FETCH       1, %ecx                 # %ecx<- AAAA
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vB
    movq        %xmm0, (rFP, %ecx, 4)   # vA<- vB; %xmm0
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT: /* 0x07 */
/* File: x86-atom/OP_MOVE_OBJECT.S */
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
    * File: OP_MOVE_OBJECT.S
    */

/* File: x86-atom/OP_MOVE.S */
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
    * File: OP_MOVE.S
    *
    * Code: Copies contents from one register to another. Uses no
    *       substitutions.
    *
    * For: move, move-object, long-to-int
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vB
    SET_VREG    rINST, %ecx             # vA<- vB; %edx
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_FROM16: /* 0x08 */
/* File: x86-atom/OP_MOVE_OBJECT_FROM16.S */
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
    * File: OP_MOVE_OBJECT_FROM16.S
    */

/* File: x86-atom/OP_MOVE_FROM16.S */
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
    * File: OP_MOVE_FROM16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/from16, move-object/from16
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: AA|op BBBB (22x)
    *
    * Syntax: op vAA, vBBBB
    */

    FETCH       1, %edx                 # %edx<- BBBB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, rINST             # vA<- vB; %edx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_16: /* 0x09 */
/* File: x86-atom/OP_MOVE_OBJECT_16.S */
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
    * File: OP_MOVE_OBJECT_16.S
    */

/* File: x86-atom/OP_MOVE_16.S */
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
    * File: OP_MOVE_16.S
    *
    * Code: Copies contents from one register to another
    *
    * For: move/16, move-object/16
    *
    * Description: Copies contents from one non-object register to another.
    *              fp[A]<- fp[B]
    *
    * Format: ØØ|op AAAA BBBB (32x)
    *
    * Syntax: op vAAAA, vBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBB
    FETCH       1, %ecx                 # %ecx<- AAAA
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    SET_VREG    %edx, %ecx              # vA<- vB; %edx
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT: /* 0x0a */
/* File: x86-atom/OP_MOVE_RESULT.S */
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
    * File: OP_MOVE_RESULT.S
    *
    * Code: Copies a return value to a register
    *
    * For: move-result, move-result-object
    *
    * Description: Move the single-word non-object result of the most
    *              recent method invocation into the indicated register. This
    *              must be done as the instruction immediately after a
    *              method invocation whose (single-word, non-object) result
    *              is not to be ignored; anywhere else is invalid.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    FFETCH_ADV  1, %ecx                 # %ecx<- next instruction hi; fetch, advance
    movl        offGlue_retval(%eax), %edx # %edx<- glue->retval
    SET_VREG    %edx, rINST             # vA<- glue->retval
    FGETOP_JMP  1, %ecx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_WIDE: /* 0x0b */
/* File: x86-atom/OP_MOVE_RESULT_WIDE.S */
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
    * File: OP_MOVE_RESULT_WIDE.S
    *
    * Code: Copies a return value to a register
    *
    * For: move-result-wide
    *
    * Description: Move the double-word non-object result of the most
    *              recent method invocation into the indicated register. This
    *              must be done as the instruction immediately after a
    *              method invocation whose (single-word, non-object) result
    *              is not to be ignored; anywhere else is invalid.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movq        offGlue_retval(%eax), %xmm0 # %xmm0<- glue->retval
    movq        %xmm0, (rFP, rINST, 4)  # vA<- glue->retval
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_OBJECT: /* 0x0c */
/* File: x86-atom/OP_MOVE_RESULT_OBJECT.S */
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
    * File: OP_MOVE_RESULT_OBJECT.S
    */

/* File: x86-atom/OP_MOVE_RESULT.S */
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
    * File: OP_MOVE_RESULT.S
    *
    * Code: Copies a return value to a register
    *
    * For: move-result, move-result-object
    *
    * Description: Move the single-word non-object result of the most
    *              recent method invocation into the indicated register. This
    *              must be done as the instruction immediately after a
    *              method invocation whose (single-word, non-object) result
    *              is not to be ignored; anywhere else is invalid.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    FFETCH_ADV  1, %ecx                 # %ecx<- next instruction hi; fetch, advance
    movl        offGlue_retval(%eax), %edx # %edx<- glue->retval
    SET_VREG    %edx, rINST             # vA<- glue->retval
    FGETOP_JMP  1, %ecx                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_EXCEPTION: /* 0x0d */
/* File: x86-atom/OP_MOVE_EXCEPTION.S */
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
    * File: OP_MOVE_EXCEPTION.S
    *
    * Code: Moves an exception to a register
    *
    * For: move-exception
    *
    * Description: Save a just-caught exception into the given register. This
    *              instruction is only valid as the first instruction of an
    *              exception handler.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %ecx # %ecx<- glue->self
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        offThread_exception(%ecx), %edx # %edx<- glue->self->exception
    movl        $0, offThread_exception(%ecx) # clear exception
    SET_VREG    %edx, rINST             # vAA<- glue->self->exception
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_RETURN_VOID: /* 0x0e */
/* File: x86-atom/OP_RETURN_VOID.S */
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
    * File: OP_RETURN_VOID.S
    */

    jmp         common_returnFromMethod

/* ------------------------------ */
    .balign 64
.L_OP_RETURN: /* 0x0f */
/* File: x86-atom/OP_RETURN.S */
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
    * File: OP_RETURN.S
    */

/* File: x86-atom/OP_RETURN_COMMON.S */
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
    * File: OP_RETURN_COMMON.S
    *
    * Code: Return a 32-bit value. Uses no substitutions.
    *
    * For: return, return-object
    *
    * Description: Copies the return value into the "glue"
    *              structure, then jumps to the return handler.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offGlue_retval(%edx) # glue->retval<- vAA
    jmp         common_returnFromMethod # jump to common return code


/* ------------------------------ */
    .balign 64
.L_OP_RETURN_WIDE: /* 0x10 */
/* File: x86-atom/OP_RETURN_WIDE.S */
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
    * File: OP_RETURN_WIDE.S
    *
    * Code: Return a 64-bit value. Uses no substitutions.
    *
    * For: return-wide
    *
    * Description: Copies the return value into the "glue"
    *              structure, then jumps to the return handler.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vAA
    movq        %xmm0, offGlue_retval(%edx)# glue->retval<- vAA
    jmp         common_returnFromMethod # jump to common return code

/* ------------------------------ */
    .balign 64
.L_OP_RETURN_OBJECT: /* 0x11 */
/* File: x86-atom/OP_RETURN_OBJECT.S */
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
    * File: OP_RETURN_OBJECT.S
    */

/* File: x86-atom/OP_RETURN_COMMON.S */
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
    * File: OP_RETURN_COMMON.S
    *
    * Code: Return a 32-bit value. Uses no substitutions.
    *
    * For: return, return-object
    *
    * Description: Copies the return value into the "glue"
    *              structure, then jumps to the return handler.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offGlue_retval(%edx) # glue->retval<- vAA
    jmp         common_returnFromMethod # jump to common return code


/* ------------------------------ */
    .balign 64
.L_OP_CONST_4: /* 0x12 */
/* File: x86-atom/OP_CONST_4.S */
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
    * File: OP_CONST_4.S
    *
    * Code: Moves a literal to a register. Uses no substitutions.
    *
    * For: const/4
    *
    * Description: Move the given literal value (right-zero-extended to 32
    *              bits) into the specified register.
    *
    * Format: B|A|op (11n)
    *
    * Syntax: op vA, #+B
    */

    movl        rINST, %edx             # %edx<- BA
    andl        $15, rINST             # rINST<- A
    shl         $24, %edx              # %edx<- B000
    sar         $28, %edx              # %edx<- right-zero-extended B
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    SET_VREG    %edx, rINST             # vA<- %edx; literal
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_16: /* 0x13 */
/* File: x86-atom/OP_CONST_16.S */
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
    * File: OP_CONST_16.S
    *
    * Code: Moves a literal to a register. Uses no substitutions.
    *
    * For: const/16
    *
    * Description: Move the given literal value (right-zero-extended to 32
    *              bits) into the specified register
    *
    * Format: AA|op BBBB (21s)
    *
    * Syntax: op vAA, #+BBBB
    */

    FETCHs      1, %edx                 # %edx<- BBBB
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    SET_VREG    %edx rINST              # vAA<- BBBB; literal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST: /* 0x14 */
/* File: x86-atom/OP_CONST.S */
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
    * File: OP_CONST.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const
    *
    * Description: Move the given literal value into the specified register
    *
    * Format: AA|op BBBBlo BBBBhi (31i)
    *
    * Syntax: op vAA, #+BBBBBBBB
    */

    FETCH       2, %edx                 # %edx<- BBBBhi
    FETCH       1, %ecx                 # %ecx<- BBBBlo
    shl         $16, %edx              # move BBBB to high bits
    or          %edx, %ecx              # %ecx<- #+BBBBBBBB
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    SET_VREG    %ecx, rINST             # vAA<- %ecx; literal
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_HIGH16: /* 0x15 */
/* File: x86-atom/OP_CONST_HIGH16.S */
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
    * File: OP_CONST_HIGH16.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const/high16
    *
    * Description: Move the given literal value (right-zero-extended to 32
    *              bits) into the specified register
    *
    * Format: AA|op BBBB (21h)
    *
    * Syntax: op vAA, #+BBBB0000
    */

    FETCH       1, %ecx                 # %ecx<- 0000BBBB (zero-extended)
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    shl         $16, %ecx              # %ecx<- BBBB0000
    SET_VREG    %ecx, rINST             # vAA<- %ecx; BBBB0000
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_16: /* 0x16 */
/* File: x86-atom/OP_CONST_WIDE_16.S */
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
    * File: OP_CONST_WIDE_16.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide/16
    *
    * Description: Move the given literal value (sign-extended to 64 bits)
    *              into the specified register-pair
    *
    * Format: AA|op BBBB (21s)
    *
    * Syntax: op vAA, #+BBBB
    */

    FETCHs      1, %ecx                 # %ecx<- ssssBBBB (sign-extended)
    movl        %ecx, %edx              # %edx<- ssssBBBB (sign-extended)
    sar         $31, %ecx              # %ecx<- sign bit
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        %edx, (rFP, rINST, 4)   # vAA<- ssssBBBB
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1<- ssssssss
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_32: /* 0x17 */
/* File: x86-atom/OP_CONST_WIDE_32.S */
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
    * File: OP_CONST_WIDE_32.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide/32
    *
    * Description: Move the given literal value (sign-extended to 64 bits)
    *              into the specified register-pair
    *
    * Format: AA|op BBBBlo BBBBhi (31i)
    *
    * Syntax: op vAA, #+BBBBBBBB
    */

    FETCH       1,  %edx                # %edx<- BBBBlo
    FETCHs      2, %ecx                 # %ecx<- BBBBhi
    shl         $16, %ecx              # prepare to create #+BBBBBBBB
    or          %ecx, %edx              # %edx<- %edx<- #+BBBBBBBB
    sar         $31, %ecx              # %ecx<- sign bit
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        %edx, (rFP, rINST, 4)   # vAA<-  BBBBBBBB
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1<- ssssssss
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE: /* 0x18 */
/* File: x86-atom/OP_CONST_WIDE.S */
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
    * File: OP_CONST_WIDE.S
    *
    * Code: Move a literal to a register. Uses no substitutions.
    *
    * For: const-wide
    *
    * Description: Move the given literal value into the specified
    *              register pair
    *
    * Format: AA|op BBBBlolo BBBBlohi BBBBhilo BBBBhihi (51l)
    *
    * Syntax: op vAA, #+BBBBBBBBBBBBBBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBBlolo
    FETCH       2, %edx                 # %edx<- BBBBlohi
    shl         $16, %edx              # %edx<- prepare to create #+BBBBBBBBlo
    or          %edx, %ecx              # %ecx<- #+BBBBBBBBlo
    movl        %ecx, (rFP, rINST, 4)   # vAA <- #+BBBBBBBBlo
    FETCH       3, %ecx                 # %ecx<- BBBBhilo
    FETCH       4, %edx                 # %edx<- BBBBhihi
    FFETCH_ADV  5, %eax                 # %eax<- next instruction hi; fetch, advance
    shl         $16, %edx              # %edx<- prepare to create #+BBBBBBBBhi
    or          %edx, %ecx              # %ecx<- #+BBBBBBBBhi
    movl        %ecx, 4(rFP, rINST, 4)  # vAA+1 <- #+BBBBBBBBlo
    FGETOP_JMP  5, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_HIGH16: /* 0x19 */
/* File: x86-atom/OP_CONST_WIDE_HIGH16.S */
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
    * File: OP_CONST_WIDE_HIGH16.S
    *
    * Code: Move a literal value to a register. Uses no substitutions.
    *
    * For: const-wide/high16
    *
    * Description: Move the given literal value (right-zero-extended to 64
    *              bits) into the specified register
    *
    * Format: AA|op BBBB (21h)
    *
    * Syntax: op vAA, #+BBBB000000000000
    */

    FETCH       1, %ecx                 # %ecx<- 0000BBBB (zero-extended)
    shl         $16, %ecx              # rINST<- AA
    movl        $0, (rFP, rINST, 4)    # vAAlow<- 00000000
    movl        %ecx, 4(rFP, rINST, 4)  # vAAhigh<- %ecx; BBBB0000
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING: /* 0x1a */
/* File: x86-atom/OP_CONST_STRING.S */
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
    * File: OP_CONST_STRING.S
    *
    * Code: Move a string reference to a register. Uses no substitutions.
    *
    * For: const/string
    *
    * Description: Move a referece to the string specified by the given
    *              index into the specified register. vAA <- pResString[BBBB]
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- glue->methodClassDex
    movl        offDvmDex_pResStrings(%eax), %eax # %eax<- glue->methodClassDex->pResStrings
    movl        (%eax, %ecx, 4), %eax   # %eax<- pResStrings[BBBB]
    cmp         $0, %eax               # check if string is resolved
    je          .LOP_CONST_STRING_resolve     # resolve string reference
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FINISH      2                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING_JUMBO: /* 0x1b */
/* File: x86-atom/OP_CONST_STRING_JUMBO.S */
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
    * File: OP_CONST_STRING_JUMBO.S
    *
    * Code: Move a string reference to a register. Uses no substitutions.
    *
    * For: const/string-jumbo
    *
    * Description: Move a reference to the string specified by the given
    *              index into the specified register. vAA <- pResString[BBBB]
    *
    * Format: AA|op BBBBlo BBBBhi (31c)
    *
    * Syntax: op vAA, string@BBBBBBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- glue->methodClassDex
    movl        offDvmDex_pResStrings(%eax), %eax # %eax<- glue->methodClassDex->pResStrings
    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $16, %edx              # %edx<- prepare to create &BBBBBBBB
    or          %edx, %ecx              # %ecx<- &BBBBBBBB
    movl        (%eax, %ecx, 4), %eax   # %eax<- pResStrings[BBBB]
    cmp         $0, %eax               # check if string is resolved
    je          .LOP_CONST_STRING_JUMBO_resolve     # resolve string reference
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FINISH      3                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CONST_CLASS: /* 0x1c */
/* File: x86-atom/OP_CONST_CLASS.S */
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
    * File: OP_CONST_CLASS.S
    *
    * Code: Move a class reference to a register. Uses no substitutions.
    *
    * For: const/class
    *
    * Description: Move a reference to the class specified
    *              by the given index into the specified register.
    *              In the case where the indicated type is primitive,
    *              this will store a reference to the primitive type's
    *              degenerate class.
    *
    * Format: AA|op BBBBlo BBBBhi (21c)
    *
    * Syntax: op vAA, field@BBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- pDvmDex
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- pDvmDex->pResClasses
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved class
    cmp         $0, %eax               # check if classes is resolved before?
    je          .LOP_CONST_CLASS_resolve     # resolve class
    SET_VREG    %eax, rINST             # vAA<- resolved class
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_ENTER: /* 0x1d */
/* File: x86-atom/OP_MONITOR_ENTER.S */
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
    * File: OP_MONITOR_ENTER.S
    *
    * Code: Aquire a monitor
    *
    * For: monitor-enter
    *
    * Description: Aquire a monitor for the indicated object.
    *
    *
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $0, rINST              # check for null object
    movl        offGlue_self(%eax), %eax # %eax<- glue->self
#ifdef WITH_MONITOR_TRACKING
    EXPORT_PC   # export PC so we can grab stack trace
#endif
    je          common_errNullObject    # handle null object
#    jmp         .LOP_MONITOR_ENTER_finish
#%break
#.LOP_MONITOR_ENTER_finish:
    movl        rINST, -4(%esp)         # push parameter reference
    movl        %eax, -8(%esp)          # push parameter
    lea         -8(%esp), %esp
    call        dvmLockObject           # call: (struct Thread* self,
                                        #       struct Object* obj)
                                        # return: void
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    lea         8(%esp), %esp
#ifdef WITH_DEADLOCK_PREDICTION
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %eax # %eax<- glue->self
    movl        offThread_exception(%eax), %eax # %eax<- glue->self->exception
    cmp         $0, %eax               # check for exception
    jne         common_exceptionThrown  # handle exception
#endif
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_EXIT: /* 0x1e */
/* File: x86-atom/OP_MONITOR_EXIT.S */
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
    * File: OP_MONITOR_EXIT.S
    *
    * Code: Release a monitor
    *
    * For: monitor-exit
    *
    * Description: Release a monitor for the indicated object. If this instruction needs
    *              to throw an execption, it must do so as if the pc has already
    *              advanced pased the instruction.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # export the pc
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $0, rINST              # rINST<- check for null object
    je          common_errNullObject    # handle null object
    push        rINST                   # push parameter object
    push        offGlue_self(%eax)      # push parameter self
    call        dvmUnlockObject         # call: (struct Thread* self,
                                        #       struct Object* obj)
                                        # return: bool
    FINISH_FETCH_ADVANCE 1, %edx        # advance pc before exception
    cmp         $0, %eax               # check for success
    lea         8(%esp), %esp
    je          common_exceptionThrown  # handle exception
    FINISH_JMP  %edx                    # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CHECK_CAST: /* 0x1f */
/* File: x86-atom/OP_CHECK_CAST.S */
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
    * File: OP_CHECK_CAST.S
    *
    * Code: Checks to see if a cast is allowed. Uses no substitutions.
    *
    * For: check-cast
    *
    * Description: Throw if the reference in the given register cannot be
    *              cast to the indicated type. The type must be a reference
    *              type (not a primitive type).
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, type@BBBB
    */

    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %eax # %eax<- pDvmDex
    GET_VREG    rINST                   # rINST<- vAA
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- pDvmDex->pResClasses
    cmp         $0, rINST              # check for null reference object
    je          .LOP_CHECK_CAST_okay        # can always cast null object
    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        (%eax, %ecx, 4), %ecx   # %ecx<- resolved class
    cmp         $0, %ecx               # check if classes is resolved before?
    je          .LOP_CHECK_CAST_resolve     # resolve class
    jmp         .LOP_CHECK_CAST_resolved    # continue

/* ------------------------------ */
    .balign 64
.L_OP_INSTANCE_OF: /* 0x20 */
/* File: x86-atom/OP_INSTANCE_OF.S */
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
    * File: OP_INSTANCE_OF.S
    *
    * Code: Checks if object is instance of a class. Uses no substitutions.
    *
    * For: instance-of
    *
    * Description: Store in the given destination register 1 if the indicated
    *              reference is an instance of the given type, or 0 if not.
    *              The type must be a reference type (not a primitive type).
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    GET_VREG    %edx                    # %edx<- vB
    cmp         $0, %edx               # check for null object
    je          .LOP_INSTANCE_OF_store       # null object
    jmp         .LOP_INSTANCE_OF_break

/* ------------------------------ */
    .balign 64
.L_OP_ARRAY_LENGTH: /* 0x21 */
/* File: x86-atom/OP_ARRAY_LENGTH.S */
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
    * File: OP_ARRAY_LENGTH.S
    *
    * Code: 32-bit array length operation.
    *
    * For: array-length
    *
    * Description: Store the length of the indicated array in the given
    *              destination register. vB <- offArrayObject_length(vA)
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB
    cmp         $0, %eax               # check for null array object
    je          common_errNullObject    # handle null array object
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    movl        offArrayObject_length(%eax), %eax # %eax<- array length
    movl        %eax, (rFP, rINST, 4)   # vA<- %eax; array length
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_NEW_INSTANCE: /* 0x22 */
/* File: x86-atom/OP_NEW_INSTANCE.S */
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
    * File: OP_NEW_INSTANCE.S
    *
    * Code: Create a new instance of a given type. Uses no substitutions.
    *
    * For: new-instance
    *
    * Description: Construct a new instance of the indicated type,
    *              storing a reference to it in the destination.
    *              The type must refer to a non-array class.
    *
    *
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, type@BBBB
    *         op vAA, field@BBBB
    *         op vAA, string@BBBB
    */

    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %edx                 # %edx<- BBBB
    movl        offDvmDex_pResClasses(%ecx), %ecx # %ecx<- glue->pDvmDex->pResClasses
    movl        (%ecx, %edx, 4), %edx   # %edx<- vB
    EXPORT_PC                           # required for resolve
    cmp         $0, %edx               # check for null
    je          .LOP_NEW_INSTANCE_resolve     # need to resolve

   /*
    *  %edx holds class object
    */

.LOP_NEW_INSTANCE_resolved:
    movzbl      offClassObject_status(%edx), %eax # %eax<- class status
    cmp         $CLASS_INITIALIZED, %eax # check if class is initialized
    jne         .LOP_NEW_INSTANCE_needinit    # initialize class

   /*
    *  %edx holds class object
    */

.LOP_NEW_INSTANCE_initialized:
    testl       $(ACC_INTERFACE|ACC_ABSTRACT), offClassObject_accessFlags(%edx)
    mov         $ALLOC_DONT_TRACK, %eax # %eax<- flag for alloc call
    je          .LOP_NEW_INSTANCE_finish      # continue
    jmp         .LOP_NEW_INSTANCE_abstract    # handle abstract or interface

   /*
    *  %edx holds class object
    *  %eax holds flags for alloc call
    */


/* ------------------------------ */
    .balign 64
.L_OP_NEW_ARRAY: /* 0x23 */
/* File: x86-atom/OP_NEW_ARRAY.S */
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
    * File: OP_NEW_ARRAY.S
    *
    * Code: Create a new array. Uses no substitutions.
    *
    * For: new-array
    *
    * Description: Construct a new array of the indicated type and size.
    *              The type must be an array type.
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- glue->pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    GET_VREG    %edx                    # %edx<- vB
    movl        offDvmDex_pResClasses(%eax), %eax # %eax<- glue->pDvmDex->pResClasses
    cmp         $0, %edx               # check for negative length
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved class
    js          common_errNegativeArraySize # handle negative array length
    cmp         $0, %eax               # check for null
    EXPORT_PC                           # required for resolve
    jne         .LOP_NEW_ARRAY_finish      # already resovled so continue
    jmp         .LOP_NEW_ARRAY_resolve     # need to resolve

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY: /* 0x24 */
/* File: x86-atom/OP_FILLED_NEW_ARRAY.S */
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
    * File: OP_FILLED_NEW_ARRAY.S
    *
    * Code: Constructs and fills an array with the given data. Provides
    *
    * For: float-to-int
    *
    * Description: Construct an array of the given type and size,
    *              filling it with the supplied contents. The type
    *              must be an array type. The array's contents
    *              must be single-word. The constructed instance
    *              is stored as a result in the same way that the
    *              method invocation instructions store their results,
    *              so the constructed instance must be moved to a
    *              register with a subsequent move-result-object
    *              instruction.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc) (range)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, vtaboff@CCCC
    *         [B=4] op {vD, vE, vF, vG}, vtaboff@CCCC
    *         [B=3] op {vD, vE, vF}, vtaboff@CCCC
    *         [B=2] op {vD, vE}, vtaboff@CCCC
    *         [B=1] op {vD}, vtaboff@CCCC
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB
    *         op {vCCCC .. vNNNN}, type@BBBB
    */


    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- glue->methodClassDex
    movl        offDvmDex_pResClasses(%edx), %edx # %edx<- glue->methodClassDex->pResClasses
    FETCH       1, %ecx                 # %ecx<- BBBB
    EXPORT_PC
    movl (%edx, %ecx, 4), %eax # %eax<- possibly resolved class
    cmp         $0, %eax               # %eax<- check if already resolved
    jne         .LOP_FILLED_NEW_ARRAY_continue
    jmp         .LOP_FILLED_NEW_ARRAY_break

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY_RANGE: /* 0x25 */
/* File: x86-atom/OP_FILLED_NEW_ARRAY_RANGE.S */
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
    * File: OP_FILLED_NEW_ARRAY_RANGE.S
    */

/* File: x86-atom/OP_FILLED_NEW_ARRAY.S */
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
    * File: OP_FILLED_NEW_ARRAY.S
    *
    * Code: Constructs and fills an array with the given data. Provides
    *
    * For: float-to-int
    *
    * Description: Construct an array of the given type and size,
    *              filling it with the supplied contents. The type
    *              must be an array type. The array's contents
    *              must be single-word. The constructed instance
    *              is stored as a result in the same way that the
    *              method invocation instructions store their results,
    *              so the constructed instance must be moved to a
    *              register with a subsequent move-result-object
    *              instruction.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc) (range)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, vtaboff@CCCC
    *         [B=4] op {vD, vE, vF, vG}, vtaboff@CCCC
    *         [B=3] op {vD, vE, vF}, vtaboff@CCCC
    *         [B=2] op {vD, vE}, vtaboff@CCCC
    *         [B=1] op {vD}, vtaboff@CCCC
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB
    *         op {vCCCC .. vNNNN}, type@BBBB
    */


    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- glue->methodClassDex
    movl        offDvmDex_pResClasses(%edx), %edx # %edx<- glue->methodClassDex->pResClasses
    FETCH       1, %ecx                 # %ecx<- BBBB
    EXPORT_PC
    movl (%edx, %ecx, 4), %eax # %eax<- possibly resolved class
    cmp         $0, %eax               # %eax<- check if already resolved
    jne         .LOP_FILLED_NEW_ARRAY_RANGE_continue
    jmp         .LOP_FILLED_NEW_ARRAY_RANGE_break


/* ------------------------------ */
    .balign 64
.L_OP_FILL_ARRAY_DATA: /* 0x26 */
/* File: x86-atom/OP_FILL_ARRAY_DATA.S */
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
    * File: OP_FILL_ARRAY_DATA.S
    *
    * Code: Fills an array with given data. Uses no substitutions.
    *
    * For: fill-array-data
    *
    * Description: Fill the given array with the idicated data. The reference
    *              must be an array of primitives, and the data table must
    *              match it in type and size
    *
    * Format: AA|op BBBBlo BBBBhi (31t)
    *
    * Syntax: op vAA, +BBBBBBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $16, %edx              # prepare to create +BBBBBBBB
    or          %ecx, %edx              # %edx<- +BBBBBBBB
    lea         (rPC, %edx, 2), %edx    # %edx<- PC + +BBBBBBBB; array data location
    EXPORT_PC
    push        %edx
    push        (rFP, rINST, 4)
    call        dvmInterpHandleFillArrayData # call: (ArrayObject* arrayObject, const u2* arrayData)
                                             # return: bool
    FFETCH_ADV  3, %edx                 # %edx<- next instruction hi; fetch, advance
    cmp         $0, %eax
    lea         8(%esp), %esp
    je          common_exceptionThrown
    FGETOP_JMP  3, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_THROW: /* 0x27 */
/* File: x86-atom/OP_THROW.S */
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
    * File: OP_THROW.S
    *
    * Code: Throw an exception
    *
    * For: throw
    *
    * Description: Throw an exception object in the current thread.
    *
    * Format: AA|op (11x)
    *
    * Syntax: op vAA
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # export the pc
    GET_VREG    rINST                   # rINST<- vAA
    cmp         $0, rINST              # check for null
    movl        offGlue_self(%eax), %ecx # %ecx<- glue->self
    je          common_errNullObject    # handle null object
    movl        rINST, offThread_exception(%ecx) # thread->exception<- object
    jmp         common_exceptionThrown  # handle exception

/* ------------------------------ */
    .balign 64
.L_OP_GOTO: /* 0x28 */
/* File: x86-atom/OP_GOTO.S */
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
    shl         $1, %edx               # %edx is shifted for byte offset
    js          common_periodicChecks_backwardBranch  # do check on backwards branch
    FINISH_RB   %edx, %ecx              # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_GOTO_16: /* 0x29 */
/* File: x86-atom/OP_GOTO_16.S */
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
    * File: OP_GOTO_16.S
    *
    * Code: Do an unconditional branch. Uses no substitutions.
    *
    * For: goto/16
    *
    * Description: Performs an unconditionally jump to the indicated instruction.
    *              The branch uses a 16-bit offset that cannot be zero.
    *
    * Format: ØØ|op AAAA (20t)
    *
    * Syntax: op +AAAA
    */

    FETCHs      1, %edx                 # %edx<- ssssAAAA (sign-extended)
    shl         $1, %edx               # %edx is doubled to get the byte offset
    js          common_periodicChecks_backwardBranch  # do check on backwards branch
    FINISH_RB   %edx, %ecx              # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_GOTO_32: /* 0x2a */
/* File: x86-atom/OP_GOTO_32.S */
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
    * File: OP_GOTO_32.S
    *
    * Code: Do an unconditional branch. Uses no substitutions.
    *
    * For: goto/32
    *
    * Description:  Performs an unconditionally jump to the indicated instruction.
    *               The branch uses a 32-bit offset that can be zero.
    *
    * Format: ØØ|op AAAAlo AAAAhi (30t)
    *
    * Syntax: op +AAAAAAAA
    */

    FETCH       1, %edx                 # %edx<- AAAAlo
    FETCH       2, %ecx                 # %ecx<- AAAAhi
    shl         $16, %ecx              # prepare to create +AAAAAAAA
    or          %ecx, %edx              # %edx<- +AAAAAAAA
    shl         $1, %edx               # %edx is doubled to get the byte offset
    jle          common_periodicChecks_backwardBranch  # do check on backwards branch
    FINISH_RB   %edx, %ecx              # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_PACKED_SWITCH: /* 0x2b */
/* File: x86-atom/OP_PACKED_SWITCH.S */
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
    * File: OP_PACKED_SWITCH.S
    *
    * Code: Jump to a new instruction using a jump table
    *
    * For: packed-switch, sparse-switch
    *
    * Description: Jump to a new instruction based on the value in the given
    *              register, using a table of offsets corresponding to each
    *              value in a particular integral range, or fall through to
    *              the next instruction if there is no match.
    *
    * Format: AA|op BBBBlo BBBBhi (31t)
    *
    * Syntax: op vAA, +BBBBBBBB
    */


    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $16, %edx              # prepare to create +BBBBBBBB
    or          %edx, %ecx              # %ecx<- +BBBBBBBB
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, -4(%esp)         # push parameter vAA
    lea         (rPC, %ecx, 2), %ecx    # %ecx<- PC + +BBBBBBBB*2
    movl        %ecx, -8(%esp)          # push parameter PC + +BBBBBBBB*2
    lea         -8(%esp), %esp
    call        dvmInterpHandlePackedSwitch                   # call code-unit branch offset
    shl         $1, %eax               # shift for byte offset
    movl        %eax, %edx              # %edx<- offset
    lea         8(%esp), %esp
    jle         common_periodicChecks_backwardBranch  # do backward branch
    jmp         .LOP_PACKED_SWITCH_finish

/* ------------------------------ */
    .balign 64
.L_OP_SPARSE_SWITCH: /* 0x2c */
/* File: x86-atom/OP_SPARSE_SWITCH.S */
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
    * File: OP_SPARSE_SWITCH.S
    */

/* File: x86-atom/OP_PACKED_SWITCH.S */
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
    * File: OP_PACKED_SWITCH.S
    *
    * Code: Jump to a new instruction using a jump table
    *
    * For: packed-switch, sparse-switch
    *
    * Description: Jump to a new instruction based on the value in the given
    *              register, using a table of offsets corresponding to each
    *              value in a particular integral range, or fall through to
    *              the next instruction if there is no match.
    *
    * Format: AA|op BBBBlo BBBBhi (31t)
    *
    * Syntax: op vAA, +BBBBBBBB
    */


    FETCH       1, %ecx                 # %ecx<- BBBBlo
    FETCH       2, %edx                 # %edx<- BBBBhi
    shl         $16, %edx              # prepare to create +BBBBBBBB
    or          %edx, %ecx              # %ecx<- +BBBBBBBB
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, -4(%esp)         # push parameter vAA
    lea         (rPC, %ecx, 2), %ecx    # %ecx<- PC + +BBBBBBBB*2
    movl        %ecx, -8(%esp)          # push parameter PC + +BBBBBBBB*2
    lea         -8(%esp), %esp
    call        dvmInterpHandleSparseSwitch                   # call code-unit branch offset
    shl         $1, %eax               # shift for byte offset
    movl        %eax, %edx              # %edx<- offset
    lea         8(%esp), %esp
    jle         common_periodicChecks_backwardBranch  # do backward branch
    jmp         .LOP_SPARSE_SWITCH_finish


/* ------------------------------ */
    .balign 64
.L_OP_CMPL_FLOAT: /* 0x2d */
/* File: x86-atom/OP_CMPL_FLOAT.S */
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
    * File: OP_CMPL_FLOAT.S
    *
    * Code: Provides a "nan" variable to specify the return value for
    *       NaN. Provides a variable "sod" which appends a "s" or a "d"
    *       to the move and comparison instructions, depending on if we
    *       are working with a float or a double. For instructions
    *       cmpx-float and cmpx-double, the x will be eiher a g or a l
    *       to specify positive or negative bias for NaN.
    *
    * For: cmpg-double, dmpg-float, cmpl-double, cmpl-float
    *
    * Description: Perform the indicated floating point or long comparison,
    *              storing 0 if the two arguments are equal, 1 if the second
    *              argument is larger, or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movss    (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    comiss   (rFP, %edx, 4), %xmm0   # do comparison
    ja          .LOP_CMPL_FLOAT_greater
    jp          .LOP_CMPL_FLOAT_finalNan
    jz          .LOP_CMPL_FLOAT_final

.LOP_CMPL_FLOAT_less:
    movl        $0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_CMPG_FLOAT: /* 0x2e */
/* File: x86-atom/OP_CMPG_FLOAT.S */
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
    * File: OP_CMPG_FLOAT.S
    */

/* File: x86-atom/OP_CMPL_FLOAT.S */
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
    * File: OP_CMPL_FLOAT.S
    *
    * Code: Provides a "nan" variable to specify the return value for
    *       NaN. Provides a variable "sod" which appends a "s" or a "d"
    *       to the move and comparison instructions, depending on if we
    *       are working with a float or a double. For instructions
    *       cmpx-float and cmpx-double, the x will be eiher a g or a l
    *       to specify positive or negative bias for NaN.
    *
    * For: cmpg-double, dmpg-float, cmpl-double, cmpl-float
    *
    * Description: Perform the indicated floating point or long comparison,
    *              storing 0 if the two arguments are equal, 1 if the second
    *              argument is larger, or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movss    (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    comiss   (rFP, %edx, 4), %xmm0   # do comparison
    ja          .LOP_CMPG_FLOAT_greater
    jp          .LOP_CMPG_FLOAT_finalNan
    jz          .LOP_CMPG_FLOAT_final

.LOP_CMPG_FLOAT_less:
    movl        $0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp



/* ------------------------------ */
    .balign 64
.L_OP_CMPL_DOUBLE: /* 0x2f */
/* File: x86-atom/OP_CMPL_DOUBLE.S */
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
    * File: OP_CMPL_DOUBLE.S
    */

/* File: x86-atom/OP_CMPL_FLOAT.S */
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
    * File: OP_CMPL_FLOAT.S
    *
    * Code: Provides a "nan" variable to specify the return value for
    *       NaN. Provides a variable "sod" which appends a "s" or a "d"
    *       to the move and comparison instructions, depending on if we
    *       are working with a float or a double. For instructions
    *       cmpx-float and cmpx-double, the x will be eiher a g or a l
    *       to specify positive or negative bias for NaN.
    *
    * For: cmpg-double, dmpg-float, cmpl-double, cmpl-float
    *
    * Description: Perform the indicated floating point or long comparison,
    *              storing 0 if the two arguments are equal, 1 if the second
    *              argument is larger, or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movsd    (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    comisd   (rFP, %edx, 4), %xmm0   # do comparison
    ja          .LOP_CMPL_DOUBLE_greater
    jp          .LOP_CMPL_DOUBLE_finalNan
    jz          .LOP_CMPL_DOUBLE_final

.LOP_CMPL_DOUBLE_less:
    movl        $0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp



/* ------------------------------ */
    .balign 64
.L_OP_CMPG_DOUBLE: /* 0x30 */
/* File: x86-atom/OP_CMPG_DOUBLE.S */
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
    * File: OP_CMPG_DOUBLE.S
    */

/* File: x86-atom/OP_CMPL_FLOAT.S */
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
    * File: OP_CMPL_FLOAT.S
    *
    * Code: Provides a "nan" variable to specify the return value for
    *       NaN. Provides a variable "sod" which appends a "s" or a "d"
    *       to the move and comparison instructions, depending on if we
    *       are working with a float or a double. For instructions
    *       cmpx-float and cmpx-double, the x will be eiher a g or a l
    *       to specify positive or negative bias for NaN.
    *
    * For: cmpg-double, dmpg-float, cmpl-double, cmpl-float
    *
    * Description: Perform the indicated floating point or long comparison,
    *              storing 0 if the two arguments are equal, 1 if the second
    *              argument is larger, or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movsd    (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    comisd   (rFP, %edx, 4), %xmm0   # do comparison
    ja          .LOP_CMPG_DOUBLE_greater
    jp          .LOP_CMPG_DOUBLE_finalNan
    jz          .LOP_CMPG_DOUBLE_final

.LOP_CMPG_DOUBLE_less:
    movl        $0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp



/* ------------------------------ */
    .balign 64
.L_OP_CMP_LONG: /* 0x31 */
/* File: x86-atom/OP_CMP_LONG.S */
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
    * File: OP_CMP_LONG.S
    *
    * Code: Compare floating point values. Uses no substitutions.
    *
    * For: cmp-long
    *
    * Description: Perform a long comparison, storing 0 if the two
    *              arguments are equal, 1 if the second argument is larger
    *              or -1 if the first argument is larger.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    movl        4(rFP, %ecx, 4), %eax   # %eax<- vBBhi
    cmp         4(rFP, %edx, 4), %eax   # compare vCChi and vBBhi
    jl          .LOP_CMP_LONG_less
    jg          .LOP_CMP_LONG_greater
    movl        (rFP, %ecx, 4), %eax    # %eax<- vBBlo
    cmp         (rFP, %edx, 4), %eax    # compare vCClo and vBBlo
    ja          .LOP_CMP_LONG_greater
    jne         .LOP_CMP_LONG_less
    jmp         .LOP_CMP_LONG_final

/* ------------------------------ */
    .balign 64
.L_OP_IF_EQ: /* 0x32 */
/* File: x86-atom/OP_IF_EQ.S */
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
    * File: OP_IF_EQ.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    jne  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_NE: /* 0x33 */
/* File: x86-atom/OP_IF_NE.S */
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
    * File: OP_IF_NE.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    je  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_LT: /* 0x34 */
/* File: x86-atom/OP_IF_LT.S */
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
    * File: OP_IF_LT.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    jge  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_GE: /* 0x35 */
/* File: x86-atom/OP_IF_GE.S */
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
    * File: OP_IF_GE.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    jl  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_GT: /* 0x36 */
/* File: x86-atom/OP_IF_GT.S */
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
    * File: OP_IF_GT.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    jle  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_LE: /* 0x37 */
/* File: x86-atom/OP_IF_LE.S */
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
    * File: OP_IF_LE.S
    */

/* File: x86-atom/bincmp.S */
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
    * File: bincmp.S
    *
    * Code: Generic 32-bit comparison operation. Provides a "revcmp"
    *       variable to specify the reverse comparison to perform.
    *
    * For: if-eq, if-ge, if-gt, if-le, if-lt, if-ne
    *
    * Description: Branch to the given destination if the comparison
    *              test between the given registers values is true.
    *
    * Format: B|A|op CCCC (22t)
    *
    * Syntax: op vA, vB, +CCCC
    */

    movl        rINST,  %eax            # %eax<- BA
    andl        $15, rINST             # rINST<- A
    shr         $4, %eax               # %eax<- B
    GET_VREG    rINST                   # rINST<- vA
    movl        $4, %edx               # %edx<- 4
    cmp         (rFP, %eax, 4), rINST   # compare vA and vB
    jg  1f                      # goto next instruction if reverse
                                        # comparison is true
    FETCHs      1, %edx                 # %edx<- +CCCC, Branch offset
    sal         $1, %edx
    js          common_periodicChecks_backwardBranch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_EQZ: /* 0x38 */
/* File: x86-atom/OP_IF_EQZ.S */
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
    * File: OP_IF_EQZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    jne  OP_IF_EQZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_EQZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_NEZ: /* 0x39 */
/* File: x86-atom/OP_IF_NEZ.S */
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
    * File: OP_IF_NEZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    je  OP_IF_NEZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_NEZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_LTZ: /* 0x3a */
/* File: x86-atom/OP_IF_LTZ.S */
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
    * File: OP_IF_LTZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    jge  OP_IF_LTZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_LTZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_GEZ: /* 0x3b */
/* File: x86-atom/OP_IF_GEZ.S */
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
    * File: OP_IF_GEZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    jl  OP_IF_GEZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_GEZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_GTZ: /* 0x3c */
/* File: x86-atom/OP_IF_GTZ.S */
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
    * File: OP_IF_GTZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    jle  OP_IF_GTZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_GTZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IF_LEZ: /* 0x3d */
/* File: x86-atom/OP_IF_LEZ.S */
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
    * File: OP_IF_LEZ.S
    */

/* File: x86-atom/zcmp.S */
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

    cmp         $0, (rFP, rINST, 4)    # compare vAA with zero
    jg  OP_IF_LEZ_2f                    # goto next instruction or branch
    FETCHs      1, %edx                 # %edx<- BBBB; branch offset
    sal         $1, %edx               # %edx<- adjust byte offset

   /*
    * Inline common_backwardBranch
    */

    js          common_periodicChecks_backwardBranch  # jump on backwards branch
1:
    FINISH_RB   %edx, %ecx              # jump to next instruction

   /*
    * FINISH code
    */

OP_IF_LEZ_2f:
    movzbl      4(rPC), %edx            # grab the next opcode
    movzbl      5(rPC), rINST           # update the instruction
    addl        $4, rPC                # update the program counter
    jmp         *dvmAsmInstructionJmpTable(, %edx, 4) # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3E: /* 0x3e */
/* File: x86-atom/OP_UNUSED_3E.S */
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
    * File: OP_UNUSED_3E.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3F: /* 0x3f */
/* File: x86-atom/OP_UNUSED_3F.S */
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
    * File: OP_UNUSED_3F.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_40: /* 0x40 */
/* File: x86-atom/OP_UNUSED_40.S */
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
    * File: OP_UNUSED_40.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_41: /* 0x41 */
/* File: x86-atom/OP_UNUSED_41.S */
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
    * File: OP_UNUSED_41.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_42: /* 0x42 */
/* File: x86-atom/OP_UNUSED_42.S */
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
    * File: OP_UNUSED_42.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_43: /* 0x43 */
/* File: x86-atom/OP_UNUSED_43.S */
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
    * File: OP_UNUSED_43.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_AGET: /* 0x44 */
/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 4), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_AGET_WIDE: /* 0x45 */
/* File: x86-atom/OP_AGET_WIDE.S */
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
    * File: OP_AGET_WIDE.S
    *
    * Code: 64-bit array get operation.
    *
    * For: aget-wide
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the destination
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        offArrayObject_contents(%ecx, %edx, 8), %xmm0 # %xmm0<- vBB[vCC]
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %xmm0; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_AGET_OBJECT: /* 0x46 */
/* File: x86-atom/OP_AGET_OBJECT.S */
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
    * File: OP_AGET_OBJECT.S
    */

/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 4), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_AGET_BOOLEAN: /* 0x47 */
/* File: x86-atom/OP_AGET_BOOLEAN.S */
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
    * File: OP_AGET_BOOLEAN.S
    */

/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 1), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movzbl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_AGET_BYTE: /* 0x48 */
/* File: x86-atom/OP_AGET_BYTE.S */
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
    * File: OP_AGET_BYTE.S
    */

/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 1), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movsbl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_AGET_CHAR: /* 0x49 */
/* File: x86-atom/OP_AGET_CHAR.S */
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
    * File: OP_AGET_CHAR.S
    */

/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 2), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movzwl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_AGET_SHORT: /* 0x4a */
/* File: x86-atom/OP_AGET_SHORT.S */
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
    * File: OP_AGET_SHORT.S
    */

/* File: x86-atom/OP_AGET.S */
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
    * File: OP_AGET.S
    *
    * Code: Generic 32-bit array "get" operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *
    * For: aget, aget-boolean, aget-byte, aget-char, aget-object, sget, aget-short
    *
    * Description: Perform an array get operation at the identified index
    *              of a given array; load the array value into the value
    *              register. vAA <- vBB[vCC].
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    lea         (%ecx, %edx, 2), %ecx # %ecx<- &vBB[vCC]
                                           # trying: lea (%ecx, %edx, scale), %ecx
                                           # to reduce code size
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movswl offArrayObject_contents(%ecx), %edx # %edx<- vBB[vCC]
                                                # doing this and the previous instr
                                                # with one instr was not faster
    SET_VREG    %edx  rINST             # vAA<- %edx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_APUT: /* 0x4b */
/* File: x86-atom/OP_APUT.S */
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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, 4), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    movl     rINST, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_APUT_WIDE: /* 0x4c */
/* File: x86-atom/OP_APUT_WIDE.S */
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
    * File: OP_APUT_WIDE.S
    *
    * Code: 64-bit array put operation.
    *
    * For: aput-wide
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vAA
    movq        %xmm0, offArrayObject_contents(%ecx, %edx, 8) # vBB[vCC]<- %xmm0; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_APUT_OBJECT: /* 0x4d */
/* File: x86-atom/OP_APUT_OBJECT.S */
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
    * File: OP_APUT_OBJECT.S
    *
    * Code: 32-bit array put operation.  Provides an "scale" variable
    *       specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       mov performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the mov
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %eax                    # %eax<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %eax               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%eax), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    GET_VREG    rINST                   # rINST<- vAA
    lea         (%eax, %edx, 4), %edx   # %edx<- &vBB[vCC]
    cmp         $0, rINST              # check for null reference
    je          .LOP_APUT_OBJECT_skip_check  # reference is null so skip type check
    jmp         .LOP_APUT_OBJECT_finish

/* ------------------------------ */
    .balign 64
.L_OP_APUT_BOOLEAN: /* 0x4e */
/* File: x86-atom/OP_APUT_BOOLEAN.S */
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
    * File: OP_APUT_BOOLEAN.S
    */

/* File: x86-atom/OP_APUT.S */
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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, 1), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    movb     rINSTbl, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_APUT_BYTE: /* 0x4f */
/* File: x86-atom/OP_APUT_BYTE.S */
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
    * File: OP_APUT_BYTE.S
    */

/* File: x86-atom/OP_APUT.S */
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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, 1), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    movb     rINSTbl, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_APUT_CHAR: /* 0x50 */
/* File: x86-atom/OP_APUT_CHAR.S */
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
    * File: OP_APUT_CHAR.S
    */

/* File: x86-atom/OP_APUT.S */
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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, 2), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    movw     rINSTw, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_APUT_SHORT: /* 0x51 */
/* File: x86-atom/OP_APUT_SHORT.S */
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
    * File: OP_APUT_SHORT.S
    */

/* File: x86-atom/OP_APUT.S */
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
    * File: OP_APUT.S
    *
    * Code: Generic 32-bit array put operation.  Provides a "scale" variable
    *       to specify a scale value which depends on the width of the array
    *       elements. Provides a "mov" variable which determines the type of
    *       move performed also dependent on the type of the array element.
    *       Provides a "value" register to specify the source of the move
    *
    * For: aput-boolean, aput-byte, aput-char, aput-object, aput-short
    *
    * Description: Perform an array put operation from the value register;
    *              store the value register at the identified index of a
    *              given array. vBB[vCC] <- vAA
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    cmp         $0, %ecx               # check for null array object
    je          common_errNullObject    # handle null array object
    cmp         offArrayObject_length(%ecx), %edx # compare index to arrayObj->length
    jnc         common_errArrayIndex    # handle index >= length, bail
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         (%ecx, %edx, 2), %ecx # %ecx<- &vBB[vCC]
    GET_VREG    rINST                   # rINST<- vAA
    movw     rINSTw, offArrayObject_contents(%ecx) # vBB[vCC]<- rINSTx; value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_IGET: /* 0x52 */
/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_finish

/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE: /* 0x53 */
/* File: x86-atom/OP_IGET_WIDE.S */
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
    * File: OP_IGET_WIDE.S
    *
    * Code: 64 bit instance field "get" operation. Uses no substitutions.
    *
    * For: iget-wide
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    * Format:  B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %eax             # %eax<- MterpGlue pointer
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- pDvmDex
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- CCCC
    FETCH       1, %edx                 # %edx<- pDvmDex->pResFields
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved InstField ptr
    cmp         $0, %ecx               # check for null ptr; resolved InstField ptr
    jne         .LOP_IGET_WIDE_finish
    movl        offGlue_method(%eax), %ecx # %ecx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- method->clazz
    movl        %ecx, -8(%esp)          # push parameter CCCC; field ref
    movl        %edx, -4(%esp)          # push parameter method->clazz
    jmp         .LOP_IGET_WIDE_finish2

/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT: /* 0x54 */
/* File: x86-atom/OP_IGET_OBJECT.S */
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
    * File: OP_IGET_OBJECT.S
    */

/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_OBJECT_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_OBJECT_finish


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BOOLEAN: /* 0x55 */
/* File: x86-atom/OP_IGET_BOOLEAN.S */
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
    * File: OP_IGET_BOOLEAN.S
    */

/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_BOOLEAN_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_BOOLEAN_finish


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BYTE: /* 0x56 */
/* File: x86-atom/OP_IGET_BYTE.S */
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
    * File: OP_IGET_BYTE.S
    */

/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_BYTE_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_BYTE_finish


/* ------------------------------ */
    .balign 64
.L_OP_IGET_CHAR: /* 0x57 */
/* File: x86-atom/OP_IGET_CHAR.S */
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
    * File: OP_IGET_CHAR.S
    */

/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_CHAR_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_CHAR_finish


/* ------------------------------ */
    .balign 64
.L_OP_IGET_SHORT: /* 0x58 */
/* File: x86-atom/OP_IGET_SHORT.S */
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
    * File: OP_IGET_SHORT.S
    */

/* File: x86-atom/OP_IGET.S */
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
    * File: OP_IGET.S
    *
    * Code: Generic 32-bit instance field "get" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iget's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iget-boolean, iget-byte, iget-char, iget-object, iget
    *      iget-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IGET_SHORT_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_method(%edx), %edx # %edx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    jmp         .LOP_IGET_SHORT_finish


/* ------------------------------ */
    .balign 64
.L_OP_IPUT: /* 0x59 */
/* File: x86-atom/OP_IPUT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_finish

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE: /* 0x5a */
/* File: x86-atom/OP_IPUT_WIDE.S */
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
    * File: OP_IPUT_WIDE.S
    *
    * Code: 64 bit instance field "put" operation. Uses no substitutions.
    *
    * For: iget-wide
    *
    * Description: Perform the object instance field "put" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    * Format:  B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %eax             # %eax<- MterpGlue pointer
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- pDvmDex
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- CCCC
    FETCH       1, %edx                 # %edx<- pDvmDex->pResFields
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved InstField ptr
    cmp         $0, %ecx               # check for null ptr; resolved InstField ptr
    jne         .LOP_IPUT_WIDE_finish
    movl        offGlue_method(%eax), %ecx # %ecx <- current method
    EXPORT_PC                           # in case an exception is thrown
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- method->clazz
    movl        %ecx, -8(%esp)          # push parameter CCCC; field ref
    movl        %edx, -4(%esp)          # push parameter method->clazz
    jmp         .LOP_IPUT_WIDE_finish2

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT: /* 0x5b */
/* File: x86-atom/OP_IPUT_OBJECT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_OBJECT_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_OBJECT_finish

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BOOLEAN: /* 0x5c */
/* File: x86-atom/OP_IPUT_BOOLEAN.S */
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
    * File: OP_IPUT_BOOLEAN.S
    */

/* File: x86-atom/OP_IPUT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_BOOLEAN_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_BOOLEAN_finish


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BYTE: /* 0x5d */
/* File: x86-atom/OP_IPUT_BYTE.S */
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
    * File: OP_IPUT_BYTE.S
    */

/* File: x86-atom/OP_IPUT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_BYTE_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_BYTE_finish


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_CHAR: /* 0x5e */
/* File: x86-atom/OP_IPUT_CHAR.S */
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
    * File: OP_IPUT_CHAR.S
    */

/* File: x86-atom/OP_IPUT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_CHAR_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_CHAR_finish


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_SHORT: /* 0x5f */
/* File: x86-atom/OP_IPUT_SHORT.S */
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
    * File: OP_IPUT_SHORT.S
    */

/* File: x86-atom/OP_IPUT.S */
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
    * File: OP_IPUT.S
    *
    * Code: Generic 32-bit instance field "put" operation. Provides a
    *       "mov" variable which determines the type of mov performed.
    *       Currently, none of the iput's use this variable - may want
    *       to change this, but seems ok for now.
    *
    * For: iput-boolean, iput-byte, iput-char, iput-object, iput
    *      iput-short
    *
    * Description: Perform the object instance field "get" operation
    *              with the identified field; load the instance value into
    *              the value register.
    *
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, type@CCCC
    *         op vA, vB, field@CCCC
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %edx # %edx<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- CCCC
    movl        offDvmDex_pResFields(%edx), %edx # %edx<- pDvmDex->pResFields
    cmp         $0, (%edx, %ecx, 4)    # check for null ptr; resolved InstField ptr
    movl        (%edx, %ecx, 4), %eax   # %eax<- resolved InstField ptr
    jne         .LOP_IPUT_SHORT_finish2
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    jmp         .LOP_IPUT_SHORT_finish


/* ------------------------------ */
    .balign 64
.L_OP_SGET: /* 0x60 */
/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_resolve
    jmp         .LOP_SGET_finish

/* ------------------------------ */
    .balign 64
.L_OP_SGET_WIDE: /* 0x61 */
/* File: x86-atom/OP_SGET_WIDE.S */
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
    * File: OP_SGET_WIDE.S
    *
    * Code: 64-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-wide
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field, loading or storing
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %edx                 # %edx<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %edx, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_WIDE_resolve

.LOP_SGET_WIDE_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        offStaticField_value(%ecx), %xmm0 # %xmm0<- field value
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- field value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_SGET_OBJECT: /* 0x62 */
/* File: x86-atom/OP_SGET_OBJECT.S */
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
    * File: OP_SGET_OBJECT.S
    */

/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_OBJECT_resolve
    jmp         .LOP_SGET_OBJECT_finish


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BOOLEAN: /* 0x63 */
/* File: x86-atom/OP_SGET_BOOLEAN.S */
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
    * File: OP_SGET_BOOLEAN.S
    */

/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_BOOLEAN_resolve
    jmp         .LOP_SGET_BOOLEAN_finish


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BYTE: /* 0x64 */
/* File: x86-atom/OP_SGET_BYTE.S */
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
    * File: OP_SGET_BYTE.S
    */

/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_BYTE_resolve
    jmp         .LOP_SGET_BYTE_finish


/* ------------------------------ */
    .balign 64
.L_OP_SGET_CHAR: /* 0x65 */
/* File: x86-atom/OP_SGET_CHAR.S */
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
    * File: OP_SGET_CHAR.S
    */

/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_CHAR_resolve
    jmp         .LOP_SGET_CHAR_finish


/* ------------------------------ */
    .balign 64
.L_OP_SGET_SHORT: /* 0x66 */
/* File: x86-atom/OP_SGET_SHORT.S */
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
    * File: OP_SGET_SHORT.S
    */

/* File: x86-atom/OP_SGET.S */
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
    * File: OP_SGET.S
    *
    * Code: Generic 32-bit static field "get" operation. Uses no substitutions.
    *
    * For: sget-boolean, sget-byte, sget-char, sget-object, sget, sget-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; load the field value
    *              into the value register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SGET_SHORT_resolve
    jmp         .LOP_SGET_SHORT_finish


/* ------------------------------ */
    .balign 64
.L_OP_SPUT: /* 0x67 */
/* File: x86-atom/OP_SPUT.S */
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
    * File: OP_SPUT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_resolve
    jmp         .LOP_SPUT_finish

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_WIDE: /* 0x68 */
/* File: x86-atom/OP_SPUT_WIDE.S */
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
    * File: OP_SPUT_WIDE.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %ecx # %ecx<- glue->pDvmDex
    FETCH       1, %edx                 # %edx<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %edx, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_WIDE_resolve

.LOP_SPUT_WIDE_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vAA
    movq        %xmm0, offStaticField_value(%ecx) # field value<- field value
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_OBJECT: /* 0x69 */
/* File: x86-atom/OP_SPUT_OBJECT.S */
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
    * File: OP_SPUT_OBJECT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField
    je          .LOP_SPUT_OBJECT_resolve
    jmp         .LOP_SPUT_OBJECT_finish

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BOOLEAN: /* 0x6a */
/* File: x86-atom/OP_SPUT_BOOLEAN.S */
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
    * File: OP_SPUT_BOOLEAN.S
    */

/* File: x86-atom/OP_SPUT.S */
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
    * File: OP_SPUT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_BOOLEAN_resolve
    jmp         .LOP_SPUT_BOOLEAN_finish


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BYTE: /* 0x6b */
/* File: x86-atom/OP_SPUT_BYTE.S */
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
    * File: OP_SPUT_BYTE.S
    */

/* File: x86-atom/OP_SPUT.S */
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
    * File: OP_SPUT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_BYTE_resolve
    jmp         .LOP_SPUT_BYTE_finish


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_CHAR: /* 0x6c */
/* File: x86-atom/OP_SPUT_CHAR.S */
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
    * File: OP_SPUT_CHAR.S
    */

/* File: x86-atom/OP_SPUT.S */
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
    * File: OP_SPUT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_CHAR_resolve
    jmp         .LOP_SPUT_CHAR_finish


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_SHORT: /* 0x6d */
/* File: x86-atom/OP_SPUT_SHORT.S */
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
    * File: OP_SPUT_SHORT.S
    */

/* File: x86-atom/OP_SPUT.S */
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
    * File: OP_SPUT.S
    *
    * Code: Generic 32-bit static field "put" operation. Uses no substitutions.
    *
    * For: sput-boolean, sput-byte, sput-char, sput-object, sput, sput-short
    *
    * Description: Perform the identified object static field operation
    *              with the identified static field; store the field value
    *              register.
    *
    * Format: AA|op BBBB (21c)
    *
    * Syntax: op vAA, string@BBBB
    */

    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offDvmDex_pResFields(%ecx), %ecx # %ecx<- pResFields
    cmp         $0, (%ecx, %eax, 4)    # check for null ptr; resolved StaticField ptr
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved StaticField ptr
    je          .LOP_SPUT_SHORT_resolve
    jmp         .LOP_SPUT_SHORT_finish


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL: /* 0x6e */
/* File: x86-atom/OP_INVOKE_VIRTUAL.S */
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
    * File: OP_INVOKE_VIRTUAL.S
    *
    * Code: Call a virtual method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-virtual, invoke-virtual/range
    *
    * Description: invoke-virtual is used to invoke a normal virtual method;
    *              a method that is not static or final, and is not a constructor.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # must export pc for invoke
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offDvmDex_pResMethods(%eax), %eax # %eax<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if         (!0)
    and         $15, %edx              # %edx<- D if not range
    .endif
    cmp         $0, (%eax, %ecx, 4)    # check if already resolved
    je          .LOP_INVOKE_VIRTUAL_break
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved base method
    jmp         .LOP_INVOKE_VIRTUAL_continue

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER: /* 0x6f */
/* File: x86-atom/OP_INVOKE_SUPER.S */
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
    * File: OP_INVOKE_SUPER.S
    *
    * Code: Call super method.
    *
    * For: invoke-super, invoke-super/range
    *
    * Description: invoke-super is used to invoke the closest superclass's virtual
    *              method (as opposed to the one with the same method_id in the
    *              calling class).
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    FETCH       2, %eax                 # %eax<- GFED or CCCC
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    .if         (!0)
    and         $15, %eax              # %eax<- D if not range
    .endif
    FETCH       1, %edx                 # %edx<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    cmp         $0, (rFP, %eax, 4)     # check for null object
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved base method
    je          common_errNullObject    # handle null object
    jmp         .LOP_INVOKE_SUPER_continue2

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT: /* 0x70 */
/* File: x86-atom/OP_INVOKE_DIRECT.S */
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
    * File: OP_INVOKE_DIRECT.S
    *
    * Code: Call a non-static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-direct, invoke-direct/range
    *
    * Description: invoke-direct is used to invoke a non-static direct method;
    *              an instance method that is non-overridable, for example,
    *              either a private instance method or a constructor.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    .if         (!0)
    andl        $15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    movl        %edx, -4(%esp)          # save "this" pointer register
    cmp         $0, %ecx               # check if already resolved
    GET_VREG    %edx                    # %edx<- "this" pointer
    je          .LOP_INVOKE_DIRECT_resolve     # handle resolve

.LOP_INVOKE_DIRECT_finish:
    cmp         $0, %edx               # check for null "this"
    jne         common_invokeMethodNoRange # invoke method common code
    jmp         common_errNullObject

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC: /* 0x71 */
/* File: x86-atom/OP_INVOKE_STATIC.S */
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
    * File: OP_INVOKE_STATIC.S
    *
    * Code: Call static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_static that allows up to 255 arguments.
    *
    * For: invoke-static, invoke-static/range
    *
    * Description: invoke-static is used to invoke static direct method.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %edx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %edx<- pDvmDex->pResMethods
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    cmp         $0, %ecx               # check if already resolved
    EXPORT_PC                           # must export for invoke
    jne         common_invokeMethodNoRange # invoke method common code
    jmp         .LOP_INVOKE_STATIC_break

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE: /* 0x72 */
/* File: x86-atom/OP_INVOKE_INTERFACE.S */
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
    * File: OP_INVOKE_INTERFACE.S
    *
    * Code: Call at method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_interface that allows up to 255 arguments.
    *
    * For: invoke-interface, invoke-interface-range
    *
    * Description: invoke-interface is used to invoke an interface method; on an
    *              object whose concrete class isn't known, using a method_id that
    *              refers to an interface.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    FETCH       1, %ecx                 # %ecx<- method index
    movl        %ecx, -12(%esp)         # push argument method index
    .if         (!0)
    and         $15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    GET_VREG    %edx                    # %edx<- first arg "this pointer"
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- glue->pDvmDex
    movl        %eax, -4(%esp)          # push parameter class
    cmp         $0, %edx               # check for null object
    je          common_errNullObject    # handle null object
    jmp         .LOP_INVOKE_INTERFACE_break

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_73: /* 0x73 */
/* File: x86-atom/OP_UNUSED_73.S */
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
    * File: OP_UNUSED_73.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_RANGE: /* 0x74 */
/* File: x86-atom/OP_INVOKE_VIRTUAL_RANGE.S */
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
    * File: OP_INVOKE_VIRTUAL_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_VIRTUAL.S */
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
    * File: OP_INVOKE_VIRTUAL.S
    *
    * Code: Call a virtual method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-virtual, invoke-virtual/range
    *
    * Description: invoke-virtual is used to invoke a normal virtual method;
    *              a method that is not static or final, and is not a constructor.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %eax             # %eax<- pMterpGlue
    EXPORT_PC                           # must export pc for invoke
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- pDvmDex
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offDvmDex_pResMethods(%eax), %eax # %eax<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if         (!1)
    and         $15, %edx              # %edx<- D if not range
    .endif
    cmp         $0, (%eax, %ecx, 4)    # check if already resolved
    je          .LOP_INVOKE_VIRTUAL_RANGE_break
    movl        (%eax, %ecx, 4), %eax   # %eax<- resolved base method
    jmp         .LOP_INVOKE_VIRTUAL_RANGE_continue


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_RANGE: /* 0x75 */
/* File: x86-atom/OP_INVOKE_SUPER_RANGE.S */
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
    * File: OP_INVOKE_SUPER_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_SUPER.S */
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
    * File: OP_INVOKE_SUPER.S
    *
    * Code: Call super method.
    *
    * For: invoke-super, invoke-super/range
    *
    * Description: invoke-super is used to invoke the closest superclass's virtual
    *              method (as opposed to the one with the same method_id in the
    *              calling class).
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    FETCH       2, %eax                 # %eax<- GFED or CCCC
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    .if         (!1)
    and         $15, %eax              # %eax<- D if not range
    .endif
    FETCH       1, %edx                 # %edx<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    cmp         $0, (rFP, %eax, 4)     # check for null object
    movl        (%ecx, %edx, 4), %ecx   # %ecx<- resolved base method
    je          common_errNullObject    # handle null object
    jmp         .LOP_INVOKE_SUPER_RANGE_continue2


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_RANGE: /* 0x76 */
/* File: x86-atom/OP_INVOKE_DIRECT_RANGE.S */
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
    * File: OP_INVOKE_DIRECT_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_DIRECT.S */
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
    * File: OP_INVOKE_DIRECT.S
    *
    * Code: Call a non-static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_direct that allows up to 255 arguments.
    *
    * For: invoke-direct, invoke-direct/range
    *
    * Description: invoke-direct is used to invoke a non-static direct method;
    *              an instance method that is non-overridable, for example,
    *              either a private instance method or a constructor.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %ecx<- pDvmDex->pResMethods
    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    .if         (!1)
    andl        $15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    movl        %edx, -4(%esp)          # save "this" pointer register
    cmp         $0, %ecx               # check if already resolved
    GET_VREG    %edx                    # %edx<- "this" pointer
    je          .LOP_INVOKE_DIRECT_RANGE_resolve     # handle resolve

.LOP_INVOKE_DIRECT_RANGE_finish:
    cmp         $0, %edx               # check for null "this"
    jne         common_invokeMethodRange # invoke method common code
    jmp         common_errNullObject


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC_RANGE: /* 0x77 */
/* File: x86-atom/OP_INVOKE_STATIC_RANGE.S */
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
    * File: OP_INVOKE_STATIC_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_STATIC.S */
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
    * File: OP_INVOKE_STATIC.S
    *
    * Code: Call static direct method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_static that allows up to 255 arguments.
    *
    * For: invoke-static, invoke-static/range
    *
    * Description: invoke-static is used to invoke static direct method.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        offGlue_methodClassDex(%edx), %ecx # %edx<- pDvmDex
    FETCH       1, %eax                 # %eax<- method index
    movl        offDvmDex_pResMethods(%ecx), %ecx # %edx<- pDvmDex->pResMethods
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved method to call
    cmp         $0, %ecx               # check if already resolved
    EXPORT_PC                           # must export for invoke
    jne         common_invokeMethodRange # invoke method common code
    jmp         .LOP_INVOKE_STATIC_RANGE_break


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE_RANGE: /* 0x78 */
/* File: x86-atom/OP_INVOKE_INTERFACE_RANGE.S */
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
    * File: OP_INVOKE_INTERFACE_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_INTERFACE.S */
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
    * File: OP_INVOKE_INTERFACE.S
    *
    * Code: Call at method. Provides an "isrange" variable and
    *       a "routine" variable to specify this is the "range" version of
    *       invoke_interface that allows up to 255 arguments.
    *
    * For: invoke-interface, invoke-interface-range
    *
    * Description: invoke-interface is used to invoke an interface method; on an
    *              object whose concrete class isn't known, using a method_id that
    *              refers to an interface.
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    *         AA|op BBBB CCCC (3rc)
    *
    * Syntax: [B=5] op {vD, vE, vF, vG, vA}, meth@CCCC (35c)
    *         [B=5] op {vD, vE, vF, vG, vA}, type@CCCC (35c)
    *         [B=4] op {vD, vE, vF, vG}, kind@CCCC (35c)
    *         [B=3] op {vD, vE, vF}, kind@CCCC (35c)
    *         [B=2] op {vD, vE}, kind@CCCC (35c)
    *         [B=1] op {vD}, kind@CCCC (35c)
    *         [B=0] op {}, kind@CCCC (35c)
    *
    *         op {vCCCC .. vNNNN}, meth@BBBB (3rc) (where NNNN = CCCC+AA-1, that
    *         op {vCCCC .. vNNNN}, type@BBBB (3rc) is A determines the count 0..255,
    *                                              and C determines the first register)
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    FETCH       1, %ecx                 # %ecx<- method index
    movl        %ecx, -12(%esp)         # push argument method index
    .if         (!1)
    and         $15, %edx              # %edx<- D if not range
    .endif
    EXPORT_PC                           # must export for invoke
    GET_VREG    %edx                    # %edx<- first arg "this pointer"
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_methodClassDex(%eax), %eax # %eax<- glue->pDvmDex
    movl        %eax, -4(%esp)          # push parameter class
    cmp         $0, %edx               # check for null object
    je          common_errNullObject    # handle null object
    jmp         .LOP_INVOKE_INTERFACE_RANGE_break


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_79: /* 0x79 */
/* File: x86-atom/OP_UNUSED_79.S */
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
    * File: OP_UNUSED_79.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_7A: /* 0x7a */
/* File: x86-atom/OP_UNUSED_7A.S */
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
    * File: OP_UNUSED_7A.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_NEG_INT: /* 0x7b */
/* File: x86-atom/OP_NEG_INT.S */
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
    * File: OP_NEG_INT.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
                               # do operation part 1
    neg        %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_NOT_INT: /* 0x7c */
/* File: x86-atom/OP_NOT_INT.S */
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
    * File: OP_NOT_INT.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
                               # do operation part 1
    not        %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_NEG_LONG: /* 0x7d */
/* File: x86-atom/OP_NEG_LONG.S */
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
    * File: OP_NEG_LONG.S
    */

/* File: x86-atom/unopWide.S */
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
    * File: unopWide.S
    *
    * Code: Generic 64-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%xmm0 = op %xmm1".
    *
    * For:  neg-double, neg-long, not-long
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vB
    xorps %xmm1, %xmm1                           # do operation part 1
    psubq %xmm0, %xmm1                              # do operation part 2
    movq        %xmm1, (rFP, %ecx, 4) # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_NOT_LONG: /* 0x7e */
/* File: x86-atom/OP_NOT_LONG.S */
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
    * File: OP_NOT_LONG.S
    */

/* File: x86-atom/unopWide.S */
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
    * File: unopWide.S
    *
    * Code: Generic 64-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%xmm0 = op %xmm1".
    *
    * For:  neg-double, neg-long, not-long
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vB
                               # do operation part 1
    pandn  0xFFFFFFFF, %xmm0                              # do operation part 2
    movq        %xmm0, (rFP, %ecx, 4) # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_NEG_FLOAT: /* 0x7f */
/* File: x86-atom/OP_NEG_FLOAT.S */
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
    * File: OP_NEG_FLOAT.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
                               # do operation part 1
    addl      $0x80000000, %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_NEG_DOUBLE: /* 0x80 */
/* File: x86-atom/OP_NEG_DOUBLE.S */
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
    * File: OP_NEG_DOUBLE.S
    */

/* File: x86-atom/unopWide.S */
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
    * File: unopWide.S
    *
    * Code: Generic 64-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%xmm0 = op %xmm1".
    *
    * For:  neg-double, neg-long, not-long
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vB
    movq .LdoubNeg, %xmm1                           # do operation part 1
    pxor %xmm1, %xmm0                              # do operation part 2
    movq        %xmm0, (rFP, %ecx, 4) # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_LONG: /* 0x81 */
/* File: x86-atom/OP_INT_TO_LONG.S */
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
    * File: OP_INT_TO_LONG.S
    *
    * Code:  Convert an int to a long. Uses no substitutions.
    *
    * For:
    *
    * Description: Convert an int in the source register, to a long, and
    *              stores the result in the destintation register. vA<- (long) vB
    *
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA+
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %eax               # %eax<- B
    andl        $15, %ecx              # %ecx<- A
    GET_VREG    %eax                    # %eax<- vB
    cdq                                 # %edx:%eax<- sign-extend of %eax
    movl        %eax, (rFP, %ecx, 4)    # vA<- lo part
    movl        %edx, 4(rFP, %ecx, 4)   # vA+1<- hi part
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_FLOAT: /* 0x82 */
/* File: x86-atom/OP_INT_TO_FLOAT.S */
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
    * File: OP_INT_TO_FLOAT.S
    *
    * Code: Convert an int to a float. Uses no substitutions.
    *
    * For: int-to-float
    *
    * Description: Convert an int in the source register, to a float, and
    *              stores the result in the destintation register. vA<- (float) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA+
    shr         $4, %eax               # %eax<- B
    andl        $15,  rINST            # rINST<- A
    cvtsi2ss    (rFP,%eax,4), %xmm0     # %xmm0<- vB
    movss       %xmm0, (rFP, rINST, 4)  # vA<- %xmm0
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_DOUBLE: /* 0x83 */
/* File: x86-atom/OP_INT_TO_DOUBLE.S */
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
    * File: OP_INT_TO_DOUBLE.S
    *
    * Code: Convert an int to a double. Uses no substitutions.
    *
    * For: int-to-double
    *
    * Description: Converts an int in the source register, to a double, and
    *              stores the result in the destination register. vA<- (double) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %eax             # %eax<- BA+
    shr         $4, %eax               # %eax<- B
    andl        $15, rINST             # rINST<- A
    cvtsi2sd    (rFP, %eax, 4), %xmm0   # %xmm0<- vB
    movq        %xmm0, (rFP, rINST, 4)  # vA<- %xmm0; (double) vB
    FFETCH_ADV  1, %edx                 # %edx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_INT: /* 0x84 */
/* File: x86-atom/OP_LONG_TO_INT.S */
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
    * File: OP_LONG_TO_INT.S
    */

/* File: x86-atom/OP_MOVE.S */
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
    * File: OP_MOVE.S
    *
    * Code: Copies contents from one register to another. Uses no
    *       substitutions.
    *
    * For: move, move-object, long-to-int
    *
    * Description: Copies contents from one non-object register to another.
    *              vA<- vB; fp[A]<- fp[B]
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vB
    SET_VREG    rINST, %ecx             # vA<- vB; %edx
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_FLOAT: /* 0x85 */
/* File: x86-atom/OP_LONG_TO_FLOAT.S */
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
    * File: OP_LONG_TO_FLOAT.S
    *
    * Code: Convert a long to a float. Uses no substitutions.
    *
    * For: int-to-float
    *
    * Description: Converts a float in the source register, to a float, and
    *              stores the result in the destination register. vA<- (double) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    fildll      (rFP, rINST, 4)         # FPU<- vB
    fstps       (rFP, %ecx, 4)          # vA<- FPU; (float) vB
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_DOUBLE: /* 0x86 */
/* File: x86-atom/OP_LONG_TO_DOUBLE.S */
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
    * File: OP_LONG_TO_DOUBLE.S
    *
    * Code: Convert a long to a dobule. Uses no substitutions.
    *
    * For: long-to-double
    *
    * Description: Converts a long in the source register to a double, and
    *              stores the result in the destination register. vA<- (double) vB
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, rINST              # rINST<- B
    and         $15, %ecx              # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    fildll      (rFP, rINST, 4)         # FPU<- vB
    fstpl       (rFP, %ecx, 4)          # vA<- FPU; (double) vB
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_INT: /* 0x87 */
/* File: x86-atom/OP_FLOAT_TO_INT.S */
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
    * File: OP_FLOAT_TO_INT.S
    *
    * Code: Converts a float to a int. Uses no substitutions.
    *
    * For: float-to-int
    *
    * Description: Convert the float in source register to a int
    *              and store the result in the destintation register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %edx<- A
    flds        (rFP, rINST, 4)         # push vB to floating point stack
    fildl       .LintMax                # push max int value
    fildl       .LintMin                # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .LOP_FLOAT_TO_INT_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .LOP_FLOAT_TO_INT_nanInf      # handle posInf or NaN
    jmp         .LOP_FLOAT_TO_INT_break       # do conversion

/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_LONG: /* 0x88 */
/* File: x86-atom/OP_FLOAT_TO_LONG.S */
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
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %edx<- A
    flds        (rFP, rINST, 4)         # push vB to floating point stack
    fildll      .LvaluePosInfLong       # push max int value
    fildll      .LvalueNegInfLong       # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .LOP_FLOAT_TO_LONG_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .LOP_FLOAT_TO_LONG_nanInf      # handle posInf or NaN
    jmp         .LOP_FLOAT_TO_LONG_break       # do conversion

/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_DOUBLE: /* 0x89 */
/* File: x86-atom/OP_FLOAT_TO_DOUBLE.S */
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
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %edx<- A
    flds        (rFP, rINST, 4)         # load float
    fstpl       (rFP, %edx, 4)          # store double
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_INT: /* 0x8a */
/* File: x86-atom/OP_DOUBLE_TO_INT.S */
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
    * File: OP_DOUBLE_TO_INT.S
    *
    * Code: Converts a double to an integer. Uses no substitutions.
    *
    * For: double-to-int
    *
    * Description: Convert the source register (a double) to an integer
    *              and store the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %edx<- A
    fldl        (rFP, rINST, 4)         # load &vB
    fildl       .LintMax                # push max int value
    fildl       .LintMin                # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .LOP_DOUBLE_TO_INT_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .LOP_DOUBLE_TO_INT_nanInf      # handle posInf or NaN
    jmp         .LOP_DOUBLE_TO_INT_break       # do conversion

/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_LONG: /* 0x8b */
/* File: x86-atom/OP_DOUBLE_TO_LONG.S */
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
    * File: OP_DOUBLE_TO_LONG.S
    *
    * Code: Converts a double to a long. Uses no substitutions.
    *
    * For: double-to-long
    *
    * Description: Convert the double in source register to a long
    *              and store in the destintation register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %ecx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %ecx<- A
    fldl        (rFP, rINST, 4)         # push vB to floating point stack
    fildll      .LvaluePosInfLong       # push max int value
    fildll      .LvalueNegInfLong       # push min int value
    fucomip     %st(2), %st(0)          # check for negInf
    jae         .LOP_DOUBLE_TO_LONG_negInf      # handle negInf
    fucomip     %st(1), %st(0)          # check for posInf or NaN
    jc          .LOP_DOUBLE_TO_LONG_nanInf      # handle posInf or NaN
    jmp         .LOP_DOUBLE_TO_LONG_break       # do conversion

/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_FLOAT: /* 0x8c */
/* File: x86-atom/OP_DOUBLE_TO_FLOAT.S */
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
    * File: OP_DOUBLE_TO_FLOAT.S
    *
    * Code: Converts a double to a float. Uses no substitutions.
    *
    * For: double-to-float
    *
    * Description: Convert the source register (a double) to a float
    *              and store the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    and         $15, %edx              # %edx<- A
    fldl        (rFP, rINST, 4)         # load &vB
    fstps       (rFP, %edx, 4)          # store float
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_BYTE: /* 0x8d */
/* File: x86-atom/OP_INT_TO_BYTE.S */
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
    * File: OP_INT_TO_BYTE.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    sal $24, %ecx                           # do operation part 1
    sar $24, %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_CHAR: /* 0x8e */
/* File: x86-atom/OP_INT_TO_CHAR.S */
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
    * File: OP_INT_TO_CHAR.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    sal $16, %ecx                           # do operation part 1
    shr $16, %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_SHORT: /* 0x8f */
/* File: x86-atom/OP_INT_TO_SHORT.S */
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
    * File: OP_INT_TO_SHORT.S
    */

/* File: x86-atom/unop.S */
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
    * File: unop.S
    *
    * Code: Generic 32-bit unary operation. Provide an "instr" variable and a
    *       preinstr variable that together specify an instruction that
    *       performs, for example, "%ecx = op %edx".
    *
    * For: int-to-byte, int-to-char, int-to-short, neg-float, neg-int, not-int
    *
    * Description: Perform the identified unary operation on the source
    *              register, storing the result in the destination register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    sal $16, %ecx                           # do operation part 1
    sar $16, %ecx                              # do operation part 2
    SET_VREG    %ecx, rINST             # vA<- result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT: /* 0x90 */
/* File: x86-atom/OP_ADD_INT.S */
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
    * File: OP_ADD_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    addl     %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT: /* 0x91 */
/* File: x86-atom/OP_SUB_INT.S */
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
    * File: OP_SUB_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    subl     %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT: /* 0x92 */
/* File: x86-atom/OP_MUL_INT.S */
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
    * File: OP_MUL_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    imul     %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT: /* 0x93 */
/* File: x86-atom/OP_DIV_INT.S */
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
    * File: OP_DIV_INT.S
    */

/* File: x86-atom/binopD.S */
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
    * File: binopD.S
    *
    * Code: 32-bit integer divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int, rem-int
    *
    * Description: Perform a binary operation on two source
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    GET_VREG    %eax                    # %eax<- vBB
    GET_VREG    %ecx                    # %ecx<- vCC
    cmp         $0, %ecx               # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_DIV_INT_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_DIV_INT_break
.LOP_DIV_INT_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    .if  1
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_DIV_INT_break2


/* ------------------------------ */
    .balign 64
.L_OP_REM_INT: /* 0x94 */
/* File: x86-atom/OP_REM_INT.S */
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
    * File: OP_REM_INT.S
    */

/* File: x86-atom/binopD.S */
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
    * File: binopD.S
    *
    * Code: 32-bit integer divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int, rem-int
    *
    * Description: Perform a binary operation on two source
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    GET_VREG    %eax                    # %eax<- vBB
    GET_VREG    %ecx                    # %ecx<- vCC
    cmp         $0, %ecx               # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_REM_INT_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_REM_INT_break
.LOP_REM_INT_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    .if  0
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_REM_INT_break2


/* ------------------------------ */
    .balign 64
.L_OP_AND_INT: /* 0x95 */
/* File: x86-atom/OP_AND_INT.S */
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
    * File: OP_AND_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    andl     %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT: /* 0x96 */
/* File: x86-atom/OP_OR_INT.S */
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
    * File: OP_OR_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    or %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT: /* 0x97 */
/* File: x86-atom/OP_XOR_INT.S */
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
    * File: OP_XOR_INT.S
    */

/* File: x86-atom/binop.S */
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
    * File: binop.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    * For: add-int, and-int, mul-int, or-int, sub-int, xor-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    xor     %edx, %ecx                              # %ecx<- vBB op vCC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT: /* 0x98 */
/* File: x86-atom/OP_SHL_INT.S */
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
    * File: OP_SHL_INT.S
    */

/* File: x86-atom/binopS.S */
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
    * File: binopS.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%edx = %edx op %cl"
    *
    * For: shl-int, shr-int, ushr-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    GET_VREG    %ecx                    # %ecx<- vCC
    sal     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT: /* 0x99 */
/* File: x86-atom/OP_SHR_INT.S */
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
    * File: OP_SHR_INT.S
    */

/* File: x86-atom/binopS.S */
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
    * File: binopS.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%edx = %edx op %cl"
    *
    * For: shl-int, shr-int, ushr-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    GET_VREG    %ecx                    # %ecx<- vCC
    sar     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT: /* 0x9a */
/* File: x86-atom/OP_USHR_INT.S */
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
    * File: OP_USHR_INT.S
    */

/* File: x86-atom/binopS.S */
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
    * File: binopS.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%edx = %edx op %cl"
    *
    * For: shl-int, shr-int, ushr-int
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    GET_VREG    %ecx                    # %ecx<- vCC
    shr     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG: /* 0x9b */
/* File: x86-atom/OP_ADD_LONG.S */
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
    * File: OP_ADD_LONG.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    paddq   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG: /* 0x9c */
/* File: x86-atom/OP_SUB_LONG.S */
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
    * File: OP_SUB_LONG.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    psubq    %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG: /* 0x9d */
/* File: x86-atom/OP_MUL_LONG.S */
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
    jmp         .LOP_MUL_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG: /* 0x9e */
/* File: x86-atom/OP_DIV_LONG.S */
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
    * File: OP_DIV_LONG.S
    */

/* File: x86-atom/binopDivRemLong.S */
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
    * File: binopDivRemLong.S
    *
    * Code: 64-bit long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long, rem-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_CC    1, %edx                 # %edx<- CC
    movl        (rFP, %edx, 4), %eax    # %eax<- vCC
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vCC+1
    movl        %eax, -8(%esp)          # push arg vCC
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    FETCH_BB    1, %edx                 # %edx<- BB
    movl        %ecx, -4(%esp)          # push arg vCC+1
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vBB,vBB+1
    jmp         .LOP_DIV_LONG_finish


/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG: /* 0x9f */
/* File: x86-atom/OP_REM_LONG.S */
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
    * File: OP_REM_LONG.S
    */

/* File: x86-atom/binopDivRemLong.S */
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
    * File: binopDivRemLong.S
    *
    * Code: 64-bit long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long, rem-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */


    FETCH_CC    1, %edx                 # %edx<- CC
    movl        (rFP, %edx, 4), %eax    # %eax<- vCC
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vCC+1
    movl        %eax, -8(%esp)          # push arg vCC
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    FETCH_BB    1, %edx                 # %edx<- BB
    movl        %ecx, -4(%esp)          # push arg vCC+1
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vBB,vBB+1
    jmp         .LOP_REM_LONG_finish


/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG: /* 0xa0 */
/* File: x86-atom/OP_AND_LONG.S */
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
    * File: OP_AND_LONG.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    pand   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG: /* 0xa1 */
/* File: x86-atom/OP_OR_LONG.S */
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
    * File: OP_OR_LONG.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    por %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG: /* 0xa2 */
/* File: x86-atom/OP_XOR_LONG.S */
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
    * File: OP_XOR_LONG.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    pxor   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG: /* 0xa3 */
/* File: x86-atom/OP_SHL_LONG.S */
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
    * File: OP_SHL_LONG.S
    *
    * Code: Performs a shift left long. Uses no substitutions.
    *
    * For: shl-long
    *
    * Description: Perform a binary shift operation using two source registers
    *              where one is the shift amount and the other is the value to shift.
    *              Store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_CC    1, %eax                 # %eax<- CC
    FETCH_BB    1, %edx                 # %edx<- BB
    movq        .LshiftMask, %xmm2      # %xmm2<- mask for the shift bits
    movss       (rFP, %eax, 4), %xmm0   # %xmm0<- vCC
    pand        %xmm2, %xmm0            # %xmm0<- masked shift bits
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vBB
    psllq       %xmm0, %xmm1            # %xmm1<- shifted vBB
    movq        %xmm1, (rFP, rINST, 4)  # vAA<- shifted vBB
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG: /* 0xa4 */
/* File: x86-atom/OP_SHR_LONG.S */
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
    * File: OP_SHR_LONG.S
    *
    * Code: Performs a shift right long
    *
    * For: shl-long
    *
    * Description: Perform a binary shift operation using two source registers
    *              where one is the shift amount and the other is the value to shift.
    *              Store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CC    1, %eax                 # %eax<- CC
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vBB
    movss       (rFP, %eax, 4), %xmm0   # %xmm0<- vCC
    movq        .LshiftMask, %xmm2
    pand        %xmm2, %xmm0            # %xmm0<- masked for the shift bits
    psrlq       %xmm0, %xmm1            # %xmm1<- shifted vBB
    cmpl        $0, 4(rFP, %edx, 4)    # check if we need to consider sign
    jl          .LOP_SHR_LONG_finish      # consider sign
    jmp         .LOP_SHR_LONG_final       # sign is fine, finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG: /* 0xa5 */
/* File: x86-atom/OP_USHR_LONG.S */
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
    * File: OP_USHR_LONG.S
    *
    * Code: Performs an unsigned shift right long operation. Uses no substitutions.
    *
    * For: ushr-long
    *
    * Description: Perform a binary shift operation using two source registers
    *              where one is the shift amount and the other is the value to shift.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_CC    1, %eax                 # %eax<- CC
    FETCH_BB    1, %edx                 # %edx<- BB
    movsd        .LshiftMask, %xmm2     # %xmm2<- mask for the shift bits
    movss       (rFP, %eax, 4), %xmm0   # %xmm0<- vCC
    pand        %xmm2, %xmm0            # %xmm0<- masked shift bits
    movsd       (rFP, %edx, 4), %xmm1   # %xmm1<- vBB
    psrlq       %xmm0, %xmm1            # %xmm1<- shifted vBB
    movsd       %xmm1, (rFP, rINST, 4)  # vAA<- shifted vBB
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT: /* 0xa6 */
/* File: x86-atom/OP_ADD_FLOAT.S */
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
    * File: OP_ADD_FLOAT.S
    */

/* File: x86-atom/binopF.S */
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
    * File: binopF.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-float, mul-float, sub-float
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<-vBB
    movss       (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    addss     %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movss       %xmm0, (rFP, rINST, 4)  # vAA<- %xmm0; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT: /* 0xa7 */
/* File: x86-atom/OP_SUB_FLOAT.S */
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
    * File: OP_SUB_FLOAT.S
    */

/* File: x86-atom/binopF.S */
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
    * File: binopF.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-float, mul-float, sub-float
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<-vBB
    movss       (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    subss     %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movss       %xmm0, (rFP, rINST, 4)  # vAA<- %xmm0; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT: /* 0xa8 */
/* File: x86-atom/OP_MUL_FLOAT.S */
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
    * File: OP_MUL_FLOAT.S
    */

/* File: x86-atom/binopF.S */
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
    * File: binopF.S
    *
    * Code: Generic 32-bit binary operation.  Provides an "instr" line to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-float, mul-float, sub-float
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<-vBB
    movss       (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    mulss %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movss       %xmm0, (rFP, rINST, 4)  # vAA<- %xmm0; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT: /* 0xa9 */
/* File: x86-atom/OP_DIV_FLOAT.S */
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
    * File: OP_DIV_FLOAT.S
    *
    * Code: Divides floats. Uses no substitutions.
    *
    * For: div-float
    *
    * Description: Divide operation on two source registers, storing
    *              the result in a destiniation register
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CC    1, %ecx                 # %ecx<- CC
    flds        (rFP, %eax, 4)          # floating point stack vBB
    fdivs       (rFP, %ecx, 4)          # divide double; vBB/vCC
    fstps       (rFP, rINST, 4)         # vAA<- result
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT: /* 0xaa */
/* File: x86-atom/OP_REM_FLOAT.S */
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
    * File: OP_REM_FLOAT.S
    *
    * Code: Computes the remainder of a division. Performs no substitutions.
    *
    * For: rem-float
    *
    * Description: Calls fmod to compute the remainder of the result of dividing a
    *              source register by a second, and stores the result in a
    *              destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    GET_VREG    %ecx                    # %ecx<- vBB
    GET_VREG    %edx                    # %edx<- vCC
    movl        %ecx, -8(%esp)          # push parameter float
    movl        %edx, -4(%esp)          # push parameter float
    lea         -8(%esp), %esp
    call        fmodf                   # call: (float x, float y)
                                        # return: float
    lea         8(%esp), %esp
    fstps       (rFP, rINST, 4)         # vAA<- remainder; return of fmod
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE: /* 0xab */
/* File: x86-atom/OP_ADD_DOUBLE.S */
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
    * File: OP_ADD_DOUBLE.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    addsd   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE: /* 0xac */
/* File: x86-atom/OP_SUB_DOUBLE.S */
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
    * File: OP_SUB_DOUBLE.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    subsd   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE: /* 0xad */
/* File: x86-atom/OP_MUL_DOUBLE.S */
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
    * File: OP_MUL_DOUBLE.S
    */

/* File: x86-atom/binopWide.S */
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
    * File: binopWide.S
    *
    * Code: Generic 64-bit binary operation.  Provides an "instr" variable to
    *       specify an instruction that performs "%xmm0 = %xmm0 op %xmm1"
    *
    * For: add-double, add-long, and-long, mul-double, or-long,
    *      sub-double, sub-long, xor-long
    *
    * Description: Perform a binary operation on two source registers
    *              and store the result in a destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, %ecx, 4), %xmm0   # %xmm0<- vBB
    movq        (rFP, %edx, 4), %xmm1   # %xmm1<- vCC
    mulsd   %xmm1, %xmm0                              # %xmm0<- vBB op vCC
    movq        %xmm0, (rFP, rINST, 4)  # vAA<- %ecx
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE: /* 0xae */
/* File: x86-atom/OP_DIV_DOUBLE.S */
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
    * File: OP_DIV_DOUBLE.S
    *
    * Code: Divides doubles. Uses no substitutions.
    *
    * For: div-double
    *
    * Description: Divide operation on two source registers, storing
    *              the result in a destination register
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    fldl        (rFP, %ecx, 4)          # floating point stack vBB
    fdivl       (rFP, %edx, 4)          # divide double; vBB/vCC
    fstpl       (rFP, rINST, 4)         # vAA<- result
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE: /* 0xaf */
/* File: x86-atom/OP_REM_DOUBLE.S */
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
    * File: OP_REM_DOUBLE.S
    *
    * Code: Computes the remainder of a division. Performs no substitutions.
    *
    * For: rem-double
    *
    * Description: Calls fmod to compute the remainder of the result of dividing a
    *              source register by a second, and stores the result in a
    *              destination register.
    *
    * Format: AA|op CC|BB (23x)
    *
    * Syntax: op vAA, vBB, vCC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CC    1, %edx                 # %edx<- CC
    movl        (rFP, %ecx, 4), %eax    # %eax<- vBBlo
    movl        %eax, -16(%esp)         # push parameter double lo
    movl        4(rFP, %ecx, 4), %eax   # %eax<- vBBhi
    movl        %eax, -12(%esp)         # push parameter double hi
    movl        (rFP, %edx, 4), %eax    # %eax<- vCClo
    movl        %eax, -8(%esp)          # push parameter double lo
    movl        4(rFP, %edx, 4), %eax   # %eax<- vCChi
    movl        %eax, -4(%esp)          # push parameter double hi
    lea         -16(%esp), %esp
    jmp         .LOP_REM_DOUBLE_break

/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_2ADDR: /* 0xb0 */
/* File: x86-atom/OP_ADD_INT_2ADDR.S */
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
    * File: OP_ADD_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    addl     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT_2ADDR: /* 0xb1 */
/* File: x86-atom/OP_SUB_INT_2ADDR.S */
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
    * File: OP_SUB_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    subl     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_2ADDR: /* 0xb2 */
/* File: x86-atom/OP_MUL_INT_2ADDR.S */
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
    * File: OP_MUL_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    imul     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_2ADDR: /* 0xb3 */
/* File: x86-atom/OP_DIV_INT_2ADDR.S */
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
    * File: OP_DIV_INT_2ADDR.S
    */

/* File: x86-atom/binopD2addr.S */
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
    * File: binopD2addr.S
    *
    * Code: 32-bit "/2addr" integer divde operation. If "div"
    *       is set, the code returns the quotient, else it returns
    *       the remainder. Also, a divide-by-zero check is done.
    *
    * For: div-int/2addr, rem-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, rINST             # rINST<- A, to be used as dest
    movl        rINST, %eax             # %eax<- A
    shr         $4, %ecx               # %ecx<- B
    GET_VREG    %eax                    # %eax<- vA
    GET_VREG    %ecx                    # %edx<- vB
    cmp         $0, %ecx               # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_DIV_INT_2ADDR_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_DIV_INT_2ADDR_break
.LOP_DIV_INT_2ADDR_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
     .if  1
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_DIV_INT_2ADDR_break2
    #FFETCH_ADV 1, %edx  # %ecx<- next instruction hi; fetch, advance
    #FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_2ADDR: /* 0xb4 */
/* File: x86-atom/OP_REM_INT_2ADDR.S */
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
    * File: OP_REM_INT_2ADDR.S
    */

/* File: x86-atom/binopD2addr.S */
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
    * File: binopD2addr.S
    *
    * Code: 32-bit "/2addr" integer divde operation. If "div"
    *       is set, the code returns the quotient, else it returns
    *       the remainder. Also, a divide-by-zero check is done.
    *
    * For: div-int/2addr, rem-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, rINST             # rINST<- A, to be used as dest
    movl        rINST, %eax             # %eax<- A
    shr         $4, %ecx               # %ecx<- B
    GET_VREG    %eax                    # %eax<- vA
    GET_VREG    %ecx                    # %edx<- vB
    cmp         $0, %ecx               # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_REM_INT_2ADDR_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_REM_INT_2ADDR_break
.LOP_REM_INT_2ADDR_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
     .if  0
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_REM_INT_2ADDR_break2
    #FFETCH_ADV 1, %edx  # %ecx<- next instruction hi; fetch, advance
    #FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_2ADDR: /* 0xb5 */
/* File: x86-atom/OP_AND_INT_2ADDR.S */
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
    * File: OP_AND_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    andl     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_2ADDR: /* 0xb6 */
/* File: x86-atom/OP_OR_INT_2ADDR.S */
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
    * File: OP_OR_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    or %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_2ADDR: /* 0xb7 */
/* File: x86-atom/OP_XOR_INT_2ADDR.S */
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
    * File: OP_XOR_INT_2ADDR.S
    */

/* File: x86-atom/binop2addr.S */
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
    * File: binop2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%ecx = %ecx op %edx".
    *
    * For: add-int/2addr, and-int/2addr, mul-int/2addr, or-int/2addr,
    *      sub-int/2addr, xor-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %ecx             # %ecx<- A
    FFETCH_ADV  1, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vB
    GET_VREG    %ecx                    # %ecx<- vA
    xor     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp




/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_2ADDR: /* 0xb8 */
/* File: x86-atom/OP_SHL_INT_2ADDR.S */
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
    * File: OP_SHL_INT_2ADDR.S
    */

/* File: x86-atom/binopS2addr.S */
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
    * File: binopS2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%edx = %edx op %cl".
    *
    * For: shl-int/2addr, shr-int/2addr, ushr-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %edx             # %edx<- A
    FFETCH_ADV  1, %eax                 # %ecx<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    GET_VREG    %edx                    # %edx<- vA
    sal     %cl, %edx                              # %edx<- vA op vB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_2ADDR: /* 0xb9 */
/* File: x86-atom/OP_SHR_INT_2ADDR.S */
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
    * File: OP_SHR_INT_2ADDR.S
    */

/* File: x86-atom/binopS2addr.S */
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
    * File: binopS2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%edx = %edx op %cl".
    *
    * For: shl-int/2addr, shr-int/2addr, ushr-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %edx             # %edx<- A
    FFETCH_ADV  1, %eax                 # %ecx<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    GET_VREG    %edx                    # %edx<- vA
    sar     %cl, %edx                              # %edx<- vA op vB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_2ADDR: /* 0xba */
/* File: x86-atom/OP_USHR_INT_2ADDR.S */
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
    * File: OP_USHR_INT_2ADDR.S
    */

/* File: x86-atom/binopS2addr.S */
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
    * File: binopS2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%edx = %edx op %cl".
    *
    * For: shl-int/2addr, shr-int/2addr, ushr-int/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    movl        rINST, %edx             # %edx<- A
    FFETCH_ADV  1, %eax                 # %ecx<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    GET_VREG    %edx                    # %edx<- vA
    shr     %cl, %edx                              # %edx<- vA op vB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  1, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG_2ADDR: /* 0xbb */
/* File: x86-atom/OP_ADD_LONG_2ADDR.S */
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
    * File: OP_ADD_LONG_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    paddq   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG_2ADDR: /* 0xbc */
/* File: x86-atom/OP_SUB_LONG_2ADDR.S */
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
    * File: OP_SUB_LONG_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    psubq    %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG_2ADDR: /* 0xbd */
/* File: x86-atom/OP_MUL_LONG_2ADDR.S */
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
    * File: OP_MUL_LONG_2ADDR.S
    *
    * Code:  64-bit integer multiply
    *
    * For: mul-long/2addr
    *
    * Description: Multiply two sources registers and store the result
    *              in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
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

    movl        rINST, %edx             # %edx<- BA+
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movl        %edx, sReg0             # sReg0<- A
    jmp         .LOP_MUL_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG_2ADDR: /* 0xbe */
/* File: x86-atom/OP_DIV_LONG_2ADDR.S */
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
    * File: OP_DIV_LONG_2ADDR.S
    */

/* File: x86-atom/binopDivRemLong2Addr.S */
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
    * File: binopDivRemLong2Addr.S
    *
    * Code: 64-bit "/2addr" long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long/2addr, rem-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    and         $15, rINST             # rINST<- A
    movl        (rFP, %edx, 4), %eax    # %eax<- vB
    movl        %eax, -12(%esp)         # push arg vB
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vB+1
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    movl        %ecx, -8(%esp)          # push arg vB+1
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vA,vA+1
    jmp         .LOP_DIV_LONG_2ADDR_break


/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG_2ADDR: /* 0xbf */
/* File: x86-atom/OP_REM_LONG_2ADDR.S */
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
    * File: OP_REM_LONG_2ADDR.S
    */

/* File: x86-atom/binopDivRemLong2Addr.S */
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
    * File: binopDivRemLong2Addr.S
    *
    * Code: 64-bit "/2addr" long divide operation. Variable
    *       "func" defines the function called to do the operation.
    *
    * For: div-long/2addr, rem-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */


    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    and         $15, rINST             # rINST<- A
    movl        (rFP, %edx, 4), %eax    # %eax<- vB
    movl        %eax, -12(%esp)         # push arg vB
    movl        4(rFP, %edx, 4), %ecx   # %ecx<- vB+1
    or          %ecx, %eax              # check for divide by zero
    je          common_errDivideByZero  # handle divide by zero
    movl        %ecx, -8(%esp)          # push arg vB+1
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vA,vA+1
    jmp         .LOP_REM_LONG_2ADDR_break


/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG_2ADDR: /* 0xc0 */
/* File: x86-atom/OP_AND_LONG_2ADDR.S */
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
    * File: OP_AND_LONG_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    pand   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG_2ADDR: /* 0xc1 */
/* File: x86-atom/OP_OR_LONG_2ADDR.S */
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
    * File: OP_OR_LONG_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    por %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG_2ADDR: /* 0xc2 */
/* File: x86-atom/OP_XOR_LONG_2ADDR.S */
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
    * File: OP_XOR_LONG_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    pxor   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG_2ADDR: /* 0xc3 */
/* File: x86-atom/OP_SHL_LONG_2ADDR.S */
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
    * File: OP_SHL_LONG_2ADDR.S
    *
    * Code: Performs a shift left long. Uses no substitutions.
    *
    * For: shl-long/2addr
    *
    * Description: Perform a binary shift operation using two source registers
    *              where the fist is the value to shift and the second is the
    *              shift amount. Store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movss       (rFP, %edx, 4),  %xmm0  # %xmm0<- vB
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vA
    movq        .LshiftMask, %xmm2      # %xmm2<- mask for the shift bits
    pand        %xmm2, %xmm0            # %xmm0<- masked shift bits
    psllq       %xmm0, %xmm1            # %xmm1<- shifted vA
    movq        %xmm1, (rFP, rINST, 4)  # vA<- shifted vA
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG_2ADDR: /* 0xc4 */
/* File: x86-atom/OP_SHR_LONG_2ADDR.S */
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
    * File: OP_SHR_LONG_2ADDR.S
    *
    * Code: Performs a shift left long
    *
    * For: shl-long/2addr
    *
    * Description: Perform a binary shift operation using two source registers
    *              where the fist is the value to shift and the second is the
    *              shift amount. Store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- BA
    movss       (rFP, %edx, 4),  %xmm0  # %xmm0<- vB
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vA
    movq        .LshiftMask, %xmm2
    pand        %xmm2, %xmm0            # %xmm0<- masked for the shift bits
    psrlq       %xmm0, %xmm1            # %xmm1<- shifted vBB
    cmpl        $0, 4(rFP, rINST, 4)   # check if we need to consider sign
    jl          .LOP_SHR_LONG_2ADDR_finish      # consider sign
    jmp         .LOP_SHR_LONG_2ADDR_final       # sign is fine, finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG_2ADDR: /* 0xc5 */
/* File: x86-atom/OP_USHR_LONG_2ADDR.S */
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
    * File: OP_USHR_LONG_2ADDR.S
    *
    * Code: Performs an unsigned shift right long operation. Uses no substiutions.
    *
    * For: ushr-long/2addr
    *
    * Description: Perform a binary shift operation using two source registers
    *              where the fist is the value to shift and the second is the
    *              shift amount. Store the result in the first source register.
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    movq        .LshiftMask, %xmm2      # %xmm2<- mask for the shift bits
    movss       (rFP, %edx, 4),  %xmm0  # %xmm0<- vB
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vA
    pand        %xmm2, %xmm0            # %xmm0<- masked shift bits
    psrlq       %xmm0, %xmm1            # %xmm1<- shifted vA
    movq        %xmm1, (rFP, rINST, 4)  # vA<- shifted vA
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT_2ADDR: /* 0xc6 */
/* File: x86-atom/OP_ADD_FLOAT_2ADDR.S */
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
    * File: OP_ADD_FLOAT_2ADDR.S
    */

/* File: x86-atom/binopF2addr.S */
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
    * File: binopF2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0 = %xmm0 op %xmm1".
    *
    * For: add-float/2addr, mul-float/2addr, sub-float/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, %ecx              # %ecx<- A
    shr         $4, rINST              # rINST<- B
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<- vA
    movss       (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    addss     %xmm1, %xmm0                              # %xmm0<- vA op vB
    movss       %xmm0, (rFP, %ecx, 4)   # vA<- %xmm0; result
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT_2ADDR: /* 0xc7 */
/* File: x86-atom/OP_SUB_FLOAT_2ADDR.S */
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
    * File: OP_SUB_FLOAT_2ADDR.S
    */

/* File: x86-atom/binopF2addr.S */
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
    * File: binopF2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0 = %xmm0 op %xmm1".
    *
    * For: add-float/2addr, mul-float/2addr, sub-float/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, %ecx              # %ecx<- A
    shr         $4, rINST              # rINST<- B
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<- vA
    movss       (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    subss     %xmm1, %xmm0                              # %xmm0<- vA op vB
    movss       %xmm0, (rFP, %ecx, 4)   # vA<- %xmm0; result
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT_2ADDR: /* 0xc8 */
/* File: x86-atom/OP_MUL_FLOAT_2ADDR.S */
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
    * File: OP_MUL_FLOAT_2ADDR.S
    */

/* File: x86-atom/binopF2addr.S */
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
    * File: binopF2addr.S
    *
    * Code: Generic 32-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0 = %xmm0 op %xmm1".
    *
    * For: add-float/2addr, mul-float/2addr, sub-float/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, %ecx              # %ecx<- A
    shr         $4, rINST              # rINST<- B
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    movss       (rFP, %ecx, 4), %xmm0   # %xmm0<- vA
    movss       (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    mulss %xmm1, %xmm0                              # %xmm0<- vA op vB
    movss       %xmm0, (rFP, %ecx, 4)   # vA<- %xmm0; result
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT_2ADDR: /* 0xc9 */
/* File: x86-atom/OP_DIV_FLOAT_2ADDR.S */
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
    * File: OP_DIV_FLOAT_2ADDR.S
    *
    * Code: Divides floats. Uses no substitutions.
    *
    * For: div-float/2addr
    *
    * Description: Divide operation on two source registers, storing
    *              the result in the first source reigster
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %ecx             # %ecx<- BA
    andl        $15, %ecx              # %ecx<- A
    shr         $4, rINST              # rINST<- B
    flds        (rFP, %ecx, 4)          # %xmm0<- vA
    fdivs       (rFP, rINST, 4)         # divide double; vA/vB
    fstps       (rFP, %ecx, 4)          # vAA<- result
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT_2ADDR: /* 0xca */
/* File: x86-atom/OP_REM_FLOAT_2ADDR.S */
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
    * File: OP_REM_FLOAT_2ADDR.S
    *
    * Code: Computes the remainder of a division. Performs no substitutions.
    *
    * For: rem-float/2addr
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
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB
    movl        (rFP, rINST, 4), %ecx   # %ecx<- vA
    movl        %ecx, -8(%esp)          # push parameter vA
    movl        %edx, -4(%esp)          # push parameter vB
    lea         -8(%esp), %esp
    call        fmodf                   # call: (float x, float y)
                                        # return: float
    lea         8(%esp), %esp
    fstps       (rFP, rINST, 4)
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE_2ADDR: /* 0xcb */
/* File: x86-atom/OP_ADD_DOUBLE_2ADDR.S */
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
    * File: OP_ADD_DOUBLE_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    addsd   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE_2ADDR: /* 0xcc */
/* File: x86-atom/OP_SUB_DOUBLE_2ADDR.S */
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
    * File: OP_SUB_DOUBLE_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    subsd   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE_2ADDR: /* 0xcd */
/* File: x86-atom/OP_MUL_DOUBLE_2ADDR.S */
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
    * File: OP_MUL_DOUBLE_2ADDR.S
    */

/* File: x86-atom/binopWide2addr.S */
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
    * File: binopWide2addr.S
    *
    * Code: Generic 64-bit "/2addr" binary operation.  Provides an
    *       "instr" line to specify an instruction that performs
    *       "%xmm0= %xmm0 op %xmm1".
    *
    * For: add-double/2addr, add-long/2addr, and-long/2addr, mul-long/2addr,
    *      or-long/2addr, sub-double/2addr, sub-long/2addr, xor-long/2addr
    *
    * Description: Perform a binary operation on two sources registers
    *              and store the result in the first source register
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, rINST              # rINST<- B
    andl        $15, %edx              # %edx<- A
    movq        (rFP, rINST, 4), %xmm1  # %xmm1<- vB
    movq        (rFP, %edx, 4), %xmm0   # %xmm0<- vA
    mulsd   %xmm1, %xmm0                              # %xmm0<- vA op vB
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; result
    FINISH      1                       # jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE_2ADDR: /* 0xce */
/* File: x86-atom/OP_DIV_DOUBLE_2ADDR.S */
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
    * File: OP_DIV_DOUBLE_2ADDR.S
    *
    * Code: Divides doubles. Uses no substitutions.
    *
    * For: div-double/2addr
    *
    * Description: Divide operation on two source registers, storing
    *              the result in the first source reigster
    *
    * Format: B|A|op (12x)
    *
    * Syntax: op vA, vB
    */

    movl        rINST, %edx             # %edx<- BA
    andl        $15, %edx              # %edx<- A
    shr         $4, rINST              # rINST<- B
    fldl        (rFP, %edx, 4)          # %xmm0<- vA
    fdivl       (rFP, rINST, 4)         # divide double; vA/vB
    fstpl       (rFP, %edx, 4)          # vAA<- result
    FINISH      1                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE_2ADDR: /* 0xcf */
/* File: x86-atom/OP_REM_DOUBLE_2ADDR.S */
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
    and         $15, rINST             # rINST<- A
    shr         $4, %edx               # %edx<- B
    movl        (rFP, rINST, 4), %eax   # %eax<- vAlo
    movl        %eax, -20(%esp)         # push parameter vAAlo
    movl        4(rFP, rINST, 4), %eax  # %eax<- vAhi
    movl        %eax, -16(%esp)         # push parameter vAAhi
    movl        (rFP, %edx, 4), %eax    # %eax<- vBlo
    movl        %eax, -12(%esp)         # push parameter vBBlo
    movl        4(rFP, %edx, 4), %eax   # %eax<- vBhi
    movl        %eax, -8(%esp)          # push parameter vBBhi
    lea         -20(%esp), %esp
    jmp         .LOP_REM_DOUBLE_2ADDR_break

/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT16: /* 0xd0 */
/* File: x86-atom/OP_ADD_INT_LIT16.S */
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
    * File: OP_ADD_INT_LIT16.S
    */

/* File: x86-atom/binopLit16.S */
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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    addl     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT: /* 0xd1 */
/* File: x86-atom/OP_RSUB_INT.S */
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
    * File: OP_RSUB_INT.S
    *
    * Code: 32-bit reverse-subtraction. Uses no substitutions.
    *
    * For: rsub-int
    *
    * Description: Perform a reverse subtraction on a register and a
    *              signed extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    GET_VREG    %ecx                    # %ecx<- vB
    subl        %ecx, %edx              # %edx<- +CCCC sub vB
    SET_VREG    %edx, rINST             # vA<- %edx; result
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT16: /* 0xd2 */
/* File: x86-atom/OP_MUL_INT_LIT16.S */
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
    * File: OP_MUL_INT_LIT16.S
    */

/* File: x86-atom/binopLit16.S */
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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    imul     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT16: /* 0xd3 */
/* File: x86-atom/OP_DIV_INT_LIT16.S */
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
    * File: OP_DIV_INT_LIT16.S
    */

/* File: x86-atom/binopDLit16.S */
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
    * File: binopDLit16.S
    *
    * Code: 32-bit "lit16" divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int/lit16, rem-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */


    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    FETCHs      1, %ecx                 # %ecx<- +CCCC, sign-extended literal
    cmp         $0, %ecx               # check for divide by zero
    GET_VREG    %eax                    # %eax<- vB
    je          common_errDivideByZero  # handle divide by zero
    andl        $15, rINST             # rINST<- A
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_DIV_INT_LIT16_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_DIV_INT_LIT16_break
.LOP_DIV_INT_LIT16_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    #FFETCH_ADV 2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    .if  1
    SET_VREG    %eax rINST              # vA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vA<- %edx (remainder)
    .endif
    jmp         .LOP_DIV_INT_LIT16_break2



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT16: /* 0xd4 */
/* File: x86-atom/OP_REM_INT_LIT16.S */
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
    * File: OP_REM_INT_LIT16.S
    */

/* File: x86-atom/binopDLit16.S */
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
    * File: binopDLit16.S
    *
    * Code: 32-bit "lit16" divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int/lit16, rem-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */


    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    FETCHs      1, %ecx                 # %ecx<- +CCCC, sign-extended literal
    cmp         $0, %ecx               # check for divide by zero
    GET_VREG    %eax                    # %eax<- vB
    je          common_errDivideByZero  # handle divide by zero
    andl        $15, rINST             # rINST<- A
    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_REM_INT_LIT16_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_REM_INT_LIT16_break
.LOP_REM_INT_LIT16_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    #FFETCH_ADV 2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    .if  0
    SET_VREG    %eax rINST              # vA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vA<- %edx (remainder)
    .endif
    jmp         .LOP_REM_INT_LIT16_break2



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT16: /* 0xd5 */
/* File: x86-atom/OP_AND_INT_LIT16.S */
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
    * File: OP_AND_INT_LIT16.S
    */

/* File: x86-atom/binopLit16.S */
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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    andl     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT16: /* 0xd6 */
/* File: x86-atom/OP_OR_INT_LIT16.S */
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
    * File: OP_OR_INT_LIT16.S
    */

/* File: x86-atom/binopLit16.S */
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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    or %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT16: /* 0xd7 */
/* File: x86-atom/OP_XOR_INT_LIT16.S */
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
    * File: OP_XOR_INT_LIT16.S
    */

/* File: x86-atom/binopLit16.S */
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
    * File: binopLit16.S
    *
    * Code: 32-bit "lit16" operation. Provides an "instr" line to
    *       specify an instruction that performs "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit16, and-int/lit16, mul-int/lit16, or-int/lit16
    *      xor-int/lit16
    *
    * Description: Perform a binary operation on a register and a
    *              sign extended 16-bit literal value and store the
    *              result in a destination register.
    *
    * Format: B|A|op CCCC (22s)
    *
    * Syntax: op vA, vB, #+CCCC
    */

    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    andl        $15, rINST             # rINST<- A
    FETCHs      1, %edx                 # %edx<- +CCCC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vB
    xor     %edx, %ecx                              # %ecx<- vA op vB
    SET_VREG    %ecx, rINST             # vA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT8: /* 0xd8 */
/* File: x86-atom/OP_ADD_INT_LIT8.S */
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
    * File: OP_ADD_INT_LIT8.S
    */

/* File: x86-atom/binopLit8.S */
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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    addl     %edx, %ecx                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT_LIT8: /* 0xd9 */
/* File: x86-atom/OP_RSUB_INT_LIT8.S */
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
    * File: OP_RSUB_INT_LIT8.S
    *
    * Code: 32-bit reverse-subtraction. Uses no substitutions.
    *
    * For: rsub-int/lit8
    *
    * Description: Perform a reverse subtraction on a register and a
    *              signed extended 8-bit literal value.
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    GET_VREG    %ecx                    # %ecx<- vBB
    sub         %ecx, %edx              # %edx<- +CC sub vBB
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FINISH      2                       # jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT8: /* 0xda */
/* File: x86-atom/OP_MUL_INT_LIT8.S */
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
    * File: OP_MUL_INT_LIT8.S
    */

/* File: x86-atom/binopLit8.S */
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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    imul     %edx, %ecx                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT8: /* 0xdb */
/* File: x86-atom/OP_DIV_INT_LIT8.S */
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
    * File: OP_DIV_INT_LIT8.S
    */

/* File: x86-atom/binopDLit8.S */
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
    * File: binopDLit8.S
    *
    * Code: 32-bit "lit8" divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int/lit8, rem-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signe extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */


    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    cmp         $0, %ecx               # check for divide by zero
    GET_VREG    %eax                    # %eax<- vBB
    je          common_errDivideByZero  # handle divide by zero

    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_DIV_INT_LIT8_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_DIV_INT_LIT8_break
.LOP_DIV_INT_LIT8_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    #FFETCH_ADV 2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    .if  1
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_DIV_INT_LIT8_break2


/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT8: /* 0xdc */
/* File: x86-atom/OP_REM_INT_LIT8.S */
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
    * File: OP_REM_INT_LIT8.S
    */

/* File: x86-atom/binopDLit8.S */
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
    * File: binopDLit8.S
    *
    * Code: 32-bit "lit8" divide operation. If "div" is set, the code
    *       returns the quotient, else it returns the remainder.
    *       Also, a divide-by-zero check is done.
    *
    * For: div-int/lit8, rem-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signe extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */


    FETCH_BB    1, %eax                 # %eax<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    cmp         $0, %ecx               # check for divide by zero
    GET_VREG    %eax                    # %eax<- vBB
    je          common_errDivideByZero  # handle divide by zero

    cmpl        $-1, %ecx              # handle -1 special case divide error
    jne         .LOP_REM_INT_LIT8_noerror
    cmpl        $0x80000000,%eax       # handle min int special case divide error
    je         .LOP_REM_INT_LIT8_break
.LOP_REM_INT_LIT8_noerror:
    cdq                                 # sign-extend %eax to %edx
    idiv        %ecx                    # divide %edx:%eax by %ecx
    #FFETCH_ADV 2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    .if  0
    SET_VREG    %eax rINST              # vAA<- %eax (quotient)
    .else
    SET_VREG    %edx rINST              # vAA<- %edx (remainder)
    .endif
    jmp         .LOP_REM_INT_LIT8_break2


/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT8: /* 0xdd */
/* File: x86-atom/OP_AND_INT_LIT8.S */
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
    * File: OP_AND_INT_LIT8.S
    */

/* File: x86-atom/binopLit8.S */
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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    andl     %edx, %ecx                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT8: /* 0xde */
/* File: x86-atom/OP_OR_INT_LIT8.S */
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
    * File: OP_OR_INT_LIT8.S
    */

/* File: x86-atom/binopLit8.S */
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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    or %edx, %ecx                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT8: /* 0xdf */
/* File: x86-atom/OP_XOR_INT_LIT8.S */
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
    * File: OP_XOR_INT_LIT8.S
    */

/* File: x86-atom/binopLit8.S */
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
    * File: binopLit8.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs  "%ecx = %ecx op %edx"
    *
    *
    * For: add-int/lit8, and-int/lit8, mul-int/lit8, or-int/lit8
    *      xor-int/lit8
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %ecx                 # %ecx<- BB
    FETCH_CCs   1, %edx                 # %edx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %ecx                    # %ecx<- vBB
    xor     %edx, %ecx                              # %ecx<- vBB op +CC
    SET_VREG    %ecx, rINST             # vAA<- %ecx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_LIT8: /* 0xe0 */
/* File: x86-atom/OP_SHL_INT_LIT8.S */
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
    * File: OP_SHL_INT_LIT8.S
    */

/* File: x86-atom/binopLit8S.S */
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
    * File: binopLit8S.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs "%edx = %edx op %cl"
    *
    *
    * For: shl-int/lit8, shr-int/lit8, ushr-int/lit8
    *
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    sal     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_LIT8: /* 0xe1 */
/* File: x86-atom/OP_SHR_INT_LIT8.S */
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
    * File: OP_SHR_INT_LIT8.S
    */

/* File: x86-atom/binopLit8S.S */
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
    * File: binopLit8S.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs "%edx = %edx op %cl"
    *
    *
    * For: shl-int/lit8, shr-int/lit8, ushr-int/lit8
    *
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    sar     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_LIT8: /* 0xe2 */
/* File: x86-atom/OP_USHR_INT_LIT8.S */
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
    * File: OP_USHR_INT_LIT8.S
    */

/* File: x86-atom/binopLit8S.S */
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
    * File: binopLit8S.S
    *
    * Code: 32-bit "lit8" divide operation. Provides an "instr" line
    *       to specify an instruction that performs "%edx = %edx op %cl"
    *
    *
    * For: shl-int/lit8, shr-int/lit8, ushr-int/lit8
    *
    *
    * Description: Perform a binary operation on a register and a
    *              signed extended 8-bit literal value
    *
    * Format: AA|op CC|BB (22b)
    *
    * Syntax: op vAA, vBB, #+CC
    */

    FETCH_BB    1, %edx                 # %edx<- BB
    FETCH_CCs   1, %ecx                 # %ecx<- +CC, sign-extended literal
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    %edx                    # %edx<- vBB
    shr     %cl, %edx                              # %edx<- vBB op +CC
    SET_VREG    %edx, rINST             # vAA<- %edx; result
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_IGET_VOLATILE: /* 0xe3 */
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
    call        dvmMterp_OP_IGET_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_IPUT_VOLATILE: /* 0xe4 */
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
    call        dvmMterp_OP_IPUT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SGET_VOLATILE: /* 0xe5 */
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
    call        dvmMterp_OP_SGET_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SPUT_VOLATILE: /* 0xe6 */
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
    call        dvmMterp_OP_SPUT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT_VOLATILE: /* 0xe7 */
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
    call        dvmMterp_OP_IGET_OBJECT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE_VOLATILE: /* 0xe8 */
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
    call        dvmMterp_OP_IGET_WIDE_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE_VOLATILE: /* 0xe9 */
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
    call        dvmMterp_OP_IPUT_WIDE_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SGET_WIDE_VOLATILE: /* 0xea */
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
    call        dvmMterp_OP_SGET_WIDE_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SPUT_WIDE_VOLATILE: /* 0xeb */
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
    call        dvmMterp_OP_SPUT_WIDE_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_BREAKPOINT: /* 0xec */
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
    call        dvmMterp_OP_BREAKPOINT      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_THROW_VERIFICATION_ERROR: /* 0xed */
/* File: x86-atom/OP_THROW_VERIFICATION_ERROR.S */
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

/* ------------------------------ */
    .balign 64
.L_OP_EXECUTE_INLINE: /* 0xee */
/* File: x86-atom/OP_EXECUTE_INLINE.S */
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
    * File: OP_EXECUTE_INLINE.S
    *
    * Code: Executes a "native inline" instruction. Uses no substitutions.
    *
    * For: execute-inline
    *
    * Description: Executes a "native inline" instruction. This instruction
    *              is generated by the optimizer.
    *
    * Format:
    *
    * Syntax: vAA, {vC, vD, vE, vF}, inline@BBBB
    */

    FETCH       1, %ecx                 # %ecx<- BBBB
    movl        rGLUE, %eax             # %eax<- MterpGlue pointer
    addl        $offGlue_retval, %eax  # %eax<- &glue->retval
    EXPORT_PC
    shr         $4, rINST              # rINST<- B
    movl        %eax, -8(%esp)          # push parameter glue->retval
    lea         -24(%esp), %esp
    jmp         .LOP_EXECUTE_INLINE_continue

/* ------------------------------ */
    .balign 64
.L_OP_EXECUTE_INLINE_RANGE: /* 0xef */
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
    call        dvmMterp_OP_EXECUTE_INLINE_RANGE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_EMPTY: /* 0xf0 */
/* File: x86-atom/OP_INVOKE_DIRECT_EMPTY.S */
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
    * File: OP_INVOKE_DIRECT_EMPTY.S
    *
    * Code: Used as a no-op. Uses no substitutions.
    *
    * For: invoke-direct-empty
    *
    * Format: B|A|op CCCC G|F|E|D (35c)
    */

    FINISH 3

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_F1: /* 0xf1 */
/* File: x86-atom/OP_UNUSED_F1.S */
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
    * File: OP_UNUSED_F1.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort


/* ------------------------------ */
    .balign 64
.L_OP_IGET_QUICK: /* 0xf2 */
/* File: x86-atom/OP_IGET_QUICK.S */
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
    * File: OP_IGET_QUICK.S
    *
    * Code: Optimization for iget
    *
    * For: iget-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB; object to operate on
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    cmp         $0, %eax               # check if object is null
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %edx                 # %eax<- next instruction hi; fetch, advance
    movl        (%ecx, %eax), %eax      # %eax<- object field
    SET_VREG    %eax, rINST             # fp[A]<- %eax
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE_QUICK: /* 0xf3 */
/* File: x86-atom/OP_IGET_WIDE_QUICK.S */
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
    * File: OP_IGET_WIDE_QUICK.S
    *
    * Code: Optimization for iget
    *
    * For: iget/wide-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB; object to operate on
    cmp         $0, %edx               # check if object is null
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    je          common_errNullObject    # handle null object
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    movq        (%ecx, %edx), %xmm0     # %xmm0<- object field
    movq        %xmm0, (rFP, rINST, 4)  # fp[A]<- %xmm0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT_QUICK: /* 0xf4 */
/* File: x86-atom/OP_IGET_OBJECT_QUICK.S */
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
    * File: OP_IGET_OBJECT_QUICK.S
    */

/* File: x86-atom/OP_IGET_QUICK.S */
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
    * File: OP_IGET_QUICK.S
    *
    * Code: Optimization for iget
    *
    * For: iget-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB; object to operate on
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    cmp         $0, %eax               # check if object is null
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %edx                 # %eax<- next instruction hi; fetch, advance
    movl        (%ecx, %eax), %eax      # %eax<- object field
    SET_VREG    %eax, rINST             # fp[A]<- %eax
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_QUICK: /* 0xf5 */
/* File: x86-atom/OP_IPUT_QUICK.S */
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
    * File: OP_IPUT_QUICK.S
    * Code: Optimization for iput
    *
    * For: iput-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB; object to operate on
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    cmp         $0, %eax               # check if object is null
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl        rINST, (%eax, %ecx)     # object field<- vA
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE_QUICK: /* 0xf6 */
/* File: x86-atom/OP_IPUT_WIDE_QUICK.S */
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
    * File: OP_IPUT_WIDE_QUICK.S
    *
    * Code: Optimization for iput
    *
    * For: iput/wide-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB; object to operate on
    cmp         $0, %edx               # check if object is null
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- fp[A]
    movq        %xmm0, (%edx, %ecx)     # object field<- %xmm0; fp[A]
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT_QUICK: /* 0xf7 */
/* File: x86-atom/OP_IPUT_OBJECT_QUICK.S */
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
    * File: OP_IPUT_QUICK.S
    * Code: Optimization for iput
    *
    * For: iput-quick
    *
    * Format: B|A|op CCCC (22c)
    *
    * Syntax: op vA, vB, offset@CCCC
    */

    movl        rINST, %eax             # %eax<- BA
    shr         $4, %eax               # %eax<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %eax                    # %eax<- vB; object to operate on
    FETCH       1, %ecx                 # %ecx<- CCCC; field byte offset
    cmp         $0, %eax               # check if object is null
    je          common_errNullObject    # handle null object
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl        rINST, (%eax, %ecx)     # object field<- vA
    testl       rINST, rINST            # did we write a null object
    je          1f
    movl        rGLUE, %ecx             # get glue
    movl        offGlue_cardTable(%ecx), %ecx # get card table base
    shrl        $GC_CARD_SHIFT, %eax   # get gc card index
    movb        %cl, (%eax, %ecx)       # mark gc card in table
1:
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK: /* 0xf8 */
/* File: x86-atom/OP_INVOKE_VIRTUAL_QUICK.S */
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
    * File: OP_INVOKE_VIRTUAL_QUICK.S
    *
    * Code: Optimization for invoke-virtual and invoke-virtual/range
    *
    * For: invoke-virtual/quick, invoke-virtual/quick-range
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if (!0)
    and         $15, %edx              # %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    GET_VREG    %edx                    # %edx<- "this" ptr
    cmp         $0, %edx               # %edx<- check for null "this"
    EXPORT_PC                           # must export pc for invoke
    je          common_errNullObject
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodNoRange # invoke method common code

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK_RANGE: /* 0xf9 */
/* File: x86-atom/OP_INVOKE_VIRTUAL_QUICK_RANGE.S */
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
    * File: OP_INVOKE_VIRTUAL_QUICK_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_VIRTUAL_QUICK.S */
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
    * File: OP_INVOKE_VIRTUAL_QUICK.S
    *
    * Code: Optimization for invoke-virtual and invoke-virtual/range
    *
    * For: invoke-virtual/quick, invoke-virtual/quick-range
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    .if (!1)
    and         $15, %edx              # %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    GET_VREG    %edx                    # %edx<- "this" ptr
    cmp         $0, %edx               # %edx<- check for null "this"
    EXPORT_PC                           # must export pc for invoke
    je          common_errNullObject
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodRange # invoke method common code


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK: /* 0xfa */
/* File: x86-atom/OP_INVOKE_SUPER_QUICK.S */
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
    * File: OP_INVOKE_SUPER_QUICK.S
    *
    * Code: Optimization for invoke-super and invoke-super/range
    *
    * For: invoke-super/quick, invoke-super/quick-range
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %eax # %eax<- glue->method
    .if         (!0)
    and         $15, %edx              #  %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        offClassObject_super(%eax), %eax # %eax<- glue->method->clazz->super
    EXPORT_PC                           # must export for invoke
    movl        offClassObject_vtable(%eax), %eax # %edx<- glue->method->clazz->super->vtable
    cmp         $0, (rFP, %edx, 4)     # check for null object
    movl        (%eax, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    je          common_errNullObject    # handle null object
    jmp         common_invokeMethodNoRange # invoke method common code

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK_RANGE: /* 0xfb */
/* File: x86-atom/OP_INVOKE_SUPER_QUICK_RANGE.S */
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
    * File: OP_INVOKE_SUPER_QUICK_RANGE.S
    */

/* File: x86-atom/OP_INVOKE_SUPER_QUICK.S */
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
    * File: OP_INVOKE_SUPER_QUICK.S
    *
    * Code: Optimization for invoke-super and invoke-super/range
    *
    * For: invoke-super/quick, invoke-super/quick-range
    */


    FETCH       2, %edx                 # %edx<- GFED or CCCC
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %eax # %eax<- glue->method
    .if         (!1)
    and         $15, %edx              #  %edx<- D if not range
    .endif
    FETCH       1, %ecx                 # %ecx<- method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        offClassObject_super(%eax), %eax # %eax<- glue->method->clazz->super
    EXPORT_PC                           # must export for invoke
    movl        offClassObject_vtable(%eax), %eax # %edx<- glue->method->clazz->super->vtable
    cmp         $0, (rFP, %edx, 4)     # check for null object
    movl        (%eax, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    je          common_errNullObject    # handle null object
    jmp         common_invokeMethodRange # invoke method common code


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT_VOLATILE: /* 0xfc */
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
    call        dvmMterp_OP_IPUT_OBJECT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SGET_OBJECT_VOLATILE: /* 0xfd */
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
    call        dvmMterp_OP_SGET_OBJECT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_SPUT_OBJECT_VOLATILE: /* 0xfe */
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
    call        dvmMterp_OP_SPUT_OBJECT_VOLATILE      # call c-based implementation
    lea         4(%esp), %esp
    LOAD_PC_FP_FROM_GLUE                # restore program counter and frame pointer
    FINISH_A                            # jump to next instruction
/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FF: /* 0xff */
/* File: x86-atom/OP_UNUSED_FF.S */
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
    * File: OP_UNUSED_FF.S
    */

/* File: x86-atom/unused.S */
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
    * File: unused.S
    *
    * Code: Common code for unused bytecodes. Uses no subtitutions.
    *
    * For: all unused bytecodes
    *
    * Description: aborts if executed.
    *
    * Format: ØØ|op (10x)
    *
    * Syntax: op
    */

    call        common_abort



    .balign 64
    .size   dvmAsmInstructionStart, .-dvmAsmInstructionStart
    .global dvmAsmInstructionEnd
dvmAsmInstructionEnd:

/*
 * ===========================================================================
 *  Sister implementations
 * ===========================================================================
 */
    .global dvmAsmSisterStart
    .type   dvmAsmSisterStart, %function
    .text
    .balign 4
dvmAsmSisterStart:

/* continuation for OP_CONST_STRING */


   /*
    * Continuation if the Class has not yet been resolved.
    *  %ecx: BBBB (Class ref)
    *  need: target register
    */

.LOP_CONST_STRING_resolve:
    EXPORT_PC
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %ecx, -4(%esp)          # push parameter class ref
    movl        %edx, -8(%esp)          # push parameter glue->method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveString        # resolve string reference
                                        # call: (const ClassObject* referrer, u4 stringIdx)
                                        # return: StringObject*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved string failed
    je          common_exceptionThrown  # resolve failed; exception thrown
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_CONST_STRING_JUMBO */


   /*
    * Continuation if the Class has not yet been resolved.
    *  %ecx: BBBB (Class ref)
    *  need: target register
    */
.LOP_CONST_STRING_JUMBO_resolve:
    EXPORT_PC
    movl        rGLUE, %edx             # get MterpGlue pointer
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx <- glue->method->clazz
    movl        %ecx, -4(%esp)          # push parameter class ref
    movl        %edx, -8(%esp)          # push parameter glue->method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveString        # resolve string reference
                                        # call: (const ClassObject* referrer, u4 stringIdx)
                                        # return: StringObject*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved string failed
    je          common_exceptionThrown  # resolve failed; exception thrown
    SET_VREG    %eax, rINST             # vAA<- %eax; pResString[BBBB]
    FINISH      3                       # jump to next instruction

/* continuation for OP_CONST_CLASS */

   /*
    * Continuation if the Class has not yet been resolved.
    *  %ecx: BBBB (Class ref)
    *  need: target register
    */

.LOP_CONST_CLASS_resolve:
    EXPORT_PC
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        $1, -4(%esp)           # push parameter true
    movl        %ecx, -8(%esp)          # push parameter
    movl        %edx, -12(%esp)         # push parameter glue->method->clazz
    lea         -12(%esp), %esp
    call        dvmResolveClass         # resolve ClassObject pointer
                                        # class: (const ClassObject* referrer, u4 classIdx,
                                        #         bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    cmp         $0, %eax               # check for null pointer
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vAA<- resolved class
    FINISH      2                       # jump to next instruction

/* continuation for OP_CHECK_CAST */

.LOP_CHECK_CAST_resolved:
    cmp         %ecx, offObject_clazz(rINST) # check for same class
    jne         .LOP_CHECK_CAST_fullcheck   # not same class; do full check

.LOP_CHECK_CAST_okay:
    FINISH      2                       # jump to next instruction

   /*
    *  Trivial test failed, need to perform full check.
    *  offObject_clazz(rINST) holds obj->clazz
    *  %ecx holds class resolved from BBBB
    *  rINST holds object
    */

.LOP_CHECK_CAST_fullcheck:
    movl        offObject_clazz(rINST), %eax  # %eax<- obj->clazz
    movl        %eax, -12(%esp)         # push parameter obj->clazz
    movl        %ecx, -8(%esp)          # push parameter # push parameter resolved class
    lea         -12(%esp), %esp
    call        dvmInstanceofNonTrivial # call: (ClassObject* instance, ClassObject* clazz)
                                        # return: int
    lea         12(%esp), %esp
    cmp         $0, %eax               # failed?
    jne         .LOP_CHECK_CAST_okay        # success

   /*
    * A cast has failed.  We need to throw a ClassCastException with the
    * class of the object that failed to be cast.
    */

    EXPORT_PC                           # we will throw an exception
    movl        $.LstrClassCastExceptionPtr, -8(%esp) # push parameter message
    movl        offObject_clazz(rINST), rINST # rINST<- obj->clazz
    movl        offClassObject_descriptor(rINST), rINST # rINST<- obj->clazz->descriptor
    movl        rINST, -4(%esp)         # push parameter obj->clazz->descriptor
    lea         -8(%esp), %esp
    call        dvmThrowExceptionWithClassMessage # call: (const char* exceptionDescriptor,
                                                  #       const char* messageDescriptor, Object* cause)
                                                  # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown

   /*
    * Resolution required.  This is the least-likely path.
    *
    *  rINST holds object
    */

.LOP_CHECK_CAST_resolve:
    movl        offGlue_method(%edx), %eax # %eax<- glue->method
    FETCH       1, %ecx                 # %ecx holds BBBB
    EXPORT_PC                           # in case we throw an exception
    movl        $0, -8(%esp)           # push parameter false
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter BBBB
    movl        %eax, -16(%esp)         # push parameter glue->method>clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # resolve ClassObject pointer
                                        # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return ClassObject*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null pointer
    je          common_exceptionThrown  # handle excpetion
    movl        %eax, %ecx              # %ecx<- resolved class
    jmp         .LOP_CHECK_CAST_resolved

.LstrClassCastExceptionPtr:
.asciz      "Ljava/lang/ClassCastException;"

/* continuation for OP_INSTANCE_OF */

.LOP_INSTANCE_OF_break:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_methodClassDex(%ecx), %ecx # %ecx<- pDvmDex
    FETCH       1, %eax                 # %eax<- CCCC
    movl        offDvmDex_pResClasses(%ecx), %ecx # %ecx<- pDvmDex->pResClasses
    movl        (%ecx, %eax, 4), %ecx   # %ecx<- resolved class
    movl        offObject_clazz(%edx), %edx # %edx<- obj->clazz
    cmp         $0, %ecx               # check if already resovled
    je          .LOP_INSTANCE_OF_resolve     # not resolved before, so resolve now

.LOP_INSTANCE_OF_resolved:
    cmp         %ecx, %edx              # check if same class
    je          .LOP_INSTANCE_OF_trivial     # yes, finish
    jmp         .LOP_INSTANCE_OF_fullcheck   # no, do full check

   /*
    * The trivial test failed, we need to perform a full check.
    * %edx holds obj->clazz
    * %ecx holds class resolved from BBBB
    */

.LOP_INSTANCE_OF_fullcheck:
    movl        %edx, -8(%esp)          # push parameter obj->clazz
    movl        %ecx, -4(%esp)          # push parameter resolved class
    lea         -8(%esp), %esp
    call        dvmInstanceofNonTrivial # perform full check
                                        # call: (ClassObject* instance, ClassObject* clazz)
                                        # return: int
    andl        $15, rINST             # rINST<- A
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    lea         8(%esp), %esp
    SET_VREG    %eax, rINST             # vA<- r0
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

   /*
    * %edx holds boolean result
    */

.LOP_INSTANCE_OF_store:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    andl        $15, rINST             # rINST<- A
    SET_VREG    %edx, rINST             # vA<- r0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

   /*
    * Trivial test succeeded, save and bail.
    */

.LOP_INSTANCE_OF_trivial:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    andl        $15, rINST             # rINST<- A
    SET_VREG    $1, rINST              # vA<- r0
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

   /*
    * Resolution required.  This is the least-likely path.
    * %eax holds BBBB
    */

.LOP_INSTANCE_OF_resolve:

    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    EXPORT_PC
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter glue->method->clazz
    movl        %eax, -8(%esp)          # push parameter CCCC; type index
    movl        $1, -4(%esp)           # push parameter true
    lea         -12(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    cmp         $0, %eax               # check for null
    je          common_exceptionThrown  # handle exception
    movl        rINST, %edx             # %edx<- BA+
    shr         $4, %edx               # %edx<- B
    movl        %eax, %ecx              # need class in %ecx
    GET_VREG    %edx                    # %edx<- vB
    movl        offObject_clazz(%edx), %edx # %edx<- obj->clazz
    jmp         .LOP_INSTANCE_OF_resolved    # clazz resolved, continue

/* continuation for OP_NEW_INSTANCE */
.balign 32
.LOP_NEW_INSTANCE_finish:
    movl        %edx, -8(%esp)          # push parameter object
    movl        %eax, -4(%esp)          # push parameter flags
    lea         -8(%esp), %esp
    call        dvmAllocObject          # call: (ClassObject* clazz, int flags)
                                        # return: Object*
    cmp         $0, %eax               # check for failure
    lea         8(%esp), %esp
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vAA<- pObject
    FINISH      2                       # jump to next instruction

   /*
    * Class initialization required.
    *
    *  %edx holds class object
    */

.LOP_NEW_INSTANCE_needinit:
    movl        %edx, -4(%esp)          # push parameter object
    lea         -4(%esp), %esp
    call        dvmInitClass            # call: (ClassObject* clazz)
                                        # return: bool
    lea         4(%esp), %esp
    cmp         $0, %eax               # check for failure
    movl        -4(%esp), %edx          # %edx<- object
    je          common_exceptionThrown  # handle exception
    testl       $(ACC_INTERFACE|ACC_ABSTRACT), offClassObject_accessFlags(%edx)
    mov         $ALLOC_DONT_TRACK, %eax # %eax<- flag for alloc call
    je          .LOP_NEW_INSTANCE_finish      # continue
    jmp         .LOP_NEW_INSTANCE_abstract    # handle abstract or interface

   /*
    * Resolution required.  This is the least-likely path.
    *
    *  BBBB in %eax
    */

.LOP_NEW_INSTANCE_resolve:


    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    FETCH       1, %eax                 # %eax<- BBBB
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        offMethod_clazz(%ecx), %ecx # %ecx<- glue->method->clazz
    movl        %ecx, -12(%esp)         # push parameter clazz
    movl        $0, -4(%esp)           # push parameter false
    movl        %eax, -8(%esp)          # push parameter BBBB
    lea         -12(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer,
                                        #       u4 classIdx, bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         12(%esp), %esp
    movl        %eax, %edx              # %edx<- pObject
    cmp         $0, %edx               # check for failure
    jne         .LOP_NEW_INSTANCE_resolved    # continue
    jmp         common_exceptionThrown  # handle exception

   /*
    * We can't instantiate an abstract class or interface, so throw an
    * InstantiationError with the class descriptor as the message.
    *
    *  %edx holds class object
    */

.LOP_NEW_INSTANCE_abstract:
    movl        offClassObject_descriptor(%edx), %ecx # %ecx<- descriptor
    movl        %ecx, -4(%esp)          # push parameter descriptor
    movl        $.LstrInstantiationErrorPtr, -8(%esp) # push parameter message
    lea         -8(%esp), %esp
    call        dvmThrowExceptionWithClassMessage # call: (const char* exceptionDescriptor,
                                                  #        const char* messageDescriptor)
                                                  # return: void
    jmp         common_exceptionThrown  # handle exception

.LstrInstantiationErrorPtr:
.asciz      "Ljava/lang/InstantiationError;"

/* continuation for OP_NEW_ARRAY */

   /*
    * Resolve class.  (This is an uncommon case.)
    *
    *  %edx holds array length
    *  %ecx holds class ref CCCC
    */

.LOP_NEW_ARRAY_resolve:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        %edx, -4(%esp)          # save length
    movl        $0, -8(%esp)           # push parameter false
    movl        %ecx, -12(%esp)         # push parameter class ref
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    movl        %eax, -16(%esp)         # push parameter clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer,
                                        #       u4 classIdx, bool fromUnverifiedConstant)
                                        # return: ClassObject*
    cmp         $0, %eax               # check for failure
    lea         16(%esp), %esp
    je          common_exceptionThrown  # handle exception
    movl        -4(%esp), %edx          # %edx<- length

   /*
    * Finish allocation.
    *
    *  %eax holds class
    *  %edx holds array length
    */

.LOP_NEW_ARRAY_finish:
    movl        %eax, -12(%esp)         # push parameter class
    movl        %edx, -8(%esp)          # push parameter length
    movl        $ALLOC_DONT_TRACK, -4(%esp)
    lea         -12(%esp), %esp
    call        dvmAllocArrayByClass    # call: (ClassObject* arrayClass,
                                        # size_t length, int allocFlags)
                                        # return: ArrayObject*
    and         $15, rINST             # rINST<- A
    cmp         $0, %eax               # check for allocation failure
    lea         12(%esp), %esp
    je          common_exceptionThrown  # handle exception
    SET_VREG    %eax, rINST             # vA<- pArray
    FINISH      2                       # jump to next instruction

/* continuation for OP_FILLED_NEW_ARRAY */

.LOP_FILLED_NEW_ARRAY_break:
    movl        $0, -8(%esp)           # push parameter false
    movl        %ecx, -12(%esp)         # push parameter BBBB
    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter glue->method->clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

   /*
    * On entry:
    *  %eax holds array class
    *  rINST holds BA or AA
    */

.LOP_FILLED_NEW_ARRAY_continue:
    movl        offClassObject_descriptor(%eax), %eax # %eax<- arrayClass->descriptor
    movzbl      1(%eax), %eax           # %eax<- descriptor[1]
    cmpb        $'I', %al             # check if array of ints
    je          1f
    cmpb        $'L', %al
    je          1f
    cmpb        $'[', %al
    jne         .LOP_FILLED_NEW_ARRAY_notimpl     # jump to not implemented
1:
    movl        %eax, sReg0             # save type
    movl        rINST, -12(%esp)        # push parameter length
    movl        %eax, -16(%esp)         # push parameter descriptor[1]
    movl        $ALLOC_DONT_TRACK, -8(%esp) # push parameter to allocate flags
    .if         (!0)
    shrl        $4, -12(%esp)          # parameter length is B
    .endif
    lea         -16(%esp), %esp
    call        dvmAllocPrimitiveArray  # call: (char type, size_t length, int allocFlags)
                                        # return: ArrayObject*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

    FETCH       2, %edx                 # %edx<- FEDC or CCCC
    movl        rGLUE, %ecx             # %ecx<- MterpGlue pointer
    movl        %eax, offGlue_retval(%ecx) # retval<- new array
    lea         offArrayObject_contents(%eax), %eax # %eax<- newArray->contents
    subl        $1, -12(%esp)          # length--; check for negative
    js          2f                      # if length was zero, finish

   /*
    * copy values from registers into the array
    * %eax=array, %edx=CCCC/FEDC, -12(%esp)=length (from AA or B), rINST=AA/BA
    */

    .if         0
    lea         (rFP, %edx, 4), %ecx    # %ecx<- &fpp[CCCC]
1:
    movl        (%ecx), %edx            # %edx<- %ecx++
    lea         4(%ecx), %ecx           # %ecx++
    movl        %edx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    subl        $1, -12(%esp)          # length--
    jns         1b                      # or continue at 2
    .else
    cmp         $4, -12(%esp)          # check length
    jne         1f                      # has four args
    and         $15, rINST             # rINST<- A
    GET_VREG    rINST                   # rINST<- vA
    subl        $1, -12(%esp)          # count--
    movl        rINST, 16(%eax)         # contents[4]<- vA
1:
    movl        %edx, %ecx              # %ecx<- %edx; ecx for temp
    andl        $15, %ecx              # %ecx<- G/F/E/D
    GET_VREG    %ecx                    # %ecx<- vG/vF/vE/vD
    shr         $4, %edx               # %edx<- put next reg in low 4
    subl        $1, -12(%esp)          # count--
    movl        %ecx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    jns         1b                      # or continue at 2
    .endif
2:
    cmpb        $'I', sReg0            # check for int array
    je          3f
    movl        rGLUE, %ecx             # %ecx<- MterpGlue pointer
    movl        offGlue_retval(%ecx), %eax # Object head
    movl        offGlue_cardTable(%ecx), %ecx # card table base
    shrl        $GC_CARD_SHIFT, %eax   # convert to card num
    movb        %cl,(%ecx, %eax)        # mark card based on object head
3:
    FINISH      3                       # jump to next instruction

   /*
    * Throw an exception to indicate this mode of filled-new-array
    * has not been implemented.
    */

.LOP_FILLED_NEW_ARRAY_notimpl:
    movl        $.LstrInternalError, -8(%esp)
    movl        $.LstrFilledNewArrayNotImpl, -4(%esp)
    lea         -8(%esp), %esp
    call        dvmThrowException # call: (const char* exceptionDescriptor,
                                  #        const char* msg)
                                  # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown

.if         (!0)                 # define in one or the other, not both
.LstrFilledNewArrayNotImpl:
.asciz      "filled-new-array only implemented for 'int'"
.LstrInternalError:
.asciz  "Ljava/lang/InternalError;"
.endif

/* continuation for OP_FILLED_NEW_ARRAY_RANGE */

.LOP_FILLED_NEW_ARRAY_RANGE_break:
    movl        $0, -8(%esp)           # push parameter false
    movl        %ecx, -12(%esp)         # push parameter BBBB
    movl        rGLUE, %edx             # %edx<- MterpGlue pointer
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter glue->method->clazz
    lea         -16(%esp), %esp
    call        dvmResolveClass         # call: (const ClassObject* referrer, u4 classIdx,
                                        #        bool fromUnverifiedConstant)
                                        # return: ClassObject*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

   /*
    * On entry:
    *  %eax holds array class
    *  rINST holds BA or AA
    */

.LOP_FILLED_NEW_ARRAY_RANGE_continue:
    movl        offClassObject_descriptor(%eax), %eax # %eax<- arrayClass->descriptor
    movzbl      1(%eax), %eax           # %eax<- descriptor[1]
    cmpb        $'I', %al             # check if array of ints
    je          1f
    cmpb        $'L', %al
    je          1f
    cmpb        $'[', %al
    jne         .LOP_FILLED_NEW_ARRAY_RANGE_notimpl     # jump to not implemented
1:
    movl        %eax, sReg0             # save type
    movl        rINST, -12(%esp)        # push parameter length
    movl        %eax, -16(%esp)         # push parameter descriptor[1]
    movl        $ALLOC_DONT_TRACK, -8(%esp) # push parameter to allocate flags
    .if         (!1)
    shrl        $4, -12(%esp)          # parameter length is B
    .endif
    lea         -16(%esp), %esp
    call        dvmAllocPrimitiveArray  # call: (char type, size_t length, int allocFlags)
                                        # return: ArrayObject*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null return
    je          common_exceptionThrown  # handle exception

    FETCH       2, %edx                 # %edx<- FEDC or CCCC
    movl        rGLUE, %ecx             # %ecx<- MterpGlue pointer
    movl        %eax, offGlue_retval(%ecx) # retval<- new array
    lea         offArrayObject_contents(%eax), %eax # %eax<- newArray->contents
    subl        $1, -12(%esp)          # length--; check for negative
    js          2f                      # if length was zero, finish

   /*
    * copy values from registers into the array
    * %eax=array, %edx=CCCC/FEDC, -12(%esp)=length (from AA or B), rINST=AA/BA
    */

    .if         1
    lea         (rFP, %edx, 4), %ecx    # %ecx<- &fpp[CCCC]
1:
    movl        (%ecx), %edx            # %edx<- %ecx++
    lea         4(%ecx), %ecx           # %ecx++
    movl        %edx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    subl        $1, -12(%esp)          # length--
    jns         1b                      # or continue at 2
    .else
    cmp         $4, -12(%esp)          # check length
    jne         1f                      # has four args
    and         $15, rINST             # rINST<- A
    GET_VREG    rINST                   # rINST<- vA
    subl        $1, -12(%esp)          # count--
    movl        rINST, 16(%eax)         # contents[4]<- vA
1:
    movl        %edx, %ecx              # %ecx<- %edx; ecx for temp
    andl        $15, %ecx              # %ecx<- G/F/E/D
    GET_VREG    %ecx                    # %ecx<- vG/vF/vE/vD
    shr         $4, %edx               # %edx<- put next reg in low 4
    subl        $1, -12(%esp)          # count--
    movl        %ecx, (%eax)            # *contents<- vX
    lea         4(%eax), %eax           # %eax++; contents++
    jns         1b                      # or continue at 2
    .endif
2:
    cmpb        $'I', sReg0            # check for int array
    je          3f
    movl        rGLUE, %ecx             # %ecx<- MterpGlue pointer
    movl        offGlue_retval(%ecx), %eax # Object head
    movl        offGlue_cardTable(%ecx), %ecx # card table base
    shrl        $GC_CARD_SHIFT, %eax   # convert to card num
    movb        %cl,(%ecx, %eax)        # mark card based on object head
3:
    FINISH      3                       # jump to next instruction

   /*
    * Throw an exception to indicate this mode of filled-new-array
    * has not been implemented.
    */

.LOP_FILLED_NEW_ARRAY_RANGE_notimpl:
    movl        $.LstrInternalError, -8(%esp)
    movl        $.LstrFilledNewArrayNotImpl, -4(%esp)
    lea         -8(%esp), %esp
    call        dvmThrowException # call: (const char* exceptionDescriptor,
                                  #        const char* msg)
                                  # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown

.if         (!1)                 # define in one or the other, not both
.LstrFilledNewArrayNotImpl:
.asciz      "filled-new-array only implemented for 'int'"
.LstrInternalError:
.asciz  "Ljava/lang/InternalError;"
.endif

/* continuation for OP_PACKED_SWITCH */
.LOP_PACKED_SWITCH_finish:
    FINISH_RB   %edx, %ecx              # jump to next instruction

/* continuation for OP_SPARSE_SWITCH */
.LOP_SPARSE_SWITCH_finish:
    FINISH_RB   %edx, %ecx              # jump to next instruction

/* continuation for OP_CMPL_FLOAT */
.LOP_CMPL_FLOAT_greater:
    movl        $0x1, (rFP, rINST, 4)  # vAA<- greater than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPL_FLOAT_final:
    movl        $0x0, (rFP, rINST, 4)  # vAA<- equal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPL_FLOAT_finalNan:
    movl        $0xFFFFFFFF, (rFP, rINST, 4)   # vAA<- NaN
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_CMPG_FLOAT */
.LOP_CMPG_FLOAT_greater:
    movl        $0x1, (rFP, rINST, 4)  # vAA<- greater than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPG_FLOAT_final:
    movl        $0x0, (rFP, rINST, 4)  # vAA<- equal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPG_FLOAT_finalNan:
    movl        $0x1, (rFP, rINST, 4)   # vAA<- NaN
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_CMPL_DOUBLE */
.LOP_CMPL_DOUBLE_greater:
    movl        $0x1, (rFP, rINST, 4)  # vAA<- greater than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPL_DOUBLE_final:
    movl        $0x0, (rFP, rINST, 4)  # vAA<- equal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPL_DOUBLE_finalNan:
    movl        $0xFFFFFFFF, (rFP, rINST, 4)   # vAA<- NaN
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_CMPG_DOUBLE */
.LOP_CMPG_DOUBLE_greater:
    movl        $0x1, (rFP, rINST, 4)  # vAA<- greater than
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPG_DOUBLE_final:
    movl        $0x0, (rFP, rINST, 4)  # vAA<- equal
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

.LOP_CMPG_DOUBLE_finalNan:
    movl        $0x1, (rFP, rINST, 4)   # vAA<- NaN
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_CMP_LONG */

.LOP_CMP_LONG_final:
    movl        $0x0, (rFP, rINST, 4)  # vAA<- equal
    FINISH      2                       # jump to next instruction

.LOP_CMP_LONG_less:
    movl        $0xFFFFFFFF, (rFP, rINST, 4) # vAA<- less than
    FINISH      2                       # jump to next instruction

.LOP_CMP_LONG_greater:
    movl        $0x1, (rFP, rINST, 4)  # vAA<- greater than
    FINISH      2                       # jump to next instruction

/* continuation for OP_APUT_OBJECT */

.LOP_APUT_OBJECT_finish:
    movl        %edx, sReg0             # save &vBB[vCC]
    movl        %eax, sReg1             # save object head
    movl        offObject_clazz(rINST), %edx # %edx<- obj->clazz
    movl        %edx, -8(%esp)          # push parameter obj->clazz
    movl        offObject_clazz(%eax), %eax # %eax<- arrayObj->clazz
    movl        %eax, -4(%esp)          # push parameter arrayObj->clazz
    lea         -8(%esp), %esp
    call        dvmCanPutArrayElement   # test object type vs. array type
                                        # call: ClassObject* elemClass, ClassObject* arrayClass)
                                        # return: bool
    lea         8(%esp), %esp
    testl       %eax, %eax              # check for invalid array value
    je          common_errArrayStore    # handle invalid array value
    movl        sReg0, %edx             # restore &vBB[vCC]
    movl        rINST, offArrayObject_contents(%edx)
    movl        rGLUE, %eax
    FFETCH_ADV  2, %ecx                 # %ecx<- next instruction hi; fetch, advance
    movl        offGlue_cardTable(%eax), %eax # get card table base
    movl        sReg1, %edx             # restore object head
    shrl        $GC_CARD_SHIFT, %edx   # object head to card number
    movb        %al, (%eax, %edx)       # mark card using object head
    FGETOP_JMP  2, %ecx                 # jump to next instruction; getop, jmp
.LOP_APUT_OBJECT_skip_check:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl        rINST, offArrayObject_contents(%edx)
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET */

.LOP_IGET_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_WIDE */

.LOP_IGET_WIDE_finish2:
    lea         -8(%esp), %esp
    call        dvmResolveInstField     # resolve InstField ptr
                                        # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    movl        %eax, %ecx              # %ecx<- %eax; %ecx expected to hold field
    je          common_exceptionThrown

   /*
    *  %ecx holds resolved field
    */

.LOP_IGET_WIDE_finish:

    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB
    cmp         $0, %edx               # check for null object
    je          common_errNullObject
    movl        offInstField_byteOffset(%ecx), %ecx # %ecx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (%ecx, %edx), %xmm0     # %xmm0<- object field
    movq        %xmm0, (rFP, rINST, 4)  # vA<- %xmm0; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_OBJECT */

.LOP_IGET_OBJECT_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_OBJECT_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_BOOLEAN */

.LOP_IGET_BOOLEAN_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_BOOLEAN_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_BYTE */

.LOP_IGET_BYTE_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_BYTE_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_CHAR */

.LOP_IGET_CHAR_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_CHAR_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IGET_SHORT */

.LOP_IGET_SHORT_finish:
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    je          common_exceptionThrown  # not resolved; handle exception

    /*
     *  %eax holds resolved field
     */

.LOP_IGET_SHORT_finish2:
    movl        rINST, %ecx             # %ecx<- BA
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A

    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movl     (%ecx, %edx), %edx      # %edx<- object field
    SET_VREG    %edx, rINST             # vA<- %edx; object field
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT */

.LOP_IPUT_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl     rINST, (%edx, %ecx)     # object field<- vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_WIDE */

.LOP_IPUT_WIDE_finish2:
    lea         -8(%esp), %esp
    call        dvmResolveInstField     # resolve InstField ptr
    cmp         $0, %eax               # check if resolved
    lea         8(%esp), %esp
    movl        %eax, %ecx              # %ecx<- %eax; %ecx expected to hold field
    jne         .LOP_IPUT_WIDE_finish
    jmp         common_exceptionThrown

   /*
    * Currently:
    *  %ecx holds resolved field
    *  %edx does not hold object yet
    */

.LOP_IPUT_WIDE_finish:
    movl        rINST, %edx             # %edx<- BA
    shr         $4, %edx               # %edx<- B
    andl        $15, rINST             # rINST<- A
    GET_VREG    %edx                    # %edx<- vB
    cmp         $0, %edx               # check for null object
    je          common_errNullObject
    movl        offInstField_byteOffset(%ecx), %ecx # %ecx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    movq        (rFP, rINST, 4), %xmm0  # %xmm0<- vA
    movq        %xmm0, (%ecx, %edx)     # object field<- %xmm0; vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_OBJECT */

.LOP_IPUT_OBJECT_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_OBJECT_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_OBJECT_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    GET_VREG    rINST                   # rINST<- vA
    movl        rINST, (%edx, %ecx)     # object field<- vA
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl     rGLUE, %eax             # get glue
    movl        offGlue_cardTable(%eax), %eax # get card table base
    testl       rINST, rINST            # test if we stored a null value
    je          1f                     # skip card mark if null stored
    shrl        $GC_CARD_SHIFT, %ecx   # set obeject head to card number
    movb        %al, (%eax, %ecx)
1:
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_BOOLEAN */

.LOP_IPUT_BOOLEAN_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_BOOLEAN_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_BOOLEAN_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl     rINST, (%edx, %ecx)     # object field<- vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_BYTE */

.LOP_IPUT_BYTE_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_BYTE_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_BYTE_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl     rINST, (%edx, %ecx)     # object field<- vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_CHAR */

.LOP_IPUT_CHAR_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_CHAR_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_CHAR_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl     rINST, (%edx, %ecx)     # object field<- vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_IPUT_SHORT */

.LOP_IPUT_SHORT_finish:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %ecx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    lea         -8(%esp), %esp
    movl        %edx, (%esp)            # push parameter method->clazz
    call        dvmResolveInstField     # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: InstField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if resolved
    jne         .LOP_IPUT_SHORT_finish2
    jmp         common_exceptionThrown  # not resolved; handle exception

.LOP_IPUT_SHORT_finish2:
    movl        rINST, %ecx             # %ecx<- BA+
    shr         $4, %ecx               # %ecx<- B
    and         $15, rINST             # rINST<- A
    GET_VREG    %ecx                    # %ecx<- vB
    cmp         $0, %ecx               # check for null object
    je          common_errNullObject    # handle null object
    movl        offInstField_byteOffset(%eax), %edx # %edx<- field offset
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vA
    movl     rINST, (%edx, %ecx)     # object field<- vA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET */

.LOP_SGET_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET_WIDE */

   /*
    * Continuation if the field has not yet been resolved.
    *  %edx: BBBB field ref
    */

.LOP_SGET_WIDE_resolve:
    movl        offGlue_method(%eax), %eax # %eax <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %edx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%eax), %eax # %eax<- method->clazz
    movl        %eax, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if initalization failed
    movl        %eax, %ecx              # %ecx<- result
    jne         .LOP_SGET_WIDE_finish      # success, continue
    jmp         common_exceptionThrown  # failed; handle exception

/* continuation for OP_SGET_OBJECT */

.LOP_SGET_OBJECT_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_OBJECT_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET_BOOLEAN */

.LOP_SGET_BOOLEAN_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_BOOLEAN_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET_BYTE */

.LOP_SGET_BYTE_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_BYTE_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET_CHAR */

.LOP_SGET_CHAR_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_CHAR_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SGET_SHORT */

.LOP_SGET_SHORT_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    mov         %eax, %ecx              # %ecx<- result

.LOP_SGET_SHORT_finish:
    FFETCH_ADV  2, %edx                 # %edx<- next instruction hi; fetch, advance
    movl offStaticField_value(%ecx), %eax # %eax<- field value
    SET_VREG    %eax, rINST             # vAA<- field value
    FGETOP_JMP  2, %edx                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT */

.LOP_SPUT_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT_WIDE */

   /*
    * Continuation if the field has not yet been resolved.
    *  %edx: BBBB field ref
    */

.LOP_SPUT_WIDE_resolve:
    movl        offGlue_method(%eax), %eax # %eax <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %edx, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%eax), %eax # %eax<- method->clazz
    movl        %eax, -8(%esp)
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    lea         8(%esp), %esp
    cmp         $0, %eax               # check if initalization failed
    movl        %eax, %ecx              # %ecx<- result
    jne         .LOP_SPUT_WIDE_finish      # success, continue
    jmp         common_exceptionThrown  # failed; handle exception

/* continuation for OP_SPUT_OBJECT */

.LOP_SPUT_OBJECT_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_OBJECT_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA


    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    testl       rINST, rINST            # stored null object ptr?
    je          1f
    movl        rGLUE, %edx             # get glue
    movl        offField_clazz(%ecx), %ecx # ecx<- field->clazz
    movl        offGlue_cardTable(%edx), %edx # get card table base
    shrl        $GC_CARD_SHIFT, %ecx   # head to card number
    movb        %dl, (%edx, %ecx)       # mark card
1:
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT_BOOLEAN */

.LOP_SPUT_BOOLEAN_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_BOOLEAN_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT_BYTE */

.LOP_SPUT_BYTE_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_BYTE_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT_CHAR */

.LOP_SPUT_CHAR_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_CHAR_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_SPUT_SHORT */

.LOP_SPUT_SHORT_resolve:
    movl        offGlue_method(%edx), %edx # %edx <- glue->method
    EXPORT_PC                           # in case an exception is thrown
    movl        %eax, -4(%esp)          # push parameter CCCC; field ref
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    movl        %edx, -8(%esp)          # push parameter method->clazz
    lea         -8(%esp), %esp
    call        dvmResolveStaticField   # call: (const ClassObject* referrer, u4 ifieldIdx)
                                        # return: StaticField*
    cmp         $0, %eax               # check if initalization failed
    lea         8(%esp), %esp
    je          common_exceptionThrown  # failed; handle exception
    movl        %eax, %ecx              # %ecx<- result

.LOP_SPUT_SHORT_finish:
    FFETCH_ADV  2, %eax                 # %eax<- next instruction hi; fetch, advance
    GET_VREG    rINST                   # rINST<- vAA
    movl        rINST, offStaticField_value(%ecx) # field value<- vAA
    FGETOP_JMP  2, %eax                 # jump to next instruction; getop, jmp

/* continuation for OP_INVOKE_VIRTUAL */

.LOP_INVOKE_VIRTUAL_break:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %edx, -4(%esp)          # save "this" pointer register
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        $METHOD_VIRTUAL, -8(%esp) # push parameter method type
    movl        %ecx, -12(%esp)         # push paramter method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    lea         -16(%esp), %esp
    movl        %eax, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    jne         .LOP_INVOKE_VIRTUAL_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * At this point:
    *  %eax = resolved base method
    *  %edx = D or CCCC (index of first arg, which is the "this" ptr)
    */

.LOP_INVOKE_VIRTUAL_continue:
    GET_VREG    %edx                    # %edx<- "this" ptr
    movzwl      offMethod_methodIndex(%eax), %eax # %eax<- baseMethod->methodIndex
    cmp         $0, %edx               # %edx<- check for null "this"
    je          common_errNullObject    # handle null object
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %eax, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodNoRange # invoke method common code

/* continuation for OP_INVOKE_SUPER */

.LOP_INVOKE_SUPER_continue2:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    EXPORT_PC                           # must export for invoke
    cmp         $0, %ecx               # check if already resolved
    jne         .LOP_INVOKE_SUPER_continue
    jmp         .LOP_INVOKE_SUPER_resolve     # handle resolve

   /*
    *  %ecx = resolved base method
    *  %eax = method->clazz
    */

.LOP_INVOKE_SUPER_continue:
    movl        offClassObject_super(%eax), %edx # %edx<- glue->method->clazz->super
    movzwl      offMethod_methodIndex(%ecx), %ecx # %ecx<-  baseMethod->methodIndex
    cmp          offClassObject_vtableCount(%edx), %ecx # compare vtableCount with methodIndex
    EXPORT_PC                           # must export for invoke
    jnc         .LOP_INVOKE_SUPER_nsm         # handle method not present
    movl        offClassObject_vtable(%edx), %edx # %edx<- glue->method->clazz->super->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodNoRange # invoke method common code

.LOP_INVOKE_SUPER_resolve:
    movl        %eax, -12(%esp)         # push parameter clazz
    movl        %edx, -8(%esp)          # push parameter method index
    movl        $METHOD_VIRTUAL, -4(%esp) # push parameter method type
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    movl        %eax, %ecx              # %ecx<- method
    cmp         $0, %ecx               # check for null method return
    movl        -12(%esp), %eax         # %eax<- glue->method->clazz
    jne         .LOP_INVOKE_SUPER_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * Throw a NoSuchMethodError with the method name as the message.
    * %ecx = resolved base method
    */

.LOP_INVOKE_SUPER_nsm:
    movl        offMethod_name(%ecx), %edx # %edx<- method name
    jmp         common_errNoSuchMethod

/* continuation for OP_INVOKE_DIRECT */

   /*
    * %eax = reference (BBBB or CCCC)
    * -4(%esp) = "this" register
    */

.LOP_INVOKE_DIRECT_resolve:
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        $METHOD_DIRECT, -8(%esp) # push parameter method type
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        %eax, -12(%esp)         # push parameter reference
    lea         -16(%esp), %esp
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    GET_VREG    %edx                    # get "this" pointer
    je          common_exceptionThrown  # null pointer; handle exception
    cmp         $0, %edx               # check for null "this"
    movl        %eax, %ecx              # %ecx<- method
    jne         common_invokeMethodNoRange # invoke method common code
    jmp         common_errNullObject    # handle null object

/* continuation for OP_INVOKE_STATIC */

.LOP_INVOKE_STATIC_break:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        $METHOD_STATIC, -4(%esp) # resolver method type
    movl        %eax, -8(%esp)          # push parameter method index
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -12(%esp)         # push parameter method
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    cmp         $0, %eax               # check for null method
    je          common_exceptionThrown
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethodNoRange # invoke method common code

/* continuation for OP_INVOKE_INTERFACE */
.LOP_INVOKE_INTERFACE_break:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        %ecx, -8(%esp)          # push parameter method
    movl        offObject_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter
    lea         -16(%esp), %esp
    call        dvmFindInterfaceMethodInCache # call: (ClassObject* thisClass, u4 methodIdx,
                                              #       const Method* method, DvmDex* methodClassDex)
                                              # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check if find failed
    je          common_exceptionThrown  # handle exception
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethodNoRange # invoke method common code

/* continuation for OP_INVOKE_VIRTUAL_RANGE */

.LOP_INVOKE_VIRTUAL_RANGE_break:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %edx, -4(%esp)          # save "this" pointer register
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        $METHOD_VIRTUAL, -8(%esp) # push parameter method type
    movl        %ecx, -12(%esp)         # push paramter method index
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    lea         -16(%esp), %esp
    movl        %eax, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    jne         .LOP_INVOKE_VIRTUAL_RANGE_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * At this point:
    *  %eax = resolved base method
    *  %edx = D or CCCC (index of first arg, which is the "this" ptr)
    */

.LOP_INVOKE_VIRTUAL_RANGE_continue:
    GET_VREG    %edx                    # %edx<- "this" ptr
    movzwl      offMethod_methodIndex(%eax), %eax # %eax<- baseMethod->methodIndex
    cmp         $0, %edx               # %edx<- check for null "this"
    je          common_errNullObject    # handle null object
    movl        offObject_clazz(%edx), %edx # %edx<- thisPtr->clazz
    movl        offClassObject_vtable(%edx), %edx # %edx<- thisPtr->clazz->vtable
    movl        (%edx, %eax, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodRange # invoke method common code

/* continuation for OP_INVOKE_SUPER_RANGE */

.LOP_INVOKE_SUPER_RANGE_continue2:
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %eax # %eax<- glue->method
    movl        offMethod_clazz(%eax), %eax # %eax<- glue->method->clazz
    EXPORT_PC                           # must export for invoke
    cmp         $0, %ecx               # check if already resolved
    jne         .LOP_INVOKE_SUPER_RANGE_continue
    jmp         .LOP_INVOKE_SUPER_RANGE_resolve     # handle resolve

   /*
    *  %ecx = resolved base method
    *  %eax = method->clazz
    */

.LOP_INVOKE_SUPER_RANGE_continue:
    movl        offClassObject_super(%eax), %edx # %edx<- glue->method->clazz->super
    movzwl      offMethod_methodIndex(%ecx), %ecx # %ecx<-  baseMethod->methodIndex
    cmp          offClassObject_vtableCount(%edx), %ecx # compare vtableCount with methodIndex
    EXPORT_PC                           # must export for invoke
    jnc         .LOP_INVOKE_SUPER_RANGE_nsm         # handle method not present
    movl        offClassObject_vtable(%edx), %edx # %edx<- glue->method->clazz->super->vtable
    movl        (%edx, %ecx, 4), %ecx   # %ecx<- vtable[methodIndex]
    jmp         common_invokeMethodRange # invoke method common code

.LOP_INVOKE_SUPER_RANGE_resolve:
    movl        %eax, -12(%esp)         # push parameter clazz
    movl        %edx, -8(%esp)          # push parameter method index
    movl        $METHOD_VIRTUAL, -4(%esp) # push parameter method type
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    movl        %eax, %ecx              # %ecx<- method
    cmp         $0, %ecx               # check for null method return
    movl        -12(%esp), %eax         # %eax<- glue->method->clazz
    jne         .LOP_INVOKE_SUPER_RANGE_continue
    jmp         common_exceptionThrown  # null pointer; handle exception

   /*
    * Throw a NoSuchMethodError with the method name as the message.
    * %ecx = resolved base method
    */

.LOP_INVOKE_SUPER_RANGE_nsm:
    movl        offMethod_name(%ecx), %edx # %edx<- method name
    jmp         common_errNoSuchMethod

/* continuation for OP_INVOKE_DIRECT_RANGE */

   /*
    * %eax = reference (BBBB or CCCC)
    * -4(%esp) = "this" register
    */

.LOP_INVOKE_DIRECT_RANGE_resolve:
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        $METHOD_DIRECT, -8(%esp) # push parameter method type
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        %eax, -12(%esp)         # push parameter reference
    lea         -16(%esp), %esp
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, (%esp)            # push parameter clazz
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check for null method return
    movl        -4(%esp), %edx          # get "this" pointer register
    GET_VREG    %edx                    # get "this" pointer
    je          common_exceptionThrown  # null pointer; handle exception
    cmp         $0, %edx               # check for null "this"
    movl        %eax, %ecx              # %ecx<- method
    jne         common_invokeMethodRange # invoke method common code
    jmp         common_errNullObject    # handle null object

/* continuation for OP_INVOKE_STATIC_RANGE */

.LOP_INVOKE_STATIC_RANGE_break:
    movl        offGlue_method(%edx), %edx # %edx<- glue->method
    movl        $METHOD_STATIC, -4(%esp) # resolver method type
    movl        %eax, -8(%esp)          # push parameter method index
    movl        offMethod_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -12(%esp)         # push parameter method
    lea         -12(%esp), %esp
    call        dvmResolveMethod        # call: (const ClassObject* referrer,
                                        #       u4 methodIdx, MethodType methodType)
                                        # return: Method*
    lea         12(%esp), %esp
    cmp         $0, %eax               # check for null method
    je          common_exceptionThrown
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethodRange # invoke method common code

/* continuation for OP_INVOKE_INTERFACE_RANGE */
.LOP_INVOKE_INTERFACE_RANGE_break:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_method(%ecx), %ecx # %ecx<- glue->method
    movl        %ecx, -8(%esp)          # push parameter method
    movl        offObject_clazz(%edx), %edx # %edx<- glue->method->clazz
    movl        %edx, -16(%esp)         # push parameter
    lea         -16(%esp), %esp
    call        dvmFindInterfaceMethodInCache # call: (ClassObject* thisClass, u4 methodIdx,
                                              #       const Method* method, DvmDex* methodClassDex)
                                              # return: Method*
    lea         16(%esp), %esp
    cmp         $0, %eax               # check if find failed
    je          common_exceptionThrown  # handle exception
    movl        %eax, %ecx              # %ecx<- method
    jmp         common_invokeMethodRange # invoke method common code

/* continuation for OP_FLOAT_TO_INT */

.LOP_FLOAT_TO_INT_break:
    fnstcw      -2(%esp)                # save control word
    orl         $0xc00, -2(%esp)       # reset control
    fldcw       -2(%esp)                # load control word
    xorl        $0xc00, -2(%esp)       # reset control
    fistpl      (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_INT_nanInf:
    jnp         .LOP_FLOAT_TO_INT_posInf      # handle posInf
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $0x00000000,  (rFP, %edx, 4) # vA<- NaN
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_INT_posInf:
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $0x7FFFFFFF,  (rFP, %edx, 4) # vA<- posInf
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_INT_negInf:
    fstps       (rFP, %edx, 4)          # pop floating point stack
    fstps       (rFP, %edx, 4)          # pop floating point stack
    movl        $0x80000000, (rFP, %edx, 4) # vA<- negInf
    FINISH      1                       # jump to next instruction

/* continuation for OP_FLOAT_TO_LONG */

.LOP_FLOAT_TO_LONG_break:
    fnstcw      -2(%esp)                # save control word
    orl         $0xc00, -2(%esp)       # update control
    fldcw       -2(%esp)                # load control word
    xorl        $0xc00, -2(%esp)       # reset control
    fistpll     (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_LONG_nanInf:
    jnp         .LOP_FLOAT_TO_LONG_posInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNanLong, %xmm0   # %xmm0<- NaN
    movq        %xmm0,  (rFP, %edx, 4)  # vA<- %xmm0; NaN
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_LONG_posInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvaluePosInfLong, %xmm0 # %xmm0<- posInf
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; posInf
    FINISH      1                       # jump to next instruction

.LOP_FLOAT_TO_LONG_negInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNegInfLong, %xmm0 # %xmm0<- negInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; negInf
    FINISH      1                       # jump to next instruction

/* continuation for OP_DOUBLE_TO_INT */

.LOP_DOUBLE_TO_INT_break:
    fnstcw      -2(%esp)                # save control word
    orl         $0xc00, -2(%esp)       # reset control
    fldcw       -2(%esp)                # load control word
    xorl        $0xc00, -2(%esp)       # reset control
    fistpl      (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_INT_nanInf:
    jnp         .LOP_DOUBLE_TO_INT_posInf
    fstps       (rFP, %edx, 4)
    movl        $0x00000000,  (rFP, %edx, 4) # vA<- NaN
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_INT_posInf:
    fstps       (rFP, %edx, 4)
    movl        $0x7FFFFFFF,  (rFP, %edx, 4) # vA<- posInf
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_INT_negInf:
    fstps       (rFP, %edx, 4)
    fstps       (rFP, %edx, 4)
    movl        $0x80000000,  (rFP, %edx, 4) # vA<- negInf
    FINISH      1                       # jump to next instruction

/* continuation for OP_DOUBLE_TO_LONG */

.LOP_DOUBLE_TO_LONG_break:
    fnstcw      -2(%esp)                # save control word
    orl         $0xc00, -2(%esp)       # reset control
    fldcw       -2(%esp)                # load control word
    xorl        $0xc00, -2(%esp)       # reset control
    fistpll     (rFP, %edx, 4)          # move converted int
    fldcw       -2(%esp)                # load saved control word
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_LONG_nanInf:
    jnp         .LOP_DOUBLE_TO_LONG_posInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNanLong, %xmm0   # %xmm0<- NaN
    movq        %xmm0,  (rFP, %edx, 4)  # vA<- %xmm0; NaN
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_LONG_posInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvaluePosInfLong, %xmm0 # %xmm0<- posInf
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; posInf
    FINISH      1                       # jump to next instruction

.LOP_DOUBLE_TO_LONG_negInf:
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        .LvalueNegInfLong, %xmm0 # %xmm0<- negInf
    fstpl       (rFP, %edx, 4)          # move converted int
    movq        %xmm0, (rFP, %edx, 4)   # vA<- %xmm0; negInf
    FINISH      1                       # jump to next instruction

/* continuation for OP_DIV_INT */
.LOP_DIV_INT_break:
    .if  1
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_DIV_INT_break2:
    FINISH      2                       # jump to next instruction

/* continuation for OP_REM_INT */
.LOP_REM_INT_break:
    .if  0
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_REM_INT_break2:
    FINISH      2                       # jump to next instruction

/* continuation for OP_MUL_LONG */

   /*
    * X = (rFP, rINST, 4)
    * W = 4(rFP, rINST, 4)
    * Z = (rFP, %edx, 4)
    * Y = 4(rFP, %edx, 4)
    */

.LOP_MUL_LONG_finish:
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

/* continuation for OP_DIV_LONG */
.LOP_DIV_LONG_finish:
    movq        %xmm0, -16(%esp)        # push arg vBB,vBB+1
    lea         -16(%esp), %esp
    call        __divdi3                   # call func
    lea         16(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vAA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vAA+1<- return high
    FINISH      2                       # jump to next instruction

/* continuation for OP_REM_LONG */
.LOP_REM_LONG_finish:
    movq        %xmm0, -16(%esp)        # push arg vBB,vBB+1
    lea         -16(%esp), %esp
    call        __moddi3                   # call func
    lea         16(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vAA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vAA+1<- return high
    FINISH      2                       # jump to next instruction

/* continuation for OP_SHR_LONG */

.LOP_SHR_LONG_finish:
    movq        .Lvalue64, %xmm3        # %xmm3<- 64
    psubq       %xmm0, %xmm3            # %xmm3<- 64 - shift amount
    movq        .L64bits, %xmm4         # %xmm4<- lower 64 bits set
    psllq       %xmm3, %xmm4            # %xmm4<- correct mask for sign bits
    por         %xmm4, %xmm1            # %xmm1<- signed and shifted vBB

.LOP_SHR_LONG_final:
    movq        %xmm1, (rFP, rINST, 4)  # vAA<- shifted vBB
    FINISH      2                       # jump to next instruction

/* continuation for OP_REM_DOUBLE */

.LOP_REM_DOUBLE_break:
    call        fmod                    # call: (long double x, long double y)
                                        # return: double
    lea         16(%esp), %esp
    fstpl       (rFP, rINST, 4)         # vAA<- remainder; return of fmod
    FINISH      2                       # jump to next instruction

/* continuation for OP_DIV_INT_2ADDR */
.LOP_DIV_INT_2ADDR_break:
    .if  1
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_DIV_INT_2ADDR_break2:
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp


/* continuation for OP_REM_INT_2ADDR */
.LOP_REM_INT_2ADDR_break:
    .if  0
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_REM_INT_2ADDR_break2:
    FFETCH_ADV  1, %edx                 # %ecx<- next instruction hi; fetch, advance
    FGETOP_JMP  1, %edx                 # jump to next instruction; getop, jmp


/* continuation for OP_MUL_LONG_2ADDR */

   /*
    * X = (rFP, rINST, 4)
    * W = 4(rFP, rINST, 4)
    * Z = (rFP, %edx, 4)
    * Y = 4(rFP, %edx, 4)
    */

.LOP_MUL_LONG_2ADDR_finish:
    movl        4(rFP, rINST, 4), %ecx  # %ecx<- W
    imull       (rFP, %edx, 4), %ecx    # %ecx<- WxZ
    movl                4(rFP, %edx, 4), %eax   # %eax<- Y
    imull       (rFP, rINST, 4), %eax   # %eax<- X*Y
    addl        %eax, %ecx              # %ecx<- (WZ + XY)
    movl        (rFP, %edx, 4), %eax    # %eax<- Z
    mull        (rFP, rINST, 4)         # %edx:eax<- XZ
    addl        %edx, %ecx              # %ecx<- carry + (WZ + XY)
    movl        sReg0, %edx             # %edx<- A
    movl        %ecx, 4(rFP, %edx, 4)   # vA+1<- results hi
    movl        %eax, (rFP, %edx, 4)    # vA<- results lo
    FINISH      1                       # jump to next instruction

/* continuation for OP_DIV_LONG_2ADDR */
.LOP_DIV_LONG_2ADDR_break:
    movq        %xmm0, -20(%esp)        # push arg vA, vA+1
    lea         -20(%esp), %esp
    call        __divdi3                   # call func
    lea         20(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vA<- return high
    FFETCH_ADV  1, %ecx                 # %ecx<- next instruction hi; fetch, advance
    FGETOP_JMP 1, %ecx                  # jump to next instruction; getop, jmp

/* continuation for OP_REM_LONG_2ADDR */
.LOP_REM_LONG_2ADDR_break:
    movq        %xmm0, -20(%esp)        # push arg vA, vA+1
    lea         -20(%esp), %esp
    call        __moddi3                   # call func
    lea         20(%esp), %esp
    movl        %eax, (rFP, rINST, 4)   # vA<- return low
    movl        %edx, 4(rFP, rINST, 4)  # vA<- return high
    FFETCH_ADV  1, %ecx                 # %ecx<- next instruction hi; fetch, advance
    FGETOP_JMP 1, %ecx                  # jump to next instruction; getop, jmp

/* continuation for OP_SHR_LONG_2ADDR */

.LOP_SHR_LONG_2ADDR_finish:
    movq        .Lvalue64, %xmm3        # %xmm3<- 64
    psubq       %xmm0, %xmm3            # %xmm3<- 64 - shift amount
    movq        .L64bits, %xmm4         # %xmm4<- lower 64 bits set
    psllq       %xmm3, %xmm4            # %xmm4<- correct mask for sign bits
    por         %xmm4, %xmm1            # %xmm1<- signed and shifted vBB

.LOP_SHR_LONG_2ADDR_final:
    movq        %xmm1, (rFP, rINST, 4)  # vAA<- shifted vBB
    FINISH      1                       # jump to next instruction

/* continuation for OP_REM_DOUBLE_2ADDR */

.LOP_REM_DOUBLE_2ADDR_break:
    call        fmod                    # call: (long double x, long double y)
                                        # return: double
    lea         20(%esp), %esp
    fstpl       (rFP, rINST, 4)         # vAA<- remainder; return of fmod
    FINISH      1                       # jump to next instruction

/* continuation for OP_DIV_INT_LIT16 */
.LOP_DIV_INT_LIT16_break:
    .if  1
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_DIV_INT_LIT16_break2:

    FINISH      2                       # jump to next instruction

/* continuation for OP_REM_INT_LIT16 */
.LOP_REM_INT_LIT16_break:
    .if  0
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif
.LOP_REM_INT_LIT16_break2:

    FINISH      2                       # jump to next instruction

/* continuation for OP_DIV_INT_LIT8 */
.LOP_DIV_INT_LIT8_break:
    .if  1
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif

.LOP_DIV_INT_LIT8_break2:
    FINISH      2                       # jump to next instruction
    #FGETOP_JMP 2, %ecx                 # jump to next instruction; getop, jmp

/* continuation for OP_REM_INT_LIT8 */
.LOP_REM_INT_LIT8_break:
    .if  0
    movl        $0x80000000, (rFP, rINST, 4) # vAA<- min int
    .else
    movl        $0, (rFP, rINST, 4)    # vAA<- 0
    .endif

.LOP_REM_INT_LIT8_break2:
    FINISH      2                       # jump to next instruction
    #FGETOP_JMP 2, %ecx                 # jump to next instruction; getop, jmp

/* continuation for OP_EXECUTE_INLINE */

   /*
    * Extract args, call function.
    *  rINST = #of args (0-4)
    *  %ecx = call index
    */

.LOP_EXECUTE_INLINE_continue:
    FETCH       2, %edx                 # %edx<- FEDC
    cmp         $1, rINST              # determine number of arguments
    jl          0f                      # handle zero args
    je          1f                      # handle one arg
    cmp         $3, rINST
    jl          2f                      # handle two args
    je          3f                      # handle three args
4:
    movl        %edx, rINST             # rINST<- FEDC
    and         $0xf000, rINST         # isolate F
    shr         $10, rINST
    movl        (rFP, rINST), rINST     # rINST<- vF
    movl        rINST, 12(%esp)         # push parameter vF
3:
    movl        %edx, rINST             # rINST<- FEDC
    and         $0x0f00, rINST         # isolate E
    shr         $6, rINST
    movl        (rFP, rINST), rINST     # rINST<- vE
    movl        rINST, 8(%esp)          # push parameter E
2:
    movl        %edx, rINST             # rINST<- FEDC
    and         $0x00f0, rINST         # isolate D
    shr         $2, rINST
    movl        (rFP, rINST), rINST     # rINST<- vD
    movl        rINST, 4(%esp)          # push parameter D
1:
    and         $0x000f, %edx          # isolate C
    movl        (rFP, %edx, 4), %edx    # rINST<- vC
    movl        %edx, (%esp)            # push parameter C
0:
    shl         $4, %ecx
    movl        $gDvmInlineOpsTable, %eax # %eax<- address for table of inline operations
    call        *(%eax, %ecx)           # call function

    cmp         $0, %eax               # check boolean result of inline
    FFETCH_ADV  3, %eax                 # %eax<- next instruction hi; fetch, advance
    lea         24(%esp), %esp          # update stack pointer
    je          common_exceptionThrown  # handle exception
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

    .size   dvmAsmSisterStart, .-dvmAsmSisterStart
    .global dvmAsmSisterEnd
dvmAsmSisterEnd:

/* File: x86-atom/entry.S */
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
    cmp         $kInterpEntryInstr, offGlue_entryPoint(%ecx) # check instruction
    movl        offGlue_fp(%ecx), rFP   # get frame pointer
    movl        %esp, offGlue_bailPtr(%ecx) # save SP for eventual return
    FFETCH      %edx                    # %edx<- opcode
    jne         .Lnot_instr             # no, handle it
    FGETOP_JMPa %edx                    # start executing the instruction at rPC

   /*
    * Not an instruction. Are we returning from a method?
    */

.Lnot_instr:
    cmpl        $kInterpEntryReturn, offGlue_entryPoint(%ecx)
    je          common_returnFromMethod

   /*
    * No, are we throwing an exception?
    */

.Lnot_return:
    cmpl        $kInterpEntryThrow, offGlue_entryPoint(%ecx)
    je          common_exceptionThrown

   /*
    * No, then we must abort.
    */

.Lbad_arg:
    pushl       offGlue_entryPoint(%ecx)
    movl        $.LstrBadEntryPoint, -4(%esp)
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

/* File: x86-atom/footer.S */
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
    * File: footer.S
    */

    .text
    .align      2

   /*
    * Check to see if the thread needs to be suspended or debugger/profiler
    * activity has begun.
    *
    * On entry:
    *  %ecx is reentry type, e.g. kInterpEntryInstr
    *  %edx is PC adjustment in bytes
    */

common_periodicChecks:
    movl        %edx, -8(%esp)          # save pc adjustments
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        %ebx, -4(%esp)          # save %ebx to the stack
    movl        offGlue_pSelfSuspendCount(%edx), %ebx # %ebx<- pSuspendCount (int)
4:
    movl        offGlue_pDebuggerActive(%edx), %eax # %eax<- pDebuggerActive
    testl       %eax, %eax
    je          5f
    movzbl        (%eax), %eax            # %eax<- get debuggerActive (boolean)
5:
    cmp         $0, (%ebx)             # check if suspend is pending
    jne         2f                      # handle suspend
    movl        offGlue_pActiveProfilers(%edx), %ebx # %ebx<- activeProfilers (int)
    orl         (%ebx), %eax            # %eax<- merge activeProfilers and debuggerActive
    movl        -8(%esp), %edx          # %edx<- restore %edx
    jne         3f                      # debugger or profiler active; switch interp
    movl        -4(%esp), %ebx          # %ebx<- restore %ebx
    ret                                 # return
2:                                      # check suspended
    EXPORT_PC
    movl        offGlue_self(%edx), %eax # %eax<- glue->self
    movl        %eax, -12(%esp)         # push parameter boolean
    lea         -12(%esp), %esp
    call        dvmCheckSuspendPending  # call: (Thread* self)
                                        # return: bool
    movl        4(%esp), %edx           # %edx<- restore %edx
    movl        8(%esp), %ebx           # %ebx<- restore %ebx
    lea         12(%esp), %esp
    ret
3:                                      # debugger/profiler enabled, bail out
    leal        (rPC, %edx, 2), rPC     # adjust pc to show target
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movb        $kInterpEntryInstr, offGlue_entryPoint(%ecx)
    movl        $1, %edx               # switch interpreter
    jmp         common_gotoBail         # bail

   /*
    * Check to see if the thread needs to be suspended or debugger/profiler
    * activity has begun. With this variant, the reentry type is hard coded
    * as kInterpEntryInstr.
    *
    * On entry:
    *  %edx is PC adjustment in bytes
    */

common_periodicChecks_backwardBranch:
    EXPORT_PC
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_pSelfSuspendCount(%ecx), rINST # %ebx<- pSuspendCount (int)
4:
    movl        offGlue_pDebuggerActive(%ecx), %eax # %eax<- pDebuggerActive
    testl       %eax, %eax              # test for NULL pointer
    je          5f
    movzbl      (%eax), %eax            # %eax<- get debuggerActive count
5:
    cmp         $0, (rINST)            # check if suspend is pending
    jne         2f                      # handle suspend
    movl        offGlue_pActiveProfilers(%ecx), rINST # %edx<- activeProfilers (int)
    orl          (rINST), %eax           # %eax<- merge activeProfilers and debuggerActive
    jne         3f                      # debugger or profiler active; switch interp
    FINISH_RB   %edx, %ecx              # jump to next instruction
2:                                      # check suspended
    movl        offGlue_self(%ecx), %eax# %eax<- glue->self
    movl        %edx, rINST
    movl        %eax, -12(%esp)         # push parameter boolean
    lea         -12(%esp), %esp
    call        dvmCheckSuspendPending  # call: (Thread* self)
                                        # return: bool
    movl        rINST, %edx             # %edx<- restore %edx
    lea         12(%esp), %esp
    FINISH_RB   %edx, %ecx
3:                                      # debugger/profiler enabled, bail out
    leal        (rPC, %edx, 2), rPC     # adjust pc to show target
    movb        $kInterpEntryInstr, offGlue_entryPoint(%ecx)
    movl        $1, %edx               # switch interpreter
    jmp         common_gotoBail         # bail

   /*
    * The equivalent of "goto bail", this calls through the "bail handler".
    * State registers will be saved to the "glue" area before bailing.
    *
    * On entry:
    *  %edx is "bool changeInterp", indicating if we want to switch to the
    *     other interpreter or just bail all the way out
    */

common_gotoBail:
    SAVE_PC_FP_TO_GLUE %ecx             # save program counter and frame pointer

   /*
    * Inlined dvmMterpStdBail
    */

    lea         40(%ebp), %esp
    movl        %edx, %eax
    movl        24(%ebp), %edi
    movl        28(%ebp), %esi
    movl        32(%ebp), %ebx
    movl        36(%ebp), %ebp
    ret

   /*
    * Common code for method invocation with range.
    *
    * On entry:
    *  %ecx is "Method* methodToCall", the method we're trying to call
    */

common_invokeMethodRange:
.LinvokeNewRange:

   /*
    * prepare to copy args to "outs" area of current frame
    */

    SAVEAREA_FROM_FP %eax               # %eax<- &outs; &StackSaveArea
    test        rINST, rINST            # test for no args
    movl        rINST, sReg0            # sReg0<- AA
    jz          .LinvokeArgsDone        # no args; jump to args done
    FETCH       2, %edx                 # %edx<- CCCC

   /*
    * %ecx=methodToCall, %edx=CCCC, sReg0=count, %eax=&outs (&stackSaveArea)
    * (very few methods have > 10 args; could unroll for common cases)
    */

    movl        %ebx, sReg1             # sReg1<- save %ebx
    lea         (rFP, %edx, 4), %edx    # %edx<- &vCCCC
    shll        $2, sReg0              # sReg0<- offset
    subl        sReg0, %eax             # %eax<- update &outs
    shrl        $2, sReg0              # sReg0<- offset
1:
    movl        (%edx), %ebx            # %ebx<- vCCCC
    lea         4(%edx), %edx           # %edx<- &vCCCC++
    subl        $1, sReg0              # sReg<- sReg--
    movl        %ebx, (%eax)            # *outs<- vCCCC
    lea         4(%eax), %eax           # outs++
    jne         1b                      # loop if count (sReg0) not zero
    movl        sReg1, %ebx             # %ebx<- restore %ebx
    jmp         .LinvokeArgsDone        # continue

   /*
    * %ecx is "Method* methodToCall", the method we're trying to call
    * prepare to copy args to "outs" area of current frame
    */

common_invokeMethodNoRange:
.LinvokeNewNoRange:
    movl        rINST, sReg0            # sReg0<- BA
    shrl        $4, sReg0              # sReg0<- B
    je          .LinvokeArgsDone        # no args; jump to args done
    SAVEAREA_FROM_FP %eax               # %eax<- &outs; &StackSaveArea
    FETCH       2, %edx                 # %edx<- GFED

   /*
    * %ecx=methodToCall, %edx=GFED, sReg0=count, %eax=outs
    */

.LinvokeNonRange:
    cmp         $2, sReg0              # compare sReg0 to 2
    movl        %edx, sReg1             # sReg1<- GFED
    jl          1f                      # handle 1 arg
    je          2f                      # handle 2 args
    cmp         $4, sReg0              # compare sReg0 to 4
    jl          3f                      # handle 3 args
    je          4f                      # handle 4 args
5:
    andl        $15, rINST             # rINST<- A
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, rINST, 4), %edx   # %edx<- vA
    movl        %edx, (%eax)            # *outs<- vA
    movl        sReg1, %edx             # %edx<- GFED
4:
    shr         $12, %edx              # %edx<- G
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx, 4), %edx    # %edx<- vG
    movl        %edx, (%eax)            # *outs<- vG
    movl        sReg1, %edx             # %edx<- GFED
3:
    and         $0x0f00, %edx          # %edx<- 0F00
    shr         $6, %edx               # %edx<- F at correct offset
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx), %edx       # %edx<- vF
    movl        %edx, (%eax)            # *outs<- vF
    movl        sReg1, %edx             # %edx<- GFED
2:
    and         $0x00f0, %edx          # %edx<- 00E0
    shr         $2, %edx               # %edx<- E at correct offset
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx), %edx       # %edx<- vE
    movl        %edx, (%eax)            # *outs<- vE
    movl        sReg1, %edx             # %edx<- GFED
1:
    and         $0x000f, %edx          # %edx<- 000D
    movl        (rFP, %edx, 4), %edx    # %edx<- vD
    movl        %edx, -4(%eax)          # *--outs<- vD
0:

   /*
    * %ecx is "Method* methodToCall", the method we're trying to call
    * find space for the new stack frame, check for overflow
    */

.LinvokeArgsDone:
    movzwl      offMethod_registersSize(%ecx), %eax # %eax<- methodToCall->regsSize
    movzwl      offMethod_outsSize(%ecx), %edx # %edx<- methodToCall->outsSize
    movl        %ecx, sReg0             # sReg<- methodToCall
    shl         $2, %eax               # %eax<- update offset
    SAVEAREA_FROM_FP %ecx               # %ecx<- &outs; &StackSaveArea
    subl        %eax, %ecx              # %ecx<- newFP; (old savearea - regsSize)
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %ecx, sReg1             # sReg1<- &outs
    subl        $sizeofStackSaveArea, %ecx # %ecx<- newSaveArea (stack save area using newFP)
    movl        offGlue_interpStackEnd(%eax), %eax # %eax<- glue->interpStackEnd
    movl        %eax, sReg2             # sReg2<- glue->interpStackEnd
    shl         $2, %edx               # %edx<- update offset for outsSize
    movl        %ecx, %eax              # %eax<- newSaveArea
    sub         %edx, %ecx              # %ecx<- bottom; (newSaveArea - outsSize)
    cmp         sReg2, %ecx             # compare interpStackEnd and bottom
    movl        sReg0, %ecx             # %ecx<- restore methodToCall
    jl          .LstackOverflow         # handle frame overflow

   /*
    * set up newSaveArea
    */

#ifdef EASY_GDB
    SAVEAREA_FROM_FP %edx               # %edx<- &outs; &StackSaveArea
    movl        %edx, offStackSaveArea_prevSave(%eax) # newSaveArea->prevSave<- &outs
#endif
    movl        rFP, offStackSaveArea_prevFrame(%eax) # newSaveArea->prevFrame<- rFP
    movl        rPC, offStackSaveArea_savedPc(%eax) # newSaveArea->savedPc<- rPC
    testl       $ACC_NATIVE, offMethod_accessFlags(%ecx) # check for native call
    movl        %ecx, offStackSaveArea_method(%eax) # newSaveArea->method<- method to call
    jne         .LinvokeNative          # handle native call

   /*
    * Update "glue" values for the new method
    * %ecx=methodToCall, sReg1=newFp
    */

    movl        offMethod_clazz(%ecx), %edx # %edx<- method->clazz
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %ecx, offGlue_method(%eax) # glue->method<- methodToCall
    movl        offClassObject_pDvmDex(%edx), %edx # %edx<- method->clazz->pDvmDex
    movl        offMethod_insns(%ecx), rPC # rPC<- methodToCall->insns
    movl        %edx, offGlue_methodClassDex(%eax) # glue->methodClassDex<- method->clazz->pDvmDex
    movl        offGlue_self(%eax), %ecx # %ecx<- glue->self
    movl        sReg1, rFP              # rFP<- newFP
    movl        rFP, offThread_curFrame(%ecx) # glue->self->curFrame<- newFP
    FINISH_A                            # jump to methodToCall->insns

   /*
    * Prep for the native call
    * %ecx=methodToCall, sReg1=newFP, %eax=newSaveArea
    */

.LinvokeNative:
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        %ecx, -20(%esp)         # push parameter methodToCall
    movl        offGlue_self(%edx), %edx # %edx<- glue->self
    movl        offThread_jniLocal_topCookie(%edx), %ecx # %ecx<- glue->self->thread->refNext
    movl        %ecx, offStackSaveArea_localRefCookie(%eax) # newSaveArea->localRefCookie<- refNext
    movl        %eax, -4(%esp)          # save newSaveArea
    movl        sReg1, %eax             # %eax<- newFP
    movl        %eax, offThread_curFrame(%edx) # glue->self->curFrame<- newFP
    movl        %edx, -8(%esp)          # save glue->self
    movl        %edx, -16(%esp)         # push parameter glue->self
    movl        rGLUE, %edx             # %edx<- pMterpGlue
    movl        -20(%esp), %ecx         # %ecx<- methodToCall
    lea         offGlue_retval(%edx), %edx # %edx<- &retval
    movl        %edx, -24(%esp)         # push parameter pMterpGlue
    movl        %eax, -28(%esp)         # push parameter newFP
    lea         -28(%esp), %esp

#ifdef ASSIST_DEBUGGER
    jmp         .Lskip
    .type       dalvik_mterp, %function
dalvik_mterp:
    MTERP_ENTRY
.Lskip:
#endif
    call        *offMethod_nativeFunc(%ecx) # call methodToCall->nativeFunc
    lea         28(%esp), %esp
    movl        -4(%esp), %edx          # %edx<- newSaveArea
    movl        -8(%esp), %ecx          # %ecx<- glue->self
    movl        offStackSaveArea_localRefCookie(%edx), %eax  # %eax<- newSaveArea->localRefCookie
    FFETCH_ADV  3, %edx                 # %edx<- next instruction hi; fetch, advance
    cmp         $0, offThread_exception(%ecx) # check for exception
    movl        rFP, offThread_curFrame(%ecx) # glue->self->curFrame<- rFP
    movl        %eax, offThread_jniLocal_topCookie(%ecx) # glue->self<- newSaveArea->localRefCookie
    jne         common_exceptionThrown  # handle exception
    FGETOP_JMP  3, %edx                 # jump to next instruction; getop, jmp

.LstackOverflow:
    movl        %ecx, -4(%esp)          # push method to call
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_self(%ecx), %ecx # %ecx<- glue->self
    movl        %ecx, -8(%esp)          # push parameter self
    lea         -8(%esp), %esp
    call        dvmHandleStackOverflow  # call: (Thread* self, Method *meth)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception
#ifdef ASSIST_DEBUGGER
#endif

   /*
    * Common code for handling a return instruction.
    *
    * This does not return.
    */

common_returnFromMethod:
.LreturnNew:

   /*
    * Inline common periodic checks
    */

    movl        rGLUE, rINST            # %ecx<- pMterpGlue
    movl        offGlue_pSelfSuspendCount(rINST), %edx # %ebx<- pSuspendCount (int)
    movl        offGlue_pDebuggerActive(rINST), %eax # %eax<- pDebuggerActive
    movl        (%eax), %eax            # %eax<- get debuggerActive (boolean)
    and         $7, %eax               # %eax<- mask for boolean (just how many bits does it take?)
    cmp         $0, (%edx)             # check if suspend is pending
    jne         2f                      # handle suspend
    movl        offGlue_pActiveProfilers(rINST), %edx # %edx<- activeProfilers (int)
    or          (%edx), %eax            # %eax<- merge activeProfilers and debuggerActive
    cmp         $0, %eax               # check for debuggerActive
    jne         3f                      # debugger or profiler active; switch interp
    jmp         4f
2:                                      # check suspended
    movl        offGlue_self(rINST), %eax# %eax<- glue->self
    movl        %eax, -12(%esp)         # push parameter boolean
    lea         -12(%esp), %esp
    call        dvmCheckSuspendPending  # call: (Thread* self)
                                        # return: bool
    lea         12(%esp), %esp
    jmp         4f
3:                                      # debugger/profiler enabled, bail out
    movl        $kInterpEntryInstr, offGlue_entryPoint(rINST) # glue->entryPoint<- reentry type
    movl        $1, %edx               # switch to interp<- true
    jmp         common_gotoBail         # bail


   /*
    * Get save area; rGLUE is %ebx, rFP is %eax
    */
4:
    SAVEAREA_FROM_FP %ecx               # %ecx<- saveArea(old)
    movl        offStackSaveArea_prevFrame(%ecx), rFP # rFP<- saveArea->PrevFrame
    movl        (offStackSaveArea_method - sizeofStackSaveArea)(rFP), %edx # %edx<- method we are returning to
    cmpl        $0, %edx               # check for break frame
    je          common_gotoBail         # bail if break frame
    movl        offStackSaveArea_savedPc(%ecx), rPC # rPC<- saveAreaOld->savedPc
    movl        offGlue_self(rINST), %ecx # %eax<- glue->self
    movl        %edx, offGlue_method(rINST) # glue->method<- newSave->method
    movl        offMethod_clazz(%edx), %edx # %edx<- method->clazz
    FFETCH_ADV  3, %eax                 # %ecx<- next instruction hi; fetch, advance
    movl        rFP, offThread_curFrame(%ecx) # glue->self->curFrame<- rFP
    movl        offClassObject_pDvmDex(%edx), %edx # %edx<- method->clazz->pDvmDex
    movl        %edx, offGlue_methodClassDex(rINST) # glue->pDvmDex<- method->clazz->pDvmDex
    FGETOP_JMP  3, %eax                 # jump to next instruction; getop, jmp

   /*
    * Handle thrown an exception. If the exception processing code
    * returns to us (instead of falling out of the interpreter),
    * continue with whatever the next instruction now happens to be.
    * This does not return.
    */

common_exceptionThrown:
.LexceptionNew:
    movl        $kInterpEntryThrow, %ecx # %ecx<- reentry type
    movl        $0, %edx               # %edx<- pc adjustment
    call        common_periodicChecks
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %edx # %edx<- glue->self
    movl        offThread_exception(%edx), %ecx # %ecx<- pMterpGlue->self->exception
    movl        %edx, -4(%esp)          # push parameter self
    movl        %ecx, -8(%esp)          # push parameter obj
    lea         -8(%esp), %esp
    call        dvmAddTrackedAlloc      # don't allow the exception to be GC'd
                                        # call: (Object* obj, Thread* self)
                                        # return: void
    movl        4(%esp), %edx           # %edx<- glue->self
    movl        $0, offThread_exception(%edx) # glue->self->exception<- NULL

   /*
    * set up args and a local for &fp
    */

    movl        rFP, -4(%esp)           # move fp to stack
    lea         -4(%esp), %esp          # update %esp
    movl        %esp, -4(%esp)          # push parameter 4<- &fp
    movl        $0, -8(%esp)           # push parameter 3<- false
    movl        4(%esp), %edx
    movl        %edx, -12(%esp)         # push parameter 2<- glue->self->exception
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %edx # %edx<- glue->method
    movl        offMethod_insns(%edx), %edx # %edx<- glue->method->insns
    movl        rPC, %ecx               # %ecx<- rPC
    subl        %edx, %ecx              # %ecx<- pc - glue->method->insns
    sar         $1, %ecx               # %ecx<- adjust %ecx for offset
    movl        %ecx, -16(%esp)         # push parameter 1<- glue->method->insns
    movl        8(%esp), %edx
    movl        %edx, -20(%esp)         # push parameter 0<- glue->self
    lea         -20(%esp), %esp

   /*
    * call dvmFindCatchBlock, %eax gets catchRelPc (a code-unit offset)
    */

    call        dvmFindCatchBlock       # call: (Thread* self, int relPc, Object* exception,
                                        #      bool doUnroll, void** newFrame)
                                        # return: int
    lea         32(%esp), %esp
    movl        -12(%esp), rFP          # rFP<- updated rFP
    cmp         $0, %eax               # check for catchRelPc < 0
    jl          .LnotCaughtLocally      # handle not caught locally

   /*
    * fix stack overflow if necessary
    */

    movl        -4(%esp), %ecx          # %ecx<- glue->self
    cmp         $0, offThread_stackOverflowed(%ecx)
    je          1f
    movl        %eax, -4(%esp)          # save %eax for later
    movl        %ecx, -12(%esp)         # push parameter 2 glue->self
    lea         -12(%esp), %esp
    call        dvmCleanupStackOverflow # call: (Thread* self, Object* exception)
                                        # return: void
    lea         12(%esp), %esp
    movl        -4(%esp), %eax          # %eax<- restore %eax
    jmp         2f
1:
    movl        %ecx, -12(%esp)         # push parameter 2 glue->self
2:

   /*
    * adjust locals to match self->curFrame and updated PC
    *
    */

    SAVEAREA_FROM_FP %edx               # %edx<- get newSaveArea
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offStackSaveArea_method(%edx), rPC # rPC<- newMethod
    movl        rPC, offGlue_method(%ecx) # glue->method<- newMethod
    movl        offMethod_clazz(rPC), %edx # %edx<- method->clazz
    movl        offMethod_insns(rPC), rPC # rPC<- method->insns
    movl        offClassObject_pDvmDex(%edx), %edx # %edx<- method->clazz->pDvmDex
    lea         (rPC, %eax, 2), rPC     # rPC<- method->insns + catchRelPc
    movl        %edx, offGlue_methodClassDex(%ecx) # glue->pDvmDex<- method->clazz->pDvmDex
    movl        -8(%esp), %eax
    movl        %eax, -16(%esp)         # push parameter 1 obj
    lea         -16(%esp), %esp
    call        dvmReleaseTrackedAlloc  # call: (Object* obj, Thread* self)
                                        # return: void
    lea         16(%esp), %esp
    FINISH_FETCH %eax
    cmp         $OP_MOVE_EXCEPTION, %eax # is it a move exception
    jne         1f
    movl        -12(%esp), %edx         # %edx<- glue->self
    movl        -8(%esp), %ecx          # %ecx<- exception
    movl        %ecx, offThread_exception(%edx) # restore the exception
1:
    FINISH_JMP  %eax

   /*
    * -8(%esp) = exception, -4(%esp) = self
    */

.LnotCaughtLocally:
    movl        -4(%esp), %edx          # %edx<- glue->self
    movzb       offThread_stackOverflowed(%edx), %eax # %eax<- self->stackOverflowed
    cmp         $0, %eax               # check for stack overflow;
                                        # maybe should use cmpb
    je          1f                      #
    movl        %edx, -12(%esp)         # push parameter 1 glue->self
    lea         -12(%esp), %esp
    call        dvmCleanupStackOverflow # call: (Thread* self, Object* exception)
                                        # return: void
    lea         12(%esp), %esp

   /*
    * Release the exception
    * -8(%esp) = exception, -4(%esp) = self
    */
1:
    movl        -8(%esp), %ecx          # %ecx<- exception
    movl        -4(%esp), %edx          # %edx<- glue->self
    movl        %ecx, offThread_exception(%edx) # glue->self<- exception
    lea         -8(%esp), %esp
    call        dvmReleaseTrackedAlloc  # call: (Object* obj, Thread* self)
                                        # return: void
    lea         8(%esp), %esp
    movl        $0, %edx               # switch to interp<- false
    jmp         common_gotoBail         # bail

   /*
    * After returning from a "glued" function, pull out the updated
    * values and start executing at the next instruction.
    */

common_resumeAfterGlueCall:
    LOAD_PC_FP_FROM_GLUE                # pull rPC and rFP out of glue
    FINISH_A                            # jump to next instruction

   /*
    * For debugging, cause an immediate fault.
    */

common_abort:
    jmp         .LdeadFood

.LdeadFood:
.int 0xdeadf00d

   /*
    * Invalid array index.
    */

common_errArrayIndex:
    EXPORT_PC
    movl        $.LstrArrayIndexException, -8(%esp) # push parameter description
    movl        $0, -4(%esp)           # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * Invalid array value.
    */

common_errArrayStore:
    EXPORT_PC
    movl        $.LstrArrayStoreException, -8(%esp) # push parameter description
    movl        $0, -4(%esp)           # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * Integer divide or mod by zero.
    */

common_errDivideByZero:
    EXPORT_PC
    movl        $.LstrArithmeticException, -8(%esp) # push parameter description
    movl        $.LstrDivideByZero, -4(%esp) # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * Attempt to allocate an array with a negative size.
    */

common_errNegativeArraySize:
    EXPORT_PC
    movl        $.LstrNegativeArraySizeException, -8(%esp) # push parameter description
    movl        $0, -4(%esp)           # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * Invocation of a non-existent method.
    */

common_errNoSuchMethod:
    EXPORT_PC
    movl        $.LstrNoSuchMethodError, -8(%esp) # push parameter description
    movl        $0, -4(%esp)           # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * Unexpected null object.
    */

common_errNullObject:
    EXPORT_PC
    movl        $.LstrNullPointerException, -8(%esp) # push parameter description
    movl        $0, -4(%esp)           # push parameter msg paramter
    lea         -8(%esp), %esp
    call        dvmThrowException       # call: (const char* exceptionDescriptor, const char* msg)
                                        # return: void
    lea         8(%esp), %esp
    jmp         common_exceptionThrown  # handle exception

   /*
    * String references
    */

    .align 4
    .section .rodata
.LstrArithmeticException:
    .asciz "Ljava/lang/ArithmeticException;"
.LstrArrayIndexException:
    .asciz "Ljava/lang/ArrayIndexOutOfBoundsException;"
.LstrArrayStoreException:
    .asciz "Ljava/lang/ArrayStoreException;"
.LstrClassCastException:
    .asciz "Ljava/lang/ClassCastException;"
.LstrDivideByZero:
    .asciz "divide by zero"
.LstrInstantiationError:
    .asciz "Ljava/lang/InstantiationError;"
.LstrNegativeArraySizeException:
    .asciz "Ljava/lang/NegativeArraySizeException;"
.LstrNoSuchMethodError:
    .asciz "Ljava/lang/NoSuchMethodError;"
.LstrNullPointerException:
    .asciz "Ljava/lang/NullPointerException;"
.LstrExceptionNotCaughtLocally:
    .asciz "Exception %s from %s:%d not caught locally\n"

