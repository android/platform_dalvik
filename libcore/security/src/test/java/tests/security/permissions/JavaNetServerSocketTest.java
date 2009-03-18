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

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.Permission;
/*
 * This class tests the security permissions which are documented in
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/permissions.html#PermsAndMethods
 * for class java.net.ServerSocket
 */
@TestTargetClass(java.net.ServerSocket.class)
public class JavaNetServerSocketTest extends TestCase {
    
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
            notes = "Verifies that java.net.ServerSocket constructor calls checkListen on the security manager.",
            method = "ServerSocket",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.ServerSocket constructor calls checkListen on the security manager.",
            method = "ServerSocket",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.ServerSocket constructor calls checkListen on the security manager.",
            method = "ServerSocket",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies that java.net.ServerSocket constructor calls checkListen on the security manager.",
            method = "ServerSocket",
            args = {int.class, int.class, java.net.InetAddress.class}
        )
    })
    public void test_ctor() throws IOException {
        class TestSecurityManager extends SecurityManager {
            boolean called = false;
            int port = 0;
            void reset(){
                called = false;
                port = 0;
            }
            @Override
            public void checkListen(int port) {
                called = true;
                this.port = port;
            }
            @Override
            public void checkPermission(Permission permission) {
                
            }
        }
        
        TestSecurityManager s = new TestSecurityManager();
        System.setSecurityManager(s);
        
        s.reset();
        ServerSocket ss = new ServerSocket(8888);
        assertTrue("java.net.ServerSocket ctor must call checkListen on security permissions", s.called);
        assertEquals("Argument of checkListen is not correct", 8888, s.port);    
        ss.close();
        
        s.reset();
        ss = new ServerSocket(8888, 55);
        assertTrue("java.net.ServerSocket ctor must call checkListen on security permissions", s.called);
        assertEquals("Argument of checkListen is not correct", 8888, s.port);    
        ss.close();
        
        s.reset();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(0));
        assertTrue("java.net.ServerSocket ctor must call checkListen on security permissions", s.called);
        assertEquals("Argument of checkListen is not correct", 0, s.port);    
        ss.close();
        
        s.reset();
        ss = new ServerSocket(8888, 55, InetAddress.getLocalHost());
        assertTrue("java.net.ServerSocket ctor must call checkListen on security permissions", s.called);
        assertEquals("Argument of checkListen is not correct", 8888, s.port);    
        ss.close();
    }
}
