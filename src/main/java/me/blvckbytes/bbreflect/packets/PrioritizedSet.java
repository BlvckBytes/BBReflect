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

package me.blvckbytes.bbreflect.packets;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PrioritizedSet<T> implements Iterable<T> {

  private final Map<T, EPriority> items;
  private final Map<T, EPriority> sortedItems;

  public PrioritizedSet() {
    this.items = new HashMap<>();
    this.sortedItems = new LinkedHashMap<>();
  }

  public void add(T item, EPriority priority) {
    synchronized (this.sortedItems) {
      this.items.put(item, priority);
      this.updateSortedItems();
    }
  }

  public void remove(T item) {
    synchronized (this.sortedItems) {
      this.items.remove(item);
    }
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    synchronized (this.sortedItems) {
      return this.sortedItems.keySet().iterator();
    }
  }

  private void updateSortedItems() {
    synchronized (this.sortedItems) {
      this.sortedItems.clear();

      List<Map.Entry<T, EPriority>> list = new ArrayList<>(this.items.entrySet());
      list.sort(Map.Entry.comparingByValue());

      for (Map.Entry<T, EPriority> entry : list)
        this.sortedItems.put(entry.getKey(), entry.getValue());
    }
  }
}
