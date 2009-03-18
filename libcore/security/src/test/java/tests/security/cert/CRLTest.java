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
* @author Vladimir N. Molotkov
* @version $Revision$
*/

package tests.security.cert;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.cert.CRL;
import java.security.cert.Certificate;

import org.apache.harmony.security.tests.support.SpiEngUtils;

/**
 * Tests for <code>java.security.cert.CRL</code> fields and methods
 * 
 */
@TestTargetClass(CRL.class)
public class CRLTest extends TestCase {

    public static final String[] validValues = { "X.509", "x.509" };
    
    private final static String[] invalidValues = SpiEngUtils.invalidValues;
    
    //
    // Tests
    //

    /**
     * Test for <code>CRL(String type)</code> constructor<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "CRL",
        args = {java.lang.String.class}
    )
    public final void testConstructor() {
        for (int i = 0; i< validValues.length; i++) {
            CRL crl = new MyCRL(validValues[i]);
            assertEquals(validValues[i], crl.getType());
        }
        
        for (int i = 0; i< invalidValues.length; i++) {
            CRL crl = new MyCRL(invalidValues[i]);
            assertEquals(invalidValues[i], crl.getType());
        }
        
        try {
            CRL crl = new MyCRL(null);
        } catch (Exception e) {
            fail("Unexpected exception for NULL parameter");
        }
    }

    
    /**
     * Test #1 for <code>getType()</code> method<br>
     * Assertion: returns <code>CRL</code> type
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getType",
        args = {}
    )
    public final void testGetType01() {
        CRL crl = new MyCRL("TEST_TYPE");
        assertEquals("TEST_TYPE", crl.getType());
    }

    /**
     * Test #2 for <code>getType()</code> method<br>
     * Assertion: returns <code>CRL</code> type
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "getType",
        args = {}
    )
    public final void testGetType02() {
        CRL crl = new MyCRL(null);
        assertNull(crl.getType());
    }

    //
    // the following tests just call methods
    // that are abstract in <code>Certificate</code>
    // (So they just like signature tests)
    //

    /**
     * Test for <code>toString()</code> method
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public final void testToString() {
        CRL crl = new MyCRL("TEST_TYPE");
        crl.toString();
    }

    /**
     * Test for <code>isRevoked()</code> method
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isRevoked",
        args = {java.security.cert.Certificate.class}
    )
    public final void testIsRevoked() {
        CRL crl = new MyCRL("TEST_TYPE");
        crl.isRevoked(null);
    }

    class MyCRL extends CRL {

        protected MyCRL(String type) {
            super(type);
        }

        @Override
        public boolean isRevoked(Certificate cert) {
            return false;
        }

        @Override
        public String toString() {
            return null;
        }
        
    }
}
