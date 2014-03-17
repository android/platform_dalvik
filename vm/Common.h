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
 * Common defines for all Dalvik code.
 */
#ifndef DALVIK_COMMON_H_
#define DALVIK_COMMON_H_

#ifndef LOG_TAG
# define LOG_TAG "dalvikvm"
#endif

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <assert.h>
#include <inttypes.h>
#include "cutils/log.h"

#if defined(HAVE_ENDIAN_H)
# include <endian.h>
#else /*not HAVE_ENDIAN_H*/
# define __BIG_ENDIAN 4321
# define __LITTLE_ENDIAN 1234
# if defined(HAVE_LITTLE_ENDIAN)
#  define __BYTE_ORDER __LITTLE_ENDIAN
# else
#  define __BYTE_ORDER __BIG_ENDIAN
# endif
#endif /*not HAVE_ENDIAN_H*/



#if !defined(NDEBUG) && defined(WITH_DALVIK_ASSERT)
# undef assert
// assert_address is defined instead of the literal 39 to avoid errors with undefined behaviour when
// assigning to the same address multiple times in the same expression.
inline int *assert_address() { return (int*)39;}
# define assert(x) \
    ((x) ? ((void)0) : (ALOGE("ASSERT FAILED (%s:%d): %s", \
        __FILE__, __LINE__, #x), *assert_address()=39, (void)0) )
#endif

#define MIN(x,y) (((x) < (y)) ? (x) : (y))
#define MAX(x,y) (((x) > (y)) ? (x) : (y))

#define LIKELY(exp) (__builtin_expect((exp) != 0, true))
#define UNLIKELY(exp) (__builtin_expect((exp) != 0, false))

#define ALIGN_UP(x, n) (((size_t)(x) + (n) - 1) & ~((n) - 1))
#define ALIGN_DOWN(x, n) ((size_t)(x) & -(n))
#define ALIGN_UP_TO_PAGE_SIZE(p) ALIGN_UP(p, SYSTEM_PAGE_SIZE)
#define ALIGN_DOWN_TO_PAGE_SIZE(p) ALIGN_DOWN(p, SYSTEM_PAGE_SIZE)

/*
 * CLZ_U4 handles 32bit unsigned integers.
 * CLZ handles the native register size, 32 or 64 bits.
 */
#ifdef _LP64
#define CLZ(x) __builtin_clzl(x)
#define CLZ_U4(x) __builtin_clz(x)
#else
#define CLZ(x) __builtin_clz(x)
#define CLZ_U4(x) __builtin_clz(x)
#endif

/*
 * If "very verbose" logging is enabled, make it equivalent to ALOGV.
 * Otherwise, make it disappear.
 *
 * Define this above the #include "Dalvik.h" to enable for only a
 * single file.
 */
/* #define VERY_VERBOSE_LOG */
#if defined(VERY_VERBOSE_LOG)
# define LOGVV      ALOGV
# define IF_LOGVV() IF_ALOGV()
#else
# define LOGVV(...) ((void)0)
# define IF_LOGVV() if (false)
#endif


/*
 * These match the definitions in the VM specification.
 */
typedef uint8_t             u1;
typedef uint16_t            u2;
typedef uint32_t            u4;
typedef uint64_t            u8;
typedef int8_t              s1;
typedef int16_t             s2;
typedef int32_t             s4;
typedef int64_t             s8;

#if defined(_LP64)
# define WITH_COMPREFS
#endif

#ifdef _LP64
typedef u8 StackSlot;
#else
typedef u4 StackSlot;
#endif

/*
 * Storage for primitive types and object references.
 *
 * Some parts of the code (notably object field access) assume that values
 * are "left aligned", i.e. given "JValue jv", "jv.i" and "*((s4*)&jv)"
 * yield the same result.  This seems to be guaranteed by gcc on big- and
 * little-endian systems.
 */
struct Object;
struct ClassObject;

/*
 * Definitions for Compressed references support.
 *
 * A forward definition is needed for dvmRefExpandClazzGlobal
 * as it may access gDvm.heapBase before that global is available.
 */
#if defined(WITH_COMPREFS)

typedef u4 ObjectRef;
typedef u4 ClassObjectRef;

#define NULLREF 0

inline ClassObject* dvmRefExpandClazzGlobal(const u4 objectRef);

#else

typedef Object* ObjectRef;
typedef ClassObject* ClassObjectRef;

inline ClassObject* dvmRefExpandClazzGlobal(const ClassObject* objectRef);

#define NULLREF NULL

#endif


//"hl" for object references on the heap.
union JValue {
#if defined(HAVE_LITTLE_ENDIAN)
    u1      z;
    s1      b;
    u2      c;
    s2      s;
    s4      i;
    s8      j;
    float   f;
    double  d;
    Object* l;
    ObjectRef hl;
#endif
#if defined(HAVE_BIG_ENDIAN)
#if !defined(_LP64)
    struct {
        u1    _z[3];
        u1    z;
    };
    struct {
        s1    _b[3];
        s1    b;
    };
    struct {
        u2    _c;
        u2    c;
    };
    struct {
        s2    _s;
        s2    s;
    };
    s4      i;
#else
    struct {
        u1    _z[7];
        u1    z;
    };
    struct {
        s1    _b[7];
        s1    b;
    };
    struct {
        u2    _c[3];
        u2    c;
    };
    struct {
        s2    _s[3];
        s2    s;
    };
    struct {
        s4 _i;
        s4 i;
    };
#endif
    s8      j;
    float   f;
    double  d;
    void*   l;
    ObjectRef hl;
#endif
};

#define OFFSETOF_MEMBER(t, f)         \
  (reinterpret_cast<char*>(           \
     &reinterpret_cast<t*>(16)->f) -  \
   reinterpret_cast<char*>(16))

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#endif  // DALVIK_COMMON_H_
