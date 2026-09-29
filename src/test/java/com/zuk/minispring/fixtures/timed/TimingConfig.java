package com.zuk.minispring.fixtures.timed;

import com.zuk.minispring.aop.TimedBeanPostProcessor;
import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Configuration;
import com.zuk.minispring.fixtures.LifecycleLog;

@Configuration
public class TimingConfig {

    /** Static, so the post-processor can be created before this configuration and everything else. */
    @Bean
    public static TimedBeanPostProcessor timedBeanPostProcessor() {
        return new TimedBeanPostProcessor((beanName, method, nanos) ->
                LifecycleLog.record("timed " + beanName + "." + method.getName()));
    }
}
