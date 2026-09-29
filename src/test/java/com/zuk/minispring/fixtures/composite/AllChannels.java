package com.zuk.minispring.fixtures.composite;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Primary;

import java.util.List;

/** A composite: itself a Channel, it receives every other Channel. */
@Component
@Primary
public class AllChannels implements Channel {
    private final List<Channel> channels;

    public AllChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public List<Channel> getChannels() {
        return channels;
    }
}
