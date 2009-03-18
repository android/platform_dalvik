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

package org.apache.harmony.archive.tests.java.util.jar;

import dalvik.annotation.KnownFailure;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import tests.support.Support_Exec;
import tests.support.resource.Support_Resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 
 * tests for various cases of java -jar ... execution with .zip files as args
 * some tests are just copy of JarExecTest ones
 */

@TestTargetClass(ZipOutputStream.class)
public class ZipExecTest extends junit.framework.TestCase {
    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "Regression functional test. Exception checking missed.",
        method = "putNextEntry",
        args = {java.util.zip.ZipEntry.class}
    )
    @KnownFailure("Maybe not a failure, but dalvikvm -jar is not supported (, as yet).")
    public void test_1562() throws Exception {
        Manifest man = new Manifest();
        Attributes att = man.getMainAttributes();
        att.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        att.put(Attributes.Name.MAIN_CLASS, "foo.bar.execjartest.Foo");

        File outputZip = File.createTempFile("hyts_", ".zip");
        outputZip.deleteOnExit();
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
                outputZip));
        File resources = Support_Resources.createTempFolder();

        for (String zipClass : new String[] {"Foo", "Bar"}) {
            zout.putNextEntry(new ZipEntry("foo/bar/execjartest/" + zipClass
                    + ".class"));
            zout.write(getResource(resources, "hyts_" + zipClass + ".ser"));
        }

        zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zout);
        zout.close();


        // set up the VM parameters
        String[] args = new String[] {"-jar", outputZip.getAbsolutePath()};

        // execute the JAR and read the result
        String res = Support_Exec.execJava(args, null, false);

        assertTrue("Error executing ZIP : result returned was incorrect.", res
                .startsWith("FOOBAR"));
    }

    /**
     * tests Class-Path entry in manifest
     * 
     * @throws Exception in case of troubles
     */
    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "Functional test.",
        method = "ZipOutputStream",
        args = {java.io.OutputStream.class}
    )
    @KnownFailure("Maybe not a failure, but dalvikvm -jar is not supported (, as yet).")
    public void test_zip_class_path() throws Exception {
        File fooZip = File.createTempFile("hyts_", ".zip");
        File barZip = File.createTempFile("hyts_", ".zip");
        fooZip.deleteOnExit();
        barZip.deleteOnExit();

        // create the manifest
        Manifest man = new Manifest();
        Attributes att = man.getMainAttributes();
        att.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        att.put(Attributes.Name.MAIN_CLASS, "foo.bar.execjartest.Foo");
        att.put(Attributes.Name.CLASS_PATH, barZip.getName());

        File resources = Support_Resources.createTempFolder();

        ZipOutputStream zoutFoo = new ZipOutputStream(new FileOutputStream(
                fooZip));
        zoutFoo.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zoutFoo);
        zoutFoo.putNextEntry(new ZipEntry("foo/bar/execjartest/Foo.class"));
        zoutFoo.write(getResource(resources, "hyts_Foo.ser"));
        zoutFoo.close();

        ZipOutputStream zoutBar = new ZipOutputStream(new FileOutputStream(
                barZip));
        zoutBar.putNextEntry(new ZipEntry("foo/bar/execjartest/Bar.class"));
        zoutBar.write(getResource(resources, "hyts_Bar.ser"));
        zoutBar.close();

        String[] args = new String[] {"-jar", fooZip.getAbsolutePath()};

        // execute the JAR and read the result
        String res = Support_Exec.execJava(args, null, false);

        assertTrue("Error executing JAR : result returned was incorrect.", res
                .startsWith("FOOBAR"));

        // rewrite manifest so it contains not only reference to bar but useless
        // entries as well
        att.put(Attributes.Name.CLASS_PATH, "xx yy zz " + barZip.getName());
        zoutFoo = new ZipOutputStream(new FileOutputStream(fooZip));
        zoutFoo.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zoutFoo);
        zoutFoo.putNextEntry(new ZipEntry("foo/bar/execjartest/Foo.class"));
        zoutFoo.write(getResource(resources, "hyts_Foo.ser"));
        zoutFoo.close();
        // execute the JAR and read the result
        res = Support_Exec.execJava(args, null, false);
        assertTrue("Error executing JAR : result returned was incorrect.", res
                .startsWith("FOOBAR"));


        // play with relative file names - put relative path as ../<parent dir
        // name>/xx.zip
        att.put(Attributes.Name.CLASS_PATH, ".." + File.separator
                + barZip.getParentFile().getName() + File.separator
                + barZip.getName());
        zoutFoo = new ZipOutputStream(new FileOutputStream(fooZip));
        zoutFoo.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zoutFoo);
        zoutFoo.putNextEntry(new ZipEntry("foo/bar/execjartest/Foo.class"));
        zoutFoo.write(getResource(resources, "hyts_Foo.ser"));
        zoutFoo.close();
        // execute the ZIP and read the result
        res = Support_Exec.execJava(args, null, false);
        assertTrue("Error executing JAR : result returned was incorrect.", res
                .startsWith("FOOBAR"));
    }


    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "Functional test.",
        method = "ZipOutputStream",
        args = {java.io.OutputStream.class}
    )
    @KnownFailure("Maybe not a failure, but dalvikvm -jar is not supported (, as yet).")
    public void test_zip_jar_mix() throws Exception {
        File fooJar = File.createTempFile("hyts_", ".jar");
        File barZip = File.createTempFile("hyts_", ".zip");
        fooJar.deleteOnExit();
        barZip.deleteOnExit();

        // create the manifest
        Manifest man = new Manifest();
        Attributes att = man.getMainAttributes();
        att.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        att.put(Attributes.Name.MAIN_CLASS, "foo.bar.execjartest.Foo");
        att.put(Attributes.Name.CLASS_PATH, barZip.getName());

        File resources = Support_Resources.createTempFolder();

        JarOutputStream joutFoo = new JarOutputStream(new FileOutputStream(
                fooJar), man);
        joutFoo.putNextEntry(new JarEntry("foo/bar/execjartest/Foo.class"));
        joutFoo.write(getResource(resources, "hyts_Foo.ser"));
        joutFoo.close();

        ZipOutputStream zoutBar = new ZipOutputStream(new FileOutputStream(
                barZip));
        zoutBar.putNextEntry(new ZipEntry("foo/bar/execjartest/Bar.class"));
        zoutBar.write(getResource(resources, "hyts_Bar.ser"));
        zoutBar.close();

        String[] args = new String[] {"-jar", fooJar.getAbsolutePath()};

        // execute the JAR and read the result
        String res = Support_Exec.execJava(args, null, false);

        assertTrue("Error executing JAR : result returned was incorrect.", res
                .startsWith("FOOBAR"));
    }

    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "Functional test.",
        method = "ZipOutputStream",
        args = {java.io.OutputStream.class}
    )
    @KnownFailure("Maybe not a failure, but dalvikvm -jar is not supported (, as yet).")
    public void test_zip_jar_mix_1() throws Exception {
        File fooZip = File.createTempFile("hyts_", ".zip");
        File barJar = File.createTempFile("hyts_", ".jar");
        fooZip.deleteOnExit();
        barJar.deleteOnExit();

        // create the manifest
        Manifest man = new Manifest();
        Attributes att = man.getMainAttributes();
        att.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        att.put(Attributes.Name.MAIN_CLASS, "foo.bar.execjartest.Foo");
        att.put(Attributes.Name.CLASS_PATH, barJar.getName());

        File resources = Support_Resources.createTempFolder();

        ZipOutputStream zoutFoo = new ZipOutputStream(new FileOutputStream(
                fooZip));
        zoutFoo.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zoutFoo);
        zoutFoo.putNextEntry(new ZipEntry("foo/bar/execjartest/Foo.class"));
        zoutFoo.write(getResource(resources, "hyts_Foo.ser"));
        zoutFoo.close();

        JarOutputStream joutBar = new JarOutputStream(new FileOutputStream(
                barJar));
        joutBar.putNextEntry(new ZipEntry("foo/bar/execjartest/Bar.class"));
        joutBar.write(getResource(resources, "hyts_Bar.ser"));
        joutBar.close();

        String[] args = new String[] {"-jar", fooZip.getAbsolutePath()};

        // execute the JAR and read the result
        String res = Support_Exec.execJava(args, null, false);

        assertTrue("Error executing ZIP : result returned was incorrect.", res
                .startsWith("FOOBAR"));
    }

    /**
     * tests case when Main-Class is not in the zip launched but in another zip
     * referenced by Class-Path
     * 
     * @throws Exception in case of troubles
     */
    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "Functional test.",
        method = "ZipOutputStream",
        args = {java.io.OutputStream.class}
    )
    @KnownFailure("Maybe not a failure, but dalvikvm -jar is not supported (, as yet).")
    public void test_main_class_in_another_zip() throws Exception {
        File fooZip = File.createTempFile("hyts_", ".zip");
        File barZip = File.createTempFile("hyts_", ".zip");
        fooZip.deleteOnExit();
        barZip.deleteOnExit();

        // create the manifest
        Manifest man = new Manifest();
        Attributes att = man.getMainAttributes();
        att.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        att.put(Attributes.Name.MAIN_CLASS, "foo.bar.execjartest.Foo");
        att.put(Attributes.Name.CLASS_PATH, fooZip.getName());

        File resources = Support_Resources.createTempFolder();

        ZipOutputStream zoutFoo = new ZipOutputStream(new FileOutputStream(
                fooZip));
        zoutFoo.putNextEntry(new ZipEntry("foo/bar/execjartest/Foo.class"));
        zoutFoo.write(getResource(resources, "hyts_Foo.ser"));
        zoutFoo.close();

        ZipOutputStream zoutBar = new ZipOutputStream(new FileOutputStream(
                barZip));
        zoutBar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        man.write(zoutBar);

        zoutBar.putNextEntry(new ZipEntry("foo/bar/execjartest/Bar.class"));
        zoutBar.write(getResource(resources, "hyts_Bar.ser"));
        zoutBar.close();

        String[] args = new String[] {"-jar", barZip.getAbsolutePath()};

        // execute the JAR and read the result
        String res = Support_Exec.execJava(args, null, false);

        assertTrue("Error executing JAR : result returned was incorrect.", res
                .startsWith("FOOBAR"));
    }


    private static byte[] getResource(File tempDir, String resourceName)
            throws IOException {
        Support_Resources.copyFile(tempDir, null, resourceName);
        File resourceFile = new File(tempDir, resourceName);
        resourceFile.deleteOnExit();

        // read whole resource data into memory
        byte[] resourceBody = new byte[(int) resourceFile.length()];
        FileInputStream fis = new FileInputStream(resourceFile);
        fis.read(resourceBody);
        fis.close();

        return resourceBody;
    }

}
