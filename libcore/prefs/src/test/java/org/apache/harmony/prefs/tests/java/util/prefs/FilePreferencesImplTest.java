/* Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.harmony.prefs.tests.java.util.prefs;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.FilePermission;
import java.security.Permission;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import junit.framework.TestCase;

@TestTargetClass(java.util.prefs.Preferences.class)
public class FilePreferencesImplTest extends TestCase {

    public FilePreferencesImplTest() {
        super();
    }
    
    @Override
    protected void setUp(){
    }
    @Override
    protected void tearDown(){
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "put",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "get",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "keys",
            args = {}
        )
    })
    public void testUserPutGet() throws BackingStoreException {
        Preferences uroot = Preferences.userRoot().node("test");
        uroot.put("ukey1", "value1");
        assertEquals("value1", uroot.get("ukey1", null));
        String[] names = uroot.keys();
        assertTrue(names.length >= 1);

        uroot.put("ukey2", "value3");
        assertEquals("value3", uroot.get("ukey2", null));
        uroot.put("\u4e2d key1", "\u4e2d value1");
        assertEquals("\u4e2d value1", uroot.get("\u4e2d key1", null));
        names = uroot.keys();
        assertEquals(3, names.length);

        uroot.clear();
        names = uroot.keys();
        assertEquals(0, names.length);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "put",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "get",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Exceptions checking missed, but method is abstract, probably it is OK",
            method = "keys",
            args = {}
        )
    })
    public void testSystemPutGet() throws BackingStoreException {
        Preferences sroot = Preferences.systemRoot().node("test");
        sroot.put("skey1", "value1");
        assertEquals("value1", sroot.get("skey1", null));
        sroot.put("\u4e2d key1", "\u4e2d value1");
        assertEquals("\u4e2d value1", sroot.get("\u4e2d key1", null));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Exceptions checking missed, but method is abstract, probably it is OK",
        method = "childrenNames",
        args = {}
    )
    public void testUserChildNodes() throws Exception {
        Preferences uroot = Preferences.userRoot().node("test");

        Preferences child1 = uroot.node("child1");
        Preferences child2 = uroot.node("\u4e2d child2");
        Preferences grandchild = child1.node("grand");
        assertNotNull(grandchild);

        String[] childNames = uroot.childrenNames();
        assertContains(childNames, "child1");
        assertContains(childNames, "\u4e2d child2");
        assertNotContains(childNames, "grand");

        childNames = child1.childrenNames();
        assertContains(childNames, "grand");

        childNames = child2.childrenNames();
        assertEquals(0, childNames.length);

        child1.removeNode();
        childNames = uroot.childrenNames();
        assertNotContains(childNames, "child1");
        assertContains(childNames, "\u4e2d child2");
        assertNotContains(childNames, "grand");

        child2.removeNode();
        childNames = uroot.childrenNames();
        assertNotContains(childNames, "child1");
        assertNotContains(childNames, "\u4e2d child2");
        assertNotContains(childNames, "grand");
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Exceptions checking missed, but method is abstract, probably it is OK",
        method = "childrenNames",
        args = {}
    )
    @AndroidOnly("It seems like the RI can't remove nodes created in the system root.")
    public void testSystemChildNodes() throws Exception {
        Preferences sroot = Preferences.systemRoot().node("test");

        Preferences child1 = sroot.node("child1");
        Preferences child2 = sroot.node("child2");
        Preferences grandchild = child1.node("grand");

        String[] childNames = sroot.childrenNames();
        assertContains(childNames, "child1");
        assertContains(childNames, "child2");
        assertNotContains(childNames, "grand");

        childNames = child1.childrenNames();
        assertEquals(1, childNames.length);

        childNames = child2.childrenNames();
        assertEquals(0, childNames.length);

        child1.removeNode();
        childNames = sroot.childrenNames();
        assertNotContains(childNames, "child1");
        assertContains(childNames, "child2");
        assertNotContains(childNames, "grand");

        child2.removeNode();
        childNames = sroot.childrenNames();
        assertNotContains(childNames, "child1");
        assertNotContains(childNames, "child2");
        assertNotContains(childNames, "grand");
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "SecurityException checking only, but methods are abstract, probably it is OK",
            method = "node",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "SecurityException checking only, but methods are abstract, probably it is OK",
            method = "removeNode",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "SecurityException checking only, but methods are abstract, probably it is OK",
            method = "childrenNames",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "SecurityException checking only, but methods are abstract, probably it is OK",
            method = "flush",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "SecurityException checking only, but methods are abstract, probably it is OK",
            method = "sync",
            args = {}
        )
    })
    public void testSecurityException() throws BackingStoreException {
        Preferences uroot = Preferences.userRoot().node("test");

        Preferences child1 = uroot.node("child1");
        MockFileSecurityManager manager = new MockFileSecurityManager();
        manager.install();
        try {
            try {
                uroot.node("securityNode");
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                // need FilePermission(delete);
                child1.removeNode();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                uroot.childrenNames();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            uroot.keys();
            uroot.put("securitykey", "value1");
            uroot.remove("securitykey");
            try {
                uroot.flush();
                fail("should throw security exception");
            } catch (SecurityException e) {
            } catch (BackingStoreException e) {
                assertTrue(e.getCause() instanceof SecurityException);
            }
            try {
                uroot.sync();
                fail("should throw security exception");
            } catch (SecurityException e) {
            } catch (BackingStoreException e) {
                assertTrue(e.getCause() instanceof SecurityException);
            }
        } finally {
            manager.restoreDefault();
        }
    }

    private void assertContains(String[] childNames, String name) {
        for (String childName : childNames) {
            if (childName == name) {
                return;
            }
        }
        fail("No child with name " + name + " was found. It was expected to exist.");
    }

    private void assertNotContains(String[] childNames, String name) {
        for (String childName : childNames) {
            if (childName == name) {
                fail("Child with name " + name + " was found. This was unexpected.");
            }
        }
    }

    static class MockFileSecurityManager extends SecurityManager {

        SecurityManager dflt;

        public MockFileSecurityManager() {
            super();
            dflt = System.getSecurityManager();
        }

        public void install() {
            System.setSecurityManager(this);
        }

        public void restoreDefault() {
            System.setSecurityManager(dflt);
        }

        public void checkPermission(Permission perm) {
            if (perm instanceof FilePermission) {
                throw new SecurityException();
            } else if (dflt != null) {
                dflt.checkPermission(perm);
            }
        }

        public void checkPermission(Permission perm, Object ctx) {
            if (perm instanceof FilePermission) {
                System.out.println(perm.getActions());
                throw new SecurityException();
            } else if (dflt != null) {
                dflt.checkPermission(perm, ctx);
            }
        }

    }
}
