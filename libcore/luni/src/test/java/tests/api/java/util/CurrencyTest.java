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

package tests.api.java.util;

import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.AndroidOnly;

import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.Iterator;
import java.util.Locale;

@TestTargetClass(Currency.class) 
public class CurrencyTest extends junit.framework.TestCase {

    private static Locale defaultLocale = Locale.getDefault();

    /**
     * @tests java.util.Currency#getInstance(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "getInstance(String) method is tested in test_getInstanceLjava_util_Locale() test.",
        method = "getInstance",
        args = {java.lang.String.class}
    )
    public void test_getInstanceLjava_lang_String() {
        // see test_getInstanceLjava_util_Locale() tests
    }

    /**
     * @tests java.util.Currency#getInstance(java.util.Locale)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInstance",
        args = {java.util.Locale.class}
    )
    public void test_getInstanceLjava_util_Locale() {
        /*
         * the behaviour in all these three cases should be the same since this
         * method ignores language and variant component of the locale.
         */
        Currency c0 = Currency.getInstance("CAD");
        Currency c1 = Currency.getInstance(new Locale("en", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"en\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c1 == c0);
        Currency c2 = Currency.getInstance(new Locale("fr", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"fr\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c2 == c0);
        Currency c3 = Currency.getInstance(new Locale("", "CA"));
        assertTrue(
                "Currency.getInstance(new Locale(\"\",\"CA\")) isn't equal to Currency.getInstance(\"CAD\")",
                c3 == c0);

        c0 = Currency.getInstance("JPY");
        c1 = Currency.getInstance(new Locale("ja", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"ja\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c1 == c0);
        c2 = Currency.getInstance(new Locale("", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c2 == c0);
        c3 = Currency.getInstance(new Locale("bogus", "JP"));
        assertTrue(
                "Currency.getInstance(new Locale(\"bogus\",\"JP\")) isn't equal to Currency.getInstance(\"JPY\")",
                c3 == c0);

        Locale localeGu = new Locale("gu", "IN");
        Currency cGu = Currency.getInstance(localeGu);
        Locale localeKn = new Locale("kn", "IN");
        Currency cKn = Currency.getInstance(localeKn);
        assertTrue("Currency.getInstance(Locale_" + localeGu.toString() + "))"
                + "isn't equal to " + "Currency.getInstance(Locale_"
                + localeKn.toString() + "))", cGu == cKn);

        // some teritories do not have currencies, like Antarctica
        Locale loc = new Locale("", "AQ");
        try {
            Currency curr = Currency.getInstance(loc);
            assertNull(
                    "Currency.getInstance(new Locale(\"\", \"AQ\")) did not return null",
                    curr);
        } catch (IllegalArgumentException e) {
            fail("Unexpected IllegalArgumentException " + e);
        }

        // unsupported/legacy iso3 countries
        loc = new Locale("", "ZR");
        try {
            Currency curr = Currency.getInstance(loc);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        loc = new Locale("", "ZAR");
        try {
            Currency curr = Currency.getInstance(loc);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        loc = new Locale("", "FX");
        try {
            Currency curr = Currency.getInstance(loc);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        loc = new Locale("", "FXX");
        try {
            Currency curr = Currency.getInstance(loc);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests java.util.Currency#getSymbol()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSymbol",
        args = {}
    )
    @AndroidOnly("icu and the RI have different data. Because Android"
            + "only defines a few locales as a must have, it was not possible"
            + "to find a set of combinations where no differences between"
            + "the RI and Android exist.")
    public void test_getSymbol() {
        
        Currency currK = Currency.getInstance("KRW");
        Currency currI = Currency.getInstance("IEP");
        Currency currUS = Currency.getInstance("USD");

        Locale.setDefault(Locale.US);
        assertEquals("currK.getSymbol()", "KRW", currK.getSymbol());
        assertEquals("currI.getSymbol()", "IR\u00a3", currI.getSymbol());
        assertEquals("currUS.getSymbol()", "$", currUS.getSymbol());

        Locale.setDefault(new Locale("en", "IE"));
        assertEquals("currK.getSymbol()", "KRW", currK.getSymbol());
        assertEquals("currI.getSymbol()", "\u00a3", currI.getSymbol());
        assertEquals("currUS.getSymbol()", "USD", currUS.getSymbol());

        // test what happens if this is an invalid locale,
        // one with Korean country but an India language
        Locale.setDefault(new Locale("kr", "KR"));
        assertEquals("currK.getSymbol()", "KRW", currK.getSymbol());
        assertEquals("currI.getSymbol()", "IR\u00a3", currI.getSymbol());
        assertEquals("currUS.getSymbol()", "$", currUS.getSymbol());
    }

    /**
     * @tests java.util.Currency#getSymbol(java.util.Locale)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSymbol",
        args = {java.util.Locale.class}
    )
    @AndroidOnly("specification doesn't include strong requirements for returnig symbol. On android platform used wrong character for yen sign: \u00a5 instead of \uffe5. Both of them give correct image though")
    public void test_getSymbolLjava_util_Locale() {
        //Tests was simplified because java specification not
        // includes strong requirements for returnig symbol.
        // on android platform used wrong character for yen
        // sign: \u00a5 instead of \uffe5
        Locale[] loc1 = new Locale[]{
                Locale.JAPAN,  Locale.JAPANESE,
                Locale.FRANCE, Locale.FRENCH,
                Locale.US,     Locale.UK,
                Locale.CANADA, Locale.CANADA_FRENCH,
                Locale.ENGLISH, 
                new Locale("ja", "JP"), new Locale("", "JP"),

                new Locale("fr", "FR"), new Locale("", "FR"),

                new Locale("en", "US"), new Locale("", "US"),
                new Locale("es", "US"), new Locale("ar", "US"),
                new Locale("ja", "US"),

                new Locale("en", "CA"), new Locale("fr", "CA"), 
                new Locale("", "CA"),   new Locale("ar", "CA"),

                new Locale("ja", "JP"), new Locale("", "JP"),
                new Locale("ar", "JP"),

                new Locale("ja", "AE"), new Locale("en", "AE"),
                new Locale("ar", "AE"),

                new Locale("da", "DK"), new Locale("", "DK"),

                new Locale("da", ""), new Locale("ja", ""),
                new Locale("en", "")};
                
        String[] euro    = new String[] {"EUR", "\u20ac"};
        // \u00a5 and \uffe5 are actually the same symbol, just different code points.
        // But the RI returns the \uffe5 and Android returns those with \u00a5 
        String[] yen     = new String[] {"JPY", "\u00a5", "\u00a5JP", "JP\u00a5", "\uffe5", "\uffe5JP", "JP\uffe5"};
        String[] dollar  = new String[] {"USD", "$", "US$", "$US"};
        String[] cDollar = new String[] {"CAD", "$", "Can$", "$Ca"};

        Currency currE   = Currency.getInstance("EUR");
        Currency currJ   = Currency.getInstance("JPY");
        Currency currUS  = Currency.getInstance("USD");
        Currency currCA  = Currency.getInstance("CAD");

        int i, j, k;
        boolean flag;
        
        for(k = 0; k < loc1.length; k++) {
            Locale.setDefault(loc1[k]);
            
            for (i = 0; i < loc1.length; i++) {
                flag = false;
                for  (j = 0; j < euro.length; j++) {
                    if (currE.getSymbol(loc1[i]).equals(euro[j])) {
                        flag = true;
                        break;
                    }
                }
                assertTrue("Default Locale is: " + Locale.getDefault()
                        + ". For locale " + loc1[i]
                        + " the Euro currency returned "
                        + currE.getSymbol(loc1[i])
                        + ". Expected was one of these: "
                        + Arrays.toString(euro), flag);
            }
            
            for (i = 0; i < loc1.length; i++) {
                flag = false;
                for  (j = 0; j < yen.length; j++) {
                    byte[] b1 = null;
                    byte[] b2 = null;
                    if (currJ.getSymbol(loc1[i]).equals(yen[j])) {
                        flag = true;
                        break;
                    }
                }
                assertTrue("Default Locale is: " + Locale.getDefault()
                        + ". For locale " + loc1[i]
                        + " the Yen currency returned "
                        + currJ.getSymbol(loc1[i])
                        + ". Expected was one of these: "
                        + Arrays.toString(yen), flag);
            }
            
            for (i = 0; i < loc1.length; i++) {
                flag = false;
                for  (j = 0; j < dollar.length; j++) {
                    if (currUS.getSymbol(loc1[i]).equals(dollar[j])) {
                        flag = true;
                        break;
                    }
                }
                assertTrue("Default Locale is: " + Locale.getDefault()
                        + ". For locale " + loc1[i]
                        + " the Dollar currency returned "
                        + currUS.getSymbol(loc1[i])
                        + ". Expected was one of these: "
                        + Arrays.toString(dollar), flag);
            }
            
            for (i = 0; i < loc1.length; i++) {
                flag = false;
                for  (j = 0; j < cDollar.length; j++) {
                    if (currCA.getSymbol(loc1[i]).equals(cDollar[j])) {
                        flag = true;
                        break;
                    }
                }
                assertTrue("Default Locale is: " + Locale.getDefault()
                        + ". For locale " + loc1[i]
                        + " the Canadian Dollar currency returned "
                        + currCA.getSymbol(loc1[i])
                        + ". Expected was one of these: "
                        + Arrays.toString(cDollar), flag);
            }
        }
    }

    /**
     * @tests java.util.Currency#getDefaultFractionDigits()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDefaultFractionDigits",
        args = {}
    )
    public void test_getDefaultFractionDigits() {

        Currency c1 = Currency.getInstance("TND");
        c1.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c1
                + "\") returned incorrect number of digits. ", 3, c1
                .getDefaultFractionDigits());

        Currency c2 = Currency.getInstance("EUR");
        c2.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c2
                + "\") returned incorrect number of digits. ", 2, c2
                .getDefaultFractionDigits());

        Currency c3 = Currency.getInstance("JPY");
        c3.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c3
                + "\") returned incorrect number of digits. ", 0, c3
                .getDefaultFractionDigits());

        Currency c4 = Currency.getInstance("XXX");
        c4.getDefaultFractionDigits();
        assertEquals(" Currency.getInstance(\"" + c4
                + "\") returned incorrect number of digits. ", -1, c4
                .getDefaultFractionDigits());
    }

    /**
     * @tests java.util.Currency#getCurrencyCode() Note: lines under remarks
     *        (Locale.CHINESE, Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
     *        Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN) raises exception
     *        on SUN VM
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getCurrencyCode",
        args = {}
    )
    public void test_getCurrencyCode() {
        final Collection<Locale> locVal = Arrays.asList(
                Locale.CANADA,
                Locale.CANADA_FRENCH,
                Locale.CHINA,
                // Locale.CHINESE,
                // Locale.ENGLISH,
                Locale.FRANCE,
                // Locale.FRENCH,
                // Locale.GERMAN,
                Locale.GERMANY,
                // Locale.ITALIAN,
                Locale.ITALY, Locale.JAPAN,
                // Locale.JAPANESE,
                Locale.KOREA,
                // Locale.KOREAN,
                Locale.PRC, Locale.SIMPLIFIED_CHINESE, Locale.TAIWAN, Locale.TRADITIONAL_CHINESE,
                Locale.UK, Locale.US);
        final Collection<String> locDat = Arrays.asList("CAD", "CAD", "CNY", "EUR", "EUR", "EUR",
                "JPY", "KRW", "CNY", "CNY", "TWD", "TWD", "GBP", "USD");

        Iterator<String> dat = locDat.iterator();
        for (Locale l : locVal) {
            String d = dat.next().trim();
            assertEquals("For locale " + l + " currency code wrong", Currency.getInstance(l)
                    .getCurrencyCode(), d);
        }
    }

    /**
     * @tests java.util.Currency#toString() Note: lines under remarks
     *        (Locale.CHINESE, Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
     *        Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN) raises exception
     *        on SUN VM
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        final Collection<Locale> locVal = Arrays.asList(
                Locale.CANADA,
                Locale.CANADA_FRENCH,
                Locale.CHINA,
                // Locale.CHINESE,
                // Locale.ENGLISH,
                Locale.FRANCE,
                // Locale.FRENCH,
                // Locale.GERMAN,
                Locale.GERMANY,
                // Locale.ITALIAN,
                Locale.ITALY, Locale.JAPAN,
                // Locale.JAPANESE,
                Locale.KOREA,
                // Locale.KOREAN,
                Locale.PRC, Locale.SIMPLIFIED_CHINESE, Locale.TAIWAN, Locale.TRADITIONAL_CHINESE,
                Locale.UK, Locale.US);
        final Collection<String> locDat = Arrays.asList("CAD", "CAD", "CNY", "EUR", "EUR", "EUR",
                "JPY", "KRW", "CNY", "CNY", "TWD", "TWD", "GBP", "USD");
    
        Iterator<String> dat = locDat.iterator();
        for (Locale l : locVal) {
            String d = dat.next().trim();
            assertEquals("For locale " + l + " Currency.toString method returns wrong value",
                    Currency.getInstance(l).toString(), d);
        }
    }

    protected void setUp() {
        Locale.setDefault(defaultLocale);
    }

    protected void tearDown() {
    }

    /**
     * Helper method to display Currency info
     * 
     * @param c
     */
    private void printCurrency(Currency c) {
        System.out.println();
        System.out.println(c.getCurrencyCode());
        System.out.println(c.getSymbol());
        System.out.println(c.getDefaultFractionDigits());
    }

    /**
     * helper method to display Locale info
     */
    private static void printLocale(Locale loc) {
        System.out.println();
        System.out.println(loc.getDisplayName());
        System.out.println(loc.getCountry());
        System.out.println(loc.getLanguage());
        System.out.println(loc.getDisplayCountry());
        System.out.println(loc.getDisplayLanguage());
        System.out.println(loc.getDisplayName());
        System.out.println(loc.getISO3Country());
        System.out.println(loc.getISO3Language());
    }
}
