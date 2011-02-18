/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.tests.java.lang;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(StrictMath.class) 
public class StrictMathTest extends junit.framework.TestCase {

    double HYP = StrictMath.sqrt(2.0);

    double OPP = 1.0;

    double ADJ = 1.0;

    /* Required to make previous preprocessor flags work - do not remove */
    int unused = 0;

    /**
     * @tests java.lang.StrictMath#pow(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "pow",
        args = {double.class, double.class}
    )
    public void test_pow() {
        // tests changes in fdlibm5.3
        assertTrue(Double.longBitsToDouble(-4610068591539890326L) == 
            StrictMath.pow(-1.0000000000000002e+00,4.5035996273704970e+15));
        assertTrue(Double.longBitsToDouble( 4601023824101950163L) == 
            StrictMath.pow(-9.9999999999999978e-01,4.035996273704970e+15));
        
        assertEquals("Incorrect value was returned.", 1.0, 
                StrictMath.pow(Double.MAX_VALUE, 0.0));
        assertEquals("Incorrect value was returned.", 1.0, 
                StrictMath.pow(Double.MAX_VALUE, -0.0));
        assertEquals("Incorrect value was returned.", Double.NaN, 
                StrictMath.pow(Double.MAX_VALUE, Double.NaN)); 
        assertEquals("Incorrect value was returned.", Double.NaN, 
                StrictMath.pow(Double.NaN, 1.0));
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY, 
                StrictMath.pow(1.1, Double.POSITIVE_INFINITY));    
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY, 
                StrictMath.pow(0.9, Double.NEGATIVE_INFINITY));   
        
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(1.1, Double.NEGATIVE_INFINITY));   
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(0.9, Double.POSITIVE_INFINITY));    
        
        assertEquals("Incorrect value was returned.", Double.NaN, 
                StrictMath.pow(-1.0, Double.POSITIVE_INFINITY));   
        assertEquals("Incorrect value was returned.", Double.NaN, 
                StrictMath.pow(1.0, Double.NEGATIVE_INFINITY));
        
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(0.0, 1.1));   
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(Double.POSITIVE_INFINITY, -1.0));   
        
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(-0.0, 1.1));   
        assertEquals("Incorrect value was returned.", 0.0, 
                StrictMath.pow(Double.POSITIVE_INFINITY, -1.0)); 
        
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY,
                StrictMath.pow(0.0, -1.0));
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY,
                StrictMath.pow(Double.POSITIVE_INFINITY, 1.0));        
        
        assertEquals("Incorrect value was returned.", 0.0,
                StrictMath.pow(-0.0, 2.0));
        assertEquals("Incorrect value was returned.", 0.0,
                StrictMath.pow(Double.NEGATIVE_INFINITY, -2.0));  
        
        assertEquals("Incorrect value was returned.", -0.0,
                StrictMath.pow(-0.0, 1.0));
        assertEquals("Incorrect value was returned.", -0.0,
                StrictMath.pow(Double.NEGATIVE_INFINITY, -1.0));  
        
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY,
                StrictMath.pow(-0.0, -2.0));
        assertEquals("Incorrect value was returned.", Double.POSITIVE_INFINITY,
                StrictMath.pow(Double.NEGATIVE_INFINITY, 2.0)); 
        
        assertEquals("Incorrect value was returned.", Double.NEGATIVE_INFINITY,
                StrictMath.pow(-0.0, -1.0));
        assertEquals("Incorrect value was returned.", Double.NEGATIVE_INFINITY,
                StrictMath.pow(Double.NEGATIVE_INFINITY, 1.0));   
        
        assertEquals("Incorrect value was returned.", -0.999,
                StrictMath.pow(-0.999, 1.0));   
        
        assertEquals("Incorrect value was returned.", Double.NaN,
                StrictMath.pow(-0.999, 1.1));
    }

    /**
     * @tests java.lang.StrictMath#tan(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "tan",
        args = {double.class}
    )
    public void test_tan(){
        // tests changes in fdlibm5.3
        assertTrue(Double.longBitsToDouble( 4850236541654588678L) == StrictMath.tan( 1.7765241907548024E+269));
        assertEquals("Incorrect value of tan was returned.",
                Double.NaN, StrictMath.tan(Double.NaN));
        assertEquals("Incorrect value of tan was returned.",
                Double.NaN, StrictMath.tan(Double.POSITIVE_INFINITY));     
        assertEquals("Incorrect value of tan was returned.",
                Double.NaN, StrictMath.tan(Double.NEGATIVE_INFINITY));
        assertEquals("Incorrect value of tan was returned.",
                0.0, StrictMath.tan(0.0));    
        assertEquals("Incorrect value of tan was returned.",
                -0.0, StrictMath.tan(-0.0));            
    }

    /**
     * @tests java.lang.StrictMath#asin(double)
     * @tests java.lang.StrictMath#exp(double)
     * @tests java.lang.StrictMath#sinh(double)
     * @tests java.lang.StrictMath#expm1(double)
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Checks one value.",
            method = "asin",
            args = {double.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Checks one value.",
            method = "exp",
            args = {double.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Checks one value.",
            method = "sinh",
            args = {double.class}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Checks one value.",
            method = "expm1",
            args = {double.class}
        )
    })
    public void test_inexact(){
        assertTrue( 4485585228743840298L == Double.doubleToRawLongBits(StrictMath.asin(7.4505805E-9)));
        assertTrue( 4607182418816794624L == Double.doubleToRawLongBits(StrictMath.exp(3.7252902E-9)));
        assertTrue( 4481081628995577220L == Double.doubleToRawLongBits(StrictMath.sinh(3.7252902E-9)));
        assertTrue(-4616189618054758400L == Double.doubleToRawLongBits(StrictMath.expm1(-40)));
    }
    
    /**
     * @tests java.lang.StrictMath#abs(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "abs",
        args = {double.class}
    )
    public void test_absD() {
        // Test for method double java.lang.StrictMath.abs(double)

        assertTrue("Incorrect double abs value",
                (StrictMath.abs(-1908.8976) == 1908.8976));
        assertTrue("Incorrect double abs value",
                (StrictMath.abs(1908.8976) == 1908.8976));
        
        assertEquals(0.0, StrictMath.abs(0.0));
        assertEquals(0.0, StrictMath.abs(-0.0));
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.abs(Double.POSITIVE_INFINITY));    
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.abs(Double.NEGATIVE_INFINITY));      
        assertEquals(Double.NaN, StrictMath.abs(Double.NaN));      
    }

    /**
     * @tests java.lang.StrictMath#abs(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "abs",
        args = {float.class}
    )
    public void test_absF() {
        // Test for method float java.lang.StrictMath.abs(float)
        assertTrue("Incorrect float abs value",
                (StrictMath.abs(-1908.8976f) == 1908.8976f));
        assertTrue("Incorrect float abs value",
                (StrictMath.abs(1908.8976f) == 1908.8976f));
        
        assertEquals(0f, StrictMath.abs(0f));
        assertEquals(0f, StrictMath.abs(-0f));
        assertEquals(Float.POSITIVE_INFINITY, StrictMath.abs(Float.POSITIVE_INFINITY));    
        assertEquals(Float.POSITIVE_INFINITY, StrictMath.abs(Float.NEGATIVE_INFINITY));      
        assertEquals(Float.NaN, StrictMath.abs(Float.NaN));        
    }

    /**
     * @tests java.lang.StrictMath#abs(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "abs",
        args = {int.class}
    )
    public void test_absI() {
        // Test for method int java.lang.StrictMath.abs(int)
        assertTrue("Incorrect int abs value",
                (StrictMath.abs(-1908897) == 1908897));
        assertTrue("Incorrect int abs value",
                (StrictMath.abs(1908897) == 1908897));
        
        assertEquals(Integer.MIN_VALUE, StrictMath.abs(Integer.MIN_VALUE));
    }

    /**
     * @tests java.lang.StrictMath#abs(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "abs",
        args = {long.class}
    )
    public void test_absJ() {
        // Test for method long java.lang.StrictMath.abs(long)
        assertTrue("Incorrect long abs value", (StrictMath
                .abs(-19088976000089L) == 19088976000089L));
        assertTrue("Incorrect long abs value",
                (StrictMath.abs(19088976000089L) == 19088976000089L));
        
        assertEquals(Long.MIN_VALUE, StrictMath.abs(Long.MIN_VALUE));        
    }

    /**
     * @tests java.lang.StrictMath#acos(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "acos",
        args = {double.class}
    )
    public void test_acosD() {
        // Test for method double java.lang.StrictMath.acos(double)
        assertTrue("Returned incorrect arc cosine", StrictMath.cos(StrictMath
                .acos(ADJ / HYP)) == ADJ / HYP);
        
        assertEquals(Double.NaN, StrictMath.acos(Double.NaN));
        assertEquals(Double.NaN, StrictMath.acos(1.1));        
    }

    /**
     * @tests java.lang.StrictMath#asin(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "asin",
        args = {double.class}
    )
    public void test_asinD() {
        // Test for method double java.lang.StrictMath.asin(double)
        assertTrue("Returned incorrect arc sine", StrictMath.sin(StrictMath
                .asin(OPP / HYP)) == OPP / HYP);
        
        assertEquals(Double.NaN, StrictMath.asin(Double.NaN));
        assertEquals(Double.NaN, StrictMath.asin(1.1));        
        assertEquals(0.0, StrictMath.asin(0.0));  
        assertEquals(-0.0, StrictMath.asin(-0.0));          
    }

    /**
     * @tests java.lang.StrictMath#atan(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Doesn't check boundary values.",
        method = "atan",
        args = {double.class}
    )
    public void test_atanD() {
        // Test for method double java.lang.StrictMath.atan(double)
        double answer = StrictMath.tan(StrictMath.atan(1.0));
        assertTrue("Returned incorrect arc tangent: " + answer, answer <= 1.0
                && answer >= 9.9999999999999983E-1);
    }

    /**
     * @tests java.lang.StrictMath#atan2(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "atan2",
        args = {double.class, double.class}
    )
    public void test_atan2DD() {
        // Test for method double java.lang.StrictMath.atan2(double, double)
        double answer = StrictMath.atan(StrictMath.tan(1.0));
        assertTrue("Returned incorrect arc tangent: " + answer, answer <= 1.0
                && answer >= 9.9999999999999983E-1);
        
        assertEquals(Double.NaN, StrictMath.atan2(Double.NaN, 1.0));
        assertEquals(Double.NaN, StrictMath.atan2(Double.NaN, 1.0));
        
        assertEquals(0.0, StrictMath.atan2(0.0, 1.0));
        assertEquals(0.0, StrictMath.atan2(1.0, Double.POSITIVE_INFINITY));   
        
        assertEquals(-0.0, StrictMath.atan2(-0.0, 1.0));      
        assertEquals(-0.0, StrictMath.atan2(-1.0, Double.POSITIVE_INFINITY)); 
        
        assertEquals(StrictMath.PI, StrictMath.atan2(0.0, -1.0));      
        assertEquals(StrictMath.PI, StrictMath.atan2(1.0, 
                                                     Double.NEGATIVE_INFINITY));     
        
        assertEquals(-StrictMath.PI, StrictMath.atan2(-0.0, -1.0));   
        assertEquals(-StrictMath.PI, StrictMath.atan2(-1.0, 
                                                     Double.NEGATIVE_INFINITY));     
        
        assertEquals(StrictMath.PI/2, StrictMath.atan2(1.0, 0.0));   
        assertEquals(StrictMath.PI/2, StrictMath.atan2(1.0, -0.0));        
        assertEquals(StrictMath.PI/2, StrictMath.atan2(Double.POSITIVE_INFINITY, 0.0)); 
        
        assertEquals(-StrictMath.PI/2, StrictMath.atan2(-1.0, 0.0));   
        assertEquals(-StrictMath.PI/2, StrictMath.atan2(-1.0, -0.0));        
        assertEquals(-StrictMath.PI/2, StrictMath.atan2(Double.NEGATIVE_INFINITY, 1.0));  
        
        assertEquals(StrictMath.PI/4, StrictMath.atan2(Double.POSITIVE_INFINITY, 
                                                     Double.POSITIVE_INFINITY)); 
        assertEquals(3*StrictMath.PI/4, 
                                      StrictMath.atan2(Double.POSITIVE_INFINITY, 
                                                     Double.NEGATIVE_INFINITY));     
        
        assertEquals(-StrictMath.PI/4, 
                StrictMath.atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));  
        
        assertEquals(-3*StrictMath.PI/4, 
                                      StrictMath.atan2(Double.NEGATIVE_INFINITY, 
                                                     Double.NEGATIVE_INFINITY));        
    }
    
    /**
     * @tests java.lang.StrictMath#cbrt(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "cbrt",
        args = {double.class}
    )
    @SuppressWarnings("boxing")
    public void test_cbrt_D() {
        // Test for special situations
        assertTrue("Should return Double.NaN", Double.isNaN(StrictMath
                .cbrt(Double.NaN)));
        assertEquals("Should return Double.POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .cbrt(Double.POSITIVE_INFINITY));
        assertEquals("Should return Double.NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath
                        .cbrt(Double.NEGATIVE_INFINITY));
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.cbrt(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.cbrt(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.cbrt(-0.0)));

        assertEquals("Should return 3.0", 3.0, StrictMath.cbrt(27.0));
        assertEquals("Should return 23.111993172558684", 23.111993172558684,
                StrictMath.cbrt(12345.6));
        assertEquals("Should return 5.643803094122362E102",
                5.643803094122362E102, StrictMath.cbrt(Double.MAX_VALUE));
        assertEquals("Should return 0.01", 0.01, StrictMath.cbrt(0.000001));

        assertEquals("Should return -3.0", -3.0, StrictMath.cbrt(-27.0));
        assertEquals("Should return -23.111993172558684", -23.111993172558684,
                StrictMath.cbrt(-12345.6));
        assertEquals("Should return 1.7031839360032603E-108",
                1.7031839360032603E-108, StrictMath.cbrt(Double.MIN_VALUE));
        assertEquals("Should return -0.01", -0.01, StrictMath.cbrt(-0.000001));
        
        try{
            StrictMath.cbrt((Double)null);
            fail("Should throw NullPointerException");
        }catch(NullPointerException e){
            //expected
        }
    }

    /**
     * @tests java.lang.StrictMath#ceil(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ceil",
        args = {double.class}
    )
    public void test_ceilD() {
        // Test for method double java.lang.StrictMath.ceil(double)
                assertEquals("Incorrect ceiling for double",
                             79, StrictMath.ceil(78.89), 0.0);
        assertEquals("Incorrect ceiling for double",
                             -78, StrictMath.ceil(-78.89), 0.0);
        
        assertEquals("Incorrect ceiling for mathematical integer",
                             -78, StrictMath.ceil(-78), 0.0);  
        assertEquals("Incorrect ceiling for NaN",
                                       Double.NaN, StrictMath.ceil(Double.NaN));
        assertEquals("Incorrect ceiling for positive infinity", 
                Double.POSITIVE_INFINITY, 
                StrictMath.ceil(Double.POSITIVE_INFINITY));        
        assertEquals("Incorrect ceiling for negative infinity", 
                Double.NEGATIVE_INFINITY, 
                StrictMath.ceil(Double.NEGATIVE_INFINITY));  
        assertEquals("Incorrect ceiling for positive zero.", 
                0.0, StrictMath.ceil(0.0));
        assertEquals("Incorrect ceiling for negative zero.", 
                -0.0, StrictMath.ceil(-0.0)); 
        assertEquals("Incorrect ceiling for negative zero.", 
                -0.0, StrictMath.ceil(-0.5));         
    }

    /**
     * @tests java.lang.StrictMath#cos(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "cos",
        args = {double.class}
    )
    public void test_cosD() {
        // Test for method double java.lang.StrictMath.cos(double)

        assertTrue("Returned incorrect cosine", StrictMath.cos(StrictMath
                .acos(ADJ / HYP)) == ADJ / HYP);
        assertEquals("Returned incorrect cosine", StrictMath.cos(Double.NaN), 
                                                                 Double.NaN);        
    }
    
    /**
     * @tests java.lang.StrictMath#cosh(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "cosh",
        args = {double.class}
    )
    @SuppressWarnings("boxing")
    public void test_cosh_D() {
        // Test for special situations        
        assertTrue("Should return NaN", Double.isNaN(StrictMath
                .cosh(Double.NaN)));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .cosh(Double.POSITIVE_INFINITY));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .cosh(Double.NEGATIVE_INFINITY));
        assertEquals("Should return 1.0", 1.0, StrictMath.cosh(+0.0));
        assertEquals("Should return 1.0", 1.0, StrictMath.cosh(-0.0));

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.cosh(1234.56));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.cosh(-1234.56));
        assertEquals("Should return 1.0000000000005", 1.0000000000005,
                StrictMath.cosh(0.000001));
        assertEquals("Should return 1.0000000000005", 1.0000000000005,
                StrictMath.cosh(-0.000001));
        assertEquals("Should return 5.212214351945598", 5.212214351945598,
                StrictMath.cosh(2.33482));

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.cosh(Double.MAX_VALUE));
        assertEquals("Should return 1.0", 1.0, StrictMath
                .cosh(Double.MIN_VALUE));
    }

    /**
     * @tests java.lang.StrictMath#exp(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "exp",
        args = {double.class}
    )
    public void test_expD() {
        // Test for method double java.lang.StrictMath.exp(double)
        assertTrue("Incorrect answer returned for simple power", StrictMath
                .abs(StrictMath.exp(4D) - StrictMath.E * StrictMath.E
                        * StrictMath.E * StrictMath.E) < 0.1D);
        assertTrue("Incorrect answer returned for larger power", StrictMath
                .log(StrictMath.abs(StrictMath.exp(5.5D)) - 5.5D) < 10.0D);
        assertEquals("Returned incorrect value for NaN argument", Double.NaN, 
                                                    StrictMath.exp(Double.NaN));
        assertEquals("Returned incorrect value for positive infinity.", 
            Double.POSITIVE_INFINITY, StrictMath.exp(Double.POSITIVE_INFINITY));    
        assertEquals("Returned incorrect value for negative infinity.", 
                                 0.0, StrictMath.exp(Double.NEGATIVE_INFINITY));  
    }
    
    /**
     * @tests java.lang.StrictMath#expm1(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "expm1",
        args = {double.class}
    )
    @SuppressWarnings("boxing")
    public void test_expm1_D() {
        //Test for special cases        
        assertTrue("Should return NaN", Double.isNaN(StrictMath.expm1(Double.NaN)));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.expm1(Double.POSITIVE_INFINITY));
        assertEquals("Should return -1.0", -1.0, StrictMath
                .expm1(Double.NEGATIVE_INFINITY));
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.expm1(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.expm1(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.expm1(-0.0)));

        assertEquals("Should return -9.999950000166666E-6",
                -9.999950000166666E-6, StrictMath.expm1(-0.00001));
        assertEquals("Should return 1.0145103074469635E60",
                1.0145103074469635E60, StrictMath.expm1(138.16951162));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .expm1(123456789123456789123456789.4521584223));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.expm1(Double.MAX_VALUE));
        assertEquals("Should return MIN_VALUE", Double.MIN_VALUE, StrictMath
                .expm1(Double.MIN_VALUE));
       
    }    

    /**
     * @tests java.lang.StrictMath#floor(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "floor",
        args = {double.class}
    )
    public void test_floorD() {
        // Test for method double java.lang.StrictMath.floor(double)
                assertEquals("Incorrect floor for double",
                             78, StrictMath.floor(78.89), 0.0);
        assertEquals("Incorrect floor for double",
                             -79, StrictMath.floor(-78.89), 0.0);
        assertEquals("Incorrect floor for mathematical integer",
                             -79, StrictMath.floor(-79), 0.0);
        assertEquals("Incorrect floor for NaN",
                        Double.NaN, StrictMath.floor(Double.NaN));    
        assertEquals("Incorrect floor for positive infinity.",
          Double.POSITIVE_INFINITY, StrictMath.floor(Double.POSITIVE_INFINITY));    
        assertEquals("Incorrect floor for negative infinity.",
          Double.NEGATIVE_INFINITY, StrictMath.floor(Double.NEGATIVE_INFINITY));  
        assertEquals("Incorrect floor for positive zero.",
                                                    0.0, StrictMath.floor(0.0)); 
        assertEquals("Incorrect floor for negative zero.",
                                                  -0.0, StrictMath.floor(-0.0));        
    }
    
    /**
     * @tests java.lang.StrictMath#hypot(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hypot",
        args = {double.class, double.class}
    )
    @SuppressWarnings("boxing")
    public void test_hypot_DD() {
        // Test for special cases
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(Double.POSITIVE_INFINITY,
                        1.0));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(Double.NEGATIVE_INFINITY,
                        123.324));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(-758.2587,
                        Double.POSITIVE_INFINITY));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(5687.21,
                        Double.NEGATIVE_INFINITY));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(Double.POSITIVE_INFINITY,
                        Double.NEGATIVE_INFINITY));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.hypot(Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY));
        assertTrue("Should return NaN",Double.isNaN(StrictMath.hypot(Double.NaN,
                2342301.89843)));
        assertTrue("Should return NaN",Double.isNaN(StrictMath.hypot(-345.2680,
                Double.NaN)));

        assertEquals("Should return 2396424.905416697", 2396424.905416697, StrictMath
                .hypot(12322.12, -2396393.2258));
        assertEquals("Should return 138.16958070558556", 138.16958070558556,
                StrictMath.hypot(-138.16951162, 0.13817035864));
        assertEquals("Should return 1.7976931348623157E308",
                1.7976931348623157E308, StrictMath.hypot(Double.MAX_VALUE, 211370.35));
        assertEquals("Should return 5413.7185", 5413.7185, StrictMath.hypot(
                -5413.7185, Double.MIN_VALUE));

    }

    /**
     * @tests java.lang.StrictMath#IEEEremainder(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "IEEEremainder",
        args = {double.class, double.class}
    )
    public void test_IEEEremainderDD() {
        // Test for method double java.lang.StrictMath.IEEEremainder(double,
        // double)
        assertEquals("Incorrect remainder returned", 0.0, StrictMath.IEEEremainder(
                1.0, 1.0), 0.0);
        assertTrue(
                "Incorrect remainder returned",
                StrictMath.IEEEremainder(1.32, 89.765) >= 1.4705063220631647E-2
                        || StrictMath.IEEEremainder(1.32, 89.765) >= 1.4705063220631649E-2);
        
        assertEquals(Double.NaN, StrictMath.IEEEremainder(Double.NaN, 0.0));
        assertEquals(Double.NaN, StrictMath.IEEEremainder(0.0, Double.NaN));
        assertEquals(Double.NaN, StrictMath.IEEEremainder(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(Double.NaN, StrictMath.IEEEremainder(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(Double.NaN, StrictMath.IEEEremainder(0.0, 0.0));
        assertEquals(Double.NaN, StrictMath.IEEEremainder(-0.0, 0.0));
        
        assertEquals(1.0, StrictMath.IEEEremainder(1.0, Double.POSITIVE_INFINITY));
        assertEquals(1.0, StrictMath.IEEEremainder(1.0, Double.NEGATIVE_INFINITY));        
        
    }

    /**
     * @tests java.lang.StrictMath#log(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log",
        args = {double.class}
    )
    public void test_logD() {
        // Test for method double java.lang.StrictMath.log(double)
        for (double d = 10; d >= -10; d -= 0.5) {
            double answer = StrictMath.log(StrictMath.exp(d));
            assertTrue("Answer does not equal expected answer for d = " + d
                    + " answer = " + answer,
                    StrictMath.abs(answer - d) <= StrictMath
                            .abs(d * 0.00000001));
        }
        
        assertEquals("Returned incorrect value for NaN.", 
                                        Double.NaN, StrictMath.log(Double.NaN));
        assertEquals("Returned incorrect value for positive infinity.", 
            Double.POSITIVE_INFINITY, StrictMath.log(Double.POSITIVE_INFINITY));     
        assertEquals("Returned incorrect value for negative infinity.", 
                          Double.NaN, StrictMath.log(Double.NEGATIVE_INFINITY));  
        assertEquals("Returned incorrect value for positive zero.", 
                                 Double.NEGATIVE_INFINITY, StrictMath.log(0.0));  
        assertEquals("Returned incorrect value for negative zero.", 
                                Double.NEGATIVE_INFINITY, StrictMath.log(-0.0));        
    }
    
    /**
     * @tests java.lang.StrictMath#log10(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log10",
        args = {double.class}
    )
    @SuppressWarnings("boxing")
    public void test_log10_D() {
        // Test for special cases        
        assertTrue("Should return NaN", Double.isNaN(StrictMath
                .log10(Double.NaN)));
        assertTrue("Should return NaN", Double.isNaN(StrictMath
                .log10(-2541.05745687234187532)));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .log10(Double.POSITIVE_INFINITY));
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath.log10(0.0));
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath.log10(+0.0));
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath.log10(-0.0));
        assertEquals("Should return 14.0", 14.0, StrictMath.log10(StrictMath
                .pow(10, 14)));

        assertEquals("Should return 3.7389561269540406", 3.7389561269540406,
                StrictMath.log10(5482.2158));
        assertEquals("Should return 14.661551142893833", 14.661551142893833,
                StrictMath.log10(458723662312872.125782332587));
        assertEquals("Should return -0.9083828622192334", -0.9083828622192334,
                StrictMath.log10(0.12348583358871));
        assertEquals("Should return 308.25471555991675", 308.25471555991675,
                StrictMath.log10(Double.MAX_VALUE));
        assertEquals("Should return -323.3062153431158", -323.3062153431158,
                StrictMath.log10(Double.MIN_VALUE));
    }

    /**
     * @tests java.lang.StrictMath#log1p(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "log1p",
        args = {double.class}
    )
    @SuppressWarnings("boxing")
    public void test_log1p_D() {
        // Test for special cases
        assertTrue("Should return NaN", Double.isNaN(StrictMath
                .log1p(Double.NaN)));
        assertTrue("Should return NaN", Double.isNaN(StrictMath
                .log1p(-32.0482175)));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .log1p(Double.POSITIVE_INFINITY));
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.log1p(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.log1p(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.log1p(-0.0)));

        assertEquals("Should return -0.2941782295312541", -0.2941782295312541,
                StrictMath.log1p(-0.254856327));
        assertEquals("Should return 7.368050685564151", 7.368050685564151,
                StrictMath.log1p(1583.542));
        assertEquals("Should return 0.4633708685409921", 0.4633708685409921,
                StrictMath.log1p(0.5894227));
        assertEquals("Should return 709.782712893384", 709.782712893384,
                StrictMath.log1p(Double.MAX_VALUE));
        assertEquals("Should return Double.MIN_VALUE", Double.MIN_VALUE,
                StrictMath.log1p(Double.MIN_VALUE));
    }

    /**
     * @tests java.lang.StrictMath#max(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "max",
        args = {double.class, double.class}
    )
    public void test_maxDD() {
        // Test for method double java.lang.StrictMath.max(double, double)
        assertEquals("Incorrect double max value", 1908897.6000089, StrictMath.max(
                -1908897.6000089, 1908897.6000089), 0D);
        assertEquals("Incorrect double max value", 1908897.6000089, StrictMath.max(2.0,
                1908897.6000089), 0D);
        assertEquals("Incorrect double max value", -2.0, StrictMath.max(-2.0,
                -1908897.6000089), 0D);

        assertEquals("Incorrect double max value", Double.NaN, 
                                               StrictMath.max(Double.NaN, 1.0));
        assertEquals("Incorrect double max value", 0.0, 
                                                     StrictMath.max(0.0, -0.0));        
    }

    /**
     * @tests java.lang.StrictMath#max(float, float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "max",
        args = {float.class, float.class}
    )
    public void test_maxFF() {
        // Test for method float java.lang.StrictMath.max(float, float)
        assertTrue("Incorrect float max value", StrictMath.max(-1908897.600f,
                1908897.600f) == 1908897.600f);
        assertTrue("Incorrect float max value", StrictMath.max(2.0f,
                1908897.600f) == 1908897.600f);
        assertTrue("Incorrect float max value", StrictMath.max(-2.0f,
                -1908897.600f) == -2.0f);
        assertEquals("Incorrect float max value", Float.NaN, 
                                                 StrictMath.max(Float.NaN, 1f));
        assertEquals("Incorrect float max value", 0f, 
                                                       StrictMath.max(0f, -0f));           
    }

    /**
     * @tests java.lang.StrictMath#max(int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "max",
        args = {int.class, int.class}
    )
    public void test_maxII() {
        // Test for method int java.lang.StrictMath.max(int, int)
        assertEquals("Incorrect int max value", 19088976, StrictMath.max(-19088976,
                19088976));
        assertEquals("Incorrect int max value",
                19088976, StrictMath.max(20, 19088976));
        assertEquals("Incorrect int max value",
                -20, StrictMath.max(-20, -19088976));
        assertEquals("Returned incorrect value.", Integer.MAX_VALUE, 
                      StrictMath.max(Integer.MAX_VALUE, 1));  
        assertEquals("Returned incorrect value.", Integer.MIN_VALUE, 
                StrictMath.max(Integer.MIN_VALUE, Integer.MIN_VALUE));         
    }

    /**
     * @tests java.lang.StrictMath#max(long, long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "max",
        args = {long.class, long.class}
    )
    public void test_maxJJ() {
        // Test for method long java.lang.StrictMath.max(long, long)
        assertEquals("Incorrect long max value", 19088976000089L, StrictMath.max(-19088976000089L,
                19088976000089L));
        assertEquals("Incorrect long max value", 19088976000089L, StrictMath.max(20,
                19088976000089L));
        assertEquals("Incorrect long max value", -20, StrictMath.max(-20,
                -19088976000089L));
        
        assertEquals("Returned incorrect value.", Long.MAX_VALUE, 
                StrictMath.max(Long.MAX_VALUE, 1));  
        assertEquals("Returned incorrect value.", Long.MIN_VALUE, 
          StrictMath.max(Long.MIN_VALUE, Long.MIN_VALUE));         
    }

    /**
     * @tests java.lang.StrictMath#min(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "min",
        args = {double.class, double.class}
    )
    public void test_minDD() {
        // Test for method double java.lang.StrictMath.min(double, double)
        assertEquals("Incorrect double min value", -1908897.6000089, StrictMath.min(
                -1908897.6000089, 1908897.6000089), 0D);
        assertEquals("Incorrect double min value", 2.0, StrictMath.min(2.0,
                1908897.6000089), 0D);
        assertEquals("Incorrect double min value", -1908897.6000089, StrictMath.min(-2.0,
                -1908897.6000089), 0D);
        assertEquals("Returned incorrect value.", Double.NaN, 
                                               StrictMath.min(Double.NaN, 1.0));
        assertEquals("Returned incorrect value.", Double.NaN, 
                                               StrictMath.min(1.0, Double.NaN));      
        assertEquals("Returned incorrect value.", -0.0, StrictMath.min(0.0, -0.0));  
    }

    /**
     * @tests java.lang.StrictMath#min(float, float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "min",
        args = {float.class, float.class}
    )
    public void test_minFF() {
        // Test for method float java.lang.StrictMath.min(float, float)
        assertTrue("Incorrect float min value", StrictMath.min(-1908897.600f,
                1908897.600f) == -1908897.600f);
        assertTrue("Incorrect float min value", StrictMath.min(2.0f,
                1908897.600f) == 2.0f);
        assertTrue("Incorrect float min value", StrictMath.min(-2.0f,
                -1908897.600f) == -1908897.600f);
        
        assertEquals("Returned incorrect value.", Float.NaN, 
                StrictMath.min(Float.NaN, 1f));
        assertEquals("Returned incorrect value.", Float.NaN, 
                StrictMath.min(1f, Float.NaN));      
        assertEquals("Returned incorrect value.", -0f, StrictMath.min(0f, -0f));  
    }

    /**
     * @tests java.lang.StrictMath#min(int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "min",
        args = {int.class, int.class}
    )
    public void test_minII() {
        // Test for method int java.lang.StrictMath.min(int, int)
        assertEquals("Incorrect int min value", -19088976, StrictMath.min(-19088976,
                19088976));
        assertEquals("Incorrect int min value",
                20, StrictMath.min(20, 19088976));
        assertEquals("Incorrect int min value",
                -19088976, StrictMath.min(-20, -19088976));

        assertEquals("Incorrect value was returned.", Double.MIN_VALUE, 
                StrictMath.min(Double.MIN_VALUE, Double.MIN_VALUE));
    }

    /**
     * @tests java.lang.StrictMath#min(long, long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "min",
        args = {long.class, long.class}
    )
    public void test_minJJ() {
        // Test for method long java.lang.StrictMath.min(long, long)
        assertEquals("Incorrect long min value", -19088976000089L, StrictMath.min(-19088976000089L,
                19088976000089L));
        assertEquals("Incorrect long min value", 20, StrictMath.min(20,
                19088976000089L));
        assertEquals("Incorrect long min value", -19088976000089L, StrictMath.min(-20,
                -19088976000089L));
        assertEquals("Incorrect value was returned.", Long.MIN_VALUE, 
                StrictMath.min(Long.MIN_VALUE, Long.MIN_VALUE));        
    }

    /**
     * @tests java.lang.StrictMath#pow(double, double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "pow",
        args = {double.class, double.class}
    )
    public void test_powDD() {
        // Test for method double java.lang.StrictMath.pow(double, double)
        assertTrue("pow returned incorrect value",
                (long) StrictMath.pow(2, 8) == 256l);
        assertTrue("pow returned incorrect value",
                StrictMath.pow(2, -8) == 0.00390625d);
        
        assertEquals(1.0, StrictMath.pow(1.0, 0.0));
        assertEquals(1.0, StrictMath.pow(1.0, -0.0));
        
        assertEquals(Double.NaN, StrictMath.pow(1.0, Double.NaN));
        assertEquals(Double.NaN, StrictMath.pow(Double.NaN, 1.0));  
        
        assertEquals(Double.POSITIVE_INFINITY, 
                StrictMath.pow(1.1, Double.POSITIVE_INFINITY));
        assertEquals(Double.POSITIVE_INFINITY, 
                StrictMath.pow(0.1, Double.NEGATIVE_INFINITY));  
        
        assertEquals(0.0, StrictMath.pow(1.1, Double.NEGATIVE_INFINITY));
        assertEquals(0.0, StrictMath.pow(0.1, Double.POSITIVE_INFINITY));
        
        assertEquals(Double.NaN, StrictMath.pow(1.0, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, StrictMath.pow(1.0, Double.POSITIVE_INFINITY));

        assertEquals(0.0, StrictMath.pow(0.0, 1.0));
        assertEquals(0.0, StrictMath.pow(Double.POSITIVE_INFINITY, -1.0));
        
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.pow(0.0, -1.0));
        assertEquals(Double.POSITIVE_INFINITY, 
                StrictMath.pow(Double.POSITIVE_INFINITY, 1.0));
        
        assertEquals(0.0, StrictMath.pow(-0.0, 2.0));
        assertEquals(0.0, StrictMath.pow(Double.NEGATIVE_INFINITY, -2.0));
        
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.pow(-0.0, -2.0));
        assertEquals(Double.POSITIVE_INFINITY, StrictMath.pow(
                Double.NEGATIVE_INFINITY, 2.0)); 
        
        assertEquals(Double.NEGATIVE_INFINITY, StrictMath.pow(-0.0, -1.0));
        assertEquals(Double.NEGATIVE_INFINITY, StrictMath.pow(
                Double.NEGATIVE_INFINITY, 1.0));         
        
        assertEquals(Double.NaN, StrictMath.pow(-1.0, 1.1));       
    }

    /**
     * @tests java.lang.StrictMath#rint(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "rint",
        args = {double.class}
    )
    public void test_rintD() {
        // Test for method double java.lang.StrictMath.rint(double)
        assertEquals("Failed to round properly - up to odd",
                3.0, StrictMath.rint(2.9), 0D);
        assertTrue("Failed to round properly - NaN", Double.isNaN(StrictMath
                .rint(Double.NaN)));
        assertEquals("Failed to round properly down  to even", 2.0, StrictMath
                .rint(2.1), 0D);
        assertTrue("Failed to round properly " + 2.5 + " to even", StrictMath
                .rint(2.5) == 2.0);
    }

    /**
     * @tests java.lang.StrictMath#round(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "round",
        args = {double.class}
    )
    public void test_roundD() {
        // Test for method long java.lang.StrictMath.round(double)
        assertEquals("Incorrect rounding of a float",
                -91, StrictMath.round(-90.89d));
        
        assertEquals("Incorrect rounding of NaN", 0l, 
                                                  StrictMath.round(Double.NaN));
        assertEquals("Incorrect rounding of NEGATIVE_INFINITY", Long.MIN_VALUE, 
                                    StrictMath.round(Double.NEGATIVE_INFINITY));
        assertEquals("Incorrect rounding of value less than Long.MIN_VALUE", 
            Long.MIN_VALUE, StrictMath.round(new Double(Long.MIN_VALUE - 0.1)));   
        assertEquals("Incorrect rounding of Long.MIN_VALUE", 
                Long.MIN_VALUE, StrictMath.round(new Double(Long.MIN_VALUE))); 
        assertEquals("Incorrect rounding of Long.MAX_VALUE", 
                Long.MAX_VALUE, StrictMath.round(Double.POSITIVE_INFINITY));  
        assertEquals("Incorrect rounding of value greater than Long.MAX_VALUE", 
            Long.MAX_VALUE, StrictMath.round(new Double(Long.MAX_VALUE + 0.1)));        
        
    }

    /**
     * @tests java.lang.StrictMath#round(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "round",
        args = {float.class}
    )
    public void test_roundF() {
        // Test for method int java.lang.StrictMath.round(float)
        assertEquals("Incorrect rounding of a float",
                -91, StrictMath.round(-90.89f));
        
        assertEquals("Incorrect rounding of NaN", 0l, 
                        StrictMath.round(Float.NaN));
        assertEquals("Incorrect rounding of NEGATIVE_INFINITY", 
                  Integer.MIN_VALUE, StrictMath.round(Float.NEGATIVE_INFINITY));
        assertEquals("Incorrect rounding of value less than Integer.MIN_VALUE", 
        Integer.MIN_VALUE, StrictMath.round(new Float(Integer.MIN_VALUE - 0.1)));   
        assertEquals("Incorrect rounding of Integer.MIN_VALUE", 
        Integer.MIN_VALUE, StrictMath.round(new Float(Integer.MIN_VALUE))); 
        assertEquals("Incorrect rounding of Integer.MAX_VALUE", 
        Integer.MAX_VALUE, StrictMath.round(Float.POSITIVE_INFINITY));  
        assertEquals("Incorrect rounding of value greater than Integer.MAX_VALUE", 
        Integer.MAX_VALUE, StrictMath.round(new Float(Integer.MAX_VALUE + 0.1)));
    }
    
    /**
     * @tests java.lang.StrictMath#signum(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "signum",
        args = {double.class}
    )
    public void test_signum_D() {
        assertTrue(Double.isNaN(StrictMath.signum(Double.NaN)));
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.signum(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.signum(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.signum(-0.0)));

        assertEquals(1.0, StrictMath.signum(253681.2187962), 0D);
        assertEquals(-1.0, StrictMath.signum(-125874693.56), 0D);
        assertEquals(1.0, StrictMath.signum(1.2587E-308), 0D);
        assertEquals(-1.0, StrictMath.signum(-1.2587E-308), 0D);

        assertEquals(1.0, StrictMath.signum(Double.MAX_VALUE), 0D);
        assertEquals(1.0, StrictMath.signum(Double.MIN_VALUE), 0D);
        assertEquals(-1.0, StrictMath.signum(-Double.MAX_VALUE), 0D);
        assertEquals(-1.0, StrictMath.signum(-Double.MIN_VALUE), 0D);
        assertEquals(1.0, StrictMath.signum(Double.POSITIVE_INFINITY), 0D);
        assertEquals(-1.0, StrictMath.signum(Double.NEGATIVE_INFINITY), 0D);

    }
    
    /**
     * @tests java.lang.StrictMath#signum(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "signum",
        args = {float.class}
    )
    public void test_signum_F() {
        assertTrue(Float.isNaN(StrictMath.signum(Float.NaN)));
        assertEquals(Float.floatToIntBits(0.0f), Float
                .floatToIntBits(StrictMath.signum(0.0f)));
        assertEquals(Float.floatToIntBits(+0.0f), Float
                .floatToIntBits(StrictMath.signum(+0.0f)));
        assertEquals(Float.floatToIntBits(-0.0f), Float
                .floatToIntBits(StrictMath.signum(-0.0f)));

        assertEquals(1.0f, StrictMath.signum(253681.2187962f), 0f);
        assertEquals(-1.0f, StrictMath.signum(-125874693.56f), 0f);
        assertEquals(1.0f, StrictMath.signum(1.2587E-11f), 0f);
        assertEquals(-1.0f, StrictMath.signum(-1.2587E-11f), 0f);

        assertEquals(1.0f, StrictMath.signum(Float.MAX_VALUE), 0f);
        assertEquals(1.0f, StrictMath.signum(Float.MIN_VALUE), 0f);
        assertEquals(-1.0f, StrictMath.signum(-Float.MAX_VALUE), 0f);
        assertEquals(-1.0f, StrictMath.signum(-Float.MIN_VALUE), 0f);
        assertEquals(1.0f, StrictMath.signum(Float.POSITIVE_INFINITY), 0f);
        assertEquals(-1.0f, StrictMath.signum(Float.NEGATIVE_INFINITY), 0f);
    }

    /**
     * @tests java.lang.StrictMath#sin(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "sin",
        args = {double.class}
    )
    public void test_sinD() {
        // Test for method double java.lang.StrictMath.sin(double)
        assertTrue("Returned incorrect sine", StrictMath.sin(StrictMath
                .asin(OPP / HYP)) == OPP / HYP);
        
        assertEquals("Returned incorrect sin value.", 
                Double.NaN, StrictMath.sin(Double.NaN));
        
        assertEquals("Returned incorrect sin value.", 
                Double.NaN, StrictMath.sin(Double.POSITIVE_INFINITY));
        
        assertEquals("Returned incorrect sin value.", 
                Double.NaN, StrictMath.sin(Double.NEGATIVE_INFINITY));   
        
        assertEquals("Returned incorrect sin value.", 
                0.0, StrictMath.sin(0.0));   
        assertEquals("Returned incorrect sin value.", 
                -0.0, StrictMath.sin(-0.0));        
    }

    /**
     * @tests java.lang.StrictMath#sinh(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "sinh",
        args = {double.class}
    )
    public void test_sinh_D() {
        // Test for special situations
        assertTrue(Double.isNaN(StrictMath.sinh(Double.NaN)));
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath
                        .sinh(Double.POSITIVE_INFINITY), 0D);
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath
                        .sinh(Double.NEGATIVE_INFINITY), 0D);
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.sinh(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.sinh(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.sinh(-0.0)));

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.sinh(1234.56), 0D);
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, StrictMath.sinh(-1234.56), 0D);
        assertEquals("Should return 1.0000000000001666E-6",
                1.0000000000001666E-6, StrictMath.sinh(0.000001), 0D);
        assertEquals("Should return -1.0000000000001666E-6",
                -1.0000000000001666E-6, StrictMath.sinh(-0.000001), 0D);
        assertEquals("Should return 5.115386441963859", 5.115386441963859,
                StrictMath.sinh(2.33482), 0D);
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, StrictMath.sinh(Double.MAX_VALUE), 0D);
        assertEquals("Should return 4.9E-324", 4.9E-324, StrictMath
                .sinh(Double.MIN_VALUE), 0D);
    }
    
    /**
     * @tests java.lang.StrictMath#sqrt(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "sqrt",
        args = {double.class}
    )
    public void test_sqrtD() {
        // Test for method double java.lang.StrictMath.sqrt(double)
        assertEquals("Incorrect root returned1",
                             2, StrictMath.sqrt(StrictMath.pow(StrictMath.sqrt(2), 4)), 0.0);
        assertEquals("Incorrect root returned2", 7, StrictMath.sqrt(49), 0.0);
        
        assertEquals("Incorrect root was returned.", Double.NaN, 
                StrictMath.sqrt(Double.NaN));
        assertEquals("Incorrect root was returned.", Double.NaN, 
                StrictMath.sqrt(-0.1));        
        assertEquals("Incorrect root was returned.", Double.POSITIVE_INFINITY, 
                StrictMath.sqrt(Double.POSITIVE_INFINITY)); 
        
        assertEquals("Incorrect root was returned.", 0.0, StrictMath.sqrt(0.0));     
        assertEquals("Incorrect root was returned.", -0.0, StrictMath.sqrt(-0.0));       
    }

    /**
     * @tests java.lang.StrictMath#tan(double)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Doesn't check boundary values.",
        method = "tan",
        args = {double.class}
    )
    public void test_tanD() {
        // Test for method double java.lang.StrictMath.tan(double)
        assertTrue(
                "Returned incorrect tangent: ",
                StrictMath.tan(StrictMath.atan(1.0)) <= 1.0
                        || StrictMath.tan(StrictMath.atan(1.0)) >= 9.9999999999999983E-1);
    }

    /**
     * @tests java.lang.StrictMath#tanh(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "tanh",
        args = {double.class}
    )
    public void test_tanh_D() {
        // Test for special situations
        assertTrue(Double.isNaN(StrictMath.tanh(Double.NaN)));
        assertEquals("Should return +1.0", +1.0, StrictMath
                .tanh(Double.POSITIVE_INFINITY), 0D);
        assertEquals("Should return -1.0", -1.0, StrictMath
                .tanh(Double.NEGATIVE_INFINITY), 0D);
        assertEquals(Double.doubleToLongBits(0.0), Double
                .doubleToLongBits(StrictMath.tanh(0.0)));
        assertEquals(Double.doubleToLongBits(+0.0), Double
                .doubleToLongBits(StrictMath.tanh(+0.0)));
        assertEquals(Double.doubleToLongBits(-0.0), Double
                .doubleToLongBits(StrictMath.tanh(-0.0)));

        assertEquals("Should return 1.0", 1.0, StrictMath.tanh(1234.56), 0D);
        assertEquals("Should return -1.0", -1.0, StrictMath.tanh(-1234.56), 0D);
        assertEquals("Should return 9.999999999996666E-7",
                9.999999999996666E-7, StrictMath.tanh(0.000001), 0D);
        assertEquals("Should return 0.981422884124941", 0.981422884124941,
                StrictMath.tanh(2.33482), 0D);
        assertEquals("Should return 1.0", 1.0, StrictMath
                .tanh(Double.MAX_VALUE), 0D);
        assertEquals("Should return 4.9E-324", 4.9E-324, StrictMath
                .tanh(Double.MIN_VALUE), 0D);
    }
    
    /**
     * @tests java.lang.StrictMath#random()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "random",
        args = {}
    )
    public void test_random() {
        // There isn't a place for these tests so just stick them here
        assertEquals("Wrong value E",
                4613303445314885481L, Double.doubleToLongBits(StrictMath.E));
        assertEquals("Wrong value PI",
                4614256656552045848L, Double.doubleToLongBits(StrictMath.PI));

        for (int i = 500; i >= 0; i--) {
            double d = StrictMath.random();
            assertTrue("Generated number is out of range: " + d, d >= 0.0
                    && d < 1.0);
        }
    }

    /**
     * @tests java.lang.StrictMath#toRadians(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toRadians",
        args = {double.class}
    )
    public void test_toRadiansD() {
        for (double d = 500; d >= 0; d -= 1.0) {
            double converted = StrictMath.toDegrees(StrictMath.toRadians(d));
            assertTrue("Converted number not equal to original. d = " + d,
                    converted >= d * 0.99999999 && converted <= d * 1.00000001);
        }
    }

    /**
     * @tests java.lang.StrictMath#toDegrees(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toDegrees",
        args = {double.class}
    )
    public void test_toDegreesD() {
        for (double d = 500; d >= 0; d -= 1.0) {
            double converted = StrictMath.toRadians(StrictMath.toDegrees(d));
            assertTrue("Converted number not equal to original. d = " + d,
                    converted >= d * 0.99999999 && converted <= d * 1.00000001);
        }
    }
    
    /**
     * @tests java.lang.StrictMath#ulp(double)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ulp",
        args = {double.class}
    )
     @SuppressWarnings("boxing")
    public void test_ulp_D() {
        // Test for special cases
        assertTrue("Should return NaN", Double
                .isNaN(StrictMath.ulp(Double.NaN)));
        assertEquals("Returned incorrect value", Double.POSITIVE_INFINITY,
                StrictMath.ulp(Double.POSITIVE_INFINITY), 0D);
        assertEquals("Returned incorrect value", Double.POSITIVE_INFINITY,
                StrictMath.ulp(Double.NEGATIVE_INFINITY), 0D);
        assertEquals("Returned incorrect value", Double.MIN_VALUE, StrictMath
                .ulp(0.0), 0D);
        assertEquals("Returned incorrect value", Double.MIN_VALUE, StrictMath
                .ulp(+0.0), 0D);
        assertEquals("Returned incorrect value", Double.MIN_VALUE, StrictMath
                .ulp(-0.0), 0D);
        assertEquals("Returned incorrect value", StrictMath.pow(2, 971),
                StrictMath.ulp(Double.MAX_VALUE), 0D);
        assertEquals("Returned incorrect value", StrictMath.pow(2, 971),
                StrictMath.ulp(-Double.MAX_VALUE), 0D);

        assertEquals("Returned incorrect value", Double.MIN_VALUE, StrictMath
                .ulp(Double.MIN_VALUE), 0D);
        assertEquals("Returned incorrect value", Double.MIN_VALUE, StrictMath
                .ulp(-Double.MIN_VALUE), 0D);

        assertEquals("Returned incorrect value", 2.220446049250313E-16,
                StrictMath.ulp(1.0), 0D);
        assertEquals("Returned incorrect value", 2.220446049250313E-16,
                StrictMath.ulp(-1.0), 0D);
        assertEquals("Returned incorrect value", 2.2737367544323206E-13,
                StrictMath.ulp(1153.0), 0D);
    }

    /**
     * @tests java.lang.StrictMath#ulp(float)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ulp",
        args = {float.class}
    )
    @SuppressWarnings("boxing")
    public void test_ulp_f() {
        // Test for special cases
        assertTrue("Should return NaN", Float.isNaN(StrictMath.ulp(Float.NaN)));
        assertEquals("Returned incorrect value", Float.POSITIVE_INFINITY,
                StrictMath.ulp(Float.POSITIVE_INFINITY), 0f);
        assertEquals("Returned incorrect value", Float.POSITIVE_INFINITY,
                StrictMath.ulp(Float.NEGATIVE_INFINITY), 0f);
        assertEquals("Returned incorrect value", Float.MIN_VALUE, StrictMath
                .ulp(0.0f), 0f);
        assertEquals("Returned incorrect value", Float.MIN_VALUE, StrictMath
                .ulp(+0.0f), 0f);
        assertEquals("Returned incorrect value", Float.MIN_VALUE, StrictMath
                .ulp(-0.0f), 0f);
        assertEquals("Returned incorrect value", 2.028241E31f, StrictMath
                .ulp(Float.MAX_VALUE), 0f);
        assertEquals("Returned incorrect value", 2.028241E31f, StrictMath
                .ulp(-Float.MAX_VALUE), 0f);

        assertEquals("Returned incorrect value", 1.4E-45f, StrictMath
                .ulp(Float.MIN_VALUE), 0f);
        assertEquals("Returned incorrect value", 1.4E-45f, StrictMath
                .ulp(-Float.MIN_VALUE), 0f);

        assertEquals("Returned incorrect value", 1.1920929E-7f, StrictMath
                .ulp(1.0f), 0f);
        assertEquals("Returned incorrect value", 1.1920929E-7f, StrictMath
                .ulp(-1.0f), 0f);
        assertEquals("Returned incorrect value", 1.2207031E-4f, StrictMath
                .ulp(1153.0f), 0f);
        assertEquals("Returned incorrect value", 5.6E-45f, Math
                .ulp(9.403954E-38f), 0f);
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Stress test.",
        method = "pow",
        args = {double.class, double.class}
    )
    public void test_pow_stress() {
        assertTrue(Double.longBitsToDouble(-4610068591539890326L) ==
                StrictMath.pow(-1.0000000000000002e+00,
                        4.5035996273704970e+15));
        assertTrue(Double.longBitsToDouble(4601023824101950163L) == 
                StrictMath.pow(-9.9999999999999978e-01,
                        4.035996273704970e+15));
    }
    
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Stress test.",
        method = "tan",
        args = {double.class}
    )
    public void test_tan_stress(){
        assertTrue(Double.longBitsToDouble(4850236541654588678L) == 
            StrictMath.tan(1.7765241907548024E+269));
    }
    
}
