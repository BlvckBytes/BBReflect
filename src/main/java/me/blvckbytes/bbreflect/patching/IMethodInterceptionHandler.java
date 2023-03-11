package me.blvckbytes.bbreflect.patching;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IMethodInterceptionHandler {

  String handleStringifiedComponent(String input);

  void handleNBTTagCompound(Object input, Consumer<@Nullable Runnable> serializeAndRestore);

}
