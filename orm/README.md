# ORM

An ORM or Object Relational Mapping is a library that tries to bridge object relational data and
object instances.

### Two kinds of ORM

There are two kinds of object relational mapping libraries, depending on if the object world
is the source of true of the SQL world is the source of true
- Libraries that maps SQL requests to Java objects,
  a library like [JDBI](https://jdbi.org/) creates one instance for each row of a query expressed in SQL.
  So two different queries on the same table may use two different classes.
- Libraries that maps Java objects to database table.
  a library like [Hibernate](https://hibernate.org/) creates multiple instances for each row one by table
  from a query expressed in SQL.

In this exercice, we will implement the first kind, because it's far easier.

### DataSource and in memory database

In Java, a [DataSource](../JDBC.md#datasource-connection-and-transaction)
is an object used to define how to access to a database (login, password, IP adress, port, etc)
and create connections from it. Note, the connection are not direct TCP connections, there are abstract connection
that reuses real TCP connection. So creating a connection / closing a connection is fast.

Most embedded databases, databases that run inside the JVM not externally as another process or on another server,
have a special test mode that creates the table of the database in memory so tables only exist for one
connection and are cleaned once the connection ends.

With H2, setting the URL to "jdbc:h2:mem:test" asks for this specific mode.

```java
var dataSource = new org.h2.jdbcx.JdbcDataSource();
dataSource.setURL("jdbc:h2:mem:test");
```

### A simple ORM

This ORM sees each row of a table as an instance of a Java bean.
It can
- create a database table from the bean definition of a Java class
- insert and update a row using the values of a bean instance
- execute a SQL query and returns a list of bean instances

First we need a way to represent a SQL transaction, that will commit all the modifications at the end
or rollback the transaction is an exception occurs. The method `ORM.transaction` takes a lambda
and run it inside a transaction.

```java
var dataSource = new org.h2.jdbcx.JdbcDataSource();
dataSource.setURL("jdbc:h2:mem:test");
ORM.transaction(dataSource, () -> {
  // start of the transaction
  ...
  // end of a transaction  
});
```

Then, we need a Java bean, here a `Country` with a field `id` and a field `name`.
The getter of the property `id` is annotated with `@Id` meaning it's the primary key and
`@GeneratedValue`meaning that the database will provide a value if one is not provided.

```java
class Country {
  private Long id;
  private String name;

  public Country() {}
  public Country(String name) {
    this.name = name;
  }

  @Id
  @GeneratedValue
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Country country &&
        Objects.equals(id, country.id) &&
        Objects.equals(name, country.name);
  }
  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "Country { id=" + id + ", name='" + name + "'}";
  }
}
```

We also need to define a repository of `Country`, a repository is an interface with methods allowing to emit SQL queries
specialized for a specific bean. This is how to declare a repository of `Country` with a primary key of type `Long`.
```java
interface CountryRepository extends Repository<Country, Long> {
  @Query("SELECT * FROM COUNTRY WHERE NAME LIKE ?")
  List<Country> findAllWithNameLike(String name);

  Optional<Country> findByName(String name);
}
```

This interface contains the user-defined methods
- The method `findAllWithNameLike` is annotated by `@Query` with the SQL query to execute.
- The method `findByName()` has no query attached but by convention if the method name starts with "finBy",
  the next word is the property used to find the instance, here this is equivalent to define the
  query "SELECT * FROM COUNTRY WHERE name = ?".

The interface `Repository` is declared like this
```java
public interface Repository<T, ID> {
  List<T> findAll();  // find all instances
  Optional<T> findById(ID id); // find a specific instance by its primary key
  T save(T entity); // insert/update a specific instance
}
```

By default, a repository provides the methods
- the method `findAll` is equivalent to a "SELECT * FROM COUNTRY"
- the method `findById` is equivalent to a "SELECT * FROM COUNTRY WHERE id = ?"
- the method `save` is equivalent to either an INSERT INTO or an UPDATE. In case of an INSERT INTO,
  the setter of the primary key is called if the value of the primary key was not defined.

The method `ORM.createRepository` returns a [dynamic proxy](../COMPANION.md#dynamic-proxy)  that implements
all the methods of the interface `Repository` and all the user defined repository.

The method `ORM.createTable` creates a new table from the class definition.

```java


var repository = ORM.createRepository(PersonRepository.class);
ORM.transaction(dataSource, () -> {
  createTable(Country.class);
  repository.save(new Country("France"));
  repository.save(new Country("Spain"));
  repository.save(new Country("Australia"));
  repository.save(new Country("Austria"));
  ...  
```


## Let's implement it

1. We want to be able to create a transaction and be able to retrieve the underlying SQL `Connection` instance
   if we are inside the transaction, such as the following code works.
   ```java
   var dataSource = ...
   transaction(dataSource, () -> {
     var connection = ORM.currentConnection();
     ...
   });
   ```
   Add a method `transaction(dataSource, block)` that takes a DataSource and a lambda, 
   create a connection from the DataSource, runs the lambda block and close the connection.
   The lambda block is a kind of Runnable that can throw a SQLException, thus is defined by the following code
   ```java
   @FunctionalInterface
   public interface TransactionBlock {
     void run() throws SQLException;
   }
   ```
   We also want a non-public method `currentConnection()` that returns the current Connection when called
   inside the transaction block or throw an exception otherwise.
   In order to store the connection associated to the current thread, you can use the class
   [ThreadLocal](../COMPANION.md#threadlocal).
   Check that the tests in the nested class "Q1" all pass.   


2. Modify the code of `transaction` to implement real SQL transactions.
   At the beginning of a transaction, the auto-commit should be set to false.
   At the end of a transaction, the method `commit()` should be called on the transaction.
   In case of an exception, the method `rollback()` should be called. If the method `rollback()` itself
   throw an exception, the initial exception should be rethrown.
   Check that the tests in the nested class "Q2" all pass.


3. We now want to implement the method `createTable(beanClass)` that take a class of a Java bean,
   find all its properties and uses the current connection to create a [SQL table](../JDBC.md#create-a-table)
   with one column per property.
   The name of the table is the name of the class apart if the class is annotated with `@Table`,
   in that case, it's the value of the annotation.
   The name of each column is the name of the property apart if the getter of the property is annotated
   with `@Column`, in that case, it's the value of the annotation.
   First create a non-public method `findTableName(beanClass)` that takes the class as argument and returns
   the name of the table. Then create a non-public method `findColumnName(property)` that takes a PropertyDescriptor
   and returns the name of a column.
   Then implement the method `createTable(beanClass)` that uses the current connection to create a table
   knowing that for now all columns are of type `VARCHAR(255)`.
   Check that the tests in the nested class "Q3" all pass.


4. We now want to find for the type of a property (a PropertyDescriptor) the corresponding SQL type.
   For that, we have a predefined Map that associate the common Java type with their SQL equivalent.
   ```java
   private static final Map<Class<?>, String> TYPE_MAPPING = Map.of(
       int.class, "INTEGER",
       Integer.class, "INTEGER",
       long.class, "BIGINT",
       Long.class, "BIGINT",
       String.class, "VARCHAR(255)"
     );
   ```
   We also want that the column that correspond to a primitive type to be declared NOT NULL.
   Modify the method `createTable(beanClass)` to declare the column with the right type.
   Check that the tests in the nested class "Q4" all pass.


5. We now want to support the annotation `@Id` and `@GeneratedValue`.
   `@Ìd`adds after the column, the text "PRIMARY KEY (foo)" if foo is the primary key and
   `@GeneratedValue` adds "AUTO_INCREMENT" at the end of a column declaration.
   For example, with the class `Country` declared above, the SQL to create the table should be
   ```sql
   CREATE TABLE COUNTRY(
     ID BIGINT AUTO_INCREMENT,
     PRIMARY KEY (id),
     NAME VARCHAR(255)
   );
   ```
   Modify the method `createTable(beanClass)` to add the support of primary key and generated value.
   Check that the tests in the nested class "Q5" all pass.


6. In order to implement `createRepository(repositoryType)` we need to create a dynamic proxy
   that will switch over the methods of the interface to implement them after having
   verified that the call to those methods is done inside a transaction.
   For now, we will implement only the method `findAll()` that returns an empty list 
   For the methods `equals`, `hashCode` and `toString`, we will throw an UnsupportedOperationException
   For all other methods, we will throw a IllegalStateException.
   Check that the tests in the nested class "Q6" all pass.


7. In order to finish the implementation `repository.findAll()`, we need several helper methods.
   - `findBeanTypeFromRepository(repositoryType)`, extract the bean class from
      the declaration of a user-defined repository.
      For example with a `CountryRepository` defined like this
      ```java
      interface CountryRepository extends Repository<Country, Long> { }
     ```
     `findBeanTypeFromRepository(CountryRepository.class)` returns `Country.class`.
     This method is already implemented.
   - `toEntityClass(resultSet, beanInfo, constructor)` takes the result of a SQL query,
     creates a bean instance using the constructor and for each column value, calls the corresponding
     property setter and returns the instance.
   - `findAll(connection, sqlQuery, beanInfo, constructor)` execute a SQL query as a
     [prepared statement](../JDBC.md#statement-and-preparedstatement)
     and uses `toEntityClass` to returns a list of instances.
   Once those methods are implemented, modify the implementation of `createRepository(repositoryType)`
   so `Repository.findAll()` fully work.
   Note: if a SQL exception occurs while executing the query, given that the method of the repository do
   not declare to throw a SQLException, the exception has to be wrapped into a runtime exception
   (you can use `UncheckedSQLException` for that)
   Check that the tests in the nested class "Q7" all pass.


8. We now want to implement the method `repository.save(bean)` that take an instance of a bean as parameter
   and insert its values into the corresponding table.
   For that, we will first implement two helper methods
   - `createSaveQuery(tableName, beanInfo)` that generate a [SQL insert](../JDBC.md#insert-data) to add
     all the values of as a prepared statement.
   - `save(connection, tableName, beanInfo, bean, idProperty)` that generate the SQL insert query using
     `createSaveQuery` and execute it with the values of the `bean`. For now, the parameter idProperty
      is useless and will always be null.
      Note: in SQL, the first column is the column 1 not 0.
   Once those methods are implemented, modify the implementation of `createRepository(repositoryType)`
   so `Repository.save(bean)` works.
   Check that the tests in the nested class "Q8" all pass.


9. We want to improve the method `repository.save(bean)` so a [generated primary key](../JDBC.md#generated-primary-key)
   computed when inserting a row is updated in the bean.
   For that, we first need a method `findId(beanType, beanInfo)` that returns the property of the primary key
   (the one annotated with `@Id`) or null otherwise.
   Then we can change the code of `save(connection, tableName, beanInfo, bean, idProperty)` to call the setter
   of the `idProperty` when the values are inserted if the property is not null
   Modify the implementation of `createRepository(repositoryType)` so `Repository.save(bean)` fully works.
   Check that the tests in the nested class "Q9" all pass.


10. We now want to update the value of a row if it already exists in the table.
    There is a simple solution for that, uses the SQL request "MERGE INTO" instead of "INSERT INTO".
    Check that the tests in the nested class "Q10" all pass.


11. We now want to implement the method `repository.findById(id)`, instead of implementing a new method
    to execute a query, we will change the method
    `findAll(connection, sqlQuery, beanInfo, constructor, args)` to takes arguments as last parameter
    and pass those arguments to the prepared statement.
    First, modify `findAll` to takes arguments as parameter, then modify the implementation of
    `createRepository(repositoryType)` so `Repository.findById(id)` works.
    Check that the tests in the nested class "Q11" all pass.


12. We now want to implement any methods declared in the user defined repository that is annotated
    with the annotation `@Query`. The idea, is again to delegate the execution of the query to `findAll`.
    Check that the tests in the nested class "Q12" all pass.


13. To finish, we want to implement all methods that start with the prefix "findBy*" followed by the name
    of a property. Thos methods takes an argument and returns the first instance that has the value of the property
    equals to the argument as an Optional or Optional.empty() if there is no result. 
    Yet again, here, we can delegate most of the work to `findAll()`.
    Note: there is a method `Introspector.decapitalize(name)` to transform a name that starts with
    an uppercase letter to a property name).
    Check that the tests in the nested class "Q13" all pass.
    