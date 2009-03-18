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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

import dalvik.system.PathClassLoader;
import dalvik.system.VMStack;

/**
 * Loads classes and resources from a repository. One or more class loaders are
 * installed at runtime. These are consulted whenever the runtime system needs a
 * specific class that is not yet available in-memory. Typically, class loaders
 * are grouped into a tree where child class loaders delegate all requests to
 * parent class loaders. Only if the parent class loader cannot satisfy the
 * request, the child class loader itself tries to handle it.
 * <p>
 * {@code ClassLoader} is an abstract class that implements the common
 * infrastructure required by all class loaders. Android provides several
 * concrete implementations of the class, with
 * {@link dalvik.system.PathClassLoader} being the one typically used. Other
 * applications may implement subclasses of {@code ClassLoader} to provide
 * special ways for loading classes.
 * </p>
 * 
 * @since Android 1.0
 * @see Class
 */
public abstract class ClassLoader {

    // BEGIN android-note
    /*
     * Because of a potential class initialization race between ClassLoader and
     * java.lang.System, reproducible when using JDWP with "suspend=y", we defer
     * creation of the system class loader until first use. We use a static
     * inner class to get synchronization at init time without having to sync on
     * every access.
     */
    // END android-note
    /**
     * The 'System' ClassLoader - the one that is responsible for loading
     * classes from the classpath. It is not equal to the bootstrap class loader -
     * that one handles the built-in classes.
     * 
     * @see #getSystemClassLoader()
     */
    static private class SystemClassLoader {
        public static ClassLoader loader = ClassLoader.createSystemClassLoader();
    }

    /**
     * The parent ClassLoader.
     */
    private ClassLoader parent;

    /**
     * The packages known to the class loader.
     */
    private Map<String, Package> packages = new HashMap<String, Package>();

    /**
     * Create the system class loader. Note this is NOT the bootstrap class
     * loader (which is managed by the VM). We use a null value for the parent
     * to indicate that the bootstrap loader is our parent.
     */
    private static ClassLoader createSystemClassLoader() {
        String classPath = System.getProperty("java.class.path", ".");

        // String[] paths = classPath.split(":");
        // URL[] urls = new URL[paths.length];
        // for (int i = 0; i < paths.length; i++) {
        // try {
        // urls[i] = new URL("file://" + paths[i]);
        // }
        // catch (Exception ex) {
        // ex.printStackTrace();
        // }
        // }
        //            
        // return new java.net.URLClassLoader(urls, null);

        // TODO Make this a java.net.URLClassLoader once we have those?
        return new PathClassLoader(classPath, BootClassLoader.getInstance());
    }

    /**
     * Returns the system class loader. This is the parent for new
     * {@code ClassLoader} instances and is typically the class loader used to
     * start the application. If a security manager is present and the caller's
     * class loader is neither {@code null} nor the same as or an ancestor of
     * the system class loader, then this method calls the security manager's
     * checkPermission method with a RuntimePermission("getClassLoader")
     * permission to ensure that it is ok to access the system class loader. If
     * not, a {@code SecurityException} is thrown.
     * 
     * @return the system class loader.
     * @throws SecurityException
     *             if a security manager exists and it does not allow access to
     *             the system class loader.
     * @since Android 1.0
     */
    public static ClassLoader getSystemClassLoader() {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            ClassLoader caller = VMStack.getCallingClassLoader();
            if (caller != null && !caller.isAncestorOf(SystemClassLoader.loader)) {
                smgr.checkPermission(new RuntimePermission("getClassLoader"));
            }
        }

        return SystemClassLoader.loader;
    }

    /**
     * Finds the URL of the resource with the specified name. The system class
     * loader's resource lookup algorithm is used to find the resource.
     * 
     * @return the {@code URL} object for the requested resource or {@code null}
     *         if the resource can not be found.
     * @param resName
     *            the name of the resource to find.
     * @see Class#getResource
     * @since Android 1.0
     */
    public static URL getSystemResource(String resName) {
        return SystemClassLoader.loader.getResource(resName);
    }

    /**
     * Returns an enumeration of URLs for the resource with the specified name.
     * The system class loader's resource lookup algorithm is used to find the
     * resource.
     * 
     * @return an enumeration of {@code URL} objects containing the requested
     *         resources.
     * @param resName
     *            the name of the resource to find.
     * @throws IOException
     *             if an I/O error occurs.
     * @since Android 1.0
     */
    public static Enumeration<URL> getSystemResources(String resName) throws IOException {
        return SystemClassLoader.loader.getResources(resName);
    }

    /**
     * Returns a stream for the resource with the specified name. The system
     * class loader's resource lookup algorithm is used to find the resource.
     * Basically, the contents of the java.class.path are searched in order,
     * looking for a path which matches the specified resource.
     * 
     * @return a stream for the resource or {@code null}.
     * @param resName
     *            the name of the resource to find.
     * @see Class#getResourceAsStream
     * @since Android 1.0
     */
    public static InputStream getSystemResourceAsStream(String resName) {
        return SystemClassLoader.loader.getResourceAsStream(resName);
    }

    /**
     * Constructs a new instance of this class with the system class loader as
     * its parent.
     * 
     * @throws SecurityException
     *             if a security manager exists and it does not allow the
     *             creation of a new {@code ClassLoader}.
     * @since Android 1.0
     */
    protected ClassLoader() {
        this(getSystemClassLoader(), false);
    }

    /**
     * Constructs a new instance of this class with the specified class loader
     * as its parent.
     * 
     * @param parentLoader
     *            The {@code ClassLoader} to use as the new class loader's
     *            parent.
     * @throws SecurityException
     *             if a security manager exists and it does not allow the
     *             creation of new a new {@code ClassLoader}.
     * @since Android 1.0
     */
    protected ClassLoader(ClassLoader parentLoader) {
        this(parentLoader, false);
    }

    /*
     * constructor for the BootClassLoader which needs parent to be null.
     */
    ClassLoader(ClassLoader parentLoader, boolean nullAllowed) {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            smgr.checkCreateClassLoader();
        }
        
        if (parentLoader == null && !nullAllowed) {
            throw new NullPointerException(
                    "Parent ClassLoader may not be null");
        }

        parent = parentLoader;
    }

    /**
     * Constructs a new class from an array of bytes containing a class
     * definition in class file format.
     * 
     * @param classRep
     *            the memory image of a class file.
     * @param offset
     *            the offset into {@code classRep}.
     * @param length
     *            the length of the class file.
     * @return the {@code Class} object created from the specified subset of
     *         data in {@code classRep}.
     * @throws ClassFormatError
     *             if {@code classRep} does not contain a valid class.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0}, {@code length < 0} or if
     *             {@code offset + length} is greater than the length of
     *             {@code classRep}.
     * @deprecated Use {@link #defineClass(String, byte[], int, int)}
     * @since Android 1.0
     */
    @Deprecated
    protected final Class<?> defineClass(byte[] classRep, int offset, int length)
            throws ClassFormatError {

        return VMClassLoader.defineClass(this, classRep, offset, length, null);
    }

    /**
     * Constructs a new class from an array of bytes containing a class
     * definition in class file format.
     * 
     * @param className
     *            the expected name of the new class, may be {@code null} if not
     *            known.
     * @param classRep
     *            the memory image of a class file.
     * @param offset
     *            the offset into {@code classRep}.
     * @param length
     *            the length of the class file.
     * @return the {@code Class} object created from the specified subset of
     *         data in {@code classRep}.
     * @throws ClassFormatError
     *             if {@code classRep} does not contain a valid class.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0}, {@code length < 0} or if
     *             {@code offset + length} is greater than the length of
     *             {@code classRep}.
     * @since Android 1.0
     */
    protected final Class<?> defineClass(String className, byte[] classRep, int offset, int length)
            throws ClassFormatError {

        // TODO Define a default ProtectionDomain on first use
        return defineClass(className, classRep, offset, length, null);
    }

    /**
     * Constructs a new class from an array of bytes containing a class
     * definition in class file format and assigns the specified protection
     * domain to the new class. If the provided protection domain is
     * {@code null} then a default protection domain is assigned to the class.
     * 
     * @param className
     *            the expected name of the new class, may be {@code null} if not
     *            known.
     * @param classRep
     *            the memory image of a class file.
     * @param offset
     *            the offset into {@code classRep}.
     * @param length
     *            the length of the class file.
     * @param protectionDomain
     *            the protection domain to assign to the loaded class, may be
     *            {@code null}.
     * @return the {@code Class} object created from the specified subset of
     *         data in {@code classRep}.
     * @throws ClassFormatError
     *             if {@code classRep} does not contain a valid class.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0}, {@code length < 0} or if
     *             {@code offset + length} is greater than the length of
     *             {@code classRep}.
     * @throws NoClassDefFoundError
     *             if {@code className} is not equal to the name of the class
     *             contained in {@code classRep}.
     * @since Android 1.0
     */
    protected final Class<?> defineClass(String className, byte[] classRep, int offset, int length,
            ProtectionDomain protectionDomain) throws java.lang.ClassFormatError {

        return VMClassLoader.defineClass(this, className, classRep, offset, length,
                protectionDomain);
    }

    /**
     * Defines a new class with the specified name, byte code from the byte
     * buffer and the optional protection domain. If the provided protection
     * domain is {@code null} then a default protection domain is assigned to
     * the class.
     * 
     * @param name
     *            the expected name of the new class, may be {@code null} if not
     *            known.
     * @param b
     *            the byte buffer containing the byte code of the new class.
     * @param protectionDomain
     *            the protection domain to assign to the loaded class, may be
     *            {@code null}.
     * @return the {@code Class} object created from the data in {@code b}.
     * @throws ClassFormatError
     *             if {@code b} does not contain a valid class.
     * @throws NoClassDefFoundError
     *             if {@code className} is not equal to the name of the class
     *             contained in {@code b}.
     * @since Android 1.0
     */
    protected final Class<?> defineClass(String name, ByteBuffer b,
            ProtectionDomain protectionDomain) throws ClassFormatError {

        byte[] temp = new byte[b.remaining()];
        b.get(temp);
        return defineClass(name, temp, 0, temp.length, protectionDomain);
    }

    /**
     * Overridden by subclasses, throws a {@code ClassNotFoundException} by
     * default. This method is called by {@code loadClass} after the parent
     * {@code ClassLoader} has failed to find a loaded class of the same name.
     * 
     * @param className
     *            the name of the class to look for.
     * @return the {@code Class} object that is found.
     * @throws ClassNotFoundException
     *             if the class cannot be found.
     * @since Android 1.0
     */
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        throw new ClassNotFoundException(className);
    }

    /**
     * Returns the class with the specified name if it has already been loaded
     * by the virtual machine or {@code null} if it has not yet been loaded.
     * 
     * @param className
     *            the name of the class to look for.
     * @return the {@code Class} object or {@code null} if the requested class
     *         has not been loaded.
     * @since Android 1.0
     */
    protected final Class<?> findLoadedClass(String className) {
        ClassLoader loader;
        if (this == BootClassLoader.getInstance())
            loader = null;
        else
            loader = this;
        return VMClassLoader.findLoadedClass(loader, className);
    }

    /**
     * Finds the class with the specified name, loading it using the system
     * class loader if necessary.
     * 
     * @param className
     *            the name of the class to look for.
     * @return the {@code Class} object with the requested {@code className}.
     * @throws ClassNotFoundException
     *             if the class can not be found.
     * @since Android 1.0
     */
    protected final Class<?> findSystemClass(String className) throws ClassNotFoundException {
        return Class.forName(className, false, getSystemClassLoader());
    }

    /**
     * Returns this class loader's parent.
     * 
     * @return this class loader's parent or {@code null}.
     * @throws SecurityException
     *             if a security manager exists and it does not allow to
     *             retrieve the parent class loader.
     * @since Android 1.0
     */
    public final ClassLoader getParent() {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null) {
            smgr.checkPermission(new RuntimePermission("getClassLoader"));
        }

        return parent;
    }

    /**
     * Returns the URL of the resource with the specified name. This
     * implementation first tries to use the parent class loader to find the
     * resource; if this fails then {@link #findResource(String)} is called to
     * find the requested resource.
     * 
     * @param resName
     *            the name of the resource to find.
     * @return the {@code URL} object for the requested resource or {@code null}
     *         if either the resource can not be found or a security manager
     *         does not allow to access the resource.
     * @see Class#getResource
     * @since Android 1.0
     */
    public URL getResource(String resName) {
        URL resource = null;

        resource = parent.getResource(resName);

        if (resource == null) {
            resource = findResource(resName);
        }

        return resource;
    }

    /**
     * Returns an enumeration of URLs for the resource with the specified name.
     * This implementation first uses this class loader's parent to find the
     * resource, then it calls {@link #findResources(String)} to get additional
     * URLs. The returned enumeration contains the {@code URL} objects of both
     * find operations.
     * 
     * @return an enumeration of {@code URL} objects for the requested resource.
     * @param resName
     *            the name of the resource to find.
     * @throws IOException
     *             if an I/O error occurs.
     * @since Android 1.0
     */
    @SuppressWarnings("unchecked")
    public Enumeration<URL> getResources(String resName) throws IOException {

        Enumeration first = parent.getResources(resName);
        Enumeration second = findResources(resName);

        return new TwoEnumerationsInOne(first, second);
    }

    /**
     * Returns a stream for the resource with the specified name. See
     * {@link #getResource(String)} for a description of the lookup algorithm
     * used to find the resource.
     * 
     * @return a stream for the resource or {@code null} if either the resource
     *         can not be found or a security manager does not allow to access
     *         the resource.
     * @param resName
     *            the name of the resource to find.
     * @see Class#getResourceAsStream
     * @since Android 1.0
     */
    public InputStream getResourceAsStream(String resName) {
        try {
            URL url = getResource(resName);
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException ex) {
            // Don't want to see the exception.
        }

        return null;
    }

    /**
     * Loads the class with the specified name. Invoking this method is
     * equivalent to calling {@code loadClass(className, false)}.
     * <p>
     * <strong>Note:</strong> In the Android reference implementation, the
     * second parameter of {@link #loadClass(String, boolean)} is ignored
     * anyway.
     * </p>
     * 
     * @return the {@code Class} object.
     * @param className
     *            the name of the class to look for.
     * @throws ClassNotFoundException
     *             if the class can not be found.
     * @since Android 1.0
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, false);
    }

    /**
     * Loads the class with the specified name, optionally linking it after
     * loading. The following steps are performed:
     * <ol>
     * <li> Call {@link #findLoadedClass(String)} to determine if the requested
     * class has already been loaded.</li>
     * <li>If the class has not yet been loaded: Invoke this method on the
     * parent class loader.</li>
     * <li>If the class has still not been loaded: Call
     * {@link #findClass(String)} to find the class.</li>
     * </ol>
     * <p>
     * <strong>Note:</strong> In the Android reference implementation, the
     * {@code resolve} parameter is ignored; classes are never linked.
     * </p>
     * 
     * @return the {@code Class} object.
     * @param className
     *            the name of the class to look for.
     * @param resolve
     *            Indicates if the class should be resolved after loading. This
     *            parameter is ignored on the Android reference implementation;
     *            classes are not resolved.
     * @throws ClassNotFoundException
     *             if the class can not be found.
     * @since Android 1.0
     */
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            try {
                clazz = parent.loadClass(className, false);
            } catch (ClassNotFoundException e) {
                // Don't want to see this.
            }

            if (clazz == null) {
                clazz = findClass(className);
            }
        }

        return clazz;
    }

    /**
     * Forces a class to be linked (initialized). If the class has already been
     * linked this operation has no effect.
     * <p>
     * <strong>Note:</strong> In the Android reference implementation, this
     * method has no effect.
     * </p>
     * 
     * @param clazz
     *            the class to link.
     * @since Android 1.0
     */
    protected final void resolveClass(Class<?> clazz) {
        // no-op, doesn't make sense on android.
    }

    /**
     * Indicates whether this class loader is the system class loader. This
     * method must be provided by the virtual machine vendor, as it is used by
     * other provided class implementations in this package. A sample
     * implementation of this method is provided by the reference
     * implementation. This method is used by
     * SecurityManager.classLoaderDepth(), currentClassLoader() and
     * currentLoadedClass(). Returns true if the receiver is a system class
     * loader.
     * <p>
     * Note that this method has package visibility only. It is defined here to
     * avoid the security manager check in getSystemClassLoader, which would be
     * required to implement this method anywhere else.
     * </p>
     * 
     * @return {@code true} if the receiver is a system class loader
     * @see Class#getClassLoaderImpl()
     */
    final boolean isSystemClassLoader() {
        return false;
    }

    /**
     * <p>
     * Returns true if the receiver is ancestor of another class loader. It also
     * returns true if the two class loader are equal.
     * </p>
     * <p>
     * Note that this method has package visibility only. It is defined here to
     * avoid the security manager check in getParent, which would be required to
     * implement this method anywhere else. The method is also required in other
     * places where class loaders are accesses.
     * </p>
     * 
     * @param child
     *            A child candidate
     * @return {@code true} if the receiver is ancestor of, or equal to,
     *         the parameter
     */
    final boolean isAncestorOf(ClassLoader child) {
        for (ClassLoader current = child; current != null;
                current = child.parent) {
            if (current == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the URL of the resource with the specified name. This
     * implementation just returns {@code null}; it should be overridden in
     * subclasses.
     * 
     * @param resName
     *            the name of the resource to find.
     * @return the {@code URL} object for the requested resource.
     * @since Android 1.0
     */
    protected URL findResource(String resName) {
        return null;
    }

    /**
     * Finds an enumeration of URLs for the resource with the specified name.
     * This implementation just returns an empty {@code Enumeration}; it should
     * be overridden in subclasses.
     * 
     * @param resName
     *            the name of the resource to find.
     * @return an enumeration of {@code URL} objects for the requested resource.
     * @throws IOException
     *             if an I/O error occurs.
     * @since Android 1.0
     */
    @SuppressWarnings( {
            "unchecked", "unused"
    })
    protected Enumeration<URL> findResources(String resName) throws IOException {
        return EmptyEnumeration.getInstance();
    }

    /**
     * Returns the absolute path of the native library with the specified name,
     * or {@code null}. If this method returns {@code null} then the virtual
     * machine searches the directories specified by the system property
     * "java.library.path".
     * <p>
     * This implementation always returns {@code null}.
     * </p>
     * 
     * @param libName
     *            the name of the library to find.
     * @return the absolute path of the library.
     * @since Android 1.0
     */
    protected String findLibrary(String libName) {
        return null;
    }

    /**
     * Returns the package with the specified name. Package information is
     * searched in this class loader.
     * 
     * @param name
     *            the name of the package to find.
     * @return the package with the requested name; {@code null} if the package
     *         can not be found.
     * @since Android 1.0
     */
    protected Package getPackage(String name) {
        synchronized (packages) {
            Package p = packages.get(name);
            return p;
        }
    }

    /**
     * Gets the package with the specified name, searching it in the specified
     * class loader.
     * 
     * @param loader
     *            the class loader to search the package in.
     * @param name
     *            the name of the package to find.
     * @return the package with the requested name; {@code null} if the package
     *         can not be found.
     * @since Android 1.0
     */
    static Package getPackage(ClassLoader loader, String name) {
        return loader.getPackage(name);
    }

    /**
     * Returns all the packages known to this class loader.
     * 
     * @return an array with all packages known to this class loader.
     * @since Android 1.0
     */
    protected Package[] getPackages() {
        synchronized (packages) {
            Collection<Package> col = packages.values();
            Package[] result = new Package[col.size()];
            col.toArray(result);
            return result;
        }
    }

    /**
     * Defines and returns a new {@code Package} using the specified
     * information. If {@code sealBase} is {@code null}, the package is left
     * unsealed. Otherwise, the package is sealed using this URL.
     * 
     * @param name
     *            the name of the package.
     * @param specTitle
     *            the title of the specification.
     * @param specVersion
     *            the version of the specification.
     * @param specVendor
     *            the vendor of the specification.
     * @param implTitle
     *            the implementation title.
     * @param implVersion
     *            the implementation version.
     * @param implVendor
     *            the specification vendor.
     * @param sealBase
     *            the URL used to seal this package or {@code null} to leave the
     *            package unsealed.
     * @return the {@code Package} object that has been created.
     * @throws IllegalArgumentException
     *             if a package with the specified name already exists.
     * @since Android 1.0
     */
    protected Package definePackage(String name, String specTitle, String specVersion,
            String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase)
            throws IllegalArgumentException {

        synchronized (packages) {
            if (packages.containsKey(name)) {
                throw new IllegalArgumentException("Package " + name + " already defined");
            }

            Package newPackage = new Package(name, specTitle, specVersion, specVendor, implTitle,
                    implVersion, implVendor, sealBase);

            packages.put(name, newPackage);

            return newPackage;
        }
    }

    /**
     * Gets the signers of the specified class. This implementation returns
     * {@code null}.
     * 
     * @param c
     *            the {@code Class} object for which to get the signers.
     * @return signers the signers of {@code c}.
     * @since Android 1.0
     */
    final Object[] getSigners(Class<?> c) {
        return null;
    }

    /**
     * Sets the signers of the specified class. This implementation does
     * nothing.
     * 
     * @param c
     *            the {@code Class} object for which to set the signers.
     * @param signers
     *            the signers for {@code c}.
     * @since Android 1.0
     */
    protected final void setSigners(Class<?> c, Object[] signers) {
        return;
    }

    /**
     * <p>
     * This must be provided by the VM vendor. It is used by
     * SecurityManager.checkMemberAccess() with depth = 3. Note that
     * checkMemberAccess() assumes the following stack when called:<br>
     * </p>
     * 
     * <pre>
     *          &lt; user code &amp;gt; &lt;- want this class
     *          Class.getDeclared*();
     *          Class.checkMemberAccess();
     *          SecurityManager.checkMemberAccess(); &lt;- current frame
     * </pre>
     * 
     * <p>
     * Returns the ClassLoader of the method (including natives) at the
     * specified depth on the stack of the calling thread. Frames representing
     * the VM implementation of java.lang.reflect are not included in the list.
     * </p>
     * Notes:
     * <ul>
     * <li>This method operates on the defining classes of methods on stack.
     * NOT the classes of receivers.</li>
     * <li>The item at depth zero is the caller of this method</li>
     * </ul>
     * 
     * @param depth
     *            the stack depth of the requested ClassLoader
     * @return the ClassLoader at the specified depth
     */
    static final ClassLoader getStackClassLoader(int depth) {
        Class<?>[] stack = VMStack.getClasses(depth + 1, false);
        if(stack.length < depth + 1) {
            return null;
        }
        return stack[depth].getClassLoader(); 
    }

    /**
     * This method must be provided by the VM vendor, as it is called by
     * java.lang.System.loadLibrary(). System.loadLibrary() cannot call
     * Runtime.loadLibrary() because this method loads the library using the
     * ClassLoader of the calling method. Loads and links the library specified
     * by the argument.
     * 
     * @param libName
     *            the name of the library to load
     * @param loader
     *            the classloader in which to load the library
     * @throws UnsatisfiedLinkError
     *             if the library could not be loaded
     * @throws SecurityException
     *             if the library was not allowed to be loaded
     * <p>
     * <strong>Note: </strong>This method does nothing in the Android reference
     * implementation.
     * </p>
     */
    static void loadLibraryWithClassLoader(String libName, ClassLoader loader) {
        return;
    }

    /**
     * This method must be provided by the VM vendor, as it is called by
     * java.lang.System.load(). System.load() cannot call Runtime.load() because
     * the library is loaded using the ClassLoader of the calling method. Loads
     * and links the library specified by the argument. No security check is
     * done.
     * <p>
     * <strong>Note: </strong>This method does nothing in the Android reference
     * implementation.
     * </p>
     * 
     * @param libName
     *            the name of the library to load
     * @param loader
     *            the classloader in which to load the library
     * @param libraryPath
     *            the library path to search, or null
     * @throws UnsatisfiedLinkError
     *             if the library could not be loaded
     */
    static void loadLibraryWithPath(String libName, ClassLoader loader, String libraryPath) {
        return;
    }

    /**
     * Sets the assertion status of the class with the specified name.
     * <p>
     * <strong>Note: </strong>This method does nothing in the Android reference
     * implementation.
     * </p>
     * 
     * @param cname
     *            the name of the class for which to set the assertion status.
     * @param enable
     *            the new assertion status.
     * @since Android 1.0
     */
    public void setClassAssertionStatus(String cname, boolean enable) {
        return;
    }

    /**
     * Sets the assertion status of the package with the specified name.
     * <p>
     * <strong>Note: </strong>This method does nothing in the Android reference
     * implementation.
     * </p>
     * 
     * @param pname
     *            the name of the package for which to set the assertion status.
     * @param enable
     *            the new assertion status.
     * @since Android 1.0
     */    
    public void setPackageAssertionStatus(String pname, boolean enable) {
        return;
    }

    /**
     * Sets the default assertion status for this class loader.
     * <p>
     * <strong>Note: </strong>This method does nothing in the Android reference
     * implementation.
     * </p>
     * 
     * @param enable
     *            the new assertion status.
     * @since Android 1.0
     */
    public void setDefaultAssertionStatus(boolean enable) {
        return;
    }

    /**
     * Sets the default assertion status for this class loader to {@code false}
     * and removes any package default and class assertion status settings.
     * <p>
     * <strong>Note:</strong> This method does nothing in the Android reference
     * implementation.
     * </p>
     * 
     * @since Android 1.0
     */
    public void clearAssertionStatus() {
        return;
    }

    /**
     * Returns the assertion status of the named class Returns the assertion
     * status of the class or nested class if it has been set. Otherwise returns
     * the assertion status of its package or superpackage if that has been set.
     * Otherwise returns the default assertion status. Returns 1 for enabled and
     * 0 for disabled.
     * 
     * @return the assertion status.
     * @param cname
     *            the name of class.
     */
    boolean getClassAssertionStatus(String cname) {
        return false;
    }

    /**
     * Returns the assertion status of the named package Returns the assertion
     * status of the named package or superpackage if that has been set.
     * Otherwise returns the default assertion status. Returns 1 for enabled and
     * 0 for disabled.
     * 
     * @return the assertion status.
     * @param pname
     *            the name of package.
     */
    boolean getPackageAssertionStatus(String pname) {
        return false;
    }

    /**
     * Returns the default assertion status
     * 
     * @return the default assertion status.
     */
    boolean getDefaultAssertionStatus() {
        return false;
    }
}

/*
 * Provides a helper class that combines two existing URL enumerations into one.
 * It is required for the getResources() methods. Items are fetched from the
 * first enumeration until it's empty, then from the second one.
 */
class TwoEnumerationsInOne implements Enumeration<URL> {

    private Enumeration<URL> first;

    private Enumeration<URL> second;

    public TwoEnumerationsInOne(Enumeration<URL> first, Enumeration<URL> second) {
        this.first = first;
        this.second = second;
    }

    public boolean hasMoreElements() {
        return first.hasMoreElements() || second.hasMoreElements();
    }

    public URL nextElement() {
        if (first.hasMoreElements()) {
            return first.nextElement();
        } else {
            return second.nextElement();
        }
    }

}

/**
 * Provides an explicit representation of the boot class loader. It sits at the
 * head of the class loader chain and delegates requests to the VM's internal
 * class loading mechanism.
 */
class BootClassLoader extends ClassLoader {

    static BootClassLoader instance;

    public static BootClassLoader getInstance() {
        if (instance == null) {
            instance = new BootClassLoader();
        }

        return instance;
    }

    public BootClassLoader() {
        super(null, true);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return VMClassLoader.loadClass(name, false);
    }

    @Override
    protected URL findResource(String name) {
        return VMClassLoader.getResource(name);
    }

    @SuppressWarnings("unused")
    @Override
    protected Enumeration<URL> findResources(String resName) throws IOException {
        Enumeration<URL> result = VMClassLoader.getResources(resName);

        // VMClassLoader doesn't keep the contract for getResources()
        if (result == null) {
            result = EmptyEnumeration.getInstance();
        }

        return result;
    }

    /**
     * Returns package information for the given package. Unfortunately, the
     * Android BootClassLoader doesn't really have this information, and as a
     * non-secure ClassLoader, it isn't even required to, according to the spec.
     * Yet, we want to provide it, in order to make all those hopeful callers of
     * {@code myClass.getPackage().getName()} happy. Thus we construct a Package
     * object the first time it is being requested and fill most of the fields
     * with dummy values. The Package object is then put into the ClassLoader's
     * Package cache, so we see the same one next time. We don't create Package
     * objects for null arguments or for the default package.
     * <p>
     * There a limited chance that we end up with multiple Package objects
     * representing the same package: It can happen when when a package is
     * scattered across different JAR files being loaded by different
     * ClassLoaders. Rather unlikely, and given that this whole thing is more or
     * less a workaround, probably not worth the effort.
     */
    @Override
    protected Package getPackage(String name) {
        if (name != null && !"".equals(name)) {
            synchronized (this) {
                Package pack = super.getPackage(name);

                if (pack == null) {
                    pack = definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0",
                            "Unknown", null);
                }

                return pack;
            }
        }

        return null;
    }

    @Override
    public URL getResource(String resName) {
        return findResource(resName);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve)
           throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            clazz = findClass(className);
        }

        return clazz;
    }

    @Override
    public Enumeration<URL> getResources(String resName) throws IOException {
        return findResources(resName);
    }
}

/**
 * TODO Open issues - Missing / empty methods - Signer stuff - Protection
 * domains - Assertions
 */
