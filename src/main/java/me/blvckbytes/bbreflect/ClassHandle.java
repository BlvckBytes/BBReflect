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

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

@SuppressWarnings("rawtypes")
public class ClassHandle extends AHandle<Class> {

  // Caching manual encapsulations using the of() constructor here
  private static final Map<Class<?>, ClassHandle> encapsulations;

  // Caching enumeration constants
  private static final Map<Class<?>, EnumHandle> enumerations;

  static {
    encapsulations = new HashMap<>();
    enumerations = new HashMap<>();
  }

  public ClassHandle(Class<?> target, ServerVersion version, FMemberPredicate<Class> predicate) throws NoSuchElementException {
    super(target, Class.class, version, predicate);
  }

  protected ClassHandle(Class handle, ServerVersion version) {
    super(handle, Class.class, version);
  }

  /**
   * Checks whether an object is an instance of this class
   * @param o Object to check
   */
  public boolean isInstance(Object o) {
    return handle.isInstance(o);
  }

  /**
   * Interpret this class as an enumeration and get a handle to it
   * @throws IllegalStateException Thrown if this class is not an enumeration
   */
  public EnumHandle asEnum() throws IllegalStateException {
    EnumHandle enumHandle = enumerations.get(handle);

    // Use cached value
    if (enumHandle != null)
      return enumHandle;

    // Create a new enum handle on this class
    enumHandle = new EnumHandle(handle, version);

    // Store in cache and return
    enumerations.put(handle, enumHandle);
    return enumHandle;
  }

  /**
   * Create a new FieldHandle builder which will query this class
   */
  public FieldPredicateBuilder locateField() {
    return new FieldPredicateBuilder(this, version);
  }

  /**
   * Create a new MethodHandle builder which will query this class
   */
  public MethodPredicateBuilder locateMethod() {
    return new MethodPredicateBuilder(this, version);
  }

  /**
   * Create a new ConstructorHandle builder which will query this class
   */
  public ConstructorPredicateBuilder locateConstructor() {
    return new ConstructorPredicateBuilder(this, version);
  }

  /**
   * Create a new ClassHandle builder which will query this class
   */
  public ClassPredicateBuilder locateClass() {
    return new ClassPredicateBuilder(this, version);
  }

  @Override
  protected String stringify(Class member) {
    StringJoiner sj = new StringJoiner(" ");

    sj.add(Modifier.toString(member.getModifiers()));
    sj.add(member.isInterface() ? "interface" : "class");
    sj.add(member.getName());

    return sj.toString();
  }

  /**
   * Create a new class handle on top of a vanilla class
   * @param c Target class
   * @param version Current server version
   */
  public static ClassHandle of(Class<?> c, ServerVersion version) {
    ClassHandle handle = encapsulations.get(c);

    // Create new instance
    if (handle == null) {
      handle = new ClassHandle(c, version);
      encapsulations.put(c, handle);
      return handle;
    }

    // Return existing instance
    return handle;
  }
}
