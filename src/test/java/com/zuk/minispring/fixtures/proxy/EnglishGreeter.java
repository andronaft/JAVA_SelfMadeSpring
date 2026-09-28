package com.zuk.minispring.fixtures.proxy;

import com.zuk.minispring.annotation.Component;

@Component
public class EnglishGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name;
    }
}
