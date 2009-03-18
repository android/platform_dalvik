/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.security.permissions;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.Permission;
import java.security.Provider;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.HashSet;
import java.util.Set;
/*
 * This class tests the secrity permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.security.Security
 */
@TestTargetClass(java.security.Security.class)
public class JavaSecuritySecurityTest extends TestCase {
    
    SecurityManager old;

    @Override
    protected void setUp() throws Exception {
        old = System.getSecurityManager();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        System.setSecurityManager(old);
        super.tearDown();
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getProperty() calls checkPermission on security permissions.",
        method = "getProperty",
        args = {java.lang.String.class}
    )
    public void test_getProperty() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            String target = null;
            void reset() {
                called = false;
                target = null;
            }

            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof SecurityPermission) {
                  target = permission.getName();
                    if (target.equals("getProperty.key")) {
                        called = true;
                    }
                }
            }

        }
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Security.getProperty("key");
        assertTrue("java.security.Security.getProperty() must call checkSecurityAccess on security manager", s.called);
        assertEquals("Argument of checkSecurityAccess is not correct", "getProperty.key", s.target);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that setProperty() method calls checkSecurityAccess on security manager.",
        method = "setProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_setProperty() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            String target = null;
            void reset(){
                called = false;
                target = null;
            }
            
            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof SecurityPermission) {
                  target = permission.getName();
                    if (target.equals("setProperty.key")) {
                        called = true;
                    }
                }
            }
        }
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        Security.setProperty("key", "value");
        assertTrue("java.security.Security.setProperty() must call checkSecurityAccess on security manager", s.called);
        assertEquals("Argument of checkSecurityAccess is not correct", "setProperty.key", s.target);
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that addProvider(), insertProviderAt() and removeProvider() methods call checkSecurityAccess method on security manager.",
            method = "addProvider",
            args = {java.security.Provider.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that addProvider(), insertProviderAt() and removeProvider() methods call checkSecurityAccess method on security manager.",
            method = "insertProviderAt",
            args = {java.security.Provider.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that addProvider(), insertProviderAt() and removeProvider() methods call checkSecurityAccess method on security manager.",
            method = "removeProvider",
            args = {java.lang.String.class}
        )
    })
    public void test_Provider() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            Set<String> targets = new HashSet<String>();
            void reset(){
                called = false;
                targets.clear();
            }
            @Override
            public void checkSecurityAccess(String target) {
                called = true;       
                this.targets.add(target);
                super.checkSecurityAccess(target);
            }         
            
            @Override
            public void checkPermission(Permission permission) {
            }
        }
        
        class MyProvider extends Provider {
            private static final long serialVersionUID = 1L;
            MyProvider(){
                super("DummyProvider", 1.0, "Provider for test purposes only");
            }
        }
        
        Provider p = new MyProvider();
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();        
        Security.addProvider(p);
        assertTrue("java.security.Security.addProvider() must call checkSecurityAccess on security manager", s.called);
        assertTrue("Argument of checkSecurityAccess is not correct", s.targets.contains("insertProvider.DummyProvider"));
        
        s.reset();        
        Security.removeProvider(p.getName());
        assertTrue("java.security.Security.removeProvider() must call checkSecurityAccess on security manager", s.called);
        assertTrue("Argument of checkSecurityAccess is not correct", s.targets.contains("removeProvider.DummyProvider"));

        s.reset();        
        Security.insertProviderAt(p, 0);
        assertTrue("java.security.Security.insertProviderAt() must call checkSecurityAccess on security manager", s.called);
        assertTrue("Argument of checkSecurityAccess is not correct", s.targets.contains("insertProvider.DummyProvider"));
    }
}
