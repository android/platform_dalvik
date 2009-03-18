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

package org.apache.harmony.math.tests.java.math;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
@TestTargetClass(BigDecimal.class)
/**
 * Class:  java.math.BigDecimal
 * Methods: add, subtract, multiply, divide 
 */
public class BigDecimalArithmeticTest extends TestCase {

    /**
     * Add two numbers of equal positive scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void testAddEqualScalePosPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "123121247898748373566323807282924555312937.1991359555";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.add(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of equal positive scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Together with all other methods including a MathContext these tests form m a complete test set.",
        method = "add",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testAddMathContextEqualScalePosPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1.2313E+41";
        int cScale = -37;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(5, RoundingMode.UP);
        BigDecimal result = aNumber.add(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of equal negative scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void testAddEqualScaleNegNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -10;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "1.231212478987483735663238072829245553129371991359555E+61";
        int cScale = -10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.add(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of equal negative scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Together with all other methods including a MathContext these tests form a complete test set.",
        method = "add",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testAddMathContextEqualScaleNegNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -10;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "1.2312E+61";
        int cScale = -57;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(5, RoundingMode.FLOOR);
        BigDecimal result = aNumber.add(bNumber, mc);
        assertEquals("incorrect value ", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of different scales; the first is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void testAddDiffScalePosNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "7472334294161400358170962860775454459810457634.781384756794987";
        int cScale = 15;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.add(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of different scales using MathContext; the first is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Together with all other methods including a MathContext these tests form a complete test set.",
        method = "add",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testAddMathContextDiffScalePosNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "7.47233429416141E+45";
        int cScale = -31;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(15, RoundingMode.CEILING);
        BigDecimal result = aNumber.add(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two numbers of different scales; the first is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void testAddDiffScaleNegPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1231212478987482988429808779810457634781459480137916301878791834798.7234564568";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.add(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Add two zeroes of different scales; the first is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void testAddDiffScaleZeroZero() {
        String a = "0";
        int aScale = -15;
        String b = "0";
        int bScale = 10;
        String c = "0E-10";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.add(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "add",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testAddMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("123456789012345.678");
        b = new BigDecimal("100000000000000.009");
        assertEquals("incorrect value", "123456789012345.67",
                a.round(mc).toString());
        assertEquals("incorrect value", "100000000000000.00",
                b.round(mc).toString());
        assertEquals("incorrect value", "223456789012345.67",
                a.round(mc).add(b.round(mc)).toString());
        res = a.add(b, mc);
        assertEquals("incorrect value", "223456789012345.68", res.toString());

        mc = new MathContext(33, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("1000000000000000090000000000.0000005");
        res = a.add(b, mc);
        assertEquals("Incorrect value!", "2234567890123456879012345678.90124", res.toString());
        assertEquals("Incorrect scale!", 5, res.scale());
        assertEquals("Incorrect precision!", 33, res.precision());
    }

    /**
     * Subtract two numbers of equal positive scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "subtract",
        args = {java.math.BigDecimal.class}
    )
    public void testSubtractEqualScalePosPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "123121247898748224119637948679166971643339.7522230419";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.subtract(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of equal positive scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "subtract",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testSubtractMathContextEqualScalePosPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1.23121247898749E+41";
        int cScale = -27;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(15, RoundingMode.CEILING);
        BigDecimal result = aNumber.subtract(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of equal negative scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "subtract",
        args = {java.math.BigDecimal.class}
    )
    public void testSubtractEqualScaleNegNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -10;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "1.231212478987482241196379486791669716433397522230419E+61";
        int cScale = -10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.subtract(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of different scales; the first is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "subtract",
        args = {java.math.BigDecimal.class}
    )
    public void testSubtractDiffScalePosNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "-7472334291698975400195996883915836900189542365.218615243205013";
        int cScale = 15;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.subtract(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of different scales using MathContext;
     *  the first is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "subtract",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testSubtractMathContextDiffScalePosNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "-7.4723342916989754E+45";
        int cScale = -29;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(17, RoundingMode.DOWN);
        BigDecimal result = aNumber.subtract(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of different scales; the first is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "subtract",
        args = {java.math.BigDecimal.class}
    )
    public void testSubtractDiffScaleNegPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1231212478987482988429808779810457634781310033452057698121208165201.2765435432";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.subtract(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Subtract two numbers of different scales using MathContext;
     *  the first is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "subtract",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testSubtractMathContextDiffScaleNegPos() {
        String a = "986798656676789766678767876078779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 40;
        String c = "9.867986566767897666787678760787798104576347813847567949870000000000000E+71";
        int cScale = -2;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(70, RoundingMode.HALF_DOWN);
        BigDecimal result = aNumber.subtract(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "subtract",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testSubtractMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567.8");
        b = new BigDecimal("10000000000000000.9");
        assertEquals("incorrect value", "2345678901234567",
                a.round(mc).subtract(b.round(mc)).toString());
        res = a.subtract(b, mc);
        assertEquals("incorrect value", "2345678901234566.9", res.toString());
        assertEquals("Incorrect scale!", 1, res.scale());
        assertEquals("Incorrect precision!", 17, res.precision());

        mc = new MathContext(33, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("1000000000000000090000000000.0000005");
        res = a.subtract(b, mc);
        assertEquals("incorrect value", "234567890123456699012345678.901239", res.toString());
        assertEquals("Incorrect scale!", 6, res.scale());
        assertEquals("Incorrect precision!", 33, res.precision());
    }

    /**
     * Multiply two numbers of positive scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "multiply",
        args = {java.math.BigDecimal.class}
    )
    public void testMultiplyScalePosPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "92000312286217574978643009574114545567010139156902666284589309.1880727173060570190220616";
        int cScale = 25;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.multiply(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of positive scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "multiply",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testMultiplyMathContextScalePosPos() {
        String a = "97665696756578755423325476545428779810457634781384756794987";
        int aScale = -25;
        String b = "87656965586786097685674786576598865";
        int bScale = 10;
        String c = "8.561078619600910561431314228543672720908E+108";
        int cScale = -69;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(40, RoundingMode.HALF_DOWN);
        BigDecimal result = aNumber.multiply(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of negative scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "multiply",
        args = {java.math.BigDecimal.class}
    )
    public void testMultiplyEqualScaleNegNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "9.20003122862175749786430095741145455670101391569026662845893091880727173060570190220616E+111";
        int cScale = -25;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.multiply(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of different scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "multiply",
        args = {java.math.BigDecimal.class}
    )
    public void testMultiplyDiffScalePosNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        String b = "747233429293018787918347987234564568";
        int bScale = -10;
        String c = "920003122862175749786430095741145455670101391569026662845893091880727173060570190220616";
        int cScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.multiply(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of different scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "multiply",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testMultiplyMathContextDiffScalePosNeg() {
        String a = "987667796597975765768768767866756808779810457634781384756794987";
        int aScale = 100;
        String b = "747233429293018787918347987234564568";
        int bScale = -70;
        String c = "7.3801839465418518653942222612429081498248509257207477E+68";
        int cScale = -16;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(53, RoundingMode.HALF_UP);
        BigDecimal result = aNumber.multiply(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of different scales
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for subtract method.",
        method = "multiply",
        args = {java.math.BigDecimal.class}
    )
    public void testMultiplyDiffScaleNegPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "9.20003122862175749786430095741145455670101391569026662845893091880727173060570190220616E+91";
        int cScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.multiply(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Multiply two numbers of different scales using MathContext
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "multiply",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testMultiplyMathContextDiffScaleNegPos() {
        String a = "488757458676796558668876576576579097029810457634781384756794987";
        int aScale = -63;
        String b = "747233429293018787918347987234564568";
        int bScale = 63;
        String c = "3.6521591193960361339707130098174381429788164316E+98";
        int cScale = -52;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        MathContext mc = new MathContext(47, RoundingMode.HALF_UP);
        BigDecimal result = aNumber.multiply(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "multiply",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testMultiplyMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("92345678901234567.8");
        b = new BigDecimal("10000000000000000.9");
        res = a.round(mc).multiply(b.round(mc));
        assertEquals("incorrect value", "923456789012345670000000000000000", res.toString());
        res = res.round(mc);
        assertEquals("incorrect value", "9.2345678901234567E+32", res.toString());
        res = a.multiply(b, mc);
        assertEquals("incorrect value", "9.2345678901234576E+32", res.toString());
        assertEquals("Incorrect scale!", -16, res.scale());
        assertEquals("Incorrect precision!", 17, res.precision());
    }

    /**
     * pow(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "pow",
        args = {int.class}
    )
    public void testPow() {
        String a = "123121247898748298842980";
        int aScale = 10;
        int exp = 10;
        String c = "8004424019039195734129783677098845174704975003788210729597" +
                   "4875206425711159855030832837132149513512555214958035390490" +
                   "798520842025826.594316163502809818340013610490541783276343" +
                   "6514490899700151256484355936102754469438371850240000000000";
        int cScale = 100;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.pow(exp);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * pow(0)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "pow",
        args = {int.class}
    )
    public void testPow0() {
        String a = "123121247898748298842980";
        int aScale = 10;
        int exp = 0;
        String c = "1";
        int cScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.pow(exp);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * ZERO.pow(0)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "pow",
        args = {int.class}
    )
    public void testZeroPow0() {
        String c = "1";
        int cScale = 0;
        BigDecimal result = BigDecimal.ZERO.pow(0);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "pow",
        args = {int.class}
    )
    public void testPowNonTrivial() {
        BigDecimal a, b, res;

        a = new BigDecimal("100.9");
        try {
            res = a.pow(-1);
            fail("ArithmeticException is not thrown for negative exponent");
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            res = a.pow(-103);
            fail("ArithmeticException is not thrown for negative exponent");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * pow(int, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "pow",
        args = {int.class, java.math.MathContext.class}
    )
    public void testPowMathContext() {
        String a = "123121247898748298842980";
        int aScale = 10;
        int exp = 10;
        String c = "8.0044E+130";
        int cScale = -126;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
        BigDecimal result = aNumber.pow(exp, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", cScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "pow",
        args = {int.class, java.math.MathContext.class}
    )
    public void testPowMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(7, RoundingMode.FLOOR);
        a = new BigDecimal("1000000.9");
        assertEquals("incorrect value", "1.000000E+6000",
                a.round(mc).pow(1000).round(mc).toString());
        res = a.pow(1000, mc);
        assertEquals("incorrect value", "1.000900E+6000", res.toString());

        mc = new MathContext(4, RoundingMode.FLOOR);
        a = new BigDecimal("1000.9");
        assertEquals("incorrect value", "1.000E+3000",
                a.round(mc).pow(1000).round(mc).toString());
        res = a.pow(1000, mc);
        assertEquals("incorrect value", "2.458E+3000", res.toString());

        mc = new MathContext(2, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234");
        try {
            res = a.pow(-2, mc);
            fail("ArithmeticException is not thrown");
        } catch (ArithmeticException e) {
            // expected
        }

        a = new BigDecimal("100");
        mc = new MathContext(4, RoundingMode.UNNECESSARY);
        res = a.pow(-2, mc);
        assertEquals("incorrect value", "0.0001", res.toString());

        a = new BigDecimal("1000.9");
        try {
            mc = new MathContext(0, RoundingMode.FLOOR);
            res = a.pow(-1, mc);
            fail("ArithmeticException is not thrown for negative exponent and precision = 0");
        } catch (ArithmeticException e) {
            // expected
        }

        a = new BigDecimal("000.0001");
        try {
            mc = new MathContext(0, RoundingMode.FLOOR);
            res = a.pow(-1, mc);
            fail("ArithmeticException is not thrown for negative exponent and precision = 0");
        } catch (ArithmeticException e) {
            // expected
        }

        a = new BigDecimal("1E-400");
        mc = new MathContext(4, RoundingMode.UNNECESSARY);
        res = a.pow(-1, mc);
        assertEquals("incorrect value", "1E+400", res.toString());

//        Doesn't succeed against JDK of Sun!:
//        mc = new MathContext(3, RoundingMode.FLOOR);
//        a = new BigDecimal("100.9");
//        assertEquals("incorrect value", "1.00E+2000",
//                a.round(mc).pow(1000).round(mc).toString());
//        res = a.pow(1000).round(mc);
//        res = a.pow(1000, mc);
//        assertEquals("incorrect value", "7.783E+2003", res.toString());
    }

    /**
     * Divide by zero
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "ArithmeticException checked.",
        method = "divide",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideByZero() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = BigDecimal.valueOf(0L);
        try {
            aNumber.divide(bNumber);
            fail("ArithmeticException has not been caught");
        } catch (ArithmeticException e) {
            assertEquals("Improper exception message", "Division by zero", e.getMessage());
        }
    }

    /**
     * Divide with ROUND_UNNECESSARY
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException only checked.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class}
    )
    public void testDivideExceptionRM() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        try {
            aNumber.divide(bNumber, BigDecimal.ROUND_UNNECESSARY);
            fail("ArithmeticException has not been caught");
        } catch (ArithmeticException e) {
            assertEquals("Improper exception message", "Rounding necessary", e.getMessage());
        }
    }

    /**
     * Divide with invalid rounding mode
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "IllegalArgumentException only checked.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class}
    )
    public void testDivideExceptionInvalidRM() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        try {
            aNumber.divide(bNumber, 100);
            fail("IllegalArgumentException has not been caught");
        } catch (IllegalArgumentException e) {
            assertEquals("Improper exception message", "Invalid rounding mode", e.getMessage());
        }
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class}
    )
    public void testDivideINonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567E1234");
        b = new BigDecimal("1.23456789012345679");
        assertEquals("incorrect value", "1E+1250",
                a.round(mc).divide(b.round(mc)).toString());
        res = a.divide(b, BigDecimal.ROUND_FLOOR);
        assertEquals("incorrect value", "9.999999999999999E+1249", res.toString());

        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("6172839450617283945061728394.5061975");
        res = a.divide(b, BigDecimal.ROUND_UNNECESSARY);
        assertEquals("incorrect value", "0.2000000", res.toString());

        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("1000000000000000090000000000.0000005");
        try {
            res = a.divide(b, BigDecimal.ROUND_UNNECESSARY);
            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * Divide: local variable exponent is less than zero
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideExpLessZero() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1.64770E+10";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: local variable exponent is equal to zero
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed. Should be added checking for ArithmeticException to complete functional testing.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideExpEqualsZero() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 10;
        String c = "1.64769459009933764189139568605273529E+40";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: local variable exponent is greater than zero
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideExpGreaterZero() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -15;
        String b = "747233429293018787918347987234564568";
        int bScale = 20;
        String c = "1.647694590099337641891395686052735285121058381E+50";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: remainder is zero
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRemainderIsZero() {
        String a = "8311389578904553209874735431110";
        int aScale = -15;
        String b = "237468273682987234567849583746";
        int bScale = 20;
        String c = "3.5000000000000000000000000000000E+36";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_UP, result is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundUpNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_UP, result is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundUpPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_DOWN, result is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundDownNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799283E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_DOWN, result is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundDownPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799283E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_FLOOR, result is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundFloorPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799283E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_FLOOR);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_FLOOR, result is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundFloorNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_FLOOR);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_CEILING, result is positive
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundCeilingPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_CEILING, result is negative
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundCeilingNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799283E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_CEILING);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is positive; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfUpPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is negative; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
 public void testDivideRoundHalfUpNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is positive; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfUpPos1() {
        String a = "92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is negative; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfUpNeg1() {
        String a = "-92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "-1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is negative; equidistant
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfUpNeg2() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        String c = "-1E+5";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_UP);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_DOWN, result is positive; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfDownPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_DOWN, result is negative; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfDownNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_DOWN, result is positive; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfDownPos1() {
        String a = "92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_DOWN, result is negative; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfDownNeg1() {
        String a = "-92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "-1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_UP, result is negative; equidistant
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfDownNeg2() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        String c = "0E+5";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_DOWN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_EVEN, result is positive; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfEvenPos() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_EVEN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_EVEN, result is negative; distance = -1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfEvenNeg() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        String c = "-1.24390557635720517122423359799284E+53";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_EVEN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Divide: rounding mode is ROUND_HALF_EVEN, result is positive; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfEvenPos1() {
        String a = "92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_EVEN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_EVEN, result is negative; distance = 1
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfEvenNeg1() {
        String a = "-92948782094488478231212478987482988798104576347813847567949855464535634534563456";
        int aScale = -24;
        String b = "74723342238476237823754692930187879183479";
        int bScale = 13;
        String c = "-1.2439055763572051712242335979928354832010167729111113605E+76";
        int resScale = -21;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_EVEN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide: rounding mode is ROUND_HALF_EVEN, result is negative; equidistant
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ROUND_UNNECESSARY and exceptions checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideRoundHalfEvenNeg2() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        String c = "0E+5";
        int resScale = -5;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, resScale, BigDecimal.ROUND_HALF_EVEN);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, int.class}
    )
    public void testDivideIINonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567E1234");
        b = new BigDecimal("1.23456789012345679");
        res = a.divide(b, -1220, BigDecimal.ROUND_FLOOR);
        assertEquals("incorrect value", "9.99999999999999927099999343899E+1249", res.toString());

        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("6172839450617283945061728394.5061975");
        res = a.divide(b, 1, BigDecimal.ROUND_UNNECESSARY);
        assertEquals("incorrect value", "0.2", res.toString());

        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("6172839450617283945061728394.5061975");
        try {
            res = a.divide(b, 0, BigDecimal.ROUND_UNNECESSARY);
            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * Divide to BigDecimal
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Common functionality checked",
        method = "divide",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideBigDecimal1() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        String c = "-5E+4";
        int resScale = -4;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Divide to BigDecimal
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Common functionality checked",
        method = "divide",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideBigDecimal2() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = -15;
        String c = "-5E-26";
        int resScale = 26;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeUP() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = -15;
        int newScale = 31;
        RoundingMode rm = RoundingMode.UP;
        String c = "-5.00000E-26";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeDOWN() {
        String a = "-37361671119238118911893939591735";
        int aScale = 10;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 31;
        RoundingMode rm = RoundingMode.DOWN;
        String c = "-50000.0000000000000000000000000000000";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeCEILING() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 100;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 45;
        RoundingMode rm = RoundingMode.CEILING;
        String c = "1E-45";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeFLOOR() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 100;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 45;
        RoundingMode rm = RoundingMode.FLOOR;
        String c = "0E-45";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeHALF_UP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -51;
        String b = "74723342238476237823787879183470";
        int bScale = 45;
        int newScale = 3;
        RoundingMode rm = RoundingMode.HALF_UP;
        String c = "50000260373164286401361913262100972218038099522752460421" +
                   "05959924024355721031761947728703598332749334086415670525" +
                   "3761096961.670";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeHALF_DOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 5;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 7;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        String c = "500002603731642864013619132621009722.1803810";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * divide(BigDecimal, scale, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException and UNNECESSARY round mode checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    public void testDivideBigDecimalScaleRoundingModeHALF_EVEN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 5;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 7;
        RoundingMode rm = RoundingMode.HALF_EVEN;
        String c = "500002603731642864013619132621009722.1803810";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divide",
        args = {java.math.BigDecimal.class, int.class, java.math.RoundingMode.class}
    )
    @KnownFailure("Has a rounding problem. seems like the precision is"
            + " 1 too small and cuts off the last digit. also this test might"
            + "not be correct. The name implies that scale should be used.")
    public void testDivideScaleRoundingModeNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567.1");
        b = new BigDecimal("12345678901234567.9");
        assertEquals("incorrect value", "1",
                a.round(mc).divide(b.round(mc)).toString());
        res = a.divide(b, mc);
        assertEquals("incorrect value", "0.99999999999999993", res.toString());

        mc = new MathContext(13, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("6172839450617283945061728394.5061975");
        res = a.divide(b, mc);
        assertEquals("incorrect value", "0.2", res.toString());

        mc = new MathContext(33, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("1000000000000000090000000000.0000005");
        try {
            res = a.divide(b, mc);
            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextUP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 15;
        String b = "748766876876723342238476237823787879183470";
        int bScale = 10;
        int precision = 21;
        RoundingMode rm = RoundingMode.UP;
        MathContext mc = new MathContext(precision, rm);
        String c = "49897861180.2562512996";
        int resScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextDOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 15;
        String b = "748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.DOWN;
        MathContext mc = new MathContext(precision, rm);
        String c = "4.98978611802562512995E+70";
        int resScale = -50;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextCEILING() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 15;
        String b = "748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.CEILING;
        MathContext mc = new MathContext(precision, rm);
        String c = "4.98978611802562512996E+70";
        int resScale = -50;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextFLOOR() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 15;
        String b = "748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.FLOOR;
        MathContext mc = new MathContext(precision, rm);
        String c = "4.98978611802562512995E+70";
        int resScale = -50;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextHALF_UP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.HALF_UP;
        MathContext mc = new MathContext(precision, rm);
        String c = "2.77923185514690367475E+26";
        int resScale = -6;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextHALF_DOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        MathContext mc = new MathContext(precision, rm);
        String c = "2.77923185514690367475E+26";
        int resScale = -6;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divide(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideBigDecimalScaleMathContextHALF_EVEN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 21;
        RoundingMode rm = RoundingMode.HALF_EVEN;
        MathContext mc = new MathContext(precision, rm);
        String c = "2.77923185514690367475E+26";
        int resScale = -6;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    @KnownFailure("The same test and the same problem like "
            + "testDivideScaleRoundingModeNonTrivial")
    public void testDivideMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

// FAILS AGAINST RI!:
//        mc = new MathContext(6, RoundingMode.FLOOR);
//        a = new BigDecimal("12345.1");
//        b = new BigDecimal("12345.9");
//        assertEquals("incorrect value", "1",
//                a.round(mc).divide(b.round(mc)).toString());
//        res = a.divide(b, mc);
//        assertEquals("incorrect value", "0.99993", res.toString());

        mc = new MathContext(5, RoundingMode.FLOOR);
        a = new BigDecimal("12345.1");
        b = new BigDecimal("12345.9");
        assertEquals("incorrect value", "1",
                a.round(mc).divide(b.round(mc)).toString());
        res = a.divide(b, mc);
        assertEquals("incorrect value", "0.99993", res.toString());

        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567.1");
        b = new BigDecimal("12345678901234567.9");
        assertEquals("incorrect value", "1",
                a.round(mc).divide(b.round(mc)).toString());
        res = a.divide(b, mc);
        assertEquals("incorrect value", "0.99999999999999993", res.toString());
        assertEquals("incorrect value", res.round(mc).toString(), res.toString());

        mc = new MathContext(13, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("6172839450617283945061728394.5061975");
        res = a.divide(b, mc);
        assertEquals("incorrect value", "0.2", res.toString());

        mc = new MathContext(33, RoundingMode.UNNECESSARY);
        a = new BigDecimal("1234567890123456789012345678.9012395");
        b = new BigDecimal("1000000000000000090000000000.0000005");
        try {
            res = a.divide(b, mc);
            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * divideToIntegralValue(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "divideToIntegralValue",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideToIntegralValue() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        String c = "277923185514690367474770683";
        int resScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divideToIntegralValue(bNumber);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "divideToIntegralValue",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideToIntegralValueByZero() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "0";
        int bScale = -70;
        String res = "277923185514690367474770683";
        int resScale = 0;
        String rem = "1.3032693871288309587558885943391070087960319452465789990E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        try {
            BigDecimal result = aNumber.divideToIntegralValue(bNumber);
            fail("ArithmeticException not thrown for division by 0");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * divideToIntegralValue(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divideToIntegralValue",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideToIntegralValueMathContextUP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 32;
        RoundingMode rm = RoundingMode.UP;
        MathContext mc = new MathContext(precision, rm);
        String c = "277923185514690367474770683";
        int resScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divideToIntegralValue(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * divideToIntegralValue(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divideToIntegralValue",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideToIntegralValueMathContextDOWN() {
        String a = "3736186567876876578956958769675785435673453453653543654354365435675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 75;
        RoundingMode rm = RoundingMode.DOWN;
        MathContext mc = new MathContext(precision, rm);
        String c = "2.7792318551469036747477068339450205874992634417590178670822889E+62";
        int resScale = -1;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divideToIntegralValue(bNumber, mc);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }
    
    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divideToIntegralValue",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideToIntegralValueMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        a = new BigDecimal("92345678901234567.8");
        b = new BigDecimal("43");
        res = a.multiply(b);
        assertEquals("incorrect value", "3970864192753086415.4", res.toString());

        mc = new MathContext(20, RoundingMode.DOWN);
        a = new BigDecimal("3970864192753086415.4");
        b = new BigDecimal("92345678901234567.8");
        b = new BigDecimal("92345678901234567.8001");
        assertEquals("incorrect value", "43",
                a.round(mc).divideToIntegralValue(b.round(mc)).toString());
        res = a.divideToIntegralValue(b, mc);
        assertEquals("incorrect value", "42", res.toString());

//        mc = new MathContext(1, RoundingMode.DOWN);
//        res = a.divideToIntegralValue(b, mc);
//        assertEquals("incorrect value", "42", res.toString());


        mc = new MathContext(17, RoundingMode.FLOOR);
        a = new BigDecimal("518518513851851830");
        b = new BigDecimal("12345678901234567.9");
        assertEquals("incorrect value", "42",
                a.round(mc).divideToIntegralValue(b.round(mc)).toString());
        res = a.divideToIntegralValue(b, mc);
        assertEquals("incorrect value", "41", res.toString());
    }

    /**
     * divideAndRemainder(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideAndRemainder1() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        String res = "277923185514690367474770683";
        int resScale = 0;
        String rem = "1.3032693871288309587558885943391070087960319452465789990E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result[] = aNumber.divideAndRemainder(bNumber);
        assertEquals("incorrect quotient value", res, result[0].toString());
        assertEquals("incorrect quotient scale", resScale, result[0].scale());
        assertEquals("incorrect remainder value", rem, result[1].toString());
        assertEquals("incorrect remainder scale", remScale, result[1].scale());
    }

    /**
     * divideAndRemainder(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideAndRemainder2() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        String res = "2779231855146903674747706830969461168692256919247547952" +
                     "2608549363170374005512836303475980101168105698072946555" +
                     "6862849";
        int resScale = 0;
        String rem = "3.4935796954060524114470681810486417234751682675102093970E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result[] = aNumber.divideAndRemainder(bNumber);
        assertEquals("incorrect quotient value", res, result[0].toString());
        assertEquals("incorrect quotient scale", resScale, result[0].scale());
        assertEquals("incorrect remainder value", rem, result[1].toString());
        assertEquals("incorrect remainder scale", remScale, result[1].scale());
    }
    
    /**
     * divideAndRemainder(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class}
    )
    public void testDivideAndRemainderByZero() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "0";
        int bScale = -70;
        String res = "277923185514690367474770683";
        int resScale = 0;
        String rem = "1.3032693871288309587558885943391070087960319452465789990E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        try {
            BigDecimal result[] = aNumber.divideAndRemainder(bNumber);
            fail("ArithmeticException not thrown for division by 0");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * divideAndRemainder(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideAndRemainderMathContextUP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 70;
        int precision = 75;
        RoundingMode rm = RoundingMode.UP;
        MathContext mc = new MathContext(precision, rm);
        String res = "277923185514690367474770683";
        int resScale = 0;
        String rem = "1.3032693871288309587558885943391070087960319452465789990E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result[] = aNumber.divideAndRemainder(bNumber, mc);
        assertEquals("incorrect quotient value", res, result[0].toString());
        assertEquals("incorrect quotient scale", resScale, result[0].scale());
        assertEquals("incorrect remainder value", rem, result[1].toString());
        assertEquals("incorrect remainder scale", remScale, result[1].scale());
    }

    /**
     * divideAndRemainder(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideAndRemainderMathContextDOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 20;
        int precision = 15;
        RoundingMode rm = RoundingMode.DOWN;
        MathContext mc = new MathContext(precision, rm);
        String res = "0E-25";
        int resScale = 25;
        String rem = "3736186567876.876578956958765675671119238118911893939591735";
        int remScale = 45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result[] = aNumber.divideAndRemainder(bNumber, mc);
        assertEquals("incorrect quotient value", res, result[0].toString());
        assertEquals("incorrect quotient scale", resScale, result[0].scale());
        assertEquals("incorrect remainder value", rem, result[1].toString());
        assertEquals("incorrect remainder scale", remScale, result[1].scale());
    }
    
    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "divideAndRemainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testDivideAndRemainderMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res[];

        mc = new MathContext(13, RoundingMode.FLOOR);
        a = new BigDecimal("12345678901234567.1");
        b = new BigDecimal("12345678901234567.9");
        assertEquals("incorrect value", "0E+4",
                a.round(mc).divideAndRemainder(b.round(mc))[1].toString());
        res = a.divideAndRemainder(b, mc);
        assertEquals("incorrect value", "12345678901234567.1", res[1].toString());

        mc = new MathContext(1, RoundingMode.UNNECESSARY);
        a = new BigDecimal("6172839450617283945061728394.5061976");
        b = new BigDecimal("1234567890123456789012345678.9012395");
        res = a.divideAndRemainder(b, mc);
        assertEquals("incorrect value", "1E-7", res[1].toString());

        mc = new MathContext(3, RoundingMode.UNNECESSARY);
        a = new BigDecimal("6172839450617283945061728394.6000000");
        b = new BigDecimal("1234567890123456789012345678.9012395");
        try {
            res = a.divideAndRemainder(b, mc);
            assertEquals("incorrect value", "0.0938025", res[1].toString());
            assertEquals("incorrect value", "0.09", res[1].round(mc).toString());
//            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * remainder(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "remainder",
        args = {java.math.BigDecimal.class}
    )
    public void testRemainder1() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 10;
        String res = "3736186567876.876578956958765675671119238118911893939591735";
        int resScale = 45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.remainder(bNumber);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    /**
     * remainder(BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "remainder",
        args = {java.math.BigDecimal.class}
    )
    public void testRemainder2() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 10;
        String res = "1149310942946292909508821656680979993738625937.2065885780";
        int resScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.remainder(bNumber);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "remainder",
        args = {java.math.BigDecimal.class}
    )
    public void testRemainderByZero() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "0";
        int bScale = -70;
        String res = "277923185514690367474770683";
        int resScale = 0;
        String rem = "1.3032693871288309587558885943391070087960319452465789990E-15";
        int remScale = 70;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        try {
            BigDecimal result = aNumber.remainder(bNumber);
            fail("ArithmeticException not thrown for division by 0");
        } catch (ArithmeticException e) {
            // expected
        }
    }
    /**
     * remainder(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "remainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testRemainderMathContextHALF_UP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 10;
        int precision = 15;
        RoundingMode rm = RoundingMode.HALF_UP;
        MathContext mc = new MathContext(precision, rm);
        String res = "3736186567876.876578956958765675671119238118911893939591735";
        int resScale = 45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.remainder(bNumber, mc);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    /**
     * remainder(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "remainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testRemainderMathContextHALF_DOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -45;
        String b = "134432345432345748766876876723342238476237823787879183470";
        int bScale = 10;
        int precision = 75;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        MathContext mc = new MathContext(precision, rm);
        String res = "1149310942946292909508821656680979993738625937.2065885780";
        int resScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.remainder(bNumber, mc);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    /**
     * Non-trivial tests using MathContext:
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "remainder",
        args = {java.math.BigDecimal.class, java.math.MathContext.class}
    )
    public void testRemainderMathContextNonTrivial() {
        MathContext mc;
        BigDecimal a, b, res;

        mc = new MathContext(13, RoundingMode.DOWN);
        a = new BigDecimal("12345678901234567.1");
        b = new BigDecimal("12345678901234567.9");
        assertEquals("incorrect value", "0E+4",
                a.round(mc).divideAndRemainder(b.round(mc))[1].toString());
        res = a.remainder(b, mc);
        assertEquals("incorrect value", "12345678901234567.1", res.toString());

        mc = new MathContext(1, RoundingMode.UNNECESSARY);
        a = new BigDecimal("6172839450617283945061728394.5061976");
        b = new BigDecimal("1234567890123456789012345678.9012395");
        res = a.remainder(b, mc);
        assertEquals("incorrect value", "1E-7", res.toString());

        mc = new MathContext(3, RoundingMode.UNNECESSARY);
        a = new BigDecimal("6172839450617283945061728394.6000000");
        b = new BigDecimal("1234567890123456789012345678.9012395");
        try {
            res = a.remainder(b, mc);
            assertEquals("incorrect value", "0.0938025", res.toString());
            assertEquals("incorrect value", "0.09", res.round(mc).toString());
//            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * round(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "round",
        args = {java.math.MathContext.class}
    )
    public void testRoundMathContextHALF_DOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -45;
        int precision = 75;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        MathContext mc = new MathContext(precision, rm);
        String res = "3.736186567876876578956958765675671119238118911893939591735E+102";
        int resScale = -45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.round(mc);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    /**
     * round(BigDecimal, MathContext)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "round",
        args = {java.math.MathContext.class}
    )
    public void testRoundMathContextHALF_UP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        int precision = 15;
        RoundingMode rm = RoundingMode.HALF_UP;
        MathContext mc = new MathContext(precision, rm);
        String res = "3736186567876.88";
        int resScale = 2;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.round(mc);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", resScale, result.scale());
    }

    /**
     * round(BigDecimal, MathContext) when precision = 0
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "ArithmeticException checking missed.",
        method = "round",
        args = {java.math.MathContext.class}
    )
    public void testRoundMathContextPrecision0() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        int precision = 0;
        RoundingMode rm = RoundingMode.HALF_UP;
        MathContext mc = new MathContext(precision, rm);
        String res = "3736186567876.876578956958765675671119238118911893939591735";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.round(mc);
        assertEquals("incorrect quotient value", res, result.toString());
        assertEquals("incorrect quotient scale", aScale, result.scale());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "round",
        args = {java.math.MathContext.class}
    )
    public void testRoundNonTrivial() {
        MathContext mc;
        String biStr = new String( "12345678901234567890123456789012345.0E+10");
        String nbiStr = new String("-12345678901234567890123456789012345.E+10");
        BigDecimal bd;

        mc = new MathContext(17, RoundingMode.FLOOR);
        bd = new BigDecimal(new BigInteger("123456789012345678"), 3, mc);
        assertEquals("incorrect value", "123456789012345.67", bd.toString());

        mc = new MathContext(31, RoundingMode.UP);
        bd = (new BigDecimal(biStr)).round(mc);
        assertEquals("incorrect value",  "1.234567890123456789012345678902E+44", bd.toString());
        bd = (new BigDecimal(nbiStr)).round(mc);
        assertEquals("incorrect value", "-1.234567890123456789012345678902E+44", bd.toString());

        mc = new MathContext(28, RoundingMode.DOWN);
        bd = (new BigDecimal(biStr)).round(mc);
        assertEquals("incorrect value",  "1.234567890123456789012345678E+44", bd.toString());
        bd = (new BigDecimal(nbiStr)).round(mc);
        assertEquals("incorrect value", "-1.234567890123456789012345678E+44", bd.toString());

        mc = new MathContext(33, RoundingMode.CEILING);
        bd = (new BigDecimal(biStr)).round(mc);
        assertEquals("incorrect value",  "1.23456789012345678901234567890124E+44", bd.toString());
        bd = (new BigDecimal(nbiStr)).round(mc);
        assertEquals("incorrect value", "-1.23456789012345678901234567890123E+44", bd.toString());

        mc = new MathContext(34, RoundingMode.UNNECESSARY);
        try {
            bd = (new BigDecimal(biStr)).round(mc);
            fail("No ArithmeticException for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }
        try {
            bd = (new BigDecimal(nbiStr)).round(mc);
            fail("No ArithmeticException for RoundingMode.UNNECESSARY");
        } catch (ArithmeticException e) {
            // expected
        }

        mc = new MathContext(7, RoundingMode.FLOOR);
        bd = new BigDecimal("1000000.9", mc);
        assertEquals("incorrect value", "1000000", bd.toString());
    }

    /**
     * ulp() of a positive BigDecimal
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for ulp method.",
        method = "ulp",
        args = {}
    )
    public void testUlpPos() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = -45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.ulp();
        String res = "1E+45";
        int resScale = -45;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * ulp() of a negative BigDecimal
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for ulp method.",
        method = "ulp",
        args = {}
    )
    public void testUlpNeg() {
        String a = "-3736186567876876578956958765675671119238118911893939591735";
        int aScale = 45;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.ulp();
        String res = "1E-45";
        int resScale = 45;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * ulp() of a negative BigDecimal
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for ulp method.",
        method = "ulp",
        args = {}
    )
    public void testUlpZero() {
        String a = "0";
        int aScale = 2;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.ulp();
        String res = "0.01";
        int resScale = 2;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

// ANDROID ADDED

    /**
     * @tests java.math.BigDecimal#add(java.math.BigDecimal)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for add method.",
        method = "add",
        args = {java.math.BigDecimal.class}
    )
    public void test_addBigDecimal() {
        BigDecimal add1 = new BigDecimal("23.456");
        BigDecimal add2 = new BigDecimal("3849.235");
        BigDecimal sum = add1.add(add2);
        assertTrue("the sum of 23.456 + 3849.235 is wrong", sum.unscaledValue().toString().equals(
                "3872691")
                && sum.scale() == 3);
        assertTrue("the sum of 23.456 + 3849.235 is not printed correctly", sum.toString().equals(
                "3872.691"));
        BigDecimal add3 = new BigDecimal(12.34E02D);
        assertTrue("the sum of 23.456 + 12.34E02 is not printed correctly", (add1.add(add3))
                .toString().equals("1257.456"));
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.MathContext) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeUP() {
        String a = "-37361671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.UP;
        String c = "-1";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeDOWN() {
        String a = "-37361671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.DOWN;
        String c = "0";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeCEILING() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.CEILING;
        String c = "50000260373164286401361914";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeFLOOR() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.FLOOR;
        String c = "50000260373164286401361913";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeHALF_UP() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.HALF_UP;
        String c = "50000260373164286401361913";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeHALF_DOWN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        int aScale = 5;
        String b = "74723342238476237823787879183470";
        int bScale = 15;
        int newScale = 7;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        String c = "500002603731642864013619132621009722.1803810";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal result = aNumber.divide(bNumber, newScale, rm);
        assertEquals("incorrect value", c, result.toString());
        assertEquals("incorrect scale", newScale, result.scale());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingModeHALF_EVEN() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.HALF_EVEN;
        String c = "50000260373164286401361913";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        BigDecimal result = aNumber.divide(bNumber, rm);
        assertEquals("incorrect value", c, result.toString());
    }

    /**
     * @tests java.math.BigDecimal#divide(java.math.BigDecimal,
     *        java.math.RoundingMode) divide(BigDecimal, RoundingMode)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "This is a complete subset of tests for divide method.",
        method = "divide",
        args = {java.math.BigDecimal.class, java.math.RoundingMode.class}
    )
    public void test_DivideBigDecimalRoundingExc() {
        String a = "3736186567876876578956958765675671119238118911893939591735";
        String b = "74723342238476237823787879183470";
        RoundingMode rm = RoundingMode.UNNECESSARY;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        BigDecimal bNumber = new BigDecimal(new BigInteger(b));
        try {
            aNumber.divide(bNumber, rm);
            fail("ArithmeticException is not thrown for RoundingMode.UNNECESSARY divider");
        } catch (java.lang.ArithmeticException ae) {
            // expected
        }
        try {
            bNumber = new BigDecimal(0);
            aNumber.divide(bNumber, rm);
            fail("ArithmeticException is not thrown for zero divider");
        } catch (java.lang.ArithmeticException ae) {
            // expected
        }
    }

}
