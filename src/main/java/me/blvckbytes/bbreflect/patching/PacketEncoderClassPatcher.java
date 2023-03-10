/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbreflect.patching;

import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.RClass;
import me.blvckbytes.bbreflect.handle.ClassHandle;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.function.BiFunction;

public class PacketEncoderClassPatcher {

  private final Class<?> newPacketDataSerializer;

  private final ClassHandle C_PACKET_ENCODER, C_BYTE_BUF, C_CHANNEL_HANDLER_CONTEXT, C_PACKET_DATA_SERIALIZER;

  public PacketEncoderClassPatcher(IReflectionHelper reflectionHelper, Class<?> newPacketDataSerializer) throws Exception {
    this.newPacketDataSerializer = newPacketDataSerializer;

    C_PACKET_ENCODER = reflectionHelper.getClass(RClass.PACKET_ENCODER);
    C_PACKET_DATA_SERIALIZER = reflectionHelper.getClass(RClass.PACKET_DATA_SERIALIZER);
    C_BYTE_BUF = reflectionHelper.getClass(RClass.BYTE_BUF);
    C_CHANNEL_HANDLER_CONTEXT = reflectionHelper.getClass(RClass.CHANNEL_HANDLER_CONTEXT);
  }

  // TODO: Either generate the CustomDataSerializer from scratch or patch the existing class
//  public byte[] generate(String name) throws Exception {
//    ClassNode classNode = new ClassNode();
//
//    String contextName = getInternalName(C_CHANNEL_HANDLER_CONTEXT.getHandle());
//    String bufferName = getInternalName(C_BYTE_BUF.getHandle());
//    String pdsName = getInternalName(C_PACKET_DATA_SERIALIZER.getHandle());
//
//    classNode.name = name;
//    classNode.superName = pdsName;
//
//    MethodNode constructor = new MethodNode();
//
//    constructor.name = "<init>";
//    constructor.access = Opcodes.ACC_PUBLIC;
//    constructor.desc = "(L" + bufferName + ";L" + contextName + ";)V";
//    constructor.instructions = new InsnList();
//
//    constructor.instructions.add();
//
//    classNode.methods.add(constructor);
//
//    ClassWriter cw = new ClassWriter(0);
//    classNode.accept(cw);
//
//    return cw.toByteArray();
//  }

  public Class<?> patchAndLoad(BiFunction<String, byte[], Class<?>> defineFunction) throws Exception {
    String name = C_PACKET_ENCODER.getHandle().getName().replace('.', '/') + ".class";

    try (
      InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)
    ) {
      if (is == null)
        throw new IllegalStateException("Could not get resource " + name);

      ClassReader classReader = new ClassReader(is);
      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

      String oldName = getInternalName(C_PACKET_DATA_SERIALIZER.getHandle());
      String newName = getInternalName(newPacketDataSerializer);

      String contextName = getInternalName(C_CHANNEL_HANDLER_CONTEXT.getHandle());
      String bufferName = getInternalName(C_BYTE_BUF.getHandle());

      for (MethodNode method : classNode.methods) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
          int opcode = insn.getOpcode();

          if (opcode == Opcodes.NEW) {
            TypeInsnNode typeInsn = (TypeInsnNode) insn;

            if (!oldName.equals(typeInsn.desc))
              continue;

            typeInsn.desc = newName;
            continue;
          }

          if (opcode == Opcodes.INVOKESPECIAL) {
            MethodInsnNode methodInsn = (MethodInsnNode) insn;

            if (!oldName.equals(methodInsn.owner))
              continue;

            methodInsn.owner = newName;
            methodInsn.desc = "(L" + bufferName + ";L" + contextName + ";)V";

            // Parameter 1 is the context
            method.instructions.insertBefore(methodInsn, new VarInsnNode(Opcodes.ALOAD, 1));
          }
        }
      }

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      classNode.accept(cw);

      return defineFunction.apply(classNode.name.replace('/', '.'), cw.toByteArray());
    }
  }

  private String getInternalName(Class<?> c) {
    return c.getName().replace('.', '/');
  }
}
