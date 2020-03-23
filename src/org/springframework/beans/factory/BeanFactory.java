package org.springframework.beans.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.stereotype.Component;
import org.springframework.beans.factory.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class BeanFactory {
    private Map<String, Object> singletons = new HashMap();

    public Object getBean(String beanName){
        return singletons.get(beanName);
    }
    public void instantiate(String basePackage) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        String path = basePackage.replace('.','/');

        try {

            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()){
               URL resource = resources.nextElement();

                File file = new File(resource.toURI());
                for (File classFile: file.listFiles()){
                    String fileName = classFile.getName();

                    if(fileName.endsWith(".class")){
                        String className = fileName.substring(0,fileName.indexOf("."));

                        Class classObject = Class.forName(basePackage + "." + className);

                        if(classObject.isAnnotationPresent(Component.class)|| classObject.isAnnotationPresent(Service.class)){
                            System.out.println("Component: " + classObject);
                        }
                    }
                }

            }

        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void populateProperties() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        System.out.println("==populateProperties==");


        for (Object object : singletons.values()) {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {

                    for (Object dependency : singletons.values()) {
                        if (dependency.getClass().equals(field.getType())) {
                            String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                            System.out.println("Setter name = " + setterName);
                            Method setter = object.getClass().getMethod(setterName, dependency.getClass());
                            setter.invoke(object, dependency);
                        }
                    }
                }
            }
        }

    }
}
