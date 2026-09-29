package com.zuk.minispring.beans;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds every concrete class in a package and its subpackages,
 * both in classpath directories and inside JAR files.
 */
class ClassPathScanner {

    private final ClassLoader classLoader;

    ClassPathScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    List<Class<?>> scan(String basePackage) {
        String packagePath = basePackage.replace('.', '/');
        // Sorted, so beans are registered in the same order on every file system.
        Set<String> classNames = new TreeSet<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            for (URL resource : Collections.list(resources)) {
                switch (resource.getProtocol()) {
                    case "file" -> collectFromDirectory(resource, basePackage, classNames);
                    case "jar" -> collectFromJar(resource, packagePath, classNames);
                    default -> throw new BeanCreationException("Unsupported classpath resource: " + resource);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new BeanCreationException("Failed to scan package '" + basePackage + "'", e);
        }

        List<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            Class<?> type = load(className);
            if (isConcrete(type)) {
                classes.add(type);
            }
        }
        return classes;
    }

    private void collectFromDirectory(URL resource, String basePackage, Set<String> classNames)
            throws URISyntaxException, IOException {
        Path root = Path.of(resource.toURI());
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(file -> file.toString().endsWith(".class"))
                    .map(file -> root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "."))
                    .map(relative -> basePackage + "." + stripClassSuffix(relative))
                    .forEach(classNames::add);
        }
    }

    private void collectFromJar(URL resource, String packagePath, Set<String> classNames) throws IOException {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = ((JarURLConnection) connection).getJarFile()) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (name.startsWith(packagePath + "/") && name.endsWith(".class")) {
                    classNames.add(stripClassSuffix(name).replace('/', '.'));
                }
            }
        }
    }

    private Class<?> load(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new BeanCreationException("Failed to load class " + className, e);
        }
    }

    private static String stripClassSuffix(String name) {
        return name.substring(0, name.length() - ".class".length());
    }

    private static boolean isConcrete(Class<?> type) {
        if (type.isInterface() || type.isAnnotation() || type.isEnum() || type.isAnonymousClass()) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        // A non-static inner class needs an outer instance, so it can't be a bean.
        return !type.isMemberClass() || Modifier.isStatic(type.getModifiers());
    }
}
