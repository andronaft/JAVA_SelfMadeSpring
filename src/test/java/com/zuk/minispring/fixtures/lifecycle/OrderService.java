package com.zuk.minispring.fixtures.lifecycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.beans.BeanNameAware;
import com.zuk.minispring.beans.InitializingBean;
import com.zuk.minispring.context.ApplicationListener;
import com.zuk.minispring.context.ContextClosedEvent;
import com.zuk.minispring.fixtures.LifecycleLog;

@Component
public class OrderService implements BeanNameAware, InitializingBean, ApplicationListener<ContextClosedEvent> {

    @Autowired
    private OrderRepository orderRepository;

    private String beanName;

    public OrderRepository getOrderRepository() {
        return orderRepository;
    }

    public String getBeanName() {
        return beanName;
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public void afterPropertiesSet() {
        LifecycleLog.record("OrderService.afterPropertiesSet");
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LifecycleLog.record("OrderService.onContextClosed");
    }
}
