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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class MethodPredicateBuilder extends APredicateBuilder<MethodHandle, MethodPredicateBuilder> {

  private @Nullable FCallTransformer callTransformer;
  private @Nullable Boolean isStatic;
  private @Nullable Boolean isPublic;
  private @Nullable Boolean isAbstract;
  private @Nullable String name;
  private @Nullable ComparableType returnType;
  private final List<ComparableType> returnGenerics;
  private final List<ComparableType> parameterTypes;
  private boolean allowSuperclass;
  private int skip;

  /**
   * Create a new method predicate builder on a class handle
   * @param targetClass Class to search through
   * @param version Current server version
   */
  public MethodPredicateBuilder(ClassHandle targetClass, ServerVersion version) {
    super(targetClass, version);

    this.isStatic = false;
    this.allowSuperclass = false;
    this.parameterTypes = new ArrayList<>();
    this.returnGenerics = new ArrayList<>();
  }

  ////////////////////////////////// Transformer /////////////////////////////////

  /**
   * Set the call transformer which will be invoked before relaying
   * the call to the handle's wrapped member
   * @param transformer Transformer to set
   * @param dependencies List of handles which this transformer depends on and which have to be present
   */
  public MethodPredicateBuilder withTransformer(@Nullable FCallTransformer transformer, AHandle<?>... dependencies) {
    this.callTransformer = transformer;

    if (isInVersionRange()) {
      for (AHandle<?> handle : dependencies) {
        if (handle == null)
          throw new IllegalStateException("One of the transformers dependencies is missing");
      }
    }

    return this;
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target method's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public MethodPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /**
   * Define the target method's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public MethodPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  /**
   * Define the target method's abstract modifier presence
   * @param mode Abstract modifier presence, null means wildcard
   */
  public MethodPredicateBuilder withAbstract(@Nullable Boolean mode) {
    this.isAbstract = mode;
    return this;
  }

  //////////////////////////////////// Name /////////////////////////////////////

  /**
   * Define the target method's name
   * @param name Method name, null means wildcard
   */
  public MethodPredicateBuilder withName(@Nullable String name) {
    this.name = name;
    return this;
  }

  //////////////////////////////////// Type /////////////////////////////////////

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   */
  public MethodPredicateBuilder withReturnType(@Nullable Class<?> type) {
    return withReturnType(type, false, Assignability.NONE);
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   */
  public MethodPredicateBuilder withReturnType(@Nullable ClassHandle type) {
    return withReturnType(type == null ? null : type.getHandle());
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnType(@Nullable Class<?> type, boolean allowBoxing, Assignability assignability) {
    if (type == null) {
      this.returnType = null;
      return this;
    }

    this.returnType = new ComparableType(type, allowBoxing, assignability);
    return this;
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnType(@Nullable ClassHandle type, boolean allowBoxing, Assignability assignability) {
    return withReturnType(type == null ? null : type.getHandle(), allowBoxing, assignability);
  }

  ////////////////////////////////// Generics ///////////////////////////////////

  /**
   * Add another generic return type of the target method to the sequence
   * @param generic Generic type to be present
   */
  public MethodPredicateBuilder withReturnGeneric(Class<?> generic) {
    return withReturnGeneric(generic, false, Assignability.NONE);
  }

  /**
   * Add another generic return type of the target method to the sequence
   * @param generic Generic type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnGeneric(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.returnGenerics.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add another generic return type of the target method to the sequence
   * @param generic Generic type to be present
   */
  public MethodPredicateBuilder withReturnGeneric(ClassHandle generic) {
    return withReturnGeneric(generic.getHandle());
  }

  /**
   * Add another generic return type of the target method to the sequence
   * @param generic Generic type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnGeneric(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withReturnGeneric(generic.getHandle(), allowBoxing, assignability);
  }

  ///////////////////////////////// Parameters //////////////////////////////////

  /**
   * Add more parameter types of the target method to the sequence
   * @param types Types to be present
   */
  public MethodPredicateBuilder withParameters(Class<?>... types) {
    for (Class<?> t : types)
      withParameter(t, false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withParameter(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.parameterTypes.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param type Type to be present
   */
  public MethodPredicateBuilder withParameter(Class<?> type) {
    this.parameterTypes.add(new ComparableType(type, false, Assignability.NONE));
    return this;
  }

  /**
   * Add more parameter types of the target method to the sequence
   * @param types Types to be present
   */
  public MethodPredicateBuilder withParameters(ClassHandle... types) {
    for (ClassHandle t : types)
      withParameter(t.getHandle(), false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param type Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withParameter(ClassHandle type, boolean allowBoxing, Assignability assignability) {
    return withParameter(type.getHandle(), allowBoxing, assignability);
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param type Type to be present
   */
  public MethodPredicateBuilder withParameter(ClassHandle type) {
    this.parameterTypes.add(new ComparableType(type.getHandle(), false, Assignability.NONE));
    return this;
  }

  ////////////////////////////////// Superclass /////////////////////////////////

  /**
   * Define whether walking up into the superclass is allowed
   * @param mode Superclass walking mode
   */
  public MethodPredicateBuilder withAllowSuperclass(boolean mode) {
    this.allowSuperclass = mode;
    return this;
  }

  /////////////////////////////////// Skipping //////////////////////////////////

  /**
   * Define how many matches to skip
   * @param skip Number of matches to skip
   */
  public MethodPredicateBuilder withSkip(int skip) {
    this.skip = skip;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public MethodPredicateBuilder orElse(Supplier<MethodPredicateBuilder> builder) {
    fallbacks.add(builder.get());
    return this;
  }

  @Override
  public MethodHandle required() throws NoSuchElementException {
    // At least a name , a return type or parameter types are required
    if (name == null && returnType == null && parameterTypes.size() == 0)
      throw new IncompletePredicateBuilderException();

    try {
      checkVersionRange();

      return new MethodHandle(targetClass.getHandle(), version, callTransformer, (member, counter) -> {

        // Is inside of another class but superclass walking is disabled
        if (!allowSuperclass && member.getDeclaringClass() != targetClass.getHandle())
          return false;

        // Static modifier mismatch
        if (isStatic != null && Modifier.isStatic(member.getModifiers()) != isStatic)
          return false;

        // Abstract modifier mismatch
        if (isAbstract != null && Modifier.isAbstract(member.getModifiers()) != isAbstract)
          return false;

        // Public modifier mismatch
        if (isPublic != null && Modifier.isPublic(member.getModifiers()) != isPublic)
          return false;

        // Name mismatch
        if (name != null && !member.getName().equalsIgnoreCase(name))
          return false;

        // Return type mismatch
        if (returnType != null && !returnType.matches(member.getReturnType()))
          return false;

        // Check parameters, if applicable
        int numParameters = parameterTypes.size();
        Class<?>[] parameters = member.getParameterTypes();

        // Not exactly as many parameters as requested
        if (numParameters != parameters.length)
          return false;

        // Parameters need to match in sequence
        for (int i = 0; i < numParameters; i++) {
          if (!parameterTypes.get(i).matches(parameters[i]))
            return false;
        }

        // Check generic return parameters, if applicable
        int numGenerics = returnGenerics.size();
        if (numGenerics > 0) {
          Type[] types = ((ParameterizedType) member.getGenericReturnType()).getActualTypeArguments();

          // Not enough generic type parameters available
          if (types.length < numGenerics)
            return false;

          // Type parameters need to match in sequence
          for (int i = 0; i < numGenerics; i++) {
            if (!returnGenerics.get(i).matches((Class<?>) types[i]))
              return false;
          }
        }

        // Everything matches, while skip > matchCounter, count up
        if (skip > counter)
          return null;

        return true;
      });
    } catch (NoSuchElementException e) {
      return invokeFallbacks(e);
    }
  }
}
