package com.streaminglab.origin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "origin.storage")
public class OriginProperties {

    /**
     * Plain String, not java.nio.file.Path: Spring's Path binder treats a leading ".."
     * as an escaping classpath resource path and rejects it before this ever runs.
     */
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
