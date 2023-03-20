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
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.blvckbytes.bbreflect.handle.*;
import me.blvckbytes.bbreflect.handle.predicate.Assignability;
import me.blvckbytes.bbreflect.packets.*;
import me.blvckbytes.bbreflect.version.ServerVersion;
import me.blvckbytes.utilitytypes.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ReflectionHelper implements IReflectionHelper {

  public final ServerVersion version;

  private final FieldHandle
    F_CRAFT_PLAYER__HANDLE,
    F_ENTITY_PLAYER__CONNECTION,
    F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL;

  private final MethodHandle M_NETWORK_MANAGER__SEND, M_ENUM_PROTOCOL__GET_PACKET_ID;

  private final EnumHandle E_ENUM_PROTOCOL_DIRECTION, E_ENUM_PROTOCOL;

  private final WeakHashMap<Player, Tuple<Object, Channel>> networkManagerAndChannelCache;

  private final Map<Class<?>, Integer> packetIdByType;

  private InterceptorFactory interceptorFactory;

  private final Constructor<?> javaLangObjectConstructor;
  private final Tuple<Object, Method> serializationConstructorFactory;
  private final Map<Class<?>, Constructor<?>> emptyConstructorCache;
  private final Plugin plugin;
  private final Logger logger;

  public ReflectionHelper(Logger logger, Plugin plugin, ServerVersion version) throws Exception {
    this.version = version;
    this.plugin = plugin;
    this.logger = logger;
    this.networkManagerAndChannelCache = new WeakHashMap<>();
    this.emptyConstructorCache = new HashMap<>();
    this.packetIdByType = new HashMap<>();

    ClassHandle C_CRAFT_PLAYER = getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_ENTITY_PLAYER = getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_NETWORK_MANAGER = getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_PACKET = getClass(RClass.PACKET);

    ClassHandle C_ENUM_PROTOCOL = getClass(RClass.ENUM_PROTOCOL);
    ClassHandle C_ENUM_PROTOCOL_DIRECTION = getClass(RClass.ENUM_PROTOCOL_DIRECTION);

    M_ENUM_PROTOCOL__GET_PACKET_ID = C_ENUM_PROTOCOL.locateMethod()
      .withPublic(true)
      .withParameter(C_ENUM_PROTOCOL_DIRECTION, false, Assignability.TYPE_TO_TARGET)
      .withParameter(C_PACKET, false, Assignability.TYPE_TO_TARGET)
      .withReturnType(Integer.class)
      .required();

    E_ENUM_PROTOCOL_DIRECTION = C_ENUM_PROTOCOL_DIRECTION.asEnum();
    E_ENUM_PROTOCOL = C_ENUM_PROTOCOL.asEnum();

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

    ClassHandle C_PACKET_SEND_LISTENER = getClassOptional(RClass.PACKET_SEND_LISTENER);
    MethodHandle C_PACKET_SEND_LISTENER__FROM_RUNNABLE = C_PACKET_SEND_LISTENER == null ? null : C_PACKET_SEND_LISTENER.locateMethod()
      .withPublic(true)
      .withStatic(true)
      .withParameters(Runnable.class)
      .withReturnType(C_PACKET_SEND_LISTENER)
      .optional();

    /*
      method(
        [0] Packet packet
        [1] Runnable complete
      )
     */
    M_NETWORK_MANAGER__SEND = C_NETWORK_MANAGER.locateMethod()
      .withVersionRange(ServerVersion.V1_19_R1, null)
      .withPublic(true)
      .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
      .withParameter(C_PACKET_SEND_LISTENER, false, Assignability.TARGET_TO_TYPE)
      .withCallTransformer(args -> {
        assert C_PACKET_SEND_LISTENER__FROM_RUNNABLE != null;
        return new Object[] {
          args[0], C_PACKET_SEND_LISTENER__FROM_RUNNABLE.invoke(null, args[1])
        };
      }, C_PACKET_SEND_LISTENER__FROM_RUNNABLE)
      .orElse(() -> (
        C_NETWORK_MANAGER.locateMethod()
          .withVersionRange(null, ServerVersion.V1_19_R0)
          .withCallTransformer(args -> new Object[] { args[0], makeFutureListener((Runnable) args[1]) })
          .withPublic(true)
          .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
          .withParameter(GenericFutureListener.class)
        ))
      .orElse(() -> (
        C_NETWORK_MANAGER.locateMethod()
          .withVersionRange(null, ServerVersion.V1_12_R2)
          .withCallTransformer(args -> new Object[] { args[0], makeFutureListener((Runnable) args[1]), new GenericFutureListener[0] })
          .withPublic(true)
          .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
          .withParameter(GenericFutureListener.class)
          .withParameter(GenericFutureListener[].class)
      ))
      .orElse(() -> (
        C_NETWORK_MANAGER.locateMethod()
          .withVersionRange(null, ServerVersion.V1_7_R10)
          .withCallTransformer(args -> new Object[] { args[0], new GenericFutureListener[] { makeFutureListener((Runnable) args[1]) } })
          .withPublic(true)
          .withParameter(C_PACKET, false, Assignability.TARGET_TO_TYPE)
          .withParameter(GenericFutureListener[].class)
      ))
      .required();

    F_NETWORK_MANAGER__CHANNEL = C_NETWORK_MANAGER.locateField()
      .withType(Channel.class)
      .required();

    this.javaLangObjectConstructor = Object.class.getConstructor();
    this.serializationConstructorFactory = getSerializationConstructorFactory();
  }

  private @Nullable GenericFutureListener<?> makeFutureListener(@Nullable Runnable runnable) {
    if (runnable == null)
      return null;
    return future -> runnable.run();
  }

  private Tuple<Object, Channel> findNetworkManagerAndChannel(Player player) throws Exception {
    Object entityPlayer = F_CRAFT_PLAYER__HANDLE.get(player);
    Object playerConnection = F_ENTITY_PLAYER__CONNECTION.get(entityPlayer);
    Object networkManager = F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);
    Object channel = F_NETWORK_MANAGER__CHANNEL.get(networkManager);
    return new Tuple<>(networkManager, (Channel) channel);
  }

  @Override
  public void setupInterception(String handlerName, IInterceptorFeatureProvider featureProvider, Consumer<IInterceptor> interceptor) throws Exception {
    if (this.interceptorFactory != null)
      throw new IllegalStateException("The interceptor factory has already been set up");

    this.interceptorFactory = new InterceptorFactory(featureProvider.getInterceptorFeatures(), logger, this, handlerName);
    this.interceptorFactory.setupInterception(interceptor::accept);
    Bukkit.getPluginManager().registerEvents(this.interceptorFactory, plugin);
  }

  @Override
  public void cleanupInterception() {
    if (interceptorFactory == null)
      return;

    this.interceptorFactory.cleanupInterception();
    HandlerList.unregisterAll(this.interceptorFactory);
    this.interceptorFactory = null;
  }

  @Override
  public @Nullable IInterceptor getInterceptorFor(Player p) {
    if (this.interceptorFactory == null)
      return null;
    return this.interceptorFactory.getPlayerInterceptor(p);
  }

  public void sendPacket(Object networkManager, Object packet, @Nullable Runnable completion) throws Exception {
    if (completion == null)
      completion = () -> {};
    M_NETWORK_MANAGER__SEND.invoke(networkManager, packet, completion);
  }

  @Override
  public void sendPacket(Player player, Object packet, @Nullable Runnable completion) throws Exception {
    Tuple<Object, Channel> networkManagerAndChannel = networkManagerAndChannelCache.get(player);

    if (networkManagerAndChannel == null) {
      networkManagerAndChannel = findNetworkManagerAndChannel(player);
      networkManagerAndChannelCache.put(player, networkManagerAndChannel);
    }

    if (!networkManagerAndChannel.b.isOpen())
      return;

    if (packet instanceof RawPacket) {
      Channel channel = networkManagerAndChannel.b;
      ChannelFuture future = channel.writeAndFlush(packet);

      if (completion != null)
        future.addListener((GenericFutureListener<? extends Future<? super Void>>) makeFutureListener(completion));

      return;
    }

    sendPacket(networkManagerAndChannel.a, packet, completion);
  }

  @Override
  public ClassHandle getClass(RClass rc) throws ClassNotFoundException {
    return rc.resolve(this.version);
  }

  @Override
  public @Nullable ClassHandle getClassOptional(RClass rc) {
    try {
      return getClass(rc);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public @Nullable EnumHandle getEnumOptional(RClass rc) {
    try {
      return rc.resolve(version).asEnum();
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public Object instantiateUnsafely(Class<?> type) throws Exception {
    Constructor<?> constructor = emptyConstructorCache.get(type);

    if (constructor == null) {
      constructor = newConstructorForSerialization(type);
      emptyConstructorCache.put(type, constructor);
    }

    return constructor.newInstance((Object[]) null);
  }

  private int tryLocatePacketId(Class<?> type) throws Exception {
    Object dummyInstance = instantiateUnsafely(type);

    for (EProtocol protocol : EProtocol.values) {
      for (EProtocolDirection direction : protocol.availableDirections) {
        Object nmsProtocol = E_ENUM_PROTOCOL.getByCopy(protocol);
        Object nmsDirection = E_ENUM_PROTOCOL_DIRECTION.getByCopy(direction);

        Integer id = (Integer) M_ENUM_PROTOCOL__GET_PACKET_ID.invoke(nmsProtocol, nmsDirection, dummyInstance);

        if (id != null)
          return id;
      }
    }

    throw new IllegalStateException("No protocol/direction combination yielded a packet ID result for " + type);
  }

  @Override
  public int getPacketId(Class<?> type) throws Exception {
    Integer packetId = packetIdByType.get(type);

    if (packetId != null)
      return packetId;

    packetId = tryLocatePacketId(type);
    packetIdByType.put(type, packetId);
    return packetId;
  }

  public ServerVersion getVersion() {
    return this.version;
  }

  private Tuple<Object, Method> getSerializationConstructorFactory() throws Exception {
    Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
    Object factory = reflectionFactoryClass.getDeclaredMethod("getReflectionFactory").invoke(null);
    Method method = reflectionFactoryClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class);
    return new Tuple<>(factory, method);
  }

  private Constructor<?> newConstructorForSerialization(Class<?> type) throws Exception {
    Constructor<?> constructor = (Constructor<?>) serializationConstructorFactory.b.invoke(serializationConstructorFactory.a, type, javaLangObjectConstructor);
    constructor.setAccessible(true);
    return constructor;
  }
}
