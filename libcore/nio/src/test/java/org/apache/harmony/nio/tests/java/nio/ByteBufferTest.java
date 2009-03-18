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

package org.apache.harmony.nio.tests.java.nio;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.AndroidOnly;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.InvalidMarkException;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Tests java.nio.ByteBuffer
 * 
 */
@TestTargetClass(ByteBuffer.class)
public abstract class ByteBufferTest extends AbstractBufferTest {
    protected static final int SMALL_TEST_LENGTH = 5;
    protected static final int BUFFER_LENGTH = 250;
    
    protected ByteBuffer buf;

    protected void setUp() throws Exception {
        capacity = BUFFER_LENGTH;
        buf = ByteBuffer.allocate(BUFFER_LENGTH);
        loadTestData1(buf);
        baseBuf = buf;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        buf = null;
        baseBuf = null;
    }

    /*
     * test for method static ByteBuffer allocate(int capacity)
     * test covers following usecases:
     * 1. case for check ByteBuffer testBuf properties
     * 2. case expected IllegalArgumentException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "allocate",
        args = {int.class}
    )
    public void test_AllocateI() {
        // case: ByteBuffer testBuf properties is satisfy the conditions
        // specification
        ByteBuffer testBuf = ByteBuffer.allocate(20);
        assertEquals(0, testBuf.position());
        assertNotNull(testBuf.array());
        assertEquals(0, testBuf.arrayOffset());
        assertEquals(20, testBuf.limit());
        assertEquals(20, testBuf.capacity());

        testBuf = ByteBuffer.allocate(0);
        assertEquals(0, testBuf.position());
        assertNotNull(testBuf.array());
        assertEquals(0, testBuf.arrayOffset());
        assertEquals(0, testBuf.limit());
        assertEquals(0, testBuf.capacity());

        // case: expected IllegalArgumentException
        try {
            testBuf = ByteBuffer.allocate(-20);
            fail("allocate method does not throws expected exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    /*
     * test for method static ByteBuffer allocateDirect(int capacity)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "allocateDirect",
        args = {int.class}
    )
    public void test_AllocateDirectI() {
        // case: ByteBuffer testBuf properties is satisfy the conditions
        // specification
        ByteBuffer testBuf = ByteBuffer.allocateDirect(20);
        assertEquals(0, testBuf.position());
        assertEquals(20, testBuf.limit());
        assertEquals(20, testBuf.capacity());

        testBuf = ByteBuffer.allocateDirect(0);
        assertEquals(0, testBuf.position());
        assertEquals(0, testBuf.limit());
        assertEquals(0, testBuf.capacity());
        
        try {
            testBuf.array();
            fail("Didn't throw expected UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            testBuf.arrayOffset();
            fail("Didn't throw expected UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        // case: expected IllegalArgumentException
        try {
            testBuf = ByteBuffer.allocate(-20);
            fail("allocate method does not throws expected exception");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "array",
        args = {}
    )
    public void testArray() {
        byte array[] = buf.array();
        assertContentEquals(buf, array, buf.arrayOffset(), buf.capacity());

        loadTestData1(array, buf.arrayOffset(), buf.capacity());
        assertContentEquals(buf, array, buf.arrayOffset(), buf.capacity());

        loadTestData2(array, buf.arrayOffset(), buf.capacity());
        assertContentEquals(buf, array, buf.arrayOffset(), buf.capacity());

        loadTestData1(buf);
        assertContentEquals(buf, array, buf.arrayOffset(), buf.capacity());

        loadTestData2(buf);
        assertContentEquals(buf, array, buf.arrayOffset(), buf.capacity());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "arrayOffset",
        args = {}
    )
    public void testArrayOffset() {
        byte array[] = buf.array();
        for(int i = 0; i < buf.capacity(); i++) {
            array[i] = (byte) i;
        }
        int offset = buf.arrayOffset();
        assertContentEquals(buf, array, offset, buf.capacity());

        ByteBuffer wrapped = ByteBuffer.wrap(array, 3, array.length - 3);
        
        loadTestData1(array, wrapped.arrayOffset(), wrapped.capacity());
        assertContentEquals(buf, array, offset, buf.capacity());

        loadTestData2(array, wrapped.arrayOffset(), wrapped.capacity());
        assertContentEquals(buf, array, offset, buf.capacity());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asReadOnlyBuffer",
        args = {}
    )
    public void testAsReadOnlyBuffer() {
        loadTestData1(buf);
        buf.clear();
        buf.mark();
        buf.position(buf.limit());

        // readonly's contents should be the same as buf
        ByteBuffer readonly = buf.asReadOnlyBuffer();
        assertNotSame(buf, readonly);
        assertTrue(readonly.isReadOnly());
        assertEquals(buf.position(), readonly.position());
        assertEquals(buf.limit(), readonly.limit());
        assertEquals(buf.isDirect(), readonly.isDirect());
        assertEquals(buf.order(), readonly.order());
        assertContentEquals(buf, readonly);

        // readonly's position, mark, and limit should be independent to buf
        readonly.reset();
        assertEquals(0, readonly.position());
        readonly.clear();
        assertEquals(buf.position(), buf.limit());
        buf.reset();
        assertEquals(0, buf.position());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "compact",
        args = {}
    )
    @AndroidOnly("Fails on RI. See comment below")
    public void testCompact() {
        if (buf.isReadOnly()) {
            try {
                buf.compact();
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        // case: buffer is full
        buf.clear();
        buf.mark();
        loadTestData1(buf);
        ByteBuffer ret = buf.compact();
        assertSame(ret, buf);
        assertEquals(buf.capacity(), buf.position());
        assertEquals(buf.capacity(), buf.limit());
        assertContentLikeTestData1(buf, 0, (byte) 0, buf.capacity());
        try {
            // Fails on RI. Spec doesn't specify the behavior if
	    // actually nothing to be done by compact(). So RI doesn't reset
            // mark position 
            buf.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }

        // case: buffer is empty
        buf.position(0);
        buf.limit(0);
        buf.mark();
        ret = buf.compact();
        assertSame(ret, buf);
        assertEquals(0, buf.position());
        assertEquals(buf.limit(), buf.capacity());
        assertContentLikeTestData1(buf, 0, (byte) 0, buf.capacity());
        try {
            buf.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }

        // case: normal
        assertTrue(buf.capacity() > SMALL_TEST_LENGTH);
        buf.position(1);
        buf.limit(SMALL_TEST_LENGTH);
        buf.mark();
        ret = buf.compact();
        assertSame(ret, buf);
        assertEquals(4, buf.position());
        assertEquals(buf.limit(), buf.capacity());
        assertContentLikeTestData1(buf, 0, (byte) 1, 4);
        try {
            buf.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "compareTo",
        args = {java.nio.ByteBuffer.class}
    )
    public void testCompareTo() {
        // compare to self
        assertEquals(0, buf.compareTo(buf));

        // normal cases
        if (!buf.isReadOnly()) {
            assertTrue(buf.capacity() > SMALL_TEST_LENGTH);
            buf.clear();
            ByteBuffer other = ByteBuffer.allocate(buf.capacity());
            loadTestData1(buf);
            loadTestData1(other);
            assertEquals(0, buf.compareTo(other));
            assertEquals(0, other.compareTo(buf));
            buf.position(1);
            assertTrue(buf.compareTo(other) > 0);
            assertTrue(other.compareTo(buf) < 0);
            other.position(2);
            assertTrue(buf.compareTo(other) < 0);
            assertTrue(other.compareTo(buf) > 0);
            buf.position(2);
            other.limit(SMALL_TEST_LENGTH);
            assertTrue(buf.compareTo(other) > 0);
            assertTrue(other.compareTo(buf) < 0);
        }
        
        assertTrue(ByteBuffer.wrap(new byte[21]).compareTo(ByteBuffer.allocateDirect(21)) == 0);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "duplicate",
        args = {}
    )
    public void testDuplicate() {
        buf.clear();
        buf.mark();
        buf.position(buf.limit());

        // duplicate's contents should be the same as buf
        ByteBuffer duplicate = buf.duplicate();
        assertNotSame(buf, duplicate);
        assertEquals(buf.position(), duplicate.position());
        assertEquals(buf.limit(), duplicate.limit());
        assertEquals(buf.isReadOnly(), duplicate.isReadOnly());
        assertEquals(buf.isDirect(), duplicate.isDirect());
        assertEquals(buf.order(), duplicate.order());
        assertContentEquals(buf, duplicate);

        // duplicate's position, mark, and limit should be independent to buf
        duplicate.reset();
        assertEquals(0, duplicate.position());
        duplicate.clear();
        assertEquals(buf.position(), buf.limit());
        buf.reset();
        assertEquals(0, buf.position());

        // duplicate share the same content with buf
        if (!duplicate.isReadOnly()) {
            loadTestData1(buf);
            assertContentEquals(buf, duplicate);
            loadTestData2(duplicate);
            assertContentEquals(buf, duplicate);
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEquals() {
        loadTestData1(buf);

        // equal to self
        assertTrue(buf.equals(buf));
        ByteBuffer readonly = buf.asReadOnlyBuffer();
        assertTrue(buf.equals(readonly));
        ByteBuffer duplicate = buf.duplicate();
        assertTrue(buf.equals(duplicate));

        // always false, if type mismatch
        assertFalse(buf.equals(Boolean.TRUE));

        assertTrue(buf.capacity() > SMALL_TEST_LENGTH);

        buf.limit(buf.capacity()).position(0);
        readonly.limit(readonly.capacity()).position(1);
        assertFalse(buf.equals(readonly));

        buf.limit(buf.capacity() - 1).position(0);
        duplicate.limit(duplicate.capacity()).position(0);
        assertFalse(buf.equals(duplicate));

        buf.limit(buf.capacity() - 1).position(0);
        duplicate.limit(duplicate.capacity()).position(1);
        assertFalse(buf.equals(duplicate));
    }

    /*
     * Class under test for byte get()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {}
    )
    public void testGet() {
        loadTestData1(buf);
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(i, buf.position());
            assertEquals(buf.get(), buf.get(i));
        }
        try {
            buf.get();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.ByteBuffer get(byte[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {byte[].class}
    )
    public void testGetbyteArray() {
        byte array[] = new byte[1];
        loadTestData1(buf);
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(i, buf.position());
            ByteBuffer ret = buf.get(array);
            assertEquals(array[0], buf.get(i));
            assertSame(ret, buf);
        }

        buf.get(new byte[0]);

        try {
            buf.get(array);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        try {
            buf.get((byte[])null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.ByteBuffer get(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {byte[].class, int.class, int.class}
    )
    public void testGetbyteArrayintint() {
        loadTestData1(buf);
        buf.clear();
        byte array[] = new byte[buf.capacity()];

        try {
            buf.get(new byte[buf.capacity() + 1], 0, buf.capacity() + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(0, buf.position());
        try {
            buf.get(array, -1, array.length);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        buf.get(array, array.length, 0);
        try {
            buf.get(array, array.length + 1, 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(0, buf.position());
        try {
            buf.get(array, 2, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.get(array, 2, array.length);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.get((byte[])null, -1, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        try {
            buf.get(array, 1, Integer.MAX_VALUE);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.get(array, Integer.MAX_VALUE, 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(0, buf.position());

        buf.clear();
        ByteBuffer ret = buf.get(array, 0, array.length);
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(buf, array, 0, array.length);
        assertSame(ret, buf);
    }

    /*
     * Class under test for byte get(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {int.class}
    )
    public void testGetint() {
        loadTestData1(buf);
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(i, buf.position());
            assertEquals(buf.get(), buf.get(i));
        }
        try {
            buf.get(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.get(buf.limit());
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies hasArray method for wrapped ByteBuffer.",
        method = "hasArray",
        args = {}
    )
    public void testHasArray() {
        assertTrue(buf.hasArray());
        assertNotNull(buf.array());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() {
        buf.clear();
        loadTestData1(buf);
        ByteBuffer readonly = buf.asReadOnlyBuffer();
        ByteBuffer duplicate = buf.duplicate();
        assertTrue(buf.hashCode() == readonly.hashCode());
        assertTrue(buf.capacity() > SMALL_TEST_LENGTH);
        duplicate.position(buf.capacity()/2);
        assertTrue(buf.hashCode()!= duplicate.hashCode());
    }
    
    //for the testHashCode() method of readonly subclasses
    protected void readOnlyHashCode(boolean direct) {
        //create a new buffer initiated with some data
        ByteBuffer buf;
        if (direct) {
            buf = ByteBuffer.allocateDirect(BUFFER_LENGTH);
        } else {
            buf = ByteBuffer.allocate(BUFFER_LENGTH);
        }
        loadTestData1(buf);
        buf = buf.asReadOnlyBuffer();
        buf.clear();
        ByteBuffer readonly = buf.asReadOnlyBuffer();
        ByteBuffer duplicate = buf.duplicate();
        assertEquals(buf.hashCode(),readonly.hashCode());
        duplicate.position(buf.capacity()/2);
        assertTrue(buf.hashCode()!= duplicate.hashCode());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies isDirect method with not direct buffer.",
        method = "isDirect",
        args = {}
    )
    public void testIsDirect() {
        assertFalse(buf.isDirect());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "isReadOnly",
        args = {}
    )
    public void testIsReadOnly() {
        assertFalse(buf.isReadOnly());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "order",
        args = {}
    )
    public void testOrder() {
        // BIG_ENDIAN is the default byte order
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());

        buf.order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buf.order());
        
        buf.order(ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());

        // Regression test for HARMONY-798
        buf.order((ByteOrder)null);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buf.order());
        
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    /*
     * test for method public final ByteBuffer order(ByteOrder bo)
     * test covers following usecases:
     * 1. case for check
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "order",
        args = {java.nio.ByteOrder.class}
    )
    public void test_OrderLjava_lang_ByteOrder() {
        //         BIG_ENDIAN is the default byte order
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());

        buf.order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buf.order());

        buf.order(ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());

        // Regression test for HARMONY-798
        buf.order((ByteOrder)null);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buf.order());

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    /*
     * Class under test for java.nio.ByteBuffer put(byte)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {byte.class}
    )
    public void testPutbyte() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.put((byte) 0);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(i, buf.position());
            ByteBuffer ret = buf.put((byte) i);
            assertEquals((byte) i, buf.get(i));
            assertSame(ret, buf);
        }
        try {
            buf.put((byte) 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.rewind();
        buf.put(Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, buf.get(0));
        buf.rewind();
        buf.put(Byte.MIN_VALUE);
        assertEquals(Byte.MIN_VALUE, buf.get(0));
    }

    /*
     * Class under test for java.nio.ByteBuffer put(byte[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {byte[].class}
    )
    public void testPutbyteArray() {
        byte array[] = new byte[1];
        if (buf.isReadOnly()) {
            try {
                buf.put(array);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(i, buf.position());
            array[0] = (byte) i;
            ByteBuffer ret = buf.put(array);
            assertEquals((byte) i, buf.get(i));
            assertSame(ret, buf);
        }
        try {
            buf.put(array);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        try {
            buf.put((byte[])null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.ByteBuffer put(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {byte[].class, int.class, int.class}
    )
    public void testPutbyteArrayintint() {
        buf.clear();
        byte array[] = new byte[buf.capacity()];
        if (buf.isReadOnly()) {
            try {
                buf.put(array, 0, array.length);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        try {
            buf.put(new byte[buf.capacity() + 1], 0, buf.capacity() + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        assertEquals(0, buf.position());
        try {
            buf.put(array, -1, array.length);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put(array, array.length + 1, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        buf.put(array, array.length, 0);
        assertEquals(0, buf.position());
        try {
            buf.put(array, 0, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put(array, 2, array.length);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            buf.put(array, 2, Integer.MAX_VALUE);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put(array, Integer.MAX_VALUE, 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put((byte[])null, 2, Integer.MAX_VALUE);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        
        assertEquals(0, buf.position());

        loadTestData2(array, 0, array.length);
        ByteBuffer ret = buf.put(array, 0, array.length);
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(buf, array, 0, array.length);
        assertSame(ret, buf);
    }

    /*
     * Class under test for java.nio.ByteBuffer put(java.nio.ByteBuffer)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {java.nio.ByteBuffer.class}
    )
    public void testPutByteBuffer() {
        ByteBuffer other = ByteBuffer.allocate(buf.capacity());
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.put(other);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            try {
                buf.clear();
                buf.put((ByteBuffer)null);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        try {
            buf.put(buf);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            buf.put(ByteBuffer.allocate(buf.capacity() + 1));
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        
        try {
            buf.put((ByteBuffer)null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        loadTestData2(other);
        other.clear();
        buf.clear();
        ByteBuffer ret = buf.put(other);
        assertEquals(other.position(), other.capacity());
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(other, buf);
        assertSame(ret, buf);
    }

    /*
     * Class under test for java.nio.ByteBuffer put(int, byte)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "put",
        args = {int.class, byte.class}
    )
    public void testPutintbyte() {
        if (buf.isReadOnly()) {
            try {
                buf.put(0, (byte) 0);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(0, buf.position());
            ByteBuffer ret = buf.put(i, (byte) i);
            assertEquals((byte) i, buf.get(i));
            assertSame(ret, buf);
        }
        try {
            buf.put(-1, (byte) 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put(buf.limit(), (byte) 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.put(0, Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, buf.get(0));
        buf.put(0, Byte.MIN_VALUE);
        assertEquals(Byte.MIN_VALUE, buf.get(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "slice",
        args = {}
    )
    public void testSlice() {
        loadTestData1(buf);
        assertTrue(buf.capacity() > SMALL_TEST_LENGTH);
        buf.position(1);
        buf.limit(buf.capacity() - 1);

        ByteBuffer slice = buf.slice();
        assertEquals(buf.isReadOnly(), slice.isReadOnly());
        assertEquals(buf.isDirect(), slice.isDirect());
        assertEquals(buf.order(), slice.order());
        assertEquals(0, slice.position());
        assertEquals(buf.remaining(), slice.limit());
        assertEquals(buf.remaining(), slice.capacity());
        try {
            slice.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }

        // slice share the same content with buf
        if (!slice.isReadOnly()) {
            loadTestData1(slice);
            assertContentLikeTestData1(buf, 1, (byte) 0, slice.capacity());
            buf.put(2, (byte) 100);
            assertEquals(100, slice.get(1));
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void testToString() {
        String str = buf.toString();
        assertTrue(str.indexOf("Byte") >= 0 || str.indexOf("byte") >= 0);
        assertTrue(str.indexOf("" + buf.position()) >= 0);
        assertTrue(str.indexOf("" + buf.limit()) >= 0);
        assertTrue(str.indexOf("" + buf.capacity()) >= 0);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asCharBuffer",
        args = {}
    )
    public void testAsCharBuffer() {
        CharBuffer charBuffer;
        byte bytes[] = new byte[2];
        char value;
        loadTestData1(buf);

        // test BIG_ENDIAN char buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        charBuffer = buf.asCharBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, charBuffer.order());
        while (charBuffer.remaining() > 0) {
            buf.get(bytes);
            value = charBuffer.get();
            assertEquals(bytes2char(bytes, buf.order()), value);
        }

        // test LITTLE_ENDIAN char buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        charBuffer = buf.asCharBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, charBuffer.order());
        while (charBuffer.remaining() > 0) {
            buf.get(bytes);
            value = charBuffer.get();
            assertEquals(bytes2char(bytes, buf.order()), value);
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN char buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            charBuffer = buf.asCharBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, charBuffer.order());
            while (charBuffer.remaining() > 0) {
                value = (char) charBuffer.remaining();
                charBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, char2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN char buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            charBuffer = buf.asCharBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, charBuffer.order());
            while (charBuffer.remaining() > 0) {
                value = (char) charBuffer.remaining();
                charBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, char2bytes(value, buf.order())));
            }
        }
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asDoubleBuffer",
        args = {}
    )
    public void testAsDoubleBuffer() {
        DoubleBuffer doubleBuffer;
        byte bytes[] = new byte[8];
        double value;
        loadTestData1(buf);

        // test BIG_ENDIAN double buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        doubleBuffer = buf.asDoubleBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, doubleBuffer.order());
        while (doubleBuffer.remaining() > 0) {
            buf.get(bytes);
            value = doubleBuffer.get();
            if (!(Double.isNaN(bytes2double(bytes, buf.order())) && Double
                    .isNaN(value))) {
                assertEquals(bytes2double(bytes, buf.order()), value, 0.00);
            }
        }

        // test LITTLE_ENDIAN double buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        doubleBuffer = buf.asDoubleBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, doubleBuffer.order());
        while (doubleBuffer.remaining() > 0) {
            buf.get(bytes);
            value = doubleBuffer.get();
            if (!(Double.isNaN(bytes2double(bytes, buf.order())) && Double
                    .isNaN(value))) {
                assertEquals(bytes2double(bytes, buf.order()), value, 0.00);
            }
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN double buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            doubleBuffer = buf.asDoubleBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, doubleBuffer.order());
            while (doubleBuffer.remaining() > 0) {
                value = doubleBuffer.remaining();
                doubleBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, double2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN double buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            doubleBuffer = buf.asDoubleBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, doubleBuffer.order());
            while (doubleBuffer.remaining() > 0) {
                value = doubleBuffer.remaining();
                doubleBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, double2bytes(value, buf.order())));
            }
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asFloatBuffer",
        args = {}
    )
    public void testAsFloatBuffer() {
        FloatBuffer floatBuffer;
        byte bytes[] = new byte[4];
        float value;
        loadTestData1(buf);

        // test BIG_ENDIAN float buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        floatBuffer = buf.asFloatBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, floatBuffer.order());
        while (floatBuffer.remaining() > 0) {
            buf.get(bytes);
            value = floatBuffer.get();
            if (!(Float.isNaN(bytes2float(bytes, buf.order())) && Float
                    .isNaN(value))) {
                assertEquals(bytes2float(bytes, buf.order()), value, 0.00);
            }
        }

        // test LITTLE_ENDIAN float buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        floatBuffer = buf.asFloatBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, floatBuffer.order());
        while (floatBuffer.remaining() > 0) {
            buf.get(bytes);
            value = floatBuffer.get();
            if (!(Float.isNaN(bytes2float(bytes, buf.order())) && Float
                    .isNaN(value))) {
                assertEquals(bytes2float(bytes, buf.order()), value, 0.00);
            }
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN float buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            floatBuffer = buf.asFloatBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, floatBuffer.order());
            while (floatBuffer.remaining() > 0) {
                value = floatBuffer.remaining();
                floatBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, float2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN float buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            floatBuffer = buf.asFloatBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, floatBuffer.order());
            while (floatBuffer.remaining() > 0) {
                value = floatBuffer.remaining();
                floatBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, float2bytes(value, buf.order())));
            }
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asIntBuffer",
        args = {}
    )
    public void testAsIntBuffer() {
        IntBuffer intBuffer;
        byte bytes[] = new byte[4];
        int value;
        loadTestData1(buf);

        // test BIG_ENDIAN int buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        intBuffer = buf.asIntBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, intBuffer.order());
        while (intBuffer.remaining() > 0) {
            buf.get(bytes);
            value = intBuffer.get();
            assertEquals(bytes2int(bytes, buf.order()), value);
        }

        // test LITTLE_ENDIAN int buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        intBuffer = buf.asIntBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, intBuffer.order());
        while (intBuffer.remaining() > 0) {
            buf.get(bytes);
            value = intBuffer.get();
            assertEquals(bytes2int(bytes, buf.order()), value);
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN int buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            intBuffer = buf.asIntBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, intBuffer.order());
            while (intBuffer.remaining() > 0) {
                value = intBuffer.remaining();
                intBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, int2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN int buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            intBuffer = buf.asIntBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, intBuffer.order());
            while (intBuffer.remaining() > 0) {
                value = intBuffer.remaining();
                intBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, int2bytes(value, buf.order())));
            }
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asLongBuffer",
        args = {}
    )
    public void testAsLongBuffer() {
        LongBuffer longBuffer;
        byte bytes[] = new byte[8];
        long value;
        loadTestData1(buf);

        // test BIG_ENDIAN long buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        longBuffer = buf.asLongBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, longBuffer.order());
        while (longBuffer.remaining() > 0) {
            buf.get(bytes);
            value = longBuffer.get();
            assertEquals(bytes2long(bytes, buf.order()), value);
        }

        // test LITTLE_ENDIAN long buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        longBuffer = buf.asLongBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, longBuffer.order());
        while (longBuffer.remaining() > 0) {
            buf.get(bytes);
            value = longBuffer.get();
            assertEquals(bytes2long(bytes, buf.order()), value);
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN long buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            longBuffer = buf.asLongBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, longBuffer.order());
            while (longBuffer.remaining() > 0) {
                value = longBuffer.remaining();
                longBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, long2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN long buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            longBuffer = buf.asLongBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, longBuffer.order());
            while (longBuffer.remaining() > 0) {
                value = longBuffer.remaining();
                longBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, long2bytes(value, buf.order())));
            }
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asShortBuffer",
        args = {}
    )
    public void testAsShortBuffer() {
        ShortBuffer shortBuffer;
        byte bytes[] = new byte[2];
        short value;
        loadTestData1(buf);

        // test BIG_ENDIAN short buffer, read
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        shortBuffer = buf.asShortBuffer();
        assertSame(ByteOrder.BIG_ENDIAN, shortBuffer.order());
        while (shortBuffer.remaining() > 0) {
            buf.get(bytes);
            value = shortBuffer.get();
            assertEquals(bytes2short(bytes, buf.order()), value);
        }

        // test LITTLE_ENDIAN short buffer, read
        buf.clear();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        shortBuffer = buf.asShortBuffer();
        assertSame(ByteOrder.LITTLE_ENDIAN, shortBuffer.order());
        while (shortBuffer.remaining() > 0) {
            buf.get(bytes);
            value = shortBuffer.get();
            assertEquals(bytes2short(bytes, buf.order()), value);
        }

        if (!buf.isReadOnly()) {
            // test BIG_ENDIAN short buffer, write
            buf.clear();
            buf.order(ByteOrder.BIG_ENDIAN);
            shortBuffer = buf.asShortBuffer();
            assertSame(ByteOrder.BIG_ENDIAN, shortBuffer.order());
            while (shortBuffer.remaining() > 0) {
                value = (short) shortBuffer.remaining();
                shortBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, short2bytes(value, buf.order())));
            }

            // test LITTLE_ENDIAN short buffer, write
            buf.clear();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            shortBuffer = buf.asShortBuffer();
            assertSame(ByteOrder.LITTLE_ENDIAN, shortBuffer.order());
            while (shortBuffer.remaining() > 0) {
                value = (short) shortBuffer.remaining();
                shortBuffer.put(value);
                buf.get(bytes);
                assertTrue(Arrays.equals(bytes, short2bytes(value, buf.order())));
            }
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getChar",
        args = {}
    )
    public void testGetChar() {
        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        char value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getChar();
            assertEquals(bytes2char(bytes, buf.order()), value);
        }

        try {
            buf.getChar();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getChar",
        args = {int.class}
    )
    public void testGetCharint() {
        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        char value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getChar(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertEquals(bytes2char(bytes, buf.order()), value);
        }

        try {
            buf.getChar(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getChar(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putChar",
        args = {char.class}
    )
    public void testPutChar() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putChar((char) 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        char value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = (char) i;
            buf.mark();
            buf.putChar(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(char2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putChar(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putChar(0, Character.MAX_VALUE);
        assertEquals(Character.MAX_VALUE, buf.getChar(0));
        buf.rewind();
        buf.putChar(0, Character.MIN_VALUE);
        assertEquals(Character.MIN_VALUE, buf.getChar(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putChar",
        args = {int.class, char.class}
    )
    public void testPutCharint() {
        if (buf.isReadOnly()) {
            try {
                buf.putChar(0, (char) 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        char value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = (char) i;
            buf.position(i);
            buf.putChar(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(char2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putChar(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putChar(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        try {
            ByteBuffer.allocateDirect(16).putChar(Integer.MAX_VALUE, 'h');
        } catch (IndexOutOfBoundsException e) {
            //expected 
        }

        buf.putChar(0, Character.MAX_VALUE);
        assertEquals(Character.MAX_VALUE, buf.getChar(0));
        buf.putChar(0, Character.MIN_VALUE);
        assertEquals(Character.MIN_VALUE, buf.getChar(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getDouble",
        args = {}
    )
    public void testGetDouble() {
        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        double value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getDouble();
            if (!(Double.isNaN(bytes2double(bytes, buf.order())) && Double
                    .isNaN(value))) {
                assertEquals(bytes2double(bytes, buf.order()), value, 0.00);
            }
        }

        try {
            buf.getDouble();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getDouble",
        args = {int.class}
    )
    public void testGetDoubleint() {
        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        double value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getDouble(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            if (!(Double.isNaN(bytes2double(bytes, buf.order())) && Double
                    .isNaN(value))) {
                assertEquals(bytes2double(bytes, buf.order()), value, 0.00);
            }
        }

        try {
            buf.getDouble(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getDouble(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        try {
            ByteBuffer.allocateDirect(16).getDouble(Integer.MAX_VALUE);
        } catch (IndexOutOfBoundsException e) {
            //expected 
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putDouble",
        args = {double.class}
    )
    public void testPutDouble() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putDouble(1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        double value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.mark();
            buf.putDouble(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(double2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putDouble(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putDouble(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, buf.getDouble(0));
        buf.rewind();
        buf.putDouble(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, buf.getDouble(0));
        buf.rewind();
        buf.putDouble(Double.NaN);
        assertEquals(Double.NaN, buf.getDouble(0));
        buf.rewind();
        buf.putDouble(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, buf.getDouble(0));
        buf.rewind();
        buf.putDouble(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, buf.getDouble(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putDouble",
        args = {int.class, double.class}
    )
    public void testPutDoubleint() {
        if (buf.isReadOnly()) {
            try {
                buf.putDouble(0, 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        double value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.position(i);
            buf.putDouble(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(double2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putDouble(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putDouble(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putDouble(0, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, buf.getDouble(0));
        buf.putDouble(0, Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, buf.getDouble(0));
        buf.putDouble(0, Double.NaN);
        assertEquals(Double.NaN, buf.getDouble(0));
        buf.putDouble(0, Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, buf.getDouble(0));
        buf.putDouble(0, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, buf.getDouble(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getFloat",
        args = {}
    )
    public void testGetFloat() {
        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        float value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getFloat();
            if (!(Float.isNaN(bytes2float(bytes, buf.order())) && Float
                    .isNaN(value))) {
                assertEquals(bytes2float(bytes, buf.order()), value, 0.00);
            }
        }

        try {
            buf.getFloat();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getFloat",
        args = {int.class}
    )
    public void testGetFloatint() {
        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        float value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getFloat(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            if (!(Float.isNaN(bytes2float(bytes, buf.order())) && Float
                    .isNaN(value))) {
                assertEquals(bytes2float(bytes, buf.order()), value, 0.00);
            }
        }

        try {
            buf.getFloat(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getFloat(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putFloat",
        args = {float.class}
    )
    public void testPutFloat() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putFloat(1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        float value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.mark();
            buf.putFloat(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(float2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putFloat(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putFloat(Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, buf.getFloat(0));
        buf.rewind();
        buf.putFloat(Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, buf.getFloat(0));
        buf.rewind();
        buf.putFloat(Float.NaN);
        assertEquals(Float.NaN, buf.getFloat(0));
        buf.rewind();
        buf.putFloat(Float.NEGATIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, buf.getFloat(0));
        buf.rewind();
        buf.putFloat(Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, buf.getFloat(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putFloat",
        args = {int.class, float.class}
    )
    public void testPutFloatint() {
        if (buf.isReadOnly()) {
            try {
                buf.putFloat(0, 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        float value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.position(i);
            buf.putFloat(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(float2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putFloat(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putFloat(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putFloat(0, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, buf.getFloat(0));
        buf.putFloat(0, Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, buf.getFloat(0));
        buf.putFloat(0, Float.NaN);
        assertEquals(Float.NaN, buf.getFloat(0));
        buf.putFloat(0, Float.NEGATIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, buf.getFloat(0));
        buf.putFloat(0, Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, buf.getFloat(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getInt",
        args = {}
    )
    public void testGetInt() {
        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        int value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getInt();
            assertEquals(bytes2int(bytes, buf.order()), value);
        }

        try {
            buf.getInt();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getInt",
        args = {int.class}
    )
    public void testGetIntint() {
        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        int value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getInt(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertEquals(bytes2int(bytes, buf.order()), value);
        }

        try {
            buf.getInt(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getInt(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
        try {
            ByteBuffer.allocateDirect(16).getInt(Integer.MAX_VALUE);
        } catch (IndexOutOfBoundsException e) {
            //expected 
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putInt",
        args = {int.class}
    )
    public void testPutInt() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putInt(1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        int value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.mark();
            buf.putInt(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(int2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putInt(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, buf.getInt(0));
        buf.rewind();
        buf.putInt(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, buf.getInt(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putInt",
        args = {int.class, int.class}
    )
    public void testPutIntint() {
        if (buf.isReadOnly()) {
            try {
                buf.putInt(0, 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 4;
        byte bytes[] = new byte[nbytes];
        int value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.position(i);
            buf.putInt(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(int2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putInt(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putInt(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putInt(0, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, buf.getInt(0));
        buf.putInt(0, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, buf.getInt(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getLong",
        args = {}
    )
    public void testGetLong() {
        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        long value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getLong();
            assertEquals(bytes2long(bytes, buf.order()), value);
        }

        try {
            buf.getLong();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getLong",
        args = {int.class}
    )
    public void testGetLongint() {
        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        long value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getLong(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertEquals(bytes2long(bytes, buf.order()), value);
        }

        try {
            buf.getLong(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getLong(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putLong",
        args = {long.class}
    )
    public void testPutLong() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putLong(1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        long value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.mark();
            buf.putLong(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(long2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putLong(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putLong(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, buf.getLong(0));
        buf.rewind();
        buf.putLong(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, buf.getLong(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putLong",
        args = {int.class, long.class}
    )
    public void testPutLongint() {
        if (buf.isReadOnly()) {
            try {
                buf.putLong(0, 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 8;
        byte bytes[] = new byte[nbytes];
        long value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = i;
            buf.position(i);
            buf.putLong(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(long2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putLong(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putLong(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putLong(0, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, buf.getLong(0));
        buf.putLong(0, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, buf.getLong(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getShort",
        args = {}
    )
    public void testGetShort() {
        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        short value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            assertEquals(i * nbytes, buf.position());
            buf.mark();
            buf.get(bytes);
            buf.reset();
            value = buf.getShort();
            assertEquals(bytes2short(bytes, buf.order()), value);
        }

        try {
            buf.getShort();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getShort",
        args = {int.class}
    )
    public void testGetShortint() {
        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        short value;
        loadTestData1(buf);

        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            buf.position(i);
            value = buf.getShort(i);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertEquals(bytes2short(bytes, buf.order()), value);
        }

        try {
            buf.getShort(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.getShort(buf.limit() - nbytes + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putShort",
        args = {short.class}
    )
    public void testPutShort() {
        if (buf.isReadOnly()) {
            try {
                buf.clear();
                buf.putShort((short) 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        short value = 0;
        buf.clear();
        for (int i = 0; buf.remaining() >= nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = (short) i;
            buf.mark();
            buf.putShort(value);
            assertEquals((i + 1) * nbytes, buf.position());
            buf.reset();
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(short2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putShort(value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.rewind();
        buf.putShort(Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, buf.getShort(0));
        buf.rewind();
        buf.putShort(Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, buf.getShort(0));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "putShort",
        args = {int.class, short.class}
    )
    public void testPutShortint() {
        if (buf.isReadOnly()) {
            try {
                buf.putShort(0, (short) 1);
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (ReadOnlyBufferException e) {
                // expected
            }
            return;
        }

        int nbytes = 2;
        byte bytes[] = new byte[nbytes];
        short value = 0;
        buf.clear();
        for (int i = 0; i <= buf.limit() - nbytes; i++) {
            buf.order(i % 2 == 0 ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            value = (short) i;
            buf.position(i);
            buf.putShort(i, value);
            assertEquals(i, buf.position());
            buf.get(bytes);
            assertTrue("Wrong value at " + i,
                    Arrays.equals(short2bytes(value, buf.order()), bytes));
        }

        try {
            buf.putShort(-1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.putShort(buf.limit() - nbytes + 1, value);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putShort(0, Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, buf.getShort(0));
        buf.putShort(0, Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, buf.getShort(0));
    }
    
    /**
     * @tests java.nio.ByteBuffer.wrap(byte[],int,int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression test. Verifies NullPointerException, IndexOutOfBoundsException.",
        method = "wrap",
        args = {byte[].class, int.class, int.class}
    )
    public void testWrappedByteBuffer_null_array() {
        // Regression for HARMONY-264
        byte array[] = null;
        try {
            ByteBuffer.wrap(array, -1, 0);
            fail("Should throw NPE"); //$NON-NLS-1$
        } catch (NullPointerException e) {
        }
        try {
            ByteBuffer.wrap(new byte[10], Integer.MAX_VALUE, 2);
            fail("Should throw IndexOutOfBoundsException"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
        }
    }

    /*
     * test for method static ByteBuffer wrap(byte[] array)
     * test covers following usecases:
     * 1. case for check ByteBuffer buf2 properties
     * 2. case for check equal between buf2 and byte array[]
     * 3. case for check a buf2 dependens to array[]
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "wrap",
        args = {byte[].class}
    )
    public void test_Wrap$B() {
        byte array[] = new byte[BUFFER_LENGTH];
        loadTestData1(array, 0, BUFFER_LENGTH);
        ByteBuffer buf2 = ByteBuffer.wrap(array);

        // case: ByteBuffer buf2 properties is satisfy the conditions specification
        assertEquals(array.length, buf2.capacity());
        assertEquals(array.length, buf2.limit());
        assertEquals(0, buf2.position());

        //     case: ByteBuffer buf2 is equal to byte array[]
        assertContentEquals(buf2, array, 0, array.length);

        // case: ByteBuffer buf2 is depended to byte array[]
        loadTestData2(array, 0, buf.capacity());
        assertContentEquals(buf2, array, 0, array.length);
    }

    /*
     * test for method static ByteBuffer wrap(byte[] array, int offset, int length)
     * test covers following usecases:
     * 1. case for check ByteBuffer buf2 properties
     * 2. case for check equal between buf2 and byte array[]
     * 3. case for check a buf2 dependens to array[]
     * 4. case expected IndexOutOfBoundsException  
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "wrap",
        args = {byte[].class, int.class, int.class}
    )
    public void test_Wrap$BII() {
        byte array[] = new byte[BUFFER_LENGTH];
        int offset = 5;
        int length = BUFFER_LENGTH - offset;
        loadTestData1(array, 0, BUFFER_LENGTH);
        ByteBuffer buf2 = ByteBuffer.wrap(array, offset, length);

        // case: ByteBuffer buf2 properties is satisfy the conditions specification
        assertEquals(array.length, buf2.capacity());
        assertEquals(offset, buf2.position());
        assertEquals(offset + length, buf2.limit());
        assertEquals(0, buf2.arrayOffset());

        //     case: ByteBuffer buf2 is equal to byte array[]
        assertContentEquals(buf2, array, 0, array.length);

        // case: ByteBuffer buf2 is depended to byte array[]
        loadTestData2(array, 0, buf.capacity());
        assertContentEquals(buf2, array, 0, array.length);

        //     case: expected IndexOutOfBoundsException
        try {
            offset = 7;
            buf2 = ByteBuffer.wrap(array, offset, length);
            fail("wrap method does not throws expected exception");
        } catch (IndexOutOfBoundsException e) {
            //expected
        }
    }

    protected void loadTestData1(byte array[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = (byte) i;
        }
    }

    protected void loadTestData2(byte array[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = (byte) (length - i);
        }
    }

    protected void loadTestData1(ByteBuffer buf) {
        if (buf.isReadOnly()) {
            return;
        }
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, (byte) i);
        }
    }

    protected void loadTestData2(ByteBuffer buf) {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, (byte) (buf.capacity() - i));
        }
    }

    private void assertContentEquals(ByteBuffer buf, byte array[],
            int offset, int length) {
        for (int i = 0; i < length; i++) {
            assertEquals(array[offset + i], buf.get(i));
        }
    }

    private void assertContentEquals(ByteBuffer buf, ByteBuffer other) {
        assertEquals(buf.capacity(), other.capacity());
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.get(i), other.get(i));
        }
    }

    private void assertContentLikeTestData1(ByteBuffer buf,
            int startIndex, byte startValue, int length) {
        byte value = startValue;
        for (int i = 0; i < length; i++) {
            assertEquals(buf.get(startIndex + i), value);
            value = (byte) (value + 1);
        }
    }

    private int bytes2int(byte bytes[], ByteOrder order) {
        int nbytes = 4, bigHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            bigHead = 0;
            step = 1;
        } else {
            bigHead = nbytes - 1;
            step = -1;
        }
        int result = 0;
        int p = bigHead;
        for (int i = 0; i < nbytes; i++) {
            result = result << 8;
            result = result | (bytes[p] & 0xff);
            p += step;
        }
        return result;
    }

    private long bytes2long(byte bytes[], ByteOrder order) {
        int nbytes = 8, bigHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            bigHead = 0;
            step = 1;
        } else {
            bigHead = nbytes - 1;
            step = -1;
        }
        long result = 0;
        int p = bigHead;
        for (int i = 0; i < nbytes; i++) {
            result = result << 8;
            result = result | (bytes[p] & 0xff);
            p += step;
        }
        return result;
    }

    private short bytes2short(byte bytes[], ByteOrder order) {
        int nbytes = 2, bigHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            bigHead = 0;
            step = 1;
        } else {
            bigHead = nbytes - 1;
            step = -1;
        }
        short result = 0;
        int p = bigHead;
        for (int i = 0; i < nbytes; i++) {
            result = (short) (result << 8);
            result = (short) (result | (bytes[p] & 0xff));
            p += step;
        }
        return result;
    }

    private char bytes2char(byte bytes[], ByteOrder order) {
        return (char) bytes2short(bytes, order);
    }

    private float bytes2float(byte bytes[], ByteOrder order) {
        return Float.intBitsToFloat(bytes2int(bytes, order));
    }

    private double bytes2double(byte bytes[], ByteOrder order) {
        return Double.longBitsToDouble(bytes2long(bytes, order));
    }

    private byte[] int2bytes(int value, ByteOrder order) {
        int nbytes = 4, smallHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            smallHead = nbytes - 1;
            step = -1;
        } else {
            smallHead = 0;
            step = 1;
        }
        byte bytes[] = new byte[nbytes];
        int p = smallHead;
        for (int i = 0; i < nbytes; i++) {
            bytes[p] = (byte) (value & 0xff);
            value = value >> 8;
            p += step;
        }
        return bytes;
    }

    private byte[] long2bytes(long value, ByteOrder order) {
        int nbytes = 8, smallHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            smallHead = nbytes - 1;
            step = -1;
        } else {
            smallHead = 0;
            step = 1;
        }
        byte bytes[] = new byte[nbytes];
        int p = smallHead;
        for (int i = 0; i < nbytes; i++) {
            bytes[p] = (byte) (value & 0xff);
            value = value >> 8;
            p += step;
        }
        return bytes;
    }

    private byte[] short2bytes(short value, ByteOrder order) {
        int nbytes = 2, smallHead, step;
        if (order == ByteOrder.BIG_ENDIAN) {
            smallHead = nbytes - 1;
            step = -1;
        } else {
            smallHead = 0;
            step = 1;
        }
        byte bytes[] = new byte[nbytes];
        int p = smallHead;
        for (int i = 0; i < nbytes; i++) {
            bytes[p] = (byte) (value & 0xff);
            value = (short) (value >> 8);
            p += step;
        }
        return bytes;
    }

    private byte[] char2bytes(char value, ByteOrder order) {
        return short2bytes((short) value, order);
    }

    private byte[] float2bytes(float value, ByteOrder order) {
        return int2bytes(Float.floatToRawIntBits(value), order);
    }

    private byte[] double2bytes(double value, ByteOrder order) {
        return long2bytes(Double.doubleToRawLongBits(value), order);
    }
}
