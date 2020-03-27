package com.zuk;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.stereotype.Component;

@Component
public class PromotionsService implements BeanNameAware, InitializingBean {
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
        System.out.println("propertiesSet");
    }
}
