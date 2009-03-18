/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package tests.api.javax.security.auth;

import java.security.Permission;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import javax.security.auth.PrivateCredentialPermission;


/**
 * Tests for <code>PrivateCredentialPermission</code> class constructors and methods.
 * 
 */
@TestTargetClass(PrivateCredentialPermission.class) 
public class PrivateCredentialPermissionTest extends TestCase {
    
    private final static String cred_class1 = "a.b.Credential";
    private final static String cred_class2 = "a.b.Credential1";
    private final static String name1 = cred_class1 + " a.b.Principal \"*\"";
    private final static String name2 = cred_class1 + " a.c.Principal \"*\"";
    private final static String name4 = cred_class2 + " a.c.Principal \"*\"";
    private final static String pc1 = "a.b.Principal";
    private final static String pn1 = "*";
    private final static String pc2 = "a.c.Principal";
    private final static String pn2 = "abc";
    
    private final static String name3 = cred_class1 + " " + pc1 + " \"" + pn1 + "\" " + pc2 + " \"" + pn2 + "\"";

    /**
     * @tests javax.security.auth.PrivateCredentialPermission#PrivateCredentialPermission(String name, String actions) 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrivateCredentialPermission",
        args = {String.class, String.class}
    )
    public void test_Constructor_01() {
        PrivateCredentialPermission ap = new PrivateCredentialPermission(name1, "read");
        
        String actions[] = { "write", "", null };
        
        for(int i = 0; i < actions.length; i++) {
            try {
                ap = new PrivateCredentialPermission(name1, "write");
                fail("expected IllegalArgumentException if action is not \"read\"");
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
        
        String names[] = { null, 
                           "",
                           "a.b.Credential a.c.Principal *\"",
                           "a.b.Credential_a.c.Principal_\"*\"",
                           "a.b.Credential a.c.Principal_\"*\"",
                           "a.b.Credential * \"a\""
                          };
        
        for(int i = 0; i < names.length; i++) {
            try {
                ap = new PrivateCredentialPermission(names[i], "read");
                fail("expected IllegalArgumentException for malformed \"name\" argument (" + names[i] +")");
            } catch (IllegalArgumentException e) {
                // expected
            } catch (NullPointerException npe) {
                if (names[i] != null)
                    throw npe;
                else
                    ;     // expected if name is null
            }
        }
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#getActions() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getActions",
        args = {}
    )
    public void test_getActions() {
        PrivateCredentialPermission ap = new PrivateCredentialPermission(name1, "read");
        assertEquals("getActions() must alway return \"read\"", "read", ap.getActions());
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#implies() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "implies",
        args = { Permission.class }
    )
    public void test_implies() {
        PrivateCredentialPermission p1 = new PrivateCredentialPermission("* P1 \"abc\"", "read");
        PrivateCredentialPermission p2 = new PrivateCredentialPermission("a.b.Credential P1 \"abc\"", "read");
        PrivateCredentialPermission p3 = new PrivateCredentialPermission("C1 P1 \"abc\"", "read");
        PrivateCredentialPermission p4 = new PrivateCredentialPermission("C1 P1 \"abc\" P2 \"abcd\"", "read");
        PrivateCredentialPermission p5 = new PrivateCredentialPermission("C1 P1 \"*\"", "read");
        PrivateCredentialPermission p6 = new PrivateCredentialPermission("a.b.Credential * \"*\"", "read");
        PrivateCredentialPermission p7 = new PrivateCredentialPermission("a.b.Credential P2 \"abc\"", "read");
        PrivateCredentialPermission p8 = new PrivateCredentialPermission("a.b.Credential1 P2 \"abc\"", "read");
        PrivateCredentialPermission p9 = new PrivateCredentialPermission("a.b.Credential1 P2 \"*\"", "read");
        
        PrivateCredentialPermission[][] arr = { { p1, p2 },
                                                { p2, p1 },
                                                { p3, p4 },
                                                { p5, p3 },
                                                { p6, p2 },
                                                { p2, p7 },
                                                { p7, p8 },
                                                { p8, p9 }};
        
        boolean[] r = { true, false, true, true, true, false, false, false };
        for(int i = 0; i < arr.length; i++)
            assertEquals("implies() returned wrong result (" + i + ")", r[i], arr[i][0].implies(arr[i][1]));
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#getCredentialClass() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getCredentialClass",
        args = {}
    )
    public void test_getCredentialClass() {
        PrivateCredentialPermission ap = new PrivateCredentialPermission(name1, "read");
        assertEquals("getCredentialClass() returned wrong name", cred_class1, ap.getCredentialClass());
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#getPrincipals() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPrincipals",
        args = {}
    )
    public void test_getPrincipals() {
       
        PrivateCredentialPermission ap = new PrivateCredentialPermission(name3, "read");
        String[][] p = ap.getPrincipals();
        
        assertEquals("wrong number of principals", 2, p.length);
        
        assertEquals("wrong principal class 0", pc1, p[0][0]);
        assertEquals("wrong principal name 0", pn1, p[0][1]);
        
        assertEquals("wrong principal class 1", pc2, p[1][0]);
        assertEquals("wrong principal name 1", pn2, p[1][1]);
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#equals() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = { Object.class }
    )
    public void test_equals() {
        PrivateCredentialPermission p1 = new PrivateCredentialPermission(name3, "read");
        PrivateCredentialPermission p2 = new PrivateCredentialPermission(name3, "read");
        PrivateCredentialPermission p3 = new PrivateCredentialPermission(name1, "read");
        PrivateCredentialPermission p4 = new PrivateCredentialPermission(name1, "read");
        PrivateCredentialPermission p5 = new PrivateCredentialPermission(name2, "read");
        PrivateCredentialPermission p6 = new PrivateCredentialPermission(name4, "read");
        
        PrivateCredentialPermission arr[][] = { { p1, p2 }, 
                                                { p3, p4 },
                                                { p4, p5 },
                                                { p1, p3 },
                                                { p4, p6 } };
        boolean r[] = { true, true, false, false, false };
        
        for(int i = 0; i < arr.length; i++) {
            assertEquals("equals() returned wrong result", r[i], arr[i][0].equals(arr[i][1]));
        }
        
        try {
            assertFalse(p1.equals(null));
        } catch(NullPointerException npe) {
            
        }
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#hashCode() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void test_hashCode() {
        PrivateCredentialPermission p1 = new PrivateCredentialPermission(name1, "read");
        PrivateCredentialPermission p2 = new PrivateCredentialPermission(name1, "read");
        int arr[][] = new int[10][];
        for(int i = 0; i < 10; i++) {
            int h1 = p1.hashCode();
            
            System.gc();
            
            // force some memory allocations
            arr[i] = new int[50000];
            
            assertEquals("hashCode() must consistently return the same integer", h1, p1.hashCode());
            assertEquals("hashCode() must be the same for equal PrivateCredentialPermission objects", p1.hashCode(), p2.hashCode());
        }
        
        
        PrivateCredentialPermission p3 = new PrivateCredentialPermission(name4, "read");
        assertFalse("hashCode() must not be the same for non-equal PrivateCredentialPermission objects", p1.hashCode() == p3.hashCode());
    }
    
    /**
     * @tests javax.security.auth.PrivateCredentialPermission#newPermissionCollection() 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "newPermissionCollection",
        args = {}
    )
    public void test_newPermissionCollection() {
        PrivateCredentialPermission ap = new PrivateCredentialPermission(name1, "read");
        assertNull("newPermissionCollection must always return null", ap.newPermissionCollection());
    }
    
}

