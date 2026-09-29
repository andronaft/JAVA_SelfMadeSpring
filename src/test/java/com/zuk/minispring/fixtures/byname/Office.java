package com.zuk.minispring.fixtures.byname;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

/** Two printers and no @Primary: the field name picks the bean. */
@Component
public class Office {
    @Autowired
    private Printer laserPrinter;

    public Printer getPrinter() {
        return laserPrinter;
    }
}
