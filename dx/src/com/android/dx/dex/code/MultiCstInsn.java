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

package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.cst.Constant;

/**
 * Instruction which has multiple constant argument in addition
 * to all the normal instruction information.
 */
public final class MultiCstInsn extends FixedSizeInsn {
    private static final int NOT_SET = -1;

    /** {@code non-null;} the constant argument for this instruction */
    private final Constant[] constants;

    /**
     * {@code >= NOT_SET;} the constant pool indicies for {@link #constant}, or
     * {@code NOT_SET} if not yet set
     */
    private final int[] index;

    /**
     * {@code >= NOT_SET;} the constant pool index for the class reference in
     * {@link #constant} if any, or {@code NOT_SET} if not yet set
     */
    private int classIndex;

    /**
     * Constructs an instance. The output address of this instance is
     * initially unknown ({@code -1}) as is the constant pool index.
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins or outs)
     * @param constants {@code non-null;} constants argument
     */
    public MultiCstInsn(Dop opcode, SourcePosition position,
                        RegisterSpecList registers, Constant [] constants) {
        super(opcode, position, registers);

        if (constants == null) {
            throw new NullPointerException("constants == null");
        }

        this.constants = constants;
        this.index = new int[constants.length];
        for (int i = 0; i < this.index.length; ++i) {
            if (constants[i] == null) {
                throw new NullPointerException("constants[i] == null");
            }
            this.index[i] = NOT_SET;
        }
        this.classIndex = NOT_SET;
    }

    private MultiCstInsn(Dop opcode, SourcePosition position,
            RegisterSpecList registers, Constant[] constants, int[] index,
            int classIndex) {
        super(opcode, position, registers);
        this.constants = constants;
        this.index = index;
        this.classIndex = classIndex;
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withOpcode(Dop opcode) {
        return new MultiCstInsn(opcode, getPosition(), getRegisters(),
                this.constants, this.index, this.classIndex);
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new MultiCstInsn(getOpcode(), getPosition(), registers,
                this.constants, this.index, this.classIndex);
    }

    /**
     * Gets the number of constants associated with instruction.
     * @return the number of constants.
     */
    public int getNumberOfConstants() {
        return constants.length;
    }

    /**
     * Gets the constant argument.
     *
     * @return {@code non-null;} the constant argument
     */
    public Constant getConstant(int i) {
        return constants[i];
    }

    /**
     * Gets the constant's index. It is only valid to call this after
     * {@link #setIndex} has been called.
     *
     * @return {@code >= 0;} the constant pool index
     */
    public int getIndex(int i) {
        if (index[i] == NOT_SET) {
            throw new RuntimeException("index not yet set for constant " + i +
                    " value = " + constants[i]);
        }

        return index[i];
    }

    /**
     * Returns whether the constants index has been set for this instance.
     *
     * @see #setIndex
     *
     * @return {@code true} if the index has been set
     */
    public boolean hasIndex(int i) {
        return (index[i] != NOT_SET);
    }

    /**
     * Sets the constant's index. It is only valid to call this method once
     * per instance.
     *
     * @param index {@code >= 0;} the constant pool index
     */
    public void setIndex(int i, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (this.index[i] != NOT_SET) {
            throw new RuntimeException("index already set");
        }

        this.index[i] = index;
    }

    /**
     * Gets the constant's class index. It is only valid to call this after
     * {@link #setClassIndex} has been called.
     *
     * @return {@code >= 0;} the constant's class's constant pool index
     */
    public int getClassIndex() {
        if (classIndex == NOT_SET) {
            throw new RuntimeException("class index not yet set");
        }

        return classIndex;
    }

    /**
     * Returns whether the constant's class index has been set for this
     * instance.
     *
     * @see #setClassIndex
     *
     * @return {@code true} iff the index has been set
     */
    public boolean hasClassIndex() {
        return (classIndex != NOT_SET);
    }

    /**
     * Sets the constant's class index. This is the constant pool index
     * for the class referred to by this instance's constant. Only
     * reference constants have a class, so it is only on instances
     * with reference constants that this method should ever be
     * called. It is only valid to call this method once per instance.
     *
     * @param index {@code >= 0;} the constant's class's constant pool index
     */
    public void setClassIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (this.classIndex != NOT_SET) {
            throw new RuntimeException("class index already set");
        }

        this.classIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < constants.length; ++i) {
            if (sb.length() > 0) sb.append(",");
            sb.append(constants[i].toHuman());
        }
        return sb.toString();
    }
}
