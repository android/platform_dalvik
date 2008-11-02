%verify "executed"
%verify "exception handled"
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
    bl      .L${opcode}_continue        @ make call; will return after
    add     sp, sp, #8                  @ pop stack
    cmp     r0, #0                      @ test boolean result of inline
    beq     common_exceptionThrown      @ returned false, handle exception
    FETCH_ADVANCE_INST(3)               @ advance rPC, load rINST
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
    GOTO_OPCODE(ip)                     @ jump to next instruction
%break

    /*
     * Extract args, call function.
     *  r0 = #of args (0-4)
     *  r10 = call index
     *  lr = return addr, above  [DO NOT bl in here w/o preserving LR]
     *
     * Other ideas:
     * - Use a jump table from the main piece to jump directly into the
     *   AND/LDR pairs.  Costs a data load, saves a branch.
     * - Have five separate pieces that do the loading, so we can work the
     *   interleave a little better.  Increases code size.
     */
.L${opcode}_continue:
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
    @b       dvmPerformInlineOp4Std
    ldr     r9, .L${opcode}_table       @ table of InlineOperation
#ifndef __ARM_ARCH_4__
    ldr     pc, [r9, r10, lsl #4]       @ sizeof=16, "func" is first entry
#else
    ldr     ip, [r9, r10, lsl #4]       @ sizeof=16, "func" is first entry
    bx      ip
#endif
    @ (not reached)

.L${opcode}_table:
    .word   gDvmInlineOpsTable

