package com.zuk.minispring.aop;

import java.lang.reflect.Method;

/** Receives the duration of every @Timed call, including calls that throw. */
@FunctionalInterface
public interface TimingListener {
    void onTimed(String beanName, Method method, long nanos);
}
