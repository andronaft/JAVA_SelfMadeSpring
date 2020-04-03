package com.zuk.minispring.context;

public interface ApplicationListener<E> {
    void onApplicationEvent(E event);
}
