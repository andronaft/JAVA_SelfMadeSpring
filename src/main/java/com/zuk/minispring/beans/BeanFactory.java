package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.PreDestroy;
import com.zuk.minispring.annotation.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Keeps bean definitions and creates beans from them on demand. A bean's dependencies are
 * created before the bean itself, so it always receives fully initialized dependencies,
 * including the objects post-processors return in their place (proxies).
 */
public class BeanFactory {
    private final Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

    // The three caches Spring's DefaultSingletonBeanRegistry uses to resolve circular references.
    /** 1st level: fully initialized singletons, in creation order. */
    private final Map<String, Object> singletonObjects = new LinkedHashMap<>();
    /** 2nd level: singletons handed out before their initialization finished, to break a cycle. */
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    /** 3rd level: factories for those early references, registered right after instantiation. */
    private final Map<String, Supplier<Object>> singletonFactories = new HashMap<>();

    /** The objects behind the singletons before any post-processor wrapped them; destroy callbacks run on these. */
    private final Map<String, Object> rawSingletons = new LinkedHashMap<>();
    /** Beans being created right now, in request order. Requesting one of them again means a cycle. */
    private final Set<String> beansInCreation = new LinkedHashSet<>();

    private final ClassLoader classLoader;
    private final PropertyResolver propertyResolver;

    public BeanFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /** @Value properties come from application.properties on the class path and from system properties. */
    public BeanFactory(ClassLoader classLoader) {
        this(classLoader, PropertyResolver.fromClasspath(classLoader));
    }

    public BeanFactory(ClassLoader classLoader, PropertyResolver propertyResolver) {
        this.classLoader = classLoader;
        this.propertyResolver = propertyResolver;
    }

    /**
     * Registers a definition for every @Component, @Service and @Configuration class in the package
     * and its subpackages, plus one for every @Bean method of the @Configuration classes.
     */
    public void scan(String basePackage) {
        for (Class<?> type : new ClassPathScanner(classLoader).scan(basePackage)) {
            if (BeanDefinitionReader.isComponent(type)) {
                BeanDefinitionReader.read(type).forEach(this::registerBeanDefinition);
            }
        }
    }

    public void registerBeanDefinition(BeanDefinition definition) {
        BeanDefinition existing = beanDefinitions.putIfAbsent(definition.name(), definition);
        if (existing != null) {
            throw new BeanCreationException("Bean name '" + definition.name() + "' is used by both "
                    + existing.beanType().getName() + " and " + definition.beanType().getName());
        }
    }

    public void addPostProcessor(BeanPostProcessor postProcessor) {
        postProcessors.add(postProcessor);
    }

    /** Creates every singleton up front, as ApplicationContext.refresh() does in Spring, so wiring errors show up at startup. */
    public void preInstantiateSingletons() {
        for (String name : List.copyOf(beanDefinitions.keySet())) {
            getBean(name);
        }
    }

    public Object getBean(String name) {
        BeanDefinition definition = getBeanDefinition(name);
        Object singleton = getSingleton(name);
        if (singleton != null) {
            return singleton;
        }
        beforeCreation(name);
        try {
            return createSingleton(name, definition);
        } finally {
            beansInCreation.remove(name);
            earlySingletonObjects.remove(name);
            singletonFactories.remove(name);
        }
    }

    public <T> T getBean(Class<T> type) {
        return getBean(findCandidate(type, null, null), type);
    }

    public <T> T getBean(String name, Class<T> type) {
        Object bean = getBean(name);
        if (!type.isInstance(bean)) {
            throw new BeanNotOfRequiredTypeException(name, type, bean.getClass());
        }
        return type.cast(bean);
    }

    public BeanDefinition getBeanDefinition(String name) {
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition == null) {
            throw new NoSuchBeanDefinitionException("No bean named '" + name + "'");
        }
        return definition;
    }

    public List<String> getBeanDefinitionNames() {
        return List.copyOf(beanDefinitions.keySet());
    }

    /** Names of the beans whose definition is assignable to the type, in registration order. */
    public List<String> getBeanNamesForType(Class<?> type) {
        List<String> names = new ArrayList<>();
        beanDefinitions.forEach((name, definition) -> {
            if (type.isAssignableFrom(definition.beanType())) {
                names.add(name);
            }
        });
        return names;
    }

    /** Names of the singletons created so far, in creation order. */
    public List<String> getSingletonNames() {
        return List.copyOf(singletonObjects.keySet());
    }

    /** The class of the object behind a bean, looking through any proxy a post-processor put in front of it. */
    public Class<?> getType(String name) {
        Object raw = rawSingletons.get(name);
        return raw != null ? raw.getClass() : getBeanDefinition(name).beanType();
    }

    /**
     * Calls @PreDestroy methods and DisposableBean.destroy() on every singleton, in reverse creation
     * order, so a bean is destroyed before the beans it depends on. A failing bean doesn't stop the
     * others from being destroyed; all failures are reported at the end.
     */
    public void close() {
        List<Throwable> failures = new ArrayList<>();
        List<String> names = new ArrayList<>(rawSingletons.keySet());
        Collections.reverse(names);
        for (String name : names) {
            destroy(name, rawSingletons.get(name), failures);
        }
        rawSingletons.clear();
        singletonObjects.clear();
        if (!failures.isEmpty()) {
            BeansException exception = new BeansException(failures.size() + " bean(s) failed to shut down");
            failures.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    /** A finished singleton, or the early reference of one that is still being created (a cycle through fields). */
    private Object getSingleton(String name) {
        Object bean = singletonObjects.get(name);
        if (bean == null && beansInCreation.contains(name)) {
            bean = earlySingletonObjects.get(name);
            if (bean == null) {
                Supplier<Object> factory = singletonFactories.remove(name);
                if (factory != null) {
                    bean = factory.get();
                    earlySingletonObjects.put(name, bean);
                }
            }
        }
        return bean;
    }

    private void beforeCreation(String name) {
        if (!beansInCreation.add(name)) {
            List<String> path = new ArrayList<>(beansInCreation);
            List<String> cycle = new ArrayList<>(path.subList(path.indexOf(name), path.size()));
            cycle.add(name);
            String requester = cycle.get(cycle.size() - 2);
            throw new BeanCurrentlyInCreationException("Circular dependency: " + String.join(" -> ", cycle)
                    + ". '" + requester + "' needs '" + name + "' before '" + name + "' is even constructed; inject '"
                    + cycle.get(1) + "' into '" + name + "' through a field instead, so '" + name + "' can be created first");
        }
    }

    private Object createSingleton(String name, BeanDefinition definition) {
        Object bean = instantiate(name, definition);
        // From here on the bean can be handed out early, before its fields are set.
        singletonFactories.put(name, () -> bean);
        populate(name, bean);
        Object exposed = initialize(name, bean);

        Object early = earlySingletonObjects.get(name);
        if (early != null) {
            if (exposed == bean) {
                exposed = early;
            } else if (exposed != early) {
                throw new BeanCurrentlyInCreationException("Bean '" + name + "' was injected into other beans "
                        + "before a post-processor replaced it with a " + exposed.getClass().getName()
                        + ", so those beans hold the raw object. Break the circular dependency");
            }
        }
        singletonObjects.put(name, exposed);
        rawSingletons.put(name, bean);
        return exposed;
    }

    private Object instantiate(String name, BeanDefinition definition) {
        if (definition.factoryMethod() != null) {
            return invokeFactoryMethod(name, definition);
        }
        Constructor<?> constructor = chooseConstructor(name, definition.beanType());
        Object[] args = resolveArguments(name, constructor.getParameters(), "the constructor");
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException("Constructor of bean '" + name + "' threw an exception", e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new BeanCreationException("Failed to instantiate bean '" + name + "'", e);
        }
    }

    /**
     * Calls the @Bean method with its parameters resolved like constructor arguments. A non-static
     * method runs on its @Configuration bean, which is created first if needed. Unlike Spring, the
     * configuration class isn't subclassed with CGLIB, so one @Bean method calling another directly
     * gets a new object; ask for the other bean as a parameter instead.
     */
    private Object invokeFactoryMethod(String name, BeanDefinition definition) {
        Method method = definition.factoryMethod();
        Object configuration = null;
        if (definition.factoryBeanName() != null) {
            configuration = getBean(definition.factoryBeanName());
            if (!method.getDeclaringClass().isInstance(configuration)) {
                configuration = rawSingletons.get(definition.factoryBeanName()); // a proxy stands in front of it
            }
        }
        Object[] args = resolveArguments(name, method.getParameters(), "@Bean method '" + method.getName() + "'");
        Object bean;
        try {
            method.setAccessible(true);
            bean = method.invoke(configuration, args);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException("@Bean method '" + method.getName() + "' threw an exception", e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new BeanCreationException("Cannot call @Bean method '" + method.getName() + "'", e);
        }
        if (bean == null) {
            throw new BeanCreationException("@Bean method '" + method.getName() + "' returned null");
        }
        return bean;
    }

    /** The only constructor, else the @Autowired one, else the no-arg one. Spring picks the same way. */
    private static Constructor<?> chooseConstructor(String name, Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        List<Constructor<?>> autowired = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .toList();
        if (autowired.size() > 1) {
            throw new BeanCreationException("Bean '" + name + "' has " + autowired.size()
                    + " @Autowired constructors; mark only one");
        }
        if (autowired.size() == 1) {
            return autowired.get(0);
        }
        return Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterCount() == 0)
                .findFirst()
                .orElseThrow(() -> new BeanCreationException("Bean '" + name + "' (" + type.getName() + ") has "
                        + constructors.length + " constructors: mark one with @Autowired or add a no-arg constructor"));
    }

    private Object[] resolveArguments(String beanName, Parameter[] parameters, String owner) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolve(beanName, DependencyDescriptor.forParameter(parameters[i], i, owner));
        }
        return args;
    }

    /** Injects @Autowired and @Value fields, starting with the ones declared in superclasses. */
    private void populate(String name, Object bean) {
        for (Class<?> type : hierarchy(bean.getClass())) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class) && !field.isAnnotationPresent(Value.class)) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new BeanCreationException("Cannot inject static field '" + field.getName()
                            + "' of bean '" + name + "'");
                }
                Object value = resolve(name, DependencyDescriptor.forField(field));
                try {
                    field.setAccessible(true);
                    field.set(bean, value);
                } catch (IllegalAccessException | RuntimeException e) {
                    throw new BeanCreationException("Cannot inject field '" + field.getName()
                            + "' of bean '" + name + "'", e);
                }
            }
        }
    }

    /** Resolves a dependency; on failure, says which bean needed it and where. */
    private Object resolve(String beanName, DependencyDescriptor dependency) {
        try {
            return resolveDependency(beanName, dependency);
        } catch (BeanCurrentlyInCreationException e) {
            throw e; // its message already names the whole cycle
        } catch (BeansException e) {
            throw new BeanCreationException("Cannot inject " + dependency.description() + " of bean '"
                    + beanName + "': " + e.getMessage(), e);
        }
    }

    private Object resolveDependency(String beanName, DependencyDescriptor dependency) {
        if (dependency.value() != null) {
            return propertyResolver.resolve(dependency.value(), dependency.type());
        }
        if (dependency.type() == List.class) {
            return resolveList(beanName, dependency);
        }
        String candidate = findCandidate(dependency.type(), dependency.qualifier(), dependency.name());
        return getBean(candidate, dependency.type());
    }

    /** Every bean of the element type, in registration order, except the bean being created (a composite can list its peers). */
    private List<Object> resolveList(String beanName, DependencyDescriptor dependency) {
        Class<?> elementType = elementType(dependency);
        List<String> names = new ArrayList<>(qualified(getBeanNamesForType(elementType), dependency.qualifier()));
        names.remove(beanName);
        if (names.isEmpty()) {
            throw new NoSuchBeanDefinitionException("No bean of type " + elementType.getName()
                    + qualifierSuffix(dependency.qualifier()));
        }
        List<Object> beans = new ArrayList<>();
        for (String name : names) {
            beans.add(getBean(name, elementType));
        }
        return List.copyOf(beans);
    }

    private static Class<?> elementType(DependencyDescriptor dependency) {
        if (dependency.genericType() instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> elementType) {
            return elementType;
        }
        throw new BeanCreationException("A List to inject must name its element type, like List<Notifier>, not "
                + dependency.genericType().getTypeName());
    }

    /**
     * Picks one bean of the type, the way Spring does: only the ones matching the qualifier (by bean name
     * or @Qualifier on the bean); if several are left, the @Primary one; else the one named like the field.
     */
    private String findCandidate(Class<?> type, String qualifier, String dependencyName) {
        List<String> candidates = qualified(getBeanNamesForType(type), qualifier);
        if (candidates.isEmpty()) {
            throw new NoSuchBeanDefinitionException("No bean of type " + type.getName() + qualifierSuffix(qualifier));
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        List<String> primary = candidates.stream().filter(name -> beanDefinitions.get(name).primary()).toList();
        if (primary.size() == 1) {
            return primary.get(0);
        }
        if (primary.size() > 1) {
            throw new NoUniqueBeanDefinitionException(type, primary, "more than one of them is marked @Primary");
        }
        if (dependencyName != null && candidates.contains(dependencyName)) {
            return dependencyName;
        }
        throw new NoUniqueBeanDefinitionException(type, candidates);
    }

    private List<String> qualified(List<String> candidates, String qualifier) {
        if (qualifier == null) {
            return candidates;
        }
        return candidates.stream()
                .filter(name -> name.equals(qualifier) || qualifier.equals(beanDefinitions.get(name).qualifier()))
                .toList();
    }

    private static String qualifierSuffix(String qualifier) {
        return qualifier != null ? " with qualifier '" + qualifier + "'" : "";
    }

    /**
     * Runs post-processors around the init callback. Whatever a post-processor returns
     * replaces the bean, so it can wrap the bean in a proxy.
     */
    private Object initialize(String name, Object bean) {
        if (bean instanceof BeanNameAware aware) {
            aware.setBeanName(name);
        }
        Object current = bean;
        for (BeanPostProcessor postProcessor : postProcessors) {
            current = applyPostProcessor(current, postProcessor.postProcessBeforeInitialization(current, name));
        }
        if (current instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
            } catch (RuntimeException e) {
                throw new BeanCreationException("Initialization of bean '" + name + "' failed", e);
            }
        }
        for (BeanPostProcessor postProcessor : postProcessors) {
            current = applyPostProcessor(current, postProcessor.postProcessAfterInitialization(current, name));
        }
        return current;
    }

    private static void destroy(String name, Object bean, List<Throwable> failures) {
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
    }

    /** The class and its superclasses, top-most first. */
    private static List<Class<?>> hierarchy(Class<?> type) {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            classes.add(0, current);
        }
        return classes;
    }

    /** Like Spring, a post-processor that returns null keeps the current bean. */
    private static Object applyPostProcessor(Object current, Object result) {
        return result != null ? result : current;
    }
}
