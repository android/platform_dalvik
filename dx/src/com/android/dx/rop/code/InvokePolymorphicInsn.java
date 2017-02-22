/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dx.rop.code;

import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

/**
 * An invoke-polymorphic instruction. This is a throwing instruction with
 * multiple constants.
 */
public class InvokePolymorphicInsn extends Insn {
    private static final CstString INVOKE_DESCRIPTOR =
            new CstString("([Ljava/lang/Object;)Ljava/lang/Object;");

    /** {@code non-null;} list of exceptions caught */
    private final TypeList catches;

    /**
     * {@code non-null;} method as it appears at the call site of the original
     * invoke-virtual instruction. This is used to construct the invoke method
     * to target and the call-site prototype.
     */
    private final CstMethodRef callSiteMethod;

    /**
     * {@code non-null;} method to invoke, either {@code java.lang.invoke.MethodHandle.invoke} or
     * {@code java.lang.invoke.MethodHandle.invokeExact}.
     */
    private final CstMethodRef invokeMethod;

    /**
     * {@code non-null;} the call site prototype.
     */
    private final CstProtoRef callSiteProto;

    /**
     * *
     * @param opcode
     * @param position
     * @param sources
     * @param catches
     * @param callSiteMethod
     */
    public InvokePolymorphicInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, TypeList catches,
            CstMethodRef callSiteMethod) {
        super(opcode, position, null, sources);

        if (opcode.getBranchingness() != Rop.BRANCH_THROW) {
            throw new IllegalArgumentException("bogus branchingness");
        }

        if (catches == null) {
            throw new NullPointerException("catches == null");
        }
        this.catches = catches;

        if (callSiteMethod == null) {
            throw new NullPointerException("callSiteMethod == null");
        } else if (!callSiteMethod.isSignaturePolymorphic()) {
            throw new IllegalArgumentException("callSiteMethod is not signature polymorphic");
        }

        this.callSiteMethod = callSiteMethod;
        this.invokeMethod = makeInvokeMethod(callSiteMethod);
        this.callSiteProto = makeCallSiteProto(callSiteMethod);
    }

    @Override
    public TypeList getCatches() {
        return this.catches;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitInvokePolymorphicInsn(this);
    }

    @Override
    public Insn withAddedCatch(Type type) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                getSources(), catches.withAddedType(type), getCallSiteMethod());
    }

    @Override
    public Insn withRegisterOffset(int delta) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                getSources().withOffset(delta),
                catches, getCallSiteMethod());
    }

    @Override
    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                sources, catches, getCallSiteMethod());
    }

    public CstMethodRef getCallSiteMethod() {
        return callSiteMethod;
    }

    public CstMethodRef getInvokeMethod() {
        return invokeMethod;
    }

    public CstProtoRef getCallSiteProto() {
        return callSiteProto;
    }

    @Override
    public String getInlineString() {
        return getInvokeMethod().toString() + " " +
            getCallSiteProto().toString() + " " +
            ThrowingInsn.toCatchString(catches);
    }

    private static CstMethodRef makeInvokeMethod(final CstMethodRef callSiteMethod) {
        // The name is either invoke or invokeExact. The INVOKE_DESCRIPTOR is fixed.
        CstNat cstNat = new CstNat(callSiteMethod.getNat().getName(), INVOKE_DESCRIPTOR);
        return new CstMethodRef(CstType.METHOD_HANDLE, cstNat);
    }

    private static CstProtoRef makeCallSiteProto(final CstMethodRef callSiteMethod) {
        return new CstProtoRef(callSiteMethod.getPrototype(true));
    }
}
