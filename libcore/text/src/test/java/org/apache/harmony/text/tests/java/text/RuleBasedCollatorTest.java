/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.text.tests.java.text;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import java.text.CharacterIterator;
import java.text.CollationElementIterator;
import java.text.CollationKey;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.text.StringCharacterIterator;
import java.util.Locale;

@TestTargetClass(RuleBasedCollator.class) 
public class RuleBasedCollatorTest extends TestCase {

    /**
     * @tests java.text.RuleBasedCollator#RuleBasedCollator(String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "RuleBasedCollator",
        args = {java.lang.String.class}
    )
    public void test_constrLRuleBasedCollatorLjava_lang_String() {
        RuleBasedCollator rbc;
        try {
            rbc = new RuleBasedCollator("<a< b< c< d");
            assertNotSame("RuleBasedCollator object is null", null, rbc);
        } catch (java.text.ParseException pe) {
            fail("java.text.ParseException is thrown for correct string");
        }

        try {
            rbc = new RuleBasedCollator("<a< '&'b< \u0301< d");
            assertNotSame("RuleBasedCollator object is null", null, rbc);
        } catch (java.text.ParseException pe) {
            fail("java.text.ParseException is thrown for correct string");
        }

        try {
            new RuleBasedCollator(null);
            fail("No Exception is thrown for correct string");
        } catch (java.text.ParseException pe) {
            fail("java.lang.NullPointerException is not thrown for correct string");
        } catch (java.lang.NullPointerException npe) {

        }

// Commented since fails agains RI and Android, too:
//
//        // Android allows to pass empty rules to a collator. It results in
//        // a collator with default UCA rules.
//        try {
//            new RuleBasedCollator("");
//        } catch (java.text.ParseException pe) {
//            fail("java.text.ParseException is thrown for empty string");
//        }

        try {
            new RuleBasedCollator("1234567%$#845");
            fail("java.text.ParseException is not thrown for wrong rules");
        } catch (java.text.ParseException pe) {
        }
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Regression test. Doesn't verify positive functionality.",
        method = "getCollationKey",
        args = {java.lang.String.class}
    )
    public void test_getCollationKeyLjava_lang_String() {
        // Regression test for HARMONY-28
        String source = null;
        RuleBasedCollator rbc = null;
        try {
            String Simple = "< a< b< c< d";
            rbc = new RuleBasedCollator(Simple);
        } catch (ParseException e) {
            fail("Assert 0: Unexpected format exception " + e);
        }
        CollationKey ck = rbc.getCollationKey(source);
        assertNull("Assert 1: getCollationKey (null) does not return null", ck);
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() throws ParseException {
        {
            String rule = "< a < b < c < d";
            RuleBasedCollator coll = new RuleBasedCollator(rule);
            assertEquals(rule.hashCode(), coll.hashCode());
        }

        {
            String rule = "< a < b < c < d < e";
            RuleBasedCollator coll = new RuleBasedCollator(rule);
            assertEquals(rule.hashCode(), coll.hashCode());
        }

    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public void testClone() throws ParseException {
        RuleBasedCollator coll = (RuleBasedCollator) Collator
                .getInstance(Locale.US);
        RuleBasedCollator clone = (RuleBasedCollator) coll.clone();
        assertNotSame(coll, clone);
        assertEquals(coll.getRules(), clone.getRules());
        assertEquals(coll.getDecomposition(), clone.getDecomposition());
        assertEquals(coll.getStrength(), clone.getStrength());
    }

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEqualsObject() throws ParseException {
        String rule = "< a < b < c < d < e";
        RuleBasedCollator coll = new RuleBasedCollator(rule);

        assertEquals(Collator.TERTIARY, coll.getStrength());
        assertEquals(Collator.CANONICAL_DECOMPOSITION, coll.getDecomposition());
        RuleBasedCollator other = new RuleBasedCollator(rule);
        assertTrue(coll.equals(other));

        coll.setStrength(Collator.PRIMARY);
        assertFalse(coll.equals(other));

        coll.setStrength(Collator.TERTIARY);
        coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        assertTrue(coll.equals(other));
    }

    /*
     * Class under test for int compare(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "compare",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testCompareStringString() throws ParseException {
        String rule = "< c < b < a";
        RuleBasedCollator coll = new RuleBasedCollator(rule);
        assertEquals(-1, coll.compare("c", "a"));
        assertEquals(-1, coll.compare("a", "d"));
        assertEquals(1, coll.compare("3", "1"));
        assertEquals(1, coll.compare("A", "1"));
        assertEquals(0, coll.compare("A", "A"));
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Doesn't verify null as a parameter.",
        method = "getCollationKey",
        args = {java.lang.String.class}
    )
    public void testGetCollationKey() {
        RuleBasedCollator coll = (RuleBasedCollator) Collator
                .getInstance(Locale.GERMAN);
        String source = "abc";
        CollationKey key1 = coll.getCollationKey(source);
        assertEquals(source, key1.getSourceString());
        String source2 = "abb";
        CollationKey key2 = coll.getCollationKey(source2);
        assertEquals(source2, key2.getSourceString());
        assertTrue(key1.compareTo(key2) > 0);
        assertTrue(coll.compare(source, source2) > 0);

    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getRules",
        args = {}
    )
    public void testGetRules() throws ParseException {
        String rule = "< a = b < c";
        RuleBasedCollator coll = new RuleBasedCollator(rule);
        assertEquals(rule, coll.getRules());
    }

    /*
     * Class under test for java.text.CollationElementIterator
     * getCollationElementIterator(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getCollationElementIterator",
        args = {java.lang.String.class}
    )
    public void testGetCollationElementIteratorString() throws Exception {
        {
            Locale locale = new Locale("es", "", "TRADITIONAL");
            RuleBasedCollator coll = (RuleBasedCollator) Collator
                    .getInstance(locale);
            String source = "cha";
            CollationElementIterator iterator = coll
                    .getCollationElementIterator(source);
            int[] e_offset = { 0, 1, 2 };
            int offset = iterator.getOffset();
            int i = 0;
            assertEquals(e_offset[i++], offset);
            while (offset != source.length() - 1) {
                iterator.next();
                offset = iterator.getOffset();
                assertEquals(e_offset[i++], offset);
            }
        }

        {
            Locale locale = new Locale("de", "DE");
            RuleBasedCollator coll = (RuleBasedCollator) Collator
                    .getInstance(locale);
            String source = "\u00E6b";
            CollationElementIterator iterator = coll
                    .getCollationElementIterator(source);
            int[] e_offset = { 0, 1, 1, 2 };
            int offset = iterator.getOffset();
            int i = 0;
            assertEquals(e_offset[i++], offset);
            while (offset != source.length()) {
                iterator.next();
                offset = iterator.getOffset();
                assertEquals(e_offset[i++], offset);
            }
        }
        // Regression for HARMONY-1352
        try {
            new RuleBasedCollator("< a< b< c< d").getCollationElementIterator((String)null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /*
     * Class under test for java.text.CollationElementIterator
     * getCollationElementIterator(java.text.CharacterIterator)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getCollationElementIterator",
        args = {java.text.CharacterIterator.class}
    )
    @KnownFailure("ICU seems to miss collation data")
    public void testGetCollationElementIteratorCharacterIterator() throws Exception {
        {
            Locale locale = new Locale("cs", "CZ", "");
            RuleBasedCollator coll = (RuleBasedCollator) Collator
                    .getInstance(locale);
            String text = "cha";
            StringCharacterIterator source = new StringCharacterIterator(text);
            CollationElementIterator iterator = coll
                    .getCollationElementIterator(source);
            int[] e_offset = { 0, 2 };
            int offset = iterator.getOffset();
            int i = 0;
            assertEquals(e_offset[i++], offset);
            while (offset != text.length() - 1) {
                iterator.next();
                offset = iterator.getOffset();
                assertEquals(e_offset[i], offset);
                i++;
            }
        }

        {
            Locale locale = new Locale("de", "DE");
            RuleBasedCollator coll = (RuleBasedCollator) Collator
                    .getInstance(locale);
            String text = "\u00E6b";
            StringCharacterIterator source = new StringCharacterIterator(text);
            CollationElementIterator iterator = coll
                    .getCollationElementIterator(source);
            int[] e_offset = { 0, 1, 1, 2 };
            int offset = iterator.getOffset();
            int i = 0;
            assertEquals(e_offset[i++], offset);
            while (offset != text.length()) {
                iterator.next();
                offset = iterator.getOffset();
                assertEquals(e_offset[i++], offset);
            }
        }
        // Regression for HARMONY-1352
        try {
            new RuleBasedCollator("< a< b< c< d").getCollationElementIterator((CharacterIterator)null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Doesn't verify setStrength method with PRIMARY, SECONDARY, TERTIARY or IDENTICAL values as a parameter; doesn't verify thatsetStrength method can throw IllegalArgumentException.",
            method = "setStrength",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Doesn't verify setStrength method with PRIMARY, SECONDARY, TERTIARY or IDENTICAL values as a parameter; doesn't verify thatsetStrength method can throw IllegalArgumentException.",
            method = "getStrength",
            args = {}
        )
    })
    public void testStrength() {
        RuleBasedCollator coll = (RuleBasedCollator) Collator
                .getInstance(Locale.US);
        for (int i = 0; i < 4; i++) {
            coll.setStrength(i);
            assertEquals(i, coll.getStrength());
        }

    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setDecomposition",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getDecomposition",
            args = {}
        )
    })
    public void testDecomposition() {
        RuleBasedCollator coll = (RuleBasedCollator) Collator
                .getInstance(Locale.US);
        
        int [] decompositions = {Collator.NO_DECOMPOSITION, 
                                 Collator.CANONICAL_DECOMPOSITION}; 
        
        for (int decom:decompositions) {
            coll.setDecomposition(decom);
            assertEquals(decom, coll.getDecomposition());
        }
        
        try {
            coll.setDecomposition(-1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInstance",
        args = {}
    )
    public void testCollator_GetInstance() {
        Collator coll = Collator.getInstance();
        Object obj1 = "a";
        Object obj2 = "b";
        assertEquals(-1, coll.compare(obj1, obj2));

        Collator.getInstance();
        assertFalse(coll.equals("A", "\uFF21"));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getAvailableLocales",
        args = {}
    )
    public void testGetAvaiableLocales() {
        Locale[] locales = Collator.getAvailableLocales();
        boolean isUS = false;
        for (int i = 0; i < locales.length; i++) {
            if(locales[i].equals(Locale.US))
                isUS = true;
        }
        assertTrue("No Locale.US in the array.", isUS);
    }

    // Test CollationKey
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getCollationKey",
        args = {java.lang.String.class}
    )
    public void testCollationKey() {
        Collator coll = Collator.getInstance(Locale.US);
        String text = "abc";
        CollationKey key = coll.getCollationKey(text);
        key.hashCode();

        CollationKey key2 = coll.getCollationKey("abc");

        assertEquals(0, key.compareTo(key2));
    }

    /**
     * @tests java.text.RuleBasedCollator.RuleBasedCollator(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies RuleBasedCollator(java.lang.String) constructor with null as a parameter.",
        method = "RuleBasedCollator",
        args = {java.lang.String.class}
    )
    public void testNullPointerException() throws Exception {
        // Regression for HARMONY-241
        try {
            new RuleBasedCollator(null);
            fail("Constructor RuleBasedCollator(null) "
                    + "should throw NullPointerException");
        } catch (NullPointerException e) {}
    }

    /**
     * @tests java.text.RuleBasedCollator.compare(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies null as parameters.",
        method = "compare",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testCompareNull() throws Exception {
        // Regression for HARMONY-836
        try {
            new RuleBasedCollator("< a").compare(null, null);
            fail("RuleBasedCollator.compare(null, null) "
                    + "should throw NullPointerException");
        } catch (NullPointerException e) {}
    }

    /**
     * @tests java.text.RuleBasedCollator.RuleBasedCollator(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies empty string as a parameter.",
        method = "RuleBasedCollator",
        args = {java.lang.String.class}
    )
    @AndroidOnly("Android uses icu for collating. " +
            "Icu has default UCA rules it uses to collate. " +
            "To create a default instance with these rules an empty " +
            "rule has to be passed to icu. This behavior is different " +
            "from the RI which would throw an exception.")
    public void testEmptyStringException() {
        try {
            RuleBasedCollator coll = new RuleBasedCollator("");
            assertTrue(coll.equals(new RuleBasedCollator("")));
        } catch (ParseException e) {
            fail("Constructor RuleBasedCollator(\"\") "
                    + "should NOT throw ParseException.");
        }
    }
}
