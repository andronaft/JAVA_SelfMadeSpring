package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.OrderRepository;
import com.zuk.minispring.fixtures.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeanFactoryTest {

    private static final String FIXTURES = "com.zuk.minispring.fixtures";

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        LifecycleLog.clear();
        beanFactory = new BeanFactory();
    }

    @Test
    void instantiateRegistersOnlyAnnotatedClasses() {
        beanFactory.instantiate(FIXTURES);

        assertEquals(2, beanFactory.getSingletons().size());
        assertInstanceOf(OrderService.class, beanFactory.getBean("OrderService"));
        assertInstanceOf(OrderRepository.class, beanFactory.getBean("OrderRepository"));
        assertNull(beanFactory.getBean("NotABean"));
        assertNull(beanFactory.getBean("LifecycleLog"));
    }

    @Test
    void beansAreSingletons() {
        beanFactory.instantiate(FIXTURES);

        assertSame(beanFactory.getBean("OrderService"), beanFactory.getBean("OrderService"));
    }

    @Test
    void populatePropertiesInjectsAutowiredDependency() throws ReflectiveOperationException {
        beanFactory.instantiate(FIXTURES);
        beanFactory.populateProperties();

        OrderService orderService = (OrderService) beanFactory.getBean("OrderService");
        assertSame(beanFactory.getBean("OrderRepository"), orderService.getOrderRepository());
    }

    @Test
    void injectBeanNamesSetsNameOnBeanNameAwareBeans() {
        beanFactory.instantiate(FIXTURES);
        beanFactory.injectBeanNames();

        OrderService orderService = (OrderService) beanFactory.getBean("OrderService");
        assertEquals("OrderService", orderService.getBeanName());
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
        beanFactory.instantiate(FIXTURES);
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
    void closeCallsPreDestroyThenDisposableBean() {
        beanFactory.instantiate(FIXTURES);
        beanFactory.close();

        assertEquals(List.of("OrderRepository.preDestroy", "OrderRepository.destroy"), LifecycleLog.events());
    }
}
