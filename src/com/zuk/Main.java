package com.zuk;

import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        new Main();
    }

    public Main() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.addPostProcessor(new CustomPostProcessor());
        beanFactory.instantiate("com.zuk");
        beanFactory.populateProperties();
        beanFactory.injectBeanNames();
        beanFactory.initializeBeans();


        ProductService productService = (ProductService) beanFactory.getBean("ProductService");
        PromotionsService promotionsService = productService.getPromotionsService();
        System.out.println("BeanName " + promotionsService.getBeanName());

        System.out.println(promotionsService);
    }


}
