%default { "isrange":"0", "routine":"NoRange" }
%verify "executed"
%verify "null object"
    /*
     * Handle an optimized virtual method call.
     *
     * for: [opt] invoke-virtual-quick, invoke-virtual-quick/range
     */
    /* op vB, {vD, vE, vF, vG, vA}, class@CCCC */
    /* op vAA, {vCCCC..v(CCCC+AA-1)}, meth@BBBB */
    movzwl    4(rPC),%eax               # eax<- FEDC or CCCC
    movzwl    2(rPC),%ecx               # ecx<- BBBB
    .if     (!$isrange)
    andl      $$0xf,%eax                # eax<- C (or stays CCCC)
    .endif
    GET_VREG(%eax,%eax)                 # eax<- vC ("this" ptr)
    testl     %eax,%eax                 # null?
    je        common_errNullObject      # yep, throw exception
    movl      offObject_clazz(%eax),%eax # eax<- thisPtr->clazz
    movl      offClassObject_vtable(%eax),%eax # eax<- thisPtr->clazz->vtable
    EXPORT_PC()                         # might throw later - get ready
    movl      (%eax,%ecx,4),%eax        # eax<- vtable[BBBB]
    jmp       common_invokeMethod${routine}


