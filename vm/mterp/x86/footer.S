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
    shll        $$2, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- offset
    subl        LOCAL0_OFFSET(%ebp), %edx       # %edx<- update &outs
    shrl        $$2, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- offset
1:
    movl        (%ecx), %ebx            # %ebx<- vCCCC
    lea         4(%ecx), %ecx           # %ecx<- &vCCCC++
    subl        $$1, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET<- LOCAL0_OFFSET--
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
    shrl        $$4, LOCAL0_OFFSET(%ebp)        # LOCAL0_OFFSET(%ebp)<- B
    je          .LinvokeArgsDone        # no args; jump to args done
    movzwl      4(rPC), %ecx            # %ecx<- GFED
    SAVEAREA_FROM_FP(%edx,rFP)          # %edx<- &StackSaveArea

   /*
    * %eax=methodToCall, %ecx=GFED, LOCAL0_OFFSET(%ebp)=count, %edx=outs
    */

.LinvokeNonRange:
    cmp         $$2, LOCAL0_OFFSET(%ebp)        # compare LOCAL0_OFFSET(%ebp) to 2
    movl        %ecx, LOCAL1_OFFSET(%ebp)       # LOCAL1_OFFSET(%ebp)<- GFED
    jl          1f                      # handle 1 arg
    je          2f                      # handle 2 args
    cmp         $$4, LOCAL0_OFFSET(%ebp)        # compare LOCAL0_OFFSET(%ebp) to 4
    jl          3f                      # handle 3 args
    je          4f                      # handle 4 args
5:
    andl        $$15, rINST_FULL        # rINST<- A
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, rINST_FULL, 4), %ecx # %ecx<- vA
    movl        %ecx, (%edx)            # *outs<- vA
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
4:
    shr         $$12, %ecx              # %ecx<- G
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vG
    movl        %ecx, (%edx)            # *outs<- vG
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
3:
    and         $$0x0f00, %ecx          # %ecx<- 0F00
    shr         $$8, %ecx               # %ecx<- F
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vF
    movl        %ecx, (%edx)            # *outs<- vF
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
2:
    and         $$0x00f0, %ecx          # %ecx<- 00E0
    shr         $$4, %ecx               # %ecx<- E
    lea         -4(%edx), %edx          # %edx<- update &outs; &outs--
    movl        (rFP, %ecx, 4), %ecx    # %ecx<- vE
    movl        %ecx, (%edx)            # *outs<- vE
    movl        LOCAL1_OFFSET(%ebp), %ecx       # %ecx<- GFED
1:
    and         $$0x000f, %ecx          # %ecx<- 000D
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
    shl         $$2, %edx               # %edx<- update offset
    SAVEAREA_FROM_FP(%eax,rFP)          # %eax<- &StackSaveArea
    subl        %edx, %eax              # %eax<- newFP; (old savearea - regsSize)
    GET_GLUE(%edx)                      # %edx<- pMterpGlue
    movl        %eax, LOCAL1_OFFSET(%ebp)       # LOCAL1_OFFSET(%ebp)<- &outs
    subl        $$sizeofStackSaveArea, %eax # %eax<- newSaveArea (stack save area using newFP)
    movl        offGlue_interpStackEnd(%edx), %edx # %edx<- glue->interpStackEnd
    movl        %edx, LOCAL2_OFFSET(%ebp)       # LOCAL2_OFFSET<- glue->interpStackEnd
    shl         $$2, %ecx               # %ecx<- update offset for outsSize
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
    testl       $$ACC_NATIVE, offMethod_accessFlags(%eax) # check for native call
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
    cmp         $$0, offThread_exception(%eax) # check for exception
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
    cmpl    $$0,(%eax)
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
    subl    $$24,%esp
    movl    %eax,OUT_ARG0(%esp)
    call    dvmCheckSuspendPending
    addl    $$24,%esp
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
    movl    $$1,rINST_FULL         # set changeInterp to true
    jmp     common_gotoBail


/*
 * Common code for handling a return instruction
 */
common_returnFromMethod:
    GET_GLUE(%ecx)
    /* Set entry mode in case we bail */
    movb    $$kInterpEntryReturn,offGlue_entryPoint(%ecx)
    xorl    rINST_FULL,rINST_FULL   # zero offset in case we switch interps
    call    common_periodicChecks   # Note: expects %ecx to be preserved

    SAVEAREA_FROM_FP(%eax,rFP)                    # eax<- saveArea (old)
    movl    offStackSaveArea_prevFrame(%eax),rFP  # rFP<- prevFrame
    movl    (offStackSaveArea_method-sizeofStackSaveArea)(rFP),rINST_FULL
    cmpl    $$0,rINST_FULL                        # break?
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
    movb    $$kInterpEntryInstr,offGlue_entryPoint(%ecx)
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
    movl    $$.LstrArithmeticException,%eax
    movl    %eax,OUT_ARG0(%esp)
    movl    $$.LstrDivideByZero,%eax
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
    movl    $$.LstrNegativeArraySizeException,%eax
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
    movl    $$.LstrNoSuchMethodError,%eax
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
    movl    $$.LstrNullPointerException,%eax
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
    movl    $$.LstrArrayIndexException,%eax
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
    movl    $$.LstrArrayStoreException,%eax
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
    movl    $$0xdeadf00d,%eax
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

