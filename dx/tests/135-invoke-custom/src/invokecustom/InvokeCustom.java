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

package invokecustom;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class Super {
  public void targetMethodTest5() {
    System.out.println("targetMethodTest5 from Super");
  }
}

public class InvokeCustom extends Super {

  private static void targetMethodTest1() {
    System.out.println("Hello World!");
  }

  private static void targetMethodTest2(boolean z, byte b, char c, short s, int i, float f, long l,
      double d, String str) {
    System.out.println(z);
    System.out.println(b);
    System.out.println(c);
    System.out.println(s);
    System.out.println(i);
    System.out.println(f);
    System.out.println(l);
    System.out.println(d);
    System.out.println(str);
  }

  private static void targetMethodTest3() {
  }

  public static CallSite bsmLookupStatic(MethodHandles.Lookup caller, String name, MethodType type)
      throws NoSuchMethodException, IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final MethodHandle targetMH = lookup.findStatic(lookup.lookupClass(), name, type);
    return new ConstantCallSite(targetMH.asType(type));
  }

  public static CallSite bsmLookupStaticWithExtraArgs(
      MethodHandles.Lookup caller, String name, MethodType type, int i, long l, float f, double d)
      throws NoSuchMethodException, IllegalAccessException {
    System.out.println(i);
    System.out.println(l);
    System.out.println(f);
    System.out.println(d);
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final MethodHandle targetMH = lookup.findStatic(lookup.lookupClass(), name, type);
    return new ConstantCallSite(targetMH.asType(type));
  }

  @Override
  public void targetMethodTest5() {
    System.out.println("targetMethodTest5 from InvokeCustom");
  }

  private static void targetMethodTest4() {
    System.out.println("targetMethodTest4");
  }

  public static CallSite bsmCreateCallSite(
      MethodHandles.Lookup caller, String name, MethodType type, MethodHandle mh)
      throws Throwable {
    System.out.println(mh.toString());
    return new ConstantCallSite(mh);
  }
}
