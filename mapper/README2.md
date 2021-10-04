# Reading objects from JSON

The aim of class `JSONReader` is to transform JSON objects in textual form into Java Beans,
or records and JSON array into `java.util.List`.

First, we register `TypeMatcher`s with `addTypeMatcher(typeMatcher)`, that recognize types like records or list and
provide a specific `Collector` which is able to create objects populated with the values from the JSON text.
Then we call `parseJSON(text, type)` with a JSON text, and a type that will be used to decode the JSON text.

Here is an example using a record
```java
public record Person(String name, int age) {}

var reader = new JSONReader();
reader.addTypeMatcher(type -> Optional.of(Utils.erase(type))
    .filter(Class::isRecord)
    .map(Collector::record));
var person = reader.parseJSON("""
    {
      "name": "Ana", "age": 24
    }
    """, Person.class);
assertEquals(new Person("Ana", 24), person);
```

In the code above, the `TypeMatcher` recognizes the type that can be erased as records and provides a
specific `Collector` named `Collector.record(recordClass)` which decodes records.


### Class vs Type

In order to be able to know decode a JSON array of JSON object, we can not use `java.lang.Class`
to represent the type of the JSON element, we will use a `java.lang.reflect.Type` for that. 
See [](../COMPANION.md#java-compiler-generics-attributes) for mode info.


### ToyJSONParser

Instead or using a real JSON parser like [Jackson](https://github.com/FasterXML/jackson-core),
we are using a toy one.
`ToyJSONParser` is a simple parser that doesn't really implement the JSON format but that's enough
for what we want.  The method `ToyJSONParser.parseJSON(text, visitor)` takes a JSON text and calls
the methods of the visitor during the parsing.

The interface `ToyJSONParser.JSONVisitor` is defined like this
```java
public interface JSONVisitor {
    /**
     * Called during the parsing or the content of an object or an array.
     *
     * @param key the key of the value if inside an object, {@code null} otherwise.
     * @param value the value
     */
    void value(String key, Object value);

    /**
     * Called during the parsing at the beginning of an object.
     * @param key the key of the value if inside an object, {@code null} otherwise.
     *
     * @see #endObject(String)
     */
    void startObject(String key);

    /**
     * Called during the parsing at the end of an object.
     * @param key the key of the value if inside an object, {@code null} otherwise.
     *
     * @see #startObject(String)
     */
    void endObject(String key);

    /**
     * Called during the parsing at the beginning of an array.
     * @param key the key of the value if inside an object, {@code null} otherwise.
     *
     * @see #endArray(String)
     */
    void startArray(String key);

    /**
     * Called during the parsing at the end of an array.
     * @param key the key of the value if inside an object, {@code null} otherwise.
     *
     * @see #startArray(String)
     */
    void endArray(String key);
  }
```

JSON values are either inside an object or an array, if they are inside an object, the label (the `key`)
is provided. If the values are inside an array, the `key` is `null`.
Given that an object or an array can itself be in an object, the methods `startObject`/`startArray` and
`endObject`/`endArray`also takes a `key` as parameter.

For example, for the JSON object `{ "kevin": true, "data": [1, 2] }`, the visitor will call `startObject(null)`,
`value("kevin", true)`, `startArray("data")`, `value(null, 1)`, `value(null, 2)`, `endArray("data")`
and `endObject(null)`.


### Collector

A `JSONReader` uses two abstractions, one called a Collector (because it's behavior is close to a
[Stream Collector](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/stream/Collector.html)).
A collector abstract the way to create an object, fill it with values and transform it to another object
before returning it. In order to propagate the type, it also has a function that return the type of a `key`

A collector is composed of 4 functions
- a *qualifier* that returns the `Type` of a key `(key) -> Type`
- a *supplier* that creates a data `() -> data`
- a *populater* that inserts the key / value into the data `(data, key, value) -> void`
- a *finisher* that transform the data into an object `(data) -> Object`

The *finisher* can be used to make the data immutable by example.


### TypeMatcher

Now that we have a collector, we need to answer to the question, which collector to associate to
a peculiar `Type`. This is the role of the `TypeMatcher`. It indicates for a type if it knows
a collector (encapsulated in an optional) or if it does not know this kind of type and returns
`Optional.empty()`.

```java
@FunctionalInterface
public interface TypeMatcher {
  Optional<Collector<?>> match(Type type);
}
```


## Let's implement it

The unit tests are in [JSONReaderTest.java](src/test/java/com/github/forax/framework/mapper/JSONReaderTest.java)

1. Let's start small, in the class `JSONReader` write a method `parseJSON(text, class)` that takes
   a JSON text, and the class of a Java Beans and returns an instance of the class with
   initialized by calling the setters corresponding to the JSON keys.
   
   For example, with the JSON object `{ "foo": 3, "bar": 4 }`, the method `parseJSON(text, class)`
   creates an instance of the class using the default constructor, then for the key "foo" calls
   the setter `setFoo(3)` and for the key "bar" calls the setter `setBar(4)` and returns
   the initialized instance.
   Then check that the tests in the nested class "Q1" all pass.

   Note: you can use a type variable 'T' to indicate that the type of the class and the return type
   of `parseJSON` are the same.
   Note2: to avoid to find the setter in an array of properties each time there is a value,
   precompute a `Map<String, PropertyDescriptor>` that associate a property name to the property.
   

2. We now want to support a JSON object defined inside a JSON object.
   For that, we need a stack that stores a pair of `Map<String, PropertyDescriptor>` and bean instance
   (otherwise we will not know which setter to call on which bean when we come back in a `endObject()`).
   Change the code of `parseJSON(text, class)` to handle the recursive definition of JSON objects
   and check that the tests in the nested class "Q2" all pass.

   Note: in Java, the class
   [ArrayDeque](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/ArrayDeque.html)
   can act as a stack using the methods `peek()`, `push()` and `pop()` 


3. We now want to abstract the code to support other model than the Java bean model.
   For that we introduce a record `Collector` that let users define how to retrieve the type from a key
   (`qualifier`), how to create a temporary object (`supplier`), how to store value into the temporary object
   (`populater`) and how to create an immutable version of the temporary objet (`finisher`). 
   ```java
   public record Collector<B>(Function<? super String, ? extends Type> qualifier,
                           Supplier<? extends B> supplier, Populater<B> populater, Function<? super B, ?> finisher) {
     public interface Populater<B> {
       void populate(B builder, String key, Object value);
     }
   
     @SuppressWarnings("unchecked")
     private Collector<Object> raw() {
       return (Collector<Object>) (Collector<?>) this;
     }
   }
   ```
   Before changing the code of `parseJSON(text, class)` to use a collector, let's first create a collector
   for the Java Beans. For that, we will create a static method of `Collector` named `bean(beanClass)` that takes
   a class of a bean as parameter and return a `Collector<Object>` able to create a Java bean instance
   and populate it (a bean is inherently mutable,thus the finisher will be the identity function).
   
   Create the method `Collector.bean(beanClass)` and check that the tests in the nested class "Q3" all pass.

   Note: the method `Collector.raw()` is useless for this question, but will be handy in the next one. 


4. Now that we have a collector of beans, we can rewrite the code of `parseJSON(text, class)` to use
   a `Collector<Object>` instead of a `Map<String, PropertyDescriptor>`.
   To simulate the fact that in the future, there will be several collectors, we propose to declare
   a private method `findCollector(type)` that for now always return the collector of beans.
   ```java
   private Collector<?> findCollector(Type type) {
     return Collector.bean(Utils.erase(type));
   }
   ```
   
   You can notice that both a `Collector` and `findCollector(type)` works on `Type` and not on `Class`,
   so we can add an overload to the method `parseJSON()` that takes a `Type` as second parameter
   instead of a `Class` so the code will work for all `Type`s.
   Fortunately, given that a `Class` is a `Type`, you do not have to duplicate the code between
   the two overloads.
   
   Modify the code to have the two methods `parseJSON(text, class)` and `parseJSON(text, type)`,
   and checks that the tests in the nested class "Q4" all pass.
   

5. We now want to add a new `Collector` tailored for supporting JSON array as `java.util.List`,
   for that add a method static `list(elementType)` in `Collector` that takes the type of
   the element as parameter and returns the collector.

   We also want to let users to be able to add their own collectors, exactly, their own `TypeMatcher`.
   ```java
   @FunctionalInterface
   public interface TypeMatcher {
     Optional<Collector<?>> match(Type type);
   }
   ```
   A `TypeMatcher` which specify for a type a collector to use (by returning a collector
   wrapped into an `Optional`) or say that the type is not supported by the `TypeMartcher`
   (and return `Optional.empty()`).
   
   To add a `TypeMatcher`, we introduce a method `addTypeMatcher(typeMatcher)` and
   modify the method `findCollector(type)` to call the `TypeMatcher`s
   in reverse order of their insertion order.
   If no `TypeMatcher` answer for a `Type`, use the `Collector.bean()`.
   Checks that the tests in the nested class "Q5" all pass.

   Note: there is a method `Utils.reverseList()` that reverse a List without actually moving the elements.


6. Creating a `Type` is not something easy for a user because all the implementations
   are not visible, the indirect way is to ask for a `Type` of
   a field, a method or a class/interface by reflection.
   Jackson uses a type named `TypeReference` to help people to specify a `Type`.
   
   The idea is that if you provide an implementation of `TypeRefrence` as an anonymous class,
   the compiler inserts in the anonymous class an attribute indicating the generic interfaces,
   and you can write a code that extract the type argument of a generic interfaces.
   
   For example with,
   ```java
   var typeReference = new TypeRefrence<Foo>() {};
   ```
   finding `Foo` is equivalent to getting the class of  `typeReference`, asking for the first
   generic interface (with `getGenericInterfaces()`), seeing it as a `ParameterizedType` and
   extracting the first actual type argument (with `getActualTypeArguments()`).
   
   Implement a new method `parseJSON(text, typeRefrence)` that extract the type argument from
   the anonymous class and calls `parseJSON(text, type)` with the type argument.
   Checks that the tests in the nested class "Q6" all pass.
   

7. We now want to support records.
   Add a static method `record(recordClass)` in `Collector` so the first example at the beginning
   of this page works.
   Checks that the tests in the nested class "Q7" all pass.
   
   Note: to create a record, you first need to create an array to store all the component values,
   in the order of the component, then call the canonical constructor (see `Utils.canonicalConstructor`).