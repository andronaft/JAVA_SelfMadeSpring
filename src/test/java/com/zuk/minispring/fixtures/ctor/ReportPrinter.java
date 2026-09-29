package com.zuk.minispring.fixtures.ctor;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

/** Several constructors: the @Autowired one wins over the no-arg one. */
@Component
public class ReportPrinter {
    private final TaxCalculator taxCalculator;

    public ReportPrinter() {
        this(null);
    }

    @Autowired
    public ReportPrinter(TaxCalculator taxCalculator) {
        this.taxCalculator = taxCalculator;
    }

    public TaxCalculator getTaxCalculator() {
        return taxCalculator;
    }
}
