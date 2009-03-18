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

package tests.api.java.util;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import tests.support.resource.Support_Resources;

@TestTargetClass(Properties.class) 
public class PropertiesTest extends junit.framework.TestCase {

    Properties tProps;

    byte[] propsFile;

    /**
     * @tests java.util.Properties#Properties()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Properties",
        args = {}
    )
    public void test_Constructor() {
        Properties p = new Properties();
        // do something to avoid getting a variable unused warning
        p.clear();
    }

    /**
     * @tests java.util.Properties#Properties(java.util.Properties)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Properties",
        args = {java.util.Properties.class}
    )
    public void test_ConstructorLjava_util_Properties() {
        if (System.getProperty("java.vendor") != null) {
            Properties p = new Properties(System.getProperties());
            assertNotNull("failed to construct correct properties", p
                    .getProperty("java.vendor"));
        }
    }

    /**
     * @tests java.util.Properties#getProperty(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies positive case.",
        method = "getProperty",
        args = {java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_String() {
        assertEquals("Did not retrieve property", "this is a test property",
                tProps.getProperty("test.prop"));
    }

    /**
     * @tests java.util.Properties#getProperty(java.lang.String,
     *        java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_StringLjava_lang_String() {
        assertEquals("Did not retrieve property", "this is a test property",
                tProps.getProperty("test.prop", "Blarg"));
        assertEquals("Did not return default value", "Gabba", tProps
                .getProperty("notInThere.prop", "Gabba"));
        assertNull(tProps.getProperty("", null));
    }

    /**
     * @tests java.util.Properties#getProperty(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression test.",
        method = "getProperty",
        args = {java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_String2() {
        // regression test for HARMONY-3518
        MyProperties props = new MyProperties();
        assertNull(props.getProperty("key"));
    }

    /**
     * @tests java.util.Properties#getProperty(java.lang.String,
     *        java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression test.",
        method = "getProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_StringLjava_lang_String2() {
        // regression test for HARMONY-3518
        MyProperties props = new MyProperties();
        assertEquals(props.getProperty("key", "defaultValue"), "defaultValue");
    }

    // regression testing for HARMONY-3518
    static class MyProperties extends Properties {
        public synchronized Object get(Object key) {
            return getProperty((String) key); // assume String
        }
    }

    /**
     * @tests java.util.Properties#list(java.io.PrintStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "list",
        args = {java.io.PrintStream.class}
    )
    public void test_listLjava_io_PrintStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Properties myProps = new Properties();
        String propList;
        myProps.setProperty("Abba", "Cadabra");
        myProps.setProperty("Open", "Sesame");
        myProps.list(ps);
        ps.flush();
        propList = baos.toString();
        assertTrue("Property list innacurate", (propList
                .indexOf("Abba=Cadabra") >= 0)
                && (propList.indexOf("Open=Sesame") >= 0));
    }

    /**
     * @tests java.util.Properties#list(java.io.PrintWriter)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "list",
        args = {java.io.PrintWriter.class}
    )
    public void test_listLjava_io_PrintWriter() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        Properties myProps = new Properties();
        String propList;
        myProps.setProperty("Abba", "Cadabra");
        myProps.setProperty("Open", "Sesame");
        myProps.list(pw);
        pw.flush();
        propList = baos.toString();
        assertTrue("Property list innacurate", (propList
                .indexOf("Abba=Cadabra") >= 0)
                && (propList.indexOf("Open=Sesame") >= 0));
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#load(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "load",
        args = {java.io.InputStream.class}
    )
    public void test_loadLjava_io_InputStream() throws IOException {
        Properties prop = new Properties();
        InputStream is = new ByteArrayInputStream(writeProperties());
        prop.load(is);
        is.close();
        
        assertEquals("Failed to load correct properties", "harmony.tests", prop
                .getProperty("test.pkg"));
        assertNull("Load failed to parse incorrectly", prop
                .getProperty("commented.entry"));

        prop = new Properties();
        prop.load(new ByteArrayInputStream("=".getBytes()));
        assertTrue("Failed to add empty key", prop.get("").equals(""));

        prop = new Properties();
        prop.load(new ByteArrayInputStream(" = ".getBytes()));
        assertTrue("Failed to add empty key2", prop.get("").equals(""));

        prop = new Properties();
        prop.load(new ByteArrayInputStream(" a= b".getBytes()));
        assertEquals("Failed to ignore whitespace", "b", prop.get("a"));

        prop = new Properties();
        prop.load(new ByteArrayInputStream(" a b".getBytes()));
        assertEquals("Failed to interpret whitespace as =", "b", prop.get("a"));

        prop = new Properties();
        prop.load(new ByteArrayInputStream("#\u008d\u00d2\na=\u008d\u00d3"
                .getBytes("ISO8859_1")));
        assertEquals("Failed to parse chars >= 0x80", "\u008d\u00d3", prop
                .get("a"));

        prop = new Properties();
        prop.load(new ByteArrayInputStream(
                "#properties file\r\nfred=1\r\n#last comment"
                        .getBytes("ISO8859_1")));
        assertEquals("Failed to load when last line contains a comment", "1",
                prop.get("fred"));

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{'\\', 'u', 'x', 'x', 'x', 'x'});
        try {
            prop.load(bais);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#load(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify IOException, IllegalArgumentException.",
        method = "load",
        args = {java.io.InputStream.class}
    )
    public void test_loadLjava_io_InputStream_subtest0() throws IOException {
        InputStream is = Support_Resources
                .getStream("hyts_PropertiesTest.properties");
        Properties props = new Properties();
        props.load(is);
        is.close();
        assertEquals("1", "\n \t \f", props.getProperty(" \r"));
        assertEquals("2", "a", props.getProperty("a"));
        assertEquals("3", "bb as,dn   ", props.getProperty("b"));
        assertEquals("4", ":: cu", props.getProperty("c\r \t\nu"));
        assertEquals("5", "bu", props.getProperty("bu"));
        assertEquals("6", "d\r\ne=e", props.getProperty("d"));
        assertEquals("7", "fff", props.getProperty("f"));
        assertEquals("8", "g", props.getProperty("g"));
        assertEquals("9", "", props.getProperty("h h"));
        assertEquals("10", "i=i", props.getProperty(" "));
        assertEquals("11", "   j", props.getProperty("j"));
        assertEquals("12", "   c", props.getProperty("space"));
        assertEquals("13", "\\", props.getProperty("dblbackslash"));
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#save(java.io.OutputStream, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "save",
        args = {java.io.OutputStream.class, java.lang.String.class}
    )
    public void test_saveLjava_io_OutputStreamLjava_lang_String()
            throws IOException {
        Properties myProps = new Properties();
        myProps.setProperty("Property A", "aye");
        myProps.setProperty("Property B", "bee");
        myProps.setProperty("Property C", "see");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        myProps.save(out, "A Header");
        out.close();

        Properties myProps2 = new Properties();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        myProps2.load(in);
        in.close();

        Enumeration e = myProps.propertyNames();
        while (e.hasMoreElements()) {
            String nextKey = (String) e.nextElement();
            assertTrue("Stored property list not equal to original", myProps2
                    .getProperty(nextKey).equals(myProps.getProperty(nextKey)));
        }
    }

    /**
     * @tests java.util.Properties#setProperty(java.lang.String,
     *        java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_setPropertyLjava_lang_StringLjava_lang_String() {
        Properties myProps = new Properties();
        myProps.setProperty("Yoink", "Yabba");
        assertEquals("Failed to set property", "Yabba", myProps
                .getProperty("Yoink"));
        myProps.setProperty("Yoink", "Gab");
        assertEquals("Failed to reset property", "Gab", myProps
                .getProperty("Yoink"));
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#store(java.io.OutputStream, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "store",
        args = {java.io.OutputStream.class, java.lang.String.class}
    )
    public void test_storeLjava_io_OutputStreamLjava_lang_String()
            throws IOException {
        Properties myProps = new Properties();
        myProps.put("Property A", " aye\\\f\t\n\r\b");
        myProps.put("Property B", "b ee#!=:");
        myProps.put("Property C", "see");

        Properties myProps2 = new Properties();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        myProps.store(out, "A Header");
        myProps.store(out, null);
        out.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        myProps2.load(in);
        in.close();

        Enumeration e = myProps.propertyNames();
        while (e.hasMoreElements()) {
            String nextKey = (String) e.nextElement();
            assertTrue("Stored property list not equal to original", myProps2
                    .getProperty(nextKey).equals(myProps.getProperty(nextKey)));
        }
        
        try {
            myProps.store(null, "String");
            fail("NullPointerException expected");
        } catch (NullPointerException ee){
            //expected
        }
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#loadFromXML(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "loadFromXML",
        args = {java.io.InputStream.class}
    )
    @KnownFailure("Expected Exception is not thrown.")
    public void test_loadFromXMLLjava_io_InputStream() throws IOException {
        Properties myProps = new Properties();
        myProps.put("Property A", " aye\\\f\t\n\r\b");
        myProps.put("Property B", "b ee#!=:");
        myProps.put("Property C", "see");
        Properties myProps2 = new Properties();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        myProps.storeToXML(out, "A Header");
        out.close();
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try {
            myProps2.loadFromXML(in);
            fail("InvalidPropertiesFormatException expected");
        } catch (InvalidPropertiesFormatException e) {
            //expected
        }
        in.close();
        
        
        Properties prop = new Properties();
        InputStream is = new ByteArrayInputStream(writePropertiesXML("UTF-8"));
        prop.loadFromXML(is);
        is.close();

        assertEquals("Failed to load correct properties", "value3", prop
                .getProperty("key3"));
        assertEquals("Failed to load correct properties", "value1", prop
                .getProperty("key1"));

        prop = new Properties();
        is = new ByteArrayInputStream(writePropertiesXML("ISO-8859-1"));
        prop.loadFromXML(is);
        is.close();

        assertEquals("Failed to load correct properties", "value2", prop
                .getProperty("key2"));
        assertEquals("Failed to load correct properties", "value1", prop
                .getProperty("key1"));
        
        try {
            prop.loadFromXML(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            //expected
        }
    }

    /**
     * @throws IOException
     * @tests java.util.Properties#storeToXML(java.io.OutputStream,
     *        java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "storeToXML",
        args = {java.io.OutputStream.class, java.lang.String.class, java.lang.String.class}
    )
    public void test_storeToXMLLjava_io_OutputStreamLjava_lang_StringLjava_lang_String()
            throws IOException {
        Properties myProps = new Properties();
        myProps.setProperty("key1", "value1");
        myProps.setProperty("key2", "value2");
        myProps.setProperty("key3", "value3");
        myProps.setProperty("<a>key4</a>", "\"value4");
        myProps.setProperty("key5   ", "<h>value5</h>");
        myProps.setProperty("<a>key6</a>", "   <h>value6</h>   ");
        myProps.setProperty("<comment>key7</comment>", "value7");
        myProps.setProperty("  key8   ", "<comment>value8</comment>");
        myProps.setProperty("&lt;key9&gt;", "\u0027value9");
        myProps.setProperty("key10\"", "&lt;value10&gt;");
        myProps.setProperty("&amp;key11&amp;", "value11");
        myProps.setProperty("key12", "&amp;value12&amp;");
        myProps.setProperty("<a>&amp;key13&lt;</a>",
                "&amp;&value13<b>&amp;</b>");

        // store in UTF-8 encoding
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        myProps.storeToXML(out, "comment");
        out.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Properties myProps2 = new Properties();
        myProps2.loadFromXML(in);
        in.close();

        Enumeration e = myProps.propertyNames();
        while (e.hasMoreElements()) {
            String nextKey = (String) e.nextElement();
            assertTrue("Stored property list not equal to original", myProps2
                    .getProperty(nextKey).equals(myProps.getProperty(nextKey)));
        }

        // store in ISO-8859-1 encoding
        out = new ByteArrayOutputStream();
        myProps.storeToXML(out, "comment", "ISO-8859-1");
        out.close();

        in = new ByteArrayInputStream(out.toByteArray());
        myProps2 = new Properties();
        myProps2.loadFromXML(in);
        in.close();

        e = myProps.propertyNames();
        while (e.hasMoreElements()) {
            String nextKey = (String) e.nextElement();
            assertTrue("Stored property list not equal to original", myProps2
                    .getProperty(nextKey).equals(myProps.getProperty(nextKey)));
        }

        out = new ByteArrayOutputStream();
        myProps.storeToXML(out, "comment", "ISO-8859-1");
        myProps.storeToXML(out, null, "ISO-8859-1");
        out.close();
        
        try {
            myProps.storeToXML(out, "comment", null);
            fail("NulPointerException expected");
        } catch (NullPointerException ee) {
            //expected
        }
        
        try {
            myProps.storeToXML(null, "comment", "ISO-8859-1");
            fail("NulPointerException expected");
        } catch (NullPointerException ee) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "storeToXML",
        args = {java.io.OutputStream.class, java.lang.String.class}
    )
    public void test_storeToXMLLjava_io_OutputStreamLjava_lang_String()
    throws IOException {
        Properties myProps = new Properties();
        myProps.put("Property A", "value 1");
        myProps.put("Property B", "value 2");
        myProps.put("Property C", "value 3");
        
        Properties myProps2 = new Properties();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        myProps.storeToXML(out, "A Header");
        out.close();
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        myProps2.loadFromXML(in);
        in.close();
        
        Enumeration e = myProps.propertyNames();
        while (e.hasMoreElements()) {
            String nextKey = (String) e.nextElement();
            assertTrue("Stored property list not equal to original", myProps2
                    .getProperty(nextKey).equals(myProps.getProperty(nextKey)));
        }
        
        try {
            myProps.storeToXML(null, "String");
            fail("NullPointerException expected");
        } catch (NullPointerException ee){
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "propertyNames",
        args = {}
    )
    public void test_propertyNames() {
        Properties myProps = new Properties();
        myProps.put("Property A", "value 1");
        myProps.put("Property B", "value 2");
        myProps.put("Property C", "value 3");
        
        Enumeration e = myProps.propertyNames();
        
        int count = 0;
        while (e.hasMoreElements()) {
            count++;
            assertTrue(myProps.containsKey(e.nextElement()));
        }
        
        assertTrue(myProps.size() == count);
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() throws Exception {
        super.setUp();
        tProps = new Properties();
        tProps.put("test.prop", "this is a test property");
        tProps.put("bogus.prop", "bogus");
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        tProps = null;
        super.tearDown();
    }

    protected byte[] writeProperties() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        ps.println("#commented.entry=Bogus");
        ps.println("test.pkg=harmony.tests");
        ps.println("test.proj=Automated Tests");
        ps.close();
        return bout.toByteArray();
    }

    protected byte[] writePropertiesXML(String encoding) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout, true, encoding);
        ps.println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
        ps
                .println("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">");
        ps.println("<properties>");
        ps.println("<comment>comment</comment>");
        ps.println("<entry key=\"key4\">value4</entry>");
        ps.println("<entry key=\"key3\">value3</entry>");
        ps.println("<entry key=\"key2\">value2</entry>");
        ps.println("<entry key=\"key1\"><!-- xml comment -->value1</entry>");
        ps.println("</properties>");
        ps.close();
        return bout.toByteArray();
    }
}
