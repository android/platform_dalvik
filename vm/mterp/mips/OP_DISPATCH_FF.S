%verify "executed"
    srl     t0, rINST, 8                # t0<- extended opcode
    addu    t0, t0, 256                 # add offset for extended opcodes
    GOTO_OPCODE(t0)                     # go to proper extended handler
