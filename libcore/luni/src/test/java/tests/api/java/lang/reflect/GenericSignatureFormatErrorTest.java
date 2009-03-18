package tests.api.java.lang.reflect;

import dalvik.annotation.AndroidOnly;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.system.DexFile;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.TypeVariable;

@TestTargetClass(GenericSignatureFormatError.class)
public class GenericSignatureFormatErrorTest extends TestCase{

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "GenericSignatureFormatError",
        args = {}
    )
    public void test_Constructor() {
        assertNotNull(new GenericSignatureFormatError());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "GenericSignatureFormatError",
        args = {}
    )
    public void test_readResource() throws Exception {
        File tf = File.createTempFile("classes", ".dex");
        // System.out.println("GenericSignatureFormatErrorTest:"
        //         +tf.getAbsolutePath()+", canRead: "+tf.canRead()
        //         +", canWrite: "+tf.canWrite()); 
        InputStream is = this.getClass().getResourceAsStream("dex1.bytes");
        assertNotNull(is);
    }

    
    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        notes = "",
        method = "GenericSignatureFormatError",
        args = {}
    )    
    @AndroidOnly("Uses Android specific class dalvik.system.DexFile " +
            "for loading classes.")
    public void test_signatureFormatError() throws Exception {
        /*
         * dex1.bytes is a jar file with a classes.dex in it.
         * the classes.dex was javac'ed, dx'ed and patched 
         * with the following java file:
         * 
         * package demo;
         *  public class HelloWorld<U> {
         *      public HelloWorld(U t) {}
         *  }
         * 
         * patch:
         * the string constant (class generics signature string)
         *  "<U:" was changed to "<<:"
         * 
         */
        
        File tf = File.createTempFile("classes", ".dex");
        // System.out.println("GenericSignatureFormatErrorTest:" + 
        //         tf.getAbsolutePath() + ", canRead: " + tf.canRead() + 
        //         ", canWrite: "+tf.canWrite());
        InputStream is = this.getClass().getResourceAsStream("dex1.bytes");
        assertNotNull(is);
        OutputStream fos = new FileOutputStream(tf);
        copy(is, fos);
        fos.close();
        
        
        // class signature string "<U:" was changed to "<<:"
        //System.out.println("file length:"+tf.length());
        try {
            DexFile df = new DexFile(tf);
            Class clazz = df.loadClass("demo/HelloWorld", this.getClass().getClassLoader());
            TypeVariable[] tvs = clazz.getTypeParameters();
            fail("expecting a GenericSignatureFormatError");
            // for (TypeVariable tv : tvs) {
            //     System.out.println("tv:"+tv.toString());
            // }
        } catch (GenericSignatureFormatError gsfe) {
            // expected
        }
    }
    
    private void copy(InputStream is, OutputStream os) {
        try {
            int b; 
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            is.close();
        } catch (IOException ex) {
            throw new RuntimeException("io error", ex);
        }
    }
}
