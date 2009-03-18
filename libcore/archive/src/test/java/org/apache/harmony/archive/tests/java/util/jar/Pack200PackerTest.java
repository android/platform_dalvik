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
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

@TestTargetClass(Pack200.Packer.class)
public class Pack200PackerTest extends TestCase {
    Packer packer;
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
        method = "pack",
        args = {java.util.jar.JarFile.class, java.io.OutputStream.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testPackJarFileOutputStream() throws IOException {
        File resources = Support_Resources.createTempFolder();
        //Use junit4.jar file for testing pack200 compressing rate.
        //file can be changed to any other.
        Support_Resources.copyFile(resources, null, "junit4-4.3.1.jar");
        File jarFile = new File(resources, "junit4-4.3.1.jar");
        JarFile jf = new JarFile(jarFile);

        File packFile1 = Support_Resources.createTempFile("pack200_1");
        File packFile2 = Support_Resources.createTempFile("pack200_2");
        File packFile3 = Support_Resources.createTempFile("pack200_3");
        FileOutputStream fos1 = new FileOutputStream(packFile1);
        FileOutputStream fos2 = new FileOutputStream(packFile2);
        FileOutputStream fos3 = new FileOutputStream(packFile3);
        properties.put(Packer.EFFORT, "0");
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
        assertTrue(jarFile.length()!=packFile1.length());
        assertTrue(packFile1.length()>packFile2.length());
        assertTrue(packFile2.length()>packFile3.length());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "pack",
        args = {java.util.jar.JarInputStream.class, java.io.OutputStream.class}
    )
    @KnownFailure("No Implementation in Android!")
    public void testPackJarInputStreamOutputStream() throws IOException {
        File resources = Support_Resources.createTempFolder();
        //Use junit4.jar file for testing pack200 compressing rate.
        //file can be changed to any other.
        Support_Resources.copyFile(resources, null, "junit4-4.3.1.jar");
        File jarFile = new File(resources, "junit4-4.3.1.jar");
        JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
    
        File packFile1 = Support_Resources.createTempFile("pack200_1");
        File packFile2 = Support_Resources.createTempFile("pack200_2");
        File packFile3 = Support_Resources.createTempFile("pack200_3");
        FileOutputStream fos1 = new FileOutputStream(packFile1);
        FileOutputStream fos2 = new FileOutputStream(packFile2);
        FileOutputStream fos3 = new FileOutputStream(packFile3);
        properties.put(Packer.EFFORT, "0");
        packer.pack(jis, fos1);
        fos1.close();
        jis = new JarInputStream(new FileInputStream(jarFile));
        properties.put(Packer.EFFORT, "1");
        packer.pack(jis, fos2);
        fos2.close();
        jis = new JarInputStream(new FileInputStream(jarFile));
        properties.put(Packer.EFFORT, "9");
        packer.pack(jis, fos3);
        fos3.close();
        assertTrue(jarFile.length()!=packFile1.length());
        assertTrue(packFile1.length()>packFile2.length());
        assertTrue(packFile2.length()>packFile3.length());
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
        packer.addPropertyChangeListener(pcl);
        assertFalse(pcl.isCalled());
        properties.put(Packer.EFFORT, "7");
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
        packer.addPropertyChangeListener(pcl);
        assertFalse(pcl.isCalled());
        packer.removePropertyChangeListener(pcl);
        properties.put(Packer.EFFORT, "7");
        assertFalse(pcl.isCalled());
    }

    @Override
    protected void setUp() {
        packer = Pack200.newPacker();
        properties = packer.properties();
    }

    @Override
    protected void tearDown() {
        packer = null;
        properties = null;
    }
}
