package com.zuk.minispring.fixtures.config;

import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Configuration;
import com.zuk.minispring.annotation.Value;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration
public class AppConfig {

    /** java.time.Clock can't carry @Component, which is what @Bean methods are for. */
    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    /** Parameters are injected: a bean and a property. */
    @Bean
    public Greeting greeting(@Value("${app.greeting:Hello}") String text, Clock clock) {
        return new Greeting(text, clock);
    }

    /** Static: creating it doesn't create AppConfig. */
    @Bean("audit")
    public static AuditTrail auditTrail() {
        return new AuditTrail();
    }
}
