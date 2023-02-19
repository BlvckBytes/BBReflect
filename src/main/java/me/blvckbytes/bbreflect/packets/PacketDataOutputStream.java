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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PacketDataOutputStream extends DataOutputStream {

  public PacketDataOutputStream(OutputStream out) {
    super(out);
  }

  public void writeVarInt(int i) throws IOException {
    while((i & -128) != 0) {
      write(i & 127 | 128);
      i >>>= 7;
    }

    write(i);
  }

  public void writeVarLong(long i) throws IOException {
    while((i & -128L) != 0L) {
      write((int)(i & 127L) | 128);
      i >>>= 7;
    }

    write((int)i);
  }
}
