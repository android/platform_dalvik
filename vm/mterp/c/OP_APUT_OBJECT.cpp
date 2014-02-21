HANDLE_OPCODE(OP_APUT_OBJECT /*vAA, vBB, vCC*/)
    {
        ArrayObject* arrayObj;
        Object* obj;
        u2 arrayInfo;
        EXPORT_PC();
        vdst = INST_AA(inst);       /* AA: source value */
        arrayInfo = FETCH(1);
        vsrc1 = arrayInfo & 0xff;   /* BB: array ptr */
        vsrc2 = arrayInfo >> 8;     /* CC: index */
        ILOGV("|aput%s v%d,v%d,v%d", "-object", vdst, vsrc1, vsrc2);
        arrayObj = (ArrayObject*) GET_REGISTER(vsrc1);
        if (!checkForNull((Object*) arrayObj))
            GOTO_exceptionThrown();
        if (GET_REGISTER(vsrc2) >= arrayObj->length) {
            dvmThrowArrayIndexOutOfBoundsException(
                arrayObj->length, GET_REGISTER(vsrc2));
            GOTO_exceptionThrown();
        }
        obj = (Object*) GET_REGISTER(vdst);
        if (obj != NULL) {
            if (!checkForNull(obj))
                GOTO_exceptionThrown();
            if (!dvmCanPutArrayElement(dvmRefExpandClazz(obj->clazz, heapBase),
                    dvmRefExpandClazz(arrayObj->clazz, heapBase))) {
                ALOGV("Can't put a '%s'(%p) into array type='%s'(%p)",
                        dvmRefExpandClazz(obj->clazz, heapBase)->descriptor, obj,
                        dvmRefExpandClazz(arrayObj->clazz, heapBase)->descriptor, arrayObj);
                dvmThrowArrayStoreExceptionIncompatibleElement(dvmRefExpandClazz(obj->clazz, heapBase),
                        dvmRefExpandClazz(arrayObj->clazz,heapBase));
                GOTO_exceptionThrown();
            }
        }
        ILOGV("+ APUT[%d]="PRIxPTR, (u4)GET_REGISTER(vsrc2), GET_REGISTER_AS_OBJECT(vdst));
        dvmSetObjectArrayElement(arrayObj,
                                 GET_REGISTER(vsrc2),
                                 (Object *)GET_REGISTER(vdst));
    }
    FINISH(2);
OP_END
