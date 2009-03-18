/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.SQLite;

import SQLite.Exception;
import SQLite.JDBCDriver;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;


@TestTargetClass(JDBCDriver.class)
public class JDBCDriverTest extends JDBCDriverFunctionalTest {
    
    /**
     * The SQLite db file.
     */
    private JDBCDriver jDriver;
    
    private Driver returnedDriver;

    public void setUp() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, Exception  {
        
        try {
            super.setUp();
            returnedDriver = DriverManager.getDriver(getConnectionURL());
            if (returnedDriver instanceof JDBCDriver) {
                this.jDriver = (JDBCDriver) returnedDriver;
            }
        } catch (SQLException e) {
          System.out.println("Cannot get driver");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("DB Setup failed");
            e.printStackTrace();
        }
   }

    /**
     * @tests JDBCDriver#JDBCDriver()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "constructor test",
        method = "JDBCDriver",
        args = {}
    )
    public void testJDBCDriver() {
        assertTrue(returnedDriver instanceof JDBCDriver);
    }

    /**
     * @tests JDBCDriver#acceptsURL(String)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "constructor test",
            method = "acceptsURL",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "constructor test",
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,
            method = "acceptsURL",
            args = {java.lang.String.class}
        )        
    })
    public void testAcceptsURL() {
        try {
            if (this.jDriver != null) {
                assertTrue(jDriver.acceptsURL(getConnectionURL()));
            } else {
                fail("no Driver available");
            }
        } catch (SQLException e) {
            fail("Driver does not accept URL");
            e.printStackTrace();
        }
    }

    /**
     * @tests JDBCDriver#connect(String, java.util.Properties)
     */
    @TestTargets({    
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            method = "connect",
            args = {java.lang.String.class, java.util.Properties.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,
            notes = "method test",
            method = "connect",
            args = {java.lang.String.class, java.util.Properties.class}
        )
    })
    public void testConnect() {
        try {
            if (this.jDriver != null) {
                Connection c = jDriver.connect(getConnectionURL(), null);
                assertFalse(c.isClosed());
                DriverManager.getConnection(getConnectionURL());
            } else {
                fail("no Driver available");
            }
        } catch (SQLException e) {
            fail("Driver does not connect");
            e.printStackTrace();
        }
    }

    /**
     * @tests JDBCDriver#getMajorVersion()
     */
   @TestTargets({    
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            method = "getMajorVersion",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,
            notes = "method test",
            method = "getMajorVersion",
            args = {}
        )
    })    
    public void testGetMajorVersion() {
        if (this.jDriver != null) {
            assertTrue(jDriver.getMajorVersion() > 0);
        } else {
            fail("no Driver available");
        }
    }

    /**
     * @tests JDBCDriver#getMinorVersion()
     */
   @TestTargets({       
       @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            method = "getMinorVersion",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,            
            method = "getMinorVersion",
            args = {}
        )
   })
   public void testGetMinorVersion() {
        if (this.jDriver != null) {
            assertTrue(jDriver.getMinorVersion() > 0);
        } else {
            fail("no version information available");
        }
    }

    /**
     * @tests JDBCDriver#getPropertyInfo(String, java.util.Properties)
     */
   @TestTargets({
       @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            method = "getPropertyInfo",
            args = {java.lang.String.class, java.util.Properties.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,            
            method = "getPropertyInfo",
            args = {java.lang.String.class, java.util.Properties.class}
        )
   })
   public void testGetPropertyInfo() {
        DriverPropertyInfo[] info = null;
        try {
            if (this.jDriver != null) {
                info = jDriver.getPropertyInfo(getConnectionURL(), null);
                assertNotNull(info);
                assertTrue(info.length > 0);
            } else {
                fail("no Driver available");
            }
        } catch (SQLException e) {
            fail("Driver property details not available");
            e.printStackTrace();
        }
        
        assertNotNull(info);
     
    }

    /**
     * @tests JDBCDriver#jdbcCompliant()
     */
   @TestTargets({    
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "method test",
            method = "jdbcCompliant",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            // we have to list the Driver target explicitly, since SQLite
            // is not part of the target packages
            clazz = Driver.class,
            notes = "method test",
            method = "jdbcCompliant",
            args = {}
        )
    })        
    public void testJdbcCompliant() {
        if (this.jDriver != null) {
            assertFalse(jDriver.jdbcCompliant());
        } else {
            fail("no version information available");
        }
    }
    /**
     * Tears down an unit test by calling the tearDown method of the super class
     * and deleting the SQLite test db file.
     */
    @Override
    protected void tearDown() throws SQLException {
        super.tearDown();
    }

}
