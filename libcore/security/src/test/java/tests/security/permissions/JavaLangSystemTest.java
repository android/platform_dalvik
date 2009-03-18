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

import java.io.InputStream;
import java.io.PrintStream;
import java.security.Permission;
import java.util.Properties;
import java.util.PropertyPermission;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.lang.System
 */
@TestTargetClass(java.lang.System.class)
public class JavaLangSystemTest extends TestCase {
    
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
            notes = "Verifies that getProperties and setProperties call checkPropertiesAccess on security manager.",
            method = "getProperties",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that getProperties and setProperties call checkPropertiesAccess on security manager.",
            method = "setProperties",
            args = {java.util.Properties.class}
        )
    })
    public void test_Properties() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            
            void reset(){
                called = false;
            }
            
            @Override
            public void checkPropertiesAccess() {
                called = true;
            }

            @Override
            public void checkPermission(Permission p) {
                // nothing to do
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        Properties props = System.getProperties();
        assertTrue("System.getProperties must call checkPropertiesAccess on security manager", s.called);
        
        s.reset();
        System.setProperties(props);
        assertTrue("System.setProperties must call checkPropertiesAccess on security manager", s.called);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that System.getProperty calls checkPropertyAccess on security manager.",
            method = "getProperty",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that System.getProperty calls checkPropertyAccess on security manager.",
            method = "getProperty",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    public void test_getProperty() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            String key;
            
            void reset(){
                called = false;
                key = null;
            }
            
            @Override
            public void checkPropertyAccess(String key) {
                called = true;
                this.key = key;
            }

            @Override
            public void checkPermission(Permission p) {
                // nothing to do
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        System.getProperty("key");
        assertTrue("System.getProperty must call checkPropertyAccess on security manager", s.called);
        assertEquals("Argument of checkPropertyAccess is not correct", "key", s.key);
        
        s.reset();
        System.getProperty("key", "value");
        assertTrue("System.getProperty must call checkPropertyAccess on security manager", s.called);
        assertEquals("Argument of checkPropertyAccess is not correct", "key", s.key);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that System.setProperty method calls checkPermission of security manager.",
        method = "setProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_setProperty() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            Permission p;
            
            void reset(){
                called = false;
                p = null;
            }
            
            @Override
            public void checkPermission(Permission p) {
                called = true;
                this.p = p;
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        System.setProperty("key", "value");
        assertTrue("System.setProperty must call checkPermission on security manager", s.called);
        assertEquals("Argument of checkPermission is not correct", new PropertyPermission("key", "write"), s.p);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that System.setSecurityManager method checks security permissions.",
        method = "setSecurityManager",
        args = {java.lang.SecurityManager.class}
    )
    public void test_setSecurityManager() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            @Override
            public void checkPermission(Permission permission) {
                if(permission instanceof RuntimePermission && "setSecurityManager".equals(permission.getName())){
                    called = true;              
                }
            }
            
        }
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        System.setSecurityManager(s);
        assertTrue("System.setSecurityManager must check security permissions", s.called);
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that setIn/Out/Err methods call checkPermission method of security manager., needs a fix in class System, see ticket #67",
            method = "setIn",
            args = {java.io.InputStream.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that setIn/Out/Err methods call checkPermission " +
                    "method of security manager., needs a fix in class System, " +
                    "see ticket #67",
            method = "setOut",
            args = {java.io.PrintStream.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that setIn/Out/Err methods call checkPermission " +
                    "method of security manager., needs a fix in class System, " +
                    "see ticket #67",
            method = "setErr",
            args = {java.io.PrintStream.class}
        )
    })
    public void test_setInOutErr() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            Permission p;
            
            void reset(){
                called = false;
                p = null;
            }
            
            @Override
            public void checkPermission(Permission p) {
                called = true;
                this.p = p;
            }
        }
        
        InputStream in = System.in;
        PrintStream out = System.out;
        PrintStream err = System.err;
        Permission p = new RuntimePermission("setIO");

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        System.setIn(in);
        assertTrue("System.setIn(Inputstream) must call checkPermission on security manager", s.called);
        assertEquals("Argument of checkPermission is not correct", p, s.p);

        System.setOut(err);
        assertTrue("System.setOut(PrintStream) must call checkPermission on security manager", s.called);
        assertEquals("Argument of checkPermission is not correct", p, s.p);

        System.setErr(out);
        assertTrue("System.setErr(PrintStream) must call checkPermission on security manager", s.called);
        assertEquals("Argument of checkPermission is not correct", p, s.p);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that exit calls checkExit on security manager.",
        method = "exit",
        args = {int.class}
    )
    public void test_exit() {
        class ExitNotAllowedException extends RuntimeException {}
        class TestSecurityManager extends SecurityManager {
            boolean called;
            int status;
            void reset(){
                called = false;
                status = -1;
            }
            @Override
            public void checkExit(int status){
                this.called = true;
                this.status = status;
                throw new ExitNotAllowedException(); // prevent that the system is shut down
            }

            @Override
            public void checkPermission(Permission p) {
                // nothing to do
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        try {
            System.exit(11);
            fail("Runtime.exit must call checkExit on security manager with a RuntimePermission");
        }
        catch(ExitNotAllowedException e){
             // expected exception
        }
        assertTrue("Runtime.exit must call checkExit on security manager with a RuntimePermission", s.called);
        assertEquals("Argument of checkExit is not correct", 11, s.status);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies that runFinalizersOnExit calls checkExit on security manager., implementation of Runtime.runFinalizersOnExit needs to be fixed, see ticket 57",
        method = "runFinalizersOnExit",
        args = {boolean.class}
    )
    public void test_runFinalizersOnExit() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            int status;
            void reset(){
                called = false;
                status = -1;
            }
            @Override
            public void checkExit(int status){
                this.called = true;
                this.status = status;
            }

            @Override
            public void checkPermission(Permission p) {
                // nothing to do
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        System.runFinalizersOnExit(true);
        assertTrue("System.runFinalizersOnExit(true) must call checkExit on security manager", s.called);
        assertEquals("Argument of checkExit is not correct", 0, s.status);

        s.reset();
        System.runFinalizersOnExit(false);
        assertTrue("System.runFinalizersOnExit(false) must call checkExit on security manager", s.called);
        assertEquals("Argument of checkExit is not correct", 0, s.status);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that methods load and loadLibrary call checkLink on security manager.",
            method = "load",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that methods load and loadLibrary call checkLink on security manager.",
            method = "loadLibrary",
            args = {java.lang.String.class}
        )
    })
    public void test_load() {
        final String library = "library";
        
        class CheckLinkCalledException extends RuntimeException {}
        
        class TestSecurityManager extends SecurityManager {
            @Override
            public void checkLink(String lib){
                if(library.equals(lib)){
                    throw new CheckLinkCalledException();
                }
                super.checkLink(lib);
            }

            @Override
            public void checkPermission(Permission p) {
                // nothing to do
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        try {
            System.load(library);
            fail("System.load must call checkLink on security manager with argument \"" + library + "\"");
        }
        catch(CheckLinkCalledException e){
            // ok
        }
        catch(Throwable t){
            fail("System.load must call checkLink on security manager with argument \"" + library + "\"");
        }
        
        try {
            System.loadLibrary(library);
            fail("System.loadLibrary must call checkLink on security manager with argument \"" + library + "\"");
        }
        catch(CheckLinkCalledException e){
            // ok
        }
        catch(Throwable t){
            fail("System.loadLibrary must call checkLink on security manager with argument \"" + library + "\"");
        }
        
    }
}
