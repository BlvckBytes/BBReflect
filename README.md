<!-- This file is rendered by https://github.com/BlvckBytes/readme_helper -->

🧨 WARNING: The API documentation is out of sync, use at your own risk!

# BBReflect

Reflection utility library used for finding handle dependencies by describing them through the
use of builders in a fuzzy manner. Also provides various packet I/O utilities.

## Table of Contents
- [Handles](#handles)
  - [RClass](#rclass)
  - [ClassHandle](#classhandle)
  - [EnumHandle](#enumhandle)
  - [MethodHandle](#methodhandle)
  - [FieldHandle](#fieldhandle)
- [Intercepting Packets](#intercepting-packets)
  - [Setting Up The Factory](#setting-up-the-factory)
  - [Using the Interceptor](#using-the-interceptor)

## Handles

Handles are not only a wrapper on top of reflection access, but also provide a way to query handles
which can contain children members by bootstrapping a new predicate builder. A predicate builder
builds a member predicate step by step, which will decide which member matches the description at
runtime. It is strongly advised that all used handles are required in the constructor of the class
using them, so the burden of executing predicates happens only once on initialization.

### RClass

The `RClass` enum provides a way to resolve classes known by an internal name at runtime into a `ClassHandle`
through the use of the `ReflectionHelper`.

### ClassHandle

A class handle represents a `Class<?>` and is the main entry-point when querying for members.

<details>
<summary>ClassHandle.java</summary>

```java
package me.blvckbytes.bbreflect;

@SuppressWarnings("rawtypes")
public class ClassHandle extends AHandle<Class> {

  // Caching manual encapsulations using the of() constructor here
  private static final Map<Class<?>, ClassHandle> encapsulations;

  // Caching enumeration constants
  private static final Map<Class<?>, EnumHandle> enumerations;

  static {
    encapsulations = new HashMap<>();
    enumerations = new HashMap<>();
  }

  public ClassHandle(Class<?> target, ServerVersion version, FMemberPredicate<Class> predicate) throws NoSuchElementException {
    super(target, Class.class, version, predicate);
  }

  protected ClassHandle(Class handle, ServerVersion version) {
    super(handle, Class.class, version);
  }

  /**
   * Checks whether an object is an instance of this class
   * @param o Object to check
   */
  public boolean isInstance(Object o) {
    return handle.isInstance(o);
  }

  /**
   * Interpret this class as an enumeration and get a handle to it
   * @throws IllegalStateException Thrown if this class is not an enumeration
   */
  public EnumHandle asEnum() throws IllegalStateException {
    EnumHandle enumHandle = enumerations.get(handle);

    // Use cached value
    if (enumHandle != null)
      return enumHandle;

    // Create a new enum handle on this class
    enumHandle = new EnumHandle(handle, version);

    // Store in cache and return
    enumerations.put(handle, enumHandle);
    return enumHandle;
  }

  /**
   * Create a new FieldHandle builder which will query this class
   */
  public FieldPredicateBuilder locateField() {
    return new FieldPredicateBuilder(this, version);
  }

  /**
   * Create a new MethodHandle builder which will query this class
   */
  public MethodPredicateBuilder locateMethod() {
    return new MethodPredicateBuilder(this, version);
  }

  /**
   * Create a new ConstructorHandle builder which will query this class
   */
  public ConstructorPredicateBuilder locateConstructor() {
    return new ConstructorPredicateBuilder(this, version);
  }

  /**
   * Create a new ClassHandle builder which will query this class
   */
  public ClassPredicateBuilder locateClass() {
    return new ClassPredicateBuilder(this, version);
  }

  @Override
  protected String stringify(Class member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.isInterface() ? "interface" : "class");
    sj.add(member.getName());

    return sj.toString();
  }

  /**
   * Create a new class handle on top of a vanilla class
   * @param c Target class
   * @param version Current server version
   */
  public static ClassHandle of(Class<?> c, ServerVersion version) {
    ClassHandle handle = encapsulations.get(c);

    // Create new instance
    if (handle == null) {
      handle = new ClassHandle(c, version);
      encapsulations.put(c, handle);
      return handle;
    }

    // Return existing instance
    return handle;
  }
}
```
</details>


### EnumHandle

An enum handle is in essence representing a class too, which is why it's a derivation of the `ClassHandle`. It
aims to offer access to enumeration constants.

<details>
<summary>EnumHandle.java</summary>

```java
package me.blvckbytes.bbreflect;

public class EnumHandle extends ClassHandle {

  // TODO: Check against enum copies if they match the number of entries, else throw
  // TODO: Get an "enum" from static constants of a specific type within a class

  private final List<Enum<?>> e;

  /**
   * Create a new enumeration handle on top of a enumeration class
   * @param c Class which represents an enumeration
   * @param version Current server version
   * @throws IllegalStateException Thrown if the provided class is not an enumeration
   */
  public EnumHandle(Class<?> c, ServerVersion version) throws IllegalStateException {
    super(c, version);

    Object[] constants = c.getEnumConstants();

    // The provided class hasn't been of an enumeration type
    if (constants == null)
      throw new IllegalStateException("This class does not represent an enumeration.");

    // Create a unmodifiable list of constants and wrap into a handle
    e = Arrays.asList((Enum<?>[]) constants);
  }

  /**
   * Get an enumeration constant by it's ordinal integer
   * @param ordinal Ordinal integer
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  @SuppressWarnings("unchecked")
  public Enum<?> getByOrdinal(int ordinal) throws EnumConstantNotPresentException {
    try {
      return e.get(ordinal);
    } catch (Exception e) {
      throw new EnumConstantNotPresentException((Class<? extends Enum<?>>) handle, "ordinal=" + ordinal);
    }
  }

  /**
   * Get an enumeration constant by looking up the ordinal of a
   * copy enum which has it's constants sorted in the exact same order.
   * @param other Constant of a copy
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  public Enum<?> getByCopy(Enum<?> other) throws EnumConstantNotPresentException {
    return getByOrdinal(other.ordinal());
  }
}
```
</details>


### MethodHandle

A method handle represents a `Method` and is used to perform method invocations.

<details>
<summary>MethodHandle.java</summary>

```java
package me.blvckbytes.bbreflect;

public class MethodHandle extends AHandle<Method> {

  private final @Nullable FCallTransformer callTransformer;

  protected MethodHandle(Class<?> target, ServerVersion version, @Nullable FCallTransformer callTransformer, FMemberPredicate<Method> predicate) throws NoSuchElementException {
    super(target, Method.class, version, predicate);
    this.callTransformer = callTransformer;
  }

  /**
   * Invoke this method on an object instance
   * @param o Target object to invoke on
   * @param args Arguments to pass when invoking the method
   * @return Method return value
   */
  public Object invoke(Object o, Object... args) throws Exception {
    if (callTransformer != null)
      args = callTransformer.apply(args);
    return handle.invoke(o, args);
  }

  @Override
  protected String stringify(Method member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.getReturnType().getName());
    sj.add(member.getName());
    sj.add("(");

    StringJoiner argJoiner = new StringJoiner(", ");
    for (Class<?> parameter : member.getParameterTypes())
      argJoiner.add(parameter.getName());

    sj.add(argJoiner.toString());
    sj.add(")");

    return sj.toString();
  }
}
```
</details>


### FieldHandle

A field handle represents a `Field` and is used to perform set- and get operations.

<details>
<summary>FieldHandle.java</summary>

```java
package me.blvckbytes.bbreflect;

public class FieldHandle extends AHandle<Field> {

  public FieldHandle(Class<?> target, ServerVersion version, FMemberPredicate<Field> predicate) throws NoSuchElementException {
    super(target, Field.class, version, predicate);
  }

  /**
   * Set the field's value on an object instance
   * @param o Target object to modify
   * @param v Field value to set
   */
  public void set(Object o, Object v) throws IllegalAccessException {
    this.handle.set(o, v);
  }

  /**
   * Get the field's value from an object instance
   * @param o Target object to read from
   * @return Field value
   */
  public Object get(Object o) throws IllegalAccessException {
    return this.handle.get(o);
  }

  @Override
  protected String stringify(Field member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.getType().getName());
    sj.add(member.getName());

    return sj.toString();
  }
}
```
</details>


## Intercepting Packets

All packets which travel through socket connections between the server and it's clients are intercepted
if the interceptor factory is set up properly, which allows for custom packet patches as well
as packet injections.

### Setting Up The Factory

The `InterceptorFactory` creates `Interceptor` instances, where each instance handles one socket connection.

<details>
<summary>InterceptorFactory.java</summary>

```java
package me.blvckbytes.bbreflect.packets;

public class InterceptorFactory implements IPacketOperator {

  private final String handlerName;

  private final ClassHandle C_PACKET_LOGIN;
  private final MethodHandle M_CRAFT_PLAYER__HANDLE, M_NETWORK_MANAGER__SEND_PACKET;
  private final @Nullable FieldHandle F_PACKET_LOGIN__GAME_PROFILE;
  private @Nullable FieldHandle F_PACKET_LOGIN__NAME;

  private final FieldHandle F_CRAFT_SERVER__MINECRAFT_SERVER, F_MINECRAFT_SERVER__SERVER_CONNECTION,
    F_SERVER_CONNECTION__CHANNEL_FUTURES, F_ENTITY_PLAYER__PLAYER_CONNECTION, F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL;

  private final Map<Channel, ChannelInboundHandlerAdapter> channelHandlers;
  private final WeakHashMap<String, Interceptor> interceptorByPlayerName;
  private final List<Interceptor> interceptors;

  public InterceptorFactory(ReflectionHelper helper, String handlerName) throws Exception {
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
    ClassHandle C_PACKET = helper.getClass(RClass.PACKET);

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

    // public NetworkManager#sendPacket(Packet<?>, GenericFutureListener<?>)
    M_NETWORK_MANAGER__SEND_PACKET = C_NETWORK_MANAGER.locateMethod()
      .withPublic(true)
      .withParameter(C_PACKET)
      .withParameter(GenericFutureListener.class)
      .required();
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
    GenericFutureListener<? extends Future<? super Void>> listener = null;

    if (completion != null)
      listener = f -> completion.run();

    M_NETWORK_MANAGER__SEND_PACKET.invoke(networkManager, packet, listener);
  }
}
```
</details>


The factory also provides a way to get an interceptor by a player reference. This correspondence lookup is only
available as soon as the login process is completed and the socket's player-name is known.

The following example sets up the factory in the advised manner in order to access full control over all packets:

```java
public class Playground extends JavaPlugin {

  private static final String HANDLER_NAME = "playground_injector";
  private final InterceptorFactory interceptorFactory;

  // This reflection helper should only exist once per plugin.
  // If it fails to find it's dependency handles internally, the
  // plugin will be disabled due to the thrown exception
  public Playground() throws Exception {
    ReflectionHelper reflectionHelper = new ReflectionHelper(null);
    this.interceptorFactory = new InterceptorFactory(reflectionHelper, HANDLER_NAME);
  }

  @Override
  public void onEnable() {
    this.interceptorFactory.setupInterception(this::onInterceptorCreation);
  }

  @Override
  public void onDisable() {
    // This step is crucial to leave a clean vanilla-state after unloading this plugin
    this.interceptorFactory.cleanupInterception();
  }

  // Called whenever a new interceptor is instantiated
  private void onInterceptorCreation(Interceptor interceptor) {
    interceptor.setInboundInterceptor((playerName, packet, channel) -> {
      Bukkit.getConsoleSender().sendMessage("§dInbound: " + packet.getClass().getSimpleName());
      return packet;
    });

    interceptor.setOutboundInterceptor((playerName, packet, channel) -> {
      Bukkit.getConsoleSender().sendMessage("§dOutbound: " + packet.getClass().getSimpleName());
      return packet;
    });
  }
}
```

### Using the Interceptor

To use the interceptor for interception of existing packets, use the in-/outbound interceptor setters.
In order to send custom packets, call the `sendPacket` method on the interceptor instance.

<details>
<summary>Interceptor.java</summary>

```java
package me.blvckbytes.bbreflect.packets;

public class Interceptor extends ChannelDuplexHandler {

  // Don't keep closed channels from being garbage-collected
  private final WeakReference<Channel> channel;
  private final IPacketOperator operator;

  private @Nullable String handlerName;
  private @Nullable Object networkManager;

  private volatile @Nullable String playerName;

  @Setter
  private FPacketInterceptor inboundInterceptor, outboundInterceptor;

  /**
   * Create a new packet interceptor on top of a network channel
   * @param channel Underlying network channel to intercept data on
   * @param playerName Name of the player, if it's already known at the time of instantiation
   * @param operator External packet operator which does all reflective access
   */
  public Interceptor(Channel channel, @Nullable String playerName, IPacketOperator operator) {
    this.playerName = playerName;
    this.channel = new WeakReference<>(channel);
    this.operator = operator;
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    Channel ch = channel.get();

    // Try to extract the name and update the local reference, if applicable
    try {
      String extractedName = operator.tryExtractName(this, o);
      if (extractedName != null)
        playerName = extractedName;
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Call the inbound interceptor, if applicable
    if (inboundInterceptor != null && ch != null) {
      try {
        o = inboundInterceptor.intercept(playerName, o, ch);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Dropped the packet
      if (o == null)
        return;
    }

    super.channelRead(channelHandlerContext, o);
  }

  @Override
  public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
    Channel ch = channel.get();

    // Call the outbound interceptor, if applicable
    if (outboundInterceptor != null && ch != null) {
      try {
        o = outboundInterceptor.intercept(playerName, o, ch);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Dropped the packet
      if (o == null)
        return;
    }

    super.write(channelHandlerContext, o, channelPromise);
  }

  /**
   * Calls the consumer with the pipe of the underlying channel if
   * the channel is present and hasn't yet been garbage-collected
   * @param action Consumer of the pipe
   */
  private void ifPipePresent(Consumer<ChannelPipeline> action) {
    Channel ch = channel.get();

    if (ch == null)
      return;

    action.accept(ch.pipeline());
  }

  /**
   * Attaches this interceptor to it's underlying channel
   * @param name Name to attach as within the pipeline
   */
  public void attach(String name) {
    if (this.handlerName != null)
      throw new IllegalStateException("Tried to attach twice");

    ifPipePresent(pipe -> {
      // The network manager instance is also registered within the
      // pipe, get it by it's name to have a reference available
      networkManager = pipe.get("packet_handler");

      // Register before the packet handler to have an interception capability
      pipe.addBefore("packet_handler", name, this);
      this.handlerName = name;
    });
  }

  /**
   * Detaches this interceptor from it's underlying channel
   */
  public void detach() {
    if (this.handlerName == null)
      return;

    ifPipePresent(pipe -> {
      List<String> names = pipe.names();

      if (!names.contains(handlerName))
        return;

      pipe.remove(handlerName);
    });

    this.networkManager = null;
    this.handlerName = null;
  }

  /**
   * Used to send a packet using a network manager instance
   * @param packet Packet instance to send
   * @param completion Optional completion callback, nullable
   */
  public void sendPacket(Object packet, @Nullable Runnable completion) throws Exception {
    if (networkManager == null)
      throw new IllegalStateException("Could not find the network manager");

    this.operator.sendPacket(packet, completion, networkManager);
  }
}
```
</details>


