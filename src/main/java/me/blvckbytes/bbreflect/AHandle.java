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
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

@Getter
public abstract class AHandle<T> {

  protected final T handle;
  protected final Class<T> handleType;
  protected final ServerVersion version;

  /**
   * Create a new handle by running a member predicate on a target class' members
   * @param target Target class
   * @param memberType Target member type
   * @param version Current server version
   * @param predicate Member predicate to run
   * @throws NoSuchElementException Member predicate found no matches
   */
  protected AHandle(Class<?> target, Class<T> memberType, ServerVersion version, FMemberPredicate<T> predicate) throws NoSuchElementException {
    if (target == null)
      throw new IllegalStateException("Target has to be present");

    // Execute the predicate on all members of target type within the whole target class hierarchy
    T result = walkClassHierarchyFor(memberType, target, (member, counter) -> {
      Boolean predicateResponse = predicate.matches(member, counter);
      if (predicateResponse == null)
        return HierarchyWalkDecision.SKIP;
      return predicateResponse ? HierarchyWalkDecision.BREAK : HierarchyWalkDecision.CONTINUE;
    });

    // The predicate matched on none of them
    if (result == null) {
      StringBuilder message = new StringBuilder("Could not satisfy the member predicate\nAvailable members:\n");

      // Print all available members by walking the hierarchy again
      walkClassHierarchyFor(memberType, target, (member, counter) -> {
        message.append('-').append(stringify(member)).append('\n');
        return HierarchyWalkDecision.CONTINUE;
      });

      throw new NoSuchElementException(message.toString());
    }

    if (result instanceof AccessibleObject)
      ((AccessibleObject) result).setAccessible(true);

    this.version = version;
    this.handle = result;
    this.handleType = memberType;
  }

  /**
   * Construct a new handle using an immediate value
   * @param handle Immediate value
   * @param type Type of handle
   * @param version Current server version
   */
  protected AHandle(T handle, Class<T> type, ServerVersion version) {
    this.handle = handle;
    this.handleType = type;
    this.version = version;
  }

  /**
   * Used to print a member in a human readable format with all available information displayed
   * @param member Member to stringify
   */
  protected abstract String stringify(T member);

  //=========================================================================//
  //                             Object Overrides                            //
  //=========================================================================//

  @Override
  public boolean equals(Object obj) {
    if (!handleType.isInstance(obj))
      return false;

    return handle.equals(obj);
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }

  @Override
  public String toString() {
    return stringify(handle);
  }

  //=========================================================================//
  //                                 Helpers                                 //
  //=========================================================================//

  protected @Nullable T walkClassHierarchyFor(Class<T> member, Class<?> base, BiFunction<T, Integer, HierarchyWalkDecision> decider) {
    int matchCounter = 0;
    T res = null;

    // Walk up the hierarchy chain
    Class<?> curr = base;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all member items of the current class
      for (T item : getAllEntriesOf(member, curr)) {
        HierarchyWalkDecision decision = decider.apply(item, matchCounter);

        // Skip means that the item would have matched, but
        // the skip counter has not yet elapsed
        if (decision == HierarchyWalkDecision.SKIP) {
          matchCounter++;
          continue;
        }

        // Predicate match, take the item
        if (decision == HierarchyWalkDecision.BREAK) {
          res = item;
          break;
        }
      }

      curr = curr.getSuperclass();
    }

    return res;
  }

  @SuppressWarnings("unchecked")
  private T[] getAllEntriesOf(Class<T> member, Class<?> from) {
    if (member == Field.class)
      return (T[]) from.getDeclaredFields();

    if (member == Method.class)
      return (T[]) from.getDeclaredMethods();

    if (member == Class.class)
      return (T[]) from.getDeclaredClasses();

    if (member == Constructor.class)
      return (T[]) from.getDeclaredConstructors();

    throw new IllegalStateException("Unknown class member " + member + " requested");
  }
}
