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
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import tests.support.resource.Support_Resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@TestTargetClass(ZipInputStream.class)
public class ZipInputStreamTest extends TestCase {
    // the file hyts_zipFile.zip used in setup needs to included as a resource
    private ZipEntry zentry;

    private ZipInputStream zis;

    private byte[] zipBytes;

    private byte[] dataBytes = "Some data in my file".getBytes();

    @Override
    protected void setUp() {
        try {
            InputStream is = Support_Resources.getStream("hyts_ZipFile.zip");
            if (is == null) {
                System.out.println("file hyts_ZipFile.zip can not be found");
            }
            zis = new ZipInputStream(is);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);
            ZipEntry entry = new ZipEntry("myFile");
            zos.putNextEntry(entry);
            zos.write(dataBytes);
            zos.closeEntry();
            zos.close();
            zipBytes = bos.toByteArray();
        } catch (Exception e) {
            System.out.println("Exception during ZipFile setup:");
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() {
        if (zis != null) {
            try {
                zis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * @tests java.util.zip.ZipInputStream#ZipInputStream(java.io.InputStream)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ZipInputStream",
        args = {java.io.InputStream.class}
    )
    public void test_ConstructorLjava_io_InputStream() throws Exception {
        zentry = zis.getNextEntry();
        zis.closeEntry();
    }

    /**
     * @tests java.util.zip.ZipInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "close",
        args = {}
    )
    public void test_close() {
        try {
            zis.close();
            byte[] rbuf = new byte[10];
            zis.read(rbuf, 0, 1);
        } catch (IOException e) {
            return;
        }
        fail("Read data after stream was closed");
    }

    /**
     * @tests java.util.zip.ZipInputStream#close()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Checks calling method two times",
        method = "close",
        args = {}
    )
    public void test_close2() throws Exception {
        // Regression for HARMONY-1101
        zis.close();
        // another call to close should NOT cause an exception
        zis.close();
    }

    /**
     * @tests java.util.zip.ZipInputStream#closeEntry()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "closeEntry",
        args = {}
    )
    public void test_closeEntry() throws Exception {
        zentry = zis.getNextEntry();
        zis.closeEntry();
        zentry = zis.getNextEntry();
        zis.close();
        try {
            zis.closeEntry();
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(new File(resources,
                "Broken_manifest.jar"));

        ZipInputStream zis1 = new ZipInputStream(fis);

        try {
            for (int i = 0; i < 6; i++) {
                zis1.getNextEntry();
                zis1.closeEntry();
            }
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "close",
        args = {}
    )
    @KnownFailure("after an exception has been thrown wile reading a "
            + "call to close also throws an exception.")
    public void test_closeAfterException() throws Exception {
        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(new File(resources,
                "Broken_manifest.jar"));

        ZipInputStream zis1 = new ZipInputStream(fis);

        try {
            for (int i = 0; i < 6; i++) {
                zis1.getNextEntry();
            }
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }

        zis1.close();
        try {
            zis1.getNextEntry();
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    /**
     * @tests java.util.zip.ZipInputStream#getNextEntry()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getNextEntry",
        args = {}
    )
    public void test_getNextEntry() throws Exception {
        assertNotNull("getNextEntry failed", zis.getNextEntry());

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(new File(resources,
                "Broken_manifest.jar"));

        ZipInputStream zis1 = new ZipInputStream(fis);

        try {
            for (int i = 0; i < 6; i++) {
                zis1.getNextEntry();
            }
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }

        try {
            zis1.close();  // Android throws exception here, already!
            zis1.getNextEntry();  // But RI here, only!
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    /**
     * @tests java.util.zip.ZipInputStream#read(byte[], int, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "read",
        args = {byte[].class, int.class, int.class}
    )
    public void test_read$BII() throws Exception {
        zentry = zis.getNextEntry();
        byte[] rbuf = new byte[(int) zentry.getSize()];
        int r = zis.read(rbuf, 0, rbuf.length);
        new String(rbuf, 0, r);
        assertEquals("Failed to read entry", 12, r);

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(new File(resources,
                "Broken_manifest.jar"));

        ZipInputStream zis1 = new ZipInputStream(fis);

        zis1.getNextEntry();
        zis1.getNextEntry();

        rbuf = new byte[100];

        try {
            zis1.read(rbuf, 10, 90);
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }

        try {
            zis1.close();  // Android throws exception here, already!
            zis1.read(rbuf, 10, 90);  // But RI here, only!
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    /**
     * @tests java.util.zip.ZipInputStream#skip(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "skip",
        args = {long.class}
    )
    public void test_skipJ() throws Exception {
        zentry = zis.getNextEntry();
        byte[] rbuf = new byte[(int) zentry.getSize()];
        zis.skip(2);
        int r = zis.read(rbuf, 0, rbuf.length);
        assertEquals("Failed to skip data", 10, r);

        zentry = zis.getNextEntry();
        zentry = zis.getNextEntry();
        long s = zis.skip(1025);
        assertTrue("invalid skip: " + s, s == 1025);

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(
                zipBytes));
        zis.getNextEntry();
        long skipLen = dataBytes.length / 2;
        assertEquals("Assert 0: failed valid skip", skipLen, zis.skip(skipLen));
        zis.skip(dataBytes.length);
        assertEquals("Assert 1: performed invalid skip", 0, zis.skip(1));
        assertEquals("Assert 2: failed zero len skip", 0, zis.skip(0));
        try {
            zis.skip(-1);
            fail("Assert 3: Expected Illegal argument exception");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(new File(resources,
                "Broken_manifest.jar"));

        ZipInputStream zis1 = new ZipInputStream(fis);

        zis1.getNextEntry();
        zis1.getNextEntry();

        try {
            zis1.skip(10);
            fail("ZipException expected");
        } catch (ZipException ee) {
            // expected
        }

        try {
            zis1.close();  // Android throws exception here, already!
            zis1.skip(10);  // But RI here, only!
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "available",
        args = {}
    )
    public void test_available() throws Exception {

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "hyts_ZipFile.zip");
        File fl = new File(resources, "hyts_ZipFile.zip");
        FileInputStream fis = new FileInputStream(fl);

        ZipInputStream zis1 = new ZipInputStream(fis);
        ZipEntry entry = zis1.getNextEntry();
        assertNotNull("No entry in the archive.", entry);
        long entrySize = entry.getSize();
        assertTrue("Entry size was < 1", entrySize > 0);
        int i = 0;
        for (i = 0; i < entrySize; i++) {
            zis1.skip(1);
            if (zis1.available() == 0) break;
        }
        if (i != entrySize) {
            fail("ZipInputStream.available or ZipInputStream.skip does not " +
                    "working properly. Only skipped " + i +
                    " bytes instead of " + entrySize);
        }
        zis1.skip(1);
        assertTrue(zis1.available() == 0);
        zis1.closeEntry();
        assertFalse(zis.available() == 0);
        zis1.close();
        try {
            zis1.available();
            fail("IOException expected");
        } catch (IOException ee) {
            // expected
        }
    }

    class Mock_ZipInputStream extends ZipInputStream {
        boolean createFlag = false;

        public Mock_ZipInputStream(InputStream arg0) {
            super(arg0);
        }

        boolean getCreateFlag() {
            return createFlag;
        }

        protected ZipEntry createZipEntry(String name) {
            createFlag = true;
            return super.createZipEntry(name);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "createZipEntry",
        args = {java.lang.String.class}
    )
    public void test_createZipEntryLjava_lang_String() throws Exception {

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "Broken_manifest.jar");
        File fl = new File(resources, "Broken_manifest.jar");
        FileInputStream fis = new FileInputStream(fl);

        Mock_ZipInputStream zis1 = new Mock_ZipInputStream(fis);
        assertFalse(zis1.getCreateFlag());
        zis1.getNextEntry();
        assertTrue(zis1.getCreateFlag());
    }
}
