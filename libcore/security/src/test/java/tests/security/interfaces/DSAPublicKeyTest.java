/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.security.interfaces;
import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAParameterSpec;

@TestTargetClass(DSAPublicKey.class)
public class DSAPublicKeyTest extends TestCase {
    
    /**
     * @tests java.security.interfaces.DSAPublicKey 
     * #getY()
     * test covers following use cases
     *   Case 1: check with predefined p, q, g, x
     *   Case 2: check with random p, q, g, x. It takes some time (up to  
     *           minute)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getY",
        args = {}
    )
    @BrokenTest("Incorrect value was returned for method " +
                  "java.security.interfaces.DSAPublicKey.getY(). "+
                  "This test does not pass on the RI.")
    public void test_getY() throws Exception {
        KeyPairGenerator keyGen = null;
        KeyPair keys = null;
        DSAPrivateKey priv = null;
        DSAPublicKey publ = null;
        
        // Case 1: check with predefined p, q, g, x
        keyGen = KeyPairGenerator.getInstance("DSA");
        keyGen.initialize(new DSAParameterSpec(Util.P, Util.Q, Util.G),
                new SecureRandom(new MySecureRandomSpi(), null) {
                });
        keys = keyGen.generateKeyPair();
        priv = (DSAPrivateKey) keys.getPrivate();
        publ = (DSAPublicKey) keys.getPublic();
        assertEquals("Invalid Y value", (Util.G.modPow(Util.RND_RET, Util.P)),
                publ.getY());

        // Case 2: check with random p, q, g, x. It takes some time (up to 
        // minute)
        keyGen = KeyPairGenerator.getInstance("DSA");
        keys = keyGen.generateKeyPair();
        priv = (DSAPrivateKey) keys.getPrivate();
        publ = (DSAPublicKey) keys.getPublic();
        assertEquals("Invalid Y value", (priv.getParams().getG().modPow(
                priv.getX(), priv.getParams().getP())), publ.getY());
    }
}
