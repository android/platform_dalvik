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
import java.lang.reflect.*;

public class ReturnsAndArgPassing {

  public static final String testName = "ReturnsAndArgPassing";

  static void check(boolean x) {
    if (!x) {
      throw new AssertionError(testName + " Check failed");
    }
  }

  interface MyInterface {
    void voidFoo();
    void voidBar();
    boolean booleanFoo();
    boolean booleanBar();
    byte byteFoo();
    byte byteBar();
    char charFoo();
    char charBar();
    short shortFoo();
    short shortBar();
    int intFoo();
    int intBar();
    long longFoo();
    long longBar();
    float floatFoo();
    float floatBar();
    double doubleFoo();
    double doubleBar();
    Object selectArg(int select, int a, long b, float c, double d, Object x);
  }

  static int fooInvocations = 0;
  static int barInvocations = 0;

  static class MyInvocationHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) {
      check(proxy instanceof Proxy);
      check(method.getDeclaringClass() == MyInterface.class);
      String name = method.getName();
      if (name.endsWith("Foo")) {
        check(args == null);
        fooInvocations++;
      } else if (name.endsWith("Bar")) {
        check(args == null);
        barInvocations++;
      }
      if      (name.equals("voidFoo"))    { return null; }
      else if (name.equals("voidBar"))    { return null; }
      else if (name.equals("booleanFoo")) { return true; }
      else if (name.equals("booleanBar")) { return false; }
      else if (name.equals("byteFoo"))    { return Byte.MAX_VALUE; }
      else if (name.equals("byteBar"))    { return Byte.MIN_VALUE; }
      else if (name.equals("charFoo"))    { return Character.MAX_VALUE; }
      else if (name.equals("charBar"))    { return Character.MIN_VALUE; }
      else if (name.equals("shortFoo"))   { return Short.MAX_VALUE; }
      else if (name.equals("shortBar"))   { return Short.MIN_VALUE; }
      else if (name.equals("intFoo"))     { return Integer.MAX_VALUE; }
      else if (name.equals("intBar"))     { return Integer.MIN_VALUE; }
      else if (name.equals("longFoo"))    { return Long.MAX_VALUE; }
      else if (name.equals("longBar"))    { return Long.MIN_VALUE; }
      else if (name.equals("floatFoo"))   { return Float.MAX_VALUE; }
      else if (name.equals("floatBar"))   { return Float.MIN_VALUE; }
      else if (name.equals("doubleFoo"))  { return Double.MAX_VALUE; }
      else if (name.equals("doubleBar"))  { return Double.MIN_VALUE; }
      else if (name.equals("selectArg")) {
        check(args.length == 6);
        int select = (Integer)args[0];
        return args[select];
      } else {
        throw new AssertionError("Unexpect method " + method);
      }
    }
  }

  static void testProxyReturns() {
    System.out.println(testName + ".testProxyReturns RUNNING");
    MyInvocationHandler myHandler = new MyInvocationHandler();
    MyInterface proxyMyInterface =
        (MyInterface)Proxy.newProxyInstance(ReturnsAndArgPassing.class.getClassLoader(),
                                            new Class[] { MyInterface.class },
                                            myHandler);
    check(fooInvocations == 0);
    proxyMyInterface.voidFoo();
    check(fooInvocations == 1);

    check(barInvocations == 0);
    proxyMyInterface.voidBar();
    check(barInvocations == 1);

    check(fooInvocations == 1);
    check(proxyMyInterface.booleanFoo() == true);
    check(fooInvocations == 2);

    check(barInvocations == 1);
    check(proxyMyInterface.booleanBar() == false);
    check(barInvocations == 2);

    check(fooInvocations == 2);
    check(proxyMyInterface.byteFoo() == Byte.MAX_VALUE);
    check(fooInvocations == 3);

    check(barInvocations == 2);
    check(proxyMyInterface.byteBar() == Byte.MIN_VALUE);
    check(barInvocations == 3);

    check(fooInvocations == 3);
    check(proxyMyInterface.charFoo() == Character.MAX_VALUE);
    check(fooInvocations == 4);

    check(barInvocations == 3);
    check(proxyMyInterface.charBar() == Character.MIN_VALUE);
    check(barInvocations == 4);

    check(fooInvocations == 4);
    check(proxyMyInterface.shortFoo() == Short.MAX_VALUE);
    check(fooInvocations == 5);

    check(barInvocations == 4);
    check(proxyMyInterface.shortBar() == Short.MIN_VALUE);
    check(barInvocations == 5);

    check(fooInvocations == 5);
    check(proxyMyInterface.intFoo() == Integer.MAX_VALUE);
    check(fooInvocations == 6);

    check(barInvocations == 5);
    check(proxyMyInterface.intBar() == Integer.MIN_VALUE);
    check(barInvocations == 6);

    check(fooInvocations == 6);
    check(proxyMyInterface.longFoo() == Long.MAX_VALUE);
    check(fooInvocations == 7);

    check(barInvocations == 6);
    check(proxyMyInterface.longBar() == Long.MIN_VALUE);
    check(barInvocations == 7);

    check(fooInvocations == 7);
    check(proxyMyInterface.floatFoo() == Float.MAX_VALUE);
    check(fooInvocations == 8);

    check(barInvocations == 7);
    check(proxyMyInterface.floatBar() == Float.MIN_VALUE);
    check(barInvocations == 8);

    check(fooInvocations == 8);
    check(proxyMyInterface.doubleFoo() == Double.MAX_VALUE);
    check(fooInvocations == 9);

    check(barInvocations == 8);
    check(proxyMyInterface.doubleBar() == Double.MIN_VALUE);
    check(barInvocations == 9);

    System.out.println(testName + ".testProxyReturns PASSED");
  }

  static void testProxyArgPassing() {
    System.out.println(testName + ".testProxyArgPassing RUNNING");
    MyInvocationHandler myHandler = new MyInvocationHandler();
    MyInterface proxyMyInterface =
        (MyInterface)Proxy.newProxyInstance(ReturnsAndArgPassing.class.getClassLoader(),
                                            new Class[] { MyInterface.class },
                                            myHandler);

    check((Integer)proxyMyInterface.selectArg(0, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == 0);
    check((Integer)proxyMyInterface.selectArg(1, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == Integer.MAX_VALUE);
    check((Long)proxyMyInterface.selectArg(2, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == Long.MAX_VALUE);
    check((Float)proxyMyInterface.selectArg(3, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == Float.MAX_VALUE);
    check((Double)proxyMyInterface.selectArg(4, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == Double.MAX_VALUE);
    check(proxyMyInterface.selectArg(5, Integer.MAX_VALUE, Long.MAX_VALUE,
        Float.MAX_VALUE, Double.MAX_VALUE, Object.class) == Object.class);

    System.out.println(testName + ".testProxyArgPassing PASSED");
  }

  public static void main(String args[]) {
    testProxyReturns();
    testProxyArgPassing();
  }
}
