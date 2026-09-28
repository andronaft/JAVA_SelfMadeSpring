package com.zuk.demo;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.PreDestroy;

@Component
public class ProductService {

    @Autowired
    private PromotionsService promotionsService;

    public PromotionsService getPromotionsService(){
        return promotionsService;
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("ProductService: @PreDestroy called");
    }
}
