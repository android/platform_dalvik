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

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.Permission;

/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.lang.Thread
 */
@TestTargetClass(java.lang.Thread.class)
public class JavaLangThreadTest extends TestCase {
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
        level = TestLevel.PARTIAL,
        notes = "Verifies that setContextClassLoader calls checkPermission on security manager.",
        method = "setContextClassLoader",
        args = {java.lang.ClassLoader.class}
    )
    public void test_setContextClassLoader() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            Permission p;

            void reset() {
                called = false;
                p = null;
            }

            @Override
            public void checkPermission(Permission p) {
                called = true;
                this.p = p;
            }
        }
        
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        t.setContextClassLoader(cl);
        assertTrue(
                "Thread.setContextClassLoader must call checkPermission on security manager",
                s.called);
        assertEquals(
                "Argument of checkPermission is not correct", 
                new RuntimePermission("setContextClassLoader"), 
                s.p);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that setContextClassLoader calls checkAccess on security manager.",
        method = "enumerate",
        args = {java.lang.Thread[].class}
    )
    @AndroidOnly("RI impl differs from RI spec, Android impl does not.")
    public void test_enumerate() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            Thread t;

            void reset() {
                called = false;
                t = null;
            }

            @Override
            public void checkAccess(Thread t) {
                called = true;
                this.t = t;
            }

            @Override
            public void checkPermission(Permission p) {
                
            }
        }
        
        Thread t = Thread.currentThread();
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Thread.enumerate(new Thread[]{});
        
        assertTrue(
                "Thread.enumerate must call checkAccess on security manager",
                s.called);
        assertEquals(
                "Argument of checkAccess is not correct", 
                t, s.t);
    }
    
    
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that getContextClassLoader calls checkPermission " +
                "on security manager.",
        method = "getContextClassLoader",
        args = {}
    )
    @AndroidOnly("RI impl differs from RI spec, Android impl does not.")
    public void test_getContextClassLoader() {
        class TestSecurityManager extends SecurityManager {
            boolean called;

            void reset() {
                called = false;
            }
            
            @Override
            public void checkPermission(Permission p) {
                if(p instanceof RuntimePermission 
                && "getClassLoader".equals(p.getName())) {
                    called = true;
                }
            }
        }
        TestSecurityManager sm = new TestSecurityManager();

        Thread t1 = new Thread();
        Thread t2 = new Thread();
        Thread t3 = new Thread();
        Thread t4 = new Thread();
        Thread t5 = new Thread();
        
        assertNotNull("test assumption: caller's class loader must not be null",
                this.getClass().getClassLoader());
        
        t1.setContextClassLoader(null);
        t2.setContextClassLoader(this.getClass().getClassLoader());
        t3.setContextClassLoader(this.getClass().getClassLoader().getParent());
        t4.setContextClassLoader(
                new dalvik.system.PathClassLoader("",
                        this.getClass().getClassLoader()));
        t5.setContextClassLoader(
                new ClassLoader(this.getClass().getClassLoader()) {});


        System.setSecurityManager(sm);

        sm.reset();
        t1.getContextClassLoader();
        assertTrue("permission must be checked: caller's class loader is not " +
                "equal to the requested class loader nor to any of its parents", 
                sm.called);

        sm.reset();
        t2.getContextClassLoader();
        assertFalse("permission must not be checked: " +
                "caller's class loader is identical to requested class loader",
                sm.called);

        sm.reset();
        t3.getContextClassLoader();
        assertTrue("permission must be checked: caller's class loader is not " +
                "equal to the requested class loader nor to any of its parents" +
                " (context class loader is parent of caller's class loader)",
                sm.called);

        sm.reset();
        t4.getContextClassLoader();
        assertFalse("permission must not be checked: " +
                "caller's class loader is parent of requested class loader",
                sm.called);

        sm.reset();
        t5.getContextClassLoader();
        assertFalse("permission must not be checked: " +
                "caller's class loader is parent of requested class loader",
                sm.called);
    }
}
