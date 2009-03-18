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

package org.apache.harmony.luni.tests.java.lang;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.nio.channels.spi.SelectorProvider;
import java.security.Permission;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

@TestTargetClass(System.class)
public class SystemTest extends junit.framework.TestCase {

    static boolean flag = false;

    static boolean ranFinalize = false;

    /**
     * @tests java.lang.System#setIn(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setIn",
        args = {java.io.InputStream.class}
    )
    public void test_setInLjava_io_InputStream() {
        InputStream orgIn = System.in;
        InputStream in = new ByteArrayInputStream(new byte[0]);
        System.setIn(in);
        assertTrue("in not set", System.in == in);
        System.setIn(orgIn);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("setIO")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.setIn(in);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#setOut(java.io.PrintStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setOut",
        args = {java.io.PrintStream.class}
    )
    public void test_setOutLjava_io_PrintStream() {
        PrintStream orgOut = System.out;
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        System.setOut(out);
        assertTrue("out not set", System.out == out);
        System.setOut(orgOut);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("setIO")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.setOut(out);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#setErr(java.io.PrintStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setErr",
        args = {java.io.PrintStream.class}
    )
    public void test_setErrLjava_io_PrintStream() {
        PrintStream orgErr = System.err;
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        System.setErr(err);
        assertTrue("err not set", System.err == err);
        System.setErr(orgErr);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("setIO")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.setErr(err);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#arraycopy(java.lang.Object, int,
     *        java.lang.Object, int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "arraycopy",
        args = {java.lang.Object.class, int.class, java.lang.Object.class, 
                int.class, int.class}
    )
    public void test_arraycopyLjava_lang_ObjectILjava_lang_ObjectII() {
        // Test for method void java.lang.System.arraycopy(java.lang.Object,
        // int, java.lang.Object, int, int)
        Integer a[] = new Integer[20];
        Integer b[] = new Integer[20];
        int i = 0;
        while (i < a.length) {
            a[i] = new Integer(i);
            ++i;
        }
        System.arraycopy(a, 0, b, 0, a.length);
        for (i = 0; i < a.length; i++)
            assertTrue("Copied elements incorrectly", a[i].equals(b[i]));

        /* Non primitive array types don't need to be identical */
        String[] source1 = new String[] { "element1" };
        Object[] dest1 = new Object[1];
        System.arraycopy(source1, 0, dest1, 0, dest1.length);
        assertTrue("Invalid copy 1", dest1[0] == source1[0]);

        char[][] source = new char[][] { { 'H', 'e', 'l', 'l', 'o' },
                { 'W', 'o', 'r', 'l', 'd' } };
        char[][] dest = new char[2][];
        System.arraycopy(source, 0, dest, 0, dest.length);
        assertTrue("Invalid copy 2", dest[0] == source[0]
                && dest[1] == source[1]);

        try {
            // copy from non array object into Object array
            System.arraycopy(new Object(), 0, b, 0, 0);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from Object array into non array object
            System.arraycopy(a, 0, new Object(), 0, 0);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from primitive array into object array
            System.arraycopy(new char[] {'a'}, 0, new String[1], 0, 1);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from object array into primitive array
            System.arraycopy(new String[] {"a"}, 0, new char[1], 0, 1);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from primitive array into an array of another primitive type
            System.arraycopy(new char[] {'a'}, 0, new int[1], 0, 1);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from object array into an array of another Object type
            System.arraycopy(new Character[] {'a'}, 0, new Integer[1], 0, 1);
            fail("ArrayStoreException is not thrown.");
        } catch(ArrayStoreException  ase) {
            //expected
        }

        try {
            // copy from null into an array of a primitive type
            System.arraycopy(null, 0, new int[1], 0, 1);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            // copy from a primitive array into null
            System.arraycopy(new int[]{'1'}, 0, null, 0, 1);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            System.arraycopy(a, a.length + 1, b, 0, 1);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, -1, b, 0, 1);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, 0, b, -1, 1);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, 0, b, 0, -1);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, 11, b, 0, 10);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, Integer.MAX_VALUE, b, 0, 10);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, 0, b, Integer.MAX_VALUE, 10);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }

        try {
            System.arraycopy(a, 0, b, 10, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException is not thrown.");
        } catch(IndexOutOfBoundsException ioobe) {
            //expected
        }
    }

    /**
     * @tests java.lang.System#currentTimeMillis()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "currentTimeMillis",
        args = {}
    )
    public void test_currentTimeMillis() {
        // Test for method long java.lang.System.currentTimeMillis()
        try {
            long firstRead = System.currentTimeMillis();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
            }
            long secondRead = System.currentTimeMillis();
            assertTrue("Incorrect times returned: " + firstRead + ", "
                    + secondRead, firstRead < secondRead);
        } catch (Exception e) {
            fail("Exception during test: " + e.toString());
        }
    }

    /**
     * @tests java.lang.System#exit(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies SecurityException.",
        method = "exit",
        args = {int.class}
    )
    public void test_exitI() {
        SecurityManager sm = new SecurityManager() {

            final String forbidenPermissionName = "user.dir";

            public void checkPermission(Permission perm) {
                if (perm.getName().equals(forbidenPermissionName)) {
                    throw new SecurityException();
                }
            }

            public void checkExit(int status) {
                if(status == -1)
                    throw new SecurityException();
            }

        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            System.exit(-1);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#getProperties()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getProperties",
        args = {}
    )
    public void test_getProperties() {

       // Test for method java.util.Properties java.lang.System.getProperties()
       /* String[] props = { "java.version", "java.vendor", "java.vendor.url",
                "java.home", "java.vm.specification.version",
                "java.vm.specification.vendor", "java.vm.specification.name",
                "java.vm.version", "java.vm.vendor", "java.vm.name",
                "java.specification.name", "java.specification.vendor",
                "java.specification.name", "java.class.version",
                "java.class.path", "java.ext.dirs", "os.name", "os.arch",
                "os.version", "file.separator", "path.separator",
                "line.separator", "user.name", "user.home", "user.dir", };
        */

        String [] props = {"java.vendor.url",
                "java.class.path", "user.home",
                "java.class.version", "os.version",
                "java.vendor", "user.dir",
                /*"user.timezone",*/ "path.separator",
                "os.name", "os.arch",
                "line.separator", "file.separator",
                "user.name", "java.version", "java.home" };

        /* Available properties.
        String [] props = {"java.boot.class.path", "java.class.path",
                "java.class.version", "java.compiler", "java.ext.dirs",
                "java.home", "java.io.tmpdir", "java.library.path",
                "java.vendor", "java.vendor.url", "java.version",
                "java.vm.name", "java.vm.specification.name",
                "java.vm.specification.vendor", "java.vm.specification.version",
                "java.vm.vendor", "java.vm.version", "java.specification.name",
                "java.specification.vendor", "java.specification.version",
                "os.arch", "os.name", "os.version", "user.home", "user.name",
                "user.dir", "file.separator", "line.separator",
                "path.separator", "java.runtime.name", "java.runtime.version",
                "java.vm.vendor.url", "file.encoding","user.language",
                "user.region"};
        */

        Properties p = System.getProperties();
        assertTrue(p.size() > 0);

        // Ensure spec'ed properties are non-null. See System.getProperties()
        // spec.

        for (int i = 0; i < props.length; i++) {
            assertNotNull("There is no property among returned properties: "
                    + props[i], p.getProperty(props[i]));
            assertNotNull("System property is null: " + props[i],
                    System.getProperty(props[i]));
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }

            public void checkPropertiesAccess() {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.getProperties();
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#getProperty(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getProperty",
        args = {java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_String() {
        // Test for method java.lang.String
        // java.lang.System.getProperty(java.lang.String)
        assertTrue("Failed to return correct property value", System
                .getProperty("line.separator").indexOf("\n", 0) >= 0);

        boolean is8859_1 = true;
        String encoding = System.getProperty("file.encoding");
        byte[] bytes = new byte[128];
        char[] chars = new char[128];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i + 128);
            chars[i] = (char) (i + 128);
        }
        String charResult = new String(bytes);
        byte[] byteResult = new String(chars).getBytes();
        if (charResult.length() == 128 && byteResult.length == 128) {
            for (int i = 0; i < bytes.length; i++) {
                if (charResult.charAt(i) != (char) (i + 128)
                        || byteResult[i] != (byte) (i + 128))
                    is8859_1 = false;
            }
        } else
            is8859_1 = false;
        String[] possibles = new String[] { "ISO8859_1", "8859_1", "ISO8859-1",
                "ISO-8859-1", "ISO_8859-1", "ISO_8859-1:1978", "ISO-IR-100",
                "LATIN1", "CSISOLATIN1" };
        boolean found8859_1 = false;
        for (int i = 0; i < possibles.length; i++) {
            if (possibles[i].equals(encoding)) {
                found8859_1 = true;
                break;
            }
        }
        assertTrue("Wrong encoding: " + encoding, !is8859_1 || found8859_1);

        try {
            System.getProperty(null);
            fail("NullPointerException should be thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            System.getProperty("");
            fail("IllegalArgumentException should be thrown.");
        } catch(IllegalArgumentException  iae) {
            //expected
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }

            @SuppressWarnings("unused")
            public void checkPropertyAccess(String key) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.getProperty("user.name");
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#getProperty(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_getPropertyLjava_lang_StringLjava_lang_String() {
        // Test for method java.lang.String
        // java.lang.System.getProperty(java.lang.String, java.lang.String)
        assertTrue("Failed to return correct property value: "
                + System.getProperty("line.separator", "99999"), System
                .getProperty("line.separator", "99999").indexOf("\n", 0) >= 0);
        assertEquals("Failed to return correct property value", "bogus", System
                .getProperty("bogus.prop", "bogus"));

        try {
            System.getProperty(null, "0.0");
            fail("NullPointerException should be thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            System.getProperty("", "0");
            fail("IllegalArgumentException should be thrown.");
        } catch(IllegalArgumentException  iae) {
            //expected
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }

            @SuppressWarnings("unused")
            public void checkPropertyAccess(String key) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.getProperty("java.version", "0");
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#setProperty(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setProperty",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void test_setPropertyLjava_lang_StringLjava_lang_String() {
        // Test for method java.lang.String
        // java.lang.System.setProperty(java.lang.String, java.lang.String)

        assertNull("Failed to return null", System.setProperty("testing",
                "value1"));
        assertTrue("Failed to return old value", System.setProperty("testing",
                "value2") == "value1");
        assertTrue("Failed to find value",
                System.getProperty("testing") == "value2");

        boolean exception = false;
        try {
            System.setProperty("", "default");
        } catch (IllegalArgumentException e) {
            exception = true;
        }
        assertTrue("Expected IllegalArgumentException", exception);
    }

    /**
     * @tests java.lang.System#getSecurityManager()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Doesn't check positive functionality.",
        method = "getSecurityManager",
        args = {}
    )
    public void test_getSecurityManager() {
        // Test for method java.lang.SecurityManager
        // java.lang.System.getSecurityManager()
        assertNull("Returned incorrect SecurityManager", System
                .getSecurityManager());
    }

    /**
     * @tests java.lang.System#identityHashCode(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "identityHashCode",
        args = {java.lang.Object.class}
    )
    public void test_identityHashCodeLjava_lang_Object() {
        // Test for method int
        // java.lang.System.identityHashCode(java.lang.Object)
        Object o = new Object();
        String s = "Gabba";
        assertEquals("Nonzero returned for null",
                0, System.identityHashCode(null));
        assertTrue("Nonequal has returned for Object", System
                .identityHashCode(o) == o.hashCode());
        assertTrue("Same as usual hash returned for String", System
                .identityHashCode(s) != s.hashCode());
    }

    /**
     * @throws IOException
     * @tests java.lang.System#inheritedChannel()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "inheritedChannel",
        args = {}
    )
    public void test_inheritedChannel() throws IOException {
        Channel iChannel = System.inheritedChannel();
        assertNull("Incorrect value of channel", iChannel);
        SelectorProvider sp = SelectorProvider.provider();
        assertEquals("Incorrect value of channel",
                sp.inheritedChannel(), iChannel);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("inheritedChannel")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.inheritedChannel();
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#runFinalization()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "runFinalization",
        args = {}
    )
    public void test_runFinalization() {
        // Test for method void java.lang.System.runFinalization()

        flag = true;
        createInstance();
        int count = 10;
        // the gc below likely bogosifies the test, but will have to do for
        // the moment
        while (!ranFinalize && count-- > 0) {
            System.gc();
            System.runFinalization();
        }
        assertTrue("Failed to run finalization", ranFinalize);
    }

    /**
     * @tests java.lang.System#runFinalizersOnExit(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "runFinalizersOnExit",
        args = {boolean.class}
    )
    @SuppressWarnings("deprecation")
    public void test_runFinalizersOnExitZ() {
        // Can we call the method at least?
        try {
            System.runFinalizersOnExit(false);
        } catch (Throwable t) {
            fail("Failed to set runFinalizersOnExit");
        }

        try {
            System.runFinalizersOnExit(true);
        } catch (Throwable t) {
            fail("Failed to set runFinalizersOnExit");
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }

            public void checkExit(int status) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.runFinalizersOnExit(true);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.System#setProperties(java.util.Properties)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setProperties",
        args = {java.util.Properties.class}
    )
    public void test_setPropertiesLjava_util_Properties() {
        // Test for method void
        // java.lang.System.setProperties(java.util.Properties)

        Properties orgProps = System.getProperties();
        java.util.Properties tProps = new java.util.Properties();
        tProps.put("test.prop", "this is a test property");
        tProps.put("bogus.prop", "bogus");
        System.setProperties(tProps);
        try {
            assertEquals("Failed to set properties", "this is a test property", System.getProperties()
                    .getProperty("test.prop"));
        } finally {
            // restore the original properties
            System.setProperties(orgProps);
        }
    }

    //Regression Test for Harmony-2356
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getenv",
        args = {}
    )
    public void testEnvUnmodifiable() {
        Map map = System.getenv();
        try {
            map.containsKey(null);
            fail("Should throw NullPointerExcepiton.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            map.containsKey(new Integer(10));
            fail("Should throw ClassCastException.");
        } catch (ClassCastException e) {
            // expected
        }

        try {
            map.containsValue(null);
            fail("Should throw NullPointerExcepiton.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            map.containsValue(new Integer(10));
            fail("Should throw ClassCastException.");
        } catch (ClassCastException e) {
            // expected
        }

        try {
            map.get(null);
            fail("Should throw NullPointerExcepiton.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            map.get(new Integer(10));
            fail("Should throw ClassCastException.");
        } catch (ClassCastException e) {
            // expected
        }

        try {
            map.put(null, "AAA");
            fail("Should throw UnsupportedOperationExcepiton.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            map.put("AAA", new Integer(10));
            fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            map.put("AAA", "BBB");
            fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            map.clear();
            fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            map.remove(null);
            fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

    }
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setSecurityManager",
            args = {java.lang.SecurityManager.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getSecurityManager",
            args = {}
        )
    })
    public void test_setSecurityManagerLjava_lang_SecurityManager() {
        assertEquals("Incorrect SecurityManager",
                null, System.getSecurityManager());
        try {
            SecurityManager localManager = new MockSecurityManager();
            System.setSecurityManager(localManager);
            assertEquals("Incorrect SecurityManager",
                    localManager, System.getSecurityManager());
        } finally {
            System.setSecurityManager(null);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clearProperty",
        args = {java.lang.String.class}
    )
    public void test_clearProperty() {
        System.setProperty("test", "value");
        System.clearProperty("test");
        assertNull("Property was not deleted.", System.getProperty("test"));

        try {
            System.clearProperty(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        try {
            System.clearProperty("");
            fail("IllegalArgumentException is not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if (perm.getName().equals("test")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            System.clearProperty("test");
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "gc",
        args = {}
    )
    public void test_gc() {
        Runtime rt =  Runtime.getRuntime();
        Vector<StringBuffer> vec = new Vector<StringBuffer>();
        long beforeTest = rt.freeMemory();
        while(rt.freeMemory() < beforeTest * 2/3) {
             vec.add(new StringBuffer(1000));
        }
        vec = null;
        long beforeGC = rt.freeMemory();
        System.gc();
        long afterGC = rt.freeMemory();
        assertTrue("memory was not released after calling System.gc()." +
                "before gc: " + beforeGC + "; after gc: " + afterGC,
                beforeGC < afterGC);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getenv",
        args = {}
    )
    public void test_getenv() {

        // String[] props = { "PATH", "HOME", "USER"};
        // only PATH of these three exists on android
        String[] props = { "PATH" };

        Map<String,String> envMap = System.getenv();
        assertFalse("environment map is empty.", envMap.isEmpty());
        assertTrue("env map contains less than 3 keys.",
                props.length < envMap.keySet().size());
        for(int i = 0; i < props.length; i++) {
           assertNotNull("There is no property: " + props[i],
                   envMap.get(props[i]));
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("getenv.*")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.getenv();
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getenv",
        args = {java.lang.String.class}
    )
    public void test_getenvLString() {

        assertNotNull("PATH environment variable is not found",
                  System.getenv("PATH"));

        assertNull("Doesn't return NULL for non existent property",
                  System.getenv("nonexistent.property"));

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("getenv.PATH")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);

        try {
            System.getenv("PATH");
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        try {
            System.getenv(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "load",
        args = {java.lang.String.class}
    )
    public void test_load() {
        try {
            new TestLibrary().checkString();
            fail("UnsatisfiedLinkError was not thrown.");
        } catch(UnsatisfiedLinkError  e) {
            //expected
        }

        try {
            System.load("nonExistentLibrary");
            fail("UnsatisfiedLinkError was not thrown.");
        } catch(UnsatisfiedLinkError ule) {
            //expected
        }

        try {
            System.load(null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {

            }

            public void checkLink(String lib) {
                throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            System.load("libTestLibrary.so");
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "loadLibrary",
        args = {java.lang.String.class}
    )
    public void test_loadLibrary() {

        try {
            System.loadLibrary("nonExistentLibrary");
            fail("UnsatisfiedLinkError was not thrown.");
        } catch(UnsatisfiedLinkError ule) {
            //expected
        }

        try {
            System.loadLibrary(null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }

            public void checkLink(String lib) {
                throw new SecurityException();
            }
         };

         SecurityManager oldSm = System.getSecurityManager();
         System.setSecurityManager(sm);
         try {
             System.loadLibrary("libTestLibrary.so");
             fail("SecurityException should be thrown.");
         } catch (SecurityException e) {
             // expected
         } finally {
             System.setSecurityManager(oldSm);
         }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "mapLibraryName",
        args = {java.lang.String.class}
    )
    public void test_mapLibraryName() {
        assertEquals("libname.so", System.mapLibraryName("name"));

        try {
            System.mapLibraryName(null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "nanoTime",
        args = {}
    )
    public void test_nanoTime() {
        long sleepTime = 5000;
        long beginTime = System.nanoTime();
        try {
            Thread.sleep(sleepTime);
        } catch(Exception e) {
            fail("Unknown exception was thrown.");
        }
        long endTime = System.nanoTime();
        assertTrue((endTime - beginTime) > sleepTime * 1000000);
    }

    @Override
    protected void setUp() {
        flag = false;
        ranFinalize = false;
    }

    protected SystemTest createInstance() {
        return new SystemTest("FT");
    }

    @Override
    protected void finalize() {
        if (flag)
            ranFinalize = true;
    }

    public SystemTest() {
    }

    public SystemTest(String name) {
        super(name);
    }

    private class MockSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            if (perm.equals(new RuntimePermission("inheritedChannel")))
                throw new SecurityException("Incorrect permission");
        }
    }
}
