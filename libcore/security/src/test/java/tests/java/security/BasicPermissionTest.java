/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Alexey V. Varlamov
* @version $Revision$
*/

package tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.security.BasicPermission;
import java.security.PermissionCollection;

import junit.framework.TestCase;

/**
 * Tests for <code>BasicPermission</code>
 * 
 */
@TestTargetClass(BasicPermission.class)
public class BasicPermissionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BasicPermissionTest.class);
    }

    /**
     * Check all constructors: an object is created with the specified valid name. 
     * If name equal null then NPE should be thrown. 
     * If  name is empty then IAE should be thrown. 
     * Action is ignored.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "BasicPermission",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "BasicPermission",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    public void testCtor()
    {
        String name = "basic123*$%#";
        BasicPermission test = new BasicPermission(name){};
        assertEquals(name, test.getName());
        assertEquals("", test.getActions());
        test = new BasicPermission(name, "#$!#12435"){};
        assertEquals(name, test.getName());
        assertEquals("", test.getActions());
        try{
            new BasicPermission(null){};
            fail("NPE is not thrown");
        }
        catch (NullPointerException ok){}
        
        try{
            new BasicPermission(null, "ds235"){};
            fail("NPE is not thrown");
        }
        catch (NullPointerException ok){}
        
        try{
            new BasicPermission(""){};
            fail("IAE is not thrown");
        }
        catch (IllegalArgumentException ok){}
        try{
            new BasicPermission("", "ertre 3454"){};
            fail("IAE is not thrown");
        }
        catch (IllegalArgumentException ok){}
    }
    
    private final class BasicPermissionImpl extends BasicPermission
    {
        public BasicPermissionImpl(String name)
        {
            super(name);
        }
    }
    
    /**
     * two BasicPermissions are equal if name and class are equal; 
     * equal permissions should have the same hash code
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEquals()
    {
        BasicPermission b1 = new BasicPermissionImpl("abc");
        BasicPermission b2 = null;
        assertTrue(b1.equals(b1)); 
        assertFalse(b1.equals(null));
        assertFalse(b1.equals(new Object()));
        assertFalse(b1.equals("abc"));
        assertTrue(b1.equals(b2 = new BasicPermissionImpl("abc")));
        assertTrue(b1.hashCode() == b2.hashCode());
        assertFalse(b1.equals(new BasicPermission("abc"){}));
        assertFalse(b1.equals(new BasicPermissionImpl("abc.*")));
    }

    /** 
     * implies() should return true if a permission is equal to or is implied 
     * by wildcarded permission, false otherwise.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.Permission.class}
    )
    public void testImplies()
    {
        BasicPermission b1 = new BasicPermissionImpl("a.b.c");
        assertTrue(b1.implies(b1));
        assertTrue(b1.implies(new BasicPermissionImpl("a.b.c")));
        assertFalse(b1.implies(new BasicPermissionImpl("a.b.c.*")));
        assertFalse(b1.implies(new BasicPermission("a.b.c"){}));
        assertTrue(new BasicPermissionImpl("a.b.*").implies(b1));
        assertTrue(new BasicPermissionImpl("a.*").implies(b1));
        assertTrue(new BasicPermissionImpl("*").implies(b1));
        assertFalse(new BasicPermissionImpl("a.b*").implies(b1));
        assertFalse(new BasicPermissionImpl("a.b.c.*").implies(b1));
        assertTrue(new BasicPermissionImpl("1.*").implies(new BasicPermissionImpl("1.234.*")));
        assertTrue(new BasicPermissionImpl("*").implies(new BasicPermissionImpl("*")));
    }
    
    /**
     * newPermissionCollection() should return new BasicPermissionCollection on every invocation
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "newPermissionCollection",
        args = {}
    )
    public void testCollection()
    {
        BasicPermission b1 = new BasicPermissionImpl("a.b.c");
        PermissionCollection pc1 = b1.newPermissionCollection();
        PermissionCollection pc2 = b1.newPermissionCollection();
//        assertTrue((pc1 instanceof BasicPermissionCollection) && (pc2 instanceof BasicPermissionCollection));
        assertNotSame(pc1, pc2);
    }
}
