package com.zuk.minispring.beans;

import java.util.Collection;

/** More than one bean matches the requested type, so the container can't pick one. */
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> candidateNames) {
        this(type, candidateNames, "mark one with @Primary, or choose one with @Qualifier");
    }

    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> candidateNames, String hint) {
        super("Expected a single bean of type " + type.getName()
                + " but found " + candidateNames.size() + ": " + candidateNames + "; " + hint);
    }
}
