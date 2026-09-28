package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.PreDestroy;
import com.zuk.minispring.annotation.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BeanFactory {
    private final Map<String, Object> singletons = new LinkedHashMap<>();
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();
    private final ClassLoader classLoader;

    public BeanFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public BeanFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Object getBean(String beanName) {
        Object bean = singletons.get(beanName);
        if (bean == null) {
            throw new NoSuchBeanDefinitionException("No bean named '" + beanName + "'");
        }
        return bean;
    }

    public <T> T getBean(Class<T> type) {
        return type.cast(singletons.get(findUniqueBeanName(type)));
    }

    public void addPostProcessor(BeanPostProcessor postProcessor) {
        postProcessors.add(postProcessor);
    }

    /** Scans the package and its subpackages and creates one instance of every @Component / @Service class. */
    public void instantiate(String basePackage) {
        for (Class<?> type : new ClassPathScanner(classLoader).scan(basePackage)) {
            if (!type.isAnnotationPresent(Component.class) && !type.isAnnotationPresent(Service.class)) {
                continue;
            }
            String beanName = type.getSimpleName();
            if (singletons.containsKey(beanName)) {
                throw new BeanCreationException("Bean name '" + beanName + "' is used by both "
                        + singletons.get(beanName).getClass().getName() + " and " + type.getName());
            }
            singletons.put(beanName, createInstance(beanName, type));
        }
    }

    /** Injects every @Autowired field with the single bean assignable to the field's type. */
    public void populateProperties() {
        for (Map.Entry<String, Object> entry : singletons.entrySet()) {
            Object bean = entry.getValue();
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                Object dependency;
                try {
                    dependency = singletons.get(findUniqueBeanName(field.getType()));
                } catch (NoSuchBeanDefinitionException e) {
                    throw new BeanCreationException("Cannot autowire field '" + field.getName()
                            + "' of bean '" + entry.getKey() + "': " + e.getMessage(), e);
                }
                try {
                    field.setAccessible(true);
                    field.set(bean, dependency);
                } catch (IllegalAccessException | RuntimeException e) {
                    throw new BeanCreationException("Cannot autowire field '" + field.getName()
                            + "' of bean '" + entry.getKey() + "'", e);
                }
            }
        }
    }

    public void injectBeanNames() {
        singletons.forEach((name, bean) -> {
            if (bean instanceof BeanNameAware aware) {
                aware.setBeanName(name);
            }
        });
    }

    /**
     * Runs post-processors around the init callback. Whatever a post-processor returns
     * replaces the bean in the registry, so it can wrap the bean in a proxy.
     */
    public void initializeBeans() {
        for (Map.Entry<String, Object> entry : singletons.entrySet()) {
            String name = entry.getKey();
            Object bean = entry.getValue();

            for (BeanPostProcessor postProcessor : postProcessors) {
                bean = applyPostProcessor(bean, postProcessor.postProcessBeforeInitialization(bean, name));
            }
            if (bean instanceof InitializingBean initializingBean) {
                try {
                    initializingBean.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw new BeanCreationException("Initialization of bean '" + name + "' failed", e);
                }
            }
            for (BeanPostProcessor postProcessor : postProcessors) {
                bean = applyPostProcessor(bean, postProcessor.postProcessAfterInitialization(bean, name));
            }
            entry.setValue(bean);
        }
    }

    /**
     * Calls @PreDestroy methods and DisposableBean.destroy() on every bean. A failing bean
     * doesn't stop the others from being destroyed; all failures are reported at the end.
     */
    public void close() {
        List<Throwable> failures = new ArrayList<>();
        singletons.forEach((name, bean) -> {
            for (Method method : bean.getClass().getMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    try {
                        method.invoke(bean);
                    } catch (InvocationTargetException e) {
                        failures.add(new BeansException("@PreDestroy method of bean '" + name + "' failed", e.getCause()));
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        failures.add(new BeansException("Cannot call @PreDestroy method of bean '" + name + "'", e));
                    }
                }
            }
            if (bean instanceof DisposableBean disposableBean) {
                try {
                    disposableBean.destroy();
                } catch (RuntimeException e) {
                    failures.add(new BeansException("destroy() of bean '" + name + "' failed", e));
                }
            }
        });
        if (!failures.isEmpty()) {
            BeansException exception = new BeansException(failures.size() + " bean(s) failed to shut down");
            failures.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    public Map<String, Object> getSingletons() {
        return Collections.unmodifiableMap(singletons);
    }

    private Object createInstance(String beanName, Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new BeanCreationException("Bean '" + beanName + "' (" + type.getName()
                    + ") needs a no-arg constructor", e);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException("Constructor of bean '" + beanName + "' threw an exception", e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new BeanCreationException("Failed to instantiate bean '" + beanName + "'", e);
        }
    }

    private String findUniqueBeanName(Class<?> type) {
        List<String> candidates = new ArrayList<>();
        singletons.forEach((name, bean) -> {
            if (type.isInstance(bean)) {
                candidates.add(name);
            }
        });
        if (candidates.isEmpty()) {
            throw new NoSuchBeanDefinitionException("No bean of type " + type.getName());
        }
        if (candidates.size() > 1) {
            throw new NoUniqueBeanDefinitionException(type, candidates);
        }
        return candidates.get(0);
    }

    /** Like Spring, a post-processor that returns null keeps the current bean. */
    private static Object applyPostProcessor(Object current, Object result) {
        return result != null ? result : current;
    }
}
