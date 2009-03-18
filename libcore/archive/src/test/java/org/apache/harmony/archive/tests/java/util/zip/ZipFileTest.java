/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.archive.tests.java.util.zip;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import tests.support.Support_PlatformFile;
import tests.support.resource.Support_Resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Permission;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@TestTargetClass(ZipFile.class)
public class ZipFileTest extends junit.framework.TestCase {

    public byte[] getAllBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int iRead;
        int off;
        while (is.available() > 0) {
            iRead = is.read(buf, 0, buf.length);
            if (iRead > 0) bs.write(buf, 0, iRead);
        }
        return bs.toByteArray();
    }

    // the file hyts_zipFile.zip in setup must be included as a resource
    private String tempFileName;

    private ZipFile zfile;

    // custom security manager
    SecurityManager sm = new SecurityManager() {
        final String forbidenPermissionAction = "read";



        public void checkPermission(Permission perm) {
            // only check if it's a FilePermission because Locale checks
            // for a PropertyPermission with action"read" to get system props.
            if (perm instanceof FilePermission 
                    && perm.getActions().equals(forbidenPermissionAction)) {
                throw new SecurityException();
            }
        }
    };

    /**
     * @tests java.util.zip.ZipFile#ZipFile(java.io.File)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "setUp procedure checks this type of constructor.",
        method = "ZipFile",
        args = {java.io.File.class}
    )
    public void test_ConstructorLjava_io_File() {
        // Test for method java.util.zip.ZipFile(java.io.File)
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.util.zip.ZipFile#ZipFile(java.io.File, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "ZipFile",
        args = {java.io.File.class, int.class}
    )
    public void test_ConstructorLjava_io_FileI() throws IOException {
        zfile.close(); // about to reopen the same temp file
        File file = new File(tempFileName);
        ZipFile zip = new ZipFile(file, ZipFile.OPEN_DELETE | ZipFile.OPEN_READ);
        zip.close();
        assertTrue("Zip should not exist", !file.exists());
        file = new File(tempFileName);
        file.delete();
        try {
            zip = new ZipFile(file, ZipFile.OPEN_READ);
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
        // IOException can not be checked throws ZipException in case of IO
        // problems.
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            file = new File(tempFileName);
            zip = new ZipFile(file, ZipFile.OPEN_READ);
            fail("SecurityException expected");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
        file = new File(tempFileName);
        try {
            zip = new ZipFile(file, -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ee) {
            // expected
        }
    }

    /**
     * @throws IOException
     * @tests java.util.zip.ZipFile#ZipFile(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test contains empty brackets.",
        method = "ZipFile",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() throws IOException {
        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        
        zfile.close(); // about to reopen the same temp file
        ZipFile zip = new ZipFile(tempFileName);
        zip.close();
        File file = File.createTempFile("zip", "tmp");
        try {
            zip = new ZipFile(file.getName());
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }
        file.delete();
        // IOException can not be checked throws ZipException in case of IO
        // problems.
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            zip = new ZipFile(tempFileName);
            fail("SecurityException expected");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
            System.setProperty("user.dir", oldUserDir);
        }
    }

    protected ZipEntry test_finalize1(ZipFile zip) {
        return zip.getEntry("File1.txt");
    }

    protected ZipFile test_finalize2(File file) throws IOException {
        return new ZipFile(file);
    }

    /**
     * @tests java.util.zip.ZipFile#finalize()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "finalize",
        args = {}
    )
    public void test_finalize() throws IOException {
        InputStream in = Support_Resources.getStream("hyts_ZipFile.zip");
        File file = Support_Resources.createTempFile(".jar");
        OutputStream out = new FileOutputStream(file);
        int result;
        byte[] buf = new byte[4096];
        while ((result = in.read(buf)) != -1) {
            out.write(buf, 0, result);
        }
        in.close();
        out.close();
        /*
         * ZipFile zip = new ZipFile(file); ZipEntry entry1 =
         * zip.getEntry("File1.txt"); assertNotNull("Did not find entry",
         * entry1); entry1 = null; zip = null;
         */

        assertNotNull("Did not find entry",
                test_finalize1(test_finalize2(file)));
        System.gc();
        System.gc();
        System.runFinalization();
        file.delete();
        assertTrue("Zip should not exist", !file.exists());
    }

    /**
     * @throws IOException
     * @tests java.util.zip.ZipFile#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "close",
        args = {}
    )
    public void test_close() throws IOException {
        // Test for method void java.util.zip.ZipFile.close()
        File fl = new File(tempFileName);
        ZipFile zf = new ZipFile(fl);
        InputStream is1 = zf.getInputStream(zf.getEntry("File1.txt"));
        InputStream is2 = zf.getInputStream(zf.getEntry("File2.txt"));

        is1.read();
        is2.read();

        zf.close();

        try {
            is1.read();
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }

        try {
            is2.read();
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    /**
     * @tests java.util.zip.ZipFile#entries()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "entries",
        args = {}
    )
    public void test_entries() {
        // Test for method java.util.Enumeration java.util.zip.ZipFile.entries()
        Enumeration<? extends ZipEntry> enumer = zfile.entries();
        int c = 0;
        while (enumer.hasMoreElements()) {
            ++c;
            enumer.nextElement();
        }
        assertTrue("Incorrect number of entries returned: " + c, c == 6);

        try {
            Enumeration<? extends ZipEntry> enumeration = zfile.entries();
            zfile.close();
            zfile = null;
            boolean pass = false;
            try {
                enumeration.hasMoreElements();
            } catch (IllegalStateException e) {
                pass = true;
            }
            assertTrue("did not detect closed jar file", pass);
        } catch (Exception e) {
            fail("Exception during entries test: " + e.toString());
        }
    }

    /**
     * @tests java.util.zip.ZipFile#getEntry(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "getEntry",
        args = {java.lang.String.class}
    )
    public void test_getEntryLjava_lang_String() throws IOException {
        // Test for method java.util.zip.ZipEntry
        // java.util.zip.ZipFile.getEntry(java.lang.String)
        java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
        assertNotNull("Could not obtain ZipEntry", zentry);
        int r;
        InputStream in;

        zentry = zfile.getEntry("testdir1/File1.txt");
        assertNotNull("Could not obtain ZipEntry: testdir1/File1.txt", zentry);
        zentry = zfile.getEntry("testdir1/");
        assertNotNull("Could not obtain ZipEntry: testdir1/", zentry);
        in = zfile.getInputStream(zentry);
        assertNotNull("testdir1/ should not have null input stream", in);
        r = in.read();
        in.close();
        assertEquals("testdir1/ should not contain data", -1, r);

        zentry = zfile.getEntry("testdir1/testdir1");
        assertNotNull("Could not obtain ZipEntry: testdir1/testdir1", zentry);
        in = zfile.getInputStream(zentry);
        byte[] buf = new byte[256];
        r = in.read(buf);
        in.close();
        assertEquals("incorrect contents", "This is also text", new String(buf,
                0, r));
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Strange test that succeeds in Android but not against RI.",
        method = "getEntry",
        args = {java.lang.String.class}
    )
    @BrokenTest("Needs investigation. AndroidOnly?")
    public void test_getEntryLjava_lang_String_AndroidOnly() throws IOException {
        java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
        assertNotNull("Could not obtain ZipEntry", zentry);
        int r;
        InputStream in;

        zentry = zfile.getEntry("testdir1");
        assertNotNull("Could not obtain ZipEntry: testdir1", zentry);
        in = zfile.getInputStream(zentry);
        assertNotNull("testdir1 should not have null input stream", in);
        r = in.read();
        in.close();
        assertEquals("testdir1 should not contain data", -1, r);
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "IllegalStateException checking.",
        method = "getEntry",
        args = {java.lang.String.class}
    )
    @KnownFailure("Android does not throw IllegalStateException when using "
            + "getEntry() after close().")
    public void test_getEntryLjava_lang_String_Ex() throws IOException {
        java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
        assertNotNull("Could not obtain ZipEntry", zentry);
        int r;
        InputStream in;

        zfile.close();
        try {
            zentry = zfile.getEntry("File2.txt");
            fail("IllegalStateException expected"); // Android fails here!
        } catch (IllegalStateException ee) {
            // expected
        }
    }

    /**
     * @throws IOException
     * @tests java.util.zip.ZipFile#getInputStream(java.util.zip.ZipEntry)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getInputStream",
        args = {java.util.zip.ZipEntry.class}
    )
    public void test_getInputStreamLjava_util_zip_ZipEntry() throws IOException {
        // Test for method java.io.InputStream
        // java.util.zip.ZipFile.getInputStream(java.util.zip.ZipEntry)
        ZipEntry zentry = null;
        InputStream is = null;
        try {
            zentry = zfile.getEntry("File1.txt");
            is = zfile.getInputStream(zentry);
            byte[] rbuf = new byte[1000];
            int r;
            is.read(rbuf, 0, r = (int) zentry.getSize());
            assertEquals("getInputStream read incorrect data", "This is text",
                    new String(rbuf, 0, r));
        } catch (java.io.IOException e) {
            fail("IOException during getInputStream");
        } finally {
            try {
                is.close();
            } catch (java.io.IOException e) {
                fail("Failed to close input stream");
            }
        }

        zentry = zfile.getEntry("File2.txt");
        zfile.close();
        try {
            is = zfile.getInputStream(zentry);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ee) {
            // expected
        }

        // ZipException can not be checked. Stream object returned or null.
    }

    /**
     * @tests java.util.zip.ZipFile#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public void test_getName() {
        // Test for method java.lang.String java.util.zip.ZipFile.getName()
        assertTrue("Returned incorrect name: " + zfile.getName(), zfile
                .getName().equals(tempFileName));
    }

    /**
     * @throws IOException
     * @tests java.util.zip.ZipFile#size()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "size",
        args = {}
    )
    @KnownFailure("IllegalStateException not thrown when using ZipFile.size() "
            + "after close().")
    public void test_size() throws IOException {
        assertEquals(6, zfile.size());
        zfile.close();
        try {
            zfile.size();
            fail("IllegalStateException expected"); // Android fails here!
        } catch (IllegalStateException ee) {
            // expected
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() {
        try {
            // Create a local copy of the file since some tests want to alter
            // information.
            tempFileName = System.getProperty("java.io.tmpdir");
            String separator = System.getProperty("file.separator");
            if (tempFileName.charAt(tempFileName.length() - 1) == separator
                    .charAt(0)) {
                tempFileName = Support_PlatformFile.getNewPlatformFile(
                        tempFileName, "gabba.zip");
            } else {
                tempFileName = Support_PlatformFile.getNewPlatformFile(
                        tempFileName + separator, "gabba.zip");
            }

            File f = new File(tempFileName);
            f.delete();
            InputStream is = Support_Resources.getStream("hyts_ZipFile.zip");
            FileOutputStream fos = new FileOutputStream(f);
            byte[] rbuf = getAllBytesFromStream(is);
            fos.write(rbuf, 0, rbuf.length);
            is.close();
            fos.close();
            zfile = new ZipFile(f);
        } catch (Exception e) {
            System.out.println("Exception during ZipFile setup:");
            e.printStackTrace();
        }
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    @Override
    protected void tearDown() {
        try {
            if (zfile != null) {
                // Note zfile is a user-defined zip file used by other tests and
                // should not be deleted
                zfile.close();
                tempFileName = System.getProperty("java.io.tmpdir");
                String separator = System.getProperty("file.separator");
                if (tempFileName.charAt(tempFileName.length() - 1) == separator
                        .charAt(0)) {
                    tempFileName = Support_PlatformFile.getNewPlatformFile(
                            tempFileName, "gabba.zip");
                } else {
                    tempFileName = Support_PlatformFile.getNewPlatformFile(
                            tempFileName + separator, "gabba.zip");
                }

                File f = new File(tempFileName);
                f.delete();
            }
        } catch (Exception e) {
        }
    }

}
