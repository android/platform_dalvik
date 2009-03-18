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

package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.*;

import junit.framework.TestCase;
@TestTargetClass(PermissionCollection.class)
/**
 * Tests for <code>PermissionCollection</code>
 * 
 */

public class PermissionCollectionTest extends TestCase {

    // Bare extension to instantiate abstract PermissionCollection class
    private static final class RealPermissionCollection extends PermissionCollection
    {
        final private Set <Permission> setCol = new HashSet<Permission>(); 
        public RealPermissionCollection(Set <Permission> col)
        {
            if (col != null) {
                setCol.addAll(col);
            }
        }
               
        public void add(Permission permission) {
            if (!setCol.add(permission)) {
                throw new IllegalArgumentException("permission is not added");
            }
        }
        
        public Enumeration elements() 
        {
            return setCol == null ? null : Collections.enumeration(setCol);
        }
        
        public boolean implies(Permission permission) 
        {
            return false;
        }
    }
        
    /** Test read-only flag. Should be false by default and can be set once forever. */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "isReadOnly",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setReadOnly",
            args = {}
        )
    })
    public void testReadOnly()
    {
        PermissionCollection pc = new RealPermissionCollection(null);
        assertFalse("should not be read-only by default", pc.isReadOnly());
        pc.setReadOnly();
        assertTrue("explicitly set read-only", pc.isReadOnly());
        pc.setReadOnly();
        assertTrue("more calls to setReadOnly() should not harm", pc.isReadOnly());
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void testToString() {
        Set<Permission> perm = new HashSet<Permission>();
        Permission p = new RealPermission("TestPermission");
        perm.add(p);
        PermissionCollection pc = new RealPermissionCollection(perm);
        try {
            String str = pc.toString();
            assertNotNull("toString return null", str);
        } catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }
}

class RealPermission extends Permission {
    
    public RealPermission(String name) {
        super(name);
    }

    public boolean equals(Object obj) {
        return false;
    }

    public String getActions() {
        return null;
    }
    public int hashCode() {
        return 0;
    }

    public boolean implies(Permission permission) {
        return false;
    }
}
