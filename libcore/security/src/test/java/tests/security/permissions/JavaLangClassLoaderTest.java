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

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.Permission;

/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.lang.ClassLoader
 */
@TestTargetClass(java.lang.ClassLoader.class)
public class JavaLangClassLoaderTest extends TestCase {
    
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
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that ClassLoader constructor calls checkCreateClassLoader on security manager.",
            method = "ClassLoader",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that ClassLoader constructor calls checkCreateClassLoader on security manager.",
            method = "ClassLoader",
            args = {java.lang.ClassLoader.class}
        )
    })
    public void test_ClassLoaderCtor () {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            void reset(){
                called = false;
            }
            @Override
            public void checkCreateClassLoader(){
                called = true;
            }
            @Override
            public void checkPermission(Permission p) {
                
            }
        }
        
        // class MyClassLoader defined package visible constructors
        class MyClassLoader extends ClassLoader { 
            MyClassLoader(){super();}
            MyClassLoader(ClassLoader parent){super(parent);}            
        }

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        ClassLoader c1 = new MyClassLoader();
        assertTrue("ClassLoader ctor must call checkCreateClassLoader on security manager", s.called);

        s.reset();
        ClassLoader c2 = new MyClassLoader(c1);
        assertTrue("ClassLoader ctor must call checkCreateClassLoader on security manager", s.called);
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that ClassLoader.getSystemClassLoader() calls checkPermission on security manager.",
            method = "getSystemClassLoader",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that ClassLoader.getParent() calls checkPermission on security manager.",
            method = "getParent",
            args = {}
        )
    })
    @BrokenTest("RI and Android don't pass this test. Also this test must be executed with a new PathClassLoader")
    public void test_getSystemClassLoader () {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            void reset(){
                called = false;
            }
            @Override
            public void checkPermission(Permission permission){
                if(permission instanceof RuntimePermission && "getClassLoader".equals(permission.getName())){
                    called = true;
                }
            }
        }
        
        //System.out.println(ClassLoaderTest.class.getClassLoader());
        //=>PathClassLoader

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        // TODO
        // a new Class has to be defined in a new ClassLoader, then
        // the check will be performed.
        
        s.reset();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        assertTrue("ClassLoader.getSystemClassLoader() must call checkPermission on security manager", s.called);
        
        s.reset();
        cl.getParent();
        assertTrue("Method getParent on a class loader must call checkPermission on security manager", s.called);
    }
}

