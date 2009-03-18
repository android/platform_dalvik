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

package org.apache.harmony.luni.tests.java.lang;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

@TestTargetClass(Character.class) 
public class CharacterImplTest extends TestCase {

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "valueOf",
        args = {char.class}
    )
    @AndroidOnly("valueOf doesn't return the same values on RI.")
    public void test_valueOfC() {
        // test the cache range
        for (char c = '\u0000'; c < 512; c++) {
            Character e = new Character(c);
            Character a = Character.valueOf(c);
            assertEquals(e, a);

            // WARN: this assertion may not be valid on other JREs
            assertSame(Character.valueOf(c), Character.valueOf(c));
        }
        // test the rest of the chars
        for (int c = '\u0512'; c <= Character.MAX_VALUE; c++) {
            assertEquals(new Character((char) c), Character.valueOf((char) c));
        }
    }
}
