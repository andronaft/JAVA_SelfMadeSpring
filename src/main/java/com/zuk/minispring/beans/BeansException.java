package com.zuk.minispring.beans;

/** Base class for every error the container reports. Unchecked, like in Spring. */
public class BeansException extends RuntimeException {

    public BeansException(String message) {
        super(message);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
