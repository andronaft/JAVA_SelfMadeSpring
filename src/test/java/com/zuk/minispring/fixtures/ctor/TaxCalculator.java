package com.zuk.minispring.fixtures.ctor;

import com.zuk.minispring.annotation.Component;

@Component
public class TaxCalculator {
    public int tax(int amount) {
        return amount / 5;
    }
}
