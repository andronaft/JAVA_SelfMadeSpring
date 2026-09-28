package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.iface.CardPaymentGateway;
import com.zuk.minispring.fixtures.iface.CheckoutService;
import com.zuk.minispring.fixtures.iface.PaymentGateway;
import com.zuk.minispring.fixtures.lifecycle.OrderRepository;
import com.zuk.minispring.fixtures.lifecycle.OrderService;
import com.zuk.minispring.fixtures.proxy.Greeter;
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
    void instantiateRegistersOnlyAnnotatedClasses() {
        beanFactory.instantiate(LIFECYCLE);

        assertEquals(2, beanFactory.getSingletons().size());
        assertInstanceOf(OrderService.class, beanFactory.getBean("OrderService"));
        assertInstanceOf(OrderRepository.class, beanFactory.getBean("OrderRepository"));
        assertThrows(NoSuchBeanDefinitionException.class, () -> beanFactory.getBean("NotABean"));
    }

    @Test
    void instantiateScansSubpackagesAndSkipsAbstractClasses() {
        beanFactory.instantiate("com.zuk.minispring.fixtures.scan");

        assertEquals(2, beanFactory.getSingletons().size());
        assertInstanceOf(RootBean.class, beanFactory.getBean("RootBean"));
        assertInstanceOf(NestedBean.class, beanFactory.getBean("NestedBean"));
    }

    @Test
    void beansAreSingletons() {
        beanFactory.instantiate(LIFECYCLE);

        assertSame(beanFactory.getBean("OrderService"), beanFactory.getBean(OrderService.class));
    }

    @Test
    void getBeanByTypeFailsWhenNothingMatches() {
        beanFactory.instantiate(LIFECYCLE);

        assertThrows(NoSuchBeanDefinitionException.class, () -> beanFactory.getBean(Runnable.class));
    }

    @Test
    void populatePropertiesInjectsFieldWithoutSetter() {
        beanFactory.instantiate(LIFECYCLE);
        beanFactory.populateProperties();

        OrderService orderService = beanFactory.getBean(OrderService.class);
        assertSame(beanFactory.getBean(OrderRepository.class), orderService.getOrderRepository());
    }

    @Test
    void populatePropertiesInjectsImplementationOfInterface() {
        beanFactory.instantiate("com.zuk.minispring.fixtures.iface");
        beanFactory.populateProperties();

        assertEquals("paid 10 by card", beanFactory.getBean(CheckoutService.class).checkout(10));
        assertInstanceOf(CardPaymentGateway.class, beanFactory.getBean(PaymentGateway.class));
    }

    @Test
    void populatePropertiesFailsWhenSeveralBeansMatch() {
        beanFactory.instantiate("com.zuk.minispring.fixtures.ambiguous");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::populateProperties);
        assertInstanceOf(NoUniqueBeanDefinitionException.class, e.getCause());
        assertTrue(e.getMessage().contains("EmailNotifier") && e.getMessage().contains("SmsNotifier"), e.getMessage());
    }

    @Test
    void populatePropertiesFailsWhenDependencyIsMissing() {
        beanFactory.instantiate("com.zuk.minispring.fixtures.missing");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::populateProperties);
        assertInstanceOf(NoSuchBeanDefinitionException.class, e.getCause());
        assertTrue(e.getMessage().contains("'clock'") && e.getMessage().contains("ReportService"), e.getMessage());
    }

    @Test
    void injectBeanNamesSetsNameOnBeanNameAwareBeans() {
        beanFactory.instantiate(LIFECYCLE);
        beanFactory.injectBeanNames();

        assertEquals("OrderService", beanFactory.getBean(OrderService.class).getBeanName());
    }

    @Test
    void initializeBeansRunsPostProcessorsAroundAfterPropertiesSet() {
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
        beanFactory.instantiate(LIFECYCLE);
        beanFactory.initializeBeans();

        List<String> events = LifecycleLog.events();
        int before = events.indexOf("before:OrderService");
        int init = events.indexOf("OrderService.afterPropertiesSet");
        int after = events.indexOf("after:OrderService");
        assertTrue(before >= 0 && before < init && init < after, "Unexpected order: " + events);
        assertTrue(events.contains("before:OrderRepository"));
        assertTrue(events.contains("after:OrderRepository"));
    }

    @Test
    void beanReturnedByPostProcessorReplacesOriginal() {
        beanFactory.addPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                Greeter target = (Greeter) bean;
                return Proxy.newProxyInstance(Greeter.class.getClassLoader(), new Class<?>[]{Greeter.class},
                        (proxy, method, args) -> method.invoke(target, args).toString().toUpperCase());
            }
        });
        beanFactory.instantiate("com.zuk.minispring.fixtures.proxy");
        beanFactory.initializeBeans();

        Greeter greeter = beanFactory.getBean(Greeter.class);
        assertTrue(Proxy.isProxyClass(greeter.getClass()));
        assertEquals("HELLO, ANNA", greeter.greet("Anna"));
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
        beanFactory.instantiate(LIFECYCLE);
        beanFactory.initializeBeans();

        assertInstanceOf(OrderService.class, beanFactory.getBean("OrderService"));
    }

    @Test
    void closeCallsPreDestroyThenDisposableBean() {
        beanFactory.instantiate(LIFECYCLE);
        beanFactory.close();

        assertEquals(List.of("OrderRepository.preDestroy", "OrderRepository.destroy"), LifecycleLog.events());
    }
}
