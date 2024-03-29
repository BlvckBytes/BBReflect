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

import me.blvckbytes.autowirer.ICleanable;
import me.blvckbytes.bbreflect.ReflectionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class PacketInterceptorRegistry implements ICleanable, IPacketInterceptorRegistry {

  private final PrioritizedSet<FPacketInterceptor> inboundPacketInterceptors, outboundPacketInterceptors;
  private final PrioritizedSet<FBytesInterceptor> inboundBytesInterceptors, outboundBytesInterceptors;
  private final PrioritizedSet<IExternalInterceptorFeature> externalInterceptorFeatures;

  private final InterceptorFactory interceptorFactory;

  public PacketInterceptorRegistry(
    JavaPlugin plugin,
    Logger logger,
    ReflectionHelper reflectionHelper
  ) throws Exception {
    this.inboundPacketInterceptors = new PrioritizedSet<>();
    this.outboundPacketInterceptors = new PrioritizedSet<>();
    this.inboundBytesInterceptors = new PrioritizedSet<>();
    this.outboundBytesInterceptors = new PrioritizedSet<>();
    this.externalInterceptorFeatures = new PrioritizedSet<>();

    this.interceptorFactory = new InterceptorFactory(externalInterceptorFeatures, logger, reflectionHelper, plugin.getName());
    Bukkit.getPluginManager().registerEvents(this.interceptorFactory, plugin);

    this.interceptorFactory.setupInterception(this::setupInterceptor);
  }

  @Override
  public void cleanup() {
    this.interceptorFactory.cleanupInterception();
  }

  @Override
  public void registerInboundPacketInterceptor(FPacketInterceptor interceptor, EPriority priority) {
    this.inboundPacketInterceptors.add(interceptor, priority);
  }

  @Override
  public void unregisterInboundPacketInterceptor(FPacketInterceptor interceptor) {
    this.inboundPacketInterceptors.remove(interceptor);
  }

  @Override
  public void registerOutboundPacketInterceptor(FPacketInterceptor interceptor, EPriority priority) {
    this.outboundPacketInterceptors.add(interceptor, priority);
  }

  @Override
  public void unregisterOutboundPacketInterceptor(FPacketInterceptor interceptor) {
    this.outboundPacketInterceptors.remove(interceptor);
  }

  @Override
  public void registerInboundBytesInterceptor(FBytesInterceptor interceptor, EPriority priority) {
    this.inboundBytesInterceptors.add(interceptor, priority);
  }

  @Override
  public void unregisterInboundBytesInterceptor(FBytesInterceptor interceptor) {
    this.inboundBytesInterceptors.remove(interceptor);
  }

  @Override
  public void registerOutboundBytesInterceptor(FBytesInterceptor interceptor, EPriority priority) {
    this.outboundBytesInterceptors.add(interceptor, priority);
  }

  @Override
  public void unregisterOutboundBytesInterceptor(FBytesInterceptor interceptor) {
    this.outboundBytesInterceptors.remove(interceptor);
  }

  @Override
  public void registerExternalInterceptorFeature(IExternalInterceptorFeature feature, EPriority priority) {
    this.externalInterceptorFeatures.add(feature, priority);
  }

  @Override
  public void unregisterExternalInterceptorFeature(IExternalInterceptorFeature feature) {
    this.externalInterceptorFeatures.remove(feature);
  }

  @Override
  public @Nullable IInterceptor getPlayerInterceptor(Player p) {
    return this.interceptorFactory.getPlayerInterceptor(p);
  }

  private @Nullable Object callPacketInterceptors(Iterable<FPacketInterceptor> interceptors, IPacketOwner owner, Object packet, Object channel) throws Exception {
    Object resultingPacket = packet;

    for (FPacketInterceptor interceptor : interceptors) {
      resultingPacket = interceptor.intercept(owner, resultingPacket, channel);
      if (resultingPacket == null)
        break;
    }

    return resultingPacket;
  }

  private @Nullable Object callBytesInterceptors(Iterable<FBytesInterceptor> interceptors, IPacketOwner owner, Object buffer, Object channel) throws Exception {
    Object resultingBuffer = buffer;

    for (FBytesInterceptor interceptor : interceptors) {
      resultingBuffer = interceptor.intercept(owner, resultingBuffer, channel);
      if (resultingBuffer == null)
        break;
    }

    return resultingBuffer;
  }

  private void setupInterceptor(IInterceptor interceptor) {
    interceptor.setInboundPacketInterceptor((owner, packet, channel) -> this.callPacketInterceptors(inboundPacketInterceptors, owner, packet, channel));
    interceptor.setOutboundPacketInterceptor((owner, packet, channel) -> this.callPacketInterceptors(outboundPacketInterceptors, owner, packet, channel));
    interceptor.setInboundBytesInterceptor((owner, buffer, channel) -> this.callBytesInterceptors(inboundBytesInterceptors, owner, buffer, channel));
    interceptor.setOutboundBytesInterceptor((owner, buffer, channel) -> this.callBytesInterceptors(outboundBytesInterceptors, owner, buffer, channel));
  }
}
