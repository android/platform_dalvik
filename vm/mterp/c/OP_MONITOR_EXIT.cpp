HANDLE_OPCODE(OP_MONITOR_EXIT /*vAA*/)
    {
        Object* obj;

        EXPORT_PC();

        vsrc1 = INST_AA(inst);
        ILOGV("|monitor-exit v%d %s(%"PRIxPTR")",
            (u4)vsrc1, kSpacing+5, (uintptr_t)GET_REGISTER(vsrc1));
        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (!checkForNull(obj)) {
            /*
             * The exception needs to be processed at the *following*
             * instruction, not the current instruction (see the Dalvik
             * spec).  Because we're jumping to an exception handler,
             * we're not actually at risk of skipping an instruction
             * by doing so.
             */
            ADJUST_PC(1);           /* monitor-exit width is 1 */
            GOTO_exceptionThrown();
        }
        ILOGV("+ unlocking %p %s", obj, dvmRefExpandClazz(obj->clazz,heapBase)->descriptor);
        if (!dvmUnlockObject(self, obj)) {
            assert(dvmCheckException(self));
            ADJUST_PC(1);
            GOTO_exceptionThrown();
        }
    }
    FINISH(1);
OP_END
