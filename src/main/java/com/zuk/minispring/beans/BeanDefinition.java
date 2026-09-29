package com.zuk.minispring.beans;

import java.util.Objects;

/**
 * What the container knows about a bean before the bean exists. As in Spring, definitions are
 * registered first and instances are created from them later, in dependency order.
 *
 * @param name     unique bean name
 * @param beanType the class to instantiate
 */
public record BeanDefinition(String name, Class<?> beanType) {

    public BeanDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(beanType, "beanType");
    }

    String description() {
        return "'" + name + "' (" + beanType.getName() + ")";
    }
}
