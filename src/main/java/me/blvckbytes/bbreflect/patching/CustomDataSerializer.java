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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.blvckbytes.bbreflect.packets.Interceptor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class CustomDataSerializer extends PacketDataSerializer {

  private final ChannelHandlerContext context;

  public CustomDataSerializer(ByteBuf bytebuf, ChannelHandlerContext context) {
    super(bytebuf);
    this.context = context;
  }

  @Override
  public PacketDataSerializer a(@Nullable NBTTagCompound nbttagcompound) {
    BiFunction<EMethodType, Object, Object> interceptionHandler = context.channel().attr(Interceptor.HANDLER_KEY).get().get();

    if (interceptionHandler != null)
      nbttagcompound = (NBTTagCompound) interceptionHandler.apply(EMethodType.NBT_TAG_COMPOUND_HEAD, nbttagcompound);

    super.a(nbttagcompound);

    if (interceptionHandler != null)
      interceptionHandler.apply(EMethodType.NBT_TAG_COMPOUND_TAIL, nbttagcompound);

    return this;
  }

  @Override
  public PacketDataSerializer a(IChatBaseComponent ichatbasecomponent) {
    BiFunction<EMethodType, Object, Object> interceptionHandler = context.channel().attr(Interceptor.HANDLER_KEY).get().get();

    if (interceptionHandler != null)
      ichatbasecomponent = (IChatBaseComponent) interceptionHandler.apply(EMethodType.I_CHAT_BASE_COMPONENT_HEAD, ichatbasecomponent);

    super.a(ichatbasecomponent);

    if (interceptionHandler != null)
      interceptionHandler.apply(EMethodType.I_CHAT_BASE_COMPONENT_TAIL, ichatbasecomponent);

    return this;
  }
}
