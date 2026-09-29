package com.zuk.minispring.fixtures.proxy;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.PreDestroy;
import com.zuk.minispring.fixtures.LifecycleLog;

@Component
public class EnglishGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name;
    }

    @PreDestroy
    public void preDestroy() {
        LifecycleLog.record("EnglishGreeter.preDestroy");
    }
}
