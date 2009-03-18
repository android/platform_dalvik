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

/**
 * @author Boris V. Kuznetsov
 * @version $Revision$
 */

package org.apache.harmony.security.tests.java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Provider;
import java.security.Security;
import java.security.SecurityPermission;
import java.security.Provider.Service;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(Provider.class)
/**
 * Tests for <code>Provider</code> constructor and methods
 * 
 */
public class ProviderTest extends TestCase {

    Provider p;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        p = new MyProvider();
    }

    /*
     * Class under test for void Provider()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies provider object",
        method = "Provider",
        args = {java.lang.String.class, double.class, java.lang.String.class}
    )
    public final void testProvider() {
        if (!p.getProperty("Provider.id name").equals(
                String.valueOf(p.getName()))) {
            fail("Incorrect \"Provider.id name\" value");
        }
        if (!p.getProperty("Provider.id version").equals(
                String.valueOf(p.getVersion()))) {
            fail("Incorrect \"Provider.id version\" value");
        }
        if (!p.getProperty("Provider.id info").equals(
                String.valueOf(p.getInfo()))) {
            fail("Incorrect \"Provider.id info\" value");
        }
        if (!p.getProperty("Provider.id className").equals(
                p.getClass().getName())) {
            fail("Incorrect \"Provider.id className\" value");
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "clear",
        args = {}
    )
    public final void testClear() {
        p.clear();
        if (p.getProperty("MessageDigest.SHA-1") != null) {
            fail("Provider contains properties");
        }
    }
    
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "clear",
            args = {}
        )
    @KnownFailure("AccessController/AccessControlContext grants Permissions by default")
    public final void testClear_SecurityManager() {
        TestSecurityManager sm = new TestSecurityManager("clearProviderProperties.MyProvider");
        System.setSecurityManager(sm);
        p.clear();
        assertTrue("Provider.clear must call checkPermission with "
                + "SecurityPermission clearProviderProperties.NAME",
                sm.called);
        System.setSecurityManager(null);
    }

    /*
     * Class under test for void Provider(String, double, String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies constructor with non null parameters",
        method = "Provider",
        args = {java.lang.String.class, double.class, java.lang.String.class}
    )
    public final void testProviderStringdoubleString() {
        Provider p = new MyProvider("Provider name", 123.456, "Provider info");
        if (!p.getName().equals("Provider name") || p.getVersion() != 123.456
                || !p.getInfo().equals("Provider info")) {
            fail("Incorrect values");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public final void testGetName() {
        if (!p.getName().equals("MyProvider")) {
            fail("Incorrect provider name");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getVersion",
        args = {}
    )
    public final void testGetVersion() {
        if (p.getVersion() != 1.0) {
            fail("Incorrect provider version");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInfo",
        args = {}
    )
    public final void testGetInfo() {
        if (!p.getInfo().equals("Provider for testing")) {
            fail("Incorrect provider info");
        }
    }

    /*
     * Class under test for void putAll(Map)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "putAll",
        args = {java.util.Map.class}
    )
    public final void testPutAllMap() {
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("MessageDigest.SHA-1", "aaa.bbb.ccc.ddd");
        hm.put("Property 1", "value 1");
        hm.put("serviceName.algName attrName", "attrValue");
        hm.put("Alg.Alias.engineClassName.aliasName", "stanbdardName");
        p.putAll(hm);
        if (!"value 1".equals(p.getProperty("Property 1").trim())
                || !"attrValue".equals(p.getProperty(
                        "serviceName.algName attrName").trim())
                || !"stanbdardName".equals(p.getProperty(
                        "Alg.Alias.engineClassName.aliasName").trim())
                || !"aaa.bbb.ccc.ddd".equals(p.getProperty(
                        "MessageDigest.SHA-1").trim())) {
            fail("Incorrect property value");
        }
    }

    /*
     * Class under test for Set entrySet()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "entrySet",
        args = {}
    )
    public final void testEntrySet() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");

        Set<Map.Entry<Object, Object>> s = p.entrySet();
        try {
            s.clear();
            fail("Must return unmodifiable set");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals("Incorrect set size", 8, s.size());

        for (Iterator<Entry<Object, Object>> it = s.iterator(); it.hasNext();) {
            Entry<Object, Object> e = it.next();
            String key = (String) e.getKey();
            String val = (String) e.getValue();
            if (key.equals("MessageDigest.SHA-1")
                    && val.equals("SomeClassName")) {
                continue;
            }
            if (key.equals("Alg.Alias.MessageDigest.SHA1")
                    && val.equals("SHA-1")) {
                continue;
            }
            if (key.equals("MessageDigest.abc") && val.equals("SomeClassName")) {
                continue;
            }
            if (key.equals("Provider.id className")
                    && val.equals(p.getClass().getName())) {
                continue;
            }
            if (key.equals("Provider.id name") && val.equals("MyProvider")) {
                continue;
            }
            if (key.equals("MessageDigest.SHA-256")
                    && val.equals("aaa.bbb.ccc.ddd")) {
                continue;
            }
            if (key.equals("Provider.id version") && val.equals("1.0")) {
                continue;
            }
            if (key.equals("Provider.id info")
                    && val.equals("Provider for testing")) {
                continue;
            }
            fail("Incorrect set");
        }
    }

    /*
     * Class under test for Set keySet()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "keySet",
        args = {}
    )
    public final void testKeySet() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");

        Set<Object> s = p.keySet();
        try {
            s.clear();
        } catch (UnsupportedOperationException e) {
        }
        Set<Object> s1 = p.keySet();
        if ((s == s1) || s1.isEmpty()) {
            fail("Must return unmodifiable set");
        }
        if (s1.size() != 8) {
            fail("Incorrect set size");
        }
        if (!s1.contains("MessageDigest.SHA-256")
                || !s1.contains("MessageDigest.SHA-1")
                || !s1.contains("Alg.Alias.MessageDigest.SHA1")
                || !s1.contains("MessageDigest.abc")
                || !s1.contains("Provider.id info")
                || !s1.contains("Provider.id className")
                || !s1.contains("Provider.id version")
                || !s1.contains("Provider.id name")) {
            fail("Incorrect set");
        }
    }

    /*
     * Class under test for Collection values()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "values",
        args = {}
    )
    public final void testValues() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");

        Collection<Object> c = p.values();
        try {
            c.clear();
        } catch (UnsupportedOperationException e) {
        }
        Collection<Object> c1 = p.values();
        if ((c == c1) || c1.isEmpty()) {
            fail("Must return unmodifiable set");
        }
        if (c1.size() != 8) {
            fail("Incorrect set size " + c1.size());
        }
        if (!c1.contains("MyProvider") || !c1.contains("aaa.bbb.ccc.ddd")
                || !c1.contains("Provider for testing") || !c1.contains("1.0")
                || !c1.contains("SomeClassName") || !c1.contains("SHA-1")
                || !c1.contains(p.getClass().getName())) {
            fail("Incorrect set");
        }
    }

    /*
     * Class under test for Object put(Object, Object)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {java.lang.Object.class, java.lang.Object.class}
    )
    public final void testPutObjectObject() {
        p.put("MessageDigest.SHA-1", "aaa.bbb.ccc.ddd");
        p.put("Type.Algorithm", "className");
        if (!"aaa.bbb.ccc.ddd".equals(p.getProperty("MessageDigest.SHA-1")
                .trim())) {
            fail("Incorrect property value");
        }

        Set<Service> services = p.getServices();
        if (services.size() != 3) {
            fail("incorrect size");
        }
        for (Iterator<Service> it = services.iterator(); it.hasNext();) {
            Provider.Service s = it.next();
            if ("Type".equals(s.getType())
                    && "Algorithm".equals(s.getAlgorithm())
                    && "className".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType())
                    && "SHA-1".equals(s.getAlgorithm())
                    && "aaa.bbb.ccc.ddd".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType())
                    && "abc".equals(s.getAlgorithm())
                    && "SomeClassName".equals(s.getClassName())) {
                continue;
            }
            fail("Incorrect service");
        }
    }

    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "put",
            args = {java.lang.Object.class, java.lang.Object.class}
        )
    @KnownFailure("AccessController/AccessControlContext grants Permissions by default")
    public final void testPutObjectObject_SecurityManager() {

        TestSecurityManager sm = new TestSecurityManager("putProviderProperty.MyProvider");
        Provider p = new MyProvider();
        System.setSecurityManager(sm);
        p.put(new Object(), new Object());
        assertTrue("Provider put must call checkPermission "
                + "SecurityPermission putProviderProperty.Name", sm.called);
        System.setSecurityManager(null);
    }

    /*
     * Class under test for Object remove(Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "remove",
        args = {java.lang.Object.class}
    )
    public final void testRemoveObject() {
        Object o = p.remove("MessageDigest.SHA-1");
        if (!"SomeClassName".equals(o)) {
            fail("Incorrect return value");
        }
        if (p.getProperty("MessageDigest.SHA-1") != null) {
            fail("Provider contains properties");
        }
        if (p.getServices().size() != 1) {
            fail("Service not removed");
        }

        try {
            p.remove(null);
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // ok
        }
    }

    /*
     * Class under test for Object remove(Object)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "remove",
        args = {java.lang.Object.class}
    )
    @KnownFailure("AccessController/AccessControlContext grants Permissions by default")    
    public final void testRemoveObject_SecurityManager() {
        TestSecurityManager sm = new TestSecurityManager(
                "removeProviderProperty.MyProvider");
        System.setSecurityManager(sm);
        p.remove(new Object());
        assertTrue("Provider.remove must check permission "
                + "SecurityPermission removeProviderProperty.NAME",
                sm.called);
        System.setSecurityManager(null);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getService",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public final void testService1() {
        p.put("MessageDigest.SHA-1", "AnotherClassName");
        Provider.Service s = p.getService("MessageDigest", "SHA-1");
        if (!"AnotherClassName".equals(s.getClassName())) {
            fail("Incorrect class name " + s.getClassName());
        }
        
        try {
           p.getService("MessageDigest", null);
           fail("expected NullPointerException");
        } catch (NullPointerException e)  {
            // ok;
        }
        
        try {
            p.getService(null, "SHA-1");
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // ok
        }
    }

    @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getService",
            args = {java.lang.String.class, java.lang.String.class}
        )
    public final void testService2() {
        Provider[] pp = Security.getProviders("MessageDigest.SHA-1");
        if (pp == null) {
            return;
        }
        Provider p2 = pp[0];
        String old = p2.getProperty("MessageDigest.SHA-1");
        p2.put("MessageDigest.SHA-1", "AnotherClassName");
        Provider.Service s = p2.getService("MessageDigest", "SHA-1");
        if (!"AnotherClassName".equals(s.getClassName())) {
            fail("Incorrect class name " + s.getClassName());
        }
        try {
            s.newInstance(null);
            fail("No expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
        }
    }
     

    // Regression for HARMONY-2760.
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression test: verifies constructor with two null parameters.",
        method = "Provider",
        args = {java.lang.String.class, double.class, java.lang.String.class}
    )
    public void testConstructor() {
        MyProvider myProvider = new MyProvider(null, 1, null);
        assertNull(myProvider.getName());
        assertNull(myProvider.getInfo());
        assertEquals("null", myProvider.getProperty("Provider.id name"));
        assertEquals("null", myProvider.getProperty("Provider.id info"));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getServices",
        args = {}
    )
    public final void testGetServices() {
        MyProvider myProvider = new MyProvider(null, 1, null);
        Set<Provider.Service> services = myProvider.getServices();
        assertEquals(0, services.size());

        Provider.Service s[] = new Provider.Service[3];

        s[0] = new Provider.Service(p, "type1", "algorithm1", "className1",
                null, null);
        s[1] = new Provider.Service(p, "type2", "algorithm2", "className2",
                null, null);
        s[2] = new Provider.Service(p, "type3", "algorithm3", "className3",
                null, null);
        myProvider.putService(s[0]);
        myProvider.putService(s[1]);
        assertEquals(2, myProvider.getNumServices());
        Set<Service> actual = myProvider.getServices();

        assertTrue(actual.contains(s[0]));
        assertTrue(actual.contains(s[1]));
        assertTrue(!actual.contains(s[2]));

        myProvider.removeService(s[1]);
        actual = myProvider.getServices();
        assertEquals(1, myProvider.getNumServices());

        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(!actual.contains(s[2]));

        myProvider.putService(s[2]);
        actual = myProvider.getServices();
        assertEquals(2, myProvider.getNumServices());
        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "putService",
        args = {java.security.Provider.Service.class}
    )
    public final void testPutService() {
        MyProvider myProvider = new MyProvider(null, 1, null);
        Provider.Service s[] = new Provider.Service[3];

        s[0] = new Provider.Service(p, "type1", "algorithm1", "className1",
                null, null);
        s[1] = new Provider.Service(p, "type2", "algorithm2", "className2",
                null, null);
        s[2] = new Provider.Service(p, "type3", "algorithm3", "className3",
                null, null);
        myProvider.putService(s[0]);
        myProvider.putService(s[1]);
        assertEquals(2, myProvider.getNumServices());
        Set<Service> actual = myProvider.getServices();

        assertTrue(actual.contains(s[0]));
        assertTrue(actual.contains(s[1]));
        assertTrue(!actual.contains(s[2]));

        myProvider.removeService(s[1]);
        assertEquals(1, myProvider.getNumServices());
        actual = myProvider.getServices();

        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(!actual.contains(s[2]));

        myProvider.putService(s[2]);
        actual = myProvider.getServices();
        assertEquals(2, myProvider.getNumServices());
        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));

        myProvider.putService(s[2]);
        actual = myProvider.getServices();
        assertEquals(2, myProvider.getNumServices());
        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));

        try {
            myProvider.putService(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeService",
        args = {java.security.Provider.Service.class}
    )
    public final void testRemoveService() {
        MyProvider myProvider = new MyProvider(null, 1, null);
        try {
            myProvider.removeService(null);
            fail("NullPoiterException expected");
        } catch (NullPointerException e) {
            // expected
        }

        Provider.Service s[] = new Provider.Service[3];

        s[0] = new Provider.Service(p, "type0", "algorithm0", "className0",
                null, null);
        s[1] = new Provider.Service(p, "type1", "algorithm1", "className1",
                null, null);
        s[2] = new Provider.Service(p, "type2", "algorithm2", "className2",
                null, null);

        try {
            myProvider.removeService(s[0]);
        } catch (NullPointerException e) {
            fail("Unexpected exception");
        }

        myProvider.putService(s[0]);
        myProvider.putService(s[1]);
        myProvider.putService(s[2]);
        assertEquals(3, myProvider.getNumServices());
        Set<Service> actual = myProvider.getServices();

        assertTrue(actual.contains(s[0]));
        assertTrue(actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));

        myProvider.removeService(s[1]);
        assertEquals(2, myProvider.getNumServices());
        actual = myProvider.getServices();

        assertTrue(actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));

        myProvider.removeService(s[0]);
        assertEquals(1, myProvider.getNumServices());
        actual = myProvider.getServices();

        assertTrue(!actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(actual.contains(s[2]));

        myProvider.removeService(s[2]);
        assertEquals(0, myProvider.getNumServices());
        actual = myProvider.getServices();

        assertTrue(!actual.contains(s[0]));
        assertTrue(!actual.contains(s[1]));
        assertTrue(!actual.contains(s[2]));

        try {
            myProvider.removeService(null);
            fail("NullPoiterException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for void load(InputStream)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "load",
        args = {java.io.InputStream.class}
    )
    public final void testLoad() throws IOException {
        InputStream is = new ByteArrayInputStream(writeProperties());
        MyProvider myProvider = new MyProvider("name", 1, "info");
        myProvider.load(is);
        assertEquals("tests.security", myProvider.get("test.pkg"));
        assertEquals("Unit Tests", myProvider.get("test.proj"));
        assertNull(myProvider.get("#commented.entry"));

        assertEquals("info", myProvider.get("Provider.id info"));
        String className = myProvider.getClass().toString();
        assertEquals(
                className.substring("class ".length(), className.length()),
                myProvider.get("Provider.id className"));
        assertEquals("1.0", myProvider.get("Provider.id version"));

        try {
            myProvider.load(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "load",
            args = {java.io.InputStream.class}
        )    
    public final void testLoad2() {
        class TestInputStream extends InputStream {
            @Override
            public int read() throws IOException {
                throw new IOException();
            }
        }
        
        MyProvider p = new MyProvider();
        try {
            p.load(new TestInputStream());
            fail("expected IOException");
        } catch (IOException e) {
            // ok
        }
        
    }

    protected byte[] writeProperties() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        ps.println("#commented.entry=Bogus");
        ps.println("test.pkg=tests.security");
        ps.println("test.proj=Unit Tests");
        ps.close();
        return bout.toByteArray();
    }

    class MyProvider extends Provider {
       // private Set<Provider.Service> services = null;

        MyProvider() {
            super("MyProvider", 1.0, "Provider for testing");
            put("MessageDigest.SHA-1", "SomeClassName");
            put("MessageDigest.abc", "SomeClassName");
            put("Alg.Alias.MessageDigest.SHA1", "SHA-1");
        }

        MyProvider(String name, double version, String info) {
            super(name, version, info);
        }

        public void putService(Provider.Service s) {
            super.putService(s);
        }

        public void removeService(Provider.Service s) {
            super.removeService(s);
        }

        public int getNumServices() {
            return getServices().size();
        }
    }
    
    static class TestSecurityManager extends SecurityManager {
        boolean called = false;
        private final String permissionName;
        
        public TestSecurityManager(String permissionName) {
            this.permissionName = permissionName;
        }
        
        @Override
        public void checkPermission(Permission permission) {
            if (permission instanceof SecurityPermission) {
                if (permissionName.equals(permission.getName())) {
                    called = true;
                }
            }
        }
    }
}
