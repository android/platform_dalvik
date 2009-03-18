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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Permission;

/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.io.FileInputStream and java.io.FileOutputStream
 */
@TestTargetClass(java.io.FileInputStream.class)
public class JavaIoFileInputStreamTest extends TestCase {
    
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
            notes = "Verifies that FileInputStream() constructor calls checkRead method on security manager.",
            method = "FileInputStream",
            args = {java.io.FileDescriptor.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that FileInputStream() constructor calls checkRead method on security manager.",
            method = "FileInputStream",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that FileInputStream() constructor calls checkRead method on security manager.",
            method = "FileInputStream",
            args = {java.io.File.class}
        )
    })
    public void test_FileInputStream() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called;
            String file;
            FileDescriptor fd;
            void reset(){
                called = false;
                file = null;
                fd = null;
            }
            @Override
            public void checkRead(FileDescriptor fd) {
                called = true;
                this.fd = fd;
                super.checkRead(fd);
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
        FileDescriptor fd = new FileDescriptor();
        new FileInputStream(fd);
        assertTrue("FileInputStream(FileDescriptor) ctor must call checkRead on security manager", s.called);
        assertEquals("Argument of checkRead is not correct", fd, s.fd);
        
        s.reset();
        new FileInputStream(filename);
        assertTrue("FileInputStream(String) ctor must call checkRead on security manager", s.called);
        assertEquals("Argument of checkRead is not correct", filename, s.file);
        
        s.reset();
        new FileInputStream(f);
        assertTrue("FileInputStream(File) ctor must call checkRead on security manager", s.called);
        assertEquals("Argument of checkRead is not correct", filename, s.file);
    }
    
}
