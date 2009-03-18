/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.harmony.text.tests.java.text;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.text.AttributedString;
import java.text.Bidi;
import java.util.Arrays;

@TestTargetClass(Bidi.class) 
public class BidiTest extends TestCase {

    Bidi bd;

    public static void assertRunArrayEquals(int[][] expected, Bidi bidi) {
        assertEquals("different length", expected.length, bidi.getRunCount());

        FORRUN: for (int i = 0; i < bidi.getRunCount(); i++) {
            int[] butWas = new int[] { bidi.getRunStart(i),
                    bidi.getRunLimit(i), bidi.getRunLevel(i) };

            for (int j = 0; j < expected.length; j++) {
                if (expected[j][0] == butWas[0] && expected[j][1] == butWas[1]
                        && expected[j][2] == butWas[2]) {
                    continue FORRUN;
                }
            }
            fail("expected [" + i + "] " + " start: " + butWas[0] + " limit: "
                    + butWas[1] + " level: " + butWas[2]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify positive case for Bidi(AttributedCharacterIterator paragraph).",
            method = "Bidi",
            args = {char[].class, int.class, byte[].class, int.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify positive case for Bidi(AttributedCharacterIterator paragraph).",
            method = "Bidi",
            args = {java.text.AttributedCharacterIterator.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testNullPointerConstructor() {
        try {
            bd = new Bidi(null, Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(null, 0, new byte[] { 0 }, 0, 0,
                    Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(null);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
        }

        bd = new Bidi("a".toCharArray(), 0, null, 0, 1,
                Bidi.DIRECTION_RIGHT_TO_LEFT);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "Bidi",
        args = {char[].class, int.class, byte[].class, int.class, int.class, int.class}
    )
    public void testBadLength() {
        try {
            bd = new Bidi("1".toCharArray(), 0, new byte[] { 0 }, 0, 20,
                    Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi("1234567".toCharArray(), 0, new byte[] { 0 }, 0, 4,
                    Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi("1234567".toCharArray(), 4, new byte[] { 0, 1, 2, 3,
                    4 }, 0, 5, Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi("1234567".toCharArray(), 0, new byte[] { 0, 1, 2, 3,
                    4 }, 4, 5, Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // regression for HARMONY-1031
        try {
            bd = new Bidi(new char[] { 't', 't', 't' }, -1,
                    new byte[] { 2, 2 }, 1, 1, 1);
            fail("should be IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(new char[] { 't', 't', 't' }, 1, new byte[] { 2, 2 },
                    -1, 1, 1);
            fail("should be IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(new char[] { 't', 't', 't' }, 1, new byte[] { 2, 2 },
                    1, -1, 1);
            fail("should be IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(new char[] {}, 5, new byte[] { 2, 2, 2, 2, 2, 2 }, 8,
                    Integer.MAX_VALUE, 5);
            fail("should be IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            bd = new Bidi(null, 5, null, 8, Integer.MAX_VALUE, 5);
            fail("should be IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        bd = new Bidi(new char[] { 'o' }, 0, new byte[] { 2, 2 }, 2, 0, 2);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testEmptyParagraph() {
        bd = new Bidi("", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(0, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 0, 0 } }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(0, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 0, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());

        bd = new Bidi("", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(0, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 0, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(0, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 0, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    public void testSpaceParagraph() {
        bd = new Bidi(" ", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(" ", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());

        bd = new Bidi(" ", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(" ", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    public void testSimpleParagraph() {
        bd = new Bidi("t", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("t", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("t", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    /**
     * @tests java.text.Bidi#toString() Test of method java.text.Bidi#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void testToString() {
        try {
            bd = new Bidi("bidi", 173);
            assertNotNull("Bidi representation is null", bd.toString());
        } catch (Exception e) {
            fail("Unexpected exception " + e.toString());
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify that these methods can return all possible flags.",
            method = "Bidi",
            args = {java.lang.String.class, int.class}
        )
    })
    public void testBadFlags() {
        bd = new Bidi("", 173);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(0, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 0, 0 }, }, bd);
        assertTrue(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "Bidi",
        args = {char[].class, int.class, byte[].class, int.class, int.class, int.class}
    )
    public void testBadEmbeddings() {
        try {
            bd = new Bidi("".toCharArray(), 0, new byte[] {}, 0, 1,
                    Bidi.DIRECTION_RIGHT_TO_LEFT);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    public void testOverrideEmbeddings() {
        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) -7,
                (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(7, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 7 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) -1,
                (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) -1,
                (byte) -2, (byte) -3 }, 0, 3, Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) -1,
                (byte) -2, (byte) -3 }, 0, 3, Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    public void testDefaultEmbeddings() {
        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) 0,
                (byte) 0, (byte) 0 }, 0, 3, Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(2, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 3, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    public void testRelativeEmbeddings() {
        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) 1,
                (byte) 2, (byte) 3 }, 0, 3, Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(4, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 2, 2 }, { 2, 3, 4 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testSimpleHebrewParagraph() {
        bd = new Bidi("\u05D0", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());

        bd = new Bidi("\u05D0", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());

        bd = new Bidi("\u05D0", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertFalse(bd.isMixed());
        assertTrue(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testSimpleBidiParagraph_1() {
        bd = new Bidi("\u05D0a", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("\u05D0a", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("\u05D0a", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 0 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("\u05D0a", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testSimpleBidiParagraph_2() {
        bd = new Bidi("a\u05D0", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, { 1, 2, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("a\u05D0", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, { 1, 2, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("a\u05D0", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(0, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 0 }, { 1, 2, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi("a\u05D0", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(2, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(2, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 2 }, { 1, 2, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    /*
     * spec reads: public static final int DIRECTION_RIGHT_TO_LEFT Constant
     * indicating base direction is right-to-left. according to that, the method
     * baseIsLeftToRight() here should return false. however, RI doesn't act so.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testRIBug_1() {
        bd = new Bidi("t", Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        // the base level it the essential cause
        assertEquals(1, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    // this is essentially the same bug as Bug_1
    public void testRIBug_2() {
        bd = new Bidi("\u05D0", Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(1, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(1, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {String.class, int.class}
        )
    })
    public void testComplicatedBidi() {
        bd = new Bidi("a\u05D0a\"a\u05D0\"\u05D0a",
                Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(9, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(2, bd.getLevelAt(2));
        assertEquals(2, bd.getLevelAt(3));
        assertEquals(2, bd.getLevelAt(4));
        assertEquals(1, bd.getLevelAt(5));
        assertEquals(1, bd.getLevelAt(6));
        assertEquals(1, bd.getLevelAt(7));
        assertEquals(2, bd.getLevelAt(8));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(5, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 2 }, { 1, 2, 1 },
                { 2, 5, 2 }, { 5, 8, 1 }, { 8, 9, 2 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {char[].class, int.class, byte[].class, int.class, int.class, int.class}
        )
    })
    public void testComplicatedOverrideBidi() {
        bd = new Bidi("a\u05D0a\"a\u05D0\"\u05D0a".toCharArray(), 0,
                new byte[] { 0, 0, 0, -3, -3, 2, 2, 0, 3 }, 0, 9,
                Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(9, bd.getLength());
        assertEquals(2, bd.getLevelAt(0));
        assertEquals(1, bd.getLevelAt(1));
        assertEquals(2, bd.getLevelAt(2));
        assertEquals(3, bd.getLevelAt(3));
        assertEquals(3, bd.getLevelAt(4));
        assertEquals(3, bd.getLevelAt(5));
        assertEquals(2, bd.getLevelAt(6));
        assertEquals(1, bd.getLevelAt(7));
        assertEquals(4, bd.getLevelAt(8));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(7, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 2 }, { 1, 2, 1 },
                { 2, 3, 2 }, { 3, 6, 3 }, { 6, 7, 2 }, { 7, 8, 1 },
                { 8, 9, 4 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "requiresBidi",
        args = {char[].class, int.class, int.class}
    )
    public void testRequiresBidi() {
        try {
            Bidi.requiresBidi(null, 0, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            assertFalse(Bidi.requiresBidi(null, 0, 1));
            fail("should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("".toCharArray(), 0, 1));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(), -1, 1));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 1, -1));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("\u05D0".toCharArray(), 1, -1));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 1, 0));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 7, 7));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 1,
                    Integer.MAX_VALUE));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(Bidi.requiresBidi("aaa".toCharArray(),
                    Integer.MAX_VALUE, 1));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertFalse(Bidi.requiresBidi("".toCharArray(), 0, 0));
        assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 1, 1));
        assertFalse(Bidi.requiresBidi("aaa".toCharArray(), 0, 2));
        assertFalse(Bidi.requiresBidi("\u05D0".toCharArray(), 1, 1));
        assertTrue(Bidi.requiresBidi("\u05D0".toCharArray(), 0, 1));
        assertFalse(Bidi.requiresBidi("aa\u05D0a".toCharArray(), 0, 2));
        assertTrue(Bidi.requiresBidi("aa\u05D0a".toCharArray(), 1, 3));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "Bidi",
            args = {char[].class, int.class, byte[].class, int.class, int.class, int.class}
        )
    })
    public void testHebrewOverrideEmbeddings() {
        bd = new Bidi(new char[] { '\u05D0', '\u05D0', '\u05D0' }, 0,
                new byte[] { (byte) -1, (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { '\u05D0', '\u05D0', '\u05D0' }, 0,
                new byte[] { (byte) -1, (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { '\u05D0', '\u05D0', '\u05D0' }, 0,
                new byte[] { (byte) -1, (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_LEFT_TO_RIGHT);
        assertTrue(bd.baseIsLeftToRight());
        assertEquals(0, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(0, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());

        bd = new Bidi(new char[] { '\u05D0', '\u05D0', '\u05D0' }, 0,
                new byte[] { (byte) -1, (byte) -2, (byte) -3 }, 0, 3,
                Bidi.DIRECTION_RIGHT_TO_LEFT);
        assertFalse(bd.baseIsLeftToRight());
        assertEquals(1, bd.getBaseLevel());
        assertEquals(3, bd.getLength());
        assertEquals(1, bd.getLevelAt(0));
        assertEquals(2, bd.getLevelAt(1));
        assertEquals(3, bd.getLevelAt(2));
        assertEquals(1, bd.getLevelAt(1000));
        assertEquals(3, bd.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 1 }, { 1, 2, 2 },
                { 2, 3, 3 }, }, bd);
        assertFalse(bd.isLeftToRight());
        assertTrue(bd.isMixed());
        assertFalse(bd.isRightToLeft());
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "createLineBidi",
            args = {int.class, int.class}
        )
    })
    public void testCreateLineBidi() {
        bd = new Bidi("a\u05D0a\na\u05D0\"\u05D0a".toCharArray(), 0,
                new byte[] { 0, 0, 0, -3, -3, 2, 2, 0, 3 }, 0, 9,
                Bidi.DIRECTION_RIGHT_TO_LEFT);
        Bidi line = bd.createLineBidi(2, 7);
        assertFalse(line.baseIsLeftToRight());
        assertEquals(1, line.getBaseLevel());
        assertEquals(5, line.getLength());
        assertEquals(2, line.getLevelAt(0));
        assertEquals(1, line.getLevelAt(1));
        assertEquals(3, line.getLevelAt(2));
        assertEquals(3, line.getLevelAt(3));
        assertEquals(2, line.getLevelAt(4));
        assertEquals(1, line.getLevelAt(1000));
        assertEquals(4, line.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 1, 2 }, { 1, 2, 1 },
                { 2, 4, 3 }, { 4, 5, 2 }, }, line);
        assertFalse(line.isLeftToRight());
        assertTrue(line.isMixed());
        assertFalse(line.isRightToLeft());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "createLineBidi",
        args = {int.class, int.class}
    )
    public void testCreateLineBidiInvalid() {
        // regression for HARMONY-1050
        Bidi bidi = new Bidi("str", Bidi.DIRECTION_RIGHT_TO_LEFT);
        try {
            bidi.createLineBidi(-1, 1);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            bidi.createLineBidi(1, -1);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            bidi.createLineBidi(-1, -1);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            bidi.createLineBidi(2, 1);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // Expected
        }

// Outsourced to _AndroidFailure:
//        bidi.createLineBidi(2, 2);

        try {
            bidi.createLineBidi(2, 4);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "createLineBidi",
        args = {int.class, int.class}
    )
    @KnownFailure("Is this a failure or just a different behaviour of ICU?")
    public void testCreateLineBidi_AndroidFailure() {
        Bidi bidi = new Bidi("str", Bidi.DIRECTION_RIGHT_TO_LEFT);
        bidi.createLineBidi(2, 2);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "baseIsLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevelAt",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getRunCount",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isRightToLeft",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isLeftToRight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "isMixed",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getBaseLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLength",
            args = {}
        )
    })
    @AndroidOnly("ICU treats new lines different from RI.")
    public void testIncompatibleLineAlgorithm() {
        // ICU treat a new line as in the same run, however RI does not
        bd = new Bidi("aaaaa".toCharArray(), 0,
                new byte[] { -2, -1, -3, -3, -2 }, 0, 5,
                Bidi.DIRECTION_RIGHT_TO_LEFT);
        Bidi line = bd.createLineBidi(1, 4);
        assertFalse(line.baseIsLeftToRight());
        assertEquals(1, line.getBaseLevel());
        assertEquals(3, line.getLength());
        assertEquals(1, line.getLevelAt(0));
        assertEquals(1, line.getLevelAt(1));
        assertEquals(1, line.getLevelAt(2));
        assertEquals(1, line.getLevelAt(1000));
        assertEquals(1, line.getRunCount());
        assertRunArrayEquals(new int[][] { { 0, 3, 1 }, }, line);
        assertFalse(line.isLeftToRight());
        assertFalse(line.isMixed());
        assertTrue(line.isRightToLeft());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "reorderVisually",
        args = {byte[].class, int.class, java.lang.Object[].class, int.class, int.class}
    )
    public void testReorderVisually() {
        String[] init = new String[] { "a", "b", "c", "d" };
        String[] s = new String[4];

        System.arraycopy(init, 0, s, 0, s.length);
        Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 0, s, 0, 4);
        assertEquals("[c, b, a, d]", Arrays.asList(s).toString());

        System.arraycopy(init, 0, s, 0, s.length);
        Bidi.reorderVisually(new byte[] { 1, 3 }, 0, s, 1, 2);
        assertEquals("[a, c, b, d]", Arrays.asList(s).toString());

        System.arraycopy(init, 0, s, 0, s.length);
        Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 1, s, 1, 2);
        assertEquals("[a, c, b, d]", Arrays.asList(s).toString());

        System.arraycopy(init, 0, s, 0, s.length);
        Bidi.reorderVisually(new byte[] { 2, 1, 2, 1 }, 1, s, 0, 3);
        assertEquals("[c, b, a, d]", Arrays.asList(s).toString());

        System.arraycopy(init, 0, s, 0, s.length);
        Bidi.reorderVisually(new byte[] { 2, 1, 0, 1 }, 1, s, 0, 3);
        assertEquals("[a, b, c, d]", Arrays.asList(s).toString());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies exceptions.",
        method = "reorderVisually",
        args = {byte[].class, int.class, java.lang.Object[].class, int.class, int.class}
    )
    public void testBadReorderVisually() {
        String[] s = new String[] { "a", "b", "c", "d" };

        try {
            Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 0, s, 0, 5);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 0, s, -1, 1);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, -1, s, 0, 1);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Bidi.reorderVisually(null, 0, s, 0, 1);
            fail("should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 0, null, 0, 1);
            fail("should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Bidi.reorderVisually(new byte[] { 2, 1, 3, 0 }, 1, s, 0, -1);
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Regression test.",
            method = "getRunLimit",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Regression test.",
            method = "getRunStart",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Regression test.",
            method = "getRunCount",
            args = {}
        )
    })
    public void testGetRuns() {
        // Regression test for Harmony-1028

        String LTR = "\u0061\u0062";
        String RTL = "\u05DC\u05DD";
        String newLine = "\n";
        String defText = LTR + newLine + RTL + LTR + RTL;

        int[][] expectedRuns = { { 0, 3 }, { 3, 5 }, { 5, 7 }, { 7, 9 }, };

        Bidi bi = new Bidi(defText, 0);
        final int count = bi.getRunCount();
        for (int i = 0; i < count; i++) {
            assertEquals(expectedRuns[i][0], bi.getRunStart(i));
            assertEquals(expectedRuns[i][1], bi.getRunLimit(i));
        }
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify any int value between 0 and getRunCount().",
        method = "getRunLimit",
        args = {int.class}
    )
    public void testGetRunLimit() {
        bd = new Bidi("text", Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            assertTrue(4 == bd.getRunLimit(-1));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getRunLevel",
        args = {int.class}
    )
    public void testGetRunLevelLInt() {
        bd = new Bidi("text", Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            assertEquals(0, bd.getRunLevel(0));
            assertEquals(0, bd.getRunLevel(bd.getRunCount()));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }        
        
        bd = new Bidi("text", Bidi.DIRECTION_RIGHT_TO_LEFT);
        try {
            assertEquals(2, bd.getRunLevel(0));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }     
        
        bd = new Bidi("text", Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        try {
            assertEquals(0, bd.getRunLevel(0));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }          
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getRunStart",
        args = {int.class}
    )
    public void testGetRunStart() {
        bd = new Bidi(new char[] { 's', 's', 's' }, 0, new byte[] { (byte) -7,
                (byte) -2, (byte) 3 }, 0, 3,
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        assertEquals(0, bd.getRunStart(0));
        assertEquals(1, bd.getRunStart(1));
        assertEquals(2, bd.getRunStart(2));
        
        String LTR = "\u0061\u0062";
        String RTL = "\u05DC\u05DD";
        String newLine = "\n";
        String defText = LTR + newLine + RTL + LTR + RTL;

        int[][] expectedRuns = { { 0, 3 }, { 3, 5 }, { 5, 7 }, { 7, 9 }, };

        Bidi bi = new Bidi(defText, 0);

        final int count = bi.getRunCount();
        for (int i = 0; i < count; i++) {
            assertEquals(expectedRuns[i][0], bi.getRunStart(i));
        }        
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "Bidi",
        args = {java.text.AttributedCharacterIterator.class}
    )
    public void testBidiConstructor_Iterator() {
        AttributedString paragraph = new AttributedString("text");
        bd = new Bidi(paragraph.getIterator());
        try {
            assertTrue(4 == bd.getRunLimit(1));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
}
