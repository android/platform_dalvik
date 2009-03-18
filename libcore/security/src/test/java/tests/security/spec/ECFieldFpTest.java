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

package tests.security.spec;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.math.BigInteger;
import java.security.spec.ECFieldFp;

/**
 * Tests for <code>ECFieldFp</code> class fields and methods.
 * 
 */
@TestTargetClass(ECFieldFp.class)
public class ECFieldFpTest extends TestCase {

    //
    // Tests
    //

    /**
     * Test #1 for <code>ECFieldFp</code> constructor
     *
     * Assertion: creates new object of <code>ECFieldFp</code> class
     * using valid <code>p</code> (odd prime)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "ECFieldFp",
        args = {java.math.BigInteger.class}
    )
    public final void testECFieldFp01() {
        new ECFieldFp(BigInteger.valueOf(23L));
    }

    /**
     * Test #2 for <code>ECFieldFp</code> constructor
     * 
     * Assertion: creates new object of <code>ECFieldFp</code> class
     * using valid <code>p</code> (odd but not prime)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "ECFieldFp",
        args = {java.math.BigInteger.class}
    )
    public final void testECFieldFp02() {
        new ECFieldFp(BigInteger.valueOf(21L));
    }

    /**
     * Test #3 for <code>ECFieldFp</code> constructor
     * 
     * Assertion: IllegalArgumentException if <code>p</code> is not positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "ECFieldFp",
        args = {java.math.BigInteger.class}
    )
    public final void testECFieldFp03() {
        try {
            new ECFieldFp(BigInteger.valueOf(-1L)); 
            fail(getName() +
                    " FAILED: expected exception has not been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test #4 for <code>ECFieldFp</code> constructor
     * 
     * Assertion: IllegalArgumentException if <code>p</code> is not positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "ECFieldFp",
        args = {java.math.BigInteger.class}
    )
    public final void testECFieldFp04() {
        try {
            new ECFieldFp(BigInteger.valueOf(0L));
            fail(getName() +
                    " FAILED: expected exception has not been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test #4 for <code>ECFieldFp</code> constructor
     * 
     * Assertion: NullPointerException if <code>p</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "ECFieldFp",
        args = {java.math.BigInteger.class}
    )
    public final void testECFieldFp05() {
        try {
            new ECFieldFp(null);
            fail(getName() +
                    " FAILED: expected exception has not been thrown");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test #1 for <code>hashCode()</code> method.<br>
     *
     * Assertion: must return the same value if invoked
     * repeatedly on the same object. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public final void testHashCode01() {
        ECFieldFp f = new ECFieldFp(BigInteger.valueOf(23L));
        int hc = f.hashCode();
        assertTrue(hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode() &&
                   hc == f.hashCode());
    }

    /**
     * Test #2 for <code>hashCode()</code> method.<br>
     *
     * Assertion: must return the same value if invoked
     * on equal (according to the <code>equals(Object)</code> method) objects. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public final void testHashCode02() {
        assertTrue(new ECFieldFp(BigInteger.valueOf(23L)).hashCode() ==
                   new ECFieldFp(BigInteger.valueOf(23L)).hashCode());
    }

    /**
     * Test for <code>getFieldSize()()</code> method.<br>
     *
     * Assertion: returns field size in bits which is prime size
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getFieldSize",
        args = {}
    )
    public final void testGetFieldSize() {
        assertEquals(5, new ECFieldFp(BigInteger.valueOf(23L)).getFieldSize());
    }

    /**
     * Test for <code>getP()</code> method.<br>
     *
     * Assertion: returns prime 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getP",
        args = {}
    )
    public final void testGetP() {
        BigInteger p = BigInteger.valueOf(23L);
        assertTrue(p.equals(new ECFieldFp(p).getP()));
    }

    /**
     * Test #1 for <code>equals()</code> method.<br>
     *
     * Assertion: object equals to itself. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject01() {
        ECFieldFp obj = new ECFieldFp(BigInteger.valueOf(23L));
        assertTrue(obj.equals(obj));
    }

    /**
     * Test #2 for <code>equals(Object obj)</code> method.<br>
     *
     * Assertion: returns false if <code>obj</code> is <code>null</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject02() {
        assertFalse(new ECFieldFp(BigInteger.valueOf(23L)).equals(null));
    }

    /**
     * Test #3 for <code>equals(Object obj)</code> method.<br>
     *
     * Assertion: returns false if <code>obj</code>
     * is not instance of <code>ECFieldFp</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject03() {
        assertFalse(new ECFieldFp(BigInteger.valueOf(23L)).equals(new Object()));
    }

    /**
     * Test #4 for <code>equals()</code> method.<br>
     *
     * Assertion: true if prime values match. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject04() {
        assertTrue(new ECFieldFp(BigInteger.valueOf(23L)).equals(
                   new ECFieldFp(BigInteger.valueOf(23L))));
    }

}
