%verify "executed"
%verify "null object"
%verify "class cast exception thrown, with correct class name"
%verify "class cast exception not thrown on same class"
%verify "class cast exception not thrown on subclass"
%verify "class not resolved"
%verify "class already resolved"
    /*
     * Check to see if an object reference is an instance of a class.
     *
     * Most common situation is a non-null object, being compared against
     * an already-resolved class.
     *
     * TODO: convert most of this into a common subroutine, shared with
     *       OP_INSTANCE_OF.S.
     */
    /* instance-of/jumbo vBBBB, vCCCC, class@AAAAAAAA */
    FETCH(a3, 4)                           # a3<- vCCCC
    FETCH(rOBJ, 3)                         # rOBJ<- vBBBB
    GET_VREG(a0, a3)                       #  a0 <- vCCCC (object)
    LOAD_rSELF_methodClassDex(a2)          #  a2 <- pDvmDex
    # is object null?
    beqz      a0, .L${opcode}_store        #  null obj, not an instance, store a0
    FETCH(a1, 1)                           # r1<- aaaa (lo)
    FETCH(a3, 2)                           # r3<- AAAA (hi)
    LOAD_base_offDvmDex_pResClasses(a2, a2) #  a2 <- pDvmDex->pResClasses
    sll     a3,a3,16
    or      a3, a1, a3                     # a3<- AAAAaaaa

    LOAD_eas2(a1, a2, a3)                  #  a1 <- resolved class
    LOAD_base_offObject_clazz(a0, a0)      #  a0 <- obj->clazz
    # have we resolved this before?
    beqz      a1, .L${opcode}_resolve      #  not resolved, do it now
    b       .L${opcode}_resolved           # resolved, continue

%break

     /*
     * Class resolved, determine type of check necessary.  This is common.
     *  r0 holds obj->clazz
     *  r1 holds class resolved from AAAAAAAA
     *  r9 holds BBBB
     */

.L${opcode}_resolved:                   #  a0=obj->clazz, a1=resolved class
    # same class (trivial success)?
    beq       a0, a1, .L${opcode}_trivial  #  yes, trivial finish
    # fall through to ${opcode}_fullcheck

    /*
     * Trivial test failed, need to perform full check.  This is common.
     *  a0 holds obj->clazz
     *  a1 holds class resolved from AAAAAAAA
     *  rOBJ holds BBBB
     */
.L${opcode}_fullcheck:
    JAL(dvmInstanceofNonTrivial)           #  v0 <- boolean result
    move      a0, v0
    b         .L${opcode}_store            #  go to ${opcode}_store

.L${opcode}_trivial:
    li        a0, 1                        #  indicate success
    # fall thru
    /*
     * a0   holds boolean result
     * rOBJ holds BBBB
     */
.L${opcode}_store:
    FETCH_ADVANCE_INST(5)                  #  advance rPC, load rINST
    SET_VREG(a0, rOBJ)                     #  vBBBB <- a0
    GET_INST_OPCODE(t0)                    #  extract opcode from rINST
    GOTO_OPCODE(t0)                        #  jump to next instruction

    /*
     * Resolution required.  This is the least-likely path.
     *
     *  a3   holds AAAAAAAA
     *  rOBJ holds BBBB
     */
.L${opcode}_resolve:
    EXPORT_PC()                            #  resolve() could throw
    LOAD_rSELF_method(a0)                  #  a0 <- self->method
    move      a1, a3                       #  a1 <- AAAAAAAA
    li        a2, 1                        #  a2 <- true
    LOAD_base_offMethod_clazz(a0, a0)      #  a0 <- method->clazz
    JAL(dvmResolveClass)                   #  v0 <- resolved ClassObject ptr
    # got null?
    move      a1, v0                       #  a1 <- class resolved from BBB
    beqz      v0, common_exceptionThrown   #  yes, handle exception
    FETCH(ra, 4)                           #  a3<- vCCCC
    move       a1, a0                       #  a1<- class resolved from AAAAAAAA

    GET_VREG(a0, a3)                       #  a0 <- vCCCC (object)
    LOAD_base_offObject_clazz(a0, a0)      #  a0 <- obj->clazz
    b         .L${opcode}_resolved         #  pick up where we left off

