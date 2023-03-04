package me.blvckbytes.bbreflect.packets.communicator;

import me.blvckbytes.bbreflect.packets.IPacketOwner;

@FunctionalInterface
public interface FCustomPayloadReceiver {

  void receive(IPacketOwner owner, String key, Object data);

}
