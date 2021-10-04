package org.github.forax.framework.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Locale;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InterceptorRegistryTest {
  /*
  @Nested
  public class Q1 {

    @Retention(RUNTIME)
    @Target(METHOD)
    private @interface Intercepted { }

    @Test @Tag("Q1")
    public void createProxy() {
      interface Hello {
        @Intercepted String foobar();
        @Intercepted String hello(String message, int value);
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> method.getName());
      var proxy = registry.createProxy(Hello.class, null);
      assertAll(
          () -> assertEquals("foobar", proxy.foobar()),
          () -> assertEquals("hello", proxy.hello("a message", 42))
      );
    }

    @Test @Tag("Q1")
    public void createProxySignature() {
      interface Foo {
        @Intercepted String bar(String message, int value);
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> {
        assertEquals(Foo.class.getMethod("bar", String.class, int.class), method);
        assertEquals(List.of("hello", 42), List.of(args));
        return method.getName();
      });
      Foo foo = registry.createProxy(Foo.class, null);
      assertEquals("bar", foo.bar("hello", 42));
    }

    @Test  @Tag("Q1")
    public void addInterceptorPreconditions() {
      Interceptor interceptor = (method, proxy, args, proceed) -> null;
      var registry = new InterceptorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.addInterceptor(null, interceptor)),
          () -> assertThrows(NullPointerException.class, () -> registry.addInterceptor(Intercepted.class, null))
      );
    }

    @Test @Tag("Q1")
    public void createProxyPreconditions() {
      var registry = new InterceptorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.createProxy(null, null)),
          () -> assertThrows(IllegalArgumentException.class, () -> registry.createProxy(String.class, null))
      );
    }

  } // end Q1


  @Nested
  public class Q2 {

    @Retention(RUNTIME)
    @Target(METHOD)
    private @interface Intercepted { }

    @Test @Tag("Q2")
    public void addAndFindInterceptors() throws NoSuchMethodException {
      interface Foo {
        @Intercepted void bar();
      }

      Interceptor interceptor = (method, proxy, args, proceed) -> null;
      var bar = Foo.class.getMethod("bar");
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      assertEquals(List.of(interceptor), registry.findInterceptors(bar).toList());
    }

    @Test @Tag("Q2")
    public void addAndFindInterceptorsNoAnnotation() throws NoSuchMethodException {
      interface Foo {
        // no annotation
        String bar();
      }

      Interceptor interceptor = (method, proxy, args, proceed) -> null;
      var bar = Foo.class.getMethod("bar");
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      assertEquals(List.of(), registry.findInterceptors(bar).toList());
    }

    @Test @Tag("Q2")
    public void addAndFindInterceptorsNoInterceptor() throws NoSuchMethodException {
      interface Foo {
        @Intercepted
        void bar(int value);
      }

      var bar = Foo.class.getMethod("bar", int.class);
      var registry = new InterceptorRegistry();
      assertEquals(List.of(), registry.findInterceptors(bar).toList());
    }

  } // end Q2


  @Nested
  public class Q3 {

    @Retention(RUNTIME)
    @Target(METHOD)
    private @interface Intercepted { }

    @Test @Tag("Q3")
    public void createProxySimple() {
      interface Foo {
        int bar();
      }
      record FooImpl() implements Foo {
        @Override
        public int bar() {
          return 42;
        }
      }

      var delegate = new FooImpl();
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, delegate);
      assertEquals(42, proxy.bar());
    }

    @Retention(RUNTIME)
    @interface BarAnn {}

    @Retention(RUNTIME)
    @interface WhizzAnn {}

    @Test
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
      registry.addInterceptor(BarAnn.class, (method, proxy, args, proceed) -> 404);
      registry.addInterceptor(WhizzAnn.class, (method, proxy, args, proceed) -> "*" + proceed.call() + "*");
      Foo foo = registry.createProxy(Foo.class, new FooImpl());
      assertAll(
          () -> assertEquals(404, foo.bar()),
          () -> assertEquals("*HELLO*", foo.whizz("hello"))
      );
    }

    @Test @Tag("Q3")
    public void createProxyNoDelegateDefaultValues() {
      interface Foo {
        void methodVoid();
        boolean methodBoolean();
        byte methodByte();
        short methodShort();
        char methodChar();
        int methodInt();
        float methodFloat();
        long methodLong();
        double methodDouble();
        Object methodObject();
      }
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, null);
      assertAll(
          () -> registry.createProxy(Foo.class, null).methodVoid(),
          () -> assertFalse(proxy.methodBoolean()),
          () -> assertEquals((byte) 0, proxy.methodByte()),
          () -> assertEquals((short) 0, proxy.methodShort()),
          () -> assertEquals('\0', proxy.methodChar()),
          () -> assertEquals(0, proxy.methodInt()),
          () -> assertEquals(.0f, proxy.methodFloat()),
          () -> assertEquals(0L, proxy.methodLong()),
          () -> assertEquals(.0, proxy.methodDouble()),
          () -> assertNull(proxy.methodObject())
      );
    }

    @Test @Tag("Q3")
    public void createProxyDelegateNotCalled() {
      interface Foo {
        @Intercepted
        int bar();
      }
      record FooImpl() implements Foo {
        @Override
        public int bar() {
          return 101;
        }
      }

      var delegate = new FooImpl();
      Interceptor interceptor = (method, proxy, args, proceed) -> 42;
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      var proxy = registry.createProxy(Foo.class, delegate);
      assertEquals(42, proxy.bar());
    }

    @Test @Tag("Q3")
    public void createProxyWithAnInterceptorGetsTheRightMethod() throws Exception {
      interface Foo {
        @Intercepted
        int bar();
      }
      var bar = Foo.class.getMethod("bar");
      Interceptor interceptor = (method, proxy, args, proceed) -> {
        assertEquals(bar, method);
        return 99;
      };
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      var proxy = registry.createProxy(Foo.class, null);
      assertEquals(99, proxy.bar());
    }

    @Test @Tag("Q3")
    public void createProxyWithAnInterceptorThatPropagateCalls() {
      interface Foo {
        @Intercepted
        int bar(int multiplier);
      }
      record FooImpl() implements Foo {
        @Override
        public int bar(int multiplier) {
          return 101;
        }
      }

      var delegate = new FooImpl();
      Interceptor interceptor = (method, proxy, args, proceed) -> (Integer) args[0] * (Integer) proceed.call();
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      var proxy = registry.createProxy(Foo.class, delegate);
      assertEquals(202, proxy.bar(2));
    }

    @Test @Tag("Q3")
    public void createProxyUncheckedExceptionPropagation() {
      interface Foo {
        void bar();
      }
      record FooImpl() implements Foo {
        @Override
        public void bar() {
          throw new RuntimeException("bar !");
        }
      }

      var delegate = new FooImpl();
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, delegate);
      assertThrows(RuntimeException.class, proxy::bar);
    }

    @Test @Tag("Q3")
    public void createProxyCheckedExceptionPropagation() {
      interface Foo {
        void bar() throws IOException;
      }
      record FooImpl() implements Foo {
        @Override
        public void bar() throws IOException {
          throw new IOException();
        }
      }

      var delegate = new FooImpl();
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, delegate);
      assertThrows(IOException.class, proxy::bar);
    }

    @Test @Tag("Q3")
    public void createProxyErrorPropagation() {
      interface Foo {
        void bar();
      }
      record FooImpl() implements Foo {
        @Override
        public void bar() {
          throw new IOError(new IOException());
        }
      }

      var delegate = new FooImpl();
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, delegate);
      assertThrows(IOError.class, proxy::bar);
    }

    @Test @Tag("Q3")
    public void createProxyThrowablePropagation() {
      interface Foo {
        void bar() throws Throwable;
      }
      record FooImpl() implements Foo {
        @Override
        public void bar() throws Throwable{
          throw new Throwable();
        }
      }

      var delegate = new FooImpl();
      var registry = new InterceptorRegistry();
      var proxy = registry.createProxy(Foo.class, delegate);
      assertThrows(Throwable.class, proxy::bar);
    }

    @Test @Tag("Q3")
    public void createProxyExample() {
      interface Service {
        @Intercepted
        String hello(String message);
      }
      record ServiceImpl() implements Service {
        @Override
        public String hello(String message) {
          return "hello " + message;
        }
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> {
        System.out.println("enter " + method);
        try {
          return proceed.call();
        } finally {
          System.out.println("exit " + method);
        }
      });

      var delegate = new ServiceImpl();
      var proxy = registry.createProxy(Service.class, delegate);
      System.out.println(proxy.hello("interceptor"));
    }

  }  // end Q3



  @Nested
  public class Q4 {

    @Retention(RUNTIME)
    @interface Intercepted {}

    @Test @Tag("Q4")
    public void severalInterceptorOnTheSameAnnotation() {
      interface Foo {
        @Intercepted
        String bar(String s);
      }
      record FooImpl() implements Foo {
        @Override
        public String bar(String s) {
          return s;
        }
      }

      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> "_" + proceed.call() + "_");
      registry.addInterceptor(Intercepted.class, (method, proxy, args, proceed) -> "*" + proceed.call() + "*");
      Foo foo = registry.createProxy(Foo.class, new FooImpl());
      assertEquals("_*hello*_", foo.bar("hello"));
    }

    @Test @Tag("Q4")
    public void createProxyWithTwoInterceptorsThatPropagateCalls() {
      interface Foo {
        @Intercepted
        int bar(int adder);
      }
      record FooImpl() implements Foo {
        @Override
        public int bar(int adder) {
          return 51;
        }
      }

      var delegate = new FooImpl();
      Interceptor interceptor = (method, proxy, args, proceed) -> (Integer) args[0] + (Integer) proceed.call();
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Intercepted.class, interceptor);
      registry.addInterceptor(Intercepted.class, interceptor);
      var proxy = registry.createProxy(Foo.class, delegate);
      assertEquals(57, proxy.bar(3));
    }

    @Retention(RUNTIME)
    @interface Example1 {}

    @Retention(RUNTIME)
    @interface Example2 {}

    @Test @Tag("Q4")
    public void proxiesSharingTheSameInterface() {
      interface Foo {
        @Example1 @Example2
        String hello(String message);
      }
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Example1.class, (method, proxy, args, proceed) -> proceed.call() + " !!");
      registry.addInterceptor(Example2.class, (method, proxy, args, proceed) -> proceed.call() + " $$");
      var foo = registry.createProxy(Foo.class, message -> message);
      assertEquals("helllo $$ !!", foo.hello("helllo"));
    }

  }  // end Q4


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
      registry.addInterceptor(Example1.class, (method, proxy, args, proceed) -> "1" + proceed.call());
      registry.addInterceptor(Example2.class, (method, proxy, args, proceed) -> "2" + proceed.call());
      registry.addInterceptor(Example3.class, (method, proxy, args, proceed) -> "3" + proceed.call());
      var foo = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("123", foo.hello(""));
    }

    @Test @Tag("Q7")
    public void cacheCorrectlyInvalidated() {
      interface Foo {
        @Example1
        default String hello(String message) {
          return message;
        }
      }
      var registry = new InterceptorRegistry();
      registry.addInterceptor(Example1.class, (method, proxy, args, proceed) -> "1" + proceed.call());
      var proxy1 = registry.createProxy(Foo.class, new Foo(){});
      proxy1.hello("");  // interceptor list is cached

      registry.addInterceptor(Example1.class, (method, proxy, args, proceed) -> "2" + proceed.call());
      var proxy2 = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("12", proxy2.hello(""));
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
      registry.addInterceptor(Example1.class, (method, proxy, args, proceed) -> "-" + proceed.call() + "-");
      var foo = registry.createProxy(Foo.class, new Foo() {});
      assertEquals("-hello-", foo.hello("hello"));
    }

  }  // end Q7
  */
}