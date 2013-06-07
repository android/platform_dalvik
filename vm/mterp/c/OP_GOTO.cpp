HANDLE_OPCODE(OP_GOTO /*+AA*/)
    vdst = INST_AA(inst);
    if ((s1)vdst < 0)
        ILOGV("|goto -0x%02x", -((s1)vdst));
    else
        ILOGV("|goto +0x%02x", ((s1)vdst));
    ILOGV("> branch taken");
    FINISH((s1)vdst);
OP_END
