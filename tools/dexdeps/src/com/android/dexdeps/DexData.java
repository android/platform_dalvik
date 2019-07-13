/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.dexdeps;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Data extracted from a DEX file.
 */
public class DexData {
    private RandomAccessFile mDexFile;
    private HeaderItem mHeaderItem;
    private String[] mStrings;              // strings from string_data_*
    private TypeIdItem[] mTypeIds;
    private ProtoIdItem[] mProtoIds;
    private FieldIdItem[] mFieldIds;
    private MethodIdItem[] mMethodIds;
    private ClassDefItem[] mClassDefs;

    private byte tmpBuf[] = new byte[4];
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    /**
     * Constructs a new DexData for this file.
     */
    public DexData(RandomAccessFile raf) {
        mDexFile = raf;
    }

    /**
     * Loads the contents of the DEX file into our data structures.
     *
     * @throws IOException if we encounter a problem while reading
     * @throws DexDataException if the DEX contents look bad
     */
    public void load() throws IOException {
        parseHeaderItem();

        loadStrings();
        loadTypeIds();
        loadProtoIds();
        loadFieldIds();
        loadMethodIds();
        loadClassDefs();

        markInternalClasses();
    }

    /**
     * Verifies the given magic number.
     */
    private static boolean verifyMagic(byte[] magic) {
        return Arrays.equals(magic, HeaderItem.DEX_FILE_MAGIC_v035) ||
            Arrays.equals(magic, HeaderItem.DEX_FILE_MAGIC_v037) ||
            Arrays.equals(magic, HeaderItem.DEX_FILE_MAGIC_v038) ||
            Arrays.equals(magic, HeaderItem.DEX_FILE_MAGIC_v039);
    }

    /**
     * Parses the interesting bits out of the header.
     */
    void parseHeaderItem() throws IOException {
        mHeaderItem = new HeaderItem();

        seek(0);

        byte[] magic = new byte[8];
        readBytes(magic);
        if (!verifyMagic(magic)) {
            System.err.println("Magic number is wrong -- are you sure " +
                "this is a DEX file?");
            throw new DexDataException();
        }

        /*
         * Read the endian tag, so we properly swap things as we read
         * them from here on.
         */
        seek(8+4+20+4+4);
        mHeaderItem.endianTag = readInt();
        if (mHeaderItem.endianTag == HeaderItem.ENDIAN_CONSTANT) {
            /* do nothing */
        } else if (mHeaderItem.endianTag == HeaderItem.REVERSE_ENDIAN_CONSTANT){
            /* file is big-endian (!), reverse future reads */
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else {
            System.err.println("Endian constant has unexpected value " +
                Integer.toHexString(mHeaderItem.endianTag));
            throw new DexDataException();
        }

        seek(8+4+20);  // magic, checksum, signature
        ByteBuffer buffer = readByteBuffer(Integer.BYTES * 20);
        mHeaderItem.fileSize = buffer.getInt();
        mHeaderItem.headerSize = buffer.getInt();
        /*mHeaderItem.endianTag =*/ buffer.getInt();
        /*mHeaderItem.linkSize =*/ buffer.getInt();
        /*mHeaderItem.linkOff =*/ buffer.getInt();
        /*mHeaderItem.mapOff =*/ buffer.getInt();
        mHeaderItem.stringIdsSize = buffer.getInt();
        mHeaderItem.stringIdsOff = buffer.getInt();
        mHeaderItem.typeIdsSize = buffer.getInt();
        mHeaderItem.typeIdsOff = buffer.getInt();
        mHeaderItem.protoIdsSize = buffer.getInt();
        mHeaderItem.protoIdsOff = buffer.getInt();
        mHeaderItem.fieldIdsSize = buffer.getInt();
        mHeaderItem.fieldIdsOff = buffer.getInt();
        mHeaderItem.methodIdsSize = buffer.getInt();
        mHeaderItem.methodIdsOff = buffer.getInt();
        mHeaderItem.classDefsSize = buffer.getInt();
        mHeaderItem.classDefsOff = buffer.getInt();
        /*mHeaderItem.dataSize =*/ buffer.getInt();
        /*mHeaderItem.dataOff =*/ buffer.getInt();
    }

    /**
     * Loads the string table out of the DEX.
     *
     * First we read all of the string_id_items, then we read all of the
     * string_data_item.  Doing it this way should allow us to avoid
     * seeking around in the file.
     */
    void loadStrings() throws IOException {
        int count = mHeaderItem.stringIdsSize;
        int stringOffsets[] = new int[count];

        //System.out.println("reading " + count + " strings");

        seek(mHeaderItem.stringIdsOff);
        readByteBuffer(Integer.BYTES * count).asIntBuffer().get(stringOffsets);

        mStrings = new String[count];

        seek(stringOffsets[0]);
        for (int i = 0; i < count; i++) {
            seek(stringOffsets[i]);         // should be a no-op
            mStrings[i] = readString();
            //System.out.println("STR: " + i + ": " + mStrings[i]);
        }
    }

    /**
     * Loads the type ID list.
     */
    void loadTypeIds() throws IOException {
        int count = mHeaderItem.typeIdsSize;
        mTypeIds = new TypeIdItem[count];

        //System.out.println("reading " + count + " typeIds");
        seek(mHeaderItem.typeIdsOff);
        ByteBuffer buffer = readByteBuffer(Integer.BYTES * count);
        for (int i = 0; i < count; i++) {
            mTypeIds[i] = new TypeIdItem();
            mTypeIds[i].descriptorIdx = buffer.getInt();

            //System.out.println(i + ": " + mTypeIds[i].descriptorIdx +
            //    " " + mStrings[mTypeIds[i].descriptorIdx]);
        }
    }

    /**
     * Loads the proto ID list.
     */
    void loadProtoIds() throws IOException {
        int count = mHeaderItem.protoIdsSize;
        mProtoIds = new ProtoIdItem[count];

        //System.out.println("reading " + count + " protoIds");
        seek(mHeaderItem.protoIdsOff);
        ByteBuffer buffer = readByteBuffer(Integer.BYTES * 3 * count);

        /*
         * Read the proto ID items.
         */
        for (int i = 0; i < count; i++) {
            mProtoIds[i] = new ProtoIdItem();
            mProtoIds[i].shortyIdx = buffer.getInt();
            mProtoIds[i].returnTypeIdx = buffer.getInt();
            mProtoIds[i].parametersOff = buffer.getInt();

            //System.out.println(i + ": " + mProtoIds[i].shortyIdx +
            //    " " + mStrings[mProtoIds[i].shortyIdx]);
        }

        /*
         * Go back through and read the type lists.
         */
        for (int i = 0; i < count; i++) {
            ProtoIdItem protoId = mProtoIds[i];

            int offset = protoId.parametersOff;

            if (offset == 0) {
                protoId.types = new int[0];
                continue;
            } else {
                seek(offset);
                int size = readInt();       // #of entries in list
                buffer = readByteBuffer(Short.BYTES * size);
                protoId.types = new int[size];

                for (int j = 0; j < size; j++) {
                    protoId.types[j] = buffer.getShort() & 0xffff;
                }
            }
        }
    }

    /**
     * Loads the field ID list.
     */
    void loadFieldIds() throws IOException {
        int count = mHeaderItem.fieldIdsSize;
        mFieldIds = new FieldIdItem[count];

        //System.out.println("reading " + count + " fieldIds");
        seek(mHeaderItem.fieldIdsOff);
        ByteBuffer buffer = readByteBuffer((Integer.BYTES + Short.BYTES * 2) * count);
        for (int i = 0; i < count; i++) {
            mFieldIds[i] = new FieldIdItem();
            mFieldIds[i].classIdx = buffer.getShort() & 0xffff;
            mFieldIds[i].typeIdx = buffer.getShort() & 0xffff;
            mFieldIds[i].nameIdx = buffer.getInt();

            //System.out.println(i + ": " + mFieldIds[i].nameIdx +
            //    " " + mStrings[mFieldIds[i].nameIdx]);
        }
    }

    /**
     * Loads the method ID list.
     */
    void loadMethodIds() throws IOException {
        int count = mHeaderItem.methodIdsSize;
        mMethodIds = new MethodIdItem[count];

        //System.out.println("reading " + count + " methodIds");
        seek(mHeaderItem.methodIdsOff);
        ByteBuffer buffer = readByteBuffer((Integer.BYTES + Short.BYTES * 2) * count);
        for (int i = 0; i < count; i++) {
            mMethodIds[i] = new MethodIdItem();
            mMethodIds[i].classIdx = buffer.getShort() & 0xffff;
            mMethodIds[i].protoIdx = buffer.getShort() & 0xffff;
            mMethodIds[i].nameIdx = buffer.getInt();

            //System.out.println(i + ": " + mMethodIds[i].nameIdx +
            //    " " + mStrings[mMethodIds[i].nameIdx]);
        }
    }

    /**
     * Loads the class defs list.
     */
    void loadClassDefs() throws IOException {
        int count = mHeaderItem.classDefsSize;
        mClassDefs = new ClassDefItem[count];

        //System.out.println("reading " + count + " classDefs");
        seek(mHeaderItem.classDefsOff);
        ByteBuffer buffer = readByteBuffer(Integer.BYTES * 8 * count);
        for (int i = 0; i < count; i++) {
            mClassDefs[i] = new ClassDefItem();
            mClassDefs[i].classIdx = buffer.getInt();

            /* access_flags = */ buffer.getInt();
            /* superclass_idx = */ buffer.getInt();
            /* interfaces_off = */ buffer.getInt();
            /* source_file_idx = */ buffer.getInt();
            /* annotations_off = */ buffer.getInt();
            /* class_data_off = */ buffer.getInt();
            /* static_values_off = */ buffer.getInt();

            //System.out.println(i + ": " + mClassDefs[i].classIdx + " " +
            //    mStrings[mTypeIds[mClassDefs[i].classIdx].descriptorIdx]);
        }
    }

    /**
     * Sets the "internal" flag on type IDs which are defined in the
     * DEX file or within the VM (e.g. primitive classes and arrays).
     */
    void markInternalClasses() {
        for (int i = mClassDefs.length -1; i >= 0; i--) {
            mTypeIds[mClassDefs[i].classIdx].internal = true;
        }

        for (int i = 0; i < mTypeIds.length; i++) {
            String className = mStrings[mTypeIds[i].descriptorIdx];

            if (className.length() == 1) {
                // primitive class
                mTypeIds[i].internal = true;
            } else if (className.charAt(0) == '[') {
                mTypeIds[i].internal = true;
            }

            //System.out.println(i + " " +
            //    (mTypeIds[i].internal ? "INTERNAL" : "external") + " - " +
            //    mStrings[mTypeIds[i].descriptorIdx]);
        }
    }


    /*
     * =======================================================================
     *      Queries
     * =======================================================================
     */

    /**
     * Returns the class name, given an index into the type_ids table.
     */
    private String classNameFromTypeIndex(int idx) {
        return mStrings[mTypeIds[idx].descriptorIdx];
    }

    /**
     * Returns an array of method argument type strings, given an index
     * into the proto_ids table.
     */
    private String[] argArrayFromProtoIndex(int idx) {
        ProtoIdItem protoId = mProtoIds[idx];
        String[] result = new String[protoId.types.length];

        for (int i = 0; i < protoId.types.length; i++) {
            result[i] = mStrings[mTypeIds[protoId.types[i]].descriptorIdx];
        }

        return result;
    }

    /**
     * Returns a string representing the method's return type, given an
     * index into the proto_ids table.
     */
    private String returnTypeFromProtoIndex(int idx) {
        ProtoIdItem protoId = mProtoIds[idx];
        return mStrings[mTypeIds[protoId.returnTypeIdx].descriptorIdx];
    }

    /**
     * Returns an array with all of the class references that don't
     * correspond to classes in the DEX file.  Each class reference has
     * a list of the referenced fields and methods associated with
     * that class.
     */
    public ClassRef[] getExternalReferences() {
        // create a sparse array of ClassRef that parallels mTypeIds
        ClassRef[] sparseRefs = new ClassRef[mTypeIds.length];

        // create entries for all externally-referenced classes
        int count = 0;
        for (int i = 0; i < mTypeIds.length; i++) {
            if (!mTypeIds[i].internal) {
                sparseRefs[i] =
                    new ClassRef(mStrings[mTypeIds[i].descriptorIdx]);
                count++;
            }
        }

        // add fields and methods to the appropriate class entry
        addExternalFieldReferences(sparseRefs);
        addExternalMethodReferences(sparseRefs);

        // crunch out the sparseness
        ClassRef[] classRefs = new ClassRef[count];
        int idx = 0;
        for (int i = 0; i < mTypeIds.length; i++) {
            if (sparseRefs[i] != null)
                classRefs[idx++] = sparseRefs[i];
        }

        assert idx == count;

        return classRefs;
    }

    /**
     * Runs through the list of field references, inserting external
     * references into the appropriate ClassRef.
     */
    private void addExternalFieldReferences(ClassRef[] sparseRefs) {
        for (int i = 0; i < mFieldIds.length; i++) {
            if (!mTypeIds[mFieldIds[i].classIdx].internal) {
                FieldIdItem fieldId = mFieldIds[i];
                FieldRef newFieldRef = new FieldRef(
                        classNameFromTypeIndex(fieldId.classIdx),
                        classNameFromTypeIndex(fieldId.typeIdx),
                        mStrings[fieldId.nameIdx]);
                sparseRefs[mFieldIds[i].classIdx].addField(newFieldRef);
            }
        }
    }

    /**
     * Runs through the list of method references, inserting external
     * references into the appropriate ClassRef.
     */
    private void addExternalMethodReferences(ClassRef[] sparseRefs) {
        for (int i = 0; i < mMethodIds.length; i++) {
            if (!mTypeIds[mMethodIds[i].classIdx].internal) {
                MethodIdItem methodId = mMethodIds[i];
                MethodRef newMethodRef = new MethodRef(
                        classNameFromTypeIndex(methodId.classIdx),
                        argArrayFromProtoIndex(methodId.protoIdx),
                        returnTypeFromProtoIndex(methodId.protoIdx),
                        mStrings[methodId.nameIdx]);
                sparseRefs[mMethodIds[i].classIdx].addMethod(newMethodRef);
            }
        }
    }


    /*
     * =======================================================================
     *      Basic I/O functions
     * =======================================================================
     */

    /**
     * Seeks the DEX file to the specified absolute position.
     */
    void seek(int position) throws IOException {
        mDexFile.seek(position);
    }

    /**
     * Fills the buffer by reading bytes from the DEX file.
     */
    void readBytes(byte[] buffer) throws IOException {
        mDexFile.readFully(buffer);
    }

    /**
     * Reads a single signed byte value.
     */
    byte readByte() throws IOException {
        mDexFile.readFully(tmpBuf, 0, 1);
        return tmpBuf[0];
    }

    /**
     * Reads a signed 32-bit integer, byte-swapping if necessary.
     */
    int readInt() throws IOException {
        mDexFile.readFully(tmpBuf, 0, 4);

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (tmpBuf[3] & 0xff) | ((tmpBuf[2] & 0xff) << 8) |
                   ((tmpBuf[1] & 0xff) << 16) | ((tmpBuf[0] & 0xff) << 24);
        } else {
            return (tmpBuf[0] & 0xff) | ((tmpBuf[1] & 0xff) << 8) |
                   ((tmpBuf[2] & 0xff) << 16) | ((tmpBuf[3] & 0xff) << 24);
        }
    }

    /**
     * Reads a variable-length unsigned LEB128 value.  Does not attempt to
     * verify that the value is valid.
     *
     * @throws EOFException if we run off the end of the file
     */
    int readUnsignedLeb128() throws IOException {
        int result = 0;
        byte val;

        do {
            val = readByte();
            result = (result << 7) | (val & 0x7f);
        } while (val < 0);

        return result;
    }

    /**
     * Reads a UTF-8 string.
     *
     * We don't know how long the UTF-8 string is, so we try to read the worst case amount of bytes.
     */
    String readString() throws IOException {
        int utf16len = readUnsignedLeb128();
        byte inBuf[] = new byte[utf16len * 3];      // worst case

        int bytesRead = mDexFile.read(inBuf);
        if (bytesRead == inBuf.length) {
            for (int i = 0; i < bytesRead; i++) {
                if (inBuf[i] == 0) {
                    bytesRead = i;
                    break;
                }
            }
        }

        return new String(inBuf, 0, bytesRead, "UTF-8");
    }

    /**
     * Reads bytes and transforms them into a ByteBuffer with the desired byte order set, from which
     * primitive values can be read.
     */
    ByteBuffer readByteBuffer(int size) throws IOException {
        byte bytes[] = new byte[size];
        mDexFile.read(bytes);
        return ByteBuffer.wrap(bytes).order(byteOrder);
    }

    /*
     * =======================================================================
     *      Internal "structure" declarations
     * =======================================================================
     */

    /**
     * Holds the contents of a header_item.
     */
    static class HeaderItem {
        public int fileSize;
        public int headerSize;
        public int endianTag;
        public int stringIdsSize, stringIdsOff;
        public int typeIdsSize, typeIdsOff;
        public int protoIdsSize, protoIdsOff;
        public int fieldIdsSize, fieldIdsOff;
        public int methodIdsSize, methodIdsOff;
        public int classDefsSize, classDefsOff;

        /* expected magic values */
        public static final byte[] DEX_FILE_MAGIC_v035 =
            "dex\n035\0".getBytes(StandardCharsets.US_ASCII);

        // Dex version 036 skipped because of an old dalvik bug on some versions
        // of android where dex files with that version number would erroneously
        // be accepted and run. See: art/runtime/dex_file.cc

        // V037 was introduced in API LEVEL 24
        public static final byte[] DEX_FILE_MAGIC_v037 =
            "dex\n037\0".getBytes(StandardCharsets.US_ASCII);

        // V038 was introduced in API LEVEL 26
        public static final byte[] DEX_FILE_MAGIC_v038 =
            "dex\n038\0".getBytes(StandardCharsets.US_ASCII);

        // V039 was introduced in API LEVEL 28
        public static final byte[] DEX_FILE_MAGIC_v039 =
            "dex\n039\0".getBytes(StandardCharsets.US_ASCII);

        public static final int ENDIAN_CONSTANT = 0x12345678;
        public static final int REVERSE_ENDIAN_CONSTANT = 0x78563412;
    }

    /**
     * Holds the contents of a type_id_item.
     *
     * This is chiefly a list of indices into the string table.  We need
     * some additional bits of data, such as whether or not the type ID
     * represents a class defined in this DEX, so we use an object for
     * each instead of a simple integer.  (Could use a parallel array, but
     * since this is a desktop app it's not essential.)
     */
    static class TypeIdItem {
        public int descriptorIdx;       // index into string_ids

        public boolean internal;        // defined within this DEX file?
    }

    /**
     * Holds the contents of a proto_id_item.
     */
    static class ProtoIdItem {
        public int shortyIdx;           // index into string_ids
        public int returnTypeIdx;       // index into type_ids
        public int parametersOff;       // file offset to a type_list

        public int types[];             // contents of type list
    }

    /**
     * Holds the contents of a field_id_item.
     */
    static class FieldIdItem {
        public int classIdx;            // index into type_ids (defining class)
        public int typeIdx;             // index into type_ids (field type)
        public int nameIdx;             // index into string_ids
    }

    /**
     * Holds the contents of a method_id_item.
     */
    static class MethodIdItem {
        public int classIdx;            // index into type_ids
        public int protoIdx;            // index into proto_ids
        public int nameIdx;             // index into string_ids
    }

    /**
     * Holds the contents of a class_def_item.
     *
     * We don't really need a class for this, but there's some stuff in
     * the class_def_item that we might want later.
     */
    static class ClassDefItem {
        public int classIdx;            // index into type_ids
    }
}
