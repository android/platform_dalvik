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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.IllegalFormatException;
import java.util.Locale;

import tests.support.Support_StringReader;
import tests.support.Support_StringWriter;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(PrintWriter.class) 
public class PrintWriterTest extends junit.framework.TestCase {

    private static class MockPrintWriter extends PrintWriter {

        public MockPrintWriter(OutputStream os) {
            super(os);
        }
        
        @Override
        public void setError() {
            super.setError();
        }
    }

    static class Bogus {
        public String toString() {
            return "Bogus";
        }
    }

    private File testFile = null;
    private String testFilePath = null;

    PrintWriter pw;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    ByteArrayInputStream bai;

    BufferedReader br;

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.OutputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.OutputStream.class}
    )
    public void test_ConstructorLjava_io_OutputStream() {
        // Test for method java.io.PrintWriter(java.io.OutputStream)
        String s;
        pw = new PrintWriter(baos);
        pw.println("Random Chars");
        pw.write("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s
                    .equals("Random Chars"));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s
                    .equals("Hello World"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.OutputStream, boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.OutputStream.class, boolean.class}
    )
    public void test_ConstructorLjava_io_OutputStreamZ() {
        // Test for method java.io.PrintWriter(java.io.OutputStream, boolean)
        String s;
        pw = new PrintWriter(baos, true);
        pw.println("Random Chars");
        pw.write("Hello World");
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s
                    .equals("Random Chars"));
            pw.flush();
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s
                    .equals("Random Chars"));
            s = br.readLine();
            assertTrue("Incorrect string written/read: " + s, s
                    .equals("Hello World"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.Writer)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.Writer.class}
    )
    public void test_ConstructorLjava_io_Writer() {
        // Test for method java.io.PrintWriter(java.io.Writer)
        Support_StringWriter sw;
        pw = new PrintWriter(sw = new Support_StringWriter());
        pw.print("Hello");
        pw.flush();
        assertEquals("Failed to construct proper writer", 
                "Hello", sw.toString());
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.Writer, boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.Writer.class, boolean.class}
    )
    public void test_ConstructorLjava_io_WriterZ() {
        // Test for method java.io.PrintWriter(java.io.Writer, boolean)
        Support_StringWriter sw;
        pw = new PrintWriter(sw = new Support_StringWriter(), true);
        pw.print("Hello");
        // Auto-flush should have happened
        assertEquals("Failed to construct proper writer", 
                "Hello", sw.toString());
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.File)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.File.class}
    )
    public void test_ConstructorLjava_io_File() throws Exception {
        PrintWriter tobj;

        tobj = new PrintWriter(testFile);
        tobj.write(1);
        tobj.close();
        assertEquals("output file has wrong length", 1, testFile.length());
        tobj = new PrintWriter(testFile);
        assertNotNull(tobj);
        tobj.close();
        assertEquals("output file should be empty", 0, testFile.length());

        File file = new File("/invalidDirectory/Dummy");
        try {
            tobj = new PrintWriter(file);
            fail("FileNotFoundException not thrown.");
        } catch (FileNotFoundException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.io.File, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.io.File.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_io_File_Ljava_lang_String() throws Exception {
        PrintWriter tobj;

        tobj = new PrintWriter(testFile, "utf-8");
        tobj.write(1);
        tobj.close();
        assertEquals("output file has wrong length", 1, testFile.length());
        tobj = new PrintWriter(testFile, "utf-8");
        assertNotNull(tobj);
        tobj.close();
        assertEquals("output file should be empty", 0, testFile.length());

        File file = new File("/invalidDirectory/Dummy");
        try {
            tobj = new PrintWriter(file, "utf-8");
            fail("FileNotFoundException not thrown.");
        } catch (FileNotFoundException e) {
            // expected
        }

        try {
            tobj = new PrintWriter(testFile, "invalidEncoding");
            fail("UnsupportedEncodingException not thrown.");
        } catch (UnsupportedEncodingException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() throws Exception {
        PrintWriter tobj;

        tobj = new PrintWriter(testFilePath);
        assertNotNull(tobj);
        tobj.write(1);
        tobj.close();
        assertEquals("output file has wrong length", 1, testFile.length());
        tobj = new PrintWriter(testFilePath);
        assertNotNull(tobj);
        tobj.close();
        assertEquals("output file should be empty", 0, testFile.length());

        try {
            tobj = new PrintWriter("/invalidDirectory/Dummy");
            fail("FileNotFoundException not thrown.");
        } catch (FileNotFoundException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#PrintWriter(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PrintWriter",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String_Ljava_lang_String() throws Exception {
        PrintWriter tobj;

        tobj = new PrintWriter(testFilePath, "utf-8");
        assertNotNull(tobj);
        tobj.write(1);
        tobj.close();
        assertEquals("output file has wrong length", 1, testFile.length());
        tobj = new PrintWriter(testFilePath, "utf-8");
        assertNotNull(tobj);
        tobj.close();
        assertEquals("output file should be empty", 0, testFile.length());

        try {
            tobj = new PrintWriter("/invalidDirectory/", "utf-8");
            fail("FileNotFoundException not thrown.");
        } catch (FileNotFoundException e) {
            // expected
        }

        try {
            tobj = new PrintWriter(testFilePath, "invalidEncoding");
            fail("UnsupportedEncodingException not thrown.");
        } catch (UnsupportedEncodingException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#checkError()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "checkError",
        args = {}
    )
    public void test_checkError() {
        // Test for method boolean java.io.PrintWriter.checkError()
        pw.close();
        pw.print(490000000000.08765);
        assertTrue("Failed to return error", pw.checkError());
    }

    /**
     * @tests java.io.PrintStream#setError()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setError",
        args = {}
    )
    public void test_setError() throws Exception {
        MockPrintWriter os = new MockPrintWriter(new ByteArrayOutputStream());
        assertFalse("Test 1: Error flag should not be set.", os.checkError());
        os.setError();
        assertTrue("Test 2: Error flag should be set.", os.checkError());
    }
    
    /**
     * @tests java.io.PrintWriter#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close() {
        // Test for method void java.io.PrintWriter.close()
        pw.close();
        pw.println("l");
        assertTrue("Write on closed stream failed to generate error", pw
                .checkError());
    }

    /**
     * @tests java.io.PrintWriter#flush()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "flush",
        args = {}
    )
    public void test_flush() {
        // Test for method void java.io.PrintWriter.flush()
        final double dub = 490000000000.08765;
        pw.print(dub);
        pw.flush();
        assertTrue("Failed to flush", new String(baos.toByteArray())
                .equals(String.valueOf(dub)));
    }

    /**
     * @tests java.io.PrintWriter#print(char[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {char[].class}
    )
    public void test_print$C() {
        // Test for method void java.io.PrintWriter.print(char [])
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.print(schars);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s
                .equals("Hello World"));
        int r = 0;
        try {
            pw.print((char[]) null);
        } catch (NullPointerException e) {
            r = 1;
        }
        assertEquals("null pointer exception for printing null char[] is not caught",
                1, r);
    }

    /**
     * @tests java.io.PrintWriter#print(char)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {char.class}
    )
    public void test_printC() {
        // Test for method void java.io.PrintWriter.print(char)
        pw.print('c');
        pw.flush();
        assertEquals("Wrote incorrect char string", "c", new String(baos.toByteArray())
                );
    }

    /**
     * @tests java.io.PrintWriter#print(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {double.class}
    )
    public void test_printD() {
        // Test for method void java.io.PrintWriter.print(double)
        final double dub = 490000000000.08765;
        pw.print(dub);
        pw.flush();
        assertTrue("Wrote incorrect double string", new String(baos
                .toByteArray()).equals(String.valueOf(dub)));
    }

    /**
     * @tests java.io.PrintWriter#print(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {float.class}
    )
    public void test_printF() {
        // Test for method void java.io.PrintWriter.print(float)
        final float flo = 49.08765f;
        pw.print(flo);
        pw.flush();
        assertTrue("Wrote incorrect float string",
                new String(baos.toByteArray()).equals(String.valueOf(flo)));
    }

    /**
     * @tests java.io.PrintWriter#print(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {int.class}
    )
    public void test_printI() {
        // Test for method void java.io.PrintWriter.print(int)
        pw.print(4908765);
        pw.flush();
        assertEquals("Wrote incorrect int string", "4908765", new String(baos.toByteArray())
                );
    }

    /**
     * @tests java.io.PrintWriter#print(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {long.class}
    )
    public void test_printJ() {
        // Test for method void java.io.PrintWriter.print(long)
        pw.print(49087650000L);
        pw.flush();
        assertEquals("Wrote incorrect long string", "49087650000", new String(baos.toByteArray())
                );
    }

    /**
     * @tests java.io.PrintWriter#print(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {java.lang.Object.class}
    )
    public void test_printLjava_lang_Object() {
        // Test for method void java.io.PrintWriter.print(java.lang.Object)
        pw.print((Object) null);
        pw.flush();
        assertEquals("Did not write null", "null", new String(baos.toByteArray()));
        baos.reset();

        pw.print(new Bogus());
        pw.flush();
        assertEquals("Wrote in incorrect Object string", "Bogus", new String(baos
                .toByteArray()));
    }

    /**
     * @tests java.io.PrintWriter#print(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {java.lang.String.class}
    )
    public void test_printLjava_lang_String() {
        // Test for method void java.io.PrintWriter.print(java.lang.String)
        pw.print((String) null);
        pw.flush();
        assertEquals("did not write null", "null", new String(baos.toByteArray()));
        baos.reset();

        pw.print("Hello World");
        pw.flush();
        assertEquals("Wrote incorrect  string", "Hello World", new String(baos.toByteArray()));
    }

    /**
     * @tests java.io.PrintWriter#print(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "print",
        args = {boolean.class}
    )
    public void test_printZ() {
        // Test for method void java.io.PrintWriter.print(boolean)
        pw.print(true);
        pw.flush();
        assertEquals("Wrote in incorrect boolean string", "true", new String(baos
                .toByteArray()));
    }

    /**
     * @tests java.io.PrintWriter#println()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {}
    )
    public void test_println() {
        // Test for method void java.io.PrintWriter.println()
        String s;
        pw.println("Blarg");
        pw.println();
        pw.println("Bleep");
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals("Blarg"));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals(""));
            s = br.readLine();
            assertTrue("Wrote incorrect line: " + s, s.equals("Bleep"));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PrintWriter#println(char[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {char[].class}
    )
    public void test_println$C() {
        // Test for method void java.io.PrintWriter.println(char [])
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.println(schars);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s
                .equals("Hello World"));
    }

    /**
     * @tests java.io.PrintWriter#println(char)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {char.class}
    )
    public void test_printlnC() {
        // Test for method void java.io.PrintWriter.println(char)
        String s = null;
        pw.println("Random Chars");
        pw.println('c');
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            s = br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char string: " + s, s.equals("c"));
    }

    /**
     * @tests java.io.PrintWriter#println(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {double.class}
    )
    public void test_printlnD() {
        // Test for method void java.io.PrintWriter.println(double)
        String s = null;
        final double dub = 4000000000000000.657483;
        pw.println("Random Chars");
        pw.println(dub);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect double string: " + s, s.equals(String
                .valueOf(dub)));
    }

    /**
     * @tests java.io.PrintWriter#println(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {float.class}
    )
    public void test_printlnF() {
        // Test for method void java.io.PrintWriter.println(float)
        String s;
        final float flo = 40.4646464f;
        pw.println("Random Chars");
        pw.println(flo);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
            assertTrue("Wrote incorrect float string: " + s + " wanted: "
                    + String.valueOf(flo), s.equals(String.valueOf(flo)));
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }

    }

    /**
     * @tests java.io.PrintWriter#println(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {int.class}
    )
    public void test_printlnI() {
        // Test for method void java.io.PrintWriter.println(int)
        String s = null;
        pw.println("Random Chars");
        pw.println(400000);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect int string: " + s, s.equals("400000"));
    }

    /**
     * @tests java.io.PrintWriter#println(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {long.class}
    )
    public void test_printlnJ() {
        // Test for method void java.io.PrintWriter.println(long)
        String s = null;
        pw.println("Random Chars");
        pw.println(4000000000000L);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect long string: " + s, s
                .equals("4000000000000"));
    }

    /**
     * @tests java.io.PrintWriter#println(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {java.lang.Object.class}
    )
    public void test_printlnLjava_lang_Object() {
        // Test for method void java.io.PrintWriter.println(java.lang.Object)
        String s = null;
        pw.println("Random Chars");
        pw.println(new Bogus());
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect Object string: " + s, s.equals("Bogus"));
    }

    /**
     * @tests java.io.PrintWriter#println(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {java.lang.String.class}
    )
    public void test_printlnLjava_lang_String() {
        // Test for method void java.io.PrintWriter.println(java.lang.String)
        String s = null;
        pw.println("Random Chars");
        pw.println("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect string: " + s, s.equals("Hello World"));
    }

    /**
     * @tests java.io.PrintWriter#println(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {boolean.class}
    )
    public void test_printlnZ() {
        // Test for method void java.io.PrintWriter.println(boolean)
        String s = null;
        pw.println("Random Chars");
        pw.println(false);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect boolean string: " + s, s.equals("false"));
    }

    /**
     * @tests java.io.PrintWriter#write(char[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class}
    )
    public void test_write$C() {
        // Test for method void java.io.PrintWriter.write(char [])
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.write(schars);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test: " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s
                .equals("Hello World"));
    }

    /**
     * @tests java.io.PrintWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    public void test_write$CII() {
        // Test for method void java.io.PrintWriter.write(char [], int, int)
        String s = null;
        char[] schars = new char[11];
        "Hello World".getChars(0, 11, schars, 0);
        pw.println("Random Chars");
        pw.write(schars, 6, 5);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("World"));
    }

    /**
     * @tests java.io.PrintWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    public void test_write$CII_Exception() {
        // Test for method void java.io.PrintWriter.write(char [], int, int)
        char[] chars = new char[10];
        try {
            pw.write(chars, 0, -1);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            pw.write(chars, -1, 1);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            pw.write(chars, 10, 1);
            fail("IndexOutOfBoundsException was not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }

    /**
     * @tests java.io.PrintWriter#write(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {int.class}
    )
    public void test_writeI() {
        // Test for method void java.io.PrintWriter.write(int)
        char[] cab = new char[3];
        pw.write('a');
        pw.write('b');
        pw.write('c');
        pw.flush();
        bai = new ByteArrayInputStream(baos.toByteArray());
        cab[0] = (char) bai.read();
        cab[1] = (char) bai.read();
        cab[2] = (char) bai.read();
        assertTrue("Wrote incorrect ints", cab[0] == 'a' && cab[1] == 'b'
                && cab[2] == 'c');

    }

    /**
     * @tests java.io.PrintWriter#write(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {java.lang.String.class}
    )
    public void test_writeLjava_lang_String() {
        // Test for method void java.io.PrintWriter.write(java.lang.String)
        String s = null;
        pw.println("Random Chars");
        pw.write("Hello World");
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s
                .equals("Hello World"));
    }

    /**
     * @tests java.io.PrintWriter#write(java.lang.String, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void test_writeLjava_lang_StringII() {
        // Test for method void java.io.PrintWriter.write(java.lang.String, int,
        // int)
        String s = null;
        pw.println("Random Chars");
        pw.write("Hello World", 6, 5);
        pw.flush();
        try {
            br = new BufferedReader(new Support_StringReader(baos.toString()));
            br.readLine();
            s = br.readLine();
        } catch (IOException e) {
            fail("IOException during test : " + e.getMessage());
        }
        assertTrue("Wrote incorrect char[] string: " + s, s.equals("World"));
    }
    
    /**
     * @tests java.io.PrintWriter#append(char)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "append",
        args = {char.class}
    )
    public void test_appendChar() {
    char testChar = ' ';
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(out);
    printWriter.append(testChar);
    printWriter.flush();
    assertEquals(String.valueOf(testChar),out.toString());
    printWriter.close();
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
    public void test_appendCharSequence() {
        
        String testString = "My Test String";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.append(testString);
        printWriter.flush();
        assertEquals(testString, out.toString());
        printWriter.close();    

    }

    /**
     *  @tests java.io.PrintWriter#append(CharSequence, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "append",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    public void test_appendCharSequenceIntInt() {
        String testString = "My Test String";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.append(testString, 1, 3);
        printWriter.flush();
        assertEquals(testString.substring(1, 3), out.toString());
        try {
            printWriter.append(testString, 4, 100);
            fail("IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            printWriter.append(testString, 100, 1);
            fail("IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        printWriter.close();
    }

    /**
     * @tests java.io.PrintWriter#format(java.lang.String, java.lang.Object...)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "format",
        args = {java.lang.String.class, java.lang.Object[].class}
    )
    public void test_formatLjava_lang_String$Ljava_lang_Object() {
        PrintWriter tobj;
        
        tobj = new PrintWriter(baos, false);
        tobj.format("%s %s", "Hello", "World");
        tobj.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        byte[] rbytes = new byte[11];
        bis.read(rbytes, 0, rbytes.length);
        assertEquals("Wrote incorrect string", "Hello World",
                new String(rbytes));

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.format("%1$.3G, %1$.5f, 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1.23E+04, 12345.67800, 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        try {
            tobj.format("%1$.3G, %1$x", 12345.678);
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.format("%s %q", "Hello", "World");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.format("%s %s", "Hello");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#format(java.util.Locale, java.lang.String, java.lang.Object...)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "format",
        args = {java.util.Locale.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void test_formatLjava_util_Locale_Ljava_lang_String_$Ljava_lang_Object() {
        PrintWriter tobj;

        tobj = new PrintWriter(baos, false);
        tobj.format(Locale.US, "%s %s", "Hello", "World");
        tobj.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        byte[] rbytes = new byte[11];
        bis.read(rbytes, 0, rbytes.length);
        assertEquals("Wrote incorrect string", "Hello World",
                new String(rbytes));

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.format(Locale.GERMANY, "%1$.3G; %1$.5f; 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1,23E+04; 12345,67800; 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.format(Locale.US, "%1$.3G, %1$.5f, 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1.23E+04, 12345.67800, 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        try {
            tobj.format(Locale.US, "%1$.3G, %1$x", 12345.678);
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.format(Locale.US, "%s %q", "Hello", "World");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.format(Locale.US, "%s %s", "Hello");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#printf(java.lang.String, java.lang.Object...)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "printf",
        args = {java.lang.String.class, java.lang.Object[].class}
    )
    public void test_printfLjava_lang_String$Ljava_lang_Object() {
        PrintWriter tobj;

        tobj = new PrintWriter(baos, false);
        tobj.printf("%s %s", "Hello", "World");
        tobj.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        byte[] rbytes = new byte[11];
        bis.read(rbytes, 0, rbytes.length);
        assertEquals("Wrote incorrect string", "Hello World",
                new String(rbytes));

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.printf("%1$.3G, %1$.5f, 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1.23E+04, 12345.67800, 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        try {
            tobj.printf("%1$.3G, %1$x", 12345.678);
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.printf("%s %q", "Hello", "World");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.printf("%s %s", "Hello");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }
    }

    /**
     * @tests java.io.PrintWriter#printf(java.util.Locale, java.lang.String, java.lang.Object...)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "printf",
        args = {java.util.Locale.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void test_printfLjava_util_Locale_Ljava_lang_String_$Ljava_lang_Object() {
        PrintWriter tobj;

        tobj = new PrintWriter(baos, false);
        tobj.printf(Locale.US, "%s %s", "Hello", "World");
        tobj.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        byte[] rbytes = new byte[11];
        bis.read(rbytes, 0, rbytes.length);
        assertEquals("Wrote incorrect string", "Hello World",
                new String(rbytes));

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.printf(Locale.GERMANY, "%1$.3G; %1$.5f; 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1,23E+04; 12345,67800; 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        tobj.printf(Locale.US, "%1$.3G, %1$.5f, 0%2$xx", 12345.678, 123456);
        tobj.flush();
        assertEquals("Wrong output!", "1.23E+04, 12345.67800, 01e240x", new String(baos.toByteArray()));
        tobj.close();

        baos.reset();
        tobj = new PrintWriter(baos);
        try {
            tobj.printf(Locale.US, "%1$.3G, %1$x", 12345.678);
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.printf(Locale.US, "%s %q", "Hello", "World");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }

        try {
            tobj.printf(Locale.US, "%s %s", "Hello");
            fail("IllegalFormatException not thrown");
        } catch (IllegalFormatException e) {
            // expected
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() throws Exception {
        testFile = File.createTempFile("test", null);
        testFilePath = testFile.getAbsolutePath();
        pw = new PrintWriter(baos, false);

    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    @Override
    protected void tearDown() throws Exception {
        testFile.delete();
        testFile = null;
        testFilePath = null;
        try {
            pw.close();
        } catch (Exception e) {
        }
    }
}
