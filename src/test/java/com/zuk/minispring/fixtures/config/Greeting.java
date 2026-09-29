package com.zuk.minispring.fixtures.config;

import java.time.Clock;

/** Not annotated: it only becomes a bean through a @Bean method. */
public record Greeting(String text, Clock clock) {
}
