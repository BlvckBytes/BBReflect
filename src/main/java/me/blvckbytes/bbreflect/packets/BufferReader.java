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

package me.blvckbytes.bbreflect.packets;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.nio.charset.StandardCharsets;

public class BufferReader {

  public static int readVarInt(ByteBuf buffer) {
    int i = 0;
    int j = 0;

    byte b0;
    do {
      b0 = buffer.readByte();
      i |= (b0 & 127) << j++ * 7;
      if (j > 5) {
        throw new RuntimeException("VarInt too big");
      }
    } while((b0 & 128) == 128);

    return i;
  }

  public static String readUTF8(ByteBuf buffer, int maxLength) {
    int stringLength = readVarInt(buffer);

    if (stringLength > maxLength * 4)
      throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + stringLength + " > " + maxLength * 4 + ")");

    if (stringLength < 0)
      throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");

    String string = buffer.toString(buffer.readerIndex(), stringLength, StandardCharsets.UTF_8);

    buffer.readerIndex(buffer.readerIndex() + stringLength);

    if (string.length() > maxLength)
      throw new DecoderException("The received string length is longer than maximum allowed (" + stringLength + " > " + maxLength + ")");

    return string;
  }
}
