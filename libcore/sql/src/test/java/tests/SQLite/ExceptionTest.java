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

import SQLite.Database;
import SQLite.Exception;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

@TestTargetClass(SQLite.Exception.class)
public class ExceptionTest extends SQLiteTest {
    
    private Database db = null;

    public void setUp() throws java.lang.Exception {
        super.setUp();
        db = new Database();
    }
    
    public void tearDown() {
        super.tearDown();
    }
    
    /**
     * @tests {@link Exception#Exception(String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "constructor test",
        method = "Exception",
        args = {java.lang.String.class}
    )
    public void testException() {
        try {
            db.open(dbFile.getName(), 0);
        } catch (Exception e) {
            assertNotNull(e);
            assertNotNull(e.getMessage());
        }
    }

}
