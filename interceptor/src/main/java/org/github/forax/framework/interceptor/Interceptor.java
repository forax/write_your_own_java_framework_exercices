package org.github.forax.framework.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface Interceptor {
  Object intercept(Method method, Object proxy, Object[] args, Callable<?> proceed) throws Exception;
}