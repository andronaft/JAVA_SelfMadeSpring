package com.zuk.minispring.fixtures.qualifier;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;

@Component
@Qualifier("fast")
public class SmsSender implements MessageSender {
    @Override
    public String send(String text) {
        return "sms: " + text;
    }
}
