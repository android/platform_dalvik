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
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.KnownFailure;
import dalvik.annotation.AndroidOnly;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import tests.support.Support_TimeZone;

@TestTargetClass(TimeZone.class) 
public class TimeZoneTest extends junit.framework.TestCase {

    private static final int ONE_HOUR = 3600000;

    /**
     * @tests java.util.TimeZone#getDefault()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDefault",
        args = {}
    )
    public void test_getDefault() {
        assertNotSame("returns identical",
                              TimeZone.getDefault(), TimeZone.getDefault());
    }

    /**
     * @tests java.util.TimeZone#getDSTSavings()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDSTSavings",
        args = {}
    )
    @KnownFailure("Might be a difference in data.")
    public void test_getDSTSavings() {
        // Test for method int java.util.TimeZone.getDSTSavings()

        // test on subclass SimpleTimeZone
        TimeZone st1 = TimeZone.getTimeZone("America/New_York");
        assertEquals("T1A. Incorrect daylight savings returned",
                             ONE_HOUR, st1.getDSTSavings());

        // a SimpleTimeZone with daylight savings different then 1 hour
        st1 = TimeZone.getTimeZone("Australia/Lord_Howe");
        assertEquals("T1B. Incorrect daylight savings returned",
                             1800000, st1.getDSTSavings());

        // test on subclass Support_TimeZone, an instance with daylight savings
        TimeZone tz1 = new Support_TimeZone(-5 * ONE_HOUR, true);
        assertEquals("T2. Incorrect daylight savings returned",
                             ONE_HOUR, tz1.getDSTSavings());

        // an instance without daylight savings
        tz1 = new Support_TimeZone(3 * ONE_HOUR, false);
        assertEquals("T3. Incorrect daylight savings returned, ",
                             0, tz1.getDSTSavings());
    }

    /**
     * @tests java.util.TimeZone#getOffset(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getOffset",
        args = {long.class}
    )
    public void test_getOffset_long() {
        // Test for method int java.util.TimeZone.getOffset(long time)

        // test on subclass SimpleTimeZone
        TimeZone st1 = TimeZone.getTimeZone("EST");
        long time1 = new GregorianCalendar(1998, Calendar.NOVEMBER, 11)
                .getTimeInMillis();
        assertEquals("T1. Incorrect offset returned",
                             -(5 * ONE_HOUR), st1.getOffset(time1));

        long time2 = new GregorianCalendar(1998, Calendar.JUNE, 11)
                .getTimeInMillis();
//      Not working as expected on RI.
//        st1 = TimeZone.getTimeZone("EST");
//        assertEquals("T2. Incorrect offset returned",
//                             -(4 * ONE_HOUR), st1.getOffset(time2));

        // test on subclass Support_TimeZone, an instance with daylight savings
        TimeZone tz1 = new Support_TimeZone(-5 * ONE_HOUR, true);
        assertEquals("T3. Incorrect offset returned, ",
                             -(5 * ONE_HOUR), tz1.getOffset(time1));
        assertEquals("T4. Incorrect offset returned, ",
                             -(4 * ONE_HOUR), tz1.getOffset(time2));

        // an instance without daylight savings
        tz1 = new Support_TimeZone(3 * ONE_HOUR, false);
        assertEquals("T5. Incorrect offset returned, ",
                             (3 * ONE_HOUR), tz1.getOffset(time1));
        assertEquals("T6. Incorrect offset returned, ",
                             (3 * ONE_HOUR), tz1.getOffset(time2));
    }

    /**
     * @tests java.util.TimeZone#getTimeZone(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getTimeZone",
        args = {java.lang.String.class}
    )
    @KnownFailure("Android fails the last test.")
    public void test_getTimeZoneLjava_lang_String() {
        assertEquals("Must return GMT when given an invalid TimeZone id SMT-8.",
                             "GMT", TimeZone.getTimeZone("SMT-8").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28:70.",
                             "GMT", TimeZone.getTimeZone("GMT+28:70").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28:30.",
                             "GMT", TimeZone.getTimeZone("GMT+28:30").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+8:70.",
                             "GMT", TimeZone.getTimeZone("GMT+8:70").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+3:.",
                             "GMT", TimeZone.getTimeZone("GMT+3:").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+3:0.",
                             "GMT", TimeZone.getTimeZone("GMT+3:0").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+2360.",
                             "GMT", TimeZone.getTimeZone("GMT+2360").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+892.",
                             "GMT", TimeZone.getTimeZone("GMT+892").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+082.",
                             "GMT", TimeZone.getTimeZone("GMT+082").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28.",
                             "GMT", TimeZone.getTimeZone("GMT+28").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+30.",
                             "GMT", TimeZone.getTimeZone("GMT+30").getID());
        assertEquals("Must return GMT when given TimeZone GMT.",
                             "GMT", TimeZone.getTimeZone("GMT").getID());
        assertEquals("Must return GMT when given TimeZone GMT+.",
                             "GMT", TimeZone.getTimeZone("GMT+").getID());
        assertEquals("Must return GMT when given TimeZone GMT-.",
                             "GMT", TimeZone.getTimeZone("GMT-").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT-8.45.",
                             "GMT", TimeZone.getTimeZone("GMT-8.45").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT-123:23.",
                             "GMT", TimeZone.getTimeZone("GMT-123:23").getID());
        assertEquals("Must return proper GMT formatted string for GMT+8:30 (eg. GMT+08:20).",
                             "GMT+08:30", TimeZone.getTimeZone("GMT+8:30").getID());
        assertEquals("Must return proper GMT formatted string for GMT+3 (eg. GMT+08:20).",
                             "GMT+03:00", TimeZone.getTimeZone("GMT+3").getID());
        assertEquals("Must return proper GMT formatted string for GMT+3:02 (eg. GMT+08:20).",
                             "GMT+03:02", TimeZone.getTimeZone("GMT+3:02").getID());
        assertEquals("Must return proper GMT formatted string for GMT+2359 (eg. GMT+08:20).",
                             "GMT+23:59", TimeZone.getTimeZone("GMT+2359").getID());
        assertEquals("Must return proper GMT formatted string for GMT+520 (eg. GMT+08:20).",
                             "GMT+05:20", TimeZone.getTimeZone("GMT+520").getID());
        assertEquals("Must return proper GMT formatted string for GMT+052 (eg. GMT+08:20).",
                             "GMT+00:52", TimeZone.getTimeZone("GMT+052").getID());
        assertEquals("Must return proper GMT formatted string for GMT-0 (eg. GMT+08:20).",
                             "GMT-00:00", TimeZone.getTimeZone("GMT-0").getID());
    }

    /**
     * @tests java.util.TimeZone#setDefault(java.util.TimeZone)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDefault",
        args = {java.util.TimeZone.class}
    )
    public void test_setDefaultLjava_util_TimeZone() {
        TimeZone oldDefault = TimeZone.getDefault();
        TimeZone zone = new SimpleTimeZone(45, "TEST");
        TimeZone.setDefault(zone);
        assertEquals("timezone not set", zone, TimeZone.getDefault());
        TimeZone.setDefault(null);
        assertEquals("default not restored",
                             oldDefault, TimeZone.getDefault());
    }
    
    class Mock_TimeZone extends TimeZone {
        @Override
        public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
            return 0;
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
            
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "TimeZone",
        args = {}
    )
    public void test_constructor() {
        assertNotNull(new Mock_TimeZone());
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public void test_clone() {
        TimeZone tz1 = TimeZone.getDefault();
        TimeZone tz2 = (TimeZone)tz1.clone();
        
        assertTrue(tz1.equals(tz2));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAvailableIDs",
        args = {}
    )
    public void test_getAvailableIDs() {
        String[] str = TimeZone.getAvailableIDs();
        assertNotNull(str);
        assertTrue(str.length != 0);
        for(int i = 0; i < str.length; i++) {
            assertNotNull(TimeZone.getTimeZone(str[i]));
        }
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAvailableIDs",
        args = {int.class}
    )
    public void test_getAvailableIDsI() {
        String[] str = TimeZone.getAvailableIDs(0);
        assertNotNull(str);
        assertTrue(str.length != 0);
        for(int i = 0; i < str.length; i++) {
            assertNotNull(TimeZone.getTimeZone(str[i]));
        }
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDisplayName",
        args = {}
    )
    public void test_getDisplayName() {
        TimeZone tz = TimeZone.getTimeZone("GMT-6");
        assertEquals("GMT-06:00", tz.getDisplayName());
        tz = TimeZone.getTimeZone("America/Los_Angeles");
        assertEquals("Pacific Standard Time", tz.getDisplayName());
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDisplayName",
        args = {java.util.Locale.class}
    )
    public void test_getDisplayNameLjava_util_Locale() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        assertEquals("Pacific Standard Time", tz.getDisplayName(new Locale("US")));
        assertEquals("Heure normale du Pacifique", tz.getDisplayName(Locale.FRANCE));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDisplayName",
        args = {boolean.class, int.class}
    )
    public void test_getDisplayNameZI() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        assertEquals("PST",                   tz.getDisplayName(false, 0));
        assertEquals("Pacific Daylight Time", tz.getDisplayName(true, 1));
        assertEquals("Pacific Standard Time", tz.getDisplayName(false, 1));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDisplayName",
        args = {boolean.class, int.class, java.util.Locale.class}
    )
    @AndroidOnly("fail on RI. See comment below")
    public void test_getDisplayNameZILjava_util_Locale() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        assertEquals("PST",                   tz.getDisplayName(false, 0, Locale.US));
        assertEquals("Pacific Daylight Time", tz.getDisplayName(true,  1, Locale.US));
        assertEquals("Pacific Standard Time", tz.getDisplayName(false, 1, Locale.UK));
        //RI fails on following line. RI always returns short time zone name as "PST" 
        assertEquals("HMG-08:00",             tz.getDisplayName(false, 0, Locale.FRANCE));
        assertEquals("Heure avanc\u00e9e du Pacifique", tz.getDisplayName(true,  1, Locale.FRANCE));
        assertEquals("Heure normale du Pacifique", tz.getDisplayName(false, 1, Locale.FRANCE));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getID",
        args = {}
    )
    public void test_getID() {
        TimeZone tz = TimeZone.getTimeZone("GMT-6");
        assertEquals("GMT-06:00", tz.getID());
        tz = TimeZone.getTimeZone("America/Denver");
        assertEquals("America/Denver", tz.getID());
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hasSameRules",
        args = {java.util.TimeZone.class}
    )
    @KnownFailure("Arizona doesn't observe DST")
    public void test_hasSameRulesLjava_util_TimeZone() {
        TimeZone tz1 = TimeZone.getTimeZone("America/Denver");
        TimeZone tz2 = TimeZone.getTimeZone("America/Phoenix");
        assertEquals(tz1.getDisplayName(false, 0), tz2.getDisplayName(false, 0));
        // Arizona doesn't observe DST. See http://phoenix.about.com/cs/weather/qt/timezone.htm
        assertFalse(tz1.hasSameRules(tz2));
        assertFalse(tz1.hasSameRules(null));
        tz1 = TimeZone.getTimeZone("America/Montreal");
        tz2 = TimeZone.getTimeZone("America/New_York");
        assertEquals(tz1.getDisplayName(), tz2.getDisplayName());
        assertFalse(tz1.getID().equals(tz2.getID()));
        assertTrue(tz2.hasSameRules(tz1));
        assertTrue(tz1.hasSameRules(tz1));
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setID",
        args = {java.lang.String.class}
    )
    public void test_setIDLjava_lang_String() {
        TimeZone tz = TimeZone.getTimeZone("GMT-6");
        assertEquals("GMT-06:00", tz.getID());
        tz.setID("New ID for GMT-6");
        assertEquals("New ID for GMT-6", tz.getID());
    }
    
    Locale loc = null;

    protected void setUp() {
        loc = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    protected void tearDown() {
        Locale.setDefault(loc);
    }
}
