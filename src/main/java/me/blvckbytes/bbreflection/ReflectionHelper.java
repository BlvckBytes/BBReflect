package me.blvckbytes.bbreflection;

import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import lombok.Getter;
import me.blvckbytes.bbreflection.packets.Interceptor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.bukkit.Bukkit.getServer;

public class ReflectionHelper {

  private final MethodHandle M_CIS__AS_NEW_CRAFT_STACK, M_FURNACE__GET_LUT, M_CIS__GET_TYPE, M_CRAFT_PLAYER__HANDLE;
  private final ClassHandle C_PACKET_LOGIN;

  private final @Nullable FieldHandle F_PACKET_LOGIN__GAME_PROFILE;
  private @Nullable FieldHandle F_PACKET_LOGIN__NAME;

  private final Map<Material, Integer> burningTimes;

  private final FieldHandle F_CRAFT_SERVER__MINECRAFT_SERVER, F_MINECRAFT_SERVER__SERVER_CONNECTION, F_SERVER_CONNECTION__CHANNEL_FUTURES, F_ENTITY_PLAYER__PLAYER_CONNECTION, F_PLAYER_CONNECTION__NETWORK_MANAGER, F_NETWORK_MANAGER__CHANNEL;

  private final Map<Channel, ChannelInboundHandlerAdapter> channelHandlers;
  private final List<Interceptor> interceptors;

  // Server version information
  @Getter private final String versionStr;
  @Getter private final int[] versionNumbers;
  @Getter private final boolean refactored;

  public ReflectionHelper(@Nullable Supplier<String> versionSupplier) throws Exception {
    this.burningTimes = new HashMap<>();
    this.interceptors = new ArrayList<>();
    this.channelHandlers = new HashMap<>();

    this.versionStr = versionSupplier == null ? findVersion() : versionSupplier.get();
    this.versionNumbers = parseVersion(this.versionStr);
    this.refactored = this.versionNumbers[1] >= 17;

    ClassHandle C_CRAFT_PLAYER = getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_ENTITY_PLAYER = getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_NETWORK_MANAGER = getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_CRAFT_SERVER = getClass(RClass.CRAFT_SERVER);
    ClassHandle C_MINECRAFT_SERVER = getClass(RClass.MINECRAFT_SERVER);
    ClassHandle C_SERVER_CONNECTION = getClass(RClass.SERVER_CONNECTION);
    ClassHandle C_ITEM = getClass(RClass.ITEM);
    ClassHandle C_CIS = getClass(RClass.CRAFT_ITEM_STACK);
    ClassHandle C_TEF = getClass(RClass.TILE_ENTITY_FURNACE);

    C_PACKET_LOGIN = getClass(RClass.PACKET_I_LOGIN);

    M_CIS__AS_NEW_CRAFT_STACK = C_CIS.locateMethod().withName("asNewCraftStack").withParameters(C_ITEM).withStatic(true).required();
    M_CIS__GET_TYPE = C_CIS.locateMethod().withName("getType").withReturnType(Material.class).required();

    M_FURNACE__GET_LUT = C_TEF.locateMethod()
      .withReturnType(Map.class)
      .withReturnGeneric(C_ITEM)
      .withReturnGeneric(Integer.class)
      .withStatic(true)
      .required();

    F_CRAFT_SERVER__MINECRAFT_SERVER = C_CRAFT_SERVER
      .locateField()
      .withType(C_MINECRAFT_SERVER, false, Assignability.TYPE_TO_TARGET)
      .required();

    F_MINECRAFT_SERVER__SERVER_CONNECTION = C_MINECRAFT_SERVER.locateField().withType(C_SERVER_CONNECTION).required();
    F_SERVER_CONNECTION__CHANNEL_FUTURES = C_SERVER_CONNECTION.locateField().withType(List.class).withGeneric(ChannelFuture.class).required();

    F_PACKET_LOGIN__GAME_PROFILE = C_PACKET_LOGIN.locateField().withType(GameProfile.class).optional();

    if (F_PACKET_LOGIN__GAME_PROFILE == null)
      F_PACKET_LOGIN__NAME = C_PACKET_LOGIN.locateField().withType(String.class).required();

    // CraftPlayer->EntityPlayer (handle)
    M_CRAFT_PLAYER__HANDLE = C_CRAFT_PLAYER.locateMethod().withReturnType(C_ENTITY_PLAYER).required();

    // EntityPlayer->PlayerConnection
    F_ENTITY_PLAYER__PLAYER_CONNECTION = C_ENTITY_PLAYER.locateField().withType(C_PLAYER_CONNECTION).required();

    // PlayerConnection->NetworkManager
    F_PLAYER_CONNECTION__NETWORK_MANAGER = C_PLAYER_CONNECTION.locateField().withType(C_NETWORK_MANAGER).required();

    // NetworkManager -> Channel
    F_NETWORK_MANAGER__CHANNEL = C_NETWORK_MANAGER.locateField().withType(Channel.class).required();
  }

  public ClassHandle getClass(RClass rc) throws ClassNotFoundException {
    return rc.resolve(refactored, this.versionStr);
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
   * Tries to extract the sender name (GameProfile's Player-Name) from a LoginInPacket
   * @param packet Packet to extract from
   * @return Player-Name on success, null if not a LoginInPacket
   */
  public @Nullable String extractSenderNameFromPacket(Object packet) throws Exception {
    if (!C_PACKET_LOGIN.isInstance(packet))
      return null;

    if (F_PACKET_LOGIN__GAME_PROFILE != null)
      return ((GameProfile) F_PACKET_LOGIN__GAME_PROFILE.get(packet)).getName();

    assert F_PACKET_LOGIN__NAME != null;
    return (String) F_PACKET_LOGIN__NAME.get(packet);
  }

  public Optional<Integer> getBurnTime(Material mat) {
    Integer dur = burningTimes.get(mat);
    if (dur != null)
      return Optional.of(dur);

    try {
      // Iterate all entries
      Map<?, ?> lut = (Map<?, ?>) M_FURNACE__GET_LUT.invoke(null);
      for (Map.Entry<?, ?> e : lut.entrySet()) {
        Object craftStack = M_CIS__AS_NEW_CRAFT_STACK.invoke(null, e.getKey());
        Material m = (Material) M_CIS__GET_TYPE.invoke(craftStack);

        // Material mismatch, continue
        if (!mat.equals(m))
          continue;

        dur = (Integer) e.getValue();
        burningTimes.put(mat, dur);
        return Optional.of(dur);
      }

      return Optional.empty();
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Find the server's version by looking at craftbukkit's package
   * @return Version part of the package
   */
  private String findVersion() {
    return getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get the major, minor and revision version numbers the server's running on
   * @return [major, minor, revision]
   */
  private int[] parseVersion(String version) {
    String[] data = version.split("_");
    return new int[] {
      Integer.parseInt(data[0].substring(1)), // remove leading v
      Integer.parseInt(data[1]),
      Integer.parseInt(data[2].substring(1)) // Remove leading R
    };
  }

  /**
   * Attaches an initialization listener function to a channel which will get called
   * as soon as that channel is fully initialized and ready to be injected
   * @param channel Channel to attach the listener to
   * @param container Container to synchronize over while waiting for initialization
   * @param initialized Callback containing the initialized channel
   * @return Listener handle to detach after use
   */
  private ChannelInboundHandlerAdapter attachInitializationListener(Channel channel, List<?> container, Consumer<Channel> initialized) {
    // Called when a new channel has been instantiated
    ChannelInboundHandlerAdapter adapter = new ChannelInboundHandlerAdapter() {

      @Override
      public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) {
        ((Channel) o).pipeline().addFirst(new ChannelInitializer<>() {

          @Override
          protected void initChannel(Channel channel) {

            // Add this initializer as the first item in the channel to be the
            // first receiver which gets a hold of it
            channel.pipeline().addLast(new ChannelInitializer<>() {

              @Override
              protected void initChannel(Channel channel) {

                // Wait for initialization
                synchronized (container) {
                  channel.eventLoop().submit(() -> initialized.accept(channel));
                }
              }
            });
          }
        });

        channelHandlerContext.fireChannelRead(o);
      }
    };

    channel.pipeline().addFirst(adapter);
    return adapter;
  }

  /**
   * Attach an initialization listener using {@link #attachInitializationListener}
   * to every currently available server channel
   * @param handlerName Handler name to register as within channel's pipelines
   * @param interceptor Consumer of internally created interception instances
   */
  private void attachInitializationListeners(String handlerName, Consumer<Interceptor> interceptor) {
    try {
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(getServer());
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);
      List<?> futures = (List<?>) F_SERVER_CONNECTION__CHANNEL_FUTURES.get(serverConnection);

      for (Object item : futures) {
        ChannelFuture future = (ChannelFuture) item;
        Channel channel = future.channel();

        ChannelInboundHandlerAdapter handler = attachInitializationListener(channel, futures, newChannel -> {
          interceptor.accept(this.attachInterceptor(newChannel, handlerName));
        });

        this.channelHandlers.put(channel, handler);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Detaches all initialization listeners which got previously attached
   * by using {@link #attachInitializationListeners}
   */
  private void detachInitializationListeners() {
    Iterator<Map.Entry<Channel, ChannelInboundHandlerAdapter>> it = this.channelHandlers.entrySet().iterator();

    while (it.hasNext()) {
      Map.Entry<Channel, ChannelInboundHandlerAdapter> entry = it.next();
      entry.getKey().pipeline().remove(entry.getValue());
      it.remove();
    }
  }

  /**
   * Calls {@link Interceptor#detach} on all interceptors previously added
   * by using {@link #attachInterceptor}
   */
  private void detachInterceptors() {
    Iterator<Interceptor> it = this.interceptors.iterator();

    while (it.hasNext()) {
      it.next().detach();
      it.remove();
    }
  }

  /**
   * Attaches an interceptor on a specific channel with the provided handler name
   * @param channel Channel to attach an interceptor to
   * @param handlerName Handler name to register as within channel's pipelines
   * @return The attached interceptor instance
   */
  private Interceptor attachInterceptor(Channel channel, String handlerName) {
    Interceptor interceptor = new Interceptor(channel, this::extractSenderNameFromPacket);

    interceptor.attach(handlerName);
    interceptors.add(interceptor);
    return interceptor;
  }

  /**
   * Get the channel of a player's network manager
   * @param p Target player to get the channel of
   * @return Channel on success, null on internal errors
   */
  private @Nullable Channel getPlayersChannel(Player p) {
    try {
      Object playerHandle = M_CRAFT_PLAYER__HANDLE.invoke(p);
      Object playerConnection = F_ENTITY_PLAYER__PLAYER_CONNECTION.get(playerHandle);
      Object networkManager = F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);
      return (Channel) F_NETWORK_MANAGER__CHANNEL.get(networkManager);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Set up interception to capture all currently online players as well
   * as all new incoming channel connections
   * @param handlerName Handler name to register as within channel's pipelines
   * @param interceptor Consumer of internally created interception instances
   */
  public void setupInterception(String handlerName, Consumer<Interceptor> interceptor) {
    attachInitializationListeners(handlerName, interceptor);

    // Attach on all currently online players for convenience while developing and reloading
    for (Player p : Bukkit.getOnlinePlayers()) {
      Channel c = getPlayersChannel(p);

      if (c != null)
        interceptor.accept(attachInterceptor(c, handlerName));
    }
  }

  /**
   * Clean up all interception modifications to get back into a vanilla state
   */
  public void cleanupInterception() {
    detachInitializationListeners();
    detachInterceptors();
  }
}
