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

import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;

/**
 * Prototype reference.
 */
public class CstProtoRef extends TypedConstant {

    /** {@code non-null;} the prototype */
    private final Prototype prototype;

    public CstProtoRef(Prototype prototype) {
        this.prototype = prototype;
    }

    @Override
    public boolean isCategory2() {
        return false;
    }

    @Override
    public String typeName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected int compareTo0(Constant other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toHuman() {
        return prototype.toString();
    }

    @Override
    public Type getType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return "prototype {" + toHuman() + '}';
    }

    public Prototype getPrototype() {
        return prototype;
    }
}
