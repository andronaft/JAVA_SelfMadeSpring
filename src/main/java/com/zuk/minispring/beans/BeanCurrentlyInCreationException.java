package com.zuk.minispring.beans;

/** A bean was requested while it was still being created: its dependencies form a cycle. */
public class BeanCurrentlyInCreationException extends BeanCreationException {
    public BeanCurrentlyInCreationException(String message) {
        super(message);
    }
}
