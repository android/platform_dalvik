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
package tests.api.java.io;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass; 

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Arrays;

@TestTargetClass(Serializable.class) 
public class SerializationStressTest1 extends SerializationStressTest {

    // The purpose of these two classes is to test if serialization, when
    // loading, runs the object's constructor (wrong) or the constructor defined
    // at the topmost Serializable superclass(correct).
    static final int INIT_INT_VALUE = 7;

    // HAS to be static class so that our constructor signature will remain
    // untouched (no synthetic param)
    private static class SerializationTest implements java.io.Serializable {
        int anInt = INIT_INT_VALUE;

        public SerializationTest() {
            super();
        }
    }

    static final String INIT_STR_VALUE = "a string that is blortz";

    // HAS to be static class so that our constructor signature will remain
    // untouched (no synthetic param)
    private static class SerializationTestSubclass1 extends SerializationTest {
        String aString = INIT_STR_VALUE;

        public SerializationTestSubclass1() {
            super();
            // Just to change default superclass init value
            anInt = INIT_INT_VALUE / 2;
        }
    }

    // -----------------------------------------------------------------------------------

    private static class SpecTestSuperClass implements Runnable {
        protected java.lang.String instVar;

        public void run() {
        }
    }

    private static class SpecTest extends SpecTestSuperClass implements
            Cloneable, Serializable {
        public java.lang.String instVar1;

        public static java.lang.String staticVar1;

        public static java.lang.String staticVar2;
        {
            instVar1 = "NonStaticInitialValue";
        }
        static {
            staticVar1 = "StaticInitialValue";
            staticVar1 = new String(staticVar1);
        }

        public Object method(Object objParam, Object objParam2) {
            return new Object();
        }

        public boolean method(boolean bParam, Object objParam) {
            return true;
        }

        public boolean method(boolean bParam, Object objParam, Object objParam2) {
            return true;
        }

    }

    private static class SpecTestSubclass extends SpecTest {
        public transient java.lang.String transientInstVar = "transientValue";
    }

    // -----------------------------------------------------------------------------------

    // This one tests what happens if the read/writeObject methods are defined
    // Serialization should work fine.
    private static class ReadWriteObject implements java.io.Serializable {
        public boolean calledWriteObject = false;

        public boolean calledReadObject = false;

        public ReadWriteObject() {
            super();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            calledReadObject = true;
            String s = ((String) in.readObject());
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException {
            calledWriteObject = true;
            out.writeObject(FOO);
        }
    }

    // This one tests what happens if the read/writeObject methods are not
    // private.
    // Serialization should fail.
    private static class PublicReadWriteObject implements java.io.Serializable {
        public boolean calledWriteObject = false;

        public boolean calledReadObject = false;

        public PublicReadWriteObject() {
            super();
        }

        public void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            calledReadObject = true;
            String s = ((String) in.readObject());
        }

        public void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException {
            calledWriteObject = true;
            out.writeObject(FOO);
        }
    }

    // This one tests if field names are serialized in the same way (sorting)
    // across different VMs
    private static class FieldOrder implements Serializable {
        String aaa1NonPrimitive = "aaa1";

        int bbb1PrimitiveInt = 5;

        boolean aaa2PrimitiveBoolean = true;

        String bbb2NonPrimitive = "bbb2";
    }

    // This one tests what happens if you define just readObject, but not
    // writeObject.
    // Does it run or not ?
    private static class JustReadObject implements java.io.Serializable {
        public boolean calledReadObject = false;

        public JustReadObject() {
            super();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            calledReadObject = true;
            in.defaultReadObject();
        }
    }

    // This one tests what happens if you define just writeObject, but not
    // readObject.
    // Does it run or not ?
    private static class JustWriteObject implements java.io.Serializable {
        public boolean calledWriteObject = false;

        public JustWriteObject() {
            super();
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException, ClassNotFoundException {
            calledWriteObject = true;
            out.defaultWriteObject();
        }
    }

    // This one tests class-based replacement when dumping
    private static class ClassBasedReplacementWhenDumping implements
            java.io.Serializable {
        public boolean calledReplacement = false;

        public ClassBasedReplacementWhenDumping() {
            super();
        }

        private Object writeReplace() {
            calledReplacement = true;
            return FOO; // Replacement is a String
        }
    }

    // This one tests whether class-based replacement supports multiple levels.
    // MultipleClassBasedReplacementWhenDumping -> C1 -> C2 -> C3 -> FOO
    private static class MultipleClassBasedReplacementWhenDumping implements
            java.io.Serializable {
        private static class C1 implements java.io.Serializable {
            private Object writeReplace() {
                return new C2();
            }
        }

        private static class C2 implements java.io.Serializable {
            private Object writeReplace() {
                return new C3();
            }
        }

        private static class C3 implements java.io.Serializable {
            private Object writeReplace() {
                return FOO;
            }
        }

        public MultipleClassBasedReplacementWhenDumping() {
            super();
        }

        private Object writeReplace() {
            return new C1();
        }
    }

    // This one tests class-based replacement when loading
    private static class ClassBasedReplacementWhenLoading implements
            java.io.Serializable {
        public ClassBasedReplacementWhenLoading() {
            super();
        }

        private Object readResolve() {
            return FOO; // Replacement is a String
        }
    }

    // This one tests what happens if a loading-replacement is not
    // type-compatible with the original object
    private static class ClassBasedReplacementWhenLoadingViolatesFieldType
            implements java.io.Serializable {
        public ClassBasedReplacementWhenLoading classBasedReplacementWhenLoading = new ClassBasedReplacementWhenLoading();

        public ClassBasedReplacementWhenLoadingViolatesFieldType() {
            super();
        }
    }

    // What happens if dumping causes an error and you try to reload ?
    // Should the load throw the same exception ?
    private static class MyExceptionWhenDumping1 implements
            java.io.Serializable {
        private static class MyException extends java.io.IOException {
        };

        // A primitive instance variable exposes a bug in the serialization
        // spec.
        // Primitive instance variables are written without primitive data tags
        // and so are read without checking for tags. If an exception is
        // written, reading primitive data will just read bytes from the stream
        // which may be tags
        public boolean anInstanceVar = false;

        public MyExceptionWhenDumping1() {
            super();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException, ClassNotFoundException {
            throw new MyException();
        }
    }

    // What happens if dumping causes an error and you try to reload ?
    // Should the load throw the same exception ?
    private static class MyExceptionWhenDumping2 implements
            java.io.Serializable {
        private static class MyException extends java.io.IOException {
        };

        public Integer anInstanceVar = new Integer(0xA1);

        public MyExceptionWhenDumping2() {
            super();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException, ClassNotFoundException {
            throw new MyException();
        }
    }

    // What happens if dumping causes an error (NonSerializable inst var) and
    // you try to reload ?
    // Should the load throw the same exception ?
    private static class NonSerializableExceptionWhenDumping implements
            java.io.Serializable {
        public Object anInstanceVar = new Object();

        public NonSerializableExceptionWhenDumping() {
            super();
        }
    }

    // What happens if dumping causes an error (which is not serializable) and
    // you try to reload ?
    // Should the load throw the same exception ?
    private static class MyUnserializableExceptionWhenDumping implements
            java.io.Serializable {
        private static class MyException extends java.io.IOException {
            private Object notSerializable = new Object();
        };

        public boolean anInstanceVar = false;

        public MyUnserializableExceptionWhenDumping() {
            super();
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException, ClassNotFoundException {
            throw new MyException();
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_1_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = "HelloWorld";
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, (((String) objLoaded)
                    .equals((String) objToSave)));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type: "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_2_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = null;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, objLoaded == objToSave);

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_3_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            byte[] bytes = { 0, 1, 2, 3 };
            objToSave = bytes;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (byte[]) objLoaded, (byte[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_4_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            int[] ints = { 0, 1, 2, 3 };
            objToSave = ints;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (int[]) objLoaded, (int[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_5_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            short[] shorts = { 0, 1, 2, 3 };
            objToSave = shorts;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (short[]) objLoaded, (short[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_6_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            long[] longs = { 0, 1, 2, 3 };
            objToSave = longs;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (long[]) objLoaded, (long[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_7_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            float[] floats = { 0.0f, 1.1f, 2.2f, 3.3f };
            objToSave = floats;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (float[]) objLoaded, (float[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_8_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            double[] doubles = { 0.0, 1.1, 2.2, 3.3 };
            objToSave = doubles;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (double[]) objLoaded, (double[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_9_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            boolean[] booleans = { true, false, false, true };
            objToSave = booleans;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (boolean[]) objLoaded, (boolean[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : " + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_10_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            String[] strings = { "foo", "bar", "java" };
            objToSave = strings;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (Object[]) objLoaded, (Object[]) objToSave));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("Unable to read Object type: " + e.toString());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_11_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            objToSave = new Object(); // Not serializable
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            boolean passed = false;
            Throwable t = null;
            try {
                objLoaded = dumpAndReload(objToSave);
            } catch (NotSerializableException ns) {
                passed = true;
                t = ns;
            } catch (Exception wrongExc) {
                passed = false;
                t = wrongExc;
            }
            assertTrue(
                    "Failed to throw NotSerializableException when serializing "
                            + objToSave + " Threw(if non-null) this: " + t,
                    passed);
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_12_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        try {
            if (DEBUG)
                System.out.println("Obj = <mixed>");
            t_MixPrimitivesAndObjects();
        } catch (IOException e) {
            fail("IOException serializing data : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when dumping mixed types");
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_13_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            SerializationTestSubclass1 st = new SerializationTestSubclass1();
            // Just change the default ivar values
            st.anInt = Integer.MAX_VALUE;
            st.aString = FOO;
            objToSave = st;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // non-serializable inst var has to be initialized from top
            // constructor
            assertTrue(
                    MSG_TEST_FAILED + objToSave,
                    ((SerializationTestSubclass1) objLoaded).anInt == Integer.MAX_VALUE);
            // but serialized var has to be restored as it was in the object
            // when dumped
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((SerializationTestSubclass1) objLoaded).aString
                            .equals(FOO));
        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    @BrokenTest("throws IllegalAccessException on Android and InvalidClassException on RI")
    public void test_18_14_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            SpecTest specTest = new SpecTest();
            // Just change the default ivar values
            specTest.instVar = FOO;
            specTest.instVar1 = specTest.instVar;
            objToSave = specTest;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // non-serializable inst var has to be initialized from top
            // constructor
            assertNull(MSG_TEST_FAILED + objToSave,
                    ((SpecTest) objLoaded).instVar); 
            // instVar from non-serialized class, cant  be  saved/restored
            // by serialization but serialized ivar has to be restored as it
            // was in the object when dumped
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((SpecTest) objLoaded).instVar1.equals(FOO));

        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    @BrokenTest("throws IllegalAccessException on Android and InvalidClassException on RI")
    public void test_18_15_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            SpecTestSubclass specTestSubclass = new SpecTestSubclass();
            // Just change the default ivar values
            specTestSubclass.transientInstVar = FOO;
            objToSave = specTestSubclass;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // non-serializable inst var cant be saved, and it is not init'ed
            // from top constructor in this case
            assertNull(MSG_TEST_FAILED + objToSave,
                    ((SpecTestSubclass) objLoaded).transientInstVar);
            // transient slot, cant be saved/restored by serialization 
        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_16_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            String[] strings = new String[2];
            strings[0] = FOO;
            strings[1] = (" " + FOO + " ").trim(); // Safe way to get a copy
            // that is not ==
            objToSave = strings;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            String[] stringsLoaded = (String[]) objLoaded;
            // Serialization has to use identity-based table for assigning IDs
            assertTrue(MSG_TEST_FAILED + objToSave,
                    !(stringsLoaded[0] == stringsLoaded[1]));
        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_17_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            ReadWriteObject readWrite = new ReadWriteObject();
            objToSave = readWrite;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // has to have called the writeObject on the instance to dump
            assertTrue(MSG_TEST_FAILED + objToSave, readWrite.calledWriteObject);
            // has to have called the readObject on the instance loaded
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((ReadWriteObject) objLoaded).calledReadObject);

        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_18_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PublicReadWriteObject publicReadWrite = new PublicReadWriteObject();
            objToSave = publicReadWrite;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Can't have called the writeObject on the instance to dump
            assertTrue(MSG_TEST_FAILED + objToSave,
                    !publicReadWrite.calledWriteObject);
            // Can't have called the readObject on the instance loaded
            assertTrue(MSG_TEST_FAILED + objToSave,
                    !((PublicReadWriteObject) objLoaded).calledReadObject);

        } catch (IOException e) {
            fail("Exception serializing " + objToSave + "\t->"
                    + e.toString());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_19_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            FieldOrder fieldOrder = new FieldOrder();
            objToSave = fieldOrder;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // This test is only useful for X-loading, so if it managed to
            // dump&load, we passed the test
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_20_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = Class.forName("java.lang.Integer");
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Classes with the same name are unique, so test for ==
            assertTrue(MSG_TEST_FAILED + objToSave, objLoaded == objToSave);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_21_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            // Even though instances of java.lang.Object are not Serializable,
            // instances of java.lang.Class are. So, the object
            // java.lang.Object.class
            // should be serializable
            objToSave = Class.forName("java.lang.Object");
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Classes with the same name are unique, so test for ==
            assertTrue(MSG_TEST_FAILED + objToSave, objLoaded == objToSave);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_22_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.net.URL url = new java.net.URL("http://localhost/a.txt");
            objToSave = url;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue("URLs are not the same: " + url + "\t,\t" + objLoaded,
                    url.equals(objLoaded));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_23_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            JustReadObject justReadObject = new JustReadObject();
            objToSave = justReadObject;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Only calls readObject on the instance loaded if writeObject was
            // also defined
            assertTrue("Called readObject on an object without a writeObject",
                    !((JustReadObject) objLoaded).calledReadObject);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_24_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {

            JustWriteObject justWriteObject = new JustWriteObject();
            objToSave = justWriteObject;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Call writeObject on the instance even if it does not define
            // readObject
            assertTrue(MSG_TEST_FAILED + objToSave,
                    justWriteObject.calledWriteObject);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type: "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_25_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.util.Vector vector = new java.util.Vector(1);
            vector.add(FOO);
            objToSave = vector;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have the string there
            assertTrue(MSG_TEST_FAILED + objToSave, FOO
                    .equals(((java.util.Vector) objLoaded).elementAt(0)));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_26_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.util.Hashtable hashTable = new java.util.Hashtable(5);
            hashTable.put(FOO, FOO);
            objToSave = hashTable;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            java.util.Hashtable loadedHashTable = (java.util.Hashtable) objLoaded;
            // Has to have the key/value there (FOO -> FOO)
            assertTrue(MSG_TEST_FAILED + objToSave, FOO.equals(loadedHashTable
                    .get(FOO)));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_27_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            ClassBasedReplacementWhenDumping classBasedReplacementWhenDumping = new ClassBasedReplacementWhenDumping();
            objToSave = classBasedReplacementWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have run the replacement method
            assertTrue("Did not run writeReplace",
                    classBasedReplacementWhenDumping.calledReplacement);

            // Has to have loaded a String (replacement object)
            assertTrue("Did not replace properly", FOO.equals(objLoaded));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    @BrokenTest("Needs investigation. fails on RI, succeeds on Android. Maybe a bug in the RI.")
    public void test_18_28_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            MultipleClassBasedReplacementWhenDumping multipleClassBasedReplacementWhenDumping = new MultipleClassBasedReplacementWhenDumping();
            objToSave = multipleClassBasedReplacementWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have loaded a String (replacement object)
            assertTrue(
                    "Executed multiple levels of replacement (see PR 1F9RNT1), loaded= "
                            + objLoaded,
                    objLoaded instanceof MultipleClassBasedReplacementWhenDumping.C1);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.toString());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_29_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            ClassBasedReplacementWhenLoading classBasedReplacementWhenLoading = new ClassBasedReplacementWhenLoading();
            objToSave = classBasedReplacementWhenLoading;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have loaded a String (replacement object)
            assertTrue("Did not run readResolve", FOO.equals(objLoaded));

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_30_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            ClassBasedReplacementWhenLoadingViolatesFieldType classBasedReplacementWhenLoadingViolatesFieldType = new ClassBasedReplacementWhenLoadingViolatesFieldType();
            objToSave = classBasedReplacementWhenLoadingViolatesFieldType;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // We cannot gere here, the load replacement must have caused a
            // field type violation
            fail(
                    "Loading replacements can cause field type violation in this implementation");

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (ClassCastException e) {
            assertTrue(
                    "Loading replacements can NOT cause field type violation in this implementation",
                    true);
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_31_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            MyExceptionWhenDumping1 exceptionWhenDumping = new MyExceptionWhenDumping1();
            objToSave = exceptionWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            boolean causedException = false;
            try {
                dump(objToSave);
            } catch (MyExceptionWhenDumping1.MyException e) {
                causedException = true;
            }
            ;
            assertTrue("Should have caused an exception when dumping",
                    causedException);
            causedException = false;
            try {
                objLoaded = reload();
                // Although the spec says we should get a WriteAbortedException,
                // the serialization format handle an Exception when reading
                // primitive data so we get ClassCastException instead
            } catch (ClassCastException e) {
                causedException = true;
            }
            ;
            assertTrue("Should have caused a ClassCastException when loading",
                    causedException);
        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_32_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            MyExceptionWhenDumping2 exceptionWhenDumping = new MyExceptionWhenDumping2();
            objToSave = exceptionWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            boolean causedException = false;
            try {
                dump(objToSave);
            } catch (MyExceptionWhenDumping2.MyException e) {
                causedException = true;
            }
            ;
            assertTrue("Should have caused an exception when dumping",
                    causedException);
            causedException = false;
            try {
                objLoaded = reload();
            } catch (java.io.WriteAbortedException e) {
                causedException = true;
            }
            ;
            assertTrue(
                    "Should have caused a java.io.WriteAbortedException when loading",
                    causedException);
        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (ClassCastException e) {
            fail("ClassCastException : " + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_NonSerializableExceptionWhenDumping() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            NonSerializableExceptionWhenDumping nonSerializableExceptionWhenDumping = new NonSerializableExceptionWhenDumping();
            objToSave = nonSerializableExceptionWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            boolean causedException = false;
            try {
                dump(objToSave);
            } catch (java.io.NotSerializableException e) {
                causedException = true;
            }
            ;
            assertTrue("Should have caused an exception when dumping",
                    causedException);
            causedException = false;
            try {
                objLoaded = reload();
            } catch (java.io.WriteAbortedException e) {
                causedException = true;
            }
            ;
            assertTrue(
                    "Should have caused a java.io.WriteAbortedException when loading",
                    causedException);
        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    @BrokenTest("succeeds on Android, but fails on RI with MyException being thrown.")
    public void test_18_33_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            MyUnserializableExceptionWhenDumping exceptionWhenDumping = new MyUnserializableExceptionWhenDumping();
            objToSave = exceptionWhenDumping;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            boolean causedException = false;
            try {
                dump(objToSave);
            } catch (java.io.StreamCorruptedException e) {
                causedException = true;
            }
            ;
            assertTrue("Should have caused an exception when dumping",
                    causedException);
            // As the stream is corrupted, reading the stream will have
            // undefined results
        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_34_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ioe = new java.io.IOException();
            objToSave = ioe;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_35_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = Class.forName("java.util.Hashtable");
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Classes with the same name are unique, so test for ==
            assertTrue(MSG_TEST_FAILED + objToSave, objLoaded == objToSave);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_36_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ex = new java.io.InvalidClassException(FOO);
            objToSave = ex;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_37_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ex = new java.io.InvalidObjectException(FOO);
            objToSave = ex;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_38_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ex = new java.io.NotActiveException(FOO);
            objToSave = ex;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_39_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ex = new java.io.NotSerializableException(FOO);
            objToSave = ex;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_40_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            java.io.IOException ex = new java.io.StreamCorruptedException(FOO);
            objToSave = ex;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to be able to save/load an exception
            assertTrue(MSG_TEST_FAILED + objToSave, true);

        } catch (IOException e) {
            fail("IOException serializing " + objToSave + " : "
                    + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail("ClassNotFoundException reading Object type : "
                    + e.getMessage());
        } catch (Error err) {
            System.out.println("Error when obj = " + objToSave);
            // err.printStackTrace();
            throw err;
        }
    }
}
