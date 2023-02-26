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

public class ByteBufBuffer implements IBinaryBuffer {

  private final ByteBuf buffer;

  public ByteBufBuffer(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public byte getByte(int index) {
    return this.buffer.getByte(index);
  }

  @Override
  public void setByte(int index, byte value) {
    this.buffer.setByte(index, value);
  }

  public int capacity() {
    return this.buffer.capacity();
  }

  @Override
  public byte[] asByteArray() {
    return this.buffer.array();
  }

  @Override
  public Object asByteBuf() {
    return this.buffer;
  }
}
