package com.zuk.minispring.fixtures.failing;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.beans.DisposableBean;
import com.zuk.minispring.fixtures.LifecycleLog;

@Component
public class Database implements DisposableBean {
    @Override
    public void destroy() {
        LifecycleLog.record("Database.destroy");
    }
}
