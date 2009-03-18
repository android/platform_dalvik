/*
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

package org.apache.harmony.luni.tests.java.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Security;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import tests.support.resource.Support_Resources;
import dalvik.annotation.AndroidOnly;
import dalvik.annotation.BrokenTest;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@SuppressWarnings("deprecation")
@TestTargetClass(Class.class)
public class ClassTest extends junit.framework.TestCase {

    public static final String FILENAME =
        ClassTest.class.getPackage().getName().replace('.', '/') +
        "/test#.properties";

    final String packageName = getClass().getPackage().getName();
    final String classNameInitError1 = packageName + ".TestClass1";
    final String classNameInitError2 = packageName + ".TestClass1B";
    final String classNameLinkageError = packageName + ".TestClass";
    final String sourceJARfile = "illegalClasses.jar";
    final String illegalClassName = "illegalClass";

    static class StaticMember$Class {
        class Member2$A {
        }
    }

    class Member$Class {
        class Member3$B {
        }
    }

    public static class TestClass {
        @SuppressWarnings("unused")
        private int privField = 1;

        public int pubField = 2;

        private Object cValue = null;

        public Object ack = new Object();

        @SuppressWarnings("unused")
        private int privMethod() {
            return 1;
        }

        public int pubMethod() {
            return 2;
        }

        public Object cValue() {
            return cValue;
        }

        public TestClass() {
        }

        @SuppressWarnings("unused")
        private TestClass(Object o) {
        }
    }

    public static class SubTestClass extends TestClass {
    }

    interface Intf1 {
        public int field1 = 1;
        public int field2 = 1;
        void test();
    }

    interface Intf2 {
        public int field1 = 1;
        void test();
    }

    interface Intf3 extends Intf1 {
        public int field1 = 1;
    }

    interface Intf4 extends Intf1, Intf2 {
        public int field1 = 1;
        void test2(int a, Object b);
    }

    interface Intf5 extends Intf1 {
    }

    class Cls1 implements Intf2 {
        public int field1 = 2;
        public int field2 = 2;
        public void test() {
        }
    }

    class Cls2 extends Cls1 implements Intf1 {
        public int field1 = 2;
        @Override
        public void test() {
        }
    }

    class Cls3 implements Intf3, Intf4 {
        public void test() {
        }
        public void test2(int a, Object b) {
        }
    }

    static class Cls4 {

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAnnotations",
        args = {}
    )
    public void test_getAnnotations() {
      Annotation [] annotations = PublicTestClass.class.getAnnotations();
      assertEquals(1, annotations.length);
      assertEquals(TestAnnotation.class, annotations[0].annotationType());

      annotations = ExtendTestClass.class.getAnnotations();
      assertEquals(2, annotations.length);

      for(int i = 0; i < annotations.length; i++) {
          Class<? extends Annotation> type = annotations[i].annotationType();
          assertTrue("Annotation's type " + i + ": " + type,
              type.equals(Deprecated.class) ||
              type.equals(TestAnnotation.class));
      }
    }

    /**
     * @tests java.lang.Class#forName(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "java.lang.LinkageError can't be checked.",
        method = "forName",
        args = {java.lang.String.class}
    )
    @AndroidOnly("harmony specific: test with " +
            "'org.apache.harmony.luni.tests.java.lang.TestClass1'")
    public void test_forNameLjava_lang_String() throws Exception {

        assertSame("Class for name failed for java.lang.Object",
                   Object.class, Class.forName("java.lang.Object"));
        assertSame("Class for name failed for [[Ljava.lang.Object;",
                   Object[][].class, Class.forName("[[Ljava.lang.Object;"));

        assertSame("Class for name failed for [I",
                   int[].class, Class.forName("[I"));

        try {
            Class.forName("int");
            fail();
        } catch (ClassNotFoundException e) {
        }

        try {
            Class.forName("byte");
            fail();
        } catch (ClassNotFoundException e) {
        }
        try {
            Class.forName("char");
            fail();
        } catch (ClassNotFoundException e) {
        }

        try {
            Class.forName("void");
            fail();
        } catch (ClassNotFoundException e) {
        }

        try {
            Class.forName("short");
            fail();
        } catch (ClassNotFoundException e) {
        }
        try {
            Class.forName("long");
            fail();
        } catch (ClassNotFoundException e) {
        }

        try {
            Class.forName("boolean");
            fail();
        } catch (ClassNotFoundException e) {
        }
        try {
            Class.forName("float");
            fail();
        } catch (ClassNotFoundException e) {
        }
        try {
            Class.forName("double");
            fail();
        } catch (ClassNotFoundException e) {
        }

        //regression test for JIRA 2162
        try {
            Class.forName("%");
            fail("should throw ClassNotFoundException.");
        } catch (ClassNotFoundException e) {
        }

        //Regression Test for HARMONY-3332
        String securityProviderClassName;
        int count = 1;
        while ((securityProviderClassName = Security
                .getProperty("security.provider." + count++)) != null) {
            Class.forName(securityProviderClassName);
        }

        try {
            Class.forName(classNameInitError1);
            fail("ExceptionInInitializerError or ClassNotFoundException " +
                    "expected.");
        } catch (java.lang.ExceptionInInitializerError ie) {
            // Expected for the RI.
        } catch (java.lang.ClassNotFoundException ce) {
            // Expected for Android.
        }
    }

    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "",
        method = "forName",
        args = {java.lang.String.class, boolean.class, java.lang.ClassLoader.class}
    )
    public void test_forNameLjava_lang_StringLbooleanLClassLoader() throws Exception {

        ClassLoader pcl = getClass().getClassLoader();

        Class<?> [] classes = {PublicTestClass.class, ExtendTestClass.class,
                ExtendTestClass1.class, TestInterface.class, String.class};

        for(int i = 0; i < classes.length; i++) {
            Class<?> clazz = Class.forName(classes[i].getName(), true, pcl);
            assertEquals(classes[i], clazz);

            clazz = Class.forName(classes[i].getName(), false, pcl);
            assertEquals(classes[i], clazz);
        }

        for(int i = 0; i < classes.length; i++) {
            Class<?> clazz = Class.forName(classes[i].getName(), true,
                                            ClassLoader.getSystemClassLoader());
            assertEquals(classes[i], clazz);

            clazz = Class.forName(classes[i].getName(), false,
                                            ClassLoader.getSystemClassLoader());
            assertEquals(classes[i], clazz);
        }

        try  {
            Class.forName(null, true, pcl);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException  npe) {
            //expected
        }

        try {
            Class.forName("NotExistClass", true, pcl);
            fail("ClassNotFoundException is not thrown for non existent class.");
        } catch(ClassNotFoundException cnfe) {
            //expected
        }

        try {
            Class.forName("String", false, pcl);
            fail("ClassNotFoundException is not thrown for non existent class.");
        } catch(ClassNotFoundException cnfe) {
            //expected
        }

        try {
            Class.forName("org.apache.harmony.luni.tests.java.PublicTestClass",
                                                                    false, pcl);
            fail("ClassNotFoundException is not thrown for non existent class.");
        } catch(ClassNotFoundException cnfe) {
            //expected
        }
    }

    @TestTargetNew(
            level = TestLevel.SUFFICIENT,
            notes = "",
            method = "forName",
            args = {java.lang.String.class, boolean.class, java.lang.ClassLoader.class}
    )
    @AndroidOnly("Class.forName method throws ClassNotFoundException on " +
            "Android.")
    public void test_forNameLjava_lang_StringLbooleanLClassLoader_AndroidOnly() throws Exception {

        // Android doesn't support loading class files from a jar.
        try {

            URL url = getClass().getClassLoader().getResource(
                    packageName.replace(".", "/") + "/" + sourceJARfile);

            ClassLoader loader = new URLClassLoader(new URL[] { url },
                    getClass().getClassLoader());
            try {
                Class.forName(classNameLinkageError, true, loader);
                fail("LinkageError or ClassNotFoundException expected.");
            } catch (java.lang.LinkageError le) {
                // Expected for the RI.
            } catch (java.lang.ClassNotFoundException ce) {
                // Expected for Android.
            }
        } catch(Exception e) {
            fail("Unexpected exception was thrown: " + e.toString());
        }

        try {
            Class.forName(classNameInitError2,
                    true, getClass().getClassLoader());
            fail("ExceptionInInitializerError or ClassNotFoundException " +
            "should be thrown.");
        } catch (java.lang.ExceptionInInitializerError ie) {
            // Expected for the RI.
        // Remove this comment to let the test pass on Android.
        } catch (java.lang.ClassNotFoundException ce) {
            // Expected for Android.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAnnotation",
        args = {java.lang.Class.class}
    )
    public void test_getAnnotation() {
      TestAnnotation target = PublicTestClass.class.getAnnotation(TestAnnotation.class);
      assertEquals(target.value(), PublicTestClass.class.getName());

      assertNull(PublicTestClass.class.getAnnotation(Deprecated.class));

      Deprecated target2 = ExtendTestClass.class.getAnnotation(Deprecated.class);
      assertNotNull(target2);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredAnnotations",
        args = {}
    )
    public void test_getDeclaredAnnotations() {
        Annotation [] annotations = PublicTestClass.class.getDeclaredAnnotations();
        assertEquals(1, annotations.length);

        annotations = ExtendTestClass.class.getDeclaredAnnotations();
        assertEquals(2, annotations.length);

        annotations = TestInterface.class.getDeclaredAnnotations();
        assertEquals(0, annotations.length);

        annotations = String.class.getDeclaredAnnotations();
        assertEquals(0, annotations.length);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getEnclosingClass",
        args = {}
    )
    public void test_getEnclosingClass() {
        Class clazz = ExtendTestClass.class.getEnclosingClass();
        assertNull(clazz);

        assertEquals(getClass(), Cls1.class.getEnclosingClass());
        assertEquals(getClass(), Intf1.class.getEnclosingClass());
        assertEquals(getClass(), Cls4.class.getEnclosingClass());
    }


    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getEnclosingMethod",
        args = {}
    )
    public void test_getEnclosingMethod() {
        Method clazz = ExtendTestClass.class.getEnclosingMethod();
        assertNull(clazz);

        PublicTestClass ptc = new PublicTestClass();
        try {
            assertEquals("getEnclosingMethod returns incorrect method.",
                    PublicTestClass.class.getMethod("getLocalClass",
                            (Class []) null),
                    ptc.getLocalClass().getClass().getEnclosingMethod());
        } catch(NoSuchMethodException nsme) {
            fail("NoSuchMethodException was thrown.");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getEnclosingConstructor",
        args = {}
    )
    public void test_getEnclosingConstructor() {

        PublicTestClass ptc = new PublicTestClass();

        assertEquals("getEnclosingConstructor method returns incorrect class.",
                PublicTestClass.class.getConstructors()[0],
                ptc.clazz.getClass().getEnclosingConstructor());

        assertNull("getEnclosingConstructor should return null for local " +
                "class declared in method.",
                ptc.getLocalClass().getClass().getEnclosingConstructor());

        assertNull("getEnclosingConstructor should return null for local " +
                "class declared in method.",
                ExtendTestClass.class.getEnclosingConstructor());
    }


    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getEnumConstants",
        args = {}
    )
    public void test_getEnumConstants() {
        Object [] clazz = ExtendTestClass.class.getEnumConstants();
        assertNull(clazz);
        Object [] constants = TestEnum.class.getEnumConstants();
        assertEquals(TestEnum.values().length, constants.length);
        for(int i = 0; i < constants.length; i++) {
            assertEquals(TestEnum.values()[i], constants[i]);
        }
        assertEquals(0, TestEmptyEnum.class.getEnumConstants().length);
    }
    public enum TestEnum {
        ONE, TWO, THREE
    }
    public enum TestEmptyEnum {
    }
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "GenericSignatureFormatError, TypeNotPresentException, " +
                "MalformedParameterizedTypeException are not verified.",
        method = "getGenericInterfaces",
        args = {}
    )
    public void test_getGenericInterfaces() {
        Type [] types = ExtendTestClass1.class.getGenericInterfaces();
        assertEquals(0, types.length);

        Class [] interfaces = {TestInterface.class, Serializable.class,
                               Cloneable.class};
        types = PublicTestClass.class.getGenericInterfaces();
        assertEquals(interfaces.length, types.length);
        for(int i = 0; i < types.length; i++) {
            assertEquals(interfaces[i], types[i]);
        }

        types = TestInterface.class.getGenericInterfaces();
        assertEquals(0, types.length);

        types = List.class.getGenericInterfaces();
        assertEquals(1, types.length);
        assertEquals(Collection.class, ((ParameterizedType)types[0]).getRawType());

        assertEquals(0, int.class.getGenericInterfaces().length);
        assertEquals(0, void.class.getGenericInterfaces().length);
    }

    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "GenericSignatureFormatError, TypeNotPresentException, MalformedParameterizedTypeException are not verified.",
        method = "getGenericSuperclass",
        args = {}
    )
    public void test_getGenericSuperclass () {
        assertEquals(PublicTestClass.class,
                                  ExtendTestClass.class.getGenericSuperclass());
        assertEquals(ExtendTestClass.class,
                ExtendTestClass1.class.getGenericSuperclass());
        assertEquals(Object.class, PublicTestClass.class.getGenericSuperclass());
        assertEquals(Object.class, String.class.getGenericSuperclass());
        assertEquals(null, TestInterface.class.getGenericSuperclass());

        ParameterizedType type = (ParameterizedType) Vector.class.getGenericSuperclass();
        assertEquals(AbstractList.class, type.getRawType());
    }

    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        method = "getPackage",
        args = {}
    )
    @AndroidOnly("Uses dalvik.system.PathClassLoader.")
    public void test_getPackage() {

      Package thisPackage = getClass().getPackage();
      assertEquals("org.apache.harmony.luni.tests.java.lang",
                      thisPackage.getName());

      Package stringPackage = String.class.getPackage();
      assertNotNull("java.lang", stringPackage.getName());

      String hyts_package_name = "hyts_package_dex.jar";
      File resources = Support_Resources.createTempFolder();
      Support_Resources.copyFile(resources, "Package", hyts_package_name);

      String resPath = resources.toString();
      if (resPath.charAt(0) == '/' || resPath.charAt(0) == '\\')
          resPath = resPath.substring(1);

      try {

          URL resourceURL = new URL("file:/" + resPath + "/Package/"
                  + hyts_package_name);

          ClassLoader cl =  new dalvik.system.PathClassLoader(
                  resourceURL.getPath(), getClass().getClassLoader());

          Class clazz = cl.loadClass("C");
          assertNull("getPackage for C.class should return null",
                  clazz.getPackage());

          clazz = cl.loadClass("a.b.C");
          Package cPackage = clazz.getPackage();
          assertNotNull("getPackage for a.b.C.class should not return null",
                  cPackage);

        /*
         * URLClassLoader doesn't work on Android for jar files
         *
         * URL url = getClass().getClassLoader().getResource(
         *         packageName.replace(".", "/") + "/" + sourceJARfile);
         *
         * ClassLoader loader = new URLClassLoader(new URL[] { url }, null);
         *
         * try {
         *     Class<?> clazz = loader.loadClass(illegalClassName);
         *     Package pack = clazz.getPackage();
         *     assertNull(pack);
         * } catch(ClassNotFoundException cne) {
         *     fail("ClassNotFoundException was thrown for " + illegalClassName);
         * }
        */
      } catch(Exception e) {
          fail("Unexpected exception was thrown: " + e.toString());
      }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getProtectionDomain",
        args = {}
    )
    @BrokenTest("There is no protection domain set in Android.")
    public void test_getProtectionDomain() {
        ProtectionDomain pd = PublicTestClass.class.getProtectionDomain();
        assertNotNull("Test 1: Protection domain expected to be set.", pd);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if (perm.getName().equals("getProtectionDomain")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            PublicTestClass.class.getProtectionDomain();
            fail("Test 2: SecurityException expected.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "",
        method = "getSigners",
        args = {}
    )
    public void test_getSigners() {
        assertNull(void.class.getSigners());
        assertNull(PublicTestClass.class.getSigners());

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSimpleName",
        args = {}
    )
    public void test_getSimpleName() {
        assertEquals("PublicTestClass", PublicTestClass.class.getSimpleName());
        assertEquals("void", void.class.getSimpleName());
        assertEquals("int[]", int[].class.getSimpleName());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getTypeParameters",
        args = {}
    )
    public void test_getTypeParameters() {
        assertEquals(0, PublicTestClass.class.getTypeParameters().length);
        TypeVariable [] tv = TempTestClass1.class.getTypeParameters();
        assertEquals(1, tv.length);
        assertEquals(Object.class, tv[0].getBounds()[0]);

        TempTestClass2<String> tc = new TempTestClass2<String>();
        tv = tc.getClass().getTypeParameters();
        assertEquals(1, tv.length);
        assertEquals(String.class, tv[0].getBounds()[0]);
    }

    class TempTestClass1<T> {
    }

    class TempTestClass2<S extends String> extends TempTestClass1<S> {
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAnnotation",
        args = {}
    )
    public void test_isAnnotation() {
        assertTrue(Deprecated.class.isAnnotation());
        assertTrue(TestAnnotation.class.isAnnotation());
        assertFalse(PublicTestClass.class.isAnnotation());
        assertFalse(String.class.isAnnotation());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAnnotationPresent",
        args = {java.lang.Class.class}
    )
     public void test_isAnnotationPresent() {
        assertTrue(PublicTestClass.class.isAnnotationPresent(TestAnnotation.class));
        assertFalse(ExtendTestClass1.class.isAnnotationPresent(TestAnnotation.class));
        assertFalse(String.class.isAnnotationPresent(Deprecated.class));
        assertTrue(ExtendTestClass.class.isAnnotationPresent(TestAnnotation.class));
        assertTrue(ExtendTestClass.class.isAnnotationPresent(Deprecated.class));
     }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAnonymousClass",
        args = {}
    )
    public void test_isAnonymousClass() {
        assertFalse(PublicTestClass.class.isAnonymousClass());
        assertTrue((new Thread() {}).getClass().isAnonymousClass());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isEnum",
        args = {}
    )
    public void test_isEnum() {
      assertFalse(PublicTestClass.class.isEnum());
      assertFalse(ExtendTestClass.class.isEnum());
      assertTrue(TestEnum.ONE.getClass().isEnum());
      assertTrue(TestEnum.class.isEnum());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isLocalClass",
        args = {}
    )
    public void test_isLocalClass() {
        assertFalse(ExtendTestClass.class.isLocalClass());
        assertFalse(TestInterface.class.isLocalClass());
        assertFalse(TestEnum.class.isLocalClass());
        class InternalClass {}
        assertTrue(InternalClass.class.isLocalClass());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isMemberClass",
        args = {}
    )
    public void test_isMemberClass() {
        assertFalse(ExtendTestClass.class.isMemberClass());
        assertFalse(TestInterface.class.isMemberClass());
        assertFalse(String.class.isMemberClass());
        assertTrue(TestEnum.class.isMemberClass());
        assertTrue(StaticMember$Class.class.isMemberClass());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isSynthetic",
        args = {}
    )
    public void test_isSynthetic() {
      assertFalse("Returned true for non synthetic class.",
              ExtendTestClass.class.isSynthetic());
      assertFalse("Returned true for non synthetic class.",
              TestInterface.class.isSynthetic());
      assertFalse("Returned true for non synthetic class.",
              String.class.isSynthetic());

      String className = "org.apache.harmony.luni.tests.java.lang.ClassLoaderTest$1";

      /*
       *try {
       *   assertTrue("Returned false for synthetic class.",
       *           getClass().getClassLoader().loadClass(className).
       *           isSynthetic());
       *} catch(ClassNotFoundException cnfe) {
       *   fail("Class " + className + " can't be found.");
       *}
       */

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isInstance",
        args = {java.lang.Object.class}
    )
    public void test_isInstance() {
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getCanonicalName",
        args = {}
    )
    public void test_getCanonicalName() {
        String name = int[].class.getCanonicalName();
        Class [] classArray = { int.class, int[].class, String.class,
                                PublicTestClass.class, TestInterface.class,
                                ExtendTestClass.class };
        String [] classNames = {"int", "int[]", "java.lang.String",
                      "org.apache.harmony.luni.tests.java.lang.PublicTestClass",
                        "org.apache.harmony.luni.tests.java.lang.TestInterface",
                     "org.apache.harmony.luni.tests.java.lang.ExtendTestClass"};

        for(int i = 0; i < classArray.length; i++) {
            assertEquals(classNames[i], classArray[i].getCanonicalName());
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getClassLoader",
        args = {}
    )
    public void test_getClassLoader() {

        assertEquals(ExtendTestClass.class.getClassLoader(),
                         PublicTestClass.class.getClassLoader());

        assertNull(int.class.getClassLoader());
        assertNull(void.class.getClassLoader());

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if ((perm instanceof RuntimePermission) &&
                        perm.getName().equals("getClassLoader")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            System.class.getClassLoader();
        } catch (SecurityException e) {
            fail("SecurityException should not be thrown.");
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getClasses()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getClasses",
        args = {}
    )
    public void test_getClasses() {
        assertEquals("Incorrect class array returned",
                     4, ClassTest.class.getClasses().length);
    }

    /**
     * @tests java.lang.Class#getClasses()
     */
    @TestTargetNew(
        level = TestLevel.NOT_FEASIBLE,
        method = "getClasses",
        args = {}
    )
    @BrokenTest("Class.forName does not work with an URLClassLoader; " +
            "the VMClassLoader does not support loading classes from a " +
            "(jar) byte array.")
    public void test_getClasses_subtest0() {
        final Permission privCheckPermission = new BasicPermission("Privilege check") {
            private static final long serialVersionUID = 1L;
        };

        class MyCombiner implements DomainCombiner {
            boolean combine;

            public ProtectionDomain[] combine(ProtectionDomain[] executionDomains,
                    ProtectionDomain[] parentDomains) {
                combine = true;
                return new ProtectionDomain[0];
            }

            private boolean recurring = false;

            public boolean isPriviledged() {
                if (recurring) {
                    return true;
                }
                try {
                    recurring = true;
                    combine = false;
                    try {
                        AccessController.checkPermission(privCheckPermission);
                    } catch (SecurityException e) {}
                    return !combine;
                } finally {
                    recurring = false;
                }
            }
        }

        final MyCombiner combiner = new MyCombiner();
        class SecurityManagerCheck extends SecurityManager {
            String reason;

            Class<?> checkClass;

            int checkType;

            int checkPermission;

            int checkMemberAccess;

            int checkPackageAccess;

            public void setExpected(String reason, Class<?> cls, int type) {
                this.reason = reason;
                checkClass = cls;
                checkType = type;
                checkPermission = 0;
                checkMemberAccess = 0;
                checkPackageAccess = 0;
            }

            @Override
            public void checkPermission(Permission perm) {
                if (combiner.isPriviledged())
                    return;
                checkPermission++;
            }

            @Override
            public void checkMemberAccess(Class<?> cls, int type) {
                if (combiner.isPriviledged())
                    return;
                checkMemberAccess++;
                assertEquals(reason + " unexpected class", checkClass, cls);
                assertEquals(reason + "unexpected type", checkType, type);
            }

            @Override
            public void checkPackageAccess(String packageName) {
                if (combiner.isPriviledged())
                    return;
                checkPackageAccess++;
                String name = checkClass.getName();
                int index = name.lastIndexOf('.');
                String checkPackage = name.substring(0, index);
                assertEquals(reason + " unexpected package",
                             checkPackage,  packageName);
            }

            public void assertProperCalls() {
                assertEquals(reason + " unexpected checkPermission count",
                             0, checkPermission);
                assertEquals(reason + " unexpected checkMemberAccess count",
                             1, checkMemberAccess);
                assertEquals(reason + " unexpected checkPackageAccess count",
                             1, checkPackageAccess);
            }
        }

        AccessControlContext acc = new AccessControlContext(new ProtectionDomain[0]);
        AccessControlContext acc2 = new AccessControlContext(acc, combiner);

        PrivilegedAction<?> action = new PrivilegedAction<Object>() {
            public Object run() {
                File resources = Support_Resources.createTempFolder();
                try {
                    Support_Resources.copyFile(resources, null, "hyts_security.jar");
                    File file = new File(resources.toString() + "/hyts_security.jar");
                    URL url = new URL("file:" + file.getPath());
                    ClassLoader loader = new URLClassLoader(new URL[] { url }, null);
                    Class<?> cls = Class.forName("packB.SecurityTestSub", false, loader);
                    SecurityManagerCheck sm = new SecurityManagerCheck();
                    System.setSecurityManager(sm);
                    try {
                        sm.setExpected("getClasses", cls, Member.PUBLIC);
                        cls.getClasses();
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredClasses", cls, Member.DECLARED);
                        cls.getDeclaredClasses();
                        sm.assertProperCalls();

                        sm.setExpected("getConstructor", cls, Member.PUBLIC);
                        cls.getConstructor(new Class[0]);
                        sm.assertProperCalls();

                        sm.setExpected("getConstructors", cls, Member.PUBLIC);
                        cls.getConstructors();
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredConstructor", cls, Member.DECLARED);
                        cls.getDeclaredConstructor(new Class[0]);
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredConstructors", cls, Member.DECLARED);
                        cls.getDeclaredConstructors();
                        sm.assertProperCalls();

                        sm.setExpected("getField", cls, Member.PUBLIC);
                        cls.getField("publicField");
                        sm.assertProperCalls();

                        sm.setExpected("getFields", cls, Member.PUBLIC);
                        cls.getFields();
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredField", cls, Member.DECLARED);
                        cls.getDeclaredField("publicField");
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredFields", cls, Member.DECLARED);
                        cls.getDeclaredFields();
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredMethod", cls, Member.DECLARED);
                        cls.getDeclaredMethod("publicMethod", new Class[0]);
                        sm.assertProperCalls();

                        sm.setExpected("getDeclaredMethods", cls, Member.DECLARED);
                        cls.getDeclaredMethods();
                        sm.assertProperCalls();

                        sm.setExpected("getMethod", cls, Member.PUBLIC);
                        cls.getMethod("publicMethod", new Class[0]);
                        sm.assertProperCalls();

                        sm.setExpected("getMethods", cls, Member.PUBLIC);
                        cls.getMethods();
                        sm.assertProperCalls();

                        sm.setExpected("newInstance", cls, Member.PUBLIC);
                        cls.newInstance();
                        sm.assertProperCalls();
                    } finally {
                        System.setSecurityManager(null);
                    }
/* Remove this comment to let the test pass on Android.
                } catch (java.lang.ClassNotFoundException ce) {
                    // Expected for Android.
*/
                } catch (Exception e) {
                    if (e instanceof RuntimeException)
                        throw (RuntimeException) e;
                    fail("unexpected exception: " + e);
                }
                return null;
            }
        };
        AccessController.doPrivileged(action, acc2);
    }

    /**
     * @tests java.lang.Class#getComponentType()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getComponentType",
        args = {}
    )
    public void test_getComponentType() {
        assertSame("int array does not have int component type", int.class, int[].class
                .getComponentType());
        assertSame("Object array does not have Object component type", Object.class,
                Object[].class.getComponentType());
        assertNull("Object has non-null component type", Object.class.getComponentType());
    }

    /**
     * @tests java.lang.Class#getConstructor(java.lang.Class[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConstructor",
        args = {java.lang.Class[].class}
    )
    public void test_getConstructor$Ljava_lang_Class()
        throws NoSuchMethodException {
        Constructor constr = TestClass.class.getConstructor(new Class[0]);
        assertNotNull(constr);
        assertEquals("org.apache.harmony.luni.tests.java.lang.ClassTest$TestClass",
                constr.getName());
        try {
            TestClass.class.getConstructor(Object.class);
            fail("Found private constructor");
        } catch (NoSuchMethodException e) {
            // Correct - constructor with obj is private
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getConstructor(new Class[0]);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getConstructors()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getConstructors",
        args = {}
    )
    public void test_getConstructors() throws Exception {
        Constructor[] c = TestClass.class.getConstructors();
        assertEquals("Incorrect number of constructors returned", 1, c.length);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getConstructors();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredClasses()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredClasses",
        args = {}
    )
    public void test_getDeclaredClasses() {

        Class [] declClasses = Object.class.getDeclaredClasses();
        assertEquals("Incorrect length of declared classes array is returned " +
                "for Object.", 0, declClasses.length);

        declClasses = PublicTestClass.class.getDeclaredClasses();
        assertEquals(2, declClasses.length);

        assertEquals(0, int.class.getDeclaredClasses().length);
        assertEquals(0, void.class.getDeclaredClasses().length);

        for(int i = 0; i < declClasses.length; i++) {
            Constructor<?> constr = declClasses[i].getDeclaredConstructors()[0];
            constr.setAccessible(true);
            PublicTestClass publicClazz = new PublicTestClass();
            try {
                Object o = constr.newInstance(publicClazz);
                assertTrue("Returned incorrect class: " + o.toString(),
                        o.toString().startsWith("PrivateClass"));
            } catch(Exception e) {
                fail("Unexpected exception was thrown: " + e.toString());
            }
        }


        declClasses = TestInterface.class.getDeclaredClasses();
        assertEquals(0, declClasses.length);

        SecurityManager sm = new SecurityManager() {

            final String forbidenPermissionName = "user.dir";

            public void checkPermission(Permission perm) {
                if (perm.getName().equals(forbidenPermissionName)) {
                    throw new SecurityException();
                }
            }

            public void checkMemberAccess(Class<?> clazz,
                    int which) {
                if(clazz.equals(TestInterface.class)) {
                    throw new SecurityException();
                }
            }

            public void checkPackageAccess(String pkg) {
                if(pkg.equals(PublicTestClass.class.getPackage())) {
                    throw new SecurityException();
                }
            }

        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestInterface.class.getDeclaredClasses();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

    }


    /**
     * @tests java.lang.Class#getDeclaredConstructor(java.lang.Class[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredConstructor",
        args = {java.lang.Class[].class}
    )
    public void test_getDeclaredConstructor$Ljava_lang_Class() throws Exception {
        Constructor<TestClass> c = TestClass.class.getDeclaredConstructor(new Class[0]);
        assertNull("Incorrect constructor returned", c.newInstance().cValue());
        c = TestClass.class.getDeclaredConstructor(Object.class);

        try {
            TestClass.class.getDeclaredConstructor(String.class);
            fail("NoSuchMethodException should be thrown.");
        } catch(NoSuchMethodException nsme) {
            //expected
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredConstructor(Object.class);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredConstructors()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredConstructors",
        args = {}
    )
    public void test_getDeclaredConstructors() throws Exception {
        Constructor[] c = TestClass.class.getDeclaredConstructors();
        assertEquals("Incorrect number of constructors returned", 2, c.length);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredConstructors();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredField(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredField",
        args = {java.lang.String.class}
    )
    public void test_getDeclaredFieldLjava_lang_String() throws Exception {
        Field f = TestClass.class.getDeclaredField("pubField");
        assertEquals("Returned incorrect field", 2, f.getInt(new TestClass()));

        try {
            TestClass.class.getDeclaredField(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            TestClass.class.getDeclaredField("NonExistentField");
            fail("NoSuchFieldException is not thrown.");
        } catch(NoSuchFieldException nsfe) {
            //expected
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredField("pubField");
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredFields()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredFields",
        args = {}
    )
    public void test_getDeclaredFields() throws Exception {
        Field[] f = TestClass.class.getDeclaredFields();
        assertEquals("Returned incorrect number of fields", 4, f.length);
        f = SubTestClass.class.getDeclaredFields();
        // Declared fields do not include inherited
        assertEquals("Returned incorrect number of fields", 0, f.length);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredFields();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredMethod(java.lang.String,
     *        java.lang.Class[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredMethod",
        args = {java.lang.String.class, java.lang.Class[].class}
    )
    public void test_getDeclaredMethodLjava_lang_String$Ljava_lang_Class() throws Exception {
        Method m = TestClass.class.getDeclaredMethod("pubMethod", new Class[0]);
        assertEquals("Returned incorrect method", 2, ((Integer) (m.invoke(new TestClass())))
                .intValue());
        m = TestClass.class.getDeclaredMethod("privMethod", new Class[0]);

        try {
            TestClass.class.getDeclaredMethod(null, new Class[0]);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            TestClass.class.getDeclaredMethod("NonExistentMethod", new Class[0]);
            fail("NoSuchMethodException is not thrown.");
        } catch(NoSuchMethodException nsme) {
            //expected
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredMethod("pubMethod", new Class[0]);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaredMethods()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaredMethods",
        args = {}
    )
    public void test_getDeclaredMethods() throws Exception {
        Method[] m = TestClass.class.getDeclaredMethods();
        assertEquals("Returned incorrect number of methods", 3, m.length);
        m = SubTestClass.class.getDeclaredMethods();
        assertEquals("Returned incorrect number of methods", 0, m.length);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getDeclaredMethods();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getDeclaringClass()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getDeclaringClass",
        args = {}
    )
    public void test_getDeclaringClass() {
        assertEquals(ClassTest.class, TestClass.class.getDeclaringClass());
        assertNull(PublicTestClass.class.getDeclaringClass());
    }

    /**
     * @tests java.lang.Class#getField(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getField",
        args = {java.lang.String.class}
    )
    public void test_getFieldLjava_lang_String() throws Exception {
        Field f = TestClass.class.getField("pubField");
        assertEquals("Returned incorrect field", 2, f.getInt(new TestClass()));

        f = PublicTestClass.class.getField("TEST_FIELD");
        assertEquals("Returned incorrect field", "test field",
                f.get(new PublicTestClass()));

        f = PublicTestClass.class.getField("TEST_INTERFACE_FIELD");
        assertEquals("Returned incorrect field", 0,
                f.getInt(new PublicTestClass()));

        try {
            f = TestClass.class.getField("privField");
            fail("Private field access failed to throw exception");
        } catch (NoSuchFieldException e) {
            // Correct
        }

        try {
            TestClass.class.getField(null);
            fail("NullPointerException is thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

       SecurityManager sm = new SecurityManager() {

            final String forbidenPermissionName = "user.dir";

            public void checkPermission(Permission perm) {
                if (perm.getName().equals(forbidenPermissionName)) {
                    throw new SecurityException();
                }
            }

            public void checkMemberAccess(Class<?> clazz,
                    int which) {
                if(clazz.equals(TestClass.class)) {
                    throw new SecurityException();
                }
            }

            public void checkPackageAccess(String pkg) {
                if(pkg.equals(TestClass.class.getPackage())) {
                    throw new SecurityException();
                }
            }

        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getField("pubField");
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getFields()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getFields",
        args = {}
    )
    public void test_getFields2() throws Exception {
        Field[] f;
        Field expected = null;

        f = PublicTestClass.class.getFields();
        assertEquals("Test 1: Incorrect number of fields;", 2, f.length);

        f = Cls2.class.getFields();
        assertEquals("Test 2: Incorrect number of fields;", 6, f.length);

        f = Cls3.class.getFields();
        assertEquals("Test 2: Incorrect number of fields;", 5, f.length);

        for (Field field : f) {
            if (field.toString().equals("public static final int org.apache" +
                    ".harmony.luni.tests.java.lang.ClassTest$Intf3.field1")) {
                expected = field;
                break;
            }
        }
        if (expected == null) {
            fail("Test 3: getFields() did not return all fields.");
        }
        assertEquals("Test 4: Incorrect field;", expected,
                Cls3.class.getField("field1"));

        expected = null;
        for (Field field : f) {
            if(field.toString().equals("public static final int org.apache" +
                    ".harmony.luni.tests.java.lang.ClassTest$Intf1.field2")) {
                expected = field;
                break;
            }
        }
        if (expected == null) {
            fail("Test 5: getFields() did not return all fields.");
        }
        assertEquals("Test 6: Incorrect field;", expected,
                Cls3.class.getField("field2"));
    }

    /**
     * @tests java.lang.Class#getFields()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getFields",
        args = {}
    )
    public void test_getFields() throws Exception {
        Field[] f = TestClass.class.getFields();
        assertEquals("Test 1: Incorrect number of fields;", 2, f.length);
        f = SubTestClass.class.getFields();
        // Check inheritance of pub fields
        assertEquals("Test 2: Incorrect number of fields;", 2, f.length);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getFields();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        Field expected = null;
        Field[] fields = Cls2.class.getFields();
        for (Field field : fields) {
            if(field.toString().equals("public int org.apache.harmony.luni" +
                    ".tests.java.lang.ClassTest$Cls2.field1")) {
                expected = field;
                break;
            }
        }
        if (expected == null) {
            fail("getFields() did not return all fields");
        }
        assertEquals(expected, Cls2.class.getField("field1"));
    }

    /**
     * @tests java.lang.Class#getInterfaces()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInterfaces",
        args = {}
    )
    public void test_getInterfaces() {
        Class[] interfaces;
        List<?> interfaceList;
        interfaces = Object.class.getInterfaces();
        assertEquals("Incorrect interface list for Object", 0, interfaces.length);
        interfaceList = Arrays.asList(Vector.class.getInterfaces());
        assertTrue("Incorrect interface list for Vector", interfaceList
                .contains(Cloneable.class)
                && interfaceList.contains(Serializable.class)
                && interfaceList.contains(List.class));

        Class [] interfaces1 = Cls1.class.getInterfaces();
        assertEquals(1, interfaces1.length);
        assertEquals(Intf2.class, interfaces1[0]);

        Class [] interfaces2 = Cls2.class.getInterfaces();
        assertEquals(1, interfaces2.length);
        assertEquals(Intf1.class, interfaces2[0]);

        Class [] interfaces3 = Cls3.class.getInterfaces();
        assertEquals(2, interfaces3.length);
        assertEquals(Intf3.class, interfaces3[0]);
        assertEquals(Intf4.class, interfaces3[1]);

        Class [] interfaces4 = Cls4.class.getInterfaces();
        assertEquals(0, interfaces4.length);
    }

    /**
     * @tests java.lang.Class#getMethod(java.lang.String, java.lang.Class[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMethod",
        args = {java.lang.String.class, java.lang.Class[].class}
    )
    public void test_getMethodLjava_lang_String$Ljava_lang_Class() throws Exception {
        Method m = TestClass.class.getMethod("pubMethod", new Class[0]);
        assertEquals("Returned incorrect method", 2, ((Integer) (m.invoke(new TestClass())))
                .intValue());

        m = ExtendTestClass1.class.getMethod("getCount", new Class[0]);
        assertEquals("Returned incorrect method", 0, ((Integer) (m.invoke(new ExtendTestClass1())))
                .intValue());

        try {
            m = TestClass.class.getMethod("privMethod", new Class[0]);
            fail("Failed to throw exception accessing private method");
        } catch (NoSuchMethodException e) {
            // Correct
            return;
        }

        try {
            m = TestClass.class.getMethod("init", new Class[0]);
            fail("Failed to throw exception accessing to init method");
        } catch (NoSuchMethodException e) {
            // Correct
            return;
        }

        try {
            TestClass.class.getMethod("pubMethod", new Class[0]);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getMethod("pubMethod", new Class[0]);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Class#getMethods()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMethods",
        args = {}
    )
    public void test_getMethods() throws Exception {
        Method[] m = TestClass.class.getMethods();
        assertEquals("Returned incorrect number of methods",
                     2 + Object.class.getMethods().length, m.length);
        m = SubTestClass.class.getMethods();
        assertEquals("Returned incorrect number of sub-class methods",
                     2 + Object.class.getMethods().length, m.length);
        // Number of inherited methods

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            TestClass.class.getMethods();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        assertEquals("Incorrect number of methods", 10,
                Cls2.class.getMethods().length);
        assertEquals("Incorrect number of methods", 11,
                Cls3.class.getMethods().length);

        Method expected = null;
        Method[] methods = Cls2.class.getMethods();
        for (Method method : methods) {
            if(method.toString().equals("public void org.apache.harmony.luni" +
                    ".tests.java.lang.ClassTest$Cls2.test()")) {
                expected = method;
                break;
            }
        }
        if (expected == null) {
            fail("getMethods() did not return all methods");
        }
        assertEquals(expected, Cls2.class.getMethod("test"));

        expected = null;
        methods = Cls3.class.getMethods();
        for (Method method : methods) {
            if(method.toString().equals("public void org.apache.harmony.luni" +
                    ".tests.java.lang.ClassTest$Cls3.test()")) {
                expected = method;
                break;
            }
        }
        if (expected == null) {
            fail("getMethods() did not return all methods");
        }
        assertEquals(expected, Cls3.class.getMethod("test"));

        expected = null;
        methods = Cls3.class.getMethods();
        for (Method method : methods) {
            if(method.toString().equals("public void org.apache.harmony.luni" +
                    ".tests.java.lang.ClassTest$Cls3.test2(int," +
                    "java.lang.Object)")) {
                expected = method;
                break;
            }
        }
        if (expected == null) {
            fail("getMethods() did not return all methods");
        }

        assertEquals(expected, Cls3.class.getMethod("test2", int.class,
                Object.class));

        assertEquals("Incorrect number of methods", 1,
                Intf5.class.getMethods().length);
    }

    private static final class PrivateClass {
    }
    /**
     * @tests java.lang.Class#getModifiers()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getModifiers",
        args = {}
    )
    public void test_getModifiers() {
        int dcm = PrivateClass.class.getModifiers();
        assertFalse("default class is public", Modifier.isPublic(dcm));
        assertFalse("default class is protected", Modifier.isProtected(dcm));
        assertTrue("default class is not private", Modifier.isPrivate(dcm));

        int ocm = Object.class.getModifiers();
        assertTrue("public class is not public", Modifier.isPublic(ocm));
        assertFalse("public class is protected", Modifier.isProtected(ocm));
        assertFalse("public class is private", Modifier.isPrivate(ocm));
    }

    /**
     * @tests java.lang.Class#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public void test_getName() throws Exception {
        String className = Class.forName("java.lang.Object").getName();
        assertNotNull(className);

        assertEquals("Class getName printed wrong value", "java.lang.Object", className);
        assertEquals("Class getName printed wrong value", "int", int.class.getName());
        className = Class.forName("[I").getName();
        assertNotNull(className);
        assertEquals("Class getName printed wrong value", "[I", className);

        className = Class.forName("[Ljava.lang.Object;").getName();
        assertNotNull(className);

        assertEquals("Class getName printed wrong value", "[Ljava.lang.Object;", className);
    }

    /**
     * @tests java.lang.Class#getResource(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getResource",
        args = {java.lang.String.class}
    )
    public void test_getResourceLjava_lang_String() {
        final String name = "/";
        URL res = getClass().getResource(name + "HelloWorld.txt");
        assertNotNull(res);
        assertNull(getClass().getResource(
                "org/apache/harmony/luni/tests/java/lang/NonExistentResource"));
        assertNull(getClass().getResource(name + "NonExistentResource"));
    }

    /**
     * @tests java.lang.Class#getResourceAsStream(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getResourceAsStream",
        args = {java.lang.String.class}
    )
    public void test_getResourceAsStreamLjava_lang_String() throws Exception {
        String name = "/HelloWorld.txt";
        assertNotNull("the file " + name + " can not be found in this " +
                "directory", getClass().getResourceAsStream(name));

        final String nameBadURI = "org/apache/harmony/luni/tests/" +
                "test_resource.txt";
        assertNull("the file " + nameBadURI + " should not be found in this " +
                "directory",
                getClass().getResourceAsStream(nameBadURI));

        ClassLoader pcl = getClass().getClassLoader();
        Class<?> clazz = pcl.loadClass("org.apache.harmony.luni.tests.java.lang.ClassTest");
        assertNotNull(clazz.getResourceAsStream("HelloWorld1.txt"));

        try {
            getClass().getResourceAsStream(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
    }

    /**
     * @tests java.lang.Class#getSuperclass()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSuperclass",
        args = {}
    )
    public void test_getSuperclass() {
        assertNull("Object has a superclass???", Object.class.getSuperclass());
        assertSame("Normal class has bogus superclass", InputStream.class,
                FileInputStream.class.getSuperclass());
        assertSame("Array class has bogus superclass", Object.class, FileInputStream[].class
                .getSuperclass());
        assertNull("Base class has a superclass", int.class.getSuperclass());
        assertNull("Interface class has a superclass", Cloneable.class.getSuperclass());
    }

    /**
     * @tests java.lang.Class#isArray()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isArray",
        args = {}
    )
    public void test_isArray() throws ClassNotFoundException {
        assertTrue("Non-array type claims to be.", !int.class.isArray());
        Class<?> clazz = null;
        clazz = Class.forName("[I");
        assertTrue("int Array type claims not to be.", clazz.isArray());

        clazz = Class.forName("[Ljava.lang.Object;");
        assertTrue("Object Array type claims not to be.", clazz.isArray());

        clazz = Class.forName("java.lang.Object");
        assertTrue("Non-array Object type claims to be.", !clazz.isArray());
    }

    /**
     * @tests java.lang.Class#isAssignableFrom(java.lang.Class)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAssignableFrom",
        args = {java.lang.Class.class}
    )
    public void test_isAssignableFromLjava_lang_Class() {
        Class<?> clazz1 = null;
        Class<?> clazz2 = null;

        clazz1 = Object.class;
        clazz2 = Class.class;
        assertTrue("returned false for superclass",
                clazz1.isAssignableFrom(clazz2));

        clazz1 = TestClass.class;
        assertTrue("returned false for same class",
                clazz1.isAssignableFrom(clazz1));

        clazz1 = Runnable.class;
        clazz2 = Thread.class;
        assertTrue("returned false for implemented interface",
                clazz1.isAssignableFrom(clazz2));

        assertFalse("returned true not assignable classes",
                Integer.class.isAssignableFrom(String.class));

        try {
            clazz1.isAssignableFrom(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
    }

    /**
     * @tests java.lang.Class#isInterface()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isInterface",
        args = {}
    )
    public void test_isInterface() throws ClassNotFoundException {
        assertTrue("Prim type claims to be interface.", !int.class.isInterface());
        Class<?> clazz = null;
        clazz = Class.forName("[I");
        assertTrue("Prim Array type claims to be interface.", !clazz.isInterface());

        clazz = Class.forName("java.lang.Runnable");
        assertTrue("Interface type claims not to be interface.", clazz.isInterface());
        clazz = Class.forName("java.lang.Object");
        assertTrue("Object type claims to be interface.", !clazz.isInterface());

        clazz = Class.forName("[Ljava.lang.Object;");
        assertTrue("Array type claims to be interface.", !clazz.isInterface());
    }

    /**
     * @tests java.lang.Class#isPrimitive()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isPrimitive",
        args = {}
    )
    public void test_isPrimitive() {
        assertFalse("Interface type claims to be primitive.",
                Runnable.class.isPrimitive());
        assertFalse("Object type claims to be primitive.",
                Object.class.isPrimitive());
        assertFalse("Prim Array type claims to be primitive.",
                int[].class.isPrimitive());
        assertFalse("Array type claims to be primitive.",
                Object[].class.isPrimitive());
        assertTrue("Prim type claims not to be primitive.",
                int.class.isPrimitive());
        assertFalse("Object type claims to be primitive.",
                Object.class.isPrimitive());
    }

    /**
     * @tests java.lang.Class#newInstance()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "newInstance",
        args = {}
    )
    public void test_newInstance() throws Exception {
        Class<?> clazz = null;
        clazz = Class.forName("java.lang.Object");
        assertNotNull("new object instance was null", clazz.newInstance());

        clazz = Class.forName("java.lang.Throwable");
        assertSame("new Throwable instance was not a throwable",
                   clazz, clazz.newInstance().getClass());

        clazz = Class.forName("java.lang.Integer");
        try {
            clazz.newInstance();
            fail("Exception for instantiating a newInstance with no default " +
                    "                               constructor is not thrown");
        } catch (InstantiationException e) {
            // expected
        }

        try {
            TestClass3.class.newInstance();
            fail("IllegalAccessException is not thrown.");
        } catch(IllegalAccessException  iae) {
            //expected
        }

        try {
            TestClass1C.class.newInstance();
            fail("ExceptionInInitializerError should be thrown.");
        } catch (java.lang.ExceptionInInitializerError ie) {
            //expected
        }
    }

    /**
     * @tests java.lang.Class#newInstance()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "newInstance",
        args = {}
    )
    public void test_newInstance2() throws Exception {
      SecurityManager oldSm = System.getSecurityManager();
      System.setSecurityManager(sm);
      try {
          TestClass.class.newInstance();
          fail("Test 1: SecurityException expected.");
      } catch (SecurityException e) {
          // expected
      } finally {
          System.setSecurityManager(oldSm);
      }
    }

    /**
     * @tests java.lang.Class#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() throws ClassNotFoundException {
        assertEquals("Class toString printed wrong value",
                     "int", int.class.toString());
        Class<?> clazz = null;
        clazz = Class.forName("[I");
        assertEquals("Class toString printed wrong value",
                     "class [I", clazz.toString());

        clazz = Class.forName("java.lang.Object");
        assertEquals("Class toString printed wrong value",
                     "class java.lang.Object", clazz.toString());

        clazz = Class.forName("[Ljava.lang.Object;");
        assertEquals("Class toString printed wrong value",
                     "class [Ljava.lang.Object;", clazz.toString());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "getResourceAsStream",
        args = {java.lang.String.class}
    )
    // Regression Test for JIRA-2047
    public void test_getResourceAsStream_withSharpChar() throws Exception{
        InputStream in = getClass().getResourceAsStream("/" + FILENAME);
        assertNotNull(in);
        in.close();

        in = getClass().getResourceAsStream(FILENAME);
        assertNull(in);

        in = this.getClass().getClassLoader().getResourceAsStream(
                FILENAME);
        assertNotNull(in);
        in.close();
    }

    /*
     * Regression test for HARMONY-2644:
     * Load system and non-system array classes via Class.forName()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies ClassNotFoundException.",
        method = "forName",
        args = {java.lang.String.class}
    )
    public void test_forName_arrays() throws Exception {
        Class<?> c1 = getClass();
        String s = c1.getName();
        Class<?> a1 = Class.forName("[L" + s + ";");
        Class<?> a2 = Class.forName("[[L" + s + ";");
        assertSame(c1, a1.getComponentType());
        assertSame(a1, a2.getComponentType());
        Class<?> l4 = Class.forName("[[[[[J");
        assertSame(long[][][][][].class, l4);

        try{
            Class<?> clazz = Class.forName("[;");
            fail("1: " + clazz);
        } catch (ClassNotFoundException ok) {}
        try{
            Class<?> clazz = Class.forName("[[");
            fail("2:" + clazz);
        } catch (ClassNotFoundException ok) {}
        try{
            Class<?> clazz = Class.forName("[L");
            fail("3:" + clazz);
        } catch (ClassNotFoundException ok) {}
        try{
            Class<?> clazz = Class.forName("[L;");
            fail("4:" + clazz);
        } catch (ClassNotFoundException ok) {}
        try{
            Class<?> clazz = Class.forName(";");
            fail("5:" + clazz);
        } catch (ClassNotFoundException ok) {}
        try{
            Class<?> clazz = Class.forName("");
            fail("6:" + clazz);
        } catch (ClassNotFoundException ok) {}
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "asSubclass",
        args = {java.lang.Class.class}
    )
    public void test_asSubclass1() {
        assertEquals(ExtendTestClass.class,
                ExtendTestClass.class.asSubclass(PublicTestClass.class));

        assertEquals(PublicTestClass.class,
                PublicTestClass.class.asSubclass(TestInterface.class));

        assertEquals(ExtendTestClass1.class,
                ExtendTestClass1.class.asSubclass(PublicTestClass.class));

        assertEquals(PublicTestClass.class,
                PublicTestClass.class.asSubclass(PublicTestClass.class));
    }

    @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "",
            method = "asSubclass",
            args = {java.lang.Class.class}
    )
    public void test_asSubclass2() {
        try {
            PublicTestClass.class.asSubclass(ExtendTestClass.class);
            fail("Test 1: ClassCastException expected.");
        } catch(ClassCastException cce) {
            // Expected.
        }

        try {
            PublicTestClass.class.asSubclass(String.class);
            fail("Test 2: ClassCastException expected.");
        } catch(ClassCastException cce) {
            // Expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "cast",
        args = {java.lang.Object.class}
    )
    public void test_cast() {
        Object o = PublicTestClass.class.cast(new ExtendTestClass());
        assertTrue(o instanceof ExtendTestClass);

        try {
            ExtendTestClass.class.cast(new PublicTestClass());
            fail("Test 1: ClassCastException expected.");
        } catch(ClassCastException cce) {
            //expected
        }

        try {
            ExtendTestClass.class.cast(new String());
            fail("ClassCastException is not thrown.");
        } catch(ClassCastException cce) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "desiredAssertionStatus",
        args = {}
    )
    public void test_desiredAssertionStatus() {
      Class [] classArray = { Object.class, Integer.class,
                              String.class, PublicTestClass.class,
                              ExtendTestClass.class, ExtendTestClass1.class};

      for(int i = 0; i < classArray.length; i++) {
          assertFalse("assertion status for " + classArray[i],
                       classArray[i].desiredAssertionStatus());
      }
   }



    SecurityManager sm = new SecurityManager() {

        final String forbidenPermissionName = "user.dir";

        public void checkPermission(Permission perm) {
            if (perm.getName().equals(forbidenPermissionName)) {
                throw new SecurityException();
            }
        }

        public void checkMemberAccess(Class<?> clazz,
                int which) {
            if(clazz.equals(TestClass.class)) {
                throw new SecurityException();
            }
        }

        public void checkPackageAccess(String pkg) {
            if(pkg.equals(TestClass.class.getPackage())) {
                throw new SecurityException();
            }
        }

    };
}
