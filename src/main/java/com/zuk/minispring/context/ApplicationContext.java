package com.zuk.minispring.context;

import com.zuk.minispring.beans.BeanFactory;
import com.zuk.minispring.beans.BeanPostProcessor;
import com.zuk.minispring.beans.PropertyResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Properties;

public class ApplicationContext implements AutoCloseable {
    private final BeanFactory beanFactory;
    private boolean closed;

    /**
     * Scans the package and creates every singleton. @Value properties come from application.properties
     * on the class path and from system properties. If startup fails, the beans created so far are destroyed.
     */
    public ApplicationContext(String basePackage) {
        this(basePackage, PropertyResolver.fromClasspath(Thread.currentThread().getContextClassLoader()));
    }

    /** Like {@link #ApplicationContext(String)}, with the given properties for @Value. */
    public ApplicationContext(String basePackage, Properties properties) {
        this(basePackage, new PropertyResolver(properties));
    }

    private ApplicationContext(String basePackage, PropertyResolver propertyResolver) {
        beanFactory = new BeanFactory(Thread.currentThread().getContextClassLoader(), propertyResolver);
        try {
            beanFactory.scan(basePackage);
            registerBeanPostProcessors();
            beanFactory.preInstantiateSingletons();
        } catch (RuntimeException e) {
            try {
                beanFactory.close();
            } catch (RuntimeException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    /**
     * Creates the beans that are BeanPostProcessors before any other bean, so they can process all
     * the others, as Spring's refresh() does. Whatever those post-processors depend on is created
     * early too and misses them, which is why a post-processor is best declared by a static @Bean method.
     */
    private void registerBeanPostProcessors() {
        for (String name : beanFactory.getBeanNamesForType(BeanPostProcessor.class)) {
            beanFactory.addPostProcessor(beanFactory.getBean(name, BeanPostProcessor.class));
        }
    }

    public Object getBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

    public <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }

    public <T> T getBean(String beanName, Class<T> type) {
        return beanFactory.getBean(beanName, type);
    }

    /**
     * Publishes ContextClosedEvent while beans are still alive, then destroys them (same order as Spring).
     * Closing an already closed context does nothing.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ContextClosedEvent event = new ContextClosedEvent();
        for (String name : beanFactory.getSingletonNames()) {
            // Checked on the class behind the bean: a proxy doesn't keep the listener's type argument.
            if (listensTo(beanFactory.getType(name), ContextClosedEvent.class)) {
                @SuppressWarnings("unchecked")
                ApplicationListener<ContextClosedEvent> listener =
                        (ApplicationListener<ContextClosedEvent>) beanFactory.getBean(name);
                listener.onApplicationEvent(event);
            }
        }
        beanFactory.close();
    }

    private static boolean listensTo(Class<?> beanType, Class<?> eventType) {
        for (Class<?> type = beanType; type != null; type = type.getSuperclass()) {
            for (Type implemented : type.getGenericInterfaces()) {
                if (implemented instanceof ParameterizedType parameterized
                        && parameterized.getRawType() == ApplicationListener.class
                        && parameterized.getActualTypeArguments()[0] instanceof Class<?> listened
                        && listened.isAssignableFrom(eventType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
