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

package tests;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.KnownFailure;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Protectable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * an enhanced TestSuite e.g. for running RI tests.
 * 
 * a sample command line:
 * 
 *  /usr/lib/jvm/java-1.5.0-sun/bin/java -Xmx1024m -Dcts.listOnlyFailingTests=true 
 *  -Dcts.includeKnownFailure=false -Dcts.runOnDalvikVM=false 
 *  -Dcts.allowUnderscoreTests=false -Dcts.useEnhancedJunit=true 
 *  -Dcts.collectOnly=false 
 *  -cp 
 *  /tmp/cts_outjavac:
 *  out/debug/host/linux-x86/product/sim/data/app/CtsCoreTests.apk:
 *  out/debug/target/common/obj/APPS/CtsCoreTests_intermediates/classes.jar:
 *  tools/cts/vm-tests/lib/junit.jar 
 *  
 *  junit.textui.TestRunner tests.AllTests
 * 
 */
public class TestSuiteFactory {
    
    
    
    static boolean _collectOnly = false;
    static boolean _useEnhancedJunit = false;
    static boolean _allowUnderscoreTests = false;
    static boolean _runOnDalvikVM = true;
    static boolean _includeKnowFailure = false;
    static boolean _listOnlyFailingTests = false;
    static boolean _useSuppliedTestResult = false;
    static int _maxRunningTimePerTest = 15000; // 15 seconds

    static {
        _useEnhancedJunit = System.getProperty("cts.useEnhancedJunit", "false").equals("true");
        // next only applicable if _useEnhancedJunit
        _collectOnly = System.getProperty("cts.collectOnly", "false").equals("true");
        _allowUnderscoreTests= System.getProperty("cts.allowUnderscoreTests", "false").equals("true");
        _runOnDalvikVM = System.getProperty("cts.runOnDalvikVM", "true").equals("true");
        _includeKnowFailure = System.getProperty("cts.includeKnownFailure", "false").equals("true");
        _maxRunningTimePerTest = Integer.parseInt(System.getProperty("cts.maxRunningTimePerTest", "15000"));
        _listOnlyFailingTests = System.getProperty("cts.listOnlyFailingTests", "false").equals("true");
        _useSuppliedTestResult = System.getProperty("cts.useSuppliedTestResult", "false").equals("true");
        
        System.out.println("TestSuiteFactory: v0.97");
        System.out.println("TestSuiteFactory: using cts.useEnhancedJunit: "+_useEnhancedJunit);
        System.out.println("TestSuiteFactory: using cts.collectOnly: "+_collectOnly);
        System.out.println("TestSuiteFactory: max allowed running time per test (using Thread.stop()) (cts.maxRunningTimePerTest): "+_maxRunningTimePerTest);
        System.out.println("TestSuiteFactory: run tests on a dalvik vm (cts.runOnDalvikVM): "+_runOnDalvikVM);
        System.out.println("TestSuiteFactory: include @KnowFailure when running on dalvik vm (cts.includeKnownFailure): "+_includeKnowFailure);
        System.out.println("TestSuiteFactory: include '_test...' methods in test run (cts.allowUnderscoreTests): "+_allowUnderscoreTests);
        System.out.println("TestSuiteFactory: list only failing tests (cts.listOnlyFailingTests): "+_listOnlyFailingTests);        
        System.out.println();
    }

    public static TestSuite createTestSuite(String name) {
        return _useEnhancedJunit? new MyTestSuite(name): new TestSuite(name);
    }

    public static TestSuite createTestSuite() {
        return _useEnhancedJunit? new MyTestSuite(): new TestSuite();    
    }
    
}


class MyTestSuite extends TestSuite {
    private boolean allow_underscoretests = true;

    public MyTestSuite() {
    }

    public MyTestSuite(String name) {
        super(name);
    }

    public MyTestSuite(final Class theClass) {
        String fName = theClass.getName();
        try {
            TestSuite.getTestConstructor(theClass); // Avoid generating multiple error
                                          // messages
        } catch (NoSuchMethodException e) {
            addTest(warning("Class "
                    + theClass.getName()
                    + " has no public constructor TestCase(String name) or TestCase()"));
            return;
        }

        if (!Modifier.isPublic(theClass.getModifiers())) {
            addTest(warning("Class " + theClass.getName() + " is not public"));
            return;
        }

        Class superClass = theClass;
        Vector names = new Vector();
        while (Test.class.isAssignableFrom(superClass)) {
            Method[] methods = superClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                addTestMethod(methods[i], names, theClass);
            }
            superClass = superClass.getSuperclass();
        }
        if (testCount() == 0)
            addTest(warning("No tests found in " + theClass.getName()));
    }

    private void addTestMethod(Method m, Vector names, Class theClass) {
        String name = m.getName();
        if (names.contains(name)) return;
        if (!isPublicTestMethod(m)) {
            if (isTestMethod(m))
                addTest(warning("Test method isn't public: " + m.getName()));
            return;
        }
        names.addElement(name);
        addTest(TestSuite.createTest(theClass, name));
    }

    private boolean isPublicTestMethod(Method m) {
        return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
    }
    
    private boolean isTestMethod(Method m) {
        String name = m.getName();
        Class[] parameters = m.getParameterTypes();
        Class returnType = m.getReturnType();
        return parameters.length == 0 && 
        (name.startsWith("test") || (TestSuiteFactory._allowUnderscoreTests  && name.startsWith("_test")))
        && returnType.equals(Void.TYPE);
    }

    public void addTestSuite(Class testClass) {
        try {
            addTest(new MyTestSuite(testClass));
        } catch (Throwable e) {
            System.err.println("---------------- error ----------------------");
            System.err.println("error: could not add test suite: "
                    + testClass.getName());
            e.printStackTrace();
            System.err.println("---------------- ----------------------------");
        }
    }

    private static int testCnt = 0;
    
    public void runTest(Test test, final TestResult dummy_result) {
    	
    	if (TestSuiteFactory._useSuppliedTestResult) {
    		if (test instanceof TestSetup) {
    			test = ((TestSetup)test).getTest();
    		}
    		test.run(dummy_result);
    		return;
    	}
    	
        TestResult eresult = new TestResult() {
            private String msg;
            private boolean error = false;
            
            protected void run(final TestCase testcase) {
                msg = "";
                String testName = testcase.getClass().getName() + ":" + testcase.getName()+" (nr:"+(++testCnt)+")";
                try {
                    if (!TestSuiteFactory._listOnlyFailingTests) {
                        System.out.print(testName+" ");
                    }
                    Annotation aKnownFailure = null;
                    Annotation aAndroidOnly = null;
                    if (true) { // handle annotations, allow them on both class (valid for all methods then) and method level
                         //  @KnownFailure("Fails because of a defect in ...") if the test is correct but there is a bug in the core libraries implementation.
                         //  @BrokenTest("This test is not implemented correctly because...") if there is a defect in the test method itself.
                         //  @AndroidOnly("Because...") if the test is Android-specific, succeeds on Android but fails on the JDK. 
                        try {
                            Annotation[] annosClass = testcase.getClass().getDeclaredAnnotations();
                            Method runMethod= testcase.getClass().getMethod(testcase.getName());
                            Annotation[] annosMethod = runMethod.getDeclaredAnnotations();
                            Annotation[] annos = null;
                            for (int i = 0; i < 2; i++) {
                                annos = (i==0? annosClass : annosMethod);
                                if (annos != null && annos.length > 0) {
                                    for (Annotation anno : annos) {
                                        Class<? extends Annotation> acla = anno.annotationType();
                                        if (acla.getName().equals("dalvik.annotation.AndroidOnly")) {
                                            aAndroidOnly = anno;
                                        } else if (acla.getName().equals("dalvik.annotation.KnownFailure")) {
                                            aKnownFailure = anno;
                                        }
                                    }
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            error = true;
                            msg +="::warning::unable to get test method to read annotations: testcase name="+testcase.getName();
                        }
                    }
                    boolean androidOnly = aAndroidOnly != null;
                    boolean knownFailure = aKnownFailure != null;
                    
                    if (
                            !TestSuiteFactory._collectOnly
                            && (
                                    (TestSuiteFactory._runOnDalvikVM && 
                                            (TestSuiteFactory._includeKnowFailure || !knownFailure)
                                    )
                                    || 
                                    (!TestSuiteFactory._runOnDalvikVM && !androidOnly)
                               )
                       ) {
                        
                        msg += "[";
                        long start = System.currentTimeMillis();
                        // -----start the test ----
                        startTest(testcase);
                        final Protectable p= new Protectable() {
                            public void protect() throws Throwable {
                                testcase.runBare();
                            }
                        };
                        boolean threadStopForced = false;
                        if (!TestSuiteFactory._runOnDalvikVM) {
                            // for jvm, start in a new thread, since we can stop() it for too-long running processes.
                            Thread t = new Thread(new Runnable(){
                                public void run() {
                                    try {
                                        p.protect();
                                    } 
                                    catch (AssertionFailedError e) {
                                        addFailure(testcase, e);
                                    }
                                    catch (ThreadDeath e) { // don't catch ThreadDeath by accident
                                        throw e;
                                    }
                                    catch (Throwable e) {
                                        addError(testcase, e);
                                    }
                                }});
                            t.start();
                            
                            try {
                                //System.out.println("joining...");
                                t.join(TestSuiteFactory._maxRunningTimePerTest);
                                //System.out.println("joining done...");
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            if (t.isAlive()) {
                                threadStopForced = true;
                            }
                            t.stop();
                            
                            // for RI vm : run in new thread and stop thread after a timeout
                        } else { 
                            runProtected(testcase, p);
                        }                        
                        
                        endTest(testcase);
                        // ------------------------
                        
                        msg += "]";
                        long stop = System.currentTimeMillis();
                        if (threadStopForced) {
                            error = true;
                            msg += "::warning::slow test forced to stop since it took longer than "+ TestSuiteFactory._maxRunningTimePerTest;
                        } else if (stop - start > TestSuiteFactory._maxRunningTimePerTest) {
                            error = true;
                            msg += "::warning::slow test took longer than "+ TestSuiteFactory._maxRunningTimePerTest+" milis: "+(stop-start)+" milis. ";
                        }
                        
                    }
                    if (!TestSuiteFactory._runOnDalvikVM && androidOnly) {
                        msg+= "ignoring on RI since @AndroidOnly: "+((AndroidOnly)aAndroidOnly).value();
                    }
                    if (TestSuiteFactory._runOnDalvikVM && knownFailure && !TestSuiteFactory._includeKnowFailure) {
                        msg += "ignoring on dalvik since @KnownFailure: "+((KnownFailure)aKnownFailure).value();
                    }
                }
                finally {
                    if (TestSuiteFactory._listOnlyFailingTests) {
                        if (error) {
                            // we have error / warnings
                            msg = testName + msg;
                            System.out.println(msg);
                        } // else do not output anything
                    } else {
                        System.out.println(msg+(error? "": " cts-test-passed"));
                    }
                }
            }
            
            public synchronized void addError(Test test, Throwable t) {
                error = true;
                msg+= " ::error::err:"+exceptionToString(t);
                super.addError(test, t);
            }

            public synchronized void addFailure(Test test,
                    AssertionFailedError t) {
                error = true;
                msg+= " ::error::failure:"+exceptionToString(t);
                super.addFailure(test, t);
            }
        };
        test.run(eresult);
    }
    
    private static Test warning(final String message) {
        return new TestCase("warning") {
            protected void runTest() {
                fail(message);
            }
        };
    }
    
    private static String exceptionToString(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        return stringWriter.toString();

    }
}
