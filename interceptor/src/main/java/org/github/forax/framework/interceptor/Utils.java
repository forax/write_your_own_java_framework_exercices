package org.github.forax.framework.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

final class Utils {
  private Utils() {
    throw new AssertionError();
  }

  public static Object invokeMethod(Object object, Method method, Object... args) {
    try {
      return method.invoke(object, args);
    } catch (IllegalArgumentException e) {
      throw new AssertionError("can not call " + method + " on " + object + " with " + Arrays.toString(args), e);
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError) new IllegalAccessError().initCause(e);
    } catch (InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }

  @SuppressWarnings("unchecked")   // very wrong but works
  private static <T extends Throwable> AssertionError rethrow(Throwable cause) throws T {
    throw (T) cause;
  }
}
