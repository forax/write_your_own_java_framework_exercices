package com.github.forax.framework.injector;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class Utils2 {
  private Utils2() {
    throw new AssertionError();
  }

  public static Enumeration<URL> getResources(String folderName, ClassLoader classLoader) {
    try {
      return classLoader.getResources(folderName);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Class<?> loadClass(String className, ClassLoader classLoader) {
    try {
      return Class.forName(className, /*initialize=*/ false, classLoader);
    } catch (ClassNotFoundException e) {
      throw (NoClassDefFoundError) new NoClassDefFoundError().initCause(e);
    }
  }
}
