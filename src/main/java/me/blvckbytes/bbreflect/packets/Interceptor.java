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

  private final FPacketPlayerNameExtractor nameExtractor;
  private @Nullable String name;
  private volatile @Nullable String senderName;

  @Setter
  private FPacketInterceptor inboundInterceptor, outboundInterceptor;

  /**
   * Create a new packet interceptor on top of a network channel
   * @param channel Underlying network channel to intercept data on
   * @param nameExtractor Name extractor function
   */
  public Interceptor(Channel channel, FPacketPlayerNameExtractor nameExtractor) {
    this.channel = new WeakReference<>(channel);
    this.nameExtractor = nameExtractor;
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    Channel ch = channel.get();

    // Try to extract the name and update the local reference, if applicable
    try {
      String extractedName = nameExtractor.extract(o);
      if (extractedName != null)
        senderName = extractedName;
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Call the inbound interceptor, if applicable
    if (inboundInterceptor != null && ch != null) {
      try {
        o = inboundInterceptor.intercept(senderName, o, ch);
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
        o = outboundInterceptor.intercept(senderName, o, ch);
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
    if (this.name != null)
      throw new IllegalStateException("Tried to attach twice");

    ifPipePresent(pipe -> {
      List<String> names = pipe.names();

      if (names.contains("packet_handler"))
        pipe.addBefore("packet_handler", name, this);
      else
        pipe.addLast(name, this);

      this.name = name;
    });
  }

  /**
   * Detaches this interceptor from it's underlying channel
   */
  public void detach() {
    if (this.name == null)
      return;

    ifPipePresent(pipe -> {
      List<String> names = pipe.names();

      if (!names.contains(name))
        return;

      pipe.remove(name);
      this.name = null;
    });
  }
}
