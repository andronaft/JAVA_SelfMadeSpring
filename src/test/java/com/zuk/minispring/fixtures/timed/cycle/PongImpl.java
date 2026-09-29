package com.zuk.minispring.fixtures.timed.cycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;

@Component
public class PongImpl implements Pong {
    @Autowired
    private Ping ping;

    @Override
    public Ping getPing() {
        return ping;
    }
}
