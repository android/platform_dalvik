/*
 * Copyright (C) 2012 The Android Open Source Project
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

import java.util.Random;

/**
 * Tests for division by zero.
 */
public class ZeroTests {
    private Random mRnd = new Random();
    private long result = 0;

    /*
     * This test verifies that the JIT compiler generates
     * correct code for long division with zero.
     */
    public void longDivTest() throws Exception {
        for (int i = 0; i < 500; i++) {
            if (!div1()) {
                System.out.println("longDivTest failed.");
                return;
            }
        }
        for (int i = 0; i < 500; i++) {
            if (!div2()) {
                System.out.println("longDivTest failed.");
                return;
            }
        }
        for (int i = 0; i < 500; i++) {
            if (!div3()) {
                System.out.println("longDivTest failed.");
                return;
            }
        }
        System.out.println("longDivTest passes");
    }

    /*
     * This test verifies that the JIT compiler generates
     * correct code for long modulus (reminder division) with zero.
     */
    public void longModTest() throws Exception {
        for (int i = 0; i < 500; i++) {
            if (!mod1()) {
                System.out.println("longModTest failed.");
                return;
            }
        }
        for (int i = 0; i < 500; i++) {
            if (!mod2()) {
                System.out.println("longModTest failed.");
                return;
            }
        }
        for (int i = 0; i < 500; i++) {
            if (!mod3()) {
                System.out.println("longModTest failed.");
                return;
            }
        }
        System.out.println("longModTest passes");
    }

    private boolean div1() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) / b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}

        return true;
    }

    private boolean div2() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) / b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}
        return true;
    }

    private boolean div3() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) / b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}
        return true;
    }

    private boolean mod1() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) % b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}
        return true;
    }

    private boolean mod2() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) % b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}
        return true;
    }

    private boolean mod3() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException i) {}
        try {
            long res = 0;
            long a = getNext();
            long b = getNext() - a;
            res = (1 + 10L * a) % b;
            if (b == 0) {
                return false;
            }
        } catch (ArithmeticException a) {}
        return true;
    }

    private long getNext() {
        if (mRnd.nextBoolean()) {
            result++;
        }
        return result;
    }
}
