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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.Permission;
import java.util.Map;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.util.concurrent.Semaphore;

@TestTargetClass(Thread.class) 
public class ThreadTest extends junit.framework.TestCase {
    
    int counter = 0;

    static class SimpleThread implements Runnable {
        int delay;

        public void run() {
            try {
                synchronized (this) {
                    this.notify();
                    this.wait(delay);
                }
            } catch (InterruptedException e) {
                return;
            }

        }

        public SimpleThread(int d) {
            if (d >= 0)
                delay = d;
        }
    }

    static class YieldThread implements Runnable {
        volatile int delay;

        public void run() {
            int x = 0;
            while (true) {
                ++x;
            }
        }

        public YieldThread(int d) {
            if (d >= 0)
                delay = d;
        }
    }

    static class ResSupThread implements Runnable {
        Thread parent;

        volatile int checkVal = -1;

        public void run() {
            try {
                synchronized (this) {
                    this.notify();
                }
                while (true) {
                    checkVal++;
                    zz();
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                return;
            } catch (BogusException e) {
                try {
                    // Give parent a chance to sleep
                    Thread.sleep(500);
                } catch (InterruptedException x) {
                }
                parent.interrupt();
                while (!Thread.currentThread().isInterrupted()) {
                    // Don't hog the CPU
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException x) {
                        // This is what we've been waiting for...don't throw it
                        // away!
                        break;
                    }
                }
            }
        }

        public void zz() throws BogusException {
        }

        public ResSupThread(Thread t) {
            parent = t;
        }

        public synchronized int getCheckVal() {
            return checkVal;
        }
    }

    static class BogusException extends Throwable {

        private static final long serialVersionUID = 1L;

        public BogusException(String s) {
            super(s);
        }
    }

    // TODO android-added
    class MonitoredClass {
        public synchronized void enterLocked() {
            boolean b = Thread.holdsLock(this);
            assertTrue("Thread should hold lock for object", b);
        }
        
        public void enterNonLocked() {
            boolean b = Thread.holdsLock(this);
            assertFalse("Thread should not hold lock for object", b);
        }
    }
    
    Thread st, ct, spinner;

    static boolean calledMySecurityManager = false;

    /**
     * @tests java.lang.Thread#Thread()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {}
    )
    public void test_Constructor() {
        // Test for method java.lang.Thread()

        Thread t;
        SecurityManager m = new SecurityManager() {
            @Override
            public ThreadGroup getThreadGroup() {
                calledMySecurityManager = true;
                return Thread.currentThread().getThreadGroup();
            }
            
            @Override
            public void checkPermission(Permission permission) {
                if (permission.getName().equals("setSecurityManager")) {
                    return;
                }
                super.checkPermission(permission);
            }
        };
        try {
            // To see if it checks Thread creation with our SecurityManager
            System.setSecurityManager(m);
            t = new Thread();
        } finally {
            // restore original, no side-effects
            System.setSecurityManager(null);
        }
        assertTrue("Did not call SecurityManager.getThreadGroup ()",
                calledMySecurityManager);
        t.start();
    }

    /**
     * @tests java.lang.Thread#Thread(java.lang.Runnable)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.Runnable.class}
    )
    public void test_ConstructorLjava_lang_Runnable() {
        // Test for method java.lang.Thread(java.lang.Runnable)
        try {
            ct = new Thread(new SimpleThread(10));
            ct.start();
        } catch (Exception e) {
            fail("Failed to create subthread : " + e.getMessage());
        }
    }

    /**
     * @tests java.lang.Thread#Thread(java.lang.Runnable, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.Runnable.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_RunnableLjava_lang_String() {
        // Test for method java.lang.Thread(java.lang.Runnable,
        // java.lang.String)
        Thread st1 = new Thread(new SimpleThread(1), "SimpleThread1");
        assertEquals("Constructed thread with incorrect thread name", "SimpleThread1", st1
                .getName());
        st1.start();
    }

    /**
     * @tests java.lang.Thread#Thread(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() {
        // Test for method java.lang.Thread(java.lang.String)
        Thread t = new Thread("Testing");
        assertEquals("Created tread with incorrect name", 
                "Testing", t.getName());
        t.start();
    }

    /**
     * @tests java.lang.Thread#Thread(java.lang.ThreadGroup, java.lang.Runnable)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.ThreadGroup.class, java.lang.Runnable.class}
    )
    public void test_ConstructorLjava_lang_ThreadGroupLjava_lang_Runnable() {
        // Test for method java.lang.Thread(java.lang.ThreadGroup,
        // java.lang.Runnable)
        ThreadGroup tg = new ThreadGroup("Test Group1");
        st = new Thread(tg, new SimpleThread(1), "SimpleThread2");
        assertTrue("Returned incorrect thread group", st.getThreadGroup() == tg);
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        tg.destroy();
    }

    /**
     * @tests java.lang.Thread#Thread(java.lang.ThreadGroup, java.lang.Runnable,
     *        java.lang.String)lo
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.ThreadGroup.class, java.lang.Runnable.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_ThreadGroupLjava_lang_RunnableLjava_lang_String() {
        // Test for method java.lang.Thread(java.lang.ThreadGroup,
        // java.lang.Runnable, java.lang.String)
        ThreadGroup tg = new ThreadGroup("Test Group2");
        st = new Thread(tg, new SimpleThread(1), "SimpleThread3");
        assertTrue("Constructed incorrect thread", (st.getThreadGroup() == tg)
                && st.getName().equals("SimpleThread3"));
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        tg.destroy();

        Runnable r = new Runnable() {
            public void run() {
            }
        };

        ThreadGroup foo = null;
        try {
            new Thread(foo = new ThreadGroup("foo"), r, null);
            // Should not get here
            fail("Null cannot be accepted as Thread name");
        } catch (NullPointerException npe) {
            assertTrue("Null cannot be accepted as Thread name", true);
            foo.destroy();
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.ThreadGroup.class, java.lang.Runnable.class, java.lang.String.class, long.class}
    )
    public void test_ConstructorLjava_lang_ThreadGroupLjava_lang_RunnableLjava_lang_StringL$L() {
        ThreadGroup tg = new ThreadGroup("Test Group2");
        st = new Thread(tg, new SimpleThread(1), "SimpleThread3", 1);
        assertTrue("Constructed incorrect thread", (st.getThreadGroup() == tg)
                && st.getName().equals("SimpleThread3"));
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        tg.destroy();

        Runnable r = new Runnable() {
            public void run() {
            }
        };
        
        try {
            new Thread(tg, new SimpleThread(1), "SimpleThread3", 
                    Integer.MAX_VALUE);
            fail("StackOverflowError/OutOfMemoryError is not thrown.");
        } catch(IllegalThreadStateException itse) {
            //expected
        }

    }
    
    /**
     * @tests java.lang.Thread#Thread(java.lang.ThreadGroup, java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "Thread",
        args = {java.lang.ThreadGroup.class, java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_ThreadGroupLjava_lang_String() {
        // Test for method java.lang.Thread(java.lang.ThreadGroup,
        // java.lang.String)
        st = new Thread(new SimpleThread(1), "SimpleThread4");
        assertEquals("Returned incorrect thread name", 
                "SimpleThread4", st.getName());
        st.start();
    }

    /**
     * @tests java.lang.Thread#activeCount()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "activeCount",
        args = {}
    )
    public void test_activeCount() {
        // Test for method int java.lang.Thread.activeCount()
        Thread t = new Thread(new SimpleThread(1));
        int active = Thread.activeCount();
        assertTrue("Incorrect read made: " + active, active > 0);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * @tests java.lang.Thread#checkAccess()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "checkAccess",
        args = {}
    )
    public void test_checkAccess() {
        // Test for method void java.lang.Thread.checkAccess()
        ThreadGroup tg = new ThreadGroup("Test Group3");
        try {
            st = new Thread(tg, new SimpleThread(1), "SimpleThread5");
            st.checkAccess();
            assertTrue("CheckAccess passed", true);
        } catch (SecurityException e) {
            fail("CheckAccess failed : " + e.getMessage());
        }
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        tg.destroy();
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.checkAccess();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Thread#countStackFrames()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "countStackFrames",
        args = {}
    )
    @SuppressWarnings("deprecation")
    public void test_countStackFrames() {
        /*
         * Thread.countStackFrames() is unpredictable, so we just test that it
         * doesn't throw an exception.
         */
        try {
            Thread.currentThread().countStackFrames();
        } catch (Throwable t) {
            fail("unexpected throwable: " + t.toString());
        }
    }

    /**
     * @tests java.lang.Thread#currentThread()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "currentThread",
        args = {}
    )
    public void test_currentThread() {
        assertNotNull(Thread.currentThread());
    }

    /**
     * @tests java.lang.Thread#destroy()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "destroy",
        args = {}
    )
    @SuppressWarnings("deprecation")
    public void test_destroy() {
        try {
            new Thread().destroy();
            // FIXME uncomment when IBM VME is updated
            fail("NoSuchMethodError was not thrown");
        } catch (NoSuchMethodError e) {
        }
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "dumpStack",
        args = {}
    )
    public void test_dumpStack() {
        try {
            PrintStream savedErr = System.err;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setErr(new PrintStream(baos));
            Thread.dumpStack();
            System.setErr(savedErr);
            
            String s = new String(baos.toByteArray());
            
            assertTrue(s.contains("java.lang.Thread.dumpStack"));
            
        } catch(Exception e) {
            fail("Unexpected exception was thrown: " + e.toString());
        }
    }

    /**
     * @tests java.lang.Thread#enumerate(java.lang.Thread[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "enumerate",
        args = {java.lang.Thread[].class}
    )
    public void test_enumerate$Ljava_lang_Thread() {
        // Test for method int java.lang.Thread.enumerate(java.lang.Thread [])
        // The test has been updated according to HARMONY-1974 JIRA issue.

        class MyThread extends Thread {
            MyThread(ThreadGroup tg, String name) {
                super(tg, name);
            }
            
            boolean failed = false;
            String failMessage = null;

            public void run() {
                SimpleThread st1 = null;
                SimpleThread st2 = null;
                ThreadGroup mytg = null;
                Thread firstOne = null;
                Thread secondOne = null;
                try {
                    int arrayLength = 10;
                    Thread[] tarray = new Thread[arrayLength];
                    st1 = new SimpleThread(-1);
                    st2 = new SimpleThread(-1);
                    mytg = new ThreadGroup("jp");
                    firstOne = new Thread(mytg, st1, "firstOne2");
                    secondOne = new Thread(mytg, st2, "secondOne1");
                    int count = Thread.enumerate(tarray);
                    assertEquals("Incorrect value returned1",
                            1, count);
                    synchronized (st1) {
                        firstOne.start();
                        try {
                            st1.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    count = Thread.enumerate(tarray);
                    assertEquals("Incorrect value returned2",
                            2, count);
                    synchronized (st2) {
                        secondOne.start();
                        try {
                            st2.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    count = Thread.enumerate(tarray);
                    assertEquals("Incorrect value returned3",
                            3, count);
                } catch (junit.framework.AssertionFailedError e) {
                    failed = true;
                    failMessage = e.getMessage();
                } finally {
                    synchronized (st1) {
                        firstOne.interrupt();
                    }
                    synchronized (st2) {
                        secondOne.interrupt();
                    }
                    try {
                        firstOne.join();
                        secondOne.join();
                    } catch (InterruptedException e) {
                    }
                    mytg.destroy();
                }
            }
        };
        
        ThreadGroup tg = new ThreadGroup("tg");
        MyThread t = new MyThread(tg, "top");
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail("Unexpected interrupt");
        } finally {
            tg.destroy();
        }
        assertFalse(t.failMessage, t.failed);
    }

    /**
     * @tests java.lang.Thread#getContextClassLoader()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getContextClassLoader",
        args = {}
    )
    public void test_getContextClassLoader() {
        // Test for method java.lang.ClassLoader
        // java.lang.Thread.getContextClassLoader()
        Thread t = new Thread();
        assertTrue("Incorrect class loader returned",
                t.getContextClassLoader() == Thread.currentThread()
                        .getContextClassLoader());
        t.start();

 /*       SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("getClassLoader")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            t.getContextClassLoader();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
*/        
    }

    /**
     * @tests java.lang.Thread#getName()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getName",
        args = {}
    )
    public void test_getName() {
        // Test for method java.lang.String java.lang.Thread.getName()
        st = new Thread(new SimpleThread(1), "SimpleThread6");
        assertEquals("Returned incorrect thread name", 
                "SimpleThread6", st.getName());
        st.start();
    }

    /**
     * @tests java.lang.Thread#getPriority()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getPriority",
        args = {}
    )
    public void test_getPriority() {
        // Test for method int java.lang.Thread.getPriority()
        st = new Thread(new SimpleThread(1));
        st.setPriority(Thread.MAX_PRIORITY);
        assertTrue("Returned incorrect thread priority",
                st.getPriority() == Thread.MAX_PRIORITY);
        st.start();
    }

    /**
     * @tests java.lang.Thread#getThreadGroup()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getThreadGroup",
        args = {}
    )
    public void test_getThreadGroup() {
        // Test for method java.lang.ThreadGroup
        // java.lang.Thread.getThreadGroup()
        ThreadGroup tg = new ThreadGroup("Test Group4");
        st = new Thread(tg, /* new SimpleThread(1), */ "SimpleThread8");
        assertTrue("Returned incorrect thread group", st.getThreadGroup() == tg);
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        assertNull("group should be null", st.getThreadGroup());
        assertNotNull("toString() should not be null", st.toString());
        tg.destroy();

        final Object lock = new Object();
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        };
        synchronized (lock) {
            t.start();
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
        int running = 0;
        while (t.isAlive())
            running++;
        ThreadGroup group = t.getThreadGroup();
        assertNull("ThreadGroup is not null", group);
    }

    /**
     * @tests java.lang.Thread#interrupt()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "interrupt",
        args = {}
    )
    public void test_interrupt() {
        // Test for method void java.lang.Thread.interrupt()
        final Object lock = new Object();
        class ChildThread1 extends Thread {
            Thread parent;

            boolean sync;

            @Override
            public void run() {
                if (sync) {
                    synchronized (lock) {
                        lock.notify();
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                parent.interrupt();
            }

            public ChildThread1(Thread p, String name, boolean sync) {
                super(name);
                parent = p;
                this.sync = sync;
            }
        }
        boolean interrupted = false;
        try {
            ct = new ChildThread1(Thread.currentThread(), "Interrupt Test1",
                    false);
            synchronized (lock) {
                ct.start();
                lock.wait();
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }
        assertTrue("Failed to Interrupt thread1", interrupted);

        interrupted = false;
        try {
            ct = new ChildThread1(Thread.currentThread(), "Interrupt Test2",
                    true);
            synchronized (lock) {
                ct.start();
                lock.wait();
                lock.notify();
            }
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        assertTrue("Failed to Interrupt thread2", interrupted);

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.interrupt();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }        
    }

    /**
     * @tests java.lang.Thread#interrupted()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "interrupted",
        args = {}
    )
    public void test_interrupted() {
        assertFalse("Interrupted returned true for non-interrupted thread", Thread
                .interrupted());
        Thread.currentThread().interrupt();
        assertTrue("Interrupted returned true for non-interrupted thread", Thread.interrupted());
        assertFalse("Failed to clear interrupted flag", Thread.interrupted());
    }

    /**
     * @tests java.lang.Thread#isAlive()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isAlive",
        args = {}
    )
    public void test_isAlive() {
        // Test for method boolean java.lang.Thread.isAlive()
        SimpleThread simple;
        st = new Thread(simple = new SimpleThread(500));
        assertFalse("A thread that wasn't started is alive.", st.isAlive());
        synchronized (simple) {
            st.start();
            try {
                simple.wait();
            } catch (InterruptedException e) {
            }
        }
        assertTrue("Started thread returned false", st.isAlive());
        
        try {
            st.join();
        } catch (InterruptedException e) {
            fail("Thread did not die");
        }
        assertTrue("Stopped thread returned true", !st.isAlive());
    }

    /**
     * @tests java.lang.Thread#isDaemon()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isDaemon",
        args = {}
    )
    public void test_isDaemon() {
        // Test for method boolean java.lang.Thread.isDaemon()
        st = new Thread(new SimpleThread(1), "SimpleThread10");
        assertTrue("Non-Daemon thread returned true", !st.isDaemon());
        st.setDaemon(true);
        assertTrue("Daemon thread returned false", st.isDaemon());
        st.start();
    }

    /**
     * @tests java.lang.Thread#isInterrupted()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "isInterrupted",
        args = {}
    )
    public void test_isInterrupted() {
        // Test for method boolean java.lang.Thread.isInterrupted()
        class SpinThread implements Runnable {
            public volatile boolean done = false;

            public void run() {
                while (!Thread.currentThread().isInterrupted())
                    ;
                while (!done)
                    ;
            }
        }

        SpinThread spin = new SpinThread();
        spinner = new Thread(spin);
        spinner.start();
        Thread.yield();
        try {
            assertTrue("Non-Interrupted thread returned true", !spinner
                    .isInterrupted());
            spinner.interrupt();
            assertTrue("Interrupted thread returned false", spinner
                    .isInterrupted());
            spin.done = true;
        } finally {
            spinner.interrupt();
            spin.done = true;
        }
    }

    /**
     * @tests java.lang.Thread#join()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "join",
        args = {}
    )
    public void test_join() {
        // Test for method void java.lang.Thread.join()
        SimpleThread simple;
        try {
            st = new Thread(simple = new SimpleThread(100));
            // cause isAlive() to be compiled by the JIT, as it must be called
            // within 100ms below.
            assertTrue("Thread is alive", !st.isAlive());
            synchronized (simple) {
                st.start();
                simple.wait();
            }
            st.join();
        } catch (InterruptedException e) {
            fail("Join failed ");
        }
        assertTrue("Joined thread is still alive", !st.isAlive());
        boolean result = true;
        Thread th = new Thread("test");
        try {
            th.join();
        } catch (InterruptedException e) {
            result = false;
        }
        assertTrue("Hung joining a non-started thread", result);
        th.start();
        
        st = new Thread() {
            public void run() {
                try {
                    join();
                    fail("InterruptedException was not thrown.");
                } catch(InterruptedException ie) {
                    //expected
                }       
            }           
        };

        st.start();        
    }

    /**
     * @tests java.lang.Thread#join(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "join",
        args = {long.class}
    )
    public void test_joinJ() {
        // Test for method void java.lang.Thread.join(long)
        SimpleThread simple;
        try {
            st = new Thread(simple = new SimpleThread(1000), "SimpleThread12");
            // cause isAlive() to be compiled by the JIT, as it must be called
            // within 100ms below.
            assertTrue("Thread is alive", !st.isAlive());
            synchronized (simple) {
                st.start();
                simple.wait();
            }
            st.join(10);
        } catch (InterruptedException e) {
            fail("Join failed ");
        }
        assertTrue("Join failed to timeout", st.isAlive());

        st.interrupt();
        try {
            st = new Thread(simple = new SimpleThread(100), "SimpleThread13");
            synchronized (simple) {
                st.start();
                simple.wait();
            }
            st.join(1000);
        } catch (InterruptedException e) {
            fail("Join failed : " + e.getMessage());
            return;
        }
        assertTrue("Joined thread is still alive", !st.isAlive());

        final Object lock = new Object();
        final Thread main = Thread.currentThread();
        Thread killer = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (lock) {
                        lock.notify();
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
                main.interrupt();
            }
        });
        boolean result = true;
        Thread th = new Thread("test");
        try {
            synchronized (lock) {
                killer.start();
                lock.wait();
            }
            th.join(200);
        } catch (InterruptedException e) {
            result = false;
        }
        killer.interrupt();
        assertTrue("Hung joining a non-started thread", result);
        th.start();
        
        st = new Thread() {
            public void run() {
                try {
                    join(1000);
                    fail("InterruptedException was not thrown.");
                } catch(InterruptedException ie) {
                    //expected
                }       
            }           
        };

        st.start(); 
    }

    /**
     * @tests java.lang.Thread#join(long, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "join",
        args = {long.class, int.class}
    )
    public void test_joinJI() {
        // Test for method void java.lang.Thread.join(long, int)
        SimpleThread simple;
        try {
            st = new Thread(simple = new SimpleThread(1000), "Squawk1");
            assertTrue("Thread is alive", !st.isAlive());
            synchronized (simple) {
                st.start();
                simple.wait();
            }
            
            long firstRead = System.currentTimeMillis();
            st.join(100, 999999);
            long secondRead = System.currentTimeMillis();
            assertTrue("Did not join by appropriate time: " + secondRead + "-"
                    + firstRead + "=" + (secondRead - firstRead), secondRead
                    - firstRead <= 300);
            assertTrue("Joined thread is not alive", st.isAlive());
            st.interrupt();  
        } catch (Exception e) {
            fail("Exception during test : " + e.getMessage());
        }

        final Object lock = new Object();
        final Thread main = Thread.currentThread();
        Thread killer = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (lock) {
                        lock.notify();
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
                main.interrupt();
            }
        });
        boolean result = true;
        Thread th = new Thread("test");
        try {
            synchronized (lock) {
                killer.start();
                lock.wait();
            }
            th.join(200, 20);
        } catch (InterruptedException e) {
            result = false;
        }
        killer.interrupt();
        assertTrue("Hung joining a non-started thread", result);
        th.start();
        
        st = new Thread() {
            public void run() {
                try {
                    join(1000, 20);
                    fail("InterruptedException was not thrown.");
                } catch(InterruptedException ie) {
                    //expected
                }       
            }           
        };

        st.start(); 
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setContextClassLoader",
            args = {java.lang.ClassLoader.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getContextClassLoader",
            args = {}
        )
    })
    public void test_setContextClassLoader() {
        PublicClassLoader pcl = new PublicClassLoader();
        st = new Thread();
        st.setContextClassLoader(pcl);
        assertEquals(pcl, st.getContextClassLoader());
        
        st.setContextClassLoader(null);
        assertNull(st.getContextClassLoader());
        
        SecurityManager sm = new SecurityManager() {
            public void checkPermission(Permission perm) {
                if (perm.getName().equals("setContextClassLoader") 
                        || perm.getName().equals("getClassLoader") ) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setContextClassLoader(pcl);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }
    
    
    /**
     * @tests java.lang.Thread#resume()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "resume",
        args = {}
    )
    @AndroidOnly("Android throws UnsupportedOperationException for Thread.resume()")
    @SuppressWarnings("deprecation")
    public void test_resume() {
        // Test for method void java.lang.Thread.resume()
        int orgval;
        ResSupThread t;
        try {
            t = new ResSupThread(Thread.currentThread());
            synchronized (t) {
                ct = new Thread(t, "Interrupt Test2");
                ct.start();
                t.wait();
            }
            try {
                ct.resume();
            } catch (UnsupportedOperationException e) {
                // expected
            }
        } catch (InterruptedException e) {
            fail("Unexpected interrupt occurred : " + e.getMessage());
        }

        // Security checks are made even though resume() is not supported.
        SecurityManager sm = new SecurityManager() {
            public void checkPermission(Permission perm) {
            }
            
            public void checkAccess(Thread t) {
                throw new SecurityException();
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.resume();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }        
    }

    /**
     * @tests java.lang.Thread#run()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "run",
        args = {}
    )
    public void test_run() {
        // Test for method void java.lang.Thread.run()
        class RunThread implements Runnable {
            boolean didThreadRun = false;

            public void run() {
                didThreadRun = true;
            }
        }
        RunThread rt = new RunThread();
        Thread t = new Thread(rt);
        try {
            t.start();
            int count = 0;
            while (!rt.didThreadRun && count < 20) {
                Thread.sleep(100);
                count++;
            }
            assertTrue("Thread did not run", rt.didThreadRun);
            t.join();
        } catch (InterruptedException e) {
            assertTrue("Joined thread was interrupted", true);
        }
        assertTrue("Joined thread is still alive", !t.isAlive());
    }

    /**
     * @tests java.lang.Thread#setDaemon(boolean)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setDaemon",
        args = {boolean.class}
    )
    public void test_setDaemonZ() {
        // Test for method void java.lang.Thread.setDaemon(boolean)
        st = new Thread(new SimpleThread(1), "SimpleThread14");
        st.setDaemon(true);
        assertTrue("Failed to set thread as daemon thread", st.isDaemon());
        st.start();
        
        // BEGIN android-added
        st = new Thread(new SimpleThread(5));
        st.start();
        try {
            st.setDaemon(false);
            fail("setDaemon() must throw exception for started thread");
        } catch (IllegalThreadStateException ex) {
            // We expect this one.
        }
        // END android-added

        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
           
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setDaemon(false);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Thread#setName(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setName",
        args = {java.lang.String.class}
    )
    public void test_setNameLjava_lang_String() {
        // Test for method void java.lang.Thread.setName(java.lang.String)
        st = new Thread(new SimpleThread(1), "SimpleThread15");
        st.setName("Bogus Name");
        assertEquals("Failed to set thread name", 
                "Bogus Name", st.getName());
        try {
            st.setName(null);
            fail("Null should not be accepted as a valid name");
        } catch (NullPointerException e) {
            // success
            assertTrue("Null should not be accepted as a valid name", true);
        }
        st.start();
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
            
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setName("Bogus Name");
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.lang.Thread#setPriority(int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "setPriority",
        args = {int.class}
    )
    public void test_setPriorityI() {
        // Test for method void java.lang.Thread.setPriority(int)
        st = new Thread(new SimpleThread(1));
        st.setPriority(Thread.MAX_PRIORITY);
        assertTrue("Failed to set priority",
                st.getPriority() == Thread.MAX_PRIORITY);
        st.start();
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
           
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setPriority(Thread.MIN_PRIORITY);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
        
        try {
            st.setPriority(Thread.MIN_PRIORITY - 1);
            fail("IllegalArgumentException is not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
        
        try {
            st.setPriority(Thread.MAX_PRIORITY + 1);
            fail("IllegalArgumentException is not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }        
    }

    /**
     * @tests java.lang.Thread#sleep(long)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "sleep",
        args = {long.class}
    )
    public void test_sleepJ() {
        // Test for method void java.lang.Thread.sleep(long)

        // TODO : Test needs enhancing.
        long stime = 0, ftime = 0;
        try {
            stime = System.currentTimeMillis();
            Thread.sleep(1000);
            ftime = System.currentTimeMillis();
        } catch (InterruptedException e) {
            fail("Unexpected interrupt received");
        }
        assertTrue("Failed to sleep long enough", (ftime - stime) >= 800);
        
        counter = 0;
        st = new Thread() {
           
            public void run() {
                while(true) {
                    try {
                        sleep(1000);
                        counter++;
                    } catch(InterruptedException e) {
                        
                    }
                }
            }
        };
       
        st.start();

        try {
            Thread.sleep(5000);
        } catch(InterruptedException e) {
            fail("InterruptedException was thrown.");
        }
        assertEquals(4, counter);
        
        st = new Thread() {
            public void run() {
                try {
                    sleep(10000);
                    fail("InterruptedException is thrown.");
                } catch(InterruptedException ie) {
                    //exception
                }
            }
        };

        st.start();
        st.interrupt();
    }

    /**
     * @tests java.lang.Thread#sleep(long, int)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "sleep",
        args = {long.class, int.class}
    )
    public void test_sleepJI() {
        // Test for method void java.lang.Thread.sleep(long, int)

        // TODO : Test needs revisiting.
        long stime = 0, ftime = 0;
        try {
            stime = System.currentTimeMillis();
            Thread.sleep(1000, 999999);
            ftime = System.currentTimeMillis();
        } catch (InterruptedException e) {
            fail("Unexpected interrupt received");
        }
        long result = ftime - stime;
        assertTrue("Failed to sleep long enough: " + result, result >= 900
                && result <= 1100);
        
        counter = 0;
        st = new Thread() {
           
            public void run() {
                while(true) {
                    try {
                        sleep(0, 999999);
                        counter++;
                    } catch(InterruptedException e) {
                        
                    }
                }
            }
        };
       
        st.start();

        try {
            Thread.sleep(2, 999999);
        } catch(InterruptedException e) {
            fail("InterruptedException was thrown.");
        }
        assertEquals(2, counter);
        
        st = new Thread() {
            public void run() {
                try {
                    sleep(10000, 999999);
                    fail("InterruptedException is thrown.");
                } catch(InterruptedException ie) {
                    //exception
                }
            }
        };
        st.start();
        st.interrupt();
    }

    /**
     * @tests java.lang.Thread#start()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "start",
        args = {}
    )
    public void test_start() {
        // Test for method void java.lang.Thread.start()
        try {
            ResSupThread t = new ResSupThread(Thread.currentThread());
            synchronized (t) {
                ct = new Thread(t, "Interrupt Test4");
                ct.start();
                t.wait();
            }
            assertTrue("Thread is not running1", ct.isAlive());
            // Let the child thread get going.
            int orgval = t.getCheckVal();
            Thread.sleep(150);
            assertTrue("Thread is not running2", orgval != t.getCheckVal());
            ct.interrupt();
        } catch (InterruptedException e) {
            fail("Unexpected interrupt occurred");
        }
        Thread thr = new Thread();
        thr.start();
        try {
            thr.start();
        } catch(IllegalThreadStateException itse){
            //expected
        }
    }

    /**
     * @tests java.lang.Thread#stop()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "stop",
        args = {}
    )
    @AndroidOnly("Android throws UnsupportedOperationException for Thread.stop()")
    @SuppressWarnings("deprecation")
    public void test_stop() {
        // Test for method void java.lang.Thread.stop()
        try {
            Runnable r = new ResSupThread(null);
            synchronized (r) {
                st = new Thread(r, "Interupt Test5");
                st.start();
                r.wait();
            }

        } catch (InterruptedException e) {
            fail("Unexpected interrupt received");
        }
        try {
            st.stop();
            fail("Expected UnsupportedOperationException because" +
                    "Thread.stop is not supported.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // Security checks are made even though stop() is not supported.
        SecurityManager sm = new SecurityManager() {
            public void checkPermission(Permission perm) {
                if(perm.getName().equals("stopThread")) {
                    throw new SecurityException(); 
                }
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.stop();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        } 
    }

    /**
     * @tests java.lang.Thread#stop(java.lang.Throwable)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "Verifies security.",
        method = "stop",
        args = {java.lang.Throwable.class}
    )
    @SuppressWarnings("deprecation")
    public void test_stopLjava_lang_Throwable_subtest0() {
        // Security checks are made even though stop(Throwable) is not supported.
        Thread t = new Thread("t");
        class MySecurityManager extends SecurityManager {
            @Override
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
            @Override
            public void checkPermission(Permission permission) {

                if(permission.getName().equals("stopThread")) {
                    throw new SecurityException();
                }
            }
        }
        MySecurityManager sm = new MySecurityManager();
        System.setSecurityManager(sm);
        try {
            try {
                t.stop(new ThreadDeath());
                fail("SecurityException was thrown.");
            } catch (SecurityException e) {
            }
            t.start();
            try {
                t.join(1000);
            } catch (InterruptedException e) {
            }
            try {
                t.stop(new ThreadDeath());
                fail("SecurityException was thrown.");
            } catch (SecurityException e) {
            }

        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * @tests java.lang.Thread#stop(java.lang.Throwable)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "SecurityException is not verified.",
        method = "stop",
        args = {java.lang.Throwable.class}
    )
    @AndroidOnly("Android throws UnsupportedOperationException for Thread.stop(Thorwable)")     
    @SuppressWarnings("deprecation")
    public void test_stopLjava_lang_Throwable() {
        // Test for method void java.lang.Thread.stop(java.lang.Throwable)
        ResSupThread t = new ResSupThread(Thread.currentThread());
        synchronized (t) {
            st = new Thread(t, "StopThread");
            st.setPriority(Thread.MAX_PRIORITY);
            st.start();
            try {
                t.wait();
            } catch (InterruptedException e) {
            }
        }
        try {
            st.stop(new BogusException("Bogus"));
            fail("Expected UnsupportedOperationException because" +
                    "Thread.stop is not supported.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            st.stop(null);
            fail("Expected NullPointerException was not thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests java.lang.Thread#suspend()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "suspend",
        args = {}
    )
    @AndroidOnly("Android throws UnsupportedOperationException for Thread.suspend()")     
    @SuppressWarnings("deprecation")
    public void test_suspend() {
        // Test for method void java.lang.Thread.suspend()
        int orgval;
        ResSupThread t = new ResSupThread(Thread.currentThread());
        try {
            synchronized (t) {
                ct = new Thread(t, "Interupt Test6");
                ct.start();
                t.wait();
            }
            ct.suspend();
            fail("Expected UnsupportedOperationException because" +
                    "Thread.suspend is not supported.");
        } catch (UnsupportedOperationException e) {
            // expected
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException was thrown");
        }

        // Security checks are made even though suspend() is not supported.
        SecurityManager sm = new SecurityManager() {
            public void checkPermission(Permission perm) {
            }
            
            public void checkAccess(Thread t) {
                throw new SecurityException();
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.suspend();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }          
    }

    /**
     * @tests java.lang.Thread#toString()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "toString",
        args = {}
    )
    public void test_toString() {
        // Test for method java.lang.String java.lang.Thread.toString()
        ThreadGroup tg = new ThreadGroup("Test Group5");
        st = new Thread(tg, new SimpleThread(1), "SimpleThread17");
        final String stString = st.toString();
        final String expected = "Thread[SimpleThread17,5,Test Group5]";
        assertTrue("Returned incorrect string: " + stString + "\t(expecting :"
                + expected + ")", stString.equals(expected));
        st.start();
        try {
            st.join();
        } catch (InterruptedException e) {
        }
        tg.destroy();
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "yield",
        args = {}
    )
    public void test_yield() {

        Counter [] countersNotYeld = new Counter[10];
        
        for(int i = 0; i < 10; i++) {
            countersNotYeld[i] = new Counter(false);
        }
        Counter countersYeld = new Counter(true);
        try {
            Thread.sleep(11000);
        } catch(InterruptedException ie) {}                
       
        for(Counter c:countersNotYeld) {
            assertTrue(countersYeld.counter == c.counter);
        }
    }
    
    class Counter extends Thread {
        public int counter = 0;
        boolean isDoYield = false;
        
        public Counter(boolean isDoYield) {
            this.isDoYield = isDoYield;
            start();
        }
        
        public void run() {
            for(int i = 0; i < 10000; i++) {
                if(isDoYield)
                    yield();
                counter ++;
            }
        }
    } 

    
    /**
     * @tests java.lang.Thread#getAllStackTraces()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getAllStackTraces",
        args = {}
    )
    public void test_getAllStackTraces() {
        Map<Thread, StackTraceElement[]> stMap = Thread.getAllStackTraces();
        assertNotNull(stMap);
        //TODO add security-based tests
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().equals("modifyThreadGroup")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            Thread.getAllStackTraces();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
           System.setSecurityManager(oldSm);
        }
    }
    
    /**
     * @tests java.lang.Thread#getDefaultUncaughtExceptionHandler
     * @tests java.lang.Thread#setDefaultUncaughtExceptionHandler
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setDefaultUncaughtExceptionHandler",
            args = {java.lang.Thread.UncaughtExceptionHandler.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getDefaultUncaughtExceptionHandler",
            args = {}
        )
    })
    public void test_get_setDefaultUncaughtExceptionHandler() {
        class Handler implements UncaughtExceptionHandler {
            public void uncaughtException(Thread thread, Throwable ex) {
            }
        }
        
        final Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        assertSame(handler, Thread.getDefaultUncaughtExceptionHandler());
        
        Thread.setDefaultUncaughtExceptionHandler(null);
        assertNull(Thread.getDefaultUncaughtExceptionHandler());
        //TODO add security-based tests
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().
                        equals("setDefaultUncaughtExceptionHandler")) {
                    throw new SecurityException();
                }
            }
        };

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setDefaultUncaughtExceptionHandler(handler);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }  finally {
            System.setSecurityManager(oldSm);           
        }
    }
    
    /**
     * @tests java.lang.Thread#getStackTrace()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getStackTrace",
        args = {}
    )
    public void test_getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        assertNotNull(stackTrace);

        stack_trace_loop: {
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement e = stackTrace[i];
                if (getClass().getName().equals(e.getClassName())) {
                    if ("test_getStackTrace".equals(e.getMethodName())) {
                        break stack_trace_loop;
                    }
                }
            }
            fail("class and method not found in stack trace");
        }
        
        //TODO add security-based tests
        
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
                if(perm.getName().
                        equals("getStackTrace")) {
                    throw new SecurityException();
                }
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.getStackTrace();
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }  finally {
            System.setSecurityManager(oldSm);           
        }
    }
    
    /**
     * @tests java.lang.Thread#getState()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getState",
        args = {}
    )
    public void test_getState() {
        Thread.State state = Thread.currentThread().getState();
        assertNotNull(state);
        assertEquals(Thread.State.RUNNABLE, state);
        
        final Semaphore sem = new Semaphore(0);
        final Object lock = new Object();
        Thread th = new Thread() {
            @Override
            public void run() {
                  while (!sem.hasQueuedThreads()) {}
                  sem.release();
                  while (run) {}
                  try {
                      sem.acquire();
                  } catch (InterruptedException e) {
                      fail("InterruptedException was thrown.");
                  }
                  synchronized (lock) {
                      lock.equals(new Object());
                  }
                  synchronized (lock) {
                      try {
                        sem.release();
                        lock.wait(Long.MAX_VALUE);
                      } catch (InterruptedException e) {
                          // expected
                      }
                  }
            }
        };
        assertEquals(Thread.State.NEW, th.getState());
        th.start();
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            fail("InterruptedException was thrown.");
        }
        assertEquals(Thread.State.RUNNABLE, th.getState());
        run = false;

        while (!sem.hasQueuedThreads()){}
        
        assertEquals(Thread.State.WAITING, th.getState());
        synchronized (lock) {
            sem.release();
            long start = System.currentTimeMillis();
            while(start + 1000 > System.currentTimeMillis()) {}
            assertEquals(Thread.State.BLOCKED, th.getState());
        }

        try {
            sem.acquire();
        } catch (InterruptedException e) {
            fail("InterruptedException was thrown.");
        }
        
        synchronized (lock) {
            assertEquals(Thread.State.TIMED_WAITING, th.getState());
            th.interrupt();
        }
        
        try {
            th.join(1000);
        } catch(InterruptedException ie) {
            fail("InterruptedException was thrown.");
        }
        assertEquals(Thread.State.TERMINATED, th.getState());
    }
    boolean run = true;
    
    /**
     * @tests java.lang.Thread#getUncaughtExceptionHandler
     * @tests java.lang.Thread#setUncaughtExceptionHandler
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "getUncaughtExceptionHandler",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "setUncaughtExceptionHandler",
            args = {java.lang.Thread.UncaughtExceptionHandler.class}
        )
    })
    public void test_get_setUncaughtExceptionHandler() {
        class Handler implements UncaughtExceptionHandler {
            public void uncaughtException(Thread thread, Throwable ex) {
            }
        }
        
        final Handler handler = new Handler();
        Thread.currentThread().setUncaughtExceptionHandler(handler);
        assertSame(handler, Thread.currentThread().getUncaughtExceptionHandler());
        
        Thread.currentThread().setUncaughtExceptionHandler(null);

        //TODO add security-based tests
        SecurityManager sm = new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
           
            public void checkAccess(Thread t) {
               throw new SecurityException();
            }
        };
        st = new Thread();
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            st.setUncaughtExceptionHandler(handler);
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

    }
    
    /**
     * @tests java.lang.Thread#getId()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getId",
        args = {}
    )
    public void test_getId() {
        assertTrue("current thread's ID is not positive", Thread.currentThread().getId() > 0);
        
        //check all the current threads for positive IDs
        Map<Thread, StackTraceElement[]> stMap = Thread.getAllStackTraces();
        for (Thread thread : stMap.keySet()) {
            assertTrue("thread's ID is not positive: " + thread.getName(), thread.getId() > 0);
        }
    }

    /**
     * @tests java.lang.Thread#holdLock()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "holdsLock",
        args = {java.lang.Object.class}
    )
    public void test_holdsLock() {
        MonitoredClass monitor = new MonitoredClass();
        
        monitor.enterLocked();
        monitor.enterNonLocked();
    
        try {
            Thread.holdsLock(null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException npe) {
            //expected
        }
    }

    @Override
    protected void tearDown() {
        try {
            if (st != null)
                st.interrupt();
        } catch (Exception e) {
        }
        try {
            if (spinner != null)
                spinner.interrupt();
        } catch (Exception e) {
        }
        try {
            if (ct != null)
                ct.interrupt();
        } catch (Exception e) {
        }

        try {
            spinner = null;
            st = null;
            ct = null;
            System.runFinalization();
        } catch (Exception e) {
        }
    }
}
