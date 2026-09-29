package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Qualifier;
import com.zuk.minispring.annotation.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * A field or parameter the container has to fill in, with a bean or with a @Value.
 *
 * @param type        declared type
 * @param genericType declared type with generics, to read the element type of a List
 * @param name        field or parameter name; null for a parameter compiled without -parameters
 * @param qualifier   value of @Qualifier, or null
 * @param value       expression of @Value, or null when a bean is injected
 * @param description how error messages refer to it
 */
record DependencyDescriptor(Class<?> type, Type genericType, String name, String qualifier, String value,
                            String description) {

    static DependencyDescriptor forField(Field field) {
        return new DependencyDescriptor(field.getType(), field.getGenericType(), field.getName(),
                qualifier(field.getAnnotation(Qualifier.class)), value(field.getAnnotation(Value.class)),
                "field '" + field.getName() + "'");
    }

    static DependencyDescriptor forParameter(Parameter parameter, int index, String owner) {
        String name = parameter.isNamePresent() ? parameter.getName() : null;
        String label = name != null ? "parameter '" + name + "'" : "parameter " + index;
        return new DependencyDescriptor(parameter.getType(), parameter.getParameterizedType(), name,
                qualifier(parameter.getAnnotation(Qualifier.class)), value(parameter.getAnnotation(Value.class)),
                label + " of " + owner);
    }

    private static String qualifier(Qualifier annotation) {
        return annotation != null ? annotation.value() : null;
    }

    private static String value(Value annotation) {
        return annotation != null ? annotation.value() : null;
    }
}
