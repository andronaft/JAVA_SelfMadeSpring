package com.zuk.minispring.fixtures.timednointerface;

import com.zuk.minispring.aop.TimedBeanPostProcessor;
import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Configuration;

@Configuration
public class TimingConfig {
    @Bean
    public static TimedBeanPostProcessor timedBeanPostProcessor() {
        return new TimedBeanPostProcessor((beanName, method, nanos) -> { });
    }
}
