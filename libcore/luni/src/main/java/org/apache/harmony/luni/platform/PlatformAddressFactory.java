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
    private final static int MAX_CACHE_ELEMENTS = 160;
    private static PlatformAddress[] platformAddressRingCache = null;
    private static int oldestElement = 0;
    private static int nextElement = 0;
    private static int elementCount = 0;
    // END android-added

	  public static PlatformAddress on(int value) {
        return PlatformAddressFactory.on(value, PlatformAddress.UNKNOWN);
    }

    // BEGIN android-changed
    public static PlatformAddress on(int value, long size) {
        PlatformAddress addr = (value == 0) ? PlatformAddress.NULL : newPlatformAddress(value, size);
        return addr;
    }

    /*
     * This will create a new PlatformAddress object -or- if available
     * reuse a previous PlatformAddress object with identical value+size
     * This will minimize garbage creation on the framework level and
     * greatly reduce the number of GC-hiccups in OpenGL animations.
     */
    private synchronized static PlatformAddress newPlatformAddress(int value, long size) {
        if (platformAddressRingCache == null)
            platformAddressRingCache = new PlatformAddress[MAX_CACHE_ELEMENTS];

        // search platformAddressRingCache for PlatformAddress entry with same value+size
        for (int i=0; i<elementCount; i++) {
            int idx = oldestElement+i;
            if (idx >= MAX_CACHE_ELEMENTS) {
                idx -= MAX_CACHE_ELEMENTS;
            }
            if (platformAddressRingCache[idx].osaddr == value 
                  && platformAddressRingCache[idx].size == size) {
                return platformAddressRingCache[idx];
            }
        }

        PlatformAddress newObj = new PlatformAddress(value, size);
        if (elementCount < MAX_CACHE_ELEMENTS) {
            elementCount++;
        } else if (++oldestElement == MAX_CACHE_ELEMENTS) {
            oldestElement = 0;
        }
        platformAddressRingCache[nextElement] = newObj;
        if (++nextElement == MAX_CACHE_ELEMENTS) {
            nextElement = 0;
        }
        return newObj;
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
