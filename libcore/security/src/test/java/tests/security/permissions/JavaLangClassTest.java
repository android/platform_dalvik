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

import java.security.Permission;

import junit.framework.TestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.lang.Class
 */
@TestTargetClass(java.lang.Class.class)
public class JavaLangClassTest extends TestCase {
    
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
        notes = "Verifies that Class.getProtectionDomain() checks " +
                "RuntimePermission of security manager.",
        method = "getProtectionDomain",
        args = {}
    )
    public void test_getProtectionDomain () {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            void reset(){
                called = false;
            }
            @Override
            public void checkPermission(Permission permission){
                if(permission instanceof RuntimePermission && "getProtectionDomain".equals(permission.getName())){
                    called = true;
                }
            }
        }

        Class<String> c = java.lang.String.class;
        assertTrue("java.lang.String.class not assigned", c != null);

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        c.getProtectionDomain();
        assertTrue("Test 2: Class.getProtectionDomain() must check " +
                "RuntimePermission(\"getProtectionDomain\") on " +
                "security manager", s.called);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that forName(String,boolean,Classloader) method checks RuntimePermission(getClassLoader) of security manager.",
        method = "forName",
        args = {String.class, boolean.class, ClassLoader.class}
    )
    public void test_forName() throws ClassNotFoundException {
                class TestSecurityManager extends SecurityManager {
            boolean called;
            void reset(){
                called = false;
            }
            @Override
            public void checkPermission(Permission permission){
                if (permission instanceof RuntimePermission && "getClassLoader".equals(permission.getName())){
                    called = true;
                }
            }
        }

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        Class.forName("java.lang.String", true, null);
        assertTrue("Test 2: Class.forName(String,boolean,Classloader) must " +
                "check RuntimePermission(getClassLoader) on security manager", 
                s.called);
    }
}
