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

import io.netty.channel.*;
import io.netty.util.AttributeKey;
import me.blvckbytes.bbreflect.patching.IMethodInterceptionHandler;
import me.blvckbytes.bbreflect.patching.IOwnedMethodInterceptionHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Interceptor extends ChannelDuplexHandler implements IInterceptor {

  private static final String
    PIPE_PACKET_HANDLER_NAME     = "_packet_handler",
    PIPE_RAW_PACKET_ENCODER_NAME = "_raw_packet_encoder",
    PIPE_BINARY_DECODER_NAME     = "_binary_decoder",
    PIPE_BINARY_ENCODER_NAME     = "_binary_encoder";

  private static final String[] AVAILABLE_PIPE_NAMES = {
    PIPE_PACKET_HANDLER_NAME, PIPE_RAW_PACKET_ENCODER_NAME, PIPE_BINARY_DECODER_NAME, PIPE_BINARY_ENCODER_NAME
  };

  public static final AttributeKey<Supplier<IMethodInterceptionHandler>> HANDLER_KEY;

  static {
    HANDLER_KEY = AttributeKey.valueOf("method_interception_handler");
  }

  // Don't keep closed channels from being garbage-collected
  private final WeakReference<Channel> channel;
  private final IPacketOperator operator;

  private @Nullable String handlerName;
  private @Nullable Object networkManager;

  private volatile @Nullable String playerName;
  private volatile int version;
  private @Nullable Player playerReference;

  private final IPacketOwner packetOwner;

  private FPacketInterceptor inboundPacketInterceptor, outboundPacketInterceptor;

  private FBytesInterceptor inboundBytesInterceptor, outboundBytesInterceptor;

  private IOwnedMethodInterceptionHandler methodInterceptionHandler;

  /**
   * Create a new packet interceptor on top of a network channel
   * @param channel Underlying network channel to intercept data on
   * @param player Involved player, if already known at the time of instantiation
   * @param operator External packet operator which does all reflective access
   */
  public Interceptor(Channel channel, @Nullable Player player, IPacketOperator operator) {
    this.playerReference = player;
    this.channel = new WeakReference<>(channel);
    this.operator = operator;

    this.packetOwner = new IPacketOwner() {

      @Override
      public @Nullable String getName() {
        return playerName;
      }

      @Override
      public @Nullable Player getPlayer() {
        return playerReference;
      }

      @Override
      public int getVersion() {
        return version;
      }
    };
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

    // Try to extract the version and update the local reference, if applicable
    try {
      int extractedVersion = operator.tryExtractVersion(this, o);
      if (extractedVersion > 0)
        this.version = extractedVersion;
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Call the inbound interceptor, if applicable
    if (inboundPacketInterceptor != null && ch != null) {
      try {
        o = inboundPacketInterceptor.intercept(packetOwner, o, ch);
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
    if (outboundPacketInterceptor != null && ch != null) {
      try {
        o = outboundPacketInterceptor.intercept(packetOwner, o, ch);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Dropped the packet
      if (o == null)
        return;
    }

    super.write(channelHandlerContext, o, channelPromise);
  }

  public void setPlayerReference(@Nullable Player player) {
    this.playerReference = player;
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

  private IMethodInterceptionHandler getInterceptionHandler() {
    return new IMethodInterceptionHandler() {

      @Override
      public String handleStringifiedComponent(String input) {
        if (methodInterceptionHandler == null)
          return input;

        return methodInterceptionHandler.handleStringifiedComponent(packetOwner, input);
      }

      @Override
      public void handleNBTTagCompound(Object input, Consumer<@Nullable Runnable> serializeAndRestore) {
        if (methodInterceptionHandler == null) {
          serializeAndRestore.accept(null);
          return;
        }

        methodInterceptionHandler.handleNBTTagCompound(packetOwner, input, serializeAndRestore);
      }
    };
  }

  /**
   * Attaches this interceptor to it's underlying channel
   * @param name Name to attach as within the pipeline
   */
  public void attach(String name) {
    if (this.handlerName != null)
      throw new IllegalStateException("Tried to attach twice");

    ifPipePresent(pipe -> {

      this.handlerName = name;

      EnumSet<EInterceptorFeature> features = operator.getFeatures();

      if (features.contains(EInterceptorFeature.METHOD_INTERCEPTION)) {
        if (pipe.channel().hasAttr(HANDLER_KEY))
          throw new IllegalStateException("There was already another interception handler attached");

        IMethodInterceptionHandler interceptionHandler = getInterceptionHandler();
        pipe.channel().attr(HANDLER_KEY).set(() -> {
          if (this.methodInterceptionHandler == null)
            return null;
          return interceptionHandler;
        });

        exchangeHandler(pipe, "encoder", operator::createModified);
      }

      // The network manager instance is also registered within the
      // pipe, get it by it's name to have a reference available
      networkManager = pipe.get("packet_handler");

      if (features.contains(EInterceptorFeature.PACKET_INTERCEPTION)) {
        // Register before the packet handler to have an interception capability
        pipe.addBefore("packet_handler", this.handlerName + PIPE_PACKET_HANDLER_NAME, this);
      }

      if (features.contains(EInterceptorFeature.BYTES_INTERCEPTION)) {
        // Register the custom binary decoder before the actual decoder to have an interception capability
        pipe.addBefore("decoder", this.handlerName + PIPE_BINARY_DECODER_NAME, new BinaryPacketReadHandler(message -> {
          Channel channelInstance = channel.get();

          if (inboundBytesInterceptor != null && channelInstance != null) {
            try {
              IBinaryBuffer result = inboundBytesInterceptor.intercept(packetOwner, new ByteBufBuffer(message), channelInstance);

              if (result == null)
                return null;

              return result.asByteBuf();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          return message;
        }));

        // Register the custom binary encoder before the actual encoder to see it's results
        pipe.addBefore("encoder", this.handlerName + PIPE_BINARY_ENCODER_NAME, new BinaryPacketWriteHandler(message -> {
          Channel channelInstance = channel.get();

          if (outboundBytesInterceptor != null && channelInstance != null) {
            try {
              IBinaryBuffer result = outboundBytesInterceptor.intercept(packetOwner, new ByteBufBuffer(message), channelInstance);

              if (result == null)
                return null;

              return result.asByteBuf();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          return message;
        }));
      }

      if (features.contains(EInterceptorFeature.RAW_PACKET_ENCODER)) {
        pipe.addAfter("encoder", this.handlerName + PIPE_RAW_PACKET_ENCODER_NAME, new RawPacketEncoder());
      }
    });
  }

  private void exchangeHandler(ChannelPipeline pipe, String name, Function<Object, Object> newEntry) {
    // Get the next name to add before in order to end up at the same location
    int encoderIndex = pipe.names().indexOf(name);
    String nextName = pipe.names().get(encoderIndex + 1);

    Object previousEntry = pipe.remove(name);
    pipe.addBefore(nextName, name, (ChannelHandler) newEntry.apply(previousEntry));
  }

  /**
   * Detaches this interceptor from it's underlying channel
   */
  public void detach() {
    // Already detached, noop
    if (this.handlerName == null)
      return;

    ifPipePresent(pipe -> {
      EnumSet<EInterceptorFeature> features = operator.getFeatures();

      List<String> names = pipe.names();

      for (String pipeName : AVAILABLE_PIPE_NAMES) {
        String registeredName = this.handlerName + pipeName;
        if (names.contains(registeredName))
          pipe.remove(registeredName);
      }

      if (features.contains(EInterceptorFeature.METHOD_INTERCEPTION)) {
        // Create new vanilla instance, because is not a @Sharable handler, so can't be added or removed multiple times
        exchangeHandler(pipe, "encoder", operator::createVanilla);
        pipe.channel().attr(HANDLER_KEY).remove();
      }
    });

    this.networkManager = null;
    this.handlerName = null;
  }

  @Override
  public void sendPacket(Object packet, @Nullable Runnable completion) throws Exception {
    if (networkManager == null)
      throw new IllegalStateException("Could not find the network manager");

    this.operator.sendPacket(packet, completion, networkManager);
  }

  @Override
  public void setInboundPacketInterceptor(FPacketInterceptor interceptor) {
    this.inboundPacketInterceptor = interceptor;
  }

  @Override
  public void setOutboundPacketInterceptor(FPacketInterceptor interceptor) {
    this.outboundPacketInterceptor = interceptor;
  }

  @Override
  public void setInboundBytesInterceptor(FBytesInterceptor interceptor) {
    this.inboundBytesInterceptor = interceptor;
  }

  @Override
  public void setOutboundBytesInterceptor(FBytesInterceptor interceptor) {
    this.outboundBytesInterceptor = interceptor;
  }

  @Override
  public void setMethodInterceptionHandler(IOwnedMethodInterceptionHandler handler) {
    this.methodInterceptionHandler = handler;
  }
}
