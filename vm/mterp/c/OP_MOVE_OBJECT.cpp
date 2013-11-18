HANDLE_OPCODE($opcode /*vA, vB*/)
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|move%s v%d,v%d %s(v%d=%"PRIxPTR")",
        (INST_INST(inst) == OP_MOVE) ? "" : "-object", vdst, vsrc1,
        kSpacing, vdst, (uintptr_t)GET_REGISTER_AS_OBJECT(vsrc1));
    SET_REGISTER_AS_OBJECT(vdst, GET_REGISTER_AS_OBJECT(vsrc1));
    FINISH(1);
OP_END
