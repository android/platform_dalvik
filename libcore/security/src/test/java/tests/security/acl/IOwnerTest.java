/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.security.acl;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.acl.Owner;
import java.security.Principal;
import java.security.acl.NotOwnerException;
import java.security.acl.LastOwnerException;

import org.apache.harmony.security.tests.support.acl.*;

@TestTargetClass(Owner.class)
public class IOwnerTest extends TestCase {
    
    class MyOwner extends OwnerImpl {
        public MyOwner(Principal pr) {
            super(pr);
        }
    }
    
    /**
     * @tests java.security.acl.Owner#isOwner(Principal owner)
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isOwner",
        args = {java.security.Principal.class}
    )
    public void test_isOwner() {
        MyOwner mo = new MyOwner(new PrincipalImpl("NewOwner"));
        try {
            assertFalse("Method returns TRUE", mo.isOwner(new PrincipalImpl("TestOwner")));
            assertTrue("Method returns FALSE", mo.isOwner(new PrincipalImpl("NewOwner")));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
    }
    
    /**
     * @tests java.security.acl.Owner#addOwner(Principal caller, Principal owner)
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addOwner",
        args = {java.security.Principal.class, java.security.Principal.class}
    )
    public void test_addOwner() {
        Principal p1 = new PrincipalImpl("Owner");
        Principal p2 = new PrincipalImpl("AclOwner");
        Principal pt = new PrincipalImpl("NewOwner");
        MyOwner mo = new MyOwner(p1);
        try {
            //add new owner - TRUE expected
            assertTrue("Method returns FALSE", mo.addOwner(p1, pt));
            //add existent owner - FALSE expected
            assertFalse("Method returns TRUE", mo.addOwner(p1, pt));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
        //exception case
        try {
            mo.addOwner(p2, pt);
            fail("NotOwnerException was not thrown");
        } catch (NotOwnerException noe) {
            //expected
        }
    }
    
    /**
     * @tests java.security.acl.Owner#deleteOwner(Principal caller, Principal owner)
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "deleteOwner",
        args = {java.security.Principal.class, java.security.Principal.class}
    )
    public void test_deleteOwner() {
        Principal caller = new PrincipalImpl("Owner");
        Principal owner1 = new PrincipalImpl("NewOwner1");
        Principal owner2 = new PrincipalImpl("NewOwner2");
        Principal notCaller = new PrincipalImpl("AclOwner");
        MyOwner mo = new MyOwner(caller);
        
        try {
            if (!mo.isOwner(owner1))  mo.addOwner(caller, owner1);
            if (!mo.isOwner(owner2))  mo.addOwner(caller, owner2);
        } catch (Exception e) {
            fail("Unexpected exception " + e + " was thrown for addOwner");
        }
        
        try {
            //remove existent owner - TRUE expected
            assertTrue("Method returns FALSE", mo.deleteOwner(caller, owner1));
            assertFalse("Object presents in the owner list", mo.isOwner(owner1));
            //remove owner which is not part of the list of owners - FALSE expected
            assertFalse("Method returns TRUE", mo.deleteOwner(caller, owner1));
            assertTrue("Method returns FALSE", mo.deleteOwner(caller, owner2));
        } catch (Exception ex) {
            fail("Unexpected exception " + ex);
        }
        //exception case - NotOwnerException
        try {
            mo.deleteOwner(notCaller, owner1);
            fail("NotOwnerException was not thrown");
        } catch (NotOwnerException noe) {
            //expected
        } catch (Exception e) {
            fail(e + " was thrown instead of NotOwnerException");
        }
        //exception case - LastOwnerException
        try {
            mo.deleteOwner(caller, owner2);
            fail("LastOwnerException was not thrown");
        } catch (LastOwnerException loe) {
            //expected
        } catch (Exception e) {
            fail(e + " was thrown instead of LastOwnerException");
        }
    }
}