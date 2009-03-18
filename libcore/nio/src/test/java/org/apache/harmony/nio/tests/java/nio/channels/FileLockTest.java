/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.nio.tests.java.nio.channels;

import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import junit.framework.TestCase;
@TestTargetClass(FileLock.class)
/**
 * Tests class FileLock.
 */
public class FileLockTest extends TestCase {

    private FileChannel readWriteChannel;

    private MockFileLock mockLock;

    class MockFileLock extends FileLock {

        boolean isValid = true;

        protected MockFileLock(FileChannel channel, long position, long size,
                boolean shared) {
            super(channel, position, size, shared);
        }

        public boolean isValid() {
            return isValid;
        }

        public void release() throws IOException {
            isValid = false;
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        File tempFile = File.createTempFile("testing", "tmp");
        tempFile.deleteOnExit();
        RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");
        readWriteChannel = randomAccessFile.getChannel();
        mockLock = new MockFileLock(readWriteChannel, 10, 100, false);
    }

    /**
     * @tests java.nio.channels.FileLock#FileLock(FileChannel, long, long,
     *        boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "FileLock",
        args = {java.nio.channels.FileChannel.class, long.class, long.class, boolean.class}
    )
    public void test_Constructor_Ljava_nio_channels_FileChannelJJZ() {
        FileLock fileLock1 = new MockFileLock(null, 0, 0, false);
        assertNull(fileLock1.channel());

        try {
            new MockFileLock(readWriteChannel, -1, 0, false);
            fail("should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            new MockFileLock(readWriteChannel, 0, -1, false);
            fail("should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        // Harmony-682 regression test
        try {
            new MockFileLock(readWriteChannel, Long.MAX_VALUE, 1, false);
            fail("should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * @tests java.nio.channels.FileLock#channel()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "channel",
        args = {}
    )
    public void test_channel() {
        assertSame(readWriteChannel, mockLock.channel());
        FileLock lock = new MockFileLock(null, 0, 10, true);
        assertNull(lock.channel());
    }

    /**
     * @tests java.nio.channels.FileLock#position()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "position",
        args = {}
    )
    public void test_position() {
        FileLock fileLock1 = new MockFileLock(readWriteChannel, 20, 100, true);
        assertEquals(20, fileLock1.position());

        final long position = ((long) Integer.MAX_VALUE + 1);
        FileLock fileLock2 = new MockFileLock(readWriteChannel, position, 100,
                true);
        assertEquals(position, fileLock2.position());
    }

    /**
     * @tests java.nio.channels.FileLock#size()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "size",
        args = {}
    )
    public void test_size() {
        FileLock fileLock1 = new MockFileLock(readWriteChannel, 20, 100, true);
        assertEquals(100, fileLock1.size());

        final long position = 0x0FFFFFFFFFFFFFFFL;
        final long size = ((long) Integer.MAX_VALUE + 1);
        FileLock fileLock2 = new MockFileLock(readWriteChannel, position, size,
                true);
        assertEquals(size, fileLock2.size());
    }

    /**
     * @tests java.nio.channels.FileLock#isShared()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "[check with false shared parameter]",
        method = "isShared",
        args = {}
    )
    public void test_isShared() {
        assertFalse(mockLock.isShared());
        FileLock lock = new MockFileLock(null, 0, 10, true);
        assertTrue(lock.isShared());
    }

    /**
     * @tests java.nio.channels.FileLock#overlaps(long, long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "overlaps",
        args = {long.class, long.class}
    )
    public void test_overlaps_JJ() {
        assertTrue(mockLock.overlaps(0, 11));
        assertFalse(mockLock.overlaps(0, 10));
        assertTrue(mockLock.overlaps(100, 110));
        assertTrue(mockLock.overlaps(99, 110));
        assertFalse(mockLock.overlaps(-1, 10));
        //Harmony-671 regression test
        assertTrue(mockLock.overlaps(1, 120));
        assertTrue(mockLock.overlaps(20, 50));
    }

    /**
     * @tests java.nio.channels.FileLock#isValid()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isValid",
        args = {}
    )
    public void test_isValid() throws IOException {
        FileLock fileLock = readWriteChannel.lock();
        assertTrue(fileLock.isValid());
        fileLock.release();
        assertFalse(fileLock.isValid());
    }
    
    /**
     * @tests java.nio.channels.FileLock#release()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "release",
        args = {}
    )
    public void test_release() throws Exception {
        File file = File.createTempFile("test", "tmp");
        file.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(file);
        FileChannel fileChannel = fout.getChannel();
        FileLock fileLock = fileChannel.lock();
        fileChannel.close();
        try {
            fileLock.release();
            fail("should throw ClosedChannelException");
        } catch (ClosedChannelException e) {
            // expected
        }

        // release after release
        fout = new FileOutputStream(file);
        fileChannel = fout.getChannel();
        fileLock = fileChannel.lock();
        fileLock.release();
        fileChannel.close();
        try {
            fileLock.release();
            fail("should throw ClosedChannelException");
        } catch (ClosedChannelException e) {
            //expected
        }
    }
    
    /**
     * @tests java.nio.channels.FileLock#release()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() throws Exception {
        File file = File.createTempFile("test", "tmp");
        file.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(file);
        FileChannel fileChannel = fout.getChannel();
        FileLock fileLock = fileChannel.lock();
        assertTrue(fileLock.toString().length() > 0);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Regression test",
        method = "release",
        args = {}
    )
    public void testFileLock() throws Exception {
        String fileName = File.createTempFile("test", "tmp").getAbsolutePath();
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");   
        FileLock lock = raf.getChannel().tryLock();
        raf.write("file lock test".getBytes()); 
        lock.release();   
        raf.close();
      }
}
