HANDLE_OPCODE(OP_MOVE_RESULT_WIDE /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result-wide v%d %s(0x%08"PRIx64")", (u4)vdst, kSpacing, (u8)retval.j);
    SET_REGISTER_WIDE(vdst, retval.j);
    FINISH(1);
OP_END
