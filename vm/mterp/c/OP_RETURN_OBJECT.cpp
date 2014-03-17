#ifndef _LP64
%include "c/OP_RETURN.cpp"
#else
HANDLE_OPCODE($opcode /*vAA*/)
    vsrc1 = INST_AA(inst);
    ILOGV("|return%s v%d",
        "-object", vsrc1);
    retval.j = GET_REGISTER(vsrc1);
    GOTO_returnFromMethod();
OP_END
#endif
