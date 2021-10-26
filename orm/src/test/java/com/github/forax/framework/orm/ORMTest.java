package com.github.forax.framework.orm;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("static-method")
public class ORMTest {
  /*
  @Nested
  public class Q1 {
    @Test @Tag("Q1")
    @SuppressWarnings("resource")
    public void testCurrentConnection() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        var connection = ORM.currentConnection();
        assertNotNull(connection);
      });
    }

    @Test @Tag("Q1")
    @SuppressWarnings("resource")
    public void testCurrentConnectionOutsideATransaction() {
      assertThrows(IllegalStateException.class, ORM::currentConnection);
    }

    @Test @Tag("Q1")
    @SuppressWarnings("resource")
    public void testTransactionIsNotAvailableAfterTransaction() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        // empty
      });
      assertThrows(IllegalStateException.class, ORM::currentConnection);
    }

    @Test @Tag("Q1")
    @SuppressWarnings("resource")
    public void testTransactionIsNotAvailableAfterTransactionWhichEndsWithAnException() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertThrows(SQLException.class, () -> {
        ORM.transaction(dataSource, () -> {
          throw new SQLException();
        });
      });
      assertThrows(IllegalStateException.class, ORM::currentConnection);
    }

    @Test @Tag("Q1")
    @SuppressWarnings("resource")
    public void testTransactionNull() {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> ORM.transaction(dataSource, null)),
          () -> assertThrows(NullPointerException.class, () -> ORM.transaction(null, () -> {}))
      );
    }
  }


  @Nested
  public class Q2 {

    @Test @Tag("Q2")
    @SuppressWarnings("resource")
    public void testCommitConnection() throws SQLException, IOException {
      var path = Files.createTempFile("", ".h2db");
      try {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:" + path);
        ORM.transaction(dataSource, () -> {
          var connection = ORM.currentConnection();
          var update = """
            CREATE TABLE FOO (
              ID BIGINT,
              NAME VARCHAR(255),
              PRIMARY KEY (ID)
            );
            INSERT INTO FOO (ID, NAME) VALUES (1, 'bar');
            INSERT INTO FOO (ID, NAME) VALUES (2, 'baz');
            """;

          try(var statement = connection.createStatement()) {
            statement.executeUpdate(update);
          }
          // commit
        });
        ORM.transaction(dataSource, () -> {
          var connection2 = ORM.currentConnection();
          var query2 = """
            SELECT * FROM FOO;
            """;
          record Foo(Long id, String name) {}
          var list = new ArrayList<>();
          try(var statement = connection2.createStatement()) {
            var resultSet = statement.executeQuery(query2);
            while(resultSet.next()) {
              var id = (Long) resultSet.getObject(1);
              var name = (String) resultSet.getObject(2);
              list.add(new Foo(id, name));
            }
          }
          assertEquals(List.of(new Foo(1L, "bar"), new Foo(2L, "baz")), list);
        });
      } finally {
        Files.delete(path);
      }
    }

    @Test @Tag("Q2")
    @SuppressWarnings("resource")
    public void testRollbackConnection() throws SQLException, IOException {
      var path = Files.createTempFile("", ".h2db");
      try {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:" + path);

        ORM.transaction(dataSource, () -> {
          var connection = ORM.currentConnection();
          var update = """
            CREATE TABLE FOO (
              ID BIGINT,
              NAME VARCHAR(255),
              PRIMARY KEY (ID)
            );
            """;
          try(var statement = connection.createStatement()) {
            statement.executeUpdate(update);
          }
          // commit
        });
        assertThrows(RuntimeException.class, () -> {
          ORM.transaction(dataSource, () -> {
            var connection = ORM.currentConnection();
            var update = """
            INSERT INTO FOO (ID, NAME) VALUES (1, 'bar');
            INSERT INTO FOO (ID, NAME) VALUES (2, 'baz');
            """;
            try(var statement = connection.createStatement()) {
              statement.executeUpdate(update);
            }
            throw new RuntimeException("rollback");
            // rollback
          });
        });
        ORM.transaction(dataSource, () -> {
          var connection2 = ORM.currentConnection();
          var query2 = """
            SELECT * FROM FOO;
            """;
          var list = new ArrayList<Long>();
          try(var statement = connection2.createStatement()) {
            var resultSet = statement.executeQuery(query2);
            while (resultSet.next()) {
              list.add((Long) resultSet.getObject(1));
            }
          }
          assertEquals(List.of(), list);
        });
      } finally {
        Files.delete(path);
      }
    }
  }


  @SuppressWarnings("unused")
  public static final class Furniture {
    private String name;

    public Furniture() { }  // for reflection

    @Column("CAT_NAME")
    public String getName() {
      return name;
    }
  }

  @Table("EMPTY")
  public static final class EmptyBean {
    public EmptyBean() { }
  }

  @Nested
  public class Q3 {

    @Test @Tag("Q3")
    public void testFindTableName() {
      assertAll(
          () -> assertEquals("FURNITURE", ORM.findTableName(Furniture.class)),
          () -> assertEquals("EMPTY", ORM.findTableName(EmptyBean.class))
      );
    }

    @Test @Tag("Q3")
    public void testFindColumnName() {
      var bean = Utils.beanInfo(Furniture.class);
      var property = Arrays.stream(bean.getPropertyDescriptors())
          .filter(p -> !p.getName().equals("class"))
          .findFirst().orElseThrow();
      assertEquals("CAT_NAME", ORM.findColumnName(property));
    }

    @Test @Tag("Q3")
    @SuppressWarnings("resource")
    public void testCreateTableFurniture() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Furniture.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "FURNITURE", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(
            new Column("CAT_NAME", "VARCHAR", 255, true, false)
        ), set);
      });
    }

    @Test @Tag("Q3")
    @SuppressWarnings("resource")
    public void testCreateTableEmpty() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(EmptyBean.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "EMPTY", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(), set);
      });
    }

    @Test @Tag("Q3")
    public void testCreateTableNotInTransaction() {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertThrows(IllegalStateException.class, () -> ORM.createTable(Furniture.class));
    }

    @Test @Tag("Q3")
    public void testCreateTableSQLExceptionAreCorrectlyPropagated() {
      interface Static {
        @SuppressWarnings("unused")
        @Table("weird!name")
        class InvalidTableName {
          private long id;

          @Id
          public long getId() {
            return id;
          }
          public void setId(long id) {
            this.id = id;
          }
        }
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertThrows(SQLException.class, () -> {
        ORM.transaction(dataSource, () -> ORM.createTable(Static.InvalidTableName.class));
      });
    }

    @Test @Tag("Q3")
    public void testCreateTableNull() {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertThrows(NullPointerException.class, () -> ORM.createTable(null));
    }
  }

  @SuppressWarnings("unused")
  public static final class Company {
    private Long id;
    private String name;
    private int employeeCount;

    public Company() { }  // for reflection

    public Long getId() {
      return id;
    }
    public String getName() {
      return name;
    }
    @Column("EMPLOYEE_COUNT")
    public int getEmployeeCount() {
      return employeeCount;
    }
  }

  @Nested
  public class Q4 {

    @Test @Tag("Q4")
    @SuppressWarnings("resource")
    public void testCreateTableCompany() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Company.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "COMPANY", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(
            new Column("ID", "BIGINT", 19, true, false),
            new Column("NAME", "VARCHAR", 255, true, false),
            new Column("EMPLOYEE_COUNT", "INTEGER", 10, false, false)
        ), set);
      });
    }
  }


  @SuppressWarnings("unused")
  public static final class Point {
    private Long id;
    private int x;
    private int y;

    public Point() { }  // for reflection

    @Id  // primary key
    @GeneratedValue  // auto_increment
    public Long getId() {
      return id;
    }

    public int getX() {
      return x;
    }
    public int getY() {
      return y;
    }
  }

  @SuppressWarnings("unused")
  @Table("User")
  public static final class User {
    private Long id;
    private String name;
    private int age;

    public User() {}  // for reflection

    @Id  // primary key
    @GeneratedValue  // auto_increment
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
    public int getAge() {
      return age;
    }
    public void setAge(int age) {
      this.age = age;
    }
  }

  @Nested
  public class Q5 {

    @Test @Tag("Q5")
    @SuppressWarnings("resource")
    public void testCreateTablePoint() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Point.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "POINT", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(
            new Column("ID", "BIGINT", 19, false, true),
            new Column("X", "INTEGER", 10, false, false),
            new Column("Y", "INTEGER", 10, false, false)
        ), set);
      });
    }

    @Test @Tag("Q5")
    @SuppressWarnings("resource")
    public void testCreateTableUser() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(User.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "USER", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(
            new Column("ID", "BIGINT", 19, false, true),
            new Column("AGE", "INTEGER", 10, false, false),
            new Column("NAME", "VARCHAR", 255, true, false)
        ), set);
      });
    }

    @SuppressWarnings("unused")
    @Table("USER2")
    static final class AnotherUser {
      private Long id;

      public AnotherUser() {}

      @Id
      @Column("key")
      public Long getId() {
        return id;
      }
      public void setId(Long id) {
        this.id = id;
      }
    }

    @Test @Tag("Q5")
    @SuppressWarnings("resource")
    public void testCreateTableWithColumnNameRenamed() throws SQLException {
      record Column(String name, String typeName, int size, boolean isNullable, boolean isAutoIncrement) {}
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(AnotherUser.class);
        var set = new HashSet<Column>();
        var connection = ORM.currentConnection();
        var metaData = connection.getMetaData();
        try(var resultSet = metaData.getColumns(null, null, "USER2", null)) {
          while(resultSet.next()) {
            var column = new Column(
                resultSet.getString(4),                 // COLUMN_NAME
                resultSet.getString(6),                 // TYPE_NAME
                resultSet.getInt(7),                    // COLUMN_SIZE
                resultSet.getString(18).equals("YES"),  // IS_NULLABLE
                resultSet.getString(23).equals("YES")   // IS_AUTOINCREMENT
            );
            set.add(column);
          }
        }
        assertEquals(Set.of(
            new Column("KEY", "BIGINT", 19, false, false)
        ), set);
      });
    }
  }


  @SuppressWarnings("unused")
  public static final class Person {
    private Long id;
    private String name;

    public Person() {
    }
    public Person(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    @Id
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
      return o instanceof Person person &&
          Objects.equals(id, person.id) &&
          Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }

    @Override
    public String toString() {
      return "Person{" +
          "id=" + id +
          ", name='" + name + '\'' +
          '}';
    }
  }

  @Nested
  public class Q6 {
    @Test @Tag("Q6")
    public void testFindAllEmptyTable() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        interface PersonRepository extends Repository<Person, Long> {}
        var repository = ORM.createRepository(PersonRepository.class);
        var persons = repository.findAll();
        assertEquals(List.of(), persons);
      });
    }

    @Test @Tag("Q6")
    public void testFindAllOutsideATransaction() {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      interface PersonRepository extends Repository<Person, Long> {}
      var repository = ORM.createRepository(PersonRepository.class);
      assertThrows(IllegalStateException.class, repository::findAll);
    }

    @Test @Tag("Q6")
    public void testEqualsHashCodeToStringNotSupported() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> { }
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        var repository = ORM.createRepository(PersonRepository.class);
        assertAll(
            () -> assertThrows(UnsupportedOperationException.class, () -> repository.equals(null)),
            () -> assertThrows(UnsupportedOperationException.class, repository::hashCode),
            () -> assertThrows(UnsupportedOperationException.class, repository::toString)
        );
      });
    }

    @Test @Tag("Q6")
    public void testRepositoryNotInTransaction() throws SQLException {
      interface WeirdRepository extends Repository<Person, Long> {
        void weirdMethod(String name);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(WeirdRepository.class);
      ORM.transaction(dataSource, () -> {
        assertThrows(IllegalStateException.class, () -> repository.weirdMethod("weird"));
      });
    }

    @Test @Tag("Q6")
    public void testRepositoryNull() {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      assertThrows(NullPointerException.class, () -> ORM.createRepository(null));
    }
  }

  @Nested
  public class Q7 {

    @Test @Tag("Q7")
    @SuppressWarnings("resource")
    public void testToEntityClass() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        var connection = ORM.currentConnection();
        var update = """
          INSERT INTO PERSON (ID, NAME) VALUES (42, 'scott tiger');
          """;
        try(var statement = connection.createStatement()) {
          statement.executeUpdate(update);
        }
        var query = """
            SELECT * FROM PERSON;
            """;
        Object entity;
        try(var statement = connection.createStatement()) {
          var resultSet = statement.executeQuery(query);
          assertTrue(resultSet.next());
          var beanInfo = Utils.beanInfo(Person.class);
          var constructor = Utils.defaultConstructor(Person.class);
          entity = ORM.toEntityClass(resultSet, beanInfo, constructor);
          assertFalse(resultSet.next());
        }
        assertEquals(new Person(42L, "scott tiger"), entity);
      });
    }

    @Test @Tag("Q7")
    @SuppressWarnings("resource")
    public void testFindAll() throws SQLException {
      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        var connection = ORM.currentConnection();
        var update = """
          INSERT INTO PERSON (ID, NAME) VALUES (1, 'john');
          INSERT INTO PERSON (ID, NAME) VALUES (2, 'jane');
          """;
        try(var statement = connection.createStatement()) {
          statement.executeUpdate(update);
        }
        var beanInfo = Utils.beanInfo(Person.class);
        var constructor = Utils.defaultConstructor(Person.class);
        var persons = ORM.findAll(connection, "SELECT * FROM PERSON", beanInfo, constructor);
        assertEquals(List.of(
                new Person(1L, "john"),
                new Person(2L, "jane")),
            persons);
      });
    }

    @Test @Tag("Q7")
    @SuppressWarnings("resource")
    public void testFindAllPersons() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        var connection = ORM.currentConnection();
        var update = """
          INSERT INTO PERSON (ID, NAME) VALUES (1, 'iga');
          INSERT INTO PERSON (ID, NAME) VALUES (2, 'biva');
          """;
        try(var statement = connection.createStatement()) {
          statement.executeUpdate(update);
        }

        var persons = repository.findAll();
        assertEquals(List.of(
                new Person(1L, "iga"),
                new Person(2L, "biva")),
            persons);
      });
    }

    @Test @Tag("Q7")
    public void testRepositorySQLExceptionAreCorrectlyPropagated() {
      interface Static {
        @SuppressWarnings("unused")
        @Table("weird!name")
        class InvalidTableName {
          private long id;

          @Id
          public long getId() {
            return id;
          }
          public void setId(long id) {
            this.id = id;
          }
        }
      }
      interface SimpleRepository extends Repository<Static.InvalidTableName, Long> { }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(SimpleRepository.class);
      assertThrows(SQLException.class, () -> ORM.transaction(dataSource, repository::findAll));
    }
  }

  @Nested
  class Q8 {

    @Test @Tag("Q8")
    public void testCreateSaveQuery() {
      var beanInfo = Utils.beanInfo(Person.class);
      var sqlQuery = ORM.createSaveQuery("PERSON", beanInfo);
      assertEquals("MERGE INTO PERSON (id, name) VALUES (?, ?);", sqlQuery);
    }

    @Test @Tag("Q8")
    public void testSave() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        var connection = ORM.currentConnection();
        var beanInfo = Utils.beanInfo(Person.class);
        var bean = new Person(1L, "Ana");
        ORM.save(connection, "PERSON", beanInfo, bean, null);
        var all = repository.findAll();
        assertEquals(List.of(new Person(1L, "Ana")), all);
      });
    }

    @Test @Tag("Q8")
    public void testSaveOnePerson() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        repository.save(new Person(1L, "Bob"));
        var all = repository.findAll();
        assertEquals(List.of(new Person(1L, "Bob")), all);
      });
    }

    @Test @Tag("Q8")
    public void testSaveSeveralPersons() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        LongStream.range(0, 5)
            .mapToObj(i -> new Person(i, "person" + i))
            .forEach(repository::save);

        var id = 0L;
        for(var person: repository.findAll()) {
          assertEquals(person.getId(), id);
          assertEquals(person.getName(), "person" + id);
          id++;
        }
      });
    }

  }


  @SuppressWarnings("unused")
  public static class Data {
    private String id;

    @Id
    @GeneratedValue
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  @Nested
  class Q9 {

    @Test @Tag("Q9")
    public void testSaveReturnValue() throws SQLException {
      interface DataRepository extends Repository<Data, String> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(DataRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Data.class);
        var data1 = repository.save(new Data());
        assertEquals("1", data1.id);
        var data2 = repository.save(new Data());
        assertEquals("2", data2.id);
      });
    }
  }


  @SuppressWarnings("unused")
  static final class Account {
    private Integer id;
    private long balance;

    public Account() {}
    public Account(Integer id, long balance) {
      this.id = id;
      this.balance = balance;
    }

    @Id
    public Integer getId() {
      return id;
    }
    public void setId(Integer id) {
      this.id = id;
    }

    public long getBalance() {
      return balance;
    }
    public void setBalance(long balance) {
      this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Account account && Objects.equals(id, account.id) && balance == account.balance;
    }
    @Override
    public int hashCode() {
      return Objects.hash(id, balance);
    }

    @Override
    public String toString() {
      return "Account { id=" + id + ", balance=" + balance + '}';
    }
  }

  @Nested
  class Q10 {

    @Test @Tag("Q10")
    public void testMergeValues() throws SQLException {
      interface AccountRepository extends Repository<Account, String> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(AccountRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Account.class);
        repository.save(new Account(1, 867));
        repository.save(new Account(10, -687));
        repository.save(new Account(10, 501));
        var list = repository.findAll();
        assertEquals(List.of(new Account(1, 867), new Account(10, 501)), list);
      });
    }

  }

  @SuppressWarnings("unused")
  static final class Pet {
    private Long id;
    private String name;
    private int age;

    public Pet() {}
    public Pet(Long id, String name, int age) {
      this.id = id;
      this.name = name;
      this.age = age;
    }

    @Id
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

    public int getAge() {
      return age;
    }
    public void setAge(int age) {
      this.age = age;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Pet pet &&
          Objects.equals(id, pet.id) &&
          Objects.equals(name, pet.name) &&
          age == pet.age;
    }
    @Override
    public int hashCode() {
      return Objects.hash(id, name, age);
    }

    @Override
    public String toString() {
      return "Pet { id=" + id + ", name='" + name + "', age=" + age + '}';
    }
  }

  @Nested
  public class Q11 {
    public static final class NoId { }

    @Test @Tag("Q11")
    public void testFindId() {
      var beanInfo = Utils.beanInfo(Person.class);
      var property = ORM.findId(Person.class, beanInfo);
      assertEquals("id", property.getName());
    }

    @Test @Tag("Q11")
    public void testFindById() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        repository.save(new Person(1L, "iga"));
        repository.save(new Person(2L, "biva"));
        var person = repository.findById(2L).orElseThrow();
        assertEquals(new Person(2L, "biva"), person);
      });
    }

    @Test @Tag("Q11")
    public void testFindByIdNotFound() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {}

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        repository.save(new Person(1L, "iga"));
        repository.save(new Person(2L, "biva"));
        var person = repository.findById(888L);
        assertTrue(person.isEmpty());
      });
    }

    @Test @Tag("Q11")
    public void testFindNoId() {
      var beanInfo = Utils.beanInfo(NoId.class);
      assertNull(ORM.findId(NoId.class, beanInfo));
    }

    @Test @Tag("Q11")
    public void testRepositoryClassWithNoPrimaryKey() throws SQLException {
      interface VoidRepository extends Repository<NoId, Void> { }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(VoidRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(NoId.class);
        assertEquals(List.of(), repository.findAll());
      });
    }
  }


  @Nested
  public class Q12 {

    @Test @Tag("Q12")
    public void testUserDefinedQueryAccount() throws SQLException {
      interface PersonRepository extends Repository<Account, Integer> {
        @Query("SELECT * FROM ACCOUNT")
        List<Account> findAllAccount();
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Account.class);
        repository.save(new Account(1, 123));
        repository.save(new Account(2, 567));
        var list = repository.findAllAccount();
        assertAll(
            () -> assertEquals(List.of(1, 2), list.stream().map(Account::getId).toList()),
            () -> assertEquals(List.of(123L, 567L), list.stream().map(Account::getBalance).toList())
        );
      });
    }

    @Test @Tag("Q12")
    public void testUserDefinedQueryPerson() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {
        @Query("SELECT * FROM PERSON WHERE name = ?")
        List<Person> findAllUsingAName(String name);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        repository.save(new Person(1L, "Bob"));
        repository.save(new Person(2L, "Ana"));
        repository.save(new Person(3L, "John"));
        repository.save(new Person(4L, "Bob"));
        var list = repository.findAllUsingAName("Bob");
        assertEquals(List.of(1L, 4L), list.stream().map(Person::getId).toList());
      });
    }

    @Test @Tag("Q12")
    public void testQuerySeveralParameters() throws SQLException {
      interface UserRepository extends Repository<Pet, Long> {
        @Query("SELECT * FROM PET WHERE name = ? AND age >= ?")
        List<Pet> findAllWithANameGreaterThanAge(String name, int age);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(UserRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Pet.class);
        repository.save(new Pet(1L, "Scooby", 50));
        repository.save(new Pet(2L, "Scrappy", 35));
        repository.save(new Pet(3L, "Scooby", 12));
        repository.save(new Pet(4L, "Garfield", 20));
        var list = repository.findAllWithANameGreaterThanAge("Scooby", 10);
        assertEquals(List.of(
            new Pet(1L, "Scooby", 50),
            new Pet(3L, "Scooby", 12)
        ), list);
      });
    }

  }


  @Nested
  class Q13 {

    @Test @Tag("Q13")
    public void testFindPropertyBalance() {
      var beanInfo = Utils.beanInfo(Account.class);
      var property = ORM.findProperty(Account.class, beanInfo, "balance");
      assertEquals("balance", property.getName());
    }

    @Test @Tag("Q13")
    public void testFindPropertyId() {
      var beanInfo = Utils.beanInfo(Account.class);
      var property = ORM.findProperty(Account.class, beanInfo, "id");
      assertEquals("id", property.getName());
    }

    @Test @Tag("Q13")
    public void testFindNoProperty() {
      var beanInfo = Utils.beanInfo(Account.class);
      assertThrows(IllegalStateException.class, () ->  ORM.findProperty(Account.class, beanInfo, "noproperty"));
    }

    @Test @Tag("Q13")
    public void testFindByBalance() throws SQLException {
      interface PersonRepository extends Repository<Account, Integer> {
        Optional<Account> findByBalance(String name);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Account.class);
        repository.save(new Account(1, 123));
        repository.save(new Account(2, 567));
        var account = repository.findByBalance("123").orElseThrow();
        assertEquals(1, account.getId());
      });
    }

    @Test @Tag("Q13")
    public void testFindByName() throws SQLException {
      interface PersonRepository extends Repository<Person, Long> {
        Optional<Person> findByName(String name);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(PersonRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Person.class);
        repository.save(new Person(1L, "iga"));
        repository.save(new Person(2L, "biva"));
        var person = repository.findByName("biva").orElseThrow();
        assertEquals(new Person(2L, "biva"), person);
      });
    }

    @SuppressWarnings("unused")
    static final class Country {
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

    @Test @Tag("Q13")
    public void testExample() throws SQLException {
      interface CountryRepository extends Repository<Country, Long> {
        @Query("SELECT * FROM COUNTRY WHERE NAME LIKE ?")
        List<Country> findAllWithNameLike(String name);

        Optional<Country> findByName(String name);
      }

      var dataSource = new JdbcDataSource();
      dataSource.setURL("jdbc:h2:mem:test");
      var repository = ORM.createRepository(CountryRepository.class);
      ORM.transaction(dataSource, () -> {
        ORM.createTable(Country.class);
        repository.save(new Country("France"));
        repository.save(new Country("Spain"));
        repository.save(new Country("Australia"));
        repository.save(new Country("Austria"));

        assertAll(
            () -> {
              var list = repository.findAll();
              assertEquals(List.of("France", "Spain", "Australia", "Austria"), list.stream().map(Country::getName).toList());
            },() -> {
              var list = repository.findAllWithNameLike("Aus%");
              assertEquals(List.of("Australia", "Austria"), list.stream().map(Country::getName).toList());
            }, () -> {
              var country = repository.findByName("Spain").orElseThrow();
              assertEquals("Spain", country.name);
            }, () -> {
              var country = repository.findByName("France").orElseThrow();
              var country2 = repository.findById(country.id).orElseThrow();
              assertEquals(country2, country);
            });
      });
    }

  }
  */
}