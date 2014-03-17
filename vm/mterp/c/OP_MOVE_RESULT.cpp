HANDLE_OPCODE($opcode /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result%s v%d %s(v%d=0x%08"PRIxPTR")",
         (INST_INST(inst) == OP_MOVE_RESULT) ? "" : "-object",
         vdst, kSpacing+4, vdst, (uintptr_t)retval.j);
    SET_REGISTER_INT(vdst, retval.i);
    FINISH(1);
OP_END
