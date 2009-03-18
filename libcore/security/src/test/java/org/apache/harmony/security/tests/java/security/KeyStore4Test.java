package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.MyProvider;
import org.apache.harmony.security.tests.support.TestKeyStoreSpi;
import org.apache.harmony.security.tests.support.cert.MyCertificate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@TestTargetClass(KeyStore.class)
public class KeyStore4Test extends TestCase {

    Provider provider = new MyProvider();
    KeyStore keyStore;
    KeyStore uninitialized;
    KeyStore failing;
    
    public static final String KEY_STORE_TYPE = "TestKeyStore";
    
    protected void setUp() throws Exception{
        super.setUp();
        
        Security.addProvider(new MyProvider());
        
        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            keyStore.load(null, "PASSWORD".toCharArray());
        } catch (KeyStoreException e) {
            fail("test class not available");
        }
        
        try {
            uninitialized = KeyStore.getInstance(KEY_STORE_TYPE);
        } catch (KeyStoreException e) {
            fail("test keystore not available");
        }
        
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        Security.removeProvider(provider.getName());
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getInstance",
            args={String.class}
    )
    public void testGetInstanceString() {
        try {
            KeyStore ks = KeyStore.getInstance("TestKeyStore");
            assertNotNull("keystore is null", ks);
            assertEquals("KeyStore is not of expected Type", "TestKeyStore", ks.getType());
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            KeyStore.getInstance("UnknownKeyStore");
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        }
        
        try {
            KeyStore.getInstance(null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // ok
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getInstance",
            args={String.class, String.class}
    )
    public void testGetInstanceStringString() {
        try {
            KeyStore ks = KeyStore.getInstance("TestKeyStore", provider.getName());
            assertNotNull("keystore is null", ks);
            assertEquals("KeyStore is not of expected type", "TestKeyStore", ks.getType());
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchProviderException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            KeyStore.getInstance("UnknownKeyStore", provider.getName());
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        } catch (NoSuchProviderException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            KeyStore.getInstance("TestKeyStore", (String)null);
            fail("expected IllegalArgumentException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchProviderException e) {
            fail("unexpected exception: " + e);
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            KeyStore.getInstance("TestKeyStore", "");
            fail("expected IllegalArgumentException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchProviderException e) {
            fail("unexpected exception: " + e);
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        try {
            KeyStore.getInstance(null, provider.getName());
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        } catch (NoSuchProviderException e) {
            fail("unexpected exception: " + e);
        } catch (NullPointerException e) {
            // also ok            
        }
        
        try {
            KeyStore.getInstance("TestKeyStore", "UnknownProvider");
            fail("expected NoSuchProviderException");
        } catch (NoSuchProviderException e) {
            // ok
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getInstance",
            args={String.class, Provider.class}
    )
    public void testGetInstanceStringProvider() {
        try {
            KeyStore ks = KeyStore.getInstance("TestKeyStore", provider);
            assertNotNull("KeyStore is null", ks);
            assertEquals("KeyStore is not of expected type", "TestKeyStore", ks.getType());
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            KeyStore.getInstance("UnknownKeyStore", provider);
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok;
        }
        
        try {
            KeyStore.getInstance("TestKeyStore", (Provider)null);
            fail("expected IllegalArgumentException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (IllegalArgumentException e) {
            // ok
        }
        
        try {
            KeyStore.getInstance(null, provider);
            fail("expected NullPointerException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NullPointerException e) {
            // ok
        }
    }


    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getKey",
            args={String.class, char[].class}
    )
    public void testGetKey() {
        try {
            Key key = keyStore.getKey("keyalias", null);
            assertNotNull(key);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableKeyException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.getKey("certalias", null);
            fail("expected NoSuchAlgorithmException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (UnrecoverableKeyException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            uninitialized.getKey("keyalias", null);
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableKeyException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.getKey("unknownalias", null);
            fail("expected NoSuchAlgorithmException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (UnrecoverableKeyException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.getKey("unknownalias", "PASSWORD".toCharArray());
            fail("expected UnrecoverableKeyException");
        } catch (UnrecoverableKeyException e) {
            // ok
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        }
        
    }


    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getCertificateAlias",
            args={Certificate.class}
    )
    public void testGetCertificateAlias() {
        try {
            String alias = keyStore.getCertificateAlias(TestKeyStoreSpi.CERT);
            assertNotNull("alias is null", alias);
            assertEquals("alias is not expected", "certalias", alias);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            uninitialized.getCertificateAlias(TestKeyStoreSpi.CERT);
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        }
        
        try {
            keyStore.getCertificateAlias(null);
            fail("expected NullPointerException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NullPointerException e) {
            // ok
        }
        
        try {
            String certificateAlias = keyStore.getCertificateAlias(new MyCertificate("dummy", null));
            assertNull("alias was not null", certificateAlias);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="store",
            args={OutputStream.class, char[].class}
    )
    public void testStoreOutputStreamCharArray() {
        OutputStream os = new ByteArrayOutputStream();
        char[] password = "PASSWORD".toCharArray();
        
        try {
            keyStore.store(os, password);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.store(os, null);
            fail("expected NoSuchAlgorithmException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            // ok
        }
        
        try {
            keyStore.store(os, "".toCharArray());
            fail("expected CertificateException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            // ok
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.store(null, null);
            fail("expected IOException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            // ok
        }
        
        try {
            uninitialized.store(null, null);
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        

        
        
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="store",
            args={KeyStore.LoadStoreParameter.class}
    )
    public void testStoreLoadStoreParameter() {
        try {
            keyStore.store(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return new KeyStore.PasswordProtection("PASSWORD".toCharArray());
                }});
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.store(null);
            fail("expected IOException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            // ok
        }
        
        try {
            keyStore.store(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return null;
                }});
            fail("expected UnsupportedOperationException");
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        try {
            keyStore.store(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return new KeyStore.PasswordProtection("".toCharArray());
                }});
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            // ok
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.store(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return new KeyStore.PasswordProtection(null);
                }} );
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            uninitialized.store(null);
            fail("expected KeyStoreException");
        } catch (KeyStoreException e) {
            // ok
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="load",
            args={InputStream.class, char[].class}
    )
    public void testLoadInputStreamCharArray() {
        InputStream is = new ByteArrayInputStream("DATA".getBytes());
        char[] password = "PASSWORD".toCharArray();
        try {
            keyStore.load(is, password);
            assertTrue(keyStore.containsAlias("keyalias"));
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.load(new ByteArrayInputStream("".getBytes()), password);
            fail("expected IOException");
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            // ok
        }
        
        try {
            keyStore.load(is, null);
            fail("expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.load(is, new char[] {});
            fail("expected CertificateException");
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            // ok
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="load",
            args={KeyStore.LoadStoreParameter.class}
    )
    public void testLoadLoadStoreParameter() {
        try {
            keyStore.load(null);
            fail("expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.load(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return new KeyStore.PasswordProtection("PASSWORD".toCharArray());
                }
                
            });
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.load(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return null;
                }
                
            });
            fail("expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (CertificateException e) {
            fail("unexpected exception: " + e);
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }

        try {
            keyStore.load(new KeyStore.LoadStoreParameter() {

                public ProtectionParameter getProtectionParameter() {
                    return new KeyStore.ProtectionParameter() {};
                }
                
            });
            fail("expected CertificateException");
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (CertificateException e) {
            // ok
        } catch (IOException e) {
            fail("unexpected exception: " + e);
        }
    }

    @TestTargetNew(
            level=TestLevel.SUFFICIENT,
            method="getEntry",
            args={String.class, KeyStore.ProtectionParameter.class}
    )
    public void testGetEntry() {
        try {
            Entry entry = keyStore.getEntry("certalias", null);
            assertNotNull("entry is null", entry);
            assertTrue("entry is not cert entry", entry instanceof KeyStore.TrustedCertificateEntry);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableEntryException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            Entry entry = keyStore.getEntry("certalias", new KeyStore.ProtectionParameter() {});
            assertNotNull(entry);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableEntryException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (UnsupportedOperationException e) {
            // ok
        }
        
        try {
            Entry entry = keyStore.getEntry("keyalias", new KeyStore.PasswordProtection(new char[] {} ));
            assertNotNull(entry);
            assertTrue(entry instanceof KeyStore.SecretKeyEntry);
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableEntryException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        }
        
        try {
            keyStore.getEntry("unknownalias", new KeyStore.PasswordProtection(new char[] {}));
            fail("expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
            // ok
        } catch (UnrecoverableEntryException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (UnsupportedOperationException e) {
            // also ok
        }
        
        try {
            keyStore.getEntry(null, new KeyStore.ProtectionParameter() {});
            fail("expected NullPointerException");
        } catch (NoSuchAlgorithmException e) {
            fail("unexpected exception: " + e);
        } catch (UnrecoverableEntryException e) {
            fail("unexpected exception: " + e);
        } catch (KeyStoreException e) {
            fail("unexpected exception: " + e);
        } catch (NullPointerException e) {
            // ok
        }
    }

    

    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getType"
    )
    public void testGetType() {
        assertEquals(KEY_STORE_TYPE, keyStore.getType());
    }
    
    @TestTargetNew(
            level=TestLevel.COMPLETE,
            method="getProvider"
    )
    public void testGetProvider() {
        assertNotNull(keyStore.getProvider());
        assertEquals("not equal", provider, keyStore.getProvider());
    }

}
