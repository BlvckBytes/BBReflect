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

package me.blvckbytes.bbreflect.handle.predicate;

import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class ClassPredicateBuilder extends APredicateBuilder<ClassHandle, ClassPredicateBuilder> {

  private @Nullable Boolean isPublic;
  private @Nullable Boolean isStatic;
  private @Nullable String name;
  private final List<String> skipNames;
  private int skip;

  /**
   * Create a new class predicate builder on a class handle
   * @param targetClass Class to search through
   * @param version Current server version
   */
  public ClassPredicateBuilder(ClassHandle targetClass, ServerVersion version) {
    super(targetClass, version);

    this.skipNames = new ArrayList<>();
  }

  //////////////////////////////////// Name /////////////////////////////////////

  /**
   * Define the target class' name
   * @param name Class name, null means wildcard
   */
  public ClassPredicateBuilder withName(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Define a list of class names to skip
   * @param names Class names to skip
   */
  public ClassPredicateBuilder skipNames(String... names) {
    this.skipNames.addAll(Arrays.asList(names));
    return this;
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target class's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  /**
   * Define the target class's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /////////////////////////////////// Skipping //////////////////////////////////

  /**
   * Define how many matches to skip
   * @param skip Number of matches to skip
   */
  public ClassPredicateBuilder withSkip(int skip) {
    this.skip = skip;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public ClassPredicateBuilder orElse(Supplier<ClassPredicateBuilder> builder) {
    fallbacks.add(builder.get());
    return this;
  }

  @Override
  public ClassHandle required() throws NoSuchElementException {
    try {
      checkVersionRange();

      return new ClassHandle(targetClass.getHandle(), version, (c, mc) -> {

        // Static modifier mismatch
        if (isStatic != null && Modifier.isStatic(c.getModifiers()) != isStatic)
          return false;

        // Public modifier mismatch
        if (isPublic != null && Modifier.isPublic(c.getModifiers()) != isPublic)
          return false;

        // Name mismatch
        if (this.name != null && !isClassNameEqualTo(c, name))
          return false;

        // Skip this name
        if (this.skipNames.stream().anyMatch(skip -> isClassNameEqualTo(c, skip)))
          return false;

        // Everything matches, while skip > matchCounter, count up
        if (skip > mc)
          return null;

        return true;
      });
    } catch (NoSuchElementException e) {
      return invokeFallbacks(e);
    }
  }

  private boolean isClassNameEqualTo(Class<?> c, String name) {
    String cName = c.getName();

    int lastDollar = cName.lastIndexOf('$');
    if (lastDollar >= 0)
      cName = cName.substring(lastDollar + 1);

    return cName.equals(name);
  }
}
