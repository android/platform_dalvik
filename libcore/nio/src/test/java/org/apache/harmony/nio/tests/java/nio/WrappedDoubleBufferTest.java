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

import java.nio.DoubleBuffer;

@TestTargetClass(java.nio.DoubleBuffer.class)
public class WrappedDoubleBufferTest extends DoubleBufferTest {
    protected void setUp() throws Exception {
        capacity = BUFFER_LENGTH;
        buf = DoubleBuffer.wrap(new double[BUFFER_LENGTH]);
        loadTestData1(buf);
        baseBuf = buf;
    }

    protected void tearDown() throws Exception {
        buf = null;
        baseBuf = null;
    }

    /**
     * @tests java.nio.CharBuffer#allocate(char[],int,int)
     * 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "wrap",
        args = {double[].class, int.class, int.class}
    )
    public void testWrappedDoubleuffer_IllegalArg() {
        double array[] = new double[20];
        try {
            DoubleBuffer.wrap(array, -1, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap(array, 21, 0);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap(array, 0, -1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap(array, 0, 21);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap(array, Integer.MAX_VALUE, 1);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap(array, 1, Integer.MAX_VALUE);
            fail("Should throw Exception"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
        try {
            DoubleBuffer.wrap((double[])null, -1, 0);
            fail("Should throw NPE"); //$NON-NLS-1$
        } catch (NullPointerException e) {
        }

        DoubleBuffer buf = DoubleBuffer.wrap(array, 2, 16);
        assertEquals(buf.position(), 2);
        assertEquals(buf.limit(), 18);
        assertEquals(buf.capacity(), 20);
    }
}
