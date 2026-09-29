package com.zuk.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A bean whose @Bean methods produce more beans. Use it for objects the container can't
 * scan: classes from libraries, or ones that need setup code.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
    /** Bean name. Defaults to the simple class name with a lower-case first letter. */
    String value() default "";
}
