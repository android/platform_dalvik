/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.dx.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class ByteArrayAnnotatedOutputTest {
    @Test
    public void testArrayZeroedOut() {
        int length = 100;
        byte[] data = new byte[length];
        Arrays.fill(data, (byte) 0xFF);

        ByteArrayAnnotatedOutput output = new ByteArrayAnnotatedOutput(data);

        output.writeZeroes(length);

        for (int i = 0; i < length; i++) {
            assertEquals("Position " + i + " has not been zeroed out", 0, data[i]);
        }
    }

    @Test
    public void testArrayAligned() {
        int length = 16;
        byte[] data = new byte[length];
        Arrays.fill(data, (byte) 0xFF);

        ByteArrayAnnotatedOutput output = new ByteArrayAnnotatedOutput(data);

        // write at least one byte, so alignment is not correct
        output.writeByte(0);
        output.alignTo(length);

        for (int i = 0; i < length; i++) {
            assertEquals("Position " + i + " has not been zeroed out", 0, data[i]);
        }
    }
}
