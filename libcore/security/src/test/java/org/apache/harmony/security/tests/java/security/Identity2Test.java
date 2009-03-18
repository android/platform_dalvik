/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.security.tests.java.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Certificate;
import java.security.Identity;
import java.security.IdentityScope;
import java.security.KeyManagementException;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.harmony.security.tests.java.security.IdentityScope2Test.IdentityScopeSubclass;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@SuppressWarnings("deprecation")
@TestTargetClass(value=Identity.class,
        untestedMethods={
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="getGuarantor",
                    args={},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="encode",
                    args={OutputStream.class},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="decode",
                    args={InputStream.class},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="toString",
                    args={boolean.class},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="getFormat",
                    args={},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="getPrincipal",
                    args={},
                    notes="no implementation"
            ),
            @TestTargetNew(
                    level=TestLevel.NOT_NECESSARY,
                    clazz=Certificate.class,
                    method="getPublicKey",
                    args={},
                    notes="no implementation"
            )
})
public class Identity2Test extends junit.framework.TestCase {

    static PublicKey pubKey;
    static {
        try {
            pubKey = KeyPairGenerator.getInstance("DSA").genKeyPair().getPublic();
        } catch (Exception e) {
            fail(e.toString());
        }
    }
     
    public static class CertificateImpl implements java.security.Certificate {

        X509Certificate cert;

        public CertificateImpl(X509Certificate cert) {
            this.cert = cert;
        }

        public Principal getGuarantor() {
            return cert.getIssuerDN();
        }

        public void encode(OutputStream out) {
        }

        public void decode(InputStream in) {
        }

        public String toString() {
            return "";
        }

        public String toString(boolean b) {
            return "";
        }

        public String getFormat() {
            return cert.getType();
        }

        public Principal getPrincipal() {
            return cert.getSubjectDN();
        }

        public PublicKey getPublicKey() {
            return cert.getPublicKey();
        }
    }

    String certificate = "-----BEGIN CERTIFICATE-----\n"
            + "MIICZTCCAdICBQL3AAC2MA0GCSqGSIb3DQEBAgUAMF8xCzAJBgNVBAYTAlVTMSAw\n"
            + "HgYDVQQKExdSU0EgRGF0YSBTZWN1cml0eSwgSW5jLjEuMCwGA1UECxMlU2VjdXJl\n"
            + "IFNlcnZlciBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw05NzAyMjAwMDAwMDBa\n"
            + "Fw05ODAyMjAyMzU5NTlaMIGWMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZv\n"
            + "cm5pYTESMBAGA1UEBxMJUGFsbyBBbHRvMR8wHQYDVQQKExZTdW4gTWljcm9zeXN0\n"
            + "ZW1zLCBJbmMuMSEwHwYDVQQLExhUZXN0IGFuZCBFdmFsdWF0aW9uIE9ubHkxGjAY\n"
            + "BgNVBAMTEWFyZ29uLmVuZy5zdW4uY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n"
            + "iQKBgQCofmdY+PiUWN01FOzEewf+GaG+lFf132UpzATmYJkA4AEA/juW7jSi+LJk\n"
            + "wJKi5GO4RyZoyimAL/5yIWDV6l1KlvxyKslr0REhMBaD/3Z3EsLTTEf5gVrQS6sT\n"
            + "WMoSZAyzB39kFfsB6oUXNtV8+UKKxSxKbxvhQn267PeCz5VX2QIDAQABMA0GCSqG\n"
            + "SIb3DQEBAgUAA34AXl3at6luiV/7I9MN5CXYoPJYI8Bcdc1hBagJvTMcmlqL2uOZ\n"
            + "H9T5hNMEL9Tk6aI7yZPXcw/xI2K6pOR/FrMp0UwJmdxX7ljV6ZtUZf7pY492UqwC\n"
            + "1777XQ9UEZyrKJvF5ntleeO0ayBqLGVKCWzWZX9YsXCpv47FNLZbupE=\n"
            + "-----END CERTIFICATE-----\n";

    ByteArrayInputStream certArray = new ByteArrayInputStream(certificate
            .getBytes());

    String certificate2 = "-----BEGIN CERTIFICATE-----\n"
            + "MIICZzCCAdCgAwIBAgIBGzANBgkqhkiG9w0BAQUFADBhMQswCQYDVQQGEwJVUzEY\n"
            + "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsT\n"
            + "A1BLSTEcMBoGA1UEAxMTRG9EIFBLSSBNZWQgUm9vdCBDQTAeFw05ODA4MDMyMjAy\n"
            + "MjlaFw0wODA4MDQyMjAyMjlaMGExCzAJBgNVBAYTAlVTMRgwFgYDVQQKEw9VLlMu\n"
            + "IEdvdmVybm1lbnQxDDAKBgNVBAsTA0RvRDEMMAoGA1UECxMDUEtJMRwwGgYDVQQD\n"
            + "ExNEb0QgUEtJIE1lZCBSb290IENBMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
            + "gQDbrM/J9FrJSX+zxFUbsI9Vw5QbguVBIa95rwW/0M8+sM0r5gd+DY6iubm6wnXk\n"
            + "CSvbfQlFEDSKr4WYeeGp+d9WlDnQdtDFLdA45tCi5SHjnW+hGAmZnld0rz6wQekF\n"
            + "5xQaa5A6wjhMlLOjbh27zyscrorMJ1O5FBOWnEHcRv6xqQIDAQABoy8wLTAdBgNV\n"
            + "HQ4EFgQUVrmYR6m9701cHQ3r5kXyG7zsCN0wDAYDVR0TBAUwAwEB/zANBgkqhkiG\n"
            + "9w0BAQUFAAOBgQDVX1Y0YqC7vekeZjVxtyuC8Mnxbrz6D109AX07LEIRzNYzwZ0w\n"
            + "MTImSp9sEzWW+3FueBIU7AxGys2O7X0qmN3zgszPfSiocBuQuXIYQctJhKjF5KVc\n"
            + "VGQRYYlt+myhl2vy6yPzEVCjiKwMEb1Spu0irCf+lFW2hsdjvmSQMtZvOw==\n"
            + "-----END CERTIFICATE-----\n";

    ByteArrayInputStream certArray2 = new ByteArrayInputStream(certificate2
            .getBytes());

    
    public static class IdentitySubclass extends Identity {
        private static final long serialVersionUID = 1L;

        public IdentitySubclass() {
            super();
        }

        public IdentitySubclass(String name) {
            super(name);
        }

        public IdentitySubclass(String name, IdentityScope scope)
                throws KeyManagementException {
            super(name, scope);
        }
    }

    /**
     * @tests java.security.Identity#Identity()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Identity",
        args = {}
    )
    public void test_Constructor() {
        new IdentitySubclass();
    }

    /**
     * @tests java.security.Identity#Identity(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Identity",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() {
        String[] str = {"test", "", null};
        IdentitySubclass is;
        for (int i = 0; i < str.length; i++) {
            try {
                is = new IdentitySubclass(str[i]);
                assertNotNull(is);
                assertTrue(is instanceof Identity);
            } catch (Exception e) {
                fail("Unexpected exception for Identity(java.lang.String) with parameter " + str[i]);
            }
        }
    }

    /**
     * @tests java.security.Identity#Identity(java.lang.String,
     *        java.security.IdentityScope)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Identity",
        args = {java.lang.String.class, java.security.IdentityScope.class}
    )
    public void test_ConstructorLjava_lang_StringLjava_security_IdentityScope() {
        String nameNull = null;
        String[] str = {"test", "", "!@#$%^&*()", "identity name"};
        IdentityScopeSubclass iss = new IdentityScopeSubclass("name");
        IdentitySubclass is;
        
        for (int i = 0; i < str.length; i++) {
            try {
                is = new IdentitySubclass(str[i], new IdentityScopeSubclass());
                assertNotNull(is);
                assertTrue(is instanceof Identity);
            } catch (Exception e) {
                System.out.println(e);
                fail("Unexpected exception for parameter " + str[i]);
            }
        }
        
        try {
            is = new IdentitySubclass(nameNull, new IdentityScopeSubclass());
        } catch (NullPointerException npe) {
        } catch (Exception e) {
            fail("Incorrect exception " + e + " was thrown");
        }
        
        try {
            is = new IdentitySubclass("test", iss);
            is = new IdentitySubclass("test", iss);
            fail("KeyManagementException was not thrown");
        } catch (KeyManagementException npe) {
            //expected
        } catch (Exception e) {
            fail("Incorrect exception " + e + " was thrown instead of KeyManagementException");
        }
    }

    /**
     * @tests java.security.Identity#getScope()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getScope",
        args = {}
    )
    public void test_getScope() throws Exception {
               IdentityScope scope = new IdentityScopeSubclass();
               IdentitySubclass sub = new IdentitySubclass("test", scope);
               IdentityScope returnedScope = sub.getScope();
               assertEquals("Wrong Scope returned", scope, returnedScope);
    }

    /**
     * @tests java.security.Identity#getPublicKey()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPublicKey",
        args = {}
    )
    public void test_getPublicKey() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test",
                       new IdentityScopeSubclass());
               sub.setPublicKey(pubKey);
               PublicKey returnedPubKey = sub.getPublicKey();
               assertEquals("Wrong PublicKey returned", pubKey, returnedPubKey);
    }

    /**
     * @tests java.security.Identity#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public void test_getName() throws Exception {
               String name = "test";
               IdentitySubclass sub = new IdentitySubclass(name,
                       new IdentityScopeSubclass());
               assertEquals("Wrong Name returned", name, sub.getName());
    }

    /**
     * @tests java.security.Identity#getInfo()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInfo",
        args = {}
    )
    public void test_getInfo() throws Exception {
               String info = "This is the general information.";
               IdentitySubclass sub = new IdentitySubclass("test",
                       new IdentityScopeSubclass());
               sub.setInfo(info);
               assertEquals("Wrong Info returned", info, sub.getInfo());
    }

    /**
     * @tests java.security.Identity#certificates()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "certificates",
        args = {}
    )
    public void test_certificates() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test",
                       new IdentityScopeSubclass());
               CertificateFactory cf = CertificateFactory.getInstance("X.509");
               X509Certificate cert[] = new X509Certificate[1];
               cert[0] = (X509Certificate) cf.generateCertificate(certArray);
               sub.setPublicKey(cert[0].getPublicKey());
               CertificateImpl certImpl = new CertificateImpl(cert[0]);
               sub.addCertificate(certImpl);
               java.security.Certificate[] certs = sub.certificates();
               assertEquals("Certificate not contained in the identity",
                       certs[0], certImpl);
    }

    /**
     * @tests java.security.Identity#removeCertificate(java.security.Certificate)
     */
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.PARTIAL_COMPLETE,
                method = "addCertificate",
                args = {java.security.Certificate.class}
        ),
        @TestTargetNew(
                level = TestLevel.PARTIAL_COMPLETE,
                method = "removeCertificate",
                args = {java.security.Certificate.class}
        )
    })
    @AndroidOnly("Spec says: Removing unknown certificates throw an exception. "
            + "The RI ignores unknown certificates.")
    public void test_removeCertificateLjava_security_Certificate() throws Exception {
        IdentitySubclass sub = new IdentitySubclass("test",
                new IdentityScopeSubclass());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert[] = new X509Certificate[1];
        cert[0] = (X509Certificate) cf.generateCertificate(certArray);
        sub.setPublicKey(cert[0].getPublicKey());
        CertificateImpl certImpl = new CertificateImpl(cert[0]);
        sub.addCertificate(certImpl);

        try {
            sub.removeCertificate(null);
            fail("Test 1: KeyManagementException expected.");
        } catch (KeyManagementException e) {
            // Expected.
        }
        assertEquals("Test 2: Certificate should not have been removed.", 
                1, sub.certificates().length);

        sub.removeCertificate(certImpl);
        assertEquals("Test 3: Certificate has not been removed.", 
                0, sub.certificates().length);

        // Removing the same certificate a second time should fail.
        try {
            sub.removeCertificate(certImpl);
            fail("Test 4: KeyManagementException expected.");
        } catch (KeyManagementException e) {
            // Expected.
        }

    }

    /**
     * @tests java.security.Identity#equals(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void test_equalsLjava_lang_Object() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test",
                       new IdentityScopeSubclass());
               CertificateFactory cf = CertificateFactory.getInstance("X.509");
               X509Certificate cert[] = new X509Certificate[1];
               cert[0] = (X509Certificate) cf.generateCertificate(certArray);
               sub.setPublicKey(cert[0].getPublicKey());
               CertificateImpl certImpl = new CertificateImpl(cert[0]);
               sub.addCertificate(certImpl);
               IdentitySubclass sub2 = new IdentitySubclass("test",
                       new IdentityScopeSubclass());
               IdentitySubclass sub3 = new IdentitySubclass("identity name",
                       new IdentityScopeSubclass());
               assertEquals("the two Identity objects are not equal", sub2, sub);
               boolean res1 = sub.equals(sub2); //true
               if (!res1)  fail("Method equals() should returned TRUE");
               res1 = sub.equals(sub3); //false
               if (res1)  fail("Method equals() should returned FALSE");
    }

    /**
     * @tests java.security.Identity#identityEquals(java.security.Identity)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Method identityEquals(java.security.Identity) is not tested",
        method = "identityEquals",
        args = {java.security.Identity.class}
    )
    public void test_identityEqualsLjava_security_Identity() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test", null);
               CertificateFactory cf = CertificateFactory.getInstance("X.509");
               X509Certificate cert[] = new X509Certificate[1];
               cert[0] = (X509Certificate) cf.generateCertificate(certArray);
               sub.setPublicKey(cert[0].getPublicKey());
               CertificateImpl certImpl = new CertificateImpl(cert[0]);
               sub.addCertificate(certImpl);
               IdentitySubclass sub2 = new IdentitySubclass("test", null);
               sub2.setPublicKey(cert[0].getPublicKey());
               assertEquals("the two Identity objects are not identity-equal",
                       sub2, sub);
    }

    /**
     * @tests java.security.Identity#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test", null);
               assertNotNull(sub.toString());
               assertTrue("The String returned is not valid", sub.toString()
                       .length() > 0);
               // Regression for HARMONY-1566
               assertNotNull(new IdentitySubclass().toString());
    }

    /**
     * @tests java.security.Identity#toString(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {boolean.class}
    )
    public void test_toStringZ() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test", null);
               assertNotNull(sub.toString(true));
               assertTrue("The String returned is not valid", sub.toString(true)
                       .length() > 0);
    }

    /**
     * @tests java.security.Identity#hashCode()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void test_hashCode() throws Exception {
               IdentitySubclass sub = new IdentitySubclass("test", null);
               IdentitySubclass sub2 = new IdentitySubclass("test", null);
               assertEquals("The 2 hash codes are not equal", sub.hashCode(), sub2
                       .hashCode());
    }
    
    /**
     * @tests java.security.Identity#setInfo(String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setInfo",
        args = {java.lang.String.class}
    )
    public void testSetInfo() throws Exception{
        String[] info = {"This is the general information.", "test", "", null};
        IdentitySubclass sub = new IdentitySubclass("test", new IdentityScopeSubclass());

        for (int i = 0; i < info.length; i++) {
            try {
                sub.setInfo(info[i]);
                assertEquals("Wrong Info returned", info[i], sub.getInfo());
            } catch (Exception e) {
                fail("Unexpected exception for parameter " + info[i]);
            }
        }
           
    }
    
    /**
     * @tests java.security.Identity#setPublicKey(PublicKey key)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "SecurityException is not checked",
        method = "setPublicKey",
        args = {java.security.PublicKey.class}
    )
    public void testSetPublicKey() throws Exception{
        IdentitySubclass sub = new IdentitySubclass("test",
                   new IdentityScopeSubclass());
           sub.setPublicKey(pubKey);
           PublicKey returnedPubKey = sub.getPublicKey();
           assertEquals("Wrong PublicKey returned", pubKey, returnedPubKey);
           
           sub.setPublicKey(null);
           assertEquals("Wrong PublicKey returned", null, sub.getPublicKey());
    }

}
