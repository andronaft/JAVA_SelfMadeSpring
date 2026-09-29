package com.zuk.minispring.fixtures.ctor;

import com.zuk.minispring.annotation.Component;

/** Several constructors and none is @Autowired: the no-arg one is used. */
@Component
public class LegacyExporter {
    private final TaxCalculator taxCalculator;

    public LegacyExporter() {
        this.taxCalculator = null;
    }

    public LegacyExporter(TaxCalculator taxCalculator) {
        this.taxCalculator = taxCalculator;
    }

    public TaxCalculator getTaxCalculator() {
        return taxCalculator;
    }
}
