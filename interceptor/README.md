# Interceptor

An interceptor is a function that is called before/after a method call to react to the arguments or return value
of that method call. To select which interceptor will be run on which method call, we register
an interceptor with an annotation class and all methods annotated with an annotation of that class will be
intercepted by this interceptor.

## Advice and interceptor

There are more or less two different kind of API to intercept a method call.
- the around advice, an interface with two methods, `before` and `after`that are respectively called
  before and after a call.
  ```java
  public interface AroundAdvice {
    void before(Object instance, Method method, Object[] args) throws Throwable;
    void after(Object instance, Method method, Object[] args, Object result) throws Throwable;
  }
  ```
  The `instance` is the object on which the method is be called, `method` is the method called,
  `args` are the arguments of the call (or `null` is there is no argument).
  The last parameter of the method `after`, `result` is the returned value of the method call.

- one single method that takes as last parameter a way to call the next interceptor
  ```java
  @FunctionalInterface
  public interface Interceptor {
    Object intercept(Method method, Object proxy, Object[] args, Invocation invocation) throws Throwable;
  }
  ```
  with `Invocation` a functional interface corresponding to the next interceptor i.e. an interface
  with an abstract method bound to a specific interceptor (partially applied if you prefer).
  ```java
  @FunctionalInterface
  public interface Invocation {
    Object proceed(Object instance, Method method, Object[] args) throws Throwable;
  }
  ```

The interceptor API is more powerful and can be used to simulate the around advice API.


## Interceptors and/or Aspect Oriented Programming

The interface we are implementing here, is very similar to
[Spring method interceptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/aopalliance/intercept/MethodInterceptor.html),
[CDI interceptor](https://docs.oracle.com/javaee/6/tutorial/doc/gkhjx.html) or
[Guice interceptor](https://www.baeldung.com/guice).

All of them are using the same API provided by the
[Aspect Oriented Programming Alliance](http://aopalliance.sourceforge.net/)
which is a group created to define a common API for interceptors in Java.
Compared to the API we are about to implement, the AOP Alliance API encapsulates the parameters
(instance, method, args, link to the next interceptor) inside the interface `MethodInvocation`.

[Aspect Oriented Programming, AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming) is a more general
conceptual framework from the beginning of 2000s, an interceptor is equivalent to the around advice.


## An example

The API works in two steps, first register an advice (or an interceptor) for an annotation,
then creates a proxy of an interface. When a method of the proxy is called through the interface,
if the method is annotated, the corresponding advices/interceptors will be called. 

For example, if we want to implement an advice that will check that the arguments of a method are not null.
First we need to define an annotation

```java
@Retention(RUNTIME)
@Target(METHOD)
@interface CheckNotNull { }
```

If we want to check the argument of a method of an interface, we need to annotate it with `@CheckNotNull`
```java
interface Hello {
  @CheckNotNull String say(String message, String name);
}
```

We also have an implementation of that interface, that provides the behavior the user want
```java
class HelloImpl implements Hello {
   @Override
   public String say(String message, String name) {
      return message + " " + name;
   }
}
```

Step 1, we create an interceptor registry and add an around advice that checks that the arguments are not null
```java
    var registry = new InterceptorRegistry();
    registry.addAroundAdvice(CheckNotNull.class, new AroundAdvice() {
      @Override
      public void before(Object delegate, Method method, Object[] args) {
        Arrays.stream(args).forEach(Objects::requireNonNull);
      }

      @Override
      public void after(Object delegate, Method method, Object[] args, Object result) {}
    });
```

Step 2, we create a proxy in between the interface and the implementation 
```java
    var proxy = registry.createProxy(Hello.class, hello);

    assertAll(
        () -> assertEquals("hello around advice", proxy.say("hello", "around advice")),
        () -> assertThrows(NullPointerException.class, () -> proxy.say("hello", null))
        );
```

We can test the proxy with several arguments, null or not
```java
    assertAll(
        () -> assertEquals("hello around advice", proxy.say("hello", "around advice")),
        () -> assertThrows(NullPointerException.class, () -> proxy.say("hello", null))
        );
```


## The interceptor registry

An `InterceptorRegistry` is a class that manage the interceptors, it defines three public methods
- `addAroundAdvice(annotationClass, aroundAdvice)` register an around advice for an annotation
- `addInterceptor(annotationClass, interceptor)` register an interceptor for an annotation
- `createProxy(interfaceType, instance)` create a proxy that for each annotated methods will call
   the advices/interceptors before calling the method on the instance.



## Let's implement it

The idea is to gradually implement the class `InterceptorRegistry`, first by implementing the support
for around advice then add the support of interceptor and retrofit around advices to be implemented
as interceptors. To finish, add a cache avoiding recomputing of the linked list of invocations
at each call.


1. Create a class `InterceptorRegistry` with two public methods
   - a method `addAroundAdvice(annotationClass, aroundAdvice)` that for now do not care about the
   `annotationClass` and store the advice in a field.
   - a method `createProxy(type, delegate)` that creates a [dynamic proxy](../COMPANION.md#dynamic-proxy)
     implementing the interface and calls the method `before` and `after` of the around advice
     (if one is defined) around the call of each method using `Utils.invokeMethod()`.
   Check that the tests in the nested class "Q1" all pass.
   

2. Change the implementation of `addAroundAdvice` to store all advices by annotation class.
   And add a package private instance method `findAdvices(method)` that takes a `java.lang.reflect.Method` as
   parameter and returns a list of all advices that should be called.
   An around advice is called for a method if that method is annotated with an annotation of
   the annotation class on which the advice is registered.
   The idea is to gather [all annotations](../COMPANION.md#annotation) of that method
   and find all corresponding advices.
   Once the method `findAdvices` works, modify the method `createProxy`to use it.
   Check that the tests in the nested class "Q2" all pass.


3. We now want to be support the interceptor API, and for now we will implement it as an addon,
   without changing the support of the around advices.
   Add a method `addInterceptor(annotationClass, interceptor)` and a method
   `findInterceptors(method)` that respectively add an interceptor for an annotation class and
   returns a list of all interceptors to call for a method.
   Check that the tests in the nested class "Q3" all pass.


4. We want to add a method `getInvocation(interceptorList)` that takes a list of interceptors
   as parameter and returns an Invocation which when it is called will call the first interceptor
   with as last argument an Invocation allowing to call the second interceptor, etc.
   The last invocation will call the method on the instance with the arguments.
   Because each Invocation need to know the next Invocation, the chained list of Invocation
   need to be constructed from the last one to the first one.
   To loop over the interceptors in reverse order, you can use the method `List.reversed()`
   which return a reversed list without moving the elements of the initial list.
   Add the method `getInvocation`.
   Check that the tests in the nested class "Q4" all pass.


5. We know want to change the implementation to only uses interceptor internally
   and rewrite the method `addAroundAdvice` to use an interceptor that will calls
   the around advice.
   Change the implementation of `addAroundAdvice` to use an interceptor, and modify the
   code of `createProxy` to use interceptors instead of advices.
   Check that the tests in the nested class "Q5" all pass.
   Note: given that the method `findAdvices` is now useless, the test Q2.findAdvices() should be commented.

   
6. Add a cache avoiding recomputing a new `Invocation` each time a method is called.
   When the cache should be invalidated ? Change the code to invalidate the cache when necessary.
   Check that the tests in the nested class "Q6" all pass.


7. We currently support only annotations on methods, we want to be able to intercept methods if the annotation
   is not only declared on that method but on the declaring interface of that method or on one of the parameter
   of that method.
   Modify the method `findInterceptors(method)`.
   Check that the tests in the nested class "Q7" all pass.
