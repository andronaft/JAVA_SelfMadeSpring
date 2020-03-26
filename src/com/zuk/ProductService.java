package com.zuk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.stereotype.Component;

@Component
public class ProductService {
    public ProductService(int hel) {
        this.hel = hel;
    }

    @Autowired
    private PromotionsService promotionsService;

    private int hel = 2;
    public int hell = 4;

    public PromotionsService getPromotionsService(){
        return promotionsService;
    }

    public void setPromotionsService(PromotionsService promotionsService){
        this.promotionsService = promotionsService;
    }
}
