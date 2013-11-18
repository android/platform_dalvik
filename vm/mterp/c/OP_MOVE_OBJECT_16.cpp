HANDLE_OPCODE($opcode /*vAAAA, vBBBB*/)
    vdst = FETCH(1);
    vsrc1 = FETCH(2);
    ILOGV("|move%s/16 v%d,v%d %s(v%d=%"PRIxPTR")",
        (INST_INST(inst) == OP_MOVE_16) ? "" : "-object", vdst, vsrc1,
        kSpacing, vdst, (uintptr_t)GET_REGISTER_AS_OBJECT(vsrc1));
    SET_REGISTER_AS_OBJECT(vdst, GET_REGISTER_AS_OBJECT(vsrc1));
    FINISH(3);
OP_END
