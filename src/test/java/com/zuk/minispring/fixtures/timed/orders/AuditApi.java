package com.zuk.minispring.fixtures.timed.orders;

public interface AuditApi {
    void record(String entry);

    int count();
}
