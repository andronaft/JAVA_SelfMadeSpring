package com.zuk.minispring.context;

import com.zuk.minispring.beans.BeanCreationException;
import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.lifecycle.OrderRepository;
import com.zuk.minispring.fixtures.lifecycle.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    private static final String LIFECYCLE = "com.zuk.minispring.fixtures.lifecycle";

    @BeforeEach
    void setUp() {
        LifecycleLog.clear();
    }

    @Test
    void contextRunsTheWholeLifecycleOnStartup() {
        try (ApplicationContext context = new ApplicationContext(LIFECYCLE)) {
            OrderService orderService = context.getBean(OrderService.class);
            assertInstanceOf(OrderRepository.class, orderService.getOrderRepository());
            assertEquals("orderService", orderService.getBeanName());
            assertTrue(LifecycleLog.events().contains("OrderService.afterPropertiesSet"));
        }
    }

    @Test
    void closePublishesContextClosedEventBeforeDestroyingBeans() {
        ApplicationContext context = new ApplicationContext(LIFECYCLE);
        LifecycleLog.clear();

        context.close();

        assertEquals(List.of("OrderService.onContextClosed", "OrderRepository.preDestroy", "OrderRepository.destroy"),
                LifecycleLog.events());
    }

    @Test
    void closingTwiceRunsCallbacksOnce() {
        ApplicationContext context = new ApplicationContext(LIFECYCLE);
        LifecycleLog.clear();

        context.close();
        context.close();

        assertEquals(3, LifecycleLog.events().size(), LifecycleLog.events().toString());
    }

    @Test
    void startupFailsWithClearErrorWhenDependencyIsMissing() {
        BeanCreationException e = assertThrows(BeanCreationException.class,
                () -> new ApplicationContext("com.zuk.minispring.fixtures.missing"));
        assertTrue(e.getMessage().contains("No bean of type com.zuk.minispring.fixtures.missing.Clock"), e.getMessage());
    }

    @Test
    void failedStartupDestroysTheBeansCreatedSoFar() {
        BeanCreationException e = assertThrows(BeanCreationException.class,
                () -> new ApplicationContext("com.zuk.minispring.fixtures.failing"));

        assertTrue(e.getMessage().contains("Initialization of bean 'migrator' failed"), e.getMessage());
        assertEquals(List.of("Database.destroy"), LifecycleLog.events());
    }
}
