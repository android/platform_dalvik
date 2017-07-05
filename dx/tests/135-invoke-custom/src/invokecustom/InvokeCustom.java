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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

abstract class Super {
  public void targetMethodTest4() {
    System.out.println("targetMethodTest4 from Super");
  }

  public abstract void helperMethodTest9();
}

public class InvokeCustom extends Super implements Runnable {

  public InvokeCustom() {}
  public InvokeCustom(int i) {
    System.out.println("InvokeCustom.<init>(" + i + ")");
  }

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
    System.out.println("targetMethodTest3 from InvokeCustom");
  }

  @Override
  public void targetMethodTest4() {
    // The generated code should be calling Super.targetMethodTest4.
    System.out.println("targetMethodTest4 from InvokeCustom (oops!)");
  }

  public static int targetMethodTest5(int x, int y, int total) {
    int calculated = x + y;
    System.out.println("targetMethodTest5 " + x + " + " + y + " = " + calculated);
    if (calculated != total) {
        System.out.println("Failed " + calculated + " != " + total);
    }
    return calculated;
  }

  public static long targetMethodTest6(long x, long y, long total) {
    long calculated = x + y;
    System.out.println("targetMethodTest6 " + x + " + " + y + " = " + calculated);
    if (calculated != total) {
        System.out.println("Failed " + calculated + " != " + total);
    }
    return calculated;
  }

  public static double targetMethodTest7(float x, float y, double product) {
    double calculated = x * y;
    System.out.println("targetMethodTest7 " + x + " * " + y + " = " + calculated);
    if (calculated != product) {
      System.out.println("Failed " + calculated + " != " + product);
    }
    return calculated;
  }

  public static void targetMethodTest8(String s) {
    System.out.println("targetMethodTest8 " + s);
  }

  private static int staticFieldTest9 = 0;

  private static void checkStaticFieldTest9(MethodHandle getter, MethodHandle setter)
      throws Throwable {
    final int NEW_VALUE = 0x76543210;
    int oldValue = (int) getter.invokeExact();
    setter.invokeExact(NEW_VALUE);
    int newValue = (int) getter.invokeExact();
    System.out.print("checkStaticFieldTest9: old " + oldValue + " new " + newValue +
                     " expected " + NEW_VALUE + " ");
    System.out.println((newValue == NEW_VALUE) ? "OK" : "ERROR");
  }

  private float fieldTest9 = 0.0f;

  private void checkFieldTest9(MethodHandle getter, MethodHandle setter)
      throws Throwable {
    final float NEW_VALUE = 1.99e-19f;
    float oldValue = (float) getter.invokeExact(this);
    setter.invokeExact(this, NEW_VALUE);
    float newValue = (float) getter.invokeExact(this);
    System.out.print("checkFieldTest9: old " + oldValue + " new " + newValue +
                     " expected " + NEW_VALUE + " ");
    System.out.println((newValue == NEW_VALUE) ? "OK" : "ERROR");
  }

  public void helperMethodTest9() {
    System.out.println("helperMethodTest9 in " + InvokeCustom.class);
  }

  private static void targetMethodTest9() {
    System.out.println("targetMethodTest9()");
  }

  public void run() {
    System.out.println("run() for Test9");
  }

  public static CallSite bsmLookupStatic(MethodHandles.Lookup caller, String name, MethodType type)
      throws NoSuchMethodException, IllegalAccessException {
    System.out.println("bsmLookupStatic []");
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final MethodHandle targetMH = lookup.findStatic(lookup.lookupClass(), name, type);
    return new ConstantCallSite(targetMH.asType(type));
  }

  public static CallSite bsmLookupStaticWithExtraArgs(
      MethodHandles.Lookup caller, String name, MethodType type, int i, long l, float f, double d)
      throws NoSuchMethodException, IllegalAccessException {
    System.out.println("bsmLookupStaticWithExtraArgs [" + i + ", " + l + ", " + f + ", " + d + "]");
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final MethodHandle targetMH = lookup.findStatic(lookup.lookupClass(), name, type);
    return new ConstantCallSite(targetMH.asType(type));
  }

  public static CallSite bsmCreateCallSite(
      MethodHandles.Lookup caller, String name, MethodType type, MethodHandle mh)
      throws Throwable {
    System.out.println("bsmCreateCallSite [" + mh + "]");
    return new ConstantCallSite(mh);
  }

  private void privateMethodTest9() {
    System.out.println("InvokeCustom.privateMethodTest9()");
  }

  public static CallSite bsmLookupTest9(MethodHandles.Lookup caller, String name, MethodType type,
                                        MethodHandle staticGetter,  MethodHandle staticSetter,
                                        MethodHandle fieldGetter, MethodHandle fieldSetter,
                                        MethodHandle instanceInvoke, MethodHandle constructor,
                                        MethodHandle interfaceInvoke, MethodHandle privateInvoke)
          throws Throwable {
    System.out.println("bsmLookupTest9 [" + staticGetter + ", " + staticSetter + ", " +
                       fieldGetter + ", " + fieldSetter + "]");
    System.out.println(name + " " + type);

    // Check constant method handles passed can be invoked.
    checkStaticFieldTest9(staticGetter, staticSetter);
    InvokeCustom instance = new InvokeCustom();
    instance.checkFieldTest9(fieldGetter, fieldSetter);

    // Check virtual method.
    instanceInvoke.invokeExact(instance);

    // InvokeCustom instance2 = (InvokeCustom) constructor.invokeExact(3);
    InvokeCustom instance2 = instance;
    interfaceInvoke.invoke(instance2);
    privateInvoke.invoke(instance2);

    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final MethodHandle targetMH = lookup.findStatic(lookup.lookupClass(), name, type);
    return new ConstantCallSite(targetMH.asType(type));
  }

  public static void lambdaTest() {
    List<String> strings = Arrays.asList(new String[] { "Three", "One", "FortyTwo" });
    String sample = strings.stream().filter(x -> "One".equals(x.trim()))
        .map(String::trim).findAny().orElse("");
    strings.stream().forEach(System.out::println);
  }
}
