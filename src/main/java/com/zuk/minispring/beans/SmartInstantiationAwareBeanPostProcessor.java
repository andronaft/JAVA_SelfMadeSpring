package com.zuk.minispring.beans;

/**
 * A post-processor that also gets a say when a bean is handed out early to break a circular
 * dependency. A post-processor that wraps beans in proxies needs this: otherwise the beans in the
 * cycle would hold the raw object while everyone else gets the proxy.
 */
public interface SmartInstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * Called with a bean that is instantiated but not yet initialized, when another bean needs it
     * early. Return the object that bean should receive, typically the same proxy that
     * postProcessAfterInitialization would create later.
     */
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }
}
