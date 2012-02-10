%default {"volatile":"0"}
%verify "executed"
%verify "field already resolved"
%verify "field not yet resolved"
%verify "field cannot be resolved"
    /*
     * Jumbo 64-bit SGET handler.
     */
    /* sget-wide/jumbo vBBBB, field@AAAAAAAA */
    LOAD_rSELF_methodClassDex(a2)          #  a2 <- DvmDex
    FETCH(a0, 1)                           # a0<- aaaa (lo)
    FETCH(a1, 2)                           # a1<- AAAA (hi)
    LOAD_base_offDvmDex_pResFields(a2, a2) #  a2 <- dvmDex->pResFields
    sll       a1,a1,16
    or        a1, a0, a1                 # a1<- AAAAaaaa
    LOAD_eas2(a0, a2, a1)                #  a0 <- resolved StaticField ptr
    # is resolved entry null?
    bnez      a0, .L${opcode}_finish

    /*
     * Continuation if the field has not yet been resolved.
     *  a1:  AAAAAAAA field ref
     *
     * Returns StaticField pointer in v0.
     */
    LOAD_rSELF_method(a2)                  #  a2 <- current method
    EXPORT_PC()                            #  resolve() could throw, so export now
    LOAD_base_offMethod_clazz(a0, a2)      #  a0 <- method->clazz
    JAL(dvmResolveStaticField)             #  a0 <- resolved StaticField ptr
    move      a0, v0
    # success?
    beqz      v0, common_exceptionThrown   #  no, handle exception
    b        .L${opcode}_finish            # resume
%break

.L${opcode}_finish:
    FETCH(a1, 3)                           # a1<- BBBB
    .if $volatile
    vLOAD64_off(a2, a3, a0, offStaticField_value) #  a2/a3 <- field value (aligned)
    .else
    LOAD64_off(a2, a3, a0, offStaticField_value) #  a2/a3 <- field value (aligned)
    .endif
    FETCH_ADVANCE_INST(4)                  #  advance rPC, load rINST
    EAS2(a1, rFP, a1)                      #  a1 <- &fp[BBBB]
    STORE64(a2, a3, a1)                    #  vBBBB/vBBBB+1 <- a2/a3
    GET_INST_OPCODE(t0)                    #  extract opcode from rINST
    GOTO_OPCODE(t0)                        #  jump to next instruction
