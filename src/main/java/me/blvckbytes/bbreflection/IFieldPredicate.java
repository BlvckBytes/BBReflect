package me.blvckbytes.bbreflection;

import java.lang.reflect.Field;

@FunctionalInterface
public interface IFieldPredicate {

  /**
   * Tests whether a given field matches the requirements
   * @param f Field in question
   * @param counter Counter, increases by one whenever the predicate yields null
   * @return True if matching, false otherwise, null to increase the passed counter once
   */
  Boolean matches(Field f, int counter);

}
