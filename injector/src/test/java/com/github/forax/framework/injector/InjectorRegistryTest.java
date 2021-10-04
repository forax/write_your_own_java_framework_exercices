package com.github.forax.framework.injector;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unused")
public class InjectorRegistryTest {
  /*
  @Nested
  public class Q1 {
    @Test @Tag("Q1")
    public void getRegistry() {
      var registry = new InjectorRegistry();
      assertNotNull(registry);
    }

    @Test @Tag("Q1")
    public void atInjectTargetMethodAndConstructorAndRetentionIsRuntime() {
      assertEquals(List.of(METHOD, CONSTRUCTOR), List.of(Inject.class.getAnnotation(Target.class).value()));
    }

    @Test @Tag("Q1")
    public void registerInstanceAndGetInstanceString() {
      var registry = new InjectorRegistry();
      registry.registerInstance(String.class, "hello");
      assertEquals("hello", registry.lookupInstance(String.class));
    }

    @Test @Tag("Q1")
    public void registerInstanceAndGetInstanceInteger() {
      var registry = new InjectorRegistry();
      registry.registerInstance(Integer.class, 42);
      assertEquals(42, registry.lookupInstance(Integer.class));
    }

    @Test @Tag("Q1")
    public void registerInstanceAndGetInstanceSameInstance() {
      record Person(String name) {}

      var registry = new InjectorRegistry();
      var bob = new Person("Bob");
      registry.registerInstance(Person.class, bob);
      assertSame(bob, registry.lookupInstance(Person.class));
    }

    @Test @Tag("Q1")
    public void registerInstanceAndGetInstanceWithAnInterface() {
      interface I {
        String hello();
      }
      class Impl implements I {
        @Override
        public String hello() {
          return "hello";
        }
      }

      var registry = new InjectorRegistry();
      var impl = new Impl();
      registry.registerInstance(I.class, impl);
      assertSame(impl, registry.lookupInstance(I.class));
    }

    @Test @Tag("Q1")
    public void registerInstancePreconditions() {
      var registry = new InjectorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.registerInstance(null, new Object())),
          () -> assertThrows(NullPointerException.class, () -> registry.registerInstance(Consumer.class, null))
      );
    }

    @Test @Tag("Q1")
    public void lookupInstancePreconditions() {
      var registry = new InjectorRegistry();
      assertThrows(NullPointerException.class, () -> registry.lookupInstance(null));
    }
  }


  @Nested
  public class Q2 {
    @Test @Tag("Q2")
    public void registerInstanceAndGetInstancePreciseSignature() {
      InjectorRegistry registry = new InjectorRegistry();
      registry.registerInstance(String.class, "hello");
      String instance = registry.lookupInstance(String.class);
      assertEquals("hello", instance);
    }
    //@Test @Tag("Q2")
    //public void shouldNotCompilePreciseSignature() {
    //  var registry = new Registry();
    //  registry.registerInstance(String.class, 3);
    //}
  }


  @Nested
  public class Q3 {
    @Test @Tag("Q3")
    public void registerProvider() {
      record Bar() {}

      var registry = new InjectorRegistry();
      registry.registerProvider(Bar.class, Bar::new);
      var instance1 = registry.lookupInstance(Bar.class);
      var instance2 = registry.lookupInstance(Bar.class);
      assertNotSame(instance1, instance2);
      assertEquals(instance1, instance2);
    }

    @Test @Tag("Q3")
    public void registerProviderWithAnInterface() {
      interface I {
        String hello();
      }
      record Impl() implements I {
        @Override
        public String hello() {
          return "hello";
        }
      }

      var registry = new InjectorRegistry();
      registry.registerProvider(I.class, Impl::new);
      var instance1 = registry.lookupInstance(I.class);
      var instance2 = registry.lookupInstance(I.class);
      assertNotSame(instance1, instance2);
      assertEquals(instance1, instance2);
    }

    @Test @Tag("Q3")
    public void registerProviderPreconditions() {
      var registry = new InjectorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.registerProvider(null, Object::new)),
          () -> assertThrows(NullPointerException.class, () -> registry.registerInstance(Consumer.class, null))
      );
    }
  }


  @Nested
  public class Q4 {
    public static class A {
      @Inject
      public void setValue(String value) {}
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesOneInjectMethod() {
      List<PropertyDescriptor> properties = InjectorRegistry.findInjectableProperties(A.class);
      assertAll(
          () -> assertEquals(1, properties.size()),
          () -> assertEquals(A.class.getMethod("setValue", String.class), properties.get(0).getWriteMethod())
      );
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesNoInjectMethod() {
      class B {
        // No @Inject
        public void setValue(String value) {}
      }

      var properties = InjectorRegistry.findInjectableProperties(B.class);
      assertEquals(List.of(), properties);
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesNoPublicMethod() {
      class C {
        @Inject
        private void setValue(String value) {}
      }

      var properties = InjectorRegistry.findInjectableProperties(C.class);
      assertEquals(List.of(), properties);
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesOneInjectAbstractMethod() {
      interface I {
        @Inject
        void setValue(String value);
      }
      var properties = InjectorRegistry.findInjectableProperties(I.class);
      assertAll(
          () -> assertEquals(1, properties.size()),
          () -> assertEquals(I.class.getMethod("setValue", String.class), properties.get(0).getWriteMethod())
      );
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesOneInjectDefaultMethod() {
      interface I {
        @Inject
        default void setValue(Integer value) {}
      }
      var properties = InjectorRegistry.findInjectableProperties(I.class);
      assertAll(
          () -> assertEquals(1, properties.size()),
          () -> assertEquals(I.class.getMethod("setValue", Integer.class), properties.get(0).getWriteMethod())
      );
    }

    public static class D {
      @Inject
      public void setValue1(Double value) {}
      @Inject
      public void setValue2(Double value) {}
    }

    @Test @Tag("Q4")
    public void findInjectablePropertiesTwoInjectMethod() throws NoSuchMethodException {
      var properties = InjectorRegistry.findInjectableProperties(D.class);
      var methods = Set.of(
          D.class.getMethod("setValue1", Double.class),
          D.class.getMethod("setValue2", Double.class)
      );
      assertAll(
          () -> assertEquals(2, properties.size()),
          () -> assertEquals(methods, properties.stream().map(PropertyDescriptor::getWriteMethod).collect(toSet()))
      );
    }
  }

  @Nested
  public class Q5 {
    public static class A {
      private String s;
      private int i;

      @Inject
      public void setString(String s) {
        this.s = s;
      }
      @Inject
      public void setInteger(Integer i) {
        this.i = i;
      }

      // No @Inject
      public void setAnotherInteger(Integer i) {
        fail();
      }
    }

    @Test @Tag("Q5")
    public void registerProviderClassWithSettersInjection() {
      var registry = new InjectorRegistry();
      registry.registerProviderClass(A.class, A.class);
      registry.registerInstance(String.class, "hello");
      registry.registerInstance(Integer.class, 42);
      var a = registry.lookupInstance(A.class);
      assertAll(
          () -> assertEquals("hello", a.s),
          () -> assertEquals(42, a.i)
      );
    }

    public static class B {
      private int value1;
      private int value2;

      @Inject
      public void setValue1(Integer value1) {
        this.value1 = value1;
      }
      @Inject
      public void setValue2(Integer value2) {
        this.value2 = value2;
      }
    }

    @Test @Tag("Q5")
    public void registerProviderClassWithSettersWithProviderInjection() {
      var counter = new Object() { int count; };
      var registry = new InjectorRegistry();
      registry.registerProviderClass(B.class, B.class);
      registry.registerProvider(Integer.class, () -> counter.count++);
      var b = registry.lookupInstance(B.class);
      assertTrue((b.value1 == 0 && b.value2 == 1) ||
          (b.value2 == 0 && b.value1 == 1));
    }

    public static class C {
      @Inject
      static void setValue(String s) {
        fail();
      }
    }

    @Test @Tag("Q5")
    public void registerProviderClassWithSettersStaticShouldBeIgnored() {
      var counter = new Object() { int count; };
      var registry = new InjectorRegistry();
      registry.registerInstance(String.class, "hello");
      registry.registerProviderClass(C.class, C.class);
      var c = registry.lookupInstance(C.class);
      assertNotNull(c);
    }

    public interface D { }
    public static class E implements D { }

    @Test @Tag("Q5")
    public void registerProviderClassWithAnInterface() {
      var registry = new InjectorRegistry();
      registry.registerProviderClass(D.class, E.class);
      var d = registry.lookupInstance(D.class);
      assertTrue(d instanceof E);
    }

    public interface F {}
    public static class G {
      @Inject
      public void setF(F f) {
        fail();
      }
    }

    @Test @Tag("Q5")
    public void registerProviderClassWithAMissingDependency() {
      var registry = new InjectorRegistry();
      registry.registerProviderClass(G.class, G.class);  // ok
      assertThrows(IllegalStateException.class, () -> registry.lookupInstance(G.class));
    }

    @Test @Tag("Q5")
    public void registerProviderClassPreconditions() {
      var registry = new InjectorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> registry.registerProviderClass(null, Object.class)),
          () -> assertThrows(NullPointerException.class, () -> registry.registerProviderClass(Consumer.class, null))
      );
    }
  }


  @Nested
  public class Q6 {
    @Test @Tag("Q6")
    public void registerProviderClassNoInjectConstructorNoDefaultConstructor() {
      class A {
        // No @Inject, No default (local class have a reference to the enclosing class)
        public A() {}
      }

      var registry = new InjectorRegistry();
      assertThrows(NoSuchMethodError.class, () -> registry.registerProviderClass(A.class, A.class));
    }

    public static class B {
      @Inject
      public B(Integer i) {}

      @Inject
      public B(String s) {}
    }

    @Test @Tag("Q6")
    public void registerProviderClassMultipleInjectConstructors() {
      var registry = new InjectorRegistry();
      assertThrows(IllegalStateException.class, () -> registry.registerProviderClass(B.class, B.class));
    }

    @Test @Tag("Q6")
    public void registerProviderClassWithAnEmptyRecord() {
      record A() {
        @Inject
        public A {}
      }

      var registry = new InjectorRegistry();
      registry.registerProviderClass(A.class, A.class);
      A a = registry.lookupInstance(A.class);
      assertNotNull(a);
    }

    @Test @Tag("Q6")
    public void registerProviderClassWithAMissingDependency() {
      record Bar() {}
      record Foo(Bar bar) {
        @Inject
        public Foo {}
      }

      var registry = new InjectorRegistry();
      assertThrows(IllegalStateException.class, () -> registry.registerProviderClass(Foo.class, Foo.class));
    }

    @Test @Tag("Q6")
    public void registerProviderClassWithAConstructorWithTwoIntegers() {
      record A(Integer value1, Integer value2) {
        @Inject
        public A {}
      }

      var counter = new Object() { int count; };
      var registry = new InjectorRegistry();
      registry.registerProvider(Integer.class, () -> counter.count++);
      registry.registerProviderClass(A.class, A.class);
      A a = registry.lookupInstance(A.class);
      assertAll(
          () -> assertEquals(0, a.value1),
          () -> assertEquals(1, a.value2)
      );
    }

    @Test @Tag("Q6")
    public void registerProviderClassWithAnInterface() {
      interface D {}
      record E() implements D {
        public E {}
      }

      var registry = new InjectorRegistry();
      registry.registerProviderClass(D.class, E.class);
      var d = registry.lookupInstance(D.class);
      assertTrue(d instanceof E);
    }

    public record Point(int x, int y) {
      public Point() { this(0, 0); }
    }
    public static class Circle {
      private final Point center;
      private Point point;

      @Inject
      public Circle(Point center) {
        this.center = center;
      }

      @Inject
      public void setPoint(Point point) {
        this.point = point;
      }
    }

    @Test @Tag("Q6")
    public void exampleWithAll() {
      var registry = new InjectorRegistry();
      registry.registerInstance(String.class, "hello");
      registry.registerProvider(Point.class, Point::new);
      registry.registerProviderClass(Circle.class, Circle.class);

      var string = registry.lookupInstance(String.class);
      var point = registry.lookupInstance(Point.class);
      var circle = registry.lookupInstance(Circle.class);
      assertAll(
          () -> assertEquals("hello", string),
          () -> assertEquals(new Point(0, 0), point),
          () -> assertEquals(new Point(0, 0), circle.center),
          () -> assertEquals(new Point(0, 0), circle.point)
      );
    }
  }
  */
}