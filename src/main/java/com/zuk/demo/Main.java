package com.zuk.demo;

import com.zuk.demo.lifecycle.CustomPostProcessor;
import com.zuk.demo.lifecycle.ProductService;
import com.zuk.demo.lifecycle.PromotionsService;
import com.zuk.demo.shop.Cart;
import com.zuk.demo.shop.Checkout;
import com.zuk.minispring.beans.BeanFactory;
import com.zuk.minispring.context.ApplicationContext;

public class Main {

    public static void main(String[] args) {
        runBeanFactoryStepByStep();
        runShop();
    }

    /** Drives a bare BeanFactory by hand, with a custom BeanPostProcessor, to show the lifecycle order. */
    private static void runBeanFactoryStepByStep() {
        System.out.println("==== BeanFactory, step by step ====");
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.addPostProcessor(new CustomPostProcessor());
        beanFactory.scan("com.zuk.demo.lifecycle");
        beanFactory.preInstantiateSingletons();

        ProductService productService = beanFactory.getBean(ProductService.class);
        PromotionsService promotionsService = productService.getPromotionsService();
        System.out.println("Injected PromotionsService, bean name: " + promotionsService.getBeanName());

        beanFactory.close();
    }

    /** An ApplicationContext with the rest of the features: @Configuration, @Value, qualifiers, prototypes, @Timed. */
    private static void runShop() {
        System.out.println();
        System.out.println("==== ApplicationContext ====");
        try (ApplicationContext context = new ApplicationContext("com.zuk.demo.shop")) {
            Checkout checkout = context.getBean(Checkout.class);
            System.out.println("Quote with the @Primary discount: " + checkout.quote("book"));
            System.out.println("@Qualifier(\"loyal\") discount on 200: " + checkout.loyalPrice(200));
            System.out.println("List<DiscountPolicy>: " + checkout.discountNames());
            System.out.println("Catalog injected into Checkout is a proxy: " + checkout.getCatalog().getClass().getName());
            System.out.println("Prototype: two requests, two carts: " + (context.getBean(Cart.class) != context.getBean(Cart.class)));
        }
    }
}
