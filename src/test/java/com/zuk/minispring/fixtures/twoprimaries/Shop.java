package com.zuk.minispring.fixtures.twoprimaries;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

@Component
public class Shop {
    @Autowired
    private Store store;
}
