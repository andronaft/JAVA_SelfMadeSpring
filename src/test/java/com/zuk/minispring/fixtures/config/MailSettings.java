package com.zuk.minispring.fixtures.config;

import com.zuk.minispring.annotation.Component;
import com.zuk.minispring.annotation.Value;

@Component
public class MailSettings {
    public enum Mode { TEST, LIVE }

    @Value("${mail.host}")
    private String host;

    @Value("${mail.port:25}")
    private int port;

    @Value("${mail.tls:false}")
    private boolean tls;

    @Value("${mail.mode:TEST}")
    private Mode mode;

    @Value("smtp://${mail.host}:${mail.port:25}")
    private String url;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isTls() {
        return tls;
    }

    public Mode getMode() {
        return mode;
    }

    public String getUrl() {
        return url;
    }
}
