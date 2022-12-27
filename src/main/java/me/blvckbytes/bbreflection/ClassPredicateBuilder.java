package me.blvckbytes.bbreflection;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;

public class ClassPredicateBuilder extends APredicateBuilder<ClassHandle> {

  private final ClassHandle targetClass;
  private @Nullable Boolean isPublic;
  private @Nullable Boolean isStatic;
  private int skip;

  /**
   * Create a new class predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public ClassPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target class's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  /**
   * Define the target class's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /////////////////////////////////// Skipping //////////////////////////////////

  /**
   * Define how many matches to skip
   * @param skip Number of matches to skip
   */
  public ClassPredicateBuilder withSkip(int skip) {
    this.skip = skip;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable ClassHandle optional() {
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
  public ClassHandle required() throws Exception {
    return new ClassHandle(targetClass.get(), (c, mc) -> {

      // Static modifier mismatch
      if (isStatic != null && Modifier.isStatic(c.getModifiers()) != isStatic)
        return false;

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(c.getModifiers()) != isPublic)
        return false;

      // Everything matches, while skip > matchCounter, count up
      if (skip > mc)
        return null;

      return true;
    });
  }
}
