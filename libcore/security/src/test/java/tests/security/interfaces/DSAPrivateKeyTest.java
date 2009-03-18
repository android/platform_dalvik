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
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.BrokenTest;

import junit.framework.TestCase;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.DSAParameterSpec;

@TestTargetClass(DSAPrivateKey.class)
public class DSAPrivateKeyTest extends TestCase {
    
    /**
     * @tests java.security.interfaces.DSAPrivateKey 
     * #getX()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getX",
        args = {}
    )
    @BrokenTest("Incorrect value was returned for method " +
                  "java.security.interfaces.DSAPrivateKey.getX(). "+
                  "This test does not pass on the RI.")
    public void test_getX() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        keyGen.initialize(new DSAParameterSpec(Util.P, Util.Q, Util.G),
                new SecureRandom(new MySecureRandomSpi(), null) {                    
                });
        DSAPrivateKey key = (DSAPrivateKey) keyGen.generateKeyPair().getPrivate();
        assertEquals("Invalid X value", Util.RND_RET, key.getX());
    }
}
