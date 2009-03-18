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
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass; 

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.ObjectStreamField;
import java.io.OptionalDataException;
import java.math.BigInteger;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyPermission;
import java.util.TimeZone;
import java.util.Vector;

@TestTargetClass(java.io.Serializable.class) 
public class SerializationStressTest3 extends SerializationStressTest {

    // -----------------------------------------------------------------------------------
    private static class DefaultConstructor implements java.io.Serializable {
        int f1;

        static int valueAfterConstructor = 5;

        DefaultConstructor() {
            f1 = valueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. It is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof DefaultConstructor))
                return false;

            DefaultConstructor inst = (DefaultConstructor) obj;
            return inst.f1 == valueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class NonSerDefaultConstructor {
        public int f1;

        public static int valueAfterConstructor = 5;

        NonSerDefaultConstructor() {
            f1 = valueAfterConstructor;
        }

        public NonSerDefaultConstructor(String notUsed) {
        }
    }

    private static class NonSerPrivateConstructor {
        public int f1;

        public static int valueAfterConstructor = 5;

        private NonSerPrivateConstructor() {
            f1 = valueAfterConstructor;
        }

        public NonSerPrivateConstructor(String notUsed) {
        }
    }

    private static class NonSerProtectedConstructor {
        public int f1;

        public static int valueAfterConstructor = 5;

        protected NonSerProtectedConstructor() {
            f1 = valueAfterConstructor;
        }
    }

    private static class NonSerPublicConstructor {
        public int f1;

        public static int valueAfterConstructor = 5;

        public NonSerPublicConstructor() {
            f1 = valueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class DefaultConstructorSub extends NonSerDefaultConstructor
            implements java.io.Serializable {
        int fsub;

        static int subValueAfterConstructor = 11;

        public DefaultConstructorSub() {
            f1 = 7;
            fsub = subValueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. It is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof DefaultConstructorSub))
                return false;

            DefaultConstructorSub inst = (DefaultConstructorSub) obj;
            if (inst.f1 != valueAfterConstructor)
                return false;
            return inst.fsub == subValueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class PrivateConstructor implements java.io.Serializable {
        int f1;

        static int valueAfterConstructor = 5;

        private PrivateConstructor() {
            f1 = valueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. Is is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof PrivateConstructor))
                return false;

            PrivateConstructor inst = (PrivateConstructor) obj;
            return inst.f1 == valueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class PrivateConstructorSub extends NonSerPrivateConstructor
            implements java.io.Serializable {
        int fsub;

        static int subValueAfterConstructor = 11;

        public PrivateConstructorSub() {
            super("notUsed");
            f1 = 7;
            fsub = subValueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. Is is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof PrivateConstructorSub))
                return false;

            PrivateConstructorSub inst = (PrivateConstructorSub) obj;
            return inst.f1 == valueAfterConstructor
                    && inst.fsub == subValueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class ProtectedConstructor implements java.io.Serializable {
        int f1;

        static int valueAfterConstructor = 5;

        protected ProtectedConstructor() {
            f1 = valueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. Is is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof ProtectedConstructor))
                return false;

            ProtectedConstructor inst = (ProtectedConstructor) obj;
            return inst.f1 == valueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class ProtectedConstructorSub extends
            NonSerProtectedConstructor implements java.io.Serializable {
        int fsub;

        static int subValueAfterConstructor = 11;

        public ProtectedConstructorSub() {
            f1 = 7;
            fsub = subValueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. Is is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof ProtectedConstructorSub))
                return false;

            ProtectedConstructorSub inst = (ProtectedConstructorSub) obj;
            return inst.f1 == valueAfterConstructor
                    && inst.fsub == subValueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class PublicConstructor implements java.io.Serializable {
        int f1;

        static int valueAfterConstructor = 5;

        public PublicConstructor() {
            f1 = valueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. Is is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof PublicConstructor))
                return false;

            PublicConstructor inst = (PublicConstructor) obj;
            return inst.f1 == valueAfterConstructor;
        }
    }

    // -----------------------------------------------------------------------------------
    private static class PublicConstructorSub extends NonSerPublicConstructor
            implements java.io.Serializable {
        int fsub;

        static final int subValueAfterConstructor = 11;

        public PublicConstructorSub() {
            f1 = 7;
            fsub = subValueAfterConstructor;
        }

        public boolean equals(Object obj) {
            /*
             * This method is not answering it the objs is equal. It is
             * answering if the vars have the value that it have to have after
             * dumping and loading
             */

            if (obj == null)
                return false;
            if (!(obj instanceof PublicConstructorSub))
                return false;

            PublicConstructorSub inst = (PublicConstructorSub) obj;
            return inst.f1 == valueAfterConstructor
                    && inst.fsub == subValueAfterConstructor;
        }
    }

    // Tests the behavior of ObjectOutputStream.PutField.write()
    private static class WriteFieldsUsingPutFieldWrite implements
            java.io.Serializable {
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("object1", Vector.class),
                new ObjectStreamField("int1", Integer.TYPE) };

        private static Vector v1 = new Vector(Arrays.asList(new String[] {
                "1st", "2nd" }));

        private boolean passed = false;

        public WriteFieldsUsingPutFieldWrite() {
            super();
        }

        public boolean passed() {
            return passed;
        }

        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException, ClassNotFoundException {
            int int1 = in.readInt();
            Vector object1 = (Vector) in.readObject();
            passed = int1 == 0xA9 && object1.equals(v1);
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException, ClassNotFoundException {
            ObjectOutputStream.PutField fields = out.putFields();
            fields.put("object1", v1);
            fields.put("int1", 0xA9);
            // Use fields.write() instead of out.writeFields();
            fields.write(out);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_81_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            new ObjectOutputStream(dos); // just to make sure we get a header
            dos.writeByte(ObjectStreamConstants.TC_BLOCKDATALONG);
            int length = 333; // Bigger than 1 byte
            dos.writeInt(length);
            for (int i = 0; i < length; i++) {
                dos.writeByte(0); // actual value does not matter
            }
            dos.flush();
            int lengthRead = 0;
            try {
                ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(out.toByteArray()));
                Object obj = ois.readObject();
            } catch (OptionalDataException e) {
                lengthRead = e.length;
            }
            assertTrue("Did not throw exception with optional data size ",
                    length == lengthRead);
        } catch (ClassNotFoundException e) {
            fail("Unable to read BLOCKDATA : " + e.getMessage());
        } catch (IOException e) {
            fail("IOException testing BLOCKDATALONG : " + e.getMessage());
        } catch (Error err) {
            System.out.println("Error " + err + " when testing BLOCKDATALONG");
            throw err;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies serialization.",
        method = "!Serialization",
        args = {}
    )
    public void test_18_82_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            DefaultConstructor test = new DefaultConstructor();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_83_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            DefaultConstructorSub test = new DefaultConstructorSub();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_84_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PrivateConstructor test = new PrivateConstructor();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    @BrokenTest("Needs investigation.Fails on Android with IlegalAccessException and RI with IOException: no valid constructor")
    public void test_18_85_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PrivateConstructorSub test = new PrivateConstructorSub();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_86_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            ProtectedConstructor test = new ProtectedConstructor();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_87_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            ProtectedConstructorSub test = new ProtectedConstructorSub();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_88_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PublicConstructor test = new PublicConstructor();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_89_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PublicConstructorSub test = new PublicConstructorSub();
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_90_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = TABLE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, TABLE.equals(objLoaded));

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
    public void test_18_91_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedMap(TABLE);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_92_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableMap(TABLE);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_93_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = MAP;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, MAP.equals(objLoaded));

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
    public void test_18_94_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedMap(MAP);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_95_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableMap(MAP);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_96_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = ALIST;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, ALIST.equals(objLoaded));

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
    public void test_18_97_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = LIST;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, LIST.equals(objLoaded));

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
    public void test_18_98_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedList(LIST);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_99_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableList(LIST);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_100_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = SET;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, SET.equals(objLoaded));

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
    public void test_18_101_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedSet(SET);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_102_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableSet(SET);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_103_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = TREE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, TREE.equals(objLoaded));

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
    public void test_18_104_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedSortedMap(TREE);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_105_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableSortedMap(TREE);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_106_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = SORTSET;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, SET.equals(objLoaded));

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
    public void test_18_107_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.synchronizedSortedSet(SORTSET);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_108_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            Object col = Collections.unmodifiableSortedSet(SORTSET);
            objToSave = col;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, col.equals(objLoaded));

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
    public void test_18_109_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = CALENDAR;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, CALENDAR.equals(objLoaded));

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
    public void test_18_110_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            TimeZone test = TimeZone.getTimeZone("EST");
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_111_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            TimeZone test = TimeZone.getTimeZone("EST");
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    public void test_18_112_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            TimeZone test = TimeZone.getTimeZone("GMT");
            objToSave = test;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, test.equals(objLoaded));

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
    @KnownFailure("Serialization of SimpleDateFormat object fails")
    public void test_18_113_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = DATEFORM;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, DATEFORM.equals(objLoaded));

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
    public void test_18_114_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = CHOICE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, CHOICE.equals(objLoaded));

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
    @KnownFailure("Serialization of NumberFormat object fails")
    public void test_18_115_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = NUMBERFORM;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, NUMBERFORM
                    .equals(objLoaded));

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
    public void test_18_116_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = MESSAGE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, MESSAGE.toPattern().equals(
                    ((java.text.MessageFormat) objLoaded).toPattern()));

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
    public void test_18_117_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = PERM;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, PERM.equals(objLoaded));

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
    public void test_18_118_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = PERMCOL;
            Enumeration elementsBefore = PERMCOL.elements();
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            Enumeration elementsAfter = ((PermissionCollection) objLoaded)
                    .elements();
            boolean equals = true;
            while (elementsBefore.hasMoreElements()) {
                // To make sure elements are the same
                Object oBefore = elementsBefore.nextElement();
                Object oAfter = elementsAfter.nextElement();
                equals &= oBefore.equals(oAfter);

            }
            // To make sure sizes are the same
            equals &= elementsBefore.hasMoreElements() == elementsAfter
                    .hasMoreElements();

            assertTrue(MSG_TEST_FAILED + objToSave, equals);

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
    public void test_18_119_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = Locale.CHINESE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, Locale.CHINESE
                    .equals(objLoaded));

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
    public void test_18_120_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = LINKEDLIST;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(MSG_TEST_FAILED + objToSave, LINKEDLIST
                    .equals(objLoaded));

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
    public void test_18_121_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = java.text.AttributedCharacterIterator.Attribute.INPUT_METHOD_SEGMENT;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(
                    MSG_TEST_FAILED + objToSave,
                    java.text.AttributedCharacterIterator.Attribute.INPUT_METHOD_SEGMENT == objLoaded);

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
    public void test_18_122_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = java.text.AttributedCharacterIterator.Attribute.LANGUAGE;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(
                    MSG_TEST_FAILED + objToSave,
                    java.text.AttributedCharacterIterator.Attribute.LANGUAGE == objLoaded);

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
    public void test_18_123_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = java.text.AttributedCharacterIterator.Attribute.READING;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Has to have worked
            assertTrue(
                    MSG_TEST_FAILED + objToSave,
                    java.text.AttributedCharacterIterator.Attribute.READING == objLoaded);

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
    public void test_18_124_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = new Object[] { Integer.class, new Integer(1) };
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            // Classes with the same name are unique, so test for ==
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((Object[]) objLoaded)[0] == ((Object[]) objToSave)[0]
                            && ((Object[]) objLoaded)[1]
                                    .equals(((Object[]) objToSave)[1]));

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
    public void test_18_125_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = new BigInteger[] { BigInteger.ZERO, BigInteger.ONE,
                    BigInteger.valueOf(-1), BigInteger.valueOf(255),
                    BigInteger.valueOf(-255),
                    new BigInteger("75881644843307850793466070"),
                    new BigInteger("-636104487142732527326202462") };
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, Arrays.equals(
                    (BigInteger[]) objLoaded, (BigInteger[]) objToSave));

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
    public void test_18_126_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            objToSave = new WriteFieldsUsingPutFieldWrite();
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((WriteFieldsUsingPutFieldWrite) objLoaded).passed());

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
    public void test_18_127_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            BitSet bs = new BitSet(64);
            bs.set(1);
            bs.set(10);
            bs.set(100);
            bs.set(1000);
            objToSave = bs;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave, bs.equals(objLoaded));

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
    public void test_18_128_writeObject() {
        // Test for method void
        // java.io.ObjectOutputStream.writeObject(java.lang.Object)

        Object objToSave = null;
        Object objLoaded;

        try {
            PropertyPermission test = new PropertyPermission("java.*",
                    "read,write");
            PermissionCollection p = test.newPermissionCollection();
            p.add(new PropertyPermission("java.*", "read"));
            p.add(new PropertyPermission("java.*", "write"));
            // System.out.println("Does implies work? " + p.implies(test));

            objToSave = p;
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            objLoaded = dumpAndReload(objToSave);
            assertTrue(MSG_TEST_FAILED + objToSave,
                    ((PermissionCollection) objLoaded).implies(test));

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
