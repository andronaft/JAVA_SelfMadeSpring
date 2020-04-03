package com.zuk.minispring.context;

import com.zuk.minispring.beans.BeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ApplicationContext {
    private BeanFactory beanFactory = new BeanFactory();

    public ApplicationContext(String basePackage) throws ReflectiveOperationException{
        System.out.println("----Context is under construction----");

        beanFactory.instantiate(basePackage);
        beanFactory.populateProperties();
        beanFactory.injectBeanNames();
        beanFactory.initializeBeans();
    }

    public Object getBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

    public void close() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException{
        beanFactory.close();

        for(Object bean : beanFactory.getSingletons().values()) {
            if (bean instanceof ApplicationListener) {
                for(Type type: bean.getClass().getGenericInterfaces()){
                    if(type instanceof ParameterizedType){
                        ParameterizedType parameterizedType = (ParameterizedType) type;

                        Type firstParameter = parameterizedType.getActualTypeArguments()[0];
                        if(firstParameter.equals(ContextClosedEvent.class)){
                            Method method = bean.getClass().getMethod("onApplicationEvent", ContextClosedEvent.class);
                            method.invoke(bean, new ContextClosedEvent());
                        }
                    }
                }
            }
        }
    }
}
