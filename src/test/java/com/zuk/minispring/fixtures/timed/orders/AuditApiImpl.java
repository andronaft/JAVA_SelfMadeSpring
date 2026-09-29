package com.zuk.minispring.fixtures.timed.orders;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.aop.Timed;

/** @Timed on the class: every method of its interfaces is measured. */
@Component
@Timed
public class AuditApiImpl implements AuditApi {
    private int count;

    @Override
    public void record(String entry) {
        count++;
    }

    @Override
    public int count() {
        return count;
    }
}
