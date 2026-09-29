package com.zuk.minispring.fixtures.failing;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.beans.InitializingBean;

/** Fails during startup, after the Database bean it depends on has been created. */
@Component
public class Migrator implements InitializingBean {
    public Migrator(Database database) {
    }

    @Override
    public void afterPropertiesSet() {
        throw new IllegalStateException("migration failed");
    }
}
