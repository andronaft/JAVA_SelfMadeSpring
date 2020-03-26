package com.zuk;

import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args){
        new Main();
    }

    public Main(){
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.instantiate("com.zuk");
        System.out.println("1");
        try {

            beanFactory.populateProperties();
            System.out.println("populate");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


        ProductService productService = (ProductService) beanFactory.getBean("ProductService");
        //PromotionsService promotionsService = productService.getPromotionsService();

        System.out.println(productService);
        System.out.println(productService.getPromotionsService());
    }


}
