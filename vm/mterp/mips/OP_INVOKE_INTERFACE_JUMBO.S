%default { "isrange":"0", "routine":"NoRange" }
%verify "executed"
%verify "unknown method"
%verify "null object"
    /*
     * Handle an interface method call.
     */
    /* invoke-interface/jumbo {vCCCC..v(CCCC+BBBB-1)}, meth@AAAAAAAA */
    FETCH(a2, 4)                           # a2<- CCCC
    FETCH(a0, 1)                           # a0<- aaaa (lo)
    FETCH(a1, 2)                           # a1<- AAAA (hi)
    EXPORT_PC()                            #  must export for invoke
    sll       a1,a1,16
    or        a1, a0, a1                   #  a1<- AAAAaaaa
    GET_VREG(rOBJ, a2)                     #  rOBJ <- first arg ("this")
    LOAD_rSELF_methodClassDex(a3)          #  a3 <- methodClassDex
    LOAD_rSELF_method(a2)                  #  a2 <- method
    # null obj?
    beqz      rOBJ, common_errNullObject   #  yes, fail
    LOAD_base_offObject_clazz(a0, rOBJ)      #  a0 <- thisPtr->clazz
    JAL(dvmFindInterfaceMethodInCache)     #  v0 <- call(class, ref, method, dex)
    move      a0, v0
    # failed?
    beqz      v0, common_exceptionThrown   #  yes, handle exception
    b         common_invokeMethodJumbo #  (a0=method, rOBJ="this")
