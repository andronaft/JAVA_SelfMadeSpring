package com.zuk.minispring.fixtures.ctor;

import com.zuk.minispring.annotation.Service;

/** A single constructor: the container uses it without any annotation. */
@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    private final TaxCalculator taxCalculator;

    public InvoiceService(InvoiceRepository repository, TaxCalculator taxCalculator) {
        this.repository = repository;
        this.taxCalculator = taxCalculator;
    }

    public InvoiceRepository getRepository() {
        return repository;
    }

    public TaxCalculator getTaxCalculator() {
        return taxCalculator;
    }
}
