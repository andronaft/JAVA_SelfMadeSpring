package com.zuk.minispring.fixtures.badqualifier;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Qualifier;

/** Asks for a qualifier no bean has. */
@Component
public class Newsletter {
    @Autowired
    @Qualifier("missing")
    private Mailer mailer;
}
