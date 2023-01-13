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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public abstract class APredicateBuilder<T, B extends APredicateBuilder<T, B>> {

  protected ClassHandle targetClass;
  protected List<B> fallbacks;
  protected @Nullable ServerVersion minVersion, maxVersion;
  protected ServerVersion version;

  protected APredicateBuilder(ClassHandle targetClass, ServerVersion version) {
    this.targetClass = targetClass;
    this.version = version;
    this.fallbacks = new ArrayList<>();
  }

  /**
   * Set a version range on which this predicate is valid on
   * @param minVersion Minimum server version
   * @param maxVersion Maximum server version
   */
  @SuppressWarnings("unchecked")
  public B withVersionRange(@Nullable ServerVersion minVersion, @Nullable ServerVersion maxVersion) {
    if (minVersion != null && maxVersion != null && minVersion.compare(maxVersion) > 0)
      throw new IllegalArgumentException("Min version cannot be greater than max version");

    this.minVersion = minVersion;
    this.maxVersion = maxVersion;
    return (B) this;
  }

  /**
   * Get the predicate's result and return null if it couldn't be located
   */
  public @Nullable T optional() {
    try {
      return required();
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  /**
   * Specify another fallback builder instance to invoke when the current builder
   * couldn't be executed successfully
   * @param builder Fallback predicate
   */
  public abstract B orElse(Supplier<B> builder);

  protected boolean isInVersionRange() {
    return (maxVersion == null || version.compare(maxVersion) <= 0) && (minVersion == null || version.compare(minVersion) >= 0);
  }

  protected void checkVersionRange() throws NoSuchElementException {
    if (maxVersion != null && version.compare(maxVersion) > 0)
      throw new NoSuchElementException("This version is higher than the supported version");

    if (minVersion != null && version.compare(minVersion) < 0)
      throw new NoSuchElementException("This version is lower than the supported version");
  }

  /**
   * Get the predicate's result and require that it's not null
   * @throws NoSuchElementException Not found exception if the result could not be located
   */
  public abstract T required() throws NoSuchElementException;

  /**
   * Tries to invoke all available fallbacks and returns the result of the
   * first successful call to {@link #required()}
   * @throws NoSuchElementException Exception thrown by the last callback, on failure
   */
  protected T invokeFallbacks(NoSuchElementException firstException) throws NoSuchElementException {
    NoSuchElementException lastThrown = firstException;

    for (B fallback : fallbacks) {
      try {
        return fallback.required();
      } catch (NoSuchElementException e) {
        lastThrown = e;
      }
    }

    throw lastThrown;
  }
}
