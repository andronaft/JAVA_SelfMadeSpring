package com.zuk.minispring.beans;

import java.lang.reflect.Proxy;

/** The bean exists but isn't of the requested type, usually because a post-processor replaced it with a proxy. */
public class BeanNotOfRequiredTypeException extends BeansException {
    public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
        super("Bean '" + beanName + "' should be a " + requiredType.getName() + " but is a " + actualType.getName()
                + (Proxy.isProxyClass(actualType) ? "; a JDK proxy only implements interfaces, so ask for an interface" : ""));
    }
}
