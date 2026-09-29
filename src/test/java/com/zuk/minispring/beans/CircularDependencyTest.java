package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.cycle.Chicken;
import com.zuk.minispring.fixtures.cycle.Egg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircularDependencyTest {

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactory();
    }

    @Test
    void fieldCycleIsResolvedWithEarlyReference() {
        beanFactory.scan("com.zuk.minispring.fixtures.cycle");

        Chicken chicken = beanFactory.getBean(Chicken.class);
        Egg egg = beanFactory.getBean(Egg.class);
        assertSame(egg, chicken.getEgg());
        assertSame(chicken, egg.getChicken());
    }

    @Test
    void constructorCycleIsReportedWithItsPath() {
        beanFactory.scan("com.zuk.minispring.fixtures.ctorcycle");

        BeanCurrentlyInCreationException e = assertThrows(BeanCurrentlyInCreationException.class,
                beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().startsWith("Circular dependency: alpha -> beta -> gamma -> alpha."), e.getMessage());
        assertTrue(e.getMessage().contains("inject 'beta' into 'alpha' through a field"), e.getMessage());
    }

    @Test
    void failedCycleLeavesNoBeanHalfCreated() {
        beanFactory.scan("com.zuk.minispring.fixtures.ctorcycle");

        assertThrows(BeanCurrentlyInCreationException.class, beanFactory::preInstantiateSingletons);
        assertThrows(BeanCurrentlyInCreationException.class, beanFactory::preInstantiateSingletons);
    }

    @Test
    void wrappingABeanThatWasAlreadyInjectedEarlyIsRejected() {
        beanFactory.addPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return beanName.equals("chicken") ? new Chicken() : bean;
            }
        });
        beanFactory.scan("com.zuk.minispring.fixtures.cycle");

        BeanCurrentlyInCreationException e = assertThrows(BeanCurrentlyInCreationException.class,
                beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().contains("'chicken' was injected into other beans"), e.getMessage());
    }
}
