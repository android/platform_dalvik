/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.api.javax.net.ssl;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import javax.net.ssl.SSLProtocolException;

import junit.framework.TestCase;

@TestTargetClass(SSLProtocolException.class) 
public class SSLProtocolExceptionTest extends TestCase {
    
    private static String[] msgs = {
            "",
            "Check new message",
            "Check new message Check new message Check new message Check new message Check new message" };
    
    
    /**
     * Test for <code>SSLProtocolException(String)</code> constructor Assertion:
     * constructs SSLProtocolException with detail message msg. Parameter
     * <code>msg</code> is not null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "SSLProtocolException",
        args = {java.lang.String.class}
    )
    public void test_Constructor01() {
        SSLProtocolException sslE;
        for (int i = 0; i < msgs.length; i++) {
            sslE = new SSLProtocolException(msgs[i]);
            assertEquals("getMessage() must return: ".concat(msgs[i]), sslE.getMessage(), msgs[i]);
            assertNull("getCause() must return null", sslE.getCause());
        }
    }
    
    /**
     * Test for <code>SSLProtocolException(String)</code> constructor Assertion:
     * constructs SSLProtocolException with detail message msg. Parameter
     * <code>msg</code> is null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "SSLProtocolException",
        args = {java.lang.String.class}
    )
    public void test_Constructor02() {
        String msg = null;
        SSLProtocolException sslE = new SSLProtocolException(msg);
        assertNull("getMessage() must return null.", sslE.getMessage());
        assertNull("getCause() must return null", sslE.getCause());
    }
}