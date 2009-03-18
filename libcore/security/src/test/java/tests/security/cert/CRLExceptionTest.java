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

package tests.security.cert;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.cert.CRLException;

/**
 * Tests for <code>CRLException</code> class constructors and methods.
 * 
 */
@TestTargetClass(CRLException.class)
public class CRLExceptionTest extends TestCase {

    private static String[] msgs = {
            "",
            "Check new message",
            "Check new message Check new message Check new message Check new message Check new message" };

    private static Throwable tCause = new Throwable("Throwable for exception");

    /**
     * Test for <code>CRLException()</code> constructor Assertion: constructs
     * CRLException with no detail message
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "CRLException",
        args = {}
    )
    public void testCRLException01() {
        CRLException tE = new CRLException();
        assertNull("getMessage() must return null.", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>CRLException(String)</code> constructor Assertion:
     * constructs CRLException with detail message msg. Parameter
     * <code>msg</code> is not null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CRLException",
        args = {java.lang.String.class}
    )
    public void testCRLException02() {
        CRLException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new CRLException(msgs[i]);
            assertEquals("getMessage() must return: ".concat(msgs[i]), tE
                    .getMessage(), msgs[i]);
            assertNull("getCause() must return null", tE.getCause());
        }
    }

    /**
     * Test for <code>CRLException(String)</code> constructor Assertion:
     * constructs CRLException when <code>msg</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "CRLException",
        args = {java.lang.String.class}
    )
    public void testCRLException03() {
        String msg = null;
        CRLException tE = new CRLException(msg);
        assertNull("getMessage() must return null.", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>CRLException(Throwable)</code> constructor Assertion:
     * constructs CRLException when <code>cause</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "CRLException",
        args = {java.lang.Throwable.class}
    )
    public void testCRLException04() {
        Throwable cause = null;
        CRLException tE = new CRLException(cause);
        assertNull("getMessage() must return null.", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>CRLException(Throwable)</code> constructor Assertion:
     * constructs CRLException when <code>cause</code> is not null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CRLException",
        args = {java.lang.Throwable.class}
    )
    public void testCRLException05() {
        CRLException tE = new CRLException(tCause);
        if (tE.getMessage() != null) {
            String toS = tCause.toString();
            String getM = tE.getMessage();
            assertTrue("getMessage() should contain ".concat(toS), (getM
                    .indexOf(toS) != -1));
        }
        assertNotNull("getCause() must not return null", tE.getCause());
        assertEquals("getCause() must return ".concat(tCause.toString()), tE
                .getCause(), tCause);
    }

    /**
     * Test for <code>CRLException(String, Throwable)</code> constructor
     * Assertion: constructs CRLException when <code>cause</code> is null
     * <code>msg</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as parameters.",
        method = "CRLException",
        args = {java.lang.String.class, java.lang.Throwable.class}
    )
    public void testCRLException06() {
        CRLException tE = new CRLException(null, null);
        assertNull("getMessage() must return null", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>CRLException(String, Throwable)</code> constructor
     * Assertion: constructs CRLException when <code>cause</code> is null
     * <code>msg</code> is not null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as the second parameter.",
        method = "CRLException",
        args = {java.lang.String.class, java.lang.Throwable.class}
    )
    public void testCRLException07() {
        CRLException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new CRLException(msgs[i], null);
            assertEquals("getMessage() must return: ".concat(msgs[i]), tE
                    .getMessage(), msgs[i]);
            assertNull("getCause() must return null", tE.getCause());
        }
    }

    /**
     * Test for <code>CRLException(String, Throwable)</code> constructor
     * Assertion: constructs CRLException when <code>cause</code> is not null
     * <code>msg</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as the first parameter.",
        method = "CRLException",
        args = {java.lang.String.class, java.lang.Throwable.class}
    )
    public void testCRLException08() {
        CRLException tE = new CRLException(null, tCause);
        if (tE.getMessage() != null) {
            String toS = tCause.toString();
            String getM = tE.getMessage();
            assertTrue("getMessage() must should ".concat(toS), (getM
                    .indexOf(toS) != -1));
        }
        assertNotNull("getCause() must not return null", tE.getCause());
        assertEquals("getCause() must return ".concat(tCause.toString()), tE
                .getCause(), tCause);
    }

    /**
     * Test for <code>CRLException(String, Throwable)</code> constructor
     * Assertion: constructs CRLException when <code>cause</code> is not null
     * <code>msg</code> is not null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CRLException",
        args = {java.lang.String.class, java.lang.Throwable.class}
    )
    public void testCRLException09() {
        CRLException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new CRLException(msgs[i], tCause);
            String getM = tE.getMessage();
            String toS = tCause.toString();
            if (msgs[i].length() > 0) {
                assertTrue("getMessage() must contain ".concat(msgs[i]), getM
                        .indexOf(msgs[i]) != -1);
                if (!getM.equals(msgs[i])) {
                    assertTrue("getMessage() should contain ".concat(toS), getM
                            .indexOf(toS) != -1);
                }
            }
            assertNotNull("getCause() must not return null", tE.getCause());
            assertEquals("getCause() must return ".concat(tCause.toString()),
                    tE.getCause(), tCause);
        }
    }
}
