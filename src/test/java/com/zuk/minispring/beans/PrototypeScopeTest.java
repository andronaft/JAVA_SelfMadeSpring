package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.fixtures.prototype.Checkout;
import com.zuk.minispring.fixtures.prototype.PriceList;
import com.zuk.minispring.fixtures.prototype.ShoppingCart;
import com.zuk.minispring.fixtures.prototype.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrototypeScopeTest {

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        LifecycleLog.clear();
        beanFactory = new BeanFactory();
        beanFactory.scan("com.zuk.minispring.fixtures.prototype");
    }

    @Test
    void everyRequestCreatesAFullyInitializedInstance() {
        ShoppingCart first = beanFactory.getBean(ShoppingCart.class);
        ShoppingCart second = beanFactory.getBean(ShoppingCart.class);

        assertNotSame(first, second);
        assertSame(beanFactory.getBean(PriceList.class), first.getPriceList());
        assertSame(first.getPriceList(), second.getPriceList());
        assertEquals(List.of("ShoppingCart.afterPropertiesSet", "ShoppingCart.afterPropertiesSet"), LifecycleLog.events());
    }

    @Test
    void everyInjectionPointGetsItsOwnInstance() {
        Checkout checkout = beanFactory.getBean(Checkout.class);

        assertNotSame(checkout.getFirstCart(), checkout.getSecondCart());
    }

    @Test
    void prototypeBeanMethodRunsOnEveryRequest() {
        assertEquals(new Ticket(1), beanFactory.getBean(Ticket.class));
        assertEquals(new Ticket(2), beanFactory.getBean("ticket"));
    }

    @Test
    void prototypesAreNotCreatedUpFrontNorDestroyed() {
        beanFactory.preInstantiateSingletons();
        assertFalse(beanFactory.getSingletonNames().contains("shoppingCart"));
        beanFactory.getBean(ShoppingCart.class);
        LifecycleLog.clear();

        beanFactory.close();

        assertEquals(List.of(), LifecycleLog.events());
    }

    @Test
    void prototypeThatNeedsItselfIsReported() {
        BeanFactory factory = new BeanFactory();
        factory.scan("com.zuk.minispring.fixtures.prototypecycle");

        BeanCurrentlyInCreationException e = assertThrows(BeanCurrentlyInCreationException.class,
                () -> factory.getBean("node"));
        assertTrue(e.getMessage().startsWith("Circular dependency: node -> node. 'node' is a prototype"), e.getMessage());
    }

    @Test
    void unknownScopeIsRejected() {
        BeanCreationException e = assertThrows(BeanCreationException.class, () -> new BeanDefinition("cart",
                ShoppingCart.class, "session", false, null, null, null));
        assertTrue(e.getMessage().contains("unknown scope 'session'"), e.getMessage());
    }
}
