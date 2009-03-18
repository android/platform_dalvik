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

import java.nio.ByteBuffer;

@TestTargetClass(ByteBuffer.class)
public class DirectByteBufferTest extends ByteBufferTest {

    protected void setUp() throws Exception {
        capacity = BUFFER_LENGTH;
        buf = ByteBuffer.allocateDirect(BUFFER_LENGTH);
        loadTestData1(buf);
        baseBuf = buf;
    }

    protected void tearDown() throws Exception {
        buf = null;
        baseBuf = null;
    }

    /**
     * @tests java.nio.ByteBuffer#allocateDirect(int)
     * 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "allocateDirect",
        args = {int.class}
    )
    public void testAllocatedByteBuffer_IllegalArg() {
        try {
            ByteBuffer.allocateDirect(-1);
            fail("Should throw Exception"); //$NON-NLS-1$
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
        try {
            buf.array();
            fail("Should throw UnsupportedOperationException"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "arrayOffset",
        args = {}
    )
    public void testArrayOffset() {
        try {
            buf.arrayOffset();
            fail("Should throw UnsupportedOperationException"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies isDirect method for direct ByteBuffer.",
        method = "isDirect",
        args = {}
    )
    public void testIsDirect() {
        assertTrue(buf.isDirect());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies hasArray method for direct ByteBuffer.",
        method = "hasArray",
        args = {}
    )
    public void testHasArray() {
        assertFalse(buf.hasArray());
        try {
            buf.array();
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
