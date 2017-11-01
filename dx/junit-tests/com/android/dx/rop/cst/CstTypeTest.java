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

import com.android.dx.rop.type.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CstTypeTest {


    @Test
    public void checkClearInternTable() {
        CstType boolArray = CstType.BOOLEAN_ARRAY;
        assertTrue(boolArray == CstType.intern(Type.BOOLEAN_ARRAY));
        CstType myClass = CstType.intern(Type.intern("Lcom/example/Foo;"));

        CstType.clearInternTable();
        Type.clearInternTable();

        assertTrue(boolArray == CstType.intern(Type.BOOLEAN_ARRAY));
        CstType myClass2 = CstType.intern(Type.intern("Lcom/example/Foo;"));
        assertEquals(myClass.getClassType(), myClass2.getClassType());
        assertFalse(myClass == myClass2);
    }
}