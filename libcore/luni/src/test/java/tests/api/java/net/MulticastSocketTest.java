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

package tests.api.java.net;

import dalvik.annotation.KnownFailure; 
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Enumeration;

import tests.support.Support_NetworkInterface;
import tests.support.Support_PortManager;

@TestTargetClass(MulticastSocket.class) 
public class MulticastSocketTest extends SocketTestCase {

    Thread t;

    MulticastSocket mss;

    MulticastServer server;

    // private member variables used for tests
    boolean atLeastOneInterface = false;

    boolean atLeastTwoInterfaces = false;

    private NetworkInterface networkInterface1 = null;

    private NetworkInterface networkInterface2 = null;

    private NetworkInterface IPV6networkInterface1 = null;

    static class MulticastServer extends Thread {

        public MulticastSocket ms;

        boolean running = true;

        volatile public byte[] rbuf = new byte[512];

        volatile DatagramPacket rdp = null;

        public void run() {
            try {
                while (running) {
                    try {
                        ms.receive(rdp);
                    } catch (java.io.InterruptedIOException e) {
                        Thread.yield();
                    }
                    ;
                }
                ;
            } catch (java.io.IOException e) {
                System.out.println("Multicast server failed: " + e);
            } finally {
                ms.close();
            }
        }

        public synchronized void leaveGroup(InetAddress aGroup)
                throws java.io.IOException {
            ms.leaveGroup(aGroup);
        }

        public void stopServer() {
            running = false;
        }

        public MulticastServer(InetAddress anAddress, int aPort)
                throws java.io.IOException {
            rbuf = new byte[512];
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new MulticastSocket(aPort);
            ms.setSoTimeout(2000);
            ms.joinGroup(anAddress);
        }

        public MulticastServer(SocketAddress anAddress, int aPort,
                NetworkInterface netInterface) throws java.io.IOException {
            rbuf = new byte[512];
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new MulticastSocket(aPort);
            ms.setSoTimeout(2000);
            ms.joinGroup(anAddress, netInterface);
        }
    }

    /**
     * @tests java.net.MulticastSocket#MulticastSocket()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "IOException exception checking missed.",
        method = "MulticastSocket",
        args = {}
    )
    public void test_Constructor() throws IOException {
        // regression test for 497
        MulticastSocket s = new MulticastSocket();
        // regression test for Harmony-1162
        assertTrue(s.getReuseAddress());
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkListen(int port) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new MulticastSocket();
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (IOException e) {
            fail("IOException was thrown.");
            e.printStackTrace();
        } finally {
            System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.net.MulticastSocket#MulticastSocket(int)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "IOException exception checking missed.",
        method = "MulticastSocket",
        args = {int.class}
    )
    public void test_ConstructorI() {
        // Test for method java.net.MulticastSocket(int)
        // Used in tests
        MulticastSocket dup = null;
        try {
            mss = new MulticastSocket();
            int port = mss.getLocalPort();
            dup = new MulticastSocket(port);
            // regression test for Harmony-1162
            assertTrue(dup.getReuseAddress());
        } catch (IOException e) {
            fail("duplicate binding not allowed: " + e);
        }
        
        
        
        if (dup != null)
            dup.close();
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkListen(int port) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new MulticastSocket(1);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (IOException e) {
            fail("IOException was thrown.");
            e.printStackTrace();
        } finally {
            System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.net.MulticastSocket#getInterface()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInterface",
        args = {}
    )
    @KnownFailure("No interfaces if there's no debugger connected")
    public void test_getInterface() {
        // Test for method java.net.InetAddress
        // java.net.MulticastSocket.getInterface()
        assertTrue("Used for testing.", true);

        int groupPort = Support_PortManager.getNextPortForUDP();
        try {
            if (atLeastOneInterface) {
                // validate that we get the expected response when one was not
                // set
                mss = new MulticastSocket(groupPort);
                String preferIPv4StackValue = System
                        .getProperty("java.net.preferIPv4Stack");
                String preferIPv6AddressesValue = System
                        .getProperty("java.net.preferIPv6Addresses");
                if (((preferIPv4StackValue == null) || preferIPv4StackValue
                        .equalsIgnoreCase("false"))
                        && (preferIPv6AddressesValue != null)
                        && (preferIPv6AddressesValue.equals("true"))) {
                    // we expect an IPv6 ANY in this case
                    assertTrue("inet Address returned when not set:"
                            + mss.getInterface().toString(), InetAddress
                            .getByName("::0").equals(mss.getInterface()));
                } else {
                    // we expect an IPv4 ANY in this case
                    assertNotNull("inet Address returned when not set:", 
                            mss.getInterface());
                }

                // validate that we get the expected response when we set via
                // setInterface
                Enumeration addresses = networkInterface1.getInetAddresses();
                if (addresses != null) {
                    InetAddress firstAddress = (InetAddress) addresses
                            .nextElement();
                    mss.setInterface(firstAddress);
                    assertTrue(
                            "getNetworkInterface did not return interface set " +
                            "by setInterface.  Expected:"
                                    + firstAddress + " Got:"
                                    + mss.getInterface(), firstAddress
                                    .equals(mss.getInterface()));

                    groupPort = Support_PortManager.getNextPortForUDP();
                    mss = new MulticastSocket(groupPort);
                    mss.setNetworkInterface(networkInterface1);
                    InetAddress addr = mss.getInterface();
                    NetworkInterface if1 = NetworkInterface.getByInetAddress(addr);
                    assertEquals(
                            "getInterface did not return interface set by " +
                            "setNeworkInterface Expected: " + firstAddress
                                    + "Got:" + mss.getInterface(),
                                    networkInterface1, if1);
                }
                mss.close();
                try {
                    mss.getInterface();
                    fail("SocketException was not thrown.");
                } catch(SocketException ioe) {
                    //expected
                }
            }
        } catch (Exception e) {
            fail("Exception during getInterface test: " + e.toString());
        }
    }

    /**
     * @throws IOException 
     * @tests java.net.MulticastSocket#getNetworkInterface()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getNetworkInterface",
        args = {}
    )
    @KnownFailure("No interfaces if there's no debugger connected")
    public void test_getNetworkInterface() throws IOException {
        int groupPort = Support_PortManager.getNextPortForUDP();
        if (atLeastOneInterface) {
            // validate that we get the expected response when one was not
            // set
            mss = new MulticastSocket(groupPort);
            NetworkInterface theInterface = mss.getNetworkInterface();
            assertNotNull(
                    "network interface returned wrong network interface when " +
                    "not set:"
                            + theInterface, theInterface.getInetAddresses());
            InetAddress firstAddress = (InetAddress) theInterface
                    .getInetAddresses().nextElement();
            // validate we the first address in the network interface is the
            // ANY address
            String preferIPv4StackValue = System
                    .getProperty("java.net.preferIPv4Stack");
            String preferIPv6AddressesValue = System
                    .getProperty("java.net.preferIPv6Addresses");
            if (((preferIPv4StackValue == null) || preferIPv4StackValue
                    .equalsIgnoreCase("false"))
                    && (preferIPv6AddressesValue != null)
                    && (preferIPv6AddressesValue.equals("true"))) {
                assertTrue(
                        "network interface returned wrong network interface " +
                        "when not set:"
                                + theInterface, InetAddress.getByName("::0")
                                .equals(firstAddress));

            } else {
                assertTrue(
                        "network interface returned wrong network interface " +
                        "when not set:"
                                + theInterface, InetAddress
                                .getByName("0.0.0.0").equals(firstAddress));
            }

            mss.setNetworkInterface(networkInterface1);
            assertTrue(
                    "getNetworkInterface did not return interface set by " +
                    "setNeworkInterface",
                    networkInterface1.equals(mss.getNetworkInterface()));

            if (atLeastTwoInterfaces) {
                mss.setNetworkInterface(networkInterface2);
                assertTrue(
                        "getNetworkInterface did not return network interface " +
                        "set by second setNetworkInterface call",
                        networkInterface2.equals(mss.getNetworkInterface()));
            }

            groupPort = Support_PortManager.getNextPortForUDP();
            mss = new MulticastSocket(groupPort);
            if (IPV6networkInterface1 != null) {
                mss.setNetworkInterface(IPV6networkInterface1);
                assertTrue(
                        "getNetworkInterface did not return interface set " +
                        "by setNeworkInterface",
                        IPV6networkInterface1.equals(mss.getNetworkInterface()));
            }

            // validate that we get the expected response when we set via
            // setInterface
            groupPort = Support_PortManager.getNextPortForUDP();
            mss = new MulticastSocket(groupPort);
            Enumeration addresses = networkInterface1.getInetAddresses();
            if (addresses != null) {
                firstAddress = (InetAddress) addresses.nextElement();
                mss.setInterface(firstAddress);
                assertTrue(
                        "getNetworkInterface did not return interface set " +
                        "by setInterface",
                        networkInterface1.equals(mss.getNetworkInterface()));
            }
            
            mss.close();
            try {
                mss.getNetworkInterface();
                fail("SocketException was not thrown.");
            } catch(SocketException ioe) {
                //expected
            }
        }
    }

    /**
     * @tests java.net.MulticastSocket#getTimeToLive()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getTimeToLive",
        args = {}
    )
    public void test_getTimeToLive() {
        try {
            mss = new MulticastSocket();
            mss.setTimeToLive(120);
            assertTrue("Returned incorrect 1st TTL: " + mss.getTimeToLive(),
                    mss.getTimeToLive() == 120);
            mss.setTimeToLive(220);
            assertTrue("Returned incorrect 2nd TTL: " + mss.getTimeToLive(),
                    mss.getTimeToLive() == 220);
            mss.close();
            try {
                mss.getTimeToLive();
                fail("IOException was not thrown.");
            } catch(IOException ioe) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
        }
    }

    /**
     * @tests java.net.MulticastSocket#getTTL()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getTTL",
        args = {}
    )
    public void test_getTTL() {
        // Test for method byte java.net.MulticastSocket.getTTL()

        try {
            mss = new MulticastSocket();
            mss.setTTL((byte) 120);
            assertTrue("Returned incorrect TTL: " + mss.getTTL(),
                    mss.getTTL() == 120);
            mss.close();
            try {
                mss.getTTL();
                fail("IOException was not thrown.");
            } catch(IOException ioe) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
        }
    }

    /**
     * @tests java.net.MulticastSocket#joinGroup(java.net.InetAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "joinGroup",
        args = {java.net.InetAddress.class}
    )
    public void test_joinGroupLjava_net_InetAddress() {
        // Test for method void
        // java.net.MulticastSocket.joinGroup(java.net.InetAddress)
        String msg = null;
        InetAddress group = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        int groupPort = ports[0];
        try {
            group = InetAddress.getByName("224.0.0.3");
            server = new MulticastServer(group, groupPort);
            server.start();
            Thread.sleep(1000);
            msg = "Hello World";
            mss = new MulticastSocket(ports[1]);
            DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg
                    .length(), group, groupPort);
            mss.joinGroup(group);
            mss.send(sdp, (byte) 10);
            Thread.sleep(1000);
            
            SecurityManager sm = new SecurityManager() {

                public void checkPermission(Permission perm) {
                }
                
                public void checkMulticast(InetAddress maddr) {
                    throw new SecurityException();
                }
            };

            SecurityManager oldSm = System.getSecurityManager();
            System.setSecurityManager(sm);
            try {
                mss.joinGroup(group);
                fail("SecurityException should be thrown.");
            } catch (SecurityException e) {
                // expected
            } catch (SocketException e) {
                fail("SocketException was thrown.");
            } catch (IOException e) {
                fail("IOException was thrown.");
                e.printStackTrace();
            } finally {
                System.setSecurityManager(oldSm);
            } 
            
            mss.close();
            try {
                mss.joinGroup(group);
                fail("SocketException was not thrown.");
            } catch(SocketException ioe) {
                //expected
            }
        } catch (Exception e) {
            fail("Exception during joinGroup test: " + e.toString());
        }
        assertTrue("Group member did not recv data: ", new String(server.rdp
                .getData(), 0, server.rdp.getLength()).equals(msg));

    }

    /**
     * @throws IOException 
     * @throws InterruptedException 
     * @tests java.net.MulticastSocket#joinGroup(java.net.SocketAddress,
     *                                              java.net.NetworkInterface)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "joinGroup",
        args = {java.net.SocketAddress.class, java.net.NetworkInterface.class}
    )
    @KnownFailure("Needs investigation")
    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface() 
                                    throws IOException, InterruptedException {
        // security manager that allows us to check that we only return the
        // addresses that we should
        class mySecurityManager extends SecurityManager {

            public void checkPermission(Permission perm) {
            }
            
            public void checkMulticast(InetAddress address) {
                throw new SecurityException("not allowed");
            }
        }

        String msg = null;
        InetAddress group = null;
        SocketAddress groupSockAddr = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        int groupPort = ports[0];
        int serverPort = ports[1];

            Enumeration theInterfaces = NetworkInterface.getNetworkInterfaces();

        // first validate that we handle a null group ok
        mss = new MulticastSocket(groupPort);
        try {
            mss.joinGroup(null, null);
            fail("Did not get exception when group was null");
        } catch (IllegalArgumentException e) {
        }

        // now validate we get the expected error if the address specified
        // is not a multicast group
        try {
            groupSockAddr = new InetSocketAddress(InetAddress
                    .getByName("255.255.255.255"), groupPort);
            mss.joinGroup(groupSockAddr, null);
            fail("Did not get exception when group is not a multicast address");
        } catch (IOException e) {
        }

        // now try to join a group if we are not authorized
        // set the security manager that will make the first address not
        // visible
        System.setSecurityManager(new mySecurityManager());
        try {
            group = InetAddress.getByName("224.0.0.3");
            groupSockAddr = new InetSocketAddress(group, groupPort);
            mss.joinGroup(groupSockAddr, null);
            fail("Did not get exception when joining group is not allowed");
        } catch (SecurityException e) {
        }
        System.setSecurityManager(null);

        if (atLeastOneInterface) {
            // now validate that we can properly join a group with a null
            // network interface
            ports = Support_PortManager.getNextPortsForUDP(2);
            groupPort = ports[0];
            serverPort = ports[1];
            mss = new MulticastSocket(groupPort);
            mss.joinGroup(groupSockAddr, null);
            mss.setTimeToLive(2);
            Thread.sleep(1000);

            // set up the server and join the group on networkInterface1
            group = InetAddress.getByName("224.0.0.3");
            groupSockAddr = new InetSocketAddress(group, groupPort);
            server = new MulticastServer(groupSockAddr, serverPort,
                    networkInterface1);
            server.start();
            Thread.sleep(1000);
            msg = "Hello World";
            DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg
                    .length(), group, serverPort);
            mss.setTimeToLive(2);
            mss.send(sdp);
            Thread.sleep(1000);
            // now vaildate that we received the data as expected
            assertTrue("Group member did not recv data: ", new String(
                    server.rdp.getData(), 0, server.rdp.getLength())
                    .equals(msg));
            server.stopServer();

            // now validate that we handled the case were we join a
            // different multicast address.
            // verify we do not receive the data
            ports = Support_PortManager.getNextPortsForUDP(2);
            serverPort = ports[0];
            server = new MulticastServer(groupSockAddr, serverPort,
                    networkInterface1);
            server.start();
            Thread.sleep(1000);

            groupPort = ports[1];
            mss = new MulticastSocket(groupPort);
            InetAddress group2 = InetAddress.getByName("224.0.0.4");
            mss.setTimeToLive(10);
            msg = "Hello World - Different Group";
            sdp = new DatagramPacket(msg.getBytes(), msg.length(), group2,
                    serverPort);
            mss.send(sdp);
            Thread.sleep(1000);
            assertFalse(
                    "Group member received data when sent on different group: ",
                    new String(server.rdp.getData(), 0, server.rdp.getLength())
                            .equals(msg));
            server.stopServer();

            // if there is more than one network interface then check that
            // we can join on specific interfaces and that we only receive
            // if data is received on that interface
            if (atLeastTwoInterfaces) {
                // set up server on first interfaces
                NetworkInterface loopbackInterface = NetworkInterface
                        .getByInetAddress(InetAddress.getByName("127.0.0.1"));

                theInterfaces = NetworkInterface.getNetworkInterfaces();
                while (theInterfaces.hasMoreElements()) {

                    NetworkInterface thisInterface = (NetworkInterface) 
                                                    theInterfaces.nextElement();
                    if ((thisInterface.getInetAddresses() != null && thisInterface
                            .getInetAddresses().hasMoreElements())
                            && (Support_NetworkInterface
                                    .useInterface(thisInterface) == true)) {
                        // get the first address on the interface

                        // start server which is joined to the group and has
                        // only asked for packets on this interface
                        Enumeration addresses = thisInterface
                                .getInetAddresses();

                        NetworkInterface sendingInterface = null;
                        boolean isLoopback = false;
                        if (addresses != null) {
                            InetAddress firstAddress = (InetAddress) addresses
                                    .nextElement();
                            if (firstAddress.isLoopbackAddress()) {
                                isLoopback = true;
                            }
                            if (firstAddress instanceof Inet4Address) {
                                group = InetAddress.getByName("224.0.0.4");
                                if (networkInterface1.equals(NetworkInterface
                                        .getByInetAddress(InetAddress
                                                .getByName("127.0.0.1")))) {
                                    sendingInterface = networkInterface2;
                                } else {
                                    sendingInterface = networkInterface1;
                                }
                            } else {
                                // if this interface only seems to support
                                // IPV6 addresses
                                group = InetAddress
                                        .getByName("FF01:0:0:0:0:0:2:8001");
                                sendingInterface = IPV6networkInterface1;
                            }
                        }

                        InetAddress useAddress = null;
                        addresses = sendingInterface.getInetAddresses();
                        if ((addresses != null)
                                && (addresses.hasMoreElements())) {
                            useAddress = (InetAddress) addresses.nextElement();
                        }

                        ports = Support_PortManager.getNextPortsForUDP(2);
                        serverPort = ports[0];
                        groupPort = ports[1];
                        groupSockAddr = new InetSocketAddress(group, serverPort);
                        server = new MulticastServer(groupSockAddr, serverPort,
                                thisInterface);
                        server.start();
                        Thread.sleep(1000);

                        // Now send out a package on interface
                        // networkInterface 1. We should
                        // only see the packet if we send it on interface 1
                        InetSocketAddress theAddress = new InetSocketAddress(
                                useAddress, groupPort);
                        mss = new MulticastSocket(groupPort);
                        mss.setNetworkInterface(sendingInterface);
                        msg = "Hello World - Again" + thisInterface.getName();
                        sdp = new DatagramPacket(msg.getBytes(), msg.length(),
                                group, serverPort);
                        mss.send(sdp);
                        Thread.sleep(1000);
                        if (thisInterface.equals(sendingInterface)) {
                            assertTrue(
                                    "Group member did not recv data when " +
                                    "bound on specific interface: ",
                                    new String(server.rdp.getData(), 0,
                                            server.rdp.getLength()).equals(msg));
                        } else {
                            assertFalse(
                                    "Group member received data on other " +
                                    "interface when only asked for it on one " +
                                    "interface: ",
                                    new String(server.rdp.getData(), 0,
                                            server.rdp.getLength()).equals(msg));
                        }

                        server.stopServer();
                    }
                }

                // validate that we can join the same address on two
                // different interfaces but not on the same interface
                groupPort = Support_PortManager.getNextPortForUDP();
                mss = new MulticastSocket(groupPort);
                mss.joinGroup(groupSockAddr, networkInterface1);
                mss.joinGroup(groupSockAddr, networkInterface2);
                try {
                    mss.joinGroup(groupSockAddr, networkInterface1);
                    fail("Did not get expected exception when joining for " +
                            "second time on same interface");
                } catch (IOException e) {
                }
            }
        }
        System.setSecurityManager(null);
    }

    /**
     * @tests java.net.MulticastSocket#leaveGroup(java.net.InetAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "leaveGroup",
        args = {java.net.InetAddress.class}
    )
    public void test_leaveGroupLjava_net_InetAddress() {
        // Test for method void
        // java.net.MulticastSocket.leaveGroup(java.net.InetAddress)
        String msg = null;
        boolean except = false;
        InetAddress group = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        int groupPort = ports[0];

        try {
            group = InetAddress.getByName("224.0.0.3");
            msg = "Hello World";
            mss = new MulticastSocket(ports[1]);
            DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg
                    .length(), group, groupPort);
            mss.send(sdp, (byte) 10);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
        }
        try {
            // Try to leave s group that mss is not a member of
            mss.leaveGroup(group);
        } catch (java.io.IOException e) {
            // Correct
            except = true;
        }
        assertTrue("Failed to throw exception leaving non-member group", except);
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkMulticast(InetAddress maddr) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            mss.leaveGroup(group);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (IOException e) {
            fail("IOException was thrown.");
            e.printStackTrace();
        } finally {
            System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.net.MulticastSocket#leaveGroup(java.net.SocketAddress,
     * java.net.NetworkInterface)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "leaveGroup",
        args = {java.net.SocketAddress.class, java.net.NetworkInterface.class}
    )
    public void test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface() {
        // security manager that allows us to check that we only return the
        // addresses that we should
        class mySecurityManager extends SecurityManager {
            
            public void checkPermission(Permission p) {
            }

            public void checkMulticast(InetAddress address) {
                throw new SecurityException("not allowed");
            }
        }

        String msg = null;
        InetAddress group = null;
        int groupPort = Support_PortManager.getNextPortForUDP();
        SocketAddress groupSockAddr = null;
        SocketAddress groupSockAddr2 = null;

        try {
            Enumeration theInterfaces = NetworkInterface.getNetworkInterfaces();

            // first validate that we handle a null group ok
            mss = new MulticastSocket(groupPort);
            try {
                mss.leaveGroup(null, null);
                fail("Did not get exception when group was null");
            } catch (IllegalArgumentException e) {
            }

            // now validate we get the expected error if the address specified
            // is not a multicast group
            try {
                group = InetAddress.getByName("255.255.255.255");
                groupSockAddr = new InetSocketAddress(group, groupPort);
                mss.leaveGroup(groupSockAddr, null);
                fail("Did not get exception when group is not a " +
                        "multicast address");
            } catch (IOException e) {
            }

            // now try to leave a group if we are not authorized
            // set the security manager that will make the first address not
            // visible
            System.setSecurityManager(new mySecurityManager());
            try {
                group = InetAddress.getByName("224.0.0.3");
                groupSockAddr = new InetSocketAddress(group, groupPort);
                mss.leaveGroup(groupSockAddr, null);
                fail("Did not get exception when joining group is " +
                        "not allowed");
            } catch (SecurityException e) {
            }
            System.setSecurityManager(null);

            if (atLeastOneInterface) {

                // now test that we can join and leave a group successfully
                groupPort = Support_PortManager.getNextPortForUDP();
                mss = new MulticastSocket(groupPort);
                groupSockAddr = new InetSocketAddress(group, groupPort);
                mss.joinGroup(groupSockAddr, null);
                mss.leaveGroup(groupSockAddr, null);
                try {
                    mss.leaveGroup(groupSockAddr, null);
                    fail(
                            "Did not get exception when trying to leave " +
                            "group that was allready left");
                } catch (IOException e) {
                }

                InetAddress group2 = InetAddress.getByName("224.0.0.4");
                groupSockAddr2 = new InetSocketAddress(group2, groupPort);
                mss.joinGroup(groupSockAddr, networkInterface1);
                try {
                    mss.leaveGroup(groupSockAddr2, networkInterface1);
                    fail(
                            "Did not get exception when trying to leave " +
                            "group that was never joined");
                } catch (IOException e) {
                }

                mss.leaveGroup(groupSockAddr, networkInterface1);
                if (atLeastTwoInterfaces) {
                    mss.joinGroup(groupSockAddr, networkInterface1);
                    try {
                        mss.leaveGroup(groupSockAddr, networkInterface2);
                        fail(
                                "Did not get exception when trying to leave " +
                                "group on wrong interface joined on ["
                                        + networkInterface1
                                        + "] left on ["
                                        + networkInterface2 + "]");
                    } catch (IOException e) {
                    }
                }
            }
        } catch (Exception e) {
            fail("Exception during leaveGroup test: " + e.toString());
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * @tests java.net.MulticastSocket#send(java.net.DatagramPacket, byte)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "send",
        args = {java.net.DatagramPacket.class, byte.class}
    )
    public void test_sendLjava_net_DatagramPacketB() {
        // Test for method void
        // java.net.MulticastSocket.send(java.net.DatagramPacket, byte)

        String msg = "Hello World";
        InetAddress group = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        int groupPort = ports[0];

        try {
            group = InetAddress.getByName("224.0.0.3");
            mss = new MulticastSocket(ports[1]);
            server = new MulticastServer(group, groupPort);
            server.start();
            Thread.sleep(200);
            DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg
                    .length(), group, groupPort);
            mss.send(sdp, (byte) 10);
            Thread.sleep(1000);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
            try {
                mss.close();
            } catch (Exception ex) {
            }
            
            return;
        }
        
        DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg
                .length(), group, groupPort);

        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkConnect(String host,
                    int port) {
                throw new SecurityException();
            }
            
            public void checkMulticast(InetAddress maddr) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            mss.send(sdp);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (IOException e) {
            fail("IOException was thrown.");
            e.printStackTrace();
        } finally {
            System.setSecurityManager(oldSm);
        } 
        
        mss.close();
        
        try {
            mss.send(sdp);
            fail("IOException should be thrown.");
        } catch(IOException ioe) {
            //expected
        }
        
        byte[] data = server.rdp.getData();
        int length = server.rdp.getLength();
        assertTrue("Failed to send data. Received " + length, new String(data,
                0, length).equals(msg));
    }

    /**
     * @tests java.net.MulticastSocket#setInterface(java.net.InetAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setInterface",
        args = {java.net.InetAddress.class}
    )
    public void test_setInterfaceLjava_net_InetAddress() {
        // Test for method void
        // java.net.MulticastSocket.setInterface(java.net.InetAddress)
        // Note that the machine is not multi-homed

        try {
            mss = new MulticastSocket();
            mss.setInterface(InetAddress.getLocalHost());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST_INTERFACE);
        } catch (Exception e) {
            //handleException(e, SO_MULTICAST_INTERFACE);
            return;
        }
        try {
            InetAddress theInterface = mss.getInterface();
            // under IPV6 we are not guarrenteed to get the same address back as
            // the address, all we should be guaranteed is that we get an
            // address on the same interface
            if (theInterface instanceof Inet6Address) {
                assertTrue(
                        "Failed to return correct interface IPV6",
                        NetworkInterface
                                .getByInetAddress(mss.getInterface())
                                .equals(
                                        NetworkInterface
                                                .getByInetAddress(theInterface)));
            } else {
                assertTrue("Failed to return correct interface IPV4 got:"
                        + mss.getInterface() + " excpeted: "
                        + InetAddress.getLocalHost(), mss.getInterface()
                        .equals(InetAddress.getLocalHost()));
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (SocketException e) {
            handleException(e, SO_MULTICAST);
        } catch (UnknownHostException e) {
            fail("Exception during setInterface test: " + e.toString());
        }

        // Regression test for Harmony-2410
        try {
            mss = new MulticastSocket();
            mss.setInterface(InetAddress.getByName("224.0.0.5"));
        } catch (UnknownHostException uhe) {
            fail("Unable to get InetAddress by name from '224.0.0.5' addr: " 
                    + uhe.toString());
        } catch (SocketException se) {
            // expected
        } catch (IOException ioe) {
            handleException(ioe, SO_MULTICAST_INTERFACE);
            return;
        }
    }

    /**
     * @throws IOException 
     * @throws InterruptedException 
     * @tests java.net.MulticastSocket#setNetworkInterface(
     *                                              java.net.NetworkInterface)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setNetworkInterface",
        args = {java.net.NetworkInterface.class}
    )
    @KnownFailure("No interfaces if there's no debugger connected")
    public void test_setNetworkInterfaceLjava_net_NetworkInterface() 
                                    throws IOException, InterruptedException {
        String msg = null;
        InetAddress group = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        int groupPort = ports[0];
        int serverPort = ports[1];
        if (atLeastOneInterface) {
            // validate that null interface is handled ok
            mss = new MulticastSocket(groupPort);

            // this should through a socket exception to be compatible
            try {
                mss.setNetworkInterface(null);
                fail("No socket exception when we set then network " +
                        "interface with NULL");
            } catch (SocketException ex) {
            }

            // validate that we can get and set the interface
            groupPort = Support_PortManager.getNextPortForUDP();
            mss = new MulticastSocket(groupPort);
            mss.setNetworkInterface(networkInterface1);
            assertTrue(
                    "Interface did not seem to be set by setNeworkInterface",
                    networkInterface1.equals(mss.getNetworkInterface()));

            // set up the server and join the group
            group = InetAddress.getByName("224.0.0.3");

            Enumeration theInterfaces = NetworkInterface.getNetworkInterfaces();
            while (theInterfaces.hasMoreElements()) {
                NetworkInterface thisInterface = (NetworkInterface) theInterfaces
                        .nextElement();
                if (thisInterface.getInetAddresses() != null
                        && thisInterface.getInetAddresses().hasMoreElements()) {
                    if ((!((InetAddress) thisInterface.getInetAddresses()
                            .nextElement()).isLoopbackAddress())
                            &&
                            // for windows we cannot use these pseudo
                            // interfaces for the test as the packets still
                            // come from the actual interface, not the
                            // Pseudo interface that was set
                            (Support_NetworkInterface
                                    .useInterface(thisInterface) == true)) {
                        ports = Support_PortManager.getNextPortsForUDP(2);
                        serverPort = ports[0];
                        server = new MulticastServer(group, serverPort);
                        server.start();
                        // give the server some time to start up
                        Thread.sleep(1000);

                        // Send the packets on a particular interface. The
                        // source address in the received packet
                        // should be one of the addresses for the interface
                        // set
                        groupPort = ports[1];
                        mss = new MulticastSocket(groupPort);
                        mss.setNetworkInterface(thisInterface);
                        msg = thisInterface.getName();
                        byte theBytes[] = msg.getBytes();
                        DatagramPacket sdp = new DatagramPacket(theBytes,
                                theBytes.length, group, serverPort);
                        mss.send(sdp);
                        Thread.sleep(1000);
                        String receivedMessage = new String(server.rdp
                                .getData(), 0, server.rdp.getLength());
                        assertTrue(
                                "Group member did not recv data when send on " +
                                "a specific interface: ",
                                receivedMessage.equals(msg));
                        assertTrue(
                                "Datagram was not received from expected " +
                                "interface expected["
                                        + thisInterface
                                        + "] got ["
                                        + NetworkInterface
                                                .getByInetAddress(server.rdp
                                                        .getAddress()) + "]",
                                NetworkInterface.getByInetAddress(
                                        server.rdp.getAddress()).equals(
                                        thisInterface));

                        // stop the server
                        server.stopServer();
                    }
                }
            }
        }
    }

    /**
     * @tests java.net.MulticastSocket#setTimeToLive(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTimeToLive",
        args = {int.class}
    )
    public void test_setTimeToLiveI() {
        try {
            mss = new MulticastSocket();
            mss.setTimeToLive(120);
            assertTrue("Returned incorrect 1st TTL: " + mss.getTimeToLive(),
                    mss.getTimeToLive() == 120);
            mss.setTimeToLive(220);
            assertTrue("Returned incorrect 2nd TTL: " + mss.getTimeToLive(),
                    mss.getTimeToLive() == 220);
            mss.close();
            try {
                mss.setTimeToLive(1);
                fail("IOException was not thrown.");
            }catch(IOException ioe) {
                //expected
            } 
            
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
        }
    }

    /**
     * @tests java.net.MulticastSocket#setTTL(byte)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTTL",
        args = {byte.class}
    )
    public void test_setTTLB() {
        // Test for method void java.net.MulticastSocket.setTTL(byte)
        try {
            mss = new MulticastSocket();
            mss.setTTL((byte) 120);
            assertTrue("Failed to set TTL: " + mss.getTTL(),
                    mss.getTTL() == 120);
            
            mss.close();
            try {
                mss.setTTL((byte) 1);
                fail("IOException was not thrown.");
            } catch(IOException ioe) {
                //expected
            } 
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_MULTICAST);
        } catch (Exception e) {
            handleException(e, SO_MULTICAST);
        }
    }

    /**
     * @tests java.net.MulticastSocket#MulticastSocket(java.net.SocketAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "MulticastSocket",
        args = {java.net.SocketAddress.class}
    )
    public void test_ConstructorLjava_net_SocketAddress() throws Exception {    
        MulticastSocket ms = new MulticastSocket((SocketAddress) null);
        assertTrue("should not be bound", !ms.isBound() && !ms.isClosed()
                && !ms.isConnected());
        ms.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                Support_PortManager.getNextPortForUDP()));
        assertTrue("should be bound", ms.isBound() && !ms.isClosed()
                && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        ms = new MulticastSocket(new InetSocketAddress(InetAddress
                .getLocalHost(), Support_PortManager.getNextPortForUDP()));
        assertTrue("should be bound", ms.isBound() && !ms.isClosed()
                && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        ms = new MulticastSocket(new InetSocketAddress("localhost",
                Support_PortManager.getNextPortForUDP()));
        assertTrue("should be bound", ms.isBound() && !ms.isClosed()
                && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        boolean exception = false;
        try {
            ms = new MulticastSocket(new InetSocketAddress("unresolvedname",
                    Support_PortManager.getNextPortForUDP()));
        } catch (IOException e) {
            exception = true;
        }
        assertTrue("Expected IOException", exception);

        // regression test for Harmony-1162
        InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 0);
        MulticastSocket s = new MulticastSocket(addr);
        assertTrue(s.getReuseAddress()); 
        
        InetSocketAddress isa = new InetSocketAddress(InetAddress
                .getLocalHost(), Support_PortManager.getNextPortForUDP());
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkListen(int port) {
                throw new SecurityException();
            }
        };
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new MulticastSocket(isa);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (IOException e) {
            fail("IOException was thrown.");
            e.printStackTrace();
        } finally {
            System.setSecurityManager(oldSm);
        }  
    }

    /**
     * @tests java.net.MulticastSocket#getLoopbackMode()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLoopbackMode",
        args = {}
    )
    public void test_getLoopbackMode() {
        try {
            MulticastSocket ms = new MulticastSocket((SocketAddress) null);
            assertTrue("should not be bound", !ms.isBound() && !ms.isClosed()
                    && !ms.isConnected());
            ms.getLoopbackMode();
            assertTrue("should not be bound", !ms.isBound() && !ms.isClosed()
                    && !ms.isConnected());
            ms.close();
            assertTrue("should be closed", ms.isClosed());
            try {
                ms.getLoopbackMode();
                fail("SocketException was not thrown.");
            } catch(SocketException ioe) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_USELOOPBACK);
        } catch (IOException e) {
            handleException(e, SO_USELOOPBACK);
        }
    }

    /**
     * @tests java.net.MulticastSocket#setLoopbackMode(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setLoopbackMode",
        args = {boolean.class}
    )
    public void test_setLoopbackModeZ() {
        try {
            MulticastSocket ms = new MulticastSocket();
            ms.setLoopbackMode(true);
            assertTrue("loopback should be true", ms.getLoopbackMode());
            ms.setLoopbackMode(false);
            assertTrue("loopback should be false", !ms.getLoopbackMode());
            ms.close();
            assertTrue("should be closed", ms.isClosed());
            
            try {
                ms.setLoopbackMode(true);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_USELOOPBACK);
        } catch (IOException e) {
            handleException(e, SO_USELOOPBACK);
        }
    }
    
    /**
     * @tests java.net.MulticastSocket#setLoopbackMode(boolean)
     */
    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "SocketException checking missed",
        method = "setLoopbackMode",
        args = {boolean.class}
    )
    public void test_setLoopbackModeSendReceive() throws IOException{
        final String ADDRESS = "224.1.2.3";
        final int PORT = Support_PortManager.getNextPortForUDP();
        final String message = "Hello, world!";

        // test send receive
        MulticastSocket socket = null;
        try {
            // open a multicast socket
            socket = new MulticastSocket(PORT);
            socket.setLoopbackMode(false); // false indecates doing loop back
            socket.joinGroup(InetAddress.getByName(ADDRESS));

            // send the datagram
            byte[] sendData = message.getBytes();
            DatagramPacket sendDatagram = new DatagramPacket(sendData, 0,
                    sendData.length, new InetSocketAddress(InetAddress
                            .getByName(ADDRESS), PORT));
            socket.send(sendDatagram);

            // receive the datagram
            byte[] recvData = new byte[100];
            DatagramPacket recvDatagram = new DatagramPacket(recvData,
                    recvData.length);
            socket.setSoTimeout(5000); // prevent eternal block in
            // socket.receive()
            socket.receive(recvDatagram);
            String recvMessage = new String(recvData, 0, recvDatagram
                    .getLength());
            assertEquals(message, recvMessage);
        } finally {
            if (socket != null)
                socket.close();
        }
    }
    
    
    /**
     * @tests java.net.MulticastSocket#setReuseAddress(boolean)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "SocketException checking missesd. Method inherited from " +
                "class java.net.DatagramSocket",
        method = "setReuseAddress",
        args = {boolean.class}
    )
    public void test_setReuseAddressZ() {
        try {
            // test case were we set it to false
            MulticastSocket theSocket1 = null;
            MulticastSocket theSocket2 = null;
            try {
                InetSocketAddress theAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(), Support_PortManager
                                .getNextPortForUDP());
                theSocket1 = new MulticastSocket(null);
                theSocket2 = new MulticastSocket(null);
                theSocket1.setReuseAddress(false);
                theSocket2.setReuseAddress(false);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
                fail(
                        "No exception when trying to connect to do " +
                        "duplicate socket bind with re-useaddr set to false");
            } catch (BindException e) {

            }
            if (theSocket1 != null)
                theSocket1.close();
            if (theSocket2 != null)
                theSocket2.close();

            // test case were we set it to true
            try {
                InetSocketAddress theAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(), Support_PortManager
                                .getNextPortForUDP());
                theSocket1 = new MulticastSocket(null);
                theSocket2 = new MulticastSocket(null);
                theSocket1.setReuseAddress(true);
                theSocket2.setReuseAddress(true);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
            } catch (Exception e) {
                fail(
                        "unexpected exception when trying to connect " +
                        "to do duplicate socket bind with re-useaddr set " +
                        "to true");
            }
            if (theSocket1 != null)
                theSocket1.close();
            if (theSocket2 != null)
                theSocket2.close();

            // test the default case which we expect to be the same on all
            // platforms
            try {
                InetSocketAddress theAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(), Support_PortManager
                                .getNextPortForUDP());
                theSocket1 = new MulticastSocket(null);
                theSocket2 = new MulticastSocket(null);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
            } catch (BindException e) {
                fail(
                        "unexpected exception when trying to connect to do " +
                        "duplicate socket bind with re-useaddr left as default");
            }
            if (theSocket1 != null)
                theSocket1.close();
            if (theSocket2 != null)
                theSocket2.close();
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_REUSEADDR);
        } catch (Exception e) {
            handleException(e, SO_REUSEADDR);
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {

        Enumeration theInterfaces = null;
        try {
            theInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (Exception e) {
        }

        // only consider interfaces that have addresses associated with them.
        // Otherwise tests don't work so well
        if (theInterfaces != null) {

            atLeastOneInterface = false;
            while (theInterfaces.hasMoreElements()
                    && (atLeastOneInterface == false)) {
                networkInterface1 = (NetworkInterface) theInterfaces
                        .nextElement();
                if ((networkInterface1.getInetAddresses() != null)
                        && networkInterface1.getInetAddresses()
                                .hasMoreElements() &&
                        // we only want real interfaces
                        (Support_NetworkInterface
                                .useInterface(networkInterface1) == true)) {
                    atLeastOneInterface = true;
                }
            }

            atLeastTwoInterfaces = false;
            if (theInterfaces.hasMoreElements()) {
                while (theInterfaces.hasMoreElements()
                        && (atLeastTwoInterfaces == false)) {
                    networkInterface2 = (NetworkInterface) theInterfaces
                            .nextElement();
                    if ((networkInterface2.getInetAddresses() != null)
                            && networkInterface2.getInetAddresses()
                                    .hasMoreElements() &&
                            // we only want real interfaces
                            (Support_NetworkInterface
                                    .useInterface(networkInterface2) == true)) {
                        atLeastTwoInterfaces = true;
                    }
                }
            }

            Enumeration addresses;

            // first the first interface that supports IPV6 if one exists
            try {
                theInterfaces = NetworkInterface.getNetworkInterfaces();
            } catch (Exception e) {
            }
            boolean found = false;
            while (theInterfaces.hasMoreElements() && !found) {
                NetworkInterface nextInterface = (NetworkInterface) theInterfaces
                        .nextElement();
                addresses = nextInterface.getInetAddresses();
                if (addresses != null) {
                    while (addresses.hasMoreElements()) {
                        InetAddress nextAddress = (InetAddress) addresses
                                .nextElement();
                        if (nextAddress instanceof Inet6Address) {
                            IPV6networkInterface1 = nextInterface;
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {

        if (t != null)
            t.interrupt();
        if (mss != null)
            mss.close();
        if (server != null)
            server.stopServer();
    }
}
