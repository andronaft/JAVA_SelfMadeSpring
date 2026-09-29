package com.zuk.minispring.fixtures.inherited;

import com.zuk.minispring.annotation.Autowired;

/** Not a bean itself; declares a field its subclasses need injected. */
public abstract class BaseController {
    @Autowired
    private AuditLog auditLog;

    public AuditLog getAuditLog() {
        return auditLog;
    }
}
