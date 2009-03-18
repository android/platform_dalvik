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

import dalvik.annotation.TestTargetClass;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import junit.framework.TestCase;

@TestTargetClass(DriverManager.class)
/**
 * Helper class for the Driver manager tes - it allows the test code to be
 * loaded under a different classloader, necessary for testing the
 * DeregisterDriver function of DriverManager
 * 
 */
public class TestHelper_DriverManager extends TestCase {

    static Driver testDriver = null;

    static TestHelper_DriverManager theHelper;

    static {
        theHelper = new TestHelper_DriverManager();
        // theHelper.testDeregister();
    } // end static

    public TestHelper_DriverManager() {
        super();
    } // end constructor TestHelper_DriverManager()

    public static void setDriver(Driver theDriver) {
        testDriver = theDriver;
      //  Logger.global.info("TestHelper_DriverManager: Test Driver set!");

        theHelper.checkDeregister();
    } // end method setDriver( Driver )

    public void checkDeregister() {

        String baseURL = "jdbc:mikes1";

      //  Logger.global.info("Calling checkDeregister in TestHelper_DriverManager....");

        Driver aDriver;

       // Logger.global.info("checkDeregister classloader: this.getClass().getClassLoader()");

        // Try to get a driver from the general pool... this should fail
        try {
            aDriver = DriverManager.getDriver(baseURL);
            fail("testDeregisterDriver: Didn't get exception when getting valid driver from other classloader.");
        } catch (SQLException e) {
            // e.printStackTrace();
            assertTrue(
                    "testDeregisterDriver: Got exception when getting valid driver from other classloader.",
                    true);
            // return;
        } // end try

        // OK, now THIS driver was loaded by someone else....
        aDriver = testDriver;

        // printClassLoader( aDriver );

        // Deregister this driver
        try {
            DriverManager.deregisterDriver(aDriver);
            // We shouldn't get here - but if we do, we need to re-register the
            // driver to
            // prevent subsequent tests from failing due to inability to get to
            // this driver...
            DriverManager.registerDriver(aDriver);
            fail("checkDeregisterDriver: Didn't get Security Exception deregistering invalid driver.");
        } catch (SecurityException s) {
            // This is the exception we should get...
            // System.out.println("checkDeregisterDriver: got expected Security
            // Exception");
        } catch (Exception e) {
            fail("checkDeregisterDriver: Got wrong exception type when deregistering invalid driver.");
        } // end try

    } // end method testDeRegister

    static void printClassLoader(Object theObject) {
        Class<? extends Object> theClass = theObject.getClass();
        ClassLoader theClassLoader = theClass.getClassLoader();
        System.out.println("ClassLoader is: " + theClassLoader.toString()
                + " for object: " + theObject.toString());
    } // end method printClassLoader( Object )

} // end class TestHelper_DriverManager

