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
import java.nio.ByteOrder;

@TestTargetClass(java.nio.CharBuffer.class)
public class DirectCharBufferTest extends CharBufferTest {
    
    protected void setUp(){
        capacity = BUFFER_LENGTH;
        buf = ByteBuffer.allocateDirect(BUFFER_LENGTH * 2).asCharBuffer();
        loadTestData1(buf);
        baseBuf = buf;
    }
    
    protected void tearDown(){
        buf = null;
        baseBuf = null;
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies hasArray method for direct CharBuffer.",
        method = "hasArray",
        args = {}
    )
    public void testHasArray() {
        assertFalse(buf.hasArray());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies array method for direct CharBuffer.",
        method = "array",
        args = {}
    )
    public void testArray() {
        try {
            buf.array();
            fail("Should throw UnsupportedOperationException"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies arrayOffset method for direct CharBuffer.",
        method = "arrayOffset",
        args = {}
    )
    public void testArrayOffset() {
        try {
            buf.arrayOffset();
            fail("Should throw UnsupportedOperationException"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies isDirect method for direct CharBuffer.",
        method = "isDirect",
        args = {}
    )
    public void testIsDirect() {
        assertTrue(buf.isDirect());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "order",
        args = {}
    )
    public void testOrder() {
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());
    }
}
