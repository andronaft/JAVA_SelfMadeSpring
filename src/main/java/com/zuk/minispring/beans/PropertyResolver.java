package com.zuk.minispring.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * Resolves {@code ${key}} and {@code ${key:default}} placeholders for @Value and converts
 * the result to the type of the field or parameter.
 */
public class PropertyResolver {
    private final Properties properties = new Properties();

    public PropertyResolver(Properties properties) {
        this.properties.putAll(properties);
    }

    /** application.properties from the classpath root, if there is one; system properties (-Dkey=value) override it. */
    public static PropertyResolver fromClasspath(ClassLoader classLoader) {
        Properties properties = new Properties();
        try (InputStream in = classLoader.getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new BeansException("Cannot read application.properties", e);
        }
        properties.putAll(System.getProperties());
        return new PropertyResolver(properties);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /** Replaces every placeholder in the text. A missing key without a default is an error, as in Spring. */
    public String resolvePlaceholders(String text) {
        StringBuilder result = new StringBuilder();
        int from = 0;
        int start;
        while ((start = text.indexOf("${", from)) >= 0) {
            int end = text.indexOf('}', start);
            if (end < 0) {
                throw new BeansException("Placeholder is not closed in \"" + text + "\"");
            }
            String placeholder = text.substring(start + 2, end);
            int colon = placeholder.indexOf(':');
            String key = colon < 0 ? placeholder : placeholder.substring(0, colon);
            String value = properties.getProperty(key);
            if (value == null) {
                if (colon < 0) {
                    throw new BeansException("Could not resolve placeholder '" + key + "' in \"" + text + "\"");
                }
                value = placeholder.substring(colon + 1);
            }
            result.append(text, from, start).append(value);
            from = end + 1;
        }
        return result.append(text.substring(from)).toString();
    }

    /** Resolves the placeholders in a @Value expression and converts the text to the target type. */
    public Object resolve(String expression, Class<?> targetType) {
        return convert(resolvePlaceholders(expression), targetType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object convert(String value, Class<?> type) {
        if (type == String.class || type == Object.class) {
            return value;
        }
        String text = value.trim();
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(text);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(text);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(text);
            }
            if (type == boolean.class || type == Boolean.class) {
                return parseBoolean(text);
            }
            if (type.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) type, text);
            }
        } catch (IllegalArgumentException e) {
            throw new BeansException("Cannot convert \"" + value + "\" to " + type.getSimpleName(), e);
        }
        throw new BeansException("@Value can't convert to " + type.getName()
                + "; supported types are String, int, long, double, boolean and enums");
    }

    /** Stricter than Boolean.parseBoolean, which turns any typo into false. */
    private static boolean parseBoolean(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("not true or false");
        };
    }
}
