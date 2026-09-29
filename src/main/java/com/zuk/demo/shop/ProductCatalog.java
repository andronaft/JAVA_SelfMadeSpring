package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Service;
import com.zuk.minispring.annotation.Value;
import com.zuk.minispring.aop.Timed;

/** Constructor injection of a bean (the @Primary discount) and of a property. */
@Service
public class ProductCatalog implements Catalog {
    private final DiscountPolicy discount;
    private final int basePrice;

    public ProductCatalog(DiscountPolicy discount, @Value("${shop.base-price:100}") int basePrice) {
        this.discount = discount;
        this.basePrice = basePrice;
    }

    /** Wrapped by TimedBeanPostProcessor, so every call through the Catalog interface is measured. */
    @Timed
    @Override
    public int priceOf(String product) {
        return discount.apply(basePrice);
    }
}
