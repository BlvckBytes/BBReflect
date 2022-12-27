package me.blvckbytes.bbreflection;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
      withParameter(t.get(), false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target constructor to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public ConstructorPredicateBuilder withParameter(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withParameter(generic.get(), allowBoxing, assignability);
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
  public ConstructorHandle required() throws Exception {
    return new ConstructorHandle(targetClass.get(), c -> {

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(c.getModifiers()) != isPublic)
        return false;

      // Not exactly as many parameters as requested
      int numParameters = parameterTypes.size();
      if (c.getParameterCount() != numParameters)
        return false;

      // Check parameters, if applicable
      Class<?>[] parameters = c.getParameterTypes();

      // Parameters need to match in sequence
      for (int i = 0; i < numParameters; i++) {
        if (!parameterTypes.get(i).matches(parameters[i]))
          return false;
      }

      return true;
    });
  }
}
