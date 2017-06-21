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

package com.android.dx.io.instructions;

import com.android.dx.io.IndexType;

/** A decoded invoke-polymorphic/range instruction. */
public class InvokePolymorphicRangeDecodedInstruction extends DecodedInstruction {

    private final int a;
    private final int registerCount;
    private final int protoIndex;

    public InvokePolymorphicRangeDecodedInstruction(
            InstructionCodec format,
            int opcode,
            int index,
            IndexType indexType,
            int a,
            int registerCount,
            int protoIndex) {
        super(format, opcode, index, indexType, 0, 0);
        if (protoIndex != (short) protoIndex) {
          throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.a = a;
        this.registerCount = registerCount;
        this.protoIndex = protoIndex;
    }

    @Override
    public int getRegisterCount() {
        return registerCount;
    }

    @Override
    public int getA() {
        return a;
    }

    @Override
    public DecodedInstruction withProtoIndex(int newIndex, int newProtoIndex) {
        return new InvokePolymorphicRangeDecodedInstruction(
                getFormat(),
                getOpcode(),
                newIndex,
                getIndexType(),
                a,
                registerCount,
                newProtoIndex);
    }

    @Override
    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException("see withProtoIndex");
    }

    @Override
    public short getProtoIndex() {
        return (short) protoIndex;
    }
}
