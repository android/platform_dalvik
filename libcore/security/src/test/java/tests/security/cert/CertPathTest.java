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

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.cert.MyCertPath;
import org.apache.harmony.security.tests.support.cert.MyFailingCertPath;
import org.apache.harmony.security.tests.support.cert.TestUtils;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;

/**
 * Tests for <code>CertPath</code> fields and methods
 * 
 */
@TestTargetClass(CertPath.class)
public class CertPathTest extends TestCase {
    /**
     * Meaningless cert path encoding just for testing purposes
     */
    private static final byte[] testEncoding = new byte[] {
            (byte)1, (byte)2, (byte)3, (byte)4, (byte)5
    };
    
    private static final byte[] testEncoding1 = new byte[] {
        (byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6
    };

    //
    // Tests
    //

    /**
     * Test for <code>CertPath(String type)</code> method<br>
     * Assertion: returns hash of the <code>Certificate</code> instance
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "CertPath",
        args = {java.lang.String.class}
    )
    public final void testCertPath() {
        try {
            CertPath cp1 = new MyCertPath(testEncoding);
            assertEquals("MyEncoding", cp1.getType());
            assertTrue(Arrays.equals(testEncoding, cp1.getEncoded()));
        } catch (CertificateEncodingException e) {
            fail("Unexpected CertificateEncodingException " + e.getMessage());
        }
        
        try {
            CertPath cp1 = new MyCertPath(null);
        } catch (Exception e) {
            fail("Unexpected exception " + e.getMessage());
        }
    }

    /**
     * Test for <code>hashCode()</code> method<br>
     * Assertion: returns hash of the <code>Certificate</code> instance
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public final void testHashCode() {
        CertPath cp1 = new MyCertPath(testEncoding);
        CertPath cp2 = new MyCertPath(testEncoding);
        CertPath cp3 = new MyCertPath(testEncoding1);

        assertTrue(cp1.hashCode() == cp2.hashCode());
        assertTrue(cp1.hashCode() != cp3.hashCode());
    }

    /**
     * Test for <code>hashCode()</code> method<br>
     * Assertion: hash code of equal objects should be the same
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public final void testHashCodeEqualsObject() {
        CertPath cp1 = new MyCertPath(testEncoding);
        CertPath cp2 = new MyCertPath(testEncoding);
        assertTrue((cp1.hashCode() == cp2.hashCode()) && cp1.equals(cp2));
    }

    /**
     * Test for <code>getType()</code> method<br>
     * Assertion: returns cert path type
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getType",
        args = {}
    )
    public final void testGetType() {
        assertEquals("MyEncoding", new MyCertPath(testEncoding).getType());
    }

    /**
     * Test #1 for <code>equals(Object)</code> method<br>
     * Assertion: object equals to itself 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that object equals to itself.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject01() {
        CertPath cp1 = new MyCertPath(testEncoding);
        assertTrue(cp1.equals(cp1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object equals to other <code>CertPath</code>
     * instance with the same state
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that CertPath object equals to other CertPath with the same state.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject02() {
        CertPath cp1 = new MyCertPath(testEncoding);
        CertPath cp2 = new MyCertPath(testEncoding);
        assertTrue(cp1.equals(cp2) && cp2.equals(cp1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to <code>null</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject03() {
        CertPath cp1 = new MyCertPath(testEncoding);
        assertFalse(cp1.equals(null));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to other which is not
     * instance of <code>CertPath</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies non equal objects.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public final void testEqualsObject04() {
        CertPath cp1 = new MyCertPath(testEncoding);
        assertFalse(cp1.equals("MyEncoding"));
    }

    /**
     * Test for <code>toString()</code> method<br>
     * Assertion: returns string representation of
     * <code>CertPath</code> object
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public final void testToString() {
        CertPath cp1 = new MyCertPath(testEncoding);
        assertNotNull(cp1.toString());
    }

    //
    // the following tests just call methods
    // that are abstract in <code>CertPath</code>
    // (So they just like signature tests)
    //

    /**
     * This test just calls <code>getCertificates()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Abstract method.",
        method = "getCertificates",
        args = {}
    )
    public final void testGetCertificates() {
        CertPath cp1 = new MyCertPath(testEncoding);
        cp1.getCertificates();
    }

    /**
     * This test just calls <code>getEncoded()</code> method<br>
     *
     * @throws CertificateEncodingException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Abstract method.",
        method = "getEncoded",
        args = {}
    )
    public final void testGetEncoded() throws CertificateEncodingException {
        CertPath cp1 = new MyCertPath(testEncoding);
        cp1.getEncoded();
    }

    /**
     * This test just calls <code>getEncoded(String)</code> method<br>
     *
     * @throws CertificateEncodingException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Abstract method.",
        method = "getEncoded",
        args = {java.lang.String.class}
    )
    public final void testGetEncodedString() throws CertificateEncodingException {
        CertPath cp1 = new MyCertPath(testEncoding);
        cp1.getEncoded("MyEncoding");
    }

    /**
     * This test just calls <code>getEncodings()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Abstract method.",
        method = "getEncodings",
        args = {}
    )
    public final void testGetEncodings() {
        CertPath cp1 = new MyCertPath(testEncoding);
        cp1.getEncodings();
    }

    /**
     * This test just calls <code>writeReplace()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ObjectStreamException.",
        method = "writeReplace",
        args = {}
    )
    public final void testWriteReplace() {
        try {
            MyCertPath cp1 = new MyCertPath(testEncoding);
            Object obj = cp1.writeReplace();
            assertTrue(obj.toString().contains(
                    "java.security.cert.CertPath$CertPathRep"));
        } catch (ObjectStreamException e) {
            fail("Unexpected ObjectStreamException " + e.getMessage());
        }
    }
    
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "verifies ObjectStreamException.",
            method = "writeReplace",
            args = {}
        )
    public final void testWriteReplace_ObjectStreamException() {
        try {
            MyFailingCertPath cp = new MyFailingCertPath(testEncoding);
            Object obj = cp.writeReplace();
            fail("expected ObjectStreamException");
        } catch (ObjectStreamException e) {
            // ok
        }
    }
    
    /**
     * @tests serialization/deserialization compatibility.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Verifies serialization/deserialization compatibility. And tests default constructor",
            method = "!SerializationSelf",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "writeReplace",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "CertPath.CertPathRep.readResolve",
            args = {}
        )
    })
    // Test passed on RI
    public void testSerializationSelf() throws Exception {
        TestUtils.initCertPathSSCertChain();
        CertPath certPath = TestUtils.buildCertPathSSCertChain();

        SerializationTest.verifySelf(certPath);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Verifies serialization/deserialization compatibility.",
            method = "!SerializationGolden",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "writeReplace",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "CertPath.CertPathRep.readResolve",
            args = {}
        )
    })
    // Test passed on RI
    public void testSerializationCompatibility() throws Exception {
        TestUtils.initCertPathSSCertChain();
        CertPath certPath = TestUtils.buildCertPathSSCertChain();

        SerializationTest.verifyGolden(this, certPath);
    }
}
