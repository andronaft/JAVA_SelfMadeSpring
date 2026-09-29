package com.zuk.minispring.fixtures.ctorcycle;

import com.zuk.minispring.annotation.Component;

/** Alpha -> Beta -> Gamma -> Alpha, all through constructors: no order of creation works. */
@Component
public class Alpha {
    public Alpha(Beta beta) {
    }
}
