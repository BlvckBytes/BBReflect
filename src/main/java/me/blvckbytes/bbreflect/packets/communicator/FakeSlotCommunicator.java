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
import me.blvckbytes.bbreflect.handle.*;
import me.blvckbytes.bbreflect.handle.predicate.Assignability;
import me.blvckbytes.bbreflect.packets.EPriority;
import me.blvckbytes.bbreflect.packets.IPacketInterceptorRegistry;
import me.blvckbytes.bbreflect.packets.IPacketOwner;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FakeSlotCommunicator implements IFakeSlotCommunicator, IInitializable, ICleanable, Listener {

  private static final ItemStack ITEM_AIR = new ItemStack(Material.AIR, 1);

  private final Map<Player, WindowItemsBlockingSession> windowItemsBlockedPlayers;
  private final Set<Object> sentSetSlotPackets;

  private final ConstructorHandle CT_PO_SET_SLOT;
  private final ClassHandle C_PO_SET_SLOT, C_PO_WINDOW_ITEMS, C_PI_WINDOW_CLICK;
  private final FieldHandle F_PO_WINDOW_ITEMS__WINDOW_ID, F_PO_SET_SLOT__WINDOW_ID, F_PO_SET_SLOT__ITEM,
    F_PO_SET_SLOT__STATE_ID_OR_SLOT_ID, F_PI_WINDOW_CLICK__INVENTORY_CLICK_TYPE_ORDINAL;
  private final @Nullable FieldHandle F_PO_SET_SLOT__SLOT_ID;
  private final MethodHandle M_AS_NMS_COPY;

  private final IReflectionHelper reflectionHelper;
  private final Logger logger;
  private final IWindowOpenCommunicator windowOpenWatcher;
  private final IPacketInterceptorRegistry interceptorRegistry;

  public FakeSlotCommunicator(
    IReflectionHelper reflectionHelper,
    Logger logger,
    IWindowOpenCommunicator windowOpenWatcher,
    IPacketInterceptorRegistry interceptorRegistry
  ) throws Exception {
    this.reflectionHelper = reflectionHelper;
    this.interceptorRegistry = interceptorRegistry;
    this.logger = logger;
    this.windowOpenWatcher = windowOpenWatcher;
    this.windowItemsBlockedPlayers = new HashMap<>();
    this.sentSetSlotPackets = new HashSet<>();

    C_PO_WINDOW_ITEMS = reflectionHelper.getClass(RClass.PACKET_O_WINDOW_ITEMS);
    C_PO_SET_SLOT = reflectionHelper.getClass(RClass.PACKET_O_SET_SLOT);
    C_PI_WINDOW_CLICK = reflectionHelper.getClass(RClass.PACKET_I_WINDOW_CLICK);
    ClassHandle E_INVENTORY_CLICK_TYPE = reflectionHelper.getClass(RClass.INVENTORY_CLICK_TYPE);

    F_PI_WINDOW_CLICK__INVENTORY_CLICK_TYPE_ORDINAL = C_PI_WINDOW_CLICK.locateField()
      .withType(E_INVENTORY_CLICK_TYPE)
      .withResponseTransformer(value -> {
        assert value != null;
        return (byte) ((Enum<?>) value).ordinal();
      })
      // In 1.8 and below, the click type has been a byte, which exactly represented
      // the ordinal value of the nowadays enumeration
      .orElse(() -> (
        C_PI_WINDOW_CLICK.locateField()
        .withVersionRange(null, ServerVersion.V1_8_R9)
        .withType(byte.class)
      ))
      .required();


    ClassHandle C_ITEM_STACK = reflectionHelper.getClass(RClass.ITEM_STACK);
    ClassHandle C_CRAFT_ITEM_STACK = reflectionHelper.getClass(RClass.CRAFT_ITEM_STACK);

    M_AS_NMS_COPY = C_CRAFT_ITEM_STACK.locateMethod()
      .withPublic(true)
      .withStatic(true)
      .withName("asNMSCopy")
      .required();

    /*
      constructor(
        int windowId,
        int slot,
        ItemStack item
      )
     */
    CT_PO_SET_SLOT = C_PO_SET_SLOT.locateConstructor()
      .withVersionRange(ServerVersion.V1_18_R0, null)
      .withParameters(int.class, int.class, int.class)
      .withParameter(C_ITEM_STACK, false, Assignability.TYPE_TO_TARGET)
      .withCallTransformer(args -> (
        // Looks like just setting the state ID to zero works out in all cases
        // These kind of user interfaces this is used on don't really carry state
        new Object[] { args[0], 0, args[1], M_AS_NMS_COPY.invoke(null, args[2]) }
      ), M_AS_NMS_COPY)
      .orElse(() -> (
         C_PO_SET_SLOT.locateConstructor()
          .withVersionRange(null, ServerVersion.V1_17_R0)
          .withParameters(int.class, int.class)
          .withParameter(C_ITEM_STACK, false, Assignability.TYPE_TO_TARGET)
          .withCallTransformer(args -> (
            new Object[] { args[0], args[1], M_AS_NMS_COPY.invoke(null, args[2]) }
          ), M_AS_NMS_COPY)
      ))
      .required();

    F_PO_SET_SLOT__WINDOW_ID = C_PO_SET_SLOT.locateField()
      .withType(int.class)
      .required();

    F_PO_SET_SLOT__STATE_ID_OR_SLOT_ID = C_PO_SET_SLOT.locateField()
      .withType(int.class)
      .withSkip(1)
      .required();

    F_PO_SET_SLOT__SLOT_ID = C_PO_SET_SLOT.locateField()
      .withType(int.class)
      .withSkip(2)
      .optional();

    F_PO_SET_SLOT__ITEM = C_PO_SET_SLOT.locateField()
      .withType(C_ITEM_STACK)
      .required();

    F_PO_WINDOW_ITEMS__WINDOW_ID = C_PO_WINDOW_ITEMS.locateField()
      .withType(int.class)
      .required();
  }

  @Override
  public void setFakeSlot(Player player, int slotId, boolean top, ItemStack item) {
    try {
      int windowId = 0;

      // Top inventory, using raw slots
      if (top) {
        windowId = windowOpenWatcher.getCurrentTopInventoryWindowId(player);

        if (windowId < 0)
          return;
      }

      if (item == null)
        item = ITEM_AIR;

      Object packet = CT_PO_SET_SLOT.newInstance(windowId, slotId, item);
      sentSetSlotPackets.add(packet);

      this.reflectionHelper.sendPacket(player, packet, null);
    } catch (Exception e) {
      this.logger.log(Level.SEVERE, e, () -> "Could not set a fake slot");
    }
  }

  @Override
  public void blockWindowItems(Player player, EnumSet<EInventoryType> targets, FFakeItemSupplier supplier) {
    windowItemsBlockedPlayers.put(player, new WindowItemsBlockingSession(targets, supplier));
  }

  @Override
  public void unblockWindowItems(Player player) {
    windowItemsBlockedPlayers.remove(player);
  }

  @Override
  public @Nullable EInventoryClickType getLastReceivedClickType(Player player) {
    WindowItemsBlockingSession blockingSession = windowItemsBlockedPlayers.get(player);
    if (blockingSession == null)
      return null;
    return blockingSession.lastReceivedClickType;
  }

  private @Nullable Object interceptIncoming(IPacketOwner packetOwner, Object packet, Object channel) throws Exception {
    Player player = packetOwner.getPlayer();
    if (player == null)
      return packet;

    WindowItemsBlockingSession blockingSession = windowItemsBlockedPlayers.get(player);
    if (blockingSession == null)
      return packet;

    if (C_PI_WINDOW_CLICK.isInstance(packet)) {
      byte clickTypeOrdinal = (byte) F_PI_WINDOW_CLICK__INVENTORY_CLICK_TYPE_ORDINAL.get(packet);
      blockingSession.lastReceivedClickType = EInventoryClickType.fromOrdinal(clickTypeOrdinal, reflectionHelper.getVersion());
    }

    return packet;
  }

  private @Nullable Object interceptOutgoing(IPacketOwner packetOwner, Object packet, Object channel) throws Exception {
    Player player = packetOwner.getPlayer();
    if (player == null)
      return packet;

    if (C_PO_WINDOW_ITEMS.isInstance(packet)) {
      WindowItemsBlockingSession blockingSession = windowItemsBlockedPlayers.get(player);

      if (blockingSession == null)
        return packet;

      int windowId = (int) F_PO_WINDOW_ITEMS__WINDOW_ID.get(packet);

      if (!isWindowIdBlocked(blockingSession, windowId))
        return packet;

      return null;
    }

    if (C_PO_SET_SLOT.isInstance(packet)) {
      // Don't modify packets that we've sent ourselves
      if (sentSetSlotPackets.remove(packet))
        return packet;

      WindowItemsBlockingSession blockingSession = windowItemsBlockedPlayers.get(player);

      if (blockingSession == null)
        return packet;

      int windowId = (int) F_PO_SET_SLOT__WINDOW_ID.get(packet);

      if (!isWindowIdBlocked(blockingSession, windowId))
        return packet;

      int slotId;

      if (F_PO_SET_SLOT__SLOT_ID != null)
        slotId = (int) F_PO_SET_SLOT__SLOT_ID.get(packet);
      else
        slotId = (int) F_PO_SET_SLOT__STATE_ID_OR_SLOT_ID.get(packet);

      // Setting item on cursor
      // No need to intervene, as fake items are always cancelled and so this
      // will only clear the cursor (which is desired anyways)
      if (windowId == -1 && slotId == -1)
        return packet;

      ItemStack fakeItem = blockingSession.itemSupplier.apply(slotId);

      if (fakeItem == null)
        return packet;

      F_PO_SET_SLOT__ITEM.set(packet, M_AS_NMS_COPY.invoke(null, fakeItem));
    }

    return packet;
  }

  @Override
  public void cleanup() {
    interceptorRegistry.unregisterOutboundPacketInterceptor(this::interceptOutgoing);
    interceptorRegistry.unregisterOutboundPacketInterceptor(this::interceptIncoming);
  }

  @Override
  public void initialize() {
    interceptorRegistry.registerOutboundPacketInterceptor(this::interceptOutgoing, EPriority.LOWEST);
    interceptorRegistry.registerInboundPacketInterceptor(this::interceptIncoming, EPriority.LOWEST);
  }

  private boolean isWindowIdBlocked(WindowItemsBlockingSession blockingSession, int windowId) {
    if (windowId == -1)
      return blockingSession.doesTargetType(EInventoryType.BOTTOM);
    return blockingSession.doesTargetType(EInventoryType.TOP);
  }
}
