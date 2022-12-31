package me.blvckbytes.bbreflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class FieldHandle extends AHandle<Field> {

  public FieldHandle(Class<?> target, FMemberPredicate<Field> predicate) throws NoSuchElementException {
    super(target, Field.class, predicate);
  }

  /**
   * Set the field's value on an object instance
   * @param o Target object to modify
   * @param v Field value to set
   */
  public void set(Object o, Object v) throws IllegalAccessException {
    this.handle.set(o, v);
  }

  /**
   * Get the field's value from an object instance
   * @param o Target object to read from
   * @return Field value
   */
  public Object get(Object o) throws IllegalAccessException {
    return this.handle.get(o);
  }

  @Override
  protected String stringify(Field member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.getType().getName());
    sj.add(member.getName());

    return sj.toString();
  }
}
