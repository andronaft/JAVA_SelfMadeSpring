package com.zuk.minispring.aop;

import com.zuk.minispring.beans.BeanCreationException;
import com.zuk.minispring.beans.SmartInstantiationAwareBeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps every bean that has @Timed methods in a JDK dynamic proxy that measures those calls, the
 * way Spring AOP wraps beans for @Transactional or @Async. Declare it as a bean, ideally from a
 * static @Bean method, and the ApplicationContext applies it to all other beans.
 * <p>
 * Limits shared with Spring's JDK proxies: the proxy implements only the bean's interfaces, so a
 * @Timed bean needs an interface and must be injected by it; and a call the bean makes to its own
 * method ({@code this.foo()}) doesn't go through the proxy, so it isn't measured. Spring falls back
 * to CGLIB subclasses for beans without interfaces; this container has no bytecode generation.
 */
public class TimedBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {
    private static final System.Logger LOG = System.getLogger(TimedBeanPostProcessor.class.getName());

    private final TimingListener listener;
    /** Beans that already got their proxy through getEarlyBeanReference, so they aren't wrapped twice. */
    private final Map<String, Object> earlyProxyReferences = new HashMap<>();

    /** Logs each call through System.Logger. */
    public TimedBeanPostProcessor() {
        this((beanName, method, nanos) -> LOG.log(System.Logger.Level.INFO, "{0}.{1}() took {2} ms",
                beanName, method.getName(), nanos / 1_000_000.0));
    }

    public TimedBeanPostProcessor(TimingListener listener) {
        this.listener = listener;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        earlyProxyReferences.put(beanName, bean);
        return wrapIfTimed(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (earlyProxyReferences.remove(beanName) == bean) {
            return bean; // the proxy exists already; the factory keeps using it
        }
        return wrapIfTimed(bean, beanName);
    }

    private Object wrapIfTimed(Object bean, String beanName) {
        Class<?> type = bean.getClass();
        boolean hasTimedMethods = type.isAnnotationPresent(Timed.class)
                || Arrays.stream(type.getMethods()).anyMatch(method -> method.isAnnotationPresent(Timed.class));
        if (!hasTimedMethods) {
            return bean;
        }
        Class<?>[] interfaces = interfacesOf(type);
        if (interfaces.length == 0) {
            throw new BeanCreationException("Bean '" + beanName + "' (" + type.getName() + ") has @Timed methods "
                    + "but implements no interface, and a JDK proxy can only implement interfaces");
        }
        return Proxy.newProxyInstance(type.getClassLoader(), interfaces, new TimingHandler(bean, beanName));
    }

    private static Class<?>[] interfacesOf(Class<?> type) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            interfaces.addAll(Arrays.asList(current.getInterfaces()));
        }
        return interfaces.toArray(Class<?>[]::new);
    }

    private final class TimingHandler implements InvocationHandler {
        private final Object target;
        private final String beanName;
        private final Map<Method, Boolean> timed = new ConcurrentHashMap<>();

        TimingHandler(Object target, String beanName) {
            this.target = target;
            this.beanName = beanName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!timed.computeIfAbsent(method, this::isTimed)) {
                return call(method, args);
            }
            long start = System.nanoTime();
            try {
                return call(method, args);
            } finally {
                listener.onTimed(beanName, method, System.nanoTime() - start);
            }
        }

        /** The proxy sees the interface method; the annotation is on the class's implementation of it. */
        private boolean isTimed(Method method) {
            if (method.getDeclaringClass() == Object.class) {
                return false;
            }
            try {
                Method implementation = target.getClass().getMethod(method.getName(), method.getParameterTypes());
                return implementation.isAnnotationPresent(Timed.class)
                        || target.getClass().isAnnotationPresent(Timed.class);
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        private Object call(Method method, Object[] args) throws Throwable {
            try {
                method.trySetAccessible(); // for a non-public interface; a public one works without it
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause(); // the caller sees the bean's own exception, not the reflection wrapper
            }
        }
    }
}
