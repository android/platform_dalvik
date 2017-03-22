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

package com.android.dx.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.android.dex.Dex;
import com.android.dx.command.Main;
import com.android.dx.command.dexer.DxContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DexMergerTest {
    static class NoFieldsClassA {
    }
    static class NoFieldsClassB {
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_merge_dexesWithEmptyFieldsSection() throws IOException {
        List<Dex> outputDexes = new ArrayList<>();
        outputDexes.add(getDexForClass(NoFieldsClassA.class));
        outputDexes.add(getDexForClass(NoFieldsClassB.class));

        Dex merged =
                new DexMerger(
                        outputDexes.toArray(new Dex[outputDexes.size()]),
                        CollisionPolicy.FAIL,
                        new DxContext())
                        .merge();
        assertNotNull(merged);
        assertNotNull(merged.getTableOfContents());
        assertEquals(0, merged.getTableOfContents().fieldIds.off);
    }

    private Dex getDexForClass(Class<?> clazz) throws IOException {
        String path = clazz.getName().replace('.', '/') + ".class";
        Path classesJar = temporaryFolder.newFile(clazz.getName() + ".jar").toPath();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path);
             ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(classesJar))) {

            ZipEntry entry = new ZipEntry(path);
            zip.putNextEntry(entry);
            zip.write(readEntireStream(in));
            zip.closeEntry();
        }

        Path output = temporaryFolder.newFolder().toPath();
        Main.main(new String[]{"--dex", "--output=" + output.toString(), classesJar.toString()});

        return new Dex(Files.readAllBytes(output.resolve("classes.dex")));
    }

    private static byte[] readEntireStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        int count;
        while ((count = inputStream.read(buffer)) != -1) {
            bytesOut.write(buffer, 0, count);
        }

        return bytesOut.toByteArray();
    }
}
