package me.blvckbytes.bbreflect;

import java.util.List;

public class EnumHandle extends ClassHandle {

  // TODO: Check against enum copies if they match the number of entries, else throw
  // TODO: Get an "enum" from static constants of a specific type within a class

  private final List<Enum<?>> e;

  /**
   * Create a new enumeration handle on top of a enumeration class
   * @param c Class which represents an enumeration
   * @throws IllegalStateException Thrown if the provided class is not an enumeration
   */
  public EnumHandle(Class<?> c) throws IllegalStateException {
    super(c);

    Object[] constants = c.getEnumConstants();

    // The provided class hasn't been of an enumeration type
    if (constants == null)
      throw new IllegalStateException("This class does not represent an enumeration.");

    // Create a unmodifiable list of constants and wrap into a handle
    e = List.of((Enum<?>[]) constants);
  }

  /**
   * Get an enumeration constant by it's ordinal integer
   * @param ordinal Ordinal integer
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  @SuppressWarnings("unchecked")
  public Enum<?> getByOrdinal(int ordinal) throws EnumConstantNotPresentException {
    try {
      return e.get(ordinal);
    } catch (Exception e) {
      throw new EnumConstantNotPresentException((Class<? extends Enum<?>>) handle, "ordinal=" + ordinal);
    }
  }

  /**
   * Get an enumeration constant by looking up the ordinal of a
   * copy enum which has it's constants sorted in the exact same order.
   * @param other Constant of a copy
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  public Enum<?> getByCopy(Enum<?> other) throws EnumConstantNotPresentException {
    return getByOrdinal(other.ordinal());
  }
}
