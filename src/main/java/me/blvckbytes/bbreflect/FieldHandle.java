package me.blvckbytes.bbreflect;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiFunction;

public class FieldHandle {

  private final Field field;

  /**
   * Create a new field handle by locating the target field within
   * the given target class by dispatching the predicate immediately.
   * @param target Target class to search in
   * @param predicate Predicate which chooses the matching field
   * @throws NoSuchFieldException Thrown if the predicate didn't yield any results
   */
  public FieldHandle(Class<?> target, IFieldPredicate predicate) throws NoSuchFieldException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    Field result = walkClassesFields(target, (f, counter) -> {
      Boolean predicateResponse = predicate.matches(f, counter);
      if (predicateResponse == null)
        return WalkIterationDecision.SKIP;
      return predicateResponse ? WalkIterationDecision.BREAK : WalkIterationDecision.CONTINUE;
    });

    // The predicate matched on none of them
    if (result == null) {
      StringBuilder message = new StringBuilder("Could not satisfy the field predicate\nAvailable fields:\n");

      walkClassesFields(target, (f, counter) -> {
        message.append('-');
        stringifyField(f, message);
        message.append('\n');
        return WalkIterationDecision.CONTINUE;
      });

      throw new NoSuchFieldException(message.toString());
    }

    // Set the field accessible and hold a reference to it
    this.field = result;
    this.field.setAccessible(true);
  }

  // NOTE: Adds a trailing space
  private void stringifyModifiers(int modifiers, StringBuilder sb) {
    if (Modifier.isPrivate(modifiers))
      sb.append("private ");

    if (Modifier.isStatic(modifiers))
      sb.append("static ");

    if (Modifier.isPublic(modifiers))
      sb.append("public ");

    if (Modifier.isFinal(modifiers))
      sb.append("final ");

    if (Modifier.isProtected(modifiers))
      sb.append("protected ");
  }

  private void stringifyField(Field f, StringBuilder sb) {
    int modifiers = f.getModifiers();

    stringifyModifiers(modifiers, sb);
    sb.append(f.getType());
    sb.append(' ').append(f.getName());
  }

  private @Nullable Field walkClassesFields(Class<?> c, BiFunction<Field, Integer, WalkIterationDecision> decider) {
    int counter = 0;
    Field res = null;

    // Walk up the hierarchy chain
    Class<?> curr = c;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all fields of the current class
      for (Field f : curr.getDeclaredFields()) {
        WalkIterationDecision decision = decider.apply(f, counter);

        // Skip means that it would have matched, but the
        // skip counter has not yet elapsed
        if (decision == WalkIterationDecision.SKIP) {
          counter++;
          continue;
        }

        // Predicate match, take the field
        if (decision == WalkIterationDecision.BREAK) {
          res = f;
          break;
        }
      }

      curr = curr.getSuperclass();
    }

    return res;
  }

  /**
   * Set the field's value on an object instance
   * @param o Target object to modify
   * @param v Field value to set
   */
  public void set(Object o, Object v) throws IllegalAccessException {
    this.field.set(o, v);
  }

  /**
   * Get the field's value from an object instance
   * @param o Target object to read from
   * @return Field value
   */
  public Object get(Object o) throws IllegalAccessException {
    return this.field.get(o);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Field))
      return false;

    return field.equals(obj);
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }

  @Override
  public String toString() {
    return field.toString();
  }
}
