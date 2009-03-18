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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.Permission;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.io.RandomAccessFile
 */
@TestTargetClass(java.io.RandomAccessFile.class)
public class JavaIoRandomAccessFileTest extends TestCase {
    
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
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that RandomAccessFile constructor calls checkRead method of security manager.",
            method = "RandomAccessFile",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that RandomAccessFile constructor calls checkRead method of security manager.",
            method = "RandomAccessFile",
            args = {java.io.File.class, java.lang.String.class}
        )
    })
    public void test_RandomAccessFile1() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            String file;
            void reset(){
                called = false;
                file = null;
            }
            @Override
            public void checkRead(String file){
                called = true;
                this.file = file;
                super.checkRead(file);
            }
            @Override
            public void checkPermission(Permission p) {
                
            }
        }

        long id = new java.util.Date().getTime();
        String filename = "SecurityPermissionsTest_"+id;
        File f = File.createTempFile(filename, null);
        f.deleteOnExit();
        filename = f.getCanonicalPath();
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        new RandomAccessFile(filename, "r");
        assertTrue("RandomAccessFile(String,String) ctor must call checkRead on security manager", s.called);
        assertEquals("Argument of checkRead is not correct", filename, s.file);
        
        s.reset();
        new RandomAccessFile(f, "r");
        assertTrue("RandomAccessFile(File, String) ctor must call checkRead on security manager", s.called);
        assertEquals("Argument of checkRead is not correct", filename, s.file);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that RandomAccessFile constructor calls checkRead and checkWrite on security manager.",
        method = "RandomAccessFile",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_RandomAccessFile2() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean checkReadCalled;
            boolean checkWriteCalled;
            String checkReadFile;
            String checkWriteFile;
            
            void reset(){
                checkReadCalled = false;
                checkWriteCalled = false;
                checkReadFile = null;
                checkWriteFile = null;
            }
            
            @Override
            public void checkRead(String file) {
                checkReadCalled = true;
                this.checkReadFile = file;
                super.checkRead(file);
            }
            @Override
            public void checkWrite(String file) {
                checkWriteCalled = true;
                this.checkWriteFile = file;
                super.checkWrite(file);
            }
            @Override
            public void checkPermission(Permission p) {
                
            }
        }

        long id = new java.util.Date().getTime();
        String filename = "SecurityPermissionsTest_"+id;
        File f = File.createTempFile(filename, null);
        f.deleteOnExit();
        filename = f.getCanonicalPath();
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        new RandomAccessFile(filename, "rw");
        assertTrue("RandomAccessFile(String,String) ctor must call checkRead on security manager", s.checkReadCalled);
        assertTrue("RandomAccessFile(String,String) ctor must call checkWrite on security manager", s.checkWriteCalled);
        assertEquals("Argument of checkRead is not correct", filename, s.checkReadFile);
        assertEquals("Argument of checkWrite is not correct", filename, s.checkWriteFile);
    }
}
