/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.sql;

import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@TestTargetClass(Statement.class)
public class SQLTest extends TestCase {
    static Connection conn;

    public void setUp() {
        getSQLiteConnection();
        createZoo();
    }

    private File dbFile;

    protected void getSQLiteConnection() {
        String tmp = System.getProperty("java.io.tmpdir");
        assertEquals(tmp,System.getProperty("java.io.tmpdir"));
        File tmpDir = new File(tmp);
        try {
            if (tmpDir.isDirectory()) {
                dbFile = File.createTempFile("sqliteTest", ".db", tmpDir);
                dbFile.deleteOnExit();
            } else {
                System.err.println("java.io.tmpdir does not exist");
            }
            
            Class.forName("SQLite.JDBCDriver").newInstance();
            conn = DriverManager.getConnection("jdbc:sqlite:/"
                    + dbFile.getPath());
            assertNotNull("Connection created ",conn);

        } catch (IOException e) {
            System.out.println("Problem creating file " + tmp);
        } catch (Exception e) {
            fail("Exception: " + e.toString());
        }
    }

    public void tearDown() {
    Statement st = null;
        try {
            if (! conn.isClosed()) {
            st = conn.createStatement();
            st.execute("drop table if exists zoo");
            }
        } catch (SQLException e) {
            fail("Couldn't drop table: " + e.getMessage());
        } finally {
        try {
            if (st != null) {
            st.close();
                conn.close();
            }
                } catch(SQLException ee) {}
        }
    }

    public void createZoo() {

        String[] queries = {
                "create table zoo(id smallint,  name varchar(10), family varchar(10))",
                "insert into zoo values (1, 'Kesha', 'parrot')",
                "insert into zoo values (2, 'Yasha', 'sparrow')" };
        
    Statement st = null;    
        try {
            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.execute(queries[i]);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        } finally {
        try {
            st.close();
         } catch (SQLException ee) {} 
    }
    }

    public void createProcedure() {
        String proc = "CREATE PROCEDURE welcomeAnimal (IN parameter1 integer, IN parameter2 char(20), IN parameter3 char(20)) "
                + " BEGIN "
                + " INSERT INTO zoo(id, name, family) VALUES (parameter1, parameter2, parameter3); "
                + "SELECT * FROM zoo;" + " END;";
    Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("DROP PROCEDURE IF EXISTS welcomeAnimal");
            st.execute(proc);
        } catch (SQLException e) {
            fail("Unexpected exception: " + e.getMessage());
        } finally {
        try {
            st.close();
         } catch (SQLException ee) {} 
    }
    }

    public int getCount(ResultSet rs) {
        int count = 0;
        try {
            while (rs.next()) {
                count++;
            }
        } catch (SQLException e) {
            fail("SQLException is thrown");
        }
        return count;
    }
}
