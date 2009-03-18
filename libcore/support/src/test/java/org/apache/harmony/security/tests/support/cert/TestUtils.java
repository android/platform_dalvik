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

package org.apache.harmony.security.tests.support.cert;

import tests.support.resource.Support_Resources;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PolicyNode;
import java.security.cert.PolicyQualifierInfo;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * java.security.cert test utilities
 * 
 */
public class TestUtils {
    // Certificate type used during testing
    private static final String certType = "X.509";
    // Key store type used during testing
    private static final String keyStoreType = "BKS";
    // The file name prefix to load keystore from
    private static final String keyStoreFileName = "test." + keyStoreType
            + ".ks";
    //
    // The file name suffixes to load keystore from
    //  *.ks1 - keystore containing untrusted certificates only
    //  *.ks2 - keystore containing trusted certificates only
    //  *.ks3 - keystore containing both trusted and untrusted certificates
    //
    public static final int UNTRUSTED = 1;
    public static final int TRUSTED = 2;
    public static final int TRUSTED_AND_UNTRUSTED = 3;
    //
    // Common passwords for all test keystores
    //
    private final static char[] storepass =
        new char[] {'s','t','o','r','e','p','w','d'};

    /**
     * Creates <code>TrustAnchor</code> instance
     * constructed using self signed test certificate
     *
     * @return <code>TrustAnchor</code> instance
     */
    public static TrustAnchor getTrustAnchor() {
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance(certType);
        } catch (CertificateException e) {
            // requested cert type is not available in the
            // default provider package or any of the other provider packages
            // that were searched
            throw new RuntimeException(e);
        }
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new ByteArrayInputStream(
                    getEncodedX509Certificate()));
            X509Certificate c1 = (X509Certificate)cf.generateCertificate(bis);

            return new TrustAnchor(c1, null);
        } catch (Exception e) {
            // all failures are fatal
            throw new RuntimeException(e);
        } finally {
            if (bis != null) {
                try {
                    bis.close() ;
                } catch (IOException ign) {}
            }
        }
    }

    /**
     * Creates <code>Set</code> of <code>TrustAnchor</code>s
     * containing single element (self signed test certificate).
     * @return Returns <code>Set</code> of <code>TrustAnchor</code>s
     */
    public static Set<TrustAnchor> getTrustAnchorSet() {
        TrustAnchor ta = getTrustAnchor();
        if (ta == null) {
            return null;
        }
        HashSet<TrustAnchor> set = new HashSet<TrustAnchor>();
        if (!set.add(ta)) {
            throw new RuntimeException("Could not create trust anchor set");
        }
        return set;
    }

    /**
     * Creates test <code>KeyStore</code> instance
     * 
     * @param initialize
     *  Do not initialize returned <code>KeyStore</code> if false
     * 
     * @param testKeyStoreType 
     *  this parameter ignored if <code>initialize</code> is false;
     *  The following types supported:<br>
     *  1 - <code>KeyStore</code> with untrusted certificates only<br>
     *  2 - <code>KeyStore</code> with trusted certificates only<br>
     *  3 - <code>KeyStore</code> with both trusted and untrusted certificates
     * 
     * @return Returns test <code>KeyStore</code> instance
     */
    public static KeyStore getKeyStore(boolean initialize,
            int testKeyStoreType) {
        BufferedInputStream bis = null;
        try {
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            if (initialize) {
                String fileName = keyStoreFileName + testKeyStoreType;
                ks.load(Support_Resources.getResourceStream(fileName),
                        storepass);
            }
            return ks;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (initialize && bis != null) {
                try {
                    bis.close();
                } catch (IOException ign) {}
            }
        }
    }

    /**
     * Creates <code>List</code> of <code>CollectionCertStores</code>
     *
     * @return The list created
     * 
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     */
    public static List<CertStore> getCollectionCertStoresList()
        throws InvalidAlgorithmParameterException,
               NoSuchAlgorithmException {
        CertStore cs = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters());
        ArrayList<CertStore> l = new ArrayList<CertStore>();
        if (!l.add(cs)) {
            throw new RuntimeException("Could not create cert stores list");
        }
        return l;
    }

    /**
     * Creates stub implementation of the <code>PKIXCertPathChecker</code>
     *
     * @return Stub implementation of the <code>PKIXCertPathChecker</code>
     */
    public static PKIXCertPathChecker getTestCertPathChecker() {
        // stub implementation for testing purposes only
        return new PKIXCertPathChecker() {
            private boolean forward = false;


            @SuppressWarnings({"unused", "unchecked"})
            public void check(Certificate arg0, Collection arg1)
                    throws CertPathValidatorException {
            }

            public Set<String> getSupportedExtensions() {
                return null;
            }

            @SuppressWarnings("unused")
            public void init(boolean arg0) throws CertPathValidatorException {
                forward = arg0;
            }

            public boolean isForwardCheckingSupported() {
                // just to check this checker state
                return forward;
            }
        };
    }

    /**
     * Creates policy tree stub containing two <code>PolicyNode</code>s
     * for testing purposes
     *
     * @return root <code>PolicyNode</code> of the policy tree
     */
    public static PolicyNode getPolicyTree() {
        return new PolicyNode() {
            final PolicyNode parent = this;
            public int getDepth() {
                // parent
                return 0;
            }

            public boolean isCritical() {
                return false;
            }

            public String getValidPolicy() {
                return null;
            }

            public PolicyNode getParent() {
                return null;
            }

            public Iterator<PolicyNode> getChildren() {
                PolicyNode child = new PolicyNode() {
                    public int getDepth() {
                        // child
                        return 1;
                    }

                    public boolean isCritical() {
                        return false;
                    }

                    public String getValidPolicy() {
                        return null;
                    }

                    public PolicyNode getParent() {
                        return parent;
                    }

                    public Iterator<PolicyNode> getChildren() {
                        return null;
                    }

                    public Set<String> getExpectedPolicies() {
                        return null;
                    }

                    public Set<? extends PolicyQualifierInfo> getPolicyQualifiers() {
                        return null;
                    }
                };
                HashSet<PolicyNode> s = new HashSet<PolicyNode>();
                s.add(child);
                return s.iterator();
            }

            public Set<String> getExpectedPolicies() {
                return null;
            }

            public Set<? extends PolicyQualifierInfo> getPolicyQualifiers() {
                return null;
            }
        };
    }
    // X.509 encoded certificate
    private static final String ENCODED_X509_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDHTCCAtsCBEFT72swCwYHKoZIzjgEAwUAMHQxCzAJBgNVBAYTAlJVMQwwCgYDVQQIEwNOU08x\n"
            + "FDASBgNVBAcTC05vdm9zaWJpcnNrMQ4wDAYDVQQKEwVJbnRlbDEVMBMGA1UECxMMRFJMIFNlY3Vy\n"
            + "aXR5MRowGAYDVQQDExFWbGFkaW1pciBNb2xvdGtvdjAeFw0wNDA5MjQwOTU2NTlaFw0wNjA1MTcw\n"
            + "OTU2NTlaMHQxCzAJBgNVBAYTAlJVMQwwCgYDVQQIEwNOU08xFDASBgNVBAcTC05vdm9zaWJpcnNr\n"
            + "MQ4wDAYDVQQKEwVJbnRlbDEVMBMGA1UECxMMRFJMIFNlY3VyaXR5MRowGAYDVQQDExFWbGFkaW1p\n"
            + "ciBNb2xvdGtvdjCCAbgwggEsBgcqhkjOOAQBMIIBHwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3Ujzv\n"
            + "RADDHj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7\n"
            + "qdf+t8Yb+DtX58aophUPBPuD9tPFHsMCNVQTWhaRMvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8V\n"
            + "IwvMspK5gqLrhAvwWBz1AoGBAPfhoIXWmz3ey7yrXDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrU\n"
            + "WU/mcQcQgYC0SRZxI+hMKBYTt88JMozIpuE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEk\n"
            + "O8yk8b6oUZCJqIPf4VrlnwaSi2ZegHtVJWQBTDv+z0kqA4GFAAKBgQDiNmj9jgWu1ILYqYWcUhNN\n"
            + "8CjjRitf80yWP/s/565wZz3anb2w72jum63mdShDko9eOOOd1hiVuiBnNhSL7D6JfIYBJvNXr1av\n"
            + "Gw583BBv12OBgg0eAW/GRWBn2Ak2JjsoBc5x2c1HAEufakep7T6RoC+n3lqbKPKyHWVdfqQ9KTAL\n"
            + "BgcqhkjOOAQDBQADLwAwLAIUaRS3C9dXcMbrOAhmidFBr7oMvH0CFEC3LUwfLJX5gY8P6uxpkPx3\n"
            + "JDSM\n" + "-----END CERTIFICATE-----\n";

    public static byte[] getEncodedX509Certificate() {
        return ENCODED_X509_CERTIFICATE.getBytes();
    }
    
    /**
     * Returns X.509 certificate encoding corresponding to version v1.
     * 
     * Certificate encoding was created by hands according to X.509 Certificate
     * ASN.1 notation. The certificate encoding has the following encoded
     * field values:<br> 
     * - version: 1<br>
     * - serialNumber: 5<br>
     * - issuer: CN=Z<br>
     * - notBefore: 13 Dec 1999 14:15:16<br>
     * - notAfter: 01 Jan 2000 00:00:00<br>
     * - subject: CN=Y<br>
     * 
     * @return X.509 certificate encoding corresponding to version v1.
     */
    public static byte[] getX509Certificate_v1() {
        return new byte[] {
        // Certificate: SEQUENCE
            0x30, 0x6B,

            //
            // TBSCertificate: SEQUENCE {
            //
            0x30, 0x5C,

            // version: [0] EXPLICIT Version DEFAULT v1
            (byte) 0xA0, 0x03, 0x02, 0x01, 0x00,

            // serialNumber: CertificateSerialNumber
            0x02, 0x01, 0x05,

            // signature: AlgorithmIdentifier
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY

            //issuer: Name
            0x30, 0x0C, 0x31, 0x0A, 0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x13, 0x01, 0x5A, // CN=Z

            //validity: Validity
            0x30, 0x1E, // SEQUENCE
            // notBefore: UTCTime
            0x17, 0x0D, 0x39, 0x39, 0x31, 0x32, 0x31, 0x33, 0x31, 0x34, 0x31,
            0x35, 0x31, 0x36, 0x5A, // 13 Dec 1999 14:15:16
            // notAfter:  UTCTime
            0x17, 0x0D, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31, 0x30, 0x30, 0x30,
            0x30, 0x30, 0x30, 0x5A, // 01 Jan 2000 00:00:00

            //subject: Name
            0x30, 0x0C, 0x31, 0x0A, 0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x13, 0x01, 0x59, // CN=Y
            //SubjectPublicKeyInfo  ::=  SEQUENCE  {
            //    algorithm            AlgorithmIdentifier,
            //    subjectPublicKey     BIT STRING  }
            0x30, 0x0D, // SEQUENCE
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY
            0x03, 0x02, 0x00, 0x01, // subjectPublicKey

            // issuerUniqueID - missed
            // subjectUniqueID - missed
            // extensions - missed

            // } end TBSCertificate

            //
            // signatureAlgorithm: AlgorithmIdentifier
            //
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY

            //
            // signature: BIT STRING  
            //
            0x03, 0x02, 0x00, 0x01 };
    }

    /**
     * Returns X.509 certificate encoding corresponding to version v3.
     * 
     * Certificate encoding was created by hands according to X.509 Certificate
     * ASN.1 notation. The certificate encoding has the following encoded
     * field values:<br> 
     * - version: 3<br>
     * - serialNumber: 5<br>
     * - issuer: CN=Z<br>
     * - notBefore: 13 Dec 1999 14:15:16<br>
     * - notAfter: 01 Jan 2000 00:00:00<br>
     * - subject: CN=Y<br>
     * - extensions:
     *       1) AuthorityKeyIdentifier(OID=2.5.29.35): no values in it(empty sequence) 
     * 
     * @return X.509 certificate encoding corresponding to version v3.
     */
    public static byte[] getX509Certificate_v3() {
        return new byte[] {
        // Certificate: SEQUENCE
            0x30, 0x7D,

            //
            // TBSCertificate: SEQUENCE {
            //
            0x30, 0x6E,

            // version: [0] EXPLICIT Version DEFAULT v1
            (byte) 0xA0, 0x03, 0x02, 0x01, 0x02,

            // serialNumber: CertificateSerialNumber
            0x02, 0x01, 0x05,

            // signature: AlgorithmIdentifier
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY

            //issuer: Name
            0x30, 0x0C, 0x31, 0x0A, 0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x13, 0x01, 0x5A, // CN=Z

            //validity: Validity
            0x30, 0x1E, // SEQUENCE
            // notBefore: UTCTime
            0x17, 0x0D, 0x39, 0x39, 0x31, 0x32, 0x31, 0x33, 0x31, 0x34, 0x31,
            0x35, 0x31, 0x36, 0x5A, // 13 Dec 1999 14:15:16
            // notAfter:  UTCTime
            0x17, 0x0D, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31, 0x30, 0x30, 0x30,
            0x30, 0x30, 0x30, 0x5A, // 01 Jan 2000 00:00:00

            //subject: Name
            0x30, 0x0C, 0x31, 0x0A, 0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x13, 0x01, 0x59, // CN=Y
            //SubjectPublicKeyInfo  ::=  SEQUENCE  {
            //    algorithm            AlgorithmIdentifier,
            //    subjectPublicKey     BIT STRING  }
            0x30, 0x0D, // SEQUENCE
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY
            0x03, 0x02, 0x00, 0x01, // subjectPublicKey

            // issuerUniqueID - missed
            // subjectUniqueID - missed
            // extensions : [3]  EXPLICIT Extensions OPTIONAL
            (byte) 0xA3, 0x10,
            // Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
            0x30, 0x0E,
            // Extension  ::=  SEQUENCE  {
            // extnID      OBJECT IDENTIFIER,
            // critical    BOOLEAN DEFAULT FALSE,
            // extnValue   OCTET STRING  }

            // 1) AuthorityKeyIdentifier extension (see HARMONY-3384)
            0x30, 0x0C,
            0x06, 0x03, 0x55, 0x1D, 0x23, // OID = 2.5.29.35
            0x01, 0x01, 0x00, // critical = FALSE
            0x04, 0x02, 0x30, 0x00, // extnValue: MUST be empty sequence
            // missed: keyIdentifier
            // missed: authorityCertIssuer
            // missed" authorityCertSerialNumber

            // } end TBSCertificate

            //
            // signatureAlgorithm: AlgorithmIdentifier
            //
            0x30, 0x07, // SEQUENCE
            0x06, 0x02, 0x03, 0x05,//OID
            0x01, 0x01, 0x07, //ANY

            //
            // signature: BIT STRING  
            //
            0x03, 0x02, 0x00, 0x01 };
    }

    /**
     * Returns X.509 CRL encoding corresponding to version v1.
     * 
     * CRL encoding was created by hands according to X.509 CRL ASN.1
     * notation. The CRL encoding has the following encoded field values:<br> 
     * - version: 1<br>
     * - issuer: CN=Z<br>
     * - thisUpdate: 01 Jan 2001 01:02:03<br>
     * 
     * @return X.509 CRL encoding corresponding to version v1.
     */
    public static byte[] getX509CRL_v1() {
        return new byte[] {
                //CertificateList: SEQUENCE
                0x30, 0x35, 
                
                // TBSCertList: SEQUENCE  
                0x30, 0x27,
                // Version: INTEGER OPTIONAL
                // 0x02, 0x01, 0x01, - missed here cause it is v1
                // signature: AlgorithmIdentifier
                0x30, 0x06, // SEQUENCE
                0x06, 0x01, 0x01, // OID
                0x01, 0x01, 0x11, // ANY
                // issuer: Name                   
                0x30, 0x0C, 0x31, 0x0A, 0x30, 0x08, 0x06, 0x03, 0x55, 0x04,
                0x03, 0x13, 0x01, 0x5A, // CN=Z 
                // thisUpdate: ChoiceOfTime
                // GeneralizedTime: 01 Jan 2001 01:02:03
                0x18, 0x0F, 0x32, 0x30, 0x30, 0x31, 0x30, 0x31, 0x30, 0x31,
                0x30, 0x31, 0x30, 0x32, 0x30, 0x33, 0x5A,
                
                // nextUpdate - missed
                // revokedCertificates - missed
                // crlExtensions - missed
                
                // signatureAlgorithm: AlgorithmIdentifier
                0x30, 0x06, // SEQUENCE
                0x06, 0x01, 0x01, //OID
                0x01, 0x01, 0x11, //ANY
                // signature: BIT STRING  
                0x03, 0x02, 0x00, 0x01 };
    }
    //--------------------------------------------------------------------------
    
    // Second example
    /**
     * Certificate:
     * <pre>
    Data:
        Version: 3 (0x2)
        Serial Number: 0 (0x0)
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: C=AN, ST=Android, O=Android, OU=Android, CN=Android/emailAddress=android
        Validity
            Not Before: Dec  9 16:35:30 2008 GMT
            Not After : Dec  9 16:35:30 2011 GMT
        Subject: C=AN, ST=Android, O=Android, OU=Android, CN=Android/emailAddress=android
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
            RSA Public Key: (1024 bit)
                Modulus (1024 bit):
                    00:c5:fb:5e:68:37:82:1d:58:ed:cb:31:8c:08:7f:
                    51:31:4c:68:40:8c:4d:07:a1:0e:18:36:02:6b:89:
                    92:c1:cf:88:1e:cf:00:22:00:8c:37:e8:6a:76:94:
                    71:53:81:78:e1:48:94:fa:16:61:93:eb:a0:ee:62:
                    9d:6a:d2:2c:b8:77:9d:c9:36:d5:d9:1c:eb:26:3c:
                    43:66:4d:7b:1c:1d:c7:a1:37:66:e2:84:54:d3:ed:
                    21:dd:01:1c:ec:9b:0c:1e:35:e9:37:15:9d:2b:78:
                    a8:3b:11:3a:ee:c2:de:55:44:4c:bd:40:8d:e5:52:
                    b0:fc:53:33:73:4a:e5:d0:df
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            X509v3 Subject Key Identifier: 
                4B:E3:22:14:AD:0A:14:46:B7:52:31:8B:AB:9E:5A:62:F3:98:37:80
            X509v3 Authority Key Identifier: 
                keyid:4B:E3:22:14:AD:0A:14:46:B7:52:31:8B:AB:9E:5A:62:F3:98:37:80
                DirName:/C=AN/ST=Android/O=Android/OU=Android/CN=Android/emailAddress=android
                serial:00

            X509v3 Basic Constraints: 
                CA:TRUE
    Signature Algorithm: sha1WithRSAEncryption
        72:4f:12:8a:4e:61:b2:9a:ba:58:17:0b:55:96:f5:66:1c:a8:
        ba:d1:0f:8b:9b:2d:ab:a8:00:ac:7f:99:7d:f6:0f:d7:85:eb:
        75:4b:e5:42:37:71:46:b1:4a:b0:1b:17:e4:f9:7c:9f:bd:20:
        75:35:9f:27:8e:07:95:e8:34:bd:ab:e4:10:5f:a3:7b:4c:56:
        69:d4:d0:f1:e9:74:15:2d:7f:77:f0:38:77:eb:8a:99:f3:a9:
        88:f0:63:58:07:b9:5a:61:f8:ff:11:e7:06:a1:d1:f8:85:fb:
        99:1c:f5:cb:77:86:36:cd:43:37:99:09:c2:9a:d8:f2:28:05:
        06:0c

     * </pre>
     */
    public static final String rootCert = "-----BEGIN CERTIFICATE-----\n" + 
    "MIIDGzCCAoSgAwIBAgIBADANBgkqhkiG9w0BAQUFADBtMQswCQYDVQQGEwJBTjEQ\n" + 
    "MA4GA1UECBMHQW5kcm9pZDEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5k\n" + 
    "cm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEWMBQGCSqGSIb3DQEJARYHYW5kcm9pZDAe\n" + 
    "Fw0wODEyMDkxNjM1MzBaFw0xMTEyMDkxNjM1MzBaMG0xCzAJBgNVBAYTAkFOMRAw\n" + 
    "DgYDVQQIEwdBbmRyb2lkMRAwDgYDVQQKEwdBbmRyb2lkMRAwDgYDVQQLEwdBbmRy\n" + 
    "b2lkMRAwDgYDVQQDEwdBbmRyb2lkMRYwFAYJKoZIhvcNAQkBFgdhbmRyb2lkMIGf\n" + 
    "MA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDF+15oN4IdWO3LMYwIf1ExTGhAjE0H\n" + 
    "oQ4YNgJriZLBz4gezwAiAIw36Gp2lHFTgXjhSJT6FmGT66DuYp1q0iy4d53JNtXZ\n" + 
    "HOsmPENmTXscHcehN2bihFTT7SHdARzsmwweNek3FZ0reKg7ETruwt5VREy9QI3l\n" + 
    "UrD8UzNzSuXQ3wIDAQABo4HKMIHHMB0GA1UdDgQWBBRL4yIUrQoURrdSMYurnlpi\n" + 
    "85g3gDCBlwYDVR0jBIGPMIGMgBRL4yIUrQoURrdSMYurnlpi85g3gKFxpG8wbTEL\n" + 
    "MAkGA1UEBhMCQU4xEDAOBgNVBAgTB0FuZHJvaWQxEDAOBgNVBAoTB0FuZHJvaWQx\n" + 
    "EDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWQxFjAUBgkqhkiG9w0B\n" + 
    "CQEWB2FuZHJvaWSCAQAwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQUFAAOBgQBy\n" + 
    "TxKKTmGymrpYFwtVlvVmHKi60Q+Lmy2rqACsf5l99g/Xhet1S+VCN3FGsUqwGxfk\n" + 
    "+XyfvSB1NZ8njgeV6DS9q+QQX6N7TFZp1NDx6XQVLX938Dh364qZ86mI8GNYB7la\n" + 
    "Yfj/EecGodH4hfuZHPXLd4Y2zUM3mQnCmtjyKAUGDA==\n" + 
    "-----END CERTIFICATE-----";
    
    public static final String rootPrivateKey =
         "-----BEGIN RSA PRIVATE KEY-----\n" + 
         "Proc-Type: 4,ENCRYPTED\n" + 
         "DEK-Info: DES-EDE3-CBC,D9682F66FDA316E5\n" + 
         "\n" + 
         "8lGaQPlUZ/iHhdldB//xfNUrZ3RAkBthzKg+n9HBJsjztXXAZ40NGYZmgvpgnfmr\n" + 
         "7ZJxHxYHFc3GAmBBk9v+/dA8E5yWJa71roffWMQUuFNfGzHhGTOxvNC04W7yAajs\n" + 
         "CPuyI+xnAAo73F7NVTiqX3NVgu4bB8RVxJyToMe4M289oh93YvxWQ4buVTf0ErJ8\n" + 
         "Yc8+0ugpfXjGfRhL36qj6B1CcV7NMdXAVExrGlTf0TWT9wVbiROk4XaoaFuWh17h\n" + 
         "11NEDjsKQ8T4M9kRdC+tKfST8sLik1Pq6jRLIKeX8GQd7tV1IWVZ3KcQBJwu9zLq\n" + 
         "Hi0GTSF7IWCdwXjDyniMQiSbkmHNP+OnVyhaqew5Ooh0uOEQq/KWFewXg7B3VMr0\n" + 
         "l6U8sBX9ODGeW0wVdNopvl17udCkV0xm3S+MRZDnZiTlAXwKx/a/gyf5R5XYp3S0\n" + 
         "0eqrfy2o6Ax4hRkwcNJ2KMeLQNIiYYWKABQj5/i4TYZV6npCIXOnQEkXa9DmqyUE\n" + 
         "qB7eFj5FcXeqQ8ERmsLveWArsLDn2NNPdv5EaKIs2lrvwoKYeYF7hrKNpifq+QqS\n" + 
         "u1kN+KHjibcF42EAUozNVmkHsW8VqlywAs4MsMwxU0D57cVGWycuSedraKhc0D6j\n" + 
         "a4pQOWWY3ZMLoAA1ZmHG9cjDPqcJt0rqk5AhSBRmGVUccfkP7dk9KyJQizro87LI\n" + 
         "u7zWwMIqTfmlhyfAP0AWjrt/bMN9heGByVA55xkyCdSEVaC5gsIfmGpNy4u+wbZ9\n" + 
         "rSWVuTfAbjW0n0FW+CDS1LgdjXNkeAP2Uvc1QgVRCPdA23WniLFFJQ==\n" + 
         "-----END RSA PRIVATE KEY-----";
    
    /**
     * Certificate:
     * <pre>
    Data:
        Version: 3 (0x2)
        Serial Number: 1 (0x1)
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: C=AN, ST=Android, O=Android, OU=Android, CN=Android/emailAddress=android
        Validity
            Not Before: Dec  9 16:40:35 2008 GMT
            Not After : Dec  9 16:40:35 2009 GMT
        Subject: C=AN, ST=Android, L=Android, O=Android, OU=Android, CN=Android Certificate/emailAddress=android
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
            RSA Public Key: (1024 bit)
                Modulus (1024 bit):
                    00:b8:e3:de:c7:a9:40:47:2c:2a:6f:f5:2a:f4:cd:
                    f2:2d:40:fa:15:3f:1c:37:66:73:a5:67:4d:5b:a0:
                    b6:b1:dd:dc:bf:01:c7:e2:c1:48:1a:8f:1c:ce:ec:
                    b0:a2:55:29:9a:1b:3a:6e:cc:7b:d7:65:ae:0b:05:
                    34:03:8a:af:db:f0:dc:01:80:92:03:b4:13:e5:d6:
                    fd:79:66:7f:c3:1a:62:d5:5e:3d:c0:19:a4:42:15:
                    47:19:e6:f0:c8:b7:e2:7b:82:a2:c7:3d:df:ac:8c:
                    d5:bc:39:b8:e5:93:ac:3f:af:30:b7:cc:00:a8:00:
                    f3:38:23:b0:97:0e:92:b1:1b
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            X509v3 Basic Constraints: 
                CA:FALSE
            Netscape Comment: 
                OpenSSL Generated Certificate
            X509v3 Subject Key Identifier: 
                88:4D:EC:16:26:A7:76:F5:26:43:BC:34:99:DF:D5:EA:7B:F8:5F:DE
            X509v3 Authority Key Identifier: 
                keyid:4B:E3:22:14:AD:0A:14:46:B7:52:31:8B:AB:9E:5A:62:F3:98:37:80

    Signature Algorithm: sha1WithRSAEncryption
        55:73:95:e6:4c:40:fc:fd:52:8a:5f:83:15:49:73:ca:f3:d8:
        5f:bb:d6:f5:2e:90:e6:7f:c3:7d:4d:27:d3:45:c6:53:9b:aa:
        e3:32:99:40:b3:a9:d3:14:7d:d5:e6:a7:70:95:30:6e:dc:8c:
        7b:48:e1:98:d1:65:7a:eb:bf:b0:5c:cd:c2:eb:31:5e:b6:e9:
        df:56:95:bc:eb:79:74:27:5b:6d:c8:55:63:09:d3:f9:e2:40:
        ba:b4:a2:c7:2c:cb:b1:3a:c2:d8:0c:21:31:ee:68:7e:97:ce:
        98:22:2e:c6:cf:f0:1a:11:04:ca:9a:06:de:98:48:85:ac:6c:
        6f:98
     * </pre>
     */
    public static final String  endCert = 
        "-----BEGIN CERTIFICATE-----\n" + 
        "MIIC6jCCAlOgAwIBAgIBATANBgkqhkiG9w0BAQUFADBtMQswCQYDVQQGEwJBTjEQ\n" + 
        "MA4GA1UECBMHQW5kcm9pZDEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5k\n" + 
        "cm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEWMBQGCSqGSIb3DQEJARYHYW5kcm9pZDAe\n" + 
        "Fw0wODEyMDkxNjQwMzVaFw0wOTEyMDkxNjQwMzVaMIGLMQswCQYDVQQGEwJBTjEQ\n" + 
        "MA4GA1UECBMHQW5kcm9pZDEQMA4GA1UEBxMHQW5kcm9pZDEQMA4GA1UEChMHQW5k\n" + 
        "cm9pZDEQMA4GA1UECxMHQW5kcm9pZDEcMBoGA1UEAxMTQW5kcm9pZCBDZXJ0aWZp\n" + 
        "Y2F0ZTEWMBQGCSqGSIb3DQEJARYHYW5kcm9pZDCBnzANBgkqhkiG9w0BAQEFAAOB\n" + 
        "jQAwgYkCgYEAuOPex6lARywqb/Uq9M3yLUD6FT8cN2ZzpWdNW6C2sd3cvwHH4sFI\n" + 
        "Go8czuywolUpmhs6bsx712WuCwU0A4qv2/DcAYCSA7QT5db9eWZ/wxpi1V49wBmk\n" + 
        "QhVHGebwyLfie4Kixz3frIzVvDm45ZOsP68wt8wAqADzOCOwlw6SsRsCAwEAAaN7\n" + 
        "MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQg\n" + 
        "Q2VydGlmaWNhdGUwHQYDVR0OBBYEFIhN7BYmp3b1JkO8NJnf1ep7+F/eMB8GA1Ud\n" + 
        "IwQYMBaAFEvjIhStChRGt1Ixi6ueWmLzmDeAMA0GCSqGSIb3DQEBBQUAA4GBAFVz\n" + 
        "leZMQPz9UopfgxVJc8rz2F+71vUukOZ/w31NJ9NFxlObquMymUCzqdMUfdXmp3CV\n" + 
        "MG7cjHtI4ZjRZXrrv7BczcLrMV626d9WlbzreXQnW23IVWMJ0/niQLq0oscsy7E6\n" + 
        "wtgMITHuaH6XzpgiLsbP8BoRBMqaBt6YSIWsbG+Y\n" + 
        "-----END CERTIFICATE-----";
    
    public static final String endPrivateKey =
        "-----BEGIN RSA PRIVATE KEY-----\n" + 
        "Proc-Type: 4,ENCRYPTED\n" + 
        "DEK-Info: DES-EDE3-CBC,E20AAB000D1D90B1\n" + 
        "\n" + 
        "cWrCb6eHuwb6/gnbX12Va47qSpFW0j99Lq2eEj0fqLdlwA6+KvD3/U+Nj4ldaAQ4\n" + 
        "rYryQv0MJu/kT9z/mJbBI4NwunX/9vXttyuh8s07sv8AqdHCylYR9miz61Q0LkLR\n" + 
        "9H9D8NWMgMnuVhlj+NUXlkF+Jfriu5xkIqeYDhN8c3/AMawQoNdW/pWmgz0BfFIP\n" + 
        "DUxszfXHx5mfSMoRdC2YZGlFdsONSO7s14Ayz8+pKD0PzSARXtTEJ5+mELCnhFsw\n" + 
        "R7zYYwD+9WjL702bjYQxwRS5Sk1Z/VAxLFfjdtlUFSi6VLGIG+jUnM1RF91KtJY1\n" + 
        "bJOQrlHw9/wyH75y9sXUrVpil4qH9shILHgu4A0VaL7IpIFjWS9vPY7SvwqRlbk7\n" + 
        "QPhxoIpiNzjzjEa7PG6nSqy8mRzJP0OLWzRUoMWJn6ntf+oj7CzaaIgFrrwRGOCQ\n" + 
        "BYibTTMZ/paxKDvZ9Lcl8a6uRvi2II2/F63bPcTcILsKDsBdQp93Evanw1QKXdGi\n" + 
        "jb4b0Y1LYZM0jl7z2TSBZ27HyHKp4jMQP9q9mujEKInjzSB+gsRGfP6++OilrR2U\n" + 
        "Y7kN2o/ufnPHltel0pUWOHr45IyK8zowgXWtKVl9U+VRwr2thGbdqkRGk55KjJK4\n" + 
        "Q+OfwvIKHgvn/4cN/BGIA/52eyY//bTFk6ePGY2vlQK4mvB7MeSxtxoCGxdCYQru\n" + 
        "wI28rOHyQ1cdx141yxlKVSIcxBVZHm8sfh9PHeKMKuaOgc8kfx+Qh8IghFHyJ+yg\n" + 
        "PboNF9/PiM/glaaBzY2OKTYQKY6LiTetZiI6RdLE7Y+SFwG7Wwo5dg==\n" + 
        "-----END RSA PRIVATE KEY-----";
    
    /**
     * a self signed certificate
     */
    public static X509Certificate rootCertificateSS;

    public static X509Certificate endCertificate;

    public static MyCRL crl;

    public static X509CertSelector theCertSelector;

    public static CertPathBuilder builder;
    private static CertStore store;
    
    public static void initCertPathSSCertChain() throws CertificateException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            IOException {
        // create certificates and CRLs
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bi = new ByteArrayInputStream(rootCert.getBytes());
        rootCertificateSS = (X509Certificate) cf.generateCertificate(bi);
        bi = new ByteArrayInputStream(endCert.getBytes());
        endCertificate = (X509Certificate) cf.generateCertificate(bi);

        BigInteger revokedSerialNumber = BigInteger.valueOf(1);
        crl = new MyCRL("X.509");
//        X509CRL rootCRL = X509CRL;
//        X509CRL interCRL = X509CRLExample.createCRL(interCert, interPair
//                .getPrivate(), revokedSerialNumber);

        // create CertStore to support path building
        List<Object> list = new ArrayList<Object>();

        list.add(rootCertificateSS);
        list.add(endCertificate);

        CollectionCertStoreParameters params = new CollectionCertStoreParameters(
                list);
        store = CertStore.getInstance("Collection", params);

        theCertSelector = new X509CertSelector();
        theCertSelector.setCertificate(endCertificate);
        theCertSelector.setIssuer(endCertificate.getIssuerX500Principal()
                .getEncoded());
        
     // build the path
        builder = CertPathBuilder.getInstance("PKIX");

    }
    
    public static CertPathBuilder getCertPathBuilder() {
        if (builder == null) {
            throw new RuntimeException(
            "Call initCertPathSSCertChain prior to buildCertPath");
        }
        return builder;
    }
    
    public static CertPath buildCertPathSSCertChain()
            throws InvalidAlgorithmParameterException {

        CertPathBuilderResult result;
        CertPathParameters buildParams = getCertPathParameters();
        try {
            result = builder.build(buildParams);
        } catch (CertPathBuilderException e) {
            return null;
        }
        return result.getCertPath();
    }

    public static CertPathParameters getCertPathParameters()
            throws InvalidAlgorithmParameterException {
        if ((rootCertificateSS == null) || (theCertSelector == null)
                || (builder == null)) {
            throw new RuntimeException(
                    "Call initCertPathSSCertChain prior to buildCertPath");
        }
        PKIXCertPathBuilderResult result = null;
        PKIXBuilderParameters buildParams = new PKIXBuilderParameters(
                Collections.singleton(new TrustAnchor(rootCertificateSS, null)),
                theCertSelector);

        buildParams.addCertStore(store);
        buildParams.setRevocationEnabled(false);

        return buildParams;

    }
}
