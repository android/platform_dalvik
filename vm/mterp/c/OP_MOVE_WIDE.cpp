HANDLE_OPCODE(OP_MOVE_WIDE /*vA, vB*/)
    /* IMPORTANT: must correctly handle overlapping registers, e.g. both
     * "move-wide v6, v7" and "move-wide v7, v6" */
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|move-wide v%d,v%d %s(v%d=0x%08"PRIx64")", (u4)vdst, (u4)vsrc1,
        kSpacing+5, (u4)vdst, (u8)GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(1);
OP_END
