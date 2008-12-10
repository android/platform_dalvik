%verify "executed"
%verify "field already resolved"
%verify "field not yet resolved"
%verify "field cannot be resolved"
    /*
     * 64-bit SPUT handler.
     */
    /* sput-wide vAA, field@BBBB */
    ldr     r2, [rGLUE, #offGlue_methodClassDex]    @ r2<- DvmDex
    FETCH(r1, 1)                        @ r1<- field ref BBBB
    ldr     r2, [r2, #offDvmDex_pResFields] @ r2<- dvmDex->pResFields
    mov     r9, rINST, lsr #8           @ r9<- AA
    ldr     r0, [r2, r1, lsl #2]        @ r0<- resolved StaticField ptr
    add     r9, rFP, r9, lsl #2         @ r9<- &fp[AA]
    cmp     r0, #0                      @ is resolved entry null?
    beq     .L${opcode}_resolve         @ yes, do resolve
.L${opcode}_finish: @ field ptr in r0, AA in r9
    FETCH_ADVANCE_INST(2)               @ advance rPC, load rINST
    ldmia   r9, {r2-r3}                 @ r2/r3<- vAA/vAA+1
    GET_INST_OPCODE(ip)                 @ extract opcode from rINST
#ifdef __ARM_HAVE_PAIR_LOAD_STORE
    strd    r2, [r0, #offStaticField_value] @ field<- vAA/vAA+1
#else
    str     r2, [r0, #offStaticField_value] @ field<- vAA/vAA+1
    str     r3, [r0, #(offStaticField_value+4)] @ field<- vAA/vAA+1
#endif
    GOTO_OPCODE(ip)                     @ jump to next instruction
%break

    /*
     * Continuation if the field has not yet been resolved.
     *  r1: BBBB field ref
     *  r9: &fp[AA]
     */
.L${opcode}_resolve:
    ldr     r2, [rGLUE, #offGlue_method]    @ r2<- current method
    EXPORT_PC()                         @ resolve() could throw, so export now
    ldr     r0, [r2, #offMethod_clazz]  @ r0<- method->clazz
    bl      dvmResolveStaticField       @ r0<- resolved StaticField ptr
    cmp     r0, #0                      @ success?
    bne     .L${opcode}_finish          @ yes, finish
    b       common_exceptionThrown      @ no, handle exception

