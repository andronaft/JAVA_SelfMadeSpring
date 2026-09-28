package com.zuk.minispring.fixtures.ambiguous;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

@Component
public class AlertService {

    @Autowired
    private Notifier notifier;
}
