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

import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.util.Collection;
import java.util.Vector;

import org.apache.harmony.security.tests.support.cert.MyCertificate;
/**
 * Tests for <code>CollectionCertStoreParameters</code>
 * 
 */
@TestTargetClass(CollectionCertStoreParameters.class)
public class CollectionCertStoreParametersTest extends TestCase {

    //
    // Tests
    //

    /**
     * Test #1 for <code>CollectionCertStoreParameters()</code> constructor<br>
     * Assertion: Creates an instance of CollectionCertStoreParameters
     * with the default parameter values (an empty and immutable Collection) 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CollectionCertStoreParameters",
        args = {}
    )
    public final void testCollectionCertStoreParameters01() {
        CertStoreParameters cp = new CollectionCertStoreParameters();
        assertTrue("isCollectionCertStoreParameters",
                cp instanceof CollectionCertStoreParameters);
    }

    /**
     * Test #2 for <code>CollectionCertStoreParameters</code> constructor<br>
     * Assertion: Creates an instance of CollectionCertStoreParameters
     * with the default parameter values (an empty and immutable Collection) 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CollectionCertStoreParameters",
        args = {}
    )
    @SuppressWarnings("unchecked")
    public final void testCollectionCertStoreParameters02() {
        CollectionCertStoreParameters cp = new CollectionCertStoreParameters();
        Collection c = cp.getCollection();
        assertTrue("isEmpty", c.isEmpty());

        // check that empty collection is immutable
        try {
            // try to modify it
            c.add(new Object());
            fail("empty collection must be immutable");
        } catch (Exception e) {
        }
    }

    /**
     * Test #1 for <code>CollectionCertStoreParameters(Collection)</code>
     * constructor<br>
     * Assertion: Creates an instance of CollectionCertStoreParameters 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CollectionCertStoreParameters",
        args = {java.util.Collection.class}
    )
    public final void testCollectionCertStoreParametersCollection01() {
        Vector<Certificate> certificates = new Vector<Certificate>();
        certificates.add(new MyCertificate("TEST", new byte[] {}));
        new CollectionCertStoreParameters(certificates);
    }

    /**
     * Test #2 for <code>CollectionCertStoreParameters(Collection)</code>
     * constructor<br>
     * Assertion: If the specified <code>Collection</code> contains an object
     * that is not a <code>Certificate</code> or <code>CRL</code>, that object
     * will be ignored by the Collection <code>CertStore</code>. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CollectionCertStoreParameters",
        args = {java.util.Collection.class}
    )
    public final void testCollectionCertStoreParametersCollection02() {
        // just check that we able to create CollectionCertStoreParameters
        // object passing Collection containing Object which is not
        // a Certificate or CRL
        Vector<String> certificates = new Vector<String>();
        certificates.add(new String("Not a Certificate"));
        new CollectionCertStoreParameters(certificates);
    }

    /**
     * Test #3 for <code>CollectionCertStoreParameters(Collection)</code>
     * constructor<br>
     * Assertion: The Collection is not copied. Instead, a reference is used.
     * This allows the caller to subsequently add or remove Certificates or
     * CRLs from the Collection, thus changing the set of Certificates or CRLs
     * available to the Collection CertStore. The Collection CertStore will
     * not modify the contents of the Collection 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "CollectionCertStoreParameters",
        args = {java.util.Collection.class}
    )
    public final void testCollectionCertStoreParametersCollection03() {
        Vector<Certificate> certificates = new Vector<Certificate>();
        // create using empty collection
        CollectionCertStoreParameters cp =
            new CollectionCertStoreParameters(certificates);
        // check that the reference is used 
        assertTrue("isRefUsed_1", certificates == cp.getCollection());
        // check that collection still empty
        assertTrue("isEmpty", cp.getCollection().isEmpty());
        // modify our collection
        certificates.add(new MyCertificate("TEST", new byte[] {(byte)1}));
        certificates.add(new MyCertificate("TEST", new byte[] {(byte)2}));
        // check that internal state has been changed accordingly
        assertTrue("isRefUsed_2", certificates.equals(cp.getCollection()));
    }

    /**
     * Test #4 for <code>CollectionCertStoreParameters(Collection)</code>
     * constructor<br>
     * Assertion: <code>NullPointerException</code> - if
     * <code>collection</code> is <code>null</code> 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "CollectionCertStoreParameters",
        args = {java.util.Collection.class}
    )
    public final void testCollectionCertStoreParametersCollection04() {
        try {
            new CollectionCertStoreParameters(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test #1 for <code>clone()</code> method<br>
     * Assertion: Returns a copy of this object
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public final void testClone01() {
        Vector<Certificate> certificates = new Vector<Certificate>();
        certificates.add(new MyCertificate("TEST", new byte[] {(byte)4}));
        CollectionCertStoreParameters cp1 =
            new CollectionCertStoreParameters(certificates);
        CollectionCertStoreParameters cp2 =
            (CollectionCertStoreParameters)cp1.clone();
        // check that that we have new object
        assertTrue(cp1 != cp2);
    }

    /**
     * Test #2 for <code>clone()</code> method<br>
     * Assertion: ...only a reference to the <code>Collection</code>
     * is copied, and not the contents
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public final void testClone02() {
        Vector<Certificate> certificates = new Vector<Certificate>();
        certificates.add(new MyCertificate("TEST", new byte[] {(byte)4}));
        CollectionCertStoreParameters cp1 =
            new CollectionCertStoreParameters(certificates);
        CollectionCertStoreParameters cp2 =
            (CollectionCertStoreParameters)cp1.clone();
        // check that both objects hold the same reference
        assertTrue(cp1.getCollection() == cp2.getCollection());
    }

    /**
     * Test #3 for <code>clone()</code> method<br>
     * Assertion: ...only a reference to the <code>Collection</code>
     * is copied, and not the contents
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public final void testClone03() {
        CollectionCertStoreParameters cp1 =
            new CollectionCertStoreParameters();
        CollectionCertStoreParameters cp2 =
            (CollectionCertStoreParameters)cp1.clone();
        CollectionCertStoreParameters cp3 =
            (CollectionCertStoreParameters)cp2.clone();
        // check that all objects hold the same reference
        assertTrue(cp1.getCollection() == cp2.getCollection() &&
                   cp3.getCollection() == cp2.getCollection());
    }

    /**
     * Test #1 for <code>toString()</code> method<br>
     * Assertion: returns the formatted string describing parameters
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public final void testToString01() {
        CollectionCertStoreParameters cp =
            new CollectionCertStoreParameters();
        String s = cp.toString();
        assertNotNull(s);
    }

    /**
     * Test #2 for <code>toString()</code> method<br>
     * Assertion: returns the formatted string describing parameters
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public final void testToString02() {
        Vector<Certificate> certificates = new Vector<Certificate>();
        certificates.add(new MyCertificate("TEST", new byte[] {(byte)4}));
        CollectionCertStoreParameters cp =
            new CollectionCertStoreParameters(certificates);

        assertNotNull(cp.toString());
    }

    /**
     * Test #1 for <code>getCollection()</code> method<br>
     * Assertion: returns the Collection (never null)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCollection",
        args = {}
    )
    public final void testGetCollection01() {
        CollectionCertStoreParameters cp = new CollectionCertStoreParameters();
        assertNotNull(cp.getCollection());
    }

    /**
     * Test #2 for <code>getCollection()</code> method<br>
     * Assertion: returns the Collection (never null)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCollection",
        args = {}
    )
    public final void testGetCollection02() {
        Vector certificates = new Vector();
        CollectionCertStoreParameters cp =
            new CollectionCertStoreParameters(certificates);
        assertNotNull(cp.getCollection());
    }

}
