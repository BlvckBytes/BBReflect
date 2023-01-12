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

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ConstructorPredicateBuilder extends APredicateBuilder<ConstructorHandle> {

  private final ClassHandle targetClass;
  private @Nullable Boolean isPublic;
  private final List<ComparableType> parameterTypes;

  /**
   * Create a new constructor predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public ConstructorPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
    this.parameterTypes = new ArrayList<>();
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target constructor's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public ConstructorPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  ///////////////////////////////// Parameters //////////////////////////////////

  /**
   * Add more parameter types of the target constructor to the sequence
   * @param types Types to be present
   */
  public ConstructorPredicateBuilder withParameters(Class<?>... types) {
    for (Class<?> t : types)
      withParameter(t, false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target constructor to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public ConstructorPredicateBuilder withParameter(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.parameterTypes.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add more parameter types of the target constructor to the sequence
   * @param types Types to be present
   */
  public ConstructorPredicateBuilder withParameters(ClassHandle... types) {
    for (ClassHandle t : types)
      withParameter(t.getHandle(), false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target constructor to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public ConstructorPredicateBuilder withParameter(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withParameter(generic.getHandle(), allowBoxing, assignability);
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable ConstructorHandle optional() {
    try {
      return required();
    }

    catch (IncompletePredicateBuilderException e) {
      throw e;
    }

    catch (Exception e) {
      return null;
    }
  }

  @Override
  public ConstructorHandle required() throws NoSuchElementException {
    return new ConstructorHandle(targetClass.getHandle(), (member, count) -> {

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(member.getModifiers()) != isPublic)
        return false;

      // Not exactly as many parameters as requested
      int numParameters = parameterTypes.size();
      if (member.getParameterCount() != numParameters)
        return false;

      // Check parameters, if applicable
      Class<?>[] parameters = member.getParameterTypes();

      // Parameters need to match in sequence
      for (int i = 0; i < numParameters; i++) {
        if (!parameterTypes.get(i).matches(parameters[i]))
          return false;
      }

      return true;
    });
  }
}
