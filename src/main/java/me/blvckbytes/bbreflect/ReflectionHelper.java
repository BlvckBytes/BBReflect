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

package me.blvckbytes.bbreflect;

import io.netty.channel.Channel;
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
    F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL;

  private final MethodHandle M_NETWORK_MANAGER__SEND;

  private final WeakHashMap<Player, Tuple<Object, Channel>> networkManagerAndChannelCache;

  public ReflectionHelper(@Nullable Supplier<String> versionSupplier) throws Exception {
    this.versionStr = versionSupplier == null ? findVersion() : versionSupplier.get();
    this.version = parseVersion(this.versionStr);
    this.networkManagerAndChannelCache = new WeakHashMap<>();

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
      .orElse(() -> (
        C_NETWORK_MANAGER.locateMethod()
          .withVersionRange(null, ServerVersion.V1_12_R2)
          .withTransformer(args -> new Object[] { args[0], args[1], new GenericFutureListener[0] })
          .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
          .withParameter(GenericFutureListener.class)
          .withParameter(GenericFutureListener[].class)
      ))
      .required();

    F_NETWORK_MANAGER__CHANNEL = C_NETWORK_MANAGER.locateField()
      .withType(Channel.class)
      .required();
  }

  private Tuple<Object, Channel> findNetworkManagerAndChannel(Player player) throws Exception {
    Object entityPlayer = F_CRAFT_PLAYER__HANDLE.get(player);
    Object playerConnection = F_ENTITY_PLAYER__CONNECTION.get(entityPlayer);
    Object networkManager = F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);
    Object channel = F_NETWORK_MANAGER__CHANNEL.get(networkManager);
    return new Tuple<>(networkManager, (Channel) channel);
  }

  public void sendPacket(Object networkManager, Object packet, @Nullable Runnable completion) throws Exception {
    GenericFutureListener<? extends Future<? super Void>> listener = null;

    if (completion != null)
      listener = v -> completion.run();

    M_NETWORK_MANAGER__SEND.invoke(networkManager, packet, listener);
  }

  public void sendPacket(Player player, Object packet, @Nullable Runnable completion) throws Exception {
    Tuple<Object, Channel> networkManagerAndChannel = networkManagerAndChannelCache.get(player);

    if (networkManagerAndChannel == null) {
      networkManagerAndChannel = findNetworkManagerAndChannel(player);
      networkManagerAndChannelCache.put(player, networkManagerAndChannel);
    }

    if (!networkManagerAndChannel.getB().isOpen())
      return;

    sendPacket(networkManagerAndChannel.getA(), packet, completion);
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
