package me.blvckbytes.bbreflection.packets;

import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FPacketInterceptor {

  /**
   * Called whenever a packet passes through the intercepted pipeline
   * @param senderName Name of the sender, only non-null at and after the LoginIn-Packet
   * @param packet Packet instance
   * @param channel Intercepted channel
   * @return The received (and possibly modified) packet or null to drop the packet
   */
  @Nullable Object intercept(@Nullable String senderName, Object packet, Channel channel) throws Exception;

}
