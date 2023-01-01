package me.blvckbytes.bbreflect.packets;

import org.jetbrains.annotations.Nullable;

public interface IPacketOperator {

  /**
   * Used to extract the player-name from a LoginIn-Packet
   * @param packet Any packet, maybe a LoginIn-Packet
   * @return Name on success, null otherwise
   */
  @Nullable String tryExtractName(Object packet) throws Exception;

  /**
   * Used to send a packet using a network manager instance
   * @param packet Packet instance to send
   * @param completion Optional completion callback, nullable
   * @param networkManager Network manager reference to invoke sending on
   */
  void sendPacket(Object packet, @Nullable Runnable completion, Object networkManager) throws Exception;

}
