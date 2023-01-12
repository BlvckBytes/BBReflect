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

import lombok.Getter;

@Getter
public class ComparableType {

  private final Class<?> type;
  private final boolean ignoreBoxing;
  private final Assignability assignability;

  /**
   * Create a new parameterized comparable type
   * @param type Base type
   * @param ignoreBoxing Whether to ignore boxing/unboxing
   * @param assignability Assignability mode/direction
   */
  public ComparableType(Class<?> type, boolean ignoreBoxing, Assignability assignability) {
    // If boxing is to be ignored, unwrap ahead of time
    this.type = ignoreBoxing ? Primitives.unwrap(type) : type;
    this.ignoreBoxing = ignoreBoxing;
    this.assignability = assignability;
  }

  /**
   * Checks whether this type matches another type
   * @param other Type to check against
   */
  public boolean matches(Class<?> other) {
    // Unbox the other type, if boxing is ignored
    if (ignoreBoxing)
      other = Primitives.unwrap(other);

    return isAssignable(other);
  }

  /**
   * Checks whether this type matches the specified
   * assignability with another type
   * @param other Type to check against
   */
  private boolean isAssignable(Class<?> other) {
    if (assignability == Assignability.NONE)
      return type.equals(other);

    // Check assignability modes
    switch (assignability) {

      case TARGET_TO_TYPE:
        return other.isAssignableFrom(type);

      case TYPE_TO_TARGET:
        return type.isAssignableFrom(other);

      // Don't try to match on assignability, just compare
      default:
        return type.equals(other);
    }
  }
}
