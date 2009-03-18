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
 * Dalvik initialization, shutdown, and command-line argument processing.
 */
#include "Dalvik.h"
#include "test/Test.h"
#include "mterp/Mterp.h"

#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#include <limits.h>
#include <ctype.h>
#include <sys/wait.h>
#include <unistd.h>

#define kMinHeapStartSize   (1*1024*1024)
#define kMinHeapSize        (2*1024*1024)
#define kMaxHeapSize        (1*1024*1024*1024)

/*
 * Register VM-agnostic native methods for system classes.
 *
 * Currently defined in ../include/nativehelper/AndroidSystemNatives.h
 */
extern int jniRegisterSystemMethods(JNIEnv* env);

/* fwd */
static bool registerSystemNatives(JNIEnv* pEnv);
static bool dvmInitJDWP(void);
static bool dvmInitZygote(void);


/* global state */
struct DvmGlobals gDvm;

/*
 * Show usage.
 *
 * We follow the tradition of unhyphenated compound words.
 */
static void dvmUsage(const char* progName)
{
    dvmFprintf(stderr, "%s: [options] class [argument ...]\n", progName);
    dvmFprintf(stderr, "%s: [options] -jar file.jar [argument ...]\n",progName);
    dvmFprintf(stderr, "\n");
    dvmFprintf(stderr, "The following standard options are recognized:\n");
    dvmFprintf(stderr, "  -classpath classpath\n");
    dvmFprintf(stderr, "  -Dproperty=value\n");
    dvmFprintf(stderr, "  -verbose:tag  ('gc', 'jni', or 'class')\n");
    dvmFprintf(stderr, "  -ea[:<package name>... |:<class name>]\n");
    dvmFprintf(stderr, "  -da[:<package name>... |:<class name>]\n");
    dvmFprintf(stderr, "   (-enableassertions, -disableassertions)\n");
    dvmFprintf(stderr, "  -esa\n");
    dvmFprintf(stderr, "  -dsa\n");
    dvmFprintf(stderr,
                "   (-enablesystemassertions, -disablesystemassertions)\n");
    dvmFprintf(stderr, "  -showversion\n");
    dvmFprintf(stderr, "  -help\n");
    dvmFprintf(stderr, "\n");
    dvmFprintf(stderr, "The following extended options are recognized:\n");
    dvmFprintf(stderr, "  -Xrunjdwp:<options>\n");
    dvmFprintf(stderr, "  -Xbootclasspath:bootclasspath\n");
    dvmFprintf(stderr, "  -Xcheck:tag  (e.g. 'jni')\n");
    dvmFprintf(stderr, "  -XmsN  (min heap, must be multiple of 1K, >= 1MB)\n");
    dvmFprintf(stderr, "  -XmxN  (max heap, must be multiple of 1K, >= 2MB)\n");
    dvmFprintf(stderr, "  -XssN  (stack size, >= %dKB, <= %dKB)\n",
        kMinStackSize / 1024, kMaxStackSize / 1024);
    dvmFprintf(stderr, "  -Xverify:{none,remote,all}\n");
    dvmFprintf(stderr, "  -Xrs\n");
    dvmFprintf(stderr,
                "  -Xint  (extended to accept ':portable' and ':fast')\n");
    dvmFprintf(stderr, "\n");
    dvmFprintf(stderr, "These are unique to Dalvik:\n");
    dvmFprintf(stderr, "  -Xzygote\n");
    dvmFprintf(stderr, "  -Xdexopt:{none,verified,all}\n");
    dvmFprintf(stderr, "  -Xnoquithandler\n");
    dvmFprintf(stderr,
                "  -Xjnigreflimit:N  (must be multiple of 100, >= 200)\n");
    dvmFprintf(stderr, "  -Xjniopts:{warnonly,forcecopy}\n");
    dvmFprintf(stderr, "  -Xdeadlockpredict:{off,warn,err,abort}\n");
    dvmFprintf(stderr, "  -Xstacktracefile:<filename>\n");
    dvmFprintf(stderr, "  -Xgenregmap\n");
    dvmFprintf(stderr, "  -Xcheckdexsum\n");
    dvmFprintf(stderr, "\n");
    dvmFprintf(stderr, "Configured with:"
#ifdef WITH_DEBUGGER
        " debugger"
#endif
#ifdef WITH_PROFILER
        " profiler"
#endif
#ifdef WITH_MONITOR_TRACKING
        " monitor_tracking"
#endif
#ifdef WITH_DEADLOCK_PREDICTION
        " deadlock_prediction"
#endif
#ifdef WITH_HPROF
        " hprof"
#endif
#ifdef WITH_HPROF_STACK
        " hprof_stack"
#endif
#ifdef WITH_HPROF_STACK_UNREACHABLE
        " hprof_stack_unreachable"
#endif
#ifdef WITH_ALLOC_LIMITS
        " alloc_limits"
#endif
#ifdef WITH_TRACKREF_CHECKS
        " trackref_checks"
#endif
#ifdef WITH_INSTR_CHECKS
        " instr_checks"
#endif
#ifdef WITH_EXTRA_OBJECT_VALIDATION
        " extra_object_validation"
#endif
#ifdef WITH_DALVIK_ASSERT
        " dalvik_assert"
#endif
#ifdef WITH_JNI_STACK_CHECK
        " jni_stack_check"
#endif
#ifdef EASY_GDB
        " easy_gdb"
#endif
#ifdef CHECK_MUTEX
        " check_mutex"
#endif
#ifdef PROFILE_FIELD_ACCESS
        " profile_field_access"
#endif
#ifdef DVM_TRACK_HEAP_MARKING
        " track_heap_marking"
#endif
#if DVM_RESOLVER_CACHE == DVM_RC_REDUCING
        " resolver_cache_reducing"
#elif DVM_RESOLVER_CACHE == DVM_RC_EXPANDING
        " resolver_cache_expanding"
#elif DVM_RESOLVER_CACHE == DVM_RC_NO_CACHE
        " resolver_cache_disabled"
#endif
    );
#ifdef DVM_SHOW_EXCEPTION
    dvmFprintf(stderr, " show_exception=%d", DVM_SHOW_EXCEPTION);
#endif
    dvmFprintf(stderr, "\n\n");
}

/*
 * Show helpful information on JDWP options.
 */
static void showJdwpHelp(void)
{
    dvmFprintf(stderr,
        "Example: -Xrunjdwp:transport=dt_socket,address=8000,server=y\n");
    dvmFprintf(stderr,
        "Example: -Xrunjdwp:transport=dt_socket,address=localhost:6500,server=n\n");
}

/*
 * Show version and copyright info.
 */
static void showVersion(void)
{
    dvmFprintf(stdout, "DalvikVM version %d.%d.%d\n",
        DALVIK_MAJOR_VERSION, DALVIK_MINOR_VERSION, DALVIK_BUG_VERSION);
    dvmFprintf(stdout, 
        "Copyright (C) 2007 The Android Open Source Project\n\n"
        "This software is built from source code licensed under the "
        "Apache License,\n"
        "Version 2.0 (the \"License\"). You may obtain a copy of the "
        "License at\n\n"
        "     http://www.apache.org/licenses/LICENSE-2.0\n\n"
        "See the associated NOTICE file for this software for further "
        "details.\n");
}

/*
 * Parse a string of the form /[0-9]+[kKmMgG]?/, which is used to specify
 * memory sizes.  [kK] indicates kilobytes, [mM] megabytes, and
 * [gG] gigabytes.
 *
 * "s" should point just past the "-Xm?" part of the string.
 * "min" specifies the lowest acceptable value described by "s".
 * "div" specifies a divisor, e.g. 1024 if the value must be a multiple
 * of 1024.
 *
 * The spec says the -Xmx and -Xms options must be multiples of 1024.  It
 * doesn't say anything about -Xss.
 *
 * Returns 0 (a useless size) if "s" is malformed or specifies a low or
 * non-evenly-divisible value.
 */
static unsigned int dvmParseMemOption(const char *s, unsigned int div)
{
    /* strtoul accepts a leading [+-], which we don't want,
     * so make sure our string starts with a decimal digit.
     */
    if (isdigit(*s)) {
        const char *s2;
        unsigned int val;

        val = (unsigned int)strtoul(s, (char **)&s2, 10);
        if (s2 != s) {
            /* s2 should be pointing just after the number.
             * If this is the end of the string, the user
             * has specified a number of bytes.  Otherwise,
             * there should be exactly one more character
             * that specifies a multiplier.
             */
            if (*s2 != '\0') {
                char c;

                /* The remainder of the string is either a single multiplier
                 * character, or nothing to indicate that the value is in
                 * bytes.
                 */
                c = *s2++;
                if (*s2 == '\0') {
                    unsigned int mul;

                    if (c == '\0') {
                        mul = 1;
                    } else if (c == 'k' || c == 'K') {
                        mul = 1024;
                    } else if (c == 'm' || c == 'M') {
                        mul = 1024 * 1024;
                    } else if (c == 'g' || c == 'G') {
                        mul = 1024 * 1024 * 1024;
                    } else {
                        /* Unknown multiplier character.
                         */
                        return 0;
                    }

                    if (val <= UINT_MAX / mul) {
                        val *= mul;
                    } else {
                        /* Clamp to a multiple of 1024.
                         */
                        val = UINT_MAX & ~(1024-1);
                    }
                } else {
                    /* There's more than one character after the
                     * numeric part.
                     */
                    return 0;
                }
            }

            /* The man page says that a -Xm value must be
             * a multiple of 1024.
             */
            if (val % div == 0) {
                return val;
            }
        }
    }

    return 0;
}

/*
 * Handle one of the JDWP name/value pairs.
 *
 * JDWP options are:
 *  help: if specified, show help message and bail
 *  transport: may be dt_socket or dt_shmem
 *  address: for dt_socket, "host:port", or just "port" when listening
 *  server: if "y", wait for debugger to attach; if "n", attach to debugger
 *  timeout: how long to wait for debugger to connect / listen
 *
 * Useful with server=n (these aren't supported yet):
 *  onthrow=<exception-name>: connect to debugger when exception thrown
 *  onuncaught=y|n: connect to debugger when uncaught exception thrown
 *  launch=<command-line>: launch the debugger itself
 *
 * The "transport" option is required, as is "address" if server=n.
 */
static bool handleJdwpOption(const char* name, const char* value)
{
    if (strcmp(name, "transport") == 0) {
        if (strcmp(value, "dt_socket") == 0) {
            gDvm.jdwpTransport = kJdwpTransportSocket;
        } else if (strcmp(value, "dt_android_adb") == 0) {
            gDvm.jdwpTransport = kJdwpTransportAndroidAdb;
        } else {
            LOGE("JDWP transport '%s' not supported\n", value);
            return false;
        }
    } else if (strcmp(name, "server") == 0) {
        if (*value == 'n')
            gDvm.jdwpServer = false;
        else if (*value == 'y')
            gDvm.jdwpServer = true;
        else {
            LOGE("JDWP option 'server' must be 'y' or 'n'\n");
            return false;
        }
    } else if (strcmp(name, "suspend") == 0) {
        if (*value == 'n')
            gDvm.jdwpSuspend = false;
        else if (*value == 'y')
            gDvm.jdwpSuspend = true;
        else {
            LOGE("JDWP option 'suspend' must be 'y' or 'n'\n");
            return false;
        }
    } else if (strcmp(name, "address") == 0) {
        /* this is either <port> or <host>:<port> */
        const char* colon = strchr(value, ':');
        char* end;
        long port;

        if (colon != NULL) {
            free(gDvm.jdwpHost);
            gDvm.jdwpHost = (char*) malloc(colon - value +1);
            strncpy(gDvm.jdwpHost, value, colon - value +1);
            gDvm.jdwpHost[colon-value] = '\0';
            value = colon + 1;
        }
        if (*value == '\0') {
            LOGE("JDWP address missing port\n");
            return false;
        }
        port = strtol(value, &end, 10);
        if (*end != '\0') {
            LOGE("JDWP address has junk in port field '%s'\n", value);
            return false;
        }
        gDvm.jdwpPort = port;
    } else if (strcmp(name, "launch") == 0 ||
               strcmp(name, "onthrow") == 0 ||
               strcmp(name, "oncaught") == 0 ||
               strcmp(name, "timeout") == 0)
    {
        /* valid but unsupported */
        LOGI("Ignoring JDWP option '%s'='%s'\n", name, value);
    } else {
        LOGI("Ignoring unrecognized JDWP option '%s'='%s'\n", name, value);
    }

    return true;
}

/*
 * Parse the latter half of a -Xrunjdwp/-agentlib:jdwp= string, e.g.:
 * "transport=dt_socket,address=8000,server=y,suspend=n"
 */
static bool parseJdwpOptions(const char* str)
{
    char* mangle = strdup(str);
    char* name = mangle;
    bool result = false;

    /*
     * Process all of the name=value pairs.
     */
    while (true) {
        char* value;
        char* comma;

        value = strchr(name, '=');
        if (value == NULL) {
            LOGE("JDWP opts: garbage at '%s'\n", name);
            goto bail;
        }

        comma = strchr(name, ',');      // use name, not value, for safety
        if (comma != NULL) {
            if (comma < value) {
                LOGE("JDWP opts: found comma before '=' in '%s'\n", mangle);
                goto bail;
            }
            *comma = '\0';
        }

        *value++ = '\0';        // stomp the '='

        if (!handleJdwpOption(name, value))
            goto bail;

        if (comma == NULL) {
            /* out of options */
            break;
        }
        name = comma+1;
    }

    /*
     * Make sure the combination of arguments makes sense.
     */
    if (gDvm.jdwpTransport == kJdwpTransportUnknown) {
        LOGE("JDWP opts: must specify transport\n");
        goto bail;
    }
    if (!gDvm.jdwpServer && (gDvm.jdwpHost == NULL || gDvm.jdwpPort == 0)) {
        LOGE("JDWP opts: when server=n, must specify host and port\n");
        goto bail;
    }
    // transport mandatory
    // outbound server address

    gDvm.jdwpConfigured = true;
    result = true;

bail:
    free(mangle);
    return result;
}

/*
 * Handle one of the four kinds of assertion arguments.
 *
 * "pkgOrClass" is the last part of an enable/disable line.  For a package
 * the arg looks like "-ea:com.google.fubar...", for a class it looks
 * like "-ea:com.google.fubar.Wahoo".  The string we get starts at the ':'.
 *
 * For system assertions (-esa/-dsa), "pkgOrClass" is NULL.
 *
 * Multiple instances of these arguments can be specified, e.g. you can
 * enable assertions for a package and then disable them for one class in
 * the package.
 */
static bool enableAssertions(const char* pkgOrClass, bool enable)
{
    AssertionControl* pCtrl = &gDvm.assertionCtrl[gDvm.assertionCtrlCount++];
    pCtrl->enable = enable;

    if (pkgOrClass == NULL) {
        /* enable or disable for all system classes */
        pCtrl->isPackage = false;
        pCtrl->pkgOrClass = NULL;
        pCtrl->pkgOrClassLen = 0;
    } else {
        if (*pkgOrClass == '\0') {
            /* global enable/disable for all but system */
            pCtrl->isPackage = false;
            pCtrl->pkgOrClass = strdup("");
            pCtrl->pkgOrClassLen = 0;
        } else {
            pCtrl->pkgOrClass = dvmDotToSlash(pkgOrClass+1);    // skip ':'
            if (pCtrl->pkgOrClass == NULL) {
                /* can happen if class name includes an illegal '/' */
                LOGW("Unable to process assertion arg '%s'\n", pkgOrClass);
                return false;
            }

            int len = strlen(pCtrl->pkgOrClass);
            if (len >= 3 && strcmp(pCtrl->pkgOrClass + len-3, "///") == 0) {
                /* mark as package, truncate two of the three slashes */
                pCtrl->isPackage = true;
                *(pCtrl->pkgOrClass + len-2) = '\0';
                pCtrl->pkgOrClassLen = len - 2;
            } else {
                /* just a class */
                pCtrl->isPackage = false;
                pCtrl->pkgOrClassLen = len;
            }
        }
    }

    return true;
}

/*
 * Turn assertions on when requested to do so by the Zygote.
 *
 * This is a bit sketchy.  We can't (easily) go back and fiddle with all
 * of the classes that have already been initialized, so this only
 * affects classes that have yet to be loaded.  If some or all assertions
 * have been enabled through some other means, we don't want to mess with
 * it here, so we do nothing.  Finally, we assume that there's room in
 * "assertionCtrl" to hold at least one entry; this is guaranteed by the
 * allocator.
 *
 * This must only be called from the main thread during zygote init.
 */
void dvmLateEnableAssertions(void)
{
    if (gDvm.assertionCtrl == NULL) {
        LOGD("Not late-enabling assertions: no assertionCtrl array\n");
        return;
    } else if (gDvm.assertionCtrlCount != 0) {
        LOGD("Not late-enabling assertions: some asserts already configured\n");
        return;
    }
    LOGD("Late-enabling assertions\n");

    /* global enable for all but system */
    AssertionControl* pCtrl = gDvm.assertionCtrl;
    pCtrl->pkgOrClass = strdup("");
    pCtrl->pkgOrClassLen = 0;
    pCtrl->isPackage = false;
    pCtrl->enable = true;
    gDvm.assertionCtrlCount = 1;
}


/*
 * Release memory associated with the AssertionCtrl array.
 */
static void freeAssertionCtrl(void)
{
    int i;

    for (i = 0; i < gDvm.assertionCtrlCount; i++)
        free(gDvm.assertionCtrl[i].pkgOrClass);
    free(gDvm.assertionCtrl);
}


/*
 * Process an argument vector full of options.  Unlike standard C programs,
 * argv[0] does not contain the name of the program.
 *
 * If "ignoreUnrecognized" is set, we ignore options starting with "-X" or "_"
 * that we don't recognize.  Otherwise, we return with an error as soon as
 * we see anything we can't identify.
 *
 * Returns 0 on success, -1 on failure, and 1 for the special case of
 * "-version" where we want to stop without showing an error message.
 */
static int dvmProcessOptions(int argc, const char* const argv[],
    bool ignoreUnrecognized)
{
    int i;

    LOGV("VM options (%d):\n", argc);
    for (i = 0; i < argc; i++)
        LOGV("  %d: '%s'\n", i, argv[i]);

    /*
     * Over-allocate AssertionControl array for convenience.  If allocated,
     * the array must be able to hold at least one entry, so that the
     * zygote-time activation can do its business.
     */
    assert(gDvm.assertionCtrl == NULL);
    if (argc > 0) {
        gDvm.assertionCtrl =
            (AssertionControl*) malloc(sizeof(AssertionControl) * argc);
        if (gDvm.assertionCtrl == NULL)
            return -1;
        assert(gDvm.assertionCtrlCount == 0);
    }

    for (i = 0; i < argc; i++) {
        if (strcmp(argv[i], "-help") == 0) {
            /* show usage and stop */
            return -1;

        } else if (strcmp(argv[i], "-version") == 0) {
            /* show version and stop */
            showVersion();
            return 1;
        } else if (strcmp(argv[i], "-showversion") == 0) {
            /* show version and continue */
            showVersion();

        } else if (strcmp(argv[i], "-classpath") == 0 ||
                   strcmp(argv[i], "-cp") == 0)
        {
            /* set classpath */
            if (i == argc-1) {
                dvmFprintf(stderr, "Missing classpath path list\n");
                return -1;
            }
            free(gDvm.classPathStr); /* in case we have compiled-in default */
            gDvm.classPathStr = strdup(argv[++i]);

        } else if (strncmp(argv[i], "-Xbootclasspath:",
                sizeof("-Xbootclasspath:")-1) == 0)
        {
            /* set bootclasspath */
            const char* path = argv[i] + sizeof("-Xbootclasspath:")-1;

            if (*path == '\0') {
                dvmFprintf(stderr, "Missing bootclasspath path list\n");
                return -1;
            }
            free(gDvm.bootClassPathStr);
            gDvm.bootClassPathStr = strdup(path);

            /*
             * TODO: support -Xbootclasspath/a and /p, which append or
             * prepend to the default bootclasspath.  We set the default
             * path earlier.
             */

        } else if (strncmp(argv[i], "-D", 2) == 0) {
            /* set property */
            dvmAddCommandLineProperty(argv[i] + 2);

        } else if (strcmp(argv[i], "-jar") == 0) {
            // TODO: handle this; name of jar should be in argv[i+1]
            dvmFprintf(stderr, "-jar not yet handled\n");
            assert(false);

        } else if (strncmp(argv[i], "-Xms", 4) == 0) {
            unsigned int val = dvmParseMemOption(argv[i]+4, 1024);
            if (val != 0) {
                if (val >= kMinHeapStartSize && val <= kMaxHeapSize) {
                    gDvm.heapSizeStart = val;
                } else {
                    dvmFprintf(stderr,
                        "Invalid -Xms '%s', range is %dKB to %dKB\n",
                        argv[i], kMinHeapStartSize/1024, kMaxHeapSize/1024);
                    return -1;
                }
            } else {
                dvmFprintf(stderr, "Invalid -Xms option '%s'\n", argv[i]);
                return -1;
            }
        } else if (strncmp(argv[i], "-Xmx", 4) == 0) {
            unsigned int val = dvmParseMemOption(argv[i]+4, 1024);
            if (val != 0) {
                if (val >= kMinHeapSize && val <= kMaxHeapSize) {
                    gDvm.heapSizeMax = val;
                } else {
                    dvmFprintf(stderr,
                        "Invalid -Xmx '%s', range is %dKB to %dKB\n",
                        argv[i], kMinHeapSize/1024, kMaxHeapSize/1024);
                    return -1;
                }
            } else {
                dvmFprintf(stderr, "Invalid -Xmx option '%s'\n", argv[i]);
                return -1;
            }
        } else if (strncmp(argv[i], "-Xss", 4) == 0) {
            unsigned int val = dvmParseMemOption(argv[i]+4, 1);
            if (val != 0) {
                if (val >= kMinStackSize && val <= kMaxStackSize) {
                    gDvm.stackSize = val;
                } else {
                    dvmFprintf(stderr, "Invalid -Xss '%s', range is %d to %d\n",
                        argv[i], kMinStackSize, kMaxStackSize);
                    return -1;
                }
            } else {
                dvmFprintf(stderr, "Invalid -Xss option '%s'\n", argv[i]);
                return -1;
            }

        } else if (strcmp(argv[i], "-verbose") == 0 ||
            strcmp(argv[i], "-verbose:class") == 0)
        {
            // JNI spec says "-verbose:gc,class" is valid, but cmd line
            // doesn't work that way; may want to support.
            gDvm.verboseClass = true;
        } else if (strcmp(argv[i], "-verbose:jni") == 0) {
            gDvm.verboseJni = true;
        } else if (strcmp(argv[i], "-verbose:gc") == 0) {
            gDvm.verboseGc = true;

        } else if (strncmp(argv[i], "-enableassertions", 17) == 0) {
            enableAssertions(argv[i] + 17, true);
        } else if (strncmp(argv[i], "-ea", 3) == 0) {
            enableAssertions(argv[i] + 3, true);
        } else if (strncmp(argv[i], "-disableassertions", 18) == 0) {
            enableAssertions(argv[i] + 18, false);
        } else if (strncmp(argv[i], "-da", 3) == 0) {
            enableAssertions(argv[i] + 3, false);
        } else if (strcmp(argv[i], "-enablesystemassertions") == 0 ||
                   strcmp(argv[i], "-esa") == 0)
        {
            enableAssertions(NULL, true);
        } else if (strcmp(argv[i], "-disablesystemassertions") == 0 ||
                   strcmp(argv[i], "-dsa") == 0)
        {
            enableAssertions(NULL, false);

        } else if (strncmp(argv[i], "-Xcheck:jni", 11) == 0) {
            /* nothing to do now -- was handled during JNI init */

        } else if (strcmp(argv[i], "-Xdebug") == 0) {
            /* accept but ignore */

        } else if (strncmp(argv[i], "-Xrunjdwp:", 10) == 0 ||
            strncmp(argv[i], "-agentlib:jdwp=", 15) == 0)
        {
            const char* tail;
            bool result = false;

            if (argv[i][1] == 'X')
                tail = argv[i] + 10;
            else
                tail = argv[i] + 15;

            if (strncmp(tail, "help", 4) == 0 || !parseJdwpOptions(tail)) {
                showJdwpHelp();
                return 1;
            }
        } else if (strcmp(argv[i], "-Xrs") == 0) {
            gDvm.reduceSignals = true;
        } else if (strcmp(argv[i], "-Xnoquithandler") == 0) {
            /* disables SIGQUIT handler thread while still blocking SIGQUIT */
            /* (useful if we don't want thread but system still signals us) */
            gDvm.noQuitHandler = true;
        } else if (strcmp(argv[i], "-Xzygote") == 0) {
            gDvm.zygote = true;
        } else if (strncmp(argv[i], "-Xdexopt:", 9) == 0) {
            if (strcmp(argv[i] + 9, "none") == 0)
                gDvm.dexOptMode = OPTIMIZE_MODE_NONE;
            else if (strcmp(argv[i] + 9, "verified") == 0)
                gDvm.dexOptMode = OPTIMIZE_MODE_VERIFIED;
            else if (strcmp(argv[i] + 9, "all") == 0)
                gDvm.dexOptMode = OPTIMIZE_MODE_ALL;
            else {
                dvmFprintf(stderr, "Unrecognized dexopt option '%s'\n",argv[i]);
                return -1;
            }
        } else if (strncmp(argv[i], "-Xverify:", 9) == 0) {
            if (strcmp(argv[i] + 9, "none") == 0)
                gDvm.classVerifyMode = VERIFY_MODE_NONE;
            else if (strcmp(argv[i] + 9, "remote") == 0)
                gDvm.classVerifyMode = VERIFY_MODE_REMOTE;
            else if (strcmp(argv[i] + 9, "all") == 0)
                gDvm.classVerifyMode = VERIFY_MODE_ALL;
            else {
                dvmFprintf(stderr, "Unrecognized verify option '%s'\n",argv[i]);
                return -1;
            }
        } else if (strncmp(argv[i], "-Xjnigreflimit:", 15) == 0) {
            int lim = atoi(argv[i] + 15);
            if (lim < 200 || (lim % 100) != 0) {
                dvmFprintf(stderr, "Bad value for -Xjnigreflimit: '%s'\n",
                    argv[i]+15);
                return -1;
            }
            gDvm.jniGrefLimit = lim;

        } else if (strcmp(argv[i], "-Xlog-stdio") == 0) {
            gDvm.logStdio = true;

        } else if (strncmp(argv[i], "-Xint", 5) == 0) {
            if (argv[i][5] == ':') {
                if (strcmp(argv[i] + 6, "portable") == 0)
                    gDvm.executionMode = kExecutionModeInterpPortable;
                else if (strcmp(argv[i] + 6, "fast") == 0)
                    gDvm.executionMode = kExecutionModeInterpFast;
                else {
                    dvmFprintf(stderr,
                        "Warning: Unrecognized interpreter mode %s\n",argv[i]);
                    /* keep going */
                }
            } else {
                /* disable JIT -- nothing to do here for now */
            }

        } else if (strncmp(argv[i], "-Xdeadlockpredict:", 18) == 0) {
#ifdef WITH_DEADLOCK_PREDICTION
            if (strcmp(argv[i] + 18, "off") == 0)
                gDvm.deadlockPredictMode = kDPOff;
            else if (strcmp(argv[i] + 18, "warn") == 0)
                gDvm.deadlockPredictMode = kDPWarn;
            else if (strcmp(argv[i] + 18, "err") == 0)
                gDvm.deadlockPredictMode = kDPErr;
            else if (strcmp(argv[i] + 18, "abort") == 0)
                gDvm.deadlockPredictMode = kDPAbort;
            else {
                dvmFprintf(stderr, "Bad value for -Xdeadlockpredict");
                return -1;
            }
            if (gDvm.deadlockPredictMode != kDPOff)
                LOGD("Deadlock prediction enabled (%s)\n", argv[i]+18);
#endif

        } else if (strncmp(argv[i], "-Xstacktracefile:", 17) == 0) {
            gDvm.stackTraceFile = strdup(argv[i]+17);

        } else if (strcmp(argv[i], "-Xgenregmap") == 0) {
            gDvm.generateRegisterMaps = true;

        } else if (strcmp(argv[i], "-Xcheckdexsum") == 0) {
            gDvm.verifyDexChecksum = true;

        } else {
            if (!ignoreUnrecognized) {
                dvmFprintf(stderr, "Unrecognized option '%s'\n", argv[i]);
                return -1;
            }
        }
    }

    if (gDvm.heapSizeStart > gDvm.heapSizeMax) {
        dvmFprintf(stderr, "Heap start size must be <= heap max size\n");
        return -1;
    }

    return 0;
}

/*
 * Set defaults for fields altered or modified by arguments.
 */
static void setCommandLineDefaults()
{
    const char* envStr;

    envStr = getenv("CLASSPATH");
    if (envStr != NULL)
        gDvm.classPathStr = strdup(envStr);
    else
        gDvm.classPathStr = strdup(".");
    envStr = getenv("BOOTCLASSPATH");
    if (envStr != NULL)
        gDvm.bootClassPathStr = strdup(envStr);
    else
        gDvm.bootClassPathStr = strdup(".");

    /* Defaults overridden by -Xms and -Xmx.
     * TODO: base these on a system or application-specific default
     */
    gDvm.heapSizeStart = 2 * 1024 * 1024;   // Spec says 16MB; too big for us.
    gDvm.heapSizeMax = 16 * 1024 * 1024;    // Spec says 75% physical mem
    gDvm.stackSize = kDefaultStackSize;

    /* gDvm.jdwpSuspend = true; */

    /* allowed unless zygote config doesn't allow it */
    gDvm.jdwpAllowed = true;

    /* default verification and optimization modes */
    gDvm.classVerifyMode = VERIFY_MODE_ALL;
    gDvm.dexOptMode = OPTIMIZE_MODE_VERIFIED;

    /*
     * Default execution mode.
     *
     * This should probably interact with the mterp code somehow, e.g. if
     * we know we're using the "desktop" build we should probably be
     * using "portable" rather than "fast".
     */
    gDvm.executionMode = kExecutionModeInterpFast;
}


/*
 * Handle a SIGBUS, which frequently occurs because somebody replaced an
 * optimized DEX file out from under us.
 */
static void busCatcher(int signum, siginfo_t* info, void* context)
{
    void* addr = info->si_addr;

    LOGE("Caught a SIGBUS (%d), addr=%p\n", signum, addr);

    /*
     * If we return at this point the SIGBUS just keeps happening, so we
     * remove the signal handler and allow it to kill us.  TODO: restore
     * the original, which points to a debuggerd stub; if we don't then
     * debuggerd won't be notified.
     */
    signal(SIGBUS, SIG_DFL);
}

/*
 * Configure signals.  We need to block SIGQUIT so that the signal only
 * reaches the dump-stack-trace thread.
 *
 * This can be disabled with the "-Xrs" flag.
 */
static void blockSignals()
{
    sigset_t mask;
    int cc;

    sigemptyset(&mask);
    sigaddset(&mask, SIGQUIT);
    sigaddset(&mask, SIGUSR1);      // used to initiate heap dump
    //sigaddset(&mask, SIGPIPE);
    cc = sigprocmask(SIG_BLOCK, &mask, NULL);
    assert(cc == 0);

    if (false) {
        /* TODO: save the old sigaction in a global */
        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sa.sa_sigaction = busCatcher;
        sa.sa_flags = SA_SIGINFO;
        cc = sigaction(SIGBUS, &sa, NULL);
        assert(cc == 0);
    }
}

/*
 * VM initialization.  Pass in any options provided on the command line.
 * Do not pass in the class name or the options for the class.
 *
 * Returns 0 on success.
 */
int dvmStartup(int argc, const char* const argv[], bool ignoreUnrecognized,
    JNIEnv* pEnv)
{
    int i, cc;

    assert(gDvm.initializing);

    LOGV("VM init args (%d):\n", argc);
    for (i = 0; i < argc; i++)
        LOGV("  %d: '%s'\n", i, argv[i]);

    setCommandLineDefaults();

    /* prep properties storage */
    if (!dvmPropertiesStartup(argc))
        goto fail;

    /*
     * Process the option flags (if any).
     */
    cc = dvmProcessOptions(argc, argv, ignoreUnrecognized);
    if (cc != 0) {
        if (cc < 0) {
            dvmFprintf(stderr, "\n");
            dvmUsage("dalvikvm");
        }
        goto fail;
    }

    /* configure signal handling */
    if (!gDvm.reduceSignals)
        blockSignals();

    /* mterp setup */
    LOGV("Using executionMode %d\n", gDvm.executionMode);
    dvmCheckAsmConstants();

    /*
     * Initialize components.
     */
    if (!dvmAllocTrackerStartup())
        goto fail;
    if (!dvmGcStartup())
        goto fail;
    if (!dvmThreadStartup())
        goto fail;
    if (!dvmInlineNativeStartup())
        goto fail;
    if (!dvmVerificationStartup())
        goto fail;
    if (!dvmInstanceofStartup())
        goto fail;
    if (!dvmClassStartup())
        goto fail;
    if (!dvmThreadObjStartup())
        goto fail;
    if (!dvmExceptionStartup())
        goto fail;
    if (!dvmStringInternStartup())
        goto fail;
    if (!dvmNativeStartup())
        goto fail;
    if (!dvmInternalNativeStartup())
        goto fail;
    if (!dvmJniStartup())
        goto fail;
    if (!dvmReflectStartup())
        goto fail;
#ifdef WITH_PROFILER
    if (!dvmProfilingStartup())
        goto fail;
#endif

    /* make sure we got these [can this go away?] */
    assert(gDvm.classJavaLangClass != NULL);
    assert(gDvm.classJavaLangObject != NULL);
    //assert(gDvm.classJavaLangString != NULL);
    assert(gDvm.classJavaLangThread != NULL);
    assert(gDvm.classJavaLangVMThread != NULL);
    assert(gDvm.classJavaLangThreadGroup != NULL);

    /*
     * Make sure these exist.  If they don't, we can return a failure out
     * of main and nip the whole thing in the bud.
     */
    static const char* earlyClasses[] = {
        "Ljava/lang/InternalError;",
        "Ljava/lang/StackOverflowError;",
        "Ljava/lang/UnsatisfiedLinkError;",
        "Ljava/lang/NoClassDefFoundError;",
        NULL
    };
    const char** pClassName;
    for (pClassName = earlyClasses; *pClassName != NULL; pClassName++) {
        if (dvmFindSystemClassNoInit(*pClassName) == NULL)
            goto fail;
    }

    /*
     * Miscellaneous class library validation.
     */
    if (!dvmValidateBoxClasses())
        goto fail;

    /*
     * Do the last bits of Thread struct initialization we need to allow
     * JNI calls to work.
     */
    if (!dvmPrepMainForJni(pEnv))
        goto fail;

    /*
     * Register the system native methods, which are registered through JNI.
     */
    if (!registerSystemNatives(pEnv))
        goto fail;

    /*
     * Do some "late" initialization for the memory allocator.  This may
     * allocate storage and initialize classes.
     */
    if (!dvmGcLateInit())
        goto fail;

    /*
     * At this point, the VM is in a pretty good state.  Finish prep on
     * the main thread (specifically, create a java.lang.Thread object to go
     * along with our Thread struct).  Note we will probably be executing
     * some interpreted class initializer code in here.
     */
    if (!dvmPrepMainThread())
        goto fail;

    /* general debugging setup */
    if (!dvmDebuggerStartup())
        goto fail;

    /*
     * Init for either zygote mode or non-zygote mode.  The key difference
     * is that we don't start any additional threads in Zygote mode.
     */
    if (gDvm.zygote) {
        if (!dvmInitZygote())
            goto fail;
    } else {
        if (!dvmInitAfterZygote())
            goto fail;
    }


#ifndef NDEBUG
    dvmTestHash();
#endif

    assert(!dvmCheckException(dvmThreadSelf()));
    gDvm.initExceptionCount = 0;

    return 0;

fail:
    dvmShutdown();
    return 1;
}

/*
 * Register java.* natives from our class libraries.  We need to do
 * this after we're ready for JNI registration calls, but before we
 * do any class initialization.
 *
 * If we get this wrong, we will blow up in the ThreadGroup class init if
 * interpreted code makes any reference to System.  It will likely do this
 * since it wants to do some java.io.File setup (e.g. for static in/out/err).
 *
 * We need to have gDvm.initializing raised here so that JNI FindClass
 * won't try to use the system/application class loader.
 */
static bool registerSystemNatives(JNIEnv* pEnv)
{
    Thread* self;

    /* main thread is always first in list */
    self = gDvm.threadList;

    /* must set this before allowing JNI-based method registration */
    self->status = THREAD_NATIVE;

    if (jniRegisterSystemMethods(pEnv) < 0) {
        LOGW("jniRegisterSystemMethods failed\n");
        return false;
    }

    /* back to run mode */
    self->status = THREAD_RUNNING;

    return true;
}


/*
 * Do zygote-mode-only initialization.
 */
static bool dvmInitZygote(void)
{
    /* zygote goes into its own process group */
    setpgid(0,0);

    return true;
}

/*
 * Do non-zygote-mode initialization.  This is done during VM init for
 * standard startup, or after a "zygote fork" when creating a new process.
 */
bool dvmInitAfterZygote(void)
{
    u8 startHeap, startQuit, startJdwp;
    u8 endHeap, endQuit, endJdwp;
    
    startHeap = dvmGetRelativeTimeUsec();

    /*
     * Post-zygote heap initialization, including starting
     * the HeapWorker thread.
     */
    if (!dvmGcStartupAfterZygote())
        return false;

    endHeap = dvmGetRelativeTimeUsec();
    startQuit = dvmGetRelativeTimeUsec();

    /* start signal catcher thread that dumps stacks on SIGQUIT */
    if (!gDvm.reduceSignals && !gDvm.noQuitHandler) {
        if (!dvmSignalCatcherStartup())
            return false;
    }

    /* start stdout/stderr copier, if requested */
    if (gDvm.logStdio) {
        if (!dvmStdioConverterStartup())
            return false;
    }

    endQuit = dvmGetRelativeTimeUsec();
    startJdwp = dvmGetRelativeTimeUsec();

    /*
     * Start JDWP thread.  If the command-line debugger flags specified
     * "suspend=y", this will pause the VM.  We probably want this to
     * come last.
     */
    if (!dvmInitJDWP()) {
        LOGD("JDWP init failed; continuing anyway\n");
    }

    endJdwp = dvmGetRelativeTimeUsec();

    LOGV("thread-start heap=%d quit=%d jdwp=%d total=%d usec\n",
        (int)(endHeap-startHeap), (int)(endQuit-startQuit),
        (int)(endJdwp-startJdwp), (int)(endJdwp-startHeap));

    return true;
}

/*
 * Prepare for a connection to a JDWP-compliant debugger.
 *
 * Note this needs to happen fairly late in the startup process, because
 * we need to have all of the java.* native methods registered (which in
 * turn requires JNI to be fully prepped).
 *
 * There are several ways to initialize:
 *   server=n
 *     We immediately try to connect to host:port.  Bail on failure.  On
 *     success, send VM_START (suspending the VM if "suspend=y").
 *   server=y suspend=n
 *     Passively listen for a debugger to connect.  Return immediately.
 *   server=y suspend=y
 *     Wait until debugger connects.  Send VM_START ASAP, suspending the
 *     VM after the message is sent.
 *
 * This gets more complicated with a nonzero value for "timeout".
 */
static bool dvmInitJDWP(void)
{
    assert(!gDvm.zygote);

#ifndef WITH_DEBUGGER
    LOGI("Debugger support not compiled into VM\n");
    return false;
#endif

    /*
     * Init JDWP if the debugger is enabled.  This may connect out to a
     * debugger, passively listen for a debugger, or block waiting for a
     * debugger.
     */
    if (gDvm.jdwpAllowed && gDvm.jdwpConfigured) {
        JdwpStartupParams params;

        if (gDvm.jdwpHost != NULL) {
            if (strlen(gDvm.jdwpHost) >= sizeof(params.host)-1) {
                LOGE("ERROR: hostname too long: '%s'\n", gDvm.jdwpHost);
                return false;
            }
            strcpy(params.host, gDvm.jdwpHost);
        } else {
            params.host[0] = '\0';
        }
        params.transport = gDvm.jdwpTransport;
        params.server = gDvm.jdwpServer;
        params.suspend = gDvm.jdwpSuspend;
        params.port = gDvm.jdwpPort;

        gDvm.jdwpState = dvmJdwpStartup(&params);
        if (gDvm.jdwpState == NULL) {
            LOGW("WARNING: debugger thread failed to initialize\n");
            /* TODO: ignore? fail? need to mimic "expected" behavior */
        }
    }

    /*
     * If a debugger has already attached, send the "welcome" message.  This
     * may cause us to suspend all threads.
     */
    if (dvmJdwpIsActive(gDvm.jdwpState)) {
        //dvmChangeStatus(NULL, THREAD_RUNNING);
        if (!dvmJdwpPostVMStart(gDvm.jdwpState, gDvm.jdwpSuspend)) {
            LOGW("WARNING: failed to post 'start' message to debugger\n");
            /* keep going */
        }
        //dvmChangeStatus(NULL, THREAD_NATIVE);
    }

    return true;
}

/*
 * An alternative to JNI_CreateJavaVM/dvmStartup that does the first bit
 * of initialization and then returns with "initializing" still set.  (Used
 * by DexOpt command-line utility.)
 *
 * Attempting to use JNI or internal natives will fail.  It's best if
 * no bytecode gets executed, which means no <clinit>, which means no
 * exception-throwing.  We check the "initializing" flag anyway when
 * throwing an exception, so we can insert some code that avoids chucking
 * an exception when we're optimizing stuff.
 *
 * Returns 0 on success.
 */
int dvmPrepForDexOpt(const char* bootClassPath, DexOptimizerMode dexOptMode,
    DexClassVerifyMode verifyMode, int dexoptFlags)
{
    gDvm.initializing = true;
    gDvm.optimizing = true;

    /* configure signal handling */
    blockSignals();

    /* set some defaults */
    setCommandLineDefaults();
    free(gDvm.bootClassPathStr);
    gDvm.bootClassPathStr = strdup(bootClassPath);

    /* set opt/verify modes */
    gDvm.dexOptMode = dexOptMode;
    gDvm.classVerifyMode = verifyMode;
    gDvm.generateRegisterMaps = (dexoptFlags & DEXOPT_GEN_REGISTER_MAPS) != 0;

    /*
     * Initialize the heap, some basic thread control mutexes, and
     * get the bootclasspath prepped.
     *
     * We can't load any classes yet because we may not yet have a source
     * for things like java.lang.Object and java.lang.Class.
     */
    if (!dvmGcStartup())
        goto fail;
    if (!dvmThreadStartup())
        goto fail;
    if (!dvmInlineNativeStartup())
        goto fail;
    if (!dvmVerificationStartup())
        goto fail;
    if (!dvmInstanceofStartup())
        goto fail;
    if (!dvmClassStartup())
        goto fail;

    /*
     * We leave gDvm.initializing set to "true" so that, if we're not
     * able to process the "core" classes, we don't go into a death-spin
     * trying to throw a "class not found" exception.
     */

    return 0;

fail:
    dvmShutdown();
    return 1;
}


/*
 * All threads have stopped.  Finish the shutdown procedure.
 *
 * We can also be called if startup fails partway through, so be prepared
 * to deal with partially initialized data.
 *
 * Free any storage allocated in gGlobals.
 *
 * We can't dlclose() shared libs we've loaded, because it's possible a
 * thread not associated with the VM is running code in one.
 *
 * This is called from the JNI DestroyJavaVM function, which can be
 * called from any thread.  (In practice, this will usually run in the
 * same thread that started the VM, a/k/a the main thread, but we don't
 * want to assume that.)
 */
void dvmShutdown(void)
{
    LOGV("VM shutting down\n");

    if (CALC_CACHE_STATS)
        dvmDumpAtomicCacheStats(gDvm.instanceofCache);

    /*
     * Stop our internal threads.
     */
    dvmHeapWorkerShutdown();

    if (gDvm.jdwpState != NULL)
        dvmJdwpShutdown(gDvm.jdwpState);
    free(gDvm.jdwpHost);
    gDvm.jdwpHost = NULL;
    free(gDvm.stackTraceFile);
    gDvm.stackTraceFile = NULL;

    /* tell signal catcher to shut down if it was started */
    dvmSignalCatcherShutdown();

    /* shut down stdout/stderr conversion */
    dvmStdioConverterShutdown();

    /*
     * Kill any daemon threads that still exist.  Actively-running threads
     * are likely to crash the process if they continue to execute while
     * the VM shuts down.
     */
    dvmSlayDaemons();

    LOGD("VM cleaning up\n");

    dvmDebuggerShutdown();
    dvmReflectShutdown();
#ifdef WITH_PROFILER
    dvmProfilingShutdown();
#endif
    dvmJniShutdown();
    dvmStringInternShutdown();
    dvmExceptionShutdown();
    dvmThreadShutdown();
    dvmClassShutdown();
    dvmVerificationShutdown();
    dvmInstanceofShutdown();
    dvmInlineNativeShutdown();
    dvmGcShutdown();
    dvmAllocTrackerShutdown();
    dvmPropertiesShutdown();

    /* these must happen AFTER dvmClassShutdown has walked through class data */
    dvmNativeShutdown();
    dvmInternalNativeShutdown();

    free(gDvm.bootClassPathStr);
    free(gDvm.classPathStr);

    freeAssertionCtrl();

    /*
     * We want valgrind to report anything we forget to free as "definitely
     * lost".  If there's a pointer in the global chunk, it would be reported
     * as "still reachable".  Erasing the memory fixes this.
     *
     * This must be erased to zero if we want to restart the VM within this
     * process.
     */
    memset(&gDvm, 0xcd, sizeof(gDvm));
}


/*
 * fprintf() wrapper that calls through the JNI-specified vfprintf hook if
 * one was specified.
 */
int dvmFprintf(FILE* fp, const char* format, ...)
{
    va_list args;
    int result;

    va_start(args, format);
    if (gDvm.vfprintfHook != NULL)
        result = (*gDvm.vfprintfHook)(fp, format, args);
    else
        result = vfprintf(fp, format, args);
    va_end(args);

    return result;
}

/*
 * Abort the VM.  We get here on fatal errors.  Try very hard not to use
 * this; whenever possible, return an error to somebody responsible.
 */
void dvmAbort(void)
{
    LOGE("VM aborting\n");

    fflush(NULL);       // flush all open file buffers

    /* JNI-supplied abort hook gets right of first refusal */
    if (gDvm.abortHook != NULL)
        (*gDvm.abortHook)();

    /*
     * If we call abort(), all threads in the process receives a SIBABRT.
     * debuggerd dumps the stack trace of the main thread, whether or not
     * that was the thread that failed.
     *
     * By stuffing a value into a bogus address, we cause a segmentation
     * fault in the current thread, and get a useful log from debuggerd.
     * We can also trivially tell the difference between a VM crash and
     * a deliberate abort by looking at the fault address.
     */
    *((char*)0xdeadd00d) = 38;
    abort();

    /* notreached */
}
