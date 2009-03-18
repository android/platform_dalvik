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

package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.harmony.luni.net.NetUtil;
import org.apache.harmony.luni.platform.INetworkSystem;
import org.apache.harmony.luni.platform.Platform;
import org.apache.harmony.luni.util.Inet6Util;
import org.apache.harmony.luni.util.Msg;
import org.apache.harmony.luni.util.PriviAction;

/**
 * The Internet Protocol (IP) address representation class. This class
 * encapsulates an IP address and provides name and reverse name resolution
 * functions. The address is stored in network order, but as a signed (rather
 * than unsigned) integer.
 * 
 * @since Android 1.0
 */
public class InetAddress extends Object implements Serializable {

    final static byte[] any_bytes = { 0, 0, 0, 0 };

    final static byte[] localhost_bytes = { 127, 0, 0, 1 };

    static InetAddress ANY = new Inet4Address(any_bytes);

    private final static INetworkSystem NETIMPL = Platform.getNetworkSystem();

    final static InetAddress LOOPBACK = new Inet4Address(localhost_bytes,
            "localhost"); //$NON-NLS-1$

    private static final String ERRMSG_CONNECTION_REFUSED = "Connection refused"; //$NON-NLS-1$

    private static final long serialVersionUID = 3286316764910316507L;

    // BEGIN android-added
    /**
     * default time-to-live for DNS cache entries; 600 seconds == 10 minutes
     */
    private static final String DEFAULT_NETADDR_CACHE_TTL_SECS = "600";
    // END android-added

    String hostName;

    private static class WaitReachable {
    }

    private transient Object waitReachable = new WaitReachable();

    private boolean reached;

    private int addrCount;

    int family = 2;

    byte[] ipaddress;

    // BEGIN android-removed
    // // Fill in the JNI id caches
    // private static native void oneTimeInitialization(boolean supportsIPv6);
    // 
    // static {
    //     oneTimeInitialization(true);
    // }
    // END android-removed

    /**
     * Constructs an InetAddress.
     */
    InetAddress() {
        super();
    }

    /**
     * Constructs an {@code InetAddress}, representing the {@code address} and
     * {@code hostName}.
     * 
     * @param address
     *            the network address.
     */
    InetAddress(byte[] address) {
        super();
        this.ipaddress = address;
    }

    /**
     * Constructs an {@code InetAddress}, representing the {@code address} and
     * {@code hostName}.
     * 
     * @param address
     *            the network address.
     */
    InetAddress(byte[] address, String hostName) {
        super();
        this.ipaddress = address;
        this.hostName = hostName;
    }

    // BEGIN android-removed
    // Removed in newer version of harmony
    // /**
    //  * Returns the IP address of the argument {@code addr} as a byte array. The
    //  * elements are in network order (the highest order address byte is in the
    //  * zeroth element).
    //  * 
    //  * @return the network address as a byte array.
    //  */
    // static byte[] addressOf(int addr) {
    //     int temp = addr;
    //     byte array[] = new byte[4];
    //     array[3] = (byte) (temp & 0xFF);
    //     array[2] = (byte) ((temp >>>= 8) & 0xFF);
    //     array[1] = (byte) ((temp >>>= 8) & 0xFF);
    //     array[0] = (byte) ((temp >>>= 8) & 0xFF);
    //     return array;
    // }
    // BEGIN android-removed

    CacheElement cacheElement() {
        return new CacheElement();
    }

    /**
     * Compares this {@code InetAddress} instance against the specified address
     * in {@code obj}. Two addresses are equal if their address byte arrays have
     * the same length and if the bytes in the arrays are equal.
     * 
     * @param obj
     *            the object to be tested for equality.
     * @return {@code true} if both objects are equal, {@code false} otherwise.
     * @since Android 1.0
     */
    @Override
    public boolean equals(Object obj) {
        // BEGIN android-changed
        if (!(obj instanceof InetAddress)) {
            return false;
        }
        // END android-changed

        // now check if their byte arrays match...
        byte[] objIPaddress = ((InetAddress) obj).ipaddress;
        // BEGIN android-added
        if (objIPaddress.length != ipaddress.length) {
            return false;
        }
        // END android-added
        for (int i = 0; i < objIPaddress.length; i++) {
            if (objIPaddress[i] != this.ipaddress[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the IP address represented by this {@code InetAddress} instance
     * as a byte array. The elements are in network order (the highest order
     * address byte is in the zeroth element).
     * 
     * @return the address in form of a byte array.
     * @since Android 1.0
     */
    public byte[] getAddress() {
        return ipaddress.clone();
    }

    /**
     * Gets all IP addresses associated with the given {@code host} identified
     * by name or IP address in dot-notation. The IP address is resolved by the
     * configured name service. If the host name is empty or {@code null} an
     * {@code UnknownHostException} is thrown. If the host name is a dotted IP
     * address string an array with the corresponding single {@code InetAddress}
     * is returned.
     * 
     * @param host
     *            the host's name or IP to be resolved to an address.
     * @return the array of addresses associated with the specified host.
     * @throws UnknownHostException
     *             if the address lookup fails.
     * @since Android 1.0
     */
    public static InetAddress[] getAllByName(String host)
            throws UnknownHostException {
        // BEGIN android-changed
        // Added change taken from newer harmony concerning zero length hostname.
        // Added special handling for localhost, since it doesn't work properly.
        // TODO Get rid of this later...
        if (host == null || 0 == host.length() ||
                "localhost".equalsIgnoreCase(host)) {
            return new InetAddress[] { preferIPv6Addresses()
                                       ? Inet6Address.LOOPBACK
                                       : LOOPBACK };
        }
        // END android-changed

        if (isHostName(host)) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(host, -1);
            }
            // BEGIN android-changed
            byte[][] rawAddresses = getallbyname(host,
                                                 Socket.preferIPv4Stack());
            InetAddress[] returnedAddresses = new
                                              InetAddress[rawAddresses.length];
            for (int i = 0; i < rawAddresses.length; i++) {
                byte[] rawAddress = rawAddresses[i];
                if (rawAddress.length == 16) {
                    returnedAddresses[i] = new Inet6Address(rawAddress, host);
                } else if (rawAddress.length == 4) {
                    returnedAddresses[i] = new Inet4Address(rawAddress, host);
                } else {
                  // Cannot happen, because the underlying code only returns
                  // addresses that are 4 or 16 bytes long.
                  throw new AssertionError("Impossible address length " + 
                                           rawAddress.length);
                }
            }

            // ok we may have to re-order to make sure the
            // preferIPv6Addresses is respected
            // END android-changed
            InetAddress[] orderedAddresses = null;
            if (returnedAddresses != null) {
                orderedAddresses = new InetAddress[returnedAddresses.length];
                int curPosition = 0;
                if (InetAddress.preferIPv6Addresses()) {
                    for (int i = 0; i < returnedAddresses.length; i++) {
                        if (returnedAddresses[i] instanceof Inet6Address) {
                            orderedAddresses[curPosition] = returnedAddresses[i];
                            curPosition++;
                        }
                    }
                    for (int i = 0; i < returnedAddresses.length; i++) {
                        // BEGIN android-changed
                        if (!(returnedAddresses[i] instanceof Inet6Address)) {
                        // END android-changed
                            orderedAddresses[curPosition] = returnedAddresses[i];
                            curPosition++;
                        }
                    }
                } else {
                    for (int i = 0; i < returnedAddresses.length; i++) {
                        // BEGIN android-changed
                        if (!(returnedAddresses[i] instanceof Inet6Address)) {
                        // END android-changed
                            orderedAddresses[curPosition] = returnedAddresses[i];
                            curPosition++;
                        }
                    }
                    for (int i = 0; i < returnedAddresses.length; i++) {
                        if (returnedAddresses[i] instanceof Inet6Address) {
                            orderedAddresses[curPosition] = returnedAddresses[i];
                            curPosition++;
                        }
                    }
                }
            }
            return orderedAddresses;
        }

        byte[] hBytes = Inet6Util.createByteArrayFromIPAddressString(host);
        // BEGIN android-added
        // Copied from a newer version of harmony
        if (hBytes.length == 4) {
            return (new InetAddress[] { new Inet4Address(hBytes) });
        } else if (hBytes.length == 16) {
            return (new InetAddress[] { new Inet6Address(hBytes) });
        }
        // END android-added
        return (new InetAddress[] { new InetAddress(hBytes) });
    }

    /**
     * Returns the address of a host according to the given host string name
     * {@code host}. The host string may be either a machine name or a dotted
     * string IP address. If the latter, the {@code hostName} field is
     * determined upon demand. {@code host} can be {@code null} which means that
     * an address of the loopback interface is returned.
     * 
     * @param host
     *            the hostName to be resolved to an address or {@code null}.
     * @return the {@code InetAddress} instance representing the host.
     * @throws UnknownHostException
     *             if the address lookup fails.
     * @since Android 1.0
     */
    public static InetAddress getByName(String host)
            throws UnknownHostException {
        // BEGIN android-changed
        // Added special handling for localhost, since it doesn't work properly.
        // TODO Get rid of this later...
        if (host == null || 0 == host.length() || 
                "localhost".equalsIgnoreCase(host)) {
            return InetAddress.LOOPBACK;
        }
        // END android-changed
        if (host.equals("0")) { //$NON-NLS-1$
            return InetAddress.ANY;
        }

        if (isHostName(host)) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(host, -1);
            }
            return lookupHostByName(host);
        }

        return createHostNameFromIPAddress(host);
    }

    /**
     * Gets the textual representation of this IP address.
     * 
     * @return the textual representation of this host address in form of a
     *         dotted string.
     * @since Android 1.0
     */
    public String getHostAddress() {
        return inetNtoaImpl(bytesToInt(ipaddress, 0));
    }

    /**
     * Gets the host name of this IP address. If the IP address could not be
     * resolved, the textual representation in a dotted-quad-notation is
     * returned.
     * 
     * @return the corresponding string name of this IP address.
     * @since Android 1.0
     */
    public String getHostName() {
        try {
            if (hostName == null) {
                int address = 0;
                if (ipaddress.length == 4) {
                    address = bytesToInt(ipaddress, 0);
                    if (address == 0) {
                        return hostName = inetNtoaImpl(address);
                    }
                }
                hostName = getHostByAddrImpl(ipaddress).hostName;
                if (hostName.equals("localhost") && ipaddress.length == 4 //$NON-NLS-1$
                        && address != 0x7f000001) {
                    return hostName = inetNtoaImpl(address);
                }
            }
        } catch (UnknownHostException e) {
            return hostName = Inet6Util
                    .createIPAddrStringFromByteArray(ipaddress);
        }
        SecurityManager security = System.getSecurityManager();
        try {
            // Only check host names, not addresses
            if (security != null && isHostName(hostName)) {
                security.checkConnect(hostName, -1);
            }
        } catch (SecurityException e) {
            return Inet6Util.createIPAddrStringFromByteArray(ipaddress);
        }
        return hostName;
    }

    /**
     * Gets the fully qualified domain name for the host associated with this IP
     * address. If a security manager is set, it is checked if the method caller
     * is allowed to get the hostname. Otherwise, the textual representation in
     * a dotted-quad-notation is returned.
     * 
     * @return the fully qualified domain name of this IP address.
     * @since Android 1.0
     */
    public String getCanonicalHostName() {
        String canonicalName;
        try {
            int address = 0;
            if (ipaddress.length == 4) {
                address = bytesToInt(ipaddress, 0);
                if (address == 0) {
                    return inetNtoaImpl(address);
                }
            }
            canonicalName = getHostByAddrImpl(ipaddress).hostName;
        } catch (UnknownHostException e) {
            return Inet6Util.createIPAddrStringFromByteArray(ipaddress);
        }
        SecurityManager security = System.getSecurityManager();
        try {
            // Only check host names, not addresses
            if (security != null && isHostName(canonicalName)) {
                security.checkConnect(canonicalName, -1);
            }
        } catch (SecurityException e) {
            return Inet6Util.createIPAddrStringFromByteArray(ipaddress);
        }
        return canonicalName;
    }

    /**
     * Gets the local host address if the security policy allows this.
     * Otherwise, gets the loopback address which allows this machine to be
     * contacted.
     * <p>
     * The current implementation returns always the loopback address.
     * </p>
     * 
     * @return the {@code InetAddress} representing the local host.
     * @throws UnknownHostException
     *             if the address lookup fails.
     * @since Android 1.0
     */
    public static InetAddress getLocalHost() throws UnknownHostException {
        String host = getHostNameImpl();
        SecurityManager security = System.getSecurityManager();
        try {
            if (security != null) {
                security.checkConnect(host, -1);
            }
        } catch (SecurityException e) {
            return InetAddress.LOOPBACK;
        }
        return lookupHostByName(host);
    }

    /**
     * Gets the hashcode of the represented IP address.
     * 
     * @return the appropriate hashcode value.
     * @since Android 1.0
     */
    @Override
    public int hashCode() {
        return bytesToInt(ipaddress, 0);
    }

    /**
     * Returns whether this address is an IP multicast address or not.
     * 
     * @return {@code true} if this address is in the multicast group, {@code
     *         false} otherwise.
     * @since Android 1.0
     */
    public boolean isMulticastAddress() {
        return ((ipaddress[0] & 255) >>> 4) == 0xE;
    }

    static synchronized InetAddress lookupHostByName(String host)
            throws UnknownHostException {
        int ttl = -1;

        // BEGIN android-changed
        String ttlValue = AccessController
                .doPrivileged(new PriviAction<String>(
                        "networkaddress.cache.ttl", DEFAULT_NETADDR_CACHE_TTL_SECS)); //$NON-NLS-1$
        // END android-changed
        try {
            if (ttlValue != null) {
                ttl = Integer.decode(ttlValue).intValue();
            }
        } catch (NumberFormatException e) {
            // Ignored
        }
        CacheElement element = null;
        if (ttl == 0) {
            Cache.clear();
        } else {
            element = Cache.get(host);
            // BEGIN android-changed
            if (element != null && ttl > 0) {
                long delta = System.nanoTime() - element.nanoTimeAdded;
                if (delta > secondsToNanos(ttl)) {
                    element = null;
                }
            }
            // END android-changed
        }
        if (element != null) {
            return element.inetAddress();
        }

        // now try the negative cache
        String failedMessage = NegativeCache.getFailedMessage(host);
        if (failedMessage != null) {
            throw new UnknownHostException(host + " - " + failedMessage); //$NON-NLS-1$
        }

        InetAddress anInetAddress;
        try {
            anInetAddress = getHostByNameImpl(host, preferIPv6Addresses());
        } catch (UnknownHostException e) {
            // put the entry in the negative cache
            NegativeCache.put(host, e.getMessage());
            throw new UnknownHostException(host + " - " + e.getMessage()); //$NON-NLS-1$
        }

        Cache.add(anInetAddress);
        return anInetAddress;
    }

    // BEGIN android-added
    /**
     * Multiplies value by 1 billion.
     */
    private static long secondsToNanos(int ttl) {
        return (long) ttl * 1000000000;
    }
    // END android-added

    // BEGIN android-deleted
    // static native InetAddress[] getAliasesByNameImpl(String name)
    //     throws UnknownHostException;
    // END android-deleted

    // BEGIN android-changed
    /**
     * Resolves a host name to its IP addresses. Thread safe.
     */
    private static native byte[][] getallbyname(String name,
                                                boolean preferIPv4Stack);
    // END android-changed

    /**
     * Query the IP stack for the host address. The host is in address form.
     * 
     * @param addr
     *            the host address to lookup.
     * @throws UnknownHostException
     *             if an error occurs during lookup.
     */
    // BEGIN android-changed
    // static native InetAddress getHostByAddrImpl(byte[] addr)
    //    throws UnknownHostException;
    static InetAddress getHostByAddrImpl(byte[] addr)
            throws UnknownHostException {
        return new InetAddress(addr, gethostbyaddr(addr));
    }

    /**
     * Resolves an IP address to a hostname. Thread safe.
     */
    private static native String gethostbyaddr(byte[] addr);
    // END android-changed

    static int inetAddr(String host) throws UnknownHostException {
        return (host.equals("255.255.255.255")) ? 0xFFFFFFFF //$NON-NLS-1$
                : inetAddrImpl(host);
    }

    /**
     * Convert a string containing an IPv4 Internet Protocol dotted address into
     * a binary address. Note, the special case of '255.255.255.255' throws an
     * exception, so this value should not be used as an argument. See also
     * inetAddr(String).
     */
    // BEGIN android-changed
    // static native int inetAddrImpl(String host) throws UnknownHostException;
    static int inetAddrImpl(String host) throws UnknownHostException {
        // TODO Probably not exactly what we want, and also inefficient. Provide native later.
        try {
            String[] args = host.split("\\.");
            
            int a = Integer.parseInt(args[0]) << 24;
            int b = Integer.parseInt(args[1]) << 16;
            int c = Integer.parseInt(args[2]) <<  8;
            int d = Integer.parseInt(args[3])      ;
            
            return a | b | c | d;
        } catch (Exception ex) {
            throw new UnknownHostException(host);
        }
    }
    // END android-changed

    /**
     * Convert a binary address into a string containing an Ipv4 Internet
     * Protocol dotted address.
     */
    // BEGIN android-changed
    // static native String inetNtoaImpl(int hipAddr);
    static String inetNtoaImpl(int hipAddr) {
        // TODO Inefficient and probably wrong. Provide proper (native?) implementation later.
        int a = (hipAddr >> 24) & 0xFF;
        int b = (hipAddr >> 16) & 0xFF;
        int c = (hipAddr >>  8) & 0xFF;
        int d = (hipAddr      ) & 0xFF;
        
        return "" + a + "." + b + "." + c + "." + d;
    }
    // END android-changed

    /**
     * Query the IP stack for the host address. The host is in string name form.
     * 
     * @param name
     *            the host name to lookup
     * @param preferIPv6Addresses
     *            address preference if underlying platform is V4/V6
     * @return InetAddress the host address
     * @throws UnknownHostException
     *             if an error occurs during lookup
     */
    // BEGIN android-changed
    // static native InetAddress getHostByNameImpl(String name,
    //          boolean preferIPv6Address) throws UnknownHostException;
    static InetAddress getHostByNameImpl(String name,
            boolean preferIPv6Address) throws UnknownHostException {
        // TODO Mapped Harmony to Android native. Get rid of indirection later.
        return getAllByName(name)[0];
    }
    // END android-changed

    /**
     * Gets the host name of the system.
     * 
     * @return String the system hostname
     */
    // BEGIN android-changed
    static String getHostNameImpl() {
        // TODO Mapped Harmony to Android native. Get rid of indirection later.

        return gethostname();
    }
    static native String gethostname();
    // END android-changed

    static String getHostNameInternal(String host) throws UnknownHostException {
        if (host == null || 0 == host.length()) {
            return InetAddress.LOOPBACK.getHostAddress();
        }
        if (isHostName(host)) {
            return lookupHostByName(host).getHostAddress();
        }
        return host;
    }

    /**
     * Returns a string containing a concise, human-readable description of this
     * IP address.
     * 
     * @return the description, as host/address.
     * @since Android 1.0
     */
    @Override
    public String toString() {
        return (hostName == null ? "" : hostName) + "/" + getHostAddress(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    class CacheElement {
        // BEGIN android-changed
        // Partly copied from a newer version of harmony
        final long nanoTimeAdded = System.nanoTime();

        CacheElement next;

        CacheElement() {
            super();
        }
        // END android-changed

        String hostName() {
            return hostName;
        }

        InetAddress inetAddress() {
            return InetAddress.this;
        }
    }

    // BEGIN android-changed
    // Copied from a newer version of harmony
    static class Cache {
        private static int maxSize = 5;

        private static int size = 0;

        private static CacheElement head;

        static synchronized void clear() {
            size = 0;
            head = null;
        }

        static synchronized void add(InetAddress value) {
            CacheElement newElement = value.cacheElement();
            if (size < maxSize) {
                size++;
            } else {
                deleteTail();
            }
            newElement.next = head; // If the head is null, this does no harm.
            head = newElement;
        }

        static synchronized CacheElement get(String name) {
            CacheElement previous = null;
            CacheElement current = head;
            boolean notFound = true;
            while ((null != current)
                    && (notFound = !(name.equals(current.hostName())))) {
                previous = current;
                current = current.next;
            }
            if (notFound) {
                return null;
            }
            moveToHead(current, previous);
            return current;
        }

        private synchronized static void deleteTail() {
            if (0 == size) {
                return;
            }
            if (1 == size) {
                head = null;
            }

            CacheElement previous = null;
            CacheElement current = head;
            while (null != current.next) {
                previous = current;
                current = current.next;
            }
            previous.next = null;
        }

        private synchronized static void moveToHead(CacheElement element,
                CacheElement elementPredecessor) {
            if (null == elementPredecessor) {
                head = element;
            } else {
                elementPredecessor.next = element.next;
                element.next = head;
                head = element;
            }
        }
    }
    // END android-changed

    /**
     * Returns true if the string is a host name, false if it is an IP Address.
     */
    private static boolean isHostName(String value) {
        return !(Inet6Util.isValidIPV4Address(value) || Inet6Util
                .isValidIP6Address(value));
    }

    /**
     * Returns whether this address is a loopback address or not. This
     * implementation returns always {@code false}. Valid IPv4 loopback
     * addresses are 127.d.d.d The only valid IPv6 loopback address is ::1.
     * 
     * @return {@code true} if this instance represents a loopback address,
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isLoopbackAddress() {
        return false;
    }

    /**
     * Returns whether this address is a link-local address or not. This
     * implementation returns always {@code false}.
     * <p>
     * Valid IPv6 link-local addresses are FE80::0 through to
     * FEBF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF.
     * </p>
     * <p>
     * There are no valid IPv4 link-local addresses.
     * </p>
     * 
     * @return {@code true} if this instance represents a link-local address,
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isLinkLocalAddress() {
        return false;
    }

    /**
     * Returns whether this address is a site-local address or not. This
     * implementation returns always {@code false}.
     * <p>
     * Valid IPv6 site-local addresses are FEC0::0 through to
     * FEFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF.
     * </p>
     * <p>
     * There are no valid IPv4 site-local addresses.
     * </p>
     * 
     * @return {@code true} if this instance represents a site-local address,
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isSiteLocalAddress() {
        return false;
    }

    /**
     * Returns whether this address is a global multicast address or not. This
     * implementation returns always {@code false}.
     * <p>
     * Valid IPv6 link-global multicast addresses are FFxE:/112 where x is a set
     * of flags, and the additional 112 bits make up the global multicast
     * address space.
     * </p>
     * <p>
     * Valid IPv4 global multicast addresses are between: 224.0.1.0 to
     * 238.255.255.255.
     * </p>
     * 
     * @return {@code true} if this instance represents a global multicast
     *         address, {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMCGlobal() {
        return false;
    }

    /**
     * Returns whether this address is a node-local multicast address or not.
     * This implementation returns always {@code false}.
     * <p>
     * Valid IPv6 node-local multicast addresses are FFx1:/112 where x is a set
     * of flags, and the additional 112 bits make up the node-local multicast
     * address space.
     * </p>
     * <p>
     * There are no valid IPv4 node-local multicast addresses.
     * </p>
     * 
     * @return {@code true} if this instance represents a node-local multicast
     *         address, {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMCNodeLocal() {
        return false;
    }

    /**
     * Returns whether this address is a link-local multicast address or not.
     * This implementation returns always {@code false}.
     * <p>
     * Valid IPv6 link-local multicast addresses are FFx2:/112 where x is a set
     * of flags, and the additional 112 bits make up the link-local multicast
     * address space.
     * </p>
     * <p>
     * Valid IPv4 link-local addresses are between: 224.0.0.0 to 224.0.0.255
     * </p>
     * 
     * @return {@code true} if this instance represents a link-local multicast
     *         address, {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMCLinkLocal() {
        return false;
    }

    /**
     * Returns whether this address is a site-local multicast address or not.
     * This implementation returns always {@code false}.
     * <p>
     * Valid IPv6 site-local multicast addresses are FFx5:/112 where x is a set
     * of flags, and the additional 112 bits make up the site-local multicast
     * address space.
     * </p>
     * <p>
     * Valid IPv4 site-local addresses are between: 239.252.0.0 to
     * 239.255.255.255
     * </p>
     * 
     * @return {@code true} if this instance represents a site-local multicast
     *         address, {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMCSiteLocal() {
        return false;
    }

    /**
     * Returns whether this address is a organization-local multicast address or
     * not. This implementation returns always {@code false}.
     * <p>
     * Valid IPv6 organization-local multicast addresses are FFx8:/112 where x
     * is a set of flags, and the additional 112 bits make up the
     * organization-local multicast address space.
     * </p>
     * <p>
     * Valid IPv4 organization-local addresses are between: 239.192.0.0 to
     * 239.251.255.255
     * </p>
     * 
     * @return {@code true} if this instance represents a organization-local
     *         multicast address, {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMCOrgLocal() {
        return false;
    }

    /**
     * Returns whether this is a wildcard address or not. This implementation
     * returns always {@code false}.
     * 
     * @return {@code true} if this instance represents a wildcard address,
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isAnyLocalAddress() {
        return false;
    }

    /**
     * Tries to reach this {@code InetAddress}. This method first tries to use
     * ICMP <i>(ICMP ECHO REQUEST)</i>. When first step fails, a TCP connection
     * on port 7 (Echo) of the remote host is established.
     * 
     * @param timeout
     *            timeout in milliseconds before the test fails if no connection
     *            could be established.
     * @return {@code true} if this address is reachable, {@code false}
     *         otherwise.
     * @throws IOException
     *             if an error occurs during an I/O operation.
     * @throws IllegalArgumentException
     *             if timeout is less than zero.
     * @since Android 1.0
     */
    public boolean isReachable(int timeout) throws IOException {
        return isReachable(null, 0, timeout);
    }

    /**
     * Tries to reach this {@code InetAddress}. This method first tries to use
     * ICMP <i>(ICMP ECHO REQUEST)</i>. When first step fails, a TCP connection
     * on port 7 (Echo) of the remote host is established.
     * 
     * @param netif
     *            the network interface on which to connection should be
     *            established.
     * @param ttl
     *            the maximum count of hops (time-to-live).
     * @param timeout
     *            timeout in milliseconds before the test fails if no connection
     *            could be established.
     * @return {@code true} if this address is reachable, {@code false}
     *         otherwise.
     * @throws IOException
     *             if an error occurs during an I/O operation.
     * @throws IllegalArgumentException
     *             if ttl or timeout is less than zero.
     * @since Android 1.0
     */
    public boolean isReachable(NetworkInterface netif, final int ttl,
            final int timeout) throws IOException {
        if (0 > ttl || 0 > timeout) {
            throw new IllegalArgumentException(Msg.getString("K0051")); //$NON-NLS-1$
        }
        boolean reachable = false;
        if (null == netif) {
            // network interface is null, binds to no address
            // BEGIN android-changed
            // reachable = NETIMPL.isReachableByICMP(this, null, ttl, timeout);
            // if (!reachable) {
                reachable = isReachableByTCP(this, null, timeout);
            // }
            // END android-changed
        } else {
            // Not Bind to any address
            if (null == netif.addresses) {
                return false;
            }
            // binds to all address on this NetworkInterface, tries ICMP ping
            // first
            // BEGIN android-changed
            // reachable = isReachableByICMPUseMultiThread(netif, ttl, timeout);
            // if (!reachable) {
                // tries TCP echo if ICMP ping fails
                reachable = isReachableByMultiThread(netif, ttl, timeout);
            // }
            // END adnroid-changed
        }
        return reachable;
    }

    /*
     * Uses multi-Thread to try if isReachable, returns true if any of threads
     * returns in time
     */
    // BEGIN android-changed
    private boolean isReachableByMultiThread(NetworkInterface netif,
            final int ttl, final int timeout)
    // END android-changed
            throws IOException {
        if (null == netif.addresses) {
            return false;
        }
        Enumeration<InetAddress> addresses = netif.getInetAddresses();
        reached = false;
        addrCount = netif.addresses.length;
        boolean needWait = false;
        while (addresses.hasMoreElements()) {
            final InetAddress addr = addresses.nextElement();

            // loopback interface can only reach to local addresses
            if (addr.isLoopbackAddress()) {
                Enumeration<NetworkInterface> NetworkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (NetworkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = NetworkInterfaces
                            .nextElement();
                    Enumeration<InetAddress> localAddresses = networkInterface
                            .getInetAddresses();
                    while (localAddresses.hasMoreElements()) {
                        if (InetAddress.this.equals(localAddresses
                                .nextElement())) {
                            return true;
                        }
                    }
                }

                synchronized (waitReachable) {
                    addrCount--;

                    if (addrCount == 0) {
                        // if count equals zero, all thread
                        // expired,notifies main thread
                        waitReachable.notifyAll();
                    }
                }
                continue;
            }

            needWait = true;
            new Thread() {
                @Override
                public void run() {
                    boolean threadReached = false;
                    // BEGIN android-changed
                    // if isICMP, tries ICMP ping, else TCP echo
                    // if (isICMP) {
                    //     threadReached = NETIMPL.isReachableByICMP(
                    //             InetAddress.this, addr, ttl, timeout);
                    // } else {
                        try {
                            threadReached = isReachableByTCP(InetAddress.this,
                                    addr, timeout);
                        } catch (IOException e) {
                            // do nothing
                        }
                    // }
                    // END android-changed

                    synchronized (waitReachable) {
                        if (threadReached) {
                            // if thread reached this address, sets reached to
                            // true and notifies main thread
                            reached = true;
                            waitReachable.notifyAll();
                        } else {
                            addrCount--;
                            if (0 == addrCount) {
                                // if count equals zero, all thread
                                // expired,notifies main thread
                                waitReachable.notifyAll();
                            }
                        }
                    }
                }
            }.start();
        }

        if (needWait) {
            synchronized (waitReachable) {
                try {
                    while (!reached && (addrCount != 0)) {
                        // wait for notification
                        waitReachable.wait(1000);
                    }
                } catch (InterruptedException e) {
                    // do nothing
                }
                return reached;
            }
        }

        return false;
    }

    // BEGIN android-removed
    // private boolean isReachableByICMPUseMultiThread(NetworkInterface netif,
    //         int ttl, int timeout) throws IOException {
    //     return isReachableByMultiThread(netif, ttl, timeout, true);
    // }
    // 
    // private boolean isReachableByTCPUseMultiThread(NetworkInterface netif,
    //         int ttl, int timeout) throws IOException {
    //     return isReachableByMultiThread(netif, ttl, timeout, false);
    // }
    // END android-removed

    private boolean isReachableByTCP(InetAddress dest, InetAddress source,
            int timeout) throws IOException {
        FileDescriptor fd = new FileDescriptor();
        // define traffic only for parameter
        int traffic = 0;
        boolean reached = false;
        NETIMPL.createSocket(fd, NetUtil.preferIPv4Stack());
        try {
            if (null != source) {
                NETIMPL.bind(fd, 0, source);
            }
            NETIMPL.connectStreamWithTimeoutSocket(fd, 7, timeout, traffic,
                    dest);
            reached = true;
        } catch (IOException e) {
            if (ERRMSG_CONNECTION_REFUSED.equals(e.getMessage())) {
                // Connection refused means the IP is reachable
                reached = true;
            }
        }

        NETIMPL.socketClose(fd);

        return reached;
    }

    /**
     * Returns the {@code InetAddress} corresponding to the array of bytes. In
     * the case of an IPv4 address there must be exactly 4 bytes and for IPv6
     * exactly 16 bytes. If not, an {@code UnknownHostException} is thrown.
     * <p>
     * The IP address is not validated by a name service.
     * </p>
     * <p>
     * The high order byte is {@code ipAddress[0]}.
     * </p>
     * 
     * @param ipAddress
     *            is either a 4 (IPv4) or 16 (IPv6) byte long array.
     * @return an {@code InetAddress} instance representing the given IP address
     *         {@code ipAddress}.
     * @throws UnknownHostException
     *             if the given byte array has no valid length.
     * @since Android 1.0
     */
    public static InetAddress getByAddress(byte[] ipAddress)
            throws UnknownHostException {
        // simply call the method by the same name specifying the default scope
        // id of 0
        return getByAddress(ipAddress, 0);
    }

    /**
     * Returns the {@code InetAddress} corresponding to the array of bytes. In
     * the case of an IPv4 address there must be exactly 4 bytes and for IPv6
     * exactly 16 bytes. If not, an {@code UnknownHostException} is thrown. The
     * IP address is not validated by a name service. The high order byte is
     * {@code ipAddress[0]}.
     * 
     * @param ipAddress
     *            either a 4 (IPv4) or 16 (IPv6) byte array.
     * @param scope_id
     *            the scope id for an IPV6 scoped address. If not a scoped
     *            address just pass in 0.
     * @return the InetAddress
     * @throws UnknownHostException
     */
    static InetAddress getByAddress(byte[] ipAddress, int scope_id)
            throws UnknownHostException {
        byte[] copy_address;
        if (ipAddress != null && ipAddress.length == 4) {
            copy_address = new byte[4];
            for (int i = 0; i < 4; i++) {
                copy_address[i] = ipAddress[i];
            }
            // BEGIN adnroid-changed
            // Copied from a newer version of harmony
            return new Inet4Address(copy_address);
            // END android-changed
        }

        if (ipAddress != null && ipAddress.length == 16) {
            // First check to see if the address is an IPv6-mapped
            // IPv4 address. If it is, then we can make it a IPv4
            // address, otherwise, we'll create an IPv6 address.
            if (isIPv4MappedAddress(ipAddress)) {
                copy_address = new byte[4];
                for (int i = 0; i < 4; i++) {
                    copy_address[i] = ipAddress[12 + i];
                }
                return new Inet4Address(copy_address);
            }
            copy_address = ipAddress.clone();
            return new Inet6Address(copy_address, scope_id);
        }
        throw new UnknownHostException(Msg.getString("K0339")); //$NON-NLS-1$
    }

    private static boolean isIPv4MappedAddress(byte ipAddress[]) {
        // Check if the address matches ::FFFF:d.d.d.d
        // The first 10 bytes are 0. The next to are -1 (FF).
        // The last 4 bytes are varied.
        for (int i = 0; i < 10; i++) {
            if (ipAddress[i] != 0) {
                return false;
            }
        }

        if (ipAddress[10] != -1 || ipAddress[11] != -1) {
            return false;
        }

        return true;
    }

    /**
     * Returns the {@code InetAddress} corresponding to the array of bytes, and
     * the given hostname. In the case of an IPv4 address there must be exactly
     * 4 bytes and for IPv6 exactly 16 bytes. If not, an {@code
     * UnknownHostException} will be thrown.
     * <p>
     * The host name and IP address are not validated.
     * </p>
     * <p>
     * The hostname either be a machine alias or a valid IPv6 or IPv4 address
     * format.
     * </p>
     * <p>
     * The high order byte is {@code ipAddress[0]}.
     * </p>
     * 
     * @param hostName
     *            the string representation of hostname or IP address.
     * @param ipAddress
     *            either a 4 (IPv4) or 16 (IPv6) byte long array.
     * @return an {@code InetAddress} instance representing the given IP address
     *         and hostname.
     * @throws UnknownHostException
     *             if the given byte array has no valid length.
     * @since Android 1.0
     */
    public static InetAddress getByAddress(String hostName, byte[] ipAddress)
            throws UnknownHostException {
        // just call the method by the same name passing in a default scope id
        // of 0
        return getByAddressInternal(hostName, ipAddress, 0);
    }

    /**
     * Returns the {@code InetAddress} corresponding to the array of bytes, and
     * the given hostname. In the case of an IPv4 address there must be exactly
     * 4 bytes and for IPv6 exactly 16 bytes. If not, an {@code
     * UnknownHostException} is thrown. The host name and IP address are not
     * validated. The hostname either be a machine alias or a valid IPv6 or IPv4
     * address format. The high order byte is {@code ipAddress[0]}.
     * 
     * @param hostName
     *            string representation of hostname or IP address.
     * @param ipAddress
     *            either a 4 (IPv4) or 16 (IPv6) byte array.
     * @param scope_id
     *            the scope id for a scoped address. If not a scoped address
     *            just pass in 0.
     * @return the InetAddress
     * @throws UnknownHostException
     */
    static InetAddress getByAddressInternal(String hostName, byte[] ipAddress,
            int scope_id) throws UnknownHostException {
        byte[] copy_address;
        if (ipAddress != null && ipAddress.length == 4) {
            copy_address = new byte[4];
            for (int i = 0; i < 4; i++) {
                copy_address[i] = ipAddress[i];
            }
            return new Inet4Address(ipAddress, hostName);
        }

        if (ipAddress != null && ipAddress.length == 16) {
            // First check to see if the address is an IPv6-mapped
            // IPv4 address. If it is, then we can make it a IPv4
            // address, otherwise, we'll create an IPv6 address.
            if (isIPv4MappedAddress(ipAddress)) {
                copy_address = new byte[4];
                for (int i = 0; i < 4; i++) {
                    copy_address[i] = ipAddress[12 + i];
                }
                return new Inet4Address(ipAddress, hostName);
            }

            copy_address = new byte[16];
            for (int i = 0; i < 16; i++) {
                copy_address[i] = ipAddress[i];
            }

            return new Inet6Address(ipAddress, hostName, scope_id);
        }

        throw new UnknownHostException(Msg.getString("K0332", hostName)); //$NON-NLS-1$
    }

    /**
     * Takes the integer and chops it into 4 bytes, putting it into the byte
     * array starting with the high order byte at the index start. This method
     * makes no checks on the validity of the parameters.
     */
    static void intToBytes(int value, byte bytes[], int start) {
        // Shift the int so the current byte is right-most
        // Use a byte mask of 255 to single out the last byte.
        bytes[start] = (byte) ((value >> 24) & 255);
        bytes[start + 1] = (byte) ((value >> 16) & 255);
        bytes[start + 2] = (byte) ((value >> 8) & 255);
        bytes[start + 3] = (byte) (value & 255);
    }

    /**
     * Takes the byte array and creates an integer out of four bytes starting at
     * start as the high-order byte. This method makes no checks on the validity
     * of the parameters.
     */
    static int bytesToInt(byte bytes[], int start) {
        // First mask the byte with 255, as when a negative
        // signed byte converts to an integer, it has bits
        // on in the first 3 bytes, we are only concerned
        // about the right-most 8 bits.
        // Then shift the rightmost byte to align with its
        // position in the integer.
        int value = ((bytes[start + 3] & 255))
                | ((bytes[start + 2] & 255) << 8)
                | ((bytes[start + 1] & 255) << 16)
                | ((bytes[start] & 255) << 24);
        return value;
    }

    /**
     * Creates an InetAddress based on the {@code ipAddressString}. No error
     * handling is performed here.
     */
    static InetAddress createHostNameFromIPAddress(String ipAddressString)
            throws UnknownHostException {

        InetAddress address = null;

        if (Inet6Util.isValidIPV4Address(ipAddressString)) {
            byte[] byteAddress = new byte[4];
            String[] parts = ipAddressString.split("\\."); //$NON-NLS-1$
            int length = parts.length;
            if (length == 1) {
                long value = Long.parseLong(parts[0]);
                for (int i = 0; i < 4; i++) {
                    byteAddress[i] = (byte) (value >> ((3 - i) * 8));
                }
            } else {
                for (int i = 0; i < length; i++) {
                    byteAddress[i] = (byte) Integer.parseInt(parts[i]);
                }
            }

            // adjust for 2/3 parts address
            if (length == 2) {
                byteAddress[3] = byteAddress[1];
                byteAddress[1] = 0;
            }
            if (length == 3) {
                byteAddress[3] = byteAddress[2];
                byteAddress[2] = 0;
            }

            address = new Inet4Address(byteAddress);
        } else { // otherwise it must be ipv6

            if (ipAddressString.charAt(0) == '[') {
                ipAddressString = ipAddressString.substring(1, ipAddressString
                        .length() - 1);
            }

            StringTokenizer tokenizer = new StringTokenizer(ipAddressString,
                    ":.%", true); //$NON-NLS-1$
            ArrayList<String> hexStrings = new ArrayList<String>();
            ArrayList<String> decStrings = new ArrayList<String>();
            String scopeString = null;
            String token = ""; //$NON-NLS-1$
            String prevToken = ""; //$NON-NLS-1$
            String prevPrevToken = ""; //$NON-NLS-1$
            int doubleColonIndex = -1; // If a double colon exists, we need to
            // insert 0s.

            // Go through the tokens, including the separators ':' and '.'
            // When we hit a : or . the previous token will be added to either
            // the hex list or decimal list. In the case where we hit a ::
            // we will save the index of the hexStrings so we can add zeros
            // in to fill out the string
            while (tokenizer.hasMoreTokens()) {
                prevPrevToken = prevToken;
                prevToken = token;
                token = tokenizer.nextToken();

                if (token.equals(":")) { //$NON-NLS-1$
                    if (prevToken.equals(":")) { //$NON-NLS-1$
                        doubleColonIndex = hexStrings.size();
                    } else if (!prevToken.equals("")) { //$NON-NLS-1$
                        hexStrings.add(prevToken);
                    }
                } else if (token.equals(".")) { //$NON-NLS-1$
                    decStrings.add(prevToken);
                } else if (token.equals("%")) { //$NON-NLS-1$
                    // add the last word before the % properly
                    if (!prevToken.equals(":") && !prevToken.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
                        if (prevPrevToken.equals(":")) { //$NON-NLS-1$
                            hexStrings.add(prevToken);
                        } else if (prevPrevToken.equals(".")) { //$NON-NLS-1$
                            decStrings.add(prevToken);
                        }
                    }

                    // the rest should be the scope string
                    scopeString = tokenizer.nextToken();
                    while (tokenizer.hasMoreTokens()) {
                        scopeString = scopeString + tokenizer.nextToken();
                    }
                }
            }

            if (prevToken.equals(":")) { //$NON-NLS-1$
                if (token.equals(":")) { //$NON-NLS-1$
                    doubleColonIndex = hexStrings.size();
                } else {
                    hexStrings.add(token);
                }
            } else if (prevToken.equals(".")) { //$NON-NLS-1$
                decStrings.add(token);
            }

            // figure out how many hexStrings we should have
            // also check if it is a IPv4 address
            int hexStringsLength = 8;

            // If we have an IPv4 address tagged on at the end, subtract
            // 4 bytes, or 2 hex words from the total
            if (decStrings.size() > 0) {
                hexStringsLength -= 2;
            }

            // if we hit a double Colon add the appropriate hex strings
            if (doubleColonIndex != -1) {
                int numberToInsert = hexStringsLength - hexStrings.size();
                for (int i = 0; i < numberToInsert; i++) {
                    hexStrings.add(doubleColonIndex, "0"); //$NON-NLS-1$
                }
            }

            byte ipByteArray[] = new byte[16];

            // Finally convert these strings to bytes...
            for (int i = 0; i < hexStrings.size(); i++) {
                Inet6Util.convertToBytes(hexStrings.get(i), ipByteArray, i * 2);
            }

            // Now if there are any decimal values, we know where they go...
            for (int i = 0; i < decStrings.size(); i++) {
                ipByteArray[i + 12] = (byte) (Integer.parseInt(decStrings
                        .get(i)) & 255);
            }

            // now check to see if this guy is actually and IPv4 address
            // an ipV4 address is ::FFFF:d.d.d.d
            boolean ipV4 = true;
            for (int i = 0; i < 10; i++) {
                if (ipByteArray[i] != 0) {
                    ipV4 = false;
                    break;
                }
            }

            if (ipByteArray[10] != -1 || ipByteArray[11] != -1) {
                ipV4 = false;
            }

            if (ipV4) {
                byte ipv4ByteArray[] = new byte[4];
                for (int i = 0; i < 4; i++) {
                    ipv4ByteArray[i] = ipByteArray[i + 12];
                }
                address = InetAddress.getByAddress(ipv4ByteArray);
            } else {
                int scopeId = 0;
                if (scopeString != null) {
                    try {
                        scopeId = Integer.parseInt(scopeString);
                    } catch (Exception e) {
                        // this should not occur as we should not get into this
                        // function unless the address is in a valid format
                    }
                }
                address = InetAddress.getByAddress(ipByteArray, scopeId);
            }
        }

        return address;
    }

    static boolean preferIPv6Addresses() {
        String result = AccessController.doPrivileged(new PriviAction<String>(
                "java.net.preferIPv6Addresses")); //$NON-NLS-1$
        return "true".equals(result); //$NON-NLS-1$
    }

    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("address", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("family", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("hostName", String.class) }; //$NON-NLS-1$

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        if (ipaddress == null) {
            fields.put("address", 0); //$NON-NLS-1$
        } else {
            fields.put("address", bytesToInt(ipaddress, 0)); //$NON-NLS-1$
        }
        fields.put("family", family); //$NON-NLS-1$
        fields.put("hostName", hostName); //$NON-NLS-1$

        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        int addr = fields.get("address", 0); //$NON-NLS-1$
        ipaddress = new byte[4];
        intToBytes(addr, ipaddress, 0);
        hostName = (String) fields.get("hostName", null); //$NON-NLS-1$
        family = fields.get("family", 2); //$NON-NLS-1$
    }

    private Object readResolve() throws ObjectStreamException {
        return new Inet4Address(ipaddress, hostName);
    }
}
