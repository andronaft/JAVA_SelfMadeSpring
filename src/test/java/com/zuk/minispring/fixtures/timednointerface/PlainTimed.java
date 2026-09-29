package com.zuk.minispring.fixtures.timednointerface;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.aop.Timed;

/** @Timed, but no interface for a JDK proxy to implement. */
@Component
public class PlainTimed {
    @Timed
    public void work() {
    }
}
