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

/*
 * Class loading, including bootstrap class loader, linking, and
 * initialization.
 */

#define LOG_CLASS_LOADING 0

#include "Dalvik.h"
#include "libdex/DexClass.h"

#include <stdlib.h>
#include <stddef.h>
#include <sys/stat.h>

#if LOG_CLASS_LOADING
#include <unistd.h>
#include <pthread.h>
#include <cutils/process_name.h>
#include <sys/types.h>
#endif

/*
Notes on Linking and Verification

The basic way to retrieve a class is to load it, make sure its superclass
and interfaces are available, prepare its fields, and return it.  This gets
a little more complicated when multiple threads can be trying to retrieve
the class simultaneously, requiring that we use the class object's monitor
to keep things orderly.

The linking (preparing, resolving) of a class can cause us to recursively
load superclasses and interfaces.  Barring circular references (e.g. two
classes that are superclasses of each other), this will complete without
the loader attempting to access the partially-linked class.

With verification, the situation is different.  If we try to verify
every class as we load it, we quickly run into trouble.  Even the lowly
java.lang.Object requires CloneNotSupportedException; follow the list
of referenced classes and you can head down quite a trail.  The trail
eventually leads back to Object, which is officially not fully-formed yet.

The VM spec (specifically, v2 5.4.1) notes that classes pulled in during
verification do not need to be prepared or verified.  This means that we
are allowed to have loaded but unverified classes.  It further notes that
the class must be verified before it is initialized, which allows us to
defer verification for all classes until class init.  You can't execute
code or access fields in an uninitialized class, so this is safe.

It also allows a more peaceful coexistence between verified and
unverifiable code.  If class A refers to B, and B has a method that
refers to a bogus class C, should we allow class A to be verified?
If A only exercises parts of B that don't use class C, then there is
nothing wrong with running code in A.  We can fully verify both A and B,
and allow execution to continue until B causes initialization of C.  The
VerifyError is thrown close to the point of use.

This gets a little weird with java.lang.Class, which is the only class
that can be instantiated before it is initialized.  We have to force
initialization right after the class is created, because by definition we
have instances of it on the heap, and somebody might get a class object and
start making virtual calls on it.  We can end up going recursive during
verification of java.lang.Class, but we avoid that by checking to see if
verification is already in progress before we try to initialize it.
*/

/*
Notes on class loaders and interaction with optimization / verification

In what follows, "pre-verification" and "optimization" are the steps
performed by the dexopt command, which attempts to verify and optimize
classes as part of unpacking jar files and storing the DEX data in the
dalvik-cache directory.  These steps are performed by loading the DEX
files directly, without any assistance from ClassLoader instances.

When we pre-verify and optimize a class in a DEX file, we make some
assumptions about where the class loader will go to look for classes.
If we can't guarantee those assumptions, e.g. because a class ("AppClass")
references something not defined in the bootstrap jars or the AppClass jar,
we can't pre-verify or optimize the class.

The VM doesn't define the behavior of user-defined class loaders.
For example, suppose application class AppClass, loaded by UserLoader,
has a method that creates a java.lang.String.  The first time
AppClass.stringyMethod tries to do something with java.lang.String, it
asks UserLoader to find it.  UserLoader is expected to defer to its parent
loader, but isn't required to.  UserLoader might provide a replacement
for String.

We can run into trouble if we pre-verify AppClass with the assumption that
java.lang.String will come from core.jar, and don't verify this assumption
at runtime.  There are two places that an alternate implementation of
java.lang.String can come from: the AppClass jar, or from some other jar
that UserLoader knows about.  (Someday UserLoader will be able to generate
some bytecode and call DefineClass, but not yet.)

To handle the first situation, the pre-verifier will explicitly check for
conflicts between the class being optimized/verified and the bootstrap
classes.  If an app jar contains a class that has the same package and
class name as a class in a bootstrap jar, the verification resolver refuses
to find either, which will block pre-verification and optimization on
classes that reference ambiguity.  The VM will postpone verification of
the app class until first load.

For the second situation, we need to ensure that all references from a
pre-verified class are satisified by the class' jar or earlier bootstrap
jars.  In concrete terms: when resolving a reference to NewClass,
which was caused by a reference in class AppClass, we check to see if
AppClass was pre-verified.  If so, we require that NewClass comes out
of either the AppClass jar or one of the jars in the bootstrap path.
(We may not control the class loaders, but we do manage the DEX files.
We can verify that it's either (loader==null && dexFile==a_boot_dex)
or (loader==UserLoader && dexFile==AppClass.dexFile).  Classes from
DefineClass can't be pre-verified, so this doesn't apply.)

This should ensure that you can't "fake out" the pre-verifier by creating
a user-defined class loader that replaces system classes.  It should
also ensure that you can write such a loader and have it work in the
expected fashion; all you lose is some performance due to "just-in-time
verification" and the lack of DEX optimizations.

There is a "back door" of sorts in the class resolution check, due to
the fact that the "class ref" entries are shared between the bytecode
and meta-data references (e.g. annotations and exception handler lists).
The class references in annotations have no bearing on class verification,
so when a class does an annotation query that causes a class reference
index to be resolved, we don't want to fail just because the calling
class was pre-verified and the resolved class is in some random DEX file.
The successful resolution adds the class to the "resolved classes" table,
so when optimized bytecode references it we don't repeat the resolve-time
check.  We can avoid this by not updating the "resolved classes" table
when the class reference doesn't come out of something that has been
checked by the verifier, but that has a nonzero performance impact.
Since the ultimate goal of this test is to catch an unusual situation
(user-defined class loaders redefining core classes), the added caution
may not be worth the performance hit.
*/

static ClassPathEntry* processClassPath(const char* pathStr, bool isBootstrap);
static void freeCpeArray(ClassPathEntry* cpe);

static ClassObject* findClassFromLoaderNoInit(
    const char* descriptor, Object* loader);
static ClassObject* findClassNoInit(const char* descriptor, Object* loader,\
    DvmDex* pDvmDex);
static ClassObject* loadClassFromDex(DvmDex* pDvmDex,
    const DexClassDef* pClassDef, Object* loader);
static void loadMethodFromDex(ClassObject* clazz, const DexMethod* pDexMethod,\
    Method* meth);
static int computeJniArgInfo(const DexProto* proto);
static void loadSFieldFromDex(ClassObject* clazz,
    const DexField* pDexSField, StaticField* sfield);
static void loadIFieldFromDex(ClassObject* clazz,
    const DexField* pDexIField, InstField* field);
static void freeMethodInnards(Method* meth);
static bool createVtable(ClassObject* clazz);
static bool createIftable(ClassObject* clazz);
static bool insertMethodStubs(ClassObject* clazz);
static bool computeFieldOffsets(ClassObject* clazz);
static void throwEarlierClassFailure(ClassObject* clazz);

#if LOG_CLASS_LOADING
/*
 * Logs information about a class loading with given timestamp.
 *
 * TODO: In the case where we fail in dvmLinkClass() and log the class as closing (type='<'),
 * it would probably be better to use a new type code to indicate the failure.  This change would
 * require a matching change in the parser and analysis code in frameworks/base/tools/preload.
 */
static void logClassLoadWithTime(char type, ClassObject* clazz, u8 time) {
    pid_t ppid = getppid();
    pid_t pid = getpid();
    unsigned int tid = (unsigned int) pthread_self();

    LOG(LOG_INFO, "PRELOAD", "%c%d:%d:%d:%s:%d:%s:%lld\n", type, ppid, pid, tid,
        get_process_name(), (int) clazz->classLoader, clazz->descriptor,
        time);
}

/*
 * Logs information about a class loading.
 */
static void logClassLoad(char type, ClassObject* clazz) {
    logClassLoadWithTime(type, clazz, dvmGetThreadCpuTimeNsec());
}
#endif

/*
 * Some LinearAlloc unit tests.
 */
static void linearAllocTests()
{
    char* fiddle;
    int try = 1;

    switch (try) {
    case 0:
        fiddle = dvmLinearAlloc(NULL, 3200-28);
        dvmLinearReadOnly(NULL, fiddle);
        break;
    case 1:
        fiddle = dvmLinearAlloc(NULL, 3200-24);
        dvmLinearReadOnly(NULL, fiddle);
        break;
    case 2:
        fiddle = dvmLinearAlloc(NULL, 3200-20);
        dvmLinearReadOnly(NULL, fiddle);
        break;
    case 3:
        fiddle = dvmLinearAlloc(NULL, 3200-16);
        dvmLinearReadOnly(NULL, fiddle);
        break;
    case 4:
        fiddle = dvmLinearAlloc(NULL, 3200-12);
        dvmLinearReadOnly(NULL, fiddle);
        break;
    }
    fiddle = dvmLinearAlloc(NULL, 896);
    dvmLinearReadOnly(NULL, fiddle);
    fiddle = dvmLinearAlloc(NULL, 20);      // watch addr of this alloc
    dvmLinearReadOnly(NULL, fiddle);

    fiddle = dvmLinearAlloc(NULL, 1);
    fiddle[0] = 'q';
    dvmLinearReadOnly(NULL, fiddle);
    fiddle = dvmLinearAlloc(NULL, 4096);
    fiddle[0] = 'x';
    fiddle[4095] = 'y';
    dvmLinearReadOnly(NULL, fiddle);
    dvmLinearFree(NULL, fiddle);
    fiddle = dvmLinearAlloc(NULL, 0);
    dvmLinearReadOnly(NULL, fiddle);
    fiddle = dvmLinearRealloc(NULL, fiddle, 12);
    fiddle[11] = 'z';
    dvmLinearReadOnly(NULL, fiddle);
    fiddle = dvmLinearRealloc(NULL, fiddle, 5);
    dvmLinearReadOnly(NULL, fiddle);
    fiddle = dvmLinearAlloc(NULL, 17001);
    fiddle[0] = 'x';
    fiddle[17000] = 'y';
    dvmLinearReadOnly(NULL, fiddle);

    char* str = dvmLinearStrdup(NULL, "This is a test!");
    LOGI("GOT: '%s'\n", str);

    dvmLinearAllocDump(NULL);
    dvmLinearFree(NULL, str);
}

/*
 * Initialize the bootstrap class loader.
 *
 * Call this after the bootclasspath string has been finalized.
 */
bool dvmClassStartup(void)
{
    ClassObject* unlinkedClass;

    /* make this a requirement -- don't currently support dirs in path */
    if (strcmp(gDvm.bootClassPathStr, ".") == 0) {
        LOGE("ERROR: must specify non-'.' bootclasspath\n");
        return false;
    }

    gDvm.loadedClasses =
        dvmHashTableCreate(256, (HashFreeFunc) dvmFreeClassInnards);

    gDvm.pBootLoaderAlloc = dvmLinearAllocCreate(NULL);
    if (gDvm.pBootLoaderAlloc == NULL)
        return false;

    if (false) {
        linearAllocTests();
        exit(0);
    }

    /*
     * Class serial number.  We start with a high value to make it distinct
     * in binary dumps (e.g. hprof).
     */
    gDvm.classSerialNumber = 0x50000000;


    /* This placeholder class is used while a ClassObject is
     * loading/linking so those not in the know can still say
     * "obj->clazz->...".
     */
    unlinkedClass = &gDvm.unlinkedJavaLangClassObject;

    memset(unlinkedClass, 0, sizeof(*unlinkedClass));

    /* Set obj->clazz to NULL so anyone who gets too interested
     * in the fake class will crash.
     */
    DVM_OBJECT_INIT(&unlinkedClass->obj, NULL);
    unlinkedClass->descriptor = "!unlinkedClass";
    dvmSetClassSerialNumber(unlinkedClass);

    gDvm.unlinkedJavaLangClass = unlinkedClass;

    /*
     * Process the bootstrap class path.  This means opening the specified
     * DEX or Jar files and possibly running them through the optimizer.
     */
    assert(gDvm.bootClassPath == NULL);
    processClassPath(gDvm.bootClassPathStr, true);

    if (gDvm.bootClassPath == NULL)
        return false;

    return true;
}

/*
 * Clean up.
 */
void dvmClassShutdown(void)
{
    int i;

    /* discard all system-loaded classes */
    dvmHashTableFree(gDvm.loadedClasses);
    gDvm.loadedClasses = NULL;

    /* discard primitive classes created for arrays */
    for (i = 0; i < PRIM_MAX; i++)
        dvmFreeClassInnards(gDvm.primitiveClass[i]);

    /* this closes DEX files, JAR files, etc. */
    freeCpeArray(gDvm.bootClassPath);
    gDvm.bootClassPath = NULL;

    dvmLinearAllocDestroy(NULL);
}


/*
 * ===========================================================================
 *      Bootstrap class loader
 * ===========================================================================
 */

/*
 * Dump the contents of a ClassPathEntry array.
 */
static void dumpClassPath(const ClassPathEntry* cpe)
{
    int idx = 0;

    while (cpe->kind != kCpeLastEntry) {
        const char* kindStr;

        switch (cpe->kind) {
        case kCpeDir:       kindStr = "dir";    break;
        case kCpeJar:       kindStr = "jar";    break;
        case kCpeDex:       kindStr = "dex";    break;
        default:            kindStr = "???";    break;
        }

        LOGI("  %2d: type=%s %s %p\n", idx, kindStr, cpe->fileName, cpe->ptr);
        if (CALC_CACHE_STATS && cpe->kind == kCpeJar) {
            JarFile* pJarFile = (JarFile*) cpe->ptr;
            DvmDex* pDvmDex = dvmGetJarFileDex(pJarFile);
            dvmDumpAtomicCacheStats(pDvmDex->pInterfaceCache);
        }

        cpe++;
        idx++;
    }
}

/*
 * Dump the contents of the bootstrap class path.
 */
void dvmDumpBootClassPath(void)
{
    dumpClassPath(gDvm.bootClassPath);
}

/*
 * Returns "true" if the class path contains the specified path.
 */
bool dvmClassPathContains(const ClassPathEntry* cpe, const char* path)
{
    while (cpe->kind != kCpeLastEntry) {
        if (strcmp(cpe->fileName, path) == 0)
            return true;

        cpe++;
    }
    return false;
}

/*
 * Free an array of ClassPathEntry structs.
 *
 * We release the contents of each entry, then free the array itself.
 */
static void freeCpeArray(ClassPathEntry* cpe)
{
    ClassPathEntry* cpeStart = cpe;

    if (cpe == NULL)
        return;

    while (cpe->kind != kCpeLastEntry) {
        switch (cpe->kind) {
        case kCpeJar:
            /* free JarFile */
            dvmJarFileFree((JarFile*) cpe->ptr);
            break;
        case kCpeDex:
            /* free RawDexFile */
            dvmRawDexFileFree((RawDexFile*) cpe->ptr);
            break;
        default:
            /* e.g. kCpeDir */
            assert(cpe->ptr == NULL);
            break;
        }

        free(cpe->fileName);
        cpe++;
    }

    free(cpeStart);
}

/*
 * Prepare a ClassPathEntry struct, which at this point only has a valid
 * filename.  We need to figure out what kind of file it is, and for
 * everything other than directories we need to open it up and see
 * what's inside.
 */
static bool prepareCpe(ClassPathEntry* cpe, bool isBootstrap)
{
    JarFile* pJarFile = NULL;
    RawDexFile* pRawDexFile = NULL;
    struct stat sb;
    int cc;

    cc = stat(cpe->fileName, &sb);
    if (cc < 0) {
        LOGW("Unable to stat classpath element '%s'\n", cpe->fileName);
        return false;
    }
    if (S_ISDIR(sb.st_mode)) {
        /*
         * The directory will usually have .class files in subdirectories,
         * which may be a few levels down.  Doing a recursive scan and
         * caching the results would help us avoid hitting the filesystem
         * on misses.  Whether or not this is of measureable benefit
         * depends on a number of factors, but most likely it is not
         * worth the effort (especially since most of our stuff will be
         * in DEX or JAR).
         */
        cpe->kind = kCpeDir;
        assert(cpe->ptr == NULL);
        return true;
    }

    if (dvmJarFileOpen(cpe->fileName, NULL, &pJarFile, isBootstrap) == 0) {
        cpe->kind = kCpeJar;
        cpe->ptr = pJarFile;
        return true;
    }

    // TODO: do we still want to support "raw" DEX files in the classpath?
    if (dvmRawDexFileOpen(cpe->fileName, NULL, &pRawDexFile, isBootstrap) == 0)
    {
        cpe->kind = kCpeDex;
        cpe->ptr = pRawDexFile;
        return true;
    }

    return false;
}

/*
 * Convert a colon-separated list of directories, Zip files, and DEX files
 * into an array of ClassPathEntry structs.
 *
 * If we're unable to load a bootstrap class path entry, we fail.  This
 * is necessary to preserve the dependencies implied by optimized DEX files
 * (e.g. if the same class appears in multiple places).
 *
 * During normal startup we fail if there are no entries, because we won't
 * get very far without the basic language support classes, but if we're
 * optimizing a DEX file we allow it.
 */
static ClassPathEntry* processClassPath(const char* pathStr, bool isBootstrap)
{
    ClassPathEntry* cpe = NULL;
    char* mangle;
    char* cp;
    const char* end;
    int idx, count;

    assert(pathStr != NULL);

    mangle = strdup(pathStr);

    /*
     * Run through and essentially strtok() the string.  Get a count of
     * the #of elements while we're at it.
     *
     * If the path was constructed strangely (e.g. ":foo::bar:") this will
     * over-allocate, which isn't ideal but is mostly harmless.
     */
    count = 1;
    for (cp = mangle; *cp != '\0'; cp++) {
        if (*cp == ':') {   /* separates two entries */
            count++;
            *cp = '\0';
        }
    }
    end = cp;

    /*
     * Allocate storage.  We over-alloc by one so we can set an "end" marker.
     */
    cpe = (ClassPathEntry*) calloc(count+1, sizeof(ClassPathEntry));

    /*
     * Set the global pointer so the DEX file dependency stuff can find it.
     */
    gDvm.bootClassPath = cpe;

    /*
     * Go through a second time, pulling stuff out.
     */
    cp = mangle;
    idx = 0;
    while (cp < end) {
        if (*cp == '\0') {
            /* leading, trailing, or doubled ':'; ignore it */
        } else {
            ClassPathEntry tmp;
            tmp.kind = kCpeUnknown;
            tmp.fileName = strdup(cp);
            tmp.ptr = NULL;

            /* drop an end marker here so DEX loader can walk unfinished list */
            cpe[idx].kind = kCpeLastEntry;
            cpe[idx].fileName = NULL;
            cpe[idx].ptr = NULL;

            if (!prepareCpe(&tmp, isBootstrap)) {
                LOGD("Failed on '%s' (boot=%d)\n", tmp.fileName, isBootstrap);
                /* drop from list and continue on */
                free(tmp.fileName);

                if (isBootstrap || gDvm.optimizing) {
                    /* if boot path entry or we're optimizing, this is fatal */
                    free(cpe);
                    cpe = NULL;
                    goto bail;
                }
            } else {
                /* copy over, pointers and all */
                if (tmp.fileName[0] != '/')
                    LOGW("Non-absolute bootclasspath entry '%s'\n",
                        tmp.fileName);
                cpe[idx] = tmp;
                idx++;
            }
        }

        cp += strlen(cp) +1;
    }
    assert(idx <= count);
    if (idx == 0 && !gDvm.optimizing) {
        LOGE("ERROR: no valid entries found in bootclasspath '%s'\n", pathStr);
        free(cpe);
        cpe = NULL;
        goto bail;
    }

    LOGVV("  (filled %d of %d slots)\n", idx, count);

    /* put end marker in over-alloc slot */
    cpe[idx].kind = kCpeLastEntry;
    cpe[idx].fileName = NULL;
    cpe[idx].ptr = NULL;

    //dumpClassPath(cpe);

bail:
    free(mangle);
    gDvm.bootClassPath = cpe;
    return cpe;
}

/*
 * Search the DEX files we loaded from the bootstrap class path for a DEX
 * file that has the class with the matching descriptor.
 *
 * Returns the matching DEX file and DexClassDef entry if found, otherwise
 * returns NULL.
 */
static DvmDex* searchBootPathForClass(const char* descriptor,
    const DexClassDef** ppClassDef)
{
    const ClassPathEntry* cpe = gDvm.bootClassPath;
    const DexClassDef* pFoundDef = NULL;
    DvmDex* pFoundFile = NULL;

    LOGVV("+++ class '%s' not yet loaded, scanning bootclasspath...\n",
        descriptor);

    while (cpe->kind != kCpeLastEntry) {
        //LOGV("+++  checking '%s' (%d)\n", cpe->fileName, cpe->kind);

        switch (cpe->kind) {
        case kCpeDir:
            LOGW("Directory entries ('%s') not supported in bootclasspath\n",
                cpe->fileName);
            break;
        case kCpeJar:
            {
                JarFile* pJarFile = (JarFile*) cpe->ptr;
                const DexClassDef* pClassDef;
                DvmDex* pDvmDex;

                pDvmDex = dvmGetJarFileDex(pJarFile);
                pClassDef = dexFindClass(pDvmDex->pDexFile, descriptor);
                if (pClassDef != NULL) {
                    /* found */
                    pFoundDef = pClassDef;
                    pFoundFile = pDvmDex;
                    goto found;
                }
            }
            break;
        case kCpeDex:
            {
                RawDexFile* pRawDexFile = (RawDexFile*) cpe->ptr;
                const DexClassDef* pClassDef;
                DvmDex* pDvmDex;

                pDvmDex = dvmGetRawDexFileDex(pRawDexFile);
                pClassDef = dexFindClass(pDvmDex->pDexFile, descriptor);
                if (pClassDef != NULL) {
                    /* found */
                    pFoundDef = pClassDef;
                    pFoundFile = pDvmDex;
                    goto found;
                }
            }
            break;
        default:
            LOGE("Unknown kind %d\n", cpe->kind);
            assert(false);
            break;
        }

        cpe++;
    }

    /*
     * Special handling during verification + optimization.
     *
     * The DEX optimizer needs to load classes from the DEX file it's working
     * on.  Rather than trying to insert it into the bootstrap class path
     * or synthesizing a class loader to manage it, we just make it available
     * here.  It logically comes after all existing entries in the bootstrap
     * class path.
     */
    if (gDvm.bootClassPathOptExtra != NULL) {
        const DexClassDef* pClassDef;

        pClassDef =
            dexFindClass(gDvm.bootClassPathOptExtra->pDexFile, descriptor);
        if (pClassDef != NULL) {
            /* found */
            pFoundDef = pClassDef;
            pFoundFile = gDvm.bootClassPathOptExtra;
        }
    }

found:
    *ppClassDef = pFoundDef;
    return pFoundFile;
}

/*
 * Set the "extra" DEX, which becomes a de facto member of the bootstrap
 * class set.
 */
void dvmSetBootPathExtraDex(DvmDex* pDvmDex)
{
    gDvm.bootClassPathOptExtra = pDvmDex;
}


/*
 * Return the #of entries in the bootstrap class path.
 *
 * (Used for ClassLoader.getResources().)
 */
int dvmGetBootPathSize(void)
{
    const ClassPathEntry* cpe = gDvm.bootClassPath;

    while (cpe->kind != kCpeLastEntry)
        cpe++;

    return cpe - gDvm.bootClassPath;
}

/*
 * Find a resource with the specified name in entry N of the boot class path.
 *
 * We return a newly-allocated String of one of these forms:
 *   file://path/name
 *   jar:file://path!/name
 * Where "path" is the bootstrap class path entry and "name" is the string
 * passed into this method.  "path" needs to be an absolute path (starting
 * with '/'); if it's not we'd need to "absolutify" it as part of forming
 * the URL string.
 */
StringObject* dvmGetBootPathResource(const char* name, int idx)
{
    const int kUrlOverhead = 13;        // worst case for Jar URL
    const ClassPathEntry* cpe = gDvm.bootClassPath;
    StringObject* urlObj = NULL;

    LOGV("+++ searching for resource '%s' in %d(%s)\n",
        name, idx, cpe[idx].fileName);

    /* we could use direct array index, but I don't entirely trust "idx" */
    while (idx-- && cpe->kind != kCpeLastEntry)
        cpe++;
    if (cpe->kind == kCpeLastEntry) {
        assert(false);
        return NULL;
    }

    char urlBuf[strlen(name) + strlen(cpe->fileName) + kUrlOverhead +1];

    switch (cpe->kind) {
    case kCpeDir:
        sprintf(urlBuf, "file://%s/%s", cpe->fileName, name);
        if (access(urlBuf+7, F_OK) != 0)
            goto bail;
        break;
    case kCpeJar:
        {
            JarFile* pJarFile = (JarFile*) cpe->ptr;
            if (dexZipFindEntry(&pJarFile->archive, name) == NULL)
                goto bail;
            sprintf(urlBuf, "jar:file://%s!/%s", cpe->fileName, name);
        }
        break;
    case kCpeDex:
        LOGV("No resources in DEX files\n");
        goto bail;
    default:
        assert(false);
        goto bail;
    }

    LOGV("+++ using URL='%s'\n", urlBuf);
    urlObj = dvmCreateStringFromCstr(urlBuf, ALLOC_DEFAULT);

bail:
    return urlObj;
}


/*
 * ===========================================================================
 *      Class list management
 * ===========================================================================
 */

/* search for these criteria in the Class hash table */
typedef struct ClassMatchCriteria {
    const char* descriptor;
    Object*     loader;
} ClassMatchCriteria;

#define kInitLoaderInc  4       /* must be power of 2 */

/*
 * Determine if "loader" appears in clazz' initiating loader list.
 *
 * The class hash table lock must be held when calling here, since
 * it's also used when updating a class' initiating loader list.
 *
 * TODO: switch to some sort of lock-free data structure so we don't have
 * to grab the lock to do a lookup.  Among other things, this would improve
 * the speed of compareDescriptorClasses().
 */
bool dvmLoaderInInitiatingList(const ClassObject* clazz, const Object* loader)
{
    /*
     * The bootstrap class loader can't be just an initiating loader for
     * anything (it's always the defining loader if the class is visible
     * to it).  We don't put defining loaders in the initiating list.
     */
    if (loader == NULL)
        return false;

    /*
     * Scan the list for a match.  The list is expected to be short.
     */
    int i;
    for (i = clazz->initiatingLoaderCount-1; i >= 0; --i) {
        if (clazz->initiatingLoaders[i] == loader) {
            //LOGI("+++ found initiating match %p in %s\n",
            //    loader, clazz->descriptor);
            return true;
        }
    }
    return false;
}

/*
 * Add "loader" to clazz's initiating loader set, unless it's the defining
 * class loader.
 *
 * In the common case this will be a short list, so we don't need to do
 * anything too fancy here.
 *
 * This locks gDvm.loadedClasses for synchronization, so don't hold it
 * when calling here.
 */
void dvmAddInitiatingLoader(ClassObject* clazz, Object* loader)
{
    if (loader != clazz->classLoader) {
        assert(loader != NULL);

        LOGVV("Adding %p to '%s' init list\n", loader, clazz->descriptor);
        dvmHashTableLock(gDvm.loadedClasses);

        /*
         * Make sure nobody snuck in.  The penalty for adding twice is
         * pretty minor, and probably outweighs the O(n^2) hit for
         * checking before every add, so we may not want to do this.
         */
        if (false && dvmLoaderInInitiatingList(clazz, loader)) {
            LOGW("WOW: simultaneous add of initiating class loader\n");
            goto bail_unlock;
        }

        /*
         * The list never shrinks, so we just keep a count of the
         * number of elements in it, and reallocate the buffer when
         * we run off the end.
         *
         * The pointer is initially NULL, so we *do* want to call realloc
         * when count==0.
         */
        if ((clazz->initiatingLoaderCount & (kInitLoaderInc-1)) == 0) {
            Object** newList;

            newList = (Object**) realloc(clazz->initiatingLoaders,
                        (clazz->initiatingLoaderCount + kInitLoaderInc)
                         * sizeof(Object*));
            if (newList == NULL) {
                /* this is mainly a cache, so it's not the EotW */
                assert(false);
                goto bail_unlock;
            }
            clazz->initiatingLoaders = newList;

            //LOGI("Expanded init list to %d (%s)\n",
            //    clazz->initiatingLoaderCount+kInitLoaderInc,
            //    clazz->descriptor);
        }

        clazz->initiatingLoaders[clazz->initiatingLoaderCount++] = loader;

bail_unlock:
        dvmHashTableUnlock(gDvm.loadedClasses);
    }
}

/*
 * (This is a dvmHashTableLookup callback.)
 *
 * Entries in the class hash table are stored as { descriptor, d-loader }
 * tuples.  If the hashed class descriptor matches the requested descriptor,
 * and the hashed defining class loader matches the requested class
 * loader, we're good.  If only the descriptor matches, we check to see if the
 * loader is in the hashed class' initiating loader list.  If so, we
 * can return "true" immediately and skip some of the loadClass melodrama.
 *
 * The caller must lock the hash table before calling here.
 *
 * Returns 0 if a matching entry is found, nonzero otherwise.
 */
static int hashcmpClassByCrit(const void* vclazz, const void* vcrit)
{
    const ClassObject* clazz = (const ClassObject*) vclazz;
    const ClassMatchCriteria* pCrit = (const ClassMatchCriteria*) vcrit;
    bool match;

    match = (strcmp(clazz->descriptor, pCrit->descriptor) == 0 &&
             (clazz->classLoader == pCrit->loader ||
              (pCrit->loader != NULL &&
               dvmLoaderInInitiatingList(clazz, pCrit->loader)) ));
    //if (match)
    //    LOGI("+++ %s %p matches existing %s %p\n",
    //        pCrit->descriptor, pCrit->loader,
    //        clazz->descriptor, clazz->classLoader);
    return !match;
}

/*
 * Like hashcmpClassByCrit, but passing in a fully-formed ClassObject
 * instead of a ClassMatchCriteria.
 */
static int hashcmpClassByClass(const void* vclazz, const void* vaddclazz)
{
    const ClassObject* clazz = (const ClassObject*) vclazz;
    const ClassObject* addClazz = (const ClassObject*) vaddclazz;
    bool match;

    match = (strcmp(clazz->descriptor, addClazz->descriptor) == 0 &&
             (clazz->classLoader == addClazz->classLoader ||
              (addClazz->classLoader != NULL &&
               dvmLoaderInInitiatingList(clazz, addClazz->classLoader)) ));
    return !match;
}

/*
 * Search through the hash table to find an entry with a matching descriptor
 * and an initiating class loader that matches "loader".
 *
 * The table entries are hashed on descriptor only, because they're unique
 * on *defining* class loader, not *initiating* class loader.  This isn't
 * great, because it guarantees we will have to probe when multiple
 * class loaders are used.
 *
 * Note this does NOT try to load a class; it just finds a class that
 * has already been loaded.
 *
 * If "unprepOkay" is set, this will return classes that have been added
 * to the hash table but are not yet fully loaded and linked.  Otherwise,
 * such classes are ignored.  (The only place that should set "unprepOkay"
 * is findClassNoInit(), which will wait for the prep to finish.)
 *
 * Returns NULL if not found.
 */
ClassObject* dvmLookupClass(const char* descriptor, Object* loader,
    bool unprepOkay)
{
    ClassMatchCriteria crit;
    void* found;
    u4 hash;

    crit.descriptor = descriptor;
    crit.loader = loader;
    hash = dvmComputeUtf8Hash(descriptor);

    LOGVV("threadid=%d: dvmLookupClass searching for '%s' %p\n",
        dvmThreadSelf()->threadId, descriptor, loader);

    dvmHashTableLock(gDvm.loadedClasses);
    found = dvmHashTableLookup(gDvm.loadedClasses, hash, &crit,
                hashcmpClassByCrit, false);
    dvmHashTableUnlock(gDvm.loadedClasses);

    /*
     * The class has been added to the hash table but isn't ready for use.
     * We're going to act like we didn't see it, so that the caller will
     * go through the full "find class" path, which includes locking the
     * object and waiting until it's ready.  We could do that lock/wait
     * here, but this is an extremely rare case, and it's simpler to have
     * the wait-for-class code centralized.
     */
    if (found != NULL && !unprepOkay && !dvmIsClassLinked(found)) {
        LOGV("Ignoring not-yet-ready %s, using slow path\n",
            ((ClassObject*)found)->descriptor);
        found = NULL;
    }

    return (ClassObject*) found;
}

/*
 * Add a new class to the hash table.
 *
 * The class is considered "new" if it doesn't match on both the class
 * descriptor and the defining class loader.
 *
 * TODO: we should probably have separate hash tables for each
 * ClassLoader. This could speed up dvmLookupClass and
 * other common operations. It does imply a VM-visible data structure
 * for each ClassLoader object with loaded classes, which we don't
 * have yet.
 */
bool dvmAddClassToHash(ClassObject* clazz)
{
    void* found;
    u4 hash;

    hash = dvmComputeUtf8Hash(clazz->descriptor);

    dvmHashTableLock(gDvm.loadedClasses);
    found = dvmHashTableLookup(gDvm.loadedClasses, hash, clazz,
                hashcmpClassByClass, true);
    dvmHashTableUnlock(gDvm.loadedClasses);

    LOGV("+++ dvmAddClassToHash '%s' %p (isnew=%d) --> %p\n",
        clazz->descriptor, clazz->classLoader,
        (found == (void*) clazz), clazz);

    //dvmCheckClassTablePerf();

    /* can happen if two threads load the same class simultaneously */
    return (found == (void*) clazz);
}

#if 0
/*
 * Compute hash value for a class.
 */
u4 hashcalcClass(const void* item)
{
    return dvmComputeUtf8Hash(((const ClassObject*) item)->descriptor);
}

/*
 * Check the performance of the "loadedClasses" hash table.
 */
void dvmCheckClassTablePerf(void)
{
    dvmHashTableLock(gDvm.loadedClasses);
    dvmHashTableProbeCount(gDvm.loadedClasses, hashcalcClass,
        hashcmpClassByClass);
    dvmHashTableUnlock(gDvm.loadedClasses);
}
#endif

/*
 * Remove a class object from the hash table.
 */
static void removeClassFromHash(ClassObject* clazz)
{
    LOGV("+++ removeClassFromHash '%s'\n", clazz->descriptor);

    u4 hash = dvmComputeUtf8Hash(clazz->descriptor);

    dvmHashTableLock(gDvm.loadedClasses);
    if (!dvmHashTableRemove(gDvm.loadedClasses, hash, clazz))
        LOGW("Hash table remove failed on class '%s'\n", clazz->descriptor);
    dvmHashTableUnlock(gDvm.loadedClasses);
}


/*
 * ===========================================================================
 *      Class creation
 * ===========================================================================
 */

/*
 * Set clazz->serialNumber to the next available value.
 *
 * This usually happens *very* early in class creation, so don't expect
 * anything else in the class to be ready.
 */
void dvmSetClassSerialNumber(ClassObject* clazz)
{
    u4 oldValue, newValue;

    assert(clazz->serialNumber == 0);

    do {
        oldValue = gDvm.classSerialNumber;
        newValue = oldValue + 1;
    } while (!ATOMIC_CMP_SWAP(&gDvm.classSerialNumber, oldValue, newValue));

    clazz->serialNumber = (u4) oldValue;
}


/*
 * Find the named class (by descriptor), using the specified
 * initiating ClassLoader.
 *
 * The class will be loaded and initialized if it has not already been.
 * If necessary, the superclass will be loaded.
 *
 * If the class can't be found, returns NULL with an appropriate exception
 * raised.
 */
ClassObject* dvmFindClass(const char* descriptor, Object* loader)
{
    ClassObject* clazz;

    clazz = dvmFindClassNoInit(descriptor, loader);
    if (clazz != NULL && clazz->status < CLASS_INITIALIZED) {
        /* initialize class */
        if (!dvmInitClass(clazz)) {
            /* init failed; leave it in the list, marked as bad */
            assert(dvmCheckException(dvmThreadSelf()));
            assert(clazz->status == CLASS_ERROR);
            return NULL;
        }
    }

    return clazz;
}

/*
 * Find the named class (by descriptor), using the specified
 * initiating ClassLoader.
 *
 * The class will be loaded if it has not already been, as will its
 * superclass.  It will not be initialized.
 *
 * If the class can't be found, returns NULL with an appropriate exception
 * raised.
 */
ClassObject* dvmFindClassNoInit(const char* descriptor,
        Object* loader)
{
    assert(descriptor != NULL);
    //assert(loader != NULL);

    LOGVV("FindClassNoInit '%s' %p\n", descriptor, loader);

    if (*descriptor == '[') {
        /*
         * Array class.  Find in table, generate if not found.
         */
        return dvmFindArrayClass(descriptor, loader);
    } else {
        /*
         * Regular class.  Find in table, load if not found.
         */
        if (loader != NULL) {
            return findClassFromLoaderNoInit(descriptor, loader);
        } else {
            return dvmFindSystemClassNoInit(descriptor);
        }
    }
}

/*
 * Load the named class (by descriptor) from the specified class
 * loader.  This calls out to let the ClassLoader object do its thing.
 *
 * Returns with NULL and an exception raised on error.
 */
static ClassObject* findClassFromLoaderNoInit(const char* descriptor,
    Object* loader)
{
    //LOGI("##### findClassFromLoaderNoInit (%s,%p)\n",
    //        descriptor, loader);

    Thread* self = dvmThreadSelf();
    ClassObject* clazz;

    assert(loader != NULL);

    /*
     * Do we already have it?
     *
     * The class loader code does the "is it already loaded" check as
     * well.  However, this call is much faster than calling through
     * interpreted code.  Doing this does mean that in the common case
     * (365 out of 420 calls booting the sim) we're doing the
     * lookup-by-descriptor twice.  It appears this is still a win, so
     * I'm keeping it in.
     */
    clazz = dvmLookupClass(descriptor, loader, false);
    if (clazz != NULL) {
        LOGVV("Already loaded: %s %p\n", descriptor, loader);
        return clazz;
    } else {
        LOGVV("Not already loaded: %s %p\n", descriptor, loader);
    }

    char* dotName = NULL;
    StringObject* nameObj = NULL;
    Object* excep;
    Method* loadClass;

    /* convert "Landroid/debug/Stuff;" to "android.debug.Stuff" */
    dotName = dvmDescriptorToDot(descriptor);
    if (dotName == NULL) {
        dvmThrowException("Ljava/lang/OutOfMemoryError;", NULL);
        goto bail;
    }
    nameObj = dvmCreateStringFromCstr(dotName, ALLOC_DEFAULT);
    if (nameObj == NULL) {
        assert(dvmCheckException(self));
        goto bail;
    }

    // TODO: cache the vtable offset
    loadClass = dvmFindVirtualMethodHierByDescriptor(loader->clazz, "loadClass",
                 "(Ljava/lang/String;)Ljava/lang/Class;");
    if (loadClass == NULL) {
        LOGW("Couldn't find loadClass in ClassLoader\n");
        goto bail;
    }

#ifdef WITH_PROFILER
    dvmMethodTraceClassPrepBegin();
#endif

    /*
     * Invoke loadClass().  This will probably result in a couple of
     * exceptions being thrown, because the ClassLoader.loadClass()
     * implementation eventually calls VMClassLoader.loadClass to see if
     * the bootstrap class loader can find it before doing its own load.
     */
    LOGVV("--- Invoking loadClass(%s, %p)\n", dotName, loader);
    JValue result;
    dvmCallMethod(self, loadClass, loader, &result, nameObj);
    clazz = (ClassObject*) result.l;

#ifdef WITH_PROFILER
    dvmMethodTraceClassPrepEnd();
#endif

    excep = dvmGetException(self);
    if (excep != NULL) {
#if DVM_SHOW_EXCEPTION >= 2
        LOGD("NOTE: loadClass '%s' %p threw exception %s\n",
            dotName, loader, excep->clazz->descriptor);
#endif
        dvmAddTrackedAlloc(excep, self);
        dvmClearException(self);
        dvmThrowChainedExceptionWithClassMessage(
            "Ljava/lang/NoClassDefFoundError;", descriptor, excep);
        dvmReleaseTrackedAlloc(excep, self);
        clazz = NULL;
        goto bail;
    } else {
        assert(clazz != NULL);
    }

    dvmAddInitiatingLoader(clazz, loader);

    LOGVV("--- Successfully loaded %s %p (thisldr=%p clazz=%p)\n",
        descriptor, clazz->classLoader, loader, clazz);

bail:
    dvmReleaseTrackedAlloc((Object*)nameObj, NULL);
    free(dotName);
    return clazz;
}

/*
 * Load the named class (by descriptor) from the specified DEX file.
 * Used by class loaders to instantiate a class object from a
 * VM-managed DEX.
 */
ClassObject* dvmDefineClass(DvmDex* pDvmDex, const char* descriptor,
    Object* classLoader)
{
    assert(pDvmDex != NULL);

    return findClassNoInit(descriptor, classLoader, pDvmDex);
}


/*
 * Find the named class (by descriptor), scanning through the
 * bootclasspath if it hasn't already been loaded.
 *
 * "descriptor" looks like "Landroid/debug/Stuff;".
 *
 * Uses NULL as the defining class loader.
 */
ClassObject* dvmFindSystemClass(const char* descriptor)
{
    ClassObject* clazz;

    clazz = dvmFindSystemClassNoInit(descriptor);
    if (clazz != NULL && clazz->status < CLASS_INITIALIZED) {
        /* initialize class */
        if (!dvmInitClass(clazz)) {
            /* init failed; leave it in the list, marked as bad */
            assert(dvmCheckException(dvmThreadSelf()));
            assert(clazz->status == CLASS_ERROR);
            return NULL;
        }
    }

    return clazz;
}

/*
 * Find the named class (by descriptor), searching for it in the
 * bootclasspath.
 *
 * On failure, this returns NULL with an exception raised.
 */
ClassObject* dvmFindSystemClassNoInit(const char* descriptor)
{
    return findClassNoInit(descriptor, NULL, NULL);
}

/*
 * Find the named class (by descriptor). If it's not already loaded,
 * we load it and link it, but don't execute <clinit>. (The VM has
 * specific limitations on which events can cause initialization.)
 *
 * If "pDexFile" is NULL, we will search the bootclasspath for an entry.
 *
 * On failure, this returns NULL with an exception raised.
 *
 * TODO: we need to return an indication of whether we loaded the class or
 * used an existing definition.  If somebody deliberately tries to load a
 * class twice in the same class loader, they should get a LinkageError,
 * but inadvertent simultaneous class references should "just work".
 */
static ClassObject* findClassNoInit(const char* descriptor, Object* loader,
    DvmDex* pDvmDex)
{
    Thread* self = dvmThreadSelf();
    ClassObject* clazz;
#ifdef WITH_PROFILER
    bool profilerNotified = false;
#endif

    if (loader != NULL) {
        LOGVV("#### findClassNoInit(%s,%p,%p)\n", descriptor, loader,
            pDvmDex->pDexFile);
    }

    /*
     * We don't expect an exception to be raised at this point.  The
     * exception handling code is good about managing this.  This *can*
     * happen if a JNI lookup fails and the JNI code doesn't do any
     * error checking before doing another class lookup, so we may just
     * want to clear this and restore it on exit.  If we don't, some kinds
     * of failures can't be detected without rearranging other stuff.
     *
     * Most often when we hit this situation it means that something is
     * broken in the VM or in JNI code, so I'm keeping it in place (and
     * making it an informative abort rather than an assert).
     */
    if (dvmCheckException(self)) {
        LOGE("Class lookup %s attemped while exception %s pending\n",
            descriptor, dvmGetException(self)->clazz->descriptor);
        dvmDumpAllThreads(false);
        dvmAbort();
    }

    clazz = dvmLookupClass(descriptor, loader, true);
    if (clazz == NULL) {
        const DexClassDef* pClassDef;

#ifdef WITH_PROFILER
        dvmMethodTraceClassPrepBegin();
        profilerNotified = true;
#endif

#if LOG_CLASS_LOADING
        u8 startTime = dvmGetThreadCpuTimeNsec();
#endif

        if (pDvmDex == NULL) {
            assert(loader == NULL);     /* shouldn't be here otherwise */
            pDvmDex = searchBootPathForClass(descriptor, &pClassDef);
        } else {
            pClassDef = dexFindClass(pDvmDex->pDexFile, descriptor);
        }

        if (pDvmDex == NULL || pClassDef == NULL) {
            dvmThrowExceptionWithClassMessage(
                "Ljava/lang/NoClassDefFoundError;", descriptor);
            goto bail;
        }

        /* found a match, try to load it */
        clazz = loadClassFromDex(pDvmDex, pClassDef, loader);
        if (dvmCheckException(self)) {
            /* class was found but had issues */
            dvmReleaseTrackedAlloc((Object*) clazz, NULL);
            goto bail;
        }

        /*
         * Lock the class while we link it so other threads must wait for us
         * to finish.  Set the "initThreadId" so we can identify recursive
         * invocation.
         */
        dvmLockObject(self, (Object*) clazz);
        clazz->initThreadId = self->threadId;

        /*
         * Add to hash table so lookups succeed.
         *
         * [Are circular references possible when linking a class?]
         */
        assert(clazz->classLoader == loader);
        if (!dvmAddClassToHash(clazz)) {
            /*
             * Another thread must have loaded the class after we
             * started but before we finished.  Discard what we've
             * done and leave some hints for the GC.
             *
             * (Yes, this happens.)
             */
            //LOGW("WOW: somebody loaded %s simultaneously\n", descriptor);
            clazz->initThreadId = 0;
            dvmUnlockObject(self, (Object*) clazz);

            /* Let the GC free the class.
             */
            assert(clazz->obj.clazz == gDvm.unlinkedJavaLangClass);
            dvmReleaseTrackedAlloc((Object*) clazz, NULL);

            /* Grab the winning class.
             */
            clazz = dvmLookupClass(descriptor, loader, true);
            assert(clazz != NULL);
            goto got_class;
        }
        dvmReleaseTrackedAlloc((Object*) clazz, NULL);

#if LOG_CLASS_LOADING
        logClassLoadWithTime('>', clazz, startTime);
#endif
        /*
         * Prepare and resolve.
         */
        if (!dvmLinkClass(clazz, false)) {
            assert(dvmCheckException(self));

            /* Make note of the error and clean up the class.
             */
            removeClassFromHash(clazz);
            clazz->status = CLASS_ERROR;
            dvmFreeClassInnards(clazz);

            /* Let any waiters know.
             */
            clazz->initThreadId = 0;
            dvmObjectNotifyAll(self, (Object*) clazz);
            dvmUnlockObject(self, (Object*) clazz);

#if LOG_CLASS_LOADING
            LOG(LOG_INFO, "DVMLINK FAILED FOR CLASS ", "%s in %s\n",
                clazz->descriptor, get_process_name());

            /*
             * TODO: It would probably be better to use a new type code here (instead of '<') to
             * indicate the failure.  This change would require a matching change in the parser
             * and analysis code in frameworks/base/tools/preload.
             */
            logClassLoad('<', clazz);
#endif
            clazz = NULL;
            if (gDvm.optimizing) {
                /* happens with "external" libs */
                LOGV("Link of class '%s' failed\n", descriptor);
            } else {
                LOGW("Link of class '%s' failed\n", descriptor);
            }
            goto bail;
        }
        dvmObjectNotifyAll(self, (Object*) clazz);
        dvmUnlockObject(self, (Object*) clazz);

        /*
         * Add class stats to global counters.
         *
         * TODO: these should probably be atomic ops.
         */
        gDvm.numLoadedClasses++;
        gDvm.numDeclaredMethods +=
            clazz->virtualMethodCount + clazz->directMethodCount;
        gDvm.numDeclaredInstFields += clazz->ifieldCount;
        gDvm.numDeclaredStaticFields += clazz->sfieldCount;

        /*
         * Cache pointers to basic classes.  We want to use these in
         * various places, and it's easiest to initialize them on first
         * use rather than trying to force them to initialize (startup
         * ordering makes it weird).
         */
        if (gDvm.classJavaLangObject == NULL &&
            strcmp(descriptor, "Ljava/lang/Object;") == 0)
        {
            /* It should be impossible to get here with anything
             * but the bootclasspath loader.
             */
            assert(loader == NULL);
            gDvm.classJavaLangObject = clazz;
        }

#if LOG_CLASS_LOADING
        logClassLoad('<', clazz);
#endif

    } else {
got_class:
        if (!dvmIsClassLinked(clazz) && clazz->status != CLASS_ERROR) {
            /*
             * We can race with other threads for class linking.  We should
             * never get here recursively; doing so indicates that two
             * classes have circular dependencies.
             *
             * One exception: we force discovery of java.lang.Class in
             * dvmLinkClass(), and Class has Object as its superclass.  So
             * if the first thing we ever load is Object, we will init
             * Object->Class->Object.  The easiest way to avoid this is to
             * ensure that Object is never the first thing we look up, so
             * we get Foo->Class->Object instead.
             */
            dvmLockObject(self, (Object*) clazz);
            if (!dvmIsClassLinked(clazz) &&
                clazz->initThreadId == self->threadId)
            {
                LOGW("Recursive link on class %s\n", clazz->descriptor);
                dvmUnlockObject(self, (Object*) clazz);
                dvmThrowExceptionWithClassMessage(
                    "Ljava/lang/ClassCircularityError;", clazz->descriptor);
                clazz = NULL;
                goto bail;
            }
            //LOGI("WAITING  for '%s' (owner=%d)\n",
            //    clazz->descriptor, clazz->initThreadId);
            while (!dvmIsClassLinked(clazz) && clazz->status != CLASS_ERROR) {
                dvmObjectWait(self, (Object*) clazz, 0, 0, false);
            }
            dvmUnlockObject(self, (Object*) clazz);
        }
        if (clazz->status == CLASS_ERROR) {
            /*
             * Somebody else tried to load this and failed.  We need to raise
             * an exception and report failure.
             */
            throwEarlierClassFailure(clazz);
            clazz = NULL;
            goto bail;
        }
    }

    /* check some invariants */
    assert(dvmIsClassLinked(clazz));
    assert(gDvm.classJavaLangClass != NULL);
    assert(clazz->obj.clazz == gDvm.classJavaLangClass);
    if (clazz != gDvm.classJavaLangObject) {
        if (clazz->super == NULL) {
            LOGE("Non-Object has no superclass (gDvm.classJavaLangObject=%p)\n",
                gDvm.classJavaLangObject);
            dvmAbort();
        }
    }
    if (!dvmIsInterfaceClass(clazz)) {
        //LOGI("class=%s vtableCount=%d, virtualMeth=%d\n",
        //    clazz->descriptor, clazz->vtableCount,
        //    clazz->virtualMethodCount);
        assert(clazz->vtableCount >= clazz->virtualMethodCount);
    }

    /*
     * Normally class objects are initialized before we instantiate them,
     * but we can't do that with java.lang.Class (chicken, meet egg).  We
     * do it explicitly here.
     *
     * The verifier could call here to find Class while verifying Class,
     * so we need to check for CLASS_VERIFYING as well as !initialized.
     */
    if (clazz == gDvm.classJavaLangClass && !dvmIsClassInitialized(clazz) &&
        !(clazz->status == CLASS_VERIFYING))
    {
        LOGV("+++ explicitly initializing %s\n", clazz->descriptor);
        dvmInitClass(clazz);
    }

bail:
#ifdef WITH_PROFILER
    if (profilerNotified)
        dvmMethodTraceClassPrepEnd();
#endif
    assert(clazz != NULL || dvmCheckException(self));
    return clazz;
}

/*
 * Helper for loadClassFromDex, which takes a DexClassDataHeader and
 * encoded data pointer in addition to the other arguments.
 */
static ClassObject* loadClassFromDex0(DvmDex* pDvmDex,
    const DexClassDef* pClassDef, const DexClassDataHeader* pHeader,
    const u1* pEncodedData, Object* classLoader)
{
    ClassObject* newClass = NULL;
    const DexFile* pDexFile;
    const char* descriptor;
    int i;

    pDexFile = pDvmDex->pDexFile;
    descriptor = dexGetClassDescriptor(pDexFile, pClassDef);

    /*
     * Make sure the aren't any "bonus" flags set, since we use them for
     * runtime state.
     */
    if ((pClassDef->accessFlags & ~EXPECTED_FILE_FLAGS) != 0) {
        LOGW("Invalid file flags in class %s: %04x\n",
            descriptor, pClassDef->accessFlags);
        return NULL;
    }

    /*
     * Allocate storage for the class object on the GC heap, so that other
     * objects can have references to it.  We bypass the usual mechanism
     * (allocObject), because we don't have all the bits and pieces yet.
     *
     * Note that we assume that java.lang.Class does not override
     * finalize().
     */
    newClass = (ClassObject*) dvmMalloc(sizeof(*newClass), ALLOC_DEFAULT);
    if (newClass == NULL)
        return NULL;

    /* Until the class is loaded and linked, use a placeholder
     * obj->clazz value as a hint to the GC.  We don't want
     * the GC trying to scan the object while it's full of Idx
     * values.  Also, the real java.lang.Class may not exist
     * yet.
     */
    DVM_OBJECT_INIT(&newClass->obj, gDvm.unlinkedJavaLangClass);

    dvmSetClassSerialNumber(newClass);
    newClass->descriptor = descriptor;
    assert(newClass->descriptorAlloc == NULL);
    newClass->accessFlags = pClassDef->accessFlags;
    newClass->classLoader = classLoader;
    newClass->pDvmDex = pDvmDex;
    newClass->primitiveType = PRIM_NOT;

    /*
     * Stuff the superclass index into the object pointer field.  The linker
     * pulls it out and replaces it with a resolved ClassObject pointer.
     * I'm doing it this way (rather than having a dedicated superclassIdx
     * field) to save a few bytes of overhead per class.
     *
     * newClass->super is not traversed or freed by dvmFreeClassInnards, so
     * this is safe.
     */
    assert(sizeof(u4) == sizeof(ClassObject*));
    newClass->super = (ClassObject*) pClassDef->superclassIdx;

    /*
     * Stuff class reference indices into the pointer fields.
     *
     * The elements of newClass->interfaces are not traversed or freed by
     * dvmFreeClassInnards, so this is GC-safe.
     */
    const DexTypeList* pInterfacesList;
    pInterfacesList = dexGetInterfacesList(pDexFile, pClassDef);
    if (pInterfacesList != NULL) {
        newClass->interfaceCount = pInterfacesList->size;
        newClass->interfaces = (ClassObject**) dvmLinearAlloc(classLoader,
                newClass->interfaceCount * sizeof(ClassObject*));

        for (i = 0; i < newClass->interfaceCount; i++) {
            const DexTypeItem* pType = dexGetTypeItem(pInterfacesList, i);
            newClass->interfaces[i] = (ClassObject*)(u4) pType->typeIdx;
        }
        dvmLinearReadOnly(classLoader, newClass->interfaces);
    }

    /* load field definitions */

    /*
     * TODO: consider over-allocating the class object and appending the
     * static field info onto the end.  It's fixed-size and known at alloc
     * time.  This would save a couple of native heap allocations, but it
     * would also make heap compaction more difficult because we pass Field
     * pointers around internally.
     */

    if (pHeader->staticFieldsSize != 0) {
        /* static fields stay on system heap; field data isn't "write once" */
        int count = (int) pHeader->staticFieldsSize;
        u4 lastIndex = 0;
        DexField field;

        newClass->sfieldCount = count;
        newClass->sfields =
            (StaticField*) calloc(count, sizeof(StaticField));
        for (i = 0; i < count; i++) {
            dexReadClassDataField(&pEncodedData, &field, &lastIndex);
            loadSFieldFromDex(newClass, &field, &newClass->sfields[i]);
        }
    }

    if (pHeader->instanceFieldsSize != 0) {
        int count = (int) pHeader->instanceFieldsSize;
        u4 lastIndex = 0;
        DexField field;

        newClass->ifieldCount = count;
        newClass->ifields = (InstField*) dvmLinearAlloc(classLoader,
                count * sizeof(InstField));
        for (i = 0; i < count; i++) {
            dexReadClassDataField(&pEncodedData, &field, &lastIndex);
            loadIFieldFromDex(newClass, &field, &newClass->ifields[i]);
        }
        dvmLinearReadOnly(classLoader, newClass->ifields);
    }

    /* load method definitions */

    if (pHeader->directMethodsSize != 0) {
        int count = (int) pHeader->directMethodsSize;
        u4 lastIndex = 0;
        DexMethod method;

        newClass->directMethodCount = count;
        newClass->directMethods = (Method*) dvmLinearAlloc(classLoader,
                count * sizeof(Method));
        for (i = 0; i < count; i++) {
            dexReadClassDataMethod(&pEncodedData, &method, &lastIndex);
            loadMethodFromDex(newClass, &method, &newClass->directMethods[i]);
        }
        dvmLinearReadOnly(classLoader, newClass->directMethods);
    }

    if (pHeader->virtualMethodsSize != 0) {
        int count = (int) pHeader->virtualMethodsSize;
        u4 lastIndex = 0;
        DexMethod method;

        newClass->virtualMethodCount = count;
        newClass->virtualMethods = (Method*) dvmLinearAlloc(classLoader,
                count * sizeof(Method));
        for (i = 0; i < count; i++) {
            dexReadClassDataMethod(&pEncodedData, &method, &lastIndex);
            loadMethodFromDex(newClass, &method, &newClass->virtualMethods[i]);
        }
        dvmLinearReadOnly(classLoader, newClass->virtualMethods);
    }

    newClass->sourceFile = dexGetSourceFile(pDexFile, pClassDef);
    newClass->status = CLASS_LOADED;

    /* caller must call dvmReleaseTrackedAlloc */
    return newClass;
}

/*
 * Try to load the indicated class from the specified DEX file.
 *
 * This is effectively loadClass()+defineClass() for a DexClassDef.  The
 * loading was largely done when we crunched through the DEX.
 *
 * Returns NULL on failure.  If we locate the class but encounter an error
 * while processing it, an appropriate exception is thrown.
 */
static ClassObject* loadClassFromDex(DvmDex* pDvmDex,
    const DexClassDef* pClassDef, Object* classLoader)
{
    ClassObject* result;
    DexClassDataHeader header;
    const u1* pEncodedData;
    const DexFile* pDexFile;

    assert((pDvmDex != NULL) && (pClassDef != NULL));
    pDexFile = pDvmDex->pDexFile;

    if (gDvm.verboseClass) {
        LOGV("CLASS: loading '%s'...\n",
            dexGetClassDescriptor(pDexFile, pClassDef));
    }

    pEncodedData = dexGetClassData(pDexFile, pClassDef);

    if (pEncodedData != NULL) {
        dexReadClassDataHeader(&pEncodedData, &header);
    } else {
        // Provide an all-zeroes header for the rest of the loading.
        memset(&header, 0, sizeof(header));
    }

    result = loadClassFromDex0(pDvmDex, pClassDef, &header, pEncodedData,
            classLoader);

    if (gDvm.verboseClass && (result != NULL)) {
        LOGI("[Loaded %s from DEX %p (cl=%p)]\n",
            result->descriptor, pDvmDex, classLoader);
    }

    return result;
}

/*
 * Free anything in a ClassObject that was allocated on the system heap.
 *
 * The ClassObject itself is allocated on the GC heap, so we leave it for
 * the garbage collector.
 *
 * NOTE: this may be called with a partially-constructed object.
 * NOTE: there is no particular ordering imposed, so don't go poking at
 * superclasses.
 */
void dvmFreeClassInnards(ClassObject* clazz)
{
    void *tp;
    int i;

    if (clazz == NULL)
        return;

    assert(clazz->obj.clazz == gDvm.classJavaLangClass ||
           clazz->obj.clazz == gDvm.unlinkedJavaLangClass);

    /* Guarantee that dvmFreeClassInnards can be called on a given
     * class multiple times by clearing things out as we free them.
     * We don't make any attempt at real atomicity here; higher
     * levels need to make sure that no two threads can free the
     * same ClassObject at the same time.
     *
     * TODO: maybe just make it so the GC will never free the
     * innards of an already-freed class.
     *
     * TODO: this #define isn't MT-safe -- the compiler could rearrange it.
     */
#define NULL_AND_FREE(p) \
    do { \
        if ((p) != NULL) { \
            tp = (p); \
            (p) = NULL; \
            free(tp); \
        } \
    } while (0)
#define NULL_AND_LINEAR_FREE(p) \
    do { \
        if ((p) != NULL) { \
            tp = (p); \
            (p) = NULL; \
            dvmLinearFree(clazz->classLoader, tp); \
        } \
    } while (0)

    /* arrays just point at Object's vtable; don't free vtable in this case.
     * dvmIsArrayClass() checks clazz->descriptor, so we have to do this check
     * before freeing the name.
     */
    clazz->vtableCount = -1;
    if (dvmIsArrayClass(clazz)) {
        clazz->vtable = NULL;
    } else {
        NULL_AND_LINEAR_FREE(clazz->vtable);
    }

    clazz->descriptor = NULL;
    NULL_AND_FREE(clazz->descriptorAlloc);

    if (clazz->directMethods != NULL) {
        Method *directMethods = clazz->directMethods;
        int directMethodCount = clazz->directMethodCount;
        clazz->directMethods = NULL;
        clazz->directMethodCount = -1;
        for (i = 0; i < directMethodCount; i++) {
            freeMethodInnards(&directMethods[i]);
        }
        dvmLinearFree(clazz->classLoader, directMethods);
    }
    if (clazz->virtualMethods != NULL) {
        Method *virtualMethods = clazz->virtualMethods;
        int virtualMethodCount = clazz->virtualMethodCount;
        clazz->virtualMethodCount = -1;
        clazz->virtualMethods = NULL;
        for (i = 0; i < virtualMethodCount; i++) {
            freeMethodInnards(&virtualMethods[i]);
        }
        dvmLinearFree(clazz->classLoader, virtualMethods);
    }

    clazz->initiatingLoaderCount = -1;
    NULL_AND_FREE(clazz->initiatingLoaders);

    clazz->interfaceCount = -1;
    NULL_AND_LINEAR_FREE(clazz->interfaces);

    clazz->iftableCount = -1;
    NULL_AND_LINEAR_FREE(clazz->iftable);

    clazz->ifviPoolCount = -1;
    NULL_AND_LINEAR_FREE(clazz->ifviPool);

    clazz->sfieldCount = -1;
    NULL_AND_FREE(clazz->sfields);

    clazz->ifieldCount = -1;
    NULL_AND_LINEAR_FREE(clazz->ifields);

#undef NULL_AND_FREE
#undef NULL_AND_LINEAR_FREE
}

/*
 * Free anything in a Method that was allocated on the system heap.
 */
static void freeMethodInnards(Method* meth)
{
#if 0
    free(meth->exceptions);
    free(meth->lines);
    free(meth->locals);
#else
    // TODO: call dvmFreeRegisterMap() if meth->registerMap was allocated
    //       on the system heap
    UNUSED_PARAMETER(meth);
#endif
}

/*
 * Clone a Method, making new copies of anything that will be freed up
 * by freeMethodInnards().
 */
static void cloneMethod(Method* dst, const Method* src)
{
    memcpy(dst, src, sizeof(Method));
#if 0
    /* for current usage, these are never set, so no need to implement copy */
    assert(dst->exceptions == NULL);
    assert(dst->lines == NULL);
    assert(dst->locals == NULL);
#endif
}

/*
 * Pull the interesting pieces out of a DexMethod.
 *
 * The DEX file isn't going anywhere, so we don't need to make copies of
 * the code area.
 */
static void loadMethodFromDex(ClassObject* clazz, const DexMethod* pDexMethod,
    Method* meth)
{
    DexFile* pDexFile = clazz->pDvmDex->pDexFile;
    const DexMethodId* pMethodId;
    const DexCode* pDexCode;

    pMethodId = dexGetMethodId(pDexFile, pDexMethod->methodIdx);

    meth->name = dexStringById(pDexFile, pMethodId->nameIdx);
    dexProtoSetFromMethodId(&meth->prototype, pDexFile, pMethodId);
    meth->shorty = dexProtoGetShorty(&meth->prototype);
    meth->accessFlags = pDexMethod->accessFlags;
    meth->clazz = clazz;
    meth->jniArgInfo = 0;

    if (dvmCompareNameDescriptorAndMethod("finalize", "()V", meth) == 0) {
        SET_CLASS_FLAG(clazz, CLASS_ISFINALIZABLE);
    }

    pDexCode = dexGetCode(pDexFile, pDexMethod);
    if (pDexCode != NULL) {
        /* integer constants, copy over for faster access */
        meth->registersSize = pDexCode->registersSize;
        meth->insSize = pDexCode->insSize;
        meth->outsSize = pDexCode->outsSize;

        /* pointer to code area */
        meth->insns = pDexCode->insns;
    } else {
        /*
         * We don't have a DexCode block, but we still want to know how
         * much space is needed for the arguments (so we don't have to
         * compute it later).  We also take this opportunity to compute
         * JNI argument info.
         *
         * We do this for abstract methods as well, because we want to
         * be able to substitute our exception-throwing "stub" in.
         */
        int argsSize = dvmComputeMethodArgsSize(meth);
        if (!dvmIsStaticMethod(meth))
            argsSize++;
        meth->registersSize = meth->insSize = argsSize;
        assert(meth->outsSize == 0);
        assert(meth->insns == NULL);

        if (dvmIsNativeMethod(meth)) {
            meth->nativeFunc = dvmResolveNativeMethod;
            meth->jniArgInfo = computeJniArgInfo(&meth->prototype);
        }
    }
}

/*
 * jniArgInfo (32-bit int) layout:
 *   SRRRHHHH HHHHHHHH HHHHHHHH HHHHHHHH
 *
 *   S - if set, do things the hard way (scan the signature)
 *   R - return-type enumeration
 *   H - target-specific hints
 *
 * This info is used at invocation time by dvmPlatformInvoke.  In most
 * cases, the target-specific hints allow dvmPlatformInvoke to avoid
 * having to fully parse the signature.
 *
 * The return-type bits are always set, even if target-specific hint bits
 * are unavailable.
 */
static int computeJniArgInfo(const DexProto* proto)
{
    const char* sig = dexProtoGetShorty(proto);
    int returnType, padFlags, jniArgInfo;
    char sigByte;
    int stackOffset, padMask;
    u4 hints;

    /* The first shorty character is the return type. */
    switch (*(sig++)) {
    case 'V':
        returnType = DALVIK_JNI_RETURN_VOID;
        break;
    case 'F':
        returnType = DALVIK_JNI_RETURN_FLOAT;
        break;
    case 'D':
        returnType = DALVIK_JNI_RETURN_DOUBLE;
        break;
    case 'J':
        returnType = DALVIK_JNI_RETURN_S8;
        break;
    default:
        returnType = DALVIK_JNI_RETURN_S4;
        break;
    }

    jniArgInfo = returnType << DALVIK_JNI_RETURN_SHIFT;

    hints = dvmPlatformInvokeHints(proto);

    if (hints & DALVIK_JNI_NO_ARG_INFO) {
        jniArgInfo |= DALVIK_JNI_NO_ARG_INFO;
    } else {
        assert((hints & DALVIK_JNI_RETURN_MASK) == 0);
        jniArgInfo |= hints;
    }

    return jniArgInfo;
}

/*
 * Load information about a static field.
 *
 * This also "prepares" static fields by initializing them
 * to their "standard default values".
 */
static void loadSFieldFromDex(ClassObject* clazz,
    const DexField* pDexSField, StaticField* sfield)
{
    DexFile* pDexFile = clazz->pDvmDex->pDexFile;
    const DexFieldId* pFieldId;

    pFieldId = dexGetFieldId(pDexFile, pDexSField->fieldIdx);

    sfield->field.clazz = clazz;
    sfield->field.name = dexStringById(pDexFile, pFieldId->nameIdx);
    sfield->field.signature = dexStringByTypeIdx(pDexFile, pFieldId->typeIdx);
    sfield->field.accessFlags = pDexSField->accessFlags;

    /* Static object field values are set to "standard default values"
     * (null or 0) until the class is initialized.  We delay loading
     * constant values from the class until that time.
     */
    //sfield->value.j = 0;
    assert(sfield->value.j == 0LL);     // cleared earlier with calloc

#ifdef PROFILE_FIELD_ACCESS
    sfield->field.gets = sfield->field.puts = 0;
#endif
}

/*
 * Load information about an instance field.
 */
static void loadIFieldFromDex(ClassObject* clazz,
    const DexField* pDexIField, InstField* ifield)
{
    DexFile* pDexFile = clazz->pDvmDex->pDexFile;
    const DexFieldId* pFieldId;

    pFieldId = dexGetFieldId(pDexFile, pDexIField->fieldIdx);

    ifield->field.clazz = clazz;
    ifield->field.name = dexStringById(pDexFile, pFieldId->nameIdx);
    ifield->field.signature = dexStringByTypeIdx(pDexFile, pFieldId->typeIdx);
    ifield->field.accessFlags = pDexIField->accessFlags;
#ifndef NDEBUG
    assert(ifield->byteOffset == 0);    // cleared earlier with calloc
    ifield->byteOffset = -1;    // make it obvious if we fail to set later
#endif

#ifdef PROFILE_FIELD_ACCESS
    ifield->field.gets = ifield->field.puts = 0;
#endif
}

/*
 * Cache java.lang.ref.Reference fields and methods.
 */
static bool precacheReferenceOffsets(ClassObject *clazz)
{
    Method *meth;
    int i;

    /* We trick the GC object scanner by not counting
     * java.lang.ref.Reference.referent as an object
     * field.  It will get explicitly scanned as part
     * of the reference-walking process.
     *
     * Find the object field named "referent" and put it
     * just after the list of object reference fields.
     */
    dvmLinearReadWrite(clazz->classLoader, clazz->ifields);
    for (i = 0; i < clazz->ifieldRefCount; i++) {
        InstField *pField = &clazz->ifields[i];
        if (strcmp(pField->field.name, "referent") == 0) {
            int targetIndex;

            /* Swap this field with the last object field.
             */
            targetIndex = clazz->ifieldRefCount - 1;
            if (i != targetIndex) {
                InstField *swapField = &clazz->ifields[targetIndex];
                InstField tmpField;
                int tmpByteOffset;

                /* It's not currently strictly necessary
                 * for the fields to be in byteOffset order,
                 * but it's more predictable that way.
                 */
                tmpByteOffset = swapField->byteOffset;
                swapField->byteOffset = pField->byteOffset;
                pField->byteOffset = tmpByteOffset;

                tmpField = *swapField;
                *swapField = *pField;
                *pField = tmpField;
            }

            /* One fewer object field (wink wink).
             */
            clazz->ifieldRefCount--;
            i--;        /* don't trip "didn't find it" test if field was last */
            break;
        }
    }
    dvmLinearReadOnly(clazz->classLoader, clazz->ifields);
    if (i == clazz->ifieldRefCount) {
        LOGE("Unable to reorder 'referent' in %s\n", clazz->descriptor);
        return false;
    }

    /* Cache pretty much everything about Reference so that
     * we don't need to call interpreted code when clearing/enqueueing
     * references.  This is fragile, so we'll be paranoid.
     */
    gDvm.classJavaLangRefReference = clazz;

    gDvm.offJavaLangRefReference_referent =
        dvmFindFieldOffset(gDvm.classJavaLangRefReference,
                "referent", "Ljava/lang/Object;");
    assert(gDvm.offJavaLangRefReference_referent >= 0);

    gDvm.offJavaLangRefReference_queue =
        dvmFindFieldOffset(gDvm.classJavaLangRefReference,
                "queue", "Ljava/lang/ref/ReferenceQueue;");
    assert(gDvm.offJavaLangRefReference_queue >= 0);

    gDvm.offJavaLangRefReference_queueNext =
        dvmFindFieldOffset(gDvm.classJavaLangRefReference,
                "queueNext", "Ljava/lang/ref/Reference;");
    assert(gDvm.offJavaLangRefReference_queueNext >= 0);

    gDvm.offJavaLangRefReference_vmData =
        dvmFindFieldOffset(gDvm.classJavaLangRefReference,
                "vmData", "I");
    assert(gDvm.offJavaLangRefReference_vmData >= 0);

#if FANCY_REFERENCE_SUBCLASS
    meth = dvmFindVirtualMethodByDescriptor(clazz, "clear", "()V");
    assert(meth != NULL);
    gDvm.voffJavaLangRefReference_clear = meth->methodIndex;

    meth = dvmFindVirtualMethodByDescriptor(clazz, "enqueue", "()Z");
    assert(meth != NULL);
    gDvm.voffJavaLangRefReference_enqueue = meth->methodIndex;
#else
    /* enqueueInternal() is private and thus a direct method. */
    meth = dvmFindDirectMethodByDescriptor(clazz, "enqueueInternal", "()Z");
    assert(meth != NULL);
    gDvm.methJavaLangRefReference_enqueueInternal = meth;
#endif

    return true;
}


/*
 * Link (prepare and resolve).  Verification is deferred until later.
 *
 * This converts symbolic references into pointers.  It's independent of
 * the source file format.
 *
 * If "classesResolved" is false, we assume that superclassIdx and
 * interfaces[] are holding class reference indices rather than pointers.
 * The class references will be resolved during link.  (This is done when
 * loading from DEX to avoid having to create additional storage to pass
 * the indices around.)
 *
 * Returns "false" with an exception pending on failure.
 */
bool dvmLinkClass(ClassObject* clazz, bool classesResolved)
{
    u4 superclassIdx = 0;
    bool okay = false;
    bool resolve_okay;
    int numInterfacesResolved = 0;
    int i;

    if (gDvm.verboseClass)
        LOGV("CLASS: linking '%s'...\n", clazz->descriptor);

    /* "Resolve" the class.
     *
     * At this point, clazz's reference fields contain Dex
     * file indices instead of direct object references.
     * We need to translate those indices into real references,
     * while making sure that the GC doesn't sweep any of
     * the referenced objects.
     *
     * The GC will avoid scanning this object as long as
     * clazz->obj.clazz is gDvm.unlinkedJavaLangClass.
     * Once clazz is ready, we'll replace clazz->obj.clazz
     * with gDvm.classJavaLangClass to let the GC know
     * to look at it.
     */
    assert(clazz->obj.clazz == gDvm.unlinkedJavaLangClass);

    /* It's important that we take care of java.lang.Class
     * first.  If we were to do this after looking up the
     * superclass (below), Class wouldn't be ready when
     * java.lang.Object needed it.
     *
     * Note that we don't set clazz->obj.clazz yet.
     */
    if (gDvm.classJavaLangClass == NULL) {
        if (clazz->classLoader == NULL &&
            strcmp(clazz->descriptor, "Ljava/lang/Class;") == 0)
        {
            gDvm.classJavaLangClass = clazz;
        } else {
            gDvm.classJavaLangClass =
                dvmFindSystemClassNoInit("Ljava/lang/Class;");
            if (gDvm.classJavaLangClass == NULL) {
                /* should have thrown one */
                assert(dvmCheckException(dvmThreadSelf()));
                goto bail;
            }
        }
    }
    assert(gDvm.classJavaLangClass != NULL);

    /*
     * Resolve all Dex indices so we can hand the ClassObject
     * over to the GC.  If we fail at any point, we need to remove
     * any tracked references to avoid leaking memory.
     */

    /*
     * All classes have a direct superclass, except for java/lang/Object.
     */
    if (!classesResolved) {
        superclassIdx = (u4) clazz->super;          /* unpack temp store */
        clazz->super = NULL;
    }
    if (strcmp(clazz->descriptor, "Ljava/lang/Object;") == 0) {
        assert(!classesResolved);
        if (superclassIdx != kDexNoIndex) {
            /* TODO: is this invariant true for all java/lang/Objects,
             * regardless of the class loader?  For now, assume it is.
             */
            dvmThrowException("Ljava/lang/ClassFormatError;",
                "java.lang.Object has a superclass");
            goto bail;
        }

        /* Don't finalize objects whose classes use the
         * default (empty) Object.finalize().
         */
        CLEAR_CLASS_FLAG(clazz, CLASS_ISFINALIZABLE);
    } else {
        if (!classesResolved) {
            if (superclassIdx == kDexNoIndex) {
                dvmThrowException("Ljava/lang/LinkageError;",
                    "no superclass defined");
                goto bail;
            }
            clazz->super = dvmResolveClass(clazz, superclassIdx, false);
            if (clazz->super == NULL) {
                assert(dvmCheckException(dvmThreadSelf()));
                if (gDvm.optimizing) {
                    /* happens with "external" libs */
                    LOGV("Unable to resolve superclass of %s (%d)\n",
                        clazz->descriptor, superclassIdx);
                } else {
                    LOGW("Unable to resolve superclass of %s (%d)\n",
                        clazz->descriptor, superclassIdx);
                }
                goto bail;
            }
        }
        /* verify */
        if (dvmIsFinalClass(clazz->super)) {
            LOGW("Superclass of '%s' is final '%s'\n",
                clazz->descriptor, clazz->super->descriptor);
            dvmThrowException("Ljava/lang/IncompatibleClassChangeError;",
                "superclass is final");
            goto bail;
        } else if (dvmIsInterfaceClass(clazz->super)) {
            LOGW("Superclass of '%s' is interface '%s'\n",
                clazz->descriptor, clazz->super->descriptor);
            dvmThrowException("Ljava/lang/IncompatibleClassChangeError;",
                "superclass is an interface");
            goto bail;
        } else if (!dvmCheckClassAccess(clazz, clazz->super)) {
            LOGW("Superclass of '%s' (%s) is not accessible\n",
                clazz->descriptor, clazz->super->descriptor);
            dvmThrowException("Ljava/lang/IllegalAccessError;",
                "superclass not accessible");
            goto bail;
        }

        /* Don't let the GC reclaim the superclass.
         * TODO: shouldn't be needed; remove when things stabilize
         */
        dvmAddTrackedAlloc((Object *)clazz->super, NULL);

        /* Inherit finalizability from the superclass.  If this
         * class also overrides finalize(), its CLASS_ISFINALIZABLE
         * bit will already be set.
         */
        if (IS_CLASS_FLAG_SET(clazz->super, CLASS_ISFINALIZABLE)) {
            SET_CLASS_FLAG(clazz, CLASS_ISFINALIZABLE);
        }

        /* See if this class descends from java.lang.Reference
         * and set the class flags appropriately.
         */
        if (IS_CLASS_FLAG_SET(clazz->super, CLASS_ISREFERENCE)) {
            u4 superRefFlags;

            /* We've already determined the reference type of this
             * inheritance chain.  Inherit reference-ness from the superclass.
             */
            superRefFlags = GET_CLASS_FLAG_GROUP(clazz->super,
                    CLASS_ISREFERENCE |
                    CLASS_ISWEAKREFERENCE |
                    CLASS_ISPHANTOMREFERENCE);
            SET_CLASS_FLAG(clazz, superRefFlags);
        } else if (clazz->classLoader == NULL &&
                clazz->super->classLoader == NULL &&
                strcmp(clazz->super->descriptor,
                       "Ljava/lang/ref/Reference;") == 0)
        {
            u4 refFlags;

            /* This class extends Reference, which means it should
             * be one of the magic Soft/Weak/PhantomReference classes.
             */
            refFlags = CLASS_ISREFERENCE;
            if (strcmp(clazz->descriptor,
                       "Ljava/lang/ref/SoftReference;") == 0)
            {
                /* Only CLASS_ISREFERENCE is set for soft references.
                 */
            } else if (strcmp(clazz->descriptor,
                       "Ljava/lang/ref/WeakReference;") == 0)
            {
                refFlags |= CLASS_ISWEAKREFERENCE;
            } else if (strcmp(clazz->descriptor,
                       "Ljava/lang/ref/PhantomReference;") == 0)
            {
                refFlags |= CLASS_ISPHANTOMREFERENCE;
            } else {
                /* No-one else is allowed to inherit directly
                 * from Reference.
                 */
//xxx is this the right exception?  better than an assertion.
                dvmThrowException("Ljava/lang/LinkageError;",
                    "illegal inheritance from Reference");
                goto bail;
            }

            /* The class should not have any reference bits set yet.
             */
            assert(GET_CLASS_FLAG_GROUP(clazz,
                    CLASS_ISREFERENCE |
                    CLASS_ISWEAKREFERENCE |
                    CLASS_ISPHANTOMREFERENCE) == 0);

            SET_CLASS_FLAG(clazz, refFlags);
        }
    }

    if (!classesResolved && clazz->interfaceCount > 0) {
        /*
         * Resolve the interfaces implemented directly by this class.  We
         * stuffed the class index into the interface pointer slot.
         */
        dvmLinearReadWrite(clazz->classLoader, clazz->interfaces);
        for (i = 0; i < clazz->interfaceCount; i++) {
            u4 interfaceIdx;

            interfaceIdx = (u4) clazz->interfaces[i];   /* unpack temp store */
            assert(interfaceIdx != kDexNoIndex);

            clazz->interfaces[i] = dvmResolveClass(clazz, interfaceIdx, false);
            if (clazz->interfaces[i] == NULL) {
                const DexFile* pDexFile = clazz->pDvmDex->pDexFile;

                assert(dvmCheckException(dvmThreadSelf()));
                dvmLinearReadOnly(clazz->classLoader, clazz->interfaces);

                const char* classDescriptor;
                classDescriptor = dexStringByTypeIdx(pDexFile, interfaceIdx);
                if (gDvm.optimizing) {
                    /* happens with "external" libs */
                    LOGV("Failed resolving %s interface %d '%s'\n",
                        clazz->descriptor, interfaceIdx, classDescriptor);
                } else {
                    LOGI("Failed resolving %s interface %d '%s'\n",
                        clazz->descriptor, interfaceIdx, classDescriptor);
                }
                goto bail_during_resolve;
            }

            /* are we allowed to implement this interface? */
            if (!dvmCheckClassAccess(clazz, clazz->interfaces[i])) {
                dvmLinearReadOnly(clazz->classLoader, clazz->interfaces);
                LOGW("Interface '%s' is not accessible to '%s'\n",
                    clazz->interfaces[i]->descriptor, clazz->descriptor);
                dvmThrowException("Ljava/lang/IllegalAccessError;",
                    "interface not accessible");
                goto bail_during_resolve;
            }

            /* Don't let the GC reclaim the interface class.
             * TODO: shouldn't be needed; remove when things stabilize
             */
            dvmAddTrackedAlloc((Object *)clazz->interfaces[i], NULL);
            numInterfacesResolved++;

            LOGVV("+++  found interface '%s'\n",
                clazz->interfaces[i]->descriptor);
        }
        dvmLinearReadOnly(clazz->classLoader, clazz->interfaces);
    }

    /*
     * The ClassObject is now in a GC-able state.  We let the GC
     * realize this by punching in the real class type, which is
     * always java.lang.Class.
     *
     * After this line, clazz will be fair game for the GC.
     * Every field that the GC will look at must now be valid:
     * - clazz->super
     * - class->classLoader
     * - clazz->sfields
     * - clazz->interfaces
     */
    clazz->obj.clazz = gDvm.classJavaLangClass;

    if (false) {
bail_during_resolve:
        resolve_okay = false;
    } else {
        resolve_okay = true;
    }

    /*
     * Now that the GC can scan the ClassObject, we can let
     * go of the explicit references we were holding onto.
     *
     * Either that or we failed, in which case we need to
     * release the references so we don't leak memory.
     */
    if (clazz->super != NULL) {
        dvmReleaseTrackedAlloc((Object *)clazz->super, NULL);
    }
    for (i = 0; i < numInterfacesResolved; i++) {
        dvmReleaseTrackedAlloc((Object *)clazz->interfaces[i], NULL);
    }

    if (!resolve_okay) {
        //LOGW("resolve_okay is false\n");
        goto bail;
    }

    /*
     * Populate vtable.
     */
    if (dvmIsInterfaceClass(clazz)) {
        /* no vtable; just set the method indices */
        int count = clazz->virtualMethodCount;

        if (count != (u2) count) {
            LOGE("Too many methods (%d) in interface '%s'\n", count,
                 clazz->descriptor);
            goto bail;
        }

        dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);

        for (i = 0; i < count; i++)
            clazz->virtualMethods[i].methodIndex = (u2) i;

        dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
    } else {
        if (!createVtable(clazz)) {
            LOGW("failed creating vtable\n");
            goto bail;
        }
    }

    /*
     * Populate interface method tables.  Can alter the vtable.
     */
    if (!createIftable(clazz))
        goto bail;

    /*
     * Insert special-purpose "stub" method implementations.
     */
    if (!insertMethodStubs(clazz))
        goto bail;

    /*
     * Compute instance field offsets and, hence, the size of the object.
     */
    if (!computeFieldOffsets(clazz))
        goto bail;

    /*
     * Cache fields and methods from java/lang/ref/Reference and
     * java/lang/Class.  This has to happen after computeFieldOffsets().
     */
    if (clazz->classLoader == NULL) {
        if (strcmp(clazz->descriptor, "Ljava/lang/ref/Reference;") == 0) {
            if (!precacheReferenceOffsets(clazz)) {
                LOGE("failed pre-caching Reference offsets\n");
                dvmThrowException("Ljava/lang/InternalError;", NULL);
                goto bail;
            }
        } else if (clazz == gDvm.classJavaLangClass) {
            gDvm.offJavaLangClass_pd = dvmFindFieldOffset(clazz, "pd",
                "Ljava/security/ProtectionDomain;");
            if (gDvm.offJavaLangClass_pd <= 0) {
                LOGE("ERROR: unable to find 'pd' field in Class\n");
                dvmAbort();     /* we're not going to get much farther */
                //goto bail;
            }
        }
    }

    /*
     * Done!
     */
    if (IS_CLASS_FLAG_SET(clazz, CLASS_ISPREVERIFIED))
        clazz->status = CLASS_VERIFIED;
    else
        clazz->status = CLASS_RESOLVED;
    okay = true;
    if (gDvm.verboseClass)
        LOGV("CLASS: linked '%s'\n", clazz->descriptor);

    /*
     * We send CLASS_PREPARE events to the debugger from here.  The
     * definition of "preparation" is creating the static fields for a
     * class and initializing them to the standard default values, but not
     * executing any code (that comes later, during "initialization").
     *
     * We did the static prep in loadSFieldFromDex() while loading the class.
     *
     * The class has been prepared and resolved but possibly not yet verified
     * at this point.
     */
    if (gDvm.debuggerActive) {
        dvmDbgPostClassPrepare(clazz);
    }

bail:
    if (!okay) {
        clazz->status = CLASS_ERROR;
        if (!dvmCheckException(dvmThreadSelf())) {
            dvmThrowException("Ljava/lang/VirtualMachineError;", NULL);
        }
    }
    return okay;
}

/*
 * Create the virtual method table.
 *
 * The top part of the table is a copy of the table from our superclass,
 * with our local methods overriding theirs.  The bottom part of the table
 * has any new methods we defined.
 */
static bool createVtable(ClassObject* clazz)
{
    bool result = false;
    int maxCount;
    int i;

    if (clazz->super != NULL) {
        //LOGI("SUPER METHODS %d %s->%s\n", clazz->super->vtableCount,
        //    clazz->descriptor, clazz->super->descriptor);
    }

    /* the virtual methods we define, plus the superclass vtable size */
    maxCount = clazz->virtualMethodCount;
    if (clazz->super != NULL) {
        maxCount += clazz->super->vtableCount;
    } else {
        /* TODO: is this invariant true for all java/lang/Objects,
         * regardless of the class loader?  For now, assume it is.
         */
        assert(strcmp(clazz->descriptor, "Ljava/lang/Object;") == 0);
    }
    //LOGD("+++ max vmethods for '%s' is %d\n", clazz->descriptor, maxCount);

    /*
     * Over-allocate the table, then realloc it down if necessary.  So
     * long as we don't allocate anything in between we won't cause
     * fragmentation, and reducing the size should be unlikely to cause
     * a buffer copy.
     */
    dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);
    clazz->vtable = (Method**) dvmLinearAlloc(clazz->classLoader,
                        sizeof(Method*) * maxCount);
    if (clazz->vtable == NULL)
        goto bail;

    if (clazz->super != NULL) {
        int actualCount;

        memcpy(clazz->vtable, clazz->super->vtable,
            sizeof(*(clazz->vtable)) * clazz->super->vtableCount);
        actualCount = clazz->super->vtableCount;

        /*
         * See if any of our virtual methods override the superclass.
         */
        for (i = 0; i < clazz->virtualMethodCount; i++) {
            Method* localMeth = &clazz->virtualMethods[i];
            int si;

            for (si = 0; si < clazz->super->vtableCount; si++) {
                Method* superMeth = clazz->vtable[si];

                if (dvmCompareMethodNamesAndProtos(localMeth, superMeth) == 0)
                {
                    /* verify */
                    if (dvmIsFinalMethod(superMeth)) {
                        LOGW("Method %s.%s overrides final %s.%s\n",
                            localMeth->clazz->descriptor, localMeth->name,
                            superMeth->clazz->descriptor, superMeth->name);
                        goto bail;
                    }
                    clazz->vtable[si] = localMeth;
                    localMeth->methodIndex = (u2) si;
                    //LOGV("+++   override %s.%s (slot %d)\n",
                    //    clazz->descriptor, localMeth->name, si);
                    break;
                }
            }

            if (si == clazz->super->vtableCount) {
                /* not an override, add to end */
                clazz->vtable[actualCount] = localMeth;
                localMeth->methodIndex = (u2) actualCount;
                actualCount++;

                //LOGV("+++   add method %s.%s\n",
                //    clazz->descriptor, localMeth->name);
            }
        }

        if (actualCount != (u2) actualCount) {
            LOGE("Too many methods (%d) in class '%s'\n", actualCount,
                 clazz->descriptor);
            goto bail;
        }

        assert(actualCount <= maxCount);

        if (actualCount < maxCount) {
            assert(clazz->vtable != NULL);
            dvmLinearReadOnly(clazz->classLoader, clazz->vtable);
            clazz->vtable = dvmLinearRealloc(clazz->classLoader, clazz->vtable,
                sizeof(*(clazz->vtable)) * actualCount);
            if (clazz->vtable == NULL) {
                LOGE("vtable realloc failed\n");
                goto bail;
            } else {
                LOGVV("+++  reduced vtable from %d to %d\n",
                    maxCount, actualCount);
            }
        }

        clazz->vtableCount = actualCount;
    } else {
        /* java/lang/Object case */
        int count = clazz->virtualMethodCount;
        if (count != (u2) count) {
            LOGE("Too many methods (%d) in base class '%s'\n", count,
                 clazz->descriptor);
            goto bail;
        }

        for (i = 0; i < count; i++) {
            clazz->vtable[i] = &clazz->virtualMethods[i];
            clazz->virtualMethods[i].methodIndex = (u2) i;
        }
        clazz->vtableCount = clazz->virtualMethodCount;
    }

    result = true;

bail:
    dvmLinearReadOnly(clazz->classLoader, clazz->vtable);
    dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
    return result;
}

/*
 * Create and populate "iftable".
 *
 * The set of interfaces we support is the combination of the interfaces
 * we implement directly and those implemented by our superclass.  Each
 * interface can have one or more "superinterfaces", which we must also
 * support.  For speed we flatten the tree out.
 *
 * We might be able to speed this up when there are lots of interfaces
 * by merge-sorting the class pointers and binary-searching when removing
 * duplicates.  We could also drop the duplicate removal -- it's only
 * there to reduce the memory footprint.
 *
 * Because of "Miranda methods", this may reallocate clazz->virtualMethods.
 *
 * Returns "true" on success.
 */
static bool createIftable(ClassObject* clazz)
{
    bool result = false;
    bool zapIftable = false;
    bool zapVtable = false;
    bool zapIfvipool = false;
    int ifCount, superIfCount, idx;
    int i;

    if (clazz->super != NULL)
        superIfCount = clazz->super->iftableCount;
    else
        superIfCount = 0;

    ifCount = superIfCount;
    ifCount += clazz->interfaceCount;
    for (i = 0; i < clazz->interfaceCount; i++)
        ifCount += clazz->interfaces[i]->iftableCount;

    LOGVV("INTF: class '%s' direct w/supra=%d super=%d total=%d\n",
        clazz->descriptor, ifCount - superIfCount, superIfCount, ifCount);

    if (ifCount == 0) {
        assert(clazz->iftableCount == 0);
        assert(clazz->iftable == NULL);
        result = true;
        goto bail;
    }

    /*
     * Create a table with enough space for all interfaces, and copy the
     * superclass' table in.
     */
    clazz->iftable = (InterfaceEntry*) dvmLinearAlloc(clazz->classLoader,
                        sizeof(InterfaceEntry) * ifCount);
    zapIftable = true;
    memset(clazz->iftable, 0x00, sizeof(InterfaceEntry) * ifCount);
    if (superIfCount != 0) {
        memcpy(clazz->iftable, clazz->super->iftable,
            sizeof(InterfaceEntry) * superIfCount);
    }

    /*
     * Create a flattened interface hierarchy of our immediate interfaces.
     */
    idx = superIfCount;

    for (i = 0; i < clazz->interfaceCount; i++) {
        ClassObject* interf;
        int j;

        interf = clazz->interfaces[i];
        assert(interf != NULL);

        /* make sure this is still an interface class */
        if (!dvmIsInterfaceClass(interf)) {
            LOGW("Class '%s' implements non-interface '%s'\n",
                clazz->descriptor, interf->descriptor);
            dvmThrowExceptionWithClassMessage(
                "Ljava/lang/IncompatibleClassChangeError;",
                clazz->descriptor);
            goto bail;
        }

        /* add entry for this interface */
        clazz->iftable[idx++].clazz = interf;

        /* add entries for the interface's superinterfaces */
        for (j = 0; j < interf->iftableCount; j++) {
            clazz->iftable[idx++].clazz = interf->iftable[j].clazz;
        }
    }

    assert(idx == ifCount);

    if (false) {
        /*
         * Remove anything redundant from our recent additions.  Note we have
         * to traverse the recent adds when looking for duplicates, because
         * it's possible the recent additions are self-redundant.  This
         * reduces the memory footprint of classes with lots of inherited
         * interfaces.
         *
         * (I don't know if this will cause problems later on when we're trying
         * to find a static field.  It looks like the proper search order is
         * (1) current class, (2) interfaces implemented by current class,
         * (3) repeat with superclass.  A field implemented by an interface
         * and by a superclass might come out wrong if the superclass also
         * implements the interface.  The javac compiler will reject the
         * situation as ambiguous, so the concern is somewhat artificial.)
         *
         * UPDATE: this makes ReferenceType.Interfaces difficult to implement,
         * because it wants to return just the interfaces declared to be
         * implemented directly by the class.  I'm excluding this code for now.
         */
        for (i = superIfCount; i < ifCount; i++) {
            int j;

            for (j = 0; j < ifCount; j++) {
                if (i == j)
                    continue;
                if (clazz->iftable[i].clazz == clazz->iftable[j].clazz) {
                    LOGVV("INTF: redundant interface %s in %s\n",
                        clazz->iftable[i].clazz->descriptor,
                        clazz->descriptor);

                    if (i != ifCount-1)
                        memmove(&clazz->iftable[i], &clazz->iftable[i+1],
                            (ifCount - i -1) * sizeof(InterfaceEntry));
                    ifCount--;
                    i--;        // adjust for i++ above
                    break;
                }
            }
        }
        LOGVV("INTF: class '%s' nodupes=%d\n", clazz->descriptor, ifCount);
    } // if (false)

    clazz->iftableCount = ifCount;

    /*
     * If we're an interface, we don't need the vtable pointers, so
     * we're done.  If this class doesn't implement an interface that our
     * superclass doesn't have, then we again have nothing to do.
     */
    if (dvmIsInterfaceClass(clazz) || superIfCount == ifCount) {
        //dvmDumpClass(clazz, kDumpClassFullDetail);
        result = true;
        goto bail;
    }

    /*
     * When we're handling invokeinterface, we probably have an object
     * whose type is an interface class rather than a concrete class.  We
     * need to convert the method reference into a vtable index.  So, for
     * every entry in "iftable", we create a list of vtable indices.
     *
     * Because our vtable encompasses the superclass vtable, we can use
     * the vtable indices from our superclass for all of the interfaces
     * that weren't directly implemented by us.
     *
     * Each entry in "iftable" has a pointer to the start of its set of
     * vtable offsets.  The iftable entries in the superclass point to
     * storage allocated in the superclass, and the iftable entries added
     * for this class point to storage allocated in this class.  "iftable"
     * is flat for fast access in a class and all of its subclasses, but
     * "ifviPool" is only created for the topmost implementor.
     */
    int poolSize = 0;
    for (i = superIfCount; i < ifCount; i++) {
        /*
         * Note it's valid for an interface to have no methods (e.g.
         * java/io/Serializable).
         */
        LOGVV("INTF: pool: %d from %s\n",
            clazz->iftable[i].clazz->virtualMethodCount,
            clazz->iftable[i].clazz->descriptor);
        poolSize += clazz->iftable[i].clazz->virtualMethodCount;
    }

    if (poolSize == 0) {
        LOGVV("INTF: didn't find any new interfaces with methods\n");
        result = true;
        goto bail;
    }

    clazz->ifviPoolCount = poolSize;
    clazz->ifviPool = (int*) dvmLinearAlloc(clazz->classLoader,
                        poolSize * sizeof(int*));
    zapIfvipool = true;

    /*
     * Fill in the vtable offsets for the interfaces that weren't part of
     * our superclass.
     */
    int poolOffset = 0;
    Method** mirandaList = NULL;
    int mirandaCount = 0, mirandaAlloc = 0;

    for (i = superIfCount; i < ifCount; i++) {
        ClassObject* interface;
        int methIdx;

        clazz->iftable[i].methodIndexArray = clazz->ifviPool + poolOffset;
        interface = clazz->iftable[i].clazz;
        poolOffset += interface->virtualMethodCount;    // end here

        /*
         * For each method listed in the interface's method list, find the
         * matching method in our class's method list.  We want to favor the
         * subclass over the superclass, which just requires walking
         * back from the end of the vtable.  (This only matters if the
         * superclass defines a private method and this class redefines
         * it -- otherwise it would use the same vtable slot.  In Dalvik
         * those don't end up in the virtual method table, so it shouldn't
         * matter which direction we go.  We walk it backward anyway.)
         *
         *
         * Suppose we have the following arrangement:
         *   public interface MyInterface
         *     public boolean inInterface();
         *   public abstract class MirandaAbstract implements MirandaInterface
         *     //public abstract boolean inInterface(); // not declared!
         *     public boolean inAbstract() { stuff }    // in vtable
         *   public class MirandClass extends MirandaAbstract
         *     public boolean inInterface() { stuff }
         *     public boolean inAbstract() { stuff }    // in vtable
         *
         * The javac compiler happily compiles MirandaAbstract even though
         * it doesn't declare all methods from its interface.  When we try
         * to set up a vtable for MirandaAbstract, we find that we don't
         * have an slot for inInterface.  To prevent this, we synthesize
         * abstract method declarations in MirandaAbstract.
         *
         * We have to expand vtable and update some things that point at it,
         * so we accumulate the method list and do it all at once below.
         */
        for (methIdx = 0; methIdx < interface->virtualMethodCount; methIdx++) {
            Method* imeth = &interface->virtualMethods[methIdx];
            int j;

            IF_LOGVV() {
                char* desc = dexProtoCopyMethodDescriptor(&imeth->prototype);
                LOGVV("INTF:  matching '%s' '%s'\n", imeth->name, desc);
                free(desc);
            }

            for (j = clazz->vtableCount-1; j >= 0; j--) {
                if (dvmCompareMethodNamesAndProtos(imeth, clazz->vtable[j])
                    == 0)
                {
                    LOGVV("INTF:   matched at %d\n", j);
                    if (!dvmIsPublicMethod(clazz->vtable[j])) {
                        LOGW("Implementation of %s.%s is not public\n",
                            clazz->descriptor, clazz->vtable[j]->name);
                        dvmThrowException("Ljava/lang/IllegalAccessError;",
                            "interface implementation not public");
                        goto bail;
                    }
                    clazz->iftable[i].methodIndexArray[methIdx] = j;
                    break;
                }
            }
            if (j < 0) {
                IF_LOGV() {
                    char* desc =
                        dexProtoCopyMethodDescriptor(&imeth->prototype);
                    LOGV("No match for '%s' '%s' in '%s' (creating miranda)\n",
                            imeth->name, desc, clazz->descriptor);
                    free(desc);
                }
                //dvmThrowException("Ljava/lang/RuntimeException;", "Miranda!");
                //return false;

                if (mirandaCount == mirandaAlloc) {
                    mirandaAlloc += 8;
                    if (mirandaList == NULL) {
                        mirandaList = dvmLinearAlloc(clazz->classLoader,
                                        mirandaAlloc * sizeof(Method*));
                    } else {
                        dvmLinearReadOnly(clazz->classLoader, mirandaList);
                        mirandaList = dvmLinearRealloc(clazz->classLoader,
                                mirandaList, mirandaAlloc * sizeof(Method*));
                    }
                    assert(mirandaList != NULL);    // mem failed + we leaked
                }

                /*
                 * These may be redundant (e.g. method with same name and
                 * signature declared in two interfaces implemented by the
                 * same abstract class).  We can squeeze the duplicates
                 * out here.
                 */
                int mir;
                for (mir = 0; mir < mirandaCount; mir++) {
                    if (dvmCompareMethodNamesAndProtos(
                            mirandaList[mir], imeth) == 0)
                    {
                        IF_LOGVV() {
                            char* desc = dexProtoCopyMethodDescriptor(
                                    &imeth->prototype);
                            LOGVV("MIRANDA dupe: %s and %s %s%s\n",
                                mirandaList[mir]->clazz->descriptor,
                                imeth->clazz->descriptor,
                                imeth->name, desc);
                            free(desc);
                        }
                        break;
                    }
                }

                /* point the iftable at a phantom slot index */
                clazz->iftable[i].methodIndexArray[methIdx] =
                    clazz->vtableCount + mir;
                LOGVV("MIRANDA: %s points at slot %d\n",
                    imeth->name, clazz->vtableCount + mir);

                /* if non-duplicate among Mirandas, add to Miranda list */
                if (mir == mirandaCount) {
                    //LOGV("MIRANDA: holding '%s' in slot %d\n",
                    //    imeth->name, mir);
                    mirandaList[mirandaCount++] = imeth;
                }
            }
        }
    }

    if (mirandaCount != 0) {
        Method* newVirtualMethods;
        Method* meth;
        int oldMethodCount, oldVtableCount;

        for (i = 0; i < mirandaCount; i++) {
            LOGVV("MIRANDA %d: %s.%s\n", i,
                mirandaList[i]->clazz->descriptor, mirandaList[i]->name);
        }

        /*
         * We found methods in one or more interfaces for which we do not
         * have vtable entries.  We have to expand our virtualMethods
         * table (which might be empty) to hold some new entries.
         */
        if (clazz->virtualMethods == NULL) {
            newVirtualMethods = (Method*) dvmLinearAlloc(clazz->classLoader,
                sizeof(Method) * (clazz->virtualMethodCount + mirandaCount));
        } else {
            //dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
            newVirtualMethods = (Method*) dvmLinearRealloc(clazz->classLoader,
                clazz->virtualMethods,
                sizeof(Method) * (clazz->virtualMethodCount + mirandaCount));
        }
        if (newVirtualMethods != clazz->virtualMethods) {
            /*
             * Table was moved in memory.  We have to run through the
             * vtable and fix the pointers.  The vtable entries might be
             * pointing at superclasses, so we flip it around: run through
             * all locally-defined virtual methods, and fix their entries
             * in the vtable.  (This would get really messy if sub-classes
             * had already been loaded.)
             *
             * Reminder: clazz->virtualMethods and clazz->virtualMethodCount
             * hold the virtual methods declared by this class.  The
             * method's methodIndex is the vtable index, and is the same
             * for all sub-classes (and all super classes in which it is
             * defined).  We're messing with these because the Miranda
             * stuff makes it look like the class actually has an abstract
             * method declaration in it.
             */
            LOGVV("MIRANDA fixing vtable pointers\n");
            dvmLinearReadWrite(clazz->classLoader, clazz->vtable);
            Method* meth = newVirtualMethods;
            for (i = 0; i < clazz->virtualMethodCount; i++, meth++)
                clazz->vtable[meth->methodIndex] = meth;
            dvmLinearReadOnly(clazz->classLoader, clazz->vtable);
        }

        oldMethodCount = clazz->virtualMethodCount;
        clazz->virtualMethods = newVirtualMethods;
        clazz->virtualMethodCount += mirandaCount;

        dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);

        /*
         * We also have to expand the vtable.
         */
        assert(clazz->vtable != NULL);
        clazz->vtable = (Method**) dvmLinearRealloc(clazz->classLoader,
                        clazz->vtable,
                        sizeof(Method*) * (clazz->vtableCount + mirandaCount));
        if (clazz->vtable == NULL) {
            assert(false);
            goto bail;
        }
        zapVtable = true;

        oldVtableCount = clazz->vtableCount;
        clazz->vtableCount += mirandaCount;

        /*
         * Now we need to create the fake methods.  We clone the abstract
         * method definition from the interface and then replace a few
         * things.
         */
        meth = clazz->virtualMethods + oldMethodCount;
        for (i = 0; i < mirandaCount; i++, meth++) {
            dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);
            cloneMethod(meth, mirandaList[i]);
            meth->clazz = clazz;
            meth->accessFlags |= ACC_MIRANDA;
            meth->methodIndex = (u2) (oldVtableCount + i);
            dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);

            /* point the new vtable entry at the new method */
            clazz->vtable[oldVtableCount + i] = meth;
        }

        dvmLinearReadOnly(clazz->classLoader, mirandaList);
        dvmLinearFree(clazz->classLoader, mirandaList);

    }

    /*
     * TODO?
     * Sort the interfaces by number of declared methods.  All we really
     * want is to get the interfaces with zero methods at the end of the
     * list, so that when we walk through the list during invoke-interface
     * we don't examine interfaces that can't possibly be useful.
     *
     * The set will usually be small, so a simple insertion sort works.
     *
     * We have to be careful not to change the order of two interfaces
     * that define the same method.  (Not a problem if we only move the
     * zero-method interfaces to the end.)
     *
     * PROBLEM:
     * If we do this, we will no longer be able to identify super vs.
     * current class interfaces by comparing clazz->super->iftableCount.  This
     * breaks anything that only wants to find interfaces declared directly
     * by the class (dvmFindStaticFieldHier, ReferenceType.Interfaces,
     * dvmDbgOutputAllInterfaces, etc).  Need to provide a workaround.
     *
     * We can sort just the interfaces implemented directly by this class,
     * but that doesn't seem like it would provide much of an advantage.  I'm
     * not sure this is worthwhile.
     *
     * (This has been made largely obsolete by the interface cache mechanism.)
     */

    //dvmDumpClass(clazz);

    result = true;

bail:
    if (zapIftable)
        dvmLinearReadOnly(clazz->classLoader, clazz->iftable);
    if (zapVtable)
        dvmLinearReadOnly(clazz->classLoader, clazz->vtable);
    if (zapIfvipool)
        dvmLinearReadOnly(clazz->classLoader, clazz->ifviPool);
    return result;
}


/*
 * Provide "stub" implementations for methods without them.
 *
 * Currently we provide an implementation for all abstract methods that
 * throws an AbstractMethodError exception.  This allows us to avoid an
 * explicit check for abstract methods in every virtual call.
 *
 * NOTE: for Miranda methods, the method declaration is a clone of what
 * was found in the interface class.  That copy may already have had the
 * function pointer filled in, so don't be surprised if it's not NULL.
 *
 * NOTE: this sets the "native" flag, giving us an "abstract native" method,
 * which is nonsensical.  Need to make sure that this doesn't escape the
 * VM.  We can either mask it out in reflection calls, or copy "native"
 * into the high 16 bits of accessFlags and check that internally.
 */
static bool insertMethodStubs(ClassObject* clazz)
{
    dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);

    Method* meth;
    int i;

    meth = clazz->virtualMethods;
    for (i = 0; i < clazz->virtualMethodCount; i++, meth++) {
        if (dvmIsAbstractMethod(meth)) {
            assert(meth->insns == NULL);
            assert(meth->nativeFunc == NULL ||
                meth->nativeFunc == (DalvikBridgeFunc)dvmAbstractMethodStub);

            meth->accessFlags |= ACC_NATIVE;
            meth->nativeFunc = (DalvikBridgeFunc) dvmAbstractMethodStub;
        }
    }

    dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
    return true;
}


/*
 * Swap two instance fields.
 */
static inline void swapField(InstField* pOne, InstField* pTwo)
{
    InstField swap;

    LOGVV("  --- swap '%s' and '%s'\n", pOne->field.name, pTwo->field.name);
    swap = *pOne;
    *pOne = *pTwo;
    *pTwo = swap;
}

/*
 * Assign instance fields to u4 slots.
 *
 * The top portion of the instance field area is occupied by the superclass
 * fields, the bottom by the fields for this class.
 *
 * "long" and "double" fields occupy two adjacent slots.  On some
 * architectures, 64-bit quantities must be 64-bit aligned, so we need to
 * arrange fields (or introduce padding) to ensure this.  We assume the
 * fields of the topmost superclass (i.e. Object) are 64-bit aligned, so
 * we can just ensure that the offset is "even".  To avoid wasting space,
 * we want to move non-reference 32-bit fields into gaps rather than
 * creating pad words.
 *
 * In the worst case we will waste 4 bytes, but because objects are
 * allocated on >= 64-bit boundaries, those bytes may well be wasted anyway
 * (assuming this is the most-derived class).
 *
 * Pad words are not represented in the field table, so the field table
 * itself does not change size.
 *
 * The number of field slots determines the size of the object, so we
 * set that here too.
 *
 * This function feels a little more complicated than I'd like, but it
 * has the property of moving the smallest possible set of fields, which
 * should reduce the time required to load a class.
 *
 * NOTE: reference fields *must* come first, or precacheReferenceOffsets()
 * will break.
 */
static bool computeFieldOffsets(ClassObject* clazz)
{
    int fieldOffset;
    int i, j;

    dvmLinearReadWrite(clazz->classLoader, clazz->ifields);

    if (clazz->super != NULL)
        fieldOffset = clazz->super->objectSize;
    else
        fieldOffset = offsetof(DataObject, instanceData);

    LOGVV("--- computeFieldOffsets '%s'\n", clazz->descriptor);

    //LOGI("OFFSETS fieldCount=%d\n", clazz->ifieldCount);
    //LOGI("dataobj, instance: %d\n", offsetof(DataObject, instanceData));
    //LOGI("classobj, access: %d\n", offsetof(ClassObject, accessFlags));
    //LOGI("super=%p, fieldOffset=%d\n", clazz->super, fieldOffset);

    /*
     * Start by moving all reference fields to the front.
     */
    clazz->ifieldRefCount = 0;
    j = clazz->ifieldCount - 1;
    for (i = 0; i < clazz->ifieldCount; i++) {
        InstField* pField = &clazz->ifields[i];
        char c = pField->field.signature[0];

        if (c != '[' && c != 'L') {
            /* This isn't a reference field; see if any reference fields
             * follow this one.  If so, we'll move it to this position.
             * (quicksort-style partitioning)
             */
            while (j > i) {
                InstField* refField = &clazz->ifields[j--];
                char rc = refField->field.signature[0];

                if (rc == '[' || rc == 'L') {
                    /* Here's a reference field that follows at least one
                     * non-reference field.  Swap it with the current field.
                     * (When this returns, "pField" points to the reference
                     * field, and "refField" points to the non-ref field.)
                     */
                    swapField(pField, refField);

                    /* Fix the signature.
                     */
                    c = rc;

                    clazz->ifieldRefCount++;
                    break;
                }
            }
            /* We may or may not have swapped a field.
             */
        } else {
            /* This is a reference field.
             */
            clazz->ifieldRefCount++;
        }

        /*
         * If we've hit the end of the reference fields, break.
         */
        if (c != '[' && c != 'L')
            break;

        pField->byteOffset = fieldOffset;
        fieldOffset += sizeof(u4);
        LOGVV("  --- offset1 '%s'=%d\n", pField->field.name,pField->byteOffset);
    }

    /*
     * Now we want to pack all of the double-wide fields together.  If we're
     * not aligned, though, we want to shuffle one 32-bit field into place.
     * If we can't find one, we'll have to pad it.
     */
    if (i != clazz->ifieldCount && (fieldOffset & 0x04) != 0) {
        LOGVV("  +++ not aligned\n");

        InstField* pField = &clazz->ifields[i];
        char c = pField->field.signature[0];

        if (c != 'J' && c != 'D') {
            /*
             * The field that comes next is 32-bit, so just advance past it.
             */
            assert(c != '[' && c != 'L');
            pField->byteOffset = fieldOffset;
            fieldOffset += sizeof(u4);
            i++;
            LOGVV("  --- offset2 '%s'=%d\n",
                pField->field.name, pField->byteOffset);
        } else {
            /*
             * Next field is 64-bit, so search for a 32-bit field we can
             * swap into it.
             */
            bool found = false;
            j = clazz->ifieldCount - 1;
            while (j > i) {
                InstField* singleField = &clazz->ifields[j--];
                char rc = singleField->field.signature[0];

                if (rc != 'J' && rc != 'D') {
                    swapField(pField, singleField);
                    //c = rc;
                    LOGVV("  +++ swapped '%s' for alignment\n",
                        pField->field.name);
                    pField->byteOffset = fieldOffset;
                    fieldOffset += sizeof(u4);
                    LOGVV("  --- offset3 '%s'=%d\n",
                        pField->field.name, pField->byteOffset);
                    found = true;
                    i++;
                    break;
                }
            }
            if (!found) {
                LOGV("  +++ inserting pad field in '%s'\n", clazz->descriptor);
                fieldOffset += sizeof(u4);
            }
        }
    }

    /*
     * Alignment is good, shuffle any double-wide fields forward, and
     * finish assigning field offsets to all fields.
     */
    assert(i == clazz->ifieldCount || (fieldOffset & 0x04) == 0);
    j = clazz->ifieldCount - 1;
    for ( ; i < clazz->ifieldCount; i++) {
        InstField* pField = &clazz->ifields[i];
        char c = pField->field.signature[0];

        if (c != 'D' && c != 'J') {
            /* This isn't a double-wide field; see if any double fields
             * follow this one.  If so, we'll move it to this position.
             * (quicksort-style partitioning)
             */
            while (j > i) {
                InstField* doubleField = &clazz->ifields[j--];
                char rc = doubleField->field.signature[0];

                if (rc == 'D' || rc == 'J') {
                    /* Here's a double-wide field that follows at least one
                     * non-double field.  Swap it with the current field.
                     * (When this returns, "pField" points to the reference
                     * field, and "doubleField" points to the non-double field.)
                     */
                    swapField(pField, doubleField);
                    c = rc;

                    break;
                }
            }
            /* We may or may not have swapped a field.
             */
        } else {
            /* This is a double-wide field, leave it be.
             */
        }

        pField->byteOffset = fieldOffset;
        LOGVV("  --- offset4 '%s'=%d\n", pField->field.name,pField->byteOffset);
        fieldOffset += sizeof(u4);
        if (c == 'J' || c == 'D')
            fieldOffset += sizeof(u4);
    }

#ifndef NDEBUG
    /* Make sure that all reference fields appear before
     * non-reference fields, and all double-wide fields are aligned.
     */
    j = 0;  // seen non-ref
    for (i = 0; i < clazz->ifieldCount; i++) {
        InstField *pField = &clazz->ifields[i];
        char c = pField->field.signature[0];

        if (c == 'D' || c == 'J') {
            assert((pField->byteOffset & 0x07) == 0);
        }

        if (c != '[' && c != 'L') {
            if (!j) {
                assert(i == clazz->ifieldRefCount);
                j = 1;
            }
        } else if (j) {
            assert(false);
        }
    }
    if (!j) {
        assert(clazz->ifieldRefCount == clazz->ifieldCount);
    }
#endif

    /*
     * We map a C struct directly on top of java/lang/Class objects.  Make
     * sure we left enough room for the instance fields.
     */
    assert(clazz != gDvm.classJavaLangClass || (size_t)fieldOffset <
        offsetof(ClassObject, instanceData) + sizeof(clazz->instanceData));

    clazz->objectSize = fieldOffset;

    dvmLinearReadOnly(clazz->classLoader, clazz->ifields);
    return true;
}

/*
 * Throw the VM-spec-mandated error when an exception is thrown during
 * class initialization.
 *
 * The safest way to do this is to call the ExceptionInInitializerError
 * constructor that takes a Throwable.
 *
 * [Do we want to wrap it if the original is an Error rather than
 * an Exception?]
 */
static void throwClinitError(void)
{
    Thread* self = dvmThreadSelf();
    Object* exception;
    Object* eiie;

    exception = dvmGetException(self);
    dvmAddTrackedAlloc(exception, self);
    dvmClearException(self);

    if (gDvm.classJavaLangExceptionInInitializerError == NULL) {
        /*
         * Always resolves to same thing -- no race condition.
         */
        gDvm.classJavaLangExceptionInInitializerError =
            dvmFindSystemClass(
                    "Ljava/lang/ExceptionInInitializerError;");
        if (gDvm.classJavaLangExceptionInInitializerError == NULL) {
            LOGE("Unable to prep java/lang/ExceptionInInitializerError\n");
            goto fail;
        }

        gDvm.methJavaLangExceptionInInitializerError_init =
            dvmFindDirectMethodByDescriptor(gDvm.classJavaLangExceptionInInitializerError,
            "<init>", "(Ljava/lang/Throwable;)V");
        if (gDvm.methJavaLangExceptionInInitializerError_init == NULL) {
            LOGE("Unable to prep java/lang/ExceptionInInitializerError\n");
            goto fail;
        }
    }

    eiie = dvmAllocObject(gDvm.classJavaLangExceptionInInitializerError,
                ALLOC_DEFAULT);
    if (eiie == NULL)
        goto fail;

    /*
     * Construct the new object, and replace the exception with it.
     */
    JValue unused;
    dvmCallMethod(self, gDvm.methJavaLangExceptionInInitializerError_init,
        eiie, &unused, exception);
    dvmSetException(self, eiie);
    dvmReleaseTrackedAlloc(eiie, NULL);
    dvmReleaseTrackedAlloc(exception, self);
    return;

fail:       /* restore original exception */
    dvmSetException(self, exception);
    dvmReleaseTrackedAlloc(exception, self);
    return;
}

/*
 * The class failed to initialize on a previous attempt, so we want to throw
 * a NoClassDefFoundError (v2 2.17.5).  The exception to this rule is if we
 * failed in verification, in which case v2 5.4.1 says we need to re-throw
 * the previous error.
 */
static void throwEarlierClassFailure(ClassObject* clazz)
{
    LOGI("Rejecting re-init on previously-failed class %s v=%p\n",
        clazz->descriptor, clazz->verifyErrorClass);

    if (clazz->verifyErrorClass == NULL) {
        dvmThrowExceptionWithClassMessage("Ljava/lang/NoClassDefFoundError;",
            clazz->descriptor);
    } else {
        dvmThrowExceptionByClassWithClassMessage(clazz->verifyErrorClass,
            clazz->descriptor);
    }
}

/*
 * Initialize any static fields whose values are stored in
 * the DEX file.  This must be done during class initialization.
 */
static void initSFields(ClassObject* clazz)
{
    Thread* self = dvmThreadSelf(); /* for dvmReleaseTrackedAlloc() */
    DexFile* pDexFile;
    const DexClassDef* pClassDef;
    const DexEncodedArray* pValueList;
    EncodedArrayIterator iterator;
    int i;

    if (clazz->sfieldCount == 0) {
        return;
    }
    if (clazz->pDvmDex == NULL) {
        /* generated class; any static fields should already be set up */
        LOGV("Not initializing static fields in %s\n", clazz->descriptor);
        return;
    }
    pDexFile = clazz->pDvmDex->pDexFile;

    pClassDef = dexFindClass(pDexFile, clazz->descriptor);
    assert(pClassDef != NULL);

    pValueList = dexGetStaticValuesList(pDexFile, pClassDef);
    if (pValueList == NULL) {
        return;
    }

    dvmEncodedArrayIteratorInitialize(&iterator, pValueList, clazz);

    /*
     * Iterate over the initial values array, setting the corresponding
     * static field for each array element.
     */

    for (i = 0; dvmEncodedArrayIteratorHasNext(&iterator); i++) {
        AnnotationValue value;
        bool parsed = dvmEncodedArrayIteratorGetNext(&iterator, &value);
        StaticField* sfield = &clazz->sfields[i];
        const char* descriptor = sfield->field.signature;
        bool needRelease = false;

        if (! parsed) {
            /*
             * TODO: Eventually verification should attempt to ensure
             * that this can't happen at least due to a data integrity
             * problem.
             */
            LOGE("Static initializer parse failed for %s at index %d",
                    clazz->descriptor, i);
            dvmAbort();
        }

        /* Verify that the value we got was of a valid type. */

        switch (descriptor[0]) {
            case 'Z': parsed = (value.type == kDexAnnotationBoolean); break;
            case 'B': parsed = (value.type == kDexAnnotationByte);    break;
            case 'C': parsed = (value.type == kDexAnnotationChar);    break;
            case 'S': parsed = (value.type == kDexAnnotationShort);   break;
            case 'I': parsed = (value.type == kDexAnnotationInt);     break;
            case 'J': parsed = (value.type == kDexAnnotationLong);    break;
            case 'F': parsed = (value.type == kDexAnnotationFloat);   break;
            case 'D': parsed = (value.type == kDexAnnotationDouble);  break;
            case '[': parsed = (value.type == kDexAnnotationNull);    break;
            case 'L': {
                switch (value.type) {
                    case kDexAnnotationNull: {
                        /* No need for further tests. */
                        break;
                    }
                    case kDexAnnotationString: {
                        parsed =
                            (strcmp(descriptor, "Ljava/lang/String;") == 0);
                        needRelease = true;
                        break;
                    }
                    case kDexAnnotationType: {
                        parsed =
                            (strcmp(descriptor, "Ljava/lang/Class;") == 0);
                        needRelease = true;
                        break;
                    }
                    default: {
                        parsed = false;
                        break;
                    }
                }
                break;
            }
            default: {
                parsed = false;
                break;
            }
        }

        if (parsed) {
            /*
             * All's well, so store the value. Note: This always
             * stores the full width of a JValue, even though most of
             * the time only the first word is needed.
             */
            sfield->value = value.value;
            if (needRelease) {
                dvmReleaseTrackedAlloc(value.value.l, self);
            }
        } else {
            /*
             * Something up above had a problem. TODO: See comment
             * above the switch about verfication.
             */
            LOGE("Bogus static initialization: value type %d in field type "
                    "%s for %s at index %d",
                value.type, descriptor, clazz->descriptor, i);
            dvmAbort();
        }
    }
}


/*
 * Determine whether "descriptor" yields the same class object in the
 * context of clazz1 and clazz2.
 *
 * The caller must hold gDvm.loadedClasses.
 *
 * Returns "true" if they match.
 */
static bool compareDescriptorClasses(const char* descriptor,
    const ClassObject* clazz1, const ClassObject* clazz2)
{
    ClassObject* result1;
    ClassObject* result2;

    /*
     * Do the first lookup by name.
     */
    result1 = dvmFindClassNoInit(descriptor, clazz1->classLoader);

    /*
     * We can skip a second lookup by name if the second class loader is
     * in the initiating loader list of the class object we retrieved.
     * (This means that somebody already did a lookup of this class through
     * the second loader, and it resolved to the same class.)  If it's not
     * there, we may simply not have had an opportunity to add it yet, so
     * we do the full lookup.
     *
     * The initiating loader test should catch the majority of cases
     * (in particular, the zillions of references to String/Object).
     *
     * Unfortunately we're still stuck grabbing a mutex to do the lookup.
     *
     * For this to work, the superclass/interface should be the first
     * argument, so that way if it's from the bootstrap loader this test
     * will work.  (The bootstrap loader, by definition, never shows up
     * as the initiating loader of a class defined by some other loader.)
     */
    dvmHashTableLock(gDvm.loadedClasses);
    bool isInit = dvmLoaderInInitiatingList(result1, clazz2->classLoader);
    dvmHashTableUnlock(gDvm.loadedClasses);

    if (isInit) {
        //printf("%s(obj=%p) / %s(cl=%p): initiating\n",
        //    result1->descriptor, result1,
        //    clazz2->descriptor, clazz2->classLoader);
        return true;
    } else {
        //printf("%s(obj=%p) / %s(cl=%p): RAW\n",
        //    result1->descriptor, result1,
        //    clazz2->descriptor, clazz2->classLoader);
        result2 = dvmFindClassNoInit(descriptor, clazz2->classLoader);
    }

    if (result1 == NULL || result2 == NULL) {
        dvmClearException(dvmThreadSelf());
        if (result1 == result2) {
            /*
             * Neither class loader could find this class.  Apparently it
             * doesn't exist.
             *
             * We can either throw some sort of exception now, or just
             * assume that it'll fail later when something actually tries
             * to use the class.  For strict handling we should throw now,
             * because a "tricky" class loader could start returning
             * something later, and a pair of "tricky" loaders could set
             * us up for confusion.
             *
             * I'm not sure if we're allowed to complain about nonexistent
             * classes in method signatures during class init, so for now
             * this will just return "true" and let nature take its course.
             */
            return true;
        } else {
            /* only one was found, so clearly they're not the same */
            return false;
        }
    }

    return result1 == result2;
}

/*
 * For every component in the method descriptor, resolve the class in the
 * context of the two classes and compare the results.
 *
 * For best results, the "superclass" class should be first.
 *
 * Returns "true" if the classes match, "false" otherwise.
 */
static bool checkMethodDescriptorClasses(const Method* meth,
    const ClassObject* clazz1, const ClassObject* clazz2)
{
    DexParameterIterator iterator;
    const char* descriptor;

    /* walk through the list of parameters */
    dexParameterIteratorInit(&iterator, &meth->prototype);
    while (true) {
        descriptor = dexParameterIteratorNextDescriptor(&iterator);

        if (descriptor == NULL)
            break;

        if (descriptor[0] == 'L' || descriptor[0] == '[') {
            /* non-primitive type */
            if (!compareDescriptorClasses(descriptor, clazz1, clazz2))
                return false;
        }
    }

    /* check the return type */
    descriptor = dexProtoGetReturnType(&meth->prototype);
    if (descriptor[0] == 'L' || descriptor[0] == '[') {
        if (!compareDescriptorClasses(descriptor, clazz1, clazz2))
            return false;
    }
    return true;
}

/*
 * Validate the descriptors in the superclass and interfaces.
 *
 * What we need to do is ensure that the classes named in the method
 * descriptors in our ancestors and ourselves resolve to the same class
 * objects.  The only time this matters is when the classes come from
 * different class loaders, and the resolver might come up with a
 * different answer for the same class name depending on context.
 *
 * We don't need to check to see if an interface's methods match with
 * its superinterface's methods, because you can't instantiate an
 * interface and do something inappropriate with it.  If interface I1
 * extends I2 and is implemented by C, and I1 and I2 are in separate
 * class loaders and have conflicting views of other classes, we will
 * catch the conflict when we process C.  Anything that implements I1 is
 * doomed to failure, but we don't need to catch that while processing I1.
 *
 * On failure, throws an exception and returns "false".
 */
static bool validateSuperDescriptors(const ClassObject* clazz)
{
    int i;

    if (dvmIsInterfaceClass(clazz))
        return true;

    /*
     * Start with the superclass-declared methods.
     */
    if (clazz->super != NULL &&
        clazz->classLoader != clazz->super->classLoader)
    {
        /*
         * Walk through every method declared in the superclass, and
         * compare resolved descriptor components.  We pull the Method
         * structs out of the vtable.  It doesn't matter whether we get
         * the struct from the parent or child, since we just need the
         * UTF-8 descriptor, which must match.
         *
         * We need to do this even for the stuff inherited from Object,
         * because it's possible that the new class loader has redefined
         * a basic class like String.
         */
        const Method* meth;

        //printf("Checking %s %p vs %s %p\n",
        //    clazz->descriptor, clazz->classLoader,
        //    clazz->super->descriptor, clazz->super->classLoader);
        for (i = clazz->super->vtableCount - 1; i >= 0; i--) {
            meth = clazz->vtable[i];
            if (!checkMethodDescriptorClasses(meth, clazz->super, clazz)) {
                LOGW("Method mismatch: %s in %s (cl=%p) and super %s (cl=%p)\n",
                    meth->name, clazz->descriptor, clazz->classLoader,
                    clazz->super->descriptor, clazz->super->classLoader);
                dvmThrowException("Ljava/lang/LinkageError;",
                    "Classes resolve differently in superclass");
                return false;
            }
        }
    }

    /*
     * Check all interfaces we implement.
     */
    for (i = 0; i < clazz->iftableCount; i++) {
        const InterfaceEntry* iftable = &clazz->iftable[i];

        if (clazz->classLoader != iftable->clazz->classLoader) {
            const ClassObject* iface = iftable->clazz;
            int j;

            for (j = 0; j < iface->virtualMethodCount; j++) {
                const Method* meth;
                int vtableIndex;

                vtableIndex = iftable->methodIndexArray[j];
                meth = clazz->vtable[vtableIndex];

                if (!checkMethodDescriptorClasses(meth, iface, clazz)) {
                    LOGW("Method mismatch: %s in %s (cl=%p) and "
                            "iface %s (cl=%p)\n",
                        meth->name, clazz->descriptor, clazz->classLoader,
                        iface->descriptor, iface->classLoader);
                    dvmThrowException("Ljava/lang/LinkageError;",
                        "Classes resolve differently in interface");
                    return false;
                }
            }
        }
    }

    return true;
}

/*
 * Returns true if the class is being initialized by us (which means that
 * calling dvmInitClass will return immediately after fiddling with locks).
 *
 * There isn't a race here, because either clazz->initThreadId won't match
 * us, or it will and it was set in the same thread.
 */
bool dvmIsClassInitializing(const ClassObject* clazz)
{
    return (clazz->status == CLASS_INITIALIZING &&
            clazz->initThreadId == dvmThreadSelf()->threadId);
}

/*
 * If a class has not been initialized, do so by executing the code in
 * <clinit>.  The sequence is described in the VM spec v2 2.17.5.
 *
 * It is possible for multiple threads to arrive here simultaneously, so
 * we need to lock the class while we check stuff.  We know that no
 * interpreted code has access to the class yet, so we can use the class's
 * monitor lock.
 *
 * We will often be called recursively, e.g. when the <clinit> code resolves
 * one of its fields, the field resolution will try to initialize the class.
 *
 * This can get very interesting if a class has a static field initialized
 * to a new instance of itself.  <clinit> will end up calling <init> on
 * the members it is initializing, which is fine unless it uses the contents
 * of static fields to initialize instance fields.  This will leave the
 * static-referenced objects in a partially initialized state.  This is
 * reasonably rare and can sometimes be cured with proper field ordering.
 *
 * On failure, returns "false" with an exception raised.
 *
 * -----
 *
 * It is possible to cause a deadlock by having a situation like this:
 *   class A { static { sleep(10000); new B(); } }
 *   class B { static { sleep(10000); new A(); } }
 *   new Thread() { public void run() { new A(); } }.start();
 *   new Thread() { public void run() { new B(); } }.start();
 * This appears to be expected under the spec.
 *
 * The interesting question is what to do if somebody calls Thread.interrupt()
 * on one of the deadlocked threads.  According to the VM spec, they're both
 * sitting in "wait".  Should the interrupt code quietly raise the
 * "interrupted" flag, or should the "wait" return immediately with an
 * exception raised?
 *
 * This gets a little murky.  The VM spec says we call "wait", and the
 * spec for Thread.interrupt says Object.wait is interruptible.  So it
 * seems that, if we get unlucky and interrupt class initialization, we
 * are expected to throw (which gets converted to ExceptionInInitializerError
 * since InterruptedException is checked).
 *
 * There are a couple of problems here.  First, all threads are expected to
 * present a consistent view of class initialization, so we can't have it
 * fail in one thread and succeed in another.  Second, once a class fails
 * to initialize, it must *always* fail.  This means that a stray interrupt()
 * call could render a class unusable for the lifetime of the VM.
 *
 * In most cases -- the deadlock example above being a counter-example --
 * the interrupting thread can't tell whether the target thread handled
 * the initialization itself or had to wait while another thread did the
 * work.  Refusing to interrupt class initialization is, in most cases,
 * not something that a program can reliably detect.
 *
 * On the assumption that interrupting class initialization is highly
 * undesirable in most circumstances, and that failing to do so does not
 * deviate from the spec in a meaningful way, we don't allow class init
 * to be interrupted by Thread.interrupt().
 */
bool dvmInitClass(ClassObject* clazz)
{
#if LOG_CLASS_LOADING
    bool initializedByUs = false;
#endif

    Thread* self = dvmThreadSelf();
    const Method* method;

    dvmLockObject(self, (Object*) clazz);
    assert(dvmIsClassLinked(clazz) || clazz->status == CLASS_ERROR);

    /*
     * If the class hasn't been verified yet, do so now.
     */
    if (clazz->status < CLASS_VERIFIED) {
        /*
         * If we're in an "erroneous" state, throw an exception and bail.
         */
        if (clazz->status == CLASS_ERROR) {
            throwEarlierClassFailure(clazz);
            goto bail_unlock;
        }

        assert(clazz->status == CLASS_RESOLVED);
        assert(!IS_CLASS_FLAG_SET(clazz, CLASS_ISPREVERIFIED));

        if (gDvm.classVerifyMode == VERIFY_MODE_NONE ||
            (gDvm.classVerifyMode == VERIFY_MODE_REMOTE &&
             clazz->classLoader == NULL))
        {
            LOGV("+++ not verifying class %s (cl=%p)\n",
                clazz->descriptor, clazz->classLoader);
            goto noverify;
        }

        if (!gDvm.optimizing)
            LOGV("+++ late verify on %s\n", clazz->descriptor);

        /*
         * We're not supposed to optimize an unverified class, but during
         * development this mode was useful.  We can't verify an optimized
         * class because the optimization process discards information.
         */
        if (IS_CLASS_FLAG_SET(clazz, CLASS_ISOPTIMIZED)) {
            LOGW("Class '%s' was optimized without verification; "
                 "not verifying now\n",
                clazz->descriptor);
            LOGW("  ('rm /data/dalvik-cache/*' and restart to fix this)");
            goto verify_failed;
        }

        clazz->status = CLASS_VERIFYING;
        if (!dvmVerifyClass(clazz, VERIFY_DEFAULT)) {
verify_failed:
            dvmThrowExceptionWithClassMessage("Ljava/lang/VerifyError;",
                clazz->descriptor);
            clazz->verifyErrorClass = dvmGetException(self)->clazz;
            clazz->status = CLASS_ERROR;
            goto bail_unlock;
        }

        clazz->status = CLASS_VERIFIED;
    }
noverify:

    if (clazz->status == CLASS_INITIALIZED)
        goto bail_unlock;

    while (clazz->status == CLASS_INITIALIZING) {
        /* we caught somebody else in the act; was it us? */
        if (clazz->initThreadId == self->threadId) {
            //LOGV("HEY: found a recursive <clinit>\n");
            goto bail_unlock;
        }

        if (dvmCheckException(self)) {
            LOGW("GLITCH: exception pending at start of class init\n");
            dvmAbort();
        }

        /*
         * Wait for the other thread to finish initialization.  We pass
         * "false" for the "interruptShouldThrow" arg so it doesn't throw
         * an exception on interrupt.
         */
        dvmObjectWait(self, (Object*) clazz, 0, 0, false);

        /*
         * When we wake up, repeat the test for init-in-progress.  If there's
         * an exception pending (only possible if "interruptShouldThrow"
         * was set), bail out.
         */
        if (dvmCheckException(self)) {
            LOGI("Class init of '%s' failing with wait() exception\n",
                clazz->descriptor);
            /*
             * TODO: this is bogus, because it means the two threads have a
             * different idea of the class status.  We need to flag the
             * class as bad and ensure that the initializer thread respects
             * our notice.  If we get lucky and wake up after the class has
             * finished initialization but before being woken, we have to
             * swallow the exception, perhaps raising thread->interrupted
             * to preserve semantics.
             *
             * Since we're not currently allowing interrupts, this should
             * never happen and we don't need to fix this.
             */
            assert(false);
            throwClinitError();
            clazz->status = CLASS_ERROR;
            goto bail_unlock;
        }
        if (clazz->status == CLASS_INITIALIZING) {
            LOGI("Waiting again for class init\n");
            continue;
        }
        assert(clazz->status == CLASS_INITIALIZED ||
               clazz->status == CLASS_ERROR);
        if (clazz->status == CLASS_ERROR) {
            /*
             * The caller wants an exception, but it was thrown in a
             * different thread.  Synthesize one here.
             */
            dvmThrowException("Ljava/lang/UnsatisfiedLinkError;",
                "(<clinit> failed, see exception in other thread)");
        }
        goto bail_unlock;
    }

    /* see if we failed previously */
    if (clazz->status == CLASS_ERROR) {
        // might be wise to unlock before throwing; depends on which class
        // it is that we have locked
        dvmUnlockObject(self, (Object*) clazz);
        throwEarlierClassFailure(clazz);
        return false;
    }

    /*
     * We're ready to go, and have exclusive access to the class.
     *
     * Before we start initialization, we need to do one extra bit of
     * validation: make sure that the methods declared here match up
     * with our superclass and interfaces.  We know that the UTF-8
     * descriptors match, but classes from different class loaders can
     * have the same name.
     *
     * We do this now, rather than at load/link time, for the same reason
     * that we defer verification.
     *
     * It's unfortunate that we need to do this at all, but we risk
     * mixing reference types with identical names (see Dalvik test 068).
     */
    if (!validateSuperDescriptors(clazz)) {
        assert(dvmCheckException(self));
        clazz->status = CLASS_ERROR;
        goto bail_unlock;
    }

    /*
     * Let's initialize this thing.
     *
     * We unlock the object so that other threads can politely sleep on
     * our mutex with Object.wait(), instead of hanging or spinning trying
     * to grab our mutex.
     */
    assert(clazz->status < CLASS_INITIALIZING);

#if LOG_CLASS_LOADING
    // We started initializing.
    logClassLoad('+', clazz);
    initializedByUs = true;
#endif

    clazz->status = CLASS_INITIALIZING;
    clazz->initThreadId = self->threadId;
    dvmUnlockObject(self, (Object*) clazz);

    /* init our superclass */
    if (clazz->super != NULL && clazz->super->status != CLASS_INITIALIZED) {
        assert(!dvmIsInterfaceClass(clazz));
        if (!dvmInitClass(clazz->super)) {
            assert(dvmCheckException(self));
            clazz->status = CLASS_ERROR;
            /* wake up anybody who started waiting while we were unlocked */
            dvmLockObject(self, (Object*) clazz);
            goto bail_notify;
        }
    }

    /* Initialize any static fields whose values are
     * stored in the Dex file.  This should include all of the
     * simple "final static" fields, which are required to
     * be initialized first. (vmspec 2 sec 2.17.5 item 8)
     * More-complicated final static fields should be set
     * at the beginning of <clinit>;  all we can do is trust
     * that the compiler did the right thing.
     */
    initSFields(clazz);

    /* Execute any static initialization code.
     */
    method = dvmFindDirectMethodByDescriptor(clazz, "<clinit>", "()V");
    if (method == NULL) {
        LOGVV("No <clinit> found for %s\n", clazz->descriptor);
    } else {
        LOGVV("Invoking %s.<clinit>\n", clazz->descriptor);
        JValue unused;
        dvmCallMethod(self, method, NULL, &unused);
    }

    if (dvmCheckException(self)) {
        /*
         * We've had an exception thrown during static initialization.  We
         * need to throw an ExceptionInInitializerError, but we want to
         * tuck the original exception into the "cause" field.
         */
        LOGW("Exception %s thrown during %s.<clinit>\n",
            (dvmGetException(self)->clazz)->descriptor, clazz->descriptor);
        throwClinitError();
        //LOGW("+++ replaced\n");

        dvmLockObject(self, (Object*) clazz);
        clazz->status = CLASS_ERROR;
    } else {
        /* success! */
        dvmLockObject(self, (Object*) clazz);
        clazz->status = CLASS_INITIALIZED;
        LOGVV("Initialized class: %s\n", clazz->descriptor);
    }

bail_notify:
    /*
     * Notify anybody waiting on the object.
     */
    dvmObjectNotifyAll(self, (Object*) clazz);

bail_unlock:

#if LOG_CLASS_LOADING
    if (initializedByUs) {
        // We finished initializing.
        logClassLoad('-', clazz);
    }
#endif

    dvmUnlockObject(self, (Object*) clazz);

    return (clazz->status != CLASS_ERROR);
}

/*
 * Replace method->nativeFunc and method->insns with new values.  This is
 * performed on resolution of a native method.
 */
void dvmSetNativeFunc(const Method* method, DalvikBridgeFunc func,
    const u2* insns)
{
    ClassObject* clazz = method->clazz;

    /* just open up both; easier that way */
    dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);
    dvmLinearReadWrite(clazz->classLoader, clazz->directMethods);

    ((Method*)method)->nativeFunc = func;
    ((Method*)method)->insns = insns;

    dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
    dvmLinearReadOnly(clazz->classLoader, clazz->directMethods);
}

/*
 * Add a RegisterMap to a Method.  This is done when we verify the class
 * and compute the register maps at class initialization time, which means
 * that "pMap" is on the heap and should be freed when the Method is
 * discarded.
 */
void dvmSetRegisterMap(Method* method, const RegisterMap* pMap)
{
    ClassObject* clazz = method->clazz;

    if (method->registerMap != NULL) {
        LOGW("WARNING: registerMap already set for %s.%s\n",
            method->clazz->descriptor, method->name);
        /* keep going */
    }

    /* might be virtual or direct */
    dvmLinearReadWrite(clazz->classLoader, clazz->virtualMethods);
    dvmLinearReadWrite(clazz->classLoader, clazz->directMethods);

    method->registerMap = pMap;

    dvmLinearReadOnly(clazz->classLoader, clazz->virtualMethods);
    dvmLinearReadOnly(clazz->classLoader, clazz->directMethods);
}

/*
 * dvmHashForeach callback.  A nonzero return value causes foreach to
 * bail out.
 */
static int findClassCallback(void* vclazz, void* arg)
{
    ClassObject* clazz = vclazz;
    const char* descriptor = (const char*) arg;

    if (strcmp(clazz->descriptor, descriptor) == 0)
        return (int) clazz;
    return 0;
}

/*
 * Find a loaded class by descriptor. Returns the first one found.
 * Because there can be more than one if class loaders are involved,
 * this is not an especially good API. (Currently only used by the
 * debugger and "checking" JNI.)
 *
 * "descriptor" should have the form "Ljava/lang/Class;" or
 * "[Ljava/lang/Class;", i.e. a descriptor and not an internal-form
 * class name.
 */
ClassObject* dvmFindLoadedClass(const char* descriptor)
{
    int result;

    dvmHashTableLock(gDvm.loadedClasses);
    result = dvmHashForeach(gDvm.loadedClasses, findClassCallback,
            (void*) descriptor);
    dvmHashTableUnlock(gDvm.loadedClasses);

    return (ClassObject*) result;
}

/*
 * Retrieve the system (a/k/a application) class loader.
 */
Object* dvmGetSystemClassLoader(void)
{
    ClassObject* clazz;
    Method* getSysMeth;
    Object* loader;

    clazz = dvmFindSystemClass("Ljava/lang/ClassLoader;");
    if (clazz == NULL)
        return NULL;

    getSysMeth = dvmFindDirectMethodByDescriptor(clazz, "getSystemClassLoader",
        "()Ljava/lang/ClassLoader;");
    if (getSysMeth == NULL)
        return NULL;

    JValue result;
    dvmCallMethod(dvmThreadSelf(), getSysMeth, NULL, &result);
    loader = (Object*)result.l;
    return loader;
}


/*
 * This is a dvmHashForeach callback.
 */
static int dumpClass(void* vclazz, void* varg)
{
    const ClassObject* clazz = (const ClassObject*) vclazz;
    const ClassObject* super;
    int flags = (int) varg;
    char* desc;
    int i;

    if (clazz == NULL) {
        LOGI("dumpClass: ignoring request to dump null class\n");
        return 0;
    }

    if ((flags & kDumpClassFullDetail) == 0) {
        bool showInit = (flags & kDumpClassInitialized) != 0;
        bool showLoader = (flags & kDumpClassClassLoader) != 0;
        const char* initStr;

        initStr = dvmIsClassInitialized(clazz) ? "true" : "false";

        if (showInit && showLoader)
            LOGI("%s %p %s\n", clazz->descriptor, clazz->classLoader, initStr);
        else if (showInit)
            LOGI("%s %s\n", clazz->descriptor, initStr);
        else if (showLoader)
            LOGI("%s %p\n", clazz->descriptor, clazz->classLoader);
        else
            LOGI("%s\n", clazz->descriptor);

        return 0;
    }

    /* clazz->super briefly holds the superclass index during class prep */
    if ((u4)clazz->super > 0x10000 && (u4) clazz->super != (u4)-1)
        super = clazz->super;
    else
        super = NULL;

    LOGI("----- %s '%s' cl=%p ser=0x%08x -----\n",
        dvmIsInterfaceClass(clazz) ? "interface" : "class",
        clazz->descriptor, clazz->classLoader, clazz->serialNumber);
    LOGI("  objectSize=%d (%d from super)\n", (int) clazz->objectSize,
        super != NULL ? (int) super->objectSize : -1);
    LOGI("  access=0x%04x.%04x\n", clazz->accessFlags >> 16,
        clazz->accessFlags & JAVA_FLAGS_MASK);
    if (super != NULL)
        LOGI("  super='%s' (cl=%p)\n", super->descriptor, super->classLoader);
    if (dvmIsArrayClass(clazz)) {
        LOGI("  dimensions=%d elementClass=%s\n",
            clazz->arrayDim, clazz->elementClass->descriptor);
    }
    if (clazz->iftableCount > 0) {
        LOGI("  interfaces (%d):\n", clazz->iftableCount);
        for (i = 0; i < clazz->iftableCount; i++) {
            InterfaceEntry* ent = &clazz->iftable[i];
            int j;

            LOGI("    %2d: %s (cl=%p)\n",
                i, ent->clazz->descriptor, ent->clazz->classLoader);

            /* enable when needed */
            if (false && ent->methodIndexArray != NULL) {
                for (j = 0; j < ent->clazz->virtualMethodCount; j++)
                    LOGI("      %2d: %d %s %s\n",
                        j, ent->methodIndexArray[j],
                        ent->clazz->virtualMethods[j].name,
                        clazz->vtable[ent->methodIndexArray[j]]->name);
            }
        }
    }
    if (!dvmIsInterfaceClass(clazz)) {
        LOGI("  vtable (%d entries, %d in super):\n", clazz->vtableCount,
            super != NULL ? super->vtableCount : 0);
        for (i = 0; i < clazz->vtableCount; i++) {
            desc = dexProtoCopyMethodDescriptor(&clazz->vtable[i]->prototype);
            LOGI("    %s%2d: %p %20s %s\n",
                (i != clazz->vtable[i]->methodIndex) ? "*** " : "",
                (u4) clazz->vtable[i]->methodIndex, clazz->vtable[i],
                clazz->vtable[i]->name, desc);
            free(desc);
        }
        LOGI("  direct methods (%d entries):\n", clazz->directMethodCount);
        for (i = 0; i < clazz->directMethodCount; i++) {
            desc = dexProtoCopyMethodDescriptor(
                    &clazz->directMethods[i].prototype);
            LOGI("    %2d: %20s %s\n", i, clazz->directMethods[i].name,
                desc);
            free(desc);
        }
    } else {
        LOGI("  interface methods (%d):\n", clazz->virtualMethodCount);
        for (i = 0; i < clazz->virtualMethodCount; i++) {
            desc = dexProtoCopyMethodDescriptor(
                    &clazz->virtualMethods[i].prototype);
            LOGI("    %2d: %2d %20s %s\n", i,
                (u4) clazz->virtualMethods[i].methodIndex,
                clazz->virtualMethods[i].name,
                desc);
            free(desc);
        }
    }
    if (clazz->sfieldCount > 0) {
        LOGI("  static fields (%d entries):\n", clazz->sfieldCount);
        for (i = 0; i < clazz->sfieldCount; i++) {
            LOGI("    %2d: %20s %s\n", i, clazz->sfields[i].field.name,
                clazz->sfields[i].field.signature);
        }
    }
    if (clazz->ifieldCount > 0) {
        LOGI("  instance fields (%d entries):\n", clazz->ifieldCount);
        for (i = 0; i < clazz->ifieldCount; i++) {
            LOGI("    %2d: %20s %s\n", i, clazz->ifields[i].field.name,
                clazz->ifields[i].field.signature);
        }
    }
    return 0;
}

/*
 * Dump the contents of a single class.
 *
 * Pass kDumpClassFullDetail into "flags" to get lots of detail.
 */
void dvmDumpClass(const ClassObject* clazz, int flags)
{
    dumpClass((void*) clazz, (void*) flags);
}

/*
 * Dump the contents of all classes.
 */
void dvmDumpAllClasses(int flags)
{
    dvmHashTableLock(gDvm.loadedClasses);
    dvmHashForeach(gDvm.loadedClasses, dumpClass, (void*) flags);
    dvmHashTableUnlock(gDvm.loadedClasses);
}

/*
 * Get the number of loaded classes
 */
int dvmGetNumLoadedClasses()
{
    int count;
    dvmHashTableLock(gDvm.loadedClasses);
    count = dvmHashTableNumEntries(gDvm.loadedClasses);
    dvmHashTableUnlock(gDvm.loadedClasses);
    return count;
}

/*
 * Write some statistics to the log file.
 */
void dvmDumpLoaderStats(const char* msg)
{
    LOGV("VM stats (%s): cls=%d/%d meth=%d ifld=%d sfld=%d linear=%d\n",
        msg, gDvm.numLoadedClasses, dvmHashTableNumEntries(gDvm.loadedClasses),
        gDvm.numDeclaredMethods, gDvm.numDeclaredInstFields,
        gDvm.numDeclaredStaticFields, gDvm.pBootLoaderAlloc->curOffset);
#ifdef COUNT_PRECISE_METHODS
    LOGI("GC precise methods: %d\n",
        dvmPointerSetGetCount(gDvm.preciseMethods));
#endif
}

#ifdef PROFILE_FIELD_ACCESS
/*
 * Dump the field access counts for all fields in this method.
 */
static int dumpAccessCounts(void* vclazz, void* varg)
{
    const ClassObject* clazz = (const ClassObject*) vclazz;
    int i;

    for (i = 0; i < clazz->ifieldCount; i++) {
        Field* field = &clazz->ifields[i].field;

        if (field->gets != 0)
            printf("GI %d %s.%s\n", field->gets,
                field->clazz->descriptor, field->name);
        if (field->puts != 0)
            printf("PI %d %s.%s\n", field->puts,
                field->clazz->descriptor, field->name);
    }
    for (i = 0; i < clazz->sfieldCount; i++) {
        Field* field = &clazz->sfields[i].field;

        if (field->gets != 0)
            printf("GS %d %s.%s\n", field->gets,
                field->clazz->descriptor, field->name);
        if (field->puts != 0)
            printf("PS %d %s.%s\n", field->puts,
                field->clazz->descriptor, field->name);
    }

    return 0;
}

/*
 * Dump the field access counts for all loaded classes.
 */
void dvmDumpFieldAccessCounts(void)
{
    dvmHashTableLock(gDvm.loadedClasses);
    dvmHashForeach(gDvm.loadedClasses, dumpAccessCounts, NULL);
    dvmHashTableUnlock(gDvm.loadedClasses);
}
#endif


/*
 * Mark all classes associated with the built-in loader.
 */
static int markClassObject(void *clazz, void *arg)
{
    UNUSED_PARAMETER(arg);

    dvmMarkObjectNonNull((Object *)clazz);
    return 0;
}

/*
 * The garbage collector calls this to mark the class objects for all
 * loaded classes.
 */
void dvmGcScanRootClassLoader()
{
    /* dvmClassStartup() may not have been called before the first GC.
     */
    if (gDvm.loadedClasses != NULL) {
        dvmHashTableLock(gDvm.loadedClasses);
        dvmHashForeach(gDvm.loadedClasses, markClassObject, NULL);
        dvmHashTableUnlock(gDvm.loadedClasses);
    }
}


/*
 * ===========================================================================
 *      Method Prototypes and Descriptors
 * ===========================================================================
 */

/*
 * Compare the two method names and prototypes, a la strcmp(). The
 * name is considered the "major" order and the prototype the "minor"
 * order. The prototypes are compared as if by dvmCompareMethodProtos().
 */
int dvmCompareMethodNamesAndProtos(const Method* method1,
        const Method* method2)
{
    int result = strcmp(method1->name, method2->name);

    if (result != 0) {
        return result;
    }

    return dvmCompareMethodProtos(method1, method2);
}

/*
 * Compare the two method names and prototypes, a la strcmp(), ignoring
 * the return value. The name is considered the "major" order and the
 * prototype the "minor" order. The prototypes are compared as if by
 * dvmCompareMethodArgProtos().
 */
int dvmCompareMethodNamesAndParameterProtos(const Method* method1,
        const Method* method2)
{
    int result = strcmp(method1->name, method2->name);

    if (result != 0) {
        return result;
    }

    return dvmCompareMethodParameterProtos(method1, method2);
}

/*
 * Compare a (name, prototype) pair with the (name, prototype) of
 * a method, a la strcmp(). The name is considered the "major" order and
 * the prototype the "minor" order. The descriptor and prototype are
 * compared as if by dvmCompareDescriptorAndMethodProto().
 */
int dvmCompareNameProtoAndMethod(const char* name,
    const DexProto* proto, const Method* method)
{
    int result = strcmp(name, method->name);

    if (result != 0) {
        return result;
    }

    return dexProtoCompare(proto, &method->prototype);
}

/*
 * Compare a (name, method descriptor) pair with the (name, prototype) of
 * a method, a la strcmp(). The name is considered the "major" order and
 * the prototype the "minor" order. The descriptor and prototype are
 * compared as if by dvmCompareDescriptorAndMethodProto().
 */
int dvmCompareNameDescriptorAndMethod(const char* name,
    const char* descriptor, const Method* method)
{
    int result = strcmp(name, method->name);

    if (result != 0) {
        return result;
    }

    return dvmCompareDescriptorAndMethodProto(descriptor, method);
}
