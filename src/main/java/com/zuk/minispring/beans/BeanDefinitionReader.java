package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Primary;
import com.zuk.minispring.annotation.Qualifier;
import com.zuk.minispring.annotation.Service;

import java.util.List;

/** Turns an annotated class into bean definitions. */
final class BeanDefinitionReader {

    private BeanDefinitionReader() {
    }

    static boolean isComponent(Class<?> type) {
        return type.isAnnotationPresent(Component.class) || type.isAnnotationPresent(Service.class);
    }

    static List<BeanDefinition> read(Class<?> type) {
        Qualifier qualifier = type.getAnnotation(Qualifier.class);
        return List.of(new BeanDefinition(beanName(type), type, type.isAnnotationPresent(Primary.class),
                qualifier != null ? qualifier.value() : null));
    }

    static String beanName(Class<?> type) {
        String explicit = explicitName(type);
        return explicit.isEmpty() ? decapitalize(type.getSimpleName()) : explicit;
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
