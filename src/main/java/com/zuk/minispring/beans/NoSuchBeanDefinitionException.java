package com.zuk.minispring.beans;

/** No bean matches the requested name or type. */
public class NoSuchBeanDefinitionException extends BeansException {

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
