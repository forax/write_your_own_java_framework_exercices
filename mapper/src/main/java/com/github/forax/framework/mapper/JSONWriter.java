package com.github.forax.framework.mapper;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JSONWriter {
    private final HashMap<Class<?>, Generator> map;

    public JSONWriter() {
        this.map = new HashMap<>();
        super();
    }

    @FunctionalInterface
    private interface Generator {
        String generate(JSONWriter writer, Object bean);
    }

    private static List<PropertyDescriptor> beanProperties(Class<?> type) {
        var beanInfo = Utils.beanInfo(type);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(property -> property.getReadMethod() != null && !property.getName().equals("class"))
                .toList();
    }

    private static List<PropertyDescriptor> recordProperties(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> {
                    try {
                        return new PropertyDescriptor(component.getName(), component.getAccessor(), null);
                    } catch (IntrospectionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    private static final ClassValue<Generator> CACHED_OBJ = new ClassValue<>() {
        @Override
        protected Generator computeValue(Class<?> type) {
            var properties = type.isRecord() ? recordProperties(type) : beanProperties(type);

            var generators = properties.stream()
                    .<Generator>map(property -> {
                        var getter = property.getReadMethod();
                        var name = property.getName();
                        if (getter.isAnnotationPresent(JSONProperty.class)) {
                            name = getter.getAnnotation(JSONProperty.class).value();
                        }
                        var keyPrefix = "\"" + name + "\": ";
                        return (writer, bean) -> keyPrefix + writer.toJSON(Utils.invokeMethod(bean, getter));
                    })
                    .toList();

            return (writer, bean) -> generators.stream()
                    .map(generator -> generator.generate(writer, bean))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
    };

    public <T> void configure(Class<? extends T> type, Function<? super T, String> function) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(function);
        Generator customGenerator = (_, bean) -> function.apply(type.cast(bean));
        if (map.putIfAbsent(type, customGenerator) != null) {
            throw new IllegalStateException("already exist " + type.getName());
        }
    }

    public String toJSON(Object o) {
        return switch (o) {
            case null -> "null";
            case Boolean _, Integer _, Long _, Float _, Double _ -> String.valueOf(o);
            case String s -> "\"" + s + "\"";
            case Object _ -> {
                var type = o.getClass();
                var generator = map.get(type);
                if (generator == null) {
                    generator = CACHED_OBJ.get(type);
                }
                yield generator.generate(this, o);
            }
        };
    }
}