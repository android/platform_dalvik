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

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.harmony.security.tests.support.cert.TestUtils;

import tests.targets.security.KeyStoreTestPKCS12;

/**
 * Tests for <code>PKIXParameters</code> fields and methods
 */
@TestTargetClass(PKIXParameters.class)
public class PKIXParametersTest extends TestCase {
    /**
     * Some valid issuer name
     */
    private final static String testIssuer =
        "CN=VM,OU=DRL Security,O=Intel,L=Novosibirsk,ST=NSO,C=RU";

    //
    // Tests
    //

    /**
     * Test #1 for <code>PKIXParameters(Set)</code> constructor<br>
     * Assertion: Creates an instance of <code>PKIXParameters</code> with the
     * specified <code>Set</code> of most-trusted CAs. Each element of the set
     * is a <code>TrustAnchor</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies positive case.",
        method = "PKIXParameters",
        args = {java.util.Set.class}
    )
    public final void testPKIXParametersSet01()
            throws InvalidAlgorithmParameterException {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }
        // use valid parameter
        CertPathParameters cpp = new PKIXParameters(taSet);
        assertTrue(cpp instanceof PKIXParameters);
    }

    /**
     * Test #2 for <code>PKIXParameters(Set)</code> constructor<br>
     * Assertion: ... the <code>Set</code> is copied to protect against
     * subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "PKIXParameters",
        args = {java.util.Set.class}
    )
    @SuppressWarnings("unchecked")
    public final void testPKIXParametersSet02()
            throws InvalidAlgorithmParameterException {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }
        HashSet<TrustAnchor> originalSet = (HashSet<TrustAnchor>) taSet;
        HashSet<TrustAnchor> originalSetCopy = (HashSet<TrustAnchor>) originalSet
                .clone();
        // create test object using originalSet
        PKIXParameters pp = new PKIXParameters(originalSetCopy);
        // modify originalSet
        originalSetCopy.clear();
        // check that test object's internal state
        // has not been affected by the above modification
        Set returnedSet = pp.getTrustAnchors();
        assertEquals(originalSet, returnedSet);
    }

    /**
     * Test #3 for <code>PKIXParameters(Set)</code> constructor<br>
     * Assertion: <code>NullPointerException</code> - if the specified
     * <code>Set</code> is null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "PKIXParameters",
        args = {java.util.Set.class}
    )
    public final void testPKIXParametersSet03() throws Exception {
        try {
            // pass null
            new PKIXParameters((Set<TrustAnchor>) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test #4 for <code>PKIXParameters(Set)</code> constructor<br>
     * Assertion: <code>InvalidAlgorithmParameterException</code> - if the
     * specified <code>Set</code> is empty (
     * <code>trustAnchors.isEmpty() == true</code>)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies InvalidAlgorithmParameterException.",
        method = "PKIXParameters",
        args = {java.util.Set.class}
    )
    public final void testPKIXParametersSet04() {
        try {
            // use empty set
            new PKIXParameters(new HashSet<TrustAnchor>());
            fail("InvalidAlgorithmParameterException expected");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    /**
     * Test #5 for <code>PKIXParameters(Set)</code> constructor<br>
     * Assertion: <code>ClassCastException</code> - if any of the elements in
     * the <code>Set</code> are not of type
     * <code>java.security.cert.TrustAnchor</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ClassCastException.",
        method = "PKIXParameters",
        args = {java.util.Set.class}
    )
    @SuppressWarnings("unchecked")
    public final void testPKIXParametersSet05() throws Exception {
        Set taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        // add wrong object to valid set
        assertTrue(taSet.add(new Object()));
        try {
            new PKIXParameters(taSet);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test #3 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * Assertion: <code>NullPointerException</code> - if the
     * <code>keystore</code> is <code>null</code>
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "PKIXParameters",
        args = {java.security.KeyStore.class}
    )
    public final void testPKIXParametersKeyStore03() throws Exception {
        try {
            // pass null
            new PKIXParameters((KeyStore) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test for <code>clone()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public final void testClone() throws InvalidAlgorithmParameterException {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters cpp = new PKIXParameters(taSet);
        PKIXParameters cppc = (PKIXParameters) cpp.clone();

        assertEquals(cpp.getPolicyQualifiersRejected(), cppc
                .getPolicyQualifiersRejected());
        assertEquals(cpp.getCertPathCheckers(), cppc.getCertPathCheckers());
        assertEquals(cpp.getCertStores(), cppc.getCertStores());
        assertEquals(cpp.getDate(), cppc.getDate());
        assertEquals(cpp.getInitialPolicies(), cppc.getInitialPolicies());
        assertEquals(cpp.getSigProvider(), cppc.getSigProvider());
        assertEquals(cpp.getTargetCertConstraints(), cppc
                .getTargetCertConstraints());
        assertEquals(cpp.getTrustAnchors(), cppc.getTrustAnchors());

        assertEquals(cpp.isAnyPolicyInhibited(), cppc.isAnyPolicyInhibited());
        assertEquals(cpp.isExplicitPolicyRequired(), cppc
                .isExplicitPolicyRequired());
        assertEquals(cpp.isPolicyMappingInhibited(), cppc
                .isPolicyMappingInhibited());
        assertEquals(cpp.isRevocationEnabled(), cppc.isRevocationEnabled());

        cpp.setDate(Calendar.getInstance().getTime());
        cpp.setPolicyQualifiersRejected(!cppc.getPolicyQualifiersRejected());
        assertFalse(cpp.getDate().equals(cppc.getDate()));
        assertFalse(cpp.getPolicyQualifiersRejected() == cppc
                .getPolicyQualifiersRejected());

        cppc.setExplicitPolicyRequired(!cpp.isExplicitPolicyRequired());
        cppc.setRevocationEnabled(!cpp.isRevocationEnabled());

        assertFalse(cpp.isExplicitPolicyRequired() == cppc
                .isExplicitPolicyRequired());
        assertFalse(cpp.isRevocationEnabled() == cppc.isRevocationEnabled());

        PKIXParameters cpp1 = null;
        try {
            cpp1.clone();
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * Test #1 for <code>getPolicyQualifiersRejected()</code> method<br>
     * Assertion: When a <code>PKIXParameters</code> object is created, this
     * flag is set to <code>true</code><br>
     * Assertion: returns the current value of the PolicyQualifiersRejected flag
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPolicyQualifiersRejected",
        args = {}
    )
    public final void testGetPolicyQualifiersRejected() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertTrue(p.getPolicyQualifiersRejected());
    }

    /**
     * Test for <code>setPolicyQualifiersRejected()</code> method<br>
     * Assertion: set the new value of the <code>PolicyQualifiersRejected</code>
     * flag
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setPolicyQualifiersRejected",
        args = {boolean.class}
    )
    public final void testSetPolicyQualifiersRejected() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setPolicyQualifiersRejected(false);
        assertFalse("setFalse", p.getPolicyQualifiersRejected());
        p.setPolicyQualifiersRejected(true);
        assertTrue("setTrue", p.getPolicyQualifiersRejected());
    }

    /**
     * Test for <code>isAnyPolicyInhibited()</code> method<br>
     * Assertion: returns <code>true</code> if the any policy OID is inhibited,
     * <code>false</code> otherwise<br>
     * Assertion: By default, the any policy OID is not inhibited (
     * <code>isAnyPolicyInhibited()</code> returns false).
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAnyPolicyInhibited",
        args = {}
    )
    public final void testIsAnyPolicyInhibited() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertFalse(p.isAnyPolicyInhibited());
    }

    /**
     * Test for <code>setAnyPolicyInhibited()</code> method<br>
     * Assertion: sets state to determine if the any policy OID should be
     * processed if it is included in a certificate
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setAnyPolicyInhibited",
        args = {boolean.class}
    )
    public final void testSetAnyPolicyInhibited() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setAnyPolicyInhibited(true);
        assertTrue("setTrue", p.isAnyPolicyInhibited());
        p.setAnyPolicyInhibited(false);
        assertFalse("setFalse", p.isAnyPolicyInhibited());
    }

    /**
     * Test for <code>isExplicitPolicyRequired()</code> method<br>
     * Assertion: returns <code>true</code> if explicit policy is required,
     * <code>false</code> otherwise<br>
     * Assertion: by default, the ExplicitPolicyRequired flag is false
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isExplicitPolicyRequired",
        args = {}
    )
    public final void testIsExplicitPolicyRequired() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertFalse(p.isExplicitPolicyRequired());
    }

    /**
     * Test for <code>setExplicitPolicyRequired()</code> method<br>
     * Assertion: sets the ExplicitPolicyRequired flag
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setExplicitPolicyRequired",
        args = {boolean.class}
    )
    public final void testSetExplicitPolicyRequired() throws Exception { 
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setExplicitPolicyRequired(true);
        assertTrue("setTrue", p.isExplicitPolicyRequired());
        p.setExplicitPolicyRequired(false);
        assertFalse("setFalse", p.isExplicitPolicyRequired());
    }

    /**
     * Test for <code>isPolicyMappingInhibited()</code> method<br>
     * Assertion: returns true if policy mapping is inhibited, false otherwise
     * Assertion: by default, policy mapping is not inhibited (the flag is
     * false)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isPolicyMappingInhibited",
        args = {}
    )
    public final void testIsPolicyMappingInhibited() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName() + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertFalse(p.isPolicyMappingInhibited());
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        TestUtils.initCertPathSSCertChain();
        Set<TrustAnchor> taSet2 = Collections.singleton(new TrustAnchor(
               TestUtils.rootCertificateSS, null));
        p = new PKIXParameters(taSet2);
        
        assertFalse(p.isPolicyMappingInhibited());
        p.setPolicyMappingInhibited(true);
        assertTrue(p.isRevocationEnabled());
    }

    /**
     * Test for <code>setPolicyMappingInhibited()</code> method<br>
     * Assertion: sets the PolicyMappingInhibited flag
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setPolicyMappingInhibited",
        args = {boolean.class}
    )
    public final void testSetPolicyMappingInhibited() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setPolicyMappingInhibited(true);
        assertTrue("setTrue", p.isPolicyMappingInhibited());
        p.setPolicyMappingInhibited(false);
        assertFalse("setFalse", p.isPolicyMappingInhibited());
    }

    /**
     * Test for <code>isPolicyMappingInhibited()</code> method<br>
     * Assertion: returns the current value of the RevocationEnabled flag
     * Assertion: when a <code>PKIXParameters</code> object is created, this
     * flag is set to true
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isRevocationEnabled",
        args = {}
    )
    public final void testIsRevocationEnabled() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertTrue(p.isRevocationEnabled());
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
       TestUtils.initCertPathSSCertChain();
       Set<TrustAnchor> taSet2 = Collections.singleton(new TrustAnchor(
              TestUtils.rootCertificateSS, null));
       p = new PKIXParameters(taSet2);
       
       assertTrue(p.isRevocationEnabled());
       p.setRevocationEnabled(false);
       assertFalse(p.isRevocationEnabled());
    }

    /**
     * Test for <code>isPolicyMappingInhibited()</code> method<br>
     * Assertion: sets the RevocationEnabled flag
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setRevocationEnabled",
        args = {boolean.class}
    )
    public final void testSetRevocationEnabled() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setRevocationEnabled(false);
        assertFalse("setFalse", p.isRevocationEnabled());
        p.setRevocationEnabled(true);
        assertTrue("setTrue", p.isRevocationEnabled());
    }

    /**
     * Test for <code>getSigProvider()</code> method<br>
     * Assertion: returns the signature provider's name, or null if not set
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSigProvider",
        args = {}
    )
    public final void testGetSigProvider() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNull("not set", p.getSigProvider());
        p.setSigProvider("Some Provider");
        assertNotNull("set", p.getSigProvider());
    }

    /**
     * Test for <code>setSigProvider(String)</code> method<br>
     * Assertion: sets the signature provider's name
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setSigProvider",
        args = {java.lang.String.class}
    )
    public final void testSetSigProvider() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        String sigProviderName = "Some Provider";
        p.setSigProvider(sigProviderName);
        assertTrue("set", sigProviderName.equals(p.getSigProvider()));
        p.setSigProvider(null);
        assertNull("unset", p.getSigProvider());
    }

    /**
     * Test #1 for <code>getTargetCertConstraints()</code> method<br>
     * Assertion: returns a <code>CertSelector</code> specifying the constraints
     * on the target certificate (or <code>null</code>)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getTargetCertConstraints method returns null, if no constraints are defined.",
        method = "getTargetCertConstraints",
        args = {}
    )
    public final void testGetTargetCertConstraints01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNull(p.getTargetCertConstraints());
    }

    /**
     * Test #2 for <code>getTargetCertConstraints()</code> method<br>
     * Assertion: note that the <code>CertSelector</code> returned is cloned to
     * protect against subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    @TestTargets({
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies that returned CertSelector is cloned to protect against subsequent modifications.",
        method = "setTargetCertConstraints",
        args = {java.security.cert.CertSelector.class}
    ),
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getTargetCertConstraints",
            args = {}
        )
    })
    public final void testGetTargetCertConstraints02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        X509CertSelector x509cs = new X509CertSelector();
        PKIXParameters p = new PKIXParameters(taSet);
        p.setTargetCertConstraints(x509cs);
        // get cert selector
        X509CertSelector cs1 = (X509CertSelector)p.getTargetCertConstraints();
        X509CertSelector cs3 = (X509CertSelector)p.getTargetCertConstraints();
        assertNotNull(cs1);
        // modify returned selector
        cs1.setIssuer(testIssuer);
        // get cert selector again
        X509CertSelector cs2 = (X509CertSelector)p.getTargetCertConstraints();
        // check that selector is not the same
        assertNotSame("notTheSame", cs1, cs2);
        // check that selector's internal state has
        // not been changed by above modification
        assertFalse("internal stateNotChanged", testIssuer.equals(cs2.getIssuerAsString()));
    }

    /**
     * Test for <code>setTargetCertConstraints(CertSelector)</code> method<br>
     * Assertion: sets the required constraints on the target certificate. The
     * constraints are specified as an instance of CertSelector<br>
     * Assertion: ... If <code>null</code>, no constraints are defined
     * 
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "setTargetCertConstraints",
        args = {java.security.cert.CertSelector.class}
    )
    public final void testSetTargetCertConstraints01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        X509CertSelector x509cs = new X509CertSelector();
        x509cs.setIssuer(testIssuer);
        PKIXParameters p = new PKIXParameters(taSet);
        p.setTargetCertConstraints(x509cs);
        assertEquals("set", testIssuer, ((X509CertSelector) p
                .getTargetCertConstraints()).getIssuerAsString());
        p.setTargetCertConstraints(null);
        assertNull("unset", p.getTargetCertConstraints());
    }

    /**
     * Test #2 for <code>setTargetCertConstraints(CertSelector)</code> method<br>
     * Assertion: ... the CertSelector specified is cloned to protect against
     * subsequent modifications
     * 
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Additional test.",
        method = "setTargetCertConstraints",
        args = {java.security.cert.CertSelector.class}
    )
    public final void testSetTargetCertConstraints02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        X509CertSelector x509cs = new X509CertSelector();
        PKIXParameters p = new PKIXParameters(taSet);
        p.setTargetCertConstraints(x509cs);
        // modify selector
        x509cs.setIssuer(testIssuer);
        // get selector
        X509CertSelector x509cs1 = (X509CertSelector) p
                .getTargetCertConstraints();
        // check that selector's internal state has
        // not been changed by above modification
        assertFalse(testIssuer.equals(x509cs1.getIssuerAsString()));
    }

    /**
     * Test #1 for <code>getCertStores()</code> method<br>
     * Assertion: list ... (may be empty, but never <code>null</code>)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getCertStores method returns empty list, but not null.",
        method = "getCertStores",
        args = {}
    )
    public final void testGetCertStores01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNotNull("notNull", p.getCertStores());
        assertTrue("isEmpty", p.getCertStores().isEmpty());
    }

    /**
     * Test #2 for <code>getCertStores()</code> method<br>
     * Assertion: returns an immutable <code>List</code> of
     * <code>CertStores</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getCertStores method returns an immutable List of CertStores.",
        method = "getCertStores",
        args = {}
    )
    public final void testGetCertStores02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        List<CertStore> cs = p.getCertStores();

        try {
            // try to modify returned list
            cs.add((CertStore) (new Object()));
            fail("must be immutable");
        } catch (Exception e) {
        }
    }

    /**
     * Test #1 for <code>setCertStores(List)</code> method<br>
     * Assertion: Sets the list of CertStores ...
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ClassCastException.",
        method = "setCertStores",
        args = {java.util.List.class}
    )
    public final void testSetCertStores01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setCertStores(TestUtils.getCollectionCertStoresList());
        // check that list has been set
        assertFalse(p.getCertStores().isEmpty());
    }

    /**
     * Test #2 for <code>setCertStores(List)</code> method<br>
     * Assertion: list ... may be <code>null</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ClassCastException.",
        method = "setCertStores",
        args = {java.util.List.class}
    )
    public final void testSetCertStores02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        // add null
        p.setCertStores(null);
        // check that we have non null empty list now
        assertNotNull("notNull1", p.getCertStores());
        assertTrue("isEmpty1", p.getCertStores().isEmpty());
        // add empty
        p.setCertStores(new ArrayList<CertStore>());
        assertNotNull("notNull2", p.getCertStores());
        assertTrue("isEmpty2", p.getCertStores().isEmpty());
    }

    /**
     * Test #3 for <code>setCertStores(List)</code> method<br>
     * Assertion: list is copied to protect against subsequent modifications
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that the list is copied to protect against subsequent modifications.",
        method = "setCertStores",
        args = {java.util.List.class}
    )
    public final void testSetCertStores03() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        List<CertStore> l = TestUtils.getCollectionCertStoresList();
        p.setCertStores(l);
        // modify list just set
        l.clear();
        // check that list maintained internally has
        // not been changed by the above modification
        assertFalse(p.getCertStores().isEmpty());
    }

    /**
     * Test #4 for <code>setCertStores(List)</code> method<br>
     * Assertion: <code>ClassCastException</code> - if any of the elements in
     * the list are not of type <code>java.security.cert.CertStore</code>
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ClassCastException.",
        method = "setCertStores",
        args = {java.util.List.class}
    )
    @SuppressWarnings("unchecked")
    public final void testSetCertStores04() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        List l = TestUtils.getCollectionCertStoresList();
        // add wrong object to valid set
        assertTrue(l.add(new Object()));

        try {
            p.setCertStores(l);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test #1 for <code>addCertStore(CertStore)</code> method<br>
     * Assertion: adds a <code>CertStore</code> to the end of the list of
     * <code>CertStores</code>
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ClassCastException.",
        method = "addCertStore",
        args = {java.security.cert.CertStore.class}
    )
    public final void testAddCertStore01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.addCertStore(CertStore.getInstance("Collection",
                new CollectionCertStoreParameters()));
        assertFalse(p.getCertStores().isEmpty());
    }

    /**
     * Test #2 for <code>addCertStore(CertStore)</code> method<br>
     * Assertion: if <code>null</code>, the store is ignored (not added to list)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "addCertStore",
        args = {java.security.cert.CertStore.class}
    )
    public final void testAddCertStore02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.addCertStore(null);
        assertTrue(p.getCertStores().isEmpty());
    }

    /**
     * Test #1 for <code>getCertPathCheckers()</code> method<br>
     * Assertion: list ... may be empty, but not <code>null</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getCertPathCheckers method returns not empty list.",
        method = "getCertPathCheckers",
        args = {}
    )
    public final void testGetCertPathCheckers01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        List l = p.getCertPathCheckers();
        assertNotNull("notNull", l);
        assertTrue("isEmpty", l.isEmpty());
    }

    /**
     * Test #2 for <code>getCertPathCheckers()</code> method<br>
     * Assertion: returns an immutable <code>List</code> of
     * <code>PKIXCertPathChecker</code>s
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getCertPathCheckers method returns an immutable List of PKIXCertPathChecker objects.",
        method = "getCertPathCheckers",
        args = {}
    )
    public final void testGetCertPathCheckers02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        List<PKIXCertPathChecker> l = p.getCertPathCheckers();

        try {
            // try to modify returned list
            l.add((PKIXCertPathChecker) new Object());
            fail("must be immutable");
        } catch (Exception e) {
        }
    }

    /**
     * Test #3 for <code>getCertPathCheckers()</code> method<br>
     * Assertion: The returned List is immutable, and each
     * <code>PKIXCertPathChecker</code> in the <code>List</code> is cloned to
     * protect against subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws CertPathValidatorException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that the returned List is immutable, and each PKIXCertPathChecker in the List is cloned to protect against subsequent modifications.",
        method = "getCertPathCheckers",
        args = {}
    )
    public final void testGetCertPathCheckers03() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        // retrieve checker and modify it
        PKIXCertPathChecker cpc1 = p.getCertPathCheckers().get(0);
        cpc1.init(true);
        assertTrue("modifiedOk", cpc1.isForwardCheckingSupported());
        // retrieve checker again and check
        // that its state has not been changed
        // by the above modification
        PKIXCertPathChecker cpc2 = p.getCertPathCheckers().get(0);
        assertFalse("isCloned", cpc2.isForwardCheckingSupported());
    }

    /**
     * Test #1 for <code>setCertPathCheckers(List)</code> method<br>
     * Assertion: sets a <code>List</code> of additional certification path
     * checkers
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "setCertPathCheckers",
        args = {java.util.List.class}
    )
    public final void testSetCertPathCheckers01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        List l1 = p.getCertPathCheckers();
        assertNotNull("notNull", l1);
        assertFalse("isNotEmpty", l1.isEmpty());
    }

    /**
     * Test #2 for <code>setCertPathCheckers(List)</code> method<br>
     * Assertion: <code>List</code> ... may be null
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "setCertPathCheckers",
        args = {java.util.List.class}
    )
    public final void testSetCertPathCheckers02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setCertPathCheckers(null);
        List<PKIXCertPathChecker> l1 = p.getCertPathCheckers();
        assertNotNull("notNull1", l1);
        assertTrue("isEmpty1", l1.isEmpty());
        p.setCertPathCheckers(new ArrayList<PKIXCertPathChecker>());
        List l2 = p.getCertPathCheckers();
        assertNotNull("notNull2", l2);
        assertTrue("isEmpty2", l2.isEmpty());
    }

    /**
     * Test #3 for <code>setCertPathCheckers(List)</code> method<br>
     * Assertion: <code>List</code> supplied here is copied and each
     * <code>PKIXCertPathChecker</code> in the list is cloned to protect against
     * subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "setCertPathCheckers",
        args = {java.util.List.class}
    )
    public final void testSetCertPathCheckers03() throws Exception {
        // checks that list copied
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        // modify list
        l.clear();
        // retrieve list and check
        // that its state has not been changed
        // by the above modification
        assertFalse("isCopied", p.getCertPathCheckers().isEmpty());
    }

    /**
     * Test #4 for <code>setCertPathCheckers(List)</code> method<br>
     * Assertion: <code>List</code> supplied here is copied and each
     * <code>PKIXCertPathChecker</code> in the list is cloned to protect against
     * subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidAlgorithmParameterException
     * @throws CertPathValidatorException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "setCertPathCheckers",
        args = {java.util.List.class}
    )
    public final void testSetCertPathCheckers04() throws Exception {
        // checks that checkers cloned
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        // modify checker
        cpc.init(true);
        // retrieve list and check that CertPathChecker's
        // state it contains has not been changed by the
        // above modification
        PKIXCertPathChecker cpc1 = p.getCertPathCheckers().get(0);
        assertFalse("isCopied", cpc1.isForwardCheckingSupported());
    }

    /**
     * Test #5 for <code>setCertPathCheckers(List)</code> method<br>
     * Assertion: <code>ClassCastException</code> - if any of the elements in
     * the list are not of type
     * <code>java.security.cert.PKIXCertPathChecker</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verify exception.",
        method = "setCertPathCheckers",
        args = {java.util.List.class}
    )
    @SuppressWarnings("unchecked")
    public final void testSetCertPathCheckers05() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        // add wrong object to the list
        assertTrue("addedOk", l.add(new Object()));

        try {
            p.setCertPathCheckers(l);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test #1 for <code>addCertPathChecker(PKIXCertPathChecker)</code> method<br>
     * Assertion: adds a <code>CertPathChecker</code> to the end of the list of
     * <code>CertPathChecker</code>s
     * 
     * @throws CertPathValidatorException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addCertPathChecker",
        args = {java.security.cert.PKIXCertPathChecker.class}
    )
    public final void testAddCertPathChecker01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        // create one more PKIXCertPathChecker
        PKIXCertPathChecker cpc1 = TestUtils.getTestCertPathChecker();
        cpc1.init(true);
        p.addCertPathChecker(cpc1);
        // check that we have two PKIXCertPathCheckers and
        // they are in right order
        List l1 = p.getCertPathCheckers();
        assertEquals("listSize", 2, l1.size());
        assertFalse("order1", ((PKIXCertPathChecker) l1.get(0))
                .isForwardCheckingSupported());
        assertTrue("order2", ((PKIXCertPathChecker) l1.get(1))
                .isForwardCheckingSupported());
    }

    /**
     * Test #2 for <code>addCertPathChecker(PKIXCertPathChecker)</code> method<br>
     * Assertion: if null, the checker is ignored (not added to list).
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that if PKIXCertPathChecker parameter is null, the checker is ignored (not added to list).",
        method = "addCertPathChecker",
        args = {java.security.cert.PKIXCertPathChecker.class}
    )
    public final void testAddCertPathChecker02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List<PKIXCertPathChecker> l = new ArrayList<PKIXCertPathChecker>();
        assertTrue("addedOk", l.add(cpc));
        p.setCertPathCheckers(l);
        // try to add null
        p.addCertPathChecker(null);
        // check that we have one PKIXCertPathChecker
        List l1 = p.getCertPathCheckers();
        assertEquals("listSize", 1, l1.size());
    }

    /**
     * Test #3 for <code>addCertPathChecker(PKIXCertPathChecker)</code> method<br>
     * Assertion: <code>PKIXCertPathChecker</code> is cloned to protect against
     * subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws CertPathValidatorException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that PKIXCertPathChecker is cloned to protect against subsequent modifications.",
        method = "addCertPathChecker",
        args = {java.security.cert.PKIXCertPathChecker.class}
    )
    public final void testAddCertPathChecker03() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        // checks that checkers cloned
        PKIXParameters p = new PKIXParameters(taSet);
        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();

        p.addCertPathChecker(cpc);
        // modify checker
        cpc.init(true);
        // retrieve list and check that CertPathChecker's
        // state it contains has not been changed by the
        // above modification
        List l = p.getCertPathCheckers();
        PKIXCertPathChecker cpc1 = (PKIXCertPathChecker) l.get(0);
        assertEquals("listSize", 1, l.size());
        assertFalse("isCopied", cpc1.isForwardCheckingSupported());
    }

    /**
     * Test #1 for <code>getDate()</code> method<br>
     * Assertion: the <code>Date</code>, or <code>null</code> if not set
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getDate",
        args = {}
    )
    public final void testGetDate01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        // the Date has not been set
        // the method must return null
        assertNull("null", p.getDate());
        Date currentDate = new Date();
        p.setDate(currentDate);
        // the Date returned must match
        assertEquals("notNull", currentDate, p.getDate());
    }

    /**
     * Test #2 for <code>getDate()</code> method<br>
     * Assertion: <code>Date</code> returned is copied to protect against
     * subsequent modifications
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that returned Date is copied to protect against subsequent modifications.",
        method = "getDate",
        args = {}
    )
    public final void testGetDate02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        Date currentDate = new Date();
        p.setDate((Date) currentDate.clone());
        Date ret1 = p.getDate();
        // modify Date returned
        ret1.setTime(0L);
        // check that internal Date has not been
        // changed by the above modification
        assertEquals(currentDate, p.getDate());
    }

    /**
     * @tests java.security.cert.PKIXParameters#setDate(Date)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDate",
        args = {java.util.Date.class}
    )
    @AndroidOnly("On the RI p.setDate(null) does not reset the date to null "
            + "as specified.")
    public final void test_setDateLjava_util_Date() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        assertNotNull("could not create test TrustAnchor set", taSet);

        // test: 'date' is unset and param is null
        PKIXParameters p = new PKIXParameters(taSet);
        p.setDate(null);
        assertNull(p.getDate());

        // test: 'date' is not null
        p = new PKIXParameters(taSet);
        Date toBeSet = new Date(555L);
        p.setDate(toBeSet);
        assertEquals(555L, p.getDate().getTime());
        // modify initial 'date' - it should be copied by constructor
        toBeSet.setTime(0L);
        // check that internal 'date' has not been
        // changed by the above modification
        assertEquals(555L, p.getDate().getTime());
        // set another 'date'
        p.setDate(new Date(333L));
        assertEquals(333L, p.getDate().getTime());

        // Regression for HARMONY-2882 (non-bug difference from RI)
        p = new PKIXParameters(taSet);
        p.setDate(new Date(555L));
        p.setDate(null); // reset 'date' back to current time
        assertNull(p.getDate());
    }

    /**
     * Test #1 for <code>getInitialPolicies()</code> method<br>
     * Assertion: The default return value is an empty <code>Set</code>
     * Assertion: Never returns <code>null</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getInitialPolicies method returns an empty Set and never null.",
        method = "getInitialPolicies",
        args = {}
    )
    public final void testGetInitialPolicies01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNotNull("notNull", p.getInitialPolicies());
        assertTrue("isEmpty", p.getInitialPolicies().isEmpty());
    }

    /**
     * Test #2 for <code>getInitialPolicies()</code> method<br>
     * Assertion: returns an immutable <code>Set</code> of initial policy OIDs
     * in <code>String</code> format<br>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getInitialPolicies method returns an immutable Set of initial policy in String format.",
        method = "getInitialPolicies",
        args = {}
    )
    public final void testGetInitialPolicies02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        Set<String> s = p.getInitialPolicies();
        try {
            // try to modify returned set
            s.add((String) new Object());
            fail("must be immutable");
        } catch (Exception e) {
        }
    }

    /**
     * Test #1 for <code>setInitialPolicies(Set)</code> method<br>
     * Assertion: sets the <code>Set</code> of initial policy identifiers (OID
     * strings)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ClassCastException.",
        method = "setInitialPolicies",
        args = {java.util.Set.class}
    )
    public final void testSetInitialPolicies01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        Set<String> s = new HashSet<String>();
        s.add("1.2.3.4.5.6.7");
        PKIXParameters p = new PKIXParameters(taSet);
        p.setInitialPolicies(s);
        assertEquals(1, p.getInitialPolicies().size());
    }

    /**
     * Test #2 for <code>setInitialPolicies(Set)</code> method<br>
     * Assertion: <code>Set</code> may be <code>null</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "setInitialPolicies",
        args = {java.util.Set.class}
    )
    public final void testSetInitialPolicies02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setInitialPolicies(null);
        assertTrue(p.getInitialPolicies().isEmpty());
    }

    /**
     * Test #3 for <code>setInitialPolicies(Set)</code> method<br>
     * Assertion: <code>Set</code> may be empty
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ClassCastException.",
        method = "setInitialPolicies",
        args = {java.util.Set.class}
    )
    public final void testSetInitialPolicies03() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        p.setInitialPolicies(new HashSet<String>());
        assertTrue(p.getInitialPolicies().isEmpty());
    }

    /**
     * Test #4 for <code>setInitialPolicies(Set)</code> method<br>
     * Assertion: <code>Set</code> is copied to protect against subsequent
     * modifications
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that Set is copied to protect against subsequent modifications.",
        method = "setInitialPolicies",
        args = {java.util.Set.class}
    )
    public final void testSetInitialPolicies04() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        Set<String> s = new HashSet<String>();
        s.add("1.2.3.4.5.6.7");
        s.add("1.2.3.4.5.6.8");
        PKIXParameters p = new PKIXParameters(taSet);
        p.setInitialPolicies(s);
        // modify original set
        s.clear();
        // check that set maintained internally has
        // not been changed by the above modification
        assertEquals(2, p.getInitialPolicies().size());
    }

    /**
     * Test #5 for <code>setInitialPolicies(Set)</code> method<br>
     * Assertion: <code>ClassCastException</code> - if any of the elements in
     * the set are not of type <code>String</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ClassCastException.",
        method = "setInitialPolicies",
        args = {java.util.Set.class}
    )
    @SuppressWarnings("unchecked")
    public final void testSetInitialPolicies05() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        Set s = new HashSet();
        s.add("1.2.3.4.5.6.7");
        s.add(new Object());
        PKIXParameters p = new PKIXParameters(taSet);
        try {
            p.setInitialPolicies(s);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test #1 for <code>getTrustAnchors()</code> method<br>
     * Assertion: an immutable <code>Set</code> of <code>TrustAnchors</code>
     * (never <code>null</code>)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getTrustAnchors returns an immutable Set of TrustAnchors, and never null.",
        method = "getTrustAnchors",
        args = {}
    )
    public final void testGetTrustAnchors01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNotNull("notNull", p.getTrustAnchors());
    }

    /**
     * Test #2 for <code>getTrustAnchors()</code> method<br>
     * Assertion: an immutable <code>Set</code> of <code>TrustAnchors</code>
     * (never <code>null</code>)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that getTrustAnchors returns an immutable set of TrustAnchors, and never null.",
        method = "getTrustAnchors",
        args = {}
    )
    public final void testGetTrustAnchors02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        Set<TrustAnchor> s = p.getTrustAnchors();
        try {
            // try to modify returned set
            s.add((TrustAnchor) new Object());
            fail("must be immutable");
        } catch (Exception e) {
        }
    }

    /**
     * Test #1 for <code>setTrustAnchors(Set)</code> method<br>
     * Assertion: Sets the <code>Set</code> of most-trusted CAs
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "setTrustAnchors",
        args = {java.util.Set.class}
    )
    public final void testSetTrustAnchors01() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        Set<TrustAnchor> taSet1 = TestUtils.getTrustAnchorSet();
        PKIXParameters p = new PKIXParameters(taSet);
        p.setTrustAnchors(taSet1);
        assertFalse(p.getTrustAnchors().isEmpty());
    }

    /**
     * Test #2 for <code>setTrustAnchors(Set)</code> method<br>
     * Assertion: <code>InvalidAlgorithmParameterException</code> - if the
     * specified <code>Set</code> is empty (
     * <code>trustAnchors.isEmpty() == true</code>)
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies InvalidAlgorithmParameterException.",
        method = "setTrustAnchors",
        args = {java.util.Set.class}
    )
    public final void testSetTrustAnchors02() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        try {
            // use empty set
            p.setTrustAnchors(new HashSet<TrustAnchor>());
            fail("InvalidAlgorithmParameterException expected");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    /**
     * Test #3 for <code>setTrustAnchors(Set)</code> method<br>
     * Assertion: <code>NullPointerException</code> - if the specified
     * <code>Set</code> is <code>null</code>)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "setTrustAnchors",
        args = {java.util.Set.class}
    )
    public final void testSetTrustAnchors03() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        try {
            // use null
            p.setTrustAnchors(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test #4 for <code>setTrustAnchors(Set)</code> method<br>
     * Assertion: <code>ClassCastException</code> - if any of the elements in
     * the set are not of type <code>java.security.cert.TrustAnchor</code>
     * 
     * @throws InvalidAlgorithmParameterException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ClassCastException.",
        method = "setTrustAnchors",
        args = {java.util.Set.class}
    )
    @SuppressWarnings("unchecked")
    public final void testSetTrustAnchors04() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        Set s = new HashSet(p.getTrustAnchors());
        s.add(new Object());
        try {
            p.setTrustAnchors(s);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test for <code>toString</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public final void testToString() throws Exception {
        Set<TrustAnchor> taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName()
                    + ": not performed (could not create test TrustAnchor set)");
        }

        PKIXParameters p = new PKIXParameters(taSet);
        assertNotNull(p.toString());

        PKIXParameters p1 = null;
        try {
            p1.toString();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * Test #4 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies everything except null argument",
        method = "PKIXParameters",
        args = {java.security.KeyStore.class}
    )
    public final void testPKIXParametersKeyStore04() throws Exception {


        KeyStore store = KeyStore.getInstance("PKCS12");
        KeyStoreTestPKCS12 k = new KeyStoreTestPKCS12();
        ByteArrayInputStream stream = new ByteArrayInputStream(k.keyStoreData);

        try {
            PKIXParameters p = new PKIXParameters(store);
        } catch (KeyStoreException e) {
            // ok
        }
        
        store = KeyStore.getInstance("PKCS12");
        store.load(stream, new String(KeyStoreTestPKCS12.keyStorePassword)
                .toCharArray());
        stream.close();
        
        try {
              PKIXParameters p = new PKIXParameters(store);
        } catch (InvalidAlgorithmParameterException e) {
            // ok
        }
        
        
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null,null);
        keystore.setCertificateEntry("test", TestUtils.rootCertificateSS);
        
        
        PKIXParameters p = new PKIXParameters(keystore);
    }
}
