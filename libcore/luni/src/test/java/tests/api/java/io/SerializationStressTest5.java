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

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestTargetNew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@TestTargetClass(java.io.Serializable.class) 
public class SerializationStressTest5 extends SerializationStressTest {

    transient Throwable current;

    // Use this for retrieving a list of any Throwable Classes that did not get
    // tested.
    transient Vector missedV = new Vector();

    transient Class[][] params = new Class[][] { { String.class },
            { Throwable.class }, { Exception.class },
            { String.class, Exception.class }, { String.class, int.class },
            { String.class, String.class, String.class },
            { String.class, Error.class },
            { int.class, boolean.class, boolean.class, int.class, int.class },
            {} };

    transient Object[][] args = new Object[][] {
            { "message" },
            { new Throwable() },
            { new Exception("exception") },
            { "message", new Exception("exception") },
            { "message", new Integer(5) },
            { "message", "message", "message" },
            { "message", new Error("error") },
            { new Integer(5), new Boolean(false), new Boolean(false),
                    new Integer(5), new Integer(5) }, {} };

    @TestTargetNew(
        level = TestLevel.ADDITIONAL,
        notes = "",
        method = "!Serialization:test ThrowableClasses",
        args = {}
    )        
    public void _test_writeObject_Throwables() {
        try {
            oos.close();
        } catch (IOException e) {
        }

        File javaDir = findJavaDir();

        Vector classFilesVector = new Vector();
        if (javaDir != null)
            findClassFiles(javaDir, classFilesVector);
        else
            findClassFilesFromZip(classFilesVector);

        if (classFilesVector.size() == 0) {
            fail("No Class Files Found.");
        }

        File[] classFilesArray = new File[classFilesVector.size()];
        classFilesVector.copyInto(classFilesArray);

        Class[] throwableClasses = findThrowableClasses(classFilesArray);
        findParam(throwableClasses);

        // Use this to print out the list of Throwable classes that weren't
        // tested.
        /*
         * System.out.println(); Class[] temp = new Class[missedV.size()];
         * missedV.copyInto(temp); for (int i = 0; i < temp.length; i++)
         * System.out.println(i+1 + ": " + temp[i].getName());
         */
    }

    private File[] makeClassPathArray() {
        String classPath;
        // if (System.getProperty("java.vendor").startsWith("IBM"))
        //     classPath = System.getProperty("org.apache.harmony.boot.class.path");
        // else
        //     classPath = System.getProperty("sun.boot.class.path");
        classPath = System.getProperty("java.boot.class.path");
        int instanceOfSep = -1;
        int nextInstance = classPath.indexOf(File.pathSeparatorChar,
                instanceOfSep + 1);
        Vector elms = new Vector();
        while (nextInstance != -1) {
            elms.add(new File(classPath.substring(instanceOfSep + 1,
                    nextInstance)));
            instanceOfSep = nextInstance;
            nextInstance = classPath.indexOf(File.pathSeparatorChar,
                    instanceOfSep + 1);
        }
        elms.add(new File(classPath.substring(instanceOfSep + 1)));
        File[] result = new File[elms.size()];
        elms.copyInto(result);
        return result;
    }

    private File findJavaDir() {
        File[] files = makeClassPathArray();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                String[] tempFileNames = files[i].list();
                for (int j = 0; j < tempFileNames.length; j++) {
                    File tempfile = new File(files[i], tempFileNames[j]);
                    if (tempfile.isDirectory()
                            && tempFileNames[j].equals("java")) {
                        String[] subdirNames = tempfile.list();
                        for (int k = 0; k < subdirNames.length; k++) {
                            File subdir = new File(tempfile, subdirNames[k]);
                            if (subdir.isDirectory()
                                    && subdirNames[k].equals("lang")) {
                                return tempfile;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void findClassFiles(File dir, Vector v) {
        String[] classFileNames = dir.list();
        for (int i = 0; i < classFileNames.length; i++) {
            File file = new File(dir, classFileNames[i]);
            if (file.isDirectory())
                findClassFiles(file, v);
            else if (classFileNames[i].endsWith(".class"))
                v.add(file);
        }
    }

    private Class[] findThrowableClasses(File[] files) {
        Class thrClass = Throwable.class;
        Vector resultVector = new Vector();
        String slash = System.getProperty("file.separator");
        String begTarget = slash + "java" + slash;
        String endTarget = ".class";
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getPath();
            int instOfBegTarget = fileName.indexOf(begTarget);
            int instOfEndTarget = fileName.indexOf(endTarget);
            fileName = fileName.substring(instOfBegTarget + 1, instOfEndTarget);
            fileName = fileName.replace(slash.charAt(0), '.');
            try {
                Class theClass = Class.forName(fileName, false, ClassLoader
                        .getSystemClassLoader());
                if (thrClass.isAssignableFrom(theClass)) {
                    // java.lang.VirtualMachineError is abstract.
                    // java.io.ObjectStreamException is abstract
                    // java.beans.PropertyVetoException needs a
                    // java.beans.PropertyChangeEvent as a parameter
                    if (!fileName.equals("java.lang.VirtualMachineError")
                            && !fileName
                                    .equals("java.io.ObjectStreamException")
                            && !fileName
                                    .equals("java.beans.PropertyVetoException"))
                        resultVector.add(theClass);
                }
            } catch (ClassNotFoundException e) {
                fail("ClassNotFoundException : " + fileName);
            }
        }
        Class[] result = new Class[resultVector.size()];
        resultVector.copyInto(result);
        return result;
    }

    private void initClass(Class thrC, int num) {
        Constructor[] cons = thrC.getConstructors();
        for (int i = 0; i < cons.length; i++) {
            try {
                Throwable obj = (Throwable) cons[i].newInstance(args[num]);
                t_Class(obj, num);
                break;
            } catch (IllegalArgumentException e) {
                // This error should be caught until the correct args is hit.
            } catch (IllegalAccessException e) {
                fail(
                        "IllegalAccessException while creating instance of: "
                                + thrC.getName());
            } catch (InstantiationException e) {
                fail(
                        "InstantiationException while creating instance of: "
                                + thrC.getName());
            } catch (InvocationTargetException e) {
                fail(
                        "InvocationTargetException while creating instance of: "
                                + thrC.getName());
            }
            if (i == cons.length - 1) {
                fail(
                        "Failed to create newInstance of: " + thrC.getName());
            }
        }
    }

    public String getDumpName() {
        if (current == null) {
            dumpCount++;
            return getName();
        }
        return getName() + "_" + current.getClass().getName();
    }

    private void t_Class(Throwable objToSave, int argsNum) {
        current = objToSave;
        Object objLoaded = null;
        try {
            if (DEBUG)
                System.out.println("Obj = " + objToSave);
            try {
                objLoaded = dumpAndReload(objToSave);
            } catch (FileNotFoundException e) {
                // Must be using xload, ignore missing Throwables
                System.out.println("Ignoring: "
                        + objToSave.getClass().getName());
                return;
            }

            // Has to have worked
            boolean equals;
            equals = objToSave.getClass().equals(objLoaded.getClass());
            assertTrue(MSG_TEST_FAILED + objToSave, equals);
            if (argsNum == 0 || (argsNum >= 3 && argsNum <= 7)) {
                equals = ((Throwable) objToSave).getMessage().equals(
                        ((Throwable) objLoaded).getMessage());
                assertTrue("Message Test: " + MSG_TEST_FAILED + objToSave,
                        equals);
            } else {
                // System.out.println(((Throwable)objToSave).getMessage());
                equals = ((Throwable) objToSave).getMessage() == null;
                assertTrue("Null Test 1: (args=" + argsNum + ") "
                        + MSG_TEST_FAILED + objToSave, equals);
                equals = ((Throwable) objLoaded).getMessage() == null;
                assertTrue("Null Test 2: (args=" + argsNum + ") "
                        + MSG_TEST_FAILED + objToSave, equals);
            }
        } catch (IOException e) {
            fail("Unexpected IOException in checkIt() : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            fail(e.toString() + " - testing " + objToSave.getClass().getName());
        }
    }

    private void findParam(Class[] thrC) {
        for (int i = 0; i < thrC.length; i++) {
            Constructor con = null;
            for (int j = 0; j < params.length; j++) {
                try {
                    con = thrC[i].getConstructor(params[j]);
                } catch (NoSuchMethodException e) {
                    // This Error will be caught until the right param is found.
                }

                if (con != null) {
                    // If the param was found, initialize the Class
                    initClass(thrC[i], j);
                    break;
                }
                // If the param not found then add to missed Vector.
                if (j == params.length - 1)
                    missedV.add(thrC[i]);
            }
        }
    }

    private void findClassFilesFromZip(Vector v) {
        String slash = System.getProperty("file.separator");
        String javaHome = System.getProperty("java.home");
        if (!javaHome.endsWith(slash))
            javaHome += slash;

        String[] wanted = { "java" + slash + "io", "java" + slash + "lang",
                "java" + slash + "math", "java" + slash + "net",
                "java" + slash + "security", "java" + slash + "text",
                "java" + slash + "util", "java" + slash + "beans",
                "java" + slash + "rmi",
                // One or more class files in awt make the VM hang after being
                // loaded.
                // "java" + slash + "awt",
                "java" + slash + "sql",
        // These are (possibly) all of the throwable classes in awt
        /*
         * "java\\awt\\AWTError", "java\\awt\\AWTException",
         * "java\\awt\\color\\CMMException",
         * "java\\awt\\color\\ProfileDataException",
         * "java\\awt\\datatransfer\\MimeTypeParseException",
         * "java\\awt\\datatransfer\\UnsupportedFlavorException",
         * "java\\awt\\dnd\\InvalidDnDOperationException",
         * "java\\awt\\FontFormatException",
         * "java\\awt\\geom\\IllegalPathStateException",
         * "java\\awt\\geom\\NoninvertibleTransformException",
         * "java\\awt\\IllegalComponentStateException",
         * "java\\awt\\image\\ImagingOpException",
         * "java\\awt\\image\\RasterFormatException",
         * "java\\awt\\print\\PrinterAbortException",
         * "java\\awt\\print\\PrinterException",
         * "java\\awt\\print\\PrinterIOException"
         */
        };

        File[] files = makeClassPathArray();
        FileInputStream fis = null;
        ZipInputStream zis = null;
        ZipEntry ze = null;
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getPath();
            if (files[i].exists() && files[i].isFile()
                    && fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                try {
                    fis = new FileInputStream(files[i].getPath());
                } catch (FileNotFoundException e) {
                    fail("FileNotFoundException trying to open "
                            + files[i].getPath());
                }
                zis = new ZipInputStream(fis);
                while (true) {
                    try {
                        ze = zis.getNextEntry();
                    } catch (IOException e) {
                        fail("IOException while getting next zip entry: "
                                + e);
                    }
                    if (ze == null)
                        break;
                    String zeName = ze.getName();
                    if (zeName.endsWith(".class")) {
                        zeName = zeName.replace('/', slash.charAt(0));
                        for (int j = 0; j < wanted.length; j++) {
                            if (zeName.startsWith(wanted[j])) {
                                // When finding class files from directories the
                                // program saves them as files.
                                // To stay consistent we will turn the ZipEntry
                                // classes into instances of files.
                                File tempF = new File(javaHome + zeName);
                                // Making sure that the same class is not added
                                // twice.
                                boolean duplicate = false;
                                for (int k = 0; k < v.size(); k++) {
                                    if (v.get(k).equals(tempF))
                                        duplicate = true;
                                }
                                if (!duplicate)
                                    v.add(tempF);
                                break;
                            }
                        }
                    }
                }
                ;
                try {
                    zis.close();
                    fis.close();
                } catch (IOException e) {
                    fail(
                            "IOException while trying to close InputStreams: "
                                    + e);
                }
            }
        }
    }
}
