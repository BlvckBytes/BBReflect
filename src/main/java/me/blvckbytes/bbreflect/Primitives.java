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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Primitives {
  private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
  private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

  private Primitives() {
  }

  private static void add(Map<Class<?>, Class<?>> forward, Map<Class<?>, Class<?>> backward, Class<?> key, Class<?> value) {
    forward.put(key, value);
    backward.put(value, key);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(Class<T> type) {
    Class<T> wrapped = (Class<T>) PRIMITIVE_TO_WRAPPER_TYPE.get(type);
    return wrapped == null ? type : wrapped;
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> unwrap(Class<T> type) {
    Class<T> unwrapped = (Class<T>) WRAPPER_TO_PRIMITIVE_TYPE.get(type);
    return unwrapped == null ? type : unwrapped;
  }

  static {
    Map<Class<?>, Class<?>> primToWrap = new HashMap<>(16);
    Map<Class<?>, Class<?>> wrapToPrim = new HashMap<>(16);
    add(primToWrap, wrapToPrim, Boolean.TYPE, Boolean.class);
    add(primToWrap, wrapToPrim, Byte.TYPE, Byte.class);
    add(primToWrap, wrapToPrim, Character.TYPE, Character.class);
    add(primToWrap, wrapToPrim, Double.TYPE, Double.class);
    add(primToWrap, wrapToPrim, Float.TYPE, Float.class);
    add(primToWrap, wrapToPrim, Integer.TYPE, Integer.class);
    add(primToWrap, wrapToPrim, Long.TYPE, Long.class);
    add(primToWrap, wrapToPrim, Short.TYPE, Short.class);
    add(primToWrap, wrapToPrim, Void.TYPE, Void.class);
    PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
    WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(wrapToPrim);
  }
}