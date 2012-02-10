%default { "store":"sw", "postbarrier":"    #  noop", "prebarrier":"    #  noop", "sqnum":"0" }
%verify "executed"
%verify "null object"
%verify "field already resolved"
%verify "field not yet resolved"
%verify "field cannot be resolved"
    /*
     * Jumbo 32-bit instance field put.
     */
    /* iput-object/jumbo vBBBB, vCCCC, field@AAAAAAAA */
    FETCH(a1, 1)                           # a1<- aaaa (lo)
    FETCH(a2, 2)                           # a2<- AAAA (hi)
    FETCH(a0, 4)                           # a0<- CCCC
    LOAD_rSELF_methodClassDex(a3)          #  a3 <- DvmDex
    sll      a1,a1,16
    or       a1, a1, a2                    # a1<- AAAAaaaa
    LOAD_base_offDvmDex_pResFields(a2, a3) #  a2 <- pDvmDex->pResFields
    GET_VREG(rOBJ, a0)                     #  rOBJ <- fp[B], the object pointer
    LOAD_eas2(a0, a2, a1)                  #  a0 <- resolved InstField ptr
    # is resolved entry null?
    bnez      a0, .L${opcode}_finish       #  no, already resolved
    LOAD_rSELF_method(a2)                  #  a2 <- current method
    EXPORT_PC()                            #  resolve() could throw
    LOAD_base_offMethod_clazz(a0, a2)      #  a0 <- method->clazz
    JAL(dvmResolveInstField)               #  v0 <- resolved InstField ptr
    b         .L${opcode}_resolved

%break

    /*
     * Currently:
     *  a0 holds resolved field
     *  rOBJ holds object
     */
.L${opcode}_resolved:
    move      a0, v0
    beqz      a0, common_exceptionThrown
    # fall through to ${opcode}_finish

.L${opcode}_finish:
    #BAL(common_squeak${sqnum})
    LOAD_base_offInstField_byteOffset(a3, a0) #  a3 <- byte offset of field
    FETCH(a1, 3)                              # a1<- BBBB
    GET_VREG(a0, a1)                       #  a0 <- fp[BBBB]
    lw        a2, offThread_cardTable(rSELF) #  a2 <- card table base
    # check object for null
    beqz      rOBJ, common_errNullObject   #  object was null
    FETCH_ADVANCE_INST(5)                  #  advance rPC, load rINST
    GET_INST_OPCODE(t0)                    #  extract opcode from rINST
    addu      t2, rOBJ, a3                 #  form address
    $prebarrier                            #  releasing store
    $store a0, (t2)                        #  obj.field (32 bits) <- a0
    $postbarrier
    beqz      a0, 1f                       #  stored a null reference?
    srl       t1, rOBJ, GC_CARD_SHIFT
    addu      t2, a2, t1
    sb        a2, (t2)                     #  mark card if not
1:
    GOTO_OPCODE(t0)                        #  jump to next instruction

