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
 * Copyright (C) 2006-2007 The Android Open Source Project
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

package java.lang;

import dalvik.system.VMStack;

import org.apache.harmony.kernel.vm.StringUtils;
import org.apache.harmony.luni.lang.reflect.GenericSignatureParser;
import org.apache.harmony.luni.lang.reflect.Types;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.ref.SoftReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.net.URL;
import java.security.ProtectionDomain;

import static java.lang.ClassCache.REFLECT;
import static java.lang.ClassCache.compareClassLists;
import static java.lang.ClassCache.findMethodByName;
import static java.lang.ClassCache.findFieldByName;

/**
 * The in-memory representation of a Java class. This representation serves as
 * the starting point for querying class-related information, a process usually
 * called "reflection". There are basically three types of {@code Class}
 * instances: those representing real classes and interfaces, those representing
 * primitive types, and those representing array classes.
 * 
 * <h4>Class instances representing object types (classes or interfaces)</h4>
 * <p>
 * These represent an ordinary class or interface as found in the class
 * hierarchy. The name associated with these {@code Class} instances is simply
 * the fully qualified class name of the class or interface that it represents.
 * In addition to this human-readable name, each class is also associated by a
 * so-called <em>signature</em>, which is the letter "L", followed by the
 * class name and a semicolon (";"). The signature is what the runtime system
 * uses internally for identifying the class (for example in a DEX file).
 * </p>
 * <h4>Classes representing primitive types</h4>
 * <p>
 * These represent the standard Java primitive types and hence share their
 * names (for example "int" for the {@code int} primitive type). Although it is
 * not possible to create new instances based on these {@code Class} instances,
 * they are still useful for providing reflection information, and as the
 * component type of array classes. There is one {@code Class} instance for each
 * primitive type, and their signatures are:
 * </p>
 * <ul>
 * <li>{@code B} representing the {@code byte} primitive type</li>
 * <li>{@code S} representing the {@code short} primitive type</li>
 * <li>{@code I} representing the {@code int} primitive type</li>
 * <li>{@code J} representing the {@code long} primitive type</li>
 * <li>{@code F} representing the {@code float} primitive type</li>
 * <li>{@code D} representing the {@code double} primitive type</li>
 * <li>{@code C} representing the {@code char} primitive type</li>
 * <li>{@code Z} representing the {@code boolean} primitive type</li>
 * <li>{@code V} representing void function return values</li>
 * </ul>
 * <p>
 * <h4>Classes representing array classes</h4>
 * <p>
 * These represent the classes of Java arrays. There is one such {@code Class}
 * instance per combination of array leaf component type and arity (number of
 * dimensions). In this case, the name associated with the {@code Class}
 * consists of one or more left square brackets (one per dimension in the array)
 * followed by the signature of the class representing the leaf component type,
 * which can be either an object type or a primitive type. The signature of a
 * {@code Class} representing an array type is the same as its name. Examples
 * of array class signatures are:
 * </p>
 * <ul>
 * <li>{@code [I} representing the {@code int[]} type</li>
 * <li>{@code [Ljava/lang/String;} representing the {@code String[]} type</li>
 * <li>{@code [[[C} representing the {@code char[][][]} type (three dimensions!)</li>
 * </ul>
 * 
 * @since Android 1.0
 */
public final class Class<T> implements Serializable, AnnotatedElement, GenericDeclaration, Type {

    private static final long serialVersionUID = 3206093459760846163L;

    // TODO How is this field being initialized? What's it being used for?
    private ProtectionDomain pd;

    /**
     * null-ok; cache of reflective information, wrapped in a soft
     * reference
     */
    private volatile SoftReference<ClassCache<T>> cacheRef;
    
    private Class() {
        // Prevent this class to be instantiated, instance
        // should be created by JVM only
    }

    /**
     * Get the Signature attribute for this class.  Returns null if not found.
     */
    private String getSignatureAttribute() {
        Object[] annotation = getSignatureAnnotation();

        if (annotation == null) {
            return null;
        }

        return StringUtils.combineStrings(annotation);
    }

    /**
     * Get the Signature annotation for this class.  Returns null if not found.
     */
    native private Object[] getSignatureAnnotation();

    /**
     * Returns a {@code Class} object which represents the class with the
     * specified name. The name should be the name of a class as described in
     * the {@link Class class definition}; however, {@code Class}es representing
     * primitive types can not be found using this method.
     * <p>
     * If the class has not been loaded so far, it is being loaded and linked
     * first. This is done through either the class loader of the calling class
     * or one of its parent class loaders. The class is also being initialized,
     * which means that a possible static initializer block is executed.  
     * 
     * @param className
     *            the name of the non-primitive-type class to find.
     * @return the named {@code Class} instance.
     * @throws ClassNotFoundException
     *             if the requested class can not be found.
     * @throws LinkageError
     *             if an error occurs during linkage
     * @throws ExceptionInInitializerError
     *             if an exception occurs during static initialization of a
     *             class.
     * @since Android 1.0
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        return forName(className, true, VMStack.getCallingClassLoader());
    }

    /**
     * Returns a {@code Class} object which represents the class with the
     * specified name. The name should be the name of a class as described in
     * the {@link Class class definition}, however {@code Class}es representing
     * primitive types can not be found using this method. Security rules will
     * be obeyed.
     * <p>
     * If the class has not been loaded so far, it is being loaded and linked
     * first. This is done through either the specified class loader or one of
     * its parent class loaders. The caller can also request the class to be
     * initialized, which means that a possible static initializer block is
     * executed.  
     * 
     * @param className
     *            the name of the non-primitive-type class to find.
     * @param initializeBoolean
     *            indicates whether the class should be initialized.
     * @param classLoader
     *            the class loader to use to load the class.
     * @return the named {@code Class} instance.
     * @throws ClassNotFoundException
     *             if the requested class can not be found.
     * @throws LinkageError
     *             if an error occurs during linkage
     * @throws ExceptionInInitializerError
     *             if an exception occurs during static initialization of a
     *             class.
     * @since Android 1.0
     */
    public static Class<?> forName(String className, boolean initializeBoolean,
            ClassLoader classLoader) throws ClassNotFoundException {
        
        if (classLoader == null) {
            SecurityManager smgr = System.getSecurityManager();
            if (smgr != null) {
                ClassLoader calling = VMStack.getCallingClassLoader();
                if (calling != null) {
                    smgr.checkPermission(new RuntimePermission("getClassLoader"));
                }
            }
            
            classLoader = ClassLoader.getSystemClassLoader();
        }
        // Catch an Exception thrown by the underlying native code. It wraps
        // up everything inside a ClassNotFoundException, even if e.g. an
        // Error occurred during initialization. This as a workaround for
        // an ExceptionInInitilaizerError that's also wrapped. It is actually
        // expected to be thrown. Maybe the same goes for other errors.
        // Not wrapping up all the errors will break android though.
        Class<?> result;
        try {
            result = classForName(className, initializeBoolean,
                    classLoader);
        } catch (ClassNotFoundException e) {
            Throwable cause = e.getCause(); 
            if (cause instanceof ExceptionInInitializerError) {
                throw (ExceptionInInitializerError) cause;
            }
            throw e;
        }
        return result;
    }

    /*
     * Returns a class by name without any security checks.
     *
     * @param className The name of the non-primitive type class to find
     * @param initializeBoolean A boolean indicating whether the class should be
     *        initialized
     * @param classLoader The class loader to use to load the class
     * @return the named class.
     * @throws ClassNotFoundException If the class could not be found
     */
    static native Class<?> classForName(String className, boolean initializeBoolean,
            ClassLoader classLoader) throws ClassNotFoundException;
    
    /**
     * Returns an array containing {@code Class} objects for all public classes
     * and interfaces that are members of this class. This includes public
     * members inherited from super classes and interfaces. If there are no such
     * class members or if this object represents a primitive type then an array
     * of length 0 is returned.
     * 
     * @return the public class members of the class represented by this object.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @since Android 1.0
     */
    public Class[] getClasses() {
        // BEGIN android-note
        // trying to get closer to the RI which returns a raw class array.
        // copied from newer version of harmony
        // END android-note
        checkPublicMemberAccess();
        return getFullListOfClasses(true);
    }

    /**
     * Returns the annotation of the given type. If there is no such annotation
     * then the method returns {@code null}.
     * 
     * @param annotationClass
     *            the annotation type.
     * @return the annotation of the given type, or {@code null} if there is no
     *         such annotation.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Annotation[] list = getAnnotations();
        for (int i = 0; i < list.length; i++) {
            if (annotationClass.isInstance(list[i])) {
                return (A)list[i];
            }
        }
        
        return null;
    }

    /**
     * Returns all the annotations of this class. If there are no annotations
     * then an empty array is returned.
     * 
     * @return a copy of the array containing this class' annotations.
     * @see #getDeclaredAnnotations()         
     * @since Android 1.0
     */
    public Annotation[] getAnnotations() {
        /*
         * We need to get the annotations declared on this class, plus the
         * annotations from superclasses that have the "@Inherited" annotation
         * set.  We create a temporary map to use while we accumulate the
         * annotations and convert it to an array at the end.
         *
         * It's possible to have duplicates when annotations are inherited.
         * We use a Map to filter those out.
         *
         * HashMap might be overkill here.
         */
        HashMap<Class, Annotation> map = new HashMap<Class, Annotation>();
        Annotation[] annos = getDeclaredAnnotations();

        for (int i = annos.length-1; i >= 0; --i)
            map.put(annos[i].annotationType(), annos[i]);

        for (Class sup = getSuperclass(); sup != null;
                sup = sup.getSuperclass()) {
            annos = sup.getDeclaredAnnotations();
            for (int i = annos.length-1; i >= 0; --i) {
                Class clazz = annos[i].annotationType();
                if (!map.containsKey(clazz) &&
                        clazz.isAnnotationPresent(Inherited.class)) {
                    map.put(clazz, annos[i]);
                }
            }
        }

        /* convert annotation values from HashMap to array */
        Collection<Annotation> coll = map.values();
        return coll.toArray(new Annotation[coll.size()]);
    }

    /**
     * Returns the canonical name of this class. If this class does not have a
     * canonical name as defined in the Java Language Specification, then the
     * method returns {@code null}.
     * 
     * @return this class' canonical name, or {@code null} if it does not have a
     *         canonical name.
     * @since Android 1.0
     */
    public String getCanonicalName() {
        if (isLocalClass() || isAnonymousClass())
            return null;

        if (isArray()) {
            /*
             * The canonical name of an array type depends on the (existence of)
             * the component type's canonical name. 
             */
            String name = getComponentType().getCanonicalName();
            if (name != null) {
                return name + "[]";
            } 
        } else if (isMemberClass()) {
            /*
             * The canonical name of an inner class depends on the (existence
             * of) the declaring class' canonical name. 
             */
            String name = getDeclaringClass().getCanonicalName();
            if (name != null) {
                return name + "." + getSimpleName();
            }
        } else {
            /*
             * The canonical name of a top-level class or primitive type is
             * equal to the fully qualified name. 
             */
            return getName();
        }
        
        /*
         * Other classes don't have a canonical name.
         */
        return null;
    }

    /**
     * Returns the class loader which was used to load the class represented by
     * this {@code Class}. Implementations are free to return {@code null} for
     * classes that were loaded by the bootstrap class loader. The Android
     * reference implementation, though, returns a reference to an actual
     * representation of the bootstrap class loader.
     * 
     * @return the class loader for the represented class.
     * @throws SecurityException
     *             if a security manager exists and it does not allow accessing
     *             the class loader.
     * @see ClassLoader
     * @since Android 1.0
     */
    public ClassLoader getClassLoader() {
        SecurityManager smgr = System.getSecurityManager();
        ClassLoader loader = getClassLoaderImpl();
        if (smgr != null && loader != null) {
            ClassLoader calling = VMStack.getCallingClassLoader();

            if (calling != null && !calling.isAncestorOf(loader)) {
                smgr.checkPermission(new RuntimePermission("getClassLoader"));
            }
        }
        
        if (this.isPrimitive()) {
            return null;
        }
        
        if (loader == null) {
            loader = BootClassLoader.getInstance();
        }
        
        return loader;
    }

    /**
     * This must be provided by the VM vendor, as it is used by other provided
     * class implementations in this package. Outside of this class, it is used
     * by SecurityManager.checkMemberAccess(), classLoaderDepth(),
     * currentClassLoader() and currentLoadedClass(). Return the ClassLoader for
     * this Class without doing any security checks. The bootstrap ClassLoader
     * is returned, unlike getClassLoader() which returns null in place of the
     * bootstrap ClassLoader.
     * 
     * @return the ClassLoader
     * @see ClassLoader#isSystemClassLoader()
     */
    ClassLoader getClassLoaderImpl() {
        return getClassLoader(this);
    }

    /*
     * Returns the defining class loader for the given class.
     * 
     * @param clazz the class the class loader of which we want
     * @return the class loader
     */
    private static native ClassLoader getClassLoader(Class<?> clazz);
    
    /**
     * Returns a {@code Class} object which represents the component type if
     * this class represents an array type. Returns {@code null} if this class
     * does not represent an array type. The component type of an array type is
     * the type of the elements of the array.
     * 
     * @return the component type of this class.
     * @since Android 1.0
     */
    public native Class<?> getComponentType();

    /**
     * Returns a {@code Constructor} object which represents the public
     * constructor matching the specified parameter types.
     * 
     * @param parameterTypes
     *            the parameter types of the requested constructor.
     * @return the constructor described by {@code parameterTypes}.
     * @throws NoSuchMethodException
     *             if the constructor can not be found.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredConstructor(Class...)
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public Constructor<T> getConstructor(Class... parameterTypes) throws NoSuchMethodException,
            SecurityException {
        checkPublicMemberAccess();
        return getMatchingConstructor(getDeclaredConstructors(this, true), parameterTypes);
    }

    /**
     * Returns an array containing {@code Constructor} objects for all public
     * constructors for the class represented by this {@code Class}. If there
     * are no public constructors or if this {@code Class} represents an array
     * class, a primitive type or void then an empty array is returned.
     * 
     * @return an array with the public constructors of the class represented by
     *         this {@code Class}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredConstructors()
     * @since Android 1.0
     */
    public Constructor[] getConstructors() throws SecurityException {
        // BEGIN android-note
        // trying to get closer to the RI which returns a raw constructor array.
        // copied from newer version of harmony
        // END android-note
        checkPublicMemberAccess();
        return getDeclaredConstructors(this, true);
    }

    /**
     * Returns the annotations that are directly defined on the class
     * represented by this {@code Class}. Annotations that are inherited are not
     * included in the result. If there are no annotations at all, an empty
     * array is returned.
     * 
     * @return a copy of the array containing the annotations defined for the
     *         class that this {@code Class} represents.
     * @see #getAnnotations()
     * @since Android 1.0
     */
    native public Annotation[] getDeclaredAnnotations();

    /**
     * Returns an array containing {@code Class} objects for all classes and
     * interfaces that are declared as members of the class which this {@code
     * Class} represents. If there are no classes or interfaces declared or if
     * this class represents an array class, a primitive type or void, then an
     * empty array is returned.
     * 
     * @return an array with {@code Class} objects for all the classes and
     *         interfaces that are used in member declarations.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @since Android 1.0
     */
    public Class[] getDeclaredClasses() throws SecurityException {
        // BEGIN android-note
        // trying to get closer to the RI which returns a raw class array.
        // copied from newer version of harmony
        // END android-note
        checkDeclaredMemberAccess();
        return getDeclaredClasses(this, false);
    }

    /*
     * Returns the list of member classes without performing any security checks
     * first. This includes the member classes inherited from superclasses. If no
     * member classes exist at all, an empty array is returned.
     * 
     * @param publicOnly reflects whether we want only public members or all of them
     * @return the list of classes
     */
    private Class<?>[] getFullListOfClasses(boolean publicOnly) {
        Class<?>[] result = getDeclaredClasses(this, publicOnly);
        
        // Traverse all superclasses
        Class<?> clazz = this.getSuperclass();
        while (clazz != null) {
            Class<?>[] temp = getDeclaredClasses(clazz, publicOnly);
            if (temp.length != 0) {
                result = arraycopy(new Class[result.length + temp.length], result, temp);
            }
            
            clazz = clazz.getSuperclass();
        }
        
        return result;
    }

    /*
     * Returns the list of member classes of the given class. No security checks
     * are performed. If no members exist, an empty array is returned.
     * 
     * @param clazz the class the members of which we want
     * @param publicOnly reflects whether we want only public member or all of them
     * @return the class' class members
     */
    native private static Class<?>[] getDeclaredClasses(Class<?> clazz,
        boolean publicOnly);
    
    /**
     * Returns a {@code Constructor} object which represents the constructor
     * matching the specified parameter types that is declared by the class
     * represented by this {@code Class}.
     * 
     * @param parameterTypes
     *            the parameter types of the requested constructor.
     * @return the constructor described by {@code parameterTypes}.
     * @throws NoSuchMethodException
     *             if the requested constructor can not be found.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getConstructor(Class...)
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public Constructor<T> getDeclaredConstructor(Class... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        checkDeclaredMemberAccess();
        return getMatchingConstructor(getDeclaredConstructors(this, false), parameterTypes);
    }

    /**
     * Returns an array containing {@code Constructor} objects for all
     * constructors declared in the class represented by this {@code Class}. If
     * there are no constructors or if this {@code Class} represents an array
     * class, a primitive type or void then an empty array is returned.
     * 
     * @return an array with the constructors declared in the class represented
     *         by this {@code Class}.
     * 
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getConstructors()
     * @since Android 1.0
     */
    public Constructor[] getDeclaredConstructors() throws SecurityException {
        // BEGIN android-note
        // trying to get closer to the RI which returns a raw constructor array.
        // copied from newer version of harmony
        // END android-note
        checkDeclaredMemberAccess();
        return getDeclaredConstructors(this, false);
    }

    /*
     * Returns the list of constructors without performing any security checks
     * first. If no constructors exist, an empty array is returned.
     *
     * @param clazz the class of interest
     * @param publicOnly reflects whether we want only public constructors or all of them
     * @return the list of constructors
     */
    private static native <T> Constructor<T>[] getDeclaredConstructors(Class<T> clazz, boolean publicOnly);

    /*
     * Finds a constructor with a given signature.
     * 
     * @param list the list of constructors to search through
     * @param parameterTypes the formal parameter list
     * @return the matching constructor
     * @throws NoSuchMethodException if the constructor does not exist.
     */
    private Constructor<T> getMatchingConstructor(
            Constructor<T>[] list, Class<?>[] parameterTypes)
            throws NoSuchMethodException {
        for (int i = 0; i < list.length; i++) {
            if (compareClassLists(list[i].getParameterTypes(), parameterTypes)) {
                return list[i];
            }
        }
        
        // BEGIN android-changed
        StringBuilder sb = new StringBuilder();
        sb.append(getSimpleName());
        sb.append('(');
        boolean first = true;
        for (Class<?> p : parameterTypes) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(p.getSimpleName());
        }
        sb.append(')');
        throw new NoSuchMethodException(sb.toString());
        // END android-changed
    }
    
    /**
     * Returns a {@code Field} object for the field with the specified name
     * which is declared in the class represented by this {@code Class}.
     * 
     * @param name
     *            the name of the requested field.
     * @return the requested field in the class represented by this class.
     * @throws NoSuchFieldException
     *             if the requested field can not be found.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getField(String)
     * @since Android 1.0
     */
    public Field getDeclaredField(String name)
            throws NoSuchFieldException, SecurityException {
        checkDeclaredMemberAccess();

        Field[] fields = getClassCache().getDeclaredFields();
        Field field = findFieldByName(fields, name);

        /*
         * Make a copy of the private (to the package) object, so that
         * setAccessible() won't alter the private instance.
         */
        return REFLECT.clone(field);
    }

    /**
     * Returns an array containing {@code Field} objects for all fields declared
     * in the class represented by this {@code Class}. If there are no fields or
     * if this {@code Class} represents an array class, a primitive type or void
     * then an empty array is returned.
     * 
     * @return an array with the fields declared in the class represented by
     *         this class.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getFields()
     * @since Android 1.0
     */
    public Field[] getDeclaredFields() throws SecurityException {
        checkDeclaredMemberAccess();

        // Return a copy of the private (to the package) array.
        Field[] fields = getClassCache().getDeclaredFields();
        return ClassCache.deepCopy(fields);
    }
    
    /*
     * Returns the list of fields without performing any security checks
     * first. If no fields exist at all, an empty array is returned.
     * 
     * @param clazz the class of interest
     * @param publicOnly reflects whether we want only public fields or all of them
     * @return the list of fields 
     */
    static native Field[] getDeclaredFields(Class<?> clazz, boolean publicOnly);

    /**
     * Returns a {@code Method} object which represents the method matching the
     * specified name and parameter types that is declared by the class
     * represented by this {@code Class}.
     * 
     * @param name
     *            the requested method's name.
     * @param parameterTypes
     *            the parameter types of the requested method.
     * @return the method described by {@code name} and {@code parameterTypes}.
     * @throws NoSuchMethodException
     *             if the requested constructor can not be found.
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getMethod(String, Class...)
     * @since Android 1.0
     */
    public Method getDeclaredMethod(String name, Class... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        checkDeclaredMemberAccess();

        Method[] methods = getClassCache().getDeclaredMethods();
        Method method = findMethodByName(methods, name, parameterTypes);

        /*
         * Make a copy of the private (to the package) object, so that
         * setAccessible() won't alter the private instance.
         */
        return REFLECT.clone(method);
    }
    
    /**
     * Returns an array containing {@code Method} objects for all methods
     * declared in the class represented by this {@code Class}. If there are no
     * methods or if this {@code Class} represents an array class, a primitive
     * type or void then an empty array is returned.
     * 
     * @return an array with the methods declared in the class represented by
     *         this {@code Class}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getMethods()
     * @since Android 1.0
     */
    public Method[] getDeclaredMethods() throws SecurityException {
        checkDeclaredMemberAccess();

        // Return a copy of the private (to the package) array.
        Method[] methods = getClassCache().getDeclaredMethods();
        return ClassCache.deepCopy(methods);
    }
    
    /**
     * Returns the list of methods without performing any security checks
     * first. If no methods exist, an empty array is returned.
     */
    static native Method[] getDeclaredMethods(Class<?> clazz, boolean publicOnly);

    /**
     * Gets the {@link ClassCache} for this instance.
     *
     * @return non-null; the cache object
     */
    /*package*/ ClassCache<T> getClassCache() {
        /*
         * Note: It is innocuous if two threads try to simultaneously
         * create the cache, so we don't bother protecting against that.
         */
        ClassCache<T> cache = null;

        if (cacheRef != null) {
            cache = cacheRef.get();
        }

        if (cache == null) {
            cache = new ClassCache<T>(this);
            cacheRef = new SoftReference<ClassCache<T>>(cache);
        }

        return cache;
    }

    /**
     * Returns the declaring {@code Class} of this {@code Class}. Returns
     * {@code null} if the class is not a member of another class or if this
     * {@code Class} represents an array class, a primitive type or void.
     * 
     * @return the declaring {@code Class} or {@code null}.
     * @since Android 1.0
     */
    native public Class<?> getDeclaringClass();

    /**
     * Returns the enclosing {@code Class} of this {@code Class}. If there is no
     * enclosing class the method returns {@code null}.
     * 
     * @return the enclosing {@code Class} or {@code null}.
     * @since Android 1.0
     */
    native public Class<?> getEnclosingClass();

    /**
     * Gets the enclosing {@code Constructor} of this {@code Class}, if it is an
     * anonymous or local/automatic class; otherwise {@code null}.
     * 
     * @return the enclosing {@code Constructor} instance or {@code null}.
     * @since Android 1.0
     */
    native public Constructor<?> getEnclosingConstructor();

    /**
     * Gets the enclosing {@code Method} of this {@code Class}, if it is an
     * anonymous or local/automatic class; otherwise {@code null}.
     * 
     * @return the enclosing {@code Method} instance or {@code null}.
     * @since Android 1.0
     */
    native public Method getEnclosingMethod();

    /**
     * Gets the {@code enum} constants associated with this {@code Class}.
     * Returns {@code null} if this {@code Class} does not represent an {@code
     * enum} type.
     * 
     * @return an array with the {@code enum} constants or {@code null}.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        if (isEnum()) {
            checkPublicMemberAccess();
            T[] values = getClassCache().getEnumValuesInOrder();

            // Copy the private (to the package) array.
            return (T[]) values.clone();
        }
        
        return null;
    }
        
    /**
     * Returns a {@code Field} object which represents the public field with the
     * specified name. This method first searches the class C represented by
     * this {@code Class}, then the interfaces implemented by C and finally the
     * superclasses of C.
     * 
     * @param name
     *            the name of the requested field.
     * @return the public field specified by {@code name}.
     * @throws NoSuchFieldException
     *             if the field can not be found.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredField(String)
     * @since Android 1.0
     */
    public Field getField(String name) throws NoSuchFieldException, SecurityException {
        checkPublicMemberAccess();

        Field[] fields = getClassCache().getAllPublicFields();
        Field field = findFieldByName(fields, name);

        /*
         * Make a copy of the private (to the package) object, so that
         * setAccessible() won't alter the private instance.
         */
        return REFLECT.clone(field);
    }

    /**
     * Returns an array containing {@code Field} objects for all public fields
     * for the class C represented by this {@code Class}. Fields may be declared
     * in C, the interfaces it implements or in the superclasses of C. The
     * elements in the returned array are in no particular order.
     * <p>
     * If there are no public fields or if this class represents an array class,
     * a primitive type or {@code void} then an empty array is returned.
     * </p>
     * 
     * @return an array with the public fields of the class represented by this
     *         {@code Class}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredFields()
     * @since Android 1.0
     */
    public Field[] getFields() throws SecurityException {
        checkPublicMemberAccess();

        // Return a copy of the private (to the package) array.
        Field[] fields = getClassCache().getAllPublicFields();
        return ClassCache.deepCopy(fields);
    }

    /**
     * Gets the {@link Type}s of the interfaces that this {@code Class} directly
     * implements. If the {@code Class} represents a primitive type or {@code
     * void} then an empty array is returned.
     * 
     * @return an array of {@link Type} instances directly implemented by the
     *         class represented by this {@code class}.
     * @since Android 1.0
     */
    public Type[] getGenericInterfaces() {
        GenericSignatureParser parser = new GenericSignatureParser(
                VMStack.getCallingClassLoader2());
        parser.parseForClass(this, getSignatureAttribute());
        return Types.getClonedTypeArray(parser.interfaceTypes);
    }
    
    /**
     * Gets the {@code Type} that represents the superclass of this {@code
     * class}.
     * 
     * @return an instance of {@code Type} representing the superclass.
     * @since Android 1.0
     */
    public Type getGenericSuperclass() {
        GenericSignatureParser parser = new GenericSignatureParser(
                VMStack.getCallingClassLoader2());
        parser.parseForClass(this, getSignatureAttribute());
        return Types.getType(parser.superclassType);
    }

    /**
     * Returns an array of {@code Class} objects that match the interfaces
     * specified in the {@code implements} declaration of the class represented
     * by this {@code Class}. The order of the elements in the array is
     * identical to the order in the original class declaration. If the class
     * does not implement any interfaces, an empty array is returned.
     * 
     * @return an array with the interfaces of the class represented by this
     *         class.
     * @since Android 1.0
     */
    public native Class[] getInterfaces();
    // BEGIN android-note
    // trying to get closer to the RI which returns a raw class array.
    // copied from newer version of harmony
    // END android-note

    // Changed to raw type to be closer to the RI
    /**
     * Returns a {@code Method} object which represents the public method with
     * the specified name and parameter types. This method first searches the
     * class C represented by this {@code Class}, then the superclasses of C and
     * finally the interfaces implemented by C and finally the superclasses of C
     * for a method with matching name.
     * 
     * @param name
     *            the requested method's name.
     * @param parameterTypes
     *            the parameter types of the requested method.
     * @return the public field specified by {@code name}.
     * @throws NoSuchMethodException
     *             if the method can not be found.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredMethod(String, Class...)
     * @since Android 1.0
     */
    public Method getMethod(String name, Class... parameterTypes) throws NoSuchMethodException, 
            SecurityException {
        checkPublicMemberAccess();

        Method[] methods = getClassCache().getAllPublicMethods();
        Method method = findMethodByName(methods, name, parameterTypes);

        /*
         * Make a copy of the private (to the package) object, so that
         * setAccessible() won't alter the private instance.
         */
        return REFLECT.clone(method);
    }

    /**
     * Returns an array containing {@code Method} objects for all public methods
     * for the class C represented by this {@code Class}. Methods may be
     * declared in C, the interfaces it implements or in the superclasses of C.
     * The elements in the returned array are in no particular order.
     * <p>
     * If there are no public methods or if this {@code Class} represents a
     * primitive type or {@code void} then an empty array is returned.
     * </p>
     * 
     * @return an array with the methods of the class represented by this
     *         {@code Class}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     * @see #getDeclaredMethods()
     * @since Android 1.0
     */
    public Method[] getMethods() throws SecurityException {
        checkPublicMemberAccess();

        // Return a copy of the private (to the package) array.
        Method[] methods = getClassCache().getAllPublicMethods();
        return ClassCache.deepCopy(methods);
    }
    
    /**
     * Performs the security checks regarding the access of a public
     * member of this {@code Class}.
     * 
     * <p><b>Note:</b> Because of the {@code getCallingClassLoader2()}
     * check, this method must be called exactly one level deep into a
     * public method on this instance.</p>
     */
    /*package*/ void checkPublicMemberAccess() {
        SecurityManager smgr = System.getSecurityManager();

        if (smgr != null) {
            smgr.checkMemberAccess(this, Member.PUBLIC);

            ClassLoader calling = VMStack.getCallingClassLoader2();
            ClassLoader current = getClassLoader();
            
            if (calling != null && !calling.isAncestorOf(current)) {
                smgr.checkPackageAccess(this.getPackage().getName());
            }
        }
    }

    /**
     * Performs the security checks regarding the access of a declared
     * member of this {@code Class}.
     * 
     * <p><b>Note:</b> Because of the {@code getCallingClassLoader2()}
     * check, this method must be called exactly one level deep into a
     * public method on this instance.</p>
     */
    private void checkDeclaredMemberAccess() {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            smgr.checkMemberAccess(this, Member.DECLARED);
            
            ClassLoader calling = VMStack.getCallingClassLoader2();
            ClassLoader current = getClassLoader();

            if (calling != null && !calling.isAncestorOf(current)) {
                smgr.checkPackageAccess(this.getPackage().getName());
            }
        }
    }
    
    /**
     * Returns an integer that represents the modifiers of the class represented
     * by this {@code Class}. The returned value is a combination of bits
     * defined by constants in the {@link Modifier} class.
     * 
     * @return the modifiers of the class represented by this {@code Class}.
     * @since Android 1.0
     */
    public int getModifiers() {
        return getModifiers(this, false);
    }

    /*
     * Return the modifiers for the given class.
     * 
     * @param clazz the class of interest
     * @ignoreInnerClassesAttrib determines whether we look for and use the
     *     flags from an "inner class" attribute
     */
    private static native int getModifiers(Class<?> clazz, boolean ignoreInnerClassesAttrib);
    
    /**
     * Returns the name of the class represented by this {@code Class}. For a
     * description of the format which is used, see the class definition of
     * {@link Class}.
     * 
     * @return the name of the class represented by this {@code Class}.
     * @since Android 1.0
     */
    public native String getName();

    /**
     * Returns the simple name of the class represented by this {@code Class} as
     * defined in the source code. If there is no name (that is, the class is
     * anonymous) then an empty string is returned. If the receiver is an array
     * then the name of the underlying type with square braces appended (for
     * example {@code &quot;Integer[]&quot;}) is returned.
     * 
     * @return the simple name of the class represented by this {@code Class}.
     * @since Android 1.0
     */
    public String getSimpleName() {
        if (isArray()) {
            return getComponentType().getSimpleName() + "[]";
        }
        
        String name = getName();
        
        if (isAnonymousClass()) {
            return "";
        }

        if (isMemberClass() || isLocalClass()) {
            return getInnerClassName();
        }
        
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            return name.substring(dot + 1);
        }
        
        return name;
    }

    /*
     * Returns the simple name of a member or local class, or null otherwise. 
     * 
     * @return The name.
     */
    private native String getInnerClassName();
    
    /**
     * Returns the {@code ProtectionDomain} of the class represented by this
     * class.
     * <p>
     * Note: In order to conserve space in an embedded target like Android, we
     * allow this method to return {@code null} for classes in the system
     * protection domain (that is, for system classes). System classes are
     * always given full permissions (that is, AllPermission). This can not be
     * changed through the {@link java.security.Policy} class.
     * </p>
     * 
     * @return the {@code ProtectionDomain} of the class represented by this
     *         class.
     * @throws SecurityException
     *             if a security manager exists and it does not allow member
     *             access.
     */
    public ProtectionDomain getProtectionDomain() {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            // Security check is independent of calling class loader.
            smgr.checkPermission(new RuntimePermission("getProtectionDomain"));
        }
        
        return pd;
    }

    /**
     * Returns the URL of the resource specified by {@code resName}. The mapping
     * between the resource name and the URL is managed by the class' class
     * loader.
     * 
     * @param resName
     *            the name of the resource.
     * @return the requested resource's {@code URL} object or {@code null} if
     *         the resource can not be found.
     * @see ClassLoader
     * @since Android 1.0
     */
    public URL getResource(String resName) {
        // Get absolute resource name, but without the leading slash
        if (resName.startsWith("/")) {
            resName = resName.substring(1);
        } else {
            String pkg = getName();
            int dot = pkg.lastIndexOf('.');
            if (dot != -1) {
                pkg = pkg.substring(0, dot).replace('.', '/');
            } else {
                pkg = "";
            }
            
            resName = pkg + "/" + resName;
        }
        
        // Delegate to proper class loader
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            return loader.getResource(resName);
        } else {
            return ClassLoader.getSystemResource(resName);
        }
    }

    /**
     * Returns a read-only stream for the contents of the resource specified by
     * {@code resName}. The mapping between the resource name and the stream is
     * managed by the class' class loader.
     * 
     * @param resName
     *            the name of the resource.
     * @return a stream for the requested resource or {@code null} if no
     *         resource with the specified name can be found.
     * @see ClassLoader
     * @since Android 1.0
     */    
    public InputStream getResourceAsStream(String resName) {
        // Get absolute resource name, but without the leading slash
        if (resName.startsWith("/")) {
            resName = resName.substring(1);
        } else {
            String pkg = getName();
            int dot = pkg.lastIndexOf('.');
            if (dot != -1) {
                pkg = pkg.substring(0, dot).replace('.', '/');
            } else {
                pkg = "";
            }
            
            resName = pkg + "/" + resName;
        }
        
        // Delegate to proper class loader
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(resName);
        } else {
            return ClassLoader.getSystemResourceAsStream(resName);
        }
    }

    /**
     * Returns the signers for the class represented by this {@code Class} or
     * {@code null} if either there are no signers or this {@code Class}
     * represents a primitive type or void.
     * 
     * @return the signers of the class represented by this {@code Class}.
     * @since Android 1.0
     */
    public Object[] getSigners() {
        // TODO Delegate this to class loader somehow? What are these signers?
        return null;
    }

    /**
     * Returns the {@code Class} object which represents the superclass of the
     * class represented by this {@code Class}. If this {@code Class} represents
     * the {@code Object} class, a primitive type, an interface or void then the
     * method returns {@code null}. If this {@code Class} represents an array
     * class then the {@code Object} class is returned.
     * 
     * @return the superclass of the class represented by this {@code Class}.
     * @since Android 1.0
     */
    public native Class<? super T> getSuperclass();

    /**
     * Returns an array containing {@code TypeVariable} objects for type
     * variables declared by the generic class represented by this {@code
     * Class}. Returns an empty array if the class is not generic.
     * 
     * @return an array with the type variables of the class represented by this
     *         class.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public synchronized TypeVariable<Class<T>>[] getTypeParameters() {
        GenericSignatureParser parser = new GenericSignatureParser(
                VMStack.getCallingClassLoader2());
        parser.parseForClass(this, getSignatureAttribute());
        return parser.formalTypeParameters.clone();
    }

    /**
     * Indicates whether this {@code Class} represents an annotation class.
     * 
     * @return {@code true} if this {@code Class} represents an annotation
     *         class; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isAnnotation() {
        final int ACC_ANNOTATION = 0x2000;  // not public in reflect.Modifiers
        int mod = getModifiers(this, true);
        return (mod & ACC_ANNOTATION) != 0;
    }

    /**
     * Indicates whether the specified annotation is present for the class
     * represented by this {@code Class}.
     * 
     * @param annotationClass
     *            the annotation to look for.
     * @return {@code true} if the class represented by this {@code Class} is
     *         annotated with {@code annotationClass}; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * Indicates whether the class represented by this {@code Class} is
     * anonymously declared.
     * 
     * @return {@code true} if the class represented by this {@code Class} is
     *         anonymous; {@code false} otherwise.
     * @since Android 1.0
     */
    native public boolean isAnonymousClass();

    /**
     * Indicates whether the class represented by this {@code Class} is an array
     * class.
     * 
     * @return {@code true} if the class represented by this {@code Class} is an
     *         array class; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isArray() {
        return getComponentType() != null;
    }

    /**
     * Indicates whether the specified class type can be converted to the class
     * represented by this {@code Class}. Conversion may be done via an identity
     * conversion or a widening reference conversion (if either the receiver or
     * the argument represent primitive types, only the identity conversion
     * applies).
     * 
     * @param cls
     *            the class to check.
     * @return {@code true} if {@code cls} can be converted to the class
     *         represented by this {@code Class}; {@code false} otherwise.
     * @throws NullPointerException
     *             if {@code cls} is {@code null}.
     * @since Android 1.0
     */
    public native boolean isAssignableFrom(Class<?> cls);

    /**
     * Indicates whether the class represented by this {@code Class} is an
     * {@code enum}.
     * 
     * @return {@code true} if the class represented by this {@code Class} is an
     *         {@code enum}; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isEnum() {
        return ((getModifiers() & 0x4000) != 0) && (getSuperclass() == Enum.class);
    }

    /**
     * Indicates whether the specified object can be cast to the class
     * represented by this {@code Class}. This is the runtime version of the
     * {@code instanceof} operator.
     * 
     * @param object
     *            the object to check.
     * @return {@code true} if {@code object} can be cast to the type
     *         represented by this {@code Class}; {@code false} if {@code
     *         object} is {@code null} or cannot be cast.
     * @since Android 1.0
     */
    public native boolean isInstance(Object object);

    /**
     * Indicates whether this {@code Class} represents an interface.
     * 
     * @return {@code true} if this {@code Class} represents an interface;
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public native boolean isInterface();

    /**
     * Indicates whether the class represented by this {@code Class} is defined
     * locally.
     * 
     * @return {@code true} if the class represented by this {@code Class} is
     *         defined locally; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isLocalClass() {
        boolean enclosed = (getEnclosingMethod() != null ||
                         getEnclosingConstructor() != null);
        return enclosed && !isAnonymousClass();
    }

    /**
     * Indicates whether the class represented by this {@code Class} is a member
     * class.
     * 
     * @return {@code true} if the class represented by this {@code Class} is a
     *         member class; {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isMemberClass() {
        return getDeclaringClass() != null;
    }

    /**
     * Indicates whether this {@code Class} represents a primitive type.
     * 
     * @return {@code true} if this {@code Class} represents a primitive type;
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public native boolean isPrimitive();

    /**
     * Indicates whether this {@code Class} represents a synthetic type.
     * 
     * @return {@code true} if this {@code Class} represents a synthetic type;
     *         {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean isSynthetic() {
        final int ACC_SYNTHETIC = 0x1000;   // not public in reflect.Modifiers
        int mod = getModifiers(this, true);
        return (mod & ACC_SYNTHETIC) != 0;
    }

    /**
     * Returns a new instance of the class represented by this {@code Class},
     * created by invoking the default (that is, zero-argument) constructor. If
     * there is no such constructor, or if the creation fails (either because of
     * a lack of available memory or because an exception is thrown by the
     * constructor), an {@code InstantiationException} is thrown. If the default
     * constructor exists but is not accessible from the context where this
     * method is invoked, an {@code IllegalAccessException} is thrown.
     * 
     * @return a new instance of the class represented by this {@code Class}.
     * @throws IllegalAccessException
     *             if the default constructor is not visible.
     * @throws InstantiationException
     *             if the instance can not be created.
     * @throws SecurityException
     *             if a security manager exists and it does not allow creating
     *             new instances.
     * @since Android 1.0
     */
    public T newInstance() throws IllegalAccessException,
            InstantiationException {
        checkPublicMemberAccess();        
        return newInstanceImpl();
    }

    private native T newInstanceImpl() throws IllegalAccessException,
            InstantiationException;

    @Override
    public String toString() {
        if (isPrimitive()) {
            return getSimpleName().toLowerCase();
        } else {
            return (isInterface() ? "interface " : "class ") + getName();
        }
    }

    /**
     * Returns the {@code Package} of which the class represented by this
     * {@code Class} is a member. Returns {@code null} if no {@code Package}
     * object was created by the class loader of the class.
     * 
     * @return Package the {@code Package} of which this {@code Class} is a
     *         member or {@code null}.
     * @since Android 1.0
     */
    public Package getPackage() {
        // TODO This might be a hack, but the VM doesn't have the necessary info.
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            String name = getName();
            int dot = name.lastIndexOf('.');
            return (dot != -1 ? ClassLoader.getPackage(loader, name.substring(0, dot)) : null);
        }
        
        return null;
    }

    /**
     * Returns the assertion status for the class represented by this {@code
     * Class}. Assertion is enabled / disabled based on the class loader,
     * package or class default at runtime.
     * 
     * @return the assertion status for the class represented by this {@code
     *         Class}.
     * @since Android 1.0
     */
    public native boolean desiredAssertionStatus();

    /**
     * Casts this {@code Class} to represent a subclass of the specified class.
     * If successful, this {@code Class} is returned; otherwise a {@code
     * ClassCastException} is thrown.
     * 
     * @param clazz
     *            the required type.
     * @return this {@code Class} cast as a subclass of the given type.
     * @throws ClassCastException
     *             if this {@code Class} cannot be cast to the specified type.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        if (clazz.isAssignableFrom(this)) {
            return (Class<? extends U>)this;
        }
        throw new ClassCastException();
    }

    /**
     * Casts the specified object to the type represented by this {@code Class}.
     * If the object is {@code null} then the result is also {@code null}.
     * 
     * @param obj
     *            the object to cast.
     * @return the object that has been cast.
     * @throws ClassCastException
     *             if the object cannot be cast to the specified type.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public T cast(Object obj) {
        if (obj == null) {
            return null;
        } else if (this.isInstance(obj)) {
            return (T)obj;            
        }
        throw new ClassCastException();
    }

    /**
     * Set the "accessible" flag of the given object, without doing any
     * access checks.
     * 
     * <p><b>Note:</b> This method is implemented in native code, and,
     * as such, is less efficient than using {@link ClassCache#REFLECT}
     * to achieve the same goal. This method exists solely to help
     * bootstrap the reflection bridge.</p>
     * 
     * @param ao non-null; the object to modify
     * @param flag the new value for the accessible flag
     */
    /*package*/ static native void setAccessibleNoCheck(AccessibleObject ao,
            boolean flag);
    
    /**
     * Copies two arrays into one. Assumes that the destination array is large
     * enough. 
     * 
     * @param result the destination array
     * @param head the first source array
     * @param tail the second source array
     * @return the destination array, that is, result
     */
    private static <T extends Object> T[] arraycopy(T[] result, T[] head, T[] tail) {
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(tail, 0, result, head.length, tail.length);
        return result;
    }
    
    /**
     * This must be provided by the vm vendor, as it is used by other provided
     * class implementations in this package. This method is used by
     * SecurityManager.classDepth(), and getClassContext() which use the
     * parameters (-1, false) and SecurityManager.classLoaderDepth(),
     * currentClassLoader(), and currentLoadedClass() which use the parameters
     * (-1, true). Walk the stack and answer an array containing the maxDepth
     * most recent classes on the stack of the calling thread. Starting with the
     * caller of the caller of getStackClasses(), return an array of not more
     * than maxDepth Classes representing the classes of running methods on the
     * stack (including native methods). Frames representing the VM
     * implementation of java.lang.reflect are not included in the list. If
     * stopAtPrivileged is true, the walk will terminate at any frame running
     * one of the following methods: <code><ul>
     * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;)Ljava/lang/Object;</li>
     * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;</li>
     * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
     * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
     * </ul></code> If one of the doPrivileged methods is found, the walk terminate
     * and that frame is NOT included in the returned array. Notes:
     * <ul>
     * <li>This method operates on the defining classes of methods on stack.
     * NOT the classes of receivers.</li>
     * <li>The item at index zero in the result array describes the caller of
     * the caller of this method.</li>
     * </ul>
     * 
     * @param maxDepth
     *            maximum depth to walk the stack, -1 for the entire stack
     * @param stopAtPrivileged
     *            stop at privileged classes
     * @return the array of the most recent classes on the stack
     */
    static final Class<?>[] getStackClasses(int maxDepth, boolean stopAtPrivileged) {
        return VMStack.getClasses(maxDepth, stopAtPrivileged);
    }
    
}
