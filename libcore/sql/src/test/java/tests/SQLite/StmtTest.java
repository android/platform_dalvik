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

import SQLite.Constants;
import SQLite.Database;
import SQLite.Exception;
import SQLite.Stmt;
import SQLite.TableResult;
import dalvik.annotation.AndroidOnly;
import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;


import tests.support.DatabaseCreator;
import tests.support.Support_SQL;

import java.sql.Connection;
import java.sql.SQLException;

@TestTargetClass(Stmt.class)
public class StmtTest extends SQLiteTest {
    
    private static Database db = null;
    
    private static Stmt st = null;
    
    private static final String createAllTypes = 
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
            + " MText MEDIUMTEXT, " + " LText LONGTEXT, " + 
            
            " MaxLongVal BIGINT, MinLongVal BIGINT, "+
            
            " validURL URL, invalidURL URL "+
            
            ");";
    
    static final String insertAllTypes = 
        "insert into type (BoolVal, IntVal, LongVal, Bint, Tint, Sint, Mint,"
        + "IntegerVal, RealVal, DoubleVal, FloatVal, DecVal,"
        + "NumVal, charStr, dateVal, timeVal, TS,"
        + "DT, TBlob, BlobVal, MBlob, LBlob,"
        + "TText, TextVal, MText, LText, MaxLongVal, MinLongVal,"
        + " validURL, invalidURL"
        + ") "
        + "values (1, -1, 22, 2, 33,"
        + "3, 1, 2, 3.9, 23.2, 33.3, 44,"
        + "5, 'test string', '1799-05-26', '12:35:45', '2007-10-09 14:28:02.0',"
        + "'1221-09-22 10:11:55', 1, 2, 3, 4,"
        + "'Test text message tiny', 'Test text',"
        + " 'Test text message medium', 'Test text message long', "
        + Long.MAX_VALUE+", "+Long.MIN_VALUE+", "
        + "null, null "+
        ");";
    
    static final String allTypesTable = "type";
    
    public void setUp() throws java.lang.Exception {
        super.setUp();
        Support_SQL.loadDriver();
        db = new Database();
        db.open(dbFile.getPath(), 0);
        db.exec(DatabaseCreator.CREATE_TABLE_SIMPLE1, null);
        DatabaseCreator.fillSimpleTable1(conn);
       
    }

    public void tearDown() {
        if (st != null) {
            try {
            st.close();
            } catch (Exception e) {
                
            }
        }
        try {
            db.close();
            Connection con = Support_SQL.getConnection();
            con.close();
//            dbFile.delete();
        } catch (Exception e) {
            fail("Exception in tearDown: "+e.getMessage());
        } catch (SQLException e) {
            fail("SQLException in tearDown: "+e.getMessage());
        }
        super.tearDown();
    }
    
    /**
     * @tests {@link Stmt#Stmt()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "constructor test",
        method = "Stmt",
        args = {}
    )
    public void testStmt() {
        Stmt st = new Stmt();
        assertNotNull(st);
        try {
            Stmt actual = db.prepare("");
            assertNotNull(st);
            // no black box test assertEquals(actual.error_code,st.error_code);
        } catch (Exception e) {
            fail("Statement setup fails: "+e.getMessage());
            e.printStackTrace();
        }
        
        try {
               st.step();
               fail("Cannot execute non prepared Stmt");
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @tests {@link Stmt#finalize()}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "method test",
        method = "finalize",
        args = {}
    )
    public void testFinalize() {
        
    }

    /**
     * @tests {@link Stmt#prepare()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "prepare",
        args = {}
    )
    public void testPrepare() {
        try {
            st = db.prepare("");
            st.prepare();
            fail("statement is closed");
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }

        try {
            st = new Stmt();
            st = db.prepare("select * from " + DatabaseCreator.SIMPLE_TABLE1);
            assertFalse(st.prepare());
            st = new Stmt();
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            assertFalse(st.prepare());
            st = new Stmt();
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            st.bind(1, 1);
            st.bind(2, 10);
            st.bind(3, 30);
            assertFalse(st.prepare());
            st = db.prepare("select * from " + DatabaseCreator.SIMPLE_TABLE1
                    + "; " + "delete from " + DatabaseCreator.SIMPLE_TABLE1
                    + " where id = 5; " + "insert into "
                    + DatabaseCreator.SIMPLE_TABLE1 + " values(5, 10, 20); "
                    + "select * from " + DatabaseCreator.SIMPLE_TABLE1 + ";");
            assertTrue(st.prepare());
            assertTrue(st.prepare());
            assertTrue(st.prepare());
            assertFalse(st.prepare());
        } catch (Exception e) {
            fail("statement should be ready for execution: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * @tests {@link Stmt#step()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "step",
        args = {}
    )
    public void testStep() {
        try {
            st.step();
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }
        
        try {
            st = new Stmt();
            st = db.prepare("select name from sqlite_master where type = 'table'");
            st.step();
        } catch (Exception e) {
           fail("test fails"); 
        }
        
    }
    
    /**
     * @tests {@link Stmt#close()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "close",
        args = {}
    )
    public void testClose() {
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            st.close();
        } catch (Exception e) {
            fail("Test fails");
            e.printStackTrace();
        }
        
        try {
            st.step();
            fail("Test fails");
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }
    }
    
    /**
     * @throws Exception 
     * @tests {@link Stmt#reset()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "reset",
        args = {}
    )
    @BrokenTest("Tableresult is not cleared when resetting statement: "+
            "Either complete spec or change implementation accordingly.")
    public void testReset() throws Exception {
        db.exec("create table TEST (res integer not null)", null);
        
        st = db.prepare("insert into TEST values (:one);");
        st.bind(1, 1);
        st.step();
        
        // verify that parameter is still bound
        st.reset();
        assertEquals(1,st.bind_parameter_count());
        st.step();
        
        TableResult count = db.get_table("select count(*) from TEST where res=1", null);
        
        String[] row0 = (String[]) count.rows.elementAt(0);
        assertEquals(2, Integer.parseInt(row0[0]));
        
        //Verify that rest (tableResult) is cleared
        st = db.prepare("select * from TEST;");
        st.step();
        assertEquals(1, st.column_count());
        st.reset();
        assertEquals(0,st.column_count());
    }
    
    /**
     * @tests {@link Stmt#clear_bindings()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "not supported",
        method = "clear_bindings",
        args = {}
    )
    public void testClear_bindings() {
        try {
            st.clear_bindings();
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
    }
    
    /**
     * @tests {@link Stmt#bind(int, int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind",
        args = {int.class, int.class}
    )
    public void testBindIntInt() {
        try {
            int input = 0;
            int maxVal = Integer.MAX_VALUE;
            int minVal = Integer.MIN_VALUE;
           
            db.exec("create table TEST (res integer)", null);
            st = db.prepare("insert into TEST values (:one);");
            st.bind(1, input);
            st.step();
            
            st.reset();
            st.bind(1,maxVal);
            st.step();
            
            st.reset();
            st.bind(1,minVal);
            st.step();
            
            TableResult r = db.get_table("select * from TEST");          
            
            String[] row0 = (String[]) r.rows.elementAt(0);
            assertEquals(input,Integer.parseInt(row0[0]));
            
            String[] row1 = (String[]) r.rows.elementAt(1);
            assertEquals(maxVal,Integer.parseInt(row1[0]));
            
            String[] row2 = (String[]) r.rows.elementAt(2);
            assertEquals(minVal,Integer.parseInt(row2[0]));

        } catch (Exception e) {
            fail("Error in test setup: "+e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind(1,Integer.MIN_VALUE);
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @tests {@link Stmt#bind(int, long)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind",
        args = {int.class, long.class}
    )
    public void testBindIntLong() {
        try {
            long input = 0;
            long maxVal = Long.MAX_VALUE;
            long minVal = Long.MIN_VALUE;
           
            db.exec("create table TEST (res long)", null);
            st = db.prepare("insert into TEST values (:one);");
            st.bind(1, input);
            st.step();
            
            st.reset();
            st.bind(1,maxVal);
            st.step();
            
            st.reset();
            st.bind(1,minVal);
            st.step();
          
            TableResult r = db.get_table("select * from TEST");
            
            String[] row0 = (String[]) r.rows.elementAt(0);
            assertEquals(input,Long.parseLong(row0[0]));
            
            String[] row1 = (String[]) r.rows.elementAt(1);
            assertEquals(maxVal,Long.parseLong(row1[0]));
            
            String[] row2 = (String[]) r.rows.elementAt(2);
            assertEquals(minVal,Long.parseLong(row2[0]));

        } catch (Exception e) {
            fail("Error in test setup: "+e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind(1,Long.MIN_VALUE);
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @tests {@link Stmt#bind(int, double)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind",
        args = {int.class, double.class}
    )
    public void testBindIntDouble() {
        try {
            double input = 0.0;
            double maxVal = Double.MAX_VALUE;
            double minVal = Double.MIN_VALUE;
            double negInf = Double.NEGATIVE_INFINITY;
            double posInf = Double.POSITIVE_INFINITY;
            double nan = Double.NaN;

            db.exec("create table TEST (res double)", null);
            st = db.prepare("insert into TEST values (:one);");
            st.bind(1, input);
            st.step();

            st.reset();
            st.bind(1, maxVal);
            st.step();

            st.reset();
            st.bind(1, minVal);
            st.step();

            st.reset();
            st.bind(1, negInf);
            st.step();

            st.reset();
            st.bind(1, posInf);
            st.step();

            st.reset();
            st.bind(1, nan);
            st.step();


            TableResult r = db.get_table("select * from TEST");

            String[] row0 = (String[]) r.rows.elementAt(0);
            assertTrue(Double.compare(input, Double.parseDouble(row0[0])) == 0);

            String[] row1 = (String[]) r.rows.elementAt(1);
            assertFalse(Double.compare(maxVal, Double.parseDouble(row1[0])) == 0);
            assertTrue(Double.compare(maxVal, Double.parseDouble(row1[0])) < 0);
            assertTrue(Double.isInfinite(Double.parseDouble(row1[0])));

            String[] row2 = (String[]) r.rows.elementAt(2);
            assertTrue(Double.compare(minVal, Double.parseDouble(row2[0])) == 0);

            String[] row3 = (String[]) r.rows.elementAt(3);
            assertEquals("Double.NEGATIVE_INFINITY SQLite representation",
                    "-Inf", row3[0]);

            String[] row4 = (String[]) r.rows.elementAt(4);
            assertEquals("Double.POSITIVE_INFINITY SQLite representation",
                    "Inf", row4[0]);

            String[] row5 = (String[]) r.rows.elementAt(4);
            assertEquals("Double.Nan SQLite representation", "Inf", row5[0]);

        } catch (Exception e) {
            fail("Error in test setup: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind(1,0.0);
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
    }

    /**
     * @tests {@link Stmt#bind(int, byte[])}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "bind",
        args = {int.class, byte[].class}
    )
    public void testBindIntByteArray() {
        
        String name = "Hello World";
        
        try {
            byte[] b = new byte[name.getBytes().length];
            b = name.getBytes();
            String stringInHex = "";
            
            db.exec(DatabaseCreator.CREATE_TABLE_PARENT, null);
            st = db.prepare("insert into " + DatabaseCreator.PARENT_TABLE
                    + " values (:one, :two);");
            st.bind(1, 2);
            st.bind(2, b);
            st.step();
            
            //compare what was stored with input based on Hex representation
            // since type of column is CHAR
            TableResult r = db.get_table("select * from "
                    + DatabaseCreator.PARENT_TABLE);          
            String[] row = (String[]) r.rows.elementAt(0);
            
            for (byte aByte : b) {
                stringInHex += Integer.toHexString(aByte);
            }
            stringInHex = "X'" + stringInHex + "'";
            assertTrue(stringInHex.equalsIgnoreCase(row[1]));
            
        } catch (Exception e) {
            fail("Error in test setup: "+e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind(1,name.getBytes());
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
    }

    /**
     * @tests {@link Stmt#bind(int, String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind",
        args = {int.class, java.lang.String.class}
    )
    public void testBindIntString() {
        String name = "Hello World";
        
        try {
           
            db.exec(DatabaseCreator.CREATE_TABLE_PARENT, null);
            st = db.prepare("insert into " + DatabaseCreator.PARENT_TABLE
                    + " values (:one, :two);");
            st.bind(1, 2);
            st.bind(2, name);
            st.step();
            
            TableResult r = db.get_table("select * from "
                    + DatabaseCreator.PARENT_TABLE);          
            String[] row = (String[]) r.rows.elementAt(0);
            assertEquals(name,row[1]);

        } catch (Exception e) {
            fail("Error in test setup: "+e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind(1,name);
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @tests {@link Stmt#bind(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind",
        args = {int.class}
    )
    public void testBindInt() {
        
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            st.bind(4);
            st.bind(1, 4);
            st.bind(2, 10);
            st.bind(3, 30);
            st.step();
            fail("Test failes");
        } catch (Exception e) {
            // What happens if null is bound to non existing variable position
            assertEquals("parameter position out of bounds" , e.getMessage());
        }
        
        // functional tests
        
        try {
            st.reset();
            st.bind(1);
            st.bind(2, 10);
            st.bind(3, 30);
            st.step();
            fail("Test failes");
        } catch (Exception e) {
            // What happens if null is bound to NON NULL field
            assertEquals("SQL logic error or missing database", e.getMessage());
        }

        try {
            st.reset();
            st.bind(1, 3);
            st.bind(2);
            st.bind(3, 30);
            st.step();
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
        }

    }
    
    /**
     * @tests {@link Stmt#bind_zeroblob(int, int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "bind_zeroblob",
        args = {int.class, int.class}
    )
    public void testBind_zeroblob() {
        try {
            st.bind_zeroblob(1, 128);
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
    }
    
    /**
     * @tests {@link Stmt#bind_parameter_count()}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind_parameter_count",
        args = {}
    )
    public void testBind_parameter_count() {
        try {
            st.bind_parameter_count();
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }
        
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            assertEquals(3, st.bind_parameter_count());
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (?, ?, ?)");
            assertEquals(3, st.bind_parameter_count());
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st = db.prepare("select * from " + DatabaseCreator.SIMPLE_TABLE1);
            assertEquals(0, st.bind_parameter_count());
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st.close();
            st.bind_parameter_count();
            fail("Exception expected");
        } catch (Exception e) {
            //ok
        }
        
    }

    /**
     * @tests {@link Stmt#bind_parameter_name(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind_parameter_name",
        args = {int.class}
    )
    public void testBind_parameter_name() {
        try {
            st.bind_parameter_name(1);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }
        
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            assertEquals(":one", st.bind_parameter_name(1));
            assertEquals(":two", st.bind_parameter_name(2));
            assertEquals(":three", st.bind_parameter_name(3));
            String name = st.bind_parameter_name(4);
        } catch (Exception e) {
            assertEquals("parameter position out of bounds",e.getMessage());
        }
    }

    /**
     * @tests {@link Stmt#bind_parameter_index(String)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "bind_parameter_index",
        args = {java.lang.String.class}
    )
    public void testBind_parameter_index() {

        try {
            st.bind_parameter_index("");
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("stmt already closed", e.getMessage());
        }

        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            assertEquals(3, st.bind_parameter_index(":three"));
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            assertEquals(0, st.bind_parameter_index(":t"));
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }

        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (?, ?, ?)");
            assertEquals(0, st.bind_parameter_index("?"));
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column_int(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column_int",
        args = {int.class}
    )
    public void testColumn_int() throws Exception {
        db.exec(createAllTypes, null);
        db.exec(insertAllTypes, null);
        
        int columnObjectCastFromLong;
        Object columnObject  = null;
        int intColumn = 0;
        String selectStmt = "select * from "+DatabaseCreator.SIMPLE_TABLE1;
        
        st = db.prepare(selectStmt);
        st.step();
        // select 'speed' value
        columnObject = st.column(1);
        intColumn = st.column_int(1);
        assertNotNull(intColumn);
        
        assertTrue("Integer".equalsIgnoreCase(st.column_decltype(1)));
        int stSpeed = Integer.parseInt(columnObject.toString());
        assertNotNull(stSpeed);
        assertEquals( intColumn, stSpeed);
        assertEquals(10,stSpeed);
        
        selectStmt = "select TextVal from "+allTypesTable;
        
        st = db.prepare(selectStmt);
        st.step();
        // select double value
        try {
            st.column_int(0);
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @tests {@link Stmt#column_long(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column_long",
        args = {int.class}
    )
    public void testColumn_long() {
        Object columnObject  = null;
        int columnObjectCastFromLong;
        long longColumn = 0;
        try {
            String selectStmt = "select * from "+DatabaseCreator.SIMPLE_TABLE1;
            st = db.prepare(selectStmt);
            st.step();
            columnObject = st.column(1);
            longColumn = st.column_long(1);
            assertNotNull(longColumn);
            // column declared as integer
            assertTrue("Integer".equalsIgnoreCase(st.column_decltype(1)));
            int stSpeed = Integer.parseInt(columnObject.toString());
            assertNotNull(stSpeed);
            assertEquals( longColumn, stSpeed);
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        } 
        
        try {
            st.column_long(4);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals( "column out of bounds" , e.getMessage());
        }
        
        try {
            st.column_long(-1);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals( "column out of bounds" , e.getMessage());
        }
    }
    
    /**
     * @throws Exception 
     * @tests {@link Stmt#column_double(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column_double",
        args = {int.class}
    )
    public void testColumn_double() throws Exception {
        db.exec(createAllTypes, null);
        db.exec(insertAllTypes, null);
       
        Object columnObject  = null;
        double doubleColumn = 0;
        double actualVal = 23.2;
        String selectStmt = "select DoubleVal from "+allTypesTable;
        
        st = db.prepare(selectStmt);
        st.step();
        // select double value
        doubleColumn = st.column_double(0);
        assertNotNull(doubleColumn);
        
        assertTrue("DOUBLE".equalsIgnoreCase(st.column_decltype(0)));
        assertNotNull(doubleColumn);
        assertEquals( actualVal, doubleColumn);
        
        // Exception test
        selectStmt = "select dateVal from "+allTypesTable;
        
        st = db.prepare(selectStmt);
        st.step();
        // select double value
        try {
        st.column_double(0);
        } catch (Exception e) {
            //ok
        }
        
        
    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column_bytes(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "column_bytes",
        args = {int.class}
    )
    public void testColumn_bytes() throws Exception {
        
        db.exec("create table B(id integer primary key, val blob)",null);
        db.exec("insert into B values(1, zeroblob(128))", null);
        st = db.prepare("select val from B where id = 1");
        assertTrue(st.step());
        try {
            st.column_bytes(0);
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column_string(int)}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column_string",
        args = {int.class}
    )
    public void testColumn_string() throws Exception {
        db.exec(createAllTypes, null);
        db.exec(insertAllTypes, null);
       
        Object columnObject  = null;
        String stringColumn = "";
        String actualVal = "test string";
        String selectStmt = "select charStr from "+allTypesTable;
        
        st = db.prepare(selectStmt);
        st.step();
        // select string value
        stringColumn = st.column_string(0);
        assertNotNull(stringColumn);
        
        assertTrue("CHAR(20)".equalsIgnoreCase(st.column_decltype(0)));
        assertNotNull(stringColumn);
        assertEquals( actualVal, stringColumn);
        
        // Exception test
        selectStmt = "select DoubleVal from "+allTypesTable;
        
        st = db.prepare(selectStmt);
        st.step();
        // select double value
        try {
        st.column_string(0);
        } catch (Exception e) {
            //ok
        }
    }
    
    /**
     * @throws Exception 
     * @tests {@link Stmt#column_type(int)}
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "method test",
        method = "column_type",
        args = {int.class}
    )
    @AndroidOnly("For numeric, float and blob wrong type is returned")
    public void testColumn_type() throws Exception {
        db.exec(createAllTypes, null);
        db.exec(insertAllTypes, null);
        st = db.prepare("select * from " + allTypesTable);
        st.step();

        // Exception test
        try {
            st.column_type(100);
        } catch (Exception e) {
            // ok
        }
        
        /*
        Dictionary
        
        public static final int SQLITE_INTEGER = 1;
        public static final int SQLITE_FLOAT = 2;
        public static final int SQLITE_BLOB = 4;
        public static final int SQLITE_NULL = 5;
        public static final int SQLITE3_TEXT = 3;
        public static final int SQLITE_NUMERIC = -1;
        */

        assertEquals(Constants.SQLITE3_TEXT, st.column_type(23)); // ok TEXT
        assertEquals(Constants.SQLITE3_TEXT, st.column_type(13)); // CHAR(20)

        assertEquals(Constants.SQLITE_FLOAT, st.column_type(8));
        assertEquals(Constants.SQLITE_FLOAT, st.column_type(9));
        assertEquals(Constants.SQLITE_FLOAT, st.column_type(10)); // FLOAT

        for (int i = 0; i < 8; i++) {
            assertEquals("Expected Integer at position " + i,
                    Constants.SQLITE_INTEGER, st.column_type(i));
        }

        assertEquals(Constants.SQLITE_NULL, st.column_type(28));
        assertEquals(Constants.SQLITE_NULL, st.column_type(29));

        // Failing tests
        assertTrue("NUMERIC".equalsIgnoreCase(st.column_decltype(12)));
        assertEquals(Constants.SQLITE_NUMERIC, st.column_type(12)); // NUMERIC
                                                                    // -> got
                                                                    // INTEGER
        
        assertTrue("FLOAT".equalsIgnoreCase(st.column_decltype(11)));
        assertEquals(Constants.SQLITE_FLOAT, st.column_type(11)); // FLOAT ->
                                                                  // got INTEGER
        assertTrue("BLOB".equalsIgnoreCase(st.column_decltype(19)));
        assertEquals(Constants.SQLITE_BLOB, st.column_type(19)); // Blob got
                                                                 // INTEGER

    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column_count() )}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column_count",
        args = {}
    )
    @AndroidOnly("Wrong value is returned in case of a prepared statment to "+
            "which a '*' bound ")
    public void testColumn_count() throws Exception {
        
        String selectStmt = "select * from "+DatabaseCreator.SIMPLE_TABLE1;
        st = db.prepare(selectStmt);
        
        assertEquals(3, st.column_count());
        
        st.step();
        int columnCount = st.column_count();
        assertNotNull(columnCount);
        assertEquals( 3, columnCount);
        
        // actual prepared statement
        selectStmt = "select ? from "+DatabaseCreator.SIMPLE_TABLE1;
        st = db.prepare(selectStmt);
        
        assertEquals(3, st.column_count());
        
        st.bind(1, "*");
        st.step();
        columnCount = st.column_count();
        assertNotNull(columnCount);
        assertEquals( 3, columnCount);
      
    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column(int) )}
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "method test",
        method = "column",
        args = {int.class}
    )
    public void testColumn() throws Exception {
        Object columnObject  = null;
        int columnObjectCastFromLong;
        int intColumn = 0;
        try {
            String selectStmt = "select * from "+DatabaseCreator.SIMPLE_TABLE1;
            TableResult res = db.get_table(selectStmt);
            st = db.prepare(selectStmt);
            st.step();
            columnObject = st.column(1);
            intColumn = st.column_int(1);
            assertNotNull(intColumn);
            assertTrue("Integer".equalsIgnoreCase(st.column_decltype(1)));
            int stSpeed = Integer.parseInt(columnObject.toString());
            assertNotNull(stSpeed);
            assertEquals( intColumn, stSpeed);
        } catch (Exception e) {
            fail("Error in test setup : " + e.getMessage());
            e.printStackTrace();
        } 
        
        try {
            assertNotNull(columnObject);
            int dummy = ((Integer) columnObject).intValue();
            fail("Cast to Integer should fail");
        } catch (ClassCastException e) {
            assertEquals("java.lang.Long", e.getMessage());
        }
        
        try {
            st.column(4);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals( "column out of bounds" , e.getMessage());
        }
        
        try {
            st.column(-1);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals( "column out of bounds" , e.getMessage());
        }
    }

    /**
     * @tests {@link Stmt#column_table_name(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "column_table_name",
        args = {int.class}
    )
    public void testColumn_table_name() {
        try {
            st = db.prepare("select * from " + DatabaseCreator.SIMPLE_TABLE1);
            String name = st.column_table_name(1);
           fail("Function is now supported.");
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
    }

    /**
     * @tests {@link Stmt#column_database_name(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "column_database_name",
        args = {int.class}
    )
    public void testColumn_database_name() {
        try {
            st = db.prepare("insert into " + DatabaseCreator.SIMPLE_TABLE1
                    + " values (:one,:two,:three)");
            String name = st.column_database_name(1);
           fail("Function is now supported.");
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
        
    }

    /**
     * @throws Exception 
     * @tests {@link Stmt#column_decltype(int)}
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "method test",
        method = "column_decltype",
        args = {int.class}
    )
    public void testColumn_decltype() throws Exception {
        db.exec(createAllTypes, null);
        db.exec(insertAllTypes, null);
        st = db.prepare("select * from " + allTypesTable);
        st.step();

        // Exception test
        try {
            st.column_decltype(100);
        } catch (Exception e) {
            // ok
        }

        assertTrue(st.column_decltype(0), "BOOLEAN".equalsIgnoreCase(st
                .column_decltype(0)));
        assertTrue(st.column_decltype(1), "INT".equalsIgnoreCase(st
                .column_decltype(1)));
        assertTrue(st.column_decltype(2), "LONG".equalsIgnoreCase(st
                .column_decltype(2)));
        assertTrue(st.column_decltype(3), "BIGINT".equalsIgnoreCase(st
                .column_decltype(3)));
        assertTrue(st.column_decltype(4), "TINYINT".equalsIgnoreCase(st
                .column_decltype(4)));
        assertTrue(st.column_decltype(5), "SMALLINT".equalsIgnoreCase(st
                .column_decltype(5)));
        assertTrue(st.column_decltype(6), "MEDIUMINT".equalsIgnoreCase(st
                .column_decltype(6)));
        assertTrue(st.column_decltype(7), "INTEGER".equalsIgnoreCase(st
                .column_decltype(7)));
        assertTrue(st.column_decltype(8), "REAL".equalsIgnoreCase(st
                .column_decltype(8)));
        assertTrue(st.column_decltype(9), "DOUBLE".equalsIgnoreCase(st
                .column_decltype(9)));
        assertTrue(st.column_decltype(10), "FLOAT".equalsIgnoreCase(st
                .column_decltype(10)));
        assertTrue(st.column_decltype(11), "DECIMAL".equalsIgnoreCase(st
                .column_decltype(11)));
        assertTrue(st.column_decltype(12), "NUMERIC".equalsIgnoreCase(st
                .column_decltype(12)));
        assertTrue(st.column_decltype(13), "CHAR(20)".equalsIgnoreCase(st
                .column_decltype(13)));

        assertTrue(st.column_decltype(19), "BLOB".equalsIgnoreCase(st
                .column_decltype(19)));

        assertTrue(st.column_decltype(23), "TEXT".equalsIgnoreCase(st
                .column_decltype(23)));
        assertTrue(st.column_decltype(28), "URL".equalsIgnoreCase(st
                .column_decltype(28)));
        assertTrue(st.column_decltype(29), "URL".equalsIgnoreCase(st
                .column_decltype(29)));
    }
 
    /**
     * @tests {@link Stmt#column_origin_name(int)}
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        notes = "not supported",
        method = "column_origin_name",
        args = {int.class}
    )
    public void testColumn_origin_name() {
        try {
            st = db.prepare("select * from " + DatabaseCreator.SIMPLE_TABLE1);
            String name = st.column_origin_name(1);
           fail("Function is now supported.");
        } catch (Exception e) {
            assertEquals("unsupported", e.getMessage());
        }
    }
}
