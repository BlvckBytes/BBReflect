package me.blvckbytes.bbreflect;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;
import java.util.function.Supplier;

public class ReflectionHelper {

  // Server version information
  @Getter private final String versionStr;
  @Getter private final ServerVersion version;

  private final FieldHandle
    F_CRAFT_PLAYER__HANDLE,
    F_ENTITY_PLAYER__CONNECTION,
    F_PLAYER_CONNECTION__NETWORK_MANAGER;

  private final MethodHandle M_NETWORK_MANAGER__SEND;

  private final WeakHashMap<Player, Object> networkManagerCache;

  public ReflectionHelper(@Nullable Supplier<String> versionSupplier) throws Exception {
    this.versionStr = versionSupplier == null ? findVersion() : versionSupplier.get();
    this.version = parseVersion(this.versionStr);
    this.networkManagerCache = new WeakHashMap<>();

    ClassHandle C_CRAFT_PLAYER = getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_ENTITY_PLAYER = getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_NETWORK_MANAGER = getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_PACKET = getClass(RClass.PACKET);

    F_CRAFT_PLAYER__HANDLE = C_CRAFT_PLAYER.locateField()
      .withType(C_ENTITY_PLAYER, false, Assignability.TARGET_TO_TYPE)
      .withAllowSuperclass(true)
      .required();

    F_ENTITY_PLAYER__CONNECTION = C_ENTITY_PLAYER.locateField()
      .withType(C_PLAYER_CONNECTION, false, Assignability.TARGET_TO_TYPE)
      .withAllowSuperclass(true)
      .required();

    F_PLAYER_CONNECTION__NETWORK_MANAGER = C_PLAYER_CONNECTION.locateField()
      .withType(C_NETWORK_MANAGER, false, Assignability.TARGET_TO_TYPE)
      .withAllowSuperclass(true)
      .required();

    M_NETWORK_MANAGER__SEND = C_NETWORK_MANAGER.locateMethod()
      .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
      .withParameter(GenericFutureListener.class)
      .required();
  }

  private Object findNetworkManager(Player player) throws Exception {
    Object entityPlayer = F_CRAFT_PLAYER__HANDLE.get(player);
    Object playerConnection = F_ENTITY_PLAYER__CONNECTION.get(entityPlayer);
    return F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);
  }

  public void sendPacket(Player player, Object packet, @Nullable Runnable completion) throws Exception {
    Object networkManager = networkManagerCache.get(player);

    if (networkManager == null) {
      networkManager = findNetworkManager(player);
      networkManagerCache.put(player, networkManager);
    }

    GenericFutureListener<? extends Future<? super Void>> listener = null;

    if (completion != null)
      listener = v -> completion.run();

    M_NETWORK_MANAGER__SEND.invoke(networkManager, packet, listener);
  }

  public ClassHandle getClass(RClass rc) throws ClassNotFoundException {
    return rc.resolve(this.version);
  }

  public @Nullable ClassHandle getClassOptional(RClass rc) {
    try {
      return getClass(rc);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public @Nullable EnumHandle getEnumOptional(RClass rc) {
    try {
      return getClass(rc).asEnum();
    } catch (ClassNotFoundException | IllegalStateException e) {
      return null;
    }
  }

  /**
   * Find the server's version by looking at craftbukkit's package
   * @return Version part of the package
   */
  private String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get the major, minor and revision version numbers the server's running on
   * @return [major, minor, revision]
   */
  private ServerVersion parseVersion(String version) {
    String[] data = version.split("_");

    ServerVersion result = ServerVersion.fromVersions(
      Integer.parseInt(data[0].substring(1)), // remove leading v
      Integer.parseInt(data[1]),
      Integer.parseInt(data[2].substring(1)) // Remove leading R
    );

    if (result == null)
      throw new IllegalStateException("Unsupported version encountered: " + version);

    return result;
  }
}
