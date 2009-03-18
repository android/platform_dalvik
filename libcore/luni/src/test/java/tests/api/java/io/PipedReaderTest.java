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

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass; 

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

@TestTargetClass(PipedReader.class) 
public class PipedReaderTest extends junit.framework.TestCase {

    static class PWriter implements Runnable {
        public PipedWriter pw;

        public PWriter(PipedReader reader) {
            try {
                pw = new PipedWriter(reader);
            } catch (Exception e) {
                System.out.println("Couldn't create writer");
            }
        }

        public PWriter() {
            pw = new PipedWriter();
        }

        public void run() {
            try {
                char[] c = new char[11];
                "Hello World".getChars(0, 11, c, 0);
                pw.write(c);
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            } catch (Exception e) {
                System.out.println("Exception occurred: " + e.toString());
            }
        }
    }

    static class PWriter2 implements Runnable {
        PipedWriter pw;

        public boolean keepRunning = true;

        public void run() {
            try {
                pw.write('H');
                pw.close();
                while (keepRunning) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Error while running the writer thread.");
            }
        }

        public PWriter2(PipedWriter writer) {
            pw = writer;
        }
    }
    
    PipedReader preader;

    PWriter pwriter;

    Thread t;

    /**
     * @tests java.io.PipedReader#PipedReader()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedReader",
        args = {}
    )
    public void test_Constructor() {
        preader = new PipedReader();
        assertNotNull(preader);
        try {
            preader.close();
        } catch (IOException e) {
            fail("Unexpeceted IOException");
        }
    }

    /**
     * @tests java.io.PipedReader#PipedReader(java.io.PipedWriter)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedReader",
        args = {java.io.PipedWriter.class}
    )
    public void test_ConstructorLjava_io_PipedWriter() throws IOException {
        // Test for method java.io.PipedReader(java.io.PipedWriter)
        try {
            preader = new PipedReader(new PipedWriter());
        } catch (Exception e) {
            fail("Test 1: Constructor failed: " + e.getMessage());
        }
        preader.close();
            
        PipedWriter pw = new PipedWriter(new PipedReader());
        try {
            preader = new PipedReader(pw);
            fail("Test 2: IOException expected because the writer is already connected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "No IOException checking because it is never thrown in the source code.",
        method = "close",
        args = {}
    )
    public void test_close() throws Exception {
        // Test for method void java.io.PipedReader.close()
        char[] c = null;
        preader = new PipedReader();
        t = new Thread(new PWriter(preader), "");
        t.start();
        Thread.sleep(500); // Allow writer to start
        c = new char[11];
        preader.read(c, 0, 5);
        preader.close();
        
        try {
            preader.read(c, 0, 5);
            fail("IOException expected because the reader is closed.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#connect(java.io.PipedWriter)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "connect",
        args = {java.io.PipedWriter.class}
    )
    public void test_connectLjava_io_PipedWriter() throws Exception {
        // Test for method void java.io.PipedReader.connect(java.io.PipedWriter)
        char[] c = null;

        preader = new PipedReader();
        t = new Thread(pwriter = new PWriter(), "");
        preader.connect(pwriter.pw);
        t.start();
        Thread.sleep(500); // Allow writer to start
        c = new char[11];
        preader.read(c, 0, 11);

        assertEquals("Test 1: Wrong characters read. ", "Hello World", new String(c));
        try {
            preader.connect(new PipedWriter());
            fail("Test 2: IOException expected because the reader is already connected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {}
    )
    public void test_read_1() throws Exception {
        // Test for method int java.io.PipedReader.read()
        char[] c = null;
        preader = new PipedReader();
        t = new Thread(new PWriter(preader), "");
        t.start();
        Thread.sleep(500); // Allow writer to start
        c = new char[11];
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) preader.read();
        }
        assertEquals("Test 1: Wrong characters read. ", "Hello World", new String(c));
        
        try {
            preader.read();
            fail("Test 2: IOException expected since the thread that has " +
                 "written to the pipe is no longer alive.");
        } catch (IOException e) {
            // Expected.
        }
        
        preader.close();
        try {
            preader.read();
            fail("Test 3: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks that read() returns -1 if the PipedWriter connectedto this PipedReader is closed.",
        method = "read",
        args = {}
    )
    public void test_read_2() throws Exception {
        Thread writerThread;
        PipedWriter pw;
        PWriter2 pwriter;

        preader = new PipedReader();
        pw = new PipedWriter(preader);
        
        writerThread = new Thread(pwriter = new PWriter2(pw), "PWriter2");
        writerThread.start();
        Thread.sleep(500); // Allow writer to start
        
        preader.read();
        assertEquals("Test 1: No more data indication expected. ", -1, preader.read());
        pwriter.keepRunning = false;
    }

    /**
     * @tests java.io.PipedReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "IOException checking missed.",
        method = "read",
        args = {char[].class, int.class, int.class}
    )
    public void test_read$CII_1() throws Exception {
        // Test for method int java.io.PipedReader.read(char [], int, int)
        char[] c = null;
        preader = new PipedReader();
        t = new Thread(new PWriter(preader), "");
        t.start();
        Thread.sleep(500); // Allow writer to start
        c = new char[11];
        int n = 0;
        int x = n;
        while (x < 11) {
            n = preader.read(c, x, 11 - x);
            x = x + n;
        }
        assertEquals("Test 1: Wrong characters read. ", "Hello World", new String(c));

        preader.close();
        try {
            preader.read(c, 8, 7);
            fail("Test 2: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#read(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {char[].class, int.class, int.class}
    )
    public void test_read$CII_Exception() throws IOException{
        PipedWriter pw = new PipedWriter();
        PipedReader obj = new PipedReader(pw);
        try {
            obj.read(new char[10], 0, -1);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        try {
            obj.read(new char[10], -1, 1);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        try {
            obj.read(new char[10], 2, 9);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        try {
            obj.read(null, 0, 1);
            fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedReader#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks that read() returns -1 if the PipedWriter connectedto this PipedReader is closed.",
        method = "read",
        args = {}
    )
    public void test_read$CII_2() throws Exception {
        Thread writerThread;
        PipedWriter pw;
        PWriter2 pwriter;
        char[] c = new char[1];

        preader = new PipedReader();
        pw = new PipedWriter(preader);
        
        writerThread = new Thread(pwriter = new PWriter2(pw), "PWriter2");
        writerThread.start();
        Thread.sleep(500); // Allow writer to start
        
        preader.read(c, 0, 1);
        assertEquals("Test 1: No more data indication expected. ", 
                     -1, preader.read(c, 0, 1));
        pwriter.keepRunning = false;
    }

    /**
     * @tests java.io.PipedReader#ready()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ready",
        args = {}
    )
    public void test_ready() throws Exception {
        // Test for method boolean java.io.PipedReader.ready()
        char[] c = null;
        preader = new PipedReader();
        
        try {
            preader.ready();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        
        t = new Thread(new PWriter(preader), "");
        t.start();
        Thread.sleep(500); // Allow writer to start
        assertTrue("Test 2: Reader should be ready", preader.ready());
        c = new char[11];
        for (int i = 0; i < c.length; i++)
            c[i] = (char) preader.read();
        assertFalse("Test 3: Reader should not be ready after reading all chars",
                preader.ready());
        
        preader.close();
        try {
            preader.ready();
            fail("Test 4: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        if (t != null) {
            t.interrupt();
        }
        super.tearDown();
    }
}
