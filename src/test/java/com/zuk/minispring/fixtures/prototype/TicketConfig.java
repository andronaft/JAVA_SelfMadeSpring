package com.zuk.minispring.fixtures.prototype;

import com.zuk.minispring.annotation.Bean;
import com.zuk.minispring.annotation.Configuration;
import com.zuk.minispring.annotation.Scope;

/** A @Bean method can be a prototype too: the method runs on every request. */
@Configuration
public class TicketConfig {
    private int issued;

    @Bean
    @Scope("prototype")
    public Ticket ticket() {
        return new Ticket(++issued);
    }
}
