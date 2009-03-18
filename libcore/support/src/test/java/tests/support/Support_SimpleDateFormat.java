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

package tests.support;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormat.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

public class Support_SimpleDateFormat extends Support_Format {

    public Support_SimpleDateFormat(String p1) {
        super(p1);
    }

    @Override
    public void runTest() {
        t_formatToCharacterIterator();
        t_format_with_FieldPosition();
    }

    public static void main(String[] args) {
        new Support_SimpleDateFormat("").runTest();
    }

    public void t_format_with_FieldPosition() {
        TimeZone tz = TimeZone.getTimeZone("EST");
        Calendar cal = new GregorianCalendar(tz);
        cal.set(1999, Calendar.SEPTEMBER, 13, 17, 19, 01);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        SimpleDateFormat format = (SimpleDateFormat) DateFormat
                .getDateInstance(DateFormat.DEFAULT, Locale.US);
        format.setTimeZone(tz);

        // test with all pattern chars, and multiple occurances
        format
                .applyPattern("G GGGG y yy yyyy M MM MMM MMMM d dd ddd k kk kkk H HH HHH h hh hhh m mmm s ss sss S SS SSS EE EEEE D DD DDD F FF w www W WWW a  aaa  K KKK z zzzz Z ZZZZ");

        StringBuffer textbuffer = new StringBuffer(
                "AD AD 99 99 1999 9 09 Sep September 13 13 013 17 17 017 17 17 017 5 05");
        textbuffer
                .append(" 005 19 019 1 01 001 0 00 000 Mon Monday 256 256 256 2 02 38 038 3 003 PM");
        textbuffer.append("  PM  5 005 EDT Eastern Daylight Time -0400 -0400");

        // to avoid passing the huge Stringbuffer each time.
        super.text = textbuffer.toString();

        // test if field positions are set correctly for these fields occuring
        // multiple times.
        t_FormatWithField(0, format, date, null, Field.ERA, 0, 2);
        t_FormatWithField(1, format, date, null, Field.YEAR, 6, 8);
        t_FormatWithField(2, format, date, null, Field.MONTH, 17, 18);
        t_FormatWithField(3, format, date, null, Field.DAY_OF_MONTH, 36, 38);
        t_FormatWithField(4, format, date, null, Field.HOUR_OF_DAY1, 46, 48);
        t_FormatWithField(5, format, date, null, Field.HOUR_OF_DAY0, 56, 58);
        t_FormatWithField(6, format, date, null, Field.HOUR1, 66, 67);
        t_FormatWithField(7, format, date, null, Field.MINUTE, 75, 77);
        t_FormatWithField(8, format, date, null, Field.SECOND, 82, 83);
        t_FormatWithField(9, format, date, null, Field.MILLISECOND, 91, 92);
        t_FormatWithField(10, format, date, null, Field.DAY_OF_WEEK, 100, 103);
        t_FormatWithField(11, format, date, null, Field.DAY_OF_YEAR, 111, 114);
        t_FormatWithField(12, format, date, null, Field.DAY_OF_WEEK_IN_MONTH,
                123, 124);
        t_FormatWithField(13, format, date, null, Field.WEEK_OF_YEAR, 128, 130);
        t_FormatWithField(14, format, date, null, Field.WEEK_OF_MONTH, 135, 136);
        t_FormatWithField(15, format, date, null, Field.AM_PM, 141, 143);
        t_FormatWithField(16, format, date, null, Field.HOUR0, 149, 150);
        t_FormatWithField(17, format, date, null, Field.TIME_ZONE, 155, 158);

        // test fields that are not included in the formatted text
        t_FormatWithField(18, format, date, null,
                NumberFormat.Field.EXPONENT_SIGN, 0, 0);

        // test with simple example
        format.applyPattern("h:m z");

        super.text = "5:19 GMT-05:00";
        t_FormatWithField(21, format, date, null, Field.HOUR1, 0, 1);
        t_FormatWithField(22, format, date, null, Field.MINUTE, 2, 4);
        t_FormatWithField(23, format, date, null, Field.TIME_ZONE, 5, 14);

        // test fields that are not included in the formatted text

        t_FormatWithField(24, format, date, null, Field.ERA, 0, 0);
        t_FormatWithField(25, format, date, null, Field.YEAR, 0, 0);
        t_FormatWithField(26, format, date, null, Field.MONTH, 0, 0);
        t_FormatWithField(27, format, date, null, Field.DAY_OF_MONTH, 0, 0);
        t_FormatWithField(28, format, date, null, Field.HOUR_OF_DAY1, 0, 0);
        t_FormatWithField(29, format, date, null, Field.HOUR_OF_DAY0, 0, 0);
        t_FormatWithField(30, format, date, null, Field.SECOND, 0, 0);
        t_FormatWithField(31, format, date, null, Field.MILLISECOND, 0, 0);
        t_FormatWithField(32, format, date, null, Field.DAY_OF_WEEK, 0, 0);
        t_FormatWithField(33, format, date, null, Field.DAY_OF_YEAR, 0, 0);
        t_FormatWithField(34, format, date, null, Field.DAY_OF_WEEK_IN_MONTH,
                0, 0);
        t_FormatWithField(35, format, date, null, Field.WEEK_OF_YEAR, 0, 0);
        t_FormatWithField(36, format, date, null, Field.WEEK_OF_MONTH, 0, 0);
        t_FormatWithField(37, format, date, null, Field.AM_PM, 0, 0);
        t_FormatWithField(38, format, date, null, Field.HOUR0, 0, 0);

        t_FormatWithField(39, format, date, null, NumberFormat.Field.EXPONENT,
                0, 0);

        // test with simple example with pattern char Z
        format.applyPattern("h:m Z z");
        super.text = "5:19 -0400 GMT-05:00";
        t_FormatWithField(40, format, date, null, Field.HOUR1, 0, 1);
        t_FormatWithField(41, format, date, null, Field.MINUTE, 2, 4);
        t_FormatWithField(42, format, date, null, Field.TIME_ZONE, 5, 10);
    }

    public void t_formatToCharacterIterator() {
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        Calendar cal = new GregorianCalendar(tz);
        cal.set(1999, Calendar.SEPTEMBER, 13, 17, 19, 01);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        SimpleDateFormat format = (SimpleDateFormat) DateFormat
                .getDateInstance(DateFormat.DEFAULT, Locale.US);
        format.setTimeZone(tz);

        format.applyPattern("yyyyMMddHHmmss");
        t_Format(1, date, format, getDateVector1());

        format.applyPattern("w W dd MMMM yyyy EEEE");
        t_Format(2, date, format, getDateVector2());

        format.applyPattern("h:m z");
        t_Format(3, date, format, getDateVector3());

        format.applyPattern("h:m Z");
        t_Format(5, date, format, getDateVector5());

        // with all pattern chars, and multiple occurances
        format
                .applyPattern("G GGGG y yy yyyy M MM MMM MMMM d dd ddd k kk kkk H HH HHH h hh hhh m mmm s ss sss S SS SSS EE EEEE D DD DDD F FF w www W WWW a  aaa  K KKK z zzzz Z ZZZZ");
        t_Format(4, date, format, getDateVector4());
    }

    private Vector<FieldContainer> getDateVector1() {
        // "19990913171901"
        Vector<FieldContainer> v = new Vector<FieldContainer>();
        v.add(new FieldContainer(0, 4, Field.YEAR));
        v.add(new FieldContainer(4, 6, Field.MONTH));
        v.add(new FieldContainer(6, 8, Field.DAY_OF_MONTH));
        v.add(new FieldContainer(8, 10, Field.HOUR_OF_DAY0));
        v.add(new FieldContainer(10, 12, Field.MINUTE));
        v.add(new FieldContainer(12, 14, Field.SECOND));
        return v;
    }

    private Vector<FieldContainer> getDateVector2() {
        // "12 3 5 March 2002 Monday"
        Vector<FieldContainer> v = new Vector<FieldContainer>();
        v.add(new FieldContainer(0, 2, Field.WEEK_OF_YEAR));
        v.add(new FieldContainer(3, 4, Field.WEEK_OF_MONTH));
        v.add(new FieldContainer(5, 7, Field.DAY_OF_MONTH));
        v.add(new FieldContainer(8, 17, Field.MONTH));
        v.add(new FieldContainer(18, 22, Field.YEAR));
        v.add(new FieldContainer(23, 29, Field.DAY_OF_WEEK));
        return v;
    }

    private Vector<FieldContainer> getDateVector3() {
        // "5:19 GMT-05:00"
        Vector<FieldContainer> v = new Vector<FieldContainer>();
        v.add(new FieldContainer(0, 1, Field.HOUR1));
        v.add(new FieldContainer(2, 4, Field.MINUTE));
        v.add(new FieldContainer(5, 14, Field.TIME_ZONE));
        return v;
    }

    private Vector<FieldContainer> getDateVector5() {
        // "5:19 -0400"
        Vector<FieldContainer> v = new Vector<FieldContainer>();
        v.add(new FieldContainer(0, 1, Field.HOUR1));
        v.add(new FieldContainer(2, 4, Field.MINUTE));
        v.add(new FieldContainer(5, 10, Field.TIME_ZONE));
        return v;
    }

    private Vector<FieldContainer> getDateVector4() {
        Vector<FieldContainer> v = new Vector<FieldContainer>();

        // "AD AD 99 99 1999 9 09 Sep September 13 13 013 17 17 017 17 17 017 5
        // 05
        // 005 19 019 1 01 001 0 00 000 Mon Monday 256 256 256 2 02 38 038 3 003
        // PM
        // PM 5 005 GMT-05:00 GMT-05:00 -0500 -0500"
        v.add(new FieldContainer(0, 2, Field.ERA));
        v.add(new FieldContainer(3, 5, Field.ERA));
        v.add(new FieldContainer(6, 8, Field.YEAR));
        v.add(new FieldContainer(9, 11, Field.YEAR));
        v.add(new FieldContainer(12, 16, Field.YEAR));
        v.add(new FieldContainer(17, 18, Field.MONTH));
        v.add(new FieldContainer(19, 21, Field.MONTH));
        v.add(new FieldContainer(22, 25, Field.MONTH));
        v.add(new FieldContainer(26, 35, Field.MONTH));
        v.add(new FieldContainer(36, 38, Field.DAY_OF_MONTH));
        v.add(new FieldContainer(39, 41, Field.DAY_OF_MONTH));
        v.add(new FieldContainer(42, 45, Field.DAY_OF_MONTH));
        v.add(new FieldContainer(46, 48, Field.HOUR_OF_DAY1));
        v.add(new FieldContainer(49, 51, Field.HOUR_OF_DAY1));
        v.add(new FieldContainer(52, 55, Field.HOUR_OF_DAY1));
        v.add(new FieldContainer(56, 58, Field.HOUR_OF_DAY0));
        v.add(new FieldContainer(59, 61, Field.HOUR_OF_DAY0));
        v.add(new FieldContainer(62, 65, Field.HOUR_OF_DAY0));
        v.add(new FieldContainer(66, 67, Field.HOUR1));
        v.add(new FieldContainer(68, 70, Field.HOUR1));
        v.add(new FieldContainer(71, 74, Field.HOUR1));
        v.add(new FieldContainer(75, 77, Field.MINUTE));
        v.add(new FieldContainer(78, 81, Field.MINUTE));
        v.add(new FieldContainer(82, 83, Field.SECOND));
        v.add(new FieldContainer(84, 86, Field.SECOND));
        v.add(new FieldContainer(87, 90, Field.SECOND));
        v.add(new FieldContainer(91, 92, Field.MILLISECOND));
        v.add(new FieldContainer(93, 95, Field.MILLISECOND));
        v.add(new FieldContainer(96, 99, Field.MILLISECOND));
        v.add(new FieldContainer(100, 103, Field.DAY_OF_WEEK));
        v.add(new FieldContainer(104, 110, Field.DAY_OF_WEEK));
        v.add(new FieldContainer(111, 114, Field.DAY_OF_YEAR));
        v.add(new FieldContainer(115, 118, Field.DAY_OF_YEAR));
        v.add(new FieldContainer(119, 122, Field.DAY_OF_YEAR));
        v.add(new FieldContainer(123, 124, Field.DAY_OF_WEEK_IN_MONTH));
        v.add(new FieldContainer(125, 127, Field.DAY_OF_WEEK_IN_MONTH));
        v.add(new FieldContainer(128, 130, Field.WEEK_OF_YEAR));
        v.add(new FieldContainer(131, 134, Field.WEEK_OF_YEAR));
        v.add(new FieldContainer(135, 136, Field.WEEK_OF_MONTH));
        v.add(new FieldContainer(137, 140, Field.WEEK_OF_MONTH));
        v.add(new FieldContainer(141, 143, Field.AM_PM));
        v.add(new FieldContainer(145, 147, Field.AM_PM));
        v.add(new FieldContainer(149, 150, Field.HOUR0));
        v.add(new FieldContainer(151, 154, Field.HOUR0));
        v.add(new FieldContainer(155, 164, Field.TIME_ZONE));
        v.add(new FieldContainer(165, 174, Field.TIME_ZONE));
        v.add(new FieldContainer(175, 180, Field.TIME_ZONE));
        v.add(new FieldContainer(181, 186, Field.TIME_ZONE));
        return v;
    }

}
