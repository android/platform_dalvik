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