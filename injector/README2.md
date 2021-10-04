# Annotation classpath scanning

This is the part 2 of the implementation of an injector,
it reuses the code written for the [part 1](README.md).

The idea of the classpath scanning is to find all classes of a package, and auto-automatically
register all top-level classes with either a constructor or a method annotated with `Inject`
and all their dependencies.

For that, we will implement of method `scanClassPathPackageForAnnotations(registry, classInPackage)`
that scan all the classes of the package containing the class `classInPackage` and register
all the classes annotated with `Inject` and their dependencies.

Here is an example
```java
public interface Dummy {}

public class Dependency { }

public class OnlyConstructor { 
  private final Dependency dependency;

  @Inject
  public OnlyConstructor(Dependency dependency) {
    this.dependency = Objects.requireNonNull(dependency);
  }

  public Dependency getDependency() {
    return dependency;
  }
}
...
var registry = new InjectorRegistry();
AnnotationScanner.scanClassPathPackageForAnnotations(registry, Dummy.class);

var onlyConstructor = registry.lookupInstance(OnlyConstructor.class);
assertNotNull(onlyConstructor.getDependency());
```

`scanClassPathPackageForAnnotations()` first scan all the classes in the package containing `Dummy`,
here, `Dependency`, `Dummy` and `OnlyConstructor`, then lookup up for classes that uses `@Inject`,
so only `OnlyConstructor` is selected. Then all constructor dependencies are gathered, here,
only `Dependency`, so the class `Dependency` and `OnlyConstructor`, in that order,
are register as [provider class](README.md#our-injector) in the `Registery`.


### Early vs Late binding

In the previous part, we have implemented two kinds of injection
- injection based on constructors which is bound early,
  all dependency classes **must** be present at the registration time.
- injection based on setters which is bound late,
  all dependency classes are only checked at the time the setter is called.

Because constructors are checked early, it means that all the dependency of the constructors
must be already registered **before** the class defining that constructor can be registered.
So we have to analyze the constructor dependency to register them in the right order.


### finding and loading the classes of a package



## Let's implement it


1. We want to implement the classpath scanning, i.e. find all classes of a package
   from its different folders and register the class that have at least a constructor or a method
   annotated with `@Inject` as provider class.

   To indicate which package to scan, the method `scanClassPathPackageForAnnotations(class)`
   takes a class as parameter instead of a package name. This is trick provides both the
   [package name](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/Class.html#getPackageName())
   and the
   [class loader](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/Class.html#getClassLoader()).

   For the implementation,
   [ClassLoader.getResources()](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/ClassLoader.html#getResources(java.lang.String))
   with as parameter a package name with the dots ('.') replaced by slash ('/')  returns all the `URL`s
   containing the classes (you can have more than one folder, by example one folder in src and one folder in test).
   Calling `Path.of(url.toURI())` get a `Path` from an `URL` and
   [Class.forName(name, /*initialize=*/ false, classloader)](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader))
   loads a `Class`without initializing the class (without running its static block).

   Implement the method `scanClassPathPackageForAnnotations(class)` and
   check that the tests in the nested class "Q7" all pass.


   