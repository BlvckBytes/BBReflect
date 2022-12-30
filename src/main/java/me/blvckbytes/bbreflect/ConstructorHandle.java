package me.blvckbytes.bbreflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorHandle {

  private final Constructor<?> constructor;

  /**
   * Create a new constructor handle by locating the target constructor within
   * the given target class by dispatching the predicate immediately.
   * @param target Target class to search in
   * @param predicate Predicate which chooses the matching constructor
   * @throws NoSuchMethodException Thrown if the predicate didn't yield any results
   */
  public ConstructorHandle(Class<?> target, IConstructorPredicate predicate) throws NoSuchMethodException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    // Loop all constructors of this class and call the predicate on each of them
    Constructor<?> res = null;
    for (Constructor<?> c : target.getDeclaredConstructors()) {
      if (!predicate.matches(c))
        continue;

      // Predicate match, take constructor
      res = c;
      break;
    }

    // The predicate matched on none of them
    if (res == null)
      throw new NoSuchMethodException("Could not satisfy the constructor predicate.");

    // Set the constructor accessible and hold a reference to it
    this.constructor = res;
    this.constructor.setAccessible(true);
  }

  /**
   * Create a new instance by invoking this constructor
   * @param args Args to pass when calling
   * @return Instance of the constructor's declaring class
   */
  public Object newInstance(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
    return this.constructor.newInstance(args);
  }

  /**
   * Get the number of parameters this constructor requires
   */
  public int getParameterCount() {
    return this.constructor.getParameterCount();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Constructor<?>))
      return false;

    return constructor.equals(obj);
  }

  @Override
  public int hashCode() {
    return constructor.hashCode();
  }

  @Override
  public String toString() {
    return constructor.toString();
  }
}
