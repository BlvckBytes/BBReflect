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

import me.blvckbytes.bbreflect.packets.IInterceptor;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IReflectionHelper {

  /**
   * Sends a packet to a player and provides a way to synchronize to it's way out
   * @param player Target player to send to
   * @param packet Packet to send
   * @param completion Optional completion callback
   */
  void sendPacket(Player player, Object packet, @Nullable Runnable completion) throws Exception;

  /**
   * Get a {@link ClassHandle} by it's corresponding internal name {@link RClass}
   * @param rc Internal name to resolve
   * @return Resolved class handle
   * @throws ClassNotFoundException Target not available in the current environment
   */
  ClassHandle getClass(RClass rc) throws ClassNotFoundException;

  /**
   * Get a {@link ClassHandle} by it's corresponding internal name {@link RClass}
   * and return null if that target isn't available in the current environment
   * @param rc Internal name to resolve
   * @return Resolved class handle
   */
  @Nullable ClassHandle getClassOptional(RClass rc);

  /**
   * Get a {@link EnumHandle} by it's corresponding internal name {@link RClass}
   * after interpreting it's {@link ClassHandle} as an enumeration and return null
   * if that target isn't available in the current environment
   * @param rc Internal name to resolve
   * @return Resolved enum handle
   */
  @Nullable EnumHandle getEnumOptional(RClass rc);

  /**
   * Setup the internal hooks and preparations necessary to intercept all packet I/O
   * @param handlerName Name to use when adding a custom handler to a channel's pipeline
   * @param interceptor Handler which receives interceptor instances right after instantiation
   */
  void setupInterception(String handlerName, Consumer<IInterceptor> interceptor) throws Exception;

  /**
   * Clean up all previously setup hooks and preparations to in essence roll back to vanilla
   * state again. Does nothing if interception is not currently set up.
   */
  void cleanupInterception();

  /**
   * Get the interceptor instance which corresponds to a given player
   * @param p Target player
   * @return Interceptor instance if available, null otherwise, also null if interception is not set up
   */
  @Nullable IInterceptor getInterceptorFor(Player p);

  /**
   * Get the version the server currently runs on
   */
  ServerVersion getVersion();

}
