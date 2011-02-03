/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
 * Do some array copying to verify that the performance
 * is acceptable.
 */
public class Main {
    private static final long MAX_TIME_FWD_CP = 1300*1000*1000;
    private static final long MAX_TIME_CP = 900*1000*1000;
    private static final int TEST_SIZE = 100*1024;
    private static final int ITERATIONS = 1000;
    static Object[] objArray1 = new Object[TEST_SIZE];
    static Object[] objArray2 = new Object[TEST_SIZE];

    static int[] intArray1 = new int[TEST_SIZE];
    static int[] intArray2 = new int[TEST_SIZE];

    public static void main(String args[]) {
        fillArray();
        long t0 = System.nanoTime();
        copyObjectArrayForward();
        long t1 = System.nanoTime();
        copyObjectArrayBackWards();
        long t2 = System.nanoTime();
        copyObjectArray();
        long t3 = System.nanoTime();
        copyIntArrayForward();
        long t4 = System.nanoTime();
        copyIntArrayBackWards();
        long t5 = System.nanoTime();
        copyIntArray();
        long t6 = System.nanoTime();

        checkTime("copyObjectArrayForward  ", (t1 - t0), MAX_TIME_FWD_CP);
        checkTime("copyObjectArrayBackWards", (t2 - t1), MAX_TIME_CP);
        checkTime("copyObjectArray         ", (t3 - t2), MAX_TIME_CP);
        checkTime("copyIntArrayForward     ", (t4 - t3), MAX_TIME_FWD_CP);
        checkTime("copyIntArrayBackWards   ", (t5 - t4), MAX_TIME_CP);
        checkTime("copyIntArray            ", (t6 - t5), MAX_TIME_CP);
        System.out.println("Test completed");
    }

    static void checkTime(String useCase, long actual, long expected) {
        if (actual > expected) {
            System.out.println(useCase + " took too long time: " + actual + "us, epxected max" +
                   expected + "us, ratio=" + ((double)actual/(double)expected));
        }
    }

    static void fillArray() {
        String object = new String("My name is garbage");
        for (int i = 0; i < objArray1.length; i++) {
            objArray1[i] = object;
            objArray2[i] = object;
            intArray1[i] = i;
            intArray2[i] = i;
        }
    }

    static void copyObjectArrayForward() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(objArray1, 0, objArray1, 1, objArray1.length - 1);
        }
    }

    static void copyObjectArrayBackWards() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(objArray1, 1, objArray1, 0, objArray1.length - 1);
        }
    }

    static void copyObjectArray() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(objArray1, 0, objArray2, 0, objArray1.length);
        }
    }

    static void copyIntArrayForward() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(intArray1, 0, intArray1, 1, intArray1.length - 1);
        }
    }

    static void copyIntArrayBackWards() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(intArray1, 1, intArray1, 0, intArray1.length - 1);
        }
    }

    static void copyIntArray() {
        for (int i = 0; i < ITERATIONS; i++) {
            System.arraycopy(intArray1, 0, intArray2, 0, intArray1.length);
        }
    }
}
