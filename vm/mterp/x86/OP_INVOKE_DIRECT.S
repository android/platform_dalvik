%default { "isrange":"0", "routine":"NoRange" }
%verify "executed"
%verify "unknown method"
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
    .if       (!$isrange)
    andl      $$0xf,rPC                # rPC<- D (or stays CCCC)
    .endif
    testl     %eax,%eax                # already resolved?
    GET_VREG(%ecx,rPC)                 # ecx<- "this" ptr
    je        .L${opcode}_resolve      # not resolved, do it now
.L${opcode}_finish:
    UNSPILL(rPC)
    testl     %ecx,%ecx                # null "this"?
    jne       common_invokeMethod${routine}  # no, continue on
    jmp       common_errNullObject
%break

    /*
     * On entry:
     *   TMP_SPILL  <- "this" register
     * Things a bit ugly on this path, but it's the less
     * frequent one.  We'll have to do some reloading.
     */
.L${opcode}_resolve:
     SPILL_TMP(%ecx)
     GET_GLUE(%ecx)
     UNSPILL(rPC)
     movl     offGlue_method(%ecx),%ecx  # ecx<- glue->method
     movzwl   2(rPC),%eax      # reference (BBBB or CCCC)
     movl     offMethod_clazz(%ecx),%ecx # ecx<- method->clazz
     movl     $$METHOD_DIRECT,OUT_ARG2(%esp)
     movl     %eax,OUT_ARG1(%esp)
     movl     %ecx,OUT_ARG0(%esp)
     call     dvmResolveMethod # eax<- call(clazz, ref, flags)
     UNSPILL_TMP(%ecx)
     testl    %eax,%eax
     jne      .L${opcode}_finish
     UNSPILL(rPC)
     jmp      common_exceptionThrown
