#ifndef _LP64
%include "c/OP_MOVE_RESULT.cpp"
#else
HANDLE_OPCODE($opcode /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result%s v%d %s(v%d=%"PRIxPTR")",
         "-object",
         vdst, kSpacing+4, vdst, (uintptr_t)retval.j);
    SET_REGISTER_AS_OBJECT(vdst, retval.j);
    FINISH(1);
OP_END
#endif
