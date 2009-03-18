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

package tests.security.permissions;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Permission;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.net.Socket
 */
@TestTargetClass(java.net.Socket.class)
public class JavaNetSocketTest extends TestCase {
    
    SecurityManager old;

    @Override
    protected void setUp() throws Exception {
        old = System.getSecurityManager();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        System.setSecurityManager(old);
        super.tearDown();
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.lang.String.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.lang.String.class, int.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.lang.String.class, int.class, java.net.InetAddress.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.net.InetAddress.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.net.InetAddress.class, int.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.Socket constructor calls checkConnect on security manager.",
            method = "Socket",
            args = {java.net.InetAddress.class, int.class, java.net.InetAddress.class, int.class}
        )
    })
    public void test_ctor() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            String host = null;
            int port = -1;
            void reset(){
                called = false;
                host = null;
                port = -1;
            }
            @Override
            public void checkConnect(String host, int port) {
                this.called = true;
                this.port = port;
                this.host = host;
            }
            @Override
            public void checkPermission(Permission permission) {
                
            }
        }
        
        String host = "www.google.ch";
        int port = 80;
        String hostAddress = InetAddress.getByName(host).getHostAddress();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        new Socket(host, port);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
        
        s.reset();
        new Socket(host, port, true);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);

        s.reset();
        new Socket(host, port, InetAddress.getByAddress(new byte[] {0,0,0,0}), 0);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
        
        s.reset();
        new Socket(InetAddress.getByName(host), port);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
        
        s.reset();
        new Socket(InetAddress.getByName(host), port, true);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
        
        s.reset();
        new Socket(InetAddress.getByName(host), port,  InetAddress.getByAddress(new byte[] {0,0,0,0}), 0);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "",
        method = "Socket",
        args = {java.net.InetAddress.class, int.class, java.net.InetAddress.class, int.class}
    )
    @KnownFailure("throws SocketException with message: the socket level is invalid. Works on the RI")
    public void test_ctor2() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            String host = null;
            int port = -1;
            void reset(){
                called = false;
                host = null;
                port = -1;
            }
            @Override
            public void checkConnect(String host, int port) {
                this.called = true;
                this.port = port;
                this.host = host;
            }
            @Override
            public void checkPermission(Permission permission) {
                
            }
        }
        
        String host = "www.google.ch";
        int port = 80;
        String hostAddress = InetAddress.getByName(host).getHostAddress();

        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        new Socket(InetAddress.getByName(host), port,  InetAddress.getLocalHost(), 0);
        assertTrue("java.net.ServerSocket ctor must call checkConnect on security permissions", s.called);
        assertEquals("Argument of checkConnect is not correct", hostAddress, s.host);
        assertEquals("Argument of checkConnect is not correct", port, s.port);
    }
}
