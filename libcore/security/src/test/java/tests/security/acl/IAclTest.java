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

import java.security.acl.Acl;
import java.security.acl.AclEntry;
import java.security.acl.NotOwnerException;
import java.security.acl.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.harmony.security.tests.support.acl.*;

@TestTargetClass(Acl.class)
public class IAclTest extends TestCase {
    
    class MyAcl extends AclImpl {
        public MyAcl(Principal principal, String str) {
            super(principal, str);
        }
    }

    
    /**
     * @tests java.security.acl.Acl#addEntry(Principal caller, AclEntry entry)
     * @tests java.security.acl.Acl#removeEntry(Principal caller, AclEntry entry)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "addEntry",
            args = {java.security.Principal.class, java.security.acl.AclEntry.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "removeEntry",
            args = {java.security.Principal.class, java.security.acl.AclEntry.class}
        )
    })
    public void test_Acl01() {
        Principal pr = new PrincipalImpl("TestPrincipal");
        String str = "TestName";
        MyAcl acl = new MyAcl(pr, str);
        AclEntry ae = new AclEntryImpl(pr);
        try {
            assertTrue(acl.addEntry(pr, ae));
            assertFalse(acl.addEntry(pr, ae));
            assertTrue(acl.removeEntry(pr, ae));
            assertFalse(acl.removeEntry(pr, ae));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
        
        try {
            acl.addEntry(new PrincipalImpl("NewPrincipal"), ae);
            fail("NotOwnerException was not thrown");
        } catch (NotOwnerException noe) {
            //expected
        }
        
        try {
            acl.removeEntry(new PrincipalImpl("NewPrincipal"), ae);
            fail("NotOwnerException was not thrown");
        } catch (NotOwnerException noe) {
            //expected
        }
    }
    
    /**
     * @tests java.security.acl.Acl#setName(Principal caller, String name)
     * @tests java.security.acl.Acl#getName()
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setName",
            args = {java.security.Principal.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getName",
            args = {}
        )
    })
    public void test_Acl02() {
        Principal pr = new PrincipalImpl("TestPrincipal");
        String str = "TestName";
        String newStr = "NewName";
        MyAcl acl = new MyAcl(pr, str);
        try {
            assertEquals("Names are not equal", str, acl.getName());
            acl.setName(pr, newStr);
            assertEquals("Names are not equal", newStr, acl.getName());
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
        
        try {
            acl.setName(new PrincipalImpl("NewPrincipal"), str);
            fail("NotOwnerException was not thrown");
        } catch (NotOwnerException noe) {
            //expected
        }
    }
    
    /**
     * @tests java.security.acl.Acl#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_Acl03() {
        Principal pr = new PrincipalImpl("TestPrincipal");
        String str = "TestName";
        MyAcl acl = new MyAcl(pr, str);
        AclEntry ae = new AclEntryImpl(pr);
        Permission perm = new PermissionImpl("Permission_1");
        try {
            ae.addPermission(perm);
            acl.addEntry(pr, ae);
            String res = acl.toString();
            assertTrue(res.contains(perm.toString()));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.Acl#entries()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "entries",
        args = {}
    )
    public void test_Acl04() {
        Principal pr = new PrincipalImpl("TestPrincipal");
        String str = "TestName";
        MyAcl acl = new MyAcl(pr, str);
        AclEntry ae1 = new AclEntryImpl(pr);
        try {
            ae1.addPermission(new PermissionImpl("Permission_1"));
            acl.addEntry(pr, ae1);
            Enumeration en = acl.entries();
            Vector v = new Vector();
            while (en.hasMoreElements()) {
                v.addElement(en.nextElement());
            }
            assertEquals(v.size(), 1);
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.Acl#checkPermission(Principal principal, Permission permission)
     * @tests java.security.acl.Acl#getPermissions(Principal principal)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "checkPermission",
            args = {java.security.Principal.class, java.security.acl.Permission.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getPermissions",
            args = {java.security.Principal.class}
        )
    })
    public void test_Acl05() {
        Principal pr = new PrincipalImpl("TestPrincipal");
        String str = "TestName";
        MyAcl acl = new MyAcl(pr, str);
        AclEntry ae = new AclEntryImpl(pr);
        Permission perm = new PermissionImpl("Permission_1");
        try {
            ae.addPermission(perm);
            acl.addEntry(pr, ae);
            
            //checkPermission verification
            assertTrue("Incorrect permission", acl.checkPermission(pr, perm));
            assertFalse(acl.checkPermission(pr, new PermissionImpl("Permission_2")));
            
            //getPermissions
            Enumeration en = acl.getPermissions(pr);
            Vector v = new Vector();
            while (en.hasMoreElements()) {
                v.addElement(en.nextElement());
            }
            assertEquals(v.size(), 1);
            assertEquals(v.elementAt(0).toString(), perm.toString());
        } catch (Exception ex) {
            fail("Exception " + ex + " was thrown");
        }
    }
}