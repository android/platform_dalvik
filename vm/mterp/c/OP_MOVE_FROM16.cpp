HANDLE_OPCODE($opcode /*vAA, vBBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|move%s/from16 v%d,v%d %s(v%d=%"PRIx64")",
        (INST_INST(inst) == OP_MOVE_FROM16) ? "" : "-object", vdst, vsrc1,
        kSpacing, vdst, (uint64_t)GET_REGISTER_INT(vsrc1));
    SET_REGISTER_INT(vdst, GET_REGISTER_INT(vsrc1));
    FINISH(2);
OP_END
