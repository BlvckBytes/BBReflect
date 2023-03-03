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

import me.blvckbytes.bbreflect.patching.UtfPatcherClassLoader;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ReflectionHelperFactory {

  // NOTE: These "hardcoded" paths will be also affected by the search/replace engine of shading

  private static final String REFLECTION_HELPER_FQN = relocatableFQN("me/blvckbytes/bbreflect/ReflectionHelper");
  private static final List<String> AFFECTED_FQN_LIST;

  static {
    AFFECTED_FQN_LIST = new ArrayList<>();
    AFFECTED_FQN_LIST.add(REFLECTION_HELPER_FQN);
    AFFECTED_FQN_LIST.add(relocatableFQN("me/blvckbytes/bbreflect/packets/Interceptor"));
    AFFECTED_FQN_LIST.add(relocatableFQN("me/blvckbytes/bbreflect/packets/InterceptorFactory"));
  }

  private final boolean needsNettyPatching;
  private final ServerVersion version;
  private final Plugin plugin;
  private final Class<?> reflectionHelperClass;

  /**
   * Create a new reflection helper factory which manages loading the reflection helper
   * through the custom class loader behind the scenes to hand out an interface representation
   * @param plugin Plugin reference
   */
  public ReflectionHelperFactory(Plugin plugin) throws Exception {
    this.plugin = plugin;
    this.version = ServerVersion.current();

    // They used to inline netty on 1.7.x
    this.needsNettyPatching = this.version.compare(ServerVersion.V1_7_R10) <= 0;

    // No patching required, don't invoke the custom classloader
    if (!this.needsNettyPatching) {
      this.reflectionHelperClass = Class.forName(REFLECTION_HELPER_FQN);
      return;
    }

    // Pre-load the patched class for all subsequent make calls
    // FIXME: This custom classloader seems to produce weird behavior when reloading
    this.reflectionHelperClass = new UtfPatcherClassLoader(
      plugin.getClass().getClassLoader(), AFFECTED_FQN_LIST::contains, this::loaderPatcher
    ).loadClass(REFLECTION_HELPER_FQN);
  }

  /**
   * Create a new instance of the reflection helper on the current server version
   */
  public IReflectionHelper makeHelper() throws Exception {
    return (IReflectionHelper) reflectionHelperClass
      .getConstructor(Plugin.class, ServerVersion.class)
      .newInstance(this.plugin, version);
  }

  /**
   * Invoked whenever a UTF-8 constant is encountered within the constant
   * pool of a to-be-patched class' byte-code
   * @param name Name to patch (utf-8 value)
   * @return Patched name (utf-8 value)
   */
  private String loaderPatcher(String name) {
    if (needsNettyPatching && name.contains("io/netty"))
      return name.replace("io/netty", "net/minecraft/util/io/netty");
    return name;
  }

  /**
   * In order to allow the maven shade plugin to patch these fully qualified names
   * when it goes through UTF constants in the constant pool, I specify them as paths.
   * This method is more of a visual thing and only replaces slashes with dots.
   */
  private static String relocatableFQN(String fqn) {
    return fqn.replace('/', '.');
  }
}
