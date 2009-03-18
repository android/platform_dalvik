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

/**
* @author Vladimir N. Molotkov
* @version $Revision$
*/

package tests.security.cert;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import junit.framework.TestCase;

import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.harmony.security.tests.support.cert.MyCertificate;
import org.apache.harmony.security.tests.support.cert.TestUtils;

/**
 * Tests for <code>PKIXCertPathChecker</code>
 * 
 */
@TestTargetClass(PKIXCertPathChecker.class)
public class PKIXCertPathCheckerTest extends TestCase {

    //
    // Tests
    //
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "PKIXCertPathChecker",
        args = {}
    )
    public final void testConstructor() {
        try {
            new MyPKIXCertPathChecker();
        } catch(Exception e) {
            fail("Unexpected exception " + e.getMessage());
        }
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "clone",
        args = {}
    )
    public final void testClone() {
        PKIXCertPathChecker pc1 = TestUtils.getTestCertPathChecker();
        PKIXCertPathChecker pc2 = (PKIXCertPathChecker) pc1.clone();
        assertNotSame("notSame", pc1, pc2);
    }

    //
    // the following tests just call methods
    // that are abstract in <code>PKIXCertPathChecker</code>
    // (So they just like signature tests)
    //
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isForwardCheckingSupported",
        args = {}
    )
    public final void testIsForwardCheckingSupported() {
        PKIXCertPathChecker pc = TestUtils.getTestCertPathChecker();
        pc.isForwardCheckingSupported();
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "init",
        args = {boolean.class}
    )
    public final void testInit()
        throws CertPathValidatorException {
        PKIXCertPathChecker pc = TestUtils.getTestCertPathChecker();
        pc.init(true);
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getSupportedExtensions",
        args = {}
    )
    public final void testGetSupportedExtensions() {
        PKIXCertPathChecker pc = TestUtils.getTestCertPathChecker();
        pc.getSupportedExtensions();
    }
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "check",
        args = {java.security.cert.Certificate.class, java.util.Collection.class}
    )
    public final void testCheck() throws CertPathValidatorException {
        PKIXCertPathChecker pc = TestUtils.getTestCertPathChecker();
        pc.check(new MyCertificate("", null), new HashSet<String>());
    }

    class MyPKIXCertPathChecker extends PKIXCertPathChecker {

        public MyPKIXCertPathChecker() {
            super();
        }

        @Override
        public void check(Certificate cert,
                Collection<String> unresolvedCritExts)
        throws CertPathValidatorException {
        }

        @Override
        public Set<String> getSupportedExtensions() {
            return null;
        }

        @Override
        public void init(boolean forward) throws CertPathValidatorException {
        }

        @Override
        public boolean isForwardCheckingSupported() {
            return false;
        }

    }

}
