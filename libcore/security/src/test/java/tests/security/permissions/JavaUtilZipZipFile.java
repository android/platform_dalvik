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
import java.io.File;
import java.util.zip.ZipFile;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.util.zip.ZipFile
 */
@TestTargetClass(java.util.zip.ZipFile.class)
public class JavaUtilZipZipFile extends TestCase {
    
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
        notes = "Verifies that the constructor java.util.zip.ZipFile() calls checkRead on the security manager.",
        method = "ZipFile",
        args = {java.lang.String.class}
    )
    public void test_ZipFile() throws IOException {
        class TestSecurityManager extends SecurityManager {
            private boolean called = false;
            private String name = null;
            void reset(){
                called = false;
                name = null;
            }
            String getName(){return name;}
            @Override
            public void checkRead(String name) {
                called = true;
                this.name = name;
                super.checkRead(name);
            }
        }
        
        File file = File.createTempFile("foo", "zip");
        String filename = file.getAbsolutePath();
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        new ZipFile(filename);
        assertTrue("java.util.zip.ZipFile() construcor must call checkRead on security permissions", s.called);
        assertEquals("Argument of checkPermission is not correct", filename, s.getName());
    }
}
