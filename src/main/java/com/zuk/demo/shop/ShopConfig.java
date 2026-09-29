package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Configuration;
import com.zuk.minispring.annotation.Value;
import com.zuk.minispring.aop.TimedBeanPostProcessor;

import java.util.Currency;
import java.util.Locale;

@Configuration
public class ShopConfig {

    /** Static, so the context can create the post-processor before any other bean. */
    @Bean
    public static TimedBeanPostProcessor timedBeanPostProcessor() {
        return new TimedBeanPostProcessor((beanName, method, nanos) -> System.out.println(
                String.format(Locale.ROOT, "[timed] %s.%s() took %.3f ms", beanName, method.getName(), nanos / 1e6)));
    }

    @Bean
    public Currency currency(@Value("${shop.currency:EUR}") String code) {
        return Currency.getInstance(code);
    }
}
