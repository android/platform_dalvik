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

package tests.api.java.security;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import java.io.File;
import java.io.FilePermission;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PropertyPermission;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

@TestTargetClass(AccessControlContext.class)
public class AccessControlContextTest extends junit.framework.TestCase {

    private class TestSecurityManager extends SecurityManager {

        private ArrayList<Permission> notAllowed;

        public TestSecurityManager() {
            notAllowed = new ArrayList<Permission>(2);

            notAllowed.add(new SecurityPermission("createAccessControlContext"));
            notAllowed.add(new SecurityPermission("getDomainCombiner"));
        }

        public void checkPermission(Permission p) {
            for (Iterator<Permission> i = notAllowed.iterator(); i.hasNext(); ) {
                if (i.next().equals(p)) {
                    throw new SecurityException();
                }
            }
        }        
    }

    /**
     * @tests java.security.AccessControlContext#AccessControlContext(java.security.ProtectionDomain[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "AccessControlContext",
        args = {java.security.ProtectionDomain[].class}
    )
    public void test_Constructor$Ljava_security_ProtectionDomain() {
        // Test for method
        // java.security.AccessControlContext(java.security.ProtectionDomain [])

        // Create a permission which is not normally granted
        final Permission perm = new PropertyPermission("java.class.path",
                "read");
        PermissionCollection col = perm.newPermissionCollection();
        col.add(perm);
        final ProtectionDomain pd = new ProtectionDomain(null, col);
        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[] { pd });
        try {
            acc.checkPermission(perm);
        } catch (SecurityException e) {
            fail("Should have permission");
        }

        final boolean[] result = new boolean[] { false };
        Thread th = new Thread(new Runnable() {
            public void run() {
                AccessControlContext acc = new AccessControlContext(
                        new ProtectionDomain[] { pd });
                try {
                    acc.checkPermission(perm);
                    result[0] = true;
                } catch (SecurityException e) {
                }
            }
        });
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            // ignore
        }
        assertTrue("Test 1: Thread should have permission", result[0]);
        
        //Null parameter checking
        try {
            new AccessControlContext(null);
            fail("Test 2: NullPointerException expected.");
        } catch (Exception ex) {
            //expected
        }
    }

    /**
     * @tests java.security.AccessControlContext#AccessControlContext(java.security.AccessControlContext,
     *        java.security.DomainCombiner)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "AccessControlContext",
        args = {java.security.AccessControlContext.class, java.security.DomainCombiner.class}
    )
    public void test_ConstructorLjava_security_AccessControlContextLjava_security_DomainCombiner() {
        AccessControlContext context = AccessController.getContext();
        try {
            new AccessControlContext(context, null);
        } catch (NullPointerException e) {
            fail("should not throw NullPointerException");
        }

        try {
            new AccessControlContext(context, new SubjectDomainCombiner(
                    new Subject()));
        } catch (Exception e) {
            fail("should not throw Exception");
        }
    }
    
    /**
     * @tests java.security.AccessControlContext#AccessControlContext(java.security.AccessControlContext,
     *        java.security.DomainCombiner)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks SecurityException.",
        method = "AccessControlContext",
        args = {java.security.AccessControlContext.class, java.security.DomainCombiner.class}
    )
    public void test_ConstructorLjava_security_AccessControlContextLjava_security_DomainCombiner2() {

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(new TestSecurityManager());
        AccessControlContext context = AccessController.getContext();
        try {
            new AccessControlContext(context, null);
            fail("Test 1: SecurityException expected.");
        } catch (SecurityException e) {
            // Expected.
        }
        System.setSecurityManager(oldSm);
    }

    /**
     * @tests java.security.AccessControlException#checkPermission(Permission)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "checkPermission",
        args = {java.security.Permission.class}
    )
    public void test_checkPermission() {
        char s = File.separatorChar;
        FilePermission perm[] = new FilePermission[7];
        perm[0] = new FilePermission("test1.file", "write");
        perm[1] = new FilePermission("test1.file", "read, execute, delete");
        perm[2] = new FilePermission(s + "tmp" + s + "test" + s + "*",
                "read, write");
        perm[3] = new FilePermission(s + "tmp" + s + "test" + s
                + "collection.file", "read");
        perm[4] = new FilePermission(s + "windows" + "*", "delete");
        perm[5] = new FilePermission("aFile.file", "read");
        perm[6] = new FilePermission("hello.file", "write");

        Permissions perms = new Permissions();
        for (int i = 0; i < perm.length; i++) {
            perms.add(perm[i]);
        }
        ProtectionDomain pd = new ProtectionDomain(null, perms);

        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[] { pd });

        for (int i = 0; i < perm.length; i++) {
            try {
                acc.checkPermission(perm[i]);
            } catch (AccessControlException e) {
                fail("Should have permission " + perm[i]);
            }
        }

        try {
            acc.checkPermission(new FilePermission("test1.file", "execute"));
        } catch (AccessControlException e) {
            fail("Should have permission ");
        }

        try {
            acc.checkPermission(new FilePermission(s + "tmp" + s + "test" + s
                    + "hello.file", "read"));
        } catch (AccessControlException e) {
            fail("Should have permission ");
        }
        
        try {
            acc.checkPermission(new FilePermission("test2.file", "execute"));
            fail("SecurityException expected");
        } catch (AccessControlException e) {
            // expected
        }

        try {
            acc.checkPermission(new FilePermission(s + "tmp" + s + "test" + s
                    + "hello.file", "delete"));
            fail("SecurityException expected");
        } catch (AccessControlException e) {
            // expected
        }
        
        try {
            acc.checkPermission(null);
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * @tests java.security.AccessControlException#equals()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void test_equals() {
        final Permission perm1 = new PropertyPermission("java.class.path",
                "read");
        final Permission perm2 = new PropertyPermission("java.path", "write");

        PermissionCollection col1 = perm1.newPermissionCollection();
        col1.add(perm1);
        final ProtectionDomain pd1 = new ProtectionDomain(null, col1);
        AccessControlContext acc1 = new AccessControlContext(
                new ProtectionDomain[] { pd1 });

        AccessControlContext acc2 = new AccessControlContext(
                new ProtectionDomain[] { pd1 });

        PermissionCollection col2 = perm2.newPermissionCollection();
        col2.add(perm2);
        col2.add(perm2);
        final ProtectionDomain pd2 = new ProtectionDomain(null, col2);
        AccessControlContext acc3 = new AccessControlContext(
                new ProtectionDomain[] { pd2 });

        assertFalse(acc1.equals(null));
        assertFalse(acc2.equals(null));
        assertFalse(acc3.equals(null));

        assertTrue(acc1.equals(acc2));
        assertTrue(acc2.equals(acc1));
        assertFalse(acc1.equals(acc3));
        assertFalse(acc2.equals(acc3));

        AccessControlContext context = AccessController.getContext();

        AccessControlContext acc4 = new AccessControlContext(context, null);
        AccessControlContext acc5 = new AccessControlContext(context,
                new SubjectDomainCombiner(new Subject()));

        AccessControlContext acc6 = new AccessControlContext(context, null);

        assertFalse(acc4.equals(null));
        assertFalse(acc5.equals(null));

        assertFalse(acc4.equals(acc5));
        assertFalse(acc5.equals(acc4));

        assertTrue(acc4.equals(acc6));
        assertTrue(acc6.equals(acc4));
    }

    /**
     * @tests java.security.AccessControlException#getDomainCombiner()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getDomainCombiner",
        args = {}
    )
    public void test_getDomainCombiner() {
        AccessControlContext context = AccessController.getContext();

        AccessControlContext acc1 = new AccessControlContext(context, null);

        AccessControlContext acc2 = new AccessControlContext(context,
                new SubjectDomainCombiner(new Subject()));

        final Permission perm1 = new PropertyPermission("java.class.path",
                "read");

        PermissionCollection col1 = perm1.newPermissionCollection();
        col1.add(perm1);
        final ProtectionDomain pd1 = new ProtectionDomain(null, col1);
        AccessControlContext acc3 = new AccessControlContext(
                new ProtectionDomain[] { pd1 });

        assertNull(acc1.getDomainCombiner());
        assertNotNull(acc2.getDomainCombiner());
        assertNull(acc3.getDomainCombiner());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(new TestSecurityManager());
        try {
            acc1.getDomainCombiner();
            fail("SecurityException expected.");
        } catch (SecurityException e) {
            // Expected.
        }
        System.setSecurityManager(oldSm);
    }

    /**
     * @tests java.security.AccessControlException#hashCode()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void test_hashCode() {
        final Permission perm1 = new PropertyPermission("java.class.path",
                "read");
        final Permission perm2 = new PropertyPermission("java.path", "write");

        PermissionCollection col1 = perm1.newPermissionCollection();
        col1.add(perm1);
        final ProtectionDomain pd1 = new ProtectionDomain(null, col1);
        AccessControlContext acc1 = new AccessControlContext(
                new ProtectionDomain[] { pd1 });

        AccessControlContext acc2 = new AccessControlContext(
                new ProtectionDomain[] { pd1 });

        PermissionCollection col2 = perm2.newPermissionCollection();
        col2.add(perm2);
        col2.add(perm2);
        final ProtectionDomain pd2 = new ProtectionDomain(null, col2);
        AccessControlContext acc3 = new AccessControlContext(
                new ProtectionDomain[] { pd2 });

        assertTrue(acc1.hashCode() == acc1.hashCode());
        assertTrue(acc2.hashCode() == acc2.hashCode());
        assertTrue(acc3.hashCode() == acc3.hashCode());

        assertTrue(acc1.hashCode() == acc2.hashCode());
        assertTrue(acc2.hashCode() != acc3.hashCode());
        assertTrue(acc3.hashCode() != acc1.hashCode());

        AccessControlContext context = AccessController.getContext();

        AccessControlContext acc4 = new AccessControlContext(context, null);
        AccessControlContext acc5 = new AccessControlContext(context,
                new SubjectDomainCombiner(new Subject()));

        AccessControlContext acc6 = new AccessControlContext(context, null);

        assertTrue(acc4.hashCode() == acc4.hashCode());
        assertTrue(acc5.hashCode() == acc5.hashCode());
        assertTrue(acc6.hashCode() == acc6.hashCode());

        assertTrue(acc4.hashCode() == acc5.hashCode());
        assertTrue(acc5.hashCode() == acc6.hashCode());
        assertTrue(acc6.hashCode() == acc4.hashCode());
    }
}