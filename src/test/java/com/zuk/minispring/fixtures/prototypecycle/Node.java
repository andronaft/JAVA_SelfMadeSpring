package com.zuk.minispring.fixtures.prototypecycle;

import com.zuk.minispring.annotation.Autowired;
import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Scope;

/** A prototype that needs another instance of itself: it would never end. */
@Component
@Scope("prototype")
public class Node {
    @Autowired
    private Node next;
}
