# Interceptor

An interceptor is a function that is called before/after a method call to react to the arguments or return value
of that method call. To select which interceptor will be run on which method call, we register
an interceptor with an annotation class and all methods annotated with an annotation of that class will be
intercepted by this interceptor.

An interceptor is a functional interface that takes as parameter the intercepted method, the proxy implementing
the interface, the arguments of of the call and a `Callable` to call either the next interceptor or
the actual method.
```java
@FunctionalInterface
public interface Interceptor {
  Object intercept(Method method, Object proxy, Object[] args, Callable<?> proceed) throws Exception;
}
```

An `InterceptorRegistry` is a class that manage interceptors, it defines two public methods
- `addInterceptor(annotationClass, interceptor)` register an interceptor for an interceptor
- `createProxy(interfaceType, delegate)` create a proxy that for each annotated methods will call the interceptors
   before calling the method on the delegate, or return the default value if the delegate is null.


### An interceptor that logs method calls

By example, with an annotation `Intercepted` and an interface `Service` defined like this
```java
@Retention(RUNTIME)
@Target(METHOD)
private @interface Intercepted { }

interface Service {
  @Intercepted
  String hello(String message);
}
```

Then we can create an `InterceptorRegistry` and adds an interceptor that will be run each time a method
annotated with `@Intercepted` will be called

```java
var registry = new InterceptorRegistry();
registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> {
    System.out.println("enter " + method);
    try {
      return proceed.call();   // call the real method
    } finally {
      System.out.println("exit " + method);
    }
});
```

Now, we can ask the `InterceptorRegistry` to create a proxy on the interface `Service`

```java
record ServiceImpl() implements Service {
  @Override
  public String hello(String message) {
    return "hello " + message;
  }
}      
      
var delegate = new ServiceImpl();
var proxy = registry.createProxy(Service.class, delegate);
System.out.println(proxy.hello("interceptor"));
```

This code prints
```
enter public abstract java.lang.String Service.hello(java.lang.String)
exit public abstract java.lang.String Service.hello(java.lang.String)
hello interceptor
```


## Interceptors and Aspect Oriented Programming

The interface we are implementing here, is very similar to
[Spring interceptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/aopalliance/intercept/Interceptor.html),
[CDI interceptor](https://docs.oracle.com/javaee/6/tutorial/doc/gkhjx.html) or
[Guice interceptor](https://www.baeldung.com/guice).

All of them are using the same API provided by the
[Aspect Oriented Programming Alliance](http://aopalliance.sourceforge.net/)
which is a group created to define a common API for interceptors in Java.
Compared to the API we are about to implement, the AOP Alliance API encapsulates the parameters
(method, proxy, args, link to the next interceptor) inside the interface `InvocationContext`.

[Aspect Oriented Programming, AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming) is a more general
conceptual framework from the beginning of 2000s, the interceptors with annotations are what stay  from that experience.


## Discussion about the API

You can remark that when you call `proceed.calls` to call the 'real' method, we don't pass arguments,
the idea is that an interceptor should not interfere with the implementation but enhance the implementation,
that why an interceptor can not modify the arguments.

Not be able to modify the argument is important if you have several interceptors for the same method
because it means that the semantics is the same whatever the order of the interceptors.

You can notice that this API allows an interceptor to change with the return value, this is a known shortcoming
of this API. Don't change the return value in an interceptor.


## Let's implement it

The idea is to gradually implement the class `InterceptorRegistry`, first by dealing only with one interceptor always
called independently of if the annotation is present or not, then to add the code to manage the annotation classes.
Next, to allow several interceptors on the same annotation classes and at the end as an optimization to try to
cache the part of the computation that can be cached.


1. Create a class `InterceptorRegistry` with two public methods
   - a method `addInterceptor(annotationClass, interceptor)` that register into a `Map` the association between
     an annotation class and an interceptor
   - a method `createProxy(type, delegate)` that creates a [dynamic proxy](../COMPANION.md#dynamic-proxy)
     with all methods calling the interceptor using `java.lang.reflect.Proxy`.
   For now, the parameter `delegate` is ignored and the parameter `proceed` of the interceptor is always 'null'.
   Check that the tests in the nested class "Q1" all pass.
   

2. Add a package private instance method `findInterceptors(method)` that takes a `java.lang.reflect.Method` as
   parameter and returns a `java.util.stream.Stream` of all interceptors that should call be called to intercept
   that method. The idea is to gather [all annotations](../COMPANION.md#annotation) of that method
   and find all corresponding interceptors.
   Check that the tests in the nested class "Q2" all pass.


3. We now want to be support several interceptors, and a delegate.
   If the delegate is null, the return value will be  the default value of the return type otherwise
   it will be the result of a call to the method on the delegate.
   First, create a method `invokeDelegate(delegate, method, args)` that either return the default value or
   call the method on the delegate (see `Utils.defaultValue()` and `Utils.invokeMethod()`).
   
   Then we need to create a chain of `Callable` (the type of `proceed`),
   in such way that calling `proceeed.call()` will call the next interceptor (or the `delegate` if it's the
   last interceptor).
   For create, create first a method `getCallable(interceptors, method, proxy, args, delegate)`, that takes the
   interceptors to call (the ones from `findInterceptors`), the called method, the proxy , the arguments and the
   delegate and returns a `Callable` that once call will call the first interceptor with as `proceed` a Callable
   that will call the second interceptor,
   etc.
   ```java
   Callable<?> getCallable(Stream<Interceptor> interceptors, Method method, Object proxy, Object[] args, Object delegate)
   ```
   Note: because you need to create the `Callable`s from the last to the first, you can use `Utils.reverseList(list)`
   to inverse a list (without actually moving the elements).

   To finish, re-write `createProxy(type, delegate)` to call create a proxy that will call `findInterceptors(method)`
   to find the interceptors then creates the callables using `getCallable()` then call the first callbable.
   Check that the tests in the nested class "Q3" all pass.
   

4. We now want to be able to add several interceptors on the same annotation, for that modfiy the `Map` that stores
   the interceptors to store a `List<Interceptor>` and modify `addInterceptor(annotationClass, interceptor)`
   and `findInterceptors(method)` to support several interceptors on the same annotation.
   Check that the tests in the nested class "Q4" all pass.


5. You may have noticed that in the declaration of `getCallable()` above, the first two parameters have the same value
   for a method while the three others change (`proxy` and `args` change at each call and `delegate` change for each proxy). 
   In order to share the sequence of call between interceptor for a `Method`, declare a private functional interface
   named `Fun` that takes the last 3 parameters and write a method `getFun(interceptors, method)`
   comparable to  `getCallbable` that returns a `Fun` instead of a `Callable`.
   Then rewrite `createProxy()` to use `getFun()`.
   Check that all the previous tests pass.

   Note: you can implement `getFun` as a one giant expression using a Stream :)


6. We can now improve the implementation by caching the `Fun` per `Method` by using a `Map<Method, Fun>`.
   Modify `createProxy()` to use the cache of `Fun`.
   Check that all the previous tests pass.


7. We currently support only annotations on methods, we want to be able to intercept method if the annotation
   is not only declared on the method but on the declaring interface or on one of the parameter.
   For that, modify the method `findInterceptors(method)`.
   Check that the tests in the nested class "Q7" all pass.
