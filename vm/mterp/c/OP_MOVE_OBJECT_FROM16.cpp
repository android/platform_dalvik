HANDLE_OPCODE($opcode /*vAA, vBBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|move%s/from16 v%d,v%d %s(v%d=%"PRIxPTR")",
        (INST_INST(inst) == OP_MOVE_FROM16) ? "" : "-object", vdst, vsrc1,
        kSpacing, vdst, (uintptr_t)GET_REGISTER_AS_OBJECT(vsrc1));
    SET_REGISTER_AS_OBJECT(vdst, GET_REGISTER_AS_OBJECT(vsrc1));
    FINISH(2);
OP_END
