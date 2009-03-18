/*
 * Copyright (C) 2007 Google Inc.
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
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@TestTargetClass(PreparedStatement.class)
public class PreparedStatementTest extends SQLTest {

    String queryAllSelect = "select * from type";

    String[] queries = {
            "create table type (" +

            " BoolVal BOOLEAN," + " IntVal INT," + " LongVal LONG,"
                    + " Bint BIGINT," + " Tint TINYINT," + " Sint SMALLINT,"
                    + " Mint MEDIUMINT, " +

                    " IntegerVal INTEGER, " + " RealVal REAL, "
                    + " DoubleVal DOUBLE, " + " FloatVal FLOAT, "
                    + " DecVal DECIMAL, " +

                    " NumVal NUMERIC, " + " charStr CHAR(20), "
                    + " dateVal DATE, " + " timeVal TIME, " + " TS TIMESTAMP, "
                    +

                    " DT DATETIME, " + " TBlob TINYBLOB, " + " BlobVal BLOB, "
                    + " MBlob MEDIUMBLOB, " + " LBlob LONGBLOB, " +

                    " TText TINYTEXT, " + " TextVal TEXT, "
                    + " MText MEDIUMTEXT, " + " LText LONGTEXT " + ");",

            "insert into type (BoolVal, IntVal, LongVal, Bint, Tint, Sint, Mint,"
                    + "IntegerVal, RealVal, DoubleVal, FloatVal, DecVal,"
                    + "NumVal, charStr, dateVal, timeVal, TS,"
                    + "DT, TBlob, BlobVal, MBlob, LBlob,"
                    + "TText, TextVal, MText, LText"
                    + ") "
                    + "values (1, -1, 22, 2, 33,"
                    + "3, 1, 2, 3.9, 23.2, 33.3, 44,"
                    + "5, 'test string', '1799-05-26', '12:35:45', '2007-10-09 14:28:02.0',"
                    + "'1221-09-22 10:11:55', 1, 2, 3, 4,"
                    + "'Test text message tiny', 'Test text message', 'Test text message medium', 'Test text message long');" };

    public void setUp() {
        super.setUp();
        Statement st = null;
        try {
            st = conn.createStatement();
            for (int i = 0; i < queries.length; i++) {
                st.execute(queries[i]);
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.toString());
        } finally {
            try {
                st.close();
            } catch (Exception ee) {
            }
        }
    }

    public void tearDown() {
        Statement st = null;
        try {
            st = conn.createStatement();
            st.execute("drop table if exists type");
        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                st.close();
            } catch (SQLException ee) {
            }
        }
        super.tearDown();
    }

    /**
     * @test java.sql.PreparedStatement#addBatch()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addBatch",
        args = {}
    )
    public void testAddBatch() throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn
                    .prepareStatement("INSERT INTO zoo VALUES (3,'Tuzik', ?);");
            ps.addBatch("INSERT INTO zoo VALUES (?,'Burenka', ?); ");
            ps.addBatch("INSERT INTO zoo VALUES (?,'Mashka','cat')");
            try {
                ps.executeBatch();
            } catch (SQLException sqle) {
                fail("SQLException is thrown for executeBatch()");
            }
            ps.setString(1, "dog");
            Statement st = null;
            try {
                ps.executeBatch();
                st = conn.createStatement();
                st.execute("select * from zoo");
                ResultSet rs = st.getResultSet();
                assertEquals(2, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown for executeBatch()");
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }
        } catch (SQLException e) {
            fail("SQLException is thrown "+e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }

        try {
            ps = conn
                    .prepareStatement("INSERT INTO zoo VALUES (3,'Tuzik', ?);");
            ps.addBatch("");
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }

        try {
            ps = conn
                    .prepareStatement("INSERT INTO zoo VALUES (3,'Tuzik', ?);");
            ps.addBatch(null);
        } catch (SQLException e) {
            // expected
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

   
    /**
     * @test java.sql.PreparedStatement#execute()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "execute",
        args = {}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testExecute() {
        Statement st = null;
        PreparedStatement ps = null;
        try {
            //update
            String query = "insert into zoo(id, family, name) values(?, ?, 'unknown animal')";
            ps = conn.prepareStatement(query);
            ps.setInt(1, 3);
            ps.setString(2, "No name");
            assertFalse(ps.execute());
            assertEquals(1,ps.getUpdateCount());
            
            // select
            ps = conn.prepareStatement("select * from zoo");
            assertTrue(ps.execute());
            assertEquals(3, getCount(ps.getResultSet()));
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }

        try {
            String query = "update zoo set name='Masha', family=? where id=?;";
            ps = conn.prepareStatement(query);
            ps.setString(1, "cat");
            ps.setInt(2, 2);
            assertFalse(ps.execute());
            assertEquals(1, ps.getUpdateCount());
            st = conn.createStatement();
            st.execute("select family from zoo where id=2");
            ResultSet rs = st.getResultSet();
            rs.next();
            assertEquals("cat", rs.getString(1));
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                ps.close();
                st.close();
            } catch (Exception ee) {
            }
        }

        try {
            conn.createStatement().execute("drop table if exists hutch");
            String query = "create table hutch (id integer not null, animal_id integer, address char(20), primary key (id));";
            ps = conn.prepareStatement(query);
            assertFalse(ps.execute());
            assertTrue(ps.getUpdateCount() > 0);
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }

        try {
            String query = "select name, family from zoo where id = ?";
            ps = conn.prepareStatement(query);
            ps.setInt(1, 1);
            assertTrue(ps.execute());
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }

        try {
            String query = "select name, family from zoo where id = ?";
            ps = conn.prepareStatement(query);
            ps.execute();
        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }
        //Exception test
        try {
            String query = "update zoo set name='Masha', family=? where id=?;";
            ps = conn.prepareStatement(query);
            ps.setString(1, "cat");
            ps.setInt(2, 2);
            assertTrue(ps.execute("update zoo set name='Masha', family='cat' where id=2;"));
        } catch (SQLException e) {
            // ok Should not provide string argument for a prepared Statement
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }
    }
     
   
    /**
     * @test java.sql.PreparedStatement#executeQuery()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "executeQuery",
        args = {}
    )
      public void testExecuteQuery() {

        String[] queries2 = {
                "update zoo set name='Masha', family='cat' where id=;",
                "insert into hutch (id, animal_id, address) values (1, ?,'Birds-house, 1');",
                "insert into hutch (id, animal_id, address) values (?, 1, 'Horse-house, 5');",
                "create view address as select address from hutch where animal_id=?"};

        for (int i = 0; i < queries2.length; i++) {
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(queries2[i]);
                ps.executeQuery();
                fail("SQLException is not thrown for query: " + queries2[i]);
            } catch (SQLException sqle) {
                // expected
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }
        }

        String query = "select * from zoo where id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(query);
            ps.setInt(1, 1);
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt(1));
            assertEquals("Kesha", rs.getString(2));
            assertEquals("parrot", rs.getString(3));
        } catch (SQLException e) {
            fail("SQLException is thrown for query");
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }

        try {
            ps = conn.prepareStatement(query);
            ps.setInt(1, 5);
            ResultSet rs = ps.executeQuery();
            assertNotNull(rs);
            assertFalse(rs.next());
        } catch (SQLException e) {
            fail("SQLException is thrown for query");
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
            }
        }
    }
     
     // TODO Crashes VM. Fix later.
    /**
     * @test {@link java.sql.PreparedStatement#executeUpdate()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "executeUpdate",
        args = {}
    )
      public void testExecuteUpdate() { 
          String[] queries1 = { "insert into hutch (id, animal_id, address) values (1, ?, 'Birds-house, 1');", 
                              "insert into hutch (id, animal_id, address) values (?, 1, 'Horse-house, 5');",
                              "create view address as select address from hutch where animal_id=2" };
      
          for (int i = 0; i < queries1.length; i++) { 
              PreparedStatement ps = null;
              try { 
                  ps = conn.prepareStatement(queries1[i]); 
                  ps.executeUpdate();
                  fail("SQLException is not thrown for query: " + queries1[i]); 
          } catch(SQLException sqle) { 
              // expected 
          } finally { 
              try { 
                  ps.close(); 
              } catch(Exception ee) {} 
          } 
      } 
      
          String query = "update zoo set name='Masha', family='cat' where id=?;"; 
          PreparedStatement ps = null; 
          try {
              ps = conn.prepareStatement(query); 
              ps.setInt(1, 2); 
              int updateCount = ps.executeUpdate(); 
              assertEquals(1, updateCount); 
              ps.setInt(1, 1); 
              int updateCount1 = ps.executeUpdate(); 
              assertEquals(1, updateCount1); 
          } catch (SQLException e) { 
              fail("SQLException is thrown for query"); 
          } finally {
              try { 
                  ps.close(); 
              } catch(Exception ee) {} 
          } 
      }
      
    /**
     * @test java.sql.PreparedStatement#getMetaData()
     *  
     *  Test Fails:
     * TODO Doesn't pass. according to Java docs:
     * it is possible to invoke the method getMetaData on a 
     * PreparedStatement object before it is executed.
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMetaData",
        args = {}
    )
    @KnownFailure("it is not possible to invoke the method getMetaData on a " + 
                  "PreparedStatement object before it is executed: got NullPointerException."+
                  "Test passes on RI.")
    public void testGetMetaData() {
        PreparedStatement ps = null;
        
        // Specification testing
        
        try {
            String query = "update zoo set name='Masha', family='cat' where id=?;";
            ps = conn.prepareStatement(query);
            assertNotNull(ps);
            ResultSetMetaData meta = ps.getMetaData();
            assertNotNull(meta);
        } catch (SQLException sqle) {
            fail("SQLException is thrown: " + sqle.toString());
            sqle.printStackTrace();
        } catch (Exception e) {
            fail("Unspecified Exception: " + e.toString());
            e.printStackTrace();
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }

        try {
            String query = "select * from zoo where id = ?";
            ps = conn.prepareStatement(query);
            ResultSetMetaData rsmd = ps.getMetaData();
            assertNotNull(rsmd);
            assertEquals(3, rsmd.getColumnCount());
            assertEquals("id", rsmd.getColumnName(1));
        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }
        
        // ps closed
        try {
            ps.getMetaData();
            fail("SQLException expected");
        } catch (SQLException e) {
            // ok
        }
    }

    /**
     * @throws SQLException 
     * @test java.sql.PreparedStatement#getParameterMetaData()
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "getParameterMetaData",
        args = {}
    )
    @KnownFailure("not supported")
    public void testGetParameterMetaData() throws SQLException {
        PreparedStatement ps = null;
        String query = "select * from zoo where id = ?";
        ps = conn.prepareStatement(query);
        
        try {
            ParameterMetaData rsmd = ps.getParameterMetaData();
        } catch (SQLException e) {
            assertEquals("not supported",e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }
        
        ps.close();
     
        try {
            ps.getParameterMetaData();
            fail("SQLException expected");
        } catch (SQLException e) {
            // ok
        }
    }
    
    
    /**
     * @test java.sql.PreparedStatement#clearParameters()
     * Test fails: clearparameters should be implemented with Stmt.reset()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test fails: clearparameters should be implemented with Stmt.reset()",
        method = "clearParameters",
        args = {}
    )
    @KnownFailure("First Exception test fails: parameters not cleared.")
     public void testClearParameters() {
        PreparedStatement ps = null;
        try {
            String query = "select * from zoo where id = ? and family=?";
            ps = conn.prepareStatement(query);
            ps.clearParameters();
            try {
                ps.execute();
                fail("SQLException is not thrown during execute method after calling clearParameters()");
            } catch (SQLException sql) {
                
            }
            ps.setInt(1, 2);
            ps.setString(2, "dog");
            ps.clearParameters();
            try {
                ps.execute();
                fail("SQLException is not thrown during execute method after calling clearParameters()");
            } catch (SQLException sqle) {
                // expected
            }
            ps.setInt(1, 2);
            ps.clearParameters();
            try {
                ps.execute();
                fail("SQLException is not thrown during execute method after calling clearParameters()");
            } catch (SQLException sqle) {
                // expected
            }
            ps.setInt(1, 2);
            ps.setString(2, "cat");

            try {
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown during execute method after calling clearParameters() twice");
            }
        } catch (SQLException e) {
            fail("SQLException is thrown");
        } finally {
            try {
                ps.close();
            } catch (SQLException ee) {
            }
        }
        
    }
     
    /**
     * @test java.sql.PreparedStatement#setInt(int parameterIndex, int x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setInt",
        args = {int.class, int.class}
    )
    public void testSetInt() throws SQLException {
        
        PreparedStatement ps = null;
        Statement st = null;
        try {
            String query = "insert into type (IntVal) values (?);";
            ps = conn.prepareStatement(query);
            try {
                ps.setInt(1, Integer.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where IntVal="
                        + Integer.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                    st.close();
                } catch (Exception ee) {
                }
            }
            ps = conn.prepareStatement(query);
            try {
                ps.setInt(1, Integer.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where IntVal="
                        + Integer.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                    st.close();
                } catch (SQLException ee) {
                }
            }
            ps = conn.prepareStatement(query);
            ps.close();
            try {
                ps.setInt(1, Integer.MIN_VALUE);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setLong(int parameterIndex, long x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setLong",
        args = {int.class, long.class}
    )
    public void testSetLong() {
        
        PreparedStatement ps = null;
        try {
            String query = "insert into type (LongVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setLong(1, Long.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where LongVal="
                                + Long.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setLong(1, Long.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where LongVal="
                                + Long.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }
            
            ps.close();
            try {
                ps.setLong(1, Long.MIN_VALUE);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }

    }

    /**
     * @throws SQLException 
     * @test java.sql.PreparedStatement#setFloat(int parameterIndex, float x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setFloat",
        args = {int.class, float.class}
    )
    public void testSetFloat() throws SQLException {
        float value1 = 12345678.12345689f;
        float value2 = -12345678.12345689f;
        
        PreparedStatement ps = null;
        String query = "insert into type (FloatVal) values (?);";
        ps = conn.prepareStatement(query);
        
        try {
            
            Statement st = null;
            try {
                ps.setFloat(1, value1);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where FloatVal=" + value1);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setFloat(1, value2);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where FloatVal=" + value2);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                    
                }
            }
            ps.close();
            try {
                ps.setFloat(1, Float.MIN_VALUE);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @throws SQLException 
     * @test java.sql.PreparedStatement#setDouble(int parameterIndex, double x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDouble",
        args = {int.class, double.class}
    )
    public void testSetDouble() throws SQLException {
        
        PreparedStatement ps = null;
        String query = "insert into type (DoubleVal) values (?);";
        ps = conn.prepareStatement(query);
        
        try {
            
            Statement st = null;
            try {
                ps.setDouble(1, Double.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where DoubleVal="
                        + Double.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setDouble(1, Double.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where DoubleVal="
                        + Double.MIN_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }
            
            ps.close();
            try {
                ps.setDouble(1, 2.0);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }

    }

    /**
     * @test java.sql.PreparedStatement#setString(int parameterIndex, String x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setString",
        args = {int.class, java.lang.String.class}
    )
    public void testSetString_charField() {
        
        PreparedStatement ps = null;
        
        try {
            String query = "insert into type (charStr) values (?);";
            ps = conn.prepareStatement(query);
            
            String str = "test^text$test%";
            Statement st = null;
            try {
                ps.setString(1, str);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where charStr='" + str + "'");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where charStr=''");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            try {
                ps.setString(1, "                   ");
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where charStr='                   '");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }
            
            try {
                ps.setString(1, " test & text * test % text * test ^ text ");
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }

            try {
                ps.setString(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
            
            ps.close();
            
            try {
                ps.setString(1, "test text");
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setString(int parameterIndex, String x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setString",
        args = {int.class, java.lang.String.class}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testSetString_tinyTextField() {
        
        PreparedStatement ps = null;
        try {
            String str = "test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test";
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setString(1, str);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where TText='" + str + "'");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where TText=''");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            try {
                ps.setString(1, "                   ");
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where TText='                   '");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }
            
            try {
                ps.setString(
                                1,
                                "test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test*test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test-test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test+test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test?test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test#test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test ");
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }

            try {
                ps.setString(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
            
            ps.close();

            try {
                ps.setString(1, "test text");
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setString(int parameterIndex, String x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setString",
        args = {int.class, java.lang.String.class}
    )
    public void testSetString_textField() {
        
        PreparedStatement ps = null;
        try {
            String str = "test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test";
            String query = "insert into type (TextVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setString(1, str);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where TextVal='" + str + "'");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where TextVal=''");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            try {
                ps.setString(1, "                   ");
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where TextVal='                   '");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }


            try {
                String longString = " test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/";
                for (int i = 0; i < 10; i++) {
                    longString += longString;
                }
                ps.setString(1, longString);
                ps.execute();
 
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }

            try {
                ps.setString(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
            
            ps.close();
            
            try {
                ps.setString(2, "test text");
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setString(int parameterIndex, String x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setString",
        args = {int.class, java.lang.String.class}
    )
    public void testSetString_mediumTextField() {
        
        PreparedStatement ps = null;
        try {
            String str = "test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test";
            String query = "insert into type (MText) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setString(1, str);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where MText='" + str + "'");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where MText=''");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "                   ");
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where MText='                   '");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
            
            try {
                ps.setString(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
            
            ps.close();

            try {
                ps.setString(2, "test text");
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }


            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setString(int parameterIndex, String x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setString",
        args = {int.class, java.lang.String.class}
    )
    public void testSetString_longTextField() {
        
        PreparedStatement ps = null;
        try {
            String str = "test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test/test^text$test%test(text)test@text5test~test^text$test%test(text)test@text5test";
            String query = "insert into type (LText) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setString(1, str);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where LText='" + str + "'");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where LText=''");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setString(1, "                   ");
                ps.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where LText='                   '");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
            
            try {
                ps.setString(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
            
            ps.close();

            try {
                ps.setString(1, "test text");
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setShort(int parameterIndex, short x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setShort",
        args = {int.class, short.class}
    )
    public void testSetShort() {
        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            String query = "insert into type (Sint) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setShort(1, Short.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where Sint=" + Short.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setShort(1, Short.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where Sint=" + Short.MIN_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
            
            ps.close();

            try {
                ps.setShort(1, Short.MIN_VALUE);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);
            try {
                ps1.setShort(1, Short.MAX_VALUE);
            } catch (SQLException sqle) {
                fail("SQLException is thrown: "+sqle.getMessage());
            }

            String query2 = "insert into type (IntVal) values (?);";
            ps2 = conn.prepareStatement(query2);
            try {
                ps2.setShort(1, Short.MAX_VALUE);
                ps2.execute();
                st = conn.createStatement();
                st
                        .execute("select * from type where IntVal="
                                + Short.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
                ps2.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setBoolean(int parameterIndex, boolean
     *       x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setBoolean",
        args = {int.class, boolean.class}
    )
    public void testSetBoolean() {
        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (BoolVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setBoolean(1, false);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where BoolVal = 0");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            try {
                ps.setBoolean(1, true);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where BoolVal= 1");
                ResultSet rs = st.getResultSet();
                assertEquals(2, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
            
            ps.close();

            try {
                ps.setBoolean(1, false);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);
            try {
                ps1.setBoolean(1, true);
                ps1.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setByte(int parameterIndex, byte x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setByte",
        args = {int.class, byte.class}
    )
    public void testSetByte() {
        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (Tint) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setByte(1, Byte.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where Tint=" + Byte.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            try {
                ps.setByte(1, Byte.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where Tint=" + Byte.MIN_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            try {
                ps.setByte(2, Byte.MAX_VALUE);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            
            ps.close();

            try {
                ps.setByte(1, Byte.MIN_VALUE);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            String query1 = "insert into type (IntVal) values (?);";
            ps1 = conn.prepareStatement(query1);
            try {
                ps1.setByte(1, Byte.MAX_VALUE);
                ps1.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setBytes(int parameterIndex, byte[] x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setBytes",
        args = {int.class, byte[].class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testSetBytes() {

        byte[] bytesArray = {1, 0};
        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (LBlob) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setBytes(1, bytesArray);
                assertFalse(ps.execute());
                assertTrue(ps.getUpdateCount() > 0);
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } 

            try {
                ps.setBytes(2, bytesArray);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected RuntimeException or SQLException
            }
            
            ps.close();

            try {
                ps.setBytes(1, bytesArray);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }
            String query1 = "insert into type (TBlob) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps.setBytes(1, bytesArray);
                assertFalse(ps.execute());
                assertTrue(ps.getUpdateCount() > 0);
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } 
            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                if (ps != null) ps.close();
                if (ps1 != null) ps1.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setBigDecimal(int parameterIndex,
     *       BigDecimal x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setBigDecimal",
        args = {int.class, java.math.BigDecimal.class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testSetBigDecimal() {

        BigDecimal bd = new BigDecimal("50");
        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (DecVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setBigDecimal(1, bd);
                assertFalse(ps.execute());
                assertTrue(ps.getUpdateCount() > 0);
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } 
            

            try {
                ps.setBigDecimal(2, bd);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
                assertEquals("bad parameter index", sqle.getMessage());
            }

            try {
                ps.setBigDecimal(-2, bd);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
                assertEquals("bad parameter index", sqle.getMessage());
            }
            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setBigDecimal(1, bd);
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                if (ps != null) ps.close();
                if (ps1 != null) ps1.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setDate(int parameterIndex, Date x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "First exception test fails: integer and date are incompatible"
               +" by spec.",
        method = "setDate",
        args = {int.class, java.sql.Date.class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update. "+
            "Setting a data for a declared INTEGER should throw Exception")
    public void testSetDate_int_Date() {
        Calendar cal = new GregorianCalendar(1799, 5, 26);

        Date[] dates = {
                new Date(cal.getTimeInMillis()), new Date(Integer.MAX_VALUE),
                new Date(123456789)};


        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (dateVal) values (?);";
            ps = conn.prepareStatement(query);

            for (int i = 0; i < dates.length; i++) {
                try {
                    ps.setDate(1, dates[i]);
                    assertFalse(ps.execute());
                    assertTrue(ps.getUpdateCount() > 0);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                }
            }

            try {
                ps.setDate(2, dates[0]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }

            ps.close();

            try {
                ps.setDate(1, dates[0]);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }

            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setDate(1, dates[0]);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
                assertEquals("SQLite.Exception: error in prepare", sqle
                        .getMessage());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {

                if (ps != null) ps.close();
                if (ps1 != null) ps1.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setDate(int parameterIndex, Date x,
     *       Calendar cal)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDate",
        args = {int.class, java.sql.Date.class, java.util.Calendar.class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testSetDate_int_Date_Calendar() {

        Calendar[] cals = { Calendar.getInstance(),
                Calendar.getInstance(Locale.GERMANY),
                Calendar.getInstance(TimeZone.getDefault()) };
        Calendar cal = new GregorianCalendar(1799,5,26);

        Date[] dates = { new Date(cal.getTimeInMillis()), new Date(Integer.MAX_VALUE),
                new Date(123456789) };

        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (dateVal) values (?);";
            ps = conn.prepareStatement(query);

            for (int i = 0; i < dates.length; i++) {
                
                try {
                    ps.setDate(1, dates[i], cals[i]);
                    assertFalse(ps.execute());
                    assertTrue(ps.getUpdateCount() > 0);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                }
            }

            try {
                ps.setDate(2, dates[0], cals[0]);
                ps.execute();
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            
            ps.close();
            
            try {
                ps.setDate(1, dates[0], cals[1]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setDate(1, dates[0], cals[2]);
                ps1.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }
            
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                if (ps != null) ps.close();
                if (ps1 != null) ps1.close();
            } catch (SQLException ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setNull(int parameterIndex, int sqlType)
     * 
     * this test doesn't passed on RI
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setNull",
        args = {int.class, int.class}
    )
    public void testSetNull_int_int() {
        
        PreparedStatement ps = null;
        try {
            String query = "insert into type (BoolVal, IntVal) values ('true', ?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setNull(1, Types.INTEGER);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (BoolVal, LongVal) values ('true', ?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setNull(1, Types.BIGINT);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (BoolVal, DecVal) values ('true', ?)";
            ps = conn.prepareStatement(query);

            try {
                ps.setNull(1, Types.DECIMAL);
                ps.execute();
             } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (BoolVal, dateVal) values ('true', ?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setNull(1, Types.DATE);
                ps.execute();
            } catch (SQLException sqle) {
               fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (BoolVal, BlobVal) values ('true', ?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setNull(1, Types.BLOB);
                ps.execute();
            } catch (SQLException sqle) {
               fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    ps.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (BoolVal, TextVal) values ('true', ?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setNull(1, Types.CHAR);
                ps.execute();
            } catch (SQLException sqle) {
               fail("SQLException is thrown: " + sqle.getMessage());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setNull(int, int, String)}
     * 
     * UDTs and Ref types not supported in SQLite v 3
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setNull",
        args = {int.class, int.class, java.lang.String.class}
    )
    public void testSetNullIntintString() {
        // test UDT
        String typeCreationStmtUDT = "CREATE TYPE addressType AS "
        +"( street INTEGER, zip TEXT);";
        String personTableCreateUDT = "CREATE TABLE person (name TEXT, address addressType);";
        Statement st = null;
        PreparedStatement ps = null;
        try {
            st = conn.createStatement();
            st.execute(typeCreationStmtUDT);
            st.execute(personTableCreateUDT);
            fail("UDTs and Ref Types not supported");
            String query = "insert into person (name, address) values ('Hans', ?);";
            ps = conn.prepareStatement(query);
            try {
                ps.setNull(1, Types.DATALINK);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
                sqle.printStackTrace();
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }
            
        } catch (SQLException e) {
         // UDTs or Ref types not supported
           // ok
        } finally {
            try {
                st.execute("drop table if exists person");
                ps.close();
            } catch (Exception ee) {
            }
        }
        
        // test non UDT REF type Exception checking
        String personTableCreate = "create table person (name TEXT, Address TEXT)";
            try {
                
                st = conn.createStatement();
                st.execute(personTableCreate);
                String insert = "insert into person (name, address) values (?, '1600 Amphitheatre Mountain View');";
                ps = conn.prepareStatement(insert);
                try {
                    ps.setNull(1,1, "");
                    ps.execute();
                } catch (SQLException sqle) {
                    assertEquals("SQLite.Exception: error in step",sqle.getMessage());
                } finally {
                    try {
                        st.close();
                    } catch (Exception ee) {
                    }
                }
                
            } catch (SQLException e) {
                fail("SQLException is thrown: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    st.execute("drop table if exists person");
                    ps.close();
                } catch (Exception ee) {
                }
            }
            
         // test non UDT REF type OK
            
            personTableCreate = "create table person (name TEXT, Address TEXT)";
                try {
                    
                    st = conn.createStatement();
                    st.execute("drop table if exists person");
                    st.execute(personTableCreate);
                    String insert = "insert into person (name, address) values (?, '1600 Amphitheatre Mountain View');";
                    ps = conn.prepareStatement(insert);
                    try {
                        ps.setNull(1,1, "");
                        ps.execute();
                    } catch (SQLException sqle) {
                        fail("SQLException is thrown: " + sqle.getMessage());
                        sqle.printStackTrace();
                    } finally {
                        try {
                            st.close();
                        } catch (Exception ee) {
                        }
                    }
                    
                } catch (SQLException e) {
                    fail("SQLException is thrown: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        st.execute("drop table if exists person");
                        ps.close();
                    } catch (Exception ee) {
                    }
                }
        
        
    }


    /**
     * @test java.sql.PreparedStatement#setObject(int parameterIndex, Object x)
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setObject",
        args = {int.class, java.lang.Object.class}
    )
    public void testSetObject_int_Object() {
        
        PreparedStatement ps = null;
        try {
            String query = "insert into type (IntVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setObject(1, Integer.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where IntVal="
                        + Integer.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (LongVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, "test text");
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where LongVal='test text';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

            query = "insert into type (DecVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, new Object());
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown");
            }

            query = "insert into type (dateVal) values (?);";
            ps = conn.prepareStatement(query);
            Date d = new Date(123456789);

            try {
                ps.setObject(1, d);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where dateVal='"
                        + d.getTime() + "';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            // this sub test doesn't pass on RI
            query = "insert into type (BlobVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, null);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (SQLException ee) {
                }
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
        try {
            ps.setObject(1, "test text");
            fail("Exception not thrown");
        } catch (SQLException e) {
            // ok
        }

    }

    /**
     * @test java.sql.PreparedStatement#setObject(int parameterIndex, Object x,
     *       int targetSqlType)
     * 
     * this test doesn't pass on RI
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setObject",
        args = {int.class, java.lang.Object.class, int.class}
    )
    public void testSetObject_int_Object_int() {
        
        PreparedStatement ps = null;
        try {
            String query = "insert into type (IntVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setObject(1, Integer.MAX_VALUE, Types.INTEGER);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where IntVal="
                        + Integer.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (LongVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, "test text", Types.CHAR);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where LongVal='test text';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (DecVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, new Object(), Types.DECIMAL);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.toString());
            }

            query = "insert into type (dateVal) values (?);";
            ps = conn.prepareStatement(query);
            Date d = new Date(123456789);
            

            try {
                ps.setObject(1, d, Types.DATE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where dateVal='"
                        + d.getTime() + "';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            // this sub test doesn't pass on RI
            query = "insert into type (BlobVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, "", Types.BLOB);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
        
        try {
            ps.setObject(1, Integer.MAX_VALUE, Types.INTEGER);
            fail("Exception not thrown");
        } catch (SQLException e) {
            // ok
        }
        
    }

    /**
     * @test java.sql.PreparedStatement#setObject(int parameterIndex, Object x,
     *       int targetSqlType, int scale)
     * 
     * this test doesn't pass on RI
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setObject",
        args = {int.class, java.lang.Object.class, int.class, int.class}
    )
    public void testSetObject_int_Object_int_int() {
        
        PreparedStatement ps = null;
        try {
            String query = "insert into type (IntVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            try {
                ps.setObject(1, Integer.MAX_VALUE, Types.INTEGER,
                        Integer.MAX_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where IntVal="
                        + Integer.MAX_VALUE);
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (LongVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, "test text", Types.CHAR, Integer.MIN_VALUE);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where LongVal='test text';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            query = "insert into type (DecVal) values (?);";
            ps = conn.prepareStatement(query);
            BigDecimal bd2 = new BigDecimal("12.21");

            try {
                ps.setObject(1, bd2, Types.DECIMAL, 2);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            }

            query = "insert into type (dateVal) values (?);";
            ps = conn.prepareStatement(query);
            Date d = new Date(123456789);
            try {
                ps.setObject(1, d , Types.DATE, -1);
                ps.execute();
                st = conn.createStatement();
                st.execute("select * from type where dateVal='"
                        + d.getTime() + "';");
                ResultSet rs = st.getResultSet();
                assertEquals(1, getCount(rs));
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

            // this sub test doesn't pass on RI
            query = "insert into type(BlobVal) values (?);";
            ps = conn.prepareStatement(query);

            try {
                ps.setObject(1, "", Types.BLOB, 0);
                ps.execute();
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.getMessage());
            } finally {
                try {
                    st.close();
                } catch (Exception ee) {
                }
            }

        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
            } catch (Exception ee) {
            }
        }
        
        try {
            ps.setObject(1, "test text", Types.CHAR, Integer.MIN_VALUE);
            fail("Exception not thrown");
        } catch (SQLException e) {
            // ok
        }
    }

    /**
     * @test java.sql.PreparedStatement#setTime(int parameterIndex, Time x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTime",
        args = {int.class, java.sql.Time.class}
    )
    @KnownFailure("statment.close() does not wrap up")
    public void testSetTimeint_Time() {

        Time[] times = { new Time(24, 25, 26), new Time(Integer.MAX_VALUE),
                new Time(123456789) };

        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (timeVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            for (int i = 0; i < times.length; i++) {
                try {
                    ps.setTime(1, times[i]);
                    ps.execute();
                    st = conn.createStatement();
                    st.execute("select * from type where timeVal='"
                            + times[i].getTime() + "'");
                    ResultSet rs = st.getResultSet();
                    assertEquals(1, getCount(rs));
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                } finally {
                    try {
                        st.close();
                    } catch (Exception ee) {
                    }
                }
            }

            try {
                ps.setTime(2, times[0]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected index out of bounds
            }
            
            ps.close();
            
            try {
                ps.setTime(1, times[0]);
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }
            String query1 = "insert into type (Tint) values (?)";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setTime(1, times[0]);
                ps1.execute();

            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.toString());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setTime(int parameterIndex, Time x,
     *       Calendar cal)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTime",
        args = {int.class, java.sql.Time.class, java.util.Calendar.class}
    )
    @KnownFailure("preparedStatement.execute() does not return False on update.")
    public void testSetTime_int_Time_Calendar() {

        Calendar[] cals = { Calendar.getInstance(),
                Calendar.getInstance(Locale.GERMANY),
                Calendar.getInstance(TimeZone.getDefault()) };

        Time[] times = { new Time(24, 25, 26), new Time(Integer.MAX_VALUE),
                new Time(123456789) };

        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (timeVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            for (int i = 0; i < times.length; i++) {
                try {
                    ps.setTime(1, times[i], cals[i]);
                    assertFalse(ps.execute());
                    assertTrue(ps.getUpdateCount() > 0);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                } finally {
                    try {
                        st.close();
                    } catch (Exception ee) {
                    }
                }
            }

            try {
                ps.setTime(2, times[0], cals[0]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            
            ps.close();

            try {
                ps.setTime(-2, times[0], cals[1]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setTime(1, times[0], cals[2]);
                ps1.execute();
                
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.toString());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }

    /**
     * @test java.sql.PreparedStatement#setTimestamp(int parameterIndex,
     *       Timestamp x)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTimestamp",
        args = {int.class, java.sql.Timestamp.class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testSetTimestamp_int_Timestamp() {

        Timestamp[] timestamps = { new Timestamp(2007, 10, 17, 19, 06, 50, 23),
                new Timestamp(123) };

        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (TS) values (?);";
            ps = conn.prepareStatement(query);
          
            for (int i = 0; i < timestamps.length; i++) {
                try {
                    ps.setTimestamp(1, timestamps[i]);
                    assertFalse(ps.execute());
                    assertTrue(ps.getUpdateCount() > 0);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                }
            }

            try {
                ps.setTimestamp(2, timestamps[0]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }

            try {
                ps.setTimestamp(-2, timestamps[0]);
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setTimestamp(1, timestamps[0]);
                ps1.execute();
                
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.toString());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setBlob(int, java.sql.Blob)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setBlob",
        args = {int.class, java.sql.Blob.class}
    )
    public void testSetBlob() {
        ResultSet res = null;
        PreparedStatement ps = null;
        Blob mock = new MockBlob();
        try {
            String neverExecutedQuery = "select TBlob from type;";
            ps = conn.prepareStatement(neverExecutedQuery);
            ps.setBlob(1,mock);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            //ok 
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setClob(int, java.sql.Clob)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setClob",
        args = {int.class, java.sql.Clob.class}
    )
    public void testSetClob() {
        ResultSet res = null;
        PreparedStatement ps = null;
        Clob mock = new MockClob();
        try {
            String neverExecutedQuery = "select TBlob from type;";
            ps = conn.prepareStatement(neverExecutedQuery);
            ps.setClob(1,mock);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            //ok
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setTimestamp(int, Timestamp, Calendar)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setTimestamp",
        args = {int.class, java.sql.Timestamp.class, java.util.Calendar.class}
    )
    @KnownFailure("preparedStatement.execute() does not return false on update.")
    public void testSetTimestampIntTimestampCalendar() {
        Calendar[] cals = { Calendar.getInstance(),
                Calendar.getInstance(Locale.GERMANY),
                Calendar.getInstance(TimeZone.getDefault()) };

        Timestamp[] timestamps = { new Timestamp(2007, 10, 17, 19, 06, 50, 23),
                new Timestamp(123) };

        
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            String query = "insert into type (timeVal) values (?);";
            ps = conn.prepareStatement(query);
            Statement st = null;
            for (int i = 0; i < timestamps.length; i++) {
                try {
                    ps.setTimestamp(1, timestamps[i], cals[i]);
                    assertFalse(ps.execute());
                    assertTrue(ps.getUpdateCount() > 0);
                } catch (SQLException sqle) {
                    fail("SQLException is thrown: " + sqle.getMessage());
                } finally {
                    try {
                        st.close();
                    } catch (Exception ee) {
                    }
                }
            }

            try {
                ps.setTimestamp(2, timestamps[0], cals[0]);
                ps.execute();
                fail("SQLException is not thrown");
            } catch (Exception sqle) {
                // expected
            }
            ps.close();
            try {
                ps.setTimestamp(1, timestamps[0], cals[1]);
                ps.execute();
                fail("SQLException is not thrown");
            } catch (SQLException sqle) {
                // expected
            }
            String query1 = "insert into type (Tint) values (?);";
            ps1 = conn.prepareStatement(query1);

            try {
                ps1.setTimestamp(1, timestamps[0], cals[2]);
                ps1.execute();
                
            } catch (SQLException sqle) {
                fail("SQLException is thrown: " + sqle.toString());
            }
        } catch (SQLException e) {
            fail("SQLException is thrown: " + e.getMessage());
        } finally {
            try {
                
                ps.close();
                ps1.close();
            } catch (Exception ee) {
            }
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setURL(int, java.net.URL)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setURL",
        args = {int.class, java.net.URL.class}
    )
    public void testSetURL() {
        ResultSet res = null;
        PreparedStatement ps = null;
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            ps.setURL(1, new URL("http://www.android.com"));
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
           //ok            
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setArray(int, java.sql.Array)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setArray",
        args = {int.class, java.sql.Array.class}
    )
    public void testSetArray() {
        ResultSet res = null;
        PreparedStatement ps = null;
        Array a = new MockArray();
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            ps.setArray(1, new MockArray());
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            //ok      
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setRef(int, java.sql.Ref)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setRef",
        args = {int.class, java.sql.Ref.class}
    )
    public void testSetRef() {
        ResultSet res = null;
        PreparedStatement ps = null;
        Ref mock = new MockRef();
        try {
            String neverExecutedQuery = "select TBlob from type;";
            ps = conn.prepareStatement(neverExecutedQuery);
            ps.setRef(1,mock);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            //ok
        }
        
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setUnicodeStream",
        args = {int.class, java.io.InputStream.class, int.class}
    )
    public void testSetUnicodestream() {
        ResultSet res = null;
        PreparedStatement ps = null;
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            InputStream file = Class.forName(this.getClass().getName())
            .getResourceAsStream("/blob.c");
            ps.setUnicodeStream(0, file, 100);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            //ok            
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setCharacterStream",
        args = {int.class, java.io.Reader.class, int.class}
    )
    public void testSetCharacterSteam() {
        ResultSet res = null;
        PreparedStatement ps = null;
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            InputStream file = Class.forName(this.getClass().getName())
            .getResourceAsStream("/blob.c");
            assertNotNull("Error in test setup: file not found",file);
            Reader reader = new InputStreamReader(file);
            ps.setCharacterStream(1, reader, 100);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            // ok     
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setAsciiStream(int, InputStream, int)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setAsciiStream",
        args = {int.class, java.io.InputStream.class, int.class}
    )
    public void testSetAxciiStream() {
        ResultSet res = null;
        PreparedStatement ps = null;
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            InputStream file = Class.forName(this.getClass().getName())
            .getResourceAsStream("/blob.c");
            ps.setAsciiStream(0, file, 100);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            // ok       
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * @test {@link java.sql.PreparedStatement#setBinaryStream(int, InputStream, int)}
     * 
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "setBinaryStream",
        args = {int.class, java.io.InputStream.class, int.class}
    )
    public void testSetBinaryStream() {
        
        ResultSet res = null;
        PreparedStatement ps = null;
        try {
            String query = "insert into type (TText) values (?);";
            ps = conn.prepareStatement(query);
            InputStream file = Class.forName(this.getClass().getName())
            .getResourceAsStream("/blob.c");
            ps.setBinaryStream(0, file, 100);
            fail("Exception expected not supported"); 
        } catch (SQLException e) {
            // ok            
        } catch (Exception e) {
            fail("Error in test setup "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    private class MockRef implements Ref {

        public String getBaseTypeName() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getObject() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getObject(Map<String, Class<?>> map) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public void setObject(Object value) throws SQLException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private class MockArray implements Array {

        public Object getArray() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(long index, int count) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map)
                throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public int getBaseType() throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getBaseTypeName() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(long index, int count)
                throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public ResultSet getResultSet(Map<String, Class<?>> map)
                throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    private class MockBlob implements Blob {

        public InputStream getBinaryStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public byte[] getBytes(long pos, int length) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public long length() throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(Blob pattern, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(byte[] pattern, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public OutputStream setBinaryStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public int setBytes(long pos, byte[] theBytes) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public int setBytes(long pos, byte[] theBytes, int offset, int len)
                throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public void truncate(long len) throws SQLException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private class MockClob implements Clob {

        public InputStream getAsciiStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Reader getCharacterStream() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getSubString(long pos, int length) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public long length() throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(Clob searchstr, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public long position(String searchstr, long start) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public OutputStream setAsciiStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public Writer setCharacterStream(long pos) throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        public int setString(long pos, String str) throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public int setString(long pos, String str, int offset, int len)
                throws SQLException {
            // TODO Auto-generated method stub
            return 0;
        }

        public void truncate(long len) throws SQLException {
            // TODO Auto-generated method stub
            
        }
        
    }
}
