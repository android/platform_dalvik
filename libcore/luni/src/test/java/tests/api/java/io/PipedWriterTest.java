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

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass; 

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

import java.util.Arrays;

@TestTargetClass(PipedWriter.class) 
public class PipedWriterTest extends junit.framework.TestCase {

    static final String testString = "Lorem ipsum...";
    static final int testLength = testString.length();

    static class PReader implements Runnable {
        public PipedReader pr;

        public char[] buf;

        public PReader(PipedWriter pw) {
            try {
                pr = new PipedReader(pw);
            } catch (IOException e) {
                System.out.println("Exception setting up reader: "
                        + e.toString());
            }
        }

        public PReader(PipedReader pr) {
            this.pr = pr;
        }

/*        public void run() {
            try {
                int r = 0;
                for (int i = 0; i < buf.length; i++) {
                    r = pr.read();
                    if (r == -1)
                        break;
                    buf[i] = (char) r;
                }
            } catch (Exception e) {
                System.out.println("Exception reading ("
                        + Thread.currentThread().getName() + "): "
                        + e.toString());
            }
        } */
        
        public void run() {
            try {
                while (true) {
                    Thread.sleep(1000);
                    Thread.yield();
                }
            } catch (InterruptedException e) {
            }
        }

        public String read(int nbytes) {
            buf = new char[nbytes];
            try {
                pr.read(buf, 0, nbytes);
                return new String(buf);
            } catch (IOException e) {
                System.out.println("Exception reading ("
                        + Thread.currentThread().getName() + "): "
                        + e.toString());
                return "ERROR";
            }
        }
    }

    Thread readerThread;
    PReader reader;
    PipedWriter pw;
    char[] testBuf;

    /**
     * @tests java.io.PipedWriter#PipedWriter()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedWriter",
        args = {}
    )
    public void test_Constructor() {
        pw = new PipedWriter();
        assertNotNull(pw);
        try {
            pw.close();
        } catch (IOException e) {
            fail("Unexpeceted IOException.");
        }
    }

    /**
     * @tests java.io.PipedWriter#PipedWriter(java.io.PipedReader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedWriter",
        args = {java.io.PipedReader.class}
    )
    public void test_ConstructorLjava_io_PipedReader() throws Exception {
        PipedReader rd = new PipedReader();
        
        try {
            pw = new PipedWriter(rd);
        } catch (Exception e) {
            fail("Test 1: Construtor failed:" + e.getMessage());
        }
        
        readerThread = new Thread(reader = new PReader(rd), "Constructor(Reader)");
        readerThread.start();
        try {
            pw.write(testBuf);
        } catch (Exception e) {
            fail("Test 2: Could not write to the constructed writer: " 
                    + e.getMessage());
        }
        pw.close();
        assertEquals("Test 3: Incorrect character string received.", testString, 
                reader.read(testLength));
        
        rd = new PipedReader(new PipedWriter());
        try {
            pw = new PipedWriter(rd);
            fail("Test 4: IOException expected because the reader is already connected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedWriter#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close() throws Exception {
        PipedReader rd = new PipedReader();
        pw = new PipedWriter(rd);
        reader = new PReader(rd);
        try {
            pw.close();
        } catch (IOException e) {
            fail("Test 1: Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * @tests java.io.PipedWriter#connect(java.io.PipedReader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "connect",
        args = {java.io.PipedReader.class}
    )
    public void test_connectLjava_io_PipedReader() throws Exception {
        PipedReader rd = new PipedReader();
        pw = new PipedWriter();
        
        try {
            pw.connect(rd);
        } catch (Exception e) {
            fail("Test 1: Unexpected exception when connecting: " + 
                    e.getLocalizedMessage());
        }

        readerThread = new Thread(reader = new PReader(rd), "connect");
        readerThread.start();
        
        try {
            pw.write(testBuf);
        } catch (IOException e) {
            fail("Test 2: Unexpected IOException when writing after connecting.");
        }
        assertEquals("Test 3: Incorrect character string received.", testString, 
                reader.read(testLength));

        try {
            pw.connect(new PipedReader());
            fail("Test 4: IOException expected when reconnecting the writer.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedWriter#flush()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "No IOException checking because it is never thrown in the source code.",
        method = "flush",
        args = {}
    )
    public void test_flush() throws Exception {
        // Test for method void java.io.PipedWriter.flush()
        pw = new PipedWriter();
        readerThread = new Thread(reader = new PReader(pw), "flush");
        readerThread.start();
        pw.write(testBuf);
        pw.flush();
        assertEquals("Test 1: Flush failed. ", testString, 
                reader.read(testLength));
    }

    /**
     * @tests java.io.PipedWriter#write(char[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    public void test_write$CII() throws Exception {
        pw = new PipedWriter();
        
        try {
            pw.write(testBuf, 0, 5);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        
        pw = new PipedWriter(new PipedReader());
        
        try {
            pw.write(testBuf, -1, 1);
            fail("Test 2: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        
        try {
            pw.write(testBuf, 0, -1);
            fail("Test 3: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        
        try {
            pw.write(testBuf, 5, testString.length());
            fail("Test 4: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected.
        }
        
        pw.close();
        pw = new PipedWriter();
        try {
            readerThread = new Thread(reader = new PReader(pw), "writeCII");
            readerThread.start();
            pw.write(testBuf, 0, testLength);
            pw.close();
            reader.read(testLength);
            assertTrue("Test 5: Characters read do not match the characters written.", 
                    Arrays.equals( testBuf, reader.buf));
        } catch (IOException e) {
            fail("Test 5: Unexpected IOException: " + e.getMessage());
        }
        
        readerThread.interrupt();
        
        try {
            pw.write(testBuf, 0, 5);
            fail("Test 6: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }

        reader.pr.close();
        try {
            pw.write(testBuf, 0, 5);
            fail("Test 7: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }
    
    /**
     * @tests java.io.PipedWriter#write(char[],int,int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {char[].class, int.class, int.class}
    )
    @BrokenTest("Hangs on RI")
    public void test_write$CII_MultiThread() throws Exception {
        final PipedReader pr = new PipedReader();
        final PipedWriter pw = new PipedWriter();

        // test if writer recognizes dead reader
        pr.connect(pw);

        class WriteRunnable implements Runnable {
            boolean pass = false;

            boolean readerAlive = true;

            public void run() {
                try {
                    pw.write(1);
                    while (readerAlive) {
                    // wait the reader thread dead
                    }
                    try {
                        // should throw exception since reader thread
                        // is now dead
                        char[] buf = new char[10];
                        pw.write(buf, 0, 10);
                    } catch (IOException e) {
                        pass = true;
                    }
                } catch (IOException e) {
                  //ignore
                }
            }
        }
        WriteRunnable writeRunnable = new WriteRunnable();
        Thread writeThread = new Thread(writeRunnable);
        class ReadRunnable implements Runnable {
            boolean pass;

            public void run() {
                try {
                    pr.read();
                    pass = true;
                } catch (IOException e) {
                  //ignore
                }
            }
        }
        ReadRunnable readRunnable = new ReadRunnable();
        Thread readThread = new Thread(readRunnable);
        writeThread.start();
        readThread.start();
        while (readThread.isAlive()) {
            //wait the reader thread dead
        }
        writeRunnable.readerAlive = false;
        assertTrue("reader thread failed to read", readRunnable.pass);
        while (writeThread.isAlive()) {
            //wait the writer thread dead
        }
        assertTrue("writer thread failed to recognize dead reader",
                writeRunnable.pass);
    }

    /**
     * @tests java.io.PipedWriter#write(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {int.class}
    )
    public void test_writeI() throws Exception {
        // Test for method void java.io.PipedWriter.write(int)

        pw = new PipedWriter();
        
        try {
            pw.write(42);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }

        readerThread = new Thread(reader = new PReader(pw), "writeI");
        readerThread.start();
        pw.write(1);
        pw.write(2);
        pw.write(3);
        pw.close();
        reader.read(3);
        assertTrue("Test 2: The charaacters read do not match the characters written: " + 
                (int) reader.buf[0] + " " + (int) reader.buf[1] + " " + (int) reader.buf[2],
                reader.buf[0] == 1 && reader.buf[1] == 2 && reader.buf[2] == 3);
    }

    /**
     * @tests java.io.PipedWriter#write(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "write",
        args = {int.class}
    )
    @BrokenTest("Hangs on RI")
    public void test_writeI_MultiThread() throws IOException {
        final PipedReader pr = new PipedReader();
        final PipedWriter pw = new PipedWriter();
        // test if writer recognizes dead reader
        pr.connect(pw);

        class WriteRunnable implements Runnable {
            boolean pass = false;
            boolean readerAlive = true;
            public void run() {
                try {
                    pw.write(1);
                    while (readerAlive) {
                    // wait the reader thread dead
                    }
                    try {
                        // should throw exception since reader thread
                        // is now dead
                        pw.write(1);
                    } catch (IOException e) {
                        pass = true;
                    }
                } catch (IOException e) {
                  //ignore
                }
            }
        }
        WriteRunnable writeRunnable = new WriteRunnable();
        Thread writeThread = new Thread(writeRunnable);
        class ReadRunnable implements Runnable {
            boolean pass;
            public void run() {
                try {
                    pr.read();
                    pass = true;
                } catch (IOException e) {
                  //ignore
                }
            }
        }
        ReadRunnable readRunnable = new ReadRunnable();
        Thread readThread = new Thread(readRunnable);
        writeThread.start();
        readThread.start();
        while (readThread.isAlive()) {
           //wait the reader thread dead
        }
        writeRunnable.readerAlive = false;
        assertTrue("reader thread failed to read", readRunnable.pass);
        while (writeThread.isAlive()) {
           //wait the writer thread dead
        }
        assertTrue("writer thread failed to recognize dead reader",
                writeRunnable.pass);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        testBuf = new char[testLength];
        testString.getChars(0, testLength, testBuf, 0);
    }
    
    protected void tearDown() throws Exception {
        try {
            if (readerThread != null) {
                readerThread.interrupt();
            }
        } catch (Exception ignore) {}
        try {
            if (pw != null) {
                pw.close();
            }
        } catch (Exception ignore) {}
        super.tearDown();
    }
}
