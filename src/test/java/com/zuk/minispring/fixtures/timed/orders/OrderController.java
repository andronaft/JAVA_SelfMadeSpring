package com.zuk.minispring.fixtures.timed.orders;

import com.zuk.minispring.annotation.Component;

@Component
public class OrderController {
    private final OrderApi api;

    public OrderController(OrderApi api) {
        this.api = api;
    }

    public OrderApi getApi() {
        return api;
    }
}
