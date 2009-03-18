/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

package tests.support.resource;

import tests.support.Support_Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class Support_Resources {

    public static final String RESOURCE_PACKAGE = "/tests/resources/";

    public static final String RESOURCE_PACKAGE_NAME = "tests.resources";

    public static InputStream getStream(String name) {
        return Support_Resources.class.getResourceAsStream(RESOURCE_PACKAGE
                + name);
    }

    public static String getURL(String name) {
        String folder = null;
        String fileName = name;
        File resources = createTempFolder();
        int index = name.lastIndexOf("/");
        if (index != -1) {
            folder = name.substring(0, index);
            name = name.substring(index + 1);
        }
        copyFile(resources, folder, name);
        URL url = null;
        String resPath = resources.toString();
        if (resPath.charAt(0) == '/' || resPath.charAt(0) == '\\') {
            resPath = resPath.substring(1);
        }
        try {
            url = new URL("file:/" + resPath + "/" + fileName);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return url.toString();
    }

    public static File createTempFolder() {

        File folder = null;
        try {
            folder = File.createTempFile("hyts_resources", "", null);
            folder.delete();
            folder.mkdirs();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        folder.deleteOnExit();
        return folder;
    }

    public static File copyFile(File root, String folder, String file) {
        File f;
        if (folder != null) {
            f = new File(root.toString() + "/" + folder);
            if (!f.exists()) {
                f.mkdirs();
                f.deleteOnExit();
            }
        } else {
            f = root;
        }

        File dest = new File(f.toString() + "/" + file);

        InputStream in = Support_Resources.getStream(folder == null ? file
                : folder + "/" + file);
        try {
            copyLocalFileto(dest, in);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dest;
    }

    public static File createTempFile(String suffix) throws IOException {
        return File.createTempFile("hyts_", suffix, null);
    }

    public static void copyLocalFileto(File dest, InputStream in)
            throws FileNotFoundException, IOException {
        if (!dest.exists()) {
            FileOutputStream out = new FileOutputStream(dest);
            int result;
            byte[] buf = new byte[4096];
            while ((result = in.read(buf)) != -1) {
                out.write(buf, 0, result);
            }
            in.close();
            out.close();
            dest.deleteOnExit();
        }
    }

    public static File getExternalLocalFile(String url) throws IOException,
            MalformedURLException {
        File resources = createTempFolder();
        InputStream in = new URL(url).openStream();
        File temp = new File(resources.toString() + "/local.tmp");
        copyLocalFileto(temp, in);
        return temp;
    }

    public static String getResourceURL(String resource) {
        return "http://" + Support_Configuration.TestResources + resource;
    }

    /**
     * Util method to load resource files
     * 
     * @param name - name of resource file
     * @return - resource input stream
     */
    public static InputStream getResourceStream(String name) {
//ATTENTION:
//    Against class.getResourceStream(name) the name can start with a "/".
//    Against classLoader.getResourceStream NOT!
        
        InputStream is;
//        is = Support_Resources.class.getClassLoader().getResourceAsStream(name); This would work without leading "/"
        is = Support_Resources.class.getResourceAsStream(name);
//        is = ClassLoader.getSystemClassLoader().getResourceAsStream(name); This would work without leading "/"

        if (is == null) {
            name = "/tests/resources/" + name;
            is = Support_Resources.class.getResourceAsStream(name);
            if (is == null) {
                throw new RuntimeException("Failed to load resource: " + name);
            }
        }
        
        return is;
    }

    /**
     * Util method to write resource files directly to an OutputStream.
     * 
     * @param name - name of resource file.
     * @param out - OutputStream to write to.
     * @return - number of bytes written to out.
     */
    public static int writeResourceToStream(String name, OutputStream out) {
        InputStream input = getResourceStream(name);
        byte[] buffer = new byte[512];
        int total = 0;
        int count;
        try {
            count = input.read(buffer);
            while (count != -1) {
                out.write(buffer, 0, count);
                total = total + count;
                count = input.read(buffer);
            }
            return total;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to passed stream.", e);
        }
    }

    /**
     * Util method to get absolute path to resource file
     * 
     * @param name - name of resource file
     * @return - path to resource
     */
    public static String getAbsoluteResourcePath(String name) {

        URL url = ClassLoader.getSystemClassLoader().getResource(name);
        if (url == null) {
            throw new RuntimeException("Failed to load resource: " + name);
        }

        try {
            return new File(url.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load resource: " + name);
        }
    }
}
