package com.github.forax.framework.mapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Objects;

public class JSONReader {
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
      throw new UnsupportedOperationException("TODO");
    }
  };

  public Object parseJSON(String text, Class<?> beanClass) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(beanClass);
    var visitor = new ToyJSONParser.JSONVisitor() {
      private BeanData beanData;
      private Object result;

      @Override
      public void value(String key, Object value) {
        // call the corresponding setter on result
        throw new UnsupportedOperationException("TODO");
      }

      @Override
      public void startObject(String key) {
        //get the beanData and store it in the field
        //create an instance and store it in result
        throw new UnsupportedOperationException("TODO");
      }

      @Override
      public void endObject(String key) {
        // do nothing
      }

      @Override
      public void startArray(String key) {
        throw new UnsupportedOperationException("Implemented later");
      }

      @Override
      public void endArray(String key) {
        throw new UnsupportedOperationException("Implemented later");
      }
    };
    ToyJSONParser.parse(text, visitor);
    return visitor.result;
  }
}
