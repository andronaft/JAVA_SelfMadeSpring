package com.zuk;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        new Main();
    }

    public Main() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ApplicationContext applicationContext = null;
        try {
            applicationContext = new ApplicationContext("com.zuk");
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        applicationContext.close();

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


    void testContext() throws ReflectiveOperationException{
        ApplicationContext applicationContext = new ApplicationContext("com.kciray");
        applicationContext.close();
    }

}
