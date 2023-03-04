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
import me.blvckbytes.bbreflect.handle.predicate.Assignability;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegisterer {

  private final CommandMap CONST_COMMAND_MAP;
  private final JavaPlugin plugin;

  public CommandRegisterer(JavaPlugin plugin, IReflectionHelper reflectionHelper) throws Exception {
    this.plugin = plugin;

    ClassHandle C_CRAFT_SERVER = reflectionHelper.getClass(RClass.CRAFT_SERVER);

    FieldHandle F_CRAFT_SERVER__CRAFT_COMMAND_MAP = C_CRAFT_SERVER.locateField()
      .withType(CommandMap.class, false, Assignability.TYPE_TO_TARGET)
      .required();

    CONST_COMMAND_MAP = (CommandMap) F_CRAFT_SERVER__CRAFT_COMMAND_MAP.get(Bukkit.getServer());
  }

  public void register(Command command) {
    CONST_COMMAND_MAP.register(plugin.getName(), command);
  }
}
