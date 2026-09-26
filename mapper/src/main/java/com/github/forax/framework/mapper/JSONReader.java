package com.github.forax.framework.mapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JSONReader {
  private final ArrayList<TypeMatcher> typeMatchers = new ArrayList<>();

  @FunctionalInterface
  public interface TypeMatcher {
    Optional<ObjectBuilder<?>> match(Type type);
  }

  public static abstract class TypeReference<T> {
    private final Type type;

    protected TypeReference() {
      var superclass = getClass().getGenericSuperclass();
      if (superclass instanceof ParameterizedType parameterizedType) {
        type = parameterizedType.getActualTypeArguments()[0];
      } else {
        throw new IllegalArgumentException("TypeReference must be parameterized");
      }
    }

    public Type type() {
      return type;
    }
  }

  public record ObjectBuilder<T>(Function<? super String, ? extends Type> typeProvider,
                                 Supplier<? extends T> supplier,
                                 Populater<? super T> populater,
                                 Function<? super T, ?> finisher) {
    public ObjectBuilder {
      Objects.requireNonNull(typeProvider);
      Objects.requireNonNull(supplier);
      Objects.requireNonNull(populater);
      Objects.requireNonNull(finisher);
    }
    public interface Populater<T> {
      void populate(T instance, String key, Object value);
    }

    public static ObjectBuilder<Object> bean(Class<?> beanClass) {
      Objects.requireNonNull(beanClass);
      var dataClassValue = BEAN_DATA_CLASS_VALUE.get(beanClass);
      return new ObjectBuilder<>(
              key -> dataClassValue.findProperty(key).getWriteMethod().getGenericParameterTypes()[0],
              () -> Utils.newInstance(dataClassValue.constructor()),
              (instance, key, value) -> {
                var setter = dataClassValue.findProperty(key).getWriteMethod();
                Utils.invokeMethod(instance, setter, value);
              },
              instance -> instance
      );
    }

    public static ObjectBuilder<List<Object>> list(Type elementType) {
      Objects.requireNonNull(elementType);
      return new ObjectBuilder<>(
              _ -> elementType,
              ArrayList::new,
              (list, _, value) -> list.add(value),
              Collections::unmodifiableList
      );
    }

    public static ObjectBuilder<Object[]> record(Class<?> recordClass) {
      Objects.requireNonNull(recordClass);
      var components = recordClass.getRecordComponents();
      var map = new HashMap<String, Integer>();
      for (var i = 0; i < components.length; i++) {
        map.put(components[i].getName(), i);
      }
      var constructor = Utils.canonicalConstructor(recordClass, components);
      return new ObjectBuilder<>(
              key -> {
                var index = map.get(key);
                if (index == null) {
                  throw new IllegalStateException("unknown key " + key + " for record " + recordClass.getName());
                }
                return components[index].getGenericType();
              },
              () -> new Object[components.length],
              (array, key, value) -> {
                var index = map.get(key);
                if (index == null) {
                  throw new IllegalStateException("unknown key " + key + " for record " + recordClass.getName());
                }
                array[index] = value;
              },
              array -> Utils.newInstance(constructor, array)
      );
    }
  }

  public void addTypeMatcher(TypeMatcher typeMatcher) {
    Objects.requireNonNull(typeMatcher);
    typeMatchers.add(typeMatcher);
  }

  private ObjectBuilder<?> findObjectBuilder(Type type) {
    for (var t : typeMatchers.reversed()) {
      var v = t.match(type);
      if (v.isPresent()) {
        return v.get();
      }
    }
    return ObjectBuilder.bean(Utils.erase(type));
  }

  private record Context<T>(ObjectBuilder<T> objectBuilder, T result) {
    static <T> Context<T> create(ObjectBuilder<T> builder) {
      return new Context<>(builder, builder.supplier().get());
    }

    void populate(String key, Object value) {
      objectBuilder.populater().populate(result, key, value);
    }

    Object finish() {
      return objectBuilder.finisher().apply(result);
    }
  }

  private record BeanData(Constructor<?> constructor, Map<String, PropertyDescriptor> propertyMap) {
    PropertyDescriptor findProperty(String key) {
      var property = propertyMap.get(key);
      if (property == null) {
        throw new IllegalStateException("unknown key " + key + " for bean " + constructor.getDeclaringClass().getName());
      }
      return property;
    }
  }

  private static final ClassValue<BeanData> BEAN_DATA_CLASS_VALUE = new ClassValue<>() {
    @Override
    protected BeanData computeValue(Class<?> type) {
      var beanInfo = Utils.beanInfo(type);
      var constructor = Utils.defaultConstructor(type);
      var map = Arrays.stream(beanInfo.getPropertyDescriptors())
              .filter(property -> !property.getName().equals("class"))
              .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
      return new BeanData(constructor, map);
    }
  };


  public <T> T parseJSON(String text, Class<T> beanClass) {
    Objects.requireNonNull(beanClass);
    return beanClass.cast(parseJSON(text, (Type) beanClass));
  }

  public <T> T parseJSON(String text, TypeReference<T> typeReference) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(typeReference);
    var result = (T) parseJSON(text, typeReference.type());
    return result;
  }

  public Object parseJSON(String text, Type expectedType) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(expectedType);
    var stack = new ArrayDeque<Context<?>>();
    var visitor = new ToyJSONParser.JSONVisitor() {
      private Object result;

      @Override
      public void value(String key, Object value) {
        stack.peek().populate(key, value);
      }

      @Override
      public void startObject(String key) {
        if (stack.isEmpty()) {
          stack.push(Context.create(findObjectBuilder(expectedType)));
        } else {
          var parentContext = stack.peek();
          var subType = parentContext.objectBuilder().typeProvider().apply(key);
          stack.push(Context.create(findObjectBuilder(subType)));
        }
      }

      @Override
      public void endObject(String key) {
        var childContext = stack.pop();
        var finishedInstance = childContext.finish();

        if (stack.isEmpty()) {
          result = finishedInstance;
        } else {
          stack.peek().populate(key, finishedInstance);
        }
      }

      @Override
      public void startArray(String key) {
        startObject(key);
      }

      @Override
      public void endArray(String key) {
        endObject(key);
      }
    };
    ToyJSONParser.parse(text, visitor);
    return visitor.result;
  }
}