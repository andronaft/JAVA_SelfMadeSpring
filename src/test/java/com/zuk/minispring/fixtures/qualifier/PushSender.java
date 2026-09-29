package com.zuk.minispring.fixtures.qualifier;

import com.zuk.minispring.annotation.Component;

@Component("push")
public class PushSender implements MessageSender {
    @Override
    public String send(String text) {
        return "push: " + text;
    }
}
