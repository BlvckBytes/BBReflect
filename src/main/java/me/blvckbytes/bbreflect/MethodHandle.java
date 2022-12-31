package me.blvckbytes.bbreflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class MethodHandle extends AHandle<Method> {

  protected MethodHandle(Class<?> target, FMemberPredicate<Method> predicate) throws NoSuchElementException {
    super(target, Method.class, predicate);
  }

  /**
   * Invoke this method on an object instance
   * @param o Target object to invoke on
   * @param args Arguments to pass when invoking the method
   * @return Method return value
   */
  public Object invoke(Object o, Object... args) throws InvocationTargetException, IllegalAccessException {
    return handle.invoke(o, args);
  }

  @Override
  protected String stringify(Method member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.getReturnType().getName());
    sj.add(member.getName());
    sj.add("(");

    StringJoiner argJoiner = new StringJoiner(", ");
    for (Class<?> parameter : member.getParameterTypes())
      argJoiner.add(parameter.getName());

    sj.add(argJoiner.toString());
    sj.add(")");

    return sj.toString();
  }
}
