package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.ctor.InvoiceRepository;
import com.zuk.minispring.fixtures.ctor.InvoiceService;
import com.zuk.minispring.fixtures.ctor.LegacyExporter;
import com.zuk.minispring.fixtures.ctor.ReportPrinter;
import com.zuk.minispring.fixtures.ctor.TaxCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstructorInjectionTest {

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactory();
    }

    @Test
    void singleConstructorIsUsedWithoutAnnotation() {
        beanFactory.scan("com.zuk.minispring.fixtures.ctor");

        InvoiceService invoiceService = beanFactory.getBean(InvoiceService.class);
        assertSame(beanFactory.getBean(InvoiceRepository.class), invoiceService.getRepository());
        assertSame(beanFactory.getBean(TaxCalculator.class), invoiceService.getTaxCalculator());
    }

    @Test
    void autowiredConstructorWinsOverNoArgConstructor() {
        beanFactory.scan("com.zuk.minispring.fixtures.ctor");

        assertSame(beanFactory.getBean(TaxCalculator.class), beanFactory.getBean(ReportPrinter.class).getTaxCalculator());
    }

    @Test
    void noArgConstructorIsUsedWhenNoneIsAutowired() {
        beanFactory.scan("com.zuk.minispring.fixtures.ctor");

        assertNull(beanFactory.getBean(LegacyExporter.class).getTaxCalculator());
    }

    @Test
    void severalConstructorsWithoutAutowiredOrNoArgAreRejected() {
        beanFactory.scan("com.zuk.minispring.fixtures.noctor");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().contains("mark one with @Autowired"), e.getMessage());
    }

    @Test
    void missingConstructorArgumentNamesTheParameter() {
        beanFactory.registerBeanDefinition(new BeanDefinition("invoiceService", InvoiceService.class));

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().contains("parameter 'repository' of the constructor of bean 'invoiceService'"),
                e.getMessage());
    }
}
