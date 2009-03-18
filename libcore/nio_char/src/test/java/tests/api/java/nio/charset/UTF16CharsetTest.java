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
 * Test UTF-16.
 */

@TestTargetClass(java.nio.charset.Charset.class)
public class UTF16CharsetTest extends AbstractCharsetTestCase {

    /**
     * Constructor.
     */
    public UTF16CharsetTest() {
        super("UTF-16", new String[] { "UTF_16" }, true, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see tests.api.java.nio.charset.ConcreteCharsetTest#testEncode_Normal()
     */
    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Empty test.",
        method = "",
        args = {}
    )
    public void testEncode_Normal() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see tests.api.java.nio.charset.ConcreteCharsetTest#testDecode_Normal()
     */
    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Empty test.",
        method = "",
        args = {}
    )
    public void testDecode_Normal() {
        // TODO Auto-generated method stub

    }

}
