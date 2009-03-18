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

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.ProtectionDomain;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class javax.security.auth.SubjectDomainCombiner
 */
@TestTargetClass(javax.security.auth.SubjectDomainCombiner.class)
public class JavaxSecurityAuthSubjectDomainCombiner extends TestCase {
    
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
        notes = "Verifies that getSubject() calls checkPermission on security permissions.",
        method = "getSubject",
        args = {}
    )
    public void test_getSubject() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;

            void reset() {
                called = false;
            }

            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof AuthPermission
                        && "getSubjectFromDomainCombiner".equals(permission.getName())) {
                    called = true;
                }
            }
        }

        Subject subject = new Subject();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        SubjectDomainCombiner sdc = new SubjectDomainCombiner(subject);
        sdc.getSubject();
        assertTrue(
                "javax.security.auth.SubjectDomainCombiner.getSubject() must call checkPermission on security manager",
                s.called);
    }
}
