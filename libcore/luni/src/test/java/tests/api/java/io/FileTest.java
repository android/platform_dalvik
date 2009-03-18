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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import tests.support.Support_Exec;
import dalvik.annotation.AndroidOnly;
import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(File.class) 
public class FileTest extends junit.framework.TestCase {

    /** Location to store tests in */
    private File tempDirectory;

    /** Temp file that does exist */
    private File tempFile;

    /** File separator */
    private String slash = File.separator;

    public String fileString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_java_io_BufferedOutputStream\nTest_java_io_ByteArrayInputStream\nTest_java_io_ByteArrayOutputStream\nTest_java_io_DataInputStream\nTest_File\nTest_FileDescriptor\nTest_FileInputStream\nTest_FileNotFoundException\nTest_FileOutputStream\nTest_java_io_FilterInputStream\nTest_java_io_FilterOutputStream\nTest_java_io_InputStream\nTest_java_io_IOException\nTest_java_io_OutputStream\nTest_java_io_PrintStream\nTest_java_io_RandomAccessFile\nTest_java_io_SyncFailedException\nTest_java_lang_AbstractMethodError\nTest_java_lang_ArithmeticException\nTest_java_lang_ArrayIndexOutOfBoundsException\nTest_java_lang_ArrayStoreException\nTest_java_lang_Boolean\nTest_java_lang_Byte\nTest_java_lang_Character\nTest_java_lang_Class\nTest_java_lang_ClassCastException\nTest_java_lang_ClassCircularityError\nTest_java_lang_ClassFormatError\nTest_java_lang_ClassLoader\nTest_java_lang_ClassNotFoundException\nTest_java_lang_CloneNotSupportedException\nTest_java_lang_Double\nTest_java_lang_Error\nTest_java_lang_Exception\nTest_java_lang_ExceptionInInitializerError\nTest_java_lang_Float\nTest_java_lang_IllegalAccessError\nTest_java_lang_IllegalAccessException\nTest_java_lang_IllegalArgumentException\nTest_java_lang_IllegalMonitorStateException\nTest_java_lang_IllegalThreadStateException\nTest_java_lang_IncompatibleClassChangeError\nTest_java_lang_IndexOutOfBoundsException\nTest_java_lang_InstantiationError\nTest_java_lang_InstantiationException\nTest_java_lang_Integer\nTest_java_lang_InternalError\nTest_java_lang_InterruptedException\nTest_java_lang_LinkageError\nTest_java_lang_Long\nTest_java_lang_Math\nTest_java_lang_NegativeArraySizeException\nTest_java_lang_NoClassDefFoundError\nTest_java_lang_NoSuchFieldError\nTest_java_lang_NoSuchMethodError\nTest_java_lang_NullPointerException\nTest_java_lang_Number\nTest_java_lang_NumberFormatException\nTest_java_lang_Object\nTest_java_lang_OutOfMemoryError\nTest_java_lang_RuntimeException\nTest_java_lang_SecurityManager\nTest_java_lang_Short\nTest_java_lang_StackOverflowError\nTest_java_lang_String\nTest_java_lang_StringBuffer\nTest_java_lang_StringIndexOutOfBoundsException\nTest_java_lang_System\nTest_java_lang_Thread\nTest_java_lang_ThreadDeath\nTest_java_lang_ThreadGroup\nTest_java_lang_Throwable\nTest_java_lang_UnknownError\nTest_java_lang_UnsatisfiedLinkError\nTest_java_lang_VerifyError\nTest_java_lang_VirtualMachineError\nTest_java_lang_vm_Image\nTest_java_lang_vm_MemorySegment\nTest_java_lang_vm_ROMStoreException\nTest_java_lang_vm_VM\nTest_java_lang_Void\nTest_java_net_BindException\nTest_java_net_ConnectException\nTest_java_net_DatagramPacket\nTest_java_net_DatagramSocket\nTest_java_net_DatagramSocketImpl\nTest_java_net_InetAddress\nTest_java_net_NoRouteToHostException\nTest_java_net_PlainDatagramSocketImpl\nTest_java_net_PlainSocketImpl\nTest_java_net_Socket\nTest_java_net_SocketException\nTest_java_net_SocketImpl\nTest_java_net_SocketInputStream\nTest_java_net_SocketOutputStream\nTest_java_net_UnknownHostException\nTest_java_util_ArrayEnumerator\nTest_java_util_Date\nTest_java_util_EventObject\nTest_java_util_HashEnumerator\nTest_java_util_Hashtable\nTest_java_util_Properties\nTest_java_util_ResourceBundle\nTest_java_util_tm\nTest_java_util_Vector\n";

    private static String platformId = "Android"
            + System.getProperty("java.vm.version").replace('.', '-');

    {
        // Delete all old temporary files
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        String[] files = tempDir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(tempDir, files[i]);
            if (f.isDirectory()) {
                if (files[i].startsWith("hyts_resources"))
                    deleteTempFolder(f);
            }
            if (files[i].startsWith("hyts_") || files[i].startsWith("hyjar_"))
                new File(tempDir, files[i]).delete();
        }
    }

    private void deleteTempFolder(File dir) {
        String files[] = dir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(dir, files[i]);
            if (f.isDirectory())
                deleteTempFolder(f);
            else {
                f.delete();
            }
        }
        dir.delete();

    }

    /**
     * @tests java.io.File#File(java.io.File, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "File",
        args = {java.io.File.class, java.lang.String.class}
    )    
    public void test_ConstructorLjava_io_FileLjava_lang_String() throws Exception {
        String error;
        String dirName = System.getProperty("java.io.tmpdir");
        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", dirName);
        
        File d = new File(dirName);
        File f = new File(d, "input.tst");
        if (!dirName.regionMatches((dirName.length() - 1), slash, 0, 1))
            dirName += slash;
        dirName += "input.tst";
        error = String.format("Test 1: Incorrect file created: %s; %s expected.", f.getPath(), dirName);
        assertTrue(error, f.getPath().equals(dirName));

        String fileName = null;
        try {
            f = new File(d, fileName);
            fail("Test 2: NullPointerException expected.");
        } catch (NullPointerException e) {
        }

        d = null;
        f = new File(d, "input.tst");
        error = String.format("Test 3: Incorrect file created: %s; %s expected.", 
                f.getAbsolutePath(), dirName);
        assertTrue(error, f.getAbsolutePath().equals(dirName));

        // Regression test for Harmony-382
        File s = null;
        f = new File("/abc");
        d = new File(s, "/abc");
        assertEquals("Test 4: Incorrect file created;", 
                f.getAbsolutePath(), d.getAbsolutePath());
        
        System.setProperty("user.dir", oldUserDir);
    }

    /**
     * @tests java.io.File#File(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies File(java.lang.String) constructor.",
        method = "File",
        args = {java.lang.String.class}
    )    
    public void test_ConstructorLjava_lang_String() {
        // Test for method java.io.File(java.lang.String)
        String fileName = null;
        try {
            new File(fileName);
            fail("Test 1: NullPointerException expected.");
        } catch (NullPointerException e) {
        }

        fileName = System.getProperty("java.io.tmpdir");
        if (!fileName.regionMatches((fileName.length() - 1), slash, 0, 1))
            fileName += slash;
        fileName += "input.tst";

        File f = new File(fileName);
        assertTrue("Created incorrect file " + f.getPath(), f.getPath().equals(
                fileName));
    }

    /**
     * @tests java.io.File#File(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "File",
        args = {java.lang.String.class, java.lang.String.class}
    )    
    public void test_ConstructorLjava_lang_StringLjava_lang_String() {
        String error;
        String dirName = null;
        String fileName = "input.tst";

        String userDir = System.getProperty("java.io.tmpdir");
        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", userDir);
        
        File f = new File(dirName, fileName);
        if (!userDir.regionMatches((userDir.length() - 1), slash, 0, 1))
            userDir += slash;
        userDir += "input.tst";
        error = String.format("Test 1: Incorrect file created: %s; %s expected.", 
                f.getAbsolutePath(), userDir);
        assertTrue(error, f.getAbsolutePath().equals(userDir));

        dirName = System.getProperty("java.io.tmpdir");
        fileName = null;
        try {
            f = new File(dirName, fileName);
            fail("Test 2: NullPointerException expected.");
        } catch (NullPointerException e) {
            // Expected.
        }

        fileName = "input.tst";
        f = new File(dirName, fileName);
        assertTrue("Test 3: Incorrect file created.", f.getPath()
                .equals(userDir));

        // Regression test for Harmony-382
        String s = null;
        f = new File("/abc");
        File d = new File(s, "/abc");
        assertEquals("Test 4: Incorrect file created;", d.getAbsolutePath(), f
                .getAbsolutePath());
        assertEquals("Test3: Created Incorrect File", "/abc", f
                .getAbsolutePath());

        System.setProperty("user.dir", oldUserDir);
    }

    /**
     * @tests java.io.File#File(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "File",
        args = {java.lang.String.class, java.lang.String.class}
    )
    @AndroidOnly("Test 3 incorrectly fails on the RI; Android is more " +
            "better at resolving path names.")
    public void test_ConstructorLjava_lang_StringLjava_lang_String_112270() {
        File ref1 = new File("/dir1/file1");

        File file1 = new File("/", "/dir1/file1");
        assertEquals("wrong result 1: " + file1, ref1.getPath(), file1
                .getPath());
        File file2 = new File("/", "//dir1/file1");
        assertTrue("wrong result 2: " + file2, file2.getPath().equals(
                ref1.getPath()));
        File file3 = new File("\\", "\\dir1\\file1");
        assertTrue("wrong result 3: " + file3, file3.getPath().equals(
                ref1.getPath()));
        File file4 = new File("\\", "\\\\dir1\\file1");
        assertTrue("wrong result 4: " + file4, file4.getPath().equals(
                ref1.getPath()));

        File ref2 = new File("/lib/content-types.properties");
        File file5 = new File("/", "lib/content-types.properties");
        assertTrue("wrong result 5: " + file5, file5.getPath().equals(
                ref2.getPath()));

    }

    /**
     * @tests java.io.File#File(java.io.File, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "File",
        args = {java.io.File.class, java.lang.String.class}
    )    
    @AndroidOnly("Test 3 incorrectly fails on the RI; Android is more " +
            "better at resolving path names.")
    public void test_ConstructorLjava_io_FileLjava_lang_String_112270() {
        File ref1 = new File("/dir1/file1");

        File root = new File("/");
        File file1 = new File(root, "/dir1/file1");
        assertTrue("wrong result 1: " + file1, file1.getPath().equals(
                ref1.getPath()));
        File file2 = new File(root, "//dir1/file1");
        assertTrue("wrong result 2: " + file2, file2.getPath().equals(
                ref1.getPath()));
        File file3 = new File(root, "\\dir1\\file1");
        assertTrue("wrong result 3: " + file3, file3.getPath().equals(
                ref1.getPath()));
        File file4 = new File(root, "\\\\dir1\\file1");
        assertTrue("wrong result 4: " + file4, file4.getPath().equals(
                ref1.getPath()));

        File ref2 = new File("/lib/content-types.properties");
        File file5 = new File(root, "lib/content-types.properties");
        assertTrue("wrong result 5: " + file5, file5.getPath().equals(
                ref2.getPath()));
    }

    /**
     * @tests java.io.File#File(java.net.URI)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies File(java.net.URI) constructor with incorrect parameter.",
        method = "File",
        args = {java.net.URI.class}
    )    
    public void test_ConstructorLjava_net_URI() {
        // Test for method java.io.File(java.net.URI)
        URI uri = null;
        try {
            new File(uri);
            fail("NullPointerException Not Thrown.");
        } catch (NullPointerException e) {
        }

        // invalid file URIs
        String[] uris = new String[] { "mailto:user@domain.com", // not
                // hierarchical
                "ftp:///path", // not file scheme
                "//host/path/", // not absolute
                "file://host/path", // non empty authority
                "file:///path?query", // non empty query
                "file:///path#fragment", // non empty fragment
                "file:///path?", "file:///path#" };

        for (int i = 0; i < uris.length; i++) {
            try {
                uri = new URI(uris[i]);
            } catch (URISyntaxException e) {
                fail("Unexpected exception:" + e);
            }
            try {
                new File(uri);
                fail("Expected IllegalArgumentException for new File(" + uri
                        + ")");
            } catch (IllegalArgumentException e) {
            }
        }

        // a valid File URI
        try {
            File f = new File(new URI("file:///pa%20th/another\u20ac/pa%25th"));
            assertTrue("Created incorrect File " + f.getPath(), f.getPath()
                    .equals(
                            slash + "pa th" + slash + "another\u20ac" + slash
                                    + "pa%th"));
        } catch (URISyntaxException e) {
            fail("Unexpected exception:" + e);
        } catch (IllegalArgumentException e) {
            fail("Unexpected exception:" + e);
        }
    }

    /**
     * @tests java.io.File#canRead()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "canRead",
        args = {}
    )    
    public void test_canRead() {
        // Test for method boolean java.io.File.canRead()
        // canRead only returns if the file exists so cannot be fully tested.
        File f = new File(System.getProperty("java.io.tmpdir"), platformId
                + "canRead.tst");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue("canRead returned false", f.canRead());
            f.delete();
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            f.delete();
        }
    }

    /**
     * @tests java.io.File#canWrite()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "canWrite",
        args = {}
    )    
    public void test_canWrite() {
        // Test for method boolean java.io.File.canWrite()
        // canWrite only returns if the file exists so cannot be fully tested.
        File f = new File(System.getProperty("java.io.tmpdir"), platformId
                + "canWrite.tst");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue("canWrite returned false", f.canWrite());
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            f.delete();
        }
    }

    /**
     * @tests java.io.File#compareTo(java.io.File)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies compareTo(java.io.File) method.",
        method = "compareTo",
        args = {java.io.File.class}
    )     
    public void test_compareToLjava_io_File() {
        File f1 = new File("thisFile.file");
        File f2 = new File("thisFile.file");
        File f3 = new File("thatFile.file");
        assertEquals("Equal files did not answer zero for compareTo", 0, f1
                .compareTo(f2));
        assertTrue("f3.compareTo(f1) did not result in value < 0", f3
                .compareTo(f1) < 0);
        assertTrue("f1.compareTo(f3) did not result in vale > 0", f1
                .compareTo(f3) > 0);
    }
    
    /**
     * @tests java.io.File#createNewFile()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "IOException checked.",
        method = "createNewFile",
        args = {}
    )      
    public void test_createNewFile_EmptyString() {
        File f = new File("");
        try {
            f.createNewFile();
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * @tests java.io.File#createNewFile()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "createNewFile",
        args = {}
    )     
    public void test_createNewFile() throws IOException {
        // Test for method java.io.File.createNewFile()
        String base = System.getProperty("java.io.tmpdir");
        boolean dirExists = true;
        int numDir = 1;
        File dir = new File(base, String.valueOf(numDir));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number
            // (making
            // it a new directory name.)
            if (dir.exists()) {
                numDir++;
                dir = new File(base, String.valueOf(numDir));
            } else {
                dirExists = false;
            }
        }

        // Test for trying to create a file in a directory that does not
        // exist.
        try {
            // Try to create a file in a directory that does not exist
            File f1 = new File(dir, "tempfile.tst");
            f1.createNewFile();
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }

        dir.mkdir();

        File f1 = new File(dir, "tempfile.tst");
        File f2 = new File(dir, "tempfile.tst");
        f1.deleteOnExit();
        f2.deleteOnExit();
        dir.deleteOnExit(); 
        assertFalse("Test 2: File should not exist.", f1.isFile());
        f1.createNewFile();
        assertTrue("Test 3: File should exist.", f1.isFile());
        assertTrue("Test 4: File should exist.", f2.isFile());
        String dirName = f1.getParent();
        if (!dirName.endsWith(slash))
            dirName += slash;
        assertTrue("Test 5: File saved in the wrong directory.", 
                dirName.equals(dir.getPath() + slash));
        assertEquals("Test 6: File saved with incorrect name;", 
                "tempfile.tst", f1.getName());

        // Test for creating a file that already exists.
        assertFalse("Test 7: File already exists, createNewFile should " +
                "return false.", f2.createNewFile());
        
        // Trying to create an illegal file.
        String sep = File.separator;
        f1 = new File(sep+"a"+sep+".."+sep+".."+sep);
        try {
            f1.createNewFile();
            fail("Test 8: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }
        
        f1 = new File(base);
        assertFalse("Test 9: False expected when trying to create an " +
                "existing file", f1.createNewFile());
    }

    /**
     * @tests java.io.File#createTempFile(java.lang.String, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "createTempFile",
        args = {java.lang.String.class, java.lang.String.class}
    )      
    @AndroidOnly("The RI does not throw an IOException when an illegal" +
            "file prefix is passed to createTempFile.")
    public void test_createTempFileLjava_lang_StringLjava_lang_String() {
        // Test for method java.io.File.createTempFile(String, String)
        // Error protection against using a suffix without a "."?
        File f1 = null;
        File f2 = null;
        try {
            f1 = File.createTempFile("hyts_abc", ".tmp");
            f2 = File.createTempFile("hyts_tf", null);
            String fileLocation = f1.getParent();
            if (!fileLocation.endsWith(slash))
                ;
            fileLocation += slash;
            String tempDir = System.getProperty("java.io.tmpdir");
            if (!tempDir.endsWith(slash))
                tempDir += slash;
            assertTrue(
                    "File did not save to the default temporary-file location.",
                    fileLocation.equals(tempDir));

            // Test to see if correct suffix was used to create the tempfile.
            File currentFile;
            String fileName;
            // Testing two files, one with suffix ".tmp" and one with null
            for (int i = 0; i < 2; i++) {
                currentFile = i == 0 ? f1 : f2;
                fileName = currentFile.getPath();
                assertTrue("File Created With Incorrect Suffix.", fileName
                        .endsWith(".tmp"));
            }

            // Tests to see if the correct prefix was used to create the
            // tempfiles.
            fileName = f1.getName();
            assertTrue("Test 1: File Created With Incorrect Prefix.", fileName
                    .startsWith("hyts_abc"));
            fileName = f2.getName();
            assertTrue("Test 2: File Created With Incorrect Prefix.", fileName
                    .startsWith("hyts_tf"));

            // Tests for creating a tempfile with a filename shorter than 3
            // characters.
            try {
                File f3 = File.createTempFile("ab", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
            }
            try {
                File f3 = File.createTempFile("a", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
            }
            try {
                File f3 = File.createTempFile("", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
            }
            try {
                // Providing an illegal file prefix.
                File f3 = File.createTempFile("/../../../../../", null);
                f3.delete();
                fail("IOException not thrown");
            } catch (IOException e) {
            }

        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            if (f1 != null)
                f1.delete();
            if (f2 != null)
                f2.delete();
        }
    }

    /**
     * @tests java.io.File#createTempFile(java.lang.String, java.lang.String,
     *        java.io.File)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "createTempFile",
        args = {java.lang.String.class, java.lang.String.class, java.io.File.class}
    )    
    public void test_createTempFileLjava_lang_StringLjava_lang_StringLjava_io_File() {
        // Test for method java.io.File.createTempFile(String, String, File)
        File f1 = null;
        File f2 = null;
        String base = System.getProperty("java.io.tmpdir");
        try {

            // Test to make sure that the tempfile was saved in the correct
            // location
            // and with the correct prefix/suffix.
            f1 = File.createTempFile("hyts_tf", null, null);
            File dir = new File(base);
            f2 = File.createTempFile("hyts_tf", ".tmp", dir);
            File currentFile;
            String fileLocation;
            String fileName;
            for (int i = 0; i < 2; i++) {
                currentFile = i == 0 ? f1 : f2;
                fileLocation = currentFile.getParent();
                if (!fileLocation.endsWith(slash))
                    fileLocation += slash;
                if (!base.endsWith(slash))
                    base += slash;
                assertTrue(
                        "File not created in the default temporary-file location.",
                        fileLocation.equals(base));
                fileName = currentFile.getName();
                assertTrue("File created with incorrect suffix.", fileName
                        .endsWith(".tmp"));
                assertTrue("File created with incorrect prefix.", fileName
                        .startsWith("hyts_tf"));
                currentFile.delete();
            }

            // Test for creating a tempfile in a directory that does not exist.
            int dirNumber = 1;
            boolean dirExists = true;
            // Set dir to a non-existent directory inside the temporary
            // directory
            dir = new File(base, String.valueOf(dirNumber));
            // Making sure that the directory does not exist.
            while (dirExists) {
                // If the directory exists, add one to the directory number
                // (making it
                // a new directory name.)
                if (dir.exists()) {
                    dirNumber++;
                    dir = new File(base, String.valueOf(dirNumber));
                } else {
                    dirExists = false;
                }
            }
            try {
                // Try to create a file in a directory that does not exist
                File f3 = File.createTempFile("hyts_tf", null, dir);
                f3.delete();
                fail("IOException not thrown");
            } catch (IOException e) {
            }
            dir.delete();

            // Tests for creating a tempfile with a filename shorter than 3
            // characters.
            try {
                File f4 = File.createTempFile("ab", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
            }
            try {
                File f4 = File.createTempFile("a", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
            }
            try {
                File f4 = File.createTempFile("", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
            }

        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            if (f1 != null)
                f1.delete();
            if (f2 != null)
                f1.delete();
        }
    }

    /**
     * @tests java.io.File#delete()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "delete",
        args = {}
    )        
    public void test_delete() {
        // Test for method boolean java.io.File.delete()
        try {
            File dir = new File(System.getProperty("java.io.tmpdir"), platformId
                    + "filechk");
            dir.mkdir();
            assertTrue("Directory Does Not Exist", dir.exists()
                    && dir.isDirectory());
            File f = new File(dir, "filechk.tst");
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue("Error Creating File For Delete Test", f.exists());
            dir.delete();
            assertTrue("Directory Should Not Have Been Deleted.", dir.exists());
            f.delete();
            assertTrue("File Was Not Deleted", !f.exists());
            dir.delete();
            assertTrue("Directory Was Not Deleted", !dir.exists());
        } catch (IOException e) {
            fail("Unexpected IOException During Delete Test : "
                    + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#equals(java.lang.Object)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies equals() method.",
        method = "equals",
        args = {java.lang.Object.class}
    )    
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean java.io.File.equals(java.lang.Object)
        File f1 = new File("filechk.tst");
        File f2 = new File("filechk.tst");
        File f3 = new File("xxxx");

        assertTrue("Equality test failed", f1.equals(f2));
        assertTrue("Files Should Not Return Equal.", !f1.equals(f3));

        f3 = new File("FiLeChK.tst");
        boolean onWindows = File.separatorChar == '\\';
        boolean onUnix = File.separatorChar == '/';
        if (onWindows)
            assertTrue("Files Should Return Equal.", f1.equals(f3));
        else if (onUnix)
            assertTrue("Files Should NOT Return Equal.", !f1.equals(f3));

        try {
            f1 = new File(System.getProperty("java.io.tmpdir"), "casetest.tmp");
            f2 = new File(System.getProperty("java.io.tmpdir"), "CaseTest.tmp");
            new FileOutputStream(f1).close(); // create the file
            if (f1.equals(f2)) {
                try {
                    new FileInputStream(f2);
                } catch (IOException e) {
                    fail("File system is case sensitive");
                }
            } else {
                boolean exception = false;
                try {
                    new FileInputStream(f2);
                } catch (IOException e) {
                    exception = true;
                }
                assertTrue("File system is case insensitive", exception);
            }
            f1.delete();
        } catch (IOException e) {
            fail("Unexpected using case sensitive test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#exists()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "exists",
        args = {}
    ) 
    public void test_exists() {
        // Test for method boolean java.io.File.exists()
        try {
            File f = new File(System.getProperty("java.io.tmpdir"), platformId
                    + "exists.tst");
            assertTrue("Exists returned true for non-existent file", !f
                    .exists());
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue("Exists returned false file", f.exists());
            f.delete();
        } catch (IOException e) {
            fail("Unexpected IOException During Test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#getAbsoluteFile()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "getAbsoluteFile",
        args = {}
    )     
    public void test_getAbsoluteFile() {
        // Test for method java.io.File getAbsoluteFile()
        String base = System.getProperty("java.io.tmpdir");
        if (!base.endsWith(slash))
            base += slash;
        File f = new File(base, "temp.tst");
        File f2 = f.getAbsoluteFile();
        assertEquals("Test 1: Incorrect File Returned.", 0, f2.compareTo(f
                .getAbsoluteFile()));
        f = new File(base + "Temp" + slash + slash + "temp.tst");
        f2 = f.getAbsoluteFile();
        assertEquals("Test 2: Incorrect File Returned.", 0, f2.compareTo(f
                .getAbsoluteFile()));
        f = new File(base + slash + ".." + slash + "temp.tst");
        f2 = f.getAbsoluteFile();
        assertEquals("Test 3: Incorrect File Returned.", 0, f2.compareTo(f
                .getAbsoluteFile()));
        f.delete();
        f2.delete();
    }

    /**
     * @tests java.io.File#getAbsolutePath()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "getAbsolutePath",
        args = {}
    )     
    public void test_getAbsolutePath() {
        // Test for method java.lang.String java.io.File.getAbsolutePath()
        String base = System.getProperty("java.io.tmpdir");
        if (!base.regionMatches((base.length() - 1), slash, 0, 1))
            base += slash;
        File f = new File(base, "temp.tst");
        assertTrue("Test 1: Incorrect path returned.", f.getAbsolutePath()
                .equals(base + "temp.tst"));
        f = new File(base + "Temp" + slash + slash + slash + "Testing" + slash
                + "temp.tst");
        assertTrue("Test 2: Incorrect path returned.", f.getAbsolutePath()
                .equals(base + "Temp" + slash + "Testing" + slash + "temp.tst"));
        f = new File(base + "a" + slash + slash + ".." + slash + "temp.tst");
        assertTrue("Test 3: Incorrect path returned." + f.getAbsolutePath(), f
                .getAbsolutePath().equals(
                        base + "a" + slash + ".." + slash + "temp.tst"));
        f.delete();
    }

    /**
     * @tests java.io.File#getCanonicalFile()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "IOException not checked since it may only occur if" +
                "the native access to the filesystem fails.",
        method = "getCanonicalFile",
        args = {}
    )     
    public void test_getCanonicalFile() {
        // Test for method java.io.File.getCanonicalFile()
        try {
            String base = System.getProperty("java.io.tmpdir");
            if (!base.endsWith(slash))
                base += slash;
            File f = new File(base, "temp.tst");
            File f2 = f.getCanonicalFile();
            assertEquals("Test 1: Incorrect file returned.", 0, f2
                    .getCanonicalFile().compareTo(f.getCanonicalFile()));
            f = new File(base + "Temp" + slash + slash + "temp.tst");
            f2 = f.getCanonicalFile();
            assertEquals("Test 2: Incorrect file returned.", 0, f2
                    .getCanonicalFile().compareTo(f.getCanonicalFile()));
            f = new File(base + "Temp" + slash + slash + ".." + slash
                    + "temp.tst");
            f2 = f.getCanonicalFile();
            assertEquals("Test 3: Incorrect file returned.", 0, f2
                    .getCanonicalFile().compareTo(f.getCanonicalFile()));

            // Test for when long directory/file names in Windows    
            boolean onWindows = File.separatorChar == '\\';
            // String userDir = System.getProperty("java.io.tmpdir");
            if (onWindows) {
                File testdir = new File(base, "long-" + platformId);
                testdir.mkdir();
                File dir = new File(testdir, "longdirectory" + platformId);
                try {
                    dir.mkdir();
                    f = new File(dir, "longfilename.tst");
                    f2 = f.getCanonicalFile();
                    assertEquals("Test 4: Incorrect file returned.",
                            0, f2.getCanonicalFile().compareTo(
                                    f.getCanonicalFile()));
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                    f2 = new File(testdir + slash + "longdi~1" + slash
                            + "longfi~1.tst");
                    File canonicalf2 = f2.getCanonicalFile();
                    /*
                     * If the "short file name" doesn't exist, then assume that
                     * the 8.3 file name compatibility is disabled.
                     */
                    if (canonicalf2.exists()) {
                    assertTrue("Test 5: Incorrect file returned: "
                            + canonicalf2, canonicalf2.compareTo(f
                            .getCanonicalFile()) == 0);
                    }
                } finally {
                    f.delete();
                    f2.delete();
                    dir.delete();
                    testdir.delete();
                }
            }
        } catch (IOException e) {
            fail ("Unexpected IOException during Test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#getCanonicalPath()
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "IOException not checked since it may only occur if" +
                "the native access to the filesystem fails.",
        method = "getCanonicalPath",
        args = {}
    )    
    public void test_getCanonicalPath() {
        // Test for method java.lang.String java.io.File.getCanonicalPath()
        // Should work for Unix/Windows.
        String dots = "..";
        String expected;
        String error;
        String tmpDir = System.getProperty("java.io.tmpdir");
        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tmpDir);
        try {
            String base = new File(tmpDir).getCanonicalPath();
            if (!base.regionMatches((base.length() - 1), slash, 0, 1))
                base += slash;
            File f = new File(base, "temp.tst");
            assertEquals("Test 1: Incorrect Path Returned.", base + "temp.tst", f
                    .getCanonicalPath());
            f = new File(base + "Temp" + slash + dots + slash + "temp.tst");
            assertEquals("Test 2: Incorrect Path Returned.", base + "temp.tst", f
                    .getCanonicalPath());

            // Finding a non-existent directory for tests 3 and 4
            // This is necessary because getCanonicalPath is case sensitive and
            // could
            // cause a failure in the test if the directory exists but with
            // different
            // case letters (e.g "Temp" and "temp")
            int dirNumber = 1;
            boolean dirExists = true;
            File dir1 = new File(base, String.valueOf(dirNumber));
            while (dirExists) {
                if (dir1.exists()) {
                    dirNumber++;
                    dir1 = new File(base, String.valueOf(dirNumber));
                } else {
                    dirExists = false;
                }
            }
            f = new File(base + dirNumber + slash + dots + slash + dirNumber + slash
                    + "temp.tst");
            assertEquals("Test 3: Incorrect path returned.", base + dirNumber + slash
                    + "temp.tst", f.getCanonicalPath());
            f = new File(base + dirNumber + slash + "Temp" + slash + dots + slash + "Test"
                    + slash + "temp.tst");
            assertEquals("Test 4: Incorrect path returned.", base + dirNumber + slash + "Test"
                    + slash + "temp.tst", f.getCanonicalPath());

            f = new File("1234.567");
            expected = System.getProperty("user.dir") + "/1234.567";
            error = String.format("Test 5: Incorrect path %s returned; %s expected.",
                    f.getCanonicalPath(), expected);
            assertTrue(error, f.getCanonicalPath().equals(expected));

        } catch (IOException e) {
            fail("Unexpected IOException During Test : " + e.getMessage());
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }

    /**
     * @tests java.io.File#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies getName() method.",
        method = "getName",
        args = {}
    )    
    public void test_getName() {
        // Test for method java.lang.String java.io.File.getName()
        File f = new File("name.tst");
        assertEquals("Test 1: Returned incorrect name", 
                "name.tst", f.getName());

        f = new File("");
        assertTrue("Test 2: Returned incorrect name", f.getName().equals(""));

        f.delete();
    }

    /**
     * @tests java.io.File#getParent()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies getParent() method.",
        method = "getParent",
        args = {}
    )     
    public void test_getParent() throws Exception {
        // Test for method java.lang.String java.io.File.getParent()
        String dir = System.getProperty("java.io.tmpdir");
        if (dir == null)
            throw new Exception("System property java.io.tmpdir not defined.");

        File f = new File("p.tst");
        assertNull("Test 1: Incorrect path returned", f.getParent());
        f = new File(dir, "p.tst");
        assertTrue("Test 2: Incorrect path returned", 
                   f.getParent().equals(dir));
        try {
            f.delete();
        } catch (Exception e) {
            fail("Unexpected exception during tests : " + e.getMessage());
        }

        File f1 = new File("/directory");
        assertTrue("Wrong parent test 1", f1.getParent().equals(slash));
        f1 = new File("/directory/file");
        assertTrue("Wrong parent test 2", f1.getParent().equals(
                slash + "directory"));
        f1 = new File("directory/file");
        assertEquals("Wrong parent test 3", "directory", f1.getParent());
        f1 = new File("/");
        assertNull("Wrong parent test 4", f1.getParent());
        f1 = new File("directory");
        assertNull("Wrong parent test 5", f1.getParent());

        if (File.separatorChar == '\\' && new File("d:/").isAbsolute()) {
            f1 = new File("d:/directory");
            assertTrue("Wrong parent test 1a", f1.getParent().equals(
                    "d:" + slash));
            f1 = new File("d:/directory/file");
            assertTrue("Wrong parent test 2a", f1.getParent().equals(
                    "d:" + slash + "directory"));
            f1 = new File("d:directory/file");
            assertEquals("Wrong parent test 3a", 
                    "d:directory", f1.getParent());
            f1 = new File("d:/");
            assertNull("Wrong parent test 4a", f1.getParent());
            f1 = new File("d:directory");
            assertEquals("Wrong parent test 5a", "d:", f1.getParent());
        }
    }

    /**
     * @tests java.io.File#getParentFile()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies getParentFile() method.",
        method = "getParentFile",
        args = {}
    )       
    public void test_getParentFile() {
        // Test for method java.io.File.getParentFile()
        File f = new File("tempfile.tst");
        assertNull("Incorrect path returned", f.getParentFile());
        f = new File(System.getProperty("java.io.tmpdir"), "tempfile1.tmp");
        File f2 = new File(System.getProperty("java.io.tmpdir"), "tempfile2.tmp");
        File f3 = new File(System.getProperty("java.io.tmpdir"), "/a/tempfile.tmp");
        assertEquals("Incorrect file returned", 0, f.getParentFile().compareTo(
                f2.getParentFile()));
        assertTrue("Incorrect file returned", f.getParentFile().compareTo(
                f3.getParentFile()) != 0);
        f.delete();
        f2.delete();
        f3.delete();
    }

    /**
     * @tests java.io.File#getPath()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies getPath() method.",
        method = "getPath",
        args = {}
    )    
    public void test_getPath() {
        String oldUserDir = System.getProperty("java.io.tmpdir");
        System.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
        String base = System.getProperty("user.dir");
        String fname;
        File f1;
        if (!base.regionMatches((base.length() - 1), slash, 0, 1))
            base += slash;
        fname = base + "filechk.tst";
        f1 = new File(base, "filechk.tst");
        File f2 = new File("filechk.tst");
        File f3 = new File("c:");
        File f4 = new File(base + "a" + slash + slash + ".." + slash
                + "filechk.tst");
        assertTrue("getPath returned incorrect path(f1) " + f1.getPath(), f1
                .getPath().equals(fname));
        assertTrue("getPath returned incorrect path(f2) " + f2.getPath(), f2
                .getPath().equals("filechk.tst"));
        assertTrue("getPath returned incorrect path(f3) " + f3.getPath(), f3
                .getPath().equals("c:"));
        assertTrue("getPath returned incorrect path(f4) " + f4.getPath(), f4
                .getPath().equals(
                        base + "a" + slash + ".." + slash + "filechk.tst"));
        f1.delete();
        f2.delete();
        f3.delete();
        f4.delete();
        System.setProperty("user.dir", oldUserDir);
    }

    /**
     * @tests java.io.File#isAbsolute()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies isAbsolute() method.",
        method = "isAbsolute",
        args = {}
    )      
    public void test_isAbsolute() {
        // Test for method boolean java.io.File.isAbsolute()
        if (File.separatorChar == '\\') {
            File f = new File("c:\\test");
            File f1 = new File("\\test");
            // One or the other should be absolute on Windows or CE
            assertTrue("Absolute returned false", (f.isAbsolute() && !f1
                    .isAbsolute())
                    || (!f.isAbsolute() && f1.isAbsolute()));
        } else {
            File f = new File("/test");
            assertTrue("Absolute returned false", f.isAbsolute());
        }
        assertTrue("Non-Absolute returned true", !new File("../test")
                .isAbsolute());
    }

    /**
     * @tests java.io.File#isDirectory()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "isDirectory",
        args = {}
    )     
    public void test_isDirectory() {
        // Test for method boolean java.io.File.isDirectory()

        String base = System.getProperty("java.io.tmpdir");
        if (!base.regionMatches((base.length() - 1), slash, 0, 1))
            base += slash;
        File f = new File(base);
        assertTrue("Test 1: Directory returned false", f.isDirectory());
        f = new File(base + "zxzxzxz" + platformId);
        assertTrue("Test 2: (Not created) directory returned true.", !f
                .isDirectory());
        f.mkdir();
        try {
            assertTrue("Test 3: Directory returned false.", f.isDirectory());
        } finally {
            f.delete();
        }
    }

    /**
     * @tests java.io.File#isFile()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "isFile",
        args = {}
    )     
    public void test_isFile() {
        // Test for method boolean java.io.File.isFile()
        try {
            String base = System.getProperty("java.io.tmpdir");
            File f = new File(base);
            assertTrue("Directory returned true as being a file.", !f.isFile());
            if (!base.regionMatches((base.length() - 1), slash, 0, 1))
                base += slash;
            f = new File(base, platformId + "amiafile");
            assertTrue("Non-existent file returned true", !f.isFile());
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue("File returned false", f.isFile());
            f.delete();
        } catch (IOException e) {
            fail("IOException during isFile " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#isHidden()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "isHidden",
        args = {}
    )      
    public void test_isHidden() {
        // Test for method boolean java.io.File.isHidden()
        boolean onUnix = File.separatorChar == '/';
        
        try {
            File f = File.createTempFile("hyts_", ".tmp");
            // On Unix hidden files are marked with a "." at the beginning
            // of the file name.
            if (onUnix) {
                File f2 = new File(System.getProperty("java.io.tmpdir"), ".test.tst" + platformId);
                FileOutputStream fos2 = new FileOutputStream(f2);
                fos2.close();
                assertTrue("File returned hidden on Unix", !f.isHidden());
                assertTrue("File returned visible on Unix", f2.isHidden());
                assertTrue("File did not delete.", f2.delete());
            } else {
                // For windows, the file is being set hidden by the attrib
                // command.
                Runtime r = Runtime.getRuntime();
                assertTrue("File returned hidden", !f.isHidden());
                Process p = r.exec("attrib +h \"" + f.getAbsolutePath() + "\"");
                p.waitFor();
                assertTrue("File returned visible", f.isHidden());
                p = r.exec("attrib -h \"" + f.getAbsolutePath() + "\"");
                p.waitFor();
                assertTrue("File returned hidden", !f.isHidden());
            }
            f.delete();
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException during test : "
                    + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#lastModified()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "lastModified",
        args = {}
    )      
    public void test_lastModified() {
        // Test for method long java.io.File.lastModified()
        try {
            File f = new File(System.getProperty("java.io.tmpdir"), platformId
                    + "lModTest.tst");
            f.delete();
            long lastModifiedTime = f.lastModified();
            assertEquals("LastModified Time Should Have Returned 0.",
                    0, lastModifiedTime);
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            f.setLastModified(315550800000L);
            lastModifiedTime = f.lastModified();
            assertTrue("LastModified Time Incorrect: " + lastModifiedTime,
                    lastModifiedTime == 315550800000L);
            f.delete();
            
            // Regression for Harmony-2146
            f = new File("/../");
            assertTrue(f.lastModified() > 0);
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#length()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "length",
        args = {}
    )      
    public void test_length() throws Exception {
        // Test for method long java.io.File.length()
        try {
            File f = new File(System.getProperty("java.io.tmpdir"), platformId
                    + "input.tst");
            assertEquals("File length should have returned 0.", 0, f.length());
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(fileString.getBytes());
            fos.close();
            assertTrue("Incorrect file length returned: " + f.length(), f
                    .length() == fileString.length());
            f.delete();
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        }
        
        // regression test for Harmony-1497
        File f = File.createTempFile("cts", ".tmp");
        f.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(f, "rwd");
        raf.write(0x41);
        assertEquals(1, f.length());
    }

    /**
     * @tests java.io.File#list()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "list",
        args = {}
    )         
    public void test_list() {
        // Test for method java.lang.String [] java.io.File.list()

        String base = System.getProperty("java.io.tmpdir");
        // Old test left behind "garbage files" so this time it creates a
        // directory
        // that is guaranteed not to already exist (and deletes it afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = null;
        dir = new File(base, platformId + String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        String[] flist = dir.list();

        assertNull("Method list() Should Have Returned null.", flist);

        assertTrue("Could not create parent directory for list test", dir
                .mkdir());

        String[] files = { "mtzz1.xx", "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
        try {
            assertEquals("Method list() Should Have Returned An Array Of Length 0.",
                    0, dir.list().length);

            File file = new File(dir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.",
                        file.list());
            } catch (IOException e) {
                fail("Unexpected IOException during test : " + e.getMessage());
            } finally {
                file.delete();
            }

            try {
                for (int i = 0; i < files.length; i++) {
                    File f = new File(dir, files[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }
            } catch (IOException e) {
                fail("Unexpected IOException during test : " + e.getMessage());
            }

            flist = dir.list();
            if (flist.length != files.length) {
                fail("Incorrect list returned");
            }

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++)
                check[i] = false;
            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].equals(files[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (check[i] == false)
                    checkCount++;
            }
            assertEquals("Invalid file returned in listing", 0, checkCount);

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }

            assertTrue("Could not delete parent directory for list test.", dir
                    .delete());
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }

    }

    /**
     * @tests java.io.File#listFiles()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "listFiles",
        args = {}
    )         
    public void test_listFiles() {
        // Test for method java.io.File.listFiles()

        try {
            String base = System.getProperty("java.io.tmpdir");
            // Finding a non-existent directory to create.
            int dirNumber = 1;
            boolean dirExists = true;
            File dir = new File(base, platformId + String.valueOf(dirNumber));
            // Making sure that the directory does not exist.
            while (dirExists) {
                // If the directory exists, add one to the directory number
                // (making
                // it a new directory name.)
                if (dir.exists()) {
                    dirNumber++;
                    dir = new File(base, String.valueOf(dirNumber));
                } else {
                    dirExists = false;
                }
            }
            // Test for attempting to cal listFiles on a non-existent directory.
            assertNull("listFiles Should Return Null.", dir.listFiles());

            assertTrue("Failed To Create Parent Directory.", dir.mkdir());

            String[] files = { "1.tst", "2.tst", "3.tst", "" };
            try {
                assertEquals("listFiles Should Return An Array Of Length 0.", 0, dir
                        .listFiles().length);

                File file = new File(dir, "notADir.tst");
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.close();
                    assertNull(
                            "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.",
                            file.listFiles());
                } catch (IOException e) {
                    fail("Unexpected IOException during test : " + e.getMessage());
                } finally {
                    file.delete();
                }

                for (int i = 0; i < (files.length - 1); i++) {
                    File f = new File(dir, files[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }

                new File(dir, "doesNotExist.tst");
                File[] flist = dir.listFiles();

                // Test to make sure that only the 3 files that were created are
                // listed.
                assertEquals("Incorrect Number Of Files Returned.",
                        3, flist.length);

                // Test to make sure that listFiles can read hidden files.
                boolean onUnix = File.separatorChar == '/';
                boolean onWindows = File.separatorChar == '\\';
                if (onWindows) {
                    files[3] = "4.tst";
                    File f = new File(dir, "4.tst");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec("attrib +h \"" + f.getPath() + "\"");
                    p.waitFor();
                }
                if (onUnix) {
                    files[3] = ".4.tst";
                    File f = new File(dir, ".4.tst");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }
                flist = dir.listFiles();
                assertEquals("Incorrect Number Of Files Returned.",
                        4, flist.length);

                // Checking to make sure the correct files were are listed in
                // the array.
                boolean[] check = new boolean[flist.length];
                for (int i = 0; i < check.length; i++)
                    check[i] = false;
                for (int i = 0; i < files.length; i++) {
                    for (int j = 0; j < flist.length; j++) {
                        if (flist[j].getName().equals(files[i])) {
                            check[i] = true;
                            break;
                        }
                    }
                }
                int checkCount = 0;
                for (int i = 0; i < check.length; i++) {
                    if (check[i] == false)
                        checkCount++;
                }
                assertEquals("Invalid file returned in listing", 0, checkCount);

                if (onWindows) {
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec("attrib -h \""
                            + new File(dir, files[3]).getPath() + "\"");
                    p.waitFor();
                }

                for (int i = 0; i < files.length; i++) {
                    File f = new File(dir, files[i]);
                    f.delete();
                }
                assertTrue("Parent Directory Not Deleted.", dir.delete());
            } finally {
                for (int i = 0; i < files.length; i++) {
                    File f = new File(dir, files[i]);
                    f.delete();
                }
                dir.delete();
            }
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.File#listFiles(java.io.FileFilter)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "listFiles",
        args = {java.io.FileFilter.class}
    )      
    public void test_listFilesLjava_io_FileFilter() {
        // Test for method java.io.File.listFiles(File Filter filter)
        
        String base = System.getProperty("java.io.tmpdir");
        // Finding a non-existent directory to create.
        int dirNumber = 1;
        boolean dirExists = true;
        File baseDir = new File(base, platformId + String.valueOf(dirNumber));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number (making
            // it a new directory name.)
            if (baseDir.exists()) {
                dirNumber++;
                baseDir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        // Creating a filter that catches directories.
        FileFilter dirFilter = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                else
                    return false;
            }
        };

        assertNull("listFiles Should Return Null.", baseDir
                .listFiles(dirFilter));

        assertTrue("Failed To Create Parent Directory.", baseDir.mkdir());

        File dir1 = null;
        String[] files = { "1.tst", "2.tst", "3.tst" };
        try {
            assertEquals("listFiles Should Return An Array Of Length 0.", 0, baseDir
                    .listFiles(dirFilter).length);

            File file = new File(baseDir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.",
                        file.listFiles(dirFilter));
            } catch (IOException e) {
                fail("Unexpected IOException During Test.");
            } finally {
                file.delete();
            }

            try {
                for (int i = 0; i < files.length; i++) {
                    File f = new File(baseDir, files[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }
            } catch (IOException e) {
                fail("Unexpected IOException during test : " + e.getMessage());
            }
            dir1 = new File(baseDir, "Temp1");
            dir1.mkdir();

            // Creating a filter that catches files.
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File f) {
                    if (f.isFile())
                        return true;
                    else
                        return false;
                }
            };

            // Test to see if the correct number of directories are returned.
            File[] directories = baseDir.listFiles(dirFilter);
            assertEquals("Incorrect Number Of Directories Returned.",
                    1, directories.length);

            // Test to see if the directory was saved with the correct name.
            assertEquals("Incorrect Directory Returned.", 0, directories[0]
                    .compareTo(dir1));

            // Test to see if the correct number of files are returned.
            File[] flist = baseDir.listFiles(fileFilter);
            assertTrue("Incorrect Number Of Files Returned.",
                    flist.length == files.length);

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++)
                check[i] = false;
            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].getName().equals(files[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (check[i] == false)
                    checkCount++;
            }
            assertEquals("Invalid file returned in listing", 0, checkCount);

            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir, files[i]);
                f.delete();
            }
            dir1.delete();
            assertTrue("Parent Directory Not Deleted.", baseDir.delete());
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir, files[i]);
                f.delete();
            }
            if (dir1 != null)
                dir1.delete();
            baseDir.delete();
        }
    }

    /**
     * @tests java.io.File#listFiles(java.io.FilenameFilter)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "listFiles",
        args = {java.io.FilenameFilter.class}
    )      
    public void test_listFilesLjava_io_FilenameFilter() {
        // Test for method java.io.File.listFiles(FilenameFilter filter)

        String base = System.getProperty("java.io.tmpdir");
        // Finding a non-existent directory to create.
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, platformId + String.valueOf(dirNumber));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number (making
            // it a new directory name.)
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, platformId + String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        // Creating a filter that catches "*.tst" files.
        FilenameFilter tstFilter = new FilenameFilter() {
            public boolean accept(File f, String fileName) {
                // If the suffix is ".tst" then send it to the array
                if (fileName.endsWith(".tst"))
                    return true;
                else
                    return false;
            }
        };

        assertNull("listFiles Should Return Null.",
                dir.listFiles(tstFilter));

        assertTrue("Failed To Create Parent Directory.", dir.mkdir());

        String[] files = { "1.tst", "2.tst", "3.tmp" };
        try {
            assertEquals("listFiles Should Return An Array Of Length 0.", 0, dir
                    .listFiles(tstFilter).length);

            File file = new File(dir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.",
                        file.listFiles(tstFilter));
            } catch (IOException e) {
                fail("Unexpected IOException during test : " + e.getMessage());
            } finally {
                file.delete();
            }

            try {
                for (int i = 0; i < files.length; i++) {
                    File f = new File(dir, files[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }
            } catch (IOException e) {
                fail("Unexpected IOException During Test : " + e.getMessage());
            }

            // Creating a filter that catches "*.tmp" files.
            FilenameFilter tmpFilter = new FilenameFilter() {
                public boolean accept(File f, String fileName) {
                    // If the suffix is ".tmp" then send it to the array
                    if (fileName.endsWith(".tmp"))
                        return true;
                    else
                        return false;
                }
            };

            // Tests to see if the correct number of files were returned.
            File[] flist = dir.listFiles(tstFilter);
            assertEquals("Incorrect Number Of Files Passed Through tstFilter.",
                    2, flist.length);
            for (int i = 0; i < flist.length; i++)
                assertTrue("File Should Not Have Passed The tstFilter.",
                        flist[i].getPath().endsWith(".tst"));

            flist = dir.listFiles(tmpFilter);
            assertEquals("Incorrect Number Of Files Passed Through tmpFilter.",
                    1, flist.length);
            assertTrue("File Should Not Have Passed The tmpFilter.", flist[0]
                    .getPath().endsWith(".tmp"));

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            assertTrue("Parent Directory Not Deleted.", dir.delete());
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    /**
     * @tests java.io.File#list(java.io.FilenameFilter)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "list",
        args = {java.io.FilenameFilter.class}
    )      
    public void test_listLjava_io_FilenameFilter() {
        // Test for method java.lang.String []
        // java.io.File.list(java.io.FilenameFilter)

        String base = System.getProperty("java.io.tmpdir");
        // Old test left behind "garbage files" so this time it creates a
        // directory
        // that is guaranteed not to already exist (and deletes it afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, platformId + String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.equals("mtzz1.xx");
            }
        };

        String[] flist = dir.list(filter);
        assertNull("Method list(FilenameFilter) Should Have Returned Null.",
                flist);

        assertTrue("Could not create parent directory for test", dir.mkdir());

        String[] files = { "mtzz1.xx", "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
        try {
            /*
             * Do not return null when trying to use list(Filename Filter) on a
             * file rather than a directory. All other "list" methods return
             * null for this test case.
             */
            /*
             * File file = new File(dir, "notADir.tst"); try { FileOutputStream
             * fos = new FileOutputStream(file); fos.close(); } catch
             * (IOException e) { fail("Unexpected IOException During
             * Test."); } flist = dir.list(filter); assertNull("listFiles
             * Should Have Returned Null When Used On A File Instead Of A
             * Directory.", flist); file.delete();
             */

            flist = dir.list(filter);
            assertEquals("Array Of Length 0 Should Have Returned.",
                    0, flist.length);

            try {
                for (int i = 0; i < files.length; i++) {
                    File f = new File(dir, files[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.close();
                }
            } catch (IOException e) {
                fail("Unexpected IOException during test : " + e.getMessage());
            }

            flist = dir.list(filter);

            if (flist.length != files.length - 1) {
                fail("Incorrect list returned");
            }

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++)
                check[i] = false;
            String[] wantedFiles = { "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
            for (int i = 0; i < wantedFiles.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].equals(wantedFiles[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (check[i] == false)
                    checkCount++;
            }
            assertEquals("Invalid file returned in listing", 0, checkCount);

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            assertTrue("Could not delete parent directory for test.", dir
                    .delete());
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    /**
     * @tests java.io.File#listRoots()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies listRoots() method.",
        method = "listRoots",
        args = {}
    )     
    public void test_listRoots() {
        // Test for method java.io.File.listRoots()

        File[] roots = File.listRoots();
        boolean onUnix = File.separatorChar == '/';
        boolean onWindows = File.separatorChar == '\\';
        if (onUnix) {
            assertEquals("Incorrect Number Of Root Directories.",
                    1, roots.length);
            String fileLoc = roots[0].getPath();
            assertTrue("Incorrect Root Directory Returned.", fileLoc
                    .startsWith(slash));
        } else if (onWindows) {
            // Need better test for Windows
            assertTrue("Incorrect Number Of Root Directories.",
                    roots.length > 0);
        }
    }

    /**
     * @tests java.io.File#mkdir()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "mkdir",
        args = {}
    )     
    public void test_mkdir() throws IOException {
        // Test for method boolean java.io.File.mkdir()

        String base = System.getProperty("java.io.tmpdir");
        // Old test left behind "garbage files" so this time it creates a
        // directory
        // that is guaranteed not to already exist (and deletes it afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }
        
        assertTrue("mkdir failed", dir.mkdir() && dir.exists());
        dir.deleteOnExit();

        String longDirName = "abcdefghijklmnopqrstuvwx";// 24 chars
        StringBuilder sb = new StringBuilder(dir + File.separator);
        StringBuilder sb2 = new StringBuilder(dir + File.separator);
        
        // Test make a long path
        while (dir.getCanonicalPath().length() < 256 - longDirName.length()) {
            sb.append(longDirName + File.separator);
            dir = new File(sb.toString());
            assertTrue("mkdir failed", dir.mkdir() && dir.exists());
            dir.deleteOnExit();
        }
        
        while (dir.getCanonicalPath().length() < 256) {
            sb.append(0);
            dir = new File(sb.toString());
            assertTrue("mkdir " + dir.getCanonicalPath().length() + " failed",
                    dir.mkdir() && dir.exists());
            dir.deleteOnExit();
        }
        
        // Test make many paths
        while (dir.getCanonicalPath().length() < 256) {
            sb2.append(0);
            dir = new File(sb2.toString());
            assertTrue("mkdir " + dir.getCanonicalPath().length() + " failed",
                    dir.mkdir() && dir.exists());
            dir.deleteOnExit();
        }     
    }

    /**
     * @tests java.io.File#mkdirs()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "mkdirs",
        args = {}
    )     
    public void test_mkdirs() {
        // Test for method boolean java.io.File.mkdirs()

        String userHome = System.getProperty("java.io.tmpdir");
        if (!userHome.endsWith(slash))
            userHome += slash;
        File f = new File(userHome + "mdtest" + platformId + slash + "mdtest2",
                "p.tst");
        File g = new File(userHome + "mdtest" + platformId + slash + "mdtest2");
        File h = new File(userHome + "mdtest" + platformId);
        f.mkdirs();
        try {
            assertTrue("Base Directory not created", h.exists());
            assertTrue("Directories not created", g.exists());
            assertTrue("File not created", f.exists());
        } finally {
            f.delete();
            g.delete();
            h.delete();
        }
    }

    /**
     * @tests java.io.File#renameTo(java.io.File)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "renameTo",
        args = {java.io.File.class}
    )     
    public void test_renameToLjava_io_File() {
        // Test for method boolean java.io.File.renameTo(java.io.File)
        String base = System.getProperty("java.io.tmpdir");
        File dir = new File(base, platformId);
        dir.mkdir();
        File f = new File(dir, "xxx.xxx");
        File rfile = new File(dir, "yyy.yyy");
        File f2 = new File(dir, "zzz.zzz");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(fileString.getBytes());
            fos.close();
            long lengthOfFile = f.length();

            rfile.delete(); // in case it already exists
            
            try {
                f.renameTo(null);
                fail("Test 1: NullPointerException expected.");
            } catch (NullPointerException e) {
                // Expected.
            }

            assertTrue("Test 2: File rename failed.", f.renameTo(rfile));
            assertTrue("Test 3: File rename failed.", rfile.exists());
            assertTrue("Test 4: Size Of File Changed.",
                    rfile.length() == lengthOfFile);

            fos = new FileOutputStream(rfile);
            fos.close();

            f2.delete(); // in case it already exists
            assertTrue("Test 5: File rename failed.", rfile.renameTo(f2));
            assertTrue("Test 6: File rename failed.", f2.exists());
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        } finally {
            f.delete();
            rfile.delete();
            f2.delete();
            dir.delete();
        }
    }

    /**
     * @tests java.io.File#setLastModified(long)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "setLastModified",
        args = {long.class}
    )      
    public void test_setLastModifiedJ() {
        // Test for method java.io.File.setLastModified()
        File f1 = null;
        try {
            // f1 = File.createTempFile("hyts_tf" , ".tmp");
            // jclRM does not include File.createTempFile
            f1 = new File(System.getProperty("java.io.tmpdir"), 
                          platformId + "hyts_tf_slm.tst");            
            f1.createNewFile();
            long orgTime = f1.lastModified();
            // Subtracting 100 000 milliseconds from the orgTime of File f1
            assertTrue(f1.setLastModified(orgTime - 100000));
            long lastModified = f1.lastModified();
            assertTrue("Test 1: LastModifed time incorrect: " + lastModified,
                    lastModified == (orgTime - 100000));
            // Subtracting 10 000 000 milliseconds from the orgTime of File f1
            assertTrue(f1.setLastModified(orgTime - 10000000));
            lastModified = f1.lastModified();
            assertTrue("Test 2: LastModifed time incorrect: " + lastModified,
                    lastModified == (orgTime - 10000000));
            // Adding 100 000 milliseconds to the orgTime of File f1
            assertTrue(f1.setLastModified(orgTime + 100000));
            lastModified = f1.lastModified();
            assertTrue("Test 3: LastModifed time incorrect: " + lastModified,
                    lastModified == (orgTime + 100000));
            // Adding 10 000 000 milliseconds from the orgTime of File f1
            assertTrue(f1.setLastModified(orgTime + 10000000));
            lastModified = f1.lastModified();
            assertTrue("Test 4: LastModifed time incorrect: " + lastModified,
                    lastModified == (orgTime + 10000000));
            // Trying to set time to an exact number
            assertTrue(f1.setLastModified(315550800000L));
            lastModified = f1.lastModified();
            assertTrue("Test 5: LastModified time incorrect: " + lastModified,
                    lastModified == 315550800000L);
            String osName = System.getProperty("os.name", "unknown");
            if (osName.equals("Windows 2000") || osName.equals("Windows NT")) {
                // Trying to set time to a large exact number
                boolean result = f1.setLastModified(4354837199000L);
                long next = f1.lastModified();
                // Dec 31 23:59:59 EST 2107 is overflow on FAT file systems, and
                // the call fails
                assertTrue("Test 6: LastModified time incorrect: " + next,
                        !result || next == 4354837199000L);
            }
            // Trying to set time to a negative number
            try {
                f1.setLastModified(-25);
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
            }
            
            File f2 = new File("/does not exist.txt");
            assertFalse(f2.setLastModified(42));
        } catch (IOException e) {
            fail("Unexpected IOException during test : " + e.getMessage());
        } finally {
            if (f1 != null)
                f1.delete();
        }
    }

    /**
     * @tests java.io.File#setReadOnly()
     */
    @TestTargets({
        @TestTargetNew(
                level = TestLevel.PARTIAL_COMPLETE,
                method = "setReadOnly",
                args = {}
        ),
        @TestTargetNew(
                level = TestLevel.PARTIAL_COMPLETE,
                method = "canWrite",
                args = {}
        )
    })
    @KnownFailure("canWrite() returns true even when a file is marked " +
            "read-only (Test 2). It is also possible to open this file " +
            "for writing (Test 3).")
    public void test_setReadOnly() {
        // Test for method java.io.File.setReadOnly()

        File f1 = null;
        File f2 = null;
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            f1 = File.createTempFile("hyts_tf", ".tmp");
            f2 = File.createTempFile("hyts_tf", ".tmp");
            
            assertTrue("Test 1: File is read-only." , f1.canWrite());
            f1.setReadOnly();
            assertTrue("Test 2: File is not read-only." , !f1.canWrite());
            
            try {
                // Attempt to write to a file that is read-only.
                new FileOutputStream(f1);
                fail("Test 3: IOException expected.");
            } catch (IOException e) {
                // Expected.
            }
            p = r.exec("chmod +w " + f1.getAbsolutePath());
            p.waitFor();

            assertTrue("Test 4: File is read-only." , f1.canWrite());
            try {
                FileOutputStream fos = new FileOutputStream(f1);
                fos.write(fileString.getBytes());
                fos.close();
                assertTrue("Test 5: Unable to write to the file.",
                        f1.length() == fileString.length());
            } catch (IOException e) {
                fail("Test 6: Unexpected IOException while attempting to " +
                        "write to the file. " + e);
            }
            assertTrue("Test 7: File has not been deleted.", f1.delete());

            // Assert is flawed because canWrite does not work.
            // assertTrue("File f2 Is Set To ReadOnly." , f2.canWrite());
            FileOutputStream fos = new FileOutputStream(f2);
            // Write to a file.
            fos.write(fileString.getBytes());
            fos.close();
            f2.setReadOnly();
            // Assert is flawed because canWrite does not work.
            // assertTrue("File f2 Is Not Set To ReadOnly." , !f2.canWrite());
            try {
                // Attempt to write to a file that has previously been written
                // to.
                // and is now set to read only.
                fos = new FileOutputStream(f2);
                fail("Test 8: IOException expected.");
            } catch (IOException e) {
            }
            p = r.exec("chmod +w " + f2.getAbsolutePath());
            p.waitFor();
            assertTrue("Test 9: File is read-only.", f2.canWrite());
            try {
                fos = new FileOutputStream(f2);
                fos.write(fileString.getBytes());
                fos.close();
            } catch (IOException e) {
                fail("Test 10: Unexpected IOException while attempting to " +
                        "write to the file. " + e);
            }
            f2.setReadOnly();
            assertTrue("Test 11: File has not been deleted.", f2.delete());
            // Similarly, trying to delete a read-only directory should succeed
            f2 = new File(System.getProperty("java.io.tmpdir"), "deltestdir");
            f2.mkdir();
            f2.setReadOnly();
            assertTrue("Test 12: Directory has not been deleted.", 
                    f2.delete());
            assertTrue("Test 13: Directory has not been deleted.", 
                    ! f2.exists());

        } catch (IOException e) {
            fail("Test 14: Unexpected IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            fail("Test 15: Unexpected InterruptedException: " + e);
        } finally {
            if (f1 != null)
                f1.delete();
            if (f2 != null)
                f2.delete();
        }
    }

    /**
     * @tests java.io.File#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Verifies toString() method.",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        // Test for method java.lang.String java.io.File.toString()
        String fileName = System.getProperty("java.io.tmpdir") + slash + "input.tst";
        File f = new File(fileName);
        assertTrue("Incorrect string returned", f.toString().equals(fileName));

        if (File.separatorChar == '\\') {
            String result = new File("c:\\").toString();
            assertTrue("Removed backslash: " + result, result.equals("c:\\"));
        }
    }

    /**
     * @tests java.io.File#toURI()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies toURI() method.",
        method = "toURI",
        args = {}
    )    
    public void test_toURI() {
        // Test for method java.io.File.toURI()
        try {
            // Need a directory that exists
            File dir = new File(System.getProperty("java.io.tmpdir"));

            // Test for toURI when the file is a directory.
            String newURIPath = dir.getAbsolutePath();
            newURIPath = newURIPath.replace(File.separatorChar, '/');
            if (!newURIPath.startsWith("/"))
                newURIPath = "/" + newURIPath;
            if (!newURIPath.endsWith("/"))
                newURIPath += '/';

            URI uri = dir.toURI();
            assertTrue("Test 1A: Incorrect URI Returned.", new File(uri)
                    .equals(dir.getAbsoluteFile()));
            assertTrue("Test 1B: Incorrect URI Returned.", uri.equals(new URI(
                    "file", null, newURIPath, null, null)));

            // Test for toURI with a file name with illegal chars.
            File f = new File(dir, "te% \u20ac st.tst");
            newURIPath = f.getAbsolutePath();
            newURIPath = newURIPath.replace(File.separatorChar, '/');
            if (!newURIPath.startsWith("/"))
                newURIPath = "/" + newURIPath;

            uri = f.toURI();
            assertTrue("Test 2A: Incorrect URI Returned.", new File(uri)
                    .equals(f.getAbsoluteFile()));
            assertTrue("Test 2B: Incorrect URI Returned.", uri.equals(new URI(
                    "file", null, newURIPath, null, null)));

            // Regression test for HARMONY-3207
            dir = new File(""); // current directory
            uri = dir.toURI();
            assertTrue("Test current dir: URI does not end with slash.",
                    uri.toString().endsWith("/"));
        } catch (URISyntaxException e1) {
            fail("Unexpected URISyntaxException: " + e1);
        }
    }

    /**
     * @tests java.io.File#toURI()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies that toURI() method works with URIs created with null parameters.",
        method = "toURI",
        args = {}
    )
    public void test_toURI2() {

        File f = new File(System.getProperty("java.io.tmpdir"), "a/b/c/../d/e/./f");

        String path = f.getAbsolutePath();
        path = path.replace(File.separatorChar, '/');
        if (!path.startsWith("/"))
            path = "/" + path;

        try {
            URI uri1 = new URI("file", null, path, null);
            URI uri2 = f.toURI();
            assertEquals("uris not equal", uri1, uri2);
        } catch (URISyntaxException e1) {
            fail("Unexpected URISyntaxException," + e1);
        }
    }

    /**
     * @tests java.io.File#toURL()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "toURL",
        args = {}
    )
    public void test_toURL() {
        // Test for method java.io.File.toURL()

        try {
            // Need a directory that exists
            File dir = new File(System.getProperty("java.io.tmpdir"));

            // Test for toURL when the file is a directory.
            String newDirURL = dir.getAbsolutePath();
            newDirURL = newDirURL.replace(File.separatorChar, '/');
            if (newDirURL.startsWith("/"))
                newDirURL = "file:" + newDirURL;
            else
                newDirURL = "file:/" + newDirURL;
            if (!newDirURL.endsWith("/"))
                newDirURL += '/';
            assertEquals("Test 1: Incorrect URL returned;", newDirURL,
                    dir.toURL().toString());

            // Test for toURL with a file.
            File f = new File(dir, "test.tst");
            String newURL = f.getAbsolutePath();
            newURL = newURL.replace(File.separatorChar, '/');
            if (newURL.startsWith("/"))
                newURL = "file:" + newURL;
            else
                newURL = "file:/" + newURL;
            assertEquals("Test 2: Incorrect URL returned;", newURL, 
                    f.toURL().toString());
        } catch (java.net.MalformedURLException e) {
            fail("Unexpected java.net.MalformedURLException during test.");
        }
    }

    /**
     * @tests java.io.File#toURL()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "toURL",
        args = {}
    )    
    public void test_toURL2() {

        File f = new File(System.getProperty("java.io.tmpdir"), "a/b/c/../d/e/./f");

        String path = f.getAbsolutePath();
        path = path.replace(File.separatorChar, '/');
        if (!path.startsWith("/"))
            path = "/" + path;

        try {
            URL url1 = new URL("file", "", path);
            URL url2 = f.toURL();
            assertEquals("urls not equal", url1, url2);
        } catch (MalformedURLException e) {
            fail("Unexpected MalformedURLException," + e);
        }
    }

    /**
     * @tests java.io.File#toURL()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "toURL",
        args = {}
    )
    @AndroidOnly("Incorrectly fails on the RI.")
    public void test_toURL3() throws MalformedURLException {
        File dir = new File(""); // current directory
        String newDirURL = dir.toURL().toString();
        assertTrue("Test 1: URL does not end with slash.",
                newDirURL.endsWith("/"));
    }

    /**
     * @tests java.io.File#deleteOnExit()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "deleteOnExit",
        args = {}
    )
    @AndroidOnly("This test only runs on Android because it instantiates " +
            "a second Dalvik VM.")
    public void test_deleteOnExit() throws IOException, InterruptedException {
        String cts = System.getProperty("java.io.tmpdir");
        File dir = new File(cts + "/hello");
        dir.mkdir();
        assertTrue(dir.exists());
        File subDir = new File(cts + "/hello/world");
        subDir.mkdir();
        assertTrue(subDir.exists());

        Support_Exec.execJava(new String[] {
                "tests.support.Support_DeleteOnExitTest",
                dir.getAbsolutePath(), subDir.getAbsolutePath() },
                new String[] {}, false);
        Thread.sleep(2000);
        assertFalse(dir.exists());
        assertFalse(subDir.exists());
    }
    
    /**
     * @tests serialization
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "regression test",
        method = "!Serialization",
        args = {}
    )     
    public void test_objectStreamClass_getFields() throws Exception {
        //Regression for HARMONY-2674
        ObjectStreamClass objectStreamClass = ObjectStreamClass
                .lookup(File.class);
        ObjectStreamField[] objectStreamFields = objectStreamClass.getFields();
        assertEquals(1, objectStreamFields.length);
        ObjectStreamField objectStreamField = objectStreamFields[0];
        assertEquals("path", objectStreamField.getName());
        assertEquals(String.class, objectStreamField.getType());
    }
    
    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() throws Exception {
        // Make sure that system properties are set correctly
        String userDir = System.getProperty("java.io.tmpdir");
        if (userDir == null)
            throw new Exception("System property java.io.tmpdir not defined.");
        System.setProperty("java.io.tmpdir", userDir);

        /** Setup the temporary directory */
        if (!userDir.regionMatches((userDir.length() - 1), slash, 0, 1))
            userDir += slash;
        tempDirectory = new File(userDir + "tempDir"
                + String.valueOf(System.currentTimeMillis()));
        if (!tempDirectory.mkdir())
            System.out.println("Setup for FileTest failed (1).");

        /** Setup the temporary file */
        tempFile = new File(tempDirectory, "tempfile");
        FileOutputStream tempStream;
        try {
            tempStream = new FileOutputStream(tempFile.getPath(), false);
            tempStream.close();
        } catch (IOException e) {
            System.out.println("Setup for FileTest failed (2).");
            return;
        }
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        if (tempFile.exists() && !tempFile.delete())
            System.out
                    .println("FileTest.tearDown() failed, could not delete file!");
        if (!tempDirectory.delete())
            System.out
                    .println("FileTest.tearDown() failed, could not delete directory!");
    }
}
