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

// BEGIN android-note
// Since the original Harmony Code of the BigInteger class was strongly modified,
// in order to use the more efficient OpenSSL BIGNUM implementation,
// no android-modification-tags were placed, at all.
// END android-note

package java.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.io.Serializable;

import org.apache.harmony.math.internal.nls.Messages;

/**
 * This class represents immutable integer numbers of arbitrary length. Large
 * numbers are typically used in security applications and therefore BigIntegers
 * offer dedicated functionality like the generation of large prime numbers or
 * the computation of modular inverse.
 * <p>
 * Since the class was modeled to offer all the functionality as the {@link Integer}
 * class does, it provides even methods that operate bitwise on a two's
 * complement representation of large integers. Note however that the
 * implementations favors an internal representation where magnitude and sign
 * are treated separately. Hence such operations are inefficient and should be
 * discouraged. In simple words: Do NOT implement any bit fields based on
 * BigInteger.
 * <p>
 * <b>Implementation Note:</b> <br>
 * The native OpenSSL library with its BIGNUM operations covers all the
 * meaningful functionality (everything but bit level operations).
 * 
 * @since Android 1.0
 */
public class BigInteger extends Number implements Comparable<BigInteger>,
        Serializable {

    /** This is the serialVersionUID used by the sun implementation. */
    private static final long serialVersionUID = -8287574255936472291L;

    transient BigInt bigInt;
    transient private boolean bigIntIsValid = false;
    transient private boolean oldReprIsValid = false;

    void establishOldRepresentation(String caller) {
        if (!oldReprIsValid) {
            sign = bigInt.sign();
            if (sign != 0) digits = bigInt.littleEndianIntsMagnitude();
            else digits = new int[] { 0 };
            numberLength = digits.length;
            oldReprIsValid = true;
        }
    }

    // The name is confusing:
    // This method is called whenever the old representation has been written.
    // It ensures that the new representation will be established on demand.
    //
    BigInteger withNewRepresentation(String caller) {
        bigIntIsValid = false;
        return this;
    }

    void validate(String caller, String param) {
        if (bigIntIsValid) {
            if (bigInt == null)
                System.out.print("Claiming bigIntIsValid BUT bigInt == null, ");
            else if (bigInt.getNativeBIGNUM() == 0)
                System.out.print("Claiming bigIntIsValid BUT bigInt.bignum == 0, ");
        }
        else {
            if (oldReprIsValid) { // establish new representation
                if (bigInt == null) bigInt = new BigInt();
                bigInt.putLittleEndianInts(digits, (sign < 0));
                bigIntIsValid = true;
            }
            else {
                throw new IllegalArgumentException(caller + ":" + param);
            }
        }
    }

    static void validate1(String caller, BigInteger a) {
        a.validate(caller, "1");
    }

    static void validate2(String caller, BigInteger a, BigInteger b) {
        a.validate(caller, "1");
        b.validate(caller, "2");
    }

    static void validate3(String caller, BigInteger a, BigInteger b, BigInteger c) {
        a.validate(caller, "1");
        b.validate(caller, "2");
        c.validate(caller, "3");
    }

    static void validate4(String caller, BigInteger a, BigInteger b, BigInteger c, BigInteger d) {
        a.validate(caller, "1");
        b.validate(caller, "2");
        c.validate(caller, "3");
        d.validate(caller, "4");
    }

    /** The magnitude of this in the little-endian representation. */
    transient int digits[];

    /**
     * The length of this in measured in ints. Can be less than digits.length().
     */
    transient int numberLength;

    /** The sign of this. */
    transient int sign;

    /* Static Fields */

    /**
     * The {@code BigInteger} constant 0.
     * 
     * @since Android 1.0
     */
    public static final BigInteger ZERO = new BigInteger(0, 0);

    /**
     * The {@code BigInteger} constant 1.
     * 
     * @since Android 1.0
     */
    public static final BigInteger ONE = new BigInteger(1, 1);

    /**
     * The {@code BigInteger} constant 10.
     * 
     * @since Android 1.0
     */
    public static final BigInteger TEN = new BigInteger(1, 10);

    /** The {@code BigInteger} constant -1. */
    static final BigInteger MINUS_ONE = new BigInteger(-1, 1);

    /** The {@code BigInteger} constant 0 used for comparison. */
    static final int EQUALS = 0;

    /** The {@code BigInteger} constant 1 used for comparison. */
    static final int GREATER = 1;

    /** The {@code BigInteger} constant -1 used for comparison. */
    static final int LESS = -1;

    /** All the {@code BigInteger} numbers in the range [0,10] are cached. */
    static final BigInteger[] SMALL_VALUES = { ZERO, ONE, new BigInteger(1, 2),
            new BigInteger(1, 3), new BigInteger(1, 4), new BigInteger(1, 5),
            new BigInteger(1, 6), new BigInteger(1, 7), new BigInteger(1, 8),
            new BigInteger(1, 9), TEN };

    /**/
    private transient int firstNonzeroDigit = -2;
    
    /* Serialized Fields */

    /** sign field, used for serialization. */
    private int signum;

    /** absolute value field, used for serialization */
    private byte[] magnitude;
    
    /** Cache for the hash code. */
    private transient int hashCode = 0;


    /* Package Constructors */

    BigInteger(BigInt a) {
        bigInt = a;
        bigIntIsValid = true;
        validate("BigInteger(BigInt)", "this");
        // !oldReprIsValid
    }

    BigInteger(int sign, long value) {
        bigInt = new BigInt();
        bigInt.putULongInt(value, (sign < 0));
        bigIntIsValid = true;
        // !oldReprIsValid
    }


    /**
     * Constructs a number without creating new space. This construct should be
     * used only if the three fields of representation are known.
     * 
     * @param sign
     *            the sign of the number.
     * @param numberLength
     *            the length of the internal array.
     * @param digits
     *            a reference of some array created before.
     */
    BigInteger(int sign, int numberLength, int[] digits) {
        this.sign = sign;
        this.numberLength = numberLength;
        this.digits = digits;
        oldReprIsValid = true;
        withNewRepresentation("BigInteger(int sign, int numberLength, int[] digits)");
    }


    /* Public Constructors */

    /**
     * Constructs a random non-negative {@code BigInteger} instance in the range
     * [0, 2^(numBits)-1].
     * 
     * @param numBits
     *            maximum length of the new {@code BigInteger} in bits.
     * @param rnd
     *            is an optional random generator to be used.
     * @throws IllegalArgumentException
     *             if {@code numBits} < 0.
     * 
     * @since Android 1.0
     */
    public BigInteger(int numBits, Random rnd) {
        if (numBits < 0) {
            // math.1B=numBits must be non-negative
            throw new IllegalArgumentException(Messages.getString("math.1B")); //$NON-NLS-1$
        }
        if (numBits == 0) {
            sign = 0;
            numberLength = 1;
            digits = new int[] { 0 };
        } else {
            sign = 1;
            numberLength = (numBits + 31) >> 5;
            digits = new int[numberLength];
            for (int i = 0; i < numberLength; i++) {
                digits[i] = rnd.nextInt();
            }
            // Using only the necessary bits
            digits[numberLength - 1] >>>= (-numBits) & 31;
            cutOffLeadingZeroes();
        }
        oldReprIsValid = true;
        withNewRepresentation("BigInteger(int numBits, Random rnd)");
    }

    /**
     * Constructs a random {@code BigInteger} instance in the range [0,
     * 2^(bitLength)-1] which is probably prime. The probability that the
     * returned {@code BigInteger} is prime is beyond (1-1/2^certainty).
     * <p>
     * <b>Implementation Note:</b>
     * Currently {@code rnd} is ignored. The implementation always uses
     * method {@code bn_rand} from the OpenSSL library. {@code bn_rand} 
     * generates cryptographically strong pseudo-random numbers.
     * @see <a href="http://www.openssl.org/docs/crypto/BN_rand.html">
     * Specification of random generator used from OpenSSL library</a>
     * 
     * @param bitLength
     *            length of the new {@code BigInteger} in bits.
     * @param certainty
     *            tolerated primality uncertainty.
     * @param rnd
     *            is an optional random generator to be used.
     * @throws ArithmeticException
     *             if {@code bitLength} < 2.
     * 
     * @since Android 1.0
     */
    public BigInteger(int bitLength, int certainty, Random rnd) {
        if (bitLength < 2) {
            // math.1C=bitLength < 2
            throw new ArithmeticException(Messages.getString("math.1C")); //$NON-NLS-1$
        }
        bigInt = BigInt.generatePrimeDefault(bitLength, rnd, null);
        bigIntIsValid = true;
        // !oldReprIsValid
    }

    /**
     * Constructs a new {@code BigInteger} instance from the string
     * representation. The string representation consists of an optional minus
     * sign followed by a non-empty sequence of decimal digits.
     * 
     * @param val
     *            string representation of the new {@code BigInteger}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if {@code val} is not a valid representation of a {@code
     *             BigInteger}.
     * 
     * @since Android 1.0
     */
    public BigInteger(String val) {
        bigInt = new BigInt();
        bigInt.putDecString(val);
        bigIntIsValid = true;
        // !oldReprIsValid
    }

    /**
     * Constructs a new {@code BigInteger} instance from the string
     * representation. The string representation consists of an optional minus
     * sign followed by a non-empty sequence of digits in the specified radix.
     * For the conversion the method {@code Character.digit(char, radix)} is
     * used.
     * 
     * @param val
     *            string representation of the new {@code BigInteger}.
     * @param radix
     *            the base to be used for the conversion.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if {@code val} is not a valid representation of a {@code
     *             BigInteger} or if {@code radix < Character.MIN_RADIX} or
     *             {@code radix > Character.MAX_RADIX}.
     *             
     * @since Android 1.0
     */
    public BigInteger(String val, int radix) {
        if (val == null) {
            throw new NullPointerException();
        }
        if (radix == 10) {
            bigInt = new BigInt();
            bigInt.putDecString(val);
            bigIntIsValid = true;
            // !oldReprIsValid
        } else if (radix == 16) {
            bigInt = new BigInt();
            bigInt.putHexString(val);
            bigIntIsValid = true;
            // !oldReprIsValid
        } else {
            if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
                // math.11=Radix out of range
                throw new NumberFormatException(Messages.getString("math.11")); //$NON-NLS-1$
            }
            if (val.length() == 0) {
                // math.12=Zero length BigInteger
                throw new NumberFormatException(Messages.getString("math.12")); //$NON-NLS-1$
            }
            BigInteger.setFromString(this, val, radix);
            // oldReprIsValid == true;
        }
    }

    /**
     * Constructs a new {@code BigInteger} instance with the given sign and the
     * given magnitude. The sign is given as an integer (-1 for negative, 0 for
     * zero, 1 for positive). The magnitude is specified as a byte array. The
     * most significant byte is the entry at index 0.
     * 
     * @param signum
     *            sign of the new {@code BigInteger} (-1 for negative, 0 for
     *            zero, 1 for positive).
     * @param magnitude
     *            magnitude of the new {@code BigInteger} with the most
     *            significant byte first.
     * @throws NullPointerException
     *             if {@code magnitude == null}.
     * @throws NumberFormatException
     *             if the sign is not one of -1, 0, 1 or if the sign is zero and
     *             the magnitude contains non-zero entries.
     *             
     * @since Android 1.0
     */
    public BigInteger(int signum, byte[] magnitude) {
        if (magnitude == null) {
            throw new NullPointerException();
        }
        if ((signum < -1) || (signum > 1)) {
            // math.13=Invalid signum value
            throw new NumberFormatException(Messages.getString("math.13")); //$NON-NLS-1$
        }
        if (signum == 0) {
            for (byte element : magnitude) {
                if (element != 0) {
                    // math.14=signum-magnitude mismatch
                    throw new NumberFormatException(Messages.getString("math.14")); //$NON-NLS-1$
                }
            }
        }
        bigInt = new BigInt();
        bigInt.putBigEndian(magnitude, (signum < 0));
        bigIntIsValid = true;
    }

    /**
     * Constructs a new {@code BigInteger} from the given two's complement
     * representation. The most significant byte is the entry at index 0. The
     * most significant bit of this entry determines the sign of the new {@code
     * BigInteger} instance. The given array must not be empty.
     * 
     * @param val
     *            two's complement representation of the new {@code BigInteger}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * @throws NumberFormatException
     *             if the length of {@code val} is zero.
     *             
     * @since Android 1.0
     */
    public BigInteger(byte[] val) {
        if (val.length == 0) {
            // math.12=Zero length BigInteger
            throw new NumberFormatException(Messages.getString("math.12")); //$NON-NLS-1$
        }
        bigInt = new BigInt();
        bigInt.putBigEndianTwosComplement(val);
        bigIntIsValid = true;
    }


    /* Public Methods */

    /**
     * Creates a new {@code BigInteger} whose value is equal to the specified
     * {@code long} argument.
     * 
     * @param val
     *            the value of the new {@code BigInteger}.
     * @return {@code BigInteger} instance with the value {@code val}.
     * 
     * @since Android 1.0
     */
    public static BigInteger valueOf(long val) {
        if (val < 0) {
            if(val != -1) {
                return new BigInteger(-1, -val);
            }
            return MINUS_ONE;
        } else if (val <= 10) {
            return SMALL_VALUES[(int) val];
        } else {// (val > 10)
            return new BigInteger(1, val);
        }
    }

    /**
     * Returns the two's complement representation of this BigInteger in a byte
     * array.
     * 
     * @return two's complement representation of {@code this}.
     * 
     * @since Android 1.0
     */
    public byte[] toByteArray() {
        return twosComplement();
    }

    /**
     * Returns a (new) {@code BigInteger} whose value is the absolute value of
     * {@code this}.
     * 
     * @return {@code abs(this)}.
     * 
     * @since Android 1.0
     */
    public BigInteger abs() {
        validate1("abs()", this);
        if (bigInt.sign() >= 0) return this;
        else {
            BigInt a = bigInt.copy();
            a.setSign(1);
            return new BigInteger(a);
        }
    }

    /**
     * Returns a new {@code BigInteger} whose value is the {@code -this}.
     * 
     * @return {@code -this}.
     * 
     * @since Android 1.0
     */
    public BigInteger negate() {
        validate1("negate()", this);
        int sign = bigInt.sign();
        if (sign == 0) return this;
        else {
            BigInt a = bigInt.copy();
            a.setSign(-sign);
            return new BigInteger(a);
        }
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this + val}.
     * 
     * @param val
     *            value to be added to {@code this}.
     * @return {@code this + val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     *             
     * @since Android 1.0
     */
    public BigInteger add(BigInteger val) {
        validate2("add", this, val);
        if (val.bigInt.sign() == 0) return this;
        if (bigInt.sign() == 0) return val;
        return new BigInteger(BigInt.addition(bigInt, val.bigInt));
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this - val}.
     * 
     * @param val
     *            value to be subtracted from {@code this}.
     * @return {@code this - val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     *             
     * @since Android 1.0
     */
    public BigInteger subtract(BigInteger val) {
        validate2("subtract", this, val);
        if (val.bigInt.sign() == 0) return this;
        else return new BigInteger(BigInt.subtraction(bigInt, val.bigInt));
    }

    /**
     * Returns the sign of this {@code BigInteger}.
     * 
     * @return {@code -1} if {@code this < 0}, 
     *         {@code 0} if {@code this == 0},
     *         {@code 1} if {@code this > 0}.
     *         
     * @since Android 1.0
     */
    public int signum() {
     // Optimization to avoid unnecessary duplicate representation:
        if (oldReprIsValid) return sign;
     // else:
        validate1("signum", this);
        return bigInt.sign();
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this >> n}. For
     * negative arguments, the result is also negative. The shift distance may
     * be negative which means that {@code this} is shifted left.
     * <p>
     * <b>Implementation Note:</b> Usage of this method on negative values is
     * not recommended as the current implementation is not efficient.
     * 
     * @param n
     *            shift distance
     * @return {@code this >> n} if {@code n >= 0}; {@code this << (-n)}
     *         otherwise
     *         
     * @since Android 1.0
     */
    public BigInteger shiftRight(int n) {
        return shiftLeft(-n);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this << n}. The
     * result is equivalent to {@code this * 2^n} if n >= 0. The shift distance
     * may be negative which means that {@code this} is shifted right. The
     * result then corresponds to {@code floor(this / 2^(-n))}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method on negative values is
     * not recommended as the current implementation is not efficient.
     * 
     * @param n
     *            shift distance.
     * @return {@code this << n} if {@code n >= 0}; {@code this >> (-n)}.
     *         otherwise
     * 
     * @since Android 1.0
     */
    public BigInteger shiftLeft(int n) {
        if (n == 0) return this;
        int sign = signum();
        if (sign == 0) return this;
        if ((sign > 0) || (n >= 0)) {
            validate1("shiftLeft", this);
            return new BigInteger(BigInt.shift(bigInt, n));
        }
        else {
            // Negative numbers faking 2's complement:
            // Not worth optimizing this:
            // Sticking to Harmony Java implementation.
            //
            return BitLevel.shiftRight(this, -n);
        }
    }

    /**
     * Returns the length of the value's two's complement representation without
     * leading zeros for positive numbers / without leading ones for negative
     * values.
     * <p>
     * The two's complement representation of {@code this} will be at least
     * {@code bitLength() + 1} bits long.
     * <p>
     * The value will fit into an {@code int} if {@code bitLength() < 32} or
     * into a {@code long} if {@code bitLength() < 64}.
     * 
     * @return the length of the minimal two's complement representation for
     *         {@code this} without the sign bit.
     * 
     * @since Android 1.0
     */
    public int bitLength() {
     // Optimization to avoid unnecessary duplicate representation:
        if (!bigIntIsValid && oldReprIsValid) return BitLevel.bitLength(this);
     // else:
        validate1("bitLength", this);
        return bigInt.bitLength();
    }

    /**
     * Tests whether the bit at position n in {@code this} is set. The result is
     * equivalent to {@code this & (2^n) != 0}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param n
     *            position where the bit in {@code this} has to be inspected.
     * @return {@code this & (2^n) != 0}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     * 
     * @since Android 1.0
     */
    public boolean testBit(int n) {
        if (n < 0) {
            // math.15=Negative bit address
            throw new ArithmeticException(Messages.getString("math.15")); //$NON-NLS-1$
        }
        int sign = signum();
        if ((sign > 0) && bigIntIsValid && !oldReprIsValid) {
            validate1("testBit", this);
            return bigInt.isBitSet(n);
        }
        else {
            // Negative numbers faking 2's complement:
            // Not worth optimizing this:
            // Sticking to Harmony Java implementation.
            //
            establishOldRepresentation("testBit");
            if (n == 0) {
                return ((digits[0] & 1) != 0);
            }
            int intCount = n >> 5;
            if (intCount >= numberLength) {
                return (sign < 0);
            }
            int digit = digits[intCount];
            n = (1 << (n & 31)); // int with 1 set to the needed position
            if (sign < 0) {
                int firstNonZeroDigit = getFirstNonzeroDigit();
                if (  intCount < firstNonZeroDigit  ){
                    return false;
                }else if( firstNonZeroDigit == intCount ){
                    digit = -digit;
                }else{
                    digit = ~digit;
                }
            }
            return ((digit & n) != 0);
        }
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n set. The result is
     * equivalent to {@code this | 2^n}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param n
     *            position where the bit in {@code this} has to be set.
     * @return {@code this | 2^n}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger setBit(int n) {
        establishOldRepresentation("setBit");
        if( !testBit( n ) ){
            return BitLevel.flipBit(this, n);
        }else{
            return this;
        }
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n cleared. The result is
     * equivalent to {@code this & ~(2^n)}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param n
     *            position where the bit in {@code this} has to be cleared.
     * @return {@code this & ~(2^n)}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger clearBit(int n) {
        establishOldRepresentation("clearBit");
        if (testBit(n)) {
            return BitLevel.flipBit(this, n);
        } else {
            return this;
        }
    }

    /**
     * Returns a new {@code BigInteger} which has the same binary representation
     * as {@code this} but with the bit at position n flipped. The result is
     * equivalent to {@code this ^ 2^n}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param n
     *            position where the bit in {@code this} has to be flipped.
     * @return {@code this ^ 2^n}.
     * @throws ArithmeticException
     *             if {@code n < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger flipBit(int n) {
        establishOldRepresentation("flipBit");
        if (n < 0) {
            // math.15=Negative bit address
            throw new ArithmeticException(Messages.getString("math.15")); //$NON-NLS-1$
        }
        return BitLevel.flipBit(this, n);
    }

    /**
     * Returns the position of the lowest set bit in the two's complement
     * representation of this {@code BigInteger}. If all bits are zero (this=0)
     * then -1 is returned as result.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @return position of lowest bit if {@code this != 0}, {@code -1} otherwise
     * 
     * @since Android 1.0
     */
    public int getLowestSetBit() {
        establishOldRepresentation("getLowestSetBit");
        if (sign == 0) {
            return -1;
        }
        // (sign != 0) implies that exists some non zero digit
        int i = getFirstNonzeroDigit();
        return ((i << 5) + Integer.numberOfTrailingZeros(digits[i]));
    }

    /**
     * Use {@code bitLength(0)} if you want to know the length of the binary
     * value in bits.
     * <p>
     * Returns the number of bits in the binary representation of {@code this}
     * which differ from the sign bit. If {@code this} is positive the result is
     * equivalent to the number of bits set in the binary representation of
     * {@code this}. If {@code this} is negative the result is equivalent to the
     * number of bits set in the binary representation of {@code -this-1}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @return number of bits in the binary representation of {@code this} which
     *         differ from the sign bit
     * 
     * @since Android 1.0
     */
    public int bitCount() {
        establishOldRepresentation("bitCount");
        return BitLevel.bitCount(this);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code ~this}. The result
     * of this operation is {@code -this-1}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @return {@code ~this}.
     * 
     * @since Android 1.0
     */
    public BigInteger not() {
        this.establishOldRepresentation("not");
        return Logical.not(this).withNewRepresentation("not");
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this & val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param val
     *            value to be and'ed with {@code this}.
     * @return {@code this & val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger and(BigInteger val) {
        this.establishOldRepresentation("and1");
        val.establishOldRepresentation("and2");
        return Logical.and(this, val).withNewRepresentation("and");
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this | val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param val
     *            value to be or'ed with {@code this}.
     * @return {@code this | val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger or(BigInteger val) {
        this.establishOldRepresentation("or1");
        val.establishOldRepresentation("or2");
        return Logical.or(this, val).withNewRepresentation("or");
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this ^ val}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param val
     *            value to be xor'ed with {@code this}
     * @return {@code this ^ val}
     * @throws NullPointerException
     *             if {@code val == null}
     * 
     * @since Android 1.0
     */
    public BigInteger xor(BigInteger val) {
        this.establishOldRepresentation("xor1");
        val.establishOldRepresentation("xor2");
        return Logical.xor(this, val).withNewRepresentation("xor");
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this & ~val}.
     * Evaluating {@code x.andNot(val)} returns the same result as {@code
     * x.and(val.not())}.
     * <p>
     * <b>Implementation Note:</b> Usage of this method is not recommended as
     * the current implementation is not efficient.
     * 
     * @param val
     *            value to be not'ed and then and'ed with {@code this}.
     * @return {@code this & ~val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger andNot(BigInteger val) {
        this.establishOldRepresentation("andNot1");
        val.establishOldRepresentation("andNot2");
        return Logical.andNot(this, val).withNewRepresentation("andNot");
    }

    /**
     * Returns this {@code BigInteger} as an int value. If {@code this} is too
     * big to be represented as an int, then {@code this} % 2^32 is returned.
     * 
     * @return this {@code BigInteger} as an int value.
     * 
     * @since Android 1.0
     */
    @Override
    public int intValue() {
        if (bigIntIsValid && (bigInt.twosCompFitsIntoBytes(4))) {
            return (int)bigInt.longInt();
        }
        else {
            this.establishOldRepresentation("intValue()");
            return (sign * digits[0]);
        }
    }

    /**
     * Returns this {@code BigInteger} as an long value. If {@code this} is too
     * big to be represented as an long, then {@code this} % 2^64 is returned.
     * 
     * @return this {@code BigInteger} as a long value.
     * 
     * @since Android 1.0
     */
    @Override
    public long longValue() {
        if (bigIntIsValid && (bigInt.twosCompFitsIntoBytes(8))) {
            establishOldRepresentation("longValue()");
            return bigInt.longInt();
        }
        else {
            establishOldRepresentation("longValue()");
            long value = (numberLength > 1) ? (((long) digits[1]) << 32)
                    | (digits[0] & 0xFFFFFFFFL) : (digits[0] & 0xFFFFFFFFL);
            return (sign * value);
        }
    }

    /**
     * Returns this {@code BigInteger} as an float value. If {@code this} is too
     * big to be represented as an float, then {@code Float.POSITIVE_INFINITY}
     * or {@code Float.NEGATIVE_INFINITY} is returned. Note, that not all
     * integers x in the range [-Float.MAX_VALUE, Float.MAX_VALUE] can be
     * represented as a float. The float representation has a mantissa of length
     * 24. For example, 2^24+1 = 16777217 is returned as float 16777216.0.
     * 
     * @return this {@code BigInteger} as a float value.
     *
     * @since Android 1.0
     */
    @Override
    public float floatValue() {
        establishOldRepresentation("floatValue()");
        return (float) doubleValue();
    }

    /**
     * Returns this {@code BigInteger} as an double value. If {@code this} is
     * too big to be represented as an double, then {@code
     * Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY} is
     * returned. Note, that not all integers x in the range [-Double.MAX_VALUE,
     * Double.MAX_VALUE] can be represented as a double. The double
     * representation has a mantissa of length 53. For example, 2^53+1 =
     * 9007199254740993 is returned as double 9007199254740992.0.
     * 
     * @return this {@code BigInteger} as a double value
     * 
     * @since Android 1.0
     */
    @Override
    public double doubleValue() {
        establishOldRepresentation("doubleValue()");
        return Conversion.bigInteger2Double(this);
    }

    /**
     * Compares this {@code BigInteger} with {@code val}. Returns one of the
     * three values 1, 0, or -1.
     * 
     * @param val
     *            value to be compared with {@code this}.
     * @return {@code 1} if {@code this > val}, {@code -1} if {@code this < val}
     *         , {@code 0} if {@code this == val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public int compareTo(BigInteger val) {
        validate2("compareTo", this, val);
        return BigInt.cmp(bigInt, val.bigInt);
    }

    /**
     * Returns the minimum of this {@code BigInteger} and {@code val}.
     * 
     * @param val
     *            value to be used to compute the minimum with {@code this}.
     * @return {@code min(this, val)}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger min(BigInteger val) {
        return ((this.compareTo(val) == -1) ? this : val);
    }

    /**
     * Returns the maximum of this {@code BigInteger} and {@code val}.
     * 
     * @param val
     *            value to be used to compute the maximum with {@code this}
     * @return {@code max(this, val)}
     * @throws NullPointerException
     *             if {@code val == null}
     * 
     * @since Android 1.0
     */
    public BigInteger max(BigInteger val) {
        return ((this.compareTo(val) == 1) ? this : val);
    }

    /**
     * Returns a hash code for this {@code BigInteger}.
     * 
     * @return hash code for {@code this}.
     * 
     * @since Android 1.0
     */
    @Override
    public int hashCode() {
        validate1("hashCode", this);
        if (hashCode != 0) {
            return hashCode;    
        }          
        establishOldRepresentation("hashCode");
        for (int i = 0; i < digits.length; i ++) {
            hashCode = (int)(hashCode * 33 + (digits[i] & 0xffffffff));            
        }  
        hashCode = hashCode * sign;
        return hashCode;
    }

    /**
     * Returns {@code true} if {@code x} is a BigInteger instance and if this
     * instance is equal to this {@code BigInteger}.
     * 
     * @param x
     *            object to be compared with {@code this}.
     * @return true if {@code x} is a BigInteger and {@code this == x}, 
     *          {@code false} otherwise.
     * 
     * @since Android 1.0
     */
    @Override
    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof BigInteger) {
            return this.compareTo((BigInteger)x) == 0;
        }
        return false;
    } 

    /**
     * Returns a string representation of this {@code BigInteger} in decimal
     * form.
     * 
     * @return a string representation of {@code this} in decimal form.
     * 
     * @since Android 1.0
     */
    @Override
    public String toString() {
        validate1("toString()", this);
        return bigInt.decString();
    }

    /**
     * Returns a string containing a string representation of this {@code
     * BigInteger} with base radix. If {@code radix < Character.MIN_RADIX} or
     * {@code radix > Character.MAX_RADIX} then a decimal representation is
     * returned. The characters of the string representation are generated with
     * method {@code Character.forDigit}.
     * 
     * @param radix
     *            base to be used for the string representation.
     * @return a string representation of this with radix 10.
     * 
     * @since Android 1.0
     */
    public String toString(int radix) {
        validate1("toString(int radix)", this);
        if (radix == 10) {
            return bigInt.decString();
//        } else if (radix == 16) {
//            return bigInt.hexString();
        } else {
            establishOldRepresentation("toString(int radix)");
            return Conversion.bigInteger2String(this, radix);
        }
   }

    /**
     * Returns a new {@code BigInteger} whose value is greatest common divisor
     * of {@code this} and {@code val}. If {@code this==0} and {@code val==0}
     * then zero is returned, otherwise the result is positive.
     * 
     * @param val
     *            value with which the greatest common divisor is computed.
     * @return {@code gcd(this, val)}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger gcd(BigInteger val) {
        validate2("gcd", this, val);
        return new BigInteger(BigInt.gcd(bigInt, val.bigInt, null));
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this * val}.
     * 
     * @param val
     *            value to be multiplied with {@code this}.
     * @return {@code this * val}.
     * @throws NullPointerException
     *             if {@code val == null}.
     * 
     * @since Android 1.0
     */
    public BigInteger multiply(BigInteger val) {
        validate2("multiply", this, val);
        return new BigInteger(BigInt.product(bigInt, val.bigInt, null));
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this ^ exp}.
     * 
     * @param exp
     *            exponent to which {@code this} is raised.
     * @return {@code this ^ exp}.
     * @throws ArithmeticException
     *             if {@code exp < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger pow(int exp) {
        if (exp < 0) {
            // math.16=Negative exponent
            throw new ArithmeticException(Messages.getString("math.16")); //$NON-NLS-1$
        }
        validate1("pow", this);
        return new BigInteger(BigInt.exp(bigInt, exp, null));
    }

    /**
     * Returns a {@code BigInteger} array which contains {@code this / divisor}
     * at index 0 and {@code this % divisor} at index 1.
     * 
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code [this / divisor, this % divisor]}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     * @see #divide
     * @see #remainder
     *
@since Android 1.0
     */
    public BigInteger[] divideAndRemainder(BigInteger divisor) {
        validate2("divideAndRemainder", this, divisor);
        BigInt quotient = new BigInt();
        BigInt remainder = new BigInt();
        BigInt.division(bigInt, divisor.bigInt, null, quotient, remainder);
        BigInteger[] a = new BigInteger[2];
        a[0] = new BigInteger(quotient);
        a[1] = new BigInteger(remainder);
        a[0].validate("divideAndRemainder", "quotient");
        a[1].validate("divideAndRemainder", "remainder");
        return a;
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this / divisor}.
     * 
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code this / divisor}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger divide(BigInteger divisor) {
        validate2("divide", this, divisor);
        BigInt quotient = new BigInt();
        BigInt.division(bigInt, divisor.bigInt, null, quotient, null);
        return new BigInteger(quotient);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this % divisor}.
     * Regarding signs this methods has the same behavior as the % operator on
     * int's, i.e. the sign of the remainder is the same as the sign of this.
     * 
     * @param divisor
     *            value by which {@code this} is divided.
     * @return {@code this % divisor}.
     * @throws NullPointerException
     *             if {@code divisor == null}.
     * @throws ArithmeticException
     *             if {@code divisor == 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger remainder(BigInteger divisor) {
        validate2("remainder", this, divisor);
        BigInt remainder = new BigInt();
        BigInt.division(bigInt, divisor.bigInt, null, null, remainder);
        return new BigInteger(remainder);
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code 1/this mod m}. The
     * modulus {@code m} must be positive. The result is guaranteed to be in the
     * interval {@code [0, m)} (0 inclusive, m exclusive). If {@code this} is
     * not relatively prime to m, then an exception is thrown.
     * 
     * @param m
     *            the modulus.
     * @return {@code 1/this mod m}.
     * @throws NullPointerException
     *             if {@code m == null}
     * @throws ArithmeticException
     *             if {@code m < 0 or} if {@code this} is not relatively prime
     *             to {@code m}
     * 
     * @since Android 1.0
     */
    public BigInteger modInverse(BigInteger m) {
        if (m.signum() <= 0) {
            // math.18=BigInteger: modulus not positive
            throw new ArithmeticException(Messages.getString("math.18")); //$NON-NLS-1$
        }
        validate2("modInverse", this, m);
        return new BigInteger(BigInt.modInverse(bigInt, m.bigInt, null));
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this^exponent mod
     * m}. The modulus {@code m} must be positive. The result is guaranteed to
     * be in the interval {@code [0, m)} (0 inclusive, m exclusive). If the
     * exponent is negative, then {@code this.modInverse(m)^(-exponent) mod m)}
     * is computed. The inverse of this only exists if {@code this} is
     * relatively prime to m, otherwise an exception is thrown.
     * 
     * @param exponent
     *            the exponent.
     * @param m
     *            the modulus.
     * @return {@code this^exponent mod val}.
     * @throws NullPointerException
     *             if {@code m == null} or {@code exponent == null}.
     * @throws ArithmeticException
     *             if {@code m < 0} or if {@code exponent<0} and this is not
     *             relatively prime to {@code m}.
     * 
     * @since Android 1.0
     */
    public BigInteger modPow(BigInteger exponent, BigInteger m) {
        if (m.signum() <= 0) {
            // math.18=BigInteger: modulus not positive
            throw new ArithmeticException(Messages.getString("math.18")); //$NON-NLS-1$
        }
        BigInteger base;
        if (exponent.signum() < 0) {
            base = modInverse(m);
//            exponent = exponent.negate(); // Not needed as sign is ignored anyway!
        } else {
            base = this;
        }
        validate3("modPow", base, exponent, m);
        return new BigInteger(BigInt.modExp(base.bigInt, exponent.bigInt, m.bigInt, null));
    }

    /**
     * Returns a new {@code BigInteger} whose value is {@code this mod m}. The
     * modulus {@code m} must be positive. The result is guaranteed to be in the
     * interval {@code [0, m)} (0 inclusive, m exclusive). The behavior of this
     * function is not equivalent to the behavior of the % operator defined for
     * the built-in {@code int}'s.
     * 
     * @param m
     *            the modulus.
     * @return {@code this mod m}.
     * @throws NullPointerException
     *             if {@code m == null}.
     * @throws ArithmeticException
     *             if {@code m < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger mod(BigInteger m) {
        if (m.signum() <= 0) {
            // math.18=BigInteger: modulus not positive
            throw new ArithmeticException(Messages.getString("math.18")); //$NON-NLS-1$
        }
        validate2("mod", this, m);
        return new BigInteger(BigInt.modulus(bigInt, m.bigInt, null));
    }

    /**
     * Tests whether this {@code BigInteger} is probably prime. If {@code true}
     * is returned, then this is prime with a probability beyond
     * (1-1/2^certainty). If {@code false} is returned, then this is definitely
     * composite. If the argument {@code certainty} <= 0, then this method
     * returns true.
     * 
     * @param certainty
     *            tolerated primality uncertainty.
     * @return {@code true}, if {@code this} is probably prime, {@code false}
     *         otherwise.
     * 
     * @since Android 1.0
     */
    public boolean isProbablePrime(int certainty) {
        validate1("isProbablePrime", this);
        return bigInt.isPrime(certainty, null, null);
    }

    /**
     * Returns the smallest integer x > {@code this} which is probably prime as
     * a {@code BigInteger} instance. The probability that the returned {@code
     * BigInteger} is prime is beyond (1-1/2^80).
     * 
     * @return smallest integer > {@code this} which is robably prime.
     * @throws ArithmeticException
     *             if {@code this < 0}.
     * 
     * @since Android 1.0
     */
    public BigInteger nextProbablePrime() {
        if (sign < 0) {
            // math.1A=start < 0: {0}
            throw new ArithmeticException(Messages.getString("math.1A", this)); //$NON-NLS-1$
        }
        return Primality.nextProbablePrime(this);
    }

    /**
     * Returns a random positive {@code BigInteger} instance in the range [0,
     * 2^(bitLength)-1] which is probably prime. The probability that the
     * returned {@code BigInteger} is prime is beyond (1-1/2^80).
     * <p>
     * <b>Implementation Note:</b> Currently {@code rnd} is ignored.
     * 
     * @param bitLength
     *            length of the new {@code BigInteger} in bits.
     * @param rnd
     *            random generator used to generate the new {@code BigInteger}.
     * @return probably prime random {@code BigInteger} instance.
     * @throws IllegalArgumentException
     *             if {@code bitLength < 2}.
     * 
     * @since Android 1.0
     */
    public static BigInteger probablePrime(int bitLength, Random rnd) {
        return new BigInteger(bitLength, 100, rnd);
    }


    /* Private Methods */

    /**
     * Returns the two's complement representation of this BigInteger in a byte
     * array.
     * 
     * @return two's complement representation of {@code this}
     */
    private byte[] twosComplement() {
        establishOldRepresentation("twosComplement()");
        if( this.sign == 0 ){
            return new byte[]{0};
        }
        BigInteger temp = this;
        int bitLen = bitLength();
        int iThis = getFirstNonzeroDigit();
        int bytesLen = (bitLen >> 3) + 1;
        /* Puts the little-endian int array representing the magnitude
         * of this BigInteger into the big-endian byte array. */
        byte[] bytes = new byte[bytesLen];
        int firstByteNumber = 0;
        int highBytes;
        int digitIndex = 0;
        int bytesInInteger = 4;
        int digit;
        int hB;

        if (bytesLen - (numberLength << 2) == 1) {
            bytes[0] = (byte) ((sign < 0) ? -1 : 0);
            highBytes = 4;
            firstByteNumber++;
        } else {
            hB = bytesLen & 3;
            highBytes = (hB == 0) ? 4 : hB;
        }
        
        digitIndex = iThis;
        bytesLen -= iThis << 2;
        
        if (sign < 0) {
            digit = -temp.digits[digitIndex];
            digitIndex++;
            if(digitIndex == numberLength){
                bytesInInteger = highBytes;
            }
            for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                bytes[--bytesLen] = (byte) digit;
            }
            while( bytesLen > firstByteNumber ){
                digit = ~temp.digits[digitIndex];
                digitIndex++;
                if(digitIndex == numberLength){
                    bytesInInteger = highBytes;
                }
                for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                    bytes[--bytesLen] = (byte) digit;
                }
            }
        } else {
            while (bytesLen > firstByteNumber) {
                digit = temp.digits[digitIndex];
                digitIndex++;
                if (digitIndex == numberLength) {
                    bytesInInteger = highBytes;
                }
                for (int i = 0; i < bytesInInteger; i++, digit >>= 8) {
                    bytes[--bytesLen] = (byte) digit;
                }
            }
        }
        return bytes;
    }


    static int multiplyByInt(int res[], int a[], final int aSize, final int factor) {
        long carry = 0;

        for (int i = 0; i < aSize; i++) {
            carry += (a[i] & 0xFFFFFFFFL) * (factor & 0xFFFFFFFFL);
            res[i] = (int)carry;
            carry >>>= 32;
        }
        return (int)carry;
    }

    static int inplaceAdd(int a[], final int aSize, final int addend) {
        long carry = addend & 0xFFFFFFFFL;

        for (int i = 0; (carry != 0) && (i < aSize); i++) {
            carry += a[i] & 0xFFFFFFFFL;
            a[i] = (int) carry;
            carry >>= 32;
        }
        return (int) carry;
    }

    /** @see BigInteger#BigInteger(String, int) */
    private static void setFromString(BigInteger bi, String val, int radix) {
        int sign;
        int[] digits;
        int numberLength;
        int stringLength = val.length();
        int startChar;
        int endChar = stringLength;

        if (val.charAt(0) == '-') {
            sign = -1;
            startChar = 1;
            stringLength--;
        } else {
            sign = 1;
            startChar = 0;
        }
        /*
         * We use the following algorithm: split a string into portions of n
         * characters and convert each portion to an integer according to the
         * radix. Then convert an exp(radix, n) based number to binary using the
         * multiplication method. See D. Knuth, The Art of Computer Programming,
         * vol. 2.
         */

        int charsPerInt = Conversion.digitFitInInt[radix];
        int bigRadixDigitsLength = stringLength / charsPerInt;
        int topChars = stringLength % charsPerInt;

        if (topChars != 0) {
            bigRadixDigitsLength++;
        }
        digits = new int[bigRadixDigitsLength];
        // Get the maximal power of radix that fits in int
        int bigRadix = Conversion.bigRadices[radix - 2];
        // Parse an input string and accumulate the BigInteger's magnitude
        int digitIndex = 0; // index of digits array
        int substrEnd = startChar + ((topChars == 0) ? charsPerInt : topChars);
        int newDigit;

        for (int substrStart = startChar; substrStart < endChar; substrStart = substrEnd, substrEnd = substrStart
                + charsPerInt) {
            int bigRadixDigit = Integer.parseInt(val.substring(substrStart,
                    substrEnd), radix);
            newDigit = multiplyByInt(digits, digits, digitIndex, bigRadix);
            newDigit += inplaceAdd(digits, digitIndex, bigRadixDigit);
            digits[digitIndex++] = newDigit;
        }
        numberLength = digitIndex;
        bi.sign = sign;
        bi.numberLength = numberLength;
        bi.digits = digits;
        bi.cutOffLeadingZeroes();
        bi.oldReprIsValid = true;
        bi.withNewRepresentation("Cordoba-BigInteger: private static setFromString");
    }


    /** Decreases {@code numberLength} if there are zero high elements. */
    final void cutOffLeadingZeroes() {
        while ((numberLength > 0) && (digits[--numberLength] == 0)) {
            ;
        }
        if (digits[numberLength++] == 0) {
            sign = 0;
        }
    }

    /** Tests if {@code this.abs()} is equals to {@code ONE} */
    boolean isOne() {
//        System.out.println("isOne");
        return ((numberLength == 1) && (digits[0] == 1));
    }


    int getFirstNonzeroDigit(){
//        validate1("Cordoba-BigInteger: getFirstNonzeroDigit", this);
        if( firstNonzeroDigit == -2 ){
            int i;
            if( this.sign == 0  ){
                i = -1;
            } else{
                for(i=0; digits[i]==0; i++)
                    ;
            }
            firstNonzeroDigit = i;
        }
        return firstNonzeroDigit;
    }

    /*
     * Returns a copy of the current instance to achieve immutability
     */
// Only used by Primality.nextProbablePrime()
    BigInteger copy() {
        establishOldRepresentation("copy()");
        int[] copyDigits = new int[numberLength];
        System.arraycopy(digits, 0, copyDigits, 0, numberLength);
        return new BigInteger(sign, numberLength, copyDigits);
    }

    /**
     * Assignes all transient fields upon deserialization of a
     * {@code BigInteger} instance.
     */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        bigInt = new BigInt();
        bigInt.putBigEndian(magnitude, (signum < 0));
        bigIntIsValid = true;
        // !oldReprIsValid
    }

    /**
     * Prepares this {@code BigInteger} for serialization, i.e. the
     * non-transient fields {@code signum} and {@code magnitude} are assigned.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        validate("writeObject", "this");
        signum = bigInt.sign();
//        if (magnitude == null)
            magnitude = bigInt.bigEndianMagnitude();
        out.defaultWriteObject();
    }

    
    void unCache(){
        firstNonzeroDigit = -2;
    }
}
