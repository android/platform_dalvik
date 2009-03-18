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
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import tests.support.Support_Configuration;
import tests.support.resource.Support_Resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.UnknownServiceException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@TestTargetClass(
   value = URLConnection.class,
   untestedMethods = {
       @TestTargetNew(
           level = TestLevel.NOT_NECESSARY,
           notes = "Default implementation returns always null according to spec.",
           method = "getHeaderField",
           args = {int.class}
       ),
       @TestTargetNew(
           level = TestLevel.NOT_NECESSARY,
           notes = "Default implementation returns always null according to spec.",
           method = "getHeaderFieldKey",
           args = {int.class}
       )
   }
) 
public class URLConnectionTest extends TestCase {
    
    private static final String testString = "Hello World";
    
    private URLConnection fileURLCon;
    
    private URL fileURL;
    
    private JarURLConnection jarURLCon;
    
    private URL jarURL;
    
    private URLConnection gifURLCon;
    
    private URL gifURL;

    public boolean isGetCalled;

    public boolean isPutCalled;
    
    private Map<String, List<String>> mockHeaderMap;

    private InputStream mockIs = new MockInputStream();

    public boolean isCacheWriteCalled;

    public boolean isAbortCalled;
    
    /**
     * @tests {@link java.net.URLConnection#addRequestProperty(String, String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Exceptions checked only. Cannot test positive test since getter method is not supported.",
        method = "addRequestProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_addRequestProperty() throws MalformedURLException,
            IOException {
        
        
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));

        try {
            // Regression for HARMONY-604
            u.addRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        
        u.connect();
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.addRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        
    }

    /**
     * @tests {@link java.net.URLConnection#setRequestProperty(String, String)}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Exceptions checked only -> only partially implemented.",
        method = "setRequestProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_setRequestProperty() throws MalformedURLException,
            IOException {

        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        try {
            u.setRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            u.setRequestProperty("user-agent", "Mozilla/5.0 (Windows; U; Windows NT 5.0; de-DE; rv:1.7.5) Gecko/20041122 Firefox/1.0");
        } catch (NullPointerException e) {
            fail("Unexpected Exception");
        }
        
        u.connect();
        
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.setRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.net.URLConnection#setUseCaches(boolean)}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Complete together with getUseCaches test.",
        method = "setUseCaches",
        args = {boolean.class}
    )
    public void test_setUseCachesZ() throws MalformedURLException, IOException {

        // Regression for HARMONY-71
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setUseCaches(true);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.net.URLConnection#setAllowUserInteraction(boolean)}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Exceptions checked only.",
        method = "setAllowUserInteraction",
        args = {boolean.class}
    )
    public void test_setAllowUserInteractionZ() throws MalformedURLException,
            IOException {

        // Regression for HARMONY-72
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setAllowUserInteraction(false);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }
    
    static class MockURLConnection extends URLConnection {

        public MockURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
            connected = true;
        }
    }
    
    static class NewHandler extends URLStreamHandler {
        protected URLConnection openConnection(URL u) throws IOException {
            return new HttpURLConnection(u) {
                @Override
                public void connect() throws IOException {
                    connected = true;
                }

                @Override
                public void disconnect() {
                    // do nothing
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }
            };
        }
    }
    
    class MockCachedResponseCache extends ResponseCache {

        public CacheResponse get(URI arg0, String arg1, Map arg2)
                throws IOException {
            if (null == arg0 || null == arg1 || null == arg2) {
                throw new NullPointerException();
            }
            isGetCalled = true;
            return new MockCacheResponse();
        }

        public CacheRequest put(URI arg0, URLConnection arg1)
                throws IOException {
            if (null == arg0 || null == arg1) {
                throw new NullPointerException();
            }
            isPutCalled = true;
            return new MockCacheRequest();
        }
    }
    
    class MockNonCachedResponseCache extends ResponseCache {

        public CacheResponse get(URI arg0, String arg1, Map arg2)
                throws IOException {
            isGetCalled = true;
            return null;
        }

        public CacheRequest put(URI arg0, URLConnection arg1)
                throws IOException {
            isPutCalled = true;
            return new MockCacheRequest();
        }
    }
    
    class MockCacheRequest extends CacheRequest {

        public OutputStream getBody() throws IOException {
            isCacheWriteCalled = true;
            return new MockOutputStream();
        }

        public void abort() {
            isAbortCalled = true;
        }

    }
    
    class MockInputStream extends InputStream {

        public int read() throws IOException {
            return 4711;
        }

        public int read(byte[] arg0, int arg1, int arg2) throws IOException {
            return 1;
        }

        public int read(byte[] arg0) throws IOException {
            return 1;
        }

    }
    
    class MockOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            isCacheWriteCalled = true;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            isCacheWriteCalled = true;
        }

        public void write(byte[] b) throws IOException {
            isCacheWriteCalled = true;
        }
    }
    
    class MockCacheResponse extends CacheResponse {

        public Map<String, List<String>> getHeaders() throws IOException {
            return mockHeaderMap;
        }

        public InputStream getBody() throws IOException {
            return mockIs;
        }
    }
    

    private static int port;

    static String getContentType(String fileName) throws IOException {
        String resourceName = "org/apache/harmony/luni/tests/" + fileName;
        URL url = ClassLoader.getSystemClassLoader().getResource(resourceName);
        assertNotNull("Cannot find test resource " + resourceName, url);
        return url.openConnection().getContentType();
    }

    URL url;
    
    URL url2;

    URLConnection uc;

    URLConnection uc2;
    
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
//        ftpURL = new URL(Support_Configuration.testFTPURL);
       
        
        url = new URL(Support_Configuration.hTTPURLgoogle);
        uc = url.openConnection();
        url2 =  new URL(Support_Configuration.hTTPURLyahoo);
        uc2 = url2.openConnection();
        
        fileURL = createTempHelloWorldFile();
        fileURLCon = fileURL.openConnection();
        
        jarURLCon = openJarURLConnection();
        jarURL = jarURLCon.getURL();
        
        gifURLCon = openGifURLConnection();
        gifURL = gifURLCon.getURL();
        
        port = 80;
        
    }

    @Override
    public void tearDown()throws Exception {
        super.tearDown();
        ((HttpURLConnection) uc).disconnect();
        ((HttpURLConnection) uc2).disconnect();
//        if (((FtpURLConnection) ftpURLCon).getInputStream() !=  null) {
//        ((FtpURLConnection) ftpURLCon).getInputStream().close();
//        }
    }

    /**
     * @throws URISyntaxException 
     * @throws ClassNotFoundException 
     * @tests {@link java.net.URLConnection#addRequestProperty(java.lang.String,java.lang.String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "addRequestProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_addRequestPropertyLjava_lang_StringLjava_lang_String()
            throws IOException, ClassNotFoundException, URISyntaxException {
        uc.setRequestProperty("prop", "yo");
        uc.setRequestProperty("prop", "yo2");
        assertEquals("yo2", uc.getRequestProperty("prop"));
        Map<String, List<String>> map = uc.getRequestProperties();
        List<String> props = uc.getRequestProperties().get("prop");
        assertEquals(1, props.size());

        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail("could modify map");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        try {
            // the list should be unmodifiable
            props.add("hi");
            fail("could modify list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        JarURLConnection con1 = openJarURLConnection();
        map = con1.getRequestProperties();
        assertNotNull(map);
        assertEquals(0, map.size());
        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getAllowUserInteraction()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "getAllowUserInteraction",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.SUFFICIENT,
            notes = "From harmony branch.",
            method = "setAllowUserInteraction",
            args = {boolean.class}
        )
    })
    public void test_getAllowUserInteraction() throws IOException {
        uc.setAllowUserInteraction(false);
        assertFalse("getAllowUserInteraction should have returned false", uc
                .getAllowUserInteraction());

        uc.setAllowUserInteraction(true);
        assertTrue("getAllowUserInteraction should have returned true", uc
                .getAllowUserInteraction());
        
        uc.connect();
        
        try {
            uc.setAllowUserInteraction(false);
            fail("Exception expected");
        } catch (IllegalStateException e) {
            //ok
        }
        
        // test if setAllowUserInteraction works
        URL serverURL = new URL("http://onearth.jpl.nasa.gov/landsat.cgi");
        
        // connect to server
        URLConnection uc2 = serverURL.openConnection();
        HttpURLConnection conn = (HttpURLConnection) uc2;
        uc2.setAllowUserInteraction(true);
  
        uc2.setDoInput(true);
        uc2.setDoOutput(true);
        
        // get reference to stream to post to
        OutputStream os = uc2.getOutputStream();
        
        InputStream in = uc2.getInputStream();
        
        
        int contentLength = uc2.getContentLength();
        String contentType = uc2.getContentType();
        int numBytesRead = 0;
        int allBytesRead = 0;

        byte[] buffer = new byte[4096];
        
        do {
            
        numBytesRead = in.read(buffer);
        allBytesRead += allBytesRead + numBytesRead;
        
        } while (numBytesRead > 0);
        
        assertTrue(allBytesRead > 0);
       
        uc2.connect();
        
        numBytesRead = in.read(buffer);
        
        assertEquals(-1, numBytesRead);
    }
    
    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#connect()}
     */
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "connect",
      args = {}
    )
    public void test_connect() throws IOException {
       
        uc.connect();
        ((HttpURLConnection) uc).disconnect();
        uc.connect();
        
        try {
            uc.setDoOutput(false);
        } catch (Exception e) {
            // ok
        }
    }

    /**
     * @tests {@link java.net.URLConnection#getContent()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getContent",
        args = {}
    )
    public void test_getContent() throws IOException {
        byte[] ba = new byte[testString.getBytes().length];
        String buf = null;
        
        Object obj = fileURLCon.getContent();
        if (obj instanceof String) { 
            buf = (String) obj;
            assertTrue("Incorrect content returned from fileURL: "+buf,
                    testString.equals(buf.trim()));
        } else if (obj instanceof InputStream) {
            InputStream i = (InputStream) obj;
            BufferedReader r = new BufferedReader(new InputStreamReader(i),testString.getBytes().length);
            buf = r.readLine();
            assertTrue("Incorrect content returned from fileURL: "+buf,
                    testString.equals(buf.trim()));
        } else {
            fail("Some unkown type is returned "+obj.toString());
        }
        
        //Exception test
        URL url = new URL("http://a/b/c/?y");
        URLConnection fakeCon = url.openConnection();
        try {
        fakeCon.getContent();
        } catch (IOException e) {
            //ok
        }
        
        ((HttpURLConnection) uc).disconnect();
        try {
            uc.getContent();
        } catch (IOException e) {
            //ok
        }
        
    }

    /**
     * @tests {@link java.net.URLConnection#getContent(Class[])}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getContent",
        args = {java.lang.Class[].class}
    )
    public void test_getContent_LjavalangClass() throws IOException {
        byte[] ba = new byte[600];
        
        fileURLCon.setDoInput(true);
        fileURLCon.connect();
        
        InputStream  helloWorld2 = (InputStream) fileURLCon.getContent(new Class[] {InputStream.class});
        assertNotNull(helloWorld2);
        BufferedReader r = new BufferedReader(new InputStreamReader(helloWorld2),testString.getBytes().length);
        assertTrue("Incorrect content returned from fileURL",
                testString.equals(r.readLine().trim()));
        
        String test = (String) fileURLCon.getContent(new Class[] {String.class} );
        assertNull(test);
        
        //Exception test
        ((HttpURLConnection) uc).disconnect();
        try {
            uc.getContent();
        } catch (IOException e) {
            //ok
        }

        try {
            ((InputStream) fileURLCon.getContent(null)).read(ba, 0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            ((InputStream) fileURLCon.getContent(new Class[] {})).read(ba, 0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            ((InputStream) fileURLCon.getContent(new Class[] { Class.class })).read(ba,
                    0, 600);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getContentEncoding()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getContentEncoding",
        args = {}
    )
    public void test_getContentEncoding() throws IOException {
        // faulty setup
        try {
        
        fileURLCon.getContentEncoding();
        fail("Exception expected");
        } catch (Throwable e) {
            //ok
        }
        
        // positive case
        
        URL url = new URL("http://www.amazon.com/");
        
        URLConnection con = url.openConnection();
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.connect();
        
        assertEquals(con.getContentEncoding(), "gzip");
        
        
        uc2.setRequestProperty("Accept-Encoding", "bla");
        uc2.connect();
        
        assertNull(uc2.getContentEncoding());
        
    }

    /**
     * @tests {@link java.net.URLConnection#getContentLength()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getContentLength",
        args = {}
    )
    public void test_getContentLength() {
        assertEquals(testString.getBytes().length, fileURLCon.getContentLength());
        assertEquals("getContentLength failed: " + uc.getContentLength(), -1,
                uc.getContentLength());
        
        assertEquals(-1, uc2.getContentLength());
        
        assertNotNull(jarURLCon.getContentLength());
        assertNotNull(gifURLCon.getContentLength());
    }

    /**
     * @tests {@link java.net.URLConnection#getContentType()}
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "only default encoding may be tested",
        method = "getContentType",
        args = {}
    )
    public void test_getContentType() throws IOException, MalformedURLException {
        
        assertTrue("getContentType failed: " + fileURLCon.getContentType(), fileURLCon
                .getContentType().contains("text/plain"));
        
        URLConnection htmlFileCon = openHTMLFile();
        String contentType = htmlFileCon.getContentType();
        if (contentType != null) {
            assertTrue(contentType.equalsIgnoreCase("text/html"));
            }
        
        
        /*
        contentType = uc.getContentType();
        if (contentType != null) {
        assertTrue(contentType.equalsIgnoreCase("text/html"));
        }

        contentType = gifURLCon.getContentType();
        if (contentType != null) {
        assertTrue(contentType.equalsIgnoreCase("image/gif"));
        }
        */
        
    }

    /**
     * @tests {@link java.net.URLConnection#getDate()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getDate",
        args = {}
    )
    public void test_getDate() {
        // should be greater than 930000000000L which represents the past
        if (uc.getDate() == 0) {
            System.out
                    .println("WARNING: server does not support 'Date', in test_getDate");
        } else {
            assertTrue("getDate gave wrong date: " + uc.getDate(),
                    uc.getDate() > 930000000000L);
        }
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getDefaultAllowUserInteraction()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "getDefaultAllowUserInteraction",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "setDefaultAllowUserInteraction",
            args = {boolean.class}
        )
    })
    public void test_getDefaultAllowUserInteraction() throws IOException {
        boolean oldSetting = URLConnection.getDefaultAllowUserInteraction();

        URLConnection.setDefaultAllowUserInteraction(false);
        assertFalse(
                "getDefaultAllowUserInteraction should have returned false",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(true);
        assertTrue("getDefaultAllowUserInteraction should have returned true",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(oldSetting);
    }

    /**
     * @tests {@link java.net.URLConnection#getDefaultRequestProperty(String)}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "getDefaultRequestProperty",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "setDefaultRequestProperty",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    @SuppressWarnings("deprecation")
    public void test_getDefaultRequestPropertyLjava_lang_String() {
        URLConnection.setDefaultRequestProperty("Shmoo", "Blah");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        URLConnection.setDefaultRequestProperty("Shmoo", "Boom");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Kapow"));

        URLConnection.setDefaultRequestProperty("Shmoo", null);
    }

    /**
     * @throws IOException 
     * @tests {@link  java.net.URLConnection#getDefaultUseCaches()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "getDefaultUseCaches",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch. test fails: throws undocumented exception IllegalAccessException.",
            method = "setDefaultUseCaches",
            args = {boolean.class}
        )
    })
    public void test_getDefaultUseCaches_CachedRC() throws IOException {
        boolean oldSetting = uc.getDefaultUseCaches();
        
        ResponseCache old = ResponseCache.getDefault();
        ResponseCache rc = new MockCachedResponseCache();
        ResponseCache.setDefault(rc);
        
        // Recreate the connection so that we get the cache from ResponseCache.
        uc2 = url2.openConnection();
        
        uc2.setUseCaches(true);
        
        uc.setDefaultUseCaches(false);

        // uc unaffected
        assertTrue(uc.getUseCaches());
        // uc2 unaffected
        assertTrue(uc2.getUseCaches());
        
        //test get
        assertFalse("getDefaultUseCaches should have returned false", uc
                .getDefaultUseCaches());
        
        // subsequent connections should have default value
        URL url3 =  new URL(Support_Configuration.hTTPURLyahoo);
        URLConnection uc3 = url3.openConnection();
        assertFalse(uc3.getUseCaches());
        
        // test if uc does not chash but uc2 does
        isGetCalled = false;
        isPutCalled = false;
        
        // test uc
        uc.setDoOutput(true);
        
        assertFalse(isGetCalled);
        uc.connect();
        assertFalse(isGetCalled);
        assertFalse(isPutCalled);
        OutputStream os = uc.getOutputStream();
        assertFalse(isPutCalled);
        assertFalse(isGetCalled);
        
        os.close();
        
        isGetCalled = false;
        isPutCalled = false;
        
        //uc2 should be unaffected
        uc2.setDoOutput(true);
        assertFalse(isGetCalled);
        uc2.connect();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);

        uc.setDefaultUseCaches(oldSetting);
        ResponseCache.setDefault(null);
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getDoInput()}
     */
    @TestTargets({
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getDoInput",
        args = {}
    ),
    @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "setDoInput",
            args = {boolean.class}
        )
    })
    public void test_getDoInput() throws IOException {
        assertTrue("Should be set to true by default", uc.getDoInput());

        fileURLCon.setDoInput(true);
        assertTrue("Should have been set to true", fileURLCon.getDoInput());

        uc2.setDoInput(false);
        assertFalse("Should have been set to false", uc2.getDoInput());

        fileURLCon.connect();
        fileURLCon.getInputStream();
        
        uc2.connect();
        try {
            uc2.getInputStream();
        } catch (Throwable e) {
            // ok
        }

    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getDoOutput()}
     */
    @TestTargets({
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getDoOutput",
        args = {}
    ),
    @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "From harmony branch.",
            method = "setDoOutput",
            args = {boolean.class}
        )
    })
    public void test_getDoOutput() throws IOException {
        assertFalse("Should be set to false by default", uc.getDoOutput());

        uc.setDoOutput(true);
        assertTrue("Should have been set to true", uc.getDoOutput());
        
        uc.connect();
        uc.getOutputStream();

        uc2.setDoOutput(false);
        assertFalse("Should have been set to false", uc2.getDoOutput());
        uc2.connect();
        
        try{
            uc2.getOutputStream();
        } catch (Throwable e) {
            //ok
        }
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getExpiration()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getExpiration",
        args = {}
    )
    @KnownFailure("URLConnection.getExpiration crashes because the returned" +
            " expiration date doesn't seems to be parsable.")
    public void test_getExpiration() throws IOException {
        URL url3 = new URL(Support_Configuration.hTTPURLwExpiration);
        URLConnection uc3 = url3.openConnection();
        
        uc.connect();
        // should be unknown
        assertEquals("getExpiration returned wrong expiration", 0, uc
                .getExpiration());
        
        uc3.connect();
        assertTrue("getExpiration returned wrong expiration", uc3
                .getExpiration() > 0);
        
        ((HttpURLConnection) uc3).disconnect();
    }

    /**
     * @tests {@link java.net.URLConnection#getFileNameMap()}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "From harmony branch.",
        method = "getFileNameMap",
        args = {}
    )
    public void test_getFileNameMap() {
        // Tests for the standard MIME types -- users may override these
        // in their JRE
        
        FileNameMap mapOld = URLConnection.getFileNameMap();
        
        try {
        // These types are defaulted
        assertEquals("text/html", mapOld.getContentTypeFor(".htm"));
        assertEquals("text/html", mapOld.getContentTypeFor(".html"));
        assertEquals("text/plain", mapOld.getContentTypeFor(".text"));
        assertEquals("text/plain", mapOld.getContentTypeFor(".txt"));

        // These types come from the properties file :
        // not black-box testing. Special tests moved to setContentType
        /*
        assertEquals("application/pdf", map.getContentTypeFor(".pdf"));
        assertEquals("application/zip", map.getContentTypeFor(".zip"));
        assertEquals("image/gif", map.getContentTypeFor("gif"));
        */

        URLConnection.setFileNameMap(new FileNameMap() {
            public String getContentTypeFor(String fileName) {
                return "Spam!";
            }
        });
       
            assertEquals("Incorrect FileNameMap returned", "Spam!",
                    URLConnection.getFileNameMap().getContentTypeFor(null));
        } finally {
            // unset the map so other tests don't fail
            URLConnection.setFileNameMap(mapOld);
        }
        // RI fails since it does not support fileName that does not begin with
        // '.'
        
    }

    /**
     * @tests {@link java.net.URLConnection#getHeaderFieldDate(java.lang.String, long)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getHeaderFieldDate",
        args = {java.lang.String.class, long.class}
    )
    public void test_getHeaderFieldDateLjava_lang_StringJ() {

        if (uc2.getHeaderFieldDate("Date", 22L) == 22L) {
            System.out
                    .println("WARNING: Server does not support 'Date', test_getHeaderFieldDateLjava_lang_StringJ not run");
            return;
        }
        
        if (uc2.getIfModifiedSince() > 0) {
        
        long time = uc2.getHeaderFieldDate("Last-Modified", 0);
        assertTrue(time > 0);
        /*
          assertEquals("Wrong date: ", time,
                Support_Configuration.URLConnectionLastModified);
        */        
        }
        
        long defaultTime;
        
        if (uc.getIfModifiedSince() == 0) {
            defaultTime = uc.getHeaderFieldDate("Last-Modified", 0);
            assertEquals(defaultTime,0);
        }
    }

    /**
     * @tests {@link java.net.URLConnection#getHeaderField(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_NECESSARY,
        notes = "not supported. Always returns null.From harmony branch.",
        method = "getHeaderField",
        args = {int.class}
    )
    public void DISABLED_test_getHeaderFieldI() {
        int i = 0;
        String hf;
        boolean foundResponse = false;
        while ((hf = uc.getHeaderField(i++)) != null) {
            if (hf.equals(Support_Configuration.HomeAddressSoftware)) {
                foundResponse = true;
            }
        }
        assertTrue("Could not find header field containing \""
                + Support_Configuration.HomeAddressSoftware + "\"",
                foundResponse);

        i = 0;
        foundResponse = false;
        while ((hf = uc.getHeaderField(i++)) != null) {
            if (hf.equals(Support_Configuration.HomeAddressResponse)) {
                foundResponse = true;
            }
        }
        assertTrue("Could not find header field containing \""
                + Support_Configuration.HomeAddressResponse + "\"",
                foundResponse);
    }

    /**
     * @tests {@link java.net.URLConnection#getHeaderFieldKey(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_NECESSARY,
        notes = "Not supported. Current implementation returns always null according to spec.",
        method = "getHeaderFieldKey",
        args = {int.class}
    )
    public void DISABLED_test_getHeaderFieldKeyI() {
        String hf;
        boolean foundResponse = false;
        for (int i = 0; i < 100; i++) {
            hf = uc.getHeaderFieldKey(i);
            if (hf != null && hf.toLowerCase().equals("content-type")) {
                foundResponse = true;
                break;
            }
        }
        assertTrue(
                "Could not find header field key containing \"content-type\"",
                foundResponse);
    }
    
    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getHeaderFieldInt(String, int)}
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "",
        method = "getHeaderFieldInt",
        args = {java.lang.String.class, int.class}
    )
    public void test_getHeaderFieldInt() throws IOException {
        String header;
        URL url3 = new URL(Support_Configuration.hTTPURLwExpiration);
        URLConnection uc3 = url3.openConnection();
        
        int hf = 0;
        hf = uc2.getHeaderFieldInt("Content-Encoding",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("Content-Length",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("Content-Type",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("Date",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        long exp = uc3.getHeaderFieldDate("Expires", 0);
        assertTrue(exp > 0);
        hf = uc2.getHeaderFieldInt("SERVER",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("Last-Modified",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("accept-ranges",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("DoesNotExist",Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, hf);
        hf = uc2.getHeaderFieldInt("Age",Integer.MIN_VALUE);
        assertFalse(hf == Integer.MIN_VALUE);
        
        ((HttpURLConnection) uc3).disconnect();
    }

    /**
     * @tests {@link java.net.URLConnection#getHeaderField(java.lang.String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getHeaderField",
        args = {java.lang.String.class}
    )
    @BrokenTest("Flaky due to third party servers used to do the test.")
    public void test_getHeaderFieldLjava_lang_String() {
        String hf;
        int hfDefault;
        hf = uc.getHeaderField("Content-Encoding");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Content-Encoding': "
                            + hf, hf);
        }
        hf = uc.getHeaderField("Content-Length");
        if (hf != null) {
            assertEquals(
                    "Wrong value returned for header field 'Content-Length': ",
                    "25", hf);
        }
        hf = uc.getHeaderField("Content-Type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Content-Type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("content-type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'content-type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("Date");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Date': " + hf,
                    Integer.parseInt(hf.substring(hf.length() - 17,
                            hf.length() - 13)) >= 1999);
        }
        hf = uc.getHeaderField("Expires");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Expires': " + hf,
                    hf);
        }
        hf = uc.getHeaderField("SERVER");
        assertNotNull(hf);
        hf = uc.getHeaderField("Last-Modified");
        if (hf != null) {
            assertTrue(
                    "No valid header field "
                            + hf,
                    Long.parseLong(hf) > 930000000000L);
        }
        hf = uc.getHeaderField("accept-ranges");
        if (hf != null) {
            assertTrue(
                    "Wrong value returned for header field 'accept-ranges': "
                            + hf, hf.equals("bytes"));
        }
        hf = uc.getHeaderField("DoesNotExist");
        if (hf != null) {
            assertNull("Wrong value returned for header field 'DoesNotExist': "
                    + hf, hf);
        }
    }

    /**
     * @throws URISyntaxException 
     * @throws ClassNotFoundException 
     * @tests {@link java.net.URLConnection#getHeaderFields()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getHeaderFields",
        args = {}
    )
    public void test_getHeaderFields() throws IOException, ClassNotFoundException, URISyntaxException {
        try {
            uc2.getInputStream();
        } catch (IOException e) {
            fail("Error in test setup: "+e.getMessage());
        }

        Map<String, List<String>> headers = uc2.getHeaderFields();
        assertNotNull(headers);

        // 'content-type' is most likely to appear
        List<String> list = headers.get("content-type");
        if (list == null) {
            list = headers.get("Content-Type");
        }
        assertNotNull(list);
        String contentType = (String) list.get(0);
        assertNotNull(contentType);

        // there should be at least 2 headers
        assertTrue("Not more than one header in URL connection",headers.size() > 1);
        
        JarURLConnection con1 = openJarURLConnection();
        headers = con1.getHeaderFields();
        assertNotNull(headers);
        assertEquals(0, headers.size());

        try {
            // the map should be unmodifiable
            headers.put("hi", Arrays.asList(new String[] { "bye" }));
            fail("The map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getLastModified()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "From harmony branch.",
        method = "getLastModified",
        args = {}
    )
    public void test_getLastModified() throws IOException {
        
        URL url4 = new URL(Support_Configuration.hTTPURLwLastModified);
        URLConnection uc4 = url4.openConnection();
        
        uc4.connect();
        
        if (uc4.getLastModified() == 0) {
            System.out
                    .println("WARNING: Server does not support 'Last-Modified', test_getLastModified() not run");
            return;
        }
        
        long millis = uc4.getHeaderFieldDate("Last-Modified", 0);
        
        assertEquals(
                "Returned wrong getLastModified value.  Wanted: "
                        + " got: " + uc4.getLastModified(),
                millis, uc4.getLastModified());
        
        
        ((HttpURLConnection) uc).disconnect();
    }
    
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "getOutputStream",
      args = {}
    )
    public void test_getOutputStream() throws IOException {
        String posted = "this is a test";
        uc.setDoOutput(true);
        uc.connect();

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(uc
                .getOutputStream()), posted.getBytes().length);

        w.write(posted);
        w.flush();
        w.close();

        int code = ((HttpURLConnection) uc).getResponseCode();


        // writing to url not allowed
        assertEquals("Got different responseCode ", 405, code);


        // try exception testing
        try {
            fileURLCon.setDoOutput(true);
            fileURLCon.connect();
            fileURLCon.getOutputStream();
        } catch (UnknownServiceException e) {
            // ok cannot write to fileURL
        }

        ((HttpURLConnection) uc2).disconnect();

        try {
            uc2.getOutputStream();
            fail("Exception expected");
        } catch (IOException e) {
            // ok
        }
    }

    /**
     * @tests {@link java.net.URLConnection#getPermission()}
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "From harmony branch.",
        method = "getPermission",
        args = {}
    )
    public void test_getPermission() throws Exception {
        java.security.Permission p = uc.getPermission();
        assertTrue("Permission of wrong type: " + p.toString(),
                p instanceof java.net.SocketPermission);
        assertTrue("Permission has wrong name: " + p.getName(), p.getName()
                .contains("google.com:" + port));

        URL fileUrl = new URL("file:myfile");
        Permission perm = new FilePermission("myfile", "read");
        Permission result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 1:" + perm + " , " + result, result
                .equals(perm));

        fileUrl = new URL("file:/myfile/");
        perm = new FilePermission("/myfile", "read");
        result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 2:" + perm + " , " + result, result
                .equals(perm));

        fileUrl = new URL("file:///host/volume/file");
        perm = new FilePermission("/host/volume/file", "read");
        result = fileUrl.openConnection().getPermission();
        assertTrue("Wrong file: permission 3:" + perm + " , " + result, result
                .equals(perm));

        URL httpUrl = new URL("http://home/myfile/");
        assertTrue("Wrong http: permission", httpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home:80", "connect")));
        httpUrl = new URL("http://home2:8080/myfile/");
        assertTrue("Wrong http: permission", httpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home2:8080", "connect")));
        URL ftpUrl = new URL("ftp://home/myfile/");
        assertTrue("Wrong ftp: permission", ftpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home:21", "connect")));
        ftpUrl = new URL("ftp://home2:22/myfile/");
        assertTrue("Wrong ftp: permission", ftpUrl.openConnection()
                .getPermission().equals(
                        new SocketPermission("home2:22", "connect")));

        URL jarUrl = new URL("jar:file:myfile!/");
        perm = new FilePermission("myfile", "read");
        result = jarUrl.openConnection().getPermission();
        assertTrue("Wrong jar: permission:" + perm + " , " + result, result
                .equals(new FilePermission("myfile", "read")));
    }

    /**
     * @tests {@link java.net.URLConnection#getRequestProperties()}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "implementation test.From harmony branch.",
        method = "getRequestProperties",
        args = {}
    )
    public void test_getRequestProperties() {
        uc.setRequestProperty("whatever", "you like");
        Map headers = uc.getRequestProperties();

        // content-length should always appear
        List header = (List) headers.get("whatever");
        assertNotNull(header);

        assertEquals("you like", header.get(0));

        assertTrue(headers.size() >= 1);

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }
        try {
            // the list should be unmodifiable
            header.add("hi");
            fail();
        } catch (UnsupportedOperationException e) {
        }

    }

    /**
     * @tests {@link java.net.URLConnection#getRequestProperties()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Exceptions checked only.From harmony branch.",
        method = "getRequestProperties",
        args = {}
    )
    public void test_getRequestProperties_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.connect();

        try {
            urlCon.getRequestProperties();
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        
    }

    /**
     * @tests {@link java.net.URLConnection#getRequestProperty(java.lang.String)}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Exceptions checked only.From harmony branch.",
        method = "getRequestProperty",
        args = { String.class }
    )
    public void test_getRequestProperty_LString_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.setRequestProperty("test", "testProperty");
        assertNull(urlCon.getRequestProperty("test"));

        urlCon.connect();
        try {
            urlCon.getRequestProperty("test");
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.net.URLConnection#getRequestProperty(java.lang.String)}
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "From harmony branch.",
        method = "getRequestProperty",
        args = {java.lang.String.class}
    )
    public void test_getRequestPropertyLjava_lang_String() {
        uc.setRequestProperty("Yo", "yo");
        assertTrue("Wrong property returned: " + uc.getRequestProperty("Yo"),
                uc.getRequestProperty("Yo").equals("yo"));
        assertNull("Wrong property returned: " + uc.getRequestProperty("No"),
                uc.getRequestProperty("No"));
    }

    /**
     * @tests {@link java.net.URLConnection#getURL()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Exceptions checked only -> only partially implemented. From harmony branch.",
        method = "getURL",
        args = {}
    )
    public void test_getURL() {
        assertTrue("Incorrect URL returned", uc.getURL().equals(url));
    }

    /**
     * @throws IOException 
     * @tests {@link java.net.URLConnection#getUseCaches()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Exceptions checked in setUseCaches. From harmony branch.",
            method = "getUseCaches",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Exceptions checked in setUseCaches. From harmony branch.",
            method = "setUseCaches",
            args = {boolean.class}
        )
    })
    public void test_getUseCaches() throws IOException {
        uc2.setUseCaches(false);
        assertTrue("getUseCaches should have returned false", !uc2
                .getUseCaches());
        uc2.setUseCaches(true);
        assertTrue("getUseCaches should have returned true", uc2.getUseCaches());
        
        uc2.connect();
        
        
        try {
        uc2.setUseCaches(false);
        fail("Exception expected");
        } catch (IllegalStateException e) {
            //ok
        }
        
        ((HttpURLConnection) uc2).disconnect();
        
    }
    
    /**
     * @tests {@link java.net.URLConnection#guessContentTypeFromName(String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "guessContentTypeFromName",
        args = {java.lang.String.class}
    )
    public void test_guessContentTypeFromName()
            throws IOException {
        
        URLConnection htmlFileCon = openHTMLFile();
        String[] expected = new String[] {"text/html",
                "text/plain" };
        String[] resources = new String[] { 
                htmlFileCon.getURL().toString(),
                fileURL.toString()
                };
        for (int i = 0; i < resources.length; i++) {
                String mime = URLConnection.guessContentTypeFromName( resources[i]);
                assertEquals("checking " + resources[i] + " expected " + expected[i]+"got " + expected[i],
                expected[i], mime);
        }

        // Try simple case
        try {
            URLConnection.guessContentTypeFromStream(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.net.URLConnection#guessContentTypeFromStream(java.io.InputStream)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test fails at UTF-8 stage. Test from harmony branch",
        method = "guessContentTypeFromStream",
        args = {java.io.InputStream.class}
    )
    @BrokenTest("MIME type application xml is not supported: only text html."+
            " Should be implemented if compatibility is required. The RI" +
            " on the other hand doesn't recognise the '<head' tag.")
    public void test_guessContentTypeFromStreamLjava_io_InputStream()
            throws IOException {
        String[] headers = new String[] { "<html>", "<head>", " <head ",
                "<body", "<BODY ", //"<!DOCTYPE html",
                "<?xml " };
        String[] expected = new String[] { "text/html","text/html", "text/html",
                "text/html","text/html", "application/xml" };

        String[] encodings = new String[] { "ASCII", "UTF-8", 
                //"UTF-16BE", not supported
                //"UTF-16LE", not supported
                //"UTF-32BE", not supported encoding
                //"UTF-32LE" not supported encoding
                };
        for (int i = 0; i < headers.length; i++) {
            for (String enc : encodings) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                String encodedString = new String(headers[i].getBytes(), enc);
                InputStream is = new ByteArrayInputStream(encodedString.getBytes());
                String mime = URLConnection.guessContentTypeFromStream(is);
                assertEquals("checking " + headers[i] + " with " + enc,
                        expected[i], mime);
            }
        }

        // Try simple case
        try {
            URLConnection.guessContentTypeFromStream(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        /* not supported
        // Test magic bytes
        byte[][] bytes = new byte[][] { { 'P', 'K' }, { 'G', 'I' } };
        expected = new String[] { "application/zip", "image/gif" };

        for (int i = 0; i < bytes.length; i++) {
            InputStream is = new ByteArrayInputStream(bytes[i]);
            assertEquals(expected[i], URLConnection
                    .guessContentTypeFromStream(is));
        }
        */
    }
    
//    /**
//     * @throws IOException 
//     * @throws IllegalAccessException 
//     * @throws IllegalArgumentException 
//     * @throws URISyntaxException 
//     * @throws MalformedURLException
//     * @tests {@link java.net.URLConnection#setContentHandlerFactory(java.net.ContentHandlerFactory)}
//     */
//    @TestTargetNew(
//        level = TestLevel.SUFFICIENT,
//        notes = "test adopted from ConentHandlerFactoryTest.",
//        method = "setContentHandlerFactory",
//        args = {java.net.ContentHandlerFactory.class}
//    )
//    public void testSetContentHandlerFactory() throws IOException,
//            IllegalArgumentException, IllegalAccessException, URISyntaxException {
//        String[] files = {
//                "hyts_checkInput.txt", "hyts_htmltest.html"};
//        ContentHandlerFactory factory = new TestContentHandlerFactory();
//        Field contentHandlerFactoryField = null;
//        int counter = 0;
//
//
//        Field[] fields = URLConnection.class.getDeclaredFields();
//
//
//        for (Field f : fields) {
//            if (ContentHandlerFactory.class.equals(f.getType())) {
//                counter++;
//                contentHandlerFactoryField = f;
//            }
//        }
//
//        if (counter != 1) {
//            fail("Error in test setup: not Factory found");
//        }
//
//        contentHandlerFactoryField.setAccessible(true);
//        ContentHandlerFactory old = (ContentHandlerFactory) contentHandlerFactoryField
//                .get(null);
//
//        try {
//            contentHandlerFactoryField.set(null, factory);
//
//            Vector<URL> urls = createContent(files);
//            for (int i = 0; i < urls.size(); i++) {
//                URLConnection urlCon = null;
//                try {
//                    urlCon = urls.elementAt(i).openConnection();
//                    urlCon.setDoInput(true);
//                    Object obj = urlCon.getContent();
//                    if (obj instanceof String) {
//                        String s = (String) obj;
//                        assertTrue("Returned incorrect content for "
//                                + urls.elementAt(i) + ": " + s, 
//                                s.equals("ok"));
//                    } else {
//                        fail("Got wrong content handler");
//                    }
//                } catch (IOException e) {
//                    fail("IOException was thrown for URL: "+ urls.elementAt(i).toURI().toString() +" " + e.toString());
//                }
//            }
//        } finally {
//            contentHandlerFactoryField.set(null, old);
//        }
//    }

    /**
     * @tests {@link java.net.URLConnection#setConnectTimeout(int)}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setConnectTimeout",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getConnectTimeout",
            args = {}
        )
    })
    public void test_setConnectTimeoutI() throws Exception {
        URLConnection uc = new URL("http://localhost").openConnection();
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(0);
        assertEquals(0, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(100);
        assertEquals(100, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getConnectTimeout());
        
        uc2.setConnectTimeout(2);
        
        try {
        uc2.connect();
        } catch (SocketTimeoutException e) {
            //ok
        }
        
    }

    /**
     * @throws IOException
     * @tests {@link java.net.URLConnection#setFileNameMap(java.net.FileNameMap)}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setFileNameMap",
            args = {java.net.FileNameMap.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getFileNameMap",
            args = {}
        )
    })
    public void test_setFileNameMapLjava_net_FileNameMap() throws IOException {
        FileNameMap mapOld = URLConnection.getFileNameMap();
        // nothing happens if set null
        URLConnection.setFileNameMap(null);
        // take no effect
        assertNotNull(URLConnection.getFileNameMap());
        
        try {
        URLConnection.setFileNameMap(new java.net.FileNameMap(){
                        public String getContentTypeFor(String fileName) {
                           if (fileName==null || fileName.length()<1)
                      return null;
                    String name=fileName.toLowerCase();
                    String type=null;
                    if (name.endsWith(".xml"))
                      type="text/xml";
                    else if (name.endsWith(".dtd"))
                      type="text/dtd";
                    else if (name.endsWith(".pdf"))
                        type = "application/pdf";
                    else if (name.endsWith(".zip"))
                        type = "application/zip";
                    else if (name.endsWith(".gif"))
                        type = "image/gif";
                    else 
                      type="application/unknown";
                    return type;
                         }
              });
        FileNameMap mapNew = URLConnection.getFileNameMap();
        assertEquals("application/pdf", mapNew.getContentTypeFor(".pdf"));
        assertEquals("application/zip", mapNew.getContentTypeFor(".zip"));
        assertEquals("image/gif", mapNew.getContentTypeFor(".gif"));
        } finally {
    
        URLConnection.setFileNameMap(mapOld); 
        }
    }

    /**
     * @tests {@link java.net.URLConnection#setIfModifiedSince(long)}
     */
    @TestTargets ( {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setIfModifiedSince",
        args = {long.class}
    ),
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "From harmony branch.",
        method = "getIfModifiedSince",
        args = {}
        )
    })
    public void test_setIfModifiedSinceJ() throws IOException {
        URL url = new URL("http://localhost:8080/");
        URLConnection connection = url.openConnection();
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2000, Calendar.MARCH, 5);

        long sinceTime = cal.getTime().getTime();
        connection.setIfModifiedSince(sinceTime);
        assertEquals("Wrong date set", sinceTime, connection
                .getIfModifiedSince());
       
        // content should be returned
        
        uc2.setIfModifiedSince(sinceTime);
        uc2.connect();
        
        
        assertEquals(200,((HttpURLConnection) uc2).getResponseCode());
        
        try {
            uc2.setIfModifiedSince(2);
            fail("Exception expected");
        } catch (IllegalStateException e) {
            //ok
        }
        
        ((HttpURLConnection) uc2).disconnect();
        
        
    }
    
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "test that page was not renewed in time indicated -> page returned event though it should not.",
        method = "getIfModifiedSince",
        args = {}
    )
    public void test_getIfModifiedSinceJ() throws IOException {
        
        uc2.setIfModifiedSince(Calendar.getInstance().getTimeInMillis());
        uc2.connect();
        
        assertEquals(200,((HttpURLConnection) uc2).getResponseCode());
        
    }
    

    /**
     * @tests {@link java.net.URLConnection#setReadTimeout(int)}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test for SocketTimeoutException fails: instead undocumented UnknownServiceException is thrown.",
            method = "setReadTimeout",
            args = {int.class}
        ),
        @TestTargetNew(
                level = TestLevel.COMPLETE,
                notes = "Test for SocketTimeoutException fails: instead undocumented UnknownServiceException is thrown.",
                method = "getReadTimeout",
                args = {}
            )
    })
    public void test_setReadTimeoutI() throws Exception {
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(0);
        assertEquals(0, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(100);
        assertEquals(100, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getReadTimeout());
        
        byte[] ba = new byte[600];
        
        uc2.setReadTimeout(5);
        uc2.setDoInput(true);
        uc2.connect();
  
        try {
        ((InputStream) uc2.getInputStream()).read(ba, 0, 600);
        } catch (SocketTimeoutException e) {
            //ok
        } catch ( UnknownServiceException e) {
            fail(""+e.getMessage());
        }
    }

    /**
     * @tests {@link java.net.URLConnection#toString()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        
        assertTrue("Wrong toString: " + uc.toString(), uc.toString().indexOf(
                "URLConnection") > 0);
        assertTrue("Wrong toString: " + uc.toString(), uc.toString().indexOf(
                uc.getURL().toString()) > 0);
    }
    
    @TestTargetNew(
      level = TestLevel.SUFFICIENT,
      notes = "protected constructor",
      method = "URLConnection",
      args = {java.net.URL.class}
    )
    public void test_URLConnection() {
        String url = uc2.getURL().toString();
        assertEquals(url2.toString(), url);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getInputStream",
        args = {}
      )
    @BrokenTest("Flaky test due to the use of third party servers")
    public void testGetInputStream() throws IOException {
        fileURLCon.setDoInput(true);
        fileURLCon.connect();

        BufferedReader buf = new BufferedReader(new InputStreamReader(
                fileURLCon.getInputStream()), testString.getBytes().length);

        String nextline;
        while ((nextline = buf.readLine()) != null) {
            assertEquals(testString, nextline);
        }

        buf.close();


        ((HttpURLConnection) uc).disconnect();

        try {
            uc.getInputStream();
            fail("Exception expected");
        } catch (IOException e) {
            // ok
        }

        uc2.getInputStream();


    }
    
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "setContentHandlerFactory",
            args = {java.net.ContentHandlerFactory.class}
     )
    public void test_setContentHandlerFactory() {
        SecurityManager sm = new SecurityManager() {
            
            public void checkPermission(Permission perm) {
            }
            
            public void checkSetFactory() {
                throw new SecurityException();
            }
        };
        SecurityManager old_sm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            uc.setContentHandlerFactory(null);
            fail("SecurityException was not thrown.");
        } catch(SecurityException se) {
            //exception
        } finally {
            System.setSecurityManager(old_sm);
        }
      
    }    
    
    private URLConnection openGifURLConnection() throws IOException {
        String cts = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(cts);
        Support_Resources.copyFile(tmpDir, null, "Harmony.GIF");
        URL fUrl1 = new URL("file:/" + tmpDir.getPath()
                + "/Harmony.GIF");
        URLConnection con1 = fUrl1.openConnection();
        return con1;
    }
    
    private JarURLConnection openJarURLConnection()
            throws MalformedURLException, IOException {
        String cts = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(cts);
        Support_Resources.copyFile(tmpDir, null, "hyts_att.jar");
        URL fUrl1 = new URL("jar:file:" + tmpDir.getPath()
                + "/hyts_att.jar!/");
        JarURLConnection con1 = (JarURLConnection) fUrl1.openConnection();
        return con1;
    }
    
    private URLConnection openHTMLFile() throws IOException {
        String cts = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(cts);
        Support_Resources.copyFile(tmpDir, null, "hyts_htmltest.html");
        URL fUrl1 = new URL("file:/" + tmpDir.getPath()
                + "/hyts_htmltest.html");
        URLConnection con1 = fUrl1.openConnection();
        return con1;
    }
    
    private URL createTempHelloWorldFile() throws MalformedURLException {
        // create content to read
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File sampleFile = null;
        try {
            if (tmpDir.isDirectory()) {
                sampleFile = File.createTempFile("openStreamTest", ".txt",
                        tmpDir);
                sampleFile.deleteOnExit();
            } else {
                fail("Error in test setup tmpDir does not exist");
            }

            FileWriter fstream = new FileWriter(sampleFile);
            BufferedWriter out = new BufferedWriter(fstream, testString.getBytes().length);
            out.write(testString);
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            fail("Error: in test setup" + e.getMessage());
        }
        
        // read content from file
        return sampleFile.toURL();
    }
    
//    /**
//     * Method copied form ContentHandlerFactory
//     */
//    private Vector<URL> createContent(String [] files) {
//        
//        File resources = new File(System.getProperty("java.io.tmpdir"));
//        
//        String resPath = resources.toString();
//        if (resPath.charAt(0) == '/' || resPath.charAt(0) == '\\')
//            resPath = resPath.substring(1);
//        
//        Vector<URL> urls = new Vector<URL> ();
//        
//        for(String file:files) {
//            Support_Resources.copyFile(resources, null, file);
//            URL resourceURL;
//            try {
//                resourceURL = new URL("file:/" + resPath + "/"
//                        + file);
//                urls.add(resourceURL);
//            } catch (MalformedURLException e) {
//                fail("URL can be created for " + file);
//            }
//            
//        }
//        return urls;
//    }
//    
//    public class TestContentHandler extends ContentHandler {
//
//        public Object getContent(URLConnection u) {
//             
//            return new String("ok");
//        }
//    }
//    
//
//    public class TestContentHandlerFactory implements ContentHandlerFactory {
//
//        final String[] mimeTypes = {
//                "text/plain", "application/xml", "image/gif", "application/zip"};
//
//        public ContentHandler createContentHandler(String mimetype) {
//            boolean isAllowed = false;
//            for (String mime : mimeTypes) {
//                if (mime.equals(mimetype)) isAllowed = true;
//            }
//            if (isAllowed) {
//                return new TestContentHandler();
//            } else
//                return null;
//        }
//    }
}
