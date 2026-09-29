package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;

@Component
@Qualifier("loyal")
public class LoyaltyDiscount implements DiscountPolicy {
    @Override
    public int apply(int price) {
        return price * 80 / 100;
    }
}
