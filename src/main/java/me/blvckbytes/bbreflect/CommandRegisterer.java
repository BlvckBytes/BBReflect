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

import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.handle.FieldHandle;
import me.blvckbytes.bbreflect.handle.MethodHandle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegisterer {

  private final Object CONST_COMMAND_MAP;
  private final MethodHandle M_COMMAND_MAP__REGISTER;
  private final JavaPlugin plugin;

  public CommandRegisterer(JavaPlugin plugin, IReflectionHelper reflectionHelper) throws Exception {
    this.plugin = plugin;

    ClassHandle C_CRAFT_COMMAND_MAP = reflectionHelper.getClass(RClass.CRAFT_COMMAND_MAP);
    ClassHandle C_CRAFT_SERVER = reflectionHelper.getClass(RClass.CRAFT_SERVER);

    FieldHandle F_CRAFT_SERVER__CRAFT_COMMAND_MAP = C_CRAFT_SERVER.locateField()
      .withType(C_CRAFT_COMMAND_MAP)
      .required();

    CONST_COMMAND_MAP = F_CRAFT_SERVER__CRAFT_COMMAND_MAP.get(Bukkit.getServer());

    M_COMMAND_MAP__REGISTER = C_CRAFT_COMMAND_MAP.locateMethod()
      .withAllowSuperclass(true)
      .withPublic(true)
      .withParameters(String.class, Command.class)
      .required();
  }

  public void register(Command command) throws Exception {
    M_COMMAND_MAP__REGISTER.invoke(CONST_COMMAND_MAP, plugin.getName(), command);
  }
}
