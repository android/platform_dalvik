%default { "isrange":"0" }
%verify "executed"
%verify "unimplemented array type"
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
    jne     .L${opcode}_continue              # yes, continue
    # less frequent path, so we'll redo some work
    GET_GLUE(%eax)
    movl    $$0,OUT_ARG2(%esp)                # arg2<- false
    movl    %ecx,OUT_ARG1(%esp)               # arg1<- BBBB
    movl    offGlue_method(%eax),%eax         # eax<- glue->method
    jmp     .L${opcode}_more
%break

.L${opcode}_more:
    movl    offMethod_clazz(%eax),%eax        # eax<- method->clazz
    movl    %eax,OUT_ARG0(%esp)               # arg0<- clazz
    call    dvmResolveClass                   # eax<- call(clazz,ref,flag)
    UNSPILL(rPC)
    testl   %eax,%eax                         # null?
    je      common_exceptionThrown            # yes, handle it

       # note: fall through to .L${opcode}_continue

    /*
     * On entry:
     *    eax holds array class [r0]
     *    rINST_FULL holds AA or BB [r10]
     *    ecx is scratch
     *    rPC is valid, but has been spilled
     */
.L${opcode}_continue:
    movl    offClassObject_descriptor(%eax),%ecx  # ecx<- arrayClass->descriptor
    movl    $$ALLOC_DONT_TRACK,OUT_ARG2(%esp)     # arg2<- flags
    movzbl  1(%ecx),%ecx                          # ecx<- descriptor[1]
    movl    %eax,OUT_ARG0(%esp)                   # arg0<- arrayClass
    cmpb    $$'I',%cl                             # supported?
    je      1f
    cmpb    $$'L',%cl
    je      1f
    cmpb    $$'[',%cl
    jne      .L${opcode}_notimpl                  # no, not handled yet
1:
    .if      (!$isrange)
    SPILL_TMP(rINST_FULL)                         # save copy, need "B" later
    sarl    $$4,rINST_FULL
    .endif
    movl    rINST_FULL,OUT_ARG1(%esp)             # arg1<- A or AA (length)
    call    dvmAllocArrayByClass                  # eax<- call(arrayClass, length, flags)
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

    .if $isrange
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
    andl   $$0x0f,rPC            # rPC<- 0000000A
    sall   $$16,rPC              # rPC<- 000A0000
    orl    %ecx,rPC              # rpc<- 000AFEDC
3:
    movl   $$0xf,%ecx
    andl   rPC,%ecx           # ecx<- next reg to load
    GET_VREG(%ecx,%ecx)
    shrl   $$4,rPC
    leal   4(%eax),%eax
    movl   %ecx,-4(%eax)
    sub    $$1,rINST_FULL
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
.L${opcode}_notimpl:
    movl    $$.LstrInternalError,%eax
    movl    %eax,OUT_ARG0(%esp)
    movl    $$.LstrFilledNewArrayNotImpl,%eax
    movl    %eax,OUT_ARG1(%esp)
    call    dvmThrowException
    UNSPILL(rPC)
    jmp     common_exceptionThrown

