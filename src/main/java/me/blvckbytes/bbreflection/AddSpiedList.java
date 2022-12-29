package me.blvckbytes.bbreflection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class AddSpiedList<T> implements List<T> {

  private final List<T> handle;
  private final Consumer<T> preAdd;

  @Override
  public int size() {
    return handle.size();
  }

  @Override
  public boolean isEmpty() {
    return handle.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return handle.contains(o);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return handle.iterator();
  }

  @Override
  public Object[] toArray() {
    return handle.toArray();
  }

  @NotNull
  public <T1> T1 @NotNull [] toArray(T1 @NotNull [] a) {
    return handle.toArray(a);
  }

  @Override
  public boolean add(T t) {
    this.preAdd.accept(t);
    return handle.add(t);
  }

  @Override
  public boolean remove(Object o) {
    return handle.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return new HashSet<>(handle).containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    throw new UnsupportedOperationException("Operation not supported on add-spied lists");
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    throw new UnsupportedOperationException("Operation not supported on add-spied lists");
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return handle.removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return handle.retainAll(c);
  }

  @Override
  public void clear() {
    handle.clear();
  }

  @Override
  public T get(int index) {
    return handle.get(index);
  }

  @Override
  public T set(int index, T element) {
    return handle.set(index, element);
  }

  @Override
  public void add(int index, T element) {
    this.preAdd.accept(element);
    handle.add(index, element);
  }

  @Override
  public T remove(int index) {
    return handle.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return handle.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return handle.lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator() {
    return handle.listIterator();
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator(int index) {
    return handle.listIterator(index);
  }

  @NotNull
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return handle.subList(fromIndex, toIndex);
  }
}
