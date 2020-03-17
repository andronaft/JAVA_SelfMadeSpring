package com.zuk;

import org.springframework.beans.factory.BeanFactory;

public class Main {

    public static void main(String[] args){
        new Main();
    }

    public Main(){
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.instantiate("com.zuk");
        ProductService productService = (ProductService) beanFactory.getBean("ProductService");
        System.out.println(productService);
    }


}
