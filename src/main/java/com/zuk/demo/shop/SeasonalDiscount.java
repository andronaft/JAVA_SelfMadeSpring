package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Primary;

/** The discount everyone gets unless they ask for another one. */
@Component
@Primary
public class SeasonalDiscount implements DiscountPolicy {
    @Override
    public int apply(int price) {
        return price * 90 / 100;
    }
}
