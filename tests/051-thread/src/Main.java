// Copyright 2006 The Android Open Source Project

/**
 * Test some basic thread stuff.
 */
public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 512; i++) {
            MyThread myThread = new MyThread();
            myThread.start();
            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        go();
        System.out.println("thread test done");
    }

    public static void go() {
        Thread t = new Thread(null, new ThreadTestSub(), "Thready", 7168);

        t.setDaemon(false);

        System.out.print("Starting thread '" + t.getName() + "'\n");
        t.start();

        try {
            t.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.print("Thread starter returning\n");
    }

    /*
     * Simple thread capacity test.
     */
    static class MyThread extends Thread {
        private static int mCount = 0;
	static Object mSyncable = new Object();
        public void run() {
	    synchronized (mSyncable)
	    {
		System.out.println("running " + (mCount++));
	    }
        }
    }
}

class ThreadTestSub implements Runnable {
    public void run() {
        System.out.print("@ Thread running\n");

        try {
            Thread.currentThread().setDaemon(true);
            System.out.print("@ FAILED: setDaemon() succeeded\n");
        } catch (IllegalThreadStateException itse) {
            System.out.print("@ Got expected setDaemon exception\n");
        }

        //if (true)
        //    throw new NullPointerException();
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException ie) {
            System.out.print("@ Interrupted!\n");
        }
        finally {
            System.out.print("@ Thread bailing\n");
        }
    }
}
