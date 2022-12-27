package me.blvckbytes.bbreflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodHandle {

  private final Method method;

  /**
   * Create a new method handle by locating the target method within
   * the given target class by dispatching the predicate immediately.
   * @param target Target class to search in
   * @param predicate Predicate which chooses the matching method
   * @throws NoSuchMethodException Thrown if the predicate didn't yield any results
   */
  public MethodHandle(Class<?> target, IMethodPredicate predicate) throws NoSuchMethodException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    Method res = null;

    // Walk up the hierarchy chain
    Class<?> curr = target;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all methods of the current class
      for (Method m : curr.getDeclaredMethods()) {
        if (!predicate.matches(m))
          continue;

        // Predicate match, take method
        res = m;
        break;
      }

      curr = curr.getSuperclass();
    }

    // The predicate matched on none of them
    if (res == null)
      throw new NoSuchMethodException("Could not satisfy the method predicate.");

    // Set the constructor accessible and hold a reference to it
    this.method = res;
    this.method.setAccessible(true);
  }

  /**
   * Invoke this method on an object instance
   * @param o Target object to invoke on
   * @param args Arguments to pass when invoking the method
   * @return Method return value
   */
  public Object invoke(Object o, Object... args) throws InvocationTargetException, IllegalAccessException {
    return this.method.invoke(o, args);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Method))
      return false;

    return method.equals(obj);
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }

  @Override
  public String toString() {
    return method.toString();
  }
}
