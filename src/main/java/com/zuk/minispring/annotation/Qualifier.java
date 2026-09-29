package com.zuk.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On a field or parameter: inject the bean with this name or this qualifier.
 * On a bean class: give the bean a qualifier that injection points can ask for.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Qualifier {
    String value();
}
