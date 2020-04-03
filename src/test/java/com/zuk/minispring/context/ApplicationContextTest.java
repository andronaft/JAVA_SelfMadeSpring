package com.zuk.minispring.context;

import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.OrderRepository;
import com.zuk.minispring.fixtures.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    private static final String FIXTURES = "com.zuk.minispring.fixtures";

    @BeforeEach
    void setUp() {
        LifecycleLog.clear();
    }

    @Test
    void contextRunsTheWholeLifecycleOnStartup() throws ReflectiveOperationException {
        ApplicationContext context = new ApplicationContext(FIXTURES);

        OrderService orderService = (OrderService) context.getBean("OrderService");
        assertInstanceOf(OrderRepository.class, orderService.getOrderRepository());
        assertEquals("OrderService", orderService.getBeanName());
        assertTrue(LifecycleLog.events().contains("OrderService.afterPropertiesSet"));
    }

    @Test
    void closeDestroysBeansAndPublishesContextClosedEvent() throws ReflectiveOperationException {
        ApplicationContext context = new ApplicationContext(FIXTURES);
        LifecycleLog.clear();

        context.close();

        assertTrue(LifecycleLog.events().contains("OrderRepository.preDestroy"));
        assertTrue(LifecycleLog.events().contains("OrderRepository.destroy"));
        assertTrue(LifecycleLog.events().contains("OrderService.onContextClosed"));
    }
}
