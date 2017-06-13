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

package com.android.dx.rop.cst;

/**
 * Constants of type {@code MethodHandle}.
 */
public final class CstMethodHandle extends Constant {

    public static final int KIND_GETFIELD = 1;
    public static final int KIND_GETSTATIC = 2;
    public static final int KIND_PUTFIELD = 3;
    public static final int KIND_PUTSTATIC = 4;
    public static final int KIND_INVOKEVIRTUAL = 5;
    public static final int KIND_INVOKESTATIC = 6;
    public static final int KIND_INVOKESPECIAL = 7;
    public static final int KIND_NEWINVOKESPECIAL = 8;
    public static final int KIND_INVOKEINTERFACE = 9;

    public static final String [] KIND_NAMES = {
        "get-field", "get-static", "put-field", "put-static",
        "invoke-virtual", "invoke-static", "invoke-special", "invoke-constructor",
        "invoke-interface"
    };

    /** The kind of MethodHandle */
    private final int kind;

    /** {@code non-null;} the referenced constant */
    private final Constant ref;

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param kind the kind of this handle
     * @param ref {@code non-null;} the referenced field or method constant
     * @return {@code non-null;} the appropriate instance
     */
    public static CstMethodHandle make(int kind, Constant ref) {
        if (isAccessor(kind)) {
            if (!(ref instanceof CstFieldRef)) {
                throw new IllegalArgumentException("ref has wrong type: " + ref.getClass());
            }
        } else if (isInvocation(kind)) {
            if (!(ref instanceof CstBaseMethodRef)) {
                throw new IllegalArgumentException("ref has wrong type: " + ref.getClass());
            }
        } else {
            throw new IllegalArgumentException("kind is out of range: " + kind);
        }
        return new CstMethodHandle(kind, ref);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param kind the kind of this handle
     * @param ref the actual referenced constant
     */
    private CstMethodHandle(int kind, Constant ref) {
        this.kind = kind;
        this.ref = ref;
    }

    /**
     * Gets the actual constant.
     *
     * @return the value
     */
    public Constant getRef() {
        return ref;
    }

    /**
     * Gets the kind of this method handle.
     *
     * @return the kind
     */
    public int getKind() {
        return kind;
    }

    /**
     * Reports whether the method handle kind is a field accessor.
     *
     * @param kind the method handle kind
     * @return true if the method handle kind is a field accessor, false otherwise
     */
    public static boolean isAccessor(int kind) {
        switch (kind) {
            case KIND_GETFIELD:
            case KIND_GETSTATIC:
            case KIND_PUTFIELD:
            case KIND_PUTSTATIC:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reports whether the method handle is a field accessor.
     *
     * @return true if the method handle is a field accessor, false otherwise
     */
    public boolean isAccessor() {
        return isAccessor(kind);
    }

    /**
     * Reports whether the method handle kind is a method invocation.
     *
     * @param kind the method handle kind
     * @return true if the method handle kind is a method invocation, false otherwise
     */
    public static boolean isInvocation(int kind) {
        switch (kind) {
            case KIND_INVOKEVIRTUAL:
            case KIND_INVOKESTATIC:
            case KIND_INVOKESPECIAL:
            case KIND_NEWINVOKESPECIAL:
            case KIND_INVOKEINTERFACE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reports whether the method handle is a method invocation.
     *
     * @return true if the method handle is a method invocation, false otherwise
     */
    public boolean isInvocation() {
        return isInvocation(kind);
    }

    /**
     * Gets a human readable name for a method handle kind.
     *
     * @param kind the method handle kind
     * @return the string representation of the kind
     */
    public static String getKindName(final int kind) {
        final int index = kind - KIND_GETFIELD;
        return KIND_NAMES[index];
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCategory2() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected int compareTo0(Constant other) {
        CstMethodHandle otherHandle = (CstMethodHandle) other;
        if (getKind() == otherHandle.getKind()) {
            return getRef().compareTo(otherHandle.getRef());
        } else {
            return Integer.compare(getKind(), otherHandle.getKind());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "method-handle{" + toHuman() + "}";
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "method handle";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return getKindName(kind)+ "," + ref.toString();
    }
}
