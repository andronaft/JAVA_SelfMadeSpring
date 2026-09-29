package com.zuk.minispring.fixtures.cycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

@Component
public class Egg {
    @Autowired
    private Chicken chicken;

    public Chicken getChicken() {
        return chicken;
    }
}
