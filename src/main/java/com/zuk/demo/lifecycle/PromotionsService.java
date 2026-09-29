package com.zuk.demo.lifecycle;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.beans.BeanNameAware;
import com.zuk.minispring.beans.InitializingBean;
import com.zuk.minispring.context.ApplicationListener;
import com.zuk.minispring.context.ContextClosedEvent;

@Component
public class PromotionsService implements BeanNameAware, InitializingBean, ApplicationListener<ContextClosedEvent> {
    private String beanName;

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    public String getBeanName(){
        return beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("PromotionsService: afterPropertiesSet called");
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("PromotionsService: received ContextClosedEvent");
    }
}
