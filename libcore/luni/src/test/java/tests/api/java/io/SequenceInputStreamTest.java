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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;

import tests.support.Support_ASimpleInputStream;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(SequenceInputStream.class) 
public class SequenceInputStreamTest extends junit.framework.TestCase {

    Support_ASimpleInputStream simple1, simple2;
    SequenceInputStream si;
    String s1 = "Hello";
    String s2 = "World";

    /**
     * @tests SequenceInputStream#SequenceInputStream(java.io.InputStream,
     *        java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Checks NullPointerException. A positive test of this " +
                "constructor is implicitely done in setUp(); if it would " +
                "fail, all other tests will also fail.",
        method = "SequenceInputStream",
        args = {java.io.InputStream.class, java.io.InputStream.class}
    )
    public void test_Constructor_LInputStreamLInputStream_Null() {        
        try {
            si = new SequenceInputStream(null , null);
            fail("Test 1: NullPointerException expected.");
        } catch (NullPointerException e) {
            // Expected.
        }
        
        //will not throw NullPointerException if the first InputStream is not null
        InputStream is = new ByteArrayInputStream(s1.getBytes()); 
        si = new SequenceInputStream(is , null);
    }

    /**
     * @tests java.io.SequenceInputStream#SequenceInputStream(java.util.Enumeration)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "SequenceInputStream",
        args = {java.util.Enumeration.class}
    )
    public void test_ConstructorLjava_util_Enumeration() {
        // Test for method java.io.SequenceInputStream(java.util.Enumeration)
        class StreamEnumerator implements Enumeration<InputStream> {
            InputStream streams[] = new InputStream[2];

            int count = 0;

            public StreamEnumerator() {
                streams[0] = new ByteArrayInputStream(s1.getBytes());
                streams[1] = new ByteArrayInputStream(s2.getBytes());
            }

            public boolean hasMoreElements() {
                return count < streams.length;
            }

            public InputStream nextElement() {
                return streams[count++];
            }
        }

        try {
            si = new SequenceInputStream(new StreamEnumerator());
            byte buf[] = new byte[s1.length() + s2.length()];
            si.read(buf, 0, s1.length());
            si.read(buf, s1.length(), s2.length());
            assertTrue("Read incorrect bytes: " + new String(buf), new String(
                    buf).equals(s1 + s2));
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }

    }

    /**
     * @throws IOException 
     * @tests java.io.SequenceInputStream#available()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "available",
        args = {}
    )
    public void test_available() throws IOException {
        assertEquals("Returned incorrect number of bytes!", s1.length(), si.available());
        simple2.throwExceptionOnNextUse = true;
        assertTrue("IOException on second stream should not affect at this time!",
                si.available() == s1.length());
        simple1.throwExceptionOnNextUse = true;
        try {
            si.available();
            fail("IOException not thrown!");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * @tests java.io.SequenceInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close() throws IOException {
        assertTrue("Something is available!", si.available() > 0);
        si.close();
        //will not throw IOException to close a stream which is closed already
        si.close();
        assertTrue("Nothing is available, now!", si.available() <= 0);
//        assertEquals("And not on the underlying streams either!", 0, simple1.available());
//        assertTrue("And not on the underlying streams either!", simple1.available() <= 0);
//        assertTrue("And not on the underlying streams either!", simple2.available() <= 0);
    }

    /**
     * @tests java.io.SequenceInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close2() throws IOException {
        simple1.throwExceptionOnNextUse = true;
        try {
            si.close();
            fail("IOException not thrown!");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * @tests java.io.SequenceInputStream#read()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "read",
        args = {}
    )
    public void test_read() throws IOException {
        si.read();
        assertEquals("Test 1: Incorrect char read;", 
                s1.charAt(1), (char) si.read());
        
        // We are still reading from the first input stream, should be ok.
        simple2.throwExceptionOnNextUse = true;
        try {
            assertEquals("Test 2: Incorrect char read;", 
                    s1.charAt(2), (char) si.read());
        } catch (IOException e) {
            fail("Test 3: Unexpected IOException.");
        }

        simple1.throwExceptionOnNextUse = true;
        try {
            si.read();
            fail("Test 4: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        simple1.throwExceptionOnNextUse = false;
        
        // Reading bytes 4 and 5 of the first input stream should be ok again.
        si.read();
        si.read();

        // Reading the first byte of the second input stream should fail.
        try {
            si.read();
            fail("Test 5: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        
        // Reading from the second input stream should be ok now.
        simple2.throwExceptionOnNextUse = false;
        try {
            assertEquals("Test 6: Incorrect char read;", 
                    s2.charAt(0), (char) si.read());
        } catch (IOException e) {
            fail("Test 7: Unexpected IOException.");
        }

        si.close();
        assertTrue("Test 8: -1 expected when reading from a closed " + 
                   "sequence input stream.", si.read() == -1);        
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read_exc() throws IOException {
        simple2.throwExceptionOnNextUse = true;
        assertEquals("IOException on second stream should not affect at this time!", 72, si.read());
        simple1.throwExceptionOnNextUse = true;
        try {
            si.read();
            fail("IOException not thrown!");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * @tests java.io.SequenceInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII() throws IOException {
        // Test for method int java.io.SequenceInputStream.read(byte [], int,
        // int)
        try {
            byte buf[] = new byte[s1.length() + s2.length()];
            si.read(buf, 0, s1.length());
            si.read(buf, s1.length(), s2.length());
            assertTrue("Read incorrect bytes: " + new String(buf), new String(
                    buf).equals(s1 + s2));
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }
        
        ByteArrayInputStream bis1 = new ByteArrayInputStream(
                new byte[] { 1, 2, 3, 4 });
        ByteArrayInputStream bis2 = new ByteArrayInputStream(
                new byte[] { 5, 6, 7, 8 });
        SequenceInputStream sis = new SequenceInputStream(bis1, bis2);

        try {
            sis.read(null, 0, 2);
            fail("Expected NullPointerException exception");
        } catch (NullPointerException e) {
            // expected
        }
        
        assertEquals(4, sis.read(new byte[10], 0, 8));
        // The call to read will return after the end of the first substream is
        // reached. So the next call to read will close the first substream
        // because the read call to that substream will return -1, and
        // it will continue reading from the second substream.
        assertEquals(5, sis.read());
        
        //returns -1 if the stream is closed , do not throw IOException
        byte[] array = new byte[] { 1 , 2 , 3 ,4 };
        sis.close();
        int result = sis.read(array , 0 , 5);
        assertEquals(-1 , result);    
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII_Excpetion() throws IOException {
        byte[] buf = new byte[4];
        si.read(buf, 0, 2);
        si.read(buf, 2, 1);
        simple2.throwExceptionOnNextUse = true;
        si.read(buf, 3, 1);
        assertEquals("Wrong stuff read!", "Hell", new String(buf));
        simple1.throwExceptionOnNextUse = true;
        try {
            si.read(buf, 3, 1);
            fail("IOException not thrown!");
        } catch (IOException e) {
            // expected
        }

        buf = new byte[10];
        simple1 = new Support_ASimpleInputStream(s1);
        simple2 = new Support_ASimpleInputStream(s2);
        si = new SequenceInputStream(simple1, simple2);
        try {
            si.read(buf, -1, 1);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            si.read(buf, 0, -1);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            si.read(buf, 1, 10);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        simple1 = new Support_ASimpleInputStream(s1);
        simple2 = new Support_ASimpleInputStream(s2);
        si = new SequenceInputStream(simple1, simple2);
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
    }
}
