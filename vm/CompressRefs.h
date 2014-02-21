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
 * This file contains the definitions and inlines required for compressed
 * references. Compressed references are enabled when the macro "WITH_COMPREFS"
 * is defined. The inlines will pass references through unchanged when
 * WITH_COMPREFS is not defined, and will be optimised out.
 *
 * Compressed references are object references that are smaller than the native
 * pointer size.
 *
 * This implementation of compressed references depends on the heap being
 * aligned on a 4GB boundary. This allows an object reference to be transformed
 * to a 64bit pointer by ORing the heap base pointer. A 64 bit pointer can be
 * compressed by discarding the top 32 bits.
 *
 * null references are 0 compressed, and when expanded to 64 bits are equal to
 * the heap base address. The heap is managed such that no object appears at the
 * 0 address (compressed).
 *
 * As a rule, there are no compressed object references stored in structures
 * outside of the heap. On the heap all instance object references are
 * compressed, the ClassObject reference "Object.clazz" is compressed.
 * All of the ClassObject fields remain uncompressed, including object
 * references in static fields.
 *
 * The inlines all start "dvmRef". The inlines including "Global" don't require
 * a heap base value to be passed, and instead use the heap base stored in a
 * global variable. The inlines with "Clazz" are to expand and compress the
 * Object.clazz field.
 *
 * bool dvmRefIsNull(Object* objectRef)
 *   Returns true if an object reference is null (0 or heap base).
 *
 * Object* dvmRefNormalize(Object* objectRef)
 *   Returns the object reference such that if it is 0 or heap base, 0 is
 *   returned.
 *   Required when passed to native code.
 *
 * Object* dvmRefExpand(ObjectRef objectRef, void *heapBase)
 *   Decompresses an object reference using the passed heap base.
 *
 * Object* dvmRefExpandGlobal(ObjectRef objectRef)
 *    Decompresses an object reference using a global variable.
 *
 * ClassObject* dvmRefExpandClazz(ClassObjectRef objectRef, void *heapBase)
 *    Decompresses a class reference.
 *
 * ClassObject* dvmRefExpandClazzGlobal(ClassObjectRef objectRef)
 *    Decompresses a class reference using a global variable.
 *
 * The dvmRefExpand... inlines all call dvmRefNormalize to ensure null is 0L.
 * TODOAarch64 remove the normalization from the inlines and insert it in the
 *      appropriate places.
 *
 *
 * When WITH_COMPREFS is not defined, all of the inlines switch to an
 * alternative definition where object references are passed through unmodified.
 *
 * Shifted Compressed References
 * =============================
 *
 * When the macro "WITH_SHIFTREFS" is defined, in addition to the ANDing and
 * ORing of the heap base with ordinary compressed references, references are
 * shifted right by 3 bits when compressed and left by 3 bits when expanded.
 * This is because objects are aligned on at least 8 byte boundaries. This
 * allows heaps of up to 32GB.
 * TODOAarch64 This is experimental - if the bottom bits are used this might not
 *      be worth implementing. For this reason we can't use the Copying GC.
 *
 */
#ifndef DALVIK_COMPRESSREFS_H_
#define DALVIK_COMPRESSREFS_H_

#include "Common.h"


#if defined(WITH_COMPREFS) && defined(WITH_SHIFTREFS)
// Mask and alignment for bottom 35 bits.
# define REF_ALIGNBITS 35

#elif defined(WITH_COMPREFS)
// Mask and alignment for bottom 32bits.
# define REF_ALIGNBITS 32
#endif

#if defined(WITH_COMPREFS)
# define REF_ALIGNMENT (1L << REF_ALIGNBITS)
# define REF_MASK (REF_ALIGNMENT - 1)

inline bool dvmRefIsNull(const Object* objectRef) {
    return ( ((uintptr_t) (objectRef)) & REF_MASK) == 0L;
}

inline Object* dvmRefNormalize(const Object* objectRef) {
    return dvmRefIsNull(objectRef) ? (Object*) NULL : const_cast<Object*>(objectRef);
}

#endif

#if defined(WITH_COMPREFS) && defined(WITH_SHIFTREFS)

inline u4 dvmRefCompress(const Object* objectRef) {
    return ( ((uintptr_t) objectRef) >> 3) & REF_MASK;
}

inline u4 dvmRefCompressClazz(const ClassObject* objectRef) {
    return ( ((uintptr_t) objectRef) >> 3) & REF_MASK;
}

inline Object* dvmRefExpand(const u4 objectRef, const void *heapBase) {
    return dvmRefNormalize((Object*)( ((uintptr_t) objectRef) << 3 | (uintptr_t) heapBase));
}

inline Object* dvmRefExpandGlobal(const u4 objectRef) {
    return dvmRefNormalize((Object*)( ((uintptr_t) objectRef) << 3 | (uintptr_t) gDvm.heapBase));
}

inline ClassObject* dvmRefExpandClazz(const u4 objectRef, const void *heapBase) {
    return (ClassObject*)dvmRefNormalize((Object*)( ((uintptr_t) objectRef) << 3 | (uintptr_t) heapBase));
}

inline ClassObject* dvmRefExpandClazzGlobal(const u4 objectRef) {
    return (ClassObject*)dvmRefNormalize((Object*)( ((uintptr_t) objectRef) << 3 | (uintptr_t) gDvm.heapBase));
}

#elif defined(WITH_COMPREFS)

inline u4 dvmRefCompress(const Object* objectRef) {
   return ((uintptr_t) objectRef) & REF_MASK;
}

inline u4 dvmRefCompressClazz(const ClassObject* objectRef) {
   return ((uintptr_t) objectRef) & REF_MASK;
}

inline Object* dvmRefExpand(const u4 objectRef, const void *heapBase) {
    return dvmRefNormalize( (Object*)(((uintptr_t) objectRef) | (uintptr_t)heapBase) );
}

inline Object* dvmRefExpandGlobal(const u4 objectRef) {
    return dvmRefNormalize( (Object*)(((uintptr_t) objectRef) | (uintptr_t)(gDvm.heapBase)) );
}

inline ClassObject* dvmRefExpandClazz(const u4 objectRef, const void *heapBase) {
    return (ClassObject*)dvmRefNormalize((Object*)(((uintptr_t) objectRef) | (uintptr_t)heapBase));
}

inline ClassObject* dvmRefExpandClazzGlobal(const u4 objectRef) {
    return (ClassObject*)dvmRefNormalize((Object*)(((uintptr_t) objectRef) | (uintptr_t)(gDvm.heapBase)) );
}

#else

// Pass through uncompressed references.

inline Object* dvmRefCompress(const Object* objectRef) {
    return const_cast<Object*>(objectRef);
}

inline ClassObject* dvmRefCompressClazz(const ClassObject* objectRef) {
   return const_cast<ClassObject*>(objectRef);
}

inline Object* dvmRefExpand(const Object* objectRef, const void *heapBase) {
    return const_cast<Object*>(objectRef);
}

inline Object* dvmRefExpandGlobal(const Object* objectRef) {
    return const_cast<Object*>(objectRef);
}

inline ClassObject* dvmRefExpandClazz(const ClassObject* objectRef, const void *heapBase) {
    return const_cast<ClassObject*>(objectRef);
}

inline ClassObject* dvmRefExpandClazzGlobal(const ClassObject* objectRef) {
    return const_cast<ClassObject*>(objectRef);
}

inline bool dvmRefIsNull(const Object* objectRef) {
    return  ((uintptr_t) objectRef) == 0L;
}

inline Object* dvmRefNormalize(const Object* objectRef) {
    return const_cast<Object*>(objectRef);
}
#endif

#endif // DALVIK_COMPRESSREFS_H_
