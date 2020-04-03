package com.zuk.demo;

import com.zuk.minispring.beans.BeanFactory;
import com.zuk.minispring.context.ApplicationContext;

public class Main {

    private static final String BASE_PACKAGE = "com.zuk.demo";

    public static void main(String[] args) throws ReflectiveOperationException {
        runBeanFactoryStepByStep();
        runApplicationContext();
    }

    /** Walks through every lifecycle phase manually, with a custom BeanPostProcessor. */
    private static void runBeanFactoryStepByStep() throws ReflectiveOperationException {
        System.out.println("==== BeanFactory, step by step ====");
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.addPostProcessor(new CustomPostProcessor());
        beanFactory.instantiate(BASE_PACKAGE);
        beanFactory.populateProperties();
        beanFactory.injectBeanNames();
        beanFactory.initializeBeans();

        ProductService productService = (ProductService) beanFactory.getBean("ProductService");
        PromotionsService promotionsService = productService.getPromotionsService();
        System.out.println("Injected PromotionsService, bean name: " + promotionsService.getBeanName());

        beanFactory.close();
    }

    /** The same lifecycle hidden behind an ApplicationContext, plus the ContextClosedEvent on close. */
    private static void runApplicationContext() throws ReflectiveOperationException {
        System.out.println();
        System.out.println("==== ApplicationContext ====");
        ApplicationContext context = new ApplicationContext(BASE_PACKAGE);
        ProductService productService = (ProductService) context.getBean("ProductService");
        System.out.println("ProductService has PromotionsService: " + (productService.getPromotionsService() != null));
        context.close();
    }
}
