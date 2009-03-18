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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for java.nio package
 * 
 */
public class AllTests {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = tests.TestSuiteFactory.createTestSuite("Tests for java.nio");
        //$JUnit-BEGIN$
        suite.addTestSuite(BufferOverflowExceptionTest.class);
        suite.addTestSuite(BufferUnderflowExceptionTest.class);
        suite.addTestSuite(ByteOrderTest.class);
        suite.addTestSuite(DirectByteBufferTest.class);
        suite.addTestSuite(DirectCharBufferTest.class);
        suite.addTestSuite(DirectDoubleBufferTest.class);
        suite.addTestSuite(DirectFloatBufferTest.class);
        suite.addTestSuite(DirectIntBufferTest.class);
        suite.addTestSuite(DirectLongBufferTest.class);
        suite.addTestSuite(DirectShortBufferTest.class);
        suite.addTestSuite(DuplicateDirectByteBufferTest.class);
        suite.addTestSuite(DuplicateHeapByteBufferTest.class);
        suite.addTestSuite(DuplicateWrappedByteBufferTest.class);
        suite.addTestSuite(HeapByteBufferTest.class);
        suite.addTestSuite(HeapCharBufferTest.class);
        suite.addTestSuite(HeapDoubleBufferTest.class);
        suite.addTestSuite(HeapFloatBufferTest.class);
        suite.addTestSuite(HeapIntBufferTest.class);
        suite.addTestSuite(HeapLongBufferTest.class);
        suite.addTestSuite(HeapShortBufferTest.class);
        suite.addTestSuite(InvalidMarkExceptionTest.class);
        suite.addTestSuite(MappedByteBufferTest.class);
        suite.addTestSuite(ReadOnlyBufferExceptionTest.class);
        suite.addTestSuite(ReadOnlyCharBufferTest.class);
        suite.addTestSuite(ReadOnlyDirectByteBufferTest.class);
        suite.addTestSuite(ReadOnlyDoubleBufferTest.class);
        suite.addTestSuite(ReadOnlyFloatBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapByteBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapCharBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapDoubleBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapFloatBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapIntBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapLongBufferTest.class);
        suite.addTestSuite(ReadOnlyHeapShortBufferTest.class);
        suite.addTestSuite(ReadOnlyIntBufferTest.class);
        suite.addTestSuite(ReadOnlyLongBufferTest.class);
        suite.addTestSuite(ReadOnlyShortBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedByteBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedCharBufferTest1.class);
        suite.addTestSuite(ReadOnlyWrappedDoubleBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedFloatBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedIntBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedLongBufferTest.class);
        suite.addTestSuite(ReadOnlyWrappedShortBufferTest.class);
        suite.addTestSuite(SliceDirectByteBufferTest.class);
        suite.addTestSuite(SliceHeapByteBufferTest.class);
        suite.addTestSuite(SliceWrappedByteBufferTest.class);
        suite.addTestSuite(WrappedByteBufferTest.class);
        suite.addTestSuite(WrappedCharBufferTest1.class);
        suite.addTestSuite(WrappedCharBufferTest2.class);
        suite.addTestSuite(WrappedDoubleBufferTest.class);
        suite.addTestSuite(WrappedFloatBufferTest.class);
        suite.addTestSuite(WrappedIntBufferTest.class);
        suite.addTestSuite(WrappedLongBufferTest.class);
        suite.addTestSuite(WrappedShortBufferTest.class);
        //$JUnit-END$
        return suite;
    }
}
