package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.iface.CardPaymentGateway;
import com.zuk.minispring.fixtures.iface.CheckoutService;
import com.zuk.minispring.fixtures.iface.PaymentGateway;
import com.zuk.minispring.fixtures.inherited.AuditLog;
import com.zuk.minispring.fixtures.inherited.UserController;
import com.zuk.minispring.fixtures.lifecycle.OrderRepository;
import com.zuk.minispring.fixtures.lifecycle.OrderService;
import com.zuk.minispring.fixtures.proxy.EnglishGreeter;
import com.zuk.minispring.fixtures.proxy.Greeter;
import com.zuk.minispring.fixtures.proxy.GreetingService;
import com.zuk.minispring.fixtures.scan.RootBean;
import com.zuk.minispring.fixtures.scan.sub.NestedBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeanFactoryTest {

    private static final String LIFECYCLE = "com.zuk.minispring.fixtures.lifecycle";

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        LifecycleLog.clear();
        beanFactory = new BeanFactory();
    }

    @Test
    void scanRegistersOnlyAnnotatedClassesUnderSpringStyleNames() {
        beanFactory.scan(LIFECYCLE);

        assertEquals(List.of("orderRepository", "orderService"), beanFactory.getBeanDefinitionNames());
        assertInstanceOf(OrderService.class, beanFactory.getBean("orderService"));
        assertThrows(NoSuchBeanDefinitionException.class, () -> beanFactory.getBean("notABean"));
    }

    @Test
    void scanCoversSubpackagesAndSkipsAbstractClasses() {
        beanFactory.scan("com.zuk.minispring.fixtures.scan");

        assertEquals(List.of("rootBean", "nestedBean"), beanFactory.getBeanDefinitionNames());
        assertInstanceOf(RootBean.class, beanFactory.getBean("rootBean"));
        assertInstanceOf(NestedBean.class, beanFactory.getBean("nestedBean"));
    }

    @Test
    void scanningOnlyRegistersDefinitionsBeansAreCreatedOnDemand() {
        beanFactory.scan(LIFECYCLE);
        assertEquals(List.of(), beanFactory.getSingletonNames());

        beanFactory.getBean(OrderService.class);

        // The dependency is finished first.
        assertEquals(List.of("orderRepository", "orderService"), beanFactory.getSingletonNames());
    }

    @Test
    void beansAreSingletons() {
        beanFactory.scan(LIFECYCLE);

        assertSame(beanFactory.getBean("orderService"), beanFactory.getBean(OrderService.class));
    }

    @Test
    void getBeanByTypeFailsWhenNothingMatches() {
        beanFactory.scan(LIFECYCLE);

        assertThrows(NoSuchBeanDefinitionException.class, () -> beanFactory.getBean(Runnable.class));
    }

    @Test
    void autowiredFieldIsInjectedWithoutSetter() {
        beanFactory.scan(LIFECYCLE);

        OrderService orderService = beanFactory.getBean(OrderService.class);
        assertSame(beanFactory.getBean(OrderRepository.class), orderService.getOrderRepository());
    }

    @Test
    void autowiredFieldDeclaredInSuperclassIsInjected() {
        beanFactory.scan("com.zuk.minispring.fixtures.inherited");

        assertSame(beanFactory.getBean(AuditLog.class), beanFactory.getBean(UserController.class).getAuditLog());
    }

    @Test
    void interfaceIsResolvedToItsImplementation() {
        beanFactory.scan("com.zuk.minispring.fixtures.iface");

        assertEquals("paid 10 by card", beanFactory.getBean(CheckoutService.class).checkout(10));
        assertInstanceOf(CardPaymentGateway.class, beanFactory.getBean(PaymentGateway.class));
    }

    @Test
    void injectionFailsWhenSeveralBeansMatch() {
        beanFactory.scan("com.zuk.minispring.fixtures.ambiguous");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertInstanceOf(NoUniqueBeanDefinitionException.class, e.getCause());
        assertTrue(e.getMessage().contains("emailNotifier") && e.getMessage().contains("smsNotifier"), e.getMessage());
    }

    @Test
    void injectionFailsWhenDependencyIsMissing() {
        beanFactory.scan("com.zuk.minispring.fixtures.missing");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertInstanceOf(NoSuchBeanDefinitionException.class, e.getCause());
        assertTrue(e.getMessage().contains("field 'clock'") && e.getMessage().contains("'reportService'"), e.getMessage());
    }

    @Test
    void beanNameAwareBeansReceiveTheirName() {
        beanFactory.scan(LIFECYCLE);

        assertEquals("orderService", beanFactory.getBean(OrderService.class).getBeanName());
    }

    @Test
    void postProcessorsRunAroundAfterPropertiesSet() {
        beanFactory.addPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                LifecycleLog.record("before:" + beanName);
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                LifecycleLog.record("after:" + beanName);
                return bean;
            }
        });
        beanFactory.scan(LIFECYCLE);
        beanFactory.preInstantiateSingletons();

        assertEquals(List.of("before:orderRepository", "after:orderRepository",
                        "before:orderService", "OrderService.afterPropertiesSet", "after:orderService"),
                LifecycleLog.events());
    }

    @Test
    void beanReturnedByPostProcessorReplacesOriginalAndIsWhatDependentsReceive() {
        beanFactory.addPostProcessor(upperCasingGreeterProxy());
        beanFactory.scan("com.zuk.minispring.fixtures.proxy");

        Greeter greeter = beanFactory.getBean(Greeter.class);
        assertTrue(Proxy.isProxyClass(greeter.getClass()));
        assertEquals("HELLO, ANNA", greeter.greet("Anna"));
        assertSame(greeter, beanFactory.getBean(GreetingService.class).getGreeter());
    }

    @Test
    void askingForTheClassBehindAProxyExplainsWhyItFails() {
        beanFactory.addPostProcessor(upperCasingGreeterProxy());
        beanFactory.scan("com.zuk.minispring.fixtures.proxy");

        BeanNotOfRequiredTypeException e = assertThrows(BeanNotOfRequiredTypeException.class,
                () -> beanFactory.getBean(EnglishGreeter.class));
        assertTrue(e.getMessage().contains("ask for an interface"), e.getMessage());
    }

    @Test
    void destroyCallbacksRunOnTheObjectBehindAProxy() {
        beanFactory.addPostProcessor(upperCasingGreeterProxy());
        beanFactory.scan("com.zuk.minispring.fixtures.proxy");
        beanFactory.preInstantiateSingletons();

        beanFactory.close();

        assertEquals(List.of("EnglishGreeter.preDestroy"), LifecycleLog.events());
    }

    @Test
    void postProcessorReturningNullKeepsCurrentBean() {
        beanFactory.addPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return null;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return null;
            }
        });
        beanFactory.scan(LIFECYCLE);

        assertInstanceOf(OrderService.class, beanFactory.getBean("orderService"));
    }

    @Test
    void closeCallsPreDestroyThenDisposableBean() {
        beanFactory.scan(LIFECYCLE);
        beanFactory.preInstantiateSingletons();
        LifecycleLog.clear();

        beanFactory.close();

        assertEquals(List.of("OrderRepository.preDestroy", "OrderRepository.destroy"), LifecycleLog.events());
    }

    @Test
    void closeDestroysOnlyOnce() {
        beanFactory.scan(LIFECYCLE);
        beanFactory.preInstantiateSingletons();
        LifecycleLog.clear();

        beanFactory.close();
        beanFactory.close();

        assertEquals(List.of("OrderRepository.preDestroy", "OrderRepository.destroy"), LifecycleLog.events());
    }

    @Test
    void twoBeansWithTheSameNameAreRejected() {
        beanFactory.registerBeanDefinition(new BeanDefinition("clock", Object.class));

        BeanCreationException e = assertThrows(BeanCreationException.class,
                () -> beanFactory.registerBeanDefinition(new BeanDefinition("clock", String.class)));
        assertTrue(e.getMessage().contains("java.lang.Object") && e.getMessage().contains("java.lang.String"), e.getMessage());
    }

    private static BeanPostProcessor upperCasingGreeterProxy() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof Greeter target)) {
                    return bean;
                }
                return Proxy.newProxyInstance(Greeter.class.getClassLoader(), new Class<?>[]{Greeter.class},
                        (proxy, method, args) -> method.invoke(target, args).toString().toUpperCase());
            }
        };
    }
}
