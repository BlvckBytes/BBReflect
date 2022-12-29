package me.blvckbytes.bbreflection.packets;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FPacketPlayerNameExtractor {

  /**
   * Used to extract the player-name from a LoginIn-Packet
   * @param packet Any packet, maybe a LoginIn-Packet
   * @return Name on success, null otherwise
   */
  @Nullable String extract(Object packet) throws Exception;

}
