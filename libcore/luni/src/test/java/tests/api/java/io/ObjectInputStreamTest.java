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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.support.Support_ASimpleInputStream;
import tests.support.Support_IOTestSecurityManager;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(ObjectInputStream.class) 
public class ObjectInputStreamTest extends junit.framework.TestCase implements
        Serializable {

    static final long serialVersionUID = 1L;
    
    ObjectInputStream ois;

    ObjectOutputStream oos;

    ByteArrayOutputStream bao;
    
    boolean readStreamHeaderCalled;

    private final String testString = "Lorem ipsum...";

    private final int testLength = testString.length();

    /**
     * @tests java.io.ObjectInputStream#ObjectInputStream()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies the protected ObjectInputStream() constructor.",
        method = "ObjectInputStream",
        args = {}
    )     
    public void test_Constructor() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new Support_IOTestSecurityManager());
        
        try { 
            ois = new BasicObjectInputStream();
            fail("SecurityException expected.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(sm);
        }
    }

    /**
     * @tests java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "ObjectInputStream",
        args = {java.io.InputStream.class}
    )     
    public void test_ConstructorLjava_io_InputStream() throws IOException {
        // Test for method java.io.ObjectInputStream(java.io.InputStream)
        oos.writeDouble(Double.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.close();
        oos.close();

        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(new byte[90]));
            fail("StreamCorruptedException expected");
        } catch (StreamCorruptedException e) {}
    }

    /**
     * @tests java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "ObjectInputStream",
        args = {java.io.InputStream.class}
    )        
    public void test_ConstructorLjava_io_InputStream_IOException() throws IOException {
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        sis.throwExceptionOnNextUse = true;
        try {
            ois = new ObjectInputStream(sis);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.PARTIAL_COMPLETE,
            notes = "Verifies that object can be serialized and deserialized correctly with reading descriptor from serialization stream.",
            method = "readClassDescriptor",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Verifies that object can be serialized and deserialized correctly with reading descriptor from serialization stream.",
            method = "readObject",
            args = {}
        )
    })    
    public void test_ClassDescriptor() throws IOException,
            ClassNotFoundException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStreamWithWriteDesc oos = new ObjectOutputStreamWithWriteDesc(
                baos);
        oos.writeObject(String.class);
        oos.close();
        Class<?> cls = TestClassForSerialization.class;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStreamWithReadDesc ois = new ObjectInputStreamWithReadDesc(
                bais, cls);
        Object obj = ois.readObject();
        ois.close();
        assertEquals(cls, obj);
    }
        
    /**
     * @tests java.io.ObjectInputStream#available()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "available",
        args = {}
    )        
    public void test_available() throws IOException {
        // Test for method int java.io.ObjectInputStream.available()
        oos.writeBytes(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        assertEquals("Test 1: Incorrect number of bytes;", testLength, ois.available());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#available()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "available",
        args = {}
    )        
    public void test_available_IOException() throws IOException {
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.available();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "close",
        args = {}
    )     
    public void test_close() throws Exception {
        // Test for method void java.io.ObjectInputStream.close()
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.close();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#defaultReadObject()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "defaultReadObject",
        args = {}
    )      
    public void test_defaultReadObject() throws Exception {
        // Test for method void java.io.ObjectInputStream.defaultReadObject()
        // SM. This method may as well be private, as if called directly it
        // throws an exception.
        String s = testString;
        oos.writeObject(s);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        try {
            ois.defaultReadObject();
            fail("NotActiveException expected");
        } catch (NotActiveException e) {
            // Desired behavior
        } finally {
            ois.close();
        }
    }

    /**
     * @tests java.io.ObjectInputStream#enableResolveObject(boolean)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies enableResolveObject(boolean).",
        method = "enableResolveObject",
        args = {boolean.class}
    )     
    public void test_enableResolveObjectB() throws IOException {
        // Start testing without a SecurityManager.
        BasicObjectInputStream bois = new BasicObjectInputStream();
        assertFalse("Test 1: Object resolving must be disabled by default.",
                bois.enableResolveObject(true));
        
        assertTrue("Test 2: enableResolveObject did not return the previous value.",
                bois.enableResolveObject(false));
    }

    /**
     * @tests java.io.ObjectInputStream#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "read",
        args = {}
    )       
    public void test_read() throws IOException {
        // Test for method int java.io.ObjectInputStream.read()
        oos.write('T');
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect byte value", 'T', ois.read());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#read()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "read",
        args = {}
    )        
    public void test_read_IOException() throws IOException {
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.read();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "read",
        args = {byte[].class, int.class, int.class}
    )    
    public void test_read$BII() throws IOException {
        // Test for method int java.io.ObjectInputStream.read(byte [], int, int)
        byte[] buf = new byte[testLength];
        oos.writeBytes(testString);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.read(buf, 0, testLength);
        ois.close();
        assertEquals("Read incorrect bytes", testString, new String(buf));
    }

    /**
     * @tests java.io.ObjectInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks Exceptions.",
        method = "read",
        args = {byte[].class, int.class, int.class}
    )        
    public void test_read$BII_Exception() throws IOException {
        byte[] buf = new byte[testLength];
        oos.writeObject(testString);
        oos.close();

        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        try {
            ois.read(buf, 0, -1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            ois.read(buf, -1,1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            ois.read(buf, testLength, 1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        ois.close();
        
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.read(buf, 0, testLength);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readFields()
     * @tests java.io.ObjectOutputStream#writeFields()
     */
    @TestTargets({
        @TestTargetNew(
                method = "readFields",
                args = {},
                level = TestLevel.COMPLETE
        ),
        @TestTargetNew(
                method = "writeFields",
                args = {},
                clazz = ObjectOutputStream.class,
                level = TestLevel.COMPLETE
        )
    })    
    public void test_readFields() throws Exception {
        // Test for method java.io.ObjectInputStream$GetField
        // java.io.ObjectInputStream.readFields()

        SerializableTestHelper sth;

        /*
         * "SerializableTestHelper" is an object created for these tests with
         * two fields (Strings) and simple implementations of readObject and
         * writeObject which simply read and write the first field but not the
         * second
         */

        oos.writeObject(new SerializableTestHelper("Gabba", "Jabba"));
        oos.flush();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        sth = (SerializableTestHelper) (ois.readObject());
        assertEquals("readFields / writeFields failed--first field not set",
                "Gabba", sth.getText1());
        assertNull(
                "readFields / writeFields failed--second field should not have been set",
                sth.getText2());
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "readFully",
        args = {byte[].class}
    )     
    public void test_readFully$B() throws IOException {
        byte[] buf = new byte[testLength];
        oos.writeBytes(testString);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.readFully(buf);
        assertEquals("Test 1: Incorrect bytes read;", 
                testString, new String(buf));
        ois.close();

        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.read();
        try {
            ois.readFully(buf);
            fail("Test 2: EOFException expected.");
        } catch (EOFException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[])
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "readFully",
        args = {byte[].class}
    )        
    public void test_readFully$B_Exception() throws IOException {
        byte[] buf = new byte[testLength];
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.readFully(buf);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "readFully",
        args = {byte[].class, int.class, int.class}
    )      
    public void test_readFully$BII() throws IOException {
        // Test for method void java.io.ObjectInputStream.readFully(byte [],
        // int, int)
        byte[] buf = new byte[testLength];
        oos.writeBytes(testString);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.readFully(buf, 0, testLength);
        assertEquals("Read incorrect bytes", testString, new String(buf));
        ois.close();
        
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.read();
        try {
            ois.readFully(buf);
            fail("Test 2: EOFException expected.");
        } catch (EOFException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks Exceptions.",
        method = "readFully",
        args = {byte[].class, int.class, int.class}
    )        
    public void test_readFully$BII_Exception() throws IOException {
        byte[] buf = new byte[testLength];
        oos.writeObject(testString);
        oos.close();

        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        try {
            ois.readFully(buf, 0, -1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            ois.readFully(buf, -1,1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            ois.readFully(buf, testLength, 1);
            fail("IndexOutOfBoundsException was not thrown.");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        ois.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.readFully(buf, 0, 1);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readLine()
     */
    @SuppressWarnings("deprecation")
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "readLine",
        args = {}
    )       
    public void test_readLine() throws IOException {
        String line;
        oos.writeBytes("Lorem\nipsum\rdolor sit amet...");
        oos.close();

        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        line = ois.readLine();
        assertTrue("Test 1: Incorrect line written or read: " + line, 
                line.equals("Lorem"));
        line = ois.readLine();
        assertTrue("Test 2: Incorrect line written or read: " + line, 
                line.equals("ipsum"));
        line = ois.readLine();
        assertTrue("Test 3: Incorrect line written or read: " + line, 
                line.equals("dolor sit amet..."));
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readLine()
     */
    @SuppressWarnings("deprecation")
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "readLine",
        args = {}
    )    
    public void test_readLine_IOException() throws IOException {
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.readLine();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "readObject",
        args = {}
    )     
    public void test_readObject() throws Exception {
        // Test for method java.lang.Object
        // java.io.ObjectInputStream.readObject()
        String s = testString;
        oos.writeObject(s);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect Object value", s, ois.readObject());
        ois.close();

        // Regression for HARMONY-91
        // dynamically create serialization byte array for the next hierarchy:
        // - class A implements Serializable
        // - class C extends A

        byte[] cName = C.class.getName().getBytes();
        byte[] aName = A.class.getName().getBytes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] begStream = new byte[] { (byte) 0xac, (byte) 0xed, // STREAM_MAGIC
                (byte) 0x00, (byte) 0x05, // STREAM_VERSION
                (byte) 0x73, // TC_OBJECT
                (byte) 0x72, // TC_CLASSDESC
                (byte) 0x00, // only first byte for C class name length
        };

        out.write(begStream, 0, begStream.length);
        out.write(cName.length); // second byte for C class name length
        out.write(cName, 0, cName.length); // C class name

        byte[] midStream = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x21, // serialVersionUID = 33L
                (byte) 0x02, // flags
                (byte) 0x00, (byte) 0x00, // fields : none
                (byte) 0x78, // TC_ENDBLOCKDATA
                (byte) 0x72, // Super class for C: TC_CLASSDESC for A class
                (byte) 0x00, // only first byte for A class name length
        };

        out.write(midStream, 0, midStream.length);
        out.write(aName.length); // second byte for A class name length
        out.write(aName, 0, aName.length); // A class name

        byte[] endStream = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x0b, // serialVersionUID = 11L
                (byte) 0x02, // flags
                (byte) 0x00, (byte) 0x01, // fields

                (byte) 0x4c, // field description: type L (object)
                (byte) 0x00, (byte) 0x04, // length
                // field = 'name'
                (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,

                (byte) 0x74, // className1: TC_STRING
                (byte) 0x00, (byte) 0x12, // length
                //
                (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x2f, (byte) 0x6c, (byte) 0x61,
                (byte) 0x6e, (byte) 0x67, (byte) 0x2f, (byte) 0x53,
                (byte) 0x74, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
                (byte) 0x67, (byte) 0x3b,

                (byte) 0x78, // TC_ENDBLOCKDATA
                (byte) 0x70, // NULL super class for A class

                // classdata
                (byte) 0x74, // TC_STRING
                (byte) 0x00, (byte) 0x04, // length
                (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65, // value
        };

        out.write(endStream, 0, endStream.length);
        out.flush();

        // read created serial. form
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                out.toByteArray()));
        Object o = ois.readObject();
        assertEquals(C.class, o.getClass());

        // Regression for HARMONY-846
        assertNull(new ObjectInputStream() {}.readObject());
    }

    private void fillStreamHeader(byte[] buffer) {
        short magic = java.io.ObjectStreamConstants.STREAM_MAGIC;
        short version = java.io.ObjectStreamConstants.STREAM_VERSION;

        if (buffer.length < 4) {
            throw new IllegalArgumentException("The buffer's minimal length must be 4.");
        }

        // Initialize the buffer with the correct header for object streams
        buffer[0] = (byte) (magic >> 8);
        buffer[1] = (byte) magic;
        buffer[2] = (byte) (version >> 8);
        buffer[3] = (byte) (version);
    }
    
    /**
     * @tests java.io.ObjectInputStream#readObjectOverride()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies readObjectOverride().",
        method = "readObjectOverride",
        args = {}
    )     
    public void test_readObjectOverride() throws Exception {
        byte[] buffer = new byte[4];

        // Initialize the buffer with the correct header for object streams
        fillStreamHeader(buffer);
        
        // Test 1: Check that readObjectOverride() returns null if there
        // is no input stream.
        BasicObjectInputStream bois = new BasicObjectInputStream();
        assertNull("Test 1:", bois.readObjectOverride());
        
        // Test 2: Check that readObjectOverride() throws an IOException
        // if there is an input stream.
        bois = new BasicObjectInputStream(new ByteArrayInputStream(buffer));
        try {
            bois.readObjectOverride();
            fail("Test 2: IOException expected.");
        } catch (IOException e) {}
        
        bois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "readObject",
        args = {}
    )     
    public void test_readObjectMissingClasses() throws Exception {
        SerializationTest.verifySelf(new A1(), new SerializableAssert() {
            public void assertDeserialized(Serializable initial,
                    Serializable deserialized) {
                assertEquals(5, ((A1) deserialized).b1.i);
            }
        });
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "readObject",
        args = {}
    )     
    public void test_readObjectCorrupt() {
        byte[] bytes = { 00, 00, 00, 0x64, 0x43, 0x48, (byte) 0xFD, 0x71, 00,
                00, 0x0B, (byte) 0xB8, 0x4D, 0x65 };
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        boolean exception = false;
        try {
            ObjectInputStream in = new ObjectInputStream(bin);
            in.readObject();
            fail("Unexpected read of corrupted stream");
        } catch (StreamCorruptedException e) {
            exception = true;
        } catch (IOException e) {
            fail("Unexpected: " + e);
        } catch (ClassNotFoundException e) {
            fail("Unexpected: " + e);
        }
        assertTrue("Expected StreamCorruptedException", exception);
    }

    /**
     * @tests java.io.ObjectInputStream#readStreamHeader()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies readStreamHeader().",
        method = "readStreamHeader",
        args = {}
    )     
    public void test_readStreamHeader() throws IOException {
        String testString = "Lorem ipsum";
        BasicObjectInputStream bois;
        short magic = java.io.ObjectStreamConstants.STREAM_MAGIC;
        short version = java.io.ObjectStreamConstants.STREAM_VERSION;
        byte[] buffer = new byte[20];

        // Initialize the buffer with the correct header for object streams
        fillStreamHeader(buffer);
        System.arraycopy(testString.getBytes(), 0, buffer, 4, testString.length());

        // Test 1: readStreamHeader should not throw a StreamCorruptedException.
        // It should get called by the ObjectInputStream constructor.
        try {
            readStreamHeaderCalled = false;
            bois = new BasicObjectInputStream(new ByteArrayInputStream(buffer));
            bois.close();
        } catch (StreamCorruptedException e) {
            fail("Test 1: Unexpected StreamCorruptedException.");
        }
        assertTrue("Test 1: readStreamHeader() has not been called.", 
                    readStreamHeaderCalled);

        // Test 2: Make the stream magic number invalid and check that
        // readStreamHeader() throws an exception.
        buffer[0] = (byte)magic;
        buffer[1] = (byte)(magic >> 8);
        try {
            readStreamHeaderCalled = false;
            bois = new BasicObjectInputStream(new ByteArrayInputStream(buffer));
            fail("Test 2: StreamCorruptedException expected.");
            bois.close();
        } catch (StreamCorruptedException e) {
        }
        assertTrue("Test 2: readStreamHeader() has not been called.", 
                    readStreamHeaderCalled);

        // Test 3: Make the stream version invalid and check that
        // readStreamHeader() throws an exception.
        buffer[0] = (byte)(magic >> 8);
        buffer[1] = (byte)magic;
        buffer[2] = (byte)(version);
        buffer[3] = (byte)(version >> 8);
        try {
            readStreamHeaderCalled = false;
            bois = new BasicObjectInputStream(new ByteArrayInputStream(buffer));
            fail("Test 3: StreamCorruptedException expected.");
            bois.close();
        } catch (StreamCorruptedException e) {
        }
        assertTrue("Test 3: readStreamHeader() has not been called.", 
                    readStreamHeaderCalled);
    }

    /**
     * @tests java.io.ObjectInputStream#readUnsignedByte()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "readUnsignedByte",
        args = {}
    )     
    public void test_readUnsignedByte() throws IOException {
        oos.writeByte(-1);
        oos.close();
        
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Test 1: Incorrect unsigned byte written or read.", 
                255, ois.readUnsignedByte());
        
        try {
            ois.readUnsignedByte();
            fail("Test 2: EOFException expected.");
        } catch (EOFException e) {
            // Expected.
        }

        ois.close();
        try {
            ois.readUnsignedByte();
            fail("Test 3: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ObjectInputStream#readUnsignedShort()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "readUnsignedShort",
        args = {}
    )    
    public void test_readUnsignedShort() throws IOException {
        // Test for method int java.io.ObjectInputStream.readUnsignedShort()
        oos.writeShort(-1);
        oos.close();

        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Test 1: Incorrect unsigned short written or read.", 
                65535, ois.readUnsignedShort());
        
        try {
            ois.readUnsignedShort();
            fail("Test 2: EOFException expected.");
        } catch (EOFException e) {
            // Expected.
        }

        ois.close();
        try {
            ois.readUnsignedShort();
            fail("Test 3: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
    }

    /**
     * @tests java.io.ObjectInputStream#resolveProxyClass(String[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies resolveProxyClass(String[]).",
        method = "resolveProxyClass",
        args = {java.lang.String[].class}
    )      
    public void test_resolveProxyClass() throws IOException {
        BasicObjectInputStream bois;
        byte[] buffer = new byte[10];

        // Initialize the buffer with the header for object streams
        fillStreamHeader(buffer);
        bois = new BasicObjectInputStream(new ByteArrayInputStream(buffer));

        // Test 1: Check that a NullPointerException is thrown
        // if null is passed to the method.
        try {
            bois.resolveProxyClass(null);
            fail("Test 1: NullPointerException expected.");
        }
        catch (NullPointerException npe) {
        }
        catch (ClassNotFoundException cnfe) {
            fail("Test 1: Unexpected ClassNotFoundException.");
        }
        
        // Test 2: Check that visible interfaces are found.
        try {
            String[] interfaces = { "java.io.Closeable", 
                                    "java.lang.Cloneable" };
            bois.resolveProxyClass(interfaces);
        }
        catch (ClassNotFoundException cnfe) {
            fail("Test 2: Unexpected ClassNotFoundException.");
        }
        
        // Test 3: Check that a ClassNotFoundException is thrown if the
        // array of interfaces is not valid.
        try {
            String[] interfaces = { "java.io.Closeable", 
                                    "java.io.Closeable" };
            bois.resolveProxyClass(interfaces);
            fail ("Test 3: ClassNotFoundException expected.");
        }
        catch (ClassNotFoundException cnfe) {
        }
        
        bois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#skipBytes(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "skipBytes",
        args = {int.class}
    )    
    public void test_skipBytesI() throws IOException {
        // Test for method int java.io.ObjectInputStream.skipBytes(int)
        byte[] buf = new byte[testLength];
        oos.writeBytes(testString);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.skipBytes(5);
        ois.read(buf, 0, 5);
        ois.close();
        assertEquals("Skipped incorrect bytes", testString.substring(5, 10), 
                new String(buf, 0, 5));

        // Regression for HARMONY-844
        try {
            new ObjectInputStream() {}.skipBytes(0);
            fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }
    
    /**
     * @tests java.io.ObjectInputStream#skipBytes(int)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks IOException.",
        method = "skipBytes",
        args = {int.class}
    )    
    public void test_skipBytesI_IOException() throws IOException {
        oos.writeObject(testString);
        oos.close();
        
        Support_ASimpleInputStream sis = new Support_ASimpleInputStream(bao.toByteArray());
        ois = new ObjectInputStream(sis);
        sis.throwExceptionOnNextUse = true;
        try {
            ois.skipBytes(5);
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        sis.throwExceptionOnNextUse = false;
        ois.close();
    }

    // Regression Test for JIRA 2192
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "readObject",
        args = {}
    )    
    public void test_readObject_withPrimitiveClass() throws Exception {
        // Make sure that system properties are set correctly
        String dir = System.getProperty("java.io.tmpdir");
        if (dir == null)
            throw new Exception("System property java.io.tmpdir not defined.");
        File file = new File(dir, "test.ser");
        file.deleteOnExit();
        Test test = new Test();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
                file));
        out.writeObject(test);
        out.close();

        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        Test another = (Test) in.readObject();
        in.close();
        assertEquals(test, another);
    }

    public static class A implements Serializable {

        private static final long serialVersionUID = 11L;

        public String name = "name";
    }

    public static class B extends A {}

    public static class C extends B {

        private static final long serialVersionUID = 33L;
    }

    public static class A1 implements Serializable {

        static final long serialVersionUID = 5942584913446079661L;

        B1 b1 = new B1();

        B1 b2 = b1;

        Vector v = new Vector();
    }

    public static class B1 implements Serializable {

        int i = 5;

        Hashtable h = new Hashtable();
    }
   
    public class SerializableTestHelper implements Serializable {

        public String aField1;

        public String aField2;

        SerializableTestHelper() {
            aField1 = null;
            aField2 = null;
        }

        SerializableTestHelper(String s, String t) {
            aField1 = s;
            aField2 = t;
        }

        private void readObject(ObjectInputStream ois) throws Exception {
            // note aField2 is not read
            ObjectInputStream.GetField fields = ois.readFields();
            aField1 = (String) fields.get("aField1", "Zap");
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            // note aField2 is not written
            ObjectOutputStream.PutField fields = oos.putFields();
            fields.put("aField1", aField1);
            oos.writeFields();
        }

        public String getText1() {
            return aField1;
        }

        public void setText1(String s) {
            aField1 = s;
        }

        public String getText2() {
            return aField2;
        }

        public void setText2(String s) {
            aField2 = s;
        }
    }


    class BasicObjectInputStream extends ObjectInputStream {
        public BasicObjectInputStream() throws IOException, SecurityException {
            super();
        }

        public BasicObjectInputStream(InputStream input)
                throws StreamCorruptedException, IOException {
            super(input);
        }

        public boolean enableResolveObject(boolean enable) 
                throws SecurityException {
            return super.enableResolveObject(enable);
        }
        
        public Object readObjectOverride() throws OptionalDataException,
                ClassNotFoundException, IOException {
            return super.readObjectOverride();
        }
        
        public void readStreamHeader() throws IOException,
                StreamCorruptedException {
            readStreamHeaderCalled = true;
            super.readStreamHeader();
        }

        public Class<?> resolveProxyClass(String[] interfaceNames) 
                throws IOException, ClassNotFoundException {
            return super.resolveProxyClass(interfaceNames);
        }
    }
    
    //Regression Test for JIRA-2249
    public static class ObjectOutputStreamWithWriteDesc extends
            ObjectOutputStream {
        public ObjectOutputStreamWithWriteDesc(OutputStream os)
                throws IOException {
            super(os);
        }

        public void writeClassDescriptor(ObjectStreamClass desc)
                throws IOException {
        }
    }

    public static class ObjectInputStreamWithReadDesc extends
            ObjectInputStream {
        private Class returnClass;

        public ObjectInputStreamWithReadDesc(InputStream is, Class returnClass)
                throws IOException {
            super(is);
            this.returnClass = returnClass;
        }

        public ObjectStreamClass readClassDescriptor() throws IOException,
                ClassNotFoundException {
            return ObjectStreamClass.lookup(returnClass);

        }
    }
    
    static class TestClassForSerialization implements Serializable {
        private static final long serialVersionUID = 1L;
    }


    // Regression Test for JIRA-2340
    public static class ObjectOutputStreamWithWriteDesc1 extends
            ObjectOutputStream {
        public ObjectOutputStreamWithWriteDesc1(OutputStream os)
                throws IOException {
            super(os);
        }

        public void writeClassDescriptor(ObjectStreamClass desc)
                throws IOException {
            super.writeClassDescriptor(desc);
        }
    }

    public static class ObjectInputStreamWithReadDesc1 extends
            ObjectInputStream {        

        public ObjectInputStreamWithReadDesc1(InputStream is)
                throws IOException {
            super(is);            
        }

        public ObjectStreamClass readClassDescriptor() throws IOException,
                ClassNotFoundException {
            return super.readClassDescriptor();
        }
    }
    
    // Regression test for Harmony-1921
    public static class ObjectInputStreamWithResolve extends ObjectInputStream {
        public ObjectInputStreamWithResolve(InputStream in) throws IOException {
            super(in);
        }

        protected Class<?> resolveClass(ObjectStreamClass desc)
                throws IOException, ClassNotFoundException {
            if (desc.getName().equals(
                    "org.apache.harmony.luni.tests.pkg1.TestClass")) {
                return org.apache.harmony.luni.tests.pkg2.TestClass.class;
            }
            return super.resolveClass(desc);
        }
    }
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "No IOException testing since this seems not to be thrown.",
        method = "resolveClass",
        args = {java.io.ObjectStreamClass.class}
    )
    public void test_resolveClass() throws Exception {
        org.apache.harmony.luni.tests.pkg1.TestClass to1 = new org.apache.harmony.luni.tests.pkg1.TestClass();
        to1.i = 555;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(to1);
        oos.flush();
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStreamWithResolve(bais);
        org.apache.harmony.luni.tests.pkg2.TestClass to2 = (org.apache.harmony.luni.tests.pkg2.TestClass) ois
                .readObject();

        if (to2.i != to1.i) {
            fail("Wrong object read. Expected val: " + to1.i + ", got: " + to2.i);
        }
    }
    
    static class ObjectInputStreamWithResolveObject extends ObjectInputStream {
        
        public static Integer intObj = Integer.valueOf(1000);
        
        public ObjectInputStreamWithResolveObject(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }
        
        protected Object resolveObject(Object obj) throws IOException {
            if(obj instanceof Integer){
                obj = intObj;
            }
            return super.resolveObject(obj);
        }        
    }
    
    /**
     * @tests java.io.ObjectInputStream#resolveObject(Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "resolveObject",
        args = {java.lang.Object.class}
    )
    public void test_resolveObjectLjava_lang_Object() throws Exception {
        // Write an Integer object into memory
        Integer original = new Integer(10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.flush();
        oos.close();

        // Read the object from memory
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStreamWithResolveObject ois = 
            new ObjectInputStreamWithResolveObject(bais);
        Integer actual = (Integer) ois.readObject();
        ois.close();

        // object should be resolved from 10 to 1000 
        assertEquals(ObjectInputStreamWithResolveObject.intObj, actual);
    }
    
    /**
     * @tests java.io.ObjectInputStream#readClassDescriptor()
     * @tests java.io.ObjectOutputStream#writeClassDescriptor(ObjectStreamClass)
     */
    @TestTargets(
            { 
                @TestTargetNew(
                        method = "readClassDescriptor",
                        args = {},
                        level = TestLevel.PARTIAL_COMPLETE
                ),
                @TestTargetNew(
                    method = "writeClassDescriptor",
                    args = {ObjectStreamClass.class},
                    clazz = ObjectOutputStream.class,
                    level = TestLevel.COMPLETE
              )
            }
    )    
    public void test_readClassDescriptor() throws IOException,
            ClassNotFoundException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStreamWithWriteDesc1 oos = new ObjectOutputStreamWithWriteDesc1(
                baos);
        ObjectStreamClass desc = ObjectStreamClass
        .lookup(TestClassForSerialization.class);
        oos.writeClassDescriptor(desc);
        oos.close();
        
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStreamWithReadDesc1 ois = new ObjectInputStreamWithReadDesc1(
                bais);
        Object obj = ois.readClassDescriptor();
        ois.close();
        assertEquals(desc.getClass(), obj.getClass());
        
        //eof
        bais = new ByteArrayInputStream(bytes);
        ExceptionalBufferedInputStream bis = new ExceptionalBufferedInputStream(
                bais);
        ois = new ObjectInputStreamWithReadDesc1(bis);

        bis.setEOF(true);
        
        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            ois.close();
        }
        
        //throw exception
        bais = new ByteArrayInputStream(bytes);
        bis = new ExceptionalBufferedInputStream(bais);
        ois = new ObjectInputStreamWithReadDesc1(bis);

        bis.setException(new IOException());

        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            ois.close();
        }

        //corrupt
        bais = new ByteArrayInputStream(bytes);
        bis = new ExceptionalBufferedInputStream(bais);
        ois = new ObjectInputStreamWithReadDesc1(bis);
        
        bis.setCorrupt(true);
        
        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            ois.close();
        }

    }
    
    static class ExceptionalBufferedInputStream extends BufferedInputStream {
        private boolean eof = false;
        private IOException exception = null; 
        private boolean corrupt = false; 
        
        public ExceptionalBufferedInputStream(InputStream in) {
            super(in);
        }
        
        public int read() throws IOException {
            if (exception != null) {
                throw exception;
            }
            
            if (eof) {
                return -1;
            }
            
            if (corrupt) {
                return 0;
            }
            return super.read();
        }
        
        public void setEOF(boolean eof) {
            this.eof = eof;
        }
        
        public void setException(IOException exception) {
            this.exception = exception;
        }
        
        public void setCorrupt(boolean corrupt) {
            this.corrupt = corrupt;
        }
    }

    // Regression Test for Harmony-2402
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "registerValidation",
        args = {java.io.ObjectInputValidation.class, int.class}
    )        
    public void test_registerValidation() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));

        try {
            ois.registerValidation(null, 256);
            fail("NotActiveException should be thrown");
        } catch (NotActiveException nae) {
            // expected
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        oos = new ObjectOutputStream(bao = new ByteArrayOutputStream());
    }
}


class Test implements Serializable {
    private static final long serialVersionUID = 1L;

    Class<?> classes[] = new Class[] { byte.class, short.class, int.class,
            long.class, boolean.class, char.class, float.class, double.class };

    public boolean equals(Object o) {
        if (!(o instanceof Test)) {
            return false;
        }
        return Arrays.equals(classes, ((Test) o).classes);
    }
}
