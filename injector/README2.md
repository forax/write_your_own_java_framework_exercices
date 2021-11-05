# Annotation classpath scanning

This is the part 2 of the implementation of an injector,
it reuses the code written for the [part 1](README.md) (only in the tests).

The idea of the classpath scanning is to find all classes of a package annotated with some pre-registered annotation
types, and auto-automatically execute the action corresponding to the annotation on those classes.

Here is an example, let suppose we have an annotation `Component` defined like this
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
}
```

Then we have two classes `Service`and `Dependency` annotated with `@Component` defined as such
```java
@Component
public class Service {
   private final Dependency dependency;

   @Inject
   public Service(Dependency dependency) {
      this.dependency = Objects.requireNonNull(dependency);
   }

   public Dependency getDependency() {
      return dependency;
   }
}

@Component
public class Dependency {
}
```

An annotation scanner is a class that will scan all the classes of the package, here all the classes of the package
containing the class `Application` and for each class annotated with `@Component` execute the action, here,
register the class as [provider class](README.md#our-injector) in the registry.

So when calling `lookupInstance` on the registry, the registry knows how to create a `Service`and a `Dependency`.

```java
public class Application {
  public static void main(String[] args) {
    var registry = new InjectorRegistry();
    var scanner  = new AnnotationScanner();
    scanner.addAction(Component.class, registry::registerProviderClass);
    scanner.scanClassPathPackageForAnnotations(Application.class);
    var service = registry.lookupInstance(Service.class);
  }
}
```

### What do I need to implement it ?

For the implementation,
[ClassLoader.getResources()](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ClassLoader.html#getResources(java.lang.String))
with as parameter a package name with the dots ('.') replaced by slash ('/')  returns all the `URL`s of the folders
containing the classes (you can have more than one folder, by example one folder in src and one folder in test).

[Class.forName](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader))(name, /*initialize=*/ false, classloader)
loads a `Class`without initializing the class (without running its static block).

Those two methods are already available in the class [Utils2.java](src/main/java/com/github/forax/framework/injector/Utils2.java)
with the exceptions correctly managed.


## Let's implement it

1. First, we want to implement a method `findAllJavaFilesInFolder(path)` (not public) that takes a `Path` and returns
   the name of all the Java class in the folder. A Java class is file with a filename that ends with ".class".
   The name of a Java class is the name of the file without the extension ".class".
   Implement the method `findAllJavaFilesInFolder` and
   check that the tests in the nested class "Q1" all pass.

2. Then we want to implement a method `findAllClasses(packageName, classloader)` (not public) that return a list
   of the classes (the `Class<?>`) contained in the package `packageName` loaded by the `classloader`
   taken as argument.
   Implement the method `findAllClasses` and check that the tests in the nested class "Q2" all pass.
   
   Note: `Utils2.getResources(packageName.replace('.', '/'), classLoader)` returns the URLs of the folders that
   contains the class files of a package. From a URL, you can get a URI and you can construct a Path
   from a URI. `Utils2.loadClass(packageName + '.' + className, classLoader)` loads a class
   from the qualified name of a class (the name with '.' in it).
   
3. We now want to add the method `addAction(annotationClass, action)` that take `annotationClass` the class of
   an annotation and `action` a function that takes a class and return `void`.
   `addAction` register the action for the annotation class and only one action can be registered
   for an annotation class
   Implement the method `addAction` and check that the tests in the nested class "Q3" all pass.

4. We want to implement the method `scanClassPathPackageForAnnotations(class)`
   that takes a class as parameter, find the corresponding package, load all the class from the folders
   corresponding to the package, find all annotations of the classes and run the action each time
   corresponding to the annotation classes.
   Implement the method `scanClassPathPackageForAnnotations` and check that the tests in the nested class "Q4" all pass.

   Note: there is a method
   [getPackageName()](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html#getPackageName())
   to find the name of a package of a class and a method 
   [getClassLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html#getClassLoader())
   to get the classloader of a class.



   