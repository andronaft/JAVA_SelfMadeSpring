package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.config.AuditTrail;
import com.zuk.minispring.fixtures.config.Greeting;
import com.zuk.minispring.fixtures.config.MailSettings;
import com.zuk.minispring.fixtures.config.Scheduler;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** @Configuration classes with @Bean methods, and @Value properties. */
class ConfigurationTest {

    private static final String CONFIG = "com.zuk.minispring.fixtures.config";

    @Test
    void beanMethodsRegisterBeansNamedAfterTheMethod() {
        BeanFactory beanFactory = factory("mail.host", "smtp.example.com");

        assertEquals(List.of("appConfig", "audit", "clock", "greeting", "mailSettings", "scheduler"),
                beanFactory.getBeanDefinitionNames());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), beanFactory.getBean(Clock.class).instant());
    }

    @Test
    void beanMethodParametersAreInjected() {
        BeanFactory beanFactory = factory("app.greeting", "Hi");

        Greeting greeting = beanFactory.getBean(Greeting.class);
        assertEquals("Hi", greeting.text());
        assertSame(beanFactory.getBean(Clock.class), greeting.clock());
    }

    @Test
    void beanMadeByAMethodIsASingletonLikeAnyOther() {
        BeanFactory beanFactory = factory();

        assertSame(beanFactory.getBean("greeting"), beanFactory.getBean(Greeting.class));
        assertSame(beanFactory.getBean(Clock.class), beanFactory.getBean(Scheduler.class).getClock());
    }

    @Test
    void staticBeanMethodDoesNotCreateTheConfiguration() {
        BeanFactory beanFactory = factory();

        assertInstanceOf(AuditTrail.class, beanFactory.getBean("audit"));
        assertEquals(List.of("audit"), beanFactory.getSingletonNames());
    }

    @Test
    void valuesAreConvertedToTheFieldType() {
        BeanFactory beanFactory = factory("mail.host", "smtp.example.com", "mail.port", "587",
                "mail.tls", "TRUE", "mail.mode", "LIVE");

        MailSettings mail = beanFactory.getBean(MailSettings.class);
        assertEquals("smtp.example.com", mail.getHost());
        assertEquals(587, mail.getPort());
        assertTrue(mail.isTls());
        assertEquals(MailSettings.Mode.LIVE, mail.getMode());
        assertEquals("smtp://smtp.example.com:587", mail.getUrl());
    }

    @Test
    void defaultsApplyWhenPropertiesAreMissing() {
        BeanFactory beanFactory = factory("mail.host", "localhost");

        MailSettings mail = beanFactory.getBean(MailSettings.class);
        assertEquals(25, mail.getPort());
        assertFalse(mail.isTls());
        assertEquals(MailSettings.Mode.TEST, mail.getMode());
        assertEquals(2, beanFactory.getBean(Scheduler.class).getThreads());
        assertEquals("Hello", beanFactory.getBean(Greeting.class).text());
    }

    @Test
    void missingPropertyWithoutDefaultNamesTheField() {
        BeanFactory beanFactory = factory();

        BeanCreationException e = assertThrows(BeanCreationException.class, () -> beanFactory.getBean(MailSettings.class));
        assertTrue(e.getMessage().contains("field 'host' of bean 'mailSettings'")
                && e.getMessage().contains("Could not resolve placeholder 'mail.host'"), e.getMessage());
    }

    @Test
    void valueThatDoesNotConvertIsReported() {
        BeanFactory beanFactory = factory("mail.host", "localhost", "mail.port", "twenty-five");

        BeanCreationException e = assertThrows(BeanCreationException.class, () -> beanFactory.getBean(MailSettings.class));
        assertTrue(e.getMessage().contains("Cannot convert \"twenty-five\" to int"), e.getMessage());
    }

    private static BeanFactory factory(String... keysAndValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        BeanFactory beanFactory = new BeanFactory(ConfigurationTest.class.getClassLoader(), new PropertyResolver(properties));
        beanFactory.scan(CONFIG);
        return beanFactory;
    }
}
