package me.blvckbytes.bbreflect;

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

  public ClassHandle(Class<?> target, FMemberPredicate<Class> predicate) throws NoSuchElementException {
    super(target, Class.class, predicate);
  }

  protected ClassHandle(Class handle) {
    super(handle, Class.class);
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
    enumHandle = new EnumHandle(handle);

    // Store in cache and return
    enumerations.put(handle, enumHandle);
    return enumHandle;
  }

  /**
   * Create a new FieldHandle builder which will query this class
   */
  public FieldPredicateBuilder locateField() {
    return new FieldPredicateBuilder(this);
  }

  /**
   * Create a new MethodHandle builder which will query this class
   */
  public MethodPredicateBuilder locateMethod() {
    return new MethodPredicateBuilder(this);
  }

  /**
   * Create a new ConstructorHandle builder which will query this class
   */
  public ConstructorPredicateBuilder locateConstructor() {
    return new ConstructorPredicateBuilder(this);
  }

  /**
   * Create a new ClassHandle builder which will query this class
   */
  public ClassPredicateBuilder locateClass() {
    return new ClassPredicateBuilder(this);
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
   */
  public static ClassHandle of(Class<?> c) {
    ClassHandle handle = encapsulations.get(c);

    // Create new instance
    if (handle == null) {
      handle = new ClassHandle(c);
      encapsulations.put(c, handle);
      return handle;
    }

    // Return existing instance
    return handle;
  }
}
