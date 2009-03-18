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

package tests.api.java.net;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.NetPermission;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import tests.support.Support_Configuration;

@TestTargetClass(
    value = ResponseCache.class,
    untestedMethods = {
        @TestTargetNew(
            level = TestLevel.NOT_FEASIBLE,
            notes = "put method is not tested completely",
            method = "put",
            args = {java.net.URI.class, java.net.URLConnection.class}
        )
    }
)
public class ResponseCacheTest extends TestCase {

    
    
    /**
     * @tests java.net.ResponseCache#getDefault()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for getDefault method.",
        method = "getDefault",
        args = {}
    )
    public void test_GetDefault() throws Exception {
        assertNull(ResponseCache.getDefault());
    }

    /**
     * @tests java.net.ResponseCache#setDefault(ResponseCache)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "This is a complete subset of tests for setDefault method.",
            method = "setDefault",
            args = {java.net.ResponseCache.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "This is a complete subset of tests for setDefault method.",
            method = "ResponseCache",
            args = {}
        )
    })
    public void test_SetDefaultLjava_net_ResponseCache_Normal()
            throws Exception {
        ResponseCache rc1 = new MockResponseCache();
        ResponseCache rc2 = new MockResponseCache();
        ResponseCache.setDefault(rc1);
        assertSame(ResponseCache.getDefault(), rc1);
        ResponseCache.setDefault(rc2);
        assertSame(ResponseCache.getDefault(), rc2);
        ResponseCache.setDefault(null);
        assertNull(ResponseCache.getDefault());
    }

    /**
     * @tests java.net.ResponseCache#getDefault()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for getDefault method.",
        method = "getDefault",
        args = {}
    )
    public void test_GetDefault_Security() {
        SecurityManager old = System.getSecurityManager();
        try {
            System.setSecurityManager(new MockSM());
        } catch (SecurityException e) {
            System.err.println("No setSecurityManager permission.");
            System.err.println("test_setDefaultLjava_net_ResponseCache_NoPermission is not tested");
            return;
        }
        try {
            ResponseCache.getDefault();
            fail("should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(old);
        }
    }

    /**
     * @tests java.net.ResponseCache#setDefault(ResponseCache)
     */
    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "This is a complete subset of tests for setDefault method.",
        method = "setDefault",
        args = {java.net.ResponseCache.class}
    )
    public void test_setDefaultLjava_net_ResponseCache_NoPermission() {
        ResponseCache rc = new MockResponseCache();
        SecurityManager old = System.getSecurityManager();
        try {
            System.setSecurityManager(new MockSM());
        } catch (SecurityException e) {
            System.err.println("No setSecurityManager permission.");
            System.err.println("test_setDefaultLjava_net_ResponseCache_NoPermission is not tested");
            return;
        }
        try {
            ResponseCache.setDefault(rc);
            fail("should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(old);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "get",
        args = {java.net.URI.class, java.lang.String.class, java.util.Map.class}
    )
    @BrokenTest("This test fails on both RI and android. Also only getting " +
            "from the cache is tested. The put method is not tested.")
    public void test_get_put() throws Exception {
        
        URL url  = new URL("http://" + 
                Support_Configuration.SpecialInetTestAddress);
        ResponseCache.setDefault(new TestResponseCache());
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setUseCaches(true);
        httpCon.connect();
        try {
            Thread.sleep(5000);
        } catch(Exception e) {}
        
        InputStream is = httpCon.getInputStream();
        byte [] array = new byte [10];
        is.read(array);
        assertEquals("Cache test", new String(array));

        try {
            Thread.sleep(5000);
        } catch(Exception e) {}
        is.close();
        httpCon.disconnect();
    }

    /*
     * MockResponseCache for testSetDefault(ResponseCache)
     */
    class MockResponseCache extends ResponseCache {

        public CacheResponse get(URI arg0, String arg1, Map arg2)
                throws IOException {
            return null;
        }

        public CacheRequest put(URI arg0, URLConnection arg1)
                throws IOException {
            return null;
        }
    }

    /*
     * MockSecurityMaanger. It denies NetPermission("getResponseCache") and
     * NetPermission("setResponseCache").
     */
    class MockSM extends SecurityManager {
        public void checkPermission(Permission permission) {
            if (permission instanceof NetPermission) {
                if ("setResponseCache".equals(permission.getName())) {
                    throw new SecurityException();
                }
            }

            if (permission instanceof NetPermission) {
                if ("getResponseCache".equals(permission.getName())) {
                    
                    throw new SecurityException();
                }
            }

            if (permission instanceof RuntimePermission) {
                if ("setSecurityManager".equals(permission.getName())) {
                    return;
                }
            }
        }
    }
    
    class TestCacheResponse extends CacheResponse {
        InputStream is = null;
        Map<String, List<String>> headers = null;
        
        public TestCacheResponse(String filename) {
            String path = getClass().getPackage().getName().replace(".", "/");
            is = getClass().getResourceAsStream("/" + path + "/" + filename);
        }

        public InputStream getBody() throws IOException {
           return is;
        }

         public Map getHeaders() throws IOException {
           return null;
         }
    }
    
    class TestCacheRequest extends CacheRequest {
        
        public TestCacheRequest(String filename,
                            Map<String, List<String>> rspHeaders) {
        }
        public OutputStream getBody() throws IOException {
            return null;
        }

        public void abort() {
        }
    }
    
    class TestResponseCache extends ResponseCache {
        
        URI uri1 = null;    
    
        public CacheResponse get(URI uri, String rqstMethod, Map rqstHeaders)
                throws IOException {
          try {
            uri1  = new URI("http://" + 
                    Support_Configuration.SpecialInetTestAddress);
          } catch (URISyntaxException e) {
          }  
          if (uri.equals(uri1)) {
            return new TestCacheResponse("file1.cache");
          }
          return null;
        }

       public CacheRequest put(URI uri, URLConnection conn)
              throws IOException {
           try {
               uri1  = new URI("http://www.google.com");
             } catch (URISyntaxException e) {
             }  
          if (uri.equals(uri1)) {
              return new TestCacheRequest("file2.cache",
                          conn.getHeaderFields());
          }
          return null;
        }
    }
}