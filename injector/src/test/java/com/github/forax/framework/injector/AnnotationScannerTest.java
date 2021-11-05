package com.github.forax.framework.injector;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("unused")
public class AnnotationScannerTest {
  /*
  @Nested
  public class Q1 {
    @Test
    public void test() throws IOException {
      var folder = Files.createTempDirectory("annotation-scanner");
      var textPath = Files.writeString(folder.resolve("text.txt"), "this is a text");
      var javaPath = Files.writeString(folder.resolve("AFakeJava.class"), "this is a fake java class");
      try {

        List<String> list;
        try(var stream = AnnotationScanner.findAllJavaFilesInFolder(folder)) {
          list = stream.toList();
        }
        assertEquals(List.of("AFakeJava"), list);

      } finally {
        Files.delete(javaPath);
        Files.delete(textPath);
        Files.delete(folder);
      }
    }
  }   // end of Q1


  @Nested
  public class Q2 {
    static class AnotherClass {}

    @Test @Tag("Q2")
    public void findAllClasses() {
      var packageName = Q2.class.getPackageName();
      var classLoader = Q2.class.getClassLoader();
      var list = AnnotationScanner.findAllClasses(packageName, classLoader);
      assertTrue(list.contains(AnotherClass.class));
    }

    @Test @Tag("Q2")
    public void findAllClassesPrecondition() {
      var packageName = "a.package.that.do.not.exist";
      var classLoader = Q2.class.getClassLoader();
      assertThrows(IllegalStateException.class, () -> AnnotationScanner.findAllClasses(packageName, classLoader));
    }
  }    // end of Q2

  @Nested
  public class Q3 {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Hello {
    }

    @Test @Tag("Q3")
    public void addAction() {
      var scanner = new AnnotationScanner();
      scanner.addAction(Hello.class, (Class<?> type) -> {});
    }

    @Test @Tag("Q3")
    public void addActionSameAnnotationClassTwice() {
      var scanner = new AnnotationScanner();
      scanner.addAction(Hello.class, __ -> {});
      assertThrows(IllegalStateException.class, () -> scanner.addAction(Hello.class, __ -> {}));
    }

    @Test @Tag("Q3")
    public void addActionPrecondition() {
      var scanner = new AnnotationScanner();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> scanner.addAction(Hello.class, null)),
          () -> assertThrows(NullPointerException.class, () -> scanner.addAction(null, __ -> {}))
      );
    }
  }    // end of Q3


  // Q4

  @Nested
  public class Q4 {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Entity {
    }

    @Entity
    static class AnnotatedClass {}

    @Test @Tag("Q4")
    public void scanClassPathPackageForAnnotations() {
      var scanner  = new AnnotationScanner();
      scanner.addAction(Entity.class, type -> assertEquals(AnnotatedClass.class, type));
      scanner.scanClassPathPackageForAnnotations(Q4.class);
    }


    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Component {
    }

    @Component
    static class Service {
      public Service() {
      }
    }

    @Test @Tag("Q4")
    public void scanAndInjectSimple() {
      var registry = new InjectorRegistry();
      var scanner  = new AnnotationScanner();
      scanner.addAction(Component.class, registry::registerProviderClass);
      scanner.scanClassPathPackageForAnnotations(Q4.class);
      var service = registry.lookupInstance(Service.class);
      assertNotNull(service);
    }


    @Component
    public static class Dependency { }

    @Component
    static class ServiceWithDependency {
      private final Dependency dependency;

      @Inject
      public ServiceWithDependency(Dependency dependency) {
        this.dependency = Objects.requireNonNull(dependency);
      }

      public Dependency getDependency() {
        return dependency;
      }
    }

    @Test @Tag("Q4")
    public void scanAndInjectWithDependency() {
      var registry = new InjectorRegistry();
      var scanner  = new AnnotationScanner();
      scanner.addAction(Component.class, registry::registerProviderClass);
      scanner.scanClassPathPackageForAnnotations(Q4.class);
      var service = registry.lookupInstance(ServiceWithDependency.class);
      assertNotNull(service);
      assertNotNull(service.getDependency());
    }


    public static class NonAnnotatedDependency { }

    @Component
    static class EntityWithANonAnnotatedDependency {
      private final NonAnnotatedDependency nonAnnotatedDependency;

      @Inject
      public EntityWithANonAnnotatedDependency(NonAnnotatedDependency nonAnnotatedDependency) {
        this.nonAnnotatedDependency = nonAnnotatedDependency;
      }
    }

    @Test @Tag("Q4")
    public void scanAndInjectWithMissingDependency() {
      var registry = new InjectorRegistry();
      var scanner  = new AnnotationScanner();
      scanner.addAction(Component.class, registry::registerProviderClass);
      scanner.scanClassPathPackageForAnnotations(Q4.class);
      assertThrows(IllegalStateException.class, () -> registry.lookupInstance(EntityWithANonAnnotatedDependency.class));
    }

    @Test @Tag("Q4")
    public void scanClassPathPackageForAnnotationsPrecondition() {
      var scanner  = new AnnotationScanner();
      assertThrows(NullPointerException.class, () -> scanner.scanClassPathPackageForAnnotations(null));
    }

  }  // end of Q4
   */
}