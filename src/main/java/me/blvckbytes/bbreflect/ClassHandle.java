package me.blvckbytes.bbreflect;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class ClassHandle {

  // Caching manual encapsulations using the of() constructor here
  private static final Map<Class<?>, ClassHandle> encapsulations;

  // Caching enumeration constants
  private static final Map<Class<?>, EnumHandle> enumerations;

  static {
    encapsulations = new HashMap<>();
    enumerations = new HashMap<>();
  }

  protected final Class<?> c;

  /**
   * Create a new class handle by locating the target class within
   * the given target class by dispatching the predicate immediately.
   * @param target Target class to search in
   * @param predicate Predicate which chooses the matching class
   * @throws ClassNotFoundException Thrown if the predicate didn't yield any results
   */
  public ClassHandle(Class<?> target, IClassPredicate predicate) throws ClassNotFoundException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    int counter = 0;
    Class<?> res = null;

    // Walk up the hierarchy chain
    Class<?> curr = target;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all inner classes of the current class
      for (Class<?> c : curr.getDeclaredClasses()) {
        Boolean result = predicate.matches(c, counter);

        // Null means that it would have matched, but the
        // skip counter has not yet elapsed
        if (result == null) {
          counter++;
          continue;
        }

        // Predicate match, take the class
        if (result) {
          res = c;
          break;
        }
      }

      curr = curr.getSuperclass();
    }

    // The predicate matched on none of them
    if (res == null)
      throw new ClassNotFoundException("Could not satisfy the class predicate.");

    // Hold a reference to it
    this.c = res;
  }

  /**
   * Get the encapsulated class directly
   */
  public Class<?> get() {
    return this.c;
  }

  /**
   * Checks whether an object is an instance of this class
   * @param o Object to check
   */
  public boolean isInstance(Object o) {
    return this.c.isInstance(o);
  }

  /**
   * Interpret this class as an enumeration and get a handle to it
   * @throws IllegalStateException Thrown if this class is not an enumeration
   */
  public EnumHandle asEnum() throws IllegalStateException {
    EnumHandle enumHandle = enumerations.get(c);

    // Use cached value
    if (enumHandle != null)
      return enumHandle;

    // Create a new enum handle on this class
    enumHandle = new EnumHandle(c);

    // Store in cache and return
    enumerations.put(c, enumHandle);
    return enumHandle;
  }

  /**
   * Create a new FieldHandle builder which will query this class
   */
  public FieldPredicateBuilder locateField() {
    return new FieldPredicateBuilder(this);
  }

  /**
   * Create a new MethodHandle builder which will query this class
   */
  public MethodPredicateBuilder locateMethod() {
    return new MethodPredicateBuilder(this);
  }

  /**
   * Create a new ConstructorHandle builder which will query this class
   */
  public ConstructorPredicateBuilder locateConstructor() {
    return new ConstructorPredicateBuilder(this);
  }

  /**
   * Create a new ClassHandle builder which will query this class
   */
  public ClassPredicateBuilder locateClass() {
    return new ClassPredicateBuilder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Class<?>))
      return false;

    return c.equals(obj);
  }

  @Override
  public int hashCode() {
    return c.hashCode();
  }

  @Override
  public String toString() {
    return c.toString();
  }

  /**
   * Create a new class handle on top of a vanilla class
   * @param c Target class
   */
  public static ClassHandle of(Class<?> c) {
    ClassHandle handle = encapsulations.get(c);

    // Create new instance
    if (handle == null) {
      handle = new ClassHandle(c);
      encapsulations.put(c, handle);
      return handle;
    }

    // Return existing instance
    return handle;
  }
}
