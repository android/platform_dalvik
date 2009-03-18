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

import java.io.CharArrayReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;

import tests.support.Support_ASimpleReader;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(PushbackReader.class) 
public class PushbackReaderTest extends junit.framework.TestCase {

    Support_ASimpleReader underlying = new Support_ASimpleReader();
    PushbackReader pbr;

    String pbString = "Hello World";

    /**
     * @tests java.io.PushbackReader#PushbackReader(java.io.Reader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "PushbackReader",
        args = {java.io.Reader.class}
    )
    public void test_ConstructorLjava_io_Reader() {
        // Test for method java.io.PushbackReader(java.io.Reader)
        try {
            pbr.close();
            pbr = new PushbackReader(new StringReader(pbString));
            char buf[] = new char[5];
            pbr.read(buf, 0, 5);
            pbr.unread(buf);
            fail("Created reader with buffer larger than 1");;
        } catch (IOException e) {
            // Expected
        }
        
        try {
            pbr = new PushbackReader(null);
        } catch (NullPointerException e) {
            // EXpected
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#PushbackReader(java.io.Reader, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "PushbackReader",
        args = {java.io.Reader.class, int.class}
    )
    public void test_ConstructorLjava_io_ReaderI() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying, 10000);
        tobj = new PushbackReader(underlying, 1);

        try {
            tobj = new PushbackReader(underlying, -1);
            tobj.close();
            fail("IOException not thrown.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            tobj = new PushbackReader(underlying, 0);
            tobj.close();
            fail("IOException not thrown.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "close",
        args = {}
    )
    public void test_close() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying);
        tobj.close();
        tobj.close();
        tobj = new PushbackReader(underlying);
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.close();
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method void java.io.PushbackReader.close()
        try {
            pbr.close();
            pbr.read();
        } catch (Exception e) {
            return;
        }
        fail("Failed to throw exception reading from closed reader");

    }

    /**
     * @tests java.io.PushbackReader#mark(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "mark",
        args = {int.class}
    )
    public void test_markI() {
        try {
            pbr.mark(3);
            fail("Test 1: IOException expected because marking is not supported.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PushbackReader#markSupported()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "markSupported",
        args = {}
    )
    public void test_markSupported() {
        assertFalse("Test 1: markSupported() must return false.", 
                pbr.markSupported());
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#read()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "read",
        args = {}
    )
    public void test_read() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying);
        assertEquals("Wrong value read!", 66, tobj.read());
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.read();
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method int java.io.PushbackReader.read()
        try {
            char c;
            pbr.read();
            c = (char) pbr.read();
            assertTrue("Failed to read char: " + c, c == pbString.charAt(1));
            Reader reader = new PushbackReader(new CharArrayReader(
                    new char[] { '\u8765' }));
            assertTrue("Wrong double byte character", reader.read() == '\u8765');
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "read",
        args = {char[].class, int.class, int.class}
    )
    public void test_read$CII() throws IOException {
        PushbackReader tobj;
        char[] buf = ("01234567890123456789").toCharArray();

        tobj = new PushbackReader(underlying);
        tobj.read(buf, 6, 5);
        assertEquals("Wrong value read!", "BEGIN", new String(buf, 6, 5));
        assertEquals("Too much read!", "012345BEGIN123456789", new String(buf));
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.read(buf, 6, 5);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method int java.io.PushbackReader.read(char [], int, int)
        try {
            char[] c = new char[5];
            pbr.read(c, 0, 5);
            assertTrue("Failed to read chars", new String(c).equals(pbString
                    .substring(0, 5)));

            assertEquals(0, pbr.read(c, 0, 0));
            assertEquals(c.length, pbr.read(c, 0, c.length));
            assertEquals(0, pbr.read(c, c.length, 0));
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }
    }
    
    /**
     * @tests java.io.PushbackReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks exceptions.",
        method = "read",
        args = {char[].class, int.class, int.class}
    )
    public void test_read_$CII_Exception() throws IOException {
        pbr = new PushbackReader(new StringReader(pbString), 10);
        
        char[] nullCharArray = null;
        char[] charArray = new char[10];
        
        try {
            pbr.read(nullCharArray, 0, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, 0, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, -1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, charArray.length + 1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, charArray.length, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, 1, charArray.length);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        
        try {
            pbr.read(charArray, 0, charArray.length + 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        pbr.close();

        try {
            pbr.read(charArray, 0, 1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#ready()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Could also add tests where underlying says no but push back buffer has contents.",
        method = "ready",
        args = {}
    )
    public void test_ready() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying);
        assertTrue("Should be ready!", tobj.ready());
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.ready();
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method boolean java.io.PushbackReader.ready()
        try {
            char[] c = new char[11];
            if (c.length > 0)
                ;// use c to avoid warning msg
            assertTrue("Ready stream returned false to ready()", pbr.ready());
        } catch (IOException e) {
            fail("IOException during ready() test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PushbackReader#reset()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "reset",
        args = {}
    )
    public void test_reset() {
        try {
            pbr.reset();
        } catch (IOException e) {
            // correct
            return;
        }
        fail("reset failed to throw expected IOException");
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#unread(char[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "unread",
        args = {char[].class}
    )
    public void test_unread$C() throws IOException {
        PushbackReader tobj;
        String str2 = "0123456789";
        char[] buf2 = str2.toCharArray();
        char[] readBuf = new char[10];

        tobj = new PushbackReader(underlying, 10);
        tobj.unread(buf2);
        try {
            tobj.unread(buf2);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }
        tobj.read(readBuf);
        assertEquals("Incorrect bytes read", str2, new String(readBuf));
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.read(buf2);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method void java.io.PushbackReader.unread(char [])
        try {
            char[] c = new char[5];
            pbr.read(c, 0, 5);
            pbr.unread(c);
            pbr.read(c, 0, 5);
            assertTrue("Failed to unread chars", new String(c).equals(pbString
                    .substring(0, 5)));
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#skip(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "skip",
        args = {long.class}
    )
    public void test_skip$J() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying);
        tobj.skip(6);
        tobj.skip(1000000);
        tobj.skip(1000000);
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.skip(1);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        char chars[] = new char[] { 'h', 'e', 'l', 'l', 'o' };
        for (int i = 0; i < 3; i++) {
            Reader reader, reader2;
            switch (i) {
            case 0:
                reader = new StringReader(new String(chars));
                reader2 = new StringReader(new String(chars));
                break;
            case 1:
                reader = new FilterReader(new StringReader(new String(chars))) {
                };
                reader2 = new FilterReader(new StringReader(new String(chars))) {
                };
                break;
            default:
                reader = new CharArrayReader(chars);
                reader2 = new CharArrayReader(chars);
            }
            PushbackReader pReader = new PushbackReader(reader, 2);
            PushbackReader pReader2 = new PushbackReader(reader2, 2);
            boolean skipped = false;
            long numSkipped = 0;
            try {
                numSkipped = pReader2.skip(3);
                pReader2.unread('a');
                pReader2.unread('b');
                numSkipped += pReader2.skip(10);
                numSkipped += pReader2.skip(10);
                numSkipped += pReader2.skip(10);
                numSkipped += pReader2.skip(10);
                numSkipped += pReader2.skip(10);
                numSkipped += pReader2.skip(10);
                assertEquals("Did not skip correct number of characters",
                        7, numSkipped);
                numSkipped = 0;
                numSkipped += pReader.skip(2);
                pReader.unread('i');
                numSkipped += pReader.skip(2);
                numSkipped += pReader.skip(0);
                skipped = true;
                numSkipped += pReader.skip(-1);
                fail("Failed to throw "
                        + new IllegalArgumentException().getClass().getName());
            } catch (IllegalArgumentException e) {
                assertTrue("Failed to skip characters" + e, skipped);
            } catch (IOException e) {
                fail("Failed to skip characters" + e);
            }
            try {
                numSkipped += pReader.skip(1);
                numSkipped += pReader.skip(1);
                numSkipped += pReader.skip(1);
                assertEquals("Failed to skip all characters", 6, numSkipped);
                long nextSkipped = pReader.skip(1);
                assertEquals("skipped empty reader", 0, nextSkipped);
            } catch (IOException e) {
                fail("Failed to skip more characters" + e);
            }
        }
    }

    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#unread(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "unread",
        args = {char[].class, int.class, int.class}
    )
    public void test_unread$CII() throws IOException {
        PushbackReader tobj;
        String str2 = "0123456789";
        char[] buf2 = (str2 + str2 + str2).toCharArray();
        char[] readBuf = new char[10];

        tobj = new PushbackReader(underlying, 10);
        tobj.unread(buf2, 15, 10);
        try {
            tobj.unread(buf2, 15, 10);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }
        tobj.read(readBuf);
        assertEquals("Incorrect bytes read", "5678901234", new String(readBuf));
        underlying.throwExceptionOnNextUse = true;
        try {
            tobj.read(buf2, 15, 10);
            fail("IOException not thrown.");
        } catch (IOException e) {
            // expected
        }

        // Test for method void java.io.PushbackReader.unread(char [], int, int)
        try {
            char[] c = new char[5];
            pbr.read(c, 0, 5);
            pbr.unread(c, 0, 2);
            pbr.read(c, 0, 5);
            assertTrue("Failed to unread chars", new String(c).equals(pbString
                    .substring(0, 2)
                    + pbString.substring(5, 8)));
        } catch (IOException e) {
            fail("IOException during unread test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PushbackReader#unread(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "unread",
        args = {char[].class, int.class, int.class}
    )
    public void test_unread_$CII_NullPointerException() throws IOException {
        //a pushback reader with one character buffer
        pbr = new PushbackReader(new StringReader(pbString));
        
        try {
            pbr.unread(null, 0, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * @tests java.io.PushbackReader#unread(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "unread",
        args = {char[].class, int.class, int.class}
    )
    public void test_unread_$CII_Exception_InsufficientBuffer() throws IOException {
        //a pushback reader with one character buffer
        pbr = new PushbackReader(new StringReader(pbString));
        
        //if count > buffer's size , should throw IOException
        try {
            pbr.unread(new char[pbString.length()], 0, 2);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }
    
    /**
     * @tests java.io.PushbackReader#unread(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "unread",
        args = {char[].class, int.class, int.class}
    )
    public void test_unread_$CII_ArrayIndexOutOfBoundsException() throws IOException {
        //a pushback reader with one character buffer
        pbr = new PushbackReader(new StringReader(pbString));

        try {
            pbr.unread(new char[pbString.length()], -1 , 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            pbr.unread(new char[pbString.length()], 0 , -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            pbr.unread(new char[10], 10 , 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
    
    /**
     * @throws IOException 
     * @tests java.io.PushbackReader#unread(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "unread",
        args = {int.class}
    )
    public void test_unreadI() throws IOException {
        PushbackReader tobj;

        tobj = new PushbackReader(underlying);
        tobj.unread(23); // Why does this work?!?
        tobj.skip(2);
        tobj.unread(23);
        assertEquals("Wrong value read!", 23, tobj.read());
        tobj.unread(13);
        try {
            tobj.unread(13);
            fail("IOException not thrown (ACTUALLY NOT SURE WHETHER IT REALLY MUST BE THROWN!).");
        } catch (IOException e) {
            // expected
        }

        // Test for method void java.io.PushbackReader.unread(int)
        try {
            int c;
            pbr.read();
            c = pbr.read();
            pbr.unread(c);
            assertTrue("Failed to unread char", pbr.read() == c);
        } catch (IOException e) {
            fail("IOException during unread test : " + e.getMessage());
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        pbr = new PushbackReader(new StringReader(pbString), 10);
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        try {
            pbr.close();
        } catch (IOException e) {
        }
    }
}
