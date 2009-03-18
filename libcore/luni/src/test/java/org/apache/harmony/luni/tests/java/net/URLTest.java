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

package org.apache.harmony.luni.tests.java.net;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import tests.support.Support_Configuration;
import tests.support.Support_PortManager;
import tests.support.resource.Support_Resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TestTargetClass(URL.class) 
public class URLTest extends TestCase {
   
    private static final String helloWorldString = "Hello World";
    /**
     * @tests java.net.URL#getHost()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Regression test.",
        method = "getHost",
        args = {}
    )
    public void test_getHost() throws MalformedURLException {
        // Regression for HARMONY-60
        String ipv6Host = "FEDC:BA98:7654:3210:FEDC:BA98:7654:3210";
        URL url = new URL("http", ipv6Host, -1, "myfile");
        assertEquals(("[" + ipv6Host + "]"), url.getHost());
    }

    /**
     * @tests java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Regression test.",
        method = "URL",
        args = {java.lang.String.class, java.lang.String.class, int.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_StringLjava_lang_StringILjava_lang_String()
            throws MalformedURLException {
        // Regression for HARMONY-83
        new URL("http", "apache.org", 123456789, "file");
        try {
            new URL("http", "apache.org", -123, "file");
            fail("Assert 0: Negative port should throw exception");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URL#URL(java.lang.String, java.lang.String,
     *        java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URL",
        args = {java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_StringLjava_lang_StringLjava_lang_String() throws MalformedURLException {
        // Strange behavior in reference, the hostname contains a ':' so it gets wrapped in '[', ']'
        URL testURL = new URL("http", "www.apache.org:8082", "test.html#anch");
        assertEquals("Assert 0: wrong protocol", "http", testURL.getProtocol());
        assertEquals("Assert 1: wrong host", "[www.apache.org:8082]", testURL.getHost());
        assertEquals("Assert 2: wrong port", -1, testURL.getPort());
        assertEquals("Assert 3: wrong file", "test.html", testURL.getFile());
        assertEquals("Assert 4: wrong anchor", "anch", testURL.getRef());
        
        try {
            new URL("hftp", "apache.org:8082", "test.html#anch");
            fail("Assert 0: Invalid protocol");
        } catch (MalformedURLException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URL#URL(String, String, String)
     *
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Regression test.",
        method = "URL",
        args = {java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void test_java_protocol_handler_pkgs_prop() throws MalformedURLException {
        // Regression test for Harmony-3094
        final String HANDLER_PKGS = "java.protocol.handler.pkgs";
        String pkgs = System.getProperty(HANDLER_PKGS);
        System.setProperty(HANDLER_PKGS, "fake|org.apache.harmony.luni.tests.java.net");

        try {
            new URL("test_protocol", "", "fake.jar");
        } catch (MalformedURLException e) {
            // expected
        } finally {
            if (pkgs == null) {
                System.clearProperty(HANDLER_PKGS);
            } else {
                System.setProperty(HANDLER_PKGS, pkgs);
            }
        }
    }
    
    /**
     * Test method for {@link java.net.URL#hashCode()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() throws MalformedURLException {
        URL testURL1 = new URL("http", "www.apache.org:8080", "test.html#anch");
        URL testURL2 = new URL("http", "www.apache.org:8080", "test.html#anch");
        URL changedURL = new URL("http", "www.apache.org:8082",
                "test.html#anch");
        assertEquals("Assert 0: error in hashCode: not same",
                testURL1.hashCode(), testURL2.hashCode());
        assertFalse("Assert 0: error in hashCode: should be same", testURL1
                .hashCode() == changedURL.hashCode());
    }

    /**
     * Test method for {@link java.net.URL#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory)}.
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "cannot test since no sophisticated StreamHandlerFactory available.",
        method = "setURLStreamHandlerFactory",
        args = {java.net.URLStreamHandlerFactory.class}
    )
    public void testSetURLStreamHandlerFactory() throws MalformedURLException, IOException, IllegalArgumentException, IllegalAccessException {
        URLStreamHandlerFactory factory = new MyURLStreamHandlerFactory();
        Field streamHandlerFactoryField = null;
        int counter = 0;
        File sampleFile = createTempHelloWorldFile();
        
        URL fileURL = sampleFile.toURL();


        Field[] fields = URL.class.getDeclaredFields();


        for (Field f : fields) {
            if (URLStreamHandlerFactory.class.equals(f.getType())) {
                counter++;
                streamHandlerFactoryField = f;
            }
        }

        if (counter != 1) {
            fail("Error in test setup: not Factory found");
        }

        streamHandlerFactoryField.setAccessible(true);
        URLStreamHandlerFactory old = (URLStreamHandlerFactory) streamHandlerFactoryField.get(null);

        try {
            streamHandlerFactoryField.set(null, factory);
            BufferedReader buf = new BufferedReader(new InputStreamReader(
                    fileURL.openStream()), helloWorldString.getBytes().length);
            String nextline;
            while ((nextline = buf.readLine()) != null) {
                assertEquals(helloWorldString, nextline);
            }
            
            buf.close();
            
        } finally {

            streamHandlerFactoryField.set(null, old);
        }
        
    }

    /**
     * Test method for {@link java.net.URL#URL(java.lang.String)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URL",
        args = {java.lang.String.class}
    )
    public void testURLString() throws MalformedURLException {
        URL testURL = new URL("ftp://myname@host.dom/etc/motd");

        assertEquals("Assert 0: wrong protocol", "ftp", testURL.getProtocol());
        assertEquals("Assert 1: wrong host", "host.dom", testURL.getHost());
        assertEquals("Assert 2: wrong port", -1, testURL.getPort());
        assertEquals("Assert 3: wrong userInfo", "myname", testURL
                .getUserInfo());
        assertEquals("Assert 4: wrong path", "/etc/motd", testURL.getPath());

        try {
            new URL("ftpmyname@host.dom/etc/motd");
            fail("Assert 0: malformed URL should throw exception");
        } catch (MalformedURLException e) {
            // expected
        }

    }

    /**
     * Test method for {@link java.net.URL#URL(java.net.URL, java.lang.String)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URL",
        args = {java.net.URL.class, java.lang.String.class}
    )
    public void testURLURLString() throws MalformedURLException {

        URL gamelan = new URL("http://www.gamelan.com/pages/");

        URL gamelanNetwork = new URL(gamelan, "Gamelan.net.html");
        URL gamelanNetworkBottom = new URL(gamelanNetwork, "#BOTTOM");
        assertEquals("Assert 0: wrong anchor", "BOTTOM", gamelanNetworkBottom
                .getRef());
        assertEquals("Assert 1: wrong protocol", "http", gamelanNetworkBottom
                .getProtocol());
        
        // same protocol
        URL gamelanNetworBottom2 = new URL(gamelanNetwork,
                "http://www.gamelan.com/pages/Gamelan.net.html#BOTTOM");
        assertEquals(gamelanNetwork.getProtocol(), gamelanNetworBottom2.getProtocol());
        
        // changed protocol
        URL gamelanNetworkBottom3 = new URL(gamelanNetwork,
                "ftp://www.gamelan2.com/pages/Gamelan.net.html#BOTTOM");
        URL absoluteNew = new URL(
                "ftp://www.gamelan2.com/pages/Gamelan.net.html#BOTTOM");
        assertEquals("Host of context URL instead of new URL", "ftp",
                gamelanNetworkBottom3.getProtocol());
        assertTrue("URL is context URL instead of new URL",
                gamelanNetworkBottom3.sameFile(absoluteNew));
        
        // exception testing
        try {
            u = null;
            u1 = new URL(u, "somefile.java");
            fail("didn't throw the expected MalFormedURLException");
        } catch (MalformedURLException e) {
            // ok
        }
        
        // Non existing protocol
     // exception testing
        try {
        u  =  new URL(gamelanNetwork,
                    "someFancyNewProt://www.gamelan2.com/pages/Gamelan.net.html#BOTTOM");
        assertTrue("someFancyNewProt".equalsIgnoreCase(u.getProtocol()));
        } catch (MalformedURLException e) {
            // ok
        }
    }

    /**
     * Test method for {@link java.net.URL#set(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)}.
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "final class protected method",
        method = "set",
        args = {java.lang.String.class, java.lang.String.class, int.class, java.lang.String.class, java.lang.String.class}
    )
    public void testSetStringStringIntStringString() {
        
    }

    /**
     * Test method for {@link java.net.URL#equals(java.lang.Object)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject() throws MalformedURLException {
        URL testURL1 = new URL("http", "www.apache.org:8080", "test.html");
        URL wrongProto = new URL("ftp", "www.apache.org:8080", "test.html");
        URL wrongPort = new URL("http", "www.apache.org:8082", "test.html");
        URL wrongHost = new URL("http", "www.apache2.org:8080", "test.html");
        URL wrongRef = new URL("http", "www.apache.org:8080",
                "test2.html#BOTTOM");
        URL testURL2 = new URL("http://www.apache.org:8080/test.html");
        
        
        assertFalse("Assert 0: error in equals: not same", testURL1
                .equals(wrongProto));
        assertFalse("Assert 1: error in equals: not same", testURL1
                .equals(wrongPort));
        assertFalse("Assert 2: error in equals: not same", testURL1
                .equals(wrongHost));
        assertFalse("Assert 3: error in equals: not same", testURL1
                .equals(wrongRef));
        
        assertFalse("Assert 4: error in equals: not same", testURL1
                .equals(testURL2));
        
        URL testURL3 = new URL("http", "www.apache.org", "/test.html");
        URL testURL4 = new URL("http://www.apache.org/test.html");
        assertTrue("Assert 4: error in equals: same", testURL3
                .equals(testURL4));
    }

    /**
     * Test method for {@link java.net.URL#sameFile(java.net.URL)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "non trivial reference test fails",
        method = "sameFile",
        args = {java.net.URL.class}
    )
    public void testSameFile() throws MalformedURLException {
        URL gamelan = new URL("file:///pages/index.html");
        URL gamelanFalse = new URL("file:///pages/out/index.html");

        URL gamelanNetwork = new URL(gamelan, "#BOTTOM");
        assertTrue(gamelanNetwork.sameFile(gamelan));
        
        assertFalse(gamelanNetwork.sameFile(gamelanFalse));
        
        // non trivial test
        URL url = new URL("http://web2.javasoft.com/some+file.html");
        URL url1 = new URL("http://web2.javasoft.com/some%20file.html");

        assertFalse(url.sameFile(url1));
    }

    /**
     * Test method for {@link java.net.URL#getContent()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "image and sound content not testable: mime types",
        method = "getContent",
        args = {}
    )
    public void testGetContent() throws MalformedURLException {
        
        File sampleFile = createTempHelloWorldFile();

        // read content from file
        URL fileURL = sampleFile.toURL();
        
        try {
            InputStream output = (InputStream) fileURL.getContent();
            assertTrue(output.available() > 0);
            // ok
        } catch (Exception e) {
            fail("Did not get output type from File URL");
        }
        
        //Exception test
        URL invalidFile = new URL("file:///nonexistenttestdir/tstfile");
        
        try {
            invalidFile.getContent();
            fail("Access to invalid file worked");
        } catch (IOException e) {
            //ok
        }
        
    }

    /**
     * Test method for {@link java.net.URL#openStream()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "openStream",
        args = {}
    )
    public void testOpenStream() throws MalformedURLException, IOException {
      
        File sampleFile = createTempHelloWorldFile();

        // read content from file
        URL fileURL = sampleFile.toURL();
        BufferedReader dis = null;
        String inputLine;
        StringBuffer buf = new StringBuffer(32);
        try {
            dis = new BufferedReader(
                    new InputStreamReader(fileURL.openStream()), 32);
            while ((inputLine = dis.readLine()) != null) {
                buf.append(inputLine);
            }
            dis.close();
        } catch (IOException e) {
            fail("Unexpected error in test setup: " + e.getMessage());
        }

        assertTrue("Assert 0: Nothing was read from file ", buf.length() > 0);
        assertEquals("Assert 1: Wrong stream content", "Hello World", buf
                .toString());
        
        // exception test
        
        URL invalidFile = new URL("file:///nonexistenttestdir/tstfile");
        
        try {
            dis = new BufferedReader(
                    new InputStreamReader(invalidFile.openStream()), 32);
            while ((inputLine = dis.readLine()) != null) {
                buf.append(inputLine);
            }
            fail("Access to invalid file worked");
        } catch (Exception e) {
            //ok
        } finally {
            dis.close();
        }
    }

    /**
     * Test method for {@link java.net.URL#openConnection()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "openConnection",
        args = {}
    )
    public void testOpenConnection() throws MalformedURLException, IOException {
        
        File sampleFile = createTempHelloWorldFile();
        
        byte[] ba;
        InputStream is;
        String s;
        u = sampleFile.toURL();
        u.openConnection();

        is = (InputStream) u.getContent(new Class[] { Object.class });
        is.read(ba = new byte[4096]);
        s = new String(ba);
        assertTrue("Incorrect content " + u
                + " does not contain: \"Hello World\"",
                s.indexOf("Hello World") >= 0);
        
        try {
            URL u = new URL("https://a.xy.com/index.html");
            URLConnection conn = u.openConnection();
            conn.connect();
            fail("Should not be able to read from this site.");
        } catch (IOException e) {
            //ok
        } 
        
        System.setSecurityManager(new MockSecurityManager());
        try {
            URL u = new URL("http://127.0.0.1");
            URLConnection conn = u.openConnection();
            conn.connect();
            fail("Should not be able to read from this site.");
        } catch (SecurityException e) {
            //ok
        } finally {
            System.setSecurityManager(null);
        }
          
    }

    /**
     * Test method for {@link java.net.URL#toURI()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toURI",
        args = {}
    )
    public void testToURI() throws MalformedURLException, URISyntaxException {
        String testHTTPURLString = "http://www.gamelan.com/pages/";
        String testFTPURLString = "ftp://myname@host.dom/etc/motd";
        URL testHTTPURL = new URL(testHTTPURLString);
        URL testFTPURL = new URL(testFTPURLString);
        
        URI testHTTPURI = testHTTPURL.toURI();
        URI testFTPURI = testFTPURL.toURI();
        assertEquals(testHTTPURI.toString(),testHTTPURLString);
        assertEquals(testFTPURI.toString(),testFTPURLString);
        
        //Exception test
        String[] constructorTestsInvalid = new String[] {
                "http:///a path#frag", // space char in path, not in escaped
                // octet form, with no host
                "http://host/a[path#frag", // an illegal char, not in escaped
                // octet form, should throw an
                // exception
                "http://host/a%path#frag", // invalid escape sequence in path
                "http://host/a%#frag", // incomplete escape sequence in path

                "http://host#a frag", // space char in fragment, not in
                // escaped octet form, no path
                "http://host/a#fr#ag", // illegal char in fragment
                "http:///path#fr%ag", // invalid escape sequence in fragment,
                // with no host
                "http://host/path#frag%", // incomplete escape sequence in
                // fragment

                "http://host/path?a query#frag", // space char in query, not
                // in escaped octet form
                "http://host?query%ag", // invalid escape sequence in query, no
                // path
                "http:///path?query%", // incomplete escape sequence in query,
                // with no host

                "mailto:user^name@fklkf.com" // invalid char in scheme
        };
       
        for (String malformedURI : Arrays.asList(constructorTestsInvalid)) {
            try {
                URL urlQuery = new URL("http://host/a%path#frag");
                urlQuery.toURI();
                fail("Exception expected");
            } catch (URISyntaxException e) {
                // ok
            }
        }
    }

    /**
     * Test method for {@link java.net.URL#openConnection(java.net.Proxy)}.
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "See ExcludedProxyTest.",
        method = "openConnection",
        args = {java.net.Proxy.class}
    )
    @BrokenTest("the host address isn't working anymore")
    public void testOpenConnectionProxy() throws IOException {
        
        // create Proxy
        
        System.setProperty("http.proxyHost",
                Support_Configuration.ProxyServerTestHost);
        System.setProperty("http.proxyPort", "80");

        URL u2 = new URL("http://"
                + Support_Configuration.ProxyServerTestHost
                + "/cgi-bin/test.pl");

        SocketAddress addr1 = new InetSocketAddress(Support_Configuration.ProxyServerTestHost, 80);
        Proxy proxy1 = new Proxy(Proxy.Type.HTTP, addr1);
        
        // create test input   
        String posted = "just a test";
        
        // perform test
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u2
                .openConnection(proxy1);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(3000);
      
        OutputStream out = conn.getOutputStream();
        out.write(posted.getBytes());
        out.close();
        
        
        /*
        InputStream in = conn.getErrorStream();
        if (in != null ){
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 200);
        String line;
        while((line = r.readLine())!= null) {
            System.err.println(line);
        }
        }
        */
        
        conn.getResponseCode();
        InputStream is = conn.getInputStream();
        String response = "";
        byte[] b = new byte[1024];
        int count = 0;
        while ((count = is.read(b)) > 0) {
            response += new String(b, 0, count);
        }
        assertTrue("Response to POST method invalid", response
                .equals(posted));
        
        // Exception test
        URL httpUrl = new URL("http://abc.com");
        URL jarUrl = new URL("jar:"
                 + Support_Resources.getResourceURL("/JUC/lf.jar!/plus.bmp"));
        URL ftpUrl = new URL("ftp://" + Support_Configuration.FTPTestAddress
                + "/nettest.txt");
        URL fileUrl = new URL("file://abc");
        URL[] urlList = { httpUrl, jarUrl, ftpUrl, fileUrl };
        for (int i = 0; i < urlList.length; ++i) {
            try {
                urlList[i].openConnection(null);
            } catch (IllegalArgumentException iae) {
                // expected
            }
        }
        // should not throw exception too
        fileUrl.openConnection(Proxy.NO_PROXY);

    }

    /**
     * Test method for {@link java.net.URL#toString()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void testToString() throws MalformedURLException {
        URL testHTTPURL = new URL("http://www.gamelan.com/pages/");
        URL testFTPURL = new URL("ftp://myname@host.dom/etc/motd");
        
        assertEquals(testHTTPURL.toString(),testHTTPURL.toExternalForm());
        assertEquals(testFTPURL.toString(),testFTPURL.toExternalForm());
        
        assertEquals("http://www.gamelan.com/pages/", testHTTPURL.toString());
    }

    /**
     * Test method for {@link java.net.URL#toExternalForm()}.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "simple tests",
        method = "toExternalForm",
        args = {}
    )
    public void testToExternalForm() throws MalformedURLException {
        URL testHTTPURL = new URL("http://www.gamelan.com/pages/");
        URL testFTPURL = new URL("ftp://myname@host.dom/etc/motd");
        
        assertEquals(testHTTPURL.toString(),testHTTPURL.toExternalForm());
        assertEquals(testFTPURL.toString(),testFTPURL.toExternalForm());
        
        assertEquals("http://www.gamelan.com/pages/", testHTTPURL.toExternalForm());
    }

    /**
     * Test method for {@link java.net.URL#getFile()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getFile",
        args = {}
    )
    public void testGetFile() throws MalformedURLException {
       
        File sampleFile = createTempHelloWorldFile();
        
        // read content from file
        URL fileURL = sampleFile.toURL();
        assertNotNull(fileURL);
        assertEquals(sampleFile.getPath(),fileURL.getFile());
    }

    /**
     * Test method for {@link java.net.URL#getPort()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPort",
        args = {}
    )
    public void testGetPort() throws MalformedURLException {
        URL testHTTPURL = new URL("http://www.gamelan.com/pages/");
        URL testFTPURL = new URL("ftp://myname@host.dom/etc/motd");
        
        assertEquals(-1,testFTPURL.getPort());
        assertEquals(-1,testHTTPURL.getPort());
        
        URL testHTTPURL8082 = new URL("http://www.gamelan.com:8082/pages/");
        assertEquals(8082, testHTTPURL8082.getPort());
        
    }

    /**
     * Test method for {@link java.net.URL#getProtocol()}.
     */
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "getProtocol",
      args = {}
    )
    public void testGetProtocol() throws MalformedURLException {
        URL testHTTPURL = new URL("http://www.gamelan.com/pages/");
        URL testHTTPSURL = new URL("https://www.gamelan.com/pages/");
        URL testFTPURL = new URL("ftp://myname@host.dom/etc/motd");
        URL testFile = new URL("file:///pages/index.html");
        URL testJarURL = new URL("jar:file:///bar.jar!/foo.jar!/Bugs/HelloWorld.class");
        
        assertTrue("http".equalsIgnoreCase(testHTTPURL.getProtocol()));
        assertTrue("https".equalsIgnoreCase(testHTTPSURL.getProtocol()));
        assertTrue("ftp".equalsIgnoreCase(testFTPURL.getProtocol()));
        assertTrue("file".equalsIgnoreCase(testFile.getProtocol()));
        assertTrue("jar".equalsIgnoreCase(testJarURL.getProtocol()));
         
    }

    /**
     * Test method for {@link java.net.URL#getRef()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getRef",
        args = {}
    )
    public void testGetRef() throws MalformedURLException {
        URL gamelan = new URL("http://www.gamelan.com/pages/");
        
        String output = gamelan.getRef();
        assertTrue(output == null || output.equals(""));

        URL gamelanNetwork = new URL(gamelan, "Gamelan.net.html#BOTTOM");
        assertEquals("BOTTOM", gamelanNetwork.getRef());

        URL gamelanNetwork2 = new URL("http", "www.gamelan.com",
                "Gamelan.network.html#BOTTOM");

        assertEquals("BOTTOM", gamelanNetwork2.getRef());

    }

    /**
     * Test method for {@link java.net.URL#getQuery()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getQuery",
        args = {}
    )
    public void testGetQuery() throws MalformedURLException {
        URL urlQuery = new URL(
                "http://www.example.com/index.html?attrib1=value1&attrib2=value&attrib3#anchor");
        URL urlNoQuery = new URL(
        "http://www.example.com/index.html#anchor");
        
        assertEquals("attrib1=value1&attrib2=value&attrib3", urlQuery.getQuery());
        
        String output = urlNoQuery.getQuery();
        assertTrue(output == null || "".equals(output));
    }

    /**
     * Test method for {@link java.net.URL#getPath()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPath",
        args = {}
    )
    public void testGetPath() throws MalformedURLException {
        URL url = new URL("http://www.example.com");
        String output = url.getPath();
        
        assertTrue("".equals(output) || output == null);
        
        URL url2 = new URL(url,"/foo/index.html");
        assertEquals("/foo/index.html",url2.getPath());
    }

    /**
     * Test method for {@link java.net.URL#getUserInfo()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getUserInfo",
        args = {}
    )
    public void testGetUserInfo() throws MalformedURLException {
        URL urlNoUserInfo = new URL("http://www.java2s.com:8080");
        URL url = new URL("ftp://myUser:password@host.dom/etc/motd");
        
        assertEquals("Assert 0: Wrong user","myUser:password",url.getUserInfo());
        String userInfo = urlNoUserInfo.getUserInfo();
        assertTrue("".equals(userInfo) || null == userInfo);
    }

    /**
     * Test method for {@link java.net.URL#getAuthority()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAuthority",
        args = {}
    )
    public void testGetAuthority() throws MalformedURLException, URISyntaxException {
        // legal authority information userInfo (user,password),domain,port

        URL url = new URL("http://www.java2s.com:8080");
        assertEquals("Assert 0: Wrong authority ", "www.java2s.com:8080", url
                .getAuthority());
        
        URL ftpURL = new URL("ftp://myname@host.dom/etc/motd");
        assertEquals("Assert 1: Wrong authority ", "myname@host.dom", ftpURL
                .getAuthority());
        
        URI testURI = new URI("/relative/URI/with/absolute/path/to/resource.txt");
        String output = testURI.getAuthority();
        assertTrue("".equals(output) || null == output);
    }

    /**
     * Test method for {@link java.net.URL#set(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "final class protected method",
        method = "set",
        args = {java.lang.String.class, java.lang.String.class, int.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testSetStringStringIntStringStringStringStringString() {
        
    }

    /**
     * Test method for {@link java.net.URL#getDefaultPort()}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDefaultPort",
        args = {}
    )
    public void testGetDefaultPort() throws MalformedURLException {
        URL testHTTPURL = new URL("http://www.gamelan.com/pages/");
        URL testFTPURL = new URL("ftp://myname@host.dom/etc/motd");
        
        assertEquals(21,testFTPURL.getDefaultPort());
        assertEquals(80,testHTTPURL.getDefaultPort());
    }
    
    private File createTempHelloWorldFile() {
        // create content to read
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File sampleFile = null;
        try {
            if (tmpDir.isDirectory()) {
                sampleFile = File.createTempFile("openStreamTest", ".txt",
                        tmpDir);
                sampleFile.deleteOnExit();
            } else {
                fail("Error in test setup java.io.tmpdir does not exist");
            }

            FileWriter fstream = new FileWriter(sampleFile);
            BufferedWriter out = new BufferedWriter(fstream, 32);
            out.write(helloWorldString);
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            fail("Error: in test setup" + e.getMessage());
        }
        
        return sampleFile;
    }
    
    // start HARMONY branch
    
    public static class MyHandler extends URLStreamHandler {
        protected URLConnection openConnection(URL u)
                throws IOException {
            return null;
        }
    }

    URL u;

    URL u1;

    URL u2;

    boolean caught = false;

    static boolean isSelectCalled;


    
    static class MockProxySelector extends ProxySelector {

        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            System.out.println("connection failed");
        }

        public List<Proxy> select(URI uri) {
            isSelectCalled = true;
            ArrayList<Proxy> proxyList = new ArrayList<Proxy>(1);
            proxyList.add(Proxy.NO_PROXY);
            return proxyList;
        }
    }

    static class MockSecurityManager extends SecurityManager {

        public void checkConnect(String host, int port) {
            if ("127.0.0.1".equals(host)) {
                throw new SecurityException("permission is not allowed");
            }
        }

        public void checkPermission(Permission permission) {
            if ("setSecurityManager".equals(permission.getName())) {
                return;
            }
            super.checkPermission(permission);
        }

    }

    
    static class MyURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL arg0) throws IOException {
            try {
                URLConnection con = arg0.openConnection();
                con.setDoInput(true);
                con.connect();
                return con;
            } catch (Throwable e) {
                return null;
            }
        }

        public void parse(URL url, String spec, int start, int end) {
            parseURL(url, spec, start, end);
        }
    }

    static class MyURLStreamHandlerFactory implements URLStreamHandlerFactory {

        public static MyURLStreamHandler handler = new MyURLStreamHandler();

        public URLStreamHandler createURLStreamHandler(String arg0) {
            handler = new MyURLStreamHandler();
            return handler;
        }

    }
    
    /**
     * URLStreamHandler implementation class necessary for tests.
     */
    private class TestURLStreamHandler extends URLStreamHandler {
        public URLConnection openConnection(URL arg0) throws IOException {
            try {
                URLConnection con = arg0.openConnection();
                con.setDoInput(true);
                con.connect();
                return con;
            } catch (Throwable e) {
                return null;
            }
        }

        public URLConnection openConnection(URL arg0, Proxy proxy)
                throws IOException {
            return super.openConnection(u, proxy);
        }
    }
    
    /**
     * Test method for {@link java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String, java.net.URLStreamHandler)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch. No meaningful MalformedURLException foundwhich doesn't end as a NullPointerException.",
        method = "URL",
        args = {java.lang.String.class, java.lang.String.class, int.class, java.lang.String.class, java.net.URLStreamHandler.class}
    )
    public void test_ConstructorLjava_lang_StringLjava_lang_StringILjava_lang_StringLjava_net_URLStreamHandler()
            throws Exception {
        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo", null);
        assertEquals("SSISH1 returns a wrong protocol", "http", u.getProtocol());
        assertEquals("SSISH1 returns a wrong host", "www.yahoo.com", u
                .getHost());
        assertEquals("SSISH1 returns a wrong port", 8080, u.getPort());
        assertEquals("SSISH1 returns a wrong file", "test.html", u.getFile());
        assertTrue("SSISH1 returns a wrong anchor: " + u.getRef(), u.getRef()
                .equals("foo"));

        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo",
                new MyHandler());
        assertEquals("SSISH2 returns a wrong protocol", "http", u.getProtocol());
        assertEquals("SSISH2 returns a wrong host", "www.yahoo.com", u
                .getHost());
        assertEquals("SSISH2 returns a wrong port", 8080, u.getPort());
        assertEquals("SSISH2 returns a wrong file", "test.html", u.getFile());
        assertTrue("SSISH2 returns a wrong anchor: " + u.getRef(), u.getRef()
                .equals("foo"));

        byte[] ba;
        InputStream is;
        String s;
        
        // Cannot make this test fail if no exception is thrown: Debatable
        /*
        try {
            u = new URL("http", "www.yahoo.com", 8080, "test.html#foo",
                    null);
            fail("No error occurred");
        } catch (MalformedURLException e) {
            // ok
        } catch (NullPointerException e) {
            // ok
        }
        */

        TestURLStreamHandler lh = new TestURLStreamHandler();
        
        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo",
                lh);
        

        try {
            new URL(null, "1", 0, "file", lh);
            fail("Exception expected, but nothing was thrown!");
        } catch (MalformedURLException e) {
            // ok
        } catch (NullPointerException e) {
            // Expected NullPointerException
        }

    }
    
    /**
     * Test method for {@link java.net.URL#getContent(java.lang.Class[])}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "throws unexpected exception: NullPointerException in first execution",
        method = "getContent",
        args = {java.lang.Class[].class}
    )
    public void test_getContent_LJavaLangClass() throws Exception {

        File sampleFile = createTempHelloWorldFile();

        byte[] ba;
        String s;

        InputStream is = null;

        try {
            u = new URL("file:///data/tmp/hyts_htmltest.html");
            is = (InputStream) u.getContent(new Class[] {InputStream.class});
            is.read(ba = new byte[4096]);
            fail("No error occurred reading from nonexisting file");
        } catch (IOException e) {
            // ok
        }

        try {
            u = new URL("file:///data/tmp/hyts_htmltest.html");
            is = (InputStream) u.getContent(new Class[] {
                    String.class, InputStream.class});
            is.read(ba = new byte[4096]);
            fail("No error occurred reading from nonexisting file");
        } catch (IOException e) {
            // ok
        }

        // Check for null
        u = sampleFile.toURL();
        u.openConnection();
        assertNotNull(u);

        s = (String) u.getContent(new Class[] {String.class});
        assertNull(s);

    }
    
    /**
     * Test method for {@link java.net.URL#URL(java.net.URL, java.lang.String, java.net.URLStreamHandler)}.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch",
        method = "URL",
        args = {java.net.URL.class, java.lang.String.class, java.net.URLStreamHandler.class}
    )
    public void testURLURLStringURLStreamHandler() throws MalformedURLException {
        u = new URL("http://www.yahoo.com");
        // basic ones
        u1 = new URL(u, "file.java", new MyHandler());
        assertEquals("1 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("1 returns a wrong host", "www.yahoo.com", u1.getHost());
        assertEquals("1 returns a wrong port", -1, u1.getPort());
        assertEquals("1 returns a wrong file", "/file.java", u1.getFile());
        assertNull("1 returns a wrong anchor", u1.getRef());

        u1 = new URL(u, "systemresource:/+/FILE0/test.java", new MyHandler());
        assertEquals("2 returns a wrong protocol", "systemresource", u1
                .getProtocol());
        assertTrue("2 returns a wrong host", u1.getHost().equals(""));
        assertEquals("2 returns a wrong port", -1, u1.getPort());
        assertEquals("2 returns a wrong file", "/+/FILE0/test.java", u1
                .getFile());
        assertNull("2 returns a wrong anchor", u1.getRef());

        u1 = new URL(u, "dir1/dir2/../file.java", null);
        assertEquals("3 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("3 returns a wrong host", "www.yahoo.com", u1.getHost());
        assertEquals("3 returns a wrong port", -1, u1.getPort());
        assertEquals("3 returns a wrong file", "/dir1/dir2/../file.java", u1
                .getFile());
        assertNull("3 returns a wrong anchor", u1.getRef());

        // test for question mark processing
        u = new URL("http://www.foo.com/d0/d1/d2/cgi-bin?foo=bar/baz");

        // test for relative file and out of bound "/../" processing
        u1 = new URL(u, "../dir1/dir2/../file.java", new MyHandler());
        assertTrue("A) returns a wrong file: " + u1.getFile(), u1.getFile()
                .equals("/d0/d1/dir1/file.java"));

        // test for absolute and relative file processing
        u1 = new URL(u, "/../dir1/dir2/../file.java", null);
        assertEquals("B) returns a wrong file", "/../dir1/dir2/../file.java",
                u1.getFile());

        URL one;
        try {
            one = new URL("http://www.ibm.com");
        } catch (MalformedURLException ex) {
            // Should not happen.
            throw new RuntimeException(ex.getMessage());
        }
        try {
            new URL(one, (String) null, null);
            fail("Specifying null spec on URL constructor should throw MalformedURLException");
        } catch (MalformedURLException e) {
            // expected
        }

    }
    
    /**
     * Test method for {@link java.net.URL#toExternalForm()}.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "From harmony branch",
        method = "toExternalForm",
        args = {}
    )
    public void test_toExternalForm_Relative() throws MalformedURLException {
        String strURL = "http://a/b/c/d;p?q";
        String ref = "?y";
        URL url = new URL(new URL(strURL), ref);
        assertEquals("http://a/b/c/?y", url.toExternalForm());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "From harmony branch",
        method = "toExternalForm",
        args = {}
    )
    public void test_toExternalForm_Absolute() throws MalformedURLException {
        String strURL = "http://localhost?name=value";
        URL url = new URL(strURL);
        assertEquals(strURL, url.toExternalForm());

        strURL = "http://localhost?name=value/age=12";
        url = new URL(strURL);
        assertEquals(strURL, url.toExternalForm());
    }
    

    ServerSocket ss;

    Socket s;

    Thread t;

    
    
    protected void startServer(String name, int port) {
        try {
            ss = new ServerSocket(port, 5);
        } catch (IOException e) {
            fail(name + ": " + e);
        }
    }
    
    class SServer extends Thread implements Runnable {
        Socket s1 = null;

        public void run() {
            try {
                ss.setSoTimeout(5000);
                s1 = ss.accept();
                ss.close();
                Thread.sleep(4000);
            } catch (java.io.InterruptedIOException x) {
                System.out.println(Thread.currentThread()
                        + ", accept() timeout fired: " + x);
            } catch (InterruptedException x) {
            } catch (Exception e) {
                System.out.println("Unable to accept: " + e.toString());
            } finally {
                try {
                    if (s1 != null)
                        s1.close();
                } catch (java.io.IOException e) {
                }
            }
        }
    }

   

}
