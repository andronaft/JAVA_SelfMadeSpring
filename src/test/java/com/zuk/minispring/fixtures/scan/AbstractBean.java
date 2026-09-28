package com.zuk.minispring.fixtures.scan;

import com.zuk.minispring.annotation.Component;

/** Abstract, so the scanner must skip it even though it's annotated. */
@Component
public abstract class AbstractBean {
}
