%verify "executed"
%verify "unknown method"
%verify "null object"
    /*
     * Handle a virtual method call.
     */
     /* invoke-virtual/jumbo {vCCCC..v(CCCC+BBBB-1)}, meth@AAAAAAAA */
    LOAD_rSELF_methodClassDex(a3)          #  a3 <- pDvmDex
    FETCH(a0, 1)                           # a0<- aaaa (lo)
    FETCH(a1, 2)                           # a1<- AAAA (hi)
    LOAD_base_offDvmDex_pResMethods(a3, a3) #  a3 <- pDvmDex->pResMethods
    sll     a1,a1,16
    or      a1, a0, a1                     # a1<- AAAAaaaa
    LOAD_eas2(a0, a3, a1)                  #  a0 <- resolved baseMethod
    EXPORT_PC()                            #  must export for invoke
    # already resolved?
    bnez      a0, .L${opcode}_continue     #  yes, continue on

    LOAD_rSELF_method(a3)                  #  a3 <- self->method
    LOAD_base_offMethod_clazz(a0, a3)      #  a0 <- method->clazz
    li        a2, METHOD_VIRTUAL           #  resolver method type
    JAL(dvmResolveMethod)                  #  v0 <- call(clazz, ref, flags)
    move      a0, v0
    # got null?
    bnez      v0, .L${opcode}_continue     #  no, continue
    b         common_exceptionThrown       #  yes, handle exception
%break

    /*
     * At this point:
     *  a0 = resolved base method
     *  rBIX= C or CCCC (index of first arg, which is the "this" ptr)
     */
.L${opcode}_continue:
    FETCH(rBIX,4)                          #  rBIX <- CCCC
    GET_VREG(rOBJ, rBIX)                   #  rOBJ <- "this" ptr
    LOADu2_offMethod_methodIndex(a2, a0)   #  a2 <- baseMethod->methodIndex
    # is "this" null?
    beqz      rOBJ, common_errNullObject   #  null "this", throw exception
    LOAD_base_offObject_clazz(a3, rOBJ)    #  a3 <- thisPtr->clazz
    LOAD_base_offClassObject_vtable(a3, a3) #  a3 <- thisPtr->clazz->vtable
    LOAD_eas2(a0, a3, a2)                  #  a0 <- vtable[methodIndex]
    b         common_invokeMethodJumbo     #  (a0=method, rOBJ="this")

