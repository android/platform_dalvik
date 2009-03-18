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

import java.security.acl.Permission;

import org.apache.harmony.security.tests.support.acl.*;

@TestTargetClass(Permission.class)
public class IPermissionTest extends TestCase {
    
    class MyPermission extends PermissionImpl {
        public MyPermission(String str) {
            super(str);
        }
    }    
    
    /**
     * @tests java.security.acl.Permission#equals(Object another)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void test_equals() {
        try {
            MyPermission mp1 = new MyPermission("TestPermission");
            MyPermission mp2 = new MyPermission("NewTestPermission");
            Object another = new Object();
            assertFalse(mp1.equals(another));
            assertFalse(mp1.equals(mp2));
            assertTrue(mp1.equals(new MyPermission("TestPermission")));
        } catch (Exception e) {
            fail("Unexpected exception - subtest1");
        }
    }
    
    /**
     * @tests java.security.acl.Permission#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        try {
            MyPermission obj = new MyPermission("TestPermission");
            String res = obj.toString();
            assertEquals(res, "TestPermission");
        } catch (Exception e) {
            fail("Unexpected exception - subtest2");
        }
    }
}