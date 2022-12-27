package me.blvckbytes.bbreflection;

import lombok.Getter;

@Getter
public class ComparableType {

  private final Class<?> type;
  private final boolean ignoreBoxing;
  private final Assignability assignability;

  /**
   * Create a new parameterized comparable type
   * @param type Base type
   * @param ignoreBoxing Whether to ignore boxing/unboxing
   * @param assignability Assignability mode/direction
   */
  public ComparableType(Class<?> type, boolean ignoreBoxing, Assignability assignability) {
    // If boxing is to be ignored, unwrap ahead of time
    this.type = ignoreBoxing ? Primitives.unwrap(type) : type;
    this.ignoreBoxing = ignoreBoxing;
    this.assignability = assignability;
  }

  /**
   * Checks whether this type matches another type
   * @param other Type to check against
   */
  public boolean matches(Class<?> other) {
    // Unbox the other type, if boxing is ignored
    if (ignoreBoxing)
      other = Primitives.unwrap(other);

    return isAssignable(other);
  }

  /**
   * Checks whether this type matches the specified
   * assignability with another type
   * @param other Type to check against
   */
  private boolean isAssignable(Class<?> other) {
    if (assignability == Assignability.NONE)
      return type.equals(other);

    // Check assignability modes
    switch (assignability) {

      case TARGET_TO_TYPE:
        return other.isAssignableFrom(type);

      case TYPE_TO_TARGET:
        return type.isAssignableFrom(other);

      // Don't try to match on assignability, just compare
      default:
        return type.equals(other);
    }
  }
}
