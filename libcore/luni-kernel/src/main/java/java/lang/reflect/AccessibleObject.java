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

package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.Hashtable;

import org.apache.harmony.kernel.vm.StringUtils;
import org.apache.harmony.kernel.vm.ReflectionAccess;

/**
 * {@code AccessibleObject} is the superclass of all member reflection classes
 * (Field, Constructor, Method). AccessibleObject provides the ability to toggle
 * a flag controlling access checks for these objects. By default, accessing a
 * member (for example, setting a field or invoking a method) checks the
 * validity of the access (for example, invoking a private method from outside
 * the defining class is prohibited) and throws IllegalAccessException if the
 * operation is not permitted. If the accessible flag is set to true, these
 * checks are omitted. This allows privileged code, such as Java object
 * serialization, object inspectors, and debuggers to have complete access to
 * objects.
 * 
 * @see Field
 * @see Constructor
 * @see Method
 * @see ReflectPermission
 * 
 * @since Android 1.0
 */
public class AccessibleObject implements AnnotatedElement {

    // If true, object is accessible, bypassing normal security checks
    boolean flag = false;

    /**
     * one dimensional array
     */
    private static final String DIMENSION_1 = "[]";
    
    /**
     * two dimensional array
     */
    private static final String DIMENSION_2 = "[][]";

    /**
     * three dimensional array
     */
    private static final String DIMENSION_3 = "[][][]";
    
    // Holds a mapping from Java type names to native type codes.
    static Hashtable<String, String> trans;
    
    static {
        trans = new Hashtable<String, String>(9);
        trans.put("byte", "B");
        trans.put("char", "C");
        trans.put("short", "S");
        trans.put("int", "I");
        trans.put("long", "J");
        trans.put("float", "F");
        trans.put("double", "D");
        trans.put("void", "V");
        trans.put("boolean", "Z");
    }

    /**
     * Attempts to set the value of the accessible flag for all the objects in
     * the array provided. Only one security check is performed. Setting this
     * flag to {@code false} will enable access checks, setting to {@code true}
     * will disable them. If there is a security manager, checkPermission is
     * called with a {@code ReflectPermission("suppressAccessChecks")}.
     * 
     * @param objects
     *            the accessible objects
     * @param flag
     *            the new value for the accessible flag
     *            
     * @throws SecurityException
     *             if the request is denied
     *             
     * @see #setAccessible(boolean)
     * @see ReflectPermission
     * 
     * @since Android 1.0
     */
    public static void setAccessible(AccessibleObject[] objects, boolean flag)
            throws SecurityException {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            smgr.checkPermission(new ReflectPermission("suppressAccessChecks"));
        }
        
        synchronized(AccessibleObject.class) {
            for (int i = 0; i < objects.length; i++) {
                objects[i].flag = flag;
            }
        }
    }

    /**
     * Constructs a new {@code AccessibleObject} instance. {@code
     * AccessibleObject} instances can only be constructed by the virtual
     * machine.
     * 
     * @since Android 1.0
     */
    protected AccessibleObject() {
        super();
    }

    /**
     * Indicates whether this object is accessible without security checks being
     * performed. Returns the accessible flag.
     * 
     * @return {@code true} if this object is accessible without security
     *         checks, {@code false} otherwise
     *         
     * @since Android 1.0
     */
    public boolean isAccessible() {
        return flag;
    }

    /**
     * Attempts to set the value of the accessible flag. Setting this flag to
     * {@code false} will enable access checks, setting to {@code true} will
     * disable them. If there is a security manager, checkPermission is called
     * with a {@code ReflectPermission("suppressAccessChecks")}.
     * 
     * @param flag
     *            the new value for the accessible flag
     *            
     * @throws SecurityException
     *             if the request is denied
     *             
     * @see ReflectPermission
     * 
     * @since Android 1.0
     */
    public void setAccessible(boolean flag) throws SecurityException {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            smgr.checkPermission(new ReflectPermission("suppressAccessChecks"));
        }
        
        this.flag = flag;
    }

    /**
     * Sets the accessible flag on this instance without doing any checks.
     * 
     * @param flag
     *            the new value for the accessible flag
     */
    /*package*/ void setAccessibleNoCheck(boolean flag) {
        this.flag = flag;
    }
    
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    public Annotation[] getDeclaredAnnotations() {
        throw new RuntimeException("subclass must override this method");
    }

    public Annotation[] getAnnotations() {
        // for all but Class, getAnnotations == getDeclaredAnnotations
        return getDeclaredAnnotations();
    }

    /* slow, but works for all sub-classes */
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException();
        }
        Annotation[] annos = getAnnotations();
        for (int i = annos.length-1; i >= 0; --i) {
            if (annos[i].annotationType() == annotationType) {
                return (T) annos[i];
            }
        }
        return null;
    }

    /**
     * Returns the signature for a class. This is the kind of signature used
     * internally by the JVM, with one-character codes representing the basic
     * types. It is not suitable for printing.
     *
     * @param clazz
     *            the class for which a signature is required
     * 
     * @return The signature as a string
     */
    String getSignature(Class<?> clazz) {
        String result = "";
        String nextType = clazz.getName();
        
        if(trans.containsKey(nextType)) {
            result = trans.get(nextType);
        } else {
            if(clazz.isArray()) {
                result = "[" + getSignature(clazz.getComponentType());   
            } else {
                result = "L" + nextType + ";";
            }
        }
        return result;
    }

    /**
     * Returns a printable String consisting of the canonical names of the
     * classes contained in an array. The form is that used in parameter and
     * exception lists, that is, the class or type names are separated by
     * commas.
     *
     * @param types
     *            the array of classes
     * 
     * @return The String of names
     */
    String toString(Class<?>[] types) {
        StringBuilder result = new StringBuilder();

        if (types.length != 0) {
            result.append(types[0].getCanonicalName());
            for (int i = 1; i < types.length; i++) {
                result.append(',');
                result.append(types[i].getCanonicalName());
            }
        }
        
        return result.toString();
    }

    /**
     * Gets the Signature attribute for this instance. Returns {@code null}
     * if not found.
     */
    /*package*/ String getSignatureAttribute() {
        /*
         * Note: This method would have been declared abstract, but the
         * standard API lists this class as concrete.
         */
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieve the signature attribute from an arbitrary class.  This is
     * the same as Class.getSignatureAttribute(), but it can be used from
     * the java.lang.reflect package.
     */
    /*package*/ static String getClassSignatureAttribute(Class clazz) {
        Object[] annotation = getClassSignatureAnnotation(clazz);

        if (annotation == null) {
            return null;
        }

        return StringUtils.combineStrings(annotation);
    }

    /**
     * Retrieve the signature annotation from an arbitrary class.  This is
     * the same as Class.getSignatureAttribute(), but it can be used from
     * the java.lang.reflect package.
     */
    private static native Object[] getClassSignatureAnnotation(Class clazz);

    /**
     * Gets the unique instance of {@link ReflectionAccessImpl}.
     *
     * @return non-null; the unique instance
     */
    static /*package*/ ReflectionAccess getReflectionAccess() {
        return ReflectionAccessImpl.THE_ONE;
    }
    
    
    /**
     * Appends the specified class name to the buffer. The class may represent
     * a simple type, a reference type or an array type.
     *
     * @param sb buffer
     * @param obj the class which name should be appended to the buffer
     * 
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayType(StringBuilder sb, Class<?> obj) {
        if (!obj.isArray()) {
            sb.append(obj.getName());
            return;
        }
        int dimensions = 1;
        Class simplified = obj.getComponentType();
        obj = simplified;
        while (simplified.isArray()) {
            obj = simplified;
            dimensions++;
        }
        sb.append(obj.getName());
        switch (dimensions) {
        case 1:
            sb.append(DIMENSION_1);
            break;
        case 2:
            sb.append(DIMENSION_2);
            break;
        case 3:
            sb.append(DIMENSION_3);
            break;
        default:
            for (; dimensions > 0; dimensions--) {
                sb.append(DIMENSION_1);
            }
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * Output format: java.lang.Object[], java.io.File, void
     *
     * @param sb buffer
     * @param objs array of classes to print the names
     * 
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayType(StringBuilder sb, Class[] objs) {
        if (objs.length > 0) {
            appendArrayType(sb, objs[0]);
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                appendArrayType(sb, objs[i]);
            }
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * Output format: java.lang.Object[], java.io.File, void
     *
     * @param sb buffer
     * @param objs array of classes to print the names
     * 
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendArrayGenericType(StringBuilder sb, Type[] objs) {
        if (objs.length > 0) {
            appendGenericType(sb, objs[0]);
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                appendGenericType(sb, objs[i]);
            }
        }
    }

    /**
     * Appends the generic type representation to the buffer.
     *
     * @param sb buffer
     * @param obj the generic type which representation should be appended to the buffer
     * 
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendGenericType(StringBuilder sb, Type obj) {
        if (obj instanceof TypeVariable) {
            sb.append(((TypeVariable)obj).getName());
        } else if (obj instanceof ParameterizedType) {
            sb.append(obj.toString());
        } else if (obj instanceof GenericArrayType) { //XXX: is it a working branch?
            Type simplified = ((GenericArrayType)obj).getGenericComponentType();
            appendGenericType(sb, simplified);
            sb.append("[]");
        } else if (obj instanceof Class) {
            Class c = ((Class<?>)obj);
            if (c.isArray()){
                String as[] = c.getName().split("\\[");
                int len = as.length-1;
                if (as[len].length() > 1){
                    sb.append(as[len].substring(1, as[len].length()-1));
                } else {
                    char ch = as[len].charAt(0);
                    if (ch == 'I')
                        sb.append("int");
                    else if (ch == 'B')
                        sb.append("byte");
                    else if (ch == 'J')
                        sb.append("long");
                    else if (ch == 'F')
                        sb.append("float");
                    else if (ch == 'D')
                        sb.append("double");
                    else if (ch == 'S')
                        sb.append("short");
                    else if (ch == 'C')
                        sb.append("char");
                    else if (ch == 'Z')
                        sb.append("boolean");
                    else if (ch == 'V') //XXX: is it a working branch?
                        sb.append("void");
                }
                for (int i = 0; i < len; i++){
                    sb.append("[]");
                }
            } else {
                sb.append(c.getName());
            }
        }
    }

    /**
     * Appends names of the specified array classes to the buffer. The array
     * elements may represent a simple type, a reference type or an array type.
     * In case if the specified array element represents an array type its
     * internal will be appended to the buffer.   
     * Output format: [Ljava.lang.Object;, java.io.File, void
     *
     * @param sb buffer
     * @param objs array of classes to print the names
     * 
     * @throws NullPointerException if any of the arguments is null 
     */
    void appendSimpleType(StringBuilder sb, Class<?>[] objs) {
        if (objs.length > 0) {
            sb.append(objs[0].getName());
            for (int i = 1; i < objs.length; i++) {
                sb.append(',');
                sb.append(objs[i].getName());
            }
        }
    }
}
