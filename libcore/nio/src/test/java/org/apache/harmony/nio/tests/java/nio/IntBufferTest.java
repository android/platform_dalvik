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
import java.nio.IntBuffer;
import java.nio.InvalidMarkException;

/**
 * Tests java.nio.IntBuffer
 * 
 */
@TestTargetClass(java.nio.IntBuffer.class)
public abstract class IntBufferTest extends AbstractBufferTest {
    
    
    protected static final int SMALL_TEST_LENGTH = 5;

    protected static final int BUFFER_LENGTH = 20;

    protected IntBuffer buf;

    protected void setUp() throws Exception {
        capacity = BUFFER_LENGTH;
        buf = IntBuffer.allocate(BUFFER_LENGTH);
        loadTestData1(buf);
        baseBuf = buf;
    }

    protected void tearDown() throws Exception {
        buf = null;
        baseBuf = null;
    }

    /*
     * test for method static IntBuffer allocate(int capacity) test covers
     * following usecases: 1. case for check IntBuffer testBuf properties 2.
     * case expected IllegalArgumentException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "allocate",
        args = {int.class}
    )
    public void test_AllocateI() {
        // case: IntBuffer testBuf properties is satisfy the conditions
        // specification
        IntBuffer testBuf = IntBuffer.allocate(20);
        assertEquals(0, testBuf.position());
        assertNotNull(testBuf.array());
        assertEquals(0, testBuf.arrayOffset());
        assertEquals(20, testBuf.limit());
        assertEquals(20, testBuf.capacity());

        testBuf = IntBuffer.allocate(0);
        assertEquals(0, testBuf.position());
        assertNotNull(testBuf.array());
        assertEquals(0, testBuf.arrayOffset());
        assertEquals(0, testBuf.limit());
        assertEquals(0, testBuf.capacity());

        // case: expected IllegalArgumentException
        try {
            testBuf = IntBuffer.allocate(-20);
            fail("allocate method does not throws expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "array",
        args = {}
    )
    public void testArray() {
        int array[] = buf.array();
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
    public  void testArrayOffset() {
        int array[] = buf.array();
        for(int i = 0; i < buf.capacity(); i++) {
            array[i] = i;
        }
        int offset = buf.arrayOffset();
        assertContentEquals(buf, array, offset, buf.capacity());

        IntBuffer wrapped = IntBuffer.wrap(array, 3, array.length - 3);
        
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
    public  void testAsReadOnlyBuffer() {
        buf.clear();
        buf.mark();
        buf.position(buf.limit());

        // readonly's contents should be the same as buf
        IntBuffer readonly = buf.asReadOnlyBuffer();
        assertNotSame(buf, readonly);
        assertTrue(readonly.isReadOnly());
        assertEquals(buf.position(), readonly.position());
        assertEquals(buf.limit(), readonly.limit());
        assertEquals(buf.isDirect(), readonly.isDirect());
        assertEquals(buf.order(), readonly.order());
        assertContentEquals(buf, readonly);

        // readonly's position, mark, and limit should be independent to buf
        readonly.reset();
        assertEquals(readonly.position(), 0);
        readonly.clear();
        assertEquals(buf.position(), buf.limit());
        buf.reset();
        assertEquals(buf.position(), 0);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "compact",
        args = {}
    )
    @AndroidOnly("Fails on RI. See comment below")
    public  void testCompact() {
        // case: buffer is full
        buf.clear();
        buf.mark();
        loadTestData1(buf);
        IntBuffer ret = buf.compact();
        assertSame(ret, buf);
        assertEquals(buf.position(), buf.capacity());
        assertEquals(buf.limit(), buf.capacity());
        assertContentLikeTestData1(buf, 0, 0, buf.capacity());
        try {
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
        assertEquals(buf.position(), 0);
        assertEquals(buf.limit(), buf.capacity());
        assertContentLikeTestData1(buf, 0, 0, buf.capacity());
        try {
            // Fails on RI. Spec doesn't specify the behavior if
	    // actually nothing to be done by compact(). So RI doesn't reset
            // mark position 
            buf.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }

        // case: normal
        assertTrue(buf.capacity() > 5);
        buf.position(1);
        buf.limit(5);
        buf.mark();
        ret = buf.compact();
        assertSame(ret, buf);
        assertEquals(buf.position(), 4);
        assertEquals(buf.limit(), buf.capacity());
        assertContentLikeTestData1(buf, 0, 1, 4);
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
        args = {java.nio.IntBuffer.class}
    )
    public  void testCompareTo() {
        // compare to self
        assertEquals(0, buf.compareTo(buf));

        // normal cases
        assertTrue(buf.capacity() > 5);
        buf.clear();
        IntBuffer other = IntBuffer.allocate(buf.capacity());
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
        other.limit(5);
        assertTrue(buf.compareTo(other) > 0);
        assertTrue(other.compareTo(buf) < 0);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "duplicate",
        args = {}
    )
    public  void testDuplicate() {
        buf.clear();
        buf.mark();
        buf.position(buf.limit());

        // duplicate's contents should be the same as buf
        IntBuffer duplicate = buf.duplicate();
        assertNotSame(buf, duplicate);
        assertEquals(buf.position(), duplicate.position());
        assertEquals(buf.limit(), duplicate.limit());
        assertEquals(buf.isReadOnly(), duplicate.isReadOnly());
        assertEquals(buf.isDirect(), duplicate.isDirect());
        assertEquals(buf.order(), duplicate.order());
        assertContentEquals(buf, duplicate);

        // duplicate's position, mark, and limit should be independent to buf
        duplicate.reset();
        assertEquals(duplicate.position(), 0);
        duplicate.clear();
        assertEquals(buf.position(), buf.limit());
        buf.reset();
        assertEquals(buf.position(), 0);

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
    public  void testEquals() {
        // equal to self
        assertTrue(buf.equals(buf));
        IntBuffer readonly = buf.asReadOnlyBuffer();
        assertTrue(buf.equals(readonly));
        IntBuffer duplicate = buf.duplicate();
        assertTrue(buf.equals(duplicate));

        // always false, if type mismatch
        assertFalse(buf.equals(Boolean.TRUE));

        assertTrue(buf.capacity() > 5);

        buf.limit(buf.capacity()).position(0);
        readonly.limit(readonly.capacity()).position(1);
        assertFalse(buf.equals(readonly));

        buf.limit(buf.capacity() - 1).position(0);
        duplicate.limit(duplicate.capacity()).position(0);
        assertFalse(buf.equals(duplicate));
    }

    /*
     * Class under test for int get()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {}
    )
    public  void testGet() {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), i);
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
     * Class under test for java.nio.IntBuffer get(int[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {int[].class}
    )
    public  void testGetintArray() {
        int array[] = new int[1];
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), i);
            IntBuffer ret = buf.get(array);
            assertEquals(array[0], buf.get(i));
            assertSame(ret, buf);
        }

        buf.get(new int[0]);

        try {
            buf.get(array);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }

        try {
            buf.get((int[])null);
            fail("Should throw NPE"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.IntBuffer get(int[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {int[].class, int.class, int.class}
    )
    public  void testGetintArrayintint() {
        buf.clear();
        int array[] = new int[buf.capacity()];

        try {
            buf.get(new int[buf.capacity() + 1], 0, buf.capacity() + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(buf.position(), 0);
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
        assertEquals(buf.position(), 0);
        try {
            buf.get(array, 2, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.get((int[])null, 2, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        try {
            buf.get(array, 2, array.length);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
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
        assertEquals(buf.position(), 0);

        buf.clear();
        IntBuffer ret = buf.get(array, 0, array.length);
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(buf, array, 0, array.length);
        assertSame(ret, buf);
    }

    /*
     * Class under test for int get(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "get",
        args = {int.class}
    )
    public  void testGetint() {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), i);
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
        notes = "",
        method = "hasArray",
        args = {}
    )
    public  void testHasArray() {
        if (buf.hasArray()) {
            assertNotNull(buf.array());
        } else {
            try {
                buf.array();
                fail("Should throw Exception"); //$NON-NLS-1$
            } catch (UnsupportedOperationException e) {
                // expected
                // Note:can not tell when to catch 
                // UnsupportedOperationException or
                // ReadOnlyBufferException, so catch all.
            }
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public  void testHashCode() {
        buf.clear();
        IntBuffer readonly = buf.asReadOnlyBuffer();
        IntBuffer duplicate = buf.duplicate();
        assertTrue(buf.hashCode() == readonly.hashCode());

        assertTrue(buf.capacity() > 5);
        duplicate.position(buf.capacity() / 2);
        assertTrue(buf.hashCode() != duplicate.hashCode());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies isDirect method for non direct buffer.",
        method = "isDirect",
        args = {}
    )
    public  void testIsDirect() {
        assertFalse(buf.isDirect());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Abstract method.",
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
    public  void testOrder() {
        assertEquals(ByteOrder.nativeOrder(), buf.order());
    }

    /*
     * Class under test for java.nio.IntBuffer put(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ReadOnlyBufferException.",
        method = "put",
        args = {int.class}
    )
    public  void testPutint() {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), i);
            IntBuffer ret = buf.put((int) i);
            assertEquals(buf.get(i), (int) i);
            assertSame(ret, buf);
        }
        try {
            buf.put(0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.IntBuffer put(int[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ReadOnlyBufferException.",
        method = "put",
        args = {int[].class}
    )
    public  void testPutintArray() {
        int array[] = new int[1];
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), i);
            array[0] = (int) i;
            IntBuffer ret = buf.put(array);
            assertEquals(buf.get(i), (int) i);
            assertSame(ret, buf);
        }
        try {
            buf.put(array);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        try {
            buf.position(buf.limit());
            buf.put((int[])null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.nio.IntBuffer put(int[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ReadOnlyBufferException.",
        method = "put",
        args = {int[].class, int.class, int.class}
    )
    public  void testPutintArrayintint() {
        buf.clear();
        int array[] = new int[buf.capacity()];
        try {
            buf.put(new int[buf.capacity() + 1], 0, buf.capacity() + 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        assertEquals(buf.position(), 0);
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
        assertEquals(buf.position(), 0);
        try {
            buf.put(array, 0, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put((int[])null, 0, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        try {
            buf.put(array, 2, array.length);
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
            buf.put(array, 1, Integer.MAX_VALUE);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        assertEquals(buf.position(), 0);

        loadTestData2(array, 0, array.length);
        IntBuffer ret = buf.put(array, 0, array.length);
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(buf, array, 0, array.length);
        assertSame(ret, buf);
    }
    
    /*
     * Class under test for java.nio.IntBuffer put(int[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression Test",
        method = "put",
        args = {int[].class, int.class, int.class}
    )
    public  void testPutintArrayintint2() {
        // Regression test
        ByteBuffer buf = ByteBuffer.allocateDirect(20);
        IntBuffer intBuf = buf.asIntBuffer();
        int[] src = new int[5];
        intBuf.put(src);
        intBuf.clear();
        try {
            intBuf.put(src);
        } catch (BufferOverflowException e) {
            fail("should not throw a BufferOverflowException");
        }
    }

    /*
     * Class under test for java.nio.IntBuffer put(java.nio.IntBuffer)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ReadOnlyBufferException.",
        method = "put",
        args = {java.nio.IntBuffer.class}
    )
    public  void testPutIntBuffer() {
        IntBuffer other = IntBuffer.allocate(buf.capacity());
        try {
            buf.put(buf);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            buf.put(IntBuffer.allocate(buf.capacity() + 1));
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (BufferOverflowException e) {
            // expected
        }
        try {
            buf.flip();
            buf.put((IntBuffer)null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }

        loadTestData2(other);
        other.clear();
        buf.clear();
        IntBuffer ret = buf.put(other);
        assertEquals(other.position(), other.capacity());
        assertEquals(buf.position(), buf.capacity());
        assertContentEquals(other, buf);
        assertSame(ret, buf);
    }

    /*
     * Class under test for java.nio.IntBuffer put(int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ReadOnlyBufferException.",
        method = "put",
        args = {int.class, int.class}
    )
    public  void testPutintint() {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.position(), 0);
            IntBuffer ret = buf.put(i, (int) i);
            assertEquals(buf.get(i), (int) i);
            assertSame(ret, buf);
        }
        try {
            buf.put(-1, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            buf.put(buf.limit(), 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "slice",
        args = {}
    )
    public  void testSlice() {
        assertTrue(buf.capacity() > 5);
        buf.position(1);
        buf.limit(buf.capacity() - 1);

        IntBuffer slice = buf.slice();
        assertEquals(buf.isReadOnly(), slice.isReadOnly());
        assertEquals(buf.isDirect(), slice.isDirect());
        assertEquals(buf.order(), slice.order());
        assertEquals(slice.position(), 0);
        assertEquals(slice.limit(), buf.remaining());
        assertEquals(slice.capacity(), buf.remaining());
        try {
            slice.reset();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (InvalidMarkException e) {
            // expected
        }

        // slice share the same content with buf
        if (!slice.isReadOnly()) {
            loadTestData1(slice);
            assertContentLikeTestData1(buf, 1, 0, slice.capacity());
            buf.put(2, 500);
            assertEquals(slice.get(1), 500);
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public  void testToString() {
        String str = buf.toString();
        assertTrue(str.indexOf("Int") >= 0 || str.indexOf("int") >= 0);
        assertTrue(str.indexOf("" + buf.position()) >= 0);
        assertTrue(str.indexOf("" + buf.limit()) >= 0);
        assertTrue(str.indexOf("" + buf.capacity()) >= 0);
    }

    /*
     * test for method static IntBuffer wrap(int[] array) test covers following
     * usecases: 1. case for check IntBuffer buf2 properties 2. case for check
     * equal between buf2 and int array[] 3. case for check a buf2 dependens to
     * array[]
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "wrap",
        args = {int[].class}
    )
    public void test_Wrap$I() {
        int array[] = new int[BUFFER_LENGTH];
        loadTestData1(array, 0, BUFFER_LENGTH);
        IntBuffer buf2 = IntBuffer.wrap(array);

        // case: IntBuffer buf2 properties is satisfy the conditions
        // specification
        assertEquals(buf2.capacity(), array.length);
        assertEquals(buf2.limit(), array.length);
        assertEquals(buf2.position(), 0);

        // case: IntBuffer buf2 is equal to int array[]
        assertContentEquals(buf2, array, 0, array.length);

        // case: IntBuffer buf2 is depended to int array[]
        loadTestData2(array, 0, buf.capacity());
        assertContentEquals(buf2, array, 0, array.length);
    }

    /*
     * test for method static IntBuffer wrap(int[] array, int offset, int
     * length) test covers following usecases: 1. case for check IntBuffer buf2
     * properties 2. case for check equal between buf2 and int array[] 3. case
     * for check a buf2 dependens to array[] 4. case expected
     * IndexOutOfBoundsException
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "wrap",
        args = {int[].class, int.class, int.class}
    )
    public void test_Wrap$III() {
        int array[] = new int[BUFFER_LENGTH];
        int offset = 5;
        int length = BUFFER_LENGTH - offset;
        loadTestData1(array, 0, BUFFER_LENGTH);
        IntBuffer buf2 = IntBuffer.wrap(array, offset, length);

        // case: IntBuffer buf2 properties is satisfy the conditions
        // specification
        assertEquals(buf2.capacity(), array.length);
        assertEquals(buf2.position(), offset);
        assertEquals(buf2.limit(), offset + length);
        assertEquals(buf2.arrayOffset(), 0);

        // case: IntBuffer buf2 is equal to int array[]
        assertContentEquals(buf2, array, 0, array.length);

        // case: IntBuffer buf2 is depended to int array[]
        loadTestData2(array, 0, buf.capacity());
        assertContentEquals(buf2, array, 0, array.length);

        // case: expected IndexOutOfBoundsException
        try {
            offset = 7;
            buf2 = IntBuffer.wrap(array, offset, length);
            fail("wrap method does not throws expected exception");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    void loadTestData1(int array[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = (int)i;
        }
    }

    void loadTestData2(int array[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = (int)length - i;
        }
    }

    void loadTestData1(IntBuffer buf) {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, (int)i);
        }
    }

    void loadTestData2(IntBuffer buf) {
        buf.clear();
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, (int)buf.capacity() - i);
        }
    }

      void assertContentEquals(IntBuffer buf, int array[],
            int offset, int length) {
        for (int i = 0; i < length; i++) {
            assertEquals(buf.get(i), array[offset + i]);
        }
    }

      void assertContentEquals(IntBuffer buf, IntBuffer other) {
        assertEquals(buf.capacity(), other.capacity());
        for (int i = 0; i < buf.capacity(); i++) {
            assertEquals(buf.get(i), other.get(i));
        }
    }

      void assertContentLikeTestData1(IntBuffer buf,
            int startIndex, int startValue, int length) {
        int value = startValue;
        for (int i = 0; i < length; i++) {
            assertEquals(buf.get(startIndex + i), value);
            value = value + 1;
        }
    }
}
