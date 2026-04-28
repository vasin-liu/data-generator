package org.springframework.boot.autoconfigure.elasticsearch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Boot 4 moved Elasticsearch properties to a new package. The internal
 * gensokyo starter still links against the Boot 3 package name, so this bridge
 * restores the old runtime type.
 */
public class ElasticsearchProperties {

    private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));
    private String username;
    private String password;
    private Duration connectionTimeout;
    private Duration socketTimeout;
    private boolean socketKeepAlive;
    private String pathPrefix;
    private final Restclient restclient = new Restclient();

    public List<String> getUris() {
        return this.uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Duration getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isSocketKeepAlive() {
        return this.socketKeepAlive;
    }

    public void setSocketKeepAlive(boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }

    public String getPathPrefix() {
        return this.pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public Restclient getRestclient() {
        return this.restclient;
    }

    public static class Restclient {
    }
}
