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

package me.blvckbytes.bbreflect.patching;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

public class UtfPatcherClassLoader extends ClassLoader {

  // FIXME: Is there a way to clear cache onDisable of the plugin?

  private final ConstantPoolUtfPatcher patcher;
  private final Function<String, Boolean> patchChecker;

  /**
   * Create a new class loader which will invoke an external utf patcher for every entry in
   * the constant pool of loaded classes which match on the passed path checker predicate
   * @param parent Parent class loader
   * @param patchChecker Path checker predicate which decides which class needs patching
   * @param utfPatcher Utf patcher function, invoked for every utf constant of every patched class
   */
  public UtfPatcherClassLoader(
    ClassLoader parent,
    Function<String, Boolean> patchChecker,
    Function<String, String> utfPatcher
  ) {
    super(parent);

    this.patcher = new ConstantPoolUtfPatcher(utfPatcher);
    this.patchChecker = patchChecker;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    try {
      String path = name.replace('.', '/') + ".class";

      // If this class doesn't need patching it's not handled and the request
      // is being bubbled up to the parent by the CNFE
      if (!this.patchChecker.apply(normalizeName(name)))
        throw new ClassNotFoundException();

      // Try to load the requested resource at the requested path
      URL resource = getResource(path);
      if (resource == null)
        throw new ClassNotFoundException();

      try (
        InputStream is = resource.openStream();
        DataInputStream input = new DataInputStream(is);
        ByteArrayOutputStream output = new ByteArrayOutputStream()
      ) {
        // Run the class' byte-code through the patcher before passing it
        // to the byte-code reader to be turned into an actual class
        this.patcher.patch(input, output);
        byte[] bytes = output.toByteArray();
        return defineClass(name, bytes, 0, bytes.length);
      }
    }

    // Don't add unnecessary layers to a CNFE
    catch (ClassNotFoundException e) {
      throw e;
    }

    // Transform internal exceptions into CNFE with a suppressed inner exception
    catch (Throwable t) {
      t.printStackTrace();
      ClassNotFoundException exception = new ClassNotFoundException("Internal error while loading class " + name);
      exception.addSuppressed(t);
      throw exception;
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);

    if (c != null) {
      if (resolve)
        resolveClass(c);
      return c;
    }

    try {
      c = findClass(name);
      if (resolve)
        resolveClass(c);
      return c;
    } catch (Throwable t) {
      try {
        return super.loadClass(name, resolve);
      } catch (ClassNotFoundException cnfe) {
        cnfe.addSuppressed(t);
        throw cnfe;
      }
    }
  }

  /**
   * Normalizes a name to only contain the root class, which means
   * member class notation is being stripped off
   */
  private String normalizeName(String name) {
    // If there's a member in the target class, I of course also want it patched.
    int firstIndex = name.indexOf('$');

    if (firstIndex < 0)
      return name;

    return name.substring(0, firstIndex);
  }
}