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

import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.PermissionCollection;
import java.security.UnresolvedPermission;

import junit.framework.TestCase;
@TestTargetClass(AllPermission.class)
/**
 * Tests for <code>AllPermission</code>
 * 
 */
public class AllPermissionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllPermissionTest.class);
    }

    /**
     * Test all constructors: an object is created, name and actions are ignored
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "AllPermission",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "AllPermission",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    public void testCtor()
    {
        AllPermission a1 = new AllPermission();
        assertEquals("<all permissions>", a1.getName());
        assertEquals("<all actions>", a1.getActions());
        
        a1 = new AllPermission("sdfsdfwe&^$", "*&IUGJKHVB764");
        assertEquals("<all permissions>", a1.getName());
        assertEquals("<all actions>", a1.getActions());
        
        a1 = new AllPermission(null, "");
        assertEquals("<all permissions>", a1.getName());
        assertEquals("<all actions>", a1.getActions());
    }

    /** Any of AllPermission instances are equal and have the same hash code */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEquals()
    {
        AllPermission a1 = new AllPermission();
        AllPermission a2 = new AllPermission();
        assertTrue(a1.equals(a2));
        assertTrue(a1.hashCode() == a2.hashCode());
        assertFalse(a1.equals(null));
        assertFalse(a1.equals(new BasicPermission("hgf"){}));
    }
    
    /** AllPermission implies any other permission */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Null parameter checking missed",
        method = "implies",
        args = {java.security.Permission.class}
    )
    public void testImplies()
    {
        AllPermission a1 = new AllPermission();
        assertTrue(a1.implies(new AllPermission()));
        assertTrue(a1.implies(new BasicPermission("2323"){}));
        assertTrue(a1.implies(new UnresolvedPermission("2323", "", "", null)));
    }
    
    /** newPermissionCollection() returns a new AllPermissionCollection on every invocation. */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "newPermissionCollection",
        args = {}
    )
    public void testCollection()
    {
        AllPermission a1 = new AllPermission();
        PermissionCollection pc1 = a1.newPermissionCollection();
        PermissionCollection pc2 = a1.newPermissionCollection();
//        assertTrue((pc1 instanceof AllPermissionCollection) && (pc2 instanceof AllPermissionCollection));
        assertNotSame(pc1, pc2);
    }
}
