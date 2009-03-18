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

package tests.api.java.io;

import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;

import java.io.IOException;
import java.io.StringWriter;

@TestTargetClass(StringWriter.class) 
public class StringWriterTest extends junit.framework.TestCase {

    StringWriter sw;

    /**
     * @tests java.io.StringWriter#StringWriter()
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "StringWriter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "StringWriter",
            args = {int.class}
        )
    })
    public void test_Constructor() {
        new StringWriter();
        new StringWriter(10);
        new StringWriter(10000);
    }

    /**
     * @tests java.io.StringWriter#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "There won't be IOException, ever!",
        method = "close",
        args = {}
    )
    public void test_close() {
        // Test for method void java.io.StringWriter.close()
        try {
            sw.close();
        } catch (IOException e) {
            fail("IOException closing StringWriter : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.StringWriter#flush()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "flush",
        args = {}
    )
    public void test_flush() {
        // Test for method void java.io.StringWriter.flush()
        sw.flush();
        sw.write('c');
        assertEquals("Failed to flush char", "c", sw.toString());
    }

    /**
     * @tests java.io.StringWriter#getBuffer()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getBuffer",
        args = {}
    )
    public void test_getBuffer() {
        // Test for method java.lang.StringBuffer
        // java.io.StringWriter.getBuffer()

        sw.write("This is a test string");
        StringBuffer sb = sw.getBuffer();
        assertEquals("Incorrect buffer returned", 
                "This is a test string", sb.toString());
    }

    /**
     * @tests java.io.StringWriter#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        // Test for method java.lang.String java.io.StringWriter.toString()
        sw.write("This is a test string");
        assertEquals("Incorrect string returned", 
                "This is a test string", sw.toString());
    }

    /**
     * @tests java.io.StringWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    public void test_write$CII() {
        // Test for method void java.io.StringWriter.write(char [], int, int)
        char[] c = new char[1000];
        "This is a test string".getChars(0, 21, c, 0);
        sw.write(c, 4, 14);
        assertEquals("Chars not written properly", 
                " is a test str", sw.toString());
    }

    /**
     * @tests java.io.StringWriter#write(char[], int, int)
     * Regression for HARMONY-387
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    public void test_write$CII_Exception() {
        StringWriter obj = new StringWriter();
        try {
            obj.write(new char[10], 0, -1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            obj.write(new char[10], -1, 1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            obj.write(new char[10], 2, 9);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }

    /**
     * @tests java.io.StringWriter#write(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {int.class}
    )
    public void test_writeI() {
        // Test for method void java.io.StringWriter.write(int)
        sw.write('c');
        assertEquals("Char not written properly", "c", sw.toString());
    }

    /**
     * @tests java.io.StringWriter#write(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {java.lang.String.class}
    )
    public void test_writeLjava_lang_String() {
        // Test for method void java.io.StringWriter.write(java.lang.String)
        sw.write("This is a test string");
        assertEquals("String not written properly", 
                "This is a test string", sw.toString());
    }

    /**
     * @tests java.io.StringWriter#write(java.lang.String, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void test_writeLjava_lang_StringII() {
        // Test for method void java.io.StringWriter.write(java.lang.String,
        // int, int)
        sw.write("This is a test string", 2, 2);
        assertEquals("String not written properly", "is", sw.toString());
    }
    
    /**
     * @tests java.io.StringWriter#append(char)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "append",
        args = {char.class}
    )
    public void test_appendChar() throws IOException {
        char testChar = ' ';
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testChar);
        assertEquals(String.valueOf(testChar), stringWriter.toString());
        stringWriter.close();
    }

    /**
     * @tests java.io.PrintWriter#append(CharSequence)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "append",
        args = {java.lang.CharSequence.class}
    )
    public void test_appendCharSequence() throws IOException {

        String testString = "My Test String";
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testString);
        assertEquals(String.valueOf(testString), stringWriter.toString());
        stringWriter.close();
    }

    /**
     * @tests java.io.PrintWriter#append(CharSequence, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "append",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    public void test_appendCharSequenceIntInt() throws IOException {
        String testString = "My Test String";
        StringWriter stringWriter = new StringWriter(20);
        stringWriter.append(testString, 1, 3);
        assertEquals(testString.substring(1, 3), stringWriter.toString());
        stringWriter.close();

        try {
            StringWriter tobj = new StringWriter(9);
            tobj.append("01234567890123456789", 19, 2);
            fail("IndexOutOfBoundsException not thrown!");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            StringWriter tobj = new StringWriter(9);
            tobj.append("01234567890123456789", 29, 2);
            fail("IndexOutOfBoundsException not thrown!");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {

        sw = new StringWriter();
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
    }
}
