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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;
import tests.support.Support_OutputStream;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

/**
 * Automated Test Suite for class java.io.ByteArrayOutputStream
 * 
 * @see java.io.ByteArrayOutputStream
 */
@TestTargetClass(ByteArrayOutputStream.class) 
public class ByteArrayOutputStreamTest extends TestCase {

    ByteArrayOutputStream bos = null;

    public String fileString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_java_io_BufferedOutputStream\nTest_java_io_ByteArrayInputStream\nTest_ByteArrayOutputStream\nTest_java_io_DataInputStream\n";

    /**
     * @tests java.io.ByteArrayOutputStream#ByteArrayOutputStream(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "ByteArrayOutputStream",
        args = {int.class}
    )    
    public void test_ConstructorI() {
        bos = new java.io.ByteArrayOutputStream(100);
        assertEquals("Test 1: Failed to create stream;", 0, bos.size());
        
        try {
            bos = new ByteArrayOutputStream(-1);
            fail("Test 2: IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ByteArrayOutputStream#ByteArrayOutputStream()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "ByteArrayOutputStream",
        args = {}
    )           
    public void test_Constructor() {
        // Test for method java.io.ByteArrayOutputStream()
        bos = new java.io.ByteArrayOutputStream();
        assertEquals("Failed to create stream", 0, bos.size());
    }

    /**
     * @tests java.io.ByteArrayOutputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "No IOException check since it is never thrown.",
        method = "close",
        args = {}
    )           
    public void test_close() {
        // Test for method void java.io.ByteArrayOutputStream.close()

        assertTrue(
                "close() does nothing for this implementation of OutputSteam",
                true);

        // The spec seems to say that a closed output stream can't be written
        // to. We don't throw an exception if attempt is made to write.
        // Right now our implementation doesn't do anything testable but
        // should we decide to throw an exception if a closed stream is
        // written to, the appropriate test is commented out below.

        /***********************************************************************
         * java.io.ByteArrayOutputStream bos = new
         * java.io.ByteArrayOutputStream(); bos.write (fileString.getBytes(), 0,
         * 100); try { bos.close(); } catch (java.io.IOException e) {
         * fail("IOException closing stream"); } try { bos.write
         * (fileString.getBytes(), 0, 100); bos.toByteArray(); fail("Wrote
         * to closed stream"); } catch (Exception e) { }
         **********************************************************************/
    }

    /**
     * @tests java.io.ByteArrayOutputStream#reset()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "reset",
        args = {}
    )        
    public void test_reset() {
        // Test for method void java.io.ByteArrayOutputStream.reset()
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, 100);
        bos.reset();
        assertEquals("Test 1: Reset failed;", 0, bos.size());
    }

    /**
     * @tests java.io.ByteArrayOutputStream#size()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies size() method.",
        method = "size",
        args = {}
    )        
    public void test_size() {
        // Test for method int java.io.ByteArrayOutputStream.size()
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, 100);
        assertEquals("size test failed", 100, bos.size());
        bos.reset();
        assertEquals("size test failed", 0, bos.size());
    }

    /**
     * @tests java.io.ByteArrayOutputStream#toByteArray()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies toByteArray() method.",
        method = "toByteArray",
        args = {}
    )        
    public void test_toByteArray() {
        // Test for method byte [] java.io.ByteArrayOutputStream.toByteArray()
        byte[] bytes;
        byte[] sbytes = fileString.getBytes();
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, fileString.length());
        bytes = bos.toByteArray();
        for (int i = 0; i < fileString.length(); i++) {
            assertTrue("Error in byte array", bytes[i] == sbytes[i]);
        }
    }

    /**
     * @tests java.io.ByteArrayOutputStream#toString(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "toString",
        args = {java.lang.String.class}
    )     
    public void test_toStringLjava_lang_String() throws Exception {
        bos = new ByteArrayOutputStream();

        bos.write(fileString.getBytes(), 0, fileString.length());
        assertTrue("Test 1: Returned incorrect 8859-1 String", 
                bos.toString("8859_1").equals(fileString));
        assertTrue("Test 2: Returned incorrect 8859-2 String", 
                bos.toString("8859_2").equals(fileString));
        
        try {
            bos.toString("NotAnEcoding");
            fail("Test 3: UnsupportedEncodingException expected.");
        } catch (UnsupportedEncodingException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ByteArrayOutputStream#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies toString() method.",
        method = "toString",
        args = {}
    )        
    public void test_toString() {
        // Test for method java.lang.String
        // java.io.ByteArrayOutputStream.toString()
        java.io.ByteArrayOutputStream bos = null;
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, fileString.length());
        assertTrue("Returned incorrect String", bos.toString().equals(
                fileString));
    }

    /**
     * @tests java.io.ByteArrayOutputStream#toString(int)
     */
    @SuppressWarnings("deprecation")
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies toString(int hibyte) method.",
        method = "toString",
        args = {int.class}
    )    
    public void test_toStringI() {
        // Test for method java.lang.String
        // java.io.ByteArrayOutputStream.toString(int)
        java.io.ByteArrayOutputStream bos = null;
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, fileString.length());
        assertTrue("Returned incorrect String",
                bos.toString(5).length() == fileString.length());
    }

    /**
     * @tests java.io.ByteArrayOutputStream#write(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "write",
        args = {int.class}
    )     
    public void test_writeI() {
        // Test for method void java.io.ByteArrayOutputStream.write(int)
        bos = new java.io.ByteArrayOutputStream();
        bos.write('t');
        byte[] result = bos.toByteArray();
        assertEquals("Wrote incorrect bytes",
                "t", new String(result, 0, result.length));
    }

    /**
     * @tests java.io.ByteArrayOutputStream#write(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "write",
        args = {byte[].class, int.class, int.class}
    )        
    public void test_write$BII() {
        // Test for method void java.io.ByteArrayOutputStream.write(byte [],
        // int, int)
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, 100);
        byte[] result = bos.toByteArray();
        assertTrue("Wrote incorrect bytes",
                new String(result, 0, result.length).equals(fileString
                        .substring(0, 100)));
    }

    /**
     * @tests java.io.ByteArrayOutputStream#write(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Illegal argument checks.",
        method = "write",
        args = {byte[].class, int.class, int.class}
    )     
    public void test_write$BII_Exception() {
        byte[] target = new byte[10];
        bos = new ByteArrayOutputStream();
        try {
            bos.write(target, -1, 1);
            fail("Test 1: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            bos.write(target, 0, -1);
            fail("Test 2: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            bos.write(target, 1, target.length);
            fail("Test 3: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            bos.write(null, 1, 1);
            fail("Test 4: NullPointerException expected.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ByteArrayOutputStream#writeTo(java.io.OutputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "writeTo",
        args = {java.io.OutputStream.class}
    )       
    public void test_writeToLjava_io_OutputStream() throws Exception {
        Support_OutputStream sos = new Support_OutputStream();
        bos = new java.io.ByteArrayOutputStream();
        bos.write(fileString.getBytes(), 0, 10);
        bos.writeTo(sos);
        assertTrue("Test 1: Incorrect string written.", 
                sos.toString().equals(
                        fileString.substring(0, 10)));

        sos.setThrowsException(true);
        try {
            bos.writeTo(sos);
            fail("Test 2: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }
}
