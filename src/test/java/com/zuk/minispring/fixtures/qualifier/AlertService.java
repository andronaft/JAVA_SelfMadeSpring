package com.zuk.minispring.fixtures.qualifier;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;

import java.util.List;

/** The same resolution rules apply to constructor parameters. */
@Component
public class AlertService {
    private final MessageSender sender;
    private final List<MessageSender> allSenders;

    public AlertService(@Qualifier("push") MessageSender sender, List<MessageSender> allSenders) {
        this.sender = sender;
        this.allSenders = allSenders;
    }

    public MessageSender getSender() {
        return sender;
    }

    public List<MessageSender> getAllSenders() {
        return allSenders;
    }
}
