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

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass; 

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.WriteAbortedException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Automated Test Suite for class java.io.ObjectOutputStream
 * 
 */
@TestTargetClass(Serializable.class) 
public class SerializationStressTest extends junit.framework.TestCase implements
        Serializable {

    // protected static final String MODE_XLOAD = "xload";

    // protected static final String MODE_XDUMP = "xdump";

    static final String FOO = "foo";

    static final String MSG_TEST_FAILED = "Failed to write/read/assertion checking: ";

    protected static final boolean DEBUG = false;

    protected static boolean xload = false;

    protected static boolean xdump = false;

    protected static String xFileName = null;

    protected transient int dumpCount = 0;

    protected transient ObjectInputStream ois;

    protected transient ObjectOutputStream oos;

    protected transient ByteArrayOutputStream bao;

    protected void t_MixPrimitivesAndObjects() throws IOException,
            ClassNotFoundException {
        int i = 7;
        String s1 = "string 1";
        String s2 = "string 2";
        byte[] bytes = { 1, 2, 3 };

        oos.writeInt(i);
        oos.writeObject(s1);
        oos.writeUTF(s2);
        oos.writeObject(bytes);
        oos.close();
        try {
            ois = new ObjectInputStream(loadStream());

            int j = ois.readInt();
            assertTrue("Wrong int :" + j, i == j);

            String l1 = (String) ois.readObject();
            assertTrue("Wrong obj String :" + l1, s1.equals(l1));

            String l2 = (String) ois.readUTF();
            assertTrue("Wrong UTF String :" + l2, s2.equals(l2));

            byte[] bytes2 = (byte[]) ois.readObject();
            assertTrue("Wrong byte[]", Arrays.equals(bytes, bytes2));

        } finally {
            ois.close();
        }
    }

    // -----------------------------------------------------------------------------------

    static final Map TABLE = new Hashtable();

    static final Map MAP = new HashMap();

    static final SortedMap TREE = new TreeMap();

    static final LinkedHashMap LINKEDMAP = new LinkedHashMap();

    static final LinkedHashSet LINKEDSET = new LinkedHashSet();

    static final IdentityHashMap IDENTITYMAP = new IdentityHashMap();

    static final List ALIST = Arrays.asList(new String[] { "a", "list", "of",
            "strings" });

    static final List LIST = new ArrayList(ALIST);

    static final Set SET = new HashSet(Arrays.asList(new String[] { "one",
            "two", "three" }));

    static final Permission PERM = new PropertyPermission("file.encoding",
            "write");

    static final PermissionCollection PERMCOL = PERM.newPermissionCollection();

    static final SortedSet SORTSET = new TreeSet(Arrays.asList(new String[] {
            "one", "two", "three" }));

    static final java.text.DateFormat DATEFORM = java.text.DateFormat
            .getInstance();

    static final java.text.ChoiceFormat CHOICE = new java.text.ChoiceFormat(
            "1#one|2#two|3#three");

    static final java.text.NumberFormat NUMBERFORM = java.text.NumberFormat
            .getInstance();

    static final java.text.MessageFormat MESSAGE = new java.text.MessageFormat(
            "the time: {0,time} and date {0,date}");

    static final LinkedList LINKEDLIST = new LinkedList(Arrays
            .asList(new String[] { "a", "linked", "list", "of", "strings" }));

    static final SimpleTimeZone TIME_ZONE = new SimpleTimeZone(3600000,
            "S-TEST");

    static final Calendar CALENDAR = new GregorianCalendar(TIME_ZONE);
    
    static Exception INITIALIZE_EXCEPTION = null;

    static {
    	try {
	        TABLE.put("one", "1");
	        TABLE.put("two", "2");
	        TABLE.put("three", "3");
	        MAP.put("one", "1");
	        MAP.put("two", "2");
	        MAP.put("three", "3");
	        LINKEDMAP.put("one", "1");
	        LINKEDMAP.put("two", "2");
	        LINKEDMAP.put("three", "3");
	        IDENTITYMAP.put("one", "1");
	        IDENTITYMAP.put("two", "2");
	        IDENTITYMAP.put("three", "3");
	        LINKEDSET.add("one");
	        LINKEDSET.add("two");
	        LINKEDSET.add("three");
	        TREE.put("one", "1");
	        TREE.put("two", "2");
	        TREE.put("three", "3");
	        PERMCOL.add(PERM);
	        // To make sure they all use the same Calendar
	        CALENDAR.setTimeZone(new SimpleTimeZone(0, "GMT"));
	        CALENDAR.set(1999, Calendar.JUNE, 23, 15, 47, 13);
	        CALENDAR.set(Calendar.MILLISECOND, 553);
	        DATEFORM.setCalendar(CALENDAR);
	        java.text.DateFormatSymbols symbols = new java.text.DateFormatSymbols();
	        symbols.setZoneStrings(new String[][] { { "a", "b", "c", "d" },
	                { "e", "f", "g", "h" } });
	        ((java.text.SimpleDateFormat) DATEFORM).setDateFormatSymbols(symbols);
	        DATEFORM.setNumberFormat(new java.text.DecimalFormat("#.#;'-'#.#"));
	        DATEFORM.setTimeZone(TimeZone.getTimeZone("EST"));
	        ((java.text.DecimalFormat) NUMBERFORM).applyPattern("#.#;'-'#.#");
	        MESSAGE.setFormat(0, DATEFORM);
	        MESSAGE.setFormat(1, DATEFORM);
    	} catch (Exception e) {
    		INITIALIZE_EXCEPTION = e;
    	}
    }

    public SerializationStressTest() {
    }

    public SerializationStressTest(String name) {
        super(name);
    }

    public String getDumpName() {
        return getName() + dumpCount;
    }

    protected void dump(Object o) throws IOException, ClassNotFoundException {
        if (dumpCount > 0)
            setUp();
        // Dump the object
        try {
            oos.writeObject(o);
        } finally {
            oos.close();
        }
    }

    protected Object dumpAndReload(Object o) throws IOException,
            ClassNotFoundException {
        dump(o);
        return reload();
    }

    protected InputStream loadStream() throws IOException {
        // Choose the load stream
        if (xload || xdump) {
            // Load from pre-existing file
            return new FileInputStream(xFileName + "-" + getDumpName() + ".ser");
        } else {
            // Just load from memory, we dumped to memory
            return new ByteArrayInputStream(bao.toByteArray());
        }
    }

    protected Object reload() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(loadStream());
        dumpCount++;
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
    	if (INITIALIZE_EXCEPTION != null) {
    		throw new ExceptionInInitializerError(INITIALIZE_EXCEPTION);
    	}
        try {
            if (xdump) {
                oos = new ObjectOutputStream(new FileOutputStream(xFileName
                        + "-" + getDumpName() + ".ser"));
            } else {
                oos = new ObjectOutputStream(bao = new ByteArrayOutputStream());
            }
        } catch (Exception e) {
            fail("Exception thrown during setup : " + e.getMessage());
        }
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        if (oos != null) {
            try {
                oos.close();
            } catch (Exception e) {
            }
        }
    }


}
