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

package tests.api.java.nio.charset;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;

/**
 * Test UTF-16BE.
 */

@TestTargetClass(java.nio.charset.Charset.class)
public class UTF16BECharsetTest extends AbstractCharsetTestCase {

    /**
     * Constructor.
     */
    public UTF16BECharsetTest() {
        super("UTF-16BE", new String[] { "X-UTF-16BE", "UTF_16BE" },
                true, true); // "ISO-10646-UCS-2"
    }

    /*
     * (non-Javadoc)
     * 
     * @see tests.api.java.nio.charset.ConcreteCharsetTest#testEncode_Normal()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Functional test, text source: AbstractCharsetTestCase.internalTestEncode. Exceptions checking missed.",
        method = "encode",
        args = {java.lang.String.class}
    )
    public void testEncode_Normal() {
        String input = "ab\u5D14\u654F";
        byte[] output = new byte[] { 0, 97, 0, 98, 93, 20, 101, 79 };
        internalTestEncode(input, output);
    }

    /*
     * (non-Javadoc)
     * 
     * @see tests.api.java.nio.charset.ConcreteCharsetTest#testDecode_Normal()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Functional test, text source: AbstractCharsetTestCase.internalTestDecode. Exceptions checking missed.",
        method = "decode",
        args = {java.nio.ByteBuffer.class}
    )
    public void testDecode_Normal() {
        byte[] input = new byte[] { 0, 97, 0, 98, 93, 20, 101, 79 };
        char[] output = "ab\u5D14\u654F".toCharArray();
        internalTestDecode(input, output);
    }
}
