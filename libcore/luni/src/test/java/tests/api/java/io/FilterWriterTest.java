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

import tests.api.java.io.FilterReaderTest.MyFilterReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(FilterWriter.class) 
public class FilterWriterTest extends junit.framework.TestCase {

    private boolean called;
    private FilterWriter fw;
   
    static class MyFilterWriter extends java.io.FilterWriter {
        public MyFilterWriter(java.io.Writer writer) {
            super(writer);
        }
    }

    class MockWriter extends java.io.Writer {
        public MockWriter() {
        }
        
        public void close() throws IOException {
            called = true;
        }
        
        public void flush() throws IOException {
            called = true;
        }
        
        public void write(char[] buffer, int offset, int count) throws IOException {
            called = true;
        }

        public void write(int oneChar) throws IOException {
            called = true;
        }
        
        public void write(String str, int offset, int count) throws IOException {
            called = true;
        }
        
        public long skip(long count) throws IOException {
            called = true;
            return 0;
        }
    }
    
    /**
     * @tests java.io.FilterWriter#FilterReader(java.io.Reader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies constructor FilterWriter(java.io.Writer).",
        method = "FilterWriter",
        args = {java.io.Writer.class}
    )     
    public void test_ConstructorLjava_io_Writer() {
        
        FilterWriter myWriter = null;

        called = true;
        
        try {
            myWriter = new MyFilterWriter(null);
            fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * @tests java.io.FilterWriter#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies close().",
        method = "close",
        args = {}
    )     
    public void test_close() throws IOException {
        fw.close();
        assertTrue("close() has not been called.", called);
    }
    
    /**
     * @tests java.io.FilterWriter#flush()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies flush().",
        method = "flush",
        args = {}
    )     
    public void test_flush() throws IOException {
        fw.flush();
        assertTrue("flush() has not been called.", called);
    }
    
    /**
     * @tests java.io.FilterWriter#write(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies write(int).",
        method = "write",
        args = {int.class}
    )     
    public void test_writeI() throws IOException {
        fw.write(0);
        assertTrue("write(int) has not been called.", called);
    }
    
    /**
     * @tests java.io.FilterWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies write(char[], int, int).",
        method = "write",
        args = {char[].class, int.class, int.class}
    )     
    public void test_write$CII() throws IOException {
        char[] buffer = new char[5];       
        fw.write(buffer, 0, 5);
        assertTrue("write(char[], int, int) has not been called.", called);
    }
    
    /**
     * @tests java.io.FilterReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies write(char[], int, int).",
        method = "write",
        args = {char[].class, int.class, int.class}
    )     
    public void test_write$CII_Exception() throws IOException {
        char[] buffer = new char[10];
        
        fw = new MyFilterWriter(new OutputStreamWriter(
            new ByteArrayOutputStream()));

        try {
            fw.write(buffer, 0, -1);
            fail("Test 1: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }

        try {
            fw.write(buffer, -1, 1);
            fail("Test 2: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }

        try {
            fw.write(buffer, 10, 1);
            fail("Test 3: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
    }
    
    /**
     * @tests java.io.FilterWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies write(String, int, int).",
        method = "write",
        args = {java.lang.String.class, int.class, int.class}
    )     
    public void test_writeLjava_lang_StringII() throws IOException {
        fw.write("Hello world", 0, 5);
        assertTrue("write(String, int, int) has not been called.", called);
    }
        
    /**
     * This method is called before a test is executed. It creates a
     * FilterWriter instance.
     */
    protected void setUp() {

        fw = new MyFilterWriter(new MockWriter());
        called = false;
    }

    /**
     * This method is called after a test is executed. It closes the
     * FilterWriter instance.
     */
    protected void tearDown() {

        try {
            fw.close();
        } catch (Exception e) {
            System.out.println("Exception during FilterWriterTest tear down.");
        }
    }
}
