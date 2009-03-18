/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.security.acl;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.acl.AclEntry;
import java.security.acl.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.harmony.security.tests.support.acl.*;

@TestTargetClass(AclEntry.class)
public class IAclEntryTest extends TestCase {
    
    class MyAclEntry extends AclEntryImpl {
        public MyAclEntry() {
            super();
        }
        public MyAclEntry(Principal pr) {
            super(pr);
        }
    }
    
    
    /**
     * @tests java.security.acl.AclEntry#addPermission(Permission permission) 
     * @tests java.security.acl.AclEntry#checkPermission(Permission permission)
     * @tests java.security.acl.AclEntry#removePermission(Permission permission)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "addPermission",
            args = {java.security.acl.Permission.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "checkPermission",
            args = {java.security.acl.Permission.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "removePermission",
            args = {java.security.acl.Permission.class}
        )
    })
    public void test_AclEntry01() {
        Permission perm = new PermissionImpl("Permission_1");
        MyAclEntry ae = new MyAclEntry(new PrincipalImpl("TestPrincipal"));
        try {
            assertTrue(ae.addPermission(perm));
            assertFalse(ae.addPermission(perm));
            assertTrue(ae.checkPermission(perm));
            assertTrue(ae.removePermission(perm));
            assertFalse(ae.removePermission(perm));
            assertFalse(ae.checkPermission(perm));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.AclEntry#getPrincipal() 
     * @tests java.security.acl.AclEntry#setPrincipal(Principal user)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getPrincipal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setPrincipal",
            args = {java.security.Principal.class}
        )
    })
    public void test_AclEntry02() {
        MyAclEntry ae = new MyAclEntry();
        Principal mp = new PrincipalImpl("TestPrincipal");
        try {
            assertTrue(ae.setPrincipal(mp));
            Principal p = ae.getPrincipal();
            assertEquals("Names are not equal", p.getName(), mp.getName());
            assertFalse(ae.setPrincipal(mp));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.AclEntry#setNegativePermissions() 
     * @tests java.security.acl.AclEntry#isNegative()
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setNegativePermissions",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "isNegative",
            args = {}
        )
    })
    public void test_AclEntry03() {
        MyAclEntry ae = new MyAclEntry(new PrincipalImpl("TestPrincipal"));
        try {
            assertFalse("isNegative() returns TRUE",ae.isNegative());
            ae.setNegativePermissions();
            assertTrue("isNegative() returns FALSE", ae.isNegative());
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.AclEntry#permissions() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "permissions",
        args = {}
    )
    public void test_AclEntry04() {
        MyAclEntry ae = new MyAclEntry(new PrincipalImpl("TestPrincipal"));
        Permission perm = new PermissionImpl("Permission_1");
        try {
            Enumeration en = ae.permissions();
            assertFalse("Not empty enumeration", en.hasMoreElements());
            ae.addPermission(perm);
            en = ae.permissions();
            assertTrue("Eempty enumeration", en.hasMoreElements());
            Vector v = new Vector();
            while (en.hasMoreElements()) {
                v.addElement(en.nextElement());
            }
            assertEquals(v.size(), 1);
            assertEquals(v.elementAt(0).toString(), perm.toString());
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.AclEntry#toString() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_AclEntry05() {
        MyAclEntry ae = new MyAclEntry(new PrincipalImpl("TestPrincipal"));
        try {
            String res = ae.toString();
            assertTrue(res.contains("TestPrincipal"));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.AclEntry#clone() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public void test_AclEntry06() {
        MyAclEntry ae = new MyAclEntry(new PrincipalImpl("TestPrincipal"));
        try {
            assertEquals("Objects are not equal", ae.toString(), ae.clone().toString());
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
}