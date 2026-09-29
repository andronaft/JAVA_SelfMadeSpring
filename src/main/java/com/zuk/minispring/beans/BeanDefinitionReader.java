package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Configuration;
import com.zuk.minispring.annotation.Primary;
import com.zuk.minispring.annotation.Qualifier;
import com.zuk.minispring.annotation.Service;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Turns an annotated class into bean definitions: one for the class, plus one per @Bean method of a @Configuration. */
final class BeanDefinitionReader {

    private BeanDefinitionReader() {
    }

    static boolean isComponent(Class<?> type) {
        return type.isAnnotationPresent(Component.class) || type.isAnnotationPresent(Service.class)
                || type.isAnnotationPresent(Configuration.class);
    }

    static List<BeanDefinition> read(Class<?> type) {
        String name = beanName(type);
        List<BeanDefinition> definitions = new ArrayList<>();
        definitions.add(new BeanDefinition(name, type, type.isAnnotationPresent(Primary.class), qualifier(type),
                null, null));
        if (type.isAnnotationPresent(Configuration.class)) {
            for (Method method : beanMethods(type)) {
                definitions.add(forBeanMethod(name, method));
            }
        }
        return definitions;
    }

    static String beanName(Class<?> type) {
        String explicit = explicitName(type);
        return explicit.isEmpty() ? decapitalize(type.getSimpleName()) : explicit;
    }

    /** Sorted by name, because reflection returns methods in no particular order. */
    private static List<Method> beanMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
    }

    private static BeanDefinition forBeanMethod(String configurationName, Method method) {
        if (method.getReturnType() == void.class) {
            throw new BeanCreationException("@Bean method " + method.getDeclaringClass().getName() + "."
                    + method.getName() + "() must return the bean");
        }
        String explicit = method.getAnnotation(Bean.class).value();
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        return new BeanDefinition(explicit.isEmpty() ? method.getName() : explicit, method.getReturnType(),
                method.isAnnotationPresent(Primary.class), qualifier(method), method,
                isStatic ? null : configurationName);
    }

    private static String qualifier(AnnotatedElement element) {
        Qualifier qualifier = element.getAnnotation(Qualifier.class);
        return qualifier != null ? qualifier.value() : null;
    }

    private static String explicitName(Class<?> type) {
        Component component = type.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        Service service = type.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }
        Configuration configuration = type.getAnnotation(Configuration.class);
        if (configuration != null && !configuration.value().isEmpty()) {
            return configuration.value();
        }
        return "";
    }

    /** Same rule as java.beans.Introspector: "OrderService" becomes "orderService", "URLParser" stays as is. */
    static String decapitalize(String name) {
        if (name.isEmpty() || (name.length() > 1 && Character.isUpperCase(name.charAt(0))
                && Character.isUpperCase(name.charAt(1)))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
