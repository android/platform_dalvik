/*
 * Copyright (C) 2017 The Android Open Source Project
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;

public class VarHandleDexTest {

    // A static field to access.
    private static boolean bsValue = false;

    // An instance field to access.
    private Float fValue = Float.valueOf(99.9f);

    public static void main(String[] args) throws Throwable {
        // This code is entirely nonsense. It is just to exercise
        // signature polymorphic methods in dx.
        VarHandleDexTest t = new VarHandleDexTest();
        {
            VarHandle vb = MethodHandles.lookup().findStaticVarHandle(t.getClass(), "bsValue", boolean.class);
            boolean newValue = true;
            boolean expectedValue = false;

            boolean b0 = (boolean) vb.compareAndExchangeAcquire(t, expectedValue, newValue);
            vb.compareAndExchangeAcquire(t, expectedValue, newValue);
            boolean b1 = (boolean) vb.compareAndExchange(t, expectedValue, newValue);
            vb.compareAndExchange(t, expectedValue, newValue);
            boolean b2 = (boolean) vb.compareAndExchangeRelease(t, expectedValue, newValue);
            vb.compareAndExchangeRelease(t, expectedValue, newValue);

            boolean r0 = vb.compareAndSet(t, expectedValue, newValue);
            vb.compareAndSet(t, expectedValue, newValue);
            boolean r1 = vb.weakCompareAndSetAcquire(t, expectedValue, newValue);
            vb.weakCompareAndSetAcquire(t, expectedValue, newValue);
            boolean r2 = vb.weakCompareAndSet(t, expectedValue, newValue);
            vb.weakCompareAndSet(t, expectedValue, newValue);
            boolean r3 = vb.weakCompareAndSetPlain(t, expectedValue, newValue);
            vb.weakCompareAndSetPlain(t, expectedValue, newValue);
            boolean r4 = vb.weakCompareAndSetRelease(t, expectedValue, newValue);
            vb.weakCompareAndSetRelease(t, expectedValue, newValue);

            boolean b3 = (boolean) vb.getAndAddAcquire(t, expectedValue, newValue);
            vb.getAndAddAcquire(t, expectedValue, newValue);
            boolean b4 = (boolean) vb.getAndAdd(t, expectedValue, newValue);
            vb.getAndAdd(t, expectedValue, newValue);
            boolean b5 = (boolean) vb.getAndAddRelease(t, expectedValue, newValue);
            vb.getAndAddRelease(t, expectedValue, newValue);
            boolean b6 = (boolean) vb.getAndBitwiseAndAcquire(t, expectedValue, newValue);
            vb.getAndBitwiseAndAcquire(t, expectedValue, newValue);
            boolean b7 = (boolean) vb.getAndBitwiseAnd(t, expectedValue, newValue);
            vb.getAndBitwiseAnd(t, expectedValue, newValue);
            boolean b8 = (boolean) vb.getAndBitwiseAndRelease(t, expectedValue, newValue);
            vb.getAndBitwiseAndRelease(t, expectedValue, newValue);
            boolean b9 = (boolean) vb.getAndBitwiseOrAcquire(t, expectedValue, newValue);
            vb.getAndBitwiseOrAcquire(t, expectedValue, newValue);
            boolean b10 = (boolean) vb.getAndBitwiseOr(t, expectedValue, newValue);
            vb.getAndBitwiseOr(t, expectedValue, newValue);
            boolean b11 = (boolean) vb.getAndBitwiseOrRelease(t, expectedValue, newValue);
            vb.getAndBitwiseOrRelease(t, expectedValue, newValue);
            boolean b12 = (boolean) vb.getAndBitwiseXorAcquire(t, expectedValue, newValue);
            vb.getAndBitwiseXorAcquire(t, expectedValue, newValue);
            boolean b13 = (boolean) vb.getAndBitwiseXor(t, expectedValue, newValue);
            vb.getAndBitwiseXor(t, expectedValue, newValue);
            boolean b14 = (boolean) vb.getAndBitwiseXorRelease(t, expectedValue, newValue);
            vb.getAndBitwiseXorRelease(t, expectedValue, newValue);

            boolean b15 = (boolean) vb.getAndSetAcquire(t, newValue);
            vb.getAndSetAcquire(t, newValue);
            boolean b16 = (boolean) vb.getAndSet(t, newValue);
            vb.getAndSet(t, newValue);
            boolean b17 = (boolean) vb.getAndSetRelease(t, newValue);
            vb.getAndSetRelease(t, newValue);

            boolean b18 = (boolean) vb.get(t);
            vb.get(t);
            boolean b19 = (boolean) vb.getAcquire(t);
            vb.getAcquire(t);
            boolean b20 = (boolean) vb.getOpaque(t);
            vb.getOpaque(t);
            boolean b21 = (boolean) vb.getVolatile(t);
            vb.getVolatile(t);

            vb.set(t, newValue);
            vb.setOpaque(t, newValue);
            vb.setRelease(t, newValue);
            vb.setVolatile(t, newValue);
        }
        {
            VarHandle vf = MethodHandles.lookup().findStaticVarHandle(t.getClass(), "fValue", Float.class);
            Float newValue = Float.valueOf(1.1f);
            Float expectedValue = Float.valueOf(2.2e-6f);

            Float f0 = (Float) vf.compareAndExchangeAcquire(t, expectedValue, newValue);
            vf.compareAndExchangeAcquire(t, expectedValue, newValue);
            Float f1 = (Float) vf.compareAndExchange(t, expectedValue, newValue);
            vf.compareAndExchange(t, expectedValue, newValue);
            Float f2 = (Float) vf.compareAndExchangeRelease(t, expectedValue, newValue);
            vf.compareAndExchangeRelease(t, expectedValue, newValue);

            boolean r0 = vf.compareAndSet(t, expectedValue, newValue);
            vf.compareAndSet(t, expectedValue, newValue);
            boolean r1 = vf.weakCompareAndSetAcquire(t, expectedValue, newValue);
            vf.weakCompareAndSetAcquire(t, expectedValue, newValue);
            boolean r2 = vf.weakCompareAndSet(t, expectedValue, newValue);
            vf.weakCompareAndSet(t, expectedValue, newValue);
            boolean r3 = vf.weakCompareAndSetPlain(t, expectedValue, newValue);
            vf.weakCompareAndSetPlain(t, expectedValue, newValue);
            boolean r4 = vf.weakCompareAndSetRelease(t, expectedValue, newValue);
            vf.weakCompareAndSetRelease(t, expectedValue, newValue);

            Float f3 = (Float) vf.getAndAddAcquire(t, expectedValue, newValue);
            vf.getAndAddAcquire(t, expectedValue, newValue);
            Float f4 = (Float) vf.getAndAdd(t, expectedValue, newValue);
            vf.getAndAdd(t, expectedValue, newValue);
            Float f5 = (Float) vf.getAndAddRelease(t, expectedValue, newValue);
            vf.getAndAddRelease(t, expectedValue, newValue);
            Float f6 = (Float) vf.getAndBitwiseAndAcquire(t, expectedValue, newValue);
            vf.getAndBitwiseAndAcquire(t, expectedValue, newValue);
            Float f7 = (Float) vf.getAndBitwiseAnd(t, expectedValue, newValue);
            vf.getAndBitwiseAnd(t, expectedValue, newValue);
            Float f8 = (Float) vf.getAndBitwiseAndRelease(t, expectedValue, newValue);
            vf.getAndBitwiseAndRelease(t, expectedValue, newValue);
            Float f9 = (Float) vf.getAndBitwiseOrAcquire(t, expectedValue, newValue);
            vf.getAndBitwiseOrAcquire(t, expectedValue, newValue);
            Float f10 = (Float) vf.getAndBitwiseOr(t, expectedValue, newValue);
            vf.getAndBitwiseOr(t, expectedValue, newValue);
            Float f11 = (Float) vf.getAndBitwiseOrRelease(t, expectedValue, newValue);
            vf.getAndBitwiseOrRelease(t, expectedValue, newValue);
            Float f12 = (Float) vf.getAndBitwiseXorAcquire(t, expectedValue, newValue);
            vf.getAndBitwiseXorAcquire(t, expectedValue, newValue);
            Float f13 = (Float) vf.getAndBitwiseXor(t, expectedValue, newValue);
            vf.getAndBitwiseXor(t, expectedValue, newValue);
            Float f14 = (Float) vf.getAndBitwiseXorRelease(t, expectedValue, newValue);
            vf.getAndBitwiseXorRelease(t, expectedValue, newValue);

            Float f15 = (Float) vf.getAndSetAcquire(t, newValue);
            vf.getAndSetAcquire(t, newValue);
            Float f16 = (Float) vf.getAndSet(t, newValue);
            vf.getAndSet(t, newValue);
            Float f17 = (Float) vf.getAndSetRelease(t, newValue);
            vf.getAndSetRelease(t, newValue);

            Float f18 = (Float) vf.get(t);
            vf.get(t);
            Float f19 = (Float) vf.getAcquire(t);
            vf.getAcquire(t);
            Float f20 = (Float) vf.getOpaque(t);
            vf.getOpaque(t);
            Float f21 = (Float) vf.getVolatile(t);
            vf.getVolatile(t);

            vf.set(t, newValue);
            vf.setOpaque(t, newValue);
            vf.setRelease(t, newValue);
            vf.setVolatile(t, newValue);
        }
        {
            String[] words = { "okay", "stevie", "bring", "your", "three", "friends", "up" };
            VarHandle vw = MethodHandles.arrayElementVarHandle(words.getClass());
            String newValue = "four";
            String expectedValue = "three";
            int index = 4;

            String s0 = (String) vw.compareAndExchangeAcquire(words, index, expectedValue, newValue);
            vw.compareAndExchangeAcquire(words, index, expectedValue, newValue);
            String s1 = (String) vw.compareAndExchange(words, index, expectedValue, newValue);
            vw.compareAndExchange(words, index, expectedValue, newValue);
            String s2 = (String) vw.compareAndExchangeRelease(words, index, expectedValue, newValue);
            vw.compareAndExchangeRelease(words, index, expectedValue, newValue);

            boolean r0 = vw.compareAndSet(words, index, expectedValue, newValue);
            vw.compareAndSet(words, index, expectedValue, newValue);
            boolean r1 = vw.weakCompareAndSetAcquire(words, index, expectedValue, newValue);
            vw.weakCompareAndSetAcquire(words, index, expectedValue, newValue);
            boolean r2 = vw.weakCompareAndSet(words, index, expectedValue, newValue);
            vw.weakCompareAndSet(words, index, expectedValue, newValue);
            boolean r3 = vw.weakCompareAndSetPlain(words, index, expectedValue, newValue);
            vw.weakCompareAndSetPlain(words, index, expectedValue, newValue);
            boolean r4 = vw.weakCompareAndSetRelease(words, index, expectedValue, newValue);
            vw.weakCompareAndSetRelease(words, index, expectedValue, newValue);

            String s3 = (String) vw.getAndAddAcquire(words, index, expectedValue, newValue);
            vw.getAndAddAcquire(words, index, expectedValue, newValue);
            String s4 = (String) vw.getAndAdd(words, index, expectedValue, newValue);
            vw.getAndAdd(words, index, expectedValue, newValue);
            String s5 = (String) vw.getAndAddRelease(words, index, expectedValue, newValue);
            vw.getAndAddRelease(words, index, expectedValue, newValue);
            String s6 = (String) vw.getAndBitwiseAndAcquire(words, index, expectedValue, newValue);
            vw.getAndBitwiseAndAcquire(words, index, expectedValue, newValue);
            String s7 = (String) vw.getAndBitwiseAnd(words, index, expectedValue, newValue);
            vw.getAndBitwiseAnd(words, index, expectedValue, newValue);
            String s8 = (String) vw.getAndBitwiseAndRelease(words, index, expectedValue, newValue);
            vw.getAndBitwiseAndRelease(words, index, expectedValue, newValue);
            String s9 = (String) vw.getAndBitwiseOrAcquire(words, index, expectedValue, newValue);
            vw.getAndBitwiseOrAcquire(words, index, expectedValue, newValue);
            String s10 = (String) vw.getAndBitwiseOr(words, index, expectedValue, newValue);
            vw.getAndBitwiseOr(words, index, expectedValue, newValue);
            String s11 = (String) vw.getAndBitwiseOrRelease(words, index, expectedValue, newValue);
            vw.getAndBitwiseOrRelease(words, index, expectedValue, newValue);
            String s12 = (String) vw.getAndBitwiseXorAcquire(words, index, expectedValue, newValue);
            vw.getAndBitwiseXorAcquire(words, index, expectedValue, newValue);
            String s13 = (String) vw.getAndBitwiseXor(words, index, expectedValue, newValue);
            vw.getAndBitwiseXor(words, index, expectedValue, newValue);
            String s14 = (String) vw.getAndBitwiseXorRelease(words, index, expectedValue, newValue);
            vw.getAndBitwiseXorRelease(words, index, expectedValue, newValue);

            String s15 = (String) vw.getAndSetAcquire(words, index, newValue);
            vw.getAndSetAcquire(words, index, newValue);
            String s16 = (String) vw.getAndSet(words, index, newValue);
            vw.getAndSet(words, index, newValue);
            String s17 = (String) vw.getAndSetRelease(words, index, newValue);
            vw.getAndSetRelease(words, index, newValue);

            String s18 = (String) vw.get(words, index);
            vw.get(words, index);
            String s19 = (String) vw.getAcquire(words, index);
            vw.getAcquire(words, index);
            String s20 = (String) vw.getOpaque(words, index);
            vw.getOpaque(words, index);
            String s21 = (String) vw.getVolatile(words, index);
            vw.getVolatile(words, index);

            vw.set(words, index, newValue);
            vw.setOpaque(words, index, newValue);
            vw.setRelease(words, index, newValue);
            vw.setVolatile(words, index, newValue);
        }
    }
}
