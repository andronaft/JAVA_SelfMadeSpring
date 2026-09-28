package com.zuk.minispring.context;

import com.zuk.minispring.beans.BeanFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ApplicationContext implements AutoCloseable {
    private final BeanFactory beanFactory = new BeanFactory();

    public ApplicationContext(String basePackage) {
        beanFactory.instantiate(basePackage);
        beanFactory.populateProperties();
        beanFactory.injectBeanNames();
        beanFactory.initializeBeans();
    }

    public Object getBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

    public <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }

    /** Publishes ContextClosedEvent while beans are still alive, then destroys them (same order as Spring). */
    @Override
    public void close() {
        ContextClosedEvent event = new ContextClosedEvent();
        for (Object bean : beanFactory.getSingletons().values()) {
            if (listensTo(bean, ContextClosedEvent.class)) {
                @SuppressWarnings("unchecked")
                ApplicationListener<ContextClosedEvent> listener = (ApplicationListener<ContextClosedEvent>) bean;
                listener.onApplicationEvent(event);
            }
        }
        beanFactory.close();
    }

    private static boolean listensTo(Object bean, Class<?> eventType) {
        if (!(bean instanceof ApplicationListener<?>)) {
            return false;
        }
        for (Type type : bean.getClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType parameterized
                    && parameterized.getRawType() == ApplicationListener.class
                    && parameterized.getActualTypeArguments()[0] == eventType) {
                return true;
            }
        }
        return false;
    }
}
