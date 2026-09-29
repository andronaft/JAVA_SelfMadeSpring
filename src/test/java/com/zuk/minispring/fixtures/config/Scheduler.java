package com.zuk.minispring.fixtures.config;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Value;

import java.time.Clock;

/** A scanned bean that depends on a @Bean-made one and on a property, both through its constructor. */
@Component
public class Scheduler {
    private final Clock clock;
    private final int threads;

    public Scheduler(Clock clock, @Value("${scheduler.threads:2}") int threads) {
        this.clock = clock;
        this.threads = threads;
    }

    public Clock getClock() {
        return clock;
    }

    public int getThreads() {
        return threads;
    }
}
