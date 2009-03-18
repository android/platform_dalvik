/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.targets.security;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public abstract class KeyPairGeneratorTest extends TestCase {

    private final String algorithmName;
    private final TestHelper<KeyPair> helper;
    
    private KeyPairGenerator generator;

    protected KeyPairGeneratorTest(String algorithmName, TestHelper<KeyPair> helper) {
        this.algorithmName = algorithmName;
        this.helper = helper;
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        generator = getKeyPairGenerator();
    }
    
    private KeyPairGenerator getKeyPairGenerator() {
        try {
            return KeyPairGenerator.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            fail("cannot get KeyPairGenerator: " + e);
            return null;
        }
    }

    @TestTargets({
        @TestTargetNew(
                level = TestLevel.ADDITIONAL,
                method = "initialize",
                args = {int.class}
            ),
            @TestTargetNew(
                level = TestLevel.ADDITIONAL,
                method = "generateKeyPair",
                args = {}
            ),
            @TestTargetNew(
                level=TestLevel.COMPLETE,
                method="method",
                args={}
            )
    })
    public void testKeyPairGenerator() {
        generator.initialize(512);

        KeyPair keyPair = generator.generateKeyPair();

        assertNotNull("no keypair generated", keyPair);
        assertNotNull("no public key generated", keyPair.getPublic());
        assertNotNull("no private key generated", keyPair.getPrivate());

        helper.test(keyPair);
    }
}
