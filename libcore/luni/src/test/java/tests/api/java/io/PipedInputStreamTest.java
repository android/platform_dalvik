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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(PipedInputStream.class) 
public class PipedInputStreamTest extends junit.framework.TestCase {

    static class PWriter implements Runnable {
        PipedOutputStream pos;

        public byte bytes[];

        public void run() {
            try {
                pos.write(bytes);   
                synchronized (this) {
                    notify();
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Error while running the writer thread.");
            }
        }

        public PWriter(PipedOutputStream pout, int nbytes) {
            pos = pout;
            bytes = new byte[nbytes];
            for (int i = 0; i < bytes.length; i++)
                bytes[i] = (byte) (System.currentTimeMillis() % 9);
        }
    }

    static class PWriter2 implements Runnable {
        PipedOutputStream pos;

        public boolean keepRunning = true;

        public void run() {
            try {
                pos.write(42);
                pos.close();
                while (keepRunning) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Error while running the writer thread.");
            }
        }

        public PWriter2(PipedOutputStream pout) {
            pos = pout;
        }
    }

    Thread t;

    PWriter pw;

    PipedInputStream pis;

    PipedOutputStream pos;

    /**
     * @tests java.io.PipedInputStream#PipedInputStream()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedInputStream",
        args = {}
    )
    public void test_Constructor() throws IOException {
        pis = new PipedInputStream();
        assertEquals("There should not be any bytes available. ", 0, pis.available());
        pis.close();
    }

    /**
     * @tests java.io.PipedInputStream#PipedInputStream(java.io.PipedOutputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PipedInputStream",
        args = {java.io.PipedOutputStream.class}
    )
    public void test_ConstructorLjava_io_PipedOutputStream() throws IOException {
        pos = new PipedOutputStream(new PipedInputStream());
        
        try {
            pis = new PipedInputStream(pos);
            fail("IOException expected since the output stream is already connected.");
        } catch (IOException e) {
            // Expected.
        }
        
        pis = new PipedInputStream(new PipedOutputStream());
        assertEquals("There should not be any bytes available. ", 0, pis.available());
        
        pis.close();
        pos.close();
    }

    /**
     * @tests java.io.PipedInputStream#available()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "No IOException checking because it is never thrown in the source code.",
        method = "available",
        args = {}
    )
    public void test_available() throws Exception {
        // Test for method int java.io.PipedInputStream.available()
        pis = new PipedInputStream();
        pos = new PipedOutputStream();

        pis.connect(pos);
        t = new Thread(pw = new PWriter(pos, 1000));
        t.start();

        synchronized (pw) {
            pw.wait(10000);
        }
        assertEquals("Test 1: Incorrect number of bytes available. ",
                     1000, pis.available());

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        // We know the PipedInputStream buffer size is 1024.
        // Writing another byte would cause the write to wait
        // for a read before returning
        for (int i = 0; i < 1024; i++)
            pout.write(i);
        assertEquals("Test 2: Incorrect number of bytes available. ", 
                     1024 , pin.available());
    }

    /**
     * @tests java.io.PipedInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "No IOException checking because it is never thrown in the source code.",
        method = "close",
        args = {}
    )
    public void test_close() throws IOException {
        // Test for method void java.io.PipedInputStream.close()
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        pis.connect(pos);
        pis.close();
        try {
            pos.write((byte) 127);
            fail("IOException expected.");
        } catch (IOException e) {
            // The spec for PipedInput says an exception should be thrown if
            // a write is attempted to a closed input. The PipedOuput spec
            // indicates that an exception should be thrown only when the
            // piped input thread is terminated without closing
            return;
        }
    }

    /**
     * @tests java.io.PipedInputStream#connect(java.io.PipedOutputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "connect",
        args = {java.io.PipedOutputStream.class}
    )
    public void test_connectLjava_io_PipedOutputStream() throws Exception {
        // Test for method void
        // java.io.PipedInputStream.connect(java.io.PipedOutputStream)
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        assertEquals("Test 1: Not-connected pipe returned more than zero available bytes. ", 
                     0, pis.available());

        pis.connect(pos);
        t = new Thread(pw = new PWriter(pos, 1000));
        t.start();

        synchronized (pw) {
            pw.wait(10000);
        }
        assertEquals("Test 2: Unexpected number of bytes available. ", 
                     1000, pis.available());

        try {
            pis.connect(pos);
            fail("Test 3: IOException expected when reconnecting the pipe.");
        } catch (IOException e) {
            // Expected.
        }
        
        pis.close();
        pos.close();
    }

    /**
     * @tests java.io.PipedInputStream#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "read",
        args = {}
    )
    public void test_read() throws Exception {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();

        try {
            pis.read();
            fail("Test 1: IOException expected since the stream is not connected.");
        } catch (IOException e) {
            // Expected.
        }
        
        pis.connect(pos);
        t = new Thread(pw = new PWriter(pos, 100));
        t.start();

        synchronized (pw) {
            pw.wait(5000);
        }
        assertEquals("Test 2: Unexpected number of bytes available. ", 
                     100, pis.available());
        
        for (int i = 0; i < 100; i++) {
            assertEquals("Test 3: read() returned incorrect byte. ", 
                         pw.bytes[i], (byte) pis.read());
        }

        try {
            pis.read();
            fail("Test 4: IOException expected since the thread that has " +
                 "written to the pipe is no longer alive.");
        } catch (IOException e) {
            // Expected.
        }

        pis.close();
        try {
            pis.read();
            fail("Test 5: IOException expected since the stream is closed.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedInputStream#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks that read returns -1 if the PipedOutputStream connected to this PipedInputStream is closed.",
        method = "read",
        args = {}
    )
    public void test_read_2() throws Exception {
        Thread writerThread;
        PWriter2 pwriter;
        
        pos = new PipedOutputStream(); 
        pis = new PipedInputStream(pos);
        writerThread = new Thread(pwriter = new PWriter2(pos));
        writerThread.start();

        synchronized (pwriter) {
            pwriter.wait(5000);
        }
        pis.read();
        assertEquals("Test 1: No more data indication expected. ", -1, pis.read());
        pwriter.keepRunning = false;
        
        pis.close();
    }

    /**
     * @tests java.io.PipedInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Tests read from unconnected, connected and closed pipe.",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII() throws Exception {
        byte[] buf = new byte[400];
        pis = new PipedInputStream();
        pos = new PipedOutputStream();

        try {
            pis.read(buf, 0, 10);
            fail("Test 1: IOException expected since the stream is not connected.");
        } catch (IOException e) {
            // Expected.
        }
        
        pis.connect(pos);
        t = new Thread(pw = new PWriter(pos, 1000));
        t.start();

        synchronized (pw) {
            pw.wait(10000);
        }
        assertEquals("Test 2: Unexpected number of bytes available. ",
                     1000, pis.available());
        pis.read(buf, 0, 400);
        for (int i = 0; i < 400; i++) {
            assertEquals("Test 3: read() returned incorrect byte. ", 
                         pw.bytes[i], buf[i]);
        }
        
        pis.close();
        try {
            pis.read(buf, 0, 10);
            fail("Test 4: IOException expected since the stream is closed.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.PipedInputStream#read(byte[], int, int)
     * Regression for HARMONY-387
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Tests illegal length argument.",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII_2() throws IOException {
        PipedInputStream obj = new PipedInputStream();
        try {
            obj.read(new byte[0], 0, -1);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException t) {
            assertEquals(
                    "IndexOutOfBoundsException rather than a subclass expected.",
                    IndexOutOfBoundsException.class, t.getClass());
        }
    }

    /**
     * @tests java.io.PipedInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Tests illegal offset argument.",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII_3() throws IOException {
        PipedInputStream obj = new PipedInputStream();
        try {
            obj.read(new byte[10], -1, 1);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertTrue(e.getClass().equals(IndexOutOfBoundsException.class));
        }
        try {
            obj.read(new byte[10], 0, -1);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertTrue(e.getClass().equals(IndexOutOfBoundsException.class));
        }
        try {
            obj.read(new byte[10], 9, 2);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertTrue(e.getClass().equals(IndexOutOfBoundsException.class));
        }
    }

    /**
     * @tests java.io.PipedInputStream#receive(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "receive",
        args = {int.class}
    )
    @BrokenTest("Test hangs indefinitely on the RI")
    public void test_receive() throws IOException {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();

        // test if writer recognizes dead reader
        pis.connect(pos);
        class WriteRunnable implements Runnable {

            boolean pass = false;

            boolean readerAlive = true;

            public void run() {
                try {
                    pos.write(1);
                    while (readerAlive)
                        ;
                    try {
                        // should throw exception since reader thread
                        // is now dead
                        pos.write(1);
                    } catch (IOException e) {
                        pass = true;
                    }
                } catch (IOException e) {}
            }
        }
        WriteRunnable writeRunnable = new WriteRunnable();
        Thread writeThread = new Thread(writeRunnable);
        class ReadRunnable implements Runnable {

            boolean pass;

            public void run() {
                try {
                    pis.read();
                    pass = true;
                } catch (IOException e) {}
            }
        }
        ;
        ReadRunnable readRunnable = new ReadRunnable();
        Thread readThread = new Thread(readRunnable);
        writeThread.start();
        readThread.start();
        while (readThread.isAlive())
            ;
        writeRunnable.readerAlive = false;
        assertTrue("reader thread failed to read", readRunnable.pass);
        while (writeThread.isAlive())
            ;
        assertTrue("writer thread failed to recognize dead reader",
                writeRunnable.pass);

        // attempt to write to stream after writer closed
        pis = new PipedInputStream();
        pos = new PipedOutputStream();

        pis.connect(pos);
        class MyRunnable implements Runnable {

            boolean pass;

            public void run() {
                try {
                    pos.write(1);
                } catch (IOException e) {
                    pass = true;
                }
            }
        }
        MyRunnable myRun = new MyRunnable();
        synchronized (pis) {
            t = new Thread(myRun);
            // thread t will be blocked inside pos.write(1)
            // when it tries to call the synchronized method pis.receive
            // because we hold the monitor for object pis
            t.start();
            try {
                // wait for thread t to get to the call to pis.receive
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            // now we close
            pos.close();
        }
        // we have exited the synchronized block, so now thread t will make
        // a call to pis.receive AFTER the output stream was closed,
        // in which case an IOException should be thrown
        while (t.isAlive()) {
            ;
        }
        assertTrue(
                "write failed to throw IOException on closed PipedOutputStream",
                myRun.pass);
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        try {
            if (t != null) {
                t.interrupt();
            }
        } catch (Exception ignore) {
        }
        super.tearDown();
    }
}
