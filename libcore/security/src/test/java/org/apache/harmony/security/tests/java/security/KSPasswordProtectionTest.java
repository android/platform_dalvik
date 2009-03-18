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
* @author Vera Y. Petrashkova
* @version $Revision$
*/

package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.security.KeyStore;

import javax.security.auth.DestroyFailedException;

import junit.framework.TestCase;
@TestTargetClass(KeyStore.PasswordProtection.class)
/**
 * Tests for <code>KeyStore.PasswordProtection</code> class constructor and methods
 * 
 */

public class KSPasswordProtectionTest extends TestCase {

    /**
     * Test for <code>KeyStore.PasswordProtection(char[] password)</code> constructor
     * and the following methods 
     * <code>getPassword()<code>
     * <code>destroy()<code>
     * <code>isDestroyed()<code>
     * Assertions: constructor created new PasswordProtection object
     * getPassword() returns password or throws IllegalArgumentException
     * if PasswordProtection is destroyed
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "PasswordProtection",
            args = {char[].class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getPassword",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "isDestroyed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.SUFFICIENT,
            notes = "",
            method = "destroy",
            args = {}
        )
    })
    public void testGetPassword() throws DestroyFailedException {
        char [] pass = {'a', 'b', 'c'};
        KeyStore.PasswordProtection ksPWP = new KeyStore.PasswordProtection(pass);
        char [] rPass = ksPWP.getPassword();
        assertFalse("PasswordProtection Should not be destroyed", ksPWP.isDestroyed());        
        assertEquals("Incorrect password length", pass.length, rPass.length);
        for (int i = 0; i < pass.length; i++) {
            assertEquals("Incorrect password (item: ".concat(Integer.toString(i))
                    .concat(")"), pass[i], rPass[i]);
        }
        ksPWP.destroy();
        assertTrue("PasswordProtection must be destroyed", ksPWP.isDestroyed());
        try {
            ksPWP.getPassword();
            fail("IllegalStateException must be thrown because PasswordProtection is destroyed");
        } catch (IllegalStateException e) {
        }
        
        try {
            ksPWP = new KeyStore.PasswordProtection(null);
        } catch (Exception e) {
            fail("Unexpected exception for NULL parameter");
        }
    }
}
