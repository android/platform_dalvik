/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * Test that dvmPlatformInvoke marshals parameters correctly for JNI.
 * This is done independently of any actual classes.
 *
 * Each test has the form:
 *      bool test<shorty><type><extra>(<jnihint>)
 *
 *  <shorty> is the short signature of the JNI function being called.
 *  <type> is Static for a static method, Instance for an instance method.
 *  <jnihint> is the hint value passed directly to dvmPlatformInvoke. It must
 *  contain the correct return type.
 *  <extra> is an optional part which is used to distinguish tests which
 *  would otherwise end up having the same name.
 *
 *  For each test function there is a function with the same name, but with
 *  "Subject" appended. It's parameter must match the JNI signature.
 *
 */


#include "Dalvik.h"
#include "Native.h"
#include <string.h>

#ifndef NDEBUG

#if defined(_LP64)
# define DALVIK_JNI_RETURN_OBJECT DALVIK_JNI_RETURN_S8
#else
# define DALVIK_JNI_RETURN_OBJECT DALVIK_JNI_RETURN_S4
#endif

#define MAKE_HINT(returnType, platformHint) (((DALVIK_JNI_RETURN_##returnType) << DALVIK_JNI_RETURN_SHIFT) \
        | platformHint)


static void *JNIenv = &JNIenv;
static ClassObject  *  JNIclazz = (ClassObject*)(void*) &JNIclazz;
static void *JNIobj = &JNIobj;
static bool subjectResult = true;
static void *obj1 = &obj1;
static void *obj2 = &obj2;

/* extern "C" void dvmPlatformInvoke(void* pEnv, ClassObject* clazz, int argInfo,
    int argc, const StackSlot* argv, const char* signature, void* func, JValue* pResult);
 */

extern "C" {
static void testVInstanceSubject(void* env, void* obj) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }


    if( obj != JNIobj) {
        ALOGE("TestPlatformInvoke obj != JNIobj, was %p expected %p", obj, JNIobj);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);
}
}

static bool testVInstance(unsigned int jniHint) {
    StackSlot argv[1] = {(StackSlot) JNIobj} ;
    JValue result;

    dvmPlatformInvoke(JNIenv, NULL, jniHint
            , 1, &argv[0], "V", (void*) &testVInstanceSubject, &result);

    return subjectResult;
}


extern "C" {
static void testVIIIIIIIIStaticSubject(void* env, void* clazz, jint x0,
                    jint x1, jint x2, jint x3, jint x4, jint x5, jint x6, jint x7) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    if( x0 != 0) {
        ALOGE("TestPlatformInvoke x0 != 0, was %d", x0);
        ok = false;
    }
    if( x1 != 1) {
        ALOGE("TestPlatformInvoke x1 != 1, was %d", x1);
        ok = false;
    }
    if( x2 != 2) {
        ALOGE("TestPlatformInvoke x2 != 2, was %d", x2);
        ok = false;
    }
    if( x3 != 3) {
        ALOGE("TestPlatformInvoke x3 != 3, was %d", x3);
        ok = false;
    }
    if( x4 != 4) {
        ALOGE("TestPlatformInvoke x4 != 4, was %d", x4);
        ok = false;
    }
    if( x5 != 5) {
        ALOGE("TestPlatformInvoke x5 != 5, was %d", x5);
        ok = false;
    }
    if( x6 != 6) {
        ALOGE("TestPlatformInvoke x6 != 6, was %d", x6);
        ok = false;
    }
    if( x7 != 7) {
        ALOGE("TestPlatformInvoke x7 != 7, was %d", x7);
        ok = false;
    }
    subjectResult = ok;
    assert(ok);
}
}


static bool testVIIIIIIIIStatic(unsigned int jniHint) {
    // "Noise" before and after arguments to show up any errors in
    // alignment of arguments read.
    StackSlot noise1[8] = {1111,1222,1333,1444,1555,1666,1777,1888};
    StackSlot argv[8] = { 0,1,2,3,4,5,6,7};
    StackSlot noise2[8] = {2111,2222,2333,2444,2555,2666,2777,2888};

    (void) noise1; (void) noise2;

    JValue result;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 8, &argv[0], "VIIIIIIII", (void*) &testVIIIIIIIIStaticSubject, &result);

    return subjectResult;
}


static const jdouble double1 = -234567.8912345;
extern "C" {
static jdouble testDStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);

    return double1;
}
}

static bool testDStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;
    bool resultOK = true;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "D", (void*) &testDStaticSubject, &result);

    if ( result.d != double1 ) {
           ALOGE("TestPlatformInvoke result.d != double1, was %g expected %g", result.d, double1);
           resultOK = false;
    }


    assert (resultOK);
    return resultOK && subjectResult;
}

static const jfloat float1 = -987654.321987;
extern "C" {
static jfloat testFStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);

    return float1;
}
}

static bool testFStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;
    bool resultOK=true;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "F", (void*) &testFStaticSubject, &result);

    if ( result.f != float1 ) {
           ALOGE("TestPlatformInvoke result.f != float1, was %f expected %f", result.f, float1);
           resultOK = false;
    }


    assert (resultOK);
    return resultOK && subjectResult;
}


static const jshort short1 = -32368;
extern "C" {
static jshort testSStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);

    return short1;
}
}

static bool testSStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;
    bool resultOK=true;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "S", (void*) &testSStaticSubject, &result);

    if ( result.s != short1 ) {
           ALOGE("TestPlatformInvoke result.s != short1, was %d expected %d", result.s, short1);
           resultOK = false;
    }


    assert (resultOK);
    return resultOK && subjectResult;
}

static const jint int1 = -2014563458;
extern "C" {
static jint testIStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);

    return int1;
}
}


static bool testIStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;
    bool resultOK=true;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "I", (void*) &testIStaticSubject, &result);

    if ( result.i != int1 ) {
           ALOGE("TestPlatformInvoke result.i != int1, was %d expected %d", result.i, int1);
           resultOK = false;
    }


    assert (resultOK);
    return resultOK && subjectResult;
}


static const jlong long1 = -2011684563458L;
extern "C" {
static jlong testJStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);

    return long1;
}
}

static bool testJStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;
    bool resultOK=true;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "J", (void*) &testJStaticSubject, &result);

    if ( result.j != long1 ) {
           ALOGE("TestPlatformInvoke result.j != long1, was %" PRId64 " expected %" PRId64, result.j, long1);
           resultOK = false;
    }


    assert (resultOK);
    return resultOK && subjectResult;
}


static const jlong long2 = 1947239473847L;
extern "C" {
static void testVJStaticSubject(void* env, void* clazz, jlong longVal) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    if( longVal != long2) {
        ALOGE("TestPlatformInvoke longVal != long2, was %" PRId64" expected %" PRId64, longVal, long2);
        ok = false;
    }
    subjectResult = ok;
    assert(ok);
}
}
static bool testVJStatic(unsigned int jniHint) {
    StackSlot argv[2];
    memcpy(&argv[0], &long2, sizeof(jlong));

    JValue result;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 1, &argv[0], "VJ", (void*) &testVJStaticSubject, &result);

    return subjectResult;
}


static jlong bigSix=66666666666L, bigSeven = 77777777777L;
extern "C" {
static void testIIIIIIJJStaticSubject(void* env, void* clazz, jint x0,
                    jint x1, jint x2, jint x3, jint x4, jint x5, jlong x6, jlong x7) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }

    if( x0 != 0) {
        ALOGE("TestPlatformInvoke x0 != 0, was %d", x0);
        ok = false;
    }
    if( x1 != 1) {
        ALOGE("TestPlatformInvoke x1 != 1, was %d", x1);
        ok = false;
    }
    if( x2 != 2) {
        ALOGE("TestPlatformInvoke x2 != 2, was %d", x2);
        ok = false;
    }
    if( x3 != 3) {
        ALOGE("TestPlatformInvoke x3 != 3, was %d", x3);
        ok = false;
    }
    if( x4 != 4) {
        ALOGE("TestPlatformInvoke x4 != 4, was %d", x4);
        ok = false;
    }
    if( x5 != 5) {
        ALOGE("TestPlatformInvoke x5 != 5, was %d", x5);
        ok = false;
    }
    if( x6 != bigSix) {
        ALOGE("TestPlatformInvoke x6 != %" PRId64 ", was %" PRId64, bigSix, x6);
        ok = false;
    }
    if( x7 != bigSeven) {
        ALOGE("TestPlatformInvoke x7 != %" PRId64 ", was %" PRId64, bigSeven, x7);
        ok = false;
    }
    subjectResult = ok;
    assert(ok);
}
}

static bool testIIIIIIJJStatic(unsigned int jniHint) {
    StackSlot noise1[8] = {1111,1222,1333,1444,1555,1666,1777,1888};
    StackSlot argv[10] = { 0,1,2,3,4,5,0,0,0,0};
    StackSlot noise2[8] = {2111,2222,2333,2444,2555,2666,2777,2888};

    memcpy(&argv[6], &bigSix, sizeof(jlong));
    memcpy(&argv[8], &bigSeven, sizeof(jlong));

    (void) noise1; (void) noise2;

    JValue result;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 10, &argv[0], "VIIIIIIJJ", (void*) &testIIIIIIJJStaticSubject, &result);

    return subjectResult;
}


static double double3 = 3333.33333333;
extern "C" {
static double testDFFFFFFFFFFInstanceSubject(void* env, void* obj,
    jfloat f0, jfloat f1, jfloat f2, jfloat f3, jfloat f4, jfloat f5, jfloat f6,
    jfloat f7, jfloat f8, jfloat f9) {
    bool ok = true;

    if( env != JNIenv) {
         ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
         ok = false;
     }


     if( obj != JNIobj) {
         ALOGE("TestPlatformInvoke obj != JNIobj, was %p expected %p", obj, JNIobj);
         ok = false;
     }

     if( f0 != 0.0f) {
         ALOGE("TestPlatformInvoke f0 != %f, was %f", 0.0f, f0);
         ok = false;
     }
     if( f1 != 1.1f) {
         ALOGE("TestPlatformInvoke f1 != %f, was %f", 1.1f, f1);
         ok = false;
     }
     if( f2 != 2.2f) {
         ALOGE("TestPlatformInvoke f2 != %f, was %f", 2.2f, f2);
         ok = false;
     }
     if( f3 != 3.3f) {
         ALOGE("TestPlatformInvoke f3 != %f, was %f", 3.3f, f3);
         ok = false;
     }
     if( f4 != 4.4f) {
         ALOGE("TestPlatformInvoke f4 != %f, was %f", 4.4f, f4);
         ok = false;
     }
     if( f5 != 5.5f) {
         ALOGE("TestPlatformInvoke f5 != %f, was %f", 5.5f, f5);
         ok = false;
     }
     if( f6 != 6.6f) {
         ALOGE("TestPlatformInvoke f6 != %f, was %f", 6.6f, f6);
         ok = false;
     }
     if( f7 != 7.7f) {
              ALOGE("TestPlatformInvoke f7 != %f, was %f", 7.7f, f7);
              ok = false;
     }
     if( f8 != 8.8f) {
              ALOGE("TestPlatformInvoke f8 != %f, was %f", 8.8f, f8);
              ok = false;
     }
     if( f9 != 9.9f) {
              ALOGE("TestPlatformInvoke f9 != %f, was %f", 9.9f, f9);
              ok = false;
     }

     subjectResult = ok;
     assert(ok);
     return double3;
}
}
static bool testDFFFFFFFFFFInstance(unsigned int jniHint) {
    StackSlot noise1[8] = {1111,1222,1333,1444,1555,1666,1777,1888};
    StackSlot argv[12];
    StackSlot noise2[8] = {2111,2222,2333,2444,2555,2666,2777,2888};
    jfloat values[10] = {0.0, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9};
    bool resultOK = true;
    (void) noise1; (void) noise2;

    argv[0] = (StackSlot) &JNIobj;

    for(int i =0; i <10; i++) {
        memcpy(&argv[i+1], &values[i], sizeof(jfloat));
    }
    JValue result;

    dvmPlatformInvoke(JNIenv, NULL, jniHint
            , 11,  &argv[0], "DFFFFFFFFFF", (void*) &testDFFFFFFFFFFInstanceSubject, &result);

    if ( result.d != double3 ) {
        ALOGE("TestPlatformInvoke result.j != long1, was %g expected %g", result.d, double3);
        resultOK = false;
    }

    return subjectResult && resultOK;
}


jint intValues[8] = {1010,1011,1012,1013,1014,1015,1016,1017};
jdouble doubleValues[2] = {1000.1, 1000.2 };
jlong longValues[2] = {1000001L, 1000002L};
jfloat floatValues[8] = {0.0, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7};

extern "C" {
static jobject testLZCFIFIFDIFIJFIFIFIFIDJInstanceSubject(void *env, void *obj,
        jboolean z, jchar c, jfloat f0, jint x0, jfloat f1, jint x1, jfloat f2,
        jdouble d0, jint x2, jfloat f3, jint x3, jlong j0, jfloat f4, jint x4,
        jfloat f5, jint x5, jfloat f6, jint x6, jfloat f7, jint x7, jdouble d1,
        jlong j1) {
    bool ok = true;

    if( env != JNIenv) {
         ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
         ok = false;
    }

    if (obj != JNIobj) {
        ALOGE("TestPlatformInvoke obj != JNIobj, was %p expected %p", obj,
                JNIobj);
        ok = false;
    }

    if (z != JNI_TRUE) {
        ALOGE("TestPlatformInvoke z != JNI_TRUE, was %d expected %d", z,
                JNI_TRUE);
        ok = false;
    }

    if (z != JNI_TRUE) {
        ALOGE("TestPlatformInvoke z != JNI_TRUE, was %d expected %d", z,
                JNI_TRUE);
        ok = false;
    }

    if (x0 != intValues[0]) {
        ALOGE("TestPlatformInvoke x0 != %d, was %d", intValues[0], x0);
        ok = false;
    }
    if (x1 != intValues[1]) {
        ALOGE("TestPlatformInvoke x1 != %d, was %d", intValues[1], x1);
        ok = false;
    }
    if (x2 != intValues[2]) {
        ALOGE("TestPlatformInvoke x2 != %d, was %d", intValues[2], x2);
        ok = false;
    }
    if (x3 != intValues[3]) {
        ALOGE("TestPlatformInvoke x3 != %d, was %d", intValues[3], x3);
        ok = false;
    }
    if (x4 != intValues[4]) {
        ALOGE("TestPlatformInvoke x4 != %d, was %d", intValues[4], x4);
        ok = false;
    }
    if (x5 != intValues[5]) {
        ALOGE("TestPlatformInvoke x5 != %d, was %d", intValues[5], x5);
        ok = false;
    }
    if (x6 != intValues[6]) {
        ALOGE("TestPlatformInvoke x6 != %d, was %d", intValues[6], x6);
        ok = false;
    }
    if (x7 != intValues[7]) {
        ALOGE("TestPlatformInvoke x7 != %d, was %d", intValues[7], x7);
        ok = false;
    }

    if (f0 != floatValues[0]) {
        ALOGE("TestPlatformInvoke f0 != %f, was %f", floatValues[0], f0);
        ok = false;
    }
    if (f1 != floatValues[1]) {
        ALOGE("TestPlatformInvoke f1 != %f, was %f", floatValues[1], f1);
        ok = false;
    }
    if (f2 != floatValues[2]) {
        ALOGE("TestPlatformInvoke f2 != %f, was %f", floatValues[2], f2);
        ok = false;
    }
    if (f3 != floatValues[3]) {
        ALOGE("TestPlatformInvoke f3 != %f, was %f", floatValues[3], f3);
        ok = false;
    }
    if (f4 != floatValues[4]) {
        ALOGE("TestPlatformInvoke f4 != %f, was %f", floatValues[4], f4);
        ok = false;
    }
    if (f5 != floatValues[5]) {
        ALOGE("TestPlatformInvoke f5 != %f, was %f", floatValues[5], f5);
        ok = false;
    }
    if (f6 != floatValues[6]) {
        ALOGE("TestPlatformInvoke f6 != %f, was %f", floatValues[6], f6);
        ok = false;
    }
    if (f7 != floatValues[7]) {
        ALOGE("TestPlatformInvoke f7 != %f, was %f", floatValues[7], f7);
        ok = false;
    }

    if (j0 != longValues[0]) {
        ALOGE("TestPlatformInvoke j0 != %" PRId64 ", was %" PRId64, longValues[0], j0);
        ok = false;
    }
    if (j1 != longValues[1]) {
        ALOGE("TestPlatformInvoke j1 != %" PRId64 ", was %" PRId64, longValues[1], j1);
        ok = false;
    }

    if (d0 != doubleValues[0]) {
        ALOGE("TestPlatformInvoke d0 != %g, was %g", doubleValues[0], d0);
        ok = false;
    }
    if (d1 != doubleValues[1]) {
        ALOGE("TestPlatformInvoke d1 != %g, was %g", doubleValues[1], d1);
        ok = false;
    }

    subjectResult = ok;
    assert(ok);
    return (jobject) obj2;
}
}


static bool testLZCFIFIFDIFIJFIFIFIFIDJInstance(unsigned int jniHint) {
    /*         012345678abcdf01234567 */
    StackSlot argv[28];

    bool resultOK = true;

    argv[0] = (StackSlot) &JNIobj;

    argv[1] = JNI_TRUE;
    argv[2] = 'X';
    memcpy(&argv[3], &floatValues[0], sizeof(jfloat));
    argv[4] = intValues[0];
    memcpy(&argv[5], &floatValues[1], sizeof(jfloat));
    argv[6] = intValues[1];
    memcpy(&argv[7], &floatValues[2], sizeof(jfloat));
    memcpy(&argv[8], &doubleValues[0], sizeof(jdouble));
    argv[0xa] = intValues[2];
    memcpy(&argv[0xb], &floatValues[3], sizeof(jfloat));
    argv[0xc] = intValues[3];
    memcpy(&argv[0xd], &longValues[0], sizeof(jlong));
    memcpy(&argv[0xf], &floatValues[4], sizeof(jfloat));
    argv[0x10] = intValues[4];
    memcpy(&argv[0x11], &floatValues[5], sizeof(jfloat));
    argv[0x12] = intValues[5];
    memcpy(&argv[0x13], &floatValues[6], sizeof(jfloat));
    argv[0x14] = intValues[6];
    memcpy(&argv[0x15], &floatValues[7], sizeof(jfloat));
    argv[0x16] = intValues[7];
    memcpy(&argv[0x17], &doubleValues[1], sizeof(jdouble));
    memcpy(&argv[0x19], &longValues[1], sizeof(jlong));
    JValue result;

    dvmPlatformInvoke(JNIenv, NULL, jniHint
            , 27,  &argv[0], "LZCFIFIFDIFIJFIFIFIFIDJ", (void*) &testLZCFIFIFDIFIJFIFIFIFIDJInstanceSubject, &result);

    if ( result.l != obj2 ) {
        ALOGE("TestPlatformInvoke result.l != obj2, was %p expected %p", result.l, obj2);
        resultOK = false;
    }

    return subjectResult && resultOK;
}

extern "C" {
static void testVStaticSubject(void* env, void* clazz) {
    bool ok = true;

    if( env != JNIenv) {
        ALOGE("TestPlatformInvoke env != JNIEnv, was %p expected %p", env, JNIenv);
        ok = false;
    }

    if( clazz != JNIclazz) {
        ALOGE("TestPlatformInvoke clazz != JNIclazz, was %p expected %p", clazz, JNIclazz);
        ok = false;
    }
    subjectResult = ok;
    assert(ok);
}
}
static bool testVStatic(unsigned int jniHint) {
    StackSlot argv[0];
    JValue result;

    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint
            , 0, &argv[0], "V", (void*) &testVStaticSubject, &result);

    return subjectResult;
}

#if defined(__aarch64__)

/* Following the AArch64 ABI, native functions returning intX_t, uintX_t, where
 * X is 8, 16, 32 should return their value in the lower bits of x0 and are not
 * expected to sign extend the return value: it is the caller which is expected
 * to do so. We use the function below to define multiple Java native functions
 * returning different types of signed/unsigned integers.
 */
static const uint64_t bigInt64 = 0x8000000080008080;
extern "C" {
static uint64_t testXStaticSubject(void* env, void* clazz) {
    return bigInt64;
}
}

#define DEFINE_SIGN_EXTEND_TEST(name, typeChr, retType, retTypeSign)    \
static bool name(unsigned int jniHint) {                                \
    StackSlot argv[0];                                                  \
    JValue result;                                                      \
    int64_t signExtended;                                               \
    bool ok = true;                                                     \
                                                                        \
    /* Test return byte. */                                             \
    dvmPlatformInvoke(JNIenv, JNIclazz, jniHint                         \
        , 0, &argv[0], typeChr, (void*) &testXStaticSubject, &result);  \
    signExtended = (int64_t) (retTypeSign) (retType) bigInt64;          \
    if (result.j != signExtended) {                                     \
        ALOGE("TestPlatformInvoke (%s) result.j != %#"PRIx64            \
              ", was %#"PRIx64, #name, (uintptr_t) signExtended,        \
              (uintptr_t) result.j);                                    \
        ok = false;                                                     \
    }                                                                   \
                                                                        \
    return ok;                                                          \
}

#define DEFINE_ZERO_EXTEND_TEST(name, typeChr, retType)                 \
    DEFINE_SIGN_EXTEND_TEST(name, typeChr, retType, retType)

DEFINE_SIGN_EXTEND_TEST(testBStaticSignExtend, "B", uint8_t, int8_t);
DEFINE_SIGN_EXTEND_TEST(testSStaticSignExtend, "S", uint16_t, int16_t);
DEFINE_SIGN_EXTEND_TEST(testIStaticSignExtend, "I", uint32_t, int32_t);
DEFINE_ZERO_EXTEND_TEST(testCStaticZeroExtend, "C", uint16_t);
#endif

/*
 * Entry point for tests.
 */
bool dvmTestPlatformInvoke(void) {
    bool overallResult = true;

    // x86 requires hints.
#if !defined(__i386__)
    overallResult &= testVStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(VOID, 0));
    overallResult &= testVInstance(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(VOID, 0));
    overallResult &= testVIIIIIIIIStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(VOID, 0));
    overallResult &= testDStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(DOUBLE, 0));
    overallResult &= testFStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(FLOAT, 0));
    overallResult &= testSStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(S2, 0));
    overallResult &= testIStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(S4, 0));
    overallResult &= testJStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(S8, 0));
    overallResult &= testVJStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(VOID, 0));
    overallResult &= testIIIIIIJJStatic(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(VOID, 0));
    overallResult &= testDFFFFFFFFFFInstance(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(DOUBLE, 0));
    overallResult &= testLZCFIFIFDIFIJFIFIFIFIDJInstance(DALVIK_JNI_NO_ARG_INFO | MAKE_HINT(OBJECT, 0));
#endif
    /*
     * Same tests as above, but using hints, where available.
     * aarch64 has simple hints we can reproduce.
     */
#if defined(__aarch64__)
    overallResult &= testVStatic(MAKE_HINT(VOID, 0));
    overallResult &= testVInstance(MAKE_HINT(VOID, 0));
    overallResult &= testVIIIIIIIIStatic(MAKE_HINT(VOID, 2));
    overallResult &= testDStatic(MAKE_HINT(DOUBLE, 0));
    overallResult &= testFStatic(MAKE_HINT(FLOAT, 0));
    overallResult &= testSStatic(MAKE_HINT(S2, 0));
    overallResult &= testIStatic(MAKE_HINT(S4, 0));
    overallResult &= testJStatic(MAKE_HINT(S8, 0));
    overallResult &= testVJStatic(MAKE_HINT(VOID, 0));
    overallResult &= testIIIIIIJJStatic(MAKE_HINT(VOID, 4));
    overallResult &= testDFFFFFFFFFFInstance(MAKE_HINT(DOUBLE, 2));
    overallResult &= testLZCFIFIFDIFIJFIFIFIFIDJInstance(MAKE_HINT(OBJECT, 8));

    /* Additional AArch64-specific tests. */
    overallResult &= testBStaticSignExtend(MAKE_HINT(S1, 0));
    overallResult &= testSStaticSignExtend(MAKE_HINT(S2, 0));
    overallResult &= testIStaticSignExtend(MAKE_HINT(S4, 0));
    overallResult &= testCStaticZeroExtend(MAKE_HINT(U2, 0));

#elif defined(__i386__)
    overallResult &= testVStatic(MAKE_HINT(VOID, 0));
    overallResult &= testVInstance(MAKE_HINT(VOID, 0));
    overallResult &= testVIIIIIIIIStatic(MAKE_HINT(VOID, 8));
    overallResult &= testDStatic(MAKE_HINT(DOUBLE, 0));
    overallResult &= testFStatic(MAKE_HINT(FLOAT, 0));
    overallResult &= testSStatic(MAKE_HINT(S2, 0));
    overallResult &= testIStatic(MAKE_HINT(S4, 0));
    overallResult &= testJStatic(MAKE_HINT(S8, 0));
    overallResult &= testVJStatic(MAKE_HINT(VOID, 2));
    overallResult &= testIIIIIIJJStatic(MAKE_HINT(VOID, 10));
    overallResult &= testDFFFFFFFFFFInstance(MAKE_HINT(DOUBLE, 10));
    overallResult &= testLZCFIFIFDIFIJFIFIFIFIDJInstance(MAKE_HINT(OBJECT, 26));
#endif

    return overallResult;
}

#endif // NDEBUG

