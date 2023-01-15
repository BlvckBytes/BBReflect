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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class ConstantPoolUtfPatcher {

  private final Function<String, String> utfPatcher;

  /**
   * Create a new patcher to patch all utf-8 entries of the constant
   * pool of a class represented as byte-code. See
   * <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">specification</a>
   * @param utfPatcher External patcher function, invoked on every utf-8 element
   */
  public ConstantPoolUtfPatcher(Function<String, String> utfPatcher) {
    this.utfPatcher = utfPatcher;
  }

  /**
   * Patch the byte-code of a given class
   * @param inputStream Input stream of data
   * @param outputStream Output stream of data
   */
  public void patch(DataInputStream inputStream, OutputStream outputStream) throws IOException {
    DataOutputStream output = new DataOutputStream(outputStream);

    // magic
    output.write(inputStream.read());
    output.write(inputStream.read());
    output.write(inputStream.read());
    output.write(inputStream.read());

    // major, minor
    output.writeShort(inputStream.readShort());
    output.writeShort(inputStream.readShort());

    int constantPoolCount = inputStream.readShort();
    output.writeShort(constantPoolCount);

    // Minus one, because... because!
    for (int i = 0; i < constantPoolCount - 1; i++)
      patchCpInfo(inputStream, output);

    // Write remaining bytes
    int read;
    while ((read = inputStream.read()) != -1)
      output.write((byte) read);
  }

  /**
   * Sub-routine used to to patch a cp-info struct (constant pool entry)
   */
  private void patchCpInfo(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
    byte tag = inputStream.readByte();
    outputStream.write(tag);

    switch (tag) {
      // CONSTANT_Class
      case 7:
        // name_index
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_Fieldref
      case 9:
      // CONSTANT_Methodref
      case 10:
      // CONSTANT_InterfaceMethodref
      case 11:
        // class_index
        outputStream.writeShort(inputStream.readShort());
        // name_and_type_index
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_String
      case 8:
        // string_index
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_Integer
      case 3:
      // CONSTANT_Float
      case 4:
        // bytes
        outputStream.writeInt(inputStream.readInt());
        break;

      // CONSTANT_Long
      case 5:
      // CONSTANT_Double
      case 6:
        // high_bytes
        outputStream.writeInt(inputStream.readInt());
        // low_bytes
        outputStream.writeInt(inputStream.readInt());
        break;

      // CONSTANT_NameAndType
      case 12:
        // name_index
        outputStream.writeShort(inputStream.readShort());
        // descriptor_ref
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_Utf8
      case 1:
        // length
        short length = inputStream.readShort();

        byte[] data = new byte[length];

        if (inputStream.read(data) != length)
          throw new IllegalStateException("Reading UTF8 data failed");

        byte[] patchResult = utfPatcher.apply(new String(data)).getBytes(StandardCharsets.UTF_8);

        // Write new length and contents
        outputStream.writeShort(patchResult.length);
        outputStream.write(patchResult);
        break;

      // CONSTANT_MethodHandle
      case 15:
        // reference_kind
        outputStream.write(inputStream.read());
        // reference_index
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_MethodType
      case 16:
        // descriptor_index
        outputStream.writeShort(inputStream.readShort());
        break;

      // CONSTANT_InvokeDynamic
      case 18:
        // bootstrap_method_attr_index
        outputStream.writeShort(inputStream.readShort());
        // name_and_type_index
        outputStream.writeShort(inputStream.readShort());
        break;

      default:
        throw new IllegalStateException("Unknown tag encountered: " + tag);
    }
  }
}
