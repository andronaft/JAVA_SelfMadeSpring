package com.zuk.minispring.beans;

/** A bean could not be found on the classpath, instantiated, wired or initialized. */
public class BeanCreationException extends BeansException {

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
