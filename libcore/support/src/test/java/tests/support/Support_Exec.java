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

package tests.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import junit.framework.TestCase;

public class Support_Exec extends TestCase {

    static final boolean againstDalvik;
    static {
        String platform = System.getProperty("java.vendor");
        againstDalvik = (platform.contains("Android"));
    }

    /**
     *  This function returns the output of the process as a string
     */
    public static String execJava(String[] args, String[] classpath,
            boolean displayOutput) throws IOException, InterruptedException {
        Object[] arr =
                execJavaCommon(args, classpath, null, displayOutput, true);

        return getProcessOutput(arr, displayOutput);
    }

    /**
     * This function returns the output of the process as a string
     */
    public static String execJava(String[] args, String[] classpath, String[] envp,
            boolean displayOutput) throws IOException, InterruptedException {
        Object[] arr =
                execJavaCommon(args, classpath, envp, displayOutput, false);

        return getProcessOutput(arr, displayOutput);
    }

    public static String getProcessOutput(Object[] arr, boolean displayOutput)
            throws IOException, InterruptedException {
        Process proc = (Process) arr[0];
        StringBuilder output = new StringBuilder();
        InputStream in = proc.getInputStream();
        int result;
        byte[] bytes = new byte[1024];

        while ((result = in.read(bytes)) != -1) {
            output.append(new String(bytes, 0, result));

            if (displayOutput) {
                System.out.write(bytes, 0, result);
            }
        }

        in.close();
        proc.waitFor();
        checkStderr(arr);
        proc.destroy();

        return output.toString();
    }

    public static void checkStderr(Object[] execArgs) {
        StringBuilder errBuf = (StringBuilder) execArgs[1];

        synchronized (errBuf) {
            if (errBuf.length() > 0) {
                fail(errBuf.toString());
            }
        }
    }

    public static Object[] execJava2(String[] args, String[] classpath,
            boolean displayOutput) throws IOException, InterruptedException {
        return execJavaCommon(args, classpath, null, displayOutput, true);
    }

    private static Object[] execJavaCommon(String[] args, String[] classpath,
            String[] envp, boolean displayOutput, boolean appendToSystemClassPath)
            throws IOException, InterruptedException {
        // this function returns the resulting process from the exec
        ArrayList<String> execArgs = null;
        StringBuilder classPathString = new StringBuilder();
        StringBuilder command;
        String executable;
        String testVMArgs;
        StringTokenizer st;

        execArgs = new ArrayList<String>(3 + args.length);

        // construct the name of executable file
        executable = System.getProperty("java.home");
        if (!executable.endsWith(File.separator)) {
            executable += File.separator;
        }
        executable += "bin" + File.separator;
        if (againstDalvik) {
            execArgs.add(executable + "dalvikvm");
        } else {
            execArgs.add(executable + "java");
        }

        // add classpath string
        if (classpath != null) {
            for (String element : classpath) {
                classPathString.append(File.pathSeparator);
                classPathString.append(element);
            }
        }
        if (appendToSystemClassPath) {
            execArgs.add("-cp");
            execArgs.add(System.getProperty("java.class.path") +
                    classPathString);
        } else {
            if (classpath != null) {
                execArgs.add("-cp");
                execArgs.add(classPathString.toString());
            }
        }

        // parse hy.test.vmargs if was given
        testVMArgs = System.getProperty("hy.test.vmargs");
        if (testVMArgs != null) {
            st = new StringTokenizer(testVMArgs, " ");

            while (st.hasMoreTokens()) {
                execArgs.add(st.nextToken());
            }
        }

        // add custom args given as parameter
        for (String arg : args) {
            execArgs.add(arg);
        }

        if (displayOutput) {
            // Construct command line string and print it to stdout.
            command = new StringBuilder(execArgs.get(0));
            for (int i = 1; i < execArgs.size(); i++) {
                command.append(" ");
                command.append(execArgs.get(i));
            }
            System.out.println();
            System.out.println("Exec: " + command.toString());
        }

        // execute java process
        return execAndDigestOutput(execArgs.toArray(new String[execArgs.size()]), envp);
    }

    //
    // mc: This looks like functionaly worth publicity:
    //
    public static Object[] execAndDigestOutput (String[] cmdLine, String[] envp)
            throws IOException, InterruptedException {

//        System.out.println("Commandline BEGIN");
//        for (int i = 0; i < cmdLine.length; i++) {
//            System.out.println(cmdLine[i]);
//        }
//        System.out.println("END");

        final Process proc = Runtime.getRuntime().exec(cmdLine, envp);
        final StringBuilder errBuf = new StringBuilder();

        Thread errThread = new Thread(new Runnable() {
            public void run() {
                synchronized (errBuf) {
                    InputStream err;
                    int result;
                    byte[] bytes = new byte[1024];

                    synchronized (proc) {
                        proc.notifyAll();
                    }

                    err = proc.getErrorStream();
                    try {
                        while ((result = err.read(bytes)) != -1) {
                            System.err.write(bytes, 0, result);
                            errBuf.append(new String(bytes));
                        }
                        err.close();
                    } catch (IOException e) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        PrintStream printer = new PrintStream(out);

                        e.printStackTrace();
                        e.printStackTrace(printer);
                        printer.close();
                        errBuf.append(new String(out.toByteArray()));
                    }
                }
            }
        });

        synchronized (proc) {
            errThread.start();
            // wait for errThread to start
            proc.wait();
        }

        return new Object[] { proc, errBuf };
    }

}
