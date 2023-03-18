package me.blvckbytes.bbreflect.packets.communicator;

import io.netty.buffer.ByteBuf;
import me.blvckbytes.bbreflect.packets.IPacketOwner;

@FunctionalInterface
public interface FCustomPayloadReceiver {

  void receive(IPacketOwner owner, String key, ByteBuf data);

}
