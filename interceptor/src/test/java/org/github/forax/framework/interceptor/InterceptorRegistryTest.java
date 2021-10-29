package org.github.forax.framework.interceptor;

import org.github.forax.framework.interceptor.InterceptorRegistryTest.Q7.Example1;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InterceptorRegistryTest {
  /*
  @Nested
  public class Q1 {

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface CheckNotNull { }

    @Test @Tag("Q1")
    public void createProxy() {
      interface Hello {
        @CheckNotNull String say(String message, String name);
      }

      class HelloImpl implements Hello {
        @Override
        public String say(String message, String name) {
          return message + " " + name;
        }
      }
      var hello = new HelloImpl();

      var registry = new InterceptorRegistry();
      registry.addAroundAdvice(CheckNotNull.class, new AroundAdvice() {
        @Override
        public void before(Object delegate, Method method, Object[] args) {
          Arrays.stream(args).forEach(Objects::requireNonNull);
        }

        @Override
        public void after(Object delegate, Method method, Object[] args, Object result) {}
      });
      var proxy = registry.createProxy(Hello.class, hello);
      assertAll(
          () -> assertEquals("hello around advice", proxy.say("hello", "around advice")),
          () -> assertThrows(NullPointerException.class, () -> proxy.say("hello", null))
      );
    }

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Tagged { }

    @Test @Tag("Q1")
    public void checkArguments() throws NoSuchMethodException {
      interface Adder {
        @Tagged int add(int a, int b);
      }

      Adder sum = Integer::sum;
      Method add = Adder.class.getMethod("add", int.class, int.class);

      var registry = new InterceptorRegistry();
      registry.addAroundAdvice(Tagged.class, new AroundAdvice() {
        @Override
        public void before(Object delegate, Method method, Object[] args) {
          assertAll(
              () -> assertEquals(sum, delegate),
              () -> assertEquals(add, method),
              () -> assertEquals(List.of(2, 3), List.of(args))
          );
        }

        @Override
        public void after(Object delegate, Method method, Object[] args, Object result) {
          assertAll(
              () -> assertEquals(sum, delegate),
              () -> assertEquals(add, method),
              () -> assertEquals(List.of(2, 3), List.of(args)),
              () -> assertEquals(5, result)
          );
        }
      });
      var adder = registry.createProxy(Adder.class, sum);
      assertEquals(5, adder.add(2, 3));
    }

    @Test  @Tag("Q1")
    public void addAroundAdvicePreconditions() {
      var advice = new AroundAdvice() {
        @Override
        public void before(Object delegate, Method method, Object[] args) {}
        @Override
        public void after(Object delegate, Method method, Object[] args, Object result) {}
      };
      var registry = new InterceptorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.addAroundAdvice(null, advice)),
          () -> assertThrows(NullPointerException.class, () -> registry.addAroundAdvice(CheckNotNull.class, null))
      );
    }

    @Test @Tag("Q1")
    public void createProxyPreconditions() {
      var registry = new InterceptorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.createProxy(null, 3)),
          () -> assertThrows(NullPointerException.class, () -> registry.createProxy(Runnable.class, null)),
          () -> assertThrows(IllegalArgumentException.class, () -> registry.createProxy(String.class, "foo"))
      );
    }
  } // end Q1


  @Nested
  public class Q2 {
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Tagged1 { }

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Tagged2 { }

//    @Test @Tag("Q2")
//    public void findAdvices() throws NoSuchMethodException {
//      class EmptyAroundAdvice implements AroundAdvice {
//        @Override
//        public void pre(Object instance, Method method, Object[] args) {}
//        @Override
//        public void post(Object instance, Method method, Object[] args, Object result) {}
//      }
//
//      var registry = new InterceptorRegistry();
//      var advice1 = new EmptyAroundAdvice();
//      var advice2 = new EmptyAroundAdvice();
//      var advice3 = new EmptyAroundAdvice();
//      registry.addAroundAdvice(Tagged1.class, advice1);
//      registry.addAroundAdvice(Tagged1.class, advice2);
//      registry.addAroundAdvice(Tagged2.class, advice3);
//
//      interface Foo {
//        void method0();
//
//        @Tagged1
//        void method1();
//
//        @Tagged2
//        void method2();
//
//        @Tagged1 @Tagged2
//        void method3();
//      }
//
//      var method0 = Foo.class.getMethod("method0");
//      var method1 = Foo.class.getMethod("method1");
//      var method2 = Foo.class.getMethod("method2");
//      var method3 = Foo.class.getMethod("method3");
//      assertAll(
//          () -> assertEquals(List.of(), registry.findAdvices(method0)),
//          () -> assertEquals(List.of(advice1, advice2), registry.findAdvices(method1)),
//          () -> assertEquals(List.of(advice3), registry.findAdvices(method2)),
//          () -> assertEquals(List.of(advice1, advice2, advice3), registry.findAdvices(method3))
//      );
//    }

    @Test @Tag("Q2")
    public void withTwoAdvices() {
      record ModifyParameterAroundAdvice(int index, Object value) implements AroundAdvice {
        @Override
        public void before(Object instance, Method method, Object[] args) {
          args[index] = value;
        }
        @Override
        public void after(Object instance, Method method, Object[] args, Object result) {}
      }

      interface Hello {
        @Tagged1
        String say(String message, String name);
      }
      var hello = new Hello() {
        @Override
        public String say(String message, String name) {
          return message + " " + name;
        }
      };

      var registry = new InterceptorRegistry();
      registry.addAroundAdvice(Tagged1.class, new ModifyParameterAroundAdvice(0, "foo"));
      registry.addAroundAdvice(Tagged1.class, new ModifyParameterAroundAdvice(1, "bar"));
      var proxy = registry.createProxy(Hello.class, hello);
      assertEquals("foo bar", proxy.say("hello", "world"));
    }
  }  // end of Q2


  @Nested
  public class Q3 {
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Tagged1 { }

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Tagged2 { }

    @Test @Tag("Q3")
    public void findInterceptor() throws NoSuchMethodException {
      class EmptyInterceptor implements Interceptor {
        @Override
        public Object intercept(Object instance, Method method, Object[] args, Invocation invocation) {
          return null;
        }
      }

      var registry = new InterceptorRegistry();
      var interceptor1 = new EmptyInterceptor();
      var interceptor2 = new EmptyInterceptor();
      var interceptor3 = new EmptyInterceptor();
      registry.addInterceptor(Tagged1.class, interceptor1);
      registry.addInterceptor(Tagged1.class, interceptor2);
      registry.addInterceptor(Tagged2.class, interceptor3);

      interface Foo {
        // no annotation
        void method0();

        @Tagged1
        void method1();

        @Tagged2
        void method2();

        @Tagged1 @Tagged2
        void method3();
      }

      var method0 = Foo.class.getMethod("method0");
      var method1 = Foo.class.getMethod("method1");
      var method2 = Foo.class.getMethod("method2");
      var method3 = Foo.class.getMethod("method3");
      assertAll(
          () -> assertEquals(List.of(), registry.findInterceptors(method0)),
          () -> assertEquals(List.of(interceptor1, interceptor2), registry.findInterceptors(method1)),
          () -> assertEquals(List.of(interceptor3), registry.findInterceptors(method2)),
          () -> assertEquals(List.of(interceptor1, interceptor2, interceptor3), registry.findInterceptors(method3))
      );
    }
  }  // end of Q3


  @Nested
  public class Q4 {
    @Test @Tag("Q4")
    public void findInterceptorsNoInterceptor() throws Throwable {
      var invocation = InterceptorRegistry.getInvocation(List.of());
      class Empty {
        public static int identity(int x) {
          return x;
        }
      }
      var empty = new Empty();
      var method = Empty.class.getMethod("identity", int.class);

      assertEquals(42, invocation.proceed(empty, method, new Object[] { 42 }));
    }

    @Test @Tag("Q4")
    public void findInterceptorsStopInterceptor() throws Throwable {
      class Empty {
        public static int identity(int x) {
          return x;
        }
      }
      var empty = new Empty();
      var identity = Empty.class.getMethod("identity", int.class);

      class StopInterceptor implements Interceptor {
        @Override
        public Object intercept(Object instance, Method method, Object[] args, Invocation invocation) throws Exception {
          assertAll(
              () -> assertEquals(empty, instance),
              () -> assertEquals(identity, method),
              () -> assertEquals(List.of(42), List.of(args))
          );
          return 314;
        }
      }

      var interceptor = new StopInterceptor();
      var invocation = InterceptorRegistry.getInvocation(List.of(interceptor));
      assertEquals(314, invocation.proceed(empty, identity, new Object[] { 42 }));
    }

    @Test @Tag("Q4")
    public void findInterceptorsTwoInterceptors() throws Throwable {
      class Empty {
        public static int identity(int x) {
          return x;
        }
      }
      var empty = new Empty();
      var identity = Empty.class.getMethod("identity", int.class);

      class ChainInterceptor implements Interceptor {
        @Override
        public Object intercept(Object instance, Method method, Object[] args, Invocation invocation) throws Throwable {
          assertAll(
              () -> assertEquals(empty, instance),
              () -> assertEquals(identity, method),
              () -> assertEquals(List.of(42), List.of(args))
          );
          return invocation.proceed(instance, method, args);
        }
      }

      var interceptor1 = new ChainInterceptor();
      var interceptor2 = new ChainInterceptor();

      var invocation = InterceptorRegistry.getInvocation(List.of(interceptor1, interceptor2));

      assertEquals(42, invocation.proceed(empty, identity, new Object[] { 42 }));
    }
  }  // end of Q4


  @Nested
  public class Q5 {
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Data { }

    @Test @Tag("Q5")
    public void createProxyNoInterceptor() throws Exception {
      interface Empty {
        @Data
        int identity(int x);
      }
      var empty = new Empty() {
        @Override
        public int identity(int x) {
          return x;
        }
      };

      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Empty.class, empty);
      assertEquals(42, proxy.identity(42 ));
    }

    @Test @Tag("Q5")
    public void createProxyStopInterceptor() throws Exception {
      interface Empty {
        @Data
        int identity(int x);
      }
      var empty = new Empty() {
        @Override
        public int identity(int x) {
          return x;
        }
      };
      var identity = Empty.class.getMethod("identity", int.class);

      class StopInterceptor implements Interceptor {
        @Override
        public Object intercept(Object instance, Method method, Object[] args, Invocation invocation) throws Exception {
          System.err.println("instance = " + instance);
          assertAll(
              () -> assertEquals(empty, instance),
              () -> assertEquals(identity, method),
              () -> assertEquals(List.of(42), List.of(args))
          );
          return 314;
        }
      }

      var interceptor = new StopInterceptor();
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Data.class, interceptor);
      var proxy = registry.createProxy(Empty.class, empty);
      assertEquals(314, proxy.identity(42 ));
    }

    @Test @Tag("Q5")
    public void findInterceptorsTwoInterceptors() throws Exception {
      interface Empty {
        @Data
        int identity(int x);
      }
      var empty = new Empty() {
        @Override
        public int identity(int x) {
          return x;
        }
      };
      var identity = Empty.class.getMethod("identity", int.class);

      class ChainInterceptor implements Interceptor {
        @Override
        public Object intercept(Object instance, Method method, Object[] args, Invocation invocation) throws Throwable {
          assertAll(
              () -> assertEquals(empty, instance),
              () -> assertEquals(identity, method),
              () -> assertEquals(List.of(42), List.of(args))
          );
          return invocation.proceed(instance, method, args);
        }
      }

      var interceptor1 = new ChainInterceptor();
      var interceptor2 = new ChainInterceptor();
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Data.class, interceptor1);
      registry.addInterceptor(Data.class, interceptor2);
      var proxy = registry.createProxy(Empty.class, empty);
      assertEquals(42, proxy.identity(42 ));
    }

    @Retention(RUNTIME)
    @interface BarAnn {}

    @Retention(RUNTIME)
    @interface WhizzAnn {}

    @Test @Tag("Q5")
    public void createProxy() {
      interface Foo {
        @BarAnn
        int bar();

        @WhizzAnn
        String whizz(String s);
      }
      record FooImpl() implements Foo {
        @Override
        public int bar() {
          return 45;
        }

        @Override
        public String whizz(String s) {
          return s.toUpperCase(Locale.ROOT);
        }
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(BarAnn.class, (o, m, args, next) -> 404);
      registry.addInterceptor(WhizzAnn.class, (o, m, args, next) -> "*" + next.proceed(o, m, args) + "*");
      Foo foo = registry.createProxy(Foo.class, new FooImpl());
      assertAll(
          () -> assertEquals(404, foo.bar()),
          () -> assertEquals("*HELLO*", foo.whizz("hello"))
      );
    }

    @Test @Tag("Q5")
    public void createProxyExceptionsPropagation() {
      interface Foo {
        void unchecked();
        void checked() throws IOException;
        void error();
        void throwable() throws Throwable;
      }
      record FooImpl() implements Foo {
        @Override
        public void unchecked() {
          throw new RuntimeException("bar !");
        }
        @Override
        public void checked() throws IOException {
          throw new IOException();
        }
        @Override
        public void error() {
          throw new IOError(new IOException());
        }
        @Override
        public void throwable() throws Throwable {
          throw new Throwable();
        }
      }

      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, new FooImpl());
      assertAll(
          () -> assertThrows(RuntimeException.class, proxy::unchecked),
          () -> assertThrows(IOException.class, proxy::checked),
          () -> assertThrows(IOError.class, proxy::error),
          () -> assertThrows(Throwable.class, proxy::throwable)
      );
    }

    @Test @Tag("Q5")
    public void createProxyInterceptorExceptionsPropagation() {
      interface Foo {
        @Data default void unchecked() {}
        @Data default void checked() throws IOException {}
        @Data default void error() {}
        @Data default void throwable() throws Throwable {}
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(Data.class, (instance, method, args, invocation) -> {
        throw switch (method.getName()) {
          case "unchecked" -> new RuntimeException();
          case "checked" -> new IOException();
          case "error" -> new IOError(new IOException());
          case "throwable" -> new Throwable();
          default -> new AssertionError("fail !");
        };
      });
      var proxy = registry.createProxy(Foo.class, new Foo() {});
      assertAll(
          () -> assertThrows(RuntimeException.class, proxy::unchecked),
          () -> assertThrows(IOException.class, proxy::checked),
          () -> assertThrows(IOError.class, proxy::error),
          () -> assertThrows(Throwable.class, proxy::throwable)
      );
    }
  }  // end of Q5


  @Nested
  class Q6 {
    @Test @Tag("Q6")
    public void cacheCorrectlyInvalidated() {
      interface Foo {
        @Example1
        default String hello(String message) {
          return message;
        }
      }
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Example1.class, (o, m, args, next) -> "1" + next.proceed(o, m, args));
      var proxy1 = registry.createProxy(Foo.class, new Foo(){});
      proxy1.hello("");  // interceptor list is cached

      registry.addInterceptor(Example1.class, (o, m, args, next) -> "2" + next.proceed(o, m, args));
      var proxy2 = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("12", proxy2.hello(""));
    }
  }  // end of Q6


  @Nested
  public class Q7 {

    @Retention(RUNTIME)
    @interface Example1 {}

    @Retention(RUNTIME)
    @interface Example2 {}

    @Retention(RUNTIME)
    @interface Example3 {}

    @Test @Tag("Q7")
    public void annotationOnClassMethodAndParameters() {
      @Example1
      interface Foo {
        @Example2
        default String hello(@Example3 String message) {
          return message;
        }
      }
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Example1.class, (o, m, args, next) -> "1" + next.proceed(o, m, args));
      registry.addInterceptor(Example2.class, (o, m, args, next) -> "2" + next.proceed(o, m, args));
      registry.addInterceptor(Example3.class, (o, m, args, next) -> "3" + next.proceed(o, m, args));
      var foo = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("123", foo.hello(""));
    }

    @Test @Tag("Q7")
    public void annotationOnClassMethodAndParametersDoNotRepeat() {
      @Example1
      interface Foo {
        @Example1
        default String hello(@Example1 String message) {
          return message;
        }
      }
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Example1.class, (o, m, args, next) -> "-" + next.proceed(o, m, args) + "-");
      var foo = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("-hello-", foo.hello("hello"));
    }
  }  // end Q7

  */
}