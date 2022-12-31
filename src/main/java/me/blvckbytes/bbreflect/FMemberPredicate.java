package me.blvckbytes.bbreflect;

@FunctionalInterface
public interface FMemberPredicate<T> {

  Boolean matches(T member, int counter);

}
