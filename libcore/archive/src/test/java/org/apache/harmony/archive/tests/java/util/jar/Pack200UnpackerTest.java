/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.apache.harmony.archive.tests.java.util.jar;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import junit.framework.TestCase;

import tests.support.resource.Support_Resources;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;

@TestTargetClass(Pack200.Unpacker.class)
public class Pack200UnpackerTest extends TestCase {
    Unpacker unpacker;
    Map properties;

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "properties",
        args = {}
    )
    @KnownFailure("No Implementation in Android!")
    public void testProperties() {
        assertTrue(properties.size()>0);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "unpack",
        args = {java.io.File.class, java.util.jar.JarOutputStream.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testUnpackInputStreamJarOutputStream() throws IOException {
        File resources = Support_Resources.createTempFolder();
        //Use junit4.jar file for testing pack200 compressing rate.
        //file can be changed to any other.
        Support_Resources.copyFile(resources, null, "junit4-4.3.1.jar");
        File jarFile = new File(resources, "junit4-4.3.1.jar");
        JarFile jf = new JarFile(jarFile);
        int jarEntries = jf.size();

        File packFile1 = Support_Resources.createTempFile("pack200_1");
        File packFile2 = Support_Resources.createTempFile("pack200_2");
        File packFile3 = Support_Resources.createTempFile("pack200_3");
        FileOutputStream fos1 = new FileOutputStream(packFile1);
        FileOutputStream fos2 = new FileOutputStream(packFile2);
        FileOutputStream fos3 = new FileOutputStream(packFile3);
        properties.put(Packer.EFFORT, "0");
        Packer packer = Pack200.newPacker();
        packer.pack(jf, fos1);
        jf.close();
        fos1.close();
        jf = new JarFile(jarFile);
        properties.put(Packer.EFFORT, "1");
        packer.pack(jf, fos2);
        jf.close();
        fos2.close();
        jf = new JarFile(jarFile);
        properties.put(Packer.EFFORT, "9");
        packer.pack(jf, fos3);
        jf.close();
        fos3.close();
        
        File jarFile1 = Support_Resources.createTempFile("jar_1");
        File jarFile2 = Support_Resources.createTempFile("jar_2");
        File jarFile3 = Support_Resources.createTempFile("jar_3");
        JarOutputStream jos1 = new JarOutputStream(new FileOutputStream(jarFile1));
        JarOutputStream jos2 = new JarOutputStream(new FileOutputStream(jarFile2));
        JarOutputStream jos3 = new JarOutputStream(new FileOutputStream(jarFile3));
        
        unpacker.unpack(packFile1, jos1);
        unpacker.unpack(packFile2, jos2);
        unpacker.unpack(packFile3, jos3);

        jos1.close();
        jos2.close();
        jos3.close();

        assertEquals(jarFile1.length(), jarFile2.length());
        assertEquals(jarFile2.length(), jarFile3.length());
        
        assertEquals(jarEntries, new JarFile(jarFile1).size());
        assertEquals(jarEntries, new JarFile(jarFile2).size());
        assertEquals(jarEntries, new JarFile(jarFile3).size());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "unpack",
        args = {java.io.InputStream.class, java.util.jar.JarOutputStream.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testUnpackFileJarOutputStream() throws IOException {
        File resources = Support_Resources.createTempFolder();
        //Use junit4.jar file for testing pack200 compressing rate.
        //file can be changed to any other.
        Support_Resources.copyFile(resources, null, "junit4-4.3.1.jar");
        File jarFile = new File(resources, "junit4-4.3.1.jar");
        JarFile jf = new JarFile(jarFile);
        int jarEntries = jf.size();

        File packFile1 = Support_Resources.createTempFile("pack200_1");
        File packFile2 = Support_Resources.createTempFile("pack200_2");
        File packFile3 = Support_Resources.createTempFile("pack200_3");
        FileOutputStream fos1 = new FileOutputStream(packFile1);
        FileOutputStream fos2 = new FileOutputStream(packFile2);
        FileOutputStream fos3 = new FileOutputStream(packFile3);
        properties.put(Packer.EFFORT, "0");
        Packer packer = Pack200.newPacker();
        packer.pack(jf, fos1);
        jf.close();
        fos1.close();
        jf = new JarFile(jarFile);
        properties.put(Packer.EFFORT, "1");
        packer.pack(jf, fos2);
        jf.close();
        fos2.close();
        jf = new JarFile(jarFile);
        properties.put(Packer.EFFORT, "9");
        packer.pack(jf, fos3);
        jf.close();
        fos3.close();
        
        File jarFile1 = Support_Resources.createTempFile("jar_1");
        File jarFile2 = Support_Resources.createTempFile("jar_2");
        File jarFile3 = Support_Resources.createTempFile("jar_3");
        JarOutputStream jos1 = new JarOutputStream(new FileOutputStream(jarFile1));
        JarOutputStream jos2 = new JarOutputStream(new FileOutputStream(jarFile2));
        JarOutputStream jos3 = new JarOutputStream(new FileOutputStream(jarFile3));
        FileInputStream fis1 = new FileInputStream(packFile1);
        FileInputStream fis2 = new FileInputStream(packFile2);
        FileInputStream fis3 = new FileInputStream(packFile3);
        
        unpacker.unpack(fis1, jos1);
        unpacker.unpack(fis2, jos2);
        unpacker.unpack(fis3, jos3);

        jos1.close();
        jos2.close();
        jos3.close();

        assertEquals(jarFile1.length(), jarFile2.length());
        assertEquals(jarFile2.length(), jarFile3.length());
        
        assertEquals(jarEntries, new JarFile(jarFile1).size());
        assertEquals(jarEntries, new JarFile(jarFile2).size());
        assertEquals(jarEntries, new JarFile(jarFile3).size());
    }

    class MyPCL implements PropertyChangeListener {
        boolean flag = false;
        
        public boolean isCalled() {
            return flag;
        }

        public void propertyChange(PropertyChangeEvent arg0) {
            flag = true;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addPropertyChangeListener",
        args = {java.beans.PropertyChangeListener.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testAddPropertyChangeListener() {
        MyPCL pcl = new MyPCL();
        unpacker.addPropertyChangeListener(pcl);
        assertFalse(pcl.isCalled());
        properties.put(Unpacker.PROGRESS, "0");
        assertTrue(pcl.isCalled());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "removePropertyChangeListener",
        args = {java.beans.PropertyChangeListener.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testRemovePropertyChangeListener() {
        MyPCL pcl = new MyPCL();
        unpacker.addPropertyChangeListener(pcl);
        assertFalse(pcl.isCalled());
        unpacker.removePropertyChangeListener(pcl);
        properties.put(Unpacker.PROGRESS, "7");
        assertFalse(pcl.isCalled());
    }

    @Override
    protected void setUp() {
        unpacker = Pack200.newUnpacker();
        properties = unpacker.properties();
    }

    @Override
    protected void tearDown() {
        unpacker = null;
        properties = null;
    }
}
