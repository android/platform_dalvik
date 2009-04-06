   /* Copyright (C) 2008 The Android Open Source Project
    *
    * Licensed under the Apache License, Version 2.0 (the "License");
    * you may not use this file except in compliance with the License.
    * You may obtain a copy of the License at
    *
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
#if defined(WITH_DEBUGGER)
    movl        offGlue_pDebuggerActive(%edx), %eax # %eax<- pDebuggerActive
    movl        (%eax), %eax            # %eax<- get debuggerActive (boolean)
    and         $$7, %eax               # %eax<- mask for boolean (just how many bits does it take?)
#endif
    cmp         $$0, (%ebx)             # check if suspend is pending
    jne         2f                      # handle suspend
#if defined(WITH_DEBUGGER) || defined(WITH_PROFILER)
#if defined(WITH_PROFILER)
    movl        offGlue_pActiveProfilers(%edx), %ebx # %ebx<- activeProfilers (int)
    or          (%ebx), %eax            # %eax<- merge activeProfilers and debuggerActive
#else
    cmp         $$0, %eax               # check for debuggerActive
#endif
    jne         3f                      # debugger or profiler active; switch interp
#endif
    movl        -8(%esp), %edx          # %edx<- restore %edx
    movl        -4(%esp), %ebx          # %ebx<- restore %ebx
    ret                                 # return
2:                                      # check suspended
    movl        offGlue_self(%edx), %eax # %eax<- glue->self
    movl        %eax, -12(%esp)         # push parameter boolean
    lea         -12(%esp), %esp
    call        dvmCheckSuspendPending  # call: (Thread* self)
                                        # return: bool
    movl        4(%esp), %edx           # %edx<- restore %edx
    movl        8(%esp), %ebx           # %ebx<- restore %ebx
    lea         12(%esp), %esp
    ret                                 # return
3:                                      # debugger/profiler enabled, bail out
    add         -8(%esp), rPC           # rPC<- pc adjustments
    movl        %ecx, offGlue_entryPoint(%edx) # glue->entryPoint<- reentry type
    movl        $$1, %edx               # switch to interp == true
    jmp         common_gotoBail         # bail

   /*
    * Check to see if the thread needs to be suspended or debugger/profiler
    * activity has begun. With this variant, the reentry type is hard coded
    * as kInterpEntryInstr.
    *
    * On entry:
    *  %edx is PC adjustment in bytes
    */

common_periodicChecks2:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_pSelfSuspendCount(%ecx), rINST # %ebx<- pSuspendCount (int)
#if defined(WITH_DEBUGGER)
    movl offGlue_pDebuggerActive(%ecx), %eax # %eax<- pDebuggerActive
    movl        (%eax), %eax            # %eax<- get debuggerActive (boolean)
    and         $$7, %eax               # %eax<- mask for boolean (just how many bits does it take?)
#endif
    cmp         $$0, (rINST)            # check if suspend is pending
    jne         2f                      # handle suspend
#if defined(WITH_DEBUGGER) || defined(WITH_PROFILER)
#if defined(WITH_PROFILER)
    movl        offGlue_pActiveProfilers(%ecx), rINST # %edx<- activeProfilers (int)
    or          (rINST), %eax           # %eax<- merge activeProfilers and debuggerActive
#else
    cmp         $$0, %eax               # check for debuggerActive
#endif
    jne         3f                      # debugger or profiler active; switch interp
#endif
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
    FINISH_RB   %edx, %ecx              # jump to next instruction

3:                                      # debugger/profiler enabled, bail out
    add         -8(%esp), rPC           # rPC<- pc adjustments
    movl        $$kInterpEntryInstr, offGlue_entryPoint(%ecx) # glue->entryPoint<- reentry type
    movl        $$1, %edx               # switch to interp<- true
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
    shll        $$2, sReg0              # sReg0<- offset
    subl        sReg0, %eax             # %eax<- update &outs
    shrl        $$2, sReg0              # sReg0<- offset
1:
    movl        (%edx), %ebx            # %ebx<- vCCCC
    lea         4(%edx), %edx           # %edx<- &vCCCC++
    subl        $$1, sReg0              # sReg<- sReg--
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
    shrl        $$4, sReg0              # sReg0<- B
    je          .LinvokeArgsDone        # no args; jump to args done
    SAVEAREA_FROM_FP %eax               # %eax<- &outs; &StackSaveArea
    FETCH       2, %edx                 # %edx<- GFED

   /*
    * %ecx=methodToCall, %edx=GFED, sReg0=count, %eax=outs
    */

.LinvokeNonRange:
    cmp         $$2, sReg0              # compare sReg0 to 2
    movl        %edx, sReg1             # sReg1<- GFED
    jl          1f                      # handle 1 arg
    je          2f                      # handle 2 args
    cmp         $$4, sReg0              # compare sReg0 to 4
    jl          3f                      # handle 3 args
    je          4f                      # handle 4 args
5:
    andl        $$15, rINST             # rINST<- A
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, rINST, 4), %edx   # %edx<- vA
    movl        %edx, (%eax)            # *outs<- vA
    movl        sReg1, %edx             # %edx<- GFED
4:
    shr         $$12, %edx              # %edx<- G
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx, 4), %edx    # %edx<- vG
    movl        %edx, (%eax)            # *outs<- vG
    movl        sReg1, %edx             # %edx<- GFED
3:
    and         $$0x0f00, %edx          # %edx<- 0F00
    shr         $$6, %edx               # %edx<- F at correct offset
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx), %edx       # %edx<- vF
    movl        %edx, (%eax)            # *outs<- vF
    movl        sReg1, %edx             # %edx<- GFED
2:
    and         $$0x00f0, %edx          # %edx<- 00E0
    shr         $$2, %edx               # %edx<- E at correct offset
    lea         -4(%eax), %eax          # %eax<- update &outs; &outs--
    movl        (rFP, %edx), %edx       # %edx<- vE
    movl        %edx, (%eax)            # *outs<- vE
    movl        sReg1, %edx             # %edx<- GFED
1:
    and         $$0x000f, %edx          # %edx<- 000D
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
    shl         $$2, %eax               # %eax<- update offset
    SAVEAREA_FROM_FP %ecx               # %ecx<- &outs; &StackSaveArea
    subl        %eax, %ecx              # %ecx<- newFP; (old savearea - regsSize)
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        %ecx, sReg1             # sReg1<- &outs
    subl        $$sizeofStackSaveArea, %ecx # %ecx<- newSaveArea (stack save area using newFP)
    movl        offGlue_interpStackEnd(%eax), %eax # %eax<- glue->interpStackEnd
    movl        %eax, sReg2             # sReg2<- glue->interpStackEnd
    shl         $$2, %edx               # %edx<- update offset for outsSize
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
    testl       $$ACC_NATIVE, offMethod_accessFlags(%ecx) # check for native call
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
    cmp         $$0, offThread_exception(%ecx) # check for exception
    movl        rFP, offThread_curFrame(%ecx) # glue->self->curFrame<- rFP
    movl        %eax, offThread_jniLocal_topCookie(%ecx) # glue->self<- newSaveArea->localRefCookie
    jne         common_exceptionThrown  # handle exception
    FGETOP_JMP  3, %edx                 # jump to next instruction; getop, jmp

.LstackOverflow:
    movl        rGLUE, %ecx             # %ecx<- pMterpGlue
    movl        offGlue_self(%ecx), %ecx # %ecx<- glue->self
    movl        %ecx, -4(%esp)          # push parameter self
    lea         -4(%esp), %esp
    call        dvmHandleStackOverflow  # call: (Thread* self)
                                        # return: void
    lea         4(%esp), %esp
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
#if defined(WITH_DEBUGGER)
    movl        offGlue_pDebuggerActive(rINST), %eax # %eax<- pDebuggerActive
    movl        (%eax), %eax            # %eax<- get debuggerActive (boolean)
    and         $$7, %eax               # %eax<- mask for boolean (just how many bits does it take?)
#endif
    cmp         $$0, (%edx)             # check if suspend is pending
    jne         2f                      # handle suspend
#if defined(WITH_DEBUGGER) || defined(WITH_PROFILER)
#if defined(WITH_PROFILER)
    movl        offGlue_pActiveProfilers(rINST), %edx # %edx<- activeProfilers (int)
    or          (%edx), %eax            # %eax<- merge activeProfilers and debuggerActive
#else
    cmp         $$0, %eax               # check for debuggerActive
#endif
    jne         3f                      # debugger or profiler active; switch interp
#endif
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
    movl        $$kInterpEntryInstr, offGlue_entryPoint(rINST) # glue->entryPoint<- reentry type
    movl        $$1, %edx               # switch to interp<- true
    jmp         common_gotoBail         # bail


   /*
    * Get save area; rGLUE is %ebx, rFP is %eax
    */
4:
    SAVEAREA_FROM_FP %ecx               # %ecx<- saveArea(old)
    movl        offStackSaveArea_prevFrame(%ecx), rFP # rFP<- saveArea->PrevFrame
    movl        (offStackSaveArea_method - sizeofStackSaveArea)(rFP), %edx # %edx<- method we are returning to
    cmpl        $$0, %edx               # check for break frame
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
    movl        $$kInterpEntryThrow, %ecx # %ecx<- reentry type
    movl        $$0, %edx               # %edx<- pc adjustment
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
    movl        $$0, offThread_exception(%edx) # glue->self->exception<- NULL

   /*
    * set up args and a local for &fp
    */

    movl        rFP, -4(%esp)           # move fp to stack
    lea         -4(%esp), %esp          # update %esp
    movl        %esp, -4(%esp)          # push parameter 4<- &fp
    movl        $$0, -8(%esp)           # push parameter 3<- false
    movl        4(%esp), %edx
    movl        %edx, -12(%esp)         # push parameter 2<- glue->self->exception
    movl        rGLUE, %eax             # %eax<- pMterpGlue
    movl        offGlue_method(%eax), %edx # %edx<- glue->method
    movl        offMethod_insns(%edx), %edx # %edx<- glue->method->insns
    movl        rPC, %ecx               # %ecx<- rPC
    subl        %edx, %ecx              # %ecx<- pc - glue->method->insns
    sar         $$1, %ecx               # %ecx<- adjust %ecx for offset
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
    cmp         $$0, %eax               # check for catchRelPc < 0
    jl          .LnotCaughtLocally      # handle not caught locally

   /*
    * fix stack overflow if necessary
    */

    movl        -4(%esp), %ecx          # %ecx<- glue->self
    cmp         $$0, offThread_stackOverflowed(%ecx)
    je          1f
    movl        %eax, -4(%esp)          # save %eax for later
    movl        %ecx, -12(%esp)         # push parameter 2 glue->self
    lea         -12(%esp), %esp
    call        dvmCleanupStackOverflow # call: (Thread* self)
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
    cmp         $$OP_MOVE_EXCEPTION, %eax # is it a move exception
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
    cmp         $$0, %eax               # check for stack overflow;
                                        # maybe should use cmpb
    je          1f                      #
    movl        %edx, -12(%esp)         # push parameter 1 glue->self
    lea         -12(%esp), %esp
    call        dvmCleanupStackOverflow # call: (Thread* self)
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
    movl        $$0, %edx               # switch to interp<- false
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
.word 0xdeadf00d

   /*
    * Invalid array index.
    */

common_errArrayIndex:
    EXPORT_PC
    movl        $$.LstrArrayIndexException, -8(%esp) # push parameter description
    movl        $$0, -4(%esp)           # push parameter msg paramter
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
    movl        $$.LstrArrayStoreException, -8(%esp) # push parameter description
    movl        $$0, -4(%esp)           # push parameter msg paramter
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
    movl        $$.LstrArithmeticException, -8(%esp) # push parameter description
    movl        $$.LstrDivideByZero, -4(%esp) # push parameter msg paramter
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
    movl        $$.LstrNegativeArraySizeException, -8(%esp) # push parameter description
    movl        $$0, -4(%esp)           # push parameter msg paramter
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
    movl        $$.LstrNoSuchMethodError, -8(%esp) # push parameter description
    movl        $$0, -4(%esp)           # push parameter msg paramter
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
    movl        $$.LstrNullPointerException, -8(%esp) # push parameter description
    movl        $$0, -4(%esp)           # push parameter msg paramter
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
