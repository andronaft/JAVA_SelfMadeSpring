package com.zuk.minispring.fixtures.prototype;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

/** A singleton with two prototype fields: each field gets its own cart, once. */
@Component
public class Checkout {
    @Autowired
    private ShoppingCart firstCart;

    @Autowired
    private ShoppingCart secondCart;

    public ShoppingCart getFirstCart() {
        return firstCart;
    }

    public ShoppingCart getSecondCart() {
        return secondCart;
    }
}
