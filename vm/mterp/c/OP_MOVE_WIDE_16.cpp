HANDLE_OPCODE(OP_MOVE_WIDE_16 /*vAAAA, vBBBB*/)
    vdst = FETCH(1);
    vsrc1 = FETCH(2);
    ILOGV("|move-wide/16 v%d,v%d %s(v%d=0x%08"PRIx64")", (u4)vdst, (u4)vsrc1,
        kSpacing+8, (u4)vdst, (u8)GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(3);
OP_END
