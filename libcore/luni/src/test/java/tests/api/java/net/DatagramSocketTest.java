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
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.DatagramSocketImplFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.security.Permission;
import java.util.Date;
import java.util.Vector;

import tests.support.Support_Configuration;
import tests.support.Support_PortManager;

@TestTargetClass(DatagramSocket.class) 
public class DatagramSocketTest extends SocketTestCase {

    java.net.DatagramSocket ds;

    java.net.DatagramPacket dp;

    DatagramSocket sds = null;

    String retval;

    String testString = "Test String";

    boolean interrupted;

    class DatagramServer extends Thread {

        public DatagramSocket ms;

        boolean running = true;

        public volatile byte[] rbuf = new byte[512];

        volatile DatagramPacket rdp = null;

        public void run() {
            try {
                while (running) {
                    try {
                        ms.receive(rdp);
                        // echo the packet back
                        ms.send(rdp);
                    } catch (java.io.InterruptedIOException e) {
                        Thread.yield();
                    }
                    ;
                }
                ;
            } catch (java.io.IOException e) {
                System.out.println("DatagramServer server failed: " + e);
            } finally {
                ms.close();
            }
        }

        public void stopServer() {
            running = false;
        }

        public DatagramServer(int aPort, InetAddress address)
                throws java.io.IOException {
            rbuf = new byte[512];
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new DatagramSocket(aPort, address);
            ms.setSoTimeout(2000);
        }
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "SocketException checking missed.",
        method = "DatagramSocket",
        args = {}
    )
    public void test_Constructor() {
        // Test for method java.net.DatagramSocket()
        try {
            ds = new java.net.DatagramSocket();
        } catch (Exception e) {
            fail("Could not create DatagramSocket : " + e.getMessage());
        }

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
            new DatagramSocket();
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "DatagramSocket",
        args = {int.class}
    )
    @KnownFailure("New DatagramSocket(1) doesn't throw an expected exception.")
    public void test_ConstructorI() {
        // Test for method java.net.DatagramSocket(int)
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            assertTrue("Created socket with incorrect port",
                    ds.getLocalPort() == portNumber);
        } catch (Exception e) {
            fail("Could not create DatagramSocket : " + e.getMessage());
        }
        
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
            new DatagramSocket(8080);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } finally {
            System.setSecurityManager(oldSm);
        }        
        
        try {
            DatagramSocket ds = new java.net.DatagramSocket(1);
            fail("SocketException was not thrown.");
        } catch (SocketException e) {
            //expected
        }
        
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "DatagramSocket",
        args = {int.class, java.net.InetAddress.class}
    )
    public void test_ConstructorILjava_net_InetAddress() {
        // Test for method java.net.DatagramSocket(int, java.net.InetAddress)
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber, InetAddress
                    .getLocalHost());
            assertTrue("Created socket with incorrect port",
                    ds.getLocalPort() == portNumber);
            assertTrue("Created socket with incorrect address", ds
                    .getLocalAddress().equals(InetAddress.getLocalHost()));
        } catch (Exception e) {
            fail("Could not create DatagramSocket : " + e.getMessage());
        }
        
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
            new java.net.DatagramSocket(8080, InetAddress
                    .getLocalHost());
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        } finally {
            System.setSecurityManager(oldSm);
        }   
        
        try {
            new java.net.DatagramSocket(1, InetAddress
                    .getLocalHost());
            fail("SocketException was not thrown.");
        } catch(SocketException se) {
            //expected
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        }
    }

    /**
     * @tests java.net.DatagramSocket#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close() {
        // Test for method void java.net.DatagramSocket.close()
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            dp = new DatagramPacket("Test String".getBytes(), 11, InetAddress
                    .getLocalHost(), 0);
            ds.close();
            try {
                ds.send(dp);
                fail("IOException was not thrown.");
            } catch(IOException ioe) {
                //expected
            }
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * @tests java.net.DatagramSocket#connect(java.net.InetAddress, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "connect",
        args = {java.net.InetAddress.class, int.class}
    )
    public void test_connectLjava_net_InetAddressI() {
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            assertTrue("Incorrect InetAddress", ds.getInetAddress().equals(
                    inetAddress));
            assertTrue("Incorrect Port", ds.getPort() == portNumber);
            ds.disconnect();
        } catch (Exception e) {
            fail("Exception during test : " + e.getMessage());
        }

        if ("true".equals(System.getProperty("run.ipv6tests"))) {
            System.out
                    .println("Running test_connectLjava_net_InetAddressI" +
                            "(DatagramSocketTest) with IPv6GlobalAddressJcl4: "
                            + Support_Configuration.IPv6GlobalAddressJcl4);
            try {
                ds = new java.net.DatagramSocket();
                InetAddress inetAddress = InetAddress
                        .getByName(Support_Configuration.IPv6GlobalAddressJcl4);
                int portNumber = Support_PortManager.getNextPortForUDP();
                ds.connect(inetAddress, portNumber);
                assertTrue("Incorrect InetAddress", ds.getInetAddress().equals(
                        inetAddress));
                assertTrue("Incorrect Port", ds.getPort() == portNumber);
                ds.disconnect();
            } catch (Exception e) {
                fail("Exception during test : " + e.getMessage());
            }
        }

        try {
            // Create a connected datagram socket to test
            // PlainDatagramSocketImpl.peek()
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket();
            int port = ds.getLocalPort();
            ds.connect(localHost, port);
            DatagramPacket send = new DatagramPacket(new byte[10], 10,
                    localHost, port);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("Wrong size: " + receive.getLength(), receive
                    .getLength() == 10);
            assertTrue("Wrong receiver", receive.getAddress().equals(localHost));
        } catch (IOException e) {
            fail("Unexpected IOException : " + e.getMessage());
        }

        class DatagramServer extends Thread {

            public DatagramSocket ms;

            boolean running = true;

            public byte[] rbuf = new byte[512];

            DatagramPacket rdp = null;

            public void run() {
                try {
                    while (running) {
                        try {
                            ms.receive(rdp);
                            // echo the packet back
                            ms.send(rdp);
                        } catch (java.io.InterruptedIOException e) {
                            Thread.yield();
                        }
                        
                    }
                    
                } catch (java.io.IOException e) {
                    System.out.println("Multicast server failed: " + e);
                } finally {
                    ms.close();
                }
            }

            public void stopServer() {
                running = false;
            }

            public DatagramServer(int aPort, InetAddress address)
                    throws java.io.IOException {
                rbuf = new byte[512];
                rbuf[0] = -1;
                rdp = new DatagramPacket(rbuf, rbuf.length);
                ms = new DatagramSocket(aPort, address);
                ms.setSoTimeout(2000);
            }
        }

        // validate that we get the PortUnreachable exception if we try to
        // send a dgram to a server that is not running and then do a recv
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            DatagramPacket send = new DatagramPacket(new byte[10], 10);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(10000);
            ds.receive(receive);
            ds.close();
            fail(
                    "No PortUnreachableException when connected at native level on recv ");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to connect at native level on recv: "
                            + e.toString(),
                    (e instanceof PortUnreachableException));
        }

        // validate that we can send/receive with datagram sockets connected at
        // the native level
        DatagramServer server = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(localHost, serverPortNumber);

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("Wrong size data received: " + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("Wrong receiver:" + receive.getAddress() + ":"
                    + localHost, receive.getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can disconnect
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            ds.disconnect();
            ds.close();
        } catch (Exception e) {
            assertTrue("Unexpected exception when trying to connect at native"
                    + e.toString(), (e instanceof PortUnreachableException));
        }

        // validate that once connected we cannot send to another address
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            DatagramPacket send = new DatagramPacket(new byte[10], 10,
                    inetAddress, portNumber + 1);
            ds.send(send);
            ds.close();
            fail(
                    "No Exception when trying to send to a different address on a connected socket ");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to send to a different address on a connected socket: "
                            + e.toString(),
                    (e instanceof IllegalArgumentException));
        }

        // validate that we can connect, then disconnect, then connect then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(localHost, serverPortNumber + 1);
            ds.disconnect();
            ds.connect(localHost, serverPortNumber);

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue(
                    "connect/disconnect/connect - Wrong size data received: "
                            + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/disconnect/connect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/disconnect/connect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/disconnect/connect:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect/disconnect then send/recv to any address
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(localHost, serverPortNumber + 1);
            ds.disconnect();

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length, localHost, serverPortNumber);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("connect/disconnect - Wrong size data received: "
                    + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/disconnect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/disconnect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/disconnect:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect on an allready connected socket and then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(localHost, serverPortNumber + 1);
            ds.connect(localHost, serverPortNumber);

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("connect/connect - Wrong size data received: "
                    + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/connect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/connect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/connect: "
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // test for when we fail to connect at the native level. Even though we
        // fail at the native level there is no way to return an exception so
        // there should be no exception
        try {
            ds = new java.net.DatagramSocket();
            byte[] addressBytes = { 0, 0, 0, 0 };
            InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
        } catch (Exception e) {
            fail(
                    "Unexcpected exception when trying to connect at native level with bad address for signature with no exception to be returned: "
                            + e.toString());
        }

        if ("true".equals(System.getProperty("run.ipv6tests"))) {
            System.out
                    .println("Running test_connectLjava_net_InetAddressI(DatagramSocketTest) with IPv6 address");
            try {
                ds = new java.net.DatagramSocket();
                byte[] addressBytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0 };
                InetAddress inetAddress = InetAddress
                        .getByAddress(addressBytes);
                int portNumber = Support_PortManager.getNextPortForUDP();
                ds.connect(inetAddress, portNumber);
            } catch (Exception e) {
                fail(
                        "Unexcpected exception when trying to connect at native level with bad IPv6 address for signature with no exception to be returned: "
                                + e.toString());
            }
        }
        
        try {
            byte[] addressBytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0 };
            ds = new java.net.DatagramSocket();
            int portNumber = Support_PortManager.getNextPortForUDP();

            final InetAddress inetAddress = InetAddress   
                .getByAddress(addressBytes);
        
            SecurityManager sm = new SecurityManager() {

                public void checkPermission(Permission perm) {
                    if(perm.getActions().equals("connect,resolve") ) {
                        throw new SecurityException();
                    }
                }
          
                public void checkListen(int port) {
                    throw new SecurityException();
                }
            };

            SecurityManager oldSm = System.getSecurityManager();
            System.setSecurityManager(sm);
            try {
                ds.connect(inetAddress, portNumber);
                fail("SecurityException should be thrown.");
            } catch (SecurityException e) {
                // expected
            } finally {
                System.setSecurityManager(oldSm);
            } 
        
        } catch(Exception e) {
            fail("Unexpected exception was thrown:" + e.getClass() + ", " 
                    + e.getMessage());
        }
    }

    /**
     * @tests java.net.DatagramSocket#disconnect()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "disconnect",
        args = {}
    )
    public void test_disconnect() {
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            ds.disconnect();
            assertNull("Incorrect InetAddress", ds.getInetAddress());
            assertEquals("Incorrect Port", -1, ds.getPort());
        } catch (Exception e) {
            fail("Exception during test : " + e.getMessage());
        }

        if ("true".equals(System.getProperty("run.ipv6tests"))) {
            System.out
                    .println("Running test_disconnect(DatagramSocketTest) with IPv6GlobalAddressJcl4: "
                            + Support_Configuration.IPv6GlobalAddressJcl4);
            try {
                ds = new java.net.DatagramSocket();
                InetAddress inetAddress = InetAddress
                        .getByName(Support_Configuration.IPv6GlobalAddressJcl4);
                int portNumber = Support_PortManager.getNextPortForUDP();
                ds.connect(inetAddress, portNumber);
                ds.disconnect();
                assertNull("Incorrect InetAddress", ds.getInetAddress());
                assertEquals("Incorrect Port", -1, ds.getPort());
            } catch (Exception e) {
                fail("Exception during test : " + e.getMessage());
            }
        }

    }

    /**
     * @tests java.net.DatagramSocket#getInetAddress()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInetAddress",
        args = {}
    )
    public void test_getInetAddress() {
        Vector<InetAddress> ias = new Vector<InetAddress>();
        try {
            ias.add(InetAddress.getLocalHost());
            ias.add(InetAddress
                       .getByName(Support_Configuration.IPv6GlobalAddressJcl4));
            ias.add(InetAddress
                       .getByName(Support_Configuration.InetTestAddress2));
            ias.add(InetAddress
                    .getByName(Support_Configuration.InetTestAddress2));
            ias.add(InetAddress
                    .getByName(Support_Configuration.InetTestIP));
        } catch(Exception e) {
            fail("Unexpected exception was thrown: " + e.toString());
        }
        
        for(InetAddress ia:ias) {
            int portNumber = Support_PortManager.getNextPortForUDP();
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket();
                ds.connect(ia, portNumber);
                assertEquals(ia, ds.getInetAddress());
                assertEquals("" + ia, ia, ds.getInetAddress());
            } catch (SocketException e) {
                fail("SocketException was thrown.");
            } finally {
                ds.disconnect();
                ds.close();
            }
        }
        
        try {
            assertNull(new DatagramSocket().getInetAddress());
         } catch (SocketException e) {
             fail("SocketException was thrown.");
         }

    }

    /**
     * @tests java.net.DatagramSocket#getLocalAddress()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLocalAddress",
        args = {}
    )
    public void test_getLocalAddress() {
        // Test for method java.net.InetAddress
        // java.net.DatagramSocket.getLocalAddress()
        InetAddress local = null;
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            local = InetAddress.getLocalHost();
            ds = new java.net.DatagramSocket(portNumber, local);
            assertTrue("Returned incorrect address. Got:"
                    + ds.getLocalAddress()
                    + " wanted: "
                    + InetAddress.getByName(InetAddress.getLocalHost()
                            .getHostName()), InetAddress.getByName(
                    InetAddress.getLocalHost().getHostName()).equals(
                    ds.getLocalAddress()));

            // now validate thet behaviour when the any address is returned
            String preferIPv4StackValue = System
                    .getProperty("java.net.preferIPv4Stack");
            String preferIPv6AddressesValue = System
                    .getProperty("java.net.preferIPv6Addresses");
            DatagramSocket s = new DatagramSocket(0);
            if (((preferIPv4StackValue == null) || preferIPv4StackValue
                    .equalsIgnoreCase("false"))
                    && (preferIPv6AddressesValue != null)
                    && (preferIPv6AddressesValue.equals("true"))) {
                assertTrue(
                        "ANY address not returned correctly (getLocalAddress) with preferIPv6Addresses=true, preferIPv4Stack=false "
                                + s.getLocalSocketAddress(), s
                                .getLocalAddress() instanceof Inet6Address);
            } else {
                assertTrue(
                        "ANY address not returned correctly (getLocalAddress) with preferIPv6Addresses=true, preferIPv4Stack=true "
                                + s.getLocalSocketAddress(), s
                                .getLocalAddress() instanceof Inet4Address);
            }
            s.close();
        } catch (Exception e) {
            fail(
                    "Exception during getLocalAddress: " + local + " - " + e);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getLocalPort()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLocalPort",
        args = {}
    )
    public void test_getLocalPort() {
        // Test for method int java.net.DatagramSocket.getLocalPort()
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            assertTrue("Returned incorrect port",
                    ds.getLocalPort() == portNumber);
        } catch (Exception e) {
            fail("Exception during getLocalAddress : " + e.getMessage());
        }
    }

    /**
     * @tests java.net.DatagramSocket#getPort()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPort",
        args = {}
    )
    public void test_getPort() {
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            DatagramSocket theSocket = new DatagramSocket(portNumber);
            assertEquals("Expected -1 for remote port as not connected",
                    -1, theSocket.getPort());

            // now connect the socket and validate that we get the right port
            theSocket.connect(InetAddress.getLocalHost(), portNumber);
            assertTrue("getPort returned wrong value:" + theSocket.getPort()
                    + ":Expected:" + portNumber,
                    theSocket.getPort() == portNumber);
        } catch (Exception e) {
            fail("unexpected exception during getPort test : " + e.getMessage());
        }
    }

    /**
     * @tests java.net.DatagramSocket#getReceiveBufferSize()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getReceiveBufferSize",
        args = {}
    )
    public void test_getReceiveBufferSize() {
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setReceiveBufferSize(130);
            assertTrue("Incorrect buffer size",
                    ds.getReceiveBufferSize() >= 130);
            ds.close();
            try {
                ds.getReceiveBufferSize();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_RCVBUF);
        } catch (Exception e) {
            handleException(e, SO_RCVBUF);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getSendBufferSize()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSendBufferSize",
        args = {}
    )
    public void test_getSendBufferSize() {
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSendBufferSize(134);
            assertTrue("Incorrect buffer size", ds.getSendBufferSize() >= 134);
            ds.close();
            try {
                ds.getSendBufferSize();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_SNDBUF);
        } catch (Exception e) {
            handleException(e, SO_SNDBUF);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getSoTimeout()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSoTimeout",
        args = {}
    )
    public void test_getSoTimeout() {
        // Test for method int java.net.DatagramSocket.getSoTimeout()
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(100);
            assertEquals("Returned incorrect timeout", 100, ds.getSoTimeout());
            ds.close();
            try {
                ds.getSoTimeout();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }            
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_TIMEOUT);
        } catch (Exception e) {
            handleException(e, SO_TIMEOUT);
        }
    }

    /**
     * @tests java.net.DatagramSocket#receive(java.net.DatagramPacket)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "PortUnreachableException,IOException checking missed.",
        method = "receive",
        args = {java.net.DatagramPacket.class}
    )
    public void test_receiveLjava_net_DatagramPacket() {
        // Test for method void
        // java.net.DatagramSocket.receive(java.net.DatagramPacket)

        receive_oversize_java_net_DatagramPacket();
        final int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGRcv implements Runnable {
            public void run() {
                InetAddress localHost = null;
                try {
                    localHost = InetAddress.getLocalHost();
                    Thread.sleep(1000);
                    DatagramSocket sds = new DatagramSocket(ports[1]);
                    DatagramPacket rdp = new DatagramPacket("Test String"
                            .getBytes(), 11, localHost, portNumber);
                    sds.send(rdp);
                    sds.close();
                } catch (Exception e) {
                    System.err.println("host " + localHost + " port "
                            + portNumber + " failed to send data: " + e);
                    e.printStackTrace();
                }
            }
        }

        try {
            new Thread(new TestDGRcv(), "DGSender").start();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(6000);
            byte rbuf[] = new byte[1000];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            ds.receive(rdp);
            ds.close();
            assertTrue("Send/Receive failed to return correct data: "
                    + new String(rbuf, 0, 11), new String(rbuf, 0, 11)
                    .equals("Test String"));
        } catch (Exception e) {
            System.err.println("Exception during send test: " + e);
            e.printStackTrace();
            fail("port " + portNumber + " Exception during send test: "
                    + e);
        } finally {
            ds.close();
        }
        DatagramSocket socket = null;
        try {
            byte rbuf[] = new byte[1000];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            SocketAddress address = new InetSocketAddress(portNumber);
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            socket = channel.socket();
            socket.receive(rdp);
            fail("IllegalBlockingModeException was not thrown.");
        } catch(IllegalBlockingModeException ibme) {    
            //expected
        } catch(IOException ioe) {
            fail("IOException was thrown: " + ioe.getMessage());
        } finally {
            socket.close();
        }
        
        try {
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(1000);
            byte rbuf[] = new byte[1000];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            ds.receive(rdp);
            fail("SocketTimeoutException was not thrown.");
        } catch(SocketTimeoutException te) {
            //expected
        } catch (Exception e) {
            System.err.println("Exception during send test: " + e);
            e.printStackTrace();
            fail("port " + portNumber + " Exception during send test: "
                    + e);
        } finally {
            ds.close();
        }
        
        

        try {
            interrupted = false;
            final DatagramSocket ds = new DatagramSocket();
            ds.setSoTimeout(12000);
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        ds.receive(new DatagramPacket(new byte[1], 1));
                    } catch (InterruptedIOException e) {
                        interrupted = true;
                    } catch (IOException e) {
                    }
                }
            };
            Thread thread = new Thread(runnable, "DatagramSocket.receive1");
            thread.start();
            try {
                do {
                    Thread.sleep(500);
                } while (!thread.isAlive());
            } catch (InterruptedException e) {
            }
            ds.close();
            int c = 0;
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (interrupted) {
                    fail("received interrupt");
                }
                if (++c > 4) {
                    fail("read call did not exit");
                }
            } while (thread.isAlive());

            interrupted = false;
            int[] ports1 = Support_PortManager.getNextPortsForUDP(2);
            final int portNum = ports[0];
            final DatagramSocket ds2 = new DatagramSocket(ports[1]);
            ds2.setSoTimeout(12000);
            Runnable runnable2 = new Runnable() {
                public void run() {
                    try {
                        ds2.receive(new DatagramPacket(new byte[1], 1,
                                InetAddress.getLocalHost(), portNum));
                    } catch (InterruptedIOException e) {
                        interrupted = true;
                    } catch (IOException e) {
                    }
                }
            };
            Thread thread2 = new Thread(runnable2, "DatagramSocket.receive2");
            thread2.start();
            try {
                do {
                    Thread.sleep(500);
                } while (!thread2.isAlive());
            } catch (InterruptedException e) {
            }
            ds2.close();
            int c2 = 0;
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (interrupted) {
                    fail("receive2 was interrupted");
                }
                if (++c2 > 4) {
                    fail("read2 call did not exit");
                }
            } while (thread2.isAlive());

            interrupted = false;
            DatagramSocket ds3 = new DatagramSocket();
            ds3.setSoTimeout(500);
            Date start = new Date();
            try {
                ds3.receive(new DatagramPacket(new byte[1], 1));
            } catch (InterruptedIOException e) {
                interrupted = true;
            }
            ds3.close();
            assertTrue("receive not interrupted", interrupted);
            int delay = (int) (new Date().getTime() - start.getTime());
            assertTrue("timeout too soon: " + delay, delay >= 490);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e.getMessage());
        }
        
        
    }

    /**
     * @tests java.net.DatagramSocket#send(java.net.DatagramPacket)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "PortUnreachableException,IOExceptions checking missed.",
        method = "send",
        args = {java.net.DatagramPacket.class}
    )
    public void test_sendLjava_net_DatagramPacket() throws Exception {
        // Test for method void
        // java.net.DatagramSocket.send(java.net.DatagramPacket)
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGSend implements Runnable {
            Thread pThread;

            public TestDGSend(Thread t) {
                pThread = t;
            }

            public void run() {
                try {
                    byte[] rbuf = new byte[1000];

                    sds = new DatagramSocket(portNumber);
                    DatagramPacket sdp = new DatagramPacket(rbuf, rbuf.length);
                    sds.setSoTimeout(6000);
                    sds.receive(sdp);
                    retval = new String(rbuf, 0, testString.length());
                    pThread.interrupt();
                } catch (java.io.InterruptedIOException e) {
                    System.out.println("Recv operation timed out");
                    pThread.interrupt();
                    ds.close();
                    return;
                } catch (Exception e) {
                    System.out
                            .println("Failed to establish Dgram server: " + e);
                }
            }
        }
        try {
            new Thread(new TestDGSend(Thread.currentThread()), "DGServer")
                    .start();
            ds = new java.net.DatagramSocket(ports[1]);
            dp = new DatagramPacket(testString.getBytes(), testString.length(),
                    InetAddress.getLocalHost(), portNumber);
            // Wait to allow send to occur
            try {
                Thread.sleep(500);
                ds.send(dp);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                ds.close();
                assertTrue("Incorrect data sent: " + retval, retval
                        .equals(testString));
            }
        } catch (Exception e) {
            fail("Exception during send test : " + e.getMessage());
        } finally {
            ds.close();
        }
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkMulticast(InetAddress maddr) {
                throw new SecurityException();
            }
            
            public void checkConnect(String host,
                    int port) {
                throw new SecurityException();
            }
        };
        try {
            
            ds = new java.net.DatagramSocket(ports[1]);
            dp = new DatagramPacket(testString.getBytes(), testString.length(),
                    InetAddress.getLocalHost(), portNumber);
            
            SecurityManager oldSm = System.getSecurityManager();
            System.setSecurityManager(sm);
            try {
                ds.send(dp);
                fail("SecurityException should be thrown.");
            } catch (SecurityException e) {
                // expected
            } catch (SocketException e) {
                fail("SocketException was thrown.");
            } finally {
                System.setSecurityManager(oldSm);
            }
        } catch(Exception e) {
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
        
        DatagramSocket socket = null;
        try {
            byte rbuf[] = new byte[1000];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            SocketAddress address = new InetSocketAddress(portNumber);
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            socket = channel.socket();
            socket.send(rdp);
            fail("IllegalBlockingModeException was not thrown.");
        } catch(IllegalBlockingModeException ibme) {    
            //expected
        } catch(IOException ioe) {
            fail("IOException was thrown: " + ioe.getMessage());
        } finally {
            socket.close();
        }
        
        //Regression for HARMONY-1118
        class testDatagramSocket extends DatagramSocket {
            public testDatagramSocket(DatagramSocketImpl impl){
               super(impl);
            }
        }
        class testDatagramSocketImpl extends DatagramSocketImpl { 
            protected void create() throws SocketException {} 
            protected void bind(int arg0, InetAddress arg1) throws SocketException {} 
            protected void send(DatagramPacket arg0) throws IOException {} 
            protected int peek(InetAddress arg0) throws IOException { 
                return 0; 
            } 
            protected int peekData(DatagramPacket arg0) throws IOException { 
                return 0; 
            } 
            protected void receive(DatagramPacket arg0) throws IOException {} 
            protected void setTTL(byte arg0) throws IOException {} 
            protected byte getTTL() throws IOException { 
                return 0; 
            } 
            protected void setTimeToLive(int arg0) throws IOException {} 
            protected int getTimeToLive() throws IOException { 
                return 0; 
            } 
            protected void join(InetAddress arg0) throws IOException {} 
            protected void leave(InetAddress arg0) throws IOException {} 
            protected void joinGroup(SocketAddress arg0, NetworkInterface arg1) throws IOException {} 
            protected void leaveGroup(SocketAddress arg0, NetworkInterface arg1) throws IOException {} 
            protected void close() {} 
            public void setOption(int arg0, Object arg1) throws SocketException {} 
            public Object getOption(int arg0) throws SocketException { 
                return null; 
            } 
        } 
        InetSocketAddress sa = InetSocketAddress.createUnresolved("localhost", 0); 
        //no exception expected for next line
        new testDatagramSocket(new testDatagramSocketImpl()).send(new DatagramPacket(new byte[272], 3, sa)); 
        
        // Regression test for Harmony-2938
        InetAddress i = InetAddress.getByName("127.0.0.1");
        DatagramSocket d = new DatagramSocket(0, i);
        try {
            d.send(new DatagramPacket(new byte[] { 1 }, 1));
            fail("should throw NPE.");
        } catch (NullPointerException e) {
            // expected;
        } finally {
            d.close();
        }
    }

    /**
     * @tests java.net.DatagramSocket#setSendBufferSize(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setSendBufferSize",
        args = {int.class}
    )
    public void test_setSendBufferSizeI() {
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSendBufferSize(134);
            assertTrue("Incorrect buffer size", ds.getSendBufferSize() >= 134);
            ds.close();
            try {
                ds.setSendBufferSize(1);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_SNDBUF);
        } catch (Exception e) {
            handleException(e, SO_SNDBUF);
        }
    }

    /**
     * @tests java.net.DatagramSocket#setReceiveBufferSize(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setReceiveBufferSize",
        args = {int.class}
    )
    public void test_setReceiveBufferSizeI() {
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setReceiveBufferSize(130);
            assertTrue("Incorrect buffer size",
                    ds.getReceiveBufferSize() >= 130);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_RCVBUF);
        } catch (Exception e) {
            handleException(e, SO_RCVBUF);
        }
        
        try {
            ds.setReceiveBufferSize(0);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        }
        
        try {
            ds.setReceiveBufferSize(-1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        }
        
        ds.close();
        
        try {
            ds.setReceiveBufferSize(1);
            fail("SocketException was not thrown.");
        } catch(IllegalArgumentException iae) {
            fail("IllegalArgumentException was thrown.");
        } catch (SocketException e) {
            //expected
        }
    }

    /**
     * @tests java.net.DatagramSocket#setSoTimeout(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setSoTimeout",
        args = {int.class}
    )
    public void test_setSoTimeoutI() {
        // Test for method void java.net.DatagramSocket.setSoTimeout(int)
        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(5000);
            assertTrue("Set incorrect timeout", ds.getSoTimeout() >= 5000);
            ds.close();
            try {
                ds.setSoTimeout(100);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_TIMEOUT);
        } catch (Exception e) {
            handleException(e, SO_TIMEOUT);
        }
    }
    /**
     * @tests java.net.DatagramSocket#DatagramSocket(java.net.DatagramSocketImpl)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "DatagramSocket",
        args = {java.net.DatagramSocketImpl.class}
    )
    public void test_ConstructorLjava_net_DatagramSocketImpl() {
        class testDatagramSocket extends DatagramSocket {
            public testDatagramSocket(DatagramSocketImpl impl){
               super(impl);
            }
        }

        try {
            new testDatagramSocket((DatagramSocketImpl) null);
            fail("exception expected");
        } catch (NullPointerException ex) {
            //expected
        }
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(java.net.SocketAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "DatagramSocket",
        args = {java.net.SocketAddress.class}
    )
    public void test_ConstructorLjava_net_SocketAddress() {
        class mySocketAddress extends SocketAddress {

            public mySocketAddress() {
            }
        }

        try {
            try {
                int portNumber = Support_PortManager.getNextPortForUDP();
                ds = new java.net.DatagramSocket(new InetSocketAddress(
                        InetAddress.getLocalHost(), portNumber));
                assertTrue(ds.getBroadcast());
                assertTrue("Created socket with incorrect port", ds
                        .getLocalPort() == portNumber);
                assertTrue("Created socket with incorrect address", ds
                        .getLocalAddress().equals(InetAddress.getLocalHost()));
            } catch (Exception e) {
                fail("Could not create DatagramSocket : " + e.getMessage());
            }

            try {
                int portNumber = Support_PortManager.getNextPortForUDP();
                ds = new java.net.DatagramSocket(new mySocketAddress());
                fail(
                        "No exception when constucting datagramSocket with unsupported SocketAddress type");
            } catch (IllegalArgumentException e) {

            }
            //regression for Harmony-894
            ds = new DatagramSocket((SocketAddress)null);
            assertTrue(ds.getBroadcast());
        } catch (Exception ex) {
            fail(
                    "unexpected exception when datagramSocket SocketAddress constructor test");
        }
        
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
            new DatagramSocket(new InetSocketAddress(
                    InetAddress.getLocalHost(), 1));
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        } finally {
            System.setSecurityManager(oldSm);
        }
        
        InetSocketAddress isa = null;
        try {
            isa = new InetSocketAddress(
                    InetAddress.getLocalHost(), 1);
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        }
        
        try {
            new java.net.DatagramSocket(isa);
            fail("SocketException was thrown.");
        } catch(SocketException se) {
            //expected
        }
    }

    /**
     * @tests java.net.DatagramSocket#bind(java.net.SocketAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "bind",
        args = {java.net.SocketAddress.class}
    )
    public void test_bindLjava_net_SocketAddress() {
        class mySocketAddress extends SocketAddress {

            public mySocketAddress() {
            }
        }

        DatagramServer server = null;
        try {
            // now create a socket that is not bound and then bind it
            int[] ports = Support_PortManager.getNextPortsForUDP(3);
            int portNumber = ports[0];
            int serverPortNumber = ports[1];
            DatagramSocket theSocket = new DatagramSocket(
                    new InetSocketAddress(InetAddress.getLocalHost(),
                            portNumber));

            // validate that the localSocketAddress reflects the address we
            // bound to
            assertTrue("Local address not correct after bind:"
                    + theSocket.getLocalSocketAddress().toString()
                    + "Expected: "
                    + (new InetSocketAddress(InetAddress.getLocalHost(),
                            portNumber)).toString(), theSocket
                    .getLocalSocketAddress().equals(
                            new InetSocketAddress(InetAddress.getLocalHost(),
                                    portNumber)));

            // now make sure that datagrams sent from this socket appear to come
            // from the address we bound to
            InetAddress localHost = InetAddress.getLocalHost();
            portNumber = ports[2];
            DatagramSocket ds = new DatagramSocket(null);
            ds.bind(new InetSocketAddress(localHost, portNumber));

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for bin datagram socket test ");
            }

            ds.connect(new InetSocketAddress(localHost, serverPortNumber));

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            Thread.sleep(1000);
            ds.close();
            assertTrue(
                    "Address in packet sent does not match address bound to:"
                            + server.rdp.getAddress() + ":"
                            + server.rdp.getPort() + ":" + localHost + ":"
                            + portNumber, (server.rdp.getAddress()
                            .equals(localHost))
                            && (server.rdp.getPort() == portNumber));

            // validate if we pass in null that it picks an address for us and
            // all is ok
            theSocket = new DatagramSocket(null);
            theSocket.bind(null);
            assertNotNull("Bind with null did not work", theSocket
                    .getLocalSocketAddress());
            theSocket.close();

            // now check the error conditions

            // Address we cannot bind to
            theSocket = new DatagramSocket(null);
            try {
                theSocket
                        .bind(new InetSocketAddress(
                                InetAddress
                                        .getByAddress(Support_Configuration.nonLocalAddressBytes),
                                Support_PortManager.getNextPortForUDP()));
                fail("No exception when binding to bad address");
            } catch (SocketException ex) {
            }
            theSocket.close();

            // Address that we have allready bound to
            ports = Support_PortManager.getNextPortsForUDP(2);
            theSocket = new DatagramSocket(null);
            DatagramSocket theSocket2 = new DatagramSocket(ports[0]);
            try {
                InetSocketAddress theAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(), ports[1]);
                theSocket.bind(theAddress);
                theSocket2.bind(theAddress);
                fail("No exception binding to address that is not available");
            } catch (SocketException ex) {
                //expected
            }
            theSocket.close();
            theSocket2.close();

            SecurityManager sm = new SecurityManager() {

                public void checkPermission(Permission perm) {
                }
                
                public void checkListen(int port) {
                    throw new SecurityException();
                }
            };

            ports = Support_PortManager.getNextPortsForUDP(2);
            ds = new DatagramSocket(null);
            SecurityManager oldSm = System.getSecurityManager();
            System.setSecurityManager(sm);
            try {

                ds.bind(new InetSocketAddress(localHost, ports[0]));
                fail("SecurityException should be thrown.");
            } catch (SecurityException e) {
                // expected
            } catch (SocketException e) {
                fail("SocketException was thrown.");
            } finally {
                System.setSecurityManager(oldSm);
            }
            
            // unsupported SocketAddress subclass
            theSocket = new DatagramSocket(null);
            try {
                theSocket.bind(new mySocketAddress());
                fail("No exception when binding using unsupported SocketAddress subclass");
            } catch (IllegalArgumentException ex) {
            }
            theSocket.close();

        } catch (Exception e) {
            fail("Unexpected exception during bind test : " + e.getMessage());
        }

        if (server != null) {
            server.stopServer();
        }
    }

    /**
     * @tests java.net.DatagramSocket#connect(java.net.SocketAddress)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "connect",
        args = {java.net.SocketAddress.class}
    )
    public void test_connectLjava_net_SocketAddress() {

        // validate that we get the PortUnreachable exception if we try to
        // send a dgram to a server that is not running and then do a recv
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            DatagramPacket send = new DatagramPacket(new byte[10], 10);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(10000);
            ds.receive(receive);
            ds.close();
            fail(
                    "No PortUnreachableException when connected at native level on recv ");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to connect at native level on recv: "
                            + e.toString(),
                    (e instanceof PortUnreachableException));
        }
        
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            
            ds.connect(new InetSocketAddress("asdfasdf", 1));
            ds.close();
            fail("SocketException was not thrown.");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to connect to unknown host: "
                            + e.toString(),
                    (e instanceof SocketException));
        }        

        // validate that we can send/receive with datagram sockets connected at
        // the native level
        DatagramServer server = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(new InetSocketAddress(localHost, serverPortNumber));

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("Wrong size data received: " + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("Wrong receiver:" + receive.getAddress() + ":"
                    + localHost, receive.getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can disconnect
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            ds.disconnect();
            ds.close();
        } catch (Exception e) {
            assertTrue("Unexpected exception when trying to connect at native"
                    + e.toString(), (e instanceof PortUnreachableException));
        }

        // validate that once connected we cannot send to another address
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            DatagramPacket send = new DatagramPacket(new byte[10], 10,
                    inetAddress, portNumber + 1);
            ds.send(send);
            ds.close();
            fail(
                    "No Exception when trying to send to a different address on a connected socket ");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to send to a different address on a connected socket: "
                            + e.toString(),
                    (e instanceof IllegalArgumentException));
        }

        // validate that we can connect, then disconnect, then connect then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
            ds.disconnect();
            ds.connect(new InetSocketAddress(localHost, serverPortNumber));

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue(
                    "connect/disconnect/connect - Wrong size data received: "
                            + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/disconnect/connect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/disconnect/connect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/disconnect/connect:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect/disconnect then send/recv to any address
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
            ds.disconnect();

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length, localHost, serverPortNumber);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("connect/disconnect - Wrong size data received: "
                    + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/disconnect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/disconnect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/disconnect:"
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect on an allready connected socket and then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket ds = new DatagramSocket(ports[1]);
            DatagramSocket ds2 = new DatagramSocket(ports[2]);

            try {
                server = new DatagramServer(serverPortNumber, localHost);
                server.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                fail(
                        "Failed to set up datagram server for native connected Dgram socket test ");
            }

            int port = ds.getLocalPort();
            ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
            ds.connect(new InetSocketAddress(localHost, serverPortNumber));

            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(2000);
            ds.receive(receive);
            ds.close();
            assertTrue("connect/connect - Wrong size data received: "
                    + receive.getLength(),
                    receive.getLength() == sendBytes.length);
            assertTrue("connect/connect - Wrong data received"
                    + new String(receive.getData(), 0, receive.getLength())
                    + ":" + new String(sendBytes), new String(
                    receive.getData(), 0, receive.getLength())
                    .equals(new String(sendBytes)));
            assertTrue("connect/connect - Wrong receiver:"
                    + receive.getAddress() + ":" + localHost, receive
                    .getAddress().equals(localHost));
        } catch (Exception e) {
            fail(
                    "Unexpected exception when sending data on dgram connected at native level after connect/connect: "
                            + e.toString());
        }

        if (server != null) {
            server.stopServer();
        }

        // test for when we fail to connect at the native level. It seems to
        // fail for the any address so we use this. Now to be compatible we
        // don't throw the exception but eat it and then act as if we were
        // connected at the Java level.
        try {
            ds = new java.net.DatagramSocket();
            byte[] addressBytes = { 0, 0, 0, 0 };
            InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
            int portNumber = Support_PortManager.getNextPortForUDP();
            InetAddress localHost = InetAddress.getLocalHost();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            assertTrue("Is not connected after connect to inaddr any", ds
                    .isConnected());
            byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
            DatagramPacket send = new DatagramPacket(sendBytes,
                    sendBytes.length, localHost, portNumber);
            ds.send(send);
            fail(
                    "No exception when trying to connect at native level with bad address (exception from send)  ");
        } catch (Exception e) {
            assertTrue(
                    "Wrong exception when trying to connect at native level with bad address (exception from send): "
                            + e.toString(),
                    (e instanceof IllegalArgumentException));
        }
    }

    /**
     * @tests java.net.DatagramSocket#isBound()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isBound",
        args = {}
    )
    public void test_isBound() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            int[] ports = Support_PortManager.getNextPortsForUDP(3);
            int port = ports[0];

            DatagramSocket theSocket = new DatagramSocket(ports[1]);
            assertTrue("Socket indicated  not bound when it should be (1)",
                    theSocket.isBound());
            theSocket.close();

            theSocket = new DatagramSocket(new InetSocketAddress(addr, port));
            assertTrue("Socket indicated  not bound when it should be (2)",
                    theSocket.isBound());
            theSocket.close();

            theSocket = new DatagramSocket(null);
            assertFalse("Socket indicated  bound when it should not be (1)",
                    theSocket.isBound());
            theSocket.close();

            // connect causes implicit bind
            theSocket = new DatagramSocket(null);
            theSocket.connect(new InetSocketAddress(addr, port));
            assertTrue("Socket indicated not bound when it should be (3)",
                    theSocket.isBound());
            theSocket.close();

            // now test when we bind explicitely
            InetSocketAddress theLocalAddress = new InetSocketAddress(
                    InetAddress.getLocalHost(), ports[2]);
            theSocket = new DatagramSocket(null);
            assertFalse("Socket indicated bound when it should not be (2)",
                    theSocket.isBound());
            theSocket.bind(theLocalAddress);
            assertTrue("Socket indicated not bound when it should be (4)",
                    theSocket.isBound());
            theSocket.close();
            assertTrue("Socket indicated not bound when it should be (5)",
                    theSocket.isBound());
        } catch (Exception e) {
            fail("Got exception during isBound tests" + e.toString());
        }
    }

    /**
     * @tests java.net.DatagramSocket#isConnected()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isConnected",
        args = {}
    )
    public void test_isConnected() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            int[] ports = Support_PortManager.getNextPortsForUDP(4);
            int port = ports[0];

            // base test
            DatagramSocket theSocket = new DatagramSocket(ports[1]);
            assertFalse("Socket indicated connected when it should not be",
                    theSocket.isConnected());
            theSocket.connect(new InetSocketAddress(addr, port));
            assertTrue("Socket indicated  not connected when it should be",
                    theSocket.isConnected());

            // reconnect the socket and make sure we get the right answer
            theSocket.connect(new InetSocketAddress(addr, ports[2]));
            assertTrue("Socket indicated  not connected when it should be",
                    theSocket.isConnected());

            // now disconnect the socket and make sure we get the right answer
            theSocket.disconnect();
            assertFalse("Socket indicated connected when it should not be",
                    theSocket.isConnected());
            theSocket.close();

            // now check behavior when socket is closed when connected
            theSocket = new DatagramSocket(ports[3]);
            theSocket.connect(new InetSocketAddress(addr, port));
            theSocket.close();
            assertTrue("Socket indicated  not connected when it should be",
                    theSocket.isConnected());
        } catch (Exception e) {
            fail("Got exception during isConnected tests" + e.toString());
        }
    }

    /**
     * @tests java.net.DatagramSocket#getRemoteSocketAddress()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getRemoteSocketAddress",
        args = {}
    )
    public void test_getRemoteSocketAddress() {
        try {
            int[] ports = Support_PortManager.getNextPortsForUDP(3);
            int sport = ports[0];
            int portNumber = ports[1];
            DatagramSocket s = new DatagramSocket(new InetSocketAddress(
                    InetAddress.getLocalHost(), portNumber));
            s.connect(new InetSocketAddress(InetAddress.getLocalHost(), sport));
            assertTrue("Returned incorrect InetSocketAddress(1):"
                    + s.getLocalSocketAddress().toString(), s
                    .getRemoteSocketAddress().equals(
                            new InetSocketAddress(InetAddress.getLocalHost(),
                                    sport)));
            s.close();

            // now create one that is not connected and validate that we get the
            // right answer
            DatagramSocket theSocket = new DatagramSocket(null);
            portNumber = ports[2];
            theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                    portNumber));
            assertNull(
                    "Returned incorrect InetSocketAddress -unconnected socket:"
                            + "Expected: NULL", theSocket
                            .getRemoteSocketAddress());

            // now connect and validate we get the right answer
            theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(),
                    sport));
            assertTrue("Returned incorrect InetSocketAddress(2):"
                    + theSocket.getRemoteSocketAddress().toString(), theSocket
                    .getRemoteSocketAddress().equals(
                            new InetSocketAddress(InetAddress.getLocalHost(),
                                    sport)));
            theSocket.close();

        } catch (Exception e) {
            fail("Exception during getRemoteSocketAddress test: " + e);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getLocalSocketAddress()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLocalSocketAddress",
        args = {}
    )
    public void test_getLocalSocketAddress() {

        try {
            int portNumber = Support_PortManager.getNextPortForUDP();
            DatagramSocket s = new DatagramSocket(new InetSocketAddress(
                    InetAddress.getLocalHost(), portNumber));
            assertTrue("Returned incorrect InetSocketAddress(1):"
                    + s.getLocalSocketAddress().toString()
                    + "Expected: "
                    + (new InetSocketAddress(InetAddress.getLocalHost(),
                            portNumber)).toString(), s.getLocalSocketAddress()
                    .equals(
                            new InetSocketAddress(InetAddress.getLocalHost(),
                                    portNumber)));
            s.close();

            InetSocketAddress remoteAddress = (InetSocketAddress) s
                    .getRemoteSocketAddress();

            // now create a socket that is not bound and validate we get the
            // right answer
            DatagramSocket theSocket = new DatagramSocket(null);
            assertNull(
                    "Returned incorrect InetSocketAddress -unbound socket- Expected null",
                    theSocket.getLocalSocketAddress());

            // now bind the socket and make sure we get the right answer
            portNumber = Support_PortManager.getNextPortForUDP();
            theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                    portNumber));
            assertTrue("Returned incorrect InetSocketAddress(2):"
                    + theSocket.getLocalSocketAddress().toString()
                    + "Expected: "
                    + (new InetSocketAddress(InetAddress.getLocalHost(),
                            portNumber)).toString(), theSocket
                    .getLocalSocketAddress().equals(
                            new InetSocketAddress(InetAddress.getLocalHost(),
                                    portNumber)));
            theSocket.close();

            // now validate thet behaviour when the any address is returned
            s = new DatagramSocket(0);

            String preferIPv4StackValue = System
                    .getProperty("java.net.preferIPv4Stack");
            String preferIPv6AddressesValue = System
                    .getProperty("java.net.preferIPv6Addresses");
            if (((preferIPv4StackValue == null) || preferIPv4StackValue
                    .equalsIgnoreCase("false"))
                    && (preferIPv6AddressesValue != null)
                    && (preferIPv6AddressesValue.equals("true"))) {
                assertTrue(
                        "ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=false "
                                + s.getLocalSocketAddress(),
                        ((InetSocketAddress) s.getLocalSocketAddress())
                                .getAddress() instanceof Inet6Address);
            } else {
                assertTrue(
                        "ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=true "
                                + s.getLocalSocketAddress(),
                        ((InetSocketAddress) s.getLocalSocketAddress())
                                .getAddress() instanceof Inet4Address);
            }
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception during getLocalSocketAddress test: " + e);
        }
    }

    /**
     * @tests java.net.DatagramSocket#setReuseAddress(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setReuseAddress",
        args = {boolean.class}
    )
    public void test_setReuseAddressZ() {
        try {
            // test case were we set it to false
            DatagramSocket theSocket1 = null;
            DatagramSocket theSocket2 = null;
            try {
                InetSocketAddress theAddress = new InetSocketAddress(
                        InetAddress.getLocalHost(), Support_PortManager
                                .getNextPortForUDP());
                theSocket1 = new DatagramSocket(null);
                theSocket2 = new DatagramSocket(null);
                theSocket1.setReuseAddress(false);
                theSocket2.setReuseAddress(false);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
                fail(
                        "No exception when trying to connect to do duplicate socket bind with re-useaddr set to false");
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
                theSocket1 = new DatagramSocket(null);
                theSocket2 = new DatagramSocket(null);
                theSocket1.setReuseAddress(true);
                theSocket2.setReuseAddress(true);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
            } catch (Exception e) {
                fail(
                        "unexpected exception when trying to connect to do duplicate socket bind with re-useaddr set to true");
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
                theSocket1 = new DatagramSocket(null);
                theSocket2 = new DatagramSocket(null);
                theSocket1.bind(theAddress);
                theSocket2.bind(theAddress);
                fail(
                        "No exception when trying to connect to do duplicate socket bind with re-useaddr left as default");
            } catch (BindException e) {

            }
            if (theSocket1 != null)
                theSocket1.close();
            if (theSocket2 != null)
                theSocket2.close();
            
            try {
                theSocket1.setReuseAddress(true);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_REUSEADDR);
        } catch (Exception e) {
            handleException(e, SO_REUSEADDR);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getReuseAddress()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getReuseAddress",
        args = {}
    )
    public void test_getReuseAddress() {
        try {
            DatagramSocket theSocket = new DatagramSocket();
            theSocket.setReuseAddress(true);
            assertTrue("getReuseAddress false when it should be true",
                    theSocket.getReuseAddress());
            theSocket.setReuseAddress(false);
            assertFalse("getReuseAddress true when it should be false",
                    theSocket.getReuseAddress());
            theSocket.close();
            try {
                theSocket.getReuseAddress();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_REUSEADDR);
        } catch (Exception e) {
            handleException(e, SO_REUSEADDR);
        }
    }

    /**
     * @tests java.net.DatagramSocket#setBroadcast(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setBroadcast",
        args = {boolean.class}
    )
    public void test_setBroadcastZ() {

        try {
            int[] ports = Support_PortManager.getNextPortsForUDP(3);
            DatagramSocket theSocket = new DatagramSocket(ports[0]);
            theSocket.setBroadcast(false);
            byte theBytes[] = { -1, -1, -1, -1 };

            // validate we cannot connect to the broadcast address when
            // setBroadcast is false
            try {
                theSocket.connect(new InetSocketAddress(InetAddress
                        .getByAddress(theBytes), ports[1]));
                assertFalse(
                        "No exception when connecting to broadcast address with setBroadcast(false)",
                        theSocket.getBroadcast());
            } catch (SocketException ex) {
                //expected
            }

            // now validate that we can connect to the broadcast address when
            // setBroadcast is true
            theSocket.setBroadcast(true);
            theSocket
                    .connect(new InetSocketAddress(InetAddress
                            .getByAddress(theBytes), ports[2]));
            
            theSocket.close();
            try {
                theSocket.setBroadcast(false);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            } 
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_BROADCAST);
        } catch (Exception e) {
            handleException(e, SO_BROADCAST);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getBroadcast()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getBroadcast",
        args = {}
    )
    public void test_getBroadcast() {
        try {
            DatagramSocket theSocket = new DatagramSocket();
            theSocket.setBroadcast(true);
            assertTrue("getBroadcast false when it should be true", theSocket
                    .getBroadcast());
            theSocket.setBroadcast(false);
            assertFalse("getBroadcast true when it should be False", theSocket
                    .getBroadcast());
            theSocket.close();
            try {
                theSocket.getBroadcast();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_BROADCAST);
        } catch (Exception e) {
            handleException(e, SO_BROADCAST);
        }
    }

    /**
     * @tests java.net.DatagramSocket#setTrafficClass(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTrafficClass",
        args = {int.class}
    )
    public void test_setTrafficClassI() {
        try {
            int IPTOS_LOWCOST = 0x2;
            int IPTOS_RELIABILTY = 0x4;
            int IPTOS_THROUGHPUT = 0x8;
            int IPTOS_LOWDELAY = 0x10;
            int[] ports = Support_PortManager.getNextPortsForUDP(2);

            new InetSocketAddress(InetAddress.getLocalHost(),
                    ports[0]);
            DatagramSocket theSocket = new DatagramSocket(ports[1]);

            // validate that value set must be between 0 and 255
            try {
                theSocket.setTrafficClass(256);
                fail("No exception when traffic class set to 256");
            } catch (IllegalArgumentException e) {
            }

            try {
                theSocket.setTrafficClass(-1);
                fail("No exception when traffic class set to -1");
            } catch (IllegalArgumentException e) {
            }

            // now validate that we can set it to some good values
            theSocket.setTrafficClass(IPTOS_LOWCOST);
            theSocket.setTrafficClass(IPTOS_THROUGHPUT);
            
            theSocket.close();
            try {
                theSocket.setTrafficClass(1);
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            }
           
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(IP_TOS);
        } catch (Exception e) {
            handleException(e, IP_TOS);
        }
    }

    /**
     * @tests java.net.DatagramSocket#getTrafficClass()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getTrafficClass",
        args = {}
    )
    public void test_getTrafficClass() {
        try {
            int IPTOS_LOWCOST = 0x2;
            int IPTOS_RELIABILTY = 0x4;
            int IPTOS_THROUGHPUT = 0x8;
            int IPTOS_LOWDELAY = 0x10;
            int[] ports = Support_PortManager.getNextPortsForUDP(2);

            new InetSocketAddress(InetAddress.getLocalHost(),
                    ports[0]);
            DatagramSocket theSocket = new DatagramSocket(ports[1]);

            /*
             * we cannot actually check that the values are set as if a platform
             * does not support the option then it may come back unset even
             * though we set it so just get the value to make sure we can get it
             */
            int trafficClass = theSocket.getTrafficClass();
            
            theSocket.close();
            try {
                theSocket.getTrafficClass();
                fail("SocketException was not thrown.");
            } catch(SocketException se) {
                //expected
            } 
        } catch (Exception e) {
            handleException(e, IP_TOS);
        }
    }

    /**
     * @tests java.net.DatagramSocket#isClosed()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isClosed",
        args = {}
    )
    public void test_isClosed() {
        try {
            DatagramSocket theSocket = new DatagramSocket();

            // validate isClosed returns expected values
            assertFalse("Socket should indicate it is not closed(1):",
                    theSocket.isClosed());
            theSocket.close();
            assertTrue("Socket should indicate it is not closed(1):", theSocket
                    .isClosed());

            InetSocketAddress theAddress = new InetSocketAddress(InetAddress
                    .getLocalHost(), Support_PortManager.getNextPortForUDP());
            theSocket = new DatagramSocket(theAddress);
            assertFalse("Socket should indicate it is not closed(2):",
                    theSocket.isClosed());
            theSocket.close();
            assertTrue("Socket should indicate it is not closed(2):", theSocket
                    .isClosed());
        } catch (Exception e) {
            fail("Got exception during isClosed tests" + e.toString());
        }
    }
    
    /**
     * @tests java.net.DatagramSocket#getChannel()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getChannel",
        args = {}
    )
    public void test_getChannel() throws Exception {
        assertNull(new DatagramSocket().getChannel());
        
        int portNumber = Support_PortManager.getNextPortForUDP();
        DatagramSocket ds = null;
        try {
            InetAddress ia = InetAddress
                        .getByName(Support_Configuration.IPv6GlobalAddressJcl4);
            ds = new DatagramSocket();
            assertNull(ds.getChannel());
            ds.connect(ia, portNumber);
            assertNull(ds.getChannel());
        } catch (SocketException e) {
            fail("SocketException was thrown.");
        } finally {
            ds.disconnect();
            ds.close();
        }
        portNumber = Support_PortManager.getNextPortForUDP();
        SocketAddress address = new InetSocketAddress(portNumber);
        DatagramChannel channel = DatagramChannel.open();
        DatagramSocket socket = channel.socket();
        assertEquals(channel, socket.getChannel());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies SecurityException.",
        method = "setDatagramSocketImplFactory",
        args = {java.net.DatagramSocketImplFactory.class}
    )
    public void test_setDatagramSocketImplFactory() {
        TestDatagramSocketImplFactory dsf = new TestDatagramSocketImplFactory();
        
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
            DatagramSocket.setDatagramSocketImplFactory(dsf);
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
    
    class TestDatagramSocketImplFactory implements DatagramSocketImplFactory {
        public DatagramSocketImpl createDatagramSocketImpl() {
            return new TestDatagramSocketImpl();
        }
    }
    
    class TestDatagramSocketImpl extends DatagramSocketImpl {

        @Override
        protected void bind(int arg0, InetAddress arg1) throws SocketException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void close() {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void create() throws SocketException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected byte getTTL() throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        protected int getTimeToLive() throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        protected void join(InetAddress arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void joinGroup(SocketAddress arg0, NetworkInterface arg1) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void leave(InetAddress arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void leaveGroup(SocketAddress arg0, NetworkInterface arg1) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected int peek(InetAddress arg0) throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        protected int peekData(DatagramPacket arg0) throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        protected void receive(DatagramPacket arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void send(DatagramPacket arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void setTTL(byte arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void setTimeToLive(int arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }

        public Object getOption(int arg0) throws SocketException {
            // TODO Auto-generated method stub
            return null;
        }

        public void setOption(int arg0, Object arg1) throws SocketException {
            // TODO Auto-generated method stub
            
        }
        
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        retval = "Bogus retval";
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        try {
            ds.close();
            sds.close();
        } catch (Exception e) {
        }
    }

    protected void receive_oversize_java_net_DatagramPacket() {
        final int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGRcvOver implements Runnable {
            public void run() {
                InetAddress localHost = null;
                try {
                    localHost = InetAddress.getLocalHost();
                    Thread.sleep(1000);
                    DatagramSocket sds = new DatagramSocket(ports[1]);
                    DatagramPacket rdp = new DatagramPacket("0123456789"
                            .getBytes(), 10, localHost, portNumber);
                    sds.send(rdp);
                    sds.close();
                } catch (Exception e) {
                    System.err.println("host " + localHost + " port "
                            + portNumber + " failed to send oversize data: "
                            + e);
                    e.printStackTrace();
                }
            }
        }

        try {
            new Thread(new TestDGRcvOver(), "DGSenderOver").start();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(6000);
            byte rbuf[] = new byte[5];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            ;
            ds.receive(rdp);
            ds.close();
            assertTrue("Send/Receive oversize failed to return correct data: "
                    + new String(rbuf, 0, 5), new String(rbuf, 0, 5)
                    .equals("01234"));
        } catch (Exception e) {
            System.err.println("Exception during send test: " + e);
            e.printStackTrace();
            fail("port " + portNumber + " Exception: " + e
                    + " during oversize send test");
        } finally {
            ds.close();
        }
    }
}
