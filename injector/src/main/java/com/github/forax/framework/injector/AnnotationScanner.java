package com.github.forax.framework.injector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AnnotationScanner {
    private final HashMap<Class<?>, Consumer<Class<?>>> map;

    public AnnotationScanner() {
        map = new HashMap<>();
    }

    static Stream<String> findAllJavaFilesInFolder(Path path)  {
        try {
            return Files.list(path)
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(fileName -> fileName.endsWith(".class"))
                    .map(fileName -> fileName.substring(0, fileName.length() - ".class".length()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<Class<?>> findAllClasses(String packageName, ClassLoader classLoader)  {
        var enumerates = Utils2.getResources(packageName.replace('.', '/'), classLoader);
        if (!enumerates.hasMoreElements()) {
            throw new IllegalStateException();
        }
        var classes = new ArrayList<Class<?>>();
        while (enumerates.hasMoreElements()) {
            try {
                var javaFiles = AnnotationScanner.findAllJavaFilesInFolder(Path.of(enumerates.nextElement().toURI())).toList();
                for (var javaFile : javaFiles) {
                    classes.add(Utils2.loadClass(packageName+ '.' +javaFile, classLoader));
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.unmodifiableList(classes);
    }

    public void addAction(Class<?> annotationClass, Consumer<Class<?>> action) {
        Objects.requireNonNull(annotationClass);
        Objects.requireNonNull(action);
        if (map.putIfAbsent(annotationClass, action) != null) {
            throw new IllegalStateException();
        }
    }

    public void scanClassPathPackageForAnnotations(Class<?> rootClass) {
        var packageName = rootClass.getPackageName();
        var classLoader = rootClass.getClassLoader();
        var classes = AnnotationScanner.findAllClasses(packageName, classLoader);
        for (var clazz : classes) {
            for (var annotation : clazz.getAnnotations()) {
                var action = map.get(annotation.annotationType());
                if (action != null) {
                    action.accept(clazz);
                }
            }
        }
    }

}
