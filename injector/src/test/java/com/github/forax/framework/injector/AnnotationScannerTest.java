package com.github.forax.framework.injector;

import com.github.forax.framework.injector.scanner.AutoScanned;
import com.github.forax.framework.injector.scanner.Dependency;
import com.github.forax.framework.injector.scanner.Dummy;
import com.github.forax.framework.injector.scanner.OnlyConstructor;
import com.github.forax.framework.injector.scanner.OnlySetter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class AnnotationScannerTest {
  /*
  @Nested
  public class Q1 {
    @Test @Tag("Q1")
    public void findAllClassesOfInjectorDotScanner() {
      var classes = AnnotationScanner.findAllClasses(Dummy.class.getPackageName(), Dummy.class.getClassLoader())
          .collect(toCollection(() -> new TreeSet<>(Comparator.comparing(Class::getName))));
      assertEquals(Set.of(AutoScanned.class, Dependency.class, Dummy.class, OnlyConstructor.class, OnlySetter.class), classes);
    }

    @Test @Tag("Q1")
    public void findAllClassesOfInjector() {
      var classes = AnnotationScanner.findAllClasses(Utils2.class.getPackageName(), Utils2.class.getClassLoader())
          .filter(clazz -> !clazz.isMemberClass() && !clazz.isLocalClass() && !clazz.isAnonymousClass())
          .collect(toCollection(() -> new TreeSet<>(Comparator.comparing(Class::getName))));
      assertEquals(
          Set.of(AnnotationScanner.class, Inject.class, InjectorRegistry.class, Utils.class, Utils2.class,
                 AnnotationScannerTest.class, InjectorRegistryTest.class),
          classes);
    }

  }  // end of Q1


  @Nested
  public class Q2 {
    public static class MemberClass {}

    @Test @Tag("Q2")
    public void isAProviderClassInvalidClass() {
      class LocalClass {}
      var anonymousClass = new Object() {}.getClass();

      assertAll(
          () -> assertFalse(AnnotationScanner.isAProviderClass(MemberClass.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(LocalClass.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(anonymousClass)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(List.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(AbstractList.class))
      );
    }

    @Test @Tag("Q2")
    public void isAProviderClassNoInject() {
      assertAll(
          () -> assertFalse(AnnotationScanner.isAProviderClass(String.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(LocalDate.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(Integer.class)),
          () -> assertFalse(AnnotationScanner.isAProviderClass(Dependency.class))
      );
    }

    @Test @Tag("Q2")
    public void isAProviderClassInjectOk() {
      assertAll(
          () -> assertTrue(AnnotationScanner.isAProviderClass(AutoScanned.class)),
          () -> assertTrue(AnnotationScanner.isAProviderClass(OnlyConstructor.class)),
          () -> assertTrue(AnnotationScanner.isAProviderClass(OnlySetter.class))
      );
    }

  }  // end of Q2


  @Nested
  public class Q3 {

    @Test @Tag("Q3")
    public void dependenciesNotProviderClass() {
      assertAll(
          () -> assertEquals(List.of(), AnnotationScanner.dependencies(String.class)),
          () -> assertEquals(List.of(), AnnotationScanner.dependencies(LocalDate.class)),
          () -> assertEquals(List.of(), AnnotationScanner.dependencies(Integer.class)),
          () -> assertEquals(List.of(), AnnotationScanner.dependencies(Dependency.class))
      );
    }

    @Test @Tag("Q3")
    public void dependenciesProviderClass() {
      assertAll(
          () -> assertEquals(List.of(Dependency.class), AnnotationScanner.dependencies(AutoScanned.class)),
          () -> assertEquals(List.of(Dependency.class), AnnotationScanner.dependencies(OnlyConstructor.class)),
          () -> assertEquals(List.of(), AnnotationScanner.dependencies(OnlySetter.class))
      );
    }

  }  // end of Q3


  @Nested
  public class Q4 {
    @Test @Tag("Q4")
    public void findDependenciesInOrderAutoScannedAndOnlyConstructor() {
      var dependencies = AnnotationScanner.findDependenciesInOrder(List.of(AutoScanned.class, OnlyConstructor.class));
      assertAll(
          () -> assertEquals(Set.of(Dependency.class, AutoScanned.class, OnlyConstructor.class), dependencies),
          () -> assertEquals(List.of(Dependency.class, AutoScanned.class, OnlyConstructor.class), new ArrayList<>(dependencies))
          );
    }

    @Test @Tag("Q4")
    public void findDependenciesInOrderOnlyConstructor() {
      var dependencies = AnnotationScanner.findDependenciesInOrder(List.of(OnlyConstructor.class));
      assertAll(
          () -> assertEquals(Set.of(Dependency.class, OnlyConstructor.class), dependencies),
          () -> assertEquals(List.of(Dependency.class, OnlyConstructor.class), new ArrayList<>(dependencies))
      );
    }

    @Test @Tag("Q4")
    public void findDependenciesInOrderOnlySetter() {
      var dependencies = AnnotationScanner.findDependenciesInOrder(List.of(OnlySetter.class));
      assertEquals(Set.of(OnlySetter.class), dependencies);
    }

  }  // end of Q4


  @Nested
  public class Q5 {
    @Test @Tag("Q5")
    public void scanPackageForAnnotationsOnlyConstructor() {
      var registry = new InjectorRegistry();
      AnnotationScanner.scanClassPathPackageForAnnotations(registry, Dummy.class);

      var onlyConstructor = registry.lookupInstance(OnlyConstructor.class);
      assertNotNull(onlyConstructor.dependency());
    }

    @Test @Tag("Q5")
    public void scanPackageForAnnotationsOnlySetter() {
      var registry = new InjectorRegistry();
      AnnotationScanner.scanClassPathPackageForAnnotations(registry, Dummy.class);

      var onlySetter = registry.lookupInstance(OnlySetter.class);
      assertNotNull(onlySetter.getDependency());
    }

    @Test @Tag("Q5")
    public void scanPackageForAnnotationsMixedWithAnInstance() {
      var registry = new InjectorRegistry();
      AnnotationScanner.scanClassPathPackageForAnnotations(registry, Dummy.class);
      registry.registerInstance(String.class, "hello");

      var autoScanned = registry.lookupInstance(AutoScanned.class);
      assertAll(
          () -> assertEquals("hello", autoScanned.getText()),
          () -> assertNotNull(autoScanned.getDependency())
      );
    }

    @Test @Tag("Q5")
    public void scanPackageForAnnotationsPreconditions() {
      var registry = new InjectorRegistry();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> AnnotationScanner.scanClassPathPackageForAnnotations(null, Dummy.class)),
          () -> assertThrows(NullPointerException.class, () -> AnnotationScanner.scanClassPathPackageForAnnotations(registry, null))
      );
    }

  }  // end of Q5
  */
}