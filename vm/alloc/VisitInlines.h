/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef DALVIK_ALLOC_VISITINLINES_H_
#define DALVIK_ALLOC_VISITINLINES_H_

/*
 * Visits the instance fields of a class or data object.
 */
static void visitFields(Visitor *visitor, Object *obj, void *arg)
{
    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    ClassObject *expandedClazz = dvmRefExpandClazzGlobal(obj->clazz);
    if (expandedClazz->refOffsets != CLASS_WALK_SUPER) {
        u4 refOffsets = expandedClazz->refOffsets;
        while (refOffsets != 0) {
            size_t rshift = CLZ_U4(refOffsets);
            size_t offset = CLASS_OFFSET_FROM_CLZ(rshift);
            ObjectRef*ref = (ObjectRef*)BYTE_OFFSET(obj, offset);
            Object *object = dvmRefExpandGlobal(*ref);
            (*visitor)(&object, arg);
            refOffsets &= ~(CLASS_HIGH_BIT >> rshift);
        }
    } else {
        for (ClassObject *clazz = expandedClazz;
             clazz != NULL;
             clazz = clazz->super) {
            InstField *field = clazz->ifields;
            for (int i = 0; i < clazz->ifieldRefCount; ++i, ++field) {
                size_t offset = field->byteOffset;
                ObjectRef *ref = (ObjectRef*)BYTE_OFFSET(obj, offset);
                Object *object = dvmRefExpandGlobal(*ref);
                (*visitor)(&object, arg);
            }
        }
    }
}

/*
 * Visits the static fields of a class object.
 */
static void visitStaticFields(Visitor *visitor, ClassObject *clazz,
                              void *arg)
{
    assert(visitor != NULL);
    assert(clazz != NULL);
    for (int i = 0; i < clazz->sfieldCount; ++i) {
        char ch = clazz->sfields[i].signature[0];
        if (ch == '[' || ch == 'L') {
            Object *ref = dvmRefExpandGlobal(clazz->sfields[i].value.hl);
            (*visitor)(&ref, arg);
        }
    }
}

/*
 * Visit the interfaces of a class object.
 */
static void visitInterfaces(Visitor *visitor, ClassObject *clazz,
                            void *arg)
{
    assert(visitor != NULL);
    assert(clazz != NULL);
    for (int i = 0; i < clazz->interfaceCount; ++i) {
        (*visitor)(&clazz->interfaces[i], arg);
    }
}

/*
 * Visits all the references stored in a class object instance.
 */
static void visitClassObject(Visitor *visitor, Object *obj, void *arg)
{
    ClassObject *asClass;

    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    assert(!strcmp(dvmRefExpandClazzGlobal(obj->clazz)->descriptor, "Ljava/lang/Class;"));
    Object *ref = dvmRefExpandGlobal(obj->clazz);
    (*visitor)(&ref, arg);
    asClass = (ClassObject *)obj;
    if (IS_CLASS_FLAG_SET(asClass, CLASS_ISARRAY)) {
        (*visitor)(&asClass->elementClass, arg);
    }
    if (asClass->status > CLASS_IDX) {
        (*visitor)(&asClass->super, arg);
    }
    (*visitor)(&asClass->classLoader, arg);
    visitFields(visitor, obj, arg);
    visitStaticFields(visitor, asClass, arg);
    if (asClass->status > CLASS_IDX) {
      visitInterfaces(visitor, asClass, arg);
    }
}

/*
 * Visits the class object and, if the array is typed as an object
 * array, all of the array elements.
 */
static void visitArrayObject(Visitor *visitor, Object *obj, void *arg)
{
    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    Object *ref = dvmRefExpandGlobal(obj->clazz);
    (*visitor)(&ref, arg);
    if (IS_CLASS_FLAG_SET(dvmRefExpandClazzGlobal(obj->clazz), CLASS_ISOBJECTARRAY)) {
        ArrayObject *array = (ArrayObject *)obj;
        ObjectRef*contents = (ObjectRef*)(void *)array->contents;
        for (size_t i = 0; i < array->length; ++i) {
            Object *ref = dvmRefExpandGlobal(contents[i]);
            (*visitor)(&ref, arg);
        }
    }
}

/*
 * Visits the class object and reference typed instance fields of a
 * data object.
 */
static void visitDataObject(Visitor *visitor, Object *obj, void *arg)
{
    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    Object *ref = dvmRefExpandGlobal(obj->clazz);
    (*visitor)(&ref, arg);
    visitFields(visitor, obj, arg);
}

/*
 * Like visitDataObject, but visits the hidden referent field that
 * belongings to the subclasses of java.lang.Reference.
 */
static void visitReferenceObject(Visitor *visitor, Object *obj, void *arg)
{
    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    visitDataObject(visitor, obj, arg);
    size_t offset = gDvm.offJavaLangRefReference_referent;
    Object *ref = dvmRefExpandGlobal(*((ObjectRef*)BYTE_OFFSET(obj, offset)));

    (*visitor)(&ref, arg);
}

/*
 * Visits all of the reference stored in an object.
 */
static void visitObject(Visitor *visitor, Object *obj, void *arg)
{
    assert(visitor != NULL);
    assert(obj != NULL);
    assert(obj->clazz != NULLREF);
    if (dvmIsClassObject(obj)) {
        visitClassObject(visitor, obj, arg);
    } else if (IS_CLASS_FLAG_SET(dvmRefExpandClazzGlobal(obj->clazz), CLASS_ISARRAY)) {
        visitArrayObject(visitor, obj, arg);
    } else if (IS_CLASS_FLAG_SET(dvmRefExpandClazzGlobal(obj->clazz), CLASS_ISREFERENCE)) {
        visitReferenceObject(visitor, obj, arg);
    } else {
        visitDataObject(visitor, obj, arg);
    }
}

#endif  // DALVIK_ALLOC_VISITINLINES_H_
