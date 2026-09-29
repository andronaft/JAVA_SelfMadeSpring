package com.zuk.minispring.fixtures.qualifier;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Primary;

@Component
@Primary
public class EmailSender implements MessageSender {
    @Override
    public String send(String text) {
        return "email: " + text;
    }
}
