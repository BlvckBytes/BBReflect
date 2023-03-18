package me.blvckbytes.bbreflect.packets.communicator;

import io.netty.buffer.ByteBuf;
import me.blvckbytes.autowirer.ICleanable;
import me.blvckbytes.autowirer.IInitializable;
import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.RClass;
import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.handle.FieldHandle;
import me.blvckbytes.bbreflect.handle.predicate.Assignability;
import me.blvckbytes.bbreflect.packets.EPriority;
import me.blvckbytes.bbreflect.packets.IPacketInterceptorRegistry;
import me.blvckbytes.bbreflect.packets.IPacketOwner;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CustomPayloadCommunicator implements ICustomPayloadCommunicator, IInitializable, ICleanable {

  private final ClassHandle C_PI_CUSTOM_PAYLOAD;
  private final @Nullable FieldHandle F_PI_CUSTOM_PAYLOAD__STRING_KEY, F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY;
  private final FieldHandle F_PI_CUSTOM_PAYLOAD__DATA;
  private final IPacketInterceptorRegistry packetInterceptor;
  private final Set<FCustomPayloadReceiver> receivers;

  public CustomPayloadCommunicator(
    IReflectionHelper reflectionHelper,
    IPacketInterceptorRegistry packetInterceptor
  ) throws Exception {
    this.packetInterceptor = packetInterceptor;
    this.receivers = new HashSet<>();

    C_PI_CUSTOM_PAYLOAD = reflectionHelper.getClass(RClass.PACKET_I_CUSTOM_PAYLOAD);

    F_PI_CUSTOM_PAYLOAD__STRING_KEY = C_PI_CUSTOM_PAYLOAD.locateField()
      .withType(String.class)
      .optional();

    F_PI_CUSTOM_PAYLOAD__DATA = C_PI_CUSTOM_PAYLOAD.locateField()
      .withType(ByteBuf.class, false, Assignability.TYPE_TO_TARGET)
      .required();

    // MinecraftKey is being used since 1_13 in the custom payload packet
    if (reflectionHelper.getVersion().compare(ServerVersion.V1_13_R0) >= 0) {
      ClassHandle C_MINECRAFT_KEY = reflectionHelper.getClassOptional(RClass.MINECRAFT_KEY);
      F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY = C_PI_CUSTOM_PAYLOAD.locateField()
        .withType(C_MINECRAFT_KEY, false, Assignability.TYPE_TO_TARGET)
        .required();
    }

    else
      F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY = null;

    if (F_PI_CUSTOM_PAYLOAD__STRING_KEY == null && F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY == null)
      throw new IllegalStateException("Couldn't find either the string key or minecraft key field");
  }

  private @Nullable Object interceptIncoming(IPacketOwner owner, Object packet, Object channel) throws Exception {
    Player player = owner.getPlayer();
    if (player == null)
      return packet;

    if (C_PI_CUSTOM_PAYLOAD.isInstance(packet)) {
      String key;

      if (F_PI_CUSTOM_PAYLOAD__STRING_KEY != null)
        key = (String) F_PI_CUSTOM_PAYLOAD__STRING_KEY.get(packet);
      else {
        assert F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY != null;
        key = String.valueOf(F_PI_CUSTOM_PAYLOAD__MINECRAFT_KEY.get(packet));
      }

      ByteBuf data = (ByteBuf) F_PI_CUSTOM_PAYLOAD__DATA.get(packet);

      for (FCustomPayloadReceiver receiver : receivers)
        receiver.receive(owner, key, data);
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

  @Override
  public void registerReceiver(FCustomPayloadReceiver receiver) {
    this.receivers.add(receiver);
  }

  @Override
  public void unregisterReceiver(FCustomPayloadReceiver receiver) {
    this.receivers.remove(receiver);
  }
}
