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
package org.apache.harmony.nio.tests.java.nio;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;

@TestTargetClass(java.nio.CharBuffer.class)
public class ReadOnlyCharBufferTest extends CharBufferTest {

    protected void setUp() throws Exception {
        super.setUp();
        buf = buf.asReadOnlyBuffer();
        baseBuf = buf;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that isReadOnly returns true for read only CharBuffer.",
        method = "isReadOnly",
        args = {}
    )
    public void testIsReadOnly() {
        assertTrue(buf.isReadOnly());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that hasArray returns false for read only CharBuffer.",
        method = "hasArray",
        args = {}
    )
    public void testHasArray() {
        assertFalse(buf.hasArray());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "array",
        args = {}
    )
    public void testArray() {
        try {
            buf.array();
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() {
        CharBuffer duplicate = buf.duplicate();
        assertEquals(buf.hashCode(), duplicate.hashCode());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "arrayOffset",
        args = {}
    )
    public void testArrayOffset() {
        try {
            buf.arrayOffset();
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "compact",
        args = {}
    )
    public void testCompact() {
        try {
            buf.compact();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {char.class}
    )
    public void testPutchar() {
        try {
            buf.put((char) 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException and NullPointerException.",
        method = "put",
        args = {char[].class}
    )
    public void testPutcharArray() {
        char array[] = new char[1];
        try {
            buf.put(array);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put((char[]) null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {char[].class, int.class, int.class}
    )
    public void testPutcharArrayintint() {
        char array[] = new char[1];
        try {
            buf.put(array, 0, array.length);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put((char[]) null, 0, 1);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put(new char[buf.capacity() + 1], 0, buf.capacity() + 1);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put(array, -1, array.length);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {java.nio.CharBuffer.class}
    )
    public void testPutCharBuffer() {
        CharBuffer other = CharBuffer.allocate(1);
        try {
            buf.put(other);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put((CharBuffer) null);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put(buf);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {int.class, char.class}
    )
    public void testPutintchar() {
        try {
            buf.put(0, (char) 0);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put(-1, (char) 0);
            fail("Should throw ReadOnlyBufferException"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void testPutStringintint() {
        buf.clear();
        String str = String.valueOf(new char[buf.capacity()]);
        try {
            buf.put(str, 0, str.length());
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put((String) null, 0, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        try {
            buf.put(str, -1, str.length());
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        String longStr = String.valueOf(new char[buf.capacity()+1]);
        try {
            buf.put(longStr, 0, longStr.length());
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ReadOnlyBufferException.",
        method = "put",
        args = {java.lang.String.class}
    )
    public void testPutString() {
        String str = " ";
        try {
            buf.put(str);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (ReadOnlyBufferException e) {
            // expected
        }
        try {
            buf.put((String)null);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }
}
