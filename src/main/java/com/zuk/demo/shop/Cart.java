package com.zuk.demo.shop;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/** Every customer gets a cart of their own. */
@Component
@Scope("prototype")
public class Cart {
    private final List<String> items = new ArrayList<>();

    public void add(String item) {
        items.add(item);
    }

    public List<String> getItems() {
        return items;
    }
}
