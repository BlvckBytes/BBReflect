package me.blvckbytes.bbreflect;

import java.lang.reflect.Constructor;

@FunctionalInterface
public interface IConstructorPredicate {

  /**
   * Tests whether a given constructor matches the requirements
   * @param c Constructor in question
   * @return True if matching, false otherwise
   */
  boolean matches(Constructor<?> c);

}
