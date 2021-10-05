# Writing objects to JSON

The idea is to implement an object, the `JSONWriter`, that is able to convert an object to a
[JSON](https://json.org) text.

A `JSONWriter` is able to convert
- basic JSON type like boolean, int or String
- can be configured to handle specific type like `MonthDay` of `java.time`
- recursive types, types composed of other types, likes Java Beans or records

Here is an example of a `Person` defined as a record, with the `Address` defined as a bean.

```java
class Address {
   private boolean international;

   public boolean isInternational() {
      return international;
   }
}
record Person(@JSONProperty("birth-day") MonthDay birthday, Address address) { }
```

We can create a `JSONWriter`, configure it to use a user defined format for instances of the class `MonthDay`
and calls `toJSON()` to get the corresponding JSON text.

```java
var writer = new JSONWriter();
        writer.configure(MonthDay.class,
        monthDay -> writer.toJSON(monthDay.getMonth() + "-" + monthDay.getDayOfMonth()));

        var person = new Person(MonthDay.of(4, 17), new Address());
        var json = writer.toJSON(person);  // {"birth-day": "APRIL-17", "address": {"international": false}}
```


## Let's implement it

The unit tests are in [JSONWriterTest.java](src/test/java/com/github/forax/framework/mapper/JSONWriterTest.java)

1. Create the class `JSONWriter` and adds the method `toJSON()` that works only with
   JSON primitive values, `null`, `true`, `false`, any integers or doubles and strings.
   Then check that the tests in the nested class "Q1" all pass.

2. Adds the support of [Java Beans](../COMPANION.md#java-bean-and-beaninfo) by modifying `toJSON()` to get the `BeanInfo`.
   Get the properties  from it and use a stream with a `collect(Collectors.joining())`
   to add the '{' and '}' and  separate the values by a comma.
   Then check that the tests in the nested class "Q2" all pass.

   Note: the method `Utils.beanInfo()` already provides a way to get the `BeanInfo` of a class.
   the method `Utils.invoke()` deals with the exception correctly when calling a `Method`.

3. The problem with the current solution is that the `BeanInfo` and the properties are computed each times
   even if the properties of a class are always the same.
   The idea is to declare a [ClassValue](../COMPANION.md#classvalue) that caches an array of properties for a class.
   So modify the method `toJSON()` to use a `ClassValue<PropertyDescriptor[]>`.
   All the tests from the previous questions should still pass.

4. We can cache more values, by example the property name and the getter are always the same for a pair of key/value.
   We can observe that from the JSONWriter POV, there are two kinds of type,
   - either it's a primitive those only need the object to generate the JSON text
   - or it's a bean type, those need the object, and the writer to recursively call `writer.toJSON()`
     on the properties
     Thus to represent the computation, we can declare a private functional interface `Generator` that takes
     a `JSONWriter` and an `Object` as parameter.
   ```java
   private interface Generator {
     String generate(JSONWriter writer, Object bean);
   }
   ```
   Change your code to use `ClassValue<Generator>` instead of a `ClassValue<PropertyDescriptor[]>`,
   and modify the implementation of the method `toJSON()` accordingly.
   All the tests from the previous questions should still pass.

5. Adds a method `configure()` that takes a `Class` and a lambda that takes an instance of that class
   and returns a string and modify `toJSON()` to work with instances of the configured classes.
   Internally, a HashMap that associates a class to the computation of the JSON text using the lambda.
   Then check that the tests in the nested class "Q5" all pass.

   **Note**: the lambda takes a value and returns a value thus it can be typed by a `java.util.function.Function`.
   The type of the class, and the type of the first parameter of the lambda are the same,
   you need to introduce a type parameter for that. Exactly the type of the first parameter of the
   lambda is a super type of the type of the class.

6. JSON keys can use any identifier not only the ones that are valid in Java.
   For that, we introduce an annotation `@JSONProperty` defined like this
   ```java
   @Retention(RUNTIME)
   @Target({METHOD, RECORD_COMPONENT})
   public @interface JSONProperty {
     String value();
   }
   ```
   To support that, add a check if the getter is annotated with the annotation `@JSONProperty`
   and in that case, use the name provided by the annotation instead of the name of the property.
   Then check that the tests in the nested class "Q6" all pass

7. Modify the code to support not only Java beans but also [records](../COMPANION.md#record) by refactoring
   your code to have two private methods  that takes a Class and returns either the properties of the bean
   or the properties of the records.
   ```java
   private static List<PropertyDescriptor> beanProperties(Class<?> type) {
     // TODO
   }

   private static List<PropertyDescriptor> recordProperties(Class<?> type) {
     // TODO
   }
   ```
   Change the code so `toJSON()` works with both records and beans.
   Then check that the tests in the nested class "Q1" all pass
