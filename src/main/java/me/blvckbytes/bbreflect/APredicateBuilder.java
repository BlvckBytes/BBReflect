package me.blvckbytes.bbreflect;

import org.jetbrains.annotations.Nullable;

public abstract class APredicateBuilder<T> {

  /**
   * Get the predicate's result and return null if it couldn't be located
   */
  public abstract @Nullable T optional();

  /**
   * Get the predicate's result and require that it's not null
   * @throws Exception Not found exception if the result could not be located
   */
  public abstract T required() throws Exception;

}
