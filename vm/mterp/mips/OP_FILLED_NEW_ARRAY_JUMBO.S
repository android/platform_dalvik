%default { "isrange":"0" }
%verify "executed"
%verify "unimplemented array type"
    /*
     * Create a new array with elements filled from registers.
     *
     * TODO: convert most of this into a common subroutine, shared with
     *       OP_FILLED_NEW_ARRAY.S.
     */
     /* filled-new-array/jumbo {vCCCC..v(CCCC+BBBB-1)}, type@AAAAAAAA */

    LOAD_rSELF_methodClassDex(a3)          #  a3 <- pDvmDex
    FETCH(a0, 1)                           # r0<- aaaa (lo)
    FETCH(a1, 2)                           # r1<- AAAA (hi)
    LOAD_base_offDvmDex_pResClasses(a3, a3) #  a3 <- pDvmDex->pResClasses
    sll       a1,a1,16
    or        a1, a0, a1                   # a1<- AAAAaaaa
    LOAD_eas2(a0, a3, a1)                  #  a0 <- resolved class
    GET_OPA(rOBJ)                          #  rOBJ <- AA or BA
    EXPORT_PC()                            #  need for resolve and alloc
    # already resolved?
    bnez      a0, .L${opcode}_continue     #  yes, continue on
    LOAD_rSELF_method(a3)                  #  a3 <- self->method
    li        a2, 0                        #  a2 <- false
    LOAD_base_offMethod_clazz(a0, a3)      #  a0 <- method->clazz
    JAL(dvmResolveClass)                   #  v0 <- call(clazz, ref)
    move      a0, v0
    # got null?
    beqz      v0, common_exceptionThrown   #  yes, handle exception
    b         .L${opcode}_continue
%break

    /*
     * On entry:
     *  a0 holds array class
     *  rOBJ holds AA or BA
     */
.L${opcode}_continue:
    LOAD_base_offClassObject_descriptor(a3, a0) #  a3 <- arrayClass->descriptor
    li        a2, ALLOC_DONT_TRACK         #  a2 <- alloc flags
    lbu       rINST, 1(a3)                 #  rINST <- descriptor[1]
    FETCH(a1, 3)                           # a1<- BBBB (length)
    seq       t0, rINST, 'I'               #  array of ints?
    seq       t1, rINST, 'L'               #  array of objects?
    or        t0, t1
    seq       t1, rINST, '['               #  array of arrays?
    or        t0, t1
    move      rBIX, a1                     #  save length in rBIX
    beqz      t0, .L${opcode}_notimpl      #  no, not handled yet
    JAL(dvmAllocArrayByClass)              #  v0 <- call(arClass, length, flags)
    # null return?
    beqz      v0, common_exceptionThrown   #  alloc failed, handle exception

    FETCH(a1, 4)                           #  a1 CCCC
    sw        v0, offThread_retval(rSELF)  #  retval.l <- new array
    sw        rINST, (offThread_retval+4)(rSELF) #  retval.h <- type
    addu      a0, v0, offArrayObject_contents #  a0 <- newArray->contents
    subu      rBIX, rBIX, 1                #  length--, check for neg
    FETCH_ADVANCE_INST(5)                  #  advance to next instr, load rINST
    bltz      rBIX, 2f                     #  was zero, bail

    # copy values from registers into the array
    # a0=array, a1=CCCC, t0=BBBB(length)
    move      t0, rBIX
    EAS2(a2, rFP, a1)                      #  a2 <- &fp[CCCC]
1:
    lw        a3, 0(a2)                    #  a3 <- *a2++
    addu      a2, 4
    subu      t0, t0, 1                    #  count--
    sw        a3, (a0)                     #  *contents++ = vX
    addu      a0, 4
    bgez      t0, 1b

2:
    lw        a0, offThread_retval(rSELF)  #  a0 <- object
    lw        a1, (offThread_retval+4)(rSELF) #  a1 <- type
    seq       t1, a1, 'I'                  #  Is int array?
    bnez      t1, 3f
    lw        a2, offThread_cardTable(rSELF) #  a2 <- card table base
    srl       t3, a0, GC_CARD_SHIFT
    addu      t2, a2, t3
    sb        a2, (t2)
3:
    GET_INST_OPCODE(t0)                    #  ip <- opcode from rINST
    GOTO_OPCODE(t0)                        #  execute it


    /*
     * Throw an exception indicating that we have not implemented this
     * mode of filled-new-array.
     */
.L${opcode}_notimpl:
    la        a0, .LstrFilledNewArrayNotImpl
    JAL(dvmThrowInternalError)
    b         common_exceptionThrown
