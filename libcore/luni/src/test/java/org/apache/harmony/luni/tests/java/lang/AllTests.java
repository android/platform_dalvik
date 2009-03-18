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

package org.apache.harmony.luni.tests.java.lang;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Listing of all the tests that are to be run.
 */
public class AllTests {

    public static void run() {
        TestRunner.main(new String[] { AllTests.class.getName() });
    }

    public static final Test suite() {
        TestSuite suite = tests.TestSuiteFactory.createTestSuite("Tests for java.lang");
        
        suite.addTestSuite(AbstractMethodErrorTest.class);
        suite.addTestSuite(ArithmeticExceptionTest.class);
        suite.addTestSuite(ArrayIndexOutOfBoundsExceptionTest.class);
        suite.addTestSuite(ArrayStoreExceptionTest.class);
        suite.addTestSuite(AssertionErrorTest.class);
        suite.addTestSuite(BooleanTest.class);
        suite.addTestSuite(ByteTest.class);
        suite.addTestSuite(CharacterImplTest.class);
        suite.addTestSuite(Character_SubsetTest.class);
        suite.addTestSuite(CharacterTest.class);
        suite.addTestSuite(Character_UnicodeBlockTest.class);
        suite.addTestSuite(ClassCastExceptionTest.class);
        suite.addTestSuite(ClassCircularityErrorTest.class);
        suite.addTestSuite(ClassFormatErrorTest.class); 
        suite.addTestSuite(ClassLoaderTest.class);
        suite.addTestSuite(ClassNotFoundExceptionTest.class);
        suite.addTestSuite(ClassTest.class); 
        suite.addTestSuite(ClassTest2.class);
        suite.addTestSuite(CloneNotSupportedExceptionTest.class);
        suite.addTestSuite(CompilerTest.class);
        suite.addTestSuite(DoubleTest.class);
        suite.addTestSuite(EnumConstantNotPresentExceptionTest.class);
        suite.addTestSuite(EnumTest.class);
        suite.addTestSuite(ErrorTest.class);
        suite.addTestSuite(ExceptionInInitializerErrorTest.class);
        suite.addTestSuite(ExceptionTest.class);
        suite.addTestSuite(FloatTest.class);
        suite.addTestSuite(IllegalAccessErrorTest.class);
        suite.addTestSuite(IllegalAccessExceptionTest.class);
        suite.addTestSuite(IllegalArgumentExceptionTest.class);
        suite.addTestSuite(IllegalMonitorStateExceptionTest.class);
        suite.addTestSuite(IllegalStateExceptionTest.class);
        suite.addTestSuite(IllegalThreadStateExceptionTest.class);
        suite.addTestSuite(IncompatibleClassChangeErrorTest.class);
        suite.addTestSuite(IndexOutOfBoundsExceptionTest.class);
        suite.addTestSuite(InheritableThreadLocalTest.class);
        suite.addTestSuite(InstantiationErrorTest.class);
        suite.addTestSuite(InstantiationExceptionTest.class);
        suite.addTestSuite(IntegerTest.class);
        suite.addTestSuite(InternalErrorTest.class);
        suite.addTestSuite(InterruptedExceptionTest.class);
        suite.addTestSuite(LinkageErrorTest.class);
        suite.addTestSuite(LongTest.class);
        suite.addTestSuite(MathTest.class);
        suite.addTestSuite(NegativeArraySizeExceptionTest.class);
        suite.addTestSuite(NoClassDefFoundErrorTest.class);
        suite.addTestSuite(NoSuchFieldErrorTest.class);
        suite.addTestSuite(NoSuchFieldExceptionTest.class);
        suite.addTestSuite(NoSuchMethodErrorTest.class);
        suite.addTestSuite(NoSuchMethodExceptionTest.class);
        suite.addTestSuite(NullPointerExceptionTest.class);
        suite.addTestSuite(NumberFormatExceptionTest.class);
        suite.addTestSuite(NumberTest.class);
        suite.addTestSuite(ObjectTest.class);
        suite.addTestSuite(OutOfMemoryErrorTest.class);
        suite.addTestSuite(PackageTest.class);
        suite.addTestSuite(ProcessBuilderTest.class);
        suite.addTestSuite(RuntimeExceptionTest.class);
        suite.addTestSuite(RuntimePermissionTest.class);
        suite.addTestSuite(RuntimeTest.class);
        suite.addTestSuite(SecurityExceptionTest.class);
        suite.addTestSuite(SecurityManagerTest.class);
        suite.addTestSuite(ShortTest.class);
        suite.addTestSuite(StackOverflowErrorTest.class);
        suite.addTestSuite(StackTraceElementTest.class);
        suite.addTestSuite(StrictMathTest.class);
        suite.addTestSuite(String2Test.class);
        suite.addTestSuite(StringBuffer2Test.class);
        suite.addTestSuite(StringBufferTest.class);
        suite.addTestSuite(StringBuilderTest.class);
        suite.addTestSuite(StringIndexOutOfBoundsExceptionTest.class);
        suite.addTestSuite(StringTest.class);
        suite.addTestSuite(SystemTest.class); 
        suite.addTestSuite(ThreadDeathTest.class);
        suite.addTestSuite(ThreadGroupTest.class);
        suite.addTestSuite(ThreadLocalTest.class);
        suite.addTestSuite(ThreadTest.class);
        suite.addTestSuite(ThreadStateTest.class);
        suite.addTestSuite(ThrowableTest.class);
        suite.addTestSuite(TypeNotPresentExceptionTest.class);
        suite.addTestSuite(UnknownErrorTest.class);
        suite.addTestSuite(UnsatisfiedLinkErrorTest.class);
        suite.addTestSuite(UnsupportedOperationExceptionTest.class);
        suite.addTestSuite(UnsupportedClassVersionErrorTest.class); 
        suite.addTestSuite(VerifyErrorTest.class);
        suite.addTestSuite(VirtualMachineErrorTest.class);

    return suite;
    }
}
