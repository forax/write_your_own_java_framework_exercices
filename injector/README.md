# Dependency Injection

An `InjectorRegistry` is a class able to provide instances of classes from recipes of object creation.
There are two ways to get such instances either explicitly using `lookupInstance(type)` or
implicitly on constructors or setters annotated by the annotation `@Inject`.


### Protocols of injection

Usual injection framework like [Guice](https://github.com/google/guice),
[Spring](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-dependencies)
or [CDI/Weld](https://docs.oracle.com/javaee/6/tutorial/doc/giwhb.html)
provides 3 ways to implicitly get an instance of a class
- constructor based dependency injection, the constructor annotated with
  [@Inject](https://javax-inject.github.io/javax-inject/)
  is called with the instances as arguments, it's considered as the best way to get a dependency
- setter based dependency injection, after a call to the default constructor, the setters are called.
  The main drawback is that the setters can be called in any order (the order may depend on
  the version of the compiler/VM used)
- field based dependency injection, after a call to the default constructor, the fields are filled with the instances,
  this methods bypass the default security model of Java using **deep reflection**, relying on either
  not having a module declared or the package being open in the module-info.java. Because of that,
  this is not the recommended way of doing injection.

We will only implement the constructor based and setter based dependency injection.


### Early vs Late checking

When injecting instances, an error can occur if the `InjectorRegistry` has no recipe to create
an instance of a class. Depending on the implementation of the injector, the error can be
detected either
- when a class that asks for injection is registered
- when an instance of a class asking for injection is requested

The former is better than the later because the configuration error are caught earlier,
but here, because we want to implement a simple injector, all configuration errors will appear
late when an instance is requested.


### Configuration

There are several ways to configure an injector, it can be done
- using an XML file, this the historical way (circa 2000-2005) to do the configuration.
- using classpath/modulepath scanning. All the classes of the application are scanned and classes with
  annotated members are added to the injector. The drawback of this method is that this kind of scanning
  is quite slow, slowing down the startup time of the application.
  Recent frameworks like Quarkus or Spring Native move the annotation discovery at compile time using
  an annotation processor to alleviate that issue.
- using an API to explicitly register the recipe to get the dependency.

We will implement the explicit API while the classpath scanning is implemented in [the part 2](README2.md).


### Our injector

The class `InjectorRegistry` has 4 methods
- `lookupInstance(type)` which returns an instance of a type using a recipe previously registered
- `registerInstance(type, object)` register the only instance (singleton) to always return for a type
- `registerProvider(type, supplier)` register a supplier to call to get the instance for a type
- `registerProviderClass(type, class)` register a bean class that will be instantiated for a type

As an example, suppose we have a record `Point` and a bean `Circle` with a constructor `Circle` annotated
with `@Inject` and a setter `setName` of `String` also annotated with `@Inject`.

```java
record Point(int x, int y) {}

class Circle {
  private final Point center;
  private String name;

  @Inject
  public Circle(Point center) {
    this.center = center;
  }

  @Inject
  public void setName(String name) {
    this.name = name;
  }
}
```

We can register the `Point(0, 0)` as the instance that will always be returned when an instance of `Point` is requested.
We can register a `Supplier` (here, one that always return "hello") when an instance of `String` is requested.
We can register a class `Circle.class` (the second parameter), that will be instantiated when an instance of `Circle`
is requested.

```java
var registry = new InjectorRegistry();
registry.registerInstance(Point.class, new Point(0, 0));
registry.registerProvider(String.class, () -> "hello");
registry.registerProviderClass(Circle.class, Circle.class);

var circle = registry.lookupInstance(Circle.class);
System.out.println(circle.center);  // Point(0, 0)
System.out.println(circle.name);  // hello    
```


## Let's implement it

The unit tests are in [InjectorRegistryTest.java](src/test/java/com/github/forax/framework/injector/InjectorRegistryTest.java)

1. Create a class `InjectorRegistry` and add the methods `registerInstance(type, instance)` and
   `lookupInstance(type)` that respectively registers an instance into a `Map` and retrieves an instance for
   a type. `registerInstance(type, instance)` should allow registering only one instance per type and
   `lookupInstance(type)` should throw an exception if no instance have been registered for a type.
   Then check that the tests in the nested class "Q1" all pass.

   Note: for now, the instance does not have to be an instance of the type `type`.
   You can use [Map.putIfAbsent()](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/Map.html#putIfAbsent(K,V))
   to detect if there is already a pair/entry with the same key in the `Map` in one call.


2. We want to enforce that the instance has to be an instance of the type taken as parameter.
   For that, declare a `T` and say that the type of the `Class`and the type of the instance is the same.
   Then use the same trick for `lookupInstance(type)` and check that the tests in the nested class "Q2" all pass.

   Note: inside `lookupInstance(type)`, now that we now that the instance we return has to be
   an instance of the type, we can use
   [Class.cast()](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/Class.html#cast(java.lang.Object))
   to avoid an unsafe cast.


3. We now want to add the method `registerProvider(type, supplier)` that register a supplier (a function with
   no parameter that return a value) that will be called each time an instance is requested.
   An astute reader can remark that a supplier can always return the same instance thus we do not need two `Map`s,
   but only one that stores suppliers.
   Add the method `registerProvider(type, supplier)` and modify your implementation to support it.
   Then check that the tests in the nested class "Q3" all pass.


4. In order to implement the injection using setters, we need to find all the
   [Bean properties](../COMPANION.md#java-bean-and-beaninfo)
   that have a setter [annotated](../COMPANION.md#methodisannotationpresent-methodgetannotation-methodgetannotations)
   with `@Inject`.
   Write a helper method `findInjectableProperties(class)` that takes a class as parameter and returns a list of
   all properties (`PropertyDescriptor`) that have a setter annotated with `@Inject`.
   Then check that the tests in the nested class "Q4" all pass.

   Note: The class `Utils` already defines a method `beanInfo()`.


5. We want to add a method `registerProviderClass(type, providerClass)` that takes a type and a class,
   the `providerClass` implementing that type and register a recipe that create a new instance of
   `providerClass` by calling the default constructor. This instance is initialized by calling all the
   setters annotated with `@Inject` with an instance of the corresponding property type
   (obtained by calling `lookupInstance`).
   Write the method `registerProviderClass(type, providerClass)` and
   check that the tests in the nested class "Q5" all pass.

   Note: The class `Utils` defines the methods `defaultConstructor()`, `newInstance()` and `invokeMethod()`.


6. We want to add the support of constructor injection.
   The idea is that either only one of the
   [public constructors](../COMPANION.md#classgetmethod-classgetmethods-classgetconstructors)
   of the `providerClass` is annotated with `@Inject` or a public default constructor
   (a constructor with no parameter) should exist.
   Modify the code that instantiate the `providerClass` to use that constructor to
   creates an instance.
   Then check that the tests in the nested class "Q6" all pass.


7. To finish, we want to add a user-friendly overload of `registerProviderClass`,
   `registerProviderClass(providerClass)` that takes only a `providerClass`
   and is equivalent to `registerProviderClass(providerClass, providerClass)`.
   