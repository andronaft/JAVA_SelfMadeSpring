package com.zuk.minispring.fixtures.timed.orders;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.PreDestroy;
import com.zuk.minispring.aop.Timed;
import com.zuk.minispring.context.ApplicationListener;
import com.zuk.minispring.context.ContextClosedEvent;
import com.zuk.minispring.fixtures.LifecycleLog;

@Component
public class OrderApiImpl implements OrderApi, ApplicationListener<ContextClosedEvent> {

    @Timed
    @Override
    public String place(String item) {
        if (item.isEmpty()) {
            throw new IllegalArgumentException("empty item");
        }
        return "placed " + item;
    }

    @Override
    public String status() {
        return "open";
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LifecycleLog.record("OrderApiImpl.onContextClosed");
    }

    @PreDestroy
    public void shutdown() {
        LifecycleLog.record("OrderApiImpl.preDestroy");
    }
}
