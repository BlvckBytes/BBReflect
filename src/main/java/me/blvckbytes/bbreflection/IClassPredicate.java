package me.blvckbytes.bbreflection;

@FunctionalInterface
public interface IClassPredicate {

  /**
   * Tests whether a given class matches the requirements
   * @param c Class in question
   * @param counter Counter, increases by one whenever the predicate yields null
   * @return True if matching, false otherwise, null to increase the passed counter once
   */
  Boolean matches(Class<?> c, int counter);

}
