HANDLE_OPCODE(OP_MONITOR_ENTER /*vAA*/)
    {
        Object* obj;

        vsrc1 = INST_AA(inst);
        ILOGV("|monitor-enter v%d %s(%"PRIxPTR")",
            (u4)vsrc1, kSpacing+6, (uintptr_t)GET_REGISTER(vsrc1));
        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (!checkForNullExportPC(obj, fp, pc))
            GOTO_exceptionThrown();
        ILOGV("+ locking %p %s", obj, dvmRefExpandClazz(obj->clazz, heapBase)->descriptor);
        EXPORT_PC();    /* need for precise GC */
        dvmLockObject(self, obj);
    }
    FINISH(1);
OP_END
