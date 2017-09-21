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

package constmethodhandle;

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
        TestGenerator.class.getPackage().getName(), ConstTest.class.getSimpleName() + ".class"));
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
            generateMethodMain(cw);
            super.visitEnd();
          }
        }, 0);
    new FileOutputStream(classNamePath.toFile()).write(cw.toByteArray());
  }

  /* generate main method that only call all test methods. */
  private void generateMethodMain(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                      "main", "([Ljava/lang/String;)V", null, null);
    String internalName = Type.getInternalName(ConstTest.class);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, "test1",
                       "()Ljava/lang/invoke/MethodHandle;", false);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, "test2",
                       "()Ljava/lang/invoke/MethodType;", false);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   * Generate a test that returns a constant method handle.
   */
  private void generateMethodTest1(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test1",
                                      "()Ljava/lang/invoke/MethodHandle;", null, null);
    MethodType mt = MethodType.methodType(Class.class);
    Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Object.class),
                                  "getClass", mt.toMethodDescriptorString(), false);
    mv.visitLdcInsn(bootstrap);
    mv.visitInsn(Opcodes.ARETURN);
    mv.visitMaxs(-1, -1);
  }

  /**
   * Generate a test that returns a constant method type.
   */
  private void generateMethodTest2(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "test2",
                                      "()Ljava/lang/invoke/MethodType;", null, null);
    Type mt = Type.getMethodType(Type.getType(boolean.class), Type.getType(char.class),
                                 Type.getType(short.class), Type.getType(int.class),
                                 Type.getType(long.class), Type.getType(float.class),
                                 Type.getType(double.class), Type.getType(Object.class));
    mv.visitLdcInsn(mt);
    mv.visitInsn(Opcodes.ARETURN);
    mv.visitMaxs(-1, -1);
  }
}
