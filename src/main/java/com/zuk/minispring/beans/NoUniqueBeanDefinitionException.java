package com.zuk.minispring.beans;

import java.util.Collection;

/** More than one bean matches the requested type, so the container can't pick one. */
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> candidateNames) {
        super("Expected a single bean of type " + type.getName()
                + " but found " + candidateNames.size() + ": " + candidateNames);
    }
}
