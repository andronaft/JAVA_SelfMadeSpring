package com.zuk.minispring.fixtures.proxy;

import com.zuk.minispring.annotation.Component;

/** Depends on the Greeter, so it must receive whatever a post-processor put in the Greeter's place. */
@Component
public class GreetingService {
    private final Greeter greeter;

    public GreetingService(Greeter greeter) {
        this.greeter = greeter;
    }

    public Greeter getGreeter() {
        return greeter;
    }
}
