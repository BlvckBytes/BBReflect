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
import me.blvckbytes.bbreflect.packets.IPacketOwner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class WindowOpenCommunicator implements IWindowOpenCommunicator, Listener, IInitializable, ICleanable {

  private final Map<Player, Integer> topInventoryWindowIdByPlayer;
  private final ClassHandle C_PI_CLOSE_WINDOW, C_PO_OPEN_WINDOW;
  private final FieldHandle C_PI_CLOSE_WINDOW__WINDOW_ID, C_PO_OPEN_WINDOW__WINDOW_ID;
  private final IPacketInterceptorRegistry packetInterceptor;

  public WindowOpenCommunicator(IReflectionHelper reflectionHelper, IPacketInterceptorRegistry packetInterceptor) throws Exception {
    this.packetInterceptor = packetInterceptor;
    this.topInventoryWindowIdByPlayer = new HashMap<>();

    C_PI_CLOSE_WINDOW = reflectionHelper.getClass(RClass.PACKET_I_CLOSE_WINDOW);
    C_PO_OPEN_WINDOW = reflectionHelper.getClass(RClass.PACKET_O_OPEN_WINDOW);

    C_PI_CLOSE_WINDOW__WINDOW_ID = C_PI_CLOSE_WINDOW.locateField()
      .withType(int.class)
      .required();

    C_PO_OPEN_WINDOW__WINDOW_ID = C_PO_OPEN_WINDOW.locateField()
      .withType(int.class)
      .required();
  }

  @Override
  public int getCurrentTopInventoryWindowId(Player player) {
    Integer windowId = this.topInventoryWindowIdByPlayer.get(player);

    if (windowId == null)
      return -1;

    return windowId;
  }

  private @Nullable Object interceptIncoming(IPacketOwner owner, Object packet, Object channel) throws Exception {
    Player player = owner.getPlayer();
    if (player == null)
      return packet;

    if (C_PI_CLOSE_WINDOW.isInstance(packet)) {
      int windowId = (int) C_PI_CLOSE_WINDOW__WINDOW_ID.get(packet);
      Integer currentWindowId = topInventoryWindowIdByPlayer.get(player);

      if (currentWindowId == null)
        return packet;

      if (currentWindowId == windowId)
        topInventoryWindowIdByPlayer.remove(player);
    }

    return packet;
  }

  private @Nullable Object interceptOutgoing(IPacketOwner owner, Object packet, Object channel) throws Exception {
    Player player = owner.getPlayer();
    if (player == null)
      return packet;

    if (C_PO_OPEN_WINDOW.isInstance(packet)) {
      int windowId = (int) C_PO_OPEN_WINDOW__WINDOW_ID.get(packet);
      topInventoryWindowIdByPlayer.put(player, windowId);
    }

    return packet;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.topInventoryWindowIdByPlayer.remove(event.getPlayer());
  }

  @Override
  public void cleanup() {
    this.packetInterceptor.unregisterInboundPacketInterceptor(this::interceptIncoming);
    this.packetInterceptor.unregisterOutboundPacketInterceptor(this::interceptOutgoing);
  }

  @Override
  public void initialize() {
    this.packetInterceptor.registerInboundPacketInterceptor(this::interceptIncoming, EPriority.LOWEST);
    this.packetInterceptor.registerOutboundPacketInterceptor(this::interceptOutgoing, EPriority.LOWEST);
  }
}
