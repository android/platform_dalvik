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

