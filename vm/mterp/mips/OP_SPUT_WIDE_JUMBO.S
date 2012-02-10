%default {"volatile":"0"}
%verify "executed"
%verify "field already resolved"
%verify "field not yet resolved"
%verify "field cannot be resolved"
    /*
     * Jumbo 64-bit SPUT handler.
     */
    /* sput-wide/jumbo vBBBB, field@AAAAAAAA */
    LOAD_rSELF_methodClassDex(a2)          #  a2 <- DvmDex
    FETCH(a1, 1)                           # a1<- aaaa (lo)
    FETCH(a2, 2)                           # a2<- AAAA (hi)
    LOAD_base_offDvmDex_pResFields(rBIX, a2) #  rBIX <- dvmDex->pResFields
    sll     a2,a2,16
    or      a1, a1, a2                    # a1<- AAAAaaaa
    FETCH(rOBJ, 3)                        # rOBJ<- BBBB    solved StaticField ptr
    EAS2(rOBJ, rFP, t0)                    #  rOBJ<- &fp[BBBB]
    # is resolved entry null?
    beqz      a2, .L${opcode}_resolve      #  yes, do resolve
.L${opcode}_finish:                        #  field ptr in a2, BBBB in rOBJ
    FETCH_ADVANCE_INST(4)                  #  advance rPC, load rINST
    LOAD64(a0, a1, rOBJ)                   #  a0/a1 <- vBBBB/vBBBB+1
    GET_INST_OPCODE(rBIX)                  #  extract opcode from rINST
    .if $volatile
    addu    a2, offStaticField_value       #  a2<- pointer to data
    JAL(dvmQuasiAtomicSwap64Sync)          #  stores a0/a1 into addr a2
    .else
    STORE64_off(a0, a1, a2, offStaticField_value) #  field <- vBBBB/vBBBB+1
    .endif
    GOTO_OPCODE(rBIX)                      #  jump to next instruction
%break

    /*
     * Continuation if the field has not yet been resolved.
     *  a1:  AAAAAAAA field ref
     *  rOBJ:  &fp[BBBB]
     *  rBIX: dvmDex->pResFields
     *
     * Returns StaticField pointer in a2.
     */
.L${opcode}_resolve:
    LOAD_rSELF_method(a2)                  #  a2 <- current method
#if defined(WITH_JIT)
    EAS2(rBIX, rBIX, a1)                   #  rBIX<- &dvmDex->pResFields[field]
#endif
    EXPORT_PC()                            #  resolve() could throw, so export now
    LOAD_base_offMethod_clazz(a0, a2)      #  a0 <- method->clazz
    JAL(dvmResolveStaticField)             #  v0 <- resolved StaticField ptr
    # success ?
    move      a0, v0
    beqz      v0, common_exceptionThrown   #  no, handle exception
#if defined(WITH_JIT)
    /*
     * If the JIT is actively building a trace we need to make sure
     * that the field is fully resolved before including this instruction.
     */
    JAL(common_verifyField)
#endif
    move      a2, v0
    b         .L${opcode}_finish           # resume
