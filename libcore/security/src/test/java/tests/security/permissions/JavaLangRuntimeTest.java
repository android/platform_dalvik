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

import java.io.IOException;
import java.security.Permission;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.lang.Runtime
 */
@TestTargetClass(java.lang.Runtime.class)
public class JavaLangRuntimeTest extends TestCase {
    
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
            notes = "Verifies that Runtime.exec calls checkExec method on security manager",
            method = "exec",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that Runtime.exec calls checkExec method on security manager",
            method = "exec",
            args = {java.lang.String.class, java.lang.String[].class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that Runtime.exec calls checkExec method on security manager",
            method = "exec",
            args = {java.lang.String[].class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that Runtime.exec calls checkExec method on security manager",
            method = "exec",
            args = {java.lang.String[].class, java.lang.String[].class}
        )
    })
    public void test_exec() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            String cmd;
            void reset(){
                called = false;
                cmd = null;
            }
            @Override
            public void checkExec(String cmd) {
                called = true;
                this.cmd = cmd;
            }
            @Override
            public void checkPermission(Permission p) {
                
            }
        }
        
        String cmd = "ls";
        String arg = "-al";
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        Runtime.getRuntime().exec(cmd);
        assertTrue("Runtime.exec must call checkExcec on security manager", s.called);
        assertEquals("Argument of checkExec is not correct", cmd, s.cmd);
        
        s.reset();
        Runtime.getRuntime().exec(cmd, null);
        assertTrue("Runtime.exec must call checkExcec on security manager", s.called);
        assertEquals("Argument of checkExec is not correct", cmd, s.cmd);
        
        s.reset();
        Runtime.getRuntime().exec(new String[]{cmd, arg});
        assertTrue("Runtime.exec must call checkExcec on security manager", s.called);
        assertEquals("Argument of checkExec is not correct", cmd, s.cmd);
        
        s.reset();
        Runtime.getRuntime().exec(new String[]{cmd, arg}, null);
        assertTrue("Runtime.exec must call checkExcec on security manager", s.called);
        assertEquals("Argument of checkExec is not correct", cmd, s.cmd);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that addShutdownHook and remove ShutdownHook call checkPermission on security manager., disabled due to implementation bug, see ticket #55",
            method = "addShutdownHook",
            args = {java.lang.Thread.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that addShutdownHook and remove ShutdownHook call checkPermission on security manager., disabled due to implementation bug, see ticket #55",
            method = "removeShutdownHook",
            args = {java.lang.Thread.class}
        )
    })
    public void test_shutdownHook() {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            Permission permission;
            void reset(){
                called = false;
                permission = null;
            }
            @Override
            public void checkPermission(Permission permission){
                if(permission instanceof RuntimePermission){
                    called = true;
                    this.permission = permission;
                }
            }
        }
        
        Thread hook = new Thread(){};

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Runtime.getRuntime().addShutdownHook(hook);
        assertTrue("Runtime.addShutdownHook must call checkPermission on security manager with a RuntimePermission", s.called);
        assertEquals("Name of RuntimePermission passed to checkPermission is not correct", "shutdownHooks", s.permission.getName());

        s.reset();
        Runtime.getRuntime().removeShutdownHook(hook);
        assertTrue("Runtime.removeShutdownHook must call checkPermission on security manager with a RuntimePermission", s.called);
        assertEquals("Name of RuntimePermission passed to checkPermission is not correct", "shutdownHooks", s.permission.getName());
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
                
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        try {
            Runtime.getRuntime().exit(11);
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
        notes = "Verifies that runFinalizersOnExit calls checkExit on security manager., disabled due to implementation bug, see ticket #55",
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
                
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Runtime.runFinalizersOnExit(true);
        assertTrue("Runtime.runFinalizersOnExit must call checkExit on security manager with a RuntimePermission", s.called);
        assertEquals("Argument of checkExit is not correct", 0, s.status);

        s.reset();
        Runtime.runFinalizersOnExit(false);
        assertTrue("Runtime.runFinalizersOnExit must call checkExit on security manager with a RuntimePermission", s.called);
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
            }
            @Override
            public void checkPermission(Permission p) {
                
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        try {
            Runtime.getRuntime().load(library);
            fail("System.load must call checkLink on security manager with argument "+library);
        }
        catch(CheckLinkCalledException e){
            // ok
        }
        catch(Throwable t){
            fail("System.load must call checkLink on security manager with argument "+library);
        }
        
        try {
            Runtime.getRuntime().loadLibrary(library);
            fail("System.load must call checkLink on security manager with argument "+library);
        }
        catch(CheckLinkCalledException e){
            // ok
        }
        catch(Throwable t){
            fail("System.load must call checkLink on security manager with argument "+library);
        }
    }

}
