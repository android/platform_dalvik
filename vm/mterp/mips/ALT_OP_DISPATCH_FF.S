%verify "executed"
/*
 * Unlike other alt stubs, we don't want to call dvmCheckBefore() here.
 * Instead, just treat this as a trampoline to reach the real alt
 * handler (which will do the dvmCheckBefore() call.
 */
    mov     ip, rINST, lsr #8           @ ip<- extended opcode
    add     ip, ip, #256                @ add offset for extended opcodes
    GOTO_OPCODE(ip)                     @ go to proper extended handler

