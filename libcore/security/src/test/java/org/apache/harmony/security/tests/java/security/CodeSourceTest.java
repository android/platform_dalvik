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
* @author Alexander V. Astapchuk
* @version $Revision$
*/

package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.File;
import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.cert.CertPath;
import java.security.cert.Certificate;

import org.apache.harmony.security.tests.support.TestCertUtils;

import junit.framework.TestCase;
@TestTargetClass(CodeSource.class)
/**
 * Unit test for CodeSource.
 * 
 */

public class CodeSourceTest extends TestCase {
    /**
     * 
     * Entry point for standalone runs.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(CodeSourceTest.class);
    }

    private java.security.cert.Certificate[] chain = null;

    /* Below are various URLs used during the testing */
    private static URL urlSite;

    private static URL urlDir; // must NOT end with '/'

    private static URL urlDirOtherSite; // same as urlDir, but another site

    private static URL urlDir_port80, urlDir_port81;

    /* must be exactly the same as urlDir, but with slash added */
    private static URL urlDirWithSlash;

    //private static URL urlDirFtp;
    private static URL urlDir_FileProtocol;

    private static URL urlDirIP;

    private static URL urlFile, urlFileWithAdditionalDirs, urlFileDirOtherDir;

    private static URL urlFileDirMinus;

    private static URL urlFileDirStar;

    private static URL urlRef1, urlRef2;
    
    private boolean init = false;

    private void init() {
        if (!init) {
            try {
                String siteName = "www.intel.com";
                InetAddress addr = InetAddress.getByName(siteName);
                String siteIP = addr.getHostAddress();
    
                urlSite = new URL("http://"+siteName+"");
                urlDir = new URL("http://"+siteName+"/drl_test");
                urlDirOtherSite = new URL("http://www.any-other-site-which-is-not-siteName.com/drl_test");
    
                urlDir_port80 = new URL("http://"+siteName+":80/drl_test");
                urlDir_port81 = new URL("http://"+siteName+":81/drl_test");
                urlDirWithSlash = new URL(urlDir + "/");
    
                //urlDirFtp = new URL("ftp://www.intel.com/drl_test");
                urlDir_FileProtocol = new URL("file://"+siteName+"/drl_test");
    
                urlDirIP = new URL("http://"+siteIP+"/drl_test");
    
                urlFile = new URL("http://"+siteName+"/drl_test/empty.jar");
                urlFileWithAdditionalDirs = new URL(
                        "http://"+siteName+"/drl_test/what/ever/here/empty.jar");
    
                urlFileDirMinus = new URL("http://"+siteName+"/drl_test/-");
                urlFileDirStar = new URL("http://"+siteName+"/drl_test/*");
                urlFileDirOtherDir = new URL("http://"+siteName+"/_test_drl_/*");
    
                urlRef1 = new URL("http://"+siteName+"/drl_test/index.html#ref1");
                urlRef2 = new URL("http://"+siteName+"/drl_test/index.html#ref2");
            } catch (MalformedURLException ex) {
                throw new Error(ex);
            } catch (UnknownHostException ex) {
                throw new Error(ex);
            } finally {
                init = true;
            }
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        init();
        chain = TestCertUtils.getCertChain();
    }

    /**
     * Tests hashCode().<br>
     * javadoc says nothing, so test DRL-specific implementation. 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() {
        // when nothing is specified, then hashCode obviously must be 0. 
        assertTrue(new CodeSource(null, (Certificate[]) null).hashCode() == 0);
        // only URL.hashCode is taken into account...
        assertTrue(new CodeSource(urlSite, (Certificate[]) null).hashCode() == urlSite
                .hashCode());
        // ... and certs[] does not affect it
        assertTrue(new CodeSource(urlSite, chain).hashCode() == urlSite
                .hashCode());
    }

    /**
     * Tests CodeSource(URL, Certificate[]).
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "CodeSource",
        args = {java.net.URL.class, java.security.cert.Certificate[].class}
    )
    public void testCodeSourceURLCertificateArray() {
        new CodeSource(null, (Certificate[]) null);
        new CodeSource(urlSite, (Certificate[]) null);
        new CodeSource(null, chain);
        new CodeSource(urlSite, chain);
    }

    /**
     * Tests CodeSource(URL, CodeSigner[]).
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies method with null parameters only",
        method = "CodeSource",
        args = {java.net.URL.class, java.security.CodeSigner[].class}
    )
    public void testCodeSourceURLCodeSignerArray() {
        if (!has_15_features()) {
            return;
        }
        new CodeSource(null, (CodeSigner[]) null);

    }

    /**
     * equals(Object) must return <code>false</code> for null
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Null parameter checked",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject_00() {
        CodeSource thiz = new CodeSource(urlSite, (Certificate[]) null);
        assertFalse(thiz.equals(null));

    }

    /**
     * equals(Object) must return <code>true</code> for the same object
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Same objects checked",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject_01() {
        CodeSource thiz = new CodeSource(urlSite, (Certificate[]) null);
        assertTrue(thiz.equals(thiz));
    }

    /**
     * Test for equals(Object)<br>
     * The signer certificate chain must contain the same set of certificates, but 
     * the order of the certificates is not taken into account.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject_02() {
        Certificate cert0 = new TestCertUtils.TestCertificate();
        Certificate cert1 = new TestCertUtils.TestCertificate();
        Certificate[] certs0 = new Certificate[] { cert0, cert1 };
        Certificate[] certs1 = new Certificate[] { cert1, cert0 };
        CodeSource thiz = new CodeSource(urlSite, certs0);
        CodeSource that = new CodeSource(urlSite, certs1);
        assertTrue(thiz.equals(that));
    }

    /**
     * Test for equals(Object)<br>
     * Checks that both 'null' and not-null URLs are taken into account - properly. 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject_04() {
        CodeSource thiz = new CodeSource(urlSite, (Certificate[]) null);
        CodeSource that = new CodeSource(null, (Certificate[]) null);
        assertFalse(thiz.equals(that));
        assertFalse(that.equals(thiz));

        that = new CodeSource(urlFile, (Certificate[]) null);
        assertFalse(thiz.equals(that));
        assertFalse(that.equals(thiz));
    }

    /**
     * Tests CodeSource.getCertificates().
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCertificates",
        args = {}
    )
    public void testGetCertificates_00() {
        assertNull(new CodeSource(null, (Certificate[]) null).getCertificates());
        java.security.cert.Certificate[] got = new CodeSource(null, chain)
                .getCertificates();
        // The returned array must be clone()-d ...
        assertNotSame(got, chain);
        // ... but must represent the same set of certificates
        assertTrue(checkEqual(got, chain));
    }

    /**
     * Tests whether the getCertificates() returns certificates obtained from 
     * the signers.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCertificates",
        args = {}
    )
    public void testGetCertificates_01() {
        if (!has_15_features()) {
            return;
        }
        CertPath cpath = TestCertUtils.getCertPath();
        Certificate[] certs = (Certificate[]) cpath.getCertificates().toArray();
        CodeSigner[] signers = { new CodeSigner(cpath, null) };
        CodeSource cs = new CodeSource(null, signers);
        Certificate[] got = cs.getCertificates();
        // The set of certificates must be exactly the same, 
        // but the order is not specified
        assertTrue(presented(certs, got));
        assertTrue(presented(got, certs));
    }

    /**
     * Checks whether two arrays of certificates represent the same same set of 
     * certificates - in the same order.
     * @param one first array 
     * @param two second array
     * @return <code>true</code> if both arrays represent the same set of 
     * certificates, 
     * <code>false</code> otherwise.
     */
    private static boolean checkEqual(java.security.cert.Certificate[] one,
            java.security.cert.Certificate[] two) {

        if (one == null) {
            return two == null;
        }

        if (two == null) {
            return false;
        }

        if (one.length != two.length) {
            return false;
        }

        for (int i = 0; i < one.length; i++) {
            if (one[i] == null) {
                if (two[i] != null) {
                    return false;
                }
            } else {
                if (!one[i].equals(two[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Performs a test whether the <code>what</code> certificates are all
     * presented in <code>where</code> certificates.
     * 
     * @param what - first array of Certificates
     * @param where  - second array of Certificates
     * @return <code>true</code> if each and every certificate from 'what' 
     * (including null) is presented in 'where' <code>false</code> otherwise
     */
    private static boolean presented(Certificate[] what, Certificate[] where) {
        boolean whereHasNull = false;
        for (int i = 0; i < what.length; i++) {
            if (what[i] == null) {
                if (whereHasNull) {
                    continue;
                }
                for (int j = 0; j < where.length; j++) {
                    if (where[j] == null) {
                        whereHasNull = true;
                        break;
                    }
                }
                if (!whereHasNull) {
                    return false;
                }
            } else {
                boolean found = false;
                for (int j = 0; j < where.length; j++) {
                    if (what[i].equals(where[j])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Tests CodeSource.getCodeSigners().
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCodeSigners",
        args = {}
    )
    public void testGetCodeSigners_00() {
        if (!has_15_features()) {
            return;
        }
        CodeSigner[] signers = { new CodeSigner(TestCertUtils.getCertPath(),
                null) };
        CodeSource cs = new CodeSource(null, signers);
        CodeSigner[] got = cs.getCodeSigners();
        assertNotNull(got);
        assertTrue(signers.length == got.length);
        // not sure whether they must be in the same order
        for (int i = 0; i < signers.length; i++) {
            CodeSigner s = signers[i];
            boolean found = false;
            for (int j = 0; j < got.length; j++) {
                if (got[j] == s) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
    
    /**
     * Tests CodeSource.getCodeSigners() for null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getCodeSigners",
        args = {}
    )
    public void testGetCoderSignersNull() throws Exception{
        assertNull(new CodeSource(new URL("http://url"), (Certificate[])null).getCodeSigners()); //$NON-NLS-1$
    }

    /**
     * Tests CodeSource.getLocation()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLocation",
        args = {}
    )
    public void testGetLocation() {
        assertTrue(new CodeSource(urlSite, (Certificate[]) null).getLocation() == urlSite);
        assertTrue(new CodeSource(urlSite, chain).getLocation() == urlSite);
        assertNull(new CodeSource(null, (Certificate[]) null).getLocation());
        assertNull(new CodeSource(null, chain).getLocation());
    }

    /**
     * Tests CodeSource.toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void testToString() {
        // Javadoc keeps silence about String's format, 
        // just make sure it can be invoked.
        new CodeSource(urlSite, chain).toString();
        new CodeSource(null, chain).toString();
        new CodeSource(null, (Certificate[]) null).toString();
    }

    /**
     * Tests whether we are running with the 1.5 features.<br>
     * The test is preformed by looking for (via reflection) the CodeSource's 
     * constructor  {@link CodeSource#CodeSource(URL, CodeSigner[])}.
     * @return <code>true</code> if 1.5 feature is presented, <code>false</code> 
     * otherwise.
     */
    private static boolean has_15_features() {
        Class klass = CodeSource.class;
        Class[] ctorArgs = { URL.class, new CodeSigner[] {}.getClass() };
        try {
            klass.getConstructor(ctorArgs);
        } catch (NoSuchMethodException ex) {
            // NoSuchMethod == Not RI.v1.5 and not DRL 
            return false;
        }
        return true;
    }

    /**
     * must not imply null CodeSource
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_00() {
        CodeSource cs0 = new CodeSource(null, (Certificate[]) null);
        assertFalse(cs0.implies(null));
    }

    /**
     * CodeSource with location=null && Certificate[] == null implies any other
     * CodeSource
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_01() throws Exception {
        CodeSource thizCS = new CodeSource(urlSite, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(null, (Certificate[]) null);
        assertTrue(thatCS.implies(thizCS));
        assertTrue(thatCS.implies(thatCS));

        assertFalse(thizCS.implies(thatCS));
    }

    /**
     * If this object's location equals codesource's location, then return true.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_02() throws Exception {
        CodeSource thizCS = new CodeSource(urlSite, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(thizCS.getLocation(),
                (Certificate[]) null);
        assertTrue(thizCS.implies(thatCS));
        assertTrue(thatCS.implies(thizCS));

    }

    /**
     * This object's protocol (getLocation().getProtocol()) must be equal to
     * codesource's protocol.
     */
    /*
     * FIXME
     * commented out for temporary, as there is no FTP:// protocol supported yet.
     * to be uncommented back.
     public void testImplies_03() throws Exception {
     CodeSource thizCS = new CodeSource(urlDir, (Certificate[]) null);
     CodeSource thatCS = new CodeSource(urlDirFtp, (Certificate[]) null);
     assertFalse(thizCS.implies(thatCS));
     assertFalse(thatCS.implies(thizCS));
     }
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_03_tmp() throws Exception {
        CodeSource thizCS = new CodeSource(urlDir, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlDir_FileProtocol,
                (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));
        assertFalse(thatCS.implies(thizCS));
    }

    /**
     * If this object's host (getLocation().getHost()) is not null, then the
     * SocketPermission constructed with this object's host must imply the
     * SocketPermission constructed with codesource's host.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_04() throws Exception {
        CodeSource thizCS = new CodeSource(urlDir, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlDirIP, (Certificate[]) null);

        assertTrue(thizCS.implies(thatCS));
        assertTrue(thatCS.implies(thizCS));

        // 
        // Check for another site - force to create SocketPermission
        //
        thatCS = new CodeSource(urlDirOtherSite, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));

        //
        // also check for getHost() == null
        //
        thizCS = new CodeSource(new URL("http", null, "file1"),
                (Certificate[]) null);
        thatCS = new CodeSource(new URL("http", "another.host.com", "file1"),
                (Certificate[]) null);
        // well, yes, this is accordint to the spec...
        assertTrue(thizCS.implies(thatCS));
        assertFalse(thatCS.implies(thizCS));
    }

    /**
     * If this object's port (getLocation().getPort()) is not equal to -1 (that
     * is, if a port is specified), it must equal codesource's port.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_05() throws Exception {
        CodeSource thizCS = new CodeSource(urlDir_port80, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlDir, (Certificate[]) null);

        assertTrue(thizCS.implies(thatCS));
        assertTrue(thatCS.implies(thizCS));

        thizCS = new CodeSource(urlDir, (Certificate[]) null);
        thatCS = new CodeSource(urlDir_port81, (Certificate[]) null);
        //assert*True* because thizCS has 'port=-1'
        assertTrue(thizCS.implies(thatCS));

        thizCS = new CodeSource(urlDir_port81, (Certificate[]) null);
        thatCS = new CodeSource(urlDir, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));
        //
        thizCS = new CodeSource(urlDir_port80, (Certificate[]) null);
        thatCS = new CodeSource(urlDir_port81, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));
    }

    /**
     * If this object's file (getLocation().getFile()) doesn't equal
     * codesource's file, then the following checks are made: ...
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_06() throws Exception {
        CodeSource thizCS = new CodeSource(urlFile, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlFile, (Certificate[]) null);
        assertTrue(thizCS.implies(thatCS));
    }

    /**
     * ... If this object's file ends with "/-", then codesource's file must
     * start with this object's file (exclusive the trailing "-").
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_07() throws Exception {
        CodeSource thiz = new CodeSource(urlFileDirMinus, (Certificate[]) null);
        CodeSource that = new CodeSource(urlFile, (Certificate[]) null);
        assertTrue(thiz.implies(that));

        that = new CodeSource(urlFileWithAdditionalDirs, (Certificate[]) null);
        assertTrue(thiz.implies(that));

        that = new CodeSource(urlFileDirOtherDir, (Certificate[]) null);
        assertFalse(thiz.implies(that));
    }

    /**
     * ... If this object's file ends with a "/*", then codesource's file must
     * start with this object's file and must not have any further "/"
     * separators.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_08() throws Exception {
        CodeSource thiz = new CodeSource(urlFileDirStar, (Certificate[]) null);
        CodeSource that = new CodeSource(urlFile, (Certificate[]) null);
        assertTrue(thiz.implies(that));
        that = new CodeSource(urlFileWithAdditionalDirs, (Certificate[]) null);
        assertFalse(thiz.implies(that));
        //
        that = new CodeSource(urlFileDirOtherDir, (Certificate[]) null);
        assertFalse(thiz.implies(that));
        // must not have any further '/'
        that = new CodeSource(new URL(urlFile.toString() + "/"),
                (Certificate[]) null);
        assertFalse(thiz.implies(that));
    }

    /**
     * ... If this object's file doesn't end with a "/", then codesource's file
     * must match this object's file with a '/' appended.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_09() throws Exception {
        CodeSource thizCS = new CodeSource(urlDir, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlDirWithSlash,
                (Certificate[]) null);
        assertTrue(thizCS.implies(thatCS));
        assertFalse(thatCS.implies(thizCS));
    }

    /**
     * If this object's reference (getLocation().getRef()) is not null, it must
     * equal codesource's reference.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_0A() throws Exception {
        CodeSource thizCS = new CodeSource(urlRef1, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(urlRef1, (Certificate[]) null);
        assertTrue(thizCS.implies(thatCS));

        thizCS = new CodeSource(urlRef1, (Certificate[]) null);
        thatCS = new CodeSource(urlRef2, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));

    }

    /**
     * If this certificates are not null, then all of this certificates should
     * be presented in certificates of that codesource.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_0B() {

        Certificate c0 = new TestCertUtils.TestCertificate("00");
        Certificate c1 = new TestCertUtils.TestCertificate("01");
        Certificate c2 = new TestCertUtils.TestCertificate("02");
        Certificate[] thizCerts = { c0, c1 };
        Certificate[] thatCerts = { c1, c0, c2 };

        CodeSource thiz = new CodeSource(urlSite, thizCerts);
        CodeSource that = new CodeSource(urlSite, thatCerts);
        // two CodeSource-s with different set of certificates
        assertTrue(thiz.implies(that));

        //
        that = new CodeSource(urlSite, (Certificate[]) null);
        // 'thiz' has set of certs, while 'that' has no certs. URL-s are the 
        // same.
        assertFalse(thiz.implies(that));
        assertTrue(that.implies(thiz));
    }

    /**
     * Testing with special URLs like 'localhost', 'file://' scheme ...
     * These special URLs have a special processing in implies(), 
     * so they need to be covered and performance need to be checked 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_0C() throws Exception {
        URL url0 = new URL("http://localhost/someDir");
        URL url1 = new URL("http://localhost/someOtherDir");

        CodeSource thizCS = new CodeSource(url0, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(url1, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));
        assertFalse(thatCS.implies(thizCS));
    }

    /**
     * Testing with special URLs like 'localhost', 'file://' scheme ...
     * These special URLs have a special processing in implies(), 
     * so they need to be covered and performance need to be checked 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "implies",
        args = {java.security.CodeSource.class}
    )
    public void testImplies_0D() throws Exception {
        URL url0 = new URL("file:///" + System.getProperty("java.io.tmpdir")
                + File.separator + "someDir");
        URL url1 = new URL("file:///" + System.getProperty("java.io.tmpdir")
                + File.separator + "someOtherDir");
        CodeSource thizCS = new CodeSource(url0, (Certificate[]) null);
        CodeSource thatCS = new CodeSource(url1, (Certificate[]) null);
        assertFalse(thizCS.implies(thatCS));
        assertFalse(thatCS.implies(thizCS));
    }
}
