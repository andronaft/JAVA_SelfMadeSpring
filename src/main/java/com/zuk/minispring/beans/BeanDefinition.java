package com.zuk.minispring.beans;

import java.util.Objects;

/**
 * What the container knows about a bean before the bean exists. As in Spring, definitions are
 * registered first and instances are created from them later, in dependency order.
 *
 * @param name      unique bean name
 * @param beanType  the class to instantiate
 * @param primary   wins when several beans match an injection point (@Primary)
 * @param qualifier extra name injection points can ask for with @Qualifier; may be null
 */
public record BeanDefinition(String name, Class<?> beanType, boolean primary, String qualifier) {

    public BeanDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(beanType, "beanType");
    }

    public BeanDefinition(String name, Class<?> beanType) {
        this(name, beanType, false, null);
    }
}
