package com.zuk.minispring.fixtures.cycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

/** Chicken and Egg need each other through fields, which the container can resolve. */
@Component
public class Chicken {
    @Autowired
    private Egg egg;

    public Egg getEgg() {
        return egg;
    }
}
