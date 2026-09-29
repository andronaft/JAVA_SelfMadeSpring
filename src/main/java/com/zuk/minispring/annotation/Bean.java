package com.zuk.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On a method of a @Configuration class: the returned object is a bean. The method's parameters
 * are injected like constructor parameters. A static method doesn't need the configuration
 * instance, which matters for BeanPostProcessors.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    /** Bean name. Defaults to the method name. */
    String value() default "";
}
