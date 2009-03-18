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



import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import junit.framework.TestCase;

import org.apache.harmony.logging.tests.java.util.logging.HandlerTest.NullOutputStream;
import org.apache.harmony.logging.tests.java.util.logging.util.EnvironmentHelper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.Permission;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;

/**
 * 
 * add/get logger(dot)
 * 
 */
@TestTargetClass(LogManager.class) 
public class LogManagerTest extends TestCase {

    private static final String FOO = "LogManagerTestFoo";

    LogManager mockManager;

    LogManager manager = LogManager.getLogManager();

        MockPropertyChangeListener listener;

    Properties props;

    private static String className = LogManagerTest.class.getName();

    static Handler handler = null;

    static final String CONFIG_CLASS = "java.util.logging.config.class";

    static final String CONFIG_FILE = "java.util.logging.config.file";

    static final String MANAGER_CLASS = "java.util.logging.config.manager";

    static final SecurityManager securityManager = System.getSecurityManager();

    static final String clearPath = System.getProperty("clearpath");

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockManager = new MockLogManager();
        //        listener = new MockPropertyChangeListener();
        handler = new MockHandler();
        props = new Properties();
        props.put("handlers", className + "$MockHandler " + className + "$MockHandler");
        props.put("java.util.logging.FileHandler.pattern", "%h/java%u.log");
        props.put("java.util.logging.FileHandler.limit", "50000");
        props.put("java.util.logging.FileHandler.count", "5");
        props.put("java.util.logging.FileHandler.formatter", "java.util.logging.XMLFormatter");
        props.put(".level", "FINE");
        props.put("java.util.logging.ConsoleHandler.level", "OFF");
        props.put("java.util.logging.ConsoleHandler.formatter","java.util.logging.SimpleFormatter");
        props.put("LogManagerTestFoo.handlers", "java.util.logging.ConsoleHandler");
        props.put("LogManagerTestFoo.level", "WARNING");
    }
    


    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        handler = null;
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "LogManager",
        args = {}
    )
    public void testLogManager() {
       class TestLogManager extends LogManager {
           public TestLogManager() {
               super();
           }
       }
       TestLogManager tlm = new TestLogManager();
       assertNotNull(tlm.toString());
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies NullPointerException.",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies NullPointerException.",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddGetLogger() {
        Logger log = new MockLogger(FOO, null);
        Logger foo = mockManager.getLogger(FOO);
        assertNull(foo);
        assertTrue(mockManager.addLogger(log));
        foo = mockManager.getLogger(FOO);
        assertSame(foo, log);
        assertNull(foo.getParent());

        try {
            mockManager.addLogger(null);
            fail("add null should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            mockManager.getLogger(null);
            fail("get null should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        assertNull(mockManager.getLogger("bad name"));

        Enumeration<String> enumar = mockManager.getLoggerNames();
        int i = 0;
        while (enumar.hasMoreElements()) {
            String name = enumar.nextElement();
            i++;
            assertEquals(FOO, name);
        }
        assertEquals(i, 1);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddGetLogger_duplicateName() {
        // add logger with duplicate name has no effect
        Logger foo = new MockLogger(FOO, null);
        Logger foo2 = new MockLogger(FOO, null);
        assertTrue(mockManager.addLogger(foo));
        assertSame(foo, mockManager.getLogger(FOO));
        assertFalse(mockManager.addLogger(foo2));
        assertSame(foo, mockManager.getLogger(FOO));
        Enumeration<String> enumar = mockManager.getLoggerNames();
        int i = 0;
        while (enumar.hasMoreElements()) {
            enumar.nextElement();
            i++;
        }
        assertEquals(1, i);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddGetLogger_Hierachy() {
        Logger foo = new MockLogger("testAddGetLogger_Hierachy.foo", null);
        Logger child = new MockLogger("testAddGetLogger_Hierachy.foo.child", null);
        Logger fakeChild = new MockLogger("testAddGetLogger_Hierachy.foo2.child", null);
        Logger grandson = new MockLogger("testAddGetLogger_Hierachy.foo.child.grandson", null);
        Logger otherChild = new MockLogger("testAddGetLogger_Hierachy.foo.child", null);
        assertNull(foo.getParent());
        assertNull(child.getParent());
        assertNull(grandson.getParent());
        assertNull(otherChild.getParent());

        // whenever a logger is added to a LogManager, hierarchy will be updated
        // accordingly
        assertTrue(mockManager.addLogger(child));
        assertNull(child.getParent());

        assertTrue(mockManager.addLogger(fakeChild));
        assertNull(fakeChild.getParent());

        assertTrue(mockManager.addLogger(grandson));
        assertSame(child, grandson.getParent());

        assertTrue(mockManager.addLogger(foo));
        assertSame(foo, child.getParent());
        assertNull(foo.getParent());
        assertNull(fakeChild.getParent());

        // but for non-mock LogManager, foo's parent should be root
        assertTrue(manager.addLogger(foo));
        assertSame(manager.getLogger(""), manager.getLogger("testAddGetLogger_Hierachy.foo")
                .getParent());

        // if we add one logger to two LogManager, parent will changed
        assertTrue(manager.addLogger(otherChild));
        assertTrue(manager.addLogger(grandson));
        assertSame(foo, otherChild.getParent());
        assertSame(otherChild, grandson.getParent());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddLoggerReverseOrder() {
        Logger root = new MockLogger("testAddLoggerReverseOrder", null);
        Logger foo = new MockLogger("testAddLoggerReverseOrder.foo", null);
        Logger fooChild = new MockLogger("testAddLoggerReverseOrder.foo.child", null);
        Logger fooGrandChild = new MockLogger("testAddLoggerReverseOrder.foo.child.grand", null);
        Logger fooGrandChild2 = new MockLogger("testAddLoggerReverseOrder.foo.child.grand2", null);

        Logger realRoot = manager.getLogger("");

        manager.addLogger(fooGrandChild);
        assertEquals(realRoot, fooGrandChild.getParent());

        manager.addLogger(root);
        assertSame(root, fooGrandChild.getParent());
        assertSame(realRoot, root.getParent());

        manager.addLogger(foo);
        assertSame(root, foo.getParent());
        assertSame(foo, fooGrandChild.getParent());

        manager.addLogger(fooGrandChild2);
        assertSame(foo, fooGrandChild2.getParent());
        assertSame(foo, fooGrandChild.getParent());

        manager.addLogger(fooChild);
        assertSame(fooChild, fooGrandChild2.getParent());
        assertSame(fooChild, fooGrandChild.getParent());
        assertSame(foo, fooChild.getParent());
        assertSame(root, foo.getParent());
        assertSame(realRoot, root.getParent());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "addLogger",
        args = {java.util.logging.Logger.class}
    )
    public void testAddSimiliarLogger() {
        Logger root = new MockLogger("testAddSimiliarLogger", null);
        Logger foo = new MockLogger("testAddSimiliarLogger.foo", null);
        Logger similiarFoo = new MockLogger("testAddSimiliarLogger.fop", null);
        Logger fooo = new MockLogger("testAddSimiliarLogger.fooo", null);
        Logger fooChild = new MockLogger("testAddSimiliarLogger.foo.child", null);
        Logger similiarFooChild = new MockLogger("testAddSimiliarLogger.fop.child", null);
        Logger foooChild = new MockLogger("testAddSimiliarLogger.fooo.child", null);

        manager.addLogger(root);
        manager.addLogger(fooChild);
        manager.addLogger(similiarFooChild);
        manager.addLogger(foooChild);
        assertSame(root, fooChild.getParent());
        assertSame(root, similiarFooChild.getParent());
        assertSame(root, foooChild.getParent());

        manager.addLogger(foo);
        assertSame(foo, fooChild.getParent());
        assertSame(root, similiarFooChild.getParent());
        assertSame(root, foooChild.getParent());

        manager.addLogger(similiarFoo);
        assertSame(foo, fooChild.getParent());
        assertSame(similiarFoo, similiarFooChild.getParent());
        assertSame(root, foooChild.getParent());

        manager.addLogger(fooo);
        assertSame(fooo, foooChild.getParent());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddGetLogger_nameWithSpace() {
        Logger foo = new MockLogger(FOO, null);
        Logger fooBeforeSpace = new MockLogger(FOO + " ", null);
        Logger fooAfterSpace = new MockLogger(" " + FOO, null);
        Logger fooWithBothSpace = new MockLogger(" " + FOO + " ", null);
        assertTrue(mockManager.addLogger(foo));
        assertTrue(mockManager.addLogger(fooBeforeSpace));
        assertTrue(mockManager.addLogger(fooAfterSpace));
        assertTrue(mockManager.addLogger(fooWithBothSpace));

        assertSame(foo, mockManager.getLogger(FOO));
        assertSame(fooBeforeSpace, mockManager.getLogger(FOO + " "));
        assertSame(fooAfterSpace, mockManager.getLogger(" " + FOO));
        assertSame(fooWithBothSpace, mockManager.getLogger(" " + FOO + " "));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Doesn't verify NullPointerException",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        )
    })
    public void testAddGetLogger_addRoot() {
        Logger foo = new MockLogger(FOO, null);
        Logger fooChild = new MockLogger(FOO + ".child", null);
        Logger other = new MockLogger("other", null);
        Logger root = new MockLogger("", null);
        assertNull(foo.getParent());
        assertNull(root.getParent());
        assertNull(other.getParent());

        // add root to mock logmanager and it works as "root" logger
        assertTrue(mockManager.addLogger(foo));
        assertTrue(mockManager.addLogger(other));
        assertTrue(mockManager.addLogger(fooChild));
        assertNull(foo.getParent());
        assertNull(other.getParent());
        assertSame(foo, fooChild.getParent());

        assertTrue(mockManager.addLogger(root));
        assertSame(root, foo.getParent());
        assertSame(root, other.getParent());
        assertNull(root.getParent());

        // try to add root logger to non-mock LogManager, no effect
        assertFalse(manager.addLogger(root));
        assertNotSame(root, manager.getLogger(""));
    }

    /**
     * @tests java.util.logging.LogManager#addLogger(Logger)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addLogger",
            args = {java.util.logging.Logger.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogManager",
            args = {}
        )
    })
    public void test_addLoggerLLogger_Security() throws Exception {
        // regression test for Harmony-1286
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                
            }
        });
        try {
            LogManager manager = LogManager.getLogManager();
            manager.addLogger(new MockLogger("mock", null));
            manager.addLogger(new MockLogger("mock.child", null));
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testDefaultLoggerProperties() throws Exception {
        // mock LogManager has no default logger
        assertNull(mockManager.getLogger(""));
        assertNull(mockManager.getLogger("global"));

        // non-mock LogManager has two default logger
        Logger global = manager.getLogger("global");
        Logger root = manager.getLogger("");

        assertSame(global, Logger.global);
        assertSame(root, global.getParent());

        // root properties
        manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        assertNull(root.getFilter());
        assertEquals(2, root.getHandlers().length);
        assertEquals(Level.FINE, root.getLevel());
        assertEquals("", root.getName());
        assertSame(root.getParent(), null);
        // This test sometimes fails if other tests are run before this one.
        assertNull(root.getResourceBundle());
        assertNull(root.getResourceBundleName());
        assertTrue(root.getUseParentHandlers());

    }

    /*
     * test for method public Logger getLogger(String name) 
     * test covers following usecases: 
     * case 1: test default and valid value 
     * case 2: test throw NullPointerException 
     * case 3: test bad name
     * case 4: check correct tested value
     */
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getLoggerNames",
            args = {}
        )
    })
    public void testGetLogger() throws Exception {

        // case 1: test default and valid value
        Logger log = new MockLogger(FOO, null);
        Logger foo = mockManager.getLogger(FOO);
        assertNull("Logger should be null", foo);
        assertTrue("logger was't registered successfully", mockManager.addLogger(log));
        foo = mockManager.getLogger(FOO);
        assertSame("two loggers not refer to the same object", foo, log);
        assertNull("logger foo should not haven parent", foo.getParent());

        // case 2: test throw NullPointerException
        try {
            mockManager.getLogger(null);
            fail("get null should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        // case 3: test bad name
        assertNull("LogManager should not have logger with unforeseen name", mockManager
                .getLogger("bad name"));

        // case 4: check correct tested value
        Enumeration<String> enumar = mockManager.getLoggerNames();
        int i = 0;
        while (enumar.hasMoreElements()) {
            String name = enumar.nextElement();
            i++;
            assertEquals("name logger should be equal to foreseen name", FOO, name);
        }
        assertEquals("LogManager should contain one element", 1, i);
    }

    /*
     * test for method public Logger getLogger(String name)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getLogger",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "",
            method = "getLoggerNames",
            args = {}
        )
    })
    public void testGetLogger_duplicateName() throws Exception {
        // test duplicate name
        // add logger with duplicate name has no effect
        mockManager.reset();
        Logger foo2 = new MockLogger(FOO, null);
        Logger foo3 = new MockLogger(FOO, null);
        mockManager.addLogger(foo2);
        assertSame(foo2, mockManager.getLogger(FOO));
        mockManager.addLogger(foo3);
        assertSame(foo2, mockManager.getLogger(FOO));

        Enumeration<String> enumar2 = mockManager.getLoggerNames();
        int i = 0;
        while (enumar2.hasMoreElements()) {
            enumar2.nextElement();
            i++;
        }
        assertEquals(1, i);
    }

    /*
     * test for method public Logger getLogger(String name)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_hierachy() throws Exception {
        // test hierachy
        Logger foo = new MockLogger("testGetLogger_hierachy.foo", null);
        // but for non-mock LogManager, foo's parent should be root
        assertTrue(manager.addLogger(foo));
        assertSame(manager.getLogger(""), manager.getLogger("testGetLogger_hierachy.foo")
                .getParent());
    }

    /*
     * test for method public Logger getLogger(String name)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getLogger",
        args = {java.lang.String.class}
    )
    public void testGetLogger_nameSpace() throws Exception {
        // test name with space
        Logger foo = new MockLogger(FOO, null);
        Logger fooBeforeSpace = new MockLogger(FOO + " ", null);
        Logger fooAfterSpace = new MockLogger(" " + FOO, null);
        Logger fooWithBothSpace = new MockLogger(" " + FOO + " ", null);
        assertTrue(mockManager.addLogger(foo));
        assertTrue(mockManager.addLogger(fooBeforeSpace));
        assertTrue(mockManager.addLogger(fooAfterSpace));
        assertTrue(mockManager.addLogger(fooWithBothSpace));

        assertSame(foo, mockManager.getLogger(FOO));
        assertSame(fooBeforeSpace, mockManager.getLogger(FOO + " "));
        assertSame(fooAfterSpace, mockManager.getLogger(" " + FOO));
        assertSame(fooWithBothSpace, mockManager.getLogger(" " + FOO + " "));
    }

    /*
     * test for method public void checkAccess() throws SecurityException
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "checkAccess",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getLogManager",
            args = {}
        )
    })
    public void testCheckAccess() {
        try {
            manager.checkAccess();
        } catch (SecurityException e) {
            fail("securityException should not be thrown");
        }
        System.setSecurityManager(new MockSecurityManagerLogPermission());
        mockManager.addLogger(new MockLogger("abc", null));
        mockManager.getLogger("");
        mockManager.getLoggerNames();
        mockManager.getProperty(".level");
        LogManager.getLogManager();
        try {
            manager.checkAccess();
            fail("should throw securityException");
        } catch (SecurityException e) {
        }

        System.setSecurityManager(securityManager);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "readConfiguration",
            args = {java.io.InputStream.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "readConfiguration",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "checkAccess",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "reset",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "getLogManager",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies SecurityException.",
            method = "addPropertyChangeListener",
            args = {java.beans.PropertyChangeListener.class}
        )
    })
    public void testLoggingPermission() throws IOException {
        System.setSecurityManager(new MockSecurityManagerLogPermission());
        mockManager.addLogger(new MockLogger("abc", null));
        mockManager.getLogger("");
        mockManager.getLoggerNames();
        mockManager.getProperty(".level");
        LogManager.getLogManager();
        try {
            manager.checkAccess();
            fail("should throw securityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager.readConfiguration();
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager.readConfiguration(null);
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager
                    .addPropertyChangeListener(new MockPropertyChangeListener());
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager.addPropertyChangeListener(null);
            fail("should throw NPE");
        } catch (NullPointerException e) {
        }
        try {
            mockManager.removePropertyChangeListener(null);
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        try {
            mockManager.reset();
            fail("should throw SecurityException");
        } catch (SecurityException e) {
        }
        System.setSecurityManager(securityManager);
    }


    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "readConfiguration",
            args = {java.io.InputStream.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "getProperty",
            args = {java.lang.String.class}
        )
    })
    public void testMockGetProperty() throws Exception {
        // mock manager doesn't read configuration until you call
        // readConfiguration()
        Logger root = new MockLogger("", null);
        assertTrue(mockManager.addLogger(root));
        root = mockManager.getLogger("");
        checkPropertyNull(mockManager);
        assertEquals(0, root.getHandlers().length);
        assertNull(root.getLevel());
        mockManager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        assertEquals(Level.FINE, root.getLevel());
        checkProperty(mockManager);
        mockManager.reset();
        checkPropertyNull(mockManager);
        assertEquals(Level.INFO, root.getLevel());
        assertEquals(0, mockManager.getLogger("").getHandlers().length);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getProperty",
        args = {java.lang.String.class}
    )
    public void testGetProperty() throws SecurityException, IOException {

        Logger root = manager.getLogger("");
        manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        checkProperty(manager);
        assertEquals(2, root.getHandlers().length);
        assertEquals(Level.FINE, root.getLevel());

        manager.reset();
        checkPropertyNull(manager);
        assertEquals(0, root.getHandlers().length);
        assertEquals(Level.INFO, root.getLevel());
        manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        manager.reset();
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testReadConfiguration_null() throws SecurityException, IOException {
        try {
            manager.readConfiguration(null);
            fail("should throw null pointer exception");
        } catch (NullPointerException e) {
        }

    }
    
    private static void checkPropertyNull(LogManager m) {
        // assertNull(m.getProperty(".level"));
        assertNull(m.getProperty("java.util.logging.FileHandler.limit"));
        assertNull(m.getProperty("java.util.logging.ConsoleHandler.formatter"));
        // assertNull(m.getProperty("handlers"));
        assertNull(m.getProperty("java.util.logging.FileHandler.count"));
        assertNull(m.getProperty("com.xyz.foo.level"));
        assertNull(m.getProperty("java.util.logging.FileHandler.formatter"));
        assertNull(m.getProperty("java.util.logging.ConsoleHandler.level"));
        assertNull(m.getProperty("java.util.logging.FileHandler.pattern"));
    }

    private static void checkProperty(LogManager m) {
        // assertEquals(m.getProperty(".level"), "INFO");
        assertEquals(m.getProperty("java.util.logging.FileHandler.limit"), "50000");
        assertEquals(m.getProperty("java.util.logging.ConsoleHandler.formatter"),
                "java.util.logging.SimpleFormatter");
        // assertEquals(m.getProperty("handlers"),
        // "java.util.logging.ConsoleHandler");
        assertEquals(m.getProperty("java.util.logging.FileHandler.count"), "5");
        assertEquals(m.getProperty("LogManagerTestFoo.level"), "WARNING");
        assertEquals(m.getProperty("java.util.logging.FileHandler.formatter"),
                "java.util.logging.XMLFormatter");
        assertEquals(m.getProperty("java.util.logging.ConsoleHandler.level"), "OFF");
        assertEquals(m.getProperty("java.util.logging.FileHandler.pattern"), "%h/java%u.log");
    }

    //    public void testReadConfiguration() throws SecurityException, IOException {
    //          FIXME: move the support_exec
    //            Logger foo = new MockLogger("foo", null);
    //            assertNull(foo.getLevel());
    //            assertTrue(mockManager.addLogger(foo));
    //
    //            Logger fo = new MockLogger("foo2", null);
    //            fo.setLevel(Level.ALL);
    //            assertTrue(mockManager.addLogger(fo));
    //
    //            Handler h = new ConsoleHandler();
    //            Level l = h.getLevel();
    //            assertNotSame(Level.OFF, h.getLevel());
    //
    //            // read configuration
    //            mockManager.readConfiguration();
    //            // level DO has effect
    //            assertEquals(Level.WARNING, foo.getLevel());
    //            // for non specified logger, level is reset to null
    //            assertNull(fo.getLevel());
    //
    //            // read properties don't affect handler
    //            assertNotSame(Level.OFF, h.getLevel());
    //            assertSame(l, h.getLevel());
    //
    //    }

    /*
     * Class under test for void readConfiguration(InputStream)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testReadConfigurationInputStream() throws IOException {

        Logger foo = new MockLogger(FOO, null);
        assertNull(foo.getLevel());
        assertTrue(mockManager.addLogger(foo));

        Logger fo = new MockLogger(FOO + "2", null);
        fo.setLevel(Level.ALL);
        assertTrue(mockManager.addLogger(fo));

        Handler h = new ConsoleHandler();
        Level l = h.getLevel();
        assertSame(Level.INFO, h.getLevel());

        // read configuration from stream
        InputStream stream = EnvironmentHelper.PropertiesToInputStream(props);
        mockManager.readConfiguration(stream);
        stream.close();

        // level DOES have an effect on LogManagerTestFoo
        assertEquals(Level.WARNING, foo.getLevel());

        // for non specified logger, level is reset to null
        assertNull(fo.getLevel());

        // read properties don't affect handler
        assertSame(Level.INFO, h.getLevel());
        assertSame(l, h.getLevel());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies NullPointerException.",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testReadConfigurationInputStream_null() throws SecurityException, IOException {
        try {
            mockManager.readConfiguration(null);
            fail("should throw null pointer exception");
        } catch (NullPointerException e) {
        }

    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies IOException.",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testReadConfigurationInputStream_IOException_1parm() throws SecurityException {
        try {
            mockManager.readConfiguration(new MockInputStream());
            fail("should throw IOException");
        } catch (IOException e) {
            //ignore
        }

    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testReadConfigurationInputStream_root() throws IOException {
        manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));

        Logger logger = new MockLogger("testReadConfigurationInputStream_root.foo", null);
        Logger root = manager.getLogger("");
        Logger logger2 = Logger.getLogger("testReadConfigurationInputStream_root.foo2");

        manager.addLogger(logger);
        assertNull(logger.getLevel());
        assertEquals(0, logger.getHandlers().length);
        assertSame(root, logger.getParent());

        assertNull(logger2.getLevel());
        assertEquals(0, logger2.getHandlers().length);
        assertSame(root, logger2.getParent());
        // if (!hasConfigClass) {
        assertEquals(Level.FINE, root.getLevel());
        assertEquals(2, root.getHandlers().length);
        // }

        // after read stream
        InputStream stream = EnvironmentHelper.PropertiesToInputStream(props);
        manager.readConfiguration(stream);
        stream.close();
        assertEquals(Level.FINE, root.getLevel());
        assertEquals(2, root.getHandlers().length);
        assertNull(logger.getLevel());
        assertEquals(0, logger.getHandlers().length);
        manager.reset();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addPropertyChangeListener",
            args = {java.beans.PropertyChangeListener.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "removePropertyChangeListener",
            args = {java.beans.PropertyChangeListener.class}
        )
    })
    public void testAddRemovePropertyChangeListener() throws Exception {
        MockPropertyChangeListener listener1 = new MockPropertyChangeListener();
        MockPropertyChangeListener listener2 = new MockPropertyChangeListener();
        // add same listener1 two times
        mockManager.addPropertyChangeListener(listener1);
        mockManager.addPropertyChangeListener(listener1);
        mockManager.addPropertyChangeListener(listener2);

        assertNull(listener1.getEvent());
        assertNull(listener2.getEvent());
        mockManager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        // if (!hasConfigClass) {
        assertNotNull(listener1.getEvent());
        assertNotNull(listener2.getEvent());
        // }

        listener1.reset();
        listener2.reset();

        // remove listener1, no effect
        mockManager.removePropertyChangeListener(listener1);
        mockManager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        assertNotNull(listener1.getEvent());
        assertNotNull(listener2.getEvent());
        listener1.reset();
        listener2.reset();

        // remove listener1 again and it works
        mockManager.removePropertyChangeListener(listener1);
        mockManager.readConfiguration(EnvironmentHelper
                .PropertiesToInputStream(props));
        assertNull(listener1.getEvent());
        assertNotNull(listener2.getEvent());
        listener2.reset();

        // reset don't produce event
        mockManager.reset();
        assertNull(listener2.getEvent());

        mockManager.removePropertyChangeListener(listener2);
        mockManager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        assertNull(listener1.getEvent());
        assertNull(listener2.getEvent());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "addPropertyChangeListener",
            args = {java.beans.PropertyChangeListener.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "removePropertyChangeListener",
            args = {java.beans.PropertyChangeListener.class}
        )
    })
    public void testAddRemovePropertyChangeListener_null() {
        // seems nothing happened
        try{
            mockManager.addPropertyChangeListener(null);
            fail("Should throw NPE");
        }catch(NullPointerException e){
        }
        mockManager.removePropertyChangeListener(null);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify SecurityException.",
        method = "reset",
        args = {}
    )
    public void testReset() throws SecurityException, IOException {
        // mock LogManager
        mockManager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
        assertNotNull(mockManager.getProperty("handlers"));
        Logger foo = new MockLogger(FOO, null);
        assertNull(foo.getLevel());
        assertEquals(0, foo.getHandlers().length);
        foo.setLevel(Level.ALL);
        foo.addHandler(new ConsoleHandler());
        assertTrue(mockManager.addLogger(foo));
        assertEquals(Level.WARNING, foo.getLevel());
        assertEquals(2, foo.getHandlers().length);

        // reset
        mockManager.reset();

        // properties is cleared
        assertNull(mockManager.getProperty("handlers"));

        // level is null
        assertNull(foo.getLevel());
        // handlers are all closed
        assertEquals(0, foo.getHandlers().length);

        // for root logger
        manager.reset();
        assertNull(manager.getProperty("handlers"));
        Logger root = manager.getLogger("");
        // level reset to info
        assertEquals(Level.INFO, root.getLevel());
        // also close root's handler
        assertEquals(0, root.getHandlers().length);
    }

    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Doesn't verify SecurityException.",
        method = "readConfiguration",
        args = {java.io.InputStream.class}
    )
    public void testGlobalPropertyConfig() throws Exception {
        PrintStream err = System.err;
        try {
            System.setErr(new PrintStream(new NullOutputStream()));
            // before add config property, root has two handler
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(2, manager.getLogger("").getHandlers().length);

            // one valid config class
            props.setProperty("config", className + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(3, manager.getLogger("").getHandlers().length);

            // two config class take effect orderly
            props.setProperty("config", className + "$MockValidConfig " + className
                    + "$MockValidConfig2");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(2, manager.getLogger("").getHandlers().length);

            props.setProperty("config", className + "$MockValidConfig2 " + className
                    + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(3, manager.getLogger("").getHandlers().length);

            // invalid config class which throw exception, just print exception
            // and
            // message
            props.setProperty("config", className + "$MockInvalidConfigException");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));

            // invalid config class without default constructor, just print
            // exception and message
            props.setProperty("config", className + "$MockInvalidConfigNoDefaultConstructor");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));

            // bad config class name, just print exception and message
            props.setProperty("config", "badname");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));

            // invalid separator, nothing happened
            props.setProperty("config", className + "$MockValidConfig2;" + className
                    + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(2, manager.getLogger("").getHandlers().length);
            props.setProperty("config", className + "$MockValidConfig2;" + className
                    + "$MockValidConfig " + className + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(3, manager.getLogger("").getHandlers().length);

            // duplicate config class, take effect twice
            props.setProperty("config", className + "$MockValidConfig " + className
                    + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(4, manager.getLogger("").getHandlers().length);

            // invalid config classes mixed with valid config classes, valid
            // config
            // classes take effect
            props.setProperty("config", "badname " + className + "$MockValidConfig " + className
                    + "$MockInvalidConfigNoDefaultConstructor " + className + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(4, manager.getLogger("").getHandlers().length);

            // global property take effect before logger specified property
            props.setProperty("config", className + "$MockValidConfig");
            manager.readConfiguration(EnvironmentHelper.PropertiesToInputStream(props));
            assertEquals(Level.FINE, manager.getLogger("").getLevel());
        } finally {
            System.setErr(err);
            manager.reset();
        }

    }
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "readConfiguration",
        args = {}
    )
    public void testValidConfigClass() throws Exception {
        String oldProperty = System.getProperty("java.util.logging.config.class");
        try {
            //            System.setProperty("java.util.logging.config.class", "org.apache.harmony.logging.tests.java.util.logging.LogManagerTest$ConfigClass");
            System.setProperty("java.util.logging.config.class", this.getClass().getName()
                    + "$ConfigClass");
            assertNull(manager.getLogger("testConfigClass.foo"));

            manager.readConfiguration();
            assertNull(manager.getLogger("testConfigClass.foo"));
            Logger l = Logger.getLogger("testConfigClass.foo.child");
            assertSame(Level.FINEST, manager.getLogger("").getLevel());
            assertEquals(0, manager.getLogger("").getHandlers().length);
            assertEquals("testConfigClass.foo", l.getParent().getName());
        } finally {
            if (oldProperty != null) {
                System.setProperty("java.util.logging.config.class", oldProperty);
            }
        }
    }

    /*
     * ---------------------------------------------------- 
     * mock classes
     * ----------------------------------------------------
     */
    

    public static class ConfigClass {
        public ConfigClass() throws Exception {
            LogManager man = LogManager.getLogManager();
            Properties props = new Properties();
            props.put("handlers", className + "$MockHandler " + className + "$MockHandler");
            props.put("java.util.logging.FileHandler.pattern", "%h/java%u.log");
            props.put("java.util.logging.FileHandler.limit", "50000");
            props.put("java.util.logging.FileHandler.count", "5");
            props.put("java.util.logging.FileHandler.formatter", "java.util.logging.XMLFormatter");
            props.put(".level", "FINE");
            props.put("java.util.logging.ConsoleHandler.level", "OFF");
            props.put("java.util.logging.ConsoleHandler.formatter","java.util.logging.SimpleFormatter");
            props.put("LogManagerTestFoo.handlers", "java.util.logging.ConsoleHandler");
            props.put("LogManagerTestFoo.level", "WARNING");
            props.put("testConfigClass.foo.level", "OFF");
            props.put("testConfigClass.foo.handlers", "java.util.logging.ConsoleHandler");
            props.put(".level", "FINEST");
            props.remove("handlers");
            InputStream in = EnvironmentHelper.PropertiesToInputStream(props);
            man.readConfiguration(in);
        }
    }

    public static class MockInvalidInitClass {
        public MockInvalidInitClass() {
            throw new RuntimeException();
        }
    }

    public static class TestInvalidConfigFile {
        public static void main(String[] args) {
            LogManager manager = LogManager.getLogManager();
            Logger root = manager.getLogger("");
            checkPropertyNull(manager);
            assertEquals(0, root.getHandlers().length);
            assertEquals(Level.INFO, root.getLevel());

            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkProperty(manager);
            assertNull(root.getHandlers()[0].getLevel());
            assertEquals(1, root.getHandlers().length);
            assertEquals(Level.INFO, root.getLevel());

            manager.reset();
            checkProperty(manager);
            assertEquals(0, root.getHandlers().length);
            assertEquals(Level.INFO, root.getLevel());
            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            manager.reset();
        }
    }

    public static class TestValidConfigFile {
        public static void main(String[] args) {
            LogManager manager = LogManager.getLogManager();
            Logger root = manager.getLogger("");
            checkPropertyNull(manager);
            assertEquals(2, root.getHandlers().length);
            assertEquals(root.getHandlers()[0].getLevel(), Level.OFF);
            assertEquals(Level.ALL, root.getLevel());

            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkPropertyNull(manager);
            assertEquals(root.getHandlers()[0].getLevel(), Level.OFF);
            assertEquals(2, root.getHandlers().length);
            assertEquals(Level.ALL, root.getLevel());

            manager.reset();
            checkPropertyNull(manager);
            assertEquals(0, root.getHandlers().length);
            assertEquals(Level.INFO, root.getLevel());
            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            manager.reset();
        }
    }

    public static class TestMockLogManager {
        public static void main(String[] args) {
            LogManager manager = LogManager.getLogManager();
            assertTrue(manager instanceof MockLogManager);
        }
    }

    public static class TestValidConfigClass {
        public static void main(String[] args) {
            LogManager manager = LogManager.getLogManager();
            Logger root = manager.getLogger("");
            checkPropertyNull(manager);
            assertEquals(1, root.getHandlers().length);
            assertEquals(Level.OFF, root.getLevel());

            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkPropertyNull(manager);
            assertEquals(1, root.getHandlers().length);
            assertEquals(Level.OFF, root.getLevel());

            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkPropertyNull(manager);
            assertEquals(1, root.getHandlers().length);
            assertEquals(Level.OFF, root.getLevel());

            manager.reset();
            checkPropertyNull(manager);
            assertEquals(0, root.getHandlers().length);
            assertEquals(Level.INFO, root.getLevel());
            try {
                manager.readConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            manager.reset();
        }
    }

    public static class MockLogger extends Logger {
        public MockLogger(String name, String rbName) {
            super(name, rbName);
        }
    }

    public static class MockLogManager extends LogManager {
    }

    public static class MockHandler extends Handler {
        static int number = 0;

        public MockHandler() {
            addNumber();
            // System.out.println(this + ":start:" + number);
        }

        private synchronized void addNumber() {
            number++;
        }

        public void close() {
            minusNumber();
            // System.out.println(this + ":close:" + number);
        }

        private synchronized void minusNumber() {
            number--;
        }

        public void flush() {
            // System.out.println(this + ":flush");
        }

        public void publish(LogRecord record) {
        }

    }

    public static class MockValidInitClass {
        public MockValidInitClass() {
            Properties p = new Properties();
            p.put("handlers", className + "$MockHandler");
            p.put(".level", "OFF");
            InputStream in = null;
            try {
                in = EnvironmentHelper.PropertiesToInputStream(p);
                LogManager manager = LogManager.getLogManager();
                manager.readConfiguration(in);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static class MockValidConfig {
        public MockValidConfig() {
            handler = new MockHandler();
            LogManager manager = LogManager.getLogManager();
            Logger root = null;
            if (null != manager) {
                root = manager.getLogger("");
            } else {
                System.out.println("null manager");
            }
            if (null != root) {
                root.addHandler(handler);
                root.setLevel(Level.OFF);
            }
        }
    }

    public static class MockValidConfig2 {

        static Logger root = null;

        public MockValidConfig2() {
            root = LogManager.getLogManager().getLogger("");
            root.removeHandler(handler);
        }
    }

    public static class MockInvalidConfigException {
        public MockInvalidConfigException() {
            throw new RuntimeException("invalid config class - throw exception");
        }
    }

    public static class MockInvalidConfigNoDefaultConstructor {
        public MockInvalidConfigNoDefaultConstructor(int i) {
            throw new RuntimeException("invalid config class - no default constructor");
        }
    }

    public static class MockPropertyChangeListener implements
            PropertyChangeListener {

        PropertyChangeEvent event = null;

        public void propertyChange(PropertyChangeEvent event) {
            this.event = event;
        }

        public PropertyChangeEvent getEvent() {
            return event;
        }

        public void reset() {
            event = null;
        }

    }

    public static class MockSecurityManagerLogPermission extends SecurityManager {

        public void checkPermission(Permission permission, Object context) {
            if (permission instanceof LoggingPermission) {
                throw new SecurityException();
            }
        }

        public void checkPermission(Permission permission) {
            if (permission instanceof LoggingPermission) {
                StackTraceElement[] stack = (new Throwable()).getStackTrace();
                for (int i = 0; i < stack.length; i++) {
                    if (stack[i].getClassName().equals("java.util.logging.Logger")) {
                        return;
                    }
                }
                throw new SecurityException("Found LogManager checkAccess()");
            }
        }
    }

    public static class MockSecurityManagerOtherPermission extends SecurityManager {

        public void checkPermission(Permission permission, Object context) {
            if (permission instanceof LoggingPermission) {
                return;
            }
            if (permission.getName().equals("setSecurityManager")) {
                return;
            }
            // throw new SecurityException();
            super.checkPermission(permission, context);
        }

        public void checkPermission(Permission permission) {
            if (permission instanceof LoggingPermission) {
                return;
            }
            if (permission.getName().equals("setSecurityManager")) {
                return;
            }
            super.checkPermission(permission);
        }
    }

    /*
     * Test config class loading
     * java -Djava.util.logging.config.class=badConfigClassName ClassLoadingTest 
     */
    public static class ClassLoadingTest {
        public static void main(String[] args) {
            Thread.currentThread().setContextClassLoader(new MockErrorClassLoader());
            try {
                LogManager.getLogManager();
                fail("Should throw mock error");
            } catch (MockError e) {
            }
        }

        static class MockErrorClassLoader extends ClassLoader {
            public Class<?> loadClass(String name) {
                throw new MockError();
            }
        }

        static class MockError extends Error {
        }
    }
    
    public static class MockInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException();
        }

      
    }
    
    
}
