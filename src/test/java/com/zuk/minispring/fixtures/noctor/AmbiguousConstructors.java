package com.zuk.minispring.fixtures.noctor;

import com.zuk.minispring.annotation.Component;

/** Two constructors, neither @Autowired nor no-arg: the container can't choose. */
@Component
public class AmbiguousConstructors {
    public AmbiguousConstructors(String name) {
    }

    public AmbiguousConstructors(Integer id) {
    }
}
