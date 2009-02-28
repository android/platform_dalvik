/* Licensed to the Apache Software Foundation (ASF) under one or more
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

// BEGIN android-note
// address length was changed from long to int for performance reasons.
// END android-note

package org.apache.harmony.luni.platform;

import java.io.IOException;

public class PlatformAddressFactory {

    // BEGIN android-added
    final static int CACHESIZE = 0x100;   // values: 0x100, 0x200, ...
    final static int MAXPROBES = 5;       // values: 0 to 7
    private static PlatformAddress[] cache = new PlatformAddress[CACHESIZE];
    private static int replIdx = 0;
    // END android-added

    public static PlatformAddress on(int value) {
        return PlatformAddressFactory.on(value, PlatformAddress.UNKNOWN);
    }

    // BEGIN android-changed
    /*
     * This will create a new PlatformAddress object -or- if available
     * reuse a previous PlatformAddress object with identical value+size
     * This will minimize garbage creation on the framework level and
     * greatly reduce the number of GC-hiccups in OpenGL animations.
     */
    public synchronized static PlatformAddress on(int value, long size) {
        if(value == 0) {
            return PlatformAddress.NULL;
        }
        int idx = value >> 5;
        int probe = 0;
        do
        {
            PlatformAddress cachedObj = cache[(idx + probe) & (CACHESIZE - 1)];
            if (cachedObj == null) {
                return cache[(idx + probe) & (CACHESIZE - 1)] = 
                    new PlatformAddress(value, size);
            }
            if (cachedObj.osaddr == value && cachedObj.size == size) {
                return cachedObj;
            }
        }
        while (probe++ < MAXPROBES);
        replIdx %= MAXPROBES + 1;
        return cache[(idx + replIdx++) & (CACHESIZE - 1)] = 
            new PlatformAddress(value, size);
    }
    // END android-changed

    public static MappedPlatformAddress mapOn(int value, long size) {
        MappedPlatformAddress addr = new MappedPlatformAddress(value, size);
        return addr;
    }
    
    public static PlatformAddress allocMap(int fd, long start, long size, int mode) throws IOException{
        int osAddress = PlatformAddress.osMemory.mmap(fd, start, size, mode);
        PlatformAddress newMemory = mapOn(osAddress, size);
        PlatformAddress.memorySpy.alloc(newMemory);
        return newMemory;
    }

    /**
     * Allocates a contiguous block of OS heap memory.
     * 
     * @param size The number of bytes to allocate from the system heap.
     * @return PlatformAddress representing the memory block.
     */
    public static PlatformAddress alloc(int size) {
        int osAddress = PlatformAddress.osMemory.malloc(size);
        PlatformAddress newMemory = on(osAddress, size);
        PlatformAddress.memorySpy.alloc(newMemory);
        return newMemory;
    }

    /**
     * Allocates a contiguous block of OS heap memory and initializes it to
     * a given value.
     * 
     * @param size The number of bytes to allocate from the system heap.
     * @param init The value to initialize the memory.
     * @return PlatformAddress representing the memory block.
     */
    public static PlatformAddress alloc(int size, byte init) {
        int osAddress = PlatformAddress.osMemory.malloc(size);
        PlatformAddress.osMemory.memset(osAddress, init, size);
        PlatformAddress newMemory = on(osAddress, size);
        PlatformAddress.memorySpy.alloc(newMemory);
        return newMemory;
    }
}
