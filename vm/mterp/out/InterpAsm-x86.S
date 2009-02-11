/*
 * This file was generated automatically by gen-mterp.py for 'x86'.
 *
 * --> DO NOT EDIT <--
 */

/* File: x86/header.S */
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
 * 32-bit x86 definitions and declarations.
 */

/*
386 ABI general notes:

Caller save set:
   eax, edx, ecx, st(0)-st(7)
Callee save set:
   ebx, esi, edi, ebp
Return regs:
   32-bit in eax
   64-bit in edx:eax (low-order 32 in eax)
   fp on top of fp stack st(0)

Parameters passed on stack, pushed right-to-left.  On entry to target, first
parm is at 4(%esp).  Traditional entry code is:

functEntry:
    push    %ebp             # save old frame pointer
    mov     %ebp,%esp        # establish new frame pointer
    sub     FrameSize,%esp   # Allocate storage for spill, locals & outs

Once past the prologue, arguments are referenced at ((argno + 2)*4)(%ebp)

Alignment of stack not strictly required, but should be for performance.  We'll
align frame sizes to 16-byte multiples.

If we're not doing variable stack allocation (alloca), the frame pointer can be
eliminated and all arg references adjusted to be esp relative.

Mterp notes:

Some key interpreter variables will be assigned to registers.  Note that each
will also have an associated spill location (mostly used useful for those assigned
to callee save registers).

  nick     reg   purpose
  rPC      edx   interpreted program counter, used for fetching instructions
  rFP      esi   interpreted frame pointer, used for accessing locals and args
  rIBASE   edi   Base pointer for instruction dispatch computed goto
  rINST    bx    first 16-bit code of current instruction
  rOPCODE  bl    opcode portion of instruction word
  rINST_HI bh    high byte of instruction word, usually contains src/tgt reg names

Notes:
   o High order 16 bits of ebx must be zero on entry to handler
   o rPC, rFP, rIBASE, rINST/rOPCODE valid on handler entry and exit
   o eax and ecx are scratch, rINST/ebx sometimes scratch
   o rPC is in the caller save set, and will be killed across external calls. Don't
     forget to SPILL/UNSPILL it around call points

*/

#define rPC      %edx
#define rFP      %esi
#define rIBASE   %edi
#define rINST_FULL %ebx
#define rINST    %bx
#define rINST_HI %bh
#define rINST_LO %bl
#define rOPCODE  %bl


/* Frame diagram while executing dvmMterpStdRun, high to low addresses */
#define IN_ARG0        (  8)
#define CALLER_RP      (  4)
#define PREV_FP        (  0) /* <- dvmMterpStdRun ebp */
/* Spill offsets relative to %ebp */
#define EDI_SPILL      ( -4)
#define ESI_SPILL      ( -8)
#define EDX_SPILL      (-12) /* <- esp following dmMterpStdRun header */
#define rPC_SPILL      (-16)
#define rFP_SPILL      (-20)
#define rGLUE_SPILL    (-24)
#define rIBASE_SPILL   (-28)
#define rINST_FULL_SPILL    (-32)
#define TMP_SPILL      (-36)
#define LOCAL0_OFFSET  (-40)
#define LOCAL1_OFFSET  (-44)
#define LOCAL2_OFFSET  (-48)
#define LOCAL3_OFFSET  (-52)
/* Out Arg offsets, relative to %sp */
#define OUT_ARG4       ( 16)
#define OUT_ARG3       ( 12)
#define OUT_ARG2       (  8)
#define OUT_ARG1       (  4)
#define OUT_ARG0       (  0)  /* <- dvmMterpStdRun esp */

#define SPILL(reg) movl reg##,reg##_SPILL(%ebp)
#define UNSPILL(reg) movl reg##_SPILL(%ebp),reg
#define SPILL_TMP(reg) movl reg,TMP_SPILL(%ebp)
#define UNSPILL_TMP(reg) movl TMP_SPILL(%ebp),reg


/* save/restore the PC and/or FP from the glue struct */
#define LOAD_PC_FROM_GLUE(_glu)     movl    offGlue_pc(_glu),rPC
#define SAVE_PC_TO_GLUE(_glu)       movl    rPC,offGlue_pc(_glu)
#define LOAD_FP_FROM_GLUE(_glu)     movl    offGlue_fp(_glu),rFP
#define SAVE_FP_TO_GLUE(_glu)       movl    rFP,offGlue_fp(_glu)

#define GET_GLUE(_reg)     movl   rGLUE_SPILL(%ebp),_reg

/* The interpreter assumes a properly aligned stack on entry, and
 * will preserve 16-byte alignment.
 */

/*
 * "export" the PC to the interpreted stack frame, f/b/o future exception
 * objects.  Must * be done *before* something calls dvmThrowException.
 *
 * In C this is "SAVEAREA_FROM_FP(fp)->xtra.currentPc = pc", i.e.
 * fp - sizeof(StackSaveArea) + offsetof(SaveArea, xtra.currentPc)
 *
 * It's okay to do this more than once.
 */
#define EXPORT_PC() \
    movl     rPC, (-sizeofStackSaveArea + offStackSaveArea_currentPc)(rFP)

/*
 * Given a frame pointer, find the stack save area.
 *
 * In C this is "((StackSaveArea*)(_fp) -1)".
 */
#define SAVEAREA_FROM_FP(_reg, _fpreg) \
    leal    -sizeofStackSaveArea(_fpreg),_reg

/*
 * Fetch the next instruction from rPC into rINST.  Does not advance rPC.
 */
#define FETCH_INST()            movzwl    (rPC),rINST_FULL

/*
 * Fetch the nth instruction word from rPC into rINST.  Does not advance
 * rPC, and _count is in words
 */
#define FETCH_INST_WORD(_count)  movzwl  _count*2(rPC),rINST_FULL

/*
 * Fetch instruction word indexed (used for branching).
 * Index is in instruction word units.
 */
#define FETCH_INST_INDEXED(_reg) movzwl  (rPC,_reg,2),rINST_FULL

/*
 * Extract the opcode of the instruction in rINST
 */
#define EXTRACT_OPCODE(_reg)   movzx rOPCODE,_reg

/*
 * Advance rPC by instruction count
 */
#define ADVANCE_PC(_count)    leal  2*_count(rPC),rPC

/*
 * Advance rPC by branch offset in register
 */
#define ADVANCE_PC_INDEXED(_reg) leal (rPC,_reg,2),rPC

/*
 * Note: assumes opcode previously fetched and in rINST, and
 *       %eax is killable at this point.
 */
#if 1
.macro GOTO_NEXT
    /* For computed next version */
     movzx    rOPCODE,%eax
     sall     $6,%eax
     addl     rIBASE,%eax
     jmp      *%eax
.endm
#else
   /* For jump table version */
.macro GOTO_NEXT
     movzx   rOPCODE,%eax
     jmp     *(rIBASE,%eax,4)
.endm
#endif

/*
 * Get/set the 32-bit value from a Dalvik register.
 */
#define GET_VREG(_reg, _vreg)   movl     (rFP,_vreg,4),_reg
#define SET_VREG(_reg, _vreg)   movl     _reg,(rFP,_vreg,4)
#define GET_VREG_WORD(_reg, _vreg, _offset)   movl     4*(_offset)(rFP,_vreg,4),_reg
#define SET_VREG_WORD(_reg, _vreg, _offset)   movl     _reg,4*(_offset)(rFP,_vreg,4)

/*
 * This is a #include, not a %include, because we want the C pre-processor
 * to expand the macros into assembler assignment statements.
 */
#include "../common/asm-constants.h"



    .global dvmAsmInstructionStart
    .type   dvmAsmInstructionStart, %function
dvmAsmInstructionStart = .L_OP_NOP
    .text

/* ------------------------------ */
    .balign 64
.L_OP_NOP: /* 0x00 */
/* File: x86/OP_NOP.S */
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE: /* 0x01 */
/* File: x86/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    movzbl rINST_HI,%eax         # eax<- BA
    andb   $0xf,%al             # eax<- A
    shrl   $12,rINST_FULL       # rINST_FULL<- B
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG(%ecx,%eax)          # fp[A]<-fp[B]
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_FROM16: /* 0x02 */
/* File: x86/OP_MOVE_FROM16.S */
    /* for: move/from16, move-object/from16 */
    /* op vAA, vBBBB */
    movzx    rINST_HI,%eax            # eax <= AA
    movw     2(rPC),rINST             # rINST <= BBBB
    GET_VREG (%ecx,rINST_FULL)        # ecx<- fp[BBBB]
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG (%ecx,%eax)              # fp[AA]<- ecx]
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_16: /* 0x03 */
/* File: x86/OP_MOVE_16.S */
    /* for: move/16, move-object/16 */
    /* op vAAAA, vBBBB */
    movzwl    4(rPC),%ecx              # ecx<- BBBB
    movzwl    2(rPC),%eax              # eax<- AAAA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    SET_VREG(%ecx,%eax)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE: /* 0x04 */
/* File: x86/OP_MOVE_WIDE.S */
    /* move-wide vA, vB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    movzbl    rINST_HI,%ecx                # ecx <- BA
    sarl      $12,rINST_FULL              # rinst_FULL<- B
    GET_VREG_WORD(%eax,rINST_FULL,0)       # eax<- v[B+0]
    GET_VREG_WORD(rINST_FULL,rINST_FULL,1) # rINST_FULL<- v[B+1]
    andb      $0xf,%cl                    # ecx <- A
    SET_VREG_WORD(rINST_FULL,%ecx,1)       # v[A+1]<- rINST_FULL
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG_WORD(%eax,%ecx,0)             # v[A+0]<- eax
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_FROM16: /* 0x05 */
/* File: x86/OP_MOVE_WIDE_FROM16.S */
    /* move-wide/from16 vAA, vBBBB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    movzwl    2(rPC),%ecx              # ecx<- BBBB
    movzbl    rINST_HI,%eax            # eax<- AAAA
    GET_VREG_WORD(rINST_FULL,%ecx,0)   # rINST_FULL<- v[BBBB+0]
    GET_VREG_WORD(%ecx,%ecx,1)         # ecx<- v[BBBB+1]
    SET_VREG_WORD(rINST_FULL,%eax,0)   # v[AAAA+0]<- rINST_FULL
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG_WORD(%ecx,%eax,1)         # v[AAAA+1]<- eax
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_WIDE_16: /* 0x06 */
/* File: x86/OP_MOVE_WIDE_16.S */
    /* move-wide/16 vAAAA, vBBBB */
    /* NOTE: regs can overlap, e.g. "move v6,v7" or "move v7,v6" */
    movzwl    4(rPC),%ecx            # ecx<- BBBB
    movzwl    2(rPC),%eax            # eax<- AAAA
    GET_VREG_WORD(rINST_FULL,%ecx,0) # rINST_WORD<- v[BBBB+0]
    GET_VREG_WORD(%ecx,%ecx,1)       # ecx<- v[BBBB+1]
    SET_VREG_WORD(rINST_FULL,%eax,0) # v[AAAA+0]<- rINST_FULL
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    SET_VREG_WORD(%ecx,%eax,1)       # v[AAAA+1]<- ecx
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT: /* 0x07 */
/* File: x86/OP_MOVE_OBJECT.S */
/* File: x86/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    movzbl rINST_HI,%eax         # eax<- BA
    andb   $0xf,%al             # eax<- A
    shrl   $12,rINST_FULL       # rINST_FULL<- B
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG(%ecx,%eax)          # fp[A]<-fp[B]
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_FROM16: /* 0x08 */
/* File: x86/OP_MOVE_OBJECT_FROM16.S */
/* File: x86/OP_MOVE_FROM16.S */
    /* for: move/from16, move-object/from16 */
    /* op vAA, vBBBB */
    movzx    rINST_HI,%eax            # eax <= AA
    movw     2(rPC),rINST             # rINST <= BBBB
    GET_VREG (%ecx,rINST_FULL)        # ecx<- fp[BBBB]
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG (%ecx,%eax)              # fp[AA]<- ecx]
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_MOVE_OBJECT_16: /* 0x09 */
/* File: x86/OP_MOVE_OBJECT_16.S */
/* File: x86/OP_MOVE_16.S */
    /* for: move/16, move-object/16 */
    /* op vAAAA, vBBBB */
    movzwl    4(rPC),%ecx              # ecx<- BBBB
    movzwl    2(rPC),%eax              # eax<- AAAA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    SET_VREG(%ecx,%eax)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT: /* 0x0a */
/* File: x86/OP_MOVE_RESULT.S */
    /* for: move-result, move-result-object */
    /* op vAA */
    GET_GLUE(%eax)                         # eax<- rGLUE
    movzx    rINST_HI,%ecx                 # ecx<- AA
    movl     offGlue_retval(%eax),%eax     # eax<- glue->retval.l
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG (%eax,%ecx)                   # fp[AA]<- retval.l
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_WIDE: /* 0x0b */
/* File: x86/OP_MOVE_RESULT_WIDE.S */
    /* move-result-wide vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    movl    offGlue_retval(%ecx),%eax
    movl    4+offGlue_retval(%ecx),%ecx
    SET_VREG_WORD(%eax,rINST_FULL,0)    # v[AA+0] <- eax
    SET_VREG_WORD(%ecx,rINST_FULL,1)    # v[AA+1] <- ecx
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MOVE_RESULT_OBJECT: /* 0x0c */
/* File: x86/OP_MOVE_RESULT_OBJECT.S */
/* File: x86/OP_MOVE_RESULT.S */
    /* for: move-result, move-result-object */
    /* op vAA */
    GET_GLUE(%eax)                         # eax<- rGLUE
    movzx    rINST_HI,%ecx                 # ecx<- AA
    movl     offGlue_retval(%eax),%eax     # eax<- glue->retval.l
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG (%eax,%ecx)                   # fp[AA]<- retval.l
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MOVE_EXCEPTION: /* 0x0d */
/* File: x86/OP_MOVE_EXCEPTION.S */
    /* move-exception vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL        # rINST_FULL<- AA
    movl    offGlue_self(%ecx),%ecx    # ecx<- glue->self
    movl    offThread_exception(%ecx),%eax # eax<- dvmGetException bypass
    SET_VREG(%eax,rINST_FULL)          # fp[AA]<- exception object
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    movl    $0,offThread_exception(%ecx) # dvmClearException bypass
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_RETURN_VOID: /* 0x0e */
/* File: x86/OP_RETURN_VOID.S */
    jmp       common_returnFromMethod


/* ------------------------------ */
    .balign 64
.L_OP_RETURN: /* 0x0f */
/* File: x86/OP_RETURN.S */
    /*
     * Return a 32-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     *
     * for: return, return-object
     */
    /* op vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    GET_VREG(%eax,rINST_FULL)           # eax<- vAA
    movl    %eax,offGlue_retval(%ecx)   # retval.i <- AA
    jmp     common_returnFromMethod

/* ------------------------------ */
    .balign 64
.L_OP_RETURN_WIDE: /* 0x10 */
/* File: x86/OP_RETURN_WIDE.S */
    /*
     * Return a 64-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     */
    /* return-wide vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL            # rINST_FULL<- AA
    GET_VREG_WORD(%eax,rINST_FULL,0)       # eax<- v[AA+0]
    GET_VREG_WORD(rINST_FULL,rINST_FULL,1) # rINST_FULL<- v[AA+1]
    movl    %eax,offGlue_retval(%ecx)
    movl    rINST_FULL,4+offGlue_retval(%ecx)
    jmp     common_returnFromMethod

/* ------------------------------ */
    .balign 64
.L_OP_RETURN_OBJECT: /* 0x11 */
/* File: x86/OP_RETURN_OBJECT.S */
/* File: x86/OP_RETURN.S */
    /*
     * Return a 32-bit value.  Copies the return value into the "glue"
     * structure, then jumps to the return handler.
     *
     * for: return, return-object
     */
    /* op vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    GET_VREG(%eax,rINST_FULL)           # eax<- vAA
    movl    %eax,offGlue_retval(%ecx)   # retval.i <- AA
    jmp     common_returnFromMethod


/* ------------------------------ */
    .balign 64
.L_OP_CONST_4: /* 0x12 */
/* File: x86/OP_CONST_4.S */
    /* const/4 vA, #+B */
    movsx   rINST_HI,%eax              # eax<-ssssssBx
    movl    $0xf,%ecx
    andl    %eax,%ecx                  # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    sarl    $4,%eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CONST_16: /* 0x13 */
/* File: x86/OP_CONST_16.S */
    /* const/16 vAA, #+BBBB */
    movswl  2(rPC),%ecx                # ecx<- ssssBBBB
    movzx   rINST_HI,%eax              # eax<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%ecx,%eax)                # vAA<- ssssBBBB
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST: /* 0x14 */
/* File: x86/OP_CONST.S */
    /* const vAA, #+BBBBbbbb */
    movzbl    rINST_HI,%ecx           # ecx<- AA
    movl      2(rPC),%eax               # grab all 32 bits at once
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    SET_VREG(%eax,%ecx)                 # vAA<- eax
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_HIGH16: /* 0x15 */
/* File: x86/OP_CONST_HIGH16.S */
    /* const/high16 vAA, #+BBBB0000 */
    movzwl     2(rPC),%eax                # eax<- 0000BBBB
    movzbl     rINST_HI,%ecx              # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    sall       $16,%eax                  # eax<- BBBB0000
    SET_VREG(%eax,%ecx)                   # vAA<- eax
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_16: /* 0x16 */
/* File: x86/OP_CONST_WIDE_16.S */
    /* const-wide/16 vAA, #+BBBB */
    movswl    2(rPC),%eax               # eax<- ssssBBBB
    SPILL(rPC)
    movzbl    rINST_HI,%ecx             # ecx<- AA
    FETCH_INST_WORD(2)
    cltd                                # rPC:eax<- ssssssssssssBBBB
    SET_VREG_WORD(rPC,%ecx,1)           # store msw
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)          # store lsw
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_32: /* 0x17 */
/* File: x86/OP_CONST_WIDE_32.S */
    /* const-wide/32 vAA, #+BBBBbbbb */
    movl     2(rPC),%eax               # eax<- BBBBbbbb
    SPILL(rPC)
    movzbl    rINST_HI,%ecx             # ecx<- AA
    FETCH_INST_WORD(3)
    cltd                                # rPC:eax<- ssssssssssssBBBB
    SET_VREG_WORD(rPC,%ecx,1)           # store msw
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)          # store lsw
    ADVANCE_PC(3)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE: /* 0x18 */
/* File: x86/OP_CONST_WIDE.S */
    /* const-wide vAA, #+HHHHhhhhBBBBbbbb */
    movl      2(rPC),%eax         # eax<- lsw
    movzbl    rINST_HI,%ecx       # ecx <- AA
    movl      6(rPC),rINST_FULL   # rINST_FULL<- msw
    leal      (rFP,%ecx,4),%ecx   # dst addr
    movl      rINST_FULL,4(%ecx)
    FETCH_INST_WORD(5)
    movl      %eax,(%ecx)
    ADVANCE_PC(5)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CONST_WIDE_HIGH16: /* 0x19 */
/* File: x86/OP_CONST_WIDE_HIGH16.S */
    /* const-wide/high16 vAA, #+BBBB000000000000 */
    movzwl     2(rPC),%eax                # eax<- 0000BBBB
    movzbl     rINST_HI,%ecx              # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    sall       $16,%eax                  # eax<- BBBB0000
    SET_VREG_WORD(%eax,%ecx,1)            # v[AA+1]<- eax
    xorl       %eax,%eax
    SET_VREG_WORD(%eax,%ecx,0)            # v[AA+0]<- eax
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING: /* 0x1a */
/* File: x86/OP_CONST_STRING.S */

    /* const/string vAA, String@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax              # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx# ecx<- glue->methodClassDex
    movzbl    rINST_HI,rINST_FULL      # rINST_FULL<- AA
    movl      offDvmDex_pResStrings(%ecx),%ecx # ecx<- dvmDex->pResStrings
    movl      (%ecx,%eax,4),%eax       # eax<- rResString[BBBB]
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    testl     %eax,%eax                # resolved yet?
    je        .LOP_CONST_STRING_resolve
    SET_VREG(%eax,%ecx)                # vAA<- rResString[BBBB]
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_STRING_JUMBO: /* 0x1b */
/* File: x86/OP_CONST_STRING_JUMBO.S */

    /* const/string vAA, String@BBBBBBBB */
    GET_GLUE(%ecx)
    movl      2(rPC),%eax              # eax<- BBBBBBBB
    movl      offGlue_methodClassDex(%ecx),%ecx# ecx<- glue->methodClassDex
    movzbl    rINST_HI,rINST_FULL      # rINST_FULL<- AA
    movl      offDvmDex_pResStrings(%ecx),%ecx # ecx<- dvmDex->pResStrings
    movl      (%ecx,%eax,4),%eax       # eax<- rResString[BBBB]
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(3)
    testl     %eax,%eax                # resolved yet?
    je        .LOP_CONST_STRING_JUMBO_resolve
    SET_VREG(%eax,%ecx)                # vAA<- rResString[BBBB]
    ADVANCE_PC(3)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CONST_CLASS: /* 0x1c */
/* File: x86/OP_CONST_CLASS.S */

    /* const/class vAA, Class@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax              # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx# ecx<- glue->methodClassDex
    movzbl    rINST_HI,rINST_FULL      # rINST_FULL<- AA
    movl      offDvmDex_pResClasses(%ecx),%ecx # ecx<- dvmDex->pResClasses
    movl      (%ecx,%eax,4),%eax       # eax<- rResClasses[BBBB]
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    testl     %eax,%eax                # resolved yet?
    je        .LOP_CONST_CLASS_resolve
    SET_VREG(%eax,%ecx)                # vAA<- rResClasses[BBBB]
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_ENTER: /* 0x1d */
/* File: x86/OP_MONITOR_ENTER.S */
    /*
     * Synchronize on an object.
     */
    /* monitor-enter vAA */
    GET_GLUE(%ecx)
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    GET_VREG(%eax,rINST_FULL)           # eax<- vAA
    movl    offGlue_self(%ecx),%ecx     # ecx<- glue->self
    FETCH_INST_WORD(1)
    testl   %eax,%eax                   # null object?
#ifdef WITH_MONITOR_TRACKING
    EXPORT_PC()
#endif
    jne     .LOP_MONITOR_ENTER_continue
    jmp     common_errNullObject

/* ------------------------------ */
    .balign 64
.L_OP_MONITOR_EXIT: /* 0x1e */
/* File: x86/OP_MONITOR_EXIT.S */
    /*
     * Unlock an object.
     *
     * Exceptions that occur when unlocking a monitor need to appear as
     * if they happened at the following instruction.  See the Dalvik
     * instruction spec.
     */
    /* monitor-exit vAA */
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    GET_VREG(%eax,rINST_FULL)
    GET_GLUE(%ecx)
    EXPORT_PC()
    testl   %eax,%eax                   # null object?
    je      common_errNullObject        # go if so
    movl    offGlue_self(%ecx),%ecx     # ecx<- glue->self
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    movl    %ecx,OUT_ARG0(%esp)
    jmp     .LOP_MONITOR_EXIT_continue

/* ------------------------------ */
    .balign 64
.L_OP_CHECK_CAST: /* 0x1f */
/* File: x86/OP_CHECK_CAST.S */
    /*
     * Check to see if a cast from one class to another is allowed.
     */
    /* check-cast vAA, class@BBBB */
    GET_GLUE(%ecx)
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- vAA (object)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    testl     rINST_FULL,rINST_FULL     # is oject null?
    movl      offDvmDex_pResClasses(%ecx),%ecx # ecx<- pDvmDex->pResClasses
    je        .LOP_CHECK_CAST_okay          # null obj, cast always succeeds
    movl      (%ecx,%eax,4),%eax        # eax<- resolved class
    movl      offObject_clazz(rINST_FULL),%ecx # ecx<- obj->clazz
    testl     %eax,%eax                 # have we resolved this before?
    je        .LOP_CHECK_CAST_resolve       # no, go do it now
.LOP_CHECK_CAST_resolved:
    cmpl      %eax,%ecx                 # same class (trivial success)?
    jne       .LOP_CHECK_CAST_fullcheck     # no, do full check
.LOP_CHECK_CAST_okay:
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_INSTANCE_OF: /* 0x20 */
/* File: x86/OP_INSTANCE_OF.S */
    /*
     * Check to see if an object reference is an instance of a class.
     *
     * Most common situation is a non-null object, being compared against
     * an already-resolved class.
     */
    /* instance-of vA, vB, class@CCCC */
    movzbl  rINST_HI,%eax               # eax<- BA
    sarl    $4,%eax                    # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB (obj)
    GET_GLUE(%ecx)
    testl   %eax,%eax                   # object null?
    movl    offGlue_methodClassDex(%ecx),%ecx  # ecx<- pDvmDex
    SPILL(rPC)
    je      .LOP_INSTANCE_OF_store           # null obj, not instance, store it
    movzwl  2(rPC),rPC                  # rPC<- CCCC
    movl    offDvmDex_pResClasses(%ecx),%ecx # ecx<- pDvmDex->pResClasses
    movl    (%ecx,rPC,4),%ecx           # ecx<- resolved class
    movl    offObject_clazz(%eax),%eax  # eax<- obj->clazz
    testl   %ecx,%ecx                   # have we resolved this before?
    je      .LOP_INSTANCE_OF_resolve         # not resolved, do it now
.LOP_INSTANCE_OF_resolved:  # eax<- obj->clazz, ecx<- resolved class
    cmpl    %eax,%ecx                   # same class (trivial success)?
    je      .LOP_INSTANCE_OF_trivial         # yes, trivial finish
    jmp     .LOP_INSTANCE_OF_fullcheck       # no, do full check

/* ------------------------------ */
    .balign 64
.L_OP_ARRAY_LENGTH: /* 0x21 */
/* File: x86/OP_ARRAY_LENGTH.S */
    /*
     * Return the length of an array.
     */
   movzbl   rINST_HI,%eax              # eax<- BA
   sarl     $12,rINST_FULL            # rINST_FULL<- B
   GET_VREG(%ecx,rINST_FULL)           # ecx<- vB (object ref)
   andb     $0xf,%al                  # eax<- A
   testl    %ecx,%ecx                  # is null?
   je       common_errNullObject
   FETCH_INST_WORD(1)
   movl     offArrayObject_length(%ecx),%ecx
   ADVANCE_PC(1)
   SET_VREG(%ecx,%eax)
   GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_NEW_INSTANCE: /* 0x22 */
/* File: x86/OP_NEW_INSTANCE.S */
    /*
     * Create a new instance of a class.
     */
    /* new-instance vAA, class@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- pDvmDex
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    movl      offDvmDex_pResClasses(%ecx),%ecx # ecx<- pDvmDex->pResClasses
    EXPORT_PC()
    movl      (%ecx,%eax,4),%ecx        # ecx<- resolved class
    SPILL(rPC)
    testl     %ecx,%ecx                 # resolved?
    je        .LOP_NEW_INSTANCE_resolve       # no, go do it
.LOP_NEW_INSTANCE_resolved:  # on entry, ecx<- class
    cmpb      $CLASS_INITIALIZED,offClassObject_status(%ecx)
    je        .LOP_NEW_INSTANCE_initialized
    jmp       .LOP_NEW_INSTANCE_needinit

/* ------------------------------ */
    .balign 64
.L_OP_NEW_ARRAY: /* 0x23 */
/* File: x86/OP_NEW_ARRAY.S */
    /*
     * Allocate an array of objects, specified with the array class
     * and a count.
     *
     * The verifier guarantees that this is an array class, so we don't
     * check for it here.
     */
    /* new-array vA, vB, class@CCCC */
    GET_GLUE(%ecx)
    EXPORT_PC()
    movl    offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    movzwl  2(rPC),%eax                       # eax<- CCCC
    movl    offDvmDex_pResClasses(%ecx),%ecx  # ecx<- pDvmDex->pResClasses
    movl    (%ecx,%eax,4),%ecx                # ecx<- resolved class
    movzbl  rINST_HI,%eax
    sarl    $4,%eax                          # eax<- B
    GET_VREG(%eax,%eax)                       # eax<- vB (array length)
    movzbl  rINST_HI,rINST_FULL
    andb    $0xf,rINST_LO                    # rINST_FULL<- A
    testl   %eax,%eax
    js      common_errNegativeArraySize       # bail
    testl   %ecx,%ecx                         # already resolved?
    jne     .LOP_NEW_ARRAY_finish                # yes, fast path
    jmp     .LOP_NEW_ARRAY_resolve               # resolve now

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY: /* 0x24 */
/* File: x86/OP_FILLED_NEW_ARRAY.S */
    /*
     * Create a new array with elements filled from registers.
     *
     * for: filled-new-array, filled-new-array/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, type@BBBB */
    GET_GLUE(%eax)
    movzbl  rINST_HI,rINST_FULL               # rINST_FULL<- AA or BA
    movl    offGlue_methodClassDex(%eax),%eax # eax<- pDvmDex
    movzwl  2(rPC),%ecx                       # ecx<- BBBB
    movl    offDvmDex_pResClasses(%eax),%eax  # eax<- pDvmDex->pResClasses
    SPILL(rPC)
    movl    (%eax,%ecx,4),%eax                # eax<- resolved class
    EXPORT_PC()
    testl   %eax,%eax                         # already resolved?
    jne     .LOP_FILLED_NEW_ARRAY_continue              # yes, continue
    # less frequent path, so we'll redo some work
    GET_GLUE(%eax)
    movl    $0,OUT_ARG2(%esp)                # arg2<- false
    movl    %ecx,OUT_ARG1(%esp)               # arg1<- BBBB
    movl    offGlue_method(%eax),%eax         # eax<- glue->method
    jmp     .LOP_FILLED_NEW_ARRAY_more

/* ------------------------------ */
    .balign 64
.L_OP_FILLED_NEW_ARRAY_RANGE: /* 0x25 */
/* File: x86/OP_FILLED_NEW_ARRAY_RANGE.S */
/* File: x86/OP_FILLED_NEW_ARRAY.S */
    /*
     * Create a new array with elements filled from registers.
     *
     * for: filled-new-array, filled-new-array/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, type@BBBB */
    GET_GLUE(%eax)
    movzbl  rINST_HI,rINST_FULL               # rINST_FULL<- AA or BA
    movl    offGlue_methodClassDex(%eax),%eax # eax<- pDvmDex
    movzwl  2(rPC),%ecx                       # ecx<- BBBB
    movl    offDvmDex_pResClasses(%eax),%eax  # eax<- pDvmDex->pResClasses
    SPILL(rPC)
    movl    (%eax,%ecx,4),%eax                # eax<- resolved class
    EXPORT_PC()
    testl   %eax,%eax                         # already resolved?
    jne     .LOP_FILLED_NEW_ARRAY_RANGE_continue              # yes, continue
    # less frequent path, so we'll redo some work
    GET_GLUE(%eax)
    movl    $0,OUT_ARG2(%esp)                # arg2<- false
    movl    %ecx,OUT_ARG1(%esp)               # arg1<- BBBB
    movl    offGlue_method(%eax),%eax         # eax<- glue->method
    jmp     .LOP_FILLED_NEW_ARRAY_RANGE_more


/* ------------------------------ */
    .balign 64
.L_OP_FILL_ARRAY_DATA: /* 0x26 */
/* File: x86/OP_FILL_ARRAY_DATA.S */
    /* fill-array-data vAA, +BBBBBBBB */
    movl    2(rPC),%ecx                # ecx<- BBBBbbbb
    movzbl  rINST_HI,rINST_FULL        # rINST_FULL<- AA
    leal    (rPC,%ecx,2),%ecx          # ecx<- PC + BBBBbbbb*2
    GET_VREG(%eax,rINST_FULL)
    SPILL(rPC)
    EXPORT_PC()
    movl    %eax,OUT_ARG0(%esp)
    movl    %ecx,OUT_ARG1(%esp)
    call    dvmInterpHandleFillArrayData
    UNSPILL(rPC)
    FETCH_INST_WORD(3)
    testl   %eax,%eax                   # exception thrown?
    je      common_exceptionThrown
    ADVANCE_PC(3)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_THROW: /* 0x27 */
/* File: x86/OP_THROW.S */
    /*
     * Throw an exception object in the current thread.
     */
    /* throw vAA */
    GET_GLUE(%ecx)
    movzbl   rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,rINST_FULL)          # eax<- exception object
    movl     offGlue_self(%ecx),%ecx   # ecx<- glue->self
    testl    %eax,%eax                 # null object?
    je       common_errNullObject
    movl     %eax,offThread_exception(%ecx) # thread->exception<- obj
    jmp      common_exceptionThrown

/* ------------------------------ */
    .balign 64
.L_OP_GOTO: /* 0x28 */
/* File: x86/OP_GOTO.S */
    /*
     * Unconditional branch, 8-bit offset.
     *
     * The branch distance is a signed code-unit offset, which we need to
     * double to get a byte offset.
     */
    /* goto +AA */
    movsbl  rINST_HI,rINST_FULL         # ebx<- ssssssAA
    testl   rINST_FULL,rINST_FULL       # test for <0
    js      common_backwardBranch
    movl    rINST_FULL,%eax
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_GOTO_16: /* 0x29 */
/* File: x86/OP_GOTO_16.S */
    /*
     * Unconditional branch, 16-bit offset.
     *
     * The branch distance is a signed code-unit offset
     */
    /* goto/16 +AAAA */
    movswl  2(rPC),rINST_FULL           # rINST_FULL<- ssssAAAA
    testl   rINST_FULL,rINST_FULL       # test for <0
    js      common_backwardBranch
    movl    rINST_FULL,%eax
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_GOTO_32: /* 0x2a */
/* File: x86/OP_GOTO_32.S */
    /*
     * Unconditional branch, 32-bit offset.
     *
     * The branch distance is a signed code-unit offset.
     *
     * Unlike most opcodes, this one is allowed to branch to itself, so
     * our "backward branch" test must be "<=0" instead of "<0".
     */
    /* goto/32 AAAAAAAA */
    movl    2(rPC),rINST_FULL           # rINST_FULL<- AAAAAAAA
    cmpl    $0,rINST_FULL              # test for <= 0
    jle     common_backwardBranch
    movl    rINST_FULL,%eax
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_PACKED_SWITCH: /* 0x2b */
/* File: x86/OP_PACKED_SWITCH.S */
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
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    movl    2(rPC),%ecx                 # ecx<- BBBBbbbb
    GET_VREG(%eax,rINST_FULL)           # eax<- vAA
    leal    (rPC,%ecx,2),%ecx           # ecx<- PC + BBBBbbbb*2
    movl    %eax,OUT_ARG1(%esp)         # ARG1<- vAA
    movl    %ecx,OUT_ARG0(%esp)         # ARG0<- switchData
    SPILL(rPC)
    call    dvmInterpHandlePackedSwitch
    UNSPILL(rPC)
    testl   %eax,%eax
    movl    %eax,rINST_FULL             # set up word offset
    jle     common_backwardBranch       # check on special actions
    ADVANCE_PC_INDEXED(rINST_FULL)
    FETCH_INST()
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_SPARSE_SWITCH: /* 0x2c */
/* File: x86/OP_SPARSE_SWITCH.S */
/* File: x86/OP_PACKED_SWITCH.S */
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
    movzbl  rINST_HI,rINST_FULL         # rINST_FULL<- AA
    movl    2(rPC),%ecx                 # ecx<- BBBBbbbb
    GET_VREG(%eax,rINST_FULL)           # eax<- vAA
    leal    (rPC,%ecx,2),%ecx           # ecx<- PC + BBBBbbbb*2
    movl    %eax,OUT_ARG1(%esp)         # ARG1<- vAA
    movl    %ecx,OUT_ARG0(%esp)         # ARG0<- switchData
    SPILL(rPC)
    call    dvmInterpHandleSparseSwitch
    UNSPILL(rPC)
    testl   %eax,%eax
    movl    %eax,rINST_FULL             # set up word offset
    jle     common_backwardBranch       # check on special actions
    ADVANCE_PC_INDEXED(rINST_FULL)
    FETCH_INST()
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CMPL_FLOAT: /* 0x2d */
/* File: x86/OP_CMPL_FLOAT.S */
/* File: x86/OP_CMPG_DOUBLE.S */
    /* float/double_cmp[gl] vAA, vBB, vCC */
    movzbl    3(rPC),%eax             # eax<- CC
    movzbl    2(rPC),%ecx             # ecx<- BB
    .if 0
    fldl     (rFP,%eax,4)
    fldl     (rFP,%ecx,4)
    .else
    flds     (rFP,%eax,4)
    flds     (rFP,%ecx,4)
    .endif
    movzbl   rINST_HI,rINST_FULL
    xorl     %ecx,%ecx
    fucompp     # z if equal, p set if NaN, c set if st0 < st1
    fnstsw   %ax
    sahf
    movl      rINST_FULL,%eax
    FETCH_INST_WORD(2)
    jp       .LOP_CMPL_FLOAT_isNaN
    je       .LOP_CMPL_FLOAT_finish
    sbbl     %ecx,%ecx
    jb       .LOP_CMPL_FLOAT_finish
    incl     %ecx
.LOP_CMPL_FLOAT_finish:
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CMPG_FLOAT: /* 0x2e */
/* File: x86/OP_CMPG_FLOAT.S */
/* File: x86/OP_CMPG_DOUBLE.S */
    /* float/double_cmp[gl] vAA, vBB, vCC */
    movzbl    3(rPC),%eax             # eax<- CC
    movzbl    2(rPC),%ecx             # ecx<- BB
    .if 0
    fldl     (rFP,%eax,4)
    fldl     (rFP,%ecx,4)
    .else
    flds     (rFP,%eax,4)
    flds     (rFP,%ecx,4)
    .endif
    movzbl   rINST_HI,rINST_FULL
    xorl     %ecx,%ecx
    fucompp     # z if equal, p set if NaN, c set if st0 < st1
    fnstsw   %ax
    sahf
    movl      rINST_FULL,%eax
    FETCH_INST_WORD(2)
    jp       .LOP_CMPG_FLOAT_isNaN
    je       .LOP_CMPG_FLOAT_finish
    sbbl     %ecx,%ecx
    jb       .LOP_CMPG_FLOAT_finish
    incl     %ecx
.LOP_CMPG_FLOAT_finish:
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CMPL_DOUBLE: /* 0x2f */
/* File: x86/OP_CMPL_DOUBLE.S */
/* File: x86/OP_CMPG_DOUBLE.S */
    /* float/double_cmp[gl] vAA, vBB, vCC */
    movzbl    3(rPC),%eax             # eax<- CC
    movzbl    2(rPC),%ecx             # ecx<- BB
    .if 1
    fldl     (rFP,%eax,4)
    fldl     (rFP,%ecx,4)
    .else
    flds     (rFP,%eax,4)
    flds     (rFP,%ecx,4)
    .endif
    movzbl   rINST_HI,rINST_FULL
    xorl     %ecx,%ecx
    fucompp     # z if equal, p set if NaN, c set if st0 < st1
    fnstsw   %ax
    sahf
    movl      rINST_FULL,%eax
    FETCH_INST_WORD(2)
    jp       .LOP_CMPL_DOUBLE_isNaN
    je       .LOP_CMPL_DOUBLE_finish
    sbbl     %ecx,%ecx
    jb       .LOP_CMPL_DOUBLE_finish
    incl     %ecx
.LOP_CMPL_DOUBLE_finish:
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_CMPG_DOUBLE: /* 0x30 */
/* File: x86/OP_CMPG_DOUBLE.S */
    /* float/double_cmp[gl] vAA, vBB, vCC */
    movzbl    3(rPC),%eax             # eax<- CC
    movzbl    2(rPC),%ecx             # ecx<- BB
    .if 1
    fldl     (rFP,%eax,4)
    fldl     (rFP,%ecx,4)
    .else
    flds     (rFP,%eax,4)
    flds     (rFP,%ecx,4)
    .endif
    movzbl   rINST_HI,rINST_FULL
    xorl     %ecx,%ecx
    fucompp     # z if equal, p set if NaN, c set if st0 < st1
    fnstsw   %ax
    sahf
    movl      rINST_FULL,%eax
    FETCH_INST_WORD(2)
    jp       .LOP_CMPG_DOUBLE_isNaN
    je       .LOP_CMPG_DOUBLE_finish
    sbbl     %ecx,%ecx
    jb       .LOP_CMPG_DOUBLE_finish
    incl     %ecx
.LOP_CMPG_DOUBLE_finish:
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_CMP_LONG: /* 0x31 */
/* File: x86/OP_CMP_LONG.S */
    /*
     * Compare two 64-bit values.  Puts 0, 1, or -1 into the destination
     * register based on the results of the comparison.
     */
    /* cmp-long vAA, vBB, vCC */
    movzbl    2(rPC),%ecx              # ecx<- BB
    SPILL(rPC)
    movzbl    3(rPC),rPC               # rPC<- CC
    GET_VREG_WORD(%eax,%ecx,1)         # eax<- v[BB+1]
    GET_VREG_WORD(%ecx,%ecx,0)         # ecx<- v[BB+0]
    movzbl    rINST_HI,rINST_FULL      # rINST_FULL<- AA
    cmpl      4(rFP,rPC,4),%eax
    jl        .LOP_CMP_LONG_smaller
    jg        .LOP_CMP_LONG_bigger
    sub       (rFP,rPC,4),%ecx
    ja        .LOP_CMP_LONG_bigger
    jb        .LOP_CMP_LONG_smaller
    UNSPILL(rPC)
    jmp       .LOP_CMP_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_IF_EQ: /* 0x32 */
/* File: x86/OP_IF_EQ.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    jne   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_NE: /* 0x33 */
/* File: x86/OP_IF_NE.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    je   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_LT: /* 0x34 */
/* File: x86/OP_IF_LT.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    jge   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_GE: /* 0x35 */
/* File: x86/OP_IF_GE.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    jl   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_GT: /* 0x36 */
/* File: x86/OP_IF_GT.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    jle   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_LE: /* 0x37 */
/* File: x86/OP_IF_LE.S */
/* File: x86/bincmp.S */
    /*
     * Generic two-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * For: if-eq, if-ne, if-lt, if-ge, if-gt, if-le
     */
    /* if-cmp vA, vB, +CCCC */
    movzx    rINST_HI,%ecx              # ecx <- A+
    andb     $0xf,%cl                  # ecx <- A
    GET_VREG(%eax,%ecx)                 # eax <- vA
    sarl     $12,rINST_FULL            # rINST_FULL<- B
    cmpl     (rFP,rINST_FULL,4),%eax    # compare (vA, vB)
    movswl   2(rPC),rINST_FULL          # Get signed branch offset
    movl     $2,%eax                   # assume not taken
    jg   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IF_EQZ: /* 0x38 */
/* File: x86/OP_IF_EQZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    jne   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IF_NEZ: /* 0x39 */
/* File: x86/OP_IF_NEZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    je   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IF_LTZ: /* 0x3a */
/* File: x86/OP_IF_LTZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    jge   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IF_GEZ: /* 0x3b */
/* File: x86/OP_IF_GEZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    jl   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IF_GTZ: /* 0x3c */
/* File: x86/OP_IF_GTZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    jle   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IF_LEZ: /* 0x3d */
/* File: x86/OP_IF_LEZ.S */
/* File: x86/zcmp.S */
    /*
     * Generic one-operand compare-and-branch operation.  Provide a "revcmp"
     * fragment that specifies the *reverse* comparison to perform, e.g.
     * for "if-le" you would use "gt".
     *
     * for: if-eqz, if-nez, if-ltz, if-gez, if-gtz, if-lez
     */
    /* if-cmp vAA, +BBBB */
    movzx    rINST_HI,%ecx             # ecx <- AA
    cmpl     $0,(rFP,%ecx,4)          # compare (vA, 0)
    movswl   2(rPC),rINST_FULL         # fetch signed displacement
    movl     $2,%eax                  # assume branch not taken
    jg   1f
    testl    rINST_FULL,rINST_FULL
    js       common_backwardBranch
    movl     rINST_FULL,%eax
1:
    FETCH_INST_INDEXED(%eax)
    ADVANCE_PC_INDEXED(%eax)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3E: /* 0x3e */
/* File: x86/OP_UNUSED_3E.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_3F: /* 0x3f */
/* File: x86/OP_UNUSED_3F.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_40: /* 0x40 */
/* File: x86/OP_UNUSED_40.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_41: /* 0x41 */
/* File: x86/OP_UNUSED_41.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_42: /* 0x42 */
/* File: x86/OP_UNUSED_42.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_43: /* 0x43 */
/* File: x86/OP_UNUSED_43.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_AGET: /* 0x44 */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movl     offArrayObject_contents(%eax,%ecx,4),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_AGET_WIDE: /* 0x45 */
/* File: x86/OP_AGET_WIDE.S */
    /*
     * Array get, 64 bits.  vAA <- vBB[vCC].
     *
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jb        .LOP_AGET_WIDE_finish        # index < length, OK
    jmp       common_errArrayIndex      # index >= length, bail

/* ------------------------------ */
    .balign 64
.L_OP_AGET_OBJECT: /* 0x46 */
/* File: x86/OP_AGET_OBJECT.S */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movl     offArrayObject_contents(%eax,%ecx,4),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_AGET_BOOLEAN: /* 0x47 */
/* File: x86/OP_AGET_BOOLEAN.S */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movzbl     offArrayObject_contents(%eax,%ecx,1),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_AGET_BYTE: /* 0x48 */
/* File: x86/OP_AGET_BYTE.S */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movsbl     offArrayObject_contents(%eax,%ecx,1),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_AGET_CHAR: /* 0x49 */
/* File: x86/OP_AGET_CHAR.S */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movzwl     offArrayObject_contents(%eax,%ecx,2),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_AGET_SHORT: /* 0x4a */
/* File: x86/OP_AGET_SHORT.S */
/* File: x86/OP_AGET.S */
    /*
     * Array get, 32 bits or less.  vAA <- vBB[vCC].
     *
     * for: aget, aget-object, aget-boolean, aget-byte, aget-char, aget-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    movswl     offArrayObject_contents(%eax,%ecx,2),%eax
    movl      rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_APUT: /* 0x4b */
/* File: x86/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    leal      offArrayObject_contents(%eax,%ecx,4),%eax
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    movl     %ecx,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_APUT_WIDE: /* 0x4c */
/* File: x86/OP_APUT_WIDE.S */
    /*
     * Array put, 64 bits.  vBB[vCC]<-vAA.
     *
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jb        .LOP_APUT_WIDE_finish        # index < length, OK
    jmp       common_errArrayIndex      # index >= length, bail

/* ------------------------------ */
    .balign 64
.L_OP_APUT_OBJECT: /* 0x4d */
/* File: x86/OP_APUT_OBJECT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- vAA
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jb        .LOP_APUT_OBJECT_continue
    jmp       common_errArrayIndex      # index >= length, bail

/* ------------------------------ */
    .balign 64
.L_OP_APUT_BOOLEAN: /* 0x4e */
/* File: x86/OP_APUT_BOOLEAN.S */
/* File: x86/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    leal      offArrayObject_contents(%eax,%ecx,1),%eax
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    movb     %cl,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_APUT_BYTE: /* 0x4f */
/* File: x86/OP_APUT_BYTE.S */
/* File: x86/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    leal      offArrayObject_contents(%eax,%ecx,1),%eax
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    movb     %cl,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_APUT_CHAR: /* 0x50 */
/* File: x86/OP_APUT_CHAR.S */
/* File: x86/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    leal      offArrayObject_contents(%eax,%ecx,2),%eax
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    movw     %cx,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_APUT_SHORT: /* 0x51 */
/* File: x86/OP_APUT_SHORT.S */
/* File: x86/OP_APUT.S */
    /*
     * Array put, 32 bits or less.  vBB[vCC] <- vAA
     *
     * for: aput, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     */
    /* op vAA, vBB, vCC */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    GET_VREG(%eax,%eax)                 # eax<- vBB (array object)
    GET_VREG(%ecx,%ecx)                 # ecs<- vCC (requested index)
    testl     %eax,%eax                 # null array object?
    je        common_errNullObject      # bail if so
    cmpl      offArrayObject_length(%eax),%ecx
    jae       common_errArrayIndex      # index >= length, bail
    leal      offArrayObject_contents(%eax,%ecx,2),%eax
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    movw     %cx,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_IGET: /* 0x52 */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_resolve

/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE: /* 0x53 */
/* File: x86/OP_IGET_WIDE.S */
    /*
     * 64-bit instance field get.
     *
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_WIDE_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_WIDE_resolve

/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT: /* 0x54 */
/* File: x86/OP_IGET_OBJECT.S */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_OBJECT_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_OBJECT_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BOOLEAN: /* 0x55 */
/* File: x86/OP_IGET_BOOLEAN.S */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_BOOLEAN_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_BOOLEAN_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IGET_BYTE: /* 0x56 */
/* File: x86/OP_IGET_BYTE.S */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_BYTE_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_BYTE_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IGET_CHAR: /* 0x57 */
/* File: x86/OP_IGET_CHAR.S */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_CHAR_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_CHAR_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IGET_SHORT: /* 0x58 */
/* File: x86/OP_IGET_SHORT.S */
/* File: x86/OP_IGET.S */
    /*
     * General 32-bit instance field get.
     *
     * for: iget, iget-object, iget-boolean, iget-byte, iget-char, iget-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IGET_SHORT_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)                 # needed by dvmResolveInstField
    GET_GLUE(rIBASE)
    jmp     .LOP_IGET_SHORT_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IPUT: /* 0x59 */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_resolve

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE: /* 0x5a */
/* File: x86/OP_IPUT_WIDE.S */
    /*
     * 64-bit instance field put.
     *
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_WIDE_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_WIDE_resolve

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT: /* 0x5b */
/* File: x86/OP_IPUT_OBJECT.S */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_OBJECT_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_OBJECT_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BOOLEAN: /* 0x5c */
/* File: x86/OP_IPUT_BOOLEAN.S */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_BOOLEAN_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_BOOLEAN_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_BYTE: /* 0x5d */
/* File: x86/OP_IPUT_BYTE.S */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_BYTE_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_BYTE_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_CHAR: /* 0x5e */
/* File: x86/OP_IPUT_CHAR.S */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_CHAR_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_CHAR_resolve


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_SHORT: /* 0x5f */
/* File: x86/OP_IPUT_SHORT.S */
/* File: x86/OP_IPUT.S */

    /*
     * General 32-bit instance field put.
     *
     * for: iput, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     */
    /* op vA, vB, field@CCCC */
    GET_GLUE(%ecx)
    SPILL(rIBASE)                                 # need another reg
    movzwl  2(rPC),rIBASE                         # rIBASE<- 0000CCCC
    movl    offGlue_methodClassDex(%ecx),%eax     # eax<- DvmDex
    movzbl  rINST_HI,%ecx                         # ecx<- BA
    sarl    $4,%ecx                              # ecx<- B
    movl    offDvmDex_pResFields(%eax),%eax       # eax<- pDvmDex->pResFields
    movzbl  rINST_HI,rINST_FULL                   # rINST_FULL<- BA
    andb    $0xf,rINST_LO                        # rINST_FULL<- A
    GET_VREG(%ecx,%ecx)                           # ecx<- fp[B], the object ptr
    movl    (%eax,rIBASE,4),%eax                  # resolved entry
    testl   %eax,%eax                             # is resolved entry null?
    jne     .LOP_IPUT_SHORT_finish                    # no, already resolved
    movl    rIBASE,OUT_ARG1(%esp)
    GET_GLUE(rIBASE)
    jmp     .LOP_IPUT_SHORT_resolve


/* ------------------------------ */
    .balign 64
.L_OP_SGET: /* 0x60 */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_resolve                # if not, make it so
.LOP_SGET_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_SGET_WIDE: /* 0x61 */
/* File: x86/OP_SGET_WIDE.S */
    /*
     * 64-bit SGET handler.
     *
     */
    /* sget-wide vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_WIDE_resolve                # if not, make it so
.LOP_SGET_WIDE_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%ecx    # ecx<- lsw
    movl      4+offStaticField_value(%eax),%eax  # eax<- msw
    movzbl    rINST_HI,rINST_FULL                # rINST_FULL<- AA
    SET_VREG_WORD(%ecx,rINST_FULL,0)
    SET_VREG_WORD(%eax,rINST_FULL,1)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_SGET_OBJECT: /* 0x62 */
/* File: x86/OP_SGET_OBJECT.S */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_OBJECT_resolve                # if not, make it so
.LOP_SGET_OBJECT_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BOOLEAN: /* 0x63 */
/* File: x86/OP_SGET_BOOLEAN.S */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_BOOLEAN_resolve                # if not, make it so
.LOP_SGET_BOOLEAN_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SGET_BYTE: /* 0x64 */
/* File: x86/OP_SGET_BYTE.S */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_BYTE_resolve                # if not, make it so
.LOP_SGET_BYTE_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SGET_CHAR: /* 0x65 */
/* File: x86/OP_SGET_CHAR.S */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_CHAR_resolve                # if not, make it so
.LOP_SGET_CHAR_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SGET_SHORT: /* 0x66 */
/* File: x86/OP_SGET_SHORT.S */
/* File: x86/OP_SGET.S */
    /*
     * General 32-bit SGET handler.
     *
     * for: sget, sget-object, sget-boolean, sget-byte, sget-char, sget-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SGET_SHORT_resolve                # if not, make it so
.LOP_SGET_SHORT_finish:     # field ptr in eax
    movl      offStaticField_value(%eax),%eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SPUT: /* 0x67 */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_resolve                # if not, make it so
.LOP_SPUT_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_WIDE: /* 0x68 */
/* File: x86/OP_SPUT_WIDE.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_WIDE_resolve                # if not, make it so
.LOP_SPUT_WIDE_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG_WORD(rINST_FULL,%ecx,0)             # rINST_FULL<- lsw
    GET_VREG_WORD(%ecx,%ecx,1)                   # ecx<- msw
    movl      rINST_FULL,offStaticField_value(%eax)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    movl      %ecx,4+offStaticField_value(%eax)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_SPUT_OBJECT: /* 0x69 */
/* File: x86/OP_SPUT_OBJECT.S */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_OBJECT_resolve                # if not, make it so
.LOP_SPUT_OBJECT_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BOOLEAN: /* 0x6a */
/* File: x86/OP_SPUT_BOOLEAN.S */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_BOOLEAN_resolve                # if not, make it so
.LOP_SPUT_BOOLEAN_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_BYTE: /* 0x6b */
/* File: x86/OP_SPUT_BYTE.S */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_BYTE_resolve                # if not, make it so
.LOP_SPUT_BYTE_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_CHAR: /* 0x6c */
/* File: x86/OP_SPUT_CHAR.S */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_CHAR_resolve                # if not, make it so
.LOP_SPUT_CHAR_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SPUT_SHORT: /* 0x6d */
/* File: x86/OP_SPUT_SHORT.S */
/* File: x86/OP_SPUT.S */
    /*
     * General 32-bit SPUT handler.
     *
     * for: sput, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     */
    /* op vAA, field@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax                        # eax<- field ref BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx  # ecx<- DvmDex
    movl      offDvmDex_pResFields(%ecx),%ecx    # ecx<- dvmDex->pResFields
    movl      (%ecx,%eax,4),%eax                 # eax<- resolved StaticField ptr
    testl     %eax,%eax                          # resolved entry null?
    je        .LOP_SPUT_SHORT_resolve                # if not, make it so
.LOP_SPUT_SHORT_finish:     # field ptr in eax
    movzbl    rINST_HI,%ecx                      # ecx<- AA
    GET_VREG(%ecx,%ecx)
    FETCH_INST_WORD(2)
    movl      %ecx,offStaticField_value(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL: /* 0x6e */
/* File: x86/OP_INVOKE_VIRTUAL.S */

    /*
     * Handle a virtual method call.
     *
     * for: invoke-virtual, invoke-virtual/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%eax)
    movzwl    2(rPC),%ecx                 # ecx<- BBBB
    movl      offGlue_methodClassDex(%eax),%eax  # eax<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%eax),%eax   # eax<- pDvmDex->pResMethods
    movl      (%eax,%ecx,4),%eax          # eax<- resolved baseMethod
    testl     %eax,%eax                   # already resolved?
    jne       .LOP_INVOKE_VIRTUAL_continue        # yes, continue
    GET_GLUE(%eax)
    movl      %ecx,OUT_ARG1(%esp)         # arg1<- ref
    movl      offGlue_method(%eax),%eax   # eax<- glue->method
    SPILL(rPC)
    jmp       .LOP_INVOKE_VIRTUAL_more

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER: /* 0x6f */
/* File: x86/OP_INVOKE_SUPER.S */
    /*
     * Handle a "super" method call.
     *
     * for: invoke-super, invoke-super/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(rINST_FULL)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(rINST_FULL),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%ecx),%ecx # ecx<- pDvmDex->pResMethods
    movl      (%ecx,%eax,4),%ecx        # ecx<- resolved baseMethod
    movl      offGlue_method(rINST_FULL),%eax # eax<- method
    movzwl    4(rPC),rINST_FULL         # rINST_FULL<- GFED or CCCC
    .if       (!0)
    andl      $0xf,rINST_FULL          # rINST_FULL<- D (or stays CCCC)
    .endif
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- "this" ptr
    testl     rINST_FULL,rINST_FULL     # null "this"?
    je        common_errNullObject      # yes, throw
    movl      offMethod_clazz(%eax),%eax # eax<- method->clazz
    testl     %ecx,%ecx                 # already resolved?
    jne       .LOP_INVOKE_SUPER_continue      # yes - go on
    jmp       .LOP_INVOKE_SUPER_resolve

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT: /* 0x70 */
/* File: x86/OP_INVOKE_DIRECT.S */
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
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax              # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    SPILL(rPC)
    movl      offDvmDex_pResMethods(%ecx),%ecx  # ecx<- pDvmDex->pResMethods
    movzwl    4(rPC),rPC               # rPC<- GFED or CCCC
    movl      (%ecx,%eax,4),%eax       # eax<- resolved methodToCall
    .if       (!0)
    andl      $0xf,rPC                # rPC<- D (or stays CCCC)
    .endif
    testl     %eax,%eax                # already resolved?
    GET_VREG(%ecx,rPC)                 # ecx<- "this" ptr
    je        .LOP_INVOKE_DIRECT_resolve      # not resolved, do it now
.LOP_INVOKE_DIRECT_finish:
    UNSPILL(rPC)
    testl     %ecx,%ecx                # null "this"?
    jne       common_invokeMethodNoRange  # no, continue on
    jmp       common_errNullObject

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC: /* 0x71 */
/* File: x86/OP_INVOKE_STATIC.S */
    /*
     * Handle a static method call.
     *
     * for: invoke-static, invoke-static/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%ecx),%ecx  # ecx<- pDvmDex->pResMethods
    movl      (%ecx,%eax,4),%eax        # eax<- resolved methodToCall
    testl     %eax,%eax
    jne       common_invokeMethodNoRange
    GET_GLUE(%ecx)
    movl      offGlue_method(%ecx),%ecx # ecx<- glue->method
    movzwl    2(rPC),%eax
    movl      offMethod_clazz(%ecx),%ecx# ecx<- method->clazz
    movl      %eax,OUT_ARG1(%esp)       # arg1<- BBBB
    movl      %ecx,OUT_ARG0(%esp)       # arg0<- clazz
    jmp       .LOP_INVOKE_STATIC_continue

/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE: /* 0x72 */
/* File: x86/OP_INVOKE_INTERFACE.S */
    /*
     * Handle an interface method call.
     *
     * for: invoke-interface, invoke-interface/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    movzwl     4(rPC),%eax              # eax<- FEDC or CCCC
    GET_GLUE(%ecx)
    .if        (!0)
    andl       $0xf,%eax               # eax<- C (or stays CCCC)
    .endif
    GET_VREG(%eax,%eax)                 # eax<- "this"
    EXPORT_PC()
    testl      %eax,%eax                # null this?
    je         common_errNullObject     # yes, fail
    movl       offObject_clazz(%eax),%eax# eax<- thisPtr->clazz
    movl       %eax,OUT_ARG0(%esp)                 # arg0<- class
    movl       offGlue_methodClassDex(%ecx),%eax   # eax<- methodClassDex
    movl       offGlue_method(%ecx),%ecx           # ecx<- method
    movl       %eax,OUT_ARG3(%esp)                 # arg3<- dex
    movzwl     2(rPC),%eax                         # eax<- BBBB
    movl       %ecx,OUT_ARG2(%esp)                 # arg2<- method
    movl       %eax,OUT_ARG1(%esp)                 # arg1<- BBBB
    SPILL(rPC)
    jmp        .LOP_INVOKE_INTERFACE_continue

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_73: /* 0x73 */
/* File: x86/OP_UNUSED_73.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_RANGE: /* 0x74 */
/* File: x86/OP_INVOKE_VIRTUAL_RANGE.S */
/* File: x86/OP_INVOKE_VIRTUAL.S */

    /*
     * Handle a virtual method call.
     *
     * for: invoke-virtual, invoke-virtual/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%eax)
    movzwl    2(rPC),%ecx                 # ecx<- BBBB
    movl      offGlue_methodClassDex(%eax),%eax  # eax<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%eax),%eax   # eax<- pDvmDex->pResMethods
    movl      (%eax,%ecx,4),%eax          # eax<- resolved baseMethod
    testl     %eax,%eax                   # already resolved?
    jne       .LOP_INVOKE_VIRTUAL_RANGE_continue        # yes, continue
    GET_GLUE(%eax)
    movl      %ecx,OUT_ARG1(%esp)         # arg1<- ref
    movl      offGlue_method(%eax),%eax   # eax<- glue->method
    SPILL(rPC)
    jmp       .LOP_INVOKE_VIRTUAL_RANGE_more


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_RANGE: /* 0x75 */
/* File: x86/OP_INVOKE_SUPER_RANGE.S */
/* File: x86/OP_INVOKE_SUPER.S */
    /*
     * Handle a "super" method call.
     *
     * for: invoke-super, invoke-super/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(rINST_FULL)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(rINST_FULL),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%ecx),%ecx # ecx<- pDvmDex->pResMethods
    movl      (%ecx,%eax,4),%ecx        # ecx<- resolved baseMethod
    movl      offGlue_method(rINST_FULL),%eax # eax<- method
    movzwl    4(rPC),rINST_FULL         # rINST_FULL<- GFED or CCCC
    .if       (!1)
    andl      $0xf,rINST_FULL          # rINST_FULL<- D (or stays CCCC)
    .endif
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- "this" ptr
    testl     rINST_FULL,rINST_FULL     # null "this"?
    je        common_errNullObject      # yes, throw
    movl      offMethod_clazz(%eax),%eax # eax<- method->clazz
    testl     %ecx,%ecx                 # already resolved?
    jne       .LOP_INVOKE_SUPER_RANGE_continue      # yes - go on
    jmp       .LOP_INVOKE_SUPER_RANGE_resolve


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_RANGE: /* 0x76 */
/* File: x86/OP_INVOKE_DIRECT_RANGE.S */
/* File: x86/OP_INVOKE_DIRECT.S */
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
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax              # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    SPILL(rPC)
    movl      offDvmDex_pResMethods(%ecx),%ecx  # ecx<- pDvmDex->pResMethods
    movzwl    4(rPC),rPC               # rPC<- GFED or CCCC
    movl      (%ecx,%eax,4),%eax       # eax<- resolved methodToCall
    .if       (!1)
    andl      $0xf,rPC                # rPC<- D (or stays CCCC)
    .endif
    testl     %eax,%eax                # already resolved?
    GET_VREG(%ecx,rPC)                 # ecx<- "this" ptr
    je        .LOP_INVOKE_DIRECT_RANGE_resolve      # not resolved, do it now
.LOP_INVOKE_DIRECT_RANGE_finish:
    UNSPILL(rPC)
    testl     %ecx,%ecx                # null "this"?
    jne       common_invokeMethodRange  # no, continue on
    jmp       common_errNullObject


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_STATIC_RANGE: /* 0x77 */
/* File: x86/OP_INVOKE_STATIC_RANGE.S */
/* File: x86/OP_INVOKE_STATIC.S */
    /*
     * Handle a static method call.
     *
     * for: invoke-static, invoke-static/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%ecx)
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offGlue_methodClassDex(%ecx),%ecx # ecx<- pDvmDex
    EXPORT_PC()
    movl      offDvmDex_pResMethods(%ecx),%ecx  # ecx<- pDvmDex->pResMethods
    movl      (%ecx,%eax,4),%eax        # eax<- resolved methodToCall
    testl     %eax,%eax
    jne       common_invokeMethodRange
    GET_GLUE(%ecx)
    movl      offGlue_method(%ecx),%ecx # ecx<- glue->method
    movzwl    2(rPC),%eax
    movl      offMethod_clazz(%ecx),%ecx# ecx<- method->clazz
    movl      %eax,OUT_ARG1(%esp)       # arg1<- BBBB
    movl      %ecx,OUT_ARG0(%esp)       # arg0<- clazz
    jmp       .LOP_INVOKE_STATIC_RANGE_continue


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_INTERFACE_RANGE: /* 0x78 */
/* File: x86/OP_INVOKE_INTERFACE_RANGE.S */
/* File: x86/OP_INVOKE_INTERFACE.S */
    /*
     * Handle an interface method call.
     *
     * for: invoke-interface, invoke-interface/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    movzwl     4(rPC),%eax              # eax<- FEDC or CCCC
    GET_GLUE(%ecx)
    .if        (!1)
    andl       $0xf,%eax               # eax<- C (or stays CCCC)
    .endif
    GET_VREG(%eax,%eax)                 # eax<- "this"
    EXPORT_PC()
    testl      %eax,%eax                # null this?
    je         common_errNullObject     # yes, fail
    movl       offObject_clazz(%eax),%eax# eax<- thisPtr->clazz
    movl       %eax,OUT_ARG0(%esp)                 # arg0<- class
    movl       offGlue_methodClassDex(%ecx),%eax   # eax<- methodClassDex
    movl       offGlue_method(%ecx),%ecx           # ecx<- method
    movl       %eax,OUT_ARG3(%esp)                 # arg3<- dex
    movzwl     2(rPC),%eax                         # eax<- BBBB
    movl       %ecx,OUT_ARG2(%esp)                 # arg2<- method
    movl       %eax,OUT_ARG1(%esp)                 # arg1<- BBBB
    SPILL(rPC)
    jmp        .LOP_INVOKE_INTERFACE_RANGE_continue


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_79: /* 0x79 */
/* File: x86/OP_UNUSED_79.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_7A: /* 0x7a */
/* File: x86/OP_UNUSED_7A.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_NEG_INT: /* 0x7b */
/* File: x86/OP_NEG_INT.S */
/* File: x86/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op eax".
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)        # eax<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    
    negl %eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_NOT_INT: /* 0x7c */
/* File: x86/OP_NOT_INT.S */
/* File: x86/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op eax".
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)        # eax<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    
    notl %eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_NEG_LONG: /* 0x7d */
/* File: x86/OP_NEG_LONG.S */
    /* unop vA, vB */
    movzbl    rINST_HI,%ecx            # ecx<- BA
    sarl      $4,%ecx                 # ecx<- B
    movzbl    rINST_HI,rINST_FULL      # ecx<- BA
    andb      $0xf,rINST_LO           # rINST_FULL<- A
    GET_VREG_WORD(%eax,%ecx,0)         # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)         # ecx<- v[B+1]
    negl      %eax
    adcl      $0,%ecx
    negl      %ecx
    SET_VREG_WORD(%eax,rINST_FULL,0)   # v[A+0]<- eax
    SET_VREG_WORD(%ecx,rINST_FULL,1)   # v[A+1]<- ecx
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_NOT_LONG: /* 0x7e */
/* File: x86/OP_NOT_LONG.S */
    /* unop vA, vB */
    movzbl    rINST_HI,%ecx            # ecx<- BA
    sarl      $4,%ecx                 # ecx<- B
    movzbl    rINST_HI,rINST_FULL      # ecx<- BA
    andb      $0xf,rINST_LO           # rINST_FULL<- A
    GET_VREG_WORD(%eax,%ecx,0)         # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)         # ecx<- v[B+1]
    notl      %eax
    notl      %ecx
    SET_VREG_WORD(%eax,rINST_FULL,0)   # v[A+0]<- eax
    SET_VREG_WORD(%ecx,rINST_FULL,1)   # v[A+1]<- ecx
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_NEG_FLOAT: /* 0x7f */
/* File: x86/OP_NEG_FLOAT.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    flds    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fchs
    fstps  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_NEG_DOUBLE: /* 0x80 */
/* File: x86/OP_NEG_DOUBLE.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fldl    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fchs
    fstpl  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_LONG: /* 0x81 */
/* File: x86/OP_INT_TO_LONG.S */
    /* int to long vA, vB */
    movzbl  rINST_HI,%ecx               # ecx<- +A
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    SPILL(rPC)                          # will step on edx later
    andb    $0xf,%cl                   # ecx<- A
    cltd                                # edx:eax<- sssssssBBBBBBBB
    SET_VREG_WORD(%edx,%ecx,1)          # v[A+1]<- edx/rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)          # v[A+0]<- %eax
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_FLOAT: /* 0x82 */
/* File: x86/OP_INT_TO_FLOAT.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fildl    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstps  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_DOUBLE: /* 0x83 */
/* File: x86/OP_INT_TO_DOUBLE.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fildl    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstpl  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_INT: /* 0x84 */
/* File: x86/OP_LONG_TO_INT.S */
/* we ignore the high word, making this equivalent to a 32-bit reg move */
/* File: x86/OP_MOVE.S */
    /* for move, move-object, long-to-int */
    /* op vA, vB */
    movzbl rINST_HI,%eax         # eax<- BA
    andb   $0xf,%al             # eax<- A
    shrl   $12,rINST_FULL       # rINST_FULL<- B
    GET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    SET_VREG(%ecx,%eax)          # fp[A]<-fp[B]
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_FLOAT: /* 0x85 */
/* File: x86/OP_LONG_TO_FLOAT.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fildll    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstps  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_LONG_TO_DOUBLE: /* 0x86 */
/* File: x86/OP_LONG_TO_DOUBLE.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fildll    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstpl  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_INT: /* 0x87 */
/* File: x86/OP_FLOAT_TO_INT.S */
/* File: x86/cvtfp_int.S */
/* On fp to int conversions, Java requires that
 * if the result > maxint, it should be clamped to maxint.  If it is less
 * than minint, it should be clamped to minint.  If it is a nan, the result
 * should be zero.  Further, the rounding mode is to truncate.  This model
 * differs from what is delivered normally via the x86 fpu, so we have
 * to play some games.
 */
    /* float/double to int/long vA, vB */
    movzbl    rINST_HI,%ecx           # ecx<- A+
    sarl      $12,rINST_FULL         # rINST_FULL<- B
    .if 0
    fldl     (rFP,rINST_FULL,4)       # %st0<- vB
    .else
    flds     (rFP,rINST_FULL,4)       # %st0<- vB
    .endif
    ftst
    fnstcw   LOCAL0_OFFSET(%ebp)      # remember original rounding mode
    movzwl   LOCAL0_OFFSET(%ebp),%eax
    movb     $0xc,%ah
    movw     %ax,LOCAL0_OFFSET+2(%ebp)
    fldcw    LOCAL0_OFFSET+2(%ebp)      # set "to zero" rounding mode
    FETCH_INST_WORD(1)
    andb     $0xf,%cl                # ecx<- A
    .if 0
    fistpll  (rFP,%ecx,4)             # convert and store
    .else
    fistpl   (rFP,%ecx,4)             # convert and store
    .endif
    fldcw    LOCAL0_OFFSET(%ebp)      # restore previous rounding mode
    jmp      .LOP_FLOAT_TO_INT_continue


/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_LONG: /* 0x88 */
/* File: x86/OP_FLOAT_TO_LONG.S */
/* File: x86/cvtfp_int.S */
/* On fp to int conversions, Java requires that
 * if the result > maxint, it should be clamped to maxint.  If it is less
 * than minint, it should be clamped to minint.  If it is a nan, the result
 * should be zero.  Further, the rounding mode is to truncate.  This model
 * differs from what is delivered normally via the x86 fpu, so we have
 * to play some games.
 */
    /* float/double to int/long vA, vB */
    movzbl    rINST_HI,%ecx           # ecx<- A+
    sarl      $12,rINST_FULL         # rINST_FULL<- B
    .if 0
    fldl     (rFP,rINST_FULL,4)       # %st0<- vB
    .else
    flds     (rFP,rINST_FULL,4)       # %st0<- vB
    .endif
    ftst
    fnstcw   LOCAL0_OFFSET(%ebp)      # remember original rounding mode
    movzwl   LOCAL0_OFFSET(%ebp),%eax
    movb     $0xc,%ah
    movw     %ax,LOCAL0_OFFSET+2(%ebp)
    fldcw    LOCAL0_OFFSET+2(%ebp)      # set "to zero" rounding mode
    FETCH_INST_WORD(1)
    andb     $0xf,%cl                # ecx<- A
    .if 1
    fistpll  (rFP,%ecx,4)             # convert and store
    .else
    fistpl   (rFP,%ecx,4)             # convert and store
    .endif
    fldcw    LOCAL0_OFFSET(%ebp)      # restore previous rounding mode
    jmp      .LOP_FLOAT_TO_LONG_continue


/* ------------------------------ */
    .balign 64
.L_OP_FLOAT_TO_DOUBLE: /* 0x89 */
/* File: x86/OP_FLOAT_TO_DOUBLE.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    flds    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstpl  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_INT: /* 0x8a */
/* File: x86/OP_DOUBLE_TO_INT.S */
/* File: x86/cvtfp_int.S */
/* On fp to int conversions, Java requires that
 * if the result > maxint, it should be clamped to maxint.  If it is less
 * than minint, it should be clamped to minint.  If it is a nan, the result
 * should be zero.  Further, the rounding mode is to truncate.  This model
 * differs from what is delivered normally via the x86 fpu, so we have
 * to play some games.
 */
    /* float/double to int/long vA, vB */
    movzbl    rINST_HI,%ecx           # ecx<- A+
    sarl      $12,rINST_FULL         # rINST_FULL<- B
    .if 1
    fldl     (rFP,rINST_FULL,4)       # %st0<- vB
    .else
    flds     (rFP,rINST_FULL,4)       # %st0<- vB
    .endif
    ftst
    fnstcw   LOCAL0_OFFSET(%ebp)      # remember original rounding mode
    movzwl   LOCAL0_OFFSET(%ebp),%eax
    movb     $0xc,%ah
    movw     %ax,LOCAL0_OFFSET+2(%ebp)
    fldcw    LOCAL0_OFFSET+2(%ebp)      # set "to zero" rounding mode
    FETCH_INST_WORD(1)
    andb     $0xf,%cl                # ecx<- A
    .if 0
    fistpll  (rFP,%ecx,4)             # convert and store
    .else
    fistpl   (rFP,%ecx,4)             # convert and store
    .endif
    fldcw    LOCAL0_OFFSET(%ebp)      # restore previous rounding mode
    jmp      .LOP_DOUBLE_TO_INT_continue


/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_LONG: /* 0x8b */
/* File: x86/OP_DOUBLE_TO_LONG.S */
/* File: x86/cvtfp_int.S */
/* On fp to int conversions, Java requires that
 * if the result > maxint, it should be clamped to maxint.  If it is less
 * than minint, it should be clamped to minint.  If it is a nan, the result
 * should be zero.  Further, the rounding mode is to truncate.  This model
 * differs from what is delivered normally via the x86 fpu, so we have
 * to play some games.
 */
    /* float/double to int/long vA, vB */
    movzbl    rINST_HI,%ecx           # ecx<- A+
    sarl      $12,rINST_FULL         # rINST_FULL<- B
    .if 1
    fldl     (rFP,rINST_FULL,4)       # %st0<- vB
    .else
    flds     (rFP,rINST_FULL,4)       # %st0<- vB
    .endif
    ftst
    fnstcw   LOCAL0_OFFSET(%ebp)      # remember original rounding mode
    movzwl   LOCAL0_OFFSET(%ebp),%eax
    movb     $0xc,%ah
    movw     %ax,LOCAL0_OFFSET+2(%ebp)
    fldcw    LOCAL0_OFFSET+2(%ebp)      # set "to zero" rounding mode
    FETCH_INST_WORD(1)
    andb     $0xf,%cl                # ecx<- A
    .if 1
    fistpll  (rFP,%ecx,4)             # convert and store
    .else
    fistpl   (rFP,%ecx,4)             # convert and store
    .endif
    fldcw    LOCAL0_OFFSET(%ebp)      # restore previous rounding mode
    jmp      .LOP_DOUBLE_TO_LONG_continue


/* ------------------------------ */
    .balign 64
.L_OP_DOUBLE_TO_FLOAT: /* 0x8c */
/* File: x86/OP_DOUBLE_TO_FLOAT.S */
/* File: x86/fpcvt.S */
    /*
     * Generic 32-bit FP conversion operation.
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    fldl    (rFP,rINST_FULL,4)      # %st0<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    fstps  (rFP,%ecx,4)             # vA<- %st0
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_BYTE: /* 0x8d */
/* File: x86/OP_INT_TO_BYTE.S */
/* File: x86/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op eax".
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)        # eax<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    
    movsbl %al,%eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_CHAR: /* 0x8e */
/* File: x86/OP_INT_TO_CHAR.S */
/* File: x86/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op eax".
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)        # eax<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    
    movzwl %ax,%eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INT_TO_SHORT: /* 0x8f */
/* File: x86/OP_INT_TO_SHORT.S */
/* File: x86/unop.S */
    /*
     * Generic 32-bit unary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = op eax".
     */
    /* unop vA, vB */
    movzbl   rINST_HI,%ecx           # ecx<- A+
    sarl     $12,rINST_FULL         # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)        # eax<- vB
    andb     $0xf,%cl               # ecx<- A
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    
    
    movswl %ax,%eax
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT: /* 0x90 */
/* File: x86/OP_ADD_INT.S */
/* File: x86/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = eax op (rFP,%ecx,4)".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int, sub-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    addl (rFP,%ecx,4),%eax              # ex: addl    (rFP,%ecx,4),%eax
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT: /* 0x91 */
/* File: x86/OP_SUB_INT.S */
/* File: x86/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = eax op (rFP,%ecx,4)".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int, sub-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    subl   (rFP,%ecx,4),%eax              # ex: addl    (rFP,%ecx,4),%eax
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT: /* 0x92 */
/* File: x86/OP_MUL_INT.S */
    /*
     * 32-bit binary multiplication.
     */
    /* mul vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    SPILL(rPC)
    GET_VREG(%eax,%eax)             # eax<- vBB
    imull    (rFP,%ecx,4),%eax      # trashes rPC/edx
    UNSPILL(rPC)
    movzbl   rINST_HI,%ecx          # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT: /* 0x93 */
/* File: x86/OP_DIV_INT.S */
/* File: x86/bindiv.S */

    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_DIV_INT_continue_div
    cmpl     $0x80000000,%eax
    jne      .LOP_DIV_INT_continue_div
    movl     $0x80000000,%eax
    jmp      .LOP_DIV_INT_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT: /* 0x94 */
/* File: x86/OP_REM_INT.S */
/* File: x86/bindiv.S */

    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_REM_INT_continue_div
    cmpl     $0x80000000,%eax
    jne      .LOP_REM_INT_continue_div
    movl     $0,%edx
    jmp      .LOP_REM_INT_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT: /* 0x95 */
/* File: x86/OP_AND_INT.S */
/* File: x86/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = eax op (rFP,%ecx,4)".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int, sub-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    andl   (rFP,%ecx,4),%eax              # ex: addl    (rFP,%ecx,4),%eax
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_OR_INT: /* 0x96 */
/* File: x86/OP_OR_INT.S */
/* File: x86/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = eax op (rFP,%ecx,4)".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int, sub-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    orl   (rFP,%ecx,4),%eax              # ex: addl    (rFP,%ecx,4),%eax
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT: /* 0x97 */
/* File: x86/OP_XOR_INT.S */
/* File: x86/binop.S */
    /*
     * Generic 32-bit binary operation.  Provide an "instr" line that
     * specifies an instruction that performs "result = eax op (rFP,%ecx,4)".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int, sub-int, and-int, or-int,
     *      xor-int, shl-int, shr-int, ushr-int
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    xorl   (rFP,%ecx,4),%eax              # ex: addl    (rFP,%ecx,4),%eax
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT: /* 0x98 */
/* File: x86/OP_SHL_INT.S */
/* File: x86/binop1.S */
    /*
     * Generic 32-bit binary operation in which both operands loaded to
     * registers (op0 in eax, op1 in ecx).
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    sall    %cl,%eax                          # ex: addl    %ecx,%eax
    movzbl   rINST_HI,%ecx          # tmp<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT: /* 0x99 */
/* File: x86/OP_SHR_INT.S */
/* File: x86/binop1.S */
    /*
     * Generic 32-bit binary operation in which both operands loaded to
     * registers (op0 in eax, op1 in ecx).
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    sarl    %cl,%eax                          # ex: addl    %ecx,%eax
    movzbl   rINST_HI,%ecx          # tmp<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT: /* 0x9a */
/* File: x86/OP_USHR_INT.S */
/* File: x86/binop1.S */
    /*
     * Generic 32-bit binary operation in which both operands loaded to
     * registers (op0 in eax, op1 in ecx).
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- BB
    movzbl   3(rPC),%ecx            # ecx<- CC
    GET_VREG(%eax,%eax)             # eax<- vBB
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    shrl    %cl,%eax                          # ex: addl    %ecx,%eax
    movzbl   rINST_HI,%ecx          # tmp<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG: /* 0x9b */
/* File: x86/OP_ADD_LONG.S */
/* File: x86/binopWide.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop vAA, vBB, vCC */

    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)           # rPC<- v[BB+0]
    GET_VREG_WORD(%eax,%eax,1)          # eax<- v[BB+1]
    addl (rFP,%ecx,4),rPC         # ex: addl   (rFP,%ecx,4),rPC
    adcl 4(rFP,%ecx,4),%eax         # ex: adcl   4(rFP,%ecx,4),%eax
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    SET_VREG_WORD(rPC,rINST_FULL,0)     # v[AA+0] <- rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[AA+1] <- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG: /* 0x9c */
/* File: x86/OP_SUB_LONG.S */
/* File: x86/binopWide.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop vAA, vBB, vCC */

    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)           # rPC<- v[BB+0]
    GET_VREG_WORD(%eax,%eax,1)          # eax<- v[BB+1]
    subl (rFP,%ecx,4),rPC         # ex: addl   (rFP,%ecx,4),rPC
    sbbl 4(rFP,%ecx,4),%eax         # ex: adcl   4(rFP,%ecx,4),%eax
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    SET_VREG_WORD(rPC,rINST_FULL,0)     # v[AA+0] <- rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[AA+1] <- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG: /* 0x9d */
/* File: x86/OP_MUL_LONG.S */
    /*
     * Signed 64-bit integer multiply.
     *
     * We could definately use more free registers for
     * this code.  We must spill rPC (edx) because it
     * is used by imul.  We'll also spill rINST (ebx),
     * giving us eax, ebc, ecx and edx as computational
     * temps.  On top of that, we'll spill rIBASE (edi)
     * for use as the vB pointer and rFP (esi) for use
     * as the vC pointer.  Yuck.
     */
    /* mul-long vAA, vBB, vCC */
    movzbl    2(rPC),%eax              # eax<- B
    movzbl    3(rPC),%ecx              # ecx<- C
    SPILL(rPC)
    SPILL(rIBASE)
    SPILL(rFP)
    SPILL(rINST_FULL)
    leal      (rFP,%eax,4),rIBASE      # rIBASE<- &v[B]
    leal      (rFP,%ecx,4),rFP         # rFP<- &v[C]
    movl      4(rIBASE),%ecx      # ecx<- Bmsw
    imull     (rFP),%ecx          # ecx<- (Bmsw*Clsw)
    movl      4(rFP),%eax         # eax<- Cmsw
    imull     (rIBASE),%eax       # eax<- (Cmsw*Blsw)
    addl      %eax,%ecx           # ecx<- (Bmsw*Clsw)+(Cmsw*Blsw)
    movl      (rFP),%eax          # eax<- Clsw
    mull      (rIBASE)            # eax<- (Clsw*Alsw)
    UNSPILL(rINST_FULL)
    UNSPILL(rFP)
    jmp       .LOP_MUL_LONG_continue

/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG: /* 0x9e */
/* File: x86/OP_DIV_LONG.S */
    /* div vAA, vBB, vCC */
    movzbl    3(rPC),%eax              # eax<- CC
    movzbl    2(rPC),%ecx              # ecx<- BB
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)
    GET_VREG_WORD(%eax,%eax,1)
    movl     rPC,OUT_ARG2(%esp)
    testl    %eax,%eax
    je       .LOP_DIV_LONG_check_zero
    cmpl     $-1,%eax
    je       .LOP_DIV_LONG_check_neg1
.LOP_DIV_LONG_notSpecial:
    GET_VREG_WORD(rPC,%ecx,0)
    GET_VREG_WORD(%ecx,%ecx,1)
.LOP_DIV_LONG_notSpecial1:
    movl     %eax,OUT_ARG3(%esp)
    movl     rPC,OUT_ARG0(%esp)
    movl     %ecx,OUT_ARG1(%esp)
    jmp      .LOP_DIV_LONG_continue

/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG: /* 0x9f */
/* File: x86/OP_REM_LONG.S */
/* File: x86/OP_DIV_LONG.S */
    /* div vAA, vBB, vCC */
    movzbl    3(rPC),%eax              # eax<- CC
    movzbl    2(rPC),%ecx              # ecx<- BB
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)
    GET_VREG_WORD(%eax,%eax,1)
    movl     rPC,OUT_ARG2(%esp)
    testl    %eax,%eax
    je       .LOP_REM_LONG_check_zero
    cmpl     $-1,%eax
    je       .LOP_REM_LONG_check_neg1
.LOP_REM_LONG_notSpecial:
    GET_VREG_WORD(rPC,%ecx,0)
    GET_VREG_WORD(%ecx,%ecx,1)
.LOP_REM_LONG_notSpecial1:
    movl     %eax,OUT_ARG3(%esp)
    movl     rPC,OUT_ARG0(%esp)
    movl     %ecx,OUT_ARG1(%esp)
    jmp      .LOP_REM_LONG_continue


/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG: /* 0xa0 */
/* File: x86/OP_AND_LONG.S */
/* File: x86/binopWide.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop vAA, vBB, vCC */

    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)           # rPC<- v[BB+0]
    GET_VREG_WORD(%eax,%eax,1)          # eax<- v[BB+1]
    andl (rFP,%ecx,4),rPC         # ex: addl   (rFP,%ecx,4),rPC
    andl 4(rFP,%ecx,4),%eax         # ex: adcl   4(rFP,%ecx,4),%eax
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    SET_VREG_WORD(rPC,rINST_FULL,0)     # v[AA+0] <- rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[AA+1] <- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG: /* 0xa1 */
/* File: x86/OP_OR_LONG.S */
/* File: x86/binopWide.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop vAA, vBB, vCC */

    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)           # rPC<- v[BB+0]
    GET_VREG_WORD(%eax,%eax,1)          # eax<- v[BB+1]
    orl (rFP,%ecx,4),rPC         # ex: addl   (rFP,%ecx,4),rPC
    orl 4(rFP,%ecx,4),%eax         # ex: adcl   4(rFP,%ecx,4),%eax
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    SET_VREG_WORD(rPC,rINST_FULL,0)     # v[AA+0] <- rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[AA+1] <- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG: /* 0xa2 */
/* File: x86/OP_XOR_LONG.S */
/* File: x86/binopWide.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop vAA, vBB, vCC */

    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)           # rPC<- v[BB+0]
    GET_VREG_WORD(%eax,%eax,1)          # eax<- v[BB+1]
    xorl (rFP,%ecx,4),rPC         # ex: addl   (rFP,%ecx,4),rPC
    xorl 4(rFP,%ecx,4),%eax         # ex: adcl   4(rFP,%ecx,4),%eax
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- AA
    SET_VREG_WORD(rPC,rINST_FULL,0)     # v[AA+0] <- rPC
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[AA+1] <- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG: /* 0xa3 */
/* File: x86/OP_SHL_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.  x86 shifts automatically mask off
     * the low 5 bits of %cl, so have to handle the 64 > shiftcount > 31
     * case specially.
     */
    /* shl-long vAA, vBB, vCC */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)                          # spill edx
    GET_VREG_WORD(%edx,%eax,1)          # ecx<- v[BB+1]
    GET_VREG  (%ecx,%ecx)               # ecx<- vCC
    GET_VREG_WORD(%eax,%eax,0)          # eax<- v[BB+0]
    shldl     %eax,%edx
    sall      %cl,%eax
    testb     $32,%cl
    je        2f
    movl      %eax,%edx
    xorl      %eax,%eax
2:
    movzbl    rINST_HI,%ecx
    SET_VREG_WORD(%edx,%ecx,1)         # v[AA+1]<- %edx
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    jmp       .LOP_SHL_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG: /* 0xa4 */
/* File: x86/OP_SHR_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.  x86 shifts automatically mask off
     * the low 5 bits of %cl, so have to handle the 64 > shiftcount > 31
     * case specially.
     */
    /* shr-long vAA, vBB, vCC */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)                          # spill edx
    GET_VREG_WORD(%edx,%eax,1)          # edx<- v[BB+1]
    GET_VREG  (%ecx,%ecx)               # ecx<- vCC
    GET_VREG_WORD(%eax,%eax,0)          # eax<- v[BB+0]
    shrdl     %edx,%eax
    sarl      %cl,%edx
    testb     $32,%cl
    je        2f
    movl      %edx,%eax
    sarl      $31,%edx
2:
    movzbl    rINST_HI,%ecx
    SET_VREG_WORD(%edx,%ecx,1)         # v[AA+1]<- edx
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    jmp       .LOP_SHR_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG: /* 0xa5 */
/* File: x86/OP_USHR_LONG.S */
    /*
     * Long integer shift.  This is different from the generic 32/64-bit
     * binary operations because vAA/vBB are 64-bit but vCC (the shift
     * distance) is 32-bit.  Also, Dalvik requires us to mask off the low
     * 6 bits of the shift distance.  x86 shifts automatically mask off
     * the low 5 bits of %cl, so have to handle the 64 > shiftcount > 31
     * case specially.
     */
    /* shr-long vAA, vBB, vCC */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    2(rPC),%eax               # eax<- BB
    movzbl    3(rPC),%ecx               # ecx<- CC
    SPILL(rPC)                          # spill edx
    GET_VREG_WORD(%edx,%eax,1)          # edx<- v[BB+1]
    GET_VREG  (%ecx,%ecx)               # ecx<- vCC
    GET_VREG_WORD(%eax,%eax,0)          # eax<- v[BB+0]
    shrdl     %edx,%eax
    shrl      %cl,%edx
    testb     $32,%cl
    je        2f
    movl      %edx,%eax
    xorl      %edx,%edx
2:
    movzbl    rINST_HI,%ecx
    SET_VREG_WORD(%edx,%ecx,1)         # v[BB+1]<- edx
    UNSPILL(rPC)
    jmp       .LOP_USHR_LONG_finish

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT: /* 0xa6 */
/* File: x86/OP_ADD_FLOAT.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    flds    (rFP,%eax,4)           # vCC to fp stack
    fadds   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstps   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT: /* 0xa7 */
/* File: x86/OP_SUB_FLOAT.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    flds    (rFP,%eax,4)           # vCC to fp stack
    fsubs   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstps   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT: /* 0xa8 */
/* File: x86/OP_MUL_FLOAT.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    flds    (rFP,%eax,4)           # vCC to fp stack
    fmuls   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstps   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT: /* 0xa9 */
/* File: x86/OP_DIV_FLOAT.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    flds    (rFP,%eax,4)           # vCC to fp stack
    fdivs   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstps   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT: /* 0xaa */
/* File: x86/OP_REM_FLOAT.S */
    /* rem_float vAA, vBB, vCC */
    movzbl   3(rPC),%ecx            # ecx<- BB
    movzbl   2(rPC),%eax            # eax<- CC
    flds     (rFP,%ecx,4)           # vCC to fp stack
    flds     (rFP,%eax,4)           # vCC to fp stack
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
1:
    fprem
    fstsw     %ax
    sahf
    jp        1b
    fstp      %st(1)
    ADVANCE_PC(2)
    fstps    (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE: /* 0xab */
/* File: x86/OP_ADD_DOUBLE.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    fldl    (rFP,%eax,4)           # vCC to fp stack
    faddl   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstpl   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE: /* 0xac */
/* File: x86/OP_SUB_DOUBLE.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    fldl    (rFP,%eax,4)           # vCC to fp stack
    fsubl   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstpl   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE: /* 0xad */
/* File: x86/OP_MUL_DOUBLE.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    fldl    (rFP,%eax,4)           # vCC to fp stack
    fmull   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstpl   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE: /* 0xae */
/* File: x86/OP_DIV_DOUBLE.S */
/* File: x86/binflop.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */
    /* binop vAA, vBB, vCC */
    movzbl   2(rPC),%eax            # eax<- CC
    movzbl   3(rPC),%ecx            # ecx<- BB
    fldl    (rFP,%eax,4)           # vCC to fp stack
    fdivl   (rFP,%ecx,4)           # ex: faddp
    movzbl   rINST_HI,%ecx         # ecx<- AA
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    fstpl   (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE: /* 0xaf */
/* File: x86/OP_REM_DOUBLE.S */
    /* rem_float vAA, vBB, vCC */
    movzbl   3(rPC),%ecx            # ecx<- BB
    movzbl   2(rPC),%eax            # eax<- CC
    fldl     (rFP,%ecx,4)           # vCC to fp stack
    fldl     (rFP,%eax,4)           # vCC to fp stack
    movzbl   rINST_HI,%ecx          # ecx<- AA
    FETCH_INST_WORD(2)
1:
    fprem
    fstsw     %ax
    sahf
    jp        1b
    fstp      %st(1)
    ADVANCE_PC(2)
    fstpl    (rFP,%ecx,4)           # %st to vAA
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_2ADDR: /* 0xb0 */
/* File: x86/OP_ADD_INT_2ADDR.S */
/* File: x86/binop2addr.S */
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
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    FETCH_INST_WORD(1)
    andb    $0xf,%cl                   # ecx<- A
    addl     %eax,(rFP,%ecx,4)                              # for ex: addl   %eax,(rFP,%ecx,4)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SUB_INT_2ADDR: /* 0xb1 */
/* File: x86/OP_SUB_INT_2ADDR.S */
/* File: x86/binop2addr.S */
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
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    FETCH_INST_WORD(1)
    andb    $0xf,%cl                   # ecx<- A
    subl     %eax,(rFP,%ecx,4)                              # for ex: addl   %eax,(rFP,%ecx,4)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_2ADDR: /* 0xb2 */
/* File: x86/OP_MUL_INT_2ADDR.S */
    /* mul vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    andb    $0xf,%cl                   # ecx<- A
    SPILL(rPC)
    imull   (rFP,%ecx,4),%eax
    UNSPILL(rPC)
    SET_VREG(%eax,%ecx)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_2ADDR: /* 0xb3 */
/* File: x86/OP_DIV_INT_2ADDR.S */
/* File: x86/bindiv2addr.S */
    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* div/rem/2addr vA, vB */
    movzx    rINST_HI,%ecx          # eax<- BA
    sarl     $4,%ecx               # ecx<- B
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    movzbl   rINST_HI,rINST_FULL    # rINST_FULL<- BA
    andb     $0xf,rINST_LO         # rINST_FULL<- A
    GET_VREG(%eax,rINST_FULL)       # eax<- vBB
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_DIV_INT_2ADDR_continue_div2addr
    cmpl     $0x80000000,%eax
    jne      .LOP_DIV_INT_2ADDR_continue_div2addr
    movl     $0x80000000,%eax
    jmp      .LOP_DIV_INT_2ADDR_finish_div2addr



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_2ADDR: /* 0xb4 */
/* File: x86/OP_REM_INT_2ADDR.S */
/* File: x86/bindiv2addr.S */
    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* div/rem/2addr vA, vB */
    movzx    rINST_HI,%ecx          # eax<- BA
    sarl     $4,%ecx               # ecx<- B
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    movzbl   rINST_HI,rINST_FULL    # rINST_FULL<- BA
    andb     $0xf,rINST_LO         # rINST_FULL<- A
    GET_VREG(%eax,rINST_FULL)       # eax<- vBB
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_REM_INT_2ADDR_continue_div2addr
    cmpl     $0x80000000,%eax
    jne      .LOP_REM_INT_2ADDR_continue_div2addr
    movl     $0,%edx
    jmp      .LOP_REM_INT_2ADDR_finish_div2addr



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_2ADDR: /* 0xb5 */
/* File: x86/OP_AND_INT_2ADDR.S */
/* File: x86/binop2addr.S */
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
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    FETCH_INST_WORD(1)
    andb    $0xf,%cl                   # ecx<- A
    andl     %eax,(rFP,%ecx,4)                              # for ex: addl   %eax,(rFP,%ecx,4)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_2ADDR: /* 0xb6 */
/* File: x86/OP_OR_INT_2ADDR.S */
/* File: x86/binop2addr.S */
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
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    FETCH_INST_WORD(1)
    andb    $0xf,%cl                   # ecx<- A
    orl     %eax,(rFP,%ecx,4)                              # for ex: addl   %eax,(rFP,%ecx,4)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_2ADDR: /* 0xb7 */
/* File: x86/OP_XOR_INT_2ADDR.S */
/* File: x86/binop2addr.S */
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
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    GET_VREG(%eax,rINST_FULL)           # eax<- vB
    FETCH_INST_WORD(1)
    andb    $0xf,%cl                   # ecx<- A
    xorl     %eax,(rFP,%ecx,4)                              # for ex: addl   %eax,(rFP,%ecx,4)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_2ADDR: /* 0xb8 */
/* File: x86/OP_SHL_INT_2ADDR.S */
/* File: x86/shop2addr.S */
    /*
     * Generic 32-bit "shift/2addr" operation.
     */
    /* shift/2addr vA, vB */
    movzx    rINST_HI,%ecx          # eax<- BA
    sarl     $4,%ecx               # ecx<- B
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    movzbl   rINST_HI,rINST_FULL    # rINST_FULL<- BA
    andb     $0xf,rINST_LO         # rINST_FULL<- A
    GET_VREG(%eax,rINST_FULL)       # eax<- vAA
    sall    %cl,%eax                          # ex: sarl %cl,%eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_2ADDR: /* 0xb9 */
/* File: x86/OP_SHR_INT_2ADDR.S */
/* File: x86/shop2addr.S */
    /*
     * Generic 32-bit "shift/2addr" operation.
     */
    /* shift/2addr vA, vB */
    movzx    rINST_HI,%ecx          # eax<- BA
    sarl     $4,%ecx               # ecx<- B
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    movzbl   rINST_HI,rINST_FULL    # rINST_FULL<- BA
    andb     $0xf,rINST_LO         # rINST_FULL<- A
    GET_VREG(%eax,rINST_FULL)       # eax<- vAA
    sarl    %cl,%eax                          # ex: sarl %cl,%eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_2ADDR: /* 0xba */
/* File: x86/OP_USHR_INT_2ADDR.S */
/* File: x86/shop2addr.S */
    /*
     * Generic 32-bit "shift/2addr" operation.
     */
    /* shift/2addr vA, vB */
    movzx    rINST_HI,%ecx          # eax<- BA
    sarl     $4,%ecx               # ecx<- B
    GET_VREG(%ecx,%ecx)             # eax<- vBB
    movzbl   rINST_HI,rINST_FULL    # rINST_FULL<- BA
    andb     $0xf,rINST_LO         # rINST_FULL<- A
    GET_VREG(%eax,rINST_FULL)       # eax<- vAA
    shrl    %cl,%eax                          # ex: sarl %cl,%eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_ADD_LONG_2ADDR: /* 0xbb */
/* File: x86/OP_ADD_LONG_2ADDR.S */
/* File: x86/binopWide2addr.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop/2addr vA, vB */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG_WORD(%eax,%ecx,0)          # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)          # eax<- v[B+1]
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- BA
    andb      $0xF,rINST_LO            # rINST_FULL<- A
    addl %eax,(rFP,rINST_FULL,4)         # example: addl   %eax,(rFP,rINST_FULL,4)
    adcl %ecx,4(rFP,rINST_FULL,4)         # example: adcl   %ecx,4(rFP,rINST_FULL,4)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SUB_LONG_2ADDR: /* 0xbc */
/* File: x86/OP_SUB_LONG_2ADDR.S */
/* File: x86/binopWide2addr.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop/2addr vA, vB */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG_WORD(%eax,%ecx,0)          # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)          # eax<- v[B+1]
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- BA
    andb      $0xF,rINST_LO            # rINST_FULL<- A
    subl %eax,(rFP,rINST_FULL,4)         # example: addl   %eax,(rFP,rINST_FULL,4)
    sbbl %ecx,4(rFP,rINST_FULL,4)         # example: adcl   %ecx,4(rFP,rINST_FULL,4)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_LONG_2ADDR: /* 0xbd */
/* File: x86/OP_MUL_LONG_2ADDR.S */
    /*
     * Signed 64-bit integer multiply, 2-addr version
     *
     * We could definately use more free registers for
     * this code.  We must spill rPC (edx) because it
     * is used by imul.  We'll also spill rINST (ebx),
     * giving us eax, ebc, ecx and edx as computational
     * temps.  On top of that, we'll spill rIBASE (edi)
     * for use as the vA pointer and rFP (esi) for use
     * as the vB pointer.  Yuck.
     */
    /* mul-long/2addr vA, vB */
    movzbl    rINST_HI,%eax            # eax<- BA
    andb      $0xf,%al                # eax<- A
    sarl      $12,rINST_FULL          # rINST_FULL<- B
    SPILL(rPC)
    SPILL(rIBASE)
    SPILL(rFP)
    leal      (rFP,%eax,4),rIBASE      # rIBASE<- &v[A]
    leal      (rFP,rINST_FULL,4),rFP   # rFP<- &v[B]
    movl      4(rIBASE),%ecx      # ecx<- Amsw
    imull     (rFP),%ecx          # ecx<- (Amsw*Blsw)
    movl      4(rFP),%eax         # eax<- Bmsw
    imull     (rIBASE),%eax       # eax<- (Bmsw*Alsw)
    addl      %eax,%ecx           # ecx<- (Amsw*Blsw)+(Bmsw*Alsw)
    movl      (rFP),%eax          # eax<- Blsw
    mull      (rIBASE)            # eax<- (Blsw*Alsw)
    jmp       .LOP_MUL_LONG_2ADDR_continue

/* ------------------------------ */
    .balign 64
.L_OP_DIV_LONG_2ADDR: /* 0xbe */
/* File: x86/OP_DIV_LONG_2ADDR.S */
    /* div/2addr vA, vB */
    movzbl    rINST_HI,%eax
    shrl      $4,%eax                  # eax<- B
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)
    GET_VREG_WORD(%eax,%eax,1)
    movl     rPC,OUT_ARG2(%esp)
    testl    %eax,%eax
    je       .LOP_DIV_LONG_2ADDR_check_zero
    cmpl     $-1,%eax
    je       .LOP_DIV_LONG_2ADDR_check_neg1
.LOP_DIV_LONG_2ADDR_notSpecial:
    GET_VREG_WORD(rPC,rINST_FULL,0)
    GET_VREG_WORD(%ecx,rINST_FULL,1)
.LOP_DIV_LONG_2ADDR_notSpecial1:
    jmp      .LOP_DIV_LONG_2ADDR_continue

/* ------------------------------ */
    .balign 64
.L_OP_REM_LONG_2ADDR: /* 0xbf */
/* File: x86/OP_REM_LONG_2ADDR.S */
/* File: x86/OP_DIV_LONG_2ADDR.S */
    /* div/2addr vA, vB */
    movzbl    rINST_HI,%eax
    shrl      $4,%eax                  # eax<- B
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    SPILL(rPC)
    GET_VREG_WORD(rPC,%eax,0)
    GET_VREG_WORD(%eax,%eax,1)
    movl     rPC,OUT_ARG2(%esp)
    testl    %eax,%eax
    je       .LOP_REM_LONG_2ADDR_check_zero
    cmpl     $-1,%eax
    je       .LOP_REM_LONG_2ADDR_check_neg1
.LOP_REM_LONG_2ADDR_notSpecial:
    GET_VREG_WORD(rPC,rINST_FULL,0)
    GET_VREG_WORD(%ecx,rINST_FULL,1)
.LOP_REM_LONG_2ADDR_notSpecial1:
    jmp      .LOP_REM_LONG_2ADDR_continue


/* ------------------------------ */
    .balign 64
.L_OP_AND_LONG_2ADDR: /* 0xc0 */
/* File: x86/OP_AND_LONG_2ADDR.S */
/* File: x86/binopWide2addr.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop/2addr vA, vB */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG_WORD(%eax,%ecx,0)          # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)          # eax<- v[B+1]
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- BA
    andb      $0xF,rINST_LO            # rINST_FULL<- A
    andl %eax,(rFP,rINST_FULL,4)         # example: addl   %eax,(rFP,rINST_FULL,4)
    andl %ecx,4(rFP,rINST_FULL,4)         # example: adcl   %ecx,4(rFP,rINST_FULL,4)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_OR_LONG_2ADDR: /* 0xc1 */
/* File: x86/OP_OR_LONG_2ADDR.S */
/* File: x86/binopWide2addr.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop/2addr vA, vB */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG_WORD(%eax,%ecx,0)          # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)          # eax<- v[B+1]
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- BA
    andb      $0xF,rINST_LO            # rINST_FULL<- A
    orl %eax,(rFP,rINST_FULL,4)         # example: addl   %eax,(rFP,rINST_FULL,4)
    orl %ecx,4(rFP,rINST_FULL,4)         # example: adcl   %ecx,4(rFP,rINST_FULL,4)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_XOR_LONG_2ADDR: /* 0xc2 */
/* File: x86/OP_XOR_LONG_2ADDR.S */
/* File: x86/binopWide2addr.S */
    /*
     * Generic 64-bit binary operation.
     */
    /* binop/2addr vA, vB */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG_WORD(%eax,%ecx,0)          # eax<- v[B+0]
    GET_VREG_WORD(%ecx,%ecx,1)          # eax<- v[B+1]
    movzbl    rINST_HI,rINST_FULL       # rINST_FULL<- BA
    andb      $0xF,rINST_LO            # rINST_FULL<- A
    xorl %eax,(rFP,rINST_FULL,4)         # example: addl   %eax,(rFP,rINST_FULL,4)
    xorl %ecx,4(rFP,rINST_FULL,4)         # example: adcl   %ecx,4(rFP,rINST_FULL,4)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHL_LONG_2ADDR: /* 0xc3 */
/* File: x86/OP_SHL_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* shl-long/2addr vA, vB */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    movzbl    rINST_HI,rINST_FULL       # rINST_HI<- BA
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG_WORD(%eax,rINST_FULL,0)    # eax<- v[AA+0]
    sarl      $4,%ecx                  # ecx<- B
    SPILL(rPC)
    GET_VREG_WORD(%edx,rINST_FULL,1)    # edx<- v[AA+1]
    GET_VREG(%ecx,%ecx)                 # ecx<- vBB
    shldl     %eax,%edx
    sall      %cl,%eax
    testb     $32,%cl
    je        2f
    movl      %eax,%edx
    xorl      %eax,%eax
2:
    SET_VREG_WORD(%edx,rINST_FULL,1)   # v[AA+1]<- edx
    UNSPILL(rPC)
    jmp       .LOP_SHL_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_SHR_LONG_2ADDR: /* 0xc4 */
/* File: x86/OP_SHR_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* shl-long/2addr vA, vB */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    movzbl    rINST_HI,rINST_FULL       # rINST_HI<- BA
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG_WORD(%eax,rINST_FULL,0)    # eax<- v[AA+0]
    sarl      $4,%ecx                  # ecx<- B
    SPILL(rPC)
    GET_VREG_WORD(%edx,rINST_FULL,1)    # edx<- v[AA+1]
    GET_VREG(%ecx,%ecx)                 # ecx<- vBB
    shrdl     %edx,%eax
    sarl      %cl,%edx
    testb     $32,%cl
    je        2f
    movl      %edx,%eax
    sarl      $31,%edx
2:
    SET_VREG_WORD(%edx,rINST_FULL,1)   # v[AA+1]<- edx
    UNSPILL(rPC)
    jmp       .LOP_SHR_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_USHR_LONG_2ADDR: /* 0xc5 */
/* File: x86/OP_USHR_LONG_2ADDR.S */
    /*
     * Long integer shift, 2addr version.  vA is 64-bit value/result, vB is
     * 32-bit shift distance.
     */
    /* shl-long/2addr vA, vB */
    /* ecx gets shift count */
    /* Need to spill edx */
    /* rINST gets AA */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    movzbl    rINST_HI,rINST_FULL       # rINST_HI<- BA
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG_WORD(%eax,rINST_FULL,0)    # eax<- v[AA+0]
    sarl      $4,%ecx                  # ecx<- B
    SPILL(rPC)
    GET_VREG_WORD(%edx,rINST_FULL,1)    # edx<- v[AA+1]
    GET_VREG(%ecx,%ecx)                 # ecx<- vBB
    shrdl     %edx,%eax
    shrl      %cl,%edx
    testb     $32,%cl
    je        2f
    movl      %edx,%eax
    xorl      %edx,%edx
2:
    SET_VREG_WORD(%edx,rINST_FULL,1)   # v[AA+1]<- edx
    UNSPILL(rPC)
    jmp       .LOP_USHR_LONG_2ADDR_finish

/* ------------------------------ */
    .balign 64
.L_OP_ADD_FLOAT_2ADDR: /* 0xc6 */
/* File: x86/OP_ADD_FLOAT_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    flds    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fadds   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstps    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SUB_FLOAT_2ADDR: /* 0xc7 */
/* File: x86/OP_SUB_FLOAT_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    flds    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fsubs   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstps    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_FLOAT_2ADDR: /* 0xc8 */
/* File: x86/OP_MUL_FLOAT_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    flds    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fmuls   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstps    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_DIV_FLOAT_2ADDR: /* 0xc9 */
/* File: x86/OP_DIV_FLOAT_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    flds    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fdivs   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstps    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_REM_FLOAT_2ADDR: /* 0xca */
/* File: x86/OP_REM_FLOAT_2ADDR.S */
    /* rem_float/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    flds     (rFP,rINST_FULL,4)         # vBB to fp stack
    andb    $0xf,%cl                   # ecx<- A
    flds     (rFP,%ecx,4)               # vAA to fp stack
    FETCH_INST_WORD(1)
1:
    fprem
    fstsw     %ax
    sahf
    jp        1b
    fstp      %st(1)
    ADVANCE_PC(1)
    fstps    (rFP,%ecx,4)               # %st to vA
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_ADD_DOUBLE_2ADDR: /* 0xcb */
/* File: x86/OP_ADD_DOUBLE_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    fldl    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    faddl   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstpl    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SUB_DOUBLE_2ADDR: /* 0xcc */
/* File: x86/OP_SUB_DOUBLE_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    fldl    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fsubl   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstpl    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_DOUBLE_2ADDR: /* 0xcd */
/* File: x86/OP_MUL_DOUBLE_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    fldl    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fmull   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstpl    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_DIV_DOUBLE_2ADDR: /* 0xce */
/* File: x86/OP_DIV_DOUBLE_2ADDR.S */
/* File: x86/binflop2addr.S */
    /*
     * Generic 32-bit binary float operation.
     *
     * For: add-fp, sub-fp, mul-fp, div-fp
     */

    /* binop/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    andb    $0xf,%cl                   # ecx<- A
    fldl    (rFP,%ecx,4)               # vAA to fp stack
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fdivl   (rFP,rINST_FULL,4)           # ex: faddp
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    fstpl    (rFP,%ecx,4)              # %st to vA
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_REM_DOUBLE_2ADDR: /* 0xcf */
/* File: x86/OP_REM_DOUBLE_2ADDR.S */
    /* rem_float/2addr vA, vB */
    movzx   rINST_HI,%ecx               # ecx<- A+
    sarl    $12,rINST_FULL             # rINST_FULL<- B
    fldl     (rFP,rINST_FULL,4)         # vBB to fp stack
    andb    $0xf,%cl                   # ecx<- A
    fldl     (rFP,%ecx,4)               # vAA to fp stack
    FETCH_INST_WORD(1)
1:
    fprem
    fstsw     %ax
    sahf
    jp        1b
    fstp      %st(1)
    ADVANCE_PC(1)
    fstpl    (rFP,%ecx,4)               # %st to vA
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT16: /* 0xd0 */
/* File: x86/OP_ADD_INT_LIT16.S */
/* File: x86/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int/lit16, rsub-int,
     *      and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    addl %ecx,%eax                              # for example: addl %ecx, %eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT: /* 0xd1 */
/* File: x86/OP_RSUB_INT.S */
/* File: x86/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int/lit16, rsub-int,
     *      and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    subl %eax,%ecx                              # for example: addl %ecx, %eax
    SET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT16: /* 0xd2 */
/* File: x86/OP_MUL_INT_LIT16.S */
    /* mul/lit16 vA, vB, #+CCCC */
    /* Need A in rINST_FULL, ssssCCCC in ecx, vB in eax */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    SPILL(rPC)
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    imull     %ecx,%eax                 # trashes rPC
    UNSPILL(rPC)
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT16: /* 0xd3 */
/* File: x86/OP_DIV_INT_LIT16.S */
/* File: x86/bindivLit16.S */
    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* div/rem/lit16 vA, vB, #+CCCC */
    /* Need A in rINST_FULL, ssssCCCC in ecx, vB in eax */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_DIV_INT_LIT16_continue_div
    cmpl     $0x80000000,%eax
    jne      .LOP_DIV_INT_LIT16_continue_div
    movl     $0x80000000,%eax
    jmp      .LOP_DIV_INT_LIT16_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT16: /* 0xd4 */
/* File: x86/OP_REM_INT_LIT16.S */
/* File: x86/bindivLit16.S */
    /*
     * 32-bit binary div/rem operation.  Handles special case of op0=minint and
     * op1=-1.
     */
    /* div/rem/lit16 vA, vB, #+CCCC */
    /* Need A in rINST_FULL, ssssCCCC in ecx, vB in eax */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $-1,%ecx
    jne      .LOP_REM_INT_LIT16_continue_div
    cmpl     $0x80000000,%eax
    jne      .LOP_REM_INT_LIT16_continue_div
    movl     $0,%edx
    jmp      .LOP_REM_INT_LIT16_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT16: /* 0xd5 */
/* File: x86/OP_AND_INT_LIT16.S */
/* File: x86/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int/lit16, rsub-int,
     *      and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    andl %ecx,%eax                              # for example: addl %ecx, %eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT16: /* 0xd6 */
/* File: x86/OP_OR_INT_LIT16.S */
/* File: x86/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int/lit16, rsub-int,
     *      and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    orl     %ecx,%eax                              # for example: addl %ecx, %eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT16: /* 0xd7 */
/* File: x86/OP_XOR_INT_LIT16.S */
/* File: x86/binopLit16.S */
    /*
     * Generic 32-bit "lit16" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than eax, you can override "result".)
     *
     * For: add-int/lit16, rsub-int,
     *      and-int/lit16, or-int/lit16, xor-int/lit16
     */
    /* binop/lit16 vA, vB, #+CCCC */
    movzbl   rINST_HI,%eax              # eax<- 000000BA
    sarl     $4,%eax                   # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB
    movswl   2(rPC),%ecx                # ecx<- ssssCCCC
    movzbl   rINST_HI,rINST_FULL        # rINST_FULL<- BA
    andb     $0xf,rINST_LO             # rINST_FULL<- A
    xor    %ecx,%eax                              # for example: addl %ecx, %eax
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_ADD_INT_LIT8: /* 0xd8 */
/* File: x86/OP_ADD_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    addl %ecx,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_RSUB_INT_LIT8: /* 0xd9 */
/* File: x86/OP_RSUB_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    subl  %eax,%ecx                             # ex: addl %ecx,%eax
    SET_VREG  (%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_MUL_INT_LIT8: /* 0xda */
/* File: x86/OP_MUL_INT_LIT8.S */
    /* mul/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    SPILL(rPC)
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    imull     %ecx,%eax                # trashes rPC
    UNSPILL(rPC)
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_DIV_INT_LIT8: /* 0xdb */
/* File: x86/OP_DIV_INT_LIT8.S */
/* File: x86/bindivLit8.S */
    /*
     * 32-bit div/rem "lit8" binary operation.  Handles special case of
     * op0=minint & op1=-1
     */
    /* div/rem/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    GET_VREG  (%eax,%eax)              # eax<- rBB
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $0x80000000,%eax
    jne      .LOP_DIV_INT_LIT8_continue_div
    cmpl     $-1,%ecx
    jne      .LOP_DIV_INT_LIT8_continue_div
    movl     $0x80000000,%eax
    jmp      .LOP_DIV_INT_LIT8_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_REM_INT_LIT8: /* 0xdc */
/* File: x86/OP_REM_INT_LIT8.S */
/* File: x86/bindivLit8.S */
    /*
     * 32-bit div/rem "lit8" binary operation.  Handles special case of
     * op0=minint & op1=-1
     */
    /* div/rem/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    GET_VREG  (%eax,%eax)              # eax<- rBB
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    SPILL(rPC)
    cmpl     $0,%ecx
    je       common_errDivideByZero
    cmpl     $0x80000000,%eax
    jne      .LOP_REM_INT_LIT8_continue_div
    cmpl     $-1,%ecx
    jne      .LOP_REM_INT_LIT8_continue_div
    movl     $0,%edx
    jmp      .LOP_REM_INT_LIT8_finish_div



/* ------------------------------ */
    .balign 64
.L_OP_AND_INT_LIT8: /* 0xdd */
/* File: x86/OP_AND_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    andl %ecx,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_OR_INT_LIT8: /* 0xde */
/* File: x86/OP_OR_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    orl     %ecx,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT



/* ------------------------------ */
    .balign 64
.L_OP_XOR_INT_LIT8: /* 0xdf */
/* File: x86/OP_XOR_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    xor    %ecx,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHL_INT_LIT8: /* 0xe0 */
/* File: x86/OP_SHL_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    sall  %cl,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_SHR_INT_LIT8: /* 0xe1 */
/* File: x86/OP_SHR_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    sarl    %cl,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_USHR_INT_LIT8: /* 0xe2 */
/* File: x86/OP_USHR_INT_LIT8.S */
/* File: x86/binopLit8.S */
    /*
     * Generic 32-bit "lit8" binary operation.  Provide an "instr" line
     * that specifies an instruction that performs "result = eax op ecx".
     * This could be an x86 instruction or a function call.  (If the result
     * comes back in a register other than r0, you can override "result".)
     *
     * For: add-int/lit8, rsub-int/lit8
     *      and-int/lit8, or-int/lit8, xor-int/lit8,
     *      shl-int/lit8, shr-int/lit8, ushr-int/lit8
     */
    /* binop/lit8 vAA, vBB, #+CC */
    movzbl    2(rPC),%eax              # eax<- BB
    movsbl    3(rPC),%ecx              # ecx<- ssssssCC
    movzx     rINST_HI,rINST_FULL      # rINST_FULL<- AA
    GET_VREG  (%eax,%eax)              # eax<- rBB
    shrl     %cl,%eax                             # ex: addl %ecx,%eax
    SET_VREG  (%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E3: /* 0xe3 */
/* File: x86/OP_UNUSED_E3.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E4: /* 0xe4 */
/* File: x86/OP_UNUSED_E4.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E5: /* 0xe5 */
/* File: x86/OP_UNUSED_E5.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E6: /* 0xe6 */
/* File: x86/OP_UNUSED_E6.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E7: /* 0xe7 */
/* File: x86/OP_UNUSED_E7.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E8: /* 0xe8 */
/* File: x86/OP_UNUSED_E8.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_E9: /* 0xe9 */
/* File: x86/OP_UNUSED_E9.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EA: /* 0xea */
/* File: x86/OP_UNUSED_EA.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EB: /* 0xeb */
/* File: x86/OP_UNUSED_EB.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EC: /* 0xec */
/* File: x86/OP_UNUSED_EC.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_ED: /* 0xed */
/* File: x86/OP_UNUSED_ED.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_EXECUTE_INLINE: /* 0xee */
/* File: x86/OP_EXECUTE_INLINE.S */
    /*
     * Execute a "native inline" instruction.
     *
     * We will be calling through a function table:
     *
     * (*gDvmInlineOpsTable[opIndex].func)(arg0, arg1, arg2, arg3, pResult)
     *
     */
    /* [opt] execute-inline vAA, {vC, vD, vE, vF}, inline@BBBB */
    GET_GLUE(%ecx)
    EXPORT_PC()
    movzwl    2(rPC),%eax               # eax<- BBBB
    leal      offGlue_retval(%ecx),%ecx # ecx<- & glue->retval
    movl      %ecx,OUT_ARG4(%esp)
    sarl      $12,rINST_FULL           # rINST_FULL<- arg count (0-4)
    SPILL(rPC)
    call      .LOP_EXECUTE_INLINE_continue      # make call; will return after
    UNSPILL(rPC)
    testl     %eax,%eax                 # successful?
    FETCH_INST_WORD(3)
    je        common_exceptionThrown    # no, handle exception
    ADVANCE_PC(3)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_EF: /* 0xef */
/* File: x86/OP_UNUSED_EF.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_DIRECT_EMPTY: /* 0xf0 */
/* File: x86/OP_INVOKE_DIRECT_EMPTY.S */
    /*
     * invoke-direct-empty is a no-op in a "standard" interpreter.
     */
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_F1: /* 0xf1 */
/* File: x86/OP_UNUSED_F1.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_IGET_QUICK: /* 0xf2 */
/* File: x86/OP_IGET_QUICK.S */
    /* For: iget-quick, iget-object-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzwl    2(rPC),%eax               # eax<- field byte offset
    cmpl      $0,%ecx                  # is object null?
    je        common_errNullObject
    movl      (%ecx,%eax,1),%eax
    movzbl    rINST_HI,%ecx
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    andb      $0xf,%cl                 # rINST_FULL<- A
    SET_VREG  (%eax,%ecx)               # fp[A]<- result
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_IGET_WIDE_QUICK: /* 0xf3 */
/* File: x86/OP_IGET_WIDE_QUICK.S */
    /* For: iget-wide-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzwl    2(rPC),%eax               # eax<- field byte offset
    cmpl      $0,%ecx                  # is object null?
    je        common_errNullObject
    leal      (%ecx,%eax,1),%eax        # eax<- address of 64-bit source
    movl      (%eax),%ecx               # ecx<- lsw
    movl      4(%eax),%eax               # eax<- msw
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    SET_VREG_WORD(%ecx,rINST_FULL,0)    # v[A+0]<- lsw
    SET_VREG_WORD(%eax,rINST_FULL,1)    # v[A+1]<- msw
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_IGET_OBJECT_QUICK: /* 0xf4 */
/* File: x86/OP_IGET_OBJECT_QUICK.S */
/* File: x86/OP_IGET_QUICK.S */
    /* For: iget-quick, iget-object-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzwl    2(rPC),%eax               # eax<- field byte offset
    cmpl      $0,%ecx                  # is object null?
    je        common_errNullObject
    movl      (%ecx,%eax,1),%eax
    movzbl    rINST_HI,%ecx
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    andb      $0xf,%cl                 # rINST_FULL<- A
    SET_VREG  (%eax,%ecx)               # fp[A]<- result
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_IPUT_QUICK: /* 0xf5 */
/* File: x86/OP_IPUT_QUICK.S */
    /* For: iput-quick, iput-object-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- v[A]
    movzwl    2(rPC),%eax               # eax<- field byte offset
    testl     %ecx,%ecx                  # is object null?
    je        common_errNullObject
    movl      rINST_FULL,(%ecx,%eax,1)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_WIDE_QUICK: /* 0xf6 */
/* File: x86/OP_IPUT_WIDE_QUICK.S */
    /* For: iput-wide-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzwl    2(rPC),%eax               # eax<- field byte offset
    testl      %ecx,%ecx                # is object null?
    je        common_errNullObject
    leal      (%ecx,%eax,1),%ecx        # ecx<- Address of 64-bit target
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG_WORD(%eax,rINST_FULL,0)    # eax<- lsw
    GET_VREG_WORD(rINST_FULL,rINST_FULL,1) # rINST_FULL<- msw
    movl      %eax,(%ecx)
    movl      rINST_FULL,4(%ecx)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* ------------------------------ */
    .balign 64
.L_OP_IPUT_OBJECT_QUICK: /* 0xf7 */
/* File: x86/OP_IPUT_OBJECT_QUICK.S */
/* File: x86/OP_IPUT_QUICK.S */
    /* For: iput-quick, iput-object-quick */
    /* op vA, vB, offset@CCCC */
    movzbl    rINST_HI,%ecx             # ecx<- BA
    sarl      $4,%ecx                  # ecx<- B
    GET_VREG(%ecx,%ecx)                 # vB (object we're operating on)
    movzbl    rINST_HI,rINST_FULL
    andb      $0xf,rINST_LO            # rINST_FULL<- A
    GET_VREG(rINST_FULL,rINST_FULL)     # rINST_FULL<- v[A]
    movzwl    2(rPC),%eax               # eax<- field byte offset
    testl     %ecx,%ecx                  # is object null?
    je        common_errNullObject
    movl      rINST_FULL,(%ecx,%eax,1)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK: /* 0xf8 */
/* File: x86/OP_INVOKE_VIRTUAL_QUICK.S */
    /*
     * Handle an optimized virtual method call.
     *
     * for: [opt] invoke-virtual-quick, invoke-virtual-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    movzwl    4(rPC),%eax               # eax<- FEDC or CCCC
    movzwl    2(rPC),%ecx               # ecx<- BBBB
    .if     (!0)
    andl      $0xf,%eax                # eax<- C (or stays CCCC)
    .endif
    GET_VREG(%eax,%eax)                 # eax<- vC ("this" ptr)
    testl     %eax,%eax                 # null?
    je        common_errNullObject      # yep, throw exception
    movl      offObject_clazz(%eax),%eax # eax<- thisPtr->clazz
    movl      offClassObject_vtable(%eax),%eax # eax<- thisPtr->clazz->vtable
    EXPORT_PC()                         # might throw later - get ready
    movl      (%eax,%ecx,4),%eax        # eax<- vtable[BBBB]
    jmp       common_invokeMethodNoRange



/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_VIRTUAL_QUICK_RANGE: /* 0xf9 */
/* File: x86/OP_INVOKE_VIRTUAL_QUICK_RANGE.S */
/* File: x86/OP_INVOKE_VIRTUAL_QUICK.S */
    /*
     * Handle an optimized virtual method call.
     *
     * for: [opt] invoke-virtual-quick, invoke-virtual-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    movzwl    4(rPC),%eax               # eax<- FEDC or CCCC
    movzwl    2(rPC),%ecx               # ecx<- BBBB
    .if     (!1)
    andl      $0xf,%eax                # eax<- C (or stays CCCC)
    .endif
    GET_VREG(%eax,%eax)                 # eax<- vC ("this" ptr)
    testl     %eax,%eax                 # null?
    je        common_errNullObject      # yep, throw exception
    movl      offObject_clazz(%eax),%eax # eax<- thisPtr->clazz
    movl      offClassObject_vtable(%eax),%eax # eax<- thisPtr->clazz->vtable
    EXPORT_PC()                         # might throw later - get ready
    movl      (%eax,%ecx,4),%eax        # eax<- vtable[BBBB]
    jmp       common_invokeMethodRange




/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK: /* 0xfa */
/* File: x86/OP_INVOKE_SUPER_QUICK.S */
    /*
     * Handle an optimized "super" method call.
     *
     * for: [opt] invoke-super-quick, invoke-super-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%ecx)
    movzwl    4(rPC),%eax               # eax<- GFED or CCCC
    movl      offGlue_method(%ecx),%ecx # ecx<- current method
    .if       (!0)
    andl      $0xf,%eax                # eax<- D (or stays CCCC)
    .endif
    movl      offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
    GET_VREG(%eax,%eax)                 # eax<- "this"
    movl      offClassObject_super(%ecx),%ecx # ecx<- method->clazz->super
    testl     %eax,%eax                 # null "this"?
    je        common_errNullObject      # "this" is null, throw exception
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offClassObject_vtable(%ecx),%ecx # ecx<- vtable
    EXPORT_PC()
    movl      (%ecx,%eax,4),%eax        # eax<- super->vtable[BBBB]
    jmp       common_invokeMethodNoRange


/* ------------------------------ */
    .balign 64
.L_OP_INVOKE_SUPER_QUICK_RANGE: /* 0xfb */
/* File: x86/OP_INVOKE_SUPER_QUICK_RANGE.S */
/* File: x86/OP_INVOKE_SUPER_QUICK.S */
    /*
     * Handle an optimized "super" method call.
     *
     * for: [opt] invoke-super-quick, invoke-super-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    GET_GLUE(%ecx)
    movzwl    4(rPC),%eax               # eax<- GFED or CCCC
    movl      offGlue_method(%ecx),%ecx # ecx<- current method
    .if       (!1)
    andl      $0xf,%eax                # eax<- D (or stays CCCC)
    .endif
    movl      offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
    GET_VREG(%eax,%eax)                 # eax<- "this"
    movl      offClassObject_super(%ecx),%ecx # ecx<- method->clazz->super
    testl     %eax,%eax                 # null "this"?
    je        common_errNullObject      # "this" is null, throw exception
    movzwl    2(rPC),%eax               # eax<- BBBB
    movl      offClassObject_vtable(%ecx),%ecx # ecx<- vtable
    EXPORT_PC()
    movl      (%ecx,%eax,4),%eax        # eax<- super->vtable[BBBB]
    jmp       common_invokeMethodRange



/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FC: /* 0xfc */
/* File: x86/OP_UNUSED_FC.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FD: /* 0xfd */
/* File: x86/OP_UNUSED_FD.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FE: /* 0xfe */
/* File: x86/OP_UNUSED_FE.S */
/* File: x86/unused.S */
    jmp     common_abort


/* ------------------------------ */
    .balign 64
.L_OP_UNUSED_FF: /* 0xff */
/* File: x86/OP_UNUSED_FF.S */
/* File: x86/unused.S */
    jmp     common_abort



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

/* This is the less common path, so we'll redo some work
   here rather than force spills on the common path */
.LOP_CONST_STRING_resolve:
    GET_GLUE(%eax)
    movl     %ecx,rINST_FULL           # rINST_FULL<- AA
    EXPORT_PC()
    movl     offGlue_method(%eax),%eax # eax<- glue->method
    movzwl   2(rPC),%ecx               # ecx<- BBBB
    movl     offMethod_clazz(%eax),%eax
    SPILL(rPC)
    movl     %ecx,OUT_ARG1(%esp)
    movl     %eax,OUT_ARG0(%esp)
    call     dvmResolveString          # go resolve
    UNSPILL(rPC)
    testl    %eax,%eax                 # failed?
    je       common_exceptionThrown
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_CONST_STRING_JUMBO */

/* This is the less common path, so we'll redo some work
   here rather than force spills on the common path */
.LOP_CONST_STRING_JUMBO_resolve:
    GET_GLUE(%eax)
    movl     %ecx,rINST_FULL           # rINST_FULL<- AA
    EXPORT_PC()
    movl     offGlue_method(%eax),%eax # eax<- glue->method
    movl     2(rPC),%ecx               # ecx<- BBBBBBBB
    movl     offMethod_clazz(%eax),%eax
    SPILL(rPC)
    movl     %ecx,OUT_ARG1(%esp)
    movl     %eax,OUT_ARG0(%esp)
    call     dvmResolveString          # go resolve
    UNSPILL(rPC)
    testl    %eax,%eax                 # failed?
    je       common_exceptionThrown
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    GOTO_NEXT


/* continuation for OP_CONST_CLASS */

/* This is the less common path, so we'll redo some work
   here rather than force spills on the common path */
.LOP_CONST_CLASS_resolve:
    GET_GLUE(%eax)
    movl     %ecx,rINST_FULL           # rINST_FULL<- AA
    EXPORT_PC()
    movl     offGlue_method(%eax),%eax # eax<- glue->method
    movl     $1,OUT_ARG2(%esp)        # true
    movzwl   2(rPC),%ecx               # ecx<- BBBB
    movl     offMethod_clazz(%eax),%eax
    SPILL(rPC)
    movl     %ecx,OUT_ARG1(%esp)
    movl     %eax,OUT_ARG0(%esp)
    call     dvmResolveClass           # go resolve
    UNSPILL(rPC)
    testl    %eax,%eax                 # failed?
    je       common_exceptionThrown
    SET_VREG(%eax,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_MONITOR_ENTER */

.LOP_MONITOR_ENTER_continue:
    SPILL(rPC)                          # have to - caller save
    movl    %ecx,OUT_ARG0(%esp)
    movl    %eax,OUT_ARG1(%esp)
    call    dvmLockObject               # dvmLockObject(self,object)
    UNSPILL(rPC)
#ifdef WITH_DEADLOCK_PREDICTION
    GET_GLUE(%ecx)
    movl    offGlueSelf(%ecx),%ecx      # ecx<- glue->self
    movl    offThread_exception(%ecx),%eax
    testl   %eax,%eax
    jne     common_exceptionThrown
#endif
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_MONITOR_EXIT */

.LOP_MONITOR_EXIT_continue:
    call    dvmUnlockObject             # unlock(self,obj)
    UNSPILL(rPC)
    FETCH_INST_WORD(1)
    testl   %eax,%eax                   # success?
    je      common_exceptionThrown      # no, exception pending
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_CHECK_CAST */

    /*
     * Trivial test failed, need to perform full check.  This is common.
     *  ecx holds obj->clazz
     *  eax holds class resolved from BBBB
     *  rINST_FULL holds object
     */
.LOP_CHECK_CAST_fullcheck:
    movl    %eax,OUT_ARG1(%esp)
    movl    %ecx,OUT_ARG0(%esp)
    SPILL(rPC)
    call    dvmInstanceofNonTrivial     # eax<- boolean result
    UNSPILL(rPC)
    testl   %eax,%eax                   # failed?
    jne     .LOP_CHECK_CAST_okay            # no, success

    # A cast has failed.  We need to throw a ClassCastException with the
    # class of the object that failed to be cast.
    EXPORT_PC()
    movl    offObject_clazz(rINST_FULL),%ecx  # ecx<- obj->clazz
    movl    $.LstrClassCastException,%eax
    movl    offClassObject_descriptor(%ecx),%ecx
    movl    %eax,OUT_ARG0(%esp)     # arg0<- message
    movl    %ecx,OUT_ARG1(%esp)     # arg1<- obj->clazz->descriptor
    SPILL(rPC)
    call    dvmThrowExceptionWithClassMessage
    UNSPILL(rPC)
    jmp     common_exceptionThrown

    /*
     * Resolution required.  This is the least-likely path, and we're
     * going to have to recreate some data.
     *
     *  rINST_FULL holds object
     */
.LOP_CHECK_CAST_resolve:
    GET_GLUE(%ecx)
    EXPORT_PC()
    movzwl  2(rPC),%eax                # eax<- BBBB
    movl    offGlue_method(%ecx),%ecx  # ecx<- glue->method
    movl    %eax,OUT_ARG1(%esp)        # arg1<- BBBB
    movl    offMethod_clazz(%ecx),%ecx # ecx<- metho->clazz
    movl    $0,OUT_ARG2(%esp)         # arg2<- false
    movl    %ecx,OUT_ARG0(%esp)        # arg0<- method->clazz
    SPILL(rPC)
    call    dvmResolveClass            # eax<- resolved ClassObject ptr
    UNSPILL(rPC)
    testl   %eax,%eax                  # got null?
    je      common_exceptionThrown     # yes, handle exception
    movl    offObject_clazz(rINST_FULL),%ecx  # ecx<- obj->clazz
    jmp     .LOP_CHECK_CAST_resolved       # pick up where we left off

/* continuation for OP_INSTANCE_OF */

    /*
     * Trivial test failed, need to perform full check.  This is common.
     *  eax holds obj->clazz
     *  ecx holds class resolved from BBBB
     *  rINST_HI has BA
     *  rPC already spilled
     */
.LOP_INSTANCE_OF_fullcheck:
    movl    %eax,OUT_ARG0(%esp)
    movl    %ecx,OUT_ARG1(%esp)
    call    dvmInstanceofNonTrivial     # eax<- boolean result
    # fall through to OP_INSTANCE_OF_store

    /*
     * eax holds boolean result
     * rINST_HI holds BA
     */
.LOP_INSTANCE_OF_store:
    UNSPILL(rPC)
    movzbl  rINST_HI,%ecx               # ecx<- BA
    FETCH_INST_WORD(2)
    andb    $0xf,%cl                   # ecl<- A
    ADVANCE_PC(2)
    SET_VREG(%eax,%ecx)                 # vA<- eax
    GOTO_NEXT

    /*
     * Trivial test succeeded, save and bail.
     *  r9 holds A
     */
.LOP_INSTANCE_OF_trivial:
    UNSPILL(rPC)
    movzbl  rINST_HI,%ecx               # ecx<- BA
    FETCH_INST_WORD(2)
    andb    $0xf,%cl                   # ecl<- A
    ADVANCE_PC(2)
    movl    $1,%eax
    SET_VREG(%eax,%ecx)                  # vA<- true
    GOTO_NEXT

    /*
     * Resolution required.  This is the least-likely path.
     *
     *  rPC holds BBBB
     *  rINST_HI holds BA
     */
.LOP_INSTANCE_OF_resolve:
    movl    rPC,OUT_ARG1(%esp)          # arg1<- BBBB
    GET_GLUE(%ecx)
    UNSPILL(rPC)
    movl    offGlue_method(%ecx),%ecx
    movl    $1,OUT_ARG2(%esp)          # arg2<- true
    movl    offMethod_clazz(%ecx),%ecx  # ecx<- method->clazz
    EXPORT_PC()
    movl    %ecx,OUT_ARG0(%esp)         # arg0<- method->clazz
    call    dvmResolveClass             # eax<- resolved ClassObject ptr
    UNSPILL(rPC)
    testl   %eax,%eax                   # success?
    je      common_exceptionThrown      # no, handle exception
/* Now, we need to sync up with fast path.  We need eax to
 * hold the obj->clazz, and ecx to hold the resolved class
 */
    movl    %eax,%ecx                   # ecx<- resolved class
    movzbl  rINST_HI,%eax               # eax<- BA
    sarl    $4,%eax                    # eax<- B
    GET_VREG(%eax,%eax)                 # eax<- vB (obj)
    movl    offObject_clazz(%eax),%eax  # eax<- obj->clazz
    jmp     .LOP_INSTANCE_OF_resolved


/* continuation for OP_NEW_INSTANCE */

.LOP_NEW_INSTANCE_initialized:  # on entry, ecx<- class
    testl     $(ACC_INTERFACE|ACC_ABSTRACT),offClassObject_accessFlags(%ecx)
    movl      $ALLOC_DONT_TRACK,OUT_ARG1(%esp)
    jne       .LOP_NEW_INSTANCE_abstract
.LOP_NEW_INSTANCE_finish: # ecx=class
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmAllocObject             # eax<- new object
    UNSPILL(rPC)
    movl     rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    testl    %eax,%eax                  # success?
    je       common_exceptionThrown     # no, bail out
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT

    /*
     * Class initialization required.
     *
     *  ecx holds class object
     */
.LOP_NEW_INSTANCE_needinit:
    SPILL_TMP(%ecx)                     # save object
    movl    %ecx,OUT_ARG0(%esp)
    call    dvmInitClass                # initialize class
    UNSPILL_TMP(%ecx)                   # restore object
    testl   %eax,%eax                   # success?
    jne     .LOP_NEW_INSTANCE_initialized     # success, continue
    UNSPILL(rPC)                        # failed, restore PC
    jmp     common_exceptionThrown      # go deal with init exception

    /*
     * Resolution required.  This is the least-likely path.
     *
     */
.LOP_NEW_INSTANCE_resolve:
    GET_GLUE(%ecx)
    movzwl  2(rPC),%eax
    movl    offGlue_method(%ecx),%ecx   # ecx<- glue->method
    movl    %eax,OUT_ARG1(%esp)
    movl    offMethod_clazz(%ecx),%ecx  # ecx<- method->clazz
    movl    $0,OUT_ARG2(%esp)
    movl    %ecx,OUT_ARG0(%esp)
    call    dvmResolveClass             # call(clazz,off,flags)
    movl    %eax,%ecx                   # ecx<- resolved ClassObject ptr
    testl   %ecx,%ecx                   # success?
    jne     .LOP_NEW_INSTANCE_resolved        # good to go
    UNSPILL(rPC)
    jmp     common_exceptionThrown      # no, handle exception

    /*
     * We can't instantiate an abstract class or interface, so throw an
     * InstantiationError with the class descriptor as the message.
     *
     *  ecx holds class object
     */
.LOP_NEW_INSTANCE_abstract:
    movl    offClassObject_descriptor(%ecx),%eax
    movl    $.LstrInstantiationError,OUT_ARG0(%esp)
    movl    %eax,OUT_ARG1(%esp)
    call    dvmThrowExceptionWithClassMessage
    UNSPILL(rPC)
    jmp     common_exceptionThrown




/* continuation for OP_NEW_ARRAY */

    /*
     * Resolve class.  (This is an uncommon case.)
     *  ecx holds class (null here)
     *  eax holds array length (vB)
     */
.LOP_NEW_ARRAY_resolve:
    GET_GLUE(%ecx)
    SPILL_TMP(%eax)                    # save array length
    movl    offGlue_method(%ecx),%ecx  # ecx<- glue->method
    movzwl  2(rPC),%eax                # eax<- CCCC
    movl    offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
    movl    %eax,OUT_ARG1(%esp)
    movl    $0,OUT_ARG2(%esp)
    movl    %ecx,OUT_ARG0(%esp)
    SPILL(rPC)
    call    dvmResolveClass            # eax<- call(clazz,ref,flag)
    UNSPILL(rPC)
    movl    %eax,%ecx
    UNSPILL_TMP(%eax)
    testl   %ecx,%ecx                  # successful resolution?
    je      common_exceptionThrown     # no, bail.
# fall through to OP_NEW_ARRAY_finish

    /*
     * Finish allocation
     *
     * ecx holds class
     * eax holds array length (vB)
     */
.LOP_NEW_ARRAY_finish:
    movl    %ecx,OUT_ARG0(%esp)
    movl    %eax,OUT_ARG1(%esp)
    movl    $ALLOC_DONT_TRACK,OUT_ARG2(%esp)
    SPILL(rPC)
    call    dvmAllocArrayByClass    # eax<- call(clazz,length,flags)
    UNSPILL(rPC)
    testl   %eax,%eax               # failed?
    je      common_exceptionThrown  # yup - go handle
    movl    rINST_FULL,%ecx
    FETCH_INST_WORD(2)
    SET_VREG(%eax,%ecx)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_FILLED_NEW_ARRAY */

.LOP_FILLED_NEW_ARRAY_more:
    movl    offMethod_clazz(%eax),%eax        # eax<- method->clazz
    movl    %eax,OUT_ARG0(%esp)               # arg0<- clazz
    call    dvmResolveClass                   # eax<- call(clazz,ref,flag)
    UNSPILL(rPC)
    testl   %eax,%eax                         # null?
    je      common_exceptionThrown            # yes, handle it

       # note: fall through to .LOP_FILLED_NEW_ARRAY_continue

    /*
     * On entry:
     *    eax holds array class [r0]
     *    rINST_FULL holds AA or BB [r10]
     *    ecx is scratch
     *    rPC is valid, but has been spilled
     */
.LOP_FILLED_NEW_ARRAY_continue:
    movl    offClassObject_descriptor(%eax),%ecx  # ecx<- arrayClass->descriptor
    movl    $ALLOC_DONT_TRACK,OUT_ARG2(%esp)     # arg2<- flags
    movzbl  1(%ecx),%ecx                          # ecx<- descriptor[1]
    movl    %ecx,OUT_ARG0(%esp)                   # arg0<- typeCh
    cmpb    $'I',%cl                             # supported?
    jne      .LOP_FILLED_NEW_ARRAY_notimpl                  # no, not handled yet
    .if      (!0)
    SPILL_TMP(rINST_FULL)                         # save copy, need "B" later
    sarl    $4,rINST_FULL
    .endif
    movl    rINST_FULL,OUT_ARG1(%esp)             # arg1<- A or AA (length)
    call    dvmAllocPrimitiveArray      # eax<- call(typeCh, length, flags)
    UNSPILL(rPC)
    GET_GLUE(%ecx)
    testl   %eax,%eax                             # alloc successful?
    je      common_exceptionThrown                # no, handle exception
    movl    %eax,offGlue_retval(%ecx)             # retval.l<- new array
    movzwl  4(rPC),%ecx                           # ecx<- FEDC or CCCC
    leal    offArrayObject_contents(%eax),%eax    # eax<- newArray->contents

/* at this point:
 *     eax is pointer to tgt
 *     rINST_FULL is length
 *     ecx is FEDC or CCCC
 *     TMP_SPILL is BA
 *     rPC is valid, but spilled
 *  We now need to copy values from registers into the array
 */

    .if 0
    # set up src pointer
    SPILL(rFP)     # esi
    SPILL(rIBASE)   # edi
    movl    %eax,%edi         # set up dst ptr
    leal    (rFP,%ecx,4),%esi # set up src ptr
    movl    rINST_FULL,%ecx   # load count register
    FETCH_INST_WORD(3)
    rep
    movsd
    UNSPILL(rIBASE)
    UNSPILL(rFP)
    .else
    testl  rINST_FULL,rINST_FULL
    je     4f
    UNSPILL_TMP(rPC)
    andl   $0x0f,rPC            # rPC<- 0000000A
    sall   $16,rPC              # rPC<- 000A0000
    orl    %ecx,rPC              # rpc<- 000AFEDC
3:
    movl   $0xf,%ecx
    andl   rPC,%ecx           # ecx<- next reg to load
    GET_VREG(%ecx,%ecx)
    shrl   $4,rPC
    leal   4(%eax),%eax
    movl   %ecx,-4(%eax)
    sub    $1,rINST_FULL
    jne    3b
4:
    UNSPILL(rPC)
    FETCH_INST_WORD(3)
    .endif

    ADVANCE_PC(3)
    GOTO_NEXT


    /*
     * Throw an exception indicating that we have not implemented this
     * mode of filled-new-array.
     */
.LOP_FILLED_NEW_ARRAY_notimpl:
    movl    $.LstrInternalError,%eax
    movl    %eax,OUT_ARG0(%esp)
    movl    $.LstrFilledNewArrayNotImpl,%eax
    movl    %eax,OUT_ARG1(%esp)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown


/* continuation for OP_FILLED_NEW_ARRAY_RANGE */

.LOP_FILLED_NEW_ARRAY_RANGE_more:
    movl    offMethod_clazz(%eax),%eax        # eax<- method->clazz
    movl    %eax,OUT_ARG0(%esp)               # arg0<- clazz
    call    dvmResolveClass                   # eax<- call(clazz,ref,flag)
    UNSPILL(rPC)
    testl   %eax,%eax                         # null?
    je      common_exceptionThrown            # yes, handle it

       # note: fall through to .LOP_FILLED_NEW_ARRAY_RANGE_continue

    /*
     * On entry:
     *    eax holds array class [r0]
     *    rINST_FULL holds AA or BB [r10]
     *    ecx is scratch
     *    rPC is valid, but has been spilled
     */
.LOP_FILLED_NEW_ARRAY_RANGE_continue:
    movl    offClassObject_descriptor(%eax),%ecx  # ecx<- arrayClass->descriptor
    movl    $ALLOC_DONT_TRACK,OUT_ARG2(%esp)     # arg2<- flags
    movzbl  1(%ecx),%ecx                          # ecx<- descriptor[1]
    movl    %ecx,OUT_ARG0(%esp)                   # arg0<- typeCh
    cmpb    $'I',%cl                             # supported?
    jne      .LOP_FILLED_NEW_ARRAY_RANGE_notimpl                  # no, not handled yet
    .if      (!1)
    SPILL_TMP(rINST_FULL)                         # save copy, need "B" later
    sarl    $4,rINST_FULL
    .endif
    movl    rINST_FULL,OUT_ARG1(%esp)             # arg1<- A or AA (length)
    call    dvmAllocPrimitiveArray      # eax<- call(typeCh, length, flags)
    UNSPILL(rPC)
    GET_GLUE(%ecx)
    testl   %eax,%eax                             # alloc successful?
    je      common_exceptionThrown                # no, handle exception
    movl    %eax,offGlue_retval(%ecx)             # retval.l<- new array
    movzwl  4(rPC),%ecx                           # ecx<- FEDC or CCCC
    leal    offArrayObject_contents(%eax),%eax    # eax<- newArray->contents

/* at this point:
 *     eax is pointer to tgt
 *     rINST_FULL is length
 *     ecx is FEDC or CCCC
 *     TMP_SPILL is BA
 *     rPC is valid, but spilled
 *  We now need to copy values from registers into the array
 */

    .if 1
    # set up src pointer
    SPILL(rFP)     # esi
    SPILL(rIBASE)   # edi
    movl    %eax,%edi         # set up dst ptr
    leal    (rFP,%ecx,4),%esi # set up src ptr
    movl    rINST_FULL,%ecx   # load count register
    FETCH_INST_WORD(3)
    rep
    movsd
    UNSPILL(rIBASE)
    UNSPILL(rFP)
    .else
    testl  rINST_FULL,rINST_FULL
    je     4f
    UNSPILL_TMP(rPC)
    andl   $0x0f,rPC            # rPC<- 0000000A
    sall   $16,rPC              # rPC<- 000A0000
    orl    %ecx,rPC              # rpc<- 000AFEDC
3:
    movl   $0xf,%ecx
    andl   rPC,%ecx           # ecx<- next reg to load
    GET_VREG(%ecx,%ecx)
    shrl   $4,rPC
    leal   4(%eax),%eax
    movl   %ecx,-4(%eax)
    sub    $1,rINST_FULL
    jne    3b
4:
    UNSPILL(rPC)
    FETCH_INST_WORD(3)
    .endif

    ADVANCE_PC(3)
    GOTO_NEXT


    /*
     * Throw an exception indicating that we have not implemented this
     * mode of filled-new-array.
     */
.LOP_FILLED_NEW_ARRAY_RANGE_notimpl:
    movl    $.LstrInternalError,%eax
    movl    %eax,OUT_ARG0(%esp)
    movl    $.LstrFilledNewArrayNotImpl,%eax
    movl    %eax,OUT_ARG1(%esp)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown


/* continuation for OP_CMPL_FLOAT */

.LOP_CMPL_FLOAT_isNaN:
    movl      $-1,%ecx
    jmp       .LOP_CMPL_FLOAT_finish

/* continuation for OP_CMPG_FLOAT */

.LOP_CMPG_FLOAT_isNaN:
    movl      $1,%ecx
    jmp       .LOP_CMPG_FLOAT_finish

/* continuation for OP_CMPL_DOUBLE */

.LOP_CMPL_DOUBLE_isNaN:
    movl      $-1,%ecx
    jmp       .LOP_CMPL_DOUBLE_finish

/* continuation for OP_CMPG_DOUBLE */

.LOP_CMPG_DOUBLE_isNaN:
    movl      $1,%ecx
    jmp       .LOP_CMPG_DOUBLE_finish

/* continuation for OP_CMP_LONG */

.LOP_CMP_LONG_bigger:
    UNSPILL(rPC)
    movl      $1,%ecx
    jmp       .LOP_CMP_LONG_finish
.LOP_CMP_LONG_smaller:
    UNSPILL(rPC)
    movl      $-1,%ecx
.LOP_CMP_LONG_finish:
    SET_VREG(%ecx,rINST_FULL)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_AGET_WIDE */

.LOP_AGET_WIDE_finish:
    leal      offArrayObject_contents(%eax,%ecx,8),%eax
    movl      (%eax),%ecx
    movl      4(%eax),%eax
    SET_VREG_WORD(%ecx,rINST_FULL,0)
    SET_VREG_WORD(%eax,rINST_FULL,1)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_APUT_WIDE */

.LOP_APUT_WIDE_finish:
    leal      offArrayObject_contents(%eax,%ecx,8),%eax
    GET_VREG_WORD(%ecx,rINST_FULL,0)
    GET_VREG_WORD(rINST_FULL,rINST_FULL,1)
    movl      rINST_FULL,4(%eax)
    FETCH_INST_WORD(2)
    movl      %ecx,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_APUT_OBJECT */

    /* On entry:
     *   eax<- array object
     *   ecx<- index
     *   rINST_FULL<- vAA
     */
.LOP_APUT_OBJECT_continue:
    leal      offArrayObject_contents(%eax,%ecx,4),%ecx
    testl     rINST_FULL,rINST_FULL     # storing null reference?
    je        .LOP_APUT_OBJECT_skip_check
    SPILL(rPC)
    SPILL_TMP(%ecx)
    movl      offObject_clazz(%eax),%eax # eax<- arrayObj->clazz
    movl      offObject_clazz(rINST_FULL),%ecx # ecx<- obj->clazz
    movl      %eax,OUT_ARG1(%esp)
    movl      %ecx,OUT_ARG0(%esp)
    call      dvmCanPutArrayElement     # test object type vs. array type
    UNSPILL(rPC)
    UNSPILL_TMP(%ecx)
    testl     %eax,%eax
    je        common_errArrayStore

.LOP_APUT_OBJECT_skip_check:
    movl      rINST_FULL,(%ecx)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET */


.LOP_IGET_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_finish
    jmp     common_exceptionThrown

.LOP_IGET_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_WIDE */


.LOP_IGET_WIDE_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_WIDE_finish
    jmp     common_exceptionThrown

.LOP_IGET_WIDE_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    leal    (%ecx,%eax,1),%eax                   # eax<- address of field
    movl    (%eax),%ecx                          # ecx<- lsw
    movl    4(%eax),%eax                         # eax<- msw
    SET_VREG_WORD(%ecx,rINST_FULL,0)
    SET_VREG_WORD(%eax,rINST_FULL,1)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_OBJECT */


.LOP_IGET_OBJECT_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_OBJECT_finish
    jmp     common_exceptionThrown

.LOP_IGET_OBJECT_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_BOOLEAN */


.LOP_IGET_BOOLEAN_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_BOOLEAN_finish
    jmp     common_exceptionThrown

.LOP_IGET_BOOLEAN_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movzbl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_BYTE */


.LOP_IGET_BYTE_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_BYTE_finish
    jmp     common_exceptionThrown

.LOP_IGET_BYTE_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movsbl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_CHAR */


.LOP_IGET_CHAR_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_CHAR_finish
    jmp     common_exceptionThrown

.LOP_IGET_CHAR_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movzwl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IGET_SHORT */


.LOP_IGET_SHORT_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IGET_SHORT_finish
    jmp     common_exceptionThrown

.LOP_IGET_SHORT_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movswl   (%ecx,%eax,1),%ecx                   # ecx<- obj.field (8/16/32 bits)
    movl    rINST_FULL,%eax                      # eax<- A
    FETCH_INST_WORD(2)
    SET_VREG(%ecx,%eax)
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_IPUT */


.LOP_IPUT_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_finish
    jmp     common_exceptionThrown

.LOP_IPUT_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movl   rINST_FULL,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_WIDE */


.LOP_IPUT_WIDE_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_WIDE_finish
    jmp     common_exceptionThrown

.LOP_IPUT_WIDE_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    leal    (%ecx,%eax,1),%eax                   # eax<- address of field
    GET_VREG_WORD(%ecx,rINST_FULL,0)             # ecx<- lsw
    GET_VREG_WORD(rINST_FULL,rINST_FULL,1)       # rINST_FULL<- msw
    movl    rINST_FULL,4(%eax)
    FETCH_INST_WORD(2)
    movl    %ecx,(%eax)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_OBJECT */


.LOP_IPUT_OBJECT_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_OBJECT_finish
    jmp     common_exceptionThrown

.LOP_IPUT_OBJECT_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movl   rINST_FULL,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_BOOLEAN */


.LOP_IPUT_BOOLEAN_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_BOOLEAN_finish
    jmp     common_exceptionThrown

.LOP_IPUT_BOOLEAN_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movb   rINST_LO,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_BYTE */


.LOP_IPUT_BYTE_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_BYTE_finish
    jmp     common_exceptionThrown

.LOP_IPUT_BYTE_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movb   rINST_LO,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_CHAR */


.LOP_IPUT_CHAR_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_CHAR_finish
    jmp     common_exceptionThrown

.LOP_IPUT_CHAR_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movw   rINST,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_IPUT_SHORT */


.LOP_IPUT_SHORT_resolve:
    EXPORT_PC()
    SPILL(rPC)
    movl    offGlue_method(rIBASE),rPC            # rPC<- current method
    UNSPILL(rIBASE)
    movl    offMethod_clazz(rPC),rPC              # rPC<- method->clazz
    SPILL_TMP(%ecx)                               # save object pointer across call
    movl    rPC,OUT_ARG0(%esp)                    # pass in method->clazz
    call    dvmResolveInstField                   #  ... to dvmResolveInstField
    UNSPILL_TMP(%ecx)
    UNSPILL(rPC)
    testl   %eax,%eax                             #  ... which returns InstrField ptr
    jne     .LOP_IPUT_SHORT_finish
    jmp     common_exceptionThrown

.LOP_IPUT_SHORT_finish:
    /*
     * Currently:
     *   eax holds resolved field
     *   ecx holds object
     *   rIBASE is scratch, but needs to be unspilled
     *   rINST_FULL holds A
     */
    GET_VREG(rINST_FULL,rINST_FULL)              # rINST_FULL<- v[A]
    movl    offInstField_byteOffset(%eax),%eax   # eax<- byte offset of field
    UNSPILL(rIBASE)
    testl   %ecx,%ecx                            # object null?
    je      common_errNullObject                 # object was null
    movw   rINST,(%ecx,%eax,1)            # obj.field <- v[A](8/16/32 bits)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_SGET */

    /*
     * Go resolve the field
     */
.LOP_SGET_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_WIDE */

    /*
     * Go resolve the field
     */
.LOP_SGET_WIDE_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_WIDE_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_OBJECT */

    /*
     * Go resolve the field
     */
.LOP_SGET_OBJECT_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_OBJECT_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_BOOLEAN */

    /*
     * Go resolve the field
     */
.LOP_SGET_BOOLEAN_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_BOOLEAN_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_BYTE */

    /*
     * Go resolve the field
     */
.LOP_SGET_BYTE_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_BYTE_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_CHAR */

    /*
     * Go resolve the field
     */
.LOP_SGET_CHAR_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_CHAR_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SGET_SHORT */

    /*
     * Go resolve the field
     */
.LOP_SGET_SHORT_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SGET_SHORT_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT */

    /*
     * Go resolve the field
     */
.LOP_SPUT_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_WIDE */

    /*
     * Go resolve the field
     */
.LOP_SPUT_WIDE_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_WIDE_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_OBJECT */

    /*
     * Go resolve the field
     */
.LOP_SPUT_OBJECT_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_OBJECT_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_BOOLEAN */

    /*
     * Go resolve the field
     */
.LOP_SPUT_BOOLEAN_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_BOOLEAN_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_BYTE */

    /*
     * Go resolve the field
     */
.LOP_SPUT_BYTE_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_BYTE_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_CHAR */

    /*
     * Go resolve the field
     */
.LOP_SPUT_CHAR_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_CHAR_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_SPUT_SHORT */

    /*
     * Go resolve the field
     */
.LOP_SPUT_SHORT_resolve:
    GET_GLUE(%ecx)
    movzwl   2(rPC),%eax                        # eax<- field ref BBBB
    movl     offGlue_method(%ecx),%ecx          # ecx<- current method
    EXPORT_PC()                                 # could throw, need to export
    movl     offMethod_clazz(%ecx),%ecx         # ecx<- method->clazz
    SPILL(rPC)
    movl     %eax,OUT_ARG1(%esp)
    movl     %ecx,OUT_ARG0(%esp)
    call     dvmResolveStaticField              # eax<- resolved StaticField ptr
    UNSPILL(rPC)
    testl    %eax,%eax
    jne      .LOP_SPUT_SHORT_finish                 # success, continue
    jmp      common_exceptionThrown             # no, handle exception


/* continuation for OP_INVOKE_VIRTUAL */


.LOP_INVOKE_VIRTUAL_more:
    movl      offMethod_clazz(%eax),%eax  # ecx<- method->clazz
    movl      %eax,OUT_ARG0(%esp)         # arg0<- clazz
    movl      $METHOD_VIRTUAL,OUT_ARG2(%esp) # arg2<- flags
    call      dvmResolveMethod            # eax<- call(clazz, ref, flags)
    UNSPILL(rPC)
    testl     %eax,%eax                   # got null?
    jne       .LOP_INVOKE_VIRTUAL_continue        # no, continue
    jmp       common_exceptionThrown      # yes, handle exception

    /* At this point:
     *   eax = resolved base method
     *   ecx = scratch
     */
.LOP_INVOKE_VIRTUAL_continue:
    movzwl    4(rPC),%ecx               # ecx<- GFED or CCCC
    .if       (!0)
    andl      $0xf,%ecx                # ecx<- D (or stays CCCC)
    .endif
    GET_VREG(%ecx,%ecx)                 # ecx<- "this"
    movzwl    offMethod_methodIndex(%eax),%eax  # eax<- baseMethod->methodIndex
    testl     %ecx,%ecx                 # null this?
    je        common_errNullObject      # go if so
    movl      offObject_clazz(%ecx),%ecx  # ecx<- thisPtr->clazz
    movl      offClassObject_vtable(%ecx),%ecx # ecx<- thisPtr->clazz->vtable
    movl      (%ecx,%eax,4),%eax        # eax<- vtable[methodIndex]
    jmp       common_invokeMethodNoRange


/* continuation for OP_INVOKE_SUPER */

    /*
     * At this point:
     *  ecx = resolved base method [r0]
     *  eax = method->clazz [r9]
     */
.LOP_INVOKE_SUPER_continue:
    movl    offClassObject_super(%eax),%eax   # eax<- method->clazz->super
    movzwl  offMethod_methodIndex(%ecx),%ecx  # ecx<- baseMthod->methodIndex
    cmpl    offClassObject_vtableCount(%eax),%ecx # compare(methodIndex,vtableCount)
    jae     .LOP_INVOKE_SUPER_nsm           # method not present in superclass
    movl    offClassObject_vtable(%eax),%eax   # eax<- ...clazz->super->vtable
    movl    (%eax,%ecx,4),%eax        # eax<- vtable[methodIndex]
    jmp     common_invokeMethodNoRange


    /* At this point:
     * ecx = null (needs to be resolved base method)
     * eax = method->clazz
    */
.LOP_INVOKE_SUPER_resolve:
    SPILL_TMP(%eax)                     # method->clazz
    movl    %eax,OUT_ARG0(%esp)         # arg0<- method->clazz
    movzwl  2(rPC),%ecx                 # ecx<- BBBB
    movl    $METHOD_VIRTUAL,OUT_ARG2(%esp)  # arg2<- resolver method type
    movl    %ecx,OUT_ARG1(%esp)         # arg1<- ref
    SPILL(rPC)
    call    dvmResolveMethod            # eax<- call(clazz, ref, flags)
    UNSPILL(rPC)
    testl   %eax,%eax                   # got null?
    movl    %eax,%ecx                   # ecx<- resolved base method
    UNSPILL_TMP(%eax)                   # restore method->clazz
    jne     .LOP_INVOKE_SUPER_continue        # good to go - continue
    jmp     common_exceptionThrown      # handle exception

    /*
     * Throw a NoSuchMethodError with the method name as the message.
     *  ecx = resolved base method
     */
.LOP_INVOKE_SUPER_nsm:
    movl    offMethod_name(%ecx),%eax
    mov     %eax,OUT_ARG1(%esp)
    jmp     common_errNoSuchMethod

/* continuation for OP_INVOKE_DIRECT */

    /*
     * On entry:
     *   TMP_SPILL  <- "this" register
     * Things a bit ugly on this path, but it's the less
     * frequent one.  We'll have to do some reloading.
     */
.LOP_INVOKE_DIRECT_resolve:
     SPILL_TMP(%ecx)
     GET_GLUE(%ecx)
     UNSPILL(rPC)
     movl     offGlue_method(%ecx),%ecx  # ecx<- glue->method
     movzwl   2(rPC),%eax      # reference (BBBB or CCCC)
     movl     offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
     movl     $METHOD_DIRECT,OUT_ARG2(%esp)
     movl     %eax,OUT_ARG1(%esp)
     movl     %ecx,OUT_ARG0(%esp)
     call     dvmResolveMethod # eax<- call(clazz, ref, flags)
     UNSPILL_TMP(%ecx)
     testl    %eax,%eax
     jne      .LOP_INVOKE_DIRECT_finish
     UNSPILL(rPC)
     jmp      common_exceptionThrown

/* continuation for OP_INVOKE_STATIC */

.LOP_INVOKE_STATIC_continue:
    movl      $METHOD_STATIC,%eax
    movl      %eax,OUT_ARG2(%esp)       # arg2<- flags
    SPILL(rPC)
    call      dvmResolveMethod          # call(clazz,ref,flags)
    UNSPILL(rPC)
    testl     %eax,%eax                 # got null?
    jne       common_invokeMethodNoRange
    jmp       common_exceptionThrown


/* continuation for OP_INVOKE_INTERFACE */

.LOP_INVOKE_INTERFACE_continue:
    call       dvmFindInterfaceMethodInCache # eax<- call(class, ref, method, dex)
    UNSPILL(rPC)
    testl      %eax,%eax
    je         common_exceptionThrown
    jmp        common_invokeMethodNoRange


/* continuation for OP_INVOKE_VIRTUAL_RANGE */


.LOP_INVOKE_VIRTUAL_RANGE_more:
    movl      offMethod_clazz(%eax),%eax  # ecx<- method->clazz
    movl      %eax,OUT_ARG0(%esp)         # arg0<- clazz
    movl      $METHOD_VIRTUAL,OUT_ARG2(%esp) # arg2<- flags
    call      dvmResolveMethod            # eax<- call(clazz, ref, flags)
    UNSPILL(rPC)
    testl     %eax,%eax                   # got null?
    jne       .LOP_INVOKE_VIRTUAL_RANGE_continue        # no, continue
    jmp       common_exceptionThrown      # yes, handle exception

    /* At this point:
     *   eax = resolved base method
     *   ecx = scratch
     */
.LOP_INVOKE_VIRTUAL_RANGE_continue:
    movzwl    4(rPC),%ecx               # ecx<- GFED or CCCC
    .if       (!1)
    andl      $0xf,%ecx                # ecx<- D (or stays CCCC)
    .endif
    GET_VREG(%ecx,%ecx)                 # ecx<- "this"
    movzwl    offMethod_methodIndex(%eax),%eax  # eax<- baseMethod->methodIndex
    testl     %ecx,%ecx                 # null this?
    je        common_errNullObject      # go if so
    movl      offObject_clazz(%ecx),%ecx  # ecx<- thisPtr->clazz
    movl      offClassObject_vtable(%ecx),%ecx # ecx<- thisPtr->clazz->vtable
    movl      (%ecx,%eax,4),%eax        # eax<- vtable[methodIndex]
    jmp       common_invokeMethodRange


/* continuation for OP_INVOKE_SUPER_RANGE */

    /*
     * At this point:
     *  ecx = resolved base method [r0]
     *  eax = method->clazz [r9]
     */
.LOP_INVOKE_SUPER_RANGE_continue:
    movl    offClassObject_super(%eax),%eax   # eax<- method->clazz->super
    movzwl  offMethod_methodIndex(%ecx),%ecx  # ecx<- baseMthod->methodIndex
    cmpl    offClassObject_vtableCount(%eax),%ecx # compare(methodIndex,vtableCount)
    jae     .LOP_INVOKE_SUPER_RANGE_nsm           # method not present in superclass
    movl    offClassObject_vtable(%eax),%eax   # eax<- ...clazz->super->vtable
    movl    (%eax,%ecx,4),%eax        # eax<- vtable[methodIndex]
    jmp     common_invokeMethodRange


    /* At this point:
     * ecx = null (needs to be resolved base method)
     * eax = method->clazz
    */
.LOP_INVOKE_SUPER_RANGE_resolve:
    SPILL_TMP(%eax)                     # method->clazz
    movl    %eax,OUT_ARG0(%esp)         # arg0<- method->clazz
    movzwl  2(rPC),%ecx                 # ecx<- BBBB
    movl    $METHOD_VIRTUAL,OUT_ARG2(%esp)  # arg2<- resolver method type
    movl    %ecx,OUT_ARG1(%esp)         # arg1<- ref
    SPILL(rPC)
    call    dvmResolveMethod            # eax<- call(clazz, ref, flags)
    UNSPILL(rPC)
    testl   %eax,%eax                   # got null?
    movl    %eax,%ecx                   # ecx<- resolved base method
    UNSPILL_TMP(%eax)                   # restore method->clazz
    jne     .LOP_INVOKE_SUPER_RANGE_continue        # good to go - continue
    jmp     common_exceptionThrown      # handle exception

    /*
     * Throw a NoSuchMethodError with the method name as the message.
     *  ecx = resolved base method
     */
.LOP_INVOKE_SUPER_RANGE_nsm:
    movl    offMethod_name(%ecx),%eax
    mov     %eax,OUT_ARG1(%esp)
    jmp     common_errNoSuchMethod

/* continuation for OP_INVOKE_DIRECT_RANGE */

    /*
     * On entry:
     *   TMP_SPILL  <- "this" register
     * Things a bit ugly on this path, but it's the less
     * frequent one.  We'll have to do some reloading.
     */
.LOP_INVOKE_DIRECT_RANGE_resolve:
     SPILL_TMP(%ecx)
     GET_GLUE(%ecx)
     UNSPILL(rPC)
     movl     offGlue_method(%ecx),%ecx  # ecx<- glue->method
     movzwl   2(rPC),%eax      # reference (BBBB or CCCC)
     movl     offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
     movl     $METHOD_DIRECT,OUT_ARG2(%esp)
     movl     %eax,OUT_ARG1(%esp)
     movl     %ecx,OUT_ARG0(%esp)
     call     dvmResolveMethod # eax<- call(clazz, ref, flags)
     UNSPILL_TMP(%ecx)
     testl    %eax,%eax
     jne      .LOP_INVOKE_DIRECT_RANGE_finish
     UNSPILL(rPC)
     jmp      common_exceptionThrown

/* continuation for OP_INVOKE_STATIC_RANGE */

.LOP_INVOKE_STATIC_RANGE_continue:
    movl      $METHOD_STATIC,%eax
    movl      %eax,OUT_ARG2(%esp)       # arg2<- flags
    SPILL(rPC)
    call      dvmResolveMethod          # call(clazz,ref,flags)
    UNSPILL(rPC)
    testl     %eax,%eax                 # got null?
    jne       common_invokeMethodRange
    jmp       common_exceptionThrown


/* continuation for OP_INVOKE_INTERFACE_RANGE */

.LOP_INVOKE_INTERFACE_RANGE_continue:
    call       dvmFindInterfaceMethodInCache # eax<- call(class, ref, method, dex)
    UNSPILL(rPC)
    testl      %eax,%eax
    je         common_exceptionThrown
    jmp        common_invokeMethodRange


/* continuation for OP_FLOAT_TO_INT */


.LOP_FLOAT_TO_INT_continue:
    .if 0
    movl     $0x80000000,%eax
    xorl     4(rFP,%ecx,4),%eax
    orl      (rFP,%ecx,4),%eax
    .else
    cmpl     $0x80000000,(rFP,%ecx,4)
    .endif
    je       .LOP_FLOAT_TO_INT_special_case # fix up result

.LOP_FLOAT_TO_INT_finish:
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_FLOAT_TO_INT_special_case:
    fnstsw   %ax
    sahf
    jp       .LOP_FLOAT_TO_INT_isNaN
    adcl     $-1,(rFP,%ecx,4)
    .if 0
    adcl     $-1,4(rFP,%ecx,4)
    .endif
   jmp       .LOP_FLOAT_TO_INT_finish
.LOP_FLOAT_TO_INT_isNaN:
    movl      $0,(rFP,%ecx,4)
    .if 0
    movl      $0,4(rFP,%ecx,4)
    .endif
    jmp       .LOP_FLOAT_TO_INT_finish

/* continuation for OP_FLOAT_TO_LONG */


.LOP_FLOAT_TO_LONG_continue:
    .if 1
    movl     $0x80000000,%eax
    xorl     4(rFP,%ecx,4),%eax
    orl      (rFP,%ecx,4),%eax
    .else
    cmpl     $0x80000000,(rFP,%ecx,4)
    .endif
    je       .LOP_FLOAT_TO_LONG_special_case # fix up result

.LOP_FLOAT_TO_LONG_finish:
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_FLOAT_TO_LONG_special_case:
    fnstsw   %ax
    sahf
    jp       .LOP_FLOAT_TO_LONG_isNaN
    adcl     $-1,(rFP,%ecx,4)
    .if 1
    adcl     $-1,4(rFP,%ecx,4)
    .endif
   jmp       .LOP_FLOAT_TO_LONG_finish
.LOP_FLOAT_TO_LONG_isNaN:
    movl      $0,(rFP,%ecx,4)
    .if 1
    movl      $0,4(rFP,%ecx,4)
    .endif
    jmp       .LOP_FLOAT_TO_LONG_finish

/* continuation for OP_DOUBLE_TO_INT */


.LOP_DOUBLE_TO_INT_continue:
    .if 0
    movl     $0x80000000,%eax
    xorl     4(rFP,%ecx,4),%eax
    orl      (rFP,%ecx,4),%eax
    .else
    cmpl     $0x80000000,(rFP,%ecx,4)
    .endif
    je       .LOP_DOUBLE_TO_INT_special_case # fix up result

.LOP_DOUBLE_TO_INT_finish:
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_DOUBLE_TO_INT_special_case:
    fnstsw   %ax
    sahf
    jp       .LOP_DOUBLE_TO_INT_isNaN
    adcl     $-1,(rFP,%ecx,4)
    .if 0
    adcl     $-1,4(rFP,%ecx,4)
    .endif
   jmp       .LOP_DOUBLE_TO_INT_finish
.LOP_DOUBLE_TO_INT_isNaN:
    movl      $0,(rFP,%ecx,4)
    .if 0
    movl      $0,4(rFP,%ecx,4)
    .endif
    jmp       .LOP_DOUBLE_TO_INT_finish

/* continuation for OP_DOUBLE_TO_LONG */


.LOP_DOUBLE_TO_LONG_continue:
    .if 1
    movl     $0x80000000,%eax
    xorl     4(rFP,%ecx,4),%eax
    orl      (rFP,%ecx,4),%eax
    .else
    cmpl     $0x80000000,(rFP,%ecx,4)
    .endif
    je       .LOP_DOUBLE_TO_LONG_special_case # fix up result

.LOP_DOUBLE_TO_LONG_finish:
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_DOUBLE_TO_LONG_special_case:
    fnstsw   %ax
    sahf
    jp       .LOP_DOUBLE_TO_LONG_isNaN
    adcl     $-1,(rFP,%ecx,4)
    .if 1
    adcl     $-1,4(rFP,%ecx,4)
    .endif
   jmp       .LOP_DOUBLE_TO_LONG_finish
.LOP_DOUBLE_TO_LONG_isNaN:
    movl      $0,(rFP,%ecx,4)
    .if 1
    movl      $0,4(rFP,%ecx,4)
    .endif
    jmp       .LOP_DOUBLE_TO_LONG_finish

/* continuation for OP_DIV_INT */
.LOP_DIV_INT_continue_div:
    cltd
    idivl   %ecx
.LOP_DIV_INT_finish_div:
    movzbl   rINST_HI,%ecx         # ecl<- AA
    SET_VREG(%eax,%ecx)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_REM_INT */
.LOP_REM_INT_continue_div:
    cltd
    idivl   %ecx
.LOP_REM_INT_finish_div:
    movzbl   rINST_HI,%ecx         # ecl<- AA
    SET_VREG(%edx,%ecx)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_MUL_LONG */

.LOP_MUL_LONG_continue:
    leal      (%ecx,%edx),%edx    # full result now in %edx:%eax
    movzbl    rINST_HI,%ecx       # ecx<- A
    movl      %edx,4(rFP,%ecx,4)  # v[B+1]<- %edx
    UNSPILL(rPC)                  # restore rPC/%edx
    FETCH_INST_WORD(2)
    UNSPILL(rIBASE)
    movl      %eax,(rFP,%ecx,4)   # v[B]<- %eax
    ADVANCE_PC(2)
    GOTO_NEXT


/* continuation for OP_DIV_LONG */

.LOP_DIV_LONG_continue:
    call     __divdi3
.LOP_DIV_LONG_finish:
    movzbl   rINST_HI,%ecx
    SET_VREG_WORD(rPC,%ecx,1)
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

.LOP_DIV_LONG_check_zero:
    testl   rPC,rPC
    jne     .LOP_DIV_LONG_notSpecial
    UNSPILL(rPC)
    jmp     common_errDivideByZero
.LOP_DIV_LONG_check_neg1:
    testl   rPC,%eax
    jne     .LOP_DIV_LONG_notSpecial
    GET_VREG_WORD(rPC,%ecx,0)
    GET_VREG_WORD(%ecx,%ecx,1)
    testl    rPC,rPC
    jne      .LOP_DIV_LONG_notSpecial1
    cmpl     $0x80000000,%ecx
    jne      .LOP_DIV_LONG_notSpecial1
    /* minint / -1, return minint on div, 0 on rem */
    xorl     %eax,%eax
    movl     $0x80000000,%edx
    jmp      .LOP_DIV_LONG_finish




/* continuation for OP_REM_LONG */

.LOP_REM_LONG_continue:
    call     __moddi3
.LOP_REM_LONG_finish:
    movzbl   rINST_HI,%ecx
    SET_VREG_WORD(rPC,%ecx,1)
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

.LOP_REM_LONG_check_zero:
    testl   rPC,rPC
    jne     .LOP_REM_LONG_notSpecial
    UNSPILL(rPC)
    jmp     common_errDivideByZero
.LOP_REM_LONG_check_neg1:
    testl   rPC,%eax
    jne     .LOP_REM_LONG_notSpecial
    GET_VREG_WORD(rPC,%ecx,0)
    GET_VREG_WORD(%ecx,%ecx,1)
    testl    rPC,rPC
    jne      .LOP_REM_LONG_notSpecial1
    cmpl     $0x80000000,%ecx
    jne      .LOP_REM_LONG_notSpecial1
    /* minint / -1, return minint on div, 0 on rem */
    xorl     %eax,%eax
    movl     $0,%edx
    jmp      .LOP_REM_LONG_finish




/* continuation for OP_SHL_LONG */

.LOP_SHL_LONG_finish:
    SET_VREG_WORD(%eax,%ecx,0)         # v[AA+0]<- %eax
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_SHR_LONG */


.LOP_SHR_LONG_finish:
    SET_VREG_WORD(%eax,%ecx,0)         # v[AA+0]<- eax
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_USHR_LONG */


.LOP_USHR_LONG_finish:
    SET_VREG_WORD(%eax,%ecx,0)        # v[BB+0]<- eax
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_DIV_INT_2ADDR */
.LOP_DIV_INT_2ADDR_continue_div2addr:
    cltd
    idivl   %ecx
.LOP_DIV_INT_2ADDR_finish_div2addr:
    SET_VREG(%eax,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_REM_INT_2ADDR */
.LOP_REM_INT_2ADDR_continue_div2addr:
    cltd
    idivl   %ecx
.LOP_REM_INT_2ADDR_finish_div2addr:
    SET_VREG(%edx,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_MUL_LONG_2ADDR */

.LOP_MUL_LONG_2ADDR_continue:
    leal      (%ecx,%edx),%edx    # full result now in %edx:%eax
    movl      %edx,4(rIBASE)      # v[A+1]<- %edx
    UNSPILL(rPC)                  # restore rPC/%edx
    FETCH_INST_WORD(1)
    movl      %eax,(rIBASE)       # v[A]<- %eax
    UNSPILL(rFP)
    UNSPILL(rIBASE)
    ADVANCE_PC(1)
    GOTO_NEXT


/* continuation for OP_DIV_LONG_2ADDR */

.LOP_DIV_LONG_2ADDR_continue:
    movl     %eax,OUT_ARG3(%esp)
    movl     rPC,OUT_ARG0(%esp)
    movl     %ecx,OUT_ARG1(%esp)
    call     __divdi3
.LOP_DIV_LONG_2ADDR_finish:
    movl     rINST_FULL,%ecx
    SET_VREG_WORD(rPC,%ecx,1)
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_DIV_LONG_2ADDR_check_zero:
    testl   rPC,rPC
    jne     .LOP_DIV_LONG_2ADDR_notSpecial
    UNSPILL(rPC)
    jmp     common_errDivideByZero
.LOP_DIV_LONG_2ADDR_check_neg1:
    testl   rPC,%eax
    jne     .LOP_DIV_LONG_2ADDR_notSpecial
    GET_VREG_WORD(rPC,rINST_FULL,0)
    GET_VREG_WORD(%ecx,rINST_FULL,1)
    testl    rPC,rPC
    jne      .LOP_DIV_LONG_2ADDR_notSpecial1
    cmpl     $0x80000000,%ecx
    jne      .LOP_DIV_LONG_2ADDR_notSpecial1
    /* minint / -1, return minint on div, 0 on rem */
    xorl     %eax,%eax
    movl     $0x80000000,%edx
    jmp      .LOP_DIV_LONG_2ADDR_finish


/* continuation for OP_REM_LONG_2ADDR */

.LOP_REM_LONG_2ADDR_continue:
    movl     %eax,OUT_ARG3(%esp)
    movl     rPC,OUT_ARG0(%esp)
    movl     %ecx,OUT_ARG1(%esp)
    call     __moddi3
.LOP_REM_LONG_2ADDR_finish:
    movl     rINST_FULL,%ecx
    SET_VREG_WORD(rPC,%ecx,1)
    UNSPILL(rPC)
    SET_VREG_WORD(%eax,%ecx,0)
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

.LOP_REM_LONG_2ADDR_check_zero:
    testl   rPC,rPC
    jne     .LOP_REM_LONG_2ADDR_notSpecial
    UNSPILL(rPC)
    jmp     common_errDivideByZero
.LOP_REM_LONG_2ADDR_check_neg1:
    testl   rPC,%eax
    jne     .LOP_REM_LONG_2ADDR_notSpecial
    GET_VREG_WORD(rPC,rINST_FULL,0)
    GET_VREG_WORD(%ecx,rINST_FULL,1)
    testl    rPC,rPC
    jne      .LOP_REM_LONG_2ADDR_notSpecial1
    cmpl     $0x80000000,%ecx
    jne      .LOP_REM_LONG_2ADDR_notSpecial1
    /* minint / -1, return minint on div, 0 on rem */
    xorl     %eax,%eax
    movl     $0,%edx
    jmp      .LOP_REM_LONG_2ADDR_finish


/* continuation for OP_SHL_LONG_2ADDR */


.LOP_SHL_LONG_2ADDR_finish:
    SET_VREG_WORD(%eax,rINST_FULL,0)  # v[AA+0]<- eax
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_SHR_LONG_2ADDR */


.LOP_SHR_LONG_2ADDR_finish:
    SET_VREG_WORD(%eax,rINST_FULL,0)  # v[AA+0]<- eax
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_USHR_LONG_2ADDR */


.LOP_USHR_LONG_2ADDR_finish:
    SET_VREG_WORD(%eax,rINST_FULL,0)   # v[AA+0]<- eax
    FETCH_INST_WORD(1)
    ADVANCE_PC(1)
    GOTO_NEXT

/* continuation for OP_DIV_INT_LIT16 */
.LOP_DIV_INT_LIT16_continue_div:
    cltd
    idivl   %ecx
.LOP_DIV_INT_LIT16_finish_div:
    SET_VREG(%eax,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_REM_INT_LIT16 */
.LOP_REM_INT_LIT16_continue_div:
    cltd
    idivl   %ecx
.LOP_REM_INT_LIT16_finish_div:
    SET_VREG(%edx,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_DIV_INT_LIT8 */
.LOP_DIV_INT_LIT8_continue_div:
    cltd
    idivl   %ecx
.LOP_DIV_INT_LIT8_finish_div:
    SET_VREG(%eax,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_REM_INT_LIT8 */
.LOP_REM_INT_LIT8_continue_div:
    cltd
    idivl   %ecx
.LOP_REM_INT_LIT8_finish_div:
    SET_VREG(%edx,rINST_FULL)
    UNSPILL(rPC)
    FETCH_INST_WORD(2)
    ADVANCE_PC(2)
    GOTO_NEXT

/* continuation for OP_EXECUTE_INLINE */

.LOP_EXECUTE_INLINE_continue:
    /*
     * Extract args, call function.
     *  ecx = #of args (0-4)
     *  eax = call index
     *  @esp = return addr
     *  esp is -4 from normal
     *
     *  Go ahead and load all 4 args, even if not used.
     */
    movzwl    4(rPC),rPC

    movl      $0xf,%ecx
    andl      rPC,%ecx
    GET_VREG(%ecx,%ecx)
    sarl      $4,rPC
    movl      %ecx,4+OUT_ARG0(%esp)

    movl      $0xf,%ecx
    andl      rPC,%ecx
    GET_VREG(%ecx,%ecx)
    sarl      $4,rPC
    movl      %ecx,4+OUT_ARG1(%esp)

    movl      $0xf,%ecx
    andl      rPC,%ecx
    GET_VREG(%ecx,%ecx)
    sarl      $4,rPC
    movl      %ecx,4+OUT_ARG2(%esp)

    movl      $0xf,%ecx
    andl      rPC,%ecx
    GET_VREG(%ecx,%ecx)
    sarl      $4,rPC
    movl      %ecx,4+OUT_ARG3(%esp)

    sall      $4,%eax      # index *= sizeof(table entry)
    jmp       *gDvmInlineOpsTable(%eax)
    # will return to caller of .LOP_EXECUTE_INLINE_continue


    .size   dvmAsmSisterStart, .-dvmAsmSisterStart
    .global dvmAsmSisterEnd
dvmAsmSisterEnd:

/* File: x86/entry.S */
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


    .text
    .global dvmMterpStdRun
    .type   dvmMterpStdRun, %function
/*
 * bool dvmMterpStdRun(MterpGlue* glue)
 *
 * Interpreter entry point.  Returns changeInterp.
 *
 */
dvmMterpStdRun:
    push    %ebp
    movl    %esp,%ebp
    push    %edi
    push    %esi
    push    %ebx

/* at this point, stack is misaligned by 1 word
   We're allocating spill space for 6 words, plus
   outgoing argument (5 words) and local variables
   (4 words) - 15 words or 60 bytes total. See
   diagram in header.S
*/
    subl   $60,%esp

/* Set up "named" registers */
    movl    IN_ARG0(%ebp),%ecx
    movl    %ecx,rGLUE_SPILL(%ebp)
    LOAD_PC_FROM_GLUE(%ecx)
    LOAD_FP_FROM_GLUE(%ecx)
    movl    $dvmAsmInstructionStart,rIBASE

/* Remember %esp for future "longjmp" */
    movl    %esp,offGlue_bailPtr(%ecx)

/* How to start? */
    movb    offGlue_entryPoint(%ecx),%al

/* Normal start? */
    cmpb    $kInterpEntryInstr,%al
    jne     .Lnot_instr

   /* Normal case: start executing the instruction at rPC */
    FETCH_INST()
    GOTO_NEXT

.Lnot_instr:
    /* Reset to normal case */
    movb   $kInterpEntryInstr,offGlue_entryPoint(%ecx)
    cmpb   $kInterpEntryReturn,%al
    je     common_returnFromMethod
    cmpb   $kInterpEntryThrow,%al
    je     common_exceptionThrown
    movzx  %al,%eax
    movl   %eax,OUT_ARG1(%esp)
    movl   $.LstrBadEntryPoint,OUT_ARG0(%esp)
    call   printf
    call   dvmAbort
    /* Not reached */


    .global dvmMterpStdBail
    .type   dvmMterpStdBail, %function
/*
 * void dvmMterpStdBail(MterpGlue* glue, bool changeInterp)
 *
 * Restore the stack pointer and PC from the save point established on entry.
 * This is essentially the same as a longjmp, but should be cheaper.  The
 * last instruction causes us to return to whoever called dvmMterpStdRun.
 *
 * We're not going to build a standard frame here, so the arg accesses will
 * look a little strange.
 *
 * On entry:
 *  esp+4 (arg0)  MterpGlue* glue
 *  esp+8 (arg1)  bool changeInterp
 */
dvmMterpStdBail:
    movl    4(%esp),%ecx                 # grab glue
    movl    8(%esp),%eax                 # changeInterp to return reg
    movl    offGlue_bailPtr(%ecx),%esp   # Stack back to normal
    addl    $60,%esp                    # Strip dvmMterpStdRun's frame
    pop     %ebx
    pop     %esi
    pop     %edi
    pop     %ebp
    ret                                  # return to dvmMterpStdRun's caller


/*
 * Strings
 */
    .section    .rodata
.LstrBadEntryPoint:
    .asciz  "Bad entry point %d\n"

/* File: x86/footer.S */
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
 * Common subroutines and data.
 */

/*
 * Common code when a backwards branch is taken
 *
 * On entry:
 *   ebx (a.k.a. rINST_FULL) -> PC adjustment in 16-bit words
 */
common_backwardBranch:
    GET_GLUE(%ecx)
    call   common_periodicChecks  # Note: expects rPC to be preserved
    ADVANCE_PC_INDEXED(rINST_FULL)
    FETCH_INST()
    GOTO_NEXT



/*
 * Common code for method invocation with range.
 *
 * On entry:
 *   eax = Method* methodToCall
 *   rINST trashed, must reload
 */

common_invokeMethodRange:
.LinvokeNewRange:

   /*
    * prepare to copy args to "outs" area of current frame
    */

    movzbl      1(rPC),rINST_FULL       # rINST_FULL<- AA
    movzwl      4(rPC), %ecx            # %ecx<- CCCC
    SPILL(rPC)
    SAVEAREA_FROM_FP(%edx,rFP)          # %edx<- &StackSaveArea
    test        rINST_FULL, rINST_FULL
    movl        rINST_FULL, LOCAL0_OFFSET(%ebp) # LOCAL0_OFFSET(%ebp)<- AA
    jz          .LinvokeArgsDone        # no args; jump to args done


   /*
    * %eax=methodToCall, %ecx=CCCC, LOCAL0_OFFSET(%ebp)=count, %edx=&outs (&stackSaveArea)
    * (very few methods have > 10 args; could unroll for common cases)
    */

    movl        %ebx, LOCAL1_OFFSET(%ebp)       # LOCAL1_OFFSET(%ebp)<- save %ebx
    lea         (rFP, %ecx, 4), %ecx    # %ecx<- &vCCCC
    shll        $2, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- offset
    subl        LOCAL0_OFFSET(%ebp), %edx       # %edx<- update &outs
    shrl        $2, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- offset
1:
    movl        (%ecx), %ebx            # %ebx<- vCCCC
    lea         4(%ecx), %ecx           # %ecx<- &vCCCC++
    subl        $1, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET<- LOCAL0_OFFSET--
    movl        %ebx, (%edx)            # *outs<- vCCCC
    lea         4(%edx), %edx           # outs++
    jne         1b                      # loop if count (LOCAL0_OFFSET(%ebp)) not zero
    movl        LOCAL1_OFFSET(%ebp), %ebx       # %ebx<- restore %ebx
    jmp         .LinvokeArgsDone        # continue

   /*
    * %eax is "Method* methodToCall", the method we're trying to call
    * prepare to copy args to "outs" area of current frame
    */

common_invokeMethodNoRange:
.LinvokeNewNoRange:
    movzbl      1(rPC),rINST_FULL       # rINST_FULL<- BA
    SPILL(rPC)
    movl        rINST_FULL, LOCAL0_OFFSET(%ebp) # LOCAL0_OFFSET(%ebp)<- BA
    shrl        $4, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- B
    je          .LinvokeArgsDone        # no args; jump to args done
    movzwl      4(rPC), %ecx            # %ecx<- GFED
    SAVEAREA_FROM_FP(%edx,rFP)          # %edx<- &StackSaveArea

   /*
    * %eax=methodToCall, %ecx=GFED, LOCAL0_OFFSET(%ebp)=count, %edx=outs
    */

.LinvokeNonRange:
    cmp         $2, LOCAL0_OFFSET(%ebp)        # compare LOCAL0_OFFSET(%ebp) to 2
    movl        %ecx, LOCAL1_OFFSET(%ebp)       # LOCAL1_OFFSET(%ebp)<- GFED
    jl          1f                      # handle 1 arg
    je          2f                      # handle 2 args
    cmp         $4, LOCAL0_OFFSET(%ebp)        # compare LOCAL0_OFFSET(%ebp) to 4
    jl          3f                      # handle 3 args
    je          4f                      # handle 4 args
5:
    andl        $15, rINST_FULL        # rINST<- A
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, rINST_FULL, 4), %ecx # %ecx<- vA
    movl        %ecx, (%edx)            # *outs<- vA
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
4:
    shr         $12, %ecx              # %ecx<- G
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vG
    movl        %ecx, (%edx)            # *outs<- vG
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
3:
    and         $0x0f00, %ecx          # %ecx<- 0F00
    shr         $8, %ecx               # %ecx<- F
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vF
    movl        %ecx, (%edx)            # *outs<- vF
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
2:
    and         $0x00f0, %ecx          # %ecx<- 00E0
    shr         $4, %ecx               # %ecx<- E
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vE
    movl        %ecx, (%edx)            # *outs<- vE
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
1:
    and         $0x000f, %ecx          # %ecx<- 000D
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vD
    movl        %ecx, -4(%edx)          # *--outs<- vD
0:

   /*
    * %eax is "Method* methodToCall", the method we're trying to call
    * find space for the new stack frame, check for overflow
    */

.LinvokeArgsDone:
    movzwl      offMethod_registersSize(%eax), %edx # %edx<- methodToCall->regsSize
    movzwl      offMethod_outsSize(%eax), %ecx # %ecx<- methodToCall->outsSize
    movl        %eax, LOCAL0_OFFSET(%ebp)       # LOCAL0_OFFSET<- methodToCall
    shl         $2, %edx               # %edx<- update offset
    SAVEAREA_FROM_FP(%eax,rFP)          # %eax<- &StackSaveArea
    subl        %edx, %eax              # %eax<- newFP; (old savearea - regsSize)
    GET_GLUE(%edx)                      # %edx<- pMterpGlue
    movl        %eax, LOCAL1_OFFSET(%ebp)       # LOCAL1_OFFSET(%ebp)<- &outs
    subl        $sizeofStackSaveArea, %eax # %eax<- newSaveArea (stack save area using newFP)
    movl        offGlue_interpStackEnd(%edx), %edx # %edx<- glue->interpStackEnd
    movl        %edx, LOCAL2_OFFSET(%ebp)       # LOCAL2_OFFSET<- glue->interpStackEnd
    shl         $2, %ecx               # %ecx<- update offset for outsSize
    movl        %eax, %edx              # %edx<- newSaveArea
    sub         %ecx, %eax              # %eax<- bottom; (newSaveArea - outsSize)
    cmp         LOCAL2_OFFSET(%ebp), %eax       # compare interpStackEnd and bottom
    movl        LOCAL0_OFFSET(%ebp), %eax       # %eax<- restore methodToCall
    jl          .LstackOverflow         # handle frame overflow

   /*
    * set up newSaveArea
    */

#ifdef EASY_GDB
    SAVEAREA_FROM_FP(%ecx,rFP)          # %ecx<- &StackSaveArea
    movl        %ecx, offStackSaveArea_prevSave(%edx) # newSaveArea->prevSave<- &outs
#endif
    movl        rFP, offStackSaveArea_prevFrame(%edx) # newSaveArea->prevFrame<- rFP
    movl        rPC_SPILL(%ebp), %ecx
    movl        %ecx, offStackSaveArea_savedPc(%edx) # newSaveArea->savedPc<- rPC
    testl       $ACC_NATIVE, offMethod_accessFlags(%eax) # check for native call
    movl        %eax, offStackSaveArea_method(%edx) # newSaveArea->method<- method to call
    jne         .LinvokeNative          # handle native call

   /*
    * Update "glue" values for the new method
    * %eax=methodToCall, LOCAL1_OFFSET(%ebp)=newFp
    */

    movl        offMethod_clazz(%eax), %edx # %edx<- method->clazz
    GET_GLUE(%ecx)                      # %ecx<- pMterpGlue
    movl        offClassObject_pDvmDex(%edx), %edx # %edx<- method->clazz->pDvmDex
    movl        %eax, offGlue_method(%ecx) # glue->method<- methodToCall
    movl        %edx, offGlue_methodClassDex(%ecx) # glue->methodClassDex<- method->clazz->pDvmDex
    movl        offMethod_insns(%eax), rPC # rPC<- methodToCall->insns
    movl        offGlue_self(%ecx), %eax # %eax<- glue->self
    movl        LOCAL1_OFFSET(%ebp), rFP # rFP<- newFP
    movl        rFP, offThread_curFrame(%eax) # glue->self->curFrame<- newFP
    FETCH_INST()
    GOTO_NEXT                           # jump to methodToCall->insns

   /*
    * Prep for the native call
    * %eax=methodToCall, LOCAL1_OFFSET(%ebp)=newFP, %edx=newSaveArea
    */

.LinvokeNative:
    GET_GLUE(%ecx)                      # %ecx<- pMterpGlue
    movl        %eax, OUT_ARG1(%esp)    # push parameter methodToCall
    movl        offGlue_self(%ecx), %ecx        # %ecx<- glue->self
    movl        offThread_jniLocal_nextEntry(%ecx), %eax # %eax<- glue->self->thread->refNext
    movl        %eax, offStackSaveArea_localRefTop(%edx) # newSaveArea->localRefTop<- refNext
    movl        %edx, OUT_ARG4(%esp)    # save newSaveArea
    movl        LOCAL1_OFFSET(%ebp), %edx # %edx<- newFP
    movl        %edx, offThread_curFrame(%ecx)  # glue->self->curFrame<- newFP
    movl        %ecx, OUT_ARG3(%esp)    # save glue->self
    movl        %ecx, OUT_ARG2(%esp)    # push parameter glue->self
    GET_GLUE(%ecx)                      # %ecx<- pMterpGlue
    movl        OUT_ARG1(%esp), %eax    # %eax<- methodToCall
    lea         offGlue_retval(%ecx), %ecx # %ecx<- &retval
    movl        %ecx, OUT_ARG0(%esp)    # push parameter pMterpGlue
    push        %edx                    # push parameter newFP

    call        *offMethod_nativeFunc(%eax) # call methodToCall->nativeFunc
    lea         4(%esp), %esp
    movl        OUT_ARG4(%esp), %ecx    # %ecx<- newSaveArea
    movl        OUT_ARG3(%esp), %eax    # %eax<- glue->self
    movl        offStackSaveArea_localRefTop(%ecx), %edx # %edx<- newSaveArea->localRefTop
    cmp         $0, offThread_exception(%eax) # check for exception
    movl        rFP, offThread_curFrame(%eax) # glue->self->curFrame<- rFP
    movl        %edx, offThread_jniLocal_nextEntry(%eax) # glue->self<- newSaveArea->localRefTop
    UNSPILL(rPC)
    jne         common_exceptionThrown  # handle exception
    FETCH_INST_WORD(3)
    ADVANCE_PC(3)
    GOTO_NEXT                           # jump to next instruction

.LstackOverflow:
    GET_GLUE(%eax)                      # %eax<- pMterpGlue
    movl        offGlue_self(%eax), %eax # %eax<- glue->self
    movl        %eax, OUT_ARG0(%esp)    # push parameter self
    call        dvmHandleStackOverflow  # call: (Thread* self)
    UNSPILL(rPC)                        # return: void
    jmp         common_exceptionThrown  # handle exception


/*
 * Common invoke code (old-style).
 * TUNING:  Rewrite along lines of new armv5 code?
 *
 * On entry:
 *   eax = Method* methodToCall
 *   ecx = bool methodCallRange
 *   rINST trashed, must reload
 */
common_invokeOld:
    movl     %ecx,OUT_ARG1(%esp)     # arg1<- methodCallRange
    GET_GLUE(%ecx)
    movzwl  (rPC),rINST_FULL         # recover rINST
    movl     %eax,OUT_ARG2(%esp)     # arg2<- method
    movzwl   4(rPC),%eax             # eax<- GFED or CCCC
    SAVE_PC_TO_GLUE(%ecx)
    SAVE_FP_TO_GLUE(%ecx)
    movzbl   rINST_HI,rINST_FULL
    movl     rINST_FULL,OUT_ARG3(%esp)# arg3<- AA
    movl     %ecx,OUT_ARG0(%esp)     # arg0<- GLUE
    movl     %eax,OUT_ARG4(%esp)     # arg4<- GFED/CCCC
    call     dvmMterp_invokeMethod
    jmp      common_resumeAfterGlueCall


/*
 * Do we need the thread to be suspended or have debugger/profiling activity?
 *
 * On entry:
 *   ebx  -> PC adjustment in 16-bit words (must be preserved)
 *   ecx  -> GLUE pointer
 *
 * Note: A call will normally kill %eax, rPC/%edx and %ecx.  To
 *       streamline the normal case, this routine will preserve rPC and
 *       %ecx in addition to the normal caller save regs.  The save/restore
 *       is a bit ugly, but will happen in the relatively uncommon path.
 * TUNING: Might be worthwhile to inline this.
 * TODO: Basic-block style Jit will need a hook here as well.  Fold it into
 *       the suspendCount check so we can get both in 1 shot.
 */
common_periodicChecks:
    movl    offGlue_pSelfSuspendCount(%ecx),%eax    # eax <- &suspendCount
    cmpl    $0,(%eax)
    jne     1f

#if defined(WITH_DEBUGGER) || defined(WITH_PROFILER)
#if defined(WITH_DEBUGGER)
    movl   offGlue_pDebuggerActive(%ecx),%eax      # eax <- &DebuggerActive
#endif
#if defined(WITH_PROFILER)
    movl   offGlue_pActiveProfilers(%ecx),%ecx     # ecx <- &ActiveProfilers
#endif
#if defined(WITH_DEBUGGER) && defined(WITH_PROFILER)
    movzbl (%eax),%eax             # eax <- debuggerActive (boolean)
    orl    (%ecx),%eax             # eax <- debuggerActive || activeProfilers
#elif defined(WITH_DEBUGGER)
    movzbl (%eax),%eax             # eax <- debuggerActive (boolean)
#elif defined(WITH_PROFILER)
    movl   (%ecx),%eax             # eax <= activeProfilers
#endif
    GET_GLUE(%ecx)                 # restore rGLUE
    testl  %eax,%eax
    jne    3f                      # one or both active - switch interp
#endif

    ret

    /* Check for suspend */
1:
    /*  At this point, the return pointer to the caller of
     *  common_periodicChecks is on the top of stack.  We need to preserve
     *  rPC(edx) and GLUE(ecx).  We'll spill rPC, and reload GLUE.
     *  The outgoing profile is:
     *      bool dvmCheckSuspendPending(Thread* self)
     *  Because we reached here via a call, go ahead and build a new frame.
     */
    movl    offGlue_self(%ecx),%eax      # eax<- glue->self
    SPILL(rPC)                      # save edx
    push    %ebp
    movl    %esp,%ebp
    subl    $24,%esp
    movl    %eax,OUT_ARG0(%esp)
    call    dvmCheckSuspendPending
    addl    $24,%esp
    pop     %ebp
    UNSPILL(rPC)
    GET_GLUE(%ecx)
    ret

    /* Switch interpreters */
    /* Note: %ebx contains the 16-bit word offset to be applied to rPC to
     * "complete" the interpretation of backwards branches.  In effect, we
     * are completing the interpretation of the branch instruction here,
     * and the new interpreter will resume interpretation at the branch
     * target. However, a switch request recognized during the handling
     * of a return from method instruction results in an immediate abort,
     * and the new interpreter will resume by re-interpreting the return
     * instruction.
     */
3:
    leal    (rPC,%ebx,2),rPC       # adjust pc to show target
    GET_GLUE(%ecx)                 # bail expect GLUE already loaded
    movl    $1,rINST_FULL         # set changeInterp to true
    jmp     common_gotoBail


/*
 * Common code for handling a return instruction
 */
common_returnFromMethod:
    GET_GLUE(%ecx)
    /* Set entry mode in case we bail */
    movb    $kInterpEntryReturn,offGlue_entryPoint(%ecx)
    xorl    rINST_FULL,rINST_FULL   # zero offset in case we switch interps
    call    common_periodicChecks   # Note: expects %ecx to be preserved

    SAVEAREA_FROM_FP(%eax,rFP)                    # eax<- saveArea (old)
    movl    offStackSaveArea_prevFrame(%eax),rFP  # rFP<- prevFrame
    movl    (offStackSaveArea_method-sizeofStackSaveArea)(rFP),rINST_FULL
    cmpl    $0,rINST_FULL                        # break?
    je      common_gotoBail    # break frame, bail out completely

    movl    offStackSaveArea_savedPc(%eax),rPC    # pc<- saveArea->savedPC
    movl    offGlue_self(%ecx),%eax               # eax<- self
    movl    rINST_FULL,offGlue_method(%ecx)  # glue->method = newSave->meethod
    movl    rFP,offThread_curFrame(%eax)     # self->curFrame = fp
    movl    offMethod_clazz(rINST_FULL),%eax # eax<- method->clazz
    FETCH_INST_WORD(3)
    movl    offClassObject_pDvmDex(%eax),%eax # eax<- method->clazz->pDvmDex
    ADVANCE_PC(3)
    movl    %eax,offGlue_methodClassDex(%ecx)
    /* not bailing - restore entry mode to default */
    movb    $kInterpEntryInstr,offGlue_entryPoint(%ecx)
    GOTO_NEXT

/*
 * Prepare to strip the current frame and "longjump" back to caller of
 * dvmMterpStdRun.
 *
 * on entry:
 *    rINST_FULL holds changeInterp
 *    ecx holds glue pointer
 *
 * expected profile: dvmMterpStdBail(MterpGlue *glue, bool changeInterp)
 */
common_gotoBail:
    SAVE_PC_TO_GLUE(%ecx)                # export state to glue
    SAVE_FP_TO_GLUE(%ecx)
    movl   %ecx,OUT_ARG0(%esp)           # glue in arg0
    movl   rINST_FULL,OUT_ARG1(%esp)     # changeInterp in arg1
    call    dvmMterpStdBail              # bail out....


/*
 * After returning from a "glued" function, pull out the updated values
 * and start executing at the next instruction.
 */
 common_resumeAfterGlueCall:
     GET_GLUE(%ecx)
     LOAD_PC_FROM_GLUE(%ecx)
     LOAD_FP_FROM_GLUE(%ecx)
     FETCH_INST()
     GOTO_NEXT

/*
 * Integer divide or mod by zero
 */
common_errDivideByZero:
    EXPORT_PC()
    movl    $.LstrArithmeticException,%eax
    movl    %eax,OUT_ARG0(%esp)
    movl    $.LstrDivideByZero,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

/*
 * Attempt to allocate an array with a negative size.
 */
common_errNegativeArraySize:
    EXPORT_PC()
    movl    $.LstrNegativeArraySizeException,%eax
    movl    %eax,OUT_ARG0(%esp)
    xorl    %eax,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

/*
 * Attempt to allocate an array with a negative size.
 */
common_errNoSuchMethod:

    EXPORT_PC()
    movl    $.LstrNoSuchMethodError,%eax
    movl    %eax,OUT_ARG0(%esp)
    xorl    %eax,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

/*
 * Hit a null object when we weren't expecting one.  Export the PC, throw a
 * NullPointerException and goto the exception processing code.
 */
common_errNullObject:
    EXPORT_PC()
    movl    $.LstrNullPointerException,%eax
    movl    %eax,OUT_ARG0(%esp)
    xorl    %eax,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

/*
 * Array index exceeds max.
 */
common_errArrayIndex:
    EXPORT_PC()
    movl    $.LstrArrayIndexException,%eax
    movl    %eax,OUT_ARG0(%esp)
    xorl    %eax,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown
/*
 * Invalid array value.
 */
common_errArrayStore:
    EXPORT_PC()
    movl    $.LstrArrayStoreException,%eax
    movl    %eax,OUT_ARG0(%esp)
    xorl    %eax,%eax
    movl    %eax,OUT_ARG1(%esp)
    SPILL(rPC)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

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
    GET_GLUE(%ecx)
    SAVE_PC_TO_GLUE(%ecx)
    SAVE_FP_TO_GLUE(%ecx)
    movl    %ecx,OUT_ARG0(%esp)
    call    dvmMterp_exceptionThrown
    jmp     common_resumeAfterGlueCall

common_abort:
    movl    $0xdeadf00d,%eax
    call     *%eax


/*
 * Strings
 */

    .section     .rodata
.LstrNullPointerException:
    .asciz    "Ljava/lang/NullPointerException;"
.LstrArithmeticException:
    .asciz  "Ljava/lang/ArithmeticException;"
.LstrDivideByZero:
    .asciz  "divide by zero"
.LstrArrayIndexException:
    .asciz  "Ljava/lang/ArrayIndexOutOfBoundsException;"
.LstrArrayStoreException:
    .asciz  "Ljava/lang/ArrayStoreException;"
.LstrNegativeArraySizeException:
    .asciz  "Ljava/lang/NegativeArraySizeException;"
.LstrInstantiationError:
    .asciz  "Ljava/lang/InstantiationError;"
.LstrClassCastException:
    .asciz  "Ljava/lang/ClassCastException;"
.LstrNoSuchMethodError:
    .asciz  "Ljava/lang/NoSuchMethodError;"
.LstrInternalError:
    .asciz  "Ljava/lang/InternalError;"
.LstrFilledNewArrayNotImpl:
    .asciz  "filled-new-array only implemented for 'int'"


