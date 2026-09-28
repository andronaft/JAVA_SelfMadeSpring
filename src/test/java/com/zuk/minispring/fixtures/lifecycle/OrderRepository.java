package com.zuk.minispring.fixtures.lifecycle;

import com.zuk.minispring.annotation.PreDestroy;
import com.zuk.minispring.annotation.Service;
import com.zuk.minispring.fixtures.LifecycleLog;
import com.zuk.minispring.beans.DisposableBean;

@Service
public class OrderRepository implements DisposableBean {

    @PreDestroy
    public void preDestroy() {
        LifecycleLog.record("OrderRepository.preDestroy");
    }

    @Override
    public void destroy() {
        LifecycleLog.record("OrderRepository.destroy");
    }
}
