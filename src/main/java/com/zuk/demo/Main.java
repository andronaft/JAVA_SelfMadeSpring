package com.zuk.demo;

import com.zuk.minispring.beans.BeanFactory;
import com.zuk.minispring.context.ApplicationContext;

public class Main {

    private static final String BASE_PACKAGE = "com.zuk.demo";

    public static void main(String[] args) {
        runBeanFactoryStepByStep();
        runApplicationContext();
    }

    /** Drives a bare BeanFactory by hand, with a custom BeanPostProcessor. */
    private static void runBeanFactoryStepByStep() {
        System.out.println("==== BeanFactory, step by step ====");
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.addPostProcessor(new CustomPostProcessor());
        beanFactory.scan(BASE_PACKAGE);
        beanFactory.preInstantiateSingletons();

        ProductService productService = beanFactory.getBean(ProductService.class);
        PromotionsService promotionsService = productService.getPromotionsService();
        System.out.println("Injected PromotionsService, bean name: " + promotionsService.getBeanName());

        beanFactory.close();
    }

    /** The same lifecycle hidden behind an ApplicationContext, plus the ContextClosedEvent on close. */
    private static void runApplicationContext() {
        System.out.println();
        System.out.println("==== ApplicationContext ====");
        try (ApplicationContext context = new ApplicationContext(BASE_PACKAGE)) {
            ProductService productService = context.getBean(ProductService.class);
            System.out.println("ProductService has PromotionsService: " + (productService.getPromotionsService() != null));
        }
    }
}
