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

package tests.api.java.security;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.StringTokenizer;

import tests.support.Support_Exec;
import tests.support.Support_GetLocal;
import tests.support.resource.Support_Resources;

@TestTargetClass(PermissionCollection.class)
public class PermissionCollectionTest extends junit.framework.TestCase {

    // The below test is known to fail. Haven't got to the bottom of
    // it yet but here is what has been determined :-
    //
    // * the Support_PermissionCollection application that is forked off
    // near the end of this test needs to verify a signed jar (signedBKS.jar).
    // This means that com.ibm.oti.util.JarUtils.verifySignature() ends up
    // getting called. But at present that exists as just a lightweight/stub
    // implementation which simply returns NULL. That behaviour causes a
    // security exception inside java.util.jar.JarVerifier.
    //
    // * the above problem was fixed by rebuilding Harmony with the STUB
    // IMPLEMENTATION of com.ibm.oti.util.JarUtils.verifySignature() replaced
    // with one that delegates to
    // org.apache.harmony.security.utils.JarUtils.verifySignature().
    //
    // * unfortunately, a NPE is raised in line 103 of Harmony's JarUtils class.
    //
    // * the cause of that NPE has still not been determined. Could it be
    // related to Harmony's current stub implementation of BigInteger ?
    /**
     * @tests java.security.PermissionCollection#implies(java.security.Permission)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "",
        method = "implies",
        args = {java.security.Permission.class}
    )
    @BrokenTest("Android doesn't support protection domains. NPE at first Line")
    public void test_impliesLjava_security_Permission() throws Exception{

        // Look for the tests classpath
        URL classURL = this.getClass().getProtectionDomain().getCodeSource()
                .getLocation();
        assertNotNull("Could not get this class' location", classURL);

        File policyFile = Support_GetLocal.createTempFile(".policy");
        policyFile.deleteOnExit();

        URL signedBKS = getResourceURL("PermissionCollection/signedBKS.jar");
        URL keystoreBKS = getResourceURL("PermissionCollection/keystore.bks");
        
        // Create the policy file (and save the existing one if any)
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(policyFile);
            String linebreak = System.getProperty("line.separator");
            StringBuilder towrite = new StringBuilder();
            towrite.append("grant {");
            towrite.append(linebreak);
            towrite.append("permission java.io.FilePermission \"");
            towrite.append(signedBKS.getFile());
            towrite.append("\", \"read\";");
            towrite.append(linebreak);
            towrite.append("permission java.lang.RuntimePermission \"getProtectionDomain\";");
            towrite.append(linebreak);
            towrite.append("permission java.security.SecurityPermission \"getPolicy\";");
            towrite.append(linebreak);
            towrite.append("};");
            towrite.append(linebreak);
            towrite.append("grant codeBase \"");
            towrite.append(signedBKS.toExternalForm());
            towrite.append("\" signedBy \"eleanor\" {");
            towrite.append(linebreak);
            towrite.append("permission java.io.FilePermission \"test1.txt\", \"write\";");
            towrite.append(linebreak);
            towrite.append("permission mypackage.MyPermission \"essai\", signedBy \"eleanor,dylan\";");
            towrite.append(linebreak);
            towrite.append("};");
            towrite.append(linebreak);
            towrite.append("grant codeBase \"");
            towrite.append(signedBKS.toExternalForm());
            towrite.append("\" signedBy \"eleanor\" {");
            towrite.append(linebreak);
            towrite.append("permission java.io.FilePermission \"test2.txt\", \"write\";");
            towrite.append(linebreak);
            towrite.append("};");
            towrite.append(linebreak);
            towrite.append("grant codeBase \"");
            towrite.append(classURL.toExternalForm());
            towrite.append("\" {");
            towrite.append(linebreak);
            towrite.append("permission java.security.AllPermission;");
            towrite.append(linebreak);
            towrite.append("};");
            towrite.append(linebreak);
            towrite.append("keystore \"");
            towrite.append(keystoreBKS.toExternalForm());
            towrite.append("\",\"BKS\";");            
            fileOut.write(towrite.toString().getBytes());
            fileOut.flush();
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
        }

        // Copy mypermissionBKS.jar to the user directory so that it can be put
        // in
        // the classpath
        File jarFile = null;
        FileOutputStream fout = null;
        InputStream jis = null;
        try {
            jis = Support_Resources
                    .getResourceStream("PermissionCollection/mypermissionBKS.jar");
            jarFile = Support_GetLocal.createTempFile(".jar");
            jarFile.deleteOnExit();
            fout = new FileOutputStream(jarFile);
            int c = jis.read();
            while (c != -1) {
                fout.write(c);
                c = jis.read();
            }
            fout.flush();
        } finally {
            if (fout != null) {
                fout.close();
            }
            if (jis != null) {
                jis.close();
            }
        }

        String classPath = new File(classURL.getFile()).getPath();

        // Execute Support_PermissionCollection in another VM
        String[] classPathArray = new String[2];
        classPathArray[0] = classPath;
        classPathArray[1] = jarFile.getPath();
        String[] args = { "-Djava.security.policy=" + policyFile.toURL(),
                "tests.support.Support_PermissionCollection",
                signedBKS.toExternalForm() };

        String result = Support_Exec.execJava(args, classPathArray, true);

        StringTokenizer resultTokenizer = new StringTokenizer(result, ",");

        // Check the test result from the new VM process
        assertEquals("Permission should be granted", "false", resultTokenizer
                .nextToken());
        assertEquals("signed Permission should be granted", "false",
                resultTokenizer.nextToken());
        assertEquals("Permission should not be granted", "false",
                resultTokenizer.nextToken());
    }

    /**
     * @tests java.security.PermissionCollection#PermissionCollection()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PermissionCollection",
        args = {}
    )
    public void test_Constructor() {
        // test java.security.permissionCollection.PermissionCollection()
        SecurityPermission permi = new SecurityPermission(
                "testing permissionCollection-isReadOnly");
        PermissionCollection permCollect = permi.newPermissionCollection();
        assertNotNull("creat permissionCollection constructor returned a null",
                permCollect);
    }

    /**
     * @tests java.security.PermissionCollection#isReadOnly()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isReadOnly",
        args = {}
    )
    public void test_isReadOnly() {
        // test java.security.permissionCollection.isReadOnly()
        SecurityPermission permi = new SecurityPermission(
                "testing permissionCollection-isREadOnly");
        PermissionCollection permCollect = permi.newPermissionCollection();
        assertTrue("readOnly has not been set, but isReadOnly returned true",
                !permCollect.isReadOnly());
        permCollect.setReadOnly();
        assertTrue("readOnly is set, but isReadonly returned false",
                permCollect.isReadOnly());
    }

    /**
     * @tests java.security.PermissionCollection#setReadOnly()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setReadOnly",
        args = {}
    )
    public void test_setReadOnly() {
        // test java.security.permissionCollection.setReadOnly()
        SecurityPermission permi = new SecurityPermission(
                "testing permissionCollection-setReadOnly");
        PermissionCollection permCollect = permi.newPermissionCollection();
        assertTrue("readOnly has not been set, but isReadOnly returned true",
                !permCollect.isReadOnly());
        permCollect.setReadOnly();
        assertTrue("readOnly is set, but isReadonly returned false",
                permCollect.isReadOnly());
    }

    /**
     * @tests java.security.PermissionCollection#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        // test java.security.permissionCollection.toString()
        SecurityPermission permi = new SecurityPermission(
                "testing permissionCollection-isREadOnly");
        assertNotNull("toString should have returned a string of elements",
                permi.newPermissionCollection().toString());
    }

    // FIXME move me to Support_Resources
    public static URL getResourceURL(String name) {

        URL url = ClassLoader.getSystemClassLoader().getResource(name);

        if (url == null) {
            throw new RuntimeException("Failed to get resource url: " + name);
        }

        return url;
    }
}