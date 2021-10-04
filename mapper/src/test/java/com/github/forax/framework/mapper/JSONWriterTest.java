package com.github.forax.framework.mapper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"unused", "static-method"})
public class JSONWriterTest {
/*
  @Nested
  public class Q1 {
    @Test @Tag("Q1")
    public void toJSONPrimitive() {
      var writer = new JSONWriter();
      assertAll(
          () -> assertEquals("null", writer.toJSON(null)),
          () -> assertEquals("true", writer.toJSON(true)),
          () -> assertEquals("false", writer.toJSON(false)),
          () -> assertEquals("3", writer.toJSON(3)),
          () -> assertEquals("4.0", writer.toJSON(4.0)),
          () -> assertEquals("\"foo\"", writer.toJSON("foo"))
      );
    }
  }  // end of Q1


  public static class Car {
    private final String owner;

    public Car(String owner) {
      this.owner = owner;
    }

    public String getOwner() {
      return owner;
    }
  }

  public static class Alien {
    private final String name;
    private final String planet;

    public Alien(String name, String planet) {
      this.name = name;
      this.planet = planet;
    }

    public String getName() {
      return name;
    }

    public String getPlanet() {
      return planet;
    }
  }

  @Nested
  public class Q2 {
    @Test @Tag("Q2")
    public void toJSONWithASimpleClass() {
      var writer = new JSONWriter();
      var car = new Car("Marty");
      var json = writer.toJSON(car);
      assertEquals("""
          {"owner": "Marty"}\
          """, json);
    }

    @Test @Tag("Q2")
    public void toJSONWithAClass() {
      var writer = new JSONWriter();
      var alien = new Alien("Elvis", "Proxima Centauri");
      var json = writer.toJSON(alien);
      var expected1 = """
          {"name": "Elvis", "planet": "Proxima Centauri"}\
          """;
      var expected2 = """
          {"planet": "Proxima Centauri", "name": "Elvis"}\
          """;
      assertTrue(
          json.equals(expected1) || json.equals(expected2),
          "error: " + json + "\n expects either " + expected1 + " or " + expected2
      );
    }

    @Test @Tag("Q2")
    public void toJSONEmptyClass() {
      class Empty { }
      var writer = new JSONWriter();
      var empty = new Empty();
      var json = writer.toJSON(empty);
      assertEquals("{}", json);
    }

  } // end of Q2


  public static final class Person {
    private final String firstName;
    private final String lastName;

    public Person(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    @JSONProperty("first-name")
    public String getFirstName() {
      return firstName;
    }

    @JSONProperty("last-name")
    public String getLastName() {
      return lastName;
    }
  }

  public static class StartDate {
    private final LocalDateTime time;

    public StartDate(LocalDateTime time) {
      this.time = time;
    }

    public LocalDateTime getTime() {
      return time;
    }
  }


  @Nested
  public class Q5 {
    @Test @Tag("Q5")
    public void toJSONWithConfigure() {
      var writer = new JSONWriter();
      writer.configure(LocalDateTime.class, time -> time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      assertEquals("2021-06-16T20:53:17", writer.toJSON(LocalDateTime.of(2021, 6, 16, 20, 53, 17)));
    }

    @Test @Tag("Q5")
    public void toJSONBeanWithConfigure() {
      var writer = new JSONWriter();
      writer.configure(LocalDateTime.class, time -> time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      var startDate = new StartDate(LocalDateTime.of(2021, 7, 1, 20, 7));
      var json = writer.toJSON(startDate);
      assertEquals("""
      {"time": 2021-07-01T20:07:00}\
      """, json);
    }

    @Test @Tag("Q5")
    public void configureTwice() {
      var writer = new JSONWriter();
      writer.configure(LocalTime.class, __ -> "foo");
      assertThrows(IllegalStateException.class, () -> writer.configure(LocalTime.class, __ -> "bar"));
    }

    @Test @Tag("Q5")
    public void configurePreconditions() {
      var writer = new JSONWriter();
      assertAll(
          () -> assertThrows(NullPointerException.class, () -> writer.configure(null, String::toString)),
          () -> assertThrows(NullPointerException.class, () -> writer.configure(Timestamp.class, null))
      );
    }

  } // end of Q5

  @Nested
  public class Q6 {
    @Test @Tag("Q6")
    public void toJSONWithJSONProperty() {
      var writer = new JSONWriter();
      var person = new Person("Bob", "Hunky");
      var json = writer.toJSON(person);
      assertEquals("""
          {"first-name": "Bob", "last-name": "Hunky"}\
          """,
          json);
    }

  } // end of Q6


  public static class AddressInfo {
    private boolean international;

    public boolean isInternational() {
      return international;
    }
  }

  public record PersonInfo(@JSONProperty("birth-day") MonthDay birthday, AddressInfo address) { }

  @Nested
  public class Q7 {
    @Test @Tag("Q7")
    public void toJSONWithARecord() {
      record Person(String name, int age) { }
      var writer = new JSONWriter();
      var person = new Person("Ana", 37);
      var json = writer.toJSON(person);
      assertEquals("""
          {"name": "Ana", "age": 37}\
          """,
          json);
    }

    @Test @Tag("Q7")
    public void toJSONEmptyRecord() {
      record Empty() { }
      var writer = new JSONWriter();
      var empty = new Empty();
      var json = writer.toJSON(empty);
      assertEquals("{}", json);
    }

    @Test @Tag("Q7")
    public void toJSONRecursive() {
      record Address(String street) { }
      record Person(String name, Address address) { }
      var writer = new JSONWriter();
      var person = new Person("Bob", new Address("21 Jump Street"));
      var json = writer.toJSON(person);
      assertEquals("""
          {"name": "Bob", "address": {"street": "21 Jump Street"}}\
          """,
          json);
    }

    @Test @Tag("Q7")
    public void toJSONFullExample() {
      var writer = new JSONWriter();
      writer.configure(MonthDay.class, monthDay -> writer.toJSON(monthDay.getMonth() + "-" + monthDay.getDayOfMonth()));
      var person = new PersonInfo(MonthDay.of(4, 17), new AddressInfo());
      var json = writer.toJSON(person);
      assertEquals("""
          {"birth-day": "APRIL-17", "address": {"international": false}}\
          """,
          json);
    }

  }  // end of Q7
 */
}
