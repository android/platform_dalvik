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

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

//import org.apache.harmony.security.tests.support.acl.PrincipalImpl;

import java.security.AccessControlContext;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class javax.security.auth.Subject
 */
@TestTargetClass(
        value = javax.security.auth.Subject.class,
        untestedMethods = {
            @TestTargetNew(
                    level = TestLevel.NOT_FEASIBLE,
                    notes = "Spec not specific enough for black-box testing",
                    method = "toString",
                    args = {}
                  )
        }
)
public class JavaxSecurityAuthSubject extends TestCase {
    
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
        notes = "Verifies that getSubject() method calls checkPermission method of security permissions.",
        method = "getSubject",
        args = {java.security.AccessControlContext.class}
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
                        && "getSubject".equals(permission.getName())) {
                    called = true;
                }
            }
        }

        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[0]);

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Subject.getSubject(acc);
        assertTrue(
                "javax.security.auth.Subject.getSubject() must call checkPermission on security manager",
                s.called);
    }
    
    @TestTargets ({
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that setReadOnly() calls checkPermission on security manager.",
        method = "getSubject",
        args = {java.security.AccessControlContext.class}
      ),
        @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that setReadOnly() calls checkPermission on security manager.",
        method = "setReadOnly",
        args ={}
      )
    })
    public void test_setReadOnly() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;

            void reset() {
                called = false;
            }

            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof AuthPermission
                        && "setReadOnly".equals(permission.getName())) {
                    called = true;
                }
            }
        }

        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[0]);
        Subject subject = new Subject();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        subject.setReadOnly();
        assertTrue(
                "javax.security.auth.Subject.setReadOnly() must call checkPermission on security manager",
                s.called);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that doAs() calls checkPermission on security manager.",
            method = "doAs",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedAction.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that doAs() calls checkPermission on security manager.",
            method = "doAs",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedExceptionAction.class}
        )
    })
    public void test_doAsCheckPermission() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;

            void reset() {
                called = false;
            }

            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof AuthPermission
                        && "doAs".equals(permission.getName())) {
                    called = true;
                }
            }
        }

        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[0]);
        Subject subject = new Subject();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        Subject.doAs(subject, new PrivilegedAction<Object>(){
            public Object run() {
                return null;
            }
        });
        assertTrue(
                "javax.security.auth.Subject.doAs must call checkPermission on security manager",
                s.called);

        s.reset();
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Object>(){
                public Object run() throws Exception {
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
        }
        assertTrue(
                "javax.security.auth.Subject.doAs must call checkPermission on security manager",
                s.called);
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Exception checking missing",
            method = "doAs",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedAction.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Exception checking missing",
            method = "doAs",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedExceptionAction.class}
        )
    })
    public void testDoAs() {
        
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that doAsPrivileged() calls checkPermission on security manager.",
            method = "doAsPrivileged",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedAction.class, java.security.AccessControlContext.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that doAsPrivileged() calls checkPermission on security manager.",
            method = "doAsPrivileged",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedExceptionAction.class, java.security.AccessControlContext.class}
        )
    })
    public void test_doAsPrivilegedCheckPermission() {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;

            void reset() {
                called = false;
            }

            @Override
            public void checkPermission(Permission permission) {
                if (permission instanceof AuthPermission
                        && "doAsPrivileged".equals(permission.getName())) {
                    called = true;
                }
            }
        }

        AccessControlContext acc = new AccessControlContext(
                new ProtectionDomain[0]);
        Subject subject = new Subject();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);

        s.reset();
        Subject.doAsPrivileged(subject, new PrivilegedAction<Object>() {
            public Object run() {
                return null;
            }
        }, acc);
        assertTrue(
                "javax.security.auth.Subject.doAsPrivileged must call checkPermission on security manager",
                s.called);

        s.reset();
        try {
            Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return null;
                }
            }, acc);
        } catch (PrivilegedActionException e) {
        }
        assertTrue(
                "javax.security.auth.Subject.doAsPrivileged must call checkPermission on security manager",
                s.called);
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "",
            method = "doAsPrivileged",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedAction.class, java.security.AccessControlContext.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "",
            method = "doAsPrivileged",
            args = {javax.security.auth.Subject.class, java.security.PrivilegedExceptionAction.class, java.security.AccessControlContext.class}
        )
    })
    public void doAsPrivileged() {
        
    }
    
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.TODO,
                notes = "",
                method = "isReadOnly",
                args = {}
    ),
        @TestTargetNew(
                level = TestLevel.TODO,
                notes = "",
                method = "setReadOnly",
                args = {}
              )
    })
    public void testSetGetIsReadonly() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPrincipals",
      args = {}
    )
    public void testGetPrincipals() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPrincipals",
      args = {java.lang.Class.class}
    )
    public void testGetPrincipalsClass() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPrivateCredentials",
      args = {}
    )
    public void testgetPrivateCredentials() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPrivateCredentials",
      args = {java.lang.Class.class}
    )
    public void testgetPrivateCredentialsClass() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPublicCredentials",
      args = {}
    )
    public void testgetPublicCredentials() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getPublicCredentials",
      args = {java.lang.Class.class}
    )
    public void testgetPublicCredentialsClass() {
          
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "getSubject",
      args = {java.security.AccessControlContext.class}
    )
    public void testgetSubject() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "hashCode",
      args = {}
    )
    public void testHashCode() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "",
      method = "equals",
      args = {java.lang.Object.class}
    )
    public void testEquals() {
        
    }
    
    @TestTargetNew(
      level = TestLevel.PARTIAL,
      notes = "test only started please continue",
      method = "Subject",
      args = {}
    )
    public void testConstructorDefault() {
        Subject s = new Subject();
        assertEquals(0,s.getPrincipals().size());
        assertEquals(0,s.getPrivateCredentials().size());
        assertEquals(0,s.getPublicCredentials().size());
    }
    
    @TestTargetNew(
      level = TestLevel.TODO,
      notes = "test only started please continue. Throws exception InvalidKeySpecException line 455",
      method = "Subject",
      args = {boolean.class, java.util.Set.class, java.util.Set.class, java.util.Set.class}
    )
    public void testConstructor() throws NoSuchAlgorithmException, InvalidKeySpecException {
        /*
        Principal p = new PrincipalImpl("TestUser");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(new byte[]{(byte) 1, (byte) 2});
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = factory.generatePublic(spec);
        PrivateKey prKey = factory.generatePrivate(spec);
        
        Set<PublicKey> pubKeySet = new HashSet<PublicKey>();
        pubKeySet.add(pubKey);
        Set<PrivateKey> prKeySet = new HashSet<PrivateKey>();
        prKeySet.add(prKey);
        Set<Principal> pSet = new HashSet<Principal>();
        pSet.add(p);
        
        //positive test
        Subject s = new Subject(true,pSet,pubKeySet,prKeySet);
        assertTrue(s.isReadOnly())
        
        //readonly false
        //TODO continue here
        
        //wrong principal
        
        */
        
 ;   }
    

}
