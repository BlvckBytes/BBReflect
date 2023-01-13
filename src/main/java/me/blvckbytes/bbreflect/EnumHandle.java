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

import me.blvckbytes.bbreflect.version.ServerVersion;

import java.util.Arrays;
import java.util.List;

public class EnumHandle extends ClassHandle {

  // TODO: Check against enum copies if they match the number of entries, else throw
  // TODO: Get an "enum" from static constants of a specific type within a class

  private final List<Enum<?>> e;

  /**
   * Create a new enumeration handle on top of a enumeration class
   * @param c Class which represents an enumeration
   * @param version Current server version
   * @throws IllegalStateException Thrown if the provided class is not an enumeration
   */
  public EnumHandle(Class<?> c, ServerVersion version) throws IllegalStateException {
    super(c, version);

    Object[] constants = c.getEnumConstants();

    // The provided class hasn't been of an enumeration type
    if (constants == null)
      throw new IllegalStateException("This class does not represent an enumeration.");

    // Create a unmodifiable list of constants and wrap into a handle
    e = Arrays.asList((Enum<?>[]) constants);
  }

  /**
   * Get an enumeration constant by it's ordinal integer
   * @param ordinal Ordinal integer
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  @SuppressWarnings("unchecked")
  public Enum<?> getByOrdinal(int ordinal) throws EnumConstantNotPresentException {
    try {
      return e.get(ordinal);
    } catch (Exception e) {
      throw new EnumConstantNotPresentException((Class<? extends Enum<?>>) handle, "ordinal=" + ordinal);
    }
  }

  /**
   * Get an enumeration constant by looking up the ordinal of a
   * copy enum which has it's constants sorted in the exact same order.
   * @param other Constant of a copy
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  public Enum<?> getByCopy(Enum<?> other) throws EnumConstantNotPresentException {
    return getByOrdinal(other.ordinal());
  }
}
