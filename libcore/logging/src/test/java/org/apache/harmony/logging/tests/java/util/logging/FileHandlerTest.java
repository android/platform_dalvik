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
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.security.Permission;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.LoggingPermission;
import java.util.logging.XMLFormatter;

import junit.framework.TestCase;

import org.apache.harmony.logging.tests.java.util.logging.HandlerTest.NullOutputStream;
import org.apache.harmony.logging.tests.java.util.logging.util.EnvironmentHelper;

/**
 */
@TestTargetClass(FileHandler.class) 
public class FileHandlerTest extends TestCase {

    static LogManager manager = LogManager.getLogManager();

    final static Properties props = new Properties();

    final static String className = FileHandlerTest.class.getName();

    final static StringWriter writer = new StringWriter();

    final static SecurityManager securityManager = new MockLogSecurityManager();

    final static String SEP = File.separator;

    private String oldHomePath = System.getProperty("user.home");
    
    // The HOMEPATH can't be used in android.
    final static String HOMEPATH = System.getProperty("java.io.tmpdir") + SEP + "home";

    final static String TEMPPATH = System.getProperty("java.io.tmpdir");

    private final PrintStream err = System.err;

    private OutputStream errSubstituteStream = null;

    FileHandler handler;

    LogRecord r;

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
        
        File home = new File(HOMEPATH);
        if (!home.exists()) {
            home.mkdirs();
        } else if (!home.isDirectory()) {
            home.delete();
            home.mkdirs();
        }
        if(!home.isDirectory()) {
            fail("unable to create temp path");
        }
        System.setProperty("user.home", HOMEPATH);
        
        File file = new File(TEMPPATH + SEP + "log");
        file.mkdir();
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        r = new LogRecord(Level.CONFIG, "msg");
        errSubstituteStream = new NullOutputStream();
        System.setErr(new PrintStream(errSubstituteStream));
    }


    /*
     * @see TestCase#tearDown()
     */
    
    protected void tearDown() throws Exception {
        if (null != handler) {
            handler.close();
        }
        reset(TEMPPATH + SEP + "log", "");
        System.setErr(err);
        System.setProperty("user.home", oldHomePath);
        new File(HOMEPATH).delete();
        super.tearDown();
    }

    /*
     * test for constructor void FileHandler()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify exceptions.",
        method = "FileHandler",
        args = {}
    )
    public void testFileHandler() throws Exception {
        assertEquals("character encoding is non equal to actual value",
                "iso-8859-1", handler.getEncoding());
        assertNotNull("Filter is null", handler.getFilter());
        assertNotNull("Formatter is null", handler.getFormatter());
        assertEquals("is non equal to actual value", Level.FINE, handler
                .getLevel());
        assertNotNull("ErrorManager is null", handler.getErrorManager());
        handler.publish(r);
        handler.close();
        // output 3 times, and all records left
        // append mode is true
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler();
            handler.publish(r);
            handler.close();
        }
        assertFileContent(TEMPPATH + SEP + "log", "java0.test.0",
                new LogRecord[] { r, null, r, null, r, null, r },
                new MockFormatter());
    }

    /*
     * test for constructor void FileHandler(String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify security exception.",
        method = "FileHandler",
        args = {java.lang.String.class}
    )
    public void testFileHandler_1params() throws Exception {

        handler = new FileHandler("%t/log/string");
        assertEquals("character encoding is non equal to actual value",
                "iso-8859-1", handler.getEncoding());
        assertNotNull("Filter is null", handler.getFilter());
        assertNotNull("Formatter is null", handler.getFormatter());
        assertEquals("is non equal to actual value", Level.FINE, handler
                .getLevel());
        assertNotNull("ErrorManager is null", handler.getErrorManager());
        handler.publish(r);
        handler.close();

        // output 3 times, and all records left
        // append mode is true
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler("%t/log/string");
            handler.publish(r);
            handler.close();
        }
        assertFileContent(TEMPPATH + SEP + "log", "/string", new LogRecord[] {
                r, null, r, null, r, null, r }, new MockFormatter());

        // test if unique ids not specified, it will append at the end
        // no generation number is used
        FileHandler h = new FileHandler("%t/log/string");
        FileHandler h2 = new FileHandler("%t/log/string");
        FileHandler h3 = new FileHandler("%t/log/string");
        FileHandler h4 = new FileHandler("%t/log/string");
        h.publish(r);
        h2.publish(r);
        h3.publish(r);
        h4.publish(r);
        h.close();
        h2.close();
        h3.close();
        h4.close();
        assertFileContent(TEMPPATH + SEP + "log", "string", h.getFormatter());
        assertFileContent(TEMPPATH + SEP + "log", "string.1", h.getFormatter());
        assertFileContent(TEMPPATH + SEP + "log", "string.2", h.getFormatter());
        assertFileContent(TEMPPATH + SEP + "log", "string.3", h.getFormatter());

        // default is append mode
        FileHandler h6 = new FileHandler("%t/log/string%u.log");
        h6.publish(r);
        h6.close();
        FileHandler h7 = new FileHandler("%t/log/string%u.log");
        h7.publish(r);
        h7.close();
        try {
            assertFileContent(TEMPPATH + SEP + "log", "string0.log", h
                    .getFormatter());
            fail("should assertion failed");
        } catch (Error e) {
        }
        File file = new File(TEMPPATH + SEP + "log");
        assertTrue("length list of file is incorrect", file.list().length <= 2);

        // test unique ids
        FileHandler h8 = new FileHandler("%t/log/%ustring%u.log");
        h8.publish(r);
        FileHandler h9 = new FileHandler("%t/log/%ustring%u.log");
        h9.publish(r);
        h9.close();
        h8.close();
        assertFileContent(TEMPPATH + SEP + "log", "0string0.log", h
                .getFormatter());
        assertFileContent(TEMPPATH + SEP + "log", "1string1.log", h
                .getFormatter());
        file = new File(TEMPPATH + SEP + "log");
        assertTrue("length list of file is incorrect", file.list().length <= 2);
        
        try {
            new FileHandler("");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    /*
     * test for constructor void FileHandler(String pattern, boolean append)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify security exception.",
        method = "FileHandler",
        args = {java.lang.String.class, boolean.class}
    )
    public void testFileHandler_2params() throws Exception {
        boolean append = false;
        do {
            append = !append;
            handler = new FileHandler("%t/log/string", append);
            assertEquals("character encoding is non equal to actual value",
                    "iso-8859-1", handler.getEncoding());
            assertNotNull("Filter is null", handler.getFilter());
            assertNotNull("Formatter is null", handler.getFormatter());
            assertEquals("is non equal to actual value", Level.FINE, handler
                    .getLevel());
            assertNotNull("ErrorManager is null", handler.getErrorManager());
            handler.publish(r);
            handler.close();
            // output 3 times, and all records left
            // append mode is true
            for (int i = 0; i < 3; i++) {
                handler = new FileHandler("%t/log/string", append);
                handler.publish(r);
                handler.close();
            }
            if (append) {
                assertFileContent(TEMPPATH + SEP + "log", "/string",
                        new LogRecord[] { r, null, r, null, r, null, r },
                        new MockFormatter());
            } else {
                assertFileContent(TEMPPATH + SEP + "log", "/string",
                        new LogRecord[] { r }, new MockFormatter());
            }
        } while (append);
        
        try {
            new FileHandler("", true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    /*
     * test for constructor void FileHandler(String pattern, int limit, int
     * count)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verifysecurity exception.",
        method = "FileHandler",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void testFileHandler_3params() throws Exception {
        int limit = 120;
        int count = 1;
        handler = new FileHandler("%t/log/string", limit, count);
        assertEquals("character encoding is non equal to actual value",
                "iso-8859-1", handler.getEncoding());
        assertNotNull("Filter is null", handler.getFilter());
        assertNotNull("Formatter is null", handler.getFormatter());
        assertEquals("is non equal to actual value", Level.FINE, handler
                .getLevel());
        assertNotNull("ErrorManager is null", handler.getErrorManager());
        handler.publish(r);
        handler.close();
        // output 3 times, and all records left
        // append mode is true
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler("%t/log/string", limit, count);
            handler.publish(r);
            handler.close();
        }
        assertFileContent(TEMPPATH + SEP + "log", "/string", new LogRecord[] {
                r, null, r, null, r, null, r }, new MockFormatter());
        
        try {
            new FileHandler("", limit, count);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
        
        try {
            new FileHandler("%t/log/string", -1, count);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
        
        try {
            new FileHandler("%t/log/string", limit, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    /*
     * test for constructor public FileHandler(String pattern, int limit, int
     * count, boolean append)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify security exception.",
        method = "FileHandler",
        args = {java.lang.String.class, int.class, int.class, boolean.class}
    )
    public void testFileHandler_4params() throws Exception {
        int limit = 120;
        int count = 1;
        boolean append = false;
        do {
            append = !append;
            handler = new FileHandler("%t/log/string", limit, count, append);
            assertEquals("character encoding is non equal to actual value",
                    "iso-8859-1", handler.getEncoding());
            assertNotNull("Filter is null", handler.getFilter());
            assertNotNull("Formatter is null", handler.getFormatter());
            assertEquals("is non equal to actual value", Level.FINE, handler
                    .getLevel());
            assertNotNull("ErrorManager is null", handler.getErrorManager());
            handler.publish(r);
            handler.close();
            // output 3 times, and all records left
            // append mode is true
            for (int i = 0; i < 3; i++) {
                handler = new FileHandler("%t/log/string", limit, count, append);
                handler.publish(r);
                handler.close();
            }
            if (append) {
                assertFileContent(TEMPPATH + SEP + "log", "/string",
                        new LogRecord[] { r, null, r, null, r, null, r },
                        new MockFormatter());
            } else {
                assertFileContent(TEMPPATH + SEP + "log", "/string",
                        new LogRecord[] { r }, new MockFormatter());
            }
        } while (append);
        
        try {
            new FileHandler("", limit, count, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
        
        try {
            new FileHandler("%t/log/string", -1, count, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
        
        try {
            new FileHandler("%t/log/string", limit, 0, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getEncoding",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getFormatter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getErrorManager",
            args = {}
        )
    })
    public void testDefaultValue() throws Exception {
        handler.publish(r);
        handler.close();
        props.clear();
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        assertNull(handler.getEncoding());
        assertNull(handler.getFilter());
        assertTrue(handler.getFormatter() instanceof XMLFormatter);
        assertEquals(handler.getLevel(), Level.ALL);
        assertNotNull(handler.getErrorManager());
        handler.publish(r);
        handler.close();
        // output 3 times, and only one record left
        // default append mode is false
        for (int i = 0; i < 3; i++) {
            handler = new FileHandler();
            handler.publish(r);
            handler.close();
        }
        assertFileContent(HOMEPATH, "java0.log", new XMLFormatter());
    }

    private void assertFileContent(String homepath, String filename,
            Formatter formatter) throws Exception {
        assertFileContent(homepath, filename, new LogRecord[] { r }, formatter);
    }

    private void assertFileContent(String homepath, String filename,
            LogRecord[] lr, Formatter formatter) throws Exception {
        handler.close();
        String msg = "";
        // if formatter is null, the file content should be empty
        // else the message should be formatted given records
        if (null != formatter) {
            StringBuffer sb = new StringBuffer();
            sb.append(formatter.getHead(handler));
            for (int i = 0; i < lr.length; i++) {
                if (null == lr[i] && i < lr.length - 1) {
                    // if one record is null and is not the last record, means
                    // here is
                    // output completion point, should output tail, then output
                    // head
                    // (ready for next output)
                    sb.append(formatter.getTail(handler));
                    sb.append(formatter.getHead(handler));
                } else {
                    sb.append(formatter.format(lr[i]));
                }
            }
            sb.append(formatter.getTail(handler));
            msg = sb.toString();
        }
        char[] chars = new char[msg.length()];
        Reader reader = null;
        try {
            reader = new BufferedReader(new FileReader(homepath + SEP
                    + filename));
            reader.read(chars);
            assertEquals(msg, new String(chars));
            // assert has reached the end of the file
            assertEquals(-1, reader.read());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                // don't care
            }
            reset(homepath, filename);
        }
    }

    /**
     * Does a cleanup of given file
     * 
     * @param homepath
     * @param filename
     */
    private void reset(String homepath, String filename) {
        File file = null;
        try {
            file = new File(homepath + SEP + filename);
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            file = new File(homepath + SEP + filename + ".lck");
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "FileHandler",
        args = {java.lang.String.class, int.class, int.class, boolean.class}
    )
    public void testLimitAndCount() throws Exception {
        handler.close();
        // very small limit value, count=2
        // output, rename current output file to the second generation file
        // close it and open a new file as rotation output
        handler = new FileHandler("%t/testLimitCount%g", 1, 2, false);
        handler.publish(r);
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount1", handler.getFormatter());

        // very small limit value, count=1
        // output once, rotate(equals to nothing output)
        handler = new FileHandler("%t/testLimitCount%g", 1, 1, false);
        handler.publish(r);
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount0", new LogRecord[0],
                handler.getFormatter());

        // normal case, limit is 60(>2*msg length <3*msg length), append is
        // false
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        LogRecord[] rs = new LogRecord[10];
        // batch output twice to test the append mode
        for (int i = 0; i < 5; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        for (int i = 5; i < 10; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }

        assertFileContent(TEMPPATH, "testLimitCount0.1", new LogRecord[] {
                rs[5], rs[6], rs[7] }, handler.getFormatter());
        assertFileContent(TEMPPATH, "testLimitCount0.0", new LogRecord[] {
                rs[8], rs[9] }, handler.getFormatter());

        // normal case, limit is 60(>2*msg length <3*msg length), append is true
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, false);
        // batch output twice to test the append mode
        for (int i = 0; i < 5; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        handler = new FileHandler("%t/testLimitCount%u", 60, 3, true);
        for (int i = 5; i < 10; i++) {
            rs[i] = new LogRecord(Level.SEVERE, "msg" + i);
            handler.publish(rs[i]);
        }
        handler.close();
        assertFileContent(TEMPPATH, "testLimitCount0.2", new LogRecord[] {
                rs[3], rs[4], null, rs[5] }, handler.getFormatter());
        assertFileContent(TEMPPATH, "testLimitCount0.1", new LogRecord[] {
                rs[6], rs[7], rs[8] }, handler.getFormatter());
        assertFileContent(TEMPPATH, "testLimitCount0.0",
                new LogRecord[] { rs[9] }, handler.getFormatter());

        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        FileHandler h1 = null;
        FileHandler h2 = null;
        try {
            File logDir = new File("log");
            reset("log", "");
            logDir.mkdir();
            h1 = new FileHandler("log/a", 0, 1);
            assertNotNull(h1);
            h2 = new FileHandler("log/a", 0, 1, false);
            assertNotNull(h2);
        } finally {
            try {
                h1.close();
            } catch (Exception e) {
            }
            try {
                h2.close();
            } catch (Exception e) {
            }
            reset("log", "");
            System.setProperty("user.dir", oldUserDir);
        }
    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "close",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "FileHandler",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "FileHandler",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "FileHandler",
            args = {java.lang.String.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "FileHandler",
            args = {java.lang.String.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "FileHandler",
            args = {java.lang.String.class, int.class, int.class, boolean.class}
        )
    })
    public void testSecurity() throws IOException {
        SecurityManager currentManager = System.getSecurityManager();

        try {
            System.setSecurityManager(new MockLogSecurityManager());
            try {
                handler.close();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            handler.publish(new LogRecord(Level.SEVERE, "msg"));

            try {
                handler = new FileHandler();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }

            try {
                handler = new FileHandler("pattern1");
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern2", true);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern3", 1000, 1);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern4", 1000, 1, true);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
        } finally {
            System.setSecurityManager(currentManager);
        }

    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "close",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "FileHandler",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "FileHandler",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "FileHandler",
            args = {java.lang.String.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "FileHandler",
            args = {java.lang.String.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "FileHandler",
            args = {java.lang.String.class, int.class, int.class, boolean.class}
        )
    })
    public void testFileSecurity() throws IOException {
        SecurityManager currentManager = System.getSecurityManager();

        try {
            System.setSecurityManager(new MockFileSecurityManager());
            handler.publish(new LogRecord(Level.SEVERE, "msg"));
            try {
                handler.close();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }

            try {
                handler = new FileHandler();
                fail("should throw security exception");
            } catch (SecurityException e) {
            }

            try {
                handler = new FileHandler("pattern1");
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern2", true);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern3", 1000, 1);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
            try {
                handler = new FileHandler("pattern4", 1000, 1, true);
                fail("should throw security exception");
            } catch (SecurityException e) {
            }
        } finally {
            System.setSecurityManager(currentManager);
        }
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies FileHandler when configuration file is invalid.",
        method = "FileHandler",
        args = {}
    )
    public void testInvalidProperty() throws Exception {
        props.put("java.util.logging.FileHandler.level", "null");
        props.put("java.util.logging.FileHandler.filter", className
                + "$MockFilte");
        props.put("java.util.logging.FileHandler.formatter", className
                + "$MockFormatte");
        props.put("java.util.logging.FileHandler.encoding", "ut");
        // limit to only two message
        props.put("java.util.logging.FileHandler.limit", "-1");
        // rotation count is 2
        props.put("java.util.logging.FileHandler.count", "-1");
        // using append mode
        props.put("java.util.logging.FileHandler.append", "bad");

        handler.close();

        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        handler = new FileHandler();
        assertEquals(Level.ALL, handler.getLevel());
        assertNull(handler.getFilter());
        assertTrue(handler.getFormatter() instanceof XMLFormatter);
        assertNull(handler.getEncoding());
        handler.close();

        props.put("java.util.logging.FileHandler.pattern", "");
        manager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        try {
            handler = new FileHandler();
            fail("shouldn't open file with empty name");
        } catch (NullPointerException e) {
        }
    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies illegal parameters and exceptions: IOException, NullPointerException, IllegalArgumentException.",
            method = "FileHandler",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies illegal parameters and exceptions: IOException, NullPointerException, IllegalArgumentException.",
            method = "FileHandler",
            args = {java.lang.String.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies illegal parameters and exceptions: IOException, NullPointerException, IllegalArgumentException.",
            method = "FileHandler",
            args = {java.lang.String.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies illegal parameters and exceptions: IOException, NullPointerException, IllegalArgumentException.",
            method = "publish",
            args = {java.util.logging.LogRecord.class}
        )
    })
    @AndroidOnly("This test fails on RI. Doesn't parse special pattern \"%t/%h.")
    public void testInvalidParams() throws IOException {

        // %t and %p parsing can add file separator automatically
        FileHandler h1 = new FileHandler("%taaa");
        h1.close();
        File file = new File(TEMPPATH + SEP + "aaa");
        assertTrue(file.exists());
        reset(TEMPPATH, "aaa");

        h1 = new FileHandler("%t%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "0");
        assertTrue(file.exists());
        reset(TEMPPATH, "0");
        h1 = new FileHandler("%t%u%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "00");
        assertTrue(file.exists());
        reset(TEMPPATH, "00");

        // this is normal case
        h1 = new FileHandler("%t/%u%g%%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "00%g");
        assertTrue(file.exists());
        reset(TEMPPATH, "00%g");

        // multi separator has no effect
        h1 = new FileHandler("//%t//multi%g");
        h1.close();
        file = new File(TEMPPATH + SEP + "multi0");
        assertTrue(file.exists());
        reset(TEMPPATH, "multi0");

        // bad directory, IOException
        try {
            h1 = new FileHandler("%t/baddir/multi%g");
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());
        
        // bad directory, IOException, append
        try {
            h1 = new FileHandler("%t/baddir/multi%g", true);
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());  
        try {
            h1 = new FileHandler("%t/baddir/multi%g", false);
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());
        
        try {
            h1 = new FileHandler("%t/baddir/multi%g", 12, 4);
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());
        
        try {
            h1 = new FileHandler("%t/baddir/multi%g", 12, 4, true);
            fail("should throw IO exception");
        } catch (IOException e) {
        }
        file = new File(TEMPPATH + SEP + "baddir" + SEP + "multi0");
        assertFalse(file.exists());
        

        try {
            new FileHandler(null);
            fail("should throw null exception");
        } catch (NullPointerException e) {
        }
        try {
            handler.publish(null);
        } catch (NullPointerException e) {
            fail("should not throw NPE");
        }
        try {
            new FileHandler(null, false);
            fail("should throw null exception");
        } catch (NullPointerException e) {
        }
        try {
            new FileHandler("");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new FileHandler("%t/java%u", 0, 0);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FileHandler("%t/java%u", -1, 1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            new FileHandler("%t/java%u", -1, -1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        
        // always parse special pattern
        System.setProperty("user.home", "home");
        try {
            h1 = new FileHandler("%t/%h.txt");
        } catch (Exception e) {
            fail("Unexpected exception " + e.toString());
        }
    }

    /*
     * test for method public void publish(LogRecord record)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "publish",
        args = {java.util.logging.LogRecord.class}
    )
    public void testPublish() throws Exception {
        LogRecord[] r = new LogRecord[] { new LogRecord(Level.CONFIG, "msg__"),
                new LogRecord(Level.WARNING, "message"),
                new LogRecord(Level.INFO, "message for"),
                new LogRecord(Level.FINE, "message for test") };
        for (int i = 0; i < r.length; i++) {
            handler = new FileHandler("%t/log/stringPublish");
            handler.publish(r[i]);
            handler.close();
            assertFileContent(TEMPPATH + SEP + "log", "stringPublish",
                    new LogRecord[] { r[i] }, handler.getFormatter());
        }
    }

    /*
     * test for method public void close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void testClose() throws Exception {
        FileHandler h = new FileHandler("%t/log/stringPublish");
        h.publish(r);
        h.close();
        assertFileContent(TEMPPATH + SEP + "log", "stringPublish", h
                .getFormatter());
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Doesn't verify SecurityException.",
        method = "setOutputStream",
        args = {java.io.OutputStream.class}
    )
    // set output stream still works, just like super StreamHandler
    public void testSetOutputStream() throws Exception {
        MockFileHandler handler = new MockFileHandler("%h/setoutput.log");
        handler.setFormatter(new MockFormatter());
        handler.publish(r);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.publicSetOutputStream(out);
        handler.publish(r);
        handler.close();
        String msg = new String(out.toByteArray());
        Formatter f = handler.getFormatter();
        assertEquals(msg, f.getHead(handler) + f.format(r) + f.getTail(handler));
        assertFileContent(HOMEPATH, "setoutput.log", handler.getFormatter());
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "FileHandler",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void testEmptyPattern_3params() throws SecurityException,
            IOException {
        try {
            new FileHandler(new String(), 1, 1);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "FileHandler",
        args = {java.lang.String.class, boolean.class}
    )
    public void testEmptyPattern_2params() throws SecurityException,
            IOException {
        try {
            new FileHandler(new String(), true);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IllegalArgumentException.",
        method = "FileHandler",
        args = {java.lang.String.class, int.class, int.class, boolean.class}
    )
    public void testEmptyPattern_4params() throws SecurityException,
            IOException {
        try {
            new FileHandler(new String(), 1, 1, true);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /*
     * mock classes
     */
    public static class MockFilter implements Filter {
        public boolean isLoggable(LogRecord record) {
            return !record.getMessage().equals("false");
        }
    }

    public static class MockFormatter extends Formatter {
        public String format(LogRecord r) {
            if (null == r) {
                return "";
            }
            return r.getMessage() + " by MockFormatter\n";
        }

        public String getTail(Handler h) {
            return "tail\n";
        }

        public String getHead(Handler h) {
            return "head\n";
        }
    }

    public static class MockLogSecurityManager extends SecurityManager {
        public void checkPermission(Permission perm) {
            if (perm instanceof LoggingPermission) {
                throw new SecurityException();
            }
            return;
        }
    }

    public static class MockFileSecurityManager extends SecurityManager {
        public void checkPermission(Permission perm) {
            if (perm instanceof FilePermission) {
                throw new SecurityException();
            }
        }
    }

    public static class MockFileHandler extends FileHandler {
        public MockFileHandler() throws IOException {
            super();
        }

        public MockFileHandler(String pattern) throws IOException {
            super(pattern);
        }

        public void publicSetOutputStream(OutputStream stream) {
            super.setOutputStream(stream);
        }
    }
}
