<!-- This file is rendered by https://github.com/BlvckBytes/readme_helper -->

# BBReflect

Reflection utility library used for finding handle dependencies by describing them through the
use of builders in a fuzzy manner. Also provides various packet I/O utilities.

<!-- #toc -->

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

<!-- #include src/main/java/me/blvckbytes/bbreflect/ClassHandle.java -->

### EnumHandle

An enum handle is in essence representing a class too, which is why it's a derivation of the `ClassHandle`. It
aims to offer access to enumeration constants.

<!-- #include src/main/java/me/blvckbytes/bbreflect/EnumHandle.java -->

### MethodHandle

A method handle represents a `Method` and is used to perform method invocations.

<!-- #include src/main/java/me/blvckbytes/bbreflect/MethodHandle.java -->

### FieldHandle

A field handle represents a `Field` and is used to perform set- and get operations.

<!-- #include src/main/java/me/blvckbytes/bbreflect/FieldHandle.java -->

## Intercepting Packets

All packets which travel through socket connections between the server and it's clients are intercepted
if the interceptor factory is set up properly, which allows for custom packet patches as well
as packet injections.

### Setting Up The Factory

The `InterceptorFactory` creates `Interceptor` instances, where each instance handles one socket connection.

<!-- #include src/main/java/me/blvckbytes/bbreflect/packets/InterceptorFactory.java -->

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

<!-- #include src/main/java/me/blvckbytes/bbreflect/packets/Interceptor.java -->

<!-- #configure include SKIP_LEADING_COMMENTS true -->
<!-- #configure include SKIP_LEADING_EMPTY true -->
<!-- #configure include SKIP_LEADING_PACKAGE false -->
<!-- #configure include SKIP_LEADING_IMPORTS true -->
<!-- #configure include WRAP_IN_COLLAPSIBLE true -->
