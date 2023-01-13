/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbreflect;

import me.blvckbytes.bbreflect.version.ServerVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class FieldHandle extends AHandle<Field> {

  public FieldHandle(Class<?> target, ServerVersion version, FMemberPredicate<Field> predicate) throws NoSuchElementException {
    super(target, Field.class, version, predicate);
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
