%default { "postbarrier":"      #  no-op ", "prebarrier":"      #  no-op " }
%verify "executed"
%verify "field already resolved"
%verify "field not yet resolved"
%verify "field cannot be resolved"
    /*
     * Jumbo 32-bit SPUT handler.
     *
     * for: sput/jumbo, sput-boolean/jumbo, sput-byte/jumbo, sput-char/jumbo,
     *      sput-short/jumbo
     */
    /* exop vBBBB, field@AAAAAAAA */
    LOAD_rSELF_methodClassDex(a2)          #  a2 <- DvmDex
    FETCH(a0, 1)                           # a0<- aaaa (lo)
    FETCH(a1, 2)                           # a1<- AAAA (hi)
    LOAD_base_offDvmDex_pResFields(rBIX, a2) #  rBIX <- dvmDex->pResFields
    sll       a1,a1,16
    or        a1, a0, a1                   # a1<- AAAAaaaa
    LOAD_eas2(a0, rBIX, a1)                #  a0 <- resolved StaticField ptr
    bnez      a0, .L${opcode}_finish       #  is resolved entry null?

    /*
     * Continuation if the field has not yet been resolved.
     *  a1:  AAAAAAAA field ref
     *  rBIX: dvmDex->pResFields
     */
    LOAD_rSELF_method(a2)                  #  a2 <- current method
#if defined(WITH_JIT)
    EAS2(rBIX, rBIX, a1)                   #  rBIX<- &dvmDex->pResFields[field]
#endif
    EXPORT_PC()                            #  resolve() may throw, so export now
    LOAD_base_offMethod_clazz(a0, a2)      #  a0 <- method->clazz
    JAL(dvmResolveStaticField)             #  v0 <- resolved StaticField ptr
    move      a0, v0
    beqz      v0, common_exceptionThrown   #  success? no, handle exception
#if defined(WITH_JIT)
    /*
     * If the JIT is actively building a trace we need to make sure
     * that the field is fully resolved before including this instruction.
     */
    JAL(common_verifyField)
#endif
    b        .L${opcode}_finish            # resume
%break

.L${opcode}_finish:
    # field ptr in a0
    FETCH(a2, 3)                           # a2<- BBBB
    FETCH_ADVANCE_INST(4)                  #  advance rPC, load rINST
    GET_VREG(a1, a2)                       #  a1 <- fp[BBBB]
    GET_INST_OPCODE(t0)                    #  extract opcode from rINST
    $prebarrier                            #  releasing store
    sw        a1, offStaticField_value(a0) #  field <- vBBBB
    $postbarrier
    GOTO_OPCODE(t0)                        #  jump to next instruction
