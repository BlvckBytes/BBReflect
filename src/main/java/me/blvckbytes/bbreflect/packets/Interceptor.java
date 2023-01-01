package me.blvckbytes.bbreflect.packets;

import io.netty.channel.*;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

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
   * @param operator External packet operator which does all reflective access
   */
  public Interceptor(Channel channel, IPacketOperator operator) {
    this.channel = new WeakReference<>(channel);
    this.operator = operator;
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    Channel ch = channel.get();

    // Try to extract the name and update the local reference, if applicable
    try {
      String extractedName = operator.tryExtractName(o);
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