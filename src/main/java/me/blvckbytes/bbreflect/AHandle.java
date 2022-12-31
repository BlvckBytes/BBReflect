package me.blvckbytes.bbreflect;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

@Getter
public abstract class AHandle<T> {

  // TODO: Structure this class through the help of separator comments

  protected final T handle;
  protected final Class<T> handleType;

  protected AHandle(Class<?> target, Class<T> memberType, FMemberPredicate<T> predicate) throws NoSuchElementException {
    if (target == null)
      throw new IllegalStateException("Target has to be present");

    T result = walkClassHierarchyFor(memberType, target, (member, counter) -> {
      Boolean predicateResponse = predicate.matches(member, counter);
      if (predicateResponse == null)
        return HierarchyWalkDecision.SKIP;
      return predicateResponse ? HierarchyWalkDecision.BREAK : HierarchyWalkDecision.CONTINUE;
    });

    // The predicate matched on none of them
    if (result == null) {
      StringBuilder message = new StringBuilder("Could not satisfy the member predicate\nAvailable members:\n");

      walkClassHierarchyFor(memberType, target, (member, counter) -> {
        message.append('-').append(stringify(member)).append('\n');
        return HierarchyWalkDecision.CONTINUE;
      });

      throw new NoSuchElementException(message.toString());
    }

    if (result instanceof AccessibleObject)
      ((AccessibleObject) result).setAccessible(true);

    this.handle = result;
    this.handleType = memberType;
  }

  protected AHandle(T handle, Class<T> type) {
    this.handle = handle;
    this.handleType = type;
  }

  @Override
  public boolean equals(Object obj) {
    if (!handleType.isInstance(obj))
      return false;

    return handle.equals(obj);
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }

  @Override
  public String toString() {
    return stringify(handle);
  }

  protected abstract String stringify(T member);

  protected <T> @Nullable T walkClassHierarchyFor(Class<T> member, Class<?> base, BiFunction<T, Integer, HierarchyWalkDecision> decider) {
    int matchCounter = 0;
    T res = null;

    // Walk up the hierarchy chain
    Class<?> curr = base;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all member items of the current class
      for (T item : getAllEntriesOf(member, curr)) {
        HierarchyWalkDecision decision = decider.apply(item, matchCounter);

        // Skip means that the item would have matched, but
        // the skip counter has not yet elapsed
        if (decision == HierarchyWalkDecision.SKIP) {
          matchCounter++;
          continue;
        }

        // Predicate match, take the item
        if (decision == HierarchyWalkDecision.BREAK) {
          res = item;
          break;
        }
      }

      curr = curr.getSuperclass();
    }

    return res;
  }

  @SuppressWarnings("unchecked")
  private <T> T[] getAllEntriesOf(Class<T> member, Class<?> from) {
    if (member == Field.class)
      return (T[]) from.getDeclaredFields();

    if (member == Method.class)
      return (T[]) from.getDeclaredMethods();

    if (member == Class.class)
      return (T[]) from.getDeclaredClasses();

    if (member == Constructor.class)
      return (T[]) from.getDeclaredConstructors();

    throw new IllegalStateException("Unknown class member " + member + " requested");
  }
}
