package com.zuk.minispring.fixtures.iface;

import com.zuk.minispring.annotation.Component;

@Component
public class CardPaymentGateway implements PaymentGateway {
    @Override
    public String pay(int amount) {
        return "paid " + amount + " by card";
    }
}
