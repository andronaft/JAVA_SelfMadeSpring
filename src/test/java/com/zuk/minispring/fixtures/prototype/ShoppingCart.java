package com.zuk.minispring.fixtures.prototype;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Scope;
import com.zuk.minispring.beans.DisposableBean;
import com.zuk.minispring.beans.InitializingBean;
import com.zuk.minispring.fixtures.LifecycleLog;

/** Each customer needs a cart of their own, so every request gets a new one. */
@Component
@Scope("prototype")
public class ShoppingCart implements InitializingBean, DisposableBean {
    @Autowired
    private PriceList priceList;

    public PriceList getPriceList() {
        return priceList;
    }

    @Override
    public void afterPropertiesSet() {
        LifecycleLog.record("ShoppingCart.afterPropertiesSet");
    }

    @Override
    public void destroy() {
        LifecycleLog.record("ShoppingCart.destroy");
    }
}
