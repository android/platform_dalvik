%verify executed
    /*
     * Handle a jumbo throw-verification-error instruction.  This throws an
     * exception for an error discovered during verification.  The
     * exception is indicated by BBBB, with some detail provided by AAAAAAAA.
     */
     /* exop BBBB, Class@AAAAAAAA */
    FETCH(a1, 1)                           # a1<- aaaa (lo)
    FETCH(a2, 2)                           # a2<- AAAA (hi)
    LOAD_rSELF_method(a0)                  #  a0 <- self->method
    sll    a2,a2,16
    or     a2, a1, a2                      # a2<- AAAAaaaa
    EXPORT_PC()                            #  export the PC
    FETCH(a1, 3)                           # a1<- BBBB
    JAL(dvmThrowVerificationError)         #  always throws
    b         common_exceptionThrown       #  handle exception

