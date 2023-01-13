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

package me.blvckbytes.bbreflect.packets;

import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import me.blvckbytes.bbreflect.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static org.bukkit.Bukkit.getServer;

public class InterceptorFactory implements IPacketOperator {

  private final String handlerName;

  private final ClassHandle C_PACKET_LOGIN;
  private final MethodHandle M_CRAFT_PLAYER__HANDLE;
  private final @Nullable FieldHandle F_PACKET_LOGIN__GAME_PROFILE;
  private @Nullable FieldHandle F_PACKET_LOGIN__NAME;

  private final FieldHandle F_CRAFT_SERVER__MINECRAFT_SERVER, F_MINECRAFT_SERVER__SERVER_CONNECTION,
    F_SERVER_CONNECTION__CHANNEL_FUTURES, F_ENTITY_PLAYER__PLAYER_CONNECTION, F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL;

  private final Map<Channel, ChannelInboundHandlerAdapter> channelHandlers;
  private final WeakHashMap<String, Interceptor> interceptorByPlayerName;
  private final List<Interceptor> interceptors;
  private final ReflectionHelper helper;

  public InterceptorFactory(ReflectionHelper helper, String handlerName) throws Exception {
    this.helper = helper;
    this.handlerName = handlerName;
    this.interceptors = new ArrayList<>();
    this.channelHandlers = new HashMap<>();
    this.interceptorByPlayerName = new WeakHashMap<>();

    ClassHandle C_CRAFT_PLAYER = helper.getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_ENTITY_PLAYER = helper.getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = helper.getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_NETWORK_MANAGER = helper.getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_CRAFT_SERVER = helper.getClass(RClass.CRAFT_SERVER);
    ClassHandle C_MINECRAFT_SERVER = helper.getClass(RClass.MINECRAFT_SERVER);
    ClassHandle C_SERVER_CONNECTION = helper.getClass(RClass.SERVER_CONNECTION);

    C_PACKET_LOGIN = helper.getClass(RClass.PACKET_I_LOGIN);

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
        ((Channel) o).pipeline().addFirst(new ChannelInitializer<Channel>() {

          @Override
          protected void initChannel(Channel channel) {

            // Add this initializer as the first item in the channel to be the
            // first receiver which gets a hold of it
            channel.pipeline().addFirst(new ChannelInitializer<Channel>() {

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
   * @param interceptor Consumer of internally created interception instances
   */
  private void attachInitializationListeners(Consumer<Interceptor> interceptor) {
    try {
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(getServer());
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);
      List<?> futures = (List<?>) F_SERVER_CONNECTION__CHANNEL_FUTURES.get(serverConnection);

      for (Object item : futures) {
        ChannelFuture future = (ChannelFuture) item;
        Channel channel = future.channel();

        ChannelInboundHandlerAdapter handler = attachInitializationListener(channel, futures, newChannel -> {
          interceptor.accept(this.attachInterceptor(newChannel, null));
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
      it.remove();

      Channel channel = entry.getKey();

      if (!channel.isOpen())
        continue;

      channel.pipeline().remove(entry.getValue());
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
   * @param playerName Name of the player, if it's already known at the time of instantiation
   * @return The attached interceptor instance
   */
  private Interceptor attachInterceptor(Channel channel, @Nullable String playerName) {
    Interceptor interceptor = new Interceptor(channel, playerName, this);

    interceptor.attach(handlerName);
    interceptors.add(interceptor);

    // Detach and remove when this channel has been closed
    channel.closeFuture().addListener(future -> {
      interceptor.detach();
      interceptors.remove(interceptor);
    });

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
   * @param interceptor Consumer of internally created interception instances
   */
  public void setupInterception(Consumer<Interceptor> interceptor) {
    attachInitializationListeners(interceptor);

    // Attach on all currently online players for convenience while developing and reloading
    for (Player p : Bukkit.getOnlinePlayers()) {
      Channel c = getPlayersChannel(p);

      if (c != null) {
        String playerName = p.getName();
        Interceptor inst = attachInterceptor(c, playerName);
        interceptorByPlayerName.put(playerName, inst);
        interceptor.accept(inst);
      }
    }
  }

  /**
   * Clean up all interception modifications to get back into a vanilla state
   */
  public void cleanupInterception() {
    detachInitializationListeners();
    detachInterceptors();
  }

  /**
   * Get a player's corresponding interceptor instance
   * @param p Target player
   * @return Interceptor reference on success, null if this player is not injected
   */
  public @Nullable Interceptor getPlayerInterceptor(Player p) {
    return interceptorByPlayerName.get(p.getName());
  }

  //=========================================================================//
  //                              IPacketOperator                            //
  //=========================================================================//

  @Override
  public @Nullable String tryExtractName(Interceptor requester, Object packet) throws Exception {
    if (!C_PACKET_LOGIN.isInstance(packet))
      return null;

    String name;

    if (F_PACKET_LOGIN__GAME_PROFILE != null)
      name = ((GameProfile) F_PACKET_LOGIN__GAME_PROFILE.get(packet)).getName();

    else {
      assert F_PACKET_LOGIN__NAME != null;
      name = (String) F_PACKET_LOGIN__NAME.get(packet);
    }

    if (name != null && !name.isEmpty())
      interceptorByPlayerName.put(name, requester);

    return name;
  }

  @Override
  public void sendPacket(Object packet, @Nullable Runnable completion, Object networkManager) throws Exception {
    helper.sendPacket(networkManager, packet, completion);
  }
}
