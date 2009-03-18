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

package org.apache.harmony.luni.tests.java.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class TestLibrary {
    private native String printName();
    
    boolean checkString() {
        if(printName().equals("TestLibrary"))
            return true;
        return false;
    }
    
    TestLibrary() {
        InputStream in = TestLibrary.class.getResourceAsStream("/libTestLibrary.so");
        try {
            File tmp = File.createTempFile("libTestLibrary", "so");
            tmp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tmp);
            while (in.available() > 0) {
                out.write(in.read()); // slow
            }
            in.close();
            out.close();
            Runtime.getRuntime().load(tmp.getAbsolutePath());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }        
}
