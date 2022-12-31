package me.blvckbytes.bbreflect;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FieldPredicateBuilder extends APredicateBuilder<FieldHandle> {

  private final ClassHandle targetClass;

  private @Nullable Boolean isStatic;
  private @Nullable Boolean isPublic;
  private @Nullable String name;
  private @Nullable ComparableType type;
  private final List<ComparableType> genericTypes;
  private boolean allowSuperclass;
  private int skip;

  /**
   * Create a new field predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public FieldPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
    this.isStatic = false;
    this.allowSuperclass = false;
    this.genericTypes = new ArrayList<>();
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target field's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public FieldPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /**
   * Define the target field's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public FieldPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  //////////////////////////////////// Name /////////////////////////////////////

  /**
   * Define the target field's name
   * @param name Field name, null means wildcard
   */
  public FieldPredicateBuilder withName(@Nullable String name) {
    this.name = name;
    return this;
  }

  //////////////////////////////////// Type /////////////////////////////////////

  /**
   * Define the target field's type
   * @param type Field type, null means wildcard
   */
  public FieldPredicateBuilder withType(@Nullable Class<?> type) {
    return withType(type, false, Assignability.NONE);
  }

  /**
   * Define the target field's type
   * @param type Field type, null means wildcard
   */
  public FieldPredicateBuilder withType(@Nullable ClassHandle type) {
    return withType(type == null ? null : type.getHandle());
  }

  /**
   * Define the target field's type
   * @param type Field type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public FieldPredicateBuilder withType(@Nullable Class<?> type, boolean allowBoxing, Assignability assignability) {
    if (type == null) {
      this.type = null;
      return this;
    }

    this.type = new ComparableType(type, allowBoxing, assignability);
    return this;
  }

  /**
   * Define the target field's type
   * @param type Field type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public FieldPredicateBuilder withType(@Nullable ClassHandle type, boolean allowBoxing, Assignability assignability) {
    return withType(type == null ? null : type.getHandle(), allowBoxing, assignability);
  }

  ////////////////////////////////// Generics ///////////////////////////////////

  /**
   * Add another generic type parameter of the target field to the sequence
   * @param generic Generic type to be present
   */
  public FieldPredicateBuilder withGeneric(Class<?> generic) {
    return withGeneric(generic, false, Assignability.NONE);
  }

  /**
   * Add another generic type parameter of the target field to the sequence
   * @param generic Generic type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public FieldPredicateBuilder withGeneric(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.genericTypes.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add another generic type parameter of the target field to the sequence
   * @param generic Generic type to be present
   */
  public FieldPredicateBuilder withGeneric(ClassHandle generic) {
    return withGeneric(generic.getHandle());
  }

  /**
   * Add another generic type parameter of the target field to the sequence
   * @param generic Generic type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public FieldPredicateBuilder withGeneric(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withGeneric(generic.getHandle(), allowBoxing, assignability);
  }

  ////////////////////////////////// Superclass /////////////////////////////////

  /**
   * Define whether walking up into the superclass is allowed
   * @param mode Superclass walking mode
   */
  public FieldPredicateBuilder withAllowSuperclass(boolean mode) {
    this.allowSuperclass = mode;
    return this;
  }

  /////////////////////////////////// Skipping //////////////////////////////////

  /**
   * Define how many matches to skip
   * @param skip Number of matches to skip
   */
  public FieldPredicateBuilder withSkip(int skip) {
    this.skip = skip;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable FieldHandle optional() {
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
  public FieldHandle required() throws NoSuchElementException {
    // At least a name or a type are required
    if (name == null && type == null)
      throw new IncompletePredicateBuilderException();

    return new FieldHandle(targetClass.getHandle(), (member, counter) -> {

      // Is inside of another class but superclass walking is disabled
      if (!allowSuperclass && member.getDeclaringClass() != targetClass.getHandle())
        return false;

      // Static modifier mismatch
      if (isStatic != null && Modifier.isStatic(member.getModifiers()) != isStatic)
        return false;

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(member.getModifiers()) != isPublic)
        return false;

      // Name mismatch
      if (name != null && !member.getName().equalsIgnoreCase(name))
        return false;

      // Type mismatch
      if (type != null && !type.matches(member.getType()))
        return false;

      // Check generic parameters, if applicable
      int numGenerics = genericTypes.size();
      if (numGenerics > 0) {
        Type genericType = member.getGenericType();

        // Not a generic type (<...>)
        if (!(genericType instanceof ParameterizedType))
          return false;

        Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();

        // Not enough generic type parameters available
        if (types.length < numGenerics)
          return false;

        // Type parameters need to match in sequence
        for (int i = 0; i < numGenerics; i++) {
          if (!genericTypes.get(i).matches(unwrapType(types[i])))
            return false;
        }
      }

      // Everything matches, while skip > matchCounter, count up
      if (skip > counter)
        return null;

      return true;
    });
  }

  /**
   * Attempts to unwrap a given type to it's raw type class
   * @param type Type to unwrap
   * @return Unwrapped type
   */
  private Class<?> unwrapType(Type type) {
    if (type instanceof Class)
      return (Class<?>) type;

    if (type instanceof ParameterizedType)
      return unwrapType(((ParameterizedType) type).getRawType());

    throw new IllegalStateException("Cannot unwrap type of class=" + type.getClass());
  }
}
