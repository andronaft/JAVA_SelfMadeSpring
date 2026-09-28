package com.zuk.minispring.fixtures.missing;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

@Component
public class ReportService {

    @Autowired
    private Clock clock;
}
