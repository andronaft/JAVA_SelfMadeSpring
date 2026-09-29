package com.zuk.minispring.beans;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertyResolverTest {

    private final PropertyResolver resolver = new PropertyResolver(properties("host", "example.com", "port", "8080"));

    @Test
    void replacesEveryPlaceholderAndKeepsTheRest() {
        assertEquals("http://example.com:8080/api", resolver.resolvePlaceholders("http://${host}:${port}/api"));
        assertEquals("no placeholders", resolver.resolvePlaceholders("no placeholders"));
    }

    @Test
    void defaultIsEverythingAfterTheFirstColon() {
        assertEquals("http://localhost", resolver.resolvePlaceholders("${proxy:http://localhost}"));
        assertEquals("", resolver.resolvePlaceholders("${missing:}"));
        assertEquals("example.com", resolver.resolvePlaceholders("${host:ignored}"));
    }

    @Test
    void missingKeyWithoutDefaultFails() {
        BeansException e = assertThrows(BeansException.class, () -> resolver.resolvePlaceholders("${user}"));
        assertTrue(e.getMessage().contains("'user'"), e.getMessage());
    }

    @Test
    void unclosedPlaceholderFails() {
        assertThrows(BeansException.class, () -> resolver.resolvePlaceholders("${host"));
    }

    @Test
    void convertsToCommonTypes() {
        assertEquals(8080, resolver.resolve("${port}", int.class));
        assertEquals(8080L, resolver.resolve("${port}", Long.class));
        assertEquals(1.5, resolver.resolve("1.5", double.class));
        assertEquals(true, resolver.resolve("true", boolean.class));
        assertEquals(Thread.State.NEW, resolver.resolve("NEW", Thread.State.class));
    }

    @Test
    void booleanMustBeTrueOrFalse() {
        assertThrows(BeansException.class, () -> resolver.resolve("yes", boolean.class));
    }

    @Test
    void unsupportedTargetTypeIsReported() {
        BeansException e = assertThrows(BeansException.class, () -> resolver.resolve("x", StringBuilder.class));
        assertTrue(e.getMessage().contains("supported types"), e.getMessage());
    }

    @Test
    void systemPropertiesOverrideApplicationProperties() {
        System.setProperty("app.greeting", "from -D");
        try {
            PropertyResolver fromClasspath = PropertyResolver.fromClasspath(getClass().getClassLoader());
            assertEquals("from -D", fromClasspath.getProperty("app.greeting"));
            assertEquals("smtp.from-file.test", fromClasspath.getProperty("mail.host"));
        } finally {
            System.clearProperty("app.greeting");
        }
    }

    private static Properties properties(String... keysAndValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }
}
