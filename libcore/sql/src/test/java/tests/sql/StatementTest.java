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

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

@TestTargetClass(Statement.class)
public class StatementTest extends SQLTest {
    
    /**
     * @test java.sql.Statement#addBatch(String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addBatch",
        args = {java.lang.String.class}
    )
    public void testAddBatch() throws SQLException {
        
        Statement st = null;
        try {
            st = conn.createStatement();
            st.addBatch("INSERT INTO zoo VALUES (3,'Tuzik','dog')");
            st.addBatch("INSERT INTO zoo VALUES (4,'Mashka','cat')");

            int[] updateCounts = st.executeBatch();
            assertEquals(2, updateCounts.length);
            assertEquals(1, updateCounts[0]);
            assertEquals(1, updateCounts[1]);

        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.addBatch("");
            st.executeBatch();
            fail("SQLException is not thrown");
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.addBatch(null);
            st.executeBatch();
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#clearWarnings()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clearWarnings",
        args = {}
    )
    public void testClearWarnings() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("select animals from zoo");
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        try {
            st = conn.createStatement();
            st.clearWarnings();
            SQLWarning w = st.getWarnings();
            assertNull(w);
        } catch (Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#getWarnings()
     * 
     * TODO getWarnings is not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported. always returns null. ",
        method = "getWarnings",
        args = {}
    )
    public void testGetWarnings() {
        
        Statement st = null;
        int errorCode1 = -1;
        int errorCode2 = -1;

        try {
            st = conn.createStatement();
            st.execute("select animals from zoooo");
            fail("SQLException was not thrown");
        } catch (SQLException e) {
            // expected
            errorCode1 = e.getErrorCode();
        }
        
        try {
            SQLWarning wrs = st.getWarnings();
            assertNull(wrs);
        } catch (Exception e) {
            fail("Change test implementation: get warnings is supported now");
        }
        /*
        Statement st = null;
        int errorCode1 = -1;
        int errorCode2 = -1;

        try {
            st = conn.createStatement();
            st.execute("select animals from zoooo");
        } catch (SQLException e) {
            // expected
            errorCode1 = e.getErrorCode();
        }
        try {
            SQLWarning wrs = st.getWarnings();
            assertNull(wrs);
        } catch (Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }
        try {
            st.execute("select horse from zoooooo");
        } catch (SQLException e) {
            // expected
            errorCode2 = e.getErrorCode();
        }

        try {
            SQLWarning wrs = st.getWarnings();
            assertEquals(errorCode1, wrs.getErrorCode());
            assertNotNull(wrs.getNextWarning());
            assertEquals(errorCode2, wrs.getErrorCode());
        } catch (Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }
        
        try {
            st.close();
        } catch (SQLException ee) {
        }
        */

    }

    /**
     * @test {@link java.sql.Statement#clearBatch()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clearBatch",
        args = {}
    )
    public void testClearBatch() throws SQLException {
        
        Statement st = null;
        
        try {
            st = conn.createStatement();
            st.addBatch("INSERT INTO zoo VALUES (3,'Tuzik','dog'); ");
            st.addBatch("INSERT INTO zoo VALUES (4,'Mashka','cat')");
            
            st.clearBatch();
            
            int[] updateCounts = st.executeBatch();
            
            for (int i = 0; i < updateCounts.length; i++) {
                assertEquals(0, updateCounts[i]);
            }

        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.addBatch("");
            st.executeBatch();
            fail("SQLException is not thrown");
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.addBatch(null);
            st.executeBatch();
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#execute(String sql)
     * 
     * TODO not pass on SQLite and RI.
     * 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "execute",
        args = {java.lang.String.class}
    )
    @KnownFailure("Return value wrong for queries below.")
    public void testExecute() throws SQLException {

        String[] queries = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "select animal_id, address from hutch where animal_id=1;",
                "create view address as select address from hutch where animal_id=2",
                "drop view address;", "drop table hutch;" };
        boolean[] results = {false, false, false, false, false, true, false,
                false, false};

        for (int i = 0; i < queries.length; i++) {
            Statement st = null;
            try {
                st = conn.createStatement();
                boolean res = st.execute(queries[i]);
                assertEquals("different result for statement no. "+i, results[i], res);
            } catch (SQLException e) {
                fail("SQLException is thrown: " + e.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
        }

        String[] inc_queries = {
                "update zoo_zoo set name='Masha', family='cat' where id=5;",
                "drop table hutchNO",
                "insert into hutch (id, animal_id, address) values (1, 2, 10);",
                "select animal_id, from hutch where animal_id=1;",
                "drop view address;", "drop table hutch;", "", null };

        for (int i = 0; i < inc_queries.length; i++) {
            Statement st = null;
            try {
                st = conn.createStatement();
                st.execute(inc_queries[i]);
                fail("SQLException is not thrown for query: " + inc_queries[i]);
            } catch (SQLException e) {
                // expected
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            } 
        }
    }

    /**
     * @test java.sql.Statement#execute(String sql, int autoGeneratedKeys)
     * TODO not supported 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Missing implementation for Statement.RETURN_GENERATED_KEYS: keys not yet supported",
        method = "execute",
        args = {java.lang.String.class, int.class}
    )
   public void testExecute_String_int() {
        String[] queries = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "select animal_id, address from hutch where animal_id=1;",
                "create view address as select address from hutch where animal_id=2",
                "drop view address;", "drop table hutch;" };

        for (int i = 0; i < queries.length; i++) {
            Statement st = null;
            try {
                st = conn.createStatement();
                st.execute(queries[i], Statement.NO_GENERATED_KEYS);
                fail("Exception expected: Not supported");
                /*
                ResultSet rs = st.getGeneratedKeys();
                fail("Revise test implemenation for feature impl. has changed");
                assertFalse(rs.next());
                */
            } catch (SQLException e) {
                // ok
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            } 
        }
        
        for (int i = 0; i < queries.length; i++) {
            Statement st = null;
            try {
                st = conn.createStatement();
                st.execute(queries[i], Statement.RETURN_GENERATED_KEYS);
                fail("Exception expected: Not supported");
                /*
                ResultSet rs = st.getGeneratedKeys();
                fail("Revise test implemenation for feature impl. has changed");
                assertFalse(rs.next());
                */
            } catch (SQLException e) {
                //ok
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            } 
        }
    }

    /**
     * @test java.sql.Statement#getConnection()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "SQLException test fails",
        method = "getConnection",
        args = {}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testGetConnection() {
        Statement st = null;
        try {
            st = conn.createStatement();
            assertSame(conn, st.getConnection());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st.close();
            st.getConnection();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
        
        
    }

    /**
     * @test java.sql.Statement#getFetchDirection()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "SQLException test fails. Not all Fetch directions supported.",
        method = "getFetchDirection",
        args = {}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testGetFetchDirection() {
        Statement st = null;
        try {
            st = conn.createStatement();
            assertEquals(ResultSet.FETCH_UNKNOWN, st.getFetchDirection());
        } catch (SQLException e) {
            fail("SQLException is thrown " + e.getMessage());
        }  finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st = conn.createStatement();
            st.setFetchDirection(ResultSet.FETCH_FORWARD);
            assertEquals(ResultSet.FETCH_FORWARD, st.getFetchDirection());
            fail("Exception expected: not supported");
        } catch (SQLException e) {
            // ok
        }  finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st.getFetchDirection();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }

    /**
     * @test java.sql.Statement#setFetchDirection(int)
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported. ",
        method = "setFetchDirection",
        args = {int.class}
    )
    public void testSetFetchDirection() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.setFetchDirection(ResultSet.FETCH_FORWARD);
            st.executeQuery("select * from zoo;");
            fail("Revise test implemenation for feature impl. has changed");
//            assertEquals(ResultSet.FETCH_FORWARD, st.getFetchDirection());
        } catch (SQLException e) {
//            fail("SQLException is thrown: " + e.getMessage());
            //ok
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        /*
        try {
            st = conn.createStatement();
            st.setFetchDirection(-1);
            fail("SQLException is not thrown");
        } catch (SQLException e) {
            // expected
        }  finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.setFetchDirection(100);
            fail("SQLException is not thrown");
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        */
    }

    /**
     * @test java.sql.Statement#getFetchSize()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "SQLException test fails",
        method = "getFetchSize",
        args = {}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testGetFetchSize() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("select * from zoo;");
            assertEquals(1, st.getFetchSize());
        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st.close();
            st.getFetchSize();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }

    /**
     * @test {@link java.sql.Statement#setFetchSize(int)}
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported.",
        method = "setFetchSize",
        args = {int.class}
    )
    public void testSetFetchSize() {
        Statement st = null;
        try {
            st = conn.createStatement();
            int rows = 100;
            for (int i = 0; i < rows; i++) {
                try {
                    st.setFetchSize(i);
                    assertEquals(i, st.getFetchSize());
                    fail("Revise: test implemenation for feature impl. has changed");
                } catch (SQLException sqle) {
    //                fail("SQLException is thrown: " + sqle.toString());
                    assertEquals("not supported", sqle.getMessage());
                }
            }
            /*
            try {
                st.setFetchSize(-1);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }
            */

        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#setMaxFieldSize(int max)
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setMaxFieldSize",
        args = {int.class}
    )
    public void testSetMaxFieldSize() {
        Statement st = null;
        try {
            st = conn.createStatement();
            for (int i = 0; i < 300; i += 50) {
                try {
                    st.setMaxFieldSize(i);
                    assertEquals(i, st.getMaxFieldSize());
                    fail("Revise test implemenation for feature impl. has changed");
                } catch (SQLException sqle) {
                    assertEquals("not supported", sqle.getMessage());
//                    fail("SQLException is thrown: " + sqle.getMessage());
                }
            }
            /*
            try {
                st.setMaxFieldSize(-1);
                fail("SQLException isn't thrown");
            } catch (SQLException sqle) {
                // expecteds
            }
            */
        } catch (SQLException e) {
            fail("Can't create statement, SQLException is thrown: "
                    + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#getMaxFieldSize()
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "getMaxFieldSize",
        args = {}
    )
    public void testGetMaxFieldSize() {
        Statement st = null;
        try {
            st = conn.createStatement();
            for (int i = 200; i < 500; i += 50) {
                try {
                    st.setMaxFieldSize(i);
                    fail("Revise test implemenation for feature impl. has changed");
                } catch (SQLException sqle) {
                    assertEquals("not supported", sqle.getMessage());
 //                   fail("SQLException is thrown: " + sqle.getMessage());
                }
            }
        } catch (SQLException e) {
            fail("Can't create statement, SQLException is thrown: "
                    + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#setMaxRows(int max)
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setMaxRows",
        args = {int.class}
    )
    public void testSetMaxRows() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("select * from zoo;");
            for (int i = 0; i < 300; i += 50) {
                try {
                    st.setMaxRows(i);
                    assertEquals(i, st.getMaxRows());
                    fail("Revise test implemenation for feature impl. has changed");
                } catch (SQLException sqle) {
                    assertEquals("not supported", sqle.getMessage());
//                   fail("SQLException is thrown: " + sqle.getMessage());
                }
            }
            try {
                st.setMaxRows(-1);
                fail("SQLException isn't thrown");
            } catch (SQLException sqle) {
                // expecteds
            }
        } catch (SQLException e) {
            fail("Can't create statement, SQLException is thrown: "
                    + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#getMaxRows()
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "getMaxRows",
        args = {}
    )
    public void testGetMaxRows() {
        Statement st = null;
        try {
            st = conn.createStatement();
            for (int i = 200; i < 500; i += 50) {
                try {
                    st.setMaxRows(i);
                    assertEquals(i, st.getMaxRows());
                    fail("Revise test implemenation for feature impl. has changed");
                } catch (SQLException sqle) {
                    assertEquals("not supported", sqle.getMessage());
//                    fail("SQLException is thrown: " + sqle.getMessage());
                }
            }
        } catch (SQLException e) {
            fail("Can't create statement, SQLException is thrown: "
                    + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#close()
     * not passed according to Java Docs: should release all resources 
     * IMMEDIATELY
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testClose() {
        Statement st = null;
        ResultSet res = null;
        try {
            String[] queries = {
                    "update zoo set name='Masha', family='cat' where id=2;",
                    "insert into zoo (id, name, family) values (3, 'Vorobey', 'sparrow');",
                    "insert into zoo (id, name, family) values (4, 'Slon', 'elephant');",
                    "select * from zoo"};
            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.execute(queries[i]);
            }
            res = st.getResultSet();
            assertNotNull(res);
            assertTrue(res.next());
            st.close();
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        // test release of resources:
        // this code should throw an exception as the db is not available
        // anymore in fact every resource which is used afterwards should throw
        // an SQLException.
        try {
            res.next();
            fail("Exception expected");
        } catch (SQLException e) {
            // ok
        }

    }

    /**
     * @test java.sql.Statement#execute(String sql, int[] columnIndexes)
     * TODO not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "execute",
        args = {java.lang.String.class, int[].class}
    )
    public void testExecute_String_intArray() {
        Statement st = null;
        try {
            String[] queries = {
                    "update zoo set name='Masha', family='cat' where id=2;",
                    "insert zoo(id, name, family) values (3, 'Vorobey', 'sparrow');",
                    "insert zoo(id, name, family) values (4, 'Slon', 'elephant');",
                    "select * from zoo" };
            Vector<int[]> array = new Vector<int[]>();
            array.addElement(null);
            array.addElement(new int[] { 1, 2, 3 });
            array.addElement(new int[] { 1, 2, 10, 100 });
            array.addElement(new int[] {});

            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.execute(queries[i], (int[]) array.elementAt(i));
                fail("SQLException expected: not supported");
            }
            /*
            fail("Revise test implemenation for feature impl. has changed");
            assertNotNull(st.getResultSet());
            st.close();
            assertNull(st.getResultSet());
            */
        } catch (SQLException e) {
            // ok: not supported
//            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.Statement#execute(String sql, String[] columnNames)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "execute",
        args = {java.lang.String.class, java.lang.String[].class}
    )
    public void testExecute_String_StringArray() {
        Statement st = null;
        try {
            String[] queries = {
                    "update zoo set name='Masha', family='cat' where id=2;",
                    "insert zoo(id, name, family) values (3, 'Vorobey', 'sparrow');",
                    "insert zoo(id, name, family) values (4, 'Slon', 'elephant');",
                    "select * from zoo" };
            Vector<String[]> array = new Vector<String[]>();
            array.addElement(null);
            array.addElement(new String[] { "", "", "", "", "", "", "", "" });
            array.addElement(new String[] { "field 1", "", "field2" });
            array.addElement(new String[] { "id", "family", "name" });

            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.execute(queries[i], (String[]) array.elementAt(i));
                fail("Exception expected: not supported");
            }
            fail("Revise test implemenation for feature impl. has changed");
            assertNotNull(st.getResultSet());
            st.close();
            assertNull(st.getResultSet());
        } catch (SQLException e) {
            // ok: not supported
            try {
                st.close();
            } catch (SQLException ee) {
            }
        } 
    }

    /**
     * @test java.sql.Statement#executeBatch()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test fails: dropping table hutch affects at least 2 rows.executeBatch() always returns same result: 1.",
        method = "executeBatch",
        args = {}
    )
    @KnownFailure("always returns 1 for no. of updates")
    public void testExecuteBatch() {

        String[] queries = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "create view address as select address from hutch where animal_id=2",
                "drop view address;", "drop table hutch;" };
        
        String[] wrongQueries = {
                "update zoo set name='Masha', family='cat' where;",
                "drop table if exists hutch;",
                "create view address as select address from hutch where animal_id=2;",
                "drop view address;", "drop table hutch;" };

        int[] result = { 1, 1, 1, 1, 1, 1, 1, 1 };
        Statement st = null;
        
        //Exception test
        try {
            st = conn.createStatement();
            assertEquals(0, st.executeBatch().length);
            for (int i = 0; i < wrongQueries.length; i++) {
                st.addBatch(wrongQueries[i]);
            }
            st.executeBatch();
            fail("BatchupdateException expected");
        } catch (BatchUpdateException e) {
            //ok
        } catch (SQLException e) {
            fail("BatchupdateException expected");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        } 
        
        try {
            st = conn.createStatement();
            assertEquals(0, st.executeBatch().length);
            for (int i = 0; i < queries.length; i++) {
                st.addBatch(queries[i]);
            }
            int[] resArray = st.executeBatch();
            assertTrue(java.util.Arrays.equals(result, resArray));
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }

        try {
            st = conn.createStatement();
            st.addBatch("select * from zoo");
            st.executeBatch();
            fail("Exception expected");
        } catch (BatchUpdateException bue) {
            // ok select returns a resultSet
        } catch (SQLException sqle) {
            fail("Unknown SQLException is thrown: " + sqle.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        //Exception test
        try {
            st.close();
            st.executeBatch();
            fail("SQLException not thrown");
        } catch (SQLException e) {
            //ok
        }
    }

    /**
     * @test java.sql.Statement#executeQuery(String sql)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Not according to spec.",
        method = "executeQuery",
        args = {java.lang.String.class}
    )
    @KnownFailure("Does throw an exception on non select statment.")
    public void testExecuteQuery_String() {

        String[] queries1 = { "select * from zoo",
                "select name, family from zoo where id = 1" };

        String[] queries2 = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "create view address as select address from hutch where animal_id=2",
                "drop view address;", "drop table hutch;", "select from zoo" };
        
        Statement st = null;
        
        try {
            st = conn.createStatement();
            for (int i = 0; i < queries1.length; i++) {
                try {
                    ResultSet rs = st.executeQuery(queries1[i]);
                    assertNotNull(rs);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown for query: " + queries1[i]);
                }
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (Exception ee) {
            }
        }
        
        // queries which do not produce a ResultSet -> exception testing 
        
        try {
            st = conn.createStatement();
            for (int i = 0; i < queries2.length; i++) {
                try {
                    ResultSet rs = st.executeQuery(queries2[i]);
                    assertNotNull(rs);
                    fail("SQLException is not thrown for query: " + queries2[i]);
                } catch (SQLException sqle) {
                    // expected
                }
            }
        } catch (SQLException sqle) {
            fail("Unknown SQLException is thrown: " + sqle.getMessage());
        } finally {
            try {
                st.close();
            } catch (Exception ee) {
            }
        } 

    }

    /**
     * @throws SQLException 
     * @test java.sql.Statement#executeUpdate(String sql)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "impl not according to spec.",
        method = "executeUpdate",
        args = {java.lang.String.class}
    )
    @KnownFailure("Spec is not precise enough: should be: number of rows affected."+
            " eg. to be consistent for deletes: 'delete from s1;' should be different from "+
            "'delete from s1 where c1 = 1;' ")
    public void testExecuteUpdate_String() throws SQLException {

        String[] queries1 = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "create view address as select address from hutch where animal_id=2;",
                "drop view address;", "drop table hutch;"};

        String queries2 = "select * from zoo;";

        Statement st = null;
        try {
            st = conn.createStatement();
            for (int i = 0; i < queries1.length; i++) {
                try {
                    int count = st.executeUpdate(queries1[i]);
                    assertTrue(count > 0);
                } catch (SQLException e) {
                    fail("SQLException is thrown: " + e.getMessage());
                }
            }

            try {
               assertEquals(0, st.executeUpdate(queries2));
            } catch (SQLException e) {
               fail("SQLException is thrown: " + e.getMessage());
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (Exception ee) {
            }
        }
        
        // test return value for specific numbers 
        
        Statement stat = conn.createStatement();
        
        // there are 0 rows created therefore 0 should be returned.
        assertEquals(0 ,stat.executeUpdate("create table s1 (c1);"));
        
        assertEquals(1, stat.executeUpdate("insert into s1 values (0);"));
        assertEquals(1, stat.executeUpdate("insert into s1 values (1);"));
        assertEquals(1, stat.executeUpdate("insert into s1 values (2);"));
        assertEquals(1,stat.executeUpdate("delete from s1 where c1 = 1;"));
        assertEquals(2, stat.executeUpdate("update s1 set c1 = 5;"));
        
        // analogous to statemente before, delete all should return 2
        assertEquals(2,stat.executeUpdate("delete from s1;"));
        
        // there are no rows in table: 0 should be returned
        assertEquals(0, stat.executeUpdate("drop table s1;"));
        
        stat.executeUpdate("create table s1 (c1);"); 
        stat.executeUpdate("insert into s1 values (0);");
        stat.executeUpdate("insert into s1 values (1);");
        stat.executeUpdate("insert into s1 values (2);");
        
        // there are 3 rows in table: 3 should be returned
        assertEquals(3, stat.executeUpdate("drop table s1;"));
        
        stat.close();
    }

    /**
     * @test java.sql.Statement#executeUpdate(String sql, int[] columnIndexes)
     * 
     * TODO executeUpdate(String sql, int[] columnIndexes) is not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "executeUpdate",
        args = {java.lang.String.class, int[].class}
    )
    public void testExecuteUpdate_String_intArray() {
        Statement st = null;
        try {
            String[] queries1 = {
                    "update zoo set name='Masha', family='cat' where id=2;",
                    "drop table if exists hutch",
                    "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                    "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                    "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                    "create view address as select address from hutch where animal_id=2",
                    "drop view address;", "drop table hutch;" };

            Vector<int[]> array = new Vector<int[]>();
            array.addElement(null);
            array.addElement(new int[] { 1, 2, 3 });
            array.addElement(new int[] { 1, 2, 10, 100 });
            array.addElement(new int[] {});
            array.addElement(new int[] { 100, 200 });
            array.addElement(new int[] { -1, 0 });
            array.addElement(new int[] { 0, 0, 0, 1, 2, 3 });
            array.addElement(new int[] { -100, -200 });

            st = conn.createStatement();
            for (int i = 0; i < queries1.length; i++) {
                st.executeUpdate(queries1[i], (int[]) array.elementAt(i));
                fail("Exception expected");
            }
        } catch (SQLException e) {
            assertEquals("not supported",e.getMessage());
//            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        } 
    }

    /**
     * @test java.sql.Statement#executeUpdate(String sql, int autoGeneratedKeys)
     * 
     * TODO  executeUpdate(String sql, int autoGeneratedKeys) is not supported
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not supported",
        method = "executeUpdate",
        args = {java.lang.String.class, int.class}
    )
    public void testExecuteUpdate_String_int() {
        String[] queries = {
                "update zoo set name='Masha', family='cat' where id=2;",
                "drop table if exists hutch",
                "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                "select animal_id, address from hutch where animal_id=1;",
                "create view address as select address from hutch where animal_id=2",
                "drop view address;", "drop table hutch;" };

            Statement st = null;
            ResultSet rs = null;
            try {
                st = conn.createStatement();
                st.executeUpdate(queries[1], Statement.NO_GENERATED_KEYS);
                rs = st.getGeneratedKeys();
                assertFalse(rs.next());
                fail("Exception expected: not supported");
            } catch (SQLException e) {
                //ok
            } finally {
                try {
                    rs.close();
                    st.close();
                } catch (Exception ee) {
                }
            }
            
            try {
                st = conn.createStatement();
                st.executeUpdate(queries[1], Statement.RETURN_GENERATED_KEYS);
                rs = st.getGeneratedKeys();
                assertTrue(rs.next());
                fail("Exception expected: not supported");
            } catch (SQLException e) {
                //ok
            } finally {
                try {
                    rs.close();
                    st.close();
                } catch (Exception ee) {
                }
            }
    }

    /**
     * @test java.sql.Statement#executeUpdate(String sql, String[] columnNames)
     * 
     * TODO executeUpdate(String sql, String[] columnNames) is not supported
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "executeUpdate",
        args = {java.lang.String.class, java.lang.String[].class}
    )
    public void testExecuteUpdate_String_StringArray() {
        Statement st = null;
        try {
            String[] queries = {
                    "update zoo set name='Masha', family='cat' where id=2;",
                    "drop table if exists hutch",
                    "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));",
                    "insert into hutch (id, animal_id, address) values (1, 2, 'Birds-house, 1');",
                    "insert into hutch (id, animal_id, address) values (2, 1, 'Horse-house, 5');",
                    "create view address as select address from hutch where animal_id=2",
                    "drop view address;", "drop table hutch;" };

            Vector<String[]> array = new Vector<String[]>();
            array.addElement(null);
            array.addElement(new String[] { "", "", "", "", "", "", "", "" });
            array.addElement(new String[] { "field 1", "", "field2" });
            array.addElement(new String[] { "id", "family", "name" });
            array
                    .addElement(new String[] { "id", null, "family", null,
                            "name" });
            array.addElement(new String[] { "id", " ", "name" });
            array.addElement(new String[] { null, null, null, null });
            array.addElement(new String[] { " ", "123 21", "~!@#$%^&*()_+ ",
                    null });

            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.executeUpdate(queries[i], (String[]) array.elementAt(i));
                fail("Revise test implemenation for feature impl. has changed");
            }
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
//            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        } 
    }

    /**
     * @test java.sql.Statement#getUpdateCount()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "SQLException test fails",
        method = "getUpdateCount",
        args = {}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testGetUpdateCount() {
        Statement st = null;
        try {
            String query = "update zoo set name='Masha', family='cat' where id=2;";
            st = conn.createStatement();
            st.executeUpdate(query);
            assertEquals(1, st.getUpdateCount());
            query = "update zoo set name='Masha', family='cat' where id=5;";
            st.executeUpdate(query);
            assertEquals(0, st.getUpdateCount());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        } 
        // statment closed : Exception test
        try {
            st.getUpdateCount();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getGeneratedKeys()}
     * 
     * TODO getGeneratedKeys() is not supported
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not supported",
        method = "getGeneratedKeys",
        args = {}
    )
    public void testGeneratedKeys() {
        Statement st = null;
        try {
            String insert = "insert into zoo (id, name, family) values (8, 'Tuzik', 'dog');";
            st = conn.createStatement();
            assertNull(st.getGeneratedKeys());
            fail("Fail: statement does not fail");
        } catch (SQLException e) {
          assertEquals("not supported", e.getMessage());
        }
    }
    
    /**
     * @test {@link java.sql.Statement#setCursorName(String)}
     * 
     * TODO setCursorName() is not supported
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not supported",
        method = "setCursorName",
        args = {java.lang.String.class}
    )
    public void testSetCursorName() {
        Statement st = null;
        try {
            String select = "select * from zoo";
            st = conn.createStatement();
            st.setCursorName("test");
            fail("Fail: statement does not fail");
        } catch (SQLException e) {
          assertEquals("not supported", e.getMessage());
        }
    }
    
    /**
     * @test {@link java.sql.Statement#setEscapeProcessing}
     * 
     * TODO setExcapeProcessing() is not supported
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not supported",
        method = "setEscapeProcessing",
        args = {boolean.class}
    )
    public void testSetEscapeProcessing() {
        Statement st = null;
        try {
            String select = "select * from zoo";
            st = conn.createStatement();
            st.setEscapeProcessing(true);
            fail("Fail: statement does not fail");
        } catch (SQLException e) {
          assertEquals("not supported", e.getMessage());
        }
        
    }
    
    /**
     * @test {@link java.sql.Statement#setQueryTimeout}
     * 
     */
    @TestTargets({
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Error in impl. default query timeout for sqlite dbs is 0.",
        method = "setQueryTimeout",
        args = {int.class}
    ),
    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Error in impl. default query timeout for sqlite dbs is 0.",
            method = "getQueryTimeout",
            args = {}
        )
    })
    @KnownFailure("Error in implementation either setter or getter fails. "+
            "Getter spec is not explicit about unit.")
    public void testSetQueryTimeout() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.setQueryTimeout(2000);
            assertEquals(2000, st.getQueryTimeout());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        }
        
        st = null;
        try {
            st = conn.createStatement();
            st.setQueryTimeout(-1);
           fail("SQLException is not thrown");
        } catch (SQLException e) {
            // expected 
        }
        
    }
    
    /**
     * @test {@link java.sql.Statement#getResultSetType()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Tests fail. not fully supported: returns only ResultSet.TYPE_SCROLL_INSENSITIVE. Either should throw an unsupported exception or behave according to spec.",
        method = "getResultSetType",
        args = {}
    )
    @KnownFailure("not fully supported")
    public void testGetResultSetType() {
        Statement st = null;
        // test default value 
        try {
            st = conn.createStatement();
            st.getResultSetType();
            assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, st
                    .getResultSetType());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }

        // failing tests
        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            st.getResultSetType();
            assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, st.getResultSetType());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }

        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            st.getResultSetType();
            assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, st.getResultSetType());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }

        try {
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE);
            st.getResultSetType();
            assertEquals(ResultSet.TYPE_FORWARD_ONLY, st.getResultSetType());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getResultSetHoldability()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not supported",
        method = "getResultSetHoldability",
        args = {}
    )
    @KnownFailure("Test for default value fails")
    public void testGetResultSetHoldability() {
        
        // test default value 
        Statement st = null;
        try {
            st = conn.createStatement();
            assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, st
                    .getResultSetHoldability());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }

        // failing tests
        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.HOLD_CURSORS_OVER_COMMIT);
            fail("Exception expected: not supported");
            /*
            st.getResultSetHoldability();
            assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, st
                    .getResultSetHoldability());
            */
        } catch (SQLException e) {
            // ok: not supported
        }

        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.CLOSE_CURSORS_AT_COMMIT);
            fail("Exception expected: not supported");
            /*
            st.getResultSetHoldability();
            assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, st
                    .getResultSetHoldability());
            */
        } catch (SQLException e) {
         // ok: not supported
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getResultSetConcurrency()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "Tests fail. returns only ResultSet.TYPE_SCROLL_INSENSITIVE. Either should throw an unsupported exception or behave according to spec.",
        method = "getResultSetConcurrency",
        args = {}
    )
    @KnownFailure("Not supported")
    public void testGetResultSetConcurrency() {
        Statement st = null;
        
        // test default value 
        try {
            st = conn.createStatement();
            st.getResultSetConcurrency();
            assertEquals(ResultSet.CONCUR_READ_ONLY, st
                    .getResultSetConcurrency());
        } catch (SQLException e) {
            assertEquals("not supported", e.getMessage());
        }
       
     // failing tests

        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            st.getResultSetConcurrency();
            assertEquals(ResultSet.CONCUR_UPDATABLE, st.getResultSetConcurrency());
            fail("Exception expected: not supported");
        } catch (SQLException e) {
            //ok
       
        }
        
        try {
            st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            st.getResultSetConcurrency();
            assertEquals(ResultSet.CONCUR_READ_ONLY, st.getResultSetConcurrency());
            fail("Exception expected: not supported");
        } catch (SQLException e) {
            //ok;
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getResultSet()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Error in implementation. Is not according to spec:if updateCount > 0 resultset must be null.",
        method = "getResultSet",
        args = {}
    )
    @KnownFailure("Does not return null on update count > 0 (not a select statement) ")
    public void testGetResultSet() {
        Statement st = null;
        ResultSet res = null;
        
        try {
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.CLOSE_CURSORS_AT_COMMIT);
            st.execute("create table test (c1);");
            res = st.getResultSet();
            assertNull(res);
        } catch (SQLException e) {
            fail("Unexpected Exception "+e);
        }
        
        try {
            st = conn.createStatement();
            String select = "select * from zoo where id == 4;";
            String insert =  "insert into zoo (id, name, family) values (4, 'Vorobuy', 'bear');";
            st.execute(insert);
            st.execute(select);
            assertEquals(-1, st.getUpdateCount());
            res = st.getResultSet();
            assertNotNull(res);
            res.next();
            assertEquals(4,res.getInt(1));
            assertEquals("Vorobuy",res.getString(2));
            assertEquals("bear",res.getString(3));
//            assertEquals(0, st.getUpdateCount()); not supported            
            assertFalse(res.next());
        } catch (SQLException e) {
            fail("SQLException is thrown:"+e.getMessage());
        }
        
        try {
            st = conn.createStatement();
            String insert = "insert into zoo (id, name, family) values (3, 'Vorobey', 'sparrow');";
            st
            .execute(insert);
            res = st.getResultSet();
            // statement is an update and should return null according to spec. 
            if (st.getUpdateCount() > 0)  {
                assertNull(res);
            }
           
        } catch (SQLException e) {
            fail("SQLException is thrown:"+e.getMessage());
        }
        
        try {
            st.close();
            st.getResultSet();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }
    
    /**
     * @test {@link java.sql.Statement#setQueryTimeout}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Errors in impl.An other value is returned than was set (X * 1000)Default query timeout for sqlite dbs is 0.",
        method = "getQueryTimeout",
        args = {}
    )
    @KnownFailure("An other value is returned than was set (X * 1000)")
    public void testGetQueryTimeout() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.setQueryTimeout(2000);
            assertEquals(2000, st.getQueryTimeout());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        }
        
        try {
            st = conn.createStatement();
            assertEquals(0,st.getQueryTimeout());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        }
        
        try {
            st.close();
            st.getQueryTimeout();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getMoreResults()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "not fully supported",
        method = "getMoreResults",
        args = {}
    )
    @KnownFailure("not supported")
    public void testGetMoreResults() {
        Statement st = null;
        ResultSet res1 = null;
        ResultSet res2 = null;
        String[] queries = {
                "insert into zoo values (3,'John','bird');",
                "update zoo set name='Masha', family='cat' where id=3;",
                "update zoo set name='Masha', family='bear' where id=3;"};
        
       try {
            st = conn.createStatement();
            st.execute(queries[0]);
            assertFalse(st.getMoreResults());
 
            try {
                st.getResultSet();
                fail("Exception expected");
            } catch (SQLException e) {
                //ok
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st.getMoreResults();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }
    
    /**
     * @test {@link java.sql.Statement#getMoreResults(int)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "Callable Statements are not supported",
        method = "getMoreResults",
        args = {int.class}
    )
    public void testGetMoreResultsInt() {
        /*
        } catch (BatchUpdateException e) {
            fail("Unexpected Exception "+e.getMessage());
        } catch (SQLException e) {
            assertEquals("not supported",e.getMessage());
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        
        try {
            st.getMoreResults(Integer.MAX_VALUE);
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
        */
    }
    
    /**
     * @test {@link java.sql.Statement#cancel()}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test fails. See also SQLite.DatabaseTest test of interrupt().",
        method = "cancel",
        args = {}
    )
    @KnownFailure("Bug in implementation of cancel: Does not fulfill spec.")
    public void testCancel() {
        Statement st = null;
        try {
            st = conn.prepareStatement("insert into zoo values (7,'Speedy Gonzales','Mouse');");
            
            CancelThread c = new CancelThread(st);
            InsertThread ins = new InsertThread((PreparedStatement)st);
            
            try {
                ins.t.join();
                c.t.join();
            } catch (InterruptedException e) {
                fail("Error in test setup: ");
            } catch (Exception e){
                // Insert thread may throw an exception 
                // that it could not complete statement
            }
            
            // both threads have terminated and cancel should have cancelled the insert statement.
            ResultSet res = st.executeQuery("select * from zoo where id=7");
            assertFalse(res.next());
            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        }
        
        try {
            st.close();
            st.cancel();
            fail("Exception expected");
        } catch (SQLException e) {
            //ok
        }
    }
    
    class CancelThread implements Runnable{
        Thread t;
        Statement stmt;
        CancelThread (Statement aSt) {
           this.stmt = aSt;
           t = new Thread(this,"Cancel thread");
           t.start();
        }
        public void run() {
           Logger.global.info("*Cancel* thread started");
           try {
             Thread.sleep(1500);
         } catch (InterruptedException e1) {
             fail("Error in test setup");
             e1.printStackTrace();
         }             
           try {
               Logger.global.info("*Cancel* thread, about to do stmt.cancel()");  
               stmt.cancel();
               Logger.global.info("*Cancel* thread, stmt.cancel() done");  
           } catch (SQLException e) {
               fail("Error in test setup");
               e.printStackTrace();
           }
           Logger.global.info("*Cancel* thread terminated");
        }
     }
    
    class InsertThread implements Runnable{
        Thread t;
        PreparedStatement stmt;
        InsertThread (PreparedStatement aSt) {
           this.stmt = aSt;
           t = new Thread(this,"Insert thread");
           t.start();
        }
        public void run() {
          Logger.global.info("*Insert* thread started");
           try {
             Thread.sleep(1500);
         } catch (InterruptedException e1) {
             fail("Error in test setup");
             e1.printStackTrace();
         }             
           try {
               Logger.global.info("*Insert* thread, about to do insertion");  
               stmt.execute();
               stmt.execute();
               Logger.global.info("*Insert* thread inserted");  
           } catch (SQLException e) {
               fail("Error in test setup");
               e.printStackTrace();
           }
          Logger.global.info("*Insert* thread terminated");
        }
     }

}
