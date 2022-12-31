package me.blvckbytes.bbreflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

@SuppressWarnings("rawtypes")
public class ConstructorHandle extends AHandle<Constructor> {

  public ConstructorHandle(Class<?> target, FMemberPredicate<Constructor> predicate) throws NoSuchElementException {
    super(target, Constructor.class, predicate);
  }

  /**
   * Create a new instance by invoking this constructor
   * @param args Args to pass when calling
   * @return Instance of the constructor's declaring class
   */
  public Object newInstance(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
    return handle.newInstance(args);
  }

  /**
   * Get the number of parameters this constructor requires
   */
  public int getParameterCount() {
    return handle.getParameterCount();
  }

  @Override
  protected String stringify(Constructor member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.getDeclaringClass().getName());
    sj.add("(");

    StringJoiner argJoiner = new StringJoiner(", ");
    for (Class<?> parameter : member.getParameterTypes())
      argJoiner.add(parameter.getName());

    sj.add(argJoiner.toString());
    sj.add(")");

    return sj.toString();
  }
}
