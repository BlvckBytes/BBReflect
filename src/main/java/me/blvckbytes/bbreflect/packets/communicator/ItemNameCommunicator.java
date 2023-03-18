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

package me.blvckbytes.bbreflect.packets.communicator;

import io.netty.buffer.ByteBuf;
import me.blvckbytes.autowirer.ICleanable;
import me.blvckbytes.autowirer.IInitializable;
import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.RClass;
import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.handle.FieldHandle;
import me.blvckbytes.bbreflect.packets.BufferReader;
import me.blvckbytes.bbreflect.packets.EPriority;
import me.blvckbytes.bbreflect.packets.IPacketInterceptorRegistry;
import me.blvckbytes.bbreflect.packets.IPacketOwner;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemNameCommunicator implements IItemNameCommunicator, IInitializable, ICleanable {

  private static final int MAX_NAME_BYTES_LENGTH = 32767;
  private static final String ITEM_NAME_KEY = "MC|ItemName";

  private final @Nullable ClassHandle C_PI_ITEM_NAME;
  private final @Nullable FieldHandle C_PI_ITEM_NAME__NAME;

  private final Set<FItemNameReceiver> receivers;
  private final IPacketInterceptorRegistry packetInterceptor;
  private final ICustomPayloadCommunicator customPayloadCommunicator;
  private final Logger logger;

  public ItemNameCommunicator(
    IReflectionHelper reflectionHelper,
    IPacketInterceptorRegistry packetInterceptor,
    ICustomPayloadCommunicator customPayloadCommunicator,
    Logger logger
  ) throws Exception {
    this.logger = logger;
    this.packetInterceptor = packetInterceptor;
    this.customPayloadCommunicator = customPayloadCommunicator;
    this.receivers = new HashSet<>();

    if (reflectionHelper.getVersion().compare(ServerVersion.V1_13_R0) >= 0) {
      C_PI_ITEM_NAME = reflectionHelper.getClass(RClass.PACKET_I_ITEM_NAME);
      C_PI_ITEM_NAME__NAME = C_PI_ITEM_NAME.locateField()
        .withType(String.class)
        .required();
    }

    // Before 1.13, there was no PacketPlayInItemName
    else {
      C_PI_ITEM_NAME = null;
      C_PI_ITEM_NAME__NAME = null;
    }
  }

  @Override
  public void registerReceiver(FItemNameReceiver receiver) {
    this.receivers.add(receiver);
  }

  @Override
  public void unregisterReceiver(FItemNameReceiver receiver) {
    this.receivers.remove(receiver);
  }

  void receiveCustomPayloadData(IPacketOwner owner, String key, ByteBuf data) {
    if (!key.equals(ITEM_NAME_KEY))
      return;

    Player player = owner.getPlayer();
    if (player == null)
      return;

    try {
      String name = BufferReader.readUTF8(data, MAX_NAME_BYTES_LENGTH);
      data.resetReaderIndex();

      for (FItemNameReceiver receiver : receivers)
        receiver.receive(player, name);
    } catch (Exception e) {
      this.logger.log(Level.SEVERE, e, () -> "Could not process custom payload data");
    }
  }

  private @Nullable Object interceptIncoming(IPacketOwner owner, Object packet, Object channel) throws Exception {
    Player player = owner.getPlayer();
    if (player == null)
      return packet;

    if (C_PI_ITEM_NAME != null && C_PI_ITEM_NAME.isInstance(packet)) {
      assert C_PI_ITEM_NAME__NAME != null;
      String name = (String) C_PI_ITEM_NAME__NAME.get(packet);
      for (FItemNameReceiver receiver : receivers)
        receiver.receive(player, name);
    }

    return packet;
  }

  @Override
  public void cleanup() {
    this.packetInterceptor.unregisterInboundPacketInterceptor(this::interceptIncoming);
    this.customPayloadCommunicator.unregisterReceiver(this::receiveCustomPayloadData);
  }

  @Override
  public void initialize() {
    this.packetInterceptor.registerInboundPacketInterceptor(this::interceptIncoming, EPriority.LOWEST);

    // Packet doesn't exist yet, get the name through custom payload packets
    if (C_PI_ITEM_NAME == null)
      this.customPayloadCommunicator.registerReceiver(this::receiveCustomPayloadData);
  }
}
