package org.github.forax.framework.interceptor;

import java.lang.reflect.Method;

@FunctionalInterface
public interface Interceptor {
  Object intercept(Object instance, Method method, Object[] args, Invocation invocation) throws Throwable;
}