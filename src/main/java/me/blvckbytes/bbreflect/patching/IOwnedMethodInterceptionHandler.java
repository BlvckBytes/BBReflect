package me.blvckbytes.bbreflect.patching;

import me.blvckbytes.bbreflect.packets.IPacketOwner;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IOwnedMethodInterceptionHandler {

  String handleStringifiedComponent(IPacketOwner owner, String input);

  void handleNBTTagCompound(IPacketOwner owner, Object input, Consumer<@Nullable Runnable> serializeAndRestore);

}
