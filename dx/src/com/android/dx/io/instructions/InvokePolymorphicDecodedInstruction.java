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
import java.util.List;

/** A decoded invoke-polymorphic instruction. */
public class InvokePolymorphicDecodedInstruction extends DecodedInstruction {

    private final int protoIndex;
    private final List<Integer> registers;

    public InvokePolymorphicDecodedInstruction(
            InstructionCodec format,
            int opcode,
            int index,
            IndexType indexType,
            int protoIndex,
            List<Integer> registers) {
        super(format, opcode, index, indexType, 0, 0);
        if (protoIndex != (short) protoIndex) {
          throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.protoIndex = protoIndex;
        this.registers = registers;
    }

    @Override
    public int getRegisterCount() {
        return registers.size();
    }

    @Override
    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException("see withProtoIndex");
    }

    @Override
    public DecodedInstruction withProtoIndex(int newIndex, int newProtoIndex) {
        return new InvokePolymorphicDecodedInstruction(
                getFormat(), getOpcode(), newIndex, getIndexType(), newProtoIndex, registers);
    }

    @Override
    public int getA() {
        return registers.size() > 0 ? registers.get(0) : 0;
    }

    @Override
    public int getB() {
        return registers.size() > 1 ? registers.get(1) : 1;
    }

    @Override
    public int getC() {
        return registers.size() > 2 ? registers.get(2) : 2;
    }

    @Override
    public int getD() {
        return registers.size() > 3 ? registers.get(3) : 3;
    }

    @Override
    public int getE() {
        return registers.size() > 4 ? registers.get(4) : 4;
    }

    @Override
    public short getProtoIndex() {
        return (short) protoIndex;
    }
}
