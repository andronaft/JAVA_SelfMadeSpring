package com.zuk.demo;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.PreDestroy;

@Component
public class ProductService {

    private final PromotionsService promotionsService;

    /** The only constructor, so the container uses it and passes the PromotionsService bean. */
    public ProductService(PromotionsService promotionsService) {
        this.promotionsService = promotionsService;
    }

    public PromotionsService getPromotionsService() {
        return promotionsService;
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("ProductService: @PreDestroy called");
    }
}
