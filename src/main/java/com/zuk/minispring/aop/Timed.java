package com.zuk.minispring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Measures how long a method takes. On a class: every method of its interfaces.
 * Takes effect when a {@link TimedBeanPostProcessor} is registered.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
}
