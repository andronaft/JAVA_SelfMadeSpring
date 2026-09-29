package com.zuk.minispring.aop;

import com.zuk.minispring.beans.BeanCreationException;
import com.zuk.minispring.beans.BeanFactory;
import com.zuk.minispring.context.ApplicationContext;
import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.timed.cycle.Ping;
import com.zuk.minispring.fixtures.timed.cycle.Pong;
import com.zuk.minispring.fixtures.timed.orders.AuditApi;
import com.zuk.minispring.fixtures.timed.orders.OrderApi;
import com.zuk.minispring.fixtures.timed.orders.OrderController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimedBeanPostProcessorTest {

    private ApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new ApplicationContext("com.zuk.minispring.fixtures.timed");
        LifecycleLog.clear();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void contextAppliesThePostProcessorBeanAndDependentsReceiveTheProxy() {
        OrderApi api = context.getBean(OrderApi.class);

        assertTrue(Proxy.isProxyClass(api.getClass()));
        assertSame(api, context.getBean(OrderController.class).getApi());
    }

    @Test
    void onlyTimedMethodsAreMeasured() {
        OrderApi api = context.getBean(OrderApi.class);

        assertEquals("placed book", api.place("book"));
        assertEquals("open", api.status());

        assertEquals(List.of("timed orderApiImpl.place"), LifecycleLog.events());
    }

    @Test
    void exceptionsPassThroughUnwrappedAndAreStillMeasured() {
        OrderApi api = context.getBean(OrderApi.class);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> api.place(""));
        assertEquals("empty item", e.getMessage());
        assertEquals(List.of("timed orderApiImpl.place"), LifecycleLog.events());
    }

    @Test
    void timedOnTheClassMeasuresEveryInterfaceMethod() {
        AuditApi audit = context.getBean(AuditApi.class);

        audit.record("login");
        assertEquals(1, audit.count());

        assertEquals(List.of("timed auditApiImpl.record", "timed auditApiImpl.count"), LifecycleLog.events());
    }

    @Test
    void beanInACycleReceivesTheSameProxyAsEveryoneElse() {
        Ping ping = context.getBean(Ping.class);

        assertTrue(Proxy.isProxyClass(ping.getClass()));
        assertSame(ping, context.getBean(Pong.class).getPing());
        assertEquals("pong", ping.ping());
        assertEquals(List.of("timed pingImpl.ping"), LifecycleLog.events());
    }

    @Test
    void proxiedBeanStillReceivesContextClosedEventAndDestroyCallback() {
        context.close();

        assertEquals(List.of("OrderApiImpl.onContextClosed", "OrderApiImpl.preDestroy"), LifecycleLog.events());
    }

    @Test
    void timedBeanWithoutInterfaceFailsAtStartup() {
        BeanCreationException e = assertThrows(BeanCreationException.class,
                () -> new ApplicationContext("com.zuk.minispring.fixtures.timednointerface"));
        assertTrue(e.getMessage().contains("implements no interface"), e.getMessage());
    }

    @Test
    void worksOnABareBeanFactory() {
        List<String> calls = new ArrayList<>();
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.addPostProcessor(new TimedBeanPostProcessor((beanName, method, nanos) -> {
            assertTrue(nanos >= 0);
            calls.add(beanName + "." + method.getName());
        }));
        beanFactory.scan("com.zuk.minispring.fixtures.timed.orders");

        beanFactory.getBean(OrderApi.class).place("pen");

        assertEquals(List.of("orderApiImpl.place"), calls);
    }
}
