// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package invokecustom;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TestGenerator {

  private final Path classNamePath;

  public static void main(String[] args) throws IOException {
    assert args.length == 1;
    TestGenerator testGenerator = new TestGenerator(Paths.get(args[0],
        TestGenerator.class.getPackage().getName(), InvokeCustom.class.getSimpleName() + ".class"));
    testGenerator.generateTests();
  }

  public TestGenerator(Path classNamePath) {
    this.classNamePath = classNamePath;
  }

  private void generateTests() throws IOException {
    ClassReader cr = new ClassReader(new FileInputStream(classNamePath.toFile()));
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cr.accept(
        new ClassVisitor(Opcodes.ASM5, cw) {
          @Override
          public void visitEnd() {
            generateMethodTest1(cw);
            generateMethodTest2(cw);
            generateMethodTest3(cw);
            generateMethodTest4(cw);
            generateMethodMain(cw);
            super.visitEnd();
          }
        }, 0);
    new FileOutputStream(classNamePath.toFile()).write(cw.toByteArray());
  }

  /* generate main method that only call all test methods. */
  private void generateMethodMain(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Type.getInternalName(InvokeCustom.class), "test1", "()V", false);
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Type.getInternalName(InvokeCustom.class), "test2", "()V", false);
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Type.getInternalName(InvokeCustom.class), "test3", "()V", false);
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Type.getInternalName(InvokeCustom.class), "test4", "()V", false);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   *  Generate test with an invokedynamic, a static bootstrap method without extra args and no arg
   *  to the target method.
   */
  private void generateMethodTest1(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test1", "()V",
            null, null);
    MethodType mt =
        MethodType.methodType(
            CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    Handle bootstrap = new Handle( Opcodes.H_INVOKESTATIC, Type.getInternalName(InvokeCustom.class),
        "bsmLookupStatic", mt.toMethodDescriptorString(), false);
    mv.visitInvokeDynamicInsn("targetMethodTest1", "()V", bootstrap);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   *  Generate test with an invokedynamic, a static bootstrap method without extra args and
   *  args to the target method.
   */
  private void generateMethodTest2(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test2", "()V",
        null, null);
    MethodType mt = MethodType.methodType(
            CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    Handle bootstrap = new Handle( Opcodes.H_INVOKESTATIC, Type.getInternalName(InvokeCustom.class),
        "bsmLookupStatic", mt.toMethodDescriptorString(), false);
    mv.visitLdcInsn(new Boolean(true));
    mv.visitLdcInsn(new Byte((byte) 127));
    mv.visitLdcInsn(new Character('c'));
    mv.visitLdcInsn(new Short((short) 1024));
    mv.visitLdcInsn(new Integer(123456));
    mv.visitLdcInsn(new Float(1.2f));
    mv.visitLdcInsn(new Long(123456789));
    mv.visitLdcInsn(new Double(3.5123456789));
    mv.visitLdcInsn("String");
    mv.visitInvokeDynamicInsn("targetMethodTest2", "(ZBCSIFJDLjava/lang/String;)V", bootstrap);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   *  Generate test with an invokedynamic, a static bootstrap method with extra args and no arg
   *  to the target method.
   */
  private void generateMethodTest3(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test3", "()V",
        null, null);
    MethodType mt = MethodType.methodType(
            CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class,
        long.class, float.class, double.class);
    Handle bootstrap = new Handle( Opcodes.H_INVOKESTATIC, Type.getInternalName(InvokeCustom.class),
        "bsmLookupStaticWithExtraArgs", mt.toMethodDescriptorString(), false);
    mv.visitInvokeDynamicInsn("targetMethodTest3", "()V", bootstrap, new Integer(1),
        new Long(123456789), new Float(123.456), new Double(123456.789123));
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   *  Generate test with an invokedynamic, a static bootstrap method with an extra arg that is a
   *  MethodHandle of kind invokespecial.
   */
  private void generateMethodTest4(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test4", "()V",
        null, null);
    MethodType mt =
        MethodType.methodType(
            CallSite.class,
            MethodHandles.Lookup.class,
            String.class,
            MethodType.class,
            MethodHandle.class);
    Handle bootstrap = new Handle( Opcodes.H_INVOKESTATIC, Type.getInternalName(InvokeCustom.class),
        "bsmCreateCallSite", mt.toMethodDescriptorString(), false);
    mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(InvokeCustom.class));
    mv.visitInsn(Opcodes.DUP);
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL, Type.getInternalName(InvokeCustom.class), "<init>", "()V", false);
    mv.visitInvokeDynamicInsn("targetMethodTest5", "(Linvokecustom/InvokeCustom;)V", bootstrap,
        new Handle( Opcodes.H_INVOKESPECIAL, Type.getInternalName(Super.class),
            "targetMethodTest5", "()V", false));
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }
}
