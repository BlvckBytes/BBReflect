package me.blvckbytes.bbreflection;

import java.lang.reflect.Method;

@FunctionalInterface
public interface IMethodPredicate {

  /**
   * Tests whether a given method matches the requirements
   * @param m Method in question
   * @return True if matching, false otherwise
   */
  boolean matches(Method m);

}
