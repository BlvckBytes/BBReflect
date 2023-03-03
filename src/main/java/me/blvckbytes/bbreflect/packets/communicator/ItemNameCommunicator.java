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

import me.blvckbytes.autowirer.ICleanable;
import me.blvckbytes.autowirer.IInitializable;
import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.RClass;
import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.handle.FieldHandle;
import me.blvckbytes.bbreflect.packets.EPriority;
import me.blvckbytes.bbreflect.packets.IPacketInterceptorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ItemNameCommunicator implements IItemNameCommunicator, IInitializable, ICleanable {

  private final ClassHandle C_PI_ITEM_NAME;
  private final FieldHandle C_PI_ITEM_NAME__NAME;
  private final Set<FItemNameReceiver> receivers;
  private final IPacketInterceptorRegistry packetInterceptor;

  public ItemNameCommunicator(IReflectionHelper reflectionHelper, IPacketInterceptorRegistry packetInterceptor) throws Exception {
    this.packetInterceptor = packetInterceptor;
    this.receivers = new HashSet<>();

    C_PI_ITEM_NAME = reflectionHelper.getClass(RClass.PACKET_I_ITEM_NAME);

    C_PI_ITEM_NAME__NAME = C_PI_ITEM_NAME.locateField()
      .withType(String.class)
      .required();
  }

  @Override
  public void registerReceiver(FItemNameReceiver receiver) {
    this.receivers.add(receiver);
  }

  @Override
  public void unregisterReceiver(FItemNameReceiver receiver) {
    this.receivers.remove(receiver);
  }

  private @Nullable Object interceptIncoming(@Nullable String playerName, Object packet, Object channel) throws Exception {
    if (playerName == null)
      return packet;

    if (C_PI_ITEM_NAME.isInstance(packet)) {
      Player player = Bukkit.getPlayer(playerName);

      if (player == null)
        return packet;

      String name = (String) C_PI_ITEM_NAME__NAME.get(packet);
      for (FItemNameReceiver receiver : receivers)
        receiver.receive(player, name);
    }

    return packet;
  }

  @Override
  public void cleanup() {
    this.packetInterceptor.unregisterInboundPacketInterceptor(this::interceptIncoming);
  }

  @Override
  public void initialize() {
    this.packetInterceptor.registerInboundPacketInterceptor(this::interceptIncoming, EPriority.LOWEST);
  }
}
