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

package org.apache.harmony.luni.tests.java.net;

import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ResponseCache;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import tests.support.Support_Configuration;

@TestTargetClass(HttpURLConnection.class)
public class HttpURLConnectionTest extends junit.framework.TestCase {
    
    final String unknownURL = "http://unknown.host";

    URL url;

    HttpURLConnection uc;

    private boolean isGetCalled;

    private boolean isPutCalled;

    private boolean isCacheWriteCalled;

    private boolean isAbortCalled;

    private Map<String, List<String>> mockHeaderMap;

    private InputStream mockIs = new MockInputStream();
    
    /**
     * @tests java.net.HttpURLConnection#getResponseCode()
     */
    @TestTargetNew(
      level = TestLevel.SUFFICIENT,
      notes = "Verifies only successful response.",
      method = "getResponseCode",
      args = {}
    )
    public void test_getResponseCode() {
        try {
            uc.connect();
            assertEquals("Wrong response", 200, uc.getResponseCode());
        } catch (IOException e) {
            fail("Unexpected exception : " + e.getMessage());
        }
        try {
            URL url = new URL(unknownURL);   
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.getResponseCode();
            fail("IOException was not thrown.");
        } catch(IOException e) {
            //expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getResponseMessage()
     */
    @TestTargetNew(
      level = TestLevel.SUFFICIENT,
      notes = "Verifies only successful response message.",
      method = "getResponseMessage",
      args = {}
    )
    public void test_getResponseMessage() {
        try {
            uc.connect();
            assertTrue("Wrong response: " + uc.getResponseMessage(), uc
                    .getResponseMessage().equals("OK"));
        } catch (IOException e) {
            fail("Unexpected exception : " + e.getMessage());
        }
        
        try {
            URL url = new URL(unknownURL);   
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getResponseMessage();
            fail("IOException was not thrown.");
        } catch(IOException e) {
            //expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getHeaderFields()
     */
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "getHeaderFields",
      args = {}
    )
    public void test_getHeaderFields() throws Exception {
        url = new URL("http://" + Support_Configuration.testURL);
        uc = (HttpURLConnection) url.openConnection();
        try {
            uc.getInputStream();
        } catch (IOException e) {
            fail();
        }
        Map headers = uc.getHeaderFields();
        List list = (List) headers.get("Content-Length");
        if (list == null) {
            list = (List) headers.get("content-length");
        }
        assertNotNull(list);

        // content-length should always appear
        String contentLength = (String) list.get(0);
        assertNotNull(contentLength);

        // there should be at least 2 headers
        assertTrue(headers.size() > 1);

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }

        try {
            // the list should be unmodifiable
            list.set(0, "whatever");
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperties()
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRequestProperties",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",                
            method = "setRequestProperty",
            args = {String.class, String.class}
        )
    })
    public void test_getRequestProperties() {
        uc.setRequestProperty("whatever", "you like");
        Map headers = uc.getRequestProperties();

        List newHeader = (List) headers.get("whatever");
        assertNotNull(newHeader);

        assertEquals("you like", newHeader.get(0));

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperty(String)
     */
    @TestTargets({
        @TestTargetNew(
          level = TestLevel.PARTIAL_COMPLETE,
          notes = "",
          method = "getRequestProperty",
          args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Exception test verification.",                
            method = "setRequestProperty",
            args = {String.class, String.class}
        )
    })
    public void test_getRequestPropertyLjava_lang_String_BeforeConnected()
            throws MalformedURLException, IOException {
        uc.setRequestProperty("whatever", "you like"); //$NON-NLS-1$//$NON-NLS-2$
        String res = uc.getRequestProperty("whatever"); //$NON-NLS-1$
        assertEquals("you like", res); //$NON-NLS-1$

        uc.setRequestProperty("", "you like"); //$NON-NLS-1$//$NON-NLS-2$
        res = uc.getRequestProperty(""); //$NON-NLS-1$
        assertEquals("you like", res); //$NON-NLS-1$

        uc.setRequestProperty("", null); //$NON-NLS-1$
        res = uc.getRequestProperty(""); //$NON-NLS-1$
        assertEquals(null, res);
        try {
            uc.setRequestProperty(null, "you like"); //$NON-NLS-1$
            fail("Should throw NullPointerException"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#getRequestProperty(String)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies IllegalStateException and null as a parameter.",
            method = "setRequestProperty",
            args = {String.class, String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies IllegalStateException and null as a parameter.",                
            method = "getRequestProperty",
            args = {String.class}
        )
    })
    public void test_getRequestPropertyLjava_lang_String_AfterConnected()
            throws IOException {
        uc.connect();
        try {
            uc.setRequestProperty("whatever", "you like"); //$NON-NLS-1$//$NON-NLS-2$
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
        try {
            uc.setRequestProperty(null, "you like"); //$NON-NLS-1$
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
        String res = uc.getRequestProperty("whatever"); //$NON-NLS-1$
        assertEquals(null, res);
        res = uc.getRequestProperty(null);
        assertEquals(null, res);
        try {
            uc.getRequestProperties();
            fail("Should throw IllegalStateException"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.HttpURLConnection#setFixedLengthStreamingMode_I()
     */
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "setFixedLengthStreamingMode",
      args = {int.class}
    )
    public void test_setFixedLengthStreamingModeI() throws Exception {
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        uc.setFixedLengthStreamingMode(0);
        uc.setFixedLengthStreamingMode(1);
        try {
            uc.setChunkedStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        uc.connect();
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setChunkedStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        MockHttpConnection mock = new MockHttpConnection(url);
        assertEquals(-1, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(0);
        assertEquals(0, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(1);
        assertEquals(1, mock.getFixedLength());
        mock.setFixedLengthStreamingMode(0);
        assertEquals(0, mock.getFixedLength());
    }

    /**
     * @tests java.net.HttpURLConnection#setChunkedStreamingMode_I()
     */
   @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "setChunkedStreamingMode",
      args = {int.class}
    )
    public void test_setChunkedStreamingModeI() throws Exception {
        uc.setChunkedStreamingMode(0);
        uc.setChunkedStreamingMode(-1);
        uc.setChunkedStreamingMode(-2);

        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setFixedLengthStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        uc.connect();
        try {
            uc.setFixedLengthStreamingMode(-1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        try {
            uc.setChunkedStreamingMode(1);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // correct
        }
        MockHttpConnection mock = new MockHttpConnection(url);
        assertEquals(-1, mock.getChunkLength());
        mock.setChunkedStreamingMode(-1);
        int defaultChunk = mock.getChunkLength();
        assertTrue(defaultChunk > 0);
        mock.setChunkedStreamingMode(0);
        assertEquals(mock.getChunkLength(), defaultChunk);
        mock.setChunkedStreamingMode(1);
        assertEquals(1, mock.getChunkLength());
    }

    /**
     * @tests java.net.HttpURLConnection#setFixedLengthStreamingMode_I()
     */
   @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "setFixedLengthStreamingMode",
      args = {int.class}
   )
   public void test_setFixedLengthStreamingModeI_effect() throws Exception {
        String posted = "just a test";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url
                .openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(posted.length() - 1);
        assertNull(conn.getRequestProperty("Content-length"));
        conn.setRequestProperty("Content-length", String.valueOf(posted
                .length()));
        assertEquals(String.valueOf(posted.length()), conn
                .getRequestProperty("Content-length"));
        OutputStream out = conn.getOutputStream();
        try {
            out.write(posted.getBytes());
            fail("should throw IOException");
        } catch (IOException e) {
            // correct, too many bytes written
        }
        try {
            out.close();
            fail("should throw IOException");
        } catch (IOException e) {
            // correct, too many bytes written
        }
    }

    /**
     * @tests java.net.HttpURLConnection#setChunkedStreamingMode_I()
     */
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "setChunkedStreamingMode",
      args = {int.class}
    )
    public void test_setChunkedStreamingModeI_effect() throws Exception {
        String posted = "just a test";
        // for test, use half length of the string
        int chunkSize = posted.length() / 2;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url
                .openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setChunkedStreamingMode(chunkSize);
        assertNull(conn.getRequestProperty("Transfer-Encoding"));
        // does not take effect
        conn.setRequestProperty("Content-length", String.valueOf(posted
                .length() - 1));
        assertEquals(conn.getRequestProperty("Content-length"), String
                .valueOf(posted.length() - 1));
        OutputStream out = conn.getOutputStream();
        // no error occurs
        out.write(posted.getBytes());
        out.close();
        // no assert here, pass if no exception thrown
        assertTrue(conn.getResponseCode() > 0);
    }

    @TestTargetNew(
      level = TestLevel.PARTIAL,
      notes = "Simple test.",
      method = "getOutputStream",
      args = {}
    )
    public void test_getOutputStream_afterConnection() throws Exception {
        uc.setDoOutput(true);
        uc.connect();
        assertNotNull(uc.getOutputStream());
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetInputStream() and Connect()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "using GetInputStream() and Connect()",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_Connect_GetInputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc = (HttpURLConnection) url.openConnection();
        assertFalse(isGetCalled);
        uc.setUseCaches(true);
        uc.setDoInput(true);
        uc.connect();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        InputStream is = uc.getInputStream();
        assertTrue(isPutCalled);
        is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream() and Connect()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "using GetOutputStream() and Connect()",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_Connect_GetOutputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc.setUseCaches(true);
        URLConnection uc = url.openConnection();
        uc.setDoOutput(true);
        assertFalse(isGetCalled);
        uc.connect();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        OutputStream os = uc.getOutputStream();
        assertFalse(isPutCalled);
        os.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "real implementation\n in HttpURLConnection using GetOutputStream()",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_GetOutputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        uc = (HttpURLConnection) url.openConnection();
        assertFalse(isGetCalled);
        uc.setDoOutput(true);
        uc.setUseCaches(true);
        OutputStream os = uc.getOutputStream();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        os.write(1);
        os.flush();
        os.close();
        ((HttpURLConnection) uc).getResponseCode();
        assertTrue(isGetCalled);
    //    assertTrue(isPutCalled);
        isGetCalled = false;
        isPutCalled = false;
        //InputStream is = uc.getInputStream();
        //assertFalse(isGetCalled);
        //assertFalse(isPutCalled);
        //is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetInputStream()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "real implementation in HttpURLConnection using GetInputStream()",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_GetInputStream()
            throws Exception {
        // set cache before URLConnection created, or it does not take effect
        ResponseCache rc = new MockNonCachedResponseCache();
        ResponseCache.setDefault(rc);
        URLConnection uc = url.openConnection();
        assertFalse(isGetCalled);
        uc.setDoOutput(true);
        uc.setUseCaches(true);
        InputStream is = uc.getInputStream();
        assertTrue(isGetCalled);
        assertTrue(isPutCalled);
        ((HttpURLConnection) uc).getResponseCode();
        is.close();
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using a MockResponseCache returns cache of
     *        null
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "MockResponseCache returns cache null",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_NonCached() throws IOException {
        ResponseCache.setDefault(new MockNonCachedResponseCache());
        uc = (HttpURLConnection) url.openConnection();

        // default useCaches is true
        assertTrue(uc.getUseCaches());
        uc.setDoInput(true);

        // make sure ResponseCache.get/put is called
        isGetCalled = false;
        isPutCalled = false;
        InputStream is = uc.getInputStream();
        assertFalse(is instanceof MockInputStream);
        assertTrue(isGetCalled);
        assertTrue(isPutCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        is.read();
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        byte[] buf = new byte[1];
        is.read(buf);
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to write to cache.
        isCacheWriteCalled = false;
        buf = new byte[1];
        is.read(buf, 0, 1);
        assertTrue(isCacheWriteCalled);

        // make sure protocol handler has tried to call abort.
        isAbortCalled = false;
        is.close();
        assertTrue(isAbortCalled);
        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using a MockResponseCache returns a mock
     *        cache
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "test default, and implementation with MockResponseCache",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_Cached() throws IOException {
        ResponseCache.setDefault(new MockCachedResponseCache());
        URL u = new URL("http://" + Support_Configuration.SpecialInetTestAddress);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();

        // default useCaches is true
        assertTrue(uc.getUseCaches());

        // make sure ResponseCache.get/put is called
        isGetCalled = false;
        isPutCalled = false;
        InputStream is = uc.getInputStream();
        assertEquals(4711, is.read());
        assertTrue(isGetCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        is.read();
        assertFalse(isCacheWriteCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        byte[] buf = new byte[1];
        is.read(buf);
        assertFalse(isCacheWriteCalled);

        // make sure protocol handler doesn't try to write to cache, since
        // it has been in cache already.
        isCacheWriteCalled = false;
        buf = new byte[1];
        is.read(buf, 0, 1);
        assertFalse(isCacheWriteCalled);

        // make sure abort is not called since no write is performed
        isAbortCalled = false;
        is.close();
        assertFalse(isAbortCalled);
        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using getHeaderFields()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "tests header fields written to cache with getHeaderFields.",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_getHeaderFields()
            throws IOException {
        ResponseCache.setDefault(new MockCachedResponseCache());
        URL u = new URL("http://" + Support_Configuration.SpecialInetTestAddress);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerMap = uc.getHeaderFields();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        assertEquals(mockHeaderMap, headerMap);
        assertEquals(4711, uc.getInputStream().read());
        uc.disconnect();
    }

    /**
     * @tests java.net.URLConnection#setUseCaches() and its real implementation
     *        in HttpURLConnection using GetOutputStream()
     */
    @TestTargetNew(
      level = TestLevel.PARTIAL_COMPLETE,
      notes = "",
      method = "setUseCaches",
      args = {boolean.class}
    )
    public void test_UseCache_HttpURLConnection_NoCached_GetOutputStream()
            throws Exception {
        ResponseCache.setDefault(new MockNonCachedResponseCache());
        uc = (HttpURLConnection) url.openConnection();
        uc.setChunkedStreamingMode(10);
        uc.setDoOutput(true);
        uc.getOutputStream();
        assertTrue(isGetCalled);
        assertFalse(isPutCalled);
        assertFalse(isAbortCalled);
        uc.disconnect();
    }
    
    /**
     * @tests java.net.URLConnection#getErrorStream()
     */
    @TestTargetNew(
      level = TestLevel.SUFFICIENT,
      notes = "Negative cases depends on server responds.",
      method = "getErrorStream",
      args = {}
    )
    public void test_getErrorStream() throws Exception {
        uc.connect();
        assertEquals(200, uc.getResponseCode());        
        // no error stream
        assertNull(uc.getErrorStream());        
        uc.disconnect();
        assertNull(uc.getErrorStream());
        
        try {
            URL url = new URL(unknownURL);   
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            assertNull(conn.getErrorStream());
        } catch(IOException e) {
            fail("IOException was thrown.");
        }
    }
    
    /**
     * @tests {@link java.net.HttpURLConnection#setFollowRedirects(boolean)}
     * @tests {@link java.net.HttpURLConnection#getFollowRedirects()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getFollowRedirects",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setFollowRedirects",
            args = {boolean.class}
        )
    })
    public void test_followRedirects() {
        assertTrue("The default value of followRedirects is not true",
                HttpURLConnection.getFollowRedirects());

        HttpURLConnection.setFollowRedirects(false);
        assertFalse(HttpURLConnection.getFollowRedirects());

        HttpURLConnection.setFollowRedirects(true);
        assertTrue(HttpURLConnection.getFollowRedirects());
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkSetFactory() {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            HttpURLConnection.setFollowRedirects(false);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getInstanceFollowRedirects",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setInstanceFollowRedirects",
            args = {boolean.class}
        )
    })
    public void test_instanceFollowRedirect() {
        assertTrue(uc.getInstanceFollowRedirects());
        
        uc.setInstanceFollowRedirects(false);
        assertFalse(uc.getInstanceFollowRedirects());
        
        uc.setInstanceFollowRedirects(true);
        assertTrue(uc.getInstanceFollowRedirects());
        
        uc.setFollowRedirects(false);
        assertTrue(uc.getInstanceFollowRedirects());
    }

    /**
     * @throws ProtocolException 
     * @tests {@link java.net.HttpURLConnection#setRequestMethod(String)}
     * @tests {@link java.net.HttpURLConnection#getRequestMethod()}
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getRequestMethod",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setRequestMethod",
            args = {java.lang.String.class}
        )
    })
    public void test_requestMethod() throws MalformedURLException, ProtocolException{
        URL url = new URL("http://" + Support_Configuration.SpecialInetTestAddress);
        
        HttpURLConnection con = new MyHttpURLConnection(url);
        assertEquals("The default value of requestMethod is not \"GET\"", "GET",
                con.getRequestMethod());

        String[] methods = { "GET", "DELETE", "HEAD", "OPTIONS", "POST", "PUT",
                "TRACE" };
        // Nomal set. Should not throw ProtocolException
        for (String method : methods) {
            con.setRequestMethod(method);
            assertEquals("The value of requestMethod is not " + method, method,
                    con.getRequestMethod());
        }
            
        try {
            con.setRequestMethod("Wrong method");
            fail("Should throw ProtocolException");
        } catch (ProtocolException e) {
            // Expected
        }
        
        try {
            con.setRequestMethod("get");
            fail("Should throw ProtocolException");
        } catch (ProtocolException e) {
            // Expected
        }        
    }

    private static class MyHttpURLConnection extends HttpURLConnection {

        protected MyHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {
            // do nothing
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // do nothing
        }
    }
    
    /**
     * @tests java.net.URLConnection#getPermission()
     */
    @TestTargetNew(
      level = TestLevel.SUFFICIENT,
      notes = "IOException is not verified.",
      method = "getPermission",
      args = {}
    )
    public void test_Permission() throws Exception {
        uc.connect();
        Permission permission = uc.getPermission();
        assertNotNull(permission);
        permission.implies(new SocketPermission("localhost","connect"));
        
        try {
            URL url = new URL(unknownURL);   
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.getPermission();
        } catch(IOException e) {
            fail("IOException was thrown.");
        }
    }
    
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "HttpURLConnection",
      args = {java.net.URL.class}
    )
    public void test_Constructor() throws IOException {
        MockHttpConnection conn1 = new MockHttpConnection(url);
        conn1.connect();
        conn1.disconnect();
        
        MockHttpConnection conn2 = new MockHttpConnection(null);
        conn2.connect();
        conn2.disconnect();
        
        URL url = new URL("file://newFile.txt");
        MockHttpConnection conn3 = new MockHttpConnection(url);
        conn3.connect();
        conn3.disconnect();
    }
    
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "disconnect",
      args = {}
    )
    public void test_disconnect() {
        
        try {
            URL url1 = new URL("http://" + Support_Configuration.testURL);
            HttpURLConnection uc1 = (HttpURLConnection) url1.openConnection();
            uc1.disconnect();
            InputStream is = uc1.getInputStream();
            byte [] array = new byte [10];
            is.read(array);
            assertNotNull(array);
            try {
                uc1.connect();
                uc1.disconnect();
            } catch(IOException e) {
                fail("IOException was thrown.");
            }
        } catch (Exception e) {
            fail("Exception during setup : " + e.getMessage());
        }
    }
    
    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "getHeaderFieldDate",
      args = {java.lang.String.class, long.class}
    )
    public void test_getHeaderFieldDate(String name, long Default) {
        long date = uc.getHeaderFieldDate(uc.getHeaderField(0), 0);
        assertEquals(System.currentTimeMillis(), date);
    }

    @TestTargetNew(
      level = TestLevel.COMPLETE,
      notes = "",
      method = "usingProxy",
      args = {}
    )
    public void test_usingProxy() {
        assertFalse(uc.usingProxy());
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

    class MockCacheRequest extends CacheRequest {

        public OutputStream getBody() throws IOException {
            isCacheWriteCalled = true;
            return new MockOutputStream();
        }

        public void abort() {
            isAbortCalled = true;
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

    class MockHttpConnection extends HttpURLConnection {

        protected MockHttpConnection(URL url) {
            super(url);
        }

        public void disconnect() {
            // do nothing
        }

        public boolean usingProxy() {
            return false;
        }

        public void connect() throws IOException {
            // do nothing
        }

        public int getChunkLength() {
            return super.chunkLength;
        }

        public int getFixedLength() {
            return super.fixedContentLength;
        }

    }

    protected void setUp() {
        try {
            url = new URL(Support_Configuration.hTTPURLyahoo);
            uc = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            fail("Exception during setup : " + e.getMessage());
        }
        mockHeaderMap = new Hashtable<String, List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("value1");
        mockHeaderMap.put("field1", valueList);
        mockHeaderMap.put("field2", valueList);
        isGetCalled = false;
        isPutCalled = false;
        isCacheWriteCalled = false;
    }

    protected void tearDown() {
        uc.disconnect();
        ResponseCache.setDefault(null);
    }
}
