package com.zuk.minispring.fixtures;

import java.util.ArrayList;
import java.util.List;

/** Records lifecycle callbacks so tests can assert on their order. */
public final class LifecycleLog {
    private static final List<String> EVENTS = new ArrayList<>();

    private LifecycleLog() {
    }

    public static void record(String event) {
        EVENTS.add(event);
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    public static void clear() {
        EVENTS.clear();
    }
}
