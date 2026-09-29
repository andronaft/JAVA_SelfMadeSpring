package com.zuk.minispring.fixtures.timed.cycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.aop.Timed;

/**
 * Created first and proxied, but PongImpl needs it before it is finished: the proxy has to be
 * made early, or PongImpl would hold the raw object.
 */
@Component
public class PingImpl implements Ping {
    @Autowired
    private Pong pong;

    @Timed
    @Override
    public String ping() {
        return "pong";
    }
}
