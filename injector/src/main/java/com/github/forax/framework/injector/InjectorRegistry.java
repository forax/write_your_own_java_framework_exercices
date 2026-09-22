package com.github.forax.framework.injector;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class InjectorRegistry {

    private final HashMap<Class<?>, Supplier<?>> map;

    public InjectorRegistry() {
        this.map = new HashMap<>();
        super();
    }

    public <T> void registerInstance(Class<T> cls, T instance) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(instance);
        if (this.map.putIfAbsent(cls, () -> instance) != null) {
            throw new IllegalStateException();
        }
    }

    public <T> T lookupInstance(Class<T> cls) {
        Objects.requireNonNull(cls);
        var r = this.map.get(cls);
        if (r == null) {
            throw new IllegalStateException();
        }
        return cls.cast(r.get());
    }

    public <T> void registerProvider(Class<T> cls, Supplier<T> provider) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(provider);
        if (this.map.putIfAbsent(cls, provider) != null) {
            throw new IllegalStateException();
        }
    }

    static List<PropertyDescriptor> findInjectableProperties(Class<?> cls) {
        var beanInfo = Utils.beanInfo(cls);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(p -> {
                    var setter = p.getWriteMethod();
                    return setter != null && setter.isAnnotationPresent(Inject.class);
                })
                .toList();
    }

    private static Constructor<?> findInjectableConstructor(Class<?> cls) {
        var constructors = Arrays.stream(cls.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();
        if (constructors.size() > 1) {
            throw new IllegalStateException();
        }
        if (constructors.size() == 1) {
            return constructors.getFirst();
        }
        return Utils.defaultConstructor(cls);
    }

    private <T> Supplier<T> getSupplier(Class<T> cls) {
        var constructor = InjectorRegistry.findInjectableConstructor(cls);
        var propertyDescriptors = InjectorRegistry.findInjectableProperties(cls);
        return () -> {
            var args = Arrays.stream(constructor.getParameterTypes())
                    .map(this::lookupInstance)
                    .toArray();
            var instance = cls.cast(Utils.newInstance(constructor, args));
            for (PropertyDescriptor pd : propertyDescriptors) {
                var dependency = this.lookupInstance(pd.getPropertyType());
                Utils.invokeMethod(instance, pd.getWriteMethod(), dependency);
            }
            return instance;
        };
    }

    public <T> void registerProviderClass(Class<T> providerClass) {
        Objects.requireNonNull(providerClass);
        var supplier = this.getSupplier(providerClass);
        if (this.map.putIfAbsent(providerClass, supplier) != null) {
            throw new IllegalStateException();
        }
    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> providerClass) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(providerClass);
        var supplier = this.getSupplier(providerClass);
        if (this.map.putIfAbsent(type, supplier) != null) {
            throw new IllegalStateException();
        }
    }
}