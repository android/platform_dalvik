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
    /*
     * Defines the number of PlatformAddress objects to be cached. Must be a
     * power of two. Caching PlatformAddress objects minimizes the creation
     * of garbage and reduces the number of GC-hiccups in OpenGL animations.
     */
    private final static int CACHE_SIZE = 1<<8;

    /*
     * A mask with all bits set, matching the size of the cache.
     */
    private final static int CACHE_MASK = CACHE_SIZE - 1;

    /*
     * Defines the maximum number of probes taken per hash, used for looking
     * up an empty cache slot or a previously stored PlatformAddress.
     */
    private final static int MAX_PROBES = 5;

    /*
     * A cycling index (0 to MAX_PROBES-1) used to replace elements in the cache.
     */
    private static int replacementIndex = 0;

    /*
     * Array of PlatformAddress references kept from garbage collection.
     */
    private static PlatformAddress[] cache = new PlatformAddress[CACHE_SIZE];
    // END android-added


    // BEGIN android-changed
    public synchronized static PlatformAddress on(int value, long size) {
        if (value == 0) {
            return PlatformAddress.NULL;
        }
        int idx = value >> 5;
        for (int probe = 0; probe < MAX_PROBES; probe++)
        {
            PlatformAddress cachedObj = cache[(idx + probe) & CACHE_MASK];
            if (cachedObj == null) {
                return cache[(idx + probe) & CACHE_MASK] =
                    new PlatformAddress(value, size);
            }
            if (cachedObj.osaddr == value && cachedObj.size == size) {
                return cachedObj;
            }
        }
        replacementIndex = (replacementIndex + 1) % MAX_PROBES;
        return cache[(idx + replacementIndex) & CACHE_MASK] =
            new PlatformAddress(value, size);
    }
    // END android-changed

    public static PlatformAddress on(int value) {
        return PlatformAddressFactory.on(value, PlatformAddress.UNKNOWN);
    }

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
