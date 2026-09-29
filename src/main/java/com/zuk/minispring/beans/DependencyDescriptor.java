package com.zuk.minispring.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * A field or parameter the container has to fill in.
 *
 * @param type        declared type
 * @param genericType declared type with generics, to read the element type of a List
 * @param name        field or parameter name; null for a parameter compiled without -parameters
 * @param description how error messages refer to it
 */
record DependencyDescriptor(Class<?> type, Type genericType, String name, String description) {

    static DependencyDescriptor forField(Field field) {
        return new DependencyDescriptor(field.getType(), field.getGenericType(), field.getName(),
                "field '" + field.getName() + "'");
    }

    static DependencyDescriptor forParameter(Parameter parameter, int index, String owner) {
        String name = parameter.isNamePresent() ? parameter.getName() : null;
        String label = name != null ? "parameter '" + name + "'" : "parameter " + index;
        return new DependencyDescriptor(parameter.getType(), parameter.getParameterizedType(), name,
                label + " of " + owner);
    }
}
