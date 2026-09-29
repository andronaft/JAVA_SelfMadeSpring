package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;
import com.zuk.minispring.context.ApplicationListener;
import com.zuk.minispring.context.ContextClosedEvent;

import java.util.Currency;
import java.util.List;

@Component
public class Checkout implements ApplicationListener<ContextClosedEvent> {

    /** The TimedBeanPostProcessor's proxy, not the ProductCatalog itself. */
    @Autowired
    private Catalog catalog;

    @Autowired
    @Qualifier("loyal")
    private DiscountPolicy loyaltyDiscount;

    @Autowired
    private List<DiscountPolicy> allDiscounts;

    /** Made by a @Bean method: java.util.Currency can't carry @Component. */
    @Autowired
    private Currency currency;

    public String quote(String product) {
        return product + ": " + catalog.priceOf(product) + " " + currency.getCurrencyCode();
    }

    public int loyalPrice(int price) {
        return loyaltyDiscount.apply(price);
    }

    public List<String> discountNames() {
        return allDiscounts.stream().map(discount -> discount.getClass().getSimpleName()).toList();
    }

    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("Checkout: received ContextClosedEvent");
    }
}
