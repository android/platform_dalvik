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

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    //All tests executed with sqlite3 only
    public static Test suite() {
        TestSuite suite = tests.TestSuiteFactory.createTestSuite("Tests for SQLite");
        //$JUnit-BEGIN$
        suite.addTestSuite(DatabaseTest.class);
        suite.addTestSuite(JDBCDriverFunctionalTest.class);
        suite.addTestSuite(JDBCDriverTest.class);
        suite.addTestSuite(ConstantsTest.class);
        suite.addTestSuite(BlobTest.class);
        suite.addTestSuite(StmtTest.class);
        suite.addTestSuite(ExceptionTest.class);
        suite.addTestSuite(FunctionContextTest.class);
        //$JUnit-END$
        return suite;
    }

}
