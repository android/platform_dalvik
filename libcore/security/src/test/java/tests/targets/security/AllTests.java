/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.targets.security;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class AllTests {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllTests.suite());
    }

    public static Test suite() {
        TestSuite suite = tests.TestSuiteFactory.createTestSuite("All tests for package tests.targets.security;");
        // $JUnit-BEGIN$

        suite.addTestSuite(MessageDigestTestMD5.class);
        suite.addTestSuite(MessageDigestTestSHA1.class);
        suite.addTestSuite(MessageDigestTestSHA224.class);
        suite.addTestSuite(MessageDigestTestSHA256.class);
        suite.addTestSuite(MessageDigestTestSHA384.class);
        suite.addTestSuite(MessageDigestTestSHA512.class);

        suite.addTestSuite(KeyPairGeneratorTestRSA.class);
        suite.addTestSuite(KeyPairGeneratorTestDSA.class);
        suite.addTestSuite(KeyPairGeneratorTestDH.class);

        suite.addTestSuite(KeyFactoryTestRSA.class);
        suite.addTestSuite(KeyFactoryTestDSA.class);
        suite.addTestSuite(KeyFactoryTestDH.class);
        
        suite.addTestSuite(SignatureTestMD2withRSA.class);
        suite.addTestSuite(SignatureTestMD5withRSA.class);
        suite.addTestSuite(SignatureTestNONEwithDSA.class);
        suite.addTestSuite(SignatureTestSHA1withDSA.class);
        suite.addTestSuite(SignatureTestSHA1withRSA.class);
        suite.addTestSuite(SignatureTestSHA224withRSA.class);
        suite.addTestSuite(SignatureTestSHA256withRSA.class);
        suite.addTestSuite(SignatureTestSHA384withRSA.class);
        suite.addTestSuite(SignatureTestSHA512withRSA.class);
        
        suite.addTestSuite(AlgorithmParameterGeneratorTestAES.class);
        suite.addTestSuite(AlgorithmParameterGeneratorTestDH.class);
        suite.addTestSuite(AlgorithmParameterGeneratorTestDSA.class);
        
        suite.addTestSuite(AlgorithmParametersTestDES.class);
        suite.addTestSuite(AlgorithmParametersTestDESede.class);
        suite.addTestSuite(AlgorithmParametersTestDSA.class);
        suite.addTestSuite(AlgorithmParametersTestOAEP.class);
        suite.addTestSuite(AlgorithmParametersTestAES.class);
        suite.addTestSuite(AlgorithmParametersTestDH.class);
        
        suite.addTestSuite(KeyStoreTestPKCS12.class);
        
        suite.addTestSuite(SecureRandomTestSHA1PRNG.class);

        // $JUnit-END$
        return suite;
    }
}