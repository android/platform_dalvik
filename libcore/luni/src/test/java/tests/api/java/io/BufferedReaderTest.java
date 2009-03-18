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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.Reader;

import tests.support.Support_ASimpleReader;
import tests.support.Support_StringReader;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(BufferedReader.class) 
public class BufferedReaderTest extends junit.framework.TestCase {

    BufferedReader br;

    String testString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_java_io_BufferedOutputStream\nTest_java_io_ByteArrayInputStream\nTest_java_io_ByteArrayOutputStream\nTest_java_io_DataInputStream\nTest_java_io_File\nTest_java_io_FileDescriptor\nTest_java_io_FileInputStream\nTest_java_io_FileNotFoundException\nTest_java_io_FileOutputStream\nTest_java_io_FilterInputStream\nTest_java_io_FilterOutputStream\nTest_java_io_InputStream\nTest_java_io_IOException\nTest_java_io_OutputStream\nTest_java_io_PrintStream\nTest_java_io_RandomAccessFile\nTest_java_io_SyncFailedException\nTest_java_lang_AbstractMethodError\nTest_java_lang_ArithmeticException\nTest_java_lang_ArrayIndexOutOfBoundsException\nTest_java_lang_ArrayStoreException\nTest_java_lang_Boolean\nTest_java_lang_Byte\nTest_java_lang_Character\nTest_java_lang_Class\nTest_java_lang_ClassCastException\nTest_java_lang_ClassCircularityError\nTest_java_lang_ClassFormatError\nTest_java_lang_ClassLoader\nTest_java_lang_ClassNotFoundException\nTest_java_lang_CloneNotSupportedException\nTest_java_lang_Double\nTest_java_lang_Error\nTest_java_lang_Exception\nTest_java_lang_ExceptionInInitializerError\nTest_java_lang_Float\nTest_java_lang_IllegalAccessError\nTest_java_lang_IllegalAccessException\nTest_java_lang_IllegalArgumentException\nTest_java_lang_IllegalMonitorStateException\nTest_java_lang_IllegalThreadStateException\nTest_java_lang_IncompatibleClassChangeError\nTest_java_lang_IndexOutOfBoundsException\nTest_java_lang_InstantiationError\nTest_java_lang_InstantiationException\nTest_java_lang_Integer\nTest_java_lang_InternalError\nTest_java_lang_InterruptedException\nTest_java_lang_LinkageError\nTest_java_lang_Long\nTest_java_lang_Math\nTest_java_lang_NegativeArraySizeException\nTest_java_lang_NoClassDefFoundError\nTest_java_lang_NoSuchFieldError\nTest_java_lang_NoSuchMethodError\nTest_java_lang_NullPointerException\nTest_java_lang_Number\nTest_java_lang_NumberFormatException\nTest_java_lang_Object\nTest_java_lang_OutOfMemoryError\nTest_java_lang_RuntimeException\nTest_java_lang_SecurityManager\nTest_java_lang_Short\nTest_java_lang_StackOverflowError\nTest_java_lang_String\nTest_java_lang_StringBuffer\nTest_java_lang_StringIndexOutOfBoundsException\nTest_java_lang_System\nTest_java_lang_Thread\nTest_java_lang_ThreadDeath\nTest_java_lang_ThreadGroup\nTest_java_lang_Throwable\nTest_java_lang_UnknownError\nTest_java_lang_UnsatisfiedLinkError\nTest_java_lang_VerifyError\nTest_java_lang_VirtualMachineError\nTest_java_lang_vm_Image\nTest_java_lang_vm_MemorySegment\nTest_java_lang_vm_ROMStoreException\nTest_java_lang_vm_VM\nTest_java_lang_Void\nTest_java_net_BindException\nTest_java_net_ConnectException\nTest_java_net_DatagramPacket\nTest_java_net_DatagramSocket\nTest_java_net_DatagramSocketImpl\nTest_java_net_InetAddress\nTest_java_net_NoRouteToHostException\nTest_java_net_PlainDatagramSocketImpl\nTest_java_net_PlainSocketImpl\nTest_java_net_Socket\nTest_java_net_SocketException\nTest_java_net_SocketImpl\nTest_java_net_SocketInputStream\nTest_java_net_SocketOutputStream\nTest_java_net_UnknownHostException\nTest_java_util_ArrayEnumerator\nTest_java_util_Date\nTest_java_util_EventObject\nTest_java_util_HashEnumerator\nTest_java_util_Hashtable\nTest_java_util_Properties\nTest_java_util_ResourceBundle\nTest_java_util_tm\nTest_java_util_Vector\n";

    /**
     * @tests java.io.BufferedReader#BufferedReader(java.io.Reader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "BufferedReader",
        args = {java.io.Reader.class}
    )         
    public void test_ConstructorLjava_io_Reader() {
        // Test for method java.io.BufferedReader(java.io.Reader)
        br = new BufferedReader(new Support_StringReader(testString));
        assertNotNull(br);
    }

    /**
     * @tests java.io.BufferedReader#BufferedReader(java.io.Reader, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "BufferedReader",
        args = {java.io.Reader.class, int.class}
    )         
    public void test_ConstructorLjava_io_ReaderI() {
        // Illegal negative size argument test.
        try {
            br = new BufferedReader(new Support_StringReader(testString), 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
        br = new BufferedReader(new Support_StringReader(testString), 1024);
        assertNotNull(br);
    }

    /**
     * @tests java.io.BufferedReader#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "close",
        args = {}
    )         
    public void test_close() {
        Support_ASimpleReader ssr = new Support_ASimpleReader(true);
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            br.close();
            br.read();
            fail("Test 1: Read on closed stream.");
        } catch (IOException x) {
            // Expected.
        } catch (Exception e) {
            fail("Exception during close test " + e.toString());
        }
        
        br = new BufferedReader(ssr);
        try {
            br.close();
            fail("Test 2: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        // Avoid IOException in tearDown().
        ssr.throwExceptionOnNextUse = false;
    }

    /**
     * @tests java.io.BufferedReader#mark(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "mark",
        args = {int.class}
    )    
    public void test_markI() {
        // Test for method void java.io.BufferedReader.mark(int)
        char[] buf = null;
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            br.skip(500);
            br.mark(1000);
            br.skip(250);
            br.reset();
            buf = new char[testString.length()];
            br.read(buf, 0, 500);
            assertTrue("Failed to set mark properly", testString.substring(500,
                    1000).equals(new String(buf, 0, 500)));
        } catch (java.io.IOException e) {
            fail("Exception during mark test");
        }
        try {
            br = new BufferedReader(new Support_StringReader(testString), 800);
            br.skip(500);
            br.mark(250);
            br.read(buf, 0, 1000);
            br.reset();
            fail("Failed to invalidate mark properly");
        } catch (IOException x) {
        }

        char[] chars = new char[256];
        for (int i = 0; i < 256; i++)
            chars[i] = (char) i;
        Reader in = new BufferedReader(new Support_StringReader(new String(
                chars)), 12);
        try {
            in.skip(6);
            in.mark(14);
            in.read(new char[14], 0, 14);
            in.reset();
            assertTrue("Wrong chars", in.read() == (char) 6
                    && in.read() == (char) 7);
        } catch (IOException e) {
            fail("Exception during mark test 2:" + e);
        }

        in = new BufferedReader(new Support_StringReader(new String(chars)), 12);
        try {
            in.skip(6);
            in.mark(8);
            in.skip(7);
            in.reset();
            assertTrue("Wrong chars 2", in.read() == (char) 6
                    && in.read() == (char) 7);
        } catch (IOException e) {
            fail("Exception during mark test 3:" + e);
        }
    }

    /**
     * @tests java.io.BufferedReader#markSupported()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "markSupported",
        args = {}
    )    
    public void test_markSupported() {
        // Test for method boolean java.io.BufferedReader.markSupported()
        br = new BufferedReader(new Support_StringReader(testString));
        assertTrue("markSupported returned false.", br.markSupported());
    }

    /**
     * @tests java.io.BufferedReader#read()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "read",
        args = {}
    )         
    public void test_read() throws IOException {
        Support_ASimpleReader ssr = new Support_ASimpleReader(true);
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            int r = br.read();
            assertTrue("Char read improperly", testString.charAt(0) == r);
            br = new BufferedReader(new Support_StringReader(new String(
                    new char[] { '\u8765' })));
            assertTrue("Wrong double byte character", br.read() == '\u8765');
        } catch (java.io.IOException e) {
            fail("Exception during read test");
        }

        char[] chars = new char[256];
        for (int i = 0; i < 256; i++)
            chars[i] = (char) i;
        Reader in = new BufferedReader(new Support_StringReader(new String(
                chars)), 12);
        try {
            assertEquals("Wrong initial char", 0, in.read()); // Fill the
            // buffer
            char[] buf = new char[14];
            in.read(buf, 0, 14); // Read greater than the buffer
            assertTrue("Wrong block read data", new String(buf)
                    .equals(new String(chars, 1, 14)));
            assertEquals("Wrong chars", 15, in.read()); // Check next byte
        } catch (IOException e) {
            fail("Exception during read test 2:" + e);
        }
        
        // regression test for HARMONY-841
        assertTrue(new BufferedReader(new CharArrayReader(new char[5], 1, 0), 2).read() == -1);

        br.close();
        br = new BufferedReader(ssr);
        try {
            br.read();
            fail("IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        // Avoid IOException in tearDown().
        ssr.throwExceptionOnNextUse = false;
    }

    /**
     * @tests java.io.BufferedReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "The test verifies read(char[] cbuf, int off, int len) method.",
        method = "read",
        args = {char[].class, int.class, int.class}
    )    
    public void test_read$CII() throws Exception {
        char[] ca = new char[2];
        BufferedReader toRet = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(new byte[0])));
        try {
            toRet.close();
        } catch (IOException e) {
            fail("unexpected 1: " + e);
        }

        /* Closed reader should throw IOException reading zero bytes */
        try {
            toRet.read(ca, 0, 0);
            fail("Reading zero bytes on a closed reader should not work");
        } catch (IOException e) {
            // expected
        }

        // Test to ensure that a drained stream returns 0 at EOF
        toRet = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(new byte[2])));
        try {
            assertEquals("Emptying the reader should return two bytes", 2,
                    toRet.read(ca, 0, 2));
            assertEquals("EOF on a reader should be -1", -1, toRet.read(ca, 0,
                    2));
            assertEquals("Reading zero bytes at EOF should work", 0, toRet
                    .read(ca, 0, 0));
        } catch (IOException ex) {
            fail("Unexpected IOException : " + ex.getLocalizedMessage());
        }

        // Test for method int java.io.BufferedReader.read(char [], int, int)
        try {
            char[] buf = new char[testString.length()];
            br = new BufferedReader(new Support_StringReader(testString));
            br.read(buf, 50, 500);
            assertTrue("Chars read improperly", new String(buf, 50, 500)
                    .equals(testString.substring(0, 500)));

            br = new BufferedReader(new Support_StringReader(testString));
            assertEquals(0, br.read(buf, 0, 0));
            assertEquals(buf.length, br.read(buf, 0, buf.length));
            assertEquals(0, br.read(buf, buf.length, 0));
        } catch (java.io.IOException e) {
            fail("Exception during read test");
        }
        
        BufferedReader bufin = new BufferedReader(new Reader() {
            int size = 2, pos = 0;

            char[] contents = new char[size];

            public int read() throws IOException {
                if (pos >= size)
                    throw new IOException("Read past end of data");
                return contents[pos++];
            }

            public int read(char[] buf, int off, int len) throws IOException {
                if (pos >= size)
                    throw new IOException("Read past end of data");
                int toRead = len;
                if (toRead > (size - pos))
                    toRead = size - pos;
                System.arraycopy(contents, pos, buf, off, toRead);
                pos += toRead;
                return toRead;
            }

            public boolean ready() throws IOException {
                return size - pos > 0;
            }

            public void close() throws IOException {
            }
        });
        try {
            bufin.read();
            int result = bufin.read(new char[2], 0, 2);
            assertTrue("Incorrect result: " + result, result == 1);
        } catch (IOException e) {
            fail("Unexpected: " + e);
        }
    }

    /**
     * @tests java.io.BufferedReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "The test verifies read(char[] cbuf, int off, int len) method.",
        method = "read",
        args = {char[].class, int.class, int.class}
    )    
    public void test_read$CII_Exception() throws Exception {
        br = new BufferedReader(new Support_StringReader(testString));
        try{
            br.read(new char[10], -1, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try{
            br.read(new char[10], 0, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try{
            br.read(new char[10], 10, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        
        //regression for HARMONY-831
        try{
            new BufferedReader(new PipedReader(), 9).read(new char[] {}, 7, 0);
            fail("should throw IndexOutOfBoundsException");
        }catch(IndexOutOfBoundsException e){
        }
    }

    /**
     * @tests java.io.BufferedReader#readLine()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "readLine",
        args = {}
    )    
    public void test_readLine() throws IOException {
        String line;
        br = new BufferedReader(new Support_StringReader("Lorem\nipsum\rdolor sit amet..."));
        
        line = br.readLine();
        assertTrue("Test 1: Incorrect line written or read: " + line, 
                line.equals("Lorem"));
        line = br.readLine();
        assertTrue("Test 2: Incorrect line written or read: " + line, 
                line.equals("ipsum"));
        line = br.readLine();
        assertTrue("Test 3: Incorrect line written or read: " + line, 
                line.equals("dolor sit amet..."));
        
        br.close();
        try {
            br.readLine();
            fail("Test 4: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.BufferedReader#ready()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "ready",
        args = {}
    )    
    public void test_ready() throws IOException {
        Support_ASimpleReader ssr = new Support_ASimpleReader(true);
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            assertTrue("Test 1: ready() returned false", br.ready());
        } catch (java.io.IOException e) {
            fail("Exception during ready test" + e.toString());
        }

        br.close();
        br = new BufferedReader(ssr);
        try {
            br.close();
            fail("Test 2: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        // Avoid IOException in tearDown().
        ssr.throwExceptionOnNextUse = false;
    }

    /**
     * @tests java.io.BufferedReader#reset()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "The test verifies reset() method.",
        method = "reset",
        args = {}
    )
    public void test_reset() {
        // Test for method void java.io.BufferedReader.reset()
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            br.skip(500);
            br.mark(900);
            br.skip(500);
            br.reset();
            char[] buf = new char[testString.length()];
            br.read(buf, 0, 500);
            assertTrue("Failed to reset properly", testString.substring(500,
                    1000).equals(new String(buf, 0, 500)));
        } catch (java.io.IOException e) {
            fail("Exception during reset test");
        }
        try {
            br = new BufferedReader(new Support_StringReader(testString));
            br.skip(500);
            br.reset();
            fail("Reset succeeded on unmarked stream");
        } catch (IOException x) {
            return;

        }

    }

    /**
     * @tests java.io.BufferedReader#skip(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "skip",
        args = {long.class}
    )    
    public void test_skipJ() throws IOException {
        Support_ASimpleReader ssr = new Support_ASimpleReader(true);
        br = new BufferedReader(new Support_StringReader(testString));
        
        try {
            br.skip(-1);
            fail("Test 1: IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        br.skip(500);
        char[] buf = new char[testString.length()];
        br.read(buf, 0, 500);
        assertTrue("Test 2: Failed to set skip properly.", 
                testString.substring(500, 1000).equals(
                        new String(buf, 0, 500)));
        
        br.close();
        br = new BufferedReader(ssr);
        try {
            br.skip(1);
            fail("Test 3: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        // Avoid IOException in tearDown().
        ssr.throwExceptionOnNextUse = false;
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        try {
            br.close();
        } catch (Exception e) {
        }
    }
}
