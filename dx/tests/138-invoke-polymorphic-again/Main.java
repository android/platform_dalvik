/*
 * Copyright (C) 2016 The Android Open Source Project
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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class Main {
  public static void assertEquals(int i1, int i2) {
  }

  public int foof(int a, int b, int c, int d,
                  int e, int f, int g, int h,
                  int i, int j, int k, int l,
                  int m, int n, int o) {
    return a;
  }

  public static void $opt$BasicTest() throws Throwable {
    Main m = null;
    // OTH: >= 15 arguments to invokeExact tickles this bug. Fewer, no repro.
    assertEquals(42, -42);
    m.foof(0, 1, 2, 3,
           4, 5, 6, 7,
           8, 9, 10, 11,
           12, 13, 14);
    MethodHandle mh0 = null;
    mh0.invokeExact("bad");
  }

  public static int $opt$BasicTest2() throws Throwable {
    Main m = null;
    // OTH: >= 15 arguments to invokeExact tickles this bug. Fewer, no repro.
    assertEquals(42, -42);
    m.foof(0, 1, 2, 3,
           4, 5, 6, 7,
           8, 9, 10, 11,
           12, 13, 14);
    MethodHandle mh0 = null;
    return (int) mh0.invokeExact("bad");
  }

  public static void main(String[] args) throws Throwable {
    $opt$BasicTest();
  }
}
