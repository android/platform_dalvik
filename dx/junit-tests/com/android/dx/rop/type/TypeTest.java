package com.android.dx.rop.type;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TypeTest {

    @Test
    public void testClearInternTable() {
        Type boolArray = Type.BOOLEAN.getArrayType();
        assertTrue(boolArray == Type.BOOLEAN_ARRAY);
        assertTrue(boolArray == Type.intern("[Z"));
        Type myClass = Type.intern("Lcom/example/Foo;");

        Type.clearInternTable();

        assertTrue(boolArray == Type.intern("[Z"));
        Type myClass2 = Type.intern("Lcom/example/Foo;");
        assertEquals(myClass, myClass2);
        assertFalse(myClass == myClass2);
    }
}