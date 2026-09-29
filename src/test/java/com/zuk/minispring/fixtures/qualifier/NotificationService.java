package com.zuk.minispring.fixtures.qualifier;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;

import java.util.List;

@Component
public class NotificationService {
    /** Three senders match; the @Primary one is injected. */
    @Autowired
    private MessageSender defaultSender;

    /** Matches the @Qualifier("fast") on SmsSender. */
    @Autowired
    @Qualifier("fast")
    private MessageSender fastSender;

    /** Matches the bean name "push". */
    @Autowired
    @Qualifier("push")
    private MessageSender pushSender;

    @Autowired
    private List<MessageSender> allSenders;

    public MessageSender getDefaultSender() {
        return defaultSender;
    }

    public MessageSender getFastSender() {
        return fastSender;
    }

    public MessageSender getPushSender() {
        return pushSender;
    }

    public List<MessageSender> getAllSenders() {
        return allSenders;
    }
}
