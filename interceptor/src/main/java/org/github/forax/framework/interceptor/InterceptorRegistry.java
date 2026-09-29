package org.github.forax.framework.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;

public final class InterceptorRegistry {
  //private final HashMap<Class<? extends Annotation>, List<AroundAdvice>> advicesMap;
  private final HashMap<Class<? extends Annotation>, List<Interceptor>> interceptorMap;
  private final HashMap<Method, Invocation> invocationCache;

  public InterceptorRegistry() {
      //advicesMap = new HashMap<>();
      interceptorMap = new HashMap<>();
      invocationCache = new HashMap<>();
      super();
  }

  public void addInterceptor(Class<? extends Annotation> annotationClass, Interceptor interceptor) {
      Objects.requireNonNull(interceptor);
      Objects.requireNonNull(annotationClass);
      interceptorMap.computeIfAbsent(annotationClass, _ -> new ArrayList<>()).add(interceptor);
      invocationCache.clear();
  }

    List<Interceptor> findInterceptors(Method method) {
        return Stream.of(
                Arrays.stream(method.getDeclaringClass().getAnnotations()),
                Arrays.stream(method.getAnnotations()),
                Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream))
        .flatMap(s -> s)
        .map(Annotation::annotationType)
        .distinct()
        .flatMap(annotationType -> interceptorMap.getOrDefault(annotationType, List.of()).stream())
        .toList();
    }

  public void addAroundAdvice(Class<? extends Annotation> annotationClass, AroundAdvice advice) {
    Objects.requireNonNull(annotationClass);
    Objects.requireNonNull(advice);
    addInterceptor(annotationClass, ((instance, method, args, invocation) -> {
        advice.before(instance, method, args);
        var result = invocation.proceed(instance, method, args);
        advice.after(instance, method, args, result);
        return result;
    }));
    //advicesMap.computeIfAbsent(annotationClass, _ -> new ArrayList<>()).add(advice);
  }

  /*
  List<AroundAdvice> findAdvices(Method method) {
      var results = new ArrayList<AroundAdvice>();
      for (var annotation : method.getAnnotations()) {
          results.addAll(advicesMap.getOrDefault(annotation.annotationType(),  Collections.emptyList()));
      }
      return Collections.unmodifiableList(results);
  }
*/

  public <T>  T createProxy(Class<T> type, T delegate) {
     Objects.requireNonNull(type);
     Objects.requireNonNull(delegate);
      return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
            new Class<?>[] { type },
              (Object _, Method method, Object[] args) -> {
//                var advices = findAdvices(method);
//                for (var advice : advices) {
//                    advice.before(delegate, method, args);
//                }
//                var result = method.invoke(delegate, args);
//                for (var advice : advices) {
//                    advice.after(delegate, method, args, result);
//                }
                  //return result;
                  var invocation = invocationCache.computeIfAbsent(method, m ->{
                      var interceptors = findInterceptors(m);
                      return getInvocation(interceptors);
                  });
                  return invocation.proceed(delegate, method, args);
           }));
  }

    public static Invocation getInvocation(List<Interceptor> interceptorList) {
        Objects.requireNonNull(interceptorList);
        Invocation invocation = Utils::invokeMethod;
        for (var interceptor : interceptorList.reversed()) {
            var next = invocation;
            invocation = (instance, method, args) -> interceptor.intercept(instance, method, args, next);
        }
        return invocation;
    }

}
