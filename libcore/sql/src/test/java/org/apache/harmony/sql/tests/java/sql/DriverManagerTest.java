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

package org.apache.harmony.sql.tests.java.sql;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.security.Permission;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLPermission;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.TestCase;
@TestTargetClass(DriverManager.class)
/**
 * JUnit Testcase for the java.sql.DriverManager class
 * 
 */
public class DriverManagerTest extends TestCase {

    // Set of driver names to use
    static final String DRIVER1 = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver1";

    static final String DRIVER2 = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver2";

    static final String DRIVER3 = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver3";

    static final String DRIVER4 = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver4";

    static final String DRIVER5 = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver5";

    static final String INVALIDDRIVER1 = "abc.klm.Foo";

    static String[] driverNames = { DRIVER1, DRIVER2 };

    static int numberLoaded;

    static String baseURL1 = "jdbc:mikes1";

    static String baseURL4 = "jdbc:mikes4";

    static final String JDBC_PROPERTY = "jdbc.drivers";

    static TestHelper_ClassLoader testClassLoader = new TestHelper_ClassLoader();

    // Static initializer to load the drivers so that they are available to all
    // the
    // test methods as needed.
    @Override
    public void setUp() {
        numberLoaded = loadDrivers();
    } // end setUp()

    /**
     * Test for the method DriverManager.deregisterDriver
     * @throws SQLException 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "SQLException checking missed: not feasible.",
        method = "deregisterDriver",
        args = {java.sql.Driver.class}
    )
    public void testDeregisterDriver() throws Exception {
        // First get one of the drivers loaded by the test
        Driver aDriver;
        aDriver = DriverManager.getDriver(baseURL4);

        // Deregister this driver
        DriverManager.deregisterDriver(aDriver);

        assertFalse("testDeregisterDriver: Driver was not deregistered.",
                isDriverLoaded(aDriver));

        // Re-register this driver (so subsequent tests have it available)
        DriverManager.registerDriver(aDriver);
        assertTrue("testDeregisterDriver: Driver did not reload.",
                isDriverLoaded(aDriver));

        // Test deregistering a null driver
        DriverManager.deregisterDriver(null);

        // Test deregistering a driver which was not loaded by this test's
        // classloader
        // TODO - need to load a driver with a different classloader!!
        aDriver = DriverManager.getDriver(baseURL1);

        try {
            Class<?> driverClass = Class
                    .forName(
                            "org.apache.harmony.sql.tests.java.sql.TestHelper_DriverManager",
                            true, testClassLoader);

            // Give the Helper class one of our drivers....
            Class<?>[] methodClasses = {Class.forName("java.sql.Driver")};
            Method theMethod = driverClass.getDeclaredMethod("setDriver",
                    methodClasses);
            Object[] args = {aDriver};
            assertNotNull(args);
            theMethod.invoke(null, args);
        } catch (Exception e) {
            fail("testDeregisterDriver: Got exception allocating TestHelper: "
                    + e.getMessage());
            e.printStackTrace();
            return;
        } // end try

        // Check that the driver was not deregistered
        assertTrue(
                "testDeregisterDriver: Driver was incorrectly deregistered.",
                DriverManagerTest.isDriverLoaded(aDriver));

    } // end method testDeregisterDriver()

    static void printClassLoader(Object theObject) {
        Class<? extends Object> theClass = theObject.getClass();
        ClassLoader theClassLoader = theClass.getClassLoader();
        System.out.println("ClassLoader is: " + theClassLoader.toString()
                + " for object: " + theObject.toString());
    } // end method printClassLoader( Object )

    static boolean isDriverLoaded(Driver theDriver) {
        Enumeration<?> driverList = DriverManager.getDrivers();
        while (driverList.hasMoreElements()) {
            if ((Driver) driverList.nextElement() == theDriver) {
                return true;
            }
        } // end while
        return false;
    } // end method isDriverLoaded( Driver )

    /*
     * Class under test for Connection getConnection(String)
     */
    // valid connection - data1 does not require a user and password...
    static String validConnectionURL = "jdbc:mikes1:data1";

    // invalid connection - data2 requires a user & password
    static String invalidConnectionURL1 = "jdbc:mikes1:data2";

    // invalid connection - URL is gibberish
    static String invalidConnectionURL2 = "xyz1:abc3:456q";

    // invalid connection - URL is null
    static String invalidConnectionURL3 = null;

    static String[] invalidConnectionURLs = { invalidConnectionURL2,
            invalidConnectionURL3 };

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConnection",
        args = {java.lang.String.class}
    )
    public void testGetConnectionString() throws SQLException {
        Connection theConnection = null;
        // validConnection - no user & password required
        theConnection = DriverManager.getConnection(validConnectionURL);
        assertNotNull(theConnection);
        assertNotNull(DriverManager.getConnection(invalidConnectionURL1));

        for (String element : invalidConnectionURLs) {
            try {
                theConnection = DriverManager
                        .getConnection(element);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                //expected
            } // end try
        } // end for
    } // end method testGetConnectionString()
    
    /**
     * @tests java.sql.DriverManager#getConnection(String, Properties)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConnection",
        args = {java.lang.String.class, java.util.Properties.class}
    )
    public void test_getConnection_LStringLProperties() {
        try {
            DriverManager.getConnection("fff", //$NON-NLS-1$
                    new Properties());
            fail("Should throw SQLException.");
        } catch (SQLException e) {
            assertEquals("08001", e.getSQLState()); //$NON-NLS-1$
        }
        
        try {
            DriverManager.getConnection(null, 
                    new Properties());
            fail("Should throw SQLException.");
        } catch (SQLException e) {
            assertEquals("08001", e.getSQLState()); //$NON-NLS-1$
        }
    }

    /*
     * Class under test for Connection getConnection(String, Properties)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConnection",
        args = {java.lang.String.class, java.util.Properties.class}
    )
    public void testGetConnectionStringProperties() throws SQLException {
        String validURL1 = "jdbc:mikes1:data2";
        String validuser1 = "theuser";
        String validpassword1 = "thepassword";
        String invalidURL1 = "xyz:abc1:foo";
        String invalidURL2 = "jdbc:mikes1:crazyone";
        String invalidURL3 = "";
        String invaliduser1 = "jonny nouser";
        String invalidpassword1 = "whizz";
        Properties nullProps = null;
        Properties validProps = new Properties();
        validProps.setProperty("user", validuser1);
        validProps.setProperty("password", validpassword1);
        Properties invalidProps1 = new Properties();
        invalidProps1.setProperty("user", invaliduser1);
        invalidProps1.setProperty("password", invalidpassword1);
        String[] invalidURLs = { null, invalidURL1,
                invalidURL2, invalidURL3 };
        Properties[] invalidProps = { nullProps, invalidProps1};
        
        

        Connection theConnection = null;
        // validConnection - user & password required
        theConnection = DriverManager.getConnection(validURL1, validProps);
        assertNotNull(theConnection);

        // invalid Connections
        for (int i = 0; i < invalidURLs.length; i++) {
            theConnection = null;
            try {
                theConnection = DriverManager.getConnection(invalidURLs[i],
                        validProps);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                //expected
            } // end try
        } // end for
        for (Properties invalidProp : invalidProps) {
            assertNotNull(DriverManager.getConnection(validURL1, invalidProp));
        } 
    } // end method testGetConnectionStringProperties()

    /*
     * Class under test for Connection getConnection(String, String, String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConnection",
        args = {java.lang.String.class, java.lang.String.class, java.lang.String.class}
    )
    public void testGetConnectionStringStringString() throws SQLException {
        String validURL1 = "jdbc:mikes1:data2";
        String validuser1 = "theuser";
        String validpassword1 = "thepassword";
        String invalidURL1 = "xyz:abc1:foo";
        String invaliduser1 = "jonny nouser";
        String invalidpassword1 = "whizz";
        String[] invalid1 = { null, validuser1, validpassword1 };
        String[] invalid2 = { validURL1, null, validpassword1 };
        String[] invalid3 = { validURL1, validuser1, null };
        String[] invalid4 = { invalidURL1, validuser1, validpassword1 };
        String[] invalid5 = { validURL1, invaliduser1, invalidpassword1 };
        String[] invalid6 = { validURL1, validuser1, invalidpassword1 };
        String[][] invalids1 = { invalid1, invalid4};
        String[][] invalids2 = {invalid2, invalid3, invalid5, invalid6 };

        Connection theConnection = null;
        // validConnection - user & password required
        theConnection = DriverManager.getConnection(validURL1, validuser1,
                validpassword1);
        assertNotNull(theConnection);
        for (String[] theData : invalids1) {
            theConnection = null;
            try {
                theConnection = DriverManager.getConnection(theData[0],
                        theData[1], theData[2]);
                fail("Should throw SQLException.");
            } catch (SQLException e) {
                //expected
            } // end try
        } // end for
        for (String[] theData : invalids2) {
            assertNotNull(DriverManager.getConnection(theData[0], theData[1],
                    theData[2]));
        } 
    } // end method testGetConnectionStringStringString()

    static String validURL1 = "jdbc:mikes1";

    static String validURL2 = "jdbc:mikes2";

    static String invalidURL1 = "xyz:acb";

    static String invalidURL2 = null;

    static String[] validURLs = { validURL1, validURL2 };

    static String[] invalidURLs = { invalidURL1, invalidURL2 };

    static String exceptionMsg1 = "No suitable driver";

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDriver",
        args = {java.lang.String.class}
    )
    public void testGetDriver() throws SQLException {
        for (String element : validURLs) {
            Driver validDriver = DriverManager.getDriver(element);
            assertNotNull(validDriver);
        } // end for

        for (String element : invalidURLs) {
            try {
                DriverManager.getDriver(element);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                assertEquals("08001", e.getSQLState());
                assertEquals(exceptionMsg1, e.getMessage());
            } // end try
        } // end for

    } // end method testGetDriver()

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDrivers",
        args = {}
    )
    public void testGetDrivers() {
        // Load a driver manager
        Enumeration<Driver> driverList = DriverManager.getDrivers();
        int i = 0;
        while (driverList.hasMoreElements()) {
            Driver theDriver = driverList.nextElement();
            assertNotNull(theDriver);
            i++;
        } // end while

        // Check that all the drivers are in the list...
        assertEquals("testGetDrivers: Don't see all the loaded drivers - ", i,
                numberLoaded);
    } // end method testGetDrivers()

    static int timeout1 = 25;

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLoginTimeout",
        args = {}
    )
    public void testGetLoginTimeout() {
        DriverManager.setLoginTimeout(timeout1);
        assertEquals(timeout1, DriverManager.getLoginTimeout());
    } // end method testGetLoginTimeout()

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLogStream",
        args = {}
    )
    @SuppressWarnings("deprecation")
    public void testGetLogStream() {
        assertNull(DriverManager.getLogStream());

        DriverManager.setLogStream(testPrintStream);
        assertTrue(DriverManager.getLogStream() == testPrintStream);

        DriverManager.setLogStream(null);
    } // end method testGetLogStream()

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getLogWriter",
        args = {}
    )
    public void testGetLogWriter() {
        assertNull(DriverManager.getLogWriter());

        DriverManager.setLogWriter(testPrintWriter);

        assertTrue(DriverManager.getLogWriter() == testPrintWriter);

        DriverManager.setLogWriter(null);
    } // end method testGetLogWriter()

    static String testMessage = "DriverManagerTest: test message for print stream";

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "println",
        args = {java.lang.String.class}
    )
    @SuppressWarnings("deprecation")
    public void testPrintln() {
        // System.out.println("testPrintln");
        DriverManager.println(testMessage);

        DriverManager.setLogWriter(testPrintWriter);
        DriverManager.println(testMessage);

        String theOutput = outputStream.toString();
        // System.out.println("testPrintln: output= " + theOutput );
        assertTrue(theOutput.startsWith(testMessage));

        DriverManager.setLogWriter(null);

        DriverManager.setLogStream(testPrintStream);
        DriverManager.println(testMessage);

        theOutput = outputStream2.toString();
        // System.out.println("testPrintln: output= " + theOutput );
        assertTrue(theOutput.startsWith(testMessage));

        DriverManager.setLogStream(null);
    } // end method testPrintln()

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "SQLException checking missed: not feasible",
        method = "registerDriver",
        args = {java.sql.Driver.class}
    )
    public void testRegisterDriver() throws ClassNotFoundException,
            SQLException, IllegalAccessException, InstantiationException {
        String EXTRA_DRIVER_NAME = "org.apache.harmony.sql.tests.java.sql.TestHelper_Driver3";

        try {
            DriverManager.registerDriver(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        } // end try

        Driver theDriver = null;
        // Load another Driver that isn't in the basic set
        Class<?> driverClass = Class.forName(EXTRA_DRIVER_NAME);
        theDriver = (Driver) driverClass.newInstance();
        DriverManager.registerDriver(theDriver);

        assertTrue("testRegisterDriver: driver not in loaded set",
                isDriverLoaded(theDriver));
        
        

    } // end testRegisterDriver()

    static int validTimeout1 = 15;

    static int validTimeout2 = 0;

    static int[] validTimeouts = { validTimeout1, validTimeout2 };

    static int invalidTimeout1 = -10;

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setLoginTimeout",
        args = {int.class}
    )
    public void testSetLoginTimeout() {
        for (int element : validTimeouts) {
            DriverManager.setLoginTimeout(element);

            assertEquals(element, DriverManager.getLoginTimeout());
        } // end for
        // Invalid timeouts
        DriverManager.setLoginTimeout(invalidTimeout1);
        assertEquals(invalidTimeout1, DriverManager.getLoginTimeout());
    } // end testSetLoginTimeout()

    static ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();

    static PrintStream testPrintStream = new PrintStream(outputStream2);

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setLogStream",
        args = {java.io.PrintStream.class}
    )
    @SuppressWarnings("deprecation")
    public void testSetLogStream() {
        // System.out.println("testSetLogStream");
        DriverManager.setLogStream(testPrintStream);

        assertSame(testPrintStream, DriverManager.getLogStream());

        DriverManager.setLogStream(null);

        assertNull(DriverManager.getLogStream());

        // Now let's deal with the case where there is a SecurityManager in
        // place
        TestSecurityManager theSecManager = new TestSecurityManager();
        System.setSecurityManager(theSecManager);

        theSecManager.setLogAccess(false);

        try {
            DriverManager.setLogStream(testPrintStream);
            fail("Should throw SecurityException.");
        } catch (SecurityException s) {
            //expected
        }

        theSecManager.setLogAccess(true);

        DriverManager.setLogStream(testPrintStream);

        System.setSecurityManager(null);
    } // end method testSetLogStream()

    static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    static PrintWriter testPrintWriter = new PrintWriter(outputStream);

    /**
     * Test for the setLogWriter method
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setLogWriter",
        args = {java.io.PrintWriter.class}
    )
    public void testSetLogWriter() {
        // System.out.println("testSetLogWriter");
        DriverManager.setLogWriter(testPrintWriter);

        assertSame(testPrintWriter, DriverManager.getLogWriter());

        DriverManager.setLogWriter(null);

        assertNull("testDriverManager: Log writer not null:", DriverManager
                .getLogWriter());

        // Now let's deal with the case where there is a SecurityManager in
        // place
        TestSecurityManager theSecManager = new TestSecurityManager();
        System.setSecurityManager(theSecManager);

        theSecManager.setLogAccess(false);

        try {
            DriverManager.setLogWriter(testPrintWriter);
            fail("Should throw SecurityException.");
        } catch (SecurityException s) {
            //expected
        }

        theSecManager.setLogAccess(true);
        DriverManager.setLogWriter(testPrintWriter);

        System.setSecurityManager(null);
    } // end method testSetLogWriter()

    /*
     * Method which loads a set of JDBC drivers ready for use by the various
     * tests @return the number of drivers loaded
     */
    static boolean driversLoaded = false;

    private static int loadDrivers() {
        if (driversLoaded) {
            return numberLoaded;
        }
        /*
         * First define a value for the System property "jdbc.drivers" - before
         * the DriverManager class is loaded - this property defines a set of
         * drivers which the DriverManager will load during its initialization
         * and which will be loaded on the System ClassLoader - unlike the ones
         * loaded later by this method which are loaded on the Application
         * ClassLoader.
         */
        int numberLoaded = 0;
        String theSystemDrivers = DRIVER4 + ":" + DRIVER5 + ":"
                + INVALIDDRIVER1;
        System.setProperty(JDBC_PROPERTY, theSystemDrivers);
        numberLoaded += 2;

        for (String element : driverNames) {
            try {
                Class<?> driverClass = Class.forName(element);
                assertNotNull(driverClass);
                // System.out.println("Loaded driver - classloader = " +
                // driverClass.getClassLoader());
                numberLoaded++;
            } catch (ClassNotFoundException e) {
                System.out.println("DriverManagerTest: failed to load Driver: "
                        + element);
            } // end try
        } // end for
        /*
         * System.out.println("DriverManagerTest: number of drivers loaded: " +
         * numberLoaded);
         */
        driversLoaded = true;
        return numberLoaded;
    } // end method loadDrivers()

    class TestSecurityManager extends SecurityManager {

        boolean logAccess = true;

        SQLPermission sqlPermission = new SQLPermission("setLog");

        RuntimePermission setManagerPermission = new RuntimePermission(
                "setSecurityManager");

        TestSecurityManager() {
            super();
        } // end method TestSecurityManager()

        void setLogAccess(boolean allow) {
            logAccess = allow;
        } // end method setLogAccess( boolean )

        @Override
        public void checkPermission(Permission thePermission) {
            if (thePermission.equals(sqlPermission)) {
                if (!logAccess) {
                    throw new SecurityException("Cannot set the sql Log Writer");
                } // end if
                return;
            } // end if

            if (thePermission.equals(setManagerPermission)) {
                return;
            } // end if
            // super.checkPermission( thePermission );
        } // end method checkPermission( Permission )

    } // end class TestSecurityManager

} // end class DriverManagerTest


