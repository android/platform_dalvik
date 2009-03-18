/* Licensed to the Apache Software Foundation (ASF) under one or more
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
package tests.api.java.nio.charset;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.AndroidOnly;

import junit.framework.TestCase;

import tests.api.java.nio.charset.CharsetTest.MockCharset;
import tests.api.java.nio.charset.CharsetTest.MockSecurityManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Vector;

@TestTargetClass(CharsetProvider.class)
/**
 * Test charset providers managed by Charset.
 */
public class CharsetProviderTest extends TestCase {

    // need to be modified, e.g., read from system property
    static String PROP_CONFIG_FILE1 = "clear.tests.cp1";

    static String CONFIG_FILE1 = null;


    static MockCharset charset1 = new MockCharset("mockCharset00",
            new String[] { "mockCharset01", "mockCharset02" });

    static MockCharset charset2 = new MockCharset("mockCharset10",
            new String[] { "mockCharset11", "mockCharset12" });

    @Override
    protected void setUp() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null) {
            fail("java.io.tmpdir not set");
        }

        File tmpdir = new File(tmpDir);
        if (!tmpdir.isDirectory()) {
            fail("java.io.tmpdir is not a directory");
        }
        
        String sep = System.getProperty("file.separator");

        if (!tmpDir.endsWith(sep)) {
            tmpDir += sep;
        }

        CONFIG_FILE1 = tmpDir +  "META-INF" + sep + "services" + sep
                + "java.nio.charset.spi.CharsetProvider";
        
        URL url = null;
        try {
            url = new URL("file://" + tmpDir);
        } catch (MalformedURLException e) {
            fail("unexpected exception: " + e);
        }
        URLClassLoader urlc = new URLClassLoader(new URL[] { url });
        
        Thread.currentThread().setContextClassLoader(urlc);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Thread.currentThread().setContextClassLoader(null);
    }
    
    /*
     * Write the string to the config file.
     */
    private void setupFile(String path, String content) throws Exception {
        String sep = System.getProperty("file.separator");
        int sepIndex = path.lastIndexOf(sep);
        File f = new File(path.substring(0, sepIndex));
        f.mkdirs();
        
        FileOutputStream fos = new FileOutputStream(path);
        OutputStreamWriter writer = new OutputStreamWriter(fos);// , "UTF-8");
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    /*
     * Write the string to the config file.
     */
    private void cleanupFile(String path) throws Exception {
        File f = new File(path);
        f.delete();
    }

    /*
     * Test the method isSupported(String) with charset supported by some
     * providers (multiple).
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")
    public void testIsSupported_And_ForName_NormalProvider() throws Exception {
        try {
            assertFalse(Charset.isSupported("mockCharset10"));
            assertFalse(Charset.isSupported("mockCharset11"));
            assertFalse(Charset.isSupported("mockCharset12"));
            try {
                Charset.forName("mockCharset10");
                fail("Should throw UnsupportedCharsetException!");
            } catch (UnsupportedCharsetException e) {
                // expected
            }
            try {
                Charset.forName("mockCharset11");
                fail("Should throw UnsupportedCharsetException!");
            } catch (UnsupportedCharsetException e) {
                // expected
            }
            try {
                Charset.forName("mockCharset12");
                fail("Should throw UnsupportedCharsetException!");
            } catch (UnsupportedCharsetException e) {
                // expected
            }

            StringBuffer sb = new StringBuffer();
            sb.append("#comment\r");
            sb.append("\n");
            sb.append("\r\n");
            sb.append(" \ttests.api.java.nio.charset."
                    + "CharsetTest$MockCharsetProvider \t\n\r");
            sb.append(" \ttests.api.java.nio.charset."
                    + "CharsetTest$MockCharsetProvider \t");
            setupFile(CONFIG_FILE1, sb.toString());

            sb = new StringBuffer();
            sb.append(" #comment\r");
            sb.append("\n");
            sb.append("\r\n");
            sb.append(" \ttests.api.java.nio.charset."
                    + "CharsetProviderTest$MockCharsetProvider \t\n\r");
            setupFile(CONFIG_FILE1, sb.toString());

            assertTrue(Charset.isSupported("mockCharset10"));
            // ignore case problem in mock, intended
            assertTrue(Charset.isSupported("MockCharset11"));
            assertTrue(Charset.isSupported("MockCharset12"));
            assertTrue(Charset.isSupported("MOCKCharset10"));
            // intended case problem in mock
            assertTrue(Charset.isSupported("MOCKCharset11"));
            assertTrue(Charset.isSupported("MOCKCharset12"));

            assertTrue(Charset.forName("mockCharset10") instanceof MockCharset);
            assertTrue(Charset.forName("mockCharset11") instanceof MockCharset);
            assertTrue(Charset.forName("mockCharset12") instanceof MockCharset);

            assertTrue(Charset.forName("mockCharset10") == charset2);
            // intended case problem in mock
            Charset.forName("mockCharset11");
            assertTrue(Charset.forName("mockCharset12") == charset2);
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method isSupported(String) when the configuration file contains
     * a non-existing class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    public void testIsSupported_NonExistingClass() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("impossible\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.isSupported("impossible");
            fail("Should throw Error!");
        } catch (Error e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method isSupported(String) when the configuration file contains
     * a non-CharsetProvider class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")
    public void testIsSupported_NotCharsetProviderClass() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("java.lang.String\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.isSupported("impossible");
            fail("Should throw ClassCastException!");
        } catch (ClassCastException e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method isSupported(String) with insufficient privilege to use
     * charset provider.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    public void testIsSupported_InsufficientPrivilege() throws Exception {
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            Charset.isSupported("UTF-8");

            try {
                StringBuffer sb = new StringBuffer();
                sb.append("tests.api.java.nio.charset."
                        + "CharsetProviderTest$MockCharsetProvider\r");
                setupFile(CONFIG_FILE1, sb.toString());

                assertFalse(Charset.isSupported("gb180300000"));
            } catch (SecurityException e) {
                fail("unexpected SecurityException!:" + e);
            } finally {
                cleanupFile(CONFIG_FILE1);
            }
        } finally {
            System.setSecurityManager(oldMan);
        }
    }
    
    /*
     * Test the method isSupported(String) with insufficient privilege to use
     * charset provider.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    public void testForName_InsufficientPrivilege() throws Exception {
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            Charset.forName("UTF-8");

            try {
                StringBuffer sb = new StringBuffer();
                sb.append("tests.api.java.nio.charset."
                        + "CharsetProviderTest$MockCharsetProvider\r");
                setupFile(CONFIG_FILE1, sb.toString());

                Charset.forName("gb180300000");
                fail("expected UnsupportedCharsetException!");
            } catch (SecurityException e) {
                fail("unexpected SecurityException!:" + e);
            } catch (UnsupportedCharsetException e) {
                // ok
            } finally {
                cleanupFile(CONFIG_FILE1);
            }
        } finally {
            System.setSecurityManager(oldMan);
        }
    }    

    /*
     * Test the method forName(String) when the charset provider supports a
     * built-in charset.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "charsets",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "charsetForName",
            args = {String.class}
        )
    }) 
    public void testForName_DuplicateWithBuiltInCharset() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("tests.api.java.nio.charset." +
                    "CharsetProviderTest$MockCharsetProviderACSII\r");
            setupFile(CONFIG_FILE1, sb.toString());

            assertFalse(Charset.forName("us-ascii") instanceof MockCharset);
            Charset charset = Charset.availableCharsets().get("us-ascii");
            assertFalse(charset instanceof MockCharset);
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method forName(String) when the configuration file contains a
     * non-existing class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")    
    public void testForName_NonExistingClass() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("impossible\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.forName("impossible");
            fail("Should throw Error!");
        } catch (Error e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method forName(String) when the configuration file contains a
     * non-CharsetProvider class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsetForName",
        args = {String.class}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")    
    public void testForName_NotCharsetProviderClass() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("java.lang.String\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.forName("impossible");
            fail("Should throw ClassCastException!");
        } catch (ClassCastException e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method availableCharsets() with charset supported by some
     * providers (multiple).
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsets",
        args = {}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")    
    public void testAvailableCharsets_NormalProvider() throws Exception {
        try {
            assertFalse(Charset.availableCharsets()
                    .containsKey("mockCharset10"));
            assertFalse(Charset.availableCharsets()
                    .containsKey("mockCharset11"));
            assertFalse(Charset.availableCharsets()
                    .containsKey("mockCharset12"));

            StringBuffer sb = new StringBuffer();
            sb.append("#comment\r");
            sb.append("\n");
            sb.append("\r\n");
            sb.append("\ttests.api.java.nio.charset."
                    + "CharsetTest$MockCharsetProvider \t\n\r");
            sb.append("\ttests.api.java.nio.charset."
                    + "CharsetTest$MockCharsetProvider \t");
            sb.append("#comment\r");
            sb.append("\n");
            sb.append("\r\n");
            sb.append(" \ttests.api.java.nio.charset."
                    + "CharsetProviderTest$MockCharsetProvider \t\n\r");
            setupFile(CONFIG_FILE1, sb.toString());

            SortedMap<String, Charset> availableCharsets = Charset
                    .availableCharsets();
            assertTrue(availableCharsets.containsKey("mockCharset00"));
            assertTrue(availableCharsets.containsKey("MOCKCharset00"));
            Charset charset = availableCharsets.get("mockCharset00");
            assertTrue(charset instanceof MockCharset);

            charset = availableCharsets.get("MOCKCharset00");
            assertTrue(charset instanceof MockCharset);

            assertFalse(availableCharsets.containsKey("mockCharset01"));
            assertFalse(availableCharsets.containsKey("mockCharset02"));

            charset = availableCharsets.get("mockCharset10");
            assertTrue(charset == charset2);

            charset = availableCharsets.get("MOCKCharset10");
            assertTrue(charset == charset2);

            assertFalse(availableCharsets.containsKey("mockCharset11"));
            assertFalse(availableCharsets.containsKey("mockCharset12"));

            assertTrue(availableCharsets.containsKey("mockCharset10"));
            assertTrue(availableCharsets.containsKey("MOCKCharset10"));

            charset = availableCharsets.get("mockCharset10");
            assertTrue(charset == charset2);

            assertFalse(availableCharsets.containsKey("mockCharset11"));
            assertFalse(availableCharsets.containsKey("mockCharset12"));
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method availableCharsets(String) when the configuration file
     * contains a non-existing class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsets",
        args = {}
    )
    public void testAvailableCharsets_NonExistingClass() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("impossible\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.availableCharsets();
            fail("Should throw Error!");
        } catch (Error e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method availableCharsets(String) when the configuration file
     * contains a non-CharsetProvider class name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsets",
        args = {}
    )
    @AndroidOnly("Looks like RI doesn't use current thread's context class "+
    "loader to lookup charset providers")    
    public void testAvailableCharsets_NotCharsetProviderClass()
            throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("java.lang.String\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.availableCharsets();
            fail("Should throw ClassCastException!");
        } catch (ClassCastException e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }

    /*
     * Test the method availableCharsets(String) when the configuration file
     * contains an illegal string.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "charsets",
        args = {}
    )
    public void testAvailableCharsets_IllegalString() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("java String\r");
            setupFile(CONFIG_FILE1, sb.toString());

            Charset.availableCharsets();
            fail("Should throw Error!");
        } catch (Error e) {
            // expected
        } finally {
            cleanupFile(CONFIG_FILE1);
        }
    }
    
    /*
     * Mock charset provider.
     */
    public static class MockCharsetProvider extends CharsetProvider {

        public Charset charsetForName(String charsetName) {
            if ("MockCharset10".equalsIgnoreCase(charsetName)
                    || "MockCharset11".equalsIgnoreCase(charsetName)
                    || "MockCharset12".equalsIgnoreCase(charsetName)) {
                return charset2;
            }
            return null;
        }

        public Iterator<Charset> charsets() {
            Vector<Charset> v = new Vector<Charset>();
            v.add(charset2);
            return v.iterator();
        }
    }
    
    /*
     * Another mock charset provider providing build-in charset "ascii".
     */
    public static class MockCharsetProviderACSII extends CharsetProvider {

        public Charset charsetForName(String charsetName) {
            if ("US-ASCII".equalsIgnoreCase(charsetName)
                    || "ASCII".equalsIgnoreCase(charsetName)) {
                return new MockCharset("US-ASCII", new String[] { "ASCII" });
            }
            return null;
        }

        public Iterator<Charset> charsets() {
            Vector<Charset> v = new Vector<Charset>();
            v.add(new MockCharset("US-ASCII", new String[] { "ASCII" }));
            return v.iterator();
        }
    }

}
