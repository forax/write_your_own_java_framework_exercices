# JDBC

## H2 Database

```
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>1.4.200</version>
</dependency>
```

[All H2 commands](https://h2database.com/html/commands.html)

## DataSource, Connection and Transaction

[DataSource](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/javax/sql/DataSource.html)


```java
DataSource dataSource = new JdbcDataSource();
dataSource.setURL("jdbc:h2:" + path);
```

```java
DataSource dataSource = new JdbcDataSource();
dataSource.setURL("jdbc:h2:mem:test");
```

[Connection](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html)

```java
try(Connection connection = dataSource.getConnection()) {
  ...  
}
```

### Transaction

[Connection.setAutoCommit](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#setAutoCommit(boolean))
[commit]([https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#commit())
[rollback](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#rollback())

```java
try(Connection connection = dataSource.getConnection()) {
  connection.setAutoCommit(false);
  try {
    ...
    connection.commit();
  } catch(...) {
    connection.rollback();
  }
}
```


### Transaction semantics

[TRANSACTION_READ_COMMITTED](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#TRANSACTION_READ_COMMITTED)
see modification from other transactions

[TRANSACTION_REPEATABLE_READ](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#TRANSACTION_REPEATABLE_READ)
don't see modification from other transactions



## Statement and PreparedStatement

[Connection.createStatement()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#createStatement())
[Connection.prepareStatement()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#prepareStatement(java.lang.String))

```java
Connection connection = ...
String sqlQuery = """
  INSERT INTO FOO (id, name) VALUES (42, 'James Bond');
""";
try(Statement statement = connection.createStatement()) {
  statement.executeUpdate(query);
}
connection.commit();
```

```java
Connection connection = ...
String sqlQuery = """
  INSERT INTO FOO (id, name) VALUES (?, ?);
""";
try(PreparedStement statement = connection.prepareStatement(query)) {
  statement.setObject(1, 42);
  statement.setObject(2, "James Bond");
  statement.executeUpdate();
}
connection.commit();
```


## Create a Table

[CREATE TABLE](https://h2database.com/html/commands.html#create_table)

[Statement.executeUpdate()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Statement.html#executeUpdate(java.lang.String))

```java
Connection connection = ...
String query = """
  CREATE TABLE FOO (
    id BIGINT,
    name VARCHAR(255),
    PRIMARY KEY (id)
  );
  """;
try(Statement statement = connection.createStatement()) {
  statement.executeUpdate(query);
}
connection.commit();
```

## Insert data

[INSERT INTO](https://h2database.com/html/commands.html#insert)

[PreparedStatement.setObject()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/PreparedStatement.html#setObject(int,java.lang.Object))

```java
Connection connection = ...
String sqlQuery = """
  INSERT INTO FOO (id, name) VALUES (?, ?);
""";
try(PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
  statement.setObject(1, 42);
  statement.setObject(2, "James Bond");
  statement.executeUpdate();
}
connection.commit();
```

# Merge data

[MERGE INTO](https://h2database.com/html/commands.html#merge_into)

```java
Connection connection = ...
String sqlQuery = """
  MERGE INTO FOO (id, name) VALUES (?, ?);
""";
try(PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
  statement.setObject(1, 42);
  statement.setObject(2, "James Bond");
  statement.executeUpdate();
}
connection.commit();
```

### Generated Primary Key

[H2 AUTO_INCREMENT](https://stackoverflow.com/questions/9353167/auto-increment-id-in-h2-database#9356818)

```java
String query = """
  CREATE TABLE FOO (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(255),
    PRIMARY KEY (id)
  );
  """;
```

[Connection.prepareStatement(query,  Statement.RETURN_GENERATED_KEYS)](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#prepareStatement(java.lang.String,int))

[Statement.getGeneratedKeys()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Statement.html#getGeneratedKeys())

[ResultSet](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/ResultSet.html)

[ResultSet.next()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/ResultSet.html#next())

[ResultSet.getObject()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/ResultSet.html#getObject(int))

```java
Connection connection = ...
String sqlQuery = """
  INSERT INTO FOO (name) VALUES (?);
""";
try(PreparedStatement statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS)) {
  statement.setObject(1, 42);
  statement.setObject(2, "James Bond");
  statement.executeUpdate(query);
  try(ResultSet resultSet = statement.getGeneratedKeys()) {
    if (resultSet.next()) {
      Long key = (Long) resultSet.getObject(1);
      ...
    }
  }
}
connection.commit();
```


## Query

[Connection.prepareStatement()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/Connection.html#prepareStatement(java.lang.String))

[PreparedStatement.executeQuery](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/PreparedStatement.html#executeQuery())

[ResultSet.next()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/ResultSet.html#next())

[ResultSet.getObject()](https://docs.oracle.com/en/java/javase/16/docs/api/java.sql/java/sql/ResultSet.html#getObject(int))


```java
Connection connection = ...
String sqlQuery = """
  SELECT (id, name) FROM FOO WHERE name = ?;
""";
try(PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
  statement.setObject(1, "James Bond");
  try(ResultSet resultSet = statement.executeQuery()) {
    while(resultSet.next()) {
      Long id = (Long) resultSet.getObject(1);
      String name = (String) resultSet.getObject(2);
      ...          
    }
  }
}
connection.commit();
```