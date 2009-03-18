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

import java.security.acl.Group;
import java.security.Principal;
import java.util.Enumeration;

import org.apache.harmony.security.tests.support.acl.*;

@TestTargetClass(Group.class)
public class IGroupTest extends TestCase {
    
    class MyGroup extends GroupImpl {
        public MyGroup(String str) {
            super(str);
        }
    }
    
    /**
     * @tests java.security.acl.Group#addMember(Principal user)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "addMember",
            args = {java.security.Principal.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "isMember",
            args = {java.security.Principal.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "removeMember",
            args = {java.security.Principal.class}
        )
    })
    public void test_addMember() {
        MyGroup gr = new MyGroup("TestOwners");
        Principal pr = new PrincipalImpl("TestPrincipal");
        try {
            assertTrue(gr.addMember(pr));
            assertFalse(gr.addMember(pr));
            assertTrue(gr.isMember(pr));
            assertTrue(gr.removeMember(pr));
            assertFalse(gr.isMember(pr));
            assertFalse(gr.removeMember(pr));
        } catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }
    
    /**
     * @tests java.security.acl.Group#members()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "members",
        args = {}
    )
    public void test_members() {
        MyGroup gr = new MyGroup("TestOwners");
        Principal pr = new PrincipalImpl("TestPrincipal");
        try {
            Enumeration en = gr.members();
            assertFalse("Not empty enumeration", en.hasMoreElements());
            assertTrue(gr.addMember(pr));
            assertTrue("Empty enumeration", en.hasMoreElements());
        } catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }
}