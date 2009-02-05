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

    public static PlatformAddress on(int value) {
        return PlatformAddressFactory.on(value, PlatformAddress.UNKNOWN);
    }

    public static PlatformAddress on(int value, long size) {
        PlatformAddress addr = (value == 0) ? PlatformAddress.NULL : newPlatformAddress(value, size);
        return addr;
    }

    /*
     * This will create a new PlatformAddress object
     * -or- reuse a previous PlatformAddress object with identical value+size
     * This way minimizing garbage creation on the framework level and
     * greatly reducing the number of GC-hiccups in OpenGL animations.
     * myObjectReused/myObjectCreated may be used to verify effectiveness
     */
    private final static int maxCacheElements = 160;
    // private static int myObjectReused = 0, myObjectCreated = 0;
    private static PlatformAddress[] platformAddressRingCache = null;
    private static int oldestElement = 0;
    private static int nextElement = 0;
    private static int elementCount = 0;

    private static PlatformAddress newPlatformAddress(int value, long size)
    {
      if(platformAddressRingCache==null)
        platformAddressRingCache = new PlatformAddress[maxCacheElements];

      // search platformAddressRingCache for PlatformAddress entry with value/size
      for(int i=0; i<elementCount; i++) {
        int idx = oldestElement+i;
        if(idx>=maxCacheElements)
          idx-=maxCacheElements;
        if(platformAddressRingCache[idx].osaddr==value && platformAddressRingCache[idx].size==size) {
          //if(++myObjectReused%500==0)
          //  System.out.println("newPlatformAddress value="+value+" size="+size+" reuse "+myObjectCreated+"/"+myObjectReused);
          return platformAddressRingCache[idx];
        }
      }

      PlatformAddress newObj = new PlatformAddress(value, size);
      if(elementCount<maxCacheElements)
        elementCount++;
      else
      if(++oldestElement==maxCacheElements)
        oldestElement=0;

      platformAddressRingCache[nextElement]=newObj;
      if(++nextElement==maxCacheElements)
        nextElement=0;

      // if(++myObjectCreated%500==0)
      //  System.out.println("newPlatformAddress value="+value+" size="+size+" create "+myObjectCreated+"/"+myObjectReused);
      return newObj;
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
