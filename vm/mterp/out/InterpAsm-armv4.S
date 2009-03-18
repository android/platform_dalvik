/*
 * This file was generated automatically by gen-mterp.py for 'armv4'.
 *
 * --> DO NOT EDIT <--
 */

/* File: armv5te/header.S */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * ARMv5 definitions and declarations.
 */

/*
ARM EABI general notes:

r0-r3 hold first 4 args to a method; they are not preserved across method calls
r4-r8 are available for general use
r9 is given special treatment in some situations, but not for us
r10 (sl) seems to be generally available
r11 (fp) is used by gcc (unless -fomit-frame-pointer is set)
r12 (ip) is scratch -- not preserved across method calls
r13 (sp) should be managed carefully in case a signal arrives
r14 (lr) must be preserved
r15 (pc) can be tinkered with directly

r0 holds returns of <= 4 bytes
r0-r1 hold returns of 8 bytes, low word in r0

Callee must save/restore r4+ (except r12) if it modifies them.

Stack is "full descending".  Only the arguments that don't fit in the first 4
registers are placed on the stack.  "sp" points at the first stacked argument
(i.e. the 5th arg).

VFP: single-precision results in s0, double-precision results in d0.

In the EABI, "sp" must be 64-bit aligned on entry to a function, and any
64-bit quantities (long long, double) must be 64-bit aligned.
*/

/*
Mterp and ARM notes:

The following registers have fixed assignments:

  reg nick      purpose
  r4  rPC       interpreted program counter, used for fetching instructions
  r5  rFP       interpreted frame pointer, used for accessing locals and args
  r6  rGLUE     MterpGlue pointer
  r7  rIBASE    interpreted instruction base pointer, used for computed goto
  r8  rINST     first 16-bit code unit of current instruction

Macros are provided for common operations.  Each macro MUST emit only
one instruction to make instruction-counting easier.  They MUST NOT alter
unspecified registers or condition codes.
*/

/* single-purpose registers, given names for clarity */
#define rPC     r4
#define rFP     r5
#define rGLUE   r6
#define rIBASE  r7
#define rINST   r8

/* save/restore the PC and/or FP from the glue struct */
#define LOAD_PC_FROM_GLUE()     ldr     rPC, [rGLUE, #offGlue_pc]
#define SAVE_PC_TO_GLUE()       str     rPC, [rGLUE, #offGlue_pc]
#define LOAD_FP_FROM_GLUE()     ldr     rFP, [rGLUE, #offGlue_fp]
#define SAVE_FP_TO_GLUE()       str     rFP, [rGLUE, #offGlue_fp]
#define LOAD_PC_FP_FROM_GLUE()  ldmia   rGLUE, {rPC, rFP}
#define SAVE_PC_FP_TO_GLUE()    stmia   rGLUE, {rPC, rFP}

/*
 * "export" the PC to the stack frame, f/b/o future exception objects.  Must
 * be done *before* something calls dvmThrowException.
 *
 * In C this is "SAVEAREA_FROM_FP(fp)->xtra.currentPc = pc", i.e.
 * fp - sizeof(StackSaveArea) + offsetof(SaveArea, xtra.currentPc)
 *
 * It's okay to do this more than once.
 */
#define EXPORT_PC() \
    str     rPC, [rFP, #(-sizeofStackSaveArea + offStackSaveArea_currentPc)]

/*
 * Given a frame pointer, find the stack save area.
 *
 * In C this is "((StackSaveArea*)(_fp) -1)".
 */
#define SAVEAREA_FROM_FP(_reg, _fpreg) \
    sub     _reg, _fpreg, #sizeofStackSaveArea

/*
 * Fetch the next instruction from rPC into rINST.  Does not advance rPC.
 */
#define FETCH_INST()            ldrh    rINST, [rPC]

/*
 * Fetch the next instruction from the specified offset.  Advances rPC
 * to point to the next instruction.  "_count" is in 16-bit code units.
 *
 * Because of the limited size of immediate constants on ARM, this is only
 * suitable for small forward movements (i.e. don't try to implement "goto"
 * with this).
 *
 * This must come AFTER anything that can throw an exception, or the
 * exception catch may miss.  (This also implies that it must come after
 * EXPORT_PC().)
 */
#define FETCH_ADVANCE_INST(_count) ldrh    rINST, [rPC, #(_count*2)]!

/*
 * Fetch the next instruction from an offset specified by _reg.  Updates
 * rPC to point to the next instruction.  "_reg" must specify the distance
 * in bytes, *not* 16-bit code units, and may be a signed value.
 *
 * We want to write "ldrh rINST, [rPC, _reg, lsl #2]!", but some of the
 * bits that hold the shift distance are used for the half/byte/sign flags.
 * In some cases we can pre-double _reg for free, so we require a byte offset
 * here.
 */
#define FETCH_ADVANCE_INST_RB(_reg) ldrh    rINST, [rPC, _reg]!

/*
 * Fetch a half-word code unit from an offset past the current PC.  The
 * "_count" value is in 16-bit code units.  Does not advance rPC.
 *
 * The "_S" variant works the same but treats the value as signed.
 */
#define FETCH(_reg, _count)     ldrh    _reg, [rPC, #(_count*2)]
#define FETCH_S(_reg, _count)   ldrsh   _reg, [rPC, #(_count*2)]

/*
 * Fetch one byte from an offset past the current PC.  Pass in the same
 * "_count" as you would for FETCH, and an additional 0/1 indicating which
 * byte of the halfword you want (lo/hi).
 */
#define FETCH_B(_reg, _count, _byte) ldrb     _reg, [rPC, #(_count*2+_byte)]

/*
 * Put the instruction's opcode field into the specified register.
 */
#define GET_INST_OPCODE(_reg)   and     _reg, rINST, #255

/*
 * Begin executing the opcode in _reg.  Because this only jumps within the
 * interpreter, we don't have to worry about pre-ARMv5 THUMB interwork.
 */
#define GOTO_OPCODE(_reg)       add     pc, rIBASE, _reg, lsl #6

/*
 * Get/set the 32-bit value from a Dalvik register.
 */
#define GET_VREG(_reg, _vreg)   ldr     _reg, [rFP, _vreg, lsl #2]
#define SET_VREG(_reg, _vreg)   str     _reg, [rFP, _vreg, lsl #2]

/*
 * This is a #include, not a %include, because we want the C pre-processor
 * to expand the macros into assembler assignment statements.
 */
#include "../common/asm-constants.h"


/* File: armv5te/platform.S */
/*
 * ===========================================================================
 *  CPU-version-specific defines
 * ===========================================================================
 */

/*
 * Macro for "LDR PC,xxx", which is not allowed pre-ARMv5.  Essentially a
 * one-way branch.
 *
 * May modify IP.  Does not modify LR.
 */
.macro  LDR_PC source
    ldr     pc, \source
.endm

/*
 * Macro for "MOV LR,PC / LDR PC,xxx", which is not allowed pre-ARMv5.
 * Jump to subroutine.
 *
 * May modify IP and LR.
 */
.macro  LDR_PC_LR source
    mov     lr, pc
    ldr     pc, \source
.endm

/*
 * Macro for "LDMFD SP!, {...regs...,PC}".
 *
 * May modify IP and LR.
 */
.macro  LDMFD_PC regs
    ldmfd   sp!, {\regs,pc}
.endm


/* File: armv5te/entry.S */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Interpreter entry point.
 */

/*
 * We don't have formal stack frames, so gdb scans upward in the code
 * to find the start of the function (a label with the %function type),
 * and then looks at the next few instructions to figure out what
 * got pushed onto the stack.  From this it figures out how to restore
 * the registers, including PC, for the previous stack frame.  If gdb
 * sees a non-function label, it stops scanning, so either we need to
 * have nothing but assembler-local labels between the entry point and
 * the break, or we need to fake it out.
 *
 * When this is defined, we add some stuff to make gdb less confused.
 */
#define ASSIST_DEBUGGER 1

    .text
    .align  2
    .global dvmMterpStdRun
    .type   dvmMterpStdRun, %function

/*
 * On entry:
 *  r0  MterpGlue* glue
 *
 * This function returns a boolean "changeInterp" value.  The return comes
 * via a call to dvmMterpStdBail().
 */
dvmMterpStdRun:
#define MTERP_ENTRY1 \
    .save {r4-r10,fp,lr}; \
    stmfd   sp!, {r4-r10,fp,lr}         @ save 9 regs
#define MTERP_ENTRY2 \
    .pad    #4; \
    sub     sp, sp, #4                  @ align 64

    .fnstart
    MTERP_ENTRY1
    MTERP_ENTRY2

    /* save stack pointer, add magic word for debuggerd */
    str     sp, [r0, #offGlue_bailPtr]  @ save SP for eventual return

    /* set up "named" registers, figure out entry point */
    mov     rGLUE, r0                   @ set rGLUE
    ldrb    r1, [r0, #offGlue_entryPoint]   @ InterpEntry enum is char
    LOAD_PC_FP_FROM_GLUE()              @ load rPC and rFP from "glue"
    adr     rIBASE, dvmAsmInstructionStart  @ set rIBASE
    cmp     r1, #kInterpEntryInstr      @ usual case?
    bne     .Lnot_instr                 @ no, handle it

    /* start executing the instruction at rPC */
    FETCH_INST()                        @ load rINST from rPC
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

.Lnot_instr:
    cmp     r1, #kInterpEntryReturn     @ were we returning from a method?
    beq     common_returnFromMethod

.Lnot_return:
    cmp     r1, #kInterpEntryThrow      @ were we throwing an exception?
    beq     common_exceptionThrown

.Lbad_arg:
    ldr     r0, strBadEntryPoint
    @ r1 holds value of entryPoint
    bl      printf
    bl      dvmAbort
    .fnend


    .global dvmMterpStdBail
    .type   dvmMterpStdBail, %function

/*
 * Restore the stack pointer and PC from the save point established on entry.
 * This is essentially the same as a longjmp, but should be cheaper.  The
 * last instruction causes us to return to whoever called dvmMterpStdRun.
 *
 * We pushed some registers on the stack in dvmMterpStdRun, then saved
 * SP and LR.  Here we restore SP, restore the registers, and then restore
 * LR to PC.
 *
 * On entry:
 *  r0  MterpGlue* glue
 *  r1  bool changeInterp
 */
dvmMterpStdBail:
    ldr     sp, [r0, #offGlue_bailPtr]      @ sp<- saved SP
    mov     r0, r1                          @ return the changeInterp value
    add     sp, sp, #4                      @ un-align 64
    LDMFD_PC "r4-r10,fp"                    @ restore 9 regs and return


/*
 * String references.
 */
strBadEntryPoint:
    .word   .LstrBadEntryPoint



    .global dvmAsmInstructionStart
    .type   dvmAsmInstructionStart, %function
dvmAsmInstructionStart = .L_OP_NOP
    .text

/* ------------------------------ */
    .balign 64
.L_OP_NOP: /* 0x00 */
/* File: armv5te/OP_NOP.S */
    FETCH_ADVANCE_INST(1)               @ advance to next instr, load rINST
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    GOTO_OPCODE(ip)                     @ execute it

#ifdef ASSIST_DEBUGGER
    /* insert fake function header to help gdb find the stack frame */
    .type   dalvik_inst, %function
dalvik_inst:
    .fnstart
    MTERP_ENTRY1
    MTERP_ENTRY2
    .fnend
#endif


/* ------------------------------ */
    .balign 64
.L_OP_MOVE: /* 0x01 */
/* File: armv5te/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    mov     r1, rINST, lsr #12          @ r1<- B from 15:12
    mov     r0, rINST, lsr #8           @ r0<- A from 11:8
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[B]
    and     r0, r0, #15
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    SET_VREG(r2, r0)                    @ fp[A]<- r2
    GOTO_OPCODE(ip)                     @ execute next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_FROM16: /* 0x02 */
/* File: armv5te/OP_MOVE_FROM16.S */
    /* for: move/from16, move-object/from16 */
    /* op vAA, vBBBB */
    FETCH(r1, 1)                        @ r1<- BBBB
    mov     r0, rINST, lsr #8           @ r0<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[BBBB]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r0)                    @ fp[AA]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_16: /* 0x03 */
/* File: armv5te/OP_MOVE_16.S */
    /* for: move/16, move-object/16 */
    /* op vAAAA, vBBBB */
    FETCH(r1, 2)                        @ r1<- BBBB
    FETCH(r0, 1)                        @ r0<- AAAA
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[BBBB]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r0)                    @ fp[AAAA]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE: /* 0x04 */
/* File: armv5te/OP_MOVE_WIDE.S */
    /* move-wide vA, vB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r2, r2, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- fp[B]
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r2, {r0-r1}                 @ fp[A]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_FROM16: /* 0x05 */
/* File: armv5te/OP_MOVE_WIDE_FROM16.S */
    /* move-wide/from16 vAA, vBBBB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    FETCH(r3, 1)                        @ r3<- BBBB
    mov     r2, rINST, lsr #8           @ r2<- AA
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[BBBB]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[AA]
    ldmia   r3, {r0-r1}                 @ r0/r1<- fp[BBBB]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r2, {r0-r1}                 @ fp[AA]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_16: /* 0x06 */
/* File: armv5te/OP_MOVE_WIDE_16.S */
    /* move-wide/16 vAAAA, vBBBB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    FETCH(r3, 2)                        @ r3<- BBBB
    FETCH(r2, 1)                        @ r2<- AAAA
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[BBBB]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[AAAA]
    ldmia   r3, {r0-r1}                 @ r0/r1<- fp[BBBB]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r2, {r0-r1}                 @ fp[AAAA]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT: /* 0x07 */
/* File: armv5te/OP_MOVE_OBJECT.S */
/* File: armv5te/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    mov     r1, rINST, lsr #12          @ r1<- B from 15:12
    mov     r0, rINST, lsr #8           @ r0<- A from 11:8
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[B]
    and     r0, r0, #15
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    SET_VREG(r2, r0)                    @ fp[A]<- r2
    GOTO_OPCODE(ip)                     @ execute next instruction



/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_FROM16: /* 0x08 */
/* File: armv5te/OP_MOVE_OBJECT_FROM16.S */
/* File: armv5te/OP_MOVE_FROM16.S */
    /* for: move/from16, move-object/from16 */
    /* op vAA, vBBBB */
    FETCH(r1, 1)                        @ r1<- BBBB
    mov     r0, rINST, lsr #8           @ r0<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[BBBB]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r0)                    @ fp[AA]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_16: /* 0x09 */
/* File: armv5te/OP_MOVE_OBJECT_16.S */
/* File: armv5te/OP_MOVE_16.S */
    /* for: move/16, move-object/16 */
    /* op vAAAA, vBBBB */
    FETCH(r1, 2)                        @ r1<- BBBB
    FETCH(r0, 1)                        @ r0<- AAAA
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[BBBB]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r0)                    @ fp[AAAA]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT: /* 0x0a */
/* File: armv5te/OP_MOVE_RESULT.S */
    /* for: move-result, move-result-object */
    /* op vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    ldr     r0, [rGLUE, #offGlue_retval]    @ r0<- glue->retval.i
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[AA]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_WIDE: /* 0x0b */
/* File: armv5te/OP_MOVE_RESULT_WIDE.S */
    /* move-result-wide vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    add     r3, rGLUE, #offGlue_retval  @ r3<- &glue->retval
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[AA]
    ldmia   r3, {r0-r1}                 @ r0/r1<- retval.j
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r2, {r0-r1}                 @ fp[AA]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_OBJECT: /* 0x0c */
/* File: armv5te/OP_MOVE_RESULT_OBJECT.S */
/* File: armv5te/OP_MOVE_RESULT.S */
    /* for: move-result, move-result-object */
    /* op vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    ldr     r0, [rGLUE, #offGlue_retval]    @ r0<- glue->retval.i
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[AA]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_MOVE_EXCEPTION: /* 0x0d */
/* File: armv5te/OP_MOVE_EXCEPTION.S */
    /* move-exception vAA */
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    mov     r2, rINST, lsr #8           @ r2<- AA
    ldr     r3, [r0, #offThread_exception]  @ r3<- dvmGetException bypass
    mov     r1, #0                      @ r1<- 0
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    SET_VREG(r3, r2)                    @ fp[AA]<- exception obj
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offThread_exception]  @ dvmClearException bypass
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_RETURN_VOID: /* 0x0e */
/* File: armv5te/OP_RETURN_VOID.S */
    b       common_returnFromMethod


/* ------------------------------ */
    .balign 64
.L_OP_RETURN: /* 0x0f */
/* File: armv5te/OP_RETURN.S */
    /*
     * Return a 32-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     *
     * for: return, return-object
     */
    /* op vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    GET_VREG(r0, r2)                    @ r0<- vAA
    str     r0, [rGLUE, #offGlue_retval] @ retval.i <- vAA
    b       common_returnFromMethod


/* ------------------------------ */
    .balign 64
.L_OP_RETURN_WIDE: /* 0x10 */
/* File: armv5te/OP_RETURN_WIDE.S */
    /*
     * Return a 64-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     */
    /* return-wide vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[AA]
    add     r3, rGLUE, #offGlue_retval  @ r3<- &glue->retval
    ldmia   r2, {r0-r1}                 @ r0/r1 <- vAA/vAA+1
    stmia   r3, {r0-r1}                 @ retval<- r0/r1
    b       common_returnFromMethod


/* ------------------------------ */
    .balign 64
.L_OP_RETURN_OBJECT: /* 0x11 */
/* File: armv5te/OP_RETURN_OBJECT.S */
/* File: armv5te/OP_RETURN.S */
    /*
     * Return a 32-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     *
     * for: return, return-object
     */
    /* op vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    GET_VREG(r0, r2)                    @ r0<- vAA
    str     r0, [rGLUE, #offGlue_retval] @ retval.i <- vAA
    b       common_returnFromMethod



/* ------------------------------ */
    .balign 64
.L_OP_CONST_4: /* 0x12 */
/* File: armv5te/OP_CONST_4.S */
    /* const/4 vA, #+B */
    mov     r1, rINST, lsl #16          @ r1<- Bxxx0000
    mov     r0, rINST, lsr #8           @ r0<- A+
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mov     r1, r1, asr #28             @ r1<- sssssssB (sign-extended)
    and     r0, r0, #15
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    SET_VREG(r1, r0)                    @ fp[A]<- r1
    GOTO_OPCODE(ip)                     @ execute next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_16: /* 0x13 */
/* File: armv5te/OP_CONST_16.S */
    /* const/16 vAA, #+BBBB */
    FETCH_S(r0, 1)                      @ r0<- ssssBBBB (sign-extended)
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r0, r3)                    @ vAA<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST: /* 0x14 */
/* File: armv5te/OP_CONST.S */
    /* const vAA, #+BBBBbbbb */
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH(r0, 1)                        @ r0<- bbbb (low)
    FETCH(r1, 2)                        @ r1<- BBBB (high)
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    orr     r0, r0, r1, lsl #16         @ r0<- BBBBbbbb
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r3)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_HIGH16: /* 0x15 */
/* File: armv5te/OP_CONST_HIGH16.S */
    /* const/high16 vAA, #+BBBB0000 */
    FETCH(r0, 1)                        @ r0<- 0000BBBB (zero-extended)
    mov     r3, rINST, lsr #8           @ r3<- AA
    mov     r0, r0, lsl #16             @ r0<- BBBB0000
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r0, r3)                    @ vAA<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_16: /* 0x16 */
/* File: armv5te/OP_CONST_WIDE_16.S */
    /* const-wide/16 vAA, #+BBBB */
    FETCH_S(r0, 1)                      @ r0<- ssssBBBB (sign-extended)
    mov     r3, rINST, lsr #8           @ r3<- AA
    mov     r1, r0, asr #31             @ r1<- ssssssss
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r3, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_32: /* 0x17 */
/* File: armv5te/OP_CONST_WIDE_32.S */
    /* const-wide/32 vAA, #+BBBBbbbb */
    FETCH(r0, 1)                        @ r0<- 0000bbbb (low)
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_S(r2, 2)                      @ r2<- ssssBBBB (high)
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    orr     r0, r0, r2, lsl #16         @ r0<- BBBBbbbb
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[AA]
    mov     r1, r0, asr #31             @ r1<- ssssssss
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r3, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE: /* 0x18 */
/* File: armv5te/OP_CONST_WIDE.S */
    /* const-wide vAA, #+HHHHhhhhBBBBbbbb */
    FETCH(r0, 1)                        @ r0<- bbbb (low)
    FETCH(r1, 2)                        @ r1<- BBBB (low middle)
    FETCH(r2, 3)                        @ r2<- hhhh (high middle)
    orr     r0, r0, r1, lsl #16         @ r0<- BBBBbbbb (low word)
    FETCH(r3, 4)                        @ r3<- HHHH (high)
    mov     r9, rINST, lsr #8           @ r9<- AA
    orr     r1, r2, r3, lsl #16         @ r1<- HHHHhhhh (high word)
    FETCH_ADVANCE_INST(5)               @ advance rPC, load rINST
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_HIGH16: /* 0x19 */
/* File: armv5te/OP_CONST_WIDE_HIGH16.S */
    /* const-wide/high16 vAA, #+BBBB000000000000 */
    FETCH(r1, 1)                        @ r1<- 0000BBBB (zero-extended)
    mov     r3, rINST, lsr #8           @ r3<- AA
    mov     r0, #0                      @ r0<- 00000000
    mov     r1, r1, lsl #16             @ r1<- BBBB0000
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r3, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING: /* 0x1a */
/* File: armv5te/OP_CONST_STRING.S */
    /* const/string vAA, String@BBBB */
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- glue->methodClassDex
    mov     r9, rINST, lsr #8           @ r9<- AA
    ldr     r2, [r2, #offDvmDex_pResStrings]   @ r2<- dvmDex->pResStrings
    ldr     r0, [r2, r1, lsl #2]        @ r0<- pResStrings[BBBB]
    cmp     r0, #0                      @ not yet resolved?
    beq     .LOP_CONST_STRING_resolve
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING_JUMBO: /* 0x1b */
/* File: armv5te/OP_CONST_STRING_JUMBO.S */
    /* const/string vAA, String@BBBBBBBB */
    FETCH(r0, 1)                        @ r0<- bbbb (low)
    FETCH(r1, 2)                        @ r1<- BBBB (high)
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- glue->methodClassDex
    mov     r9, rINST, lsr #8           @ r9<- AA
    ldr     r2, [r2, #offDvmDex_pResStrings]   @ r2<- dvmDex->pResStrings
    orr     r1, r0, r1, lsl #16         @ r1<- BBBBbbbb
    ldr     r0, [r2, r1, lsl #2]        @ r0<- pResStrings[BBBB]
    cmp     r0, #0
    beq     .LOP_CONST_STRING_JUMBO_resolve
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CONST_CLASS: /* 0x1c */
/* File: armv5te/OP_CONST_CLASS.S */
    /* const/class vAA, Class@BBBB */
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- glue->methodClassDex
    mov     r9, rINST, lsr #8           @ r9<- AA
    ldr     r2, [r2, #offDvmDex_pResClasses]   @ r2<- dvmDex->pResClasses
    ldr     r0, [r2, r1, lsl #2]        @ r0<- pResClasses[BBBB]
    cmp     r0, #0                      @ not yet resolved?
    beq     .LOP_CONST_CLASS_resolve
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_ENTER: /* 0x1d */
/* File: armv5te/OP_MONITOR_ENTER.S */
    /*
     * Synchronize on an object.
     */
    /* monitor-enter vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    GET_VREG(r1, r2)                    @ r1<- vAA (object)
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    cmp     r1, #0                      @ null object?
#ifdef WITH_MONITOR_TRACKING
    EXPORT_PC()                         @ export PC so we can grab stack trace
#endif
    beq     common_errNullObject        @ null object, throw an exception
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      dvmLockObject               @ call(self, obj)
#ifdef WITH_DEADLOCK_PREDICTION /* implies WITH_MONITOR_TRACKING */
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    ldr     r1, [r0, #offThread_exception] @ check for exception
    cmp     r1, #0
    bne     common_exceptionThrown      @ exception raised, bail out
#endif
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_EXIT: /* 0x1e */
/* File: armv5te/OP_MONITOR_EXIT.S */
    /*
     * Unlock an object.
     *
     * Exceptions that occur when unlocking a monitor need to appear as
     * if they happened at the following instruction.  See the Dalvik
     * instruction spec.
     */
    /* monitor-exit vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    EXPORT_PC()                         @ before fetch: export the PC
    GET_VREG(r1, r2)                    @ r1<- vAA (object)
    cmp     r1, #0                      @ null object?
    beq     common_errNullObject        @ yes
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    bl      dvmUnlockObject             @ r0<- success for unlock(self, obj)
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yes, exception is pending
    FETCH_ADVANCE_INST(1)               @ before throw: advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CHECK_CAST: /* 0x1f */
/* File: armv5te/OP_CHECK_CAST.S */
    /*
     * Check to see if a cast from one class to another is allowed.
     */
    /* check-cast vAA, class@BBBB */
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH(r2, 1)                        @ r2<- BBBB
    GET_VREG(r9, r3)                    @ r9<- object
    ldr     r0, [rGLUE, #offGlue_methodClassDex]    @ r0<- pDvmDex
    cmp     r9, #0                      @ is object null?
    ldr     r0, [r0, #offDvmDex_pResClasses]    @ r0<- pDvmDex->pResClasses
    beq     .LOP_CHECK_CAST_okay            @ null obj, cast always succeeds
    ldr     r1, [r0, r2, lsl #2]        @ r1<- resolved class
    ldr     r0, [r9, #offObject_clazz]  @ r0<- obj->clazz
    cmp     r1, #0                      @ have we resolved this before?
    beq     .LOP_CHECK_CAST_resolve         @ not resolved, do it now
.LOP_CHECK_CAST_resolved:
    cmp     r0, r1                      @ same class (trivial success)?
    bne     .LOP_CHECK_CAST_fullcheck       @ no, do full check
.LOP_CHECK_CAST_okay:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_INSTANCE_OF: /* 0x20 */
/* File: armv5te/OP_INSTANCE_OF.S */
    /*
     * Check to see if an object reference is an instance of a class.
     *
     * Most common situation is a non-null object, being compared against
     * an already-resolved class.
     */
    /* instance-of vA, vB, class@CCCC */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB (object)
    and     r9, r9, #15                 @ r9<- A
    cmp     r0, #0                      @ is object null?
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- pDvmDex
    beq     .LOP_INSTANCE_OF_store           @ null obj, not an instance, store r0
    FETCH(r3, 1)                        @ r3<- CCCC
    ldr     r2, [r2, #offDvmDex_pResClasses]    @ r2<- pDvmDex->pResClasses
    ldr     r1, [r2, r3, lsl #2]        @ r1<- resolved class
    ldr     r0, [r0, #offObject_clazz]  @ r0<- obj->clazz
    cmp     r1, #0                      @ have we resolved this before?
    beq     .LOP_INSTANCE_OF_resolve         @ not resolved, do it now
.LOP_INSTANCE_OF_resolved: @ r0=obj->clazz, r1=resolved class
    cmp     r0, r1                      @ same class (trivial success)?
    beq     .LOP_INSTANCE_OF_trivial         @ yes, trivial finish
    b       .LOP_INSTANCE_OF_fullcheck       @ no, do full check

/* ------------------------------ */
    .balign 64
.L_OP_ARRAY_LENGTH: /* 0x21 */
/* File: armv5te/OP_ARRAY_LENGTH.S */
    /*
     * Return the length of an array.
     */
    mov     r1, rINST, lsr #12          @ r1<- B
    mov     r2, rINST, lsr #8           @ r2<- A+
    GET_VREG(r0, r1)                    @ r0<- vB (object ref)
    and     r2, r2, #15                 @ r2<- A
    cmp     r0, #0                      @ is object null?
    beq     common_errNullObject        @ yup, fail
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- array length
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r3, r2)                    @ vB<- length
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_NEW_INSTANCE: /* 0x22 */
/* File: armv5te/OP_NEW_INSTANCE.S */
    /*
     * Create a new instance of a class.
     */
    /* new-instance vAA, class@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResClasses]    @ r3<- pDvmDex->pResClasses
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved class
    EXPORT_PC()                         @ req'd for init, resolve, alloc
    cmp     r0, #0                      @ already resolved?
    beq     .LOP_NEW_INSTANCE_resolve         @ no, resolve it now
.LOP_NEW_INSTANCE_resolved:   @ r0=class
    ldrb    r1, [r0, #offClassObject_status]    @ r1<- ClassStatus enum
    cmp     r1, #CLASS_INITIALIZED      @ has class been initialized?
    bne     .LOP_NEW_INSTANCE_needinit        @ no, init class now
.LOP_NEW_INSTANCE_initialized: @ r0=class
    ldr     r3, [r0, #offClassObject_accessFlags]   @ r3<- clazz->accessFlags
    tst     r3, #(ACC_INTERFACE|ACC_ABSTRACT)   @ abstract or interface?
    mov     r1, #ALLOC_DONT_TRACK       @ flags for alloc call
    beq     .LOP_NEW_INSTANCE_finish          @ concrete class, continue
    b       .LOP_NEW_INSTANCE_abstract        @ fail

/* ------------------------------ */
    .balign 64
.L_OP_NEW_ARRAY: /* 0x23 */
/* File: armv5te/OP_NEW_ARRAY.S */
    /*
     * Allocate an array of objects, specified with the array class
     * and a count.
     *
     * The verifier guarantees that this is an array class, so we don't
     * check for it here.
     */
    /* new-array vA, vB, class@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    FETCH(r2, 1)                        @ r2<- CCCC
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    GET_VREG(r1, r0)                    @ r1<- vB (array length)
    ldr     r3, [r3, #offDvmDex_pResClasses]    @ r3<- pDvmDex->pResClasses
    cmp     r1, #0                      @ check length
    ldr     r0, [r3, r2, lsl #2]        @ r0<- resolved class
    bmi     common_errNegativeArraySize @ negative length, bail
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ req'd for resolve, alloc
    bne     .LOP_NEW_ARRAY_finish          @ resolved, continue
    b       .LOP_NEW_ARRAY_resolve         @ do resolve now

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY: /* 0x24 */
/* File: armv5te/OP_FILLED_NEW_ARRAY.S */
    /*
     * Create a new array with elements filled from registers.
     *
     * for: filled-new-array, filled-new-array/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, type@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResClasses]    @ r3<- pDvmDex->pResClasses
    EXPORT_PC()                         @ need for resolve and alloc
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved class
    mov     r10, rINST, lsr #8          @ r10<- AA or BA
    cmp     r0, #0                      @ already resolved?
    bne     .LOP_FILLED_NEW_ARRAY_continue        @ yes, continue on
8:  ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    mov     r2, #0                      @ r2<- false
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- call(clazz, ref)
    cmp     r0, #0                      @ got null?
    beq     common_exceptionThrown      @ yes, handle exception
    b       .LOP_FILLED_NEW_ARRAY_continue

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY_RANGE: /* 0x25 */
/* File: armv5te/OP_FILLED_NEW_ARRAY_RANGE.S */
/* File: armv5te/OP_FILLED_NEW_ARRAY.S */
    /*
     * Create a new array with elements filled from registers.
     *
     * for: filled-new-array, filled-new-array/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, type@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResClasses]    @ r3<- pDvmDex->pResClasses
    EXPORT_PC()                         @ need for resolve and alloc
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved class
    mov     r10, rINST, lsr #8          @ r10<- AA or BA
    cmp     r0, #0                      @ already resolved?
    bne     .LOP_FILLED_NEW_ARRAY_RANGE_continue        @ yes, continue on
8:  ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    mov     r2, #0                      @ r2<- false
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- call(clazz, ref)
    cmp     r0, #0                      @ got null?
    beq     common_exceptionThrown      @ yes, handle exception
    b       .LOP_FILLED_NEW_ARRAY_RANGE_continue


/* ------------------------------ */
    .balign 64
.L_OP_FILL_ARRAY_DATA: /* 0x26 */
/* File: armv5te/OP_FILL_ARRAY_DATA.S */
    /* fill-array-data vAA, +BBBBBBBB */
    FETCH(r0, 1)                        @ r0<- bbbb (lo)
    FETCH(r1, 2)                        @ r1<- BBBB (hi)
    mov     r3, rINST, lsr #8           @ r3<- AA
    orr     r1, r0, r1, lsl #16         @ r1<- BBBBbbbb
    GET_VREG(r0, r3)                    @ r0<- vAA (array object)
    add     r1, rPC, r1, lsl #1         @ r1<- PC + BBBBbbbb*2 (array data off.)
    EXPORT_PC();
    bl      dvmInterpHandleFillArrayData@ fill the array with predefined data
    cmp     r0, #0                      @ 0 means an exception is thrown
    beq     common_exceptionThrown      @ has exception
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_THROW: /* 0x27 */
/* File: armv5te/OP_THROW.S */
    /*
     * Throw an exception object in the current thread.
     */
    /* throw vAA */
    mov     r2, rINST, lsr #8           @ r2<- AA
    GET_VREG(r1, r2)                    @ r1<- vAA (exception object)
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    cmp     r1, #0                      @ null object?
    beq     common_errNullObject        @ yes, throw an NPE instead
    @ bypass dvmSetException, just store it
    str     r1, [r0, #offThread_exception]  @ thread->exception<- obj
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_GOTO: /* 0x28 */
/* File: armv5te/OP_GOTO.S */
    /*
     * Unconditional branch, 8-bit offset.
     *
     * The branch distance is a signed code-unit offset, which we need to
     * double to get a byte offset.
     */
    /* goto +AA */
    mov     r0, rINST, lsl #16          @ r0<- AAxx0000
    movs    r9, r0, asr #24             @ r9<- ssssssAA (sign-extended)
    mov     r9, r9, lsl #1              @ r9<- byte offset
    bmi     common_backwardBranch       @ backward branch, do periodic checks
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_GOTO_16: /* 0x29 */
/* File: armv5te/OP_GOTO_16.S */
    /*
     * Unconditional branch, 16-bit offset.
     *
     * The branch distance is a signed code-unit offset, which we need to
     * double to get a byte offset.
     */
    /* goto/16 +AAAA */
    FETCH_S(r0, 1)                      @ r0<- ssssAAAA (sign-extended)
    movs    r9, r0, asl #1              @ r9<- byte offset, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_GOTO_32: /* 0x2a */
/* File: armv5te/OP_GOTO_32.S */
    /*
     * Unconditional branch, 32-bit offset.
     *
     * The branch distance is a signed code-unit offset, which we need to
     * double to get a byte offset.
     *
     * Unlike most opcodes, this one is allowed to branch to itself, so
     * our "backward branch" test must be "<=0" instead of "<0".  The ORRS
     * instruction doesn't affect the V flag, so we need to clear it
     * explicitly.
     */
    /* goto/32 +AAAAAAAA */
    FETCH(r0, 1)                        @ r0<- aaaa (lo)
    FETCH(r1, 2)                        @ r1<- AAAA (hi)
    cmp     ip, ip                      @ (clear V flag during stall)
    orrs    r0, r0, r1, lsl #16         @ r0<- AAAAaaaa, check sign
    mov     r9, r0, asl #1              @ r9<- byte offset
    ble     common_backwardBranch       @ backward branch, do periodic checks
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_PACKED_SWITCH: /* 0x2b */
/* File: armv5te/OP_PACKED_SWITCH.S */
    /*
     * Handle a packed-switch or sparse-switch instruction.  In both cases
     * we decode it and hand it off to a helper function.
     *
     * We don't really expect backward branches in a switch statement, but
     * they're perfectly legal, so we check for them here.
     *
     * for: packed-switch, sparse-switch
     */
    /* op vAA, +BBBB */
    FETCH(r0, 1)                        @ r0<- bbbb (lo)
    FETCH(r1, 2)                        @ r1<- BBBB (hi)
    mov     r3, rINST, lsr #8           @ r3<- AA
    orr     r0, r0, r1, lsl #16         @ r0<- BBBBbbbb
    GET_VREG(r1, r3)                    @ r1<- vAA
    add     r0, rPC, r0, lsl #1         @ r0<- PC + BBBBbbbb*2
    bl      dvmInterpHandlePackedSwitch                       @ r0<- code-unit branch offset
    movs    r9, r0, asl #1              @ r9<- branch byte offset, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
    beq     common_backwardBranch       @ (want to use BLE but V is unknown)
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPARSE_SWITCH: /* 0x2c */
/* File: armv5te/OP_SPARSE_SWITCH.S */
/* File: armv5te/OP_PACKED_SWITCH.S */
    /*
     * Handle a packed-switch or sparse-switch instruction.  In both cases
     * we decode it and hand it off to a helper function.
     *
     * We don't really expect backward branches in a switch statement, but
     * they're perfectly legal, so we check for them here.
     *
     * for: packed-switch, sparse-switch
     */
    /* op vAA, +BBBB */
    FETCH(r0, 1)                        @ r0<- bbbb (lo)
    FETCH(r1, 2)                        @ r1<- BBBB (hi)
    mov     r3, rINST, lsr #8           @ r3<- AA
    orr     r0, r0, r1, lsl #16         @ r0<- BBBBbbbb
    GET_VREG(r1, r3)                    @ r1<- vAA
    add     r0, rPC, r0, lsl #1         @ r0<- PC + BBBBbbbb*2
    bl      dvmInterpHandleSparseSwitch                       @ r0<- code-unit branch offset
    movs    r9, r0, asl #1              @ r9<- branch byte offset, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
    beq     common_backwardBranch       @ (want to use BLE but V is unknown)
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_CMPL_FLOAT: /* 0x2d */
/* File: armv5te/OP_CMPL_FLOAT.S */
    /*
     * Compare two floating-point values.  Puts 0, 1, or -1 into the
     * destination register based on the results of the comparison.
     *
     * Provide a "naninst" instruction that puts 1 or -1 into r1 depending
     * on what value we'd like to return when one of the operands is NaN.
     *
     * The operation we're implementing is:
     *   if (x == y)
     *     return 0;
     *   else if (x < y)
     *     return -1;
     *   else if (x > y)
     *     return 1;
     *   else
     *     return {-1,1};  // one or both operands was NaN
     *
     * The straightforward implementation requires 3 calls to functions
     * that return a result in r0.  We can do it with two calls if our
     * EABI library supports __aeabi_cfcmple (only one if we want to check
     * for NaN directly):
     *   check x <= y
     *     if <, return -1
     *     if ==, return 0
     *   check y <= x
     *     if <, return 1
     *   return {-1,1}
     *
     * for: cmpl-float, cmpg-float
     */
    /* op vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r9, r2)                    @ r9<- vBB
    GET_VREG(r10, r3)                   @ r10<- vCC
    mov     r0, r9                      @ copy to arg registers
    mov     r1, r10
    bl      __aeabi_cfcmple             @ cmp <=: C clear if <, Z set if eq
    bhi     .LOP_CMPL_FLOAT_gt_or_nan       @ C set and Z clear, disambiguate
    mvncc   r1, #0                      @ (less than) r1<- -1
    moveq   r1, #0                      @ (equal) r1<- 0, trumps less than
.LOP_CMPL_FLOAT_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CMPG_FLOAT: /* 0x2e */
/* File: armv5te/OP_CMPG_FLOAT.S */
/* File: armv5te/OP_CMPL_FLOAT.S */
    /*
     * Compare two floating-point values.  Puts 0, 1, or -1 into the
     * destination register based on the results of the comparison.
     *
     * Provide a "naninst" instruction that puts 1 or -1 into r1 depending
     * on what value we'd like to return when one of the operands is NaN.
     *
     * The operation we're implementing is:
     *   if (x == y)
     *     return 0;
     *   else if (x < y)
     *     return -1;
     *   else if (x > y)
     *     return 1;
     *   else
     *     return {-1,1};  // one or both operands was NaN
     *
     * The straightforward implementation requires 3 calls to functions
     * that return a result in r0.  We can do it with two calls if our
     * EABI library supports __aeabi_cfcmple (only one if we want to check
     * for NaN directly):
     *   check x <= y
     *     if <, return -1
     *     if ==, return 0
     *   check y <= x
     *     if <, return 1
     *   return {-1,1}
     *
     * for: cmpl-float, cmpg-float
     */
    /* op vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r9, r2)                    @ r9<- vBB
    GET_VREG(r10, r3)                   @ r10<- vCC
    mov     r0, r9                      @ copy to arg registers
    mov     r1, r10
    bl      __aeabi_cfcmple             @ cmp <=: C clear if <, Z set if eq
    bhi     .LOP_CMPG_FLOAT_gt_or_nan       @ C set and Z clear, disambiguate
    mvncc   r1, #0                      @ (less than) r1<- -1
    moveq   r1, #0                      @ (equal) r1<- 0, trumps less than
.LOP_CMPG_FLOAT_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CMPL_DOUBLE: /* 0x2f */
/* File: armv5te/OP_CMPL_DOUBLE.S */
    /*
     * Compare two floating-point values.  Puts 0, 1, or -1 into the
     * destination register based on the results of the comparison.
     *
     * Provide a "naninst" instruction that puts 1 or -1 into r1 depending
     * on what value we'd like to return when one of the operands is NaN.
     *
     * See OP_CMPL_FLOAT for an explanation.
     *
     * For: cmpl-double, cmpg-double
     */
    /* op vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r9, r0, #255                @ r9<- BB
    mov     r10, r0, lsr #8             @ r10<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[BB]
    add     r10, rFP, r10, lsl #2       @ r10<- &fp[CC]
    ldmia   r9, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r10, {r2-r3}                @ r2/r3<- vCC/vCC+1
    bl      __aeabi_cdcmple             @ cmp <=: C clear if <, Z set if eq
    bhi     .LOP_CMPL_DOUBLE_gt_or_nan       @ C set and Z clear, disambiguate
    mvncc   r1, #0                      @ (less than) r1<- -1
    moveq   r1, #0                      @ (equal) r1<- 0, trumps less than
.LOP_CMPL_DOUBLE_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_CMPG_DOUBLE: /* 0x30 */
/* File: armv5te/OP_CMPG_DOUBLE.S */
/* File: armv5te/OP_CMPL_DOUBLE.S */
    /*
     * Compare two floating-point values.  Puts 0, 1, or -1 into the
     * destination register based on the results of the comparison.
     *
     * Provide a "naninst" instruction that puts 1 or -1 into r1 depending
     * on what value we'd like to return when one of the operands is NaN.
     *
     * See OP_CMPL_FLOAT for an explanation.
     *
     * For: cmpl-double, cmpg-double
     */
    /* op vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r9, r0, #255                @ r9<- BB
    mov     r10, r0, lsr #8             @ r10<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[BB]
    add     r10, rFP, r10, lsl #2       @ r10<- &fp[CC]
    ldmia   r9, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r10, {r2-r3}                @ r2/r3<- vCC/vCC+1
    bl      __aeabi_cdcmple             @ cmp <=: C clear if <, Z set if eq
    bhi     .LOP_CMPG_DOUBLE_gt_or_nan       @ C set and Z clear, disambiguate
    mvncc   r1, #0                      @ (less than) r1<- -1
    moveq   r1, #0                      @ (equal) r1<- 0, trumps less than
.LOP_CMPG_DOUBLE_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_CMP_LONG: /* 0x31 */
/* File: armv5te/OP_CMP_LONG.S */
    /*
     * Compare two 64-bit values.  Puts 0, 1, or -1 into the destination
     * register based on the results of the comparison.
     *
     * We load the full values with LDM, but in practice many values could
     * be resolved by only looking at the high word.  This could be made
     * faster or slower by splitting the LDM into a pair of LDRs.
     *
     * If we just wanted to set condition flags, we could do this:
     *  subs    ip, r0, r2
     *  sbcs    ip, r1, r3
     *  subeqs  ip, r0, r2
     * Leaving { <0, 0, >0 } in ip.  However, we have to set it to a specific
     * integer value, which we can do with 2 conditional mov/mvn instructions
     * (set 1, set -1; if they're equal we already have 0 in ip), giving
     * us a constant 5-cycle path plus a branch at the end to the
     * instruction epilogue code.  The multi-compare approach below needs
     * 2 or 3 cycles + branch if the high word doesn't match, 6 + branch
     * in the worst case (the 64-bit values are equal).
     */
    /* cmp-long vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    cmp     r1, r3                      @ compare (vBB+1, vCC+1)
    blt     .LOP_CMP_LONG_less            @ signed compare on high part
    bgt     .LOP_CMP_LONG_greater
    subs    r1, r0, r2                  @ r1<- r0 - r2
    bhi     .LOP_CMP_LONG_greater         @ unsigned compare on low part
    bne     .LOP_CMP_LONG_less
    b       .LOP_CMP_LONG_finish          @ equal; r1 already holds 0

/* ------------------------------ */
    .balign 64
.L_OP_IF_EQ: /* 0x32 */
/* File: armv5te/OP_IF_EQ.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    bne  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_NE: /* 0x33 */
/* File: armv5te/OP_IF_NE.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    beq  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_LT: /* 0x34 */
/* File: armv5te/OP_IF_LT.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    bge  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_GE: /* 0x35 */
/* File: armv5te/OP_IF_GE.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    blt  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_GT: /* 0x36 */
/* File: armv5te/OP_IF_GT.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    ble  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_LE: /* 0x37 */
/* File: armv5te/OP_IF_LE.S */
/* File: armv5te/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r3, r1)                    @ r3<- vB
    GET_VREG(r2, r0)                    @ r2<- vA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, r3                      @ compare (vA, vB)
    bgt  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ yes, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_EQZ: /* 0x38 */
/* File: armv5te/OP_IF_EQZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    bne  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_NEZ: /* 0x39 */
/* File: armv5te/OP_IF_NEZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    beq  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_LTZ: /* 0x3a */
/* File: armv5te/OP_IF_LTZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    bge  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_GEZ: /* 0x3b */
/* File: armv5te/OP_IF_GEZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    blt  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_GTZ: /* 0x3c */
/* File: armv5te/OP_IF_GTZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    ble  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IF_LEZ: /* 0x3d */
/* File: armv5te/OP_IF_LEZ.S */
/* File: armv5te/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    mov     r0, rINST, lsr #8           @ r0<- AA
    GET_VREG(r2, r0)                    @ r2<- vAA
    mov     r9, #4                      @ r0<- BYTE branch dist for not-taken
    cmp     r2, #0                      @ compare (vA, 0)
    bgt  1f                      @ branch to 1 if comparison failed
    FETCH_S(r9, 1)                      @ r9<- branch offset, in code units
    movs    r9, r9, asl #1              @ convert to bytes, check sign
    bmi     common_backwardBranch       @ backward branch, do periodic checks
1:  FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3E: /* 0x3e */
/* File: armv5te/OP_UNUSED_3E.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3F: /* 0x3f */
/* File: armv5te/OP_UNUSED_3F.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_40: /* 0x40 */
/* File: armv5te/OP_UNUSED_40.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_41: /* 0x41 */
/* File: armv5te/OP_UNUSED_41.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_42: /* 0x42 */
/* File: armv5te/OP_UNUSED_42.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_43: /* 0x43 */
/* File: armv5te/OP_UNUSED_43.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_AGET: /* 0x44 */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #2     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldr   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_AGET_WIDE: /* 0x45 */
/* File: armv4/OP_AGET_WIDE.S */
    /*
     * Array get, 64 bits.  vAA <- vBB[vCC].
     *
     * Arrays of long/double are 64-bit aligned, so it's okay to use LDRD.
     */
    /* aget-wide vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #3          @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcc     .LOP_AGET_WIDE_finish          @ okay, continue below
    b       common_errArrayIndex        @ index >= length, bail
    @ May want to swap the order of these two branches depending on how the
    @ branch prediction (if any) handles conditional forward branches vs.
    @ unconditional forward branches.

/* ------------------------------ */
    .balign 64
.L_OP_AGET_OBJECT: /* 0x46 */
/* File: armv5te/OP_AGET_OBJECT.S */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #2     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldr   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_AGET_BOOLEAN: /* 0x47 */
/* File: armv5te/OP_AGET_BOOLEAN.S */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #0     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldrb   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_AGET_BYTE: /* 0x48 */
/* File: armv5te/OP_AGET_BYTE.S */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #0     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldrsb   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_AGET_CHAR: /* 0x49 */
/* File: armv5te/OP_AGET_CHAR.S */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #1     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldrh   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_AGET_SHORT: /* 0x4a */
/* File: armv5te/OP_AGET_SHORT.S */
/* File: armv5te/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #1     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldrsh   r2, [r0, #offArrayObject_contents]  @ r2<- vBB[vCC]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r2, r9)                    @ vAA<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_APUT: /* 0x4b */
/* File: armv5te/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aput, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #2     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r9)                    @ r2<- vAA
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r2, [r0, #offArrayObject_contents]  @ vBB[vCC]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_APUT_WIDE: /* 0x4c */
/* File: armv4/OP_APUT_WIDE.S */
    /*
     * Array put, 64 bits.  vBB[vCC] <- vAA.
     */
    /* aput-wide vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #3          @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    bcc     .LOP_APUT_WIDE_finish          @ okay, continue below
    b       common_errArrayIndex        @ index >= length, bail
    @ May want to swap the order of these two branches depending on how the
    @ branch prediction (if any) handles conditional forward branches vs.
    @ unconditional forward branches.

/* ------------------------------ */
    .balign 64
.L_OP_APUT_OBJECT: /* 0x4d */
/* File: armv5te/OP_APUT_OBJECT.S */
    /*
     * Store an object into an array.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     */
    /* op vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r1, r2)                    @ r1<- vBB (array object)
    GET_VREG(r0, r3)                    @ r0<- vCC (requested index)
    cmp     r1, #0                      @ null array object?
    GET_VREG(r9, r9)                    @ r9<- vAA
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r1, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r10, r1, r0, lsl #2         @ r10<- arrayObj + index*width
    cmp     r0, r3                      @ compare unsigned index, length
    bcc     .LOP_APUT_OBJECT_finish          @ we're okay, continue on
    b       common_errArrayIndex        @ index >= length, bail


/* ------------------------------ */
    .balign 64
.L_OP_APUT_BOOLEAN: /* 0x4e */
/* File: armv5te/OP_APUT_BOOLEAN.S */
/* File: armv5te/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aput, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #0     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r9)                    @ r2<- vAA
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    strb  r2, [r0, #offArrayObject_contents]  @ vBB[vCC]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_APUT_BYTE: /* 0x4f */
/* File: armv5te/OP_APUT_BYTE.S */
/* File: armv5te/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aput, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #0     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r9)                    @ r2<- vAA
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    strb  r2, [r0, #offArrayObject_contents]  @ vBB[vCC]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_APUT_CHAR: /* 0x50 */
/* File: armv5te/OP_APUT_CHAR.S */
/* File: armv5te/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aput, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #1     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r9)                    @ r2<- vAA
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    strh  r2, [r0, #offArrayObject_contents]  @ vBB[vCC]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_APUT_SHORT: /* 0x51 */
/* File: armv5te/OP_APUT_SHORT.S */
/* File: armv5te/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA.
     *
     * Note: using the usual FETCH/and/shift stuff, this fits in exactly 17
     * instructions.  We use a pair of FETCH_Bs instead.
     *
     * for: aput, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    FETCH_B(r2, 1, 0)                   @ r2<- BB
    mov     r9, rINST, lsr #8           @ r9<- AA
    FETCH_B(r3, 1, 1)                   @ r3<- CC
    GET_VREG(r0, r2)                    @ r0<- vBB (array object)
    GET_VREG(r1, r3)                    @ r1<- vCC (requested index)
    cmp     r0, #0                      @ null array object?
    beq     common_errNullObject        @ yes, bail
    ldr     r3, [r0, #offArrayObject_length]    @ r3<- arrayObj->length
    add     r0, r0, r1, lsl #1     @ r0<- arrayObj + index*width
    cmp     r1, r3                      @ compare unsigned index, length
    bcs     common_errArrayIndex        @ index >= length, bail
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r2, r9)                    @ r2<- vAA
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    strh  r2, [r0, #offArrayObject_contents]  @ vBB[vCC]<- r2
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IGET: /* 0x52 */
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_finish
    b       common_exceptionThrown

/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE: /* 0x53 */
/* File: armv4/OP_IGET_WIDE.S */
    /*
     * Wide 32-bit instance field get.
     */
    /* iget-wide vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_WIDE_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method] @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_WIDE_finish
    b       common_exceptionThrown

/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT: /* 0x54 */
/* File: armv5te/OP_IGET_OBJECT.S */
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_OBJECT_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_OBJECT_finish
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BOOLEAN: /* 0x55 */
/* File: armv5te/OP_IGET_BOOLEAN.S */
@include "armv5te/OP_IGET.S" { "load":"ldrb", "sqnum":"1" }
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_BOOLEAN_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_BOOLEAN_finish
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BYTE: /* 0x56 */
/* File: armv5te/OP_IGET_BYTE.S */
@include "armv5te/OP_IGET.S" { "load":"ldrsb", "sqnum":"2" }
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_BYTE_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_BYTE_finish
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IGET_CHAR: /* 0x57 */
/* File: armv5te/OP_IGET_CHAR.S */
@include "armv5te/OP_IGET.S" { "load":"ldrh", "sqnum":"3" }
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_CHAR_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_CHAR_finish
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IGET_SHORT: /* 0x58 */
/* File: armv5te/OP_IGET_SHORT.S */
@include "armv5te/OP_IGET.S" { "load":"ldrsh", "sqnum":"4" }
/* File: armv5te/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IGET_SHORT_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0
    bne     .LOP_IGET_SHORT_finish
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IPUT: /* 0x59 */
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_finish          @ yes, finish up
    b       common_exceptionThrown

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE: /* 0x5a */
/* File: armv4/OP_IPUT_WIDE.S */
    /* iput-wide vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_WIDE_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method] @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_WIDE_finish          @ yes, finish up
    b       common_exceptionThrown

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT: /* 0x5b */
/* File: armv5te/OP_IPUT_OBJECT.S */
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_OBJECT_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_OBJECT_finish          @ yes, finish up
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BOOLEAN: /* 0x5c */
/* File: armv5te/OP_IPUT_BOOLEAN.S */
@include "armv5te/OP_IPUT.S" { "store":"strb", "sqnum":"1" }
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_BOOLEAN_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_BOOLEAN_finish          @ yes, finish up
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BYTE: /* 0x5d */
/* File: armv5te/OP_IPUT_BYTE.S */
@include "armv5te/OP_IPUT.S" { "store":"strb", "sqnum":"2" }
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_BYTE_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_BYTE_finish          @ yes, finish up
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_CHAR: /* 0x5e */
/* File: armv5te/OP_IPUT_CHAR.S */
@include "armv5te/OP_IPUT.S" { "store":"strh", "sqnum":"3" }
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_CHAR_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_CHAR_finish          @ yes, finish up
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_SHORT: /* 0x5f */
/* File: armv5te/OP_IPUT_SHORT.S */
@include "armv5te/OP_IPUT.S" { "store":"strh", "sqnum":"4" }
/* File: armv5te/OP_IPUT.S */
    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    mov     r0, rINST, lsr #12          @ r0<- B
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref CCCC
    ldr     r2, [r3, #offDvmDex_pResFields] @ r2<- pDvmDex->pResFields
    GET_VREG(r9, r0)                    @ r9<- fp[B], the object pointer
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ is resolved entry null?
    bne     .LOP_IPUT_SHORT_finish          @ no, already resolved
8:  ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveInstField         @ r0<- resolved InstField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_IPUT_SHORT_finish          @ yes, finish up
    b       common_exceptionThrown


/* ------------------------------ */
    .balign 64
.L_OP_SGET: /* 0x60 */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_resolve         @ yes, do resolve
.LOP_SGET_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SGET_WIDE: /* 0x61 */
/* File: armv4/OP_SGET_WIDE.S */
    /*
     * 64-bit SGET handler.
     */
    /* sget-wide vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_WIDE_resolve         @ yes, do resolve
.LOP_SGET_WIDE_finish:
    mov     r1, rINST, lsr #8           @ r1<- AA
    add     r0, r0, #offStaticField_value
    ldmia   r0, {r2-r3}                 @ r2/r3<- field value (aligned)
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[AA]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    stmia   r1, {r2-r3}                 @ vAA/vAA+1<- r2/r3
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SGET_OBJECT: /* 0x62 */
/* File: armv5te/OP_SGET_OBJECT.S */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_OBJECT_resolve         @ yes, do resolve
.LOP_SGET_OBJECT_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BOOLEAN: /* 0x63 */
/* File: armv5te/OP_SGET_BOOLEAN.S */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_BOOLEAN_resolve         @ yes, do resolve
.LOP_SGET_BOOLEAN_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BYTE: /* 0x64 */
/* File: armv5te/OP_SGET_BYTE.S */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_BYTE_resolve         @ yes, do resolve
.LOP_SGET_BYTE_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SGET_CHAR: /* 0x65 */
/* File: armv5te/OP_SGET_CHAR.S */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_CHAR_resolve         @ yes, do resolve
.LOP_SGET_CHAR_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SGET_SHORT: /* 0x66 */
/* File: armv5te/OP_SGET_SHORT.S */
/* File: armv5te/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SGET_SHORT_resolve         @ yes, do resolve
.LOP_SGET_SHORT_finish: @ field ptr in r0
    ldr     r1, [r0, #offStaticField_value] @ r1<- field value
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r2)                    @ fp[AA]<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPUT: /* 0x67 */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_resolve         @ yes, do resolve
.LOP_SPUT_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_WIDE: /* 0x68 */
/* File: armv4/OP_SPUT_WIDE.S */
    /*
     * 64-bit SPUT handler.
     */
    /* sput-wide vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    mov     r9, rINST, lsr #8           @ r9<- AA
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_WIDE_resolve         @ yes, do resolve
.LOP_SPUT_WIDE_finish: @ field ptr in r0, AA in r9
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldmia   r9, {r2-r3}                 @ r2/r3<- vAA/vAA+1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    add     r0, r0, #offStaticField_value
    stmia   r0, {r2-r3}                 @ field<- vAA/vAA+1
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_OBJECT: /* 0x69 */
/* File: armv5te/OP_SPUT_OBJECT.S */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_OBJECT_resolve         @ yes, do resolve
.LOP_SPUT_OBJECT_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BOOLEAN: /* 0x6a */
/* File: armv5te/OP_SPUT_BOOLEAN.S */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_BOOLEAN_resolve         @ yes, do resolve
.LOP_SPUT_BOOLEAN_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BYTE: /* 0x6b */
/* File: armv5te/OP_SPUT_BYTE.S */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_BYTE_resolve         @ yes, do resolve
.LOP_SPUT_BYTE_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_CHAR: /* 0x6c */
/* File: armv5te/OP_SPUT_CHAR.S */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_CHAR_resolve         @ yes, do resolve
.LOP_SPUT_CHAR_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_SHORT: /* 0x6d */
/* File: armv5te/OP_SPUT_SHORT.S */
/* File: armv5te/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ is resolved entry null?
    beq     .LOP_SPUT_SHORT_resolve         @ yes, do resolve
.LOP_SPUT_SHORT_finish:   @ field ptr in r0
    mov     r2, rINST, lsr #8           @ r2<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_VREG(r1, r2)                    @ r1<- fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [r0, #offStaticField_value] @ field<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL: /* 0x6e */
/* File: armv5te/OP_INVOKE_VIRTUAL.S */
    /*
     * Handle a virtual method call.
     *
     * for: invoke-virtual, invoke-virtual/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved baseMethod
    .if     (!0)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    bne     .LOP_INVOKE_VIRTUAL_continue        @ yes, continue on
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_VIRTUAL         @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     .LOP_INVOKE_VIRTUAL_continue        @ no, continue
    b       common_exceptionThrown      @ yes, handle exception

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER: /* 0x6f */
/* File: armv5te/OP_INVOKE_SUPER.S */
    /*
     * Handle a "super" method call.
     *
     * for: invoke-super, invoke-super/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    .if     (!0)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    GET_VREG(r2, r10)                   @ r2<- "this" ptr
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved baseMethod
    cmp     r2, #0                      @ null "this"?
    ldr     r9, [rGLUE, #offGlue_method] @ r9<- current method
    beq     common_errNullObject        @ null "this", throw exception
    cmp     r0, #0                      @ already resolved?
    ldr     r9, [r9, #offMethod_clazz]  @ r9<- method->clazz
    EXPORT_PC()                         @ must export for invoke
    bne     .LOP_INVOKE_SUPER_continue        @ resolved, continue on
    b       .LOP_INVOKE_SUPER_resolve         @ do resolve now

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT: /* 0x70 */
/* File: armv5te/OP_INVOKE_DIRECT.S */
    /*
     * Handle a direct method call.
     *
     * (We could defer the "is 'this' pointer null" test to the common
     * method invocation code, and use a flag to indicate that static
     * calls don't count.  If we do this as part of copying the arguments
     * out we could avoiding loading the first arg twice.)
     *
     * for: invoke-direct, invoke-direct/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved methodToCall
    .if     (!0)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    GET_VREG(r2, r10)                   @ r2<- "this" ptr
    beq     .LOP_INVOKE_DIRECT_resolve         @ not resolved, do it now
.LOP_INVOKE_DIRECT_finish:
    cmp     r2, #0                      @ null "this" ref?
    bne     common_invokeMethodNoRange   @ no, continue on
    b       common_errNullObject        @ yes, throw exception

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC: /* 0x71 */
/* File: armv5te/OP_INVOKE_STATIC.S */
    /*
     * Handle a static method call.
     *
     * for: invoke-static, invoke-static/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved methodToCall
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    bne     common_invokeMethodNoRange @ yes, continue on
0:  ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_STATIC          @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     common_invokeMethodNoRange @ no, continue
    b       common_exceptionThrown      @ yes, handle exception


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE: /* 0x72 */
/* File: armv5te/OP_INVOKE_INTERFACE.S */
    /*
     * Handle an interface method call.
     *
     * for: invoke-interface, invoke-interface/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r2, 2)                        @ r2<- FEDC or CCCC
    FETCH(r1, 1)                        @ r1<- BBBB
    .if     (!0)
    and     r2, r2, #15                 @ r2<- C (or stays CCCC)
    .endif
    EXPORT_PC()                         @ must export for invoke
    GET_VREG(r0, r2)                    @ r0<- first arg ("this")
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- methodClassDex
    cmp     r0, #0                      @ null obj?
    ldr     r2, [rGLUE, #offGlue_method]  @ r2<- method
    beq     common_errNullObject        @ yes, fail
    ldr     r0, [r0, #offObject_clazz]  @ r0<- thisPtr->clazz
    bl      dvmFindInterfaceMethodInCache @ r0<- call(class, ref, method, dex)
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yes, handle exception
    b       common_invokeMethodNoRange @ jump to common handler 


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_73: /* 0x73 */
/* File: armv5te/OP_UNUSED_73.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_RANGE: /* 0x74 */
/* File: armv5te/OP_INVOKE_VIRTUAL_RANGE.S */
/* File: armv5te/OP_INVOKE_VIRTUAL.S */
    /*
     * Handle a virtual method call.
     *
     * for: invoke-virtual, invoke-virtual/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved baseMethod
    .if     (!1)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    bne     .LOP_INVOKE_VIRTUAL_RANGE_continue        @ yes, continue on
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_VIRTUAL         @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     .LOP_INVOKE_VIRTUAL_RANGE_continue        @ no, continue
    b       common_exceptionThrown      @ yes, handle exception


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_RANGE: /* 0x75 */
/* File: armv5te/OP_INVOKE_SUPER_RANGE.S */
/* File: armv5te/OP_INVOKE_SUPER.S */
    /*
     * Handle a "super" method call.
     *
     * for: invoke-super, invoke-super/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    .if     (!1)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    GET_VREG(r2, r10)                   @ r2<- "this" ptr
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved baseMethod
    cmp     r2, #0                      @ null "this"?
    ldr     r9, [rGLUE, #offGlue_method] @ r9<- current method
    beq     common_errNullObject        @ null "this", throw exception
    cmp     r0, #0                      @ already resolved?
    ldr     r9, [r9, #offMethod_clazz]  @ r9<- method->clazz
    EXPORT_PC()                         @ must export for invoke
    bne     .LOP_INVOKE_SUPER_RANGE_continue        @ resolved, continue on
    b       .LOP_INVOKE_SUPER_RANGE_resolve         @ do resolve now


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_RANGE: /* 0x76 */
/* File: armv5te/OP_INVOKE_DIRECT_RANGE.S */
/* File: armv5te/OP_INVOKE_DIRECT.S */
    /*
     * Handle a direct method call.
     *
     * (We could defer the "is 'this' pointer null" test to the common
     * method invocation code, and use a flag to indicate that static
     * calls don't count.  If we do this as part of copying the arguments
     * out we could avoiding loading the first arg twice.)
     *
     * for: invoke-direct, invoke-direct/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved methodToCall
    .if     (!1)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    GET_VREG(r2, r10)                   @ r2<- "this" ptr
    beq     .LOP_INVOKE_DIRECT_RANGE_resolve         @ not resolved, do it now
.LOP_INVOKE_DIRECT_RANGE_finish:
    cmp     r2, #0                      @ null "this" ref?
    bne     common_invokeMethodRange   @ no, continue on
    b       common_errNullObject        @ yes, throw exception


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC_RANGE: /* 0x77 */
/* File: armv5te/OP_INVOKE_STATIC_RANGE.S */
/* File: armv5te/OP_INVOKE_STATIC.S */
    /*
     * Handle a static method call.
     *
     * for: invoke-static, invoke-static/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- pDvmDex
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r3, [r3, #offDvmDex_pResMethods]    @ r3<- pDvmDex->pResMethods
    ldr     r0, [r3, r1, lsl #2]        @ r0<- resolved methodToCall
    cmp     r0, #0                      @ already resolved?
    EXPORT_PC()                         @ must export for invoke
    bne     common_invokeMethodRange @ yes, continue on
0:  ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_STATIC          @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     common_invokeMethodRange @ no, continue
    b       common_exceptionThrown      @ yes, handle exception



/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE_RANGE: /* 0x78 */
/* File: armv5te/OP_INVOKE_INTERFACE_RANGE.S */
/* File: armv5te/OP_INVOKE_INTERFACE.S */
    /*
     * Handle an interface method call.
     *
     * for: invoke-interface, invoke-interface/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r2, 2)                        @ r2<- FEDC or CCCC
    FETCH(r1, 1)                        @ r1<- BBBB
    .if     (!1)
    and     r2, r2, #15                 @ r2<- C (or stays CCCC)
    .endif
    EXPORT_PC()                         @ must export for invoke
    GET_VREG(r0, r2)                    @ r0<- first arg ("this")
    ldr     r3, [rGLUE, #offGlue_methodClassDex]    @ r3<- methodClassDex
    cmp     r0, #0                      @ null obj?
    ldr     r2, [rGLUE, #offGlue_method]  @ r2<- method
    beq     common_errNullObject        @ yes, fail
    ldr     r0, [r0, #offObject_clazz]  @ r0<- thisPtr->clazz
    bl      dvmFindInterfaceMethodInCache @ r0<- call(class, ref, method, dex)
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yes, handle exception
    b       common_invokeMethodRange @ jump to common handler 



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_79: /* 0x79 */
/* File: armv5te/OP_UNUSED_79.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_7A: /* 0x7a */
/* File: armv5te/OP_UNUSED_7A.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_NEG_INT: /* 0x7b */
/* File: armv5te/OP_NEG_INT.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    rsb     r0, r0, #0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_NOT_INT: /* 0x7c */
/* File: armv5te/OP_NOT_INT.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mvn     r0, r0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_NEG_LONG: /* 0x7d */
/* File: armv5te/OP_NEG_LONG.S */
/* File: armv5te/unopWide.S */
    /*
     * Generic 64-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0/r1".
     * This could be an ARM instruction or a function call.
     *
     * For: neg-long, not-long, neg-double, long-to-double, double-to-long
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- vAA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    rsbs    r0, r0, #0                           @ optional op; may set condition codes
    rsc     r1, r1, #0                              @ r0/r1<- op, r2-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_NOT_LONG: /* 0x7e */
/* File: armv5te/OP_NOT_LONG.S */
/* File: armv5te/unopWide.S */
    /*
     * Generic 64-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0/r1".
     * This could be an ARM instruction or a function call.
     *
     * For: neg-long, not-long, neg-double, long-to-double, double-to-long
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- vAA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mvn     r0, r0                           @ optional op; may set condition codes
    mvn     r1, r1                              @ r0/r1<- op, r2-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_NEG_FLOAT: /* 0x7f */
/* File: armv5te/OP_NEG_FLOAT.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    add     r0, r0, #0x80000000                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_NEG_DOUBLE: /* 0x80 */
/* File: armv5te/OP_NEG_DOUBLE.S */
/* File: armv5te/unopWide.S */
    /*
     * Generic 64-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0/r1".
     * This could be an ARM instruction or a function call.
     *
     * For: neg-long, not-long, neg-double, long-to-double, double-to-long
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- vAA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    add     r1, r1, #0x80000000                              @ r0/r1<- op, r2-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_LONG: /* 0x81 */
/* File: armv5te/OP_INT_TO_LONG.S */
/* File: armv5te/unopWider.S */
    /*
     * Generic 32bit-to-64bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0", where
     * "result" is a 64-bit quantity in r0/r1.
     *
     * For: int-to-long, int-to-double, float-to-long, float-to-double
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r3)                    @ r0<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mov     r1, r0, asr #31                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vA/vA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_FLOAT: /* 0x82 */
/* File: armv5te/OP_INT_TO_FLOAT.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      __aeabi_i2f                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_DOUBLE: /* 0x83 */
/* File: armv5te/OP_INT_TO_DOUBLE.S */
/* File: armv5te/unopWider.S */
    /*
     * Generic 32bit-to-64bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0", where
     * "result" is a 64-bit quantity in r0/r1.
     *
     * For: int-to-long, int-to-double, float-to-long, float-to-double
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r3)                    @ r0<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      __aeabi_i2d                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vA/vA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_INT: /* 0x84 */
/* File: armv5te/OP_LONG_TO_INT.S */
/* we ignore the high word, making this equivalent to a 32-bit reg move */
/* File: armv5te/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    mov     r1, rINST, lsr #12          @ r1<- B from 15:12
    mov     r0, rINST, lsr #8           @ r0<- A from 11:8
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    GET_VREG(r2, r1)                    @ r2<- fp[B]
    and     r0, r0, #15
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    SET_VREG(r2, r0)                    @ fp[A]<- r2
    GOTO_OPCODE(ip)                     @ execute next instruction



/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_FLOAT: /* 0x85 */
/* File: armv5te/OP_LONG_TO_FLOAT.S */
/* File: armv5te/unopNarrower.S */
    /*
     * Generic 64bit-to-32bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0/r1", where
     * "result" is a 32-bit quantity in r0.
     *
     * For: long-to-float, double-to-int, double-to-float
     *
     * (This would work for long-to-int, but that instruction is actually
     * an exact match for OP_MOVE.)
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    and     r9, r9, #15
    ldmia   r3, {r0-r1}                 @ r0/r1<- vB/vB+1
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_l2f                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_DOUBLE: /* 0x86 */
/* File: armv5te/OP_LONG_TO_DOUBLE.S */
/* File: armv5te/unopWide.S */
    /*
     * Generic 64-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0/r1".
     * This could be an ARM instruction or a function call.
     *
     * For: neg-long, not-long, neg-double, long-to-double, double-to-long
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- vAA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_l2d                              @ r0/r1<- op, r2-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_INT: /* 0x87 */
/* File: armv5te/OP_FLOAT_TO_INT.S */
/* EABI appears to have Java-style conversions of +inf/-inf/NaN */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      __aeabi_f2iz                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


#if 0
@include "armv5te/unop.S" {"instr":"bl      f2i_doconv"}
@break
/*
 * Convert the float in r0 to an int in r0.
 *
 * We have to clip values to int min/max per the specification.  The
 * expected common case is a "reasonable" value that converts directly
 * to modest integer.  The EABI convert function isn't doing this for us.
 */
f2i_doconv:
    stmfd   sp!, {r4, lr}
    mov     r1, #0x4f000000             @ (float)maxint
    mov     r4, r0
    bl      __aeabi_fcmpge              @ is arg >= maxint?
    cmp     r0, #0                      @ nonzero == yes
    mvnne   r0, #0x80000000             @ return maxint (7fffffff)
    ldmnefd sp!, {r4, pc}

    mov     r0, r4                      @ recover arg
    mov     r1, #0xcf000000             @ (float)minint
    bl      __aeabi_fcmple              @ is arg <= minint?
    cmp     r0, #0                      @ nonzero == yes
    movne   r0, #0x80000000             @ return minint (80000000)
    ldmnefd sp!, {r4, pc}

    mov     r0, r4                      @ recover arg
    mov     r1, r4
    bl      __aeabi_fcmpeq              @ is arg == self?
    cmp     r0, #0                      @ zero == no
    ldmeqfd sp!, {r4, pc}               @ return zero for NaN

    mov     r0, r4                      @ recover arg
    bl      __aeabi_f2iz                @ convert float to int
    ldmfd   sp!, {r4, pc}
#endif


/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_LONG: /* 0x88 */
/* File: armv5te/OP_FLOAT_TO_LONG.S */
@include "armv5te/unopWider.S" {"instr":"bl      __aeabi_f2lz"}
/* File: armv5te/unopWider.S */
    /*
     * Generic 32bit-to-64bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0", where
     * "result" is a 64-bit quantity in r0/r1.
     *
     * For: int-to-long, int-to-double, float-to-long, float-to-double
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r3)                    @ r0<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      f2l_doconv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vA/vA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_DOUBLE: /* 0x89 */
/* File: armv5te/OP_FLOAT_TO_DOUBLE.S */
/* File: armv5te/unopWider.S */
    /*
     * Generic 32bit-to-64bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0", where
     * "result" is a 64-bit quantity in r0/r1.
     *
     * For: int-to-long, int-to-double, float-to-long, float-to-double
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r3)                    @ r0<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
                               @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    bl      __aeabi_f2d                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vA/vA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_INT: /* 0x8a */
/* File: armv5te/OP_DOUBLE_TO_INT.S */
/* EABI appears to have Java-style conversions of +inf/-inf/NaN */
/* File: armv5te/unopNarrower.S */
    /*
     * Generic 64bit-to-32bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0/r1", where
     * "result" is a 32-bit quantity in r0.
     *
     * For: long-to-float, double-to-int, double-to-float
     *
     * (This would work for long-to-int, but that instruction is actually
     * an exact match for OP_MOVE.)
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    and     r9, r9, #15
    ldmia   r3, {r0-r1}                 @ r0/r1<- vB/vB+1
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_d2iz                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


#if 0
@include "armv5te/unopNarrower.S" {"instr":"bl      d2i_doconv"}
@break
/*
 * Convert the double in r0/r1 to an int in r0.
 *
 * We have to clip values to int min/max per the specification.  The
 * expected common case is a "reasonable" value that converts directly
 * to modest integer.  The EABI convert function isn't doing this for us.
 */
d2i_doconv:
    stmfd   sp!, {r4, r5, lr}           @ save regs
    ldr     r2, .LOP_DOUBLE_TO_INT_maxlo       @ (double)maxint, lo
    ldr     r3, .LOP_DOUBLE_TO_INT_maxhi       @ (double)maxint, hi
    sub     sp, sp, #4                  @ align for EABI
    mov     r4, r0                      @ save r0
    mov     r5, r1                      @  and r1
    bl      __aeabi_dcmpge              @ is arg >= maxint?
    cmp     r0, #0                      @ nonzero == yes
    mvnne   r0, #0x80000000             @ return maxint (7fffffff)
    bne     1f

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    ldr     r3, .LOP_DOUBLE_TO_INT_min         @ (double)minint, hi
    mov     r2, #0                      @ (double)minint, lo
    bl      __aeabi_dcmple              @ is arg <= minint?
    cmp     r0, #0                      @ nonzero == yes
    movne   r0, #0x80000000             @ return minint (80000000)
    bne     1f

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    mov     r2, r4                      @ compare against self
    mov     r3, r5
    bl      __aeabi_dcmpeq              @ is arg == self?
    cmp     r0, #0                      @ zero == no
    beq     1f                          @ return zero for NaN

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    bl      __aeabi_d2iz                @ convert double to int

1:
    add     sp, sp, #4
    ldmfd   sp!, {r4, r5, pc}

.LOP_DOUBLE_TO_INT_maxlo:
    .word   0xffc00000                  @ maxint, as a double (low word)
.LOP_DOUBLE_TO_INT_maxhi:
    .word   0x41dfffff                  @ maxint, as a double (high word)
.LOP_DOUBLE_TO_INT_min:
    .word   0xc1e00000                  @ minint, as a double (high word)
#endif


/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_LONG: /* 0x8b */
/* File: armv5te/OP_DOUBLE_TO_LONG.S */
@include "armv5te/unopWide.S" {"instr":"bl      __aeabi_d2lz"}
/* File: armv5te/unopWide.S */
    /*
     * Generic 64-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0/r1".
     * This could be an ARM instruction or a function call.
     *
     * For: neg-long, not-long, neg-double, long-to-double, double-to-long
     */
    /* unop vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r3, {r0-r1}                 @ r0/r1<- vAA
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      d2l_doconv                              @ r0/r1<- op, r2-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-13 instructions */




/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_FLOAT: /* 0x8c */
/* File: armv5te/OP_DOUBLE_TO_FLOAT.S */
/* File: armv5te/unopNarrower.S */
    /*
     * Generic 64bit-to-32bit unary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = op r0/r1", where
     * "result" is a 32-bit quantity in r0.
     *
     * For: long-to-float, double-to-int, double-to-float
     *
     * (This would work for long-to-int, but that instruction is actually
     * an exact match for OP_MOVE.)
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[B]
    and     r9, r9, #15
    ldmia   r3, {r0-r1}                 @ r0/r1<- vB/vB+1
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_d2f                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-11 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_BYTE: /* 0x8d */
/* File: armv5te/OP_INT_TO_BYTE.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
    mov     r0, r0, asl #24                           @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mov     r0, r0, asr #24                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_CHAR: /* 0x8e */
/* File: armv5te/OP_INT_TO_CHAR.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
    mov     r0, r0, asl #16                           @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mov     r0, r0, lsr #16                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_SHORT: /* 0x8f */
/* File: armv5te/OP_INT_TO_SHORT.S */
/* File: armv5te/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op r0".
     * This could be an ARM instruction or a function call.
     *
     * for: neg-int, not-int, neg-float, int-to-float, float-to-int,
     *      int-to-byte, int-to-char, int-to-short
     */
    /* unop vA, vB */
    mov     r3, rINST, lsr #12          @ r3<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r3)                    @ r0<- vB
    and     r9, r9, #15
    mov     r0, r0, asl #16                           @ optional op; may set condition codes
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    mov     r0, r0, asr #16                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 9-10 instructions */


/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT: /* 0x90 */
/* File: armv5te/OP_ADD_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    add     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT: /* 0x91 */
/* File: armv5te/OP_SUB_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    sub     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT: /* 0x92 */
/* File: armv5te/OP_MUL_INT.S */
/* must be "mul r0, r1, r0" -- "r0, r0, r1" is illegal */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    mul     r0, r1, r0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT: /* 0x93 */
/* File: armv5te/OP_DIV_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl     __aeabi_idiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT: /* 0x94 */
/* File: armv5te/OP_REM_INT.S */
/* idivmod returns quotient in r0 and remainder in r1 */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_idivmod                              @ r1<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r1, r9)               @ vAA<- r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT: /* 0x95 */
/* File: armv5te/OP_AND_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    and     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_INT: /* 0x96 */
/* File: armv5te/OP_OR_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    orr     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT: /* 0x97 */
/* File: armv5te/OP_XOR_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    eor     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT: /* 0x98 */
/* File: armv5te/OP_SHL_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asl r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT: /* 0x99 */
/* File: armv5te/OP_SHR_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT: /* 0x9a */
/* File: armv5te/OP_USHR_INT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, lsr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG: /* 0x9b */
/* File: armv5te/OP_ADD_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    adds    r0, r0, r2                           @ optional op; may set condition codes
    adc     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG: /* 0x9c */
/* File: armv5te/OP_SUB_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    subs    r0, r0, r2                           @ optional op; may set condition codes
    sbc     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG: /* 0x9d */
/* File: armv5te/OP_MUL_LONG.S */
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
     *
     * Unlike most ARM math operations, multiply instructions have
     * restrictions on using the same register more than once (Rd and Rm
     * cannot be the same).
     */
    /* mul-long vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    mul     ip, r2, r1                  @  ip<- ZxW
    umull   r9, r10, r2, r0             @  r9/r10 <- ZxX
    mla     r2, r0, r3, ip              @  r2<- YxX + (ZxW)
    mov     r0, rINST, lsr #8           @ r0<- AA
    add     r10, r2, r10                @  r10<- r10 + low(ZxW + (YxX))
    add     r0, rFP, r0, lsl #2         @ r0<- &fp[AA]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    b       .LOP_MUL_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG: /* 0x9e */
/* File: armv5te/OP_DIV_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 1
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ldivmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG: /* 0x9f */
/* File: armv5te/OP_REM_LONG.S */
/* ldivmod returns quotient in r0/r1 and remainder in r2/r3 */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 1
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ldivmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r2,r3}     @ vAA/vAA+1<- r2/r3
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG: /* 0xa0 */
/* File: armv5te/OP_AND_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    and     r0, r0, r2                           @ optional op; may set condition codes
    and     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG: /* 0xa1 */
/* File: armv5te/OP_OR_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    orr     r0, r0, r2                           @ optional op; may set condition codes
    orr     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG: /* 0xa2 */
/* File: armv5te/OP_XOR_LONG.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    eor     r0, r0, r2                           @ optional op; may set condition codes
    eor     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG: /* 0xa3 */
/* File: armv5te/OP_SHL_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.
     */
    /* shl-long vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r3, r0, #255                @ r3<- BB
    mov     r0, r0, lsr #8              @ r0<- CC
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[BB]
    GET_VREG(r2, r0)                    @ r2<- vCC
    ldmia   r3, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    and     r2, r2, #63                 @ r2<- r2 & 0x3f
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]

    mov     r1, r1, asl r2              @  r1<- r1 << r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r1, r1, r0, lsr r3          @  r1<- r1 | (r0 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    movpl   r1, r0, asl ip              @  if r2 >= 32, r1<- r0 << (r2-32)
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    b       .LOP_SHL_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG: /* 0xa4 */
/* File: armv5te/OP_SHR_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.
     */
    /* shr-long vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r3, r0, #255                @ r3<- BB
    mov     r0, r0, lsr #8              @ r0<- CC
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[BB]
    GET_VREG(r2, r0)                    @ r2<- vCC
    ldmia   r3, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    and     r2, r2, #63                 @ r0<- r0 & 0x3f
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]

    mov     r0, r0, lsr r2              @  r0<- r2 >> r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r0, r0, r1, asl r3          @  r0<- r0 | (r1 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    movpl   r0, r1, asr ip              @  if r2 >= 32, r0<-r1 >> (r2-32)
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    b       .LOP_SHR_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG: /* 0xa5 */
/* File: armv5te/OP_USHR_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.
     */
    /* ushr-long vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r3, r0, #255                @ r3<- BB
    mov     r0, r0, lsr #8              @ r0<- CC
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[BB]
    GET_VREG(r2, r0)                    @ r2<- vCC
    ldmia   r3, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    and     r2, r2, #63                 @ r0<- r0 & 0x3f
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]

    mov     r0, r0, lsr r2              @  r0<- r2 >> r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r0, r0, r1, asl r3          @  r0<- r0 | (r1 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    movpl   r0, r1, lsr ip              @  if r2 >= 32, r0<-r1 >>> (r2-32)
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    b       .LOP_USHR_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT: /* 0xa6 */
/* File: armv5te/OP_ADD_FLOAT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_fadd                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT: /* 0xa7 */
/* File: armv5te/OP_SUB_FLOAT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_fsub                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT: /* 0xa8 */
/* File: armv5te/OP_MUL_FLOAT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_fmul                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT: /* 0xa9 */
/* File: armv5te/OP_DIV_FLOAT.S */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      __aeabi_fdiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT: /* 0xaa */
/* File: armv5te/OP_REM_FLOAT.S */
/* EABI doesn't define a float remainder function, but libm does */
/* File: armv5te/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.  Note that we
     * *don't* check for (INT_MIN / -1) here, because the ARM math lib
     * handles it correctly.
     *
     * For: add-int, sub-int, mul-int, div-int, rem-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int, add-float, sub-float,
     *      mul-float, div-float, rem-float
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    mov     r3, r0, lsr #8              @ r3<- CC
    and     r2, r0, #255                @ r2<- BB
    GET_VREG(r1, r3)                    @ r1<- vCC
    GET_VREG(r0, r2)                    @ r0<- vBB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif

    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
                               @ optional op; may set condition codes
    bl      fmodf                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 11-14 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE: /* 0xab */
/* File: armv5te/OP_ADD_DOUBLE.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dadd                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE: /* 0xac */
/* File: armv5te/OP_SUB_DOUBLE.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dsub                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE: /* 0xad */
/* File: armv5te/OP_MUL_DOUBLE.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dmul                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE: /* 0xae */
/* File: armv5te/OP_DIV_DOUBLE.S */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ddiv                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE: /* 0xaf */
/* File: armv5te/OP_REM_DOUBLE.S */
/* EABI doesn't define a double remainder function, but libm does */
/* File: armv5te/binopWide.S */
    /*
     * Generic 64-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * for: add-long, sub-long, div-long, rem-long, and-long, or-long,
     *      xor-long, add-double, sub-double, mul-double, div-double,
     *      rem-double
     *
     * IMPORTANT: you may specify "chkzero" or "preinstr" but not both.
     */
    /* binop vAA, vBB, vCC */
    FETCH(r0, 1)                        @ r0<- CCBB
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    add     r2, rFP, r2, lsl #2         @ r2<- &fp[BB]
    add     r3, rFP, r3, lsl #2         @ r3<- &fp[CC]
    ldmia   r2, {r0-r1}                 @ r0/r1<- vBB/vBB+1
    ldmia   r3, {r2-r3}                 @ r2/r3<- vCC/vCC+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      fmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 14-17 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_2ADDR: /* 0xb0 */
/* File: armv5te/OP_ADD_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    add     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT_2ADDR: /* 0xb1 */
/* File: armv5te/OP_SUB_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    sub     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_2ADDR: /* 0xb2 */
/* File: armv5te/OP_MUL_INT_2ADDR.S */
/* must be "mul r0, r1, r0" -- "r0, r0, r1" is illegal */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    mul     r0, r1, r0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_2ADDR: /* 0xb3 */
/* File: armv5te/OP_DIV_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl     __aeabi_idiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_2ADDR: /* 0xb4 */
/* File: armv5te/OP_REM_INT_2ADDR.S */
/* idivmod returns quotient in r0 and remainder in r1 */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_idivmod                              @ r1<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r1, r9)               @ vAA<- r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_2ADDR: /* 0xb5 */
/* File: armv5te/OP_AND_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    and     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_2ADDR: /* 0xb6 */
/* File: armv5te/OP_OR_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    orr     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_2ADDR: /* 0xb7 */
/* File: armv5te/OP_XOR_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    eor     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_2ADDR: /* 0xb8 */
/* File: armv5te/OP_SHL_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asl r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_2ADDR: /* 0xb9 */
/* File: armv5te/OP_SHR_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_2ADDR: /* 0xba */
/* File: armv5te/OP_USHR_INT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, lsr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG_2ADDR: /* 0xbb */
/* File: armv5te/OP_ADD_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    adds    r0, r0, r2                           @ optional op; may set condition codes
    adc     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG_2ADDR: /* 0xbc */
/* File: armv5te/OP_SUB_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    subs    r0, r0, r2                           @ optional op; may set condition codes
    sbc     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG_2ADDR: /* 0xbd */
/* File: armv5te/OP_MUL_LONG_2ADDR.S */
    /*
     * Signed 64-bit integer multiply, "/2addr" version.
     *
     * See OP_MUL_LONG for an explanation.
     *
     * We get a little tight on registers, so to avoid looking up &fp[A]
     * again we stuff it into rINST.
     */
    /* mul-long/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     rINST, rFP, r9, lsl #2      @ rINST<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   rINST, {r0-r1}              @ r0/r1<- vAA/vAA+1
    mul     ip, r2, r1                  @  ip<- ZxW
    umull   r9, r10, r2, r0             @  r9/r10 <- ZxX
    mla     r2, r0, r3, ip              @  r2<- YxX + (ZxW)
    mov     r0, rINST                   @ r0<- &fp[A] (free up rINST)
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    add     r10, r2, r10                @  r10<- r10 + low(ZxW + (YxX))
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r0, {r9-r10}                @ vAA/vAA+1<- r9/r10
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG_2ADDR: /* 0xbe */
/* File: armv5te/OP_DIV_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 1
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ldivmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG_2ADDR: /* 0xbf */
/* File: armv5te/OP_REM_LONG_2ADDR.S */
/* ldivmod returns quotient in r0/r1 and remainder in r2/r3 */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 1
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ldivmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r2,r3}     @ vAA/vAA+1<- r2/r3
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG_2ADDR: /* 0xc0 */
/* File: armv5te/OP_AND_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    and     r0, r0, r2                           @ optional op; may set condition codes
    and     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG_2ADDR: /* 0xc1 */
/* File: armv5te/OP_OR_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    orr     r0, r0, r2                           @ optional op; may set condition codes
    orr     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG_2ADDR: /* 0xc2 */
/* File: armv5te/OP_XOR_LONG_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

    eor     r0, r0, r2                           @ optional op; may set condition codes
    eor     r1, r1, r3                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG_2ADDR: /* 0xc3 */
/* File: armv5te/OP_SHL_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* shl-long/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r2, r3)                    @ r2<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    and     r2, r2, #63                 @ r2<- r2 & 0x3f
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1

    mov     r1, r1, asl r2              @  r1<- r1 << r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r1, r1, r0, lsr r3          @  r1<- r1 | (r0 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    movpl   r1, r0, asl ip              @  if r2 >= 32, r1<- r0 << (r2-32)
    mov     r0, r0, asl r2              @  r0<- r0 << r2
    b       .LOP_SHL_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG_2ADDR: /* 0xc4 */
/* File: armv5te/OP_SHR_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* shr-long/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r2, r3)                    @ r2<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    and     r2, r2, #63                 @ r2<- r2 & 0x3f
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1

    mov     r0, r0, lsr r2              @  r0<- r2 >> r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r0, r0, r1, asl r3          @  r0<- r0 | (r1 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    movpl   r0, r1, asr ip              @  if r2 >= 32, r0<-r1 >> (r2-32)
    mov     r1, r1, asr r2              @  r1<- r1 >> r2
    b       .LOP_SHR_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG_2ADDR: /* 0xc5 */
/* File: armv5te/OP_USHR_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* ushr-long/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r2, r3)                    @ r2<- vB
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    and     r2, r2, #63                 @ r2<- r2 & 0x3f
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1

    mov     r0, r0, lsr r2              @  r0<- r2 >> r2
    rsb     r3, r2, #32                 @  r3<- 32 - r2
    orr     r0, r0, r1, asl r3          @  r0<- r0 | (r1 << (32-r2))
    subs    ip, r2, #32                 @  ip<- r2 - 32
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST
    movpl   r0, r1, lsr ip              @  if r2 >= 32, r0<-r1 >>> (r2-32)
    mov     r1, r1, lsr r2              @  r1<- r1 >>> r2
    b       .LOP_USHR_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT_2ADDR: /* 0xc6 */
/* File: armv5te/OP_ADD_FLOAT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_fadd                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT_2ADDR: /* 0xc7 */
/* File: armv5te/OP_SUB_FLOAT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_fsub                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT_2ADDR: /* 0xc8 */
/* File: armv5te/OP_MUL_FLOAT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_fmul                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT_2ADDR: /* 0xc9 */
/* File: armv5te/OP_DIV_FLOAT_2ADDR.S */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_fdiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT_2ADDR: /* 0xca */
/* File: armv5te/OP_REM_FLOAT_2ADDR.S */
/* EABI doesn't define a float remainder function, but libm does */
/* File: armv5te/binop2addr.S */
    /*
     * Generic 32-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/2addr, sub-int/2addr, mul-int/2addr, div-int/2addr,
     *      rem-int/2addr, and-int/2addr, or-int/2addr, xor-int/2addr,
     *      shl-int/2addr, shr-int/2addr, ushr-int/2addr, add-float/2addr,
     *      sub-float/2addr, mul-float/2addr, div-float/2addr, rem-float/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r3, rINST, lsr #12          @ r3<- B
    and     r9, r9, #15
    GET_VREG(r0, r9)                    @ r0<- vA
    GET_VREG(r1, r3)                    @ r1<- vB
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      fmodf                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE_2ADDR: /* 0xcb */
/* File: armv5te/OP_ADD_DOUBLE_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dadd                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE_2ADDR: /* 0xcc */
/* File: armv5te/OP_SUB_DOUBLE_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dsub                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE_2ADDR: /* 0xcd */
/* File: armv5te/OP_MUL_DOUBLE_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_dmul                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE_2ADDR: /* 0xce */
/* File: armv5te/OP_DIV_DOUBLE_2ADDR.S */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_ddiv                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE_2ADDR: /* 0xcf */
/* File: armv5te/OP_REM_DOUBLE_2ADDR.S */
/* EABI doesn't define a double remainder function, but libm does */
/* File: armv5te/binopWide2addr.S */
    /*
     * Generic 64-bit "/2addr" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0-r1 op r2-r3".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-long/2addr, sub-long/2addr, div-long/2addr, rem-long/2addr,
     *      and-long/2addr, or-long/2addr, xor-long/2addr, add-double/2addr,
     *      sub-double/2addr, mul-double/2addr, div-double/2addr,
     *      rem-double/2addr
     */
    /* binop/2addr vA, vB */
    mov     r9, rINST, lsr #8           @ r9<- A+
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r9, r9, #15
    add     r1, rFP, r1, lsl #2         @ r1<- &fp[B]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[A]
    ldmia   r1, {r2-r3}                 @ r2/r3<- vBB/vBB+1
    ldmia   r9, {r0-r1}                 @ r0/r1<- vAA/vAA+1
    .if 0
    orrs    ip, r2, r3                  @ second arg (r2-r3) is zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(1)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      fmod                              @ result<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0,r1}     @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 12-15 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT16: /* 0xd0 */
/* File: armv5te/OP_ADD_INT_LIT16.S */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    add     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT: /* 0xd1 */
/* File: armv5te/OP_RSUB_INT.S */
/* this op is "rsub-int", but can be thought of as "rsub-int/lit16" */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    rsb     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT16: /* 0xd2 */
/* File: armv5te/OP_MUL_INT_LIT16.S */
/* must be "mul r0, r1, r0" -- "r0, r0, r1" is illegal */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    mul     r0, r1, r0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT16: /* 0xd3 */
/* File: armv5te/OP_DIV_INT_LIT16.S */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    bl     __aeabi_idiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT16: /* 0xd4 */
/* File: armv5te/OP_REM_INT_LIT16.S */
/* idivmod returns quotient in r0 and remainder in r1 */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 1
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    bl      __aeabi_idivmod                              @ r1<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r1, r9)               @ vAA<- r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT16: /* 0xd5 */
/* File: armv5te/OP_AND_INT_LIT16.S */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    and     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT16: /* 0xd6 */
/* File: armv5te/OP_OR_INT_LIT16.S */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    orr     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT16: /* 0xd7 */
/* File: armv5te/OP_XOR_INT_LIT16.S */
/* File: armv5te/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit16, rsub-int, mul-int/lit16, div-int/lit16,
     *      rem-int/lit16, and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    FETCH_S(r1, 1)                      @ r1<- ssssCCCC (sign-extended)
    mov     r2, rINST, lsr #12          @ r2<- B
    mov     r9, rINST, lsr #8           @ r9<- A+
    GET_VREG(r0, r2)                    @ r0<- vB
    and     r9, r9, #15
    .if 0
    cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    eor     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-13 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT8: /* 0xd8 */
/* File: armv5te/OP_ADD_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    add     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT_LIT8: /* 0xd9 */
/* File: armv5te/OP_RSUB_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    rsb     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT8: /* 0xda */
/* File: armv5te/OP_MUL_INT_LIT8.S */
/* must be "mul r0, r1, r0" -- "r0, r0, r1" is illegal */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    mul     r0, r1, r0                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT8: /* 0xdb */
/* File: armv5te/OP_DIV_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 1
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl     __aeabi_idiv                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT8: /* 0xdc */
/* File: armv5te/OP_REM_INT_LIT8.S */
/* idivmod returns quotient in r0 and remainder in r1 */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 1
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    bl      __aeabi_idivmod                              @ r1<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r1, r9)               @ vAA<- r1
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT8: /* 0xdd */
/* File: armv5te/OP_AND_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    and     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT8: /* 0xde */
/* File: armv5te/OP_OR_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    orr     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT8: /* 0xdf */
/* File: armv5te/OP_XOR_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

                               @ optional op; may set condition codes
    eor     r0, r0, r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_LIT8: /* 0xe0 */
/* File: armv5te/OP_SHL_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asl r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_LIT8: /* 0xe1 */
/* File: armv5te/OP_SHR_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, asr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_LIT8: /* 0xe2 */
/* File: armv5te/OP_USHR_INT_LIT8.S */
/* File: armv5te/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = r0 op r1".
     * This could be an ARM instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * If "chkzero" is set to 1, we perform a divide-by-zero check on
     * vCC (r1).  Useful for integer division and modulus.
     *
     * For: add-int/lit8, rsub-int/lit8, mul-int/lit8, div-int/lit8,
     *      rem-int/lit8, and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    FETCH_S(r3, 1)                      @ r3<- ssssCCBB (sign-extended for CC)
    mov     r9, rINST, lsr #8           @ r9<- AA
    and     r2, r3, #255                @ r2<- BB
    GET_VREG(r0, r2)                    @ r0<- vBB
    movs    r1, r3, asr #8              @ r1<- ssssssCC (sign extended)
    .if 0
    @cmp     r1, #0                      @ is second operand zero?
    beq     common_errDivideByZero
    .endif
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST

    and     r1, r1, #31                           @ optional op; may set condition codes
    mov     r0, r0, lsr r1                              @ r0<- op, r0-r3 changed
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)               @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction
    /* 10-12 instructions */



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E3: /* 0xe3 */
/* File: armv5te/OP_UNUSED_E3.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E4: /* 0xe4 */
/* File: armv5te/OP_UNUSED_E4.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E5: /* 0xe5 */
/* File: armv5te/OP_UNUSED_E5.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E6: /* 0xe6 */
/* File: armv5te/OP_UNUSED_E6.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E7: /* 0xe7 */
/* File: armv5te/OP_UNUSED_E7.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E8: /* 0xe8 */
/* File: armv5te/OP_UNUSED_E8.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E9: /* 0xe9 */
/* File: armv5te/OP_UNUSED_E9.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EA: /* 0xea */
/* File: armv5te/OP_UNUSED_EA.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EB: /* 0xeb */
/* File: armv5te/OP_UNUSED_EB.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EC: /* 0xec */
/* File: armv5te/OP_UNUSED_EC.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_ED: /* 0xed */
/* File: armv5te/OP_UNUSED_ED.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_EXECUTE_INLINE: /* 0xee */
/* File: armv5te/OP_EXECUTE_INLINE.S */
    /*
     * Execute a "native inline" instruction.
     *
     * We need to call:
     *  dvmPerformInlineOp4Std(arg0, arg1, arg2, arg3, &retval, ref)
     *
     * The first four args are in r0-r3, but the last two must be pushed
     * onto the stack.
     */
    /* [opt] execute-inline vAA, {vC, vD, vE, vF}, inline@BBBB */
    FETCH(r10, 1)                       @ r10<- BBBB
    add     r1, rGLUE, #offGlue_retval  @ r1<- &glue->retval
    EXPORT_PC()                         @ can throw
    sub     sp, sp, #8                  @ make room for arg(s)
    mov     r0, rINST, lsr #12          @ r0<- B
    str     r1, [sp]                    @ push &glue->retval
    bl      .LOP_EXECUTE_INLINE_continue        @ make call; will return after
    add     sp, sp, #8                  @ pop stack
    cmp     r0, #0                      @ test boolean result of inline
    beq     common_exceptionThrown      @ returned false, handle exception
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EF: /* 0xef */
/* File: armv5te/OP_UNUSED_EF.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_EMPTY: /* 0xf0 */
/* File: armv5te/OP_INVOKE_DIRECT_EMPTY.S */
    /*
     * invoke-direct-empty is a no-op in a "standard" interpreter.
     */
    FETCH_ADVANCE_INST(3)               @ advance to next instr, load rINST
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    GOTO_OPCODE(ip)                     @ execute it

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_F1: /* 0xf1 */
/* File: armv5te/OP_UNUSED_F1.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_IGET_QUICK: /* 0xf2 */
/* File: armv5te/OP_IGET_QUICK.S */
    /* For: iget-quick, iget-object-quick */
    /* op vA, vB, offset@CCCC */
    mov     r2, rINST, lsr #12          @ r2<- B
    GET_VREG(r3, r2)                    @ r3<- object we're operating on
    FETCH(r1, 1)                        @ r1<- field byte offset
    cmp     r3, #0                      @ check object for null
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    beq     common_errNullObject        @ object was null
    ldr     r0, [r3, r1]                @ r0<- obj.field (always 32 bits)
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE_QUICK: /* 0xf3 */
/* File: armv4/OP_IGET_WIDE_QUICK.S */
    /* iget-wide-quick vA, vB, offset@CCCC */
    mov     r2, rINST, lsr #12          @ r2<- B
    GET_VREG(r3, r2)                    @ r3<- object we're operating on
    FETCH(r1, 1)                        @ r1<- field byte offset
    cmp     r3, #0                      @ check object for null
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    beq     common_errNullObject        @ object was null
    add     r9, r3, r1                  @ r9<- object + offset
    ldmia   r9, {r0-r1}                 @ r0/r1<- obj.field (64 bits, aligned)
    and     r2, r2, #15                 @ r2<- A
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r3, rFP, r2, lsl #2         @ r3<- &fp[A]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r3, {r0-r1}                 @ fp[A]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT_QUICK: /* 0xf4 */
/* File: armv5te/OP_IGET_OBJECT_QUICK.S */
/* File: armv5te/OP_IGET_QUICK.S */
    /* For: iget-quick, iget-object-quick */
    /* op vA, vB, offset@CCCC */
    mov     r2, rINST, lsr #12          @ r2<- B
    GET_VREG(r3, r2)                    @ r3<- object we're operating on
    FETCH(r1, 1)                        @ r1<- field byte offset
    cmp     r3, #0                      @ check object for null
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    beq     common_errNullObject        @ object was null
    ldr     r0, [r3, r1]                @ r0<- obj.field (always 32 bits)
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_IPUT_QUICK: /* 0xf5 */
/* File: armv5te/OP_IPUT_QUICK.S */
    /* For: iput-quick, iput-object-quick */
    /* op vA, vB, offset@CCCC */
    mov     r2, rINST, lsr #12          @ r2<- B
    GET_VREG(r3, r2)                    @ r3<- fp[B], the object pointer
    FETCH(r1, 1)                        @ r1<- field byte offset
    cmp     r3, #0                      @ check object for null
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    beq     common_errNullObject        @ object was null
    and     r2, r2, #15
    GET_VREG(r0, r2)                    @ r0<- fp[A]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    str     r0, [r3, r1]                @ obj.field (always 32 bits)<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE_QUICK: /* 0xf6 */
/* File: armv4/OP_IPUT_WIDE_QUICK.S */
    /* iput-wide-quick vA, vB, offset@CCCC */
    mov     r0, rINST, lsr #8           @ r0<- A(+)
    mov     r1, rINST, lsr #12          @ r1<- B
    and     r0, r0, #15
    GET_VREG(r2, r1)                    @ r2<- fp[B], the object pointer
    add     r3, rFP, r0, lsl #2         @ r3<- &fp[A]
    cmp     r2, #0                      @ check object for null
    ldmia   r3, {r0-r1}                 @ r0/r1<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH(r3, 1)                        @ r3<- field byte offset
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r2, r2, r3                  @ r2<- object + byte offset
    stmia   r2, {r0-r1}                 @ obj.field (64 bits, aligned)<- r0/r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT_QUICK: /* 0xf7 */
/* File: armv5te/OP_IPUT_OBJECT_QUICK.S */
/* File: armv5te/OP_IPUT_QUICK.S */
    /* For: iput-quick, iput-object-quick */
    /* op vA, vB, offset@CCCC */
    mov     r2, rINST, lsr #12          @ r2<- B
    GET_VREG(r3, r2)                    @ r3<- fp[B], the object pointer
    FETCH(r1, 1)                        @ r1<- field byte offset
    cmp     r3, #0                      @ check object for null
    mov     r2, rINST, lsr #8           @ r2<- A(+)
    beq     common_errNullObject        @ object was null
    and     r2, r2, #15
    GET_VREG(r0, r2)                    @ r0<- fp[A]
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    str     r0, [r3, r1]                @ obj.field (always 32 bits)<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction



/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK: /* 0xf8 */
/* File: armv5te/OP_INVOKE_VIRTUAL_QUICK.S */
    /*
     * Handle an optimized virtual method call.
     *
     * for: [opt] invoke-virtual-quick, invoke-virtual-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r3, 2)                        @ r3<- FEDC or CCCC
    FETCH(r1, 1)                        @ r1<- BBBB
    .if     (!0)
    and     r3, r3, #15                 @ r3<- C (or stays CCCC)
    .endif
    GET_VREG(r2, r3)                    @ r2<- vC ("this" ptr)
    cmp     r2, #0                      @ is "this" null?
    beq     common_errNullObject        @ null "this", throw exception
    ldr     r2, [r2, #offObject_clazz]  @ r2<- thisPtr->clazz
    ldr     r2, [r2, #offClassObject_vtable]    @ r2<- thisPtr->clazz->vtable
    EXPORT_PC()                         @ invoke must export
    ldr     r0, [r2, r1, lsl #2]        @ r3<- vtable[BBBB]
    bl      common_invokeMethodNoRange @ continue on

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK_RANGE: /* 0xf9 */
/* File: armv5te/OP_INVOKE_VIRTUAL_QUICK_RANGE.S */
/* File: armv5te/OP_INVOKE_VIRTUAL_QUICK.S */
    /*
     * Handle an optimized virtual method call.
     *
     * for: [opt] invoke-virtual-quick, invoke-virtual-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r3, 2)                        @ r3<- FEDC or CCCC
    FETCH(r1, 1)                        @ r1<- BBBB
    .if     (!1)
    and     r3, r3, #15                 @ r3<- C (or stays CCCC)
    .endif
    GET_VREG(r2, r3)                    @ r2<- vC ("this" ptr)
    cmp     r2, #0                      @ is "this" null?
    beq     common_errNullObject        @ null "this", throw exception
    ldr     r2, [r2, #offObject_clazz]  @ r2<- thisPtr->clazz
    ldr     r2, [r2, #offClassObject_vtable]    @ r2<- thisPtr->clazz->vtable
    EXPORT_PC()                         @ invoke must export
    ldr     r0, [r2, r1, lsl #2]        @ r3<- vtable[BBBB]
    bl      common_invokeMethodRange @ continue on


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK: /* 0xfa */
/* File: armv5te/OP_INVOKE_SUPER_QUICK.S */
    /*
     * Handle an optimized "super" method call.
     *
     * for: [opt] invoke-super-quick, invoke-super-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    .if     (!0)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r2, [r2, #offMethod_clazz]  @ r2<- method->clazz
    EXPORT_PC()                         @ must export for invoke
    ldr     r2, [r2, #offClassObject_super]     @ r2<- method->clazz->super
    GET_VREG(r3, r10)                   @ r3<- "this"
    ldr     r2, [r2, #offClassObject_vtable]    @ r2<- ...clazz->super->vtable
    cmp     r3, #0                      @ null "this" ref?
    ldr     r0, [r2, r1, lsl #2]        @ r0<- super->vtable[BBBB]
    beq     common_errNullObject        @ "this" is null, throw exception
    bl      common_invokeMethodNoRange @ continue on


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK_RANGE: /* 0xfb */
/* File: armv5te/OP_INVOKE_SUPER_QUICK_RANGE.S */
/* File: armv5te/OP_INVOKE_SUPER_QUICK.S */
    /*
     * Handle an optimized "super" method call.
     *
     * for: [opt] invoke-super-quick, invoke-super-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    FETCH(r10, 2)                       @ r10<- GFED or CCCC
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    .if     (!1)
    and     r10, r10, #15               @ r10<- D (or stays CCCC)
    .endif
    FETCH(r1, 1)                        @ r1<- BBBB
    ldr     r2, [r2, #offMethod_clazz]  @ r2<- method->clazz
    EXPORT_PC()                         @ must export for invoke
    ldr     r2, [r2, #offClassObject_super]     @ r2<- method->clazz->super
    GET_VREG(r3, r10)                   @ r3<- "this"
    ldr     r2, [r2, #offClassObject_vtable]    @ r2<- ...clazz->super->vtable
    cmp     r3, #0                      @ null "this" ref?
    ldr     r0, [r2, r1, lsl #2]        @ r0<- super->vtable[BBBB]
    beq     common_errNullObject        @ "this" is null, throw exception
    bl      common_invokeMethodRange @ continue on



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FC: /* 0xfc */
/* File: armv5te/OP_UNUSED_FC.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FD: /* 0xfd */
/* File: armv5te/OP_UNUSED_FD.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FE: /* 0xfe */
/* File: armv5te/OP_UNUSED_FE.S */
/* File: armv5te/unused.S */
    bl      common_abort



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FF: /* 0xff */
/* File: armv5te/OP_UNUSED_FF.S */
/* File: armv5te/unused.S */
    bl      common_abort




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
     * Continuation if the String has not yet been resolved.
     *  r1: BBBB (String ref)
     *  r9: target register
     */
.LOP_CONST_STRING_resolve:
    EXPORT_PC()
    ldr     r0, [rGLUE, #offGlue_method] @ r0<- glue->method
    ldr     r0, [r0, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveString            @ r0<- String reference
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yup, handle the exception
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_CONST_STRING_JUMBO */

    /*
     * Continuation if the String has not yet been resolved.
     *  r1: BBBBBBBB (String ref)
     *  r9: target register
     */
.LOP_CONST_STRING_JUMBO_resolve:
    EXPORT_PC()
    ldr     r0, [rGLUE, #offGlue_method] @ r0<- glue->method
    ldr     r0, [r0, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveString            @ r0<- String reference
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yup, handle the exception
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_CONST_CLASS */

    /*
     * Continuation if the Class has not yet been resolved.
     *  r1: BBBB (Class ref)
     *  r9: target register
     */
.LOP_CONST_CLASS_resolve:
    EXPORT_PC()
    ldr     r0, [rGLUE, #offGlue_method] @ r0<- glue->method
    mov     r2, #1                      @ r2<- true
    ldr     r0, [r0, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- Class reference
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yup, handle the exception
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r9)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_CHECK_CAST */

    /*
     * Trivial test failed, need to perform full check.  This is common.
     *  r0 holds obj->clazz
     *  r1 holds class resolved from BBBB
     *  r9 holds object
     */
.LOP_CHECK_CAST_fullcheck:
    bl      dvmInstanceofNonTrivial     @ r0<- boolean result
    cmp     r0, #0                      @ failed?
    bne     .LOP_CHECK_CAST_okay            @ no, success

    @ A cast has failed.  We need to throw a ClassCastException with the
    @ class of the object that failed to be cast.
    EXPORT_PC()                         @ about to throw
    ldr     r3, [r9, #offObject_clazz]  @ r3<- obj->clazz
    ldr     r0, .LstrClassCastExceptionPtr
    ldr     r1, [r3, #offClassObject_descriptor] @ r1<- obj->clazz->descriptor
    bl      dvmThrowExceptionWithClassMessage
    b       common_exceptionThrown

    /*
     * Resolution required.  This is the least-likely path.
     *
     *  r2 holds BBBB
     *  r9 holds object
     */
.LOP_CHECK_CAST_resolve:
    EXPORT_PC()                         @ resolve() could throw
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    mov     r1, r2                      @ r1<- BBBB
    mov     r2, #0                      @ r2<- false
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- resolved ClassObject ptr
    cmp     r0, #0                      @ got null?
    beq     common_exceptionThrown      @ yes, handle exception
    mov     r1, r0                      @ r1<- class resolved from BBB
    ldr     r0, [r9, #offObject_clazz]  @ r0<- obj->clazz
    b       .LOP_CHECK_CAST_resolved        @ pick up where we left off

.LstrClassCastExceptionPtr:
    .word   .LstrClassCastException


/* continuation for OP_INSTANCE_OF */

    /*
     * Trivial test failed, need to perform full check.  This is common.
     *  r0 holds obj->clazz
     *  r1 holds class resolved from BBBB
     *  r9 holds A
     */
.LOP_INSTANCE_OF_fullcheck:
    bl      dvmInstanceofNonTrivial     @ r0<- boolean result
    @ fall through to OP_INSTANCE_OF_store

    /*
     * r0 holds boolean result
     * r9 holds A
     */
.LOP_INSTANCE_OF_store:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r0, r9)                    @ vA<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * Trivial test succeeded, save and bail.
     *  r9 holds A
     */
.LOP_INSTANCE_OF_trivial:
    mov     r0, #1                      @ indicate success
    @ could b OP_INSTANCE_OF_store, but copying is faster and cheaper
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r0, r9)                    @ vA<- r0
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * Resolution required.  This is the least-likely path.
     *
     *  r3 holds BBBB
     *  r9 holds A
     */
.LOP_INSTANCE_OF_resolve:
    EXPORT_PC()                         @ resolve() could throw
    ldr     r0, [rGLUE, #offGlue_method]    @ r0<- glue->method
    mov     r1, r3                      @ r1<- BBBB
    mov     r2, #1                      @ r2<- true
    ldr     r0, [r0, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- resolved ClassObject ptr
    cmp     r0, #0                      @ got null?
    beq     common_exceptionThrown      @ yes, handle exception
    mov     r1, r0                      @ r1<- class resolved from BBB
    mov     r3, rINST, lsr #12          @ r3<- B
    GET_VREG(r0, r3)                    @ r0<- vB (object)
    ldr     r0, [r0, #offObject_clazz]  @ r0<- obj->clazz
    b       .LOP_INSTANCE_OF_resolved        @ pick up where we left off


/* continuation for OP_NEW_INSTANCE */

    .balign 32                          @ minimize cache lines
.LOP_NEW_INSTANCE_finish: @ r0=class
    bl      dvmAllocObject              @ r0<- new object
    mov     r3, rINST, lsr #8           @ r3<- AA
    cmp     r0, #0                      @ failed?
    beq     common_exceptionThrown      @ yes, handle the exception
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r3)                    @ vAA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * Class initialization required.
     *
     *  r0 holds class object
     */
.LOP_NEW_INSTANCE_needinit:
    mov     r9, r0                      @ save r0
    bl      dvmInitClass                @ initialize class
    cmp     r0, #0                      @ check boolean result
    mov     r0, r9                      @ restore r0
    bne     .LOP_NEW_INSTANCE_initialized     @ success, continue
    b       common_exceptionThrown      @ failed, deal with init exception

    /*
     * Resolution required.  This is the least-likely path.
     *
     *  r1 holds BBBB
     */
.LOP_NEW_INSTANCE_resolve:
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    mov     r2, #0                      @ r2<- false
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- resolved ClassObject ptr
    cmp     r0, #0                      @ got null?
    bne     .LOP_NEW_INSTANCE_resolved        @ no, continue
    b       common_exceptionThrown      @ yes, handle exception

    /*
     * We can't instantiate an abstract class or interface, so throw an
     * InstantiationError with the class descriptor as the message.
     *
     *  r0 holds class object
     */
.LOP_NEW_INSTANCE_abstract:
    ldr     r1, [r0, #offClassObject_descriptor]
    ldr     r0, .LstrInstantiationErrorPtr
    bl      dvmThrowExceptionWithClassMessage
    b       common_exceptionThrown

.LstrInstantiationErrorPtr:
    .word   .LstrInstantiationError


/* continuation for OP_NEW_ARRAY */


    /*
     * Resolve class.  (This is an uncommon case.)
     *
     *  r1 holds array length
     *  r2 holds class ref CCCC
     */
.LOP_NEW_ARRAY_resolve:
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    mov     r9, r1                      @ r9<- length (save)
    mov     r1, r2                      @ r1<- CCCC
    mov     r2, #0                      @ r2<- false
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveClass             @ r0<- call(clazz, ref)
    cmp     r0, #0                      @ got null?
    mov     r1, r9                      @ r1<- length (restore)
    beq     common_exceptionThrown      @ yes, handle exception
    @ fall through to OP_NEW_ARRAY_finish

    /*
     * Finish allocation.
     *
     *  r0 holds class
     *  r1 holds array length
     */
.LOP_NEW_ARRAY_finish:
    mov     r2, #ALLOC_DONT_TRACK       @ don't track in local refs table
    bl      dvmAllocArrayByClass        @ r0<- call(clazz, length, flags)
    cmp     r0, #0                      @ failed?
    mov     r2, rINST, lsr #8           @ r2<- A+
    beq     common_exceptionThrown      @ yes, handle the exception
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ vA<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_FILLED_NEW_ARRAY */

    /*
     * On entry:
     *  r0 holds array class
     *  r10 holds AA or BA
     */
.LOP_FILLED_NEW_ARRAY_continue:
    ldr     r3, [r0, #offClassObject_descriptor] @ r3<- arrayClass->descriptor
    mov     r2, #ALLOC_DONT_TRACK       @ r2<- alloc flags
    ldrb    r3, [r3, #1]                @ r3<- descriptor[1]
    .if     0
    mov     r1, r10                     @ r1<- AA (length)
    .else
    mov     r1, r10, lsr #4             @ r1<- B (length)
    .endif
    cmp     r3, #'I'                    @ array of ints?
    cmpne   r3, #'L'                    @ array of objects?
    cmpne   r3, #'['                    @ array of arrays?
    mov     r9, r1                      @ save length in r9
    bne     .LOP_FILLED_NEW_ARRAY_notimpl         @ no, not handled yet
    bl      dvmAllocArrayByClass        @ r0<- call(arClass, length, flags)
    cmp     r0, #0                      @ null return?
    beq     common_exceptionThrown      @ alloc failed, handle exception

    FETCH(r1, 2)                        @ r1<- FEDC or CCCC
    str     r0, [rGLUE, #offGlue_retval]    @ retval.l <- new array
    add     r0, r0, #offArrayObject_contents @ r0<- newArray->contents
    subs    r9, r9, #1                  @ length--, check for neg
    FETCH_ADVANCE_INST(3)               @ advance to next instr, load rINST
    bmi     2f                          @ was zero, bail

    @ copy values from registers into the array
    @ r0=array, r1=CCCC/FEDC, r9=length (from AA or B), r10=AA/BA
    .if     0
    add     r2, rFP, r1, lsl #2         @ r2<- &fp[CCCC]
1:  ldr     r3, [r2], #4                @ r3<- *r2++
    subs    r9, r9, #1                  @ count--
    str     r3, [r0], #4                @ *contents++ = vX
    bpl     1b
    @ continue at 2
    .else
    cmp     r9, #4                      @ length was initially 5?
    and     r2, r10, #15                @ r2<- A
    bne     1f                          @ <= 4 args, branch
    GET_VREG(r3, r2)                    @ r3<- vA
    sub     r9, r9, #1                  @ count--
    str     r3, [r0, #16]               @ contents[4] = vA
1:  and     r2, r1, #15                 @ r2<- F/E/D/C
    GET_VREG(r3, r2)                    @ r3<- vF/vE/vD/vC
    mov     r1, r1, lsr #4              @ r1<- next reg in low 4
    subs    r9, r9, #1                  @ count--
    str     r3, [r0], #4                @ *contents++ = vX
    bpl     1b
    @ continue at 2
    .endif

2:
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    GOTO_OPCODE(ip)                     @ execute it

    /*
     * Throw an exception indicating that we have not implemented this
     * mode of filled-new-array.
     */
.LOP_FILLED_NEW_ARRAY_notimpl:
    ldr     r0, .L_strInternalError
    ldr     r1, .L_strFilledNewArrayNotImpl
    bl      dvmThrowException
    b       common_exceptionThrown

    .if     (!0)                 @ define in one or the other, not both
.L_strFilledNewArrayNotImpl:
    .word   .LstrFilledNewArrayNotImpl
.L_strInternalError:
    .word   .LstrInternalError
    .endif


/* continuation for OP_FILLED_NEW_ARRAY_RANGE */

    /*
     * On entry:
     *  r0 holds array class
     *  r10 holds AA or BA
     */
.LOP_FILLED_NEW_ARRAY_RANGE_continue:
    ldr     r3, [r0, #offClassObject_descriptor] @ r3<- arrayClass->descriptor
    mov     r2, #ALLOC_DONT_TRACK       @ r2<- alloc flags
    ldrb    r3, [r3, #1]                @ r3<- descriptor[1]
    .if     1
    mov     r1, r10                     @ r1<- AA (length)
    .else
    mov     r1, r10, lsr #4             @ r1<- B (length)
    .endif
    cmp     r3, #'I'                    @ array of ints?
    cmpne   r3, #'L'                    @ array of objects?
    cmpne   r3, #'['                    @ array of arrays?
    mov     r9, r1                      @ save length in r9
    bne     .LOP_FILLED_NEW_ARRAY_RANGE_notimpl         @ no, not handled yet
    bl      dvmAllocArrayByClass        @ r0<- call(arClass, length, flags)
    cmp     r0, #0                      @ null return?
    beq     common_exceptionThrown      @ alloc failed, handle exception

    FETCH(r1, 2)                        @ r1<- FEDC or CCCC
    str     r0, [rGLUE, #offGlue_retval]    @ retval.l <- new array
    add     r0, r0, #offArrayObject_contents @ r0<- newArray->contents
    subs    r9, r9, #1                  @ length--, check for neg
    FETCH_ADVANCE_INST(3)               @ advance to next instr, load rINST
    bmi     2f                          @ was zero, bail

    @ copy values from registers into the array
    @ r0=array, r1=CCCC/FEDC, r9=length (from AA or B), r10=AA/BA
    .if     1
    add     r2, rFP, r1, lsl #2         @ r2<- &fp[CCCC]
1:  ldr     r3, [r2], #4                @ r3<- *r2++
    subs    r9, r9, #1                  @ count--
    str     r3, [r0], #4                @ *contents++ = vX
    bpl     1b
    @ continue at 2
    .else
    cmp     r9, #4                      @ length was initially 5?
    and     r2, r10, #15                @ r2<- A
    bne     1f                          @ <= 4 args, branch
    GET_VREG(r3, r2)                    @ r3<- vA
    sub     r9, r9, #1                  @ count--
    str     r3, [r0, #16]               @ contents[4] = vA
1:  and     r2, r1, #15                 @ r2<- F/E/D/C
    GET_VREG(r3, r2)                    @ r3<- vF/vE/vD/vC
    mov     r1, r1, lsr #4              @ r1<- next reg in low 4
    subs    r9, r9, #1                  @ count--
    str     r3, [r0], #4                @ *contents++ = vX
    bpl     1b
    @ continue at 2
    .endif

2:
    GET_INST_OPCODE(ip)                 @ ip<- opcode from rINST
    GOTO_OPCODE(ip)                     @ execute it

    /*
     * Throw an exception indicating that we have not implemented this
     * mode of filled-new-array.
     */
.LOP_FILLED_NEW_ARRAY_RANGE_notimpl:
    ldr     r0, .L_strInternalError
    ldr     r1, .L_strFilledNewArrayNotImpl
    bl      dvmThrowException
    b       common_exceptionThrown

    .if     (!1)                 @ define in one or the other, not both
.L_strFilledNewArrayNotImpl:
    .word   .LstrFilledNewArrayNotImpl
.L_strInternalError:
    .word   .LstrInternalError
    .endif


/* continuation for OP_CMPL_FLOAT */

    @ Test for NaN with a second comparison.  EABI forbids testing bit
    @ patterns, and we can't represent 0x7fc00000 in immediate form, so
    @ make the library call.
.LOP_CMPL_FLOAT_gt_or_nan:
    mov     r1, r9                      @ reverse order
    mov     r0, r10
    bl      __aeabi_cfcmple             @ r0<- Z set if eq, C clear if <
    @bleq    common_abort
    movcc   r1, #1                      @ (greater than) r1<- 1
    bcc     .LOP_CMPL_FLOAT_finish
    mvn     r1, #0                            @ r1<- 1 or -1 for NaN
    b       .LOP_CMPL_FLOAT_finish


#if 0       /* "clasic" form */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r9, r2)                    @ r9<- vBB
    GET_VREG(r10, r3)                   @ r10<- vCC
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmpeq              @ r0<- (vBB == vCC)
    cmp     r0, #0                      @ equal?
    movne   r1, #0                      @ yes, result is 0
    bne     OP_CMPL_FLOAT_finish
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmplt              @ r0<- (vBB < vCC)
    cmp     r0, #0                      @ less than?
    b       OP_CMPL_FLOAT_continue
@%break

OP_CMPL_FLOAT_continue:
    mvnne   r1, #0                      @ yes, result is -1
    bne     OP_CMPL_FLOAT_finish
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmpgt              @ r0<- (vBB > vCC)
    cmp     r0, #0                      @ greater than?
    beq     OP_CMPL_FLOAT_nan               @ no, must be NaN
    mov     r1, #1                      @ yes, result is 1
    @ fall through to _finish

OP_CMPL_FLOAT_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * This is expected to be uncommon, so we double-branch (once to here,
     * again back to _finish).
     */
OP_CMPL_FLOAT_nan:
    mvn     r1, #0                            @ r1<- 1 or -1 for NaN
    b       OP_CMPL_FLOAT_finish

#endif


/* continuation for OP_CMPG_FLOAT */

    @ Test for NaN with a second comparison.  EABI forbids testing bit
    @ patterns, and we can't represent 0x7fc00000 in immediate form, so
    @ make the library call.
.LOP_CMPG_FLOAT_gt_or_nan:
    mov     r1, r9                      @ reverse order
    mov     r0, r10
    bl      __aeabi_cfcmple             @ r0<- Z set if eq, C clear if <
    @bleq    common_abort
    movcc   r1, #1                      @ (greater than) r1<- 1
    bcc     .LOP_CMPG_FLOAT_finish
    mov     r1, #1                            @ r1<- 1 or -1 for NaN
    b       .LOP_CMPG_FLOAT_finish


#if 0       /* "clasic" form */
    FETCH(r0, 1)                        @ r0<- CCBB
    and     r2, r0, #255                @ r2<- BB
    mov     r3, r0, lsr #8              @ r3<- CC
    GET_VREG(r9, r2)                    @ r9<- vBB
    GET_VREG(r10, r3)                   @ r10<- vCC
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmpeq              @ r0<- (vBB == vCC)
    cmp     r0, #0                      @ equal?
    movne   r1, #0                      @ yes, result is 0
    bne     OP_CMPG_FLOAT_finish
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmplt              @ r0<- (vBB < vCC)
    cmp     r0, #0                      @ less than?
    b       OP_CMPG_FLOAT_continue
@%break

OP_CMPG_FLOAT_continue:
    mvnne   r1, #0                      @ yes, result is -1
    bne     OP_CMPG_FLOAT_finish
    mov     r0, r9                      @ r0<- vBB
    mov     r1, r10                     @ r1<- vCC
    bl      __aeabi_fcmpgt              @ r0<- (vBB > vCC)
    cmp     r0, #0                      @ greater than?
    beq     OP_CMPG_FLOAT_nan               @ no, must be NaN
    mov     r1, #1                      @ yes, result is 1
    @ fall through to _finish

OP_CMPG_FLOAT_finish:
    mov     r3, rINST, lsr #8           @ r3<- AA
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r3)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * This is expected to be uncommon, so we double-branch (once to here,
     * again back to _finish).
     */
OP_CMPG_FLOAT_nan:
    mov     r1, #1                            @ r1<- 1 or -1 for NaN
    b       OP_CMPG_FLOAT_finish

#endif


/* continuation for OP_CMPL_DOUBLE */

    @ Test for NaN with a second comparison.  EABI forbids testing bit
    @ patterns, and we can't represent 0x7fc00000 in immediate form, so
    @ make the library call.
.LOP_CMPL_DOUBLE_gt_or_nan:
    ldmia   r10, {r0-r1}                @ reverse order
    ldmia   r9, {r2-r3}
    bl      __aeabi_cdcmple             @ r0<- Z set if eq, C clear if <
    @bleq    common_abort
    movcc   r1, #1                      @ (greater than) r1<- 1
    bcc     .LOP_CMPL_DOUBLE_finish
    mvn     r1, #0                            @ r1<- 1 or -1 for NaN
    b       .LOP_CMPL_DOUBLE_finish


/* continuation for OP_CMPG_DOUBLE */

    @ Test for NaN with a second comparison.  EABI forbids testing bit
    @ patterns, and we can't represent 0x7fc00000 in immediate form, so
    @ make the library call.
.LOP_CMPG_DOUBLE_gt_or_nan:
    ldmia   r10, {r0-r1}                @ reverse order
    ldmia   r9, {r2-r3}
    bl      __aeabi_cdcmple             @ r0<- Z set if eq, C clear if <
    @bleq    common_abort
    movcc   r1, #1                      @ (greater than) r1<- 1
    bcc     .LOP_CMPG_DOUBLE_finish
    mov     r1, #1                            @ r1<- 1 or -1 for NaN
    b       .LOP_CMPG_DOUBLE_finish


/* continuation for OP_CMP_LONG */

.LOP_CMP_LONG_less:
    mvn     r1, #0                      @ r1<- -1
    @ Want to cond code the next mov so we can avoid branch, but don't see it;
    @ instead, we just replicate the tail end.
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r9)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

.LOP_CMP_LONG_greater:
    mov     r1, #1                      @ r1<- 1
    @ fall through to _finish

.LOP_CMP_LONG_finish:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    SET_VREG(r1, r9)                    @ vAA<- r1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_AGET_WIDE */

.LOP_AGET_WIDE_finish:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r0, r0, #offArrayObject_contents
    ldmia   r0, {r2-r3}                 @ r2/r3 <- vBB[vCC]
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r2-r3}                 @ vAA/vAA+1<- r2/r3
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_APUT_WIDE */

.LOP_APUT_WIDE_finish:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldmia   r9, {r2-r3}                 @ r2/r3<- vAA/vAA+1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    add     r0, #offArrayObject_contents
    stmia   r0, {r2-r3}                 @ vBB[vCC] <- r2/r3
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_APUT_OBJECT */
    /*
     * On entry:
     *  r1 = vBB (arrayObj)
     *  r9 = vAA (obj)
     *  r10 = offset into array (vBB + vCC * width)
     */
.LOP_APUT_OBJECT_finish:
    cmp     r9, #0                      @ storing null reference?
    beq     .LOP_APUT_OBJECT_skip_check      @ yes, skip type checks
    ldr     r0, [r9, #offObject_clazz]  @ r0<- obj->clazz
    ldr     r1, [r1, #offObject_clazz]  @ r1<- arrayObj->clazz
    bl      dvmCanPutArrayElement       @ test object type vs. array type
    cmp     r0, #0                      @ okay?
    beq     common_errArrayStore        @ no
.LOP_APUT_OBJECT_skip_check:
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r9, [r10, #offArrayObject_contents] @ vBB[vCC]<- vAA
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_finish:
    @bl      common_squeak0
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_WIDE */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_WIDE_finish:
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    mov     r2, rINST, lsr #8           @ r2<- A+
    add     r9, r9, r3                  @ r9<- obj + field offset
    ldmia   r9, {r0-r1}                 @ r0/r1<- obj.field (64-bit align ok)
    and     r2, r2, #15                 @ r2<- A
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    add     r3, rFP, r2, lsl #2         @ r3<- &fp[A]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r3, {r0-r1}                 @ fp[A]<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_OBJECT */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_OBJECT_finish:
    @bl      common_squeak0
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_BOOLEAN */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_BOOLEAN_finish:
    @bl      common_squeak1
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_BYTE */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_BYTE_finish:
    @bl      common_squeak2
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_CHAR */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_CHAR_finish:
    @bl      common_squeak3
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IGET_SHORT */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IGET_SHORT_finish:
    @bl      common_squeak4
    cmp     r9, #0                      @ check object for null
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    beq     common_errNullObject        @ object was null
    ldr   r0, [r9, r3]                @ r0<- obj.field (8/16/32 bits)
    mov     r2, rINST, lsr #8           @ r2<- A+
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    and     r2, r2, #15                 @ r2<- A
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    SET_VREG(r0, r2)                    @ fp[A]<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_finish:
    @bl      common_squeak0
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_WIDE */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_WIDE_finish:
    mov     r2, rINST, lsr #8           @ r2<- A+
    cmp     r9, #0                      @ check object for null
    and     r2, r2, #15                 @ r2<- A
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    add     r2, rFP, r2, lsl #2         @ r3<- &fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldmia   r2, {r0-r1}                 @ r0/r1<- fp[A]
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    add     r9, r9, r3                  @ r9<- object + byte offset
    stmia   r9, {r0-r1}                 @ obj.field (64 bits, aligned)<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_OBJECT */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_OBJECT_finish:
    @bl      common_squeak0
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_BOOLEAN */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_BOOLEAN_finish:
    @bl      common_squeak1
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_BYTE */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_BYTE_finish:
    @bl      common_squeak2
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_CHAR */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_CHAR_finish:
    @bl      common_squeak3
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_IPUT_SHORT */

    /*
     * Currently:
     *  r0 holds resolved field
     *  r9 holds object
     */
.LOP_IPUT_SHORT_finish:
    @bl      common_squeak4
    mov     r1, rINST, lsr #8           @ r1<- A+
    ldr     r3, [r0, #offInstField_byteOffset]  @ r3<- byte offset of field
    and     r1, r1, #15                 @ r1<- A
    cmp     r9, #0                      @ check object for null
    GET_VREG(r0, r1)                    @ r0<- fp[A]
    beq     common_errNullObject        @ object was null
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str  r0, [r9, r3]                @ obj.field (8/16/32 bits)<- r0
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_SGET */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_WIDE */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_WIDE_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_WIDE_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_OBJECT */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_OBJECT_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_OBJECT_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_BOOLEAN */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_BOOLEAN_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_BOOLEAN_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_BYTE */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_BYTE_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_BYTE_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_CHAR */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_CHAR_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_CHAR_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SGET_SHORT */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SGET_SHORT_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SGET_SHORT_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_WIDE */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     *  r9: &fp[AA]
     */
.LOP_SPUT_WIDE_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_WIDE_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_OBJECT */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_OBJECT_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_OBJECT_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_BOOLEAN */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_BOOLEAN_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_BOOLEAN_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_BYTE */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_BYTE_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_BYTE_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_CHAR */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_CHAR_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_CHAR_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_SPUT_SHORT */

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     */
.LOP_SPUT_SHORT_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .LOP_SPUT_SHORT_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception


/* continuation for OP_INVOKE_VIRTUAL */

    /*
     * At this point:
     *  r0 = resolved base method
     *  r10 = C or CCCC (index of first arg, which is the "this" ptr)
     */
.LOP_INVOKE_VIRTUAL_continue:
    GET_VREG(r1, r10)                   @ r1<- "this" ptr
    ldrh    r2, [r0, #offMethod_methodIndex]    @ r2<- baseMethod->methodIndex
    cmp     r1, #0                      @ is "this" null?
    beq     common_errNullObject        @ null "this", throw exception
    ldr     r3, [r1, #offObject_clazz]  @ r1<- thisPtr->clazz
    ldr     r3, [r3, #offClassObject_vtable]    @ r3<- thisPtr->clazz->vtable
    ldr     r0, [r3, r2, lsl #2]        @ r3<- vtable[methodIndex]
    bl      common_invokeMethodNoRange @ continue on


/* continuation for OP_INVOKE_SUPER */

    /*
     * At this point:
     *  r0 = resolved base method
     *  r9 = method->clazz
     */
.LOP_INVOKE_SUPER_continue:
    ldr     r1, [r9, #offClassObject_super]     @ r1<- method->clazz->super
    ldrh    r2, [r0, #offMethod_methodIndex]    @ r2<- baseMethod->methodIndex
    ldr     r3, [r1, #offClassObject_vtableCount]   @ r3<- super->vtableCount
    EXPORT_PC()                         @ must export for invoke
    cmp     r2, r3                      @ compare (methodIndex, vtableCount)
    bcs     .LOP_INVOKE_SUPER_nsm             @ method not present in superclass
    ldr     r1, [r1, #offClassObject_vtable]    @ r1<- ...clazz->super->vtable
    ldr     r0, [r1, r2, lsl #2]        @ r3<- vtable[methodIndex]
    bl      common_invokeMethodNoRange @ continue on

.LOP_INVOKE_SUPER_resolve:
    mov     r0, r9                      @ r0<- method->clazz
    mov     r2, #METHOD_VIRTUAL         @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     .LOP_INVOKE_SUPER_continue        @ no, continue
    b       common_exceptionThrown      @ yes, handle exception

    /*
     * Throw a NoSuchMethodError with the method name as the message.
     *  r0 = resolved base method
     */
.LOP_INVOKE_SUPER_nsm:
    ldr     r1, [r0, #offMethod_name]   @ r1<- method name
    b       common_errNoSuchMethod


/* continuation for OP_INVOKE_DIRECT */

    /*
     * On entry:
     *  r1 = reference (BBBB or CCCC)
     *  r10 = "this" register
     */
.LOP_INVOKE_DIRECT_resolve:
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_DIRECT          @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    GET_VREG(r2, r10)                   @ r2<- "this" ptr (reload)
    bne     .LOP_INVOKE_DIRECT_finish          @ no, continue
    b       common_exceptionThrown      @ yes, handle exception


/* continuation for OP_INVOKE_VIRTUAL_RANGE */

    /*
     * At this point:
     *  r0 = resolved base method
     *  r10 = C or CCCC (index of first arg, which is the "this" ptr)
     */
.LOP_INVOKE_VIRTUAL_RANGE_continue:
    GET_VREG(r1, r10)                   @ r1<- "this" ptr
    ldrh    r2, [r0, #offMethod_methodIndex]    @ r2<- baseMethod->methodIndex
    cmp     r1, #0                      @ is "this" null?
    beq     common_errNullObject        @ null "this", throw exception
    ldr     r3, [r1, #offObject_clazz]  @ r1<- thisPtr->clazz
    ldr     r3, [r3, #offClassObject_vtable]    @ r3<- thisPtr->clazz->vtable
    ldr     r0, [r3, r2, lsl #2]        @ r3<- vtable[methodIndex]
    bl      common_invokeMethodRange @ continue on


/* continuation for OP_INVOKE_SUPER_RANGE */

    /*
     * At this point:
     *  r0 = resolved base method
     *  r9 = method->clazz
     */
.LOP_INVOKE_SUPER_RANGE_continue:
    ldr     r1, [r9, #offClassObject_super]     @ r1<- method->clazz->super
    ldrh    r2, [r0, #offMethod_methodIndex]    @ r2<- baseMethod->methodIndex
    ldr     r3, [r1, #offClassObject_vtableCount]   @ r3<- super->vtableCount
    EXPORT_PC()                         @ must export for invoke
    cmp     r2, r3                      @ compare (methodIndex, vtableCount)
    bcs     .LOP_INVOKE_SUPER_RANGE_nsm             @ method not present in superclass
    ldr     r1, [r1, #offClassObject_vtable]    @ r1<- ...clazz->super->vtable
    ldr     r0, [r1, r2, lsl #2]        @ r3<- vtable[methodIndex]
    bl      common_invokeMethodRange @ continue on

.LOP_INVOKE_SUPER_RANGE_resolve:
    mov     r0, r9                      @ r0<- method->clazz
    mov     r2, #METHOD_VIRTUAL         @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    bne     .LOP_INVOKE_SUPER_RANGE_continue        @ no, continue
    b       common_exceptionThrown      @ yes, handle exception

    /*
     * Throw a NoSuchMethodError with the method name as the message.
     *  r0 = resolved base method
     */
.LOP_INVOKE_SUPER_RANGE_nsm:
    ldr     r1, [r0, #offMethod_name]   @ r1<- method name
    b       common_errNoSuchMethod


/* continuation for OP_INVOKE_DIRECT_RANGE */

    /*
     * On entry:
     *  r1 = reference (BBBB or CCCC)
     *  r10 = "this" register
     */
.LOP_INVOKE_DIRECT_RANGE_resolve:
    ldr     r3, [rGLUE, #offGlue_method] @ r3<- glue->method
    ldr     r0, [r3, #offMethod_clazz]  @ r0<- method->clazz
    mov     r2, #METHOD_DIRECT          @ resolver method type
    bl      dvmResolveMethod            @ r0<- call(clazz, ref, flags)
    cmp     r0, #0                      @ got null?
    GET_VREG(r2, r10)                   @ r2<- "this" ptr (reload)
    bne     .LOP_INVOKE_DIRECT_RANGE_finish          @ no, continue
    b       common_exceptionThrown      @ yes, handle exception


/* continuation for OP_FLOAT_TO_LONG */
/*
 * Convert the float in r0 to a long in r0/r1.
 *
 * We have to clip values to long min/max per the specification.  The
 * expected common case is a "reasonable" value that converts directly
 * to modest integer.  The EABI convert function isn't doing this for us.
 */
f2l_doconv:
    stmfd   sp!, {r4, lr}
    mov     r1, #0x5f000000             @ (float)maxlong
    mov     r4, r0
    bl      __aeabi_fcmpge              @ is arg >= maxlong?
    cmp     r0, #0                      @ nonzero == yes
    mvnne   r0, #0                      @ return maxlong (7fffffff)
    mvnne   r1, #0x80000000
    ldmnefd sp!, {r4, pc}

    mov     r0, r4                      @ recover arg
    mov     r1, #0xdf000000             @ (float)minlong
    bl      __aeabi_fcmple              @ is arg <= minlong?
    cmp     r0, #0                      @ nonzero == yes
    movne   r0, #0                      @ return minlong (80000000)
    movne   r1, #0x80000000
    ldmnefd sp!, {r4, pc}

    mov     r0, r4                      @ recover arg
    mov     r1, r4
    bl      __aeabi_fcmpeq              @ is arg == self?
    cmp     r0, #0                      @ zero == no
    moveq   r1, #0                      @ return zero for NaN
    ldmeqfd sp!, {r4, pc}

    mov     r0, r4                      @ recover arg
    bl      __aeabi_f2lz                @ convert float to long
    ldmfd   sp!, {r4, pc}


/* continuation for OP_DOUBLE_TO_LONG */
/*
 * Convert the double in r0/r1 to a long in r0/r1.
 *
 * We have to clip values to long min/max per the specification.  The
 * expected common case is a "reasonable" value that converts directly
 * to modest integer.  The EABI convert function isn't doing this for us.
 */
d2l_doconv:
    stmfd   sp!, {r4, r5, lr}           @ save regs
    ldr     r3, .LOP_DOUBLE_TO_LONG_max         @ (double)maxlong, hi
    sub     sp, sp, #4                  @ align for EABI
    mov     r2, #0                      @ (double)maxlong, lo
    mov     r4, r0                      @ save r0
    mov     r5, r1                      @  and r1
    bl      __aeabi_dcmpge              @ is arg >= maxlong?
    cmp     r0, #0                      @ nonzero == yes
    mvnne   r0, #0                      @ return maxlong (7fffffffffffffff)
    mvnne   r1, #0x80000000
    bne     1f

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    ldr     r3, .LOP_DOUBLE_TO_LONG_min         @ (double)minlong, hi
    mov     r2, #0                      @ (double)minlong, lo
    bl      __aeabi_dcmple              @ is arg <= minlong?
    cmp     r0, #0                      @ nonzero == yes
    movne   r0, #0                      @ return minlong (8000000000000000)
    movne   r1, #0x80000000
    bne     1f

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    mov     r2, r4                      @ compare against self
    mov     r3, r5
    bl      __aeabi_dcmpeq              @ is arg == self?
    cmp     r0, #0                      @ zero == no
    moveq   r1, #0                      @ return zero for NaN
    beq     1f

    mov     r0, r4                      @ recover arg
    mov     r1, r5
    bl      __aeabi_d2lz                @ convert double to long

1:
    add     sp, sp, #4
    ldmfd   sp!, {r4, r5, pc}

.LOP_DOUBLE_TO_LONG_max:
    .word   0x43e00000                  @ maxlong, as a double (high word)
.LOP_DOUBLE_TO_LONG_min:
    .word   0xc3e00000                  @ minlong, as a double (high word)


/* continuation for OP_MUL_LONG */

.LOP_MUL_LONG_finish:
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r0, {r9-r10}                @ vAA/vAA+1<- r9/r10
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_SHL_LONG */

.LOP_SHL_LONG_finish:
    mov     r0, r0, asl r2              @  r0<- r0 << r2
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_SHR_LONG */

.LOP_SHR_LONG_finish:
    mov     r1, r1, asr r2              @  r1<- r1 >> r2
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_USHR_LONG */

.LOP_USHR_LONG_finish:
    mov     r1, r1, lsr r2              @  r1<- r1 >>> r2
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_SHL_LONG_2ADDR */

.LOP_SHL_LONG_2ADDR_finish:
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_SHR_LONG_2ADDR */

.LOP_SHR_LONG_2ADDR_finish:
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_USHR_LONG_2ADDR */

.LOP_USHR_LONG_2ADDR_finish:
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    stmia   r9, {r0-r1}                 @ vAA/vAA+1<- r0/r1
    GOTO_OPCODE(ip)                     @ jump to next instruction


/* continuation for OP_EXECUTE_INLINE */

    /*
     * Extract args, call function.
     *  r0 = #of args (0-4)
     *  r10 = call index
     *  lr = return addr, above  [DO NOT bl out of here w/o preserving LR]
     *
     * Other ideas:
     * - Use a jump table from the main piece to jump directly into the
     *   AND/LDR pairs.  Costs a data load, saves a branch.
     * - Have five separate pieces that do the loading, so we can work the
     *   interleave a little better.  Increases code size.
     */
.LOP_EXECUTE_INLINE_continue:
    rsb     r0, r0, #4                  @ r0<- 4-r0
    FETCH(r9, 2)                        @ r9<- FEDC
    add     pc, pc, r0, lsl #3          @ computed goto, 2 instrs each
    bl      common_abort                @ (skipped due to ARM prefetch)
4:  and     ip, r9, #0xf000             @ isolate F
    ldr     r3, [rFP, ip, lsr #10]      @ r3<- vF (shift right 12, left 2)
3:  and     ip, r9, #0x0f00             @ isolate E
    ldr     r2, [rFP, ip, lsr #6]       @ r2<- vE
2:  and     ip, r9, #0x00f0             @ isolate D
    ldr     r1, [rFP, ip, lsr #2]       @ r1<- vD
1:  and     ip, r9, #0x000f             @ isolate C
    ldr     r0, [rFP, ip, lsl #2]       @ r0<- vC
0:
    ldr     r9, .LOP_EXECUTE_INLINE_table       @ table of InlineOperation
    LDR_PC  "[r9, r10, lsl #4]"         @ sizeof=16, "func" is first entry
    @ (not reached)

.LOP_EXECUTE_INLINE_table:
    .word   gDvmInlineOpsTable


    .size   dvmAsmSisterStart, .-dvmAsmSisterStart
    .global dvmAsmSisterEnd
dvmAsmSisterEnd:

/* File: armv5te/footer.S */
/*
 * ===========================================================================
 *  Common subroutines and data
 * ===========================================================================
 */

    .text
    .align  2

/*
 * Common code when a backward branch is taken.
 *
 * On entry:
 *  r9 is PC adjustment *in bytes*
 */
common_backwardBranch:
    mov     r0, #kInterpEntryInstr
    bl      common_periodicChecks
    FETCH_ADVANCE_INST_RB(r9)           @ update rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction


/*
 * Need to see if the thread needs to be suspended or debugger/profiler
 * activity has begun.
 *
 * TODO: if JDWP isn't running, zero out pDebuggerActive pointer so we don't
 * have to do the second ldr.
 *
 * TODO: reduce this so we're just checking a single location.
 *
 * On entry:
 *  r0 is reentry type, e.g. kInterpEntryInstr
 *  r9 is trampoline PC adjustment *in bytes*
 */
common_periodicChecks:
    ldr     r3, [rGLUE, #offGlue_pSelfSuspendCount] @ r3<- &suspendCount

#if defined(WITH_DEBUGGER)
    ldr     r1, [rGLUE, #offGlue_pDebuggerActive]   @ r1<- &debuggerActive
#endif
#if defined(WITH_PROFILER)
    ldr     r2, [rGLUE, #offGlue_pActiveProfilers]  @ r2<- &activeProfilers
#endif

    ldr     r3, [r3]                    @ r3<- suspendCount (int)

#if defined(WITH_DEBUGGER)
    ldrb    r1, [r1]                    @ r1<- debuggerActive (boolean)
#endif
#if defined (WITH_PROFILER)
    ldr     r2, [r2]                    @ r2<- activeProfilers (int)
#endif

    cmp     r3, #0                      @ suspend pending?
    bne     2f                          @ yes, check suspend

#if defined(WITH_DEBUGGER) || defined(WITH_PROFILER)
# if defined(WITH_DEBUGGER) && defined(WITH_PROFILER)
    orrs    r1, r1, r2                  @ r1<- r1 | r2
    cmp     r1, #0                      @ debugger attached or profiler started?
# elif defined(WITH_DEBUGGER)
    cmp     r1, #0                      @ debugger attached?
# elif defined(WITH_PROFILER)
    cmp     r2, #0                      @ profiler started?
# endif
    bne     3f                          @ debugger/profiler, switch interp
#endif

    bx      lr                          @ nothing to do, return

2:  @ check suspend
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- glue->self
    b       dvmCheckSuspendPending      @ suspend if necessary, then return

3:  @ debugger/profiler enabled, bail out
    add     rPC, rPC, r9                @ update rPC
    str     r0, [rGLUE, #offGlue_entryPoint]
    mov     r1, #1                      @ "want switch" = true
    b       common_gotoBail


/*
 * The equivalent of "goto bail", this calls through the "bail handler".
 *
 * State registers will be saved to the "glue" area before bailing.
 *
 * On entry:
 *  r1 is "bool changeInterp", indicating if we want to switch to the
 *     other interpreter or just bail all the way out
 */
common_gotoBail:
    SAVE_PC_FP_TO_GLUE()                @ export state to "glue"
    mov     r0, rGLUE                   @ r0<- glue ptr
    b       dvmMterpStdBail             @ call(glue, changeInterp)

    @add     r1, r1, #1                  @ using (boolean+1)
    @add     r0, rGLUE, #offGlue_jmpBuf  @ r0<- &glue->jmpBuf
    @bl      _longjmp                    @ does not return
    @bl      common_abort


/*
 * Common code for method invocation with range.
 *
 * On entry:
 *  r0 is "Method* methodToCall", the method we're trying to call
 */
common_invokeMethodRange:
.LinvokeNewRange:
    @ prepare to copy args to "outs" area of current frame
    movs    r2, rINST, lsr #8           @ r2<- AA (arg count) -- test for zero
    SAVEAREA_FROM_FP(r10, rFP)          @ r10<- stack save area
    beq     .LinvokeArgsDone            @ if no args, skip the rest
    FETCH(r1, 2)                        @ r1<- CCCC

    @ r0=methodToCall, r1=CCCC, r2=count, r10=outs
    @ (very few methods have > 10 args; could unroll for common cases)
    add     r3, rFP, r1, lsl #2         @ r3<- &fp[CCCC]
    sub     r10, r10, r2, lsl #2        @ r10<- "outs" area, for call args
1:  ldr     r1, [r3], #4                @ val = *fp++
    subs    r2, r2, #1                  @ count--
    str     r1, [r10], #4               @ *outs++ = val
    bne     1b                          @ ...while count != 0
    b       .LinvokeArgsDone

/*
 * Common code for method invocation without range.
 *
 * On entry:
 *  r0 is "Method* methodToCall", the method we're trying to call
 */
common_invokeMethodNoRange:
.LinvokeNewNoRange:
    @ prepare to copy args to "outs" area of current frame
    movs    r2, rINST, lsr #12          @ r2<- B (arg count) -- test for zero
    SAVEAREA_FROM_FP(r10, rFP)          @ r10<- stack save area
    beq     .LinvokeArgsDone            @ if no args, skip the rest
    FETCH(r1, 2)                        @ r1<- GFED

    @ r0=methodToCall, r1=GFED, r2=count, r10=outs
.LinvokeNonRange:
    rsb     r2, r2, #5                  @ r2<- 5-r2
    add     pc, pc, r2, lsl #4          @ computed goto, 4 instrs each
    bl      common_abort                @ (skipped due to ARM prefetch)
5:  and     ip, rINST, #0x0f00          @ isolate A
    ldr     r3, [rFP, ip, lsr #6]       @ r3<- vA (shift right 8, left 2)
    mov     r0, r0                      @ nop
    str     r3, [r10, #-4]!             @ *--outs = vA
4:  and     ip, r1, #0xf000             @ isolate G
    ldr     r3, [rFP, ip, lsr #10]      @ r3<- vG (shift right 12, left 2)
    mov     r0, r0                      @ nop
    str     r3, [r10, #-4]!             @ *--outs = vG
3:  and     ip, r1, #0x0f00             @ isolate F
    ldr     r3, [rFP, ip, lsr #6]       @ r3<- vF
    mov     r0, r0                      @ nop
    str     r3, [r10, #-4]!             @ *--outs = vF
2:  and     ip, r1, #0x00f0             @ isolate E
    ldr     r3, [rFP, ip, lsr #2]       @ r3<- vE
    mov     r0, r0                      @ nop
    str     r3, [r10, #-4]!             @ *--outs = vE
1:  and     ip, r1, #0x000f             @ isolate D
    ldr     r3, [rFP, ip, lsl #2]       @ r3<- vD
    mov     r0, r0                      @ nop
    str     r3, [r10, #-4]!             @ *--outs = vD
0:  @ fall through to .LinvokeArgsDone

.LinvokeArgsDone: @ r0=methodToCall
    @ find space for the new stack frame, check for overflow
    SAVEAREA_FROM_FP(r1, rFP)           @ r1<- stack save area
    ldrh    r2, [r0, #offMethod_registersSize]  @ r2<- methodToCall->regsSize
    ldrh    r3, [r0, #offMethod_outsSize]   @ r3<- methodToCall->outsSize
    sub     r1, r1, r2, lsl #2          @ r1<- newFp (old savearea - regsSize)
    SAVEAREA_FROM_FP(r10, r1)           @ r10<- newSaveArea
@    bl      common_dumpRegs
    ldr     r9, [rGLUE, #offGlue_interpStackEnd]    @ r9<- interpStackEnd
    sub     r3, r10, r3, lsl #2         @ r3<- bottom (newsave - outsSize)
    cmp     r3, r9                      @ bottom < interpStackEnd?
    blt     .LstackOverflow             @ yes, this frame will overflow stack

    @ set up newSaveArea
#ifdef EASY_GDB
    SAVEAREA_FROM_FP(ip, rFP)           @ ip<- stack save area
    str     ip, [r10, #offStackSaveArea_prevSave]
#endif
    str     rFP, [r10, #offStackSaveArea_prevFrame]
    str     rPC, [r10, #offStackSaveArea_savedPc]
    str     r0, [r10, #offStackSaveArea_method]

    ldr     r3, [r0, #offMethod_accessFlags] @ r3<- methodToCall->accessFlags
    tst     r3, #ACC_NATIVE
    bne     .LinvokeNative

    /*
    stmfd   sp!, {r0-r3}
    bl      common_printNewline
    mov     r0, rFP
    mov     r1, #0
    bl      dvmDumpFp
    ldmfd   sp!, {r0-r3}
    stmfd   sp!, {r0-r3}
    mov     r0, r1
    mov     r1, r10
    bl      dvmDumpFp
    bl      common_printNewline
    ldmfd   sp!, {r0-r3}
    */

    @ Update "glue" values for the new method
    @ r0=methodToCall, r1=newFp
    ldr     r3, [r0, #offMethod_clazz]      @ r3<- method->clazz
    str     r0, [rGLUE, #offGlue_method]    @ glue->method = methodToCall
    ldr     r3, [r3, #offClassObject_pDvmDex] @ r3<- method->clazz->pDvmDex
    ldr     rPC, [r0, #offMethod_insns]     @ rPC<- method->insns
    str     r3, [rGLUE, #offGlue_methodClassDex] @ glue->methodClassDex = ...
    ldr     r2, [rGLUE, #offGlue_self]      @ r2<- glue->self
    FETCH_INST()                            @ load rINST from rPC
    mov     rFP, r1                         @ fp = newFp
    GET_INST_OPCODE(ip)                     @ extract opcode from rINST
    str     r1, [r2, #offThread_curFrame]   @ self->curFrame = newFp
    GOTO_OPCODE(ip)                         @ jump to next instruction

.LinvokeNative:
    @ Prep for the native call
    @ r0=methodToCall, r1=newFp, r10=newSaveArea
    ldr     r3, [rGLUE, #offGlue_self]      @ r3<- glue->self
    ldr     r9, [r3, #offThread_jniLocal_nextEntry] @ r9<- thread->refNext
    str     r1, [r3, #offThread_curFrame]   @ self->curFrame = newFp
    str     r9, [r10, #offStackSaveArea_localRefTop] @newFp->localRefTop=refNext
    mov     r9, r3                      @ r9<- glue->self (preserve)

    mov     r2, r0                      @ r2<- methodToCall
    mov     r0, r1                      @ r0<- newFp (points to args)
    add     r1, rGLUE, #offGlue_retval  @ r1<- &retval

#ifdef ASSIST_DEBUGGER
    /* insert fake function header to help gdb find the stack frame */
    b       .Lskip
    .type   dalvik_mterp, %function
dalvik_mterp:
    .fnstart
    MTERP_ENTRY1
    MTERP_ENTRY2
.Lskip:
#endif

    @mov     lr, pc                      @ set return addr
    @ldr     pc, [r2, #offMethod_nativeFunc] @ pc<- methodToCall->nativeFunc
    LDR_PC_LR "[r2, #offMethod_nativeFunc]"

    @ native return; r9=self, r10=newSaveArea
    @ equivalent to dvmPopJniLocals
    ldr     r0, [r10, #offStackSaveArea_localRefTop] @ r0<- newSave->localRefTop
    ldr     r1, [r9, #offThread_exception] @ check for exception
    str     rFP, [r9, #offThread_curFrame]  @ self->curFrame = fp
    cmp     r1, #0                      @ null?
    str     r0, [r9, #offThread_jniLocal_nextEntry] @ self->refNext<- r0
    bne     common_exceptionThrown      @ no, handle exception

    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

.LstackOverflow:
    ldr     r0, [rGLUE, #offGlue_self]  @ r0<- self
    bl      dvmHandleStackOverflow
    b       common_exceptionThrown
#ifdef ASSIST_DEBUGGER
    .fnend
#endif


    /*
     * Common code for method invocation, calling through "glue code".
     *
     * TODO: now that we have range and non-range invoke handlers, this
     *       needs to be split into two.  Maybe just create entry points
     *       that set r9 and jump here?
     *
     * On entry:
     *  r0 is "Method* methodToCall", the method we're trying to call
     *  r9 is "bool methodCallRange", indicating if this is a /range variant
     */
     .if    0
.LinvokeOld:
    sub     sp, sp, #8                  @ space for args + pad
    FETCH(ip, 2)                        @ ip<- FEDC or CCCC
    mov     r2, r0                      @ A2<- methodToCall
    mov     r0, rGLUE                   @ A0<- glue
    SAVE_PC_FP_TO_GLUE()                @ export state to "glue"
    mov     r1, r9                      @ A1<- methodCallRange
    mov     r3, rINST, lsr #8           @ A3<- AA
    str     ip, [sp, #0]                @ A4<- ip
    bl      dvmMterp_invokeMethod       @ call the C invokeMethod
    add     sp, sp, #8                  @ remove arg area
    b       common_resumeAfterGlueCall  @ continue to next instruction
    .endif



/*
 * Common code for handling a return instruction.
 *
 * This does not return.
 */
common_returnFromMethod:
.LreturnNew:
    mov     r0, #kInterpEntryReturn
    mov     r9, #0
    bl      common_periodicChecks

    SAVEAREA_FROM_FP(r0, rFP)           @ r0<- saveArea (old)
    ldr     rFP, [r0, #offStackSaveArea_prevFrame] @ fp = saveArea->prevFrame
    ldr     r2, [rFP, #(offStackSaveArea_method - sizeofStackSaveArea)]
                                        @ r2<- method we're returning to
    cmp     r2, #0                      @ is this a break frame?
    mov     r1, #0                      @ "want switch" = false
    beq     common_gotoBail             @ break frame, bail out completely

    ldr     rPC, [r0, #offStackSaveArea_savedPc] @ pc = saveArea->savedPc
    ldr     r3, [rGLUE, #offGlue_self]      @ r3<- glue->self
    str     r2, [rGLUE, #offGlue_method]    @ glue->method = newSave->method
    str     rFP, [r3, #offThread_curFrame]  @ self->curFrame = fp
    ldr     r1, [r2, #offMethod_clazz]      @ r1<- method->clazz
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    ldr     r1, [r1, #offClassObject_pDvmDex]   @ r1<- method->clazz->pDvmDex
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    str     r1, [rGLUE, #offGlue_methodClassDex]
    GOTO_OPCODE(ip)                     @ jump to next instruction

    /*
     * Return handling, calls through "glue code".
     */
     .if    0
.LreturnOld:
    SAVE_PC_FP_TO_GLUE()                @ export state
    mov     r0, rGLUE                   @ arg to function
    bl      dvmMterp_returnFromMethod
    b       common_resumeAfterGlueCall
    .endif


/*
 * Somebody has thrown an exception.  Handle it.
 *
 * If the exception processing code returns to us (instead of falling
 * out of the interpreter), continue with whatever the next instruction
 * now happens to be.
 *
 * This does not return.
 */
common_exceptionThrown:
.LexceptionNew:
    mov     r0, #kInterpEntryThrow
    mov     r9, #0
    bl      common_periodicChecks

    ldr     r10, [rGLUE, #offGlue_self] @ r10<- glue->self
    ldr     r9, [r10, #offThread_exception] @ r9<- self->exception
    mov     r1, r10                     @ r1<- self
    mov     r0, r9                      @ r0<- exception
    bl      dvmAddTrackedAlloc          @ don't let the exception be GCed
    mov     r3, #0                      @ r3<- NULL
    str     r3, [r10, #offThread_exception] @ self->exception = NULL

    /* set up args and a local for "&fp" */
    /* (str sp, [sp, #-4]!  would be perfect here, but is discouraged) */
    str     rFP, [sp, #-4]!             @ *--sp = fp
    mov     ip, sp                      @ ip<- &fp
    mov     r3, #0                      @ r3<- false
    str     ip, [sp, #-4]!              @ *--sp = &fp
    ldr     r1, [rGLUE, #offGlue_method] @ r1<- glue->method
    mov     r0, r10                     @ r0<- self
    ldr     r1, [r1, #offMethod_insns]  @ r1<- method->insns
    mov     r2, r9                      @ r2<- exception
    sub     r1, rPC, r1                 @ r1<- pc - method->insns
    mov     r1, r1, asr #1              @ r1<- offset in code units

    /* call, r0 gets catchRelPc (a code-unit offset) */
    bl      dvmFindCatchBlock           @ call(self, relPc, exc, scan?, &fp)

    /* fix earlier stack overflow if necessary; may trash rFP */
    ldrb    r1, [r10, #offThread_stackOverflowed]
    cmp     r1, #0                      @ did we overflow earlier?
    beq     1f                          @ no, skip ahead
    mov     rFP, r0                     @ save relPc result in rFP
    mov     r0, r10                     @ r0<- self
    bl      dvmCleanupStackOverflow     @ call(self)
    mov     r0, rFP                     @ restore result
1:

    /* update frame pointer and check result from dvmFindCatchBlock */
    ldr     rFP, [sp, #4]               @ retrieve the updated rFP
    cmp     r0, #0                      @ is catchRelPc < 0?
    add     sp, sp, #8                  @ restore stack
    bmi     .LnotCaughtLocally

    /* adjust locals to match self->curFrame and updated PC */
    SAVEAREA_FROM_FP(r1, rFP)           @ r1<- new save area
    ldr     r1, [r1, #offStackSaveArea_method] @ r1<- new method
    str     r1, [rGLUE, #offGlue_method]    @ glue->method = new method
    ldr     r2, [r1, #offMethod_clazz]      @ r2<- method->clazz
    ldr     r3, [r1, #offMethod_insns]      @ r3<- method->insns
    ldr     r2, [r2, #offClassObject_pDvmDex] @ r2<- method->clazz->pDvmDex
    add     rPC, r3, r0, asl #1             @ rPC<- method->insns + catchRelPc
    str     r2, [rGLUE, #offGlue_methodClassDex] @ glue->pDvmDex = meth...

    /* release the tracked alloc on the exception */
    mov     r0, r9                      @ r0<- exception
    mov     r1, r10                     @ r1<- self
    bl      dvmReleaseTrackedAlloc      @ release the exception

    /* restore the exception if the handler wants it */
    FETCH_INST()                        @ load rINST from rPC
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    cmp     ip, #OP_MOVE_EXCEPTION      @ is it "move-exception"?
    streq   r9, [r10, #offThread_exception] @ yes, restore the exception
    GOTO_OPCODE(ip)                     @ jump to next instruction

.LnotCaughtLocally: @ r9=exception, r10=self
    /* fix stack overflow if necessary */
    ldrb    r1, [r10, #offThread_stackOverflowed]
    cmp     r1, #0                      @ did we overflow earlier?
    movne   r0, r10                     @ if yes: r0<- self
    blne    dvmCleanupStackOverflow     @ if yes: call(self)

    @ may want to show "not caught locally" debug messages here
#if DVM_SHOW_EXCEPTION >= 2
    /* call __android_log_print(prio, tag, format, ...) */
    /* "Exception %s from %s:%d not caught locally" */
    @ dvmLineNumFromPC(method, pc - method->insns)
    ldr     r0, [rGLUE, #offGlue_method]
    ldr     r1, [r0, #offMethod_insns]
    sub     r1, rPC, r1
    asr     r1, r1, #1
    bl      dvmLineNumFromPC
    str     r0, [sp, #-4]!
    @ dvmGetMethodSourceFile(method)
    ldr     r0, [rGLUE, #offGlue_method]
    bl      dvmGetMethodSourceFile
    str     r0, [sp, #-4]!
    @ exception->clazz->descriptor
    ldr     r3, [r9, #offObject_clazz]
    ldr     r3, [r3, #offClassObject_descriptor]
    @
    ldr     r2, strExceptionNotCaughtLocally
    ldr     r1, strLogTag
    mov     r0, #3                      @ LOG_DEBUG
    bl      __android_log_print
#endif
    str     r9, [r10, #offThread_exception] @ restore exception
    mov     r0, r9                      @ r0<- exception
    mov     r1, r10                     @ r1<- self
    bl      dvmReleaseTrackedAlloc      @ release the exception
    mov     r1, #0                      @ "want switch" = false
    b       common_gotoBail             @ bail out


    /*
     * Exception handling, calls through "glue code".
     */
    .if     0
.LexceptionOld:
    SAVE_PC_FP_TO_GLUE()                @ export state
    mov     r0, rGLUE                   @ arg to function
    bl      dvmMterp_exceptionThrown
    b       common_resumeAfterGlueCall
    .endif


/*
 * After returning from a "glued" function, pull out the updated
 * values and start executing at the next instruction.
 */
common_resumeAfterGlueCall:
    LOAD_PC_FP_FROM_GLUE()              @ pull rPC and rFP out of glue
    FETCH_INST()                        @ load rINST from rPC
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction

/*
 * Invalid array index.
 */
common_errArrayIndex:
    EXPORT_PC()
    ldr     r0, strArrayIndexException
    mov     r1, #0
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * Invalid array value.
 */
common_errArrayStore:
    EXPORT_PC()
    ldr     r0, strArrayStoreException
    mov     r1, #0
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * Integer divide or mod by zero.
 */
common_errDivideByZero:
    EXPORT_PC()
    ldr     r0, strArithmeticException
    ldr     r1, strDivideByZero
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * Attempt to allocate an array with a negative size.
 */
common_errNegativeArraySize:
    EXPORT_PC()
    ldr     r0, strNegativeArraySizeException
    mov     r1, #0
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * Invocation of a non-existent method.
 */
common_errNoSuchMethod:
    EXPORT_PC()
    ldr     r0, strNoSuchMethodError
    mov     r1, #0
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * We encountered a null object when we weren't expecting one.  We
 * export the PC, throw a NullPointerException, and goto the exception
 * processing code.
 */
common_errNullObject:
    EXPORT_PC()
    ldr     r0, strNullPointerException
    mov     r1, #0
    bl      dvmThrowException
    b       common_exceptionThrown

/*
 * For debugging, cause an immediate fault.  The source address will
 * be in lr (use a bl instruction to jump here).
 */
common_abort:
    ldr     pc, .LdeadFood
.LdeadFood:
    .word   0xdeadf00d

/*
 * Spit out a "we were here", preserving all registers.  (The attempt
 * to save ip won't work, but we need to save an even number of
 * registers for EABI 64-bit stack alignment.)
 */
    .macro  SQUEAK num
common_squeak\num:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    ldr     r0, strSqueak
    mov     r1, #\num
    bl      printf
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr
    .endm

    SQUEAK  0
    SQUEAK  1
    SQUEAK  2
    SQUEAK  3
    SQUEAK  4
    SQUEAK  5

/*
 * Spit out the number in r0, preserving registers.
 */
common_printNum:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    mov     r1, r0
    ldr     r0, strSqueak
    bl      printf
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr

/*
 * Print a newline, preserving registers.
 */
common_printNewline:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    ldr     r0, strNewline
    bl      printf
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr

    /*
     * Print the 32-bit quantity in r0 as a hex value, preserving registers.
     */
common_printHex:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    mov     r1, r0
    ldr     r0, strPrintHex
    bl      printf
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr

/*
 * Print the 64-bit quantity in r0-r1, preserving registers.
 */
common_printLong:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    mov     r3, r1
    mov     r2, r0
    ldr     r0, strPrintLong
    bl      printf
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr

/*
 * Print full method info.  Pass the Method* in r0.  Preserves regs.
 */
common_printMethod:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bl      dvmMterpPrintMethod
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr

/*
 * Call a C helper function that dumps regs and possibly some
 * additional info.  Requires the C function to be compiled in.
 */
    .if     0
common_dumpRegs:
    stmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bl      dvmMterpDumpArmRegs
    ldmfd   sp!, {r0, r1, r2, r3, ip, lr}
    bx      lr
    .endif


/*
 * String references, must be close to the code that uses them.
 */
    .align  2
strArithmeticException:
    .word   .LstrArithmeticException
strArrayIndexException:
    .word   .LstrArrayIndexException
strArrayStoreException:
    .word   .LstrArrayStoreException
strDivideByZero:
    .word   .LstrDivideByZero
strNegativeArraySizeException:
    .word   .LstrNegativeArraySizeException
strNoSuchMethodError:
    .word   .LstrNoSuchMethodError
strNullPointerException:
    .word   .LstrNullPointerException

strLogTag:
    .word   .LstrLogTag
strExceptionNotCaughtLocally:
    .word   .LstrExceptionNotCaughtLocally

strNewline:
    .word   .LstrNewline
strSqueak:
    .word   .LstrSqueak
strPrintHex:
    .word   .LstrPrintHex
strPrintLong:
    .word   .LstrPrintLong

/*
 * Zero-terminated ASCII string data.
 *
 * On ARM we have two choices: do like gcc does, and LDR from a .word
 * with the address, or use an ADR pseudo-op to get the address
 * directly.  ADR saves 4 bytes and an indirection, but it's using a
 * PC-relative addressing mode and hence has a limited range, which
 * makes it not work well with mergeable string sections.
 */
    .section .rodata.str1.4,"aMS",%progbits,1

.LstrBadEntryPoint:
    .asciz  "Bad entry point %d\n"
.LstrArithmeticException:
    .asciz  "Ljava/lang/ArithmeticException;"
.LstrArrayIndexException:
    .asciz  "Ljava/lang/ArrayIndexOutOfBoundsException;"
.LstrArrayStoreException:
    .asciz  "Ljava/lang/ArrayStoreException;"
.LstrClassCastException:
    .asciz  "Ljava/lang/ClassCastException;"
.LstrDivideByZero:
    .asciz  "divide by zero"
.LstrFilledNewArrayNotImpl:
    .asciz  "filled-new-array only implemented for objects and 'int'"
.LstrInternalError:
    .asciz  "Ljava/lang/InternalError;"
.LstrInstantiationError:
    .asciz  "Ljava/lang/InstantiationError;"
.LstrNegativeArraySizeException:
    .asciz  "Ljava/lang/NegativeArraySizeException;"
.LstrNoSuchMethodError:
    .asciz  "Ljava/lang/NoSuchMethodError;"
.LstrNullPointerException:
    .asciz  "Ljava/lang/NullPointerException;"

.LstrLogTag:
    .asciz  "mterp"
.LstrExceptionNotCaughtLocally:
    .asciz  "Exception %s from %s:%d not caught locally\n"

.LstrNewline:
    .asciz  "\n"
.LstrSqueak:
    .asciz  "<%d>"
.LstrPrintHex:
    .asciz  "<0x%x>"
.LstrPrintLong:
    .asciz  "<%lld>"


