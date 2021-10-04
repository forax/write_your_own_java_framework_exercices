package com.github.forax.framework.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

final class Utils {
  private Utils() {
    throw new AssertionError();
  }

  public static BeanInfo beanInfo(Class<?> beanType) {
    try {
      return Introspector.getBeanInfo(beanType);
    } catch (IntrospectionException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Object invokeMethod(Object instance, Method method, Object... args) {
    try {
      return method.invoke(instance, args);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError) new IllegalAccessError().initCause(e);
    } catch (InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }

  @SuppressWarnings("unchecked")   // very wrong but works
  private static <T extends Throwable> AssertionError rethrow(Throwable cause) throws T {
    throw (T) cause;
  }

  public static Constructor<?> defaultConstructor(Class<?> beanType) {
    try {
      return beanType.getConstructor();
    } catch (NoSuchMethodException e) {
      throw (NoSuchMethodError) new NoSuchMethodError("no public default constructor " + beanType.getName()).initCause(e);
    }
  }

  public static Constructor<?> canonicalConstructor(Class<?> recordClass, RecordComponent[] components) {
    try {
      return recordClass.getConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new));
    } catch (NoSuchMethodException e) {
      throw (NoSuchMethodError) new NoSuchMethodError("no public canonical constructor " + recordClass.getName()).initCause(e);
    }
  }

  public static <T> T newInstance(Constructor<T> constructor, Object... args) {
    try {
      return constructor.newInstance(args);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    } catch (InstantiationException e) {
      throw (InstantiationError) new InstantiationError().initCause(e);
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError) new IllegalAccessError().initCause(e);
    } catch (InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }

  public static <T> List<T> reverseList(List<T> list) {
    assert list instanceof RandomAccess;
    return new AbstractList<>() {
      @Override
      public T get(int i) {
        return list.get(list.size() - 1 - i);
      }

      @Override
      public int size() {
        return list.size();
      }
    };
  }

  public static Class<?> erase(Type type) {
    /*
    // TODO use a switch on type here
    return switch(type) {
      case Class<?> clazz -> clazz;
      case ParameterizedType parameterizedType -> parameterizedType.getRawType();
      case GenericArrayType genericArrayType -> erase(genericArrayType.getGenericComponentType()).arrayType();
      case TypeVariable<?> typeVariable -> erase(typeVariable.getBounds()[0]);
      case WildcardType wildcardType -> erase(wildcardType.getLowerBounds()[0]);
      default -> throw new AssertionError("unknown type " + type.getTypeName());
    };
     */
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterizedType) {
      return (Class<?>) parameterizedType.getRawType();
    }
    if (type instanceof GenericArrayType genericArrayType) {
      return erase(genericArrayType.getGenericComponentType()).arrayType();
    }
    if (type instanceof TypeVariable<?> typeVariable) {
      return erase(typeVariable.getBounds()[0]);
    }
    if (type instanceof WildcardType wildcardType) {
      return erase(wildcardType.getLowerBounds()[0]);
    }
    throw new AssertionError("unknown type " + type.getTypeName());
  }
}