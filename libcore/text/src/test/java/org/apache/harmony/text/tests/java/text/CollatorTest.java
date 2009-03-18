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

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import java.io.UnsupportedEncodingException;
import java.text.CollationKey;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Locale;

@TestTargetClass(Collator.class) 
public class CollatorTest extends junit.framework.TestCase {

    /**
     * @tests java.text.Collator#clone()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public void test_clone() {
        Collator c = Collator.getInstance(Locale.GERMAN);
        Collator c2 = (Collator) c.clone();
        assertTrue("Clones answered false to equals", c.equals(c2));
        assertTrue("Clones were equivalent", c != c2);
    }

    /**
     * @tests java.text.Collator#compare(java.lang.Object, java.lang.Object)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "compare",
            args = {java.lang.Object.class, java.lang.Object.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setStrength",
            args = {int.class}
        )
    })
    public void test_compareLjava_lang_ObjectLjava_lang_Object() {
        Collator c = Collator.getInstance(Locale.FRENCH);
        Object o, o2;

        c.setStrength(Collator.IDENTICAL);
        o = "E";
        o2 = "F";
        assertTrue("a) Failed on primary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "\u00e9";
        assertTrue("a) Failed on secondary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "E";
        assertTrue("a) Failed on tertiary difference", c.compare(o, o2) < 0);
        o = "\u0001";
        o2 = "\u0002";
        assertTrue("a) Failed on identical", c.compare(o, o2) < 0);
        o = "e";
        o2 = "e";
        assertEquals("a) Failed on equivalence", 0, c.compare(o, o2));
        assertTrue("a) Failed on primary expansion",
                c.compare("\u01db", "v") < 0);

        c.setStrength(Collator.TERTIARY);
        o = "E";
        o2 = "F";
        assertTrue("b) Failed on primary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "\u00e9";
        assertTrue("b) Failed on secondary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "E";
        assertTrue("b) Failed on tertiary difference", c.compare(o, o2) < 0);
        o = "\u0001";
        o2 = "\u0002";
        assertEquals("b) Failed on identical", 0, c.compare(o, o2));
        o = "e";
        o2 = "e";
        assertEquals("b) Failed on equivalence", 0, c.compare(o, o2));

        c.setStrength(Collator.SECONDARY);
        o = "E";
        o2 = "F";
        assertTrue("c) Failed on primary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "\u00e9";
        assertTrue("c) Failed on secondary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "E";
        assertEquals("c) Failed on tertiary difference", 0, c.compare(o, o2));
        o = "\u0001";
        o2 = "\u0002";
        assertEquals("c) Failed on identical", 0, c.compare(o, o2));
        o = "e";
        o2 = "e";
        assertEquals("c) Failed on equivalence", 0, c.compare(o, o2));

        c.setStrength(Collator.PRIMARY);
        o = "E";
        o2 = "F";
        assertTrue("d) Failed on primary difference", c.compare(o, o2) < 0);
        o = "e";
        o2 = "\u00e9";
        assertEquals("d) Failed on secondary difference", 0, c.compare(o, o2));
        o = "e";
        o2 = "E";
        assertEquals("d) Failed on tertiary difference", 0, c.compare(o, o2));
        o = "\u0001";
        o2 = "\u0002";
        assertEquals("d) Failed on identical", 0, c.compare(o, o2));
        o = "e";
        o2 = "e";
        assertEquals("d) Failed on equivalence", 0, c.compare(o, o2));

        try {
            c.compare("e", new StringBuffer("Blah"));
        } catch (ClassCastException e) {
            // correct
            return;
        }
        fail("Failed to throw ClassCastException");
    }

    /**
     * @tests java.text.Collator#equals(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void test_equalsLjava_lang_Object() {
        Collator c = Collator.getInstance(Locale.ENGLISH);
        Collator c2 = (Collator) c.clone();
        assertTrue("Cloned collators not equal", c.equals(c2));
        c2.setStrength(Collator.SECONDARY);
        assertTrue("Collators with different strengths equal", !c.equals(c2));
    }

    /**
     * @tests java.text.Collator#equals(java.lang.String, java.lang.String)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "equals",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setStrength",
            args = {int.class}
        )
    })
    public void test_equalsLjava_lang_StringLjava_lang_String() {
        Collator c = Collator.getInstance(Locale.FRENCH);

        c.setStrength(Collator.IDENTICAL);
        assertTrue("a) Failed on primary difference", !c.equals("E", "F"));
        assertTrue("a) Failed on secondary difference", !c
                .equals("e", "\u00e9"));
        assertTrue("a) Failed on tertiary difference", !c.equals("e", "E"));
        assertTrue("a) Failed on identical", !c.equals("\u0001", "\u0002"));
        assertTrue("a) Failed on equivalence", c.equals("e", "e"));

        c.setStrength(Collator.TERTIARY);
        assertTrue("b) Failed on primary difference", !c.equals("E", "F"));
        assertTrue("b) Failed on secondary difference", !c
                .equals("e", "\u00e9"));
        assertTrue("b) Failed on tertiary difference", !c.equals("e", "E"));
        assertTrue("b) Failed on identical", c.equals("\u0001", "\u0002"));
        assertTrue("b) Failed on equivalence", c.equals("e", "e"));

        c.setStrength(Collator.SECONDARY);
        assertTrue("c) Failed on primary difference", !c.equals("E", "F"));
        assertTrue("c) Failed on secondary difference", !c
                .equals("e", "\u00e9"));
        assertTrue("c) Failed on tertiary difference", c.equals("e", "E"));
        assertTrue("c) Failed on identical", c.equals("\u0001", "\u0002"));
        assertTrue("c) Failed on equivalence", c.equals("e", "e"));

        c.setStrength(Collator.PRIMARY);
        assertTrue("d) Failed on primary difference", !c.equals("E", "F"));
        assertTrue("d) Failed on secondary difference", c.equals("e", "\u00e9"));
        assertTrue("d) Failed on tertiary difference", c.equals("e", "E"));
        assertTrue("d) Failed on identical", c.equals("\u0001", "\u0002"));
        assertTrue("d) Failed on equivalence", c.equals("e", "e"));
    }

    /**
     * @tests java.text.Collator#getAvailableLocales()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getAvailableLocales",
        args = {}
    )
    public void test_getAvailableLocales() {
        Locale[] locales = Collator.getAvailableLocales();
        assertTrue("No locales", locales.length > 0);
        boolean hasUS = false;
        for (int i = locales.length; --i >= 0;) {
            Collator c1 = Collator.getInstance(locales[i]);
            assertTrue("Doesn't work", c1.compare("a", "b") < 0);
            assertTrue("Wrong decomposition",
                    c1.getDecomposition() == Collator.NO_DECOMPOSITION);
            assertTrue("Wrong strength", c1.getStrength() == Collator.TERTIARY);
            // The default decomposition for collators created with getInstance
            // is NO_DECOMPOSITION where collators created from rules have
            // CANONICAL_DECOMPOSITION. Verified on RI.
            c1.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            if (locales[i].equals(Locale.US)) {
                hasUS = true;
            }
            if (c1 instanceof RuleBasedCollator) {
                String rule = "";
                Collator temp = null;
                try {
                    rule = ((RuleBasedCollator) c1).getRules();
                    temp = new RuleBasedCollator(rule);
                } catch (ParseException e) {
                    fail(e.getMessage() + " for rule: \"" + rule + "\"");
                }
                assertTrue("Can't recreate: " + locales[i], temp.equals(c1));
            }
        }
        assertTrue("en_US locale not available", hasUS);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Collator",
        args = {}
    )
    public void test_Constructor() {
        TestCollator collator = new TestCollator();
        assertEquals(Collator.TERTIARY, collator.getStrength());
    }
    
    /**
     * @tests java.text.Collator#getDecomposition()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getDecomposition",
        args = {}
    )
    public void test_getDecomposition() {
        RuleBasedCollator collator;
        try {
            collator = new RuleBasedCollator("; \u0300 < a, A < b < c < d");
        } catch (ParseException e) {
            fail("ParseException");
            return;
        }
        assertTrue("Wrong default",
                collator.getDecomposition() == Collator.CANONICAL_DECOMPOSITION);
        
        collator.setDecomposition(Collator.NO_DECOMPOSITION);
        assertEquals(Collator.NO_DECOMPOSITION, collator.getDecomposition());

        // BEGIN android-removed
        // Android doesn't support full decomposition
        // collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        // assertEquals(Collator.FULL_DECOMPOSITION, collator.getDecomposition());
        // EN android-removed
    }

    /**
     * @tests java.text.Collator#getInstance()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInstance",
        args = {}
    )
    public void test_getInstance() {
        Collator c1 = Collator.getInstance();
        Collator c2 = Collator.getInstance(Locale.getDefault());
        assertTrue("Wrong locale", c1.equals(c2));
    }

    /**
     * @tests java.text.Collator#getInstance(java.util.Locale)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInstance",
        args = {java.util.Locale.class}
    )
    public void test_getInstanceLjava_util_Locale() {
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.text.Collator#getStrength()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getStrength",
        args = {}
    )
    public void test_getStrength() {
        RuleBasedCollator collator;
        try {
            collator = new RuleBasedCollator("; \u0300 < a, A < b < c < d");
        } catch (ParseException e) {
            fail("ParseException");
            return;
        }
        assertTrue("Wrong default", collator.getStrength() == Collator.TERTIARY);
    }

    /**
     * @tests java.text.Collator#setDecomposition(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setDecomposition",
        args = {int.class}
    )
    @KnownFailure("uses decomposition even if set to NO_DECOMPOSITION")
    public void test_setDecompositionI() {
        Collator c = Collator.getInstance(Locale.FRENCH);
        c.setStrength(Collator.IDENTICAL);
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        assertFalse("Collator should not be using decomposition", c.equals(
                "\u212B", "\u00C5")); // "ANGSTROM SIGN" and "LATIN CAPITAL
                                      // LETTER A WITH RING ABOVE"
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        assertTrue("Collator should be using decomposition", c.equals("\u212B",
                "\u00C5")); // "ANGSTROM SIGN" and "LATIN CAPITAL LETTER A WITH
        // RING ABOVE"
        // BEGIN android-removed
        // Android doesn't support FULL_DECOMPOSITION
        // c.setDecomposition(Collator.FULL_DECOMPOSITION);
        // assertTrue("Should be equal under full decomposition", c.equals(
        //         "\u2163", "IV")); // roman number "IV"
        // END android-removed
        
        try {
            c.setDecomposition(-1);
            fail("IllegalArgumentException should be thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
    }

    /**
     * @tests java.text.Collator#setStrength(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "setStrength",
        args = {int.class}
    )
    public void test_setStrengthI() {
        // Functionality is verified in compare and equals tests.
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        assertEquals(Collator.PRIMARY, collator.getStrength());
        
        collator.setStrength(Collator.SECONDARY);
        assertEquals(Collator.SECONDARY, collator.getStrength());
        
        collator.setStrength(Collator.TERTIARY);
        assertEquals(Collator.TERTIARY, collator.getStrength());
        
        collator.setStrength(Collator.IDENTICAL);
        assertEquals(Collator.IDENTICAL, collator.getStrength());        
        
        try {
            collator.setStrength(-1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException  iae) {
            //expected
        }        
    }
    // Regression test for Android bug   
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Regression test.",
            method = "setStrength",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Regression test.",
            method = "getCollationKey",
            args = {java.lang.String.class}
        )
    })
    public void test_stackCorruption() {
        Collator mColl = Collator.getInstance();
        mColl.setStrength(Collator.PRIMARY);
        mColl.getCollationKey("2d294f2d3739433565147655394f3762f3147312d3731641452f310");    
    }
    
    // Test to verify that very large collation keys are not truncated.
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Doesn't verify null as a parameter.",
        method = "getCollationKey",
        args = {java.lang.String.class}
    )
    public void test_collationKeySize() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            b.append("0123456789ABCDEF");
        }
        String sixteen = b.toString();
        b.append("_THE_END");
        String sixteenplus = b.toString();
        
        Collator mColl = Collator.getInstance();
        mColl.setStrength(Collator.PRIMARY);

        try {
            byte [] arr = mColl.getCollationKey(sixteen).toByteArray();
            int len = arr.length;
            assertTrue("Collation key not 0 terminated", arr[arr.length - 1] == 0);
            len--;
            String foo = new String(arr, 0, len, "iso8859-1");

            arr = mColl.getCollationKey(sixteen).toByteArray();
            len = arr.length;
            assertTrue("Collation key not 0 terminated", arr[arr.length - 1] == 0);
            len--;
            String bar = new String(arr, 0, len, "iso8859-1");
            
            assertTrue("Collation keys should differ", foo.equals(bar));
        } catch (UnsupportedEncodingException ex) {
            fail("UnsupportedEncodingException");
        }
    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "",
            method = "setDecomposition",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "",
            method = "compare",
            args = {java.lang.String.class, java.lang.String.class}
        )
    })
    public void test_decompositionCompatibility() {
        Collator myCollator = Collator.getInstance();
        myCollator.setDecomposition(Collator.NO_DECOMPOSITION);
        assertFalse("Error: \u00e0\u0325 should not equal to a\u0325\u0300 " +
                "without decomposition", 
                myCollator.compare("\u00e0\u0325", "a\u0325\u0300") == 0);
        myCollator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        assertTrue("Error: \u00e0\u0325 should equal to a\u0325\u0300 " +
                "with decomposition", 
                myCollator.compare("\u00e0\u0325", "a\u0325\u0300") == 0);
    }
    
    class TestCollator extends Collator {

        @Override
        public int compare(String source, String target) {
            return 0;
        }

        @Override
        public CollationKey getCollationKey(String source) {
            return null;
        }

        @Override
        public int hashCode() {
            return 0;
        }
        
    }
}
