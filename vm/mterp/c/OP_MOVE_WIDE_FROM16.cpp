HANDLE_OPCODE(OP_MOVE_WIDE_FROM16 /*vAA, vBBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|move-wide/from16 v%d,v%d  (v%d=0x%08"PRIx64")", (u4)vdst, (u4)vsrc1,
        (u4)vdst, (u8)GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(2);
OP_END
