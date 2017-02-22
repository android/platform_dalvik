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

import java.lang.invoke.MethodHandle;

public class TestInvokePolymorphic {
    public static void testInvokeVoidReturnNoArgs(MethodHandle mh) throws Throwable {
        mh.invoke();
    }

    public static void testInvokeExactVoidReturnNoArgs(MethodHandle mh) throws Throwable {
        mh.invokeExact();
    }

    public static int testInvokeIntReturnNoArgs(MethodHandle mh) throws Throwable {
        return (int) mh.invoke();
    }

    public static int testInvokeExactIntReturnNoArgs(MethodHandle mh) throws Throwable {
        return (int) mh.invokeExact();
    }

    public static long testInvokeLongReturnNoArgs(MethodHandle mh) throws Throwable {
        return (long) mh.invoke();
    }

    public static long testInvokeExactLongReturnNoArgs(MethodHandle mh) throws Throwable {
        return (long) mh.invokeExact();
    }

    public static double testInvokeDoubleReturnNoArgs(MethodHandle mh) throws Throwable {
        return (double) mh.invoke();
    }

    public static double testInvokeExactDoubleReturnNoArgs(MethodHandle mh) throws Throwable {
        return (double) mh.invokeExact();
    }

    public static double testInvokeDoubleReturn2Arguments(MethodHandle mh, Object o, long l)
            throws Throwable {
        return (double) mh.invoke(o, l);
    }

    public static double testInvokeExactDoubleReturn2Arguments(MethodHandle mh, Object o, long l)
            throws Throwable {
        return (double) mh.invokeExact(o, l);
    }

    public static void testInvokeVoidReturn3IntArguments(MethodHandle mh, int x, int y, int z)
            throws Throwable {
        mh.invoke( x, y, z);
    }

    public static void testInvokeExactVoidReturn3IntArguments(MethodHandle mh, int x, int y, int z)
            throws Throwable {
        mh.invokeExact(x, y, z);
    }

    public static void testInvokeVoidReturn3Arguments(MethodHandle mh, Object o, long l, double d)
            throws Throwable {
        mh.invoke(o, l, d);
    }

    public static void testInvokeExactVoidReturn3Arguments(MethodHandle mh, Object o, long l,
                                                           double d) throws Throwable {
        mh.invokeExact(o, l, d);
    }

    public static int testInvokeIntReturn5Arguments(MethodHandle mh, Object o, long l, double d,
                                                    float f, String s) throws Throwable {
        return (int) mh.invoke(o, l, d, f, s);
    }

    public static int testInvokeExactIntReturn5Arguments(MethodHandle mh, Object o, long l,
                                                         double d, float f, String s)
            throws Throwable {
        return (int) mh.invokeExact(o, l, d, f, s);
    }
}
