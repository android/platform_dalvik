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

import dalvik.annotation.BrokenTest;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import org.apache.harmony.logging.tests.java.util.logging.util.EnvironmentHelper;

import tests.util.CallVerificationStack;

import java.security.Permission;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;

/**
 * Test suite for the class java.util.logging.Logger.
 */
@TestTargetClass(Logger.class)
public class LoggerTest extends TestCase {

    private final static String VALID_RESOURCE_BUNDLE = "bundles/java/util/logging/res";

    private final static String VALID_RESOURCE_BUNDLE2 = "bundles/java/util/logging/res2";

    private final static String VALID_RESOURCE_BUNDLE3 = "bundles/java/util/logging/res3";

    private final static String INVALID_RESOURCE_BUNDLE = "impossible_not_existing";

    private final static String LOGGING_CONFIG_FILE = "src/test/resources/config/java/util/logging/logging.config";

    private final static String VALID_KEY = "LOGGERTEST";

    private final static String VALID_VALUE = "Test_ZH_CN";

    private final static String VALID_VALUE2 = "Test_NoLocale2";

    private Logger sharedLogger = null;

    private Locale oldLocale = null;

    /*
     * @see TestCase#setUp() Notice : Logger constructor is protected =>
     * MockLogger
     */
    protected void setUp() throws Exception {
        super.setUp();
        oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("zh", "CN"));
        sharedLogger = new MockLogger("SharedLogger", VALID_RESOURCE_BUNDLE);
        sharedLogger.addHandler(new MockHandler());
    }

    /*
     * Reset the log manager.
     */
    protected void tearDown() throws Exception {
        CallVerificationStack.getInstance().clear();
        Locale.setDefault(oldLocale);
        LogManager.getLogManager().reset();
        super.tearDown();
    }

    /*
     * Test the global logger
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGlobalLogger() {
        assertNull(Logger.global.getFilter());
        assertEquals(0, Logger.global.getHandlers().length);
        assertNull(Logger.global.getLevel());
        assertEquals("global", Logger.global.getName());
        assertNull(Logger.global.getParent().getParent());
        assertNull(Logger.global.getResourceBundle());
        assertNull(Logger.global.getResourceBundleName());
        assertTrue(Logger.global.getUseParentHandlers());
        assertSame(Logger.global, Logger.getLogger("global"));
        assertSame(Logger.global, LogManager.getLogManager()
                .getLogger("global"));
    }

    /*
     * Test constructor under normal conditions.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies constructor under normal conditions.",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testConstructor_Normal() {
        MockLogger mlog = new MockLogger("myname", VALID_RESOURCE_BUNDLE);
        assertNull(mlog.getFilter());
        assertEquals(0, mlog.getHandlers().length);
        assertNull(mlog.getLevel());
        assertEquals("myname", mlog.getName());
        assertNull(mlog.getParent());
        ResourceBundle rb = mlog.getResourceBundle();
        assertEquals(VALID_VALUE, rb.getString(VALID_KEY));
        assertEquals(mlog.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
        assertTrue(mlog.getUseParentHandlers());
    }

    /*
     * Test constructor with null parameters.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies constructor with null parameters.",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testConstructor_Null() {
        MockLogger mlog = new MockLogger(null, null);
        assertNull(mlog.getFilter());
        assertEquals(0, mlog.getHandlers().length);
        assertNull(mlog.getLevel());
        assertNull(mlog.getName());
        assertNull(mlog.getParent());
        assertNull(mlog.getResourceBundle());
        assertNull(mlog.getResourceBundleName());
        assertTrue(mlog.getUseParentHandlers());
    }

    /*
     * Test constructor with invalid name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies constructor with invalid name.",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testConstructor_InvalidName() {
        MockLogger mlog = new MockLogger("...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|",
                null);
        assertEquals("...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|", mlog.getName());
    }

    /*
     * Test constructor with empty name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies constructor with empty name.",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testConstructor_EmptyName() {
        MockLogger mlog = new MockLogger("", null);
        assertEquals("", mlog.getName());
    }

    /*
     * Test constructor with invalid resource bundle name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies MissingResourceException.",
        method = "Logger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testConstructor_InvalidResourceBundle() {

        // try anonymous with invalid resource
        try {
            new MockLogger(null, INVALID_RESOURCE_BUNDLE);
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
            // ok !
        }
        // try named Logger with invalid resource
        try {
            new MockLogger("testConstructor_InvalidResourceBundle",
                    INVALID_RESOURCE_BUNDLE);
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
            // ok !
        }
        // try empty string
        try {
            new MockLogger(null, "");
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
            // ok !
        }
    }

    /*
     * Test getAnonymousLogger()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAnonymousLogger",
        args = {}
    )
    public void testGetAnonymousLogger() {
        Logger alog = Logger.getAnonymousLogger();
        assertNotSame(alog, Logger.getAnonymousLogger());
        assertNull(alog.getFilter());
        assertEquals(0, alog.getHandlers().length);
        assertNull(alog.getLevel());
        assertNull(alog.getName());
        assertEquals("", alog.getParent().getName());
        assertNull(alog.getParent().getParent());
        assertNull(alog.getResourceBundle());
        assertNull(alog.getResourceBundleName());
        assertTrue(alog.getUseParentHandlers());
    }

    /*
     * Test getAnonymousLogger(String resourceBundleName) with valid resource
     * bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getAnonymousLogger(String resourceBundleName) with valid resource bundle.",
        method = "getAnonymousLogger",
        args = {java.lang.String.class}
    )
    public void testGetAnonymousLogger_ValidResourceBundle() {
        Logger alog = Logger.getAnonymousLogger(VALID_RESOURCE_BUNDLE);
        assertNotSame(alog, Logger.getAnonymousLogger(VALID_RESOURCE_BUNDLE));
        assertNull(alog.getFilter());
        assertEquals(0, alog.getHandlers().length);
        assertNull(alog.getLevel());
        assertNull(alog.getName());
        assertEquals("", alog.getParent().getName());
        assertNull(alog.getParent().getParent());
        assertEquals(VALID_VALUE, alog.getResourceBundle().getString(VALID_KEY));
        assertEquals(alog.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
        assertTrue(alog.getUseParentHandlers());
    }

    /*
     * Test getAnonymousLogger(String resourceBundleName) with null resource
     * bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getAnonymousLogger(String resourceBundleName) with null resource bundle.",
        method = "getAnonymousLogger",
        args = {java.lang.String.class}
    )
    public void testGetAnonymousLogger_NullResourceBundle() {
        Logger alog = Logger.getAnonymousLogger(null);
        assertNotSame(alog, Logger.getAnonymousLogger(null));
        assertNull(alog.getFilter());
        assertEquals(0, alog.getHandlers().length);
        assertNull(alog.getLevel());
        assertNull(alog.getName());
        assertEquals("", alog.getParent().getName());
        assertNull(alog.getParent().getParent());
        assertNull(alog.getResourceBundle());
        assertNull(alog.getResourceBundleName());
        assertTrue(alog.getUseParentHandlers());
    }

    /*
     * Test getAnonymousLogger(String resourceBundleName) with invalid resource
     * bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getAnonymousLogger(String resourceBundleName) with invalid resource bundle.",
        method = "getAnonymousLogger",
        args = {java.lang.String.class}
    )
    public void testGetAnonymousLogger_InvalidResourceBundle() {
        try {
            Logger.getAnonymousLogger(INVALID_RESOURCE_BUNDLE);
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
        }
        // try empty name
        try {
            Logger.getAnonymousLogger("");
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
        }
    }

    /*
     * Test getLogger(String), getting a logger with no parent.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String), getting a logger with no parent.",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_Normal() throws Exception {
        // config the level
        Properties p = new Properties();
        p.put("testGetLogger_Normal_ANewLogger.level", "ALL");
        LogManager.getLogManager().readConfiguration(
                EnvironmentHelper.PropertiesToInputStream(p));

        assertNull(LogManager.getLogManager().getLogger(
                "testGetLogger_Normal_ANewLogger"));
        // create a new logger
        Logger log = Logger.getLogger("testGetLogger_Normal_ANewLogger");
        // get an existing logger
        assertSame(log, Logger.getLogger("testGetLogger_Normal_ANewLogger"));
        // check it has been registered
        assertSame(log, LogManager.getLogManager().getLogger(
                "testGetLogger_Normal_ANewLogger"));

        assertNull(log.getFilter());
        assertEquals(0, log.getHandlers().length);
        // check it's set to the preconfigured level
        assertSame(Level.ALL, log.getLevel());
        assertEquals("testGetLogger_Normal_ANewLogger", log.getName());
        assertNull(log.getParent().getParent());
        assertNull(log.getResourceBundle());
        assertNull(log.getResourceBundleName());
        assertTrue(log.getUseParentHandlers());


    }

    /*
     * Test getLogger(String), getting a logger with invalid level configured.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String), getting a logger with invalid level configured.",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_InvalidLevel() throws Exception {
        // config the level
        Properties p = new Properties();
        p
                .put("testGetLogger_InvalidLevel_ANewLogger.level",
                        "impossible_level");
        LogManager.getLogManager().readConfiguration(
                EnvironmentHelper.PropertiesToInputStream(p));

        assertNull(LogManager.getLogManager().getLogger(
                "testGetLogger_InvalidLevel_ANewLogger"));
        // create a new logger
        Logger log = Logger.getLogger("testGetLogger_InvalidLevel_ANewLogger");
        // get an existing logger
        assertSame(log, Logger
                .getLogger("testGetLogger_InvalidLevel_ANewLogger"));
        // check it has been registered
        assertSame(log, LogManager.getLogManager().getLogger(
                "testGetLogger_InvalidLevel_ANewLogger"));
        assertNull(log.getLevel());
    }

    /*
     * Test getLogger(String) with null name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String) with null name.",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_Null() {
        try {
            Logger.getLogger(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // ok !
        }
        try {
            Logger.getLogger(null, null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // ok !
        }
    }

    /*
     * Test getLogger(String) with invalid name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String) with invalid name.",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_Invalid() {
        Logger log = Logger.getLogger("...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|");
        assertEquals("...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|", log.getName());
    }

    /*
     * Test getLogger(String) with empty name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String) with empty name.",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    @BrokenTest("This fails on RI and Android." +
            "getResourceBundle and getResourceBundleName methods return " +
            "null on RI for Logger with empty string name. On the RI " + 
            "getHandlers() returns a non empty array.")
    public void testGetLogger_Empty() {
        assertNotNull(LogManager.getLogManager().getLogger(""));
        Logger log = Logger.getLogger("");
        assertSame(log, LogManager.getLogManager().getLogger(""));
        assertNull(log.getFilter());
        assertEquals(0, log.getHandlers().length);
        // check it's set to the preconfigured level
        assertSame(Level.INFO, log.getLevel());
        assertEquals("", log.getName());
        assertNull(log.getParent());
        assertTrue(log.getUseParentHandlers());
        assertNull(log.getResourceBundle());
        assertNull(log.getResourceBundleName());
    }

    /*
     * Test getLogger(String), getting a logger with existing parent.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String), getting a logger with existing parent.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLogger_WithParent() {
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLogger_WithParent_ParentLogger"));

        // get root of hierarchy
        Logger root = Logger.getLogger("");
        // create the parent logger
        Logger pLog = Logger.getLogger("testGetLogger_WithParent_ParentLogger",
                VALID_RESOURCE_BUNDLE);
        pLog.setLevel(Level.CONFIG);
        pLog.addHandler(new MockHandler());
        pLog.setFilter(new MockFilter());
        pLog.setUseParentHandlers(false);
        // check root parent
        assertEquals("testGetLogger_WithParent_ParentLogger", pLog.getName());
        assertSame(pLog.getParent(), root);

        // child part
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLogger_WithParent_ParentLogger.child"));
        // create the child logger
        Logger child = Logger
                .getLogger("testGetLogger_WithParent_ParentLogger.child");
        assertNull(child.getFilter());
        assertEquals(0, child.getHandlers().length);
        assertNull(child.getLevel());
        assertEquals("testGetLogger_WithParent_ParentLogger.child", child
                .getName());
        assertSame(child.getParent(), pLog);
        assertNull(child.getResourceBundle());
        assertNull(child.getResourceBundleName());
        assertTrue(child.getUseParentHandlers());

        // create not valid child
        Logger notChild = Logger
                .getLogger("testGetLogger_WithParent_ParentLogger1.child");
        assertNull(notChild.getFilter());
        assertEquals(0, notChild.getHandlers().length);
        assertNull(notChild.getLevel());
        assertEquals("testGetLogger_WithParent_ParentLogger1.child", notChild
                .getName());
        assertNotSame(notChild.getParent(), pLog);
        assertNull(notChild.getResourceBundle());
        assertNull(notChild.getResourceBundleName());
        assertTrue(notChild.getUseParentHandlers());
        // verify two level root.parent
        assertEquals("testGetLogger_WithParent_ParentLogger.child", child
                .getName());
        assertSame(child.getParent().getParent(), root);


        // create three level child
        Logger childOfChild = Logger
                .getLogger("testGetLogger_WithParent_ParentLogger.child.child");
        assertNull(childOfChild.getFilter());
        assertEquals(0, childOfChild.getHandlers().length);
        assertSame(child.getParent().getParent(), root);
        assertNull(childOfChild.getLevel());
        assertEquals("testGetLogger_WithParent_ParentLogger.child.child",
                childOfChild.getName());

        assertSame(childOfChild.getParent(), child);
        assertSame(childOfChild.getParent().getParent(), pLog);
        assertSame(childOfChild.getParent().getParent().getParent(), root);
        assertNull(childOfChild.getResourceBundle());
        assertNull(childOfChild.getResourceBundleName());
        assertTrue(childOfChild.getUseParentHandlers());

        // abnormal case : lookup to root parent in a hierarchy without a logger
        // parent created between
        assertEquals("testGetLogger_WithParent_ParentLogger1.child", notChild
                .getName());
        assertSame(child.getParent().getParent(), root); 
        assertNotSame(child.getParent(), root); 

        // abnormal cases
        assertNotSame(root.getParent(), root);
        Logger twoDot = Logger.getLogger("..");
        assertSame(twoDot.getParent(), root);

    }


    /*
     * Test getLogger(String, String), getting a logger with no parent.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String), getting a logger with no parent.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_Normal() throws Exception {
        // config the level
        Properties p = new Properties();
        p.put("testGetLoggerWithRes_Normal_ANewLogger.level", "ALL");
        LogManager.getLogManager().readConfiguration(
                EnvironmentHelper.PropertiesToInputStream(p));

        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_Normal_ANewLogger"));
        // create a new logger
        Logger log = Logger.getLogger("testGetLoggerWithRes_Normal_ANewLogger",
                VALID_RESOURCE_BUNDLE);
        // get an existing logger
        assertSame(log, Logger
                .getLogger("testGetLoggerWithRes_Normal_ANewLogger"));
        // check it has been registered
        assertSame(log, LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_Normal_ANewLogger"));

        assertNull(log.getFilter());
        assertEquals(0, log.getHandlers().length);
        // check it's set to the preconfigured level
        assertSame(Level.ALL, log.getLevel());
        assertEquals("testGetLoggerWithRes_Normal_ANewLogger", log.getName());
        assertNull(log.getParent().getParent());
        assertEquals(VALID_VALUE, log.getResourceBundle().getString(VALID_KEY));
        assertEquals(log.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
        assertTrue(log.getUseParentHandlers());
    }

    /*
     * Test getLogger(String, String) with null parameters.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with null parameters.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_Null() {
        Logger.getLogger("testGetLoggerWithRes_Null_ANewLogger", null);
        try {
            Logger.getLogger(null, VALID_RESOURCE_BUNDLE);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }


    /*
     * Test getLogger(String, String) with invalid resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with invalid resource bundle.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    @KnownFailure("IllegalArgumentException is thrown instead of " +
            "MissingResourceException if resource bundle with specified " +
            "name can't be found.")
    public void testGetLoggerWithRes_InvalidResourceBundle() {

        assertNull(LogManager.getLogManager().getLogger(
                "testMissingResourceException"));

        try {
            Logger.getLogger("testMissingResourceException",
                    INVALID_RESOURCE_BUNDLE);
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
            // correct
        }
        assertNull(Logger.getLogger("testMissingResourceException")
                .getResourceBundle());
        assertNull(Logger.getLogger("testMissingResourceException")
                .getResourceBundleName());
        // try empty string
        try {
            Logger.getLogger("testMissingResourceException", "");
            fail("Should throw MissingResourceException!");
        } catch (MissingResourceException e) {
            // correct
        }

        assertNotNull(LogManager.getLogManager().getLogger(""));
        // The root logger always exists TODO
        try { 
            Logger.getLogger("", INVALID_RESOURCE_BUNDLE);         
        } catch (MissingResourceException e) { 
            //correct 
        }
    }

    /*
     * Test getLogger(String, String) with valid resource bundle, to get an
     * existing logger with no associated resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with valid resource bundle, to get an existing logger with no associated resource bundle.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_ExistingLoggerWithNoRes() {
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithNoRes_ANewLogger"));
        // create a new logger
        Logger log1 = Logger
                .getLogger("testGetLoggerWithRes_ExistingLoggerWithNoRes_ANewLogger");
        // get an existing logger
        Logger log2 = Logger.getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithNoRes_ANewLogger",
                VALID_RESOURCE_BUNDLE);
        assertSame(log1, log2);
        assertEquals(VALID_VALUE, log1.getResourceBundle().getString(VALID_KEY));
        assertEquals(log1.getResourceBundleName(), VALID_RESOURCE_BUNDLE);

    }

    /*
     * Test getLogger(String, String) with valid resource bundle, to get an
     * existing logger with the same associated resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with valid resource bundle, to get an existing logger with the same associated resource bundle.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_ExistingLoggerWithSameRes() {
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithSameRes_ANewLogger"));
        // create a new logger
        Logger log1 = Logger.getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithSameRes_ANewLogger",
                VALID_RESOURCE_BUNDLE);
        // get an existing logger
        Logger log2 = Logger.getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithSameRes_ANewLogger",
                VALID_RESOURCE_BUNDLE);
        assertSame(log1, log2);
        assertEquals(VALID_VALUE, log1.getResourceBundle().getString(VALID_KEY));
        assertEquals(log1.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
    }

    /*
     * Test getLogger(String, String) with valid resource bundle, to get an
     * existing logger with different associated resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with valid resource bundle, to get an existing logger with different associated resource bundle.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_ExistingLoggerWithDiffRes() {
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithDiffRes_ANewLogger"));
        // create a new logger
        Logger log1 = Logger.getLogger(
                "testGetLoggerWithRes_ExistingLoggerWithDiffRes_ANewLogger",
                VALID_RESOURCE_BUNDLE);
        assertNotNull(log1);
        // get an existing logger
        try {
            Logger
                    .getLogger(
                            "testGetLoggerWithRes_ExistingLoggerWithDiffRes_ANewLogger",
                            VALID_RESOURCE_BUNDLE2);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
        }

        try {
            Logger
                    .getLogger(
                            "testGetLoggerWithRes_ExistingLoggerWithDiffRes_ANewLogger",
                            null);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test getLogger(String, String) with invalid name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with invalid name.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_InvalidName() {
        Logger log = Logger.getLogger(
                "...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|WithRes",
                VALID_RESOURCE_BUNDLE);
        assertEquals("...#$%%^&&()-_+=!@~./,[]{};:'\\\"?|WithRes", log
                .getName());
    }

    /*
     * Test getLogger(String, String) with empty name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String) with empty name.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_Empty() {
        Logger log = Logger.getLogger("", VALID_RESOURCE_BUNDLE);
        assertSame(log, LogManager.getLogManager().getLogger(""));
        assertNull(log.getFilter());
        assertEquals(0, log.getHandlers().length);
        // check it's set to the preconfigured level
        assertSame(Level.INFO, log.getLevel());
        assertEquals("", log.getName());
        assertNull(log.getParent());
        assertEquals(VALID_VALUE, log.getResourceBundle().getString(VALID_KEY));
        assertEquals(log.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
        assertTrue(log.getUseParentHandlers());
    }

    /*
     * Test getLogger(String, String), getting a logger with existing parent.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getLogger(String, String), getting a logger with existing parent.",
        method = "getLogger",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testGetLoggerWithRes_WithParentNormal() {
        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_WithParent_ParentLogger"));
        // create the parent logger
        Logger pLog = Logger
                .getLogger("testGetLoggerWithRes_WithParent_ParentLogger");
        pLog.setLevel(Level.CONFIG);
        pLog.addHandler(new MockHandler());
        pLog.setFilter(new MockFilter());
        pLog.setUseParentHandlers(false);

        assertNull(LogManager.getLogManager().getLogger(
                "testGetLoggerWithRes_WithParent_ParentLogger.child"));
        // create the child logger
        Logger log = Logger.getLogger(
                "testGetLoggerWithRes_WithParent_ParentLogger.child",
                VALID_RESOURCE_BUNDLE);
        assertNull(log.getFilter());
        assertEquals(0, log.getHandlers().length);
        assertNull(log.getLevel());
        assertEquals("testGetLoggerWithRes_WithParent_ParentLogger.child", log
                .getName());
        assertSame(log.getParent(), pLog);
        assertEquals(VALID_VALUE, log.getResourceBundle().getString(VALID_KEY));
        assertEquals(log.getResourceBundleName(), VALID_RESOURCE_BUNDLE);
        assertTrue(log.getUseParentHandlers());
    }

    /*
     * Test addHandler(Handler) for a named logger with sufficient privilege.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_NamedLoggerSufficientPrivilege() {
        Logger log = Logger
                .getLogger("testAddHandler_NamedLoggerSufficientPrivilege");
        MockHandler h = new MockHandler();
        assertEquals(log.getHandlers().length, 0);
        log.addHandler(h);
        assertEquals(log.getHandlers().length, 1);

    }

    /*
     * Test addHandler(Handler) for a named logger with sufficient privilege,
     * add duplicate handlers.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_NamedLoggerSufficientPrivilegeDuplicate() {
        Logger log = Logger
                .getLogger("testAddHandler_NamedLoggerSufficientPrivilegeDuplicate");
        MockHandler h = new MockHandler();
        assertEquals(log.getHandlers().length, 0);
        for (int i = 0; i < 12; i++) {
            log.addHandler(h);
        }
        assertEquals(log.getHandlers().length, 12);
        assertSame(log.getHandlers()[0], h);
        assertSame(log.getHandlers()[5], h);
        assertSame(log.getHandlers()[11], h);
    }

    /*
     * Test addHandler(Handler) with a null handler.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_Null() {
        Logger log = Logger.getLogger("testAddHandler_Null");
        try {
            log.addHandler(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test addHandler(Handler) for a named logger with insufficient privilege.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_NamedLoggerInsufficientPrivilege() {
        Logger log = Logger
                .getLogger("testAddHandler_NamedLoggerInsufficientPrivilege");
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());

        try {
            log.addHandler(h);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test addHandler(Handler) for a named logger with insufficient privilege,
     * using a null handler.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_NamedLoggerInsufficientPrivilegeNull() {
        Logger log = Logger
                .getLogger("testAddHandler_NamedLoggerInsufficientPrivilege");
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());

        try {
            log.addHandler(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test addHandler(Handler) for an anonymous logger with sufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_AnonyLoggerSufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        MockHandler h = new MockHandler();
        assertEquals(log.getHandlers().length, 0);
        log.addHandler(h);
        assertEquals(log.getHandlers().length, 1);
        assertSame(log.getHandlers()[0], h);
    }

    /*
     * Test addHandler(Handler) for an anonymous logger with insufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            assertEquals(log.getHandlers().length, 0);
            log.addHandler(h);
            assertEquals(log.getHandlers().length, 1);
            assertSame(log.getHandlers()[0], h);
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test addHandler(Handler) for a null-named mock logger with insufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies addHandler(Handler) for a null-named mock logger with insufficient privilege, SecurityException.",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testAddHandler_NullNamedMockLoggerInsufficientPrivilege() {
        MockLogger mlog = new MockLogger(null, null);
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            mlog.addHandler(h);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test removeHandler(Handler) for a named logger with sufficient privilege,
     * remove an existing handler.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_NamedLoggerSufficientPrivilege() {
        Logger log = Logger
                .getLogger("testRemoveHandler_NamedLoggerSufficientPrivilege");
        MockHandler h = new MockHandler();
        log.addHandler(h);
        assertEquals(log.getHandlers().length, 1);
        log.removeHandler(h);
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test removeHandler(Handler) for a named logger with sufficient privilege,
     * remove a non-existing handler.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_NamedLoggerSufficientPrivilegeNotExisting() {
        Logger log = Logger
                .getLogger("testRemoveHandler_NamedLoggerSufficientPrivilegeNotExisting");
        MockHandler h = new MockHandler();
        assertEquals(log.getHandlers().length, 0);
        log.removeHandler(h);
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test removeHandler(Handler) with a null handler.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_Null() {
        Logger log = Logger.getLogger("testRemoveHandler_Null");
        log.removeHandler(null);
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test removeHandler(Handler) for a named logger with insufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_NamedLoggerInsufficientPrivilege() {
        Logger log = Logger
                .getLogger("testRemoveHandler_NamedLoggerInsufficientPrivilege");
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());

        try {
            log.removeHandler(h);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test removeHandler(Handler) for a named logger with insufficient
     * privilege, using a null handler.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_NamedLoggerInsufficientPrivilegeNull() {
        Logger log = Logger
                .getLogger("testRemoveHandler_NamedLoggerInsufficientPrivilege");
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());

        try {
            log.removeHandler(null);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test removeHandler(Handler) for an anonymous logger with sufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_AnonyLoggerSufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        MockHandler h = new MockHandler();
        log.addHandler(h);
        assertEquals(log.getHandlers().length, 1);
        log.removeHandler(h);
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test removeHandler(Handler) for an anonymous logger with insufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.addHandler(h);
            assertEquals(log.getHandlers().length, 1);
            log.removeHandler(h);
            assertEquals(log.getHandlers().length, 0);
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test removeHandler(Handler) for a null-named mock logger with
     * insufficient privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removeHandler",
        args = {java.util.logging.Handler.class}
    )
    public void testRemoveHandler_NullNamedMockLoggerInsufficientPrivilege() {
        MockLogger mlog = new MockLogger(null, null);
        MockHandler h = new MockHandler();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            mlog.removeHandler(h);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test getHandlers() when there's no handler.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getHandlers() when there's no handler.",
        method = "getHandlers",
        args = {}
    )
    public void testGetHandlers_None() {
        Logger log = Logger.getLogger("testGetHandlers_None");
        assertEquals(log.getHandlers().length, 0);
    }

    /*
     * Test getHandlers() when there are several handlers.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getHandlers() when there are several handlers.",
        method = "getHandlers",
        args = {}
    )
    public void testGetHandlers_Several() {
        Logger log = Logger.getLogger("testGetHandlers_None");
        assertEquals(log.getHandlers().length, 0);
        MockHandler h1 = new MockHandler();
        MockHandler h2 = new MockHandler();
        MockHandler h3 = new MockHandler();
        log.addHandler(h1);
        log.addHandler(h2);
        log.addHandler(h3);
        assertEquals(log.getHandlers().length, 3);
        assertSame(log.getHandlers()[0], h1);
        assertSame(log.getHandlers()[1], h2);
        assertSame(log.getHandlers()[2], h3);
        // remove one
        log.removeHandler(h2);
        assertEquals(log.getHandlers().length, 2);
        assertSame(log.getHandlers()[0], h1);
        assertSame(log.getHandlers()[1], h3);

    }

    /*
     * Test getFilter & setFilter with normal value for a named logger, having
     * sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getFilter & setFilter with normal value for a named logger, having sufficient privilege.",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getFilter & setFilter with normal value for a named logger, having sufficient privilege.",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testGetSetFilter_NamedLoggerSufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetFilter_NamedLoggerSufficientPrivilege");
        Filter f = new MockFilter();

        assertNull(log.getFilter());
        log.setFilter(f);
        assertSame(f, log.getFilter());
    }

    /*
     * Test getFilter & setFilter with null value, having sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getFilter & setFilter with null value, having sufficient privilege.",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getFilter & setFilter with null value, having sufficient privilege.",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testGetSetFilter_Null() {
        Logger log = Logger.getLogger("testGetSetFilter_Null");

        assertNull(log.getFilter());
        log.setFilter(null);
        assertNull(log.getFilter());
        log.setFilter(new MockFilter());
        log.setFilter(null);
        assertNull(log.getFilter());
    }

    /*
     * Test setFilter with normal value for a named logger, having insufficient
     * privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies setFilter with normal value for a named logger, having insufficient privilege.",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies setFilter with normal value for a named logger, having insufficient privilege.",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testGetSetFilter_NamedLoggerInsufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetFilter_NamedLoggerInsufficientPrivilege");
        Filter f = new MockFilter();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setFilter(f);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setFilter for an anonymous logger with sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testSetFilter_AnonyLoggerSufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        Filter f = new MockFilter();
        assertNull(log.getFilter());
        log.setFilter(f);
        assertSame(f, log.getFilter());
    }

    /*
     * Test setFilter for an anonymous logger with insufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testSetFilter_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        Filter f = new MockFilter();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            assertNull(log.getFilter());
            log.setFilter(f);
            assertSame(f, log.getFilter());
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setFilter for a null-named mock logger with insufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setFilter",
            args = {java.util.logging.Filter.class}
        )
    })
    public void testSetFilter_NullNamedMockLoggerInsufficientPrivilege() {
        MockLogger mlog = new MockLogger(null, null);
        Filter f = new MockFilter();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            mlog.setFilter(f);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test getLevel & setLevel with normal value for a named logger, having
     * sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getLevel & setLevel with normal value for a named logger, having sufficient privilege.",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getLevel & setLevel with normal value for a named logger, having sufficient privilege.",
            method = "getLevel",
            args = {}
        )
    })
    public void testGetSetLevel_NamedLoggerSufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetLevel_NamedLoggerSufficientPrivilege");

        assertNull(log.getLevel());
        log.setLevel(Level.CONFIG);
        assertSame(Level.CONFIG, log.getLevel());
    }

    /*
     * Test getLevel & setLevel with null value, having sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getLevel & setLevel with null value, having sufficient privilege.",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies getLevel & setLevel with null value, having sufficient privilege.",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        )
    })
    public void testGetSetLevel_Null() {
        Logger log = Logger.getLogger("testGetSetLevel_Null");

        assertNull(log.getLevel());
        log.setLevel(null);
        assertNull(log.getLevel());
        log.setLevel(Level.CONFIG);
        log.setLevel(null);
        assertNull(log.getLevel());
    }

    /*
     * Test setLevel with normal value for a named logger, having insufficient
     * privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies setLevel with normal value for a named logger, having insufficient privilege.",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Verifies setLevel with normal value for a named logger, having insufficient privilege.",
            method = "getLevel",
            args = {}
        )
    })
    public void testGetSetLevel_NamedLoggerInsufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetLevel_NamedLoggerInsufficientPrivilege");
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setLevel(Level.CONFIG);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setLevel for an anonymous logger with sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        )
    })
    public void testSetLevel_AnonyLoggerSufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        assertNull(log.getLevel());
        log.setLevel(Level.CONFIG);
        assertSame(Level.CONFIG, log.getLevel());
    }

    /*
     * Test setLevel for an anonymous logger with insufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        )
    })
    public void testSetLevel_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            assertNull(log.getLevel());
            log.setLevel(Level.CONFIG);
            assertSame(Level.CONFIG, log.getLevel());
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setLevel for a null-named mock logger with insufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setLevel",
            args = {java.util.logging.Level.class}
        )
    })
    public void testSetLevel_NullNamedMockLoggerInsufficientPrivilege() {
        MockLogger mlog = new MockLogger(null, null);
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            mlog.setLevel(Level.CONFIG);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test getUseParentHandlers & setUseParentHandlers with normal value for a
     * named logger, having sufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUseParentHandlers",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUseParentHandlers",
            args = {boolean.class}
        )
    })
    public void testGetSetUseParentHandlers_NamedLoggerSufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetUseParentHandlers_NamedLoggerSufficientPrivilege");

        assertTrue(log.getUseParentHandlers());
        log.setUseParentHandlers(false);
        assertFalse(log.getUseParentHandlers());
    }

    /*
     * Test setUseParentHandlers with normal value for a named logger, having
     * insufficient privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUseParentHandlers",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUseParentHandlers",
            args = {boolean.class}
        )
    })
    public void testGetSetUseParentHandlers_NamedLoggerInsufficientPrivilege() {
        Logger log = Logger
                .getLogger("testGetSetUseParentHandlers_NamedLoggerInsufficientPrivilege");
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setUseParentHandlers(true);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setUseParentHandlers for an anonymous logger with sufficient
     * privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUseParentHandlers",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUseParentHandlers",
            args = {boolean.class}
        )
    })
    public void testSetUseParentHandlers_AnonyLoggerSufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        assertTrue(log.getUseParentHandlers());
        log.setUseParentHandlers(false);
        assertFalse(log.getUseParentHandlers());
    }

    /*
     * Test setUseParentHandlers for an anonymous logger with insufficient
     * privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUseParentHandlers",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUseParentHandlers",
            args = {boolean.class}
        )
    })
    public void testSetUseParentHandlers_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            assertTrue(log.getUseParentHandlers());
            log.setUseParentHandlers(false);
            assertFalse(log.getUseParentHandlers());
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setUseParentHandlers for a null-named mock logger with insufficient
     * privilege.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUseParentHandlers",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUseParentHandlers",
            args = {boolean.class}
        )
    })
    public void testSetUseParentHandlers_NullNamedMockLoggerInsufficientPrivilege() {
        MockLogger mlog = new MockLogger(null, null);
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            mlog.setUseParentHandlers(true);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test getParent() for root logger.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getParent() for root logger.",
        method = "getParent",
        args = {}
    )
    public void testGetParent_Root() {
        assertNull(Logger.getLogger("").getParent());
    }

    /*
     * Test getParent() for normal named loggers.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getParent() for normal named loggers.",
        method = "getParent",
        args = {}
    )
    public void testGetParent_NormalNamed() {
        Logger log = Logger.getLogger("testGetParent_NormalNamed");
        assertSame(log.getParent(), Logger.getLogger(""));
        Logger child = Logger.getLogger("testGetParent_NormalNamed.child");
        assertSame(child.getParent(), log);
        Logger child2 = Logger.getLogger("testGetParent_NormalNamed.a.b.c");
        assertSame(child2.getParent(), log);
    }

    /*
     * Test getParent() for anonymous loggers.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getParent() for anonymous loggers.",
        method = "getParent",
        args = {}
    )
    public void testGetParent_Anonymous() {
        assertSame(Logger.getAnonymousLogger().getParent(), Logger
                .getLogger(""));
    }

    /*
     * Test setParent(Logger) for the mock logger since it is advised not to
     * call this method on named loggers. Test normal conditions.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setParent",
        args = {java.util.logging.Logger.class}
    )
    public void testSetParent_Normal() {
        Logger log = new MockLogger(null, null);
        Logger parent = new MockLogger(null, null);
        assertNull(log.getParent());
        log.setParent(parent);
        assertSame(log.getParent(), parent);
    }

    /*
     * Test setParent(Logger) with null.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setParent",
        args = {java.util.logging.Logger.class}
    )
    public void testSetParent_Null() {
        try {
            (new MockLogger(null, null)).setParent(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test setParent(Logger), having insufficient privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setParent",
        args = {java.util.logging.Logger.class}
    )
    public void testSetParent_InsufficientPrivilege() {
        MockLogger log = new MockLogger(null, null);
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setParent(log);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setParent(Logger) with null, having insufficient privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setParent",
        args = {java.util.logging.Logger.class}
    )
    public void testSetParent_InsufficientPrivilegeNull() {
        MockLogger log = new MockLogger(null, null);
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setParent(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test setParent(Logger) for an anonymous logger with insufficient
     * privilege.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setParent",
        args = {java.util.logging.Logger.class}
    )
    public void testSetParent_AnonyLoggerInsufficientPrivilege() {
        Logger log = Logger.getAnonymousLogger();
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockSecurityManager());
        try {
            log.setParent(log);
            fail("Should throw SecurityException!");
        } catch (SecurityException e) {
        } finally {
            System.setSecurityManager(oldMan);
        }
    }

    /*
     * Test getName() for normal names.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getName() for normal names.",
        method = "getName",
        args = {}
    )
    public void testGetName_Normal() {
        Logger log = Logger.getLogger("testGetName_Normal");
        assertEquals("testGetName_Normal", log.getName());

        Logger mlog = new MockLogger("testGetName_Normal", null);
        assertEquals("testGetName_Normal", mlog.getName());
    }

    /*
     * Test getName() for empty name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getName() for empty name.",
        method = "getName",
        args = {}
    )
    public void testGetName_Empty() {
        Logger log = Logger.getLogger("");
        assertEquals("", log.getName());

        Logger mlog = new MockLogger("", null);
        assertEquals("", mlog.getName());
    }

    /*
     * Test getName() for null name.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getName() for null name.",
        method = "getName",
        args = {}
    )
    public void testGetName_Null() {
        Logger log = Logger.getAnonymousLogger();
        assertNull(log.getName());

        Logger mlog = new MockLogger(null, null);
        assertNull(mlog.getName());
    }

    /*
     * Test getResourceBundle() when it it not null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getResourceBundle() when it it not null.",
        method = "getResourceBundle",
        args = {}
    )
    public void testGetResourceBundle_Normal() {
        Logger log = Logger.getLogger("testGetResourceBundle_Normal",
                VALID_RESOURCE_BUNDLE);
        assertEquals(VALID_VALUE, log.getResourceBundle().getString(VALID_KEY));

        Logger mlog = new MockLogger(null, VALID_RESOURCE_BUNDLE);
        assertEquals(VALID_VALUE, mlog.getResourceBundle().getString(VALID_KEY));
    }

    /*
     * Test getResourceBundle() when it it null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getResourceBundle() when it it null.",
        method = "getResourceBundle",
        args = {}
    )
    public void testGetResourceBundle_Null() {
        Logger log = Logger.getLogger("testGetResourceBundle_Null", null);
        assertNull(log.getResourceBundle());

        Logger mlog = new MockLogger(null, null);
        assertNull(mlog.getResourceBundle());
    }


    /*
     * Test getResourceBundleName() when it it not null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getResourceBundleName() when it it not null.",
        method = "getResourceBundleName",
        args = {}
    )
    public void testGetResourceBundleName_Normal() {
        Logger log = Logger.getLogger("testGetResourceBundleName_Normal",
                VALID_RESOURCE_BUNDLE);
        assertEquals(VALID_RESOURCE_BUNDLE, log.getResourceBundleName());

        Logger mlog = new MockLogger(null, null);
        assertNull(mlog.getResourceBundleName());
    }

    /*
     * Test getResourceBundleName() when it it null.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies getResourceBundleName() when it it null.",
        method = "getResourceBundleName",
        args = {}
    )
    public void testGetResourceBundleName_Null() {
        Logger log = Logger.getLogger("testGetResourceBundleName_Null", null);
        assertNull(log.getResourceBundleName());

        Logger mlog = new MockLogger(null, null);
        assertNull(mlog.getResourceBundleName());
    }


    /*
     * Test isLoggable(Level).
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isLoggable",
        args = {java.util.logging.Level.class}
    )
    public void testIsLoggable() {
        MockLogger mlog = new MockLogger(null, null);
        assertNull(mlog.getLevel());
        assertNull(mlog.getParent());

        assertTrue(mlog.isLoggable(Level.SEVERE));
        assertTrue(mlog.isLoggable(Level.WARNING));
        assertTrue(mlog.isLoggable(Level.INFO));
        assertFalse(mlog.isLoggable(Level.CONFIG));
        assertFalse(mlog.isLoggable(Level.FINE));
        assertFalse(mlog.isLoggable(Level.ALL));
        assertTrue(mlog.isLoggable(Level.OFF));

        mlog.setLevel(Level.CONFIG);
        assertTrue(mlog.isLoggable(Level.SEVERE));
        assertTrue(mlog.isLoggable(Level.CONFIG));
        assertFalse(mlog.isLoggable(Level.ALL));
        assertTrue(mlog.isLoggable(Level.OFF));

        mlog.setLevel(Level.ALL);
        assertTrue(mlog.isLoggable(Level.ALL));
        assertTrue(mlog.isLoggable(Level.SEVERE));
        assertTrue(mlog.isLoggable(Level.OFF));

        mlog.setLevel(Level.OFF);
        assertFalse(mlog.isLoggable(Level.ALL));
        assertFalse(mlog.isLoggable(Level.SEVERE));
        assertFalse(mlog.isLoggable(Level.OFF));
    }

    /*
     * Test throwing(String, String, Throwable) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "throwing",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testThrowing_Normal() {
        Throwable t = new Throwable();
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.throwing("sourceClass", "sourceMethod", t);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "THROW");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters(), null);
        assertSame(r.getThrown(), t);


        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.throwing("sourceClass", "sourceMethod", t);

        // FINE is a level too low, message will be lost => empty
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test throwing(String, String, Throwable) with null values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "throwing",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testThrowing_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.throwing(null, null, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "THROW");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters(), null);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test entering(String, String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String) with normal values.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testEntering_StringString_Normal() {
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.entering("sourceClass", "sourceMethod");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "ENTRY");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters(), null);
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.entering("sourceClass", "sourceMethod");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test entering(String, String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String) with null values.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testEntering_StringString_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.entering(null, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "ENTRY");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters(), null);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test entering(String, String, Object) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String, Object) with normal values.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testEntering_StringStringObject_Normal() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.entering("sourceClass", "sourceMethod", param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "ENTRY {0}");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters()[0], param);
        assertEquals(1, r.getParameters().length);
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.entering("sourceClass", "sourceMethod", param);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test entering(String, String, Object) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String, Object) with null values.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testEntering_StringStringObject_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.entering(null, null, (Object) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "ENTRY {0}");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertEquals(r.getParameters().length, 1);
        assertNull(r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test entering(String, String, Object[]) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String, Object[]) with normal values.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testEntering_StringStringObjects_Normal() {
        Object[] params = new Object[2];
        params[0] = new Object();
        params[1] = new Object();
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.entering("sourceClass", "sourceMethod", params);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "ENTRY {0} {1}");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters()[0], params[0]);
        assertSame(r.getParameters()[1], params[1]);
        assertEquals(2, r.getParameters().length);
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.entering("sourceClass", "sourceMethod", params);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test entering(String, String, Object[]) with null class name and method
     * name and empty parameter array.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String, Object[]) with null class name and method name and empty parameter array.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testEntering_StringStringObjects_NullEmpty() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.entering(null, null, new Object[0]);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "ENTRY");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertEquals(0, r.getParameters().length);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test entering(String, String, Object[]) with null values with appropriate
     * logging level set.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies  entering(String, String, Object[]) with null values with appropriate logging level set.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testEntering_StringStringObjects_Null() {
        sharedLogger.setLevel(Level.FINER);
        sharedLogger.entering(null, null, (Object[]) null);
        // regression test for Harmony-1265
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(sharedLogger.getName(), r.getLoggerName());
        assertEquals("ENTRY", r.getMessage());
        assertSame(sharedLogger.getResourceBundleName(), r
                .getResourceBundleName());
        assertSame(sharedLogger.getResourceBundle(), r.getResourceBundle());
        assertNull(r.getSourceClassName());
        assertNull(r.getSourceMethodName());
        assertSame(Level.FINER, r.getLevel());
        assertNull(r.getParameters());
        assertNull(r.getThrown());
    }

    /*
     * Test entering(String, String, Object[]) with null values with
     * inappropriate logging level set.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies entering(String, String, Object[]) with null values with inappropriate logging level set.",
        method = "entering",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testEntering_StringStringObjects_NullDisabled() {
        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.entering(null, null, (Object[]) null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test exiting(String, String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies exiting(String, String) with normal values.",
        method = "exiting",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testExiting_StringString_Normal() {
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.exiting("sourceClass", "sourceMethod");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "RETURN");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.exiting("sourceClass", "sourceMethod");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test exiting(String, String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies exiting(String, String) with null values.",
        method = "exiting",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testExiting_StringString_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.exiting(null, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "RETURN");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters(), null);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test exiting(String, String, Object) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies exiting(String, String, Object) with normal values.",
        method = "exiting",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testExiting_StringStringObject_Normal() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.exiting("sourceClass", "sourceMethod", param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "RETURN {0}");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.FINER);
        assertSame(r.getParameters()[0], param);
        assertEquals(1, r.getParameters().length);
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.exiting("sourceClass", "sourceMethod", param);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test exiting(String, String, Object) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies exiting(String, String, Object) with null values.",
        method = "exiting",
        args = {java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testExiting_StringStringObject_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.exiting(null, null, (Object) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertEquals(r.getMessage(), "RETURN {0}");
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertEquals(r.getParameters().length, 1);
        assertNull(r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test config(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "config",
        args = {java.lang.String.class}
    )
    public void testConfig_Normal() {
        this.sharedLogger.setLevel(Level.CONFIG);
        this.sharedLogger.config("config msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "config msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.CONFIG);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.config("config again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test config(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies null as a parameter.",
        method = "config",
        args = {java.lang.String.class}
    )
    public void testConfig_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.CONFIG);
        child.config(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.CONFIG);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.config(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test fine(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "fine",
        args = {java.lang.String.class}
    )
    public void testFine_Normal() {
        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.fine("fine msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertEquals(r.getMessage(), "fine msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.CONFIG);
        this.sharedLogger.fine("fine again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test fine(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies fine(String) with null values.",
        method = "fine",
        args = {java.lang.String.class}
    )
    public void testFine_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINE);
        child.fine(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.CONFIG);
        this.sharedLogger.fine(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test finer(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies finer(String) with normal values.",
        method = "finer",
        args = {java.lang.String.class}
    )
    public void testFiner_Normal() {
        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.finer("finer msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "finer msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.finer("finer again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test finer(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies finer(String) with null values.",
        method = "finer",
        args = {java.lang.String.class}
    )
    public void testFiner_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINER);
        child.finer(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINER);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINE);
        this.sharedLogger.finer(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test finest(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies finest(String) with normal values.",
        method = "finest",
        args = {java.lang.String.class}
    )
    public void testFinest_Normal() {
        this.sharedLogger.setLevel(Level.FINEST);
        this.sharedLogger.finest("finest msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "finest msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINEST);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.finest("finest again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test finest(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies finest(String) with null values.",
        method = "finest",
        args = {java.lang.String.class}
    )
    public void testFinest_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.FINEST);
        child.finest(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINEST);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.FINER);
        this.sharedLogger.finest(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test info(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "info",
        args = {java.lang.String.class}
    )
    public void testInfo_Normal() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.info("info msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "info msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.WARNING);
        this.sharedLogger.info("info again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test info(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "info",
        args = {java.lang.String.class}
    )
    public void testInfo_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.info(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.WARNING);
        this.sharedLogger.info(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test warning(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "warning",
        args = {java.lang.String.class}
    )
    public void testWarning_Normal() {
        this.sharedLogger.setLevel(Level.WARNING);
        this.sharedLogger.warning("warning msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "warning msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.WARNING);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.SEVERE);
        this.sharedLogger.warning("warning again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test warning(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "warning",
        args = {java.lang.String.class}
    )
    public void testWarning_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.WARNING);
        child.warning(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.WARNING);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.SEVERE);
        this.sharedLogger.warning(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test severe(String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "severe",
        args = {java.lang.String.class}
    )
    public void testSevere_Normal() {
        this.sharedLogger.setLevel(Level.SEVERE);
        this.sharedLogger.severe("severe msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "severe msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.SEVERE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.severe("severe again");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test severe(String) with null values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "severe",
        args = {java.lang.String.class}
    )
    public void testSevere_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.SEVERE);
        child.severe(null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.SEVERE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.severe(null);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(Level, String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class}
    )
    public void testLog_LevelString_Normal() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.log(Level.INFO, "log(Level, String) msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "log(Level, String) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.log(Level.CONFIG, "log(Level, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.log(Level.OFF, "log(Level, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(Level, String) with null message.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class}
    )
    public void testLog_LevelString_NullMsg() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.log(Level.INFO, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(Level, String) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class}
    )
    public void testLog_LevelString_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.log(null, "log(Level, String) msg");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test log(Level, String, Object) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLog_LevelStringObject_Normal() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.log(Level.INFO, "log(Level, String, Object) msg",
                param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "log(Level, String, Object) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertSame(param, r.getParameters()[0]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.log(Level.CONFIG, "log(Level, String, Object) msg",
                param);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.log(Level.OFF, "log(Level, String, Object) msg",
                param);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(Level, String, Object) with null message and object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLog_LevelStringObject_NullMsgObj() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.log(Level.INFO, null, (Object) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertNull(r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(Level, String, Object) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLog_LevelStringObject_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.log(null, "log(Level, String, Object) msg",
                    new Object());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test log(Level, String, Object[]) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object[].class}
    )
            
    public void testLog_LevelStringObjects_Normal() {
        Object[] params = new Object[2];
        params[0] = new Object();
        params[1] = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.log(Level.INFO, "log(Level, String, Object[]) msg",
                params);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "log(Level, String, Object[]) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(2, r.getParameters().length);
        assertSame(params[0], r.getParameters()[0]);
        assertSame(params[1], r.getParameters()[1]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.log(Level.CONFIG, "log(Level, String, Object[]) msg",
                params);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.log(Level.OFF, "log(Level, String, Object[]) msg",
                params);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(Level, String, Object[]) with null message and object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLog_LevelStringObjects_NullMsgObj() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.log(Level.INFO, null, (Object[]) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(Level, String, Object[]) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLog_LevelStringObjects_NullLevel() {
        try {
            this.sharedLogger.log(null, "log(Level, String, Object[]) msg",
                    new Object[0]);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test log(Level, String, Throwable) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLog_LevelStringThrowable_Normal() {
        Throwable t = new Throwable();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.log(Level.INFO, "log(Level, String, Throwable) msg",
                t);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "log(Level, String, Throwable) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), t);

        this.sharedLogger.log(Level.CONFIG,
                "log(Level, String, Throwable) msg", t);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger
                .log(Level.OFF, "log(Level, String, Throwable) msg", t);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(Level, String, Throwable) with null message and throwable.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLog_LevelStringThrowable_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.log(Level.INFO, null, (Throwable) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(Level, String, Throwable) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLog_LevelStringThrowable_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.log(null, "log(Level, String, Throwable) msg",
                    new Throwable());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logp(Level, String, String, String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogp_LevelStringStringString_Normal() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logp(Level.INFO, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String) msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(), "logp(Level, String, String, String) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.logp(Level.CONFIG, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logp(Level.OFF, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logp(Level, String, String, String) with null message.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogp_LevelStringStringString_NullMsg() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.logp(Level.INFO, null, null, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logp(Level, String, String, String) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogp_LevelStringStringString_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logp(null, "sourceClass", "sourceMethod",
                    "logp(Level, String, String, String) msg");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logp(Level, String, String, String, Object) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogp_LevelStringStringStringObject_Normal() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logp(Level.INFO, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object) msg", param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logp(Level, String, String, String, Object) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertSame(param, r.getParameters()[0]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.logp(Level.CONFIG, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object) msg", param);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logp(Level.OFF, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object) msg", param);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logp(Level, String, String, String, Object) with null message and
     * object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogp_LevelStringStringStringObject_NullMsgObj() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.logp(Level.INFO, null, null, null, (Object) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertNull(r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logp(Level, String, String, String, Object) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
            
    public void testLogp_LevelStringStringStringObject_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logp(null, "sourceClass", "sourceMethod",
                    "logp(Level, String, String, String, Object) msg",
                    new Object());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logp(Level, String, String, String, Object[]) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
                    
    public void testLogp_LevelStringStringStringObjects_Normal() {
        Object[] params = new Object[2];
        params[0] = new Object();
        params[1] = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logp(Level.INFO, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object[]) msg", params);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logp(Level, String, String, String, Object[]) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(2, r.getParameters().length);
        assertSame(params[0], r.getParameters()[0]);
        assertSame(params[1], r.getParameters()[1]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.logp(Level.CONFIG, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object[]) msg", params);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logp(Level.OFF, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Object[]) msg", params);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logp(Level, String, String, String, Object[]) with null message and
     * object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLogp_LevelStringStringStringObjects_NullMsgObj() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.logp(Level.INFO, null, null, null, (Object[]) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logp(Level, String, String, String, Object[]) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLogp_LevelStringStringStringObjects_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logp(null, "sourceClass", "sourceMethod",
                    "logp(Level, String, String, String, Object[]) msg",
                    new Object[0]);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logp(Level, String, String, String, Throwable) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogp_LevelStringStringStringThrowable_Normal() {
        Throwable t = new Throwable();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logp(Level.INFO, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Throwable) msg", t);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logp(Level, String, String, String, Throwable) msg");
        assertSame(r.getResourceBundleName(), this.sharedLogger
                .getResourceBundleName());
        assertSame(r.getResourceBundle(), this.sharedLogger.getResourceBundle());
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), t);

        this.sharedLogger.logp(Level.CONFIG, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Throwable) msg", t);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logp(Level.OFF, "sourceClass", "sourceMethod",
                "logp(Level, String, String, String, Throwable) msg", t);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logp(Level, String, String, String, Throwable) with null message and
     * throwable.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogp_LevelStringStringStringThrowable_Null() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);

        child.setLevel(Level.INFO);
        child.logp(Level.INFO, null, null, null, (Throwable) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), child.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), parent.getResourceBundleName());
        assertSame(r.getResourceBundle(), parent.getResourceBundle());
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logp(Level, String, String, String, Throwable) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logp",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogp_LevelStringStringStringThrowable_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logp(null, "sourceClass", "sourceMethod",
                    "log(Level, String, String, String, Throwable) msg",
                    new Throwable());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logrb(Level, String, String, String, String) with normal values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogrb_LevelStringStringString_Normal() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String) msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String) msg");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE2);
        assertEquals(VALID_VALUE2, r.getResourceBundle().getString(VALID_KEY));
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        this.sharedLogger.logrb(Level.CONFIG, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logrb(Level.OFF, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String) msg");
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logrb(Level, String, String, String, String) with null message.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogrb_LevelStringStringString_NullMsg() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, null, null, null, null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String) with null level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogrb_LevelStringStringString_NullLevel() {
        try {
            this.sharedLogger.logrb(null, "sourceClass", "sourceMethod",
                    VALID_RESOURCE_BUNDLE2,
                    "logrb(Level, String, String, String, String) msg");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logrb(Level, String, String, String, String) with invalid resource
     * bundle.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testLogrb_LevelStringStringString_InvalidRes() {
        this.sharedLogger.setLevel(Level.ALL);
        this.sharedLogger.logrb(Level.ALL, "sourceClass", "sourceMethod",
                INVALID_RESOURCE_BUNDLE,
                "logrb(Level, String, String, String, String) msg");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String) msg");
        assertSame(r.getResourceBundleName(), INVALID_RESOURCE_BUNDLE);
        assertSame(r.getResourceBundle(), null);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.ALL);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String, String, Object) with normal
     * values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogrb_LevelStringStringStringObject_Normal() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object) msg",
                param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String, Object) msg");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE2);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertSame(param, r.getParameters()[0]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.logrb(Level.CONFIG, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object) msg",
                param);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logrb(Level.OFF, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object) msg",
                param);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logrb(Level, String, String, String, String, Object) with null
     * message and object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogrb_LevelStringStringStringObject_NullMsgObj() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, null, null, null, null,
                (Object) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(1, r.getParameters().length);
        assertNull(r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /**
     * java.util.logging.Logger#logrb(Level, String, String, String, String,
     * Object)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Regression test.",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void test_logrbLLevel_LString_LString_LObject_Security()
            throws Exception {
        // regression test for Harmony-1290
        SecurityManager originalSecurityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new MockSecurityManager());
            Logger.global.logrb(Level.OFF, null, null, "abc", "def");
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    /*
     * Test logrb(Level, String, String, String, String, Object) with null
     * level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogrb_LevelStringStringStringObject_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logrb(null, "sourceClass", "sourceMethod",
                    VALID_RESOURCE_BUNDLE2,
                    "logrb(Level, String, String, String, String, Object) msg",
                    new Object());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logrb(Level, String, String, String, String, Object) with invalid
     * resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class}
    )
    public void testLogrb_LevelStringStringStringObject_InvalidRes() {
        Object param = new Object();
        this.sharedLogger.setLevel(Level.ALL);
        this.sharedLogger.logrb(Level.ALL, "sourceClass", "sourceMethod",
                INVALID_RESOURCE_BUNDLE,
                "logrb(Level, String, String, String, String) msg", param);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String) msg");
        assertSame(r.getResourceBundleName(), INVALID_RESOURCE_BUNDLE);
        assertSame(r.getResourceBundle(), null);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.ALL);
        assertEquals(1, r.getParameters().length);
        assertSame(param, r.getParameters()[0]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String, String, Object[]) with normal
     * values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )      
    public void testLogrb_LevelStringStringStringObjects_Normal() {
        Object[] params = new Object[2];
        params[0] = new Object();
        params[1] = new Object();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object[]) msg",
                params);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String, Object[]) msg");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE2);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.INFO);
        assertEquals(2, r.getParameters().length);
        assertSame(params[0], r.getParameters()[0]);
        assertSame(params[1], r.getParameters()[1]);
        assertSame(r.getThrown(), null);

        this.sharedLogger.logrb(Level.CONFIG, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object[]) msg",
                params);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logrb(Level.OFF, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Object[]) msg",
                params);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logrb(Level, String, String, String, String, Object[]) with null
     * message and object.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLogrb_LevelStringStringStringObjects_NullMsgObj() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, null, null, null, null,
                (Object[]) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String, String, Object[]) with null
     * level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLogrb_LevelStringStringStringObjects_NullLevel() {
        try {
            this.sharedLogger.logrb(
                    null,
                    "sourceClass",
                    "sourceMethod",
                    VALID_RESOURCE_BUNDLE2,
                    "logrb(Level, String, String, String, String, Object[]) msg",
                    new Object[0]);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logrb(Level, String, String, String, String, Object[]) with invalid
     * resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class}
    )
    public void testLogrb_LevelStringStringStringObjects_InvalidRes() {
        Object[] params = new Object[2];
        params[0] = new Object();
        params[1] = new Object();
        this.sharedLogger.setLevel(Level.ALL);
        this.sharedLogger.logrb(Level.ALL, "sourceClass", "sourceMethod",
                INVALID_RESOURCE_BUNDLE,
                "logrb(Level, String, String, String, String) msg", params);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String) msg");
        assertSame(r.getResourceBundleName(), INVALID_RESOURCE_BUNDLE);
        assertSame(r.getResourceBundle(), null);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.ALL);
        assertEquals(2, r.getParameters().length);
        assertSame(params[0], r.getParameters()[0]);
        assertSame(params[1], r.getParameters()[1]);
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String, String, Throwable) with normal
     * values.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogrb_LevelStringStringStringThrowable_Normal() {
        Throwable t = new Throwable();
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.parse("1611"), "sourceClass",
                "sourceMethod", VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Throwable) msg",
                t);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
                "logrb(Level, String, String, String, String, Throwable) msg");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE2);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.parse("1611"));
        assertNull(r.getParameters());
        assertSame(r.getThrown(), t);
        assertNull(Level.parse("1611").getResourceBundleName());

        this.sharedLogger.logrb(Level.CONFIG, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Throwable) msg",
                t);
        assertTrue(CallVerificationStack.getInstance().empty());
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.logrb(Level.OFF, "sourceClass", "sourceMethod",
                VALID_RESOURCE_BUNDLE2,
                "logrb(Level, String, String, String, String, Throwable) msg",
                t);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test logrb(Level, String, String, String, String, Throwable) with null
     * message and throwable.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogrb_LevelStringTStringStringhrowable_NullMsgObj() {
        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.logrb(Level.INFO, null, null, null, null,
                (Throwable) null);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertNull(r.getMessage());
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test logrb(Level, String, String, String, String, Throwable) with null
     * level.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogrb_LevelStringStringStringThrowable_NullLevel() {
        // this.sharedLogger.setLevel(Level.OFF);
        try {
            this.sharedLogger.logrb(
                    null,
                    "sourceClass",
                    "sourceMethod",
                    VALID_RESOURCE_BUNDLE2,
                    "log(Level, String, String, String, String, Throwable) msg",
                    new Throwable());
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test logrb(Level, String, String, String, String, Throwable) with invalid
     * resource bundle.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "logrb",
        args = {java.util.logging.Level.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class}
    )
    public void testLogrb_LevelStringStringStringThrowable_InvalidRes() {
        Throwable t = new Throwable();
        this.sharedLogger.setLevel(Level.ALL);
        this.sharedLogger.logrb(Level.ALL, "sourceClass", "sourceMethod",
                INVALID_RESOURCE_BUNDLE,
                "logrb(Level, String, String, String, String) msg", t);
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), this.sharedLogger.getName());
        assertEquals(r.getMessage(),
        "logrb(Level, String, String, String, String) msg");
        assertSame(r.getResourceBundleName(), INVALID_RESOURCE_BUNDLE);
        assertSame(r.getResourceBundle(), null);
        assertSame(r.getSourceClassName(), "sourceClass");
        assertSame(r.getSourceMethodName(), "sourceMethod");
        assertSame(r.getLevel(), Level.ALL);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), t);
    }

    /*
     * Test log(LogRecord) for a normal log record. Meanwhile the logger has an
     * appropriate level, no filter, no parent.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_LogRecord_AppropriateLevelNoFilterNoParent() {
        LogRecord r = new LogRecord(Level.INFO,
        "testLog_LogRecord_AppropriateLevelNoFilterNoParent");

        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.log(r);

        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), null);
        assertEquals(r.getMessage(),
        "testLog_LogRecord_AppropriateLevelNoFilterNoParent");
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(LogRecord) with null log record.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_LogRecord_Null() {
        this.sharedLogger.setLevel(Level.INFO);
        try {
            this.sharedLogger.log(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /*
     * Test log(LogRecord) for a normal log record. Meanwhile the logger has an
     * inappropriate level, no filter, no parent.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_LogRecord_InppropriateLevelNoFilterNoParent() {
        LogRecord r = new LogRecord(Level.INFO,
                "testLog_LogRecord_InppropriateLevelNoFilterNoParent");

        this.sharedLogger.setLevel(Level.WARNING);
        this.sharedLogger.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());

        r.setLevel(Level.OFF);
        this.sharedLogger.setLevel(Level.OFF);
        this.sharedLogger.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test log(LogRecord) for a normal log record. Meanwhile the logger has an
     * appropriate level, a filter that accepts the fed log record, no parent.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_LogRecord_AppropriateLevelTrueFilterNoParent() {
        LogRecord r = new LogRecord(Level.INFO,
                "testLog_LogRecord_AppropriateLevelTrueFilterNoParent");

        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.setFilter(new MockTrueFilter());
        this.sharedLogger.log(r);

        // pop twice, one pushed by mock handler, one by true mock filter
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        assertSame(r.getLoggerName(), null);
        assertEquals(r.getMessage(),
                "testLog_LogRecord_AppropriateLevelTrueFilterNoParent");
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test log(LogRecord) for a normal log record. Meanwhile the logger has an
     * appropriate level, a filter that rejects the fed log record, no parent.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_LogRecord_AppropriateLevelFalseFilterNoParent() {
        LogRecord r = new LogRecord(Level.INFO,
                "testLog_LogRecord_AppropriateLevelFalseFilterNoParent");

        this.sharedLogger.setLevel(Level.INFO);
        this.sharedLogger.setFilter(new MockFilter());
        this.sharedLogger.log(r);

        // pop only once, pushed by mock filter
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        assertSame(r.getLoggerName(), null);
        assertEquals(r.getMessage(),
                "testLog_LogRecord_AppropriateLevelFalseFilterNoParent");
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
    }

    /*
     * Test that the parent's handler is notified for a new log record when
     * getUseParentHandlers() is true.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_ParentInformed() {
        Logger child = new MockLogger("childLogger", VALID_RESOURCE_BUNDLE);
        Logger parent = new MockParentLogger("parentLogger",
                VALID_RESOURCE_BUNDLE2);

        child.setParent(parent);
        child.setLevel(Level.INFO);
        parent.setLevel(Level.INFO);
        parent.addHandler(new MockHandler());
        LogRecord r = new LogRecord(Level.INFO, "testLog_ParentInformed");
        child.log(r);
        assertTrue(child.getUseParentHandlers());
        // pop only once, pushed by the parent logger's handler, not by the
        // parent itself!
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());
        assertSame(r.getLoggerName(), null);
        assertEquals(r.getMessage(), "testLog_ParentInformed");
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.INFO);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);

        // set the child logger to disabling level
        child.setLevel(Level.SEVERE);
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());

        // set the parent logger to disabling level
        child.setLevel(Level.INFO);
        parent.setLevel(Level.SEVERE);
        child.log(r);
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        // set the child logger off
        child.setLevel(Level.OFF);
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());

        // set the record off
        r.setLevel(Level.OFF);
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test that the ancestor's handler is notified for a new log record when
     * getUseParentHandlers() is true.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_AncestorInformed() {
        Logger child = new MockLogger("childLogger", VALID_RESOURCE_BUNDLE);
        Logger parent = new MockParentLogger("parentLogger",
                VALID_RESOURCE_BUNDLE2);
        Logger ancestor = new MockParentLogger("ancestorLogger",
                VALID_RESOURCE_BUNDLE3);

        child.setParent(parent);
        parent.setParent(ancestor);
        child.setLevel(Level.INFO);
        parent.setLevel(Level.INFO);
        ancestor.setLevel(Level.OFF);
        ancestor.addHandler(new MockHandler());
        LogRecord r = new LogRecord(Level.INFO, "testLog_AncestorInformed");
        child.log(r);
        assertTrue(child.getUseParentHandlers());
        assertTrue(parent.getUseParentHandlers());
        // pop only once, pushed by the ancestor's logger's handler, not by the
        // parent itself!
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        // set parent's level to a disabling one
        parent.setLevel(Level.WARNING);
        child.log(r);
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        // set child's level to a disabling one
        parent.setLevel(Level.INFO);
        child.setLevel(Level.WARNING);
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());

        // set parent's useParentHandlers to false
        parent.setLevel(Level.INFO);
        child.setLevel(Level.INFO);
        parent.setUseParentHandlers(false);
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test that the parent's handler is notified for a new log record when
     * getUseParentHandlers() is false.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_ParentNotInformed() {
        Logger child = new MockLogger("childLogger", VALID_RESOURCE_BUNDLE);
        Logger parent = new MockParentLogger("parentLogger",
                VALID_RESOURCE_BUNDLE2);

        child.setParent(parent);
        child.setLevel(Level.INFO);
        parent.setLevel(Level.INFO);
        parent.addHandler(new MockHandler());
        LogRecord r = new LogRecord(Level.INFO, "testLog_ParentInformed");
        child.setUseParentHandlers(false);
        child.log(r);
        assertFalse(child.getUseParentHandlers());
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test that a logger with null level and no parent. Defaulted to
     * Level.INFO.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_NullLevelNoParent() {
        LogRecord r = new LogRecord(Level.INFO, "testLog_NullLevelNoParent");
        assertNull(this.sharedLogger.getLevel());
        assertNull(this.sharedLogger.getParent());
        assertTrue(this.sharedLogger.isLoggable(r.getLevel()));
        this.sharedLogger.log(r);
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());
        assertNull(this.sharedLogger.getLevel());

        r.setLevel(Level.WARNING);
        assertTrue(this.sharedLogger.isLoggable(r.getLevel()));
        this.sharedLogger.log(r);
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        r.setLevel(Level.CONFIG);
        this.sharedLogger.log(r);
        assertFalse(this.sharedLogger.isLoggable(r.getLevel()));
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test that a logger inherits its parent level when its level is null.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_NullLevelHasParent() {
        Logger child = new MockLogger("childLogger", VALID_RESOURCE_BUNDLE);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);
        parent.setLevel(Level.FINER);

        assertNull(child.getLevel());

        LogRecord r = new LogRecord(Level.FINE, "testLog_NullLevelHasParent");
        child.log(r);
        assertTrue(child.isLoggable(r.getLevel()));
        // pop only once, pushed by the child logger's handler
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());

        assertSame(r.getLoggerName(), null);
        assertEquals(r.getMessage(), "testLog_NullLevelHasParent");
        assertSame(r.getResourceBundleName(), null);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
        assertNull(child.getLevel());

        // set the parent logger to disabling level
        parent.setLevel(Level.CONFIG);
        assertFalse(child.isLoggable(r.getLevel()));
        child.log(r);
        assertTrue(CallVerificationStack.getInstance().empty());
        assertNull(child.getLevel());

        // test ancestor
        Logger ancestor = new MockLogger("ancestorLogger",
                VALID_RESOURCE_BUNDLE3);
        parent.setParent(ancestor);
        parent.setLevel(null);
        parent.setUseParentHandlers(false);
        ancestor.setLevel(Level.ALL);
        child.log(r);
        assertTrue(child.isLoggable(r.getLevel()));
        assertSame(r, CallVerificationStack.getInstance().pop());
        assertTrue(CallVerificationStack.getInstance().empty());
        assertNull(child.getLevel());
        assertNull(parent.getLevel());
    }

    /*
     * Test that a logger with null resource bundle and no parent. Defaulted to
     * null.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_NullResNoParent() {
        Logger log = new MockLogger("Logger", null);
        log.addHandler(new MockHandler());
        log.setLevel(Level.FINE);

        assertNull(log.getResourceBundle());
        assertNull(log.getResourceBundleName());
        assertNull(log.getParent());
        log.log(Level.INFO, "testLog_NullResNoParent");
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());
        assertNull(log.getResourceBundle());
        assertNull(log.getResourceBundleName());
        assertNull(r.getResourceBundle());
        assertNull(r.getResourceBundleName());
    }

    /*
     * Test that a logger inherits its parent resource bundle when its resource
     * bundle is null.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_NullResHasParent() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", VALID_RESOURCE_BUNDLE2);
        child.addHandler(new MockHandler());
        child.setParent(parent);
        parent.setLevel(Level.FINER);
        assertNull(child.getResourceBundle());
        assertNull(child.getResourceBundleName());

        child.log(Level.FINE, "testLog_NullResHasParent");
        // pop only once, pushed by the child logger's handler
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());

        assertSame(r.getLoggerName(), "childLogger");
        assertEquals(r.getMessage(), "testLog_NullResHasParent");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE2);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
        assertNull(child.getResourceBundle());
        assertNull(child.getResourceBundleName());
    }

    /*
     * Test that a logger inherits its ancestor's resource bundle when its
     * resource bundle and its parent's resource bundle are both null.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_NullResHasAncestor() {
        Logger child = new MockLogger("childLogger", null);
        Logger parent = new MockLogger("parentLogger", null);
        Logger ancestor = new MockLogger("ancestorLogger",
                VALID_RESOURCE_BUNDLE3);
        child.addHandler(new MockHandler());
        child.setParent(parent);
        parent.setParent(ancestor);
        parent.setLevel(Level.FINER);
        assertNull(child.getResourceBundle());
        assertNull(child.getResourceBundleName());

        child.log(Level.FINE, "testLog_NullResHasAncestor");
        // pop only once, pushed by the child logger's handler
        LogRecord r = (LogRecord) CallVerificationStack.getInstance().pop();
        assertTrue(CallVerificationStack.getInstance().empty());

        assertSame(r.getLoggerName(), "childLogger");
        assertEquals(r.getMessage(), "testLog_NullResHasAncestor");
        assertSame(r.getResourceBundleName(), VALID_RESOURCE_BUNDLE3);
        assertSame(r.getSourceClassName(), null);
        assertSame(r.getSourceMethodName(), null);
        assertSame(r.getLevel(), Level.FINE);
        assertNull(r.getParameters());
        assertSame(r.getThrown(), null);
        assertNull(child.getResourceBundle());
        assertNull(child.getResourceBundleName());
    }

    /*
     * Test when one handler throws an exception.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {java.util.logging.LogRecord.class}
    )
    public void testLog_ExceptionalHandler() {
        MockLogger l = new MockLogger("testLog_ExceptionalHandler", null);
        l.addHandler(new MockExceptionalHandler());
        l.addHandler(new MockHandler());
        try {
            l.severe("testLog_ExceptionalHandler");
            fail("Should throw RuntimeException!");
        } catch (RuntimeException e) {
        }
        assertTrue(CallVerificationStack.getInstance().empty());
    }

    /*
     * Test whether privileged code is used to load resource bundles.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAnonymousLogger",
        args = {java.lang.String.class}
    )
    public void testLoadResourceBundle() {
        // 
        SecurityManager oldMan = System.getSecurityManager();
        System.setSecurityManager(new MockNoLoadingClassSecurityManager());
        try {
            Logger.getAnonymousLogger(VALID_RESOURCE_BUNDLE);
        } finally {
            System.setSecurityManager(oldMan);
        }
    }


    /**
     * java.util.logging.Logger#logrb(Level, String, String, String, String,
     * Object)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void test_init_logger() throws Exception {
        Properties p = new Properties();
        p.put("testGetLogger_Normal_ANewLogger2.level", "ALL");
        LogManager.getLogManager().readConfiguration(
                EnvironmentHelper.PropertiesToInputStream(p));

        assertNull(LogManager.getLogManager().getLogger(
                "testGetLogger_Normal_ANewLogger2"));
        SecurityManager originalSecurityManager = System.getSecurityManager();
        try {
            Logger logger = Logger
                            .getLogger("testGetLogger_Normal_ANewLogger2");
            System.setSecurityManager(new MockSecurityManager());
            try {
                logger.setLevel(Level.ALL);
                fail("should throw SecurityException");
            } catch (SecurityException e) {
                // expected
            }
            try {
                logger.setParent(Logger.getLogger(""));
                fail("should throw SecurityException");
            } catch (SecurityException e) {
                // expected
            }
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }


    /*
     * A mock logger, used to test the protected constructors and fields.
     */
    public static class MockLogger extends Logger {

        public MockLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }
    }

    /*
     * A mock logger, used to test inheritance.
     */
    public static class MockParentLogger extends Logger {

        public MockParentLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }

        public void log(LogRecord record) {
            CallVerificationStack.getInstance().push(record);
            super.log(record);
        }

    }

    /*
     * A mock handler, used to validate the expected method is called with the
     * expected parameters.
     */
    public static class MockHandler extends Handler {

        public void close() {
            // System.out.println("close!");
        }

        public void flush() {
            // System.out.println("flushed!");
        }

        public void publish(LogRecord record) {
            // System.out.println("publish!");
            CallVerificationStack.getInstance().push(record);
        }
    }

    /*
     * A mock handler that throws an exception when publishing a log record.
     */
    public static class MockExceptionalHandler extends Handler {

        public void close() {
            // System.out.println("close!");
        }

        public void flush() {
            // System.out.println("flushed!");
        }

        public void publish(LogRecord record) {
            // System.out.println("publish!");
            throw new RuntimeException();
        }
    }

    /*
     * Used to grant all permissions except logging control.
     */
    public static class MockSecurityManager extends SecurityManager {

        public MockSecurityManager() {
        }

        public void checkPermission(Permission perm) {
            // grant all permissions except logging control
            if (perm instanceof LoggingPermission) {
                throw new SecurityException();
            }
        }

        public void checkPermission(Permission perm, Object context) {
            // grant all permissions except logging control
            if (perm instanceof LoggingPermission) {
                throw new SecurityException();
            }
        }
    }

    /*
     * Used to grant all permissions except getting class loader.
     */
    public static class MockNoLoadingClassSecurityManager extends
            SecurityManager {

        public MockNoLoadingClassSecurityManager() {
        }

        public void checkPermission(Permission perm) {
            // grant all permissions except getting class loader
            if (perm instanceof RuntimePermission) {
                if ("getClassLoader".equals(perm.getName())) {
                    throw new SecurityException();
                }
            }
        }

        public void checkPermission(Permission perm, Object context) {
            // grant all permissions except logging control
            if (perm instanceof LoggingPermission) {
                throw new SecurityException();
            }
        }
    }


    /*
     * A mock filter, always return false.
     */
    public static class MockFilter implements Filter {

        public boolean isLoggable(LogRecord record) {
            CallVerificationStack.getInstance().push(record);
            return false;
        }
    }

    /*
     * A mock filter, always return true.
     */
    public static class MockTrueFilter implements Filter {

        public boolean isLoggable(LogRecord record) {
            CallVerificationStack.getInstance().push(record);
            return true;
        }
    }
}
