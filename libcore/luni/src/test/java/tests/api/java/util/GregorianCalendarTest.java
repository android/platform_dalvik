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

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.KnownFailure;

import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;


@TestTargetClass(GregorianCalendar.class) 
public class GregorianCalendarTest extends junit.framework.TestCase {

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {}
    )
    public void test_Constructor() {
        // Test for method java.util.GregorianCalendar()
        assertTrue("Constructed incorrect calendar", (new GregorianCalendar()
                .isLenient()));
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(int, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {int.class, int.class, int.class}
    )
    public void test_ConstructorIII() {
        // Test for method java.util.GregorianCalendar(int, int, int)
        GregorianCalendar gc = new GregorianCalendar(1972, Calendar.OCTOBER, 13);
        assertEquals("Incorrect calendar constructed 1",
                1972, gc.get(Calendar.YEAR));
        assertTrue("Incorrect calendar constructed 2",
                gc.get(Calendar.MONTH) == Calendar.OCTOBER);
        assertEquals("Incorrect calendar constructed 3", 13, gc
                .get(Calendar.DAY_OF_MONTH));
        assertTrue("Incorrect calendar constructed 4", gc.getTimeZone().equals(
                TimeZone.getDefault()));
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(int, int, int, int,
     *        int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {int.class, int.class, int.class, int.class, int.class}
    )
    public void test_ConstructorIIIII() {
        // Test for method java.util.GregorianCalendar(int, int, int, int, int)
        GregorianCalendar gc = new GregorianCalendar(1972, Calendar.OCTOBER,
                13, 19, 9);
        assertEquals("Incorrect calendar constructed",
                1972, gc.get(Calendar.YEAR));
        assertTrue("Incorrect calendar constructed",
                gc.get(Calendar.MONTH) == Calendar.OCTOBER);
        assertEquals("Incorrect calendar constructed", 13, gc
                .get(Calendar.DAY_OF_MONTH));
        assertEquals("Incorrect calendar constructed", 7, gc.get(Calendar.HOUR));
        assertEquals("Incorrect calendar constructed",
                1, gc.get(Calendar.AM_PM));
        assertEquals("Incorrect calendar constructed",
                9, gc.get(Calendar.MINUTE));
        assertTrue("Incorrect calendar constructed", gc.getTimeZone().equals(
                TimeZone.getDefault()));

        //Regression for HARMONY-998
        gc = new GregorianCalendar(1900, 0, 0, 0, Integer.MAX_VALUE);
        assertEquals("Incorrect calendar constructed",
                5983, gc.get(Calendar.YEAR));
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(int, int, int, int,
     *        int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {int.class, int.class, int.class, int.class, int.class, int.class}
    )
    public void test_ConstructorIIIIII() {
        // Test for method java.util.GregorianCalendar(int, int, int, int, int,
        // int)
        GregorianCalendar gc = new GregorianCalendar(1972, Calendar.OCTOBER,
                13, 19, 9, 59);
        assertEquals("Incorrect calendar constructed",
                1972, gc.get(Calendar.YEAR));
        assertTrue("Incorrect calendar constructed",
                gc.get(Calendar.MONTH) == Calendar.OCTOBER);
        assertEquals("Incorrect calendar constructed", 13, gc
                .get(Calendar.DAY_OF_MONTH));
        assertEquals("Incorrect calendar constructed", 7, gc.get(Calendar.HOUR));
        assertEquals("Incorrect calendar constructed",
                1, gc.get(Calendar.AM_PM));
        assertEquals("Incorrect calendar constructed",
                9, gc.get(Calendar.MINUTE));
        assertEquals("Incorrect calendar constructed",
                59, gc.get(Calendar.SECOND));
        assertTrue("Incorrect calendar constructed", gc.getTimeZone().equals(
                TimeZone.getDefault()));
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(java.util.Locale)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {java.util.Locale.class}
    )
    public void test_ConstructorLjava_util_Locale() {
        // Test for method java.util.GregorianCalendar(java.util.Locale)
        Date date = new Date();
        GregorianCalendar gcUS = new GregorianCalendar(Locale.US);
        gcUS.setTime(date);
        GregorianCalendar gcUS2 = new GregorianCalendar(Locale.US);
        gcUS2.setTime(date);
        GregorianCalendar gcFrance = new GregorianCalendar(Locale.FRANCE);
        gcFrance.setTime(date);
        assertTrue("Locales not created correctly", gcUS.equals(gcUS2));
        assertFalse("Locales not created correctly", gcUS.equals(gcFrance));
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(java.util.TimeZone)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {java.util.TimeZone.class}
    )
    public void test_ConstructorLjava_util_TimeZone() {
        // Test for method java.util.GregorianCalendar(java.util.TimeZone)
        Date date = new Date();
        TimeZone.getDefault();
        GregorianCalendar gc1 = new GregorianCalendar(TimeZone
                .getTimeZone("EST"));
        gc1.setTime(date);
        GregorianCalendar gc2 = new GregorianCalendar(TimeZone
                .getTimeZone("CST"));
        gc2.setTime(date);

        assertFalse(gc1.equals(gc2));

        gc1 = new GregorianCalendar(TimeZone
                .getTimeZone("GMT+2"));
        gc1.setTime(date);
        gc2 = new GregorianCalendar(TimeZone
                .getTimeZone("GMT+1"));
        gc2.setTime(date);
        assertTrue("Incorrect calendar returned",
                gc1.get(Calendar.HOUR) == ((gc2.get(Calendar.HOUR) + 1) % 12));
        
        // Regression test for HARMONY-2961
        SimpleTimeZone timezone = new SimpleTimeZone(-3600 * 24 * 1000 * 2,
                "GMT");
        GregorianCalendar gc = new GregorianCalendar(timezone);
    }

    /**
     * @tests java.util.GregorianCalendar#GregorianCalendar(java.util.TimeZone,
     *        java.util.Locale)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "GregorianCalendar",
        args = {java.util.TimeZone.class, java.util.Locale.class}
    )
    public void test_ConstructorLjava_util_TimeZoneLjava_util_Locale() {
        // Test for method java.util.GregorianCalendar(java.util.TimeZone,
        // java.util.Locale)
        Date date = new Date();
        TimeZone.getDefault();
        GregorianCalendar gc1 = new GregorianCalendar(TimeZone
                .getTimeZone("EST"), Locale.US);
        gc1.setTime(date);
        GregorianCalendar gc2 = new GregorianCalendar(TimeZone
                .getTimeZone("EST"), Locale.US);
        gc2.setTime(date);
        GregorianCalendar gc3 = new GregorianCalendar(TimeZone
                .getTimeZone("CST"), Locale.FRANCE);
        gc3.setTime(date);
        assertTrue(gc1.equals(gc2));
        assertFalse(gc2.equals(gc3));
        assertFalse(gc3.equals(gc1));

        gc1 = new GregorianCalendar(TimeZone
                .getTimeZone("GMT+2"), Locale.US);
        gc1.setTime(date);
        gc3 = new GregorianCalendar(TimeZone
                .getTimeZone("GMT+1"), Locale.FRANCE);
        gc3.setTime(date);
        // CST is 1 hour before EST, add 1 to the CST time and convert to 0-12
        // value
        assertTrue("Incorrect calendar returned",
                gc1.get(Calendar.HOUR) == ((gc3.get(Calendar.HOUR) + 1) % 12));
    }

    /**
     * @tests java.util.GregorianCalendar#add(int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "add",
        args = {int.class, int.class}
    )
    @AndroidOnly("This test fails on the RI with version 1.5 but succeeds"
            + "on the RI with version 1.6")
    public void test_addII() {
        // Test for method void java.util.GregorianCalendar.add(int, int)
        GregorianCalendar gc1 = new GregorianCalendar(1998, 11, 6);
        gc1.add(GregorianCalendar.YEAR, 1);
        assertEquals("Add failed to Increment",
                1999, gc1.get(GregorianCalendar.YEAR));

        gc1 = new GregorianCalendar(1999, Calendar.JULY, 31);
        gc1.add(Calendar.MONTH, 7);
        assertEquals("Wrong result year 1", 2000, gc1.get(Calendar.YEAR));
        assertTrue("Wrong result month 1",
                gc1.get(Calendar.MONTH) == Calendar.FEBRUARY);
        assertEquals("Wrong result date 1", 29, gc1.get(Calendar.DATE));

        gc1.add(Calendar.YEAR, -1);
        assertEquals("Wrong result year 2", 1999, gc1.get(Calendar.YEAR));
        assertTrue("Wrong result month 2",
                gc1.get(Calendar.MONTH) == Calendar.FEBRUARY);
        assertEquals("Wrong result date 2", 28, gc1.get(Calendar.DATE));

        gc1 = new GregorianCalendar(TimeZone.getTimeZone("EST"));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.MILLISECOND, 24 * 60 * 60 * 1000);
        
        assertEquals("Wrong time after MILLISECOND change", 16, gc1
              .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.SECOND, 24 * 60 * 60);
        assertEquals("Wrong time after SECOND change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.MINUTE, 24 * 60);
        assertEquals("Wrong time after MINUTE change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.HOUR, 24);
        assertEquals("Wrong time after HOUR change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.HOUR_OF_DAY, 24);
        assertEquals("Wrong time after HOUR_OF_DAY change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));

        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.AM_PM, 2);
        assertEquals("Wrong time after AM_PM change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.DATE, 1);
        assertEquals("Wrong time after DATE change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.DAY_OF_YEAR, 1);
        assertEquals("Wrong time after DAY_OF_YEAR change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.DAY_OF_WEEK, 1);
        assertEquals("Wrong time after DAY_OF_WEEK change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.WEEK_OF_YEAR, 1);
        assertEquals("Wrong time after WEEK_OF_YEAR change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.WEEK_OF_MONTH, 1);
        assertEquals("Wrong time after WEEK_OF_MONTH change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));
        gc1.set(1999, Calendar.APRIL, 3, 16, 0); // day before DST change
        gc1.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        assertEquals("Wrong time after DAY_OF_WEEK_IN_MONTH change", 16, gc1
                .get(Calendar.HOUR_OF_DAY));

        gc1.clear();
        gc1.set(2000, Calendar.APRIL, 1, 23, 0);
        gc1.add(Calendar.DATE, 1);
        assertTrue("Wrong time after DATE change near DST boundary", gc1
                .get(Calendar.MONTH) == Calendar.APRIL
                && gc1.get(Calendar.DATE) == 2
                && gc1.get(Calendar.HOUR_OF_DAY) == 23);
    }

    /**
     * @tests java.util.GregorianCalendar#equals(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean
        // java.util.GregorianCalendar.equals(java.lang.Object)
        GregorianCalendar gc1 = new GregorianCalendar(1998, 11, 6);
        GregorianCalendar gc2 = new GregorianCalendar(2000, 11, 6);
        GregorianCalendar gc3 = new GregorianCalendar(1998, 11, 6);
        assertTrue("Equality check failed", gc1.equals(gc3));
        assertTrue("Equality check failed", !gc1.equals(gc2));
        gc3.setGregorianChange(new Date());
        assertTrue("Different gregorian change", !gc1.equals(gc3));
    }

    /**
     * @tests java.util.GregorianCalendar#getActualMaximum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getActualMaximum",
        args = {int.class}
    )
    public void test_getActualMaximumI() {
        // Test for method int java.util.GregorianCalendar.getActualMaximum(int)
        GregorianCalendar gc1 = new GregorianCalendar(1900, 1, 1);
        GregorianCalendar gc2 = new GregorianCalendar(1996, 1, 1);
        GregorianCalendar gc3 = new GregorianCalendar(1997, 1, 1);
        GregorianCalendar gc4 = new GregorianCalendar(2000, 1, 1);
        GregorianCalendar gc5 = new GregorianCalendar(2000, 9, 9);
        GregorianCalendar gc6 = new GregorianCalendar(2000, 3, 3);
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Feb 1900",
                28, gc1.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Feb 1996",
                29, gc2.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Feb 1998",
                28, gc3.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Feb 2000",
                29, gc4.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Oct 2000",
                31, gc5.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertEquals("Wrong actual maximum value for DAY_OF_MONTH for Apr 2000",
                30, gc6.getActualMaximum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong actual maximum value for MONTH", gc1
                .getActualMaximum(Calendar.MONTH) == Calendar.DECEMBER);
        assertEquals("Wrong actual maximum value for HOUR_OF_DAY", 23, gc1
                .getActualMaximum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong actual maximum value for HOUR", 11, gc1
                .getActualMaximum(Calendar.HOUR));
        assertEquals("Wrong actual maximum value for DAY_OF_WEEK_IN_MONTH", 4, gc6
                .getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH));
        
        
        // Regression test for harmony 2954
        Date date = new Date(Date.parse("Jan 15 00:00:01 GMT 2000"));
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(Date.parse("Dec 15 00:00:01 GMT 1582"));
        assertEquals(355, gc.getActualMaximum(Calendar.DAY_OF_YEAR)); 
        gc.setGregorianChange(date);
        gc.setTimeInMillis(Date.parse("Jan 16 00:00:01 GMT 2000"));
        assertEquals(353, gc.getActualMaximum(Calendar.DAY_OF_YEAR)); 
        
        //Regression test for HARMONY-3004
        gc = new GregorianCalendar(1900, 7, 1);
        String[] ids = TimeZone.getAvailableIDs();
        for (int i = 0; i < ids.length; i++) {
            TimeZone tz = TimeZone.getTimeZone(ids[i]);
            gc.setTimeZone(tz);
            for (int j = 1900; j < 2000; j++) {
                gc.set(Calendar.YEAR, j);
                assertEquals(7200000, gc.getActualMaximum(Calendar.DST_OFFSET));
            }
        }
    }

    /**
     * @tests java.util.GregorianCalendar#getActualMinimum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getActualMinimum",
        args = {int.class}
    )
    public void test_getActualMinimumI() {
        // Test for method int java.util.GregorianCalendar.getActualMinimum(int)
        GregorianCalendar gc1 = new GregorianCalendar(1900, 1, 1);
        new GregorianCalendar(1996, 1, 1);
        new GregorianCalendar(1997, 1, 1);
        new GregorianCalendar(2000, 1, 1);
        new GregorianCalendar(2000, 9, 9);
        GregorianCalendar gc6 = new GregorianCalendar(2000, 3, 3);
        assertEquals("Wrong actual minimum value for DAY_OF_MONTH for Feb 1900",
                1, gc1.getActualMinimum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong actual minimum value for MONTH", gc1
                .getActualMinimum(Calendar.MONTH) == Calendar.JANUARY);
        assertEquals("Wrong actual minimum value for HOUR_OF_DAY", 0, gc1
                .getActualMinimum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong actual minimum value for HOUR", 0, gc1
                .getActualMinimum(Calendar.HOUR));
        assertEquals("Wrong actual minimum value for DAY_OF_WEEK_IN_MONTH", 1, gc6
                .getActualMinimum(Calendar.DAY_OF_WEEK_IN_MONTH));
    }

    /**
     * @tests java.util.GregorianCalendar#getGreatestMinimum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getGreatestMinimum",
        args = {int.class}
    )
    public void test_getGreatestMinimumI() {
        // Test for method int
        // java.util.GregorianCalendar.getGreatestMinimum(int)
        GregorianCalendar gc = new GregorianCalendar();
        assertEquals("Wrong greatest minimum value for DAY_OF_MONTH", 1, gc
                .getGreatestMinimum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong greatest minimum value for MONTH", gc
                .getGreatestMinimum(Calendar.MONTH) == Calendar.JANUARY);
        assertEquals("Wrong greatest minimum value for HOUR_OF_DAY", 0, gc
                .getGreatestMinimum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong greatest minimum value for HOUR", 0, gc
                .getGreatestMinimum(Calendar.HOUR));

        BitSet result = new BitSet();
        int[] min = { 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000,
                0 };
        for (int i = 0; i < min.length; i++) {
            if (gc.getGreatestMinimum(i) != min[i])
                result.set(i);
        }
        assertTrue("Wrong greatest min for " + result, result.length() == 0);
    }

    /**
     * @tests java.util.GregorianCalendar#getGregorianChange()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getGregorianChange",
        args = {}
    )
    public void test_getGregorianChange() {
        // Test for method java.util.Date
        // java.util.GregorianCalendar.getGregorianChange()
        GregorianCalendar gc = new GregorianCalendar();
        GregorianCalendar returnedChange = new GregorianCalendar(TimeZone
                .getTimeZone("EST"));
        returnedChange.setTime(gc.getGregorianChange());
        assertEquals("Returned incorrect year",
                1582, returnedChange.get(Calendar.YEAR));
        assertTrue("Returned incorrect month", returnedChange
                .get(Calendar.MONTH) == Calendar.OCTOBER);
        assertEquals("Returned incorrect day of month", 4, returnedChange
                .get(Calendar.DAY_OF_MONTH));
    }

    /**
     * @tests java.util.GregorianCalendar#getLeastMaximum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLeastMaximum",
        args = {int.class}
    )
    public void test_getLeastMaximumI() {
        // Test for method int java.util.GregorianCalendar.getLeastMaximum(int)
        GregorianCalendar gc = new GregorianCalendar();
        assertEquals("Wrong least maximum value for DAY_OF_MONTH", 28, gc
                .getLeastMaximum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong least maximum value for MONTH", gc
                .getLeastMaximum(Calendar.MONTH) == Calendar.DECEMBER);
        assertEquals("Wrong least maximum value for HOUR_OF_DAY", 23, gc
                .getLeastMaximum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong least maximum value for HOUR", 11, gc
                .getLeastMaximum(Calendar.HOUR));

        BitSet result = new BitSet();
        Vector values = new Vector();
        int[] max = { 1, 292269054, 11, 50, 3, 28, 355, 7, 3, 1, 11, 23, 59,
                59, 999, 50400000, 1200000 };
        for (int i = 0; i < max.length; i++) {
            if (gc.getLeastMaximum(i) != max[i]) {
                result.set(i);
                values.add(new Integer(gc.getLeastMaximum(i)));
            }
        }
        assertTrue("Wrong least max for " + result + " = " + values, result
                .length() == 0);
        
        // Regression test for harmony-2947
        Date date = new Date(Date.parse("Jan 1 00:00:01 GMT 2000"));
        gc = new GregorianCalendar();
        gc.setGregorianChange(date);
        gc.setTime(date);
        assertEquals(gc.getActualMaximum(Calendar.WEEK_OF_YEAR), gc
                .getLeastMaximum(Calendar.WEEK_OF_YEAR));
    }

    /**
     * @tests java.util.GregorianCalendar#getMaximum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMaximum",
        args = {int.class}
    )
    public void test_getMaximumI() {
        // Test for method int java.util.GregorianCalendar.getMaximum(int)
        GregorianCalendar gc = new GregorianCalendar();
        assertEquals("Wrong maximum value for DAY_OF_MONTH", 31, gc
                .getMaximum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong maximum value for MONTH", gc
                .getMaximum(Calendar.MONTH) == Calendar.DECEMBER);
        assertEquals("Wrong maximum value for HOUR_OF_DAY", 23, gc
                .getMaximum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong maximum value for HOUR",
                11, gc.getMaximum(Calendar.HOUR));

        BitSet result = new BitSet();
        Vector values = new Vector();
        int[] max = { 1, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59,
                59, 999, 50400000, 7200000 };
        for (int i = 0; i < max.length; i++) {
            if (gc.getMaximum(i) != max[i]) {
                result.set(i);
                values.add(new Integer(gc.getMaximum(i)));
            }
        }
        assertTrue("Wrong max for " + result + " = " + values,
                result.length() == 0);
    }

    /**
     * @tests java.util.GregorianCalendar#getMinimum(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMinimum",
        args = {int.class}
    )
    public void test_getMinimumI() {
        // Test for method int java.util.GregorianCalendar.getMinimum(int)
        GregorianCalendar gc = new GregorianCalendar();
        assertEquals("Wrong minimum value for DAY_OF_MONTH", 1, gc
                .getMinimum(Calendar.DAY_OF_MONTH));
        assertTrue("Wrong minimum value for MONTH", gc
                .getMinimum(Calendar.MONTH) == Calendar.JANUARY);
        assertEquals("Wrong minimum value for HOUR_OF_DAY", 0, gc
                .getMinimum(Calendar.HOUR_OF_DAY));
        assertEquals("Wrong minimum value for HOUR",
                0, gc.getMinimum(Calendar.HOUR));

        BitSet result = new BitSet();
        int[] min = { 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000,
                0 };
        for (int i = 0; i < min.length; i++) {
            if (gc.getMinimum(i) != min[i])
                result.set(i);
        }
        assertTrue("Wrong min for " + result, result.length() == 0);
    }

    /**
     * @tests java.util.GregorianCalendar#isLeapYear(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isLeapYear",
        args = {int.class}
    )
    public void test_isLeapYearI() {
        // Test for method boolean java.util.GregorianCalendar.isLeapYear(int)
        GregorianCalendar gc = new GregorianCalendar(1998, 11, 6);
        assertTrue("Returned incorrect value for leap year", !gc
                .isLeapYear(1998));
        assertTrue("Returned incorrect value for leap year", gc
                .isLeapYear(2000));

    }

    /**
     * @tests java.util.GregorianCalendar#roll(int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "roll",
        args = {int.class, int.class}
    )
    public void test_rollII() {
        // Test for method void java.util.GregorianCalendar.roll(int, int)
        GregorianCalendar gc = new GregorianCalendar(1972, Calendar.OCTOBER, 8,
                2, 5, 0);
        gc.roll(Calendar.DAY_OF_MONTH, -1);
        assertTrue("Failed to roll DAY_OF_MONTH down by 1", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 7, 2, 5,
                        0)));
        gc = new GregorianCalendar(1972, Calendar.OCTOBER, 8, 2, 5, 0);
        gc.roll(Calendar.DAY_OF_MONTH, 25);
        assertTrue("Failed to roll DAY_OF_MONTH up by 25", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 2, 2, 5,
                        0)));
        gc = new GregorianCalendar(1972, Calendar.OCTOBER, 8, 2, 5, 0);
        gc.roll(Calendar.DAY_OF_MONTH, -10);
        assertTrue("Failed to roll DAY_OF_MONTH down by 10", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 29, 2, 5,
                        0)));
    }

    /**
     * @tests java.util.GregorianCalendar#roll(int, boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "roll",
        args = {int.class, boolean.class}
    )
    public void test_rollIZ() {
        // Test for method void java.util.GregorianCalendar.roll(int, boolean)
        GregorianCalendar gc = new GregorianCalendar(1972, Calendar.OCTOBER,
                13, 19, 9, 59);
        gc.roll(Calendar.DAY_OF_MONTH, false);
        assertTrue("Failed to roll day_of_month down", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 12, 19,
                        9, 59)));
        gc = new GregorianCalendar(1972, Calendar.OCTOBER, 13, 19, 9, 59);
        gc.roll(Calendar.DAY_OF_MONTH, true);
        assertTrue("Failed to roll day_of_month up", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 14, 19,
                        9, 59)));
        gc = new GregorianCalendar(1972, Calendar.OCTOBER, 31, 19, 9, 59);
        gc.roll(Calendar.DAY_OF_MONTH, true);
        assertTrue("Failed to roll day_of_month up", gc
                .equals(new GregorianCalendar(1972, Calendar.OCTOBER, 1, 19, 9,
                        59)));

        GregorianCalendar cal = new GregorianCalendar();
        int result;
        try {
            cal.roll(Calendar.ZONE_OFFSET, true);
            result = 0;
        } catch (IllegalArgumentException e) {
            result = 1;
        }
        assertEquals("ZONE_OFFSET roll", 1, result);
        try {
            cal.roll(Calendar.DST_OFFSET, true);
            result = 0;
        } catch (IllegalArgumentException e) {
            result = 1;
        }
        assertEquals("ZONE_OFFSET roll", 1, result);

        cal.set(2004, Calendar.DECEMBER, 31, 5, 0, 0);
        cal.roll(Calendar.WEEK_OF_YEAR, true);
        assertTrue("Wrong year: " + cal.getTime(),
                cal.get(Calendar.YEAR) == 2004);
        assertTrue("Wrong month: " + cal.getTime(),
                cal.get(Calendar.MONTH) == Calendar.JANUARY);
        assertTrue("Wrong date: " + cal.getTime(), cal.get(Calendar.DATE) == 9);
    }

    /**
     * @tests java.util.GregorianCalendar#setGregorianChange(java.util.Date)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setGregorianChange",
        args = {java.util.Date.class}
    )
    public void test_setGregorianChangeLjava_util_Date() {
        // Test for method void
        // java.util.GregorianCalendar.setGregorianChange(java.util.Date)
        GregorianCalendar gc1 = new GregorianCalendar(1582, Calendar.OCTOBER,
                4, 0, 0);
        GregorianCalendar gc2 = new GregorianCalendar(1972, Calendar.OCTOBER,
                13, 0, 0);
        gc1.setGregorianChange(gc2.getTime());
        assertTrue("Returned incorrect value", gc2.getTime().equals(
                gc1.getGregorianChange()));
    }

    /**
     * @tests java.util.GregorianCalendar#clone()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public void test_clone() {
        
        // Regression for HARMONY-498
        GregorianCalendar gCalend = new GregorianCalendar();

        gCalend.set(Calendar.MILLISECOND, 0);
        int dayOfMonth = gCalend.get(Calendar.DAY_OF_MONTH);

        // create clone object and change date
        GregorianCalendar gCalendClone = (GregorianCalendar) gCalend.clone();
        gCalendClone.add(Calendar.DATE, 1);
        
        assertEquals("Before", dayOfMonth, gCalend.get(Calendar.DAY_OF_MONTH));
        gCalend.set(Calendar.MILLISECOND, 0);//changes nothing
        assertEquals("After", dayOfMonth, gCalend.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * @tests java.util.GregorianCalendar#getMinimalDaysInFirstWeek()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMinimalDaysInFirstWeek",
        args = {}
    )
    @KnownFailure("Some difference in timezones and/or locales data"
            + "Who is right, the CLDR or the RI?")
    public void test_getMinimalDaysInFirstWeek() {
        // Regression for Harmony-1037
        GregorianCalendar g = new GregorianCalendar(TimeZone
                .getTimeZone("Paris/France"), new Locale("en", "GB"));
        int minimalDaysInFirstWeek = g.getMinimalDaysInFirstWeek();
        assertEquals(4, minimalDaysInFirstWeek);

        g = new GregorianCalendar(TimeZone.getTimeZone("Paris/France"),
                new Locale("fr", "FR"));
        minimalDaysInFirstWeek = g.getMinimalDaysInFirstWeek();
        assertEquals(4, minimalDaysInFirstWeek);
        
        g = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"),
                new Locale("fr", "CA"));
        minimalDaysInFirstWeek = g.getMinimalDaysInFirstWeek();
        assertEquals(1, minimalDaysInFirstWeek);
    }

    /**
     * @tests java.util.GregorianCalendar#computeTime()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Checks computeTime indirectly.",
        method = "computeTime",
        args = {}
    )
    public void test_computeTime() {
    
        // Regression for Harmony-493
        GregorianCalendar g = new GregorianCalendar(
            TimeZone.getTimeZone("Europe/London"),
            new Locale("en", "GB")
        );
        g.clear();
        g.set(2006, 02, 26, 01, 50, 00);
        assertEquals(1143337800000L, g.getTimeInMillis());

        GregorianCalendar g1 = new GregorianCalendar(
            TimeZone.getTimeZone("Europe/Moscow")
        );
        g1.clear();
        g1.set(2006, 02, 26, 02, 20, 00); // in the DST transition interval
        assertEquals(1143328800000L, g1.getTimeInMillis());
        assertEquals(3, g1.get(Calendar.HOUR_OF_DAY));
        g1.clear();
        g1.set(2006, 9, 29, 02, 50, 00); // transition from DST
        assertEquals(1162079400000L, g1.getTimeInMillis());
        assertEquals(2, g1.get(Calendar.HOUR_OF_DAY));
        // End of regression test

        g1.set(2006, -9, 29, 02, 50, 00); // transition from DST
        g1.setLenient(false);

        try {
            g1.getTimeInMillis();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Checks computeFields indirectly.",
        method = "computeFields",
        args = {}
    )
    public void test_computeFields() {
        GregorianCalendar g = new GregorianCalendar(
            TimeZone.getTimeZone("Europe/London"),
            new Locale("en", "GB")
        );
        g.clear();
        g.setTimeInMillis(1222185600225L);
        assertEquals(1,                  g.get(Calendar.ERA));
        assertEquals(2008,               g.get(Calendar.YEAR));
        assertEquals(Calendar.SEPTEMBER, g.get(Calendar.MONTH));
        assertEquals(23,                 g.get(Calendar.DAY_OF_MONTH));
        assertEquals(17,                 g.get(Calendar.HOUR_OF_DAY));
        assertEquals(0,                  g.get(Calendar.MINUTE));
    }
    
    /**
     * @tests java.util.GregorianCalendar#get(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify ArrayIndexOutOfBoundsException.",
        method = "get",
        args = {int.class}
    )
    @SuppressWarnings("deprecation")
    public void test_getI() { 
        // Regression test for HARMONY-2959
        Date date = new Date(Date.parse("Jan 15 00:00:01 GMT 2000")); 
        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
        gc.setGregorianChange(date); 
        gc.setTimeInMillis(Date.parse("Dec 24 00:00:01 GMT 2000")); 
        assertEquals(346, gc.get(Calendar.DAY_OF_YEAR)); 
        
        // Regression test for HARMONY-3003
        date = new Date(Date.parse("Feb 28 00:00:01 GMT 2000"));
        gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        gc.setGregorianChange(date);
        gc.setTimeInMillis(Date.parse("Dec 1 00:00:01 GMT 2000"));
        assertEquals(1, gc.get(Calendar.DAY_OF_MONTH));
        assertEquals(11, gc.get(Calendar.MONTH));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hashCode",
        args = {}
    )
    public void test_hashCode() {
        GregorianCalendar g = new GregorianCalendar(
                TimeZone.getTimeZone("Europe/London"),
                new Locale("en", "GB")
            );
            g.clear();
            g.setTimeInMillis(1222185600225L);

            GregorianCalendar g1 = new GregorianCalendar(
                    TimeZone.getTimeZone("Europe/Moscow"));
            g1.clear();
            g1.set(2008, Calendar.SEPTEMBER, 23, 18, 0, 0);
            assertNotSame(g.hashCode(), g1.hashCode());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setFirstDayOfWeek",
        args = {int.class}
    )
    public void test_setFirstDayOfWeekI() {
        GregorianCalendar g = new GregorianCalendar(
                TimeZone.getTimeZone("Europe/London"),
                new Locale("en", "GB"));
        
        for (int i = 0; i < 10; i++) {
            g.setFirstDayOfWeek(i);
            assertEquals(i, g.getFirstDayOfWeek());
        }
        g.setLenient(false);
        g.setFirstDayOfWeek(10);
        g.setFirstDayOfWeek(-10);
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setMinimalDaysInFirstWeek",
        args = {int.class}
    )
    public void test_setMinimalDaysInFirstWeekI() {
        GregorianCalendar g = new GregorianCalendar(
                TimeZone.getTimeZone("Europe/London"),
                new Locale("en", "GB"));
        
        for (int i = 0; i < 10; i++) {
            g.setMinimalDaysInFirstWeek(i);
            assertEquals(i, g.getMinimalDaysInFirstWeek());
        }
        g.setLenient(false);
        g.setMinimalDaysInFirstWeek(10);
        g.setMinimalDaysInFirstWeek(-10);
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        Locale.setDefault(Locale.US);
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
    }
}
