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

import junit.framework.TestCase;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.spec.DSAParameterSpec;

@TestTargetClass(DSAParams.class)
public class DSAParamsTest extends TestCase {
    
    private final BigInteger p = new BigInteger("4");
    private final BigInteger q = BigInteger.TEN;
    private final BigInteger g = BigInteger.ZERO;
    
    /**
     * @tests java.security.interfaces.DSAParams
     * #getG()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getG",
        args = {}
    )
    public void test_getG() {
        DSAParams params = new DSAParameterSpec(p, q, g);
        assertEquals("Invalid G", g, params.getG());
    }
    
    /**
     * @tests java.security.interfaces.DSAParams
     * #getP()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getP",
        args = {}
    )
    public void test_getP() {
        DSAParams params = new DSAParameterSpec(p, q, g);
        assertEquals("Invalid P", p, params.getP());
    }
    
    /**
     * @tests java.security.interfaces.DSAParams
     * #getQ()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getQ",
        args = {}
    )
    public void test_getQ() {
        DSAParams params = new DSAParameterSpec(p, q, g);
        assertEquals("Invalid Q", q, params.getQ());
    }
}
