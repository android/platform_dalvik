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
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import org.apache.harmony.security.tests.support.cert.MyCertificate;
import org.apache.harmony.security.tests.support.cert.MyFailingCertificate;
import org.apache.harmony.security.tests.support.cert.TestUtils;
import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Tests for <code>Certificate</code> fields and methods
 * 
 */
@TestTargetClass(Certificate.class)
public class CertificateTest extends TestCase {
    /**
     * Meaningless cert encoding just for testing purposes
     */
    private static final byte[] testEncoding = new byte[] { (byte) 1, (byte) 2,
            (byte) 3, (byte) 4, (byte) 5 };

    //
    // Tests
    //

    /**
     * Test for <code>Certificate(String type)</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "",
        method = "Certificate",
        args = {java.lang.String.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")
    public final void testCertificate() {
        try {
            Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
            assertTrue(Arrays.equals(testEncoding, c1.getEncoded()));
            assertEquals("TEST", c1.getPublicKey().getAlgorithm());
            assertTrue(Arrays.equals(
                    new byte[] { (byte) 1, (byte) 2, (byte) 3 }, c1
                            .getPublicKey().getEncoded()));
            assertEquals("TEST_FORMAT", c1.getPublicKey().getFormat());
            assertEquals("TEST_TYPE", c1.getType());
        } catch (CertificateEncodingException e) {
            fail("Unexpected CertificateEncodingException " + e.getMessage());
        }  
    }

    /**
     * Test for <code>hashCode()</code> method<br>
     * Assertion: returns hash of the <code>Certificate</code> instance
     * @throws CertificateEncodingException 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")   
    public final void testHashCode() throws CertificateEncodingException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);

        assertTrue(c1.hashCode() == c2.hashCode());

        assertFalse(c1.hashCode() == new MyCertificate("TEST_TYPE", cert
                .getEncoded()).hashCode());
        assertFalse(c1.hashCode() == cert.hashCode());
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
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testHashCodeEqualsObject() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);

        assertTrue((c1.hashCode() == c2.hashCode()) && c1.equals(c2));
        assertFalse(cert.equals(c1));
    }


    /**
     * Test for <code>getType()</code> method<br>
     * Assertion: returns this certificate type 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getType",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")      
    public final void testGetType() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertEquals("TEST_TYPE", c1.getType());
    }

    /**
     * Test #1 for <code>equals(Object)</code> method<br>
     * Assertion: object equals to itself 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies positive case.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")      
    public final void testEqualsObject01() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertTrue(c1.equals(c1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object equals to other <code>Certificate</code>
     * instance with the same state
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies positive case.",
        method = "equals",
        args = {java.lang.Object.class}
    )    
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")      
    public final void testEqualsObject02() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);
        assertTrue(c1.equals(c2) && c2.equals(c1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to <code>null</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies equals method with null as a parameter.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")      
    public final void testEqualsObject03() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertFalse(c1.equals(null));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to other which is not
     * instance of <code>Certificate</code>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies negative case.",
        method = "equals",
        args = {java.lang.Object.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")      
    public final void testEqualsObject04() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertFalse(c1.equals("TEST_TYPE"));
    }

    //
    // the following tests just call methods
    // that are abstract in <code>Certificate</code>
    // (So they just like signature tests)
    //

    /**
     * This test just calls <code>getEncoded()</code> method<br>
     * @throws CertificateException 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Can not verify CertificateEncodingException. indirectly tested",
        method = "getEncoded",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    @KnownFailure("Assertion does not evaluate to true... Works in javax.Certificate")
    public final void testGetEncoded() throws CertificateException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertNotNull(c1.getEncoded());
        
        assertTrue(Arrays.equals(TestUtils.rootCert.getBytes(),cert.getEncoded()));
        
        byte[] b = TestUtils.rootCert.getBytes();
        
        b[4] = (byte) 200;
        
        try {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        cert = cf.generateCertificate(stream);
        } catch (CertificateException e) {
            //ok
        }
    }

    /**
     * This test just calls <code>verify(PublicKey)</code> method<br>
     * 
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies only null as a parameter.",
        method = "verify",
        args = {java.security.PublicKey.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testVerifyPublicKey()
        throws InvalidKeyException,
               CertificateException,
               NoSuchAlgorithmException,
               NoSuchProviderException,
               SignatureException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.verify(null);
    }

    /**
     * This test just calls <code>verify(PublicKey,String)</code> method<br>
     *
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies only null as parameters.",
        method = "verify",
        args = {java.security.PublicKey.class, java.lang.String.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testVerifyPublicKeyString()
        throws InvalidKeyException,
               CertificateException,
               NoSuchAlgorithmException,
               NoSuchProviderException,
               SignatureException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.verify(null, null);
    }

    /**
     * This test just calls <code>toString()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testToString() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.toString();
    }

    /**
     * This test just calls <code>testGetPublicKey()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPublicKey",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testGetPublicKey() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.getPublicKey();
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
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")    
    public final void testWriteReplace() {
        MyCertificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        
        try {
            Object obj = c1.writeReplace();
            assertTrue(obj.toString().contains(
                    "java.security.cert.Certificate$CertificateRep"));
        } catch (ObjectStreamException e) {
            fail("Unexpected ObjectStreamException " + e.getMessage());
        }
    }
    
public class MyModifiablePublicKey implements PublicKey {
        
        private PublicKey key;
        private boolean modifiedAlgo;
        private String algo;
        private String format;
        private boolean modifiedFormat;
        private boolean modifiedEncoding;
        private byte[] encoding;
        
        public MyModifiablePublicKey(PublicKey k) {
            super();
            this.key = k;
        }

        public String getAlgorithm() {
            if (modifiedAlgo) {
                return algo;
            } else {
                return key.getAlgorithm();
            }
        }

        public String getFormat() {
            if (modifiedFormat) {
                return this.format;
            } else {
                return key.getFormat();
            }
            
        }

        public byte[] getEncoded() {
            if (modifiedEncoding) {
                return this.encoding;
            } else {
                return key.getEncoded();
            }
            
        }

        public long getSerVerUID() {
            return key.serialVersionUID;
        }
        
        public void setAlgorithm(String myAlgo) {
            modifiedAlgo = true;
            this.algo = myAlgo;
        }
        
        public void setFormat(String myFormat) {
            modifiedFormat = true;
            format = myFormat;
        }
        
        public void setEncoding(byte[] myEncoded) {
            modifiedEncoding = true;
            encoding = myEncoded;
        }
    }
    
    private Certificate cert;

    private Provider wrongProvider;

    private Provider useFulProvider;
   
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.initCertPathSSCertChain();
        cert = TestUtils.rootCertificateSS;
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        wrongProvider = cf.getProvider();
        useFulProvider = Security.getProviders("Signature.sha1WithRSAEncryption")[0];
    }
    
    /**
     * This test just calls <code>verify(PublicKey,String)</code> method<br>
     *
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Test fails: ClassCastException when SignatureException is expected",
        method = "verify",
        args = {java.security.PublicKey.class, java.lang.String.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")     
    public final void testVerifyPublicKeyString2() throws InvalidKeyException,
            CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {

        // real test
        cert.verify(cert.getPublicKey(), useFulProvider.getName());

        // Exception tests

        try {
            cert.verify(cert.getPublicKey(), "UnknownProvider");
        } catch (NoSuchProviderException e) {
            // ok
        }

        // This test has side effects affecting all other tests running later
        // on in the same vm instance. Maybe a better way would be to first add
        // a new provider, test if it works, then remove it and test if the
        // exception is thrown.
        // 
        // Security.removeProvider(wrongProvider.getName());
        // 
        // try {
        //     cert.verify(cert.getPublicKey(), wrongProvider.getName());
        // } catch (NoSuchAlgorithmException e) {
        //     // ok
        // }
        // 
        // Security.insertProviderAt(wrongProvider, oldPosition);

        /*
        PublicKey k = cert.getPublicKey();
        MyModifiablePublicKey tamperedKey = new MyModifiablePublicKey(k);
        tamperedKey.setAlgorithm("wrongAlgo");

        try {
            cert.verify(tamperedKey, provs[0].getName());
        } catch (SignatureException e) {
            // ok
        }
        
        try {
            cert.verify(c1.getPublicKey(), provs[0].getName());
        } catch (InvalidKeyException e) {
            // ok
        }
        */
    }
    
    /**
     * This test just calls <code>verify(PublicKey)</code> method<br>
     * 
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     * @throws IOException 
     * @throws InvalidAlgorithmParameterException 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Can't test exception for cases where the algorithm is unknown",
        method = "verify",
        args = {java.security.PublicKey.class}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")     
    public final void testVerifyPublicKey2() throws InvalidKeyException,
            CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException, InvalidAlgorithmParameterException, IOException {
        
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.verify(null);

        cert.verify(cert.getPublicKey());
        
        PublicKey k = cert.getPublicKey();

        MyModifiablePublicKey changedEncoding = new MyModifiablePublicKey(k);
        changedEncoding
                .setEncoding(new byte[cert.getEncoded().length - 1]);
        
        try {
            cert.verify(c1.getPublicKey());
            fail("expected InvalidKeyException");
        } catch (InvalidKeyException e) {
            // ok
        }

        try {
            cert.verify(changedEncoding);
            fail("Exception expected");
        } catch (Exception e) {
            // ok
        }        
    }
    
    /**
     * This test just calls <code>writeReplace()</code> method<br>
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "writeReplace",
        args = {}
    )
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")     
    public final void testWriteReplace2() {
        MyCertificate c1 = new MyFailingCertificate("TEST_TYPE", testEncoding);
        
        try {
            Object obj = c1.writeReplace();
        } catch (ObjectStreamException e) {
            //ok
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
            method = "Certificate.CertificateRep.readResolve",
            args = {}
        )
    })
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")     
    public void testSerializationSelf() throws Exception {
        TestUtils.initCertPathSSCertChain();

        SerializationTest.verifySelf(TestUtils.rootCertificateSS);
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
            method = "Certificate.CertificateRep.readResolve",
            args = {}
        )
    })
    @AndroidOnly("Gets security providers with specific signature algorithm: " +
            "Security.getProviders(\"Signature.sha1WithRSAEncryption\")")     
    public void testSerializationCompatibility() throws Exception {
        //create test file (once)
//        SerializationTest.createGoldenFile("device/dalvik/libcore/security/src/test/resources/serialization", this, TestUtils.rootCertificateSS);
        TestUtils.initCertPathSSCertChain();

        SerializationTest.verifyGolden(this, TestUtils.rootCertificateSS);
    }
}
