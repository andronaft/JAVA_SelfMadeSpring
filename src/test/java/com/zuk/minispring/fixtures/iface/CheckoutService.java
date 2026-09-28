package com.zuk.minispring.fixtures.iface;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Service;

/** Depends on an interface and has no setter: the field is injected directly. */
@Service
public class CheckoutService {

    @Autowired
    private PaymentGateway paymentGateway;

    public String checkout(int amount) {
        return paymentGateway.pay(amount);
    }
}
