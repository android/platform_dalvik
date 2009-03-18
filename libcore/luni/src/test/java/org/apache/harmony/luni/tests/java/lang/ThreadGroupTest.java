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

package org.apache.harmony.luni.tests.java.lang;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.security.Permission;
import java.util.Vector;

@TestTargetClass(ThreadGroup.class) 
public class ThreadGroupTest extends junit.framework.TestCase implements Thread.UncaughtExceptionHandler {
    
    class MyThread extends Thread {
        public volatile int heartBeat = 0;

        public MyThread(ThreadGroup group, String name)
                throws SecurityException, IllegalThreadStateException {
            super(group, name);
        }

        @Override
        public void run() {
            while (true) {
                heartBeat++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public boolean isActivelyRunning() {
            long MAX_WAIT = 100;
            return isActivelyRunning(MAX_WAIT);
        }

        public boolean isActivelyRunning(long maxWait) {
            int beat = heartBeat;
            long start = System.currentTimeMillis();
            do {
                Thread.yield();
                int beat2 = heartBeat;
                if (beat != beat2) {
                    return true;
                }
            } while (System.currentTimeMillis() - start < maxWait);
            return false;
        }

    }

    private ThreadGroup rootThreadGroup = null;

    private ThreadGroup initialThreadGroup = null;

    /**
     * @tests java.lang.ThreadGroup#ThreadGroup(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ThreadGroup",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() {
        // Test for method java.lang.ThreadGroup(java.lang.String)

        // Unfortunately we have to use other APIs as well as we test the
        // constructor

        ThreadGroup newGroup = null;
        ThreadGroup initial = getInitialThreadGroup();
        final String name = "Test name";
        newGroup = new ThreadGroup(name);
        assertTrue(
                "Has to be possible to create a subgroup of current group using simple constructor",
                newGroup.getParent() == initial);
        assertTrue("Name has to be correct", newGroup.getName().equals(name));

        // cleanup
        newGroup.destroy();
        
        newGroup = new ThreadGroup("");
        assertEquals("", newGroup.getName());
        
        newGroup = new ThreadGroup(null);
        assertNull(newGroup.getName());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new ThreadGroup(name);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }              
    }
    
    /**
     * @tests java.lang.ThreadGroup#ThreadGroup(java.lang.ThreadGroup,
     *        java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "ThreadGroup",
        args = {java.lang.ThreadGroup.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_ThreadGroupLjava_lang_String() {
        // Test for method java.lang.ThreadGroup(java.lang.ThreadGroup,
        // java.lang.String)

        // Unfortunately we have to use other APIs as well as we test the
        // constructor

        ThreadGroup newGroup = null;

        try {
            newGroup = new ThreadGroup(null, null);
        } catch (NullPointerException e) {
        }
        assertNull("Can't create a ThreadGroup with a null parent",
                newGroup);

        newGroup = new ThreadGroup(getInitialThreadGroup(), null);
        assertTrue("Has to be possible to create a subgroup of current group",
                newGroup.getParent() == Thread.currentThread().getThreadGroup());

        // Lets start all over
        newGroup.destroy();

        newGroup = new ThreadGroup(getRootThreadGroup(), "a name here");
        assertTrue("Has to be possible to create a subgroup of root group",
                newGroup.getParent() == getRootThreadGroup());

        // Lets start all over
        newGroup.destroy();

        try {
            newGroup = new ThreadGroup(newGroup, "a name here");
        } catch (IllegalThreadStateException e) {
            newGroup = null;
        }
        
        assertNull("Can't create a subgroup of a destroyed group",
                newGroup);
        
        try {
            new ThreadGroup(null, "name");
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
        
        try {
            new ThreadGroup(newGroup, null);
            fail("NullPointerException is not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new ThreadGroup(getRootThreadGroup(), "a name here");
            fail("SecurityException was not thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }             
    }

    /**
     * @tests java.lang.ThreadGroup#activeCount()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "activeCount",
        args = {}
    )
    public void test_activeCount() {
        // Test for method int java.lang.ThreadGroup.activeCount()
        ThreadGroup tg = new ThreadGroup("activeCount");
        Thread t1 = new Thread(tg, new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        });
        int beforeCount = tg.activeCount();
        t1.start();
        int afterCount = tg.activeCount();
        assertTrue("count of active threads should be increased", 
                (afterCount - beforeCount) == 1);
        t1.interrupt();
        try {
            t1.join();
        } catch (InterruptedException e) {
        }
        // cleanup
        tg.destroy();
    }

    /**
     * @tests java.lang.ThreadGroup#activeGroupCount()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "activeGroupCount",
        args = {}
    )
    public void test_activeGroupCount() {
     
        ThreadGroup tg = new ThreadGroup("group count");
        assertEquals("Incorrect number of groups",
                0, tg.activeGroupCount());
        Thread t1 = new Thread(tg, new Runnable() {
            public void run() {

            }
        });
        assertEquals("Incorrect number of groups",
                0, tg.activeGroupCount());
        t1.start();
        assertEquals("Incorrect number of groups",
                0, tg.activeGroupCount());
        new ThreadGroup(tg, "test group 1");
        assertEquals("Incorrect number of groups",
                1, tg.activeGroupCount());
        new ThreadGroup(tg, "test group 2");
        assertEquals("Incorrect number of groups",
                2, tg.activeGroupCount());
    }

    /**
     * @tests java.lang.ThreadGroup#allowThreadSuspension(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "allowThreadSuspension",
        args = {boolean.class}
    )
    @SuppressWarnings("deprecation")
    public void test_allowThreadSuspensionZ() {
        ThreadGroup tg = new ThreadGroup("thread suspension");
        assertTrue("Thread suspention can not be changed",
                tg.allowThreadSuspension(false));
        assertTrue("Thread suspention can not be changed",
                tg.allowThreadSuspension(true));
    }

    /**
     * @tests java.lang.ThreadGroup#checkAccess()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "checkAccess",
        args = {}
    )
    public void test_checkAccess() {
        // Test for method void java.lang.ThreadGroup.checkAccess()

        final ThreadGroup originalCurrent = getInitialThreadGroup();
        ThreadGroup testRoot = new ThreadGroup(originalCurrent, "Test group");

        SecurityManager currentManager = System.getSecurityManager();
        boolean passed = true;

        try {
            if (currentManager != null) {
                testRoot.checkAccess();
            }
        } catch (SecurityException se) {
            passed = false;
        }

        assertTrue("CheckAccess is no-op with no SecurityManager", passed);

        testRoot.destroy();

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            testRoot.checkAccess();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }            
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "enumerate",
        args = {java.lang.Thread[].class}
    )
    public void test_enumerateLThreadArray() {
        int numThreads = initialThreadGroup.activeCount();
        Thread[] listOfThreads = new Thread[numThreads];

        int countThread = initialThreadGroup.enumerate(listOfThreads);
        assertEquals(numThreads, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            initialThreadGroup.enumerate(listOfThreads);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }        
    }    
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "enumerate",
        args = {java.lang.Thread[].class, boolean.class}
    )
    public void test_enumerateLThreadArrayLZ() {
        int numThreads = initialThreadGroup.activeCount();
        Thread[] listOfThreads = new Thread[numThreads];

        int countThread = initialThreadGroup.enumerate(listOfThreads, false);
        assertEquals(numThreads, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]);    

        countThread = initialThreadGroup.enumerate(listOfThreads, true);
        assertEquals(numThreads, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]);         

        ThreadGroup subGroup = new ThreadGroup(initialThreadGroup, "Test Group 1");
        int subThreadsCount = 3;
        Vector<MyThread> subThreads = populateGroupsWithThreads(subGroup, 
                subThreadsCount);
        
        countThread = initialThreadGroup.enumerate(listOfThreads, true);
        assertEquals(numThreads, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]); 
        
        for(MyThread thr:subThreads) {
            thr.start();
        }
        // lets give them some time to start
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            fail("Should not be interrupted");
        }

        int numThreads2 = initialThreadGroup.activeCount();
        listOfThreads = new Thread[numThreads2];
        
        assertEquals(numThreads + subThreadsCount, numThreads2);
        
        countThread = initialThreadGroup.enumerate(listOfThreads, true);
        assertEquals(numThreads2, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]); 
        
        for(MyThread thr:subThreads) {
            thr.interrupt();
        }
        // lets give them some time to die
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            fail("Should not be interrupted");
        }       

        int numThreads3 = initialThreadGroup.activeCount();
        listOfThreads = new Thread[numThreads3];
        
        assertEquals(numThreads, numThreads3);
        
        countThread = initialThreadGroup.enumerate(listOfThreads, false);
        assertEquals(numThreads3, countThread);
        assertEquals(Thread.currentThread(), listOfThreads[0]); 

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            initialThreadGroup.enumerate(listOfThreads, true);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }   
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "enumerate",
        args = {java.lang.ThreadGroup[].class}
    )
    public void test_enumerateLThreadGroupArray() {
        int numGroupThreads = initialThreadGroup.activeGroupCount();
        ThreadGroup[] listOfGroups = new ThreadGroup[numGroupThreads];

        int countGroupThread = initialThreadGroup.enumerate(listOfGroups);
        assertEquals(numGroupThreads, countGroupThread);
        
        ThreadGroup[] listOfGroups1 = new ThreadGroup[numGroupThreads + 1];
        countGroupThread = initialThreadGroup.enumerate(listOfGroups1);
        assertEquals(numGroupThreads, countGroupThread);
        assertNull(listOfGroups1[listOfGroups1.length - 1]);
        
        ThreadGroup[] listOfGroups2 = new ThreadGroup[numGroupThreads - 1];
        countGroupThread = initialThreadGroup.enumerate(listOfGroups2);
        assertEquals(numGroupThreads - 1, countGroupThread);
        
        ThreadGroup thrGroup1 = new ThreadGroup("Test Group 1");
        countGroupThread = thrGroup1.enumerate(listOfGroups);
        assertEquals(0, countGroupThread);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            initialThreadGroup.enumerate(listOfGroups);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }   
     }    
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "enumerate",
        args = {java.lang.ThreadGroup[].class, boolean.class}
    )
    public void test_enumerateLThreadGroupArrayLZ() {
        ThreadGroup thrGroup = new ThreadGroup("Test Group 1");
        Vector<MyThread> subThreads = populateGroupsWithThreads(thrGroup, 3);
        int numGroupThreads = thrGroup.activeGroupCount();
        ThreadGroup[] listOfGroups = new ThreadGroup[numGroupThreads];
        
        assertEquals(0, thrGroup.enumerate(listOfGroups, true));
        assertEquals(0, thrGroup.enumerate(listOfGroups, false));
        
        for(MyThread thr:subThreads) {
            thr.start();
        }
        
        numGroupThreads = thrGroup.activeGroupCount();
        listOfGroups = new ThreadGroup[numGroupThreads];
        
        assertEquals(0, thrGroup.enumerate(listOfGroups, true));
        assertEquals(0, thrGroup.enumerate(listOfGroups, false));
        
        ThreadGroup subGroup1 = new ThreadGroup(thrGroup, "Test Group 2");
        Vector<MyThread> subThreads1 = populateGroupsWithThreads(subGroup1, 3); 
        numGroupThreads = thrGroup.activeGroupCount();
        listOfGroups = new ThreadGroup[numGroupThreads];
        
        assertEquals(1, thrGroup.enumerate(listOfGroups, true));
        assertEquals(1, thrGroup.enumerate(listOfGroups, false));      
        
        for(MyThread thr:subThreads1) {
            thr.start();
        }
        numGroupThreads = thrGroup.activeGroupCount();
        listOfGroups = new ThreadGroup[numGroupThreads];
            
        assertEquals(1, thrGroup.enumerate(listOfGroups, true));
        assertEquals(1, thrGroup.enumerate(listOfGroups, false));              
        
        for(MyThread thr:subThreads) {
            thr.interrupt();
         }        
        
        ThreadGroup subGroup2 = new ThreadGroup(subGroup1, "Test Group 3");
        Vector<MyThread> subThreads2 = populateGroupsWithThreads(subGroup2, 3); 
        numGroupThreads = thrGroup.activeGroupCount();
        listOfGroups = new ThreadGroup[numGroupThreads];
        
        assertEquals(2, thrGroup.enumerate(listOfGroups, true));
        assertEquals(1, thrGroup.enumerate(listOfGroups, false));  

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            thrGroup.enumerate(listOfGroups, true);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }   
    }
    
    /**
     * @tests java.lang.ThreadGroup#destroy()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "destroy",
        args = {}
    )
    public void test_destroy() {
        // Test for method void java.lang.ThreadGroup.destroy()

        final ThreadGroup originalCurrent = getInitialThreadGroup();
        ThreadGroup testRoot = new ThreadGroup(originalCurrent, "Test group");
        final int DEPTH = 4;
        final Vector<ThreadGroup> subgroups = buildRandomTreeUnder(testRoot, DEPTH);

        // destroy them all
        testRoot.destroy();

        for (int i = 0; i < subgroups.size(); i++) {
            ThreadGroup child = subgroups.elementAt(i);
            assertEquals("Destroyed child can't have children", 0, child
                    .activeCount());
            boolean passed = false;
            try {
                child.destroy();
            } catch (IllegalThreadStateException e) {
                passed = true;
            }
            
            assertTrue("Destroyed child can't be destroyed again", passed);
        }

        testRoot = new ThreadGroup(originalCurrent, "Test group (daemon)");
        testRoot.setDaemon(true);

        ThreadGroup child = new ThreadGroup(testRoot, "daemon child");

        // If we destroy the last daemon's child, the daemon should get destroyed
        // as well
        child.destroy();

        boolean passed = false;
        try {
            child.destroy();
        } catch (IllegalThreadStateException e) {
            passed = true;
        }
        
        assertTrue("Daemon should have been destroyed already", passed);

        passed = false;
        try {
            testRoot.destroy();
        } catch (IllegalThreadStateException e) {
            passed = true;
        }
        
        assertTrue("Daemon parent should have been destroyed automatically",
                passed);

        assertTrue(
                "Destroyed daemon's child should not be in daemon's list anymore",
                !arrayIncludes(groups(testRoot), child));
        assertTrue("Destroyed daemon should not be in parent's list anymore",
                !arrayIncludes(groups(originalCurrent), testRoot));

        testRoot = new ThreadGroup(originalCurrent, "Test group (daemon)");
        testRoot.setDaemon(true);
        Thread noOp = new Thread(testRoot, null, "no-op thread") {
            @Override
            public void run() {
            }
        };
        noOp.start();

        // Wait for the no-op thread to run inside daemon ThreadGroup
        try {
            noOp.join();
        } catch (InterruptedException ie) {
            fail("Should not be interrupted");
        }
        

        passed = false;
        try {
            child.destroy();
        } catch (IllegalThreadStateException e) {
            passed = true;
        }
        
        assertTrue(
                "Daemon group should have been destroyed already when last thread died",
                passed);

        testRoot = new ThreadGroup(originalCurrent, "Test group (daemon)");
        noOp = new Thread(testRoot, null, "no-op thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    fail("Should not be interrupted");
                }
            }
        };

        // Has to execute the next lines in an interval < the sleep interval of
        // the no-op thread
        noOp.start();
        passed = false;
        try {
            testRoot.destroy();
        } catch (IllegalThreadStateException its) {
            passed = true;
        }
        assertTrue("Can't destroy a ThreadGroup that has threads", passed);

        // But after the thread dies, we have to be able to destroy the thread
        // group
        try {
            noOp.join();
        } catch (InterruptedException ie) {
            fail("Should not be interrupted");
        }
        
        passed = true;
        try {
            testRoot.destroy();
        } catch (IllegalThreadStateException its) {
            passed = false;
        }
        assertTrue(
                "Should be able to destroy a ThreadGroup that has no threads",
                passed);

        ThreadGroup tg = new ThreadGroup("ThreadGroup");
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            originalCurrent.destroy();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.lang.ThreadGroup#destroy()
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Verifies IllegalThreadStateException.",
        method = "destroy",
        args = {}
    )
    public void test_destroy_subtest0() {
        ThreadGroup group1 = new ThreadGroup("test_destroy_subtest0");
        group1.destroy();
        try {
            new Thread(group1, "test_destroy_subtest0");
            fail("should throw IllegalThreadStateException");
        } catch (IllegalThreadStateException e) {
        }
    }

    /**
     * @tests java.lang.ThreadGroup#getMaxPriority()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getMaxPriority",
        args = {}
    )
    public void test_getMaxPriority() {
        // Test for method int java.lang.ThreadGroup.getMaxPriority()
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        ThreadGroup testRoot = new ThreadGroup(originalCurrent, "Test group");

        boolean passed = true;
        try {
            testRoot.setMaxPriority(Thread.MIN_PRIORITY);
        } catch (IllegalArgumentException iae) {
            passed = false;
        }
        assertTrue("Should be able to set priority", passed);

        assertTrue("New value should be the same as we set", testRoot
                .getMaxPriority() == Thread.MIN_PRIORITY);

        testRoot.destroy();

    }

    /**
     * @tests java.lang.ThreadGroup#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public void test_getName() {
        // Test for method java.lang.String java.lang.ThreadGroup.getName()
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        final String name = "Test group";
        final ThreadGroup testRoot = new ThreadGroup(originalCurrent, name);

        assertTrue("Setting a name&getting does not work", testRoot.getName()
                .equals(name));

        testRoot.destroy();

    }

    /**
     * @tests java.lang.ThreadGroup#getParent()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getParent",
        args = {}
    )
    public void test_getParent() {
        // Test for method java.lang.ThreadGroup
        // java.lang.ThreadGroup.getParent()
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        ThreadGroup testRoot = new ThreadGroup(originalCurrent, "Test group");

        assertTrue("Parent is wrong", testRoot.getParent() == originalCurrent);

        // Create some groups, nested some levels.
        final int TOTAL_DEPTH = 5;
        ThreadGroup current = testRoot;
        Vector<ThreadGroup> groups = new Vector<ThreadGroup>();
        // To maintain the invariant that a thread in the Vector is parent
        // of the next one in the collection (and child of the previous one)
        groups.addElement(testRoot);

        for (int i = 0; i < TOTAL_DEPTH; i++) {
            current = new ThreadGroup(current, "level " + i);
            groups.addElement(current);
        }

        // Now we walk the levels down, checking if parent is ok
        for (int i = 1; i < groups.size(); i++) {
            current = groups.elementAt(i);
            ThreadGroup previous = groups.elementAt(i - 1);
            assertTrue("Parent is wrong", current.getParent() == previous);
        }

        final ThreadGroup[] checkAccessGroup = new ThreadGroup[1];
        class SecurityManagerImpl extends MutableSecurityManager {
            @Override
            public void checkAccess(ThreadGroup group) {
                checkAccessGroup[0] = group;
            }
        }
        SecurityManagerImpl sm = new SecurityManagerImpl();
        //add permission to allow reset of security manager
        sm.addPermission(MutableSecurityManager.SET_SECURITY_MANAGER);

        ThreadGroup parent;
        try {
            // To see if it checks Thread creation with our SecurityManager
            System.setSecurityManager(sm);
            parent = testRoot.getParent();
        } finally {
            // restore original, no side-effects
            System.setSecurityManager(null);
        }
        assertTrue("checkAccess with incorrect group",
                checkAccessGroup[0] == parent);

        testRoot.destroy();
    }

    /**
     * @tests java.lang.ThreadGroup#interrupt()
     */
    private static boolean interrupted = false;
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "interrupt",
        args = {}
    )    
    public void test_interrupt() {

        Thread.setDefaultUncaughtExceptionHandler(this);
        ThreadGroup tg = new ThreadGroup("interrupt");
        Thread t1 = new Thread(tg, new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    fail("ok");
                }
            }
        });
        assertFalse("Incorrect state of thread", interrupted);
        t1.start();
        assertFalse("Incorrect state of thread", interrupted);
        t1.interrupt();
        try {
            t1.join();
        } catch (InterruptedException e) {
        }
        assertTrue("Incorrect state of thread", interrupted);
        tg.destroy();

        ThreadGroup threadGroup = new ThreadGroup("securityCheck");
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            threadGroup.interrupt();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }        
    }

    /**
     * @tests java.lang.ThreadGroup#isDaemon()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isDaemon",
        args = {}
    )
    public void test_isDaemon() {
        // Test for method boolean java.lang.ThreadGroup.isDaemon()
        daemonTests();
    }

    /**
     * @tests java.lang.ThreadGroup#isDestroyed()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isDestroyed",
        args = {}
    )
    public void test_isDestroyed() {
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");
        assertFalse("Test group is not destroyed yet",
                testRoot.isDestroyed());
        testRoot.destroy();
        assertTrue("Test group already destroyed",
                testRoot.isDestroyed());
    }

    /**
     * @tests java.lang.ThreadGroup#list()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "list",
        args = {}
    )
    public void test_list() {
        // Test for method void java.lang.ThreadGroup.list()

        final ThreadGroup originalCurrent = getInitialThreadGroup();
        // wipeSideEffectThreads destroy all side effect of threads created in
        // java.lang.Thread
        boolean result = wipeSideEffectThreads(originalCurrent);
        if (result == false) {
            fail("wipe threads in test_list() not successful");
        }
        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");

        // First save the original System.out
        java.io.PrintStream originalOut = System.out;

        try {
            java.io.ByteArrayOutputStream contentsStream = new java.io.ByteArrayOutputStream(
                    100);
            java.io.PrintStream newOut = new java.io.PrintStream(contentsStream);

            // We have to "redirect" System.out to test the method 'list'
            System.setOut(newOut);

            originalCurrent.list();

            /*
             * The output has to look like this
             *
             * java.lang.ThreadGroup[name=main,maxpri=10] Thread[main,5,main]
             * java.lang.ThreadGroup[name=Test group,maxpri=10]
             *
             */

            String contents = new String(contentsStream.toByteArray());
            boolean passed = (contents.indexOf("ThreadGroup[name=main") != -1) &&
                             (contents.indexOf("Thread[") != -1) &&
                             (contents.indexOf("ThreadGroup[name=Test group") != -1);
            assertTrue("'list()' does not print expected contents. "
                    + "Result from list: "
                    + contents, passed);
            // Do proper cleanup
            testRoot.destroy();

        } finally {
            // No matter what, we need to restore the original System.out
            System.setOut(originalOut);
        }

    }

    /**
     * @tests java.lang.ThreadGroup#parentOf(java.lang.ThreadGroup)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "parentOf",
        args = {java.lang.ThreadGroup.class}
    )
    public void test_parentOfLjava_lang_ThreadGroup() {
        // Test for method boolean
        // java.lang.ThreadGroup.parentOf(java.lang.ThreadGroup)
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");
        final int DEPTH = 4;
        buildRandomTreeUnder(testRoot, DEPTH);

        final ThreadGroup[] allChildren = allGroups(testRoot);
        for (ThreadGroup element : allChildren) {
            assertTrue("Have to be parentOf all children", testRoot
                    .parentOf(element));
        }

        assertTrue("Have to be parentOf itself", testRoot.parentOf(testRoot));

        testRoot.destroy();
        assertTrue("Parent can't have test group as subgroup anymore",
                !arrayIncludes(groups(testRoot.getParent()), testRoot));

        try {
            System.setSecurityManager(new MutableSecurityManager(MutableSecurityManager.SET_SECURITY_MANAGER));
            assertTrue("Should not be parent", !testRoot
                    .parentOf(originalCurrent));
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * @tests java.lang.ThreadGroup#resume()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "resume",
        args = {}
    )
   @SuppressWarnings("deprecation")
   @BrokenTest("Thread.resume is implemented on some RI")
    public void test_resume() throws OutOfMemoryError {
        // Test for method void java.lang.ThreadGroup.resume()
        final ThreadGroup originalCurrent = getInitialThreadGroup();

        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");
        final int DEPTH = 2;
        buildRandomTreeUnder(testRoot, DEPTH);

        final int THREADS_PER_GROUP = 2;
        final Vector<MyThread> threads = populateGroupsWithThreads(testRoot,
                THREADS_PER_GROUP);
        try {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.start();
            }
        } catch (OutOfMemoryError e) {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.interrupt();
            }
            throw e;
        }

        try {
            testRoot.resume();
            fail("Thread.resume() is not supported and throws an UOE on Android.");
        } catch (UnsupportedOperationException e) {
            // expected
        }


        for (int i = 0; i < threads.size(); i++) {
            MyThread t = threads.elementAt(i);
            t.interrupt();
        }

        // Make sure we do cleanup before returning
        testRoot.destroy();

        assertEquals("Method destroy must have problems",
                0, testRoot.activeCount());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            originalCurrent.resume();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.lang.ThreadGroup#setDaemon(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDaemon",
        args = {boolean.class}
    )
    public void test_setDaemonZ() {
        // Test for method void java.lang.ThreadGroup.setDaemon(boolean)
        daemonTests();
        
        final ThreadGroup testRoot = new ThreadGroup("Test group");

        testRoot.setDaemon(true);

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        
        try {
            new ThreadGroup("");
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.ThreadGroup#setMaxPriority(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setMaxPriority",
        args = {int.class}
    )
    public void test_setMaxPriorityI() {
        // Test for method void java.lang.ThreadGroup.setMaxPriority(int)
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        ThreadGroup testRoot = new ThreadGroup(originalCurrent, "Test group");

        boolean passed;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        int currentMax = testRoot.getMaxPriority();
        testRoot.setMaxPriority(Thread.MAX_PRIORITY + 1);
        passed = testRoot.getMaxPriority() == currentMax;
        assertTrue(
                "setMaxPriority: Any value higher than the current one is ignored. Before: "
                        + currentMax + " , after: " + testRoot.getMaxPriority(),
                passed);

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        currentMax = testRoot.getMaxPriority();
        testRoot.setMaxPriority(Thread.MIN_PRIORITY - 1);
        passed = testRoot.getMaxPriority() == Thread.MIN_PRIORITY;
        assertTrue(
                "setMaxPriority: Any value smaller than MIN_PRIORITY is adjusted to MIN_PRIORITY. Before: "
                        + currentMax + " , after: " + testRoot.getMaxPriority(),
                passed);

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        testRoot.destroy();
        testRoot = new ThreadGroup(originalCurrent, "Test group");

        // Create some groups, nested some levels. Each level will have maxPrio
        // 1 unit smaller than the parent's. However, there can't be a group
        // with priority < Thread.MIN_PRIORITY
        final int TOTAL_DEPTH = testRoot.getMaxPriority() - Thread.MIN_PRIORITY
                - 2;
        ThreadGroup current = testRoot;
        for (int i = 0; i < TOTAL_DEPTH; i++) {
            current = new ThreadGroup(current, "level " + i);
        }

        // Now we walk the levels down, changing the maxPrio and later verifying
        // that the value is indeed 1 unit smaller than the parent's maxPrio.
        int maxPrio, parentMaxPrio;
        current = testRoot;

        // To maintain the invariant that when we are to modify a child,
        // its maxPriority is always 1 unit smaller than its parent's.
        // We have to set it for the root manually, and the loop does the rest
        // for all the other sub-levels
        current.setMaxPriority(current.getParent().getMaxPriority() - 1);

        for (int i = 0; i < TOTAL_DEPTH; i++) {
            maxPrio = current.getMaxPriority();
            parentMaxPrio = current.getParent().getMaxPriority();

            ThreadGroup[] children = groups(current);
            assertEquals("Can only have 1 subgroup", 1, children.length);
            current = children[0];
            assertTrue(
                    "Had to be 1 unit smaller than parent's priority in iteration="
                            + i + " checking->" + current,
                    maxPrio == parentMaxPrio - 1);
            current.setMaxPriority(maxPrio - 1);

            // The next test is sort of redundant, since in next iteration it
            // will be the parent tGroup, so the test will be done.
            assertTrue("Had to be possible to change max priority", current
                    .getMaxPriority() == maxPrio - 1);
        }

        assertTrue(
                "Priority of leaf child group has to be much smaller than original root group",
                current.getMaxPriority() == testRoot.getMaxPriority()
                        - TOTAL_DEPTH);

        testRoot.destroy();

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        passed = true;
        testRoot = new ThreadGroup(originalCurrent, "Test group");
        try {
            testRoot.setMaxPriority(Thread.MAX_PRIORITY);
        } catch (IllegalArgumentException iae) {
            passed = false;
        }
        assertTrue(
                "Max Priority = Thread.MAX_PRIORITY should be possible if the test is run with default system ThreadGroup as root",
                passed);
        testRoot.destroy();

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        passed = true;
        testRoot = new ThreadGroup(originalCurrent, "Test group");
        System.setSecurityManager(new MutableSecurityManager(MutableSecurityManager.SET_SECURITY_MANAGER));
        try {
            try {
                testRoot.setMaxPriority(Thread.MIN_PRIORITY);
            } catch (IllegalArgumentException iae) {
                passed = false;
            }
        } finally {
            System.setSecurityManager(null);
        }
        assertTrue(
                "Min Priority = Thread.MIN_PRIORITY should be possible, always",
                passed);
        testRoot.destroy();

        try {
            System.setSecurityManager(new MutableSecurityManager(MutableSecurityManager.SET_SECURITY_MANAGER));
            originalCurrent.setMaxPriority(Thread.MAX_PRIORITY);
        } finally {
            System.setSecurityManager(null);
        }

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        
        try {
            testRoot.setMaxPriority(Thread.MAX_PRIORITY);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.ThreadGroup#stop()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "stop",
        args = {}
    )
    @BrokenTest("stop() method not implemented.")
    @SuppressWarnings("deprecation")
    public void test_stop() throws OutOfMemoryError {
        // Test for method void java.lang.ThreadGroup.stop()
        final ThreadGroup originalCurrent = getInitialThreadGroup();

        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");
        final int DEPTH = 2;
        buildRandomTreeUnder(testRoot, DEPTH);

        final int THREADS_PER_GROUP = 2;
        final Vector<MyThread> threads = populateGroupsWithThreads(testRoot,
                THREADS_PER_GROUP);

        try {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.start();
            }
        } catch (OutOfMemoryError e) {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.interrupt();
            }
            throw e;
        }

        // Now that they are all running, let's stop the ThreadGroup
        try {
            testRoot.stop();
            fail("Thread.stop() is not supported and throws an UOE on Android.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        for (int i = 0; i < threads.size(); i++) {
            Thread t = threads.elementAt(i);
            t.interrupt();
        }

        // To make sure that even if we fail, we exit in a clean state
        testRoot.destroy();

        assertEquals("Method destroy (or wipeAllThreads) must have problems",
                0, testRoot.activeCount());

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        
        try {
            originalCurrent.stop();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.ThreadGroup#suspend()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "suspend",
        args = {}
    )
    @BrokenTest("suspend() method not implemented.")
    @SuppressWarnings("deprecation")
    public void test_suspend() throws OutOfMemoryError {
        // Test for method void java.lang.ThreadGroup.suspend()
        final ThreadGroup originalCurrent = getInitialThreadGroup();

        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");
        final int DEPTH = 2;
        buildRandomTreeUnder(testRoot, DEPTH);

        final int THREADS_PER_GROUP = 2;
        final Vector<MyThread> threads = populateGroupsWithThreads(testRoot,
                THREADS_PER_GROUP);

        try {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.start();
            }
        } catch (OutOfMemoryError e) {
            for (int i = 0; i < threads.size(); i++) {
                Thread t = threads.elementAt(i);
                t.interrupt();
            }
            throw e;
        }

        try {
            testRoot.suspend();
            fail("Thread.suspend() is not supported and throws an UOE on Android.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        for (int i = 0; i < threads.size(); i++) {
            Thread t = threads.elementAt(i);
            t.interrupt();
         }
        // Make sure we cleanup before returning from the method
        testRoot.destroy();

        assertEquals("Method destroy (or wipeAllThreads) must have problems",
                0, testRoot.activeCount());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            originalCurrent.suspend();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }  
    }

    /**
     * @tests java.lang.ThreadGroup#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        // Test for method java.lang.String java.lang.ThreadGroup.toString()
        final ThreadGroup originalCurrent = getInitialThreadGroup();
        final String tGroupName = "Test group";

        // Our own subclass
        class MyThreadGroup extends ThreadGroup {
            // Have to define a constructor since there's no default one
            public MyThreadGroup(ThreadGroup parent, String name) {
                super(parent, name);
            }
        }
        ;

        ThreadGroup testRoot = new MyThreadGroup(originalCurrent, tGroupName);
        final String toString = testRoot.toString();

        StringBuffer expectedResult = new StringBuffer();
        expectedResult.append(testRoot.getClass().getName());
        expectedResult.append("[name=");
        expectedResult.append(tGroupName);
        expectedResult.append(",maxpri=");
        expectedResult.append(testRoot.getMaxPriority());
        expectedResult.append("]");

        String expectedValue = expectedResult.toString();

        assertTrue("toString does not follow the Java language spec.", toString
                .equals(expectedValue));

        testRoot.destroy();
    }

    /**
     * @tests java.lang.ThreadGroup#uncaughtException(java.lang.Thread,
     *        java.lang.Throwable)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "uncaughtException",
        args = {java.lang.Thread.class, java.lang.Throwable.class}
    )  
    @SuppressWarnings("deprecation")
    public void test_uncaughtExceptionLjava_lang_ThreadLjava_lang_Throwable() {
        // Test for method void
        // java.lang.ThreadGroup.uncaughtException(java.lang.Thread,
        // java.lang.Throwable)

        final ThreadGroup originalCurrent = getInitialThreadGroup();
 
        // indices for the array defined below
        final int TEST_DEATH = 0;
        final int TEST_OTHER = 1;
        final int TEST_EXCEPTION_IN_UNCAUGHT = 2;
        final int TEST_OTHER_THEN_DEATH = 3;
        final int TEST_FORCING_THROW_THREAD_DEATH = 4;
        final int TEST_KILLING = 5;
        final int TEST_DEATH_AFTER_UNCAUGHT = 6;

        final boolean[] passed = new boolean[] { false, false, false, false,
                false, false, false };

        ThreadGroup testRoot;
        Thread thread;

        // Our own exception class
        class TestException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - -
        testRoot = new ThreadGroup(originalCurrent,
                "Test Forcing a throw of ThreadDeath") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof ThreadDeath) {
                    passed[TEST_FORCING_THROW_THREAD_DEATH] = true;
                }
                // always forward, any exception
                super.uncaughtException(t, e);
            }
        };

        // Test if a Thread tells its ThreadGroup about ThreadDeath
        thread = new Thread(testRoot, null, "suicidal thread") {
            @Override
            public void run() {
                throw new ThreadDeath();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        testRoot.destroy();
        assertTrue(
                "Any thread should notify its ThreadGroup about its own death, even if suicide:"
                        + testRoot, passed[TEST_FORCING_THROW_THREAD_DEATH]);
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - -

        testRoot = new ThreadGroup(originalCurrent, "Test ThreadDeath") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                passed[TEST_DEATH] = false;
                // always forward, any exception
                super.uncaughtException(t, e);
            }
        };

        // Test if a Thread tells its ThreadGroup about ThreadDeath
        passed[TEST_DEATH] = true;
        thread = new Thread(testRoot, null, "no-op thread");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        testRoot.destroy();
        assertTrue("A thread should not call uncaughtException when it dies:"
                + testRoot, passed[TEST_DEATH]);
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - -

        testRoot = new ThreadGroup(originalCurrent, "Test other Exception") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof TestException) {
                    passed[TEST_OTHER] = true;
                } else {
                    // only forward exceptions other than our test
                    super.uncaughtException(t, e);
                }
            }
        };

        // Test if a Thread tells its ThreadGroup about an Exception
        thread = new Thread(testRoot, null, "no-op thread") {
            @Override
            public void run() {
                throw new TestException();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        testRoot.destroy();
        assertTrue(
                "Any thread should notify its ThreadGroup about an uncaught exception:"
                        + testRoot, passed[TEST_OTHER]);
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - -

        // Our own uncaught exception class
        class UncaughtException extends TestException {
            private static final long serialVersionUID = 1L;
        }

        testRoot = new ThreadGroup(originalCurrent,
                "Test Exception in uncaught exception") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof TestException) {
                    passed[TEST_EXCEPTION_IN_UNCAUGHT] = true;
                    // Let's simulate an error inside our uncaughtException
                    // method.
                    // This should be no-op according to the spec
                    throw new UncaughtException();
                }
                // only forward exceptions other than our test
                super.uncaughtException(t, e);
            }
        };

        // Test if an Exception in uncaughtException is really a no-op
        thread = new Thread(testRoot, null, "no-op thread") {
            @Override
            public void run() {
                try {
                    throw new TestException();
                } catch (UncaughtException ue) {
                    // any exception in my ThreadGroup's uncaughtException must
                    // not be propagated.
                    // If it gets propagated and we detected that, the test failed
                    passed[TEST_EXCEPTION_IN_UNCAUGHT] = false;
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        testRoot.destroy();
        assertTrue(
                "Any uncaughtException in uncaughtException should be no-op:"
                        + testRoot, passed[TEST_EXCEPTION_IN_UNCAUGHT]);
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - -

        // This is a mix of 2 of the tests above. It is assumed that ThreadDeath
        // and any random exception do work , tested separately. Now we test
        // if after an uncaughtException is forwarded to the ThreadGroup and
        // the Thread dies, if ThreadDeath is also forwarded. It should be
        // (so that a ThreadGroup can know its Thread died)
        testRoot = new ThreadGroup(originalCurrent,
                "Test Uncaught followed by ThreadDeath") {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof ThreadDeath) {
                    passed[TEST_DEATH_AFTER_UNCAUGHT] = true;
                }
                if (e instanceof TestException) {
                    passed[TEST_OTHER_THEN_DEATH] = true;
                } else {
                    // only forward exceptions other than our test
                    super.uncaughtException(t, e);
                }
            }
        };

        // Test if a Thread tells its ThreadGroup about an Exception and also
        // ThreadDeath
        thread = new Thread(testRoot, null, "no-op thread") {
            @Override
            public void run() {
                throw new TestException();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        testRoot.destroy();
    }

    /*
     * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
     */
    public void uncaughtException(Thread t, Throwable e) {
        interrupted = true;
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    @Override
    protected void setUp() {
        initialThreadGroup = Thread.currentThread().getThreadGroup();
        rootThreadGroup = initialThreadGroup;
        while (rootThreadGroup.getParent() != null) {
            rootThreadGroup = rootThreadGroup.getParent();
        }
    }

    @Override
    protected void tearDown() {
        try {
            // Give the threads a chance to die.
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
    }

    private Thread[] threads(ThreadGroup parent) {
        // No API to get the count of immediate children only ?
        int count = parent.activeCount();
        Thread[] all = new Thread[count];
        int actualSize = parent.enumerate(all, false);
        Thread[] result;
        if (actualSize == all.length) {
            result = all;
        } else {
            result = new Thread[actualSize];
            System.arraycopy(all, 0, result, 0, actualSize);
        }

        return result;

    }

    private ThreadGroup getInitialThreadGroup() {
        return initialThreadGroup;
    }

    private ThreadGroup[] allGroups(ThreadGroup parent) {
        int count = parent.activeGroupCount();
        ThreadGroup[] all = new ThreadGroup[count];
        parent.enumerate(all, true);
        return all;
    }

    private void daemonTests() {
        // Test for method void java.lang.ThreadGroup.setDaemon(boolean)

        final ThreadGroup originalCurrent = getInitialThreadGroup();
        final ThreadGroup testRoot = new ThreadGroup(originalCurrent,
                "Test group");

        testRoot.setDaemon(true);
        assertTrue("Setting daemon&getting does not work", testRoot.isDaemon());

        testRoot.setDaemon(false);
        assertTrue("Setting daemon&getting does not work", !testRoot.isDaemon());

        testRoot.destroy();
    }

    private boolean wipeAllThreads(final ThreadGroup aGroup) {
        boolean ok = true;
        Thread[] threads = threads(aGroup);
        for (Thread t : threads) {
            ok = ok && wipeThread(t);
        }

        // Recursively for subgroups (if any)
        ThreadGroup[] children = groups(aGroup);
        for (ThreadGroup element : children) {
            ok = ok && wipeAllThreads(element);
        }
        return ok;
    }

    private boolean wipeSideEffectThreads(ThreadGroup aGroup) {
        boolean ok = true;
        Thread[] threads = threads(aGroup);
        for (Thread t : threads) {
            if (t.getName().equals("SimpleThread")
                    || t.getName().equals("Bogus Name")
                    || t.getName().equals("Testing")
                    || t.getName().equals("foo")
                    || t.getName().equals("Test Group")
                    || t.getName().equals("Squawk")
                    || t.getName().equals("Thread-1")
                    || t.getName().equals("firstOne")
                    || t.getName().equals("secondOne")
                    || t.getName().equals("Thread-16")
                    || t.getName().equals("Thread-14")) {
                ok = ok && wipeThread(t);
            }
        }

        // Recursively for subgroups (if any)
        ThreadGroup[] children = groups(aGroup);

        for (ThreadGroup element : children) {
            ok = ok && wipeSideEffectThreads(element);
            if(element.getName() !=  null) {
                if (element.getName().equals("Test Group")
                    || element.getName().equals("foo")
                    || element.getName().equals("jp")) {
                    element.destroy();
                }
            }
        }
        try {
            // Give the threads a chance to die.
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
        return ok;
    }

    private void asyncBuildRandomTreeUnder(final ThreadGroup aGroup,
            final int depth, final Vector<ThreadGroup> allCreated) {
        if (depth <= 0) {
            return;
        }

        final int maxImmediateSubgroups = random(3);
        for (int i = 0; i < maxImmediateSubgroups; i++) {
            final int iClone = i;
            final String name = " Depth = " + depth + ",N = " + iClone
                    + ",Vector size at creation: " + allCreated.size();
            // Use concurrency to maximize chance of exposing concurrency bugs
            // in ThreadGroups
            Thread t = new Thread(aGroup, name) {
                @Override
                public void run() {
                    ThreadGroup newGroup = new ThreadGroup(aGroup, name);
                    allCreated.addElement(newGroup);
                    asyncBuildRandomTreeUnder(newGroup, depth - 1, allCreated);
                }
            };
            t.start();
        }

    }

    private Vector<ThreadGroup> asyncBuildRandomTreeUnder(final ThreadGroup aGroup,
            final int depth) {
        Vector<ThreadGroup> result = new Vector<ThreadGroup>();
        asyncBuildRandomTreeUnder(aGroup, depth, result);
        return result;

    }

    private boolean allSuspended(Vector<MyThread> threads) {
        for (int i = 0; i < threads.size(); i++) {
            MyThread t = threads.elementAt(i);
            if (t.isActivelyRunning()) {
                return false;
            }
        }

        return true;

    }

    private ThreadGroup[] groups(ThreadGroup parent) {
        // No API to get the count of immediate children only ?
        int count = parent.activeGroupCount();
        ThreadGroup[] all = new ThreadGroup[count];
        parent.enumerate(all, false);
        // Now we may have nulls in the array, we must find the actual size
        int actualSize = 0;
        for (; actualSize < all.length; actualSize++) {
            if (all[actualSize] == null) {
                break;
            }
        }
        ThreadGroup[] result;
        if (actualSize == all.length) {
            result = all;
        } else {
            result = new ThreadGroup[actualSize];
            System.arraycopy(all, 0, result, 0, actualSize);
        }

        return result;

    }

    private Vector<MyThread> populateGroupsWithThreads(final ThreadGroup aGroup,
            final int threadCount) {
        Vector<MyThread> result = new Vector<MyThread>();
        populateGroupsWithThreads(aGroup, threadCount, result);
        return result;

    }

    private void populateGroupsWithThreads(final ThreadGroup aGroup,
            final int threadCount, final Vector<MyThread> allCreated) {
        for (int i = 0; i < threadCount; i++) {
            final int iClone = i;
            final String name = "(MyThread)N =" + iClone + "/" + threadCount
                    + " ,Vector size at creation: " + allCreated.size();

            MyThread t = new MyThread(aGroup, name);
            allCreated.addElement(t);
        }

        // Recursively for subgroups (if any)
        ThreadGroup[] children = groups(aGroup);
        for (ThreadGroup element : children) {
            populateGroupsWithThreads(element, threadCount, allCreated);
        }

    }

    private int random(int max) {

        return 1 + ((new Object()).hashCode() % max);

    }

    @SuppressWarnings("deprecation")
    private boolean wipeThread(Thread t) {
        t.stop();
        try {
            t.join(1000);
        } catch (InterruptedException ie) {
            fail("Should not have been interrupted");
        }
        // The thread had plenty (subjective) of time to die so there
        // is a problem.
        if (t.isAlive()) {
            return false;
        }

        return true;
    }

    private Vector<ThreadGroup> buildRandomTreeUnder(ThreadGroup aGroup, int depth) {
        Vector<ThreadGroup> result = asyncBuildRandomTreeUnder(aGroup, depth);
        while (true) {
            int sizeBefore = result.size();
            try {
                Thread.sleep(1000);
                int sizeAfter = result.size();
                // If no activity for a while, we assume async building may be
                // done.
                if (sizeBefore == sizeAfter) {
                    // It can only be done if no more threads. Unfortunately we
                    // are relying on this API to work as well.
                    // If it does not, we may loop forever.
                    if (aGroup.activeCount() == 0) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
            }
        }
        return result;

    }

    private boolean arrayIncludes(Object[] array, Object toTest) {
        for (Object element : array) {
            if (element == toTest) {
                return true;
            }
        }

        return false;
    }

    protected void myassertTrue(String msg, boolean b) {
        // This method is defined here just to solve a visibility problem
        // of protected methods with inner types
        assertTrue(msg, b);
    }

    private ThreadGroup getRootThreadGroup() {
        return rootThreadGroup;

    }
    
    SecurityManager sm = new SecurityManager() {

        public void checkPermission(Permission perm) {
        }
        
        public void checkAccess(ThreadGroup g) {
           throw new SecurityException();
        }
    };
}
