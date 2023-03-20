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
import me.blvckbytes.utilitytypes.Tuple;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class PacketEncoderClassPatcher {

  private final ClassHandle C_PACKET_ENCODER, C_BYTE_BUF, C_CHANNEL_HANDLER_CONTEXT, C_PACKET_DATA_SERIALIZER;

  private final List<Class<?>[]> targetMethodParameters;
  private final ConstantPoolUtfPatcher utfPatcher;

  public PacketEncoderClassPatcher(IReflectionHelper reflectionHelper) throws Exception {
    C_PACKET_ENCODER = reflectionHelper.getClass(RClass.PACKET_ENCODER);
    C_PACKET_DATA_SERIALIZER = reflectionHelper.getClass(RClass.PACKET_DATA_SERIALIZER);
    C_BYTE_BUF = reflectionHelper.getClass(RClass.BYTE_BUF);
    C_CHANNEL_HANDLER_CONTEXT = reflectionHelper.getClass(RClass.CHANNEL_HANDLER_CONTEXT);

    ClassHandle C_NBT_TAG_COMPOUND = reflectionHelper.getClass(RClass.NBT_TAG_COMPOUND);
    ClassHandle C_BASE_COMPONENT = reflectionHelper.getClass(RClass.I_CHAT_BASE_COMPONENT);

    Map<String, String> pdsConstantPoolPatches = new HashMap<>();

      /*
        io.netty.buffer.ByteBuf;
        io.netty.channel.ChannelHandlerContext;
        io.netty.util.AttributeKey;
        net.minecraft.nbt.NBTTagCompound;
        net.minecraft.network.PacketDataSerializer;
        org.jetbrains.annotations.Nullable;
        java.util.function.Supplier;
       */
    pdsConstantPoolPatches.put("net.minecraft.nbt.NBTTagCompound".replace('.', '/'), C_NBT_TAG_COMPOUND.getHandle().getName().replace('.', '/'));
    pdsConstantPoolPatches.put("net.minecraft.network.PacketDataSerializer".replace('.', '/'), C_PACKET_DATA_SERIALIZER.getHandle().getName().replace('.', '/'));

    this.utfPatcher = new ConstantPoolUtfPatcher(value -> pdsConstantPoolPatches.getOrDefault(value, value));

    this.targetMethodParameters = new ArrayList<>();
    this.targetMethodParameters.add(new Class<?>[] { C_NBT_TAG_COMPOUND.getHandle() });
    this.targetMethodParameters.add(new Class<?>[] { C_BASE_COMPONENT.getHandle() });
  }

  private String reducedDescriptorFromMethod(Method method) {
    StringBuilder builder = new StringBuilder();

    builder.append('(');
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      builder.append(parameterTypes[i].getSimpleName());

      if (i != parameterTypes.length - 1)
        builder.append(';');
    }
    builder.append(')');
    builder.append(method.getReturnType().getSimpleName());

    return builder.toString();
  }

  private Tuple<String, String> reduceType(String input) {
    char type = input.charAt(0);

    /*
      Base Type Character | Type    | Interpretation
      B                   byte      signed byte
      C                   char      Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16
      D                   double    double-precision floating-point value
      F                   float     single-precision floating-point value
      I                   int       integer
      J                   long      long integer
      LClassName;         reference an instance of class ClassName
      S                   short     signed short
      Z                   boolean   true or false
      [                   reference one array dimension
     */

    switch (type) {
      case 'B':
        return new Tuple<>("byte", input.substring(1));
      case 'C':
        return new Tuple<>("char", input.substring(1));
      case 'D':
        return new Tuple<>("double", input.substring(1));
      case 'F':
        return new Tuple<>("float", input.substring(1));
      case 'I':
        return new Tuple<>("integer", input.substring(1));
      case 'J':
        return new Tuple<>("long", input.substring(1));
      case 'S':
        return new Tuple<>("short", input.substring(1));
      case 'Z':
        return new Tuple<>("boolean", input.substring(1));
      case 'L':
        int semicolonIndex = input.indexOf(';');
        String fqn = input.substring(0, semicolonIndex);
        return new Tuple<>(fqn.substring(fqn.lastIndexOf('/') + 1), input.substring(semicolonIndex + 1));
      default:
        throw new IllegalStateException("Encountered unknown or unimplemented type " + type);
    }
  }

  private String reducedDescriptorFromDescriptor(String string) {
    int closingBracketIndex = string.indexOf(')');
    String parameters = string.substring(1, closingBracketIndex);
    String returnType = string.substring(closingBracketIndex + 1);

    StringBuilder builder = new StringBuilder("(");
    int initialBuilderLength = builder.length();

    while (parameters.length() > 0) {
      Tuple<String, String> reduceResult = reduceType(parameters);
      parameters = reduceResult.b;

      if (builder.length() != initialBuilderLength)
        builder.append(';');

      builder.append(reduceResult.a);
    }

    builder.append(")");
    builder.append(reduceType(returnType).a);

    return builder.toString();
  }

  private boolean isTargetMethod(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();

    for (Class<?>[] targetParameters : targetMethodParameters) {
      if (targetParameters.length != parameterTypes.length)
        continue;

      boolean allMatched = true;
      for (int i = 0; i < parameterTypes.length; i++) {
        if (parameterTypes[i].equals(targetParameters[i]))
          continue;

        allMatched = false;
        break;
      }

      if (allMatched)
        return true;
    }

    return false;
  }

  private boolean isTargetMethodName(String name) {
    char firstChar = name.charAt(0);
    return (
      (firstChar >= 'a' && firstChar <= 'z') ||
      (firstChar >= 'A' && firstChar <= 'Z')
    );
  }

  private Map<String, String> buildNameByReducedDescriptorTable(Class<?> c) {
    Method[] originalMethods = c.getMethods();
    Map<String, String> nameByReducedDescriptor = new HashMap<>();

    for (Method method : originalMethods) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (!isTargetMethod(method))
        continue;

      String name = method.getName();

      String existingKey;
      if ((existingKey = nameByReducedDescriptor.put(reducedDescriptorFromMethod(method), name)) != null)
        throw new IllegalStateException("Collision on method " + name + ": " + existingKey + "");
    }

    return nameByReducedDescriptor;
  }

  private void loadPatchedPds(String name, BiFunction<String, byte[], Class<?>> defineFunction) throws Exception {
    name = name + ".class";

    try (
      InputStream is = getClass().getClassLoader().getResourceAsStream(name)
    ) {
      if (is == null)
        throw new IllegalStateException("Could not get resource " + name);

      ClassReader classReader = new ClassReader(is);
      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

      Map<String, String> nameByReducedDescriptor = buildNameByReducedDescriptorTable(C_PACKET_DATA_SERIALIZER.getHandle());

      for (MethodNode methodNode : classNode.methods) {
        if ((methodNode.access & Opcodes.ACC_PUBLIC) == 0)
          continue;

        if (!isTargetMethodName(methodNode.name))
          continue;

        String newName = nameByReducedDescriptor.remove(reducedDescriptorFromDescriptor(methodNode.desc));

        if (newName == null)
          continue;

        // FIXME: It's not only method names that need patching, but also super invocations
        methodNode.name = newName;
      }

      if (nameByReducedDescriptor.size() > 0)
        throw new IllegalStateException("Could not match all methods! Remaining: " + nameByReducedDescriptor);

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      classNode.accept(cw);

      byte[] bytes = patchPdsConstantPool(cw.toByteArray());
      defineFunction.apply(classNode.name.replace('/', '.'), bytes);
    }
  }

  private byte[] patchPdsConstantPool(byte[] bytes) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
    DataOutputStream dos = new DataOutputStream(bos);

    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    DataInputStream dis = new DataInputStream(bis);

    this.utfPatcher.patch(dis, dos);

    return bos.toByteArray();
  }

  public Class<?> patchAndLoad(BiFunction<String, byte[], Class<?>> defineFunction) throws Exception {
    String name = C_PACKET_ENCODER.getHandle().getName().replace('.', '/') + ".class";
    String newPdsName = "me/blvckbytes/bbreflect/patching/CustomDataSerializer";

    // Let's just hope that because this class is defined through the custom classloader, that
    // it's also being used by the patched PacketEncoder class.
    loadPatchedPds(newPdsName, defineFunction);

    try (
      InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)
    ) {
      if (is == null)
        throw new IllegalStateException("Could not get resource " + name);

      ClassReader classReader = new ClassReader(is);
      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

      String oldName = getInternalName(C_PACKET_DATA_SERIALIZER.getHandle());

      String contextName = getInternalName(C_CHANNEL_HANDLER_CONTEXT.getHandle());
      String bufferName = getInternalName(C_BYTE_BUF.getHandle());

      for (MethodNode method : classNode.methods) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
          int opcode = insn.getOpcode();

          if (opcode == Opcodes.NEW) {
            TypeInsnNode typeInsn = (TypeInsnNode) insn;

            if (!oldName.equals(typeInsn.desc))
              continue;

            typeInsn.desc = newPdsName;
            continue;
          }

          if (opcode == Opcodes.INVOKESPECIAL) {
            MethodInsnNode methodInsn = (MethodInsnNode) insn;

            if (!oldName.equals(methodInsn.owner))
              continue;

            methodInsn.owner = newPdsName;
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
