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

package org.apache.harmony.logging.tests.java.util.logging;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import junit.framework.TestCase;

import org.apache.harmony.logging.tests.java.util.logging.util.EnvironmentHelper;

@TestTargetClass(Formatter.class) 
public class FormatterTest extends TestCase {
    Formatter f;

    LogRecord r;
    
    FileHandler h;

    static String MSG = "msg, pls. ignore it";

    static LogManager manager = LogManager.getLogManager();

    final static Properties props = new Properties();

    final static String className = FormatterTest.class.getName();

    final static String TEMPPATH = System.getProperty("java.io.tmpdir");

    final static String SEP = File.separator;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager.reset();
        
        //initProp
        props.clear();
        props.put("java.util.logging.FileHandler.level", "FINE");
        props.put("java.util.logging.FileHandler.filter", className
                + "$MockFilter");
        props.put("java.util.logging.FileHandler.formatter", className
                + "$MockFormatter");
        props.put("java.util.logging.FileHandler.encoding", "iso-8859-1");
        // limit to only two message
        props.put("java.util.logging.FileHandler.limit", "1000");
        // rotation count is 2
        props.put("java.util.logging.FileHandler.count", "2");
        // using append mode
        props.put("java.util.logging.FileHandler.append", "true");
        props.put("java.util.logging.FileHandler.pattern",
                        "%t/log/java%u.test");
        
        File file = new File(TEMPPATH + SEP + "log");
        file.mkdir();
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));

        f = new MockFormatter();
        r = new LogRecord(Level.FINE, MSG);
        h = new FileHandler();
    }

    /*
     * test for constructor protected Formatter()
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "Formatter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getHead",
            args = {java.util.logging.Handler.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getTail",
            args = {java.util.logging.Handler.class}
        )
    })
    public void testFormatter() {
        assertEquals("head string is not empty", "", f.getHead(null));
        assertEquals("tail string is not empty", "", f.getTail(null));

    }

    /*
     * test for method public String getHead(Handler h)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getHead",
        args = {Handler.class}
    )
    public void testGetHead() {
        assertEquals("head string is not empty", "", f.getHead(null));
        assertEquals("head string is not empty", "", f.getHead(h));
        h.publish(r);
        assertEquals("head string is not empty", "", f.getHead(h));
    }

    /*
     * test for method public String getTail(Handler h)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getTail",
        args = {Handler.class}
    )
    public void testGetTail() {
        assertEquals("tail string is not empty", "", f.getTail(null));
        assertEquals("tail string is not empty", "", f.getTail(h));
        h.publish(r);
        assertEquals("tail string is not empty", "", f.getTail(h));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "formatMessage",
        args = {LogRecord.class}
    )
    @AndroidOnly("The RI fails in this test because it uses a MessageFormat " +
            "to format the message even though it doesn't contain \"{0\". " +
            "The spec says that this would indicate that a MessageFormat " +
            "should be used and else no formatting should be done.")
    public void testFormatMessage() {
        assertEquals(MSG, f.formatMessage(r));

        String pattern = "test formatter {0, number}";
        r.setMessage(pattern);
        assertEquals(pattern, f.formatMessage(r));

        Object[] oa = new Object[0];
        r.setParameters(oa);
        assertEquals(pattern, f.formatMessage(r));

        oa = new Object[] { new Integer(100), new Float(1.2), new Float(2.2) };
        r.setParameters(oa);
        assertEquals(MessageFormat.format(pattern, oa), f.formatMessage(r));

        r.setMessage(MSG);
        assertEquals(MSG, f.formatMessage(r));

        pattern = "wrong pattern {0, asdfasfd}";
        r.setMessage(pattern);
        assertEquals(pattern, f.formatMessage(r));

        pattern = null;
        r.setMessage(pattern);
        assertNull(f.formatMessage(r));

        // The RI fails in this test because it uses a MessageFormat to format
        // the message even though it doesn't contain "{0". The spec says that
        // this would indicate that a MessageFormat should be used and else no
        // formatting should be done.
        pattern = "pattern without 0 {1, number}";
        r.setMessage(pattern);
        assertEquals(pattern, f.formatMessage(r));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "formatMessage",
        args = {LogRecord.class}
    )
    public void testLocalizedFormatMessage() {
        // normal case
        r.setMessage("msg");
        ResourceBundle rb = ResourceBundle
                .getBundle("bundles/java/util/logging/res");
        r.setResourceBundle(rb);
        assertEquals(rb.getString("msg"), f.formatMessage(r));

        // local message is a pattern
        r.setMessage("pattern");
        Object[] oa = new Object[] { new Integer(3) };
        r.setParameters(oa);
        assertEquals(MessageFormat.format(rb.getString("pattern"), oa), f
                .formatMessage(r));

        // key is a pattern, but local message is not
        r.setMessage("pattern{0,number}");
        oa = new Object[] { new Integer(3) };
        r.setParameters(oa);
        assertEquals(rb.getString("pattern{0,number}"), f.formatMessage(r));

        // another bundle
        rb = ResourceBundle.getBundle("bundles/java/util/logging/res",
                Locale.US);
        r.setMessage("msg");
        r.setResourceBundle(rb);
        assertEquals(rb.getString("msg"), f.formatMessage(r));

        // cannot find local message in bundle
        r.setMessage("msg without locale");
        assertEquals("msg without locale", f.formatMessage(r));

        // set bundle name but not bundle
        r.setResourceBundle(null);
        r.setResourceBundleName("bundles/java/util/logging/res");
        r.setMessage("msg");
        assertEquals("msg", f.formatMessage(r));
    }

    public static class MockFormatter extends Formatter {

        public String format(LogRecord arg0) {
            return "format";
        }
    }
}
