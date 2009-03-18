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

/**
* @author Alexey V. Varlamov
* @version $Revision$
*/

package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;

import java.security.SecurityPermission;

import junit.framework.TestCase;
@TestTargetClass(SecurityPermission.class)
/**
 * Tests for <code>SecurityPermission</code>
 * 
 */
public class SecurityPermissionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SecurityPermissionTest.class);
    }

    /**
     * Check all constructors: an object is created with the specified valid name. 
     * If name equal null then NPE should be thrown. 
     * If  name is empty then IAE should be thrown. 
     * Action is ignored.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "SecurityPermission",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "SecurityPermission",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    public void testCtor()
    {
        String name = "basic123*$%#";
        SecurityPermission test = new SecurityPermission(name);
        assertEquals(name, test.getName());
        assertEquals("", test.getActions());
        test = new SecurityPermission(name, "#$!#12435");
        assertEquals(name, test.getName());
        assertEquals("", test.getActions());
        try{
            new SecurityPermission(null);
            fail("NPE is not thrown");
        }
        catch (NullPointerException ok){}
        
        try{
            new SecurityPermission(null, "ds235");
            fail("NPE is not thrown");
        }
        catch (NullPointerException ok){}
        
        try{
            new SecurityPermission("");
            fail("IAE is not thrown");
        }
        catch (IllegalArgumentException ok){}
        try{
            new SecurityPermission("", "ertre 3454");
            fail("IAE is not thrown");
        }
        catch (IllegalArgumentException ok){} 
    }
}
